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
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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
import org.jruby.RubyModule;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/** 
 * Represents a module definition.
 */
public class ModuleNode extends Node implements IScopingNode {
    private final Colon3Node cpath;
    private final StaticScope scope;
    private final Node bodyNode;

    public ModuleNode(ISourcePosition position, Colon3Node cpath, StaticScope scope, Node bodyNode) {
        super(position);

        assert cpath != null : "cpath is not null";
        assert scope != null : "scope is not null";
        assert bodyNode != null : "bodyNode is not null";

        this.cpath = cpath;
        this.scope = scope;
        this.bodyNode = bodyNode;
    }

    public NodeType getNodeType() {
        return NodeType.MODULENODE;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public Object accept(NodeVisitor iVisitor) {
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
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        RubyModule enclosingModule = cpath.getEnclosingModule(runtime, context, self, aBlock);

        if (enclosingModule == null) throw runtime.newTypeError("no outer class/module");

        String name = cpath.getName();        

        RubyModule module = enclosingModule.defineOrGetModuleUnder(name);

        scope.setModule(module);        

        return ASTInterpreter.evalClassDefinitionBody(runtime, context, scope, bodyNode, module, self, aBlock);
    }
}
