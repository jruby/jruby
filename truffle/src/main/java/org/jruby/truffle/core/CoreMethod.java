/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core;

import org.jruby.runtime.Visibility;
import org.jruby.truffle.language.methods.UnsupportedOperationBehavior;
import org.jruby.truffle.platform.UnsafeGroup;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CoreMethod {

    String[] names();

    // where the method is to be defined
    Visibility visibility() default Visibility.PUBLIC;

    /**
     * Defines the method on the singleton class.
     * {@link #needsSelf() needsSelf} is always false.
     * See {@link #constructor() constructor} if you need self.
     * */
    boolean onSingleton() default false;

    /**
     * Like {@link #onSingleton() onSingleton} but with {@link #needsSelf() needsSelf} always true.
     */
    boolean constructor() default false;

    /**
     * Defines the method as public on the singleton class
     * and as a private instance method.
     * {@link #needsSelf() needsSelf} is always false
     * as it could be either a module or any receiver.
     */
    boolean isModuleFunction() default false;

    boolean needsCallerFrame() default false;

    // arguments specification
    /** Whether <code>self</code> is passed as first argument to specializations. */
    boolean needsSelf() default true;

    int required() default 0;

    int optional() default 0;

    /**
     * Give the remaining arguments as a Object[] and allows unlimited arguments.
     */
    boolean rest() default false;

    boolean needsBlock() default false;

    // arguments transformation
    boolean lowerFixnumSelf() default false;

    int[] lowerFixnumParameters() default {};

    boolean taintFromSelf() default false;

    int taintFromParameter() default -1;

    boolean raiseIfFrozenSelf() default false;

    int[] raiseIfFrozenParameters() default {};

    // extra behavior
    UnsupportedOperationBehavior unsupportedOperationBehavior() default UnsupportedOperationBehavior.TYPE_ERROR;

    boolean returnsEnumeratorIfNoBlock() default false;

    UnsafeGroup[] unsafe() default {};

    boolean unsafeNeedsAudit() default false;

}
