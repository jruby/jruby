/*
 * StringScannerSource.java - No description
 * Created on 01.02.2002, 13:17:56
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

public class StringScannerSource extends AbstractScannerSource {
    private String sourceName;
    private String sourceContent;
    private int sourcePosition = 0;
    private int sourceLen;

	public StringScannerSource(String sourceName, String sourceContent) {
	    this.sourceName = sourceName;
	    this.sourceContent = sourceContent;
	    this.sourceLen = sourceContent.length();
	}

    /**
     * @see AbstractScannerSource#internalRead()
     */
    protected char internalRead() {
        return sourcePosition < sourceLen ? sourceContent.charAt(sourcePosition++) : (char)-1;
    }

    /**
     * @see IScannerSource#getSource()
     */
    public String getSource() {
        return sourceName;
    }
}