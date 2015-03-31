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
import org.jruby.util.ByteList;

@NodeChildren({
        @NodeChild(value = "source", type = SourceNode.class),
})
public abstract class ReadStringNode extends PackNode {

    private final RubyContext context;

    @Child private ToStringNode toStringNode;

    public ReadStringNode(RubyContext context) {
        this.context = context;
    }

    public ReadStringNode(ReadStringNode prev) {
        context = prev.context;
        toStringNode = prev.toStringNode;
    }

    @Specialization(guards = "isNull(source)")
    public long read(VirtualFrame frame, Object source) {
        CompilerDirectives.transferToInterpreter();

        // Advance will handle the error
        advanceSourcePosition(frame);

        throw new IllegalStateException();
    }

    @Specialization
    public Object read(VirtualFrame frame, int[] source) {
        return readAndConvert(frame, source[advanceSourcePosition(frame)]);
    }

    @Specialization
    public Object read(VirtualFrame frame, long[] source) {
        return readAndConvert(frame, source[advanceSourcePosition(frame)]);
    }

    @Specialization
    public Object read(VirtualFrame frame, double[] source) {
        return readAndConvert(frame, source[advanceSourcePosition(frame)]);
    }

    @Specialization(guards = "!isIRubyArray(source)")
    public Object read(VirtualFrame frame, Object[] source) {
        return readAndConvert(frame, source[advanceSourcePosition(frame)]);
    }

    private Object readAndConvert(VirtualFrame frame, Object value) {
        if (toStringNode == null) {
            CompilerDirectives.transferToInterpreter();
            toStringNode = insert(ToStringNodeGen.create(context, new NullNode()));
        }

        return toStringNode.executeToString(frame, value);
    }

    @Specialization
    public ByteList read(VirtualFrame frame, IRubyObject[] source) {
        return toString(source[advanceSourcePosition(frame)]);
    }

    @CompilerDirectives.TruffleBoundary
    private ByteList toString(IRubyObject object) {
        return object.convertToString().getByteList();
    }

}
