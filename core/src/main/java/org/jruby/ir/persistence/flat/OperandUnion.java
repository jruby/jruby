// automatically generated, do not modify

package org.jruby.ir.persistence.flat;

public final class OperandUnion {
  private OperandUnion() { }
  public static final byte NONE = 0;
  public static final byte StringLiteralFlat = 1;
  public static final byte FrozenStringFlat = 2;
  public static final byte TemporaryVariableFlat = 3;
  public static final byte CurrentScopeFlat = 4;
  public static final byte ScopeModuleFlat = 5;
  public static final byte SelfFlat = 6;

  private static final String[] names = { "NONE", "StringLiteralFlat", "FrozenStringFlat", "TemporaryVariableFlat", "CurrentScopeFlat", "ScopeModuleFlat", "SelfFlat", };

  public static String name(int e) { return names[e]; }
};

