/*
 * Copyright (C) 2001 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License or
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License and GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public
 * License and GNU Lesser General Public License along with JRuby;
 * if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.lexer.yacc;

import org.ablaf.ast.INode;
import org.ablaf.common.IErrorHandler;
import org.ablaf.common.ISourcePosition;
import org.ablaf.lexer.ILexerSource;
import org.ablaf.lexer.ILexerState;
import org.ablaf.lexer.IYaccLexer;
import org.jruby.ast.*;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.StrTermNode;
import org.jruby.common.IErrors;
import org.jruby.parser.ParserSupport;
import org.jruby.parser.ReOptions;
import org.jruby.parser.Token;
import org.jruby.util.IdUtil;

import java.io.IOException;
import java.math.BigInteger;

/** This is a port of the MRI lexer to Java it is compatible to Ruby 1.6.7.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyYaccLexer implements IYaccLexer {
    // YaccLexer:
    private int token = 0;
    private Object yaccValue;

    // LexerSource:
    private IRubyLexerSupport support;
    
    private ParserSupport parserSupport = null;

    // ErrorHandler:
    private IErrorHandler errorHandler;

    // Lexer:
    private LexState lex_state;
    private StringBuffer tokenBuffer = new StringBuffer(60); // FIX: replace with special class.

    private StackState conditionState = new StackState();
    private StackState cmdArgumentState = new StackState();
    
    private INode lex_strterm = null;
    private boolean commandStart = true;
    
    // ruby constants for strings (should this be moved somewhere else?)
    private final int STR_FUNC_ESCAPE=0x01;
    private final int STR_FUNC_EXPAND=0x02;
    private final int STR_FUNC_REGEXP=0x04;
    private final int STR_FUNC_QWORDS=0x08;
    private final int STR_FUNC_SYMBOL=0x10;
    private final int STR_FUNC_INDENT=0x20;

    private final int str_squote = 0;
    private final int str_dquote = STR_FUNC_EXPAND;
    private final int str_xquote = STR_FUNC_EXPAND;
    private final int str_regexp = 
        STR_FUNC_REGEXP|STR_FUNC_ESCAPE|STR_FUNC_EXPAND;
    private final int str_sword  = STR_FUNC_QWORDS;
    private final int str_dword  = STR_FUNC_QWORDS|STR_FUNC_EXPAND;
    private final int str_ssym   = STR_FUNC_SYMBOL;
    private final int str_dsym   = STR_FUNC_SYMBOL|STR_FUNC_EXPAND;
    
    // YaccLexer implementation:
    public boolean advance() throws IOException {
        return (token = yylex()) != EOF;
    }
    
    public void setParserSupport(ParserSupport parserSupport) {
        this.parserSupport = parserSupport;
    }
    
    public INode strTerm() {
        return lex_strterm;
    }
    
    public void setStrTerm(INode strterm) {
        this.lex_strterm = strterm;
    }

    public int token() {
        return token;
    }

    public Object value() {
        return yaccValue;
    }

    public void resetStacks() {
        conditionState.reset();
        cmdArgumentState.reset();
    }

    private String tok() {
        return tokenBuffer.toString();
    }
    
    private void tokadd(char c) {
        tokenBuffer.append(c);
    }
    
    private boolean isNext_identchar() {
        char c = support.read();
        boolean test = (c != EOF && (Character.isLetterOrDigit(c) || c == '-'));

        support.unread();
        return test;
    }
    
    private int toklen() {
        return tokenBuffer.length();
    }

    private char toklast() {
        return tokenBuffer.charAt(toklen() - 1);
    }

    private void newToken() {
        tokenBuffer.setLength(0);
    }

    private boolean peek(char compareChar) {
        return support.getCharAt(0) == compareChar;
    }
    
    private boolean whole_match_p(String match, boolean indent) {
        int length = match.length();
        int j = 0;
        if (indent) {
            for (char c = support.read(); 
            	Character.isWhitespace(c);
            	c = support.read()) {
                j++;
            }
            // Put back non-space (EOF?)
            support.unread();
        }
        
        for (int i = 0; i < length; i++) {
            char c = support.read();
            if (match.charAt(i) != c) {
                support.unreadMany(i+j+1);
                return false;
            }
        }
        return true;
    }
    
    private boolean was_bol() {
        return support.getColumn() == 1;
    }
    
    private boolean ISUPPER(char c) {
        return Character.toUpperCase(c) == c;
    }
    
    private char read_escape() {
        try {
            return support.readEscape();
        } catch (LexerException lExcptn) {
            errorHandler.handleError(IErrors.SYNTAX_ERROR, support.getPosition(), lExcptn.getMessage());
        }
        return '\0';
    }
    
    private int regx_options() {
        char kcode = 0;
        int options = 0;
        char c;

        newToken();
        while ((c = support.read()) != EOF  && Character.isLetter(c) == true) {
    	switch (c) {
    	  case 'i':
    	    options |= ReOptions.RE_OPTION_IGNORECASE;
    	    break;
    	  case 'x':
    	    options |= ReOptions.RE_OPTION_EXTENDED;
    	    break;
    	  case 'm':
    	    options |= ReOptions.RE_OPTION_MULTILINE;
    	    break;
    	  case 'o':
    	    options |= ReOptions.RE_OPTION_ONCE;
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
    	    tokadd(c);
    	    break;
    	}
        }
        support.unread();
        if (toklen() != 0) {
            rb_compile_error("unknown regexp option" +
                    (toklen() > 1 ? "s" : "") + " - " + tok());
        }
        return options | kcode;
    }

    private boolean ismbchar(int c) {
        return false;
    }
    
    // All multi-byte chars in java fit in a char
    private int mbclen(int c) {
        return 1;
    }
    
    private void rb_warn(String message) {
        errorHandler.handleError(IErrors.WARN, support.getPosition(), message); 
    }

    private void rb_warning(String message) {
        errorHandler.handleError(IErrors.WARNING, support.getPosition(), message); 
    }
    
    private void rb_compile_error(String message) {
        errorHandler.handleError(IErrors.COMPILE_ERROR, support.getPosition(),
                message);
    }
    
    private void yyerror(String message) {
        errorHandler.handleError(IErrors.ERROR, support.getPosition(),
                message);
    }

    // HACK::::
    int scan_hex_len(int tryLen) {
        for (int i = 0; i < tryLen; i++) {
            if (Character.digit(support.getCharAt(i), 16) == 0) {
                return i;
            }
        }

        return tryLen;
    }

    // HACK::::
    long scan_hex(int tryLen) {
        long retVal = 0;
        int i = 0;
        
        for (; i < tryLen; i++) {
            int c = Character.digit(support.getCharAt(i), 16);
            
            if (c == 0) {
                break;
            }
            
            retVal <<= 4;
            retVal |= c & 15;
        }

        return retVal;
    }
    
    // Was a goto in original ruby lexer
    boolean escaped(int term) {
        char c;
        
        if ((c = support.read()) == '\\') {
            return tokadd_escape(term);
        } 
        if (c == EOF) {
            yyerror("Invalid escape character syntax");
            return false;
        }
        tokadd(c);
        return true;
    }
    
    boolean tokadd_escape(int term) {
        char c;

        switch (c = support.read()) {
        case '\n':
            return true;		/* just ignore */

        case '0': case '1': case '2': case '3': /* octal constant */
        case '4': case '5': case '6': case '7':
        {
            int i;

            tokadd('\\');
            tokadd(c);
            for (i=0; i<2; i++) {
                c = support.read();
                if (c == EOF) {
                    yyerror("Invalid escape character syntax");
                    return false;
                }
                if (c < '0' || '7' < c) {
                    support.unread();
                    break;
                }
                tokadd(c);
            }
        }
        return true;

        case 'x':	/* hex constant */
        {
            int numlen;

            tokadd('\\');
            tokadd(c);
            numlen = scan_hex_len(2);
            if (numlen == 0) {
                yyerror("Invalid escape character syntax");
                return false;
            }
            while (numlen-- >= 0) {
                tokadd(support.read());
            }
        }
        return true;

        case 'M':
            if ((c = support.read()) != '-') {
                yyerror("Invalid escape character syntax");
                support.unread();
                return false;
            }
            tokadd('\\'); tokadd('M'); tokadd('-');
            return escaped(term);

        case 'C':
            if ((c = support.read()) != '-') {
                yyerror("Invalid escape character syntax");
                support.unread();
                return false;
            }
            tokadd('\\'); tokadd('C'); tokadd('-');
            return escaped(term);

        case 'c':
            tokadd('\\'); tokadd('c');
            return escaped(term);

        case 0:
            yyerror("Invalid escape character syntax");
            return false;

        default:
            if (c != '\\' || c != term)
                tokadd('\\');
            tokadd(c);
        }
        return true;
    }
    
    char tokadd_string(StrTermNode node) {
        int func = node.getFunc();
        int paren = node.getParen();
        int term = node.getTerm();
        char c;

        while ((c = support.read()) != EOF) {
            if (paren != 0 && c == paren) {
                node.setNest(node.getNest()+1);
            }
            else if (c == term) {
                if (node.getNest() == 0) {
                    support.unread();
                    break;
                }
                node.setNest(node.getNest()-1);
            }
            else if ((func & STR_FUNC_EXPAND) != 0 && c == '#' && support.isEOL() == false) {
                char c2 = support.read();
                
                if (c2 == '$' || c2 == '@' || c2 == '{') {
                    support.unreadMany(2); // c2,c
                    break;
                }
            }
            else if (c == '\\') {
                c = support.read();
                switch (c) {
                case '\n':
                    if ((func & STR_FUNC_QWORDS) != 0) break;
                    if ((func & STR_FUNC_EXPAND) != 0) continue;
                    tokadd('\\');
                    break;

                case '\\':
                    if ((func & STR_FUNC_ESCAPE) != 0) tokadd((char)c);
                    break;

                default:
                    if ((func & STR_FUNC_REGEXP) != 0) {
                        support.unread();
                        if (tokadd_escape(term) == false)
                            return 0;
                        continue;
                    }
                    else if ((func & STR_FUNC_EXPAND) != 0) {
                        support.unread();
                        if ((func & STR_FUNC_ESCAPE) != 0) tokadd('\\');
                        c = (char)read_escape();
                    }
                    else if ((func & STR_FUNC_QWORDS) != 0 && Character.isWhitespace(c)) {
                        /* ignore backslashed spaces in %w */
                    }
                    else if (c != term && !(paren != 0 && c == paren)) {
                        tokadd('\\');
                    }
                }
            }
            else if (ismbchar(c)) {
                int i, len = mbclen(c)-1;

                for (i = 0; i < len; i++) {
                    tokadd(c);
                    c = support.read();
                }
            }
            else if ((func & STR_FUNC_QWORDS) != 0 && Character.isWhitespace(c)) {
                support.unread();
                break;
            }
            if (c == 0 && (func & STR_FUNC_SYMBOL) != 0) {
                func &= ~STR_FUNC_SYMBOL;
                rb_compile_error("symbol cannot contain '\\0'");
                continue;
            }
            tokadd(c);
        }
        return c;
    }
    
    private int parseQuote(char c) {
        char term;
        int paren;
        
        if (!Character.isLetterOrDigit(c)) {
            term = c;
            c = 'Q';
        } else {
            term = support.read();
            if (Character.isLetterOrDigit(term) || ismbchar(term)) {
                yyerror("unknown type of %string");
                return 0;
            }
        }
        if (c == EOF || term == EOF) {
            rb_compile_error("unterminated quoted string meets end of file");
            return 0;
        }
        paren = term;
        if (term == '(') term = ')';
        else if (term == '[') term = ']';
        else if (term == '{') term = '}';
        else if (term == '<') term = '>';
        else paren = 0;

        switch (c) {
        case 'Q':
            lex_strterm = new StrTermNode(support.getPosition(), str_dquote, term, paren);
            return Token.tSTRING_BEG;

        case 'q':
            lex_strterm = new StrTermNode(support.getPosition(), str_squote, term, paren);
            return Token.tSTRING_BEG;

        case 'W':
            lex_strterm = new StrTermNode(support.getPosition(), str_dquote | STR_FUNC_QWORDS, term, paren);
            do {c = support.read();} while (Character.isWhitespace(c));
            support.unread();
            return Token.tWORDS_BEG;

        case 'w':
            lex_strterm = new StrTermNode(support.getPosition(), str_squote | STR_FUNC_QWORDS, term, paren);
            do {c = support.read();} while (Character.isWhitespace(c));
            support.unread();
            return Token.tQWORDS_BEG;

        case 'x':
            lex_strterm = new StrTermNode(support.getPosition(), str_xquote, term, paren);
            return Token.tXSTRING_BEG;

        case 'r':
            lex_strterm = new StrTermNode(support.getPosition(), str_regexp, term, paren);
            return Token.tREGEXP_BEG;

        case 's':
            lex_strterm = new StrTermNode(support.getPosition(), str_ssym, term, paren);
            lex_state = LexState.EXPR_FNAME;
            return Token.tSYMBEG;

        default:
            errorHandler.handleError(
                    IErrors.SYNTAX_ERROR,
                    support.getPosition(),
                    Messages.getString("unknown_quotation_type", String.valueOf(c)));
            return 0;
        }
    }
    
    void heredoc_restore() {
        HereDocNode here = (HereDocNode) lex_strterm;

        support.setBuffer(here.getLastLine(), here.getPosition());
    }

    int hereDocument() {
        HereDocNode here = (HereDocNode) lex_strterm;
        char c;
        String eos = here.getValue();
        long len = eos.length();
        int func = here.getFunc();
        boolean indent = (func & STR_FUNC_INDENT) != 0;
        StringBuffer str = new StringBuffer();

        if ((c = support.read()) == EOF) {
            rb_compile_error("can't find string \"" + eos + "\" anywhere before EOF");
            heredoc_restore();
            lex_strterm = null;
            return 0;
        }

        if (was_bol() && whole_match_p(eos, indent)) {
            heredoc_restore();
            return Token.tSTRING_END;
        }

        if ((func & STR_FUNC_EXPAND) == 0) {
            if (c == '\n') {
                support.unread();
            }
            
            // Something missing here...
            /*
            int lastLineLength = here.getLastLineLength();

            if (lastLineLength > 0) {
            	// It looks like I needed to append last line as well...
            	support.unreadMany(here.getLastLineLength());
            	str.append(support.readLine());
            	str.append("\n");
            }
            */
            
            do {
                str.append(support.readLine());
                str.append("\n");

                if (support.isEOF()) {
                    rb_compile_error("can't find string \"" + eos + "\" anywhere before EOF");
                    heredoc_restore();
                    lex_strterm = null;
                    return 0;
                }
            } while (!whole_match_p(eos, indent));
        } else {
            newToken();
            if (c == '#') {
                switch (c = support.read()) {
                    case '$':
                    case '@':
                        support.unread();
                        return Token.tSTRING_DVAR;
                    case '{':
                        return Token.tSTRING_DBEG;
                }
                tokadd('#');
            }

            support.unread();
            do {

                if ((c = tokadd_string(new StrTermNode(support.getPosition(), func, '\n', 0))) == EOF) {
                    rb_compile_error("can't find string \"" + eos + "\" anywhere before EOF");
                    heredoc_restore();
                    lex_strterm = null;
                    return 0;
                }
                if (c != '\n') {
                    yaccValue = tok();
                    return Token.tSTRING_CONTENT;
                }
                tokadd(support.read());
                if ((c = support.read()) == EOF) {
                    rb_compile_error("can't find string \"" + eos + "\" anywhere before EOF");
                    heredoc_restore();
                    lex_strterm = null;
                    return 0;
                }
                // We need to pushback so when whole match looks it did not
                // lose a char during last EOF
                support.unread();
            } while (!whole_match_p(eos, indent));
            str = new StringBuffer(tok());
        }
        heredoc_restore();
        lex_strterm = new StrTermNode(support.getPosition(), -1, 0, 0);
        yaccValue = str.toString();
        return Token.tSTRING_CONTENT;
    }

    private int parseString() {
        StrTermNode quote = (StrTermNode) lex_strterm;
        int func = quote.getFunc();
        int term = quote.getTerm();
        int paren = quote.getParen();
        char c; 
        int space = 0;

        if (func == -1) return Token.tSTRING_END;
        c = support.read();
        if ((func & STR_FUNC_QWORDS) != 0 && Character.isWhitespace(c)) {
            do {c = support.read();} while (Character.isWhitespace(c));
            space = 1;
        }

        if (c == term && quote.getNest() == 0) {
            if ((func & STR_FUNC_QWORDS) != 0) {
                quote.setFunc(-1);
                return ' ';
            }
            if ((func & STR_FUNC_REGEXP) == 0) {
                return Token.tSTRING_END;
            }
            yaccValue = new RegexpNode(support.getPosition(), tok(), regx_options());
            return Token.tREGEXP_END;
        }
        if (space != 0) {
            support.unread();
            return ' ';
        }
        newToken();
        if ((func & STR_FUNC_EXPAND) != 0 && c == '#') {
            c = support.read();
            switch (c) {
            case '$':
            case '@':
                support.unread();
                return Token.tSTRING_DVAR;
            case '{':
                return Token.tSTRING_DBEG;
            }
            tokadd('#');
        }
        support.unread();
        if (tokadd_string(quote) == 0) {
            // ruby -- ruby_sourceline = nd_line(quote);
            rb_compile_error("unterminated string meets end of file");
            return Token.tSTRING_END;
        }

        yaccValue = tok();
        return Token.tSTRING_CONTENT;
    }
    
    
    private int hereDocumentIdentifier() {
        char c = support.read(); 
        int term;
        int func = 0;
        int len;

        if (c == '-') {
            c = support.read();
            func = STR_FUNC_INDENT;
        }
        
        if (c == '\'' || c == '"' || c == '`') {
            if (c == '\'') {
                func = func | str_squote;
            } else if (c == '"') {
                func = func | str_dquote;
            } else if (c == '`') {
                func = func | str_xquote; 
            }

            newToken();
            term = c;
            while ((c = support.read()) != EOF && c != term) {
                len = mbclen(c);
                do {
                    tokadd(c);
                } while (--len > 0 && (c = support.read()) != EOF);
            }
            if (c == EOF) {
                errorHandler.handleError(IErrors.COMPILE_ERROR, 
                    support.getPosition(), 
                    "unterminated here document identifier");
                return 0;
            }	
        } else {
            if (!isIdentifierChar(c)) {
                support.unread();
                if ((func & STR_FUNC_INDENT) != 0) {
                    support.unread();
                }
                return 0;
            }
            newToken();
            term = '"';
            func |= str_dquote;
            do {
                len = mbclen(c);
                do {
                    tokadd(c);
                } while (--len > 0 && (c = support.read()) != EOF);
            } while ((c = support.read()) != EOF && isIdentifierChar(c));
            support.unread();
        }

        // TODO: Adding a newline onto line make assertions in unit/test pass,
        // but then our <<A,<<B test case fails.  For now, make our internal test
        // pass..
        String line = support.readLine();
        String tok = tok();
        lex_strterm = new HereDocNode(support.getPosition(), tok, func, line);

        return term == '`' ? Token.tXSTRING_BEG : Token.tSTRING_BEG;
    }
    
    private void arg_ambiguous() {
        errorHandler.handleError(IErrors.WARNING, support.getPosition(), Messages.getString("ambiguous_first_argument")); //$NON-NLS-1$
    }

    ISourcePosition currentPos;

    private static final int EOF = 0;
    public ISourcePosition getPosition() {
        return currentPos;
    }

    /*
     * Not normally used, but is left in here since it can be useful in debugging
     * grammar and lexing problems.
     */
    private void printToken(int token) {
        //System.out.print("LOC: " + support.getPosition() + " ~ ");
        
        switch (token) {
        	case Token.yyErrorCode: System.err.print("yyErrorCode,"); break;
        	case Token.kCLASS: System.err.print("kClass,"); break;
        	case Token.kMODULE: System.err.print("kModule,"); break;
        	case Token.kDEF: System.err.print("kDEF,"); break;
        	case Token.kUNDEF: System.err.print("kUNDEF,"); break;
        	case Token.kBEGIN: System.err.print("kBEGIN,"); break;
        	case Token.kRESCUE: System.err.print("kRESCUE,"); break;
        	case Token.kENSURE: System.err.print("kENSURE,"); break;
        	case Token.kEND: System.err.print("kEND,"); break;
        	case Token.kIF: System.err.print("kIF,"); break;
        	case Token.kUNLESS: System.err.print("kUNLESS,"); break;
        	case Token.kTHEN: System.err.print("kTHEN,"); break;
        	case Token.kELSIF: System.err.print("kELSIF,"); break;
        	case Token.kELSE: System.err.print("kELSE,"); break;
        	case Token.kCASE: System.err.print("kCASE,"); break;
        	case Token.kWHEN: System.err.print("kWHEN,"); break;
        	case Token.kWHILE: System.err.print("kWHILE,"); break;
        	case Token.kUNTIL: System.err.print("kUNTIL,"); break;
        	case Token.kFOR: System.err.print("kFOR,"); break;
        	case Token.kBREAK: System.err.print("kBREAK,"); break;
        	case Token.kNEXT: System.err.print("kNEXT,"); break;
        	case Token.kREDO: System.err.print("kREDO,"); break;
        	case Token.kRETRY: System.err.print("kRETRY,"); break;
        	case Token.kIN: System.err.print("kIN,"); break;
        	case Token.kDO: System.err.print("kDO,"); break;
        	case Token.kDO_COND: System.err.print("kDO_COND,"); break;
        	case Token.kDO_BLOCK: System.err.print("kDO_BLOCK,"); break;
        	case Token.kRETURN: System.err.print("kRETURN,"); break;
        	case Token.kYIELD: System.err.print("kYIELD,"); break;
        	case Token.kSUPER: System.err.print("kSUPER,"); break;
        	case Token.kSELF: System.err.print("kSELF,"); break;
        	case Token.kNIL: System.err.print("kNIL,"); break;
        	case Token.kTRUE: System.err.print("kTRUE,"); break;
        	case Token.kFALSE: System.err.print("kFALSE,"); break;
        	case Token.kAND: System.err.print("kAND,"); break;
        	case Token.kOR: System.err.print("kOR,"); break;
        	case Token.kNOT: System.err.print("kNOT,"); break;
        	case Token.kIF_MOD: System.err.print("kIF_MOD,"); break;
        	case Token.kUNLESS_MOD: System.err.print("kUNLESS_MOD,"); break;
        	case Token.kWHILE_MOD: System.err.print("kWHILE_MOD,"); break;
        	case Token.kUNTIL_MOD: System.err.print("kUNTIL_MOD,"); break;
        	case Token.kRESCUE_MOD: System.err.print("kRESCUE_MOD,"); break;
        	case Token.kALIAS: System.err.print("kALIAS,"); break;
        	case Token.kDEFINED: System.err.print("kDEFINED,"); break;
        	case Token.klBEGIN: System.err.print("klBEGIN,"); break;
        	case Token.klEND: System.err.print("klEND,"); break;
        	case Token.k__LINE__: System.err.print("k__LINE__,"); break;
        	case Token.k__FILE__: System.err.print("k__FILE__,"); break;
        	case Token.tIDENTIFIER: System.err.print("tIDENTIFIER["+ value() + "],"); break;
        	case Token.tFID: System.err.print("tFID[" + value() + "],"); break;
        	case Token.tGVAR: System.err.print("tGVAR[" + value() + "],"); break;
        	case Token.tIVAR: System.err.print("tIVAR[" + value() +"],"); break;
        	case Token.tCONSTANT: System.err.print("tCONSTANT["+ value() +"],"); break;
        	case Token.tCVAR: System.err.print("tCVAR,"); break;
        	case Token.tINTEGER: System.err.print("tINTEGER,"); break;
        	case Token.tFLOAT: System.err.print("tFLOAT,"); break;
            case Token.tSTRING_CONTENT: System.err.print("tSTRING_CONTENT[" + yaccValue + "],"); break;
            case Token.tSTRING_BEG: System.err.print("tSTRING_BEG,"); break;
            case Token.tSTRING_END: System.err.print("tSTRING_END,"); break;
            case Token.tSTRING_DBEG: System.err.print("STRING_DBEG,"); break;
            case Token.tSTRING_DVAR: System.err.print("tSTRING_DVAR,"); break;
            case Token.tXSTRING_BEG: System.err.print("tXSTRING_BEG,"); break;
            case Token.tREGEXP_BEG: System.err.print("tREGEXP_BEG,"); break;
            case Token.tREGEXP_END: System.err.print("tREGEXP_END,"); break;
            case Token.tWORDS_BEG: System.err.print("tWORDS_BEG,"); break;
            case Token.tQWORDS_BEG: System.err.print("tQWORDS_BEG,"); break;
        	case Token.tBACK_REF: System.err.print("tBACK_REF,"); break;
        	case Token.tNTH_REF: System.err.print("tNTH_REF,"); break;
        	case Token.tUPLUS: System.err.print("tUPLUS"); break;
        	case Token.tUMINUS: System.err.print("tUMINUS,"); break;
        	case Token.tPOW: System.err.print("tPOW,"); break;
        	case Token.tCMP: System.err.print("tCMP,"); break;
        	case Token.tEQ: System.err.print("tEQ,"); break;
        	case Token.tEQQ: System.err.print("tEQQ,"); break;
        	case Token.tNEQ: System.err.print("tNEQ,"); break;
        	case Token.tGEQ: System.err.print("tGEQ,"); break;
        	case Token.tLEQ: System.err.print("tLEQ,"); break;
        	case Token.tANDOP: System.err.print("tANDOP,"); break;
        	case Token.tOROP: System.err.print("tOROP,"); break;
        	case Token.tMATCH: System.err.print("tMATCH,"); break;
        	case Token.tNMATCH: System.err.print("tNMATCH,"); break;
        	case Token.tDOT2: System.err.print("tDOT2,"); break;
        	case Token.tDOT3: System.err.print("tDOT3,"); break;
        	case Token.tAREF: System.err.print("tAREF,"); break;
        	case Token.tASET: System.err.print("tASET,"); break;
        	case Token.tLSHFT: System.err.print("tLSHFT,"); break;
        	case Token.tRSHFT: System.err.print("tRSHFT,"); break;
        	case Token.tCOLON2: System.err.print("tCOLON2,"); break;
        	case Token.tCOLON3: System.err.print("tCOLON3,"); break;
        	case Token.tOP_ASGN: System.err.print("tOP_ASGN,"); break;
        	case Token.tASSOC: System.err.print("tASSOC,"); break;
        	case Token.tLPAREN: System.err.print("tLPAREN,"); break;
        	case Token.tLPAREN_ARG: System.err.print("tLPAREN_ARG,"); break;
        	case Token.tLBRACK: System.err.print("tLBRACK,"); break;
        	case Token.tLBRACE: System.err.print("tLBRACE,"); break;
        	case Token.tSTAR: System.err.print("tSTAR,"); break;
        	case Token.tAMPER: System.err.print("tAMPER,"); break;
        	case Token.tSYMBEG: System.err.print("tSYMBEG,"); break;
        	case '\n': System.err.println("NL"); break;
        	default: System.err.print("'" + (char)token + "',"); break;
        }
    }
    
    /*
     * DEBUGGING HELP
    private int yylex() {
        int token = yylex2();
        
        printToken(token);
        
        return token;
    }
    */
    
    /**
     *  Returns the next token. Also sets yyVal is needed.
     *
     *@return    Description of the Returned Value
     */
    private int yylex() {
        char c;
        boolean spaceSeen = false;
        boolean commandState;
        
        if (lex_strterm != null) {
            if (lex_strterm instanceof HereDocNode) {
                int token = hereDocument();
                if (token == Token.tSTRING_END) {
                    lex_strterm = null;
                    lex_state = LexState.EXPR_END;
                }
                
                return token;
            } else {
                int token = parseString();
                if (token == Token.tSTRING_END || token == Token.tREGEXP_END) {
                    lex_strterm = null;
                    lex_state = LexState.EXPR_END;
                }
                
                return token;
            }
        }

        currentPos = support.getPosition();
        
        commandState = commandStart;
        commandStart = false;
        
        retry: for(;;) {
            c = support.read();
            switch(c) {
            case '\004':		/* ^D */
            case '\032':		/* ^Z */
            case 0:			/* end of script. */
                return 0;
           
                /* white spaces */
            case ' ': case '\t': case '\f': case '\r':
            case '\13': /* '\v' */
                spaceSeen = true;
                continue retry;
            case '#':		/* it's a comment */
                while ((c = support.read()) != '\n') {
                    if (c == EOF) {
                        return 0;
                    }
                }
                
                /* fall through */
            case '\n':
                if (lex_state == LexState.EXPR_BEG ||
                    lex_state == LexState.EXPR_FNAME ||
                    lex_state == LexState.EXPR_DOT ||
                    lex_state == LexState.EXPR_CLASS) {
                    continue retry;
                } 

                commandStart = true;
                lex_state = LexState.EXPR_BEG;
                return '\n';
                
            case '*':
                if ((c = support.read()) == '*') {
                    if ((c = support.read()) == '=') {
                        yaccValue = "**";
                        lex_state = LexState.EXPR_BEG;
                        return Token.tOP_ASGN;
                    }
                    support.unread();
                    c = Token.tPOW;
                } else {
                    if (c == '=') {
                        yaccValue = "*";
                        lex_state = LexState.EXPR_BEG;
                        return Token.tOP_ASGN;
                    }
                    support.unread();
                    if (lex_state.isArgument() && spaceSeen && !Character.isWhitespace(c)) {
                        errorHandler.handleError(IErrors.WARNING, support.getPosition(), "`*' interpreted as argument prefix");
                        c = Token.tSTAR;
                    } else if (lex_state == LexState.EXPR_BEG || 
                            lex_state == LexState.EXPR_MID) {
                        c = Token.tSTAR;
                    } else {
                        c = '*';
                    }
                }
                if (lex_state == LexState.EXPR_FNAME ||
                    lex_state == LexState.EXPR_DOT) {
                    lex_state = LexState.EXPR_ARG;
                } else {
                    lex_state = LexState.EXPR_BEG;
                }
                return c;

            case '!':
                lex_state = LexState.EXPR_BEG;
                if ((c = support.read()) == '=') {
                    return Token.tNEQ;
                }
                if (c == '~') {
                    return Token.tNMATCH;
                }
                support.unread();
                return '!';

            case '=':
                // Skip documentation nodes
                if (was_bol()) {
                    /* skip embedded rd document */
                    if (support.isNextNoCase("begin")) {
                        c = support.read();
                        
                        if (Character.isWhitespace(c)) {
                            // In case last next was the newline.
                            support.unread();
                            for (;;) {
                                support.readLine();
                                c = support.read();
                                if (c == EOF) {
                                    errorHandler.handleError(IErrors.COMPILE_ERROR, support.getPosition(), 
                                    	"embedded document meets end of file");
                                    return 0;
                                }
                                if (c != '=') continue;
                                if (support.isNextNoCase("end")) {
                                    if (support.isEOL()) {
                                        break;
                                    } 
                                    
                                    c = support.read();
                                    
                                    if (Character.isWhitespace(c)) {
                                        support.readLine();
                                        break;
                                    } else {
                                        support.unread();
                                    }
                                }
                            }
                            continue retry;
                        } else {
                            support.unread();
                        }
                    }
                }

                if (lex_state == LexState.EXPR_FNAME || 
                    lex_state == LexState.EXPR_DOT) {
                    lex_state = LexState.EXPR_ARG;
                } else { 
                    lex_state = LexState.EXPR_BEG;
                }

                c = support.read();
                if (c == '=') {
                    c = support.read();
                    if (c == '=') {
                        return Token.tEQQ;
                    }
                    support.unread();
                    return Token.tEQ;
                }
                if (c == '~') {
                    return Token.tMATCH;
                } else if (c == '>') {
                    return Token.tASSOC;
                }
                support.unread();
                return '=';
                
            case '<':
                c = support.read();
                if (c == '<' &&
                        lex_state != LexState.EXPR_END &&
                        lex_state != LexState.EXPR_DOT &&
                        lex_state != LexState.EXPR_ENDARG && 
                        lex_state != LexState.EXPR_CLASS &&
                        (!lex_state.isArgument() || spaceSeen)) {
                    int token = hereDocumentIdentifier();
                    if (token != 0) return token;
                }
                if (lex_state == LexState.EXPR_FNAME ||
                    lex_state == LexState.EXPR_DOT) {
                    lex_state = LexState.EXPR_ARG;
                } else {
                    lex_state = LexState.EXPR_BEG;
                }
                if (c == '=') {
                    if ((c = support.read()) == '>') {
                        return Token.tCMP;
                    }
                    support.unread();
                    return Token.tLEQ;
                }
                if (c == '<') {
                    if ((c = support.read()) == '=') {
                        yaccValue = "<<";
                        lex_state = LexState.EXPR_BEG;
                        return Token.tOP_ASGN;
                    }
                    support.unread();
                    return Token.tLSHFT;
                }
                support.unread();
                return '<';
                
            case '>':
                if (lex_state == LexState.EXPR_FNAME ||
                    lex_state == LexState.EXPR_DOT) {
                    lex_state = LexState.EXPR_ARG;
                } else {
                    lex_state = LexState.EXPR_BEG;
                }

                if ((c = support.read()) == '=') {
                    return Token.tGEQ;
                }
                if (c == '>') {
                    if ((c = support.read()) == '=') {
                        yaccValue = ">>";
                        lex_state = LexState.EXPR_BEG;
                        return Token.tOP_ASGN;
                    }
                    support.unread();
                    return Token.tRSHFT;
                }
                support.unread();
                return '>';

            case '"':
                lex_strterm = new StrTermNode(support.getPosition(), str_dquote, '"', 0);
                return Token.tSTRING_BEG;

            case '`':
                if (lex_state == LexState.EXPR_FNAME) {
                    lex_state = LexState.EXPR_END;
                    return c;
                }
                if (lex_state == LexState.EXPR_DOT) {
                    if (commandState) {
                        lex_state = LexState.EXPR_CMDARG;
                    } else {
                        lex_state = LexState.EXPR_ARG;
                    }
                    return c;
                }
                lex_strterm = new StrTermNode(support.getPosition(), str_xquote, '`', 0);
                return Token.tXSTRING_BEG;

            case '\'':
                lex_strterm = new StrTermNode(support.getPosition(), str_squote, '\'', 0);
                return Token.tSTRING_BEG;

            case '?':
                if (lex_state == LexState.EXPR_END || 
                    lex_state == LexState.EXPR_ENDARG) {
                    lex_state = LexState.EXPR_BEG;
                    return '?';
                }
                c = support.read();
                if (c == EOF) {
                    errorHandler.handleError(IErrors.COMPILE_ERROR, support.getPosition(), 
                    	"incomplete character syntax");
                    return 0;
                }
                if (Character.isWhitespace(c)){
                    if (!lex_state.isArgument()){
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
                            rb_warn("invalid character syntax; use ?\\" + c2);
                        }
                    }
                    support.unread();
                    lex_state = LexState.EXPR_BEG;
                    return '?';
                /*} else if (ismbchar(c)) { // ruby - we don't support them either?
                    rb_warn("multibyte character literal not supported yet; use ?\\" + c);
                    support.unread();
                    lexState = LexState.EXPR_BEG;
                    return '?';*/
                } else if ((Character.isLetterOrDigit(c) || c == '_') &&
                        	support.isEOL() == false &&
                        	isNext_identchar()) {
                    support.unread();
                    lex_state = LexState.EXPR_BEG;
                    return '?';
                } else if (c == '\\') {
                    c = (char)read_escape();
                }
                c &= 0xff;
                lex_state = LexState.EXPR_END;
                yaccValue = new Long(c);
                
                return Token.tINTEGER;

            case '&':
                if ((c = support.read()) == '&') {
                    lex_state = LexState.EXPR_BEG;
                    if ((c = support.read()) == '=') {
                        yaccValue = "&&";
                        lex_state = LexState.EXPR_BEG;
                        return Token.tOP_ASGN;
                    }
                    support.unread();
                    return Token.tANDOP;
                }
                else if (c == '=') {
                    yaccValue = "&";
                    lex_state = LexState.EXPR_BEG;
                    return Token.tOP_ASGN;
                }
                support.unread();
                if (lex_state.isArgument() && spaceSeen && !Character.isWhitespace(c)){
                    rb_warning("`&' interpreted as argument prefix");
                    c = Token.tAMPER;
                } else if (lex_state == LexState.EXPR_BEG || 
                        lex_state == LexState.EXPR_MID) {
                    c = Token.tAMPER;
                } else {
                    c = '&';
                }
                
                if (lex_state == LexState.EXPR_FNAME ||
                    lex_state == LexState.EXPR_DOT) {
                    lex_state = LexState.EXPR_ARG;
                } else {
                    lex_state = LexState.EXPR_BEG;
                }
                return c;
                
            case '|':
                if ((c = support.read()) == '|') {
                    lex_state = LexState.EXPR_BEG;
                    if ((c = support.read()) == '=') {
                        yaccValue = "||";
                        lex_state = LexState.EXPR_BEG;
                        return Token.tOP_ASGN;
                    }
                    support.unread();
                    return Token.tOROP;
                }
                if (c == '=') {
                    yaccValue = "|";
                    lex_state = LexState.EXPR_BEG;
                    return Token.tOP_ASGN;
                }
                if (lex_state == LexState.EXPR_FNAME || 
                    lex_state == LexState.EXPR_DOT) {
                    lex_state = LexState.EXPR_ARG;
                } else {
                    lex_state = LexState.EXPR_BEG;
                }
                support.unread();
                return '|';

            case '+':
                c = support.read();
                if (lex_state == LexState.EXPR_FNAME || 
                    lex_state == LexState.EXPR_DOT) {
                    lex_state = LexState.EXPR_ARG;
                    if (c == '@') {
                        return Token.tUPLUS;
                    }
                    support.unread();
                    return '+';
                }
                if (c == '=') {
                    yaccValue = "+";
                    lex_state = LexState.EXPR_BEG;
                    return Token.tOP_ASGN;
                }
                if (lex_state == LexState.EXPR_BEG ||
                    lex_state == LexState.EXPR_MID ||
                        (lex_state.isArgument() && spaceSeen && !Character.isWhitespace(c))) {
                    if (lex_state.isArgument()) arg_ambiguous();
                    lex_state = LexState.EXPR_BEG;
                    support.unread();
                    if (Character.isDigit(c)) {
                        c = '+';
                        return parseNumber(c);
                    }
                    return Token.tUPLUS;
                }
                lex_state = LexState.EXPR_BEG;
                support.unread();
                return '+';

            case '-':
                c = support.read();
                if (lex_state == LexState.EXPR_FNAME || 
                    lex_state == LexState.EXPR_DOT) {
                    lex_state = LexState.EXPR_ARG;
                    if (c == '@') {
                        return Token.tUMINUS;
                    }
                    support.unread();
                    return '-';
                }
                if (c == '=') {
                    yaccValue = "-";
                    lex_state = LexState.EXPR_BEG;
                    return Token.tOP_ASGN;
                }
                if (lex_state == LexState.EXPR_BEG || 
                    lex_state == LexState.EXPR_MID ||
                        (lex_state.isArgument() && spaceSeen && !Character.isWhitespace(c))) {
                    if (lex_state.isArgument()) arg_ambiguous();
                    lex_state = LexState.EXPR_BEG;
                    support.unread();
                    if (Character.isDigit(c)) {
                        return Token.tUMINUS_NUM;
                    }
                    return Token.tUMINUS;
                }
                lex_state = LexState.EXPR_BEG;
                support.unread();
                return '-';
                
            case '.':
                lex_state = LexState.EXPR_BEG;
                if ((c = support.read()) == '.') {
                    if ((c = support.read()) == '.') {
                        return Token.tDOT3;
                    }
                    support.unread();
                    return Token.tDOT2;
                }
                support.unread();
                if (!Character.isDigit(c)) {
                    lex_state = LexState.EXPR_DOT;
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
                return parseNumber(c);
                
            case ']':
            case '}':
            case ')':
                conditionState.restart();
                cmdArgumentState.restart();
                lex_state = LexState.EXPR_END;
                return c;

            case ':':
                c = support.read();
                if (c == ':') {
                    if (lex_state == LexState.EXPR_BEG ||
                        lex_state == LexState.EXPR_MID ||
                        lex_state == LexState.EXPR_CLASS || 
                        (lex_state.isArgument() && spaceSeen)) {
                        lex_state = LexState.EXPR_BEG;
                        return Token.tCOLON3;
                    }
                    lex_state = LexState.EXPR_DOT;
                    return Token.tCOLON2;
                }
                if (lex_state == LexState.EXPR_END || 
                    lex_state == LexState.EXPR_ENDARG || Character.isWhitespace(c)) {
                    support.unread();
                    lex_state = LexState.EXPR_BEG;
                    return ':';
                }
                switch (c) {
                case '\'':
                    lex_strterm = new StrTermNode(support.getPosition(), str_ssym, c, 0);
                    break;
                case '"':
                    lex_strterm = new StrTermNode(support.getPosition(), str_dsym, c, 0);
                    break;
                default:
                    support.unread();
                    break;
                }
                lex_state = LexState.EXPR_FNAME;
                return Token.tSYMBEG;

            case '/':
                if (lex_state == LexState.EXPR_BEG || 
                    lex_state == LexState.EXPR_MID) {
                    lex_strterm = new StrTermNode(support.getPosition(), str_regexp, '/', 0);
                    return Token.tREGEXP_BEG;
                }
                
                if ((c = support.read()) == '=') {
                    yaccValue = "/";
                    lex_state = LexState.EXPR_BEG;
                    return Token.tOP_ASGN;
                }
                support.unread();
                if (lex_state.isArgument() && spaceSeen) {
                    if (!Character.isWhitespace(c)) {
                        arg_ambiguous();
                        lex_strterm = new StrTermNode(support.getPosition(), str_regexp, '/', 0);
                        return Token.tREGEXP_BEG;
                    }
                }
                if (lex_state == LexState.EXPR_FNAME || 
                    lex_state == LexState.EXPR_DOT) {
                    lex_state = LexState.EXPR_ARG;
                } else {
                    lex_state = LexState.EXPR_BEG;
                }
                return '/';

            case '^':
                if ((c = support.read()) == '=') {
                    yaccValue = "^";
                    lex_state = LexState.EXPR_BEG;
                    return Token.tOP_ASGN;
                }
                if (lex_state == LexState.EXPR_FNAME || 
                    lex_state == LexState.EXPR_DOT) {
                    lex_state = LexState.EXPR_ARG;
                } else {
                    lex_state = LexState.EXPR_BEG;
                }
                support.unread();
                return '^';

            case ';':
                commandStart = true;
            case ',':
                lex_state = LexState.EXPR_BEG;
                return c;

            case '~':
                if (lex_state == LexState.EXPR_FNAME || 
                    lex_state == LexState.EXPR_DOT) {
                    if ((c = support.read()) != '@') {
                        support.unread();
                    }
                }
                if (lex_state == LexState.EXPR_FNAME || 
                        lex_state == LexState.EXPR_DOT) {
                    lex_state = LexState.EXPR_ARG;
                } else {
                    lex_state = LexState.EXPR_BEG;
                }
                return '~';
            case '(':
                commandStart = true;
                if (lex_state == LexState.EXPR_BEG || 
                    lex_state == LexState.EXPR_MID) {
                    c = Token.tLPAREN;
                } else if (spaceSeen) {
                    if (lex_state == LexState.EXPR_CMDARG) {
                        c = Token.tLPAREN_ARG;
                    } else if (lex_state == LexState.EXPR_ARG) {
                        rb_warn("don't put space before argument parentheses");
                        c = '(';
                    }
                }
                conditionState.stop();
                cmdArgumentState.stop();
                lex_state = LexState.EXPR_BEG;
                return c;

            case '[':
                if (lex_state == LexState.EXPR_FNAME || 
                    lex_state == LexState.EXPR_DOT) {
                    lex_state = LexState.EXPR_ARG;
                    if ((c = support.read()) == ']') {
                        if ((c = support.read()) == '=') {
                            return Token.tASET;
                        }
                        support.unread();
                        return Token.tAREF;
                    }
                    support.unread();
                    return '[';
                } else if (lex_state == LexState.EXPR_BEG || 
                           lex_state == LexState.EXPR_MID) {
                    c = Token.tLBRACK;
                } else if (lex_state.isArgument() && spaceSeen) {
                    c = Token.tLBRACK;
                }
                lex_state = LexState.EXPR_BEG;
                conditionState.stop();
                cmdArgumentState.stop();
                return c;
                
            case '{':
                if (lex_state.isArgument() || lex_state == LexState.EXPR_END) {
                    c = '{';          /* block (primary) */
                } else if (lex_state == LexState.EXPR_ENDARG) {
                    c = Token.tLBRACE_ARG;  /* block (expr) */
                } else {
                    c = Token.tLBRACE;      /* hash */
                }
                conditionState.stop();
                cmdArgumentState.stop();
                lex_state = LexState.EXPR_BEG;
                return c;

            case '\\':
                c = support.read();
                if (c == '\n') {
                    spaceSeen = true;
                    continue retry; /* skip \\n */
                }
                support.unread();
                return '\\';

            case '%':
                if (lex_state == LexState.EXPR_BEG || 
                    lex_state == LexState.EXPR_MID) {
                    return parseQuote(support.read());
                }
                if ((c = support.read()) == '=') {
                    yaccValue = "%";
                    lex_state = LexState.EXPR_BEG;
                    return Token.tOP_ASGN;
                }
                if (lex_state.isArgument() && spaceSeen && !Character.isWhitespace(c)) {
                    return parseQuote(c);
                }
                if (lex_state == LexState.EXPR_FNAME || 
                    lex_state == LexState.EXPR_DOT) {
                    lex_state = LexState.EXPR_ARG;
                } else {
                    lex_state = LexState.EXPR_BEG;
                }
                support.unread();
                return '%';

            case '$':
                lex_state = LexState.EXPR_END;
                newToken();
                c = support.read();
                switch (c) {
                case '_':		/* $_: last read line string */
                    c = support.read();
                    if (isIdentifierChar(c)) {
                        tokadd('$');
                        tokadd('_');
                        break;
                    }
                    support.unread();
                    c = '_';
                    /* fall through */
                case '~':		/* $~: match-data */
                    // Enebo: We had following line replace line after that,
                    // but it is commented out...
                    //parserSupport.getLocalNames().getLocalIndex(String.valueOf(c));
                    //local_cnt(c);
                    /* fall through */
                case '*':		/* $*: argv */
                case '$':		/* $$: pid */
                case '?':		/* $?: last status */
                case '!':		/* $!: error string */
                case '@':		/* $@: error position */
                case '/':		/* $/: input record separator */
                case '\\':		/* $\: output record separator */
                case ';':		/* $;: field separator */
                case ',':		/* $,: output field separator */
                case '.':		/* $.: last read line number */
                case '=':		/* $=: ignorecase */
                case ':':		/* $:: load path */
                case '<':		/* $<: reading filename */
                case '>':		/* $>: default output handle */
                case '\"':		/* $": already loaded files */
                    tokadd('$');
                    tokadd(c);
                    yaccValue = tok();
                    return Token.tGVAR;

                case '-':
                    tokadd('$');
                    tokadd(c);
                    c = support.read();
                    tokadd(c);
                    yaccValue = tok();
                    /* xxx shouldn't check if valid option variable */
                    return Token.tGVAR;

                case '&':		/* $&: last match */
                case '`':		/* $`: string before last match */
                case '\'':		/* $': string after last match */
                case '+':		/* $+: string matches last paren. */
                    yaccValue = new BackRefNode(support.getPosition(), c);
                    return Token.tBACK_REF;

                case '1': case '2': case '3':
                case '4': case '5': case '6':
                case '7': case '8': case '9':
                    tokadd('$');
                    do {
                        tokadd(c);
                        c = support.read();
                    } while (Character.isDigit(c));
                    support.unread();
                    yaccValue = new NthRefNode(support.getPosition(), Integer.parseInt(tok().substring(1)));
                    return Token.tNTH_REF;

                default:
                    if (!isIdentifierChar(c)) {
                        support.unread();
                        return '$';
                    }
                case '0':
                    tokadd('$');
                }
                break;

            case '@':
                c = support.read();
                newToken();
                tokadd('@');
                if (c == '@') {
                    tokadd('@');
                    c = support.read();
                }
                if (Character.isDigit(c)) {
                    if (toklen() == 1) {
                        rb_compile_error("`@" + c + "' is not allowed as an instance variable name");
                    }
                    else {
                        rb_compile_error("`@@" + c + "' is not allowed as a class variable name");
                    }
                }
                if (!isIdentifierChar(c)) {
                    support.unread();
                    return '@';
                }
                break;

            case '_':
                if (was_bol() && whole_match_p("__END__", false)) {
                    return 0;
                }
                newToken();
                break;

            default:
                if (!isIdentifierChar(c)) {
                    rb_compile_error("Invalid char `\\" + c + "' in expression");
                    continue retry;
                }

                newToken();
                break;
            }
    
            do {
                tokadd(c);
                if (ismbchar(c)) {
                    int i, len = mbclen(c)-1;

                    for (i = 0; i < len; i++) {
                        c = support.read();
                        tokadd(c);
                    }
                }
                c = support.read();
            } while (isIdentifierChar(c));
            
            char peek = support.read();
            if ((c == '!' || c == '?') && isIdentifierChar(tok().charAt(0)) && peek != '=') {
                support.unread(); // put peek back.
                tokadd(c);
            } else {
                support.unreadMany(2); // put peek back.
            }
            
            int result = 0;

            switch (tok().charAt(0)) {
                case '$':
                    lex_state = LexState.EXPR_END;
                    result = Token.tGVAR;
                    break;
                case '@':
                    lex_state = LexState.EXPR_END;
                    if (tok().charAt(1) == '@') {
                        result = Token.tCVAR;
                    } else {
                        result = Token.tIVAR;
                    }
                    break;

                default:
                    if (toklast() == '!' || toklast() == '?') {
                        result = Token.tFID;
                    } else {
                        if (lex_state == LexState.EXPR_FNAME) {
                            if ((c = support.read()) == '=' && !peek('~') && !peek('>') &&
                                 (!peek('=') || (support.isEOL() && support.getCharAt(1) == '>'))) {
                                result = Token.tIDENTIFIER;
                                tokadd(c);
                            }
                            else {
                                support.unread();
                            }
                        }
                        if (result == 0 && ISUPPER(tok().charAt(0))) {
                            result = Token.tCONSTANT;
                        }
                        else {
                            result = Token.tIDENTIFIER;
                        }
                    }

                    if (lex_state != LexState.EXPR_DOT) {
                        /* See if it is a reserved word.  */
                        Keyword keyword = getKeyword(tok(), toklen());
                        if (keyword != null) {
                            // enum lex_state
                            LexState state = lex_state;

                            lex_state = keyword.state;
                            if (state.isExprFName()) {
                                yaccValue = keyword.name;
                            }
                            if (keyword.id0 == Token.kDO) {
                                if (conditionState.isInState()) {
                                    return Token.kDO_COND;
                                }
                                if (cmdArgumentState.isInState() && state != LexState.EXPR_CMDARG) {
                                    return Token.kDO_BLOCK;
                                }
                                if (state == LexState.EXPR_ENDARG) {
                                    return Token.kDO_BLOCK;
                                }
                                return Token.kDO;
                            }

                            if (state == LexState.EXPR_BEG) {
                                return keyword.id0;
                            } else {
                                if (keyword.id0 != keyword.id1)
                                    lex_state = LexState.EXPR_BEG;
                                return keyword.id1;
                            }
                        }
                    }

                    if (lex_state == LexState.EXPR_BEG ||
                            lex_state == LexState.EXPR_MID ||
                            lex_state == LexState.EXPR_DOT ||
                            lex_state == LexState.EXPR_ARG ||
                            lex_state == LexState.EXPR_CMDARG) {
                        if (commandState) {
                            lex_state = LexState.EXPR_CMDARG;
                        } else {
                            lex_state = LexState.EXPR_ARG;
                        }
                    } else {
                        lex_state = LexState.EXPR_END;
                    }
            }	
            yaccValue = tok();

            // Lame: parsing logic made it into lexer in ruby...So we
            // are emulating
            if (IdUtil.isLocal((String)yaccValue) &&
                ((parserSupport.getBlockNames().isInBlock() && 
                  parserSupport.getBlockNames().isDefined((String) yaccValue)) ||
                  parserSupport.getLocalNames().isLocalRegistered((String) yaccValue))) {
                lex_state = LexState.EXPR_END;
            }

            return result;
        }
    }

    /**
     *  Parse a number from the input stream.
     *
     *@param c The first character of the number.
     *@return A int constant wich represents a token.
     */
    private int parseNumber(char c) {
        lex_state = LexState.EXPR_END;

        StringBuffer number = new StringBuffer();

        if (c == '-') {
            number.append(c);
            c = support.read();
        } else if (c == '+') {
            c = support.read();
        }

        char nondigit = '\0';

        if (c == '0') {
            int startLen = number.length();

            switch ((c = support.read())) {
                case 'x' :
                case 'X' : //  hexadecimal
                    c = support.read();
                    if (isHexDigit(c)) {
                        for (;; c = support.read()) {
                            if (c == '_') {
                                if (nondigit != '\0') {
                                    break;
                                } else {
                                    nondigit = c;
                                }
                            } else if (isHexDigit(c)) {
                                nondigit = '\0';
                                number.append(c);
                            } else {
                                break;
                            }
                        }
                    }
                    support.unread();

                    if (number.length() == startLen) {
                        errorHandler.handleError(IErrors.SYNTAX_ERROR, Messages.getString("number_without_hex_digits")); //$NON-NLS-1$
                    } else if (nondigit != '\0') {
                        errorHandler.handleError(IErrors.SYNTAX_ERROR, Messages.getString("trailing_uc")); //$NON-NLS-1$
                        // return decode_num(c, is_float, seen_uc, true);
                    }
                    yaccValue = getInteger(number.toString(), 16);
                    return Token.tINTEGER;
                case 'b' :
                case 'B' : // binary
                    c = support.read();
                    if (c == '0' || c == '1') {
                        for (;; c = support.read()) {
                            if (c == '_') {
                                if (nondigit != '\0') {
                                    break;
                                } else {
                                    nondigit = c;
                                }
                            } else if (c == '0' || c == '1') {
                                nondigit = '\0';
                                number.append(c);
                            } else {
                                break;
                            }
                        }
                    }
                    support.unread();

                    if (number.length() == startLen) {
                        errorHandler.handleError(IErrors.SYNTAX_ERROR, Messages.getString("number_without_bin_digits")); //$NON-NLS-1$
                    } else if (nondigit != '\0') {
                        errorHandler.handleError(IErrors.SYNTAX_ERROR, Messages.getString("trailing_uc")); //$NON-NLS-1$
                        // return decode_num(c, is_float, seen_uc, true);
                    }
                    yaccValue = getInteger(number.toString(), 2);
                    return Token.tINTEGER;
                case 'd' :
                case 'D' : // decimal
                    c = support.read();
                    if (Character.isDigit(c)) {
                        for (;; c = support.read()) {
                            if (c == '_') {
                                if (nondigit != '\0') {
                                    break;
                                } else {
                                    nondigit = c;
                                }
                            } else if (Character.isDigit(c)) {
                                nondigit = '\0';
                                number.append(c);
                            } else {
                                break;
                            }
                        }
                    }
                    support.unread();

                    if (number.length() == startLen) {
                        errorHandler.handleError(IErrors.SYNTAX_ERROR, Messages.getString("number_without_bin_digits")); //$NON-NLS-1$
                    } else if (nondigit != '\0') {
                        errorHandler.handleError(IErrors.SYNTAX_ERROR, Messages.getString("trailing_uc")); //$NON-NLS-1$
                        // return decode_num(c, is_float, seen_uc, true);
                    }
                    yaccValue = getInteger(number.toString(), 2);
                    return Token.tINTEGER;
                case '0' :
                case '1' :
                case '2' :
                case '3' :
                case '4' :
                case '5' :
                case '6' :
                case '7' :
                case '_' : // octal
                    for (;; c = support.read()) {
                        if (c == '_') {
                            if (nondigit != '\0') {
                                break;
                            } else {
                                nondigit = c;
                            }
                        } else if (c >= '0' && c <= '7') {
                            nondigit = '\0';
                            number.append(c);
                        } else {
                            break;
                        }
                    }
                    if (number.length() > startLen) {
                        support.unread();

                        if (nondigit != '\0') {
                            errorHandler.handleError(IErrors.SYNTAX_ERROR, Messages.getString("trailing_uc")); //$NON-NLS-1$
                        }

                        yaccValue = getInteger(number.toString(), 8);
                        return Token.tINTEGER;
                    }
                case '8' :
                case '9' :
                    errorHandler.handleError(IErrors.SYNTAX_ERROR, Messages.getString("illegal_octal_digit")); //$NON-NLS-1$
                    break;
                case '.' :
                case 'e' :
                case 'E' :
                    number.append('0');
                    break;
                default :
                    support.unread();
                    yaccValue = new Long(0);
                    return Token.tINTEGER;
            }
        }

        boolean seen_point = false;
        boolean seen_e = false;

        for (;; c = support.read()) {
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
                    number.append(c);
                    break;
                case '.' :
                    if (nondigit != '\0') {
                        support.unread();
                        errorHandler.handleError(IErrors.SYNTAX_ERROR, support.getPosition(), Messages.getString("trailing_uc")); //$NON-NLS-1$
                    } else if (seen_point || seen_e) {
                        support.unread();
                        return getNumberToken(number.toString(), true, nondigit);
                    } else {
                        if (!Character.isDigit(c = support.read())) {
                            support.unread();
                            support.unread();
                            if (support.getLastRead() == '_') {

                            } else {
                                yaccValue = getInteger(number.toString(), 10);
                                return Token.tINTEGER;
                            }
                        } else {
                            number.append('.');
                            number.append(c);
                            seen_point = true;
                            nondigit = '\0';
                        }
                    }
                    break;
                case 'e' :
                case 'E' :
                    if (nondigit != '\0') {
                        support.unread();
                        errorHandler.handleError(IErrors.SYNTAX_ERROR, support.getPosition(), Messages.getString("trailing_uc")); //$NON-NLS-1$
                        return 0;
                    } else if (seen_e) {
                        support.unread();
                        return getNumberToken(number.toString(), true, nondigit);
                    } else {
                        number.append(c);
                        seen_e = true;
                        nondigit = c;
                        c = support.read();
                        if (c == '-' || c == '+') {
                            number.append(c);
                            nondigit = c;
                        } else {
                            support.unread();
                        }
                    }
                    break;
                case '_' : //  '_' in number just ignored
                    if (nondigit != '\0') {
                        errorHandler.handleError(IErrors.SYNTAX_ERROR, support.getPosition(), Messages.getString("trailing_uc")); //$NON-NLS-1$
                        return 0;
                    }
                    nondigit = c;
                    break;
                default :
                    support.unread();
                    return getNumberToken(number.toString(), seen_e || seen_point, nondigit);
            }
        }
    }

    private int getNumberToken(String number, boolean isFloat, char nondigit) {
        if (nondigit != '\0') {
            errorHandler.handleError(IErrors.SYNTAX_ERROR, Messages.getString("trailing_uc", String.valueOf(nondigit))); //$NON-NLS-1$
            return 0;
        }
        if (isFloat) {
            Double d = new Double(0.0);
            try {
                d = Double.valueOf(number);
            } catch (NumberFormatException e) {
                errorHandler.handleError(IErrors.WARN, support.getPosition(), Messages.getString("float_out_of_range", number)); //$NON-NLS-1$
            }
            yaccValue = d;
            return Token.tFLOAT;
        } else {
            yaccValue = getInteger(number, 10);
            return Token.tINTEGER;
        }
    }

    private final boolean isArgState() {
        return lex_state.isExprArg();
    }

    private static final Keyword getKeyword(String w, int len) {
        return Keyword.getKeyword(w, len);
    }

    private static final Object getInteger(String value, int radix) {
        try {
            return Long.valueOf(value, radix);
        } catch (NumberFormatException e) {
            return new BigInteger(value, radix);
        }
    }

    private static final boolean isIdentifierChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static final boolean isHexDigit(char c) {
        return Character.isDigit(c) || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
    }

    public void setErrorHandler(IErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public void setSource(ILexerSource source) {
        this.support = new RubyLexerSupport(source);
    }

    public void setState(ILexerState state) {
        this.lex_state = (LexState) state;
    }

    public StackState getCmdArgumentState() {
        return cmdArgumentState;
    }

    public StackState getConditionState() {
        return conditionState;
    }
}
