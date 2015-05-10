/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.*;

/**
 * Rubinius primitives associated with the Ruby {@code Encoding} class..
 */
public abstract class EncodingPrimitiveNodes {

    @RubiniusPrimitive(name = "encoding_get_object_encoding", needsSelf = false)
    public static abstract class EncodingGetObjectEncodingNode extends RubiniusPrimitiveNode {

        public EncodingGetObjectEncodingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyEncoding encodingGetObjectEncoding(RubyString string) {
            return RubyEncoding.getEncoding(string.getByteList().getEncoding());
        }

        @Specialization
        public RubyEncoding encodingGetObjectEncoding(RubySymbol symbol) {
            return RubyEncoding.getEncoding(symbol.getSymbolBytes().getEncoding());
        }

        @Specialization
        public RubyEncoding encodingGetObjectEncoding(RubyEncoding encoding) {
            return encoding;
        }

        @Specialization(guards = {"!isRubyString(object)", "!isRubySymbol(object)", "!isRubyEncoding(object)"})
        public RubyBasicObject encodingGetObjectEncoding(RubyBasicObject object) {
            // TODO(CS, 26 Jan 15) something to do with __encoding__ here?
            return nil();
        }

    }

}
