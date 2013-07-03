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
package org.jruby.ast;

import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.runtime.Helpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/** 
 * Represents a '::' constant access or method call (Java::JavaClass).
 */
public abstract class Colon2Node extends Colon3Node implements INameNode {
    protected final Node leftNode;

    public Colon2Node(ISourcePosition position, Node leftNode, String name) {
        super(position, name);
        this.leftNode = leftNode;
    }

    public NodeType getNodeType() {
        return NodeType.COLON2NODE;
    }
    
    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    @Override
    public Object accept(NodeVisitor iVisitor) {
        return iVisitor.visitColon2Node(this);
    }

    /**
     * Gets the leftNode.
     * @return Returns a Node
     */
    public Node getLeftNode() {
        return leftNode;
    }

    @Override
    public List<Node> childNodes() {
        return Node.createList(leftNode);
    }

    /** Get parent module/class that this module represents */
    @Override
    public RubyModule getEnclosingModule(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
    	if (leftNode != null && leftNode instanceof NilNode) {
            throw context.runtime.newTypeError("no outer class/module");
        }
        return Helpers.prepareClassNamespace(context, context.getCurrentStaticScope(), leftNode.interpret(runtime, context, self, aBlock));
    }
 }
