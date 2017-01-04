/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jruby.truffle.Log;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.JavaException;

@NodeChildren({
        @NodeChild("receiver"),
        @NodeChild("args")
})
public abstract class OutgoingForeignCallNode extends RubyNode {

    @Child private RubyToForeignNode megamorphicToForeignNode;

    private final String name;

    public OutgoingForeignCallNode(String name) {
        this.name = name;
    }

    public abstract Object executeCall(VirtualFrame frame, TruffleObject receiver, Object[] args);

    @Specialization(
            guards = "args.length == cachedArgsLength",
            limit = "getCacheLimit()"
    )
    public Object callCached(
            VirtualFrame frame,
            TruffleObject receiver,
            Object[] args,
            @Cached("args.length") int cachedArgsLength,
            @Cached("createHelperNode(cachedArgsLength)") OutgoingNode outgoingNode,
            @Cached("createToForeignNodes(cachedArgsLength)") RubyToForeignNode[] toForeignNodes,
            @Cached("create()") ForeignToRubyNode toRubyNode) {
        Object[] foreignArgs = argsToForeign(frame, toForeignNodes, args);
        Object foreignValue = outgoingNode.executeCall(frame, receiver, foreignArgs);
        return toRubyNode.executeConvert(frame, foreignValue);
    }

    @Specialization(contains = "callCached")
    public Object callUncached(
            VirtualFrame frame,
            TruffleObject receiver,
            Object[] args,
            @Cached("create()") ForeignToRubyNode toRubyNode) {
        Log.notOptimizedOnce("megamorphic outgoing foreign call");

        if (megamorphicToForeignNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            megamorphicToForeignNode = insert(RubyToForeignNodeGen.create(null));
        }

        final Object[] foreignArgs = new Object[args.length];

        for (int n = 0; n < args.length; n++) {
            foreignArgs[n] = megamorphicToForeignNode.executeConvert(frame, args[n]);
        }

        Object foreignValue = createHelperNode(args.length).executeCall(frame, receiver, foreignArgs);
        return toRubyNode.executeConvert(frame, foreignValue);
    }

    @TruffleBoundary
    protected OutgoingNode createHelperNode(int argsLength) {
        if (name.equals("[]") && argsLength == 1) {
            return new IndexReadOutgoingNode();
        } else if (name.equals("[]=") && argsLength == 2) {
            return new IndexWriteOutgoingNode();
        } else if (name.endsWith("=") && argsLength == 1) {
            return new PropertyWriteOutgoingNode(name.substring(0, name.length() - 1));
        } else if (name.equals("call")) {
            return new CallOutgoingNode(argsLength);
        } else if (name.equals("nil?") && argsLength == 0) {
            return new IsNilOutgoingNode();
        } else if (name.endsWith("!") && argsLength == 0) {
            return new InvokeOutgoingNode(name.substring(0, name.length() - 1), argsLength);
        } else if (argsLength == 0) {
            return new PropertyReadOutgoingNode(name);
        } else {
            return new InvokeOutgoingNode(name, argsLength);
        }
    }

    protected RubyToForeignNode[] createToForeignNodes(int argsLength) {
        final RubyToForeignNode[] toForeignNodes = new RubyToForeignNode[argsLength];

        for (int n = 0; n < argsLength; n++) {
            toForeignNodes[n] = RubyToForeignNodeGen.create(null);
        }

        return toForeignNodes;
    }

    @ExplodeLoop
    protected Object[] argsToForeign(VirtualFrame frame, RubyToForeignNode[] toForeignNodes, Object[] args) {
        assert toForeignNodes.length == args.length;

        final Object[] foreignArgs = new Object[args.length];

        for (int n = 0; n < args.length; n++) {
            foreignArgs[n] = toForeignNodes[n].executeConvert(frame, args[n]);
        }

        return foreignArgs;
    }

    protected int getCacheLimit() {
        return getContext().getOptions().INTEROP_EXECUTE_CACHE;
    }

    protected abstract class OutgoingNode extends Node {

        private final BranchProfile exceptionProfile = BranchProfile.create();

        public abstract Object executeCall(VirtualFrame frame, TruffleObject receiver, Object[] args);

        protected void exceptionProfile() {
            exceptionProfile.enter();
        }

    }

    protected class IndexReadOutgoingNode extends OutgoingNode {

        @Child private Node node;

        public IndexReadOutgoingNode() {
            node = Message.READ.createNode();
        }

        @Override
        public Object executeCall(VirtualFrame frame, TruffleObject receiver, Object[] args) {
            assert args.length == 1;

            try {
                return ForeignAccess.sendRead(node, frame, receiver, args[0]);
            } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                exceptionProfile();
                throw new JavaException(e);
            }
        }

    }

    protected class IndexWriteOutgoingNode extends OutgoingNode {

        @Child private Node node;

        public IndexWriteOutgoingNode() {
            node = Message.WRITE.createNode();
        }

        @Override
        public Object executeCall(VirtualFrame frame, TruffleObject receiver, Object[] args) {
            assert args.length == 2;

            try {
                return ForeignAccess.sendWrite(node, frame, receiver, args[0], args[1]);
            } catch (UnknownIdentifierException | UnsupportedMessageException | UnsupportedTypeException e) {
                exceptionProfile();
                throw new JavaException(e);
            }
        }

    }

    protected class PropertyReadOutgoingNode extends OutgoingNode {

        private final String name;

        @Child private Node node;

        public PropertyReadOutgoingNode(String name) {
            this.name = name;
            node = Message.READ.createNode();
        }

        @Override
        public Object executeCall(VirtualFrame frame, TruffleObject receiver, Object[] args) {
            assert args.length == 0;

            try {
                return ForeignAccess.sendRead(node, frame, receiver, name);
            } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                exceptionProfile();
                throw new JavaException(e);
            }
        }

    }

    protected class PropertyWriteOutgoingNode extends OutgoingNode {

        private final String name;

        @Child private Node node;

        public PropertyWriteOutgoingNode(String name) {
            this.name = name;
            node = Message.WRITE.createNode();
        }

        @Override
        public Object executeCall(VirtualFrame frame, TruffleObject receiver, Object[] args) {
            assert args.length == 1;

            try {
                return ForeignAccess.sendWrite(node, frame, receiver, name, args[0]);
            } catch (UnknownIdentifierException | UnsupportedMessageException | UnsupportedTypeException e) {
                exceptionProfile();
                throw new JavaException(e);
            }
        }

    }

    protected class CallOutgoingNode extends OutgoingNode {

        private final int argsLength;

        @Child private Node node;

        public CallOutgoingNode(int argsLength) {
            this.argsLength = argsLength;
            node = Message.createExecute(argsLength).createNode();
        }

        @Override
        public Object executeCall(VirtualFrame frame, TruffleObject receiver, Object[] args) {
            assert args.length == argsLength;

            try {
                return ForeignAccess.sendExecute(node, frame, receiver, args);
            } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                exceptionProfile();
                throw new JavaException(e);
            }
        }

    }

    protected class IsNilOutgoingNode extends OutgoingNode {

        @Child private Node node;

        public IsNilOutgoingNode() {
            node = Message.IS_NULL.createNode();
        }

        @Override
        public Object executeCall(VirtualFrame frame, TruffleObject receiver, Object[] args) {
            assert args.length == 0;

            return ForeignAccess.sendIsNull(node, frame, receiver);
        }

    }

    protected class InvokeOutgoingNode extends OutgoingNode {

        private final String name;
        private final int argsLength;

        @Child private Node node;

        public InvokeOutgoingNode(String name, int argsLength) {
            this.name = name;
            this.argsLength = argsLength;
            node = Message.createInvoke(argsLength).createNode();
        }

        @Override
        public Object executeCall(VirtualFrame frame, TruffleObject receiver, Object[] args) {
            assert args.length == argsLength;

            try {
                return ForeignAccess.sendInvoke(node, frame, receiver, name, args);
            } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException | UnknownIdentifierException e) {
                exceptionProfile();
                throw new JavaException(e);
            }
        }

    }


}
