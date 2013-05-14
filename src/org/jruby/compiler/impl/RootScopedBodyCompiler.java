package org.jruby.compiler.impl;

import org.jruby.Ruby;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.CompilerCallback;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.util.CodegenUtils.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Behaviors common to all "root-scoped" bodies are encapsulated in this class.
 * "Root-scoped" refers to any method body which does not inherit a containing
 * variable scope. This includes method bodies and class bodies.
 */
public abstract class RootScopedBodyCompiler extends BaseBodyCompiler {
    private boolean specificArity;

    protected RootScopedBodyCompiler(StandardASMCompiler scriptCompiler, String friendlyName, String rubyName, ASTInspector inspector, StaticScope scope, int scopeIndex) {
        super(scriptCompiler, friendlyName, rubyName, inspector, scope, scopeIndex);
    }

    public String getSignature() {
        if (shouldUseBoxedArgs(scope)) {
            specificArity = false;
            return StandardASMCompiler.getStaticMethodSignature(script.getClassname(), 4);
        } else {
            specificArity = true;
            return StandardASMCompiler.getStaticMethodSignature(script.getClassname(), scope.getRequiredArgs());
        }
    }

    protected void createVariableCompiler() {
        if (inspector == null) {
            variableCompiler = new HeapBasedVariableCompiler(this, method, scope, specificArity, StandardASMCompiler.ARGS_INDEX, getFirstTempIndex());
        } else if (inspector.hasClosure() || inspector.hasScopeAwareMethods()) {
            variableCompiler = new HeapBasedVariableCompiler(this, method, scope, specificArity, StandardASMCompiler.ARGS_INDEX, getFirstTempIndex());
        } else {
            variableCompiler = new StackBasedVariableCompiler(this, method, scope, specificArity, StandardASMCompiler.ARGS_INDEX, getFirstTempIndex());
        }
    }

    public void beginMethod(CompilerCallback args, StaticScope scope) {
        method.start();

        variableCompiler.beginMethod(args, scope);

        // visit a label to start scoping for local vars in this method
        method.label(scopeStart);
    }

    public ChainedRootBodyCompiler outline(String methodName) {
        // chain to the next segment of this giant method
        method.aload(StandardASMCompiler.THIS);

        // load all arguments straight through
        for (int i = 1; i <= getClosureIndex(); i++) {
            method.aload(i);
        }
        // we append an index to ensure two identical method names will not conflict
        methodName = "chained_" + script.getAndIncrementMethodIndex() + "_" + methodName;
        method.invokestatic(script.getClassname(), methodName, getSignature());

        ChainedRootBodyCompiler methodCompiler = new ChainedRootBodyCompiler(script, methodName, rubyName, inspector, scope, this);

        methodCompiler.beginChainedMethod();

        return methodCompiler;
    }

    public void endBody() {
        // return last value from execution
        method.areturn();

        // end of variable scope
        method.label(scopeEnd);

        // method is done, declare all variables
        variableCompiler.declareLocals(scope, scopeStart, scopeEnd);

        method.end();
        if (specificArity) {
            method = new SkinnyMethodAdapter(script.getClassVisitor(), ACC_PUBLIC | ACC_STATIC, methodName, StandardASMCompiler.getStaticMethodSignature(script.getClassname(), 4), null, null);
            method.start();

            // check arity in the variable-arity version
            method.aload(1);
            method.invokevirtual(p(ThreadContext.class), "getRuntime", sig(Ruby.class));
            method.aload(3);
            method.pushInt(scope.getRequiredArgs());
            method.pushInt(scope.getRequiredArgs());
            method.invokestatic(p(Arity.class), "checkArgumentCount", sig(int.class, Ruby.class, IRubyObject[].class, int.class, int.class));
            method.pop();

            loadThis();
            loadThreadContext();
            loadSelf();
            // FIXME: missing arity check
            for (int i = 0; i < scope.getRequiredArgs(); i++) {
                method.aload(StandardASMCompiler.ARGS_INDEX);
                method.ldc(i);
                method.arrayload();
            }
            method.aload(StandardASMCompiler.ARGS_INDEX + 1);
            // load block from [] version of method
            method.invokestatic(script.getClassname(), methodName, getSignature());
            method.areturn();
            method.end();
        }
    }

    public void performReturn() {
        if (inRescue) {
            // returning from rescue, clear $!
            clearErrorInfo();
        }
        
        // normal return for method body. return jump for within a begin/rescue/ensure
        if (inNestedMethod) {
            loadThreadContext();
            invokeUtilityMethod("throwReturnJump", sig(IRubyObject.class, IRubyObject.class, ThreadContext.class));
        } else {
            method.areturn();
        }
    }

    public void issueBreakEvent(CompilerCallback value) {
        if (currentLoopLabels != null) {
            value.call(this);
            issueLoopBreak();
        } else if (inNestedMethod) {
            loadThreadContext();
            value.call(this);
            invokeUtilityMethod("breakJump", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class));
        } else {
            // in method body with no containing loop, issue jump error
            // load runtime and value, issue jump error
            loadRuntime();
            value.call(this);
            invokeUtilityMethod("breakLocalJumpError", sig(IRubyObject.class, Ruby.class, IRubyObject.class));
        }
    }

    public void issueNextEvent(CompilerCallback value) {
        if (currentLoopLabels != null) {
            value.call(this);
            issueLoopNext();
        } else if (inNestedMethod) {
            value.call(this);
            invokeUtilityMethod("nextJump", sig(IRubyObject.class, IRubyObject.class));
        } else {
            // in method body with no containing loop, issue jump error
            // load runtime and value, issue jump error
            loadRuntime();
            value.call(this);
            invokeUtilityMethod("nextLocalJumpError", sig(IRubyObject.class, Ruby.class, IRubyObject.class));
        }
    }

    public void issueRedoEvent() {
        if (currentLoopLabels != null) {
            issueLoopRedo();
        } else if (inNestedMethod) {
            invokeUtilityMethod("redoJump", sig(IRubyObject.class));
        } else {
            // in method body with no containing loop, issue jump error
            // load runtime and value, issue jump error
            loadRuntime();
            invokeUtilityMethod("redoLocalJumpError", sig(IRubyObject.class, Ruby.class));
        }
    }
}
