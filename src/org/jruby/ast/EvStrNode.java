/*
 * EvStrNode.java - description
 * Created on 28.02.2002, 00:12:36
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

import org.ablaf.ast.INode;
import org.ablaf.ast.visitor.INodeVisitor;
import org.ablaf.common.ISourcePosition;
import org.jruby.ast.visitor.NodeVisitor;

/** Represents an #{} expression in a string. This Node is always a subnode
 * of a DStrNode, DXStrNode or a DRegexpNode.
 * 
 * Before this Node is evaluated it contains the code as a String (value). After
 * the first evaluation this String is parsed into the evaluatedNode INode.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class EvStrNode extends AbstractNode {
    private final String value;
    private INode evaluatedNode;

    public EvStrNode(ISourcePosition position, String value) {
        super(position);
        this.value = value;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitEvStrNode(this);
    }

    /**
     * Gets the value.
     * @return Returns a String
     */
    public String getValue() {
        return value;
    }

    /**
     * Gets the evaluatedNode.
     * @return Returns a INode
     */
    public INode getEvaluatedNode() {
        return evaluatedNode;
    }

    /**
     * Sets the evaluatedNode.
     * @param evaluatedNode The evaluatedNode to set
     */
    public void setEvaluatedNode(INode evaluatedNode) {
        this.evaluatedNode = evaluatedNode;
    }
}
