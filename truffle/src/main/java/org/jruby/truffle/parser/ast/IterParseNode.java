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
package org.jruby.truffle.parser.ast;

import org.jruby.truffle.language.SourceIndexLength;
import org.jruby.truffle.parser.ast.visitor.NodeVisitor;
import org.jruby.truffle.parser.scope.StaticScope;

import java.util.List;

/**
 * Represents a block.  
 */
public class IterParseNode extends ParseNode implements DefNode {
    private final ParseNode varNode;
    private final ParseNode bodyNode;

    // What static scoping relationship exists when it comes into being.
    private StaticScope scope;

    /**
     *  Used by Truffle 'for' and by ForParseNode only.
     * This is to support 1.8-style assignments which only 'for' expressions use.
     */
    public IterParseNode(SourceIndexLength position, ParseNode args, StaticScope scope, ParseNode body) {
        super(position, args != null && args.containsVariableAssignment || body != null && body.containsVariableAssignment);

        this.varNode = args;
        this.scope = scope;
        this.bodyNode = body;
    }

    /**
     * Used for all non-for types of blocks.
     */
    public IterParseNode(SourceIndexLength position, ArgsParseNode args, ParseNode body, StaticScope scope) {
        super(position, args != null && args.containsVariableAssignment || body != null && body.containsVariableAssignment);

        this.varNode = args;
        this.bodyNode = body == null ? NilImplicitParseNode.NIL : body;
        this.scope = scope;
    }

    public NodeType getNodeType() {
        return NodeType.ITERNODE;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public <T> T accept(NodeVisitor<T> iVisitor) {
        return iVisitor.visitIterNode(this);
    }

    @Override
    public ArgsParseNode getArgsNode() {
        return (ArgsParseNode) varNode;
    }

    public StaticScope getScope() {
        return scope;
    }

    /**
     * Gets the bodyNode.
     * @return Returns a ParseNode
     */
    public ParseNode getBodyNode() {
        return bodyNode;
    }

    /**
     * Gets the varNode.
     * @return Returns a ParseNode
     */
    public ParseNode getVarNode() {
        return varNode;
    }

    public List<ParseNode> childNodes() {
        return ParseNode.createList(varNode, bodyNode);
    }

    public int getEndLine() {
        return -1;
    }
}
