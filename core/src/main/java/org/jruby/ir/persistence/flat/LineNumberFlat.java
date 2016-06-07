// automatically generated, do not modify

package org.jruby.ir.persistence.flat;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class LineNumberFlat extends Table {
  public static LineNumberFlat getRootAsLineNumberFlat(ByteBuffer _bb) { return getRootAsLineNumberFlat(_bb, new LineNumberFlat()); }
  public static LineNumberFlat getRootAsLineNumberFlat(ByteBuffer _bb, LineNumberFlat obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__init(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public LineNumberFlat __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; return this; }

  public int line() { int o = __offset(4); return o != 0 ? bb.getInt(o + bb_pos) : 0; }

  public static int createLineNumberFlat(FlatBufferBuilder builder,
      int line) {
    builder.startObject(1);
    LineNumberFlat.addLine(builder, line);
    return LineNumberFlat.endLineNumberFlat(builder);
  }

  public static void startLineNumberFlat(FlatBufferBuilder builder) { builder.startObject(1); }
  public static void addLine(FlatBufferBuilder builder, int line) { builder.addInt(0, line, 0); }
  public static int endLineNumberFlat(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
};

