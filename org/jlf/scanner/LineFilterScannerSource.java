/*
 * LineFilterScannerSource.java - No description
 * Created on 02.02.2002, 14:28:15
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

import java.util.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class LineFilterScannerSource extends FilterScannerSource {
    private List filteredLines = new ArrayList();

    /**
     * Constructor for LineFilterScannerSource.
     * @param source
     */
    public LineFilterScannerSource(IScannerSource source) {
        super(source);
    }
    
    public void addCurrentLine() {
    	filteredLines.add(new Integer(super.getLine()));
    }

    public void addLine(int line) {
        filteredLines.add(new Integer(line));
    }

    public void removeLine(int line) {
        filteredLines.remove(new Integer(line));
    }
    
    public char read() {
        char c = super.read();

        while (c != -1 && filteredLines.contains(new Integer(getLine()))) {
            c = super.read();
        }

        return c;
    }

    public void unread() {
        super.unread();

        while (filteredLines.contains(new Integer(getLine()))) {
            super.unread();
        }
    }

    public void unread(int n) {
        for (int i = 0; i < n; i++) {
            unread();
        }
    }

    public boolean isNext(char c) {
        boolean result = c == read();
        unread();
        return result;
    }
}