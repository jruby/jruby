/*
 * BlockNode.java - description
 * Created on 27.02.2002, 12:22:41
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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
package org.jruby.ast;

import java.util.*;

import org.ablaf.ast.*;
import org.ablaf.common.*;

import org.jruby.ast.types.*;
import org.jruby.ast.visitor.*;
import org.ablaf.ast.visitor.INodeVisitor;

/**
 * A structuring node (linked list of other nodes).
 * This type of node is used to structure the AST.
 * Used in many places it is created throught the {@link org.jruby.parser.ParserSupport#appendToBlock appendToBlock} method
 * @author  jpetersen
 * @version $Revision$
 */
public class BlockNode extends AbstractNode implements IListNode {
    private List list;

    /**
     * Builds a BlockNode with a given head.
     * The end node of this block node will be set to itself.
     * @param headNode the head (content of the link) for this block node 
     **/
    public BlockNode(ISourcePosition position) {
        super(position);
    }

    /**
     * @see IListNode#add(Node)
     */
    public IListNode add(INode node) {
        if (list == null) {
            list = new ArrayList();
        }

        list.add(node);

        return this;
    }

    /**
     * @see IListNode#iterator()
     */
    public Iterator iterator() {
        return list != null ? list.iterator() : Collections.EMPTY_LIST.iterator();
    }

    /**
     * Method used by visitors.
     * accepts the visitor
     * @param iVisitor the visitor to accept
     **/
    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitBlockNode(this);
    }
}
