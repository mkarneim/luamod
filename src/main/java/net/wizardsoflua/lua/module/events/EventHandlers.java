package net.wizardsoflua.lua.module.events;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.sandius.rembulan.exec.CallException;
import net.sandius.rembulan.runtime.LuaFunction;
import net.wizardsoflua.event.CustomLuaEvent;
import net.wizardsoflua.lua.classes.eventqueue.EventQueue;
import net.wizardsoflua.lua.classes.eventsubscription.EventSubscription;
import net.wizardsoflua.lua.data.Data;
import net.wizardsoflua.lua.extension.api.ParallelTaskFactory;
import net.wizardsoflua.lua.extension.api.PauseContext;
import net.wizardsoflua.lua.extension.api.inject.AfterInjection;
import net.wizardsoflua.lua.extension.api.inject.Inject;
import net.wizardsoflua.lua.extension.api.service.Config;
import net.wizardsoflua.lua.extension.api.service.Converter;
import net.wizardsoflua.lua.extension.api.service.ExceptionHandler;
import net.wizardsoflua.lua.extension.api.service.LuaExtensionLoader;
import net.wizardsoflua.lua.extension.api.service.LuaScheduler;
import net.wizardsoflua.lua.extension.api.service.Spell;
import net.wizardsoflua.lua.extension.api.service.Time;
import net.wizardsoflua.lua.module.types.TypesModule;

public class EventHandlers implements PauseContext {
  @Inject
  private Config config;
  @Inject
  private Converter converter;
  @Inject
  private ExceptionHandler exceptionHandler;
  @Inject
  private LuaExtensionLoader extensionLoader;
  @Inject
  private LuaScheduler scheduler;
  @Inject
  private Spell spell;
  @Inject
  private Time time;

  private TypesModule types;
  private long luaTickLimit;
  private boolean duringEventIntercepting;

  @AfterInjection
  public void initialize() {
    types = extensionLoader.getLuaExtension(TypesModule.class);
    luaTickLimit = config.getEventInterceptorTickLimit();
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
    scheduler.addPauseContext(this);
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

  @Override
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
    // TODO Adrodoc 15.04.2018: Rewrite using proxies (see SpellEntity.data)
    Data data = Data.createData(dataLuaObj, types);
    MinecraftForge.EVENT_BUS.post(new CustomLuaEvent(eventName, data));
  }

  public void onEvent(String eventName, Event event) {
    if (event.isCanceled()) {
      return;
    }
    // Avoid ConcurrentModificationException when unsubscribing within the interceptor
    List<EventSubscription> subscriptions = new ArrayList<>(this.subscriptions.get(eventName));
    for (EventSubscription subscription : subscriptions) {
      LuaFunction eventHandler = subscription.getEventHandler();
      Object luaEvent = converter.toLua(event);
      try {
        callDuringEventIntercepting(eventHandler, luaEvent);
      } catch (CallException | InterruptedException ex) {
        exceptionHandler.handle("Error in event interceptor", ex);
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
      scheduler.callUnpausable(luaTickLimit, function, args);
    } finally {
      duringEventIntercepting = wasDuringEventIntercepting;
    }
  }

  public boolean isDuringEventIntercepting() {
    return duringEventIntercepting;
  }
}
