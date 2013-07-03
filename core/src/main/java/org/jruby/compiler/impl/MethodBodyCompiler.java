package org.jruby.compiler.impl;

import org.jruby.Ruby;
import org.jruby.anno.JRubyMethod;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.CompilerCallback;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.AnnotationVisitor;
import static org.jruby.util.CodegenUtils.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * MethodBodyCompiler is the base compiler for all method bodies.
 */
public class MethodBodyCompiler extends RootScopedBodyCompiler {
    protected boolean specificArity;

    public MethodBodyCompiler(StandardASMCompiler scriptCompiler, String rubyName, String javaName, ASTInspector inspector, StaticScope scope, int scopeIndex) {
        super(scriptCompiler, javaName, rubyName, inspector, scope, scopeIndex);
    }

    public boolean isSpecificArity() {
        return specificArity;
    }

    @Override
    public String getSignature() {
        if (shouldUseBoxedArgs(scope)) {
            specificArity = false;
            return StandardASMCompiler.getStaticMethodSignature(script.getClassname(), 4);
        } else {
            specificArity = true;
            return StandardASMCompiler.getStaticMethodSignature(script.getClassname(), scope.getRequiredArgs());
        }
    }

    @Override
    protected void createVariableCompiler() {
        if (inspector == null) {
            variableCompiler = new HeapBasedVariableCompiler(this, method, scope, specificArity, StandardASMCompiler.ARGS_INDEX, getFirstTempIndex());
        } else if (inspector.hasClosure() || inspector.hasScopeAwareMethods()) {
            variableCompiler = new HeapBasedVariableCompiler(this, method, scope, specificArity, StandardASMCompiler.ARGS_INDEX, getFirstTempIndex());
        } else {
            variableCompiler = new StackBasedVariableCompiler(this, method, scope, specificArity, StandardASMCompiler.ARGS_INDEX, getFirstTempIndex());
        }
    }

    @Override
    public void beginMethod(CompilerCallback args, StaticScope scope) {
        method.start();

        variableCompiler.beginMethod(args, scope);

        // visit a label to start scoping for local vars in this method
        method.label(scopeStart);
    }

    @Override
    public void endBody() {
        // return last value from execution
        method.areturn();

        // end of variable scope
        method.label(scopeEnd);

        // method is done, declare all variables
        variableCompiler.declareLocals(scope, scopeStart, scopeEnd);
        
        // Define the annotation for the method
        AnnotationVisitor annotation = method.visitAnnotation(ci(JRubyMethod.class), true);
        annotation.visit("name", rubyName);
        annotation.visit("frame", true);
        annotation.visit("required", scope.getRequiredArgs());
        annotation.visit("optional", scope.getOptionalArgs());
        annotation.visit("rest", scope.getRestArg() >= 0);
        // TODO: reads/writes from frame
        // TODO: information on scoping
        // TODO: visibility?

        method.end();
        if (specificArity) {
            
            method = new SkinnyMethodAdapter(
                    script.getClassVisitor(),
                    ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC,
                    methodName,
                    StandardASMCompiler.getStaticMethodSignature(script.getClassname(), 4),
                    null,
                    null);
            method.start();

            // check arity in the variable-arity version
            method.aload(1);
            method.aload(3);
            method.pushInt(scope.getRequiredArgs());
            invokeUtilityMethod("checkArgumentCount", sig(void.class, ThreadContext.class, IRubyObject[].class, int.class));

            loadThis();
            loadThreadContext();
            loadSelf();
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    public boolean isSimpleRoot() {
        return !inNestedMethod;
    }
}
