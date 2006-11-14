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
 * Copyright (C) 2006 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.parser;

import org.jruby.ast.AssignableNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DVarNode;
import org.jruby.ast.Node;
import org.jruby.lexer.yacc.ISourcePosition;

public class BlockStaticScope extends StaticScope {
    private static final long serialVersionUID = -3882063260379968149L;

    public BlockStaticScope(StaticScope parentScope) {
        super(parentScope);
    }
    
    public StaticScope getLocalScope() {
        return getEnclosingScope().getLocalScope();
    }
    
    public int isDefined(String name, int depth) {
        int slot = exists(name); 
        if (slot >= 0) return (depth << 16) | exists(name);
        
        return getEnclosingScope().isDefined(name, depth + 1);
    }
    
    /**
     * @see org.jruby.parser.StaticScope#getAllNamesInScope()
     */
    public String[] getAllNamesInScope() {
        String[] variables = getEnclosingScope().getAllNamesInScope();
        String[] ourVariables = getVariables();

        // We have no names to add to existing list
        if (ourVariables == null) return variables;
        
        // we know variables cannot be null since localstaticscope will create a 0 length one.
        int newSize = variables.length + ourVariables.length;
        String[] names = new String[newSize];
        
        System.arraycopy(variables, 0, names, 0, variables.length);
        System.arraycopy(ourVariables, 0, names, variables.length, ourVariables.length);
        
        return names;
    }

    protected AssignableNode assign(ISourcePosition position, String name, Node value, 
            StaticScope topScope, int depth) {
        int slot = exists(name);
        
        if (slot >= 0) {
            return new DAsgnNode(position, name, ((depth << 16) | slot), value);
        }

        return getEnclosingScope().assign(position, name, value, topScope, depth + 1);
    }

    public AssignableNode addAssign(ISourcePosition position, String name, Node value) {
        int slot = addVariable(name);
        
        // No bit math to store level since we know level is zero for this case
        return new DAsgnNode(position, name, slot, value);
    }

    public Node declare(ISourcePosition position, String name, int depth) {
        int slot = exists(name);

        if (slot >= 0) {
            return new DVarNode(position, ((depth << 16) | slot), name);
        }
        
        return getEnclosingScope().declare(position, name, depth + 1);
    }
}
