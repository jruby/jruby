/*
 * CaseNode.java - description
 * Created on 28.02.2002, 00:33:00
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
import org.jruby.*;
import org.jruby.ast.types.*;
import org.jruby.ast.visitor.*;
import org.jruby.runtime.*;
import org.ablaf.ast.visitor.INodeVisitor;

/**
 * A Case statement.
 * 
 * Represents a complete case statement, including the body with its
 * when statements.
 * 
 * @author  jpetersen
 * @version $Revision$
 */
public class CaseNode extends AbstractNode {
	/**
	 * the case expression.
	 **/
    private INode caseNode;
	/**
	 * the body of the case.
	 * this is a list of when nodes.
	 */
    private IListNode whenNodes;
    /** the else node
     */
    private INode elseNode;
    
    public CaseNode(ISourcePosition position, INode caseNode, IListNode whenNodes, INode elseNode) {
        super(position);
        
        this.caseNode = caseNode;
        this.whenNodes = whenNodes;
        this.elseNode = elseNode;
    }

 	/**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(INodeVisitor iVisitor) {
        ((NodeVisitor)iVisitor).visitCaseNode(this);
    }

    /**
     * Gets the caseNode.
	 * caseNode is the case expression 
     * @return caseNode
     */
    public INode getCaseNode() {
        return caseNode;
    }

    /**
     * Sets the caseNode.
	 * caseNode is the case expression 
     * @param caseNode The caseNode to set
     */
    public void setCaseNode(INode caseNode) {
        this.caseNode = caseNode;
    }

    /**
     * Gets the whenNodes.
	 * the body of the case statement, a list of WhenNode
     * @return whenNodes
     */
    public IListNode getWhenNodes() {
        return whenNodes;
    }

    /**
     * Sets the whenNodes.
	 * the body of the case statement, a list of WhenNode
     * @param whenNodes The whenNodes to set
     */
    public void setWhenNodes(IListNode whenNodes) {
        this.whenNodes = whenNodes;
    }

    /**
     * Gets the elseNode.
     * @return Returns a INode
     */
    public INode getElseNode() {
        return elseNode;
    }

    /**
     * Sets the elseNode.
     * @param elseNode The elseNode to set
     */
    public void setElseNode(INode elseNode) {
        this.elseNode = elseNode;
    }
}
