/*
 * IScannerSource.java - No description
 * Created on 01.02.2002, 00:19:34
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

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public interface IScannerSource {
    /** Returns the next character.
     * 
     * @return char next character.
     */
	public char read();
	
	/** Unread the last read character.
     * 
     */
	public void unread();

	/** Unread the last 'n' read character.
     * 
     */
	public void unread(int n);

	public boolean isNext(char c);
	public boolean isEOL();

	public int getLine();

    /** Returns the column of the last read character.
     * 
     * @return int the column.
     */
	public int getColumn();

	public String getSource();
}