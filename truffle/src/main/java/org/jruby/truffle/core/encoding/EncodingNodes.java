/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Contains code modified from JRuby's RubyConverter.java
 */
package org.jruby.truffle.core.encoding;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.util.CaseInsensitiveBytesHash;
import org.jcodings.util.Hash;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.NonStandard;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;
import org.jruby.truffle.builtins.UnaryCoreMethodNode;
import org.jruby.truffle.core.cast.ToEncodingNode;
import org.jruby.truffle.core.cast.ToStrNode;
import org.jruby.truffle.core.cast.ToStrNodeGen;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.util.ByteList;

@CoreClass("Encoding")
public abstract class EncodingNodes {

    @CoreMethod(names = "ascii_compatible?")
    public abstract static class AsciiCompatibleNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "encoding == cachedEncoding", limit = "getCacheLimit()")
        public boolean isAsciiCompatibleCached(DynamicObject encoding,
                                          @Cached("encoding") DynamicObject cachedEncoding,
                                          @Cached("isAsciiCompatible(cachedEncoding)") boolean isAsciiCompatible) {
            return isAsciiCompatible;
        }

        @Specialization(contains = "isAsciiCompatibleCached")
        public boolean isAsciiCompatibleUncached(DynamicObject encoding) {
            return isAsciiCompatible(encoding);
        }

        protected int getCacheLimit() {
            return getContext().getOptions().ENCODING_LOADED_CLASSES_CACHE;
        }

        protected static boolean isAsciiCompatible(DynamicObject encoding) {
            assert RubyGuards.isRubyEncoding(encoding);

            return EncodingOperations.getEncoding(encoding).isAsciiCompatible();
        }
    }

    @CoreMethod(names = "compatible?", needsSelf = false, onSingleton = true, required = 2)
    public abstract static class CompatibleQueryNode extends CoreMethodArrayArgumentsNode {

        @Child private ToEncodingNode toEncodingNode;

        public CompatibleQueryNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toEncodingNode = ToEncodingNode.create();
        }

        public abstract DynamicObject executeCompatibleQuery(Object string, Object other);

        @Specialization(guards = {
                "getEncoding(first) == getEncoding(second)",
                "getEncoding(first) == cachedEncoding",
        }, limit = "getCacheLimit()")
        public DynamicObject isCompatibleCached(Object first, Object second,
                                                @Cached("getEncoding(first)") Encoding cachedEncoding,
                                                @Cached("getRubyEncoding(cachedEncoding)") DynamicObject result) {
            return result;
        }

        @Specialization(guards = "getEncoding(first) == getEncoding(second)", contains = "isCompatibleCached")
        public DynamicObject isCompatibleUncached(Object first, Object second) {
            return getRubyEncoding(getEncoding(first));
        }

        @Specialization(guards = {
                "firstEncoding != secondEncoding",
                "isRubyString(first)",
                "isRubyString(second)",
                "isEmpty(first) == isFirstEmpty",
                "isEmpty(second) == isSecondEmpty",
                "getCodeRange(first) == firstCodeRange",
                "getCodeRange(second) == secondCodeRange",
                "getEncoding(first) == firstEncoding",
                "getEncoding(second) == secondEncoding"
        }, limit = "getCacheLimit()")
        public DynamicObject isCompatibleStringStringCached(DynamicObject first, DynamicObject second,
                                                     @Cached("getEncoding(first)") Encoding firstEncoding,
                                                     @Cached("getEncoding(second)") Encoding secondEncoding,
                                                     @Cached("isEmpty(first)") boolean isFirstEmpty,
                                                     @Cached("isEmpty(second)") boolean isSecondEmpty,
                                                     @Cached("getCodeRange(first)") CodeRange firstCodeRange,
                                                     @Cached("getCodeRange(second)") CodeRange secondCodeRange,
                                                     @Cached("isCompatibleStringStringUncached(first, second)") DynamicObject rubyEncoding) {
            return rubyEncoding;
        }

        @Specialization(guards = {
                "getEncoding(first) != getEncoding(second)",
                "isRubyString(first)",
                "isRubyString(second)",
        }, contains = "isCompatibleStringStringCached")
        public DynamicObject isCompatibleStringStringUncached(DynamicObject first, DynamicObject second) {
            final Encoding compatibleEncoding = compatibleEncodingForStrings(first, second);

            if (compatibleEncoding != null) {
                return getContext().getEncodingManager().getRubyEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @Specialization(guards = {
                "getEncoding(first) != getEncoding(second)",
                "isRubyString(first)",
                "!isRubyString(second)",
                "getCodeRange(first) == codeRange",
                "getEncoding(first) == firstEncoding",
                "getEncoding(second) == secondEncoding"
        }, limit = "getCacheLimit()")
        public DynamicObject isCompatibleStringObjectCached(DynamicObject first, Object second,
                                                            @Cached("getEncoding(first)") Encoding firstEncoding,
                                                            @Cached("getEncoding(second)") Encoding secondEncoding,
                                                            @Cached("getCodeRange(first)") CodeRange codeRange,
                                                            @Cached("isCompatibleStringObjectUncached(first, second)") DynamicObject result) {
            return result;
        }

        @Specialization(guards = {
                "getEncoding(first) != getEncoding(second)",
                "isRubyString(first)",
                "!isRubyString(second)"
        }, contains = "isCompatibleStringObjectCached")
        public DynamicObject isCompatibleStringObjectUncached(DynamicObject first, Object second) {
            final Encoding firstEncoding = getEncoding(first);
            final Encoding secondEncoding = getEncoding(second);

            if (secondEncoding == null) {
                return nil();
            }

            if (! firstEncoding.isAsciiCompatible() || ! secondEncoding.isAsciiCompatible()) {
                return nil();
            }

            if (secondEncoding == USASCIIEncoding.INSTANCE) {
                return getContext().getEncodingManager().getRubyEncoding(firstEncoding);
            }

            if (getCodeRange(first) == CodeRange.CR_7BIT) {
                return getContext().getEncodingManager().getRubyEncoding(secondEncoding);
            }

            return nil();
        }

        @Specialization(guards = {
                "getEncoding(first) != getEncoding(second)",
                "!isRubyString(first)",
                "isRubyString(second)"
        })
        public DynamicObject isCompatibleObjectString(Object first, DynamicObject second) {
            return isCompatibleStringObjectUncached(second, first);
        }

        @Specialization(guards = {
                "firstEncoding != secondEncoding",
                "!isRubyString(first)",
                "!isRubyString(second)",
                "firstEncoding != null",
                "secondEncoding != null",
                "getEncoding(first) == firstEncoding",
                "getEncoding(second) == secondEncoding",
        }, limit = "getCacheLimit()")
        public DynamicObject isCompatibleObjectObjectCached(Object first, Object second,
                                                 @Cached("getEncoding(first)") Encoding firstEncoding,
                                                 @Cached("getEncoding(second)") Encoding secondEncoding,
                                                 @Cached("getCompatibleEncoding(getContext(), firstEncoding, secondEncoding)") DynamicObject result) {

            return result;
        }

        @Specialization(guards = {
                "getEncoding(first) != getEncoding(second)",
                "!isRubyString(first)",
                "!isRubyString(second)"
        }, contains = "isCompatibleObjectObjectCached")
        public DynamicObject isCompatibleObjectObjectUncached(Object first, Object second) {
            final Encoding firstEncoding = getEncoding(first);
            final Encoding secondEncoding = getEncoding(second);

            return getCompatibleEncoding(getContext(), firstEncoding, secondEncoding);
        }

        private static Encoding compatibleEncodingForStrings(DynamicObject first, DynamicObject second) {
            // Taken from org.jruby.RubyEncoding#areCompatible.

            assert RubyGuards.isRubyString(first);
            assert RubyGuards.isRubyString(second);

            final Rope firstRope = StringOperations.rope(first);
            final Rope secondRope = StringOperations.rope(second);

            return compatibleEncodingForRopes(firstRope, secondRope);
        }

        @TruffleBoundary
        private static Encoding compatibleEncodingForRopes(Rope firstRope, Rope secondRope) {
            // Taken from org.jruby.RubyEncoding#areCompatible.

            final Encoding firstEncoding = firstRope.getEncoding();
            final Encoding secondEncoding = secondRope.getEncoding();

            if (secondRope.isEmpty()) return firstEncoding;
            if (firstRope.isEmpty()) {
                return firstEncoding.isAsciiCompatible() && (secondRope.getCodeRange() == CodeRange.CR_7BIT) ? firstEncoding : secondEncoding;
            }

            if (!firstEncoding.isAsciiCompatible() || !secondEncoding.isAsciiCompatible()) return null;

            if (firstRope.getCodeRange() != secondRope.getCodeRange()) {
                if (firstRope.getCodeRange() == CodeRange.CR_7BIT) return secondEncoding;
                if (secondRope.getCodeRange() == CodeRange.CR_7BIT) return firstEncoding;
            }
            if (secondRope.getCodeRange() == CodeRange.CR_7BIT) return firstEncoding;
            if (firstRope.getCodeRange() == CodeRange.CR_7BIT) return secondEncoding;

            return null;
        }

        @TruffleBoundary
        private static Encoding areCompatible(Encoding enc1, Encoding enc2) {
            assert enc1 != enc2;

            if (enc1 == null || enc2 == null) return null;

            if (!enc1.isAsciiCompatible() || !enc2.isAsciiCompatible()) return null;

            if (enc2 instanceof USASCIIEncoding) return enc1;
            if (enc1 instanceof USASCIIEncoding) return enc2;

            return null;
        }

        protected static DynamicObject getCompatibleEncoding(RubyContext context, Encoding first, Encoding second) {
            final Encoding compatibleEncoding = areCompatible(first, second);

            if (compatibleEncoding != null) {
                return context.getEncodingManager().getRubyEncoding(compatibleEncoding);
            } else {
                return context.getCoreLibrary().getNilObject();
            }
        }

        protected boolean isEmpty(Object string) {
            // The Truffle DSL generator will calculate @Cached values used in guards above all guards. In practice,
            // this guard is only used on Ruby strings, but the method must handle any object type because of the form
            // of the generated code. If the object is not a Ruby string, the resulting value is never used.
            if (RubyGuards.isRubyString(string)) {
                return StringOperations.rope((DynamicObject) string).isEmpty();
            }

            return false;
        }

        protected CodeRange getCodeRange(Object string) {
            // The Truffle DSL generator will calculate @Cached values used in guards above all guards. In practice,
            // this guard is only used on Ruby strings, but the method must handle any object type because of the form
            // of the generated code. If the object is not a Ruby string, the resulting value is never used.
            if (RubyGuards.isRubyString(string)) {
                return StringOperations.rope((DynamicObject) string).getCodeRange();
            }

            return CodeRange.CR_UNKNOWN;
        }

        protected Encoding getEncoding(Object value) {
            return toEncodingNode.executeToEncoding(value);
        }

        protected DynamicObject getRubyEncoding(Encoding value) {
            if (value == null) {
                return nil();
            }

            return getContext().getEncodingManager().getRubyEncoding(value);
        }

        protected int getCacheLimit() {
            return getContext().getOptions().ENCODING_COMPATIBILE_QUERY_CACHE;
        }

    }

    @NonStandard
    @CoreMethod(names = "default_external_jruby=", onSingleton = true, required = 1)
    public abstract static class SetDefaultExternalNode extends CoreMethodArrayArgumentsNode {

        @Child private ToStrNode toStrNode;

        @TruffleBoundary
        @Specialization(guards = "isRubyEncoding(encoding)")
        public DynamicObject defaultExternalEncoding(DynamicObject encoding) {
            getContext().getJRubyRuntime().setDefaultExternalEncoding(EncodingOperations.getEncoding(encoding));

            return encoding;
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(encodingString)")
        public DynamicObject defaultExternal(DynamicObject encodingString) {
            final DynamicObject rubyEncoding = getContext().getEncodingManager().getRubyEncoding(encodingString.toString());
            getContext().getJRubyRuntime().setDefaultExternalEncoding(EncodingOperations.getEncoding(rubyEncoding));

            return rubyEncoding;
        }

        @TruffleBoundary
        @Specialization(guards = "isNil(nil)")
        public DynamicObject defaultExternal(Object nil) {
            throw new RaiseException(coreExceptions().argumentError("default external can not be nil", this));
        }

        @Specialization(guards = { "!isRubyEncoding(encoding)", "!isRubyString(encoding)", "!isNil(encoding)" })
        public DynamicObject defaultExternal(VirtualFrame frame, Object encoding) {
            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStrNode = insert(ToStrNodeGen.create(getContext(), getSourceSection(), null));
            }

            return defaultExternal(toStrNode.executeToStr(frame, encoding));
        }

    }

    @NonStandard
    @CoreMethod(names = "default_internal_jruby=", onSingleton = true, required = 1)
    public abstract static class SetDefaultInternalNode extends CoreMethodArrayArgumentsNode {

        @Child private ToStrNode toStrNode;

        @TruffleBoundary
        @Specialization(guards = "isRubyEncoding(encoding)")
        public DynamicObject defaultInternal(DynamicObject encoding) {
            getContext().getJRubyRuntime().setDefaultInternalEncoding(EncodingOperations.getEncoding(encoding));

            return encoding;
        }

        @TruffleBoundary
        @Specialization(guards = "isNil(encoding)")
        public DynamicObject defaultInternal(Object encoding) {
            getContext().getJRubyRuntime().setDefaultInternalEncoding(null);

            return nil();
        }

        @Specialization(guards = { "!isRubyEncoding(encoding)", "!isNil(encoding)" })
        public DynamicObject defaultInternal(VirtualFrame frame, Object encoding) {
            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStrNode = insert(ToStrNodeGen.create(getContext(), getSourceSection(), null));
            }

            final DynamicObject encodingName = toStrNode.executeToStr(frame, encoding);
            getContext().getJRubyRuntime().setDefaultInternalEncoding(EncodingOperations.getEncoding(getContext().getEncodingManager().getRubyEncoding(encodingName.toString())));

            return encodingName;
        }

    }

    @CoreMethod(names = "list", onSingleton = true)
    public abstract static class ListNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject list() {
            final DynamicObject[] encodings = getContext().getEncodingManager().getUnsafeEncodingList();
            final Object[] arrayStore = new Object[encodings.length];

            System.arraycopy(encodings, 0, arrayStore, 0, encodings.length);

            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), arrayStore, arrayStore.length);
        }
    }


    @CoreMethod(names = "locale_charmap", onSingleton = true)
    public abstract static class LocaleCharacterMapNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject localeCharacterMap() {
            final DynamicObject rubyEncoding = getContext().getEncodingManager().getRubyEncoding(getContext().getEncodingManager().getLocaleEncoding());

            return Layouts.ENCODING.getName(rubyEncoding);
        }
    }

    @CoreMethod(names = "dummy?")
    public abstract static class DummyNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "encoding == cachedEncoding", limit = "getCacheLimit()")
        public boolean isDummyCached(DynamicObject encoding,
                                     @Cached("encoding") DynamicObject cachedEncoding,
                                     @Cached("isDummy(cachedEncoding)") boolean isDummy) {
            return isDummy;
        }

        @Specialization(contains = "isDummyCached")
        public boolean isDummyUncached(DynamicObject encoding) {
            return isDummy(encoding);
        }

        protected int getCacheLimit() {
            return getContext().getOptions().ENCODING_LOADED_CLASSES_CACHE;
        }

        protected static boolean isDummy(DynamicObject encoding) {
            assert RubyGuards.isRubyEncoding(encoding);

            return Layouts.ENCODING.getDummy(encoding);
        }
    }

    @NonStandard
    @CoreMethod(names = "encoding_map", onSingleton = true)
    public abstract static class EncodingMapNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode upcaseNode;
        @Child private CallDispatchHeadNode toSymNode;
        @Child private CallDispatchHeadNode newLookupTableNode;
        @Child private CallDispatchHeadNode lookupTableWriteNode;
        @Child private CallDispatchHeadNode newTupleNode;

        public EncodingMapNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            upcaseNode = DispatchHeadNodeFactory.createMethodCall(context);
            toSymNode = DispatchHeadNodeFactory.createMethodCall(context);
            newLookupTableNode = DispatchHeadNodeFactory.createMethodCall(context);
            lookupTableWriteNode = DispatchHeadNodeFactory.createMethodCall(context);
            newTupleNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public Object encodingMap(VirtualFrame frame) {
            Object ret = newLookupTableNode.call(frame, coreLibrary().getLookupTableClass(), "new", null);

            final DynamicObject[] encodings = getContext().getEncodingManager().getUnsafeEncodingList();
            for (int i = 0; i < encodings.length; i++) {
                final Object upcased = upcaseNode.call(frame, Layouts.ENCODING.getName(encodings[i]), "upcase", null);
                final Object key = toSymNode.call(frame, upcased, "to_sym", null);
                final Object value = newTupleNode.call(frame, coreLibrary().getTupleClass(), "create", null, nil(), i);

                lookupTableWriteNode.call(frame, ret, "[]=", null, key, value);
            }

            final Hash<EncodingDB.Entry>.HashEntryIterator i = EncodingDB.getAliases().entryIterator();
            while (i.hasNext()) {
                final CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<EncodingDB.Entry> e =
                        ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<EncodingDB.Entry>) i.next());

                final Object upcased = upcaseNode.call(frame, createString(new ByteList(e.bytes, e.p, e.end - e.p)), "upcase", null);
                final Object key = toSymNode.call(frame, upcased, "to_sym", null);
                final DynamicObject alias = createString(new ByteList(e.bytes, e.p, e.end - e.p));
                final int index = e.value.getIndex();


                final Object value = newTupleNode.call(frame, coreLibrary().getTupleClass(), "create", null, alias, index);
                lookupTableWriteNode.call(frame, ret, "[]=", null, key, value);
            }

            final Encoding defaultInternalEncoding = getContext().getJRubyRuntime().getDefaultInternalEncoding();
            final Object internalTuple = makeTuple(frame, newTupleNode, create7BitString("internal", UTF8Encoding.INSTANCE), indexLookup(encodings, defaultInternalEncoding));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("INTERNAL"), internalTuple);

            final Encoding defaultExternalEncoding = getContext().getJRubyRuntime().getDefaultExternalEncoding();
            final Object externalTuple = makeTuple(frame, newTupleNode, create7BitString("external", UTF8Encoding.INSTANCE), indexLookup(encodings, defaultExternalEncoding));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("EXTERNAL"), externalTuple);

            final Encoding localeEncoding = getContext().getEncodingManager().getLocaleEncoding();
            final Object localeTuple = makeTuple(frame, newTupleNode, create7BitString("locale", UTF8Encoding.INSTANCE), indexLookup(encodings, localeEncoding));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("LOCALE"), localeTuple);

            final Encoding filesystemEncoding = getContext().getEncodingManager().getLocaleEncoding();
            final Object filesystemTuple = makeTuple(frame, newTupleNode, create7BitString("filesystem", UTF8Encoding.INSTANCE), indexLookup(encodings, filesystemEncoding));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("FILESYSTEM"), filesystemTuple);

            return ret;
        }

        private Object makeTuple(VirtualFrame frame, CallDispatchHeadNode newTupleNode, Object... values) {
            return newTupleNode.call(frame, coreLibrary().getTupleClass(), "create", null, values);
        }

        @TruffleBoundary
        public Object indexLookup(DynamicObject[] encodings, Encoding encoding) {
            // TODO (nirvdrum 25-Mar-15): Build up this lookup table in RubyEncoding as we register encodings.
            if (encoding == null) {
                return nil();
            }

            final Rope encodingNameRope = RopeOperations.create(encoding.getName(), ASCIIEncoding.INSTANCE, CodeRange.CR_UNKNOWN);

            for (int i = 0; i < encodings.length; i++) {
                final Rope nameRope = StringOperations.rope(Layouts.ENCODING.getName(encodings[i]));
                if (nameRope.equals(encodingNameRope)) {
                    return i;
                }
            }

            throw new UnsupportedOperationException(String.format("Could not find encoding %s in the registered encoding list", encoding.toString()));
        }
    }

    @CoreMethod(names = { "name", "to_s" })
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject toS(DynamicObject encoding) {
            return Layouts.ENCODING.getName(encoding);
        }

    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends UnaryCoreMethodNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            throw new RaiseException(coreExceptions().typeErrorAllocatorUndefinedFor(rubyClass, this));
        }

    }

    @Primitive(name = "encoding_get_object_encoding", needsSelf = false)
    public static abstract class EncodingGetObjectEncodingNode extends PrimitiveArrayArgumentsNode {

        @Specialization(guards = "isRubyString(string)")
        public DynamicObject encodingGetObjectEncodingString(DynamicObject string) {
            return getContext().getEncodingManager().getRubyEncoding(Layouts.STRING.getRope(string).getEncoding());
        }

        @Specialization(guards = "isRubySymbol(symbol)")
        public DynamicObject encodingGetObjectEncodingSymbol(DynamicObject symbol) {
            return getContext().getEncodingManager().getRubyEncoding(Layouts.SYMBOL.getRope(symbol).getEncoding());
        }

        @Specialization(guards = "isRubyEncoding(encoding)")
        public DynamicObject encodingGetObjectEncoding(DynamicObject encoding) {
            return encoding;
        }

        @Specialization(guards = "isRubyRegexp(regexp)")
        public DynamicObject encodingGetObjectEncodingRegexp(DynamicObject regexp) {
            return getContext().getEncodingManager().getRubyEncoding(Layouts.REGEXP.getSource(regexp).getEncoding());
        }

        @Specialization(guards = {"!isRubyString(object)", "!isRubySymbol(object)", "!isRubyEncoding(object)", "!isRubyRegexp(object)"})
        public DynamicObject encodingGetObjectEncodingNil(DynamicObject object) {
            // TODO(CS, 26 Jan 15) something to do with __encoding__ here?
            return nil();
        }

    }

    @NodeChildren({ @NodeChild("first"), @NodeChild("second") })
    public static abstract class CheckEncodingNode extends RubyNode {

        @Child private EncodingNodes.CompatibleQueryNode compatibleQueryNode;
        @Child private ToEncodingNode toEncodingNode;

        public CheckEncodingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            compatibleQueryNode = EncodingNodesFactory.CompatibleQueryNodeFactory.create(context, sourceSection, new RubyNode[] {});
            toEncodingNode = ToEncodingNode.create();
        }

        public abstract Encoding executeCheckEncoding(Object first, Object second);

        @Specialization
        public Encoding checkEncoding(Object first, Object second,
                                      @Cached("create()") BranchProfile errorProfile) {
            final DynamicObject rubyEncoding = compatibleQueryNode.executeCompatibleQuery(first, second);

            if (rubyEncoding == nil()) {
                errorProfile.enter();
                throw new RaiseException(getContext().getCoreExceptions().encodingCompatibilityErrorIncompatible(
                        toEncodingNode.executeToEncoding(first), toEncodingNode.executeToEncoding(second), this));
            }

            return toEncodingNode.executeToEncoding(rubyEncoding);
        }

    }

}
