/*
 **** BEGIN LICENSE BLOCK *****
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.jruby;

import java.io.IOException;

import org.jruby.CompatVersion;
import org.jruby.ast.RestArgNode;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;

import org.jruby.ast.ArgsNode;
import org.jruby.ast.ListNode;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaObject;
import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.internal.runtime.methods.DynamicMethod;

import org.jruby.ast.Node;
import org.jruby.ast.types.INameNode;
import org.jruby.internal.runtime.methods.MethodArgs;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ThreadContext;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyMethod;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.ast.MultipleAsgn19Node;
import org.jruby.ast.UnnamedRestArgNode;
import org.jruby.internal.runtime.methods.MethodArgs2;
import org.jruby.internal.runtime.methods.IRMethodArgs;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.util.RuntimeHelpers;
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
        runtime.getKernel().callMethod(context, "require", runtime.newString("java"));

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
        
        new JRubyConfigLibrary().load(runtime, wrap);
    }

    /**
     * Wrap the given object as in Java integration and return the wrapper. This
     * version uses ObjectProxyCache to guarantee the same wrapper is returned
     * as long as it is in use somewhere.
     */
    @JRubyMethod(module = true)
    public static IRubyObject reference(ThreadContext context, IRubyObject recv, IRubyObject obj) {
        Ruby runtime = context.getRuntime();

        return Java.getInstance(runtime, obj, false);
    }

    /**
     * Wrap the given object as in Java integration and return the wrapper. This
     * version does not use ObjectProxyCache.
     */
    @JRubyMethod(module = true)
    public static IRubyObject reference0(ThreadContext context, IRubyObject recv, IRubyObject obj) {
        Ruby runtime = context.getRuntime();

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
            throw context.getRuntime().newTypeError("got " + obj + ", expected wrapped Java object");
        }

        if (!(unwrapped instanceof IRubyObject)) {
            throw context.getRuntime().newTypeError("got " + obj + ", expected Java-wrapped Ruby object");
        }

        return (IRubyObject)unwrapped;
    }

    @JRubyMethod(module = true, compat = CompatVersion.RUBY2_0)
    public static IRubyObject ruby2_0(ThreadContext context, IRubyObject recv) {
        return context.runtime.newString("Welcome to the future of Ruby!");
    }
    
    public static class MethodExtensions {
        @JRubyMethod(name = "args")
        public static IRubyObject methodArgs(IRubyObject recv) {
            Ruby runtime = recv.getRuntime();
            RubyMethod rubyMethod = (RubyMethod)recv;
            RubyArray argsArray = RubyArray.newArray(runtime);
            DynamicMethod method = rubyMethod.getMethod();
            RubySymbol req = runtime.newSymbol("req");
            RubySymbol opt = runtime.newSymbol("opt");
            RubySymbol rest = runtime.newSymbol("rest");
            RubySymbol block = runtime.newSymbol("block");

            if (method instanceof MethodArgs2) {
                return RuntimeHelpers.parameterListToParameters(runtime, ((MethodArgs2)method).getParameterList(), true);
            } else if (method instanceof MethodArgs) {
                MethodArgs interpMethod = (MethodArgs)method;
                ArgsNode args = interpMethod.getArgsNode();
                
                ListNode requiredArgs = args.getPre();
                for (int i = 0; requiredArgs != null && i < requiredArgs.size(); i++) {
                    Node argNode = requiredArgs.get(i);
                    if (argNode instanceof MultipleAsgn19Node) {
                        argsArray.append(RubyArray.newArray(runtime, req));
                    } else {
                        argsArray.append(RubyArray.newArray(runtime, req, getNameFrom(runtime, (INameNode)argNode)));
                    }
                }
                
                ListNode optArgs = args.getOptArgs();
                for (int i = 0; optArgs != null && i < optArgs.size(); i++) {
                    argsArray.append(RubyArray.newArray(runtime, opt, getNameFrom(runtime, (INameNode) optArgs.get(i))));
                }

                if (args.getRestArg() >= 0) {
                    RestArgNode restArg = (RestArgNode) args.getRestArgNode();

                    if (restArg instanceof UnnamedRestArgNode) {
                        if (((UnnamedRestArgNode) restArg).isStar()) {
                            argsArray.append(RubyArray.newArray(runtime, rest));
                        }
                    } else {
                        argsArray.append(RubyArray.newArray(runtime, rest, getNameFrom(runtime, args.getRestArgNode())));
                    }
                }
                
                ListNode requiredArgsPost = args.getPost();
                for (int i = 0; requiredArgsPost != null && i < requiredArgsPost.size(); i++) {
                    Node argNode = requiredArgsPost.get(i);
                    if (argNode instanceof MultipleAsgn19Node) {
                        argsArray.append(RubyArray.newArray(runtime, req));
                    } else {
                        argsArray.append(RubyArray.newArray(runtime, req, getNameFrom(runtime, (INameNode) requiredArgsPost.get(i))));
                    }
                }

                if (args.getBlock() != null) {
                    argsArray.append(RubyArray.newArray(runtime, block, getNameFrom(runtime, args.getBlock())));
                }
            } else if (method instanceof IRMethodArgs) {
                for (String[] argParam: ((IRMethodArgs)method).getParameterList()) {
                    RubySymbol argType = runtime.newSymbol(argParam[0]);
                    if (argParam[1] == "") argsArray.append(RubyArray.newArray(runtime, argType));
                    else argsArray.append(RubyArray.newArray(runtime, argType, runtime.newSymbol(argParam[1])));
                }
            } else {
                if (method.getArity() == Arity.OPTIONAL) {
                    argsArray.append(RubyArray.newArray(runtime, rest));
                }
            }

            return argsArray;
        }
    }

    private static IRubyObject getNameFrom(Ruby runtime, INameNode node) {
        return node == null ? runtime.getNil() : RubySymbol.newSymbol(runtime, node.getName());
    }
}
