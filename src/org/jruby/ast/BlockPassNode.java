/*
 * BlockPassNode.java - No description
 * Created on 20.01.2002, 20:06:22
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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
package org.jruby.ast;

import org.jruby.ast.types.IListNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.ablaf.ast.visitor.INodeVisitor;
import org.ablaf.ast.INode;
import org.ablaf.common.ISourcePosition;

/**
 * Block passed explicitly as an argument in a method call.
 * A block passing argument in a method call (last argument prefixed by an ampersand).
 * 
 * @author  jpetersen
 * @version $Revision$
 */
public class BlockPassNode extends AbstractNode {
    static final long serialVersionUID = 7201862349971094217L;

    private INode bodyNode;
    private INode iterNode;

    /** Used by the arg_blk_pass and new_call, new_fcall and new_super
     * methods in ParserSupport to temporary save the args node.
     */
    private IListNode argsNode;

    public BlockPassNode(ISourcePosition position, INode bodyNode) {
        super(position);
        
        this.bodyNode = bodyNode;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitBlockPassNode(this);
    }

    /**
     * Gets the bodyNode.
     * @return Returns a INode
     */
    public INode getBodyNode() {
        return bodyNode;
    }

    /**
     * Sets the bodyNode.
     * @param bodyNode The bodyNode to set
     */
    public void setBodyNode(INode bodyNode) {
        this.bodyNode = bodyNode;
    }

    /**
     * Gets the iterNode.
     * @return Returns a INode
     */
    public INode getIterNode() {
        return iterNode;
    }

    /**
     * Sets the iterNode.
     * @param iterNode The iterNode to set
     */
    public void setIterNode(INode iterNode) {
        this.iterNode = iterNode;
    }

    /**
     * Gets the argsNode.
     * @return Returns a IListNode
     */
    public IListNode getArgsNode() {
        return argsNode;
    }

    /**
     * Sets the argsNode.
     * @param argsNode The argsNode to set
     */
    public void setArgsNode(IListNode argsNode) {
        this.argsNode = argsNode;
    }

}
