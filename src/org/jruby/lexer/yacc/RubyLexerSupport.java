/*
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
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

import org.ablaf.common.ISourcePosition;
import org.ablaf.internal.lexer.DefaultLexerPosition;
import org.ablaf.internal.lexer.DefaultLexerSupport;
import org.ablaf.lexer.ILexerSource;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyLexerSupport extends DefaultLexerSupport implements IRubyLexerSupport {
    private String buffer = null;
    private int bufferPos = -1;
    private ISourcePosition startPosition = null;

    /**
     * Constructor for RubyLexerSupport.
     * @param source
     */
    public RubyLexerSupport(ILexerSource source) {
        super(source);
    }

    /**
     * @see IRubyLexerSupport#setBuffer(String, ISourcePosition)
     */
    public void setBuffer(String buffer, ISourcePosition startPosition) {
        this.buffer = buffer;
        this.bufferPos = 0;
        this.startPosition = startPosition;
    }

    /**
     * @see DefaultLexerSupport#getPosition()
     */
    public ISourcePosition getPosition() {
        if (bufferPos <= 0) {
            return DefaultLexerPosition.getInstance(source.getSourceName(), source.getLine() + 1);
        } else {
            return DefaultLexerPosition.getInstance(startPosition.getFile(), startPosition.getLine(), startPosition.getColumn() + bufferPos);
        }
    }

    /**
     * @see DefaultLexerSupport#read()
     */
    public char read() {
        if (bufferPos == -1) {
            return super.read();
        } else if (bufferPos >= buffer.length()) {
            bufferPos = -1;
            return super.read();
        } else {
            return buffer.charAt(bufferPos++);
        }
    }

    /**
     * @see DefaultLexerSupport#unread()
     */
    public void unread() {
        if (bufferPos == -1) {
            super.unread();
        } else if (bufferPos > 0) {
            buffer.charAt(--bufferPos);
        }
    }

    public char readEscape() throws LexerException {
        char c;

        switch (c = read()) {
            case '\\' : // Backslash
                return '\\';
            case 'n' : // newline
                return '\n';
            case 't' : // horizontal tab
                return '\t';
            case 'r' : // carriage-return
                return '\r';
            case 'f' : // form-feed
                return '\f';
            case 'v' : // vertical tab
                return '\u0013';
            case 'a' : // alarm(bell)
                return '\u0007';
            case 'e' : // escape
                return '\u0033';
            case '0' :
            case '1' :
            case '2' :
            case '3' : // octal constant
            case '4' :
            case '5' :
            case '6' :
            case '7' :
                unread();
                return (char) Integer.parseInt(readOct(3), 8);
            case 'x' : // hex constant
                return (char) Integer.parseInt(readHex(2), 16);
            case 'b' : // backspace
                return '\010';
            case 's' : // space
                return ' ';
            case 'M' :
                if ((c = read()) != '-') {
                    unread();
                    throw new LexerException();
                } else if ((c = read()) == '\\') {
                    return (char) (readEscape() | 0x80);
                } else if (c == '\0') {
                    throw new LexerException();
                } else {
                    return (char) ((c & 0xff) | 0x80);
                }

            case 'C' :
                if ((c = read()) != '-') {
                    unread();
                    throw new LexerException();
                }
            case 'c' :
                if ((c = read()) == '\\') {
                    c = readEscape();
                } else if (c == '?') {
                    return '\u0177';
                } else if (c == '\0') {
                    throw new LexerException();
                }
                return (char) (c & 0x9f);
            case '\0' :
                throw new LexerException();
            default :
                return c;
        }
    }
}