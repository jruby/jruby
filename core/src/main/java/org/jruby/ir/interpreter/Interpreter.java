package org.jruby.ir.interpreter;

import java.util.List;
import org.jruby.EvalType;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.ast.RootNode;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.ir.IRBindingEvalScript;
import org.jruby.ir.IRBuilder;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IREvalScript;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.IRTranslator;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.representations.CFG;
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
    public static final Logger LOG = LoggerFactory.getLogger("Interpreter");
    private static final IRubyObject[] EMPTY_ARGS = new IRubyObject[]{};
    static int interpInstrsCount = 0;

    // we do not need instances of Interpreter
    // FIXME: Should we make it real singleton and get rid of static methods?
    private Interpreter() { }

    private static class InterpreterHolder {
        // FIXME: Remove static reference unless lifus does later
        public static final Interpreter instance = new Interpreter();
    }

    public static Interpreter getInstance() {
        return InterpreterHolder.instance;
    }

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

    public static void runBeginBlocks(List<IRClosure> beBlocks, ThreadContext context, IRubyObject self, StaticScope currScope, Object[] temp) {
        if (beBlocks == null) return;

        for (IRClosure b: beBlocks) {
            // SSS FIXME: Should I piggyback on WrappedIRClosure.retrieve or just copy that code here?
            b.prepareForInterpretation();
            Block blk = (Block)(new WrappedIRClosure(b.getSelf(), b)).retrieve(context, self, currScope, context.getCurrentScope(), temp);
            blk.yield(context, null);
        }
    }

    @Override
    protected IRubyObject execute(Ruby runtime, IRScriptBody irScope, IRubyObject self) {
        BeginEndInterpreterContext ic = (BeginEndInterpreterContext) irScope.prepareForInterpretation();
        ThreadContext context = runtime.getCurrentContext();
        String name = "(root)";

        if (IRRuntimeHelpers.isDebug()) LOG.info("Executing " + ic);

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
        DynamicScope tlbScope = irScope.getToplevelScope();
        if (tlbScope == null) {
            context.preMethodScopeOnly(scope);
        } else {
            context.preScopedBody(tlbScope);
            tlbScope.growIfNeeded();
        }
        context.setCurrentVisibility(Visibility.PRIVATE);

        try {
            runBeginBlocks(ic.getBeginBlocks(), context, self, scope, null);
            return INTERPRET_ROOT(context, self, ic, currModule, name);
        } catch (IRBreakJump bj) {
            throw IRException.BREAK_LocalJumpError.getException(context.runtime);
        } finally {
            dumpStats();
            context.popScope();
        }
    }

    public static IRubyObject INTERPRET_ROOT(ThreadContext context, IRubyObject self,
           InterpreterContext ic, RubyModule clazz, String name) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());
            return ic.engine.interpret(context, self, ic, clazz, name, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK, null);
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    public static IRubyObject INTERPRET_EVAL(ThreadContext context, IRubyObject self,
           InterpreterContext ic, RubyModule clazz, IRubyObject[] args, String name, Block block, Block.Type blockType) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());
            return ic.engine.interpret(context, self, ic, clazz, name, args, block, blockType);
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    public static IRubyObject INTERPRET_BLOCK(ThreadContext context, IRubyObject self,
            InterpreterContext ic, IRubyObject[] args, String name, Block block, Block.Type blockType) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());
            return ic.engine.interpret(context, self, ic, null, name, args, block, blockType);
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
        if (runtime.getInstanceConfig().getCompileMode() == RubyInstanceConfig.CompileMode.TRUFFLE) throw new UnsupportedOperationException();

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
                                          String file, int lineNumber, String name, Block block, EvalType evalType) {
        StaticScope ss = evalScope.getStaticScope();
        BeginEndInterpreterContext ic = prepareIC(context, evalScope, src, file, lineNumber, evalType);

        evalScope.setEvalType(evalType);
        context.pushScope(evalScope);
        try {
            evalScope.growIfNeeded();

            runBeginBlocks(ic.getBeginBlocks(), context, self, ss, null);

            return Interpreter.INTERPRET_EVAL(context, self, ic, ic.getStaticScope().getModule(), EMPTY_ARGS, name, block, null);
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
        if (runtime.getInstanceConfig().getCompileMode() == RubyInstanceConfig.CompileMode.TRUFFLE) throw new UnsupportedOperationException();

        DynamicScope evalScope = binding.getEvalScope(runtime);
        evalScope.getStaticScope().determineModule(); // FIXME: It would be nice to just set this or remove it from staticScope altogether

        Frame lastFrame = context.preEvalWithBinding(binding);
        try {
            return evalCommon(context, evalScope, self, src, binding.getFile(),
                    binding.getLine(), binding.getMethod(), binding.getFrame().getBlock(), EvalType.BINDING_EVAL);
        } finally {
            context.postEvalWithBinding(binding, lastFrame);
        }
    }

    private static BeginEndInterpreterContext prepareIC(ThreadContext context, DynamicScope evalScope, IRubyObject src,
                                                        String file, int lineNumber, EvalType evalType) {
        Ruby runtime = context.runtime;
        IRScope containingIRScope = evalScope.getStaticScope().getEnclosingScope().getIRScope();
        RootNode rootNode = (RootNode) runtime.parseEval(src.convertToString().getByteList(), file, evalScope, lineNumber);
        StaticScope staticScope = evalScope.getStaticScope();
        // Top-level script!
        IREvalScript script;

        if (evalType == EvalType.BINDING_EVAL) {
            script = new IRBindingEvalScript(runtime.getIRManager(), containingIRScope, file, lineNumber, staticScope, evalType);
        } else {
            script = new IREvalScript(runtime.getIRManager(), containingIRScope, file, lineNumber, staticScope, evalType);
        }

        // We link IRScope to StaticScope because we may add additional variables (like %block).  During execution
        // we end up growing dynamicscope potentially based on any changes made.
        staticScope.setIRScope(script);

        IRBuilder.topIRBuilder(runtime.getIRManager(), script).buildEvalRoot(rootNode);
        BeginEndInterpreterContext ic = (BeginEndInterpreterContext) script.prepareForInterpretation();

        if (IRRuntimeHelpers.isDebug()) {
            LOG.info(script.debugOutput());
        }

        return ic;
    }
}
