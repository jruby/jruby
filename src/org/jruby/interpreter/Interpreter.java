package org.jruby.interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import org.jruby.Ruby;
import org.jruby.ast.Node;
import org.jruby.compiler.ir.IR_Builder;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IR_Scope;
import org.jruby.compiler.ir.IR_Script;
import org.jruby.compiler.ir.compiler_pass.AddFrameInstructions;
import org.jruby.compiler.ir.compiler_pass.CFG_Builder;
import org.jruby.compiler.ir.compiler_pass.DominatorTreeBuilder;
import org.jruby.compiler.ir.compiler_pass.IR_Printer;
import org.jruby.compiler.ir.compiler_pass.LiveVariableAnalysis;
import org.jruby.compiler.ir.compiler_pass.opts.DeadCodeElimination;
import org.jruby.compiler.ir.compiler_pass.opts.LocalOptimizationPass;
import org.jruby.compiler.ir.instructions.BRANCH_Instr;
import org.jruby.compiler.ir.instructions.CallInstruction;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.instructions.JUMP_Instr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.util.ByteList;


public class Interpreter {
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
            IR_Scope scope = new IR_Builder().buildRoot(ast);
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
            scope.runCompilerPass(new DominatorTreeBuilder());
            long t6 = new Date().getTime();
            if (isDebug) System.out.println("## After dead code elimination");
            scope.runCompilerPass(new LiveVariableAnalysis());
            long t7 = new Date().getTime();
            scope.runCompilerPass(new DeadCodeElimination());
            long t8 = new Date().getTime();
            scope.runCompilerPass(new AddFrameInstructions());
            long t9 = new Date().getTime();
            if (isDebug) scope.runCompilerPass(new IR_Printer());
            interpretTop(runtime, scope);
            long t10 = new Date().getTime();

            System.out.println("Time to build AST         : " + (t2 - t1));
            System.out.println("Time to build IR          : " + (t3 - t2));
            System.out.println("Time to run local opts    : " + (t4 - t3));
            System.out.println("Time to run build cfg     : " + (t5 - t4));
            System.out.println("Time to run build domtree : " + (t6 - t5));
            System.out.println("Time to run lva           : " + (t7 - t6));
            System.out.println("Time to run dead code elim: " + (t8 - t7));
            System.out.println("Time to add frame instrs  : " + (t9 - t8));
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

    public static void interpretTop(Ruby runtime, IR_Scope scope) {
        if (scope instanceof IR_Script) {
            interpretMethod(runtime, ((IR_Script) scope).getRootClass().getRootMethod());
        } else {
            System.out.println("BONED");
        }
    }

    public static void interpretMethod(Ruby runtime, IRMethod method) {
        System.out.print(method.toString() + "(");

        Operand operands[] = method.getCallArgs();
        for (int i = 0; i < operands.length; i++) {
            System.out.print(operands[i] + ", ");
        }
        System.out.println("EOP)");

        // Dummy start and end are canonical entry and exit points for a method/closures
        // we always getFallThroughBB(previous) to walk through unless we encounter explicit jump

        // IR_Scope 
        //   getLexicalScope <--- previous StaticScope equivalent
        
        // ThreadContext, self, receiver{, arg{, arg{, arg{, arg}}}}

        // Construct primitive array as simple store for temporary variables in method and pass along

        CFG cfg = method.getCFG();
 //       BasicBlock basicBlock = cfg.getEntryBB();
        for (BasicBlock basicBlock : cfg.getNodes()) {
            for (IR_Instr i : basicBlock.getInstrs()) {
                // .. interpret i ..
                if (i instanceof BRANCH_Instr) {
                    System.out.println("In branch");
                    BRANCH_Instr branch = (BRANCH_Instr) i;
                    boolean taken = false; // .. the interpreter will tell you whether the branch was taken or not ...
                    if (taken) {
                        basicBlock = cfg.getTargetBB(branch.getJumpTarget());
                    } else {
                        basicBlock = cfg.getFallThroughBB(basicBlock);
                    }
                } else if (i instanceof JUMP_Instr) {
                    System.out.println("In jump");
                    JUMP_Instr jump = (JUMP_Instr) i;
                    basicBlock = cfg.getTargetBB(jump.getJumpTarget());
                } else if (i instanceof CallInstruction) {
                    CallInstruction callInstruction = (CallInstruction) i;

                    System.out.println("Call: " + callInstruction);

                    // Does not need to be recursive...except for scope handling
                    interpretMethod(runtime, callInstruction.getTargetMethod());
                } else {
                    System.out.println("NOT HANDLING: " + i);
                }
                //... handle returns ..
            }

            if (basicBlock == null) {
                //.. you are done with this cfg /method ..
                //.. pop call stack, etc ..
            }
        }
    }

    public static void interpret() {

    }
}
