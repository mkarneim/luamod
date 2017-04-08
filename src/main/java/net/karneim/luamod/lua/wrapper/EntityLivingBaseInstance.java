package net.karneim.luamod.lua.wrapper;

import javax.annotation.Nullable;

import net.karneim.luamod.lua.classes.ArmorClass;
import net.karneim.luamod.lua.classes.ItemStackClass;
import net.karneim.luamod.lua.classes.LuaTypesRepo;
import net.karneim.luamod.lua.util.table.DelegatingTable;
import net.minecraft.entity.EntityLivingBase;
import net.sandius.rembulan.Table;

public class EntityLivingBaseInstance<E extends EntityLivingBase> extends EntityInstance<E> {
  public EntityLivingBaseInstance(LuaTypesRepo repo, @Nullable E delegate, Table metatable) {
    super(repo, delegate, metatable);
  }

  @Override
  protected void addProperties(DelegatingTable.Builder b) {
    super.addProperties(b);
    b.add("armor", () -> getRepo().get(ArmorClass.class)
        .newInstance(delegate.getArmorInventoryList()).getLuaObject(), null);
    b.add("mainHand", () -> getRepo().get(ItemStackClass.class)
        .newInstance(delegate.getHeldItemMainhand()).getLuaObject(), null);
    b.add("offHand", () -> getRepo().get(ItemStackClass.class)
        .newInstance(delegate.getHeldItemOffhand()).getLuaObject(), null);
    b.add("health", delegate::getHealth, this::setHealth);
  }

  private void setHealth(Object arg) {
    float value = ((Number) arg).floatValue();
    delegate.setHealth(value);
  }

}
