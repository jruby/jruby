package org.jruby.compiler.ir.operands;

import org.jruby.interpreter.InterpreterContext;

public class BooleanLiteral extends Constant {
    private BooleanLiteral() { }

    public static final BooleanLiteral TRUE  = new BooleanLiteral();
    public static final BooleanLiteral FALSE = new BooleanLiteral();

    public boolean isTrue()  {
        return this == TRUE;
    }

    public boolean isFalse() {
        return this == FALSE;
    }

    public BooleanLiteral logicalNot() {
        return isTrue() ? FALSE : TRUE;
    }
    
    @Override
    public String toString() {
        return isTrue() ? "true" : "false";
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
		  if (cachedValue == null)
            cachedValue = interp.getRuntime().newBoolean(isTrue());
		  return cachedValue;
    }
}
