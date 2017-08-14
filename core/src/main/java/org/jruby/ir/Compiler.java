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
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.runtime.IRBreakJump;
import org.jruby.ir.targets.JVMVisitor;
import org.jruby.ir.targets.JVMVisitorMethodContext;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ClassDefiningClassLoader;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class Compiler extends IRTranslator<ScriptAndCode, ClassDefiningClassLoader> {

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
    protected ScriptAndCode execute(final Ruby runtime, final IRScriptBody scope, ClassDefiningClassLoader classLoader) {
        byte[] bytecode;
        MethodHandle _compiledHandle;

        if ((scope.getBeginBlocks() != null && scope.getBeginBlocks().size() > 0) ||
                (scope.getEndBlocks() != null && scope.getBeginBlocks().size() > 0)) {
            throw new NotCompilableException("target script has BEGIN or END");
        }

        try {
            JVMVisitor visitor = new JVMVisitor(runtime);
            JVMVisitorMethodContext context = new JVMVisitorMethodContext();
            bytecode = visitor.compileToBytecode(scope, context);
            Class compiled = visitor.defineFromBytecode(scope, bytecode, classLoader);

            Method compiledMethod = compiled.getMethod("RUBY$script", ThreadContext.class,
                    StaticScope.class, IRubyObject.class, IRubyObject[].class, Block.class, RubyModule.class, String.class);
            _compiledHandle = MethodHandles.publicLookup().unreflect(compiledMethod);
        } catch (NotCompilableException nce) {
            throw nce;
        } catch (Throwable t) {
            throw new NotCompilableException("failed to compile script " + scope.getName(), t);
        }

        final MethodHandle compiledHandle = _compiledHandle;
        final StaticScope staticScope = scope.getStaticScope();

        Script script = new AbstractScript() {
            @Override
            public IRubyObject __file__(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
                try {
                    return (IRubyObject) compiledHandle.invokeWithArguments(context, staticScope, self, IRubyObject.NULL_ARRAY, block, self.getMetaClass(), Interpreter.ROOT);
                } catch (Throwable t) {
                    Helpers.throwException(t);
                    return null; // not reached
                }
            }

            @Override
            public IRubyObject load(ThreadContext context, IRubyObject self, boolean wrap) {
                // Compiler does not support BEGIN/END yet and should fail to compile above

                // Copied from Interpreter
                RubyModule currModule = staticScope.getModule();

                staticScope.setModule(currModule);
                context.setCurrentVisibility(Visibility.PRIVATE);

                try {
                    return (IRubyObject) compiledHandle.invokeWithArguments(context, staticScope, self, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK, currModule, Interpreter.ROOT);
                } catch (IRBreakJump bj) {
                    throw IRException.BREAK_LocalJumpError.getException(context.runtime);
                } catch (Throwable t) {
                    Helpers.throwException(t);
                    return null; // not reached
                } finally {
                }
            }
        };

        return new ScriptAndCode(bytecode, script);

    }

}
