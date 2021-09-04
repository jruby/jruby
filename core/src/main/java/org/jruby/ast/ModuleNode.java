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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.ast;

import java.util.List;

import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.parser.StaticScope;

/** 
 * Represents a module definition.
 */
public class ModuleNode extends Node implements IScopingNode {
    private final Colon3Node cpath;
    private final StaticScope scope;
    private final Node bodyNode;
    private final int endLine;

    public ModuleNode(int line, Colon3Node cpath, StaticScope scope, Node bodyNode, int endLine) {
        super(line, cpath.containsVariableAssignment() || bodyNode.containsVariableAssignment());

        assert scope != null : "scope is not null";
        assert bodyNode != null : "bodyNode is not null";

        this.cpath = cpath;
        this.scope = scope;
        this.bodyNode = bodyNode;
        this.endLine = endLine;
    }

    public NodeType getNodeType() {
        return NodeType.MODULENODE;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public <T> T accept(NodeVisitor<T> iVisitor) {
        return iVisitor.visitModuleNode(this);
    }

    /**
     * Gets the body of this class.
     * 
     * @return the contents
     */
    public Node getBodyNode() {
        return bodyNode;
    }

    /**
     * Gets line where the 'end' was for this module.
     */
    public int getEndLine() {
        return endLine;
    }

    /**
     * Get the static scoping information.
     * 
     * @return the scoping info
     */
    public StaticScope getScope() {
        return scope;
    }

    /**
     * Gets the name.
     * @return Representation of the module path+name
     */
    public Colon3Node getCPath() {
        return cpath;
    }
    
    public List<Node> childNodes() {
        return Node.createList(cpath, bodyNode);
    }

    @Override
    public boolean executesOnce() {
        return true;
    }
}
