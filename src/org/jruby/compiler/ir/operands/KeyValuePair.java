package org.jruby.compiler.ir.operands;

public class KeyValuePair
{
   public final Operand _key;
   public final Operand _value;

   public KeyValuePair(Operand k, Operand v) { _key = k; _value = v; }
}
