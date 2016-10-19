// Generated from org/jruby/truffle/core/format/pack/Pack.g4 by ANTLR 4.5.1
package org.jruby.truffle.core.format.pack;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.RuntimeMetaData;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.VocabularyImpl;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class PackLexer extends Lexer {
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
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
		"T__9", "T__10", "T__11", "T__12", "T__13", "T__14", "T__15", "T__16", 
		"T__17", "T__18", "T__19", "T__20", "T__21", "T__22", "T__23", "T__24", 
		"T__25", "T__26", "T__27", "T__28", "T__29", "T__30", "T__31", "T__32", 
		"T__33", "T__34", "T__35", "T__36", "T__37", "T__38", "T__39", "T__40", 
		"T__41", "LITTLE", "BIG", "NATIVE", "INT", "WS", "COMMENT"
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


	public PackLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Pack.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\62\u00d9\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t"+
		"+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\3\2\3\2\3\3\3\3\3\4\3\4"+
		"\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\r"+
		"\3\r\3\16\3\16\3\17\3\17\3\20\3\20\3\21\3\21\3\22\3\22\3\23\3\23\3\24"+
		"\3\24\3\25\3\25\3\26\3\26\3\27\3\27\3\30\3\30\3\31\3\31\3\32\3\32\3\33"+
		"\3\33\3\34\3\34\3\35\3\35\3\36\3\36\3\37\3\37\3 \3 \3!\3!\3\"\3\"\3#\3"+
		"#\3$\3$\3%\3%\3&\3&\3\'\3\'\3(\3(\3)\3)\3*\3*\3+\3+\3,\3,\3-\3-\3.\3."+
		"\3/\6/\u00bf\n/\r/\16/\u00c0\3\60\6\60\u00c4\n\60\r\60\16\60\u00c5\3\60"+
		"\3\60\3\61\3\61\7\61\u00cc\n\61\f\61\16\61\u00cf\13\61\3\61\5\61\u00d2"+
		"\n\61\3\61\3\61\5\61\u00d6\n\61\3\61\3\61\3\u00cd\2\62\3\3\5\4\7\5\t\6"+
		"\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23%\24"+
		"\'\25)\26+\27-\30/\31\61\32\63\33\65\34\67\359\36;\37= ?!A\"C#E$G%I&K"+
		"\'M(O)Q*S+U,W-Y.[/]\60_\61a\62\3\2\5\4\2##aa\3\2\62;\5\2\2\2\13\17\"\""+
		"\u00dd\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2"+
		"\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3"+
		"\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2"+
		"\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2"+
		"/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2"+
		"\2\2;\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2"+
		"G\3\2\2\2\2I\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2\2Q\3\2\2\2\2S\3"+
		"\2\2\2\2U\3\2\2\2\2W\3\2\2\2\2Y\3\2\2\2\2[\3\2\2\2\2]\3\2\2\2\2_\3\2\2"+
		"\2\2a\3\2\2\2\3c\3\2\2\2\5e\3\2\2\2\7g\3\2\2\2\ti\3\2\2\2\13k\3\2\2\2"+
		"\rm\3\2\2\2\17o\3\2\2\2\21q\3\2\2\2\23s\3\2\2\2\25u\3\2\2\2\27w\3\2\2"+
		"\2\31y\3\2\2\2\33{\3\2\2\2\35}\3\2\2\2\37\177\3\2\2\2!\u0081\3\2\2\2#"+
		"\u0083\3\2\2\2%\u0085\3\2\2\2\'\u0087\3\2\2\2)\u0089\3\2\2\2+\u008b\3"+
		"\2\2\2-\u008d\3\2\2\2/\u008f\3\2\2\2\61\u0091\3\2\2\2\63\u0093\3\2\2\2"+
		"\65\u0095\3\2\2\2\67\u0097\3\2\2\29\u0099\3\2\2\2;\u009b\3\2\2\2=\u009d"+
		"\3\2\2\2?\u009f\3\2\2\2A\u00a1\3\2\2\2C\u00a3\3\2\2\2E\u00a5\3\2\2\2G"+
		"\u00a7\3\2\2\2I\u00a9\3\2\2\2K\u00ab\3\2\2\2M\u00ad\3\2\2\2O\u00af\3\2"+
		"\2\2Q\u00b1\3\2\2\2S\u00b3\3\2\2\2U\u00b5\3\2\2\2W\u00b7\3\2\2\2Y\u00b9"+
		"\3\2\2\2[\u00bb\3\2\2\2]\u00be\3\2\2\2_\u00c3\3\2\2\2a\u00c9\3\2\2\2c"+
		"d\7e\2\2d\4\3\2\2\2ef\7E\2\2f\6\3\2\2\2gh\7u\2\2h\b\3\2\2\2ij\7U\2\2j"+
		"\n\3\2\2\2kl\7x\2\2l\f\3\2\2\2mn\7p\2\2n\16\3\2\2\2op\7k\2\2p\20\3\2\2"+
		"\2qr\7n\2\2r\22\3\2\2\2st\7K\2\2t\24\3\2\2\2uv\7N\2\2v\26\3\2\2\2wx\7"+
		"X\2\2x\30\3\2\2\2yz\7P\2\2z\32\3\2\2\2{|\7s\2\2|\34\3\2\2\2}~\7S\2\2~"+
		"\36\3\2\2\2\177\u0080\7W\2\2\u0080 \3\2\2\2\u0081\u0082\7y\2\2\u0082\""+
		"\3\2\2\2\u0083\u0084\7f\2\2\u0084$\3\2\2\2\u0085\u0086\7F\2\2\u0086&\3"+
		"\2\2\2\u0087\u0088\7h\2\2\u0088(\3\2\2\2\u0089\u008a\7H\2\2\u008a*\3\2"+
		"\2\2\u008b\u008c\7G\2\2\u008c,\3\2\2\2\u008d\u008e\7g\2\2\u008e.\3\2\2"+
		"\2\u008f\u0090\7I\2\2\u0090\60\3\2\2\2\u0091\u0092\7i\2\2\u0092\62\3\2"+
		"\2\2\u0093\u0094\7C\2\2\u0094\64\3\2\2\2\u0095\u0096\7c\2\2\u0096\66\3"+
		"\2\2\2\u0097\u0098\7\\\2\2\u00988\3\2\2\2\u0099\u009a\7D\2\2\u009a:\3"+
		"\2\2\2\u009b\u009c\7d\2\2\u009c<\3\2\2\2\u009d\u009e\7J\2\2\u009e>\3\2"+
		"\2\2\u009f\u00a0\7j\2\2\u00a0@\3\2\2\2\u00a1\u00a2\7w\2\2\u00a2B\3\2\2"+
		"\2\u00a3\u00a4\7O\2\2\u00a4D\3\2\2\2\u00a5\u00a6\7o\2\2\u00a6F\3\2\2\2"+
		"\u00a7\u00a8\7r\2\2\u00a8H\3\2\2\2\u00a9\u00aa\7R\2\2\u00aaJ\3\2\2\2\u00ab"+
		"\u00ac\7B\2\2\u00acL\3\2\2\2\u00ad\u00ae\7Z\2\2\u00aeN\3\2\2\2\u00af\u00b0"+
		"\7z\2\2\u00b0P\3\2\2\2\u00b1\u00b2\7,\2\2\u00b2R\3\2\2\2\u00b3\u00b4\7"+
		"*\2\2\u00b4T\3\2\2\2\u00b5\u00b6\7+\2\2\u00b6V\3\2\2\2\u00b7\u00b8\7>"+
		"\2\2\u00b8X\3\2\2\2\u00b9\u00ba\7@\2\2\u00baZ\3\2\2\2\u00bb\u00bc\t\2"+
		"\2\2\u00bc\\\3\2\2\2\u00bd\u00bf\t\3\2\2\u00be\u00bd\3\2\2\2\u00bf\u00c0"+
		"\3\2\2\2\u00c0\u00be\3\2\2\2\u00c0\u00c1\3\2\2\2\u00c1^\3\2\2\2\u00c2"+
		"\u00c4\t\4\2\2\u00c3\u00c2\3\2\2\2\u00c4\u00c5\3\2\2\2\u00c5\u00c3\3\2"+
		"\2\2\u00c5\u00c6\3\2\2\2\u00c6\u00c7\3\2\2\2\u00c7\u00c8\b\60\2\2\u00c8"+
		"`\3\2\2\2\u00c9\u00cd\7%\2\2\u00ca\u00cc\13\2\2\2\u00cb\u00ca\3\2\2\2"+
		"\u00cc\u00cf\3\2\2\2\u00cd\u00ce\3\2\2\2\u00cd\u00cb\3\2\2\2\u00ce\u00d5"+
		"\3\2\2\2\u00cf\u00cd\3\2\2\2\u00d0\u00d2\7\17\2\2\u00d1\u00d0\3\2\2\2"+
		"\u00d1\u00d2\3\2\2\2\u00d2\u00d3\3\2\2\2\u00d3\u00d6\7\f\2\2\u00d4\u00d6"+
		"\7\2\2\3\u00d5\u00d1\3\2\2\2\u00d5\u00d4\3\2\2\2\u00d6\u00d7\3\2\2\2\u00d7"+
		"\u00d8\b\61\2\2\u00d8b\3\2\2\2\b\2\u00c0\u00c5\u00cd\u00d1\u00d5\3\b\2"+
		"\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}