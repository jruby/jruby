/*
 * IRegexpAdapter.java - No description
 * Created on 10. Oct 2001, 00:01
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */
package org.jruby;
import org.jruby.exceptions.RubyRegexpException;
/**
 * Regexp adapter interface.
 * This interface is used to decouple ruby from the actual regexp engine
 */
interface IRegexpAdapter
{

	/**
	 * Compile the regex.
	 */
	public void compile(String pattern) throws org.jruby.exceptions.RubyRegexpException;

	/**
	 * Set whether matches should be case-insensitive or not
	 */
	public void setCasefold(boolean set) ;

	/**
	 * Get whether matches are case-insensitive or not
	 */
	public boolean getCasefold() ;

	/**
	 * Set whether patterns can contain comments and extra whitespace
	 */
	public void setExtended(boolean set) ;

	/**
	 * Set whether the dot metacharacter should match newlines
	 */
	public void setMultiline(boolean set) ;

	/**
	 * Does the given argument match the pattern?
	 */
	public RubyObject search(Ruby ruby, String target, int startPos) ;
}

