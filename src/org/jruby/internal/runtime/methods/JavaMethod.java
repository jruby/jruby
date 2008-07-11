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
import org.jruby.runtime.EventHook;
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
    protected StaticScope staticScope;
    
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
        
        public Arity getArity() {return Arity.NO_ARGUMENTS;}
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
    
    public static abstract class JavaMethodZeroOrOneBlock extends JavaMethod {
        public JavaMethodZeroOrOneBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZeroOrOneBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, StaticScope staticScope, Arity arity) {
            super(implementationClass, visibility, callConfig, staticScope, arity);
        }
        public JavaMethodZeroOrOneBlock(RubyModule implementationClass, Visibility visibility, int methodIndex) {
            super(implementationClass, visibility, methodIndex);
        }

        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block);

        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg, Block block);

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            switch (args.length) {
            case 0:
                return call(context, self, clazz, name, block);
            case 1:
                return call(context, self, clazz, name, args[0], block);
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
    
    public static abstract class JavaMethodZeroOrOneOrTwoBlock extends JavaMethod {
        public JavaMethodZeroOrOneOrTwoBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZeroOrOneOrTwoBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, StaticScope staticScope, Arity arity) {
            super(implementationClass, visibility, callConfig, staticScope, arity);
        }
        public JavaMethodZeroOrOneOrTwoBlock(RubyModule implementationClass, Visibility visibility, int methodIndex) {
            super(implementationClass, visibility, methodIndex);
        }

        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block);

        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg, Block block);

        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block);

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            switch (args.length) {
            case 0:
                return call(context, self, clazz, name, block);
            case 1:
                return call(context, self, clazz, name, args[0], block);
            case 2:
                return call(context, self, clazz, name, args[0], args[1], block);
            default:
                Arity.raiseArgumentError(context.getRuntime(), args.length, 0, 2);
                return null; // never reached
            }
        }
    }

    public static abstract class JavaMethodZeroOrOneOrTwoOrThree extends JavaMethod {
        public JavaMethodZeroOrOneOrTwoOrThree(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZeroOrOneOrTwoOrThree(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, StaticScope staticScope, Arity arity) {
            super(implementationClass, visibility, callConfig, staticScope, arity);
        }
        public JavaMethodZeroOrOneOrTwoOrThree(RubyModule implementationClass, Visibility visibility, int methodIndex) {
            super(implementationClass, visibility, methodIndex);
        }
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name);
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg);
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1);
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2);
        
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            switch (args.length) {
            case 0:
                return call(context, self, clazz, name);
            case 1:
                return call(context, self, clazz, name, args[0]);
            case 2:
                return call(context, self, clazz, name, args[0], args[1]);
            case 3:
                return call(context, self, clazz, name, args[0], args[1], args[2]);
            default:
                Arity.raiseArgumentError(context.getRuntime(), args.length, 0, 3);
                return null; // never reached
            }
        }
    }
    
    public static abstract class JavaMethodZeroOrOneOrTwoOrThreeBlock extends JavaMethod {
        public JavaMethodZeroOrOneOrTwoOrThreeBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZeroOrOneOrTwoOrThreeBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, StaticScope staticScope, Arity arity) {
            super(implementationClass, visibility, callConfig, staticScope, arity);
        }
        public JavaMethodZeroOrOneOrTwoOrThreeBlock(RubyModule implementationClass, Visibility visibility, int methodIndex) {
            super(implementationClass, visibility, methodIndex);
        }

        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block);

        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg, Block block);

        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block);

        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block);

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            switch (args.length) {
            case 0:
                return call(context, self, clazz, name, block);
            case 1:
                return call(context, self, clazz, name, args[0], block);
            case 2:
                return call(context, self, clazz, name, args[0], args[1], block);
            case 3:
                return call(context, self, clazz, name, args[0], args[1], args[2], block);
            default:
                Arity.raiseArgumentError(context.getRuntime(), args.length, 0, 3);
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
        
        public Arity getArity() {return Arity.NO_ARGUMENTS;}
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
        
        public Arity getArity() {return Arity.ONE_ARGUMENT;}
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
        
        public Arity getArity() {return Arity.ONE_ARGUMENT;}
    }
    
    public static abstract class JavaMethodOneOrTwoBlock extends JavaMethod {
        public JavaMethodOneOrTwoBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodOneOrTwoBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, StaticScope staticScope, Arity arity) {
            super(implementationClass, visibility, callConfig, staticScope, arity);
        }
        public JavaMethodOneOrTwoBlock(RubyModule implementationClass, Visibility visibility, int methodIndex) {
            super(implementationClass, visibility, methodIndex);
        }
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block);
        
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block);
        
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            switch (args.length) {
            case 1:
                return call(context, self, clazz, name, args[0], block);
            case 2:
                return call(context, self, clazz, name, args[0], args[1], block);
            default:
                Arity.raiseArgumentError(context.getRuntime(), args.length, 1, 2);
                return null; // never reached
            }
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
        
        public Arity getArity() {return Arity.TWO_ARGUMENTS;}
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
        
        public Arity getArity() {return Arity.TWO_ARGUMENTS;}
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
        
        public Arity getArity() {return Arity.THREE_ARGUMENTS;}
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
        
        public Arity getArity() {return Arity.THREE_ARGUMENTS;}
    }

    public JavaMethod(RubyModule implementationClass, Visibility visibility) {
        super(implementationClass, visibility, CallConfiguration.FRAME_ONLY);
        this.staticScope = null;
    }

    public JavaMethod(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, StaticScope staticScope, Arity arity) {
        super(implementationClass, visibility, callConfig);
        this.staticScope = staticScope;
        this.arity = arity;
        this.arityValue = arity.getValue();
    }

    public JavaMethod(RubyModule implementationClass, Visibility visibility, int methodIndex) {
        super(implementationClass, visibility, CallConfiguration.FRAME_ONLY);
        this.staticScope = null;
    }
    
    protected JavaMethod() {}
    
    public void init(RubyModule implementationClass, Arity arity, Visibility visibility, StaticScope staticScope, CallConfiguration callConfig) {
        this.staticScope = staticScope;
        this.arity = arity;
        this.arityValue = arity.getValue();
        super.init(implementationClass, visibility, callConfig);
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
    
    protected final void preFrameAndScope(ThreadContext context, IRubyObject self, String name, Block block) {
        context.preMethodFrameAndScope(implementationClass, name, self, block, staticScope, this);
    }
    
    protected final void preFrameOnly(ThreadContext context, IRubyObject self, String name, Block block) {
        context.preMethodFrameOnly(implementationClass, name, self, block, this);
    }
    
    protected final void preScopeOnly(ThreadContext context) {
        context.preMethodScopeOnly(implementationClass, staticScope);
    }
    
    protected final void preBacktraceOnly(ThreadContext context, String name) {
        context.preMethodBacktraceOnly(name);
    }
    
    protected final void preBacktraceAndScope(ThreadContext context, String name) {
        context.preMethodBacktraceAndScope(name, implementationClass, staticScope);
    }
    
    protected final void postFrameAndScope(ThreadContext context) {
        context.postMethodFrameAndScope();
    }
    
    protected final void postFrameOnly(ThreadContext context) {
        context.postMethodFrameOnly();
    }
    
    protected final void postScopeOnly(ThreadContext context) {
        context.postMethodScopeOnly();
    }
    
    protected final void postBacktraceOnly(ThreadContext context) {
        context.postMethodBacktraceOnly();
    }
    
    protected final void postBacktraceAndScope(ThreadContext context) {
        context.postMethodBacktraceAndScope();
    }
    
    protected final void callTrace(ThreadContext context, String name) {
        context.trace(EventHook.RUBY_EVENT_C_CALL, name, getImplementationClass());
    }
    
    protected final void returnTrace(ThreadContext context, String name) {
        context.trace(EventHook.RUBY_EVENT_C_CALL, name, getImplementationClass());
    }
    
    public void setArity(Arity arity) {
        this.arity = arity;
        this.arityValue = arity.getValue();
    }

    public Arity getArity() {
        return arity;
    }
    
    @Deprecated
    public void setArgumentTypes(Class[] argumentTypes) {
        this.argumentTypes = argumentTypes;
    }
    
    @Deprecated
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
