/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.supercall;

import java.util.Arrays;

import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.core.ArrayOperations;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

/**
 * Get the arguments of a super call with implicit arguments (using the ones of the surrounding method).
 */
public class ReadZSuperArgumentsNode extends RubyNode {

    private final boolean hasRestParameter;
    @Children private final RubyNode[] reloadNodes;

    public ReadZSuperArgumentsNode(RubyContext context, SourceSection sourceSection, boolean hasRestParameter, RubyNode[] reloadNodes) {
        super(context, sourceSection);
        this.hasRestParameter = hasRestParameter;
        this.reloadNodes = reloadNodes;

    }

    @ExplodeLoop
    @Override
    public final Object execute(VirtualFrame frame) {
        CompilerAsserts.compilationConstant(reloadNodes.length);

        // Reload the arguments
        Object[] superArguments = new Object[reloadNodes.length];
        for (int n = 0; n < superArguments.length; n++) {
            superArguments[n] = reloadNodes[n].execute(frame);
        }

        if (hasRestParameter) {
            CompilerDirectives.transferToInterpreter();
            // TODO (eregon, 22 July 2015): Assumes rest arg is last, not true if post or keyword args.
            final Object restArg = superArguments[superArguments.length - 1];
            assert RubyGuards.isRubyArray(restArg);
            final Object[] restArgs = ArrayOperations.toObjectArray((DynamicObject) restArg);
            final int restArgIndex = reloadNodes.length - 1;
            superArguments = Arrays.copyOf(superArguments, restArgIndex + restArgs.length);
            ArrayUtils.arraycopy(restArgs, 0, superArguments, restArgIndex, restArgs.length);
        }

        return superArguments;
    }

}
