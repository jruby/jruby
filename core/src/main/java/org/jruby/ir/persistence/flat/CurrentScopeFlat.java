// automatically generated, do not modify

package org.jruby.ir.persistence.flat;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class CurrentScopeFlat extends Table {
  public static CurrentScopeFlat getRootAsCurrentScopeFlat(ByteBuffer _bb) { return getRootAsCurrentScopeFlat(_bb, new CurrentScopeFlat()); }
  public static CurrentScopeFlat getRootAsCurrentScopeFlat(ByteBuffer _bb, CurrentScopeFlat obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__init(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public CurrentScopeFlat __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; return this; }

  public byte dummy() { int o = __offset(4); return o != 0 ? bb.get(o + bb_pos) : 0; }

  public static int createCurrentScopeFlat(FlatBufferBuilder builder,
      byte dummy) {
    builder.startObject(1);
    CurrentScopeFlat.addDummy(builder, dummy);
    return CurrentScopeFlat.endCurrentScopeFlat(builder);
  }

  public static void startCurrentScopeFlat(FlatBufferBuilder builder) { builder.startObject(1); }
  public static void addDummy(FlatBufferBuilder builder, byte dummy) { builder.addByte(0, dummy, 0); }
  public static int endCurrentScopeFlat(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
};

