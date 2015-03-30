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
public abstract class ReadLongNode extends PackNode {

    private final RubyContext context;

    @Child private ToLongNode toLongNode;

    public ReadLongNode(RubyContext context) {
        this.context = context;
    }

    public ReadLongNode(ReadLongNode prev) {
        context = prev.context;
        toLongNode = prev.toLongNode;
    }

    @Specialization(guards = "isNull(source)")
    public long read(VirtualFrame frame, Object source) {
        CompilerDirectives.transferToInterpreter();

        // Advance will handle the error
        advanceSourcePosition(frame);

        throw new IllegalStateException();
    }

    @Specialization
    public long read(VirtualFrame frame, int[] source) {
        return source[advanceSourcePosition(frame)];
    }

    @Specialization
    public long read(VirtualFrame frame, long[] source) {
        return source[advanceSourcePosition(frame)];
    }

    @Specialization
    public long read(VirtualFrame frame, double[] source) {
        return (long) source[advanceSourcePosition(frame)];
    }

    @Specialization(guards = "!isIRubyArray(source)")
    public long read(VirtualFrame frame, Object[] source) {
        if (toLongNode == null) {
            CompilerDirectives.transferToInterpreter();
            toLongNode = insert(ToLongNodeGen.create(context, new NullNode()));
        }

        return toLongNode.executeToLong(frame, source[advanceSourcePosition(frame)]);
    }

    @Specialization
    public long read(VirtualFrame frame, IRubyObject[] source) {
        return toLong(source[advanceSourcePosition(frame)]);
    }

    @CompilerDirectives.TruffleBoundary
    private long toLong(IRubyObject object) {
        return object.convertToInteger().getLongValue();
    }

}
