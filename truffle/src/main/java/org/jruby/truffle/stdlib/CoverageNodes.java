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
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.hash.BucketsStrategy;
import org.jruby.truffle.core.string.StringOperations;

import java.util.HashMap;
import java.util.Map;

@CoreClass(name = "Truffle::Coverage")
public abstract class CoverageNodes {

    @CoreMethod(names = "start", needsSelf = false)
    public abstract static class CoverageStartNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject coverageStart() {
            getContext().getCoverageManager().enable();
            return nil();
        }

    }

    @CoreMethod(names = "result", needsSelf = false)
    public abstract static class CoverageResultNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject coverageResult() {
            if (getContext().getCoverageManager() == null) {
                throw new UnsupportedOperationException("coverage is disabled");
            }

            final Map<Object, Object> converted = new HashMap<>();

            for (Map.Entry<Source, long[]> source : getContext().getCoverageManager().getCounts().entrySet()) {
                final Object[] store = lineCountsStore(source.getValue());
                final DynamicObject array = Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), store, store.length);

                if (source.getKey().getPath() != null) {
                    converted.put(createString(StringOperations.encodeRope(source.getKey().getPath(), UTF8Encoding.INSTANCE)), array);
                }
            }

            return BucketsStrategy.create(getContext(), converted.entrySet(), false);
        }

        private Object[] lineCountsStore(long[] array) {
            final Object[] store = new Object[array.length];

            for (int n = 0; n < array.length; n++) {
                if (array[n] == CoverageManager.NO_CODE) {
                    store[n] = nil();
                } else {
                    store[n] = array[n];
                }
            }

            return store;
        }

    }

}
