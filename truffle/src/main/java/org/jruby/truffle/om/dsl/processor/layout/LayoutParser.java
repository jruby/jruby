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
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.ObjectType;
import org.jruby.truffle.om.dsl.api.Layout;
import org.jruby.truffle.om.dsl.api.Nullable;
import org.jruby.truffle.om.dsl.api.Volatile;
import org.jruby.truffle.om.dsl.processor.layout.model.LayoutModel;
import org.jruby.truffle.om.dsl.processor.layout.model.NameUtils;
import org.jruby.truffle.om.dsl.processor.layout.model.PropertyBuilder;
import org.jruby.truffle.om.dsl.processor.layout.model.PropertyModel;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LayoutParser {

    private String objectTypeSuperclass;
    private LayoutModel superLayout;
    private String name;
    private String packageName;
    private String interfaceFullName;
    private boolean hasObjectTypeGuard;
    private boolean hasObjectGuard;
    private boolean hasDynamicObjectGuard;
    private boolean hasShapeProperties;
    private final List<String> constructorProperties = new ArrayList<>();
    private final Map<String, PropertyBuilder> properties = new HashMap<>();

    public void parse(TypeElement layoutElement) {
        if (!layoutElement.getInterfaces().isEmpty()) {
            parseSuperLayout((TypeElement) ((DeclaredType) layoutElement.getInterfaces().get(0)).asElement());
        }

        parseName(layoutElement);

        objectTypeSuperclass = layoutElement.getAnnotation(Layout.class).objectTypeSuperclass();

        for (Element element : layoutElement.getEnclosedElements()) {
            if (element.getKind() == ElementKind.FIELD) {
                final String simpleName = element.getSimpleName().toString();

                if (simpleName.endsWith("_IDENTIFIER")) {
                    parseIdentifier((VariableElement) element);
                } else {
                    // throw new AssertionError("Unknown field in layout interface");
                }
            } else if (element.getKind() == ElementKind.METHOD) {
                final String simpleName = element.getSimpleName().toString();

                if (simpleName.equals("create" + name + "Shape")) {
                    parseShapeConstructor((ExecutableElement) element);
                } else if (simpleName.equals("create" + name)) {
                    parseConstructor((ExecutableElement) element);
                } else if (simpleName.equals("is" + name)) {
                    parseGuard((ExecutableElement) element);
                } else if (simpleName.startsWith("getAndSet")) {
                    parseGetAndSet((ExecutableElement) element);
                } else if (simpleName.startsWith("compareAndSet")) {
                    parseCompareAndSet((ExecutableElement) element);
                } else if (simpleName.startsWith("get")) {
                    parseGetter((ExecutableElement) element);
                } else if (simpleName.startsWith("set")) {
                    parseSetter((ExecutableElement) element);
                } else {
                    // throw new AssertionError("Unknown method '" + simpleName + "' in layout interface " + interfaceFullName);
                }
            }
        }

        // assert constructorProperties.size() == properties.size();
        // assert constructorProperties.containsAll(properties.keySet());
    }

    private void parseIdentifier(VariableElement fieldElement) {
        final String name = fieldElement.getSimpleName().toString();
        // assert name.endsWith("_IDENTIFIER");

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
        // assert nameString.endsWith("Layout");
        name = nameString.substring(0, nameString.length() - "Layout".length());
    }

    private void parseShapeConstructor(ExecutableElement methodElement) {
        List<? extends VariableElement> parameters = methodElement.getParameters();

        if (superLayout != null) {
            // assert parameters.size() >= superLayout.getAllShapeProperties().size();
            parameters = parameters.subList(superLayout.getAllShapeProperties().size(), parameters.size());
        }

        for (VariableElement element : parameters) {
            final String name = element.getSimpleName().toString();

            constructorProperties.add(name);
            final PropertyBuilder property = getProperty(name);
            setPropertyType(property, element.asType());
            parseConstructorParameterAnnotations(property, element);
            property.setIsShapeProperty(true);
            hasShapeProperties = true;
        }
    }

    private void parseConstructor(ExecutableElement methodElement) {
        List<? extends VariableElement> parameters = methodElement.getParameters();

        if (hasShapeProperties || (superLayout != null && superLayout.hasShapeProperties())) {
            // assert parameters.get(0).asType().toString().equals(DynamicObjectFactory.class.getName()) : parameters.get(0).asType();
            parameters = parameters.subList(1, parameters.size());
        }

        if (superLayout != null) {
            // assert parameters.size() >= superLayout.getAllNonShapeProperties().size();
            parameters = parameters.subList(superLayout.getAllNonShapeProperties().size(), parameters.size());
        }

        for (VariableElement element : parameters) {
            final String name = element.getSimpleName().toString();

            constructorProperties.add(name);
            final PropertyBuilder property = getProperty(name);
            setPropertyType(property, element.asType());
            parseConstructorParameterAnnotations(property, element);
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
        // assert methodElement.getParameters().size() == 1;

        final String type = methodElement.getParameters().get(0).asType().toString();

        if (type.equals(DynamicObject.class.getName())) {
            // assert !hasDynamicObjectGuard;
            hasDynamicObjectGuard = true;
        } else if (type.equals(ObjectType.class.getName())) {
            // assert !hasObjectTypeGuard;
            hasObjectTypeGuard = true;
        } else if (type.equals(Object.class.getName())) {
            // assert !hasObjectGuard;
            hasObjectGuard = true;
        } else {
            // assert false : "Unknown type for the first guard parameter: " + type;
        }
    }

    private void parseGetter(ExecutableElement methodElement) {
        // assert methodElement.getSimpleName().toString().startsWith("get");
        // assert methodElement.getParameters().size() == 1;

        final boolean isFactoryGetter = methodElement.getParameters().get(0).asType().toString().equals(DynamicObjectFactory.class.getName());
        final boolean isObjectTypeGetter = methodElement.getParameters().get(0).asType().toString().equals(ObjectType.class.getName());

        // assert !(isFactoryGetter & isObjectTypeGetter);

        //if (isFactoryGetter) {
        //     assert methodElement.getParameters().get(0).getSimpleName().toString().equals("factory");
        //} else if (isObjectTypeGetter) {
        //     assert methodElement.getParameters().get(0).getSimpleName().toString().equals("objectType");
        //} else {
        //     assert methodElement.getParameters().get(0).asType().toString().equals(DynamicObject.class.getName());
        //     assert methodElement.getParameters().get(0).getSimpleName().toString().equals("object");
        //}

        final String name = titleToCamel(methodElement.getSimpleName().toString().substring("get".length()));
        final PropertyBuilder property = getProperty(name);

        if (isFactoryGetter) {
            property.setHasFactoryGetter(true);
        } else if (isObjectTypeGetter) {
            property.setHasObjectTypeGetter(true);
        } else {
            property.setHasGetter(true);
        }

        setPropertyType(property, methodElement.getReturnType());
    }

    private void parseSetter(ExecutableElement methodElement) {
        // assert methodElement.getSimpleName().toString().startsWith("set");
        // assert methodElement.getParameters().size() == 2;

        final boolean isFactorySetter = methodElement.getParameters().get(0).asType().toString().equals(DynamicObjectFactory.class.getName());
        final boolean isUnsafeSetter = methodElement.getSimpleName().toString().endsWith("Unsafe");

        // assert !(isFactorySetter && isUnsafeSetter);

        //if (isFactorySetter) {
        //    assert methodElement.getParameters().get(0).getSimpleName().toString().equals("factory");
        //} else {
        //    assert methodElement.getParameters().get(0).asType().toString().equals(DynamicObject.class.getName());
        //    assert methodElement.getParameters().get(0).getSimpleName().toString().equals("object");
        //}

        String name = titleToCamel(methodElement.getSimpleName().toString().substring("set".length()));

        if (isUnsafeSetter) {
            name = name.substring(0, name.length() - "Unsafe".length());
        }

        final PropertyBuilder property = getProperty(name);

        if (isFactorySetter) {
            property.setHasFactorySetter(true);
        } else {
            if (isUnsafeSetter) {
                property.setHasUnsafeSetter(isUnsafeSetter);
            } else {
                property.setHasSetter(true);
            }
        }

        setPropertyType(property, methodElement.getParameters().get(1).asType());
    }

    private void parseCompareAndSet(ExecutableElement methodElement) {
        // assert methodElement.getSimpleName().toString().startsWith("compareAndSet");
        // assert methodElement.getParameters().size() == 3;
        // assert methodElement.getParameters().get(0).asType().toString().equals(DynamicObject.class.getName());
        // assert methodElement.getParameters().get(0).getSimpleName().toString().equals("object");

        String name = titleToCamel(methodElement.getSimpleName().toString().substring("compareAndSet".length()));
        final PropertyBuilder property = getProperty(name);

        property.setHasCompareAndSet(true);

        setPropertyType(property, methodElement.getParameters().get(1).asType());
        setPropertyType(property, methodElement.getParameters().get(2).asType());
    }

    private void parseGetAndSet(ExecutableElement methodElement) {
        // assert methodElement.getSimpleName().toString().startsWith("getAndSet");
        // assert methodElement.getParameters().size() == 2;
        // assert methodElement.getParameters().get(0).asType().toString().equals(DynamicObject.class.getName());
        // assert methodElement.getParameters().get(0).getSimpleName().toString().equals("object");

        String name = titleToCamel(methodElement.getSimpleName().toString().substring("getAndSet".length()));
        final PropertyBuilder property = getProperty(name);

        property.setHasGetAndSet(true);

        setPropertyType(property, methodElement.getParameters().get(1).asType());
        setPropertyType(property, methodElement.getReturnType());
    }

    private String titleToCamel(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private void parseConstructorParameterAnnotations(PropertyBuilder property, Element element) {
        if (element.getAnnotation(Nullable.class) != null) {
            property.setNullable(true);
        }

        if (element.getAnnotation(Volatile.class) != null) {
            property.setVolatile(true);
        }
    }

    private void setPropertyType(PropertyBuilder builder, TypeMirror type) {
        if (builder.getType() == null) {
            builder.setType(type);
        } else {
            // assert builder.getType().toString().equals(type.toString());
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
        boolean hasShapeProperties = false;

        for (PropertyBuilder property : properties.values()) {
            if (property.isShapeProperty()) {
                hasShapeProperties = true;
            }
        }

        return new LayoutModel(objectTypeSuperclass, superLayout, name, packageName, hasObjectTypeGuard, hasObjectGuard, hasDynamicObjectGuard,
                buildProperties(), interfaceFullName, hasShapeProperties);
    }

    private List<PropertyModel> buildProperties() {
        final List<PropertyModel> models = new ArrayList<>();

        for (String propertyName : constructorProperties) {
            models.add(getProperty(propertyName).build());
        }

        return models;
    }

}
