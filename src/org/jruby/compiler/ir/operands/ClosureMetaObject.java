package org.jruby.compiler.ir.operands;

import org.jruby.compiler.ir.IRClosure;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.builtin.IRubyObject;

public class ClosureMetaObject extends MetaObject {
    protected ClosureMetaObject(IRClosure scope) {
        super(scope);
    }

    @Override
    public boolean isClosure() {
        return true;
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
        BlockBody body = ((IRClosure) scope).getBlockBody();
        scope.getStaticScope().determineModule();
        Binding binding = interp.getContext().currentBinding((IRubyObject) interp.getSelf(), interp.getSharedBindingScope());

        return new Block(body, binding);
    }
}
