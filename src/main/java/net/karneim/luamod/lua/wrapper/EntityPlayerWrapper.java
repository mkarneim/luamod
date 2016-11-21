package net.karneim.luamod.lua.wrapper;

import java.util.Iterator;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Team;
import net.sandius.rembulan.Table;
import net.sandius.rembulan.impl.DefaultTable;

public class EntityPlayerWrapper extends LuaWrapper<EntityPlayer> {
  public EntityPlayerWrapper(@Nullable EntityPlayer delegate) {
    super(delegate);
  }

  @Override
  protected Table toLuaObject() {
    DefaultTable result = new DefaultTable();
    // delegate.getAbsorptionAmount();
    result.rawset("armor", new ArmorWrapper(delegate.getArmorInventoryList()).getLuaObject());
    // delegate.getBedLocation()
    // delegate.getFoodStats().getFoodLevel()
    // delegate.getFoodStats().getSaturationLevel()
    result.rawset("health", delegate.getHealth());
    result.rawset("mainHand", new ItemStackWrapper(delegate.getHeldItemMainhand()).getLuaObject());
    result.rawset("offHand", new ItemStackWrapper(delegate.getHeldItemOffhand()).getLuaObject());
    // delegate.getInventoryEnderChest()
    result.rawset("name", delegate.getName());
    Team team = delegate.getTeam();
    result.rawset("team", team != null ? team.getRegisteredName() : null);
    return result;
  }

  private class ArmorWrapper extends LuaWrapper<Iterable<ItemStack>> {
    public ArmorWrapper(@Nullable Iterable<ItemStack> delegate) {
      super(delegate);
    }

    @Override
    protected Table toLuaObject() {
      Table result = new DefaultTable();
      Iterator<ItemStack> it = delegate.iterator();
      result.rawset("feet", new ItemStackWrapper(it.next()).getLuaObject());
      result.rawset("legs", new ItemStackWrapper(it.next()).getLuaObject());
      result.rawset("chest", new ItemStackWrapper(it.next()).getLuaObject());
      result.rawset("head", new ItemStackWrapper(it.next()).getLuaObject());
      return result;
    }
  }
}
