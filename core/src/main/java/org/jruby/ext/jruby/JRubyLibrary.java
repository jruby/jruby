/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2009 MenTaLguY <mental@rydia.net>
 * Copyright (C) 2010 Charles Oliver Nutter <headius@headius.com>
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
package org.jruby.ext.jruby;

import java.util.ArrayList;
import org.jruby.AbstractRubyMethod;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.IRMethodArgs;
import org.jruby.internal.runtime.methods.MethodArgs2;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;

/**
 * Native part of require 'jruby'. Provides methods for swapping between the
 * normal Ruby reference to an object and the Java-integration-wrapped
 * reference.
 */
@JRubyModule(name="JRuby")
public class JRubyLibrary implements Library {
    public void load(Ruby runtime, boolean wrap) {
        ThreadContext context = runtime.getCurrentContext();

        runtime.getLoadService().require("java");

        // load Ruby parts of the 'jruby' library
        runtime.getLoadService().loadFromClassLoader(runtime.getJRubyClassLoader(), "jruby/jruby.rb", false);

        // define JRuby module
        RubyModule jrubyModule = runtime.getOrCreateModule("JRuby");

        jrubyModule.defineAnnotatedMethods(JRubyLibrary.class);
        jrubyModule.defineAnnotatedMethods(JRubyUtilLibrary.class);

        RubyClass threadLocalClass = jrubyModule.defineClassUnder("ThreadLocal", runtime.getObject(), JRubyThreadLocal.ALLOCATOR);
        threadLocalClass.defineAnnotatedMethods(JRubyExecutionContextLocal.class);

        RubyClass fiberLocalClass = jrubyModule.defineClassUnder("FiberLocal", runtime.getObject(), JRubyFiberLocal.ALLOCATOR);
        fiberLocalClass.defineAnnotatedMethods(JRubyExecutionContextLocal.class);

        RubyModule config = jrubyModule.defineModuleUnder("CONFIG");
        config.getSingletonClass().defineAnnotatedMethods(JRubyConfig.class);
    }

    public static class JRubyConfig {
        @JRubyMethod(name = "rubygems_disabled?")
        public static IRubyObject rubygems_disabled_p(ThreadContext context, IRubyObject self) {
            return context.runtime.newBoolean(
                    context.runtime.getInstanceConfig().isDisableGems());
        }
    }

    /**
     * Wrap the given object as in Java integration and return the wrapper. This
     * version uses ObjectProxyCache to guarantee the same wrapper is returned
     * as long as it is in use somewhere.
     */
    @JRubyMethod(module = true)
    public static IRubyObject reference(ThreadContext context, IRubyObject recv, IRubyObject obj) {
        Ruby runtime = context.runtime;

        return Java.getInstance(runtime, obj, false);
    }

    /**
     * Wrap the given object as in Java integration and return the wrapper. This
     * version does not use ObjectProxyCache.
     */
    @JRubyMethod(module = true)
    public static IRubyObject reference0(ThreadContext context, IRubyObject recv, IRubyObject obj) {
        Ruby runtime = context.runtime;

        return Java.getInstance(runtime, obj);
    }

    /**
     * Unwrap the given Java-integration-wrapped object, returning the unwrapped
     * object. If the wrapped object is not a Ruby object, an error will raise.
     */
    @JRubyMethod(module = true)
    public static IRubyObject dereference(ThreadContext context, IRubyObject recv, IRubyObject obj) {
        Object unwrapped;

        if (obj instanceof JavaProxy) {
            unwrapped = ((JavaProxy)obj).getObject();
        } else if (obj.dataGetStruct() instanceof JavaObject) {
            unwrapped = JavaUtil.unwrapJavaObject(obj);
        } else {
            throw context.runtime.newTypeError("got " + obj + ", expected wrapped Java object");
        }

        if (!(unwrapped instanceof IRubyObject)) {
            throw context.runtime.newTypeError("got " + obj + ", expected Java-wrapped Ruby object");
        }

        return (IRubyObject)unwrapped;
    }

    /**
     * Provide the "identity" hash code that System.identityHashCode would produce.
     *
     * Added here as an extension because calling System.identityHashCode (and other
     * Java-integration-related mechanisms) will cause some core types to coerce to
     * Java types, losing their proper identity.
     */
    @JRubyMethod(module = true)
    public static IRubyObject identity_hash(ThreadContext context, IRubyObject recv, IRubyObject obj) {
        return context.runtime.newFixnum(System.identityHashCode(obj));
    }
    
    public static class MethodExtensions {
        @JRubyMethod(name = "args")
        public static IRubyObject methodArgs(IRubyObject recv) {
            Ruby runtime = recv.getRuntime();
            AbstractRubyMethod rubyMethod = (AbstractRubyMethod)recv;
            RubyArray argsArray = RubyArray.newArray(runtime);
            DynamicMethod method = rubyMethod.getMethod().getRealMethod();
            RubySymbol rest = runtime.newSymbol("rest");

            if (method instanceof MethodArgs2) {
                return Helpers.parameterListToParameters(runtime, ((MethodArgs2) method).getParameterList(), true);
            } else if (method instanceof IRMethodArgs) {
                String[] argsDesc = ((IRMethodArgs) method).getParameterList();

                for (int i = 0; i < argsDesc.length; i++) {
                    RubySymbol argType = runtime.newSymbol(argsDesc[i]);
                    i++;
                    String argName = argsDesc[i];
                    if (argName.isEmpty()) {
                        argsArray.append(RubyArray.newArray(runtime, argType));
                    } else {
                        argsArray.append(RubyArray.newArray(runtime, argType, runtime.newSymbol(argName)));
                    }
                }
            } else {
                if (method.getArity() == Arity.OPTIONAL) {
                    argsArray.append(RubyArray.newArray(runtime, rest));
                }
            }

            return argsArray;
        }
        
        public static String[] methodParameters(Ruby runtime, DynamicMethod method) {
            ArrayList<String> argsArray = new ArrayList<String>();
            method = method.getRealMethod();

            if (method instanceof MethodArgs2) {
                return ((MethodArgs2) method).getParameterList();
            } else if (method instanceof IRMethodArgs) {
                String[] argsDesc = ((IRMethodArgs) method).getParameterList();

                for (int i = 0; i < argsDesc.length; i++) {
                    String argType = argsDesc[i];
                    i++;
                    String argName = argsDesc[i];
                    if (argName.isEmpty()) {
                        argsArray.add(argType);
                    } else {
                        argsArray.add(argType + argName);
                    }
                }
            } else {
                if (method.getArity() == Arity.OPTIONAL) {
                    argsArray.add("r");
                }
            }

            return argsArray.toArray(new String[argsArray.size()]);
        }
    }
}
