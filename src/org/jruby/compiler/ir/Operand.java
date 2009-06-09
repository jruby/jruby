package org.jruby.compiler.ir;

public abstract class Operand
{
   public static final Operand TOP    = new LatticeTop();
   public static final Operand BOTTOM = new LatticeBottom();
   public static final Operand ANY    = new Anything();

	/* Lattice TOP, BOTTOM, ANY values */
   private static class LatticeBottom extends Operand
   {
      LatticeBottom() { _type = (short)(NONE); }

      public String toString() { return "bottom"; }
   }

   private static class LatticeTop extends Operand
   {
      LatticeTop() { _type = (short)(NONE); }

      public String toString() { return "top"; }
      public Operand Compute_CP_Meet(Operand op) { return op; }
   }

   private static class Anything extends Operand
   {
      Anything() { _type = (short)(NONE); }

      public String toString() { return "anything"; }
      public Operand Compute_CP_Meet(Operand op) { return op; }
   }
}
