/*
 * OptNNode.java - No description
 * Created on 05. November 2001, 21:46
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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
import org.jruby.exceptions.*;
import org.jruby.nodes.visitor.*;
import org.jruby.runtime.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class OptNNode extends Node {
    
    public OptNNode(Node bodyNode) {
        super(Constants.NODE_OPT_N, null, bodyNode, null);
    }
    
    public RubyObject eval(Ruby ruby, RubyObject self) {
        while (true) {
            try {
                // while (!rb_gets().isNil() false) {
                // HACK +++
                if (true) {
                    // HACK ---
                    try {
                        getBodyNode().eval(ruby, self);
                    } catch (RedoException rExcptn) {
                    }
                }
                break;
            } catch (NextException nExcptn) {
            } catch (BreakJump bExcptn) {
                break;
            }
        }
        return ruby.getNil();
    }
	/**
	 * Accept for the visitor pattern.
	 * @param iVisitor the visitor
	 **/
	public void accept(NodeVisitor iVisitor)	
	{
		iVisitor.visitOptNNode(this);
	}
}
