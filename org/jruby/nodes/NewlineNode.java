/*
 * NewlineNode.java - No description
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
import org.jruby.nodes.visitor.*;
import org.jruby.runtime.*;

/**
 * A new (logical) source code line.
 * This is used to change the value of the ruby interpreter        
 * source and line values.
 * There is one such node for each logical line.  Logical line differs
 * from physical line in that a ';' can be used to make several logical
 * line out of a physical line and a physical line if it is in a comment
 * or in a string does not necessarily correspond to a physical line.
 * This is normally a wrapper around another more significant node.
 * The parser generates such a node around each separate statement.  
 *
 * the meaning of the Node fields for BlockNodes is:
 * <ul>
 * <li>
 * u1 ==&gt; unused
 * </li>
 * <li>
 * u2 ==&gt; nth (line number) unused, this is redundant with the line
 * field in the Node class
 * </li>
 * <li>
 * u3 ==&gt; next the wrapped node
 * </li>
 * </ul>
 * @author  jpetersen
 * @version $Revision$
 */
public class NewlineNode extends Node{
    public NewlineNode(Node nextNode) {
        super(Constants.NODE_NEWLINE, null, null, nextNode);
    }
    
    public RubyObject eval(Ruby ruby, RubyObject self) {
        ruby.setSourceFile(getFile());
        ruby.setSourceLine(getLine());
        // if (trace_func) {
        //     call_trace_func("line", ruby_sourcefile, ruby_sourceline, self,
        //     ruby_frame.last_func(),
        //     ruby_frame.last_class());
        // }
        return getNextNode().eval(ruby, self);
    }
	
	/**
	 * Method used by visitors.
	 * accepts the visitor
	 * @param iVisitor the visitor to accept
	 **/
	public void accept(NodeVisitor iVisitor)
	{
		iVisitor.visitNewlineNode(this);
	}

}
