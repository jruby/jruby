/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.lookup.LookupNode;
import org.jruby.truffle.runtime.methods.RubyMethod;

public abstract class NewCachedBoxedDispatchNode extends NewCachedDispatchNode {

    private final LookupNode expectedLookupNode;
    private final Assumption unmodifiedAssumption;
    private final RubyMethod method;

    @Child protected DirectCallNode callNode;

    public NewCachedBoxedDispatchNode(RubyContext context, NewDispatchNode next, LookupNode expectedLookupNode, RubyMethod method) {
        super(context, next);
        assert expectedLookupNode != null;
        assert method != null;

        this.expectedLookupNode = expectedLookupNode;
        this.unmodifiedAssumption = expectedLookupNode.getUnmodifiedAssumption();
        this.method = method;
        this.next = next;

        callNode = Truffle.getRuntime().createDirectCallNode(method.getCallTarget());
    }

    public NewCachedBoxedDispatchNode(NewCachedBoxedDispatchNode prev) {
        this(prev.getContext(), prev.next, prev.expectedLookupNode, prev.method);
    }

    @Specialization
    public Object dispatch(VirtualFrame frame, Object boxedCallingSelf, RubyBasicObject receiverObject, Object blockObject, Object argumentsObjects) {
        // Check the lookup node is what we expect

        if (receiverObject.getLookupNode() != expectedLookupNode) {
            return doNext(frame, boxedCallingSelf, receiverObject, CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false), argumentsObjects);
        }

        return doDispatch(frame, boxedCallingSelf, receiverObject, CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false), CompilerDirectives.unsafeCast(argumentsObjects, Object[].class, true, true));
    }

    private Object doDispatch(VirtualFrame frame, Object boxedCallingSelf, RubyBasicObject receiverObject, RubyProc blockObject, Object[] argumentsObjects) {
        // Check the class has not been modified

        try {
            unmodifiedAssumption.check();
        } catch (InvalidAssumptionException e) {
            return respecialize("class modified", frame, receiverObject, blockObject, argumentsObjects);
        }

        // Call the method
        return callNode.call(frame, RubyArguments.pack(method.getDeclarationFrame(), receiverObject, blockObject, argumentsObjects));
    }

    @Fallback
    public Object dispatch(VirtualFrame frame, Object boxedCallingSelf, Object receiverObject, Object blockObject, Object argumentsObjects) {
        return doNext(frame, boxedCallingSelf, receiverObject, CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false), argumentsObjects);
    }


    private Object doNext(VirtualFrame frame, Object callingSelf, Object receiverObject, RubyProc blockObject, Object argumentsObjects) {
        return next.executeDispatch(frame, callingSelf, receiverObject, blockObject, argumentsObjects);
    }
}
