/*
 * BlockStack
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Copyright (C) 2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Thomas E Enebo <enebo@acm.org>
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
package org.jruby.runtime;

import org.jruby.ast.Node;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.AbstractStack;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class BlockStack extends AbstractStack {
    public void push(Node varNode, ICallable method, IRubyObject self) {
        push(Block.createBlock(varNode, method, self));
    }

    public Block getCurrent() {
        return (Block) getTop();
    }

    public void setCurrent(Block block) {
        top = block;
    }
}