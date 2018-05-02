package net.wizardsoflua.lua.classes.event;

import com.google.auto.service.AutoService;

import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.wizardsoflua.lua.classes.DeclareLuaClass;
import net.wizardsoflua.lua.classes.DelegatorLuaClass;
import net.wizardsoflua.lua.classes.spi.DeclaredLuaClass;

@AutoService(DeclaredLuaClass.class)
@DeclareLuaClass (name = PlayerLoggedOutEventClass.NAME, superClass = EventClass.class)
public class PlayerLoggedOutEventClass extends
    DelegatorLuaClass<PlayerEvent.PlayerLoggedOutEvent, PlayerLoggedOutEventClass.Proxy<PlayerEvent.PlayerLoggedOutEvent>> {
  public static final String NAME = "PlayerLoggedOutEvent";

  @Override
  public Proxy<PlayerEvent.PlayerLoggedOutEvent> toLua(PlayerEvent.PlayerLoggedOutEvent javaObj) {
    return new Proxy<>(this, javaObj);
  }

  public static class Proxy<D extends PlayerEvent.PlayerLoggedOutEvent>
      extends EventClass.Proxy<EventApi<D>, D> {
    public Proxy(DelegatorLuaClass<?, ?> luaClass, D delegate) {
      super(new EventApi<>(luaClass, delegate));
      addImmutable("player", getConverters().toLua(delegate.player));
    }
  }
}
