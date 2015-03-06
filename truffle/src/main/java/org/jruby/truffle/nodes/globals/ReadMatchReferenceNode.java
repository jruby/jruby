/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.globals;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyMatchData;

public class ReadMatchReferenceNode extends RubyNode {

    public static final int PRE = -1;
    public static final int POST = -2;
    public static final int GLOBAL = -3;
    public static final int HIGHEST = -4;

    private final int index;

    public ReadMatchReferenceNode(RubyContext context, SourceSection sourceSection, int index) {
        super(context, sourceSection);
        this.index = index;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        notDesignedForCompilation("32398dd7c22e4fbfb973cf9a3a993ccb");

        final Object match = getContext().getThreadManager().getCurrentThread().getThreadLocals().getInstanceVariable("$~");

        if (match == null || match == getContext().getCoreLibrary().getNilObject()) {
            return getContext().getCoreLibrary().getNilObject();
        }

        final RubyMatchData matchData = (RubyMatchData) match;

        if (index > 0) {
            final Object[] values = matchData.getValues();

            if (index >= values.length) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return values[index];
            }
        } else if (index == PRE) {
            return matchData.getPre();
        } else if (index == POST) {
            return matchData.getPost();
        } else if (index == GLOBAL) {
            return matchData.getGlobal();
        } else if (index == HIGHEST) {
            final Object[] values = matchData.getValues();

            for (int n = values.length - 1; n >= 0; n--)
                if (values[n] != getContext().getCoreLibrary().getNilObject()) {
                    return values[n];
            }

            return getContext().getCoreLibrary().getNilObject();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        notDesignedForCompilation("c1c9966653594c748ea2f94e3e51c352");

        if (execute(frame) != getContext().getCoreLibrary().getNilObject()) {
            return getContext().makeString("global-variable");
        } else {
            return getContext().getCoreLibrary().getNilObject();
        }
    }

}
