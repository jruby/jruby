package org.jruby.runtime;

import org.jruby.RubyArray;
import org.jruby.RubyLocalJumpError;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Create.newArray;

public class NullBlockBody extends BlockBody {
    public NullBlockBody() {
        super(Signature.NO_ARGUMENTS);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject[] args) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, RubyArray.newArrayMayCopy(context.runtime, args), "no block given");
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, RubyArray.newEmptyArray(context.runtime), "no block given");
    }
    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, context.nil, "no block given");
    }
    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, RubyArray.newArray(context.runtime, arg0), "no block given");
    }
    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, context.nil, "no block given");
    }
    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, newArray(context, arg0, arg1), "no block given");
    }
    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, context.nil, "no block given");
    }
    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, RubyArray.newArray(context.runtime, arg0, arg1, arg2), "no block given");
    }
    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, context.nil, "no block given");
    }

    @Override
    protected IRubyObject doYield(ThreadContext context, Block block, IRubyObject value) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, value, "no block given");
    }

    @Override
    protected IRubyObject doYield(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, args[0], "no block given");
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
