package net.wizardsoflua.lua.classes.entity;

import javax.annotation.Nullable;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.wizardsoflua.annotation.LuaProperty;
import net.wizardsoflua.lua.classes.ProxyingLuaClass;

public class EntityLivingBaseApi<D extends EntityLivingBase> extends EntityApi<D> {
  public EntityLivingBaseApi(ProxyingLuaClass<?, ?> luaClass, D delegate) {
    super(luaClass, delegate);
  }

  @Override
  public float getRotationYaw() {
    return MathHelper.wrapDegrees(delegate.renderYawOffset);
  }

  @LuaProperty
  public @Nullable ItemStack getMainhand() {
    ItemStack itemStack = delegate.getHeldItemMainhand();
    if (itemStack.isEmpty()) {
      return null;
    }
    return itemStack;
  }

  @LuaProperty
  public void setMainhand(@Nullable ItemStack mainhand) {
    if (mainhand == null) {
      mainhand = ItemStack.EMPTY;
    }
    delegate.setHeldItem(EnumHand.MAIN_HAND, mainhand);
  }

  @LuaProperty
  public @Nullable ItemStack getOffhand() {
    ItemStack itemStack = delegate.getHeldItemOffhand();
    if (itemStack.isEmpty()) {
      return null;
    }
    return itemStack;
  }

  @LuaProperty
  public void setOffhand(@Nullable ItemStack offhand) {
    if (offhand == null) {
      offhand = ItemStack.EMPTY;
    }
    delegate.setHeldItem(EnumHand.OFF_HAND, offhand);
  }
}
