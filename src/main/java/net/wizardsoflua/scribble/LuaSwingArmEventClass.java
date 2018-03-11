package net.wizardsoflua.scribble;

import net.wizardsoflua.event.SwingArmEvent;
import net.wizardsoflua.lua.classes.DeclareLuaClass;
import net.wizardsoflua.lua.classes.InstanceCachingLuaClass;
import net.wizardsoflua.lua.classes.event.EventClass;

@DeclareLuaClass(name = "SwingArmEvent", superClass = EventClass.class)
public class LuaSwingArmEventClass extends
    InstanceCachingLuaClass<SwingArmEvent, LuaSwingArmEventProxy<LuaSwingArmEvent<SwingArmEvent>, SwingArmEvent>> {
  @Override
  protected LuaSwingArmEventProxy<LuaSwingArmEvent<SwingArmEvent>, SwingArmEvent> toLua(
      SwingArmEvent javaObject) {
    return new LuaSwingArmEventProxy<>(new LuaSwingArmEvent<>(this, javaObject));
  }
}
