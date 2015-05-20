/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.dispatch;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.RubyContext;

public class DoesRespondDispatchHeadNode extends DispatchHeadNode {

    public DoesRespondDispatchHeadNode(RubyContext context, boolean ignoreVisibility, boolean indirect, MissingBehavior missingBehavior, LexicalScope lexicalScope) {
        super(context, ignoreVisibility, indirect, missingBehavior, DispatchAction.RESPOND_TO_METHOD);
    }

    /**
     * Check if a specific method is defined on the receiver object.
     * This check is "static" and should only be used in a few VM operations.
     * In many cases, a dynamic call to Ruby's respond_to? should be used instead.
     * Similar to MRI rb_check_funcall().
     */
    public boolean doesRespondTo(
            VirtualFrame frame,
            Object methodName,
            Object receiverObject) {
        // It's ok to cast here as we control what RESPOND_TO_METHOD returns
        return (boolean) dispatch(
                frame,
                receiverObject,
                methodName,
                null,
                null);
    }

}
