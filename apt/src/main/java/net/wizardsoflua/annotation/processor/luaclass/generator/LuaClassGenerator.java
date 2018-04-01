package net.wizardsoflua.annotation.processor.luaclass.generator;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Objects.requireNonNull;
import static net.wizardsoflua.annotation.processor.Constants.DECLARE_LUA_CLASS;
import static net.wizardsoflua.annotation.processor.Constants.LUA_CLASS_SUPERCLASS;
import static net.wizardsoflua.annotation.processor.generator.GeneratorUtils.createFunctionClass;
import static net.wizardsoflua.annotation.processor.luaclass.GenerateLuaClassProcessor.GENERATED_ANNOTATION;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import net.wizardsoflua.annotation.processor.Utils;
import net.wizardsoflua.annotation.processor.luaclass.model.LuaClassModel;
import net.wizardsoflua.annotation.processor.model.FunctionModel;
import net.wizardsoflua.annotation.processor.model.ManualFunctionModel;

public class LuaClassGenerator {
  private final LuaClassModel model;
  private final ProcessingEnvironment env;
  private final Types types;

  public LuaClassGenerator(LuaClassModel model, ProcessingEnvironment env) {
    this.model = requireNonNull(model, "model == null!");
    this.env = requireNonNull(env, "env == null!");
    this.types = env.getTypeUtils();
  }

  public JavaFile generate() {
    String packageName = model.getPackageName();
    TypeSpec luaClassType = createLuaClass();
    return JavaFile.builder(packageName, luaClassType).build();
  }

  private TypeSpec createLuaClass() {
    TypeSpec.Builder luaClassType = classBuilder(model.getClassClassName())//
        .addAnnotation(GENERATED_ANNOTATION)//
        .addAnnotation(createDeclareLuaClassAnnotation())//
        .addModifiers(Modifier.PUBLIC)//
        .superclass(createSuperclassTypeName())//
        .addMethod(createToLuaMethod())//
        .addMethod(createOnLoadMethod())//
    ;
    for (FunctionModel function : model.getFunctions()) {
      WildcardType wildcard = types.getWildcardType(null, null);
      TypeMirror selfType = types.getDeclaredType(model.getAnnotatedElement(), wildcard);
      luaClassType.addType(createFunctionClass(function, "self", selfType, env));
    }
    return luaClassType.build();
  }

  private AnnotationSpec createDeclareLuaClassAnnotation() {
    return AnnotationSpec.builder(DECLARE_LUA_CLASS)//
        .addMember("name", "$S", model.getName())//
        .addMember("superClass", "$T.class", model.getSuperClassName())//
        .build();
  }

  private ParameterizedTypeName createSuperclassTypeName() {
    ClassName raw = LUA_CLASS_SUPERCLASS;
    TypeName delegate = model.getDelegateTypeName();
    TypeName instance = model.getParameterizedInstanceName();
    return ParameterizedTypeName.get(raw, delegate, instance);
  }

  private MethodSpec createToLuaMethod() {
    TypeName api = model.getParameterizedApiTypeName();
    TypeName delegate = model.getDelegateTypeName();
    TypeName instance = model.getParameterizedInstanceName();
    return methodBuilder("toLua")//
        .addAnnotation(Override.class)//
        .addModifiers(Modifier.PROTECTED)//
        .returns(instance)//
        .addParameter(delegate, "javaObject")//
        .addStatement("return new $T(new $T(this, javaObject))", instance, api)//
        .build();
  }

  private MethodSpec createOnLoadMethod() {
    MethodSpec.Builder onLoadMethod = methodBuilder("onLoad")//
        .addAnnotation(Override.class)//
        .addModifiers(Modifier.PROTECTED) //
    ;
    for (FunctionModel function : model.getFunctions()) {
      String Name = Utils.capitalize(function.getName());
      onLoadMethod.addStatement("add(new $LFunction())", Name);
    }
    for (ManualFunctionModel function : model.getManualFunctions()) {
      String name = function.getName();
      TypeElement functionType = function.getFunctionType();
      onLoadMethod.addStatement("add($S, new $T(this))", name, functionType);
    }
    return onLoadMethod.build();
  }
}
