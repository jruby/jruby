/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.encoding;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.util.CaseInsensitiveBytesHash;
import org.jcodings.util.Hash;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.YieldingCoreMethodNode;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.control.RaiseException;

@CoreClass("Truffle::Encoding")
public abstract class TruffleEncodingNodes {

    @CoreMethod(names = "default_external=", onSingleton = true, required = 1)
    public abstract static class SetDefaultExternalNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyEncoding(encoding)")
        public DynamicObject defaultExternalEncoding(DynamicObject encoding) {
            getContext().getEncodingManager().setDefaultExternalEncoding(EncodingOperations.getEncoding(encoding));
            return encoding;
        }

        @Specialization(guards = "isNil(nil)")
        public DynamicObject defaultExternal(Object nil) {
            throw new RaiseException(coreExceptions().argumentError("default external can not be nil", this));
        }

    }

    @CoreMethod(names = "default_internal=", onSingleton = true, required = 1)
    public abstract static class SetDefaultInternalNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyEncoding(encoding)")
        public DynamicObject defaultInternal(DynamicObject encoding) {
            getContext().getEncodingManager().setDefaultInternalEncoding(EncodingOperations.getEncoding(encoding));
            return encoding;
        }

        @Specialization(guards = "isNil(encoding)")
        public DynamicObject defaultInternal(Object encoding) {
            getContext().getEncodingManager().setDefaultInternalEncoding(null);
            return nil();
        }

    }

    @CoreMethod(names = "each_alias", onSingleton = true, needsBlock = true)
    public abstract static class EachAliasNode extends YieldingCoreMethodNode {

        @Specialization
        public DynamicObject eachAlias(VirtualFrame frame, DynamicObject block) {
            CompilerAsserts.neverPartOfCompilation();
            for (Hash.HashEntry<EncodingDB.Entry> entry : EncodingDB.getAliases().entryIterator()) {
                final CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<EncodingDB.Entry> e = (CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry<EncodingDB.Entry>) entry;
                final Rope aliasName = RopeOperations.create(ArrayUtils.extractRange(e.bytes, e.p, e.end), USASCIIEncoding.INSTANCE, CodeRange.CR_7BIT);
                yield(frame, block, createString(aliasName), entry.value.getIndex());
            }
            return nil();
        }
    }

    @CoreMethod(names = "get_default_encoding", onSingleton = true, required = 1)
    public abstract static class GetDefaultEncodingNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyString(name)")
        public DynamicObject getDefaultEncoding(DynamicObject name,
                                                @Cached("create()")EncodingNodes.GetRubyEncodingNode getRubyEncodingNode) {
            final Encoding encoding = getEncoding(StringOperations.getString(name));
            if (encoding == null) {
                return nil();
            } else {
                return getRubyEncodingNode.executeGetRubyEncoding(encoding);
            }
        }

        @TruffleBoundary
        private Encoding getEncoding(String name) {
            switch (name) {
                case "internal":
                    return getContext().getEncodingManager().getDefaultInternalEncoding();
                case "external":
                    return getContext().getEncodingManager().getDefaultExternalEncoding();
                case "locale":
                case "filesystem":
                    return getContext().getEncodingManager().getLocaleEncoding();
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }


}
