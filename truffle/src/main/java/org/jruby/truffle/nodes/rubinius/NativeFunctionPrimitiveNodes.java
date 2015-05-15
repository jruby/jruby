/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.subsystems.RubiniusConfiguration;

public abstract class NativeFunctionPrimitiveNodes {

    @RubiniusPrimitive(name = "nativefunction_type_size", needsSelf = false)
    public static abstract class NativeFunctionTypeSizePrimitiveNode extends RubiniusPrimitiveNode {

        public NativeFunctionTypeSizePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long typeSize(int type) {
            switch (type) {
                case RubiniusConfiguration.TYPE_CHAR:
                case RubiniusConfiguration.TYPE_UCHAR:
                    return 1;

                case RubiniusConfiguration.TYPE_SHORT:
                case RubiniusConfiguration.TYPE_USHORT:
                    return 2;

                case RubiniusConfiguration.TYPE_INT:
                case RubiniusConfiguration.TYPE_UINT:
                    return 4;

                case RubiniusConfiguration.TYPE_LONG:
                case RubiniusConfiguration.TYPE_ULONG:
                    return 8;

                case RubiniusConfiguration.TYPE_FLOAT:
                    return 4;

                case RubiniusConfiguration.TYPE_DOUBLE:
                    return 8;

                case RubiniusConfiguration.TYPE_PTR:
                case RubiniusConfiguration.TYPE_STRPTR:
                    return 8;

                case RubiniusConfiguration.TYPE_BOOL:
                case RubiniusConfiguration.TYPE_LL:
                case RubiniusConfiguration.TYPE_ULL:
                case RubiniusConfiguration.TYPE_VOID:
                case RubiniusConfiguration.TYPE_STRING:
                case RubiniusConfiguration.TYPE_CHARARR:
                case RubiniusConfiguration.TYPE_ENUM:
                case RubiniusConfiguration.TYPE_VARARGS:
                default:
                    throw new UnsupportedOperationException();
            }
        }

    }

}
