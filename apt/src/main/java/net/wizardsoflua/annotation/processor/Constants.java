package net.wizardsoflua.annotation.processor;

import com.squareup.javapoet.ClassName;

public interface Constants {
  final String LUA_CLASS_ATTRIBUTES = "net.wizardsoflua.lua.classes.LuaClassAttributes";

  final ClassName LUA_TABLE_SUPERCLASS =
      ClassName.get("net.wizardsoflua.lua.table", "GeneratedLuaTable");
  final ClassName LUA_INSTANCE_TABLE_SUPERCLASS =
      ClassName.get("net.wizardsoflua.lua.table", "GeneratedLuaInstanceTable");

  final String OBJECT_CLASS = "net.wizardsoflua.lua.classes.ObjectClass";

  final String JAVA_TO_LUA_CONVERTER = "net.wizardsoflua.extension.spell.spi.JavaToLuaConverter";

  final ClassName LUA_CONVERTERS =
      ClassName.get("net.wizardsoflua.extension.spell.api.resource", "LuaConverters");

  static ClassName getNamedFunctionClassName(int numberOfArgs) {
    return ClassName.get("net.wizardsoflua.lua.function", "NamedFunction" + numberOfArgs);
  }
}
