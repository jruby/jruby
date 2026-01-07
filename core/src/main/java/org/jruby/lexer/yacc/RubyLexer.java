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
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubySymbol;
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
import org.jruby.parser.RubyParserBase;
import org.jruby.parser.ProductionState;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;
import org.jruby.util.StringSupport;
import org.jruby.util.cli.Options;

import static org.jruby.api.Error.argumentError;
import static org.jruby.parser.RubyParser.*;
import static org.jruby.util.StringSupport.CR_7BIT;

/*
 * This is a port of the MRI lexer to Java.
 */
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

    private BignumNode newBignumNode(String value, int radix) {
        return new BignumNode(ruby_sourceline, new BigInteger(value, radix));
    }

    private FixnumNode newFixnumNode(String value, int radix) throws NumberFormatException {
        return new FixnumNode(ruby_sourceline, Long.parseLong(value, radix));
    }
    
    private RationalNode newRationalNode(String value, int radix) throws NumberFormatException {
        NumericNode numerator;

        try {
            numerator = new FixnumNode(ruby_sourceline, Long.parseLong(value, radix));
        } catch (NumberFormatException e) {
            numerator = new BignumNode(ruby_sourceline, new BigInteger(value, radix));
        }

        return new RationalNode(ruby_sourceline, numerator, new FixnumNode(ruby_sourceline, 1));
    }
    
    private ComplexNode newComplexNode(NumericNode number) {
        return new ComplexNode(ruby_sourceline, number);
    }
    
    protected void ambiguousOperator(String op, String syn) {
        warning(ID.AMBIGUOUS_ARGUMENT, "'" + op + "' after local variable or literal is interpreted as binary operator");
        warning(ID.AMBIGUOUS_ARGUMENT, "even though it seems like " + syn);
    }

    private void warning(ID id, String message) {
        warning(id, getFile(), getRubySourceline(), message);
    }

    private void warning(ID id, String file, int line, String message) {
        warnings.warning(id, file, line + 1, message); // rubysource-line is 0 based
    }

    private void warn(ID id, String message) {
        warnings.warn(id, getFile(), getRubySourceline(), message);
    }

    public enum Keyword {
        END ("end", new ByteList(new byte[] {'e', 'n', 'd'}, USASCII_ENCODING), keyword_end, keyword_end, EXPR_END),
        ELSE ("else", new ByteList(new byte[] {'e', 'l', 's', 'e'}, USASCII_ENCODING), keyword_else, keyword_else, EXPR_BEG),
        CASE ("case", new ByteList(new byte[] {'c', 'a', 's', 'e'}, USASCII_ENCODING), keyword_case, keyword_case, EXPR_BEG),
        ENSURE ("ensure", new ByteList(new byte[] {'e', 'n', 's', 'u', 'r', 'e'}, USASCII_ENCODING), keyword_ensure, keyword_ensure, EXPR_BEG),
        MODULE ("module", new ByteList(new byte[] {'m', 'o', 'd', 'u', 'l', 'e'}, USASCII_ENCODING), keyword_module, keyword_module, EXPR_BEG),
        ELSIF ("elsif", new ByteList(new byte[] {'e', 'l', 's', 'i', 'f'}, USASCII_ENCODING), keyword_elsif, keyword_elsif, EXPR_BEG),
        DEF ("def", new ByteList(new byte[] {'d', 'e', 'f'}, USASCII_ENCODING), keyword_def, keyword_def, EXPR_FNAME),
        RESCUE ("rescue", new ByteList(new byte[] {'r', 'e', 's', 'c', 'u', 'e'}, USASCII_ENCODING), keyword_rescue, modifier_rescue, EXPR_MID),
        NOT ("not", new ByteList(new byte[] {'n', 'o', 't'}, USASCII_ENCODING), keyword_not, keyword_not, EXPR_ARG),
        THEN ("then", new ByteList(new byte[] {'t', 'h', 'e', 'n'}, USASCII_ENCODING), keyword_then, keyword_then, EXPR_BEG),
        YIELD ("yield", new ByteList(new byte[] {'y', 'i', 'e', 'l', 'd'}, USASCII_ENCODING), keyword_yield, keyword_yield, EXPR_ARG),
        FOR ("for", new ByteList(new byte[] {'f', 'o', 'r'}, USASCII_ENCODING), keyword_for, keyword_for, EXPR_BEG),
        SELF ("self", new ByteList(new byte[] {'s', 'e', 'l', 'f'}, USASCII_ENCODING), keyword_self, keyword_self, EXPR_END),
        FALSE ("false", new ByteList(new byte[] {'f', 'a', 'l', 's', 'e'}, USASCII_ENCODING), keyword_false, keyword_false, EXPR_END),
        RETRY ("retry", new ByteList(new byte[] {'r', 'e', 't', 'r', 'y'}, USASCII_ENCODING), keyword_retry, keyword_retry, EXPR_END),
        RETURN ("return", new ByteList(new byte[] {'r', 'e', 't', 'u', 'r', 'n'}, USASCII_ENCODING), keyword_return, keyword_return, EXPR_MID),
        TRUE ("true", new ByteList(new byte[] {'t', 'r', 'u', 'e'}, USASCII_ENCODING), keyword_true, keyword_true, EXPR_END),
        IF ("if", new ByteList(new byte[] {'i', 'f'}, USASCII_ENCODING), keyword_if, modifier_if, EXPR_BEG),
        DEFINED_P ("defined?", new ByteList(new byte[] {'d', 'e', 'f', 'i', 'n', 'e', 'd', '?'}, USASCII_ENCODING), keyword_defined, keyword_defined, EXPR_ARG),
        SUPER ("super", new ByteList(new byte[] {'s', 'u', 'p', 'e', 'r'}, USASCII_ENCODING), keyword_super, keyword_super, EXPR_ARG),
        UNDEF ("undef",   new ByteList(new byte[] {'u', 'n', 'd', 'e', 'f'}, USASCII_ENCODING), keyword_undef, keyword_undef, EXPR_FNAME|EXPR_FITEM),
        BREAK ("break", new ByteList(new byte[] {'b', 'r', 'e', 'a', 'k'}, USASCII_ENCODING), keyword_break, keyword_break, EXPR_MID),
        IN ("in", new ByteList(new byte[] {'i', 'n'}, USASCII_ENCODING), keyword_in, keyword_in, EXPR_BEG),
        DO ("do", new ByteList(new byte[] {'d', 'o'}, USASCII_ENCODING), keyword_do, keyword_do, EXPR_BEG),
        NIL ("nil", new ByteList(new byte[] {'n', 'i', 'l'}, USASCII_ENCODING), keyword_nil, keyword_nil, EXPR_END),
        UNTIL ("until", new ByteList(new byte[] {'u', 'n', 't', 'i', 'l'}, USASCII_ENCODING), keyword_until, modifier_until, EXPR_BEG),
        UNLESS ("unless", new ByteList(new byte[] {'u', 'n', 'l', 'e', 's', 's'}, USASCII_ENCODING), keyword_unless, modifier_unless, EXPR_BEG),
        OR ("or", new ByteList(new byte[] {'o', 'r'}, USASCII_ENCODING), keyword_or, keyword_or, EXPR_BEG),
        NEXT ("next", new ByteList(new byte[] {'n', 'e', 'x', 't'}, USASCII_ENCODING), keyword_next, keyword_next, EXPR_MID),
        WHEN ("when", new ByteList(new byte[] {'w', 'h', 'e', 'n'}, USASCII_ENCODING), keyword_when, keyword_when, EXPR_BEG),
        REDO ("redo", new ByteList(new byte[] {'r', 'e', 'd', 'o'}, USASCII_ENCODING), keyword_redo, keyword_redo, EXPR_END),
        AND ("and", new ByteList(new byte[] {'a', 'n', 'd'}, USASCII_ENCODING), keyword_and, keyword_and, EXPR_BEG),
        BEGIN ("begin", new ByteList(new byte[] {'b', 'e', 'g', 'i', 'n'}, USASCII_ENCODING), keyword_begin, keyword_begin, EXPR_BEG),
        __LINE__ ("__LINE__", new ByteList(new byte[] {'_', '_', 'L', 'I', 'N', 'E', '_', '_'}, USASCII_ENCODING), keyword__LINE__, keyword__LINE__, EXPR_END),
        CLASS ("class", new ByteList(new byte[] {'c', 'l', 'a', 's', 's'}, USASCII_ENCODING), keyword_class, keyword_class, EXPR_CLASS),
        __FILE__("__FILE__", new ByteList(new byte[] {'_', '_', 'F', 'I', 'L', 'E', '_', '_'}, USASCII_ENCODING), keyword__FILE__, keyword__FILE__, EXPR_END),
        LEND ("END", new ByteList(new byte[] {'E', 'N', 'D'}, USASCII_ENCODING), keyword_END, keyword_END, EXPR_END),
        LBEGIN ("BEGIN", new ByteList(new byte[] {'B', 'E', 'G', 'I', 'N'}, USASCII_ENCODING), keyword_BEGIN, keyword_BEGIN, EXPR_END),
        WHILE ("while", new ByteList(new byte[] {'w', 'h', 'i', 'l', 'e'}, USASCII_ENCODING), keyword_while, modifier_while, EXPR_BEG),
        ALIAS ("alias", new ByteList(new byte[] {'a', 'l', 'i', 'a', 's'}, USASCII_ENCODING), keyword_alias, keyword_alias, EXPR_FNAME|EXPR_FITEM),
        __ENCODING__("__ENCODING__", new ByteList(new byte[] {'_', '_', 'E', 'N', 'C', 'O', 'D', 'I', 'N', 'G', '_', '_'}, USASCII_ENCODING), keyword__ENCODING__, keyword__ENCODING__, EXPR_END);

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
        byteList2Keyword = new HashMap<>();

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
        return byteList2Keyword.get(str);
    }

    public static Keyword getKeyword(String str) {
        return map.get(str);
    }
    
    private RubyParserBase parser;

    // What handles warnings
    private IRubyWarnings warnings;

    public Ruby getRuntime() {
        return parser.getRuntime();
    }

    public int tokenize_ident(int result) {
        // FIXME: Get token from newtok index to lex_p?
        ByteList value = createTokenByteList();
        String id = getRuntime().newSymbol(value).idString();

        if (IS_lex_state(last_state, EXPR_DOT|EXPR_FNAME) && parser.getCurrentScope().isDefined(id) >= 0) {
            setState(EXPR_END);
        }

        yaccValue = value;
        return result;
    }

    private StrTerm lex_strterm;

    public RubyLexer(RubyParserBase parser, LexerSource source, IRubyWarnings warnings) {
        super(source);
        this.parser = parser;
        this.warnings = warnings;
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
                warn(ID.VOID_VALUE_EXPRESSION, "encountered \\r in middle of line, treated as a mere space");
            }
        }

        return c;
    }

    public void heredoc_dedent(Node root) {
        int indent = heredoc_indent;

        if (indent <= 0) return;

        heredoc_indent = 0;

        if (root == null) return;

        if (root instanceof StrNode) {
            StrNode str = (StrNode) root;
            if (str.isNewline()) dedent_string(str.getValue(), indent);
        } else if (root instanceof ListNode) {
            ListNode list = (ListNode) root;
            int length = list.size();
            for (int i = 0; i < length; i++) {
                Node child = list.get(i);

                if (child instanceof StrNode && child.isNewline()) {
                    dedent_string(((StrNode) child).getValue(), indent);
                }
            }
        }
    }

    public void compile_error(String message, long start, long end) {
        throw new SyntaxException(getFile(), ruby_sourceline, prepareMessage(message, lexb, start, end));
    }

    // yyerror1
    public void compile_error(String message) {
        throw new SyntaxException(getFile(), ruby_sourceline, prepareMessage(message, lex_lastline, start, end));
    }

    // yyerror0
    public void parse_error(String message) {
        compile_error(message);
    }

    // This is somewhat based off of parser_yyerror in MRI but we seemingly have some weird differences.
    // I added an extra check on length of ptr_end because some times we end up being off by one and index past
    // the end of the string.  I thought this was because lex_pend tends to reflect chars and not bytes (this is
    // really confusing to me as it should be bytes) but I saw off by one in streams with no mbcs.
    private String prepareMessage(String message, ByteList line, long start, long end) {
        if (line != null && line.length() > 5) {
            int max_line_margin = 30;
            int start_line = ProductionState.line(start);
            int start_column = ProductionState.column(start);
            int end_line = ProductionState.line(end);
            int end_column = ProductionState.column(end);

            if ((start_line != ruby_sourceline && end_line != ruby_sourceline) ||
                    (start_line == end_line && start_column == end_column)) {
                return message;
            }

            int pend = lex_pend;
            if (pend > lex_pbeg && lexb.get(lex_pend - 1) == '\n') {
                pend--;
                if (pend > lex_pbeg && lexb.get(lex_pend - 1) == '\r') {
                    pend--;
                }
            }

            //System.out.println("lex_pbeg: " + lex_pbeg + ", lex_pend: " + lex_pend + ", start_column: " + start_column + ", end_column: " + end_column +
            //        ", ruby_sourceline: " + ruby_sourceline + ", start_line: " + start_line);
            int pt = ruby_sourceline == end_line ? lex_pbeg + end_column : lex_pend;
            int ptr = pt < pend ? pt : pend;
            int ptr_end = ptr;
            int lim = ptr - lex_pbeg > max_line_margin ? ptr - max_line_margin : lex_pbeg;
            while (lim < ptr && lexb.get(ptr - 1) != '\n') {
                ptr--;
            }

            lim = pend - ptr_end > max_line_margin ? ptr_end + max_line_margin : pend;
            while (ptr_end < lim && lexb.get(ptr_end) != '\n') {
                ptr_end++;
            }

            String pre = "";
            String post = "";
            int len = ptr_end - ptr;
            if (len > 4) {
                if (ptr > lex_pbeg) {
                    // FIXME: back up to begin of char in mbc condition
                    if (ptr > lex_pbeg) pre = "...";
                }

                if (ptr_end < pend) {
                    // FIXME: complete reading char in case of mbc condition
                    if (ptr_end < pend) post = "...";
                }
            }

            int pb = lex_pbeg;
            if (ruby_sourceline == start_line) {
                pb += start_column;
                //System.out.println("PB: " + pb + ", PT: " + pt);
                // FIXME: Something is off here.
                /*if (pb > pt) {
                    pb = pt;
                }*/
            }
            //System.out.println("PB: " + pb + ", PT: " + pt);

            if (pb < ptr) pb = ptr;
            //System.out.println("PB: " + pb + ", PT: " + pt);

            if (len <= 4 && start_line == end_line) {
                return message;
            }

            // ADD tty code here with if check

            //System.out.println("ptr: " + ptr + ", ptr_end: " + ptr_end);
            //System.out.println("line: " + line);
            lim = pt < pend ? pt : pend;

            boolean addNewline = message != null && !message.endsWith("\n");

            if (ptr - 1 < 0) ptr = 0;
            if (ptr_end - 1 < 0) ptr_end = 0;
            String shortLine = createAsEncodedString(line.unsafeBytes(), line.begin(), line.length());
            if (ptr_end > shortLine.length()) ptr_end = shortLine.length() - 1;
            shortLine = shortLine.substring(ptr, ptr_end);

            message += (addNewline ? "\n" : "") + pre + shortLine + post;
            addNewline = !message.endsWith("\n");
            int highlightSize = pb + (pre.length() == 3 ? -4 : 0);
            String highlightLine = "";
            if (highlightSize >= 0) {
                highlightLine = new String(new char[highlightSize]);
                highlightLine = highlightLine.replace("\0", " ") + "^";
                if (end_column - start_column > 1) {
                    String underscore = new String(new char[end_column - start_column - 1]);
                    underscore = underscore.replace("\0", "~");
                    highlightLine += underscore;
                }
            }

            message += (addNewline ? "\n" : "") + highlightLine;
        }

        return message;
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

        updateTokenPosition();

        return token == EOF ? 0 : token;
    }

    @Override
    protected void setCompileOptionFlag(String name, ByteList value) {
        if (tokenSeen) {
            warnings.warn(ID.ACCESSOR_MODULE_FUNCTION, "'" + name + "' is ignored after any tokens");
            return;
        }

        int b = asTruth(name, value);
        if (b < 0) return;

        // Enebo: This is a hash in MRI for multiple potential compile options but we currently only support one.
        // I am just going to set it and when a second is done we will reevaluate how they are populated.
        parser.setStringStyle(b == 1);
    }

    @Override
    protected RegexpOptions parseRegexpFlags() throws IOException {
        StringBuilder unknownFlags = new StringBuilder(10);
        RegexpOptions options = parseRegexpFlags(unknownFlags);
        if (unknownFlags.length() != 0) {
            compile_error("unknown regexp option" + (unknownFlags.length() > 1 ? "s" : "") + " - " + unknownFlags);
        }
        return options;
    }

    @Override
    protected void mismatchedRegexpEncodingError(Encoding optionEncoding, Encoding encoding) {
        compile_error("regexp encoding option '" + optionsEncodingChar(optionEncoding) + "' differs from source encoding '" + encoding + "'");
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
        Ruby runtime = getRuntime();
        Encoding newEncoding = runtime.getEncodingService().loadEncoding(name);

        if (newEncoding == null) throw argumentError(runtime.getCurrentContext(), "unknown encoding name: " + name.toString());
        if (!newEncoding.isAsciiCompatible()) throw argumentError(runtime.getCurrentContext(), "" + name + " is not ASCII compatible");

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
            type = tIMAGINARY;
        }

        setState(EXPR_END);
        return type;
    }

    private int getFloatToken(String number, int suffix) {
        if ((suffix & SUFFIX_R) != 0) {
            BigDecimal bd = new BigDecimal(number);
            BigDecimal denominator = BigDecimal.ONE.scaleByPowerOfTen(bd.scale());
            BigDecimal numerator = bd.multiply(denominator);

            try {
                yaccValue = new RationalNode(ruby_sourceline, new FixnumNode(ruby_sourceline, numerator.longValueExact()),
                        new FixnumNode(ruby_sourceline, denominator.longValueExact()));
            } catch (ArithmeticException ae) {
                yaccValue = new RationalNode(ruby_sourceline, new BignumNode(ruby_sourceline, numerator.toBigIntegerExact()),
                        new BignumNode(ruby_sourceline, denominator.toBigIntegerExact()));
            }
            return considerComplex(tRATIONAL, suffix);
        }

        double d;
        try {
            d = Double.valueOf(number);
        } catch (NumberFormatException e) {
            warning(ID.FLOAT_OUT_OF_RANGE, "Float " + number + " out of range.");

            d = number.startsWith("-") ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }
        yaccValue = new FloatNode(ruby_sourceline, d);
        return considerComplex(tFLOAT, suffix);
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
        return considerComplex(tINTEGER, suffix);
    }


    // STR_NEW3/parser_str_new
    public StrNode createStr(ByteList buffer, int flags) {
        Encoding bufferEncoding = buffer.getEncoding();
        int codeRange = StringSupport.codeRangeScan(bufferEncoding, buffer);

        if ((flags & STR_FUNC_REGEXP) == 0 && bufferEncoding.isAsciiCompatible()) {
            // If we have characters outside 7-bit range and we are still ascii then change to ascii-8bit
            if (codeRange == CR_7BIT) {
                // Do nothing like MRI
            } else if (getEncoding() == USASCII_ENCODING && bufferEncoding != UTF8_ENCODING) {
                codeRange = RubyParserBase.associateEncoding(buffer, ASCII8BIT_ENCODING, codeRange);
            }
        }

        return new StrNode(ruby_sourceline, buffer, codeRange, parser.getStringStyle());
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
        
        // Short-hand (e.g. %{,%.,%!,... versus %Q{).
        if (c == EOF) {
            compile_error("unterminated quoted string meets end of file");
            return EOF;
        }

        if (!Character.isLetterOrDigit(c)) {
            begin = c;
            c = 'Q';
            shortHand = true;
        // Long-hand (e.g. %Q{}).
        } else {
            shortHand = false;
            begin = nextc();
            if (Character.isLetterOrDigit(begin) /* no mb || ismbchar(term)*/) compile_error("unknown type of %string");
        }
        if (begin == EOF) {
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

            int newline = 0;
            term = c;
            while ((c = nextc()) != EOF && c != term) {
                if (!tokadd_mbchar(c, markerValue)) return EOF;
                if (newline == 0 && c == '\n') {
                    newline = 1;
                } else if (newline > 0) {
                    newline = 2;
                }
            }

            if (c == EOF) compile_error("unterminated here document identifier");

            switch (newline) {
                case 1:
                    parser.warn("here document identifier ends with a newline");
                    markerValue.setRealSize(markerValue.realSize() - 1);
                    if (markerValue.get(markerValue.realSize() - 1) == '\r') markerValue.setRealSize(markerValue.realSize() - 1);
                    break;
                case 2:
                    compile_error("here document identifier across newlines, never match");
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
            newtok(true);
            func |= str_dquote;
            do {
                if (!tokadd_mbchar(c, markerValue)) return EOF;
            } while ((c = nextc()) != EOF && isIdentifierChar(c));
            pushback(c);
        }

        int len = lex_p - lex_pbeg;
        lex_goto_eol();
        lex_strterm = new HeredocTerm(markerValue, func, len, ruby_sourceline, lex_lastline);
        heredoc_indent = indent;
        heredoc_line_indent = 0;
        flush();
        return token;
    }
    
    private boolean arg_ambiguous(int c) {
        if (warnings.isVerbose() && Options.PARSER_WARN_AMBIGUOUS_ARGUMENTS.load()) {
            if (c == '/') {
                warning(ID.AMBIGUOUS_ARGUMENT, "ambiguity between regexp and two divisions: wrap regexp in parentheses or add a space after '/' operator");
            } else {
                warning(ID.AMBIGUOUS_ARGUMENT, "ambiguous first argument; put parentheses or a space even after '" + (char) c + "' operator");
            }
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
            case yyErrorCode: System.err.print("yyErrorCode,"); break;
            // MISSING tokens
            case tIDENTIFIER: System.err.print("tIDENTIFIER["+ value() + "],"); break;
            case tFID: System.err.print("tFID[" + value() + "],"); break;
            case tGVAR: System.err.print("tGVAR[" + value() + "],"); break;
            case tIVAR: System.err.print("tIVAR[" + value() +"],"); break;
            case tCONSTANT: System.err.print("tCONSTANT["+ value() +"],"); break;
            case tCVAR: System.err.print("tCVAR,"); break;
            case tINTEGER: System.err.print("tINTEGER,"); break;
            case tFLOAT: System.err.print("tFLOAT,"); break;
            case tSTRING_CONTENT: System.err.print("tSTRING_CONTENT[" + ((StrNode) value()).getValue() + "],"); break;
            case tSTRING_BEG: System.err.print("tSTRING_BEG,"); break;
            case tSTRING_END: System.err.print("tSTRING_END,"); break;
            case tSTRING_DEND: System.err.print("tSTRING_DEND,"); break;
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
            case keyword_def: System.err.print("keyword_def,"); break;
            case keyword_case: System.err.print("keyword_case,"); break;
            case keyword_in: System.err.print("keyword_in,"); break;
            case keyword_do: System.err.print("keyword_do,"); break;
            case keyword_do_block: System.err.print("keyword_do_block,"); break;
            case keyword_do_cond: System.err.print("keyword_do_cond,"); break;
            case keyword_end: System.err.print("keyword_end,"); break;
            case keyword_yield: System.err.print("keyword_yield,"); break;
            case '\n': System.err.println("NL"); break;
            case EOF: System.out.println("EOF"); break;
            case tDSTAR: System.err.print("tDSTAR"); break;
            default: System.err.print("'" + (char)token + "',"); break;
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
        boolean tokenSeen = this.tokenSeen;
        
        if (lex_strterm != null) return lex_strterm.parseString(this);

        commandState = commandStart;
        commandStart = false;
        this.tokenSeen = true;

        loop: for(;;) {
            last_state = lex_state;
            flush();
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
                spaceSeen = true;
                continue;
            case '#': { /* it's a comment */
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
                boolean normalArg = IS_lex_state(lex_state, EXPR_BEG | EXPR_CLASS | EXPR_FNAME | EXPR_DOT) &&
                        !IS_lex_state(lex_state, EXPR_LABELED);
                if (normalArg || IS_lex_state_all(lex_state, EXPR_ARG | EXPR_LABELED)) {
                    if (!normalArg && getLexContext().in_kwarg) {
                        // normal_newline
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
                    case '#':
                        pushback(c);
                        continue loop;
                    case 'a': {
                        if (peek('n') && peek('d', 1) && !isIdentifierChar(peekAt(2))) {
                            // leading_logical
                            pushback(c);
                            commandState = false;
                            continue loop;
                        }
                        done = true;
                        break;
                    }
                    case 'o': {
                        if (peek('r') && !isIdentifierChar(peekAt(1))) {
                            // leading_logical
                            pushback(c);
                            commandState = false;
                            continue loop;
                        }
                        done = true;
                        break;
                    }
                    case '|': {
                        if (peek('|')) {
                            // leading_logical
                            pushback(c);
                            commandState = false;
                            continue loop;
                        }
                        done = true;
                        break;
                    }
                    case '&': {
                        if (peek('&')) {
                            // leading_logical
                            pushback(c);
                            commandState = false;
                            continue loop;
                        }
                    }
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

                setState(IS_AFTER_OPERATOR() ? EXPR_ARG : EXPR_BEG);

                c = nextc();
                if (c == '=') {
                    c = nextc();
                    if (c == '=') {
                        yaccValue = EQ_EQ_EQ;
                        return tEQQ;
                    }
                    pushback(c);
                    yaccValue = EQ_EQ;
                    return tEQ;
                }
                if (c == '~') {
                    yaccValue = EQ_TILDE;
                    return tMATCH;
                } else if (c == '>') {
                    yaccValue = EQ_GT;
                    return tASSOC;
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
                if (c == ' ') return tSP;
                if (Character.isWhitespace(c)) return c;
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
        RubySymbol symbol = getRuntime().newSymbol(value);
        String id = symbol.idString();

        if (result == tCONSTANT && !symbol.validConstantName()) result = tIDENTIFIER;
        if (result == tIDENTIFIER && !IS_lex_state(last_state, EXPR_DOT|EXPR_FNAME) &&
                parser.getCurrentScope().isDefined(id) >= 0) {
            setState(EXPR_END|EXPR_LABEL);
        }

        set_yylval_name(value);
        yaccValue = value;
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
        
        //tmpPosition is required because getPosition()'s side effects.
        //if the warning is generated, the getPosition() on line 954 (this line + 18) will create
        //a wrong position if the "inclusive" flag is not set.
        int tmpLine = ruby_sourceline;
        if (IS_SPCARG(c, spaceSeen)) {
            if (warnings.isVerbose() && Options.PARSER_WARN_ARGUMENT_PREFIX.load())
                warning(ID.ARGUMENT_AS_PREFIX, getFile(), tmpLine, "'&' interpreted as argument prefix");
            c = tAMPER;
        } else if (IS_BEG()) {
            c = tAMPER;
        } else {
            c = warn_balanced(c, spaceSeen, '&', "&", "argument prefix");
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
            setState(EXPR_END);
            return result;
        } else if (Character.isDigit(c)) {
            pushback(c);
            if (result == tIVAR) {
                compile_error("'@" + ((char) c) + "' is not allowed as an instance variable name");
            } else {
                compile_error("'@@" + ((char) c) + "' is not allowed as a class variable name");
            }
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
            if (IS_BEG() || IS_lex_state(lex_state, EXPR_CLASS) || (IS_SPCARG(-1, spaceSeen))) {
                setState(EXPR_BEG);
                yaccValue = COLON_COLON;
                return tCOLON3;
            }
            setState(EXPR_DOT);
            yaccValue = COLON_COLON;
            set_yylval_id(COLON_COLON);
            return tCOLON2;
        }

        if (IS_END() || ISSPACE(c) || c == '#') {
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
        yaccValue = COLON;
        return tSYMBEG;
    }

    private int comma(int c) {
        setState(EXPR_BEG|EXPR_LABEL);
        yaccValue = COMMA;
        
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
                return identifierToken(tGVAR, createTokenByteList());
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
            yaccValue = createTokenByteList();
            /* xxx shouldn't check if valid option variable */
            return tGVAR;

        case '&':       /* $&: last match */
        case '`':       /* $`: string before last match */
        case '\'':      /* $': string after last match */
        case '+':       /* $+: string matches last paren. */
            // Explicit reference to these vars as symbols...
            if (IS_lex_state(last_state, EXPR_FNAME)) {
                yaccValue = new ByteList(new byte[] {'$', (byte) c}, USASCII_ENCODING);
                set_yylval_name(new ByteList(new byte[] {'$', (byte) c}));
                return tGVAR;
            }
            
            yaccValue = new BackRefNode(ruby_sourceline, c);
            return tBACK_REF;

        case '1': case '2': case '3': case '4': case '5': case '6':
        case '7': case '8': case '9':
            do {
                c = nextc();
            } while (Character.isDigit(c));
            pushback(c);
            if (IS_lex_state(last_state, EXPR_FNAME)) {
                yaccValue = createTokenByteList();
                set_yylval_name(new ByteList(new byte[] {'$', (byte) c}));
                return tGVAR;
            }

            int ref;
            String refAsString = createTokenString();

            try {
                ref = Integer.parseInt(refAsString.substring(1));
            } catch (NumberFormatException e) {
                warn(ID.AMBIGUOUS_ARGUMENT, "'" + refAsString + "' is too big for a number variable, always nil");
                ref = 0;
            }

            yaccValue = new NthRefNode(ruby_sourceline, ref);
            return tNTH_REF;
        case '0':
            return identifierToken(tGVAR, new ByteList(new byte[] {'$', (byte) c}));
        default:
            if (!isIdentifierChar(c)) {
                if (c == EOF || isSpace(c)) {
                    compile_error("'$' without identifiers is not allowed as a global variable name");
                } else {
                    pushback(c);
                    compile_error("'$" + ((char) c) + "' is not allowed as a global variable name");
                }
            }

            last_state = lex_state;
            setState(EXPR_END);

            tokadd_ident(c);

            return identifierToken(tGVAR, createTokenByteList());  // $blah
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
                    warn(ID.MISCELLANEOUS, "... at EOL, should be parenthesized?");
                } else if (getLeftParenBegin() >= 0 && getLeftParenBegin() + 1 == parenNest) {
                    if (IS_lex_state(last_state, EXPR_LABEL)) {
                        return tDOT3;
                    }
                }

                return isBeg ? tBDOT3 : tDOT3;
            }
            pushback(c);
            yaccValue = DOT_DOT;
            set_yylval_id(DOT);
            return isBeg ? tBDOT2 : tDOT2;
        }
        
        pushback(c);
        if (Character.isDigit(c)) compile_error("no .<digit> floating literal anymore; put 0 before dot");

        setState(EXPR_DOT);
        yaccValue = DOT;
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
            StringBuilder builder = new StringBuilder();
            Formatter formatter = new Formatter(builder, Locale.US);
            formatter.format("Invalid char '\\x%02x' in expression", c & 0xff);
            compile_error(builder.toString());
        }

        newtok(true);
        do {
            if (!tokadd_mbchar(c)) return EOF;
            c = nextc();
        } while (isIdentifierChar(c));

        int result;
        ByteList tempVal;
        last_state = lex_state;

        // methods 'foo!' and 'foo?' are possible but if followed by '=' it is relop
        if ((c == '!' || c == '?') && !peek('=')) {
            result = tFID;
            tempVal = createTokenByteList();
        } else if (c == '=' && IS_lex_state(lex_state, EXPR_FNAME)) {
            int c2 = nextc();
            if (c2 != '~' && c2 != '>' && (c2 != '=' || peek('>'))) {
                result = tIDENTIFIER;
                pushback(c2);
            } else {
                result = tCONSTANT;  // assume provisionally
                pushback(c2);
                pushback(c);
            }
            tempVal = createTokenByteList();
        } else {
            result = tCONSTANT;  // assume provisionally
            pushback(c);
            tempVal = createTokenByteList();
        }

        if (IS_LABEL_POSSIBLE(commandState)) {
            if (IS_LABEL_SUFFIX()) {
                setState(EXPR_ARG|EXPR_LABELED);
                nextc();
                yaccValue = tempVal;
                set_yylval_name(createTokenByteList());
                return tLABEL;
            }
        }

        if (lex_state != EXPR_DOT) {
            Keyword keyword = getKeyword(tempVal); // Is it is a keyword?

            if (keyword != null) {
                int state = lex_state; // Save state at time keyword is encountered
                setState(keyword.state);

                yaccValue = keyword.bytes;
                if (IS_lex_state(state, EXPR_FNAME)) {
                    setState(EXPR_ENDFN);
                    set_yylval_name(createTokenByteList());
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

        return identifierToken(result, tempVal);
    }

    private int leftBracket(boolean spaceSeen) {
        parenNest++;
        int c = '[';
        if (IS_AFTER_OPERATOR()) {
            if ((c = nextc()) == ']') {
                parenNest--;
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
            warnings.warn(ID.MISCELLANEOUS, "parentheses after method name is interpreted as an argument list, not a decomposed argument");
            result = '(';
        } else {
            result = '(';
        }

        parenNest++;
        conditionState.push0();
        cmdArgumentState.push0();
        setState(EXPR_BEG|EXPR_LABEL);
        
        yaccValue = ruby_sourceline;
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
            return tOP_ASGN;
        }

        if (IS_SPCARG(c, spaceSeen) || (IS_lex_state(lex_state, EXPR_FITEM) && c == 's')) return parseQuote(c);

        setState(IS_AFTER_OPERATOR() ? EXPR_ARG : EXPR_BEG);
        
        pushback(c);
        yaccValue = PERCENT;
        set_yylval_id(PERCENT);
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
    
    private int questionMark() throws IOException {
        int c;
        
        if (IS_END()) {
            setState(EXPR_VALUE);
            yaccValue = QUESTION;
            return '?';
        }
        
        c = nextc();
        if (c == EOF) compile_error("incomplete character syntax");

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
                    warning(ID.INVALID_CHAR_SEQUENCE, "invalid character syntax; use ?\\" + c2);
                }
            }
            pushback(c);
            setState(EXPR_VALUE);
            yaccValue = QUESTION;
            return '?';
        } else if (!isASCII(c)) {
            if (!tokadd_mbchar(c)) return EOF;
            yaccValue = new StrNode(ruby_sourceline, createTokenByteList(1));
            setState(EXPR_END);
            return tCHAR;
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

                c = readUTFEscape(oneCharBL, false, new boolean[] { false });
                
                if (c >= 0x80) {
                    tokaddmbc(c, oneCharBL);
                } else {
                    oneCharBL.append(c);
                }
                
                setState(EXPR_END);
                yaccValue = new StrNode(ruby_sourceline, oneCharBL);
                
                return tCHAR;
            } else {
                c = readEscape();
            }
        } else {
            newtok(true);
        }

        ByteList oneCharBL = new ByteList(1);
        oneCharBL.setEncoding(getEncoding());
        oneCharBL.append(c);
        yaccValue = new StrNode(ruby_sourceline, oneCharBL);
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
        yaccValue = SLASH;

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

            pushback(c); // not a '=' put it back
            yaccValue = STAR_STAR;

            if (IS_SPCARG(c, spaceSeen)) {
                if (warnings.isVerbose() && Options.PARSER_WARN_ARGUMENT_PREFIX.load())
                    warning(ID.ARGUMENT_AS_PREFIX, "'**' interpreted as argument prefix");
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
                if (warnings.isVerbose() && Options.PARSER_WARN_ARGUMENT_PREFIX.load())
                    warning(ID.ARGUMENT_AS_PREFIX, "'*' interpreted as argument prefix");
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
     *@return An int constant which represents a token.
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
                        compile_error("Hexadecimal number without hex-digits.");
                    } else if (nondigit != 0) {
                        compile_error("Trailing '_' in number.");
                    }
                    return getIntegerToken(numberBuffer.toString(), 16, numberLiteralSuffix(SUFFIX_ALL));
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
                        parse_error("numeric literal without digits.");
                    } else if (nondigit != 0) {
                        compile_error_pos("Trailing '_' in number.");
                    }
                    return getIntegerToken(numberBuffer.toString(), 2, numberLiteralSuffix(SUFFIX_ALL));
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
                    return getIntegerToken(numberBuffer.toString(), 10, numberLiteralSuffix(SUFFIX_ALL));
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

                        return getIntegerToken(numberBuffer.toString(), 8, numberLiteralSuffix(SUFFIX_ALL));
                    }
                case '8' :
                case '9' :
                    compile_error_pos("Illegal octal digit.");
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
                    nondigit = 0;
                    numberBuffer.append((char) c);
                    break;
                case '.' :
                    if (nondigit != 0) {
                        pushback(c);
                        compile_error("Trailing '_' in number.");
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
                            nondigit = 0;
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
                        int c1 = nextc();
                        if (c1 != '-' && c1 != '+' && !Character.isDigit(c1)) {
                            pushback(c1);
                            pushback(c);
                            nondigit = 0;
                            return getNumberToken(numberBuffer.toString(), seen_e, seen_point, nondigit);
                        }
                        numberBuffer.append((char) c);
                        numberBuffer.append((char) c1);
                        seen_e = true;
                        nondigit = (c1 == '-' || c1 == '+') ? c1 : 0;
                    }
                    break;
                case '_' : //  '_' in number just ignored
                    if (nondigit != 0) compile_error_pos("Trailing '_' in number.");
                    nondigit = c;
                    break;
                default :
                    pushback(c);
                    return getNumberToken(numberBuffer.toString(), seen_e, seen_point, nondigit);
            }
        }
    }

    private int getNumberToken(String number, boolean seen_e, boolean seen_point, int nondigit) {
        boolean isFloat = seen_e || seen_point;
        if (nondigit != '\0') {
            compile_error_pos("Trailing '_' in number.");
        } else if (isFloat) {
            int suffix = numberLiteralSuffix(seen_e ? SUFFIX_I : SUFFIX_ALL);
            return getFloatToken(number, suffix);
        }
        return getIntegerToken(number, 10, numberLiteralSuffix(SUFFIX_ALL));
    }
}
