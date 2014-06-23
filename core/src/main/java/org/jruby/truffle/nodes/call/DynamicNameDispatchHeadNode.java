package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;

public class DynamicNameDispatchHeadNode extends Node {

    private final RubyContext context;
    @Child protected DynamicNameDispatchNode dispatch;

    public DynamicNameDispatchHeadNode(RubyContext context) {
        this.context = context;
        dispatch = new UninitializedDynamicNameDispatchNode(context);
    }

    public Object dispatch(VirtualFrame frame, Object receiverObject, RubySymbol name, RubyProc blockObject, Object[] argumentsObjects) {
        return dispatch.dispatch(frame, receiverObject, name, blockObject, argumentsObjects);
    }

    public Object dispatch(VirtualFrame frame, Object receiverObject, RubyString name, RubyProc blockObject, Object[] argumentsObjects) {
        return dispatch.dispatch(frame, receiverObject, name, blockObject, argumentsObjects);
    }

    public boolean doesRespondTo(VirtualFrame frame, Object receiverObject, RubySymbol name) {
        return dispatch.doesRespondTo(frame, receiverObject, name);
    }

    public boolean doesRespondTo(VirtualFrame frame, Object receiverObject, RubyString name) {
        return dispatch.doesRespondTo(frame, receiverObject, name);
    }

}
