/*
 * BufferedScannerSource.java - No description
 * Created on 03.02.2002, 16:27:16
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Chad Fowler
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@yahoo.com>
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
public class LineBufferScannerSource extends FilterScannerSource {
    private StringBuffer buffer = null;
    
    private int line;
    private int column;
    
    private StringBuffer readBuffer = null;
    private int reads = 0;

    /**
     * Constructor for BufferedScannerSource.
     * @param source
     */
    public LineBufferScannerSource(IScannerSource source) {
        super(source);
    }
    
    public void setBufferLine(String buffer, int line, int column) {
        if (buffer.length() > 0) {
        	this.buffer = new StringBuffer(buffer);
        	
        	this.line = line;
        	this.column = column;
        } else {
            this.buffer = null;
        }

        this.readBuffer = null;
        this.reads = 0;
    }

    /**
     * @see IScannerSource#getColumn()
     */
    public int getColumn() {
        return buffer != null ? column : super.getColumn();
    }

    /**
     * @see IScannerSource#getLine()
     */
    public int getLine() {
        return buffer != null ? line : super.getLine();
    }

    /**
     * @see IScannerSource#isEOL()
     */
    public boolean isEOL() {
        return buffer != null ? false : super.isEOL();
    }

    /**
     * @see IScannerSource#isNext(char)
     */
    public boolean isNext(char c) {
        return buffer != null ? buffer.charAt(0) == c : super.isNext(c);
    }

    /**
     * @see IScannerSource#read()
     */
    public char read() {
        if (buffer != null) {
        	char c =  buffer.charAt(0);
        	buffer.deleteCharAt(0);
        	if (buffer.length() == 0) {
        	    buffer = null;
        	}
        	if (readBuffer == null) {
                readBuffer = new StringBuffer();
            }
        	readBuffer.insert(0, c);
        	column++;
        	return c;
        } else {
            if (readBuffer != null) {
                reads ++;
            }
        	return super.read();
        }
    }

    /**
     * @see IScannerSource#unread()
     */
    public void unread() {
        if (readBuffer != null) {
            if (reads > 0) {
                super.unread();
                reads--;
            } else {
                if (buffer == null) {
                    buffer = new StringBuffer();
                }
                buffer.insert(0, readBuffer.charAt(0));
                readBuffer.deleteCharAt(0);
                column--;
                if (readBuffer.length() == 0) {
                    readBuffer = null;
                }
            }
        } else {
            if (buffer == null) {
        		super.unread();
            } else {
                throw new Error("Can't unread. BufferUnderflow.");
            }
        }
    }

    /**
     * @see IScannerSource#unread(int)
     */
    public void unread(int n) {
        for (int i = 0; i < n ; i++) {
            unread();
        }
    }
}