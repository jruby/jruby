package org.jruby.compiler.ir.operands;

import org.jruby.compiler.ir.IRClass;

import java.math.BigInteger;
import org.jruby.RubyBignum;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class Bignum extends Constant {

    final public BigInteger value;

    public Bignum(BigInteger value) {
        this.value = value;
    }

    @Override
    public String toString() { 
        return value + ":bignum";
    }

    @Override
    public IRClass getTargetClass() {
        return IRClass.getCoreClass("Bignum");
    }

    @Override
    public Object retrieve(InterpreterContext interp, ThreadContext context, IRubyObject self) {
/*
        if (cachedValue == null) cachedValue = RubyBignum.newBignum(interp.getRuntime(), value);
        return cachedValue;
*/
        return RubyBignum.newBignum(context.getRuntime(), value);
    }
}
