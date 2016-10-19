/*

grammar Pack;

sequence : directive* ;

directive : 'c' count?                                          # int8
          | 'C' count?                                          # uint8
          | 's' nativeOptLittle count?                          # int16Little
          | 's' nativeOptBig count?                             # int16Big
          | 's' NATIVE? count?                                  # int16Native
          | ('S' nativeOptLittle | 'v') count?                  # uint16Little
          | ('S' nativeOptBig | 'n') count?                     # uint16Big
          | 'S' NATIVE? count?                                  # uint16Native
          | ('i' nativeOptLittle | 'l' LITTLE) count?           # int32Little
          | ('i' nativeOptBig | 'l' BIG) count?                 # int32Big
          | ('i' NATIVE? | 'l') count?                          # int32Native
          | (('I' nativeOptLittle | 'L' LITTLE) | 'V') count?   # uint32Little
          | (('I' nativeOptBig | 'L' BIG) | 'N') count?         # uint32Big
          | ('I' NATIVE? | 'L') count?                          # uint32Native
          | ('q' nativeOptLittle | 'l' nativeLittle) count?     # int64Little
          | ('q' nativeOptBig | 'l' nativeBig) count?           # int64Big
          | ('q' NATIVE? | 'l' NATIVE) count?                   # int64Native
          | ('Q' nativeOptLittle | 'L' nativeLittle) count?     # uint64Little
          | ('Q' nativeOptBig | 'L' nativeBig) count?           # uint64Big
          | ('Q' NATIVE? | 'L' NATIVE) count?                   # uint64Native
          | 'U' count?                                          # utf8Character
          | 'w' count?                                          # berInteger
          | ('d' | 'D') count?                                  # f64Native
          | ('f' | 'F') count?                                  # f32Native
          | 'E' count?                                          # f64Little
          | 'e' count?                                          # f32Little
          | 'G' count?                                          # f64Big
          | 'g' count?                                          # f32Big
          | 'A' count?                                          # binaryStringSpacePadded
          | 'a' count?                                          # binaryStringNullPadded
          | 'Z' count?                                          # binaryStringNullStar
          | 'B' count?                                          # bitStringMSBFirst
          | 'b' count?                                          # bitStringMSBLast
          | 'H' count?                                          # hexStringHighFirst
          | 'h' count?                                          # hexStringLowFirst
          | 'u' count?                                          # uuString
          | 'M' count?                                          # mimeString
          | 'm' count?                                          # base64String
          | ('p' | 'P')                                         # pointer
          | '@' count?                                          # at
          | 'X' count?                                          # back
          | 'x' count?                                          # nullByte
          | subSequence                                         # subSequenceAlternate
          | ('v' | 'n' | 'V' | 'N' | 'U' | 'w' | 'd' | 'D' |
             'f' | 'F' | 'E' | 'e' | 'g' | 'G' | 'A' | 'a' |
             'Z' | 'B' | 'b' | 'H' | 'h' | 'u' | 'M' |
             'm' | 'p' | 'P' | 'X' | 'x') NATIVE                #errorDisallowedNative ;

count           : INT | '*' ;

subSequence     : '(' directive+ ')' INT? ;

nativeOptLittle : NATIVE* LITTLE NATIVE* ;
nativeOptBig    : NATIVE* BIG NATIVE* ;

nativeLittle    : NATIVE+ LITTLE NATIVE* | NATIVE* LITTLE NATIVE+ ;
nativeBig       : NATIVE+ BIG NATIVE* | NATIVE* BIG NATIVE+ ;

LITTLE          : '<' ;
BIG             : '>' ;
NATIVE          : [!_] ;

INT             : [0-9]+ ;

WS              : [ \t\n\u000b\f\r\u0000]+ -> skip ;
COMMENT         : '#' .*? (('\r'? '\n') | EOF) -> skip ;

 */

// Generated from org/jruby/truffle/core/format/pack/Pack.g4 by ANTLR 4.5.1
package org.jruby.truffle.core.format.pack;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class PackParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.5.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, T__18=19, T__19=20, T__20=21, T__21=22, T__22=23, T__23=24, 
		T__24=25, T__25=26, T__26=27, T__27=28, T__28=29, T__29=30, T__30=31, 
		T__31=32, T__32=33, T__33=34, T__34=35, T__35=36, T__36=37, T__37=38, 
		T__38=39, T__39=40, T__40=41, T__41=42, LITTLE=43, BIG=44, NATIVE=45, 
		INT=46, WS=47, COMMENT=48;
	public static final int
		RULE_sequence = 0, RULE_directive = 1, RULE_count = 2, RULE_subSequence = 3, 
		RULE_nativeOptLittle = 4, RULE_nativeOptBig = 5, RULE_nativeLittle = 6, 
		RULE_nativeBig = 7;
	public static final String[] ruleNames = {
		"sequence", "directive", "count", "subSequence", "nativeOptLittle", "nativeOptBig", 
		"nativeLittle", "nativeBig"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'c'", "'C'", "'s'", "'S'", "'v'", "'n'", "'i'", "'l'", "'I'", "'L'", 
		"'V'", "'N'", "'q'", "'Q'", "'U'", "'w'", "'d'", "'D'", "'f'", "'F'", 
		"'E'", "'e'", "'G'", "'g'", "'A'", "'a'", "'Z'", "'B'", "'b'", "'H'", 
		"'h'", "'u'", "'M'", "'m'", "'p'", "'P'", "'@'", "'X'", "'x'", "'*'", 
		"'('", "')'", "'<'", "'>'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, "LITTLE", "BIG", "NATIVE", "INT", 
		"WS", "COMMENT"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "Pack.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public PackParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class SequenceContext extends ParserRuleContext {
		public List<DirectiveContext> directive() {
			return getRuleContexts(DirectiveContext.class);
		}
		public DirectiveContext directive(int i) {
			return getRuleContext(DirectiveContext.class,i);
		}
		public SequenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sequence; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterSequence(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitSequence(this);
		}
	}

	public final SequenceContext sequence() throws RecognitionException {
		SequenceContext _localctx = new SequenceContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_sequence);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(19);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31) | (1L << T__32) | (1L << T__33) | (1L << T__34) | (1L << T__35) | (1L << T__36) | (1L << T__37) | (1L << T__38) | (1L << T__40))) != 0)) {
				{
				{
				setState(16);
				directive();
				}
				}
				setState(21);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DirectiveContext extends ParserRuleContext {
		public DirectiveContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_directive; }
	 
		public DirectiveContext() { }
		public void copyFrom(DirectiveContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class F32NativeContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public F32NativeContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterF32Native(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitF32Native(this);
		}
	}
	public static class Int32LittleContext extends DirectiveContext {
		public NativeOptLittleContext nativeOptLittle() {
			return getRuleContext(NativeOptLittleContext.class,0);
		}
		public TerminalNode LITTLE() { return getToken(PackParser.LITTLE, 0); }
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public Int32LittleContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterInt32Little(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitInt32Little(this);
		}
	}
	public static class Utf8CharacterContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public Utf8CharacterContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterUtf8Character(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitUtf8Character(this);
		}
	}
	public static class BinaryStringNullStarContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public BinaryStringNullStarContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterBinaryStringNullStar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitBinaryStringNullStar(this);
		}
	}
	public static class BackContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public BackContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterBack(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitBack(this);
		}
	}
	public static class Int16BigContext extends DirectiveContext {
		public NativeOptBigContext nativeOptBig() {
			return getRuleContext(NativeOptBigContext.class,0);
		}
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public Int16BigContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterInt16Big(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitInt16Big(this);
		}
	}
	public static class Uint16BigContext extends DirectiveContext {
		public NativeOptBigContext nativeOptBig() {
			return getRuleContext(NativeOptBigContext.class,0);
		}
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public Uint16BigContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterUint16Big(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitUint16Big(this);
		}
	}
	public static class F64LittleContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public F64LittleContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterF64Little(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitF64Little(this);
		}
	}
	public static class UuStringContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public UuStringContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterUuString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitUuString(this);
		}
	}
	public static class F32BigContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public F32BigContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterF32Big(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitF32Big(this);
		}
	}
	public static class ErrorDisallowedNativeContext extends DirectiveContext {
		public TerminalNode NATIVE() { return getToken(PackParser.NATIVE, 0); }
		public ErrorDisallowedNativeContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterErrorDisallowedNative(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitErrorDisallowedNative(this);
		}
	}
	public static class HexStringHighFirstContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public HexStringHighFirstContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterHexStringHighFirst(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitHexStringHighFirst(this);
		}
	}
	public static class HexStringLowFirstContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public HexStringLowFirstContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterHexStringLowFirst(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitHexStringLowFirst(this);
		}
	}
	public static class Int64LittleContext extends DirectiveContext {
		public NativeOptLittleContext nativeOptLittle() {
			return getRuleContext(NativeOptLittleContext.class,0);
		}
		public NativeLittleContext nativeLittle() {
			return getRuleContext(NativeLittleContext.class,0);
		}
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public Int64LittleContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterInt64Little(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitInt64Little(this);
		}
	}
	public static class Uint16NativeContext extends DirectiveContext {
		public TerminalNode NATIVE() { return getToken(PackParser.NATIVE, 0); }
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public Uint16NativeContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterUint16Native(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitUint16Native(this);
		}
	}
	public static class Int8Context extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public Int8Context(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterInt8(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitInt8(this);
		}
	}
	public static class BinaryStringSpacePaddedContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public BinaryStringSpacePaddedContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterBinaryStringSpacePadded(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitBinaryStringSpacePadded(this);
		}
	}
	public static class Uint64BigContext extends DirectiveContext {
		public NativeOptBigContext nativeOptBig() {
			return getRuleContext(NativeOptBigContext.class,0);
		}
		public NativeBigContext nativeBig() {
			return getRuleContext(NativeBigContext.class,0);
		}
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public Uint64BigContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterUint64Big(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitUint64Big(this);
		}
	}
	public static class Int16LittleContext extends DirectiveContext {
		public NativeOptLittleContext nativeOptLittle() {
			return getRuleContext(NativeOptLittleContext.class,0);
		}
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public Int16LittleContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterInt16Little(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitInt16Little(this);
		}
	}
	public static class Int32NativeContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public TerminalNode NATIVE() { return getToken(PackParser.NATIVE, 0); }
		public Int32NativeContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterInt32Native(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitInt32Native(this);
		}
	}
	public static class F32LittleContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public F32LittleContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterF32Little(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitF32Little(this);
		}
	}
	public static class Uint32LittleContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public NativeOptLittleContext nativeOptLittle() {
			return getRuleContext(NativeOptLittleContext.class,0);
		}
		public TerminalNode LITTLE() { return getToken(PackParser.LITTLE, 0); }
		public Uint32LittleContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterUint32Little(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitUint32Little(this);
		}
	}
	public static class Int64BigContext extends DirectiveContext {
		public NativeOptBigContext nativeOptBig() {
			return getRuleContext(NativeOptBigContext.class,0);
		}
		public NativeBigContext nativeBig() {
			return getRuleContext(NativeBigContext.class,0);
		}
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public Int64BigContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterInt64Big(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitInt64Big(this);
		}
	}
	public static class PointerContext extends DirectiveContext {
		public PointerContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterPointer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitPointer(this);
		}
	}
	public static class F64BigContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public F64BigContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterF64Big(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitF64Big(this);
		}
	}
	public static class BinaryStringNullPaddedContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public BinaryStringNullPaddedContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterBinaryStringNullPadded(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitBinaryStringNullPadded(this);
		}
	}
	public static class MimeStringContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public MimeStringContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterMimeString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitMimeString(this);
		}
	}
	public static class Int16NativeContext extends DirectiveContext {
		public TerminalNode NATIVE() { return getToken(PackParser.NATIVE, 0); }
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public Int16NativeContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterInt16Native(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitInt16Native(this);
		}
	}
	public static class Base64StringContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public Base64StringContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterBase64String(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitBase64String(this);
		}
	}
	public static class BitStringMSBFirstContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public BitStringMSBFirstContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterBitStringMSBFirst(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitBitStringMSBFirst(this);
		}
	}
	public static class Int64NativeContext extends DirectiveContext {
		public TerminalNode NATIVE() { return getToken(PackParser.NATIVE, 0); }
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public Int64NativeContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterInt64Native(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitInt64Native(this);
		}
	}
	public static class Uint64LittleContext extends DirectiveContext {
		public NativeOptLittleContext nativeOptLittle() {
			return getRuleContext(NativeOptLittleContext.class,0);
		}
		public NativeLittleContext nativeLittle() {
			return getRuleContext(NativeLittleContext.class,0);
		}
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public Uint64LittleContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterUint64Little(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitUint64Little(this);
		}
	}
	public static class SubSequenceAlternateContext extends DirectiveContext {
		public SubSequenceContext subSequence() {
			return getRuleContext(SubSequenceContext.class,0);
		}
		public SubSequenceAlternateContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterSubSequenceAlternate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitSubSequenceAlternate(this);
		}
	}
	public static class Uint32BigContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public NativeOptBigContext nativeOptBig() {
			return getRuleContext(NativeOptBigContext.class,0);
		}
		public TerminalNode BIG() { return getToken(PackParser.BIG, 0); }
		public Uint32BigContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterUint32Big(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitUint32Big(this);
		}
	}
	public static class Uint16LittleContext extends DirectiveContext {
		public NativeOptLittleContext nativeOptLittle() {
			return getRuleContext(NativeOptLittleContext.class,0);
		}
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public Uint16LittleContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterUint16Little(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitUint16Little(this);
		}
	}
	public static class Int32BigContext extends DirectiveContext {
		public NativeOptBigContext nativeOptBig() {
			return getRuleContext(NativeOptBigContext.class,0);
		}
		public TerminalNode BIG() { return getToken(PackParser.BIG, 0); }
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public Int32BigContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterInt32Big(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitInt32Big(this);
		}
	}
	public static class AtContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public AtContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterAt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitAt(this);
		}
	}
	public static class F64NativeContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public F64NativeContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterF64Native(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitF64Native(this);
		}
	}
	public static class Uint64NativeContext extends DirectiveContext {
		public TerminalNode NATIVE() { return getToken(PackParser.NATIVE, 0); }
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public Uint64NativeContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterUint64Native(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitUint64Native(this);
		}
	}
	public static class NullByteContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public NullByteContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterNullByte(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitNullByte(this);
		}
	}
	public static class Uint32NativeContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public TerminalNode NATIVE() { return getToken(PackParser.NATIVE, 0); }
		public Uint32NativeContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterUint32Native(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitUint32Native(this);
		}
	}
	public static class BitStringMSBLastContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public BitStringMSBLastContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterBitStringMSBLast(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitBitStringMSBLast(this);
		}
	}
	public static class Uint8Context extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public Uint8Context(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterUint8(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitUint8(this);
		}
	}
	public static class BerIntegerContext extends DirectiveContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public BerIntegerContext(DirectiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterBerInteger(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitBerInteger(this);
		}
	}

	public final DirectiveContext directive() throws RecognitionException {
		DirectiveContext _localctx = new DirectiveContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_directive);
		int _la;
		try {
			setState(278);
			switch ( getInterpreter().adaptivePredict(_input,64,_ctx) ) {
			case 1:
				_localctx = new Int8Context(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(22);
				match(T__0);
				setState(24);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(23);
					count();
					}
				}

				}
				break;
			case 2:
				_localctx = new Uint8Context(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(26);
				match(T__1);
				setState(28);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(27);
					count();
					}
				}

				}
				break;
			case 3:
				_localctx = new Int16LittleContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(30);
				match(T__2);
				setState(31);
				nativeOptLittle();
				setState(33);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(32);
					count();
					}
				}

				}
				break;
			case 4:
				_localctx = new Int16BigContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(35);
				match(T__2);
				setState(36);
				nativeOptBig();
				setState(38);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(37);
					count();
					}
				}

				}
				break;
			case 5:
				_localctx = new Int16NativeContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(40);
				match(T__2);
				setState(42);
				_la = _input.LA(1);
				if (_la==NATIVE) {
					{
					setState(41);
					match(NATIVE);
					}
				}

				setState(45);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(44);
					count();
					}
				}

				}
				break;
			case 6:
				_localctx = new Uint16LittleContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(50);
				switch (_input.LA(1)) {
				case T__3:
					{
					setState(47);
					match(T__3);
					setState(48);
					nativeOptLittle();
					}
					break;
				case T__4:
					{
					setState(49);
					match(T__4);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(53);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(52);
					count();
					}
				}

				}
				break;
			case 7:
				_localctx = new Uint16BigContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(58);
				switch (_input.LA(1)) {
				case T__3:
					{
					setState(55);
					match(T__3);
					setState(56);
					nativeOptBig();
					}
					break;
				case T__5:
					{
					setState(57);
					match(T__5);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(61);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(60);
					count();
					}
				}

				}
				break;
			case 8:
				_localctx = new Uint16NativeContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(63);
				match(T__3);
				setState(65);
				_la = _input.LA(1);
				if (_la==NATIVE) {
					{
					setState(64);
					match(NATIVE);
					}
				}

				setState(68);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(67);
					count();
					}
				}

				}
				break;
			case 9:
				_localctx = new Int32LittleContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(74);
				switch (_input.LA(1)) {
				case T__6:
					{
					setState(70);
					match(T__6);
					setState(71);
					nativeOptLittle();
					}
					break;
				case T__7:
					{
					setState(72);
					match(T__7);
					setState(73);
					match(LITTLE);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(77);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(76);
					count();
					}
				}

				}
				break;
			case 10:
				_localctx = new Int32BigContext(_localctx);
				enterOuterAlt(_localctx, 10);
				{
				setState(83);
				switch (_input.LA(1)) {
				case T__6:
					{
					setState(79);
					match(T__6);
					setState(80);
					nativeOptBig();
					}
					break;
				case T__7:
					{
					setState(81);
					match(T__7);
					setState(82);
					match(BIG);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(86);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(85);
					count();
					}
				}

				}
				break;
			case 11:
				_localctx = new Int32NativeContext(_localctx);
				enterOuterAlt(_localctx, 11);
				{
				setState(93);
				switch (_input.LA(1)) {
				case T__6:
					{
					setState(88);
					match(T__6);
					setState(90);
					_la = _input.LA(1);
					if (_la==NATIVE) {
						{
						setState(89);
						match(NATIVE);
						}
					}

					}
					break;
				case T__7:
					{
					setState(92);
					match(T__7);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(96);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(95);
					count();
					}
				}

				}
				break;
			case 12:
				_localctx = new Uint32LittleContext(_localctx);
				enterOuterAlt(_localctx, 12);
				{
				setState(105);
				switch (_input.LA(1)) {
				case T__8:
				case T__9:
					{
					setState(102);
					switch (_input.LA(1)) {
					case T__8:
						{
						setState(98);
						match(T__8);
						setState(99);
						nativeOptLittle();
						}
						break;
					case T__9:
						{
						setState(100);
						match(T__9);
						setState(101);
						match(LITTLE);
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					}
					break;
				case T__10:
					{
					setState(104);
					match(T__10);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(108);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(107);
					count();
					}
				}

				}
				break;
			case 13:
				_localctx = new Uint32BigContext(_localctx);
				enterOuterAlt(_localctx, 13);
				{
				setState(117);
				switch (_input.LA(1)) {
				case T__8:
				case T__9:
					{
					setState(114);
					switch (_input.LA(1)) {
					case T__8:
						{
						setState(110);
						match(T__8);
						setState(111);
						nativeOptBig();
						}
						break;
					case T__9:
						{
						setState(112);
						match(T__9);
						setState(113);
						match(BIG);
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					}
					break;
				case T__11:
					{
					setState(116);
					match(T__11);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(120);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(119);
					count();
					}
				}

				}
				break;
			case 14:
				_localctx = new Uint32NativeContext(_localctx);
				enterOuterAlt(_localctx, 14);
				{
				setState(127);
				switch (_input.LA(1)) {
				case T__8:
					{
					setState(122);
					match(T__8);
					setState(124);
					_la = _input.LA(1);
					if (_la==NATIVE) {
						{
						setState(123);
						match(NATIVE);
						}
					}

					}
					break;
				case T__9:
					{
					setState(126);
					match(T__9);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(130);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(129);
					count();
					}
				}

				}
				break;
			case 15:
				_localctx = new Int64LittleContext(_localctx);
				enterOuterAlt(_localctx, 15);
				{
				setState(136);
				switch (_input.LA(1)) {
				case T__12:
					{
					setState(132);
					match(T__12);
					setState(133);
					nativeOptLittle();
					}
					break;
				case T__7:
					{
					setState(134);
					match(T__7);
					setState(135);
					nativeLittle();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(139);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(138);
					count();
					}
				}

				}
				break;
			case 16:
				_localctx = new Int64BigContext(_localctx);
				enterOuterAlt(_localctx, 16);
				{
				setState(145);
				switch (_input.LA(1)) {
				case T__12:
					{
					setState(141);
					match(T__12);
					setState(142);
					nativeOptBig();
					}
					break;
				case T__7:
					{
					setState(143);
					match(T__7);
					setState(144);
					nativeBig();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(148);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(147);
					count();
					}
				}

				}
				break;
			case 17:
				_localctx = new Int64NativeContext(_localctx);
				enterOuterAlt(_localctx, 17);
				{
				setState(156);
				switch (_input.LA(1)) {
				case T__12:
					{
					setState(150);
					match(T__12);
					setState(152);
					_la = _input.LA(1);
					if (_la==NATIVE) {
						{
						setState(151);
						match(NATIVE);
						}
					}

					}
					break;
				case T__7:
					{
					setState(154);
					match(T__7);
					setState(155);
					match(NATIVE);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(159);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(158);
					count();
					}
				}

				}
				break;
			case 18:
				_localctx = new Uint64LittleContext(_localctx);
				enterOuterAlt(_localctx, 18);
				{
				setState(165);
				switch (_input.LA(1)) {
				case T__13:
					{
					setState(161);
					match(T__13);
					setState(162);
					nativeOptLittle();
					}
					break;
				case T__9:
					{
					setState(163);
					match(T__9);
					setState(164);
					nativeLittle();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(168);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(167);
					count();
					}
				}

				}
				break;
			case 19:
				_localctx = new Uint64BigContext(_localctx);
				enterOuterAlt(_localctx, 19);
				{
				setState(174);
				switch (_input.LA(1)) {
				case T__13:
					{
					setState(170);
					match(T__13);
					setState(171);
					nativeOptBig();
					}
					break;
				case T__9:
					{
					setState(172);
					match(T__9);
					setState(173);
					nativeBig();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(177);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(176);
					count();
					}
				}

				}
				break;
			case 20:
				_localctx = new Uint64NativeContext(_localctx);
				enterOuterAlt(_localctx, 20);
				{
				setState(185);
				switch (_input.LA(1)) {
				case T__13:
					{
					setState(179);
					match(T__13);
					setState(181);
					_la = _input.LA(1);
					if (_la==NATIVE) {
						{
						setState(180);
						match(NATIVE);
						}
					}

					}
					break;
				case T__9:
					{
					setState(183);
					match(T__9);
					setState(184);
					match(NATIVE);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(188);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(187);
					count();
					}
				}

				}
				break;
			case 21:
				_localctx = new Utf8CharacterContext(_localctx);
				enterOuterAlt(_localctx, 21);
				{
				setState(190);
				match(T__14);
				setState(192);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(191);
					count();
					}
				}

				}
				break;
			case 22:
				_localctx = new BerIntegerContext(_localctx);
				enterOuterAlt(_localctx, 22);
				{
				setState(194);
				match(T__15);
				setState(196);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(195);
					count();
					}
				}

				}
				break;
			case 23:
				_localctx = new F64NativeContext(_localctx);
				enterOuterAlt(_localctx, 23);
				{
				setState(198);
				_la = _input.LA(1);
				if ( !(_la==T__16 || _la==T__17) ) {
				_errHandler.recoverInline(this);
				} else {
					consume();
				}
				setState(200);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(199);
					count();
					}
				}

				}
				break;
			case 24:
				_localctx = new F32NativeContext(_localctx);
				enterOuterAlt(_localctx, 24);
				{
				setState(202);
				_la = _input.LA(1);
				if ( !(_la==T__18 || _la==T__19) ) {
				_errHandler.recoverInline(this);
				} else {
					consume();
				}
				setState(204);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(203);
					count();
					}
				}

				}
				break;
			case 25:
				_localctx = new F64LittleContext(_localctx);
				enterOuterAlt(_localctx, 25);
				{
				setState(206);
				match(T__20);
				setState(208);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(207);
					count();
					}
				}

				}
				break;
			case 26:
				_localctx = new F32LittleContext(_localctx);
				enterOuterAlt(_localctx, 26);
				{
				setState(210);
				match(T__21);
				setState(212);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(211);
					count();
					}
				}

				}
				break;
			case 27:
				_localctx = new F64BigContext(_localctx);
				enterOuterAlt(_localctx, 27);
				{
				setState(214);
				match(T__22);
				setState(216);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(215);
					count();
					}
				}

				}
				break;
			case 28:
				_localctx = new F32BigContext(_localctx);
				enterOuterAlt(_localctx, 28);
				{
				setState(218);
				match(T__23);
				setState(220);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(219);
					count();
					}
				}

				}
				break;
			case 29:
				_localctx = new BinaryStringSpacePaddedContext(_localctx);
				enterOuterAlt(_localctx, 29);
				{
				setState(222);
				match(T__24);
				setState(224);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(223);
					count();
					}
				}

				}
				break;
			case 30:
				_localctx = new BinaryStringNullPaddedContext(_localctx);
				enterOuterAlt(_localctx, 30);
				{
				setState(226);
				match(T__25);
				setState(228);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(227);
					count();
					}
				}

				}
				break;
			case 31:
				_localctx = new BinaryStringNullStarContext(_localctx);
				enterOuterAlt(_localctx, 31);
				{
				setState(230);
				match(T__26);
				setState(232);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(231);
					count();
					}
				}

				}
				break;
			case 32:
				_localctx = new BitStringMSBFirstContext(_localctx);
				enterOuterAlt(_localctx, 32);
				{
				setState(234);
				match(T__27);
				setState(236);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(235);
					count();
					}
				}

				}
				break;
			case 33:
				_localctx = new BitStringMSBLastContext(_localctx);
				enterOuterAlt(_localctx, 33);
				{
				setState(238);
				match(T__28);
				setState(240);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(239);
					count();
					}
				}

				}
				break;
			case 34:
				_localctx = new HexStringHighFirstContext(_localctx);
				enterOuterAlt(_localctx, 34);
				{
				setState(242);
				match(T__29);
				setState(244);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(243);
					count();
					}
				}

				}
				break;
			case 35:
				_localctx = new HexStringLowFirstContext(_localctx);
				enterOuterAlt(_localctx, 35);
				{
				setState(246);
				match(T__30);
				setState(248);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(247);
					count();
					}
				}

				}
				break;
			case 36:
				_localctx = new UuStringContext(_localctx);
				enterOuterAlt(_localctx, 36);
				{
				setState(250);
				match(T__31);
				setState(252);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(251);
					count();
					}
				}

				}
				break;
			case 37:
				_localctx = new MimeStringContext(_localctx);
				enterOuterAlt(_localctx, 37);
				{
				setState(254);
				match(T__32);
				setState(256);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(255);
					count();
					}
				}

				}
				break;
			case 38:
				_localctx = new Base64StringContext(_localctx);
				enterOuterAlt(_localctx, 38);
				{
				setState(258);
				match(T__33);
				setState(260);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(259);
					count();
					}
				}

				}
				break;
			case 39:
				_localctx = new PointerContext(_localctx);
				enterOuterAlt(_localctx, 39);
				{
				setState(262);
				_la = _input.LA(1);
				if ( !(_la==T__34 || _la==T__35) ) {
				_errHandler.recoverInline(this);
				} else {
					consume();
				}
				}
				break;
			case 40:
				_localctx = new AtContext(_localctx);
				enterOuterAlt(_localctx, 40);
				{
				setState(263);
				match(T__36);
				setState(265);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(264);
					count();
					}
				}

				}
				break;
			case 41:
				_localctx = new BackContext(_localctx);
				enterOuterAlt(_localctx, 41);
				{
				setState(267);
				match(T__37);
				setState(269);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(268);
					count();
					}
				}

				}
				break;
			case 42:
				_localctx = new NullByteContext(_localctx);
				enterOuterAlt(_localctx, 42);
				{
				setState(271);
				match(T__38);
				setState(273);
				_la = _input.LA(1);
				if (_la==T__39 || _la==INT) {
					{
					setState(272);
					count();
					}
				}

				}
				break;
			case 43:
				_localctx = new SubSequenceAlternateContext(_localctx);
				enterOuterAlt(_localctx, 43);
				{
				setState(275);
				subSequence();
				}
				break;
			case 44:
				_localctx = new ErrorDisallowedNativeContext(_localctx);
				enterOuterAlt(_localctx, 44);
				{
				setState(276);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__4) | (1L << T__5) | (1L << T__10) | (1L << T__11) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31) | (1L << T__32) | (1L << T__33) | (1L << T__34) | (1L << T__35) | (1L << T__37) | (1L << T__38))) != 0)) ) {
				_errHandler.recoverInline(this);
				} else {
					consume();
				}
				setState(277);
				match(NATIVE);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CountContext extends ParserRuleContext {
		public TerminalNode INT() { return getToken(PackParser.INT, 0); }
		public CountContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_count; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterCount(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitCount(this);
		}
	}

	public final CountContext count() throws RecognitionException {
		CountContext _localctx = new CountContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_count);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(280);
			_la = _input.LA(1);
			if ( !(_la==T__39 || _la==INT) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SubSequenceContext extends ParserRuleContext {
		public List<DirectiveContext> directive() {
			return getRuleContexts(DirectiveContext.class);
		}
		public DirectiveContext directive(int i) {
			return getRuleContext(DirectiveContext.class,i);
		}
		public TerminalNode INT() { return getToken(PackParser.INT, 0); }
		public SubSequenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_subSequence; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterSubSequence(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitSubSequence(this);
		}
	}

	public final SubSequenceContext subSequence() throws RecognitionException {
		SubSequenceContext _localctx = new SubSequenceContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_subSequence);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(282);
			match(T__40);
			setState(284); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(283);
				directive();
				}
				}
				setState(286); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31) | (1L << T__32) | (1L << T__33) | (1L << T__34) | (1L << T__35) | (1L << T__36) | (1L << T__37) | (1L << T__38) | (1L << T__40))) != 0) );
			setState(288);
			match(T__41);
			setState(290);
			_la = _input.LA(1);
			if (_la==INT) {
				{
				setState(289);
				match(INT);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NativeOptLittleContext extends ParserRuleContext {
		public TerminalNode LITTLE() { return getToken(PackParser.LITTLE, 0); }
		public List<TerminalNode> NATIVE() { return getTokens(PackParser.NATIVE); }
		public TerminalNode NATIVE(int i) {
			return getToken(PackParser.NATIVE, i);
		}
		public NativeOptLittleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nativeOptLittle; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterNativeOptLittle(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitNativeOptLittle(this);
		}
	}

	public final NativeOptLittleContext nativeOptLittle() throws RecognitionException {
		NativeOptLittleContext _localctx = new NativeOptLittleContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_nativeOptLittle);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(295);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NATIVE) {
				{
				{
				setState(292);
				match(NATIVE);
				}
				}
				setState(297);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(298);
			match(LITTLE);
			setState(302);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NATIVE) {
				{
				{
				setState(299);
				match(NATIVE);
				}
				}
				setState(304);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NativeOptBigContext extends ParserRuleContext {
		public TerminalNode BIG() { return getToken(PackParser.BIG, 0); }
		public List<TerminalNode> NATIVE() { return getTokens(PackParser.NATIVE); }
		public TerminalNode NATIVE(int i) {
			return getToken(PackParser.NATIVE, i);
		}
		public NativeOptBigContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nativeOptBig; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterNativeOptBig(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitNativeOptBig(this);
		}
	}

	public final NativeOptBigContext nativeOptBig() throws RecognitionException {
		NativeOptBigContext _localctx = new NativeOptBigContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_nativeOptBig);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(308);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NATIVE) {
				{
				{
				setState(305);
				match(NATIVE);
				}
				}
				setState(310);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(311);
			match(BIG);
			setState(315);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NATIVE) {
				{
				{
				setState(312);
				match(NATIVE);
				}
				}
				setState(317);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NativeLittleContext extends ParserRuleContext {
		public TerminalNode LITTLE() { return getToken(PackParser.LITTLE, 0); }
		public List<TerminalNode> NATIVE() { return getTokens(PackParser.NATIVE); }
		public TerminalNode NATIVE(int i) {
			return getToken(PackParser.NATIVE, i);
		}
		public NativeLittleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nativeLittle; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterNativeLittle(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitNativeLittle(this);
		}
	}

	public final NativeLittleContext nativeLittle() throws RecognitionException {
		NativeLittleContext _localctx = new NativeLittleContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_nativeLittle);
		int _la;
		try {
			setState(342);
			switch ( getInterpreter().adaptivePredict(_input,75,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(319); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(318);
					match(NATIVE);
					}
					}
					setState(321); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==NATIVE );
				setState(323);
				match(LITTLE);
				setState(327);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==NATIVE) {
					{
					{
					setState(324);
					match(NATIVE);
					}
					}
					setState(329);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(333);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==NATIVE) {
					{
					{
					setState(330);
					match(NATIVE);
					}
					}
					setState(335);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(336);
				match(LITTLE);
				setState(338); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(337);
					match(NATIVE);
					}
					}
					setState(340); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==NATIVE );
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NativeBigContext extends ParserRuleContext {
		public TerminalNode BIG() { return getToken(PackParser.BIG, 0); }
		public List<TerminalNode> NATIVE() { return getTokens(PackParser.NATIVE); }
		public TerminalNode NATIVE(int i) {
			return getToken(PackParser.NATIVE, i);
		}
		public NativeBigContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nativeBig; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).enterNativeBig(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PackListener ) ((PackListener)listener).exitNativeBig(this);
		}
	}

	public final NativeBigContext nativeBig() throws RecognitionException {
		NativeBigContext _localctx = new NativeBigContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_nativeBig);
		int _la;
		try {
			setState(368);
			switch ( getInterpreter().adaptivePredict(_input,80,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(345); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(344);
					match(NATIVE);
					}
					}
					setState(347); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==NATIVE );
				setState(349);
				match(BIG);
				setState(353);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==NATIVE) {
					{
					{
					setState(350);
					match(NATIVE);
					}
					}
					setState(355);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(359);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==NATIVE) {
					{
					{
					setState(356);
					match(NATIVE);
					}
					}
					setState(361);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(362);
				match(BIG);
				setState(364); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(363);
					match(NATIVE);
					}
					}
					setState(366); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==NATIVE );
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\62\u0175\4\2\t\2"+
		"\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\3\2\7\2\24\n"+
		"\2\f\2\16\2\27\13\2\3\3\3\3\5\3\33\n\3\3\3\3\3\5\3\37\n\3\3\3\3\3\3\3"+
		"\5\3$\n\3\3\3\3\3\3\3\5\3)\n\3\3\3\3\3\5\3-\n\3\3\3\5\3\60\n\3\3\3\3\3"+
		"\3\3\5\3\65\n\3\3\3\5\38\n\3\3\3\3\3\3\3\5\3=\n\3\3\3\5\3@\n\3\3\3\3\3"+
		"\5\3D\n\3\3\3\5\3G\n\3\3\3\3\3\3\3\3\3\5\3M\n\3\3\3\5\3P\n\3\3\3\3\3\3"+
		"\3\3\3\5\3V\n\3\3\3\5\3Y\n\3\3\3\3\3\5\3]\n\3\3\3\5\3`\n\3\3\3\5\3c\n"+
		"\3\3\3\3\3\3\3\3\3\5\3i\n\3\3\3\5\3l\n\3\3\3\5\3o\n\3\3\3\3\3\3\3\3\3"+
		"\5\3u\n\3\3\3\5\3x\n\3\3\3\5\3{\n\3\3\3\3\3\5\3\177\n\3\3\3\5\3\u0082"+
		"\n\3\3\3\5\3\u0085\n\3\3\3\3\3\3\3\3\3\5\3\u008b\n\3\3\3\5\3\u008e\n\3"+
		"\3\3\3\3\3\3\3\3\5\3\u0094\n\3\3\3\5\3\u0097\n\3\3\3\3\3\5\3\u009b\n\3"+
		"\3\3\3\3\5\3\u009f\n\3\3\3\5\3\u00a2\n\3\3\3\3\3\3\3\3\3\5\3\u00a8\n\3"+
		"\3\3\5\3\u00ab\n\3\3\3\3\3\3\3\3\3\5\3\u00b1\n\3\3\3\5\3\u00b4\n\3\3\3"+
		"\3\3\5\3\u00b8\n\3\3\3\3\3\5\3\u00bc\n\3\3\3\5\3\u00bf\n\3\3\3\3\3\5\3"+
		"\u00c3\n\3\3\3\3\3\5\3\u00c7\n\3\3\3\3\3\5\3\u00cb\n\3\3\3\3\3\5\3\u00cf"+
		"\n\3\3\3\3\3\5\3\u00d3\n\3\3\3\3\3\5\3\u00d7\n\3\3\3\3\3\5\3\u00db\n\3"+
		"\3\3\3\3\5\3\u00df\n\3\3\3\3\3\5\3\u00e3\n\3\3\3\3\3\5\3\u00e7\n\3\3\3"+
		"\3\3\5\3\u00eb\n\3\3\3\3\3\5\3\u00ef\n\3\3\3\3\3\5\3\u00f3\n\3\3\3\3\3"+
		"\5\3\u00f7\n\3\3\3\3\3\5\3\u00fb\n\3\3\3\3\3\5\3\u00ff\n\3\3\3\3\3\5\3"+
		"\u0103\n\3\3\3\3\3\5\3\u0107\n\3\3\3\3\3\3\3\5\3\u010c\n\3\3\3\3\3\5\3"+
		"\u0110\n\3\3\3\3\3\5\3\u0114\n\3\3\3\3\3\3\3\5\3\u0119\n\3\3\4\3\4\3\5"+
		"\3\5\6\5\u011f\n\5\r\5\16\5\u0120\3\5\3\5\5\5\u0125\n\5\3\6\7\6\u0128"+
		"\n\6\f\6\16\6\u012b\13\6\3\6\3\6\7\6\u012f\n\6\f\6\16\6\u0132\13\6\3\7"+
		"\7\7\u0135\n\7\f\7\16\7\u0138\13\7\3\7\3\7\7\7\u013c\n\7\f\7\16\7\u013f"+
		"\13\7\3\b\6\b\u0142\n\b\r\b\16\b\u0143\3\b\3\b\7\b\u0148\n\b\f\b\16\b"+
		"\u014b\13\b\3\b\7\b\u014e\n\b\f\b\16\b\u0151\13\b\3\b\3\b\6\b\u0155\n"+
		"\b\r\b\16\b\u0156\5\b\u0159\n\b\3\t\6\t\u015c\n\t\r\t\16\t\u015d\3\t\3"+
		"\t\7\t\u0162\n\t\f\t\16\t\u0165\13\t\3\t\7\t\u0168\n\t\f\t\16\t\u016b"+
		"\13\t\3\t\3\t\6\t\u016f\n\t\r\t\16\t\u0170\5\t\u0173\n\t\3\t\2\2\n\2\4"+
		"\6\b\n\f\16\20\2\7\3\2\23\24\3\2\25\26\3\2%&\6\2\7\b\r\16\21&()\4\2**"+
		"\60\60\u01e7\2\25\3\2\2\2\4\u0118\3\2\2\2\6\u011a\3\2\2\2\b\u011c\3\2"+
		"\2\2\n\u0129\3\2\2\2\f\u0136\3\2\2\2\16\u0158\3\2\2\2\20\u0172\3\2\2\2"+
		"\22\24\5\4\3\2\23\22\3\2\2\2\24\27\3\2\2\2\25\23\3\2\2\2\25\26\3\2\2\2"+
		"\26\3\3\2\2\2\27\25\3\2\2\2\30\32\7\3\2\2\31\33\5\6\4\2\32\31\3\2\2\2"+
		"\32\33\3\2\2\2\33\u0119\3\2\2\2\34\36\7\4\2\2\35\37\5\6\4\2\36\35\3\2"+
		"\2\2\36\37\3\2\2\2\37\u0119\3\2\2\2 !\7\5\2\2!#\5\n\6\2\"$\5\6\4\2#\""+
		"\3\2\2\2#$\3\2\2\2$\u0119\3\2\2\2%&\7\5\2\2&(\5\f\7\2\')\5\6\4\2(\'\3"+
		"\2\2\2()\3\2\2\2)\u0119\3\2\2\2*,\7\5\2\2+-\7/\2\2,+\3\2\2\2,-\3\2\2\2"+
		"-/\3\2\2\2.\60\5\6\4\2/.\3\2\2\2/\60\3\2\2\2\60\u0119\3\2\2\2\61\62\7"+
		"\6\2\2\62\65\5\n\6\2\63\65\7\7\2\2\64\61\3\2\2\2\64\63\3\2\2\2\65\67\3"+
		"\2\2\2\668\5\6\4\2\67\66\3\2\2\2\678\3\2\2\28\u0119\3\2\2\29:\7\6\2\2"+
		":=\5\f\7\2;=\7\b\2\2<9\3\2\2\2<;\3\2\2\2=?\3\2\2\2>@\5\6\4\2?>\3\2\2\2"+
		"?@\3\2\2\2@\u0119\3\2\2\2AC\7\6\2\2BD\7/\2\2CB\3\2\2\2CD\3\2\2\2DF\3\2"+
		"\2\2EG\5\6\4\2FE\3\2\2\2FG\3\2\2\2G\u0119\3\2\2\2HI\7\t\2\2IM\5\n\6\2"+
		"JK\7\n\2\2KM\7-\2\2LH\3\2\2\2LJ\3\2\2\2MO\3\2\2\2NP\5\6\4\2ON\3\2\2\2"+
		"OP\3\2\2\2P\u0119\3\2\2\2QR\7\t\2\2RV\5\f\7\2ST\7\n\2\2TV\7.\2\2UQ\3\2"+
		"\2\2US\3\2\2\2VX\3\2\2\2WY\5\6\4\2XW\3\2\2\2XY\3\2\2\2Y\u0119\3\2\2\2"+
		"Z\\\7\t\2\2[]\7/\2\2\\[\3\2\2\2\\]\3\2\2\2]`\3\2\2\2^`\7\n\2\2_Z\3\2\2"+
		"\2_^\3\2\2\2`b\3\2\2\2ac\5\6\4\2ba\3\2\2\2bc\3\2\2\2c\u0119\3\2\2\2de"+
		"\7\13\2\2ei\5\n\6\2fg\7\f\2\2gi\7-\2\2hd\3\2\2\2hf\3\2\2\2il\3\2\2\2j"+
		"l\7\r\2\2kh\3\2\2\2kj\3\2\2\2ln\3\2\2\2mo\5\6\4\2nm\3\2\2\2no\3\2\2\2"+
		"o\u0119\3\2\2\2pq\7\13\2\2qu\5\f\7\2rs\7\f\2\2su\7.\2\2tp\3\2\2\2tr\3"+
		"\2\2\2ux\3\2\2\2vx\7\16\2\2wt\3\2\2\2wv\3\2\2\2xz\3\2\2\2y{\5\6\4\2zy"+
		"\3\2\2\2z{\3\2\2\2{\u0119\3\2\2\2|~\7\13\2\2}\177\7/\2\2~}\3\2\2\2~\177"+
		"\3\2\2\2\177\u0082\3\2\2\2\u0080\u0082\7\f\2\2\u0081|\3\2\2\2\u0081\u0080"+
		"\3\2\2\2\u0082\u0084\3\2\2\2\u0083\u0085\5\6\4\2\u0084\u0083\3\2\2\2\u0084"+
		"\u0085\3\2\2\2\u0085\u0119\3\2\2\2\u0086\u0087\7\17\2\2\u0087\u008b\5"+
		"\n\6\2\u0088\u0089\7\n\2\2\u0089\u008b\5\16\b\2\u008a\u0086\3\2\2\2\u008a"+
		"\u0088\3\2\2\2\u008b\u008d\3\2\2\2\u008c\u008e\5\6\4\2\u008d\u008c\3\2"+
		"\2\2\u008d\u008e\3\2\2\2\u008e\u0119\3\2\2\2\u008f\u0090\7\17\2\2\u0090"+
		"\u0094\5\f\7\2\u0091\u0092\7\n\2\2\u0092\u0094\5\20\t\2\u0093\u008f\3"+
		"\2\2\2\u0093\u0091\3\2\2\2\u0094\u0096\3\2\2\2\u0095\u0097\5\6\4\2\u0096"+
		"\u0095\3\2\2\2\u0096\u0097\3\2\2\2\u0097\u0119\3\2\2\2\u0098\u009a\7\17"+
		"\2\2\u0099\u009b\7/\2\2\u009a\u0099\3\2\2\2\u009a\u009b\3\2\2\2\u009b"+
		"\u009f\3\2\2\2\u009c\u009d\7\n\2\2\u009d\u009f\7/\2\2\u009e\u0098\3\2"+
		"\2\2\u009e\u009c\3\2\2\2\u009f\u00a1\3\2\2\2\u00a0\u00a2\5\6\4\2\u00a1"+
		"\u00a0\3\2\2\2\u00a1\u00a2\3\2\2\2\u00a2\u0119\3\2\2\2\u00a3\u00a4\7\20"+
		"\2\2\u00a4\u00a8\5\n\6\2\u00a5\u00a6\7\f\2\2\u00a6\u00a8\5\16\b\2\u00a7"+
		"\u00a3\3\2\2\2\u00a7\u00a5\3\2\2\2\u00a8\u00aa\3\2\2\2\u00a9\u00ab\5\6"+
		"\4\2\u00aa\u00a9\3\2\2\2\u00aa\u00ab\3\2\2\2\u00ab\u0119\3\2\2\2\u00ac"+
		"\u00ad\7\20\2\2\u00ad\u00b1\5\f\7\2\u00ae\u00af\7\f\2\2\u00af\u00b1\5"+
		"\20\t\2\u00b0\u00ac\3\2\2\2\u00b0\u00ae\3\2\2\2\u00b1\u00b3\3\2\2\2\u00b2"+
		"\u00b4\5\6\4\2\u00b3\u00b2\3\2\2\2\u00b3\u00b4\3\2\2\2\u00b4\u0119\3\2"+
		"\2\2\u00b5\u00b7\7\20\2\2\u00b6\u00b8\7/\2\2\u00b7\u00b6\3\2\2\2\u00b7"+
		"\u00b8\3\2\2\2\u00b8\u00bc\3\2\2\2\u00b9\u00ba\7\f\2\2\u00ba\u00bc\7/"+
		"\2\2\u00bb\u00b5\3\2\2\2\u00bb\u00b9\3\2\2\2\u00bc\u00be\3\2\2\2\u00bd"+
		"\u00bf\5\6\4\2\u00be\u00bd\3\2\2\2\u00be\u00bf\3\2\2\2\u00bf\u0119\3\2"+
		"\2\2\u00c0\u00c2\7\21\2\2\u00c1\u00c3\5\6\4\2\u00c2\u00c1\3\2\2\2\u00c2"+
		"\u00c3\3\2\2\2\u00c3\u0119\3\2\2\2\u00c4\u00c6\7\22\2\2\u00c5\u00c7\5"+
		"\6\4\2\u00c6\u00c5\3\2\2\2\u00c6\u00c7\3\2\2\2\u00c7\u0119\3\2\2\2\u00c8"+
		"\u00ca\t\2\2\2\u00c9\u00cb\5\6\4\2\u00ca\u00c9\3\2\2\2\u00ca\u00cb\3\2"+
		"\2\2\u00cb\u0119\3\2\2\2\u00cc\u00ce\t\3\2\2\u00cd\u00cf\5\6\4\2\u00ce"+
		"\u00cd\3\2\2\2\u00ce\u00cf\3\2\2\2\u00cf\u0119\3\2\2\2\u00d0\u00d2\7\27"+
		"\2\2\u00d1\u00d3\5\6\4\2\u00d2\u00d1\3\2\2\2\u00d2\u00d3\3\2\2\2\u00d3"+
		"\u0119\3\2\2\2\u00d4\u00d6\7\30\2\2\u00d5\u00d7\5\6\4\2\u00d6\u00d5\3"+
		"\2\2\2\u00d6\u00d7\3\2\2\2\u00d7\u0119\3\2\2\2\u00d8\u00da\7\31\2\2\u00d9"+
		"\u00db\5\6\4\2\u00da\u00d9\3\2\2\2\u00da\u00db\3\2\2\2\u00db\u0119\3\2"+
		"\2\2\u00dc\u00de\7\32\2\2\u00dd\u00df\5\6\4\2\u00de\u00dd\3\2\2\2\u00de"+
		"\u00df\3\2\2\2\u00df\u0119\3\2\2\2\u00e0\u00e2\7\33\2\2\u00e1\u00e3\5"+
		"\6\4\2\u00e2\u00e1\3\2\2\2\u00e2\u00e3\3\2\2\2\u00e3\u0119\3\2\2\2\u00e4"+
		"\u00e6\7\34\2\2\u00e5\u00e7\5\6\4\2\u00e6\u00e5\3\2\2\2\u00e6\u00e7\3"+
		"\2\2\2\u00e7\u0119\3\2\2\2\u00e8\u00ea\7\35\2\2\u00e9\u00eb\5\6\4\2\u00ea"+
		"\u00e9\3\2\2\2\u00ea\u00eb\3\2\2\2\u00eb\u0119\3\2\2\2\u00ec\u00ee\7\36"+
		"\2\2\u00ed\u00ef\5\6\4\2\u00ee\u00ed\3\2\2\2\u00ee\u00ef\3\2\2\2\u00ef"+
		"\u0119\3\2\2\2\u00f0\u00f2\7\37\2\2\u00f1\u00f3\5\6\4\2\u00f2\u00f1\3"+
		"\2\2\2\u00f2\u00f3\3\2\2\2\u00f3\u0119\3\2\2\2\u00f4\u00f6\7 \2\2\u00f5"+
		"\u00f7\5\6\4\2\u00f6\u00f5\3\2\2\2\u00f6\u00f7\3\2\2\2\u00f7\u0119\3\2"+
		"\2\2\u00f8\u00fa\7!\2\2\u00f9\u00fb\5\6\4\2\u00fa\u00f9\3\2\2\2\u00fa"+
		"\u00fb\3\2\2\2\u00fb\u0119\3\2\2\2\u00fc\u00fe\7\"\2\2\u00fd\u00ff\5\6"+
		"\4\2\u00fe\u00fd\3\2\2\2\u00fe\u00ff\3\2\2\2\u00ff\u0119\3\2\2\2\u0100"+
		"\u0102\7#\2\2\u0101\u0103\5\6\4\2\u0102\u0101\3\2\2\2\u0102\u0103\3\2"+
		"\2\2\u0103\u0119\3\2\2\2\u0104\u0106\7$\2\2\u0105\u0107\5\6\4\2\u0106"+
		"\u0105\3\2\2\2\u0106\u0107\3\2\2\2\u0107\u0119\3\2\2\2\u0108\u0119\t\4"+
		"\2\2\u0109\u010b\7\'\2\2\u010a\u010c\5\6\4\2\u010b\u010a\3\2\2\2\u010b"+
		"\u010c\3\2\2\2\u010c\u0119\3\2\2\2\u010d\u010f\7(\2\2\u010e\u0110\5\6"+
		"\4\2\u010f\u010e\3\2\2\2\u010f\u0110\3\2\2\2\u0110\u0119\3\2\2\2\u0111"+
		"\u0113\7)\2\2\u0112\u0114\5\6\4\2\u0113\u0112\3\2\2\2\u0113\u0114\3\2"+
		"\2\2\u0114\u0119\3\2\2\2\u0115\u0119\5\b\5\2\u0116\u0117\t\5\2\2\u0117"+
		"\u0119\7/\2\2\u0118\30\3\2\2\2\u0118\34\3\2\2\2\u0118 \3\2\2\2\u0118%"+
		"\3\2\2\2\u0118*\3\2\2\2\u0118\64\3\2\2\2\u0118<\3\2\2\2\u0118A\3\2\2\2"+
		"\u0118L\3\2\2\2\u0118U\3\2\2\2\u0118_\3\2\2\2\u0118k\3\2\2\2\u0118w\3"+
		"\2\2\2\u0118\u0081\3\2\2\2\u0118\u008a\3\2\2\2\u0118\u0093\3\2\2\2\u0118"+
		"\u009e\3\2\2\2\u0118\u00a7\3\2\2\2\u0118\u00b0\3\2\2\2\u0118\u00bb\3\2"+
		"\2\2\u0118\u00c0\3\2\2\2\u0118\u00c4\3\2\2\2\u0118\u00c8\3\2\2\2\u0118"+
		"\u00cc\3\2\2\2\u0118\u00d0\3\2\2\2\u0118\u00d4\3\2\2\2\u0118\u00d8\3\2"+
		"\2\2\u0118\u00dc\3\2\2\2\u0118\u00e0\3\2\2\2\u0118\u00e4\3\2\2\2\u0118"+
		"\u00e8\3\2\2\2\u0118\u00ec\3\2\2\2\u0118\u00f0\3\2\2\2\u0118\u00f4\3\2"+
		"\2\2\u0118\u00f8\3\2\2\2\u0118\u00fc\3\2\2\2\u0118\u0100\3\2\2\2\u0118"+
		"\u0104\3\2\2\2\u0118\u0108\3\2\2\2\u0118\u0109\3\2\2\2\u0118\u010d\3\2"+
		"\2\2\u0118\u0111\3\2\2\2\u0118\u0115\3\2\2\2\u0118\u0116\3\2\2\2\u0119"+
		"\5\3\2\2\2\u011a\u011b\t\6\2\2\u011b\7\3\2\2\2\u011c\u011e\7+\2\2\u011d"+
		"\u011f\5\4\3\2\u011e\u011d\3\2\2\2\u011f\u0120\3\2\2\2\u0120\u011e\3\2"+
		"\2\2\u0120\u0121\3\2\2\2\u0121\u0122\3\2\2\2\u0122\u0124\7,\2\2\u0123"+
		"\u0125\7\60\2\2\u0124\u0123\3\2\2\2\u0124\u0125\3\2\2\2\u0125\t\3\2\2"+
		"\2\u0126\u0128\7/\2\2\u0127\u0126\3\2\2\2\u0128\u012b\3\2\2\2\u0129\u0127"+
		"\3\2\2\2\u0129\u012a\3\2\2\2\u012a\u012c\3\2\2\2\u012b\u0129\3\2\2\2\u012c"+
		"\u0130\7-\2\2\u012d\u012f\7/\2\2\u012e\u012d\3\2\2\2\u012f\u0132\3\2\2"+
		"\2\u0130\u012e\3\2\2\2\u0130\u0131\3\2\2\2\u0131\13\3\2\2\2\u0132\u0130"+
		"\3\2\2\2\u0133\u0135\7/\2\2\u0134\u0133\3\2\2\2\u0135\u0138\3\2\2\2\u0136"+
		"\u0134\3\2\2\2\u0136\u0137\3\2\2\2\u0137\u0139\3\2\2\2\u0138\u0136\3\2"+
		"\2\2\u0139\u013d\7.\2\2\u013a\u013c\7/\2\2\u013b\u013a\3\2\2\2\u013c\u013f"+
		"\3\2\2\2\u013d\u013b\3\2\2\2\u013d\u013e\3\2\2\2\u013e\r\3\2\2\2\u013f"+
		"\u013d\3\2\2\2\u0140\u0142\7/\2\2\u0141\u0140\3\2\2\2\u0142\u0143\3\2"+
		"\2\2\u0143\u0141\3\2\2\2\u0143\u0144\3\2\2\2\u0144\u0145\3\2\2\2\u0145"+
		"\u0149\7-\2\2\u0146\u0148\7/\2\2\u0147\u0146\3\2\2\2\u0148\u014b\3\2\2"+
		"\2\u0149\u0147\3\2\2\2\u0149\u014a\3\2\2\2\u014a\u0159\3\2\2\2\u014b\u0149"+
		"\3\2\2\2\u014c\u014e\7/\2\2\u014d\u014c\3\2\2\2\u014e\u0151\3\2\2\2\u014f"+
		"\u014d\3\2\2\2\u014f\u0150\3\2\2\2\u0150\u0152\3\2\2\2\u0151\u014f\3\2"+
		"\2\2\u0152\u0154\7-\2\2\u0153\u0155\7/\2\2\u0154\u0153\3\2\2\2\u0155\u0156"+
		"\3\2\2\2\u0156\u0154\3\2\2\2\u0156\u0157\3\2\2\2\u0157\u0159\3\2\2\2\u0158"+
		"\u0141\3\2\2\2\u0158\u014f\3\2\2\2\u0159\17\3\2\2\2\u015a\u015c\7/\2\2"+
		"\u015b\u015a\3\2\2\2\u015c\u015d\3\2\2\2\u015d\u015b\3\2\2\2\u015d\u015e"+
		"\3\2\2\2\u015e\u015f\3\2\2\2\u015f\u0163\7.\2\2\u0160\u0162\7/\2\2\u0161"+
		"\u0160\3\2\2\2\u0162\u0165\3\2\2\2\u0163\u0161\3\2\2\2\u0163\u0164\3\2"+
		"\2\2\u0164\u0173\3\2\2\2\u0165\u0163\3\2\2\2\u0166\u0168\7/\2\2\u0167"+
		"\u0166\3\2\2\2\u0168\u016b\3\2\2\2\u0169\u0167\3\2\2\2\u0169\u016a\3\2"+
		"\2\2\u016a\u016c\3\2\2\2\u016b\u0169\3\2\2\2\u016c\u016e\7.\2\2\u016d"+
		"\u016f\7/\2\2\u016e\u016d\3\2\2\2\u016f\u0170\3\2\2\2\u0170\u016e\3\2"+
		"\2\2\u0170\u0171\3\2\2\2\u0171\u0173\3\2\2\2\u0172\u015b\3\2\2\2\u0172"+
		"\u0169\3\2\2\2\u0173\21\3\2\2\2S\25\32\36#(,/\64\67<?CFLOUX\\_bhkntwz"+
		"~\u0081\u0084\u008a\u008d\u0093\u0096\u009a\u009e\u00a1\u00a7\u00aa\u00b0"+
		"\u00b3\u00b7\u00bb\u00be\u00c2\u00c6\u00ca\u00ce\u00d2\u00d6\u00da\u00de"+
		"\u00e2\u00e6\u00ea\u00ee\u00f2\u00f6\u00fa\u00fe\u0102\u0106\u010b\u010f"+
		"\u0113\u0118\u0120\u0124\u0129\u0130\u0136\u013d\u0143\u0149\u014f\u0156"+
		"\u0158\u015d\u0163\u0169\u0170\u0172";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}