/*
 * ZSuperNode.java - No description
 * Created on 05. November 2001, 21:46
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

import java.util.*;

import org.jruby.*;
import org.jruby.nodes.types.*;
import org.jruby.nodes.visitor.*;
import org.jruby.exceptions.*;
import org.jruby.runtime.*;
import org.jruby.util.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class ZSuperNode extends Node implements CallableNode {
    public ZSuperNode() {
        super(Constants.NODE_ZSUPER);
    }
    
    public RubyObject eval(Ruby ruby, RubyObject self) {
        // TMP_PROTECT;
        
        if (ruby.getRubyFrame().getLastClass() == null) {
            throw new NameError(ruby, "superclass method '" + 
                    ruby.getRubyFrame().getLastFunc() + "' disabled");
        }
        
        RubyPointer args = (RubyPointer)ruby.getRubyFrame().getArgs();
        // RubyObject[] argsObj = (RubyObject[])argsList.toArray(new RubyObject[argsList.size()]);
        
        ruby.getIter().push(ruby.getIter().getIter() != RubyIter.ITER_NOT ? 
                                         RubyIter.ITER_PRE : RubyIter.ITER_NOT);
        RubyObject result = ruby.getRubyFrame().getLastClass().getSuperClass().call(
                                ruby.getRubyFrame().getSelf(), 
                                ruby.getRubyFrame().getLastFunc(), args, 3);
        ruby.getIter().pop();
        
        return result;
    }
    
    public RubyObject call(Ruby ruby, RubyObject recv, String id, RubyPointer args, boolean noSuper) {
        return eval(ruby, recv);
    }
	/**
	 * Accept for the visitor pattern.
	 * @param iVisitor the visitor
	 **/
	public void accept(NodeVisitor iVisitor)	
	{
		iVisitor.visitZSuperNode(this);
	}
}
