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
 * Copyright (C) 2013 The JRuby Team (jruby@jruby.org)
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
package org.jruby.ext.ripper;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.joni.Matcher;
import org.joni.Option;
import org.joni.Regex;
import org.jruby.Ruby;
import org.jruby.RubyRegexp;
import org.jruby.lexer.yacc.StackState;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.SafeDoubleParser;
import org.jruby.util.StringSupport;

/**
 *
 * @author enebo
 */
public class RipperLexer {
    public static final Encoding UTF8_ENCODING = UTF8Encoding.INSTANCE;
    public static final Encoding USASCII_ENCODING = USASCIIEncoding.INSTANCE;
    public static final Encoding ASCII8BIT_ENCODING = ASCIIEncoding.INSTANCE;
    
    private static ByteList END_MARKER = new ByteList(new byte[] {'_', '_', 'E', 'N', 'D', '_', '_'});
    private static ByteList BEGIN_DOC_MARKER = new ByteList(new byte[] {'b', 'e', 'g', 'i', 'n'});
    private static ByteList END_DOC_MARKER = new ByteList(new byte[] {'e', 'n', 'd'});
    private static ByteList CODING = new ByteList(new byte[] {'c', 'o', 'd', 'i', 'n', 'g'});
    private static final HashMap<String, Keyword> map;

    private static final int SUFFIX_R = 1<<0;
    private static final int SUFFIX_I = 1<<1;
    private static final int SUFFIX_ALL = 3;

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

    private Encoding current_enc;
    
    public boolean ignoreNextScanEvent = false;

    public Encoding getEncoding() {
        return current_enc;
    }

    private void ambiguousOperator(String op, String syn) {
        warn("`" + op + "' after local variable is interpreted as binary operator\nevent though it seems like \"" + syn + "\"");
    }

    private void warn_balanced(int c, boolean spaceSeen, String op, String syn) {
        if (false && last_state != LexState.EXPR_CLASS && last_state != LexState.EXPR_DOT &&
                last_state != LexState.EXPR_FNAME && last_state != LexState.EXPR_ENDFN &&
                last_state != LexState.EXPR_ENDARG && spaceSeen && !Character.isWhitespace(c)) {
            ambiguousOperator(op, syn);
        }
    }

    private int getFloatToken(String number, int suffix) {
        if ((suffix & SUFFIX_R) != 0) {
            BigDecimal bd = new BigDecimal(number);
            BigDecimal denominator = BigDecimal.ONE.scaleByPowerOfTen(bd.scale());
            BigDecimal numerator = bd.multiply(denominator);

            try {
                numerator.longValueExact();
                denominator.longValueExact();
            } catch (ArithmeticException ae) {
                compile_error("Rational (" + numerator + "/" + denominator + ") out of range.");
            }
            return considerComplex(Tokens.tRATIONAL, suffix);
        }

        double d;
        try {
            d = SafeDoubleParser.parseDouble(number);
        } catch (NumberFormatException e) {
            warn("Float " + number + " out of range.");

            d = number.startsWith("-") ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }
        ByteList buf = new ByteList(number.getBytes());
        yaccValue = new Token(buf);
        return considerComplex(Tokens.tFLOAT, suffix);
    }

    private int considerComplex(int token, int suffix) {
        if ((suffix & SUFFIX_I) == 0) {
            return token;
        } else {
            return Tokens.tIMAGINARY;
        }
    }


    public boolean isVerbose() {
        return parser.getRuntime().isVerbose();
    }

    public void warn(String message) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public void warning(String message) {
        throw new UnsupportedOperationException("Not supported yet.");
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
        EXPR_FNAME, EXPR_DOT, EXPR_CLASS, EXPR_VALUE, EXPR_ENDFN
    }
    
    public static Keyword getKeyword(String str) {
        return (Keyword) map.get(str);
    }
    
    // Last token read via yylex().
    private int token;
    
    // Value of last token which had a value associated with it.
    Object yaccValue;
    
    // MRI can directly seek source but we do not so we store all idents
    // here so the parser can then look at it on-demand to check things like
    // whether it is a valid identifier.  This should be safe to be a single
    // field since all ident logic should hit sequentially.
    String identValue;

    // Stream of data that yylex() examines.
    private LexerSource src;
    
    // Used for tiny smidgen of grammar in lexer (see setParserSupport())
    private RipperParserBase parser = null;

    // Additional context surrounding tokens that both the lexer and
    // grammar use.
    private LexState lex_state;
    private LexState last_state;
    
    // Tempory buffer to build up a potential token.  Consumer takes responsibility to reset 
    // this before use.
    private StringBuilder tokenBuffer = new StringBuilder(60);

    private StackState conditionState = new StackState();
    private StackState cmdArgumentState = new StackState();
    private StrTerm lex_strterm;
    public boolean commandStart;

    // Give a name to a value.  Enebo: This should be used more.
    static final int EOF = -1; // 0 in MRI

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

    public RipperLexer(RipperParserBase parser, LexerSource src) {
        this.parser = parser;
    	token = 0;
    	yaccValue = null;
    	this.src = src;
        setState(null);
        resetStacks();
        lex_strterm = null;
        commandStart = true;
        current_enc = src.getEncoding();
    }
    
    protected int tokp = 0; // Where last token started
    protected ByteList lexb = null;
    protected int lex_p = 0; // Where current position is in current line
    protected int lex_pbeg = 0; 
    protected int lex_pend = 0; // Where line ends
    protected ByteList lex_lastline = null;
    private ByteList lex_nextline = null;
    private boolean __end__seen = false;
    protected boolean eofp = false;
    private boolean has_shebang = false;
    protected ByteList delayed = null;
    private int delayed_line = 0;
    private int delayed_col = 0;
    private int ruby_sourceline = 0;
    private int heredoc_end = 0;
    private int line_count = 0;
    
    /**
     * Has lexing started yet?
     */
    public boolean hasStarted() {
        return src != null; // if no current line then nextc has never been called.
    }
    
    public boolean isEndSeen() {
        return __end__seen;
    }
    
    protected void flush_string_content(Encoding encoding) {
        if (delayed != null) {
            int len = lex_p - tokp;
            if (len > 0) {
                delayed.setEncoding(encoding);
                delayed.append(lexb.makeShared(tokp, len));
            }
            dispatchDelayedToken(Tokens.tSTRING_CONTENT);
            tokp = lex_p;
        }
    }
    
    public int p(int offset) {
        return lexb.get(offset) & 0xff;
    }
    
    public int nextc() {
        if (lex_p == lex_pend) {
            ByteList v = lex_nextline;
            lex_nextline = null;
            
            if (v == null) {
                if (eofp) return EOF;
                
                if (src == null || (v = src.gets()) == null) {
                    eofp = true;
                    lex_goto_eol();
                    return EOF;
                } 
            }
        
            // Left over stuffs...Add to delayed for later processing.
            if (tokp < lex_pend) {
                if (delayed == null) {
                    delayed = new ByteList();
                    delayed.setEncoding(current_enc);
                    delayed.append(lexb, tokp, lex_pend - tokp);
                    delayed_line = ruby_sourceline;
                    delayed_col = tokp - lex_pbeg;
                } else {
                    delayed.append(lexb, tokp, lex_pend - tokp);
                }
            }
        
            if (heredoc_end > 0) {
                ruby_sourceline = heredoc_end;
                heredoc_end = 0;
            }
            ruby_sourceline++;
            line_count++;
            lex_pbeg = lex_p = 0;
//            System.out.println("VLEN: " + v.length() + "V = (" + v.toString() + ")");
            lex_pend = lex_p + v.length();
            lexb = v;
            flush();
            lex_lastline = v;
        }
        
        int c = p(lex_p);
        lex_p++;
        if (c == '\r' && peek('\n')) {
            lex_p++;
            c = '\n';
        }

//        System.out.println("C: " + (char) c + ", LEXP: " + lex_p + ", PEND: "+ lex_pend);
        return c;
    }
    
    public boolean peek(int c) {
        return peek(c, 0);
    }
    
    private boolean peek(int c, int n) {
        return lex_p+n < lex_pend && p(lex_p+n) == c;
    }
    
    protected void lex_goto_eol() {
        lex_p = lex_pend;
    }
    
    public int column() {
        return tokp - lex_pbeg;
    }
    
    public int lineno() {
        return ruby_sourceline + src.getLineOffset();
    }
    
    public void dispatchHeredocEnd() {
        if (delayed != null) {
            dispatchDelayedToken(Tokens.tSTRING_CONTENT);
        }
        lex_goto_eol();
        dispatchIgnoredScanEvent(Tokens.tHEREDOC_END);
    }
    
    public boolean was_bol() {
        return lex_p == lex_pbeg + 1;
    }
    
    private boolean strncmp(ByteList one, ByteList two, int length) {
        if (one.length() < length || two.length() < length) return false;
        
        return one.makeShared(0, length).equal(two.makeShared(0, length));
    }
    
    public void pushback(int c) {
        if (c == -1) return;
        
        lex_p--;
        
        if (lex_p > lex_pbeg && p(lex_p) == '\n' && p(lex_p-1) == '\r') {
            lex_p--;
        }
    }
    
    private void flush() {
        tokp = lex_p;
    }
    
    public void compile_error(String message) {
        parser.error();
        parser.dispatch("compile_error", getRuntime().newString(message));
//        throw new SyntaxException(lexb.toString(), message);
    }
    
    // FIXME: This is our main lexer code mangled into here...
    // Super slow codepoint reader when we detect non-asci chars
    public int readCodepoint(int first, Encoding encoding) throws IOException {
        int length = encoding.length(lexb.getUnsafeBytes(), lex_p - 1, lex_pend);
        if (length < 0) {
            return -2;
        }
        int codepoint = encoding.mbcToCode(lexb.getUnsafeBytes(), lex_p - 1, length);
        
        lex_p += length - 1;

        return codepoint;
    }
    
    public void heredoc_restore(HeredocTerm here) {
        ByteList line = here.lastLine;
        lex_lastline = line;
        lex_pbeg = 0;
        lex_pend = lex_pbeg + line.length();
        lex_p = lex_pbeg + here.nth;
        lexb = line;
        heredoc_end = ruby_sourceline;
        ruby_sourceline = here.line;
        flush();
    }
    
    public void parser_prepare() {
        int c = nextc();
        
        switch(c) {
            case '#':
                if (peek('!')) has_shebang = true;
                break;
            case 0xef:
                if (lex_pend - lex_p >= 2 && p(lex_p) == 0xbb && p(lex_p + 1) == 0xbf) {
                    setEncoding(UTF8_ENCODING);
                    lex_p += 2;
                    lex_pbeg = lex_p;
                    return;
                }
                break;
            case EOF:
                return;
        }
        pushback(c);
        current_enc = lex_lastline.getEncoding();
    }

    public int nextToken() throws IOException { //mri: yylex

        token = yylex();
        
        if (delayed != null) {
            dispatchDelayedToken(token);
            return token == EOF ? 0 : token;
        }
        
        if (token != EOF) dispatchScanEvent(token);

        return token == EOF ? 0 : token;
    }
    
    public String getIdent() {
        return identValue;
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
    
    public boolean whole_match_p(ByteList eos, boolean indent) {
        int len = eos.length();
        int p = lex_pbeg;
        
        if (indent) {
            for (int i = 0; i < lex_pend; i++) {
                if (!Character.isWhitespace(p(i+p))) {
                    p += i;
                    break;
                }
            }
        }
        int n = lex_pend - (p + len);
        if (n < 0 || (n > 0 && p(p+len) != '\n' && p(p+len) != '\r')) return false;

        return strncmp(eos, lexb.makeShared(p, len), len);
    }
    
    public Ruby getRuntime() {
        return parser.context.getRuntime();
    }

    /**
     * Parse must pass its support object for some check at bottom of
     * yylex().  Ruby does it this way as well (i.e. a little parsing
     * logic in the lexer).
     * 
     * @param parserSupport
     */
    public void setParser(RipperParserBase parserSupport) {
        this.parser = parserSupport;
    }

    private void setEncoding(ByteList name) {
        Encoding newEncoding = parser.getRuntime().getEncodingService().loadEncoding(name);

        if (newEncoding == null) {
            compile_error("unknown encoding name: " + name.toString());
            return;
        }

        if (!newEncoding.isAsciiCompatible()) {
            compile_error(name.toString() + " is not ASCII compatible");
            return;
        }

        setEncoding(newEncoding);
    }

    // FIXME: This is mucked up...current line knows it's own encoding so that must be changed.  but we also have two
    // other sources.  I am thinking current_enc should be removed in favor of src since it needs to know encoding to
    // provide next line.
    public void setEncoding(Encoding encoding) {
        this.current_enc = encoding;
        src.setEncoding(encoding);
        lexb.setEncoding(encoding);
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
    
    private void printState() {
        if (lex_state == null) {
            System.out.println("NULL");
        } else {
            System.out.println(lex_state);
        }
    }

    public void setState(LexState state) {
        this.lex_state = state;
//        printState();
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
        int c = nextc();
        pushback(c);

        return c != EOF && (Character.isLetterOrDigit(c) || c == '_');
    }

    // mri: parser_isascii
    public boolean isASCII() {
        return Encoding.isMbcAscii((byte)lexb.get(lex_p-1));
    }

    private boolean isBEG() {
        return lex_state == LexState.EXPR_BEG || lex_state == LexState.EXPR_MID ||
                lex_state == LexState.EXPR_CLASS || lex_state == LexState.EXPR_VALUE;
    }
    
    private boolean isEND() {
        return lex_state == LexState.EXPR_END || lex_state == LexState.EXPR_ENDARG ||
                lex_state == LexState.EXPR_ENDFN;
    }

    private boolean isARG() {
        return lex_state == LexState.EXPR_ARG || lex_state == LexState.EXPR_CMDARG;
    }
    
    private boolean isLabelPossible(boolean commandState) {
        return ((lex_state == LexState.EXPR_BEG || lex_state == LexState.EXPR_ENDFN) && !commandState) || isARG();
    }    
    
    private boolean isSpaceArg(int c, boolean spaceSeen) {
        return isARG() && spaceSeen && !Character.isWhitespace(c);
    }

    private void determineExpressionState() {
        switch (lex_state) {
        case EXPR_FNAME: case EXPR_DOT:
            setState(LexState.EXPR_ARG);
            break;
        default:
            setState(LexState.EXPR_BEG);
            break;
        }
    }

	/**
	 * @param c the character to test
	 * @return true if character is a hex value (0-9a-f)
	 */
    static boolean isHexChar(int c) {
        return Character.isDigit(c) || ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F');
    }

    /**
	 * @param c the character to test
     * @return true if character is an octal value (0-7)
	 */
    static boolean isOctChar(int c) {
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
    public boolean isIdentifierChar(int c) {
        return Character.isLetterOrDigit(c) || c == '_' || isMultiByteChar(c);
    }

    /**
     * Is this a multibyte character from a multibyte encoding?
     *
     * @param c
     * @return whether c is an multibyte char or not
     */
    protected boolean isMultiByteChar(int c) {
        return current_enc.codeToMbcLength(c) != 1;
    }

    // STR_NEW3/parser_str_new
    public IRubyObject createStr(ByteList buffer, int flags) {
        Encoding bufferEncoding = buffer.getEncoding();
        int codeRange = StringSupport.codeRangeScan(bufferEncoding, buffer);

        if ((flags & STR_FUNC_REGEXP) == 0 && bufferEncoding.isAsciiCompatible()) {
            // If we have characters outside 7-bit range and we are still ascii then change to ascii-8bit
            if (codeRange == StringSupport.CR_7BIT) {
                // Do nothing like MRI
            } else if (getEncoding() == USASCII_ENCODING &&
                    bufferEncoding != UTF8_ENCODING) {
                codeRange = RipperParserBase.associateEncoding(buffer, ASCII8BIT_ENCODING, codeRange);
            }
        }

        return getRuntime().newString(buffer);
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
        String value = "%" + (char) c;
        
        // Short-hand (e.g. %{,%.,%!,... versus %Q{).
        if (!Character.isLetterOrDigit(c)) {
            begin = c;
            c = 'Q';
            shortHand = true;
        // Long-hand (e.g. %Q{}).
        } else {
            shortHand = false;
            begin = nextc();
            value = value + (char) begin;
            if (Character.isLetterOrDigit(begin) || !isASCII()) {
                compile_error("unknown type of %string");
                return EOF;
            }
        }
        if (c == EOF || begin == EOF) {
            compile_error("unterminated quoted string meets end of file");
            return EOF;
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

        // consume spaces here to record them as part of token
        int w = nextc();
        while (Character.isWhitespace(w)) {
            value = value + (char) w;
            w = nextc();
        }
        pushback(w);
        
        yaccValue = new Token(value);
        switch (c) {
        case 'Q':
            lex_strterm = new StringTerm(str_dquote, begin ,end);
            return Tokens.tSTRING_BEG;

        case 'q':
            lex_strterm = new StringTerm(str_squote, begin, end);
            return Tokens.tSTRING_BEG;

        case 'W':
            lex_strterm = new StringTerm(str_dquote | STR_FUNC_QWORDS, begin, end);
            do {c = nextc();} while (Character.isWhitespace(c));
            pushback(c);
            return Tokens.tWORDS_BEG;

        case 'w':
            lex_strterm = new StringTerm(/* str_squote | */ STR_FUNC_QWORDS, begin, end);
            do {c = nextc();} while (Character.isWhitespace(c));
            pushback(c);
            return Tokens.tQWORDS_BEG;

        case 'x':
            lex_strterm = new StringTerm(str_xquote, begin, end);
            return Tokens.tXSTRING_BEG;

        case 'r':
            lex_strterm = new StringTerm(str_regexp, begin, end);
            return Tokens.tREGEXP_BEG;

        case 's':
            lex_strterm = new StringTerm(str_ssym, begin, end);
            setState(LexState.EXPR_FNAME);
            return Tokens.tSYMBEG;

        case 'I':
            lex_strterm = new StringTerm(str_dquote | STR_FUNC_QWORDS, begin, end);
            do {c = nextc();} while (Character.isWhitespace(c));
            pushback(c);
            return Tokens.tSYMBOLS_BEG;

        case 'i':
            lex_strterm = new StringTerm(/* str_squote | */STR_FUNC_QWORDS, begin, end);
            do {c = nextc();} while (Character.isWhitespace(c));
            pushback(c);
            return Tokens.tQSYMBOLS_BEG;
        default:
            compile_error("Unknown type of %string. Expected 'Q', 'q', 'w', 'x', 'r' or any non letter character, but found '" + c + "'.");
            return -1; //notreached
        }
    }
    
    private int hereDocumentIdentifier() throws IOException {
        int c = nextc(); 
        int term;

        int func = 0;
        if (c == '-') {
            c = nextc();
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
            markerValue.setEncoding(current_enc);
            term = c;
            while ((c = nextc()) != EOF && c != term) {
                if (!tokenAddMBC(c, markerValue)) return EOF;
            }
            if (c == EOF) compile_error("unterminated here document identifier");
        } else {
            if (!isIdentifierChar(c)) {
                pushback(c);
                if ((func & STR_FUNC_INDENT) != 0) {
                    pushback('-');
                }
                return 0;
            }
            markerValue = new ByteList();
            term = '"';
            func |= str_dquote;
            do {
                if (!tokenAddMBC(c, markerValue)) return EOF;
            } while ((c = nextc()) != EOF && isIdentifierChar(c));

            pushback(c);
        }

        dispatchScanEvent(Tokens.tHEREDOC_BEG);
        int len = lex_p - lex_pbeg;        
        lex_goto_eol();
        lex_strterm = new HeredocTerm(markerValue, func, len, ruby_sourceline, lex_lastline);

        flush();
        return term == '`' ? Tokens.tXSTRING_BEG : Tokens.tSTRING_BEG;
    }
    
    private void arg_ambiguous() {
        parser.dispatch("on_arg_ambiguous");
    }


    /* MRI: magic_comment_marker */
    /* This impl is a little sucky.  We basically double scan the same bytelist twice.  Once here
     * and once in parseMagicComment.
     */
    private int magicCommentMarker(ByteList str, int begin) {
        int i = begin;
        int len = str.length();

        while (i < len) {
            switch (str.charAt(i)) {
                case '-':
                    if (i >= 2 && str.charAt(i - 1) == '*' && str.charAt(i - 2) == '-') return i + 1;
                    i += 2;
                    break;
                case '*':
                    if (i + 1 >= len) return -1;

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
        return -1;
    }

    private static final String magicString = "([^\\s\'\":;]+)\\s*:\\s*(\"(?:\\\\.|[^\"])*\"|[^\"\\s;]+)[\\s;]*";
    private static final Regex magicRegexp = new Regex(magicString.getBytes(), 0, magicString.length(), 0, Encoding.load("ASCII"));
    
    private boolean comment_at_top() {
        int p = lex_pbeg;
        int pend = lex_p - 1;
        if (line_count != (has_shebang ? 2 : 1)) return false;
        while (p < pend) {
            if (!Character.isSpaceChar(p(p))) return false;
            p++;
        }
        return true;
    }
    
    // MRI: parser_magic_comment
    protected boolean parseMagicComment(ByteList magicLine) throws IOException {
        int length = magicLine.length();
        if (length <= 7) return false;
        int beg = magicCommentMarker(magicLine, 0);
        if (beg < 0) return false;
        int end = magicCommentMarker(magicLine, beg);
        if (end < 0) return false;

        // We only use a regex if -*- ... -*- is found.  Not too hot a path?
        int realSize = magicLine.getRealSize();
        int begin = magicLine.getBegin();
        Matcher matcher = magicRegexp.matcher(magicLine.getUnsafeBytes(), begin, begin + realSize);
        int result = RubyRegexp.matcherSearch(getRuntime(), matcher, begin, begin + realSize, Option.NONE);
        if (result < 0) return false;

        // Regexp is guarateed to have three matches
        int begs[] = matcher.getRegion().beg;
        int ends[] = matcher.getRegion().end;
        String name = magicLine.subSequence(begs[1], ends[1]).toString();
        if (!name.equalsIgnoreCase("encoding")) return false;

        ByteList val = new ByteList(magicLine.getUnsafeBytes(), begs[2], ends[2] - begs[2]);
        
        parser.dispatch("on_magic_comment", parser.getRuntime().newString(name), createStr(val, 0));

        return true;
    }

    protected void set_file_encoding(int str, int send) {
        boolean sep = false;
        for (;;) {
            if (send - str <= 6) return;
            
            switch(p(str+6)) {
                case 'C': case 'c': str += 6; continue;
                case 'O': case 'o': str += 5; continue;
                case 'D': case 'd': str += 4; continue;
                case 'I': case 'i': str += 3; continue;
                case 'N': case 'n': str += 2; continue;
                case 'G': case 'g': str += 1; continue;
                case '=': case ':':
                    sep = true;
                    str += 6;
                    break;
                default:
                    str += 6;
                    if (Character.isSpaceChar(p(str))) break;
                    continue;
            }
            if (lexb.makeShared(str - 6, 6).caseInsensitiveCmp(CODING) == 0) break;
        }
        
        for(;;) {
            do {
                str++;
                if (str >= send) return;
            } while(Character.isSpaceChar(p(str)));
            if (sep) break;
            
            if (p(str) != '=' && p(str) != ':') return;
            sep = true;
            str++;
        }
        
        int beg = str;
        while ((p(str) == '-' || p(str) == '_' || Character.isLetterOrDigit(p(str))) && ++str < send) {}
        setEncoding(lexb.makeShared(beg, str - beg));
        src.setEncoding(getEncoding()); // Change source to know what bytelist encodings to send for next source lines
        lexb.setEncoding(getEncoding()); // Also retroactively change current line to new encoding
    }

    /*
     * Not normally used, but is left in here since it can be useful in debugging
     * grammar and lexing problems.
     *
     */
    private String printToken(int token) {
        //System.out.print("LOC: " + support.getPosition() + " ~ ");
        
        switch (token) {
            case Tokens.yyErrorCode: return "yyErrorCode,";
            case Tokens.kCLASS: return "kClass,";
            case Tokens.kMODULE: return "kModule,";
            case Tokens.kDEF: return "kDEF,";
            case Tokens.kUNDEF: return "kUNDEF,";
            case Tokens.kBEGIN: return "kBEGIN,";
            case Tokens.kRESCUE: return "kRESCUE,";
            case Tokens.kENSURE: return "kENSURE,";
            case Tokens.kEND: return "kEND,";
            case Tokens.kIF: return "kIF,";
            case Tokens.kUNLESS: return "kUNLESS,";
            case Tokens.kTHEN: return "kTHEN,";
            case Tokens.kELSIF: return "kELSIF,";
            case Tokens.kELSE: return "kELSE,";
            case Tokens.kCASE: return "kCASE,";
            case Tokens.kWHEN: return "kWHEN,";
            case Tokens.kWHILE: return "kWHILE,";
            case Tokens.kUNTIL: return "kUNTIL,";
            case Tokens.kFOR: return "kFOR,";
            case Tokens.kBREAK: return "kBREAK,";
            case Tokens.kNEXT: return "kNEXT,";
            case Tokens.kREDO: return "kREDO,";
            case Tokens.kRETRY: return "kRETRY,";
            case Tokens.kIN: return "kIN,";
            case Tokens.kDO: return "kDO,";
            case Tokens.kDO_COND: return "kDO_COND,";
            case Tokens.kDO_BLOCK: return "kDO_BLOCK,";
            case Tokens.kRETURN: return "kRETURN,";
            case Tokens.kYIELD: return "kYIELD,";
            case Tokens.kSUPER: return "kSUPER,";
            case Tokens.kSELF: return "kSELF,";
            case Tokens.kNIL: return "kNIL,";
            case Tokens.kTRUE: return "kTRUE,";
            case Tokens.kFALSE: return "kFALSE,";
            case Tokens.kAND: return "kAND,";
            case Tokens.kOR: return "kOR,";
            case Tokens.kNOT: return "kNOT,";
            case Tokens.kIF_MOD: return "kIF_MOD,";
            case Tokens.kUNLESS_MOD: return "kUNLESS_MOD,";
            case Tokens.kWHILE_MOD: return "kWHILE_MOD,";
            case Tokens.kUNTIL_MOD: return "kUNTIL_MOD,";
            case Tokens.kRESCUE_MOD: return "kRESCUE_MOD,";
            case Tokens.kALIAS: return "kALIAS,";
            case Tokens.kDEFINED: return "kDEFINED,";
            case Tokens.klBEGIN: return "klBEGIN,";
            case Tokens.klEND: return "klEND,";
            case Tokens.k__LINE__: return "k__LINE__,";
            case Tokens.k__FILE__: return "k__FILE__,";
            case Tokens.k__ENCODING__: return "k__ENCODING__,";
            case Tokens.kDO_LAMBDA: return "kDO_LAMBDA,";
            case Tokens.tIDENTIFIER: return "tIDENTIFIER["+ value() + "],";
            case Tokens.tFID: return "tFID[" + value() + "],";
            case Tokens.tGVAR: return "tGVAR[" + value() + "],";
            case Tokens.tIVAR: return "tIVAR[" + value() +"],";
            case Tokens.tCONSTANT: return "tCONSTANT["+ value() +"],";
            case Tokens.tCVAR: return "tCVAR,";
            case Tokens.tINTEGER: return "tINTEGER,";
            case Tokens.tFLOAT: return "tFLOAT,";
            case Tokens.tSTRING_CONTENT: return "tSTRING_CONTENT[" + value() + "],";
            case Tokens.tSTRING_BEG: return "tSTRING_BEG,";
            case Tokens.tSTRING_END: return "tSTRING_END,";
            case Tokens.tSTRING_DBEG: return "tSTRING_DBEG,";
            case Tokens.tSTRING_DVAR: return "tSTRING_DVAR,";
            case Tokens.tXSTRING_BEG: return "tXSTRING_BEG,";
            case Tokens.tREGEXP_BEG: return "tREGEXP_BEG,";
            case Tokens.tREGEXP_END: return "tREGEXP_END,";
            case Tokens.tWORDS_BEG: return "tWORDS_BEG,";
            case Tokens.tQWORDS_BEG: return "tQWORDS_BEG,";
            case Tokens.tBACK_REF: return "tBACK_REF,";
            case Tokens.tBACK_REF2: return "tBACK_REF2,";
            case Tokens.tNTH_REF: return "tNTH_REF,";
            case Tokens.tUPLUS: return "tUPLUS";
            case Tokens.tUMINUS: return "tUMINUS,";
            case Tokens.tPOW: return "tPOW,";
            case Tokens.tCMP: return "tCMP,";
            case Tokens.tEQ: return "tEQ,";
            case Tokens.tEQQ: return "tEQQ,";
            case Tokens.tNEQ: return "tNEQ,";
            case Tokens.tGEQ: return "tGEQ,";
            case Tokens.tLEQ: return "tLEQ,";
            case Tokens.tANDOP: return "tANDOP,";
            case Tokens.tOROP: return "tOROP,";
            case Tokens.tMATCH: return "tMATCH,";
            case Tokens.tNMATCH: return "tNMATCH,";
            case Tokens.tDOT: return "tDOT,";
            case Tokens.tDOT2: return "tDOT2,";
            case Tokens.tDOT3: return "tDOT3,";
            case Tokens.tAREF: return "tAREF,";
            case Tokens.tASET: return "tASET,";
            case Tokens.tLSHFT: return "tLSHFT,";
            case Tokens.tRSHFT: return "tRSHFT,";
            case Tokens.tCOLON2: return "tCOLON2,";
            case Tokens.tCOLON3: return "tCOLON3,";
            case Tokens.tOP_ASGN: return "tOP_ASGN,";
            case Tokens.tASSOC: return "tASSOC,";
            case Tokens.tLPAREN: return "tLPAREN,";
            case Tokens.tLPAREN2: return "tLPAREN2,";
            case Tokens.tLPAREN_ARG: return "tLPAREN_ARG,";
            case Tokens.tLBRACK: return "tLBRACK,";
            case Tokens.tRBRACK: return "tRBRACK,";
            case Tokens.tLBRACE: return "tLBRACE,";
            case Tokens.tLBRACE_ARG: return "tLBRACE_ARG,";
            case Tokens.tSTAR: return "tSTAR,";
            case Tokens.tSTAR2: return "tSTAR2,";
            case Tokens.tAMPER: return "tAMPER,";
            case Tokens.tAMPER2: return "tAMPER2,";
            case Tokens.tSYMBEG: return "tSYMBEG,";
            case Tokens.tTILDE: return "tTILDE,";
            case Tokens.tPERCENT: return "tPERCENT,";
            case Tokens.tDIVIDE: return "tDIVIDE,";
            case Tokens.tPLUS: return "tPLUS,";
            case Tokens.tMINUS: return "tMINUS,";
            case Tokens.tLT: return "tLT,";
            case Tokens.tGT: return "tGT,";
            case Tokens.tCARET: return "tCARET,";
            case Tokens.tBANG: return "tBANG,";
            case Tokens.tLCURLY: return "tTLCURLY,";
            case Tokens.tRCURLY: return "tRCURLY,";
            case Tokens.tPIPE: return "tTPIPE,";
            case Tokens.tLAMBDA: return "tLAMBDA,";
            case Tokens.tLAMBEG: return "tLAMBEG,";
            case Tokens.tRPAREN: return "tRPAREN,";
            case Tokens.tLABEL: return "tLABEL("+ ((Token) value()).getValue() +":),";
            case '\n': return "NL";
            case EOF: return "EOF";
            default: return "'" + (char)token + " [" + (int) token + "',";
        }
    }
    
    public boolean hasScanEvent() {
        if (lex_p < tokp) throw parser.getRuntime().newRuntimeError("lex_p < tokp");
        
        return lex_p > tokp;
    }
    
    public void dispatchDelayedToken(int token) { //mri: rupper_dispatch_delayed_token
        int saved_line = ruby_sourceline;
        int saved_tokp = tokp;
        
        ruby_sourceline = delayed_line;
        tokp = lex_pbeg + delayed_col;
        
        //System.out.println("TOKP: " + tokp + ", LEX_P: " + lex_p);        
        IRubyObject value = parser.getRuntime().newString(delayed.dup());
        String event = tokenToEventId(token);
        //System.out.println("EVENT: " + event + ", VALUE: " + value);
        yaccValue = parser.dispatch(event, value);
        delayed = null;
        ruby_sourceline = saved_line;
        tokp = saved_tokp;
    }
    
    public void dispatchIgnoredScanEvent(int token) {
        if (!hasScanEvent()) return;
        
        scanEventValue(token);
    }
    
    public void dispatchScanEvent(int token) { //mri: ripper_dispatch_scan_event
        if (!hasScanEvent()) return;
        
        yaccValue = scanEventValue(token);
    }
    
    private IRubyObject scanEventValue(int token) { // mri: ripper_scane_event_val
        //System.out.println("TOKP: " + tokp + ", LEX_P: " + lex_p);
        IRubyObject value = parser.getRuntime().newString(lexb.makeShared(tokp, lex_p - tokp));
        String event = tokenToEventId(token);
        //System.out.println("EVENT: " + event + ", VALUE: " + value);
        IRubyObject returnValue = parser.dispatch(event, value);
        flush();
        return returnValue;
    }
    
    private String tokenToEventId(int token) {
        switch(token) {
            case ' ': return "on_words_sep";
            case Tokens.tBANG: return "on_op";
            case Tokens.tPERCENT: return "on_op";
            case Tokens.tAMPER2: return "on_op";
            case Tokens.tSTAR2: return "on_op";
            case Tokens.tPLUS: return "on_op";
            case Tokens.tMINUS: return "on_op";
            case Tokens.tDIVIDE: return "on_op";
            case Tokens.tLT: return "on_op";
            case '=': return "on_op";
            case Tokens.tGT: return "on_op";
            case '?': return "on_op";
            case Tokens.tCARET: return "on_op";
            case Tokens.tPIPE: return "on_op";
            case Tokens.tTILDE: return "on_op";
            case ':': return "on_op";
            case ',': return "on_comma";
            case '.': return "on_period";
            case Tokens.tDOT: return "on_period";
            case ';': return "on_semicolon";
            case Tokens.tBACK_REF2: return "on_backtick";
            case '\n': return "on_nl";
            case Tokens.kALIAS: return "on_kw";
            case Tokens.kAND: return "on_kw";
            case Tokens.kBEGIN: return "on_kw";
            case Tokens.kBREAK: return "on_kw";
            case Tokens.kCASE: return "on_kw";
            case Tokens.kCLASS: return "on_kw";
            case Tokens.kDEF: return "on_kw";
            case Tokens.kDEFINED: return "on_kw";
            case Tokens.kDO: return "on_kw";
            case Tokens.kDO_BLOCK: return "on_kw";
            case Tokens.kDO_COND: return "on_kw";
            case Tokens.kELSE: return "on_kw";
            case Tokens.kELSIF: return "on_kw";
            case Tokens.kEND: return "on_kw";
            case Tokens.kENSURE: return "on_kw";
            case Tokens.kFALSE: return "on_kw";
            case Tokens.kFOR: return "on_kw";
            case Tokens.kIF: return "on_kw";
            case Tokens.kIF_MOD: return "on_kw";
            case Tokens.kIN: return "on_kw";
            case Tokens.kMODULE: return "on_kw";
            case Tokens.kNEXT: return "on_kw";
            case Tokens.kNIL: return "on_kw";
            case Tokens.kNOT: return "on_kw";
            case Tokens.kOR: return "on_kw";
            case Tokens.kREDO: return "on_kw";
            case Tokens.kRESCUE: return "on_kw";
            case Tokens.kRESCUE_MOD: return "on_kw";
            case Tokens.kRETRY: return "on_kw";
            case Tokens.kRETURN: return "on_kw";
            case Tokens.kSELF: return "on_kw";
            case Tokens.kSUPER: return "on_kw";
            case Tokens.kTHEN: return "on_kw";
            case Tokens.kTRUE: return "on_kw";
            case Tokens.kUNDEF: return "on_kw";
            case Tokens.kUNLESS: return "on_kw";
            case Tokens.kUNLESS_MOD: return "on_kw";
            case Tokens.kUNTIL: return "on_kw";
            case Tokens.kUNTIL_MOD: return "on_kw";
            case Tokens.kWHEN: return "on_kw";
            case Tokens.kWHILE: return "on_kw";
            case Tokens.kWHILE_MOD: return "on_kw";
            case Tokens.kYIELD: return "on_kw";
            case Tokens.k__FILE__: return "on_kw";
            case Tokens.k__LINE__: return "on_kw";
            case Tokens.k__ENCODING__: return "on_kw";
            case Tokens.klBEGIN: return "on_kw";
            case Tokens.klEND: return "on_kw";
            case Tokens.kDO_LAMBDA: return "on_kw";
            case Tokens.tAMPER: return "on_op";
            case Tokens.tANDOP: return "on_op";
            case Tokens.tAREF: return "on_op";
            case Tokens.tASET: return "on_op";
            case Tokens.tASSOC: return "on_op";
            case Tokens.tBACK_REF: return "on_backref";
            case Tokens.tCHAR: return "on_CHAR";
            case Tokens.tCMP: return "on_op";
            case Tokens.tCOLON2: return "on_op";
            case Tokens.tCOLON3: return "on_op";
            case Tokens.tCONSTANT: return "on_const";
            case Tokens.tCVAR: return "on_cvar";
            case Tokens.tDOT2: return "on_op";
            case Tokens.tDOT3: return "on_op";
            case Tokens.tEQ: return "on_op";
            case Tokens.tEQQ: return "on_op";
            case Tokens.tFID: return "on_ident";
            case Tokens.tFLOAT: return "on_float";
            case Tokens.tGEQ: return "on_op";
            case Tokens.tGVAR: return "on_gvar";
            case Tokens.tIDENTIFIER: return "on_ident";
            case Tokens.tIMAGINARY: return "on_imaginary";
            case Tokens.tINTEGER: return "on_int";
            case Tokens.tIVAR: return "on_ivar";
            case Tokens.tLBRACE: return "on_lbrace";
            case Tokens.tLBRACE_ARG: return "on_lbrace";
            case Tokens.tLCURLY: return "on_lbrace";
            case Tokens.tRCURLY: return "on_rbrace";
            case Tokens.tLBRACK: return "on_lbracket";
            case '[': return "on_lbracket";
            case Tokens.tRBRACK: return "on_rbracket";
            case Tokens.tLEQ: return "on_op";
            case Tokens.tLPAREN: return "on_lparen";
            case Tokens.tLPAREN_ARG: return "on_lparen";
            case Tokens.tLPAREN2: return "on_lparen";
            case ')': return "on_rparen";  // ENEBO: Don't this this can happen.
            case Tokens.tLSHFT: return "on_op";
            case Tokens.tMATCH: return "on_op";
            case Tokens.tNEQ: return "on_op";
            case Tokens.tNMATCH: return "on_op";
            case Tokens.tNTH_REF: return "on_backref";
            case Tokens.tOP_ASGN: return "on_op";
            case Tokens.tOROP: return "on_op";
            case Tokens.tPOW: return "on_op";
            case Tokens.tQSYMBOLS_BEG: return "on_qsymbols_beg";
            case Tokens.tRATIONAL: return "on_rational";
            case Tokens.tSYMBOLS_BEG: return "on_symbols_beg";
            case Tokens.tQWORDS_BEG: return "on_qwords_beg";
            case Tokens.tREGEXP_BEG:return "on_regexp_beg";
            case Tokens.tREGEXP_END: return "on_regexp_end";
            case Tokens.tRPAREN: return "on_rparen";
            case Tokens.tRSHFT: return "on_op";
            case Tokens.tSTAR: return "on_op";
            case Tokens.tDSTAR: return "on_op";
            case Tokens.tSTRING_BEG: return "on_tstring_beg";
            case Tokens.tSTRING_CONTENT: return "on_tstring_content";
            case Tokens.tSTRING_DBEG: return "on_embexpr_beg";
            case Tokens.tSTRING_DEND: return "on_embexpr_end";
            case Tokens.tSTRING_DVAR: return "on_embvar";
            case Tokens.tSTRING_END: return "on_tstring_end";
            case Tokens.tSYMBEG: return "on_symbeg";
            case Tokens.tUMINUS: return "on_op";
            case Tokens.tUMINUS_NUM: return "on_op";
            case Tokens.tUPLUS: return "on_op";
            case Tokens.tWORDS_BEG: return "on_words_beg";
            case Tokens.tXSTRING_BEG: return "on_backtick";
            case Tokens.tLABEL: return "on_label";
            case Tokens.tLABEL_END: return "on_label_end";
            case Tokens.tLAMBDA: return "on_tlambda";
            case Tokens.tLAMBEG: return "on_tlambeg";

            // ripper specific tokens
            case Tokens.tIGNORED_NL: return "on_ignored_nl";
            case Tokens.tCOMMENT: return "on_comment";
            case Tokens.tEMBDOC_BEG: return "on_embdoc_beg";
            case Tokens.tEMBDOC: return "on_embdoc";
            case Tokens.tEMBDOC_END: return "on_embdoc_end";
            case Tokens.tSP: return "on_sp";
            case Tokens.tHEREDOC_BEG: return "on_heredoc_beg";
            case Tokens.tHEREDOC_END: return "on_heredoc_end";
            case Tokens.k__END__: return "on___end__";
            default: // Weird catchall but we will try and not use < 256 value trick like MRI
                return "on_CHAR";
        }
    }
    
    // DEBUGGING HELP 
    private int yylex2() throws IOException {
        try {
        int currentToken = yylex2();
        
        System.out.println(printToken(currentToken));
        
        return currentToken;
        } catch (Exception e) {
            System.out.println("FFUFUFUFUFUFUFUF: " + e);
            return EOF;
        }
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

            if (tok == Tokens.tSTRING_END && (peek('"', -1) || peek('\'', -1))) {
                if (((lex_state == LexState.EXPR_BEG || lex_state == LexState.EXPR_ENDFN) && !conditionState.isInState() ||
                        isARG()) && peek(':')) {
                    int c1 = nextc();
                    if (peek(':')) { // "mod"::SOMETHING (hack MRI does not do this)
                        pushback(c1);
                    } else {
                        tok = Tokens.tLABEL_END;
                    }
                }
            }

            if (tok == Tokens.tSTRING_END || tok == Tokens.tREGEXP_END|| tok == Tokens.tLABEL_END) {
                lex_strterm = null;
                setState(LexState.EXPR_END);
            }

            return tok;
        }

        commandState = commandStart;
        commandStart = false;

        loop: for(;;) {
            boolean fallthru = false;
            last_state = lex_state;
            c = nextc();
            switch(c) {
            case '\000': /* NUL */
            case '\004': /* ^D */
            case '\032': /* ^Z */
            case EOF:	 /* end of script. */
                return 0;
           
                /* white spaces */
            case ' ': case '\t': case '\f': case '\r':
            case '\13': /* '\v' */ {
                ByteList whitespaceBuf = new ByteList(); // FIXME: bytelist encoding hookedup
                whitespaceBuf.append(c);
                boolean looping = true;
                spaceSeen = true;
                while (looping && (c = nextc()) != EOF) {
                    switch (c) {
                        case ' ': case '\t': case '\f': case '\r':
                        case '\13': /* '\v' */
                            whitespaceBuf.append(c);
                            break;
                        default:
                            looping = false;
                            break;
                    }
                }
                pushback(c);
                dispatchScanEvent(Tokens.tSP);
                continue;
            }
            case '#':		/* it's a comment */
                if (!parseMagicComment(lexb.makeShared(lex_p, lex_pend - lex_p))) {
                    if (comment_at_top()) {
                        set_file_encoding(lex_p, lex_pend);
                    }
                }
                lex_p = lex_pend;
                dispatchScanEvent(Tokens.tCOMMENT);
                    
                fallthru = true;
                /* fall through */
            case '\n':
                switch (lex_state) {
                    case EXPR_BEG:
                    case EXPR_FNAME:
                    case EXPR_DOT:
                    case EXPR_CLASS:
                    case EXPR_VALUE:
                        if (!fallthru) dispatchScanEvent(Tokens.tIGNORED_NL);

                        continue loop;
                }

                boolean done = false;
                while (!done) {
                    c = nextc();

                    switch (c) {
                        case ' ': case '\t': case '\f': case '\r':
                        case '\13': /* '\v' */
                            spaceSeen = true;
                            continue;
                        case '.': {
                            if ((c = nextc()) != '.') {
                                pushback(c);
                                pushback('.');
                                continue loop;
                            }
                        }
                        default:
                            ruby_sourceline--;
                            lex_nextline = lex_lastline;
                        case -1:		// EOF (ENEBO: After default?
                            lex_goto_eol();
                            if (c != -1) tokp = lex_p;
                            done = true;
                    }
                }

                commandStart = true;
                setState(LexState.EXPR_BEG);
                return '\n';
            case '*':
                return star(spaceSeen);
            case '!':
                return bang();
            case '=':
                // documentation nodes
                if (was_bol()) {
                    if (strncmp(lexb.makeShared(lex_p, lex_pend - lex_p), BEGIN_DOC_MARKER, BEGIN_DOC_MARKER.length()) && 
                            Character.isWhitespace(p(lex_p + 5))) {
                        boolean first_p = true;
                        
                        lex_goto_eol();
                        
                        dispatchScanEvent(Tokens.tEMBDOC_BEG);
                        for (;;) {
                            lex_goto_eol();
                            
                            if (!first_p) dispatchScanEvent(Tokens.tEMBDOC);
                            first_p = false;
                            
                            c = nextc();

                            if (c == EOF) {
                                compile_error("embedded document meets end of file");
                                return EOF;
                            }

                            if (c != '=') continue;

                            if (strncmp(lexb.makeShared(lex_p, lex_pend - lex_p), END_DOC_MARKER, END_DOC_MARKER.length()) &&
                                    (lex_p + 3 == lex_pend || Character.isWhitespace(p(lex_p + 3)))) {
                                break;
                            }
                        }
                        lex_goto_eol();
                        dispatchScanEvent(Tokens.tEMBDOC_END);
                        
                        continue loop;
                    }
                }


                determineExpressionState();

                c = nextc();
                if (c == '=') {
                    c = nextc();
                    if (c == '=') {
                        return Tokens.tEQQ;
                    }

                    pushback(c);
                    return Tokens.tEQ;
                }
                if (c == '~') {
                    return Tokens.tMATCH;
                } else if (c == '>') {
                    return Tokens.tASSOC;
                }
                pushback(c);
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
                setState(LexState.EXPR_BEG);
                return ';';
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
                c = nextc();
                if (c == '\n') {
                    spaceSeen = true;
                    continue;
                }
                pushback(c);
                return '\\';
            case '%':
                return percent(spaceSeen);
            case '$':
                return dollar();
            case '@':
                return at();
            case '_':
                if (was_bol() && whole_match_p(END_MARKER, false)) {
                    __end__seen = true;
                    eofp = true;
                    
                    lex_goto_eol();
                    dispatchScanEvent(Tokens.k__END__);
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
                parser.getCurrentScope().isDefined(value) >= 0) {
            setState(LexState.EXPR_END);
        }

        yaccValue = new Token(value);
        identValue = value;
        return result;
    }

    private int getIdentifier(int first) throws IOException {
        if (isMultiByteChar(first)) first = readCodepoint(first, current_enc);
        if (!isIdentifierChar(first)) return first;

        tokenBuffer.append((char) first);

        int c;
        for (c = nextc(); c != EOF; c = nextc()) {
            if (isMultiByteChar(c)) c = readCodepoint(c, current_enc);
            if (!isIdentifierChar(c)) break;

            tokenBuffer.append((char) c);
        }

        pushback(c);

        return first;
    }
    
    private int ampersand(boolean spaceSeen) throws IOException {
        int c = nextc();
        
        switch (c) {
        case '&':
            setState(LexState.EXPR_BEG);
            if ((c = nextc()) == '=') {
                setState(LexState.EXPR_BEG);
                return Tokens.tOP_ASGN;
            }
            pushback(c);
            return Tokens.tANDOP;
        case '=':
            setState(LexState.EXPR_BEG);
            return Tokens.tOP_ASGN;
        }
        pushback(c);

        if (isSpaceArg(c, spaceSeen)) {
            if (isVerbose()) warning("`&' interpreted as argument prefix");
            c = Tokens.tAMPER;
        } else if (isBEG()) {
            c = Tokens.tAMPER;
        } else {
            c = Tokens.tAMPER2;
        }
        
        determineExpressionState();
        
        return c;
    }
    
    private int at() throws IOException {
        int c = nextc();
        int result;
        tokenBuffer.setLength(0);
        tokenBuffer.append('@');
        if (c == '@') {
            tokenBuffer.append('@');
            c = nextc();
            result = Tokens.tCVAR;
        } else {
            result = Tokens.tIVAR;                    
        }
        
        if (c != EOF && (Character.isDigit(c) || !isIdentifierChar(c))) {
            if (tokenBuffer.length() == 1) {
                compile_error("`@" + ((char) c) + "' is not allowed as an instance variable name");
                return EOF;
            }

            compile_error("`@@" + ((char) c) + "' is not allowed as a class variable name");
            return EOF;
        }
        
        if (!isIdentifierChar(c)) {
            pushback(c);
            return '@';
        }

        getIdentifier(c);

        last_state = lex_state;
        setState(LexState.EXPR_END);

        return identifierToken(last_state, result, tokenBuffer.toString().intern());
    }
    
    private int backtick(boolean commandState) throws IOException {
        switch (lex_state) {
        case EXPR_FNAME:
            setState(LexState.EXPR_ENDFN);
            
            return Tokens.tBACK_REF2;
        case EXPR_DOT:
            setState(commandState ? LexState.EXPR_CMDARG : LexState.EXPR_ARG);
            
            return Tokens.tBACK_REF2;
        default:
            lex_strterm = new StringTerm(str_xquote, '\0', '`');
            
            return Tokens.tXSTRING_BEG;
        }
    }
    
    private int bang() throws IOException {
        int c = nextc();

        if (lex_state == LexState.EXPR_FNAME || lex_state == LexState.EXPR_DOT) {
            setState(LexState.EXPR_ARG);
            if (c == '@') return Tokens.tBANG;
        } else {
            setState(LexState.EXPR_BEG);
        }
        
        switch (c) {
        case '=':
            return Tokens.tNEQ;
        case '~':
            return Tokens.tNMATCH;
        default: // Just a plain bang
            pushback(c);
            
            return Tokens.tBANG;
        }
    }
    
    private int caret() throws IOException {
        int c = nextc();
        if (c == '=') {
            setState(LexState.EXPR_BEG);
            
            return Tokens.tOP_ASGN;
        }
        
        determineExpressionState();
        
        pushback(c);
        
        return Tokens.tCARET;
    }

    private int colon(boolean spaceSeen) throws IOException {
        int c = nextc();
        
        if (c == ':') {
            if (isBEG() || lex_state == LexState.EXPR_CLASS || (isARG() && spaceSeen)) {
                setState(LexState.EXPR_BEG);
                return Tokens.tCOLON3;
            }
            setState(LexState.EXPR_DOT);
            return Tokens.tCOLON2;
        }

        if (isEND() || Character.isWhitespace(c)) {
            pushback(c);
            setState(LexState.EXPR_BEG);
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
            pushback(c);
            break;
        }
        
        setState(LexState.EXPR_FNAME);
        return Tokens.tSYMBEG;
    }

    private int comma(int c) throws IOException {
        setState(LexState.EXPR_BEG);
        return c;
    }

    private int doKeyword(LexState state) {
        commandStart = true;

        if (leftParenBegin > 0 && leftParenBegin == parenNest) {
            leftParenBegin = 0;
            parenNest--;
            
            return Tokens.kDO_LAMBDA;
        }

        if (conditionState.isInState()) return Tokens.kDO_COND;

        if (state != LexState.EXPR_CMDARG && cmdArgumentState.isInState()) {
            return Tokens.kDO_BLOCK;
        }
        if (state == LexState.EXPR_ENDARG || state == LexState.EXPR_BEG) {
            return Tokens.kDO_BLOCK;
        }
        return Tokens.kDO;
    }
    
    private int dollar() throws IOException {
        last_state = lex_state;
        setState(LexState.EXPR_END);
        int c = nextc();
        
        switch (c) {
        case '_':       /* $_: last read line string */
            c = nextc();
            if (isIdentifierChar(c)) {
                tokenBuffer.setLength(0);
                tokenBuffer.append("$_");
                getIdentifier(c);
                last_state = lex_state;
                setState(LexState.EXPR_END);

                return identifierToken(last_state, Tokens.tGVAR, tokenBuffer.toString().intern());
            }
            pushback(c);
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
            identValue = "$" + (char) c;
            yaccValue = new Token(identValue, Tokens.tGVAR);
            return Tokens.tGVAR;

        case '-':
            tokenBuffer.setLength(0);
            tokenBuffer.append('$');
            tokenBuffer.append((char) c);
            c = nextc();
            if (isIdentifierChar(c)) {
                tokenBuffer.append((char) c);
            } else {
                pushback(c);
            }
            identValue = tokenBuffer.toString();
            yaccValue = new Token(identValue, Tokens.tGVAR);
            /* xxx shouldn't check if valid option variable */
            return Tokens.tGVAR;

        case '&':       /* $&: last match */
        case '`':       /* $`: string before last match */
        case '\'':      /* $': string after last match */
        case '+':       /* $+: string matches last paren. */
            // Explicit reference to these vars as symbols...
            identValue = "$" + (char) c;
            yaccValue = new Token(identValue);
            if (last_state == LexState.EXPR_FNAME) return Tokens.tGVAR;

            return Tokens.tBACK_REF;
        case '1': case '2': case '3': case '4': case '5': case '6':
        case '7': case '8': case '9':
            tokenBuffer.setLength(0);
            tokenBuffer.append('$');
            do {
                tokenBuffer.append((char) c);
                c = nextc();
            } while (Character.isDigit(c));
            pushback(c);
            if (last_state == LexState.EXPR_FNAME) {
                identValue = tokenBuffer.toString();
                yaccValue = new Token(identValue, Tokens.tGVAR);
                return Tokens.tGVAR;
            }

            identValue = tokenBuffer.toString();
            yaccValue = new Token(identValue);
            return Tokens.tNTH_REF;
        case '0':
            setState(LexState.EXPR_END);

            return identifierToken(last_state, Tokens.tGVAR, ("$" + (char) c).intern());
        default:
            if (!isIdentifierChar(c)) {
                compile_error("`$" + ((char) c) + "' is not allowed as a global variable name");
                return EOF;
            }
        
            // $blah
            tokenBuffer.setLength(0);
            tokenBuffer.append('$');
            getIdentifier(c);
            last_state = lex_state;
            setState(LexState.EXPR_END);

            return identifierToken(last_state, Tokens.tGVAR, tokenBuffer.toString().intern());
        }
    }
    
    private int dot() throws IOException {
        int c;
        
        setState(LexState.EXPR_BEG);
        if ((c = nextc()) == '.') {
            if ((c = nextc()) == '.') return Tokens.tDOT3;
            
            pushback(c);
            
            return Tokens.tDOT2;
        }
        
        pushback(c);
        if (Character.isDigit(c)) compile_error("no .<digit> floating literal anymore; put 0 before dot");
        
        setState(LexState.EXPR_DOT);
        
        return Tokens.tDOT;
    }
    
    private int doubleQuote() throws IOException {
        lex_strterm = new StringTerm(str_dquote, '\0', '"');

        return Tokens.tSTRING_BEG;
    }
    
    private int greaterThan() throws IOException {
        determineExpressionState();

        int c = nextc();

        switch (c) {
        case '=':
            return Tokens.tGEQ;
        case '>':
            if ((c = nextc()) == '=') {
                setState(LexState.EXPR_BEG);
                return Tokens.tOP_ASGN;
            }
            pushback(c);
            
            return Tokens.tRSHFT;
        default:
            pushback(c);
            
            return Tokens.tGT;
        }
    }
    
    private int identifier(int c, boolean commandState) throws IOException {
        if (!isIdentifierChar(c)) {
            String badChar = "\\" + Integer.toOctalString(c & 0xff);
            compile_error("Invalid char `" + badChar + "' ('" + (char) c + "') in expression");
        }
    
        tokenBuffer.setLength(0);
        int first = getIdentifier(c);
        c = nextc();
        boolean lastBangOrPredicate = false;

        // methods 'foo!' and 'foo?' are possible but if followed by '=' it is relop
        if (c == '!' || c == '?') {
            if (!peek('=')) {
                lastBangOrPredicate = true;
                tokenBuffer.append((char) c);
            } else {
                pushback(c);
            }
        } else {
            pushback(c);
        }
        
        int result = 0;

        last_state = lex_state;
        if (lastBangOrPredicate) {
            result = Tokens.tFID;
        } else {
            if (lex_state == LexState.EXPR_FNAME) {
                if ((c = nextc()) == '=') { 
                    int c2 = nextc();

                    if (c2 != '~' && c2 != '>' &&
                            (c2 != '=' || peek('>'))) {
                        result = Tokens.tIDENTIFIER;
                        tokenBuffer.append((char) c);
                        pushback(c2);
                    } else { 
                        pushback(c2);
                        pushback(c);
                    }
                } else {
                    pushback(c);
                }
            }
            if (result == 0 && Character.isUpperCase(first)) {
                result = Tokens.tCONSTANT;
            } else {
                result = Tokens.tIDENTIFIER;
            }
        }

        String tempVal = tokenBuffer.toString().intern();

        if (isLabelPossible(commandState)) {        
            int c2 = nextc();
            if (c2 == ':' && !peek(':')) {
                setState(LexState.EXPR_BEG);
                identValue = tempVal + ':';
                yaccValue = new Token(identValue);
                return Tokens.tLABEL;
            }
            pushback(c2);
        }

        if (lex_state != LexState.EXPR_DOT) {
            Keyword keyword = getKeyword(tempVal); // Is it is a keyword?

            if (keyword != null) {
                LexState state = lex_state; // Save state at time keyword is encountered

                if (keyword == Keyword.NOT) {
                    setState(LexState.EXPR_ARG);
                } else {
                    setState(keyword.state);
                }
                if (state == LexState.EXPR_FNAME) {
                    identValue = keyword.name;
                    yaccValue = new Token(identValue);
                } else {
                    yaccValue = new Token(tempVal);
                    if (keyword.id0 == Tokens.kDO) return doKeyword(state);
                }

                if (state == LexState.EXPR_BEG || state == LexState.EXPR_VALUE) return keyword.id0;

                if (keyword.id0 != keyword.id1) setState(LexState.EXPR_BEG);

                return keyword.id1;
            }
        }

        if (isBEG() || lex_state == LexState.EXPR_DOT || isARG()) {
            setState(commandState ? LexState.EXPR_CMDARG : LexState.EXPR_ARG);
        } else if (lex_state == LexState.EXPR_FNAME) {
            setState(LexState.EXPR_ENDFN);
        } else {
            setState(LexState.EXPR_END);
        }
        
        return identifierToken(last_state, result, tempVal);
    }

    private int leftBracket(boolean spaceSeen) throws IOException {
        parenNest++;
        int c = '[';
        if (lex_state == LexState.EXPR_FNAME || lex_state == LexState.EXPR_DOT) {
            setState(LexState.EXPR_ARG);
            
            if ((c = nextc()) == ']') {
                if (peek('=')) {
                    nextc();
                    return Tokens.tASET;
                }
                return Tokens.tAREF;
            }
            pushback(c);
            return '[';
        } else if (isBEG() || (isARG() && spaceSeen)) {
            c = Tokens.tLBRACK;
        }

        setState(LexState.EXPR_BEG);
        conditionState.stop();
        cmdArgumentState.stop();
        
        return c;
    }
    
    private int leftCurly() {
        if (leftParenBegin > 0 && leftParenBegin == parenNest) {
            setState(LexState.EXPR_BEG);
            leftParenBegin = 0;
            parenNest--;
            conditionState.stop();
            cmdArgumentState.stop();
            return Tokens.tLAMBEG;
        }

        char c;
        if (isARG() || lex_state == LexState.EXPR_END || (lex_state == LexState.EXPR_ENDFN)) { // block (primary)
            c = Tokens.tLCURLY;
        } else if (lex_state == LexState.EXPR_ENDARG) { // block (expr)
            c = Tokens.tLBRACE_ARG;
        } else { // hash
            c = Tokens.tLBRACE;
        }

        conditionState.stop();
        cmdArgumentState.stop();
        setState(LexState.EXPR_BEG);
        
        if (c != Tokens.tLBRACE) commandStart = true;
        
        return c;
    }

    private int leftParen(boolean spaceSeen) throws IOException {
        int result = Tokens.tLPAREN2;
        if (isBEG()) {
            result = Tokens.tLPAREN;
        } else if (spaceSeen) {
            // ENEBO: 1.9 is IS_ARG, but we need to break apart for 1.8 support.
            if (lex_state == LexState.EXPR_CMDARG) {
                result = Tokens.tLPAREN_ARG;
            } else if (lex_state == LexState.EXPR_ARG) {
                result = Tokens.tLPAREN_ARG;
            }
        }

        parenNest++;
        conditionState.stop();
        cmdArgumentState.stop();
        setState(LexState.EXPR_BEG);
        
        return result;
    }
    
    private int lessThan(boolean spaceSeen) throws IOException {
        int c = nextc();
        if (c == '<' && lex_state != LexState.EXPR_DOT && lex_state != LexState.EXPR_CLASS &&
                !isEND() && (!isARG() || spaceSeen)) {
            int tok = hereDocumentIdentifier();
            
            if (tok != 0) {
                return tok;
            }
        }
        
        determineExpressionState();
        
        switch (c) {
        case '=':
            if ((c = nextc()) == '>') {
                return Tokens.tCMP;
            }
            pushback(c);
            return Tokens.tLEQ;
        case '<':
            if ((c = nextc()) == '=') {
                setState(LexState.EXPR_BEG);
                return Tokens.tOP_ASGN;
            }
            pushback(c);
            return Tokens.tLSHFT;
        default:
            pushback(c);
            return Tokens.tLT;
        }
    }
    
    private int minus(boolean spaceSeen) throws IOException {
        int c = nextc();
        
        if (lex_state == LexState.EXPR_FNAME || lex_state == LexState.EXPR_DOT) {
            setState(LexState.EXPR_ARG);
            if (c == '@') {
                return Tokens.tUMINUS;
            }
            pushback(c);
            return Tokens.tMINUS;
        }
        if (c == '=') {
            setState(LexState.EXPR_BEG);

            return Tokens.tOP_ASGN;
        }
        if (c == '>') {
            setState(LexState.EXPR_ARG);
            return Tokens.tLAMBDA;
        }
        if (isBEG() || isSpaceArg(c, spaceSeen)) {
            if (isARG()) arg_ambiguous();
            setState(LexState.EXPR_BEG);
            pushback(c);
            if (Character.isDigit(c)) {
                return Tokens.tUMINUS_NUM;
            }
            return Tokens.tUMINUS;
        }
        setState(LexState.EXPR_BEG);
        pushback(c);
        
        return Tokens.tMINUS;
    }

    private int percent(boolean spaceSeen) throws IOException {
        if (isBEG()) return parseQuote(nextc());

        int c = nextc();

        if (c == '=') {
            setState(LexState.EXPR_BEG);
            return Tokens.tOP_ASGN;
        }
        
        if (isSpaceArg(c, spaceSeen)) return parseQuote(c);
        
        determineExpressionState();
        
        pushback(c);
        return Tokens.tPERCENT;
    }

    private int pipe() throws IOException {
        int c = nextc();
        
        switch (c) {
        case '|':
            setState(LexState.EXPR_BEG);
            if ((c = nextc()) == '=') {
                setState(LexState.EXPR_BEG);
                return Tokens.tOP_ASGN;
            }
            pushback(c);
            return Tokens.tOROP;
        case '=':
            setState(LexState.EXPR_BEG);
            
            return Tokens.tOP_ASGN;
        default:
            determineExpressionState();
            
            pushback(c);
            
            return Tokens.tPIPE;
        }
    }
    
    private int plus(boolean spaceSeen) throws IOException {
        int c = nextc();
        if (lex_state == LexState.EXPR_FNAME || lex_state == LexState.EXPR_DOT) {
            setState(LexState.EXPR_ARG);
            if (c == '@') return Tokens.tUPLUS;

            pushback(c);

            return Tokens.tPLUS;
        }
        
        if (c == '=') {
            setState(LexState.EXPR_BEG);

            return Tokens.tOP_ASGN;
        }
        
        if (isBEG() || isSpaceArg(c, spaceSeen)) {
            if (isARG()) arg_ambiguous();
            setState(LexState.EXPR_BEG);
            pushback(c);
            if (Character.isDigit(c)) {
                c = '+';
                return parseNumber(c);
            }

            return Tokens.tUPLUS;
        }
        
        setState(LexState.EXPR_BEG);
        pushback(c);

        return Tokens.tPLUS;
    }
    
    private int questionMark() throws IOException {
        int c;
        
        if (isEND()) {
            setState(LexState.EXPR_VALUE);

            return '?';
        }
        
        c = nextc();
        if (c == EOF) {
            compile_error("incomplete character syntax");
            return EOF;
        }

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
                    warn("invalid character syntax; use ?\\" + c2);
                }
            }
            pushback(c);
            setState(LexState.EXPR_VALUE);
            return '?';
        } else if (isASCII()) {
            ByteList buffer = new ByteList(1);
            if (!tokenAddMBC(c, buffer)) return EOF;

            setState(LexState.EXPR_END);
            yaccValue = new Token(buffer);
            return Tokens.tCHAR;
        } else if (isIdentifierChar(c) && !peek('\n') && isNext_identchar()) {
            pushback(c);
            setState(LexState.EXPR_VALUE);

            return '?';
        } else if (c == '\\') {
            if (peek('u')) {
                nextc(); // Eat 'u'
                ByteList oneCharBL = new ByteList(2);
                c = readUTFEscape(oneCharBL, false, false);
                
                if (c >= 0x80) {
                    tokenAddMBC(c, oneCharBL);
                } else {
                    oneCharBL.append(c);
                }
                
                setState(LexState.EXPR_END);
                yaccValue = new Token(oneCharBL);
                
                return org.jruby.parser.Tokens.tINTEGER; // FIXME: This should be something else like a tCHAR in 1.9/2.0
            } else {
                c = readEscape();
            }
        }
        
        setState(LexState.EXPR_END);
        // TODO: this isn't handling multibyte yet
        ByteList oneCharBL = new ByteList(1);
        oneCharBL.append(c);
        yaccValue = new Token(oneCharBL);
        return Tokens.tCHAR;
    }
    
    private int rightBracket() {
        parenNest--;
        conditionState.restart();
        cmdArgumentState.restart();
        setState(LexState.EXPR_ENDARG);
        return Tokens.tRBRACK;
    }

    private int rightCurly() {
        conditionState.restart();
        cmdArgumentState.restart();
        setState(LexState.EXPR_ENDARG);
        int tok = /*braceNest != 0 ? Tokens.tSTRING_DEND : */ Tokens.tRCURLY;
        return tok;
    }

    private int rightParen() {
        parenNest--;
        conditionState.restart();
        cmdArgumentState.restart();
        setState(LexState.EXPR_ENDFN);
        return Tokens.tRPAREN;
    }
    
    private int singleQuote() throws IOException {
        lex_strterm = new StringTerm(str_squote, '\0', '\'');
        return Tokens.tSTRING_BEG;
    }
    
    private int slash(boolean spaceSeen) throws IOException {
        if (isBEG()) {
            lex_strterm = new StringTerm(str_regexp, '\0', '/');
            
            return Tokens.tREGEXP_BEG;
        }
        
        int c = nextc();
        
        if (c == '=') {
            setState(LexState.EXPR_BEG);
            
            return Tokens.tOP_ASGN;
        }
        pushback(c);
        if (isSpaceArg(c, spaceSeen)) {
            arg_ambiguous();
            lex_strterm = new StringTerm(str_regexp, '\0', '/');
            
            return Tokens.tREGEXP_BEG;
        }
        
        determineExpressionState();
        
        return Tokens.tDIVIDE;
    }

    private int star(boolean spaceSeen) throws IOException {
        int c = nextc();
        
        switch (c) {
        case '*':
            if ((c = nextc()) == '=') {
                setState(LexState.EXPR_BEG);
                return Tokens.tOP_ASGN;
            }
            pushback(c);

            if (isSpaceArg(c, spaceSeen)) {
                if (isVerbose()) warning("`**' interpreted as argument prefix");
                c = Tokens.tDSTAR;
            } else if (isBEG()) {
                c = Tokens.tDSTAR;
            } else {
                warn_balanced(c, spaceSeen, "*", "argument prefix");
                c = Tokens.tPOW;
            }
            break;
        case '=':
            setState(LexState.EXPR_BEG);
            return Tokens.tOP_ASGN;
        default:
            pushback(c);
            if (isSpaceArg(c, spaceSeen)) {
                if (isVerbose()) warning("`*' interpreted as argument prefix");
                c = Tokens.tSTAR;
            } else if (isBEG()) {
                c = Tokens.tSTAR;
            } else {
                c = Tokens.tSTAR2;
            }
        }
        
        determineExpressionState();
        return c;
    }

    private int tilde() throws IOException {
        int c;
        
        if (lex_state == LexState.EXPR_FNAME || lex_state == LexState.EXPR_DOT) {
            if ((c = nextc()) != '@') pushback(c);
            setState(LexState.EXPR_ARG);
        } else {
            setState(LexState.EXPR_BEG);
        }
        
        return Tokens.tTILDE;
    }

    /**
     *  Parse a number from the input stream.
     *
     *@param c The first character of the number.
     *@return A int constant wich represents a token.
     */
    private int parseNumber(int c) throws IOException {
        setState(LexState.EXPR_END);

        tokenBuffer.setLength(0);

        if (c == '-') {
        	tokenBuffer.append((char) c);
            c = nextc();
        } else if (c == '+') {
        	// We don't append '+' since Java number parser gets confused
            c = nextc();
        }
        
        int nondigit = 0;

        if (c == '0') {
            int startLen = tokenBuffer.length();

            switch (c = nextc()) {
                case 'x' :
                case 'X' : //  hexadecimal
                    c = nextc();
                    if (isHexChar(c)) {
                        for (;; c = nextc()) {
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
                    pushback(c);

                    if (tokenBuffer.length() == startLen) {
                        compile_error("Hexadecimal number without hex-digits.");
                    } else if (nondigit != '\0') {
                        compile_error("Trailing '_' in number.");
                    }
                    return setIntegerLiteral(tokenBuffer.toString(), numberLiteralSuffix(SUFFIX_ALL));
                case 'b' :
                case 'B' : // binary
                    c = nextc();
                    if (c == '0' || c == '1') {
                        for (;; c = nextc()) {
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
                    pushback(c);

                    if (tokenBuffer.length() == startLen) {
                        compile_error("Binary number without digits.");
                    } else if (nondigit != '\0') {
                        compile_error("Trailing '_' in number.");
                    }
                    return setIntegerLiteral(tokenBuffer.toString(), numberLiteralSuffix(SUFFIX_ALL));
                case 'd' :
                case 'D' : // decimal
                    c = nextc();
                    if (Character.isDigit(c)) {
                        for (;; c = nextc()) {
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
                    pushback(c);

                    if (tokenBuffer.length() == startLen) {
                        compile_error("Binary number without digits.");
                    } else if (nondigit != '\0') {
                        compile_error("Trailing '_' in number.");
                    }
                    return setIntegerLiteral(tokenBuffer.toString(), numberLiteralSuffix(SUFFIX_ALL));
                case 'o':
                case 'O':
                    c = nextc();
                case '0': case '1': case '2': case '3': case '4': //Octal
                case '5': case '6': case '7': case '_': 
                    for (;; c = nextc()) {
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
                        pushback(c);

                        if (nondigit != '\0') compile_error("Trailing '_' in number.");

                        return setIntegerLiteral(tokenBuffer.toString(), numberLiteralSuffix(SUFFIX_ALL));
                    }
                case '8' :
                case '9' :
                    compile_error("Illegal octal digit.");
                case '.' :
                case 'e' :
                case 'E' :
                	tokenBuffer.append('0');
                    break;
                default :
                    pushback(c);
                    yaccValue = parser.getRuntime().newFixnum(0);
                    return Tokens.tINTEGER;
            }
        }

        boolean seen_point = false;
        boolean seen_e = false;

        for (;; c = nextc()) {
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
                        pushback(c);
                        compile_error("Trailing '_' in number.");
                    } else if (seen_point || seen_e) {
                        pushback(c);
                        return getNumberLiteral(tokenBuffer.toString(), seen_e, seen_point, nondigit);
                    } else {
                    	int c2;
                        if (!Character.isDigit(c2 = nextc())) {
                            pushback(c2);
                        	pushback('.');
                            if (c == '_') { 
                            		// Enebo:  c can never be antrhign but '.'
                            		// Why did I put this here?
                            } else {
                                return getNumberLiteral(tokenBuffer.toString(), seen_e, seen_point, nondigit);
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
                        compile_error("Trailing '_' in number.");
                    } else if (seen_e) {
                        pushback(c);
                        return getNumberLiteral(tokenBuffer.toString(), seen_e, seen_point, nondigit);
                    } else {
                        tokenBuffer.append((char) c);
                        seen_e = true;
                        nondigit = c;
                        c = nextc();
                        if (c == '-' || c == '+') {
                            tokenBuffer.append((char) c);
                            nondigit = c;
                        } else {
                            pushback(c);
                        }
                    }
                    break;
                case '_' : //  '_' in number just ignored
                    if (nondigit != '\0') {
                        compile_error("Trailing '_' in number.");
                    }
                    nondigit = c;
                    break;
                default :
                    pushback(c);
                    return getNumberLiteral(tokenBuffer.toString(), seen_e, seen_point, nondigit);
            }
        }
    }

    // MRI: This is decode_num: chunk
    private int getNumberLiteral(String number, boolean seen_e, boolean seen_point, int nondigit) throws IOException {
        if (nondigit != '\0') compile_error("Trailing '_' in number.");

        boolean isFloat = seen_e || seen_point;
        if (isFloat) return getFloatToken(number, numberLiteralSuffix(seen_e ? SUFFIX_I : SUFFIX_ALL));

        return setIntegerLiteral(number, numberLiteralSuffix(SUFFIX_ALL));
    }

    private int setNumberLiteral(String number, int type, int suffix) {
        if ((suffix & SUFFIX_I) != 0) type = Tokens.tIMAGINARY;

        return type;
    }

    private int setIntegerLiteral(String value, int suffix) {
        int type = (suffix & SUFFIX_R) != 0 ? Tokens.tRATIONAL : Tokens.tINTEGER;

        return setNumberLiteral(value, type, suffix);
    }

    private int numberLiteralSuffix(int mask) throws IOException {
        int c = nextc();

        if (c == 'i') return (mask & SUFFIX_I) != 0 ?  mask & SUFFIX_I : 0;

        if (c == 'r') {
            int result = 0;
            if ((mask & SUFFIX_R) != 0) result |= (mask & SUFFIX_R);

            if (peek('i') && (mask & SUFFIX_I) != 0) {
                nextc();
                result |= (mask & SUFFIX_I);
            }

            return result;
        }
        pushback(c);

        return 0;
    }



    // Note: parser_tokadd_utf8 variant just for regexp literal parsing.  This variant is to be
    // called when string_literal and regexp_literal.
    public void readUTFEscapeRegexpLiteral(ByteList buffer) throws IOException {
        buffer.append('\\');
        buffer.append('u');

        if (peek('{')) { // handle \\u{...}
            do {
                buffer.append(nextc());
                if (scanHexLiteral(buffer, 6, false, "invalid Unicode escape") > 0x10ffff) {
                    compile_error("invalid Unicode codepoint (too large)");
                }
            } while (peek(' ') || peek('\t'));

            int c = nextc();
            if (c != '}') {
                compile_error("unterminated Unicode escape");
            }
            buffer.append((char) c);
        } else { // handle \\uxxxx
            scanHexLiteral(buffer, 4, true, "Invalid Unicode escape");
        }
        buffer.setEncoding(UTF8_ENCODING);
    }

    private byte[] mbcBuf = new byte[6];

    // mri: parser_tokadd_mbchar
    // This is different than MRI in that we return a boolean since we only care whether it was added
    // or not.  The MRI version returns the byte supplied which is never used as a value.
    public boolean tokenAddMBC(int first_byte, ByteList buffer) {
        int length = precise_mbclen();

        if (length <= 0) {
            compile_error("invalid multibyte char (" + getEncoding().getName() + ")");
            return false;
        }

        tokAdd(first_byte, buffer);                  // add first byte since we have it.
        lex_p += length - 1;                         // we already read first byte so advance pointer for remainder
        if (length > 1) tokCopy(length - 1, buffer); // copy next n bytes over.

        return true;
    }

    public void tokCopy(int length, ByteList buffer) {
        buffer.append(lexb, lex_p - length, length);
    }


    public void tokAdd(int value, ByteList buffer) {
        buffer.append((byte) value);
    }

    public int precise_mbclen() {
        byte[] data = lexb.getUnsafeBytes();
        int p = lex_p - 1;
        int begin = lexb.begin();

        return current_enc.length(data, begin+p, lex_pend-p);
    }

    public void tokenAddMBCFromSrc(int c, ByteList buffer) throws IOException {
        // read bytes for length of character
        int length = buffer.getEncoding().length((byte)c);
        buffer.append((byte)c);
        for (int off = 0; off < length - 1; off++) {
            buffer.append((byte)nextc());
        }
    }

    // MRI: parser_tokadd_utf8 sans regexp literal parsing
    public int readUTFEscape(ByteList buffer, boolean stringLiteral, boolean symbolLiteral) throws IOException {
        int codepoint;
        int c;

        if (peek('{')) { // handle \\u{...}
            do {
                nextc(); // Eat curly or whitespace
                codepoint = scanHex(6, false, "invalid Unicode escape");
                if (codepoint > 0x10ffff) {
                    compile_error("invalid Unicode codepoint (too large)");
                }
                if (buffer != null) readUTF8EscapeIntoBuffer(codepoint, buffer, stringLiteral);
            } while (peek(' ') || peek('\t'));

            c = nextc();
            if (c != '}') {
                compile_error("unterminated Unicode escape");
            }
        } else { // handle \\uxxxx
            codepoint = scanHex(4, true, "Invalid Unicode escape");
            if (buffer != null) readUTF8EscapeIntoBuffer(codepoint, buffer, stringLiteral);
        }

        return codepoint;
    }
    
    private void readUTF8EscapeIntoBuffer(int codepoint, ByteList buffer, boolean stringLiteral) {
        if (codepoint >= 0x80) {
            buffer.setEncoding(UTF8_ENCODING);
            if (stringLiteral) tokenAddMBC(codepoint, buffer);
        } else if (stringLiteral) {
            buffer.append((char) codepoint);
        }
    }    
    
    public int readEscape() throws IOException {
        int c = nextc();

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
                pushback(c);
                return scanOct(3);
            case 'x' : // hex constant
                return scanHex(2, false, "Invalid escape character syntax");
            case 'b' : // backspace
                return '\010';
            case 's' : // space
                return ' ';
            case 'M' :
                if ((c = nextc()) != '-') {
                    compile_error("Invalid escape character syntax");
                } else if ((c = nextc()) == '\\') {
                    return (char) (readEscape() | 0x80);
                } else if (c == EOF) {
                    compile_error("Invalid escape character syntax");
                } 
                return (char) ((c & 0xff) | 0x80);
            case 'C' :
                if ((c = nextc()) != '-') {
                    compile_error("Invalid escape character syntax");
                }
            case 'c' :
                if ((c = nextc()) == '\\') {
                    c = readEscape();
                } else if (c == '?') {
                    return '\177';
                } else if (c == EOF) {
                    compile_error("Invalid escape character syntax");
                }
                return (char) (c & 0x9f);
            case EOF :
                compile_error("Invalid escape character syntax");
            default :
                return c;
        }
    }

    /**
     * Read up to count hexadecimal digits and store those digits in a token buffer.  If strict is
     * provided then count number of hex digits must be present. If no digits can be read a syntax
     * exception will be thrown.  This will also return the codepoint as a value so codepoint
     * ranges can be checked.
     */
    private char scanHexLiteral(ByteList buffer, int count, boolean strict, String errorMessage)
            throws IOException {
        int i = 0;
        char hexValue = '\0';

        for (; i < count; i++) {
            int h1 = nextc();

            if (!isHexChar(h1)) {
                pushback(h1);
                break;
            }

            buffer.append(h1);

            hexValue <<= 4;
            hexValue |= Integer.parseInt("" + (char) h1, 16) & 15;
        }

        // No hex value after the 'x'.
        if (i == 0 || strict && count != i) compile_error(errorMessage);

        return hexValue;
    }

    /**
     * Read up to count hexadecimal digits.  If strict is provided then count number of hex
     * digits must be present. If no digits can be read a syntax exception will be thrown.
     */
    private int scanHex(int count, boolean strict, String errorMessage) throws IOException {
        int i = 0;
        int hexValue = '\0';

        for (; i < count; i++) {
            int h1 = nextc();

            if (!isHexChar(h1)) {
                pushback(h1);
                break;
            }

            hexValue <<= 4;
            hexValue |= Integer.parseInt("" + (char) h1, 16) & 15;
        }

        // No hex value after the 'x'.
        if (i == 0 || (strict && count != i)) compile_error(errorMessage);

        return hexValue;
    }

    private char scanOct(int count) throws IOException {
        char value = '\0';

        for (int i = 0; i < count; i++) {
            int c = nextc();

            if (!isOctChar(c)) {
                pushback(c);
                break;
            }

            value <<= 3;
            value |= Integer.parseInt("" + (char) c, 8);
        }

        return value;
    } 
}
