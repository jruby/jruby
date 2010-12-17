package org.jruby.interpreter;

import org.jruby.compiler.ir.IRMethod;

public class InlineMethodHint extends RuntimeException {
   public final IRMethod inlineableMethod;
   public InlineMethodHint(IRMethod m) { inlineableMethod = m; }
}
