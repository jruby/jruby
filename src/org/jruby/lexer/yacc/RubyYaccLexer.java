/*
 * Copyright (C) 2001 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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

import java.io.EOFException;
import java.io.IOException;
import java.math.BigInteger;

import org.ablaf.common.IErrorHandler;
import org.ablaf.common.ISourcePosition;
import org.ablaf.lexer.ILexerSource;
import org.ablaf.lexer.ILexerState;
import org.ablaf.lexer.IYaccLexer;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.DXStrNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.types.IListNode;
import org.jruby.ast.util.ListNodeUtil;
import org.jruby.common.IErrors;
import org.jruby.parser.ReOptions;
import org.jruby.parser.Token;

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

    // ErrorHandler:
    private IErrorHandler errorHandler;

    // Lexer:
    private LexState lexState;
    private StringBuffer tokenBuffer = new StringBuffer(60); // FIX: replace with special class.

    private StackState conditionState = new StackState();
    private StackState cmdArgumentState = new StackState();

    // YaccLexer implementation:
    public boolean advance() throws IOException {
        return (token = yylex()) != 0;
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

    private int toklen() {
        return tokenBuffer.length();
    }

    private char toklast() {
        return tokenBuffer.charAt(toklen() - 1);
    }

    private void newToken() {
        tokenBuffer.setLength(0);
    }

    /** Parse a regexp.
     *  @param closeQuote the closing quote (what should be looked for)
     *  @param openQuote the opening quote (in case there is some possible nesting)
     *  @return the type of token parsed (0 for an error).
     */
    private int parseRegexp(char closeQuote, char openQuote) {
        ISourcePosition position = support.getPosition();

        DRegexpNode list = new DRegexpNode(position);

        StringToken token = new StringToken(support, errorHandler, position);

        int nest = 0;
        boolean lDyn = false;
        for (char c = support.read(); c != '\0' && (c != closeQuote || nest != 0); c = support.read()) {
            switch (c) {
                case '#' :
                    try {
                        lDyn = parseExpressionString(list, closeQuote, token) || lDyn;
                    } catch (EOFException e) {
                        errorHandler.handleError(IErrors.COMPILE_ERROR, position, Messages.getString("unterminated_regexp")); //$NON-NLS-1$
                        return 0;
                    }
                    continue;
                case '\\' :
                    try {
                        token.appendEscape(closeQuote);
                    } catch (EOFException e) {
                        return 0;
                    }
                    continue;
                case '\0' :
                    errorHandler.handleError(IErrors.COMPILE_ERROR, position, Messages.getString("unterminated_regexp")); //$NON-NLS-1$
                    return 0;
                default :
                    if (openQuote != '\0') {
                        if (c == openQuote) {
                            nest++;
                        } else if (c == closeQuote) {
                            nest--;
                        }
                    }
            }
            token.append(c);
        }

        int options = 0;
        boolean once = false;
        byte kcode = 0;

        for (char c = support.read();; c = support.read()) {
            switch (c) {
                case 'i' :
                    options |= ReOptions.RE_OPTION_IGNORECASE;
                    continue;
                case 'x' :
                    options |= ReOptions.RE_OPTION_EXTENDED;
                    continue;
                case 'p' : // /p is obsolete
                    errorHandler.handleError(IErrors.WARN, position, Messages.getString("opsolete_slash_p_option")); //$NON-NLS-1$
                    options |= ReOptions.RE_OPTION_POSIXLINE;
                    continue;
                case 'm' :
                    options |= ReOptions.RE_OPTION_MULTILINE;
                    continue;
                case 'o' :
                    once = true;
                    break;
                case 'n' :
                    kcode = 16;
                    continue;
                case 'e' :
                    kcode = 32;
                    continue;
                case 's' :
                    kcode = 48;
                    continue;
                case 'u' :
                    kcode = 64;
                    continue;
                default :
                    support.unread();
            }
            break;
        }

        lexState = LexState.EXPR_END;
        if (lDyn) {
            if (token.getLength() > 0) {
                list.add(new StrNode(token.getPosition(), token.getToken()));
            }
            list.setOptions(options | kcode);
            list.setOnce(once);
            yaccValue = list;
            return Token.tDREGEXP;
        }
        yaccValue = new RegexpNode(position, token.getToken(), options | kcode);
        return Token.tREGEXP;
    }

    /**
     * parses a String.
     * this is actually used to parse single quoted strings, double quoted strings
     * and backquoted strings.
     * If the string is a SingleQuoted String the parsing is delegated to parseSingleQuotedString.
     * SubExpression (introduced by #) are parsed by the parseExpressionString method.
     *
     * @param func the type of String being parsed (single, double or back quoted)
     * @param closeQuote the closing quote to look for
     * @param openQuote the opening quote (used when there is a potential nesting problem),
     * 					this should be \0 when the openQuote, closeQuote pair do not allow nesting.
     * @return the type of token parsed (0 for an error).
     **/
    private int parseString(char func, char closeQuote, char openQuote) {
        if (func == '\'') {
            return parseSingleQuotedString(closeQuote, openQuote);
        }

        if (func == 0) {
            // read 1 line for heredoc
            // -1 for chomp
            yaccValue = support.readLine();
            return Token.tSTRING;
        }

        ISourcePosition position = support.getPosition();

        StringToken token = new StringToken(support, errorHandler, position);

        IListNode list = (func == '`') ? (IListNode) new DXStrNode(position) : new DStrNode(position);

        int nest = 0;
        boolean lDyn = false;
        for (char c = support.read(); c != closeQuote || nest > 0; c = support.read()) {
            switch (c) {
                case '\0' :
                    errorHandler.handleError(IErrors.COMPILE_ERROR, position, Messages.getString("unterminated_string")); //$NON-NLS-1$
                    return 0;
                case '#' :
                    try {
                        lDyn = parseExpressionString(list, closeQuote, token) || lDyn;
                    } catch (EOFException e) {
                        errorHandler.handleError(IErrors.COMPILE_ERROR, position, Messages.getString("unterminated_string")); //$NON-NLS-1$
                        return 0;
                    }
                    continue;
                case '\\' :
                    c = support.read();
                    if (c == '\n') {
                        continue;
                    } else if (c == closeQuote) {
                        token.append(c);
                    } else {
                        support.unread();
                        if (func != '"') {
                            token.append('\\');
                        }
                        try {
                            token.append(support.readEscape());
                        } catch (LexerException lExcptn) {
                            token.append('\0');
                            errorHandler.handleError(IErrors.SYNTAX_ERROR, support.getPosition(), lExcptn.getMessage());
                        }
                    }
                    continue;
                default :
                    if (openQuote != '\0') {
                        if (c == openQuote) {
                            nest++;
                        }
                        if (c == closeQuote && nest-- == 0) {
                            break;
                        }
                    }
                    token.append(c);
            }
        }

        lexState = LexState.EXPR_END;

        if (lDyn) //we know the string is dynamic
            {
            if (token.getLength() > 0) {
                list.add(new StrNode(token.getPosition(), token.getToken()));
            }
            yaccValue = list;
            return (func == '`') ? Token.tDXSTRING : Token.tDSTRING;
        }
        yaccValue = token.getToken();
        return (func == '`') ? Token.tXSTRING : Token.tSTRING;
    }

    /** Parse a single quoted string (', or %q).
     *
     *  @param closeQuote the closing quote (what should be looked for)
     *  @param openQuote the opening quote (in case there is some possible nesting)
     *  @return the type of token parsed (0 for an error).
     */
    private int parseSingleQuotedString(char closeQuote, char openQuote) {
        ISourcePosition position = support.getPosition();

        int nest = 0;

        StringBuffer stringToken = new StringBuffer();

        for (char c = support.read(); c != closeQuote || nest > 0; c = support.read()) {
            switch (c) {
                case '\0' :
                    errorHandler.handleError(IErrors.COMPILE_ERROR, position, Messages.getString("unterminated_string")); //$NON-NLS-1$
                    return 0;
                case '\\' :
                    c = support.read();
                    switch (c) {
                        case '\n' :
                            continue;
                        case '\\' :
                            c = '\\';
                            break;
                        default :
                            // fall through
                            if (c == closeQuote || (openQuote != '\0' && c == openQuote)) {
                                stringToken.append(c);
                                continue;
                            }
                            stringToken.append('\\');
                    }
                default :
                    if (openQuote != '\0') {
                        if (c == openQuote) {
                            nest++;
                        }
                        if (c == closeQuote && nest-- == 0) {
                            break;
                        }
                    }
                    stringToken.append(c);
            }
        }

        lexState = LexState.EXPR_END;

        yaccValue = stringToken.toString();
        return Token.tSTRING;
    }

    /** parse quoted words (%w{})
     *
     */
    private int parseQuotedWords(char closeQuote, char openQuote) {
        char c = support.read();

        ISourcePosition position = support.getPosition();

        // Skip preceding spaces.
        while (Character.isWhitespace(c)) {
            c = support.read();
        }
        support.unread();

        ArrayNode qwords = new ArrayNode(position); // FIX
        int nest = 0;

        StringBuffer stringToken = new StringBuffer();

        while ((c = support.read()) != closeQuote || nest > 0) {
            if (c == '\0') {
                errorHandler.handleError(IErrors.COMPILE_ERROR, position, Messages.getString("unterminated_string")); //$NON-NLS-1$
                return 0;
            }
            if (c == '\\') {
                c = support.read();
                switch (c) {
                    case '\n' :
                        continue;
                    case '\\' :
                        c = '\\';
                        break;
                    default :
                        if (c == closeQuote || (openQuote != 0 && c == openQuote)) {
                            stringToken.append(c);
                            continue;
                        }
                        if (!Character.isWhitespace(c)) {
                            stringToken.append('\\');
                        }
                        break;
                }
            } else if (Character.isWhitespace(c)) {
                qwords.add(new StrNode(support.getPosition(), stringToken.toString()));
                stringToken.setLength(0); //Benoit: reset the buffer
                // skip continuous spaces
                c = support.read();
                while (Character.isWhitespace(c)) {
                    c = support.read();
                }
                support.unread();
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
            stringToken.append(c);
        }
        if (stringToken.length() > 0) {
            qwords.add(new StrNode(support.getPosition(), stringToken.toString()));
        }
        lexState = LexState.EXPR_END;
        yaccValue = qwords;
        return Token.tARRAY;
    }

    /**
     * fixme: position for DStrNode and DXStrNode
     */
    private int parseHereDocument(char closeQuote, boolean indent) {
        char c;
        ISourcePosition position = support.getPosition();
        newToken();
        switch (closeQuote) {
            case '\'' :
            case '"' :
            case '`' :
                while ((c = support.read()) != closeQuote) {
                    tokenBuffer.append(c);
                }
                if (closeQuote == '\'') {
                    closeQuote = 0;
                }
                break;
            default :
                c = closeQuote;
                closeQuote = '"';
                if (!isIdentifierChar(c)) {
                    errorHandler.handleError(IErrors.WARN, support.getPosition(), Messages.getString("deprecated_bare_<<")); //$NON-NLS-1$
                    break;
                }

                while (isIdentifierChar(c)) {
                    tokenBuffer.append(c);
                    c = support.read();
                }
                support.unread();
                break;
        }

        ISourcePosition startPosition = support.getPosition();
        String buffer = support.readLine() + '\n';

        String eos = tok();

        StringBuffer sb = new StringBuffer();

        IListNode list = null;

        while (true) {
            // test if the end of file or end of string is reached.
            String line = support.readLine();
            if (line == null) {
                errorHandler.handleError(IErrors.COMPILE_ERROR, position, Messages.getString("cannot_found_string_before_eof", eos)); //$NON-NLS-1$
                return 0;
            } else if ((indent && line.trim().startsWith(eos)) || line.startsWith(eos)) {
                if (line.trim().length() == eos.length()) {
                    break;
                }
            }
            support.unreadMany(line.length() + 1);

            while (true) {
                switch (parseString(closeQuote, '\n', '\n')) {
                    case Token.tSTRING :
                    case Token.tXSTRING :
                        yaccValue = (String) yaccValue + '\n';
                        if (list == null) {
                            sb.append(yaccValue);
                        } else {
                            list.add(new StrNode(support.getPosition(), (String) yaccValue));
                        }
                        break;
                    case Token.tDSTRING :
                        if (list == null) {
                            list = new DStrNode(support.getPosition()); // FIXME position
                            list.add(new StrNode(support.getPosition(), sb.toString()));
                        }
                        // fall through
                    case Token.tDXSTRING :
                        if (list == null) {
                            list = new DXStrNode(support.getPosition()); // FIXME position
                            list.add(new StrNode(support.getPosition(), sb.toString()));
                        }
                        ListNodeUtil.addAll(list, (IListNode) yaccValue);
                        list.add(new StrNode(support.getPosition(), "\n")); //$NON-NLS-1$
                        break;
                    case 0 :
                        errorHandler.handleError(IErrors.COMPILE_ERROR, position, Messages.getString("cannot_found_string_before_eof", eos)); //$NON-NLS-1$
                        return 0;
                }
                if (support.getLastRead() == '\n') { // isEOL()) {
                    break;
                }
            }
        }

        lexState = LexState.EXPR_END;
        support.setBuffer(buffer, startPosition);

        switch (closeQuote) {
            case '\0' :
            case '\'' :
            case '"' :
                if (list != null) {
                    yaccValue = list;
                    return Token.tDSTRING;
                } else {
                    yaccValue = sb.toString();
                    return Token.tSTRING;
                }
            case '`' :
                if (list != null) {
                    yaccValue = list;
                    return Token.tDXSTRING;
                } else {
                    yaccValue = sb.toString();
                    return Token.tXSTRING;
                }
        }
        return 0;
    }

    private void arg_ambiguous() {
        errorHandler.handleError(IErrors.WARNING, support.getPosition(), Messages.getString("ambiguous_first_argument")); //$NON-NLS-1$
    }

    ISourcePosition currentPos;
    public ISourcePosition getPosition() {
        return currentPos;
    }

    /**
     *  Returns the next token. Also sets yyVal is needed.
     *
     *@return    Description of the Returned Value
     */
    private int yylex() {
        char c;
        boolean spaceSeen = false;

        //first eat as much space as possible then grab the position of the
        //beginning of the token
        for (;;) {
            switch (c = support.read()) {
                case '\0' : // NUL
                case '\004' : // ^D
                case '\032' : // ^Z
                    return 0;
                    // white spaces
                case ' ' :
                case '\t' :
                case '\f' :
                case '\r' :
                case '\u0013' : // '\v'
                    spaceSeen = true;
                    continue;
                case '#' : // it's a comment
                    while ((c = support.read()) != '\n') {
                        if (c == '\0') {
                            return 0;
                        }
                    }
                    // fall through
                case '\n' :
                    if (lexState.isExprBeg() || lexState.isExprFName() || lexState.isExprDot()) {
                        continue;
                    }
                    lexState = LexState.EXPR_BEG;
                    return '\n';
                case '=' :
                    if (support.getPosition().getColumn() == 1) {
                        // skip embedded rd document
                        if (support.isNext("begin") && Character.isWhitespace(support.getCharAt(6))) {
                            for (;;) {
                                // col = lex_pend;
                                c = support.read();
                                if (c == '\0') {
                                    errorHandler.handleError(IErrors.COMPILE_ERROR, Messages.getString("unterminated_embedded_document")); //$NON-NLS-1$
                                    return 0;
                                } else if (c != '=') {
                                    continue;
                                } else if (support.getPosition().getColumn() == 1 && support.isNext("end") && Character.isWhitespace(support.getCharAt(4))) { //$NON-NLS-1$
                                    break;
                                }
                            }
                            support.readLine();
                            continue;
                        }
                    }

            }
            break;
        }
        support.unread();
        currentPos = support.getPosition();
        retry : for (;;) {
            switch (c = support.read()) {
                case '*' :
                    if ((c = support.read()) == '*') {
                        if (support.read() == '=') {
                            lexState = LexState.EXPR_BEG;
                            yaccValue = "**"; //$NON-NLS-1$
                            return Token.tOP_ASGN;
                        }
                        support.unread();
                        c = (char) Token.tPOW;
                    } else {
                        if (c == '=') {
                            yaccValue = "*"; //$NON-NLS-1$
                            lexState = LexState.EXPR_BEG;
                            return Token.tOP_ASGN;
                        }
                        support.unread();
                        if (isArgState() && spaceSeen && !Character.isWhitespace(c)) {
                            errorHandler.handleError(IErrors.WARNING, support.getPosition(), Messages.getString("star_interpreted_as_argument_prefix")); //$NON-NLS-1$
                            c = (char) Token.tSTAR;
                        } else if (lexState.isExprBeg() || lexState.isExprMid()) {
                            c = (char) Token.tSTAR;
                        } else {
                            c = '*';
                        }
                    }
                    if (lexState.isExprFName() || lexState.isExprDot()) {
                        lexState = LexState.EXPR_ARG;
                    } else {
                        lexState = LexState.EXPR_BEG;
                    }
                    return c;
                case '!' :
                    lexState = LexState.EXPR_BEG;
                    if ((c = support.read()) == '=') {
                        return Token.tNEQ;
                    }
                    if (c == '~') {
                        return Token.tNMATCH;
                    }
                    support.unread();
                    return '!';
                case '=' :
                    if (lexState.isExprFName() || lexState.isExprDot()) {
                        lexState = LexState.EXPR_ARG;
                    } else {
                        lexState = LexState.EXPR_BEG;
                    }
                    if ((c = support.read()) == '=') {
                        if ((c = support.read()) == '=') {
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
                case '<' :
                    c = support.read();
                    if (c == '<'
                        && !lexState.isExprEnd()
                        && !lexState.isExprEndArg()
                        && !lexState.isExprClass()
                        && (!isArgState() || spaceSeen)) {
                        char c2 = support.read();
                        boolean indent = false;
                        if (c2 == '-') {
                            indent = true;
                            c2 = support.read();
                        }
                        if ("\"'`".indexOf(c2) != -1 || isIdentifierChar(c2)) { //$NON-NLS-1$
                            return parseHereDocument(c2, indent);
                        } else {
                            support.unread();
                        }
                    }
                    if (lexState.isExprFName() || lexState.isExprDot()) {
                        lexState = LexState.EXPR_ARG;
                    } else {
                        lexState = LexState.EXPR_BEG;
                    }
                    if (c == '=') {
                        if ((c = support.read()) == '>') {
                            return Token.tCMP;
                        }
                        support.unread();
                        return Token.tLEQ;
                    }
                    if (c == '<') {
                        if (support.read() == '=') {
                            lexState = LexState.EXPR_BEG;
                            yaccValue = "<<"; //$NON-NLS-1$
                            return Token.tOP_ASGN;
                        }
                        support.unread();
                        return Token.tLSHFT;
                    }
                    support.unread();
                    return '<';
                case '>' :
                    if (lexState.isExprFName() || lexState.isExprDot()) {
                        lexState = LexState.EXPR_ARG;
                    } else {
                        lexState = LexState.EXPR_BEG;
                    }
                    if ((c = support.read()) == '=') {
                        return Token.tGEQ;
                    }
                    if (c == '>') {
                        if ((c = support.read()) == '=') {
                            lexState = LexState.EXPR_BEG;
                            yaccValue = ">>"; //$NON-NLS-1$
                            return Token.tOP_ASGN;
                        }
                        support.unread();
                        return Token.tRSHFT;
                    }
                    support.unread();
                    return '>';
                case '"' :
                    return parseString(c, c, '\0');
                case '`' :
                    if (lexState.isExprFName()) {
                        return c;
                    }
                    if (lexState.isExprDot()) {
                        return c;
                    }
                    return parseString(c, c, '\0');
                case '\'' :
                    return parseSingleQuotedString(c, '\0');
                case '?' :
                    if (lexState.isExprEnd()) {
                        lexState = LexState.EXPR_BEG;
                        return '?';
                    }
                    c = support.read();
                    if (c == '\0') {
                        errorHandler.handleError(IErrors.COMPILE_ERROR, support.getPosition(), Messages.getString("incomplete_character_syntax")); //$NON-NLS-1$
                        return 0;
                    }
                    if (isArgState() && Character.isWhitespace(c)) {
                        support.unread();
                        lexState = LexState.EXPR_BEG;
                        return '?';
                    }
                    if (c == '\\') {
                        try {
                            c = support.readEscape();
                        } catch (LexerException lExcptn) {
                            c = '\0';
                            errorHandler.handleError(IErrors.SYNTAX_ERROR, support.getPosition(), lExcptn.getMessage());
                        }
                    }
                    c &= 0xff;

                    lexState = LexState.EXPR_END;

                    yaccValue = new Long(c);
                    return Token.tINTEGER;
                case '&' :
                    if ((c = support.read()) == '&') {
                        lexState = LexState.EXPR_BEG;
                        if ((c = support.read()) == '=') {
                            yaccValue = "&&"; //$NON-NLS-1$
                            return Token.tOP_ASGN;
                        }
                        support.unread();
                        return Token.tANDOP;
                    } else if (c == '=') {
                        yaccValue = "&"; //$NON-NLS-1$
                        lexState = LexState.EXPR_BEG;
                        return Token.tOP_ASGN;
                    }
                    support.unread();
                    if (isArgState() && spaceSeen && !Character.isWhitespace(c)) {
                        errorHandler.handleError(IErrors.WARNING, support.getPosition(), Messages.getString("amp_interpreted_as_argument_prefix")); //$NON-NLS-1$
                        c = (char) Token.tAMPER;
                    } else if (lexState.isExprBeg() || lexState.isExprMid()) {
                        c = (char) Token.tAMPER;
                    } else {
                        c = '&';
                    }
                    if (lexState.isExprFName() || lexState.isExprDot()) {
                        lexState = LexState.EXPR_ARG;
                    } else {
                        lexState = LexState.EXPR_BEG;
                    }
                    return c;
                case '|' :
                    if ((c = support.read()) == '|') {
                        lexState = LexState.EXPR_BEG;
                        if ((c = support.read()) == '=') {
                            yaccValue = "||"; //$NON-NLS-1$
                            return Token.tOP_ASGN;
                        }
                        support.unread();
                        return Token.tOROP;
                    } else if (c == '=') { // XXX 'else' removed in 1.6.7
                        lexState = LexState.EXPR_BEG;
                        yaccValue = "|"; //$NON-NLS-1$
                        return Token.tOP_ASGN;
                    }
                    if (lexState.isExprFName() || lexState.isExprDot()) {
                        lexState = LexState.EXPR_ARG;
                    } else {
                        lexState = LexState.EXPR_BEG;
                    }
                    support.unread();
                    return '|';
                case '+' :
                    c = support.read();
                    if (lexState.isExprFName() || lexState.isExprDot()) {
                        lexState = LexState.EXPR_ARG;
                        if (c == '@') {
                            return Token.tUPLUS;
                        }
                        support.unread();
                        return '+';
                    }
                    if (c == '=') {
                        lexState = LexState.EXPR_BEG;
                        yaccValue = "+"; //$NON-NLS-1$
                        return Token.tOP_ASGN;
                    }
                    if (lexState.isExprBeg()
                        || lexState.isExprMid()
                        || (isArgState() && spaceSeen && !Character.isWhitespace(c))) {
                        if (isArgState()) {
                            arg_ambiguous();
                        }
                        lexState = LexState.EXPR_BEG;
                        support.unread();
                        if (Character.isDigit(c)) {
                            c = '+';
                            return parseNumber(c);
                        }
                        return Token.tUPLUS;
                    }
                    lexState = LexState.EXPR_BEG;
                    support.unread();
                    return '+';
                case '-' :
                    c = support.read();
                    if (lexState.isExprFName() || lexState.isExprDot()) {
                        lexState = LexState.EXPR_ARG;
                        if (c == '@') {
                            return Token.tUMINUS;
                        }
                        support.unread();
                        return '-';
                    }
                    if (c == '=') {
                        lexState = LexState.EXPR_BEG;
                        yaccValue = "-"; //$NON-NLS-1$
                        return Token.tOP_ASGN;
                    }
                    if (lexState.isExprBeg()
                        || lexState.isExprMid()
                        || (isArgState() && spaceSeen && !Character.isWhitespace(c))) {
                        if (isArgState()) {
                            arg_ambiguous();
                        }
                        lexState = LexState.EXPR_BEG;
                        support.unread();
                        if (Character.isDigit(c)) {
                            c = '-';
                            return parseNumber(c);
                        }
                        return Token.tUMINUS;
                    }
                    lexState = LexState.EXPR_BEG;
                    support.unread();
                    return '-';
                case '.' :
                    lexState = LexState.EXPR_BEG;
                    if ((c = support.read()) == '.') {
                        if ((c = support.read()) == '.') {
                            return Token.tDOT3;
                        }
                        support.unread();
                        return Token.tDOT2;
                    }
                    support.unread();
                    if (!Character.isDigit(c)) {
                        lexState = LexState.EXPR_DOT;
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
                case ']' :
                case '}' :
                case ')' :
                    conditionState.restart();
                    cmdArgumentState.restart();
                    lexState = LexState.EXPR_END;
                    return c;
                case ':' :
                    c = support.read();
                    if (c == ':') {
                        if (lexState.isExprBeg() || lexState.isExprMid() || (isArgState() && spaceSeen)) {
                            lexState = LexState.EXPR_BEG;
                            return Token.tCOLON3;
                        }
                        lexState = LexState.EXPR_DOT;
                        return Token.tCOLON2;
                    }
                    support.unread();
                    if (lexState.isExprEnd() || Character.isWhitespace(c)) {
                        lexState = LexState.EXPR_BEG;
                        return ':';
                    }
                    lexState = LexState.EXPR_FNAME;
                    return Token.tSYMBEG;
                case '/' :
                    if (lexState.isExprBeg() || lexState.isExprMid()) {
                        return parseRegexp('/', '/');
                    }
                    if ((c = support.read()) == '=') {
                        lexState = LexState.EXPR_BEG;
                        yaccValue = "/"; //$NON-NLS-1$
                        return Token.tOP_ASGN;
                    }
                    support.unread();
                    if (isArgState() && spaceSeen) {
                        if (!Character.isWhitespace(c)) {
                            arg_ambiguous();
                            return parseRegexp('/', '/');
                        }
                    }
                    if (lexState.isExprFName() || lexState.isExprDot()) {
                        lexState = LexState.EXPR_ARG;
                    } else {
                        lexState = LexState.EXPR_BEG;
                    }
                    return '/';
                case '^' :
                    if ((c = support.read()) == '=') {
                        lexState = LexState.EXPR_BEG;
                        yaccValue = "^"; //$NON-NLS-1$
                        return Token.tOP_ASGN;
                    }
                    if (lexState.isExprFName() || lexState.isExprDot()) {
                        lexState = LexState.EXPR_ARG;
                    } else {
                        lexState = LexState.EXPR_BEG;
                    }
                    support.unread();
                    return '^';
                case ',' :
                case ';' :
                    lexState = LexState.EXPR_BEG;
                    return c;
                case '~' :
                    if (lexState.isExprFName() || lexState.isExprDot()) {
                        if ((c = support.read()) != '@') {
                            support.unread();
                        }
                    }
                    if (lexState.isExprFName() || lexState.isExprDot()) {
                        lexState = LexState.EXPR_ARG;
                    } else {
                        lexState = LexState.EXPR_BEG;
                    }
                    return '~';
                case '(' :
                    if (lexState.isExprBeg() || lexState.isExprMid()) {
                        c = (char) Token.tLPAREN;
                    } else if (lexState.isExprArg() && spaceSeen) {
                        errorHandler.handleError(IErrors.WARNING, support.getPosition(), tok() + Messages.getString("interpreted_as_method_call")); //$NON-NLS-1$
                    }
                    conditionState.stop();
                    cmdArgumentState.stop();
                    lexState = LexState.EXPR_BEG;
                    return c;
                case '[' :
                    if (lexState.isExprFName() || lexState.isExprDot()) {
                        lexState = LexState.EXPR_ARG;
                        if ((c = support.read()) == ']') {
                            if ((c = support.read()) == '=') {
                                return Token.tASET;
                            }
                            support.unread();
                            return Token.tAREF;
                        }
                        support.unread();
                        return '[';
                    } else if (lexState.isExprBeg() || lexState.isExprMid()) {
                        c = (char) Token.tLBRACK;
                    } else if (isArgState() && spaceSeen) {
                        c = (char) Token.tLBRACK;
                    }
                    conditionState.stop();
                    cmdArgumentState.stop();
                    lexState = LexState.EXPR_BEG;
                    return c;
                case '{' :
                    if (!lexState.isExprEnd() && !lexState.isExprArg()) {
                        c = (char) Token.tLBRACE;
                    }
                    conditionState.stop();
                    cmdArgumentState.stop();
                    lexState = LexState.EXPR_BEG;
                    return c;
                case '\\' :
                    c = support.read();
                    if (c == '\n') {
                        spaceSeen = true;
                        continue retry; // skip \\n
                    }
                    support.unread();
                    return '\\';
                case '%' :
                    if (lexState.isExprBeg() || lexState.isExprMid()) {
                        return parseQuotation();
                    }
                    if ((c = support.read()) == '=') {
                        yaccValue = "%"; //$NON-NLS-1$
                        return Token.tOP_ASGN;
                    }
                    if (isArgState() && spaceSeen && !Character.isWhitespace(c)) {
                        support.unread();
                        return parseQuotation();
                    }
                    if (lexState.isExprFName() || lexState.isExprDot()) {
                        lexState = LexState.EXPR_ARG;
                    } else {
                        lexState = LexState.EXPR_BEG;
                    }
                    support.unread();
                    return '%';
                case '$' :
                    lexState = LexState.EXPR_END;
                    newToken();
                    c = support.read();
                    switch (c) {
                        case '_' : // $_: last read line string
                            c = support.read();
                            if (isIdentifierChar(c)) {
                                tokenBuffer.append('$');
                                tokenBuffer.append('_');
                                break;
                            }
                            support.unread();
                            c = '_';
                            // fall through
                        case '~' : // $~: match-data
                            // parserSupport.getLocalNames().getLocalIndex(String.valueOf(c));
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
                            tokenBuffer.append('$');
                            tokenBuffer.append(c);
                            yaccValue = tok();
                            return Token.tGVAR;
                        case '-' :
                            tokenBuffer.append('$');
                            tokenBuffer.append(c);
                            c = support.read();
                            tokenBuffer.append(c);
                            yaccValue = tok();
                            /* xxx shouldn't check if valid option variable */
                            return Token.tGVAR;
                        case '&' : // $&: last match
                        case '`' : // $`: string before last match
                        case '\'' : // $': string after last match
                        case '+' : // $+: string matches last paren.
                            yaccValue = new BackRefNode(support.getPosition(), c);
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
                            tokenBuffer.append('$');
                            while (Character.isDigit(c)) {
                                tokenBuffer.append(c);
                                c = support.read();
                            }
                            if (isIdentifierChar(c)) {
                                break;
                            }
                            support.unread();
                            yaccValue = new NthRefNode(support.getPosition(), Integer.parseInt(tok().substring(1)));
                            return Token.tNTH_REF;
                        default :
                            if (!isIdentifierChar(c)) {
                                support.unread();
                                return '$';
                            }
                        case '0' :
                            tokenBuffer.append('$');
                    }
                    break;
                case '@' :
                    c = support.read();
                    newToken();
                    tokenBuffer.append('@');
                    if (c == '@') {
                        tokenBuffer.append('@');
                        c = support.read();
                    }
                    if (Character.isDigit(c)) {
                        errorHandler.handleError(
                            IErrors.COMPILE_ERROR,
                            support.getPosition(),
                            Messages.getString("invalid_instance_variable_name", String.valueOf(c)));
                        //$NON-NLS-1$
                    }
                    if (!isIdentifierChar(c)) {
                        support.unread();
                        return '@';
                    }
                    break;
                default :
                    if (!isIdentifierChar(c) || Character.isDigit(c)) {
                        errorHandler.handleError(
                            IErrors.COMPILE_ERROR,
                            support.getPosition(),
                            Messages.getString("invalid_char_in_expression", String.valueOf(c)));
                        //$NON-NLS-1$
                        continue retry;
                    }

                    newToken();
                    break;
            }
            break retry;
        }

        while (isIdentifierChar(c)) {
            tokenBuffer.append(c);
            c = support.read();
        }
        if ((c == '!' || c == '?') && isIdentifierChar(tok().charAt(0)) && !support.isNext('=')) {
            tokenBuffer.append(c);
        } else {
            support.unread();
        }
        {
            int result = 0;

            switch (tok().charAt(0)) {
                case '$' :
                    lexState = LexState.EXPR_END;
                    result = Token.tGVAR;
                    break;
                case '@' :
                    lexState = LexState.EXPR_END;
                    if (tok().charAt(1) == '@') {
                        result = Token.tCVAR;
                    } else {
                        result = Token.tIVAR;
                    }
                    break;
                default :
                    if (!lexState.isExprDot()) {
                        // See if it is a reserved word.
                        Keyword keyword = getKeyword(tok(), toklen());
                        if (keyword != null) {
                            // enum lex_state
                            LexState state = lexState;
                            lexState = keyword.state;
                            if (state.isExprFName()) {
                                yaccValue = keyword.name;
                            }
                            if (keyword.id0 == Token.kDO) {
                                if (conditionState.isInState()) {
                                    return Token.kDO_COND;
                                }
                                if (cmdArgumentState.isInState()) {
                                    return Token.kDO_BLOCK;
                                }
                                return Token.kDO;
                            }
                            if (state.isExprBeg()) {
                                return keyword.id0;
                            } else {
                                if (keyword.id0 != keyword.id1) {
                                    lexState = LexState.EXPR_BEG;
                                }
                                return keyword.id1;
                            }
                        }
                    }

                    if (toklast() == '!' || toklast() == '?') {
                        result = Token.tFID;
                    } else {
                        if (lexState.isExprFName()) {
                            if ((c = support.read()) == '='
                                && !support.isNext('~')
                                && !support.isNext('>')
                                && (!support.isNext('=') || support.getCharAt(1) == '>')) {
                                result = Token.tIDENTIFIER;
                                tokenBuffer.append(c);
                            } else {
                                support.unread();
                            }
                        }
                        if (result == 0 && Character.isUpperCase(tok().charAt(0))) {
                            result = Token.tCONSTANT;
                        } else {
                            result = Token.tIDENTIFIER;
                        }
                    }
                    if (lexState.isExprBeg() || lexState.isExprDot() || lexState.isExprArg()) {
                        lexState = LexState.EXPR_ARG;
                    } else {
                        lexState = LexState.EXPR_END;
                    }
            }
            yaccValue = tok();
            return result;
        }
    }

    /** Parse a general delimited string (%q, %Q, %x, %), string array (%w), or
     * regular expression (%r).
     *
     * @return a Token.
     */
    private int parseQuotation() {
        char type = support.read();
        char openQuote = type;

        if (Character.isLetterOrDigit(type)) {
            openQuote = support.read();
        } else {
            type = 'Q';
        }

        if (type == '\0' || openQuote == '\0') {
            errorHandler.handleError(IErrors.COMPILE_ERROR, support.getPosition(), Messages.getString("unterminated_quoted_string")); //$NON-NLS-1$
            return 0;
        }

        char closeQuote = openQuote;
        if (openQuote == '(') {
            closeQuote = ')';
        } else if (openQuote == '[') {
            closeQuote = ']';
        } else if (openQuote == '{') {
            closeQuote = '}';
        } else if (openQuote == '<') {
            closeQuote = '>';
        } else {
            openQuote = '\0';
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
                errorHandler.handleError(
                    IErrors.SYNTAX_ERROR,
                    support.getPosition(),
                    Messages.getString("unknown_quotation_type", String.valueOf(type)));
                //$NON-NLS-1$
                return 0;
        }
    }

    /**
     *  Parse a number from the input stream.
     *
     *@param c The first character of the number.
     *@return A int constant wich represents a token.
     */
    private int parseNumber(char c) {
        lexState = LexState.EXPR_END;

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
                        char oldC = c;
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
            //trailing_uc:
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

    /**
     *  This methods parse the #{}, #@ and #$ in strings.
     *	this is used when parsing Strings, XStrings and Regexp,
     *
     * @param list  the Node being parsed.
     * @param closeQuote the closing quote for the StringLikeNode being parsed
     * @param token the preceding token for this StringLikeNode.
     * @return       true if an expression was parsed, false otherwise
     */
    private boolean parseExpressionString(IListNode list, char closeQuote, StringToken token) throws EOFException {
        char c = support.read();

        switch (c) {
            case '$' :
            case '@' :
            case '{' :
                break;
            default :
                token.append('#');
                support.unread();
                return false;
        }

        if (token.getLength() > 0) {
            list.add(new StrNode(token.getPosition(), token.getToken()));
        }

        token.newToken(support.getPosition());

        char brace = '\0';
        int nest;

        fetch_id : for (;;) {
            switch (c) {
                case '$' :
                    token.append('$');
                    c = support.read();
                    if (c == '\0') {
                        throw new EOFException();
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
                            for (; Character.isDigit(c); c = support.read()) {
                                token.append(c);
                            }
                            support.unread();
                            list.add(
                                new NthRefNode(token.getPosition(), Integer.parseInt(token.getToken().substring(1))));
                            token.newToken(support.getPosition());
                            return true;
                        case '&' :
                        case '+' :
                            list.add(new BackRefNode(token.getPosition(), c));
                            token.newToken(support.getPosition());
                            return true;
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
                            token.append(c);
                            list.add(new GlobalVarNode(token.getPosition(), token.getToken()));
                            token.newToken(support.getPosition());
                            return true;
                        default :
                            if (c == closeQuote) {
                                list.add(new StrNode(token.getPosition(), "#$")); //$NON-NLS-1$
                                support.unread();
                                token.newToken(support.getPosition());
                                return true;
                            }
                            switch (c) {
                                case '\"' :
                                case '/' :
                                    token.append(c);
                                    list.add(new GlobalVarNode(token.getPosition(), token.getToken()));
                                    token.newToken(support.getPosition());
                                    return true;

                                case '\'' :
                                case '`' :
                                    list.add(new BackRefNode(token.getPosition(), c));
                                    token.newToken(support.getPosition());
                                    return true;
                            }
                            if (!isIdentifierChar(c)) {
                                errorHandler.handleError(IErrors.SYNTAX_ERROR, token.getPosition(), Messages.getString("bad_global_variable")); //$NON-NLS-1$
                                token.newToken(support.getPosition());
                                return true;
                            }
                    }

                    for (; isIdentifierChar(c); c = support.read()) {
                        token.append(c);
                    }
                    support.unread();
                    list.add(new GlobalVarNode(token.getPosition(), token.getToken()));
                    token.newToken(support.getPosition());
                    return true;
                case '@' :
                    token.append(c);
                    c = support.read();
                    if (c == '@') {
                        token.append(c);
                        c = support.read();
                    }
                    for (; isIdentifierChar(c); c = support.read()) {
                        token.append(c);
                    }
                    support.unread();
                    if (token.getToken().startsWith("@@")) {
                        list.add(new ClassVarNode(token.getPosition(), token.getToken()));
                    } else {
                        list.add(new InstVarNode(token.getPosition(), token.getToken()));
                    }
                    token.newToken(support.getPosition());
                    return true;
                case '{' :
                    if (c == '{') {
                        brace = '}';
                    }
                    nest = 0;
                    do {
                        loop_again : while (true) {
                            c = support.read();
                            switch (c) {
                                case '\0' :
                                    if (nest > 0) {
                                        errorHandler.handleError(IErrors.SYNTAX_ERROR, token.getPosition(), Messages.getString("bad_substitution")); //$NON-NLS-1$
                                        token.newToken(support.getPosition());
                                        return true;
                                    }
                                    throw new EOFException();
                                case '}' :
                                    if (c == brace) {
                                        if (nest == 0) {
                                            break;
                                        }
                                        nest--;
                                    }
                                    token.append(c);
                                    continue loop_again;
                                case '\\' :
                                    c = support.read();
                                    if (c == '\0') {
                                        throw new EOFException();
                                    } else if (c == closeQuote) {
                                        token.append(c);
                                    } else {
                                        token.append('\\');
                                        token.append(c);
                                    }
                                    continue loop_again;
                                case '{' :
                                    if (brace != '\0') {
                                        nest++;
                                    }
                                case '\"' :
                                case '/' :
                                case '`' :
                                    if (c == closeQuote) {
                                        support.unread();
                                        list.add(new StrNode(token.getPosition(), "#")); //$NON-NLS-1$
                                        errorHandler.handleError(IErrors.WARNING, support.getPosition(), Messages.getString("bad_substitution")); //$NON-NLS-1$
                                        list.add(new StrNode(token.getPosition(), token.getToken()));
                                        token.newToken(support.getPosition());
                                        return true;
                                    }
                                default :
                                    token.append(c);
                                    break;
                            }
                            break loop_again;
                        }
                    } while (c != brace);
            }
            break;
        }

        // FIX
        list.add(new EvStrNode(token.getPosition(), token.getToken()));
        token.newToken(support.getPosition());
        return true;
    }

    private final boolean isArgState() {
        return lexState.isExprArg();
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
        this.lexState = (LexState) state;
    }

    public StackState getCmdArgumentState() {
        return cmdArgumentState;
    }

    public StackState getConditionState() {
        return conditionState;
    }
}