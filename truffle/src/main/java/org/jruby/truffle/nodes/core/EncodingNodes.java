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
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;

import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.util.CaseInsensitiveBytesHash;
import org.jcodings.util.Hash;
import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.coerce.ToStrNode;
import org.jruby.truffle.nodes.coerce.ToStrNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyEncoding;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyRegexp;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.truffle.runtime.hash.HashOperations;
import org.jruby.truffle.runtime.hash.KeyValue;
import org.jruby.util.ByteList;

import java.util.ArrayList;
import java.util.List;

@CoreClass(name = "Encoding")
public abstract class EncodingNodes {

    @CoreMethod(names = "aliases", needsSelf = false, onSingleton = true, required = 0)
    public abstract static class AliasesNode extends CoreMethodNode {

        public AliasesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AliasesNode(AliasesNode prev) {
            super(prev);
        }

        @TruffleBoundary
        @Specialization
        public RubyHash aliases() {
            notDesignedForCompilation();

            final List<KeyValue> aliases = new ArrayList<>();

            final Hash.HashEntryIterator i = getContext().getRuntime().getEncodingService().getAliases().entryIterator();
            while (i.hasNext()) {
                final CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<EncodingDB.Entry> e =
                        ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<EncodingDB.Entry>)i.next());

                final RubyString alias = getContext().makeString(new ByteList(e.bytes, e.p, e.end - e.p));
                alias.freeze();

                final RubyString name = getContext().makeString(RubyEncoding.getEncoding(e.value.getIndex()).getName());
                name.freeze();

                aliases.add(new KeyValue(alias, name));
            }

            aliases.add(new KeyValue(getContext().makeString("external"),
                    getContext().makeString(new ByteList(getContext().getRuntime().getDefaultExternalEncoding().getName()))));

            aliases.add(new KeyValue(getContext().makeString("locale"),
                    getContext().makeString(new ByteList(getContext().getRuntime().getEncodingService().getLocaleEncoding().getName()))));

            return HashOperations.verySlowFromEntries(getContext(), aliases, false);
        }
    }

    @CoreMethod(names = "ascii_compatible?")
    public abstract static class AsciiCompatibleNode extends CoreMethodNode {

        public AsciiCompatibleNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AsciiCompatibleNode(AsciiCompatibleNode prev) {
            super(prev);
        }

        @Specialization
        public Object isCompatible(RubyEncoding encoding) {
            notDesignedForCompilation();
            return encoding.getEncoding().isAsciiCompatible();
        }
    }

    @CoreMethod(names = "compatible?", needsSelf = false, onSingleton = true, required = 2)
    public abstract static class CompatibleQueryNode extends CoreMethodNode {

        public CompatibleQueryNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CompatibleQueryNode(CompatibleQueryNode prev) {
            super(prev);
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

    }

    @CoreMethod(names = "default_external", onSingleton = true)
    public abstract static class DefaultExternalNode extends CoreMethodNode {

        public DefaultExternalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DefaultExternalNode(DefaultExternalNode prev) {
            super(prev);
        }

        @Specialization
        public RubyEncoding defaultExternal() {
            notDesignedForCompilation();

            Encoding encoding = getContext().getRuntime().getDefaultExternalEncoding();

            if (encoding == null) {
                encoding = UTF8Encoding.INSTANCE;
            }

            return RubyEncoding.getEncoding(encoding);
        }

    }

    @CoreMethod(names = "default_internal", onSingleton = true)
    public abstract static class DefaultInternalNode extends CoreMethodNode {

        public DefaultInternalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DefaultInternalNode(DefaultInternalNode prev) {
            super(prev);
        }

        @Specialization
        public Object defaultInternal() {
            notDesignedForCompilation();

            Encoding encoding = getContext().getRuntime().getDefaultInternalEncoding();

            if (encoding == null) {
                return nil();
                //encoding = UTF8Encoding.INSTANCE;
            }

            return RubyEncoding.getEncoding(encoding);
        }

    }

    @CoreMethod(names = "default_external=", onSingleton = true, required = 1)
    public abstract static class SetDefaultExternalNode extends CoreMethodNode {

        public SetDefaultExternalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SetDefaultExternalNode(SetDefaultExternalNode prev) {
            super(prev);
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

    }

    @CoreMethod(names = "default_internal=", onSingleton = true, required = 1)
    public abstract static class SetDefaultInternalNode extends CoreMethodNode {

        @Child private ToStrNode toStrNode;

        public SetDefaultInternalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SetDefaultInternalNode(SetDefaultInternalNode prev) {
            super(prev);
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
                toStrNode = insert(ToStrNodeFactory.create(getContext(), getSourceSection(), null));
            }

            final RubyString encodingName = toStrNode.executeRubyString(frame, encoding);
            getContext().getRuntime().setDefaultInternalEncoding(RubyEncoding.getEncoding(encodingName.toString()).getEncoding());

            return encodingName;
        }

    }

    @CoreMethod(names = "find", onSingleton = true, required = 1)
    @NodeChild(value = "name")
    public abstract static class FindNode extends RubyNode {

        public FindNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FindNode(FindNode prev) {
            super(prev);
        }

        @CreateCast("name") public RubyNode coerceNameToString(RubyNode name) {
            return ToStrNodeFactory.create(getContext(), getSourceSection(), name);
        }

        @Specialization
        public RubyEncoding find(RubyString name) {
            notDesignedForCompilation();

            return RubyEncoding.getEncoding(name.toString());
        }

    }

    @CoreMethod(names = "name_list", onSingleton = true)
    public abstract static class NameListNode extends CoreMethodNode {

        public NameListNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NameListNode(NameListNode prev) {
            super(prev);
        }

        @TruffleBoundary
        @Specialization
        public RubyArray find() {
            notDesignedForCompilation();

            final EncodingService service = getContext().getRuntime().getEncodingService();

            final Object[] array = new Object[service.getEncodings().size() + service.getAliases().size() + 2];
            int n = 0;

            Hash.HashEntryIterator i;
            
            i = service.getEncodings().entryIterator();

            while (i.hasNext()) {
                CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<EncodingDB.Entry> e =
                        ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<EncodingDB.Entry>)i.next());
                array[n++] = new RubyString(getContext().getCoreLibrary().getStringClass(), new ByteList(e.bytes, e.p, e.end - e.p));
            }

            i = service.getAliases().entryIterator();

            while (i.hasNext()) {
                CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<EncodingDB.Entry> e =
                        ((CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<EncodingDB.Entry>)i.next());
                array[n++] = new RubyString(getContext().getCoreLibrary().getStringClass(), new ByteList(e.bytes, e.p, e.end - e.p));
            }

            array[n++] = new RubyString(getContext().getCoreLibrary().getStringClass(), org.jruby.RubyEncoding.EXTERNAL);
            //array[n++] = new RubyString(getContext().getCoreLibrary().getStringClass(), org.jruby.RubyEncoding.INTERNAL);
            array[n++] = new RubyString(getContext().getCoreLibrary().getStringClass(), org.jruby.RubyEncoding.LOCALE);
            //array[n++] = new RubyString(getContext().getCoreLibrary().getStringClass(), org.jruby.RubyEncoding.FILESYSTEM);

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), array, array.length);
        }
    }

    @CoreMethod(names = "list", onSingleton = true)
    public abstract static class ListNode extends CoreMethodNode {

        public ListNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ListNode(ListNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray list() {
            notDesignedForCompilation();

            final RubyEncoding[] encodings = RubyEncoding.cloneEncodingList();

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), encodings, encodings.length);
        }
    }


    @CoreMethod(names = "locale_charmap", onSingleton = true)
    public abstract static class LocaleCharacterMapNode extends CoreMethodNode {

        public LocaleCharacterMapNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LocaleCharacterMapNode(LocaleCharacterMapNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString localeCharacterMap() {
            notDesignedForCompilation();
            final ByteList name = new ByteList(getContext().getRuntime().getEncodingService().getLocaleEncoding().getName());
            return getContext().makeString(name);
        }
    }

    @CoreMethod(names = "dummy?")
    public abstract static class DummyNode extends CoreMethodNode {

        public DummyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DummyNode(DummyNode prev) {
            super(prev);
        }

        @Specialization
        public boolean isDummy(RubyEncoding encoding) {
            notDesignedForCompilation();

            return encoding.isDummy();
        }
    }

    @CoreMethod(names = { "name", "to_s" })
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyString toS(RubyEncoding encoding) {
            final ByteList name = encoding.getName().dup();
            name.setEncoding(ASCIIEncoding.INSTANCE);
            return getContext().makeString(name);
        }
    }

    @CoreMethod(names = "inspect")
    public abstract static class InspectNode extends CoreMethodNode {

        public InspectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InspectNode(InspectNode prev) {
            super(prev);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyString toS(RubyEncoding encoding) {
            final ByteList nameByteList = encoding.getName().dup();
            nameByteList.setEncoding(ASCIIEncoding.INSTANCE);

            if (encoding.isDummy()) {
                return getContext().makeString(String.format("#<Encoding:%s (dummy)>", nameByteList.toString()));
            } else {
                return getContext().makeString(String.format("#<Encoding:%s>", nameByteList.toString()));
            }
        }
    }
}
