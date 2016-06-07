// automatically generated, do not modify

package org.jruby.ir.persistence.flat;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class CallFlat extends Table {
  public static CallFlat getRootAsCallFlat(ByteBuffer _bb) { return getRootAsCallFlat(_bb, new CallFlat()); }
  public static CallFlat getRootAsCallFlat(ByteBuffer _bb, CallFlat obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__init(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public CallFlat __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; return this; }

  public int callType() { int o = __offset(4); return o != 0 ? bb.getInt(o + bb_pos) : 0; }
  public String name() { int o = __offset(6); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer nameAsByteBuffer() { return __vector_as_bytebuffer(6, 1); }
  public int argsCount() { int o = __offset(8); return o != 0 ? bb.getInt(o + bb_pos) : 0; }
  public boolean hasClosure() { int o = __offset(10); return o != 0 ? 0!=bb.get(o + bb_pos) : false; }
  public boolean canBeEval() { int o = __offset(12); return o != 0 ? 0!=bb.get(o + bb_pos) : false; }
  public boolean targetRequiresCallersBinding() { int o = __offset(14); return o != 0 ? 0!=bb.get(o + bb_pos) : false; }
  public boolean targetRequiresCallersFrame() { int o = __offset(16); return o != 0 ? 0!=bb.get(o + bb_pos) : false; }
  public boolean splatMap(int j) { int o = __offset(18); return o != 0 ? 0!=bb.get(__vector(o) + j * 1) : false; }
  public int splatMapLength() { int o = __offset(18); return o != 0 ? __vector_len(o) : 0; }
  public ByteBuffer splatMapAsByteBuffer() { return __vector_as_bytebuffer(18, 1); }
  public boolean potentiallyRefined() { int o = __offset(20); return o != 0 ? 0!=bb.get(o + bb_pos) : false; }

  public static int createCallFlat(FlatBufferBuilder builder,
      int callType,
      int nameOffset,
      int argsCount,
      boolean hasClosure,
      boolean canBeEval,
      boolean targetRequiresCallersBinding,
      boolean targetRequiresCallersFrame,
      int splatMapOffset,
      boolean potentiallyRefined) {
    builder.startObject(9);
    CallFlat.addSplatMap(builder, splatMapOffset);
    CallFlat.addArgsCount(builder, argsCount);
    CallFlat.addName(builder, nameOffset);
    CallFlat.addCallType(builder, callType);
    CallFlat.addPotentiallyRefined(builder, potentiallyRefined);
    CallFlat.addTargetRequiresCallersFrame(builder, targetRequiresCallersFrame);
    CallFlat.addTargetRequiresCallersBinding(builder, targetRequiresCallersBinding);
    CallFlat.addCanBeEval(builder, canBeEval);
    CallFlat.addHasClosure(builder, hasClosure);
    return CallFlat.endCallFlat(builder);
  }

  public static void startCallFlat(FlatBufferBuilder builder) { builder.startObject(9); }
  public static void addCallType(FlatBufferBuilder builder, int callType) { builder.addInt(0, callType, 0); }
  public static void addName(FlatBufferBuilder builder, int nameOffset) { builder.addOffset(1, nameOffset, 0); }
  public static void addArgsCount(FlatBufferBuilder builder, int argsCount) { builder.addInt(2, argsCount, 0); }
  public static void addHasClosure(FlatBufferBuilder builder, boolean hasClosure) { builder.addBoolean(3, hasClosure, false); }
  public static void addCanBeEval(FlatBufferBuilder builder, boolean canBeEval) { builder.addBoolean(4, canBeEval, false); }
  public static void addTargetRequiresCallersBinding(FlatBufferBuilder builder, boolean targetRequiresCallersBinding) { builder.addBoolean(5, targetRequiresCallersBinding, false); }
  public static void addTargetRequiresCallersFrame(FlatBufferBuilder builder, boolean targetRequiresCallersFrame) { builder.addBoolean(6, targetRequiresCallersFrame, false); }
  public static void addSplatMap(FlatBufferBuilder builder, int splatMapOffset) { builder.addOffset(7, splatMapOffset, 0); }
  public static int createSplatMapVector(FlatBufferBuilder builder, boolean[] data) { builder.startVector(1, data.length, 1); for (int i = data.length - 1; i >= 0; i--) builder.addBoolean(data[i]); return builder.endVector(); }
  public static void startSplatMapVector(FlatBufferBuilder builder, int numElems) { builder.startVector(1, numElems, 1); }
  public static void addPotentiallyRefined(FlatBufferBuilder builder, boolean potentiallyRefined) { builder.addBoolean(8, potentiallyRefined, false); }
  public static int endCallFlat(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
};

