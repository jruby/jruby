package org.jruby.compiler.ir.operands;

import java.math.BigInteger;
import org.jruby.RubyBignum;
import org.jruby.interpreter.InterpreterContext;

public class Bignum extends Constant {

    final public BigInteger value;

    public Bignum(BigInteger value) {
        this.value = value;
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
        if (cachedValue == null) cachedValue = RubyBignum.newBignum(interp.getRuntime(), value);

        return cachedValue;
    }
}
