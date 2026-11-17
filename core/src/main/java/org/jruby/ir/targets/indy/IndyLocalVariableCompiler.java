package org.jruby.ir.targets.indy;

import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.ir.targets.LocalVariableCompiler;
import org.jruby.ir.targets.simple.NormalLocalVariableCompiler;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.DynamicScopeGenerator;

import static org.jruby.util.CodegenUtils.sig;

public class IndyLocalVariableCompiler implements LocalVariableCompiler {
    private final IRBytecodeAdapter compiler;
    private final NormalLocalVariableCompiler normalLocalVariableCompiler;

    public IndyLocalVariableCompiler(IRBytecodeAdapter irBytecodeAdapter) {
        compiler = irBytecodeAdapter;
        normalLocalVariableCompiler = new NormalLocalVariableCompiler(irBytecodeAdapter);
    }

    @Override
    public void getHeapLocal(int depth, int location) {
        if (depth == 0 && location < DynamicScopeGenerator.SPECIALIZED_GETS.size()) {
            // just use normal compiler, since it's just going to be ALOAD + INVOKEVIRTUAL anyway
            normalLocalVariableCompiler.getHeapLocal(depth, location);
            return;
        }
        compiler.adapter.invokedynamic("getHeapLocal", sig(IRubyObject.class, DynamicScope.class), HeapVariableBootstrap.GET_HEAP_LOCAL_BOOTSTRAP, depth, location);
    }

    @Override
    public void getHeapLocalOrNil(int depth, int location) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("getHeapLocalOrNil", sig(IRubyObject.class, DynamicScope.class, ThreadContext.class), HeapVariableBootstrap.GET_HEAP_LOCAL_OR_NIL_BOOTSTRAP, depth, location);
    }
}
