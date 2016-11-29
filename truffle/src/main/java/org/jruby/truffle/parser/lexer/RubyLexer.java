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
 *
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 ***** END LICENSE BLOCK *****/
package org.jruby.truffle.parser.lexer;

import org.jcodings.Encoding;
import org.joni.Matcher;
import org.joni.Option;
import org.jruby.common.IRubyWarnings;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.regexp.ClassicRegexp;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.parser.ast.BackRefParseNode;
import org.jruby.truffle.parser.ast.BignumParseNode;
import org.jruby.truffle.parser.ast.ComplexParseNode;
import org.jruby.truffle.parser.ast.FixnumParseNode;
import org.jruby.truffle.parser.ast.FloatParseNode;
import org.jruby.truffle.parser.ast.ListParseNode;
import org.jruby.truffle.parser.ast.NthRefParseNode;
import org.jruby.truffle.parser.ast.NumericParseNode;
import org.jruby.truffle.parser.ast.ParseNode;
import org.jruby.truffle.parser.ast.RationalParseNode;
import org.jruby.truffle.parser.ast.StrParseNode;
import org.jruby.truffle.parser.parser.ParserSupport;
import org.jruby.truffle.parser.parser.RubyParser;
import org.jruby.truffle.parser.parser.Tokens;
import org.jruby.util.ByteList;
import org.jruby.util.SafeDoubleParser;
import org.jruby.util.StringSupport;
import org.jruby.util.cli.Options;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

/*
 * This is a port of the MRI lexer to Java.
 */
public class RubyLexer extends LexingCommon {
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

    private BignumParseNode newBignumNode(String value, int radix) {
        return new BignumParseNode(getPosition(), new BigInteger(value, radix));
    }

    private FixnumParseNode newFixnumNode(String value, int radix) throws NumberFormatException {
        return new FixnumParseNode(getPosition(), Long.parseLong(value, radix));
    }
    
    private RationalParseNode newRationalNode(String value, int radix) throws NumberFormatException {
        return new RationalParseNode(getPosition(), Long.parseLong(value, radix), 1);
    }
    
    private ComplexParseNode newComplexNode(NumericParseNode number) {
        return new ComplexParseNode(getPosition(), number);
    }
    
    protected void ambiguousOperator(String op, String syn) {
        warnings.warn(ID.AMBIGUOUS_ARGUMENT, getPosition().getFile(), getPosition().getLine(), "`" + op + "' after local variable or literal is interpreted as binary operator");
        warnings.warn(ID.AMBIGUOUS_ARGUMENT, getPosition().getFile(), getPosition().getLine(), "even though it seems like " + syn);
    }
   
    public enum Keyword {
        END ("end", Tokens.kEND, Tokens.kEND, EXPR_END),
        ELSE ("else", Tokens.kELSE, Tokens.kELSE, EXPR_BEG),
        CASE ("case", Tokens.kCASE, Tokens.kCASE, EXPR_BEG),
        ENSURE ("ensure", Tokens.kENSURE, Tokens.kENSURE, EXPR_BEG),
        MODULE ("module", Tokens.kMODULE, Tokens.kMODULE, EXPR_BEG),
        ELSIF ("elsif", Tokens.kELSIF, Tokens.kELSIF, EXPR_BEG),
        DEF ("def", Tokens.kDEF, Tokens.kDEF, EXPR_FNAME),
        RESCUE ("rescue", Tokens.kRESCUE, Tokens.kRESCUE_MOD, EXPR_MID),
        NOT ("not", Tokens.kNOT, Tokens.kNOT, EXPR_ARG),
        THEN ("then", Tokens.kTHEN, Tokens.kTHEN, EXPR_BEG),
        YIELD ("yield", Tokens.kYIELD, Tokens.kYIELD, EXPR_ARG),
        FOR ("for", Tokens.kFOR, Tokens.kFOR, EXPR_BEG),
        SELF ("self", Tokens.kSELF, Tokens.kSELF, EXPR_END),
        FALSE ("false", Tokens.kFALSE, Tokens.kFALSE, EXPR_END),
        RETRY ("retry", Tokens.kRETRY, Tokens.kRETRY, EXPR_END),
        RETURN ("return", Tokens.kRETURN, Tokens.kRETURN, EXPR_MID),
        TRUE ("true", Tokens.kTRUE, Tokens.kTRUE, EXPR_END),
        IF ("if", Tokens.kIF, Tokens.kIF_MOD, EXPR_BEG),
        DEFINED_P ("defined?", Tokens.kDEFINED, Tokens.kDEFINED, EXPR_ARG),
        SUPER ("super", Tokens.kSUPER, Tokens.kSUPER, EXPR_ARG),
        UNDEF ("undef", Tokens.kUNDEF, Tokens.kUNDEF, EXPR_FNAME),
        BREAK ("break", Tokens.kBREAK, Tokens.kBREAK, EXPR_MID),
        IN ("in", Tokens.kIN, Tokens.kIN, EXPR_BEG),
        DO ("do", Tokens.kDO, Tokens.kDO, EXPR_BEG),
        NIL ("nil", Tokens.kNIL, Tokens.kNIL, EXPR_END),
        UNTIL ("until", Tokens.kUNTIL, Tokens.kUNTIL_MOD, EXPR_BEG),
        UNLESS ("unless", Tokens.kUNLESS, Tokens.kUNLESS_MOD, EXPR_BEG),
        OR ("or", Tokens.kOR, Tokens.kOR, EXPR_BEG),
        NEXT ("next", Tokens.kNEXT, Tokens.kNEXT, EXPR_MID),
        WHEN ("when", Tokens.kWHEN, Tokens.kWHEN, EXPR_BEG),
        REDO ("redo", Tokens.kREDO, Tokens.kREDO, EXPR_END),
        AND ("and", Tokens.kAND, Tokens.kAND, EXPR_BEG),
        BEGIN ("begin", Tokens.kBEGIN, Tokens.kBEGIN, EXPR_BEG),
        __LINE__ ("__LINE__", Tokens.k__LINE__, Tokens.k__LINE__, EXPR_END),
        CLASS ("class", Tokens.kCLASS, Tokens.kCLASS, EXPR_CLASS),
        __FILE__("__FILE__", Tokens.k__FILE__, Tokens.k__FILE__, EXPR_END),
        LEND ("END", Tokens.klEND, Tokens.klEND, EXPR_END),
        LBEGIN ("BEGIN", Tokens.klBEGIN, Tokens.klBEGIN, EXPR_END),
        WHILE ("while", Tokens.kWHILE, Tokens.kWHILE_MOD, EXPR_BEG),
        ALIAS ("alias", Tokens.kALIAS, Tokens.kALIAS, EXPR_FNAME),
        __ENCODING__("__ENCODING__", Tokens.k__ENCODING__, Tokens.k__ENCODING__, EXPR_END);
        
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
        return map.get(str);
    }
    
    // Used for tiny smidgen of grammar in lexer (see setParserSupport())
    private ParserSupport parserSupport = null;

    // What handles warnings
    private IRubyWarnings warnings;

    public int tokenize_ident(int result) {
        // FIXME: Get token from newtok index to lex_p?
        String value = createTokenString();

        if (isLexState(last_state, EXPR_DOT|EXPR_FNAME) && parserSupport.getCurrentScope().isDefined(value) >= 0) {
            setState(EXPR_END);
        }

        yaccValue = value.intern();
        return result;
    }

    private StrTerm lex_strterm;

    public RubyLexer(ParserSupport support, LexerSource source, IRubyWarnings warnings) {
        super(source);
        this.parserSupport = support;
        this.warnings = warnings;
        reset();
    }

    @Deprecated
    public RubyLexer(ParserSupport support, LexerSource source) {
        super(source);
        this.parserSupport = support;
        reset();
    }

    public void reset() {
        super.reset();
        lex_strterm = null;
        // FIXME: ripper offsets correctly but we need to subtract one?
        ruby_sourceline = src.getLineOffset() - 1;

        parser_prepare();
    }

    public int nextc() {
        if (lex_p == lex_pend) {
            line_offset += lex_pend;

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
                warnings.warn(ID.VOID_VALUE_EXPRESSION, getFile(), ruby_sourceline, "encountered \\r in middle of line, treated as a mere space");
                c = ' ';
            }
        }

        return c;
    }

    public void heredoc_dedent(ParseNode root) {
        int indent = heredoc_indent;

        if (indent <= 0 || root == null) return;

        if (root instanceof StrParseNode) {
            StrParseNode str = (StrParseNode) root;
            dedent_string(str.getValue(), indent);
        } else if (root instanceof ListParseNode) {
            ListParseNode list = (ListParseNode) root;
            int length = list.size();
            int currentLine = -1;
            for (int i = 0; i < length; i++) {
                ParseNode child = list.get(i);
                if (currentLine == child.getLine()) continue;  // Only process first element on a line?

                currentLine = child.getLine();                 // New line

                if (child instanceof StrParseNode) {
                    dedent_string(((StrParseNode) child).getValue(), indent);
                }
            }
        }
    }

    public void compile_error(String message) {
        throw new SyntaxException(SyntaxException.PID.BAD_HEX_NUMBER, getFile(), ruby_sourceline, lexb.toString(), message);
    }

    // FIXME: How does lexb.toString() vs getCurrentLine() differ.
    public void compile_error(SyntaxException.PID pid, String message) {
        String src = createAsEncodedString(lex_lastline.unsafeBytes(), lex_lastline.begin(), lex_lastline.length(), getEncoding());
        throw new SyntaxException(pid, getFile(), ruby_sourceline, src, message);
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

    public int nextToken() throws IOException {
        token = yylex();
        return token == EOF ? 0 : token;
    }

    public ISourcePosition getPosition(ISourcePosition startPosition) {
        if (startPosition != null) return startPosition;

        if (tokline != null && ruby_sourceline == tokline.getLine()) return tokline;

        return new SimpleSourcePosition(getFile(), ruby_sourceline);
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

    @Override
    protected void setCompileOptionFlag(String name, ByteList value) {
        if (tokenSeen) {
            warnings.warn(ID.ACCESSOR_MODULE_FUNCTION, "`" + name + "' is ignored after any tokens");
            return;
        }

        int b = asTruth(name, value);
        if (b < 0) return;

        // Enebo: This is a hash in MRI for multiple potential compile options but we currently only support one.
        // I am just going to set it and when a second is done we will reevaluate how they are populated.
        parserSupport.getConfiguration().setFrozenStringLiteral(b == 1);
    }

    private final ByteList TRUE = new ByteList(new byte[] {'t', 'r', 'u', 'e'});
    private final ByteList FALSE = new ByteList(new byte[] {'f', 'a', 'l', 's', 'e'});
    protected int asTruth(String name, ByteList value) {
        int result = value.caseInsensitiveCmp(TRUE);
        if (result == 0) return 1;

        result = value.caseInsensitiveCmp(FALSE);
        if (result == 0) return 0;

        warnings.warn(ID.ACCESSOR_MODULE_FUNCTION, "invalid value for " + name + ": " + value);
        return -1;
    }

    @Override
    protected void setTokenInfo(String name, ByteList value) {

    }

    protected void setEncoding(ByteList name) {
        final RubyContext context = parserSupport.getConfiguration().getContext();
        Encoding newEncoding = Layouts.ENCODING.getEncoding(context.getEncodingManager().getRubyEncoding(name.toString()));

        if (newEncoding == null) throw new RaiseException(context.getCoreExceptions().argumentError("unknown encoding name: " + name.toString(), null));
        if (!newEncoding.isAsciiCompatible()) throw new RaiseException(context.getCoreExceptions().argumentError(name.toString() + " is not ASCII compatible", null));

        setEncoding(newEncoding);
    }

    public StrTerm getStrTerm() {
        return lex_strterm;
    }

    public void setStrTerm(StrTerm strterm) {
        this.lex_strterm = strterm;
    }

    public void setWarnings(IRubyWarnings warnings) {
        this.warnings = warnings;
    }

    private int considerComplex(int token, int suffix) {
        if ((suffix & SUFFIX_I) == 0) {
            return token;
        } else {
            yaccValue = newComplexNode((NumericParseNode) yaccValue);
            return RubyParser.tIMAGINARY;
        }
    }

    private int getFloatToken(String number, int suffix) {
        if ((suffix & SUFFIX_R) != 0) {
            BigDecimal bd = new BigDecimal(number);
            BigDecimal denominator = BigDecimal.ONE.scaleByPowerOfTen(bd.scale());
            BigDecimal numerator = bd.multiply(denominator);

            try {
                yaccValue = new RationalParseNode(getPosition(), numerator.longValueExact(), denominator.longValueExact());
            } catch (ArithmeticException ae) {
                // FIXME: Rational supports Bignum numerator and denominator
                compile_error(SyntaxException.PID.RATIONAL_OUT_OF_RANGE, "Rational (" + numerator + "/" + denominator + ") out of range.");
            }
            return considerComplex(Tokens.tRATIONAL, suffix);
        }

        double d;
        try {
            d = SafeDoubleParser.parseDouble(number);
        } catch (NumberFormatException e) {
            warnings.warn(ID.FLOAT_OUT_OF_RANGE, getPosition().getFile(), getPosition().getLine(), "Float " + number + " out of range.");

            d = number.startsWith("-") ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }
        yaccValue = new FloatParseNode(getPosition(), d);
        return considerComplex(Tokens.tFLOAT, suffix);
    }

    private int getIntegerToken(String value, int radix, int suffix) {
        ParseNode literalValue;

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


    // STR_NEW3/parser_str_new
    public StrParseNode createStr(ByteList buffer, int flags) {
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

        StrParseNode newStr = new StrParseNode(getPosition(), buffer, codeRange);

        if (parserSupport.getConfiguration().isFrozenStringLiteral()) newStr.setFrozen(true);

        return newStr;
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
            if (Character.isLetterOrDigit(begin) /* no mb || ismbchar(term)*/) compile_error(SyntaxException.PID.STRING_UNKNOWN_TYPE, "unknown type of %string");
        }
        if (c == EOF || begin == EOF) compile_error(SyntaxException.PID.STRING_HITS_EOF, "unterminated quoted string meets end of file");

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
            setState(EXPR_FNAME);
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
            compile_error(SyntaxException.PID.STRING_UNKNOWN_TYPE, "unknown type of %string");
        }
        return -1; // not-reached
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
                    pushback(heredoc_indent > 0 ? '~' : '-');
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

    private boolean arg_ambiguous() {
        if (warnings.isVerbose() && Options.PARSER_WARN_AMBIGUOUS_ARGUMENTS.load() && !ParserSupport.skipTruffleRubiniusWarnings(this)) {
            warnings.warning(ID.AMBIGUOUS_ARGUMENT, getPosition().getFile(), getPosition().getLine(), "Ambiguous first argument; make sure.");
        }
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
            case Tokens.tSTRING_CONTENT: System.err.print("tSTRING_CONTENT[" + ((StrParseNode) value()).getValue() + "],"); break;
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
            case Tokens.tLABEL_END: System.err.print("tLABEL_END"); break;
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
    @SuppressWarnings("fallthrough")
    private int yylex() throws IOException {
        int c;
        boolean spaceSeen = false;
        boolean commandState;
        boolean tokenSeen = this.tokenSeen;

        if (lex_strterm != null) {
            int tok = lex_strterm.parseString(this);

            if (tok == Tokens.tSTRING_END && (lex_strterm.getFlags() & STR_FUNC_LABEL) != 0) {
                if ((isLexState(lex_state, EXPR_BEG|EXPR_ENDFN) && !conditionState.isInState() ||
                        isARG()) && isLabelSuffix()) {
                    nextc();
                    tok = Tokens.tLABEL_END;
                    setState(EXPR_BEG|EXPR_LABEL);
                    lex_strterm = null;
                }
            }

            if (tok == Tokens.tSTRING_END || tok == Tokens.tREGEXP_END) {
                lex_strterm = null;
                setState(EXPR_END);
            }

            return tok;
        }

        commandState = commandStart;
        commandStart = false;
        this.tokenSeen = true;

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
                this.tokenSeen = tokenSeen;
                if (!parseMagicComment(lexb.makeShared(lex_p, lex_pend - lex_p))) {
                    if (comment_at_top()) set_file_encoding(lex_p, lex_pend);
                }
                lex_p = lex_pend;
            }
            /* fall through */
            case '\n': {
                this.tokenSeen = tokenSeen;
                boolean normalArg = isLexState(lex_state, EXPR_BEG | EXPR_CLASS | EXPR_FNAME | EXPR_DOT) &&
                        !isLexState(lex_state, EXPR_LABELED);
                if (normalArg || isLexStateAll(lex_state, EXPR_ARG | EXPR_LABELED)) {
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
                    case ' ': case '\t': case '\f': case '\r': case '\13': /* '\v' */
                        spaceSeen = true;
                        continue;
                    case '&':
                    case '.': {
                        if (peek('.') == (c == '&')) {
                            pushback(c);

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

                setState(isAfterOperator() ? EXPR_ARG : EXPR_BEG);

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
                    line_offset += lex_pend;
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
        if (result == Tokens.tIDENTIFIER && !isLexState(last_state, EXPR_DOT|EXPR_FNAME) &&
                parserSupport.getCurrentScope().isDefined(value) >= 0) {
            setState(EXPR_END);
        }

        yaccValue = value;
        return result;
    }

    private int ampersand(boolean spaceSeen) throws IOException {
        int c = nextc();

        switch (c) {
        case '&':
            setState(EXPR_BEG);
            if ((c = nextc()) == '=') {
                yaccValue = "&&";
                setState(EXPR_BEG);
                return Tokens.tOP_ASGN;
            }
            pushback(c);
            yaccValue = "&&";
            return Tokens.tANDOP;
        case '=':
            yaccValue = "&";
            setState(EXPR_BEG);
            return Tokens.tOP_ASGN;
        case '.':
            setState(EXPR_DOT);
            yaccValue = "&.";
            return Tokens.tANDDOT;
        }
        pushback(c);

        //tmpPosition is required because getPosition()'s side effects.
        //if the warning is generated, the getPosition() on line 954 (this line + 18) will create
        //a wrong position if the "inclusive" flag is not set.
        ISourcePosition tmpPosition = getPosition();
        if (isSpaceArg(c, spaceSeen)) {
            if (warnings.isVerbose() && Options.PARSER_WARN_ARGUMENT_PREFIX.load())
                warnings.warning(ID.ARGUMENT_AS_PREFIX, tmpPosition.getFile(), tmpPosition.getLine(), "`&' interpreted as argument prefix");
            c = Tokens.tAMPER;
        } else if (isBEG()) {
            c = Tokens.tAMPER;
        } else {
            warn_balanced(c, spaceSeen, "&", "argument prefix");
            c = Tokens.tAMPER2;
        }

        setState(isAfterOperator() ? EXPR_ARG : EXPR_BEG);

        yaccValue = "&";
        return c;
    }

    // MRI: parser_magic_comment
    public boolean parseMagicComment(ByteList magicLine) throws IOException {
        int length = magicLine.length();

        if (length <= 7) return false;
        int beg = magicCommentMarker(magicLine, 0);
        if (beg >= 0) {
            int end = magicCommentMarker(magicLine, beg);
            if (end < 0) return false;
            length = end - beg - 3; // -3 is to backup over end just found
        } else {
            beg = 0;
        }

        int begin = magicLine.getBegin() + beg;
        Matcher matcher = magicRegexp.matcher(magicLine.unsafeBytes(), begin, begin + length);
        int result = ClassicRegexp.matcherSearch(matcher, begin, begin + length, Option.NONE);

        if (result < 0) return false;

        // Regexp is guaranteed to have three matches
        int begs[] = matcher.getRegion().beg;
        int ends[] = matcher.getRegion().end;
        String name = magicLine.subSequence(beg + begs[1], beg + ends[1]).toString().replace('-', '_');
        ByteList value = magicLine.makeShared(beg + begs[2], ends[2] - begs[2]);

        if ("coding".equals(name) || "encoding".equals(name)) {
            magicCommentEncoding(value);
        } else if ("frozen_string_literal".equals(name)) {
            setCompileOptionFlag(name, value);
        } else if ("warn_indent".equals(name)) {
            setTokenInfo(name, value);
        } else {
            return false;
        }

        return true;
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

        if (c == EOF || Character.isSpaceChar(c)) {
            if (result == Tokens.tIVAR) {
                compile_error("`@' without identifiers is not allowed as an instance variable name");
            }

            compile_error("`@@' without identifiers is not allowed as a class variable name");
        } else if (Character.isDigit(c) || !isIdentifierChar(c)) {
            pushback(c);
            if (result == Tokens.tIVAR) {
                compile_error(SyntaxException.PID.IVAR_BAD_NAME, "`@" + ((char) c) + "' is not allowed as an instance variable name");
            }
            compile_error(SyntaxException.PID.CVAR_BAD_NAME, "`@@" + ((char) c) + "' is not allowed as a class variable name");
        }

        if (!tokadd_ident(c)) return EOF;

        last_state = lex_state;
        setState(EXPR_END);

        return tokenize_ident(result);
    }

    private int backtick(boolean commandState) throws IOException {
        yaccValue = "`";

        if (isLexState(lex_state, EXPR_FNAME)) {
            setState(EXPR_ENDFN);
            return Tokens.tBACK_REF2;
        }
        if (isLexState(lex_state, EXPR_DOT)) {
            setState(commandState ? EXPR_CMDARG : EXPR_ARG);

            return Tokens.tBACK_REF2;
        }

        lex_strterm = new StringTerm(str_xquote, '\0', '`');
        return Tokens.tXSTRING_BEG;
    }

    private int bang() throws IOException {
        int c = nextc();

        if (isAfterOperator()) {
            setState(EXPR_ARG);
            if (c == '@') {
                yaccValue = "!";
                return Tokens.tBANG;
            }
        } else {
            setState(EXPR_BEG);
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
            setState(EXPR_BEG);
            yaccValue = "^";
            return Tokens.tOP_ASGN;
        }

        setState(isAfterOperator() ? EXPR_ARG : EXPR_BEG);

        pushback(c);
        yaccValue = "^";
        return Tokens.tCARET;
    }

    private int colon(boolean spaceSeen) throws IOException {
        int c = nextc();

        if (c == ':') {
            if (isBEG() || isLexState(lex_state, EXPR_CLASS) || (isARG() && spaceSeen)) {
                setState(EXPR_BEG);
                yaccValue = "::";
                return Tokens.tCOLON3;
            }
            setState(EXPR_DOT);
            yaccValue = ":";
            return Tokens.tCOLON2;
        }

        if (isEND() || Character.isWhitespace(c) || c == '#') {
            pushback(c);
            setState(EXPR_BEG);
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

        setState(EXPR_FNAME);
        yaccValue = ":";
        return Tokens.tSYMBEG;
    }

    private int comma(int c) throws IOException {
        setState(EXPR_BEG|EXPR_LABEL);
        yaccValue = ",";

        return c;
    }

    private int doKeyword(int state) {
        int leftParenBegin = getLeftParenBegin();
        if (leftParenBegin > 0 && leftParenBegin == parenNest) {
            setLeftParenBegin(0);
            parenNest--;
            return Tokens.kDO_LAMBDA;
        }

        if (conditionState.isInState()) return Tokens.kDO_COND;

        if (cmdArgumentState.isInState() && !isLexState(state, EXPR_CMDARG)) {
            return Tokens.kDO_BLOCK;
        }
        if (isLexState(state,  EXPR_BEG|EXPR_ENDARG)) {
            return Tokens.kDO_BLOCK;
        }
        return Tokens.kDO;
    }

    @SuppressWarnings("fallthrough")
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
            if (isLexState(last_state, EXPR_FNAME)) {
                yaccValue = "$" + (char) c;
                return Tokens.tGVAR;
            }

            yaccValue = new BackRefParseNode(getPosition(), c);
            return Tokens.tBACK_REF;

        case '1': case '2': case '3': case '4': case '5': case '6':
        case '7': case '8': case '9':
            do {
                c = nextc();
            } while (Character.isDigit(c));
            pushback(c);
            if (isLexState(last_state, EXPR_FNAME)) {
                yaccValue = createTokenString().intern();
                return Tokens.tGVAR;
            }

            int ref;
            String refAsString = createTokenString();

            try {
                ref = Integer.parseInt(refAsString.substring(1).intern());
            } catch (NumberFormatException e) {
                warnings.warn(ID.AMBIGUOUS_ARGUMENT, "`" + refAsString + "' is too big for a number variable, always nil");
                ref = 0;
            }

            yaccValue = new NthRefParseNode(getPosition(), ref);
            return Tokens.tNTH_REF;
        case '0':
            setState(EXPR_END);

            return identifierToken(Tokens.tGVAR, ("$" + (char) c).intern());
        default:
            if (!isIdentifierChar(c)) {
                if (c == EOF || Character.isSpaceChar(c)) {
                    compile_error(SyntaxException.PID.CVAR_BAD_NAME, "`$' without identifiers is not allowed as a global variable name");
                } else {
                    pushback(c);
                    compile_error(SyntaxException.PID.CVAR_BAD_NAME, "`$" + ((char) c) + "' is not allowed as a global variable name");
                }
            }

            last_state = lex_state;
            setState(EXPR_END);

            tokadd_ident(c);

            return identifierToken(Tokens.tGVAR, createTokenString().intern());  // $blah
        }
    }

    private int dot() throws IOException {
        int c;

        setState(EXPR_BEG);
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
        if (Character.isDigit(c)) compile_error(SyntaxException.PID.FLOAT_MISSING_ZERO, "no .<digit> floating literal anymore; put 0 before dot");

        setState(EXPR_DOT);
        yaccValue = ".";
        return Tokens.tDOT;
    }

    private int doubleQuote(boolean commandState) throws IOException {
        int label = isLabelPossible(commandState) ? str_label : 0;
        lex_strterm = new StringTerm(str_dquote|label, '\0', '"');
        yaccValue = "\"";

        return Tokens.tSTRING_BEG;
    }

    private int greaterThan() throws IOException {
        setState(isAfterOperator() ? EXPR_ARG : EXPR_BEG);

        int c = nextc();

        switch (c) {
        case '=':
            yaccValue = ">=";

            return Tokens.tGEQ;
        case '>':
            if ((c = nextc()) == '=') {
                setState(EXPR_BEG);
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
            compile_error(SyntaxException.PID.CHARACTER_BAD, "Invalid char `" + badChar + "' ('" + (char) c + "') in expression");
        }

        newtok(true);
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
            result = Tokens.tFID;
            tempVal = createTokenString();
        } else {
            if (isLexState(lex_state, EXPR_FNAME)) {
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
            tempVal = createTokenString();

            if (result == 0 && Character.isUpperCase(tempVal.charAt(0))) {
                result = Tokens.tCONSTANT;
            } else {
                result = Tokens.tIDENTIFIER;
            }
        }

        if (isLabelPossible(commandState)) {
            if (isLabelSuffix()) {
                setState(EXPR_ARG|EXPR_LABELED);
                nextc();
                yaccValue = tempVal.intern();
                return Tokens.tLABEL;
            }
        }

        if (lex_state != EXPR_DOT) {
            Keyword keyword = getKeyword(tempVal); // Is it is a keyword?

            if (keyword != null) {
                int state = lex_state; // Save state at time keyword is encountered
                setState(keyword.state);

                if (isLexState(state, EXPR_FNAME)) {
                    yaccValue = keyword.name;
                    return keyword.id0;
                } else {
                    yaccValue = getPosition();
                }

                if (isLexState(lex_state, EXPR_BEG)) commandStart = true;

                if (keyword.id0 == Tokens.kDO) return doKeyword(state);

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

        tempVal = tempVal.intern();

        if (tempVal.equals("function") && parserSupport.getContext().getOptions().INLINE_JS) {
            return javaScript(tempVal);
        }

        return identifierToken(result, tempVal);
    }

    private int javaScript(String keyword) {
        if (!Character.isWhitespace(p(lex_p))) {
            return identifierToken(Tokens.tIDENTIFIER, keyword);
        }

        int length = 0;

        while (Character.isWhitespace(p(lex_p + length))) {
            length++;
        }

        if (!Character.isJavaIdentifierPart(p(lex_p + length))) {
            return identifierToken(Tokens.tIDENTIFIER, keyword);
        }

        length++;

        while (Character.isJavaIdentifierPart(p(lex_p + length))) {
            length++;
        }

        if (p(lex_p + length) != '(') {
            return identifierToken(Tokens.tIDENTIFIER, keyword);
        }

        length++;

        // Commit to parsing this as JavaScript

        // TODO CS 11-09-16 strings, escaping etc

        while (p(lex_p + length) != '{') {
            length++;
        }

        length++;

        int depth = 0;

        int c;

        while ((c = p(lex_p + length)) != '}' || depth > 0) {
            switch (c) {
                case '{':
                    depth++;
                    break;
                case '}':
                    depth--;
                    break;
            }

            length++;
        }

        length++;

        final StringBuilder builder = new StringBuilder();
        builder.append(keyword);

        for (int n = 0; n < length; n++) {
            builder.append((char) nextc());
        }

        yaccValue = builder.toString();
        return Tokens.tJAVASCRIPT;
    }

    private int leftBracket(boolean spaceSeen) throws IOException {
        parenNest++;
        int c = '[';
        if (isAfterOperator()) {
            setState(EXPR_ARG);

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
            setState(getState() | EXPR_LABEL);
            yaccValue = "[";
            return '[';
        } else if (isBEG() || (isARG() && (spaceSeen || isLexState(lex_state, EXPR_LABELED)))) {
            c = Tokens.tLBRACK;
        }

        setState(EXPR_BEG|EXPR_LABEL);
        conditionState.stop();
        cmdArgumentState.stop();
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
            conditionState.stop();
            cmdArgumentState.stop();
            yaccValue = "{";
            return Tokens.tLAMBEG;
        }

        char c;
        if (isLexState(lex_state, EXPR_LABELED)) {
            c = Tokens.tLBRACE;
        } else if (isLexState(lex_state, EXPR_ARG_ANY|EXPR_END|EXPR_ENDFN)) { // block (primary)
            c = Tokens.tLCURLY;
        } else if (isLexState(lex_state, EXPR_ENDARG)) { // block (expr)
            c = Tokens.tLBRACE_ARG;
        } else { // hash
            c = Tokens.tLBRACE;
        }

        conditionState.stop();
        cmdArgumentState.stop();
        setState(EXPR_BEG);
        if (c != Tokens.tLBRACE_ARG) setState(getState() | EXPR_LABEL);
        if (c != Tokens.tLBRACE) commandStart = true;
        yaccValue = getPosition();

        return c;
    }

    private int leftParen(boolean spaceSeen) throws IOException {
        int result;

        if (isBEG()) {
            result = Tokens.tLPAREN;
        } else if (isSpaceArg('(', spaceSeen)) {
            result = Tokens.tLPAREN_ARG;
        } else {
            result = Tokens.tLPAREN2;
        }

        parenNest++;
        conditionState.stop();
        cmdArgumentState.stop();
        setState(EXPR_BEG|EXPR_LABEL);

        yaccValue = getPosition();
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

        if (isAfterOperator()) {
            setState(EXPR_ARG);
        } else {
            if (isLexState(lex_state, EXPR_CLASS)) commandStart = true;
            setState(EXPR_BEG);
        }

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
                setState(EXPR_BEG);
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

        if (isAfterOperator()) {
            setState(EXPR_ARG);
            if (c == '@') {
                yaccValue = "-@";
                return Tokens.tUMINUS;
            }
            pushback(c);
            yaccValue = "-";
            return Tokens.tMINUS;
        }
        if (c == '=') {
            setState(EXPR_BEG);
            yaccValue = "-";
            return Tokens.tOP_ASGN;
        }
        if (c == '>') {
            setState(EXPR_ENDFN);
            yaccValue = "->";
            return Tokens.tLAMBDA;
        }
        if (isBEG() || (isSpaceArg(c, spaceSeen) && arg_ambiguous())) {
            setState(EXPR_BEG);
            pushback(c);
            yaccValue = "-";
            if (Character.isDigit(c)) {
                return Tokens.tUMINUS_NUM;
            }
            return Tokens.tUMINUS;
        }
        setState(EXPR_BEG);
        pushback(c);
        yaccValue = "-";
        warn_balanced(c, spaceSeen, "-", "unary operator");
        return Tokens.tMINUS;
    }

    private int percent(boolean spaceSeen) throws IOException {
        if (isBEG()) return parseQuote(nextc());

        int c = nextc();

        if (c == '=') {
            setState(EXPR_BEG);
            yaccValue = "%";
            return Tokens.tOP_ASGN;
        }

        if (isSpaceArg(c, spaceSeen)) return parseQuote(c);

        setState(isAfterOperator() ? EXPR_ARG : EXPR_BEG);

        pushback(c);
        yaccValue = "%";
        warn_balanced(c, spaceSeen, "%", "string literal");
        return Tokens.tPERCENT;
    }

    private int pipe() throws IOException {
        int c = nextc();

        switch (c) {
        case '|':
            setState(EXPR_BEG);
            if ((c = nextc()) == '=') {
                setState(EXPR_BEG);
                yaccValue = "||";
                return Tokens.tOP_ASGN;
            }
            pushback(c);
            yaccValue = "||";
            return Tokens.tOROP;
        case '=':
            setState(EXPR_BEG);
            yaccValue = "|";
            return Tokens.tOP_ASGN;
        default:
            setState(isAfterOperator() ? EXPR_ARG : EXPR_BEG|EXPR_LABEL);

            pushback(c);
            yaccValue = "|";
            return Tokens.tPIPE;
        }
    }

    private int plus(boolean spaceSeen) throws IOException {
        int c = nextc();
        if (isAfterOperator()) {
            setState(EXPR_ARG);
            if (c == '@') {
                yaccValue = "+@";
                return Tokens.tUPLUS;
            }
            pushback(c);
            yaccValue = "+";
            return Tokens.tPLUS;
        }

        if (c == '=') {
            setState(EXPR_BEG);
            yaccValue = "+";
            return Tokens.tOP_ASGN;
        }

        if (isBEG() || (isSpaceArg(c, spaceSeen) && arg_ambiguous())) {
            setState(EXPR_BEG);
            pushback(c);
            if (Character.isDigit(c)) {
                c = '+';
                return parseNumber(c);
            }
            yaccValue = "+";
            return Tokens.tUPLUS;
        }

        setState(EXPR_BEG);
        pushback(c);
        yaccValue = "+";
        warn_balanced(c, spaceSeen, "+", "unary operator");
        return Tokens.tPLUS;
    }

    private int questionMark() throws IOException {
        int c;

        if (isEND()) {
            setState(EXPR_VALUE);
            yaccValue = "?";
            return '?';
        }

        c = nextc();
        if (c == EOF) compile_error(SyntaxException.PID.INCOMPLETE_CHAR_SYNTAX, "incomplete character syntax");

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
                    warnings.warn(ID.INVALID_CHAR_SEQUENCE, getPosition().getFile(), getPosition().getLine(), "invalid character syntax; use ?\\" + c2);
                }
            }
            pushback(c);
            setState(EXPR_VALUE);
            yaccValue = "?";
            return '?';
        }

        if (!isASCII(c)) {
            if (!tokadd_mbchar(c)) return EOF;
        } else if (isIdentifierChar(c) && !peek('\n') && isNext_identchar()) {
            newtok(true);
            pushback(c);
            setState(EXPR_VALUE);
            yaccValue = "?";
            return '?';
        } else if (c == '\\') {
            if (peek('u')) {
                nextc(); // Eat 'u'
                ByteList oneCharBL = new ByteList(2);
                oneCharBL.setEncoding(getEncoding());

                c = readUTFEscape(oneCharBL, false, false);

                if (c >= 0x80) {
                    tokaddmbc(c, oneCharBL);
                } else {
                    oneCharBL.append(c);
                }

                setState(EXPR_END);
                yaccValue = new StrParseNode(getPosition(), oneCharBL);

                return Tokens.tCHAR;
            } else {
                c = readEscape();
            }
        } else {
            newtok(true);
        }

        ByteList oneCharBL = new ByteList(1);
        oneCharBL.append(c);
        yaccValue = new StrParseNode(getPosition(), oneCharBL);
        setState(EXPR_END);
        return Tokens.tCHAR;
    }

    private int rightBracket() {
        parenNest--;
        conditionState.restart();
        cmdArgumentState.restart();
        setState(EXPR_ENDARG);
        yaccValue = "]";
        return Tokens.tRBRACK;
    }

    private int rightCurly() {
        conditionState.restart();
        cmdArgumentState.restart();
        setState(EXPR_ENDARG);
        yaccValue = "}";
        int tok = braceNest == 0 ? Tokens.tSTRING_DEND : Tokens.tRCURLY;
        braceNest--;
        return tok;
    }

    private int rightParen() {
        parenNest--;
        conditionState.restart();
        cmdArgumentState.restart();
        setState(EXPR_ENDFN);
        yaccValue = ")";
        return Tokens.tRPAREN;
    }

    private int singleQuote(boolean commandState) throws IOException {
        int label = isLabelPossible(commandState) ? str_label : 0;
        lex_strterm = new StringTerm(str_squote|label, '\0', '\'');
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
            setState(EXPR_BEG);
            yaccValue = "/";
            return Tokens.tOP_ASGN;
        }
        pushback(c);
        if (isSpaceArg(c, spaceSeen)) {
            arg_ambiguous();
            lex_strterm = new StringTerm(str_regexp, '\0', '/');
            yaccValue = "/";
            return Tokens.tREGEXP_BEG;
        }

        setState(isAfterOperator() ? EXPR_ARG : EXPR_BEG);

        yaccValue = "/";
        warn_balanced(c, spaceSeen, "/", "regexp literal");
        return Tokens.tDIVIDE;
    }

    private int star(boolean spaceSeen) throws IOException {
        int c = nextc();
        
        switch (c) {
        case '*':
            if ((c = nextc()) == '=') {
                setState(EXPR_BEG);
                yaccValue = "**";
                return Tokens.tOP_ASGN;
            }

            pushback(c); // not a '=' put it back
            yaccValue = "**";

            if (isSpaceArg(c, spaceSeen)) {
                if (warnings.isVerbose() && Options.PARSER_WARN_ARGUMENT_PREFIX.load())
                    warnings.warning(ID.ARGUMENT_AS_PREFIX, getPosition().getFile(), getPosition().getLine(), "`**' interpreted as argument prefix");
                c = Tokens.tDSTAR;
            } else if (isBEG()) {
                c = Tokens.tDSTAR;
            } else {
                warn_balanced(c, spaceSeen, "**", "argument prefix");
                c = Tokens.tPOW;
            }
            break;
        case '=':
            setState(EXPR_BEG);
            yaccValue = "*";
            return Tokens.tOP_ASGN;
        default:
            pushback(c);
            if (isSpaceArg(c, spaceSeen)) {
                if (warnings.isVerbose() && Options.PARSER_WARN_ARGUMENT_PREFIX.load() && !ParserSupport.skipTruffleRubiniusWarnings(this))
                    warnings.warning(ID.ARGUMENT_AS_PREFIX, getPosition().getFile(), getPosition().getLine(), "`*' interpreted as argument prefix");
                c = Tokens.tSTAR;
            } else if (isBEG()) {
                c = Tokens.tSTAR;
            } else {
                warn_balanced(c, spaceSeen, "*", "argument prefix");
                c = Tokens.tSTAR2;
            }
            yaccValue = "*";
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
    @SuppressWarnings("fallthrough")
    private int parseNumber(int c) throws IOException {
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
                        compile_error(SyntaxException.PID.BAD_HEX_NUMBER, "Hexadecimal number without hex-digits.");
                    } else if (nondigit != '\0') {
                        compile_error(SyntaxException.PID.TRAILING_UNDERSCORE_IN_NUMBER, "Trailing '_' in number.");
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
                        compile_error(SyntaxException.PID.EMPTY_BINARY_NUMBER, "Binary number without digits.");
                    } else if (nondigit != '\0') {
                        compile_error(SyntaxException.PID.TRAILING_UNDERSCORE_IN_NUMBER, "Trailing '_' in number.");
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
                        compile_error(SyntaxException.PID.EMPTY_BINARY_NUMBER, "Binary number without digits.");
                    } else if (nondigit != '\0') {
                        compile_error(SyntaxException.PID.TRAILING_UNDERSCORE_IN_NUMBER, "Trailing '_' in number.");
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

                        if (nondigit != '\0') compile_error(SyntaxException.PID.TRAILING_UNDERSCORE_IN_NUMBER, "Trailing '_' in number.");

                        return getIntegerToken(numberBuffer.toString(), 8, numberLiteralSuffix(SUFFIX_ALL));
                    }
                case '8' :
                case '9' :
                    compile_error(SyntaxException.PID.BAD_OCTAL_DIGIT, "Illegal octal digit.");
                case '.' :
                case 'e' :
                case 'E' :
                	numberBuffer.append('0');
                    break;
                default :
                    pushback(c);
                    numberBuffer.append('0');
                    return getIntegerToken(numberBuffer.toString(), 10, numberLiteralSuffix(SUFFIX_ALL));
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
                        compile_error(SyntaxException.PID.TRAILING_UNDERSCORE_IN_NUMBER, "Trailing '_' in number.");
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
                        compile_error(SyntaxException.PID.TRAILING_UNDERSCORE_IN_NUMBER, "Trailing '_' in number.");
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
                    if (nondigit != '\0') compile_error(SyntaxException.PID.TRAILING_UNDERSCORE_IN_NUMBER, "Trailing '_' in number.");
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
            compile_error(SyntaxException.PID.TRAILING_UNDERSCORE_IN_NUMBER, "Trailing '_' in number.");
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
                    compile_error(SyntaxException.PID.INVALID_ESCAPE_SYNTAX, "invalid Unicode codepoint (too large)");
                }
            } while (peek(' ') || peek('\t'));

            int c = nextc();
            if (c != '}') compile_error(SyntaxException.PID.INVALID_ESCAPE_SYNTAX,  "unterminated Unicode escape");

            buffer.append((char) c);
        } else { // handle \\uxxxx
            scanHexLiteral(buffer, 4, true, "Invalid Unicode escape");
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
                    compile_error(SyntaxException.PID.INVALID_ESCAPE_SYNTAX,  "invalid Unicode codepoint (too large)");
                }
                if (buffer != null) readUTF8EscapeIntoBuffer(codepoint, buffer, stringLiteral);
            } while (peek(' ') || peek('\t'));

            c = nextc();
            if (c != '}') {
                compile_error(SyntaxException.PID.INVALID_ESCAPE_SYNTAX, "unterminated Unicode escape");
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

    @SuppressWarnings("fallthrough")
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
                    compile_error(SyntaxException.PID.INVALID_ESCAPE_SYNTAX, "Invalid escape character syntax");
                } else if ((c = nextc()) == '\\') {
                    return (char) (readEscape() | 0x80);
                } else if (c == EOF) {
                    compile_error(SyntaxException.PID.INVALID_ESCAPE_SYNTAX, "Invalid escape character syntax");
                } 
                return (char) ((c & 0xff) | 0x80);
            case 'C' :
                if (nextc() != '-') {
                    compile_error(SyntaxException.PID.INVALID_ESCAPE_SYNTAX, "Invalid escape character syntax");
                }
            case 'c' :
                if ((c = nextc()) == '\\') {
                    c = readEscape();
                } else if (c == '?') {
                    return '\177';
                } else if (c == EOF) {
                    compile_error(SyntaxException.PID.INVALID_ESCAPE_SYNTAX, "Invalid escape character syntax");
                }
                return (char) (c & 0x9f);
            case EOF :
                compile_error(SyntaxException.PID.INVALID_ESCAPE_SYNTAX, "Invalid escape character syntax");
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
            hexValue |= Integer.parseInt(String.valueOf((char) h1), 16) & 15;
        }

        // No hex value after the 'x'.
        if (i == 0 || strict && count != i) {
            compile_error(SyntaxException.PID.INVALID_ESCAPE_SYNTAX, errorMessage);
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
        if (i == 0 || (strict && count != i)) compile_error(SyntaxException.PID.INVALID_ESCAPE_SYNTAX, errorMessage);

        return hexValue;
    }
}