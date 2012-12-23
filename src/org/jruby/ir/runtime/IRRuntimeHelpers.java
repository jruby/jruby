package org.jruby.ir.runtime;

import org.jruby.RubyInstanceConfig;
import org.jruby.exceptions.RaiseException;
import org.jruby.ir.IREvalScript;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.operands.IRException;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class IRRuntimeHelpers {
    private static final Logger LOG = LoggerFactory.getLogger("IRRuntimeHelpers");

    public static boolean inProfileMode() {
        return RubyInstanceConfig.IR_PROFILE;
    }

    public static boolean isDebug() {
        return RubyInstanceConfig.IR_DEBUG;
    }

    public static boolean inNonMethodBodyLambda(IRScope scope, Block.Type blockType) {
        // SSS FIXME: Hack! AST interpreter and JIT compiler marks a proc's static scope as
        // an argument scope if it is used to define a method's body via :define_method.
        // Since that is exactly what we want to figure out here, am just using that flag here.
        // But, this is ugly (as is the original hack in the current runtime).  What is really
        // needed is a new block type -- a block that is used to define a method body.
        return blockType == Block.Type.LAMBDA && !scope.getStaticScope().isArgumentScope();
    }

    public static boolean inLambda(Block.Type blockType) {
        return blockType == Block.Type.LAMBDA;
    }

    public static boolean inProc(Block.Type blockType) {
        return blockType == Block.Type.PROC;
    }

    /*
     * Handle non-local returns (ex: when nested in closures, root scopes of module/class/sclass bodies)
     */
    public static void handleNonLocalReturn(ThreadContext context, IRScope scope, IRMethod methodToReturnFrom, IRubyObject returnValue) {
        if (scope instanceof IRClosure) {
            if (methodToReturnFrom == null) {
                // SSS FIXME: As Tom correctly pointed out, this is not correct.  The example that breaks this code is:
                //
                //      jruby -X-CIR -e "Thread.new { Proc.new { return }.call }.join"
                //
                // This should report a LocalJumpError, not a ThreadError.
                //
                // The right fix would involve checking the closure to see who it is associated with.
                // If it is a thread-body, it would be a ThreadError.  If not, it would be a local-jump-error
                // This requires having access to the block -- same requirement as in handleBreakJump.
                if (context.getThread() == context.runtime.getThreadService().getMainThread()) {
                    throw IRException.RETURN_LocalJumpError.getException(context.runtime);
                } else {
                    throw context.runtime.newThreadError("return can't jump across threads");
                }
            }

            // Cannot return to the call that we have long since exited.
            if (!context.scopeExistsOnCallStack(methodToReturnFrom.getStaticScope())) {
                if (isDebug()) LOG.info("in scope: " + scope + ", raising unexpected return local jump error");
                throw IRException.RETURN_LocalJumpError.getException(context.runtime);
            }
        }

        // methodtoReturnFrom will not be null for explicit returns from class/module/sclass bodies
        throw IRReturnJump.create(methodToReturnFrom, returnValue);
    }

    public static IRubyObject handleReturnJump(IRScope scope, IRReturnJump rj, Block.Type blockType) throws IRReturnJump {
        // - If we are in a lambda or if we are in the method scope we are supposed to return from, stop propagating
        if (inNonMethodBodyLambda(scope, blockType) || (rj.methodToReturnFrom == scope)) return (IRubyObject) rj.returnValue;

        // - If not, Just pass it along!
        throw rj;
    }

    public static void initiateBreak(ThreadContext context, IRScope scope, IRBreakJump bj, IRubyObject self, Block.Type blockType) throws RaiseException, IRBreakJump {
        if (!(scope instanceof IRClosure)) {
            // Error -- breaks can only be initiated in closures
            throw IRException.BREAK_LocalJumpError.getException(context.runtime);
        }

        if (inProc(blockType)) {
            // SSS FIXME: Here we need to check if the current executing block has escaped
            // which means the block has to be passed in from Block.call -> BlockBody.call -> Interpreter.interpret -> here
            // or it has to be set in context from where we can retrieve it.
        } else if (inLambda(blockType)) {
            bj.caughtByLambda = true;
        } else if (scope instanceof IREvalScript) {
            // If we are in an eval, record it so we can account for it
            bj.breakInEval = true;
        }

        // Start the process of breaking through the intermediate scopes
        throw bj;
    }

    public static void handlePropagatedBreak(ThreadContext context, IRScope scope, IRBreakJump bj, IRubyObject self, Block.Type blockType) throws RaiseException, IRBreakJump {
        if (bj.breakInEval) {
            // If the break was in an eval, we pretend as if it was in the containing scope
            bj.breakInEval = false;
            initiateBreak(context, scope, bj, self, blockType);
        } else if (inNonMethodBodyLambda(scope, blockType)) {
            // We just unwound all the way up because of a non-local break
            throw IRException.BREAK_LocalJumpError.getException(context.getRuntime());
        } else if (!bj.caughtByLambda && (bj.scopeToReturnTo != scope)) {
            // Propagate
            throw bj;
        }

        // If we got here, the break has been handled!
    }
};
