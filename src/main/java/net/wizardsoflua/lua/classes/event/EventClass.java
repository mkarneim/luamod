package net.wizardsoflua.lua.classes.event;

import net.minecraftforge.fml.common.eventhandler.Event;
import net.wizardsoflua.lua.classes.DeclareLuaClass;
import net.wizardsoflua.lua.classes.ProxyingLuaClass;
import net.wizardsoflua.lua.classes.common.LuaInstanceProxy;

@DeclareLuaClass(name = EventClass.NAME)
public class EventClass extends ProxyingLuaClass<Event, EventClass.Proxy<Event>> {
  public static final String NAME = "Event";

  @Override
  public Proxy<Event> toLua(Event javaObj) {
    return new Proxy<>(this, javaObj);
  }

  public static class Proxy<D extends Event> extends LuaInstanceProxy<D> {
    public Proxy(ProxyingLuaClass<?, ?> luaClass, D delegate) {
      super(luaClass, delegate);
      addImmutable("name", getName());
      addReadOnly("cancelable", () -> delegate.isCancelable());
      add("canceled", this::isCanceled, this::setCanceled);
    }

    @Override
    public boolean isTransferable() {
      return true;
    }

    public String getName() {
      return getLuaClass().getName();
    }

    public boolean isCanceled() {
      return delegate.isCanceled();
    }

    public void setCanceled(Object arg) {
      boolean canceled = getConverters().toJava(boolean.class, arg, "canceled");
      delegate.setCanceled(canceled);
    }
  }
}
