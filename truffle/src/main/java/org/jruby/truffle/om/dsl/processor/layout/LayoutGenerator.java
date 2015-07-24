/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.om.dsl.processor.layout;

import org.jruby.truffle.om.dsl.processor.layout.model.LayoutModel;
import org.jruby.truffle.om.dsl.processor.layout.model.PropertyModel;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class LayoutGenerator {

    private final LayoutModel layout;

    public LayoutGenerator(LayoutModel layout) {
        this.layout = layout;
    }

    public void generate(PrintStream stream) {
        stream.printf("package %s;\n", layout.getPackageName());
        stream.println();
        stream.println("import java.util.EnumSet;");
        stream.println("import com.oracle.truffle.api.object.*;");
        stream.println("import org.jruby.truffle.om.dsl.api.UnexpectedLayoutRefusalException;");
        stream.printf("import %s;\n", layout.getInterfaceFullName());
        stream.println();
        stream.printf("public class %sLayoutImpl implements %sLayout {\n", layout.getName(), layout.getName());
        stream.println("    ");
        stream.printf("    public static final %sLayout INSTANCE = new %sLayoutImpl();\n", layout.getName(), layout.getName());
        stream.println("    ");
        stream.printf("    private static class %sType extends ObjectType {\n", layout.getName());
        stream.println("        ");
        stream.println("    }");
        stream.println("    ");
        stream.printf("    private final %sType %s_TYPE = new %sType();\n", layout.getName(), layout.getNameAsConstant(), layout.getName());
        stream.println("    ");

        for (PropertyModel property : layout.getProperties()) {
            stream.printf("    private final HiddenKey %s_IDENTIFIER = new HiddenKey(\"%s\");\n", property.getNameAsConstant(), property.getName());
            stream.printf("    private final Property %s_PROPERTY;\n", property.getNameAsConstant());
            stream.println("    ");
        }

        stream.printf("    private final DynamicObjectFactory %s_FACTORY;\n", layout.getNameAsConstant());
        stream.println("    ");
        stream.printf("    private %sLayoutImpl() {\n", layout.getName());

        stream.println("        final Layout layout = Layout.createLayout(Layout.INT_TO_LONG);");
        stream.println("        final Shape.Allocator allocator = layout.createAllocator();");
        stream.println("        ");

        for (PropertyModel property : layout.getProperties()) {
            final List<String> modifiers = new ArrayList<>();

            if (!property.isNullable()) {
                modifiers.add("LocationModifier.NonNull");
            }

            final String modifiersExpression;

            if (modifiers.isEmpty()) {
                modifiersExpression = "";
            } else {
                final StringBuilder modifiersExpressionBuilder = new StringBuilder();
                modifiersExpressionBuilder.append(", EnumSet.of(");

                for (String modifier : modifiers) {
                    if (modifier != modifiers.get(0)) {
                        modifiersExpressionBuilder.append(", ");
                    }

                    modifiersExpressionBuilder.append(modifier);
                }

                modifiersExpressionBuilder.append(")");
                modifiersExpression  = modifiersExpressionBuilder.toString();
            }

            stream.printf("        %S_PROPERTY = Property.create(%s_IDENTIFIER, allocator.locationForType(%s.class%s), %s);\n",
                    property.getNameAsConstant(), property.getNameAsConstant(),
                    property.getType(),
                    modifiersExpression,
                    "0");
        }

        stream.println("        ");

        stream.printf("        final Shape shape = layout.createShape(%s_TYPE)\n", layout.getNameAsConstant());

        for (PropertyModel property : layout.getProperties()) {
            stream.printf("            .addProperty(%s_PROPERTY)", property.getNameAsConstant());

            if (property == layout.getProperties().get(layout.getProperties().size() - 1)) {
                stream.print(";");
            }

            stream.println();
        }

        stream.println("        ");
        stream.printf("        %s_FACTORY = shape.createFactory();\n", layout.getNameAsConstant());
        stream.println("    }");
        stream.println("    ");

        stream.println("    @Override");
        stream.printf("    public DynamicObject create%s(", layout.getName());

        for (PropertyModel property : layout.getProperties()) {
            if (property != layout.getProperties().get(0)) {
                stream.print("            ");
            }

            stream.printf("%s %s", property.getType().toString(), property.getName());

            if (property == layout.getProperties().get(layout.getProperties().size() - 1)) {
                stream.println(") {");
            } else {
                stream.println(",");
            }
        }

        for (PropertyModel property : layout.getProperties()) {
            if (!property.getType().getKind().isPrimitive() && !property.isNullable()) {
                stream.printf("        assert %s != null;\n", property.getName());
            }
        }

        stream.printf("        return %s_FACTORY.newInstance(", layout.getNameAsConstant());

        for (PropertyModel property : layout.getProperties()) {
            if (property != layout.getProperties().get(0)) {
                stream.print("            ");
            }

            stream.printf("%s", property.getName());

            if (property == layout.getProperties().get(layout.getProperties().size() - 1)) {
                stream.println(");");
            } else {
                stream.println(",");
            }
        }

        stream.println("    }");
        stream.println("    ");

        if (layout.hasObjectGuard()) {
            stream.println("    @Override");
            stream.printf("    public boolean is%s(Object object) {\n", layout.getName());
            stream.printf("        return (object instanceof DynamicObject) && is%s((DynamicObject) object);\n", layout.getName());
            stream.println("    }");
            stream.println("    ");
        }

        if (layout.hasDynamicObjectGuard()) {
            stream.println("    @Override");
            stream.print("    public");
        } else {
            stream.print("    private");
        }

        stream.printf(" boolean is%s(DynamicObject object) {\n", layout.getName());
        stream.printf("        return object.getShape().getObjectType() == %s_TYPE;\n", layout.getNameAsConstant());
        stream.println("    }");
        stream.println("    ");

        for (PropertyModel property : layout.getProperties()) {
            if (property.hasGetter()) {
                stream.println("    @Override");
                stream.printf("    public %s %s(DynamicObject object) {\n", property.getType(), property.getNameAsGetter());
                stream.printf("        assert is%s(object);\n", layout.getName());
                stream.printf("        assert object.getShape().hasProperty(%s_IDENTIFIER);\n", property.getNameAsConstant());
                stream.println("        ");
                stream.printf("        return (%s) %s_PROPERTY.get(object, true);\n", property.getType(), property.getNameAsConstant());
                stream.println("    }");
                stream.println("    ");
            }

            if (property.hasSetter()) {
                stream.println("    @Override");
                stream.printf("    public void %s(DynamicObject object, %s value) {\n", property.getNameAsSetter(), property.getType());
                stream.printf("        assert is%s(object);\n", layout.getName());
                stream.printf("        assert object.getShape().hasProperty(%s_IDENTIFIER);\n", property.getNameAsConstant());

                if (!property.getType().getKind().isPrimitive() && !property.isNullable()) {
                    stream.println("        assert value != null;");
                }

                stream.println("        ");
                stream.printf("        try {\n");
                stream.printf("            %s_PROPERTY.set(object, value, object.getShape());\n", property.getNameAsConstant());
                stream.printf("        } catch (IncompatibleLocationException | FinalLocationException e) {\n");
                stream.printf("            throw new UnexpectedLayoutRefusalException(e);\n");
                stream.printf("        }\n");
                stream.println("    }");
                stream.println("    ");
            }
        }

        stream.println("}");
    }

}
