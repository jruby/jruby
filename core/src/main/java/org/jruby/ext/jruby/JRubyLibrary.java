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

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.api.Access;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.builder.IRBuilder;
import org.jruby.ir.targets.JVMVisitor;
import org.jruby.ir.targets.JVMVisitorMethodContext;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import org.jruby.util.ByteList;

import java.io.ByteArrayInputStream;

import static org.jruby.api.Access.instanceConfig;
import static org.jruby.api.Access.loadService;
import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Convert.*;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newEmptyString;
import static org.jruby.api.Define.defineModule;
import static org.jruby.api.Error.typeError;
import static org.jruby.parser.ParserType.INLINE;

/**
 * Native part of require 'jruby', e.g. provides methods for swapping between the normal Ruby reference to an
 * object and the Java-integration-wrapped reference.
 *
 * Parts of JRuby name-space are loaded even without <code>require 'jruby'</code>, those live under JRuby::Util.
 * @see JRubyUtilLibrary
 */
@JRubyModule(name="JRuby")
public class JRubyLibrary implements Library {
    public void load(Ruby runtime, boolean wrap) {
        ThreadContext context = runtime.getCurrentContext();
        var Object = objectClass(context);

        // load Ruby parts of the 'jruby' library
        loadService(context).loadFromClassLoader(runtime.getJRubyClassLoader(), "jruby/jruby.rb", false);

        var JRuby = defineModule(context, "JRuby").
                defineMethods(context, JRubyLibrary.class).defineMethods(context, JRubyUtilLibrary.class);

        JRuby.defineClassUnder(context, "ThreadLocal", Object, JRubyThreadLocal::new)
             .defineMethods(context, JRubyExecutionContextLocal.class);

        JRuby.defineClassUnder(context, "FiberLocal", Object, JRubyFiberLocal::new)
             .defineMethods(context, JRubyExecutionContextLocal.class);

        JRuby.defineModuleUnder(context, "CONFIG").
                tap(m -> m.singletonClass(context).defineMethods(context, JRubyConfig.class));
    }

    /**
     * JRuby::CONFIG
     */
    public static class JRubyConfig {
        @JRubyMethod(name = "rubygems_disabled?")
        public static IRubyObject rubygems_disabled_p(ThreadContext context, IRubyObject self) {
            return asBoolean(context, instanceConfig(context).isDisableGems());
        }

        @JRubyMethod(name = "did_you_mean_disabled?")
        public static IRubyObject did_you_mean_disabled_p(ThreadContext context, IRubyObject self) {
            return asBoolean(context, instanceConfig(context).isDisableDidYouMean());
        }
    }

    /**
     * Wrap the given object as in Java integration and return the wrapper. This
     * version uses ObjectProxyCache to guarantee the same wrapper is returned
     * as long as it is in use somewhere.
     */
    @JRubyMethod(module = true, name = {"reference", "ref"})
    public static IRubyObject reference(ThreadContext context, IRubyObject recv, IRubyObject obj) {
        return Java.getInstance(context.runtime, obj, false);
    }

    /**
     * Wrap the given object as in Java integration and return the wrapper.
     * This version does not use ObjectProxyCache.
     */
    @JRubyMethod(module = true)
    public static IRubyObject reference0(ThreadContext context, IRubyObject recv, IRubyObject obj) {
        return Java.wrapJavaObject(context, obj);
    }

    @JRubyMethod(module = true)
    public static IRubyObject runtime(ThreadContext context, IRubyObject recv) {
        return Java.wrapJavaObject(context, context.runtime);
    }

    /**
     * Unwrap the given Java-integration-wrapped object, returning the unwrapped
     * object. If the wrapped object is not a Ruby object, an error will raise.
     */
    @JRubyMethod(module = true, name = {"dereference", "deref"})
    public static IRubyObject dereference(ThreadContext context, IRubyObject recv, IRubyObject obj) {
        Object unwrapped = JavaUtil.unwrapIfJavaObject(obj);
        if (unwrapped == obj) throw typeError(context, "got " + obj.inspect(context) + ", expected wrapped Java object");
        if (unwrapped instanceof IRubyObject robj) return robj;
        throw typeError(context, "got " + obj.inspect(context) + ", expected Java-wrapped Ruby object");
    }

    /**
     * Run the provided (required) block with the "global runtime" set to the current runtime,
     * for libraries that expect to operate against the global runtime.
     */
    @JRubyMethod(module = true)
    public static IRubyObject with_current_runtime_as_global(ThreadContext context, IRubyObject recv, Block block) {
        final Ruby current = context.runtime;
        final Ruby global = Ruby.getGlobalRuntime();
        try {
            if (current != global) {
                current.useAsGlobalRuntime();
            }
            return block.yield(context, runtime(context, recv)); // previously yield (without an argument)
        }
        finally {
            if (Ruby.getGlobalRuntime() != global) {
                global.useAsGlobalRuntime();
            }
        }
    }

    @JRubyMethod(module = true, rest = true) // (loader = JRuby.runtime.jruby_class_loader)
    public static IRubyObject set_context_class_loader(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        final ClassLoader loader;
        if (args.length == 0 || args[0] == context.nil) {
            loader = context.runtime.getJRubyClassLoader();
        }
        else {
            loader = JavaUtil.unwrapJavaObject(args[0]);
        }
        java.lang.Thread.currentThread().setContextClassLoader(loader);
        return Java.wrapJavaObject(context, loader); // reference0
    }

    @JRubyMethod(name = "security_restricted?", module = true)
    public static RubyBoolean is_security_restricted(ThreadContext context, IRubyObject recv) {
        return asBoolean(context, Ruby.isSecurityRestricted());
    }

    @Deprecated(since = "10.0.0.0")
    public static RubyBoolean is_security_restricted(IRubyObject recv) {
        return is_security_restricted(((RubyBasicObject) recv).getCurrentContext(), recv);
    }

    // NOTE: its probably too late to set this when jruby library is booted (due the java library) ?
    @JRubyMethod(name = "security_restricted=", module = true)
    public static IRubyObject set_security_restricted(IRubyObject recv, IRubyObject arg) {
        Ruby.setSecurityRestricted(arg.isTrue());
        return is_security_restricted(recv);
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
        return asFixnum(context, System.identityHashCode(obj));
    }

    @JRubyMethod(module = true, name = "parse", alias = "ast_for", required = 1, optional = 3, checkArity = false)
    public static IRubyObject parse(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        // def parse(content = nil, filename = DEFAULT_FILENAME, extra_position_info = false, lineno = 0, &block)
        return Java.wrapJavaObject(context, parseImpl(context, args, block).getAST());
    }

    @JRubyMethod(module = true, name = "parse_result", required = 1, optional = 3, checkArity = false)
    public static IRubyObject parse_result(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        // def parse(content = nil, filename = DEFAULT_FILENAME, extra_position_info = false, lineno = 0, &block)
        return Java.wrapJavaObject(context, parseImpl(context, args, block));
    }

    private static ParseResult parseImpl(ThreadContext context, IRubyObject[] args, Block block) {
        if (block.isGiven()) {
            throw context.runtime.newNotImplementedError("JRuby.parse with block returning AST no longer supported");
        }

        final RubyString content = args[0].convertToString();
        final String filename;
        boolean inlineSource = false; int lineno = 0;

        switch (args.length) {
            case 1 :
                filename = "";
                break;
            case 2 :
                filename = args[1].convertToString().toString();
                break;
            case 3 :
                filename = args[1].convertToString().toString();
                inlineSource = args[2].isTrue();
                break;
            case 4 :
                filename = args[1].convertToString().toString();
                inlineSource = args[2].isTrue();
                lineno = toInt(context, args[3]);
                break;
            default :
                throw new AssertionError("unexpected arguments: " + java.util.Arrays.toString(args));
        }

        final ByteList bytes = content.getByteList();
        ByteArrayInputStream stream = new ByteArrayInputStream(bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getRealSize());
        Encoding encoding = content.getEncoding() == ASCIIEncoding.INSTANCE ? context.runtime.setupSourceEncoding(UTF8Encoding.INSTANCE) : bytes.getEncoding();

        if (inlineSource) {
            return context.runtime.getParserManager().parseMainFile(filename, lineno, stream, encoding, context.getCurrentScope(), INLINE);
        } else {
            return context.runtime.getParserManager().parseFile(filename, lineno, stream, encoding);
        }
    }

    @JRubyMethod(module = true, name = "compile_ir", required = 1, optional = 3, checkArity = false)
    public static IRubyObject compile_ir(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        // def compile_ir(content = nil, filename = DEFAULT_FILENAME, extra_position_info = false, &block)
        return Java.wrapJavaObject(context, compileIR(context, args, block));
    }

    private static IRScriptBody compileIR(ThreadContext context, IRubyObject[] args, Block block) {
        ParseResult result = parseImpl(context, args, block);
        IRManager manager = new IRManager(context.runtime, instanceConfig(context));
        manager.setBuilderFactory(context.runtime.getIRManager().getBuilderFactory());
        IRScriptBody scope = (IRScriptBody) IRBuilder.buildRoot(manager, result).getScope();
        scope.setScriptDynamicScope(result.getDynamicScope());
        scope.getStaticScope().setIRScope(scope);
        return scope;
    }

    @JRubyMethod(module = true, name = "compile", required = 1, optional = 3, checkArity = false)
    public static IRubyObject compile(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        // def compile(content = nil, filename = DEFAULT_FILENAME, extra_position_info = false, &block)

        int argc = Arity.checkArgumentCount(context, args, 1, 4);

        final Ruby runtime = context.runtime;

        final RubyString content = args[0].convertToString();
        args[0] = content;
        final RubyString filename = argc > 1 ? args[1].convertToString() : newEmptyString(context);

        IRScriptBody scope = compileIR(context, args, block);

        JVMVisitor visitor = JVMVisitor.newForJIT(runtime);
        JVMVisitorMethodContext methodContext = new JVMVisitorMethodContext();
        byte[] bytes = visitor.compileToBytecode(scope, methodContext);

        scope.getStaticScope().setModule(runtime.getTopSelf().getMetaClass());

        RubyClass CompiledScript = Access.getClass(context, "JRuby", "CompiledScript");
        // JRuby::CompiledScript#initialize(filename, class_name, content, bytes)
        return CompiledScript.newInstance(context,
                new IRubyObject[] {filename, asSymbol(context, scope.getId()), content, Java.getInstance(runtime, bytes)},
                Block.NULL_BLOCK);
    }

    @Deprecated(since = "9.2.1.0") // @JRubyMethod(meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject load_string_ext(ThreadContext context, IRubyObject recv) {
        CoreExt.loadStringExtensions(context);
        return context.nil;
    }

    @JRubyMethod(module = true)
    public static IRubyObject subclasses(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return subclasses(context, recv, arg instanceof RubyClass ? (RubyClass) arg : arg.getMetaClass(), false);
    }

    @JRubyMethod(module = true)
    public static IRubyObject subclasses(ThreadContext context, IRubyObject recv, IRubyObject arg, IRubyObject opts) {
        boolean recurseAll = false;
        opts = ArgsUtil.getOptionsArg(context, opts);
        if (opts != context.nil) {
            IRubyObject all = ((RubyHash) opts).fastARef(asSymbol(context, "all"));
            if (all != null) recurseAll = all.isTrue();
        }
        return subclasses(context, recv, arg instanceof RubyClass ? (RubyClass) arg : arg.getMetaClass(), recurseAll);
    }

    private static RubyArray subclasses(ThreadContext context, final IRubyObject recv,
                                        final RubyClass klass, final boolean recurseAll) {

        final var subclasses = newArray(context);

        RubyClass singletonClass = klass.singletonClass(context);
        RubyObjectSpace.each_objectInternal(context, recv, new IRubyObject[] { singletonClass },
                new Block(new JavaInternalBlockBody(context.runtime, Signature.ONE_ARGUMENT) {

                    @Override
                    public IRubyObject yield(ThreadContext context, IRubyObject[] args) {
                        return doYield(context, null, args[0]);
                    }

                    @Override
                    protected IRubyObject doYield(ThreadContext context, Block block, IRubyObject value) {
                        if (klass != value) {
                            if (recurseAll) return subclasses.append(context, value);
                            if (((RubyClass) value).superclass(context) == klass) return subclasses.append(context, value);
                        }
                        return context.nil;
                    }

                })
        );
        return subclasses;
    }

}
