package net.wizardsoflua.lua.extension.api;

// TODO Adrodoc 07.04.2018: How should we do the extension API for the config?
public interface Config {
  long getLuaTickLimit();

  long getEventInterceptorTickLimit();
}
