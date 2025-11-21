/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2012 The JRuby Community <www.jruby.org>
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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

import com.headius.invokebinder.SmartBinder;
import org.jruby.RubyModule;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.runtime.Helpers.arrayOf;
import static org.jruby.util.StringSupport.EMPTY_STRING_ARRAY;
import static org.jruby.util.StringSupport.split;

/**
 * A DynamicMethod backed by one or more java.lang.invoke.MethodHandle objects.
 * 
 * The MethodHandles contained in this method are used by invokedynamic-based
 * call site binding to bind more directly to the target. Previously, the
 * handles required for binding were built at the call site by inspecting the
 * DynamicMethod that came in. With the newer logic, handles are created at
 * bind time, so they're already ready and so we don't need to generate our
 * own handle classes that might only get used by the interpreter.
 *
 * @author headius
 */
public class HandleMethod extends DynamicMethod implements MethodArgs2, Cloneable {
    private Supplier<MethodHandle> maker0;
    private Supplier<MethodHandle> maker1;
    private Supplier<MethodHandle> maker2;
    private Supplier<MethodHandle> maker3;
    private Supplier<MethodHandle> maker4;
    private MethodHandle target0;
    private MethodHandle target1;
    private MethodHandle target2;
    private MethodHandle target3;
    private MethodHandle target4;
    private volatile boolean initialized0, initialized1, initialized2, initialized3, initialized4;
    private final int min;
    private final int max;
    private final String parameterDesc;
    private final Signature signature;
    private final boolean builtin;
    private final boolean notImplemented;

    private static final com.headius.invokebinder.Signature ARITY_0 =
            com.headius.invokebinder.Signature.from(
                    IRubyObject.class,
                    arrayOf(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, Block.class),
                    "context", "self", "selfType", "name", "block");
    private static final com.headius.invokebinder.Signature ARITY_1 = ARITY_0.insertArg(4, "arg0", IRubyObject.class);
    private static final com.headius.invokebinder.Signature ARITY_2 = ARITY_1.insertArg(5, "arg1", IRubyObject.class);
    private static final com.headius.invokebinder.Signature ARITY_3 = ARITY_2.insertArg(6, "arg2", IRubyObject.class);
    private static final com.headius.invokebinder.Signature[] ARITIES = {ARITY_0, ARITY_1, ARITY_2, ARITY_3};

    public HandleMethod(
            RubyModule implementationClass,
            Visibility visibility,
            String name,
            long encodedSignature,
            boolean builtin,
            boolean notImplemented,
            String parameterDesc,
            final int min,
            final int max,
            final Supplier<MethodHandle> maker0,
            final Supplier<MethodHandle> maker1,
            final Supplier<MethodHandle> maker2,
            final Supplier<MethodHandle> maker3,
            final Supplier<MethodHandle> maker4) {

        super(implementationClass, visibility, name);
        this.signature = Signature.decode(encodedSignature);
        this.builtin = builtin;
        this.notImplemented = notImplemented;
        this.parameterDesc = parameterDesc;
        this.min = min;
        this.max = max;
        this.maker0 = maker0;
        this.maker1 = maker1;
        this.maker2 = maker2;
        this.maker3 = maker3;
        this.maker4 = maker4;
    }

    @Deprecated(since = "9.3.0.0") @Override
    public Arity getArity() {
        return signature.arity();
    }

    @Override
    public Signature getSignature() {
        return signature;
    }

    @Override
    public boolean isBuiltin() {
        return builtin;
    }

    @Override
    public boolean isNotImplemented() {
        return notImplemented;
    }

    @Override
    public boolean isNative() {
        return true;
    }

    private MethodHandle ensureTarget0() {
        MethodHandle target0;
        if (!initialized0) {
            Supplier<MethodHandle> maker0 = this.maker0;
            if (maker0 == null) {
                target0 = adaptSpecificToVarargs(ensureTarget4(), 0);
            } else {
                target0 = maker0.get();
            }
            this.target0 = target0;
            this.maker0 = null;
            initialized0 = true;
        } else {
            target0 = this.target0;
        }
        return target0;
    }

    private MethodHandle ensureTarget1() {
        MethodHandle target1;
        if (!initialized1) {
            Supplier<MethodHandle> maker1 = this.maker1;
            if (maker1 == null) {
                target1 = adaptSpecificToVarargs(ensureTarget4(), 1);
            } else {
                target1 = maker1.get();
            }
            this.target1 = target1;
            this.maker1 = null;
            initialized1 = true;
        } else {
            target1 = this.target1;
        }
        return target1;
    }

    private MethodHandle ensureTarget2() {
        MethodHandle target2;
        if (!initialized2) {
            Supplier<MethodHandle> maker2 = this.maker2;
            if (maker2 == null) {
                target2 = adaptSpecificToVarargs(ensureTarget4(), 2);
            } else {
                target2 = maker2.get();
            }
            this.target2 = target2;
            this.maker2 = null;
            initialized2 = true;
        } else {
            target2 = this.target2;
        }
        return target2;
    }

    private MethodHandle ensureTarget3() {
        MethodHandle target3;
        if (!initialized3) {
            Supplier<MethodHandle> maker3 = this.maker3;
            if (maker3 == null) {
                target3 = adaptSpecificToVarargs(ensureTarget4(), 3);
            } else {
                target3 = maker3.get();
            }
            this.target3 = target3;
            this.maker3 = null;
            initialized3 = true;
        } else {
            target3 = this.target3;
        }
        return target3;
    }

    private MethodHandle ensureTarget4() {
        MethodHandle target4;
        if (!initialized4) {
            Supplier<MethodHandle> maker4 = this.maker4;
            if (maker4 == null) {
                target4 = null;
            } else {
                target4 = maker4.get();
            }
            this.target4 = target4;
            initialized4 = true;
            this.maker4 = null;
        }
        return this.target4;
    }

    private MethodHandle adaptSpecificToVarargs(MethodHandle varargs, int arity) {
        if (arity == 0) {
            return MethodHandles.insertArguments(varargs, 4, new Object[] {IRubyObject.NULL_ARRAY});
        }

        return SmartBinder.from(ARITIES[arity])
                .permute("context", "self", "type", "name", "block", "arg.*")
                .collect("args", "arg.*")
                .permute("context", "self", "type", "name", "args", "block")
                .invoke(varargs).handle();
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        try {
            MethodHandle target4 = ensureTarget4();
            if (target4 != null) {
                return (IRubyObject) target4.invokeExact(context, self, clazz, name, args, block);
            } else {
                int arity = Arity.checkArgumentCount(context, args.length, min, max);
                switch (args.length) {
                    case 0: return call(context, self, clazz, name, block);
                    case 1: return call(context, self, clazz, name, args[0], block);
                    case 2: return call(context, self, clazz, name, args[0], args[1], block);
                    case 3: return call(context, self, clazz, name, args[0], args[1], args[2], block);
                    default:
                        throw new RuntimeException("invalid arity for call: " + arity);
                }
            }
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null;
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        MethodHandle target0 = ensureTarget0();
        if (target0 == null) {
            return call(context, self, clazz, name, IRubyObject.NULL_ARRAY, block);
        }
        try {
            return (IRubyObject) target0.invokeExact(context, self, clazz, name, block);
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null;
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        MethodHandle target1 = ensureTarget1();
        if (target1 == null) {
            return call(context, self, clazz, name, new IRubyObject[]{arg0}, block);
        }
        try {
            return (IRubyObject) target1.invokeExact(context, self, clazz, name, arg0, block);
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null;
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        MethodHandle target2 = ensureTarget2();
        if (target2 == null) {
            return call(context, self, clazz, name, new IRubyObject[]{arg0, arg1}, block);
        }
        try {
            return (IRubyObject) target2.invokeExact(context, self, clazz, name, arg0, arg1, block);
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null;
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        MethodHandle target3 = ensureTarget3();
        if (target3 == null) {
            return call(context, self, clazz, name, new IRubyObject[]{arg0, arg1, arg2}, block);
        }
        try {
            return (IRubyObject) target3.invokeExact(context, self, clazz, name, arg0, arg1, arg2, block);
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null;
        }
    }

    @Override
    public DynamicMethod dup() {
        return new HandleMethod(implementationClass, getVisibility(), name, signature.encode(), builtin, notImplemented, parameterDesc, min, max, maker0, maker1, maker2, maker3, maker4);
    }

    @Override
    public String[] getParameterList() {
        if (parameterDesc != null && parameterDesc.length() > 0) {
            return split(parameterDesc, ';').toArray(EMPTY_STRING_ARRAY);
        }
        return EMPTY_STRING_ARRAY;
    }

    public MethodHandle getHandle(int arity) {
        switch (arity) {
            case -1: return ensureTarget4();
            case 0: return ensureTarget0();
            case 1: return ensureTarget1();
            case 2: return ensureTarget2();
            case 3: return ensureTarget3();
            default: return null;
        }
    }
    
}
