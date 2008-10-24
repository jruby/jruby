package org.jruby.compiler.impl;

import org.jruby.Ruby;
import org.jruby.compiler.ASTInspector;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.Label;
import static org.jruby.util.CodegenUtils.*;

public class ChainedChildBodyCompiler extends ChildScopedBodyCompiler {
    ChildScopedBodyCompiler parent;

    @Override
    public void endBody() {
        // return last value from execution
        method.areturn();
        Label end = new Label();
        method.label(end);

        method.end();
    }

    public ChainedChildBodyCompiler(StandardASMCompiler scriptCompiler, String methodName, ASTInspector inspector, StaticScope scope, ChildScopedBodyCompiler parent) {
        super(scriptCompiler, methodName, inspector, scope);
        this.parent = parent;
        this.inNestedMethod = true;
    }

    public void beginChainedMethod() {
        method.start();

        method.aload(StandardASMCompiler.THREADCONTEXT_INDEX);
        method.dup();
        method.invokevirtual(p(ThreadContext.class), "getRuntime", sig(Ruby.class));
        method.dup();
        method.astore(getRuntimeIndex());

        // grab nil for local variables
        method.invokevirtual(p(Ruby.class), "getNil", sig(IRubyObject.class));
        method.astore(getNilIndex());

        method.invokevirtual(p(ThreadContext.class), "getCurrentScope", sig(DynamicScope.class));
        method.dup();
        method.astore(getDynamicScopeIndex());
        method.invokevirtual(p(DynamicScope.class), "getValues", sig(IRubyObject[].class));
        method.astore(getVarsArrayIndex());

        // visit a label to start scoping for local vars in this method
        method.label(scopeStart);
    }
}
