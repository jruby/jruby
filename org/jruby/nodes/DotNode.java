/*
 * DotNode.java - No description
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
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class DotNode extends Node {
    private boolean exclusive;

    private boolean cannotCached = false;
    private RubyObject cachedValue = null;

    public DotNode(Node beginNode, Node endNode, boolean exclusive) {
        super(exclusive ? Constants.NODE_DOT3 : Constants.NODE_DOT2, beginNode, endNode, null);
        this.exclusive = exclusive;
    }

    public RubyObject eval(Ruby ruby, RubyObject self) {
        if (cachedValue != null) {
            return cachedValue;
        }

        RubyObject result = RubyRange.newRange(ruby, self.eval(getBeginNode()), self.eval(getEndNode()), exclusive);

        if (cannotCached) {
            return result;
        } else if (getBeginNode().getLiteral() instanceof RubyFixnum && getEndNode().getLiteral() instanceof RubyFixnum) {
            cachedValue = result;
        } else {
            cannotCached = true;
        }

        return result;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitDotNode(this);
    }
}