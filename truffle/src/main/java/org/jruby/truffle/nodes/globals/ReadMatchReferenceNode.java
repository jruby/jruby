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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.RubyString;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.util.StringSupport;

import java.util.Arrays;

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
        return readMatchReference();
    }

    @TruffleBoundary
    private Object readMatchReference() {
        DynamicObject receiver = Layouts.THREAD.getThreadLocals(getContext().getThreadManager().getCurrentThread());
        final Object match = receiver.get("$~", Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(receiver)).getContext().getCoreLibrary().getNilObject());

        if (match == null || match == nil()) {
            return nil();
        }

        final DynamicObject matchData = (DynamicObject) match;

        if (index > 0) {
            final Object[] values = Arrays.copyOf(Layouts.MATCH_DATA.getValues(matchData), Layouts.MATCH_DATA.getValues(matchData).length);

            if (index >= values.length) {
                return nil();
            } else {
                return values[index];
            }
        } else if (index == PRE) {
            return Layouts.MATCH_DATA.getPre(matchData);
        } else if (index == POST) {
            return Layouts.MATCH_DATA.getPost(matchData);
        } else if (index == GLOBAL) {
            return Layouts.MATCH_DATA.getGlobal(matchData);
        } else if (index == HIGHEST) {
            final Object[] values = Arrays.copyOf(Layouts.MATCH_DATA.getValues(matchData), Layouts.MATCH_DATA.getValues(matchData).length);

            for (int n = values.length - 1; n >= 0; n--)
                if (values[n] != nil()) {
                    return values[n];
            }

            return nil();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        if (execute(frame) != nil()) {
            return Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), RubyString.encodeBytelist("global-variable", UTF8Encoding.INSTANCE), StringSupport.CR_7BIT, null);
        } else {
            return nil();
        }
    }

}
