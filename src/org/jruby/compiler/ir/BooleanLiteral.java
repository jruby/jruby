package org.jruby.compiler.ir;

public class BooleanLiteral extends Constant
{
    private BooleanLiteral() { }

    public static final BooleanLiteral TRUE  = new BooleanLiteral();
    public static final BooleanLiteral FALSE = new BooleanLiteral();
   
    public boolean isTrue()  { return this == TRUE; }
    public boolean isFalse() { return this == FALSE; }
}
