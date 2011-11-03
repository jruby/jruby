package org.jruby.interpreter;

import java.util.List;
import java.util.Stack;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyLocalJumpError.Reason;
import org.jruby.RubyModule;
import org.jruby.ast.Node;
import org.jruby.ast.RootNode;
import org.jruby.compiler.ir.IRBuilder;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IRModule;
import org.jruby.compiler.ir.IREvalScript;
import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRScript;
import org.jruby.compiler.ir.instructions.ReturnInstr;
import org.jruby.compiler.ir.instructions.BreakInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.ThreadKill;
import org.jruby.parser.IRStaticScope;
import org.jruby.parser.StaticScope;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class Interpreter {
    private static final Logger LOG = LoggerFactory.getLogger("Interpreter");

    private static int interpInstrsCount = 0;

    // SSS FIXME: Isn't there a simpler way for doing this?
    // What am I doing wrong with using the block.escape/isEscaped logic
    private static ThreadLocal<Stack<IRExecutionScope>> callStack = new ThreadLocal<Stack<IRExecutionScope>>() {
        @Override
        protected Stack<IRExecutionScope> initialValue() {
            return new Stack<IRExecutionScope>();
        }
    };

    public static boolean isDebug() {
        return RubyInstanceConfig.IR_DEBUG;
    }

    public static IRubyObject interpret(Ruby runtime, Node rootNode, IRubyObject self) {
        IRScope scope = new IRBuilder().buildRoot((RootNode) rootNode);
        scope.prepareForInterpretation();
//        scope.runCompilerPass(new CallSplitter());

        return interpretTop(runtime, scope, self);
    }

    public static IRubyObject interpretCommonEval(Ruby runtime, String file, int lineNumber, RootNode rootNode, IRubyObject self, Block block) {
        // SSS FIXME: Weirdness here.  We cannot get the containing IR scope from ss because of static-scope wrapping that is going on
        // 1. In all cases, DynamicScope.getEvalScope wraps the executing static scope in a new local scope.
        // 2. For instance-eval (module-eval, class-eval) scenarios, there is an extra scope that is added to 
        //    the stack in ThreadContext.java:preExecuteUnder
        // I dont know what rule to apply when.  However, in both these cases, since there is no IR-scope associated,
        // I have used the hack below where I first unwrap once and see if I get a non-null IR scope.  If that doesn't
        // work, I unwarp once more and I am guaranteed to get the IR scope I want.
        StaticScope ss = rootNode.getStaticScope();
        IRScope containingIRScope = ((IRStaticScope)ss.getEnclosingScope()).getIRScope();
        if (containingIRScope == null) containingIRScope = ((IRStaticScope)ss.getEnclosingScope().getEnclosingScope()).getIRScope();

        IREvalScript evalScript = new IRBuilder().buildEvalRoot(ss, containingIRScope, file, lineNumber, rootNode);
        evalScript.prepareForInterpretation();
//        evalScript.runCompilerPass(new CallSplitter());
        ThreadContext context = runtime.getCurrentContext(); 
        runBeginEndBlocks(evalScript.getBeginBlocks(), context, self);
        IRubyObject rv = evalScript.call(context, self, evalScript.getStaticScope().getModule(), rootNode.getScope(), block);
        runBeginEndBlocks(evalScript.getEndBlocks(), context, self);
        return rv;
    }

    public static IRubyObject interpretSimpleEval(Ruby runtime, String file, int lineNumber, Node node, IRubyObject self) {
        return interpretCommonEval(runtime, file, lineNumber, (RootNode)node, self, Block.NULL_BLOCK);
    }

    public static IRubyObject interpretBindingEval(Ruby runtime, String file, int lineNumber, Node node, IRubyObject self, Block block) {
        return interpretCommonEval(runtime, file, lineNumber, (RootNode)node, self, block);
    }

    public static void runBeginEndBlocks(List<IRClosure> beBlocks, ThreadContext context, IRubyObject self) {
        if (beBlocks == null) return;

        for (IRClosure b: beBlocks) {
            // SSS FIXME: Should I piggyback on ClosureMetaObject.retrieve or just copy that code here?
            b.prepareForInterpretation();
            Block blk = (Block)MetaObject.create(b).retrieve(null, context, self);
            blk.yield(context, null);
        }
    }

    // SSS FIXME: We have two different 'prepareForInterpretation' methods
    // one in IRScopeImpl, and another in CFG.  See if they can be merged into one
    public static IRubyObject interpretTop(Ruby runtime, IRScope scope, IRubyObject self) {
        assert scope instanceof IRScript : "Must be an IRScript scope at Top!!!";

        IRScript root = (IRScript) scope;

        // We get the live object ball rolling here.  This give a valid value for the top
        // of this lexical tree.  All new scope can then retrieve and set based on lexical parent.
        if (root.getStaticScope().getModule() == null) { // If an eval this may already be setup.
            root.getStaticScope().setModule(runtime.getObject());
        }

        RubyModule currModule = root.getStaticScope().getModule();

        // Scope state for root?
        IRModule.getRootObjectScope().setModule(currModule);
        ThreadContext context = runtime.getCurrentContext();

        try {
            runBeginEndBlocks(root.getBeginBlocks(), context, self);
            IRMethod rootMethod = root.getRootClass().getRootMethod();
            InterpretedIRMethod method = new InterpretedIRMethod(rootMethod, currModule, true);
            IRubyObject rv =  method.call(context, self, currModule, "", IRubyObject.NULL_ARRAY);
            runBeginEndBlocks(root.getEndBlocks(), context, self);
            if (isDebug()) LOG.info("-- Interpreted instructions: {}", interpInstrsCount);
            return rv;
        } catch (IRBreakJump bj) {
            throw runtime.newLocalJumpError(Reason.BREAK, (IRubyObject)bj.breakValue, "unexpected break");
        }
    }

    public static IRubyObject interpret(ThreadContext context, IRubyObject self, IRExecutionScope scope, InterpreterContext interp) {
        Ruby runtime = context.getRuntime();
        boolean inClosure = (scope instanceof IRClosure);

        Instr[] instrs = scope.prepareInstructionsForInterpretation();
        int n   = instrs.length;
        int ipc = 0;
        Instr lastInstr = null;
        while (ipc < n) {
            interpInstrsCount++;
            lastInstr = instrs[ipc];
            
            if (isDebug()) LOG.info("I: {}", lastInstr);

            // We need a nested try-catch:
            // - The first try-catch around the instruction captures JRuby-implementation exceptions
            //   generated by return and break instructions.  This catch could then raise Ruby-visible
            //   LocalJump errors which could be caught by Ruby-level exception handlers.
            // - The second try-catch around the first try-catch handles Ruby-visible exceptions and
            //   invokes Ruby-level exceptions handlers.
            try {
                try {
                    Label jumpTarget = lastInstr.interpret(interp, context, self);
                    ipc = (jumpTarget == null) ? ipc + 1 : jumpTarget.getTargetPC();
                } catch (IRReturnJump rj) {
                    // - If we are in a lambda or if we are in the method scope we are supposed to return from, stop propagating
                    if (interp.inLambda() || (rj.methodToReturnFrom == scope)) return (IRubyObject) rj.returnValue;

                    // - If not, Just pass it along!
                    throw rj;
                } catch (IRBreakJump bj) {
                    if ((lastInstr instanceof BreakInstr) || bj.breakInEval) {

                        // Clear eval flag
                        bj.breakInEval = false;

                        // Error
                        if (!inClosure || interp.inProc()) throw runtime.newLocalJumpError(Reason.BREAK, (IRubyObject)bj.breakValue, "unexpected break");

                        // Lambda special case.  We are in a lambda and breaking out of it requires popping out exactly one level up.
                        if (interp.inLambda()) bj.caughtByLambda = true;
                        // If we are in an eval, record it so we can account for it
                        else if (scope instanceof IREvalScript) bj.breakInEval = true;

                        // Pass it upward
                        throw bj;
                    } else if (interp.inLambda()) {
                        // We just unwound all the way up because of a non-local break
                        throw runtime.newLocalJumpError(Reason.BREAK, (IRubyObject)bj.breakValue, "unexpected break");
                    } else if (bj.caughtByLambda || (bj.scopeToReturnTo == scope)) {
                        // We got where we need to get to (because a lambda stopped us, or because we popped to the
                        // lexical scope where we got called from).  Retrieve the result and store it.
                        Operand r = lastInstr.getResult();
                        if (r != null) r.store(interp, context, self, bj.breakValue);
                        ipc += 1;
                    } else {
                        // We need to continue to break upwards.
                        // Run any ensures we need to run before breaking up. 
                        // Quite easy to do this by passing 'bj' as the exception to the ensure block!
                        ipc = scope.getEnsurerPC(lastInstr);
                        if (ipc == -1) throw bj; // No ensure block here, just rethrow bj
                        interp.setException(bj); // Found an ensure block, set 'bj' as the exception and transfer control
                    }
                }
            } catch (RaiseException re) {
                if (isDebug()) LOG.info("in scope: " + scope + ", caught raise exception: " + re.getException() + "; excepting instr: " + lastInstr);
                ipc = scope.getRescuerPC(lastInstr);
                if (isDebug()) LOG.info("ipc for rescuer: " + ipc);
                if (ipc == -1) throw re; // No one rescued exception, pass it on!

                interp.setException(re.getException());
            } catch (ThreadKill e) {
                ipc = scope.getEnsurerPC(lastInstr);
                if (ipc == -1) throw e; // No ensure block here, pass it on! 
                interp.setException(e);
            } catch (Error e) {
                ipc = scope.getEnsurerPC(lastInstr);
                if (ipc == -1) throw e; // No ensure block here, pass it on! 
                interp.setException(e);
            }
        }

        IRubyObject rv = (IRubyObject) interp.getReturnValue(context);

        // If not in a lambda, in a closure, and lastInstr was a return, have to return from the nearest method!
        if ((lastInstr instanceof ReturnInstr) && !interp.inLambda()) {
            IRMethod methodToReturnFrom = ((ReturnInstr)lastInstr).methodToReturnFrom;
            if (inClosure && !callStack.get().contains(methodToReturnFrom)) {
                // SSS: better way to do this without having to maintain a call stack?
                // Check with Tom why the block.escape/isEscaped logic isn't working
                // if (interp.getBlock().isEscaped())
                if (isDebug()) LOG.info("in scope: " + scope + ", raising unexpected return local jump error");
                throw runtime.newLocalJumpError(Reason.RETURN, rv, "unexpected return");
            }
            else if (inClosure || (methodToReturnFrom != null)) {
                // methodtoReturnFrom will not be null for explicit returns from class/module/sclass bodies
                throw new IRReturnJump(methodToReturnFrom, rv);
            }
        }

        return rv;
    }

    public static IRubyObject INTERPRET_METHOD(ThreadContext context, IRExecutionScope scope, 
        InterpreterContext interp, IRubyObject self, String name, RubyModule implClass, boolean isTraceable) {
        Ruby runtime = context.getRuntime();
        boolean syntheticMethod = name == null || name.equals("");
        
        try {
            callStack.get().push(scope);
            String className = implClass.getName();
            if (!syntheticMethod) ThreadContext.pushBacktrace(context, className, name, context.getFile(), context.getLine());
            if (isTraceable) methodPreTrace(runtime, context, name, implClass);
            return interpret(context, self, scope, interp);
        } finally {
            callStack.get().pop();
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
