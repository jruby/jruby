/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ast.executable.AbstractScript;
import org.jruby.ast.executable.Script;
import org.jruby.ast.executable.ScriptAndCode;
import org.jruby.compiler.NotCompilableException;
import org.jruby.exceptions.JumpException;
import org.jruby.ir.targets.JVMVisitor;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JRubyClassLoader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Compiler extends IRTranslator<ScriptAndCode, JRubyClassLoader> {

    // Compiler is singleton
    private Compiler() {}

    private static class CompilerHolder {
        // FIXME: Remove as singleton unless lifus does later
        public static final Compiler instance = new Compiler();
    }

    public static Compiler getInstance() {
        return CompilerHolder.instance;
    }

    @Override
    protected ScriptAndCode execute(final Ruby runtime, final IRScriptBody scope, JRubyClassLoader classLoader) {
        JVMVisitor visitor;
        byte[] bytecode;
        Class compiled;
        StaticScope _staticScope;
        IRubyObject _runtimeTopSelf;

        Method _compiledMethod;
        try {
            visitor = new JVMVisitor();
            bytecode = visitor.compileToBytecode(scope);
            compiled = visitor.defineFromBytecode(scope, bytecode, classLoader);
            _staticScope = scope.getStaticScope();
            _runtimeTopSelf = runtime.getTopSelf();
            _staticScope.setModule(_runtimeTopSelf.getMetaClass());

            _compiledMethod = compiled.getMethod("__script__", ThreadContext.class,
                    StaticScope.class, IRubyObject.class, IRubyObject[].class, Block.class, RubyModule.class);
        } catch (NotCompilableException nce) {
            throw nce;
        } catch (Throwable t) {
            throw new NotCompilableException("failed to compile script " + scope.getName(), t);
        }

        final Method compiledMethod = _compiledMethod;
        final StaticScope staticScope = _staticScope;
        final IRubyObject runtimeTopSelf = _runtimeTopSelf;

        Script script = new AbstractScript() {
            @Override
            public IRubyObject __file__(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
                try {
                    return (IRubyObject) compiledMethod.invoke(null,
                            runtime.getCurrentContext(), scope.getStaticScope(), runtimeTopSelf, IRubyObject.NULL_ARRAY, block, runtimeTopSelf.getMetaClass());
                } catch (InvocationTargetException ite) {
                    if (ite.getCause() instanceof JumpException) {
                        throw (JumpException) ite.getCause();
                    } else {
                        throw new RuntimeException(ite);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public IRubyObject load(ThreadContext context, IRubyObject self, boolean wrap) {
                Helpers.preLoadCommon(context, staticScope, false);
                try {
                    return __file__(context, self, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
                } finally {
                    Helpers.postLoad(context);
                }
            }
        };

        return new ScriptAndCode(bytecode, script);

    }

}
