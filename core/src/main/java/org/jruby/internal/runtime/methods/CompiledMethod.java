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
 * Copyright (C) 2007 Charles Oliver Nutter <headius@headius.com>
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

import org.jruby.RubyModule;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Block;
import org.jruby.runtime.MethodFactory;
import org.jruby.runtime.PositionAware;

public abstract class CompiledMethod extends JavaMethod implements Cloneable, PositionAware, MethodArgs2, MethodWithNodes {
    protected Object $scriptObject;
    protected ISourcePosition position;

    private MethodNodes methodNodes;
    
    public static class LazyCompiledMethod extends DynamicMethod implements Cloneable, PositionAware, MethodArgs2 {
        private final String rubyName;
        private final String javaName;
        private final StaticScope scope;
        private final Object scriptObject;
        private MethodFactory factory;
        private DynamicMethod compiledMethod;
        private final ISourcePosition position;
        private final String parameterDesc;
        private final String[] parameterList;
        private final MethodNodes methodNodes;
    
        public LazyCompiledMethod(
                RubyModule implementationClass,
                String rubyName,
                String javaName,
                Visibility visibility,
                StaticScope scope,
                Object scriptObject,
                CallConfiguration callConfig,
                ISourcePosition position,
                String parameterDesc,
                MethodFactory factory,
                MethodNodes methodNodes) {
            
            super(implementationClass, visibility, callConfig);
            this.rubyName = rubyName;
            this.javaName = javaName;
            this.scope = scope;
            this.scriptObject = scriptObject;
            this.factory = factory;
            this.position = position;
            this.parameterDesc = parameterDesc;
            this.parameterList = parameterDesc.split(";");
            this.methodNodes = methodNodes;
        }
        
        private synchronized void initializeMethod() {
            if (compiledMethod != null) return;
            compiledMethod = factory.getCompiledMethod(implementationClass, rubyName, javaName, visibility, scope, scriptObject, callConfig, position, parameterDesc, methodNodes);
            factory = null;
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            if (compiledMethod == null) initializeMethod();
            return compiledMethod.call(context, self, clazz, name);
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
            if (compiledMethod == null) initializeMethod();
            return compiledMethod.call(context, self, clazz, name, arg0);
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
            if (compiledMethod == null) initializeMethod();
            return compiledMethod.call(context, self, clazz, name, arg0, arg1);
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
            if (compiledMethod == null) initializeMethod();
            return compiledMethod.call(context, self, clazz, name, arg0, arg1, arg2);
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            if (compiledMethod == null) initializeMethod();
            return compiledMethod.call(context, self, clazz, name, args);
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
            if (compiledMethod == null) initializeMethod();
            return compiledMethod.call(context, self, clazz, name, block);
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
            if (compiledMethod == null) initializeMethod();
            return compiledMethod.call(context, self, clazz, name, arg0, block);
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
            if (compiledMethod == null) initializeMethod();
            return compiledMethod.call(context, self, clazz, name, arg0, arg1, block);
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            if (compiledMethod == null) initializeMethod();
            return compiledMethod.call(context, self, clazz, name, arg0, arg1, arg2, block);
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            if (compiledMethod == null) initializeMethod();
            return compiledMethod.call(context, self, clazz, name, args, block);
        }

        @Override
        public Arity getArity() {
            if (compiledMethod == null) initializeMethod();
            return compiledMethod.getArity();
        }

        @Override
        public CallConfiguration getCallConfig() {
            if (compiledMethod == null) initializeMethod();
            return compiledMethod.getCallConfig();
        }

        @Override
        public RubyModule getImplementationClass() {
            if (compiledMethod == null) initializeMethod();
            return compiledMethod.getImplementationClass();
        }

        @Override
        public boolean isImplementedBy(RubyModule other) {
            if (compiledMethod == null) return false;
            return compiledMethod.isImplementedBy(other);
        }

        @Override
        protected RubyModule getProtectedClass() {
            if (compiledMethod == null) initializeMethod();
            return compiledMethod.getProtectedClass();
        }

        @Override
        public DynamicMethod getRealMethod() {
            if (compiledMethod == null) initializeMethod();
            return compiledMethod.getRealMethod();
        }

        @Override
        public Visibility getVisibility() {
            if (compiledMethod == null) initializeMethod();
            return compiledMethod.getVisibility();
        }

        @Override
        public boolean isCallableFrom(IRubyObject caller, CallType callType) {
            if (compiledMethod == null) initializeMethod();
            return compiledMethod.isCallableFrom(caller, callType);
        }

        @Override
        public boolean isNative() {
            if (compiledMethod == null) initializeMethod();
            return compiledMethod.isNative();
        }

        @Override
        public void setCallConfig(CallConfiguration callConfig) {
            if (compiledMethod == null) initializeMethod();
            compiledMethod.setCallConfig(callConfig);
        }

        @Override
        public void setImplementationClass(RubyModule implClass) {
            if (compiledMethod == null) initializeMethod();
            compiledMethod.setImplementationClass(implClass);
        }

        @Override
        public void setVisibility(Visibility visibility) {
            if (compiledMethod == null) initializeMethod();
            compiledMethod.setVisibility(visibility);
        }

        @Override
        public DynamicMethod dup() {
            if (compiledMethod == null) initializeMethod();
            return compiledMethod.dup();
        }

        public String getFile() {
            if (compiledMethod == null) initializeMethod();
            return position.getFile();
        }

        public int getLine() {
            if (compiledMethod == null) initializeMethod();
            return position.getLine();
        }

        public String[] getParameterList() {
            if (compiledMethod == null) initializeMethod();
            return parameterList;
        }
        
    }

    // Called by Class.newInstance in InvocationMethodCompiler and precompiled method subclasses
    public CompiledMethod() {
        // TODO: Need to have access to AST for recompilation. See #1395
        this(null);
    }
    
    protected CompiledMethod(MethodNodes methodNodes) {
        this.methodNodes = methodNodes;
    }
    
    protected void init(
            RubyModule implementationClass,
            Visibility visibility,
            StaticScope staticScope,
            Object scriptObject,
            CallConfiguration callConfig,
            ISourcePosition position,
            String parameterDesc) {
        
        this.$scriptObject = scriptObject;
        this.position = position;
        setParameterDesc(parameterDesc);
        super.init(implementationClass, null, visibility, staticScope, callConfig);
    }
        
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        return call(context, self, clazz, name, Block.NULL_BLOCK);
    }
        
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg) {
        return call(context, self, clazz, name, arg, Block.NULL_BLOCK);
    }
        
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg1, IRubyObject arg2) {
        return call(context, self, clazz, name, arg1, arg2, Block.NULL_BLOCK);
    }
        
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return call(context, self, clazz, name, arg1, arg2, arg3, Block.NULL_BLOCK);
    }
    
    @Override
    public DynamicMethod dup() {
        try {
            CompiledMethod msm = (CompiledMethod)clone();
            return msm;
        } catch (CloneNotSupportedException cnse) {
            return null;
        }
    }

    @Override
    public boolean isNative() {
        return false;
    }

    public String getFile() {
        return position.getFile();
    }

    public int getLine() {
        return position.getLine();
    }

    public Object getScriptObject() {
        return $scriptObject;
    }

    @Override
    public MethodNodes getMethodNodes() {
        return methodNodes;
    }

    public void unsafeSetMethodNodes(MethodNodes methodNodes) {
        this.methodNodes = methodNodes;
    }

}
