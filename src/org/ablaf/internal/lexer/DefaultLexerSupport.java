/*
 * DefaultLexerSupport.java
 * Created on 05.02.2002, 23:50:45
 *
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jan Arne Petersen (jpetersen@uni-bonn.de)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "AbLaF" and "Abstract Language Framework" must not be
 *    used to endorse or promote products derived from this software
 *    without prior written permission. For written permission, please
 *    contact jpetersen@uni-bonn.de.
 *
 * 5. Products derived from this software may not be called
 *    "Abstract Language Framework", nor may
 *    "Abstract Language Framework" appear in their name, without prior
 *    written permission of Jan Arne Petersen.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JAN ARNE PETERSEN OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * ====================================================================
 *
 */
package org.ablaf.internal.lexer;

import org.ablaf.common.ISourcePosition;
import org.ablaf.lexer.ILexerSource;
import org.ablaf.lexer.ILexerSupport;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class DefaultLexerSupport implements ILexerSupport {
    protected ILexerSource source;
    protected int[] lineOffset = new int[100];

    public DefaultLexerSupport(ILexerSource source) {
        this.source = source;
    }

    /**
     * @see ILexerSupport#read()
     */
    public char read() {
        char c = source.read();
        if (c == '\n') {
            if (source.getLine() >= lineOffset.length) {
                int[] _lineOffset = new int[lineOffset.length + 200];
                System.arraycopy(lineOffset, 0, _lineOffset, 0, lineOffset.length);
                lineOffset = _lineOffset;
            }
            lineOffset[source.getLine()] = source.getOffset();
        }
        return c;
    }

    /**
     * @see ILexerSupport#unread()
     */
    public void unread() {
        source.unread();
    }

    /**
     * @see ILexerSupport#getNext()
     */
    public char getNext() {
        char c = read();
        unread();
        return c;
    }

    /**
     * @see ILexerSupport#isEOF()
     */
    public boolean isEOF() {
        return getNext() == '\0';
    }

    /**
     * @see ILexerSupport#isEOL()
     */
    public boolean isEOL() {
        return getNext() == '\n';
    }

    /**
     * @see ILexerSupport#isNext(char)
     */
    public boolean isNext(char c) {
        return getNext() == c;
    }

    /**
     * @see ILexerSupport#readHex(int)
	 * @fixme should throw exception when finding the null char
     */
    public String readHex(int maxLen) {
        StringBuffer buffer = new StringBuffer(maxLen);
        for (int i = 0; i < maxLen; i++) {
            char c = read();
            if (isHex(c)) {
                buffer.append(c);
            } else if (c == '\0') {
                return null; // FIXME throw Exception
            } else {
                unread();
            }
        }
        return buffer.toString();
    }

    /**
	 * @fixme should throw exception when finding the null char
     * @see ILexerSupport#readOct(int)
     */
    public String readOct(int maxLen) {
        StringBuffer buffer = new StringBuffer(maxLen);
        for (int i = 0; i < maxLen; i++) {
            char c = read();
            if (isOct(c)) {
                buffer.append(c);
            } else if (c == '\0') {
                return null; // FIXME throw Exception
            } else {
                unread();
            }
        }
        return buffer.toString();
    }

    /**
     * @see ILexerSupport#getCharAt(int)
     */
    public char getCharAt(int idx) {
        char c = '\0';
        for (int i = 0; i < idx; i++) {
            c = read();
        }
        for (int i = 0; i < idx; i++) {
            unread();
        }
        return c;
    }

    /**
     * @see ILexerSupport#getPosition()
     */
    public ISourcePosition getPosition() {
        int col = source.getOffset();
        if (source.getLine() > 0) {
            col -= lineOffset[source.getLine() - 1];
        }
        return DefaultLexerPosition.getInstance(source.getSourceName(), source.getLine() + 1, col);
    }

    /**
	 * Check if the string s is the next string in the lexer.
	 * If this is the case the String is consumed.
     * @see ILexerSupport#isNext(String)
     */
    public boolean isNext(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != read()) {
                for (; i >= 0; i--) {
                    unread();
                }
                return false;
            }
        }
        for (int i = s.length(); i > 0; i--) {
            unread();
        }
        return true;
    }

    /**
     * @see ILexerSupport#readLine()
     */
    public String readLine() {
        StringBuffer sb = new StringBuffer(80);
        for (char c = read(); c != '\n' && c != '\0'; c = read()) {
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * @see ILexerSupport#getLastRead()
     */
    public char getLastRead() {
        return source.getLastRead();
    }

    /**
     * @see ILexerSupport#unreadMany(int)
     */
    public void unreadMany(int n) {
        for (int i = 0; i < n; i++) {
            unread();
        }
    }

	/** Returns 'true' if c is a hexadecimal number.
	 *
	 */
    private static final boolean isHex(char c) {
        return Character.isDigit(c) || ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F');
    }

	/** Returns 'true' if c is an octal number.
	 *
	 */
    private static final boolean isOct(char c) {
        return ('0' <= c && c <= '7');
    }
}
