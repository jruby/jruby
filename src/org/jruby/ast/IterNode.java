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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2006 Thomas E Enebo <enebo@acm.org>
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
import org.jruby.RubyModule;

import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.evaluator.Instruction;
import org.jruby.internal.runtime.methods.AbstractCallable;
import org.jruby.internal.runtime.methods.EvaluateCallable;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.NilCallable;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Represents a block.  
 *
 */
public class IterNode extends Node {
    static final long serialVersionUID = -9181965000180892184L;

    private final Node varNode;
    private final Node bodyNode;
    private Node iterNode;
    
    // What static scoping relationship exists when it comes into being.
    private StaticScope scope;
    
    private transient ICallable callable;

    public IterNode(ISourcePosition position, Node varNode, StaticScope scope, Node bodyNode, Node iterNode) {
        this(position, varNode, scope, bodyNode, iterNode, NodeTypes.ITERNODE);
    }
    
    public IterNode(ISourcePosition position, Node varNode, StaticScope scope, Node bodyNode, 
            Node iterNode, int id) {
        super(position, id);
        this.varNode = varNode;
        this.scope = scope;
        this.bodyNode = bodyNode;
        this.iterNode = iterNode;
        
        // if body and var are null, don't both with a heavyweight callable
        if (bodyNode == null && varNode == null) {
            callable = NilCallable.NIL_CALLABLE;
        }
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public Instruction accept(NodeVisitor iVisitor) {
        return iVisitor.visitIterNode(this);
    }
    
    public StaticScope getScope() {
        return scope;
    }

    /**
     * Gets the bodyNode.
     * @return Returns a Node
     */
    public Node getBodyNode() {
        return bodyNode;
    }

    /**
     * Gets the iterNode.
     * @return Returns a Node
     */
    public Node getIterNode() {
        return iterNode;
    }

    /**
     * Sets the iterNode.
     * @param iterNode The iterNode to set
     */
    public void setIterNode(Node iterNode) {
        this.iterNode = iterNode;
    }

    /**
     * Gets the varNode.
     * @return Returns a Node
     */
    public Node getVarNode() {
        return varNode;
    }
    
    public List childNodes() {
        return Node.createList(varNode, bodyNode, iterNode);
    }

    public ICallable getCallable() {
        if (callable == null) {
            callable = new EvaluateCallable(bodyNode, varNode);
        }
        
        return callable;
    }
}
