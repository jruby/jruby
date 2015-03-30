package org.jruby.truffle.pack.nodes.type;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.runtime.core.RubyBignum;

@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class WriteBERNode extends PackNode {

    @Specialization
    public Object doWrite(VirtualFrame frame, long value) {
        writeBytes(frame, (byte) value);
        return null;
    }

    @Specialization
    public Object doWrite(VirtualFrame frame, RubyBignum value) {
        writeBytes(frame, (byte) 0);
        return null;
    }

}
