/*
 * OpAsgn2Node.java - No description
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
import org.jruby.util.*;

/**
 *
 * @author  jpetersen
 * @version
 */
public class OpAsgn2Node extends Node {
    private RubyId vid;
    private RubyId mid;
    private RubyId aid;
    
    public OpAsgn2Node(Node recvNode, Node valueNode, RubyId vid, RubyId mid) {
        super(Constants.NODE_OP_ASGN2, recvNode, valueNode, null);

        this.vid = vid;
        this.mid = mid;
        this.aid = vid.toAttrSetId();
    }
    
    public RubyObject eval(Ruby ruby, RubyObject self) {
        RubyObject recv = getRecvNode().eval(ruby, self);
        RubyObject val = recv.funcall(vid, (RubyObject[])null);
        
        switch (mid.intValue()) {
            case 0: /* OR */
                if (val.isTrue()) {
                    return val;
                } else {
                    val = getValueNode().eval(ruby, self);
                }
                break;
            case 1: /* AND */
                if (val.isFalse()) {
                    return val;
                } else {
                    val = getValueNode().eval(ruby, self);
                }
                break;
            default:
                val = val.funcall(getMId(), getValueNode().eval(ruby, self));
        }
        
        // HACK +++
        val = recv.funcall(aid, val); // &val
        // HACK ---
        
        return val;
    }
}