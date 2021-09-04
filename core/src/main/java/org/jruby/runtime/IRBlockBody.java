package org.jruby.runtime;

import org.jruby.RubyArray;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class IRBlockBody extends ContextAwareBlockBody {
    protected final String fileName;
    protected final int lineNumber;

    // For interpreter IR
    public IRBlockBody(IRScope closure, Signature signature) {
        // ThreadLocal not set by default to avoid having many thread-local values initialized
        // servers such as Tomcat tend to do thread-local checks when un-deploying apps,
        // for JRuby leads to 100s of SEVERE warnings for a mid-size (booted) Rails app
        this(closure.getStaticScope(), closure.getFile(), closure.getLine(), signature);
    }

    // For Compiled.
    public IRBlockBody(StaticScope scope, String file, int line, Signature signature) {
        // ThreadLocal not set by default to avoid having many thread-local values initialized
        // servers such as Tomcat tend to do thread-local checks when un-deploying apps,
        // for JRuby leads to 100s of SEVERE warnings for a mid-size (booted) Rails app
        super(scope, signature);
        this.fileName = file;
        this.lineNumber = line;
    }

    @Override
    public abstract boolean canCallDirect();

    @Override
    public IRubyObject call(ThreadContext context, Block block) {
        return call(context, block, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0) {
        return call(context, block, new IRubyObject[] {arg0}, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1) {
        return call(context, block, new IRubyObject[] {arg0, arg1}, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return call(context, block, new IRubyObject[]{arg0, arg1, arg2}, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject[] args) {
        return call(context, block, args, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject[] args, Block blockArg) {
        if (canCallDirect()) {
            return callDirect(context, block, args, blockArg);
        }
        return commonYieldPath(context, block, Block.Type.PROC, prepareArgumentsForCall(context, args, block.type), null, blockArg);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block) {
        if (canCallDirect()) {
            return yieldDirect(context, block, null, null);
        } else {
            IRubyObject[] args = IRubyObject.NULL_ARRAY;
            if (block.type == Block.Type.LAMBDA) signature.checkArity(context.runtime, args);
            return commonYieldPath(context, block, Block.Type.NORMAL, args, null, Block.NULL_BLOCK);
        }
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0) {
        IRubyObject[] args;
        if (canCallDirect()) {
            if (arg0 instanceof RubyArray) { // Unwrap the array arg
                args = IRRuntimeHelpers.convertValueIntoArgArray(context, (RubyArray) arg0, signature);
            } else {
                args = new IRubyObject[] { arg0 };
            }
            return yieldDirect(context, block, args, null);
        } else {
            if (arg0 instanceof RubyArray) { // Unwrap the array arg
                args = IRRuntimeHelpers.convertValueIntoArgArray(context, (RubyArray) arg0, signature);

                // FIXME: arity error is against new args but actual error shows arity of original args.
                if (block.type == Block.Type.LAMBDA) signature.checkArity(context.runtime, args);

                return commonYieldPath(context, block, Block.Type.NORMAL, args, null, Block.NULL_BLOCK);
            } else {
                return doYield(context, block, arg0);
            }
        }
    }

    private IRubyObject yieldSpecificMultiArgsCommon(ThreadContext context, Block block, IRubyObject... args) {
        int blockArity = signature.arityValue();
        if (blockArity == 1) {
            args = new IRubyObject[] { RubyArray.newArrayMayCopy(context.runtime, args) };
        }

        if (canCallDirect()) return yieldDirect(context, block, args, null);

        if (blockArity == 0) {
            args = IRubyObject.NULL_ARRAY; // discard args
        }
        if (block.type == Block.Type.LAMBDA) signature.checkArity(context.runtime, args);

        return commonYieldPath(context, block, Block.Type.NORMAL, args, null, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1) {
        return yieldSpecificMultiArgsCommon(context, block, arg0, arg1);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return yieldSpecificMultiArgsCommon(context, block, arg0, arg1, arg2);
    }

    public static IRubyObject[] toAry(ThreadContext context, IRubyObject value) {
        final IRubyObject ary = Helpers.aryToAry(context, value);

        if (ary instanceof RubyArray) return ((RubyArray) ary).toJavaArray();

        if (ary == context.nil) return new IRubyObject[] { value };

        throw context.runtime.newTypeError(value.getType().getName() + "#to_ary should return Array");
    }

    protected IRubyObject doYieldLambda(ThreadContext context, Block block, IRubyObject value) {
        // Lambda does not splat arrays even if a rest arg is present when it wants a single parameter filled.
        IRubyObject[] args;

        if (value == null) { // no args case from BlockBody.yieldSpecific
            args = IRubyObject.NULL_ARRAY;
        } else if (signature.required() == 1 || signature.arityValue() == -1) {
            args = new IRubyObject[] { value };
        } else {
            args = toAry(context, value);
        }

        signature.checkArity(context.runtime, args);

        return commonYieldPath(context, block, Block.Type.NORMAL, args, null, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject doYield(ThreadContext context, Block block, IRubyObject value) {
        if (block.type == Block.Type.LAMBDA) return doYieldLambda(context, block, value);

        IRubyObject[] args;
        if (value == null) { // no args case from BlockBody.yieldSpecific
            args = IRubyObject.NULL_ARRAY;
        } else if (!signature.hasKwargs() && !signature.isSpreadable()) {
            args = new IRubyObject[] { value };
        } else {
            args = toAry(context, value);
        }

        return commonYieldPath(context, block, Block.Type.NORMAL, args, null, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject doYield(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self) {
        if (block.type == Block.Type.LAMBDA) signature.checkArity(context.runtime, args);

        return commonYieldPath(context, block, Block.Type.NORMAL, args, self, Block.NULL_BLOCK);
    }

    protected abstract IRubyObject commonYieldPath(ThreadContext context, Block block, Block.Type type, IRubyObject[] args, IRubyObject self, Block blockArg) ;

    static void postYield(ThreadContext context, InterpreterContext ic, Binding binding, Visibility oldVis, Frame prevFrame) {
        // IMPORTANT: Do not clear eval-type in case this is reused in bindings!
        // Ex: eval("...", foo.instance_eval { binding })
        // The dyn-scope used for binding needs to have its eval-type set to INSTANCE_EVAL
        binding.getFrame().setVisibility(oldVis);
        if (ic.popDynScope()) {
            context.postYield(binding, prevFrame);
        } else {
            context.postYieldNoScope(prevFrame);
        }
    }

    public IRClosure getScope() {
        return (IRClosure) scope.getIRScope();
    }

    @Override
    public String getFile() {
        return fileName;
    }

    @Override
    public int getLine() {
        return lineNumber;
    }
}
