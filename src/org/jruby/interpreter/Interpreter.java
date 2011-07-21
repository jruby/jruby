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
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.RubyEvent;


public class Interpreter {
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

        IRubyObject rv =  method.call(context, self, currModule, "", IRubyObject.NULL_ARRAY);
        if (isDebug()) System.out.println("-- Interpreted " + interpInstrsCount + " instructions");

        return rv;
    }

    public static IRubyObject interpret(ThreadContext context, CFG cfg, InterpreterContext interp) {
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
                
                if (isDebug()) System.out.println("I: " + lastInstr);
                
                try {
                    Label jumpTarget = lastInstr.interpret(interp);
                    ipc = (jumpTarget == null) ? ipc + 1 : jumpTarget.getTargetPC();
                }
                // SSS FIXME: This only catches Ruby exceptions
                // What about Java exceptions?
                catch (org.jruby.exceptions.RaiseException re) {
                    ipc = cfg.getRescuerPC(lastInstr);

                    if (ipc == -1) throw re; // No one rescued exception, pass it on!

                    interp.setException(re.getException());
                }
            }

            // If a closure, and lastInstr was a return, have to return from the nearest method!
            IRubyObject rv = (IRubyObject) interp.getReturnValue();

            if (lastInstr instanceof ReturnInstr && inClosure && !interp.inLambda()) {
                throw new IRReturnJump(((ReturnInstr)lastInstr).methodToReturnFrom, rv);
            }
            // If a closure, and lastInstr was a break, have to return from the nearest closure!
            else if (lastInstr instanceof BREAK_Instr) {
                if (!inClosure) throw runtime.newLocalJumpError(Reason.BREAK, rv, "unexpected break");

                RuntimeHelpers.breakJump(context, rv);
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
            InterpreterContext interp, String name, RubyModule implClass, boolean isTraceable) {
        Ruby runtime = interp.getRuntime();
        boolean syntheticMethod = name == null || name.equals("");
        
        try {
            String className = implClass.getName();
            if (!syntheticMethod) ThreadContext.pushBacktrace(context, className, name, context.getFile(), context.getLine());
            if (isTraceable) methodPreTrace(runtime, context, name, implClass);
            return interpret(context, cfg, interp);
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
