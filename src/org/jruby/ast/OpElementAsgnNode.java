/*
 * OpElementAsgnNode.java - description
 * Created on 01.03.2002, 22:55:32
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Copyright (C) 2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Thomas E Enebo <enebo@acm.org>
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

import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.SourcePosition;

/** Represents an operator assignment to an element.
 * 
 * This could be for example:
 * 
 * <pre>
 * a[4] += 5
 * a[3] &&= true
 * </pre>
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class OpElementAsgnNode extends Node {
    static final long serialVersionUID = 1509701560452403776L;

    private final Node receiverNode;
    private final String operatorName;
    private final Node argsNode;
    private final Node valueNode;

    public OpElementAsgnNode(SourcePosition position, Node receiverNode, String operatorName, Node argsNode, Node valueNode) {
        super(position);
        this.receiverNode = receiverNode;
        this.operatorName = operatorName;
        this.argsNode = argsNode;
        this.valueNode = valueNode;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitOpElementAsgnNode(this);
    }

    /**
     * Gets the argsNode.
     * @return Returns a Node
     */
    public Node getArgsNode() {
        return argsNode;
    }

    /**
     * Gets the operatorName.
     * @return Returns a String
     */
    public String getOperatorName() {
        return operatorName;
    }

    /**
     * Gets the receiverNode.
     * @return Returns a Node
     */
    public Node getReceiverNode() {
        return receiverNode;
    }

    /**
     * Gets the valueNode.
     * @return Returns a Node
     */
    public Node getValueNode() {
        return valueNode;
    }
}
