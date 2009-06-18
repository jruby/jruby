package org.jruby.compiler.ir;

public class DynamicSymbol extends Reference
{
      // SSS FIXME: Should this be Operand or CompoundString?
      // Can it happen that symbols are built out of other than compound strings?  
      // Or can it happen during optimizations that this becomes a generic operand?
   public final CompoundString _symName;

   public DynamicSymbol(CompoundString s) { super(null); _symName = s; }
}
