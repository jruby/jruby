/*
 * IObjectCreator.java - description
 * Created on 12.03.2002, 13:24:37
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
package org.jruby.runtime.classes;

/** This interface defines methods to create new objects.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public interface IObjectCreator {
    /** Returns the singleton nil object.
     * 
     * This method must always return the same object.
     * 
     */
    public INilClass getNil();

	/** Returns the singleton true object.
	 * 
	 * This method must always return the same object.
     * 
     */
    public ITrueClass getTrue();

	/** Returns the singleton false object.
     * 
     * This method must always return the same object.
     * 
     */
    public IFalseClass getFalse();

    
    
    /** Returns a Fixnum object for a given long number.
     * 
     * This method must return always the same object if it is
     * called with the same long.
     * 
     */
    public IFixnum getFixnum(long number);
}