// automatically generated, do not modify

package org.jruby.ir.persistence.flat;

public final class InstrUnion {
  private InstrUnion() { }
  public static final byte NONE = 0;
  public static final byte CopyFlat = 1;
  public static final byte ReceiveSelfFlat = 2;
  public static final byte LineNumberFlat = 3;
  public static final byte CallFlat = 4;
  public static final byte ReturnFlat = 5;
  public static final byte LoadImplicitClosureFlat = 6;
  public static final byte LoadFrameClosureFlat = 7;

  private static final String[] names = { "NONE", "CopyFlat", "ReceiveSelfFlat", "LineNumberFlat", "CallFlat", "ReturnFlat", "LoadImplicitClosureFlat", "LoadFrameClosureFlat", };

  public static String name(int e) { return names[e]; }
};

