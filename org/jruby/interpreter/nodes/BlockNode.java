/*
 * BlockNode.java - No description
 * Created on 10. September 2001, 17:46
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

package org.jruby.interpreter.nodes;

import org.jruby.*;
import org.jruby.original.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class BlockNode extends NODE implements ExpandNode, node_type {

    public BlockNode(NODE arg1) {
        super(NODE_BLOCK, arg1, null, null);
    }
    
    public NODE expand(RubyObject self) {
        /* Future NODE evaluating.
        NODE node = this;
        
        while (node.nd_next() != null) {
            self.eval(node.nd_head());
            node = node.nd_next();
        }
        return node.nd_head();
        */
        return null;
    }
}