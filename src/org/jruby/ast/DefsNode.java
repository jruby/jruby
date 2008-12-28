/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubySymbol;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.DynamicMethodFactory;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/** 
 * Represents a singleton method definition.
 */
public class DefsNode extends MethodDefNode implements INameNode {
    private final Node receiverNode;
    public DefsNode(ISourcePosition position, Node receiverNode, ArgumentNode nameNode, ArgsNode argsNode, 
            StaticScope scope, Node bodyNode) {
        super(position, nameNode, argsNode, scope, bodyNode);
        
        assert receiverNode != null : "receiverNode is not null";
        
        this.receiverNode = receiverNode;
    }

    public NodeType getNodeType() {
        return NodeType.DEFSNODE;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public Object accept(NodeVisitor iVisitor) {
        return iVisitor.visitDefsNode(this);
    }

    /**
     * Gets the receiverNode.
     * @return Returns a Node
     */
    public Node getReceiverNode() {
        return receiverNode;
    }
    
    /**
     * Gets the name of this method
     */
    @Override
    public String getName() {
        return nameNode.getName();
    }
    
    public List<Node> childNodes() {
        return Node.createList(receiverNode, nameNode, argsNode, bodyNode);
    }
    
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        IRubyObject receiver = receiverNode.interpret(runtime,context, self, aBlock);
        String name = getName();

        if (runtime.getSafeLevel() >= 4 && !receiver.isTaint()) {
            throw runtime.newSecurityError("Insecure; can't define singleton method.");
        }

        if (receiver instanceof RubyFixnum || receiver instanceof RubySymbol) {
          throw runtime.newTypeError("can't define singleton method \"" + name
          + "\" for " + receiver.getMetaClass().getBaseName());
        }

        if (receiver.isFrozen()) throw runtime.newFrozenError("object");

        RubyClass rubyClass = receiver.getSingletonClass();

        if (runtime.getSafeLevel() >= 4 && rubyClass.getMethods().get(name) != null) {
            throw runtime.newSecurityError("redefining method prohibited.");
        }

        scope.determineModule();
      
        // Make a nil node if no body.  Notice this is not part of AST.
        Node body = bodyNode == null ? new NilNode(getPosition()) : bodyNode;
        
        DynamicMethod newMethod = DynamicMethodFactory.newInterpretedMethod(
                runtime, rubyClass, name, scope, body, argsNode,
                Visibility.PUBLIC, getPosition());
   
        rubyClass.addMethod(name, newMethod);
        receiver.callMethod(context, "singleton_method_added", runtime.fastNewSymbol(name));
   
        return runtime.getNil();
    }
}
