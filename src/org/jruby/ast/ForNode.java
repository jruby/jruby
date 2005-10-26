/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ast;

import java.util.List;

import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.evaluator.SingleNodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;

/**
 * a For statement.
 * this is almost equivalent to an iternode (the difference being the visibility of the
 * local variables defined in the iterator).
 * 
 * @see IterNode
 * @author  jpetersen
 */
public class ForNode extends Node {
    static final long serialVersionUID = -8319863477790150586L;

    private final Node varNode;
    private final Node bodyNode;
    private final Node iterNode;

    public ForNode(ISourcePosition position, Node varNode, Node bodyNode, Node iterNode) {
        super(position);
        this.varNode = varNode;
        this.bodyNode = bodyNode;
        this.iterNode = iterNode;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public SingleNodeVisitor accept(NodeVisitor iVisitor) {
        return iVisitor.visitForNode(this);
    }

    /**
     * Gets the bodyNode.
	 * bodyNode is the expression after the in, it is the expression which will have its each() method called.
     * @return Returns a Node
     */
    public Node getBodyNode() {
        return bodyNode;
    }

    /**
     * Gets the iterNode.
	 * iterNode is the block which will be executed when the each() method of the bodyNode will yield.
	 * 
     * @return Returns a Node
     */
    public Node getIterNode() {
        return iterNode;
    }

    /**
     * Gets the varNode.
	 * varNode is the equivalent of the block variable in a regular method call with block type of iteration
     * @return Returns a Node
     */
    public Node getVarNode() {
        return varNode;
    }
    
    public List childNodes() {
        return Node.createList(varNode, iterNode, bodyNode);
    }

}
