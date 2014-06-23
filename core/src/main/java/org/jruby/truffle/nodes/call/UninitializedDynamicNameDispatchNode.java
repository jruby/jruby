package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;

public class UninitializedDynamicNameDispatchNode extends DynamicNameDispatchNode {

    public UninitializedDynamicNameDispatchNode(RubyContext context) {
        super(context);
    }

    @Override
    public Object dispatch(VirtualFrame frame, Object receiverObject, RubySymbol name, RubyProc blockObject, Object[] argumentsObjects) {
        return extendCache(name).dispatch(frame, receiverObject, name, blockObject, argumentsObjects);
    }

    @Override
    public Object dispatch(VirtualFrame frame, Object receiverObject, RubyString name, RubyProc blockObject, Object[] argumentsObjects) {
        return extendCache(name).dispatch(frame, receiverObject, name, blockObject, argumentsObjects);
    }

    @Override
    public boolean doesRespondTo(VirtualFrame frame, Object receiverObject, RubySymbol name) {
        return extendCache(name).doesRespondTo(frame, receiverObject, name);
    }

    @Override
    public boolean doesRespondTo(VirtualFrame frame, Object receiverObject, RubyString name) {
        return extendCache(name).doesRespondTo(frame, receiverObject, name);
    }

    private DynamicNameDispatchNode extendCache(RubySymbol name) {
        CompilerDirectives.transferToInterpreter();

        final DynamicNameDispatchNode newNode = new CachedSymbolDynamicNameDispatchNode(context, name, this);
        replace(newNode, "appending new cache entry to dynamic name dispatch chain");
        return newNode;
    }

    private DynamicNameDispatchNode extendCache(RubyString name) {
        CompilerDirectives.transferToInterpreter();

        final DynamicNameDispatchNode newNode = new CachedStringDynamicNameDispatchNode(context, name, this);
        replace(newNode, "appending new cache entry to dynamic name dispatch chain");
        return newNode;
    }

}
