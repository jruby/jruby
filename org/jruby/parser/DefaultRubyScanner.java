/*
 * DefaultRubyScanner.java - No description
 * Created on 08. Oktober 2001, 14:38
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package org.jruby.parser;

import java.io.*;

import org.jruby.scanner.*;
import org.jruby.*;
import org.jruby.nodes.*;
import org.jruby.nodes.types.*;
import org.jruby.runtime.*;
import org.jruby.scanner.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class DefaultRubyScanner implements DefaultRubyParser.yyInput {
    private Ruby ruby;
    private ParserHelper ph;
    private NodeFactory nf;

    // Scanner

    private int token = 0; // yyInput implementation

    /*
        private String lex_curline; // current line
        private int lex_pbeg;
        private int col; // pointer in current line
        private int lex_pend; // pointer to end of line
    
        private int lex_gets_ptr; // beginning of the next line
        private RubyObject lex_input; // non-nil if File
        private RubyObject lex_lastline;
    
        private boolean lex_file_io; //true, if scanner source is a file and false, if lex_get_str() shall be used.
    */

    // private MarkFilterScannerSource markFilterSource;
    // private LineFilterScannerSource source;
    private LineBufferScannerSource source;
    private IScannerSupport support;

    // deal with tokens..................
    private StringBuffer tokenbuf;
    private Object yyVal;

    // COND and CMDARG stacks
    private long cond_stack;
    private int cond_nest = 0;
    private long cmdarg_stack;

    public DefaultRubyScanner(Ruby ruby) {
        this.ruby = ruby;
        this.ph = ruby.getParserHelper();
        this.nf = new NodeFactory(ruby);
    }

    // yyInput methods

    public boolean advance() throws IOException {
        return (token = yylex()) != 0;
    }

    public int token() {
        return token;
    }

    public Object value() {
        return yyVal;
    }

    // COND and CMDARG stacks

    public void COND_PUSH() {
        cond_nest++;
        cond_stack = (cond_stack << 1) | 1;
    }

    public void COND_POP() {
        cond_nest--;
        cond_stack >>= 1;
    }

    private boolean COND_P() {
        return (cond_nest > 0 && (cond_stack & 1) != 0);
    }

    public void CMDARG_PUSH() {
        cmdarg_stack = (cmdarg_stack << 1) | 1;
    }

    public void CMDARG_POP() {
        cmdarg_stack >>= 1;
    }

    private boolean CMDARG_P() {
        return (cmdarg_stack != 0 && (cmdarg_stack & 1) != 0);
    }

    public void resetStacks() {
        cond_nest = 0;
        cond_stack = 0;
        cmdarg_stack = 0;
    }

    // Helper methods

    /* gc protect */
    /*private RubyObject lex_gets_str(RubyObject _s) {
        String s = ((RubyString) _s).getValue();
        if (lex_gets_ptr != 0) {
            if (s.length() == lex_gets_ptr) {
                return ruby.getNil();
            }
            s = s.substring(lex_gets_ptr);
        }
        int end = 0;
        while (end < s.length()) {
            if (s.charAt(end++) == '\n') {
                ph.incrementLine();
                break;
            }
        }
        lex_gets_ptr += end;
        return RubyString.newString(ruby, s, end);
    }*/

    /**
     *  Returns next line either from file or from a string.
     */
    /*private RubyObject lex_getline() {
        RubyObject line;
        if (lex_file_io) {
            // uses rb_io_gets(lex_input)
            throw new Error();
        } else {
            line = lex_gets_str(lex_input);
        }
        if (ph.getRubyDebugLines() != null && !line.isNil()) {
            ph.getRubyDebugLines().push(line);
        }
        return line;
    }*/

    /**
     *  Returns the next character from input
     */
    private int nextc() {
        int c = source.read();

        if (c == 65535) {
            c = -1;
        }
        
        ruby.setSourceLine(source.getLine());

        return c;
        /*int c;
        
        if (col == lex_pend) {
            if (lex_input != null) {
                RubyObject v = lex_getline();
        
                if (v.isNil()) {
                    return -1;
                }
                if (ph.getHeredocEnd() > 0) {
                    ruby.setSourceLine(ph.getHeredocEnd());
                    ph.setHeredocEnd(0);
                }
                ruby.setSourceLine(ruby.getSourceLine() + 1);
                lex_curline = ((RubyString) v).getValue();
                col = lex_pbeg = 0;
                lex_pend = lex_curline.length();
                if (lex_curline.startsWith("__END__")
                    && (lex_pend == 7 || lex_curline.charAt(7) == '\n' || lex_curline.charAt(7) == '\r')) {
                    ph.setRubyEndSeen(true);
                    lex_lastline = null;
                    return -1;
                }
                lex_lastline = v;
            } else {
                lex_lastline = null;
                return -1;
            }
        }
        c = lex_curline.charAt(col++);
        if (c == '\r' && col <= lex_pend && lex_curline.charAt(col) == '\n') {
            col++;
            c = '\n';
        }
        
        return c;*/
    }

    /**
     *  Puts back the given character so that nextc() will answer it next time
     *  it'll be called.
     */
    private void pushback(int c) {
        source.unread();
    }

    /**
     *  Returns true if the given character is the current one in the input
     *  stream
     */
    private boolean peek(int c) {
        // return col != lex_pend && c == lex_curline.charAt(col);
        return source.isNext((char) c);
    }

    private String tok() {
        return tokenbuf.toString();
    }

    private int toklen() {
        return tokenbuf.length();
    }

    private char toklast() {
        return tokenbuf.charAt(toklen() - 1);
    }

    private void newtok() {
        tokenbuf = new StringBuffer(60);
    }

    private void tokadd(int c) {
        tokenbuf.append((char) c);
    }

    // yylex helpers...................

    private int read_escape() {
        int c;

        switch (c = nextc()) {
            case '\\' : // Backslash
                return c;
            case 'n' : // newline
                return '\n';
            case 't' : // horizontal tab
                return '\t';
            case 'r' : // carriage-return
                return '\r';
            case 'f' : // form-feed
                return '\f';
            case 'v' : // vertical tab
                return '\013';
            case 'a' : // alarm(bell)
                return '\007';
            case 'e' : // escape
                return '\033';
            case '0' :
            case '1' :
            case '2' :
            case '3' : // octal constant
            case '4' :
            case '5' :
            case '6' :
            case '7' :
                {
                    int cc = 0;

                    pushback(c);
                    for (int i = 0; i < 3; i++) {
                        c = nextc();
                        if (c == -1) {
                            // goto eof
                            yyerror("Invalid escape character syntax");
                            return '\0';
                        }
                        if (c < '0' || '7' < c) {
                            pushback(c);
                            break;
                        }
                        cc = cc * 8 + c - '0';
                    }
                    c = cc;
                }
                return c;
            case 'x' : // hex constant
                {
                    int[] numlen = new int[1];
                    c = (int) support.readHexAsInt(2);
                }
                return c;
            case 'b' : // backspace
                return '\010';
            case 's' : // space
                return ' ';
            case 'M' :
                if ((c = nextc()) != '-') {
                    yyerror("Invalid escape character syntax");
                    pushback(c);
                    return '\0';
                }
                if ((c = nextc()) == '\\') {
                    return read_escape() | 0x80;
                } else if (c == -1) {
                    // goto eof
                    yyerror("Invalid escape character syntax");
                    return '\0';
                } else {
                    return ((c & 0xff) | 0x80);
                }

            case 'C' :
                if ((c = nextc()) != '-') {
                    yyerror("Invalid escape character syntax");
                    pushback(c);
                    return '\0';
                }
            case 'c' :
                if ((c = nextc()) == '\\') {
                    c = read_escape();
                } else if (c == '?') {
                    return 0177;
                } else if (c == -1) {
                    // goto eof
                    yyerror("Invalid escape character syntax");
                    return '\0';
                }
                return c & 0x9f;
            case -1 :
                // eof:
                yyerror("Invalid escape character syntax");
                return '\0';
            default :
                return c;
        }
    }

    private int tokadd_escape(int term) {
        /*
         *  FIX 1.6.5
         */
        int c;

        switch (c = nextc()) {
            case '\n' :
                return 0;
                /*
                 *  just ignore
                 */
            case '0' :
            case '1' :
            case '2' :
            case '3' :
                /*
                 *  octal constant
                 */
            case '4' :
            case '5' :
            case '6' :
            case '7' :
                {
                    int i;

                    tokadd('\\');
                    tokadd(c);
                    for (i = 0; i < 2; i++) {
                        c = nextc();
                        if (c == -1) {
                            // goto eof;
                            yyerror("Invalid escape character syntax");
                            return -1;
                            // goto eof; end
                        }

                        if (c < '0' || '7' < c) {
                            pushback(c);
                            break;
                        }
                        tokadd(c);
                    }
                }
                return 0;
            case 'x' : // hex constant
                {
                    tokadd('\\');
                    tokadd(c);

                    // +++ don't look nice
                    String hex = support.readHex(2);
                    for (int i = 0; i < hex.length(); i++) {
                        tokadd(hex.charAt(i));
                    }
                }
                return 0;
            case 'M' :
                if ((c = nextc()) != '-') {
                    yyerror("Invalid escape character syntax");
                    pushback(c);
                    return 0;
                }
                tokadd('\\');
                tokadd('M');
                tokadd('-');
                //goto escaped;
                if ((c = nextc()) == '\\') {
                    return tokadd_escape(term);
                    /*
                     *  FIX 1.6.5
                     */
                } else if (c == -1) {
                    // goto eof;
                    yyerror("Invalid escape character syntax");
                    return -1;
                    // goto eof; end
                }
                tokadd(c);
                return 0;
            case 'C' :
                if ((c = nextc()) != '-') {
                    yyerror("Invalid escape character syntax");
                    pushback(c);
                    return 0;
                }
                tokadd('\\');
                tokadd('C');
                tokadd('-');
                //goto escaped;
                if ((c = nextc()) == '\\') {
                    return tokadd_escape(term);
                    /*
                     *  FIX 1.6.5
                     */
                } else if (c == -1) {
                    // goto eof;
                    yyerror("Invalid escape character syntax");
                    return -1;
                    // goto eof; end
                }
                tokadd(c);
                return 0;
            case 'c' :
                tokadd('\\');
                tokadd('c');
                //escaped:
                if ((c = nextc()) == '\\') {
                    return tokadd_escape(term);
                    /*
                     *  FIX 1.6.5
                     */
                } else if (c == -1) {
                    // goto eof;
                    yyerror("Invalid escape character syntax");
                    return -1;
                    // goto eof; end
                }
                tokadd(c);
                return 0;
            case -1 :
                // eof:
                yyerror("Invalid escape character syntax");
                return -1;
            default :
                if (c != term) {
                    /*
                     *  FIX 1.6.5
                     */
                    tokadd('\\');
                }
                /*
                 *  FIX 1.6.5
                 */
                tokadd(c);
        }
        return 0;
    }

    private int parseRegexp(int closeQuote, int openQuote) {
        int c;
        char kcode = 0;
        boolean once = false;
        int nest = 0;
        int options = 0;
        int re_start = ruby.getSourceLine();
        Node list = null;

        newtok();
        regx_end : while ((c = nextc()) != -1) {
            if (c == closeQuote && nest == 0) {
                break regx_end;
            }

            switch (c) {
                case '#' :
                    list = parseExpressionString(list, closeQuote);
                    if (list == Node.MINUS_ONE) {
                        return 0;
                    }
                    continue;
                case '\\' :
                    if (tokadd_escape(closeQuote) < 0) {
                        /*
                         *  FIX 1.6.5
                         */
                        return 0;
                    }
                    continue;
                case -1 :
                    //goto unterminated;
                    ruby.setSourceLine(re_start);
                    ph.rb_compile_error("unterminated regexp meets end of file");
                    return 0;
                default :
                    if (openQuote != 0) {
                        if (c == openQuote) {
                            nest++;
                        }
                        if (c == closeQuote) {
                            nest--;
                        }
                    }
                    break;
            }
            tokadd(c);
        }

        end_options : for (;;) {
            switch (c = nextc()) {
                case 'i' :
                    options |= ReOptions.RE_OPTION_IGNORECASE;
                    break;
                case 'x' :
                    options |= ReOptions.RE_OPTION_EXTENDED;
                    break;
                case 'p' : // /p is obsolete
                    ph.rb_warn("/p option is obsolete; use /m\n\tnote: /m does not change ^, $ behavior");
                    options |= ReOptions.RE_OPTION_POSIXLINE;
                    break;
                case 'm' :
                    options |= ReOptions.RE_OPTION_MULTILINE;
                    break;
                case 'o' :
                    once = true;
                    break;
                case 'n' :
                    kcode = 16;
                    break;
                case 'e' :
                    kcode = 32;
                    break;
                case 's' :
                    kcode = 48;
                    break;
                case 'u' :
                    kcode = 64;
                    break;
                default :
                    pushback(c);
                    break end_options;
            }
        }

        ph.setLexState(LexState.EXPR_END);

        if (list != null) {
            list.setLine(re_start);
            if (toklen() > 0) {
                RubyObject ss = RubyString.newString(ruby, tok(), toklen());
                ph.list_append(list, nf.newStr(ss));
            }
            // new RuntimeException("Want to change " + list.getClass().getName() + "to DRegxNode").printStackTrace();
            // list.nd_set_type(once ? NODE.NODE_DREGX_ONCE : NODE.NODE_DREGX);
            if (once) {
                list = new DRegxOnceNode(list.getLiteral(), options | kcode);
            } else {
                list = new DRegxNode(list.getLiteral(), options | kcode);
            }
            // list.setCFlag(options | kcode);
            yyVal = list;
            return Token.tDREGEXP;
        } else {
            yyVal = newRegexp(tok(), toklen(), options | kcode);
            return Token.tREGEXP;
        }
        //unterminated:
        //ruby_sourceline = re_start;
        //rb_compile_error("unterminated regexp meets end of file");
        //return 0;
    }

    private int parseString(int func, int closeQuote, int openQuote) {
        int c;
        Node list = null;
        int strstart;
        int nest = 0;

        if (func == '\'') {
            return parseSingleQuotedString(closeQuote, openQuote);
        }
        if (func == 0) {
            // read 1 line for heredoc
            // -1 for chomp
            yyVal = RubyString.newString(ruby, support.readLine());
            return Token.tSTRING;
        }
        strstart = ruby.getSourceLine();
        newtok();
        while ((c = nextc()) != closeQuote || nest > 0) {
            if (c == -1) {
                //unterm_str:
                ruby.setSourceLine(strstart);
                ph.rb_compile_error("unterminated string meets end of file");
                return 0;
            }
            /*
             *  if (ismbchar(c)) {
             *  int i, len = mbclen(c)-1;
             *  for (i = 0; i < len; i++) {
             *  tokadd(c);
             *  c = nextc();
             *  }
             *  }
             *  else
             */
            if (c == '#') {
                list = parseExpressionString(list, closeQuote);
                if (list == Node.MINUS_ONE) {
                    //goto unterm_str;
                    ruby.setSourceLine(strstart);
                    ph.rb_compile_error("unterminated string meets end of file");
                    return 0;
                }
                continue;
            } else if (c == '\\') {
                c = nextc();
                if (c == '\n') {
                    continue;
                }
                if (c == closeQuote) {
                    tokadd(c);
                } else {
                    pushback(c);
                    if (func != '"') {
                        tokadd('\\');
                    }
                    tokadd(read_escape());
                }
                continue;
            }
            if (openQuote != 0) {
                if (c == openQuote) {
                    nest++;
                }
                if (c == closeQuote && nest-- == 0) {
                    break;
                }
            }
            tokadd(c);
        }

        ph.setLexState(LexState.EXPR_END);

        if (list != null) {
            list.setLine(strstart);
            if (toklen() > 0) {
                RubyObject ss = RubyString.newString(ruby, tok(), toklen());
                ph.list_append(list, nf.newStr(ss));
            }
            yyVal = list;
            if (func == '`') {
                // new RuntimeException("[BUG] want to change " + list.getClass().getName() + " to DXStrNode").printStackTrace();
                // list.nd_set_type(Constants.NODE_DXSTR);
                list = new DXStrNode(list.getLiteral());
                yyVal = list;
                return Token.tDXSTRING;
            } else {
                return Token.tDSTRING;
            }
        } else {
            yyVal = RubyString.newString(ruby, tok(), toklen());
            return (func == '`') ? Token.tXSTRING : Token.tSTRING;
        }
    }

	/** Parse a single quoted string (', or %q).
	 * 
	 */
    private int parseSingleQuotedString(int closeQuote, int openQuote) {
        int c;
        int nest = 0;

        int strstart = ruby.getSourceLine();
        
        StringBuffer stringToken = new StringBuffer();

        while ((c = nextc()) != closeQuote || nest > 0) {
            if (c == -1) {
                ruby.setSourceLine(strstart);
                ph.rb_compile_error("unterminated string meets end of file");
                return 0;
            }
            if (c == '\\') {
                c = nextc();
                switch (c) {
                    case '\n' :
                        continue;
                    case '\\' :
                        c = '\\';
                        break;
                    default :
                        // fall through
                        if (c == closeQuote || (openQuote != 0 && c == openQuote)) {
                            stringToken.append((char)c);
                            continue;
                        }
                        stringToken.append('\\');
                }
            }
            if (openQuote != 0) {
                if (c == openQuote) {
                    nest++;
                }
                if (c == closeQuote && nest-- == 0) {
                    break;
                }
            }
            stringToken.append((char)c);
        }

        yyVal = RubyString.newString(ruby, stringToken.toString());
        ph.setLexState(LexState.EXPR_END);
        return Token.tSTRING;
    }

	/** parse quoted words (%w{})
	 * 
	 */
    private int parseQuotedWords(int closeQuote, int openQuote) {
        Node qwords = null;
        int nest = 0;

        int strstart = ruby.getSourceLine();
        StringBuffer stringToken = new StringBuffer();

        int c = nextc();
        
        // Skip preceding spaces.
        while (isSpace(c)) {
            c = nextc();
        }
        pushback(c);

        while ((c = nextc()) != closeQuote || nest > 0) {
            if (c == -1) {
                ruby.setSourceLine(strstart);
                ph.rb_compile_error("unterminated string meets end of file");
                return 0;
            }
            if (c == '\\') {
                c = nextc();
                switch (c) {
                    case '\n' :
                        continue;
                    case '\\' :
                        c = '\\';
                        break;
                    default :
                        if (c == closeQuote || (openQuote != 0 && c == openQuote)) {
                            stringToken.append((char)c);
                            continue;
                        }
                        if (!isSpace(c)) {
                            stringToken.append('\\');
                        }
                        break;
                }
            } else if (isSpace(c)) {
                Node str = nf.newStr(RubyString.newString(ruby, stringToken.toString()));
                stringToken = new StringBuffer();

                if (qwords == null) {
                    qwords = nf.newList(str);
                } else {
                    ph.list_append(qwords, str);
                }

                // skip continuous spaces
                c = nextc();
                while (isSpace(c)) {
                    c = nextc();
                }
                pushback(c);

                continue;
            }
            if (openQuote != 0) {
                if (c == openQuote) {
                    nest++;
                }
                if (c == closeQuote && nest-- == 0) {
                    break;
                }
            }
            stringToken.append((char)c);
        }

        if (stringToken.length() > 0) {
            Node str = nf.newStr(RubyString.newString(ruby, stringToken.toString()));

            if (qwords == null) {
                qwords = nf.newList(str);
            } else {
                ph.list_append(qwords, str);
            }
        }

        yyVal = qwords != null ? qwords : nf.newZArray();
        
        ph.setLexState(LexState.EXPR_END);
        
        return Token.tDSTRING;
    }

    private int parseHereDocument(int closeQuote, boolean indent) {
        int c;
        Node list = null;

        int linesave = ruby.getSourceLine();

        newtok();

        switch (closeQuote) {
            case '\'' :
            case '"' :
            case '`' :
                while ((c = nextc()) != closeQuote) {
                    tokadd(c);
                }
                if (closeQuote == '\'') {
                    closeQuote = 0;
                }
                break;
            default :
                c = closeQuote;
                closeQuote = '"';
                if (!isIdentifierChar(c)) {
                    ph.rb_warn("use of bare << to mean <<\"\" is deprecated");
                    break;
                }

                while (isIdentifierChar(c)) {
                    tokadd(c);
                    c = nextc();
                }
                pushback(c);
                break;
        }
        
        // markFilterSource.mark();
        // support.readLine();
        int bufferLine = source.getLine();
        int bufferColumn = source.getColumn();
        String buffer = support.readLine() + '\n';

        String eos = tok();

        StringBuffer sb = new StringBuffer();

        while (true) {
            // test if the end of file or end of string is reached.
            String line = support.readLine();
            if (line == null) {
                // error:
                ruby.setSourceLine(linesave);
                ph.rb_compile_error("can't find string \"" + eos + "\" anywhere before EOF");
                return 0;
            } else if ((indent && line.trim().startsWith(eos)) || line.startsWith(eos)) {
                if (line.trim().length() == eos.length()) {
                    // source.addCurrentLine();
                    break;
                }
            }
            source.unread(line.length() + 1);

            while (true) {
                switch (parseString(closeQuote, '\n', '\n')) {
                    case Token.tSTRING :
                    case Token.tXSTRING :
                         ((RubyString) yyVal).cat("\n");
                        if (list == null) {
                            sb.append(yyVal.toString());
                        } else {
                            ph.list_append(list, nf.newStr((RubyObject) yyVal));
                        }
                        break;
                    case Token.tDSTRING :
                        if (list == null) {
                            list = nf.newDStr(RubyString.newString(ruby, sb.toString()));
                        }
                        /* fall through */
                    case Token.tDXSTRING :
                        if (list == null) {
                            list = nf.newDXStr(RubyString.newString(ruby, sb.toString()));
                        }
                        ph.list_append((Node) yyVal, nf.newStr(RubyString.newString(ruby, "\n")));

                        ((StrNodeConvertable) yyVal).convertToStrNode();

                        yyVal = nf.newList((Node) yyVal);
                        ((Node) yyVal).setNextNode(((Node) yyVal).getHeadNode().getNextNode());
                        ph.list_concat(list, (Node) yyVal);
                        break;
                    case 0 :
                        // goto error;
                        ruby.setSourceLine(linesave);
                        ph.rb_compile_error("can't find string \"" + eos + "\" anywhere before EOF");
                        return 0;
                }
                if (source.isEOL()) {
                    // source.addCurrentLine();
                    break;
                }
            }
        }

        ph.setLexState(LexState.EXPR_END);
        ph.setHeredocEnd(ruby.getSourceLine());
        ruby.setSourceLine(linesave);

        //markFilterSource.reset();
        source.setBufferLine(buffer, bufferLine, bufferColumn);

        if (list != null) {
            list.setLine(linesave + 1);
            yyVal = list;
        }
        switch (closeQuote) {
            case '\0' :
            case '\'' :
            case '"' :
                if (list != null) {
                    return Token.tDSTRING;
                }
                yyVal = RubyString.newString(ruby, sb.toString());
                return Token.tSTRING;
            case '`' :
                if (list != null) {
                    return Token.tDXSTRING;
                }
                yyVal = RubyString.newString(ruby, sb.toString());
                return Token.tXSTRING;
        }
        return 0;
    }

    private void arg_ambiguous() {
        ph.rb_warning("ambiguous first argument; make sure");
    }

    /**
     *  Returns the next token. Also sets yyVal is needed.
     *
     *@return    Description of the Returned Value
     */
    private int yylex() {
        int c;
        int space_seen = 0;
        Keyword kw;

        retry : for (;;) {
            switch (c = nextc()) {
                case '\0' : // NUL
                case '\004' : // ^D
                case '\032' : // ^Z
                case -1 : //end of script.
                    return 0;
                    // white spaces
                case ' ' :
                case '\t' :
                case '\f' :
                case '\r' :
                case '\013' : // '\v'
                    space_seen++;
                    continue retry;
                case '#' : // it's a comment
                    while ((c = nextc()) != '\n') {
                        if (c == -1) {
                            return 0;
                        }
                    }
                    // fall through
                case '\n' :
                    switch (ph.getLexState()) {
                        case LexState.EXPR_BEG :
                        case LexState.EXPR_FNAME :
                        case LexState.EXPR_DOT :
                            continue retry;
                        default :
                            break;
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return '\n';
                case '*' :
                    if ((c = nextc()) == '*') {
                        ph.setLexState(LexState.EXPR_BEG);
                        if (nextc() == '=') {
                            yyVal = "**"; // ph.newId(Token.tPOW);
                            return Token.tOP_ASGN;
                        }
                        pushback(c);
                        return Token.tPOW;
                    }
                    if (c == '=') {
                        yyVal = "*"; // ph.newId('*');
                        ph.setLexState(LexState.EXPR_BEG);
                        return Token.tOP_ASGN;
                    }
                    pushback(c);
                    if (isArgState() && space_seen != 0 && !isSpace(c)) {
                        ph.rb_warning("'*' interpreted as argument prefix");
                        c = Token.tSTAR;
                    } else if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID) {
                        c = Token.tSTAR;
                    } else {
                        c = '*';
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return c;
                case '!' :
                    ph.setLexState(LexState.EXPR_BEG);
                    if ((c = nextc()) == '=') {
                        return Token.tNEQ;
                    }
                    if (c == '~') {
                        return Token.tNMATCH;
                    }
                    pushback(c);
                    return '!';
                case '=' :
                    if (source.getColumn() == 1) {
                        // skip embedded rd document
                        if (support.isNext("begin") && Character.isWhitespace(support.getCharAt(5))) {
                            for (;;) {
                                // col = lex_pend;
                                c = nextc();
                                if (c == -1) {
                                    ph.rb_compile_error("embedded document meets end of file");
                                    return 0;
                                } else if (c != '=') {
                                    continue;
                                } else if (source.getColumn() == 1 && support.isNext("end") && Character.isWhitespace(support.getCharAt(3))) {
                                    break;
                                }
                            }
                            support.readLine();
                            continue retry;
                        }
                    }

                    ph.setLexState(LexState.EXPR_BEG);
                    if ((c = nextc()) == '=') {
                        if ((c = nextc()) == '=') {
                            return Token.tEQQ;
                        }
                        pushback(c);
                        return Token.tEQ;
                    }
                    if (c == '~') {
                        return Token.tMATCH;
                    } else if (c == '>') {
                        return Token.tASSOC;
                    }
                    pushback(c);
                    return '=';
                case '<' :
                    c = nextc();
                    if (c == '<'
                        && ph.getLexState() != LexState.EXPR_END
                        && ph.getLexState() != LexState.EXPR_ENDARG
                        && ph.getLexState() != LexState.EXPR_CLASS
                        && (!isArgState() || space_seen != 0)) {
                        int c2 = nextc();
                        boolean indent = false;
                        if (c2 == '-') {
                            indent = true;
                            c2 = nextc();
                        }
                        if (!isSpace(c2) && ("\"'`".indexOf(c2) != -1 || isIdentifierChar(c2))) {
                            return parseHereDocument(c2, indent);
                        }
                        pushback(c2);
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    if (c == '=') {
                        if ((c = nextc()) == '>') {
                            return Token.tCMP;
                        }
                        pushback(c);
                        return Token.tLEQ;
                    }
                    if (c == '<') {
                        if (nextc() == '=') {
                            yyVal = "<<"; // ph.newId(Token.tLSHFT);
                            return Token.tOP_ASGN;
                        }
                        pushback(c);
                        return Token.tLSHFT;
                    }
                    pushback(c);
                    return '<';
                case '>' :
                    ph.setLexState(LexState.EXPR_BEG);
                    if ((c = nextc()) == '=') {
                        return Token.tGEQ;
                    }
                    if (c == '>') {
                        if ((c = nextc()) == '=') {
                            yyVal = ">>"; //ph.newId(Token.tRSHFT);
                            return Token.tOP_ASGN;
                        }
                        pushback(c);
                        return Token.tRSHFT;
                    }
                    pushback(c);
                    return '>';
                case '"' :
                    return parseString(c, c, c);
                case '`' :
                    if (ph.getLexState() == LexState.EXPR_FNAME) {
                        return c;
                    }
                    if (ph.getLexState() == LexState.EXPR_DOT) {
                        return c;
                    }
                    return parseString(c, c, c);
                case '\'' :
                    return parseSingleQuotedString(c, 0);
                case '?' :
                    if (ph.getLexState() == LexState.EXPR_END) {
                        ph.setLexState(LexState.EXPR_BEG);
                        return '?';
                    }
                    c = nextc();
                    if (c == -1) { /* FIX 1.6.5 */
                        ph.rb_compile_error("incomplete character syntax");
                        return 0;
                    }
                    if (isArgState() && isSpace(c)) {
                        pushback(c);
                        ph.setLexState(LexState.EXPR_BEG);
                        return '?';
                    }
                    if (c == '\\') {
                        c = read_escape();
                    }
                    c &= 0xff;
                    yyVal = RubyFixnum.newFixnum(ruby, c);
                    ph.setLexState(LexState.EXPR_END);
                    return Token.tINTEGER;
                case '&' :
                    if ((c = nextc()) == '&') {
                        ph.setLexState(LexState.EXPR_BEG);
                        if ((c = nextc()) == '=') {
                            yyVal = "&&"; // ph.newId(Token.tANDOP);
                            return Token.tOP_ASGN;
                        }
                        pushback(c);
                        return Token.tANDOP;
                    } else if (c == '=') {
                        yyVal = "&"; //ph.newId('&');
                        ph.setLexState(LexState.EXPR_BEG);
                        return Token.tOP_ASGN;
                    }
                    pushback(c);
                    if (isArgState() && space_seen != 0 && !isSpace(c)) {
                        ph.rb_warning("`&' interpeted as argument prefix");
                        c = Token.tAMPER;
                    } else if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID) {
                        c = Token.tAMPER;
                    } else {
                        c = '&';
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return c;
                case '|' :
                    ph.setLexState(LexState.EXPR_BEG);
                    if ((c = nextc()) == '|') {
                        if ((c = nextc()) == '=') {
                            yyVal = "||"; // ph.newId(Token.tOROP);
                            return Token.tOP_ASGN;
                        }
                        pushback(c);
                        return Token.tOROP;
                    } else if (c == '=') {
                        yyVal = "|"; //ph.newId('|');
                        return Token.tOP_ASGN;
                    }
                    pushback(c);
                    return '|';
                case '+' :
                    c = nextc();
                    if (ph.getLexState() == LexState.EXPR_FNAME || ph.getLexState() == LexState.EXPR_DOT) {
                        if (c == '@') {
                            return Token.tUPLUS;
                        }
                        pushback(c);
                        return '+';
                    }
                    if (c == '=') {
                        ph.setLexState(LexState.EXPR_BEG);
                        yyVal = "+"; //ph.newId('+');
                        return Token.tOP_ASGN;
                    }
                    if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID || (isArgState() && space_seen != 0 && !isSpace(c))) {
                        if (isArgState()) {
                            arg_ambiguous();
                        }
                        ph.setLexState(LexState.EXPR_BEG);
                        pushback(c);
                        if (Character.isDigit((char) c)) {
                            c = '+';
                            return start_num(c);
                        }
                        return Token.tUPLUS;
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    pushback(c);
                    return '+';
                case '-' :
                    c = nextc();
                    if (ph.getLexState() == LexState.EXPR_FNAME || ph.getLexState() == LexState.EXPR_DOT) {
                        if (c == '@') {
                            return Token.tUMINUS;
                        }
                        pushback(c);
                        return '-';
                    }
                    if (c == '=') {
                        ph.setLexState(LexState.EXPR_BEG);
                        yyVal = "-"; // ph.newId('-');
                        return Token.tOP_ASGN;
                    }
                    if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID || (isArgState() && space_seen != 0 && !isSpace(c))) {
                        if (isArgState()) {
                            arg_ambiguous();
                        }
                        ph.setLexState(LexState.EXPR_BEG);
                        pushback(c);
                        if (Character.isDigit((char) c)) {
                            c = '-';
                            return start_num(c);
                        }
                        return Token.tUMINUS;
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    pushback(c);
                    return '-';
                case '.' :
                    ph.setLexState(LexState.EXPR_BEG);
                    if ((c = nextc()) == '.') {
                        if ((c = nextc()) == '.') {
                            return Token.tDOT3;
                        }
                        pushback(c);
                        return Token.tDOT2;
                    }
                    pushback(c);
                    if (!Character.isDigit((char) c)) {
                        ph.setLexState(LexState.EXPR_DOT);
                        return '.';
                    }
                    c = '.';
                    // fall through

                    //start_num:
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
                    return start_num(c);
                case ']' :
                case '}' :
                    ph.setLexState(LexState.EXPR_END);
                    return c;
                case ')' :
                    if (cond_nest > 0) {
                        cond_stack >>= 1;
                    }
                    ph.setLexState(LexState.EXPR_END);
                    return c;
                case ':' :
                    c = nextc();
                    if (c == ':') {
                        if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID || (isArgState() && space_seen != 0)) {
                            ph.setLexState(LexState.EXPR_BEG);
                            return Token.tCOLON3;
                        }
                        ph.setLexState(LexState.EXPR_DOT);
                        return Token.tCOLON2;
                    }
                    pushback(c);
                    if (ph.getLexState() == LexState.EXPR_END || isSpace(c)) {
                        ph.setLexState(LexState.EXPR_BEG);
                        return ':';
                    }
                    ph.setLexState(LexState.EXPR_FNAME);
                    return Token.tSYMBEG;
                case '/' :
                    if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID) {
                        return parseRegexp('/', '/');
                    }
                    if ((c = nextc()) == '=') {
                        ph.setLexState(LexState.EXPR_BEG);
                        yyVal = "/"; // ph.newId('/');
                        return Token.tOP_ASGN;
                    }
                    pushback(c);
                    if (isArgState() && space_seen != 0) {
                        if (!isSpace(c)) {
                            arg_ambiguous();
                            return parseRegexp('/', '/');
                        }
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return '/';
                case '^' :
                    ph.setLexState(LexState.EXPR_BEG);
                    if ((c = nextc()) == '=') {
                        yyVal = "^"; //ph.newId('^');
                        return Token.tOP_ASGN;
                    }
                    pushback(c);
                    return '^';
                case ',' :
                case ';' :
                    ph.setLexState(LexState.EXPR_BEG);
                    return c;
                case '~' :
                    if (ph.getLexState() == LexState.EXPR_FNAME || ph.getLexState() == LexState.EXPR_DOT) {
                        if ((c = nextc()) != '@') {
                            pushback(c);
                        }
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return '~';
                case '(' :
                    if (cond_nest > 0) {
                        cond_stack = (cond_stack << 1) | 0;
                    }
                    if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID) {
                        c = Token.tLPAREN;
                    } else if (ph.getLexState() == LexState.EXPR_ARG && space_seen != 0) {
                        ph.rb_warning(tok() + " (...) interpreted as method call");
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return c;
                case '[' :
                    if (ph.getLexState() == LexState.EXPR_FNAME || ph.getLexState() == LexState.EXPR_DOT) {
                        if ((c = nextc()) == ']') {
                            if ((c = nextc()) == '=') {
                                return Token.tASET;
                            }
                            pushback(c);
                            return Token.tAREF;
                        }
                        pushback(c);
                        return '[';
                    } else if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID) {
                        c = Token.tLBRACK;
                    } else if (isArgState() && space_seen != 0) {
                        c = Token.tLBRACK;
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return c;
                case '{' :
                    if (ph.getLexState() != LexState.EXPR_END && ph.getLexState() != LexState.EXPR_ARG) {

                        c = Token.tLBRACE;
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return c;
                case '\\' :
                    c = nextc();
                    if (c == '\n') {
                        space_seen = 1;
                        continue retry; // skip \\n
                    }
                    pushback(c);
                    return '\\';
                case '%' :
                        if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID) {
                            return parseQuotation(nextc());
                        }
                        if ((c = nextc()) == '=') {
                            yyVal = "%"; //ph.newId('%');
                            return Token.tOP_ASGN;
                        }
                        if (isArgState() && space_seen != 0 && !isSpace(c)) {
                            return parseQuotation(c);
                        }
                    ph.setLexState(LexState.EXPR_BEG);
                    pushback(c);
                    return '%';
                case '$' :
                    ph.setLexState(LexState.EXPR_END);
                    newtok();
                    c = nextc();
                    switch (c) {
                        case '_' : // $_: last read line string
                            c = nextc();
                            if (isIdentifierChar(c)) {
                                tokadd('$');
                                tokadd('_');
                                break;
                            }
                            pushback(c);
                            c = '_';
                            // fall through
                        case '~' : // $~: match-data
                            ph.getLocalIndex(String.valueOf(c));
                            // fall through
                        case '*' : // $*: argv
                        case '$' : // $$: pid
                        case '?' : // $?: last status
                        case '!' : // $!: error string
                        case '@' : // $@: error position
                        case '/' : // $/: input record separator
                        case '\\' : // $\: output record separator
                        case ';' : // $;: field separator
                        case ',' : // $,: output field separator
                        case '.' : // $.: last read line number
                        case '=' : // $=: ignorecase
                        case ':' : // $:: load path
                        case '<' : // $<: reading filename
                        case '>' : // $>: default output handle
                        case '\"' : // $": already loaded files
                            tokadd('$');
                            tokadd(c);
                            yyVal = tok();
                            return Token.tGVAR;
                        case '-' :
                            tokadd('$');
                            tokadd(c);
                            c = nextc();
                            tokadd(c);
                            yyVal = tok();
                            /* xxx shouldn't check if valid option variable */
                            return Token.tGVAR;
                        case '&' : // $&: last match
                        case '`' : // $`: string before last match
                        case '\'' : // $': string after last match
                        case '+' : // $+: string matches last paren.
                            yyVal = nf.newBackRef(c);
                            return Token.tBACK_REF;
                        case '1' :
                        case '2' :
                        case '3' :
                        case '4' :
                        case '5' :
                        case '6' :
                        case '7' :
                        case '8' :
                        case '9' :
                            tokadd('$');
                            while (Character.isDigit((char) c)) {
                                tokadd(c);
                                c = nextc();
                            }
                            if (isIdentifierChar(c)) {
                                break;
                            }
                            pushback(c);
                            yyVal = nf.newNthRef(Integer.parseInt(tok().substring(1)));
                            return Token.tNTH_REF;
                        default :
                            if (!isIdentifierChar(c)) {
                                pushback(c);
                                return '$';
                            }
                        case '0' :
                            tokadd('$');
                    }
                    break;
                case '@' :
                    c = nextc();
                    newtok();
                    tokadd('@');
                    if (c == '@') {
                        tokadd('@');
                        c = nextc();
                    }
                    if (Character.isDigit((char) c)) {
                        ph.rb_compile_error("`@" + c + "' is not a valid instance variable name");
                    }
                    if (!isIdentifierChar(c)) {
                        pushback(c);
                        return '@';
                    }
                    break;
                default :
                    if (!isIdentifierChar(c) || Character.isDigit((char) c)) {
                        ph.rb_compile_error("Invalid char `\\" + c + "' in expression");
                        continue retry;
                    }

                    newtok();
                    break;
            }
            break retry;
        }

        while (isIdentifierChar(c)) {
            tokadd(c);
            c = nextc();
        }
        if ((c == '!' || c == '?') && isIdentifierChar(tok().charAt(0)) && !peek('=')) {
            tokadd(c);
        } else {
            pushback(c);
        }
        {
            int result = 0;

            switch (tok().charAt(0)) {
                case '$' :
                    ph.setLexState(LexState.EXPR_END);
                    result = Token.tGVAR;
                    break;
                case '@' :
                    ph.setLexState(LexState.EXPR_END);
                    if (tok().charAt(1) == '@') {
                        result = Token.tCVAR;
                    } else {
                        result = Token.tIVAR;
                    }
                    break;
                default :
                    if (ph.getLexState() != LexState.EXPR_DOT) {
                        // See if it is a reserved word.
                        kw = getKeyword(tok(), toklen());
                        if (kw != null) {
                            // enum lex_state
                            int state = ph.getLexState();
                            ph.setLexState(kw.state);
                            if (state == LexState.EXPR_FNAME) {
                                yyVal = kw.name; // ruby.intern(kw.name);
                            }
                            if (kw.id0 == Token.kDO) {
                                if (COND_P()) {
                                    return Token.kDO_COND;
                                }
                                if (CMDARG_P()) {
                                    return Token.kDO_BLOCK;
                                }
                                return Token.kDO;
                            }
                            if (state == LexState.EXPR_BEG) {
                                return kw.id0;
                            } else {
                                if (kw.id0 != kw.id1) {
                                    ph.setLexState(LexState.EXPR_BEG);
                                }
                                return kw.id1;
                            }
                        }
                    }

                    if (toklast() == '!' || toklast() == '?') {
                        result = Token.tFID;
                    } else {
                        if (ph.getLexState() == LexState.EXPR_FNAME) {
                            if ((c = nextc()) == '=' && !peek('~') && !peek('>') && (!peek('=') || support.getCharAt(1) == '>')) {
                                result = Token.tIDENTIFIER;
                                tokadd(c);
                            } else {
                                pushback(c);
                            }
                        }
                        if (result == 0 && Character.isUpperCase(tok().charAt(0))) {
                            result = Token.tCONSTANT;
                        } else {
                            result = Token.tIDENTIFIER;
                        }
                    }
                    if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_DOT || ph.getLexState() == LexState.EXPR_ARG) {
                        ph.setLexState(LexState.EXPR_ARG);
                    } else {
                        ph.setLexState(LexState.EXPR_END);
                    }
            }
            yyVal = tok();
            return result;
        }
    }

    private int parseQuotation(int type) {
        int closeQuote;

        if (!Character.isLetterOrDigit((char) type)) {
            closeQuote = type;
            type = 'Q';
        } else {
            closeQuote = nextc();
        }

        if (type == -1 || closeQuote == -1) {
            ph.rb_compile_error("unterminated quoted string meets end of file");
            return 0;
        }

        int openQuote = closeQuote;
        if (closeQuote == '(') {
            closeQuote = ')';
        } else if (closeQuote == '[') {
            closeQuote = ']';
        } else if (closeQuote == '{') {
            closeQuote = '}';
        } else if (closeQuote == '<') {
            closeQuote = '>';
        } else {
            openQuote = 0;
        }

        switch (type) {
            case 'Q' :
                return parseString('"', closeQuote, openQuote);
            case 'q' :
                return parseSingleQuotedString(closeQuote, openQuote);
            case 'w' :
                return parseQuotedWords(closeQuote, openQuote);
            case 'x' :
                return parseString('`', closeQuote, openQuote);
            case 'r' :
                return parseRegexp(closeQuote, openQuote);
            default :
                yyerror("unknown type of %string");
                return 0;
        }
    }

    /**
     *  Description of the Method
     *
     *@param  c  Description of Parameter
     *@return    Description of the Returned Value
     */
    private int start_num(int c) {
        boolean is_float = false;
        boolean seen_point = false;
        boolean seen_e = false;
        boolean seen_uc = false;

        ph.setLexState(LexState.EXPR_END);
        newtok();
        if (c == '-' || c == '+') {
            tokadd(c);
            c = nextc();
        }
        if (c == '0') {
            c = nextc();
            if (c == 'x' || c == 'X') {
                //  hexadecimal
                c = nextc();
                do {
                    if (c == '_') {
                        seen_uc = true;
                        continue;
                    }
                    if (!isHexDigit(c)) {
                        break;
                    }
                    seen_uc = false;
                    tokadd(c);
                } while ((c = nextc()) != 0);
                pushback(c);
                if (toklen() == 0) {
                    yyerror("hexadecimal number without hex-digits");
                } else if (seen_uc) {
                    return decode_num(c, is_float, seen_uc, true);
                }
                yyVal = getNumFromString(tok(), 16);
                return Token.tINTEGER;
            }
            if (c == 'b' || c == 'B') {
                // binary
                c = nextc();
                do {
                    if (c == '_') {
                        seen_uc = true;
                        continue;
                    }
                    if (c != '0' && c != '1') {
                        break;
                    }
                    seen_uc = false;
                    tokadd(c);
                } while ((c = nextc()) != 0);
                pushback(c);
                if (toklen() == 0) {
                    yyerror("numeric literal without digits");
                } else if (seen_uc) {
                    return decode_num(c, is_float, seen_uc, true);
                }
                yyVal = getNumFromString(tok(), 2);
                return Token.tINTEGER;
            }
            if (c >= '0' && c <= '7' || c == '_') {
                // octal
                do {
                    if (c == '_') {
                        seen_uc = true;
                        continue;
                    }
                    if (c < '0' || c > '7') {
                        break;
                    }
                    seen_uc = false;
                    tokadd(c);
                } while ((c = nextc()) != 0);
                pushback(c);
                if (seen_uc) {
                    return decode_num(c, is_float, seen_uc, true);
                }
                yyVal = getNumFromString(tok(), 8);
                return Token.tINTEGER;
            }
            if (c > '7' && c <= '9') {
                yyerror("Illegal octal digit");
            } else if (c == '.') {
                tokadd('0');
            } else {
                pushback(c);
                yyVal = RubyFixnum.zero(ruby);
                return Token.tINTEGER;
            }
        }

        for (;;) {
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
                    seen_uc = false;
                    tokadd(c);
                    break;
                case '.' :
                    if (seen_point || seen_e) {
                        return decode_num(c, is_float, seen_uc);
                    } else {
                        int c0 = nextc();
                        if (!Character.isDigit((char) c0)) {
                            pushback(c0);
                            return decode_num(c, is_float, seen_uc);
                        }
                        c = c0;
                    }
                    tokadd('.');
                    tokadd(c);
                    is_float = true;
                    seen_point = true;
                    seen_uc = false;
                    break;
                case 'e' :
                case 'E' :
                    if (seen_e) {
                        return decode_num(c, is_float, seen_uc);
                    }
                    tokadd(c);
                    seen_e = true;
                    is_float = true;
                    while ((c = nextc()) == '_') {
                        seen_uc = true;
                    }
                    if (c == '-' || c == '+') {
                        tokadd(c);
                    } else {
                        continue;
                    }
                    break;
                case '_' : //  '_' in number just ignored
                    seen_uc = true;
                    break;
                default :
                    return decode_num(c, is_float, seen_uc);
            }
            c = nextc();
        }
    }

    private int decode_num(int c, boolean is_float, boolean seen_uc) {
        return decode_num(c, is_float, seen_uc, false);
    }

    private int decode_num(int c, boolean is_float, boolean seen_uc, boolean trailing_uc) {
        if (!trailing_uc) {
            pushback(c);
        }
        if (seen_uc || trailing_uc) {
            //trailing_uc:
            yyerror("trailing `_' in number");
        }
        if (is_float) {
            double d = 0.0;
            try {
                d = Double.parseDouble(tok());
            } catch (NumberFormatException e) {
                ph.rb_warn("Float " + tok() + " out of range");
            }
            yyVal = RubyFloat.newFloat(ruby, d);
            return Token.tFLOAT;
        }
        yyVal = getNumFromString(tok(), 10);
        return Token.tINTEGER;
    }

    /**
     *  This methods parse the #{}, #@ and #$ in strings.
     *
     *@param  list  Description of Parameter
     *@param  term  Description of Parameter
     *@return       Description of the Returned Value
     */
    private Node parseExpressionString(Node list, int closeQuote) {
        int c;
        int brace = -1;
        RubyObject ss;
        Node node;
        int nest;

        c = nextc();
        switch (c) {
            case '$' :
            case '@' :
            case '{' :
                break;
            default :
                tokadd('#');
                pushback(c);
                return list;
        }

        ss = RubyString.newString(ruby, tok(), toklen());
        if (list == null) {
            list = nf.newDStr(ss);
        } else if (toklen() > 0) {
            ph.list_append(list, nf.newStr(ss));
        }
        newtok();

        fetch_id : for (;;) {
            switch (c) {
                case '$' :
                    tokadd('$');
                    c = nextc();
                    if (c == -1) {
                        return Node.MINUS_ONE;
                    }
                    switch (c) {
                        case '1' :
                        case '2' :
                        case '3' :
                        case '4' :
                        case '5' :
                        case '6' :
                        case '7' :
                        case '8' :
                        case '9' :
                            while (Character.isDigit((char) c)) {
                                tokadd(c);
                                c = nextc();
                            }
                            pushback(c);
                            break fetch_id;
                        case '&' :
                        case '+' :
                        case '_' :
                        case '~' :
                        case '*' :
                        case '$' :
                        case '?' :
                        case '!' :
                        case '@' :
                        case ',' :
                        case '.' :
                        case '=' :
                        case ':' :
                        case '<' :
                        case '>' :
                        case '\\' :
                            //refetch:
                            tokadd(c);
                            break fetch_id;
                        default :
                            if (c == closeQuote) {
                                ph.list_append(list, nf.newStr(RubyString.newString(ruby, "#$")));
                                pushback(c);
                                newtok();
                                return list;
                            }
                            switch (c) {
                                case '\"' :
                                case '/' :
                                case '\'' :
                                case '`' :
                                    //goto refetch;
                                    tokadd(c);
                                    break fetch_id;
                            }
                            if (!isIdentifierChar(c)) {
                                yyerror("bad global variable in string");
                                newtok();
                                return list;
                            }
                    }

                    while (isIdentifierChar(c)) {
                        tokadd(c);
                        c = nextc();
                    }
                    pushback(c);
                    break;
                case '@' :
                    tokadd(c);
                    c = nextc();
                    if (c == '@') {
                        tokadd(c);
                        c = nextc();
                    }
                    while (isIdentifierChar(c)) {
                        tokadd(c);
                        c = nextc();
                    }
                    pushback(c);
                    break;
                case '{' :
                    if (c == '{') {
                        brace = '}';
                    }
                    nest = 0;
                    do {
                        loop_again : for (;;) {
                            c = nextc();
                            switch (c) {
                                case -1 :
                                    if (nest > 0) {
                                        yyerror("bad substitution in string");
                                        newtok();
                                        return list;
                                    }
                                    return Node.MINUS_ONE;
                                case '}' :
                                    if (c == brace) {
                                        if (nest == 0) {
                                            break;
                                        }
                                        nest--;
                                    }
                                    tokadd(c);
                                    continue loop_again;
                                case '\\' :
                                    c = nextc();
                                    if (c == -1) {
                                        return Node.MINUS_ONE;
                                    }
                                    if (c == closeQuote) {
                                        tokadd(c);
                                    } else {
                                        tokadd('\\');
                                        tokadd(c);
                                    }
                                    break;
                                case '{' :
                                    if (brace != -1) {
                                        nest++;
                                    }
                                case '\"' :
                                case '/' :
                                case '`' :
                                    if (c == closeQuote) {
                                        pushback(c);
                                        ph.list_append(list, nf.newStr(RubyString.newString(ruby, "#")));
                                        ph.rb_warning("bad substitution in string");
                                        ph.list_append(list, nf.newStr(RubyString.newString(ruby, tok(), toklen())));
                                        newtok();
                                        return list;
                                    }
                                default :
                                    tokadd(c);
                                    break;
                            }
                            break loop_again;
                        }
                    } while (c != brace);
            }
            break;
        }

        //fetch_id:
        node = nf.newEVStr(tok(), toklen());
        ph.list_append(list, node);
        newtok();

        return list;
    }

    // Helper functions....................

    private final boolean isArgState() {
        return ph.getLexState() == LexState.EXPR_ARG;
    }

    private static final Keyword getKeyword(String w, int len) {
        return Keyword.rb_reserved_word(w, len);
    }

    private final RubyInteger getNumFromString(String s, int radix) {
        return RubyNumeric.str2inum(ruby, RubyString.newString(ruby, s), radix);
    }

    private final RubyRegexp newRegexp(String s, int len, int options) {
        return RubyRegexp.newRegexp(ruby, RubyString.newString(ruby, s, len), options);
    }

    private final void yyerror(String msg) {
        ph.rb_errmess(msg);
    }

    /**
     *  Returns true if "ch" is a valid identifier character (letter, digit or
     *  underscore)
     */
    private static final boolean isIdentifierChar(int ch) {
        return Character.isLetterOrDigit((char) ch) || ch == '_';
    }

    /**
     *  Returns true if "c" is a white space character.
     *
     */
    private static final boolean isSpace(int c) {
        return Character.isWhitespace((char) c);
    }

    /**
     *  Returns true if "c" is a hex-digit.
     *
     */
    private static final boolean isHexDigit(int c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
    }

    /**
     * Gets the source.
     * @return Returns a IScannerSource
     */
    public IScannerSource getSource() {
        return source;
    }

    /**
     * Sets the source.
     * @param source The source to set
     */
    public void setSource(IScannerSource source) {
        // this.markFilterSource = new MarkFilterScannerSource(source);
        // this.source = new LineFilterScannerSource(markFilterSource);
        this.source = new LineBufferScannerSource(source);

        this.support = new DefaultScannerSupport(this.source);
    }
}