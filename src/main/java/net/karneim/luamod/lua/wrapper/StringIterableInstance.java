package net.karneim.luamod.lua.wrapper;

import javax.annotation.Nullable;

import net.karneim.luamod.lua.classes.LuaTypesRepo;
import net.karneim.luamod.lua.patched.PatchedImmutableTable;
import net.karneim.luamod.lua.util.wrapper.ImmutableTableWrapper;
import net.sandius.rembulan.Table;

public class StringIterableInstance extends ImmutableTableWrapper<Iterable<String>> {
  public StringIterableInstance(LuaTypesRepo repo, @Nullable Iterable<String> delegate,
      Table metatable) {
    super(repo, delegate, metatable);
  }

  @Override
  protected void addProperties(PatchedImmutableTable.Builder builder) {
    long idx = 0;
    for (String value : delegate) {
      idx++;
      builder.add(idx, value);
    }
  }
}
