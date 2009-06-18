package org.jruby.compiler.ir;

public class Float extends Constant
{
    final public Double _value;

    public Float(Double val) { _value = val; }
}
