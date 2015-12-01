package org.jruby.truffle.format.nodes.read;

import org.jruby.truffle.format.nodes.PackNode;
import org.jruby.truffle.runtime.RubyContext;

import com.oracle.truffle.api.frame.VirtualFrame;

public class LiteralIntegerNode extends PackNode {

    private final int value;

    public LiteralIntegerNode(RubyContext context, int value) {
        super(context);
        this.value = value;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return value;
    }

}
