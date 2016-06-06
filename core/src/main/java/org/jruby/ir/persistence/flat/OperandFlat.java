// automatically generated, do not modify

package org.jruby.ir.persistence.flat;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class OperandFlat extends Table {
  public static OperandFlat getRootAsOperandFlat(ByteBuffer _bb) { return getRootAsOperandFlat(_bb, new OperandFlat()); }
  public static OperandFlat getRootAsOperandFlat(ByteBuffer _bb, OperandFlat obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__init(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public OperandFlat __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; return this; }

  public byte operandType() { int o = __offset(4); return o != 0 ? bb.get(o + bb_pos) : 0; }

  public static int createOperandFlat(FlatBufferBuilder builder,
      byte operandType) {
    builder.startObject(1);
    OperandFlat.addOperandType(builder, operandType);
    return OperandFlat.endOperandFlat(builder);
  }

  public static void startOperandFlat(FlatBufferBuilder builder) { builder.startObject(1); }
  public static void addOperandType(FlatBufferBuilder builder, byte operandType) { builder.addByte(0, operandType, 0); }
  public static int endOperandFlat(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
};

