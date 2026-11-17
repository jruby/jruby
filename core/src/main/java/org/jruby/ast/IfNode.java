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

    // ---- what MRI's compiler folds away (recorded by every parse: it decides what is compiled) ----

    private byte constantPredicate;         // 0: not a literal; 1: a literal MRI folds to true; -1: one it folds to false
    private boolean foldedPredicate;        // MRI compiles nothing for the predicate (only literals decide it)
    private boolean modifier;               // written as a modifier: stmt if cond

    // ---- branch coverage (recorded by the parser when it records positions, read by the IR builder) ----

    /**
     * A Ruby-level conditional as MRI reports it (which is not every IfNode: a pattern guard is none).
     *
     * @param unless written as unless: then/else bodies are swapped
     * @param elsif an elsif clause of an enclosing if
     * @param predicateEnd just past the condition (an empty then arm is reported there), or null
     * @param elseStart the 'else' keyword, when there is one, or null
     * @param sourceBody for a modifier: the statement as written (before begin/end unwrapping), or null
     */
    private record Branch(boolean unless, boolean elsif, SourcePosition predicateEnd, SourcePosition elseStart,
                          Node sourceBody) {
    }

    public void markBranch(boolean unless, boolean elsif, long predicateEnd, long elseStart, Node sourceBody) {
        setSourceDetail(new Branch(unless, elsif, SourcePosition.of(predicateEnd), SourcePosition.of(elseStart), sourceBody));
    }

    // null when this is no branch, or positions are not recorded
    private Branch branch() {
        return (Branch) getSourceDetail();
    }

    public boolean isBranch() {
        return branch() != null;
    }

    public boolean isUnless() {
        Branch branch = branch();
        return branch != null && branch.unless;
    }

    public boolean isElsif() {
        Branch branch = branch();
        return branch != null && branch.elsif;
    }

    private SourcePosition predicateEnd() {
        Branch branch = branch();
        return branch == null ? null : branch.predicateEnd;
    }

    public boolean hasPredicateEnd() {
        return predicateEnd() != null;
    }

    public int getPredicateEndLine() {
        SourcePosition predicateEnd = predicateEnd();
        return predicateEnd == null ? -1 : predicateEnd.line();
    }

    public int getPredicateEndColumn() {
        SourcePosition predicateEnd = predicateEnd();
        return predicateEnd == null ? -1 : predicateEnd.column();
    }

    private SourcePosition elseStart() {
        Branch branch = branch();
        return branch == null ? null : branch.elseStart;
    }

    public boolean hasElseStart() {
        return elseStart() != null;
    }

    public int getElseStartLine() {
        SourcePosition elseStart = elseStart();
        return elseStart == null ? -1 : elseStart.line();
    }

    public int getElseStartColumn() {
        SourcePosition elseStart = elseStart();
        return elseStart == null ? -1 : elseStart.column();
    }

    /**
     * The statement of a modifier conditional as written, or null when this is none or positions are not recorded.
     */
    public Node getSourceBody() {
        Branch branch = branch();
        return branch == null ? null : branch.sourceBody;
    }

    public void setModifier() {
        modifier = true;
    }

    /**
     * Whether this conditional was written as a modifier (stmt if cond).
     */
    public boolean isModifier() {
        return modifier;
    }

    /**
     * MRI folds a conditional on a literal predicate away, reporting no branch for it and compiling nothing of
     * the arm that cannot run.
     *
     * @param constantPredicate 0 when the predicate is not a literal, 1 when it is a truthy one, -1 a falsy one
     */
    public void setConstantPredicate(int constantPredicate) {
        this.constantPredicate = (byte) constantPredicate;
    }

    public boolean hasConstantPredicate() {
        return constantPredicate != 0;
    }

    public boolean isConstantlyTrue() {
        return constantPredicate > 0;
    }

    /**
     * @param foldedPredicate whether MRI compiles no instructions for the predicate: a constant predicate made of
     *                        literals only (unlike <code>x and false</code>, which still calls x)
     */
    public void setFoldedPredicate(boolean foldedPredicate) {
        this.foldedPredicate = foldedPredicate;
    }

    public boolean hasFoldedPredicate() {
        return foldedPredicate;
    }
}
