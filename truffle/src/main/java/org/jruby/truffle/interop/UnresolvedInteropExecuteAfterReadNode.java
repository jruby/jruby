/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;

public class UnresolvedInteropExecuteAfterReadNode extends RubyNode {

    private final int arity;
    private final int labelIndex;

    public UnresolvedInteropExecuteAfterReadNode(RubyContext context, SourceSection sourceSection, int arity){
        super(context, sourceSection);
        this.arity = arity;
        this.labelIndex = 0;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (ForeignAccess.getArguments(frame).get(labelIndex) instanceof  String) {
            return this.replace(new ResolvedInteropExecuteAfterReadNode(getContext(), getSourceSection(), (String) ForeignAccess.getArguments(frame).get(labelIndex), arity)).execute(frame);
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException(ForeignAccess.getArguments(frame).get(0) + " not allowed as name");
        }
    }
}
