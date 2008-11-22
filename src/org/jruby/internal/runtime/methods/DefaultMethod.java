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
 * Copyright (C) 2004-2008 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.internal.runtime.methods;

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
 * This is the mixed-mode method type.  It will call out to JIT compiler to see if the compiler
 * wants to JIT or not.  If the JIT compiler does JIT this it will return the new method
 * to be executed here instead of executing the interpreted version of this method.  The next
 * invocation of the method will end up causing the runtime to load and execute the newly JIT'd
 * method.
 *
 */
public class DefaultMethod extends InterpretedMethod {
    private int callCount = 0;
    
    public DefaultMethod(RubyModule implementationClass, String name, StaticScope staticScope, Node body,
            ArgsNode argsNode, Visibility visibility, ISourcePosition position) {
        super(implementationClass, name, staticScope, body, argsNode, visibility, position);
    }

    public int getCallCount() {
        return callCount;
    }

    public int incrementCallCount() {
        return ++callCount;
    }

    public void setCallCount(int callCount) {
        this.callCount = callCount;
    }

    private DynamicMethod tryJIT(ThreadContext context, String name) {
        return context.getRuntime().getJITCompiler().tryJIT(this, context, name);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name,
            IRubyObject[] args, Block block) {
        DynamicMethod method = tryJIT(context, name);
        
        if (method != null) return method.call(context, self, clazz, name, args, block);
        return super.call(context, self, clazz, name, args, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name,
            IRubyObject[] args) {
        DynamicMethod method = tryJIT(context, name);

        if (method != null) return method.call(context, self, clazz, name, args);
        return super.call(context, self, clazz, name, args);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        DynamicMethod method = tryJIT(context, name);

        if (method != null) return method.call(context, self, clazz, name);
        return super.call(context, self, clazz, name);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name,
            Block block) {
        DynamicMethod method = tryJIT(context, name);

        if (method != null) return method.call(context, self, clazz, name, block);
        return super.call(context, self, clazz, name, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name,
            IRubyObject arg0) {
        DynamicMethod method = tryJIT(context, name);

        if (method != null) return method.call(context, self, clazz, name, arg0);
        return super.call(context, self, clazz, name, arg0);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name,
            IRubyObject arg0, Block block) {
        DynamicMethod method = tryJIT(context, name);

        if (method != null) return method.call(context, self, clazz, name, arg0, block);
        return super.call(context, self, clazz, name, arg0, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name,
            IRubyObject arg0, IRubyObject arg1) {
        DynamicMethod method = tryJIT(context, name);

        if (method != null) return method.call(context, self, clazz, name, arg0, arg1);
        return super.call(context, self, clazz, name, arg0, arg1);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name,
            IRubyObject arg0, IRubyObject arg1, Block block) {
        DynamicMethod method = tryJIT(context, name);

        if (method != null) return method.call(context, self, clazz, name, arg0, arg1, block);
        return super.call(context, self, clazz, name, arg0, arg1, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name,
            IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        DynamicMethod method = tryJIT(context, name);

        if (method != null) return method.call(context, self, clazz, name, arg0, arg1, arg2);
        return super.call(context, self, clazz, name, arg0, arg1, arg2);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name,
            IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        DynamicMethod method = tryJIT(context, name);

        if (method != null) return method.call(context, self, clazz, name, arg0, arg1, arg2, block);
        return super.call(context, self, clazz, name, arg0, arg1, arg2, block);
    }
}
