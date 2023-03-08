// Generated from c:\Users\henri\FEUP\ano3\C\comp2023-1a\src\main\antlr\comp2023\grammar\Javamm.g4 by ANTLR 4.9.2

    package pt.up.fe.comp2023;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class JavammLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.9.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, INTEGER=11, ID=12, COMMENTINLINE=13, COMMENTMULTILINE=14, WS=15;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
			"T__9", "INTEGER", "ID", "COMMENTINLINE", "COMMENTMULTILINE", "WS"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "';'", "'='", "'('", "')'", "'*'", "'/'", "'+'", "'-'", "'<'", 
			"'&&'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, "INTEGER", 
			"ID", "COMMENTINLINE", "COMMENTMULTILINE", "WS"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
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


	public JavammLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Javamm.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\21b\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\3\2\3\2\3\3\3\3\3\4"+
		"\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\13\3"+
		"\f\6\f8\n\f\r\f\16\f9\3\r\3\r\7\r>\n\r\f\r\16\rA\13\r\3\16\3\16\3\16\3"+
		"\16\7\16G\n\16\f\16\16\16J\13\16\3\16\3\16\3\17\3\17\3\17\3\17\7\17R\n"+
		"\17\f\17\16\17U\13\17\3\17\3\17\3\17\3\17\3\17\3\20\6\20]\n\20\r\20\16"+
		"\20^\3\20\3\20\3S\2\21\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27"+
		"\r\31\16\33\17\35\20\37\21\3\2\7\3\2\62;\6\2&&C\\aac|\7\2&&\62;C\\aac"+
		"|\4\2\f\f\17\17\5\2\13\f\16\17\"\"\2f\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2"+
		"\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2"+
		"\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3"+
		"\2\2\2\2\37\3\2\2\2\3!\3\2\2\2\5#\3\2\2\2\7%\3\2\2\2\t\'\3\2\2\2\13)\3"+
		"\2\2\2\r+\3\2\2\2\17-\3\2\2\2\21/\3\2\2\2\23\61\3\2\2\2\25\63\3\2\2\2"+
		"\27\67\3\2\2\2\31;\3\2\2\2\33B\3\2\2\2\35M\3\2\2\2\37\\\3\2\2\2!\"\7="+
		"\2\2\"\4\3\2\2\2#$\7?\2\2$\6\3\2\2\2%&\7*\2\2&\b\3\2\2\2\'(\7+\2\2(\n"+
		"\3\2\2\2)*\7,\2\2*\f\3\2\2\2+,\7\61\2\2,\16\3\2\2\2-.\7-\2\2.\20\3\2\2"+
		"\2/\60\7/\2\2\60\22\3\2\2\2\61\62\7>\2\2\62\24\3\2\2\2\63\64\7(\2\2\64"+
		"\65\7(\2\2\65\26\3\2\2\2\668\t\2\2\2\67\66\3\2\2\289\3\2\2\29\67\3\2\2"+
		"\29:\3\2\2\2:\30\3\2\2\2;?\t\3\2\2<>\t\4\2\2=<\3\2\2\2>A\3\2\2\2?=\3\2"+
		"\2\2?@\3\2\2\2@\32\3\2\2\2A?\3\2\2\2BC\7\61\2\2CD\7\61\2\2DH\3\2\2\2E"+
		"G\n\5\2\2FE\3\2\2\2GJ\3\2\2\2HF\3\2\2\2HI\3\2\2\2IK\3\2\2\2JH\3\2\2\2"+
		"KL\b\16\2\2L\34\3\2\2\2MN\7\61\2\2NO\7,\2\2OS\3\2\2\2PR\13\2\2\2QP\3\2"+
		"\2\2RU\3\2\2\2ST\3\2\2\2SQ\3\2\2\2TV\3\2\2\2US\3\2\2\2VW\7,\2\2WX\7\61"+
		"\2\2XY\3\2\2\2YZ\b\17\2\2Z\36\3\2\2\2[]\t\6\2\2\\[\3\2\2\2]^\3\2\2\2^"+
		"\\\3\2\2\2^_\3\2\2\2_`\3\2\2\2`a\b\20\2\2a \3\2\2\2\b\29?HS^\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}