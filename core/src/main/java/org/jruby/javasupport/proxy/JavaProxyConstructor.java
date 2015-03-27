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
 * Copyright (C) 2006 Kresten Krab Thorup <krab@gnu.org>
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

package org.jruby.javasupport.proxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyProc;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.ParameterTypes;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.javasupport.JavaCallable.inspectParameterTypes;

@JRubyClass(name="Java::JavaProxyConstructor")
public class JavaProxyConstructor extends JavaProxyReflectionObject implements ParameterTypes {

    private final Constructor<?> proxyConstructor;
    private final Class<?>[] actualParameterTypes;
    private final boolean actualVarArgs;

    private final JavaProxyClass declaringProxyClass;

    public static RubyClass createJavaProxyConstructorClass(Ruby runtime, RubyModule Java) {
        RubyClass JavaProxyConstructor = Java.defineClassUnder(
                "JavaProxyConstructor",
                runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR
        );

        JavaProxyReflectionObject.registerRubyMethods(runtime, JavaProxyConstructor);
        JavaProxyConstructor.defineAnnotatedMethods(JavaProxyConstructor.class);
        return JavaProxyConstructor;

    }

    JavaProxyConstructor(Ruby runtime, JavaProxyClass proxyClass, Constructor<?> constructor) {
        super(runtime, runtime.getJavaSupport().getJavaProxyConstructorClass());
        this.declaringProxyClass = proxyClass;
        this.proxyConstructor = constructor;
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        final int len = parameterTypes.length - 1; // last argument is our invocation handler
        // see JavaProxyClassFactory's generateConstructor ...
        System.arraycopy(parameterTypes, 0, actualParameterTypes = new Class<?>[len], 0, len);
        this.actualVarArgs = JavaProxyClassFactory.isVarArgs(proxyConstructor);
    }

    public final Class<?>[] getParameterTypes() {
        return actualParameterTypes;
    }

    public final Class<?>[] getExceptionTypes() {
        return proxyConstructor.getExceptionTypes();
    }

    public final boolean isVarArgs() { return actualVarArgs; }

    @JRubyMethod(name = "declaring_class")
    public JavaProxyClass getDeclaringClass() {
        return declaringProxyClass;
    }

    public final Object newInstance(Object[] args, JavaProxyInvocationHandler handler)
        throws IllegalArgumentException, InstantiationException,
               IllegalAccessException, InvocationTargetException {
        final int len = args.length;
        if ( len != actualParameterTypes.length ) {
            throw new IllegalArgumentException("wrong number of parameters");
        }

        final Object[] argsWithHandler = new Object[ len + 1 ];
        System.arraycopy(args, 0, argsWithHandler, 0, len);
        argsWithHandler[ len ] = handler;

        return proxyConstructor.newInstance(argsWithHandler);
    }

    @JRubyMethod
    public RubyFixnum arity() {
        return getRuntime().newFixnum(getArity());
    }

    public final int getArity() {
        return getParameterTypes().length;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof JavaProxyConstructor &&
            this.proxyConstructor == ((JavaProxyConstructor)other).proxyConstructor;
    }

    @Override
    public int hashCode() {
        return proxyConstructor.hashCode();
    }

    @Override
    @JRubyMethod
    public RubyString inspect() {
        StringBuilder str = new StringBuilder();
        str.append("#<");
        str.append( getDeclaringClass().nameOnInspection() );
        inspectParameterTypes(str, this);
        str.append(">");
        return getRuntime().newString( str.toString() );
    }

    @Override
    public String toString() {
        return inspect().toString();
    }

    @JRubyMethod
    public final RubyArray argument_types() {
        return toRubyArray(getParameterTypes());
    }

    @JRubyMethod(rest = true)
    public RubyObject new_instance2(IRubyObject[] args, Block unusedBlock) {
        final Ruby runtime = getRuntime();
        Arity.checkArgumentCount(runtime, args, 2, 2);

        final IRubyObject self = args[0];
        final Object[] convertedArgs = convertArguments((RubyArray) args[1]); // constructor arguments

        JavaProxyInvocationHandler handler = new MethodInvocationHandler(runtime, self);
        try {
            return JavaObject.wrap(runtime, newInstance(convertedArgs, handler));
        }
        catch (Exception e) {
            RaiseException ex = runtime.newArgumentError("Constructor invocation failed: " + e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

    public JavaObject newInstance(final IRubyObject self, Object[] args) throws RaiseException {
        final Ruby runtime = getRuntime();

        JavaProxyInvocationHandler handler = new MethodInvocationHandler(runtime, self);
        try {
            return JavaObject.wrap(runtime, newInstance(args, handler));
        }
        catch (Throwable t) {
            while ( t.getCause() != null ) t = t.getCause();
            RaiseException ex = runtime.newArgumentError("Constructor invocation failed: " + t.getMessage());
            ex.initCause(t);
            throw ex;
        }
    }

    private static class MethodInvocationHandler implements JavaProxyInvocationHandler {

        private final Ruby runtime;
        private final IRubyObject self;

        MethodInvocationHandler(final Ruby runtime, final IRubyObject self) {
            this.runtime = runtime; this.self = self;
        }

        public IRubyObject getOrig() { return self; }

        public Object invoke(Object proxy, JavaProxyMethod proxyMethod, Object[] nargs) throws Throwable {
            final RubyClass metaClass = self.getMetaClass();
            final String name = proxyMethod.getName();
            final DynamicMethod method = metaClass.searchMethod(name);

            final IRubyObject result = invokeRuby(method, proxyMethod, metaClass, name, nargs);

            final Class<?> returnType = proxyMethod.getReturnType();
            return returnType == void.class ? null : result.toJava( returnType );
        }

        private IRubyObject invokeRuby(final DynamicMethod method, final JavaProxyMethod proxyMethod,
            final RubyClass metaClass, final String name, final Object[] nargs) {
            final IRubyObject[] newArgs = new IRubyObject[nargs.length];
            for ( int i = nargs.length; --i >= 0; ) {
                newArgs[i] = JavaUtil.convertJavaToUsableRubyObject(runtime, nargs[i]);
            }

            final int arity = method.getArity().getValue();

            if ( arity < 0 || arity == newArgs.length ) {
                final ThreadContext context = runtime.getCurrentContext();
                return method.call(context, self, metaClass, name, newArgs);
            }
            if ( proxyMethod.hasSuperImplementation() ) {
                final ThreadContext context = runtime.getCurrentContext();
                final RubyClass superClass = metaClass.getSuperClass();
                return Helpers.invokeAs(context, superClass, self, name, newArgs, Block.NULL_BLOCK);
            }
            throw runtime.newArgumentError(newArgs.length, arity);
        }

    }

    @JRubyMethod(required = 1, optional = 1)
    public RubyObject new_instance(IRubyObject[] args, Block block) {
        final Ruby runtime = getRuntime();

        final int last = Arity.checkArgumentCount(runtime, args, 1, 2) - 1;

        final RubyProc proc;
        // Is there a supplied proc arg or do we assume a block was supplied
        if (args[last] instanceof RubyProc) {
            proc = (RubyProc) args[last];
        } else {
            proc = runtime.newProc(Block.Type.PROC, block);
        }

        final Object[] convertedArgs = convertArguments((RubyArray) args[0]);

        JavaProxyInvocationHandler handler = new ProcInvocationHandler(runtime, proc);
        try {
            return JavaObject.wrap(runtime, newInstance(convertedArgs, handler));
        }
        catch (Exception e) {
            RaiseException ex = runtime.newArgumentError("Constructor invocation failed: " + e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

    private static class ProcInvocationHandler implements JavaProxyInvocationHandler {

        private final Ruby runtime;
        private final RubyProc proc;

        ProcInvocationHandler(final Ruby runtime, final RubyProc proc) {
            this.runtime = runtime; this.proc = proc;
        }

        public IRubyObject getOrig() { return null; }

        public Object invoke(Object proxy, JavaProxyMethod method, Object[] nargs) throws Throwable {
            final int length = nargs == null ? 0 : nargs.length;
            final IRubyObject[] rubyArgs = new IRubyObject[length + 2];
            rubyArgs[0] = JavaObject.wrap(runtime, proxy);
            rubyArgs[1] = method;
            for ( int i = 0; i < length; i++ ) {
                rubyArgs[i + 2] = JavaUtil.convertJavaToRuby(runtime, nargs[i]);
            }
            IRubyObject procResult = proc.call(runtime.getCurrentContext(), rubyArgs);
            return procResult.toJava( method.getReturnType() );
        }

    }

    private Object[] convertArguments(final RubyArray arguments) {
        final int argsSize = arguments.size();

        final Object[] args = new Object[argsSize];
        final Class<?>[] parameterTypes = getParameterTypes();

        for ( int i = 0; i < argsSize; i++ ) {
            // TODO: call ruby array [] method?
            args[i] = arguments.entry(i).toJava( parameterTypes[i] );
        }
        return args;
    }

}
