/*
 * DefaultScannerSupport.java - No description
 * Created on 01.02.2002, 14:32:03
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

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class DefaultScannerSupport implements IScannerSupport {
    private IScannerSource source;
    
    public DefaultScannerSupport(IScannerSource source) {
        this.source = source;
    }

    /**
     * @see IScannerSupport#readHex(int)
     */
    public String readHex(int max) {
        StringBuffer sb = new StringBuffer(max);

        while (max > 0) {
            char c = source.read();
            if (isHex(c)) {
                sb.append(c);
            } else {
                source.unread();
                break;
            }
        }

        return sb.toString();
    }

    /**
     * @see IScannerSupport#readHexAsInt(int)
     */
    public int readHexAsInt(int max) {
        return Integer.parseInt(readHex(max), 16);
    }

    /**
     * @see IScannerSupport#readOct(int)
     */
    public String readOct(int max) {
        return null;
    }

    /**
     * @see IScannerSupport#readLine()
     */
    public String readLine() {
        StringBuffer sb = new StringBuffer();

        char c = source.read();

        if (c == -1) {
            return null;
        }

        while (c != '\n' && c != -1) {
            sb.append(c);
            c = source.read();
        }

        return sb.toString();
    }

    /**
     * @see IScannerSupport#getCharAt(int)
     */
    public char getCharAt(int idx) {
        for (int i = 0; i < idx; i++) {
            source.read();
        }
        char c = source.read();

        source.unread(idx + 1);

        return c;
    }

    /**
     * @see IScannerSupport#isNext(String)
     */
    public boolean isNext(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != source.read()) {
                source.unread(i + 1);
                return false;
            }
        }
        source.unread(s.length());

        return true;
    }

    private static final boolean isHex(char c) {
        return ('0' <= c && c <= '9') || ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F');
    }
}