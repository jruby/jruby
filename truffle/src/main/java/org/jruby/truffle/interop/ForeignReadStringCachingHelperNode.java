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
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringCachingGuards;
import org.jruby.truffle.language.RubyNode;

@ImportStatic(StringCachingGuards.class)
@NodeChildren({
        @NodeChild("receiver"),
        @NodeChild("name")
})
abstract class ForeignReadStringCachingHelperNode extends RubyNode {

    public ForeignReadStringCachingHelperNode(RubyContext context) {
        super(context, null);
    }

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
            @Cached("createNextHelper()") ForeignReadStringCachedHelperNode nextHelper) {
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
            @Cached("createNextHelper()") ForeignReadStringCachedHelperNode nextHelper) {
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
            @Cached("createNextHelper()") ForeignReadStringCachedHelperNode nextHelper) {
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
            @Cached("createNextHelper()") ForeignReadStringCachedHelperNode nextHelper) {
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
            @Cached("createNextHelper()") ForeignReadStringCachedHelperNode nextHelper) {
        return nextHelper.executeStringCachedHelper(frame, receiver, cachedName, cachedName, cachedStartsWithAt);
    }

    @Specialization(contains = "cacheJavaStringAndForward")
    public Object uncachedJavaStringAndForward(
            VirtualFrame frame,
            DynamicObject receiver,
            String name,
            @Cached("createNextHelper()") ForeignReadStringCachedHelperNode nextHelper) {
        return nextHelper.executeStringCachedHelper(frame, receiver, name, name, startsWithAt(name));
    }

    protected ForeignReadStringCachedHelperNode createNextHelper() {
        return ForeignReadStringCachedHelperNodeGen.create(null, null, null, null);
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
