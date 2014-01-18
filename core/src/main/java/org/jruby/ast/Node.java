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
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Thomas Corbat <tcorbat@hsr.ch>
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

import java.util.ArrayList;
import java.util.List;

import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.exceptions.JumpException;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.ISourcePositionHolder;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.DefinedMessage;

/**
 * Base class for all Nodes in the AST
 */
public abstract class Node implements ISourcePositionHolder, ParseResult {    
    // We define an actual list to get around bug in java integration (1387115)
    static final List<Node> EMPTY_LIST = new ArrayList<Node>();
    
    private ISourcePosition position;

    public Node(ISourcePosition position) {
        assert position != null;
        this.position = position;
    }

    /**
     * Location of this node within the source
     */
    public ISourcePosition getPosition() {
        return position;
    }

    public void setPosition(ISourcePosition position) {
        this.position = position;
    }
    
    public abstract Object accept(NodeVisitor visitor);
    public abstract List<Node> childNodes();

    protected static List<Node> createList(Node node) {
        ArrayList<Node> list = new ArrayList<Node>(1);

        list.add(node);

        return list;
    }

    protected static List<Node> createList(Node node1, Node node2) {
        ArrayList<Node> list = new ArrayList<Node>(2);

        list.add(node1);
        list.add(node2);

        return list;
    }

    protected static List<Node> createList(Node node1, Node node2, Node node3) {
        ArrayList<Node> list = new ArrayList<Node>(3);

        list.add(node1);
        list.add(node2);
        list.add(node3);

        return list;
    }

    protected static List<Node> createList(Node... nodes) {
        ArrayList<Node> list = new ArrayList<Node>(nodes.length);
        
        for (Node node: nodes) {
            if (node != null) list.add(node);
        }
        
        return list;
    }

    @Override
    public String toString() {
        if (this instanceof InvisibleNode) return "";
        
        StringBuilder builder = new StringBuilder(60);

        builder.append("(").append(getNodeName());

        if (this instanceof INameNode) {
            builder.append(":").append(((INameNode) this).getName());
        }

        builder.append(" ").append(getPosition().getStartLine());

        List<Node> children = childNodes();
        for (int i = 0; i < children.size(); i++) {
            builder.append(", ").append(children.get(i));
        }
        builder.append(")");

        return builder.toString();
    }

    protected String getNodeName() {
        String name = getClass().getName();
        int i = name.lastIndexOf('.');
        String nodeType = name.substring(i + 1);
        return nodeType;
    }

    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        throw new RuntimeException(this.getClass().getSimpleName() + " should not be directly interpreted");
    }
    
    public IRubyObject assign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject value, Block block, boolean checkArity) {
        throw new RuntimeException("Invalid node encountered in interpreter: \"" + getClass().getName() + "\", please report this at www.jruby.org");
    }
    
    public RubyString definition(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        try {
            interpret(runtime, context, self, aBlock);
            return runtime.getDefinedMessage(DefinedMessage.EXPRESSION);
        } catch (JumpException jumpExcptn) {
        }
        
        return null;
    }

    /**
     * @return the nodeId
     */
    public abstract NodeType getNodeType();

    /**
     * Whether the node evaluates to nil and has no side effects.
     *
     * @return true if nil, false otherwise
     */
    public boolean isNil() {
        return false;
    }
}
