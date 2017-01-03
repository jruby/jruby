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
 * Explicit block argument (on caller side):
 *   foobar(1, 2, &foo)
 *   foobar(1, 2, &lhs_which_returns_something_block/proc_like)
 *
 * bodyNode is any expression which can return something which is ultimately
 * coercible to a proc.
 */
public class BlockPassParseNode extends ParseNode {
    private final ParseNode bodyNode;

    /** Used by the arg_blk_pass and new_call, new_fcall and new_super
     * methods in ParserSupport to temporary save the args node.  This should
     * not be used directly by compiler or interpreter.
     */
    private ParseNode argsNode;

    public BlockPassParseNode(SourceIndexLength position, ParseNode bodyNode) {
        super(position, bodyNode != null && bodyNode.containsVariableAssignment());
        this.bodyNode = bodyNode;
    }

    public NodeType getNodeType() {
        return NodeType.BLOCKPASSNODE;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public <T> T accept(NodeVisitor<T> iVisitor) {
        return iVisitor.visitBlockPassNode(this);
    }

    /**
     * Gets the bodyNode.
     * @return Returns a ParseNode
     */
    public ParseNode getBodyNode() {
        return bodyNode;
    }

    /**
     * Gets the argsNode.
     * @return Returns a IListNode
     */
    public ParseNode getArgsNode() {
        return argsNode;
    }

    /**
     * Sets the argsNode.
     * @param argsNode The argsNode to set
     */
    public void setArgsNode(ParseNode argsNode) {
        this.argsNode = argsNode;
    }

    public List<ParseNode> childNodes() {
        return ParseNode.createList(argsNode, bodyNode);
    }
}
