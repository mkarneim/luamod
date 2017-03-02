package net.karneim.luamod.lua.wrapper;

import javax.annotation.Nullable;

import net.karneim.luamod.lua.patched.PatchedImmutableTable;
import net.karneim.luamod.lua.util.wrapper.ImmutableTableWrapper;
import net.sandius.rembulan.Table;

public class StringIterableWrapper extends ImmutableTableWrapper<Iterable<String>> {
  public StringIterableWrapper(Table env, @Nullable Iterable<String> delegate) {
    super(env, delegate);
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
