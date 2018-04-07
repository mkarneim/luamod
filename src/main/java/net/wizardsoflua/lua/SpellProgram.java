package net.wizardsoflua.lua;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.file.FileSystem;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nullable;

import com.google.common.cache.Cache;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.sandius.rembulan.StateContext;
import net.sandius.rembulan.Table;
import net.sandius.rembulan.TableFactory;
import net.sandius.rembulan.Variable;
import net.sandius.rembulan.env.RuntimeEnvironment;
import net.sandius.rembulan.env.RuntimeEnvironments;
import net.sandius.rembulan.exec.CallException;
import net.sandius.rembulan.exec.CallPausedException;
import net.sandius.rembulan.exec.Continuation;
import net.sandius.rembulan.impl.StateContexts;
import net.sandius.rembulan.lib.BasicLib;
import net.sandius.rembulan.lib.CoroutineLib;
import net.sandius.rembulan.lib.MathLib;
import net.sandius.rembulan.lib.ModuleLib;
import net.sandius.rembulan.lib.StringLib;
import net.sandius.rembulan.lib.TableLib;
import net.sandius.rembulan.load.LoaderException;
import net.sandius.rembulan.runtime.LuaFunction;
import net.sandius.rembulan.runtime.SchedulingContext;
import net.wizardsoflua.lua.classes.LuaClassLoader;
import net.wizardsoflua.lua.classes.entity.PlayerApi;
import net.wizardsoflua.lua.classes.entity.PlayerClass;
import net.wizardsoflua.lua.classes.entity.PlayerInstance;
import net.wizardsoflua.lua.compiler.PatchedCompilerChunkLoader;
import net.wizardsoflua.lua.dependency.ModuleDependencies;
import net.wizardsoflua.lua.extension.api.Config;
import net.wizardsoflua.lua.extension.api.Converter;
import net.wizardsoflua.lua.extension.api.ExceptionHandler;
import net.wizardsoflua.lua.extension.api.InitializationContext;
import net.wizardsoflua.lua.extension.api.ParallelTaskFactory;
import net.wizardsoflua.lua.extension.api.Spell;
import net.wizardsoflua.lua.module.LuaModuleLoader;
import net.wizardsoflua.lua.module.entities.EntitiesModule;
import net.wizardsoflua.lua.module.events.EventsModule;
import net.wizardsoflua.lua.module.luapath.AddPathFunction;
import net.wizardsoflua.lua.module.print.PrintRedirector;
import net.wizardsoflua.lua.module.searcher.ClasspathResourceSearcher;
import net.wizardsoflua.lua.module.searcher.LuaFunctionBinaryCache;
import net.wizardsoflua.lua.module.searcher.PatchedChunkLoadPathSearcher;
import net.wizardsoflua.lua.module.spell.SpellModule;
import net.wizardsoflua.lua.module.system.SystemAdapter;
import net.wizardsoflua.lua.module.system.SystemModule;
import net.wizardsoflua.lua.module.time.Time;
import net.wizardsoflua.lua.module.time.TimeApi;
import net.wizardsoflua.lua.module.time.TimeModule;
import net.wizardsoflua.lua.scheduling.LuaExecutor;
import net.wizardsoflua.lua.scheduling.LuaSchedulingContext;
import net.wizardsoflua.spell.SpellEntity;
import net.wizardsoflua.spell.SpellException;
import net.wizardsoflua.spell.SpellExceptionFactory;

public class SpellProgram {
  private enum State {
    NEW, PAUSED, FINISHED, TERMINATED;
  }
  public interface Context {
    String getLuaPathElementOfPlayer(String nameOrUuid);

    LuaFunctionBinaryCache getLuaFunctionBinaryCache();

    Clock getClock();

    int getLuaTicksLimit();

    int getEventListenerLuaTicksLimit();
  }

  private static final String ROOT_CLASS_PREFIX = "SpellByteCode";
  private final String code;
  private final ModuleDependencies dependencies;
  private final LuaExecutor executor;
  private final StateContext stateContext;
  private final Table env;
  private final PatchedCompilerChunkLoader loader;
  private final RuntimeEnvironment runtimeEnv;
  private final SpellExceptionFactory exceptionFactory;
  private final LuaClassLoader luaClassLoader;
  private final LuaModuleLoader moduleLoader;
  private final Collection<ParallelTaskFactory> parallelTaskFactories = new ArrayList<>();
  private final long luaTickLimit;
  private ICommandSender owner;
  private State state = State.NEW;

  private Continuation continuation;
  private SpellEntity spellEntity;
  private Time time;
  private String defaultLuaPath;
  private final Context context;

  SpellProgram(ICommandSender owner, String code, ModuleDependencies dependencies,
      String defaultLuaPath, World world, SystemAdapter systemAdapter, Context context) {
    this.owner = checkNotNull(owner, "owner==null!");
    this.code = checkNotNull(code, "code==null!");
    this.dependencies = checkNotNull(dependencies, "dependencies==null!");
    this.defaultLuaPath = checkNotNull(defaultLuaPath, "defaultLuaPath==null!");
    this.context = checkNotNull(context, "context==null!");

    luaTickLimit = context.getLuaTicksLimit();
    stateContext = StateContexts.newDefaultInstance();
    executor = new LuaExecutor(stateContext);
    time = new Time(world, luaTickLimit, new Time.Context() {
      @Override
      public Clock getClock() {
        return context.getClock();
      }

      @Override
      public LuaSchedulingContext getCurrentSchedulingContext() {
        return executor.getCurrentSchedulingContext();
      }
    });
    executor.addSchedulingContext(time);

    env = stateContext.newTable();
    runtimeEnv = RuntimeEnvironments.system();
    loader = PatchedCompilerChunkLoader.of(ROOT_CLASS_PREFIX);
    exceptionFactory = new SpellExceptionFactory(ROOT_CLASS_PREFIX);
    installSystemLibraries();
    moduleLoader = new LuaModuleLoader(env, createModuleInitializationContext());
    luaClassLoader = new LuaClassLoader(env, new LuaClassLoader.Context() {
      @Override
      public @Nullable LuaSchedulingContext getCurrentSchedulingContext() {
        return executor.getCurrentSchedulingContext();
      }

      @Override
      public EventsModule getEventsModule() {
        return moduleLoader.getModule(EventsModule.class);
      }
    });
    moduleLoader.installModules();
    luaClassLoader.loadStandardClasses();
    PrintRedirector.installInto(env, new PrintRedirector.Context() {
      @Override
      public void send(String message) {
        SpellProgram.this.owner.sendMessage(new TextComponentString(message));
      }
    });
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
    new TimeModule(new TimeApi(luaClassLoader, time)).installInto(env);
    SystemModule.installInto(env, luaClassLoader, systemAdapter);
    executor.addSchedulingContext(new SchedulingContext() {
      @Override
      public boolean shouldPause() {
        return systemAdapter.shouldPause();
      }

      @Override
      public void registerTicks(int ticks) {
        // ignore, since not required here.
      }
    });
  }

  private InitializationContext createModuleInitializationContext() {
    return new InitializationContext() {
      @Override
      public net.wizardsoflua.lua.extension.api.LuaClassLoader getClassLoader() {
        return luaClassLoader;
      }

      @Override
      public Config getConfig() {
        return new Config() {
          @Override
          public long getEventInterceptorTickLimit() {
            return context.getEventListenerLuaTicksLimit();
          }
        };
      }

      @Override
      public Converter getConverter() {
        return luaClassLoader.getConverters();
      }

      @Override
      public Table getEnv() {
        return env;
      }

      @Override
      public ExceptionHandler getExceptionHandler() {
        return t -> handleException("in module", t);
      }

      @Override
      public net.wizardsoflua.lua.extension.api.LuaExecutor getLuaExecutor() {
        return executor;
      }

      @Override
      public net.wizardsoflua.lua.extension.api.LuaModuleLoader getModuleLoader() {
        return moduleLoader;
      }

      @Override
      public Spell getSpell() {
        return new Spell() {
          @Override
          public void addParallelTaskFactory(ParallelTaskFactory parallelTaskFactory) {
            parallelTaskFactories.add(parallelTaskFactory);
          }

          @Override
          public void addSchedulingContext(SchedulingContext context) {
            executor.addSchedulingContext(context);
          };
        };
      }

      @Override
      public TableFactory getTableFactory() {
        return stateContext;
      }

      @Override
      public net.wizardsoflua.lua.extension.api.Time getTime() {
        return time;
      }
    };
  }

  public LuaClassLoader getLuaClassLoader() {
    return luaClassLoader;
  }

  private Converters getConverters() {
    return luaClassLoader.getConverters();
  }

  public LuaModuleLoader getModuleLoader() {
    return moduleLoader;
  }

  public void setSpellEntity(SpellEntity spellEntity) {
    this.spellEntity = spellEntity;
  }

  public String getCode() {
    return code;
  }

  public boolean isTerminated() {
    if (state == State.TERMINATED) {
      return true;
    }
    if (state == State.FINISHED) {
      for (ParallelTaskFactory parallelTaskFactory : parallelTaskFactories) {
        if (!parallelTaskFactory.isFinished()) {
          return false;
        }
      }
      terminate();
      return true;
    }
    return false;
  }

  public void terminate() {
    state = State.TERMINATED;
    for (ParallelTaskFactory parallelTaskFactory : parallelTaskFactories) {
      parallelTaskFactory.terminate();
    }
  }

  public void resume() {
    try {
      switch (state) {
        case NEW:
          compileAndRun();
          break;
        case PAUSED:
          executor.resume(luaTickLimit, continuation);
          break;
        case FINISHED:
        case TERMINATED:
          return;
      }
      state = State.FINISHED;
    } catch (CallPausedException ex) {
      continuation = ex.getContinuation();
      state = State.PAUSED;
    } catch (Exception ex) {
      handleException("spell execution", ex);
    }
  }

  private void handleException(String during, Throwable t) {
    terminate();
    SpellException s = exceptionFactory.create(t);
    s.printStackTrace();
    String message = String.format("Error during %s: %s", during, s.getMessage());
    TextComponentString txt = new TextComponentString(message);
    txt.setStyle((new Style()).setColor(TextFormatting.RED).setBold(Boolean.valueOf(true)));
    owner.sendMessage(txt);
  }

  private void compileAndRun()
      throws LoaderException, CallException, CallPausedException, InterruptedException {

    SpellModule.installInto(env, getConverters(), spellEntity);
    EntitiesModule.installInto(env, getConverters(), spellEntity);

    dependencies.installModules(env, executor, luaTickLimit);

    LuaFunction commandLineFunc = loader.loadTextChunk(new Variable(env), "command-line", code);
    executor.call(luaTickLimit, commandLineFunc);
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
        new PatchedChunkLoadPathSearcher.Context() {
          @Override
          public String getLuaPath() {
            return defaultLuaPath;
          }
        });

    CoroutineLib.installInto(stateContext, env);
    StringLib.installInto(stateContext, env);
    MathLib.installInto(stateContext, env);
    TableLib.installInto(stateContext, env);
  }

  public void replacePlayerInstance(EntityPlayerMP newPlayer) {
    if (this.owner.getCommandSenderEntity() instanceof EntityPlayer) {
      if (this.owner.getCommandSenderEntity().getUniqueID().equals(newPlayer.getUniqueID())) {
        this.owner = newPlayer;
      }
    }
    PlayerClass playerClass = luaClassLoader.getLuaClassOfType(PlayerClass.class);
    Cache<EntityPlayerMP, PlayerInstance<PlayerApi<EntityPlayerMP>, EntityPlayerMP>> cache =
        playerClass.getCache();
    for (EntityPlayer oldPlayer : cache.asMap().keySet()) {
      if (oldPlayer.getUniqueID().equals(newPlayer.getUniqueID())) {
        PlayerInstance<PlayerApi<EntityPlayerMP>, EntityPlayerMP> oldValue =
            cache.asMap().remove(oldPlayer);
        cache.put(newPlayer, oldValue);
        oldValue.setDelegate(newPlayer);
      }
    }
  }
}
