/*
 * ThrowJump.java - No description
 * Created on 19.01.2002, 19:29:20
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

import org.jruby.*;

/** Created by the global throw function.
 *
 * @author jpetersen
 * @version $Revision$
 */
public class ThrowJump extends JumpException {
    private String tag;
    private RubyObject value;
    private RubyException nameError;

    /**
     * Constructor for ThrowJump.
     */
    public ThrowJump(String tag, RubyObject value) {
        super();

        this.tag = tag;
        this.value = value;
        this.nameError = new NameError(value.getRuby(), "uncaught throw '" + tag + '\'').getActException();
    }
    
    public String getTag() {
        return tag;
    }
    
    public RubyObject getValue() {
        return value;
    }
    
    public RubyException getNameError() {
        return nameError;
    }
}