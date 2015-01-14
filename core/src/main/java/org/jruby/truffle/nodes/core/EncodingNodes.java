/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.util.CaseInsensitiveBytesHash;
import org.jcodings.util.Hash;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyEncoding;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.ByteList;

@CoreClass(name = "Encoding")
public abstract class EncodingNodes {

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
        public RubyEncoding defaultInternal() {
            notDesignedForCompilation();

            Encoding encoding = getContext().getRuntime().getDefaultInternalEncoding();

            if (encoding == null) {
                encoding = UTF8Encoding.INSTANCE;
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

    }

    @CoreMethod(names = "default_internal=", onSingleton = true, required = 1)
    public abstract static class SetDefaultInternalNode extends CoreMethodNode {

        public SetDefaultInternalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SetDefaultInternalNode(SetDefaultInternalNode prev) {
            super(prev);
        }

        @Specialization
        public RubyEncoding defaultExternal(RubyEncoding encoding) {
            notDesignedForCompilation();

            getContext().getRuntime().setDefaultInternalEncoding(encoding.getEncoding());

            return encoding;
        }

        @Specialization
        public RubyNilClass defaultExternal(RubyNilClass encoding) {
            notDesignedForCompilation();

            getContext().getRuntime().setDefaultInternalEncoding(ASCIIEncoding.INSTANCE);

            return encoding;
        }

    }

    @CoreMethod(names = "find", onSingleton = true, required = 1)
    public abstract static class FindNode extends CoreMethodNode {

        public FindNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FindNode(FindNode prev) {
            super(prev);
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
