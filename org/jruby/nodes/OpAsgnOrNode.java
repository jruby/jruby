/*
 * OpAsgnOrNode.java - No description
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

import org.jruby.*;
import org.jruby.runtime.*;

/**
 *
 * @author  jpetersen
 * @version
 */
public class OpAsgnOrNode extends Node {
    public OpAsgnOrNode(Node headNode, Node valueNode, RubyId aId) {
        super(Constants.NODE_OP_ASGN_OR, headNode, valueNode, aId);
    }
    
    public RubyObject eval(Ruby ruby, RubyObject self) {
        if ((getAId() != null && !self.isInstanceVarDefined(getAId())) || 
                                    getHeadNode().eval(ruby, self).isFalse()) {

            return getValueNode().eval(ruby, self);
        }
        return ruby.getTrue();
    }
}