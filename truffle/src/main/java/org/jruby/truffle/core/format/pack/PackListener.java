// Generated from org/jruby/truffle/core/format/pack/Pack.g4 by ANTLR 4.5.1
package org.jruby.truffle.core.format.pack;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link PackParser}.
 */
public interface PackListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link PackParser#sequence}.
	 * @param ctx the parse tree
	 */
	void enterSequence(PackParser.SequenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link PackParser#sequence}.
	 * @param ctx the parse tree
	 */
	void exitSequence(PackParser.SequenceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code int8}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterInt8(PackParser.Int8Context ctx);
	/**
	 * Exit a parse tree produced by the {@code int8}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitInt8(PackParser.Int8Context ctx);
	/**
	 * Enter a parse tree produced by the {@code uint8}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterUint8(PackParser.Uint8Context ctx);
	/**
	 * Exit a parse tree produced by the {@code uint8}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitUint8(PackParser.Uint8Context ctx);
	/**
	 * Enter a parse tree produced by the {@code int16Little}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterInt16Little(PackParser.Int16LittleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code int16Little}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitInt16Little(PackParser.Int16LittleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code int16Big}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterInt16Big(PackParser.Int16BigContext ctx);
	/**
	 * Exit a parse tree produced by the {@code int16Big}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitInt16Big(PackParser.Int16BigContext ctx);
	/**
	 * Enter a parse tree produced by the {@code int16Native}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterInt16Native(PackParser.Int16NativeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code int16Native}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitInt16Native(PackParser.Int16NativeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code uint16Little}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterUint16Little(PackParser.Uint16LittleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code uint16Little}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitUint16Little(PackParser.Uint16LittleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code uint16Big}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterUint16Big(PackParser.Uint16BigContext ctx);
	/**
	 * Exit a parse tree produced by the {@code uint16Big}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitUint16Big(PackParser.Uint16BigContext ctx);
	/**
	 * Enter a parse tree produced by the {@code uint16Native}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterUint16Native(PackParser.Uint16NativeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code uint16Native}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitUint16Native(PackParser.Uint16NativeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code int32Little}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterInt32Little(PackParser.Int32LittleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code int32Little}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitInt32Little(PackParser.Int32LittleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code int32Big}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterInt32Big(PackParser.Int32BigContext ctx);
	/**
	 * Exit a parse tree produced by the {@code int32Big}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitInt32Big(PackParser.Int32BigContext ctx);
	/**
	 * Enter a parse tree produced by the {@code int32Native}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterInt32Native(PackParser.Int32NativeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code int32Native}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitInt32Native(PackParser.Int32NativeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code uint32Little}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterUint32Little(PackParser.Uint32LittleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code uint32Little}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitUint32Little(PackParser.Uint32LittleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code uint32Big}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterUint32Big(PackParser.Uint32BigContext ctx);
	/**
	 * Exit a parse tree produced by the {@code uint32Big}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitUint32Big(PackParser.Uint32BigContext ctx);
	/**
	 * Enter a parse tree produced by the {@code uint32Native}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterUint32Native(PackParser.Uint32NativeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code uint32Native}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitUint32Native(PackParser.Uint32NativeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code int64Little}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterInt64Little(PackParser.Int64LittleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code int64Little}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitInt64Little(PackParser.Int64LittleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code int64Big}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterInt64Big(PackParser.Int64BigContext ctx);
	/**
	 * Exit a parse tree produced by the {@code int64Big}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitInt64Big(PackParser.Int64BigContext ctx);
	/**
	 * Enter a parse tree produced by the {@code int64Native}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterInt64Native(PackParser.Int64NativeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code int64Native}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitInt64Native(PackParser.Int64NativeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code uint64Little}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterUint64Little(PackParser.Uint64LittleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code uint64Little}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitUint64Little(PackParser.Uint64LittleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code uint64Big}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterUint64Big(PackParser.Uint64BigContext ctx);
	/**
	 * Exit a parse tree produced by the {@code uint64Big}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitUint64Big(PackParser.Uint64BigContext ctx);
	/**
	 * Enter a parse tree produced by the {@code uint64Native}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterUint64Native(PackParser.Uint64NativeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code uint64Native}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitUint64Native(PackParser.Uint64NativeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code utf8Character}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterUtf8Character(PackParser.Utf8CharacterContext ctx);
	/**
	 * Exit a parse tree produced by the {@code utf8Character}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitUtf8Character(PackParser.Utf8CharacterContext ctx);
	/**
	 * Enter a parse tree produced by the {@code berInteger}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterBerInteger(PackParser.BerIntegerContext ctx);
	/**
	 * Exit a parse tree produced by the {@code berInteger}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitBerInteger(PackParser.BerIntegerContext ctx);
	/**
	 * Enter a parse tree produced by the {@code f64Native}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterF64Native(PackParser.F64NativeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code f64Native}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitF64Native(PackParser.F64NativeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code f32Native}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterF32Native(PackParser.F32NativeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code f32Native}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitF32Native(PackParser.F32NativeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code f64Little}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterF64Little(PackParser.F64LittleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code f64Little}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitF64Little(PackParser.F64LittleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code f32Little}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterF32Little(PackParser.F32LittleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code f32Little}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitF32Little(PackParser.F32LittleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code f64Big}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterF64Big(PackParser.F64BigContext ctx);
	/**
	 * Exit a parse tree produced by the {@code f64Big}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitF64Big(PackParser.F64BigContext ctx);
	/**
	 * Enter a parse tree produced by the {@code f32Big}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterF32Big(PackParser.F32BigContext ctx);
	/**
	 * Exit a parse tree produced by the {@code f32Big}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitF32Big(PackParser.F32BigContext ctx);
	/**
	 * Enter a parse tree produced by the {@code binaryStringSpacePadded}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterBinaryStringSpacePadded(PackParser.BinaryStringSpacePaddedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code binaryStringSpacePadded}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitBinaryStringSpacePadded(PackParser.BinaryStringSpacePaddedContext ctx);
	/**
	 * Enter a parse tree produced by the {@code binaryStringNullPadded}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterBinaryStringNullPadded(PackParser.BinaryStringNullPaddedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code binaryStringNullPadded}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitBinaryStringNullPadded(PackParser.BinaryStringNullPaddedContext ctx);
	/**
	 * Enter a parse tree produced by the {@code binaryStringNullStar}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterBinaryStringNullStar(PackParser.BinaryStringNullStarContext ctx);
	/**
	 * Exit a parse tree produced by the {@code binaryStringNullStar}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitBinaryStringNullStar(PackParser.BinaryStringNullStarContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bitStringMSBFirst}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterBitStringMSBFirst(PackParser.BitStringMSBFirstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bitStringMSBFirst}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitBitStringMSBFirst(PackParser.BitStringMSBFirstContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bitStringMSBLast}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterBitStringMSBLast(PackParser.BitStringMSBLastContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bitStringMSBLast}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitBitStringMSBLast(PackParser.BitStringMSBLastContext ctx);
	/**
	 * Enter a parse tree produced by the {@code hexStringHighFirst}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterHexStringHighFirst(PackParser.HexStringHighFirstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code hexStringHighFirst}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitHexStringHighFirst(PackParser.HexStringHighFirstContext ctx);
	/**
	 * Enter a parse tree produced by the {@code hexStringLowFirst}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterHexStringLowFirst(PackParser.HexStringLowFirstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code hexStringLowFirst}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitHexStringLowFirst(PackParser.HexStringLowFirstContext ctx);
	/**
	 * Enter a parse tree produced by the {@code uuString}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterUuString(PackParser.UuStringContext ctx);
	/**
	 * Exit a parse tree produced by the {@code uuString}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitUuString(PackParser.UuStringContext ctx);
	/**
	 * Enter a parse tree produced by the {@code mimeString}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterMimeString(PackParser.MimeStringContext ctx);
	/**
	 * Exit a parse tree produced by the {@code mimeString}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitMimeString(PackParser.MimeStringContext ctx);
	/**
	 * Enter a parse tree produced by the {@code base64String}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterBase64String(PackParser.Base64StringContext ctx);
	/**
	 * Exit a parse tree produced by the {@code base64String}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitBase64String(PackParser.Base64StringContext ctx);
	/**
	 * Enter a parse tree produced by the {@code pointer}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterPointer(PackParser.PointerContext ctx);
	/**
	 * Exit a parse tree produced by the {@code pointer}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitPointer(PackParser.PointerContext ctx);
	/**
	 * Enter a parse tree produced by the {@code at}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterAt(PackParser.AtContext ctx);
	/**
	 * Exit a parse tree produced by the {@code at}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitAt(PackParser.AtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code back}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterBack(PackParser.BackContext ctx);
	/**
	 * Exit a parse tree produced by the {@code back}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitBack(PackParser.BackContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nullByte}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterNullByte(PackParser.NullByteContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nullByte}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitNullByte(PackParser.NullByteContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subSequenceAlternate}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterSubSequenceAlternate(PackParser.SubSequenceAlternateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subSequenceAlternate}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitSubSequenceAlternate(PackParser.SubSequenceAlternateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code errorDisallowedNative}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void enterErrorDisallowedNative(PackParser.ErrorDisallowedNativeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code errorDisallowedNative}
	 * labeled alternative in {@link PackParser#directive}.
	 * @param ctx the parse tree
	 */
	void exitErrorDisallowedNative(PackParser.ErrorDisallowedNativeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PackParser#count}.
	 * @param ctx the parse tree
	 */
	void enterCount(PackParser.CountContext ctx);
	/**
	 * Exit a parse tree produced by {@link PackParser#count}.
	 * @param ctx the parse tree
	 */
	void exitCount(PackParser.CountContext ctx);
	/**
	 * Enter a parse tree produced by {@link PackParser#subSequence}.
	 * @param ctx the parse tree
	 */
	void enterSubSequence(PackParser.SubSequenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link PackParser#subSequence}.
	 * @param ctx the parse tree
	 */
	void exitSubSequence(PackParser.SubSequenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link PackParser#nativeOptLittle}.
	 * @param ctx the parse tree
	 */
	void enterNativeOptLittle(PackParser.NativeOptLittleContext ctx);
	/**
	 * Exit a parse tree produced by {@link PackParser#nativeOptLittle}.
	 * @param ctx the parse tree
	 */
	void exitNativeOptLittle(PackParser.NativeOptLittleContext ctx);
	/**
	 * Enter a parse tree produced by {@link PackParser#nativeOptBig}.
	 * @param ctx the parse tree
	 */
	void enterNativeOptBig(PackParser.NativeOptBigContext ctx);
	/**
	 * Exit a parse tree produced by {@link PackParser#nativeOptBig}.
	 * @param ctx the parse tree
	 */
	void exitNativeOptBig(PackParser.NativeOptBigContext ctx);
	/**
	 * Enter a parse tree produced by {@link PackParser#nativeLittle}.
	 * @param ctx the parse tree
	 */
	void enterNativeLittle(PackParser.NativeLittleContext ctx);
	/**
	 * Exit a parse tree produced by {@link PackParser#nativeLittle}.
	 * @param ctx the parse tree
	 */
	void exitNativeLittle(PackParser.NativeLittleContext ctx);
	/**
	 * Enter a parse tree produced by {@link PackParser#nativeBig}.
	 * @param ctx the parse tree
	 */
	void enterNativeBig(PackParser.NativeBigContext ctx);
	/**
	 * Exit a parse tree produced by {@link PackParser#nativeBig}.
	 * @param ctx the parse tree
	 */
	void exitNativeBig(PackParser.NativeBigContext ctx);
}