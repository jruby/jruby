/*
 * MAsgnNode.java - No description
 * Created on 19.01.2002, 19:12:14
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
import org.jruby.nodes.types.*;
import org.jruby.nodes.visitor.*;
import org.jruby.runtime.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class MAsgnNode extends Node implements AssignableNode {
    public MAsgnNode(Node headNode, Node argsNode) {
        super(Constants.NODE_MASGN, headNode, null, argsNode);
    }

    public RubyObject eval(Ruby ruby, RubyObject self) {
        return massign(ruby, self, toArray(ruby, self.eval(getValueNode())), false);
    }
    
    private static RubyArray toArray(Ruby ruby, RubyObject value) {
        if (value == null) {
            return RubyArray.newArray(ruby);
        } else if (!(value instanceof RubyArray)) {
            if (value.respond_to(RubySymbol.newSymbol(ruby, "to_ary")).isTrue()) {
                return (RubyArray)value.funcall("to_ary");
            } else {
                return RubyArray.newArray(ruby, value);
            }
        }
        return (RubyArray)value;
    }

    public RubyObject massign(Ruby ruby, RubyObject self, RubyArray value, boolean check) {
        Node list = getHeadNode();
        int i = 0;
        for (; list != null && i < value.getLength(); i++) {
            ((AssignableNode) list.getHeadNode()).assign(ruby, self, value.entry(i), check);
            list = list.getNextNode();
        }

        if (check && list != null) {
            // error
        }

        if (getArgsNode() != null) {
            if (getArgsNode() == MINUS_ONE) {
            } else if (list == null && i < value.getLength()) {
                ((AssignableNode) getArgsNode()).assign(ruby, self,
                    value.subseq(i, value.getLength() - i), check);
            } else {
                ((AssignableNode) getArgsNode()).assign(ruby, self, RubyArray.newArray(ruby), check);
            }
        } else if (check && i < value.getLength()) {
            // error
        }

        while (list != null) {
            i++;
            ((AssignableNode) list.getHeadNode()).assign(ruby, self, ruby.getNil(), check);
            list = list.getNextNode();
        }
        return value;
    }

    public void assign(Ruby ruby, RubyObject self, RubyObject value, boolean check) {
        massign(ruby, self, toArray(ruby, value), check);
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitMAsgnNode(this);
    }
}