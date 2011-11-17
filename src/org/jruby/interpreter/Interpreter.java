package org.jruby.interpreter;

import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
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
import org.jruby.compiler.ir.instructions.ResultInstr;
import org.jruby.compiler.ir.operands.IRException;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.WrappedIRClosure;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.ThreadKill;
import org.jruby.parser.IRStaticScope;
import org.jruby.parser.StaticScope;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.Block.Type;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class Interpreter {
    private static final Logger LOG = LoggerFactory.getLogger("Interpreter");

    private static int interpInstrsCount = 0;

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

        IREvalScript evalScript = new IRBuilder().buildEvalRoot(ss, (IRExecutionScope) containingIRScope, file, lineNumber, rootNode);
        evalScript.prepareForInterpretation();
//        evalScript.runCompilerPass(new CallSplitter());
        ThreadContext context = runtime.getCurrentContext(); 
        runBeginEndBlocks(evalScript.getBeginBlocks(), context, self, null); // FIXME: No temp vars yet right?
        IRubyObject rv = evalScript.call(context, self, evalScript.getStaticScope().getModule(), rootNode.getScope(), block);
        runBeginEndBlocks(evalScript.getEndBlocks(), context, self, null); // FIXME: No temp vars right?
        return rv;
    }

    public static IRubyObject interpretSimpleEval(Ruby runtime, String file, int lineNumber, Node node, IRubyObject self) {
        return interpretCommonEval(runtime, file, lineNumber, (RootNode)node, self, Block.NULL_BLOCK);
    }

    public static IRubyObject interpretBindingEval(Ruby runtime, String file, int lineNumber, Node node, IRubyObject self, Block block) {
        return interpretCommonEval(runtime, file, lineNumber, (RootNode)node, self, block);
    }

    public static void runBeginEndBlocks(List<IRClosure> beBlocks, ThreadContext context, IRubyObject self, Object[] temp) {
        if (beBlocks == null) return;

        for (IRClosure b: beBlocks) {
            // SSS FIXME: Should I piggyback on WrappedIRClosure.retrieve or just copy that code here?
            b.prepareForInterpretation();
            Block blk = (Block)(new WrappedIRClosure(b)).retrieve(context, self, temp);
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
            runBeginEndBlocks(root.getBeginBlocks(), context, self, null); // FIXME: No temp vars yet...not needed?
            IRMethod rootMethod = root.getRootClass().getRootMethod();
            InterpretedIRMethod method = new InterpretedIRMethod(rootMethod, currModule, true);
            IRubyObject rv =  method.call(context, self, currModule, "", IRubyObject.NULL_ARRAY);
            runBeginEndBlocks(root.getEndBlocks(), context, self, null); // FIXME: No temp vars yet...not needed?
            if (isDebug()) LOG.info("-- Interpreted instructions: {}", interpInstrsCount);
            return rv;
        } catch (IRBreakJump bj) {
            throw IRException.BREAK_LocalJumpError.getException(context.getRuntime());
        }
    }

    public static IRubyObject interpret(ThreadContext context, IRubyObject self, 
            IRExecutionScope scope, IRubyObject[] args, Block block, Block.Type blockType) {
        boolean inClosure = (scope instanceof IRClosure);
        Instr[] instrs = scope.prepareInstructionsForInterpretation();
        int temporaryVariablesSize = scope.getTemporaryVariableSize();
        Object[] temporaryVariables = temporaryVariablesSize > 0 ? new Object[temporaryVariablesSize] : null;
        int n   = instrs.length;
        int ipc = 0;
        Instr lastInstr = null;
        IRubyObject rv = null;
        Object exception = null;
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
                    Object value = lastInstr.interpret(context, self, args, block, exception, temporaryVariables);
                    if (value == null) {
                        ipc++;
                    } else if (value instanceof Label) { // jump to new location
                        ipc = ((Label) value).getTargetPC();                        
                    } else {
                        rv = (IRubyObject) value;
                        ipc = scope.cfg().getExitBB().getLabel().getTargetPC();
                    }
                } catch (IRReturnJump rj) {
                    return handleReturnJumpInClosure(scope, rj, blockType);
                } catch (IRBreakJump bj) {
                    if ((lastInstr instanceof BreakInstr) || bj.breakInEval) {
                        handleBreakJumpInEval(context, scope, bj, blockType, inClosure);
                    } else if (inLambda(blockType)) {
                        // We just unwound all the way up because of a non-local break
                        throw IRException.BREAK_LocalJumpError.getException(context.getRuntime());                        
                    } else if (bj.caughtByLambda || (bj.scopeToReturnTo == scope)) {
                        // We got where we need to get to (because a lambda stopped us, or because we popped to the
                        // lexical scope where we got called from).  Retrieve the result and store it.
                        if (lastInstr instanceof ResultInstr) {
                            ((ResultInstr) lastInstr).getResult().store(context, self, temporaryVariables, bj.breakValue);
                        }
                        ipc += 1;
                    } else {
                        // We need to continue to break upwards.
                        // Run any ensures we need to run before breaking up. 
                        // Quite easy to do this by passing 'bj' as the exception to the ensure block!
                        ipc = scope.getEnsurerPC(lastInstr);
                        if (ipc == -1) throw bj; // No ensure block here, just rethrow bj
                        exception = bj; // Found an ensure block, set 'bj' as the exception and transfer control
                    }
                }
            } catch (RaiseException re) {
                if (isDebug()) LOG.info("in scope: " + scope + ", caught raise exception: " + re.getException() + "; excepting instr: " + lastInstr);
                ipc = scope.getRescuerPC(lastInstr);
                if (isDebug()) LOG.info("ipc for rescuer: " + ipc);
                if (ipc == -1) throw re; // No one rescued exception, pass it on!

                exception = re.getException();
            } catch (ThreadKill e) {
                ipc = scope.getEnsurerPC(lastInstr);
                if (ipc == -1) throw e; // No ensure block here, pass it on! 
                exception = e;
            } catch (Error e) {
                ipc = scope.getEnsurerPC(lastInstr);
                if (ipc == -1) throw e; // No ensure block here, pass it on! 
                exception = e;
            }
        }

        // If not in a lambda, and lastInstr was a return, check if this was a non-local return
        if ((lastInstr instanceof ReturnInstr) && !inLambda(blockType)) {
            handleNonLocalReturn(context, scope, (ReturnInstr) lastInstr, rv, inClosure);
        }

        return rv;
    }

    /*
     * Handle non-local returns (ex: when nested in closures, root scopes of module/class/sclass bodies)
     */
    private static void handleNonLocalReturn(ThreadContext context, IRExecutionScope scope, ReturnInstr returnInstr, IRubyObject returnValue, boolean inClosure) {
        IRMethod methodToReturnFrom = returnInstr.methodToReturnFrom;

        if (inClosure) {
            // Cannot return from root methods -- so find out where exactly we need to return.
            if (methodToReturnFrom.isAModuleRootMethod()) {
                methodToReturnFrom = methodToReturnFrom.getClosestNonRootMethodAncestor();
                if (methodToReturnFrom == null) throw IRException.RETURN_LocalJumpError.getException(context.getRuntime());
            }

            // Cannot return to the call that we have long since exited.
            if (!context.scopeExistsOnCallStack(methodToReturnFrom.getStaticScope())) {
                if (isDebug()) LOG.info("in scope: " + scope + ", raising unexpected return local jump error");
                throw IRException.RETURN_LocalJumpError.getException(context.getRuntime());
            }

            throw new IRReturnJump(methodToReturnFrom, returnValue);
        } else if ((methodToReturnFrom != null)) {
            // methodtoReturnFrom will not be null for explicit returns from class/module/sclass bodies
            throw new IRReturnJump(methodToReturnFrom, returnValue);
        }        
    }

    private static IRubyObject handleReturnJumpInClosure(IRExecutionScope scope, IRReturnJump rj, Type blockType) throws IRReturnJump {
        // - If we are in a lambda or if we are in the method scope we are supposed to return from, stop propagating
        if (inLambda(blockType) || (rj.methodToReturnFrom == scope)) return (IRubyObject) rj.returnValue;

        // - If not, Just pass it along!
        throw rj;
    }

    private static void handleBreakJumpInEval(ThreadContext context, IRExecutionScope scope, IRBreakJump bj, Type blockType, boolean inClosure) throws RaiseException, IRBreakJump {
        bj.breakInEval = false;  // Clear eval flag

        // Error
        if (!inClosure || inProc(blockType)) throw IRException.BREAK_LocalJumpError.getException(context.getRuntime());

        // Lambda special case.  We are in a lambda and breaking out of it requires popping out exactly one level up.
        if (inLambda(blockType)) bj.caughtByLambda = true;
        // If we are in an eval, record it so we can account for it
        else if (scope instanceof IREvalScript) bj.breakInEval = true;

        // Pass it upward
        throw bj;
    }

    public static IRubyObject INTERPRET_METHOD(ThreadContext context, IRExecutionScope scope, 
        IRubyObject self, String name, RubyModule implClass, IRubyObject[] args, Block block, Block.Type blockType, boolean isTraceable) {
        Ruby runtime = context.getRuntime();
        boolean syntheticMethod = name == null || name.equals("");
        
        try {
            String className = implClass.getName();
            if (!syntheticMethod) ThreadContext.pushBacktrace(context, name, context.getFile(), context.getLine());
            if (isTraceable) methodPreTrace(runtime, context, name, implClass);
            return interpret(context, self, scope, args, block, blockType);
        } finally {
            if (isTraceable) {
                try {methodPostTrace(runtime, context, name, implClass);}
                finally { if (!syntheticMethod) ThreadContext.popBacktrace(context);}
            } else {
                if (!syntheticMethod) ThreadContext.popBacktrace(context);
            }
        }
    }
    
    private static boolean inLambda(Block.Type blockType) {
        return blockType == Block.Type.LAMBDA;
    }

    public static boolean inProc(Block.Type blockType) {
        return blockType == Block.Type.PROC;
    }    

    private static void methodPreTrace(Ruby runtime, ThreadContext context, String name, RubyModule implClass) {
        if (runtime.hasEventHooks()) context.trace(RubyEvent.CALL, name, implClass);
    }

    private static void methodPostTrace(Ruby runtime, ThreadContext context, String name, RubyModule implClass) {
        if (runtime.hasEventHooks()) context.trace(RubyEvent.RETURN, name, implClass);
    }
}
