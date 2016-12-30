package net.karneim.luamod.lua;

import java.io.IOException;

import net.karneim.luamod.LuaMod;
import net.karneim.luamod.cursor.Clipboard;
import net.karneim.luamod.cursor.CursorUtil;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sandius.rembulan.load.LoaderException;

public class SpellEntityFactory {

  private final LuaMod mod;

  public SpellEntityFactory(LuaMod mod) {
    this.mod = mod;
  }

  public SpellEntity create(World world, ICommandSender sender, ICommandSender owner)
      throws IOException, LoaderException {
    Clipboard clipboard = getClipboard(sender);
    BlockPos pos = getBlockPos(sender);
    Rotation rot = getRotation(sender);
    EnumFacing surface = getSurface(sender);
    SpellEntity spell = new SpellEntity(sender.getEntityWorld(), LuaMod.instance, owner, clipboard,
        pos, rot, surface);
    return spell;
  }

  private Clipboard getClipboard(ICommandSender sender) {
    if (sender.getCommandSenderEntity() instanceof SpellEntity) {
      SpellEntity lpe = (SpellEntity) sender;
      return lpe.getClipboard();
    } else if (sender.getCommandSenderEntity() instanceof EntityPlayer) {
      return mod.getClipboards().get((EntityPlayer) sender.getCommandSenderEntity());
    }
    return new Clipboard();
  }

  private EnumFacing getSurface(ICommandSender sender) {
    Entity entity = sender.getCommandSenderEntity();
    if (entity == null) {
      return null;
    } else if (entity instanceof SpellEntity) {
      return null;
    } else {
      EnumFacing side = CursorUtil.getSideLookingAt(entity);
      return side == null ? null : side;
    }
  }

  private Rotation getRotation(ICommandSender sender) {
    Entity entity = sender.getCommandSenderEntity();
    if (entity == null) {
      return Rotation.NONE;
    } else if (entity instanceof SpellEntity) {
      SpellEntity luaProcessEntity = (SpellEntity) entity;
      return luaProcessEntity.getCursor().getRotation();
    } else {
      return CursorUtil.getRotation(entity.getHorizontalFacing());
    }
  }

  private BlockPos getBlockPos(ICommandSender sender) {
    Entity entity = sender.getCommandSenderEntity();
    if (entity == null) {
      return sender.getPosition();
    } else if (entity instanceof SpellEntity) {
      SpellEntity luaProcessEntity = (SpellEntity) entity;
      return luaProcessEntity.getCursor().getWorldPosition();
    } else {
      return CursorUtil.getPositionLookingAt(entity);
    }
  }

}
