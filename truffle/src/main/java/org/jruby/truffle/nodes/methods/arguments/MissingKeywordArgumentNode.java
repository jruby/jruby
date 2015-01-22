/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods.arguments;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;

public class MissingKeywordArgumentNode extends RubyNode {

    private final String name;

    public MissingKeywordArgumentNode(RubyContext context, SourceSection sourceSection, String name) {
        super(context, sourceSection);
        this.name = name;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();
        throw new RaiseException(getContext().getCoreLibrary().argumentErrorMissingKeyword(name, this));
    }

}
