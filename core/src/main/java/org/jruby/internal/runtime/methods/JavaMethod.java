/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
import org.jruby.runtime.Helpers;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.util.StringSupport.EMPTY_STRING_ARRAY;
import static org.jruby.util.StringSupport.split;

public abstract class JavaMethod extends DynamicMethod implements Cloneable, MethodArgs2, NativeCallMethod {
    protected Signature signature = Signature.OPTIONAL;
    private String javaName;
    private boolean isSingleton;
    protected StaticScope staticScope;
    private String parameterDesc;
    private String[] parameterList;

    /** Single-arity native call */
    protected NativeCall nativeCall;

    private static final String[] ONE_REQ = new String[] { "q" };
    private static final String[] TWO_REQ = new String[] { "q", "q" };
    private static final String[] THREE_REQ = new String[] { "q", "q", "q" };
    protected static final String[] REST = new String[] { "r" };

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

    public JavaMethod(RubyModule implementationClass, Visibility visibility, String name) {
        super(implementationClass, visibility, name);
    }

    public DynamicMethod dup() {
        try {
            return (JavaMethod) super.clone();
        }
        catch (CloneNotSupportedException ex) {
            throw new AssertionError(ex);
        }
    }

    protected final void preFrameAndScope(ThreadContext context, IRubyObject self, RubyModule frameClass, String name, Block block) {
        context.preMethodFrameAndScope(frameClass, name, self, block, staticScope);
    }

    protected final void preFrameAndDummyScope(ThreadContext context, IRubyObject self, RubyModule frameClass, String name, Block block) {
        context.preMethodFrameAndDummyScope(frameClass, name, self, block, staticScope);
    }

    protected final void preFrameOnly(ThreadContext context, IRubyObject self, RubyModule frameClass, String name, Block block) {
        context.preMethodFrameOnly(frameClass, name, self, block);
    }

    // Still used by exts like jruby-openssl. Regeneration should pick up new ones above.
    @Deprecated(since = "9.2.7.0")
    protected final void preFrameAndScope(ThreadContext context, IRubyObject self, String name, Block block) {
        context.preMethodFrameAndScope(getImplementationClass(), name, self, block, staticScope);
    }

    // Still used by exts like jruby-openssl. Regeneration should pick up new ones above.
    @Deprecated(since = "9.2.7.0")
    protected final void preFrameAndDummyScope(ThreadContext context, IRubyObject self, String name, Block block) {
        context.preMethodFrameAndDummyScope(getImplementationClass(), name, self, block, staticScope);
    }

    // Still used by exts like jruby-openssl. Regeneration should pick up new ones above.
    @Deprecated(since = "9.2.7.0")
    protected final void preFrameOnly(ThreadContext context, IRubyObject self, String name, Block block) {
        context.preMethodFrameOnly(getImplementationClass(), name, self, block);
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
        if (enabled) context.trace(RubyEvent.C_CALL, Helpers.getSuperNameFromCompositeName(name), getImplementationClass());
    }

    protected final void returnTrace(ThreadContext context, boolean enabled, String name) {
        if (enabled) context.trace(RubyEvent.C_RETURN, Helpers.getSuperNameFromCompositeName(name), getImplementationClass());
    }

    protected final void callTraceCompiled(ThreadContext context, boolean enabled, String name, String file, int line) {
        if (enabled) context.trace(RubyEvent.CALL, Helpers.getSuperNameFromCompositeName(name), getImplementationClass(), file, line);
    }

    protected final void returnTraceCompiled(ThreadContext context, boolean enabled, String name) {
        if (enabled) context.trace(RubyEvent.RETURN, Helpers.getSuperNameFromCompositeName(name), getImplementationClass());
    }

    @Deprecated(since = "9.3.0.0")
    public void setArity(Arity arity) {
        this.signature = Signature.from(arity);
    }

    public void setSignature(Signature signature) {
        this.signature = signature;
    }

    @Deprecated @Override
    public Arity getArity() {
        return getSignature().arity();
    }

    public Signature getSignature() {
        return signature;
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
                parameterList = split(parameterDesc, ';').toArray(EMPTY_STRING_ARRAY);
            }
            else {
                parameterList = EMPTY_STRING_ARRAY;
            }
        }
        return parameterList;
    }

    /**
     * @see NativeCallMethod#setNativeCall(Class, String, Class, Class[], boolean, boolean)
     */
    public void setNativeCall(Class nativeTarget, String nativeName, Class nativeReturn, Class[] nativeSignature, boolean statik, boolean java) {
        this.nativeCall = new NativeCall(nativeTarget, nativeName, nativeReturn, nativeSignature, statik, java);
    }


    /**
     * @see NativeCallMethod#setNativeCall(Class, String, Class, Class[], boolean)
     */
    public void setNativeCall(Class nativeTarget, String nativeName, Class nativeReturn, Class[] nativeSignature, boolean statik) {
        setNativeCall(nativeTarget, nativeName, nativeReturn, nativeSignature, statik, false);
    }

    /**
     * @see NativeCallMethod#getNativeCall()
     */
    public NativeCall getNativeCall() {
        return this.nativeCall;
    }

    protected static IRubyObject raiseArgumentError(JavaMethod method, ThreadContext context, String name, int given, int min, int max) {
        Arity.raiseArgumentError(context, name, given, min, max);
        throw new AssertionError("expected to throw ArgumentError"); // never reached
    }

    protected static void checkArgumentCount(JavaMethod method, ThreadContext context, String name, IRubyObject[] args, int num) {
        if (args.length != num) raiseArgumentError(method, context, name, args.length, num, num);
    }

    // promise to implement N with block
    public static abstract class JavaMethodNBlock extends JavaMethod {
        public JavaMethodNBlock(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodNBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, String name) {
            super(implementationClass, visibility, name);
        }
    }


    // promise to implement zero to N with block
    public static abstract class JavaMethodZeroOrNBlock extends JavaMethodNBlock {
        public JavaMethodZeroOrNBlock(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodZeroOrNBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodZeroOrNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            return call(context, self, clazz, name, Block.NULL_BLOCK);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block);
    }

    public static abstract class JavaMethodZeroOrOneOrNBlock extends JavaMethodZeroOrNBlock {
        public JavaMethodZeroOrOneOrNBlock(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodZeroOrOneOrNBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodZeroOrOneOrNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
            return call(context, self, clazz, name, arg0, Block.NULL_BLOCK);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg, Block block);
    }

    public static abstract class JavaMethodZeroOrOneOrTwoOrNBlock extends JavaMethodZeroOrOneOrNBlock {
        public JavaMethodZeroOrOneOrTwoOrNBlock(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodZeroOrOneOrTwoOrNBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodZeroOrOneOrTwoOrNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
            return call(context, self, clazz, name, arg0, arg1, Block.NULL_BLOCK);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block);
    }

    public static abstract class JavaMethodZeroOrOneOrTwoOrThreeOrNBlock extends JavaMethodZeroOrOneOrTwoOrNBlock {
        public JavaMethodZeroOrOneOrTwoOrThreeOrNBlock(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodZeroOrOneOrTwoOrThreeOrNBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodZeroOrOneOrTwoOrThreeOrNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
            return call(context, self, clazz, name, arg0, arg1, arg2, Block.NULL_BLOCK);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block);
    }


    // promise to implement one to N with block
    public static abstract class JavaMethodOneOrNBlock extends JavaMethodNBlock {
        public JavaMethodOneOrNBlock(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodOneOrNBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodOneOrNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
            return call(context, self, clazz, name, arg0, Block.NULL_BLOCK);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg, Block block);
    }

    public static abstract class JavaMethodOneOrTwoOrNBlock extends JavaMethodOneOrNBlock {
        public JavaMethodOneOrTwoOrNBlock(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodOneOrTwoOrNBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodOneOrTwoOrNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
            return call(context, self, clazz, name, arg0, arg1, Block.NULL_BLOCK);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block);
    }

    public static abstract class JavaMethodOneOrTwoOrThreeOrNBlock extends JavaMethodOneOrTwoOrNBlock {
        public JavaMethodOneOrTwoOrThreeOrNBlock(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodOneOrTwoOrThreeOrNBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodOneOrTwoOrThreeOrNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
            return call(context, self, clazz, name, arg0, arg1, arg2, Block.NULL_BLOCK);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block);
    }


    // promise to implement two to N with block
    public static abstract class JavaMethodTwoOrNBlock extends JavaMethodNBlock {
        public JavaMethodTwoOrNBlock(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodTwoOrNBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodTwoOrNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
            return call(context, self, clazz, name, arg0, arg1, Block.NULL_BLOCK);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block);
    }

    public static abstract class JavaMethodTwoOrThreeOrNBlock extends JavaMethodTwoOrNBlock {
        public JavaMethodTwoOrThreeOrNBlock(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodTwoOrThreeOrNBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodTwoOrThreeOrNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
            return call(context, self, clazz, name, arg0, arg1, arg2, Block.NULL_BLOCK);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block);
    }


    // promise to implement three to N with block
    public static abstract class JavaMethodThreeOrNBlock extends JavaMethodNBlock {
        public JavaMethodThreeOrNBlock(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodThreeOrNBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodThreeOrNBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
            return call(context, self, clazz, name, arg0, arg1, arg2, Block.NULL_BLOCK);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block);
    }


    // promise to implement zero to three with block
    public static abstract class JavaMethodZeroBlock extends JavaMethodZeroOrNBlock {
        public JavaMethodZeroBlock(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodZeroBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodZeroBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            if (args.length != 0) return raiseArgumentError(this, context, name, args.length, 0, 0);
            return call(context, self, clazz, name, block);
        }
    }

    public static abstract class JavaMethodZeroOrOneBlock extends JavaMethodZeroOrOneOrNBlock {
        public JavaMethodZeroOrOneBlock(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodZeroOrOneBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodZeroOrOneBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
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
        public JavaMethodZeroOrOneOrTwoBlock(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodZeroOrOneOrTwoBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodZeroOrOneOrTwoBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
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
        public JavaMethodZeroOrOneOrTwoOrThreeBlock(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodZeroOrOneOrTwoOrThreeBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodZeroOrOneOrTwoOrThreeBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
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
        public JavaMethodOneBlock(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodOneBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodOneBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            if (args.length != 1) return raiseArgumentError(this, context, name, args.length, 1, 1);
            return call(context, self, clazz, name, args[0], block);
        }

        @Deprecated @Override
        public Arity getArity() {
            return Arity.ONE_ARGUMENT;
        }

        public Signature getSignature() {
            return Signature.ONE_ARGUMENT;
        }
    }

    public static abstract class JavaMethodOneOrTwoBlock extends JavaMethodOneOrTwoOrNBlock {
        public JavaMethodOneOrTwoBlock(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodOneOrTwoBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodOneOrTwoBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
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
        public JavaMethodOneOrTwoOrThreeBlock(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodOneOrTwoOrThreeBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodOneOrTwoOrThreeBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            switch (args.length) {
            case 1:
                return call(context, self, clazz, name, args[0], block);
            case 2:
                return call(context, self, clazz, name, args[0], args[1], block);
            case 3:
                return call(context, self, clazz, name, args[0], args[1], args[2], block);
            default:
                return raiseArgumentError(this, context, name, args.length, 1, 3);
            }
        }
    }


    // promise to implement two to three with block
    public static abstract class JavaMethodTwoBlock extends JavaMethodTwoOrNBlock {
        public JavaMethodTwoBlock(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodTwoBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodTwoBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            if (args.length != 2) return raiseArgumentError(this, context, name, args.length, 2, 2);
            return call(context, self, clazz, name, args[0], args[1], block);
        }
    }

    public static abstract class JavaMethodTwoOrThreeBlock extends JavaMethodTwoOrThreeOrNBlock {
        public JavaMethodTwoOrThreeBlock(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodTwoOrThreeBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodTwoOrThreeBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
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
        public JavaMethodThreeBlock(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodThreeBlock(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodThreeBlock(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            if (args.length != 3) return raiseArgumentError(this, context, name, args.length, 3, 3);
            return call(context, self, clazz, name, args[0], args[1], args[2], block);
        }
    }

    // promise to implement N
    public static abstract class JavaMethodN extends JavaMethodNBlock {
        public JavaMethodN(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodN(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, String name) {
            super(implementationClass, visibility, name);
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

        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            switch (args.length) {
                // still delegate to different arity calls as they might get overriden e.g. for native
                // JRuby methods that use overloading for different kind arity of received arguments !
                case 0:
                    return call(context, self, clazz, name);
                case 1:
                    return call(context, self, clazz, name, args[0]);
                case 2:
                    return call(context, self, clazz, name, args[0], args[1]);
                default:
                    return call(context, self, clazz, name, args);
            }
        }

    }


    // promise to implement zero to N
    public static abstract class JavaMethodZeroOrN extends JavaMethodN {
        public JavaMethodZeroOrN(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodZeroOrN(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodZeroOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodZeroOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, String name) {
            super(implementationClass, visibility, name);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
            return call(context, self, clazz, name);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name);
    }

    public static abstract class JavaMethodZeroOrOneOrN extends JavaMethodZeroOrN {
        public JavaMethodZeroOrOneOrN(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodZeroOrOneOrN(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodZeroOrOneOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
            return call(context, self, clazz, name, arg0);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg);
    }

    public static abstract class JavaMethodZeroOrOneOrTwoOrN extends JavaMethodZeroOrOneOrN {
        public JavaMethodZeroOrOneOrTwoOrN(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodZeroOrOneOrTwoOrN(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodZeroOrOneOrTwoOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
            return call(context, self, clazz, name, arg0, arg1);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1);
    }

    public static abstract class JavaMethodZeroOrOneOrTwoOrThreeOrN extends JavaMethodZeroOrOneOrTwoOrN {
        public JavaMethodZeroOrOneOrTwoOrThreeOrN(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodZeroOrOneOrTwoOrThreeOrN(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodZeroOrOneOrTwoOrThreeOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            return call(context, self, clazz, name, arg0, arg1, arg2);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2);
    }


    // promise to implement one to N
    public static abstract class JavaMethodOneOrN extends JavaMethodN {
        public JavaMethodOneOrN(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodOneOrN(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodOneOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodOneOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, String name) {
            super(implementationClass, visibility, name);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
            return call(context, self, clazz, name, arg0);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0);
    }

    public static abstract class JavaMethodOneOrTwoOrN extends JavaMethodOneOrN {
        public JavaMethodOneOrTwoOrN(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodOneOrTwoOrN(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodOneOrTwoOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
            return call(context, self, clazz, name, arg0, arg1);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1);
    }

    public static abstract class JavaMethodOneOrTwoOrThreeOrN extends JavaMethodOneOrTwoOrN {
        public JavaMethodOneOrTwoOrThreeOrN(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodOneOrTwoOrThreeOrN(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodOneOrTwoOrThreeOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            return call(context, self, clazz, name, arg0, arg1, arg2);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2);
    }


    // promise to implement two to N
    public static abstract class JavaMethodTwoOrN extends JavaMethodN {
        public JavaMethodTwoOrN(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodTwoOrN(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodTwoOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
            return call(context, self, clazz, name, arg0, arg1);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1);
    }

    public static abstract class JavaMethodTwoOrThreeOrN extends JavaMethodTwoOrN {
        public JavaMethodTwoOrThreeOrN(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodTwoOrThreeOrN(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodTwoOrThreeOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            return call(context, self, clazz, name, arg0, arg1, arg2);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2);
    }


    // promise to implement three to N
    public static abstract class JavaMethodThreeOrN extends JavaMethodN {
        public JavaMethodThreeOrN(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodThreeOrN(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodThreeOrN(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            return call(context, self, clazz, name, arg0, arg1, arg2);
        }

        @Override
        public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2);
    }


    // promise to implement zero to three
    public static abstract class JavaMethodZero extends JavaMethodZeroOrN {
        public JavaMethodZero(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodZero(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodZero(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodZero(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, String name) {
            super(implementationClass, visibility, name);
        }

        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            if (args.length != 0) return raiseArgumentError(this, context, name, args.length, 0, 0);
            return call(context, self, clazz, name);
        }
        @Deprecated @Override
        public Arity getArity() {
            return Arity.NO_ARGUMENTS;
        }

        public Signature getSignature() {
            return Signature.NO_ARGUMENTS;
        }
    }

    public static abstract class JavaMethodZeroOrOne extends JavaMethodZeroOrOneOrN {
        public JavaMethodZeroOrOne(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodZeroOrOne(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodZeroOrOne(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
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
        public JavaMethodZeroOrOneOrTwo(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodZeroOrOneOrTwo(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodZeroOrOneOrTwo(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        @Override
        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
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
        public JavaMethodZeroOrOneOrTwoOrThree(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodZeroOrOneOrTwoOrThree(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodZeroOrOneOrTwoOrThree(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
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
        public JavaMethodOne(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
            setParameterList(ONE_REQ);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodOne(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
            setParameterList(ONE_REQ);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodOne(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
            setParameterList(ONE_REQ);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodOne(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, String name) {
            super(implementationClass, visibility, name);
            setParameterList(ONE_REQ);
        }

        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            if (args.length != 1) return raiseArgumentError(this, context, name, args.length, 1, 1);
            return call(context, self, clazz, name, args[0]);
        }

        @Deprecated @Override
        public Arity getArity() {
            return Arity.ONE_ARGUMENT;
        }

        public Signature getSignature() {
            return Signature.ONE_ARGUMENT;
        }
    }

    public static abstract class JavaMethodOneOrTwo extends JavaMethodOneOrTwoOrN {
        public JavaMethodOneOrTwo(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodOneOrTwo(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodOneOrTwo(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
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
        public JavaMethodOneOrTwoOrThree(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodOneOrTwoOrThree(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodOneOrTwoOrThree(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
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
        public JavaMethodTwo(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
            setParameterList(TWO_REQ);
        }
        @Deprecated(since = "9.1.16.0")
        public JavaMethodTwo(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
            setParameterList(TWO_REQ);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodTwo(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
            setParameterList(TWO_REQ);
        }

        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            if (args.length != 2) return raiseArgumentError(this, context, name, args.length, 2, 2);
            return call(context, self, clazz, name, args[0], args[1]);
        }

        @Deprecated @Override
        public Arity getArity() {
            return Arity.TWO_ARGUMENTS;
        }

        public Signature getSignature() {
            return Signature.TWO_ARGUMENTS;
        }
    }

    public static abstract class JavaMethodTwoOrThree extends JavaMethodTwoOrThreeOrN {
        public JavaMethodTwoOrThree(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
            setParameterList(TWO_REQ);
        }
        public JavaMethodTwoOrThree(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodTwoOrThree(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
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
        public JavaMethodThree(RubyModule implementationClass, Visibility visibility, String name) {
            super(implementationClass, visibility, name);
            setParameterList(THREE_REQ);
        }
        public JavaMethodThree(RubyModule implementationClass, Visibility visibility) {
            super(implementationClass, visibility);
            setParameterList(THREE_REQ);
        }
        @Deprecated(since = "9.0.1.0")
        public JavaMethodThree(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
            super(implementationClass, visibility);
        }

        public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            if (args.length != 3) return raiseArgumentError(this, context, name, args.length, 3, 3);
            return call(context, self, clazz, name, args[0], args[1], args[2]);
        }

        @Deprecated @Override
        public Arity getArity() {
            return Arity.THREE_ARGUMENTS;
        }

        public Signature getSignature() {
            return Signature.THREE_ARGUMENTS;
        }
    }

    @Deprecated(since = "9.0.1.0")
    public JavaMethod(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
        super(implementationClass, visibility);
    }

    @Deprecated(since = "9.0.1.0")
    public JavaMethod(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, String name) {
        super(implementationClass, visibility, name);
    }

    @Deprecated(since = "9.0.1.0")
    public CallConfiguration getCallerRequirement() {
        return CallConfiguration.FrameNoneScopeNone;
    }

    @Deprecated(since = "9.0.1.0")
    public void setCallerRequirement(CallConfiguration callerRequirement) {
    }

    /**
     * Used for old-style nameless constructor to pass name in out-of-band.
     */
    @Deprecated(since = "9.1.16.0")
    public static final ThreadLocal<String> NAME_PASSER = new ThreadLocal<>();

    @Deprecated(since = "9.1.16.0")
    public JavaMethod(RubyModule implementationClass, Visibility visibility) {
        this(implementationClass, visibility, NAME_PASSER.get());
    }
}
