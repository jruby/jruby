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
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.util.CaseInsensitiveBytesHash;
import org.jcodings.util.Hash;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.coerce.ToStrNode;
import org.jruby.truffle.nodes.coerce.ToStrNodeGen;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.om.dsl.api.Layout;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.util.ByteList;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@CoreClass(name = "Encoding")
public abstract class EncodingNodes {

    @Layout
    public interface EncodingLayout extends BasicObjectNodes.BasicObjectLayout {

        DynamicObjectFactory createEncodingShape(RubyBasicObject logicalClass, RubyBasicObject metaClass);

        DynamicObject createEncoding(DynamicObjectFactory factory, Encoding encoding, ByteList name, boolean dummy);

        boolean isEncoding(DynamicObject object);

        Encoding getEncoding(DynamicObject object);

        ByteList getName(DynamicObject object);

        boolean getDummy(DynamicObject object);

    }

    public static final EncodingLayout ENCODING_LAYOUT = EncodingLayoutImpl.INSTANCE;

    // Both are mutated only in CoreLibrary.initializeEncodingConstants().
    private static RubyBasicObject[] encodingList = new RubyBasicObject[EncodingDB.getEncodings().size()];
    private static Map<String, RubyBasicObject> lookup = new HashMap<>();

    @TruffleBoundary
    public static synchronized RubyBasicObject getEncoding(Encoding encoding) {
        return lookup.get(new String(encoding.getName(), StandardCharsets.UTF_8).toLowerCase(Locale.ENGLISH));
    }

    @TruffleBoundary
    public static RubyBasicObject getEncoding(String name) {
        return lookup.get(name.toLowerCase(Locale.ENGLISH));
    }

    public static RubyBasicObject getEncoding(int index) {
        return encodingList[index];
    }

    @TruffleBoundary
    public static void storeEncoding(int encodingListIndex, RubyBasicObject encoding) {
        assert RubyGuards.isRubyEncoding(encoding);
        encodingList[encodingListIndex] = encoding;
        lookup.put(getName(encoding).toString().toLowerCase(Locale.ENGLISH), encoding);
    }

    @TruffleBoundary
    public static void storeAlias(String aliasName, RubyBasicObject encoding) {
        assert RubyGuards.isRubyEncoding(encoding);
        lookup.put(aliasName.toLowerCase(Locale.ENGLISH), encoding);
    }

    public static RubyBasicObject newEncoding(RubyBasicObject encodingClass, Encoding encoding, byte[] name, int p, int end, boolean dummy) {
        return createRubyEncoding(encodingClass, encoding, new ByteList(name, p, end), dummy);
    }

    public static Encoding getEncoding(RubyBasicObject encoding) {
        return ENCODING_LAYOUT.getEncoding(BasicObjectNodes.getDynamicObject(encoding));
    }

    public static ByteList getName(RubyBasicObject encoding) {
        return ENCODING_LAYOUT.getName(BasicObjectNodes.getDynamicObject(encoding));
    }

    public static boolean isDummy(RubyBasicObject encoding) {
        return ENCODING_LAYOUT.getDummy(BasicObjectNodes.getDynamicObject(encoding));
    }

    public static RubyBasicObject[] cloneEncodingList() {
        final RubyBasicObject[] clone = new RubyBasicObject[encodingList.length];

        System.arraycopy(encodingList, 0, clone, 0, encodingList.length);

        return clone;
    }

    public static RubyBasicObject createRubyEncoding(RubyBasicObject encodingClass, Encoding encoding, ByteList name, boolean dummy) {
        return BasicObjectNodes.createRubyBasicObject(encodingClass, ENCODING_LAYOUT.createEncoding(ModuleNodes.getModel(encodingClass).factory, encoding, name, dummy));
    }

    @CoreMethod(names = "ascii_compatible?")
    public abstract static class AsciiCompatibleNode extends CoreMethodArrayArgumentsNode {

        public AsciiCompatibleNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object isCompatible(RubyBasicObject encoding) {
            CompilerDirectives.transferToInterpreter();
            return getEncoding(encoding).isAsciiCompatible();
        }
    }

    @CoreMethod(names = "compatible?", needsSelf = false, onSingleton = true, required = 2)
    public abstract static class CompatibleQueryNode extends CoreMethodArrayArgumentsNode {

        public CompatibleQueryNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyString(first)", "isRubyString(second)"})
        public Object isCompatibleStringString(RubyBasicObject first, RubyBasicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(StringNodes.getCodeRangeable(first), StringNodes.getCodeRangeable(second));

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyEncoding(first)", "isRubyEncoding(second)"})
        public Object isCompatibleEncodingEncoding(RubyBasicObject first, RubyBasicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(getEncoding(first), getEncoding(second));

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyString(first)", "isRubyRegexp(second)"})
        public Object isCompatibleStringRegexp(RubyBasicObject first, RubyBasicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(StringNodes.getByteList(first).getEncoding(), RegexpNodes.getRegex(second).getEncoding());

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyRegexp(first)", "isRubyString(second)"})
        public Object isCompatibleRegexpString(RubyBasicObject first, RubyBasicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(RegexpNodes.getRegex(first).getEncoding(), StringNodes.getByteList(second).getEncoding());

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyRegexp(first)", "isRubyRegexp(second)"})
        public Object isCompatibleRegexpRegexp(RubyBasicObject first, RubyBasicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(RegexpNodes.getRegex(first).getEncoding(), RegexpNodes.getRegex(second).getEncoding());

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyRegexp(first)", "isRubySymbol(second)"})
        public Object isCompatibleRegexpSymbol(RubyBasicObject first, RubyBasicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(RegexpNodes.getRegex(first).getEncoding(), SymbolNodes.getByteList(second).getEncoding());

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubySymbol(first)", "isRubyRegexp(second)"})
        public Object isCompatibleSymbolRegexp(RubyBasicObject first, RubyBasicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(SymbolNodes.getByteList(first).getEncoding(), RegexpNodes.getRegex(second).getEncoding());

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyString(first)", "isRubySymbol(second)"})
        public Object isCompatibleStringSymbol(RubyBasicObject first, RubyBasicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(StringNodes.getCodeRangeable(first), SymbolNodes.getCodeRangeable(second));

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubySymbol(first)", "isRubySymbol(second)"})
        public Object isCompatibleSymbolSymbol(RubyBasicObject first, RubyBasicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(SymbolNodes.getCodeRangeable(first), SymbolNodes.getCodeRangeable(second));

            if (compatibleEncoding != null) {
                return getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyString(first)", "isRubyEncoding(second)"})
        public Object isCompatibleStringEncoding(RubyBasicObject first, RubyBasicObject second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(StringNodes.getByteList(first).getEncoding(), getEncoding(second));

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
        public RubyBasicObject defaultExternalEncoding(RubyBasicObject encoding) {
            CompilerDirectives.transferToInterpreter();

            getContext().getRuntime().setDefaultExternalEncoding(getEncoding(encoding));

            return encoding;
        }

        @Specialization(guards = "isRubyString(encodingString)")
        public RubyBasicObject defaultExternal(RubyBasicObject encodingString) {
            CompilerDirectives.transferToInterpreter();

            final RubyBasicObject rubyEncoding = getEncoding(encodingString.toString());
            getContext().getRuntime().setDefaultExternalEncoding(getEncoding(rubyEncoding));

            return rubyEncoding;
        }

        @Specialization(guards = "isNil(nil)")
        public RubyBasicObject defaultExternal(Object nil) {
            throw new RaiseException(getContext().getCoreLibrary().argumentError("default external can not be nil", this));
        }

        @Specialization(guards = { "!isRubyEncoding(encoding)", "!isRubyString(encoding)", "!isNil(encoding)" })
        public RubyBasicObject defaultExternal(VirtualFrame frame, Object encoding) {
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
        public RubyBasicObject defaultInternal(RubyBasicObject encoding) {
            CompilerDirectives.transferToInterpreter();

            getContext().getRuntime().setDefaultInternalEncoding(getEncoding(encoding));

            return encoding;
        }

        @Specialization(guards = "isNil(encoding)")
        public RubyBasicObject defaultInternal(Object encoding) {
            CompilerDirectives.transferToInterpreter();

            getContext().getRuntime().setDefaultInternalEncoding(null);

            return nil();
        }

        @Specialization(guards = { "!isRubyEncoding(encoding)", "!isNil(encoding)" })
        public RubyBasicObject defaultInternal(VirtualFrame frame, Object encoding) {
            CompilerDirectives.transferToInterpreter();

            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreter();
                toStrNode = insert(ToStrNodeGen.create(getContext(), getSourceSection(), null));
            }

            final RubyBasicObject encodingName = toStrNode.executeToStr(frame, encoding);
            getContext().getRuntime().setDefaultInternalEncoding(getEncoding(getEncoding(encodingName.toString())));

            return encodingName;
        }

    }

    @CoreMethod(names = "list", onSingleton = true)
    public abstract static class ListNode extends CoreMethodArrayArgumentsNode {

        public ListNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject list() {
            CompilerDirectives.transferToInterpreter();

            final RubyBasicObject[] encodings = cloneEncodingList();

            return createArray(encodings, encodings.length);
        }
    }


    @CoreMethod(names = "locale_charmap", onSingleton = true)
    public abstract static class LocaleCharacterMapNode extends CoreMethodArrayArgumentsNode {

        public LocaleCharacterMapNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject localeCharacterMap() {
            CompilerDirectives.transferToInterpreter();
            final ByteList name = new ByteList(getContext().getRuntime().getEncodingService().getLocaleEncoding().getName());
            return createString(name);
        }
    }

    @CoreMethod(names = "dummy?")
    public abstract static class DummyNode extends CoreMethodArrayArgumentsNode {

        public DummyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean isDummy(RubyBasicObject encoding) {
            return EncodingNodes.isDummy(encoding);
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

            final RubyBasicObject[] encodings = cloneEncodingList();
            for (int i = 0; i < encodings.length; i++) {
                final Object upcased = upcaseNode.call(frame, createString(getName(encodings[i])), "upcase", null);
                final Object key = toSymNode.call(frame, upcased, "to_sym", null);
                final Object value = newTupleNode.call(frame, getContext().getCoreLibrary().getTupleClass(), "create", null, nil(), i);

                lookupTableWriteNode.call(frame, ret, "[]=", null, key, value);
            }

            final Hash.HashEntryIterator i = getContext().getRuntime().getEncodingService().getAliases().entryIterator();
            while (i.hasNext()) {
                final CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<EncodingDB.Entry> e =
                        ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<EncodingDB.Entry>)i.next());

                final Object upcased = upcaseNode.call(frame, createString(new ByteList(e.bytes, e.p, e.end - e.p)), "upcase", null);
                final Object key = toSymNode.call(frame, upcased, "to_sym", null);
                final RubyBasicObject alias = createString(new ByteList(e.bytes, e.p, e.end - e.p));
                final int index = e.value.getIndex();


                final Object value = newTupleNode.call(frame, getContext().getCoreLibrary().getTupleClass(), "create", null, alias, index);
                lookupTableWriteNode.call(frame, ret, "[]=", null, key, value);
            }

            final Encoding defaultInternalEncoding = getContext().getRuntime().getDefaultInternalEncoding();
            final Object internalTuple = getContext().makeTuple(frame, newTupleNode, createString("internal"), indexLookup(encodings, defaultInternalEncoding));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("INTERNAL"), internalTuple);

            final Encoding defaultExternalEncoding = getContext().getRuntime().getDefaultExternalEncoding();
            final Object externalTuple = getContext().makeTuple(frame, newTupleNode, createString("external"), indexLookup(encodings, defaultExternalEncoding));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("EXTERNAL"), externalTuple);

            final Encoding localeEncoding = getContext().getRuntime().getEncodingService().getLocaleEncoding();
            final Object localeTuple = getContext().makeTuple(frame, newTupleNode, createString("locale"), indexLookup(encodings, localeEncoding));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("LOCALE"), localeTuple);

            final Encoding filesystemEncoding = getContext().getRuntime().getEncodingService().getLocaleEncoding();
            final Object filesystemTuple = getContext().makeTuple(frame, newTupleNode, createString("filesystem"), indexLookup(encodings, filesystemEncoding));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("FILESYSTEM"), filesystemTuple);

            return ret;
        }

        @TruffleBoundary
        public Object indexLookup(RubyBasicObject[] encodings, Encoding encoding) {
            // TODO (nirvdrum 25-Mar-15): Build up this lookup table in RubyEncoding as we register encodings.
            if (encoding == null) {
                return nil();
            }

            for (int i = 0; i < encodings.length; i++) {
                if (getEncoding(encodings[i]) == encoding) {
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
        public RubyBasicObject toS(RubyBasicObject encoding) {
            final ByteList name = getName(encoding).dup();
            name.setEncoding(ASCIIEncoding.INSTANCE);
            return createString(name);
        }
    }

}
