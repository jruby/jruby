package org.jruby.compiler.impl;

import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.CompilerCallback;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.util.CodegenUtils.*;

public class ClassBodyCompiler extends RootScopedBodyCompiler {
    public ClassBodyCompiler(StandardASMCompiler scriptCompiler, String friendlyName, String rubyName, ASTInspector inspector, StaticScope scope, int scopeIndex) {
        super(scriptCompiler, friendlyName, rubyName, inspector, scope, scopeIndex);
    }

    @Override
    public void beginMethod(CompilerCallback bodyPrep, StaticScope scope) {
        method.start();

        variableCompiler.beginClass(scope);

        // visit a label to start scoping for local vars in this method
        method.label(scopeStart);
    }

    @Override
    public void performReturn() {
        if (inRescue) {
            // returning from rescue, clear $!
            clearErrorInfo();
        }
        
        // return in a class body raises error
        loadThreadContext();
        invokeUtilityMethod("throwReturnJump", sig(IRubyObject.class, IRubyObject.class, ThreadContext.class));
    }

    public boolean isSimpleRoot() {
        return false;
    }
}
