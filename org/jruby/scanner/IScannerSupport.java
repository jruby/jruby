/*
 * IScannerSupport.java - No description
 * Created on 01.02.2002, 14:28:14
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
public interface IScannerSupport {
	public String readHex(int max);
	public int readHexAsInt(int max);
	
	public String readOct(int max);
	
	/** Read upto the end of the line. Don't return the eol character.
	 * 
	 */
	public String readLine();
	
	public boolean isNext(String s);
	public char getCharAt(int idx);
}