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
import org.jruby.lexer.LexerSource;
import org.jruby.lexer.LexingCommon;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;
import org.jruby.util.SafeDoubleParser;
import org.jruby.util.StringSupport;
import org.jruby.util.cli.Options;

import static org.jruby.ext.ripper.RipperParser.tSP;

/**
 *
 * @author enebo
 */
public class RipperLexer extends LexingCommon {
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
            return considerComplex(RipperParser.tRATIONAL, suffix);
        }

        double d;
        try {
            d = SafeDoubleParser.parseDouble(number);
        } catch (NumberFormatException e) {
            warn("Float " + number + " out of range.");

            d = number.startsWith("-") ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }

        return considerComplex(RipperParser.tFLOAT, suffix);
    }

    private int considerComplex(int token, int suffix) {
        if ((suffix & SUFFIX_I) == 0) {
            return token;
        } else {
            return RipperParser.tIMAGINARY;
        }
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
        END ("end", RipperParser.keyword_end, RipperParser.keyword_end, EXPR_END),
        ELSE ("else", RipperParser.keyword_else, RipperParser.keyword_else, EXPR_BEG),
        CASE ("case", RipperParser.keyword_case, RipperParser.keyword_case, EXPR_BEG),
        ENSURE ("ensure", RipperParser.keyword_ensure, RipperParser.keyword_ensure, EXPR_BEG),
        MODULE ("module", RipperParser.keyword_module, RipperParser.keyword_module, EXPR_BEG),
        ELSIF ("elsif", RipperParser.keyword_elsif, RipperParser.keyword_elsif, EXPR_BEG),
        DEF ("def", RipperParser.keyword_def, RipperParser.keyword_def, EXPR_FNAME),
        RESCUE ("rescue", RipperParser.keyword_rescue, RipperParser.modifier_rescue, EXPR_MID),
        NOT ("not", RipperParser.keyword_not, RipperParser.keyword_not, EXPR_ARG),
        THEN ("then", RipperParser.keyword_then, RipperParser.keyword_then, EXPR_BEG),
        YIELD ("yield", RipperParser.keyword_yield, RipperParser.keyword_yield, EXPR_ARG),
        FOR ("for", RipperParser.keyword_for, RipperParser.keyword_for, EXPR_BEG),
        SELF ("self", RipperParser.keyword_self, RipperParser.keyword_self, EXPR_END),
        FALSE ("false", RipperParser.keyword_false, RipperParser.keyword_false, EXPR_END),
        RETRY ("retry", RipperParser.keyword_retry, RipperParser.keyword_retry, EXPR_END),
        RETURN ("return", RipperParser.keyword_return, RipperParser.keyword_return, EXPR_MID),
        TRUE ("true", RipperParser.keyword_true, RipperParser.keyword_true, EXPR_END),
        IF ("if", RipperParser.keyword_if, RipperParser.modifier_if, EXPR_BEG),
        DEFINED_P ("defined?", RipperParser.keyword_defined, RipperParser.keyword_defined, EXPR_ARG),
        SUPER ("super", RipperParser.keyword_super, RipperParser.keyword_super, EXPR_ARG),
        UNDEF ("undef", RipperParser.keyword_undef, RipperParser.keyword_undef, EXPR_FNAME|EXPR_FITEM),
        BREAK ("break", RipperParser.keyword_break, RipperParser.keyword_break, EXPR_MID),
        IN ("in", RipperParser.keyword_in, RipperParser.keyword_in, EXPR_BEG),
        DO ("do", RipperParser.keyword_do, RipperParser.keyword_do, EXPR_BEG),
        NIL ("nil", RipperParser.keyword_nil, RipperParser.keyword_nil, EXPR_END),
        UNTIL ("until", RipperParser.keyword_until, RipperParser.modifier_until, EXPR_BEG),
        UNLESS ("unless", RipperParser.keyword_unless, RipperParser.modifier_unless, EXPR_BEG),
        OR ("or", RipperParser.keyword_or, RipperParser.keyword_or, EXPR_BEG),
        NEXT ("next", RipperParser.keyword_next, RipperParser.keyword_next, EXPR_MID),
        WHEN ("when", RipperParser.keyword_when, RipperParser.keyword_when, EXPR_BEG),
        REDO ("redo", RipperParser.keyword_redo, RipperParser.keyword_redo, EXPR_END),
        AND ("and", RipperParser.keyword_and, RipperParser.keyword_and, EXPR_BEG),
        BEGIN ("begin", RipperParser.keyword_begin, RipperParser.keyword_begin, EXPR_BEG),
        __LINE__ ("__LINE__", RipperParser.keyword__LINE__, RipperParser.keyword__LINE__, EXPR_END),
        CLASS ("class", RipperParser.keyword_class, RipperParser.keyword_class, EXPR_CLASS),
        __FILE__("__FILE__", RipperParser.keyword__FILE__, RipperParser.keyword__FILE__, EXPR_END),
        LEND ("END", RipperParser.keyword_END, RipperParser.keyword_END, EXPR_END),
        LBEGIN ("BEGIN", RipperParser.keyword_BEGIN, RipperParser.keyword_BEGIN, EXPR_END),
        WHILE ("while", RipperParser.keyword_while, RipperParser.modifier_while, EXPR_BEG),
        ALIAS ("alias", RipperParser.keyword_alias, RipperParser.keyword_alias, EXPR_FNAME|EXPR_FITEM),
        __ENCODING__("__ENCODING__", RipperParser.keyword__ENCODING__, RipperParser.keyword__ENCODING__, EXPR_END);

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

    public RipperLexer(RipperParserBase parser, LexerSource src) {
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
            dispatchDelayedToken(RipperParser.tSTRING_CONTENT);
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
            dispatchDelayedToken(RipperParser.tSTRING_CONTENT);
        }
        lex_goto_eol();
        dispatchIgnoredScanEvent(RipperParser.tHEREDOC_END);
    }
    
    public void compile_error(String message) {
        parser.error();
        parser.dispatch("compile_error", getRuntime().newString(message));
//        throw new SyntaxException(lexb.toString(), message);
    }

    public int tokenize_ident(int result) {
        String value = createTokenString();

        if (!isLexState(last_state, EXPR_DOT|EXPR_FNAME) && parser.getCurrentScope().isDefined(value) >= 0) {
            setState(EXPR_END);
        }

        identValue = value.intern();
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
            warning("`%s' is ignored after any tokens", name);
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

        String value = "%" + (char) c;
        
        // Short-hand (e.g. %{,%.,%!,... versus %Q{).
        if (!Character.isLetterOrDigit(c)) {
            begin = c;
            c = 'Q';

        // Long-hand (e.g. %Q{}).
        } else {

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

        switch (c) {
        case 'Q':
            lex_strterm = new StringTerm(str_dquote, begin ,end);
            return RipperParser.tSTRING_BEG;

        case 'q':
            lex_strterm = new StringTerm(str_squote, begin, end);
            return RipperParser.tSTRING_BEG;

        case 'W':
            lex_strterm = new StringTerm(str_dword, begin, end);
            return RipperParser.tWORDS_BEG;

        case 'w':
            lex_strterm = new StringTerm(str_sword, begin, end);
            return RipperParser.tQWORDS_BEG;

        case 'x':
            lex_strterm = new StringTerm(str_xquote, begin, end);
            return RipperParser.tXSTRING_BEG;

        case 'r':
            lex_strterm = new StringTerm(str_regexp, begin, end);
            return RipperParser.tREGEXP_BEG;

        case 's':
            lex_strterm = new StringTerm(str_ssym, begin, end);
            setState(EXPR_FNAME|EXPR_FITEM);
            return RipperParser.tSYMBEG;

        case 'I':
            lex_strterm = new StringTerm(str_dword, begin, end);
            return RipperParser.tSYMBOLS_BEG;

        case 'i':
            lex_strterm = new StringTerm(str_sword, begin, end);
            return RipperParser.tQSYMBOLS_BEG;
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
        } else if (c == '~') {
            c = nextc();
            func = STR_FUNC_INDENT;
            heredoc_indent = Integer.MAX_VALUE;
            heredoc_line_indent = 0;
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
            markerValue.setEncoding(getEncoding());
            term = c;
            while ((c = nextc()) != EOF && c != term) {
                if (!tokenAddMBC(c, markerValue)) return EOF;
            }
            if (c == EOF) compile_error("unterminated here document identifier");
        } else {
            if (!isIdentifierChar(c)) {
                pushback(c);
                if ((func & STR_FUNC_INDENT) != 0) {
                    pushback(heredoc_indent > 0 ? '~' : '-');
                }
                return 0;
            }
            markerValue = new ByteList();
            markerValue.setEncoding(getEncoding());
            term = '"';
            func |= str_dquote;
            do {
                if (!tokenAddMBC(c, markerValue)) return EOF;
            } while ((c = nextc()) != EOF && isIdentifierChar(c));

            pushback(c);
        }

        dispatchScanEvent(RipperParser.tHEREDOC_BEG);
        int len = lex_p - lex_pbeg;        
        lex_goto_eol();
        lex_strterm = new HeredocTerm(markerValue, func, len, ruby_sourceline, lex_lastline);

        flush();
        return term == '`' ? RipperParser.tXSTRING_BEG : RipperParser.tSTRING_BEG;
    }
    
    private boolean arg_ambiguous() {
        parser.dispatch("on_arg_ambiguous");
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
            case RipperParser.yyErrorCode: System.err.print("yyErrorCode,"); break;
            // missing some RipperParser.
            case RipperParser.tIDENTIFIER: System.err.print("tIDENTIFIER["+ value() + "],"); break;
            case RipperParser.tFID: System.err.print("tFID[" + value() + "],"); break;
            case RipperParser.tGVAR: System.err.print("tGVAR[" + value() + "],"); break;
            case RipperParser.tIVAR: System.err.print("tIVAR[" + value() +"],"); break;
            case RipperParser.tCONSTANT: System.err.print("tCONSTANT["+ value() +"],"); break;
            case RipperParser.tCVAR: System.err.print("tCVAR,"); break;
            case RipperParser.tINTEGER: System.err.print("tINTEGER,"); break;
            case RipperParser.tFLOAT: System.err.print("tFLOAT,"); break;
            case RipperParser.tSTRING_CONTENT: System.err.print("tSTRING_CONTENT[" +  value() + "],"); break;
            case RipperParser.tSTRING_BEG: System.err.print("tSTRING_BEG,"); break;
            case RipperParser.tSTRING_END: System.err.print("tSTRING_END,"); break;
            case RipperParser.tSTRING_DBEG: System.err.print("tSTRING_DBEG,"); break;
            case RipperParser.tSTRING_DVAR: System.err.print("tSTRING_DVAR,"); break;
            case RipperParser.tXSTRING_BEG: System.err.print("tXSTRING_BEG,"); break;
            case RipperParser.tREGEXP_BEG: System.err.print("tREGEXP_BEG,"); break;
            case RipperParser.tREGEXP_END: System.err.print("tREGEXP_END,"); break;
            case RipperParser.tWORDS_BEG: System.err.print("tWORDS_BEG,"); break;
            case RipperParser.tQWORDS_BEG: System.err.print("tQWORDS_BEG,"); break;
            case RipperParser.tBACK_REF: System.err.print("tBACK_REF,"); break;
            case RipperParser.tBACK_REF2: System.err.print("tBACK_REF2,"); break;
            case RipperParser.tNTH_REF: System.err.print("tNTH_REF,"); break;
            case RipperParser.tUPLUS: System.err.print("tUPLUS"); break;
            case RipperParser.tUMINUS: System.err.print("tUMINUS,"); break;
            case RipperParser.tPOW: System.err.print("tPOW,"); break;
            case RipperParser.tCMP: System.err.print("tCMP,"); break;
            case RipperParser.tEQ: System.err.print("tEQ,"); break;
            case RipperParser.tEQQ: System.err.print("tEQQ,"); break;
            case RipperParser.tNEQ: System.err.print("tNEQ,"); break;
            case RipperParser.tGEQ: System.err.print("tGEQ,"); break;
            case RipperParser.tLEQ: System.err.print("tLEQ,"); break;
            case RipperParser.tANDOP: System.err.print("tANDOP,"); break;
            case RipperParser.tOROP: System.err.print("tOROP,"); break;
            case RipperParser.tMATCH: System.err.print("tMATCH,"); break;
            case RipperParser.tNMATCH: System.err.print("tNMATCH,"); break;
            case RipperParser.tDOT: System.err.print("tDOT,"); break;
            case RipperParser.tDOT2: System.err.print("tDOT2,"); break;
            case RipperParser.tDOT3: System.err.print("tDOT3,"); break;
            case RipperParser.tAREF: System.err.print("tAREF,"); break;
            case RipperParser.tASET: System.err.print("tASET,"); break;
            case RipperParser.tLSHFT: System.err.print("tLSHFT,"); break;
            case RipperParser.tRSHFT: System.err.print("tRSHFT,"); break;
            case RipperParser.tCOLON2: System.err.print("tCOLON2,"); break;
            case RipperParser.tCOLON3: System.err.print("tCOLON3,"); break;
            case RipperParser.tOP_ASGN: System.err.print("tOP_ASGN,"); break;
            case RipperParser.tASSOC: System.err.print("tASSOC,"); break;
            case RipperParser.tLPAREN: System.err.print("tLPAREN,"); break;
            case RipperParser.tLPAREN2: System.err.print("tLPAREN2,"); break;
            case RipperParser.tLPAREN_ARG: System.err.print("tLPAREN_ARG,"); break;
            case RipperParser.tLBRACK: System.err.print("tLBRACK,"); break;
            case RipperParser.tRBRACK: System.err.print("tRBRACK,"); break;
            case RipperParser.tLBRACE: System.err.print("tLBRACE,"); break;
            case RipperParser.tLBRACE_ARG: System.err.print("tLBRACE_ARG,"); break;
            case RipperParser.tSTAR: System.err.print("tSTAR,"); break;
            case RipperParser.tSTAR2: System.err.print("tSTAR2,"); break;
            case RipperParser.tAMPER: System.err.print("tAMPER,"); break;
            case RipperParser.tAMPER2: System.err.print("tAMPER2,"); break;
            case RipperParser.tSYMBEG: System.err.print("tSYMBEG,"); break;
            case RipperParser.tTILDE: System.err.print("tTILDE,"); break;
            case RipperParser.tPERCENT: System.err.print("tPERCENT,"); break;
            case RipperParser.tDIVIDE: System.err.print("tDIVIDE,"); break;
            case RipperParser.tPLUS: System.err.print("tPLUS,"); break;
            case RipperParser.tMINUS: System.err.print("tMINUS,"); break;
            case RipperParser.tLT: System.err.print("tLT,"); break;
            case RipperParser.tGT: System.err.print("tGT,"); break;
            case RipperParser.tCARET: System.err.print("tCARET,"); break;
            case RipperParser.tBANG: System.err.print("tBANG,"); break;
            case RipperParser.tLCURLY: System.err.print("tTLCURLY,"); break;
            case RipperParser.tRCURLY: System.err.print("tRCURLY,"); break;
            case RipperParser.tPIPE: System.err.print("tTPIPE,"); break;
            case RipperParser.tLAMBDA: System.err.print("tLAMBDA,"); break;
            case RipperParser.tLAMBEG: System.err.print("tLAMBEG,"); break;
            case RipperParser.tRPAREN: System.err.print("tRPAREN,"); break;
            case RipperParser.tLABEL: System.err.print("tLABEL("+ value() +":),"); break;
            case RipperParser.tLABEL_END: System.err.print("tLABEL_END"); break;
            case '\n': System.err.println("NL"); break;
            case EOF: System.out.println("EOF"); break;
            case RipperParser.tDSTAR: System.err.print("tDSTAR"); break;
            case RipperParser.tSTRING_DEND: System.err.print("tDSTRING_DEND,"); break;
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
        //System.out.println("EVENT: " + event + ", VALUE: " + value);
        IRubyObject returnValue = parser.dispatch(event, value);
        flush();
        return returnValue;
    }
    
    private String tokenToEventId(int token) {
        switch(token) {
            case ' ': return "on_words_sep";
            case RipperParser.tBANG: return "on_op";
            case RipperParser.tPERCENT: return "on_op";
            case RipperParser.tANDDOT: return "on_op";
            case RipperParser.tAMPER2: return "on_op";
            case RipperParser.tSTAR2: return "on_op";
            case RipperParser.tPLUS: return "on_op";
            case RipperParser.tMINUS: return "on_op";
            case RipperParser.tDIVIDE: return "on_op";
            case RipperParser.tLT: return "on_op";
            case '=': return "on_op";
            case RipperParser.tGT: return "on_op";
            case '?': return "on_op";
            case RipperParser.tCARET: return "on_op";
            case RipperParser.tPIPE: return "on_op";
            case RipperParser.tTILDE: return "on_op";
            case ':': return "on_op";
            case ',': return "on_comma";
            case '.': return "on_period";
            case RipperParser.tDOT: return "on_period";
            case ';': return "on_semicolon";
            case RipperParser.tBACK_REF2: return "on_backtick";
            case '\n': return "on_nl";
            case RipperParser.keyword_alias: return "on_kw";
            case RipperParser.keyword_and: return "on_kw";
            case RipperParser.keyword_begin: return "on_kw";
            case RipperParser.keyword_break: return "on_kw";
            case RipperParser.keyword_case: return "on_kw";
            case RipperParser.keyword_class: return "on_kw";
            case RipperParser.keyword_def: return "on_kw";
            case RipperParser.keyword_defined: return "on_kw";
            case RipperParser.keyword_do: return "on_kw";
            case RipperParser.keyword_do_block: return "on_kw";
            case RipperParser.keyword_do_cond: return "on_kw";
            case RipperParser.keyword_else: return "on_kw";
            case RipperParser.keyword_elsif: return "on_kw";
            case RipperParser.keyword_end: return "on_kw";
            case RipperParser.keyword_ensure: return "on_kw";
            case RipperParser.keyword_false: return "on_kw";
            case RipperParser.keyword_for: return "on_kw";
            case RipperParser.keyword_if: return "on_kw";
            case RipperParser.modifier_if: return "on_kw";
            case RipperParser.keyword_in: return "on_kw";
            case RipperParser.keyword_module: return "on_kw";
            case RipperParser.keyword_next: return "on_kw";
            case RipperParser.keyword_nil: return "on_kw";
            case RipperParser.keyword_not: return "on_kw";
            case RipperParser.keyword_or: return "on_kw";
            case RipperParser.keyword_redo: return "on_kw";
            case RipperParser.keyword_rescue: return "on_kw";
            case RipperParser.modifier_rescue: return "on_kw";
            case RipperParser.keyword_retry: return "on_kw";
            case RipperParser.keyword_return: return "on_kw";
            case RipperParser.keyword_self: return "on_kw";
            case RipperParser.keyword_super: return "on_kw";
            case RipperParser.keyword_then: return "on_kw";
            case RipperParser.keyword_true: return "on_kw";
            case RipperParser.keyword_undef: return "on_kw";
            case RipperParser.keyword_unless: return "on_kw";
            case RipperParser.modifier_unless: return "on_kw";
            case RipperParser.keyword_until: return "on_kw";
            case RipperParser.modifier_until: return "on_kw";
            case RipperParser.keyword_when: return "on_kw";
            case RipperParser.keyword_while: return "on_kw";
            case RipperParser.modifier_while: return "on_kw";
            case RipperParser.keyword_yield: return "on_kw";
            case RipperParser.keyword__FILE__: return "on_kw";
            case RipperParser.keyword__LINE__: return "on_kw";
            case RipperParser.keyword__ENCODING__: return "on_kw";
            case RipperParser.keyword_BEGIN: return "on_kw";
            case RipperParser.keyword_END: return "on_kw";
            case RipperParser.keyword_do_lambda: return "on_kw";
            case RipperParser.tAMPER: return "on_op";
            case RipperParser.tANDOP: return "on_op";
            case RipperParser.tAREF: return "on_op";
            case RipperParser.tASET: return "on_op";
            case RipperParser.tASSOC: return "on_op";
            case RipperParser.tBACK_REF: return "on_backref";
            case RipperParser.tCHAR: return "on_CHAR";
            case RipperParser.tCMP: return "on_op";
            case RipperParser.tCOLON2: return "on_op";
            case RipperParser.tCOLON3: return "on_op";
            case RipperParser.tCONSTANT: return "on_const";
            case RipperParser.tCVAR: return "on_cvar";
            case RipperParser.tDOT2: return "on_op";
            case RipperParser.tDOT3: return "on_op";
            case RipperParser.tEQ: return "on_op";
            case RipperParser.tEQQ: return "on_op";
            case RipperParser.tFID: return "on_ident";
            case RipperParser.tFLOAT: return "on_float";
            case RipperParser.tGEQ: return "on_op";
            case RipperParser.tGVAR: return "on_gvar";
            case RipperParser.tIDENTIFIER: return "on_ident";
            case RipperParser.tIMAGINARY: return "on_imaginary";
            case RipperParser.tINTEGER: return "on_int";
            case RipperParser.tIVAR: return "on_ivar";
            case RipperParser.tLBRACE: return "on_lbrace";
            case RipperParser.tLBRACE_ARG: return "on_lbrace";
            case RipperParser.tLCURLY: return "on_lbrace";
            case RipperParser.tRCURLY: return "on_rbrace";
            case RipperParser.tLBRACK: return "on_lbracket";
            case '[': return "on_lbracket";
            case RipperParser.tRBRACK: return "on_rbracket";
            case RipperParser.tLEQ: return "on_op";
            case RipperParser.tLPAREN: return "on_lparen";
            case RipperParser.tLPAREN_ARG: return "on_lparen";
            case RipperParser.tLPAREN2: return "on_lparen";
            case ')': return "on_rparen";  // ENEBO: Don't this this can happen.
            case RipperParser.tLSHFT: return "on_op";
            case RipperParser.tMATCH: return "on_op";
            case RipperParser.tNEQ: return "on_op";
            case RipperParser.tNMATCH: return "on_op";
            case RipperParser.tNTH_REF: return "on_backref";
            case RipperParser.tOP_ASGN: return "on_op";
            case RipperParser.tOROP: return "on_op";
            case RipperParser.tPOW: return "on_op";
            case RipperParser.tQSYMBOLS_BEG: return "on_qsymbols_beg";
            case RipperParser.tRATIONAL: return "on_rational";
            case RipperParser.tSYMBOLS_BEG: return "on_symbols_beg";
            case RipperParser.tQWORDS_BEG: return "on_qwords_beg";
            case RipperParser.tREGEXP_BEG:return "on_regexp_beg";
            case RipperParser.tREGEXP_END: return "on_regexp_end";
            case RipperParser.tRPAREN: return "on_rparen";
            case RipperParser.tRSHFT: return "on_op";
            case RipperParser.tSTAR: return "on_op";
            case RipperParser.tDSTAR: return "on_op";
            case RipperParser.tSTRING_BEG: return "on_tstring_beg";
            case RipperParser.tSTRING_CONTENT: return "on_tstring_content";
            case RipperParser.tSTRING_DBEG: return "on_embexpr_beg";
            case RipperParser.tSTRING_DEND: return "on_embexpr_end";
            case RipperParser.tSTRING_DVAR: return "on_embvar";
            case RipperParser.tSTRING_END: return "on_tstring_end";
            case RipperParser.tSYMBEG: return "on_symbeg";
            case RipperParser.tUMINUS: return "on_op";
            case RipperParser.tUMINUS_NUM: return "on_op";
            case RipperParser.tUPLUS: return "on_op";
            case RipperParser.tWORDS_BEG: return "on_words_beg";
            case RipperParser.tXSTRING_BEG: return "on_backtick";
            case RipperParser.tLABEL: return "on_label";
            case RipperParser.tLABEL_END: return "on_label_end";
            case RipperParser.tLAMBDA: return "on_tlambda";
            case RipperParser.tLAMBEG: return "on_tlambeg";

            // ripper specific tokens
            case RipperParser.tIGNORED_NL: return "on_ignored_nl";
            case RipperParser.tCOMMENT: return "on_comment";
            case RipperParser.tEMBDOC_BEG: return "on_embdoc_beg";
            case RipperParser.tEMBDOC: return "on_embdoc";
            case RipperParser.tEMBDOC_END: return "on_embdoc_end";
            case tSP: return "on_sp";
            case RipperParser.tHEREDOC_BEG: return "on_heredoc_beg";
            case RipperParser.tHEREDOC_END: return "on_heredoc_end";
            case RipperParser.k__END__: return "on___end__";
            default: // Weird catchall but we will try and not use < 256 value trick like MRI
                return "on_CHAR";
        }
    }
    
    // DEBUGGING HELP 
    private int yylex2() throws IOException {
        try {
        int currentToken = yylex2();

            printToken(currentToken);
        
        return currentToken;
        } catch (Exception e) {
            System.out.println("FFUFUFUFUFUFUFUF: " + e);
            return EOF;
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
                lex_p = lex_pend;
                dispatchScanEvent(RipperParser.tCOMMENT);

                fallthru = true;
            }
            /* fall through */
            case '\n': {
                this.tokenSeen = tokenSeen;
                boolean normalArg = isLexState(lex_state, EXPR_BEG | EXPR_CLASS | EXPR_FNAME | EXPR_DOT) &&
                        !isLexState(lex_state, EXPR_LABELED);
                if (normalArg || isLexStateAll(lex_state, EXPR_ARG | EXPR_LABELED)) {
                    if (!fallthru) dispatchScanEvent(RipperParser.tIGNORED_NL);
                    fallthru = false;
                    if (!normalArg && inKwarg) {
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
                        case '&':
                        case '.': {
                            dispatchDelayedToken(RipperParser.tIGNORED_NL);
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
                        
                        dispatchScanEvent(RipperParser.tEMBDOC_BEG);
                        for (;;) {
                            lex_goto_eol();
                            
                            if (!first_p) dispatchScanEvent(RipperParser.tEMBDOC);
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
                        dispatchScanEvent(RipperParser.tEMBDOC_END);
                        
                        continue loop;
                    }
                }


                setState(isAfterOperator() ? EXPR_ARG : EXPR_BEG);

                c = nextc();
                if (c == '=') {
                    c = nextc();
                    if (c == '=') {
                        return RipperParser.tEQQ;
                    }

                    pushback(c);
                    return RipperParser.tEQ;
                }
                if (c == '~') {
                    return RipperParser.tMATCH;
                } else if (c == '>') {
                    return RipperParser.tASSOC;
                }
                pushback(c);
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
                    dispatchScanEvent(RipperParser.k__END__);
                    return EOF;
                }
                return identifier(c, commandState);
            default:
                return identifier(c, commandState);
            }
        }
    }

    private int identifierToken(int last_state, int result, String value) {
        if (result == RipperParser.tIDENTIFIER && !isLexState(last_state, EXPR_DOT|EXPR_FNAME) &&
                parser.getCurrentScope().isDefined(value) >= 0) {
            setState(EXPR_END|EXPR_LABEL);
        }

        identValue = value.intern();
        return result;
    }
    
    private int ampersand(boolean spaceSeen) throws IOException {
        int c = nextc();
        
        switch (c) {
        case '&':
            setState(EXPR_BEG);
            if ((c = nextc()) == '=') {
                setState(EXPR_BEG);
                return RipperParser.tOP_ASGN;
            }
            pushback(c);
            return RipperParser.tANDOP;
        case '=':
            setState(EXPR_BEG);
            return RipperParser.tOP_ASGN;
        case '.':
            setState(EXPR_DOT);
            return RipperParser.tANDDOT;
        }
        pushback(c);

        if (isSpaceArg(c, spaceSeen)) {
            if (isVerbose()) warning("`&' interpreted as argument prefix");
            c = RipperParser.tAMPER;
        } else if (isBEG()) {
            c = RipperParser.tAMPER;
        } else {
            c = RipperParser.tAMPER2;
        }

        setState(isAfterOperator() ? EXPR_ARG : EXPR_BEG);
        
        return c;
    }

    private int at() throws IOException {
        newtok(true);
        int c = nextc();
        int result;
        if (c == '@') {
            c = nextc();
            result = RipperParser.tCVAR;
        } else {
            result = RipperParser.tIVAR;
        }

        if (c == EOF || isSpace(c)) {
            if (result == RipperParser.tIVAR) {
                compile_error("`@' without identifiers is not allowed as an instance variable name");
            }

            compile_error("`@@' without identifiers is not allowed as a class variable name");
        } else if (Character.isDigit(c) || !isIdentifierChar(c)) {
            pushback(c);
            if (result == RipperParser.tIVAR) {
                compile_error("`@" + ((char) c) + "' is not allowed as an instance variable name");
            }
            compile_error("`@@" + ((char) c) + "' is not allowed as a class variable name");
        }

        if (!tokadd_ident(c)) return EOF;

        last_state = lex_state;
        setState(EXPR_END);

        return tokenize_ident(result);
    }
    
    private int backtick(boolean commandState) throws IOException {
        if (isLexState(lex_state, EXPR_FNAME)) {
            setState(EXPR_ENDFN);
            return RipperParser.tBACK_REF2;
        }
        if (isLexState(lex_state, EXPR_DOT)) {
            setState(commandState ? EXPR_CMDARG : EXPR_ARG);

            return RipperParser.tBACK_REF2;
        }

        lex_strterm = new StringTerm(str_xquote, '\0', '`');
        return RipperParser.tXSTRING_BEG;
    }
    
    private int bang() throws IOException {
        int c = nextc();

        if (isAfterOperator()) {
            setState(EXPR_ARG);
            if (c == '@') return RipperParser.tBANG;
        } else {
            setState(EXPR_BEG);
        }
        
        switch (c) {
        case '=':
            return RipperParser.tNEQ;
        case '~':
            return RipperParser.tNMATCH;
        default: // Just a plain bang
            pushback(c);
            
            return RipperParser.tBANG;
        }
    }
    
    private int caret() throws IOException {
        int c = nextc();
        if (c == '=') {
            setState(EXPR_BEG);
            
            return RipperParser.tOP_ASGN;
        }

        setState(isAfterOperator() ? EXPR_ARG : EXPR_BEG);
        
        pushback(c);
        
        return RipperParser.tCARET;
    }

    private int colon(boolean spaceSeen) throws IOException {
        int c = nextc();
        
        if (c == ':') {
            if (isBEG() || isLexState(lex_state, EXPR_CLASS) || (isARG() && spaceSeen)) {
                setState(EXPR_BEG);
                return RipperParser.tCOLON3;
            }
            setState(EXPR_DOT);
            return RipperParser.tCOLON2;
        }

        if (isEND() || Character.isWhitespace(c) || c == '#') {
            pushback(c);
            setState(EXPR_BEG);
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

        setState(EXPR_FNAME);
        return RipperParser.tSYMBEG;
    }

    private int comma(int c) throws IOException {
        setState(EXPR_BEG|EXPR_LABEL);
        return c;
    }

    private int doKeyword(int state) {
        int leftParenBegin = getLeftParenBegin();
        if (leftParenBegin > 0 && leftParenBegin == parenNest) {
            setLeftParenBegin(0);
            parenNest--;
            return RipperParser.keyword_do_lambda;
        }

        if (conditionState.set_p()) return RipperParser.keyword_do_cond;

        if (cmdArgumentState.set_p() && !isLexState(state, EXPR_CMDARG)) {
            return RipperParser.keyword_do_block;
        }
        if (isLexState(state,  EXPR_BEG|EXPR_ENDARG)) {
            return RipperParser.keyword_do_block;
        }
        return RipperParser.keyword_do;
    }
    
    private int dollar() throws IOException {
        setState(EXPR_END);
        newtok(true);
        int c = nextc();
        
        switch (c) {
        case '_':       /* $_: last read line string */
            c = nextc();
            if (isIdentifierChar(c)) {
                if (!tokadd_ident(c)) return EOF;

                last_state = lex_state;
                setState(EXPR_END);
                identValue = createTokenString().intern();
                return RipperParser.tGVAR;
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
            return RipperParser.tGVAR;

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
            return RipperParser.tGVAR;

        case '&':       /* $&: last match */
        case '`':       /* $`: string before last match */
        case '\'':      /* $': string after last match */
        case '+':       /* $+: string matches last paren. */
            // Explicit reference to these vars as symbols...
            if (last_state == EXPR_FNAME) {
                identValue = "$" + (char) c;
                return RipperParser.tGVAR;
            }

            identValue = "$" + (char) c;
            return RipperParser.tBACK_REF;

        case '1': case '2': case '3': case '4': case '5': case '6':
        case '7': case '8': case '9':
            do {
                c = nextc();
            } while (Character.isDigit(c));
            pushback(c);
            if (last_state == EXPR_FNAME) {
                identValue = createTokenString().intern();
                return RipperParser.tGVAR;
            }

            String refAsString = createTokenString();

            try {
                Integer.parseInt(refAsString.substring(1).intern());
            } catch (NumberFormatException e) {
                warn("`" + refAsString + "' is too big for a number variable, always nil");
            }

            identValue = createTokenString().intern();
            return RipperParser.tNTH_REF;
        case '0':
            setState(EXPR_END);

            return identifierToken(last_state, RipperParser.tGVAR, ("$" + (char) c).intern());
        default:
            if (!isIdentifierChar(c)) {
                if (c == EOF || isSpace(c)) {
                    compile_error("`$' without identifiers is not allowed as a global variable name");
                } else {
                    pushback(c);
                    compile_error("`$" + ((char) c) + "' is not allowed as a global variable name");
                }
                return EOF;
            }

            last_state = lex_state;
            setState(EXPR_END);

            tokadd_ident(c);

            return identifierToken(last_state, RipperParser.tGVAR, createTokenString().intern()); // $blah
        }
    }
    
    private int dot() throws IOException {
        int c;
        
        setState(EXPR_BEG);
        if ((c = nextc()) == '.') {
            if ((c = nextc()) == '.') return RipperParser.tDOT3;
            
            pushback(c);
            
            return RipperParser.tDOT2;
        }
        
        pushback(c);
        if (Character.isDigit(c)) compile_error("no .<digit> floating literal anymore; put 0 before dot");
        
        setState(EXPR_DOT);
        
        return RipperParser.tDOT;
    }
    
    private int doubleQuote(boolean commandState) throws IOException {
        int label = isLabelPossible(commandState) ? str_label : 0;
        lex_strterm = new StringTerm(str_dquote|label, '\0', '"');

        return RipperParser.tSTRING_BEG;
    }
    
    private int greaterThan() throws IOException {
        setState(isAfterOperator() ? EXPR_ARG : EXPR_BEG);

        int c = nextc();

        switch (c) {
        case '=':
            return RipperParser.tGEQ;
        case '>':
            if ((c = nextc()) == '=') {
                setState(EXPR_BEG);
                return RipperParser.tOP_ASGN;
            }
            pushback(c);
            
            return RipperParser.tRSHFT;
        default:
            pushback(c);
            
            return RipperParser.tGT;
        }
    }

    private int identifier(int c, boolean commandState) throws IOException {
        if (!isIdentifierChar(c)) {
            String badChar = "\\" + Integer.toOctalString(c & 0xff);
            compile_error("Invalid char `" + badChar + "' ('" + (char) c + "') in expression");
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
            result = RipperParser.tFID;
            tempVal = createTokenString();
        } else {
            if (isLexState(lex_state, EXPR_FNAME)) {
                if ((c = nextc()) == '=') { 
                    int c2 = nextc();

                    if (c2 != '~' && c2 != '>' &&
                            (c2 != '=' || peek('>'))) {
                        result = RipperParser.tIDENTIFIER;
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
                result = RipperParser.tCONSTANT;
            } else {
                result = RipperParser.tIDENTIFIER;
            }
        }

        if (isLabelPossible(commandState)) {
            if (isLabelSuffix()) {
                setState(EXPR_ARG|EXPR_LABELED);
                nextc();
                identValue = tempVal.intern();
                return RipperParser.tLABEL;
            }
        }

        if (lex_state != EXPR_DOT) {
            Keyword keyword = getKeyword(createTokenString(whereKeywordShouldStart)); // Is it is a keyword?

            if (keyword != null) {
                int state = lex_state; // Save state at time keyword is encountered
                setState(keyword.state);

                if (isLexState(state, EXPR_FNAME)) {
                    identValue = tempVal;
                    return keyword.id0;
                }

                if (isLexState(lex_state, EXPR_BEG)) commandStart = true;

                if (keyword.id0 == RipperParser.keyword_do) return doKeyword(state);

                if (isLexState(state, EXPR_BEG|EXPR_LABELED)) {
                    return keyword.id0;
                } else {
                    if (keyword.id0 != keyword.id1) setState(EXPR_BEG|EXPR_LABEL);
                    return keyword.id1;
                }
            }
        }

        if (isLexState(lex_state, EXPR_BEG_ANY|EXPR_ARG_ANY|EXPR_DOT)) {
            setState(commandState ? EXPR_CMDARG : EXPR_ARG);
        } else if (lex_state == EXPR_FNAME) {
            setState(EXPR_ENDFN);
        } else {
            setState(EXPR_END);
        }
        
        return identifierToken(last_state, result, tempVal.intern());
    }

    private int leftBracket(boolean spaceSeen) throws IOException {
        parenNest++;
        int c = '[';
        if (isAfterOperator()) {
            if ((c = nextc()) == ']') {
                setState(EXPR_ARG);
                if (peek('=')) {
                    nextc();
                    return RipperParser.tASET;
                }
                return RipperParser.tAREF;
            }
            pushback(c);
            setState(EXPR_ARG|EXPR_LABEL);
            return '[';
        } else if (isBEG() || (isARG() && (spaceSeen || isLexState(lex_state, EXPR_LABELED)))) {
            c = RipperParser.tLBRACK;
        }

        setState(EXPR_BEG|EXPR_LABEL);
        conditionState.push0();
        cmdArgumentState.push0();
        yaccValue = "[";
        return c;
    }

    private int leftCurly() {
        braceNest++;
        int leftParenBegin = getLeftParenBegin();
        if (leftParenBegin > 0 && leftParenBegin == parenNest) {
            setState(EXPR_BEG);
            setLeftParenBegin(0);
            parenNest--;
            conditionState.push0();
            cmdArgumentState.push0();
            return RipperParser.tLAMBEG;
        }

        char c;
        if (isLexState(lex_state, EXPR_LABELED)) {
            c = RipperParser.tLBRACE;
        } else if (isLexState(lex_state, EXPR_ARG_ANY|EXPR_END|EXPR_ENDFN)) { // block (primary)
            c = RipperParser.tLCURLY;
        } else if (isLexState(lex_state, EXPR_ENDARG)) { // block (expr)
            c = RipperParser.tLBRACE_ARG;
        } else { // hash
            c = RipperParser.tLBRACE;
        }

        conditionState.push0();
        cmdArgumentState.push0();
        setState(EXPR_BEG);
        setState(c == RipperParser.tLBRACE_ARG ? EXPR_BEG : EXPR_BEG|EXPR_LABEL);
        if (c != RipperParser.tLBRACE) commandStart = true;

        return c;
    }

    private int leftParen(boolean spaceSeen) throws IOException {
        int result;

        if (isBEG()) {
            result = RipperParser.tLPAREN;
        } else if (isSpaceArg('(', spaceSeen)) {
            result = RipperParser.tLPAREN_ARG;
        } else {
            result = RipperParser.tLPAREN2;
        }

        parenNest++;
        conditionState.push0();
        cmdArgumentState.push0();
        setState(EXPR_BEG|EXPR_LABEL);

        return result;
    }
    
    private int lessThan(boolean spaceSeen) throws IOException {
        last_state = lex_state;
        int c = nextc();
        if (c == '<' && !isLexState(lex_state, EXPR_DOT|EXPR_CLASS) &&
                !isEND() && (!isARG() || isLexState(lex_state, EXPR_LABELED) || spaceSeen)) {
            int tok = hereDocumentIdentifier();
            
            if (tok != 0) return tok;
        }

        setState(isAfterOperator() ? EXPR_ARG : EXPR_BEG);
        
        switch (c) {
        case '=':
            if ((c = nextc()) == '>') {
                return RipperParser.tCMP;
            }
            pushback(c);
            return RipperParser.tLEQ;
        case '<':
            if ((c = nextc()) == '=') {
                setState(EXPR_BEG);
                return RipperParser.tOP_ASGN;
            }
            pushback(c);
            warn_balanced(c, spaceSeen, "<<", "here document");
            return RipperParser.tLSHFT;
        default:
            pushback(c);
            return RipperParser.tLT;
        }
    }
    
    private int minus(boolean spaceSeen) throws IOException {
        int c = nextc();
        
        if (isAfterOperator()) {
            setState(EXPR_ARG);
            if (c == '@') {
                return RipperParser.tUMINUS;
            }
            pushback(c);
            return RipperParser.tMINUS;
        }
        if (c == '=') {
            setState(EXPR_BEG);

            return RipperParser.tOP_ASGN;
        }
        if (c == '>') {
            setState(EXPR_ENDFN);
            return RipperParser.tLAMBDA;
        }
        if (isBEG() || (isSpaceArg(c, spaceSeen) && arg_ambiguous())) {
            setState(EXPR_BEG);
            pushback(c);
            if (Character.isDigit(c)) {
                return RipperParser.tUMINUS_NUM;
            }
            return RipperParser.tUMINUS;
        }
        setState(EXPR_BEG);
        pushback(c);
        warn_balanced(c, spaceSeen, "-", "unary operator");
        return RipperParser.tMINUS;
    }

    private int percent(boolean spaceSeen) throws IOException {
        if (isBEG()) return parseQuote(nextc());

        int c = nextc();

        if (c == '=') {
            setState(EXPR_BEG);
            return RipperParser.tOP_ASGN;
        }

        if (isSpaceArg(c, spaceSeen) || (isLexState(lex_state, EXPR_FITEM) && c == 's')) return parseQuote(c);

        setState(isAfterOperator() ? EXPR_ARG : EXPR_BEG);

        pushback(c);
        warn_balanced(c, spaceSeen, "%", "string literal");
        return RipperParser.tPERCENT;
    }

    private int pipe() throws IOException {
        int c = nextc();
        
        switch (c) {
        case '|':
            setState(EXPR_BEG);
            if ((c = nextc()) == '=') {
                setState(EXPR_BEG);
                return RipperParser.tOP_ASGN;
            }
            pushback(c);
            return RipperParser.tOROP;
        case '=':
            setState(EXPR_BEG);
            
            return RipperParser.tOP_ASGN;
            default:
                setState(isAfterOperator() ? EXPR_ARG : EXPR_BEG|EXPR_LABEL);

            pushback(c);
            
            return RipperParser.tPIPE;
        }
    }
    
    private int plus(boolean spaceSeen) throws IOException {
        int c = nextc();
        if (isAfterOperator()) {
            setState(EXPR_ARG);
            if (c == '@') return RipperParser.tUPLUS;

            pushback(c);

            return RipperParser.tPLUS;
        }
        
        if (c == '=') {
            setState(EXPR_BEG);

            return RipperParser.tOP_ASGN;
        }

        if (isBEG() || (isSpaceArg(c, spaceSeen) && arg_ambiguous())) {
            setState(EXPR_BEG);
            pushback(c);
            if (Character.isDigit(c)) {
                c = '+';
                return parseNumber(c);
            }

            return RipperParser.tUPLUS;
        }

        setState(EXPR_BEG);
        pushback(c);
        warn_balanced(c, spaceSeen, "+", "unary operator");
        return RipperParser.tPLUS;
    }

    // FIXME: This is a bit different than regular parser but the problem
    // I ran into was not returning the '?' with the char it is finding.
    // This in part must be some difference between MRI and our lexer impls
    // doing things a little differently.
    private int questionMark() throws IOException {
        int c;
        
        if (isEND()) {
            setState(EXPR_VALUE);

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
            setState(EXPR_VALUE);
            return '?';
        } else if (isASCII()) {
            ByteList buffer = new ByteList(1);
            if (!tokenAddMBC(c, buffer)) return EOF;

            setState(EXPR_END);
            return RipperParser.tCHAR;
        } else if (isIdentifierChar(c) && !peek('\n') && isNext_identchar()) {
            pushback(c);
            setState(EXPR_VALUE);

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
                
                setState(EXPR_END);

                return RipperParser.tINTEGER; // FIXME: This should be something else like a tCHAR in 1.9/2.0
            } else {
                c = readEscape();
            }
        }

        setState(EXPR_END);
        // TODO: this isn't handling multibyte yet
        ByteList oneCharBL = new ByteList(1);
        oneCharBL.append(c);
        return RipperParser.tCHAR;
    }

    private int rightBracket() {
        parenNest--;
        conditionState.pop();
        cmdArgumentState.pop();
        setState(EXPR_END);
        return RipperParser.tRBRACK;
    }

    private int rightCurly() {
        if (braceNest <= 0) {
            return RipperParser.tSTRING_DEND;
        }
        braceNest--;

        conditionState.pop();
        cmdArgumentState.pop();
        setState(EXPR_END);
        return RipperParser.tRCURLY;
    }

    private int rightParen() {
        parenNest--;
        conditionState.pop();
        cmdArgumentState.pop();
        setState(EXPR_ENDFN);
        return RipperParser.tRPAREN;
    }
    
    private int singleQuote(boolean commandState) throws IOException {
        int label = isLabelPossible(commandState) ? str_label : 0;
        lex_strterm = new StringTerm(str_squote|label, '\0', '\'');
        return RipperParser.tSTRING_BEG;
    }
    
    private int slash(boolean spaceSeen) throws IOException {
        if (isBEG()) {
            lex_strterm = new StringTerm(str_regexp, '\0', '/');
            
            return RipperParser.tREGEXP_BEG;
        }
        
        int c = nextc();
        
        if (c == '=') {
            setState(EXPR_BEG);
            
            return RipperParser.tOP_ASGN;
        }
        pushback(c);
        if (isSpaceArg(c, spaceSeen)) {
            arg_ambiguous();
            lex_strterm = new StringTerm(str_regexp, '\0', '/');
            
            return RipperParser.tREGEXP_BEG;
        }

        setState(isAfterOperator() ? EXPR_ARG : EXPR_BEG);


        warn_balanced(c, spaceSeen, "/", "regexp literal");
        return RipperParser.tDIVIDE;
    }

    private int star(boolean spaceSeen) throws IOException {
        int c = nextc();
        
        switch (c) {
        case '*':
            if ((c = nextc()) == '=') {
                setState(EXPR_BEG);

                return RipperParser.tOP_ASGN;
            }

            pushback(c);


            if (isSpaceArg(c, spaceSeen)) {
                if (isVerbose() && Options.PARSER_WARN_ARGUMENT_PREFIX.load()) warning("`**' interpreted as argument prefix");
                c = RipperParser.tDSTAR;
            } else if (isBEG()) {
                c = RipperParser.tDSTAR;
            } else {
                warn_balanced(c, spaceSeen, "**", "argument prefix");
                c = RipperParser.tPOW;
            }
            break;
        case '=':
            setState(EXPR_BEG);

            return RipperParser.tOP_ASGN;
        default:
            pushback(c);
            if (isSpaceArg(c, spaceSeen)) {
                if (isVerbose() && Options.PARSER_WARN_ARGUMENT_PREFIX.load()) warning("`*' interpreted as argument prefix");
                c = RipperParser.tSTAR;
            } else if (isBEG()) {
                c = RipperParser.tSTAR;
            } else {
                warn_balanced(c, spaceSeen, "*", "argument prefix");
                c = RipperParser.tSTAR2;
            }

        }

        setState(isAfterOperator() ? EXPR_ARG : EXPR_BEG);
        return c;
    }

    private int tilde() throws IOException {
        int c;
        
        if (isAfterOperator()) {
            if ((c = nextc()) != '@') pushback(c);
            setState(EXPR_ARG);
        } else {
            setState(EXPR_BEG);
        }
        
        return RipperParser.tTILDE;
    }

    private ByteList numberBuffer = new ByteList(10); // ascii is good enough.
    /**
     *  Parse a number from the input stream.
     *
     *@param c The first character of the number.
     *@return A int constant which represents a token.
     */
    private int parseNumber(int c) throws IOException {
        setState(EXPR_END);

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
                        compile_error("Hexadecimal number without hex-digits.");
                    } else if (nondigit != '\0') {
                        compile_error("Trailing '_' in number.");
                    }
                    return setIntegerLiteral(numberLiteralSuffix(SUFFIX_ALL));
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
                        compile_error("Binary number without digits.");
                    } else if (nondigit != '\0') {
                        compile_error("Trailing '_' in number.");
                    }
                    return setIntegerLiteral(numberLiteralSuffix(SUFFIX_ALL));
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
                        compile_error("Binary number without digits.");
                    } else if (nondigit != '\0') {
                        compile_error("Trailing '_' in number.");
                    }
                    return setIntegerLiteral(numberLiteralSuffix(SUFFIX_ALL));
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

                        if (nondigit != '\0') compile_error("Trailing '_' in number.");

                        return setIntegerLiteral(numberLiteralSuffix(SUFFIX_ALL));
                    }
                case '8' :
                case '9' :
                    compile_error("Illegal octal digit.");
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
                    nondigit = '\0';
                    numberBuffer.append((char) c);
                    break;
                case '.' :
                    if (nondigit != '\0') {
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
                    if (nondigit != '\0') {
                        compile_error("Trailing '_' in number.");
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
    private int getNumberLiteral(String number, boolean seen_e, boolean seen_point, int nondigit) throws IOException {
        if (nondigit != '\0') compile_error("Trailing '_' in number.");

        boolean isFloat = seen_e || seen_point;
        if (isFloat) {
            int suffix = numberLiteralSuffix(seen_e ? SUFFIX_I : SUFFIX_ALL);
            return setNumberLiteral(getFloatToken(number, suffix), suffix);
        }

        return setIntegerLiteral(numberLiteralSuffix(SUFFIX_ALL));
    }

    private int setNumberLiteral(int type, int suffix) {
        if ((suffix & SUFFIX_I) != 0) type = RipperParser.tIMAGINARY;

        setState(EXPR_END);
        return type;
    }

    private int setIntegerLiteral(int suffix) {
        int type = (suffix & SUFFIX_R) != 0 ? RipperParser.tRATIONAL : RipperParser.tINTEGER;

        return setNumberLiteral(type, suffix);
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
    }

    // mri: parser_tokadd_mbchar
    // This is different than MRI in that we return a boolean since we only care whether it was added
    // or not.  The MRI version returns the byte supplied which is never used as a value.
    public boolean tokenAddMBC(int first_byte, ByteList buffer) {
        return tokadd_mbchar(first_byte, buffer);
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
                if (nextc() != '-') {
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
            hexValue |= Integer.parseInt(String.valueOf((char) h1), 16) & 15;
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
            hexValue |= Integer.parseInt(String.valueOf((char) h1), 16) & 15;
        }

        // No hex value after the 'x'.
        if (i == 0 || (strict && count != i)) compile_error(errorMessage);

        return hexValue;
    }
}
