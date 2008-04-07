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
import org.jruby.exceptions.JumpException.ReturnJump;
import org.jruby.internal.runtime.JumpTarget;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 */
public abstract class JavaMethod extends DynamicMethod implements JumpTarget, Cloneable {
    protected int arityValue;
    protected Arity arity;
    private Class[] argumentTypes;
    private String javaName;
    private boolean isSingleton;
    protected final int methodIndex;
    protected final StaticScope staticScope;
    
    public static abstract class JavaMethodNoBlock extends JavaMethod {
        public JavaMethodNoBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodNoBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, StaticScope staticScope, Arity arity) {
            super(implementationClass, visibility, callConfig, staticScope, arity);
        }
        public JavaMethodNoBlock(RubyModule implementationClass, Visibility visibility, int methodIndex) {
            super(implementationClass, visibility, methodIndex);
        }
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args);
        
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            return call(context, self, clazz, name, args);
        }
    }
    
    public static abstract class JavaMethodZero extends JavaMethod {
        public JavaMethodZero(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZero(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, StaticScope staticScope, Arity arity) {
            super(implementationClass, visibility, callConfig, staticScope, arity);
        }
        public JavaMethodZero(RubyModule implementationClass, Visibility visibility, int methodIndex) {
            super(implementationClass, visibility, methodIndex);
        }
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name);
        
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            if (args.length != 0) throw context.getRuntime().newArgumentError(args.length, 0);
            return call(context, self, clazz, name);
        }
    }
    
    public static abstract class JavaMethodZeroOrOne extends JavaMethod {
        public JavaMethodZeroOrOne(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZeroOrOne(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, StaticScope staticScope, Arity arity) {
            super(implementationClass, visibility, callConfig, staticScope, arity);
        }
        public JavaMethodZeroOrOne(RubyModule implementationClass, Visibility visibility, int methodIndex) {
            super(implementationClass, visibility, methodIndex);
        }
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name);
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg);
        
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            switch (args.length) {
            case 0:
                return call(context, self, clazz, name);
            case 1:
                return call(context, self, clazz, name, args[0]);
            default:
                Arity.raiseArgumentError(context.getRuntime(), args.length, 0, 1);
                return null; // never reached
            }
        }
    }
    
    public static abstract class JavaMethodZeroOrOneOrTwo extends JavaMethod {
        public JavaMethodZeroOrOneOrTwo(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZeroOrOneOrTwo(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, StaticScope staticScope, Arity arity) {
            super(implementationClass, visibility, callConfig, staticScope, arity);
        }
        public JavaMethodZeroOrOneOrTwo(RubyModule implementationClass, Visibility visibility, int methodIndex) {
            super(implementationClass, visibility, methodIndex);
        }
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name);
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg);
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1);
        
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            switch (args.length) {
            case 0:
                return call(context, self, clazz, name);
            case 1:
                return call(context, self, clazz, name, args[0]);
            case 2:
                return call(context, self, clazz, name, args[0], args[1]);
            default:
                Arity.raiseArgumentError(context.getRuntime(), args.length, 0, 2);
                return null; // never reached
            }
        }
    }
    
    public static abstract class JavaMethodZeroBlock extends JavaMethod {
        public JavaMethodZeroBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZeroBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, StaticScope staticScope, Arity arity) {
            super(implementationClass, visibility, callConfig, staticScope, arity);
        }
        public JavaMethodZeroBlock(RubyModule implementationClass, Visibility visibility, int methodIndex) {
            super(implementationClass, visibility, methodIndex);
        }
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block);
        
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            if (args.length != 0) throw context.getRuntime().newArgumentError(args.length, 0);
            return call(context, self, clazz, name, block);
        }
    }
    
    public static abstract class JavaMethodOne extends JavaMethod {
        public JavaMethodOne(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodOne(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, StaticScope staticScope, Arity arity) {
            super(implementationClass, visibility, callConfig, staticScope, arity);
        }
        public JavaMethodOne(RubyModule implementationClass, Visibility visibility, int methodIndex) {
            super(implementationClass, visibility, methodIndex);
        }
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg);
        
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            if (args.length != 1) throw context.getRuntime().newArgumentError(args.length, 1);
            return call(context, self, clazz, name, args[0]);
        }
    }
    
    public static abstract class JavaMethodOneOrTwo extends JavaMethod {
        public JavaMethodOneOrTwo(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodOneOrTwo(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, StaticScope staticScope, Arity arity) {
            super(implementationClass, visibility, callConfig, staticScope, arity);
        }
        public JavaMethodOneOrTwo(RubyModule implementationClass, Visibility visibility, int methodIndex) {
            super(implementationClass, visibility, methodIndex);
        }
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg);
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg1, IRubyObject arg2);
        
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            switch (args.length) {
            case 1:
                return call(context, self, clazz, name, args[0]);
            case 2:
                return call(context, self, clazz, name, args[0], args[1]);
            default:
                Arity.raiseArgumentError(context.getRuntime(), args.length, 1, 2);
                return null; // never reached
            }
        }
    }
    
    public static abstract class JavaMethodOneOrTwoOrThree extends JavaMethod {
        public JavaMethodOneOrTwoOrThree(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodOneOrTwoOrThree(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, StaticScope staticScope, Arity arity) {
            super(implementationClass, visibility, callConfig, staticScope, arity);
        }
        public JavaMethodOneOrTwoOrThree(RubyModule implementationClass, Visibility visibility, int methodIndex) {
            super(implementationClass, visibility, methodIndex);
        }
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg);
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg1, IRubyObject arg2);
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3);
        
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            switch (args.length) {
            case 1:
                return call(context, self, clazz, name, args[0]);
            case 2:
                return call(context, self, clazz, name, args[0], args[1]);
            case 3:
                return call(context, self, clazz, name, args[0], args[1], args[2]);
            default:
                Arity.raiseArgumentError(context.getRuntime(), args.length, 1, 3);
                return null; // never reached
            }
        }
    }
    
    public static abstract class JavaMethodOneBlock extends JavaMethod {
        public JavaMethodOneBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodOneBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, StaticScope staticScope, Arity arity) {
            super(implementationClass, visibility, callConfig, staticScope, arity);
        }
        public JavaMethodOneBlock(RubyModule implementationClass, Visibility visibility, int methodIndex) {
            super(implementationClass, visibility, methodIndex);
        }
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg, Block block);
        
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            if (args.length != 1) throw context.getRuntime().newArgumentError(args.length, 1);
            return call(context, self, clazz, name, args[0], block);
        }
    }
    
    public static abstract class JavaMethodTwo extends JavaMethod {
        public JavaMethodTwo(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodTwo(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, StaticScope staticScope, Arity arity) {
            super(implementationClass, visibility, callConfig, staticScope, arity);
        }
        public JavaMethodTwo(RubyModule implementationClass, Visibility visibility, int methodIndex) {
            super(implementationClass, visibility, methodIndex);
        }
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg1, IRubyObject arg2);
        
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            if (args.length != 2) throw context.getRuntime().newArgumentError(args.length, 2);
            return call(context, self, clazz, name, args[0], args[1]);
        }
    }
    
    public static abstract class JavaMethodTwoOrThree extends JavaMethod {
        public JavaMethodTwoOrThree(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodTwoOrThree(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, StaticScope staticScope, Arity arity) {
            super(implementationClass, visibility, callConfig, staticScope, arity);
        }
        public JavaMethodTwoOrThree(RubyModule implementationClass, Visibility visibility, int methodIndex) {
            super(implementationClass, visibility, methodIndex);
        }
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg1, IRubyObject arg2);
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3);
        
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            switch (args.length) {
            case 2:
                return call(context, self, clazz, name, args[0], args[1]);
            case 3:
                return call(context, self, clazz, name, args[0], args[1], args[2]);
            default:
                Arity.raiseArgumentError(context.getRuntime(), args.length, 2, 3);
                return null; // never reached
            }
        }
    }
    
    public static abstract class JavaMethodTwoBlock extends JavaMethod {
        public JavaMethodTwoBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodTwoBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, StaticScope staticScope, Arity arity) {
            super(implementationClass, visibility, callConfig, staticScope, arity);
        }
        public JavaMethodTwoBlock(RubyModule implementationClass, Visibility visibility, int methodIndex) {
            super(implementationClass, visibility, methodIndex);
        }
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg1, IRubyObject arg2, Block block);
        
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            if (args.length != 2) throw context.getRuntime().newArgumentError(args.length, 2);
            return call(context, self, clazz, name, args[0], args[1], block);
        }
    }
    
    public static abstract class JavaMethodThree extends JavaMethod {
        public JavaMethodThree(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodThree(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, StaticScope staticScope, Arity arity) {
            super(implementationClass, visibility, callConfig, staticScope, arity);
        }
        public JavaMethodThree(RubyModule implementationClass, Visibility visibility, int methodIndex) {
            super(implementationClass, visibility, methodIndex);
        }
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3);
        
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            if (args.length != 3) throw context.getRuntime().newArgumentError(args.length, 3);
            return call(context, self, clazz, name, args[0], args[1], args[2]);
        }
    }
    
    public static abstract class JavaMethodThreeBlock extends JavaMethod {
        public JavaMethodThreeBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodThreeBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, StaticScope staticScope, Arity arity) {
            super(implementationClass, visibility, callConfig, staticScope, arity);
        }
        public JavaMethodThreeBlock(RubyModule implementationClass, Visibility visibility, int methodIndex) {
            super(implementationClass, visibility, methodIndex);
        }
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block);
        
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            if (args.length != 3) throw context.getRuntime().newArgumentError(args.length, 3);
            return call(context, self, clazz, name, args[0], args[1], args[2], block);
        }
    }

    public JavaMethod(RubyModule implementationClass, Visibility visibility) {
        super(implementationClass, visibility, CallConfiguration.FRAME_ONLY);
        this.methodIndex = -1;
        this.staticScope = null;
    }

    public JavaMethod(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, StaticScope staticScope, Arity arity) {
        super(implementationClass, visibility, callConfig);
        this.methodIndex = -1;
        this.staticScope = staticScope;
        this.arity = arity;
        this.arityValue = arity.getValue();
    }

    public JavaMethod(RubyModule implementationClass, Visibility visibility, int methodIndex) {
        super(implementationClass, visibility, CallConfiguration.FRAME_ONLY);
        this.methodIndex = methodIndex;
        this.staticScope = null;
    }

    public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block);
    
    public DynamicMethod dup() {
        try {
            JavaMethod msm = (JavaMethod)clone();
            return msm;
        } catch (CloneNotSupportedException cnse) {
            return null;
        }
    }
    
    protected void pre(ThreadContext context, IRubyObject self, String name, Block block) {
        callConfig.pre(context, self, getImplementationClass(), arity, name, block, staticScope, this);
    }
    
    protected void post(ThreadContext context) {
        callConfig.post(context);
    }
    
    protected IRubyObject handleReturnJump(ReturnJump rj) {
        if (rj.getTarget() == this) {
            return (IRubyObject)rj.getValue();
        } else {
            throw rj;
        }
    }
    
    public void setArity(Arity arity) {
        this.arity = arity;
        this.arityValue = arity.getValue();
    }

    public Arity getArity() {
        return arity;
    }
    
    public void setArgumentTypes(Class[] argumentTypes) {
        this.argumentTypes = argumentTypes;
    }
    
    public Class[] getArgumentTypes() {
        return argumentTypes;   
    }
    
    public void setJavaName(String javaName) {
        this.javaName = javaName;
    }
    
    public String getJavaName() {
        return javaName;
    }
    
    public void setSingleton(boolean isSingleton) {
        this.isSingleton = isSingleton;
    }
    
    public boolean isSingleton() {
        return isSingleton;
    }
    
    @Override
    public boolean isNative() {
        return true;
    }
}
