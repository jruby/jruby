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

import org.ablaf.ast.*;
import org.ablaf.common.*;
import org.jruby.ast.visitor.*;

/** Represents a '::' constant access or method call.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class Colon2Node extends AbstractNode {
    private INode leftNode;
    private String name;

    public Colon2Node(ISourcePosition position, INode leftNode, String name) {
        super(position);

        this.leftNode = leftNode;
        this.name = name;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitColon2Node(this);
    }

    /**
     * Gets the leftNode.
     * @return Returns a INode
     */
    public INode getLeftNode() {
        return leftNode;
    }

    /**
     * Sets the leftNode.
     * @param leftNode The leftNode to set
     */
    public void setLeftNode(INode leftNode) {
        this.leftNode = leftNode;
    }

    /**
     * Gets the name.
     * @return Returns a String
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     * @param name The name to set
     */
    public void setName(String name) {
        this.name = name;
    }

}