/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.AcceptMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.Node.Child;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringCachingGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyObjectType;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.dispatch.DoesRespondDispatchHeadNode;
import org.jruby.truffle.language.objects.ReadObjectFieldNode;
import org.jruby.truffle.language.objects.ReadObjectFieldNodeGen;

@AcceptMessage(value = "READ", receiverType = RubyObjectType.class, language = RubyLanguage.class)
public final class ForeignReadNode extends ForeignReadBaseNode {

    @Child private Node findContextNode;
    @Child private StringCachingHelperNode helperNode;

    @Override
    public Object access(VirtualFrame frame, DynamicObject object, Object name) {
        return getHelperNode().executeStringCachingHelper(frame, object, name);
    }

    private StringCachingHelperNode getHelperNode() {
        if (helperNode == null) {
            CompilerDirectives.transferToInterpreter();
            findContextNode = insert(RubyLanguage.INSTANCE.unprotectedCreateFindContextNode());
            final RubyContext context = RubyLanguage.INSTANCE.unprotectedFindContext(findContextNode);
            helperNode = insert(ForeignReadNodeFactory.StringCachingHelperNodeGen.create(null, null));
        }

        return helperNode;
    }

    @ImportStatic(StringCachingGuards.class)
    @NodeChildren({
            @NodeChild("receiver"),
            @NodeChild("name")
    })
    protected static abstract class StringCachingHelperNode extends RubyNode {

        public abstract Object executeStringCachingHelper(VirtualFrame frame, DynamicObject receiver, Object name);

        @Specialization(
                guards = {
                        "isRubyString(name)",
                        "ropesEqual(name, cachedRope)"
                },
                limit = "getCacheLimit()"
        )
        public Object cacheStringAndForward(
                VirtualFrame frame,
                DynamicObject receiver,
                DynamicObject name,
                @Cached("privatizeRope(name)") Rope cachedRope,
                @Cached("ropeToString(cachedRope)") String cachedString,
                @Cached("startsWithAt(cachedString)") boolean cachedStartsWithAt,
                @Cached("createNextHelper()") StringCachedHelperNode nextHelper) {
            return nextHelper.executeStringCachedHelper(frame, receiver, name, cachedString, cachedStartsWithAt);
        }

        @Specialization(
                guards = "isRubyString(name)",
                contains = "cacheStringAndForward"
        )
        public Object uncachedStringAndForward(
                VirtualFrame frame,
                DynamicObject receiver,
                DynamicObject name,
                @Cached("createNextHelper()") StringCachedHelperNode nextHelper) {
            final String nameString = objectToString(name);
            return nextHelper.executeStringCachedHelper(frame, receiver, name, nameString, startsWithAt(nameString));
        }

        @Specialization(
                guards = {
                        "isRubySymbol(name)",
                        "name == cachedName"
                },
                limit = "getCacheLimit()"
        )
        public Object cacheSymbolAndForward(
                VirtualFrame frame,
                DynamicObject receiver,
                DynamicObject name,
                @Cached("name") DynamicObject cachedName,
                @Cached("objectToString(cachedName)") String cachedString,
                @Cached("startsWithAt(cachedString)") boolean cachedStartsWithAt,
                @Cached("createNextHelper()") StringCachedHelperNode nextHelper) {
            return nextHelper.executeStringCachedHelper(frame, receiver, cachedName, cachedString, cachedStartsWithAt);
        }

        @Specialization(
                guards = "isRubySymbol(name)",
                contains = "cacheSymbolAndForward"
        )
        public Object uncachedSymbolAndForward(
                VirtualFrame frame,
                DynamicObject receiver,
                DynamicObject name,
                @Cached("createNextHelper()") StringCachedHelperNode nextHelper) {
            final String nameString = objectToString(name);
            return nextHelper.executeStringCachedHelper(frame, receiver, name, nameString, startsWithAt(nameString));
        }

        @Specialization(
                guards = "name == cachedName",
                limit = "getCacheLimit()"
        )
        public Object cacheJavaStringAndForward(
                VirtualFrame frame,
                DynamicObject receiver,
                String name,
                @Cached("name") String cachedName,
                @Cached("startsWithAt(cachedName)") boolean cachedStartsWithAt,
                @Cached("createNextHelper()") StringCachedHelperNode nextHelper) {
            return nextHelper.executeStringCachedHelper(frame, receiver, cachedName, cachedName, cachedStartsWithAt);
        }

        @Specialization(contains = "cacheJavaStringAndForward")
        public Object uncachedJavaStringAndForward(
                VirtualFrame frame,
                DynamicObject receiver,
                String name,
                @Cached("createNextHelper()") StringCachedHelperNode nextHelper) {
            return nextHelper.executeStringCachedHelper(frame, receiver, name, name, startsWithAt(name));
        }

        protected StringCachedHelperNode createNextHelper() {
            return ForeignReadNodeFactory.StringCachedHelperNodeGen.create(null, null, null, null);
        }

        @TruffleBoundary
        protected String objectToString(DynamicObject string) {
            return string.toString();
        }

        protected String ropeToString(Rope rope) {
            return RopeOperations.decodeRope(getContext().getJRubyRuntime(), rope);
        }

        @TruffleBoundary
        protected boolean startsWithAt(String name) {
            return !name.isEmpty() && name.charAt(0) == '@';
        }

        @Specialization(guards = {
                "isRubyString(receiver)",
                "index < 0"
        })
        public int indexStringNegative(DynamicObject receiver, int index) {
            return 0;
        }

        @Specialization(guards = {
                "isRubyString(receiver)",
                "index >= 0",
                "!inRange(receiver, index)"
        })
        public int indexStringOutOfRange(DynamicObject receiver, int index) {
            return 0;
        }

        @Specialization(guards = {
                "isRubyString(receiver)",
                "index >= 0",
                "inRange(receiver, index)"
        })
        public int indexString(DynamicObject receiver, int index) {
            return Layouts.STRING.getRope(receiver).get(index);
        }

        protected boolean inRange(DynamicObject string, int index) {
            return index < Layouts.STRING.getRope(string).byteLength();
        }

        protected int getCacheLimit() {
            return getContext().getOptions().INTEROP_READ_CACHE;
        }

    }

    @NodeChildren({
            @NodeChild("receiver"),
            @NodeChild("name"),
            @NodeChild("stringName"),
            @NodeChild("startsAt")
    })
    protected static abstract class StringCachedHelperNode extends RubyNode {

        @Child private DoesRespondDispatchHeadNode definedNode;
        @Child private DoesRespondDispatchHeadNode indexDefinedNode;
        @Child private CallDispatchHeadNode callNode;

        protected final static String INDEX_METHOD_NAME = "[]";

        public abstract Object executeStringCachedHelper(VirtualFrame frame, DynamicObject receiver, Object name,
                                                         String stringName, boolean startsAt);

        @Specialization(guards = "startsAt(startsAt)")
        public Object readInstanceVariable(
                DynamicObject receiver,
                Object name,
                String stringName,
                boolean startsAt,
                @Cached("createReadObjectFieldNode(stringName)") ReadObjectFieldNode readObjectFieldNode) {
            return readObjectFieldNode.execute(receiver);
        }

        protected boolean startsAt(boolean startsAt) {
            return startsAt;
        }

        protected ReadObjectFieldNode createReadObjectFieldNode(String name) {
            return ReadObjectFieldNodeGen.create(name, nil());
        }

        @Specialization(
                guards = {
                        "notStartsAt(startsAt)",
                        "methodDefined(frame, receiver, stringName, getDefinedNode())"
                }
        )
        public Object callMethod(
                VirtualFrame frame,
                DynamicObject receiver,
                Object name,
                String stringName,
                boolean startsAt) {
            return getCallNode().call(frame, receiver, stringName, null);
        }

        @Specialization(
                guards = {
                        "notStartsAt(startsAt)",
                        "!methodDefined(frame, receiver, stringName, getDefinedNode())",
                        "methodDefined(frame, receiver, INDEX_METHOD_NAME, getIndexDefinedNode())"
                }
        )
        public Object index(
                VirtualFrame frame,
                DynamicObject receiver,
                Object name,
                String stringName,
                boolean startsAt) {
            return getCallNode().call(frame, receiver, "[]", null, name);
        }

        protected boolean notStartsAt(boolean startsAt) {
            return !startsAt;
        }

        protected DoesRespondDispatchHeadNode getDefinedNode() {
            if (definedNode == null) {
                CompilerDirectives.transferToInterpreter();
                definedNode = insert(new DoesRespondDispatchHeadNode(getContext(), true));
            }

            return definedNode;
        }

        protected DoesRespondDispatchHeadNode getIndexDefinedNode() {
            if (indexDefinedNode == null) {
                CompilerDirectives.transferToInterpreter();
                indexDefinedNode = insert(new DoesRespondDispatchHeadNode(getContext(), true));
            }

            return indexDefinedNode;
        }

        protected boolean methodDefined(VirtualFrame frame, DynamicObject receiver, String stringName,
                                        DoesRespondDispatchHeadNode definedNode) {
            return definedNode.doesRespondTo(frame, stringName, receiver);
        }

        protected CallDispatchHeadNode getCallNode() {
            if (callNode == null) {
                CompilerDirectives.transferToInterpreter();
                callNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
            }

            return callNode;
        }

    }

}
