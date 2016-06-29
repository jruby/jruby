/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.arguments;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.language.RubyNode;

public class ReadPostArgumentNode extends RubyNode {

    private final int indexFromCount;

    public ReadPostArgumentNode(int indexFromCount) {
        this.indexFromCount = indexFromCount;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final int count = RubyArguments.getArgumentsCount(frame);
        final int effectiveIndex = count - indexFromCount;
        return RubyArguments.getArgument(frame, effectiveIndex);
    }

}
