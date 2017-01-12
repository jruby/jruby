/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.supercall;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.core.array.ArrayToObjectArrayNode;
import org.jruby.truffle.core.array.ArrayToObjectArrayNodeGen;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;

/**
 * Get the arguments of a super call with implicit arguments (using the ones of the surrounding method).
 */
public class ReadZSuperArgumentsNode extends RubyNode {

    @Children private final RubyNode[] reloadNodes;
    @Child private ArrayToObjectArrayNode unsplatNode;

    private final int restArgIndex;

    public ReadZSuperArgumentsNode(int restArgIndex, RubyNode[] reloadNodes) {
        this.restArgIndex = restArgIndex;
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

        if (restArgIndex != -1) {
            final Object restArg = superArguments[restArgIndex];
            assert RubyGuards.isRubyArray(restArg);
            final Object[] restArgs = unsplat((DynamicObject) restArg);
            final int after = superArguments.length - (restArgIndex + 1);
            Object[] splattedArguments = ArrayUtils.copyOf(superArguments, superArguments.length + restArgs.length - 1);
            ArrayUtils.arraycopy(superArguments, restArgIndex + 1, splattedArguments, restArgIndex + restArgs.length, after);
            ArrayUtils.arraycopy(restArgs, 0, splattedArguments, restArgIndex, restArgs.length);
            superArguments = splattedArguments;
        }

        return superArguments;
    }

    private Object[] unsplat(DynamicObject array) {
        if (unsplatNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            unsplatNode = insert(ArrayToObjectArrayNodeGen.create(null));
        }
        return unsplatNode.executeToObjectArray(array);
    }

}
