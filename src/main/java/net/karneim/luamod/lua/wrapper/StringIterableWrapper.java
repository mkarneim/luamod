package net.karneim.luamod.lua.wrapper;

import javax.annotation.Nullable;

import net.karneim.luamod.lua.util.wrapper.ImmutableTableWrapper;
import net.sandius.rembulan.impl.ImmutableTable;

public class StringIterableWrapper extends ImmutableTableWrapper<Iterable<String>> {
  public StringIterableWrapper(@Nullable Iterable<String> delegate) {
    super(delegate);
  }

  @Override
  protected void addProperties(ImmutableTable.Builder builder) {
    long idx = 0;
    for (String value : delegate) {
      idx++;
      builder.add(idx, value);
    }
  }
}
