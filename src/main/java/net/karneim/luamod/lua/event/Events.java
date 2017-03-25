package net.karneim.luamod.lua.event;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.karneim.luamod.lua.SpellEntity;
import net.karneim.luamod.lua.SpellRegistry;
import net.karneim.luamod.lua.classes.event.GenericLuaEventClass;
import net.sandius.rembulan.Table;

public class Events {
  private final Table env;
  private final SpellRegistry spellRegistry;
  private final Multimap<String, EventQueue> eventQueues = HashMultimap.create();
  private final Set<EventQueue> activeQueues = new HashSet<EventQueue>();

  private long currentTime;
  private long waitForEventUntil;

  public Events(Table env, SpellRegistry spellRegistry) {
    this.env = env;
    this.spellRegistry = spellRegistry;
  }

  public boolean isWaitingForEvent() {
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
    EventWrapper<?> evtWrapper = type.wrap(env, evt);
    handle(evtWrapper);
  }

  public void fire(String eventType, Object content) {
    GenericLuaEventInstance wrapper =
        GenericLuaEventClass.get().newInstance(env, content, eventType);
    Iterable<SpellEntity> spells = spellRegistry.getAll();
    for (SpellEntity spell : spells) {
      spell.getEvents().handle(wrapper);
    }
  }

  private void handle(EventWrapper event) {
    // Ensure that the lua object is generated in the context of the event producing action
    event.getLuaObject();
    String eventType = event.getType();
    Collection<EventQueue> queues = eventQueues.get(eventType);
    if (!queues.isEmpty()) {
      for (EventQueue queue : queues) {
        queue.add(event);
      }
    }
  }

}
