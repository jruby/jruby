/*
 * FilterScannerSource.java - No description
 * Created on 02.02.2002, 14:11:32
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
package org.jruby.scanner;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public abstract class FilterScannerSource implements IScannerSource {
    protected IScannerSource source;
    
    public FilterScannerSource(IScannerSource source) {
        this.source = source;
    }

    /**
     * @see IScannerSource#read()
     */
    public char read() {
        return source.read();
    }

    /**
     * @see IScannerSource#unread()
     */
    public void unread() {
        source.unread();
    }

    /**
     * @see IScannerSource#unread(int)
     */
    public void unread(int n) {
        source.unread(n);
    }

    /**
     * @see IScannerSource#isNext(char)
     */
    public boolean isNext(char c) {
        return source.isNext(c);
    }

    /**
     * @see IScannerSource#isEOL()
     */
    public boolean isEOL() {
        return source.isEOL();
    }

    /**
     * @see IScannerSource#getLine()
     */
    public int getLine() {
        return source.getLine();
    }

    /**
     * @see IScannerSource#getColumn()
     */
    public int getColumn() {
        return source.getColumn();
    }

    /**
     * @see IScannerSource#getSource()
     */
    public String getSource() {
        return source.getSource();
    }
}