/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.arguments.CheckArityNode;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.methods.SymbolProcNode;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyEncoding;
import org.jruby.truffle.runtime.core.SymbolCodeRangeableWrapper;
import org.jruby.truffle.runtime.methods.Arity;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;
import org.jruby.truffle.runtime.object.BasicObjectType;
import org.jruby.util.ByteList;

import java.util.EnumSet;

@CoreClass(name = "Symbol")
public abstract class SymbolNodes {

    public static class SymbolType extends BasicObjectType {

    }

    public static final SymbolType SYMBOL_TYPE = new SymbolType();

    private static final HiddenKey STRING_IDENTIFIER = new HiddenKey("string");
    private static final Property STRING_PROPERTY;

    private static final HiddenKey BYTE_LIST_IDENTIFIER = new HiddenKey("byteList");
    private static final Property BYTE_LIST_PROPERTY;

    private static final HiddenKey HASH_CODE_IDENTIFIER = new HiddenKey("hashCode");
    private static final Property HASH_CODE_PROPERTY;

    private static final HiddenKey CODE_RANGE_IDENTIFIER = new HiddenKey("codeRange");
    private static final Property CODE_RANGE_PROPERTY;

    private static final HiddenKey CODE_RANGEABLE_WRAPPER_IDENTIFIER = new HiddenKey("codeRangeableWrapper");
    private static final Property CODE_RANGEABLE_WRAPPER_PROPERTY;

    public static final DynamicObjectFactory SYMBOL_FACTORY;

    static {
        final Shape.Allocator allocator = RubyBasicObject.LAYOUT.createAllocator();

        STRING_PROPERTY = Property.create(STRING_IDENTIFIER, allocator.locationForType(String.class, EnumSet.of(LocationModifier.NonNull, LocationModifier.Final)), 0);
        BYTE_LIST_PROPERTY = Property.create(BYTE_LIST_IDENTIFIER, allocator.locationForType(ByteList.class, EnumSet.of(LocationModifier.NonNull, LocationModifier.Final)), 0);
        HASH_CODE_PROPERTY = Property.create(HASH_CODE_IDENTIFIER, allocator.locationForType(int.class, EnumSet.of(LocationModifier.Final)), 0);
        CODE_RANGE_PROPERTY = Property.create(CODE_RANGE_IDENTIFIER, allocator.locationForType(int.class), 0);
        CODE_RANGEABLE_WRAPPER_PROPERTY = Property.create(CODE_RANGEABLE_WRAPPER_IDENTIFIER, allocator.locationForType(SymbolCodeRangeableWrapper.class), 0);

        final Shape shape = RubyBasicObject.LAYOUT.createShape(SYMBOL_TYPE)
            .addProperty(STRING_PROPERTY)
            .addProperty(BYTE_LIST_PROPERTY)
            .addProperty(HASH_CODE_PROPERTY)
            .addProperty(CODE_RANGE_PROPERTY)
            .addProperty(CODE_RANGEABLE_WRAPPER_PROPERTY);

        SYMBOL_FACTORY = shape.createFactory();
    }

    public static String getString(RubyBasicObject symbol) {
        assert RubyGuards.isRubySymbol(symbol);
        assert symbol.getDynamicObject().getShape().hasProperty(STRING_IDENTIFIER);

        return (String) STRING_PROPERTY.get(symbol.getDynamicObject(), true);
    }

    public static ByteList getByteList(RubyBasicObject symbol) {
        assert RubyGuards.isRubySymbol(symbol);
        assert symbol.getDynamicObject().getShape().hasProperty(BYTE_LIST_IDENTIFIER);

        return (ByteList) BYTE_LIST_PROPERTY.get(symbol.getDynamicObject(), true);
    }

    public static int getHashCode(RubyBasicObject symbol) {
        assert RubyGuards.isRubySymbol(symbol);
        assert symbol.getDynamicObject().getShape().hasProperty(HASH_CODE_IDENTIFIER);

        return (int) HASH_CODE_PROPERTY.get(symbol.getDynamicObject(), true);
    }

    public static int getCodeRange(RubyBasicObject symbol) {
        assert RubyGuards.isRubySymbol(symbol);
        assert symbol.getDynamicObject().getShape().hasProperty(CODE_RANGE_IDENTIFIER);

        return (int) CODE_RANGE_PROPERTY.get(symbol.getDynamicObject(), true);
    }

    public static void setCodeRange(RubyBasicObject symbol, int codeRange) {
        assert RubyGuards.isRubySymbol(symbol);
        assert symbol.getDynamicObject().getShape().hasProperty(CODE_RANGE_IDENTIFIER);

        try {
            CODE_RANGE_PROPERTY.set(symbol.getDynamicObject(), codeRange, symbol.getDynamicObject().getShape());
        } catch (IncompatibleLocationException | FinalLocationException e) {
            throw new UnsupportedOperationException();
        }
    }

    public static SymbolCodeRangeableWrapper getCodeRangeable(RubyBasicObject symbol) {
        assert RubyGuards.isRubySymbol(symbol);
        assert symbol.getDynamicObject().getShape().hasProperty(CODE_RANGEABLE_WRAPPER_IDENTIFIER);

        SymbolCodeRangeableWrapper wrapper = (SymbolCodeRangeableWrapper)
                CODE_RANGEABLE_WRAPPER_PROPERTY.get(symbol.getDynamicObject(), true);

        if (wrapper != null) {
            return wrapper;
        }

        wrapper = new SymbolCodeRangeableWrapper(symbol);

        try {
            CODE_RANGEABLE_WRAPPER_PROPERTY.set(symbol.getDynamicObject(), wrapper, symbol.getDynamicObject().getShape());
        } catch (IncompatibleLocationException | FinalLocationException e) {
            throw new UnsupportedOperationException();
        }

        return wrapper;
    }

    @CoreMethod(names = "all_symbols", onSingleton = true)
    public abstract static class AllSymbolsNode extends CoreMethodArrayArgumentsNode {

        public AllSymbolsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject allSymbols() {
            return createArray(getContext().getSymbolTable().allSymbols().toArray());
        }

    }

    @CoreMethod(names = { "==", "eql?" }, required = 1)
    public abstract static class EqualNode extends BinaryCoreMethodNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubySymbol(b)")
        public boolean equal(RubyBasicObject a, RubyBasicObject b) {
            return a == b;
        }

        @Specialization(guards = "!isRubySymbol(b)")
        public boolean equal(VirtualFrame frame, RubyBasicObject a, Object b) {
            return false;
        }

    }

    @CoreMethod(names = "encoding")
    public abstract static class EncodingNode extends CoreMethodArrayArgumentsNode {

        public EncodingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject encoding(RubyBasicObject symbol) {
            return EncodingNodes.getEncoding(getByteList(symbol).getEncoding());
        }

    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodArrayArgumentsNode {

        public HashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int hash(RubyBasicObject symbol) {
            return getHashCode(symbol);
        }

    }

    @CoreMethod(names = "to_proc")
    public abstract static class ToProcNode extends CoreMethodArrayArgumentsNode {

        public ToProcNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "cachedSymbol == symbol")
        public RubyBasicObject toProcCached(RubyBasicObject symbol,
                                     @Cached("symbol") RubyBasicObject cachedSymbol,
                                     @Cached("createProc(symbol)") RubyBasicObject cachedProc) {
            return cachedProc;
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject toProcUncached(RubyBasicObject symbol) {
            return createProc(symbol);
        }

        protected RubyBasicObject createProc(RubyBasicObject symbol) {
            final SourceSection sourceSection = RubyCallStack.getCallerFrame(getContext())
                    .getCallNode().getEncapsulatingSourceSection();

            final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(
                    sourceSection, null, Arity.NO_ARGUMENTS, getString(symbol),
                    true, null, false);

            final RubyRootNode rootNode = new RubyRootNode(
                    getContext(), sourceSection,
                    new FrameDescriptor(),
                    sharedMethodInfo,
                    SequenceNode.sequence(getContext(), sourceSection,
                            new CheckArityNode(getContext(), sourceSection, Arity.AT_LEAST_ONE),
                            new SymbolProcNode(getContext(), sourceSection, getString(symbol))));

            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);

            return ProcNodes.createRubyProc(
                    getContext().getCoreLibrary().getProcClass(),
                    ProcNodes.Type.PROC,
                    sharedMethodInfo,
                    callTarget, callTarget, callTarget,
                    null, null,
                    symbol.getContext().getCoreLibrary().getNilObject(),
                    null);
        }

    }

    @CoreMethod(names = "to_s")
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject toS(RubyBasicObject symbol) {
            return createString(getByteList(symbol).dup());
        }

    }

}
