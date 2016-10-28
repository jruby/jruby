/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.encoding;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.cast.ToStrNode;
import org.jruby.truffle.core.cast.ToStrNodeGen;
import org.jruby.truffle.language.control.RaiseException;

@CoreClass("Truffle::Encoding")
public abstract class TruffleEncodingNodes {

    @CoreMethod(names = "default_external=", onSingleton = true, required = 1)
    public abstract static class SetDefaultExternalNode extends CoreMethodArrayArgumentsNode {

        @Child private ToStrNode toStrNode;

        @TruffleBoundary
        @Specialization(guards = "isRubyEncoding(encoding)")
        public DynamicObject defaultExternalEncoding(DynamicObject encoding) {
            getContext().getEncodingManager().setDefaultExternalEncoding(EncodingOperations.getEncoding(encoding));

            return encoding;
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(encodingString)")
        public DynamicObject defaultExternal(DynamicObject encodingString) {
            final DynamicObject rubyEncoding = getContext().getEncodingManager().getRubyEncoding(encodingString.toString());
            getContext().getEncodingManager().setDefaultExternalEncoding(EncodingOperations.getEncoding(rubyEncoding));

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
                toStrNode = insert(ToStrNodeGen.create(getContext(), null, null));
            }

            return defaultExternal(toStrNode.executeToStr(frame, encoding));
        }

    }

    @CoreMethod(names = "default_internal=", onSingleton = true, required = 1)
    public abstract static class SetDefaultInternalNode extends CoreMethodArrayArgumentsNode {

        @Child private ToStrNode toStrNode;

        @TruffleBoundary
        @Specialization(guards = "isRubyEncoding(encoding)")
        public DynamicObject defaultInternal(DynamicObject encoding) {
            getContext().getEncodingManager().setDefaultInternalEncoding(EncodingOperations.getEncoding(encoding));

            return encoding;
        }

        @TruffleBoundary
        @Specialization(guards = "isNil(encoding)")
        public DynamicObject defaultInternal(Object encoding) {
            getContext().getEncodingManager().setDefaultInternalEncoding(null);

            return nil();
        }

        @Specialization(guards = { "!isRubyEncoding(encoding)", "!isNil(encoding)" })
        public DynamicObject defaultInternal(VirtualFrame frame, Object encoding) {
            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStrNode = insert(ToStrNodeGen.create(getContext(), null, null));
            }

            final DynamicObject encodingName = toStrNode.executeToStr(frame, encoding);
            getContext().getJRubyRuntime().setDefaultInternalEncoding(EncodingOperations.getEncoding(getContext().getEncodingManager().getRubyEncoding(encodingName.toString())));

            return encodingName;
        }

    }

}
