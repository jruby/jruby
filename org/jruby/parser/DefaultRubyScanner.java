/*
 * DefaultRubyScanner.java - No description
 * Created on 08. Oktober 2001, 14:38
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
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

import org.jruby.*;
import org.jruby.nodes.*;
import org.jruby.runtime.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class DefaultRubyScanner implements DefaultRubyParser.yyInput {
    private Ruby ruby;
    private ParserHelper ph;
    private NodeFactory nf;
    
    // Scanner
    
    private int token = 0;              // yyInput implementation
    
    private String lex_curline;         // current line
    private int lex_pbeg;       
    private int lex_p;                  // pointer in current line
    private int lex_pend;               // pointer to end of line
    
    private int lex_gets_ptr;           // beginning of the next line
    private RubyObject lex_input;       // non-nil if File
    private RubyObject lex_lastline;

    private boolean lex_file_io; //true, if scanner source is a file and false, if lex_get_str() shall be used.

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
    
    private kwtable rb_reserved_word(String w, int len) {
        return kwtable.rb_reserved_word(w, len);
    }
        
    private RubyFixnum rb_cstr2inum(String s, int radix) {
        //XXX no support for _ or leading and trailing spaces
        return RubyFixnum.m_newFixnum(ruby, Integer.parseInt(s, radix));
    }
    
    private RubyRegexp rb_reg_new(String s, int len, int options) {
        return RubyRegexp.m_newRegexp(ruby, RubyString.m_newString(ruby, s, len), options);
    }
    
    private void yyerror(String msg) {
        System.err.println(msg);
    }
    
    /**
     *  Returns true if "c" is a valid identifier character (letter, digit or
     *  underscore)
     *
     *@param  ch  Description of Parameter
     *@return     Description of the Returned Value
     */
    private boolean is_identchar(int ch) {
        return Character.isLetterOrDigit((char) ch) || ch == '_';
    }

    /* gc protect */
    private RubyObject lex_gets_str(RubyObject _s) {
        String s = ((RubyString)_s).getValue();
        if (lex_gets_ptr != 0) {
            if (s.length() == lex_gets_ptr) {
                return ruby.getNil();
            }
            s = s.substring(lex_gets_ptr);
        }
        int end = 0;
        while (end < s.length()) {
            if (s.charAt(end++) == '\n') {
                break;
            }
        }
        lex_gets_ptr += end;
        return RubyString.m_newString(ruby, s, end);
    }

    /**
     *  Returns in next line either from file or from a string.
     *
     *@return    Description of the Returned Value
     */
    private RubyObject lex_getline() {
        RubyObject line;
        if (lex_file_io) {
            // uses rb_io_gets(lex_input)
            throw new Error();
        } else {
            line = lex_gets_str(lex_input);
        }
        if (ph.getRubyDebugLines() != null && !line.isNil()) {
            ph.getRubyDebugLines().m_push(line);
        }
        return line;
    }


    /**
     *  Returns the next character from input
     *
     *@return    Description of the Returned Value
     */
    private int nextc() {
        int c;

        if (lex_p == lex_pend) {
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
                lex_curline = ((RubyString)v).getValue();
                lex_p = lex_pbeg = 0;
                lex_pend = lex_curline.length();
                if (lex_curline.startsWith("__END__") && (lex_pend == 7 || lex_curline.charAt(7) == '\n' || lex_curline.charAt(7) == '\r')) {
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
        c = lex_curline.charAt(lex_p++);
        if (c == '\r' && lex_p <= lex_pend && lex_curline.charAt(lex_p) == '\n') {
            lex_p++;
            c = '\n';
        }

        return c;
    }

    /**
     *  Puts back the given character so that nextc() will answer it next time
     *  it'll be called.
     *
     *@param  c  Description of Parameter
     */
    private void pushback(int c) {
        if (c == -1) {
            return;
        }
        lex_p--;
    }

    /**
     *  Returns true if the given character is the current one in the input
     *  stream
     *
     *@param  c  Description of Parameter
     *@return    Description of the Returned Value
     */
    private boolean peek(int c) {
        return lex_p != lex_pend && c == lex_curline.charAt(lex_p);
    }

    private String tok() {
        return tokenbuf.toString();
    }

    private int toklen() {
        return tokenbuf.length();
    }

    private void tokfix() { }

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
            case '\\': // Backslash
                return c;
            case 'n':  // newline
                return '\n';
            case 't':  // horizontal tab
                return '\t';
            case 'r':  // carriage-return
                return '\r';
            case 'f':  // form-feed
                return '\f';
            case 'v':  // vertical tab
                return '\013';
            case 'a':  // alarm(bell)
                return '\007';
            case 'e':  // escape
                return '\033';
            case '0': case '1': case '2': case '3': // octal constant
            case '4': case '5': case '6': case '7':
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
            case 'x': // hex constant
            {
                int[] numlen = new int[1];
                c = (int) scan_hex(lex_curline, lex_p, 2, numlen);
                lex_p += numlen[0];
            }
                return c;
            case 'b':   // backspace
                return '\010';
            case 's':   // space
                return ' ';
            case 'M':
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

            case 'C':
                if ((c = nextc()) != '-') {
                    yyerror("Invalid escape character syntax");
                    pushback(c);
                    return '\0';
                }
            case 'c':
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
            case -1:
                // eof:
                yyerror("Invalid escape character syntax");
                return '\0';
            default:
                return c;
        }
    }

    private int tokadd_escape(int term) {
        /*
         *  FIX 1.6.5
         */
        int c;

        switch (c = nextc()) {
            case '\n':
                return 0;
            /*
             *  just ignore
             */
            case '0':
            case '1':
            case '2':
            case '3':
            /*
             *  octal constant
             */
            case '4':
            case '5':
            case '6':
            case '7':
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
            case 'x': // hex constant
            {
                tokadd('\\');
                tokadd(c);

                int[] numlen = new int[1];
                scan_hex(lex_curline, lex_p, 2, numlen);
                while (numlen[0]-- != 0) {
                    tokadd(nextc());
                }
            }
                return 0;
            case 'M':
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
            case 'C':
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
            case 'c':
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
            case -1:
                // eof:
                yyerror("Invalid escape character syntax");
                return -1;
            default:
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

    private int parse_regx(int term, int paren) {
        int c;
        char kcode = 0;
        boolean once = false;
        int nest = 0;
        int options = 0;
        int re_start = ruby.getSourceLine();
        Node list = null;

        newtok();
        regx_end :
        while ((c = nextc()) != -1) {
            if (c == term && nest == 0) {
                break regx_end;
            }

            switch (c) {
                case '#':
                    list = str_extend(list, term);
                    if (list == Node.MINUS_ONE) {
                        return 0;
                    }
                    continue;
                case '\\':
                    if (tokadd_escape(term) < 0) {
                        /*
                         *  FIX 1.6.5
                         */
                        return 0;
                    }
                    continue;
                case -1:
                    //goto unterminated;
                    ruby.setSourceLine(re_start);
                    ph.rb_compile_error("unterminated regexp meets end of file");
                    return 0;
                default:
                    if (paren != 0) {
                        if (c == paren) {
                            nest++;
                        }
                        if (c == term) {
                            nest--;
                        }
                    }
                    break;
            }
            tokadd(c);
        }

        end_options :
        for (; ; ) {
            switch (c = nextc()) {
                case 'i':
                    options |= ReOptions.RE_OPTION_IGNORECASE;
                    break;
                case 'x':
                    options |= ReOptions.RE_OPTION_EXTENDED;
                    break;
                case 'p': // /p is obsolete
                    ph.rb_warn("/p option is obsolete; use /m\n\tnote: /m does not change ^, $ behavior");
                    options |= ReOptions.RE_OPTION_POSIXLINE;
                    break;
                case 'm':
                    options |= ReOptions.RE_OPTION_MULTILINE;
                    break;
                case 'o':
                    once = true;
                    break;
                case 'n':
                    kcode = 16;
                    break;
                case 'e':
                    kcode = 32;
                    break;
                case 's':
                    kcode = 48;
                    break;
                case 'u':
                    kcode = 64;
                    break;
                default:
                    pushback(c);
                    break end_options;
            }
        }

        tokfix();
        ph.setLexState(LexState.EXPR_END);
        if (list != null) {
            list.setLine(re_start);
            if (toklen() > 0) {
                RubyObject ss = RubyString.m_newString(ruby, tok(), toklen());
                ph.list_append(list, nf.newStr(ss));
            }
            new RuntimeException("Want to change " + list.getClass().getName() + "to DRegxNode").printStackTrace();
            // list.nd_set_type(once ? NODE.NODE_DREGX_ONCE : NODE.NODE_DREGX);
            list.setCFlag(options | kcode);
            yyVal = list;
            return Token.tDREGEXP;
        } else {
            yyVal = rb_reg_new(tok(), toklen(), options | kcode);
            return Token.tREGEXP;
        }
        //unterminated:
        //ruby_sourceline = re_start;
        //rb_compile_error("unterminated regexp meets end of file");
        //return 0;
    }

    private int parse_string(int func, int term, int paren) {
        int c;
        Node list = null;
        int strstart;
        int nest = 0;

        if (func == '\'') {
            return parse_qstring(term, paren);
        }
        if (func == 0) {
            // read 1 line for heredoc
            // -1 for chomp
            yyVal = RubyString.m_newString(ruby, lex_curline, lex_pend - 1);
            lex_p = lex_pend;
            return Token.tSTRING;
        }
        strstart = ruby.getSourceLine();
        newtok();
        while ((c = nextc()) != term || nest > 0) {
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
                list = str_extend(list, term);
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
                if (c == term) {
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
            if (paren != 0) {
                if (c == paren) {
                    nest++;
                }
                if (c == term && nest-- == 0) {
                    break;
                }
            }
            tokadd(c);
        }

        tokfix();
        ph.setLexState(LexState.EXPR_END);

        if (list != null) {
            list.setLine(strstart);
            if (toklen() > 0) {
                RubyObject ss = RubyString.m_newString(ruby, tok(), toklen());
                ph.list_append(list, nf.newStr(ss));
            }
            yyVal = list;
            if (func == '`') {
                new RuntimeException("[BUG] want to change " + list.getClass().getName() + " to DXStrNode").printStackTrace();
                // list.nd_set_type(Constants.NODE_DXSTR);
                return Token.tDXSTRING;
            } else {
                return Token.tDSTRING;
            }
        } else {
            yyVal = RubyString.m_newString(ruby, tok(), toklen());
            return (func == '`') ? Token.tXSTRING : Token.tSTRING;
        }
    }

    private int parse_qstring(int term, int paren) {
        int strstart;
        int c;
        int nest = 0;

        strstart = ruby.getSourceLine();
        newtok();
        while ((c = nextc()) != term || nest > 0) {
            if (c == -1) {
                ruby.setSourceLine(strstart);
                ph.rb_compile_error("unterminated string meets end of file");
                return 0;
            }
            if (c == '\\') {
                c = nextc();
                switch (c) {
                    case '\n':
                        continue;
                    case '\\':
                        c = '\\';
                        break;
                    default:
                        // fall through
                        if (c == term || (paren != 0 && c == paren)) {
                            tokadd(c);
                            continue;
                        }
                        tokadd('\\');
                }
            }
            if (paren != 0) {
                if (c == paren) {
                    nest++;
                }
                if (c == term && nest-- == 0) {
                    break;
                }
            }
            tokadd(c);
        }

        tokfix();
        yyVal = RubyString.m_newString(ruby, tok(), toklen());
        ph.setLexState(LexState.EXPR_END);
        return Token.tSTRING;
    }

    private int parse_quotedwords(int term, int paren) {
        Node qwords = null;
        int strstart;
        int c;
        int nest = 0;

        strstart = ruby.getSourceLine();
        newtok();

        c = nextc();
        while (ISSPACE(c)) {
            c = nextc();
        }
        /*
         *  skip preceding spaces
         */
        pushback(c);
        while ((c = nextc()) != term || nest > 0) {
            if (c == -1) {
                ruby.setSourceLine(strstart);
                ph.rb_compile_error("unterminated string meets end of file");
                return 0;
            }
            if (c == '\\') {
                c = nextc();
                switch (c) {
                    case '\n':
                        continue;
                    case '\\':
                        c = '\\';
                        break;
                    default:
                        if (c == term || (paren != 0 && c == paren)) {
                            tokadd(c);
                            continue;
                        }
                        if (!ISSPACE(c)) {
                            tokadd('\\');
                        }
                        break;
                }
            } else if (ISSPACE(c)) {

                tokfix();
                Node str = nf.newStr(RubyString.m_newString(ruby, tok(), toklen()));
                newtok();
                if (qwords == null) {
                    qwords = nf.newList(str);
                } else {
                    ph.list_append(qwords, str);
                }
                c = nextc();
                while (ISSPACE(c)) {
                    c = nextc();
                }
                // skip continuous spaces
                pushback(c);
                continue;
            }
            if (paren != 0) {
                if (c == paren) {
                    nest++;
                }
                if (c == term && nest-- == 0) {
                    break;
                }
            }
            tokadd(c);
        }

        tokfix();
        if (toklen() > 0) {
            Node str = nf.newStr(RubyString.m_newString(ruby, tok(), toklen()));
            if (qwords == null) {
                qwords = nf.newList(str);
            } else {
                ph.list_append(qwords, str);
            }
        }
        if (qwords == null) {
            qwords = nf.newZArray();
        }
        yyVal = qwords;
        ph.setLexState(LexState.EXPR_END);
        return Token.tDSTRING;
    }

    /*private int here_document(int term, int indent) {
        throw new Error("not supported yet");
    }*/

    private int here_document(int term, int indent) {
        int c;
        Node list = null;
        
        int linesave = ruby.getSourceLine();
        newtok();
 
        switch (term) {
            case '\'':
            case '"':
            case '`':
                while ((c = nextc()) != term) {
                    tokadd(c);
                }
                if (term == '\'') {
                    term = 0;
                }
                break;
            default:
                c = term;
                term = '"';
                if (!is_identchar(c)) {
                    ph.rb_warn("use of bare << to mean <<\"\" is deprecated");
                    break;
                }
                
                while (is_identchar(c)) {
                    tokadd(c);
                    c = nextc();
                }
                pushback(c);
                break;
        }
        tokfix();
        RubyObject lastline_save = lex_lastline;
        int offset_save = lex_p - lex_pbeg;
        String eos = tok();
        int len = eos.length();
        RubyString str = RubyString.m_newString(ruby, "");
        
        RubyObject line;
        
        while (true) {
            lex_lastline = line = lex_getline();
            if (line.isNil()) {
                // error:
                ruby.setSourceLine(linesave);
                ph.rb_compile_error("can't find string \"" + eos + "\" anywhere before EOF");
                return 0;
            }
            ruby.setSourceLine(ruby.getSourceLine());
            String p = ((RubyString)line).getValue();
            if (indent != 0) {
                while (Character.isWhitespace(p.charAt(0))) {
                    p = p.substring(1);
                }
                //  while (*p && (*p == ' ' || *p == '\t')) {
		//      p++;
                //  } 
            }
            if (p/*.trim()*/.startsWith(eos)) {
                if (p.charAt(len) == '\n' || p.charAt(len) == '\r') {
                    break;
                }
                if (len == ((RubyString)line).getValue().length()) {
                    break;
                }
            }
            lex_curline = ((RubyString)line).getValue();
            lex_pbeg = lex_p = 0;
            lex_pend = lex_curline.length();

            while (true) {
                switch (parse_string(term, '\n', '\n')) {
                    case Token.tSTRING:
                    case Token.tXSTRING:
                        ((RubyString)yyVal).m_cat("\n");
                        if (list == null) {
                            str.m_concat((RubyString)yyVal);
                        } else {
                            ph.list_append(list, nf.newStr((RubyObject)yyVal));
                        }
                        break;
                    case Token.tDSTRING:
                        if (list == null) {
                            list = nf.newDStr(str);
                        }
                        /* fall through */
                    case Token.tDXSTRING:
                        if (list == null) {
                            list = nf.newDXStr(str);
                        }
                        ph.list_append((Node)yyVal, nf.newStr(RubyString.m_newString(ruby, "\n")));
                        
                        new RuntimeException("[BUG] Want to convert" + yyVal.getClass().getName() + " to StrNode").printStackTrace();
                        // ((NODE)yyVal).nd_set_type(NODE.NODE_STR);
                        yyVal = nf.newList((Node)yyVal);
                        ((Node)yyVal).setNextNode(((Node)yyVal).getHeadNode().getNextNode());
                        ph.list_concat(list, (Node)yyVal);
                        break;
                    case 0:
                        // goto error;
                        ruby.setSourceLine(linesave);
                        ph.rb_compile_error("can't find string \"" + eos + "\" anywhere before EOF");
                        return 0;
                }
                if (lex_p == lex_pend) {
                    break;
                }
            }
        }
        
        lex_lastline = lastline_save;
        
        lex_curline = ((RubyString)lex_lastline).getValue();
        lex_pbeg = 0;
        lex_pend = lex_pbeg + lex_curline.length();
        lex_p = lex_pbeg + offset_save;
        ph.setLexState(LexState.EXPR_END);
        ph.setHeredocEnd(ruby.getSourceLine());
        ruby.setSourceLine(linesave);
            
        if (list != null) {
            list.setLine(linesave + 1);
            yyVal = list;
        }
        switch (term) {
            case '\0':
            case '\'':
            case '"':
                if (list != null) {
                    return Token.tDSTRING;
                }
                yyVal = str;
                return Token.tSTRING;
            case '`':
                if (list != null) {
                    return Token.tDXSTRING;
                }
                yyVal = str;
                return Token.tXSTRING;
        }
        return 0;
    }

    private void arg_ambiguous() {
        ph.rb_warning("ambiguous first argument; make sure");
    }


    private boolean IS_ARG() {
        return ph.getLexState() == LexState.EXPR_ARG;
    }


    /**
     *  Returns the next token. Also sets yyVal is needed.
     *
     *@return    Description of the Returned Value
     */
    private int yylex() {
        int c;
        int space_seen = 0;
        kwtable kw;

        retry :
        for (; ; ) {
            switch (c = nextc()) {
                case '\0': // NUL
                case '\004': // ^D
                case '\032': // ^Z
                case -1: //end of script.
                    return 0;
                // white spaces
                case ' ':
                case '\t':
                case '\f':
                case '\r':
                case '\013': // '\v'
                    space_seen++;
                    continue retry;
                case '#': // it's a comment
                    while ((c = nextc()) != '\n') {
                        if (c == -1) {
                            return 0;
                        }
                    }
                    // fall through
                case '\n':
                    switch (ph.getLexState()) {
                        case LexState.EXPR_BEG:
                        case LexState.EXPR_FNAME:
                        case LexState.EXPR_DOT:
                            continue retry;
                        default:
                            break;
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return '\n';
                case '*':
                    if ((c = nextc()) == '*') {
                        ph.setLexState(LexState.EXPR_BEG);
                        if (nextc() == '=') {
                            yyVal = ph.newId(Token.tPOW);
                            return Token.tOP_ASGN;
                        }
                        pushback(c);
                        return Token.tPOW;
                    }
                    if (c == '=') {
                        yyVal = ph.newId('*');
                        ph.setLexState(LexState.EXPR_BEG);
                        return Token.tOP_ASGN;
                    }
                    pushback(c);
                    if (IS_ARG() && space_seen != 0 && !ISSPACE(c)) {
                        ph.rb_warning("`*' interpreted as argument prefix");
                        c = Token.tSTAR;
                    } else if (ph.getLexState() == LexState.EXPR_BEG || 
                               ph.getLexState() == LexState.EXPR_MID) {
                        c = Token.tSTAR;
                    } else {
                        c = '*';
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return c;
                case '!':
                    ph.setLexState(LexState.EXPR_BEG);
                    if ((c = nextc()) == '=') {
                        return Token.tNEQ;
                    }
                    if (c == '~') {
                        return Token.tNMATCH;
                    }
                    pushback(c);
                    return '!';
                case '=':
                    if (lex_p == 1) {
                        // skip embedded rd document
                        if (lex_curline.startsWith("=begin") && (lex_pend == 6 || ISSPACE(lex_curline.charAt(6)))) {
                            for (; ; ) {
                                lex_p = lex_pend;
                                c = nextc();
                                if (c == -1) {
                                    ph.rb_compile_error("embedded document meets end of file");
                                    return 0;
                                }
                                if (c != '=') {
                                    continue;
                                }
                                if (lex_curline.substring(lex_p, lex_p + 3).equals("end") &&
                                        (lex_p + 3 == lex_pend || ISSPACE(lex_curline.charAt(lex_p + 3)))) {
                                    break;
                                }
                            }
                            lex_p = lex_pend;
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
                case '<':
                    c = nextc();
                    if (c == '<' && 
                            ph.getLexState() != LexState.EXPR_END &&
                            ph.getLexState() != LexState.EXPR_ENDARG &&
                            ph.getLexState() != LexState.EXPR_CLASS &&
                            (!IS_ARG() || space_seen != 0)) {
                        int c2 = nextc();
                        int indent = 0;
                        if (c2 == '-') {
                            indent = 1;
                            c2 = nextc();
                        }
                        if (!ISSPACE(c2) && ("\"'`".indexOf(c2) != -1 || is_identchar(c2))) {
                            return here_document(c2, indent);
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
                            yyVal = ph.newId(Token.tLSHFT);
                            return Token.tOP_ASGN;
                        }
                        pushback(c);
                        return Token.tLSHFT;
                    }
                    pushback(c);
                    return '<';
                case '>':
                    ph.setLexState(LexState.EXPR_BEG);
                    if ((c = nextc()) == '=') {
                        return Token.tGEQ;
                    }
                    if (c == '>') {
                        if ((c = nextc()) == '=') {
                            yyVal = ph.newId(Token.tRSHFT);
                            return Token.tOP_ASGN;
                        }
                        pushback(c);
                        return Token.tRSHFT;
                    }
                    pushback(c);
                    return '>';
                case '"':
                    return parse_string(c, c, c);
                case '`':
                    if (ph.getLexState() == LexState.EXPR_FNAME) {
                        return c;
                    }
                    if (ph.getLexState() == LexState.EXPR_DOT) {
                        return c;
                    }
                    return parse_string(c, c, c);
                case '\'':
                    return parse_qstring(c, 0);
                case '?':
                    if (ph.getLexState() == LexState.EXPR_END) {
                        ph.setLexState(LexState.EXPR_BEG);
                        return '?';
                    }
                    c = nextc();
                    if (c == -1) { /* FIX 1.6.5 */
                        ph.rb_compile_error("incomplete character syntax");
                        return 0;
                    }
                    if (IS_ARG() && ISSPACE(c)) {
                        pushback(c);
                        ph.setLexState(LexState.EXPR_BEG);
                        return '?';
                    }
                    if (c == '\\') {
                        c = read_escape();
                    }
                    c &= 0xff;
                    yyVal = RubyFixnum.m_newFixnum(ruby, c);
                    ph.setLexState(LexState.EXPR_END);
                    return Token.tINTEGER;
                case '&':
                    if ((c = nextc()) == '&') {
                        ph.setLexState(LexState.EXPR_BEG);
                        if ((c = nextc()) == '=') {
                            yyVal = ph.newId(Token.tANDOP);
                            return Token.tOP_ASGN;
                        }
                        pushback(c);
                        return Token.tANDOP;
                    } else if (c == '=') {
                        yyVal = ph.newId('&');
                        ph.setLexState(LexState.EXPR_BEG);
                        return Token.tOP_ASGN;
                    }
                    pushback(c);
                    if (IS_ARG() && space_seen != 0 && !ISSPACE(c)) {
                        ph.rb_warning("`&' interpeted as argument prefix");
                        c = Token.tAMPER;
                    } else if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID) {
                        c = Token.tAMPER;
                    } else {
                        c = '&';
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return c;
                case '|':
                    ph.setLexState(LexState.EXPR_BEG);
                    if ((c = nextc()) == '|') {
                        if ((c = nextc()) == '=') {
                            yyVal = ph.newId(Token.tOROP);
                            return Token.tOP_ASGN;
                        }
                        pushback(c);
                        return Token.tOROP;
                    } else if (c == '=') {
                        yyVal = ph.newId('|');
                        return Token.tOP_ASGN;
                    }
                    pushback(c);
                    return '|';
                case '+':
                    c = nextc();
                    if (ph.getLexState() == LexState.EXPR_FNAME || 
                        ph.getLexState() == LexState.EXPR_DOT) {
                        if (c == '@') {
                            return Token.tUPLUS;
                        }
                        pushback(c);
                        return '+';
                    }
                    if (c == '=') {
                        ph.setLexState(LexState.EXPR_BEG);
                        yyVal = ph.newId('+');
                        return Token.tOP_ASGN;
                    }
                    if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID ||
                            (IS_ARG() && space_seen != 0 && !ISSPACE(c))) {
                        if (IS_ARG()) {
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
                case '-':
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
                        yyVal = ph.newId('-');
                        return Token.tOP_ASGN;
                    }
                    if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID ||
                            (IS_ARG() && space_seen != 0 && !ISSPACE(c))) {
                        if (IS_ARG()) {
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
                case '.':
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
                case '0':
                case '1': case '2': case '3':
                case '4': case '5': case '6':
                case '7': case '8': case '9':
                    return start_num(c);
                case ']':
                case '}':
                    ph.setLexState(LexState.EXPR_END);
                    return c;
                case ')':
                    if (cond_nest > 0) {
	                    cond_stack >>= 1;
	                }
                    ph.setLexState(LexState.EXPR_END);
                    return c;
                case ':':
                    c = nextc();
                    if (c == ':') {
                        if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID ||
                                (IS_ARG() && space_seen != 0)) {
                            ph.setLexState(LexState.EXPR_BEG);
                            return Token.tCOLON3;
                        }
                        ph.setLexState(LexState.EXPR_DOT);
                        return Token.tCOLON2;
                    }
                    pushback(c);
                    if (ph.getLexState() == LexState.EXPR_END || ISSPACE(c)) {
                        ph.setLexState(LexState.EXPR_BEG);
                        return ':';
                    }
                    ph.setLexState(LexState.EXPR_FNAME);
                    return Token.tSYMBEG;
                case '/':
                    if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID) {
                        return parse_regx('/', '/');
                    }
                    if ((c = nextc()) == '=') {
                        ph.setLexState(LexState.EXPR_BEG);
                        yyVal = ph.newId('/');
                        return Token.tOP_ASGN;
                    }
                    pushback(c);
                    if (IS_ARG() && space_seen != 0) {
                        if (!ISSPACE(c)) {
                            arg_ambiguous();
                            return parse_regx('/', '/');
                        }
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return '/';
                case '^':
                    ph.setLexState(LexState.EXPR_BEG);
                    if ((c = nextc()) == '=') {
                        yyVal = ph.newId('^');
                        return Token.tOP_ASGN;
                    }
                    pushback(c);
                    return '^';
                case ',':
                case ';':
                    ph.setLexState(LexState.EXPR_BEG);
                    return c;
                case '~':
                    if (ph.getLexState() == LexState.EXPR_FNAME || ph.getLexState() == LexState.EXPR_DOT) {
                        if ((c = nextc()) != '@') {
                            pushback(c);
                        }
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return '~';
                case '(':
                    if (cond_nest > 0) {
	                    cond_stack = (cond_stack << 1 ) | 0;
	                }
                    if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID) {
                        c = Token.tLPAREN;
                    } else if (ph.getLexState() == LexState.EXPR_ARG && space_seen != 0) {
                        ph.rb_warning(tok() + " (...) interpreted as method call");
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return c;
                case '[':
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
                    } else if (IS_ARG() && space_seen != 0) {
                        c = Token.tLBRACK;
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return c;
                case '{':
                    if (ph.getLexState() != LexState.EXPR_END &&
                        ph.getLexState() != LexState.EXPR_ARG) {
                        
                        c = Token.tLBRACE;
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    return c;
                case '\\':
                    c = nextc();
                    if (c == '\n') {
                        space_seen = 1;
                        continue retry; // skip \\n
                    }
                    pushback(c);
                    return '\\';
                case '%':
                    quotation :
                    for (; ; ) {
                        if (ph.getLexState() == LexState.EXPR_BEG || ph.getLexState() == LexState.EXPR_MID) {
                            int term;
                            int paren;

                            c = nextc();
                            if (!Character.isLetterOrDigit((char) c)) {
                                term = c;
                                c = 'Q';
                            } else {
                                term = nextc();
                            }
                            if (c == -1 || term == -1) {
                                ph.rb_compile_error("unterminated quoted string meets end of file");
                                return 0;
                            }
                            paren = term;
                            if (term == '(') {
                                term = ')';
                            } else if (term == '[') {
                                term = ']';
                            } else if (term == '{') {
                                term = '}';
                            } else if (term == '<') {
                                term = '>';
                            } else {
                                paren = 0;
                            }

                            switch (c) {
                                case 'Q':
                                    return parse_string('"', term, paren);
                                case 'q':
                                    return parse_qstring(term, paren);
                                case 'w':
                                    return parse_quotedwords(term, paren);
                                case 'x':
                                    return parse_string('`', term, paren);
                                case 'r':
                                    return parse_regx(term, paren);
                                default:
                                    yyerror("unknown type of %string");
                                    return 0;
                            }
                        }
                        if ((c = nextc()) == '=') {
                            yyVal = ph.newId('%');
                            return Token.tOP_ASGN;
                        }
                        if (IS_ARG() && space_seen != 0 && !ISSPACE(c)) {
                            pushback(c);
                            continue quotation;
                        }
                        break quotation;
                    }
                    ph.setLexState(LexState.EXPR_BEG);
                    pushback(c);
                    return '%';
                case '$':
                    ph.setLexState(LexState.EXPR_END);
                    newtok();
                    c = nextc();
                    switch (c) {
                        case '_': // $_: last read line string
                            c = nextc();
                            if (is_identchar(c)) {
                                tokadd('$');
                                tokadd('_');
                                break;
                            }
                            pushback(c);
                            c = '_';
                        // fall through
                        case '~': // $~: match-data
                            ph.local_cnt(c);
                        // fall through
                        case '*': // $*: argv
                        case '$': // $$: pid
                        case '?': // $?: last status
                        case '!': // $!: error string
                        case '@': // $@: error position
                        case '/': // $/: input record separator
                        case '\\':// $\: output record separator
                        case ';': // $;: field separator
                        case ',': // $,: output field separator
                        case '.': // $.: last read line number
                        case '=': // $=: ignorecase
                        case ':': // $:: load path
                        case '<': // $<: reading filename
                        case '>': // $>: default output handle
                        case '\"':// $": already loaded files
                            tokadd('$');
                            tokadd(c);
                            tokfix();
                            yyVal = ruby.intern(tok());
                            return Token.tGVAR;
                        case '-':
                            tokadd('$');
                            tokadd(c);
                            c = nextc();
                            tokadd(c);
                            tokfix();
                            yyVal = ruby.intern(tok());
                            /* xxx shouldn't check if valid option variable */
                            return Token.tGVAR;
                        case '&':   // $&: last match
                        case '`':   // $`: string before last match
                        case '\'':  // $': string after last match
                        case '+':   // $+: string matches last paren.
                            yyVal = nf.newBackRef(c);
                            return Token.tBACK_REF;
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            tokadd('$');
                            while (Character.isDigit((char) c)) {
                                tokadd(c);
                                c = nextc();
                            }
                            if (is_identchar(c)) {
                                break;
                            }
                            pushback(c);
                            tokfix();
                            yyVal = nf.newNthRef(Integer.parseInt(tok().substring(1)));
                            return Token.tNTH_REF;
                        default:
                            if (!is_identchar(c)) {
                                pushback(c);
                                return '$';
                            }
                        case '0':
                            tokadd('$');
                    }
                    break;
                case '@':
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
                    if (!is_identchar(c)) {
                        pushback(c);
                        return '@';
                    }
                    break;
                default:
                    if (!is_identchar(c) || Character.isDigit((char) c)) {
                        ph.rb_compile_error("Invalid char `\\" + c + "' in expression");
                        continue retry;
                    }

                    newtok();
                    break;
            }
            break retry;
        }

        while (is_identchar(c)) {
            tokadd(c);
            c = nextc();
        }
        if ((c == '!' || c == '?') && is_identchar(tok().charAt(0)) && !peek('=')) {
            tokadd(c);
        } else {
            pushback(c);
        }
        tokfix();
         {
            int result = 0;

            switch (tok().charAt(0)) {
                case '$':
                    ph.setLexState(LexState.EXPR_END);
                    result = Token.tGVAR;
                    break;
                case '@':
                    ph.setLexState(LexState.EXPR_END);
                    if (tok().charAt(1) == '@') {
                        result = Token.tCVAR;
                    } else {
                        result = Token.tIVAR;
                    }
                    break;
                default:
                    if (ph.getLexState() != LexState.EXPR_DOT) {
                        // See if it is a reserved word.
                        kw = rb_reserved_word(tok(), toklen());
                        if (kw != null) {
                            // enum lex_state
                            int state = ph.getLexState();
                            ph.setLexState(kw.state);
                            if (state == LexState.EXPR_FNAME) {
                                yyVal = ruby.intern(kw.name);
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
                            if ((c = nextc()) == '=' && !peek('~') && !peek('>') &&
                                    (!peek('=') || lex_p + 1 < lex_pend && lex_curline.charAt(lex_p + 1) == '>')) {
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
                    if (ph.getLexState() == LexState.EXPR_BEG ||
                        ph.getLexState() == LexState.EXPR_DOT ||
                        ph.getLexState() == LexState.EXPR_ARG) {
                            ph.setLexState(LexState.EXPR_ARG);
                    } else {
                        ph.setLexState(LexState.EXPR_END);
                    }
            }
            tokfix();
            
            yyVal = ruby.intern(tok());
            return result;
        }
    }


    /**
     *  Description of the Method
     *
     *@param  c  Description of Parameter
     *@return    Description of the Returned Value
     */
    private int start_num(int c) {
        boolean is_float;
        boolean seen_point;
        boolean seen_e;
        boolean seen_uc;

        is_float = seen_point = seen_e = seen_uc = false;
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
                    if (!ISXDIGIT(c)) {
                        break;
                    }
                    seen_uc = false;
                    tokadd(c);
                } while ((c = nextc()) != 0);
                pushback(c);
                tokfix();
                if (toklen() == 0) {
                    yyerror("hexadecimal number without hex-digits");
                } else if (seen_uc) {
                    return decode_num(c, is_float, seen_uc, true);
                }
                yyVal = rb_cstr2inum(tok(), 16);
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
                tokfix();
                if (toklen() == 0) {
                    yyerror("numeric literal without digits");
                } else if (seen_uc) {
                    return decode_num(c, is_float, seen_uc, true);
                }
                yyVal = rb_cstr2inum(tok(), 2);
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
                tokfix();
                if (seen_uc) {
                    return decode_num(c, is_float, seen_uc, true);
                }
                yyVal = rb_cstr2inum(tok(), 8);
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

        for (; ; ) {
            switch (c) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    seen_uc = false;
                    tokadd(c);
                    break;
                case '.':
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
                case 'e':
                case 'E':
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
                case '_': //  '_' in number just ignored
                    seen_uc = true;
                    break;
                default:
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
            tokfix();
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
            yyVal = RubyFloat.m_newFloat(ruby, d);
            return Token.tFLOAT;
        }
        yyVal = rb_cstr2inum(tok(), 10);
        return Token.tINTEGER;
    }

    /**
     *  AFAIK this methods expand the #{} in strings.
     *
     *@param  list  Description of Parameter
     *@param  term  Description of Parameter
     *@return       Description of the Returned Value
     */
    private Node str_extend(Node list, int term) {
        int c;
        int brace = -1;
        RubyObject ss;
        Node node;
        int nest;

        c = nextc();
        switch (c) {
            case '$':
            case '@':
            case '{':
                break;
            default:
                tokadd('#');
                pushback(c);
                return list;
        }

        ss = RubyString.m_newString(ruby, tok(), toklen());
        if (list == null) {
            list = nf.newDStr(ss);
        } else if (toklen() > 0) {
            ph.list_append(list, nf.newStr(ss));
        }
        newtok();

        fetch_id :
        for (; ; ) {
            switch (c) {
                case '$':
                    tokadd('$');
                    c = nextc();
                    if (c == -1) {
                        return Node.MINUS_ONE;
                    }
                    switch (c) {
                        case '1': case '2': case '3':
                        case '4': case '5': case '6':
                        case '7': case '8': case '9':
                            while (Character.isDigit((char) c)) {
                                tokadd(c);
                                c = nextc();
                            }
                            pushback(c);
                            break fetch_id;
                        case '&': case '+':
                        case '_': case '~':
                        case '*': case '$': case '?':
                        case '!': case '@': case ',':
                        case '.': case '=': case ':':
                        case '<': case '>': case '\\':
                            //refetch:
                            tokadd(c);
                            break fetch_id;
                        default:
                            if (c == term) {
                                ph.list_append(list, nf.newStr(RubyString.m_newString(ruby, "#$")));
                                pushback(c);
                                newtok();
                                return list;
                            }
                            switch (c) {
                                case '\"':
                                case '/':
                                case '\'':
                                case '`':
                                    //goto refetch;
                                    tokadd(c);
                                    break fetch_id;
                            }
                            if (!is_identchar(c)) {
                                yyerror("bad global variable in string");
                                newtok();
                                return list;
                            }
                    }

                    while (is_identchar(c)) {
                        tokadd(c);
                        c = nextc();
                    }
                    pushback(c);
                    break;
                case '@':
                    tokadd(c);
                    c = nextc();
                    if (c == '@') {
                        tokadd(c);
                        c = nextc();
                    }
                    while (is_identchar(c)) {
                        tokadd(c);
                        c = nextc();
                    }
                    pushback(c);
                    break;
                case '{':
                    if (c == '{') {
                        brace = '}';
                    }
                    nest = 0;
                    do {
                        loop_again :
                        for (; ; ) {
                            c = nextc();
                            switch (c) {
                                case -1:
                                    if (nest > 0) {
                                        yyerror("bad substitution in string");
                                        newtok();
                                        return list;
                                    }
                                    return Node.MINUS_ONE;
                                case '}':
                                    if (c == brace) {
                                        if (nest == 0) {
                                            break;
                                        }
                                        nest--;
                                    }
                                    tokadd(c);
                                    continue loop_again;
                                case '\\':
                                    c = nextc();
                                    if (c == -1) {
                                        return Node.MINUS_ONE;
                                    }
                                    if (c == term) {
                                        tokadd(c);
                                    } else {
                                        tokadd('\\');
                                        tokadd(c);
                                    }
                                    break;
                                case '{':
                                    if (brace != -1) {
                                        nest++;
                                    }
                                case '\"':
                                case '/':
                                case '`':
                                    if (c == term) {
                                        pushback(c);
                                        ph.list_append(list, nf.newStr(RubyString.m_newString(ruby, "#")));
                                        ph.rb_warning("bad substitution in string");
                                        tokfix();
                                        ph.list_append(list, nf.newStr(RubyString.m_newString(ruby, tok(), toklen())));
                                        newtok();
                                        return list;
                                    }
                                default:
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
        tokfix();
        node = nf.newEVStr(tok(), toklen());
        ph.list_append(list, node);
        newtok();

        return list;
    }
    // yylex


    // Helper functions....................

    //XXX private helpers, can be inlined

    /**
     *  Returns true if "c" is a white space character.
     *
     *@param  c  Description of Parameter
     *@return    Description of the Returned Value
     */
    private boolean ISSPACE(int c) {
        return Character.isWhitespace((char) c);
    }


    /**
     *  Returns true if "c" is a hex-digit.
     *
     *@param  c  Description of Parameter
     *@return    Description of the Returned Value
     */
    private boolean ISXDIGIT(int c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
    }


    /**
     *  Returns the value of a hex number with max "len" characters. Also
     *  returns the number of characters read. Please note the "x"-hack.
     *
     *@param  s       Description of Parameter
     *@param  start   Description of Parameter
     *@param  len     Description of Parameter
     *@param  retlen  Description of Parameter
     *@return         Description of the Returned Value
     */
    private long scan_hex(String s, int start, int len, int[] retlen) {
        String hexdigit = "0123456789abcdef0123456789ABCDEFx";
        long retval = 0;
        int tmp;
        int st = start;

        while (len-- != 0 && st < s.length() && (tmp = hexdigit.indexOf(s.charAt(st))) != -1) {
            retval <<= 4;
            retval |= tmp & 15;
            st++;
        }
        retlen[0] = st - start;
        return retval;
    }
    
    
    
    // setter for lex options
    
    
    /** Setter for property lex_file_io.
     * @param lex_file_io New value of property lex_file_io.
     */
    public void setLexFileIo(boolean lex_file_io) {
        this.lex_file_io = lex_file_io;
    }
    
    /** Setter for property lex_gets_ptr.
     * @param lex_gets_ptr New value of property lex_gets_ptr.
     */
    public void setLexGetsPtr(int lex_gets_ptr) {
        this.lex_gets_ptr = lex_gets_ptr;
    }
    
    /** Setter for property lex_input.
     * @param lex_input New value of property lex_input.
     */
    public void setLexInput(org.jruby.RubyObject lex_input) {
        this.lex_input = lex_input;
    }
    
    /** Setter for property lex_p.
     * @param lex_p New value of property lex_p.
     */
    public void setLexP(int lex_p) {
        this.lex_p = lex_p;
    }
    
    /** Setter for property lex_pend.
     * @param lex_pend New value of property lex_pend.
     */
    public void setLexPEnd(int lex_pend) {
        this.lex_pend = lex_pend;
    }
}
