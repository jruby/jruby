// automatically generated, do not modify

package org.jruby.ir.persistence.flat;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class InstrFlat extends Table {
  public static InstrFlat getRootAsInstrFlat(ByteBuffer _bb) { return getRootAsInstrFlat(_bb, new InstrFlat()); }
  public static InstrFlat getRootAsInstrFlat(ByteBuffer _bb, InstrFlat obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__init(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public InstrFlat __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; return this; }

  public short operation() { int o = __offset(4); return o != 0 ? bb.getShort(o + bb_pos) : 0; }
  public OperandFlat operands(int j) { return operands(new OperandFlat(), j); }
  public OperandFlat operands(OperandFlat obj, int j) { int o = __offset(6); return o != 0 ? obj.__init(__indirect(__vector(o) + j * 4), bb) : null; }
  public int operandsLength() { int o = __offset(6); return o != 0 ? __vector_len(o) : 0; }

  public static int createInstrFlat(FlatBufferBuilder builder,
      short operation,
      int operandsOffset) {
    builder.startObject(2);
    InstrFlat.addOperands(builder, operandsOffset);
    InstrFlat.addOperation(builder, operation);
    return InstrFlat.endInstrFlat(builder);
  }

  public static void startInstrFlat(FlatBufferBuilder builder) { builder.startObject(2); }
  public static void addOperation(FlatBufferBuilder builder, short operation) { builder.addShort(0, operation, 0); }
  public static void addOperands(FlatBufferBuilder builder, int operandsOffset) { builder.addOffset(1, operandsOffset, 0); }
  public static int createOperandsVector(FlatBufferBuilder builder, int[] data) { builder.startVector(4, data.length, 4); for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]); return builder.endVector(); }
  public static void startOperandsVector(FlatBufferBuilder builder, int numElems) { builder.startVector(4, numElems, 4); }
  public static int endInstrFlat(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
};

