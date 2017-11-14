package net.wizardsoflua.config;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;

import com.google.common.io.Files;

import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.sandius.rembulan.StateContext;
import net.sandius.rembulan.Table;
import net.sandius.rembulan.Variable;
import net.sandius.rembulan.compiler.CompilerChunkLoader;
import net.sandius.rembulan.exec.CallException;
import net.sandius.rembulan.exec.CallPausedException;
import net.sandius.rembulan.exec.DirectCallExecutor;
import net.sandius.rembulan.impl.DefaultTable;
import net.sandius.rembulan.impl.StateContexts;
import net.sandius.rembulan.load.ChunkLoader;
import net.sandius.rembulan.load.LoaderException;
import net.sandius.rembulan.runtime.AbstractFunction1;
import net.sandius.rembulan.runtime.ExecutionContext;
import net.sandius.rembulan.runtime.LuaFunction;
import net.wizardsoflua.WizardsOfLua;
import net.wizardsoflua.config.WizardConfig.Context;
import net.wizardsoflua.lua.module.luapath.AddPathFunction;
import net.wizardsoflua.lua.table.TableUtils;

public class WolConfig {
  private static final String SHARED_HOME = "shared";

  public static WolConfig create(FMLPreInitializationEvent event, String configName)
      throws FileNotFoundException, LoaderException, CallException, CallPausedException,
      InterruptedException, IOException {
    File configDir = event.getModConfigurationDirectory();
    File wolConfigDir = new File(configDir, configName);

    File oldConfigFile = new File(wolConfigDir, configName + ".cfg");
    oldConfigFile.delete();

    File wolConfigFile = new File(wolConfigDir, configName + ".luacfg");
    return new WolConfig(wolConfigFile);
  }

  private final ExecutorService service = Executors.newFixedThreadPool(1);
  private final GeneralConfig.Context generalConfigContext = new GeneralConfig.Context() {

    @Override
    public void save() {
      WolConfig.this.saveAsync();

    }

    @Override
    public File getWolConfigDir() {
      return configFile.getParentFile();
    }
  };
  private final Context wizardConfigContext = new WizardConfig.Context() {

    @Override
    public File getLuaLibDirHome() {
      return getGeneralConfig().getLuaLibDirHome();
    }

    @Override
    public void save() {
      WolConfig.this.saveAsync();
    }
  };

  private final File configFile;

  private GeneralConfig generalConfig;
  private PermissionsConfig permissionsConfig;
  private final Map<UUID, WizardConfig> wizards = new HashMap<>();

  public WolConfig(File configFile) throws LoaderException, CallException, CallPausedException,
      InterruptedException, FileNotFoundException, IOException {
    this.configFile = checkNotNull(configFile, "configFile==null!");
    if (!configFile.getParentFile().exists()) {
      if (!configFile.getParentFile().mkdirs()) {
        WizardsOfLua.instance.logger
            .warn(format("Couldn't create config directory at %s because of an unknown reason!",
                configFile.getParentFile().getAbsolutePath()));
      }
    }
    generalConfig = new GeneralConfig(generalConfigContext);
    permissionsConfig = new PermissionsConfig();

    // Fix for issue #73 - Server still crashing when config file is a directory
    if (configFile.exists() && configFile.isDirectory()) {
      configFile.delete();
    }

    if (!configFile.exists()) {
      saveSync();
    }
    String filename = configFile.getAbsolutePath();
    String content = IOUtils.toString(new FileReader(configFile));

    StateContext state = StateContexts.newDefaultInstance();
    Table env = state.newTable();
    env.rawset("General", new GeneralFunction());
    env.rawset("Wizard", new WizardFunction());
    env.rawset("Permissions", new PermissionsFunction());

    ChunkLoader loader = CompilerChunkLoader.of("WolConfigFile");
    LuaFunction main = loader.loadTextChunk(new Variable(env), filename, content);
    DirectCallExecutor.newExecutor().call(state, main);

    saveSync();
  }

  public GeneralConfig getGeneralConfig() {
    return generalConfig;
  }
  
  public PermissionsConfig getPermissionsConfig() {
    return permissionsConfig;
  }

  public Collection<WizardConfig> getWizards() {
    return Collections.unmodifiableCollection(wizards.values());
  }

  public void clearWizardConfigs() throws IOException {
    wizards.clear();
    saveSync();
  }

  public File getSharedLibDir() {
    return getGeneralConfig().getSharedLibDir();
  }

  public String getSharedLuaPath() {
    return getSharedLibDir().getAbsolutePath() + File.separator
        + AddPathFunction.LUA_EXTENSION_WILDCARD;
  }

  public void saveAsync() {
    service.submit(new Runnable() {
      @Override
      public void run() {
        try {
          saveSync();
        } catch (IOException e) {
          WizardsOfLua.instance.logger.error("Can't save config file!", e);
        }
      }
    });
  }

  public void saveSync() throws IOException {
    int dotindex = configFile.getName().lastIndexOf('.');
    String name = configFile.getName().substring(0, dotindex);
    String ext = configFile.getName().substring(dotindex);
    File tmpFile = File.createTempFile(name, ext);

    PrintWriter writer = new PrintWriter(tmpFile);
    writeTo(writer);
    writer.close();
    if (!configFile.getParentFile().exists()) {
      if (!configFile.getParentFile().mkdirs()) {
        throw new IOException(
            format("Couldn't create config directory at %s because of an unknown reason!",
                configFile.getParentFile().getAbsolutePath()));
      }
    }
    Files.move(tmpFile, configFile);
  }

  private void writeTo(PrintWriter out) {
    out.write("General ");
    TableUtils.writeTo(out, generalConfig.writeTo(new DefaultTable()));
    out.write("\n");
    
    out.write("Permissions ");
    TableUtils.writeTo(out, permissionsConfig.writeTo(new DefaultTable()));
    out.write("\n");
    
    for (WizardConfig wizardConfig : wizards.values()) {
      out.write("Wizard ");
      TableUtils.writeTo(out, wizardConfig.writeTo(new DefaultTable()));
      out.write("\n");
    }
    out.flush();
  }

  private class GeneralFunction extends AbstractFunction1 {


    public GeneralFunction() {}

    @Override
    public void invoke(ExecutionContext context, Object arg1) {
      checkNotNull(arg1, "arg1==null!");
      checkArgument(arg1 instanceof Table, "arg1 must be instance of table!");
      Table table = (Table) arg1;
      generalConfig = new GeneralConfig(table, generalConfigContext);
      context.getReturnBuffer().setTo();
    }

    @Override
    public void resume(ExecutionContext context, Object suspendedState) {}
  }

  private class PermissionsFunction extends AbstractFunction1 {

    public PermissionsFunction() {}

    @Override
    public void invoke(ExecutionContext context, Object arg1) {
      checkNotNull(arg1, "arg1==null!");
      checkArgument(arg1 instanceof Table, "arg1 must be instance of table!");
      Table table = (Table) arg1;
      permissionsConfig = new PermissionsConfig(table);
      context.getReturnBuffer().setTo();
    }

    @Override
    public void resume(ExecutionContext context, Object suspendedState) {}
  }

  private class WizardFunction extends AbstractFunction1 {


    public WizardFunction() {}

    @Override
    public void invoke(ExecutionContext context, Object arg1) {
      checkNotNull(arg1, "arg1==null!");
      checkArgument(arg1 instanceof Table, "arg1 must be instance of table!");
      Table table = (Table) arg1;
      WizardConfig wizardConfig = new WizardConfig(table, wizardConfigContext);
      wizards.put(wizardConfig.getId(), wizardConfig);
      context.getReturnBuffer().setTo();
    }

    @Override
    public void resume(ExecutionContext context, Object suspendedState) {}
  }

  public WizardConfig getOrCreateWizardConfig(UUID id) {
    WizardConfig result = getWizardConfig(id);
    if (result == null) {
      result = createWizardConfig(id);
    }
    return result;
  }

  private @Nullable WizardConfig getWizardConfig(UUID id) {
    return wizards.get(id);
  }

  private @Nullable WizardConfig createWizardConfig(UUID id) {
    checkArgument(getWizardConfig(id) == null, "Config for player with ID=%s already exists!", id);
    WizardConfig result = new WizardConfig(id, wizardConfigContext);
    wizards.put(id, result);
    saveAsync();
    return result;
  }

}
