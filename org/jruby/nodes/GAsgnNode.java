/*
 * GAsgnNode.java - No description
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
import org.jruby.nodes.types.*;
import org.jruby.nodes.visitor.*;
import org.jruby.runtime.*;

/**
 *  Global Assignment node.
 *  Assignment to a global variable.
 * the meaning of the Node fields for BlockNodes is:
 * <ul>
 * <li>
 * u1 ==&gt; unused
 * </li>
 * <li>
 * u2 ==&gt; value (any Node subtype)
 * </li>
 * <li>
 * u3 ==&gt; entry (RubyGlobalEntry)
 * </li>
 * </ul>
 * @author  jpetersen
 * @version $Revision$
 */
public class GAsgnNode extends Node implements AssignableNode {
    public GAsgnNode(Node valueNode, RubyGlobalEntry entry) {
        super(Constants.NODE_GASGN, null, valueNode, entry);
    }
    
    public void assign(Ruby ruby, RubyObject self, RubyObject value, boolean check) {
        getEntry().set(value);
    }
    
    public RubyObject eval(Ruby ruby, RubyObject self) {
        RubyObject result = getValueNode().eval(ruby, self);
        getEntry().set(result);
        return result;
    }
	
	/**
	 * Method used by visitors.
	 * accepts the visitor 
	 * @param iVisitor the visitor to accept
	 **/
	public void accept(NodeVisitor iVisitor)
	{
		iVisitor.visitGAsgnNode(this);
	}
}
