/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.regexp;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.parser.BodyTranslator;
import org.jruby.util.ByteList;

public class InterpolatedRegexpNode extends RubyNode {

    @Children private final RubyNode[] children;
    private final RegexpOptions options;
    @Child private CallDispatchHeadNode toS;

    public InterpolatedRegexpNode(RubyContext context, SourceSection sourceSection, RubyNode[] children, RegexpOptions options) {
        super(context, sourceSection);
        this.children = children;
        this.options = options;
        toS = DispatchHeadNodeFactory.createMethodCall(context);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return createRegexp(executeChildren(frame));
    }

    @TruffleBoundary
    private DynamicObject createRegexp(DynamicObject[] parts) {
        final ByteList[] strings = new ByteList[children.length];

        for (int n = 0; n < children.length; n++) {
            strings[n] = StringOperations.getByteListReadOnly(parts[n]);
        }

        final ByteList preprocessed = ClassicRegexp.preprocessDRegexp(getContext(), strings, options);

        final DynamicObject regexp = RegexpNodes.createRubyRegexp(getContext(), this, coreLibrary().getRegexpFactory(),
                StringOperations.ropeFromByteList(preprocessed), options);

        if (options.isEncodingNone()) {
            final Rope source = Layouts.REGEXP.getSource(regexp);

            if (!BodyTranslator.all7Bit(preprocessed.bytes())) {
                Layouts.REGEXP.setSource(regexp, RopeOperations.withEncodingVerySlow(source, ASCIIEncoding.INSTANCE));
            } else {
                Layouts.REGEXP.setSource(regexp, RopeOperations.withEncodingVerySlow(source, USASCIIEncoding.INSTANCE));
            }
        }

        return regexp;
    }

    @ExplodeLoop
    protected DynamicObject[] executeChildren(VirtualFrame frame) {
        DynamicObject[] values = new DynamicObject[children.length];
        for (int i = 0; i < children.length; i++) {
            final Object value = children[i].execute(frame);
            values[i] = (DynamicObject) toS.call(frame, value, "to_s");
        }
        return values;
    }

}
