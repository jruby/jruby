/*
 * Colon2Node.java - description
 * Created on 28.02.2002, 16:52:54
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

import org.ablaf.ast.visitor.INodeVisitor;
import org.ablaf.ast.INode;
import org.ablaf.common.ISourcePosition;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.ast.types.INameNode;

/** Represents a '::' constant access or method call.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class Colon2Node extends AbstractNode implements INameNode {
    static final long serialVersionUID = -3250593470034657352L;

    private final INode leftNode;
    private final String name;

    public Colon2Node(ISourcePosition position, INode leftNode, String name) {
        super(position);
        this.leftNode = leftNode;
        this.name = name;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitColon2Node(this);
    }

    /**
     * Gets the leftNode.
     * @return Returns a INode
     */
    public INode getLeftNode() {
        return leftNode;
    }

    /**
     * Gets the name.
     * @return Returns a String
     */
    public String getName() {
        return name;
    }
}
