package org.jruby.truffle.pack.nodes.type;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.pack.nodes.PackNode;

@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class Write8Node extends PackNode {

    @Specialization
    public Object doWrite(VirtualFrame frame, long value) {
        write(frame, (byte) value);
        return null;
    }

}
