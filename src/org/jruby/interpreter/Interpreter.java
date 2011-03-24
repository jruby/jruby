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

/**
 * SSS: This skeleton of this code is quite stale now.  But, the inlining part of it can be reused whenever we are
 * ready to implement inlining once again
 *
    public static IRubyObject interpret_with_inline(ThreadContext context, CFG cfg, InterpreterContext interp) {
        try {
            BasicBlock basicBlock = cfg.getEntryBB();
            Instr skipTillInstr = null;
            while (basicBlock != null) {
                Label jumpTarget = null;
                Instr prev = null;
                for (Instr instruction : basicBlock.getInstrs()) {
                    // Skip till we come back to previous execution point
                    if (skipTillInstr != null && instruction != skipTillInstr)
                        continue;

                    skipTillInstr = null;

                    if (debug) System.out.println("EXEC'ing: " + instruction);

                    interpInstrsCount++;
                    try {
                        jumpTarget = instruction.interpret(interp, (IRubyObject) interp.getSelf());
                    }
                    catch (InlineMethodHint ih) {
                        if ("array_each".equals(ih.inlineableMethod.getName())) {
                            System.out.println("Got inline method hint for: " + ih.inlineableMethod.getFullyQualifiedName() + ". inlining!");
                            cfg.inlineMethod(ih.inlineableMethod, basicBlock, (CallInstr)instruction);
                            interp.updateRenamedVariablesCount(cfg.getScope().getRenamedVariableSize());
                            skipTillInstr = prev;

                            if (debug) {
                                System.out.println("--------------------");
                                System.out.println("\nGraph:\n" + cfg.getGraph().toString());
                                System.out.println("\nInstructions:\n" + cfg.toStringInstrs());
                                System.out.println("--------------------");
                            }
                            break;
                        } else {
                            jumpTarget = instruction.interpret(interp, (IRubyObject) interp.getSelf());
                        }
                    }
                    prev = instruction;
                }

                // Explicit jump or implicit fall-through to next bb for the situation when we haven't inlined
                if (skipTillInstr == null)
                    basicBlock = (jumpTarget == null) ? cfg.getFallThroughBB(basicBlock) : cfg.getTargetBB(jumpTarget);
            }

            return (IRubyObject) interp.getReturnValue();
        } finally {
            if (interp.getFrame() != null) {
                context.popFrame();
                interp.setFrame(null);
            }
            if (interp.hasAllocatedDynamicScope()) 
                context.postMethodScopeOnly();
        }
    }
**/
}
