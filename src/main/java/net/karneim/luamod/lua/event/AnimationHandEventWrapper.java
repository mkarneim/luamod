package net.karneim.luamod.lua.event;

import javax.annotation.Nullable;

import net.karneim.luamod.lua.util.table.DelegatingTable;
import net.sandius.rembulan.Table;

public class AnimationHandEventWrapper<E extends AnimationHandEvent> extends EventWrapper<E> {
  public AnimationHandEventWrapper(Table env, @Nullable E event, EventType eventType,
      Table metatable) {
    super(env, event, eventType.name(), metatable);
  }

  @Override
  protected void addProperties(DelegatingTable.Builder builder) {
    super.addProperties(builder);
    // builder.addNullable("player",
    // EntityPlayerClass.get().newInstance(env, delegate.player).getLuaObject());
  }
}
