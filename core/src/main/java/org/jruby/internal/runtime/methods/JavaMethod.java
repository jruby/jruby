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
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 */
public abstract class JavaMethod extends DynamicMethod implements Cloneable, MethodArgs2 {
    protected int arityValue;
    protected Arity arity = Arity.OPTIONAL;
    private String javaName;
    private boolean isSingleton;
    protected StaticScope staticScope;
    private String parameterDesc;
    private String[] parameterList;
    private CallConfiguration callerRequirement = CallConfiguration.FrameNoneScopeNone;

    private static String[] ONE_REQ = new String[] { "q" };
    private static String[] TWO_REQ = new String[] { "q", "q" };
    private static String[] THREE_REQ = new String[] { "q", "q", "q" };

    public static final Class[][] METHODS = {
        {JavaMethodZero.class, JavaMethodZeroOrOne.class, JavaMethodZeroOrOneOrTwo.class, JavaMethodZeroOrOneOrTwoOrThree.class},
        {null, JavaMethodOne.class, JavaMethodOneOrTwo.class, JavaMethodOneOrTwoOrThree.class},
        {null, null, JavaMethodTwo.class, JavaMethodTwoOrThree.class},
        {null, null, null, JavaMethodThree.class},
    };

    public static final Class[][] REST_METHODS = {
        {JavaMethodZeroOrN.class, JavaMethodZeroOrOneOrN.class, JavaMethodZeroOrOneOrTwoOrN.class, JavaMethodZeroOrOneOrTwoOrThreeOrN.class},
        {null, JavaMethodOneOrN.class, JavaMethodOneOrTwoOrN.class, JavaMethodOneOrTwoOrThreeOrN.class},
        {null, null, JavaMethodTwoOrN.class, JavaMethodTwoOrThreeOrN.class},
        {null, null, null, JavaMethodThreeOrN.class},
    };

    public static final Class[][] BLOCK_METHODS = {
        {JavaMethodZeroBlock.class, JavaMethodZeroOrOneBlock.class, JavaMethodZeroOrOneOrTwoBlock.class, JavaMethodZeroOrOneOrTwoOrThreeBlock.class},
        {null, JavaMethodOneBlock.class, JavaMethodOneOrTwoBlock.class, JavaMethodOneOrTwoOrThreeBlock.class},
        {null, null, JavaMethodTwoBlock.class, JavaMethodTwoOrThreeBlock.class},
        {null, null, null, JavaMethodThreeBlock.class},
    };

    public static final Class[][] BLOCK_REST_METHODS = {
        {JavaMethodZeroOrNBlock.class, JavaMethodZeroOrOneOrNBlock.class, JavaMethodZeroOrOneOrTwoOrNBlock.class, JavaMethodZeroOrOneOrTwoOrThreeOrNBlock.class},
        {null, JavaMethodOneOrNBlock.class, JavaMethodOneOrTwoOrNBlock.class, JavaMethodOneOrTwoOrThreeOrNBlock.class},
        {null, null, JavaMethodTwoOrNBlock.class, JavaMethodTwoOrThreeOrNBlock.class},
        {null, null, null, JavaMethodThreeOrNBlock.class},
    };

    
    public JavaMethod(RubyModule implementationClass, Visibility visibility) {
        this(implementationClass, visibility, CallConfiguration.FrameFullScopeNone);
    }

    public JavaMethod(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
        super(implementationClass, visibility, callConfig);
    }
    
    public JavaMethod(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, String name) {
        super(implementationClass, visibility, callConfig, name);
    }
    
    protected JavaMethod() {}
    
    public void init(RubyModule implementationClass, Arity arity, Visibility visibility, StaticScope staticScope, CallConfiguration callConfig) {
        this.staticScope = staticScope;
        this.arity = arity;
        this.arityValue = arity.getValue();
        super.init(implementationClass, visibility, callConfig);
    }
    
    public DynamicMethod dup() {
        try {
            JavaMethod msm = (JavaMethod)clone();
            return msm;
        } catch (CloneNotSupportedException cnse) {
            return null;
        }
    }

    protected final void preFrameAndScope(ThreadContext context, IRubyObject self, String name, Block block) {
        context.preMethodFrameAndScope(implementationClass, name, self, block, staticScope);
    }

    protected final void preFrameAndDummyScope(ThreadContext context, IRubyObject self, String name, Block block) {
        context.preMethodFrameAndDummyScope(implementationClass, name, self, block, staticScope);
    }

    protected final void preFrameOnly(ThreadContext context, IRubyObject self, String name, Block block) {
        context.preMethodFrameOnly(implementationClass, name, self, block);
    }

    protected final void preScopeOnly(ThreadContext context) {
        context.preMethodScopeOnly(staticScope);
    }

    protected final void preNoFrameDummyScope(ThreadContext context) {
        context.preMethodNoFrameAndDummyScope(staticScope);
    }

    protected final void preBacktraceOnly(ThreadContext context, String name) {
        context.preMethodBacktraceOnly(name);
    }

    protected final void preBacktraceDummyScope(ThreadContext context, String name) {
        context.preMethodBacktraceDummyScope(name, staticScope);
    }

    protected final void preBacktraceAndScope(ThreadContext context, String name) {
        context.preMethodBacktraceAndScope(name, staticScope);
    }

    protected final void preNoop() {}

    protected final static void postFrameAndScope(ThreadContext context) {
        context.postMethodFrameAndScope();
    }

    protected final static void postFrameOnly(ThreadContext context) {
        context.postMethodFrameOnly();
    }

    protected final static void postScopeOnly(ThreadContext context) {
        context.postMethodScopeOnly();
    }

    protected final static void postNoFrameDummyScope(ThreadContext context) {
        context.postMethodScopeOnly();
    }

    protected final static void postBacktraceOnly(ThreadContext context) {
        context.postMethodBacktraceOnly();
    }

    protected final static void postBacktraceDummyScope(ThreadContext context) {
        context.postMethodBacktraceDummyScope();
    }

    protected final static void postBacktraceAndScope(ThreadContext context) {
        context.postMethodBacktraceAndScope();
    }

    protected final static void postNoop(ThreadContext context) {}

    protected final void callTrace(ThreadContext context, boolean enabled, String name) {
        if (enabled) context.trace(RubyEvent.C_CALL, name, getImplementationClass());
    }

    protected final void returnTrace(ThreadContext context, boolean enabled, String name) {
        if (enabled) context.trace(RubyEvent.C_RETURN, name, getImplementationClass());
    }

    protected final void callTraceCompiled(ThreadContext context, boolean enabled, String name, String file, int line) {
        if (enabled) context.trace(RubyEvent.CALL, name, getImplementationClass(), file, line);
    }

    protected final void returnTraceCompiled(ThreadContext context, boolean enabled, String name) {
        if (enabled) context.trace(RubyEvent.RETURN, name, getImplementationClass());
    }
    
    public void setArity(Arity arity) {
        this.arity = arity;
        this.arityValue = arity.getValue();
    }

    @Override
    public Arity getArity() {
        return arity;
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

    public StaticScope getStaticScope() {
        return staticScope;
    }
    
    public void setParameterDesc(String parameterDesc) {
        this.parameterDesc = parameterDesc;
        this.parameterList = null;
    }
    
    public void setParameterList(String[] parameterList) {
        this.parameterDesc = null;
        this.parameterList = parameterList;
    }

    public String[] getParameterList() {
        if (parameterList == null) {
            if (parameterDesc != null && parameterDesc.length() > 0) {
                parameterList = parameterDesc.split(";");
            } else {
                parameterList = new String[0];
            }
        }
        
        return parameterList;
    }

    /**
     * Return a CallConfiguration representing what *callers* to this method must prepare (scope/frame).
     */
    public CallConfiguration getCallerRequirement() {
        return callerRequirement;
    }

    /**
     * Set a CallConfiguration indicating what callers to this method must prepare (scope/frame).
     */
    public void setCallerRequirement(CallConfiguration callerRequirement) {
        this.callerRequirement = callerRequirement;
    }

    protected static IRubyObject raiseArgumentError(JavaMethod method, ThreadContext context, String name, int given, int min, int max) {
        Arity.raiseArgumentError(context.runtime, name, given, min, max);
        // never reached
        return context.runtime.getNil();
    }

    protected static void checkArgumentCount(JavaMethod method, ThreadContext context, String name, IRubyObject[] args, int num) {
        if (args.length != num) raiseArgumentError(method, context, name, args.length, num, num);
    }

    // promise to implement N with block
    public static abstract class JavaMethodNBlock extends JavaMethod {
        public JavaMethodNBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }
        public JavaMethodNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, String name) {
            super(implementationClass, visibility, callConfig, name);
        }
    }


    // promise to implement zero to N with block
    public static abstract class JavaMethodZeroOrNBlock extends JavaMethodNBlock {
        public JavaMethodZeroOrNBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZeroOrNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            return call(context, self, clazz, name, Block.NULL_BLOCK);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block);
    }

    public static abstract class JavaMethodZeroOrOneOrNBlock extends JavaMethodZeroOrNBlock {
        public JavaMethodZeroOrOneOrNBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZeroOrOneOrNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
            return call(context, self, clazz, name, arg0, Block.NULL_BLOCK);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg, Block block);
    }

    public static abstract class JavaMethodZeroOrOneOrTwoOrNBlock extends JavaMethodZeroOrOneOrNBlock {
        public JavaMethodZeroOrOneOrTwoOrNBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZeroOrOneOrTwoOrNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
            return call(context, self, clazz, name, arg0, arg1, Block.NULL_BLOCK);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block);
    }

    public static abstract class JavaMethodZeroOrOneOrTwoOrThreeOrNBlock extends JavaMethodZeroOrOneOrTwoOrNBlock {
        public JavaMethodZeroOrOneOrTwoOrThreeOrNBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZeroOrOneOrTwoOrThreeOrNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
            return call(context, self, clazz, name, arg0, arg1, arg2, Block.NULL_BLOCK);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block);
    }


    // promise to implement one to N with block
    public static abstract class JavaMethodOneOrNBlock extends JavaMethodNBlock {
        public JavaMethodOneOrNBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodOneOrNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
            return call(context, self, clazz, name, arg0, Block.NULL_BLOCK);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg, Block block);
    }

    public static abstract class JavaMethodOneOrTwoOrNBlock extends JavaMethodOneOrNBlock {
        public JavaMethodOneOrTwoOrNBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodOneOrTwoOrNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
            return call(context, self, clazz, name, arg0, arg1, Block.NULL_BLOCK);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block);
    }

    public static abstract class JavaMethodOneOrTwoOrThreeOrNBlock extends JavaMethodOneOrTwoOrNBlock {
        public JavaMethodOneOrTwoOrThreeOrNBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodOneOrTwoOrThreeOrNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
            return call(context, self, clazz, name, arg0, arg1, arg2, Block.NULL_BLOCK);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block);
    }


    // promise to implement two to N with block
    public static abstract class JavaMethodTwoOrNBlock extends JavaMethodNBlock {
        public JavaMethodTwoOrNBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodTwoOrNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
            return call(context, self, clazz, name, arg0, arg1, Block.NULL_BLOCK);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block);
    }

    public static abstract class JavaMethodTwoOrThreeOrNBlock extends JavaMethodTwoOrNBlock {
        public JavaMethodTwoOrThreeOrNBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodTwoOrThreeOrNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
            return call(context, self, clazz, name, arg0, arg1, arg2, Block.NULL_BLOCK);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block);
    }


    // promise to implement three to N with block
    public static abstract class JavaMethodThreeOrNBlock extends JavaMethodNBlock {
        public JavaMethodThreeOrNBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodThreeOrNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
            return call(context, self, clazz, name, arg0, arg1, arg2, Block.NULL_BLOCK);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block);
    }


    // promise to implement zero to three with block
    public static abstract class JavaMethodZeroBlock extends JavaMethodZeroOrNBlock {
        public JavaMethodZeroBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZeroBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            if (args.length != 0) return raiseArgumentError(this, context, name, args.length, 0, 0);
            return call(context, self, clazz, name, block);
        }
    }

    public static abstract class JavaMethodZeroOrOneBlock extends JavaMethodZeroOrOneOrNBlock {
        public JavaMethodZeroOrOneBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZeroOrOneBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            switch (args.length) {
            case 0:
                return call(context, self, clazz, name, block);
            case 1:
                return call(context, self, clazz, name, args[0], block);
            default:
                return raiseArgumentError(this, context, name, args.length, 0, 1);
            }
        }
    }

    public static abstract class JavaMethodZeroOrOneOrTwoBlock extends JavaMethodZeroOrOneOrTwoOrNBlock {
        public JavaMethodZeroOrOneOrTwoBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZeroOrOneOrTwoBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            switch (args.length) {
            case 0:
                return call(context, self, clazz, name, block);
            case 1:
                return call(context, self, clazz, name, args[0], block);
            case 2:
                return call(context, self, clazz, name, args[0], args[1], block);
            default:
                return raiseArgumentError(this, context, name, args.length, 0, 2);
            }
        }
    }

    public static abstract class JavaMethodZeroOrOneOrTwoOrThreeBlock extends JavaMethodZeroOrOneOrTwoOrThreeOrNBlock {
        public JavaMethodZeroOrOneOrTwoOrThreeBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZeroOrOneOrTwoOrThreeBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

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
                return raiseArgumentError(this, context, name, args.length, 0, 3);
            }
        }
    }

    // promise to implement one to three with block
    public static abstract class JavaMethodOneBlock extends JavaMethodOneOrNBlock {
        public JavaMethodOneBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodOneBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            if (args.length != 1) return raiseArgumentError(this, context, name, args.length, 1, 1);
            return call(context, self, clazz, name, args[0], block);
        }

        @Override
        public Arity getArity() {
            return Arity.ONE_ARGUMENT;
        }
    }

    public static abstract class JavaMethodOneOrTwoBlock extends JavaMethodOneOrTwoOrNBlock {
        public JavaMethodOneOrTwoBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodOneOrTwoBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            switch (args.length) {
            case 1:
                return call(context, self, clazz, name, args[0], block);
            case 2:
                return call(context, self, clazz, name, args[0], args[1], block);
            default:
                return raiseArgumentError(this, context, name, args.length, 1, 2);
            }
        }
    }

    public static abstract class JavaMethodOneOrTwoOrThreeBlock extends JavaMethodOneOrTwoOrThreeOrNBlock {
        public JavaMethodOneOrTwoOrThreeBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodOneOrTwoOrThreeBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            switch (args.length) {
            case 0:
                throw context.runtime.newArgumentError(0, 1);
            case 1: return call(context, self, clazz, name, args[0], block);
            case 2: return call(context, self, clazz, name, args[0], args[1], block);
            case 3: return call(context, self, clazz, name, args[0], args[1], args[2], block);
            default: return raiseArgumentError(this, context, name, args.length, 3, 3);
            }
        }
    }


    // promise to implement two to three with block
    public static abstract class JavaMethodTwoBlock extends JavaMethodTwoOrNBlock {
        public JavaMethodTwoBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodTwoBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            if (args.length != 2) return raiseArgumentError(this, context, name, args.length, 2, 2);
            return call(context, self, clazz, name, args[0], args[1], block);
        }
    }

    public static abstract class JavaMethodTwoOrThreeBlock extends JavaMethodTwoOrThreeOrNBlock {
        public JavaMethodTwoOrThreeBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodTwoOrThreeBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            switch (args.length) {
            case 2:
                return call(context, self, clazz, name, args[0], args[1], block);
            case 3:
                return call(context, self, clazz, name, args[0], args[1], args[2], block);
            default:
                return raiseArgumentError(this, context, name, args.length, 2, 3);
            }
        }
    }


    // promise to implement three with block
    public static abstract class JavaMethodThreeBlock extends JavaMethodThreeOrNBlock {
        public JavaMethodThreeBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodThreeBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            if (args.length != 3) return raiseArgumentError(this, context, name, args.length, 3, 3);
            return call(context, self, clazz, name, args[0], args[1], args[2], block);
        }
    }

    // promise to implement N
    public static abstract class JavaMethodN extends JavaMethodNBlock {
        public JavaMethodN(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }
        public JavaMethodN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, String name) {
            super(implementationClass, visibility, callConfig, name);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args);

        // Normally we could leave these to fall back on the superclass, but
        // since it dispatches through the [] version below, which may
        // dispatch through the []+block version, we can save it a couple hops
        // by overriding these here.
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
            return call(context, self, clazz, name, IRubyObject.NULL_ARRAY);
        }
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
            return call(context, self, clazz, name, new IRubyObject[] {arg0});
        }
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
            return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1});
        }
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1, arg2});
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            return call(context, self, clazz, name, args);
        }
    }


    // promise to implement zero to N
    public static abstract class JavaMethodZeroOrN extends JavaMethodN {
        public JavaMethodZeroOrN(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZeroOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }
        public JavaMethodZeroOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, String name) {
            super(implementationClass, visibility, callConfig, name);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
            return call(context, self, clazz, name);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name);
    }

    public static abstract class JavaMethodZeroOrOneOrN extends JavaMethodZeroOrN {
        public JavaMethodZeroOrOneOrN(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZeroOrOneOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
            return call(context, self, clazz, name, arg0);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg);
    }

    public static abstract class JavaMethodZeroOrOneOrTwoOrN extends JavaMethodZeroOrOneOrN {
        public JavaMethodZeroOrOneOrTwoOrN(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZeroOrOneOrTwoOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
            return call(context, self, clazz, name, arg0, arg1);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1);
    }

    public static abstract class JavaMethodZeroOrOneOrTwoOrThreeOrN extends JavaMethodZeroOrOneOrTwoOrN {
        public JavaMethodZeroOrOneOrTwoOrThreeOrN(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZeroOrOneOrTwoOrThreeOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            return call(context, self, clazz, name, arg0, arg1, arg2);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2);
    }


    // promise to implement one to N
    public static abstract class JavaMethodOneOrN extends JavaMethodN {
        public JavaMethodOneOrN(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodOneOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }
        public JavaMethodOneOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, String name) {
            super(implementationClass, visibility, callConfig, name);
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
            return call(context, self, clazz, name, arg0);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0);
    }

    public static abstract class JavaMethodOneOrTwoOrN extends JavaMethodOneOrN {
        public JavaMethodOneOrTwoOrN(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodOneOrTwoOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
            return call(context, self, clazz, name, arg0, arg1);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1);
    }

    public static abstract class JavaMethodOneOrTwoOrThreeOrN extends JavaMethodOneOrTwoOrN {
        public JavaMethodOneOrTwoOrThreeOrN(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodOneOrTwoOrThreeOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            return call(context, self, clazz, name, arg0, arg1, arg2);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2);
    }


    // promise to implement two to N
    public static abstract class JavaMethodTwoOrN extends JavaMethodN {
        public JavaMethodTwoOrN(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodTwoOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
            return call(context, self, clazz, name, arg0, arg1);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1);
    }

    public static abstract class JavaMethodTwoOrThreeOrN extends JavaMethodTwoOrN {
        public JavaMethodTwoOrThreeOrN(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodTwoOrThreeOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            return call(context, self, clazz, name, arg0, arg1, arg2);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2);
    }


    // promise to implement three to N
    public static abstract class JavaMethodThreeOrN extends JavaMethodN {
        public JavaMethodThreeOrN(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodThreeOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            return call(context, self, clazz, name, arg0, arg1, arg2);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2);
    }


    // promise to implement zero to three
    public static abstract class JavaMethodZero extends JavaMethodZeroOrN {
        public JavaMethodZero(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZero(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }
        public JavaMethodZero(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, String name) {
            super(implementationClass, visibility, callConfig, name);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            if (args.length != 0) return raiseArgumentError(this, context, name, args.length, 0, 0);
            return call(context, self, clazz, name);
        }
        @Override
        public Arity getArity() {
            return Arity.NO_ARGUMENTS;
        }
    }

    public static abstract class JavaMethodZeroOrOne extends JavaMethodZeroOrOneOrN {
        public JavaMethodZeroOrOne(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZeroOrOne(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            switch (args.length) {
            case 0:
                return call(context, self, clazz, name);
            case 1:
                return call(context, self, clazz, name, args[0]);
            default:
                return raiseArgumentError(this, context, name, args.length, 0, 1);
            }
        }
    }

    public static abstract class JavaMethodZeroOrOneOrTwo extends JavaMethodZeroOrOneOrTwoOrN {
        public JavaMethodZeroOrOneOrTwo(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZeroOrOneOrTwo(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            switch (args.length) {
            case 0:
                return call(context, self, clazz, name);
            case 1:
                return call(context, self, clazz, name, args[0]);
            case 2:
                return call(context, self, clazz, name, args[0], args[1]);
            default:
                return raiseArgumentError(this, context, name, args.length, 0, 2);
            }
        }
    }

    public static abstract class JavaMethodZeroOrOneOrTwoOrThree extends JavaMethodZeroOrOneOrTwoOrThreeOrN {
        public JavaMethodZeroOrOneOrTwoOrThree(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodZeroOrOneOrTwoOrThree(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
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
                return raiseArgumentError(this, context, name, args.length, 0, 3);
            }
        }
    }


    // promise to implement one to three
    public static abstract class JavaMethodOne extends JavaMethodOneOrN {
        public JavaMethodOne(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
            setParameterList(ONE_REQ);
        }
        public JavaMethodOne(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
            setParameterList(ONE_REQ);
        }
        public JavaMethodOne(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, String name) {
            super(implementationClass, visibility, callConfig, name);
            setParameterList(ONE_REQ);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            if (args.length != 1) return raiseArgumentError(this, context, name, args.length, 1, 1);
            return call(context, self, clazz, name, args[0]);
        }

        @Override
        public Arity getArity() {
            return Arity.ONE_ARGUMENT;
        }
    }

    public static abstract class JavaMethodOneOrTwo extends JavaMethodOneOrTwoOrN {
        public JavaMethodOneOrTwo(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodOneOrTwo(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            switch (args.length) {
            case 1:
                return call(context, self, clazz, name, args[0]);
            case 2:
                return call(context, self, clazz, name, args[0], args[1]);
            default:
                return raiseArgumentError(this, context, name, args.length, 1, 2);
            }
        }
    }

    public static abstract class JavaMethodOneOrTwoOrThree extends JavaMethodOneOrTwoOrThreeOrN {
        public JavaMethodOneOrTwoOrThree(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodOneOrTwoOrThree(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            switch (args.length) {
            case 1:
                return call(context, self, clazz, name, args[0]);
            case 2:
                return call(context, self, clazz, name, args[0], args[1]);
            case 3:
                return call(context, self, clazz, name, args[0], args[1], args[2]);
            default:
                return raiseArgumentError(this, context, name, args.length, 1, 3);
            }
        }
    }


    // promise to implement two to three
    public static abstract class JavaMethodTwo extends JavaMethodTwoOrN {
        public JavaMethodTwo(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
            setParameterList(TWO_REQ);
        }
        public JavaMethodTwo(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
            setParameterList(TWO_REQ);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            if (args.length != 2) return raiseArgumentError(this, context, name, args.length, 2, 2);
            return call(context, self, clazz, name, args[0], args[1]);
        }

        @Override
        public Arity getArity() {
            return Arity.TWO_ARGUMENTS;
        }
    }

    public static abstract class JavaMethodTwoOrThree extends JavaMethodTwoOrThreeOrN {
        public JavaMethodTwoOrThree(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        public JavaMethodTwoOrThree(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            switch (args.length) {
            case 2:
                return call(context, self, clazz, name, args[0], args[1]);
            case 3:
                return call(context, self, clazz, name, args[0], args[1], args[2]);
            default:
                return raiseArgumentError(this, context, name, args.length, 2, 3);
            }
        }
    }


    // promise to implement three
    public static abstract class JavaMethodThree extends JavaMethodThreeOrN {
        public JavaMethodThree(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
            setParameterList(THREE_REQ);
        }
        public JavaMethodThree(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility, callConfig);
            setParameterList(THREE_REQ);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            if (args.length != 3) return raiseArgumentError(this, context, name, args.length, 3, 3);
            return call(context, self, clazz, name, args[0], args[1], args[2]);
        }

        @Override
        public Arity getArity() {
            return Arity.THREE_ARGUMENTS;
        }
    }
}
