// automatically generated, do not modify

package org.jruby.ir.persistence.flat;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class CopyFlat extends Table {
  public static CopyFlat getRootAsCopyFlat(ByteBuffer _bb) { return getRootAsCopyFlat(_bb, new CopyFlat()); }
  public static CopyFlat getRootAsCopyFlat(ByteBuffer _bb, CopyFlat obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__init(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public CopyFlat __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; return this; }

  public byte dummy() { int o = __offset(4); return o != 0 ? bb.get(o + bb_pos) : 0; }

  public static int createCopyFlat(FlatBufferBuilder builder,
      byte dummy) {
    builder.startObject(1);
    CopyFlat.addDummy(builder, dummy);
    return CopyFlat.endCopyFlat(builder);
  }

  public static void startCopyFlat(FlatBufferBuilder builder) { builder.startObject(1); }
  public static void addDummy(FlatBufferBuilder builder, byte dummy) { builder.addByte(0, dummy, 0); }
  public static int endCopyFlat(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
};

