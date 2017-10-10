/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Mirko Stocker <me@misto.ch>
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

/**
 * Base class for DefnNode and DefsNode 
 */
import org.jruby.ast.types.INameNode;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

public abstract class MethodDefNode extends Node implements INameNode, DefNode {
    protected final ByteList name;
    protected final ArgsNode argsNode;
    protected final StaticScope scope;
    protected final Node bodyNode;
    protected final int endLine;

    public MethodDefNode(ISourcePosition position, ByteList name, ArgsNode argsNode,
                         StaticScope scope, Node bodyNode, int endLine) {
        super(position, bodyNode.containsVariableAssignment());

        assert bodyNode != null : "bodyNode must not be null";
            
        this.name = name;
        this.argsNode = argsNode;
        this.scope = scope;
        this.bodyNode = bodyNode;
        this.endLine = endLine;
    }


    /**
     * Gets the argsNode.
     * @return Returns a Node
     */
    public ArgsNode getArgsNode() {
        return argsNode;
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
     * Gets the body of this class.
     *
     * @return the contents
     */
    public Node getBodyNode() {
        return bodyNode;
    }

    /**
     * Gets the name.
     * @return Returns a String
     */
    public String getName() {
        return StringSupport.byteListAsString(name);
    }

    public ByteList getByteName() {
        return name;
    }

    /**
     * Which line is the 'end' encountered on.  Useful for RETURN event generation.
     * @return the zero-based line number
     */
    public int getEndLine() {
        return endLine;
    }
}
