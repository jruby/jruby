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
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.methods.SymbolProcNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.Arity;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;
import org.jruby.truffle.runtime.object.BasicObjectType;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeable;
import org.jruby.util.StringSupport;

import java.util.EnumSet;

@CoreClass(name = "Symbol")
public abstract class SymbolNodes {

    public static class SymbolType extends BasicObjectType {

    }

    public static final SymbolType SYMBOL_TYPE = new SymbolType();

    private static final HiddenKey STRING_IDENTIFIER = new HiddenKey("string");
    private static final Property STRING_PROPERTY;

    private static final HiddenKey BYTES_IDENTIFIER = new HiddenKey("bytes");
    private static final Property BYTES_PROPERTY;

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
        BYTES_PROPERTY = Property.create(BYTES_IDENTIFIER, allocator.locationForType(ByteList.class, EnumSet.of(LocationModifier.NonNull, LocationModifier.Final)), 0);
        HASH_CODE_PROPERTY = Property.create(HASH_CODE_IDENTIFIER, allocator.locationForType(int.class, EnumSet.of(LocationModifier.NonNull, LocationModifier.Final)), 0);
        CODE_RANGE_PROPERTY = Property.create(CODE_RANGE_IDENTIFIER, allocator.locationForType(int.class, EnumSet.of(LocationModifier.NonNull)), 0);
        CODE_RANGEABLE_WRAPPER_PROPERTY = Property.create(CODE_RANGEABLE_WRAPPER_IDENTIFIER, allocator.locationForType(SymbolCodeRangeableWrapper.class), 0);

        final Shape shape = RubyBasicObject.LAYOUT.createShape(SYMBOL_TYPE)
            .addProperty(STRING_PROPERTY)
            .addProperty(BYTES_PROPERTY)
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
        assert symbol.getDynamicObject().getShape().hasProperty(BYTES_IDENTIFIER);

        return (ByteList) BYTES_PROPERTY.get(symbol.getDynamicObject(), true);
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

        wrapper = new SymbolCodeRangeableWrapper((RubySymbol) symbol);

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
            final RubyBasicObject array = createEmptyArray();

            for (RubyBasicObject s : getContext().getSymbolTable().allSymbols()) {
                ArrayNodes.slowPush(array, s);
            }
            return array;
        }

    }

    @CoreMethod(names = "encoding")
    public abstract static class EncodingNode extends CoreMethodArrayArgumentsNode {

        public EncodingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyEncoding encoding(RubySymbol symbol) {
            return RubyEncoding.getEncoding(getByteList(symbol).getEncoding());
        }

    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodArrayArgumentsNode {

        public HashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int hash(RubySymbol symbol) {
            return getHashCode(symbol);
        }

    }

    @CoreMethod(names = "to_proc")
    public abstract static class ToProcNode extends CoreMethodArrayArgumentsNode {

        public ToProcNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "cachedSymbol == symbol")
        public RubyProc toProcCached(RubySymbol symbol,
                                     @Cached("symbol") RubySymbol cachedSymbol,
                                     @Cached("createProc(symbol)") RubyProc cachedProc) {
            return cachedProc;
        }

        @TruffleBoundary
        @Specialization
        public RubyProc toProcUncached(RubySymbol symbol) {
            return createProc(symbol);
        }

        protected RubyProc createProc(RubySymbol symbol) {
            final SourceSection sourceSection = Truffle.getRuntime().getCallerFrame()
                    .getCallNode().getEncapsulatingSourceSection();

            final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(
                    sourceSection, null, Arity.NO_ARGUMENTS, getString(symbol),
                    true, null, false);

            final RubyRootNode rootNode = new RubyRootNode(
                    getContext(), sourceSection,
                    new FrameDescriptor(),
                    sharedMethodInfo,
                    new SymbolProcNode(getContext(), sourceSection, getString(symbol)));

            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);

            return new RubyProc(
                    getContext().getCoreLibrary().getProcClass(),
                    RubyProc.Type.PROC,
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
        public RubyBasicObject toS(RubySymbol symbol) {
            return createString(getByteList(symbol).dup());
        }

    }

}
