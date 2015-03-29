package org.jruby.truffle.pack.nodes.type;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.pack.nodes.SourceNode;
import org.jruby.truffle.runtime.RubyContext;

@NodeChildren({
        @NodeChild(value = "source", type = SourceNode.class),
})
public abstract class ReadFloatNode extends PackNode {

    private final RubyContext context;

    public ReadFloatNode(RubyContext context) {
        this.context = context;
    }

    public ReadFloatNode(ReadFloatNode prev) {
        context = prev.context;
    }

    @Specialization
    public double read(VirtualFrame frame, double[] source) {
        return source[advanceSourcePosition(frame)];
    }

}
