package net.wizardsoflua.lua.extension.api.service;

import java.time.Clock;

public interface Time {
  Clock getClock();

  long getTotalWorldTime();
}
