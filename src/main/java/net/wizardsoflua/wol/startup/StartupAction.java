package net.wizardsoflua.wol.startup;

import java.util.Collections;
import java.util.Deque;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.wizardsoflua.WizardsOfLua;
import net.wizardsoflua.wol.menu.CommandAction;
import net.wizardsoflua.wol.menu.MenuEntry;

public class StartupAction extends MenuEntry implements CommandAction {

  private final WizardsOfLua wol;

  public StartupAction(WizardsOfLua wol) {
    this.wol = wol;
  }

  @Override
  public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
      Deque<String> argList, BlockPos targetPos) {
    return Collections.emptyList();
  }

  @Override
  public void execute(ICommandSender sender, Deque<String> argList) throws CommandException {
    wol.runStartupSequence(sender);
  }

}
