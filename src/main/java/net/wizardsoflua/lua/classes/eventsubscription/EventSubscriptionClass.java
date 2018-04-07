package net.wizardsoflua.lua.classes.eventsubscription;

import net.sandius.rembulan.runtime.ExecutionContext;
import net.wizardsoflua.lua.classes.DeclareLuaClass;
import net.wizardsoflua.lua.classes.ProxyingLuaClass;
import net.wizardsoflua.lua.classes.common.LuaInstance;
import net.wizardsoflua.lua.extension.api.function.NamedFunction1;

@DeclareLuaClass(name = EventSubscriptionClass.NAME)
public class EventSubscriptionClass
    extends ProxyingLuaClass<EventSubscription, EventSubscriptionClass.Proxy<EventSubscription>> {
  public static final String NAME = "EventSubscription";

  public EventSubscriptionClass() {
    add(new UnsubscribeFunction());
  }

  @Override
  public Proxy<EventSubscription> toLua(EventSubscription javaObj) {
    return new Proxy<>(this, javaObj);
  }

  public static class Proxy<D extends EventSubscription> extends LuaInstance<D> {
    public Proxy(ProxyingLuaClass<?, ?> luaClass, D delegate) {
      super(luaClass, delegate);
    }

    @Override
    public boolean isTransferable() {
      return false;
    }
  }

  private class UnsubscribeFunction extends NamedFunction1 {
    @Override
    public String getName() {
      return "unsubscribe";
    }

    @Override
    public void invoke(ExecutionContext context, Object arg1) {
      EventSubscription self =
          getConverters().toJava(EventSubscription.class, arg1, 1, "self", getName());
      self.unsubscribe();
      context.getReturnBuffer().setTo();
    }
  }
}
