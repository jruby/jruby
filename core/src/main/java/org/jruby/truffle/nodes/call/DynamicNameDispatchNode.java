package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;

public abstract class DynamicNameDispatchNode extends Node {

    protected final RubyContext context;

    public DynamicNameDispatchNode(RubyContext context) {
        this.context = context;
    }

    public abstract Object dispatch(VirtualFrame frame, Object receiverObject, RubySymbol name, RubyProc blockObject, Object[] argumentsObjects);

    public abstract Object dispatch(VirtualFrame frame, Object receiverObject, RubyString name, RubyProc blockObject, Object[] argumentsObjects);

    public abstract boolean doesRespondTo(VirtualFrame frame, Object receiverObject, RubySymbol name);

    public abstract boolean doesRespondTo(VirtualFrame frame, Object receiverObject, RubyString name);

}
