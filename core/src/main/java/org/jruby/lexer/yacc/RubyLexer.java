/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.lexer.yacc;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import org.jcodings.Encoding;
import org.joni.Matcher;
import org.joni.Option;
import org.joni.Regex;
import org.jruby.Ruby;
import org.jruby.RubyRegexp;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BignumNode;
import org.jruby.ast.ComplexNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FloatNode;
import org.jruby.ast.Node;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.NumericNode;
import org.jruby.ast.RationalNode;
import org.jruby.ast.StrNode;
import org.jruby.common.IRubyWarnings;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.lexer.LexerSource;
import org.jruby.lexer.yacc.SyntaxException.PID;
import org.jruby.parser.ParserSupport;
import org.jruby.parser.RubyParser;
import org.jruby.parser.Tokens;
import org.jruby.util.ByteList;
import org.jruby.util.SafeDoubleParser;
import org.jruby.util.StringSupport;
import org.jruby.util.cli.Options;

import static org.jruby.lexer.LexingCommon.ASCII8BIT_ENCODING;
import static org.jruby.lexer.LexingCommon.BEGIN_DOC_MARKER;
import static org.jruby.lexer.LexingCommon.CODING;
import static org.jruby.lexer.LexingCommon.END_DOC_MARKER;
import static org.jruby.lexer.LexingCommon.END_MARKER;
import static org.jruby.lexer.LexingCommon.EOF;
import static org.jruby.lexer.LexingCommon.STR_FUNC_INDENT;
import static org.jruby.lexer.LexingCommon.STR_FUNC_QWORDS;
import static org.jruby.lexer.LexingCommon.STR_FUNC_REGEXP;
import static org.jruby.lexer.LexingCommon.SUFFIX_ALL;
import static org.jruby.lexer.LexingCommon.SUFFIX_I;
import static org.jruby.lexer.LexingCommon.SUFFIX_R;
import static org.jruby.lexer.LexingCommon.USASCII_ENCODING;
import static org.jruby.lexer.LexingCommon.UTF8_ENCODING;
import static org.jruby.lexer.LexingCommon.isHexChar;
import static org.jruby.lexer.LexingCommon.isOctChar;
import static org.jruby.lexer.LexingCommon.parseMagicComment;
import static org.jruby.lexer.LexingCommon.str_dquote;
import static org.jruby.lexer.LexingCommon.str_dsym;
import static org.jruby.lexer.LexingCommon.str_regexp;
import static org.jruby.lexer.LexingCommon.str_squote;
import static org.jruby.lexer.LexingCommon.str_ssym;
import static org.jruby.lexer.LexingCommon.str_xquote;

/*
 * This is a port of the MRI lexer to Java.
 */
public class RubyLexer {
    private static final HashMap<String, Keyword> map;

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

    public Encoding getEncoding() {
        return current_enc;
    }

    private BignumNode newBignumNode(String value, int radix) {
        return new BignumNode(getPosition(), new BigInteger(value, radix));
    }

    private FixnumNode newFixnumNode(String value, int radix) throws NumberFormatException {
        return new FixnumNode(getPosition(), Long.parseLong(value, radix));
    }
    
    private RationalNode newRationalNode(String value, int radix) throws NumberFormatException {
        return new RationalNode(getPosition(), Long.parseLong(value, radix), 1);
    }
    
    private ComplexNode newComplexNode(NumericNode number) {
        return new ComplexNode(getPosition(), number);
    }
    
    private void ambiguousOperator(String op, String syn) {
        warnings.warn(ID.AMBIGUOUS_ARGUMENT, "`" + op + "' after local variable is interpreted as binary operator\nevent though it seems like \"" + syn + "\"");
    }
    
    private void warn_balanced(int c, boolean spaceSeen, String op, String syn) {
        if (false && last_state != LexState.EXPR_CLASS && last_state != LexState.EXPR_DOT &&
                last_state != LexState.EXPR_FNAME && last_state != LexState.EXPR_ENDFN &&
                last_state != LexState.EXPR_ENDARG && spaceSeen && !Character.isWhitespace(c)) {
            ambiguousOperator(op, syn);
        }
    }

    private int numberLiteralSuffix(int mask) throws IOException {
        int c = nextc();
        
        if (c == 'i') return (mask & SUFFIX_I) != 0 ?  mask & SUFFIX_I : 0;
        
        if (c == 'r') {
            int result = 0;
            if ((mask & SUFFIX_R) != 0) result |= (mask & SUFFIX_R);
            
            if (peek('i') && (mask & SUFFIX_I) != 0) {
                c = nextc();
                result |= (mask & SUFFIX_I);
            }
            
            return result;
        }
        pushback(c);

        return 0;
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
        EXPR_FNAME, EXPR_DOT, EXPR_CLASS, EXPR_VALUE, EXPR_ENDFN, EXPR_LABELARG
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
    private LexState last_state;
    public ISourcePosition tokline;

    public void newtok(boolean unreadOnce) {
        tokline = getPosition();

        tokp = lex_p - (unreadOnce ? 1 : 0); // We use tokp of ripper to mark beginning of tokens.
    }

    public boolean tokadd_ident(int c) {
        do {
            if (!tokadd_mbchar(c)) return false;
            c = nextc();
        } while (isIdentifierChar(c));
        pushback(c);

        return true;
    }

    public ByteList createTokenByteList() {
        return new ByteList(lexb.unsafeBytes(), lexb.begin() + tokp, lex_p - tokp, current_enc, false);
    }

    public String createTokenString() {
        byte[] bytes = lexb.getUnsafeBytes();
        int begin = lexb.begin();
        Charset charset;

        // FIXME: We should be able to move some faster non-exception cache using Encoding.isDefined
        try {
            charset = current_enc.getCharset();
            if (charset != null) return new String(bytes, begin + tokp, lex_p - tokp, charset);
        } catch (UnsupportedCharsetException e) {}


        return new String(bytes, begin + tokp, lex_p - tokp);
    }

    public int tokenize_ident(int result) {
        // FIXME: Get token from newtok index to lex_p?
        String value = createTokenString();

        if ((last_state != LexState.EXPR_DOT || last_state != LexState.EXPR_FNAME) &&
                parserSupport.getCurrentScope().isDefined(value) >= 0) {
            setState(LexState.EXPR_END);
        }

        yaccValue = value.intern();
        return result;
    }

    private StackState conditionState = new StackState();
    private StackState cmdArgumentState = new StackState();
    private StrTerm lex_strterm;
    public boolean commandStart;

    // Count of nested parentheses
    private int parenNest = 0;
    private int braceNest = 0;

    private int leftParenBegin = 0;
    public boolean inKwarg = false;

    public int incrementParenNest() {
        parenNest++;

        return parenNest;
    }

    public int getBraceNest() {
        return braceNest;
    }

    public void setBraceNest(int nest) {
        braceNest = nest;
    }

    public int getLeftParenBegin() {
        return leftParenBegin;
    }

    public void setLeftParenBegin(int value) {
        leftParenBegin = value;
    }

    public RubyLexer(ParserSupport support, LexerSource source) {
        this.parserSupport = support;
        this.src = source;
        reset();
    }
    
    public final void reset() {
        token = 0;
        yaccValue = null;
        setState(null);
        resetStacks();
        lex_strterm = null;
        commandStart = true;
        commandStart = true;
        parenNest = 0;
        braceNest = 0;
        tokp = 0;
        last_cr_line = -1;

        parser_prepare();
    }

    int last_cr_line;
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
        if (c == '\r') {
            if (peek('\n')) {
                lex_p++;
                c = '\n';
            } else if (ruby_sourceline > last_cr_line) {
                last_cr_line = ruby_sourceline;
                warnings.warn(ID.VOID_VALUE_EXPRESSION, src.getFilename(), ruby_sourceline, "encountered \\\\r in middle of line, treated as a mere space");
                c = ' ';
            }
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
        return ruby_sourceline + src.getLineOffset() - 1;
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
        throw new SyntaxException(PID.BAD_HEX_NUMBER, getPosition(), lexb.toString(), message);
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

    public int nextToken() throws IOException {
        token = yylex();
        return token == EOF ? 0 : token;
    }
    
    /**
     * Last token read from the lexer at the end of a call to yylex()
     * 
     * @return last token read
     */
    public int token() {
        return token;
    }
    
    /**
     * Value of last token (if it is a token which has a value).
     * 
     * @return value of last value-laden token
     */
    public Object value() {
        return yaccValue;
    }

    public ISourcePosition getPosition() {
        if (tokline != null && lineno() == tokline.getLine()) return tokline;
        return new SimpleSourcePosition(src.getFilename(), lineno());
    }

    public ISourcePosition getPosition(ISourcePosition startPosition) {
        if (startPosition != null) return startPosition;

        if (tokline != null && lineno() == tokline.getLine()) return tokline;

        return new SimpleSourcePosition(src.getFilename(), lineno());
    }

    public String getCurrentLine() {
        return lex_lastline.toString();
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
        if (n < 0) return false;
        if (n > 0 && p(p+len) != '\n') {
            if (p(p+len) != '\r') return false;
            if (n == 1 || p(p+len+1) != '\n') return false;
        }

        return strncmp(eos, lexb.makeShared(p, len), len);
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

    private void setEncoding(ByteList name) {
        Ruby runtime = parserSupport.getConfiguration().getRuntime();
        Encoding newEncoding = runtime.getEncodingService().loadEncoding(name);

        if (newEncoding == null) throw runtime.newArgumentError("unknown encoding name: " + name.toString());
        if (!newEncoding.isAsciiCompatible()) throw runtime.newArgumentError(name.toString() + " is not ASCII compatible");

        setEncoding(newEncoding);
    }

    // FIXME: This is mucked up...current line knows it's own encoding so that must be changed.  but we also have two
    // other sources.  I am thinking current_enc should be removed in favor of src since it needs to know encoding to
    // provide next line.
    public void setEncoding(Encoding encoding) {
        current_enc = encoding;
        src.setEncoding(encoding);
        lexb.setEncoding(encoding);
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

    private void printState() {
        if (lex_state == null) {
            System.out.println("NULL");
        } else {
            System.out.println(lex_state);
        }
    }

    public LexState getState() {
        return lex_state;
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

    private boolean isBEG() {
        return lex_state == LexState.EXPR_BEG || lex_state == LexState.EXPR_MID ||
                lex_state == LexState.EXPR_CLASS || lex_state == LexState.EXPR_VALUE ||
                lex_state == LexState.EXPR_LABELARG;
    }
    
    private boolean isEND() {
        return lex_state == LexState.EXPR_END || lex_state == LexState.EXPR_ENDARG ||
                (lex_state == LexState.EXPR_ENDFN);
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

    private int considerComplex(int token, int suffix) {
        if ((suffix & SUFFIX_I) == 0) {
            return token;
        } else {
            yaccValue = newComplexNode((NumericNode) yaccValue);
            return RubyParser.tIMAGINARY;
        }
    }

    private int getFloatToken(String number, int suffix) {
        if ((suffix & SUFFIX_R) != 0) {
            BigDecimal bd = new BigDecimal(number);
            BigDecimal denominator = BigDecimal.ONE.scaleByPowerOfTen(bd.scale());
            BigDecimal numerator = bd.multiply(denominator);

            try {
                yaccValue = new RationalNode(getPosition(), numerator.longValueExact(), denominator.longValueExact());
            } catch (ArithmeticException ae) {
                // FIXME: Rational supports Bignum numerator and denominator
                throw new SyntaxException(PID.RATIONAL_OUT_OF_RANGE, getPosition(), getCurrentLine(), "Rational (" + numerator + "/" + denominator + ") out of range.");
            }
            return considerComplex(Tokens.tRATIONAL, suffix);
        }

        double d;
        try {
            d = SafeDoubleParser.parseDouble(number);
        } catch (NumberFormatException e) {
            warnings.warn(ID.FLOAT_OUT_OF_RANGE, getPosition(), "Float " + number + " out of range.");

            d = number.startsWith("-") ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }
        yaccValue = new FloatNode(getPosition(), d);
        return considerComplex(Tokens.tFLOAT, suffix);
    }

    private int getIntegerToken(String value, int radix, int suffix) {
        Node literalValue;

        if ((suffix & SUFFIX_R) != 0) {
            literalValue = newRationalNode(value, radix);
        } else {
            try {
                literalValue = newFixnumNode(value, radix);
            } catch (NumberFormatException e) {
                literalValue = newBignumNode(value, radix);
            }
        }

        yaccValue = literalValue;
        return considerComplex(Tokens.tINTEGER, suffix);
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
        return !eofp && (Character.isLetterOrDigit(c) || c == '_' || isMultiByteChar(c));
    }

    public boolean isASCII(int c) {
        return Encoding.isMbcAscii((byte)c);
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
    public StrNode createStr(ByteList buffer, int flags) {
        Encoding bufferEncoding = buffer.getEncoding();
        int codeRange = StringSupport.codeRangeScan(bufferEncoding, buffer);

        if ((flags & STR_FUNC_REGEXP) == 0 && bufferEncoding.isAsciiCompatible()) {
            // If we have characters outside 7-bit range and we are still ascii then change to ascii-8bit
            if (codeRange == StringSupport.CR_7BIT) {
                // Do nothing like MRI
            } else if (getEncoding() == USASCII_ENCODING &&
                    bufferEncoding != UTF8_ENCODING) {
                codeRange = ParserSupport.associateEncoding(buffer, ASCII8BIT_ENCODING, codeRange);
            }
        }

        return new StrNode(getPosition(), buffer, codeRange);
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
            begin = nextc();
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
            yaccValue = "%"+ (shortHand ? (""+end) : ("" + c + begin));
            return Tokens.tSTRING_BEG;

        case 'q':
            lex_strterm = new StringTerm(str_squote, begin, end);
            yaccValue = "%"+c+begin;
            return Tokens.tSTRING_BEG;

        case 'W':
            lex_strterm = new StringTerm(str_dquote | STR_FUNC_QWORDS, begin, end);
            do {c = nextc();} while (Character.isWhitespace(c));
            pushback(c);
            yaccValue = "%"+c+begin;
            return Tokens.tWORDS_BEG;

        case 'w':
            lex_strterm = new StringTerm(/* str_squote | */ STR_FUNC_QWORDS, begin, end);
            do {c = nextc();} while (Character.isWhitespace(c));
            pushback(c);
            yaccValue = "%"+c+begin;
            return Tokens.tQWORDS_BEG;

        case 'x':
            lex_strterm = new StringTerm(str_xquote, begin, end);
            yaccValue = "%"+c+begin;
            return Tokens.tXSTRING_BEG;

        case 'r':
            lex_strterm = new StringTerm(str_regexp, begin, end);
            yaccValue = "%"+c+begin;
            return Tokens.tREGEXP_BEG;

        case 's':
            lex_strterm = new StringTerm(str_ssym, begin, end);
            setState(LexState.EXPR_FNAME);
            yaccValue = "%"+c+begin;
            return Tokens.tSYMBEG;
        
        case 'I':
            lex_strterm = new StringTerm(str_dquote | STR_FUNC_QWORDS, begin, end);
            do {c = nextc();} while (Character.isWhitespace(c));
            pushback(c);
            yaccValue = "%" + c + begin;
            return Tokens.tSYMBOLS_BEG;
        case 'i':
            lex_strterm = new StringTerm(/* str_squote | */STR_FUNC_QWORDS, begin, end);
            do {c = nextc();} while (Character.isWhitespace(c));
            pushback(c);
            yaccValue = "%" + c + begin;
            return Tokens.tQSYMBOLS_BEG;
        default:
            throw new SyntaxException(PID.STRING_UNKNOWN_TYPE, 
                        getPosition(), getCurrentLine(), "unknown type of %string");
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

            newtok(false); // skip past quote type

            term = c;
            while ((c = nextc()) != EOF && c != term) {
                if (!tokadd_mbchar(c)) return EOF;
            }

            if (c == EOF) compile_error("unterminated here document identifier");

            // c == term.  This differs from MRI in that we unwind term symbol so we can make
            // our marker with just tokp and lex_p info (e.g. we don't make second numberBuffer).
            pushback(term);
            markerValue = createTokenByteList();
            nextc();
        } else {
            if (!isIdentifierChar(c)) {
                pushback(c);
                if ((func & STR_FUNC_INDENT) != 0) {
                    pushback('-');
                }
                return 0;
            }
            newtok(true);
            term = '"';
            func |= str_dquote;
            do {
                if (!tokadd_mbchar(c)) return EOF;
            } while ((c = nextc()) != EOF && isIdentifierChar(c));
            pushback(c);
            markerValue = createTokenByteList();
        }

        int len = lex_p - lex_pbeg;
        lex_goto_eol();
        lex_strterm = new HeredocTerm(markerValue, func, len, ruby_sourceline, lex_lastline);

        if (term == '`') {
            yaccValue = "`";
            flush();
            return Tokens.tXSTRING_BEG;
        }
        
        yaccValue = "\"";
        flush();
        return Tokens.tSTRING_BEG;
    }
    
    private void arg_ambiguous() {
        if (warnings.isVerbose() && Options.PARSER_WARN_AMBIGUOUS_ARGUMENTS.load()) {
            warnings.warning(ID.AMBIGUOUS_ARGUMENT, getPosition(), "Ambiguous first argument; make sure.");
        }
    }

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


    // TODO: Make hand-rolled version of this
    private static final String encodingString = "[cC][oO][dD][iI][nN][gG]\\s*[=:]\\s*([a-zA-Z0-9\\-_]+)";
    private static final Regex encodingRegexp = new Regex(encodingString.getBytes(), 0,
            encodingString.length(), 0, Encoding.load("ASCII"));

    protected void handleFileEncodingComment(ByteList encodingLine) throws IOException {
        int realSize = encodingLine.getRealSize();
        int begin = encodingLine.getBegin();
        Matcher matcher = encodingRegexp.matcher(encodingLine.getUnsafeBytes(), begin, begin + realSize);
        int result = RubyRegexp.matcherSearch(parserSupport.getConfiguration().getRuntime(), matcher, begin, begin + realSize, Option.IGNORECASE);

        if (result < 0) return;

        int begs[] = matcher.getRegion().beg;
        int ends[] = matcher.getRegion().end;

        setEncoding(encodingLine.makeShared(begs[1], ends[1] - begs[1]));
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
        ByteList encodingName = lexb.makeShared(beg, str - beg);
        setEncoding(encodingName);
        src.setEncoding(getEncoding()); // Change source to know what bytelist encodings to send for next source lines
        lexb.setEncoding(getEncoding()); // Also retroactively change current line to new encoding
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
            case Tokens.tSTRING_CONTENT: System.err.print("tSTRING_CONTENT[" + ((StrNode) value()).getValue() + "],"); break;
            case Tokens.tSTRING_BEG: System.err.print("tSTRING_BEG,"); break;
            case Tokens.tSTRING_END: System.err.print("tSTRING_END,"); break;
            case Tokens.tSTRING_DBEG: System.err.print("tSTRING_DBEG,"); break;
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
            case Tokens.tLABEL: System.err.print("tLABEL("+ value() +":),"); break;
            case '\n': System.err.println("NL"); break;
            case EOF: System.out.println("EOF"); break;
            case Tokens.tDSTAR: System.err.print("tDSTAR"); break;
            default: System.err.print("'" + (char)token + "',"); break;
        }
    }

    // DEBUGGING HELP 
    private int yylex2() throws IOException {
        int currentToken = yylex2();
        
        printToken(currentToken);
        
        return currentToken;
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
            int tok = lex_strterm.parseString(this);

            if (tok == Tokens.tSTRING_END && (yaccValue.equals("\"") || yaccValue.equals("'"))) {
                if (((lex_state == LexState.EXPR_BEG || lex_state == LexState.EXPR_ENDFN) && !conditionState.isInState() ||
                        isARG()) && peek(':')) {
                    int c1 = nextc();
                    if (peek(':')) { // "mod"::SOMETHING (hack MRI does not do this)
                        pushback(c1);
                    } else {
                        nextc();
                        tok = Tokens.tLABEL_END;
                    }
                }
            }

            if (tok == Tokens.tSTRING_END || tok == Tokens.tREGEXP_END || tok == Tokens.tLABEL_END) {
                lex_strterm = null;
                setState(LexState.EXPR_END);
            }

            return tok;
        }

        commandState = commandStart;
        commandStart = false;

        loop: for(;;) {
            last_state = lex_state;
            c = nextc();
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
            case '#': {	/* it's a comment */
                ByteList encodingName = parseMagicComment(parserSupport.getConfiguration().getRuntime(), lexb.makeShared(lex_p, lex_pend - lex_p));
                // FIXME: boolean to mark we already found a magic comment to stop searching.  When found or we went too far
                if (comment_at_top()) {
                    if (encodingName != null) {
                        setEncoding(encodingName);
                    } else {
                        set_file_encoding(lex_p, lex_pend);
                    }
                }
                lex_p = lex_pend;
            }
            /* fall through */
            case '\n':
                switch (lex_state) {
                case EXPR_BEG: case EXPR_FNAME: case EXPR_DOT:
                case EXPR_CLASS: case EXPR_VALUE:
                    continue loop;
                case EXPR_LABELARG:
                    if (inKwarg) {
                        commandStart = true;
                        setState(LexState.EXPR_BEG);
                        return '\n';
                    }
                    continue loop;
                }

                boolean done = false;
                while(!done) {
                    c = nextc();

                    switch (c) {
                    case ' ': case '\t': case '\f': case '\r': case '\13': /* '\v' */
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
                    case -1:		// EOF (ENEBO: After default?
                        done = true;
                    }
                }

                if (c == -1) return EOF;

                pushback(c);
                getPosition();

                switch (lex_state) {
                case EXPR_BEG: case EXPR_FNAME: case EXPR_DOT: case EXPR_CLASS:
                    continue loop;
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
                        for (;;) {
                            lex_goto_eol();

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

                        continue loop;
                    }
                }

                determineExpressionState();

                c = nextc();
                if (c == '=') {
                    c = nextc();
                    if (c == '=') {
                        yaccValue = "===";
                        return Tokens.tEQQ;
                    }
                    pushback(c);
                    yaccValue = "==";
                    return Tokens.tEQ;
                }
                if (c == '~') {
                    yaccValue = "=~";
                    return Tokens.tMATCH;
                } else if (c == '>') {
                    yaccValue = "=>";
                    return Tokens.tASSOC;
                }
                pushback(c);
                yaccValue = "=";
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
                yaccValue = ";";
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
                yaccValue = "\\";
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
                    return EOF;
                }
                return identifier(c, commandState);
            default:
                return identifier(c, commandState);
            }
        }
    }

    private int identifierToken(int result, String value) {

        if (result == Tokens.tIDENTIFIER && last_state != LexState.EXPR_DOT &&
                parserSupport.getCurrentScope().isDefined(value) >= 0) {
            setState(LexState.EXPR_END);
        }

        yaccValue = value;
        return result;
    }
    
    private int ampersand(boolean spaceSeen) throws IOException {
        int c = nextc();
        
        switch (c) {
        case '&':
            setState(LexState.EXPR_BEG);
            if ((c = nextc()) == '=') {
                yaccValue = "&&";
                setState(LexState.EXPR_BEG);
                return Tokens.tOP_ASGN;
            }
            pushback(c);
            yaccValue = "&&";
            return Tokens.tANDOP;
        case '=':
            yaccValue = "&";
            setState(LexState.EXPR_BEG);
            return Tokens.tOP_ASGN;
        }
        pushback(c);
        
        //tmpPosition is required because getPosition()'s side effects.
        //if the warning is generated, the getPosition() on line 954 (this line + 18) will create
        //a wrong position if the "inclusive" flag is not set.
        ISourcePosition tmpPosition = getPosition();
        if (isSpaceArg(c, spaceSeen)) {
            if (warnings.isVerbose() && Options.PARSER_WARN_ARGUMENT_PREFIX.load())
                warnings.warning(ID.ARGUMENT_AS_PREFIX, tmpPosition, "`&' interpreted as argument prefix");
            c = Tokens.tAMPER;
        } else if (isBEG()) {
            c = Tokens.tAMPER;
        } else {
            warn_balanced(c, spaceSeen, "&", "argument prefix");
            c = Tokens.tAMPER2;
        }
        
        determineExpressionState();
        
        yaccValue = "&";
        return c;
    }
    
    private int at() throws IOException {
        newtok(true);
        int c = nextc();
        int result;
        if (c == '@') {
            c = nextc();
            result = Tokens.tCVAR;
        } else {
            result = Tokens.tIVAR;                    
        }
        
        if (c != EOF && (Character.isDigit(c) || !isIdentifierChar(c))) {
            pushback(c);
            if ((lex_p - tokp) == 1) {
                throw new SyntaxException(PID.IVAR_BAD_NAME, getPosition(), getCurrentLine(),
                        "`@" + ((char) c) + "' is not allowed as an instance variable name");
            }
            throw new SyntaxException(PID.CVAR_BAD_NAME, getPosition(), getCurrentLine(),
                    "`@@" + ((char) c) + "' is not allowed as a class variable name");
        }

        if (!tokadd_ident(c)) return EOF;

        last_state = lex_state;
        setState(LexState.EXPR_END);

        return tokenize_ident(result);
    }
    
    private int backtick(boolean commandState) throws IOException {
        yaccValue = "`";

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
            if (c == '@') {
                yaccValue = "!";
                return Tokens.tBANG;
            }
        } else {
            setState(LexState.EXPR_BEG);
        }
        
        switch (c) {
        case '=':
            yaccValue = "!=";
            
            return Tokens.tNEQ;
        case '~':
            yaccValue = "!~";
            
            return Tokens.tNMATCH;
        default: // Just a plain bang
            pushback(c);
            yaccValue = "!";
            
            return Tokens.tBANG;
        }
    }
    
    private int caret() throws IOException {
        int c = nextc();
        if (c == '=') {
            setState(LexState.EXPR_BEG);
            yaccValue = "^";
            return Tokens.tOP_ASGN;
        }
        
        determineExpressionState();
        
        pushback(c);
        yaccValue = "^";
        return Tokens.tCARET;
    }

    private int colon(boolean spaceSeen) throws IOException {
        int c = nextc();
        
        if (c == ':') {
            if (isBEG() || lex_state == LexState.EXPR_CLASS || (isARG() && spaceSeen)) {
                setState(LexState.EXPR_BEG);
                yaccValue = "::";
                return Tokens.tCOLON3;
            }
            setState(LexState.EXPR_DOT);
            yaccValue = ":";
            return Tokens.tCOLON2;
        }

        if (isEND() || Character.isWhitespace(c)) {
            pushback(c);
            setState(LexState.EXPR_BEG);
            yaccValue = ":";
            warn_balanced(c, spaceSeen, ":", "symbol literal");
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
        yaccValue = ":";
        return Tokens.tSYMBEG;
    }

    private int comma(int c) throws IOException {
        setState(LexState.EXPR_BEG);
        yaccValue = ",";
        
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
        setState(LexState.EXPR_END);
        newtok(true);
        int c = nextc();
        
        switch (c) {
        case '_':       /* $_: last read line string */
            c = nextc();
            if (isIdentifierChar(c)) {
                if (!tokadd_ident(c)) return EOF;

                last_state = lex_state;
                setState(LexState.EXPR_END);
                yaccValue = createTokenString().intern();
                return Tokens.tGVAR;
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
            yaccValue = "$" + (char) c;
            return Tokens.tGVAR;

        case '-':
            c = nextc();
            if (isIdentifierChar(c)) {
                if (!tokadd_mbchar(c)) return EOF;
            } else {
                pushback(c);
                pushback('-');
                return '$';
            }
            yaccValue = createTokenString().intern();
            /* xxx shouldn't check if valid option variable */
            return Tokens.tGVAR;

        case '&':       /* $&: last match */
        case '`':       /* $`: string before last match */
        case '\'':      /* $': string after last match */
        case '+':       /* $+: string matches last paren. */
            // Explicit reference to these vars as symbols...
            if (last_state == LexState.EXPR_FNAME) {
                yaccValue = "$" + (char) c;
                return Tokens.tGVAR;
            }
            
            yaccValue = new BackRefNode(getPosition(), c);
            return Tokens.tBACK_REF;

        case '1': case '2': case '3': case '4': case '5': case '6':
        case '7': case '8': case '9':
            do {
                c = nextc();
            } while (Character.isDigit(c));
            pushback(c);
            if (last_state == LexState.EXPR_FNAME) {
                yaccValue = createTokenString().intern();
                return Tokens.tGVAR;
            }

            yaccValue = new NthRefNode(getPosition(), Integer.parseInt(createTokenString().substring(1).intern()));
            return Tokens.tNTH_REF;
        case '0':
            setState(LexState.EXPR_END);

            return identifierToken(Tokens.tGVAR, ("$" + (char) c).intern());
        default:
            if (!isIdentifierChar(c)) {
                pushback(c);
                throw new SyntaxException(PID.CVAR_BAD_NAME, getPosition(), lex_lastline.toString(), "`$" + ((char) c) + "' is not allowed as a global variable name");
            }
        
            last_state = lex_state;
            setState(LexState.EXPR_END);

            tokadd_ident(c);

            return identifierToken(Tokens.tGVAR, createTokenString().intern());  // $blah
        }
    }

    // FIXME: I added number gvars here and they did not.
    public boolean isGlobalCharPunct(int c) {
        switch (c) {
            case '_': case '~': case '*': case '$': case '?': case '!': case '@':
            case '/': case '\\': case ';': case ',': case '.': case '=': case ':':
            case '<': case '>': case '\"': case '-': case '&': case '`': case '\'':
            case '+': case '1': case '2': case '3': case '4': case '5': case '6':
            case '7': case '8': case '9': case '0':
                return true;
        }
        return isIdentifierChar(c);
    }

    private int dot() throws IOException {
        int c;
        
        setState(LexState.EXPR_BEG);
        if ((c = nextc()) == '.') {
            if ((c = nextc()) == '.') {
                yaccValue = "...";
                return Tokens.tDOT3;
            }
            pushback(c);
            yaccValue = "..";
            return Tokens.tDOT2;
        }
        
        pushback(c);
        if (Character.isDigit(c)) {
            throw new SyntaxException(PID.FLOAT_MISSING_ZERO, getPosition(), getCurrentLine(),
                    "no .<digit> floating literal anymore; put 0 before dot");
        }
        
        setState(LexState.EXPR_DOT);
        yaccValue = ".";
        return Tokens.tDOT;
    }
    
    private int doubleQuote() throws IOException {
        lex_strterm = new StringTerm(str_dquote, '\0', '"');
        yaccValue = "\"";

        return Tokens.tSTRING_BEG;
    }
    
    private int greaterThan() throws IOException {
        determineExpressionState();

        int c = nextc();

        switch (c) {
        case '=':
            yaccValue = ">=";
            
            return Tokens.tGEQ;
        case '>':
            if ((c = nextc()) == '=') {
                setState(LexState.EXPR_BEG);
                yaccValue = ">>";
                return Tokens.tOP_ASGN;
            }
            pushback(c);
            
            yaccValue = ">>";
            return Tokens.tRSHFT;
        default:
            pushback(c);
            yaccValue = ">";
            return Tokens.tGT;
        }
    }
    
    private int identifier(int c, boolean commandState) throws IOException {
        if (!isIdentifierChar(c)) {
            String badChar = "\\" + Integer.toOctalString(c & 0xff);
            throw new SyntaxException(PID.CHARACTER_BAD, getPosition(), getCurrentLine(),
                    "Invalid char `" + badChar + "' ('" + (char) c + "') in expression", badChar);
        }

        newtok(true);
        int first = c;
        do {
            if (!tokadd_mbchar(c)) return EOF;
            c = nextc();
        } while (isIdentifierChar(c));

        boolean lastBangOrPredicate = false;

        // methods 'foo!' and 'foo?' are possible but if followed by '=' it is relop
        if (c == '!' || c == '?') {
            if (!peek('=')) {
                lastBangOrPredicate = true;
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
        String tempVal = createTokenString();
        
        if (isLabelPossible(commandState)) {
            int c2 = nextc();
            if (c2 == ':' && !peek(':')) {
                setState(LexState.EXPR_LABELARG);
                yaccValue = tempVal.intern();
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
                    yaccValue = keyword.name;
                } else {
                    yaccValue = getPosition();
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
        
        return identifierToken(result, tempVal.intern());
    }

    private int leftBracket(boolean spaceSeen) throws IOException {
        parenNest++;
        int c = '[';
        if (lex_state == LexState.EXPR_FNAME || lex_state == LexState.EXPR_DOT) {
            setState(LexState.EXPR_ARG);
            
            if ((c = nextc()) == ']') {
                if (peek('=')) {
                    nextc();
                    yaccValue = "[]=";
                    return Tokens.tASET;
                }
                yaccValue = "[]";
                return Tokens.tAREF;
            }
            pushback(c);
            yaccValue = "[";
            return '[';
        } else if (isBEG() || (isARG() && spaceSeen)) {
            c = Tokens.tLBRACK;
        }

        setState(LexState.EXPR_BEG);
        conditionState.stop();
        cmdArgumentState.stop();
        yaccValue = "[";
        return c;
    }
    
    private int leftCurly() {
        braceNest++;
        //System.out.println("lcurly: " + braceNest);
        if (leftParenBegin > 0 && leftParenBegin == parenNest) {
            setState(LexState.EXPR_BEG);
            leftParenBegin = 0;
            parenNest--;
            conditionState.stop();
            cmdArgumentState.stop();
            yaccValue = "{";
            return Tokens.tLAMBEG;
        }

        char c;
        if (isARG() || lex_state == LexState.EXPR_END || lex_state == LexState.EXPR_ENDFN) { // block (primary)
            c = Tokens.tLCURLY;
        } else if (lex_state == LexState.EXPR_ENDARG) { // block (expr)
            c = Tokens.tLBRACE_ARG;
        } else { // hash
            c = Tokens.tLBRACE;
        }

        conditionState.stop();
        cmdArgumentState.stop();
        setState(LexState.EXPR_BEG);
        yaccValue = getPosition();

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

            if (token == Tokens.tLAMBDA) {
                result = Tokens.tLPAREN2;
            }
        }

        parenNest++;
        conditionState.stop();
        cmdArgumentState.stop();
        setState(LexState.EXPR_BEG);
        
        yaccValue = getPosition();
        return result;
    }
    
    private int lessThan(boolean spaceSeen) throws IOException {
        last_state = lex_state;
        int c = nextc();
        if (c == '<' && lex_state != LexState.EXPR_DOT && lex_state != LexState.EXPR_CLASS &&
                !isEND() && (!isARG() || spaceSeen)) {
            int tok = hereDocumentIdentifier();
            
            if (tok != 0) return tok;
        }
        
        determineExpressionState();
        
        switch (c) {
        case '=':
            if ((c = nextc()) == '>') {
                yaccValue = "<=>";
                return Tokens.tCMP;
            }
            pushback(c);
            yaccValue = "<=";
            return Tokens.tLEQ;
        case '<':
            if ((c = nextc()) == '=') {
                setState(LexState.EXPR_BEG);
                yaccValue = "<<";
                return Tokens.tOP_ASGN;
            }
            pushback(c);
            yaccValue = "<<";
            warn_balanced(c, spaceSeen, "<<", "here document");
            return Tokens.tLSHFT;
        default:
            yaccValue = "<";
            pushback(c);
            return Tokens.tLT;
        }
    }
    
    private int minus(boolean spaceSeen) throws IOException {
        int c = nextc();
        
        if (lex_state == LexState.EXPR_FNAME || lex_state == LexState.EXPR_DOT) {
            setState(LexState.EXPR_ARG);
            if (c == '@') {
                yaccValue = "-@";
                return Tokens.tUMINUS;
            }
            pushback(c);
            yaccValue = "-";
            return Tokens.tMINUS;
        }
        if (c == '=') {
            setState(LexState.EXPR_BEG);
            yaccValue = "-";
            return Tokens.tOP_ASGN;
        }
        if (c == '>') {
            setState(LexState.EXPR_ARG);
            yaccValue = "->";
            return Tokens.tLAMBDA;
        }
        if (isBEG() || isSpaceArg(c, spaceSeen)) {
            if (isARG()) arg_ambiguous();
            setState(LexState.EXPR_BEG);
            pushback(c);
            yaccValue = "-";
            if (Character.isDigit(c)) {
                return Tokens.tUMINUS_NUM;
            }
            return Tokens.tUMINUS;
        }
        setState(LexState.EXPR_BEG);
        pushback(c);
        yaccValue = "-";
        warn_balanced(c, spaceSeen, "-", "unary operator");
        return Tokens.tMINUS;
    }

    private int percent(boolean spaceSeen) throws IOException {
        if (isBEG()) return parseQuote(nextc());

        int c = nextc();

        if (c == '=') {
            setState(LexState.EXPR_BEG);
            yaccValue = "%";
            return Tokens.tOP_ASGN;
        }

        if (isSpaceArg(c, spaceSeen)) return parseQuote(c);
        
        determineExpressionState();
        
        pushback(c);
        yaccValue = "%";
        warn_balanced(c, spaceSeen, "%%", "string literal");
        return Tokens.tPERCENT;
    }

    private int pipe() throws IOException {
        int c = nextc();
        
        switch (c) {
        case '|':
            setState(LexState.EXPR_BEG);
            if ((c = nextc()) == '=') {
                setState(LexState.EXPR_BEG);
                yaccValue = "||";
                return Tokens.tOP_ASGN;
            }
            pushback(c);
            yaccValue = "||";
            return Tokens.tOROP;
        case '=':
            setState(LexState.EXPR_BEG);
            yaccValue = "|";
            return Tokens.tOP_ASGN;
        default:
            determineExpressionState();
            
            pushback(c);
            yaccValue = "|";
            return Tokens.tPIPE;
        }
    }
    
    private int plus(boolean spaceSeen) throws IOException {
        int c = nextc();
        if (lex_state == LexState.EXPR_FNAME || lex_state == LexState.EXPR_DOT) {
            setState(LexState.EXPR_ARG);
            if (c == '@') {
                yaccValue = "+@";
                return Tokens.tUPLUS;
            }
            pushback(c);
            yaccValue = "+";
            return Tokens.tPLUS;
        }
        
        if (c == '=') {
            setState(LexState.EXPR_BEG);
            yaccValue = "+";
            return Tokens.tOP_ASGN;
        }
        
        if (isBEG() || isSpaceArg(c, spaceSeen)) { //FIXME: arg_ambiguous missing
            if (isARG()) arg_ambiguous();
            setState(LexState.EXPR_BEG);
            pushback(c);
            if (Character.isDigit(c)) {
                c = '+';
                return parseNumber(c);
            }
            yaccValue = "+";
            return Tokens.tUPLUS;
        }
        
        setState(LexState.EXPR_BEG);
        pushback(c);
        yaccValue = "+";
        warn_balanced(c, spaceSeen, "+", "unary operator");
        return Tokens.tPLUS;
    }
    
    private int questionMark() throws IOException {
        int c;
        
        if (isEND()) {
            setState(LexState.EXPR_VALUE);
            yaccValue = "?";
            return '?';
        }
        
        c = nextc();
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
            pushback(c);
            setState(LexState.EXPR_VALUE);
            yaccValue = "?";
            return '?';
        }

        if (!isASCII(c)) {
            if (!tokadd_mbchar(c)) return EOF;
        } else if (isIdentifierChar(c) && !peek('\n') && isNext_identchar()) {
            newtok(true);
            pushback(c);
            setState(LexState.EXPR_VALUE);
            yaccValue = "?";
            return '?';
        } else if (c == '\\') {
            if (peek('u')) {
                nextc(); // Eat 'u'
                ByteList oneCharBL = new ByteList(2);
                oneCharBL.setEncoding(current_enc);

                c = readUTFEscape(oneCharBL, false, false);
                
                if (c >= 0x80) {
                    tokaddmbc(c, oneCharBL);
                } else {
                    oneCharBL.append(c);
                }
                
                setState(LexState.EXPR_END);
                yaccValue = new StrNode(getPosition(), oneCharBL);
                
                return Tokens.tCHAR;
            } else {
                c = readEscape();
            }
        } else {
            newtok(true);
        }

        ByteList oneCharBL = new ByteList(1);
        oneCharBL.append(c);
        yaccValue = new StrNode(getPosition(), oneCharBL);
        setState(LexState.EXPR_END);
        return Tokens.tCHAR;
    }
    
    private int rightBracket() {
        parenNest--;
        conditionState.restart();
        cmdArgumentState.restart();
        setState(LexState.EXPR_ENDARG);
        yaccValue = "]";
        return Tokens.tRBRACK;
    }

    private int rightCurly() {
        conditionState.restart();
        cmdArgumentState.restart();
        setState(LexState.EXPR_ENDARG);
        yaccValue = "}";
        //System.out.println("braceNest: " + braceNest);
        int tok = /*braceNest != 0 ? Tokens.tSTRING_DEND : */ Tokens.tRCURLY;
        braceNest--;
        return tok;
    }

    private int rightParen() {
        parenNest--;
        conditionState.restart();
        cmdArgumentState.restart();
        setState(LexState.EXPR_ENDFN);
        yaccValue = ")";
        return Tokens.tRPAREN;
    }
    
    private int singleQuote() throws IOException {
        lex_strterm = new StringTerm(str_squote, '\0', '\'');
        yaccValue = "'";

        return Tokens.tSTRING_BEG;
    }
    
    private int slash(boolean spaceSeen) throws IOException {
        if (isBEG()) {
            lex_strterm = new StringTerm(str_regexp, '\0', '/');
            yaccValue = "/";
            return Tokens.tREGEXP_BEG;
        }
        
        int c = nextc();
        
        if (c == '=') {
            yaccValue = "/";
            setState(LexState.EXPR_BEG);
            return Tokens.tOP_ASGN;
        }
        pushback(c);
        if (isSpaceArg(c, spaceSeen)) {
            arg_ambiguous();
            lex_strterm = new StringTerm(str_regexp, '\0', '/');
            yaccValue = "/";
            return Tokens.tREGEXP_BEG;
        }
        
        determineExpressionState();
        
        yaccValue = "/";
        warn_balanced(c, spaceSeen, "/", "regexp literal");
        return Tokens.tDIVIDE;
    }

    private int star(boolean spaceSeen) throws IOException {
        int c = nextc();
        
        switch (c) {
        case '*':
            if ((c = nextc()) == '=') {
                setState(LexState.EXPR_BEG);
                yaccValue = "**";
                return Tokens.tOP_ASGN;
            }

            pushback(c); // not a '=' put it back
            yaccValue = "**";

            if (isSpaceArg(c, spaceSeen)) {
                if (warnings.isVerbose() && Options.PARSER_WARN_ARGUMENT_PREFIX.load())
                    warnings.warning(ID.ARGUMENT_AS_PREFIX, getPosition(), "`**' interpreted as argument prefix");
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
            yaccValue = "*";
            return Tokens.tOP_ASGN;
        default:
            pushback(c);
            if (isSpaceArg(c, spaceSeen)) {
                if (warnings.isVerbose() && Options.PARSER_WARN_ARGUMENT_PREFIX.load())
                    warnings.warning(ID.ARGUMENT_AS_PREFIX, getPosition(), "`*' interpreted as argument prefix");
                c = Tokens.tSTAR;
            } else if (isBEG()) {
                c = Tokens.tSTAR;
            } else {
                warn_balanced(c, spaceSeen, "*", "argument prefix");
                c = Tokens.tSTAR2;
            }
            yaccValue = "*";
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
        
        yaccValue = "~";
        return Tokens.tTILDE;
    }

    private ByteList numberBuffer = new ByteList(10); // ascii is good enough.
    /**
     *  Parse a number from the input stream.
     *
     *@param c The first character of the number.
     *@return A int constant wich represents a token.
     */
    private int parseNumber(int c) throws IOException {
        setState(LexState.EXPR_END);
        newtok(true);

        numberBuffer.setRealSize(0);

        if (c == '-') {
        	numberBuffer.append((char) c);
            c = nextc();
        } else if (c == '+') {
        	// We don't append '+' since Java number parser gets confused
            c = nextc();
        }
        
        int nondigit = 0;

        if (c == '0') {
            int startLen = numberBuffer.length();

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
                                numberBuffer.append((char) c);
                            } else {
                                break;
                            }
                        }
                    }
                    pushback(c);

                    if (numberBuffer.length() == startLen) {
                        throw new SyntaxException(PID.BAD_HEX_NUMBER, getPosition(), 
                                getCurrentLine(), "Hexadecimal number without hex-digits.");
                    } else if (nondigit != '\0') {
                        throw new SyntaxException(PID.TRAILING_UNDERSCORE_IN_NUMBER,
                                getPosition(), getCurrentLine(), "Trailing '_' in number.");
                    }
                    return getIntegerToken(numberBuffer.toString(), 16, numberLiteralSuffix(SUFFIX_ALL));
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
                                numberBuffer.append((char) c);
                            } else {
                                break;
                            }
                        }
                    }
                    pushback(c);

                    if (numberBuffer.length() == startLen) {
                        throw new SyntaxException(PID.EMPTY_BINARY_NUMBER, getPosition(),
                                getCurrentLine(), "Binary number without digits.");
                    } else if (nondigit != '\0') {
                        throw new SyntaxException(PID.TRAILING_UNDERSCORE_IN_NUMBER,
                                getPosition(), getCurrentLine(), "Trailing '_' in number.");
                    }
                    return getIntegerToken(numberBuffer.toString(), 2, numberLiteralSuffix(SUFFIX_ALL));
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
                                numberBuffer.append((char) c);
                            } else {
                                break;
                            }
                        }
                    }
                    pushback(c);

                    if (numberBuffer.length() == startLen) {
                        throw new SyntaxException(PID.EMPTY_BINARY_NUMBER, getPosition(), 
                                getCurrentLine(), "Binary number without digits.");
                    } else if (nondigit != '\0') {
                        throw new SyntaxException(PID.TRAILING_UNDERSCORE_IN_NUMBER, getPosition(),
                                getCurrentLine(), "Trailing '_' in number.");
                    }
                    return getIntegerToken(numberBuffer.toString(), 10, numberLiteralSuffix(SUFFIX_ALL));
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
                            numberBuffer.append((char) c);
                        } else {
                            break;
                        }
                    }
                    if (numberBuffer.length() > startLen) {
                        pushback(c);

                        if (nondigit != '\0') {
                            throw new SyntaxException(PID.TRAILING_UNDERSCORE_IN_NUMBER, 
                                    getPosition(), getCurrentLine(), "Trailing '_' in number.");
                        }

                        return getIntegerToken(numberBuffer.toString(), 8, numberLiteralSuffix(SUFFIX_ALL));
                    }
                case '8' :
                case '9' :
                    throw new SyntaxException(PID.BAD_OCTAL_DIGIT, getPosition(),
                            getCurrentLine(), "Illegal octal digit.");
                case '.' :
                case 'e' :
                case 'E' :
                	numberBuffer.append('0');
                    break;
                default :
                    pushback(c);
                    yaccValue = new FixnumNode(getPosition(), 0);
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
                    numberBuffer.append((char) c);
                    break;
                case '.' :
                    if (nondigit != '\0') {
                        pushback(c);
                        throw new SyntaxException(PID.TRAILING_UNDERSCORE_IN_NUMBER, getPosition(),
                                getCurrentLine(), "Trailing '_' in number.");
                    } else if (seen_point || seen_e) {
                        pushback(c);
                        return getNumberToken(numberBuffer.toString(), seen_e, seen_point, nondigit);
                    } else {
                    	int c2;
                        if (!Character.isDigit(c2 = nextc())) {
                            pushback(c2);
                        	pushback('.');
                            if (c == '_') { 
                            		// Enebo:  c can never be antrhign but '.'
                            		// Why did I put this here?
                            } else {
                                return getIntegerToken(numberBuffer.toString(), 10, numberLiteralSuffix(SUFFIX_ALL));
                            }
                        } else {
                            numberBuffer.append('.');
                            numberBuffer.append((char) c2);
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
                        pushback(c);
                        return getNumberToken(numberBuffer.toString(), seen_e, seen_point, nondigit);
                    } else {
                        numberBuffer.append((char) c);
                        seen_e = true;
                        nondigit = c;
                        c = nextc();
                        if (c == '-' || c == '+') {
                            numberBuffer.append((char) c);
                            nondigit = c;
                        } else {
                            pushback(c);
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
                    pushback(c);
                return getNumberToken(numberBuffer.toString(), seen_e, seen_point, nondigit);
            }
        }
    }

    private int getNumberToken(String number, boolean seen_e, boolean seen_point, int nondigit) throws IOException {
        boolean isFloat = seen_e || seen_point;
        if (nondigit != '\0') {
            throw new SyntaxException(PID.TRAILING_UNDERSCORE_IN_NUMBER, getPosition(),
                    getCurrentLine(), "Trailing '_' in number.");
        } else if (isFloat) {
            int suffix = numberLiteralSuffix(seen_e ? SUFFIX_I : SUFFIX_ALL);
            return getFloatToken(number, suffix);
        }
        return getIntegerToken(number, 10, numberLiteralSuffix(SUFFIX_ALL));
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
                    throw new SyntaxException(PID.INVALID_ESCAPE_SYNTAX, getPosition(),
                            getCurrentLine(), "invalid Unicode codepoint (too large)");
                }
            } while (peek(' ') || peek('\t'));

            int c = nextc();
            if (c != '}') {
                throw new SyntaxException(PID.INVALID_ESCAPE_SYNTAX, getPosition(),
                        getCurrentLine(), "unterminated Unicode escape");
            }
            buffer.append((char) c);
        } else { // handle \\uxxxx
            scanHexLiteral(buffer, 4, true, "Invalid Unicode escape");
        }
    }

    // mri: parser_tokadd_mbchar
    /**
     * This differs from MRI in a few ways.  This version does not apply value to a separate token buffer.
     * It is for use when we know we will not be omitting or including ant non-syntactical characters.  Use
     * tokadd_mbchar(int, ByteList) if the string differs from actual source.  Secondly, this returns a boolean
     * instead of the first byte passed.  MRI only used the return value as a success/failure code to return
     * EOF.
     *
     * Because this version does not use a separate token buffer we only just increment lex_p.  When we reach
     * end of the token it will just get the bytes directly from source directly.
     */
    public boolean tokadd_mbchar(int first_byte) {
        int length = precise_mbclen();

        if (length <= 0) {
            compile_error("invalid multibyte char (" + current_enc + ")");
            return false;
        }

        lex_p += length - 1;  // we already read first byte so advance pointer for remainder

        return true;
    }

    // mri: parser_tokadd_mbchar
    /**
     * @see RubyLexer::tokadd_mbchar(int)
     */
    public boolean tokadd_mbchar(int first_byte, ByteList buffer) {
        int length = precise_mbclen();

        if (length <= 0) {
            compile_error("invalid multibyte char (" + current_enc + ")");
            return false;
        }

        tokAdd(first_byte, buffer);                  // add first byte since we have it.
        lex_p += length - 1;                         // we already read first byte so advance pointer for remainder
        if (length > 1) tokCopy(length - 1, buffer); // copy next n bytes over.

        return true;
    }

    /**
     *  This looks deceptively like tokadd_mbchar(int, ByteList) but it differs in that it uses
     *  the bytelists encoding and the first parameter is a full codepoint and not the first byte
     *  of a mbc sequence.
     */
    public void tokaddmbc(int codepoint, ByteList buffer) {
        Encoding encoding = buffer.getEncoding();
        int length = encoding.codeToMbcLength(codepoint);
        buffer.ensure(buffer.getRealSize() + length);
        encoding.codeToMbc(codepoint, buffer.getUnsafeBytes(), buffer.begin() + buffer.getRealSize());
        buffer.setRealSize(buffer.getRealSize() + length);
    }

    public void tokAdd(int first_byte, ByteList buffer) {
        buffer.append((byte) first_byte);
    }

    public void tokCopy(int length, ByteList buffer) {
        buffer.append(lexb, lex_p - length, length);
    }

    public int precise_mbclen() {
        byte[] data = lexb.getUnsafeBytes();
        int begin = lexb.begin();

        // we subtract one since we have read past first byte by time we are calling this.
        return current_enc.length(data, begin+lex_p-1, begin+lex_pend);
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
                    throw new SyntaxException(PID.INVALID_ESCAPE_SYNTAX, getPosition(),
                            getCurrentLine(), "invalid Unicode codepoint (too large)");
                }
                if (buffer != null) readUTF8EscapeIntoBuffer(codepoint, buffer, stringLiteral);
            } while (peek(' ') || peek('\t'));

            c = nextc();
            if (c != '}') {
                throw new SyntaxException(PID.INVALID_ESCAPE_SYNTAX, getPosition(),
                        getCurrentLine(), "unterminated Unicode escape");
            }
        } else { // handle \\uxxxx
            codepoint = scanHex(4, true, "Invalid Unicode escape");
            if (buffer != null) readUTF8EscapeIntoBuffer(codepoint, buffer, stringLiteral);
        }

        return codepoint;
    }
    
    private void readUTF8EscapeIntoBuffer(int codepoint, ByteList buffer, boolean stringLiteral) throws IOException {
        if (codepoint >= 0x80) {
            buffer.setEncoding(UTF8_ENCODING);
            if (stringLiteral) tokaddmbc(codepoint, buffer);
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
                    throw new SyntaxException(PID.INVALID_ESCAPE_SYNTAX, getPosition(),
                            getCurrentLine(), "Invalid escape character syntax");
                } else if ((c = nextc()) == '\\') {
                    return (char) (readEscape() | 0x80);
                } else if (c == EOF) {
                    throw new SyntaxException(PID.INVALID_ESCAPE_SYNTAX, getPosition(),
                            getCurrentLine(), "Invalid escape character syntax");
                } 
                return (char) ((c & 0xff) | 0x80);
            case 'C' :
                if (nextc() != '-') {
                    throw new SyntaxException(PID.INVALID_ESCAPE_SYNTAX, getPosition(),
                            getCurrentLine(), "Invalid escape character syntax");
                }
            case 'c' :
                if ((c = nextc()) == '\\') {
                    c = readEscape();
                } else if (c == '?') {
                    return '\177';
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

    /**
     * Read up to count hexadecimal digits and store those digits in a token numberBuffer.  If strict is
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
        if (i == 0 || strict && count != i) {
            throw new SyntaxException(PID.INVALID_ESCAPE_SYNTAX, getPosition(),
                    getCurrentLine(), errorMessage);
        }

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
        if (i == 0 || (strict && count != i)) {
            throw new SyntaxException(PID.INVALID_ESCAPE_SYNTAX, getPosition(),
                    getCurrentLine(), errorMessage);
        }

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
