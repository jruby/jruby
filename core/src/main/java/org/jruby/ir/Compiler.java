/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ast.executable.AbstractScript;
import org.jruby.ast.executable.Script;
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

public class Compiler extends IRTranslator<Script, JRubyClassLoader> {

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
    protected Script execute(final Ruby runtime, final IRScope scope, JRubyClassLoader classLoader) {
        final Class compiled = JVMVisitor.compile(scope, classLoader);
        final StaticScope staticScope = scope.getStaticScope();
        final IRubyObject runtimeTopSelf = runtime.getTopSelf();
        staticScope.setModule(runtimeTopSelf.getMetaClass());

        Method _compiledMethod;
        try {
            _compiledMethod = compiled.getMethod("__script__", ThreadContext.class,
                    StaticScope.class, IRubyObject.class, IRubyObject[].class, Block.class, RubyModule.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        final Method compiledMethod = _compiledMethod;

        return new AbstractScript() {
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

    }

}
