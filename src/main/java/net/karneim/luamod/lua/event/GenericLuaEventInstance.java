package net.karneim.luamod.lua.event;

import javax.annotation.Nullable;

import net.karneim.luamod.lua.classes.LuaTypesRepo;
import net.karneim.luamod.lua.util.table.DelegatingTable;
import net.sandius.rembulan.ByteString;
import net.sandius.rembulan.Table;
import net.sandius.rembulan.impl.DefaultTable;

public class GenericLuaEventInstance extends EventWrapper<Object> {
  public GenericLuaEventInstance(LuaTypesRepo repo, @Nullable Object delegate, String name,
      Table metatable) {
    super(repo, delegate, name, metatable);
  }

  @Override
  protected void addProperties(DelegatingTable.Builder builder) {
    super.addProperties(builder);
    if (delegate instanceof ByteString) {
      builder.add("message", delegate);
    } else if (delegate instanceof String) {
      builder.add("message", delegate);
    } else if (delegate instanceof Number) {
      builder.add("message", delegate);
    } else if (delegate instanceof Boolean) {
      builder.add("message", delegate);
    } else if (delegate instanceof Table) {
      Table source = (Table) delegate;
      Object key = source.initialKey();
      while (key != null) {
        String keyStr = String.valueOf(key);
        if (keyStr.equals("id") || keyStr.equals("type")) {
          throw new IllegalArgumentException(
              String.format("Custom event must not contain a key with name '%s'!", key));
        }
        Object srcValue = source.rawget(key);
        Object copy = copyOf(srcValue);
        builder.add(key, srcValue);
        key = source.successorKeyOf(key);
      }
    }
  }

  private Object copyOf(Object content) {
    if (content instanceof Table) {
      Table table = (Table) content;
      return copyOf(table);
    }
    if (content instanceof ByteString) {
      return content;
    }
    if (content instanceof String) {
      return content;
    }
    if (content instanceof Number) {
      return content;
    }
    if (content instanceof Boolean) {
      return content;
    }
    throw new IllegalArgumentException(
        "Unsupported event content type: " + content.getClass().getSimpleName());
  }

  private Table copyOf(Table source) {
    if (source instanceof DelegatingTable) {
      DelegatingTable it = (DelegatingTable) source;
      return it;
    } else {
      Table result = DefaultTable.factory().newTable();
      Object key = source.initialKey();
      while (key != null) {
        Object srcValue = source.rawget(key);
        Object copy = copyOf(srcValue);
        result.rawset(key, srcValue);
        key = source.successorKeyOf(key);
      }
      return result;
    }
  }

}
