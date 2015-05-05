/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2006 Thomas E Enebo <enebo@acm.org>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ast;

import java.util.List;

import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.BlockBody;

/**
 * A 'for' statement.  This is implemented using iter and that is how MRI does things,
 * but 'for's do not have their own stack, so doing this way is mildly painful.
 * 
 * @see IterNode
 */
public class ForNode extends IterNode {
    private Node iterNode;

    public ForNode(ISourcePosition position, Node varNode, Node bodyNode, Node iterNode, StaticScope scope) {
        // For nodes do not have their own scope so we pass null to indicate this.
        // 'For's are implemented as blocks in evaluation, but they have no scope so we
        // just deal with this lack of scope throughout its lifespan.  We should probably
        // change the way this works to get rid of multiple null checks.
        super(position, varNode, scope, bodyNode);
        
        assert iterNode != null : "iterNode is not null";
        
        this.iterNode = iterNode;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.FORNODE;
    }
    
    public Node getIterNode() {
        return iterNode;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    @Override
    public <T> T accept(NodeVisitor<T> iVisitor) {
        return iVisitor.visitForNode(this);
    }
    
    @Override
    public List<Node> childNodes() {
        return Node.createList(getVarNode(), getBodyNode(), iterNode);
    }
}
