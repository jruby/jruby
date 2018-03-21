package org.jruby.runtime;

import org.jruby.RubyArray;
import org.jruby.RubyLocalJumpError;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

public class NullBlockBody extends BlockBody {

    private static final String YIELD_CALLED_OUT_OF_BLOCK = "yield called out of block";

    public NullBlockBody() {
        super(Signature.NO_ARGUMENTS);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject[] args, Block blockArg) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, RubyArray.newArrayMayCopy(context.runtime, args), YIELD_CALLED_OUT_OF_BLOCK);
    }
    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, context.runtime.getNil(), YIELD_CALLED_OUT_OF_BLOCK);
    }
    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0, Block blockArg) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, RubyArray.newArray(context.runtime, arg0), YIELD_CALLED_OUT_OF_BLOCK);
    }
    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, context.runtime.getNil(), YIELD_CALLED_OUT_OF_BLOCK);
    }
    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, Block blockArg) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, RubyArray.newArray(context.runtime, arg0, arg1), YIELD_CALLED_OUT_OF_BLOCK);
    }
    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, context.runtime.getNil(), YIELD_CALLED_OUT_OF_BLOCK);
    }
    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block blockArg) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, RubyArray.newArray(context.runtime, arg0, arg1, arg2), YIELD_CALLED_OUT_OF_BLOCK);
    }
    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, context.runtime.getNil(), YIELD_CALLED_OUT_OF_BLOCK);
    }

    @Override
    public IRubyObject yield(ThreadContext context, Block block, IRubyObject value, IRubyObject self, Block blockArg) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, value, YIELD_CALLED_OUT_OF_BLOCK);
    }

    @Override
    public IRubyObject yield(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self, Block blockArg) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, RubyArray.newArrayMayCopy(context.runtime, args), YIELD_CALLED_OUT_OF_BLOCK);
    }

    @Override
    public StaticScope getStaticScope() {
        return null;
    }

    public void setStaticScope(StaticScope newScope) {
    }

    public String getFile() {
        return null;
    }

    public int getLine() {
        return -1;
    }
}
