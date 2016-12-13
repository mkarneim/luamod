package net.karneim.luamod.lua.event;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.karneim.luamod.LuaMod;
import net.karneim.luamod.lua.SpellEntity;

public class Events {
  private final LuaMod mod;
  private final Multimap<String, EventQueue> eventQueues = HashMultimap.create();
  private final Set<EventQueue> activeQueues = new HashSet<EventQueue>();

  private long currentTime;
  private long wakeUpTime;
  private long waitForEventUntil;

  public Events(LuaMod mod) {
    this.mod = mod;
  }

  public boolean isWaiting() {
    return isSleeping() || isWaitingForEvent();
  }

  public void startSleep(long duration) {
    this.wakeUpTime = currentTime + duration;
  }

  private boolean isSleeping() {
    return wakeUpTime > currentTime;
  }

  private boolean isWaitingForEvent() {
    if (waitForEventUntil <= currentTime || activeQueues.isEmpty())
      return false;
    for (EventQueue listener : activeQueues) {
      if (listener.hasNext()) {
        return false;
      }
    }
    return true;
  }

  public EventQueue register(String type) {
    EventQueue result = new EventQueue(type);
    eventQueues.put(type, result);
    return result;
  }

  public boolean deregister(EventQueue queue) {
    return eventQueues.remove(queue.getType(), queue);
  }

  public void waitForEvents(Collection<? extends EventQueue> queues, int timeout) {
    activeQueues.clear();
    activeQueues.addAll(queues);
    waitForEventUntil = currentTime + timeout;
  }

  public void stopWaitingForEvent() {
    waitForEventUntil = currentTime;
    activeQueues.clear();
  }

  public void setCurrentTime(long currentTime) {
    this.currentTime = currentTime;
  }

  public void handle(EventType type, Object evt) {
    Collection<EventQueue> queues = eventQueues.get(type.name());
    if (!queues.isEmpty()) {
      // TODO move up in call stack: since the wrapper produces an immutable table, it could be
      // created earlier
      EventWrapper<?> wrapper = type.wrap(evt);
      for (EventQueue queue : queues) {
        queue.add(wrapper);
      }
    }
  }

  private void handle(EventWrapper event) {
    String eventType = event.getType();
    Collection<EventQueue> queues = eventQueues.get(eventType);
    if (!queues.isEmpty()) {
      for (EventQueue queue : queues) {
        queue.add(event);
      }
    }
  }

  public void fire(String eventType, Object content) {
    GenericLuaEventWrapper wrapper = new GenericLuaEventWrapper(content, eventType);
    Iterable<SpellEntity> spells = mod.getSpellRegistry().getAll();
    for (SpellEntity spell : spells) {
      spell.getEvents().handle(wrapper);
    }
  }

}
