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

import org.jruby.MetaClass;
import org.jruby.RubyClass;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.Node;
import org.jruby.ast.executable.Script;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.PositionAware;
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
public class DefaultMethod extends DynamicMethod implements MethodArgs, PositionAware, CacheableMethod {

    private static class DynamicMethodBox {
        public DynamicMethod actualMethod;
        public int callCount = 0;
    }

    private DynamicMethodBox box = new DynamicMethodBox();
    private final StaticScope staticScope;
    private final Node body;
    private final ArgsNode argsNode;
    private final ISourcePosition position;
    private final InterpretedMethod interpretedMethod;

    public DefaultMethod(RubyModule implementationClass, StaticScope staticScope, Node body,
            String name, ArgsNode argsNode, Visibility visibility, ISourcePosition position) {
        super(implementationClass, visibility, CallConfiguration.FrameFullScopeFull, name);
        this.interpretedMethod = DynamicMethodFactory.newInterpretedMethod(
                implementationClass.getRuntime(), implementationClass, staticScope,
                body, name, argsNode, visibility, position);
        this.interpretedMethod.serialNumber = this.serialNumber;
        this.box.actualMethod = interpretedMethod;
        this.argsNode = argsNode;
        this.body = body;
        this.staticScope = staticScope;
        this.position = position;

        assert argsNode != null;
    }

    public int getCallCount() {
        return box.callCount;
    }

    public int incrementCallCount() {
        return ++box.callCount;
    }

    public void setCallCount(int callCount) {
        this.box.callCount = callCount;
    }

    public Node getBodyNode() {
        return body;
    }

    public ArgsNode getArgsNode() {
        return argsNode;
    }

    public StaticScope getStaticScope() {
        return staticScope;
    }

    public DynamicMethod getMethodForCaching() {
        if (!RubyInstanceConfig.DYNOPT_COMPILE_ENABLED) {
            DynamicMethod method = box.actualMethod;
            if (method instanceof JittedMethod) {
                return method;
            }
        }
        return this;
    }

    public void switchToJitted(Script jitCompiledScript, CallConfiguration jitCallConfig) {
        this.box.actualMethod = DynamicMethodFactory.newJittedMethod(
                getImplementationClass().getRuntime(), getImplementationClass(),
                staticScope, jitCompiledScript, name, jitCallConfig, getVisibility(), argsNode.getArity(), position,
                this);
        this.box.actualMethod.serialNumber = this.serialNumber;
        this.box.callCount = -1;
        if (!RubyInstanceConfig.DYNOPT_COMPILE_ENABLED) {
            getImplementationClass().invalidateCacheDescendants();
        }
    }

    private DynamicMethod tryJitReturnMethod(ThreadContext context) {
        String className;
        if (implementationClass.isSingleton()) {
            MetaClass metaClass = (MetaClass)implementationClass;
            RubyClass realClass = metaClass.getRealClass();
            // if real class is Class
            if (realClass == context.runtime.getClassClass()) {
                // use the attached class's name
                className = ((RubyClass)metaClass.getAttached()).getName();
            } else {
                // use the real class name
                className = realClass.getName();
            }
        } else {
            // use the class name
            className = implementationClass.getName();
        }
        // replace double-colons with dots, to match Java
        className.replaceAll("::", ".");
        context.getRuntime().getJITCompiler().tryJIT(this, context, className, name);
        return box.actualMethod;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        if (box.callCount >= 0) {
            return tryJitReturnMethod(context).call(context, self, clazz, name, args, block);
        }
        
        return box.actualMethod.call(context, self, clazz, name, args, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
        if (box.callCount >= 0) {
            return tryJitReturnMethod(context).call(context, self, clazz, name, args);
        }

        return box.actualMethod.call(context, self, clazz, name, args);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        if (box.callCount >= 0) {
            return tryJitReturnMethod(context).call(context, self, clazz, name);
        }

        return box.actualMethod.call(context, self, clazz, name );
    }
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        if (box.callCount >= 0) {
            return tryJitReturnMethod(context).call(context, self, clazz, name, block);
        }

        return box.actualMethod.call(context, self, clazz, name, block);
    }
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
        if (box.callCount >= 0) {
            return tryJitReturnMethod(context).call(context, self, clazz, name, arg0);
        }

        return box.actualMethod.call(context, self, clazz, name , arg0);
    }
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        if (box.callCount >= 0) {
            return tryJitReturnMethod(context).call(context, self, clazz, name, arg0, block);
        }

        return box.actualMethod.call(context, self, clazz, name, arg0, block);
    }
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
        if (box.callCount >= 0) {
            return tryJitReturnMethod(context).call(context, self, clazz, name, arg0, arg1);
        }

        return box.actualMethod.call(context, self, clazz, name , arg0, arg1);
    }
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        if (box.callCount >= 0) {
            return tryJitReturnMethod(context).call(context, self, clazz, name, arg0, arg1, block);
        }

        return box.actualMethod.call(context, self, clazz, name, arg0, arg1, block);
    }
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        if (box.callCount >= 0) {
            return tryJitReturnMethod(context).call(context, self, clazz, name, arg0, arg1, arg2);
        }

        return box.actualMethod.call(context, self, clazz, name , arg0, arg1, arg2);
    }
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (box.callCount >= 0) {
            return tryJitReturnMethod(context).call(context, self, clazz, name, arg0, arg1, arg2, block);
        }

        return box.actualMethod.call(context, self, clazz, name, arg0, arg1, arg2, block);
    }

    public ISourcePosition getPosition() {
        return position;
    }

    public String getFile() {
        return position.getFile();
    }

    public int getLine() {
        return position.getLine();
    }

    @Override
    public Arity getArity() {
        return argsNode.getArity();
    }

    public DynamicMethod dup() {
        DefaultMethod newMethod = new DefaultMethod(getImplementationClass(), staticScope, body, name, argsNode, getVisibility(), position);
        newMethod.setIsBuiltin(this.builtin);
        newMethod.box = this.box;
        return newMethod;
    }

    @Override
    public void setVisibility(Visibility visibility) {
        // We promote our box to being its own box since we're changing
        // visibilities, and need it to be reflected on this method object
        // independent of any other sharing the box.
        DynamicMethodBox newBox = new DynamicMethodBox();
        newBox.actualMethod = box.actualMethod.dup();
        newBox.callCount = box.callCount;
        box = newBox;
        super.setVisibility(visibility);
    }
}
