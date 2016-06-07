// automatically generated, do not modify

package org.jruby.ir.persistence.flat;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class SelfFlat extends Table {
  public static SelfFlat getRootAsSelfFlat(ByteBuffer _bb) { return getRootAsSelfFlat(_bb, new SelfFlat()); }
  public static SelfFlat getRootAsSelfFlat(ByteBuffer _bb, SelfFlat obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__init(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public SelfFlat __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; return this; }

  public byte dummy() { int o = __offset(4); return o != 0 ? bb.get(o + bb_pos) : 0; }

  public static int createSelfFlat(FlatBufferBuilder builder,
      byte dummy) {
    builder.startObject(1);
    SelfFlat.addDummy(builder, dummy);
    return SelfFlat.endSelfFlat(builder);
  }

  public static void startSelfFlat(FlatBufferBuilder builder) { builder.startObject(1); }
  public static void addDummy(FlatBufferBuilder builder, byte dummy) { builder.addByte(0, dummy, 0); }
  public static int endSelfFlat(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
};

