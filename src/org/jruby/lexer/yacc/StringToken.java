/*
 * StringToken.java - description
 * Created on 07.03.2002, 00:29:12
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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
package org.jruby.lexer.yacc;

import org.ablaf.common.ISourcePosition;
import org.ablaf.common.IErrorHandler;
import org.ablaf.lexer.ILexerSupport;
import org.jruby.common.IErrors;

import java.io.EOFException;

/** Represents a token which is currently lexed.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class StringToken {
    private ILexerSupport support;
    private IErrorHandler errorHandler;
    private ISourcePosition position;

    private StringBuffer buffer = new StringBuffer(50);

    /**
     * Constructor for StringToken.
     */
    public StringToken(ILexerSupport support, IErrorHandler errorHandler, ISourcePosition position) {
        this.support = support;
        this.errorHandler = errorHandler;
        this.position = position;
    }

    public void append(char c) {
        buffer.append(c);
    }

    public void append(String s) {
        buffer.append(s);
    }
    
    public String getToken() {
        return buffer.toString();
    }
    
    public int getLength() {
        return buffer.length();
    }
    
    public void newToken(ISourcePosition position) {
        buffer = new StringBuffer(50);

        this.position = position;
    }
    
    public void appendEscape(char closeQuote) throws EOFException {
        char c = support.read();

        switch (c) {
            case '\n' : // just ignore.
                break; 
            case '0' : case '1' : case '2' : case '3' : // octal constant
            case '4' : case '5' : case '6' : case '7' :
                append('\\');
                append(c);
                append(support.readOct(2)); // octal constants are 3 digits
                break;
            case 'x' : // hex constant
                append('\\');
                append(c);
                append(support.readHex(2)); // hex constants are 2 digits
                break;
            case 'M' :
                if ((c = support.read()) != '-') {
                    errorHandler.handleError(IErrors.SYNTAX_ERROR, support.getPosition(), "Invalid escape character syntax. '-' expected, '" + c + "' found.");
                    support.unread();
                } else {
                    append("\\M-");
                    appendPossibleEscapedChar(closeQuote);
                }
                break;
            case 'C' :
                if ((c = support.read()) != '-') {
                    errorHandler.handleError(IErrors.SYNTAX_ERROR, support.getPosition(), "Invalid escape character syntax. '-' expected, '" + c + "' found.");
                    support.unread();
                } else {
                    append("\\C-");
                    appendPossibleEscapedChar(closeQuote);
                }
                break;
            case 'c' :
                append("\\c");
                appendPossibleEscapedChar(closeQuote);
                break;
            case '\0' :
                errorHandler.handleError(IErrors.SYNTAX_ERROR, support.getPosition(), "Invalid escape character syntax. Unexpected end of file.");
                
                throw new EOFException();
            default :
                if (c != closeQuote) {
                    append('\\');
                }
                append(c);
        }
    }
    
    /** <b>FIXME method name</b>
     * @fixme method name 
     */
    private final void appendPossibleEscapedChar(char closeQuote) throws EOFException {
        char c = support.read();
        
        switch (c) {
            case '\\' :
                appendEscape(closeQuote);
                break;
            case '\0' :
                errorHandler.handleError(IErrors.SYNTAX_ERROR, support.getPosition(), "Invalid escape character syntax. Unexpected end of file.");
                
                throw new EOFException();
            default:
                append(c);
        }
    }

    /**
     * Gets the errorHandler.
     * @return Returns a IErrorHandler
     */
    public IErrorHandler getErrorHandler() {
        return errorHandler;
    }

    /**
     * Sets the errorHandler.
     * @param errorHandler The errorHandler to set
     */
    public void setErrorHandler(IErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Gets the support.
     * @return Returns a ILexerSupport
     */
    public ILexerSupport getSupport() {
        return support;
    }

    /**
     * Sets the support.
     * @param support The support to set
     */
    public void setSupport(ILexerSupport support) {
        this.support = support;
    }

    /**
     * Gets the position.
     * @return Returns a ISourcePosition
     */
    public ISourcePosition getPosition() {
        return position;
    }

    /**
     * Sets the position.
     * @param position The position to set
     */
    public void setPosition(ISourcePosition position) {
        this.position = position;
    }
}
