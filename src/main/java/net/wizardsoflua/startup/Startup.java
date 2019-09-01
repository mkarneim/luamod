package net.wizardsoflua.startup;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;

import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.wizardsoflua.WolAnnouncementMessage;
import net.wizardsoflua.lua.LuaCommand;

public class Startup {

  private static final Comparator<String> MODULE_COMPARATOR = new Comparator<String>() {
    @Override
    public int compare(String o1, String o2) {
      int levels1 = StringUtils.countMatches(o1, ".");
      int levels2 = StringUtils.countMatches(o2, ".");
      return levels1 - levels2;
    }
  };

  public interface Context extends AddOnFinder.Context {
    Path getSharedLibDir();

    @Override
    MinecraftServer getServer();

    @Override
    Logger getLogger();
  }

  private final Context context;
  private final AddOnFinder addOnFinder;
  private final StartupModuleFinder startupModuleFinder = new StartupModuleFinder();

  public Startup(Context context) {
    this.context = checkNotNull(context, "context==null!");
    addOnFinder = new AddOnFinder(context);
  }

  public void runStartupSequence(CommandSource source) {
    checkNotNull(source, "source == null!");
    source.sendFeedback(new WolAnnouncementMessage("Running startup sequence..."), true);
    Path sharedLibDir = context.getSharedLibDir();
    try {
      List<String> modules = merge(startupModuleFinder.findStartupModulesIn(sharedLibDir),
          addOnFinder.getStartupModules());
      launchModules(source, modules);
    } catch (IOException e) {
      sendException(format("Error while searching %s for startup modules", sharedLibDir), e,
          source);
    }
  }

  private List<String> merge(List<String> modules1, List<String> modules2) {
    List<String> result = Lists.newArrayList(modules1);
    result.addAll(modules2);
    Collections.sort(result, MODULE_COMPARATOR);
    return result.stream().distinct().collect(Collectors.toList());
  }

  private void launchModules(CommandSource source, List<String> modules) {
    for (String module : modules) {
      launchModule(source, module);
    }
  }

  private void launchModule(CommandSource source, String module) {
    sendMessage(format("Launching module '%s'", module), source);
    String code = format("require('%s')", module);
    LuaCommand luaCommand =
        (LuaCommand) context.getServer().getCommandManager().getCommands().get("lua");
    try {
      luaCommand.execute(context.getServer(), source, code, null);
    } catch (CommandException e) {
      sendException(format("Error while executing module %s", module), e, source);
    }
  }

  private void sendException(String message, Throwable t, CommandSource source) {
    context.getLogger().error(message, t);
    String stackTrace = getStackTrace(t);
    WolAnnouncementMessage txt = new WolAnnouncementMessage(message);
    TextComponentString details = new TextComponentString(stackTrace);
    txt.setStyle(new Style().setColor(TextFormatting.RED).setBold(Boolean.valueOf(true)));
    txt.appendSibling(details);
    source.sendFeedback(txt, true); // FIXME: use sendErrorMessage
  }

  private void sendMessage(String message, CommandSource source) {
    WolAnnouncementMessage txt = new WolAnnouncementMessage(message);
    source.sendFeedback(txt, true);
  }

  private String getStackTrace(Throwable throwable) {
    StringWriter writer = new StringWriter();
    throwable.printStackTrace(new PrintWriter(writer));
    String result = writer.toString();
    if (result.length() > 200) {
      result = result.substring(0, 200) + "...";
    }
    return result;
  }

}
