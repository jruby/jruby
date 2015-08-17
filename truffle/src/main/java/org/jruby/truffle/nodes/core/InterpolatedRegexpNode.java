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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.translator.BodyTranslator;
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
            strings[n] = org.jruby.RubyString.newString(getContext().getRuntime(), Layouts.STRING.getByteList((DynamicObject) toS.call(frame, child, "to_s", null)));
        }

        final org.jruby.RubyString preprocessed = org.jruby.RubyRegexp.preprocessDRegexp(getContext().getRuntime(), strings, options);

        final DynamicObject regexp = RegexpNodes.createRubyRegexp(this, getContext().getCoreLibrary().getRegexpClass(), preprocessed.getByteList(), options);

        if (options.isEncodingNone()) {
            if (!BodyTranslator.all7Bit(preprocessed.getByteList().bytes())) {
                Layouts.REGEXP.getSource(regexp).setEncoding(getContext().getRuntime().getEncodingService().getAscii8bitEncoding());
            } else {
                Layouts.REGEXP.getSource(regexp).setEncoding(getContext().getRuntime().getEncodingService().getUSAsciiEncoding());
            }
        }

        return regexp;
    }
}
