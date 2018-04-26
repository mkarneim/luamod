package net.wizardsoflua.wol.sharedfile;

import static net.minecraftforge.common.ForgeHooks.newChatWithLinks;

import java.net.URL;
import java.util.Deque;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.wizardsoflua.WizardsOfLua;
import net.wizardsoflua.WolAnnouncementMessage;
import net.wizardsoflua.wol.menu.CommandAction;
import net.wizardsoflua.wol.menu.MenuEntry;

public class SharedFileEditAction extends MenuEntry implements CommandAction {
  private static final int MAX_NUM_FILES = 500;

  private final WizardsOfLua wol;

  public SharedFileEditAction(WizardsOfLua wol) {
    this.wol = wol;
  }

  @Override
  public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
      Deque<String> argList, BlockPos targetPos) {
    String name = argList.poll();

    List<String> files = wol.getFileRepository().getSharedLuaFilenames();
    return getMatchingTokens(name, files.subList(0, Math.min(files.size(), MAX_NUM_FILES)));
  }

  @Override
  public void execute(ICommandSender sender, Deque<String> argList) throws CommandException {
    String name = argList.poll();

    URL url;
    try {
      url = wol.getFileRepository().getSharedFileEditURL(name);
    } catch (IllegalArgumentException e) {
      throw new CommandException(e.getMessage());
    }
    WolAnnouncementMessage message = new WolAnnouncementMessage("Click here to edit: ");
    message.appendSibling(newChatWithLinks(url.toExternalForm(), false));
    sender.sendMessage(message);
  }

}
