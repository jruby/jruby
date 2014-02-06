/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir;

import java.lang.reflect.InvocationTargetException;

import org.jruby.Ruby;
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
        final Class compiled = JVMVisitor.compile(runtime, scope, classLoader);
        final StaticScope staticScope = scope.getStaticScope();
        final IRubyObject runtimeTopSelf = runtime.getTopSelf();
        staticScope.setModule(runtimeTopSelf.getMetaClass());
        return new AbstractScript() {
            @Override
            public IRubyObject __file__(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
                try {
                    return (IRubyObject) compiled.getMethod("__script__", ThreadContext.class,
                            StaticScope.class, IRubyObject.class, IRubyObject[].class, Block.class).invoke(null,
                            runtime.getCurrentContext(), scope.getStaticScope(), runtimeTopSelf, IRubyObject.NULL_ARRAY, block);
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
                try {
                    Helpers.preLoadCommon(context, staticScope, false);
                    return __file__(context, self, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
                } finally {
                    Helpers.postLoad(context);
                }
            }
        };

    }

}
