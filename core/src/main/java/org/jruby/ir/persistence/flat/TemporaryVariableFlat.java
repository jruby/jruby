// automatically generated, do not modify

package org.jruby.ir.persistence.flat;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class TemporaryVariableFlat extends Table {
  public static TemporaryVariableFlat getRootAsTemporaryVariableFlat(ByteBuffer _bb) { return getRootAsTemporaryVariableFlat(_bb, new TemporaryVariableFlat()); }
  public static TemporaryVariableFlat getRootAsTemporaryVariableFlat(ByteBuffer _bb, TemporaryVariableFlat obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__init(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public TemporaryVariableFlat __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; return this; }

  public int offset() { int o = __offset(4); return o != 0 ? bb.getInt(o + bb_pos) : 0; }

  public static int createTemporaryVariableFlat(FlatBufferBuilder builder,
      int offset) {
    builder.startObject(1);
    TemporaryVariableFlat.addOffset(builder, offset);
    return TemporaryVariableFlat.endTemporaryVariableFlat(builder);
  }

  public static void startTemporaryVariableFlat(FlatBufferBuilder builder) { builder.startObject(1); }
  public static void addOffset(FlatBufferBuilder builder, int offset) { builder.addInt(0, offset, 0); }
  public static int endTemporaryVariableFlat(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
};

