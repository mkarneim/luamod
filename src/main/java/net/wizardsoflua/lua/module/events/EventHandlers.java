package net.wizardsoflua.lua.module.events;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.sandius.rembulan.exec.CallException;
import net.sandius.rembulan.runtime.LuaFunction;
import net.sandius.rembulan.runtime.SchedulingContext;
import net.wizardsoflua.event.CustomLuaEvent;
import net.wizardsoflua.lua.classes.eventqueue.EventQueue;
import net.wizardsoflua.lua.classes.eventsubscription.EventSubscription;
import net.wizardsoflua.lua.data.Data;
import net.wizardsoflua.lua.extension.api.Config;
import net.wizardsoflua.lua.extension.api.Converter;
import net.wizardsoflua.lua.extension.api.ExceptionHandler;
import net.wizardsoflua.lua.extension.api.InitializationContext;
import net.wizardsoflua.lua.extension.api.LuaExecutor;
import net.wizardsoflua.lua.extension.api.LuaModuleLoader;
import net.wizardsoflua.lua.extension.api.ParallelTaskFactory;
import net.wizardsoflua.lua.extension.api.Spell;
import net.wizardsoflua.lua.extension.api.Time;
import net.wizardsoflua.lua.module.types.Types;
import net.wizardsoflua.lua.module.types.TypesModule;

public class EventHandlers {
  private final Converter converter;
  private final ExceptionHandler exceptionHandler;
  private final LuaExecutor executor;
  private final Time time;
  private final Types types;
  private final long luaTickLimit;
  private boolean duringEventIntercepting;

  public EventHandlers(InitializationContext context) {
    LuaModuleLoader moduleLoader = context.getModuleLoader();
    TypesModule typesModule = moduleLoader.getModule(TypesModule.class);
    types = typesModule.getDelegate();
    converter = context.getConverter();
    exceptionHandler = context.getExceptionHandler();
    executor = context.getLuaExecutor();
    Config config = context.getConfig();
    luaTickLimit = config.getEventInterceptorTickLimit();
    Spell spell = context.getSpell();
    spell.addParallelTaskFactory(new ParallelTaskFactory() {
      @Override
      public void terminate() {
        subscriptions.clear();
      }

      @Override
      public boolean isFinished() {
        return subscriptions.isEmpty();
      }
    });
    spell.addSchedulingContext(new SchedulingContext() {
      @Override
      public boolean shouldPause() {
        return EventHandlers.this.shouldPause();
      }

      @Override
      public void registerTicks(int ticks) {}
    });
    time = context.getTime();
  }

  private final Multimap<String, EventQueue> queues = HashMultimap.create();
  private final EventQueue.Context eventQueueContext = new EventQueue.Context() {
    @Override
    public void disconnect(EventQueue eventQueue) {
      EventHandlers.this.disconnect(eventQueue);
    }

    @Override
    public long getCurrentTime() {
      return time.getTotalWorldTime();
    }
  };
  /**
   * Using a linked multimap, because the order of subscriptions matters as later event listeners
   * are not called if the event was canceled by a previous one.
   */
  private final Multimap<String, EventSubscription> subscriptions = LinkedHashMultimap.create();
  private final EventSubscription.Context subscriptionContext = new EventSubscription.Context() {
    @Override
    public void unsubscribe(EventSubscription subscription) {
      for (String eventName : subscription.getEventNames()) {
        subscriptions.remove(eventName, subscription);
      }
    }
  };

  /**
   * @return the value of {@link #subscriptions}
   */
  public Multimap<String, EventSubscription> getSubscriptions() {
    return subscriptions;
  }

  public EventQueue connect(Iterable<String> eventNames) {
    EventQueue result = new EventQueue(eventNames, eventQueueContext);
    for (String name : eventNames) {
      queues.put(name, result);
    }
    return result;
  }

  public EventSubscription subscribe(Iterable<String> eventNames, LuaFunction eventHandler) {
    return subscribe(new EventSubscription(eventNames, eventHandler, subscriptionContext));
  }

  private EventSubscription subscribe(EventSubscription subscription) {
    for (String eventName : subscription.getEventNames()) {
      subscriptions.put(eventName, subscription);
    }
    return subscription;
  }

  public void disconnect(EventQueue queue) {
    for (String name : queue.getNames()) {
      queues.remove(name, queue);
    }
  }

  public boolean shouldPause() {
    if (queues.isEmpty()) {
      // no queues -> nothing to wait for, so keep running
      return false;
    }
    long now = time.getTotalWorldTime();

    for (EventQueue queue : queues.values()) {
      long waitUntil = queue.getWaitUntil();
      if (now <= waitUntil) {
        // we are still waiting for a message
        if (!queue.isEmpty()) {
          // but we have received one -> wake up
          return false;
        }
        // but we havn't yet received one -> pause
        return true;
      }
    }
    return false;
  }

  public void fire(String eventName, Object dataLuaObj) {
    Data data = Data.createData(dataLuaObj, types);
    MinecraftForge.EVENT_BUS.post(new CustomLuaEvent(eventName, data));
  }

  public void onEvent(String eventName, Event event) {
    if (event.isCanceled()) {
      return;
    }
    for (EventSubscription subscription : subscriptions.get(eventName)) {
      LuaFunction eventHandler = subscription.getEventHandler();
      Object luaEvent = converter.toLua(event);
      try {
        callDuringEventIntercepting(eventHandler, luaEvent);
      } catch (CallException | InterruptedException ex) {
        exceptionHandler.handle(ex);
        return;
      }
      if (event.isCanceled()) {
        return;
      }
    }
    for (EventQueue eventQueue : queues.get(eventName)) {
      eventQueue.add(event);
    }
  }

  private void callDuringEventIntercepting(LuaFunction function, Object... args)
      throws CallException, InterruptedException {
    boolean wasDuringEventIntercepting = duringEventIntercepting;
    duringEventIntercepting = true;
    try {
      executor.callUnpausable(luaTickLimit, function, args);
    } finally {
      duringEventIntercepting = wasDuringEventIntercepting;
    }
  }

  public boolean isDuringEventIntercepting() {
    return duringEventIntercepting;
  }
}
