// automatically generated, do not modify

package org.jruby.ir.persistence.flat;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class IRClosureFlat extends Table {
  public static IRClosureFlat getRootAsIRClosureFlat(ByteBuffer _bb) { return getRootAsIRClosureFlat(_bb, new IRClosureFlat()); }
  public static IRClosureFlat getRootAsIRClosureFlat(ByteBuffer _bb, IRClosureFlat obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__init(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public IRClosureFlat __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; return this; }

  public IRScopeFlat scope() { return scope(new IRScopeFlat()); }
  public IRScopeFlat scope(IRScopeFlat obj) { int o = __offset(4); return o != 0 ? obj.__init(__indirect(o + bb_pos), bb) : null; }

  public static int createIRClosureFlat(FlatBufferBuilder builder,
      int scopeOffset) {
    builder.startObject(1);
    IRClosureFlat.addScope(builder, scopeOffset);
    return IRClosureFlat.endIRClosureFlat(builder);
  }

  public static void startIRClosureFlat(FlatBufferBuilder builder) { builder.startObject(1); }
  public static void addScope(FlatBufferBuilder builder, int scopeOffset) { builder.addOffset(0, scopeOffset, 0); }
  public static int endIRClosureFlat(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
};

