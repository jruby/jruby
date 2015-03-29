package org.jruby.truffle.pack.nodes.type;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.dispatch.DispatchNode;
import org.jruby.truffle.nodes.dispatch.MissingBehavior;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.pack.runtime.NoImplicitConversionException;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyNilClass;

@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class AsLongNode extends PackNode {

    public AsLongNode() {
    }

    public AsLongNode(AsLongNode prev) {
    }

    @Specialization
    public long asLong(float object) {
        return (long) Float.floatToIntBits(object);
    }

    @Specialization
    public long asLong(double object) {
        return Double.doubleToLongBits(object);
    }

}
