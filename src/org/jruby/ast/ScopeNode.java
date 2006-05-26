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
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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

import java.io.IOException;
import java.util.List;

import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.evaluator.Instruction;
import org.jruby.lexer.yacc.ISourcePosition;

/**
 * Scope in the parse tree.
 * indicates where in the parse tree a new scope should be started when evaling.
 * Unlike many node this is not created directly as part of the parce process,
 * rather it is created as a side effect of other things like creating a ClassNode
 * or SClassNode.  It can also be created by evaling a DefnNode or DefsNode as
 * part of the call to copyNodeScope.
 *
 * @author  jpetersen
 */
public class ScopeNode extends Node {
    static final long serialVersionUID = 3694868125861223886L;

    private String[] localNames;
    private final Node bodyNode;

    public ScopeNode(ISourcePosition position, String[] table, Node bodyNode) {
        super(position);
        this.localNames = new String[table.length];
        for (int i = 0; i < table.length; i++) 
            if (table[i] != null)
                localNames[i] = table[i].intern();
        this.bodyNode = bodyNode;
    }
    
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        
        // deserialized strings are not interned; intern it now
        String[] old = localNames;
        this.localNames = new String[old.length];
        for (int i = 0; i < old.length; i++) 
            if (old[i] != null)
                localNames[i] = old[i].intern();
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public Instruction accept(NodeVisitor iVisitor) {
        return iVisitor.visitScopeNode(this);
    }

    /**
     * Gets the bodyNode.
     * @return Returns a Node
     */
    public Node getBodyNode() {
        return bodyNode;
    }

    /**
     * Gets the localNames.
     * @return Returns a List
     */
    public String[] getLocalNames() {
        return localNames;
    }
    
    public List childNodes() {
        return createList(bodyNode);
    }

}
