/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.builtins;

import org.jruby.truffle.platform.UnsafeGroup;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Primitive {

    String name();

    boolean needsSelf() default true;

    /**
     * Try to lower argument <code>i</code> (starting at 1) to an int if its value is a long.
     * Use 0 for <code>self</code>.
     */
    int[] lowerFixnum() default {};

    UnsafeGroup[] unsafe() default {};

}
