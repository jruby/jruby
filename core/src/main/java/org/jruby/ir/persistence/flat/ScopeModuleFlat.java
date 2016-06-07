// automatically generated, do not modify

package org.jruby.ir.persistence.flat;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class ScopeModuleFlat extends Table {
  public static ScopeModuleFlat getRootAsScopeModuleFlat(ByteBuffer _bb) { return getRootAsScopeModuleFlat(_bb, new ScopeModuleFlat()); }
  public static ScopeModuleFlat getRootAsScopeModuleFlat(ByteBuffer _bb, ScopeModuleFlat obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__init(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public ScopeModuleFlat __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; return this; }

  public byte dummy() { int o = __offset(4); return o != 0 ? bb.get(o + bb_pos) : 0; }

  public static int createScopeModuleFlat(FlatBufferBuilder builder,
      byte dummy) {
    builder.startObject(1);
    ScopeModuleFlat.addDummy(builder, dummy);
    return ScopeModuleFlat.endScopeModuleFlat(builder);
  }

  public static void startScopeModuleFlat(FlatBufferBuilder builder) { builder.startObject(1); }
  public static void addDummy(FlatBufferBuilder builder, byte dummy) { builder.addByte(0, dummy, 0); }
  public static int endScopeModuleFlat(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
};

