/*
 * OpAsgnNode.java - description
 * Created on 01.03.2002, 23:39:06
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

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class OpAsgnNode extends AbstractNode {
    private INode receiverNode;
    private INode valueNode;
    private String variableName;
    private String operatorName;

    public OpAsgnNode(ISourcePosition position, INode receiverNode, INode valueNode, String variableName, String methodName) {
        super(position);

        this.receiverNode = receiverNode;
        this.valueNode = valueNode;
        this.variableName = variableName;
        this.operatorName = methodName;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitOpAsgnNode(this);
    }

    /**
     * Gets the methodName.
     * @return Returns a String
     */
    public String getOperatorName() {
        return operatorName;
    }

    /**
     * Sets the methodName.
     * @param methodName The methodName to set
     */
    public void setOperatorName(String methodName) {
        this.operatorName = methodName;
    }

    /**
     * Gets the receiverNode.
     * @return Returns a INode
     */
    public INode getReceiverNode() {
        return receiverNode;
    }

    /**
     * Sets the receiverNode.
     * @param receiverNode The receiverNode to set
     */
    public void setReceiverNode(INode receiverNode) {
        this.receiverNode = receiverNode;
    }

    /**
     * Gets the valueNode.
     * @return Returns a INode
     */
    public INode getValueNode() {
        return valueNode;
    }

    /**
     * Sets the valueNode.
     * @param valueNode The valueNode to set
     */
    public void setValueNode(INode valueNode) {
        this.valueNode = valueNode;
    }

    /**
     * Gets the varibaleName.
     * @return Returns a String
     */
    public String getVariableName() {
        return variableName;
    }

    /**
     * Sets the varibaleName.
     * @param varibaleName The varibaleName to set
     */
    public void setVariableName(String varibaleName) {
        this.variableName = varibaleName;
    }

}