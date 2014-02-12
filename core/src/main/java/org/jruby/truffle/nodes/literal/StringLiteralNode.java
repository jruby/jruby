/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.literal;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.RubyString;

@NodeInfo(shortName = "string")
public class StringLiteralNode extends RubyNode {

    private final String string;

    public StringLiteralNode(RubyContext context, SourceSection sourceSection, String string) {
        super(context, sourceSection);

        assert string != null;

        this.string = string;
    }

    @Override
    public RubyString execute(VirtualFrame frame) {
        return makeString();
    }

    @CompilerDirectives.SlowPath
    private RubyString makeString() {
        return getContext().makeString(string);
    }

}
