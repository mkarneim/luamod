package net.wizardsoflua.annotation.lua;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(METHOD)
public @interface LuaProperty {
  /**
   * The name of the property. Defaults to the lowercase method name without getter or setter
   * prefix.
   */
  String name() default "";

  /**
   * The type of the property. Defaults to a value appropriate for the return / parameter type.
   */
  String type() default "";
}
