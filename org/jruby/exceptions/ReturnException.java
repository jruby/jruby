/*
 * ReturnException.java - No description
 * Created on 13.01.2002, 22:24:10
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
package org.jruby.exceptions;

import org.jruby.*;

/** The ReturnException is thrown if a 'return' statement is interpreted.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class ReturnException extends JumpException {
    private RubyObject returnValue;

	/** Creates a new ReturnException with a return value.
	 * 
	 * @param returnValue The return value.
	 */
    public ReturnException(RubyObject returnValue) {
        this.returnValue = returnValue;
    }
    
    /** Returns the return value.
     * 
     * @return Value of property returnValue.
     */
    public RubyObject getReturnValue() {
        return returnValue;
    }
}
