package org.jruby.truffle.pack.nodes.type;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.pack.nodes.SourceNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBignum;

@NodeChildren({
        @NodeChild(value = "source", type = SourceNode.class),
})
public abstract class ReadLongOrBigIntegerNode extends PackNode {

    private final RubyContext context;

    @Child private ToLongNode toLongNode;

    private final ConditionProfile bignumProfile = ConditionProfile.createBinaryProfile();

    public ReadLongOrBigIntegerNode(RubyContext context) {
        this.context = context;
    }

    public ReadLongOrBigIntegerNode(ReadLongOrBigIntegerNode prev) {
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
    public Object read(VirtualFrame frame, Object[] source) {
        final Object value = source[advanceSourcePosition(frame)];

        if (bignumProfile.profile(value instanceof RubyBignum)) {
            return ((RubyBignum) value).bigIntegerValue();
        } else {
            if (toLongNode == null) {
                CompilerDirectives.transferToInterpreter();
                toLongNode = insert(ToLongNodeGen.create(context, new NullNode()));
            }

            return toLongNode.executeToLong(frame, value);
        }
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
