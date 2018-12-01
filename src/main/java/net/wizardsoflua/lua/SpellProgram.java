package net.wizardsoflua.lua;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.Logger;

import com.google.common.cache.Cache;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.sandius.rembulan.Conversions;
import net.sandius.rembulan.StateContext;
import net.sandius.rembulan.Table;
import net.sandius.rembulan.TableFactory;
import net.sandius.rembulan.Variable;
import net.sandius.rembulan.env.RuntimeEnvironment;
import net.sandius.rembulan.env.RuntimeEnvironments;
import net.sandius.rembulan.impl.StateContexts;
import net.sandius.rembulan.lib.BasicLib;
import net.sandius.rembulan.lib.CoroutineLib;
import net.sandius.rembulan.lib.IoLib;
import net.sandius.rembulan.lib.MathLib;
import net.sandius.rembulan.lib.ModuleLib;
import net.sandius.rembulan.lib.StringLib;
import net.sandius.rembulan.lib.TableLib;
import net.sandius.rembulan.runtime.LuaFunction;
import net.wizardsoflua.WizardsOfLua;
import net.wizardsoflua.ai.WolMobAIRegistry;
import net.wizardsoflua.extension.api.inject.Resource;
import net.wizardsoflua.extension.spell.api.ParallelTaskFactory;
import net.wizardsoflua.extension.spell.api.resource.Config;
import net.wizardsoflua.extension.spell.api.resource.ExceptionHandler;
import net.wizardsoflua.extension.spell.api.resource.Injector;
import net.wizardsoflua.extension.spell.api.resource.LuaConverters;
import net.wizardsoflua.extension.spell.api.resource.LuaTypes;
import net.wizardsoflua.extension.spell.api.resource.ScriptGatewayConfig;
import net.wizardsoflua.extension.spell.api.resource.Spell;
import net.wizardsoflua.extension.spell.api.resource.Time;
import net.wizardsoflua.extension.spell.spi.JavaToLuaConverter;
import net.wizardsoflua.extension.spell.spi.LuaToJavaConverter;
import net.wizardsoflua.lua.classes.common.Delegator;
import net.wizardsoflua.lua.classes.entity.PlayerClass;
import net.wizardsoflua.lua.classes.entity.PlayerClass.Instance;
import net.wizardsoflua.lua.compiler.PatchedCompilerChunkLoader;
import net.wizardsoflua.lua.dependency.ModuleDependencies;
import net.wizardsoflua.lua.extension.InjectionScope;
import net.wizardsoflua.lua.extension.SpellScope;
import net.wizardsoflua.lua.module.events.EventsModule;
import net.wizardsoflua.lua.module.luapath.AddPathFunction;
import net.wizardsoflua.lua.module.print.PrintRedirector;
import net.wizardsoflua.lua.module.searcher.ClasspathResourceSearcher;
import net.wizardsoflua.lua.module.searcher.LuaFunctionBinaryCache;
import net.wizardsoflua.lua.module.searcher.PatchedChunkLoadPathSearcher;
import net.wizardsoflua.lua.module.spell.SpellModule;
import net.wizardsoflua.lua.module.spell.SpellsModule;
import net.wizardsoflua.lua.module.types.Types;
import net.wizardsoflua.lua.scheduling.LuaScheduler;
import net.wizardsoflua.lua.scheduling.SpellThread;
import net.wizardsoflua.lua.view.ViewFactory;
import net.wizardsoflua.spell.SpellEntity;
import net.wizardsoflua.spell.SpellException;
import net.wizardsoflua.spell.SpellExceptionFactory;
import net.wizardsoflua.spell.SpellRegistry;

public class SpellProgram {
  public interface Context {
    String getLuaPathElementOfPlayer(String nameOrUuid);

    LuaFunctionBinaryCache getLuaFunctionBinaryCache();

    Clock getClock();

    int getLuaTicksLimit();

    int getEventListenerLuaTicksLimit();

    boolean isScriptGatewayEnabled();

    Path getScriptDir();

    long getScriptTimeoutMillis();

    SpellRegistry getSpellRegistry();

    InjectionScope getRootScope();

    FileSystem getWorldFileSystem();
  }

  public static final String ROOT_CLASS_PREFIX = "SpellByteCode";
  private ICommandSender owner;
  private final SpellEntity entity;
  private final String code;
  private final LuaScheduler scheduler;
  private final StateContext stateContext;
  private final Table env;
  private final PatchedCompilerChunkLoader loader;
  private final RuntimeEnvironment runtimeEnv;
  private final SpellExceptionFactory exceptionFactory;
  private final InjectionScope injectionScope;
  private final Collection<ParallelTaskFactory> parallelTaskFactories = new ArrayList<>();
  private final List<SpellThread> threads = new CopyOnWriteArrayList<>();
  private final long luaTickLimit;
  private String defaultLuaPath;
  private final World world;
  private final Context context;
  private Injector injector;

  SpellProgram(ICommandSender owner, SpellEntity entity, String code, Object[] arguments,
      ModuleDependencies dependencies, String defaultLuaPath, World world, Context context,
      Logger logger) {
    this.owner = checkNotNull(owner, "owner==null!");
    this.entity = checkNotNull(entity, "entity==null!");
    this.code = checkNotNull(code, "code==null!");
    this.defaultLuaPath = checkNotNull(defaultLuaPath, "defaultLuaPath==null!");
    this.world = checkNotNull(world, "world == null!");
    this.context = checkNotNull(context, "context==null!");

    entity.setProgram(this);
    luaTickLimit = context.getLuaTicksLimit();
    stateContext = StateContexts.newDefaultInstance();
    scheduler = new LuaScheduler(stateContext);

    env = stateContext.newTable();
    runtimeEnv = RuntimeEnvironments.system();
    loader = PatchedCompilerChunkLoader.of(ROOT_CLASS_PREFIX);
    exceptionFactory = new SpellExceptionFactory();
    injectionScope = createInjectionScope();

    installSystemLibraries();
    loadExtensions();
    SpellModule.installInto(env, getConverters(), entity);
    SpellsModule.installInto(env, getConverters(), context.getSpellRegistry(), entity);

    PrintRedirector.installInto(env,
        message -> SpellProgram.this.owner.sendMessage(new TextComponentString(message)));
    AddPathFunction.installInto(env, getConverters(), new AddPathFunction.Context() {
      @Override
      public String getLuaPathElementOfPlayer(String nameOrUuid) {
        return context.getLuaPathElementOfPlayer(nameOrUuid);
      }

      @Override
      public void addPath(String pathelement) {
        SpellProgram.this.defaultLuaPath += ";" + pathelement;
      }
    });
    threads.add(new SpellThread(injector, "spell", luaTickLimit, () -> {
      dependencies.installModules(env, scheduler, luaTickLimit);
      LuaFunction function = loader.loadTextChunk(new Variable(env), "command-line", code);
      Conversions.toCanonicalValues(arguments);
      scheduler.call(luaTickLimit, function, arguments);
    }));
  }

  /**
   * Create the {@link InjectionScope} for this spell with all {@link Resource resources} for later
   * injection.
   */
  private InjectionScope createInjectionScope() {
    InjectionScope rootScope = context.getRootScope();
    InjectionScope scope = new SpellScope(rootScope);
    scope.registerResource(SpellEntity.class, entity);
    injector = new Injector() {
      @Override
      public <T> T injectMembers(T instance) throws IllegalStateException {
        return scope.injectMembers(instance);
      }

      @Override
      public <T> T getInstance(Class<T> cls) throws IllegalStateException {
        return scope.getInstance(cls);
      }

      @Override
      public <R> R getResource(Class<R> resourceInterface) throws IllegalArgumentException {
        return scope.getResource(resourceInterface);
      }
    };
    scope.registerResource(Injector.class, injector);
    scope.registerResource(Config.class, new Config() {
      @Override
      public long getLuaTickLimit() {
        return luaTickLimit;
      }

      @Override
      public long getEventInterceptorTickLimit() {
        return context.getEventListenerLuaTicksLimit();
      }

      @Override
      public ScriptGatewayConfig getScriptGatewayConfig() {
        return new ScriptGatewayConfig() {
          @Override
          public boolean isEnabled() {
            return context.isScriptGatewayEnabled();
          }

          @Override
          public Path getScriptDir() {
            return context.getScriptDir();
          }

          @Override
          public long getScriptTimeoutMillis() {
            return context.getScriptTimeoutMillis();
          }
        };
      }
    });
    scope.registerResource(LuaConverters.class, scope.getInstance(Converters.class));
    scope.registerResource(LuaTypes.class, scope.getInstance(Types.class));
    scope.registerResource(Table.class, env);
    scope.registerResource(ExceptionHandler.class,
        (contextMessage, t) -> handleException(contextMessage, t));
    scope.registerResource(net.wizardsoflua.extension.spell.api.resource.LuaScheduler.class,
        scheduler);
    scope.registerResource(Spell.class, new Spell() {

      @Override
      public void addParallelTaskFactory(ParallelTaskFactory factory) {
        parallelTaskFactories.add(factory);
      }

      @Override
      public void addThread(String name, LuaFunction function, Object... args) {
        Injector injector = injectionScope.getResource(Injector.class);
        SpellThread thread = new SpellThread(injector, name, luaTickLimit, function, args);
        threads.add(thread);
      }

    });
    scope.registerResource(TableFactory.class, stateContext);
    scope.registerResource(Time.class, new Time() {
      @Override
      public long getTotalWorldTime() {
        return world.getTotalWorldTime();
      }

      @Override
      public Clock getClock() {
        return context.getClock();
      }

    });
    scope.registerResource(MinecraftServer.class, world.getMinecraftServer());
    scope.registerResource(WizardsOfLua.class, WizardsOfLua.instance);
    scope.registerResource(WolMobAIRegistry.class, WizardsOfLua.instance.getMobAIRegistry());
    return scope;
  }

  private void loadExtensions() {
    ExtensionLoader.getLuaToJavaConverters().forEach(this::registerLuaToJavaConverter);
    ExtensionLoader.getJavaToLuaConverters().forEach(this::registerJavaToLuaConverter);
    ExtensionLoader.getSpellExtension().forEach(injectionScope::getInstance);
  }

  private <C extends LuaToJavaConverter<?, ?>> void registerLuaToJavaConverter(
      Class<C> converterClass) {
    C converter = injectionScope.getInstance(converterClass);
    getConverters().registerLuaToJavaConverter(converter);
  }

  private <C extends JavaToLuaConverter<?>> void registerJavaToLuaConverter(
      Class<C> converterClass) {
    C converter = injectionScope.getInstance(converterClass);
    getConverters().registerJavaToLuaConverter(converter);
  }

  public Converters getConverters() {
    return injectionScope.getInstance(Converters.class);
  }

  public EventsModule getEvents() {
    return injectionScope.getInstance(EventsModule.class);
  }

  public ViewFactory getViewFactory() {
    return injectionScope.getInstance(ViewFactory.class);
  }

  public String getCode() {
    return code;
  }

  public boolean isTerminated() {
    for (ParallelTaskFactory parallelTaskFactory : parallelTaskFactories) {
      if (!parallelTaskFactory.isFinished()) {
        return false;
      }
    }
    return threads.isEmpty();
  }

  public void terminate() {
    threads.clear();
    for (ParallelTaskFactory parallelTaskFactory : parallelTaskFactories) {
      parallelTaskFactory.terminate();
    }
  }

  public void tick() {
    for (SpellThread thread : threads) {
      try {
        if (thread.isTerminated()) {
          threads.remove(thread);
        } else {
          boolean shouldContinue = thread.tick();
          if (!shouldContinue) {
            threads.remove(thread);
          }
        }
      } catch (Exception e) {
        handleException("Error during execution of " + thread.getName(), e);
      }
    }
  }

  private void handleException(String contextMessage, Throwable t) {
    terminate();
    SpellException s = exceptionFactory.create(t);
    s.printStackTrace();
    String message = String.format("%s: %s", contextMessage, s.getMessage());
    TextComponentString txt = new TextComponentString(message);
    txt.setStyle(new Style().setColor(TextFormatting.RED).setBold(Boolean.valueOf(true)));
    owner.sendMessage(txt);
  }

  private void installSystemLibraries() {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    FileSystem fileSystem = runtimeEnv.fileSystem();
    LuaFunctionBinaryCache luaFunctionCache = context.getLuaFunctionBinaryCache();

    BasicLib.installInto(stateContext, env, /* runtimeEnv */ null, loader);

    // We don't pass the loader to the ModuleLib in order to prevent the installation of the
    // ChunkLoadPathSearcher
    ModuleLib.installInto(stateContext, env, /* runtimeEnv */ null, /* loader */ null,
        /* classLoader */ null);
    // Instead we install our own two searchers
    ClasspathResourceSearcher.installInto(env, loader, luaFunctionCache, classLoader);
    PatchedChunkLoadPathSearcher.installInto(env, loader, luaFunctionCache, classLoader, fileSystem,
        () -> defaultLuaPath);

    CoroutineLib.installInto(stateContext, env);
    StringLib.installInto(stateContext, env);
    MathLib.installInto(stateContext, env);
    TableLib.installInto(stateContext, env);

    FileSystem wolFs = context.getWorldFileSystem();
    WolRuntimeEnvironment runtimeEnvironment =
        new WolRuntimeEnvironment(RuntimeEnvironments.system(), wolFs);
    IoLib.installInto(stateContext, env, runtimeEnvironment);
  }

  public void replacePlayerInstance(EntityPlayerMP newPlayer) {
    Entity commandSender = owner.getCommandSenderEntity();
    if (commandSender instanceof EntityPlayer) {
      if (commandSender.getUniqueID().equals(newPlayer.getUniqueID())) {
        owner = newPlayer;
      }
    }
    PlayerClass playerClass = injectionScope.getInstance(PlayerClass.class);
    Cache<EntityPlayer, Delegator<? extends Instance<EntityPlayerMP>>> cache =
        playerClass.getCache();
    for (EntityPlayer oldPlayer : cache.asMap().keySet()) {
      if (oldPlayer.getUniqueID().equals(newPlayer.getUniqueID())) {
        Delegator<? extends Instance<EntityPlayerMP>> oldValue = cache.asMap().remove(oldPlayer);
        cache.put(newPlayer, oldValue);
        Instance<EntityPlayerMP> instance = oldValue.getDelegate();
        instance.setDelegate(newPlayer);
      }
    }
  }
}
