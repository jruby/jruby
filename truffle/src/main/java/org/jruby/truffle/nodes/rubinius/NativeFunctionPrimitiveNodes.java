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

public abstract class NativeFunctionPrimitiveNodes {

    public static final int TYPE_CHAR = 0;
    public static final int TYPE_UCHAR = 1;
    public static final int TYPE_BOOL = 2;
    public static final int TYPE_SHORT = 3;
    public static final int TYPE_USHORT = 4;
    public static final int TYPE_INT = 5;
    public static final int TYPE_UINT = 6;
    public static final int TYPE_LONG = 7;
    public static final int TYPE_ULONG = 8;
    public static final int TYPE_LL = 9;
    public static final int TYPE_ULL = 10;
    public static final int TYPE_FLOAT = 11;
    public static final int TYPE_DOUBLE = 12;
    public static final int TYPE_PTR = 13;
    public static final int TYPE_VOID = 14;
    public static final int TYPE_STRING = 15;
    public static final int TYPE_STRPTR = 16;
    public static final int TYPE_CHARARR = 17;
    public static final int TYPE_ENUM = 18;
    public static final int TYPE_VARARGS = 19;

    @RubiniusPrimitive(name = "nativefunction_type_size", needsSelf = false)
    public static abstract class NativeFunctionTypeSizePrimitiveNode extends RubiniusPrimitiveNode {

        public NativeFunctionTypeSizePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long typeSize(int type) {
            switch (type) {
                case TYPE_CHAR:
                case TYPE_UCHAR:
                    return 1;

                case TYPE_SHORT:
                case TYPE_USHORT:
                    return 2;

                case TYPE_INT:
                case TYPE_UINT:
                    return 4;

                case TYPE_LONG:
                case TYPE_ULONG:
                    return 8;

                case TYPE_FLOAT:
                    return 4;

                case TYPE_DOUBLE:
                    return 8;

                case TYPE_PTR:
                case TYPE_STRPTR:
                    return 8;

                case TYPE_BOOL:
                case TYPE_LL:
                case TYPE_ULL:
                case TYPE_VOID:
                case TYPE_STRING:
                case TYPE_CHARARR:
                case TYPE_ENUM:
                case TYPE_VARARGS:
                default:
                    throw new UnsupportedOperationException();
            }
        }

    }

}
