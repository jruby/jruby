/*
 * ConstNode.java - No description
 * Created on 05. November 2001, 21:45
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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

package org.jruby.nodes;

import org.jruby.*;
import org.jruby.runtime.*;

/**
 *
 * @author  jpetersen
 * @version
 */
public class ConstNode extends Node {
    public ConstNode(String vId) {
        super(Constants.NODE_CONST, vId, null, null);
    }
    
 	public RubyObject eval(Ruby ruby, RubyObject self) {
        return getConstant(ruby, self, ruby.getRubyFrame().getCbase(), getVId());
    }
    
    protected RubyObject getConstant(Ruby ruby, RubyObject self, CRefNode cref, String id) {
        CRefNode cbase = cref;
        
        // HACK +++
        if (ruby.getClasses().getClassMap().get(id) != null) {
            return (RubyObject)ruby.getClasses().getClassMap().get(id);
        }
        // HACK ---
        
        while (cbase != null && cbase.getNextNode() != null) {
            RubyObject rubyClass = cbase.getClassValue();
            if (rubyClass.isNil()) {
                return self.getRubyClass().getConstant(id);
            } else if (rubyClass.getInstanceVariables().get(id) != null) {
                return (RubyObject)rubyClass.getInstanceVariables().get(id);
            }
            cbase = (CRefNode)cbase.getNextNode();
        }
        return ((RubyModule)cref.getClassValue()).getConstant(id);
    }
	/**
	 * Accept for the visitor pattern.
	 * @param iVisitor the visitor
	 **/
	public void accept(NodeVisitor iVisitor)	
	{
		iVisitor.visitConstNode(this);
	}
}
