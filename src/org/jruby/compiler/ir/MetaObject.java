package org.jruby.compiler.ir;

public class MetaObject extends Operand
{
   public final IR_Scope _scope;

   public MetaObject(IR_Scope s) { _scope = s; }
}
