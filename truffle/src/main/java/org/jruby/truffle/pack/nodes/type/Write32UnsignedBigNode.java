package org.jruby.truffle.pack.nodes.type;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.pack.nodes.SourceNode;

@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class Write32UnsignedBigNode extends PackNode {

    @Specialization
    public Object write(VirtualFrame frame, long value) {
        write(frame,
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value);
        return null;
    }

}
