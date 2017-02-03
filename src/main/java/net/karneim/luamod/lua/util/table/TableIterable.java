package net.karneim.luamod.lua.util.table;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import net.sandius.rembulan.Table;

public class TableIterable implements Iterable<Entry<Object, Object>> {
  private final Table table;

  public TableIterable(Table table) {
    this.table = checkNotNull(table, "table == null!");
  }

  @Override
  public Iterator<Entry<Object, Object>> iterator() {
    return new TableIterator(table);
  }
}
