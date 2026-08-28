package org.jruby.ir.targets.simple;

import org.jruby.ir.IRMethod;
import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.ir.targets.LocalVariableCompiler;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.DynamicScopeGenerator;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class NormalLocalVariableCompiler implements LocalVariableCompiler {
    private final IRBytecodeAdapter compiler;

    public NormalLocalVariableCompiler(IRBytecodeAdapter irBytecodeAdapter) {
        compiler = irBytecodeAdapter;
    }

    @Override
    public void getHeapLocal(int depth, int location) {
        // We can only use the fast path with no null checking in methods, since closures may JIT independently
        // atop methods that do not guarantee all scoped vars are initialized. See jruby/jruby#4235.
        if (depth == 0) {
            if (location < DynamicScopeGenerator.SPECIALIZED_GETS.size()) {
                compiler.adapter.invokevirtual(p(DynamicScope.class), DynamicScopeGenerator.SPECIALIZED_GETS.get(location), sig(IRubyObject.class));
            } else {
                compiler.adapter.pushInt(location);
                compiler.adapter.invokevirtual(p(DynamicScope.class), "getValueDepthZero", sig(IRubyObject.class, int.class));
            }
        } else {
            compiler.adapter.pushInt(location);
            compiler.adapter.pushInt(depth);
            compiler.adapter.invokevirtual(p(DynamicScope.class), "getValue", sig(IRubyObject.class, int.class, int.class));
        }
    }

    @Override
    public void getHeapLocalOrNil(int depth, int location) {
        if (depth == 0) {
            if (location < DynamicScopeGenerator.SPECIALIZED_GETS_OR_NIL.size()) {
                compiler.getValueCompiler().pushNil();
                compiler.adapter.invokevirtual(p(DynamicScope.class), DynamicScopeGenerator.SPECIALIZED_GETS_OR_NIL.get(location), sig(IRubyObject.class, IRubyObject.class));
            } else {
                compiler.adapter.pushInt(location);
                compiler.getValueCompiler().pushNil();
                compiler.adapter.invokevirtual(p(DynamicScope.class), "getValueDepthZeroOrNil", sig(IRubyObject.class, int.class, IRubyObject.class));
            }
        } else {
            compiler.adapter.pushInt(location);
            compiler.adapter.pushInt(depth);
            compiler.getValueCompiler().pushNil();
            compiler.adapter.invokevirtual(p(DynamicScope.class), "getValueOrNil", sig(IRubyObject.class, int.class, int.class, IRubyObject.class));
        }
    }
}
