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
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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
import org.jruby.truffle.parser.ast.types.INameNode;
import org.jruby.truffle.parser.ast.visitor.NodeVisitor;

import java.util.List;

/** 
 * Represents a method call with self as an implicit receiver.
 */
public class FCallParseNode extends ParseNode implements INameNode, IArgumentNode, BlockAcceptingParseNode {
    private String name;
    protected ParseNode argsNode;
    protected ParseNode iterNode;

    public FCallParseNode(SourceIndexLength position, String name) {
        this(position, name, null, null);
    }
    public FCallParseNode(SourceIndexLength position, String name, ParseNode argsNode, ParseNode iterNode) {
        super(position, argsNode != null && argsNode.containsVariableAssignment() || iterNode != null && iterNode.containsVariableAssignment());
        this.name = name;
        this.argsNode = argsNode;
        this.iterNode = iterNode;
    }

    public NodeType getNodeType() {
        return NodeType.FCALLNODE;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public <T> T accept(NodeVisitor<T> iVisitor) {
        return iVisitor.visitFCallNode(this);
    }

    /**
     * Get the node that represents a block or a block variable.
     */
    public ParseNode getIterNode() {
        return iterNode;
    }

    public ParseNode setIterNode(ParseNode iterNode) {
        this.iterNode = iterNode;

        return this;
    }

    /**
     * Gets the argsNode.
     * @return Returns a ParseNode
     */
    public ParseNode getArgsNode() {
        return argsNode;
    }

    /**
     * Set the argsNode.  Changes to parser means fcall is made before actual
     * args are associated with fcall so we need a setter.
     */
    public ParseNode setArgsNode(ParseNode argsNode) {
        this.argsNode = argsNode;

        return argsNode;
    }

    /**
     * Gets the name.
     * @return Returns a String
     */
    public String getName() {
        return name;
    }

    public List<ParseNode> childNodes() {
        return createList(argsNode, iterNode);
    }
}
