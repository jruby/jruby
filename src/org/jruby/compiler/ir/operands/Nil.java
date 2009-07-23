package org.jruby.compiler.ir.operands;

// Records the nil object
public class Nil extends Constant
{
    public static final Nil NIL = new Nil();

    private Nil() { }

    public String toString() { return "nil"; }

    public Operand fetchCompileTimeArrayElement(int argIndex, boolean getSubArray) { return Nil.NIL; }
}
