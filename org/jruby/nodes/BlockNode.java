/*
 * BlockNode.java - No description
 * Created on 05. November 2001, 21:44
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
 * A structuring node (linked list of other nodes).
 * This type of node is used to structure the AST.
 * It is used to build a linked list
 * the meaning of the Node fields for BlockNodes is:
 * <ul>
 * <li>
 * u1 ==&gt; head the node in this link
 * </li>
 * <li>
 * u2 ==&gt; end the last link of the list this is a blocknode
 * </li>
 * <li>
 * u3 ==&gt; next the next link this is a blocknode
 * </li>
 * </ul>
 * this nodes are created through the {@link NodeFactory#newBlock newBlock} 
 * method.  This method is in turned invoked by the parser directly but also
 * by the {@link org.jruby.parser.ParserHelper#block_append block_append} method
 * @author  jpetersen
 * @version $Revision$
 */
public class BlockNode extends Node {

	/**
	 * Builds a BlockNode with a given head.
	 * The end node of this block node will be set to itself.
	 * @param headNode the head (content of the link) for this block node 
	 **/
    public BlockNode(Node headNode) {
        super(Constants.NODE_BLOCK, headNode, null, null);
		setEndNode(this);
    }
    
	/**
	 * Evaluate this node.
	 *	To eval a blocknode, if present the head node is evaled then 
	 *	the next node is treated as if it was a block node (by evaling 
	 *	its headnode if present and continuing).                       	  
	 *	@return the value returned by evaling the last node of the block
	 *			or nil.
	 **/
    public RubyObject eval(Ruby ruby, RubyObject self) {
        Node node = this;
        while (node.getNextNode() != null) {
            node.getHeadNode().eval(ruby, self);
            node = node.getNextNode();
        }
        return node.getHeadNode() != null ? node.getHeadNode().eval(ruby, self) 
                                          : ruby.getNil();
    }

	/**
	 * Method used by visitors.
	 * accepts the visitor
	 * @param iVisitor the visitor to accept
	 **/
	public void accept(NodeVisitor iVisitor)
	{
		iVisitor.visitBlockNode(this);
	}
}
