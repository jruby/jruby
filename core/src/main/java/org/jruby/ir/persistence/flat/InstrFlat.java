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

  public TemporaryVariableFlat result() { return result(new TemporaryVariableFlat()); }
  public TemporaryVariableFlat result(TemporaryVariableFlat obj) { int o = __offset(4); return o != 0 ? obj.__init(__indirect(o + bb_pos), bb) : null; }
  public OperandFlat operands(int j) { return operands(new OperandFlat(), j); }
  public OperandFlat operands(OperandFlat obj, int j) { int o = __offset(6); return o != 0 ? obj.__init(__indirect(__vector(o) + j * 4), bb) : null; }
  public int operandsLength() { int o = __offset(6); return o != 0 ? __vector_len(o) : 0; }
  public byte instrType() { int o = __offset(8); return o != 0 ? bb.get(o + bb_pos) : 0; }
  public Table instr(Table obj) { int o = __offset(10); return o != 0 ? __union(obj, o) : null; }

  public static int createInstrFlat(FlatBufferBuilder builder,
      int resultOffset,
      int operandsOffset,
      byte instr_type,
      int instrOffset) {
    builder.startObject(4);
    InstrFlat.addInstr(builder, instrOffset);
    InstrFlat.addOperands(builder, operandsOffset);
    InstrFlat.addResult(builder, resultOffset);
    InstrFlat.addInstrType(builder, instr_type);
    return InstrFlat.endInstrFlat(builder);
  }

  public static void startInstrFlat(FlatBufferBuilder builder) { builder.startObject(4); }
  public static void addResult(FlatBufferBuilder builder, int resultOffset) { builder.addOffset(0, resultOffset, 0); }
  public static void addOperands(FlatBufferBuilder builder, int operandsOffset) { builder.addOffset(1, operandsOffset, 0); }
  public static int createOperandsVector(FlatBufferBuilder builder, int[] data) { builder.startVector(4, data.length, 4); for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]); return builder.endVector(); }
  public static void startOperandsVector(FlatBufferBuilder builder, int numElems) { builder.startVector(4, numElems, 4); }
  public static void addInstrType(FlatBufferBuilder builder, byte instrType) { builder.addByte(2, instrType, 0); }
  public static void addInstr(FlatBufferBuilder builder, int instrOffset) { builder.addOffset(3, instrOffset, 0); }
  public static int endInstrFlat(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
};

