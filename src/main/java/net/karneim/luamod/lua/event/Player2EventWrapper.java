package net.karneim.luamod.lua.event;

import javax.annotation.Nullable;

import net.karneim.luamod.lua.wrapper.EntityPlayerWrapper;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.sandius.rembulan.Table;
import net.sandius.rembulan.impl.ImmutableTable;

public class Player2EventWrapper<E extends PlayerEvent> extends EventWrapper<E> {
  public Player2EventWrapper(@Nullable E event, EventType eventType) {
    super(event, eventType.name());
  }

  @Override
  protected void toLuaObject(ImmutableTable.Builder builder) {
    super.toLuaObject(builder);
    builder.add("player", new EntityPlayerWrapper(delegate.player).getLuaObject());
  }
}
