// automatically generated, do not modify

package org.jruby.ir.persistence.flat;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class ReceiveSelfFlat extends Table {
  public static ReceiveSelfFlat getRootAsReceiveSelfFlat(ByteBuffer _bb) { return getRootAsReceiveSelfFlat(_bb, new ReceiveSelfFlat()); }
  public static ReceiveSelfFlat getRootAsReceiveSelfFlat(ByteBuffer _bb, ReceiveSelfFlat obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__init(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public ReceiveSelfFlat __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; return this; }

  public byte dummy() { int o = __offset(4); return o != 0 ? bb.get(o + bb_pos) : 0; }

  public static int createReceiveSelfFlat(FlatBufferBuilder builder,
      byte dummy) {
    builder.startObject(1);
    ReceiveSelfFlat.addDummy(builder, dummy);
    return ReceiveSelfFlat.endReceiveSelfFlat(builder);
  }

  public static void startReceiveSelfFlat(FlatBufferBuilder builder) { builder.startObject(1); }
  public static void addDummy(FlatBufferBuilder builder, byte dummy) { builder.addByte(0, dummy, 0); }
  public static int endReceiveSelfFlat(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
};

