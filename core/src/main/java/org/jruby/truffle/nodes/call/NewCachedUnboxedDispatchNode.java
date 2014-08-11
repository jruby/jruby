package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Generic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.methods.RubyMethod;

/**
 * Created by mg on 8/7/14.
 */
public abstract class NewCachedUnboxedDispatchNode extends NewCachedDispatchNode {

    private final Class expectedClass;
    private final Assumption unmodifiedAssumption;
    private final RubyMethod method;

    @Child protected DirectCallNode callNode;

    public NewCachedUnboxedDispatchNode(RubyContext context, NewDispatchNode next, Class expectedClass, Assumption unmodifiedAssumption, RubyMethod method) {
        super(context, next);
        assert expectedClass != null;
        assert unmodifiedAssumption != null;
        assert method != null;

        this.expectedClass = expectedClass;
        this.unmodifiedAssumption = unmodifiedAssumption;
        this.method = method;

        this.callNode = Truffle.getRuntime().createDirectCallNode(method.getCallTarget());
    }

    public NewCachedUnboxedDispatchNode(NewCachedUnboxedDispatchNode prev) {
        this(prev.getContext(), prev.next, prev.expectedClass, prev.unmodifiedAssumption, prev.method);
    }



    @Specialization(guards = "isPrimitive")
    public Object dispatch(VirtualFrame frame, Object callingSelf, Object receiverObject, Object blockObject, Object argumentsObjects) {
        // Check the class is what we expect

        if (receiverObject.getClass() != expectedClass) {
            return next.executeDispatch(frame, callingSelf, receiverObject, blockObject, argumentsObjects);
        }
        return doDispatch(frame, callingSelf, receiverObject, CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false), CompilerDirectives.unsafeCast(argumentsObjects, Object[].class, true, true));
    }

    @Generic
    public Object dispatchGeneric(VirtualFrame frame, Object boxedCallingSelf, Object receiverObject, Object blockObject, Object argumentsObjects) {
        return doNext(frame, boxedCallingSelf, receiverObject, blockObject, argumentsObjects);
    }

    private Object doDispatch(VirtualFrame frame, Object callingSelf, Object receiverObject, RubyProc blockObject, Object[] argumentsObjects) {
        // Check the class has not been modified

        try {
            unmodifiedAssumption.check();
        } catch (InvalidAssumptionException e) {
            return respecialize("class modified", frame, receiverObject, blockObject, argumentsObjects);
        }

        // Call the method

        return callNode.call(frame, RubyArguments.pack(method.getDeclarationFrame(), receiverObject, blockObject, argumentsObjects));
    }

    private Object doNext(VirtualFrame frame, Object boxedCallingSelf, Object receiverObject, Object blockObject, Object argumentsObjects) {
        return next.executeDispatch(frame, boxedCallingSelf, receiverObject, blockObject, argumentsObjects);
    }
}
