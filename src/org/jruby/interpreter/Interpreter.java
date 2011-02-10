package org.jruby.interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ast.Node;
import org.jruby.ast.RootNode;
import org.jruby.compiler.ir.IRBuilder;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRScript;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.BEQInstr;
import org.jruby.compiler.ir.instructions.CopyInstr;
import org.jruby.compiler.ir.instructions.JumpInstr;
import org.jruby.compiler.ir.instructions.ReturnInstr;
import org.jruby.compiler.ir.instructions.BREAK_Instr;
import org.jruby.compiler.ir.instructions.LineNumberInstr;
import org.jruby.compiler.ir.compiler_pass.AddBindingInstructions;
import org.jruby.compiler.ir.compiler_pass.CFG_Builder;
import org.jruby.compiler.ir.compiler_pass.IR_Printer;
import org.jruby.compiler.ir.compiler_pass.LiveVariableAnalysis;
import org.jruby.compiler.ir.compiler_pass.opts.DeadCodeElimination;
import org.jruby.compiler.ir.compiler_pass.opts.LocalOptimizationPass;
import org.jruby.compiler.ir.compiler_pass.CallSplitter;
import org.jruby.compiler.ir.instructions.CallInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;


public class Interpreter {
    public static IRubyObject interpret(Ruby runtime, Node rootNode, IRubyObject self) {
        IRScope scope = new IRBuilder().buildRoot((RootNode) rootNode);

        scope.runCompilerPass(new LocalOptimizationPass());
        scope.runCompilerPass(new CFG_Builder());
        scope.runCompilerPass(new LiveVariableAnalysis());
        scope.runCompilerPass(new DeadCodeElimination());
        scope.runCompilerPass(new AddBindingInstructions());
//        scope.runCompilerPass(new CallSplitter());

        return Interpreter.interpretTop(runtime, scope, self);
    }

    public static void main(String[] args) {
        Ruby runtime = Ruby.getGlobalRuntime();
        boolean isDebug = args.length > 0 && args[0].equals("-debug");
        int i = isDebug ? 1 : 0;
        boolean isCommandLineScript = args.length > i && args[i].equals("-e");
        i += (isCommandLineScript ? 1 : 0);
        while (i < args.length) {
            long t1 = new Date().getTime();
            Node ast = buildAST(runtime, isCommandLineScript, args[i]);
            long t2 = new Date().getTime();
            IRScope scope = new IRBuilder().buildRoot((RootNode) ast);
            long t3 = new Date().getTime();
            if (isDebug) {
                System.out.println("## Before local optimization pass");
                scope.runCompilerPass(new IR_Printer());
            }
            scope.runCompilerPass(new LocalOptimizationPass());
            long t4 = new Date().getTime();
            if (isDebug) {
                System.out.println("## After local optimization");
                scope.runCompilerPass(new IR_Printer());
            }
            scope.runCompilerPass(new CFG_Builder());
            long t5 = new Date().getTime();
            if (isDebug) System.out.println("## After dead code elimination");
            scope.runCompilerPass(new LiveVariableAnalysis());
            long t7 = new Date().getTime();
            scope.runCompilerPass(new DeadCodeElimination());
            long t8 = new Date().getTime();
            scope.runCompilerPass(new AddBindingInstructions());
            long t9 = new Date().getTime();
            if (isDebug) scope.runCompilerPass(new IR_Printer());
            Interpreter.interpretTop(runtime, scope, runtime.getTopSelf());
            long t10 = new Date().getTime();

            System.out.println("Time to build AST         : " + (t2 - t1));
            System.out.println("Time to build IR          : " + (t3 - t2));
            System.out.println("Time to run local opts    : " + (t4 - t3));
            System.out.println("Time to run build cfg     : " + (t5 - t4));
            System.out.println("Time to run lva           : " + (t7 - t5));
            System.out.println("Time to run dead code elim: " + (t8 - t7));
            System.out.println("Time to add frame instrs  : " + (t9 - t8));
            System.out.println("Time to interpret         : " + (t10 - t9));
            i++;
        }
    }
        
    public static Node buildAST(Ruby runtime, boolean isCommandLineScript, String arg) {
        // inline script
        if (isCommandLineScript) return runtime.parse(ByteList.create(arg), "-e", null, 0, false);

        // from file
        try {
            System.out.println("-- processing " + arg + " --");
            return runtime.parseFile(new FileInputStream(new File(arg)), arg, null, 0);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private static int interpInstrsCount = 0;

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
        System.out.println("-- Interpreted " + interpInstrsCount + " instructions");
        return rv;
    }

    public static IRubyObject interpret(ThreadContext context, CFG cfg, InterpreterContext interp) {
        boolean inClosure = (cfg.getScope() instanceof IRClosure);
        try {
            interp.setMethodExitLabel(cfg.getExitBB().getLabel()); // used by return instructions!

            IRubyObject self = (IRubyObject) interp.getSelf();
            Instr[] instrs = cfg.prepareForInterpretation();
            int n   = instrs.length;
            int ipc = 0;
            Instr lastInstr = null;
            while (ipc < n) {
                interpInstrsCount++;
                lastInstr = instrs[ipc];
//                System.out.println("EXEC'ing: " + lastInstr);
                Label jumpTarget = lastInstr.interpret(interp, self);
                ipc = (jumpTarget == null) ? ipc + 1 : jumpTarget.getTargetPC();
            }

            // If I am in a closure, and lastInstr was a return, have to return from the nearest method!
            IRubyObject rv = (IRubyObject) interp.getReturnValue();
            if (inClosure && (lastInstr instanceof ReturnInstr))
                throw context.returnJump(rv);
            else if (!inClosure && (lastInstr instanceof BREAK_Instr))
                throw context.getRuntime().newLocalJumpError(org.jruby.RubyLocalJumpError.Reason.BREAK, rv, "unexpected break");

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

//                    System.out.println("EXEC'ing: " + instruction);
                    interpInstrsCount++;
                    try {
                        jumpTarget = instruction.interpret(interp, (IRubyObject) interp.getSelf());
                    }
                    catch (InlineMethodHint ih) {
                        if (ih.inlineableMethod.getName() == "array_each") {
                            System.out.println("Got inline method hint for: " + ih.inlineableMethod.getFullyQualifiedName() + ". inlining!");
                            cfg.inlineMethod(ih.inlineableMethod, basicBlock, (CallInstr)instruction);
                            interp.updateRenamedVariablesCount(cfg.getScope().getRenamedVariableSize());
                            skipTillInstr = prev;
/*
                            System.out.println("--------------------");
                            System.out.println("\nGraph:\n" + cfg.getGraph().toString());
                            System.out.println("\nInstructions:\n" + cfg.toStringInstrs());
                            System.out.println("--------------------");
*/
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
}
