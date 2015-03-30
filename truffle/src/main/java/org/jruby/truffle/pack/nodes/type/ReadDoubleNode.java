package org.jruby.truffle.pack.nodes.type;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.pack.nodes.SourceNode;
import org.jruby.truffle.runtime.RubyContext;

@NodeChildren({
        @NodeChild(value = "source", type = SourceNode.class),
})
public abstract class ReadDoubleNode extends PackNode {

    private final RubyContext context;

    public ReadDoubleNode(RubyContext context) {
        this.context = context;
    }

    public ReadDoubleNode(ReadDoubleNode prev) {
        context = prev.context;
    }

    @Specialization
    public double read(VirtualFrame frame, int[] source) {
        return source[advanceSourcePosition(frame)];
    }

    @Specialization
    public double read(VirtualFrame frame, long[] source) {
        return source[advanceSourcePosition(frame)];
    }

    @Specialization
    public double read(VirtualFrame frame, double[] source) {
        return source[advanceSourcePosition(frame)];
    }

}
