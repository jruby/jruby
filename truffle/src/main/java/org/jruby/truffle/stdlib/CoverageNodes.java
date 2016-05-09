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
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.string.StringOperations;

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

            for (Map.Entry<Source, long[]> source : getContext().getCoverageManager().getCounts().entrySet()) {
                final long[] countsArray = source.getValue();

                final Object[] countsStore = new Object[countsArray.length];

                for (int n = 0; n < countsArray.length; n++) {
                    final Object countObject;

                    if (countsArray[n] == CoverageManager.NO_CODE) {
                        countObject = nil();
                    } else {
                        countObject = countsArray[n];
                    }

                    countsStore[n] = countObject;
                }

                results.add(Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), new Object[]{
                        createString(StringOperations.encodeRope(source.getKey().getPath(), UTF8Encoding.INSTANCE)),
                        Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), countsStore, countsStore.length)
                }, 2));
            }

            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), results.toArray(), results.size());
        }

    }

}
