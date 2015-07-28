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

        if (layout.getSuperLayout() != null) {
            stream.printf("import %s.%sLayoutImpl;\n", layout.getSuperLayout().getPackageName(), layout.getSuperLayout().getName());
        }

        stream.println();
        stream.printf("public class %sLayoutImpl", layout.getName());

        if (layout.getSuperLayout() != null) {
            stream.printf(" extends %sLayoutImpl", layout.getSuperLayout().getName());
        }

        stream.printf(" implements %sLayout {\n", layout.getName());

        stream.println("    ");
        stream.printf("    public static final %sLayout INSTANCE = new %sLayoutImpl();\n", layout.getName(), layout.getName());
        stream.println("    ");

        final String typeSuperclass;

        if (layout.getSuperLayout() == null) {
            typeSuperclass = "ObjectType";
        } else {
            typeSuperclass = layout.getSuperLayout().getName() + "LayoutImpl." + layout.getSuperLayout().getName() + "Type";
        }

        stream.printf("    protected static class %sType extends %s {\n", layout.getName(), typeSuperclass);
        stream.println("        ");
        stream.println("    }");
        stream.println("    ");
        stream.printf("    protected static final %sType %s_TYPE = new %sType();\n", layout.getName(), layout.getNameAsConstant(), layout.getName());
        stream.println("    ");

        if (layout.getSuperLayout() == null) {
            stream.println("    protected static final Layout LAYOUT = Layout.createLayout(Layout.INT_TO_LONG);");
            stream.println("    protected static final Shape.Allocator ALLOCATOR = LAYOUT.createAllocator();");
        }

        for (PropertyModel property : layout.getProperties()) {
            if (!property.hasIdentifier()) {
                stream.printf("    protected static final HiddenKey %s_IDENTIFIER = new HiddenKey(\"%s\");\n", property.getNameAsConstant(), property.getName());
            }

            stream.printf("    protected static final Property %s_PROPERTY;\n", property.getNameAsConstant());
            stream.println("    ");
        }


        stream.printf("    private static final DynamicObjectFactory %s_FACTORY;\n", layout.getNameAsConstant());
        stream.println("    ");
        stream.println("    static {");

        for (PropertyModel property : layout.getProperties()) {
            final List<String> modifiers = new ArrayList<>();

            if (!property.isNullable()) {
                modifiers.add("LocationModifier.NonNull");
            }

            if (!property.hasSetter()) {
                modifiers.add("LocationModifier.Final");
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

            stream.printf("        %S_PROPERTY = Property.create(%s_IDENTIFIER, ALLOCATOR.locationForType(%s.class%s), %s);\n",
                    property.getNameAsConstant(), property.getNameAsConstant(),
                    property.getType(),
                    modifiersExpression,
                    "0");
        }

        stream.println("        ");

        stream.printf("        final Shape shape = LAYOUT.createShape(%s_TYPE)\n", layout.getNameAsConstant());

        for (PropertyModel property : layout.getAllProperties()) {
            stream.printf("            .addProperty(%s_PROPERTY)\n", property.getNameAsConstant());
        }

        stream.println("                ;");

        stream.println("        ");
        stream.printf("        %s_FACTORY = shape.createFactory();\n", layout.getNameAsConstant());
        stream.println("    }");
        stream.println("    ");

        stream.printf("    protected %sLayoutImpl() {\n", layout.getName());
        stream.println("    }");
        stream.println("    ");


        stream.println("    @Override");
        stream.printf("    public DynamicObject create%s(", layout.getName());

        for (PropertyModel property : layout.getAllProperties()) {
            if (property != layout.getAllProperties().get(0)) {
                stream.print("            ");
            }

            stream.printf("%s %s", property.getType().toString(), property.getName());

            if (property == layout.getProperties().get(layout.getProperties().size() - 1)) {
                stream.println(") {");
            } else {
                stream.println(",");
            }
        }

        for (PropertyModel property : layout.getAllProperties()) {
            if (!property.getType().getKind().isPrimitive() && !property.isNullable()) {
                stream.printf("        assert %s != null;\n", property.getName());
            }
        }

        stream.printf("        final DynamicObject object = %s_FACTORY.newInstance(", layout.getNameAsConstant());

        for (PropertyModel property : layout.getAllProperties()) {
            if (property != layout.getAllProperties().get(0)) {
                stream.print("            ");
            }

            stream.printf("%s", property.getName());

            if (property == layout.getProperties().get(layout.getProperties().size() - 1)) {
                stream.println(");");
            } else {
                stream.println(",");
            }
        }

        stream.println("        return object;");

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
        stream.printf("        return object.getShape().getObjectType() instanceof %sType;\n", layout.getName());
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
