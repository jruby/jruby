package org.jruby.ir.interpreter;

import org.jruby.ir.IRMethod;

public class InlineMethodHint extends RuntimeException {
   public final IRMethod inlineableMethod;
   public InlineMethodHint(IRMethod m) { inlineableMethod = m; }
}
