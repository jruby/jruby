/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
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
import org.jruby.RubyString;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.coerce.ToStrNode;
import org.jruby.truffle.nodes.coerce.ToStrNodeGen;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@CoreClass(name = "Encoding")
public abstract class EncodingNodes {

    // Both are mutated only in CoreLibrary.initializeEncodingConstants().
    private static DynamicObject[] encodingList = new DynamicObject[EncodingDB.getEncodings().size()];
    private static Map<String, DynamicObject> lookup = new HashMap<>();

    @TruffleBoundary
    public static synchronized DynamicObject getEncoding(Encoding encoding) {
        return lookup.get(new String(encoding.getName(), StandardCharsets.UTF_8).toLowerCase(Locale.ENGLISH));
    }

    @TruffleBoundary
    public static DynamicObject getEncoding(String name) {
        return lookup.get(name.toLowerCase(Locale.ENGLISH));
    }

    public static DynamicObject getEncoding(int index) {
        return encodingList[index];
    }

    @TruffleBoundary
    public static void storeEncoding(int encodingListIndex, DynamicObject encoding) {
        assert RubyGuards.isRubyEncoding(encoding);
        encodingList[encodingListIndex] = encoding;
        lookup.put(Layouts.ENCODING.getName(encoding).toString().toLowerCase(Locale.ENGLISH), encoding);
    }

    @TruffleBoundary
    public static void storeAlias(String aliasName, DynamicObject encoding) {
        assert RubyGuards.isRubyEncoding(encoding);
        lookup.put(aliasName.toLowerCase(Locale.ENGLISH), encoding);
    }

    public static DynamicObject newEncoding(DynamicObject encodingClass, Encoding encoding, byte[] name, int p, int end, boolean dummy) {
        return createRubyEncoding(encodingClass, encoding, new ByteList(name, p, end), dummy);
    }

    public static DynamicObject[] cloneEncodingList() {
        final DynamicObject[] clone = new DynamicObject[encodingList.length];

        System.arraycopy(encodingList, 0, clone, 0, encodingList.length);

        return clone;
    }

    public static DynamicObject createRubyEncoding(DynamicObject encodingClass, Encoding encoding, ByteList name, boolean dummy) {
        return Layouts.ENCODING.createEncoding(Layouts.CLASS.getInstanceFactory(encodingClass), encoding, name, dummy);
    }

    @CoreMethod(names = "ascii_compatible?")
    public abstract static class AsciiCompatibleNode extends CoreMethodArrayArgumentsNode {

        public AsciiCompatibleNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object isCompatible(DynamicObject encoding) {
            CompilerDirectives.transferToInterpreter();
            return Layouts.ENCODING.getEncoding(encoding).isAsciiCompatible();
        }
    }

    @CoreMethod(names = "compatible?", needsSelf = false, onSingleton = true, required = 2)
    public abstract static class CompatibleQueryNode extends CoreMethodArrayArgumentsNode {

        public CompatibleQueryNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyString(first)", "isRubyString(second)"})
        public Object isCompatibleStringString(DynamicObject first, DynamicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(StringNodes.getCodeRangeable(first), StringNodes.getCodeRangeable(second));

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyEncoding(first)", "isRubyEncoding(second)"})
        public Object isCompatibleEncodingEncoding(DynamicObject first, DynamicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(Layouts.ENCODING.getEncoding(first), Layouts.ENCODING.getEncoding(second));

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyString(first)", "isRubyRegexp(second)"})
        public Object isCompatibleStringRegexp(DynamicObject first, DynamicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(Layouts.STRING.getByteList(first).getEncoding(), Layouts.REGEXP.getRegex(second).getEncoding());

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyRegexp(first)", "isRubyString(second)"})
        public Object isCompatibleRegexpString(DynamicObject first, DynamicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(Layouts.REGEXP.getRegex(first).getEncoding(), Layouts.STRING.getByteList(second).getEncoding());

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
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(Layouts.REGEXP.getRegex(first).getEncoding(), Layouts.SYMBOL.getByteList(second).getEncoding());

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubySymbol(first)", "isRubyRegexp(second)"})
        public Object isCompatibleSymbolRegexp(DynamicObject first, DynamicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(Layouts.SYMBOL.getByteList(first).getEncoding(), Layouts.REGEXP.getRegex(second).getEncoding());

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyString(first)", "isRubySymbol(second)"})
        public Object isCompatibleStringSymbol(DynamicObject first, DynamicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(StringNodes.getCodeRangeable(first), SymbolNodes.getCodeRangeable(second));

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubySymbol(first)", "isRubySymbol(second)"})
        public Object isCompatibleSymbolSymbol(DynamicObject first, DynamicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(SymbolNodes.getCodeRangeable(first), SymbolNodes.getCodeRangeable(second));

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyString(first)", "isRubyEncoding(second)"})
        public Object isCompatibleStringEncoding(DynamicObject first, DynamicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(Layouts.STRING.getByteList(first).getEncoding(), Layouts.ENCODING.getEncoding(second));

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

    }

    @RubiniusOnly
    @CoreMethod(names = "default_external_jruby=", onSingleton = true, required = 1)
    public abstract static class SetDefaultExternalNode extends CoreMethodArrayArgumentsNode {

        @Child private ToStrNode toStrNode;

        public SetDefaultExternalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyEncoding(encoding)")
        public DynamicObject defaultExternalEncoding(DynamicObject encoding) {
            CompilerDirectives.transferToInterpreter();

            getContext().getRuntime().setDefaultExternalEncoding(Layouts.ENCODING.getEncoding(encoding));

            return encoding;
        }

        @Specialization(guards = "isRubyString(encodingString)")
        public DynamicObject defaultExternal(DynamicObject encodingString) {
            CompilerDirectives.transferToInterpreter();

            final DynamicObject rubyEncoding = getEncoding(encodingString.toString());
            getContext().getRuntime().setDefaultExternalEncoding(Layouts.ENCODING.getEncoding(rubyEncoding));

            return rubyEncoding;
        }

        @Specialization(guards = "isNil(nil)")
        public DynamicObject defaultExternal(Object nil) {
            throw new RaiseException(getContext().getCoreLibrary().argumentError("default external can not be nil", this));
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

    @RubiniusOnly
    @CoreMethod(names = "default_internal_jruby=", onSingleton = true, required = 1)
    public abstract static class SetDefaultInternalNode extends CoreMethodArrayArgumentsNode {

        @Child private ToStrNode toStrNode;

        public SetDefaultInternalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyEncoding(encoding)")
        public DynamicObject defaultInternal(DynamicObject encoding) {
            CompilerDirectives.transferToInterpreter();

            getContext().getRuntime().setDefaultInternalEncoding(Layouts.ENCODING.getEncoding(encoding));

            return encoding;
        }

        @Specialization(guards = "isNil(encoding)")
        public DynamicObject defaultInternal(Object encoding) {
            CompilerDirectives.transferToInterpreter();

            getContext().getRuntime().setDefaultInternalEncoding(null);

            return nil();
        }

        @Specialization(guards = { "!isRubyEncoding(encoding)", "!isNil(encoding)" })
        public DynamicObject defaultInternal(VirtualFrame frame, Object encoding) {
            CompilerDirectives.transferToInterpreter();

            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreter();
                toStrNode = insert(ToStrNodeGen.create(getContext(), getSourceSection(), null));
            }

            final DynamicObject encodingName = toStrNode.executeToStr(frame, encoding);
            getContext().getRuntime().setDefaultInternalEncoding(Layouts.ENCODING.getEncoding(getEncoding(encodingName.toString())));

            return encodingName;
        }

    }

    @CoreMethod(names = "list", onSingleton = true)
    public abstract static class ListNode extends CoreMethodArrayArgumentsNode {

        public ListNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject list() {
            CompilerDirectives.transferToInterpreter();

            final DynamicObject[] encodings = cloneEncodingList();

            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), encodings, encodings.length);
        }
    }


    @CoreMethod(names = "locale_charmap", onSingleton = true)
    public abstract static class LocaleCharacterMapNode extends CoreMethodArrayArgumentsNode {

        public LocaleCharacterMapNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject localeCharacterMap() {
            CompilerDirectives.transferToInterpreter();
            final ByteList name = new ByteList(getContext().getRuntime().getEncodingService().getLocaleEncoding().getName());
            return Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), name, StringSupport.CR_UNKNOWN, null);
        }
    }

    @CoreMethod(names = "dummy?")
    public abstract static class DummyNode extends CoreMethodArrayArgumentsNode {

        public DummyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean isDummy(DynamicObject encoding) {
            return Layouts.ENCODING.getDummy(encoding);
        }
    }

    @RubiniusOnly
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
            Object ret = newLookupTableNode.call(frame, getContext().getCoreLibrary().getLookupTableClass(), "new", null);

            final DynamicObject[] encodings = cloneEncodingList();
            for (int i = 0; i < encodings.length; i++) {
                final Object upcased = upcaseNode.call(frame, Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), Layouts.ENCODING.getName(encodings[i]), StringSupport.CR_UNKNOWN, null), "upcase", null);
                final Object key = toSymNode.call(frame, upcased, "to_sym", null);
                final Object value = newTupleNode.call(frame, getContext().getCoreLibrary().getTupleClass(), "create", null, nil(), i);

                lookupTableWriteNode.call(frame, ret, "[]=", null, key, value);
            }

            final Hash.HashEntryIterator i = getContext().getRuntime().getEncodingService().getAliases().entryIterator();
            while (i.hasNext()) {
                final CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<EncodingDB.Entry> e =
                        ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<EncodingDB.Entry>)i.next());

                final Object upcased = upcaseNode.call(frame, Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), new ByteList(e.bytes, e.p, e.end - e.p), StringSupport.CR_UNKNOWN, null), "upcase", null);
                final Object key = toSymNode.call(frame, upcased, "to_sym", null);
                final DynamicObject alias = Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), new ByteList(e.bytes, e.p, e.end - e.p), StringSupport.CR_UNKNOWN, null);
                final int index = e.value.getIndex();


                final Object value = newTupleNode.call(frame, getContext().getCoreLibrary().getTupleClass(), "create", null, alias, index);
                lookupTableWriteNode.call(frame, ret, "[]=", null, key, value);
            }

            final Encoding defaultInternalEncoding = getContext().getRuntime().getDefaultInternalEncoding();
            final Object internalTuple = getContext().makeTuple(frame, newTupleNode, Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), RubyString.encodeBytelist("internal", UTF8Encoding.INSTANCE), StringSupport.CR_UNKNOWN, null), indexLookup(encodings, defaultInternalEncoding));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("INTERNAL"), internalTuple);

            final Encoding defaultExternalEncoding = getContext().getRuntime().getDefaultExternalEncoding();
            final Object externalTuple = getContext().makeTuple(frame, newTupleNode, Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), RubyString.encodeBytelist("external", UTF8Encoding.INSTANCE), StringSupport.CR_UNKNOWN, null), indexLookup(encodings, defaultExternalEncoding));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("EXTERNAL"), externalTuple);

            final Encoding localeEncoding = getLocaleEncoding();
            final Object localeTuple = getContext().makeTuple(frame, newTupleNode, Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), RubyString.encodeBytelist("locale", UTF8Encoding.INSTANCE), StringSupport.CR_UNKNOWN, null), indexLookup(encodings, localeEncoding));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("LOCALE"), localeTuple);

            final Encoding filesystemEncoding = getLocaleEncoding();
            final Object filesystemTuple = getContext().makeTuple(frame, newTupleNode, Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), RubyString.encodeBytelist("filesystem", UTF8Encoding.INSTANCE), StringSupport.CR_UNKNOWN, null), indexLookup(encodings, filesystemEncoding));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("FILESYSTEM"), filesystemTuple);

            return ret;
        }

        @TruffleBoundary
        private Encoding getLocaleEncoding() {
            return getContext().getRuntime().getEncodingService().getLocaleEncoding();
        }

        @TruffleBoundary
        public Object indexLookup(DynamicObject[] encodings, Encoding encoding) {
            // TODO (nirvdrum 25-Mar-15): Build up this lookup table in RubyEncoding as we register encodings.
            if (encoding == null) {
                return nil();
            }

            for (int i = 0; i < encodings.length; i++) {
                if (Layouts.ENCODING.getEncoding(encodings[i]) == encoding) {
                    return i;
                }
            }

            throw new UnsupportedOperationException(String.format("Could not find encoding %s in the registered encoding list", encoding.toString()));
        }
    }

    @CoreMethod(names = { "name", "to_s" })
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject toS(DynamicObject encoding) {
            final ByteList name = Layouts.ENCODING.getName(encoding).dup();
            name.setEncoding(ASCIIEncoding.INSTANCE);
            return Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), name, StringSupport.CR_UNKNOWN, null);
        }
    }

}
