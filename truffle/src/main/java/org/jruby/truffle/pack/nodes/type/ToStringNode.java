package org.jruby.truffle.pack.nodes.type;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.ByteList;

@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class ToStringNode extends PackNode {

    private final RubyContext context;

    @Child private CallDispatchHeadNode toStrNode;

    public ToStringNode(RubyContext context) {
        this.context = context;
    }

    public ToStringNode(ToStringNode prev) {
        context = prev.context;
        toStrNode = prev.toStrNode;
    }

    public abstract ByteList executeToString(VirtualFrame frame, Object object);

    @Specialization
    public ByteList toString(VirtualFrame frame, RubyString string) {
        return string.getByteList();
    }

    @Specialization(guards = "!isRubyString(object)")
    public ByteList toString(VirtualFrame frame, Object object) {
        if (toStrNode == null) {
            CompilerDirectives.transferToInterpreter();
            toStrNode = insert(DispatchHeadNodeFactory.createMethodCall(context, true));
        }

        final Object value = toStrNode.call(frame, object, "to_str", null);

        if (value instanceof RubyString) {
            return ((RubyString) value).getByteList();
        }

        throw new UnsupportedOperationException();
    }

}
