package org.jruby.ir.interpreter;

import java.io.ByteArrayOutputStream;

import org.jruby.EvalType;
import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.builder.IRBuilder;
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
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.ManyVarsDynamicScope;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import static org.jruby.api.Access.objectClass;
import static org.jruby.runtime.Helpers.arrayOf;

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

        if (IRRuntimeHelpers.shouldPrintIR(runtime) && IRRuntimeHelpers.shouldPrintScope(irScope)) {
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
            currModule = objectClass(context);
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

    public static IRubyObject INTERPRET_CLASS(ThreadContext context, IRScope body, RubyModule clazz, String name) {
        return interpretFrameScope(context, null, body, clazz, null, Visibility.PUBLIC, clazz, null, name, null, Block.NULL_BLOCK);
    }

    public static IRubyObject INTERPRET_MODULE(ThreadContext context, IRScope body, RubyModule clazz, String name) {
        return interpretFrameScope(context, null, body, clazz, null, Visibility.PUBLIC, clazz, null, name,  null, Block.NULL_BLOCK);
    }

    public static IRubyObject INTERPRET_METACLASS(ThreadContext context, IRScope body, RubyModule clazz, String name, Visibility visibility) {
        return interpretFrameScope(context, null, body, clazz, context.getCurrentScope(), visibility, clazz, null, name,  null, Block.NULL_BLOCK);
    }

    public static IRubyObject INTERPRET_METHOD(ThreadContext context, IRScope body, RubyModule implClass,
                                               IRubyObject self, String name, IRubyObject[] args, Block block) {
        return interpretFrameScope(context, null, body, implClass, null, Visibility.PUBLIC, self, name, name, args, block);
    }

    public static IRubyObject INTERPRET_METHOD(ThreadContext context, IRScope body, RubyModule implClass,
                                               IRubyObject self, String name, Block block) {
        return interpretFrameScope(context, null, body, implClass, null, Visibility.PUBLIC, self, name, name, IRubyObject.NULL_ARRAY, block);
    }

    public static IRubyObject INTERPRET_METHOD(ThreadContext context, IRScope body, RubyModule implClass,
                                               IRubyObject self, String name, IRubyObject arg0, Block block) {
        return interpretFrameScope(context, null, body, implClass, null, Visibility.PUBLIC, self, name, name, arrayOf(arg0), block);
    }

    public static IRubyObject INTERPRET_METHOD(ThreadContext context, IRScope body, RubyModule implClass,
                                               IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        return interpretFrameScope(context, null, body, implClass, null, Visibility.PUBLIC, self, name, name, arrayOf(arg0, arg1), block);
    }

    public static IRubyObject INTERPRET_METHOD(ThreadContext context, IRScope body, RubyModule implClass,
                                               IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return interpretFrameScope(context, null, body, implClass, null, Visibility.PUBLIC, self, name, name, arrayOf(arg0, arg1, arg2), block);
    }

    private static IRubyObject interpretFrameScope(ThreadContext context, Block selfBlock, IRScope body, RubyModule clazz, DynamicScope parentScope, Visibility visibility, IRubyObject self, String frameName, String passedName, IRubyObject[] args, Block block) {
        InterpreterContext ic = body.getInterpreterContext();
        String id = body.getId();
        boolean hasExplicitCallProtocol =  ic.hasExplicitCallProtocol();

        try {
            ThreadContext.pushBacktrace(context, id, ic.getFileName(), ic.getLine());

            if (!hasExplicitCallProtocol) preFrameScope(ic, context, self, frameName, block, clazz, parentScope, visibility);

            try {
                return ic.getEngine().interpret(context, selfBlock, self, ic, clazz, passedName, args, block);
            } finally {
                body.cleanupAfterExecution();
                if (!hasExplicitCallProtocol) postFrameScope(ic, context);
            }
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    public static void postFrameScope(InterpreterContext ic, ThreadContext context) {
        context.popFrame();
        if (ic.popDynScope()) context.popScope();
    }

    public static void preFrameScope(InterpreterContext ic, ThreadContext context, IRubyObject self, String name, Block block, RubyModule implClass, DynamicScope parentScope, Visibility visibility) {
        context.preMethodFrameOnly(implClass, name, self, visibility, block);
        if (ic.pushNewDynScope()) {
            if (parentScope == null) {
                context.pushScope(DynamicScope.newDynamicScope(ic.getStaticScope()));
            } else {
                // Add a parent-link to current dynscope to support non-local returns cheaply
                // This doesn't affect variable scoping since local variables will all have
                // the right scope depth. Only used by metaclass bodies.
                context.pushScope(DynamicScope.newDynamicScope(ic.getStaticScope(), context.getCurrentScope()));
            }
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
            return evalCommon(context, evalScope, self, src, file, lineNumber, evalName(file, lineNumber), Block.NULL_BLOCK, evalType, false);
        } finally {
            context.popFrame();
        }
    }

    public static String evalName(String file, int line) {
        return "(eval at " + file + ":" + line + ")";
    }

    private static IRubyObject evalCommon(ThreadContext context, DynamicScope evalScope, IRubyObject self, IRubyObject src,
                                          String file, int lineNumber, String name, Block blockArg, EvalType evalType, boolean bindingGiven) {
        InterpreterContext ic = prepareIC(context, evalScope, src, file, lineNumber, evalType, bindingGiven);

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
    public static IRubyObject evalWithBinding(ThreadContext context, IRubyObject self, IRubyObject src, Binding binding, boolean bindingGiven) {
        Ruby runtime = context.runtime;

        DynamicScope evalScope = binding.getEvalScope(runtime);
        evalScope.getStaticScope().setFile(binding.getFile());
        evalScope.getStaticScope().determineModule(); // FIXME: It would be nice to just set this or remove it from staticScope altogether

        Frame lastFrame = context.preEvalWithBinding(binding);
        try {
            return evalCommon(context, evalScope, self, src, binding.getFile(),
                    binding.getLine(), binding.getMethod(), binding.getFrame().getBlock(), EvalType.BINDING_EVAL, bindingGiven);
        } finally {
            context.postEvalWithBinding(binding, lastFrame);
        }
    }

    private static InterpreterContext prepareIC(ThreadContext context, DynamicScope evalScope, IRubyObject src,
                                                        String file, int lineNumber, EvalType evalType, boolean bindingGiven) {
        Ruby runtime = context.runtime;
        IRScope containingIRScope = evalScope.getStaticScope().getEnclosingScope().getIRScope();
        ParseResult result = runtime.getParserManager().parseEval(file, lineNumber, src.convertToString().getByteList(), evalScope);
        StaticScope staticScope = evalScope.getStaticScope();

        // Top-level script!
        IREvalScript script = new IREvalScript(runtime.getIRManager(), containingIRScope, file, lineNumber, staticScope, evalType);

        // enable refinements if incoming scope already has an overlay active
        if (staticScope.getOverlayModuleForRead() != null || containingIRScope.maybeUsingRefinements()) {
            script.setIsMaybeUsingRefinements();
        }

        // We link IRScope to StaticScope because we may add additional variables (like %block).  During execution,
        // we end up growing dynamicscope potentially based on any changes made.
        staticScope.setIRScope(script);

        IRManager manager = runtime.getIRManager();
        IRBuilder builder = manager.getBuilderFactory().newIRBuilder(manager, script, null, result.getEncoding());
        builder.evalType = !bindingGiven && evalType == EvalType.BINDING_EVAL ? EvalType.INSTANCE_EVAL : evalType;
        InterpreterContext ic = builder.buildEvalRoot(result);

        if (IRRuntimeHelpers.isDebug()) LOG.info(script.debugOutput());

        return ic;
    }
}
