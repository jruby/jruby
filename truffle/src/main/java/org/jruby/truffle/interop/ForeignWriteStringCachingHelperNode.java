package org.jruby.truffle.interop;

/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringCachingGuards;
import org.jruby.truffle.language.RubyNode;

@ImportStatic(StringCachingGuards.class)
@NodeChildren({
        @NodeChild("receiver"),
        @NodeChild("name"),
        @NodeChild("value")
})
abstract class ForeignWriteStringCachingHelperNode extends RubyNode {

    public ForeignWriteStringCachingHelperNode(RubyContext context) {
        super(context, null);
    }

    public abstract Object executeStringCachingHelper(VirtualFrame frame, DynamicObject receiver,
                                                      Object name, Object value);

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
            Object value,
            @Cached("privatizeRope(name)") Rope cachedRope,
            @Cached("ropeToString(cachedRope)") String cachedString,
            @Cached("startsWithAt(cachedString)") boolean cachedStartsWithAt,
            @Cached("createNextHelper()") ForeignWriteStringCachedHelperNode nextHelper) {
        return nextHelper.executeStringCachedHelper(frame, receiver, name, cachedString, cachedStartsWithAt, value);
    }

    @Specialization(
            guards = "isRubyString(name)",
            contains = "cacheStringAndForward"
    )
    public Object uncachedStringAndForward(
            VirtualFrame frame,
            DynamicObject receiver,
            DynamicObject name,
            Object value,
            @Cached("createNextHelper()") ForeignWriteStringCachedHelperNode nextHelper) {
        final String nameString = objectToString(name);
        return nextHelper.executeStringCachedHelper(frame, receiver, name, nameString,
                startsWithAt(nameString), value);
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
            Object value,
            @Cached("name") DynamicObject cachedName,
            @Cached("objectToString(cachedName)") String cachedString,
            @Cached("startsWithAt(cachedString)") boolean cachedStartsWithAt,
            @Cached("createNextHelper()") ForeignWriteStringCachedHelperNode nextHelper) {
        return nextHelper.executeStringCachedHelper(frame, receiver, cachedName, cachedString,
                cachedStartsWithAt, value);
    }

    @Specialization(
            guards = "isRubySymbol(name)",
            contains = "cacheSymbolAndForward"
    )
    public Object uncachedSymbolAndForward(
            VirtualFrame frame,
            DynamicObject receiver,
            DynamicObject name,
            Object value,
            @Cached("createNextHelper()") ForeignWriteStringCachedHelperNode nextHelper) {
        final String nameString = objectToString(name);
        return nextHelper.executeStringCachedHelper(frame, receiver, name, nameString,
                startsWithAt(nameString), value);
    }

    @Specialization(
            guards = "name == cachedName",
            limit = "getCacheLimit()"
    )
    public Object cacheJavaStringAndForward(
            VirtualFrame frame,
            DynamicObject receiver,
            String name,
            Object value,
            @Cached("name") String cachedName,
            @Cached("startsWithAt(cachedName)") boolean cachedStartsWithAt,
            @Cached("createNextHelper()") ForeignWriteStringCachedHelperNode nextHelper) {
        return nextHelper.executeStringCachedHelper(frame, receiver, cachedName, cachedName,
                cachedStartsWithAt, value);
    }

    @Specialization(contains = "cacheJavaStringAndForward")
    public Object uncachedJavaStringAndForward(
            VirtualFrame frame,
            DynamicObject receiver,
            String name,
            Object value,
            @Cached("createNextHelper()") ForeignWriteStringCachedHelperNode nextHelper) {
        return nextHelper.executeStringCachedHelper(frame, receiver, name, name, startsWithAt(name), value);
    }

    protected ForeignWriteStringCachedHelperNode createNextHelper() {
        return ForeignWriteStringCachedHelperNodeGen.create(null, null, null, null, null);
    }

    @CompilerDirectives.TruffleBoundary
    protected String objectToString(DynamicObject string) {
        return string.toString();
    }

    protected String ropeToString(Rope rope) {
        return RopeOperations.decodeRope(getContext().getJRubyRuntime(), rope);
    }

    @CompilerDirectives.TruffleBoundary
    protected boolean startsWithAt(String name) {
        return !name.isEmpty() && name.charAt(0) == '@';
    }

    protected int getCacheLimit() {
        return getContext().getOptions().INTEROP_WRITE_CACHE;
    }

}
