/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2008 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.internal.runtime.methods;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.Node;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class TraceableInterpretedMethod extends InterpretedMethod {
    private StaticScope staticScope;
    private Node body;
    private ArgsNode argsNode;
    private ISourcePosition position;

    public TraceableInterpretedMethod(RubyModule implementationClass, StaticScope staticScope, Node body,
            String name, ArgsNode argsNode, Visibility visibility, ISourcePosition position) {
        super(implementationClass, staticScope, body, name, argsNode,
            visibility, position);
        this.body = body;
        this.staticScope = staticScope;
        this.argsNode = argsNode;
        this.position = position;
		
        assert argsNode != null;
    }

    @Override
    protected boolean isTraceable() {
        return true;
    }

    @Override
    public DynamicMethod dup() {
        return new TraceableInterpretedMethod(getImplementationClass(), staticScope, body, name, argsNode, getVisibility(), position);
    }
}
