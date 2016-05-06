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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.util.CaseInsensitiveBytesHash;
import org.jcodings.util.Hash;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.NonStandard;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;
import org.jruby.truffle.builtins.UnaryCoreMethodNode;
import org.jruby.truffle.core.cast.ToStrNode;
import org.jruby.truffle.core.cast.ToStrNodeGen;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.util.ByteList;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@CoreClass(name = "Encoding")
public abstract class EncodingNodes {

    // Both are mutated only in CoreLibrary.initializeEncodingConstants().
    private static final DynamicObject[] ENCODING_LIST = new DynamicObject[EncodingDB.getEncodings().size()];
    private static final Map<String, DynamicObject> LOOKUP = new HashMap<>();

    @TruffleBoundary
    public static synchronized DynamicObject getEncoding(Encoding encoding) {
        return LOOKUP.get(new String(encoding.getName(), StandardCharsets.UTF_8).toLowerCase(Locale.ENGLISH));
    }

    @TruffleBoundary
    public static DynamicObject getEncoding(String name) {
        return LOOKUP.get(name.toLowerCase(Locale.ENGLISH));
    }

    public static DynamicObject getEncoding(int index) {
        return ENCODING_LIST[index];
    }

    @TruffleBoundary
    public static void storeEncoding(int encodingListIndex, DynamicObject encoding) {
        assert RubyGuards.isRubyEncoding(encoding);
        ENCODING_LIST[encodingListIndex] = encoding;
        LOOKUP.put(Layouts.ENCODING.getName(encoding).toString().toLowerCase(Locale.ENGLISH), encoding);
    }

    @TruffleBoundary
    public static void storeAlias(String aliasName, DynamicObject encoding) {
        assert RubyGuards.isRubyEncoding(encoding);
        LOOKUP.put(aliasName.toLowerCase(Locale.ENGLISH), encoding);
    }

    public static DynamicObject newEncoding(DynamicObject encodingClass, Encoding encoding, byte[] name, int p, int end, boolean dummy) {
        return createRubyEncoding(encodingClass, encoding, new ByteList(name, p, end), dummy);
    }

    public static Object[] cloneEncodingList() {
        final Object[] clone = new Object[ENCODING_LIST.length];

        System.arraycopy(ENCODING_LIST, 0, clone, 0, ENCODING_LIST.length);

        return clone;
    }

    public static DynamicObject createRubyEncoding(DynamicObject encodingClass, Encoding encoding, ByteList name, boolean dummy) {
        return Layouts.ENCODING.createEncoding(Layouts.CLASS.getInstanceFactory(encodingClass), encoding, name, dummy);
    }

    @CoreMethod(names = "ascii_compatible?")
    public abstract static class AsciiCompatibleNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object isCompatible(DynamicObject encoding) {
            return EncodingOperations.getEncoding(encoding).isAsciiCompatible();
        }
    }

    @CoreMethod(names = "compatible?", needsSelf = false, onSingleton = true, required = 2)
    public abstract static class CompatibleQueryNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = {
                "isRubyString(first)",
                "isRubyString(second)",
                "firstEncoding == secondEncoding",
                "extractEncoding(first) == firstEncoding",
                "extractEncoding(second) == secondEncoding"
        }, limit = "getCacheLimit()")
        public DynamicObject isCompatibleStringStringCached(DynamicObject first, DynamicObject second,
                                                     @Cached("extractEncoding(first)") Encoding firstEncoding,
                                                     @Cached("extractEncoding(second)") Encoding secondEncoding,
                                                     @Cached("isCompatibleStringStringUncached(first, second)") DynamicObject rubyEncoding) {
            return rubyEncoding;
        }

        @Specialization(guards = {
                "isRubyString(first)", "isRubyString(second)"
        }, contains =  "isCompatibleStringStringCached")
        public DynamicObject isCompatibleStringStringUncached(DynamicObject first, DynamicObject second) {
            final Encoding compatibleEncoding = compatibleEncodingForStrings(first, second);

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyEncoding(first)", "isRubyEncoding(second)"})
        public Object isCompatibleEncodingEncoding(DynamicObject first, DynamicObject second) {
            final Encoding firstEncoding = EncodingOperations.getEncoding(first);
            final Encoding secondEncoding = EncodingOperations.getEncoding(second);
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(firstEncoding, secondEncoding);

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyString(first)", "isRubyRegexp(second)"})
        public Object isCompatibleStringRegexp(DynamicObject first, DynamicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(Layouts.STRING.getRope(first).getEncoding(), Layouts.REGEXP.getRegex(second).getEncoding());

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyRegexp(first)", "isRubyString(second)"})
        public Object isCompatibleRegexpString(DynamicObject first, DynamicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(Layouts.REGEXP.getRegex(first).getEncoding(), Layouts.STRING.getRope(second).getEncoding());

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyRegexp(first)", "isRubyRegexp(second)"})
        public Object isCompatibleRegexpRegexp(DynamicObject first, DynamicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(Layouts.REGEXP.getRegex(first).getEncoding(), Layouts.REGEXP.getRegex(second).getEncoding());

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyRegexp(first)", "isRubySymbol(second)"})
        public Object isCompatibleRegexpSymbol(DynamicObject first, DynamicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(Layouts.REGEXP.getRegex(first).getEncoding(), Layouts.SYMBOL.getRope(second).getEncoding());

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubySymbol(first)", "isRubyRegexp(second)"})
        public Object isCompatibleSymbolRegexp(DynamicObject first, DynamicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(Layouts.SYMBOL.getRope(first).getEncoding(), Layouts.REGEXP.getRegex(second).getEncoding());

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyString(first)", "isRubySymbol(second)"})
        public Object isCompatibleStringSymbol(DynamicObject first, DynamicObject second) {
            final Encoding compatibleEncoding = compatibleEncodingForRopes(StringOperations.rope(first), Layouts.SYMBOL.getRope(second));

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubySymbol(first)", "isRubySymbol(second)"})
        public Object isCompatibleSymbolSymbol(DynamicObject first, DynamicObject second) {
            final Encoding compatibleEncoding = compatibleEncodingForRopes(Layouts.SYMBOL.getRope(first), Layouts.SYMBOL.getRope(second));

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyString(first)", "isRubyEncoding(second)"})
        public Object isCompatibleStringEncoding(DynamicObject first, DynamicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(Layouts.STRING.getRope(first).getEncoding(), EncodingOperations.getEncoding(second));

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        public static Encoding compatibleEncodingForStrings(DynamicObject first, DynamicObject second) {
            // Taken from org.jruby.RubyEncoding#areCompatible.

            assert RubyGuards.isRubyString(first);
            assert RubyGuards.isRubyString(second);

            final Rope firstRope = StringOperations.rope(first);
            final Rope secondRope = StringOperations.rope(second);

            return compatibleEncodingForRopes(firstRope, secondRope);
        }

        @TruffleBoundary
        public static Encoding compatibleEncodingForRopes(Rope firstRope, Rope secondRope) {
            // Taken from org.jruby.RubyEncoding#areCompatible.

            final Encoding firstEncoding = firstRope.getEncoding();
            final Encoding secondEncoding = secondRope.getEncoding();

            if (firstEncoding == null || secondEncoding == null) return null;
            if (firstEncoding == secondEncoding) return firstEncoding;

            if (secondRope.isEmpty()) return firstEncoding;
            if (firstRope.isEmpty()) {
                return firstEncoding.isAsciiCompatible() && isAsciiOnly(secondRope) ? firstEncoding : secondEncoding;
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
        private static boolean isAsciiOnly(Rope rope) {
            return rope.getEncoding().isAsciiCompatible() && rope.getCodeRange() == CodeRange.CR_7BIT;
        }

        protected Encoding extractEncoding(DynamicObject string) {
            if (RubyGuards.isRubyString(string)) {
                return Layouts.STRING.getRope(string).getEncoding();
            }

            return null;
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
            final DynamicObject rubyEncoding = getEncoding(encodingString.toString());
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
                CompilerDirectives.transferToInterpreter();
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
                CompilerDirectives.transferToInterpreter();
                toStrNode = insert(ToStrNodeGen.create(getContext(), getSourceSection(), null));
            }

            final DynamicObject encodingName = toStrNode.executeToStr(frame, encoding);
            getContext().getJRubyRuntime().setDefaultInternalEncoding(EncodingOperations.getEncoding(getEncoding(encodingName.toString())));

            return encodingName;
        }

    }

    @CoreMethod(names = "list", onSingleton = true)
    public abstract static class ListNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject list() {
            final Object[] encodings = cloneEncodingList();

            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), encodings, encodings.length);
        }
    }


    @CoreMethod(names = "locale_charmap", onSingleton = true)
    public abstract static class LocaleCharacterMapNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject localeCharacterMap() {
            final ByteList name = new ByteList(getContext().getJRubyRuntime().getEncodingService().getLocaleEncoding().getName());
            return createString(name);
        }
    }

    @CoreMethod(names = "dummy?")
    public abstract static class DummyNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public boolean isDummy(DynamicObject encoding) {
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

            final DynamicObject[] encodings = ENCODING_LIST;
            for (int i = 0; i < encodings.length; i++) {
                final Object upcased = upcaseNode.call(frame, createString(Layouts.ENCODING.getName(encodings[i])), "upcase", null);
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

            final Encoding localeEncoding = getLocaleEncoding();
            final Object localeTuple = makeTuple(frame, newTupleNode, create7BitString("locale", UTF8Encoding.INSTANCE), indexLookup(encodings, localeEncoding));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("LOCALE"), localeTuple);

            final Encoding filesystemEncoding = getLocaleEncoding();
            final Object filesystemTuple = makeTuple(frame, newTupleNode, create7BitString("filesystem", UTF8Encoding.INSTANCE), indexLookup(encodings, filesystemEncoding));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("FILESYSTEM"), filesystemTuple);

            return ret;
        }

        private Object makeTuple(VirtualFrame frame, CallDispatchHeadNode newTupleNode, Object... values) {
            return newTupleNode.call(frame, coreLibrary().getTupleClass(), "create", null, values);
        }

        @TruffleBoundary
        private Encoding getLocaleEncoding() {
            return getContext().getJRubyRuntime().getEncodingService().getLocaleEncoding();
        }

        @TruffleBoundary
        public Object indexLookup(DynamicObject[] encodings, Encoding encoding) {
            // TODO (nirvdrum 25-Mar-15): Build up this lookup table in RubyEncoding as we register encodings.
            if (encoding == null) {
                return nil();
            }

            final ByteList encodingName = new ByteList(encoding.getName());

            for (int i = 0; i < encodings.length; i++) {
                if (Layouts.ENCODING.getName(encodings[i]).equals(encodingName)) {
                    return i;
                }
            }

            throw new UnsupportedOperationException(String.format("Could not find encoding %s in the registered encoding list", encoding.toString()));
        }
    }

    @CoreMethod(names = { "name", "to_s" })
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject toS(DynamicObject encoding) {
            final ByteList name = Layouts.ENCODING.getName(encoding).dup();
            name.setEncoding(ASCIIEncoding.INSTANCE);
            return createString(name);
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
            return EncodingNodes.getEncoding(Layouts.STRING.getRope(string).getEncoding());
        }

        @Specialization(guards = "isRubySymbol(symbol)")
        public DynamicObject encodingGetObjectEncodingSymbol(DynamicObject symbol) {
            return EncodingNodes.getEncoding(Layouts.SYMBOL.getRope(symbol).getEncoding());
        }

        @Specialization(guards = "isRubyEncoding(encoding)")
        public DynamicObject encodingGetObjectEncoding(DynamicObject encoding) {
            return encoding;
        }

        @Specialization(guards = "isRubyRegexp(regexp)")
        public DynamicObject encodingGetObjectEncodingRegexp(DynamicObject regexp) {
            return EncodingNodes.getEncoding(Layouts.REGEXP.getSource(regexp).getEncoding());
        }

        @Specialization(guards = {"!isRubyString(object)", "!isRubySymbol(object)", "!isRubyEncoding(object)", "!isRubyRegexp(object)"})
        public DynamicObject encodingGetObjectEncodingNil(DynamicObject object) {
            // TODO(CS, 26 Jan 15) something to do with __encoding__ here?
            return nil();
        }

    }

}
