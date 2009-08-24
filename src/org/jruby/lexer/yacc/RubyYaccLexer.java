/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004-2005 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2005 Zach Dennis <zdennis@mktec.com>
 * Copyright (C) 2006 Thomas Corbat <tcorbat@hsr.ch>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.lexer.yacc;

import java.io.IOException;

import java.math.BigInteger;
import java.util.HashMap;

import org.jcodings.Encoding;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BignumNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FloatNode;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.StrNode;
import org.jruby.common.IRubyWarnings;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.lexer.yacc.SyntaxException.PID;
import org.jruby.parser.ParserSupport;
import org.jruby.parser.Tokens;
import org.jruby.util.ByteList;
import org.jruby.util.Convert2;


/** This is a port of the MRI lexer to Java it is compatible to Ruby 1.8.1.
 */
public class RubyYaccLexer {
    private static ByteList END_MARKER = new ByteList(new byte[] {'_', 'E', 'N', 'D', '_', '_'});
    private static ByteList BEGIN_DOC_MARKER = new ByteList(new byte[] {'b', 'e', 'g', 'i', 'n'});
    private static ByteList END_DOC_MARKER = new ByteList(new byte[] {'e', 'n', 'd'});
    private static HashMap<String, Keyword> map;
    
    static {
        map = new HashMap<String, Keyword>();
        
        map.put("end", Keyword.END);
        map.put("else", Keyword.ELSE);
        map.put("case", Keyword.CASE);
        map.put("ensure", Keyword.ENSURE);
        map.put("module", Keyword.MODULE);
        map.put("elsif", Keyword.ELSIF);
        map.put("def", Keyword.DEF);
        map.put("rescue", Keyword.RESCUE);
        map.put("not", Keyword.NOT);
        map.put("then", Keyword.THEN);
        map.put("yield", Keyword.YIELD);
        map.put("for", Keyword.FOR);
        map.put("self", Keyword.SELF);
        map.put("false", Keyword.FALSE);
        map.put("retry", Keyword.RETRY);
        map.put("return", Keyword.RETURN);
        map.put("true", Keyword.TRUE);
        map.put("if", Keyword.IF);
        map.put("defined?", Keyword.DEFINED_P);
        map.put("super", Keyword.SUPER);
        map.put("undef", Keyword.UNDEF);
        map.put("break", Keyword.BREAK);
        map.put("in", Keyword.IN);
        map.put("do", Keyword.DO);
        map.put("nil", Keyword.NIL);
        map.put("until", Keyword.UNTIL);
        map.put("unless", Keyword.UNLESS);
        map.put("or", Keyword.OR);
        map.put("next", Keyword.NEXT);
        map.put("when", Keyword.WHEN);
        map.put("redo", Keyword.REDO);
        map.put("and", Keyword.AND);
        map.put("begin", Keyword.BEGIN);
        map.put("__LINE__", Keyword.__LINE__);
        map.put("class", Keyword.CLASS);
        map.put("__FILE__", Keyword.__FILE__);
        map.put("END", Keyword.LEND);
        map.put("BEGIN", Keyword.LBEGIN);
        map.put("while", Keyword.WHILE);
        map.put("alias", Keyword.ALIAS);
        map.put("__ENCODING__", Keyword.__ENCODING__);
    }

    private Encoding encoding;

    private int getFloatToken(String number) {
        double d;
        try {
            d = Double.parseDouble(number);
        } catch (NumberFormatException e) {
            warnings.warn(ID.FLOAT_OUT_OF_RANGE, getPosition(), "Float " + number + " out of range.", number);

            d = number.startsWith("-") ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }
        yaccValue = new FloatNode(getPosition(), d);
        return Tokens.tFLOAT;
    }

    private Object newBignumNode(String value, int radix) {
        return new BignumNode(getPosition(), new BigInteger(value, radix));
    }

    private Object newFixnumNode(String value, int radix) throws NumberFormatException {
        return new FixnumNode(getPosition(), Long.parseLong(value, radix));
    }
    
    public enum Keyword {
        END ("end", Tokens.kEND, Tokens.kEND, LexState.EXPR_END),
        ELSE ("else", Tokens.kELSE, Tokens.kELSE, LexState.EXPR_BEG),
        CASE ("case", Tokens.kCASE, Tokens.kCASE, LexState.EXPR_BEG),
        ENSURE ("ensure", Tokens.kENSURE, Tokens.kENSURE, LexState.EXPR_BEG),
        MODULE ("module", Tokens.kMODULE, Tokens.kMODULE, LexState.EXPR_BEG),
        ELSIF ("elsif", Tokens.kELSIF, Tokens.kELSIF, LexState.EXPR_BEG),
        DEF ("def", Tokens.kDEF, Tokens.kDEF, LexState.EXPR_FNAME),
        RESCUE ("rescue", Tokens.kRESCUE, Tokens.kRESCUE_MOD, LexState.EXPR_MID),
        NOT ("not", Tokens.kNOT, Tokens.kNOT, LexState.EXPR_BEG),
        THEN ("then", Tokens.kTHEN, Tokens.kTHEN, LexState.EXPR_BEG),
        YIELD ("yield", Tokens.kYIELD, Tokens.kYIELD, LexState.EXPR_ARG),
        FOR ("for", Tokens.kFOR, Tokens.kFOR, LexState.EXPR_BEG),
        SELF ("self", Tokens.kSELF, Tokens.kSELF, LexState.EXPR_END),
        FALSE ("false", Tokens.kFALSE, Tokens.kFALSE, LexState.EXPR_END),
        RETRY ("retry", Tokens.kRETRY, Tokens.kRETRY, LexState.EXPR_END),
        RETURN ("return", Tokens.kRETURN, Tokens.kRETURN, LexState.EXPR_MID),
        TRUE ("true", Tokens.kTRUE, Tokens.kTRUE, LexState.EXPR_END),
        IF ("if", Tokens.kIF, Tokens.kIF_MOD, LexState.EXPR_BEG),
        DEFINED_P ("defined?", Tokens.kDEFINED, Tokens.kDEFINED, LexState.EXPR_ARG),
        SUPER ("super", Tokens.kSUPER, Tokens.kSUPER, LexState.EXPR_ARG),
        UNDEF ("undef", Tokens.kUNDEF, Tokens.kUNDEF, LexState.EXPR_FNAME),
        BREAK ("break", Tokens.kBREAK, Tokens.kBREAK, LexState.EXPR_MID),
        IN ("in", Tokens.kIN, Tokens.kIN, LexState.EXPR_BEG),
        DO ("do", Tokens.kDO, Tokens.kDO, LexState.EXPR_BEG),
        NIL ("nil", Tokens.kNIL, Tokens.kNIL, LexState.EXPR_END),
        UNTIL ("until", Tokens.kUNTIL, Tokens.kUNTIL_MOD, LexState.EXPR_BEG),
        UNLESS ("unless", Tokens.kUNLESS, Tokens.kUNLESS_MOD, LexState.EXPR_BEG),
        OR ("or", Tokens.kOR, Tokens.kOR, LexState.EXPR_BEG),
        NEXT ("next", Tokens.kNEXT, Tokens.kNEXT, LexState.EXPR_MID),
        WHEN ("when", Tokens.kWHEN, Tokens.kWHEN, LexState.EXPR_BEG),
        REDO ("redo", Tokens.kREDO, Tokens.kREDO, LexState.EXPR_END),
        AND ("and", Tokens.kAND, Tokens.kAND, LexState.EXPR_BEG),
        BEGIN ("begin", Tokens.kBEGIN, Tokens.kBEGIN, LexState.EXPR_BEG),
        __LINE__ ("__LINE__", Tokens.k__LINE__, Tokens.k__LINE__, LexState.EXPR_END),
        CLASS ("class", Tokens.kCLASS, Tokens.kCLASS, LexState.EXPR_CLASS),
        __FILE__("__FILE__", Tokens.k__FILE__, Tokens.k__FILE__, LexState.EXPR_END),
        LEND ("END", Tokens.klEND, Tokens.klEND, LexState.EXPR_END),
        LBEGIN ("BEGIN", Tokens.klBEGIN, Tokens.klBEGIN, LexState.EXPR_END),
        WHILE ("while", Tokens.kWHILE, Tokens.kWHILE_MOD, LexState.EXPR_BEG),
        ALIAS ("alias", Tokens.kALIAS, Tokens.kALIAS, LexState.EXPR_FNAME),
        __ENCODING__("__ENCODING__", Tokens.k__ENCODING__, Tokens.k__ENCODING__, LexState.EXPR_END);
        
        public final String name;
        public final int id0;
        public final int id1;
        public final LexState state;
        
        Keyword(String name, int id0, int id1, LexState state) {
            this.name = name;
            this.id0 = id0;
            this.id1 = id1;
            this.state = state;
        }
    }
    
    public enum LexState {
        EXPR_BEG, EXPR_END, EXPR_ARG, EXPR_CMDARG, EXPR_ENDARG, EXPR_MID,
        EXPR_FNAME, EXPR_DOT, EXPR_CLASS, EXPR_VALUE
    }
    
    public static Keyword getKeyword(String str) {
        return (Keyword) map.get(str);
    }

    // Last token read via yylex().
    private int token;
    
    // Value of last token which had a value associated with it.
    Object yaccValue;

    // Stream of data that yylex() examines.
    private LexerSource src;
    
    // Used for tiny smidgen of grammar in lexer (see setParserSupport())
    private ParserSupport parserSupport = null;

    // What handles warnings
    private IRubyWarnings warnings;

    // Additional context surrounding tokens that both the lexer and
    // grammar use.
    private LexState lex_state;
    
    // Tempory buffer to build up a potential token.  Consumer takes responsibility to reset 
    // this before use.
    private StringBuilder tokenBuffer = new StringBuilder(60);

    private StackState conditionState = new StackState();
    private StackState cmdArgumentState = new StackState();
    private StrTerm lex_strterm;
    public boolean commandStart;

    // Give a name to a value.  Enebo: This should be used more.
    static final int EOF = -1;

    // ruby constants for strings (should this be moved somewhere else?)
    static final int STR_FUNC_ESCAPE=0x01;
    static final int STR_FUNC_EXPAND=0x02;
    static final int STR_FUNC_REGEXP=0x04;
    static final int STR_FUNC_QWORDS=0x08;
    static final int STR_FUNC_SYMBOL=0x10;
    // When the heredoc identifier specifies <<-EOF that indents before ident. are ok (the '-').
    static final int STR_FUNC_INDENT=0x20;

    private static final int str_squote = 0;
    private static final int str_dquote = STR_FUNC_EXPAND;
    private static final int str_xquote = STR_FUNC_EXPAND;
    private static final int str_regexp = STR_FUNC_REGEXP | STR_FUNC_ESCAPE | STR_FUNC_EXPAND;
    private static final int str_ssym   = STR_FUNC_SYMBOL;
    private static final int str_dsym   = STR_FUNC_SYMBOL | STR_FUNC_EXPAND;

    // Are we lexing Ruby 1.8 or 1.9+ syntax
    private boolean isOneEight;
    // Count of nested parentheses (1.9 only)
    private int parenNest = 0;
    // 1.9 only
    private int leftParenBegin = 0;

    public int incrementParenNest() {
        parenNest++;

        return parenNest;
    }

    public int getLeftParenBegin() {
        return leftParenBegin;
    }

    public void setLeftParenBegin(int value) {
        leftParenBegin = value;
    }

    public RubyYaccLexer() {
    	this(true);
    }
    
    public RubyYaccLexer(boolean isOneEight) {
    	reset();
        this.isOneEight = isOneEight;
    }
    
    public void reset() {
    	token = 0;
    	yaccValue = null;
    	src = null;
        lex_state = null;
        resetStacks();
        lex_strterm = null;
        commandStart = true;
    }
    
    /**
     * How the parser advances to the next token.
     * 
     * @return true if not at end of file (EOF).
     */
    public boolean advance() throws IOException {
        return (token = yylex()) != EOF;
    }
    
    /**
     * Last token read from the lexer at the end of a call to yylex()
     * 
     * @return last token read
     */
    public int token() {
        return token;
    }

    public StringBuilder getTokenBuffer() {
        return tokenBuffer;
    }
    
    /**
     * Value of last token (if it is a token which has a value).
     * 
     * @return value of last value-laden token
     */
    public Object value() {
        return yaccValue;
    }
    
    /**
     * Get position information for Token/Node that follows node represented by startPosition 
     * and current lexer location.
     * 
     * @param startPosition previous node/token
     * @param inclusive include previous node into position information of current node
     * @return a new position
     */
    public ISourcePosition getPosition(ISourcePosition startPosition) {
    	return src.getPosition(startPosition); 
    }
    
    public ISourcePosition getPosition() {
        return src.getPosition();
    }

    public String getCurrentLine() {
        return src.getCurrentLine();
    }

    /**
     * Parse must pass its support object for some check at bottom of
     * yylex().  Ruby does it this way as well (i.e. a little parsing
     * logic in the lexer).
     * 
     * @param parserSupport
     */
    public void setParserSupport(ParserSupport parserSupport) {
        this.parserSupport = parserSupport;
    }

    public void setEncoding(Encoding encoding) {
        this.encoding = encoding;
    }

    /**
     * Allow the parser to set the source for its lexer.
     * 
     * @param source where the lexer gets raw data
     */
    public void setSource(LexerSource source) {
        this.src = source;
    }

    public StrTerm getStrTerm() {
        return lex_strterm;
    }
    
    public void setStrTerm(StrTerm strterm) {
        this.lex_strterm = strterm;
    }

    public void resetStacks() {
        conditionState.reset();
        cmdArgumentState.reset();
    }
    
    public void setWarnings(IRubyWarnings warnings) {
        this.warnings = warnings;
    }


    public void setState(LexState state) {
        this.lex_state = state;
    }

    public StackState getCmdArgumentState() {
        return cmdArgumentState;
    }

    public StackState getConditionState() {
        return conditionState;
    }
    
    public void setValue(Object yaccValue) {
        this.yaccValue = yaccValue;
    }

    private boolean isNext_identchar() throws IOException {
        int c = src.read();
        src.unread(c);

        return c != EOF && (Character.isLetterOrDigit(c) || c == '_');
    }

    private boolean isBEG() {
        return lex_state == LexState.EXPR_BEG || lex_state == LexState.EXPR_MID ||
                lex_state == LexState.EXPR_CLASS || (!isOneEight && lex_state == LexState.EXPR_VALUE);
    }

    private boolean isARG() {
        return lex_state == LexState.EXPR_ARG || lex_state == LexState.EXPR_CMDARG;
    }

    private void determineExpressionState() {
        switch (lex_state) {
        case EXPR_FNAME: case EXPR_DOT:
            lex_state = LexState.EXPR_ARG;
            break;
        default:
            lex_state = LexState.EXPR_BEG;
            break;
        }
    }

    private Object getInteger(String value, int radix) {
        try {
            return newFixnumNode(value, radix);
        } catch (NumberFormatException e) {
            return newBignumNode(value, radix);
        }
    }

	/**
	 * @param c the character to test
	 * @return true if character is a hex value (0-9a-f)
	 */
    static final boolean isHexChar(int c) {
        return Character.isDigit(c) || ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F');
    }

    /**
	 * @param c the character to test
     * @return true if character is an octal value (0-7)
	 */
    static final boolean isOctChar(int c) {
        return '0' <= c && c <= '7';
    }
    
    /**
     * This is a valid character for an identifier?
     *
     * @param c is character to be compared
     * @return whether c is an identifier or not
     *
     * mri: is_identchar
     */
    protected boolean isIdentifierChar(int c) {
        return Character.isLetterOrDigit(c) || c == '_' || isMultiByteChar(c);
    }

    /**
     * Is this a multibyte character from a multibyte encoding?
     *
     * @param c
     * @return whether c is an multibyte char or not
     */
    protected boolean isMultiByteChar(int c) {
        return encoding.codeToMbcLength(c) != 1;
    }
    
    /**
     * What type/kind of quote are we dealing with?
     * 
     * @param c first character the the quote construct
     * @return a token that specifies the quote type
     */
    private int parseQuote(int c) throws IOException {
        int begin, end;
        boolean shortHand;
        
        // Short-hand (e.g. %{,%.,%!,... versus %Q{).
        if (!Character.isLetterOrDigit(c)) {
            begin = c;
            c = 'Q';
            shortHand = true;
        // Long-hand (e.g. %Q{}).
        } else {
            shortHand = false;
            begin = src.read();
            if (Character.isLetterOrDigit(begin) /* no mb || ismbchar(term)*/) {
                throw new SyntaxException(PID.STRING_UNKNOWN_TYPE, getPosition(), getCurrentLine(), "unknown type of %string");
            }
        }
        if (c == EOF || begin == EOF) {
            throw new SyntaxException(PID.STRING_HITS_EOF, getPosition(), getCurrentLine(), "unterminated quoted string meets end of file");
        }
        
        // Figure end-char.  '\0' is special to indicate begin=end and that no nesting?
        switch(begin) {
        case '(': end = ')'; break;
        case '[': end = ']'; break;
        case '{': end = '}'; break;
        case '<': end = '>'; break;
        default: 
            end = begin; 
            begin = '\0';
        }

        switch (c) {
        case 'Q':
            lex_strterm = new StringTerm(str_dquote, begin ,end);
            yaccValue = new Token("%"+ (shortHand ? (""+end) : ("" + c + begin)), getPosition());
            return Tokens.tSTRING_BEG;

        case 'q':
            lex_strterm = new StringTerm(str_squote, begin, end);
            yaccValue = new Token("%"+c+begin, getPosition());
            return Tokens.tSTRING_BEG;

        case 'W':
            lex_strterm = new StringTerm(str_dquote | STR_FUNC_QWORDS, begin, end);
            do {c = src.read();} while (Character.isWhitespace(c));
            src.unread(c);
            yaccValue = new Token("%"+c+begin, getPosition());
            return Tokens.tWORDS_BEG;

        case 'w':
            lex_strterm = new StringTerm(str_squote | STR_FUNC_QWORDS, begin, end);
            do {c = src.read();} while (Character.isWhitespace(c));
            src.unread(c);
            yaccValue = new Token("%"+c+begin, getPosition());
            return Tokens.tQWORDS_BEG;

        case 'x':
            lex_strterm = new StringTerm(str_xquote, begin, end);
            yaccValue = new Token("%"+c+begin, getPosition());
            return Tokens.tXSTRING_BEG;

        case 'r':
            lex_strterm = new StringTerm(str_regexp, begin, end);
            yaccValue = new Token("%"+c+begin, getPosition());
            return Tokens.tREGEXP_BEG;

        case 's':
            lex_strterm = new StringTerm(str_ssym, begin, end);
            lex_state = LexState.EXPR_FNAME;
            yaccValue = new Token("%"+c+begin, getPosition());
            return Tokens.tSYMBEG;

        default:
            throw new SyntaxException(PID.STRING_UNKNOWN_TYPE, getPosition(), getCurrentLine(),
                    "Unknown type of %string. Expected 'Q', 'q', 'w', 'x', 'r' or any non letter character, but found '" + c + "'.");
        }
    }
    
    private int hereDocumentIdentifier() throws IOException {
        int c = src.read(); 
        int term;

        int func = 0;
        if (c == '-') {
            c = src.read();
            func = STR_FUNC_INDENT;
        }
        
        ByteList markerValue;
        if (c == '\'' || c == '"' || c == '`') {
            if (c == '\'') {
                func |= str_squote;
            } else if (c == '"') {
                func |= str_dquote;
            } else {
                func |= str_xquote; 
            }

            markerValue = new ByteList();
            term = c;
            while ((c = src.read()) != EOF && c != term) {
                markerValue.append(c);
            }
            if (c == EOF) {
                throw new SyntaxException(PID.STRING_MARKER_MISSING, getPosition(), 
                        getCurrentLine(), "unterminated here document identifier");
            }	
        } else {
            if (!isIdentifierChar(c)) {
                src.unread(c);
                if ((func & STR_FUNC_INDENT) != 0) {
                    src.unread('-');
                }
                return 0;
            }
            markerValue = new ByteList();
            term = '"';
            func |= str_dquote;
            do {
                markerValue.append(c);
            } while ((c = src.read()) != EOF && isIdentifierChar(c));

            src.unread(c);
        }

        ByteList lastLine = src.readLineBytes();
        lastLine.append('\n');
        lex_strterm = new HeredocTerm(markerValue, func, lastLine);

        if (term == '`') {
            yaccValue = new Token("`", getPosition());
            return Tokens.tXSTRING_BEG;
        }
        
        yaccValue = new Token("\"", getPosition());
        // Hacky: Advance position to eat newline here....
        getPosition();
        return Tokens.tSTRING_BEG;
    }
    
    private void arg_ambiguous() {
        if (warnings.isVerbose()) warnings.warning(ID.AMBIGUOUS_ARGUMENT, getPosition(), "Ambiguous first argument; make sure.");
    }

    private int magicCommentMarker(String str, int begin) {
        int i = begin;
        int len = str.length();

        while (i < len) {
            switch (str.charAt(i)) {
                case '-':
                    if (str.charAt(i - 1) == '*' && str.charAt(i - 2) == '-') return i + 1;
                    i += 2;
                    break;
                case '*':
                    if (i + 1 >= len) return 0;

                    if (str.charAt(i + 1) != '-') {
                        i += 4;
                    } else if (str.charAt(i - 1) != '-') {
                        i += 2;
                    } else {
                        return i + 2;
                    }
                    break;
                default:
                    i += 3;
                    break;
            }
        }
        return 0;
    }

    /**
     * Read a comment up to end of line.
     * 
     * @param c last character read from lexer source
     * @return newline or eof value 
     */
    protected int readComment(int c) throws IOException {
        return src.skipUntil('\n');
        
    }
    
    /*
     * Not normally used, but is left in here since it can be useful in debugging
     * grammar and lexing problems.
     *
     */
    private void printToken(int token) {
        //System.out.print("LOC: " + support.getPosition() + " ~ ");
        
        switch (token) {
            case Tokens.yyErrorCode: System.err.print("yyErrorCode,"); break;
            case Tokens.kCLASS: System.err.print("kClass,"); break;
            case Tokens.kMODULE: System.err.print("kModule,"); break;
            case Tokens.kDEF: System.err.print("kDEF,"); break;
            case Tokens.kUNDEF: System.err.print("kUNDEF,"); break;
            case Tokens.kBEGIN: System.err.print("kBEGIN,"); break;
            case Tokens.kRESCUE: System.err.print("kRESCUE,"); break;
            case Tokens.kENSURE: System.err.print("kENSURE,"); break;
            case Tokens.kEND: System.err.print("kEND,"); break;
            case Tokens.kIF: System.err.print("kIF,"); break;
            case Tokens.kUNLESS: System.err.print("kUNLESS,"); break;
            case Tokens.kTHEN: System.err.print("kTHEN,"); break;
            case Tokens.kELSIF: System.err.print("kELSIF,"); break;
            case Tokens.kELSE: System.err.print("kELSE,"); break;
            case Tokens.kCASE: System.err.print("kCASE,"); break;
            case Tokens.kWHEN: System.err.print("kWHEN,"); break;
            case Tokens.kWHILE: System.err.print("kWHILE,"); break;
            case Tokens.kUNTIL: System.err.print("kUNTIL,"); break;
            case Tokens.kFOR: System.err.print("kFOR,"); break;
            case Tokens.kBREAK: System.err.print("kBREAK,"); break;
            case Tokens.kNEXT: System.err.print("kNEXT,"); break;
            case Tokens.kREDO: System.err.print("kREDO,"); break;
            case Tokens.kRETRY: System.err.print("kRETRY,"); break;
            case Tokens.kIN: System.err.print("kIN,"); break;
            case Tokens.kDO: System.err.print("kDO,"); break;
            case Tokens.kDO_COND: System.err.print("kDO_COND,"); break;
            case Tokens.kDO_BLOCK: System.err.print("kDO_BLOCK,"); break;
            case Tokens.kRETURN: System.err.print("kRETURN,"); break;
            case Tokens.kYIELD: System.err.print("kYIELD,"); break;
            case Tokens.kSUPER: System.err.print("kSUPER,"); break;
            case Tokens.kSELF: System.err.print("kSELF,"); break;
            case Tokens.kNIL: System.err.print("kNIL,"); break;
            case Tokens.kTRUE: System.err.print("kTRUE,"); break;
            case Tokens.kFALSE: System.err.print("kFALSE,"); break;
            case Tokens.kAND: System.err.print("kAND,"); break;
            case Tokens.kOR: System.err.print("kOR,"); break;
            case Tokens.kNOT: System.err.print("kNOT,"); break;
            case Tokens.kIF_MOD: System.err.print("kIF_MOD,"); break;
            case Tokens.kUNLESS_MOD: System.err.print("kUNLESS_MOD,"); break;
            case Tokens.kWHILE_MOD: System.err.print("kWHILE_MOD,"); break;
            case Tokens.kUNTIL_MOD: System.err.print("kUNTIL_MOD,"); break;
            case Tokens.kRESCUE_MOD: System.err.print("kRESCUE_MOD,"); break;
            case Tokens.kALIAS: System.err.print("kALIAS,"); break;
            case Tokens.kDEFINED: System.err.print("kDEFINED,"); break;
            case Tokens.klBEGIN: System.err.print("klBEGIN,"); break;
            case Tokens.klEND: System.err.print("klEND,"); break;
            case Tokens.k__LINE__: System.err.print("k__LINE__,"); break;
            case Tokens.k__FILE__: System.err.print("k__FILE__,"); break;
            case Tokens.k__ENCODING__: System.err.print("k__ENCODING__,"); break;
            case Tokens.kDO_LAMBDA: System.err.print("kDO_LAMBDA,"); break;
            case Tokens.tIDENTIFIER: System.err.print("tIDENTIFIER["+ value() + "],"); break;
            case Tokens.tFID: System.err.print("tFID[" + value() + "],"); break;
            case Tokens.tGVAR: System.err.print("tGVAR[" + value() + "],"); break;
            case Tokens.tIVAR: System.err.print("tIVAR[" + value() +"],"); break;
            case Tokens.tCONSTANT: System.err.print("tCONSTANT["+ value() +"],"); break;
            case Tokens.tCVAR: System.err.print("tCVAR,"); break;
            case Tokens.tINTEGER: System.err.print("tINTEGER,"); break;
            case Tokens.tFLOAT: System.err.print("tFLOAT,"); break;
            case Tokens.tSTRING_CONTENT: System.err.print("tSTRING_CONTENT[" + ((StrNode) value()).getValue().toString() + "],"); break;
            case Tokens.tSTRING_BEG: System.err.print("tSTRING_BEG,"); break;
            case Tokens.tSTRING_END: System.err.print("tSTRING_END,"); break;
            case Tokens.tSTRING_DBEG: System.err.print("STRING_DBEG,"); break;
            case Tokens.tSTRING_DVAR: System.err.print("tSTRING_DVAR,"); break;
            case Tokens.tXSTRING_BEG: System.err.print("tXSTRING_BEG,"); break;
            case Tokens.tREGEXP_BEG: System.err.print("tREGEXP_BEG,"); break;
            case Tokens.tREGEXP_END: System.err.print("tREGEXP_END,"); break;
            case Tokens.tWORDS_BEG: System.err.print("tWORDS_BEG,"); break;
            case Tokens.tQWORDS_BEG: System.err.print("tQWORDS_BEG,"); break;
            case Tokens.tBACK_REF: System.err.print("tBACK_REF,"); break;
            case Tokens.tBACK_REF2: System.err.print("tBACK_REF2,"); break;
            case Tokens.tNTH_REF: System.err.print("tNTH_REF,"); break;
            case Tokens.tUPLUS: System.err.print("tUPLUS"); break;
            case Tokens.tUMINUS: System.err.print("tUMINUS,"); break;
            case Tokens.tPOW: System.err.print("tPOW,"); break;
            case Tokens.tCMP: System.err.print("tCMP,"); break;
            case Tokens.tEQ: System.err.print("tEQ,"); break;
            case Tokens.tEQQ: System.err.print("tEQQ,"); break;
            case Tokens.tNEQ: System.err.print("tNEQ,"); break;
            case Tokens.tGEQ: System.err.print("tGEQ,"); break;
            case Tokens.tLEQ: System.err.print("tLEQ,"); break;
            case Tokens.tANDOP: System.err.print("tANDOP,"); break;
            case Tokens.tOROP: System.err.print("tOROP,"); break;
            case Tokens.tMATCH: System.err.print("tMATCH,"); break;
            case Tokens.tNMATCH: System.err.print("tNMATCH,"); break;
            case Tokens.tDOT: System.err.print("tDOT,"); break;
            case Tokens.tDOT2: System.err.print("tDOT2,"); break;
            case Tokens.tDOT3: System.err.print("tDOT3,"); break;
            case Tokens.tAREF: System.err.print("tAREF,"); break;
            case Tokens.tASET: System.err.print("tASET,"); break;
            case Tokens.tLSHFT: System.err.print("tLSHFT,"); break;
            case Tokens.tRSHFT: System.err.print("tRSHFT,"); break;
            case Tokens.tCOLON2: System.err.print("tCOLON2,"); break;
            case Tokens.tCOLON3: System.err.print("tCOLON3,"); break;
            case Tokens.tOP_ASGN: System.err.print("tOP_ASGN,"); break;
            case Tokens.tASSOC: System.err.print("tASSOC,"); break;
            case Tokens.tLPAREN: System.err.print("tLPAREN,"); break;
            case Tokens.tLPAREN2: System.err.print("tLPAREN2,"); break;
            case Tokens.tLPAREN_ARG: System.err.print("tLPAREN_ARG,"); break;
            case Tokens.tLBRACK: System.err.print("tLBRACK,"); break;
            case Tokens.tRBRACK: System.err.print("tRBRACK,"); break;
            case Tokens.tLBRACE: System.err.print("tLBRACE,"); break;
            case Tokens.tLBRACE_ARG: System.err.print("tLBRACE_ARG,"); break;
            case Tokens.tSTAR: System.err.print("tSTAR,"); break;
            case Tokens.tSTAR2: System.err.print("tSTAR2,"); break;
            case Tokens.tAMPER: System.err.print("tAMPER,"); break;
            case Tokens.tAMPER2: System.err.print("tAMPER2,"); break;
            case Tokens.tSYMBEG: System.err.print("tSYMBEG,"); break;
            case Tokens.tTILDE: System.err.print("tTILDE,"); break;
            case Tokens.tPERCENT: System.err.print("tPERCENT,"); break;
            case Tokens.tDIVIDE: System.err.print("tDIVIDE,"); break;
            case Tokens.tPLUS: System.err.print("tPLUS,"); break;
            case Tokens.tMINUS: System.err.print("tMINUS,"); break;
            case Tokens.tLT: System.err.print("tLT,"); break;
            case Tokens.tGT: System.err.print("tGT,"); break;
            case Tokens.tCARET: System.err.print("tCARET,"); break;
            case Tokens.tBANG: System.err.print("tBANG,"); break;
            case Tokens.tLCURLY: System.err.print("tTLCURLY,"); break;
            case Tokens.tRCURLY: System.err.print("tRCURLY,"); break;
            case Tokens.tPIPE: System.err.print("tTPIPE,"); break;
            case Tokens.tLAMBDA: System.err.print("tLAMBDA,"); break;
            case Tokens.tLAMBEG: System.err.print("tLAMBEG,"); break;
            case Tokens.tRPAREN: System.err.print("tRPAREN,"); break;
            case '\n': System.err.println("NL"); break;
            case EOF: System.out.println("EOF"); break;
            default: System.err.print("'" + (char)token + "',"); break;
        }
    }

    // DEBUGGING HELP 
    private int yylex2() throws IOException {
        int token = yylex2();
        
        printToken(token);
        
        return token;
    }

    /**
     *  Returns the next token. Also sets yyVal is needed.
     *
     *@return    Description of the Returned Value
     */
    private int yylex() throws IOException {
        int c;
        boolean spaceSeen = false;
        boolean commandState;
        
        if (lex_strterm != null) {
            int tok = lex_strterm.parseString(this, src);
            if (tok == Tokens.tSTRING_END || tok == Tokens.tREGEXP_END) {
                lex_strterm = null;
                lex_state = LexState.EXPR_END;
            }
            return tok;
        }

        commandState = commandStart;
        commandStart = false;

        loop: for(;;) {
            c = src.read();
            switch(c) {
            case '\000': /* NUL */
            case '\004': /* ^D */
            case '\032': /* ^Z */
            case EOF:	 /* end of script. */
                return EOF;
           
                /* white spaces */
            case ' ': case '\t': case '\f': case '\r':
            case '\13': /* '\v' */
                getPosition();
                spaceSeen = true;
                continue;
            case '#':		/* it's a comment */
                // FIXME: Need to detect magic_comment in 1.9 here for encoding
                if (readComment(c) == EOF) return EOF;
                    
                /* fall through */
            case '\n':
                if (isOneEight) {             	// Replace a string of newlines with a single one
                    while((c = src.read()) == '\n');
                } else {
                    switch (lex_state) {
                    case EXPR_BEG: case EXPR_FNAME: case EXPR_DOT:
                    case EXPR_CLASS: case EXPR_VALUE:
                        continue loop;
                    }

                    boolean done = false;
                    while(!done) {
                        c = src.read();

                        switch (c) {
                        case ' ': case '\t': case '\f': case '\r': case '\13': /* '\v' */
                            spaceSeen = true;
                            continue;
                        case '.': {
                            if ((c = src.read()) != '.') {
                                src.unread(c);
                                src.unread('.');

                                continue loop;
                            }
                        }
                        default:
                        case -1:		// EOF (ENEBO: After default?
                            done = true;
                        }
                    }
                }

                if (c == -1) return EOF;

                src.unread(c);
                getPosition();

                switch (lex_state) {
                case EXPR_BEG: case EXPR_FNAME: case EXPR_DOT: case EXPR_CLASS:
                    continue loop;
                }

                commandStart = true;
                lex_state = LexState.EXPR_BEG;
                return '\n';
            case '*':
                return star(spaceSeen);
            case '!':
                return bang();
            case '=':
                // documentation nodes
                if (src.wasBeginOfLine()) {
                    if (src.matchMarker(BEGIN_DOC_MARKER, false, false)) {
                        c = src.read();
                        
                        if (Character.isWhitespace(c)) {
                            // In case last next was the newline.
                            src.unread(c);
                            for (;;) {
                                c = src.read();

                                // If a line is followed by a blank line put
                                // it back.
                                while (c == '\n') {
                                    c = src.read();
                                }
                                if (c == EOF) {
                                    throw new SyntaxException(PID.STRING_HITS_EOF, getPosition(),
                                            getCurrentLine(), "embedded document meets end of file");
                                }
                                if (c != '=') continue;
                                if (src.wasBeginOfLine() && src.matchMarker(END_DOC_MARKER, false, false)) {
                                    ByteList list = src.readLineBytes();
                                    src.unread('\n');
                                    break;
                                }
                            }

                            continue;
                        }
						src.unread(c);
                    }
                }

                determineExpressionState();

                c = src.read();
                if (c == '=') {
                    c = src.read();
                    if (c == '=') {
                        yaccValue = new Token("===", getPosition());
                        return Tokens.tEQQ;
                    }
                    src.unread(c);
                    yaccValue = new Token("==", getPosition());
                    return Tokens.tEQ;
                }
                if (c == '~') {
                    yaccValue = new Token("=~", getPosition());
                    return Tokens.tMATCH;
                } else if (c == '>') {
                    yaccValue = new Token("=>", getPosition());
                    return Tokens.tASSOC;
                }
                src.unread(c);
                yaccValue = new Token("=", getPosition());
                return '=';
                
            case '<':
                return lessThan(spaceSeen);
            case '>':
                return greaterThan();
            case '"':
                return doubleQuote();
            case '`':
                return backtick(commandState);
            case '\'':
                return singleQuote();
            case '?':
                return questionMark();
            case '&':
                return ampersand(spaceSeen);
            case '|':
                return pipe();
            case '+':
                return plus(spaceSeen);
            case '-':
                return minus(spaceSeen);
            case '.':
                return dot();
            case '0' : case '1' : case '2' : case '3' : case '4' :
            case '5' : case '6' : case '7' : case '8' : case '9' :
                return parseNumber(c);
            case ')':
                return rightParen();
            case ']':
                return rightBracket();
            case '}':
                return rightCurly();
            case ':':
                return colon(spaceSeen);
            case '/':
                return slash(spaceSeen);
            case '^':
                return caret();
            case ';':
                commandStart = true;
                if (!isOneEight) {
                    lex_state = LexState.EXPR_BEG;
                    yaccValue = new Token(";", getPosition());
                    return ';';
                }
            case ',':
                return comma(c);
            case '~':
                return tilde();
            case '(':
                return leftParen(spaceSeen);
            case '[':
                return leftBracket(spaceSeen);
            case '{':
            	return leftCurly();
            case '\\':
                c = src.read();
                if (c == '\n') {
                    spaceSeen = true;
                    continue;
                }
                src.unread(c);
                yaccValue = new Token("\\", getPosition());
                return '\\';
            case '%':
                return percent(spaceSeen);
            case '$':
                return dollar();
            case '@':
                return at();
            case '_':
                if (src.wasBeginOfLine() && src.matchMarker(END_MARKER, false, true)) {
                	parserSupport.getResult().setEndOffset(src.getOffset());
                    return EOF;
                }
                return identifier(c, commandState);
            default:
                return identifier(c, commandState);
            }
        }
    }

    private int identifierToken(LexState last_state, int result, String value) {

        if (result == Tokens.tIDENTIFIER && last_state != LexState.EXPR_DOT &&
                parserSupport.getCurrentScope().isDefined(value) >= 0) {
            lex_state = LexState.EXPR_END;
        }

        yaccValue = new Token(value, result, getPosition());
        return result;
    }

    private int getIdentifier(int c) throws IOException {
        do {
            tokenBuffer.append((char) c);
            /* no special multibyte character handling is needed in Java
             * if (ismbchar(c)) {
                int i, len = mbclen(c)-1;

                for (i = 0; i < len; i++) {
                    c = src.read();
                    tokenBuffer.append(c);
                }
            }*/
            c = src.read();
        } while (isIdentifierChar(c));
        
        return c;
    }
    
    private int ampersand(boolean spaceSeen) throws IOException {
        int c = src.read();
        
        switch (c) {
        case '&':
            lex_state = LexState.EXPR_BEG;
            if ((c = src.read()) == '=') {
                yaccValue = new Token("&&", getPosition());
                lex_state = LexState.EXPR_BEG;
                return Tokens.tOP_ASGN;
            }
            src.unread(c);
            yaccValue = new Token("&&", getPosition());
            return Tokens.tANDOP;
        case '=':
            yaccValue = new Token("&", getPosition());
            lex_state = LexState.EXPR_BEG;
            return Tokens.tOP_ASGN;
        }
        src.unread(c);
        
        //tmpPosition is required because getPosition()'s side effects.
        //if the warning is generated, the getPosition() on line 954 (this line + 18) will create
        //a wrong position if the "inclusive" flag is not set.
        ISourcePosition tmpPosition = getPosition();
        if (isARG() && spaceSeen && !Character.isWhitespace(c)) {
            if (warnings.isVerbose()) warnings.warning(ID.ARGUMENT_AS_PREFIX, tmpPosition, "`&' interpreted as argument prefix", "&");
            c = Tokens.tAMPER;
        } else if (isBEG()) {
            c = Tokens.tAMPER;
        } else {
            c = Tokens.tAMPER2;
        }
        
        determineExpressionState();
        
        yaccValue = new Token("&", tmpPosition);
        return c;
    }
    
    private int at() throws IOException {
        int c = src.read();
        int result;
        tokenBuffer.setLength(0);
        tokenBuffer.append('@');
        if (c == '@') {
            tokenBuffer.append('@');
            c = src.read();
            result = Tokens.tCVAR;
        } else {
            result = Tokens.tIVAR;                    
        }
        
        if (Character.isDigit(c)) {
            if (tokenBuffer.length() == 1) {
                throw new SyntaxException(PID.IVAR_BAD_NAME, getPosition(), getCurrentLine(),
                        "`@" + c + "' is not allowed as an instance variable name");
            }
            throw new SyntaxException(PID.CVAR_BAD_NAME, getPosition(), getCurrentLine(),
                    "`@@" + c + "' is not allowed as a class variable name");
        }
        
        if (!isIdentifierChar(c)) {
            src.unread(c);
            yaccValue = new Token("@", getPosition());
            return '@';
        }

        c = getIdentifier(c);
        src.unread(c);

        LexState last_state = lex_state;
        lex_state = LexState.EXPR_END;

        return identifierToken(last_state, result, tokenBuffer.toString().intern());        
    }
    
    private int backtick(boolean commandState) throws IOException {
        yaccValue = new Token("`", getPosition());

        switch (lex_state) {
        case EXPR_FNAME:
            lex_state = LexState.EXPR_END;
            
            return Tokens.tBACK_REF2;
        case EXPR_DOT:
            lex_state = commandState ? LexState.EXPR_CMDARG : LexState.EXPR_ARG;

            return Tokens.tBACK_REF2;
        default:
            lex_strterm = new StringTerm(str_xquote, '\0', '`');
        
            return Tokens.tXSTRING_BEG;
        }
    }
    
    private int bang() throws IOException {
        int c = src.read();

        if (!isOneEight && (lex_state == LexState.EXPR_FNAME || lex_state == LexState.EXPR_DOT)) {
            lex_state = LexState.EXPR_ARG;
            if (c == '@') {
                yaccValue = new Token("!",getPosition());
                return Tokens.tBANG;
            }
        } else {
            lex_state = LexState.EXPR_BEG;
        }
        
        switch (c) {
        case '=':
            yaccValue = new Token("!=",getPosition());
            
            return Tokens.tNEQ;
        case '~':
            yaccValue = new Token("!~",getPosition());
            
            return Tokens.tNMATCH;
        default: // Just a plain bang
            src.unread(c);
            yaccValue = new Token("!",getPosition());
            
            return Tokens.tBANG;
        }
    }
    
    private int caret() throws IOException {
        int c = src.read();
        if (c == '=') {
            lex_state = LexState.EXPR_BEG;
            yaccValue = new Token("^", getPosition());
            return Tokens.tOP_ASGN;
        }
        
        determineExpressionState();
        
        src.unread(c);
        yaccValue = new Token("^", getPosition());
        return Tokens.tCARET;
    }

    private int colon(boolean spaceSeen) throws IOException {
        int c = src.read();
        
        if (c == ':') {
            if (isBEG() || lex_state == LexState.EXPR_CLASS || (isARG() && spaceSeen)) {
                lex_state = LexState.EXPR_BEG;
                yaccValue = new Token("::", getPosition());
                return Tokens.tCOLON3;
            }
            lex_state = LexState.EXPR_DOT;
            yaccValue = new Token(":",getPosition());
            return Tokens.tCOLON2;
        }

        if (lex_state == LexState.EXPR_END || lex_state == LexState.EXPR_ENDARG || Character.isWhitespace(c)) {
            src.unread(c);
            lex_state = LexState.EXPR_BEG;
            yaccValue = new Token(":",getPosition());
            return ':';
        }
        
        switch (c) {
        case '\'':
            lex_strterm = new StringTerm(str_ssym, '\0', c);
            break;
        case '"':
            lex_strterm = new StringTerm(str_dsym, '\0', c);
            break;
        default:
            src.unread(c);
            break;
        }
        
        lex_state = LexState.EXPR_FNAME;
        yaccValue = new Token(":", getPosition());
        return Tokens.tSYMBEG;
    }

    private int comma(int c) throws IOException {
        lex_state = LexState.EXPR_BEG;
        yaccValue = new Token(",", getPosition());
        
        return c;
    }
    
    private int dollar() throws IOException {
        LexState last_state = lex_state;
        lex_state = LexState.EXPR_END;
        int c = src.read();
        
        switch (c) {
        case '_':       /* $_: last read line string */
            c = src.read();
            if (isIdentifierChar(c)) {
                tokenBuffer.setLength(0);
                tokenBuffer.append("$_");
                c = getIdentifier(c);
                src.unread(c);
                last_state = lex_state;
                lex_state = LexState.EXPR_END;

                return identifierToken(last_state, Tokens.tGVAR, tokenBuffer.toString().intern());
            }
            src.unread(c);
            c = '_';
            
            // fall through
        case '~':       /* $~: match-data */
        case '*':       /* $*: argv */
        case '$':       /* $$: pid */
        case '?':       /* $?: last status */
        case '!':       /* $!: error string */
        case '@':       /* $@: error position */
        case '/':       /* $/: input record separator */
        case '\\':      /* $\: output record separator */
        case ';':       /* $;: field separator */
        case ',':       /* $,: output field separator */
        case '.':       /* $.: last read line number */
        case '=':       /* $=: ignorecase */
        case ':':       /* $:: load path */
        case '<':       /* $<: reading filename */
        case '>':       /* $>: default output handle */
        case '\"':      /* $": already loaded files */
            yaccValue = new Token("$" + (char) c, Tokens.tGVAR, getPosition());
            return Tokens.tGVAR;

        case '-':
            tokenBuffer.setLength(0);
            tokenBuffer.append('$');
            tokenBuffer.append((char) c);
            c = src.read();
            if (isIdentifierChar(c)) {
                tokenBuffer.append((char) c);
            } else {
                src.unread(c);
            }
            yaccValue = new Token(tokenBuffer.toString(), Tokens.tGVAR, getPosition());
            /* xxx shouldn't check if valid option variable */
            return Tokens.tGVAR;

        case '&':       /* $&: last match */
        case '`':       /* $`: string before last match */
        case '\'':      /* $': string after last match */
        case '+':       /* $+: string matches last paren. */
            // Explicit reference to these vars as symbols...
            if (last_state == LexState.EXPR_FNAME) {
                yaccValue = new Token("$" + (char) c, Tokens.tGVAR, getPosition());
                return Tokens.tGVAR;
            }
            
            yaccValue = new BackRefNode(getPosition(), c);
            return Tokens.tBACK_REF;

        case '1': case '2': case '3': case '4': case '5': case '6':
        case '7': case '8': case '9':
            tokenBuffer.setLength(0);
            tokenBuffer.append('$');
            do {
                tokenBuffer.append((char) c);
                c = src.read();
            } while (Character.isDigit(c));
            src.unread(c);
            if (last_state == LexState.EXPR_FNAME) {
                yaccValue = new Token(tokenBuffer.toString(), Tokens.tGVAR, getPosition());
                return Tokens.tGVAR;
            }
            
            yaccValue = new NthRefNode(getPosition(), Integer.parseInt(tokenBuffer.substring(1)));
            return Tokens.tNTH_REF;
        case '0':
            lex_state = LexState.EXPR_END;

            return identifierToken(last_state, Tokens.tGVAR, ("$" + (char) c).intern());
        default:
            if (!isIdentifierChar(c)) {
                src.unread(c);
                yaccValue = new Token("$", getPosition());
                return '$';
            }
        
            // $blah
            tokenBuffer.setLength(0);
            tokenBuffer.append('$');
            int d = getIdentifier(c);
            src.unread(d);
            last_state = lex_state;
            lex_state = LexState.EXPR_END;

            return identifierToken(last_state, Tokens.tGVAR, tokenBuffer.toString().intern());
        }
    }
    
    private int dot() throws IOException {
        int c;
        
        lex_state = LexState.EXPR_BEG;
        if ((c = src.read()) == '.') {
            if ((c = src.read()) == '.') {
                yaccValue = new Token("...", getPosition());
                return Tokens.tDOT3;
            }
            src.unread(c);
            yaccValue = new Token("..", getPosition());
            return Tokens.tDOT2;
        }
        
        src.unread(c);
        if (Character.isDigit(c)) {
            throw new SyntaxException(PID.FLOAT_MISSING_ZERO, getPosition(), getCurrentLine(),
                    "no .<digit> floating literal anymore; put 0 before dot");
        }
        
        lex_state = LexState.EXPR_DOT;
        yaccValue = new Token(".", getPosition());
        return Tokens.tDOT;
    }
    
    private int doubleQuote() throws IOException {
        lex_strterm = new StringTerm(str_dquote, '\0', '"');
        yaccValue = new Token("\"", getPosition());

        return Tokens.tSTRING_BEG;
    }
    
    private int greaterThan() throws IOException {
        determineExpressionState();

        int c = src.read();

        switch (c) {
        case '=':
            yaccValue = new Token(">=", getPosition());
            
            return Tokens.tGEQ;
        case '>':
            if ((c = src.read()) == '=') {
                lex_state = LexState.EXPR_BEG;
                yaccValue = new Token(">>", getPosition());
                return Tokens.tOP_ASGN;
            }
            src.unread(c);
            
            yaccValue = new Token(">>", getPosition());
            return Tokens.tRSHFT;
        default:
            src.unread(c);
            yaccValue = new Token(">", getPosition());
            return Tokens.tGT;
        }
    }
    
    private int identifier(int c, boolean commandState) throws IOException {
        if (!isIdentifierChar(c)) {
            String badChar = "\\" + Integer.toOctalString(c & 0xff);
            throw new SyntaxException(PID.CHARACTER_BAD, getPosition(), "Invalid char `" + badChar +
                    "' ('" + (char) c + "') in expression", badChar);
        }
    
        tokenBuffer.setLength(0);
        int first = c;

        c = getIdentifier(c);
        boolean lastBangOrPredicate = false;

        // methods 'foo!' and 'foo?' are possible but if followed by '=' it is relop
        if (c == '!' || c == '?') {
            if (!src.peek('=')) {
                lastBangOrPredicate = true;
                tokenBuffer.append((char) c);
            } else {
                src.unread(c);
            }
        } else {
            src.unread(c);
        }
        
        int result = 0;

        LexState last_state = lex_state;
        if (lastBangOrPredicate) {
            result = Tokens.tFID;
        } else {
            if (lex_state == LexState.EXPR_FNAME) {
                if ((c = src.read()) == '=') { 
                    int c2 = src.read();

                    if (c2 != '~' && c2 != '>' &&
                            (c2 != '=' || (c2 == '\n' && src.peek('>')))) {
                        result = Tokens.tIDENTIFIER;
                        tokenBuffer.append((char) c);
                        src.unread(c2);
                    } else { 
                        src.unread(c2);
                        src.unread(c);
                    }
                } else {
                    src.unread(c);
                }
            }
            if (result == 0 && Character.isUpperCase(first)) {
                result = Tokens.tCONSTANT;
            } else {
                result = Tokens.tIDENTIFIER;
            }
        }

        String tempVal = tokenBuffer.toString().intern();

	    if (!isOneEight && ((lex_state == LexState.EXPR_BEG && !commandState) ||
                lex_state == LexState.EXPR_ARG || lex_state == LexState.EXPR_CMDARG)) {
            int c2 = src.read();
            if (c2 == ':' && !src.peek(':')) {
                src.unread(c2);
                lex_state = LexState.EXPR_BEG;
                src.read();
                yaccValue = new Token(tempVal, getPosition());
                return Tokens.tLABEL;
            }
            src.unread(c2);
        }

        if (lex_state != LexState.EXPR_DOT) {
            /* See if it is a reserved word.  */
            //Keyword keyword = Keyword.getKeyword(tempVal, tempVal.length());
            Keyword keyword = getKeyword(tempVal);
            if (keyword != null && (keyword != Keyword.__ENCODING__ || !isOneEight)) {
                // enum lex_state
                LexState state = lex_state;

                lex_state = keyword.state;
                if (state == LexState.EXPR_FNAME) {
                    yaccValue = new Token(keyword.name, getPosition());
                } else {
                    yaccValue = new Token(tempVal, getPosition());
                    if (keyword.id0 == Tokens.kDO) {
                        commandStart = true;
                        
                        if (!isOneEight && leftParenBegin > 0 && leftParenBegin == parenNest) {
                            leftParenBegin = 0;
                            parenNest--;
                            return Tokens.kDO_LAMBDA;
                        }

                        if (conditionState.isInState()) return Tokens.kDO_COND;

                        if (state != LexState.EXPR_CMDARG && cmdArgumentState.isInState()) {
                            return Tokens.kDO_BLOCK;
                        }
                        if (state == LexState.EXPR_ENDARG || (!isOneEight && state == LexState.EXPR_BEG)) return Tokens.kDO_BLOCK;
                        return Tokens.kDO;
                    }
                }

                if (state == LexState.EXPR_BEG || (!isOneEight && state == LexState.EXPR_VALUE)) return keyword.id0;

                if (keyword.id0 != keyword.id1) lex_state = LexState.EXPR_BEG;

                return keyword.id1;
            }
        }

        if (isBEG() || lex_state == LexState.EXPR_DOT || isARG()) {
            lex_state = commandState ? LexState.EXPR_CMDARG : LexState.EXPR_ARG;
        } else {
            lex_state = LexState.EXPR_END;
        }
        
        return identifierToken(last_state, result, tempVal);
    }

    private int leftBracket(boolean spaceSeen) throws IOException {
        parenNest++;
        int c = '[';
        if (lex_state == LexState.EXPR_FNAME || lex_state == LexState.EXPR_DOT) {
            lex_state = LexState.EXPR_ARG;
            
            if ((c = src.read()) == ']') {
                if (src.peek('=')) {
                    c = src.read();
                    yaccValue = new Token("[]=", getPosition());
                    return Tokens.tASET;
                }
                yaccValue = new Token("[]", getPosition());
                return Tokens.tAREF;
            }
            src.unread(c);
            yaccValue = new Token("[", getPosition());
            return '[';
        } else if (isBEG() || (isARG() && spaceSeen)) {
            c = Tokens.tLBRACK;
        }

        lex_state = LexState.EXPR_BEG;
        conditionState.stop();
        cmdArgumentState.stop();
        yaccValue = new Token("[", getPosition());
        return c;
    }
    
    private int leftCurly() {
        char c;
        if (!isOneEight && leftParenBegin > 0 && leftParenBegin == parenNest) {
            lex_state = LexState.EXPR_BEG;
            leftParenBegin = 0;
            parenNest--;
            yaccValue = new Token("{", getPosition());
            return Tokens.tLAMBEG;
        }

        if (isARG() || lex_state == LexState.EXPR_END) { // block (primary)
            c = Tokens.tLCURLY;
        } else if (lex_state == LexState.EXPR_ENDARG) { // block (expr)
            c = Tokens.tLBRACE_ARG;
        } else { // hash
            c = Tokens.tLBRACE;
        }

        conditionState.stop();
        cmdArgumentState.stop();
        lex_state = LexState.EXPR_BEG;
        
        yaccValue = new Token("{", getPosition());
        if (!isOneEight && c != Tokens.tLBRACE) commandStart = true;
        return c;
    }

    private int leftParen(boolean spaceSeen) throws IOException {
        if (isOneEight) commandStart = true;

        int result = Tokens.tLPAREN2;
        if (isBEG()) {
            result = Tokens.tLPAREN;
        } else if (spaceSeen) {
            // ENEBO: 1.9 is IS_ARG, but we need to break apart for 1.8 support.
            if (lex_state == LexState.EXPR_CMDARG) {
                result = Tokens.tLPAREN_ARG;
            } else if (lex_state == LexState.EXPR_ARG) {
                if (isOneEight) {
                    warnings.warn(ID.ARGUMENT_EXTRA_SPACE, getPosition(), "don't put space before argument parentheses");
                    result = Tokens.tLPAREN2;
                } else {
                    result = Tokens.tLPAREN_ARG;
                }
            }
        }

        parenNest++;
        conditionState.stop();
        cmdArgumentState.stop();
        lex_state = LexState.EXPR_BEG;
        
        yaccValue = new Token("(", getPosition());
        return result;
    }
    
    private int lessThan(boolean spaceSeen) throws IOException {
        int c = src.read();
        if (c == '<' && lex_state != LexState.EXPR_END && lex_state != LexState.EXPR_DOT &&
                lex_state != LexState.EXPR_ENDARG && lex_state != LexState.EXPR_CLASS &&
                (!isARG() || spaceSeen)) {
            int tok = hereDocumentIdentifier();
            
            if (tok != 0) return tok;
        }
        
        determineExpressionState();
        
        switch (c) {
        case '=':
            if ((c = src.read()) == '>') {
                yaccValue = new Token("<=>", getPosition());
                return Tokens.tCMP;
            }
            src.unread(c);
            yaccValue = new Token("<=", getPosition());
            return Tokens.tLEQ;
        case '<':
            if ((c = src.read()) == '=') {
                lex_state = LexState.EXPR_BEG;
                yaccValue = new Token("<<", getPosition());
                return Tokens.tOP_ASGN;
            }
            src.unread(c);
            yaccValue = new Token("<<", getPosition());
            return Tokens.tLSHFT;
        default:
            yaccValue = new Token("<", getPosition());
            src.unread(c);
            return Tokens.tLT;
        }
    }
    
    private int minus(boolean spaceSeen) throws IOException {
        int c = src.read();
        
        if (lex_state == LexState.EXPR_FNAME || lex_state == LexState.EXPR_DOT) {
            lex_state = LexState.EXPR_ARG;
            if (c == '@') {
                yaccValue = new Token("-@", getPosition());
                return Tokens.tUMINUS;
            }
            src.unread(c);
            yaccValue = new Token("-", getPosition());
            return Tokens.tMINUS;
        }
        if (c == '=') {
            lex_state = LexState.EXPR_BEG;
            yaccValue = new Token("-", getPosition());
            return Tokens.tOP_ASGN;
        }
        if (!isOneEight && c == '>') {
            lex_state = LexState.EXPR_ARG;
            yaccValue = new Token("->", getPosition());
            return Tokens.tLAMBDA;
        }
        if (isBEG() || (isARG() && spaceSeen && !Character.isWhitespace(c))) {
            if (isARG()) arg_ambiguous();
            lex_state = LexState.EXPR_BEG;
            src.unread(c);
            yaccValue = new Token("-", getPosition());
            if (Character.isDigit(c)) {
                return Tokens.tUMINUS_NUM;
            }
            return Tokens.tUMINUS;
        }
        lex_state = LexState.EXPR_BEG;
        src.unread(c);
        yaccValue = new Token("-", getPosition());
        return Tokens.tMINUS;
    }

    private int percent(boolean spaceSeen) throws IOException {
        if (isBEG()) return parseQuote(src.read());

        int c = src.read();

        if (c == '=') {
            lex_state = LexState.EXPR_BEG;
            yaccValue = new Token("%", getPosition());
            return Tokens.tOP_ASGN;
        }
        
        if (isARG() && spaceSeen && !Character.isWhitespace(c)) return parseQuote(c);
        
        determineExpressionState();
        
        src.unread(c);
        yaccValue = new Token("%", getPosition());
        return Tokens.tPERCENT;
    }

    private int pipe() throws IOException {
        int c = src.read();
        
        switch (c) {
        case '|':
            lex_state = LexState.EXPR_BEG;
            if ((c = src.read()) == '=') {
                lex_state = LexState.EXPR_BEG;
                yaccValue = new Token("||", getPosition());
                return Tokens.tOP_ASGN;
            }
            src.unread(c);
            yaccValue = new Token("||", getPosition());
            return Tokens.tOROP;
        case '=':
            lex_state = LexState.EXPR_BEG;
            yaccValue = new Token("|", getPosition());
            return Tokens.tOP_ASGN;
        default:
            determineExpressionState();
            
            src.unread(c);
            yaccValue = new Token("|", getPosition());
            return Tokens.tPIPE;
        }
    }
    
    private int plus(boolean spaceSeen) throws IOException {
        int c = src.read();
        if (lex_state == LexState.EXPR_FNAME || lex_state == LexState.EXPR_DOT) {
            lex_state = LexState.EXPR_ARG;
            if (c == '@') {
                yaccValue = new Token("+@", getPosition());
                return Tokens.tUPLUS;
            }
            src.unread(c);
            yaccValue = new Token("+", getPosition());
            return Tokens.tPLUS;
        }
        
        if (c == '=') {
            lex_state = LexState.EXPR_BEG;
            yaccValue = new Token("+", getPosition());
            return Tokens.tOP_ASGN;
        }
        
        if (isBEG() || (isARG() && spaceSeen && !Character.isWhitespace(c))) {
            if (isARG()) arg_ambiguous();
            lex_state = LexState.EXPR_BEG;
            src.unread(c);
            if (Character.isDigit(c)) {
                c = '+';
                return parseNumber(c);
            }
            yaccValue = new Token("+", getPosition());
            return Tokens.tUPLUS;
        }
        
        lex_state = LexState.EXPR_BEG;
        src.unread(c);
        yaccValue = new Token("+", getPosition());
        return Tokens.tPLUS;
    }
    
    private int questionMark() throws IOException {
        int c;
        
        if (lex_state == LexState.EXPR_END || lex_state == LexState.EXPR_ENDARG) {
            lex_state = isOneEight ? LexState.EXPR_BEG : LexState.EXPR_VALUE;
            yaccValue = new Token("?",getPosition());
            return '?';
        }
        
        c = src.read();
        if (c == EOF) throw new SyntaxException(PID.INCOMPLETE_CHAR_SYNTAX, getPosition(), 
                getCurrentLine(), "incomplete character syntax");

        if (Character.isWhitespace(c)){
            if (!isARG()) {
                int c2 = 0;
                switch (c) {
                case ' ':
                    c2 = 's';
                    break;
                case '\n':
                    c2 = 'n';
                    break;
                case '\t':
                    c2 = 't';
                    break;
                        /* What is \v in C?
                    case '\v':
                        c2 = 'v';
                        break;
                        */
                case '\r':
                    c2 = 'r';
                    break;
                case '\f':
                    c2 = 'f';
                    break;
                }
                if (c2 != 0) {
                    warnings.warn(ID.INVALID_CHAR_SEQUENCE, getPosition(), "invalid character syntax; use ?\\" + c2);
                }
            }
            src.unread(c);
            lex_state = isOneEight ? LexState.EXPR_BEG : LexState.EXPR_VALUE;
            yaccValue = new Token("?", getPosition());
            return '?';
            /*} else if (ismbchar(c)) { // ruby - we don't support them either?
                rb_warn("multibyte character literal not supported yet; use ?\\" + c);
                support.unread(c);
                lexState = LexState.EXPR_BEG;
                return '?';*/
        } else if (isIdentifierChar(c) && !src.peek('\n') && isNext_identchar()) {
            src.unread(c);
            lex_state = isOneEight ? LexState.EXPR_BEG : LexState.EXPR_VALUE;
            yaccValue = new Token("?", getPosition());
            return '?';
        } else if (c == '\\') {
            // FIXME: peek('u') utf8 stuff for 1.9
            c = readEscape();
        }
        
        c &= 0xff;
        lex_state = LexState.EXPR_END;
        if (isOneEight) {
            yaccValue = new FixnumNode(getPosition(), c);
        } else {
            // TODO: this isn't handling multibyte yet
            ByteList oneCharBL = new ByteList(1);
            oneCharBL.append(c);
            yaccValue = new StrNode(getPosition(), oneCharBL);
        }
        // TODO: This should be something else like a tCHAR
        return Tokens.tINTEGER;
    }
    
    private int rightBracket() {
        parenNest--;
        conditionState.restart();
        cmdArgumentState.restart();
        lex_state = isOneEight ? LexState.EXPR_END : LexState.EXPR_ENDARG;
        yaccValue = new Token(")", getPosition());
        return Tokens.tRBRACK;
    }

    private int rightCurly() {
        conditionState.restart();
        cmdArgumentState.restart();
        lex_state = isOneEight ? LexState.EXPR_END : LexState.EXPR_ENDARG;
        yaccValue = new Token("}",getPosition());
        return Tokens.tRCURLY;
    }

    private int rightParen() {
        parenNest--;
        conditionState.restart();
        cmdArgumentState.restart();
        lex_state = LexState.EXPR_END;
        yaccValue = new Token(")", getPosition());
        return Tokens.tRPAREN;
    }
    
    private int singleQuote() throws IOException {
        lex_strterm = new StringTerm(str_squote, '\0', '\'');
        yaccValue = new Token("'", getPosition());

        return Tokens.tSTRING_BEG;
    }
    
    private int slash(boolean spaceSeen) throws IOException {
        if (isBEG()) {
            lex_strterm = new StringTerm(str_regexp, '\0', '/');
            yaccValue = new Token("/",getPosition());
            return Tokens.tREGEXP_BEG;
        }
        
        int c = src.read();
        
        if (c == '=') {
            yaccValue = new Token("/", getPosition());
            lex_state = LexState.EXPR_BEG;
            return Tokens.tOP_ASGN;
        }
        src.unread(c);
        if (isARG() && spaceSeen) {
            if (!Character.isWhitespace(c)) {
                arg_ambiguous();
                lex_strterm = new StringTerm(str_regexp, '\0', '/');
                yaccValue = new Token("/",getPosition());
                return Tokens.tREGEXP_BEG;
            }
        }
        
        determineExpressionState();
        
        yaccValue = new Token("/", getPosition());
        return Tokens.tDIVIDE;
    }

    private int star(boolean spaceSeen) throws IOException {
        int c = src.read();
        
        switch (c) {
        case '*':
            if ((c = src.read()) == '=') {
                lex_state = LexState.EXPR_BEG;
                yaccValue = new Token("**", getPosition());
                return Tokens.tOP_ASGN;
            }
            src.unread(c);
            yaccValue = new Token("**", getPosition());
            c = Tokens.tPOW;
            break;
        case '=':
            lex_state = LexState.EXPR_BEG;
            yaccValue = new Token("*", getPosition());
            return Tokens.tOP_ASGN;
        default:
            src.unread(c);
            if (isARG() && spaceSeen && !Character.isWhitespace(c)) {
                if (warnings.isVerbose()) warnings.warning(ID.ARGUMENT_AS_PREFIX, getPosition(), "`*' interpreted as argument prefix", "*");
                c = Tokens.tSTAR;
            } else if (isBEG()) {
                c = Tokens.tSTAR;
            } else {
                c = Tokens.tSTAR2;
            }
            yaccValue = new Token("*", getPosition());
        }
        
        determineExpressionState();
        return c;
    }

    private int tilde() throws IOException {
        int c;
        
        if (lex_state == LexState.EXPR_FNAME || lex_state == LexState.EXPR_DOT) {
            if ((c = src.read()) != '@') src.unread(c);
            lex_state = LexState.EXPR_ARG;
        } else {
            lex_state = LexState.EXPR_BEG;
        }
        
        yaccValue = new Token("~", getPosition());
        return Tokens.tTILDE;
    }

    /**
     *  Parse a number from the input stream.
     *
     *@param c The first character of the number.
     *@return A int constant wich represents a token.
     */
    private int parseNumber(int c) throws IOException {
        lex_state = LexState.EXPR_END;

        tokenBuffer.setLength(0);

        if (c == '-') {
        	tokenBuffer.append((char) c);
            c = src.read();
        } else if (c == '+') {
        	// We don't append '+' since Java number parser gets confused
            c = src.read();
        }
        
        int nondigit = 0;

        if (c == '0') {
            int startLen = tokenBuffer.length();

            switch (c = src.read()) {
                case 'x' :
                case 'X' : //  hexadecimal
                    c = src.read();
                    if (isHexChar(c)) {
                        for (;; c = src.read()) {
                            if (c == '_') {
                                if (nondigit != '\0') break;
                                nondigit = c;
                            } else if (isHexChar(c)) {
                                nondigit = '\0';
                                tokenBuffer.append((char) c);
                            } else {
                                break;
                            }
                        }
                    }
                    src.unread(c);

                    if (tokenBuffer.length() == startLen) {
                        throw new SyntaxException(PID.BAD_HEX_NUMBER, getPosition(), 
                                getCurrentLine(), "Hexadecimal number without hex-digits.");
                    } else if (nondigit != '\0') {
                        throw new SyntaxException(PID.TRAILING_UNDERSCORE_IN_NUMBER, getPosition(),
                                getCurrentLine(), "Trailing '_' in number.");
                    }
                    yaccValue = getInteger(tokenBuffer.toString(), 16);
                    return Tokens.tINTEGER;
                case 'b' :
                case 'B' : // binary
                    c = src.read();
                    if (c == '0' || c == '1') {
                        for (;; c = src.read()) {
                            if (c == '_') {
                                if (nondigit != '\0') break;
								nondigit = c;
                            } else if (c == '0' || c == '1') {
                                nondigit = '\0';
                                tokenBuffer.append((char) c);
                            } else {
                                break;
                            }
                        }
                    }
                    src.unread(c);

                    if (tokenBuffer.length() == startLen) {
                        throw new SyntaxException(PID.EMPTY_BINARY_NUMBER, getPosition(),
                                getCurrentLine(), "Binary number without digits.");
                    } else if (nondigit != '\0') {
                        throw new SyntaxException(PID.TRAILING_UNDERSCORE_IN_NUMBER, getPosition(),
                                getCurrentLine(), "Trailing '_' in number.");
                    }
                    yaccValue = getInteger(tokenBuffer.toString(), 2);
                    return Tokens.tINTEGER;
                case 'd' :
                case 'D' : // decimal
                    c = src.read();
                    if (Character.isDigit(c)) {
                        for (;; c = src.read()) {
                            if (c == '_') {
                                if (nondigit != '\0') break;
								nondigit = c;
                            } else if (Character.isDigit(c)) {
                                nondigit = '\0';
                                tokenBuffer.append((char) c);
                            } else {
                                break;
                            }
                        }
                    }
                    src.unread(c);

                    if (tokenBuffer.length() == startLen) {
                        throw new SyntaxException(PID.EMPTY_BINARY_NUMBER, getPosition(), 
                                getCurrentLine(), "Binary number without digits.");
                    } else if (nondigit != '\0') {
                        throw new SyntaxException(PID.TRAILING_UNDERSCORE_IN_NUMBER, getPosition(),
                                getCurrentLine(), "Trailing '_' in number.");
                    }
                    yaccValue = getInteger(tokenBuffer.toString(), 10);
                    return Tokens.tINTEGER;
                case 'o':
                case 'O':
                    c = src.read();
                case '0': case '1': case '2': case '3': case '4': //Octal
                case '5': case '6': case '7': case '_': 
                    for (;; c = src.read()) {
                        if (c == '_') {
                            if (nondigit != '\0') break;

							nondigit = c;
                        } else if (c >= '0' && c <= '7') {
                            nondigit = '\0';
                            tokenBuffer.append((char) c);
                        } else {
                            break;
                        }
                    }
                    if (tokenBuffer.length() > startLen) {
                        src.unread(c);

                        if (nondigit != '\0') {
                            throw new SyntaxException(PID.TRAILING_UNDERSCORE_IN_NUMBER, 
                                    getPosition(), getCurrentLine(), "Trailing '_' in number.");
                        }

                        yaccValue = getInteger(tokenBuffer.toString(), 8);
                        return Tokens.tINTEGER;
                    }
                case '8' :
                case '9' :
                    throw new SyntaxException(PID.BAD_OCTAL_DIGIT, getPosition(),
                            getCurrentLine(), "Illegal octal digit.");
                case '.' :
                case 'e' :
                case 'E' :
                	tokenBuffer.append('0');
                    break;
                default :
                    src.unread(c);
                    yaccValue = new FixnumNode(getPosition(), 0);
                    return Tokens.tINTEGER;
            }
        }

        boolean seen_point = false;
        boolean seen_e = false;

        for (;; c = src.read()) {
            switch (c) {
                case '0' :
                case '1' :
                case '2' :
                case '3' :
                case '4' :
                case '5' :
                case '6' :
                case '7' :
                case '8' :
                case '9' :
                    nondigit = '\0';
                    tokenBuffer.append((char) c);
                    break;
                case '.' :
                    if (nondigit != '\0') {
                        src.unread(c);
                        throw new SyntaxException(PID.TRAILING_UNDERSCORE_IN_NUMBER, getPosition(),
                                getCurrentLine(), "Trailing '_' in number.");
                    } else if (seen_point || seen_e) {
                        src.unread(c);
                        return getNumberToken(tokenBuffer.toString(), true, nondigit);
                    } else {
                    	int c2;
                        if (!Character.isDigit(c2 = src.read())) {
                            src.unread(c2);
                        	src.unread('.');
                            if (c == '_') { 
                            		// Enebo:  c can never be antrhign but '.'
                            		// Why did I put this here?
                            } else {
                                yaccValue = getInteger(tokenBuffer.toString(), 10);
                                return Tokens.tINTEGER;
                            }
                        } else {
                            tokenBuffer.append('.');
                            tokenBuffer.append((char) c2);
                            seen_point = true;
                            nondigit = '\0';
                        }
                    }
                    break;
                case 'e' :
                case 'E' :
                    if (nondigit != '\0') {
                        throw new SyntaxException(PID.TRAILING_UNDERSCORE_IN_NUMBER, getPosition(),
                                getCurrentLine(), "Trailing '_' in number.");
                    } else if (seen_e) {
                        src.unread(c);
                        return getNumberToken(tokenBuffer.toString(), true, nondigit);
                    } else {
                        tokenBuffer.append((char) c);
                        seen_e = true;
                        nondigit = c;
                        c = src.read();
                        if (c == '-' || c == '+') {
                            tokenBuffer.append((char) c);
                            nondigit = c;
                        } else {
                            src.unread(c);
                        }
                    }
                    break;
                case '_' : //  '_' in number just ignored
                    if (nondigit != '\0') {
                        throw new SyntaxException(PID.TRAILING_UNDERSCORE_IN_NUMBER, getPosition(),
                                getCurrentLine(), "Trailing '_' in number.");
                    }
                    nondigit = c;
                    break;
                default :
                    src.unread(c);
                return getNumberToken(tokenBuffer.toString(), seen_e || seen_point, nondigit);
            }
        }
    }

    private int getNumberToken(String number, boolean isFloat, int nondigit) {
        if (nondigit != '\0') {
            throw new SyntaxException(PID.TRAILING_UNDERSCORE_IN_NUMBER, getPosition(),
                    getCurrentLine(), "Trailing '_' in number.");
        } else if (isFloat) {
            return getFloatToken(number);
        }
        yaccValue = getInteger(number, 10);
        return Tokens.tINTEGER;
    }
    
    public int readEscape() throws IOException {
        int c = src.read();

        switch (c) {
            case '\\' : // backslash
                return c;
            case 'n' : // newline
                return '\n';
            case 't' : // horizontal tab
                return '\t';
            case 'r' : // carriage return
                return '\r';
            case 'f' : // form feed
                return '\f';
            case 'v' : // vertical tab
                return '\u000B';
            case 'a' : // alarm(bell)
                return '\u0007';
            case 'e' : // escape
                return '\u001B';
            case '0' : case '1' : case '2' : case '3' : // octal constant
            case '4' : case '5' : case '6' : case '7' :
                src.unread(c);
                return scanOct(3);
            case 'x' : // hex constant
                int i = 0;
                //char hexValue = scanHex(2);

                char hexValue = '\0';

                for (; i < 2; i++) {
                    int h1 = src.read();

                    if (!RubyYaccLexer.isHexChar(h1)) {
                        src.unread(h1);
                        break;
                    }

                    hexValue <<= 4;
                    hexValue |= Integer.parseInt(""+(char)h1, 16) & 15;
                }
                
                // No hex value after the 'x'.
                if (i == 0) {
                    throw new SyntaxException(PID.INVALID_ESCAPE_SYNTAX, getPosition(),
                            getCurrentLine(), "Invalid escape character syntax");
                }
                return hexValue;
            case 'b' : // backspace
                return '\010';
            case 's' : // space
                return ' ';
            case 'M' :
                if ((c = src.read()) != '-') {
                    throw new SyntaxException(PID.INVALID_ESCAPE_SYNTAX, getPosition(), 
                            getCurrentLine(), "Invalid escape character syntax");
                } else if ((c = src.read()) == '\\') {
                    return (char) (readEscape() | 0x80);
                } else if (c == EOF) {
                    throw new SyntaxException(PID.INVALID_ESCAPE_SYNTAX, getPosition(),
                            getCurrentLine(), "Invalid escape character syntax");
                } 
                return (char) ((c & 0xff) | 0x80);
            case 'C' :
                if ((c = src.read()) != '-') {
                    throw new SyntaxException(PID.INVALID_ESCAPE_SYNTAX, getPosition(),
                            getCurrentLine(), "Invalid escape character syntax");
                }
            case 'c' :
                if ((c = src.read()) == '\\') {
                    c = readEscape();
                } else if (c == '?') {
                    return '\u0177';
                } else if (c == EOF) {
                    throw new SyntaxException(PID.INVALID_ESCAPE_SYNTAX, getPosition(),
                            getCurrentLine(), "Invalid escape character syntax");
                }
                return (char) (c & 0x9f);
            case EOF :
                throw new SyntaxException(PID.INVALID_ESCAPE_SYNTAX, getPosition(),
                        getCurrentLine(), "Invalid escape character syntax");
            default :
                return c;
        }
    }

    private char scanOct(int count) throws IOException {
        char value = '\0';

        for (int i = 0; i < count; i++) {
            int c = src.read();

            if (!RubyYaccLexer.isOctChar(c)) {
                src.unread(c);
                break;
            }

            value <<= 3;
            value |= Integer.parseInt("" + (char) c, 8);
        }

        return value;
    }
}
