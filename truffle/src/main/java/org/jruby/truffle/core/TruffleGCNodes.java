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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.dsl.Specialization;
import org.jruby.RubyGC;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;

@CoreClass("Truffle::GC")
public abstract class TruffleGCNodes {

    @CoreMethod(names = "count", onSingleton = true)
    public abstract static class CountNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public int count() {
            if (TruffleOptions.AOT) {
                throw new UnsupportedOperationException("Memory manager is not available with AOT.");
            }

            return RubyGC.getCollectionCount();
        }

    }

    @CoreMethod(names = "time", onSingleton = true)
    public abstract static class TimeNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public long time() {
            if (TruffleOptions.AOT) {
                throw new UnsupportedOperationException("Memory manager is not available with AOT.");
            }

            return RubyGC.getCollectionTime();
        }

    }

}
