/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
import java.util.concurrent.Callable;

import org.jruby.RubyModule;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

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
    private Callable<MethodHandle> maker0;
    private Callable<MethodHandle> maker1;
    private Callable<MethodHandle> maker2;
    private Callable<MethodHandle> maker3;
    private Callable<MethodHandle> maker4;
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

    public HandleMethod(
            RubyModule implementationClass,
            Visibility visibility,
            long encodedSignature,
            boolean builtin,
            boolean notImplemented,
            String parameterDesc,
            final int min,
            final int max,
            final Callable<MethodHandle>... makers) {

        super(implementationClass, visibility);
        this.signature = Signature.decode(encodedSignature);
        this.builtin = builtin;
        this.notImplemented = notImplemented;
        this.parameterDesc = parameterDesc;
        this.min = min;
        this.max = max;
        this.maker0 = makers[0];
        this.maker1 = makers[1];
        this.maker2 = makers[2];
        this.maker3 = makers[3];
        this.maker4 = makers[4];
    }

    public HandleMethod(
            RubyModule implementationClass,
            Visibility visibility,
            long encodedSignature,
            boolean builtin,
            boolean notImplemented,
            String parameterDesc,
            final int min,
            final int max,
            final Callable<MethodHandle> maker0,
            final Callable<MethodHandle> maker1,
            final Callable<MethodHandle> maker2,
            final Callable<MethodHandle> maker3,
            final Callable<MethodHandle> maker4) {

        super(implementationClass, visibility);
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

    @Override
    public Arity getArity() {
        return signature.arity();
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
        if (!initialized0) {
            this.target0 = safeCall(maker0);
            initialized0 = true;
            maker0 = null;
        }
        return this.target0;
    }

    private MethodHandle ensureTarget1() {
        if (!initialized1) {
            this.target1 = safeCall(maker1);
            initialized1 = true;
            maker1 = null;
        }
        return this.target1;
    }

    private MethodHandle ensureTarget2() {
        if (!initialized2) {
            this.target2 = safeCall(maker2);
            initialized2 = true;
            maker2 = null;
        }
        return this.target2;
    }

    private MethodHandle ensureTarget3() {
        if (!initialized3) {
            this.target3 = safeCall(maker3);
            initialized3 = true;
            maker3 = null;
        }
        return this.target3;
    }

    private MethodHandle ensureTarget4() {
        if (!initialized4) {
            this.target4 = safeCall(maker4);
            initialized4 = true;
            maker4 = null;
        }
        return this.target4;
    }

    private static MethodHandle safeCall(Callable<MethodHandle> maker) {
        try {
            if (maker == null) return null;
            return maker.call();
        } catch (Exception e) {
            Helpers.throwException(e);
            return null;
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        try {
            MethodHandle target4 = ensureTarget4();
            if (target4 != null) {
                Arity.checkArgumentCount(context, args.length, min, max);
                return (IRubyObject) target4.invokeExact(context, self, clazz, name, args, block);
            } else {
                int arity = Arity.checkArgumentCount(context, args.length, min, max);
                switch (args.length) {
                    case 0: return (IRubyObject) ensureTarget0().invokeExact(context, self, clazz, name, block);
                    case 1: return (IRubyObject) ensureTarget1().invokeExact(context, self, clazz, name, args[0], block);
                    case 2: return (IRubyObject) ensureTarget2().invokeExact(context, self, clazz, name, args[0], args[1], block);
                    case 3: return (IRubyObject) ensureTarget3().invokeExact(context, self, clazz, name, args[0], args[1], args[2], block);
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
        return new HandleMethod(implementationClass, getVisibility(), signature.encode(), builtin, notImplemented, parameterDesc, min, max, maker0, maker1, maker2, maker3, maker4);
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
