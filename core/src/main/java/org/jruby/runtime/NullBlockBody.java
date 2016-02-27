package org.jruby.runtime;

import org.jruby.RubyLocalJumpError;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

public class NullBlockBody extends BlockBody {
    public NullBlockBody() {
        super(Signature.NO_ARGUMENTS);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject[] args) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, context.runtime.newArrayNoCopy(args), "yield called out of block");
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, context.runtime.newArrayNoCopy(IRubyObject.NULL_ARRAY), "yield called out of block");
    }
    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, context.runtime.getNil(), "yield called out of block");
    }
    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, context.runtime.newArrayNoCopy(new IRubyObject[]{arg0}), "yield called out of block");
    }
    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, context.runtime.getNil(), "yield called out of block");
    }
    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, context.runtime.newArrayNoCopy(new IRubyObject[]{arg0, arg1}), "yield called out of block");
    }
    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, context.runtime.getNil(), "yield called out of block");
    }
    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, context.runtime.newArrayNoCopy(new IRubyObject[]{arg0, arg1, arg2}), "yield called out of block");
    }
    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, context.runtime.getNil(), "yield called out of block");
    }

    @Override
    protected IRubyObject doYield(ThreadContext context, Block block, IRubyObject value) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, value, "yield called out of block");
    }

    @Override
    protected IRubyObject doYield(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self) {
        throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, args[0], "yield called out of block");
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
