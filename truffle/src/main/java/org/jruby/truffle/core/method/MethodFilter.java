/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.method;

import org.jruby.truffle.language.Visibility;
import org.jruby.truffle.language.methods.InternalMethod;

public abstract class MethodFilter {

    private MethodFilter() {
    }

    public static final MethodFilter PUBLIC = new MethodFilter() {
        @Override
        public boolean filter(InternalMethod method) {
            return method.getVisibility() == Visibility.PUBLIC;
        }
    };

    public static final MethodFilter PUBLIC_PROTECTED = new MethodFilter() {
        @Override
        public boolean filter(InternalMethod method) {
            return method.getVisibility() == Visibility.PUBLIC ||
                    method.getVisibility() == Visibility.PROTECTED;
        }
    };

    public static final MethodFilter PROTECTED = new MethodFilter() {
        @Override
        public boolean filter(InternalMethod method) {
            return method.getVisibility() == Visibility.PROTECTED;
        }
    };

    public static final MethodFilter PRIVATE = new MethodFilter() {
        @Override
        public boolean filter(InternalMethod method) {
            return method.getVisibility() == Visibility.PRIVATE;
        }
    };

    public abstract boolean filter(InternalMethod method);

    public static MethodFilter by(Visibility visibility) {
        switch (visibility) {
            case PUBLIC:
                return PUBLIC;
            case PROTECTED:
                return PROTECTED;
            case PRIVATE:
                return PRIVATE;
            default:
                throw new IllegalArgumentException("unsupported visibility: " + visibility);
        }
    }

}
