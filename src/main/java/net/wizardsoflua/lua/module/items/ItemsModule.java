package net.wizardsoflua.lua.module.items;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.sandius.rembulan.Table;
import net.sandius.rembulan.impl.DefaultTable;
import net.sandius.rembulan.impl.NonsuspendableFunctionException;
import net.sandius.rembulan.runtime.AbstractFunction1;
import net.sandius.rembulan.runtime.ExecutionContext;
import net.sandius.rembulan.runtime.ResolvedControlThrowable;
import net.wizardsoflua.lua.Converters;

public class ItemsModule {
  public static ItemsModule installInto(Table env, Converters converters) {
    ItemsModule result = new ItemsModule(converters);
    env.rawset("Items", result.getLuaTable());
    return result;
  }

  private final Converters converters;
  private final Table luaTable = DefaultTable.factory().newTable();

  public ItemsModule(Converters converters) {
    this.converters = converters;
    luaTable.rawset("get", new GetFunction());
  }

  public Table getLuaTable() {
    return luaTable;
  }

  public @Nullable ItemStack get(String itemId) {
    Item item = Item.getByNameOrId(itemId);
    return new ItemStack(item);
  }

  private class GetFunction extends AbstractFunction1 {
    @Override
    public void invoke(ExecutionContext context, Object arg1) throws ResolvedControlThrowable {
      String id = converters.toJava(String.class, arg1, "id");
      ItemStack item = get(id);
      Object result = converters.toLua(item);
      context.getReturnBuffer().setTo(result);
    }

    @Override
    public void resume(ExecutionContext context, Object suspendedState)
        throws ResolvedControlThrowable {
      throw new NonsuspendableFunctionException();
    }
  }
}