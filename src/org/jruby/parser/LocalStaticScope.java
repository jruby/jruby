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
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.Node;
import org.jruby.ast.VCallNode;
import org.jruby.lexer.yacc.ISourcePosition;

public class LocalStaticScope extends StaticScope {
    private static final long serialVersionUID = 2204064248888411628L;
    private static final String[] NO_NAMES = new String[0];

    protected LocalStaticScope(StaticScope enclosingScope) {
        this(enclosingScope, NO_NAMES);
    }

    protected LocalStaticScope(StaticScope enclosingScope, String[] names) {
        super(enclosingScope, names);

        setBackrefLastlineScope(true);
    }

    public StaticScope getLocalScope() {
        return this;
    }

    public int isDefined(String name, int depth) {
        return (depth << 16) | exists(name);
    }

    @Override
    public boolean isArgumentScope() {
        return true;
    }

    @Override
    public void makeArgumentScope() {
        // We blindly call this on any scope we encounter which is ok.
        // define_method(:foo, &Kernel.method(:foo)) is typically where we see this.
    }

    /**
     * @see org.jruby.parser.StaticScope#getAllNamesInScope()
     */
    public String[] getAllNamesInScope() {
        return getVariables();
    }
    
    public AssignableNode assign(ISourcePosition position, String name, Node value, 
            StaticScope topScope, int depth) {
        int slot = exists(name);
        
        // We can assign if we already have variable of that name here or we are the only
        // scope in the chain (which Local scopes always are).
        if (slot >= 0) {
            return new LocalAsgnNode(position, name, ((depth << 16) | slot), value);
        } else if (topScope == this) {
            slot = addVariable(name);

            return new LocalAsgnNode(position, name, slot , value);
        }
        
        // We know this is a block scope because a local scope cannot be within a local scope
        // If topScope was itself it would have created a LocalAsgnNode above.
        return ((BlockStaticScope) topScope).addAssign(position, name, value);
    }

    public Node declare(ISourcePosition position, String name, int depth) {
        int slot = exists(name);
        
        if (slot >= 0) return new LocalVarNode(position, ((depth << 16) | slot), name);
        
        return new VCallNode(position, name);
    }
    
    @Override
    public String toString() {
        return "LocalScope: " + super.toString();
    }
}
