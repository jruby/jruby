/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop.cext;

import static org.jruby.truffle.core.string.StringOperations.rope;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.core.rope.RopeNodes.GetByteNode;
import org.jruby.truffle.core.rope.RopeNodesFactory.GetByteNodeGen;
import org.jruby.truffle.core.string.StringNodes.SetByteNode;
import org.jruby.truffle.core.string.StringNodesFactory.SetByteNodeFactory;

@MessageResolution(
        receiverType = CExtString.class,
        language = RubyLanguage.class
)
public class CExtStringMessageResolution {

    @CanResolve
    public abstract static class Check extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof CExtString;
        }

    }

    @Resolve(message = "HAS_SIZE")
    public static abstract class ForeignHasSizeNode extends Node {

        protected Object access(CExtString object) {
            return true;
        }

    }

    @Resolve(message = "GET_SIZE")
    public static abstract class ForeignGetSizeNode extends Node {

        protected Object access(CExtString cExtString) {
            return rope(cExtString.getString()).byteLength();
        }

    }

    @Resolve(message = "READ")
    public static abstract class ForeignReadNode extends Node {

        @Child private GetByteNode getByteNode;

        protected Object access(CExtString cExtString, int index) {
            return getHelperNode().executeGetByte(rope(cExtString.getString()), index);
        }

        private GetByteNode getHelperNode() {
            if (getByteNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getByteNode = insert(GetByteNodeGen.create(null, null));
            }
            return getByteNode;
        }

    }

    @Resolve(message = "WRITE")
    public static abstract class ForeignWriteNode extends Node {

        @Child private Node findContextNode;
        @Child private SetByteNode setByteNode;

        protected Object access(CExtString cExtString, int index, Object value) {
            return getHelperNode().executeSetByte(cExtString.getString(), index, value);
        }

        private SetByteNode getHelperNode() {
            if (setByteNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                findContextNode = insert(RubyLanguage.INSTANCE.unprotectedCreateFindContextNode());
                RubyContext context = RubyLanguage.INSTANCE.unprotectedFindContext(findContextNode);
                setByteNode = insert(SetByteNodeFactory.create(context, null, null, null, null));
            }
            return setByteNode;
        }

    }

}
