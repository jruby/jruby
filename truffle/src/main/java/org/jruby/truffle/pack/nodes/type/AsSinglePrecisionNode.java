package org.jruby.truffle.pack.nodes.type;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import org.jruby.truffle.pack.nodes.PackNode;

@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class AsSinglePrecisionNode extends PackNode {

    public AsSinglePrecisionNode() {
    }

    public AsSinglePrecisionNode(AsSinglePrecisionNode prev) {
    }

    @Specialization
    public float asFloat(double object) {
        return (float) object;
    }

}
