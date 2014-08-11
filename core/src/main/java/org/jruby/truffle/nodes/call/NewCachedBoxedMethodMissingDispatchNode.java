package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Generic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.truffle.runtime.lookup.LookupNode;
import org.jruby.truffle.runtime.methods.RubyMethod;

/**
 * Created by mg on 8/7/14.
 */
public abstract class NewCachedBoxedMethodMissingDispatchNode extends NewCachedDispatchNode {

    private final LookupNode expectedLookupNode;
    private final Assumption unmodifiedAssumption;
    private final RubyMethod method;
    private final RubySymbol symbol;

    @Child protected DirectCallNode callNode;

    public NewCachedBoxedMethodMissingDispatchNode(RubyContext context, NewDispatchNode next, LookupNode expectedLookupNode, RubyMethod method, String name) {
        super(context, next);
        assert expectedLookupNode != null;
        assert method != null;

        this.expectedLookupNode = expectedLookupNode;
        unmodifiedAssumption = expectedLookupNode.getUnmodifiedAssumption();
        this.method = method;
        symbol = context.newSymbol(name);

        callNode = Truffle.getRuntime().createDirectCallNode(method.getCallTarget());
    }

    public NewCachedBoxedMethodMissingDispatchNode(NewCachedBoxedMethodMissingDispatchNode prev) {
        this(prev.getContext(), prev.next, prev.expectedLookupNode, prev.method, prev.symbol.toString());
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

        // When calling #method_missing we need to prepend the symbol

        final Object[] modifiedArgumentsObjects = new Object[1 + argumentsObjects.length];
        modifiedArgumentsObjects[0] = symbol;
        System.arraycopy(argumentsObjects, 0, modifiedArgumentsObjects, 1, argumentsObjects.length);

        // Call the method

        return callNode.call(frame, RubyArguments.pack(method.getDeclarationFrame(), receiverObject, blockObject, modifiedArgumentsObjects));
    }

    @Generic
    public Object dispatch(VirtualFrame frame, Object callingSelf, Object receiverObject, Object blockObject, Object argumentsObjects) {
        return doNext(frame, callingSelf, receiverObject, CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false), argumentsObjects);
    }

    private Object doNext(VirtualFrame frame, Object callingSelf, Object receiverObject, RubyProc blockObject, Object argumentsObjects) {
        return next.executeDispatch(frame, callingSelf, receiverObject, blockObject, argumentsObjects);
    }


}