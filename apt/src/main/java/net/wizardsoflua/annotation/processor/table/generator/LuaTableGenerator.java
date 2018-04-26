package net.wizardsoflua.annotation.processor.table.generator;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Objects.requireNonNull;
import static net.wizardsoflua.annotation.processor.Constants.LUA_CONVERTERS_CLASS;
import static net.wizardsoflua.annotation.processor.Constants.LUA_INSTANCE_TABLE_SUPERCLASS;
import static net.wizardsoflua.annotation.processor.Constants.LUA_TABLE_SUPERCLASS;
import static net.wizardsoflua.annotation.processor.generator.GeneratorUtils.createDelegatingGetter;
import static net.wizardsoflua.annotation.processor.generator.GeneratorUtils.createDelegatingSetter;
import static net.wizardsoflua.annotation.processor.generator.GeneratorUtils.createFunctionClass;
import static net.wizardsoflua.annotation.processor.table.GenerateLuaTableProcessor.GENERATED_ANNOTATION;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import net.sandius.rembulan.Table;
import net.wizardsoflua.annotation.processor.Utils;
import net.wizardsoflua.annotation.processor.model.ManualFunctionModel;
import net.wizardsoflua.annotation.processor.model.PropertyModel;
import net.wizardsoflua.annotation.processor.table.model.FunctionModel;
import net.wizardsoflua.annotation.processor.table.model.LuaTableModel;

public class LuaTableGenerator {
  private final LuaTableModel model;
  private final ProcessingEnvironment env;

  public LuaTableGenerator(LuaTableModel model, ProcessingEnvironment env) {
    this.model = requireNonNull(model, "model == null!");
    this.env = requireNonNull(env, "env == null!");
  }

  public JavaFile generate() {
    String packageName = model.getGeneratedPackageName();
    TypeSpec luaModuleType = createLuaTable();
    return JavaFile.builder(packageName, luaModuleType).build();
  }

  private TypeSpec createLuaTable() {
    TypeSpec.Builder luaModuleType = classBuilder(model.getGeneratedSimpleName())//
        .addAnnotation(GENERATED_ANNOTATION)//
        .addModifiers(Modifier.PUBLIC)//
        .addTypeVariable(createTypeVariableD())//
        .superclass(createSuperClassTypeName())//
        .addMethod(createConstructor())//
    ;
    for (PropertyModel property : model.getProperties()) {
      if (property.isReadable()) {
        luaModuleType.addMethod(createDelegatingGetter(property, "getDelegate()", env));
      }
      if (property.isWriteable()) {
        luaModuleType.addMethod(createDelegatingSetter(property, "getDelegate()"));
      }
    }
    for (FunctionModel function : model.getFunctions()) {
      TypeElement enclosingElement = function.getEnclosingElement();
      if (model.getSourceElement().equals(enclosingElement)) {
        luaModuleType.addType(createFunctionClass(function, "getDelegate()", null, env));
      } else {
        Types types = env.getTypeUtils();
        TypeMirror[] typeArgs = new TypeMirror[enclosingElement.getTypeParameters().size()];
        for (int i = 0; i < typeArgs.length; i++) {
          typeArgs[i] = types.getWildcardType(null, null);
        }
        TypeMirror delegateType = types.getDeclaredType(enclosingElement, typeArgs);
        luaModuleType.addType(createFunctionClass(function, "self", delegateType, env));
      }
    }
    return luaModuleType.build();
  }

  private TypeVariableName createTypeVariableD() {
    TypeName upperBound = model.getParameterizedSourceClassName();
    return TypeVariableName.get("D", upperBound);
  }

  private ParameterizedTypeName createSuperClassTypeName() {
    ClassName raw = model.getSuperTableClassName();
    TypeVariableName d = createTypeVariableD();
    return ParameterizedTypeName.get(raw, d);
  }

  private MethodSpec createConstructor() {
    TypeName delegate = createTypeVariableD();
    TypeName converters = LUA_CONVERTERS_CLASS;
    Builder constructor = constructorBuilder()//
        .addModifiers(Modifier.PUBLIC)//
        .addParameter(delegate, "delegate")//
    ;
    if (model.hasMetatable()) {
      constructor.addParameter(Table.class, "metatable");
    }
    constructor.addParameter(converters, "converters");
    ClassName superTableClassName = model.getSuperTableClassName();
    if (LUA_TABLE_SUPERCLASS.equals(superTableClassName)
        || LUA_INSTANCE_TABLE_SUPERCLASS.equals(superTableClassName)) {
      if (model.hasMetatable()) {
        constructor.addStatement("super(delegate, metatable, converters, $L)",
            model.isModifiable());
      } else {
        constructor.addStatement("super(delegate, converters, $L)", model.isModifiable());
      }
    } else {
      if (model.hasMetatable()) {
        constructor.addStatement("super(delegate, metatable, converters)");
      } else {
        constructor.addStatement("super(delegate, converters)");
      }
    }
    for (PropertyModel property : model.getProperties()) {
      String name = property.getName();
      String getterName = property.getGetterName();
      String setterName = property.getSetterName();
      if (property.isWriteable()) {
        constructor.addStatement("add($S, this::$L, this::$L)", name, getterName, setterName);
      } else {
        constructor.addStatement("addReadOnly($S, this::$L)", name, getterName);
      }
    }
    for (FunctionModel function : model.getFunctions()) {
      String Name = Utils.capitalize(function.getName());
      constructor.addStatement("addFunction(new $LFunction())", Name);
    }
    for (ManualFunctionModel function : model.getManualFunctions()) {
      String name = function.getName();
      TypeElement functionType = function.getFunctionType();
      if (functionType.getModifiers().contains(Modifier.STATIC)) {
        constructor.addStatement("addFunction($S, new $T(delegate))", name, functionType);
      } else {
        String functionTypeSimpleName = ClassName.get(functionType).simpleName();
        constructor.addStatement("addFunction($S, delegate.new $L())", name,
            functionTypeSimpleName);
      }
    }
    return constructor.build();
  }
}
