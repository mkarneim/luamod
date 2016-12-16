package net.karneim.luamod.lua.wrapper;

import javax.annotation.Nullable;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.GameType;
import net.sandius.rembulan.impl.ImmutableTable;

public class EntityLivingBaseWrapper<E extends EntityLivingBase> extends EntityWrapper<E> {
  public EntityLivingBaseWrapper(@Nullable E delegate) {
    super(delegate);
  }

  @Override
  protected void addProperties(ImmutableTable.Builder builder) {
    super.addProperties(builder);
    // delegate.getAbsorptionAmount();
    // delegate.getBedLocation()
    // delegate.getFoodStats().getFoodLevel()
    // delegate.getFoodStats().getSaturationLevel()
    // delegate.getInventoryEnderChest()
    builder.add("armor", new ArmorWrapper(delegate.getArmorInventoryList()).getLuaObject());
    builder.add("health", delegate.getHealth());
    builder.add("mainHand", new ItemStackWrapper(delegate.getHeldItemMainhand()).getLuaObject());
    builder.add("offHand", new ItemStackWrapper(delegate.getHeldItemOffhand()).getLuaObject());
  }

}
