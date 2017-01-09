/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.stdlib;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.control.RaiseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@CoreClass("Truffle::Coverage")
public abstract class CoverageNodes {

    @CoreMethod(names = "enable", onSingleton = true)
    public abstract static class CoverageEnableNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject enable() {
            getContext().getCoverageManager().enable();
            return nil();
        }

    }

    @CoreMethod(names = "disable", onSingleton = true)
    public abstract static class CoverageDisableNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject disable() {
            getContext().getCoverageManager().disable();
            return nil();
        }

    }

    @CoreMethod(names = "result_array", onSingleton = true)
    public abstract static class CoverageResultNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject resultArray() {
            final List<DynamicObject> results = new ArrayList<>();

            final Map<Source, long[]> counts = getContext().getCoverageManager().getCounts();

            if (counts == null) {
                throw new RaiseException(coreExceptions().runtimeErrorCoverageNotEnabled(this));
            }

            for (Map.Entry<Source, long[]> source : counts.entrySet()) {
                final long[] countsArray = source.getValue();

                final Object[] countsStore = new Object[countsArray.length];

                for (int n = 0; n < countsArray.length; n++) {
                    countsStore[n] = countsArray[n];
                }

                results.add(createArray(new Object[] {
                        createString(StringOperations.encodeRope(source.getKey().getName(), UTF8Encoding.INSTANCE)),
                        createArray(countsStore, countsStore.length)
                }, 2));
            }

            return createArray(results.toArray(), results.size());
        }

    }

}
