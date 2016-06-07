// automatically generated, do not modify

package org.jruby.ir.persistence.flat;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class IRScopeFlat extends Table {
  public static IRScopeFlat getRootAsIRScopeFlat(ByteBuffer _bb) { return getRootAsIRScopeFlat(_bb, new IRScopeFlat()); }
  public static IRScopeFlat getRootAsIRScopeFlat(ByteBuffer _bb, IRScopeFlat obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__init(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public IRScopeFlat __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; return this; }

  public String name() { int o = __offset(4); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer nameAsByteBuffer() { return __vector_as_bytebuffer(4, 1); }
  public IRClosureFlat nestedClosures(int j) { return nestedClosures(new IRClosureFlat(), j); }
  public IRClosureFlat nestedClosures(IRClosureFlat obj, int j) { int o = __offset(6); return o != 0 ? obj.__init(__indirect(__vector(o) + j * 4), bb) : null; }
  public int nestedClosuresLength() { int o = __offset(6); return o != 0 ? __vector_len(o) : 0; }
  public IRScopeFlat lexicalChildren(int j) { return lexicalChildren(new IRScopeFlat(), j); }
  public IRScopeFlat lexicalChildren(IRScopeFlat obj, int j) { int o = __offset(8); return o != 0 ? obj.__init(__indirect(__vector(o) + j * 4), bb) : null; }
  public int lexicalChildrenLength() { int o = __offset(8); return o != 0 ? __vector_len(o) : 0; }
  public InstrFlat instrs(int j) { return instrs(new InstrFlat(), j); }
  public InstrFlat instrs(InstrFlat obj, int j) { int o = __offset(10); return o != 0 ? obj.__init(__indirect(__vector(o) + j * 4), bb) : null; }
  public int instrsLength() { int o = __offset(10); return o != 0 ? __vector_len(o) : 0; }
  public short tempVariables() { int o = __offset(12); return o != 0 ? bb.getShort(o + bb_pos) : 0; }
  public boolean acceptsKeywordArguments() { int o = __offset(14); return o != 0 ? 0!=bb.get(o + bb_pos) : false; }

  public static int createIRScopeFlat(FlatBufferBuilder builder,
      int nameOffset,
      int nestedClosuresOffset,
      int lexicalChildrenOffset,
      int instrsOffset,
      short tempVariables,
      boolean acceptsKeywordArguments) {
    builder.startObject(6);
    IRScopeFlat.addInstrs(builder, instrsOffset);
    IRScopeFlat.addLexicalChildren(builder, lexicalChildrenOffset);
    IRScopeFlat.addNestedClosures(builder, nestedClosuresOffset);
    IRScopeFlat.addName(builder, nameOffset);
    IRScopeFlat.addTempVariables(builder, tempVariables);
    IRScopeFlat.addAcceptsKeywordArguments(builder, acceptsKeywordArguments);
    return IRScopeFlat.endIRScopeFlat(builder);
  }

  public static void startIRScopeFlat(FlatBufferBuilder builder) { builder.startObject(6); }
  public static void addName(FlatBufferBuilder builder, int nameOffset) { builder.addOffset(0, nameOffset, 0); }
  public static void addNestedClosures(FlatBufferBuilder builder, int nestedClosuresOffset) { builder.addOffset(1, nestedClosuresOffset, 0); }
  public static int createNestedClosuresVector(FlatBufferBuilder builder, int[] data) { builder.startVector(4, data.length, 4); for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]); return builder.endVector(); }
  public static void startNestedClosuresVector(FlatBufferBuilder builder, int numElems) { builder.startVector(4, numElems, 4); }
  public static void addLexicalChildren(FlatBufferBuilder builder, int lexicalChildrenOffset) { builder.addOffset(2, lexicalChildrenOffset, 0); }
  public static int createLexicalChildrenVector(FlatBufferBuilder builder, int[] data) { builder.startVector(4, data.length, 4); for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]); return builder.endVector(); }
  public static void startLexicalChildrenVector(FlatBufferBuilder builder, int numElems) { builder.startVector(4, numElems, 4); }
  public static void addInstrs(FlatBufferBuilder builder, int instrsOffset) { builder.addOffset(3, instrsOffset, 0); }
  public static int createInstrsVector(FlatBufferBuilder builder, int[] data) { builder.startVector(4, data.length, 4); for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]); return builder.endVector(); }
  public static void startInstrsVector(FlatBufferBuilder builder, int numElems) { builder.startVector(4, numElems, 4); }
  public static void addTempVariables(FlatBufferBuilder builder, short tempVariables) { builder.addShort(4, tempVariables, 0); }
  public static void addAcceptsKeywordArguments(FlatBufferBuilder builder, boolean acceptsKeywordArguments) { builder.addBoolean(5, acceptsKeywordArguments, false); }
  public static int endIRScopeFlat(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
  public static void finishIRScopeFlatBuffer(FlatBufferBuilder builder, int offset) { builder.finish(offset); }
};

