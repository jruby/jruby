/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyObjectType;
import org.jruby.truffle.language.dispatch.DispatchAction;
import org.jruby.truffle.language.dispatch.DispatchHeadNode;
import org.jruby.truffle.language.dispatch.MissingBehavior;

@MessageResolution(
        receiverType = RubyObjectType.class,
        language = RubyLanguage.class
)
public class RubyMessageResolution {

    @CanResolve
    public abstract static class Check extends Node {

        protected static boolean test(TruffleObject receiver) {
            return RubyGuards.isRubyBasicObject(receiver);
        }

    }

    @Resolve(message = "EXECUTE")
    public static abstract class ForeignExecuteNode extends Node {

        @Child private Node findContextNode;
        @Child private ForeignExecuteHelperNode executeMethodNode;

        protected Object access(VirtualFrame frame, DynamicObject object, Object[] arguments) {
            return getHelperNode().executeCall(frame, object, arguments);
        }

        private ForeignExecuteHelperNode getHelperNode() {
            if (executeMethodNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                findContextNode = insert(RubyLanguage.INSTANCE.unprotectedCreateFindContextNode());
                final RubyContext context = RubyLanguage.INSTANCE.unprotectedFindContext(findContextNode);
                executeMethodNode = insert(ForeignExecuteHelperNodeGen.create( null, null));
            }

            return executeMethodNode;
        }

    }

    @Resolve(message = "GET_SIZE")
    public static abstract class ForeignGetSizeNode extends Node {

        @Child private Node findContextNode;
        @Child private DispatchHeadNode dispatchNode;

        protected Object access(VirtualFrame frame, DynamicObject object) {
            return getDispatchNode().dispatch(frame, object, "size", null, new Object[] {});
        }

        private DispatchHeadNode getDispatchNode() {
            if (dispatchNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                findContextNode = insert(RubyLanguage.INSTANCE.unprotectedCreateFindContextNode());
                final RubyContext context = RubyLanguage.INSTANCE.unprotectedFindContext(findContextNode);
                dispatchNode = insert(new DispatchHeadNode(true, false, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD));
            }

            return dispatchNode;
        }

    }

    @Resolve(message = "HAS_SIZE")
    public static abstract class ForeignHasSizeNode extends Node {

        protected Object access(DynamicObject object) {
            return RubyGuards.isRubyArray(object) || RubyGuards.isRubyHash(object) || RubyGuards.isRubyString(object);
        }

    }

    @Resolve(message = "INVOKE")
    public static abstract class ForeignInvokeNode extends Node {

        @Child private Node findContextNode;
        @Child private DispatchHeadNode dispatchHeadNode;

        protected Object access(VirtualFrame frame, DynamicObject receiver, String name, Object[] arguments) {
            return getDispatchHeadNode().dispatch(frame, receiver, name, null, arguments);
        }

        private DispatchHeadNode getDispatchHeadNode() {
            if (dispatchHeadNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                findContextNode = insert(RubyLanguage.INSTANCE.unprotectedCreateFindContextNode());
                final RubyContext context = RubyLanguage.INSTANCE.unprotectedFindContext(findContextNode);
                dispatchHeadNode = insert(new DispatchHeadNode(true, false, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD));
            }

            return dispatchHeadNode;
        }

    }

    @Resolve(message = "IS_BOXED")
    public static abstract class ForeignIsBoxedNode extends Node {

        protected Object access(DynamicObject object) {
            return RubyGuards.isRubyString(object) && StringOperations.rope(object).byteLength() == 1;
        }

    }

    @Resolve(message = "IS_EXECUTABLE")
    public static abstract class ForeignIsExecutableNode extends Node {

        protected Object access(VirtualFrame frame, DynamicObject object) {
            return RubyGuards.isRubyMethod(object) || RubyGuards.isRubyProc(object);
        }

    }

    @Resolve(message = "IS_NULL")
    public static abstract class ForeignIsNullNode extends Node {

        @Child private Node findContextNode;
        @CompilationFinal RubyContext context;

        protected Object access(DynamicObject object) {
            return object == getContext().getCoreLibrary().getNilObject();
        }

        private RubyContext getContext() {
            if (context == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                findContextNode = insert(RubyLanguage.INSTANCE.unprotectedCreateFindContextNode());
                context = RubyLanguage.INSTANCE.unprotectedFindContext(findContextNode);
            }

            return context;
        }

    }

    @Resolve(message = "READ")
    public static abstract class ForeignReadNode extends Node {

        @Child private Node findContextNode;
        @Child private ForeignReadStringCachingHelperNode helperNode;

        protected Object access(VirtualFrame frame, DynamicObject object, Object name) {
            return getHelperNode().executeStringCachingHelper(frame, object, name);
        }

        private ForeignReadStringCachingHelperNode getHelperNode() {
            if (helperNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                findContextNode = insert(RubyLanguage.INSTANCE.unprotectedCreateFindContextNode());
                helperNode = insert(ForeignReadStringCachingHelperNodeGen.create( null, null));
            }

            return helperNode;
        }

    }

    @Resolve(message = "UNBOX")
    public static abstract class ForeignUnboxNode extends Node {

        private final ConditionProfile stringProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile emptyProfile = ConditionProfile.createBinaryProfile();

        protected Object access(DynamicObject object) {
            if (stringProfile.profile(RubyGuards.isRubyString(object))) {
                final Rope rope = Layouts.STRING.getRope(object);

                if (emptyProfile.profile(rope.byteLength() == 0)) {
                    throw UnsupportedMessageException.raise(Message.UNBOX);
                } else {
                    return rope.get(0);
                }
            } else {
                return object;
            }
        }

    }

    @Resolve(message = "WRITE")
    public static abstract class ForeignWriteNode extends Node {

        @Child private Node findContextNode;
        @Child private ForeignWriteStringCachingHelperNode helperNode;

        protected Object access(VirtualFrame frame, DynamicObject object, Object name, Object value) {
            return getHelperNode().executeStringCachingHelper(frame, object, name, value);
        }

        private ForeignWriteStringCachingHelperNode getHelperNode() {
            if (helperNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                findContextNode = insert(RubyLanguage.INSTANCE.unprotectedCreateFindContextNode());
                helperNode = insert(ForeignWriteStringCachingHelperNodeGen.create( null, null, null));
            }

            return helperNode;
        }

    }

    @Resolve(message = "KEYS")
    public static abstract class ForeignKeysNode extends Node {

        @CompilationFinal private RubyContext context;

        @Child private Node findContextNode;
        @Child private DispatchHeadNode dispatchNode;

        protected Object access(VirtualFrame frame, DynamicObject object) {
            return getDispatchNode().dispatch(frame, context.getCoreLibrary().getTruffleInteropModule(), "ruby_object_keys", null, new Object[]{ object });
        }

        private DispatchHeadNode getDispatchNode() {
            if (dispatchNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                findContextNode = insert(RubyLanguage.INSTANCE.unprotectedCreateFindContextNode());
                context = RubyLanguage.INSTANCE.unprotectedFindContext(findContextNode);
                dispatchNode = insert(new DispatchHeadNode(true, false, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD));
            }

            return dispatchNode;
        }

    }

}
