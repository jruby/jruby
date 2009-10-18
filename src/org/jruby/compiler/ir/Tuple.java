package org.jruby.compiler.ir;

public class Tuple<T1, T2>
{
    final public T1 _a;
    final public T2 _b;
    public Tuple(T1 a, T2 b) { _a = a; _b = b; }
}
