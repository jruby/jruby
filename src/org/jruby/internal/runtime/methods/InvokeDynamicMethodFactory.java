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

import com.headius.invokebinder.Signature;
import com.headius.invokebinder.SmartBinder;
import com.headius.invokebinder.SmartHandle;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyKernel;
import org.jruby.parser.StaticScope;
import org.jruby.RubyModule;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.MethodFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.util.CodegenUtils.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import org.jruby.runtime.invokedynamic.InvocationLinker;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.jruby.util.unsafe.UnsafeFactory;

/**
 * In order to avoid the overhead with reflection-based method handles, this
 * MethodFactory uses ASM to generate tiny invoker classes. This allows for
 * better performance and more specialization per-handle than can be supported
 * via reflection. It also allows optimizing away many conditionals that can
 * be determined once ahead of time.
 * 
 * When running in secured environments, this factory may not function. When
 * this can be detected, MethodFactory will fall back on the reflection-based
 * factory instead.
 * 
 * @see org.jruby.runtime.MethodFactory
 */
public class InvokeDynamicMethodFactory extends InvocationMethodFactory {

    private static final Logger LOG = LoggerFactory.getLogger("InvokeDynamicMethodFactory");
    
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    
    /**
     */
    public InvokeDynamicMethodFactory(ClassLoader classLoader) {
        super(classLoader);
    }

    /**
     * Use code generation to provide a method handle for a compiled Ruby method.
     * 
     * @see org.jruby.runtime.MethodFactory#getCompiledMethod
     */
    public DynamicMethod getCompiledMethodLazily(
            RubyModule implementationClass,
            String rubyName,
            String javaName,
            Arity arity,
            Visibility visibility,
            StaticScope scope,
            Object scriptObject,
            CallConfiguration callConfig,
            ISourcePosition position,
            String parameterDesc) {

        return getCompiledMethod(implementationClass, rubyName, javaName, arity, visibility, scope, scriptObject, callConfig, position, parameterDesc);
    }

    /**
     * Use JSR292 to provide a method handle for a compiled Ruby method.
     * 
     * @see org.jruby.runtime.MethodFactory#getCompiledMethod
     */
    public DynamicMethod getCompiledMethod(
            RubyModule implementationClass,
            String rubyName,
            String javaName,
            Arity arity,
            Visibility visibility,
            StaticScope scope,
            Object scriptObject,
            CallConfiguration callConfig,
            ISourcePosition position,
            String parameterDesc) {
        Class scriptClass = scriptObject.getClass();

        try {
            MethodHandle[] targets = new MethodHandle[5];
            SmartHandle directCall;
            int specificArity = -1;

            // acquire handle to the actual method body
            if (scope.getRestArg() >= 0 || scope.getOptionalArgs() > 0 || scope.getRequiredArgs() > 3) {
                // variable arity method (has optional, rest, or more args than we can splat)
                directCall = SmartBinder
                        .from(VARIABLE_ARITY_SIGNATURE.prependArg("script", scriptClass))
                        .invokeStaticQuiet(LOOKUP, scriptClass, javaName)
                        .bindTo(scriptObject);
            } else {
                // specific arity method (less than 4 required args only)
                specificArity = scope.getRequiredArgs();

                directCall = SmartBinder
                        .from(SPECIFIC_ARITY_SIGNATURES[specificArity].prependArg("script", scriptClass))
                        .invokeStaticQuiet(LOOKUP, scriptClass, javaName)
                        .bindTo(scriptObject);
            }

            // wrap with framing logic if needed
            if (!callConfig.isNoop()) {
                directCall = new SmartHandle(
                        directCall.signature(),
                        InvocationLinker.wrapWithFraming(directCall.signature(), callConfig, implementationClass, rubyName, directCall.handle(), scope));
            }

            // provide a variable-arity path for specific-arity target
            SmartHandle variableCall;
            if (specificArity >= 0) {
                SmartHandle arityCheck = SmartBinder
                        .from(ARITY_CHECK_FOLD)
                        .append(new String[]{"min", "max"}, new Class[]{int.class, int.class}, specificArity, specificArity)
                        .cast(ARITY_CHECK_SIGNATURE)
                        .invokeStaticQuiet(LOOKUP, Arity.class, "checkArgumentCount");

                variableCall = SmartBinder
                        .from(VARIABLE_ARITY_SIGNATURE)
                        .foldVoid(arityCheck)
                        .permute("script", "context", "self", "block", "args")
                        .spread("arg", specificArity)
                        .permute("script", "context", "self", "arg*", "block")
                        .invoke(directCall);
            } else {
                variableCall = directCall;
            }

            // TODO: tracing

            // pre-call trace
            if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            }
            
            if (specificArity >= 0) {
                targets[specificArity] = directCall.handle();
                targets[4] = variableCall.handle();
            } else {
                targets[4] = directCall.handle();
            }
            
            return new HandleMethod(implementationClass, visibility, callConfig, targets, parameterDesc);

        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static final Signature VARIABLE_ARITY_SIGNATURE = Signature
            .returning(IRubyObject.class)
            .appendArg("context", ThreadContext.class)
            .appendArg("self", IRubyObject.class)
            .appendArg("args", IRubyObject[].class)
            .appendArg("block", Block.class);
    
    public static final Signature ARITY_CHECK_FOLD = Signature
            .returning(void.class)
            .appendArg("context", ThreadContext.class)
            .appendArg("args", IRubyObject[].class);
    
    public static final Signature ARITY_CHECK_SIGNATURE = Signature
            .returning(int.class)
            .appendArg("context", ThreadContext.class)
            .appendArg("args", IRubyObject[].class)
            .appendArg("min", int.class)
            .appendArg("max", int.class);
    
    public static final Signature[] SPECIFIC_ARITY_SIGNATURES;
    static {
            Signature[] specifics = new Signature[4];
            Signature specific = Signature
                    .returning(IRubyObject.class)
                    .appendArg("context", ThreadContext.class)
                    .appendArg("self", IRubyObject.class);
            
            specifics[0] = specific.appendArg("block", Block.class);
            
            for (int i = 0; i < 3; i++) {
                specific = specific
                        .appendArg("arg" + i, IRubyObject.class);
                specifics[i + 1] = specific.appendArg("block", Block.class);
            }
            SPECIFIC_ARITY_SIGNATURES = specifics;
    }

    @Override
    public byte[] getCompiledMethodOffline(
            String rubyName, String javaName, String className, String invokerPath, Arity arity,
            StaticScope scope, CallConfiguration callConfig, String filename, int line) {
        throw new RuntimeException("no offline support for invokedynamic handles");
    }
}
