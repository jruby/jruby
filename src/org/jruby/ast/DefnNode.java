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
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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
package org.jruby.ast;

import java.util.List;

import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.DynamicMethodFactory;
import org.jruby.internal.runtime.methods.WrapperMethod;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * method definition node.
 */
public class DefnNode extends MethodDefNode implements INameNode {
    public DefnNode(ISourcePosition position, ArgumentNode nameNode, ArgsNode argsNode, 
            StaticScope scope, Node bodyNode) {
        super(position, nameNode, argsNode, scope, bodyNode);
    }

    public NodeType getNodeType() {
        return NodeType.DEFNNODE;
    }

    public Object accept(NodeVisitor iVisitor) {
        return iVisitor.visitDefnNode(this);
    }
    
    /**
     * Get the name of this method
     */
    @Override
    public String getName() {
        return nameNode.getName();
    }
    
    public List<Node> childNodes() {
        return Node.createList(nameNode, argsNode, bodyNode);
    }
    
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        RubyModule containingClass = context.getRubyClass();
   
        if (containingClass == runtime.getDummy()) {
            throw runtime.newTypeError("no class/module to add method");
        }
   
        String name = getName();

        if (containingClass == runtime.getObject() && name == "initialize") {
            runtime.getWarnings().warn(ID.REDEFINING_DANGEROUS, "redefining Object#initialize may cause infinite loop");
        }

        if (name == "__id__" || name == "__send__") {
            runtime.getWarnings().warn(ID.REDEFINING_DANGEROUS, "redefining `" + name + "' may cause serious problem"); 
        }

        Visibility visibility = context.getCurrentVisibility();
        if (name == "initialize" || name == "initialize_copy" || visibility == Visibility.MODULE_FUNCTION) {
            visibility = Visibility.PRIVATE;
        }
        
        scope.determineModule();
        
        // Make a nil node if no body.  Notice this is not part of AST.
        Node body = bodyNode == null ? new NilNode(getPosition()) : bodyNode;

        DynamicMethod newMethod = DynamicMethodFactory.newDefaultMethod(
                runtime, containingClass, name, scope, body, argsNode,
                visibility, getPosition());
   
        containingClass.addMethod(name, newMethod);
   
        if (context.getCurrentVisibility() == Visibility.MODULE_FUNCTION) {
            containingClass.getSingletonClass().addMethod(name,
                    new WrapperMethod(containingClass.getSingletonClass(), newMethod, Visibility.PUBLIC));
            
            containingClass.callMethod(context, "singleton_method_added", runtime.fastNewSymbol(name));
        }
   
        // 'class << state.self' and 'class << obj' uses defn as opposed to defs
        if (containingClass.isSingleton()) {
            ((MetaClass) containingClass).getAttached().callMethod(context, 
                    "singleton_method_added", runtime.fastNewSymbol(name));
        } else {
            containingClass.callMethod(context, "method_added", runtime.fastNewSymbol(name));
        }
   
        return runtime.getNil();        
    }
}
