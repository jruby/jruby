/*
 * AbstractLexerSource.java
 * Created on 05.02.2002, 23:50:08
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

import org.ablaf.lexer.ILexerSource;

/** An abstract implementation of an ILexerSource.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public abstract class AbstractLexerSource implements ILexerSource {
    private StringBuffer buffer = new StringBuffer(200);
    private int offset = 0;
    private String sourceName;
    private char lastRead = '\0';
    private int line = 0;

    protected AbstractLexerSource(String sourceName) {
        this.sourceName = sourceName;
    }

    protected abstract char internalRead();

    /**
     * @see ILexerSource#read()
     */
    public char read() {
        synchronized (buffer) {
            if (lastRead == '\n') {
                // The \n has already been read, but we increment on first char
                // of new line
                line++;
            }
            readIntoBuffer();
            lastRead = buffer.charAt(offset++);
            return lastRead;
        }
    }

	/** Fill the buffer if there are no more chars.
	 *
	 */
    private void readIntoBuffer() {
        if (buffer.length() - offset < 1) {
            buffer.append(internalRead());
        }
    }

    /**
     * @see ILexerSource#unread()
     */
    public void unread() {
        synchronized (buffer) {
            char result;
            if (offset > 0) {
            	result = buffer.charAt(--offset);
            } else {
                result = 0;
            }
            lastRead = result;
            if (offset > 0 && buffer.charAt(offset-1)=='\n')
            {
                // We have just unread the first char of a new line, so
                // now we decrement the line number
                line--;
            }
            return;
        }
    }

    /**
     * @see ILexerSource#getOffset()
     */
    public int getOffset() {
    	return offset;
    }

    /**
     * @see ILexerSource#getSourceName()
     */
    public String getSourceName() {
        return sourceName;
    }

    public char getLastRead() {
        return lastRead;
    }

    public int getLine() {
        return line;
    }
}
