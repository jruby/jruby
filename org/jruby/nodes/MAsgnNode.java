/*
 * MAsgnNode.java - No description
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
import org.jruby.runtime.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class MAsgnNode extends Node implements AssignableNode {
    public MAsgnNode(Node headNode, Node argsNode) {
        super(Constants.NODE_MASGN, headNode, null, argsNode);
    }

    public RubyObject eval(Ruby ruby, RubyObject self) {
        return massign(ruby, self, getValueNode().eval(ruby, self), false);
    }

    public RubyObject massign(Ruby ruby, RubyObject self, RubyObject val, boolean check) {
        if (val == null) {
            val = RubyArray.newArray(ruby);
        } else if (!(val instanceof RubyArray)) {
            // if ( rb_respond_to( val, to_ary ) ) {
            // val.funcall(getRuby().intern("to_a"));
            // } else {
            val = RubyArray.newArray(ruby, val);
            // }
        }
        
        Node list = getHeadNode();
        int i = 0;
        for (; list != null && i < ((RubyArray)val).getLength(); i++) {
            ((AssignableNode)list.getHeadNode()).assign(ruby, self, ((RubyArray)val).entry(i), check);
            list = list.getNextNode();
        }
        
        if (check && list != null) {
            // error
        }
        
        if (getArgsNode() != null) {
            if (getArgsNode() == MINUS_ONE) {
            } else if (list == null && i < ((RubyArray)val).getLength()) {
                ((AssignableNode)getArgsNode()).assign(ruby, self, ((RubyArray)val).subseq(((RubyArray)val).getLength() - i, i), check);
            } else {
                ((AssignableNode)getArgsNode()).assign(ruby, self, RubyArray.newArray(ruby), check);
            }
        } else if (check && i < ((RubyArray)val).getLength()) {
            // error
        }
        
        while (list != null) {
            i++;
            ((AssignableNode)list.getHeadNode()).assign(ruby, self, ruby.getNil(), check);
            list = list.getNextNode();
        }
        return val;
    }
    
    public void assign(Ruby ruby, RubyObject self, RubyObject value, boolean check) {
        massign(ruby, self, value, check);
    }
}
