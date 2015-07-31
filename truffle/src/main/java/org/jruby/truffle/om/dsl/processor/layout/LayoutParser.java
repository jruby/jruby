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

import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.om.dsl.api.Nullable;
import org.jruby.truffle.om.dsl.processor.layout.model.*;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LayoutParser {

    private LayoutModel superLayout;
    private String name;
    private String packageName;
    private String interfaceFullName;
    private boolean hasObjectGuard;
    private boolean hasDynamicObjectGuard;
    private final List<String> constructorProperties = new ArrayList<>();
    private final Map<String, PropertyBuilder> properties = new HashMap<>();

    public void parse(TypeElement layoutElement) {
        if (!layoutElement.getInterfaces().isEmpty()) {
            parseSuperLayout((TypeElement) ((DeclaredType) layoutElement.getInterfaces().get(0)).asElement());
        }

        parseName(layoutElement);

        for (Element element : layoutElement.getEnclosedElements()) {
            if (element.getKind() == ElementKind.FIELD) {
                final String simpleName = element.getSimpleName().toString();

                if (simpleName.endsWith("_IDENTIFIER")) {
                    parseIdentifier((VariableElement) element);
                } else {
                    throw new AssertionError("Unknown field in layout interface");
                }
            } else if (element.getKind() == ElementKind.METHOD) {
                final String simpleName = element.getSimpleName().toString();

                if (simpleName.equals("create" + name)) {
                    parseConstructor((ExecutableElement) element);
                } else if (simpleName.equals("is" + name)) {
                    parseGuard((ExecutableElement) element);
                } else if (simpleName.startsWith("get")) {
                    parseGetter((ExecutableElement) element);
                } else if (simpleName.startsWith("set")) {
                    parseSetter((ExecutableElement) element);
                } else {
                    throw new AssertionError("Unknown method in layout interface");
                }
            }
        }

        assert constructorProperties.size() == properties.size();
        assert constructorProperties.containsAll(properties.keySet());
    }

    private void parseIdentifier(VariableElement fieldElement) {
        final String name = fieldElement.getSimpleName().toString();
        assert name.endsWith("_IDENTIFIER");

        final String propertyName = NameUtils.constantToIdentifier(name.substring(0, name.length() - "_IDENTIFIER".length()));

        getProperty(propertyName).setHasIdentifier(true);
    }

    private void parseSuperLayout(TypeElement superTypeElement) {
        final LayoutParser superParser = new LayoutParser();
        superParser.parse(superTypeElement);

        superLayout = superParser.build();
    }

    private void parseName(TypeElement layoutElement) {
        parsePackageName(layoutElement);

        interfaceFullName = layoutElement.getQualifiedName().toString();

        final String nameString = layoutElement.getSimpleName().toString();
        assert nameString.endsWith("Layout");
        name = nameString.substring(0, nameString.length() - "Layout".length());
    }

    private void parseConstructor(ExecutableElement methodElement) {
        List<? extends VariableElement> parameters = methodElement.getParameters();

        if (superLayout != null) {
            assert parameters.size() >= superLayout.getAllProperties().size();
            parameters = parameters.subList(superLayout.getAllProperties().size(), parameters.size());
        }

        for (VariableElement element : parameters) {
            final String name = element.getSimpleName().toString();

            constructorProperties.add(name);
            final PropertyBuilder property = getProperty(name);
            parseNullable(property, element);
        }
    }

    private void parsePackageName(TypeElement layoutElement) {
        final String[] packageComponents = layoutElement.getQualifiedName().toString().split("\\.");

        final StringBuilder packageBuilder = new StringBuilder();

        for (int n = 0; n < packageComponents.length; n++) {
            if (Character.isUpperCase(packageComponents[n].charAt(0))) {
                break;
            }

            if (n > 0) {
                packageBuilder.append('.');
            }

            packageBuilder.append(packageComponents[n]);
        }

        packageName = packageBuilder.toString();
    }

    private void parseGuard(ExecutableElement methodElement) {
        assert methodElement.getParameters().size() == 1;

        final String type = methodElement.getParameters().get(0).asType().toString();
        assert type.equals(DynamicObject.class.getName()) || type.equals(Object.class.getName());

        assert methodElement.getParameters().get(0).getSimpleName().toString().equals("object");

        if (type.equals(DynamicObject.class.getName())) {
            assert !hasDynamicObjectGuard;
            hasDynamicObjectGuard = true;
        } else if (type.equals(Object.class.getName())) {
            assert !hasObjectGuard;
            hasObjectGuard = true;
        }
    }

    private void parseGetter(ExecutableElement methodElement) {
        assert methodElement.getSimpleName().toString().startsWith("get");
        assert methodElement.getParameters().size() == 1;
        assert methodElement.getParameters().get(0).asType().toString().equals(DynamicObject.class.toString());
        assert methodElement.getParameters().get(0).getSimpleName().toString().equals("object");

        final String name = titleToCamel(methodElement.getSimpleName().toString().substring("get".length()));
        final PropertyBuilder property = getProperty(name);
        property.setHasGetter(true);
        setPropertyType(property, methodElement.getReturnType());
        parseNullable(property, methodElement);
    }

    private void parseSetter(ExecutableElement methodElement) {
        assert methodElement.getSimpleName().toString().startsWith("set");
        assert methodElement.getParameters().size() == 2;
        assert methodElement.getParameters().get(0).asType().toString().equals(DynamicObject.class.toString());
        assert methodElement.getParameters().get(0).getSimpleName().toString().equals("object");
        assert methodElement.getParameters().get(1).getSimpleName().toString().equals("value");

        final String name = titleToCamel(methodElement.getSimpleName().toString().substring("get".length()));
        final PropertyBuilder property = getProperty(name);
        property.setHasSetter(true);
        setPropertyType(property, methodElement.getParameters().get(1).asType());
        parseNullable(property, methodElement);
    }

    private String titleToCamel(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private void parseNullable(PropertyBuilder property, Element element) {
        if (element.getAnnotation(Nullable.class) != null) {
            if (property.isNullable() == NullableState.DEFAULT) {
                property.setNullable(NullableState.NULLABLE);
            } else {
                assert property.isNullable() == NullableState.NULLABLE;
            }
        } else {
            if (property.isNullable() == NullableState.DEFAULT) {
                property.setNullable(NullableState.NOT_NULLABLE);
            } else {
                assert property.isNullable() == NullableState.NOT_NULLABLE;
            }
        }
    }

    private void setPropertyType(PropertyBuilder builder, TypeMirror type) {
        if (builder.getType() == null) {
            builder.setType(type);
        } else {
            assert builder.getType().equals(type);
        }
    }

    private PropertyBuilder getProperty(String name) {
        PropertyBuilder builder = properties.get(name);

        if (builder == null) {
            builder = new PropertyBuilder(name);
            properties.put(name, builder);
        }

        return builder;
    }

    public LayoutModel build() {
        return new LayoutModel(superLayout, name, packageName, hasObjectGuard, hasDynamicObjectGuard,
                buildProperties(), interfaceFullName);
    }

    private List<PropertyModel> buildProperties() {
        final List<PropertyModel> models = new ArrayList<>();

        for (String propertyName : constructorProperties) {
            models.add(getProperty(propertyName).build());
        }

        return models;
    }

}
