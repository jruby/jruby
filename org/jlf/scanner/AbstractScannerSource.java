/*
 * AbstractScannerSource.java - No description
 * Created on 01.02.2002, 00:52:26
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
package org.jlf.scanner;

import org.jruby.scanner.*;

/** An abstract implementation of a ScannerSource.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public abstract class AbstractScannerSource implements IScannerSource {
    protected int line = 1;
    protected int col = 0;

    protected abstract char internalRead();

    protected StringBuffer buffer = new StringBuffer();
    protected StringBuffer readBuffer = new StringBuffer();

    private boolean nl = false;

    /**
     * @see IScannerSource#read()
     */
    public char read() {
        char c = (char) - 1;

        if (nl) {
            line++;
            col = 0;
            nl = false;
        }

        synchronized (buffer) {
            readIntoBuffer(1);

            switch (buffer.charAt(0)) {
                case '\n' :
                    {
                        readIntoBuffer(2);
                        c = '\n';
                        buffer.deleteCharAt(0);

                        if (buffer.charAt(0) == '\r') {
                            buffer.deleteCharAt(0);
                        }

                        nl = true;
                        break;
                    }
                case '\r' :
                    {
                        readIntoBuffer(2);
                        c = '\n';
                        buffer.deleteCharAt(0);

                        if (buffer.charAt(0) == '\n') {
                            buffer.deleteCharAt(0);
                        }

                        nl = true;
                        break;
                    }
                default :
                    {
                        c = buffer.charAt(0);
                        buffer.deleteCharAt(0);
                        break;
                    }
            }

            col++;
            readBuffer.append(c);
        }
        
        return c;
    }

    protected void readIntoBuffer(int len) {
        for (int i = buffer.length(); i < len; i++) {
            buffer.append(internalRead());
        }
    }

    /**
     * @see IScannerSource#unread()
     */
    public void unread() {
        synchronized (buffer) {
            buffer.insert(0, readBuffer.charAt(readBuffer.length() - 1));
            readBuffer.deleteCharAt(readBuffer.length() - 1);

            col--;
            nl = false;
            if (col == 0) {
                line--;
                // get length of last line.
                readBuffer.deleteCharAt(readBuffer.length() - 1);
                col = readBuffer.lastIndexOf("\n");
                col = col != -1 ? readBuffer.length() - col : readBuffer.length() + 1;
                readBuffer.append('\n');
                nl = true;
            }
        }
    }
    
    public void unread(int n) {
        // +++ not performant
        for (int i = 0; i < n; i++) {
            unread();
        }
        // ---
        /*synchronized (buffer) {
            buffer.insert(0, readBuffer.substring(readBuffer.length() - n, readBuffer.length()));
            readBuffer.delete(readBuffer.length() - n, readBuffer.length());

            col--;
            nl = false;
            if (col == 0) {
                line--;
                // get length of last line.
                readBuffer.deleteCharAt(readBuffer.length() - 1);
                col = readBuffer.lastIndexOf("\n");
                col = col != -1 ? readBuffer.length() - col : readBuffer.length() + 1;
                readBuffer.append('\n');
                nl = true;
            }
        }*/
    }

    /**
     * @see IScannerSource#isNext(char)
     */
    public boolean isNext(char c) {
        synchronized (buffer) {
            readIntoBuffer(1);

            return buffer.charAt(0) == c;
        }
    }

    /**
     * @see IScannerSource#isEOL()
     */
    public boolean isEOL() {
        return nl;
    }

    /**
     * @see IScannerSource#getLine()
     */
    public int getLine() {
        return line;
    }

    /**
     * @see IScannerSource#getColumn()
     */
    public int getColumn() {
        return col;
    }
}