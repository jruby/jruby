// automatically generated, do not modify

package org.jruby.ir.persistence.flat;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class FrozenStringFlat extends Table {
  public static FrozenStringFlat getRootAsFrozenStringFlat(ByteBuffer _bb) { return getRootAsFrozenStringFlat(_bb, new FrozenStringFlat()); }
  public static FrozenStringFlat getRootAsFrozenStringFlat(ByteBuffer _bb, FrozenStringFlat obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__init(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public FrozenStringFlat __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; return this; }

  public byte bytes(int j) { int o = __offset(4); return o != 0 ? bb.get(__vector(o) + j * 1) : 0; }
  public int bytesLength() { int o = __offset(4); return o != 0 ? __vector_len(o) : 0; }
  public ByteBuffer bytesAsByteBuffer() { return __vector_as_bytebuffer(4, 1); }
  public String encoding() { int o = __offset(6); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer encodingAsByteBuffer() { return __vector_as_bytebuffer(6, 1); }
  public String str() { int o = __offset(8); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer strAsByteBuffer() { return __vector_as_bytebuffer(8, 1); }
  public int coderange() { int o = __offset(10); return o != 0 ? bb.getInt(o + bb_pos) : 0; }
  public String file() { int o = __offset(12); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer fileAsByteBuffer() { return __vector_as_bytebuffer(12, 1); }
  public int line() { int o = __offset(14); return o != 0 ? bb.getInt(o + bb_pos) : 0; }

  public static int createFrozenStringFlat(FlatBufferBuilder builder,
      int bytesOffset,
      int encodingOffset,
      int strOffset,
      int coderange,
      int fileOffset,
      int line) {
    builder.startObject(6);
    FrozenStringFlat.addLine(builder, line);
    FrozenStringFlat.addFile(builder, fileOffset);
    FrozenStringFlat.addCoderange(builder, coderange);
    FrozenStringFlat.addStr(builder, strOffset);
    FrozenStringFlat.addEncoding(builder, encodingOffset);
    FrozenStringFlat.addBytes(builder, bytesOffset);
    return FrozenStringFlat.endFrozenStringFlat(builder);
  }

  public static void startFrozenStringFlat(FlatBufferBuilder builder) { builder.startObject(6); }
  public static void addBytes(FlatBufferBuilder builder, int bytesOffset) { builder.addOffset(0, bytesOffset, 0); }
  public static int createBytesVector(FlatBufferBuilder builder, byte[] data) { builder.startVector(1, data.length, 1); for (int i = data.length - 1; i >= 0; i--) builder.addByte(data[i]); return builder.endVector(); }
  public static void startBytesVector(FlatBufferBuilder builder, int numElems) { builder.startVector(1, numElems, 1); }
  public static void addEncoding(FlatBufferBuilder builder, int encodingOffset) { builder.addOffset(1, encodingOffset, 0); }
  public static void addStr(FlatBufferBuilder builder, int strOffset) { builder.addOffset(2, strOffset, 0); }
  public static void addCoderange(FlatBufferBuilder builder, int coderange) { builder.addInt(3, coderange, 0); }
  public static void addFile(FlatBufferBuilder builder, int fileOffset) { builder.addOffset(4, fileOffset, 0); }
  public static void addLine(FlatBufferBuilder builder, int line) { builder.addInt(5, line, 0); }
  public static int endFrozenStringFlat(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
};

