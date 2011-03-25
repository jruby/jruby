package org.jruby.compiler.ir.operands;

// Records the nil object

import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class Nil extends Constant {
    public static final Nil NIL = new Nil();

    protected Nil() { }

    @Override
    public String toString() { 
        return "nil";
    }

    @Override
    public Operand fetchCompileTimeArrayElement(int argIndex, boolean getSubArray) { 
        return Nil.NIL;
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
		  if (cachedValue == null)
            cachedValue = interp.getRuntime().getNil();
		  return cachedValue;
    }
}
