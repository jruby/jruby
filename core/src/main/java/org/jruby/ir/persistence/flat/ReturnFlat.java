// automatically generated, do not modify

package org.jruby.ir.persistence.flat;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class ReturnFlat extends Table {
  public static ReturnFlat getRootAsReturnFlat(ByteBuffer _bb) { return getRootAsReturnFlat(_bb, new ReturnFlat()); }
  public static ReturnFlat getRootAsReturnFlat(ByteBuffer _bb, ReturnFlat obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__init(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public ReturnFlat __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; return this; }

  public byte dummy() { int o = __offset(4); return o != 0 ? bb.get(o + bb_pos) : 0; }

  public static int createReturnFlat(FlatBufferBuilder builder,
      byte dummy) {
    builder.startObject(1);
    ReturnFlat.addDummy(builder, dummy);
    return ReturnFlat.endReturnFlat(builder);
  }

  public static void startReturnFlat(FlatBufferBuilder builder) { builder.startObject(1); }
  public static void addDummy(FlatBufferBuilder builder, byte dummy) { builder.addByte(0, dummy, 0); }
  public static int endReturnFlat(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
};

