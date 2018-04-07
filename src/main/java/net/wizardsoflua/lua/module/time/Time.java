package net.wizardsoflua.lua.module.time;

import static com.google.common.base.Preconditions.checkState;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.annotation.Nullable;

import net.minecraft.world.World;
import net.sandius.rembulan.runtime.IllegalOperationAttemptException;
import net.sandius.rembulan.runtime.SchedulingContext;
import net.wizardsoflua.lua.scheduling.LuaSchedulingContext;

public class Time implements SchedulingContext, net.wizardsoflua.lua.extension.api.Time {
  public interface Context {
    Clock getClock();

    LuaSchedulingContext getCurrentSchedulingContext();
  }

  private final Context context;

  private final World world;
  private final long sleepTrigger;
  private int luaTotalTicks = 0;

  // Time measured in Game-Ticks:
  private long nextSpellWakeUpGameTime;

  public Time(World world, long luaTicksLimit, Context context) {
    this.world = world;
    this.context = context;
    this.sleepTrigger = luaTicksLimit / 2;
  }

  private LuaSchedulingContext getCurrentSchedulingContext() {
    LuaSchedulingContext result = context.getCurrentSchedulingContext();
    checkState(result != null, "This method can only be called through Lua");
    return result;
  }

  public long getAllowance() {
    return getCurrentSchedulingContext().getAllowance();
  }

  public boolean isAutosleep() {
    return getCurrentSchedulingContext().isAutosleep();
  }

  public void setAutosleep(boolean autosleep) {
    getCurrentSchedulingContext().setAutosleep(autosleep);
  }

  @Override
  public long getTotalWorldTime() {
    return world.getTotalWorldTime();
  }

  public long getRealtime() {
    return context.getClock().millis();
  }

  public void startSleep(@Nullable Integer ticks) {
    if (ticks == null) {
      if (getAllowance() < sleepTrigger) {
        ticks = 1;
      } else {
        return;
      }
    }
    if (ticks < 0) {
      throw new IllegalOperationAttemptException("attempt to sleep a negative amount of ticks");
    }
    this.nextSpellWakeUpGameTime = world.getTotalWorldTime() + ticks;
  }

  @Override
  public boolean shouldPause() {
    return nextSpellWakeUpGameTime > world.getTotalWorldTime();
  }

  @Override
  public void registerTicks(int ticks) {
    luaTotalTicks += ticks;
  }

  public long getLuaTicks() {
    return luaTotalTicks;
  }

  public String getDate(@Nullable String pattern) {
    DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    if (pattern != null) {
      formatter = DateTimeFormatter.ofPattern(pattern);
    }
    String result = LocalDateTime.now(context.getClock()).format(formatter);
    return result;
  }
}
