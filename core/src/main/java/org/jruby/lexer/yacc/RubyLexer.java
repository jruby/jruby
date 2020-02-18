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
import java.util.HashMap;
import java.util.Map;

import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BignumNode;
import org.jruby.ast.ComplexNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FloatNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.Node;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.NumericNode;
import org.jruby.ast.RationalNode;
import org.jruby.ast.StrNode;
import org.jruby.common.IRubyWarnings;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.lexer.LexerSource;
import org.jruby.lexer.LexingCommon;
import org.jruby.lexer.yacc.SyntaxException.PID;
import org.jruby.parser.ParserSupport;
import org.jruby.parser.RubyParser;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;
import org.jruby.util.SafeDoubleParser;
import org.jruby.util.StringSupport;
import org.jruby.util.cli.Options;

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

    private BignumNode newBignumNode(String value, int radix) {
        return new BignumNode(getPosition(), new BigInteger(value, radix));
    }

    private FixnumNode newFixnumNode(String value, int radix) throws NumberFormatException {
        return new FixnumNode(getPosition(), Long.parseLong(value, radix));
    }
    
    private RationalNode newRationalNode(String value, int radix) throws NumberFormatException {
        NumericNode numerator;

        try {
            numerator = new FixnumNode(getPosition(), Long.parseLong(value, radix));
        } catch (NumberFormatException e) {
            numerator = new BignumNode(getPosition(), new BigInteger(value, radix));
        }

        return new RationalNode(getPosition(), numerator, new FixnumNode(getPosition(), 1));
    }
    
    private ComplexNode newComplexNode(NumericNode number) {
        return new ComplexNode(getPosition(), number);
    }
    
    protected void ambiguousOperator(String op, String syn) {
        warnings.warn(ID.AMBIGUOUS_ARGUMENT, getFile(), ruby_sourceline,
                "`" + op + "' after local variable or literal is interpreted as binary operator");
        warnings.warn(ID.AMBIGUOUS_ARGUMENT, getFile(), ruby_sourceline, "even though it seems like " + syn);
    }

    public enum Keyword {
        END ("end", new ByteList(new byte[] {'e', 'n', 'd'}, USASCII_ENCODING), RubyParser.keyword_end, RubyParser.keyword_end, EXPR_END),
        ELSE ("else", new ByteList(new byte[] {'e', 'l', 's', 'e'}, USASCII_ENCODING), RubyParser.keyword_else, RubyParser.keyword_else, EXPR_BEG),
        CASE ("case", new ByteList(new byte[] {'c', 'a', 's', 'e'}, USASCII_ENCODING), RubyParser.keyword_case, RubyParser.keyword_case, EXPR_BEG),
        ENSURE ("ensure", new ByteList(new byte[] {'e', 'n', 's', 'u', 'r', 'e'}, USASCII_ENCODING), RubyParser.keyword_ensure, RubyParser.keyword_ensure, EXPR_BEG),
        MODULE ("module", new ByteList(new byte[] {'m', 'o', 'd', 'u', 'l', 'e'}, USASCII_ENCODING), RubyParser.keyword_module, RubyParser.keyword_module, EXPR_BEG),
        ELSIF ("elsif", new ByteList(new byte[] {'e', 'l', 's', 'i', 'f'}, USASCII_ENCODING), RubyParser.keyword_elsif, RubyParser.keyword_elsif, EXPR_BEG),
        DEF ("def", new ByteList(new byte[] {'d', 'e', 'f'}, USASCII_ENCODING), RubyParser.keyword_def, RubyParser.keyword_def, EXPR_FNAME),
        RESCUE ("rescue", new ByteList(new byte[] {'r', 'e', 's', 'c', 'u', 'e'}, USASCII_ENCODING), RubyParser.keyword_rescue, RubyParser.modifier_rescue, EXPR_MID),
        NOT ("not", new ByteList(new byte[] {'n', 'o', 't'}, USASCII_ENCODING), RubyParser.keyword_not, RubyParser.keyword_not, EXPR_ARG),
        THEN ("then", new ByteList(new byte[] {'t', 'h', 'e', 'n'}, USASCII_ENCODING), RubyParser.keyword_then, RubyParser.keyword_then, EXPR_BEG),
        YIELD ("yield", new ByteList(new byte[] {'y', 'i', 'e', 'l', 'd'}, USASCII_ENCODING), RubyParser.keyword_yield, RubyParser.keyword_yield, EXPR_ARG),
        FOR ("for", new ByteList(new byte[] {'f', 'o', 'r'}, USASCII_ENCODING), RubyParser.keyword_for, RubyParser.keyword_for, EXPR_BEG),
        SELF ("self", new ByteList(new byte[] {'s', 'e', 'l', 'f'}, USASCII_ENCODING), RubyParser.keyword_self, RubyParser.keyword_self, EXPR_END),
        FALSE ("false", new ByteList(new byte[] {'f', 'a', 'l', 's', 'e'}, USASCII_ENCODING), RubyParser.keyword_false, RubyParser.keyword_false, EXPR_END),
        RETRY ("retry", new ByteList(new byte[] {'r', 'e', 't', 'r', 'y'}, USASCII_ENCODING), RubyParser.keyword_retry, RubyParser.keyword_retry, EXPR_END),
        RETURN ("return", new ByteList(new byte[] {'r', 'e', 't', 'u', 'r', 'n'}, USASCII_ENCODING), RubyParser.keyword_return, RubyParser.keyword_return, EXPR_MID),
        TRUE ("true", new ByteList(new byte[] {'t', 'r', 'u', 'e'}, USASCII_ENCODING), RubyParser.keyword_true, RubyParser.keyword_true, EXPR_END),
        IF ("if", new ByteList(new byte[] {'i', 'f'}, USASCII_ENCODING), RubyParser.keyword_if, RubyParser.modifier_if, EXPR_BEG),
        DEFINED_P ("defined?", new ByteList(new byte[] {'d', 'e', 'f', 'i', 'n', 'e', 'd', '?'}, USASCII_ENCODING), RubyParser.keyword_defined, RubyParser.keyword_defined, EXPR_ARG),
        SUPER ("super", new ByteList(new byte[] {'s', 'u', 'p', 'e', 'r'}, USASCII_ENCODING), RubyParser.keyword_super, RubyParser.keyword_super, EXPR_ARG),
        UNDEF ("undef",   new ByteList(new byte[] {'u', 'n', 'd', 'e', 'f'}, USASCII_ENCODING), RubyParser.keyword_undef, RubyParser.keyword_undef, EXPR_FNAME|EXPR_FITEM),
        BREAK ("break", new ByteList(new byte[] {'b', 'r', 'e', 'a', 'k'}, USASCII_ENCODING), RubyParser.keyword_break, RubyParser.keyword_break, EXPR_MID),
        IN ("in", new ByteList(new byte[] {'i', 'n'}, USASCII_ENCODING), RubyParser.keyword_in, RubyParser.keyword_in, EXPR_BEG),
        DO ("do", new ByteList(new byte[] {'d', 'o'}, USASCII_ENCODING), RubyParser.keyword_do, RubyParser.keyword_do, EXPR_BEG),
        NIL ("nil", new ByteList(new byte[] {'n', 'i', 'l'}, USASCII_ENCODING), RubyParser.keyword_nil, RubyParser.keyword_nil, EXPR_END),
        UNTIL ("until", new ByteList(new byte[] {'u', 'n', 't', 'i', 'l'}, USASCII_ENCODING), RubyParser.keyword_until, RubyParser.modifier_until, EXPR_BEG),
        UNLESS ("unless", new ByteList(new byte[] {'u', 'n', 'l', 'e', 's', 's'}, USASCII_ENCODING), RubyParser.keyword_unless, RubyParser.modifier_unless, EXPR_BEG),
        OR ("or", new ByteList(new byte[] {'o', 'r'}, USASCII_ENCODING), RubyParser.keyword_or, RubyParser.keyword_or, EXPR_BEG),
        NEXT ("next", new ByteList(new byte[] {'n', 'e', 'x', 't'}, USASCII_ENCODING), RubyParser.keyword_next, RubyParser.keyword_next, EXPR_MID),
        WHEN ("when", new ByteList(new byte[] {'w', 'h', 'e', 'n'}, USASCII_ENCODING), RubyParser.keyword_when, RubyParser.keyword_when, EXPR_BEG),
        REDO ("redo", new ByteList(new byte[] {'r', 'e', 'd', 'o'}, USASCII_ENCODING), RubyParser.keyword_redo, RubyParser.keyword_redo, EXPR_END),
        AND ("and", new ByteList(new byte[] {'a', 'n', 'd'}, USASCII_ENCODING), RubyParser.keyword_and, RubyParser.keyword_and, EXPR_BEG),
        BEGIN ("begin", new ByteList(new byte[] {'b', 'e', 'g', 'i', 'n'}, USASCII_ENCODING), RubyParser.keyword_begin, RubyParser.keyword_begin, EXPR_BEG),
        __LINE__ ("__LINE__", new ByteList(new byte[] {'_', '_', 'L', 'I', 'N', 'E', '_', '_'}, USASCII_ENCODING), RubyParser.keyword__LINE__, RubyParser.keyword__LINE__, EXPR_END),
        CLASS ("class", new ByteList(new byte[] {'c', 'l', 'a', 's', 's'}, USASCII_ENCODING), RubyParser.keyword_class, RubyParser.keyword_class, EXPR_CLASS),
        __FILE__("__FILE__", new ByteList(new byte[] {'_', '_', 'F', 'I', 'L', 'E', '_', '_'}, USASCII_ENCODING), RubyParser.keyword__FILE__, RubyParser.keyword__FILE__, EXPR_END),
        LEND ("END", new ByteList(new byte[] {'E', 'N', 'D'}, USASCII_ENCODING), RubyParser.keyword_END, RubyParser.keyword_END, EXPR_END),
        LBEGIN ("BEGIN", new ByteList(new byte[] {'B', 'E', 'G', 'I', 'N'}, USASCII_ENCODING), RubyParser.keyword_BEGIN, RubyParser.keyword_BEGIN, EXPR_END),
        WHILE ("while", new ByteList(new byte[] {'w', 'h', 'i', 'l', 'e'}, USASCII_ENCODING), RubyParser.keyword_while, RubyParser.modifier_while, EXPR_BEG),
        ALIAS ("alias", new ByteList(new byte[] {'a', 'l', 'i', 'a', 's'}, USASCII_ENCODING), RubyParser.keyword_alias, RubyParser.keyword_alias, EXPR_FNAME|EXPR_FITEM),
        __ENCODING__("__ENCODING__", new ByteList(new byte[] {'_', '_', 'E', 'N', 'C', 'O', 'D', 'I', 'N', 'G', '_', '_'}, USASCII_ENCODING), RubyParser.keyword__ENCODING__, RubyParser.keyword__ENCODING__, EXPR_END);
        
        public final String name;
        public final ByteList bytes;
        public final int id0;
        public final int id1;
        public final int state;
        
        Keyword(String name, ByteList bytes, int id0, int id1, int state) {
            this.name = name;
            this.bytes = bytes;
            this.id0 = id0;
            this.id1 = id1;
            this.state = state;
        }
    }

    private static final Map<ByteList, Keyword> byteList2Keyword;

    static {
        byteList2Keyword = new HashMap<ByteList, Keyword>();

        byteList2Keyword.put(Keyword.END.bytes, Keyword.END);
        byteList2Keyword.put(Keyword.ELSE.bytes, Keyword.ELSE);
        byteList2Keyword.put(Keyword.CASE.bytes, Keyword.CASE);
        byteList2Keyword.put(Keyword.ENSURE.bytes, Keyword.ENSURE);
        byteList2Keyword.put(Keyword.MODULE.bytes, Keyword.MODULE);
        byteList2Keyword.put(Keyword.ELSIF.bytes, Keyword.ELSIF);
        byteList2Keyword.put(Keyword.DEF.bytes, Keyword.DEF);
        byteList2Keyword.put(Keyword.RESCUE.bytes, Keyword.RESCUE);
        byteList2Keyword.put(Keyword.NOT.bytes, Keyword.NOT);
        byteList2Keyword.put(Keyword.THEN.bytes, Keyword.THEN);
        byteList2Keyword.put(Keyword.YIELD.bytes, Keyword.YIELD);
        byteList2Keyword.put(Keyword.FOR.bytes, Keyword.FOR);
        byteList2Keyword.put(Keyword.SELF.bytes, Keyword.SELF);
        byteList2Keyword.put(Keyword.FALSE.bytes, Keyword.FALSE);
        byteList2Keyword.put(Keyword.RETRY.bytes, Keyword.RETRY);
        byteList2Keyword.put(Keyword.RETURN.bytes, Keyword.RETURN);
        byteList2Keyword.put(Keyword.TRUE.bytes, Keyword.TRUE);
        byteList2Keyword.put(Keyword.IF.bytes, Keyword.IF);
        byteList2Keyword.put(Keyword.DEFINED_P.bytes, Keyword.DEFINED_P);
        byteList2Keyword.put(Keyword.SUPER.bytes, Keyword.SUPER);
        byteList2Keyword.put(Keyword.UNDEF.bytes, Keyword.UNDEF);
        byteList2Keyword.put(Keyword.BREAK.bytes, Keyword.BREAK);
        byteList2Keyword.put(Keyword.IN.bytes, Keyword.IN);
        byteList2Keyword.put(Keyword.DO.bytes, Keyword.DO);
        byteList2Keyword.put(Keyword.NIL.bytes, Keyword.NIL);
        byteList2Keyword.put(Keyword.UNTIL.bytes, Keyword.UNTIL);
        byteList2Keyword.put(Keyword.UNLESS.bytes, Keyword.UNLESS);
        byteList2Keyword.put(Keyword.OR.bytes, Keyword.OR);
        byteList2Keyword.put(Keyword.NEXT.bytes, Keyword.NEXT);
        byteList2Keyword.put(Keyword.WHEN.bytes, Keyword.WHEN);
        byteList2Keyword.put(Keyword.REDO.bytes, Keyword.REDO);
        byteList2Keyword.put(Keyword.AND.bytes, Keyword.AND);
        byteList2Keyword.put(Keyword.BEGIN.bytes, Keyword.BEGIN);
        byteList2Keyword.put(Keyword.__LINE__.bytes, Keyword.__LINE__);
        byteList2Keyword.put(Keyword.CLASS.bytes, Keyword.CLASS);
        byteList2Keyword.put(Keyword.__FILE__.bytes, Keyword.__FILE__);
        byteList2Keyword.put(Keyword.LEND.bytes, Keyword.LEND);
        byteList2Keyword.put(Keyword.LBEGIN.bytes, Keyword.LBEGIN);
        byteList2Keyword.put(Keyword.WHILE.bytes, Keyword.WHILE);
        byteList2Keyword.put(Keyword.ALIAS.bytes, Keyword.ALIAS);
        byteList2Keyword.put(Keyword.__ENCODING__.bytes, Keyword.__ENCODING__);
    }

    public static Keyword getKeyword(ByteList str) {
        return (Keyword) byteList2Keyword.get(str);
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
        ByteList value = createTokenByteList();
        Ruby runtime = parserSupport.getConfiguration().getRuntime();
        String id = runtime.newSymbol(value).idString();

        if (isLexState(last_state, EXPR_DOT|EXPR_FNAME) && parserSupport.getCurrentScope().isDefined(id) >= 0) {
            setState(EXPR_END);
        }

        yaccValue = value;
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

    public void heredoc_dedent(Node root) {
        int indent = heredoc_indent;

        if (indent <= 0 || root == null) return;

        if (root instanceof StrNode) {
            StrNode str = (StrNode) root;
            dedent_string(str.getValue(), indent);
        } else if (root instanceof ListNode) {
            ListNode list = (ListNode) root;
            int length = list.size();
            int currentLine = -1;
            for (int i = 0; i < length; i++) {
                Node child = list.get(i);
                if (currentLine == child.getLine()) continue;  // Only process first element on a line?

                currentLine = child.getLine();                 // New line

                if (child instanceof StrNode) {
                    dedent_string(((StrNode) child).getValue(), indent);
                }
            }
        }
    }

    public void compile_error(String message) {
        throw new SyntaxException(PID.BAD_HEX_NUMBER, getFile(), ruby_sourceline, lexb.toString(), message, lex_p);
    }

    // FIXME: How does lexb.toString() vs getCurrentLine() differ.
    public void compile_error(PID pid, String message) {
        String src = createAsEncodedString(lex_lastline.unsafeBytes(), lex_lastline.begin(), lex_lastline.length(), getEncoding());
        throw new SyntaxException(pid, getFile(), ruby_sourceline, src, message, lex_p);
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

    @Override
    protected RegexpOptions parseRegexpFlags() throws IOException {
        StringBuilder unknownFlags = new StringBuilder(10);
        RegexpOptions options = parseRegexpFlags(unknownFlags);
        if (unknownFlags.length() != 0) {
            compile_error(PID.REGEXP_UNKNOWN_OPTION, "unknown regexp option" +
                    (unknownFlags.length() > 1 ? "s" : "") + " - " + unknownFlags);
        }
        return options;
    }

    @Override
    protected void mismatchedRegexpEncodingError(Encoding optionEncoding, Encoding encoding) {
        compile_error(PID.REGEXP_ENCODING_MISMATCH, "regexp encoding option '" + optionsEncodingChar(optionEncoding) +
                "' differs from source encoding '" + encoding + "'");
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
        Ruby runtime = parserSupport.getConfiguration().getRuntime();
        Encoding newEncoding = runtime.getEncodingService().loadEncoding(name);

        if (newEncoding == null) throw runtime.newArgumentError("unknown encoding name: " + name.toString());
        if (!newEncoding.isAsciiCompatible()) throw runtime.newArgumentError(name.toString() + " is not ASCII compatible");

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
        int type;

        if ((suffix & SUFFIX_I) == 0) {
            type = token;
        } else {
            yaccValue = newComplexNode((NumericNode) yaccValue);
            type = RubyParser.tIMAGINARY;
        }

        setState(EXPR_END|EXPR_ENDARG);
        return type;
    }

    private int getFloatToken(String number, int suffix) {
        if ((suffix & SUFFIX_R) != 0) {
            BigDecimal bd = new BigDecimal(number);
            BigDecimal denominator = BigDecimal.ONE.scaleByPowerOfTen(bd.scale());
            BigDecimal numerator = bd.multiply(denominator);

            try {
                yaccValue = new RationalNode(getPosition(), new FixnumNode(getPosition(), numerator.longValueExact()),
                        new FixnumNode(getPosition(), denominator.longValueExact()));
            } catch (ArithmeticException ae) {
                yaccValue = new RationalNode(getPosition(), new BignumNode(getPosition(), numerator.toBigIntegerExact()),
                        new BignumNode(getPosition(), denominator.toBigIntegerExact()));
            }
            return considerComplex(RubyParser.tRATIONAL, suffix);
        }

        double d;
        try {
            d = SafeDoubleParser.parseDouble(number);
        } catch (NumberFormatException e) {
            warnings.warn(ID.FLOAT_OUT_OF_RANGE, getFile(), ruby_sourceline, "Float " + number + " out of range.");

            d = number.startsWith("-") ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }
        yaccValue = new FloatNode(getPosition(), d);
        return considerComplex(RubyParser.tFLOAT, suffix);
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
        return considerComplex(RubyParser.tINTEGER, suffix);
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

        StrNode newStr = new StrNode(getPosition(), buffer, codeRange);

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
            if (Character.isLetterOrDigit(begin) /* no mb || ismbchar(term)*/) compile_error(PID.STRING_UNKNOWN_TYPE, "unknown type of %string");
        }
        if (c == EOF || begin == EOF) compile_error(PID.STRING_HITS_EOF, "unterminated quoted string meets end of file");

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
            return RubyParser.tSTRING_BEG;

        case 'q':
            lex_strterm = new StringTerm(str_squote, begin, end, ruby_sourceline);
            yaccValue = "%"+c+begin;
            return RubyParser.tSTRING_BEG;

        case 'W':
            lex_strterm = new StringTerm(str_dword, begin, end, ruby_sourceline);
            yaccValue = "%"+c+begin;
            return RubyParser.tWORDS_BEG;

        case 'w':
            lex_strterm = new StringTerm(str_sword, begin, end, ruby_sourceline);
            yaccValue = "%"+c+begin;
            return RubyParser.tQWORDS_BEG;

        case 'x':
            lex_strterm = new StringTerm(str_xquote, begin, end, ruby_sourceline);
            yaccValue = "%"+c+begin;
            return RubyParser.tXSTRING_BEG;

        case 'r':
            lex_strterm = new StringTerm(str_regexp, begin, end, ruby_sourceline);
            yaccValue = "%"+c+begin;
            return RubyParser.tREGEXP_BEG;

        case 's':
            lex_strterm = new StringTerm(str_ssym, begin, end, ruby_sourceline);
            setState(EXPR_FNAME|EXPR_FITEM);
            yaccValue = "%"+c+begin;
            return RubyParser.tSYMBEG;
        
        case 'I':
            lex_strterm = new StringTerm(str_dword, begin, end, ruby_sourceline);
            yaccValue = "%" + c + begin;
            return RubyParser.tSYMBOLS_BEG;
        case 'i':
            lex_strterm = new StringTerm(str_sword, begin, end, ruby_sourceline);
            yaccValue = "%" + c + begin;
            return RubyParser.tQSYMBOLS_BEG;
        default:
            compile_error(PID.STRING_UNKNOWN_TYPE, "unknown type of %string");
        }
        return -1; // not-reached
    }
    
    private int hereDocumentIdentifier() throws IOException {
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
            yaccValue = BACKTICK;
            flush();
            return RubyParser.tXSTRING_BEG;
        }
        
        yaccValue = QQ;
        heredoc_indent = indent;
        heredoc_line_indent = 0;
        flush();
        return RubyParser.tSTRING_BEG;
    }
    
    private boolean arg_ambiguous() {
        if (warnings.isVerbose() && Options.PARSER_WARN_AMBIGUOUS_ARGUMENTS.load()) {
            warnings.warning(ID.AMBIGUOUS_ARGUMENT, getFile(), ruby_sourceline, "Ambiguous first argument; make sure.");
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
            case RubyParser.yyErrorCode: System.err.print("yyErrorCode,"); break;
            // MISSING tokens
            case RubyParser.tIDENTIFIER: System.err.print("tIDENTIFIER["+ value() + "],"); break;
            case RubyParser.tFID: System.err.print("tFID[" + value() + "],"); break;
            case RubyParser.tGVAR: System.err.print("tGVAR[" + value() + "],"); break;
            case RubyParser.tIVAR: System.err.print("tIVAR[" + value() +"],"); break;
            case RubyParser.tCONSTANT: System.err.print("tCONSTANT["+ value() +"],"); break;
            case RubyParser.tCVAR: System.err.print("tCVAR,"); break;
            case RubyParser.tINTEGER: System.err.print("tINTEGER,"); break;
            case RubyParser.tFLOAT: System.err.print("tFLOAT,"); break;
            case RubyParser.tSTRING_CONTENT: System.err.print("tSTRING_CONTENT[" + ((StrNode) value()).getValue() + "],"); break;
            case RubyParser.tSTRING_BEG: System.err.print("tSTRING_BEG,"); break;
            case RubyParser.tSTRING_END: System.err.print("tSTRING_END,"); break;
            case RubyParser.tSTRING_DBEG: System.err.print("tSTRING_DBEG,"); break;
            case RubyParser.tSTRING_DVAR: System.err.print("tSTRING_DVAR,"); break;
            case RubyParser.tXSTRING_BEG: System.err.print("tXSTRING_BEG,"); break;
            case RubyParser.tREGEXP_BEG: System.err.print("tREGEXP_BEG,"); break;
            case RubyParser.tREGEXP_END: System.err.print("tREGEXP_END,"); break;
            case RubyParser.tWORDS_BEG: System.err.print("tWORDS_BEG,"); break;
            case RubyParser.tQWORDS_BEG: System.err.print("tQWORDS_BEG,"); break;
            case RubyParser.tBACK_REF: System.err.print("tBACK_REF,"); break;
            case RubyParser.tBACK_REF2: System.err.print("tBACK_REF2,"); break;
            case RubyParser.tNTH_REF: System.err.print("tNTH_REF,"); break;
            case RubyParser.tUPLUS: System.err.print("tUPLUS"); break;
            case RubyParser.tUMINUS: System.err.print("tUMINUS,"); break;
            case RubyParser.tPOW: System.err.print("tPOW,"); break;
            case RubyParser.tCMP: System.err.print("tCMP,"); break;
            case RubyParser.tEQ: System.err.print("tEQ,"); break;
            case RubyParser.tEQQ: System.err.print("tEQQ,"); break;
            case RubyParser.tNEQ: System.err.print("tNEQ,"); break;
            case RubyParser.tGEQ: System.err.print("tGEQ,"); break;
            case RubyParser.tLEQ: System.err.print("tLEQ,"); break;
            case RubyParser.tANDOP: System.err.print("tANDOP,"); break;
            case RubyParser.tOROP: System.err.print("tOROP,"); break;
            case RubyParser.tMATCH: System.err.print("tMATCH,"); break;
            case RubyParser.tNMATCH: System.err.print("tNMATCH,"); break;
            case RubyParser.tDOT: System.err.print("tDOT,"); break;
            case RubyParser.tDOT2: System.err.print("tDOT2,"); break;
            case RubyParser.tDOT3: System.err.print("tDOT3,"); break;
            case RubyParser.tAREF: System.err.print("tAREF,"); break;
            case RubyParser.tASET: System.err.print("tASET,"); break;
            case RubyParser.tLSHFT: System.err.print("tLSHFT,"); break;
            case RubyParser.tRSHFT: System.err.print("tRSHFT,"); break;
            case RubyParser.tCOLON2: System.err.print("tCOLON2,"); break;
            case RubyParser.tCOLON3: System.err.print("tCOLON3,"); break;
            case RubyParser.tOP_ASGN: System.err.print("tOP_ASGN,"); break;
            case RubyParser.tASSOC: System.err.print("tASSOC,"); break;
            case RubyParser.tLPAREN: System.err.print("tLPAREN,"); break;
            case RubyParser.tLPAREN2: System.err.print("tLPAREN2,"); break;
            case RubyParser.tLPAREN_ARG: System.err.print("tLPAREN_ARG,"); break;
            case RubyParser.tLBRACK: System.err.print("tLBRACK,"); break;
            case RubyParser.tRBRACK: System.err.print("tRBRACK,"); break;
            case RubyParser.tLBRACE: System.err.print("tLBRACE,"); break;
            case RubyParser.tLBRACE_ARG: System.err.print("tLBRACE_ARG,"); break;
            case RubyParser.tSTAR: System.err.print("tSTAR,"); break;
            case RubyParser.tSTAR2: System.err.print("tSTAR2,"); break;
            case RubyParser.tAMPER: System.err.print("tAMPER,"); break;
            case RubyParser.tAMPER2: System.err.print("tAMPER2,"); break;
            case RubyParser.tSYMBEG: System.err.print("tSYMBEG,"); break;
            case RubyParser.tTILDE: System.err.print("tTILDE,"); break;
            case RubyParser.tPERCENT: System.err.print("tPERCENT,"); break;
            case RubyParser.tDIVIDE: System.err.print("tDIVIDE,"); break;
            case RubyParser.tPLUS: System.err.print("tPLUS,"); break;
            case RubyParser.tMINUS: System.err.print("tMINUS,"); break;
            case RubyParser.tLT: System.err.print("tLT,"); break;
            case RubyParser.tGT: System.err.print("tGT,"); break;
            case RubyParser.tCARET: System.err.print("tCARET,"); break;
            case RubyParser.tBANG: System.err.print("tBANG,"); break;
            case RubyParser.tLCURLY: System.err.print("tTLCURLY,"); break;
            case RubyParser.tRCURLY: System.err.print("tRCURLY,"); break;
            case RubyParser.tPIPE: System.err.print("tTPIPE,"); break;
            case RubyParser.tLAMBDA: System.err.print("tLAMBDA,"); break;
            case RubyParser.tLAMBEG: System.err.print("tLAMBEG,"); break;
            case RubyParser.tRPAREN: System.err.print("tRPAREN,"); break;
            case RubyParser.tLABEL: System.err.print("tLABEL("+ value() +":),"); break;
            case RubyParser.tLABEL_END: System.err.print("tLABEL_END"); break;
            case RubyParser.keyword_def: System.err.print("keyword_def,"); break;
            case RubyParser.keyword_do: System.err.print("keyword_do,"); break;
            case RubyParser.keyword_do_block: System.err.print("keyword_do_block,"); break;
            case RubyParser.keyword_do_cond: System.err.print("keyword_do_cond,"); break;
            case RubyParser.keyword_do_lambda: System.err.print("keyword_do_lambda,"); break;
            case RubyParser.keyword_end: System.err.print("keyword_end,"); break;
            case RubyParser.keyword_yield: System.err.print("keyword_yield,"); break;
            case '\n': System.err.println("NL"); break;
            case EOF: System.out.println("EOF"); break;
            case RubyParser.tDSTAR: System.err.print("tDSTAR"); break;
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
        boolean tokenSeen = this.tokenSeen;
        
        if (lex_strterm != null) return lex_strterm.parseString(this);

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
                if (!tokenSeen || warnings.isVerbose()) {
                    if (!parser_magic_comment(lexb.makeShared(lex_p, lex_pend - lex_p))) {
                        if (comment_at_top()) set_file_encoding(lex_p, lex_pend);
                    }
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
                        yaccValue = EQ_EQ_EQ;
                        return RubyParser.tEQQ;
                    }
                    pushback(c);
                    yaccValue = EQ_EQ;
                    return RubyParser.tEQ;
                }
                if (c == '~') {
                    yaccValue = EQ_TILDE;
                    return RubyParser.tMATCH;
                } else if (c == '>') {
                    yaccValue = EQ_GT;
                    return RubyParser.tASSOC;
                }
                pushback(c);
                yaccValue = EQ;
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
                yaccValue = SEMICOLON;
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
                yaccValue = BACKSLASH;
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

    private int identifierToken(int result, ByteList value) {
        Ruby runtime = parserSupport.getConfiguration().getRuntime();
        String id = runtime.newSymbol(value).idString();

        if (result == RubyParser.tIDENTIFIER && !isLexState(last_state, EXPR_DOT|EXPR_FNAME) &&
                parserSupport.getCurrentScope().isDefined(id) >= 0) {
            setState(EXPR_END|EXPR_LABEL);
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
                yaccValue = AMPERSAND_AMPERSAND;
                setState(EXPR_BEG);
                return RubyParser.tOP_ASGN;
            }
            pushback(c);
            yaccValue = AMPERSAND_AMPERSAND;
            return RubyParser.tANDOP;
        case '=':
            yaccValue = AMPERSAND;
            setState(EXPR_BEG);
            return RubyParser.tOP_ASGN;
        case '.':
            setState(EXPR_DOT);
            yaccValue = AMPERSAND_DOT;
            return RubyParser.tANDDOT;
        }
        pushback(c);
        
        //tmpPosition is required because getPosition()'s side effects.
        //if the warning is generated, the getPosition() on line 954 (this line + 18) will create
        //a wrong position if the "inclusive" flag is not set.
        ISourcePosition tmpPosition = getPosition();
        if (isSpaceArg(c, spaceSeen)) {
            if (warnings.isVerbose() && Options.PARSER_WARN_ARGUMENT_PREFIX.load())
                warnings.warning(ID.ARGUMENT_AS_PREFIX, getFile(), tmpPosition.getLine(), "`&' interpreted as argument prefix");
            c = RubyParser.tAMPER;
        } else if (isBEG()) {
            c = RubyParser.tAMPER;
        } else {
            warn_balanced(c, spaceSeen, "&", "argument prefix");
            c = RubyParser.tAMPER2;
        }

        setState(isAfterOperator() ? EXPR_ARG : EXPR_BEG);
        
        yaccValue = AMPERSAND;
        return c;
    }

    private int at() throws IOException {
        newtok(true);
        int c = nextc();
        int result;
        if (c == '@') {
            c = nextc();
            result = RubyParser.tCVAR;
        } else {
            result = RubyParser.tIVAR;
        }

        if (c == EOF || isSpace(c)) {
            if (result == RubyParser.tIVAR) {
                compile_error("`@' without identifiers is not allowed as an instance variable name");
            }

            compile_error("`@@' without identifiers is not allowed as a class variable name");
        } else if (Character.isDigit(c) || !isIdentifierChar(c)) {
            pushback(c);
            if (result == RubyParser.tIVAR) {
                compile_error(PID.IVAR_BAD_NAME, "`@" + ((char) c) + "' is not allowed as an instance variable name");
            }
            compile_error(PID.CVAR_BAD_NAME, "`@@" + ((char) c) + "' is not allowed as a class variable name");
        }

        if (!tokadd_ident(c)) return EOF;

        last_state = lex_state;
        setState(EXPR_END);

        return tokenize_ident(result);
    }

    private int backtick(boolean commandState) throws IOException {
        yaccValue = BACKTICK;

        if (isLexState(lex_state, EXPR_FNAME)) {
            setState(EXPR_ENDFN);
            return RubyParser.tBACK_REF2;
        }
        if (isLexState(lex_state, EXPR_DOT)) {
            setState(commandState ? EXPR_CMDARG : EXPR_ARG);

            return RubyParser.tBACK_REF2;
        }

        lex_strterm = new StringTerm(str_xquote, '\0', '`', ruby_sourceline);
        return RubyParser.tXSTRING_BEG;
    }
    
    private int bang() throws IOException {
        int c = nextc();

        if (isAfterOperator()) {
            setState(EXPR_ARG);
            if (c == '@') {
                yaccValue = BANG;
                return RubyParser.tBANG;
            }
        } else {
            setState(EXPR_BEG);
        }
        
        switch (c) {
        case '=':
            yaccValue = BANG_EQ;
            
            return RubyParser.tNEQ;
        case '~':
            yaccValue = BANG_TILDE;
            
            return RubyParser.tNMATCH;
        default: // Just a plain bang
            pushback(c);
            yaccValue = BANG;
            
            return RubyParser.tBANG;
        }
    }
    
    private int caret() throws IOException {
        int c = nextc();
        if (c == '=') {
            setState(EXPR_BEG);
            yaccValue = CARET;
            return RubyParser.tOP_ASGN;
        }

        setState(isAfterOperator() ? EXPR_ARG : EXPR_BEG);
        
        pushback(c);
        yaccValue = CARET;
        return RubyParser.tCARET;
    }

    private int colon(boolean spaceSeen) throws IOException {
        int c = nextc();
        
        if (c == ':') {
            if (isBEG() || isLexState(lex_state, EXPR_CLASS) || (isARG() && spaceSeen)) {
                setState(EXPR_BEG);
                yaccValue = COLON_COLON;
                return RubyParser.tCOLON3;
            }
            setState(EXPR_DOT);
            yaccValue = COLON_COLON;
            return RubyParser.tCOLON2;
        }

        if (isEND() || Character.isWhitespace(c) || c == '#') {
            pushback(c);
            setState(EXPR_BEG);
            yaccValue = COLON;
            warn_balanced(c, spaceSeen, ":", "symbol literal");
            return ':';
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
        yaccValue = COLON;
        return RubyParser.tSYMBEG;
    }

    private int comma(int c) throws IOException {
        setState(EXPR_BEG|EXPR_LABEL);
        yaccValue = COMMA;
        
        return c;
    }

    private int doKeyword(int state) {
        int leftParenBegin = getLeftParenBegin();
        if (leftParenBegin > 0 && leftParenBegin == parenNest) {
            setLeftParenBegin(0);
            parenNest--;
            return RubyParser.keyword_do_lambda;
        }

        if (conditionState.isInState()) return RubyParser.keyword_do_cond;

        if (cmdArgumentState.isInState() && !isLexState(state, EXPR_CMDARG)) {
            return RubyParser.keyword_do_block;
        }
        if (isLexState(state,  EXPR_BEG|EXPR_ENDARG)) {
            return RubyParser.keyword_do_block;
        }
        return RubyParser.keyword_do;
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
                yaccValue = createTokenByteList();
                return RubyParser.tGVAR;

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
            yaccValue = new ByteList(new byte[] {'$', (byte) c}, USASCII_ENCODING);
            return RubyParser.tGVAR;


        case '-':
            c = nextc();
            if (isIdentifierChar(c)) {
                if (!tokadd_mbchar(c)) return EOF;
            } else {
                pushback(c);
                pushback('-');
                return '$';
            }
            yaccValue = createTokenByteList();
            /* xxx shouldn't check if valid option variable */
            return RubyParser.tGVAR;

        case '&':       /* $&: last match */
        case '`':       /* $`: string before last match */
        case '\'':      /* $': string after last match */
        case '+':       /* $+: string matches last paren. */
            // Explicit reference to these vars as symbols...
            if (isLexState(last_state, EXPR_FNAME)) {
                yaccValue = new ByteList(new byte[] {'$', (byte) c}, USASCII_ENCODING);
                return RubyParser.tGVAR;
            }
            
            yaccValue = new BackRefNode(getPosition(), c);
            return RubyParser.tBACK_REF;

        case '1': case '2': case '3': case '4': case '5': case '6':
        case '7': case '8': case '9':
            do {
                c = nextc();
            } while (Character.isDigit(c));
            pushback(c);
            if (isLexState(last_state, EXPR_FNAME)) {
                yaccValue = createTokenByteList();
                return RubyParser.tGVAR;
            }

            int ref;
            String refAsString = createTokenString();

            try {
                ref = Integer.parseInt(refAsString.substring(1));
            } catch (NumberFormatException e) {
                warnings.warn(ID.AMBIGUOUS_ARGUMENT, "`" + refAsString + "' is too big for a number variable, always nil");
                ref = 0;
            }

            yaccValue = new NthRefNode(getPosition(), ref);
            return RubyParser.tNTH_REF;
        case '0':
            return identifierToken(RubyParser.tGVAR, new ByteList(new byte[] {'$', (byte) c}));
        default:
            if (!isIdentifierChar(c)) {
                if (c == EOF || isSpace(c)) {
                    compile_error(PID.CVAR_BAD_NAME, "`$' without identifiers is not allowed as a global variable name");
                } else {
                    pushback(c);
                    compile_error(PID.CVAR_BAD_NAME, "`$" + ((char) c) + "' is not allowed as a global variable name");
                }
            }

            last_state = lex_state;
            setState(EXPR_END);

            tokadd_ident(c);

            return identifierToken(RubyParser.tGVAR, createTokenByteList());  // $blah
        }
    }

    private int dot() throws IOException {
        int c;
        
        setState(EXPR_BEG);
        if ((c = nextc()) == '.') {
            if ((c = nextc()) == '.') {
                yaccValue = DOT_DOT_DOT;
                return RubyParser.tDOT3;
            }
            pushback(c);
            yaccValue = DOT_DOT;
            return RubyParser.tDOT2;
        }
        
        pushback(c);
        if (Character.isDigit(c)) compile_error(PID.FLOAT_MISSING_ZERO, "no .<digit> floating literal anymore; put 0 before dot");

        setState(EXPR_DOT);
        yaccValue = DOT;
        return RubyParser.tDOT;
    }
    
    private int doubleQuote(boolean commandState) throws IOException {
        int label = isLabelPossible(commandState) ? str_label : 0;
        lex_strterm = new StringTerm(str_dquote|label, '\0', '"', ruby_sourceline);
        yaccValue = QQ;

        return RubyParser.tSTRING_BEG;
    }
    
    private int greaterThan() throws IOException {
        setState(isAfterOperator() ? EXPR_ARG : EXPR_BEG);

        int c = nextc();

        switch (c) {
        case '=':
            yaccValue = GT_EQ;
            
            return RubyParser.tGEQ;
        case '>':
            if ((c = nextc()) == '=') {
                setState(EXPR_BEG);
                yaccValue = GT_GT;
                return RubyParser.tOP_ASGN;
            }
            pushback(c);
            
            yaccValue = GT_GT;
            return RubyParser.tRSHFT;
        default:
            pushback(c);
            yaccValue = GT;
            return RubyParser.tGT;
        }
    }
    
    private int identifier(int c, boolean commandState) throws IOException {
        if (!isIdentifierChar(c)) {
            String badChar = "\\" + Integer.toOctalString(c & 0xff);
            compile_error(PID.CHARACTER_BAD, "Invalid char `" + badChar + "' ('" + (char) c + "') in expression");
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
        ByteList tempVal;
        if (lastBangOrPredicate) {
            result = RubyParser.tFID;
            tempVal = createTokenByteList();
        } else {
            if (isLexState(lex_state, EXPR_FNAME)) {
                if ((c = nextc()) == '=') { 
                    int c2 = nextc();

                    if (c2 != '~' && c2 != '>' &&
                            (c2 != '=' || peek('>'))) {
                        result = RubyParser.tIDENTIFIER;
                        pushback(c2);
                    } else {
                        pushback(c2);
                        pushback(c);
                    }
                } else {
                    pushback(c);
                }
            }
            tempVal = createTokenByteList();

            if (result == 0 && Character.isUpperCase(StringSupport.preciseCodePoint(getEncoding(), tempVal.unsafeBytes(), tempVal.begin(), tempVal.begin() + 1))) {
                result = RubyParser.tCONSTANT;
            } else {
                result = RubyParser.tIDENTIFIER;
            }
        }
        
        if (isLabelPossible(commandState)) {
            if (isLabelSuffix()) {
                setState(EXPR_ARG|EXPR_LABELED);
                nextc();
                yaccValue = tempVal;
                return RubyParser.tLABEL;
            }
        }

        if (lex_state != EXPR_DOT) {
            Keyword keyword = getKeyword(tempVal); // Is it is a keyword?

            if (keyword != null) {
                int state = lex_state; // Save state at time keyword is encountered
                setState(keyword.state);

                if (isLexState(state, EXPR_FNAME)) {
                    yaccValue = keyword.bytes;
                    return keyword.id0;
                } else {
                    yaccValue = getPosition();
                }

                if (isLexState(lex_state, EXPR_BEG)) commandStart = true;

                if (keyword.id0 == RubyParser.keyword_do) return doKeyword(state);

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

        return identifierToken(result, tempVal);
    }

    private int leftBracket(boolean spaceSeen) throws IOException {
        parenNest++;
        int c = '[';
        if (isAfterOperator()) {
            if ((c = nextc()) == ']') {
                setState(EXPR_ARG);
                if (peek('=')) {
                    nextc();
                    yaccValue = LBRACKET_RBRACKET_EQ;
                    return RubyParser.tASET;
                }
                yaccValue = LBRACKET_RBRACKET;
                return RubyParser.tAREF;
            }
            pushback(c);
            setState(EXPR_ARG|EXPR_LABEL);
            yaccValue = LBRACKET;
            return '[';
        } else if (isBEG() || (isARG() && (spaceSeen || isLexState(lex_state, EXPR_LABELED)))) {
            c = RubyParser.tLBRACK;
        }

        setState(EXPR_BEG|EXPR_LABEL);
        conditionState.stop();
        cmdArgumentState.stop();
        yaccValue = LBRACKET;
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
            yaccValue = LCURLY;
            return RubyParser.tLAMBEG;
        }

        char c;
        if (isLexState(lex_state, EXPR_LABELED)) {
            c = RubyParser.tLBRACE;
        } else if (isLexState(lex_state, EXPR_ARG_ANY|EXPR_END|EXPR_ENDFN)) { // block (primary)
            c = RubyParser.tLCURLY;
        } else if (isLexState(lex_state, EXPR_ENDARG)) { // block (expr)
            c = RubyParser.tLBRACE_ARG;
        } else { // hash
            c = RubyParser.tLBRACE;
        }

        conditionState.stop();
        cmdArgumentState.stop();
        setState(c == RubyParser.tLBRACE_ARG ? EXPR_BEG : EXPR_BEG|EXPR_LABEL);
        if (c != RubyParser.tLBRACE) commandStart = true;
        yaccValue = getPosition();

        return c;
    }

    private int leftParen(boolean spaceSeen) throws IOException {
        int result;

        if (isBEG()) {
            result = RubyParser.tLPAREN;
        } else if (isSpaceArg('(', spaceSeen)) {
            result = RubyParser.tLPAREN_ARG;
        } else {
            result = RubyParser.tLPAREN2;
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
                yaccValue = LT_EQ_RT;
                return RubyParser.tCMP;
            }
            pushback(c);
            yaccValue = LT_EQ;
            return RubyParser.tLEQ;
        case '<':
            if ((c = nextc()) == '=') {
                setState(EXPR_BEG);
                yaccValue = LT_LT;
                return RubyParser.tOP_ASGN;
            }
            pushback(c);
            yaccValue = LT_LT;
            warn_balanced(c, spaceSeen, "<<", "here document");
            return RubyParser.tLSHFT;
        default:
            yaccValue = LT;
            pushback(c);
            return RubyParser.tLT;
        }
    }
    
    private int minus(boolean spaceSeen) throws IOException {
        int c = nextc();
        
        if (isAfterOperator()) {
            setState(EXPR_ARG);
            if (c == '@') {
                yaccValue = MINUS_AT;
                return RubyParser.tUMINUS;
            }
            pushback(c);
            yaccValue = MINUS;
            return RubyParser.tMINUS;
        }
        if (c == '=') {
            setState(EXPR_BEG);
            yaccValue = MINUS;
            return RubyParser.tOP_ASGN;
        }
        if (c == '>') {
            setState(EXPR_ENDFN);
            yaccValue = MINUS_GT;
            return RubyParser.tLAMBDA;
        }
        if (isBEG() || (isSpaceArg(c, spaceSeen) && arg_ambiguous())) {
            setState(EXPR_BEG);
            pushback(c);
            yaccValue = MINUS_AT;
            if (Character.isDigit(c)) {
                return RubyParser.tUMINUS_NUM;
            }
            return RubyParser.tUMINUS;
        }
        setState(EXPR_BEG);
        pushback(c);
        yaccValue = MINUS;
        warn_balanced(c, spaceSeen, "-", "unary operator");
        return RubyParser.tMINUS;
    }

    private int percent(boolean spaceSeen) throws IOException {
        if (isBEG()) return parseQuote(nextc());

        int c = nextc();

        if (c == '=') {
            setState(EXPR_BEG);
            yaccValue = PERCENT;
            return RubyParser.tOP_ASGN;
        }

        if (isSpaceArg(c, spaceSeen) || (isLexState(lex_state, EXPR_FITEM) && c == 's')) return parseQuote(c);

        setState(isAfterOperator() ? EXPR_ARG : EXPR_BEG);
        
        pushback(c);
        yaccValue = PERCENT;
        warn_balanced(c, spaceSeen, "%", "string literal");
        return RubyParser.tPERCENT;
    }

    private int pipe() throws IOException {
        int c = nextc();
        
        switch (c) {
        case '|':
            setState(EXPR_BEG);
            if ((c = nextc()) == '=') {
                setState(EXPR_BEG);
                yaccValue = OR_OR;
                return RubyParser.tOP_ASGN;
            }
            pushback(c);
            yaccValue = OR_OR;
            return RubyParser.tOROP;
        case '=':
            setState(EXPR_BEG);
            yaccValue = OR;
            return RubyParser.tOP_ASGN;
        default:
            setState(isAfterOperator() ? EXPR_ARG : EXPR_BEG|EXPR_LABEL);
            
            pushback(c);
            yaccValue = OR;
            return RubyParser.tPIPE;
        }
    }
    
    private int plus(boolean spaceSeen) throws IOException {
        int c = nextc();
        if (isAfterOperator()) {
            setState(EXPR_ARG);
            if (c == '@') {
                yaccValue = PLUS_AT;
                return RubyParser.tUPLUS;
            }
            pushback(c);
            yaccValue = PLUS;
            return RubyParser.tPLUS;
        }
        
        if (c == '=') {
            setState(EXPR_BEG);
            yaccValue = PLUS;
            return RubyParser.tOP_ASGN;
        }
        
        if (isBEG() || (isSpaceArg(c, spaceSeen) && arg_ambiguous())) {
            setState(EXPR_BEG);
            pushback(c);
            if (Character.isDigit(c)) {
                c = '+';
                return parseNumber(c);
            }
            yaccValue = PLUS_AT;
            return RubyParser.tUPLUS;
        }

        setState(EXPR_BEG);
        pushback(c);
        yaccValue = PLUS;
        warn_balanced(c, spaceSeen, "+", "unary operator");
        return RubyParser.tPLUS;
    }
    
    private int questionMark() throws IOException {
        int c;
        
        if (isEND()) {
            setState(EXPR_VALUE);
            yaccValue = QUESTION;
            return '?';
        }
        
        c = nextc();
        if (c == EOF) compile_error(PID.INCOMPLETE_CHAR_SYNTAX, "incomplete character syntax");

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
                    warnings.warn(ID.INVALID_CHAR_SEQUENCE, getFile(), ruby_sourceline, "invalid character syntax; use ?\\" + c2);
                }
            }
            pushback(c);
            setState(EXPR_VALUE);
            yaccValue = QUESTION;
            return '?';
        }

        if (!isASCII(c)) {
            if (!tokadd_mbchar(c)) return EOF;
        } else if (isIdentifierChar(c) && !peek('\n') && isNext_identchar()) {
            newtok(true);
            pushback(c);
            setState(EXPR_VALUE);
            yaccValue = QUESTION;
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
                yaccValue = new StrNode(getPosition(), oneCharBL);
                
                return RubyParser.tCHAR;
            } else {
                c = readEscape();
            }
        } else {
            newtok(true);
        }

        ByteList oneCharBL = new ByteList(1);
        oneCharBL.setEncoding(getEncoding());
        oneCharBL.append(c);
        yaccValue = new StrNode(getPosition(), oneCharBL);
        setState(EXPR_END);
        return RubyParser.tCHAR;
    }
    
    private int rightBracket() {
        parenNest--;
        conditionState.restart();
        cmdArgumentState.restart();
        setState(EXPR_END);
        yaccValue = RBRACKET;
        return RubyParser.tRBRACK;
    }

    private int rightCurly() {
        conditionState.restart();
        cmdArgumentState.restart();
        setState(EXPR_END);
        yaccValue = RCURLY;
        int tok = braceNest == 0 ? RubyParser.tSTRING_DEND : RubyParser.tRCURLY;
        braceNest--;
        return tok;
    }

    private int rightParen() {
        parenNest--;
        conditionState.restart();
        cmdArgumentState.restart();
        setState(EXPR_ENDFN);
        yaccValue = RPAREN;
        return RubyParser.tRPAREN;
    }
    
    private int singleQuote(boolean commandState) throws IOException {
        int label = isLabelPossible(commandState) ? str_label : 0;
        lex_strterm = new StringTerm(str_squote|label, '\0', '\'', ruby_sourceline);
        yaccValue = Q;

        return RubyParser.tSTRING_BEG;
    }
    
    private int slash(boolean spaceSeen) throws IOException {
        if (isBEG()) {
            lex_strterm = new StringTerm(str_regexp, '\0', '/', ruby_sourceline);
            yaccValue = SLASH;
            return RubyParser.tREGEXP_BEG;
        }
        
        int c = nextc();
        
        if (c == '=') {
            setState(EXPR_BEG);
            yaccValue = SLASH;
            return RubyParser.tOP_ASGN;
        }
        pushback(c);
        if (isSpaceArg(c, spaceSeen)) {
            arg_ambiguous();
            lex_strterm = new StringTerm(str_regexp, '\0', '/', ruby_sourceline);
            yaccValue = SLASH;
            return RubyParser.tREGEXP_BEG;
        }

        setState(isAfterOperator() ? EXPR_ARG : EXPR_BEG);

        yaccValue = SLASH;
        warn_balanced(c, spaceSeen, "/", "regexp literal");
        return RubyParser.tDIVIDE;
    }

    private int star(boolean spaceSeen) throws IOException {
        int c = nextc();
        
        switch (c) {
        case '*':
            if ((c = nextc()) == '=') {
                setState(EXPR_BEG);
                yaccValue = STAR_STAR;
                return RubyParser.tOP_ASGN;
            }

            pushback(c); // not a '=' put it back
            yaccValue = STAR_STAR;

            if (isSpaceArg(c, spaceSeen)) {
                if (warnings.isVerbose() && Options.PARSER_WARN_ARGUMENT_PREFIX.load())
                    warnings.warning(ID.ARGUMENT_AS_PREFIX, getFile(), ruby_sourceline, "`**' interpreted as argument prefix");
                c = RubyParser.tDSTAR;
            } else if (isBEG()) {
                c = RubyParser.tDSTAR;
            } else {
                warn_balanced(c, spaceSeen, "**", "argument prefix");
                c = RubyParser.tPOW;
            }
            break;
        case '=':
            setState(EXPR_BEG);
            yaccValue = STAR;
            return RubyParser.tOP_ASGN;
        default:
            pushback(c);
            if (isSpaceArg(c, spaceSeen)) {
                if (warnings.isVerbose() && Options.PARSER_WARN_ARGUMENT_PREFIX.load())
                    warnings.warning(ID.ARGUMENT_AS_PREFIX, getFile(), ruby_sourceline, "`*' interpreted as argument prefix");
                c = RubyParser.tSTAR;
            } else if (isBEG()) {
                c = RubyParser.tSTAR;
            } else {
                warn_balanced(c, spaceSeen, "*", "argument prefix");
                c = RubyParser.tSTAR2;
            }
            yaccValue = STAR;
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
        
        yaccValue = TILDE;
        return RubyParser.tTILDE;
    }

    private ByteList numberBuffer = new ByteList(10); // ascii is good enough.
    /**
     *  Parse a number from the input stream.
     *
     *@param c The first character of the number.
     *@return An int constant which represents a token.
     */
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
                        compile_error(PID.BAD_HEX_NUMBER, "Hexadecimal number without hex-digits.");
                    } else if (nondigit != '\0') {
                        compile_error(PID.TRAILING_UNDERSCORE_IN_NUMBER, "Trailing '_' in number.");
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
                        compile_error(PID.EMPTY_BINARY_NUMBER, "Binary number without digits.");
                    } else if (nondigit != '\0') {
                        compile_error(PID.TRAILING_UNDERSCORE_IN_NUMBER, "Trailing '_' in number.");
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
                        compile_error(PID.EMPTY_BINARY_NUMBER, "Binary number without digits.");
                    } else if (nondigit != '\0') {
                        compile_error(PID.TRAILING_UNDERSCORE_IN_NUMBER, "Trailing '_' in number.");
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

                        if (nondigit != '\0') compile_error(PID.TRAILING_UNDERSCORE_IN_NUMBER, "Trailing '_' in number.");

                        return getIntegerToken(numberBuffer.toString(), 8, numberLiteralSuffix(SUFFIX_ALL));
                    }
                case '8' :
                case '9' :
                    compile_error(PID.BAD_OCTAL_DIGIT, "Illegal octal digit.");
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
                        compile_error(PID.TRAILING_UNDERSCORE_IN_NUMBER, "Trailing '_' in number.");
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
                    if (nondigit != 0) {
                        pushback(c);
                        return getNumberToken(numberBuffer.toString(), seen_e, seen_point, nondigit);
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
                    if (nondigit != '\0') compile_error(PID.TRAILING_UNDERSCORE_IN_NUMBER, "Trailing '_' in number.");
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
            compile_error(PID.TRAILING_UNDERSCORE_IN_NUMBER, "Trailing '_' in number.");
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
                    compile_error(PID.INVALID_ESCAPE_SYNTAX, "invalid Unicode codepoint (too large)");
                }
            } while (peek(' ') || peek('\t'));

            int c = nextc();
            if (c != '}') compile_error(PID.INVALID_ESCAPE_SYNTAX,  "unterminated Unicode escape");

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
                    compile_error(PID.INVALID_ESCAPE_SYNTAX,  "invalid Unicode codepoint (too large)");
                }
                if (buffer != null) readUTF8EscapeIntoBuffer(codepoint, buffer, stringLiteral);
            } while (peek(' ') || peek('\t'));

            c = nextc();
            if (c != '}') {
                compile_error(PID.INVALID_ESCAPE_SYNTAX, "unterminated Unicode escape");
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
                return scanHex(2, false, "Invalid hex escape");
            case 'b' : // backspace
                return '\010';
            case 's' : // space
                return ' ';
            case 'M' :
                if ((c = nextc()) != '-') {
                    compile_error(PID.INVALID_ESCAPE_SYNTAX, "Invalid escape character syntax");
                } else if ((c = nextc()) == '\\') {
                    return (char) (readEscape() | 0x80);
                } else if (c == EOF) {
                    compile_error(PID.INVALID_ESCAPE_SYNTAX, "Invalid escape character syntax");
                } 
                return (char) ((c & 0xff) | 0x80);
            case 'C' :
                if (nextc() != '-') {
                    compile_error(PID.INVALID_ESCAPE_SYNTAX, "Invalid escape character syntax");
                }
            case 'c' :
                if ((c = nextc()) == '\\') {
                    c = readEscape();
                } else if (c == '?') {
                    return '\177';
                } else if (c == EOF) {
                    compile_error(PID.INVALID_ESCAPE_SYNTAX, "Invalid escape character syntax");
                }
                return (char) (c & 0x9f);
            case EOF :
                compile_error(PID.INVALID_ESCAPE_SYNTAX, "Invalid escape character syntax");
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
            compile_error(PID.INVALID_ESCAPE_SYNTAX, errorMessage);
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
        if (i == 0 || (strict && count != i)) compile_error(PID.INVALID_ESCAPE_SYNTAX, errorMessage);

        return hexValue;
    }
}
