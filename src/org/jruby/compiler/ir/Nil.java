package org.jruby.compiler.ir;

// Records the nil object
public class Nil extends Constant
{
    public static final Nil NIL = new Nil();

    private Nil() { }

    public String toString() { return "nil"; }
}
