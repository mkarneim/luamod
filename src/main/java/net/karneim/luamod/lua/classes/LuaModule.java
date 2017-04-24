package net.karneim.luamod.lua.classes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LuaModule {
  String packageName() default Constants.MODULE_PACKAGE;

  String value();
}
