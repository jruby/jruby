/*
 * NextJump.java - description
 * Created on 21.02.2002, 14:51:26
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
package org.jruby.exceptions;

import org.jruby.runtime.builtin.IRubyObject;

/** The NextJump is thrown if a 'next' statement is evaluated.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class NextJump extends JumpException {
    private IRubyObject nextValue;
    
    /** Creates new NextJump */
    public NextJump() {
    }
    
    public NextJump(IRubyObject nextValue) {
        this.nextValue = nextValue;
    }
    
    /** Returns the return value.
     * 
     * @return Value of property returnValue.
     */
    public IRubyObject getNextValue() {
        return nextValue;
    }
}