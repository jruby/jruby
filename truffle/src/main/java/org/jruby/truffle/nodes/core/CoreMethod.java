/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.methods.UnsupportedOperationBehavior;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CoreMethod {

    String[] names();

    Visibility visibility() default Visibility.PUBLIC;

    /**
     * Defines the method on the singleton class.
     * needsSelf is always false.
     * */
    boolean onSingleton() default false;

    /**
     * Defines the method as public on the singleton class
     * and as a private instance method.
     * needsSelf is always false.
     */
    boolean isModuleFunction() default false;

    boolean needsSelf() default true;

    // TODO CS 3-Mar-15 needsSelf() gets ignored in some cases - see CoreMethodNodeManager#addMethod
    boolean reallyDoesNeedSelf() default false;

    int required() default 0;

    int optional() default 0;

    /**
     * Give arguments as a Object[] and allows unlimited arguments.
     */
    boolean argumentsAsArray() default false;

    boolean needsBlock() default false;

    boolean lowerFixnumSelf() default false;

    int[] lowerFixnumParameters() default {};

    boolean taintFromSelf() default false;

    int taintFromParameter() default -1;

    boolean raiseIfFrozenSelf() default false;

    int[] raiseIfFrozenParameters() default {};

    UnsupportedOperationBehavior unsupportedOperationBehavior() default UnsupportedOperationBehavior.TYPE_ERROR;

    boolean returnsEnumeratorIfNoBlock() default false;

}
