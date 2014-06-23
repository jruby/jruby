package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node.Child;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;

public class CachedSymbolDynamicNameDispatchNode extends DynamicNameDispatchNode {

    private final RubySymbol cachedName;
    @Child protected DispatchHeadNode dispatchHeadNode;
    @Child protected DynamicNameDispatchNode next;

    public CachedSymbolDynamicNameDispatchNode(RubyContext context, RubySymbol cachedName, DynamicNameDispatchNode next) {
        super(context);
        this.cachedName = cachedName;
        dispatchHeadNode = new DispatchHeadNode(context, cachedName.toString(), false, DispatchHeadNode.MissingBehavior.CALL_METHOD_MISSING);
        this.next = next;
    }

    @Override
    public Object dispatch(VirtualFrame frame, Object receiverObject, RubySymbol name, RubyProc blockObject, Object[] argumentsObjects) {
        if (name != cachedName) {
            return next.dispatch(frame, receiverObject, name, blockObject, argumentsObjects);
        }

        return dispatchHeadNode.dispatch(frame, receiverObject, blockObject, argumentsObjects);
    }

    @Override
    public Object dispatch(VirtualFrame frame, Object receiverObject, RubyString name, RubyProc blockObject, Object[] argumentsObjects) {
        return dispatchHeadNode.dispatch(frame, receiverObject, blockObject, argumentsObjects);
    }

    @Override
    public boolean doesRespondTo(VirtualFrame frame, Object receiverObject, RubySymbol name) {
        if (name != cachedName) {
            return next.doesRespondTo(frame, receiverObject, name);
        }

        return dispatchHeadNode.doesRespondTo(frame, receiverObject);
    }

    @Override
    public boolean doesRespondTo(VirtualFrame frame, Object receiverObject, RubyString name) {
        return dispatchHeadNode.doesRespondTo(frame, receiverObject);
    }


}
