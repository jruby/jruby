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
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
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
package org.jruby.runtime;

import org.jruby.EvalType;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A Block implemented using a Java-based BlockCallback implementation
 * rather than with an ICallable. For lightweight block logic within
 * Java code.
 */
@Deprecated
public class CompiledBlockLight19 extends CompiledBlock19 {
    public static Block newCompiledClosureLight(ThreadContext context, IRubyObject self, Arity arity,
            StaticScope scope, CompiledBlockCallback19 callback, boolean hasMultipleArgsHead, int argumentType) {
        Binding binding = context.currentBinding(self, Visibility.PUBLIC);
        BlockBody body = new CompiledBlockLight19(arity, scope, callback, hasMultipleArgsHead, argumentType, EMPTY_PARAMETER_LIST);

        return new Block(body, binding);
    }

    public static BlockBody newCompiledBlockLight(Signature signature,
                                                  StaticScope scope, CompiledBlockCallback19 callback, boolean hasMultipleArgsHead, int argumentType, String[] parameterList) {
        return new CompiledBlockLight19(signature, scope, callback, hasMultipleArgsHead, argumentType, parameterList);
    }

    public static BlockBody newCompiledBlockLight(Arity arity,
            StaticScope scope, CompiledBlockCallback19 callback, boolean hasMultipleArgsHead, int argumentType, String[] parameterList) {
        return new CompiledBlockLight19(arity, scope, callback, hasMultipleArgsHead, argumentType, parameterList);
    }

    public void setEvalType(EvalType evalType) {
    }

    protected CompiledBlockLight19(Signature signature, StaticScope scope, CompiledBlockCallback19 callback, boolean hasMultipleArgsHead, int argumentType, String[] parameterList) {
        super(signature, scope, callback, hasMultipleArgsHead, argumentType, parameterList);
    }

    protected CompiledBlockLight19(Arity arity, StaticScope scope, CompiledBlockCallback19 callback, boolean hasMultipleArgsHead, int argumentType, String[] parameterList) {
        super(arity, scope, callback, hasMultipleArgsHead, argumentType, parameterList);
    }
    
    @Override
    protected Frame pre(ThreadContext context, Binding binding) {
        return context.preYieldLightBlock(binding, binding.getDummyScope(scope));
    }
    
    @Override
    protected final void post(ThreadContext context, Binding binding, Visibility vis, Frame lastFrame) {
        binding.getFrame().setVisibility(vis);
        context.postYieldLight(binding, lastFrame);
    }
}
