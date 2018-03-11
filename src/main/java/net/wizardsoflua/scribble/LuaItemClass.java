package net.wizardsoflua.scribble;

import net.minecraft.item.ItemStack;
import net.sandius.rembulan.Table;
import net.sandius.rembulan.runtime.ExecutionContext;
import net.wizardsoflua.lua.classes.DeclareLuaClass;
import net.wizardsoflua.lua.classes.ObjectClass;
import net.wizardsoflua.lua.classes.ProxyCachingLuaClass;
import net.wizardsoflua.lua.function.NamedFunction2;

@DeclareLuaClass(name = "Item", superClass = ObjectClass.class)
public class LuaItemClass extends ProxyCachingLuaClass<ItemStack, LuaItemProxy> {
  @Override
  protected LuaItemProxy toLua(ItemStack javaObject) {
    return new LuaItemProxy(new LuaItem(this, javaObject));
  }

  public LuaItemClass() {
    add(new PutNbtFunction());
  }

  private class PutNbtFunction extends NamedFunction2 {
    @Override
    public String getName() {
      return "putNbt";
    }

    @Override
    public void invoke(ExecutionContext context, Object arg1, Object arg2) {
      LuaItem self = getConverters().toJava(LuaItem.class, arg1, 1, "self", getName());
      Table nbt = getConverters().toJava(Table.class, arg2, 2, "nbt", getName());
      self.putNbt(nbt);
      context.getReturnBuffer().setTo();
    }
  }
}
