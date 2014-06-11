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
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;

@NodeInfo(shortName = "fixnum(int)")
public class IntegerFixnumLiteralNode extends RubyNode {

    private final int value;

    public IntegerFixnumLiteralNode(RubyContext context, SourceSection sourceSection, int value) {
        super(context, sourceSection);
        this.value = value;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeIntegerFixnum(frame);
    }

    @Override
    public int executeIntegerFixnum(VirtualFrame frame) {
        return value;
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return getContext().makeString("expression");
    }

    public int getValue() {
        return value;
    }

}
