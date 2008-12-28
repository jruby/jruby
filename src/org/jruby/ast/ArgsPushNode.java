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
 * Copyright (C) 2006-2007 Thomas E Enebo <enebo@acm.org>
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
import org.jruby.RubyArray;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ArgsPushNode extends Node {
    private Node firstNode;
    private Node secondNode;
    
    public ArgsPushNode(ISourcePosition position, Node firstNode, Node secondNode) {
        super(position);
        
        assert firstNode != null : "ArgsPushNode.first == null";
        assert secondNode != null : "ArgsPushNode.second == null";
        
        this.firstNode = firstNode;
        this.secondNode = secondNode;
    }

    public NodeType getNodeType() {
        return NodeType.ARGSPUSHNODE;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visitArgsPushNode(this);
    }
    
    public Node getFirstNode() {
        return firstNode;
    }
    
    public Node getSecondNode() {
        return secondNode;
    }

    public List<Node> childNodes() {
        return EMPTY_LIST;
    }
    
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        RubyArray args = (RubyArray) firstNode.interpret(runtime, context, self, aBlock).dup();
        
        return args.append(secondNode.interpret(runtime, context, self, aBlock));        
    }
}
