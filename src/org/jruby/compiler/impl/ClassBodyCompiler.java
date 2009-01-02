package org.jruby.compiler.impl;

import org.jruby.Ruby;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.CompilerCallback;
import org.jruby.exceptions.JumpException;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.util.CodegenUtils.*;

public class ClassBodyCompiler extends RootScopedBodyCompiler {
    public ClassBodyCompiler(StandardASMCompiler scriptCompiler, String friendlyName, ASTInspector inspector, StaticScope scope) {
        super(scriptCompiler, friendlyName, inspector, scope);
    }

    @Override
    public void beginMethod(CompilerCallback bodyPrep, StaticScope scope) {
        method.start();

        // set up a local IRuby variable
        method.aload(StandardASMCompiler.THREADCONTEXT_INDEX);
        invokeThreadContext("getRuntime", sig(Ruby.class));
        method.dup();
        method.astore(getRuntimeIndex());

        // grab nil for local variables
        invokeRuby("getNil", sig(IRubyObject.class));
        method.astore(getNilIndex());

        variableCompiler.beginClass(bodyPrep, scope);

        // visit a label to start scoping for local vars in this method
        method.label(scopeStart);
    }

    @Override
    public void performReturn() {
        // return in a class body raises error
        loadThreadContext();
        invokeUtilityMethod("returnJump", sig(JumpException.ReturnJump.class, IRubyObject.class, ThreadContext.class));
        method.athrow();
    }
}
