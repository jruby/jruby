package org.jruby.compiler.ir;

import java.math.BigInteger;

public class Fixnum extends Constant
{
    final public Long _value;

    public Fixnum(Long val) { _value = val; }
    public Fixnum(BigInteger val) { _value = val.longValue(); }

    public String toString() {
        return "(fixnum: " + _value + ")";
    }
}
