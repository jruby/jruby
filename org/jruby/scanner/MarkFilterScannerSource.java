/*
 * MarkFilterScannerSource.java - No description
 * Created on 02.02.2002, 14:17:37
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
public class MarkFilterScannerSource extends FilterScannerSource {
    private boolean marked = false;
    private int markedRead = 0;

    /**
     * Constructor for MarkFilterScannerSource.
     * @param source
     */
    public MarkFilterScannerSource(IScannerSource source) {
        super(source);
    }

	public void mark() {
	    marked = true;
	    markedRead = 0;
	}
	
	public void reset() {
	    if (marked) {
	        if (markedRead > 0) {
	            source.unread(markedRead);
	        } else if (markedRead < 0) {
	            for (int i = 0; i < -markedRead; i++) {
	                source.read();
	            }
	        }
			marked = false;
	    }
	}
	
	public char read() {
	    markedRead++;
	    return super.read();
	}

	public void unread() {
	    markedRead--;
	    super.unread();
	}

	public void unread(int n) {
	    markedRead -= n;
	    super.unread(n);
	}
}