/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.cext;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.language.objects.ReadObjectFieldNode;
import org.jruby.truffle.language.objects.ReadObjectFieldNodeGen;
import org.jruby.truffle.language.objects.WriteObjectFieldNode;
import org.jruby.truffle.language.objects.WriteObjectFieldNodeGen;

@MessageResolution(
        receiverType = DataAdapter.class,
        language = RubyLanguage.class
)
public class DataMessageResolution {

    @CanResolve
    public abstract static class TypedDataCheckNode extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof DataAdapter;
        }

    }

    // TODO CS 2-Oct-16 why do we get reads at index 16, and writes at index 2?

    @Resolve(message = "READ")
    public static abstract class TypedDataReadNode extends Node {

        @Child private ReadObjectFieldNode readDataNode;

        protected Object access(DataAdapter dataAdapter, long index) {
            if (index == 16) {
                if (readDataNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    readDataNode = insert(ReadObjectFieldNodeGen.create("@data", 0));
                }

                return readDataNode.execute(dataAdapter.getObject());
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new UnsupportedOperationException(String.format("Don't know what to do with a read from field %s in typed data", index));
            }
        }

    }

    @Resolve(message = "WRITE")
    public static abstract class TypedDataWriteNode extends Node {

        @Child private WriteObjectFieldNode writeDataNode;

        protected Object access(DataAdapter dataAdapter, int index, Object value) {
            if (index == 2) {
                if (writeDataNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    writeDataNode = insert(WriteObjectFieldNodeGen.create("@data"));
                }

                writeDataNode.execute(dataAdapter.getObject(), value);
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new UnsupportedOperationException(String.format("Don't know what to do with a write to field %s in typed data", index));
            }

            return value;
        }

    }

}
