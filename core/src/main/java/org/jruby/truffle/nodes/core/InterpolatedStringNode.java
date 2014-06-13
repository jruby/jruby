/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.call.DispatchHeadNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.ByteList;

/**
 * A list of expressions to build up into a string.
 */
@NodeInfo(shortName = "interpolated-string")
public final class InterpolatedStringNode extends RubyNode {

    @Children protected final RubyNode[] children;
    @Child protected DispatchHeadNode toS;

    public InterpolatedStringNode(RubyContext context, SourceSection sourceSection, RubyNode[] children) {
        super(context, sourceSection);
        this.children = children;
        toS = new DispatchHeadNode(context, "to_s", false, DispatchHeadNode.MissingBehavior.CALL_METHOD_MISSING);
    }

    @ExplodeLoop
    @Override
    public Object execute(VirtualFrame frame) {
        final RubyString[] strings = new RubyString[children.length];

        for (int n = 0; n < children.length; n++) {
            strings[n] = (RubyString) toS.dispatch(frame, children[n].execute(frame), null);
        }

        return concat(strings);
    }

    @CompilerDirectives.SlowPath
    private RubyString concat(RubyString[] strings) {
        // TODO(CS): what happens to encoding here

        int length = 0;

        for (RubyString string : strings) {
            length += string.getBytes().length();
        }

        final ByteList bytes = new ByteList(length);

        for (RubyString string : strings) {
            bytes.append(string.getBytes());
        }

        return new RubyString(getContext().getCoreLibrary().getStringClass(), bytes);
    }

    @ExplodeLoop
    @Override
    public void executeVoid(VirtualFrame frame) {
        for (int n = 0; n < children.length; n++) {
            children[n].executeVoid(frame);
        }
    }

}
