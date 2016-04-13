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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.parser.jruby.BodyTranslator;
import org.jruby.util.RegexpOptions;

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
        CompilerDirectives.transferToInterpreter();

        final org.jruby.RubyString[] strings = new org.jruby.RubyString[children.length];

        for (int n = 0; n < children.length; n++) {
            final Object child = children[n].execute(frame);
            strings[n] = org.jruby.RubyString.newString(getContext().getJRubyRuntime(), StringOperations.getByteListReadOnly((DynamicObject) toS.call(frame, child, "to_s", null)));
        }

        final org.jruby.RubyString preprocessed = org.jruby.RubyRegexp.preprocessDRegexp(getContext().getJRubyRuntime(), strings, options);

        final DynamicObject regexp = RegexpNodes.createRubyRegexp(getContext(), this, coreLibrary().getRegexpClass(), StringOperations.ropeFromByteList(preprocessed.getByteList()), options);

        if (options.isEncodingNone()) {
            final Rope source = Layouts.REGEXP.getSource(regexp);

            if (!BodyTranslator.all7Bit(preprocessed.getByteList().bytes())) {
                Layouts.REGEXP.setSource(regexp, RopeOperations.withEncodingVerySlow(source, getContext().getJRubyRuntime().getEncodingService().getAscii8bitEncoding()));
            } else {
                Layouts.REGEXP.setSource(regexp, RopeOperations.withEncodingVerySlow(source, getContext().getJRubyRuntime().getEncodingService().getUSAsciiEncoding()));
            }
        }

        return regexp;
    }
}
