/*
 * ArrayNode.java - No description
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

import java.util.*;

import org.jruby.*;
import org.jruby.nodes.visitor.*;
import org.jruby.runtime.*;

/**
 * normal array
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class ArrayNode extends Node {
    public ArrayNode(Node headNode, int aLength, Node nextNode) {
        super(Constants.NODE_ARRAY, headNode, aLength, nextNode);
    }

    public RubyObject eval(Ruby ruby, RubyObject self) {
        return getRubyArray(ruby, self);
    }

    public RubyArray getRubyArray(Ruby ruby, RubyObject self) {
        return new RubyArray(ruby, getArrayList(ruby, self), true);
    }

    public ArrayList getArrayList(Ruby ruby, RubyObject self) {
        ArrayList ary = new ArrayList(getALength());
        for (Node node = this; node != null; node = node.getNextNode()) {
            ary.add(node.getHeadNode().eval(ruby, self));
        }
        return ary;
    }

    public RubyObject[] getArray(Ruby ruby, RubyObject self) {
        ArrayList ary = getArrayList(ruby, self);
        return (RubyObject[]) ary.toArray(new RubyObject[ary.size()]);
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitArrayNode(this);
    }
}