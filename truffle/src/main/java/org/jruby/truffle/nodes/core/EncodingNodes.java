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
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.util.CaseInsensitiveBytesHash;
import org.jcodings.util.Hash;
import org.jruby.truffle.nodes.coerce.ToStrNode;
import org.jruby.truffle.nodes.coerce.ToStrNodeGen;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.util.ByteList;

@CoreClass(name = "Encoding")
public abstract class EncodingNodes {

    @CoreMethod(names = "ascii_compatible?")
    public abstract static class AsciiCompatibleNode extends CoreMethodArrayArgumentsNode {

        public AsciiCompatibleNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object isCompatible(RubyEncoding encoding) {
            notDesignedForCompilation();
            return encoding.getEncoding().isAsciiCompatible();
        }
    }

    @CoreMethod(names = "compatible?", needsSelf = false, onSingleton = true, required = 2)
    public abstract static class CompatibleQueryNode extends CoreMethodArrayArgumentsNode {

        public CompatibleQueryNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object isCompatible(RubyString first, RubyString second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(first, second);

            if (compatibleEncoding != null) {
                return RubyEncoding.getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization
        public Object isCompatible(RubyEncoding first, RubyEncoding second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(first.getEncoding(), second.getEncoding());

            if (compatibleEncoding != null) {
                return RubyEncoding.getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization
        public Object isCompatible(RubyString first, RubyRegexp second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(first.getByteList().getEncoding(), second.getRegex().getEncoding());

            if (compatibleEncoding != null) {
                return RubyEncoding.getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization
        public Object isCompatible(RubyRegexp first, RubyString second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(first.getRegex().getEncoding(), second.getByteList().getEncoding());

            if (compatibleEncoding != null) {
                return RubyEncoding.getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization
        public Object isCompatible(RubyRegexp first, RubyRegexp second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(first.getRegex().getEncoding(), second.getRegex().getEncoding());

            if (compatibleEncoding != null) {
                return RubyEncoding.getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization
        public Object isCompatible(RubyRegexp first, RubySymbol second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(first.getRegex().getEncoding(), second.getByteList().getEncoding());

            if (compatibleEncoding != null) {
                return RubyEncoding.getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization
        public Object isCompatible(RubySymbol first, RubyRegexp second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(first.getByteList().getEncoding(), second.getRegex().getEncoding());

            if (compatibleEncoding != null) {
                return RubyEncoding.getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization
        public Object isCompatible(RubyString first, RubySymbol second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(first, second);

            if (compatibleEncoding != null) {
                return RubyEncoding.getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization
        public Object isCompatible(RubySymbol first, RubySymbol second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(first, second);

            if (compatibleEncoding != null) {
                return RubyEncoding.getEncoding(compatibleEncoding);
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        @Specialization
        public Object isCompatible(RubyString first, RubyEncoding second) {
            final Encoding compatibleEncoding = org.jruby.RubyEncoding.areCompatible(first.getByteList().getEncoding(), second.getEncoding());

            if (compatibleEncoding != null) {
                return RubyEncoding.getEncoding(compatibleEncoding);
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

        @Specialization
        public RubyEncoding defaultExternal(RubyEncoding encoding) {
            notDesignedForCompilation();

            getContext().getRuntime().setDefaultExternalEncoding(encoding.getEncoding());

            return encoding;
        }

        @Specialization
        public RubyEncoding defaultExternal(RubyString encodingString) {
            notDesignedForCompilation();

            final RubyEncoding rubyEncoding = RubyEncoding.getEncoding(encodingString.toString());
            getContext().getRuntime().setDefaultExternalEncoding(rubyEncoding.getEncoding());

            return rubyEncoding;
        }

        @Specialization
        public RubyEncoding defaultExternal(RubyNilClass nil) {
            throw new RaiseException(getContext().getCoreLibrary().argumentError("default external can not be nil", this));
        }

        @Specialization(guards = { "!isRubyEncoding(encoding)", "!isRubyString(encoding)", "!isRubyNilClass(encoding)" })
        public RubyEncoding defaultExternal(VirtualFrame frame, Object encoding) {
            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreter();
                toStrNode = insert(ToStrNodeGen.create(getContext(), getSourceSection(), null));
            }

            return defaultExternal(toStrNode.executeRubyString(frame, encoding));
        }

    }

    @RubiniusOnly
    @CoreMethod(names = "default_internal_jruby=", onSingleton = true, required = 1)
    public abstract static class SetDefaultInternalNode extends CoreMethodArrayArgumentsNode {

        @Child private ToStrNode toStrNode;

        public SetDefaultInternalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyEncoding defaultInternal(RubyEncoding encoding) {
            notDesignedForCompilation();

            getContext().getRuntime().setDefaultInternalEncoding(encoding.getEncoding());

            return encoding;
        }

        @Specialization
        public RubyNilClass defaultInternal(RubyNilClass encoding) {
            notDesignedForCompilation();

            getContext().getRuntime().setDefaultInternalEncoding(null);

            return encoding;
        }

        @Specialization(guards = { "!isRubyEncoding(encoding)", "!isRubyNilClass(encoding)" })
        public RubyString defaultInternal(VirtualFrame frame, Object encoding) {
            notDesignedForCompilation();

            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreter();
                toStrNode = insert(ToStrNodeGen.create(getContext(), getSourceSection(), null));
            }

            final RubyString encodingName = toStrNode.executeRubyString(frame, encoding);
            getContext().getRuntime().setDefaultInternalEncoding(RubyEncoding.getEncoding(encodingName.toString()).getEncoding());

            return encodingName;
        }

    }

    @CoreMethod(names = "list", onSingleton = true)
    public abstract static class ListNode extends CoreMethodArrayArgumentsNode {

        public ListNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyArray list() {
            notDesignedForCompilation();

            final RubyEncoding[] encodings = RubyEncoding.cloneEncodingList();

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), encodings, encodings.length);
        }
    }


    @CoreMethod(names = "locale_charmap", onSingleton = true)
    public abstract static class LocaleCharacterMapNode extends CoreMethodArrayArgumentsNode {

        public LocaleCharacterMapNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString localeCharacterMap() {
            notDesignedForCompilation();
            final ByteList name = new ByteList(getContext().getRuntime().getEncodingService().getLocaleEncoding().getName());
            return getContext().makeString(name);
        }
    }

    @CoreMethod(names = "dummy?")
    public abstract static class DummyNode extends CoreMethodArrayArgumentsNode {

        public DummyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean isDummy(RubyEncoding encoding) {
            notDesignedForCompilation();

            return encoding.isDummy();
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

            final RubyEncoding[] encodings = RubyEncoding.cloneEncodingList();
            for (int i = 0; i < encodings.length; i++) {
                final Object upcased = upcaseNode.call(frame, getContext().makeString(encodings[i].getName()), "upcase", null);
                final Object key = toSymNode.call(frame, upcased, "to_sym", null);
                final Object value = newTupleNode.call(frame, getContext().getCoreLibrary().getTupleClass(), "create", null, nil(), i);

                lookupTableWriteNode.call(frame, ret, "[]=", null, key, value);
            }

            final Hash.HashEntryIterator i = getContext().getRuntime().getEncodingService().getAliases().entryIterator();
            while (i.hasNext()) {
                final CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<EncodingDB.Entry> e =
                        ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<EncodingDB.Entry>)i.next());

                final Object upcased = upcaseNode.call(frame, getContext().makeString(new ByteList(e.bytes, e.p, e.end - e.p)), "upcase", null);
                final Object key = toSymNode.call(frame, upcased, "to_sym", null);
                final RubyString alias = getContext().makeString(new ByteList(e.bytes, e.p, e.end - e.p));
                final int index = e.value.getIndex();


                final Object value = newTupleNode.call(frame, getContext().getCoreLibrary().getTupleClass(), "create", null, alias, index);
                lookupTableWriteNode.call(frame, ret, "[]=", null, key, value);
            }

            final Encoding defaultInternalEncoding = getContext().getRuntime().getDefaultInternalEncoding();
            final Object internalTuple = getContext().makeTuple(frame, newTupleNode, getContext().makeString("internal"), indexLookup(encodings, defaultInternalEncoding));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getContext().getSymbol("INTERNAL"), internalTuple);

            final Encoding defaultExternalEncoding = getContext().getRuntime().getDefaultExternalEncoding();
            final Object externalTuple = getContext().makeTuple(frame, newTupleNode, getContext().makeString("external"), indexLookup(encodings, defaultExternalEncoding));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getContext().getSymbol("EXTERNAL"), externalTuple);

            final Encoding localeEncoding = getContext().getRuntime().getEncodingService().getLocaleEncoding();
            final Object localeTuple = getContext().makeTuple(frame, newTupleNode, getContext().makeString("locale"), indexLookup(encodings, localeEncoding));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getContext().getSymbol("LOCALE"), localeTuple);

            final Encoding filesystemEncoding = getContext().getRuntime().getEncodingService().getLocaleEncoding();
            final Object filesystemTuple = getContext().makeTuple(frame, newTupleNode, getContext().makeString("filesystem"), indexLookup(encodings, filesystemEncoding));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getContext().getSymbol("FILESYSTEM"), filesystemTuple);

            return ret;
        }

        @TruffleBoundary
        public Object indexLookup(RubyEncoding[] encodings, Encoding encoding) {
            // TODO (nirvdrum 25-Mar-15): Build up this lookup table in RubyEncoding as we register encodings.
            if (encoding == null) {
                return nil();
            }

            for (int i = 0; i < encodings.length; i++) {
                if (encodings[i].getEncoding() == encoding) {
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

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyString toS(RubyEncoding encoding) {
            final ByteList name = encoding.getName().dup();
            name.setEncoding(ASCIIEncoding.INSTANCE);
            return getContext().makeString(name);
        }
    }

}
