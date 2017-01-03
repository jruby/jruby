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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.truffle.parser.ast;

import org.jruby.truffle.language.SourceIndexLength;
import org.jruby.truffle.parser.ast.visitor.NodeVisitor;

import java.util.List;

/** 
 * Represents an until statement.
 */
public class UntilParseNode extends ParseNode {
    private final ParseNode conditionNode;
    private final ParseNode bodyNode;
    private final boolean evaluateAtStart;

    public UntilParseNode(SourceIndexLength position, ParseNode conditionNode, ParseNode bodyNode) {
        this(position, conditionNode, bodyNode, true);
    }

    public NodeType getNodeType() {
        return NodeType.UNTILNODE;
    }

    public UntilParseNode(SourceIndexLength position, ParseNode conditionNode, ParseNode bodyNode, boolean evaluateAtStart) {
        super(position, conditionNode.containsVariableAssignment() || bodyNode.containsVariableAssignment());

        assert conditionNode != null : "conditionNode is not null";
        assert bodyNode != null : "bodyNode is not null";

        this.conditionNode = conditionNode;
        this.bodyNode = bodyNode;
        this.evaluateAtStart = evaluateAtStart;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public <T> T accept(NodeVisitor<T> iVisitor) {
        return iVisitor.visitUntilNode(this);
    }

    /**
     * Gets the bodyNode.
     * @return Returns a ParseNode
     */
    public ParseNode getBodyNode() {
        return bodyNode;
    }

    /**
     * Gets the conditionNode.
     * @return Returns a ParseNode
     */
    public ParseNode getConditionNode() {
        return conditionNode;
    }

    public List<ParseNode> childNodes() {
        return ParseNode.createList(conditionNode, bodyNode);
    }
    
    /**
     * Determine whether this is while or do while
     * @return true if you are a while, false if do while
     */
    public boolean evaluateAtStart() {
        return evaluateAtStart;
    }
}
