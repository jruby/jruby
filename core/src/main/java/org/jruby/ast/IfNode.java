/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
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

package org.jruby.ast;

import java.util.List;

import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.parser.ProductionState;

/**
 * an 'if' statement.
 */
public class IfNode extends Node {
    private final Node condition;
    private final Node thenBody;
    private final Node elseBody;

    public IfNode(int line, Node condition, Node thenBody, Node elseBody) {
        super(line, condition.containsVariableAssignment || thenBody != null && thenBody.containsVariableAssignment ||
                elseBody != null && elseBody.containsVariableAssignment);

        this.condition = condition;
        this.thenBody = thenBody;
        this.elseBody = elseBody;
        setNewline();
    }

    public NodeType getNodeType() {
        return NodeType.IFNODE;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public <T> T accept(NodeVisitor<T> iVisitor) {
        return iVisitor.visitIfNode(this);
    }

    /**
     * Gets the condition.
     * @return Returns a Node
     */
    public Node getCondition() {
        return condition;
    }

    /**
     * Gets the elseBody.
     * @return Returns a Node
     */
    public Node getElseBody() {
        return elseBody;
    }

    /**
     * Gets the thenBody.
     * @return Returns a Node
     */
    public Node getThenBody() {
        return thenBody;
    }
    
    public List<Node> childNodes() {
        return Node.createList(condition, thenBody, elseBody);
    }

    // ---- branch coverage (recorded by the parser, read by the IR builder) ----

    private boolean branch;                 // a Ruby-level conditional MRI reports (not e.g. a pattern guard)
    private boolean unless;                 // written as unless: then/else bodies are swapped
    private boolean elsif;                  // an elsif clause of an enclosing if
    private int predicateEndLine = -1;      // zero-based position just past the condition (an empty then arm is reported there)
    private int predicateEndColumn = -1;
    private int elseStartLine = -1;         // zero-based position of the 'else' keyword, when there is one
    private int elseStartColumn = -1;
    private Node sourceBody;                // for a modifier: the statement as written (before begin/end unwrapping)
    private int constantPredicate;          // 0: not a literal; 1: a literal MRI folds to true; -1: one it folds to false

    public void markBranch(boolean unless, boolean elsif, long predicateEnd, long elseStart) {
        this.branch = true;
        this.unless = unless;
        this.elsif = elsif;
        if (predicateEnd >= 0) {
            predicateEndLine = ProductionState.line(predicateEnd);
            predicateEndColumn = ProductionState.column(predicateEnd);
        }
        if (elseStart >= 0) {
            elseStartLine = ProductionState.line(elseStart);
            elseStartColumn = ProductionState.column(elseStart);
        }
    }

    public boolean isBranch() {
        return branch;
    }

    public boolean isUnless() {
        return unless;
    }

    public boolean isElsif() {
        return elsif;
    }

    public boolean hasPredicateEnd() {
        return predicateEndColumn >= 0;
    }

    public int getPredicateEndLine() {
        return predicateEndLine;
    }

    public int getPredicateEndColumn() {
        return predicateEndColumn;
    }

    public boolean hasElseStart() {
        return elseStartColumn >= 0;
    }

    public int getElseStartLine() {
        return elseStartLine;
    }

    public int getElseStartColumn() {
        return elseStartColumn;
    }

    public void setSourceBody(Node sourceBody) {
        this.sourceBody = sourceBody;
    }

    /**
     * MRI folds a conditional on a literal predicate away, reporting no branch for it and compiling nothing of
     * the arm that cannot run.
     *
     * @param constantPredicate 0 when the predicate is not a literal, 1 when it is a truthy one, -1 a falsy one
     */
    public void setConstantPredicate(int constantPredicate) {
        this.constantPredicate = constantPredicate;
    }

    public boolean hasConstantPredicate() {
        return constantPredicate != 0;
    }

    public boolean isConstantlyTrue() {
        return constantPredicate > 0;
    }

    public Node getSourceBody() {
        return sourceBody;
    }
}
