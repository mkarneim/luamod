package net.wizardsoflua.lua.extension.api.service;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.wizardsoflua.config.ConversionException;
import net.wizardsoflua.lua.BadArgumentException;

public interface Converter {
  @Nullable
  <J> List<J> toJavaListNullable(Class<J> type, @Nullable Object luaObject, int argumentIndex,
      String argumentName, String functionOrPropertyName) throws BadArgumentException;

  <J> List<J> toJavaList(Class<J> type, Object luaObject, int argumentIndex, String argumentName,
      String functionOrPropertyName) throws BadArgumentException;

  <J> Optional<J> toJavaOptional(Class<J> type, @Nullable Object luaObject, int argumentIndex,
      String argumentName, String functionOrPropertyName) throws BadArgumentException;

  @Nullable
  <J> J toJavaNullable(Class<J> type, @Nullable Object luaObject, int argumentIndex,
      String argumentName, String functionOrPropertyName) throws BadArgumentException;

  <J> J toJava(Class<J> type, Object luaObject, int argumentIndex, String argumentName,
      String functionOrPropertyName) throws BadArgumentException;

  <J> List<J> toJavaList(Class<J> type, Object[] args, String functionOrPropertyName)
      throws BadArgumentException;

  @Nullable
  <J> List<J> toJavaListNullable(Class<J> type, @Nullable Object luaObject,
      String functionOrPropertyName) throws BadArgumentException;

  <J> List<J> toJavaList(Class<J> type, Object luaObject, String functionOrPropertyName)
      throws BadArgumentException;

  <J> Optional<J> toJavaOptional(Class<J> type, @Nullable Object luaObject,
      String functionOrPropertyName) throws BadArgumentException;

  @Nullable
  <J> J toJavaNullable(Class<J> type, @Nullable Object luaObject, String functionOrPropertyName)
      throws BadArgumentException;

  <J> J toJava(Class<J> type, Object luaObject, String functionOrPropertyName)
      throws BadArgumentException;

  Optional<? extends Object> toLuaOptional(@Nullable Object value) throws ConversionException;

  @Nullable
  Object toLuaNullable(@Nullable Object value) throws ConversionException;

  <J> Object toLua(J javaObject) throws ConversionException;
}
