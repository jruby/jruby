/*
 * AttrSetNode.java - No description
 * Created on 01. November 2001, 16:27
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
import org.jruby.exceptions.*;
import org.jruby.nodes.types.*;
import org.jruby.runtime.*;
import org.jruby.util.*;

/**
 *
 * @author  jpetersen
 * @version
 */
public class AttrSetNode extends Node implements CallableNode {
    private RubyId vid;
    
    public AttrSetNode(RubyId vId) {
        super(Constants.NODE_ATTRSET, vId, null, null);
    }
    
    public RubyObject eval(Ruby ruby, RubyObject self) {
        if (ruby.getRubyFrame().getArgs().size() != 1) {
            throw new RubyArgumentException(ruby, "wrong # of arguments(" + ruby.getRubyFrame().getArgs().size() + "for 1)");
        }
        return self.setInstanceVar(getVId(), (RubyObject)ruby.getRubyFrame().getArgs().get(0));
    }
    
    public RubyObject call(Ruby ruby, RubyObject recv, RubyId id, RubyPointer args, boolean noSuper) {
        return eval(ruby, recv);
    }
}