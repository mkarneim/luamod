package net.wizardsoflua.annotation.processor.doc.generator;

import static java.util.Objects.requireNonNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import com.google.common.base.Joiner;

import net.wizardsoflua.annotation.processor.doc.model.FunctionDocModel;
import net.wizardsoflua.annotation.processor.doc.model.LuaDocModel;
import net.wizardsoflua.annotation.processor.doc.model.PropertyDocModel;

public class LuaDocGenerator {
  private final LuaDocModel model;
  private final ProcessingEnvironment env;

  public LuaDocGenerator(LuaDocModel model, ProcessingEnvironment env) {
    this.model = requireNonNull(model, "model == null!");
    this.env = requireNonNull(env, "env == null!");
  }

  public String generate() {
    StringBuilder doc = new StringBuilder();
    doc.append("---\n");
    doc.append("name: ").append(model.getName()).append('\n');
    doc.append("subtitle: ").append(model.getSubtitle()).append('\n');
    doc.append("type: class\n");
    String superClass = model.getSuperClass();
    if (superClass != null) {
      doc.append("extends: ").append(superClass).append('\n');
    }
    doc.append("layout: module\n");
    doc.append("properties:\n");
    for (PropertyDocModel property : model.getProperties()) {
      doc.append("  - name: ").append(property.getName()).append('\n');
      doc.append("    type: ").append(property.getType()).append('\n');
      doc.append("    access: ").append(property.getAccess()).append('\n');
      doc.append("    description: ").append(property.getDescription()).append('\n');
    }
    doc.append("functions:\n");
    for (FunctionDocModel function : model.getFunctions()) {
      doc.append("  - name: ").append(function.getName()).append('\n');
      String args = Joiner.on(", ").join(function.getArgs());
      doc.append("    parameters: ").append(args).append('\n');
      doc.append("    results: ").append(function.getReturnType()).append('\n');
      doc.append("    description: ").append(function.getDescription()).append('\n');
    }
    doc.append("---\n");
    doc.append("\n");
    doc.append(model.getDescription()).append('\n');
    return doc.toString();
  }

  public static String getDescription(Element element, ProcessingEnvironment env) {
    Elements elements = env.getElementUtils();
    String docComment = elements.getDocComment(element);
    if (docComment == null) {
      return "";
    }
    return renderDescription(docComment);
  }

  public static String renderDescription(String description) {
    if (description.contains("\n")) {
      return '"' + description + '"';
    } else {
      return description;
    }
  }

  public String renderType(TypeMirror typeMirror) {
    return renderType(typeMirror, env);
  }

  public static String renderType(TypeMirror typeMirror, ProcessingEnvironment env) {
    TypeKind typeKind = typeMirror.getKind();
    switch (typeKind) {
      case ARRAY:
        return "table";
      case BOOLEAN:
        return "boolean";
      case BYTE:
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
      case SHORT:
        return "number (" + typeKind.toString().toLowerCase() + ")";
      case CHAR:
        return "string";
      case DECLARED:
        Elements elements = env.getElementUtils();
        Types types = env.getTypeUtils();
        TypeMirror stringType = elements.getTypeElement(String.class.getName()).asType();
        TypeMirror enumType = elements.getTypeElement(Enum.class.getName()).asType();
        if (types.isSameType(typeMirror, stringType) || types.isSubtype(typeMirror, enumType)) {
          return "string";
        }
        try {
          PrimitiveType primitiveType = types.unboxedType(typeMirror);
          return renderType(primitiveType, env);
        } catch (IllegalArgumentException ignore) {
        }
        return toReference(((DeclaredType) typeMirror).asElement().getSimpleName());
      case VOID:
        return "nil";
      case EXECUTABLE:
      case ERROR:
      case INTERSECTION:
      case NONE:
      case NULL:
      case OTHER:
      case PACKAGE:
      case TYPEVAR:
      case UNION:
      case WILDCARD:
      default:
        throw new IllegalArgumentException("Unknown type: " + typeMirror);
    }
  }

  public static String toReference(CharSequence simpleName) {
    return simpleName.length() == 0 ? ""
        : "[" + simpleName + "](!SITE_URL!/modules/" + simpleName + "/)";
  }
}
