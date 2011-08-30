package org.jruby.interpreter;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyLocalJumpError.Reason;
import org.jruby.RubyModule;
import org.jruby.ast.Node;
import org.jruby.ast.RootNode;
import org.jruby.compiler.ir.IRBuilder;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRScript;
import org.jruby.compiler.ir.instructions.ReturnInstr;
import org.jruby.compiler.ir.instructions.BREAK_Instr;
import org.jruby.compiler.ir.instructions.THROW_EXCEPTION_Instr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.RubyEvent;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class Interpreter {

    private static final Logger LOG = LoggerFactory.getLogger("Interpreter");

    public static IRubyObject interpret(Ruby runtime, Node rootNode, IRubyObject self) {
        IRScope scope = new IRBuilder().buildRoot((RootNode) rootNode);
        scope.prepareForInterpretation();
//        scope.runCompilerPass(new CallSplitter());

        return interpretTop(runtime, scope, self);
    }

    private static int interpInstrsCount = 0;

    public static boolean isDebug() {
        return RubyInstanceConfig.IR_DEBUG;
    }

    public static IRubyObject interpretTop(Ruby runtime, IRScope scope, IRubyObject self) {
        assert scope instanceof IRScript : "Must be an IRScript scope at Top!!!";

        IRScript root = (IRScript) scope;

        // We get the live object ball rolling here.  This give a valid value for the top
        // of this lexical tree.  All new scope can then retrieve and set based on lexical parent.
        if (root.getStaticScope().getModule() == null) { // If an eval this may already be setup.
            root.getStaticScope().setModule(runtime.getObject());
        }

        RubyModule currModule = root.getStaticScope().getModule();
        IRMethod rootMethod = root.getRootClass().getRootMethod();
        InterpretedIRMethod method = new InterpretedIRMethod(rootMethod, currModule);
        ThreadContext context = runtime.getCurrentContext();

        try {
            IRubyObject rv =  method.call(context, self, currModule, "", IRubyObject.NULL_ARRAY);
            if (isDebug()) LOG.debug("-- Interpreted instructions: {}", interpInstrsCount);
            return rv;
        } catch (IRBreakJump bj) {
            throw runtime.newLocalJumpError(Reason.BREAK, (IRubyObject)bj.breakValue, "unexpected break");
        }
    }

    public static IRubyObject interpret(ThreadContext context, IRubyObject self, CFG cfg, InterpreterContext interp) {
        Ruby runtime = context.getRuntime();
        boolean inClosure = (cfg.getScope() instanceof IRClosure);
        
        try {
            interp.setMethodExitLabel(cfg.getExitBB().getLabel()); // used by return and break instructions!

            Instr[] instrs = cfg.prepareForInterpretation();
            int n   = instrs.length;
            int ipc = 0;
            Instr lastInstr = null;
            while (ipc < n) {
                interpInstrsCount++;
                lastInstr = instrs[ipc];
                
                if (isDebug()) LOG.debug("I: {}", lastInstr);

                try {
                    Label jumpTarget = lastInstr.interpret(interp, context, self);
                    ipc = (jumpTarget == null) ? ipc + 1 : jumpTarget.getTargetPC();
                }
                catch (IRBreakJump bj) {
                    //System.out.println("last: " + lastInstr + "; inclosure; " + inClosure + "; proc: " + interp.inProc() + "; lambda: " + interp.inLambda());
                    if (lastInstr instanceof THROW_EXCEPTION_Instr) throw bj; // pass it along if we just executed a throw!

                    if ((lastInstr instanceof BREAK_Instr) && (!inClosure || interp.inProc()))
                        throw runtime.newLocalJumpError(Reason.BREAK, (IRubyObject)bj.breakValue, "unexpected break");

                    if (interp.inLambda()) {
                        // Lambda special case.  We are in a lambda and breaking out of it requires popping out exactly one level up.
                        if (lastInstr instanceof BREAK_Instr) {
                            bj.caughtByLambda = true;
                            throw bj;
                        }
                        else {
                            // We just unwound all the way up because of a non-local break
                            throw runtime.newLocalJumpError(Reason.BREAK, (IRubyObject)bj.breakValue, "unexpected break");
                        }
                    } else if (bj.caughtByLambda || (bj.scopeToReturnTo == cfg.getScope())) {
                        // We got where we need to get to (because a lambda stopped us, or because we popped to the
                        // lexical scope where we got called from).  Retrieve the result and store it.
                        Operand r = lastInstr.getResult();
                        if (r != null) r.store(interp, context, self, bj.breakValue);
                        ipc += 1;
                    }
                    else {
                        // We need to continue to break upwards.
                        // Run any ensures we need to run before breaking up. 
                        // Quite easy to do this by passing 'bj' as the exception to the ensure block!
                        ipc = cfg.getEnsurerPC(lastInstr);
                        if (ipc == -1) throw bj; // No ensure block here, just rethrow bj
                        interp.setException(bj); // Found an ensure block, set 'bj' as the exception and transfer control
                    }
                }
                catch (RaiseException re) {
                    if (isDebug()) LOG.debug("caught raise exception: " + re.getException() + "; excepting instr: " + lastInstr);
                    if (lastInstr instanceof THROW_EXCEPTION_Instr) throw re; // pass it along if we just executed a throw!

                    ipc = cfg.getRescuerPC(lastInstr);
                    if (isDebug()) LOG.debug("ipc for rescuer: " + ipc);
                    if (ipc == -1) throw re; // No one rescued exception, pass it on!

                    interp.setException(re.getException());
                }
                catch (Error e) {
                    if (lastInstr instanceof THROW_EXCEPTION_Instr) throw e; // pass it along if we just executed a throw!

                    ipc = cfg.getEnsurerPC(lastInstr);
                    if (ipc == -1) throw e; // No ensure block here, pass it on! 
                    interp.setException(e);
                }
            }

            // If a closure, and lastInstr was a return, have to return from the nearest method!
            IRubyObject rv = (IRubyObject) interp.getReturnValue(context);

            if (lastInstr instanceof ReturnInstr && inClosure && !interp.inLambda()) {
                throw new IRReturnJump(((ReturnInstr)lastInstr).methodToReturnFrom, rv);
            }

            return rv;
        } catch (IRReturnJump rj) {
            // - If we are in a lambda, stop propagating
            // - If not in a lambda
            //   - if in a closure, pass it along
            //   - if not in a closure, we got this return jump from a closure further up the call stack.
            //     So, continue popping the call stack till we get to the right method
            if (!interp.inLambda() && (inClosure || (rj.methodToReturnFrom != cfg.getScope()))) throw rj; // pass it along

            return (IRubyObject) rj.returnValue;
        } finally {
            if (interp.getFrame() != null) {
                context.popFrame();
                interp.setFrame(null);
            }
            
            if (interp.hasAllocatedDynamicScope()) context.postMethodScopeOnly();
        }
    }

    public static IRubyObject INTERPRET_METHOD(ThreadContext context, CFG cfg, 
        InterpreterContext interp, IRubyObject self, String name, RubyModule implClass, boolean isTraceable) {
        Ruby runtime = context.getRuntime();
        boolean syntheticMethod = name == null || name.equals("");
        
        try {
            String className = implClass.getName();
            if (!syntheticMethod) ThreadContext.pushBacktrace(context, className, name, context.getFile(), context.getLine());
            if (isTraceable) methodPreTrace(runtime, context, name, implClass);
            return interpret(context, self, cfg, interp);
        } finally {
            if (isTraceable) {
                try {methodPostTrace(runtime, context, name, implClass);}
                finally { if (!syntheticMethod) ThreadContext.popBacktrace(context);}
            } else {
                if (!syntheticMethod) ThreadContext.popBacktrace(context);
            }
        }
    }

    private static void methodPreTrace(Ruby runtime, ThreadContext context, String name, RubyModule implClass) {
        if (runtime.hasEventHooks()) context.trace(RubyEvent.CALL, name, implClass);
    }

    private static void methodPostTrace(Ruby runtime, ThreadContext context, String name, RubyModule implClass) {
        if (runtime.hasEventHooks()) context.trace(RubyEvent.RETURN, name, implClass);
    }
}
