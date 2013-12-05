package org.jruby.compiler.impl;

import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.CompilerCallback;
import org.jruby.compiler.NotCompilableException;
import org.jruby.exceptions.JumpException;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.Label;
import static org.jruby.util.CodegenUtils.*;

public class ChildScopedBodyCompiler extends BaseBodyCompiler {

    public ChildScopedBodyCompiler(StandardASMCompiler scriptCompiler, String closureMethodName, String rubyName, ASTInspector inspector, StaticScope scope, int scopeIndex) {
        super(scriptCompiler, closureMethodName, rubyName, inspector, scope, scopeIndex);
    }

    @Override
    protected int getActualArgsCount(StaticScope scope) {
        // always 1, since we pass in an Array (1.8) or an IRubyObject[] (1.9)
        return 1;
    }

    protected String getSignature() {
        return StandardASMCompiler.getStaticClosureSignature(script.getClassname());
    }

    protected void createVariableCompiler() {
        if (inspector == null) {
            variableCompiler = new HeapBasedVariableCompiler(this, method, scope, false, StandardASMCompiler.ARGS_INDEX, getFirstTempIndex());
        } else if (inspector.hasClosure() || inspector.hasScopeAwareMethods()) {
            variableCompiler = new HeapBasedVariableCompiler(this, method, scope, false, StandardASMCompiler.ARGS_INDEX, getFirstTempIndex());
        } else {
            variableCompiler = new StackBasedVariableCompiler(this, method, scope, false, StandardASMCompiler.ARGS_INDEX, getFirstTempIndex());
        }
    }

    public void beginMethod(CompilerCallback args, StaticScope scope) {
        method.start();

        if (scope == null) {
            // not using a new scope, use saved one for a flat closure
            variableCompiler.beginFlatClosure(args, this.scope);
        } else {
            // normal closure
            variableCompiler.beginClosure(args, scope);
        }
        redoJump = new Label();
        method.label(scopeStart);
    }

    public void beginClass(CompilerCallback bodyPrep, StaticScope scope) {
        throw new NotCompilableException("ERROR: closure compiler should not be used for class bodies");
    }

    public ChainedChildBodyCompiler outline(String methodName) {
        // chain to the next segment of this giant method
        method.aload(StandardASMCompiler.THIS);

        // load all arguments straight through
        for (int i = 1; i <= 4; i++) {
            method.aload(i);
        }
        // we append an index to ensure two identical method names will not conflict
        // TODO: make this match general method name structure with SYNTHETIC in place
        methodName = "chained_" + script.getAndIncrementMethodIndex() + "_" + methodName;
        method.invokestatic(script.getClassname(), methodName, getSignature());

        ChainedChildBodyCompiler methodCompiler = new ChainedChildBodyCompiler(script, methodName, rubyName, inspector, scope, this);

        methodCompiler.beginChainedMethod();

        return methodCompiler;
    }

    public void endBody() {
        // end of scoping for closure's vars
        method.areturn();
        method.label(scopeEnd);

        // we only need full-on redo exception handling if there's logic that might produce it
        if (inspector == null || inspector.hasScopeAwareMethods()) {
            // handle redos by restarting the block
            method.pop();
            method.go_to(scopeStart);

            method.trycatch(scopeStart, scopeEnd, scopeEnd, p(JumpException.RedoJump.class));
        }

        // method is done, declare all variables
        variableCompiler.declareLocals(scope, scopeStart, scopeEnd);

        method.end();
    }

    @Override
    public void loadBlock() {
        loadThreadContext();
        invokeThreadContext("getFrameBlock", sig(Block.class));
    }

    public void performReturn() {
        loadThreadContext();
        invokeUtilityMethod("throwReturnJump", sig(IRubyObject.class, IRubyObject.class, ThreadContext.class));
    }

    public void issueBreakEvent(CompilerCallback value) {
        if (currentLoopLabels != null) {
            value.call(this);
            issueLoopBreak();
        } else {
            loadThreadContext();
            value.call(this);
            invokeUtilityMethod("breakJump", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class));
        }
    }

    public void issueNextEvent(CompilerCallback value) {
        if (currentLoopLabels != null) {
            value.call(this);
            issueLoopNext();
        } else {
            value.call(this);
            invokeUtilityMethod("nextJump", sig(IRubyObject.class, IRubyObject.class));
        }
    }

    public void issueRedoEvent() {
        // FIXME: This isn't right for within ensured/rescued code
        if (currentLoopLabels != null) {
            issueLoopRedo();
        } else if (inNestedMethod) {
            invokeUtilityMethod("redoJump", sig(IRubyObject.class));
        } else {
            // jump back to the top of the main body of this closure
            method.go_to(scopeStart);
        }
    }

    public boolean isSimpleRoot() {
        return false;
    }
}
