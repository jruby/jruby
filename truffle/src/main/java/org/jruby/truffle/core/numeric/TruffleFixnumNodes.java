/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.numeric;

import com.oracle.truffle.api.dsl.Specialization;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.UnaryCoreMethodNode;
import org.jruby.truffle.core.CoreLibrary;

@CoreClass("Truffle::Fixnum")
public abstract class TruffleFixnumNodes {

    @CoreMethod(names = "lower", isModuleFunction = true, required = 1)
    public abstract static class LowerPrimitiveNode extends UnaryCoreMethodNode {

        @Specialization
        public int lower(int value) {
            return value;
        }

        @Specialization(guards = "canLower(value)")
        public int lower(long value) {
            return (int) value;
        }

        @Specialization(guards = "!canLower(value)")
        public long lowerFails(long value) {
            return value;
        }

        protected static boolean canLower(long value) {
            return CoreLibrary.fitsIntoInteger(value);
        }

    }

}
