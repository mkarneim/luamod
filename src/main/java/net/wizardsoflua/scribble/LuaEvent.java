package net.wizardsoflua.scribble;

import net.minecraftforge.fml.common.eventhandler.Event;
import net.wizardsoflua.annotation.GenerateLuaClass;
import net.wizardsoflua.annotation.LuaProperty;
import net.wizardsoflua.lua.classes.ObjectClass;
import net.wizardsoflua.lua.classes.ProxyingLuaClass;

@GenerateLuaClass(name = "Event", superClass = ObjectClass.class)
public class LuaEvent<D extends Event> extends LuaApiBase<D> {
  public LuaEvent(ProxyingLuaClass<?, ?> luaClass, D delegate) {
    super(luaClass, delegate);
  }

  @LuaProperty
  public String getName() {
    return getLuaClass().getName();
  }
}
