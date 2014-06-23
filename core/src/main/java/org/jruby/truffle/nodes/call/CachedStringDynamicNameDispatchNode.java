package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;

public class CachedStringDynamicNameDispatchNode extends DynamicNameDispatchNode {

    private final RubyString cachedName;
    @Child protected DispatchHeadNode dispatchHeadNode;
    @Child protected DynamicNameDispatchNode next;

    public CachedStringDynamicNameDispatchNode(RubyContext context, RubyString cachedName, DynamicNameDispatchNode next) {
        super(context);
        this.cachedName = cachedName;
        dispatchHeadNode = new DispatchHeadNode(context, cachedName.toString(), false, DispatchHeadNode.MissingBehavior.CALL_METHOD_MISSING);
        this.next = next;
    }

    @Override
    public Object dispatch(VirtualFrame frame, Object receiverObject, RubySymbol name, RubyProc blockObject, Object[] argumentsObjects) {
        return dispatchHeadNode.dispatch(frame, receiverObject, blockObject, argumentsObjects);
    }

    @Override
    public Object dispatch(VirtualFrame frame, Object receiverObject, RubyString name, RubyProc blockObject, Object[] argumentsObjects) {
        // TODO(CS): how to compare strings efficiently?
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean doesRespondTo(VirtualFrame frame, Object receiverObject, RubySymbol name) {
        return dispatchHeadNode.doesRespondTo(frame, receiverObject);
    }

    @Override
    public boolean doesRespondTo(VirtualFrame frame, Object receiverObject, RubyString name) {
        // TODO(CS): how to compare strings efficiently?
        throw new UnsupportedOperationException();
    }


}
