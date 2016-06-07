// automatically generated, do not modify

package org.jruby.ir.persistence.flat;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class StringLiteralFlat extends Table {
  public static StringLiteralFlat getRootAsStringLiteralFlat(ByteBuffer _bb) { return getRootAsStringLiteralFlat(_bb, new StringLiteralFlat()); }
  public static StringLiteralFlat getRootAsStringLiteralFlat(ByteBuffer _bb, StringLiteralFlat obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__init(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public StringLiteralFlat __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; return this; }

  public FrozenStringFlat frozenString() { return frozenString(new FrozenStringFlat()); }
  public FrozenStringFlat frozenString(FrozenStringFlat obj) { int o = __offset(4); return o != 0 ? obj.__init(__indirect(o + bb_pos), bb) : null; }

  public static int createStringLiteralFlat(FlatBufferBuilder builder,
      int frozenStringOffset) {
    builder.startObject(1);
    StringLiteralFlat.addFrozenString(builder, frozenStringOffset);
    return StringLiteralFlat.endStringLiteralFlat(builder);
  }

  public static void startStringLiteralFlat(FlatBufferBuilder builder) { builder.startObject(1); }
  public static void addFrozenString(FlatBufferBuilder builder, int frozenStringOffset) { builder.addOffset(0, frozenStringOffset, 0); }
  public static int endStringLiteralFlat(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
};

