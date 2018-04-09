package net.wizardsoflua.lua.classes.event;

import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.wizardsoflua.lua.classes.DeclareLuaClass;
import net.wizardsoflua.lua.classes.DelegatorLuaClass;

@DeclareLuaClass(name = PlayerRespawnEventClass.NAME, superClass = EventClass.class)
public class PlayerRespawnEventClass extends
    DelegatorLuaClass<PlayerEvent.PlayerRespawnEvent, PlayerRespawnEventClass.Proxy<PlayerEvent.PlayerRespawnEvent>> {
  public static final String NAME = "PlayerRespawnEvent";

  @Override
  public Proxy<PlayerEvent.PlayerRespawnEvent> toLua(PlayerEvent.PlayerRespawnEvent javaObj) {
    return new Proxy<>(this, javaObj);
  }

  public static class Proxy<D extends PlayerEvent.PlayerRespawnEvent>
      extends EventClass.Proxy<EventApi<D>, D> {
    public Proxy(DelegatorLuaClass<?, ?> luaClass, D delegate) {
      super(new EventApi<>(luaClass, delegate));
      addImmutable("player", getConverter().toLua(delegate.player));
      addImmutable("endConquered", getConverter().toLua(delegate.isEndConquered()));
    }
  }
}
