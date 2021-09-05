package org.jruby.ir.interpreter;

import java.io.ByteArrayOutputStream;
import org.jruby.EvalType;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.ast.RootNode;
import org.jruby.ir.IRBuilder;
import org.jruby.ir.IREvalScript;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.IRTranslator;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.persistence.IRDumper;
import org.jruby.ir.runtime.IRBreakJump;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.ManyVarsDynamicScope;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class Interpreter extends IRTranslator<IRubyObject, IRubyObject> {
    public static final Logger LOG = LoggerFactory.getLogger(Interpreter.class);
    public static final String ROOT = "<main>";

    static int interpInstrsCount = 0;

    public static void dumpStats() {
        if ((IRRuntimeHelpers.isDebug() || IRRuntimeHelpers.inProfileMode()) && interpInstrsCount > 10000) {
            LOG.info("-- Interpreted instructions: {}", interpInstrsCount);
            /*
            for (Operation o: opStats.keySet()) {
                System.out.println(o + " = " + opStats.get(o).count);
            }
            */
        }
    }

    @Override
    protected IRubyObject execute(Ruby runtime, IRScriptBody irScope, IRubyObject self) {
        InterpreterContext ic = irScope.getInterpreterContext();

        if (IRRuntimeHelpers.shouldPrintIR(runtime)) {
            ByteArrayOutputStream baos = IRDumper.printIR(irScope, false);

            LOG.info("Printing simple IR for " + irScope.getId() + ":\n" + new String(baos.toByteArray()));
        }

        ThreadContext context = runtime.getCurrentContext();
        String name = ROOT;

        if (IRRuntimeHelpers.isDebug()) LOG.info("Executing {}", ic);

        // We get the live object ball rolling here.
        // This give a valid value for the top of this lexical tree.
        // All new scopes can then retrieve and set based on lexical parent.
        StaticScope scope = ic.getStaticScope();
        RubyModule currModule = scope.getModule();
        if (currModule == null) {
            // SSS FIXME: Looks like this has to do with Kernel#load
            // and the wrap parameter. Figure it out and document it here.
            currModule = context.getRuntime().getObject();
        }

        scope.setModule(currModule);

        IRRuntimeHelpers.prepareScriptScope(context, scope);

        context.preNodeEval(self);
        context.setCurrentVisibility(Visibility.PRIVATE);

        try {
            return INTERPRET_ROOT(context, self, ic, currModule, name);
        } catch (IRBreakJump bj) {
            throw IRException.BREAK_LocalJumpError.getException(context.runtime);
        } finally {
            irScope.cleanupAfterExecution();
            dumpStats();
            context.popScope();
            context.postNodeEval();
        }
    }

    public static IRubyObject INTERPRET_ROOT(ThreadContext context, IRubyObject self,
           InterpreterContext ic, RubyModule clazz, String name) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), ic.getLine());
            return ic.getEngine().interpret(context, null, self, ic, clazz, name, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    public static IRubyObject INTERPRET_EVAL(ThreadContext context, IRubyObject self,
           InterpreterContext ic, RubyModule clazz, IRubyObject[] args, String name, Block blockArg) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), ic.getLine());
            return ic.getEngine().interpret(context, null, self, ic, clazz, name, args, blockArg);
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    public static IRubyObject INTERPRET_BLOCK(ThreadContext context, Block block, IRubyObject self,
            InterpreterContext ic, IRubyObject[] args, String name, Block blockArg) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), ic.getLine());
            return ic.getEngine().interpret(context, block, self, ic, null, name, args, blockArg);
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    /**
     * Evaluate the given string.
     * @param context the current thread's context
     * @param self the self to evaluate under
     * @param src The string containing the text to be evaluated
     * @param file The filename to use when reporting errors during the evaluation
     * @param lineNumber that the eval supposedly starts from
     * @return An IRubyObject result from the evaluation
     */
    public static IRubyObject evalSimple(ThreadContext context, RubyModule under, IRubyObject self, RubyString src, String file, int lineNumber, EvalType evalType) {
        Ruby runtime = context.runtime;

        // no binding, just eval in "current" frame (caller's frame)
        DynamicScope parentScope = context.getCurrentScope();
        DynamicScope evalScope = new ManyVarsDynamicScope(runtime.getStaticScopeFactory().newEvalScope(parentScope.getStaticScope()), parentScope);

        evalScope.getStaticScope().setModule(under);
        context.pushEvalSimpleFrame(self);

        try {
            return evalCommon(context, evalScope, self, src, file, lineNumber, "(eval)", Block.NULL_BLOCK, evalType);
        } finally {
            context.popFrame();
        }
    }

    private static IRubyObject evalCommon(ThreadContext context, DynamicScope evalScope, IRubyObject self, IRubyObject src,
                                          String file, int lineNumber, String name, Block blockArg, EvalType evalType) {
        InterpreterContext ic = prepareIC(context, evalScope, src, file, lineNumber, evalType);

        evalScope.setEvalType(evalType);
        context.pushScope(evalScope);
        try {
            evalScope.growIfNeeded();

            return Interpreter.INTERPRET_EVAL(context, self, ic, ic.getStaticScope().getModule(), IRubyObject.NULL_ARRAY, name, blockArg);
        } finally {
            evalScope.clearEvalType();
            context.popScope();
        }
    }

    /**
     * Evaluate the given string under the specified binding object. If the binding is not a Proc or Binding object
     * (RubyProc or RubyBinding) throw an appropriate type error.
     * @param context the thread context for the current thread
     * @param self the self against which eval was called; used as self in the eval in 1.9 mode
     * @param src The string containing the text to be evaluated
     * @param binding The binding object under which to perform the evaluation
     * @return An IRubyObject result from the evaluation
     */
    public static IRubyObject evalWithBinding(ThreadContext context, IRubyObject self, IRubyObject src, Binding binding) {
        Ruby runtime = context.runtime;

        DynamicScope evalScope = binding.getEvalScope(runtime);
        evalScope.getStaticScope().setFile(binding.getFile());
        evalScope.getStaticScope().determineModule(); // FIXME: It would be nice to just set this or remove it from staticScope altogether

        Frame lastFrame = context.preEvalWithBinding(binding);
        try {
            return evalCommon(context, evalScope, self, src, binding.getFile(),
                    binding.getLine(), binding.getMethod(), binding.getFrame().getBlock(), EvalType.BINDING_EVAL);
        } finally {
            context.postEvalWithBinding(binding, lastFrame);
        }
    }

    private static InterpreterContext prepareIC(ThreadContext context, DynamicScope evalScope, IRubyObject src,
                                                        String file, int lineNumber, EvalType evalType) {
        Ruby runtime = context.runtime;
        IRScope containingIRScope = evalScope.getStaticScope().getEnclosingScope().getIRScope();
        RootNode rootNode = (RootNode) runtime.parseEval(src.convertToString().getByteList(), file, evalScope, lineNumber);
        StaticScope staticScope = evalScope.getStaticScope();

        // Top-level script!
        IREvalScript script = new IREvalScript(runtime.getIRManager(), containingIRScope, file, lineNumber, staticScope, evalType);

        // enable refinements if incoming scope already has an overlay active
        if (staticScope.getOverlayModuleForRead() != null || containingIRScope.maybeUsingRefinements()) {
            script.setIsMaybeUsingRefinements();
        }

        // We link IRScope to StaticScope because we may add additional variables (like %block).  During execution
        // we end up growing dynamicscope potentially based on any changes made.
        staticScope.setIRScope(script);

        InterpreterContext ic = IRBuilder.topIRBuilder(runtime.getIRManager(), script).buildEvalRoot(rootNode);

        if (IRRuntimeHelpers.isDebug()) LOG.info(script.debugOutput());

        return ic;
    }
}
