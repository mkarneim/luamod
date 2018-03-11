package net.wizardsoflua.lua.classes.entity;

import net.minecraft.entity.EntityLiving;
import net.wizardsoflua.annotation.GenerateLuaClass;
import net.wizardsoflua.annotation.GenerateLuaDoc;
import net.wizardsoflua.annotation.LuaProperty;
import net.wizardsoflua.lua.classes.ProxyingLuaClass;

@GenerateLuaClass(name = MobApi.NAME)
@GenerateLuaDoc(subtitle = "Mobile Creatures")
public class MobApi<D extends EntityLiving> extends EntityApi<D> {
  public static final String NAME = "Mob";

  public MobApi(ProxyingLuaClass<?, ?> luaClass, D delegate) {
    super(luaClass, delegate);
  }

  @LuaProperty
  public boolean getAi() {
    return !delegate.isAIDisabled();
  }

  @LuaProperty
  public void setAi(boolean ai) {
    delegate.setNoAI(!ai);
  }
}
