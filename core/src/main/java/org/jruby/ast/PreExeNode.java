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
 * Copyright (C) 2007 Thomas E Enebo <enebo@acm.org>
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

import org.jruby.Ruby;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.InterpretedBlock;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A pre-execution construction (BEGIN { ... }).
 */
public class PreExeNode extends IterNode {
    public PreExeNode(ISourcePosition position, StaticScope scope, Node body) {
        super(position, null, scope, body);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.PREEXENODE;
    }

    @Override
    public Object accept(NodeVisitor iVisitor) {
        return iVisitor.visitPreExeNode(this);
    }
    
    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        // ensure the scope has a module associated (thish will usualy just ==
        // whatever the toplevel module is).
        getScope().determineModule();

        DynamicScope scope = DynamicScope.newDynamicScope(getScope());

        // Each root node has a top-level scope that we need to push
        context.preScopedBody(scope);

        // FIXME: I use a for block to implement END node because we need a proc which captures
        // its enclosing scope.   ForBlock now represents these node and should be renamed.
        Block block = InterpretedBlock.newInterpretedClosure(context, this, self);

        try {
            block.yield(context, null);
        } finally {
            context.postScopedBody();
        }

        return runtime.getNil();
    }
}
