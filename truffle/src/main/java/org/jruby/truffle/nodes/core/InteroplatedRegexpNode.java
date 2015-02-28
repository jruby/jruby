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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyEncoding;
import org.jruby.truffle.runtime.core.RubyRegexp;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.translator.BodyTranslator;
import org.jruby.util.RegexpOptions;

public class InteroplatedRegexpNode extends RubyNode {

    @Children private final RubyNode[] children;
    private final RegexpOptions options;
    @Child private CallDispatchHeadNode toS;

    public InteroplatedRegexpNode(RubyContext context, SourceSection sourceSection, RubyNode[] children, RegexpOptions options) {
        super(context, sourceSection);
        this.children = children;
        this.options = options;
        toS = DispatchHeadNodeFactory.createMethodCall(context);
    }

    @Override
    public RubyRegexp executeRubyRegexp(VirtualFrame frame) {
        notDesignedForCompilation();

        final org.jruby.RubyString[] strings = new org.jruby.RubyString[children.length];

        for (int n = 0; n < children.length; n++) {
            final Object child = children[n].execute(frame);
            strings[n] = org.jruby.RubyString.newString(getContext().getRuntime(), ((RubyString) toS.call(frame, child, "to_s", null)).getBytes());
        }

        final org.jruby.RubyString preprocessed = org.jruby.RubyRegexp.preprocessDRegexp(getContext().getRuntime(), strings, options);

        final RubyRegexp regexp = new RubyRegexp(this, getContext().getCoreLibrary().getRegexpClass(), preprocessed.getByteList(), options);

        if (options.isEncodingNone()) {
            if (!BodyTranslator.all7Bit(preprocessed.getByteList().bytes())) {
                regexp.getSource().setEncoding(getContext().getRuntime().getEncodingService().getAscii8bitEncoding());
            } else {
                regexp.getSource().setEncoding(getContext().getRuntime().getEncodingService().getUSAsciiEncoding());
            }
        }

        return regexp;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeRubyRegexp(frame);
    }
}
