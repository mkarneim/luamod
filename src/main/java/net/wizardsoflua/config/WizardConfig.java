package net.wizardsoflua.config;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static net.wizardsoflua.WizardsOfLua.LOGGER;
import static net.wizardsoflua.lua.table.TableUtils.getAs;
import static net.wizardsoflua.lua.table.TableUtils.getAsOptional;
import java.io.File;
import java.util.UUID;
import net.sandius.rembulan.Table;
import net.wizardsoflua.file.Crypto;
import net.wizardsoflua.lua.module.luapath.AddPathFunction;

public class WizardConfig {

  public interface Context {
    File getLuaLibDirHome();

    void save();
  }

  private final Context context;
  private UUID id;
  private String libDir;
  private String apiKey = new Crypto().createRandomPassword();

  public WizardConfig(Table table, Context context) {
    this.context = checkNotNull(context, "context==null!");
    id = UUID.fromString(getAs(String.class, table, "id"));
    libDir = getAsOptional(String.class, table, "libDir").orElse(id.toString());
    apiKey = getAsOptional(String.class, table, "apiKey").orElse(apiKey);
  }

  public WizardConfig(UUID id, Context context) {
    this.id = id;
    this.context = checkNotNull(context, "context==null!");
    libDir = id.toString();

    File dir = getLibDir();
    if (dir.exists() && !dir.isDirectory()) {
      throw new IllegalStateException(
          format("Illegal libDir. %s is not a directory!", dir.getAbsolutePath()));
    }
    if (!dir.exists()) {
      if (!dir.mkdirs()) {
        LOGGER.warn(format("Couldn't create libDir at %s because of an unknown reason!",
            dir.getAbsolutePath()));
      }
    }
  }

  public UUID getId() {
    return id;
  }

  public File getLibDir() {
    return new File(context.getLuaLibDirHome(), libDir);
  }

  public Table writeTo(Table table) {
    table.rawset("id", id.toString());
    table.rawset("libDir", libDir);
    table.rawset("apiKey", apiKey);
    return table;
  }

  public String getLibDirPathElement() {
    return getLibDir().getAbsolutePath() + File.separator + AddPathFunction.LUA_EXTENSION_WILDCARD;
  }

  public String getRestApiKey() {
    return apiKey;
  }

  public void setRestApiKey(String apiKey) {
    this.apiKey = checkNotNull(apiKey, "apiKey==null!");
    context.save();
  }

}
