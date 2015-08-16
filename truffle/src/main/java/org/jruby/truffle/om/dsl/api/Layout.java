/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.om.dsl.api;

import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.om.dsl.processor.OMProcessor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A {@link Layout} annotation is attached to an interface that describes an
 * object layout with properties. The interface has a factory method, guards,
 * getters and setters. From this a class is generated that implements these
 * methods and provides very efficient static access to these properties in a
 * {@link DynamicObject}.
 *
 * <pre>
 * @@Layout
 * public interface RectLayout {
 *    ...
 * }
 * </pre>
 *
 * The properties are defined by getters and setter method pairs. They should
 * both take a {@link DynamicObject}, and the setter should take a value. The
 * type of this value should match the type of the return value of the getter.
 * This defines an {@code int} property called {@code width}.
 *
 * <pre>
 * int getWidth(DynamicObject object);
 * void setWidth(DynamicObject object, int value);
 * </pre>
 *
 * A constructor accepts a value for each property, returning a new
 * {@link DynamicObject}. There should be as many parameters as there are
 * properties.
 *
 * <pre>
 * DynamicObject createWidget(int width, int height);
 * </pre>
 *
 * Guards can tell you if an object has this layout.
 *
 * <pre>
 * boolean isRect(DynamicObject object);
 * boolean isRect(Object object);
 * </pre>
 *
 * To access the implementation of the interface, use the {@code INSTANCE}
 * static final field of the generated {@code ...Impl} class.
 *
 * <pre>
 * RectLayout rectLayout = RectLayoutImpl.INSTANCE;
 * </pre>
 *
 * {@link Layout} annotations are processed by {@link OMProcessor}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Layout {
}
