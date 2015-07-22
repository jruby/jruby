/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import org.jruby.runtime.Visibility;
import org.jruby.truffle.runtime.methods.InternalMethod;

public interface MethodFilter {

    MethodFilter PUBLIC = new MethodFilter() {
        @Override
        public boolean filter(InternalMethod method) {
            return method.getVisibility() == Visibility.PUBLIC;
        }
    };

    MethodFilter PUBLIC_PROTECTED = new MethodFilter() {
        @Override
        public boolean filter(InternalMethod method) {
            return method.getVisibility() == Visibility.PUBLIC ||
                    method.getVisibility() == Visibility.PROTECTED;
        }
    };

    MethodFilter PROTECTED = new MethodFilter() {
        @Override
        public boolean filter(InternalMethod method) {
            return method.getVisibility() == Visibility.PROTECTED;
        }
    };

    MethodFilter PRIVATE = new MethodFilter() {
        @Override
        public boolean filter(InternalMethod method) {
            return method.getVisibility() == Visibility.PRIVATE;
        }
    };

    boolean filter(InternalMethod method);

}
