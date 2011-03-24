package org.jruby.interpreter;

import org.jruby.Ruby;
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
import org.jruby.compiler.ir.instructions.CallInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.RubyEvent;


public class Interpreter {
    private static boolean debug = Boolean.parseBoolean(System.getProperty("jruby.ir.debug", "false"));
    
    public static IRubyObject interpret(Ruby runtime, Node rootNode, IRubyObject self) {
        IRScope scope = new IRBuilder().buildRoot((RootNode) rootNode);
        scope.prepareForInterpretation();
//        scope.runCompilerPass(new CallSplitter());

        return interpretTop(runtime, scope, self);
    }

    private static int interpInstrsCount = 0;

    public static boolean isDebug() {
        return debug;
    }

    public static IRubyObject interpretTop(Ruby runtime, IRScope scope, IRubyObject self) {
        assert scope instanceof IRScript : "Must be an IRScript scope at Top!!!";

        IRScript root = (IRScript) scope;

        // We get the live object ball rolling here.  This give a valid value for the top
        // of this lexical tree.  All new scope can then retrieve and set based on lexical
        // parent.
        if (root.getStaticScope().getModule() == null) { // If an eval this may already be setup.
            root.getStaticScope().setModule(runtime.getObject());
        }

        IRMethod rootMethod = root.getRootClass().getRootMethod();
        RubyModule metaclass = self.getMetaClass();

        InterpretedIRMethod method = new InterpretedIRMethod(rootMethod, metaclass);

        IRubyObject rv =  method.call(runtime.getCurrentContext(), self, metaclass, "", new IRubyObject[]{});
        if (debug) {
            System.out.println("-- Interpreted " + interpInstrsCount + " instructions");
        }
        return rv;
    }

    public static IRubyObject interpret(ThreadContext context, CFG cfg, InterpreterContext interp) {
        boolean inClosure = (cfg.getScope() instanceof IRClosure);
        try {
            interp.setMethodExitLabel(cfg.getExitBB().getLabel()); // used by return and break instructions!

            IRubyObject self = (IRubyObject) interp.getSelf();
            Instr[] instrs = cfg.prepareForInterpretation();
            int n   = instrs.length;
            int ipc = 0;
            Instr lastInstr = null;
            while (ipc < n) {
                interpInstrsCount++;
                lastInstr = instrs[ipc];
                
                if (debug) System.out.println("EXEC'ing: " + lastInstr);
                
                try {
                    Label jumpTarget = lastInstr.interpret(interp, self);
                    ipc = (jumpTarget == null) ? ipc + 1 : jumpTarget.getTargetPC();
                }
                // SSS FIXME: This only catches Ruby exceptions
                // What about Java exceptions?
                catch (org.jruby.exceptions.RaiseException re) {
                    ipc = cfg.getRescuerPC(lastInstr);
                    // If no one rescues this exception, pass it along!
                    if (ipc == -1)
                        throw re;
                    else
                        interp.setException(re.getException());
                }
            }

            // If I am in a closure, and lastInstr was a return, have to return from the nearest method!
            IRubyObject rv = (IRubyObject) interp.getReturnValue();
            if (lastInstr instanceof ReturnInstr) {
                if (inClosure)
                    throw RuntimeHelpers.returnJump(rv, context);
            }
            // If I am in a closure, and lastInstr was a break, have to return from the nearest closure!
            else if (lastInstr instanceof BREAK_Instr) {
                if (inClosure)
                    RuntimeHelpers.breakJump(context, rv);
                else
                    throw context.getRuntime().newLocalJumpError(org.jruby.RubyLocalJumpError.Reason.BREAK, rv, "unexpected break");
            }

            return rv;
        } catch (org.jruby.exceptions.JumpException.ReturnJump rj) {
            if (inClosure) // pass it along!
                throw rj;
            else
                return (IRubyObject)rj.getValue();
        } finally {
            if (interp.getFrame() != null) {
                context.popFrame();
                interp.setFrame(null);
            }
            if (interp.hasAllocatedDynamicScope()) 
                context.postMethodScopeOnly();
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
