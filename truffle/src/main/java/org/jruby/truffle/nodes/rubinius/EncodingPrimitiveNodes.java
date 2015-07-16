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
import org.jruby.truffle.nodes.core.EncodingNodes;
import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.nodes.core.SymbolNodes;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyEncoding;

/**
 * Rubinius primitives associated with the Ruby {@code Encoding} class..
 */
public abstract class EncodingPrimitiveNodes {

    @RubiniusPrimitive(name = "encoding_get_object_encoding", needsSelf = false)
    public static abstract class EncodingGetObjectEncodingNode extends RubiniusPrimitiveNode {

        public EncodingGetObjectEncodingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(string)")
        public RubyBasicObject encodingGetObjectEncodingString(RubyBasicObject string) {
            return EncodingNodes.getEncoding(StringNodes.getByteList(string).getEncoding());
        }

        @Specialization(guards = "isRubySymbol(symbol)")
        public RubyBasicObject encodingGetObjectEncodingSymbol(RubyBasicObject symbol) {
            return EncodingNodes.getEncoding(SymbolNodes.getByteList(symbol).getEncoding());
        }

        @Specialization(guards = "isRubyEncoding(encoding)")
        public RubyBasicObject encodingGetObjectEncoding(RubyBasicObject encoding) {
            return encoding;
        }

        @Specialization(guards = {"!isRubyString(object)", "!isRubySymbol(object)", "!isRubyEncoding(object)"})
        public RubyBasicObject encodingGetObjectEncodingNil(RubyBasicObject object) {
            // TODO(CS, 26 Jan 15) something to do with __encoding__ here?
            return nil();
        }

    }

}
