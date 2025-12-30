/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2013-2017 The JRuby Team (jruby@jruby.org)
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

package org.jruby.ext.ripper;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.common.IRubyWarnings;
import org.jruby.lexer.LexerSource;
import org.jruby.lexer.LexingCommon;
import org.jruby.parser.RubyParserBase;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;
import org.jruby.util.StringSupport;
import org.jruby.util.cli.Options;

import static org.jruby.ext.ripper.RipperParser.*;
import static org.jruby.parser.RubyParser.tGVAR;
import static org.jruby.util.StringSupport.CR_7BIT;

public class RubyLexer extends LexingCommon {
    private static final HashMap<String, Keyword> map;

    static {
        map = new HashMap<>();
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

    protected void ambiguousOperator(String op, String syn) {
        parser.dispatch("on_operator_ambiguous", getRuntime().newSymbol(op), getRuntime().newString(syn));
    }

    protected boolean onMagicComment(String name, ByteList value) {
        boolean found = super.onMagicComment(name, value);

        parser.dispatch("on_magic_comment", getRuntime().newString(name), getRuntime().newString(value));

        return found;
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
            return considerComplex(tRATIONAL, suffix);
        }

        try {
            Double.valueOf(number);
        } catch (NumberFormatException e) {
            warn("Float " + number + " out of range.");
        }

        return considerComplex(tFLOAT, suffix);
    }

    private int considerComplex(int token, int suffix) {
        if ((suffix & SUFFIX_I) == 0) {
            return token;
        } else {
            return tIMAGINARY;
        }
    }

    protected int src_line() {
        return getRubySourceline();
    }

    public boolean isVerbose() {
        return parser.getRuntime().isVerbose();
    }

    public void warn(String message) {
        parser.dispatch("warn", getRuntime().newString(message));
    }

    public void warning(String fmt) {
        parser.dispatch("warning", getRuntime().newString(fmt));
    }
    public void warning(String fmt, String arg) {
        parser.dispatch("warning", getRuntime().newString(fmt), getRuntime().newString(arg));
    }
    
    public enum Keyword {
        END ("end", keyword_end, keyword_end, EXPR_END),
        ELSE ("else", keyword_else, keyword_else, EXPR_BEG),
        CASE ("case", keyword_case, keyword_case, EXPR_BEG),
        ENSURE ("ensure", keyword_ensure, keyword_ensure, EXPR_BEG),
        MODULE ("module", keyword_module, keyword_module, EXPR_BEG),
        ELSIF ("elsif", keyword_elsif, keyword_elsif, EXPR_BEG),
        DEF ("def", keyword_def, keyword_def, EXPR_FNAME),
        RESCUE ("rescue", keyword_rescue, modifier_rescue, EXPR_MID),
        NOT ("not", keyword_not, keyword_not, EXPR_ARG),
        THEN ("then", keyword_then, keyword_then, EXPR_BEG),
        YIELD ("yield", keyword_yield, keyword_yield, EXPR_ARG),
        FOR ("for", keyword_for, keyword_for, EXPR_BEG),
        SELF ("self", keyword_self, keyword_self, EXPR_END),
        FALSE ("false", keyword_false, keyword_false, EXPR_END),
        RETRY ("retry", keyword_retry, keyword_retry, EXPR_END),
        RETURN ("return", keyword_return, keyword_return, EXPR_MID),
        TRUE ("true", keyword_true, keyword_true, EXPR_END),
        IF ("if", keyword_if, modifier_if, EXPR_BEG),
        DEFINED_P ("defined?", keyword_defined, keyword_defined, EXPR_ARG),
        SUPER ("super", keyword_super, keyword_super, EXPR_ARG),
        UNDEF ("undef", keyword_undef, keyword_undef, EXPR_FNAME|EXPR_FITEM),
        BREAK ("break", keyword_break, keyword_break, EXPR_MID),
        IN ("in", keyword_in, keyword_in, EXPR_BEG),
        DO ("do", keyword_do, keyword_do, EXPR_BEG),
        NIL ("nil", keyword_nil, keyword_nil, EXPR_END),
        UNTIL ("until", keyword_until, modifier_until, EXPR_BEG),
        UNLESS ("unless", keyword_unless, modifier_unless, EXPR_BEG),
        OR ("or", keyword_or, keyword_or, EXPR_BEG),
        NEXT ("next", keyword_next, keyword_next, EXPR_MID),
        WHEN ("when", keyword_when, keyword_when, EXPR_BEG),
        REDO ("redo", keyword_redo, keyword_redo, EXPR_END),
        AND ("and", keyword_and, keyword_and, EXPR_BEG),
        BEGIN ("begin", keyword_begin, keyword_begin, EXPR_BEG),
        __LINE__ ("__LINE__", keyword__LINE__, keyword__LINE__, EXPR_END),
        CLASS ("class", keyword_class, keyword_class, EXPR_CLASS),
        __FILE__("__FILE__", keyword__FILE__, keyword__FILE__, EXPR_END),
        LEND ("END", keyword_END, keyword_END, EXPR_END),
        LBEGIN ("BEGIN", keyword_BEGIN, keyword_BEGIN, EXPR_END),
        WHILE ("while", keyword_while, modifier_while, EXPR_BEG),
        ALIAS ("alias", keyword_alias, keyword_alias, EXPR_FNAME|EXPR_FITEM),
        __ENCODING__("__ENCODING__", keyword__ENCODING__, keyword__ENCODING__, EXPR_END);

        public final String name;
        public final int id0;
        public final int id1;
        public final int state;

        Keyword(String name, int id0, int id1, int state) {
            this.name = name;
            this.id0 = id0;
            this.id1 = id1;
            this.state = state;
        }
    }
    
    public static Keyword getKeyword(String str) {
        return (Keyword) map.get(str);
    }
    
    // MRI can directly seek source but we do not so we store all idents
    // here so the parser can then look at it on-demand to check things like
    // whether it is a valid identifier.  This should be safe to be a single
    // field since all ident logic should hit sequentially.
    String identValue;

    // Used for tiny smidgen of grammar in lexer (see setParserSupport())
    private RipperParserBase parser = null;

    private StrTerm lex_strterm;

    // When the heredoc identifier specifies <<-EOF that indents before ident. are ok (the '-').
    static final int STR_FUNC_INDENT=0x20;

    public RubyLexer(RipperParserBase parser, LexerSource src) {
        this(parser, src, null);
    }

    public RubyLexer(RipperParserBase parser, LexerSource src, IRubyWarnings _warnings) {
        super(src);
        this.parser = parser;
        setState(0);
        lex_strterm = null;
        // FIXME: Do we need to parser_prepare like normal lexer?
        setCurrentEncoding(src.getEncoding());
        reset();
    }

    protected ByteList delayed = null;
    private int delayed_line = 0;
    private int delayed_col = 0;
    private boolean cr_seen = false;

    /**
     * Has lexing started yet?
     */
    public boolean hasStarted() {
        return src != null; // if no current line then nextc has never been called.
    }
    
    protected void flush_string_content(Encoding encoding) {
        if (delayed != null) {
            int len = lex_p - tokp;
            if (len > 0) {
                delayed.setEncoding(encoding);
                delayed.append(lexb.makeShared(tokp, len));
            }
            dispatchDelayedToken(tSTRING_CONTENT);
            tokp = lex_p;
        }
    }

    public void addDelayedToken(int tok, int end) {
        // Left over stuffs...Add to delayed for later processing.
        if (tok < end) {
            if (delayed == null) {
                delayed = new ByteList();
                delayed.setEncoding(getEncoding());
                delayed_line = ruby_sourceline;
                delayed_col = tok - lex_pbeg;
            }
            delayed.append(lexb, tok, end - tok);
            tokp = end;
        }
    }

    private boolean nextLine() {
        line_offset += lex_pend;

        ByteList v = lex_nextline;
        lex_nextline = null;

        if (v == null) {
            if (eofp) return true;

            if (src == null || (v = src.gets()) == null) {
                eofp = true;
                lex_goto_eol();
                return true;
            }
            cr_seen = false;
        }

        addDelayedToken(tokp, lex_pend);

        if (heredoc_end > 0) {
            ruby_sourceline = heredoc_end;
            heredoc_end = 0;
        }
        ruby_sourceline++;
        line_count++;
        lex_pbeg = lex_p = 0;
        lex_pend = lex_p + v.length();
        lexb = v;
        flush();
        lex_lastline = v;

        return false;
    }

    private int cr(int c) {
        if (peek('\n')) {
            lex_p++;
            c = '\n';
        } else if (!cr_seen) {
            cr_seen = true;
            warn("encountered \\\\r in middle of line, treated as a mere space");
        }
        return c;
    }

    public int nextc() {
        if (lex_p == lex_pend || eofp || lex_nextline != null) {
            if (nextLine()) return EOF;
        }

        int c = p(lex_p);
        lex_p++;

        if (c == '\r') c = cr(c);

        return c;
    }
    
    public void dispatchHeredocEnd() {
        if (delayed != null) {
            dispatchDelayedToken(tSTRING_CONTENT);
        }
        lex_goto_eol();
        dispatchIgnoredScanEvent(tHEREDOC_END);
    }

    // yyerror1
    public void compile_error(String message) {
        parser.error();
        parser.dispatch("compile_error", getRuntime().newString(message));
    }

    // yyerror0
    public void parse_error(String message) {
        parser.error();
        parser.dispatch("on_parse_error", getRuntime().newString(message));
    }

    // This is noop in MRI but they make a tuple type for things like tIDENTIFIER and we
    // are storing this as a special value in each production
    protected void set_yylval_id(ByteList id) {
        this.id = id;
    }

    private ByteList id = null;

    public ByteList id() {
        return id;
    }

    protected void set_yylval_noname() {
        id = null;
    }

    protected void set_yylval_name(ByteList name) {
        id = name;
    }

    protected void set_yylval_val(ByteList name) {
        id = name;
    }

    public int tokenize_ident(int result) {
        String value = createTokenString();

        if (!IS_lex_state(last_state, EXPR_DOT|EXPR_FNAME) && parser.getCurrentScope().isDefined(value) >= 0) {
            setState(EXPR_END);
        }

        identValue = value.intern();
        set_yylval_name(createTokenByteList());
        return result;
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

    public int nextToken() throws IOException { //mri: yylex

        token = yylex();

        updateTokenPosition();

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

    @Override
    protected void setCompileOptionFlag(String name, ByteList value) {
        if (tokenSeen) {
            warning("'%s' is ignored after any tokens", name);
            return;
        }
    }

    @Override
    protected RegexpOptions parseRegexpFlags() throws IOException {
        StringBuilder unknownFlags = new StringBuilder(10);
        RegexpOptions options = parseRegexpFlags(unknownFlags);
        if (unknownFlags.length() != 0) {
            compile_error("unknown regexp option" +
                    (unknownFlags.length() > 1 ? "s" : "") + " - " + unknownFlags);
        }
        return options;
    }

    @Override
    protected void mismatchedRegexpEncodingError(Encoding optionEncoding, Encoding encoding) {
        compile_error("regexp encoding option '" + optionsEncodingChar(optionEncoding) +
                "' differs from source encoding '" + encoding + "'");
    }

    @Override
    protected void setTokenInfo(String name, ByteList value) {

    }

    protected void setEncoding(ByteList name) {
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

    public StrTerm getStrTerm() {
        return lex_strterm;
    }
    
    public void setStrTerm(StrTerm strterm) {
        this.lex_strterm = strterm;
    }

    // STR_NEW3/parser_str_new
    public IRubyObject createStr(ByteList buffer, int flags) {
        Encoding bufferEncoding = buffer.getEncoding();
        int codeRange = StringSupport.codeRangeScan(bufferEncoding, buffer);

        if ((flags & STR_FUNC_REGEXP) == 0 && bufferEncoding.isAsciiCompatible()) {
            // If we have characters outside 7-bit range and we are still ascii then change to ascii-8bit
            if (codeRange == CR_7BIT) {
                // Do nothing like MRI
            } else if (getEncoding() == USASCII_ENCODING &&
                    bufferEncoding != UTF8_ENCODING) {
                codeRange = RubyParserBase.associateEncoding(buffer, ASCII8BIT_ENCODING, codeRange);
            }
        }

        RubyString newString = getRuntime().newString(buffer);
        newString.setCodeRange(codeRange);
        return newString;
    }
    
    /**
     * What type/kind of quote are we dealing with?
     * 
     * @param c first character the the quote construct
     * @return a token that specifies the quote type
     */
    private int parseQuote(int c) {
        int begin, end;
        boolean shortHand;

        if (c == EOF) {
            compile_error("unterminated quoted string meets end of file");
            return EOF;
        }

        // Short-hand (e.g. %{,%.,%!,... versus %Q{).
        if (!Character.isLetterOrDigit(c)) {
            begin = c;
            c = 'Q';
            shortHand = true;
        // Long-hand (e.g. %Q{}).
        } else {
            shortHand = false;
            begin = nextc();
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

        switch (c) {
        case 'Q':
            lex_strterm = new StringTerm(str_dquote, begin ,end, ruby_sourceline);
            yaccValue = "%"+ (shortHand ? (""+end) : ("" + c + begin));
            return tSTRING_BEG;

        case 'q':
            lex_strterm = new StringTerm(str_squote, begin, end, ruby_sourceline);
            yaccValue = "%"+c+begin;
            return tSTRING_BEG;

        case 'W':
            lex_strterm = new StringTerm(str_dword, begin, end, ruby_sourceline);
            yaccValue = "%"+c+begin;
            return tWORDS_BEG;

        case 'w':
            lex_strterm = new StringTerm(str_sword, begin, end, ruby_sourceline);
            yaccValue = "%"+c+begin;
            return tQWORDS_BEG;

        case 'x':
            lex_strterm = new StringTerm(str_xquote, begin, end, ruby_sourceline);
            yaccValue = "%"+c+begin;
            return tXSTRING_BEG;

        case 'r':
            lex_strterm = new StringTerm(str_regexp, begin, end, ruby_sourceline);
            yaccValue = "%"+c+begin;
            return tREGEXP_BEG;

        case 's':
            lex_strterm = new StringTerm(str_ssym, begin, end, ruby_sourceline);
            setState(EXPR_FNAME|EXPR_FITEM);
            yaccValue = "%"+c+begin;
            return tSYMBEG;

        case 'I':
            lex_strterm = new StringTerm(str_dword, begin, end, ruby_sourceline);
            yaccValue = "%" + c + begin;
            return tSYMBOLS_BEG;

        case 'i':
            lex_strterm = new StringTerm(str_sword, begin, end, ruby_sourceline);
            yaccValue = "%" + c + begin;
            return tQSYMBOLS_BEG;
        default:
            compile_error("unknown type of %string");
        }
        return -1; // not-reached
    }
    
    private int hereDocumentIdentifier() {
        int c = nextc();
        int term;
        int indent = 0;

        int func = 0;
        if (c == '-') {
            c = nextc();
            func = STR_FUNC_INDENT;
        } else if (c == '~') {
            c = nextc();
            func = STR_FUNC_INDENT;
            indent = Integer.MAX_VALUE;
        }

        int token = tSTRING_BEG;
        ByteList markerValue = new ByteList();
        markerValue.setEncoding(getEncoding());
        if (c == '\'' || c == '"' || c == '`') {
            if (c == '\'') {
                yaccValue = Q;
                func |= str_squote;
            } else if (c == '"') {
                yaccValue = QQ;
                func |= str_dquote;
            } else {
                yaccValue = BACKTICK;
                token = tXSTRING_BEG;
                func |= str_xquote;
            }

            term = c;
            while ((c = nextc()) != term) {
                if (c == EOF || c == '\r' || c == '\n') {
                    parse_error("unterminated here document identifier");
                    return EOF;
                }

                tokadd_mbchar(c, markerValue);
            }

            // c == term.  This differs from MRI in that we unwind term symbol so we can make
            // our marker with just tokp and lex_p info (e.g. we don't make second numberBuffer).
            pushback(term);
            nextc();
        } else {
            if (!isIdentifierChar(c)) {
                pushback(c);
                if ((func & STR_FUNC_INDENT) != 0) {
                    pushback(heredoc_indent > 0 ? '~' : '-');
                }
                return 0;
            }
            func |= str_dquote;
            do {
                if (!tokadd_mbchar(c, markerValue)) return EOF;
            } while ((c = nextc()) != EOF && isIdentifierChar(c));
            pushback(c);
        }

        dispatchScanEvent(tHEREDOC_BEG);
        int len = lex_p - lex_pbeg;        
        lex_goto_eol();
        lex_strterm = new HeredocTerm(markerValue, func, len, ruby_sourceline, lex_lastline);
        heredoc_indent = indent;
        heredoc_line_indent = 0;
        flush();
        return token;
    }

    public ByteList tokenByteList() {
        int pos = tokp - lex_pbeg;
        int len = lex_p - tokp;

        return lexb.makeShared(pos, len);
    }
    
    private boolean arg_ambiguous(char c) {
        parser.dispatch("on_arg_ambiguous", getRuntime().newString("" + c));
        return true;
    }

    /*
     * Not normally used, but is left in here since it can be useful in debugging
     * grammar and lexing problems.
     *
     */
    private void printToken(int token) {
        //System.out.print("LOC: " + support.getPosition() + " ~ ");

        switch (token) {
            case yyErrorCode: System.err.print("yyErrorCode,"); break;
            // missing some 
            case tIDENTIFIER: System.err.print("tIDENTIFIER["+ value() + "],"); break;
            case tFID: System.err.print("tFID[" + value() + "],"); break;
            case tGVAR: System.err.print("tGVAR[" + value() + "],"); break;
            case tIVAR: System.err.print("tIVAR[" + value() +"],"); break;
            case tCONSTANT: System.err.print("tCONSTANT["+ value() +"],"); break;
            case tCVAR: System.err.print("tCVAR,"); break;
            case tINTEGER: System.err.print("tINTEGER,"); break;
            case tFLOAT: System.err.print("tFLOAT,"); break;
            case tSTRING_CONTENT: System.err.print("tSTRING_CONTENT[" +  value() + "],"); break;
            case tSTRING_BEG: System.err.print("tSTRING_BEG,"); break;
            case tSTRING_END: System.err.print("tSTRING_END,"); break;
            case tSTRING_DBEG: System.err.print("tSTRING_DBEG,"); break;
            case tSTRING_DVAR: System.err.print("tSTRING_DVAR,"); break;
            case tXSTRING_BEG: System.err.print("tXSTRING_BEG,"); break;
            case tREGEXP_BEG: System.err.print("tREGEXP_BEG,"); break;
            case tREGEXP_END: System.err.print("tREGEXP_END,"); break;
            case tWORDS_BEG: System.err.print("tWORDS_BEG,"); break;
            case tQWORDS_BEG: System.err.print("tQWORDS_BEG,"); break;
            case tBACK_REF: System.err.print("tBACK_REF,"); break;
            case tNTH_REF: System.err.print("tNTH_REF,"); break;
            case tUPLUS: System.err.print("tUPLUS"); break;
            case tUMINUS: System.err.print("tUMINUS,"); break;
            case tPOW: System.err.print("tPOW,"); break;
            case tCMP: System.err.print("tCMP,"); break;
            case tEQ: System.err.print("tEQ,"); break;
            case tEQQ: System.err.print("tEQQ,"); break;
            case tNEQ: System.err.print("tNEQ,"); break;
            case tGEQ: System.err.print("tGEQ,"); break;
            case tLEQ: System.err.print("tLEQ,"); break;
            case tANDOP: System.err.print("tANDOP,"); break;
            case tOROP: System.err.print("tOROP,"); break;
            case tMATCH: System.err.print("tMATCH,"); break;
            case tNMATCH: System.err.print("tNMATCH,"); break;
            case tDOT2: System.err.print("tDOT2,"); break;
            case tDOT3: System.err.print("tDOT3,"); break;
            case tAREF: System.err.print("tAREF,"); break;
            case tASET: System.err.print("tASET,"); break;
            case tLSHFT: System.err.print("tLSHFT,"); break;
            case tRSHFT: System.err.print("tRSHFT,"); break;
            case tCOLON2: System.err.print("tCOLON2,"); break;
            case tCOLON3: System.err.print("tCOLON3,"); break;
            case tOP_ASGN: System.err.print("tOP_ASGN,"); break;
            case tASSOC: System.err.print("tASSOC,"); break;
            case tLPAREN: System.err.print("tLPAREN,"); break;
            case tLPAREN_ARG: System.err.print("tLPAREN_ARG,"); break;
            case tLBRACK: System.err.print("tLBRACK,"); break;
            case tLBRACE: System.err.print("tLBRACE,"); break;
            case tLBRACE_ARG: System.err.print("tLBRACE_ARG,"); break;
            case tSTAR: System.err.print("tSTAR,"); break;
            case tAMPER: System.err.print("tAMPER,"); break;
            case tSYMBEG: System.err.print("tSYMBEG,"); break;
            case tLAMBDA: System.err.print("tLAMBDA,"); break;
            case tLAMBEG: System.err.print("tLAMBEG,"); break;
            case tLABEL: System.err.print("tLABEL("+ value() +":),"); break;
            case tLABEL_END: System.err.print("tLABEL_END"); break;
            case '\n': System.err.println("NL"); break;
            case EOF: System.out.println("EOF"); break;
            case tDSTAR: System.err.print("tDSTAR"); break;
            case tSTRING_DEND: System.err.print("tDSTRING_DEND,"); break;
            case tBDOT3: System.err.print("tBDOT3,"); break;
            case tBDOT2: System.err.print("tBDOT2,"); break;
            default: System.err.print("'" + (char)token + "'[" + token + "]"); break;
        }
    }
    
    public boolean hasScanEvent() {
        if (lex_p < tokp) {
            throw parser.getRuntime().newRuntimeError("lex_p < tokp");
        }
        
        return lex_p > tokp;
    }
    
    public void dispatchDelayedToken(int token) { //mri: ripper_dispatch_delayed_token
        int saved_line = ruby_sourceline;
        int saved_tokp = tokp;

        if (delayed == null) return;
        ruby_sourceline = delayed_line;
        tokp = lex_pbeg + delayed_col;

        String event = tokenToEventId(token);
        IRubyObject value = delayed == null ? parser.context.nil : parser.getRuntime().newString(delayed.dup());
        
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
        // FIXME: identValue is a common value but this is really bytelist-based and not totally String-friendly
        identValue = value.asJavaString();
        IRubyObject returnValue = parser.dispatch(event, value);
        flush();
        return returnValue;
    }
    
    private String tokenToEventId(int token) {
        switch(token) {
            case ' ': return "on_words_sep";
            case '!': return "on_op";
            case '%': return "on_op";
            case tANDDOT: return "on_op";
            case '&': return "on_op";
            case '*': return "on_op";
            case '+': return "on_op";
            case '-': return "on_op";
            case '/': return "on_op";
            case '<': return "on_op";
            case '=': return "on_op";
            case '>': return "on_op";
            case '?': return "on_op";
            case '^': return "on_op";
            case '|': return "on_op";
            case '~': return "on_op";
            case ':': return "on_op";
            case ',': return "on_comma";
            case '.': return "on_period";
            case ';': return "on_semicolon";
            case '`': return "on_backtick";
            case '\n': return "on_nl";
            case keyword_alias: return "on_kw";
            case keyword_and: return "on_kw";
            case keyword_begin: return "on_kw";
            case keyword_break: return "on_kw";
            case keyword_case: return "on_kw";
            case keyword_class: return "on_kw";
            case keyword_def: return "on_kw";
            case keyword_defined: return "on_kw";
            case keyword_do: return "on_kw";
            case keyword_do_block: return "on_kw";
            case keyword_do_cond: return "on_kw";
            case keyword_else: return "on_kw";
            case keyword_elsif: return "on_kw";
            case keyword_end: return "on_kw";
            case keyword_ensure: return "on_kw";
            case keyword_false: return "on_kw";
            case keyword_for: return "on_kw";
            case keyword_if: return "on_kw";
            case modifier_if: return "on_kw";
            case keyword_in: return "on_kw";
            case keyword_module: return "on_kw";
            case keyword_next: return "on_kw";
            case keyword_nil: return "on_kw";
            case keyword_not: return "on_kw";
            case keyword_or: return "on_kw";
            case keyword_redo: return "on_kw";
            case keyword_rescue: return "on_kw";
            case modifier_rescue: return "on_kw";
            case keyword_retry: return "on_kw";
            case keyword_return: return "on_kw";
            case keyword_self: return "on_kw";
            case keyword_super: return "on_kw";
            case keyword_then: return "on_kw";
            case keyword_true: return "on_kw";
            case keyword_undef: return "on_kw";
            case keyword_unless: return "on_kw";
            case modifier_unless: return "on_kw";
            case keyword_until: return "on_kw";
            case modifier_until: return "on_kw";
            case keyword_when: return "on_kw";
            case keyword_while: return "on_kw";
            case modifier_while: return "on_kw";
            case keyword_yield: return "on_kw";
            case keyword__FILE__: return "on_kw";
            case keyword__LINE__: return "on_kw";
            case keyword__ENCODING__: return "on_kw";
            case keyword_BEGIN: return "on_kw";
            case keyword_END: return "on_kw";
            case keyword_do_LAMBDA: return "on_kw";
            case tAMPER: return "on_op";
            case tANDOP: return "on_op";
            case tAREF: return "on_op";
            case tASET: return "on_op";
            case tASSOC: return "on_op";
            case tBACK_REF: return "on_backref";
            case tBDOT2: return "on_op";
            case tBDOT3: return "on_op";
            case tCHAR: return "on_CHAR";
            case tCMP: return "on_op";
            case tCOLON2: return "on_op";
            case tCOLON3: return "on_op";
            case tCONSTANT: return "on_const";
            case tCVAR: return "on_cvar";
            case tDOT2: return "on_op";
            case tDOT3: return "on_op";
            case tEQ: return "on_op";
            case tEQQ: return "on_op";
            case tFID: return "on_ident";
            case tFLOAT: return "on_float";
            case tGEQ: return "on_op";
            case tGVAR: return "on_gvar";
            case tIDENTIFIER: return "on_ident";
            case tIMAGINARY: return "on_imaginary";
            case tINTEGER: return "on_int";
            case tIVAR: return "on_ivar";
            case tLBRACE: return "on_lbrace";
            case tLBRACE_ARG: return "on_lbrace";
            case '{': return "on_lbrace";
            case '}': return "on_rbrace";
            case tLBRACK: return "on_lbracket";
            case '[': return "on_lbracket";
            case ']': return "on_rbracket";
            case tLEQ: return "on_op";
            case tLPAREN: return "on_lparen";
            case tLPAREN_ARG: return "on_lparen";
            case '(': return "on_lparen";
            case ')': return "on_rparen";  // ENEBO: Don't this this can happen.
            case tLSHFT: return "on_op";
            case tMATCH: return "on_op";
            case tNEQ: return "on_op";
            case tNMATCH: return "on_op";
            case tNTH_REF: return "on_backref";
            case tOP_ASGN: return "on_op";
            case tOROP: return "on_op";
            case tPOW: return "on_op";
            case tQSYMBOLS_BEG: return "on_qsymbols_beg";
            case tRATIONAL: return "on_rational";
            case tSYMBOLS_BEG: return "on_symbols_beg";
            case tQWORDS_BEG: return "on_qwords_beg";
            case tREGEXP_BEG:return "on_regexp_beg";
            case tREGEXP_END: return "on_regexp_end";
            case tRSHFT: return "on_op";
            case tSTAR: return "on_op";
            case tDSTAR: return "on_op";
            case tSTRING_BEG: return "on_tstring_beg";
            case tSTRING_CONTENT: return "on_tstring_content";
            case tSTRING_DBEG: return "on_embexpr_beg";
            case tSTRING_DEND: return "on_embexpr_end";
            case tSTRING_DVAR: return "on_embvar";
            case tSTRING_END: return "on_tstring_end";
            case tSYMBEG: return "on_symbeg";
            case tUMINUS: return "on_op";
            case tUMINUS_NUM: return "on_op";
            case tUPLUS: return "on_op";
            case tWORDS_BEG: return "on_words_beg";
            case tXSTRING_BEG: return "on_backtick";
            case tLABEL: return "on_label";
            case tLABEL_END: return "on_label_end";
            case tLAMBDA: return "on_tlambda";
            case tLAMBEG: return "on_tlambeg";

            // ripper specific tokens
            case tIGNORED_NL: return "on_ignored_nl";
            case tCOMMENT: return "on_comment";
            case tEMBDOC_BEG: return "on_embdoc_beg";
            case tEMBDOC: return "on_embdoc";
            case tEMBDOC_END: return "on_embdoc_end";
            case tSP: return "on_sp";
            case tHEREDOC_BEG: return "on_heredoc_beg";
            case tHEREDOC_END: return "on_heredoc_end";
            case k__END__: return "on___end__";
            default: // Weird catchall but we will try and not use < 256 value trick like MRI
                return "on_CHAR";
        }
    }

    /**
     *  Returns the next token. Also sets yyVal as needed.
     *
     * @return    the next token
     */
    private int yylex() throws IOException {
        int c;
        boolean spaceSeen = false;
        boolean commandState;
        boolean tokenSeen = this.tokenSeen;

        if (lex_strterm != null) return lex_strterm.parseString(this, src);

        commandState = commandStart;
        commandStart = false;
        this.tokenSeen = true;
        boolean fallthru = false;

        loop: for(;;) {
            last_state = lex_state;
            c = nextc();
            switch(c) {
            case '\000': /* NUL */
            case '\004': /* ^D */
            case '\032': /* ^Z */
            case EOF:	 /* end of script. */
                return -1;
           
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
                dispatchScanEvent(tSP);
                continue;
            }
            case '#': { /* it's a comment */
                this.tokenSeen = tokenSeen;
                if (!parser_magic_comment(lexb.makeShared(lex_p, lex_pend - lex_p))) {
                    if (comment_at_top()) set_file_encoding(lex_p, lex_pend);
                }
                lex_goto_eol();
                dispatchScanEvent(tCOMMENT);

                fallthru = true;
            }
            /* fall through */
            case '\n': {
                this.tokenSeen = tokenSeen;
                boolean normalArg = IS_lex_state(lex_state, EXPR_BEG | EXPR_CLASS | EXPR_FNAME | EXPR_DOT) &&
                        !IS_lex_state(lex_state, EXPR_LABELED);
                if (normalArg || IS_lex_state_all(lex_state, EXPR_ARG | EXPR_LABELED)) {
                    if (!fallthru) dispatchScanEvent(tIGNORED_NL);
                    fallthru = false;
                    if (!normalArg && getLexContext().in_kwarg) {
                        commandStart = true;
                        setState(EXPR_BEG);
                        return '\n';
                    }
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
                        case '#':
                            pushback(c);
                            if (spaceSeen) dispatchScanEvent(tSP);
                            continue loop;
                        case '&':
                        case '.': {
                            dispatchDelayedToken(tIGNORED_NL);
                            if (peek('.') == (c == '&')) {
                                pushback(c);

                                dispatchScanEvent(tSP);
                                continue loop;
                            }
                        }
                        default:
                            ruby_sourceline--;
                            lex_nextline = lex_lastline;
                        case -1:        // EOF (ENEBO: After default?
                            lex_goto_eol();
                            if (c != -1) tokp = lex_p;
                            done = true;
                    }
                }

                commandStart = true;
                setState(EXPR_BEG);
                return '\n';
            }
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
                        
                        dispatchScanEvent(tEMBDOC_BEG);
                        for (;;) {
                            lex_goto_eol();
                            
                            if (!first_p) dispatchScanEvent(tEMBDOC);
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
                        dispatchScanEvent(tEMBDOC_END);
                        
                        continue loop;
                    }
                }

                setState(IS_AFTER_OPERATOR() ? EXPR_ARG : EXPR_BEG);

                c = nextc();
                if (c == '=') {
                    c = nextc();
                    if (c == '=') {
                        yaccValue = symbol(EQ_EQ_EQ);
                        return tEQQ;
                    }

                    pushback(c);
                    yaccValue = symbol(EQ_EQ);
                    return tEQ;
                }
                if (c == '~') {
                    yaccValue = symbol(EQ_TILDE);
                    return tMATCH;
                } else if (c == '>') {
                    yaccValue = symbol(EQ_GT);
                    return tASSOC;
                }
                pushback(c);
                yaccValue = symbol(EQ);
                return '=';
                
            case '<':
                return lessThan(spaceSeen);
            case '>':
                return greaterThan();
            case '"':
                return doubleQuote(commandState);
            case '`':
                return backtick(commandState);
            case '\'':
                return singleQuote(commandState);
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
                setState(EXPR_BEG);
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
                    dispatchScanEvent(tSP);
                    continue;
                }
                if (c == ' ') return tSP;
                if (Character.isWhitespace(c)) return c;
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
                    dispatchScanEvent(k__END__);
                    return EOF;
                }
                return identifier(c, commandState);
            default:
                return identifier(c, commandState);
            }
        }
    }

    private RubySymbol symbol(ByteList name) {
        return getRuntime().newSymbol(name);
    }

    private int identifierToken(int last_state, int result, String value) {
        if (result == tIDENTIFIER && !IS_lex_state(last_state, EXPR_DOT|EXPR_FNAME) &&
                parser.getCurrentScope().isDefined(value) >= 0) {
            setState(EXPR_END|EXPR_LABEL);
        }

        set_yylval_name(createTokenByteList());
        identValue = value.intern();
        return result;
    }
    
    private int ampersand(boolean spaceSeen) {
        int c = nextc();
        
        switch (c) {
        case '&':
            setState(EXPR_BEG);
            if ((c = nextc()) == '=') {
                yaccValue = AMPERSAND_AMPERSAND;
                set_yylval_id(AMPERSAND_AMPERSAND);
                setState(EXPR_BEG);
                return tOP_ASGN;
            }
            pushback(c);
            yaccValue = AMPERSAND_AMPERSAND;
            return tANDOP;
        case '=':
            yaccValue = AMPERSAND;
            set_yylval_id(AMPERSAND);
            setState(EXPR_BEG);
            return tOP_ASGN;
        case '.':
            setState(EXPR_DOT);
            yaccValue = AMPERSAND_DOT;
            set_yylval_id(AMPERSAND_DOT);
            return tANDDOT;
        }
        pushback(c);

        if (IS_SPCARG(c, spaceSeen)) {
            if (isVerbose()) warning("'&' interpreted as argument prefix");
            c = tAMPER;
        } else if (IS_BEG()) {
            c = warn_balanced(c, spaceSeen, tAMPER, "&", "argument prefix");
        } else {
            c = '&';
        }

        setState(IS_AFTER_OPERATOR() ? EXPR_ARG : EXPR_BEG);

        yaccValue = AMPERSAND;
        return c;
    }

    private int at() {
        newtok(true);
        int c = nextc();
        int result;
        if (c == '@') {
            c = nextc();
            result = tCVAR;
        } else {
            result = tIVAR;
        }
        setState(IS_lex_state(last_state, EXPR_FNAME) ? EXPR_ENDFN : EXPR_END);

        if (c == EOF || !isIdentifierChar(c)) {
            if (result == tIVAR) {
                compile_error("'@' without identifiers is not allowed as an instance variable name");
            } else {
                compile_error("'@@' without identifiers is not allowed as a class variable name");
            }
            set_yylval_noname();
            setState(EXPR_END);
            return result;
        } else if (Character.isDigit(c)) {
            pushback(c);
            if (result == tIVAR) {
                compile_error("'@" + ((char) c) + "' is not allowed as an instance variable name");
            } else {
                compile_error("'@@" + ((char) c) + "' is not allowed as a class variable name");
            }
            set_yylval_noname();
            setState(EXPR_END);
            return result;
        }

        if (!tokadd_ident(c)) return EOF;

        return tokenize_ident(result);
    }
    
    private int backtick(boolean commandState) {
        yaccValue = BACKTICK;

        if (IS_lex_state(lex_state, EXPR_FNAME)) {
            setState(EXPR_ENDFN);
            return '`';
        }
        if (IS_lex_state(lex_state, EXPR_DOT)) {
            setState(commandState ? EXPR_CMDARG : EXPR_ARG);

            return '`';
        }

        lex_strterm = new StringTerm(str_xquote, '\0', '`', ruby_sourceline);
        return tXSTRING_BEG;
    }
    
    private int bang() {
        int c = nextc();

        if (IS_AFTER_OPERATOR()) {
            setState(EXPR_ARG);
            if (c == '@') {
                yaccValue = BANG;
                return '!';
            }
        } else {
            setState(EXPR_BEG);
        }
        
        switch (c) {
        case '=':
            yaccValue = BANG_EQ;

            return tNEQ;
        case '~':
            yaccValue = BANG_TILDE;

            return tNMATCH;
        default: // Just a plain bang
            pushback(c);
            yaccValue = BANG;

            return '!';
        }
    }
    
    private int caret() {
        yaccValue = CARET;

        int c = nextc();
        if (c == '=') {
            setState(EXPR_BEG);
            set_yylval_id(CARET);
            return tOP_ASGN;
        }

        setState(IS_AFTER_OPERATOR() ? EXPR_ARG : EXPR_BEG);
        
        pushback(c);
        return '^';
    }

    private int colon(boolean spaceSeen) {
        int c = nextc();
        
        if (c == ':') {
            if (IS_BEG() || IS_lex_state(lex_state, EXPR_CLASS) || (IS_ARG() && spaceSeen)) {
                setState(EXPR_BEG);
                yaccValue = COLON_COLON;
                return tCOLON3;
            }
            setState(EXPR_DOT);
            yaccValue = COLON_COLON;
            set_yylval_id(COLON_COLON);
            return tCOLON2;
        }

        if (IS_END() || Character.isWhitespace(c) || c == '#') {
            pushback(c);
            setState(EXPR_BEG);
            yaccValue = COLON;
            return warn_balanced(c, spaceSeen, ':', ":", "symbol literal");
        }
        
        switch (c) {
        case '\'':
            lex_strterm = new StringTerm(str_ssym, '\0', c, ruby_sourceline);
            break;
        case '"':
            lex_strterm = new StringTerm(str_dsym, '\0', c, ruby_sourceline);
            break;
        default:
            pushback(c);
            break;
        }

        setState(EXPR_FNAME);
        return tSYMBEG;
    }

    private int comma(int c) {
        setState(EXPR_BEG|EXPR_LABEL);
        return c;
    }

    private int doKeyword(int state) {
        if (isLambdaBeginning()) {
            setLeftParenBegin(-1);
            return keyword_do_LAMBDA;
        }

        if (conditionState.set_p()) return keyword_do_cond;

        if (cmdArgumentState.set_p() && !IS_lex_state(state, EXPR_CMDARG)) {
            return keyword_do_block;
        }

        return keyword_do;
    }
    
    private int dollar() {
        setState(EXPR_END);
        newtok(true);
        int c = nextc();
        
        switch (c) {
        case '_':       /* $_: last read line string */
            c = nextc();
            if (isIdentifierChar(c)) {
                if (!tokadd_ident(c)) return EOF;

                last_state = lex_state;
                return identifierToken(last_state, tGVAR, createTokenString().intern());
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
            set_yylval_name(new ByteList(new byte[] {'$', (byte) c}));
            return tGVAR;

        case '-':
            c = nextc();
            if (isIdentifierChar(c)) {
                if (!tokadd_mbchar(c)) return EOF;
            } else {
                pushback(c);
                pushback('-');
                return '$';
            }
            identValue = createTokenString().intern();
            /* xxx shouldn't check if valid option variable */
            return tGVAR;

        case '&':       /* $&: last match */
        case '`':       /* $`: string before last match */
        case '\'':      /* $': string after last match */
        case '+':       /* $+: string matches last paren. */
            // Explicit reference to these vars as symbols...
            if (IS_lex_state(last_state, EXPR_FNAME)) {
                identValue = "$" + (char) c;
                set_yylval_name(new ByteList(new byte[] {'$', (byte) c}));
                return tGVAR;
            }

            identValue = "$" + (char) c;
            return tBACK_REF;

        case '1': case '2': case '3': case '4': case '5': case '6':
        case '7': case '8': case '9':
            do {
                c = nextc();
            } while (Character.isDigit(c));
            pushback(c);
            if (IS_lex_state(last_state, EXPR_FNAME)) {
                identValue = createTokenString().intern();
                set_yylval_name(new ByteList(new byte[] {'$', (byte) c}));
                return tGVAR;
            }

            String refAsString = createTokenString();

            try {
                Integer.parseInt(refAsString.substring(1).intern());
            } catch (NumberFormatException e) {
                warn("'" + refAsString + "' is too big for a number variable, always nil");
            }

            identValue = createTokenString().intern();
            return tNTH_REF;
        case '0':
            setState(EXPR_END);

            return identifierToken(last_state, tGVAR, ("$" + (char) c).intern());
        default:
            if (!isIdentifierChar(c)) {
                if (c == EOF || isSpace(c)) {
                    compile_error("'$' without identifiers is not allowed as a global variable name");
                } else {
                    pushback(c);
                    compile_error("'$" + ((char) c) + "' is not allowed as a global variable name");
                }
                return EOF;
            }

            last_state = lex_state;
            setState(EXPR_END);

            tokadd_ident(c);

            return identifierToken(last_state, tGVAR, createTokenString().intern()); // $blah
        }
    }
    
    private int dot() {
        int c;

        boolean isBeg = IS_BEG();
        setState(EXPR_BEG);
        if ((c = nextc()) == '.') {
            if ((c = nextc()) == '.') {
                yaccValue = DOT_DOT_DOT;

                if (getLexContext().in_argdef) {
                    setState(EXPR_ENDARG);
                    return tBDOT3;
                }

                if (parenNest == 0 && isLookingAtEOL()) {
                    warn("... at EOL, should be parenthesized?");
                } else if (getLeftParenBegin() >= 0 && getLeftParenBegin() + 1 == parenNest) {
                    if (IS_lex_state(last_state, EXPR_LABEL)) {
                        return tDOT3;
                    }
                }

                return isBeg ? tBDOT3 : tDOT3;
            }
            pushback(c);
            yaccValue = DOT_DOT;
            return isBeg ? tBDOT2 : tDOT2;
        }

        pushback(c);
        if (Character.isDigit(c)) compile_error("no .<digit> floating literal anymore; put 0 before dot");
        
        setState(EXPR_DOT);
        yaccValue = DOT;
        set_yylval_id(DOT);
        return '.';
    }
    
    private int doubleQuote(boolean commandState) {
        int label = IS_LABEL_POSSIBLE(commandState) ? str_label : 0;
        lex_strterm = new StringTerm(str_dquote|label, '\0', '"', ruby_sourceline);
        yaccValue = QQ;
        return tSTRING_BEG;
    }
    
    private int greaterThan() {
        setState(IS_AFTER_OPERATOR() ? EXPR_ARG : EXPR_BEG);

        int c = nextc();

        switch (c) {
        case '=':
            yaccValue = GT_EQ;

            return tGEQ;
        case '>':
            if ((c = nextc()) == '=') {
                setState(EXPR_BEG);
                yaccValue = GT_GT;
                set_yylval_id(GT_GT);
                return tOP_ASGN;
            }
            pushback(c);

            yaccValue = GT_GT;
            return tRSHFT;
        default:
            pushback(c);
            yaccValue = GT;
            return '>';
        }
    }

    private int identifier(int c, boolean commandState) {
        if (!isIdentifierChar(c)) {
            String badChar = "\\" + Integer.toOctalString(c & 0xff);
            compile_error("Invalid char '" + badChar + "' ('" + (char) c + "') in expression");
        }
        // FIXME: on_kw: will return BOM as part of the ident string "\xfeffclass" on MRI and Yard also seems
        // to need this to properly parse.  So I record where token should really start so I can extract as
        // a proper ident for keyword check but createTempValue below will end up creating the bom + kw.  This feels like
        // an MRI bug but it is well baked into libraries at this point???  newtok+tokadd is still different from MRI
        // and does not construct a temp buf.  Once I convert that to be the same I think I can do exactly what MRI does
        // and this hack can disappear.
        int whereKeywordShouldStart = lex_p - 1;

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
        String tempVal;
        if (lastBangOrPredicate) {
            result = tFID;
            tempVal = createTokenString();
        } else {
            if (IS_lex_state(lex_state, EXPR_FNAME)) {
                if ((c = nextc()) == '=') { 
                    int c2 = nextc();

                    if (c2 != '~' && c2 != '>' &&
                            (c2 != '=' || peek('>'))) {
                        result = tIDENTIFIER;
                        pushback(c2);
                    } else { 
                        pushback(c2);
                        pushback(c);
                    }
                } else {
                    pushback(c);
                }
            }
            tempVal = createTokenString();

            if (result == 0 && Character.isUpperCase(tempVal.charAt(0))) {
                result = tCONSTANT;
            } else {
                result = tIDENTIFIER;
            }
        }

        if (IS_LABEL_POSSIBLE(commandState)) {
            if (IS_LABEL_SUFFIX()) {
                setState(EXPR_ARG|EXPR_LABELED);
                yaccValue = tempVal;
                identValue = tempVal.intern();
                set_yylval_name(createTokenByteList());
                nextc();
                return tLABEL;
            }
        }

        if (lex_state != EXPR_DOT) {
            Keyword keyword = getKeyword(createTokenString(whereKeywordShouldStart)); // Is it is a keyword?

            if (keyword != null) {
                int state = lex_state; // Save state at time keyword is encountered
                setState(keyword.state);
                set_yylval_name(createTokenByteList());

                if (IS_lex_state(state, EXPR_FNAME)) {
                    setState(EXPR_ENDFN);
                    identValue = tempVal;
                    return keyword.id0;
                }

                if (IS_lex_state(lex_state, EXPR_BEG)) commandStart = true;

                if (keyword.id0 == keyword_do) return doKeyword(state);

                if (IS_lex_state(state, EXPR_BEG|EXPR_LABELED)) {
                    return keyword.id0;
                } else {
                    if (keyword.id0 != keyword.id1) setState(EXPR_BEG|EXPR_LABEL);
                    return keyword.id1;
                }
            }
        }

        if (IS_lex_state(lex_state, EXPR_BEG_ANY|EXPR_ARG_ANY|EXPR_DOT)) {
            setState(commandState ? EXPR_CMDARG : EXPR_ARG);
        } else if (lex_state == EXPR_FNAME) {
            setState(EXPR_ENDFN);
        } else {
            setState(EXPR_END);
        }
        
        return identifierToken(last_state, result, tempVal.intern());
    }

    private int leftBracket(boolean spaceSeen) {
        parenNest++;
        int c = '[';
        if (IS_AFTER_OPERATOR()) {
            if ((c = nextc()) == ']') {
                setState(EXPR_ARG);
                if (peek('=')) {
                    nextc();
                    yaccValue = LBRACKET_RBRACKET_EQ;
                    return tASET;
                }
                yaccValue = LBRACKET_RBRACKET;
                return tAREF;
            }
            pushback(c);
            setState(EXPR_ARG|EXPR_LABEL);
            yaccValue = LBRACKET;
            return '[';
        } else if (IS_BEG() || (IS_ARG() && (spaceSeen || IS_lex_state(lex_state, EXPR_LABELED)))) {
            c = tLBRACK;
        }

        setState(EXPR_BEG|EXPR_LABEL);
        conditionState.push0();
        cmdArgumentState.push0();
        yaccValue = LBRACKET;
        return c;
    }

    private int leftCurly() {
        braceNest++;
        char c;
        if (isLambdaBeginning()) {
            c = tLAMBEG;
        } else if (IS_lex_state(lex_state, EXPR_LABELED)) {
            c = tLBRACE;
        } else if (IS_lex_state(lex_state, EXPR_ARG_ANY|EXPR_END|EXPR_ENDFN)) { // block (primary)
            c = '{';
        } else if (IS_lex_state(lex_state, EXPR_ENDARG)) { // block (expr)
            c = tLBRACE_ARG;
        } else { // hash
            c = tLBRACE;
        }

        if (c != tLBRACE) {
            commandStart = true;
            setState(EXPR_BEG);
        } else {
            setState(EXPR_BEG|EXPR_LABEL);
        }

        parenNest++;
        conditionState.push0();
        cmdArgumentState.push0();
        yaccValue = ruby_sourceline;

        return c;
    }

    private int leftParen(boolean spaceSeen) {
        int result;

        if (IS_BEG()) {
            result = tLPAREN;
        } else if (!spaceSeen) {
            result = '(';
        } else if (IS_ARG() || IS_lex_state_all(lex_state, EXPR_END|EXPR_LABEL)) {
            result = tLPAREN_ARG;
        } else if (IS_lex_state(lex_state, EXPR_ENDFN) && !isLambdaBeginning()) {
            warn("parentheses after method name is interpreted as an argument list, not a decomposed argument");
            result = '(';
        } else {
            result = '(';
        }

        parenNest++;
        conditionState.push0();
        cmdArgumentState.push0();
        setState(EXPR_BEG|EXPR_LABEL);

        return result;
    }
    
    private int lessThan(boolean spaceSeen) {
        last_state = lex_state;
        int c = nextc();
        if (c == '<' && !IS_lex_state(lex_state, EXPR_DOT|EXPR_CLASS) &&
                !IS_END() && (!IS_ARG() || IS_lex_state(lex_state, EXPR_LABELED) || spaceSeen)) {
            int tok = hereDocumentIdentifier();
            
            if (tok != 0) return tok;
        }

        if (IS_AFTER_OPERATOR()) {
            setState(EXPR_ARG);
        } else {
            if (IS_lex_state(lex_state, EXPR_CLASS)) commandStart = true;
            setState(EXPR_BEG);
        }

        switch (c) {
        case '=':
            if ((c = nextc()) == '>') {
                yaccValue = LT_EQ_RT;
                return tCMP;
            }
            pushback(c);
            yaccValue = LT_EQ;
            return tLEQ;
        case '<':
            if ((c = nextc()) == '=') {
                setState(EXPR_BEG);
                yaccValue = LT_LT;
                return tOP_ASGN;
            }
            pushback(c);
            yaccValue = LT_LT;
            return warn_balanced(c, spaceSeen, tLSHFT, "<<", "here document");
        default:
            yaccValue = LT;
            pushback(c);
            return '<';
        }
    }
    
    private int minus(boolean spaceSeen) {
        int c = nextc();
        
        if (IS_AFTER_OPERATOR()) {
            setState(EXPR_ARG);
            if (c == '@') {
                yaccValue = MINUS_AT;
                return tUMINUS;
            }
            pushback(c);
            yaccValue = MINUS;
            return '-';
        }
        if (c == '=') {
            setState(EXPR_BEG);
            yaccValue = MINUS;
            set_yylval_id(MINUS);
            return tOP_ASGN;
        }
        if (c == '>') {
            setState(EXPR_ENDFN);
            yaccValue = MINUS_GT;
            return tLAMBDA;
        }
        if (IS_BEG() || (IS_SPCARG(c, spaceSeen) && arg_ambiguous('-'))) {
            setState(EXPR_BEG);
            pushback(c);
            yaccValue = MINUS_AT;
            if (Character.isDigit(c)) {
                return tUMINUS_NUM;
            }
            return tUMINUS;
        }
        setState(EXPR_BEG);
        pushback(c);
        yaccValue = MINUS;

        return warn_balanced(c, spaceSeen, '-', "-", "unary operator");
    }

    private int percent(boolean spaceSeen) {
        if (IS_BEG()) return parseQuote(nextc());

        int c = nextc();

        if (c == '=') {
            setState(EXPR_BEG);
            yaccValue = PERCENT;
            set_yylval_id(PERCENT);
            return tOP_ASGN;
        }

        if (IS_SPCARG(c, spaceSeen) || (IS_lex_state(lex_state, EXPR_FITEM) && c == 's')) return parseQuote(c);

        setState(IS_AFTER_OPERATOR() ? EXPR_ARG : EXPR_BEG);

        pushback(c);
        yaccValue = PERCENT;
        return warn_balanced(c, spaceSeen, '%', "%", "string literal");
    }

    private int pipe() {
        int c = nextc();
        
        switch (c) {
        case '|':
            setState(EXPR_BEG);
            if ((c = nextc()) == '=') {
                setState(EXPR_BEG);
                yaccValue = OR_OR;
                set_yylval_id(OR_OR);
                return tOP_ASGN;
            }
            pushback(c);
            if (IS_lex_state_all(last_state, EXPR_BEG)) {
                yaccValue = OR;
                pushback('|');
                return '|';
            }
            yaccValue = OR_OR;
            return tOROP;
        case '=':
            setState(EXPR_BEG);
            yaccValue = OR;
            set_yylval_id(OR);
            return tOP_ASGN;
        default:
            setState(IS_AFTER_OPERATOR() ? EXPR_ARG : EXPR_BEG|EXPR_LABEL);

            pushback(c);
            yaccValue = OR;
            return '|';
        }
    }
    
    private int plus(boolean spaceSeen) {
        int c = nextc();
        if (IS_AFTER_OPERATOR()) {
            setState(EXPR_ARG);
            if (c == '@') {
                yaccValue = PLUS_AT;
                return tUPLUS;
            }

            pushback(c);
            yaccValue = PLUS;
            return '+';
        }
        
        if (c == '=') {
            setState(EXPR_BEG);
            yaccValue = PLUS;
            set_yylval_id(PLUS);
            return tOP_ASGN;
        }

        if (IS_BEG() || (IS_SPCARG(c, spaceSeen) && arg_ambiguous('+'))) {
            setState(EXPR_BEG);
            pushback(c);
            if (Character.isDigit(c)) {
                c = '+';
                return parseNumber(c);
            }

            yaccValue = PLUS_AT;
            return tUPLUS;
        }

        setState(EXPR_BEG);
        pushback(c);
        yaccValue = PLUS;
        return warn_balanced(c, spaceSeen, '+', "+", "unary operator");
    }

    // FIXME: This is a bit different than regular parser but the problem
    // I ran into was not returning the '?' with the char it is finding.
    // This in part must be some difference between MRI and our lexer impls
    // doing things a little differently.
    private int questionMark() throws IOException {
        int c;
        
        if (IS_END()) {
            setState(EXPR_VALUE);
            yaccValue = QUESTION;
            return '?';
        }
        
        c = nextc();
        if (c == EOF) {
            compile_error("incomplete character syntax");
            return EOF;
        }

        if (Character.isWhitespace(c)){
            if (!IS_ARG()) {
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
            setState(EXPR_VALUE);
            yaccValue = QUESTION;
            return '?';
        } else if (!isASCII()) {
            if (!tokadd_mbchar(c)) return EOF;
            yaccValue = createTokenByteList(1);
            setState(EXPR_END);
            return tCHAR;
        } else if (isIdentifierChar(c) && !peek('\n') && isNext_identchar()) {
            pushback(c);
            setState(EXPR_VALUE);
            yaccValue = QUESTION;
            return '?';
        } else if (c == '\\') {
            if (peek('u')) {
                nextc(); // Eat 'u'
                ByteList oneCharBL = new ByteList(2);
                c = readUTFEscape(oneCharBL, false, new boolean[] { false });
                
                if (c >= 0x80) {
                    tokadd_mbchar(c, oneCharBL);
                } else {
                    oneCharBL.append(c);
                }
                
                setState(EXPR_END);
                yaccValue = oneCharBL;
                return tINTEGER; // FIXME: This should be something else like a tCHAR in 1.9/2.0
            } else {
                c = readEscape();
            }
        }

        ByteList oneCharBL = new ByteList(1);
        oneCharBL.setEncoding(getEncoding());
        oneCharBL.append(c);
        yaccValue = oneCharBL;
        setState(EXPR_END);
        return tCHAR;
    }

    private int rightBracket() {
        parenNest--;
        conditionState.pop();
        cmdArgumentState.pop();
        setState(EXPR_END);
        yaccValue = RBRACKET;
        return ']';
    }

    private int rightCurly() {
        yaccValue = RCURLY;
        braceNest--;
        if (braceNest < 0) return tSTRING_DEND;
        conditionState.pop();
        cmdArgumentState.pop();
        setState(EXPR_END);
        parenNest--;
        return '}';
    }

    private int rightParen() {
        parenNest--;
        conditionState.pop();
        cmdArgumentState.pop();
        setState(EXPR_ENDFN);
        yaccValue = RPAREN;
        return ')';
    }
    
    private int singleQuote(boolean commandState) {
        int label = IS_LABEL_POSSIBLE(commandState) ? str_label : 0;
        lex_strterm = new StringTerm(str_squote|label, '\0', '\'', ruby_sourceline);
        yaccValue = Q;

        return tSTRING_BEG;
    }
    
    private int slash(boolean spaceSeen) {
        if (IS_BEG()) {
            lex_strterm = new StringTerm(str_regexp, '\0', '/', ruby_sourceline);
            return tREGEXP_BEG;
        }
        
        int c = nextc();
        
        if (c == '=') {
            setState(EXPR_BEG);
            set_yylval_id(SLASH);
            return tOP_ASGN;
        }
        pushback(c);
        if (IS_SPCARG(c, spaceSeen)) {
            arg_ambiguous('/');
            lex_strterm = new StringTerm(str_regexp, '\0', '/', ruby_sourceline);
            return tREGEXP_BEG;
        }

        setState(IS_AFTER_OPERATOR() ? EXPR_ARG : EXPR_BEG);

        return warn_balanced(c, spaceSeen, '/', "/", "regexp literal");
    }

    private int star(boolean spaceSeen) {
        int c = nextc();
        
        switch (c) {
        case '*':
            if ((c = nextc()) == '=') {
                setState(EXPR_BEG);
                yaccValue = STAR_STAR;
                set_yylval_id(STAR_STAR);
                return tOP_ASGN;
            }

            pushback(c);
            yaccValue = STAR_STAR;

            if (IS_SPCARG(c, spaceSeen)) {
                if (isVerbose() && Options.PARSER_WARN_ARGUMENT_PREFIX.load()) warning("'**' interpreted as argument prefix");
                c = tDSTAR;
            } else if (IS_BEG()) {
                c = tDSTAR;
            } else {
                c = warn_balanced(c, spaceSeen, tPOW, "**", "argument prefix");
            }
            break;
        case '=':
            setState(EXPR_BEG);
            yaccValue = STAR;
            set_yylval_id(STAR);
            return tOP_ASGN;
        default:
            pushback(c);
            if (IS_SPCARG(c, spaceSeen)) {
                if (isVerbose() && Options.PARSER_WARN_ARGUMENT_PREFIX.load()) warning("'*' interpreted as argument prefix");
                c = tSTAR;
            } else if (IS_BEG()) {
                c = tSTAR;
            } else {
                c = warn_balanced(c, spaceSeen, '*', "*", "argument prefix");
            }
            yaccValue = STAR;
        }

        setState(IS_AFTER_OPERATOR() ? EXPR_ARG : EXPR_BEG);
        return c;
    }

    private int tilde() {
        int c;
        
        if (IS_AFTER_OPERATOR()) {
            if ((c = nextc()) != '@') pushback(c);
            setState(EXPR_ARG);
        } else {
            setState(EXPR_BEG);
        }

        yaccValue = TILDE;
        return '~';
    }

    private ByteList numberBuffer = new ByteList(10); // ascii is good enough.
    /**
     *  Parse a number from the input stream.
     *
     *@param c The first character of the number.
     *@return A int constant which represents a token.
     */
    private int parseNumber(int c) {
        setState(EXPR_END);
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
                                if (nondigit != 0) break;
                                nondigit = c;
                            } else if (isHexChar(c)) {
                                nondigit = 0;
                                numberBuffer.append((char) c);
                            } else {
                                break;
                            }
                        }
                    }
                    pushback(c);

                    if (numberBuffer.length() == startLen) {
                        parse_error("Hexadecimal number without hex-digits.");
                    } else if (nondigit != 0) {
                        compile_error("Trailing '_' in number.");
                    }
                    return setIntegerLiteral(numberLiteralSuffix(SUFFIX_ALL));
                case 'b' :
                case 'B' : // binary
                    c = nextc();
                    if (c == '0' || c == '1') {
                        for (;; c = nextc()) {
                            if (c == '_') {
                                if (nondigit != 0) break;
								nondigit = c;
                            } else if (c == '0' || c == '1') {
                                nondigit = 0;
                                numberBuffer.append((char) c);
                            } else {
                                break;
                            }
                        }
                    }
                    pushback(c);

                    if (numberBuffer.length() == startLen) {
                        compile_error_pos("Binary number without digits.");
                    } else if (nondigit != 0) {
                        compile_error_pos("Trailing '_' in number.");
                    }
                    return setIntegerLiteral(numberLiteralSuffix(SUFFIX_ALL));
                case 'd' :
                case 'D' : // decimal
                    c = nextc();
                    if (Character.isDigit(c)) {
                        for (;; c = nextc()) {
                            if (c == '_') {
                                if (nondigit != 0) break;
								nondigit = c;
                            } else if (Character.isDigit(c)) {
                                nondigit = 0;
                                numberBuffer.append((char) c);
                            } else {
                                break;
                            }
                        }
                    }
                    pushback(c);

                    if (numberBuffer.length() == startLen) {
                        compile_error_pos("Binary number without digits.");
                    } else if (nondigit != 0) {
                        compile_error_pos("Trailing '_' in number.");
                    }
                    return setIntegerLiteral(numberLiteralSuffix(SUFFIX_ALL));
                case 'o':
                case 'O':
                    c = nextc();
                case '0': case '1': case '2': case '3': case '4': //Octal
                case '5': case '6': case '7': case '_': 
                    for (;; c = nextc()) {
                        if (c == '_') {
                            if (nondigit != 0) break;

							nondigit = c;
                        } else if (c >= '0' && c <= '7') {
                            nondigit = 0;
                            numberBuffer.append((char) c);
                        } else {
                            break;
                        }
                    }
                    if (numberBuffer.length() > startLen) {
                        pushback(c);

                        if (nondigit != 0) compile_error_pos("Trailing '_' in number.");

                        return setIntegerLiteral(numberLiteralSuffix(SUFFIX_ALL));
                    }
                case '8' :
                case '9' :
                    parse_error("Illegal octal digit.");
                case '.' :
                case 'e' :
                case 'E' :
                	numberBuffer.append('0');
                    break;
                default :
                    pushback(c);
                    numberBuffer.append('0');
                    return setIntegerLiteral(numberLiteralSuffix(SUFFIX_ALL));
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
                    nondigit = 0;
                    numberBuffer.append((char) c);
                    break;
                case '.' :
                    if (nondigit != 0) {
                        pushback(c);
                        compile_error("Trailing '_' in number.");
                    } else if (seen_point || seen_e) {
                        pushback(c);
                        return getNumberLiteral(numberBuffer.toString(), seen_e, seen_point, nondigit);
                    } else {
                    	int c2;
                        if (!Character.isDigit(c2 = nextc())) {
                            pushback(c2);
                        	pushback('.');
                            if (c == '_') { 
                            		// Enebo:  c can never be antrhign but '.'
                            		// Why did I put this here?
                            } else {
                                return getNumberLiteral(numberBuffer.toString(), seen_e, seen_point, nondigit);
                            }
                        } else {
                            numberBuffer.append('.');
                            numberBuffer.append((char) c2);
                            seen_point = true;
                            nondigit = 0;
                        }
                    }
                    break;
                case 'e' :
                case 'E' :
                    if (nondigit != 0) {
                        compile_error("Trailing '_' in number.");
                    } else if (seen_e) {
                        pushback(c);
                        return getNumberLiteral(numberBuffer.toString(), seen_e, seen_point, nondigit);
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
                    if (nondigit != 0) {
                        compile_error_pos("Trailing '_' in number.");
                    }
                    nondigit = c;
                    break;
                default :
                    pushback(c);
                    return getNumberLiteral(numberBuffer.toString(), seen_e, seen_point, nondigit);
            }
        }
    }

    // MRI: This is decode_num: chunk
    private int getNumberLiteral(String number, boolean seen_e, boolean seen_point, int nondigit) {
        if (nondigit != 0) compile_error_pos("Trailing '_' in number.");

        boolean isFloat = seen_e || seen_point;
        if (isFloat) {
            int suffix = numberLiteralSuffix(seen_e ? SUFFIX_I : SUFFIX_ALL);
            return setNumberLiteral(getFloatToken(number, suffix), suffix);
        }

        return setIntegerLiteral(numberLiteralSuffix(SUFFIX_ALL));
    }

    private int setNumberLiteral(int type, int suffix) {
        if ((suffix & SUFFIX_I) != 0) type = tIMAGINARY;

        setState(EXPR_END);
        return type;
    }

    private int setIntegerLiteral(int suffix) {
        int type = (suffix & SUFFIX_R) != 0 ? tRATIONAL : tINTEGER;

        return setNumberLiteral(type, suffix);
    }
}
