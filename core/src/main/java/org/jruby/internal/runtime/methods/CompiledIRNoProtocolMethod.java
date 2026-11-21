/***** BEGIN LICENSE BLOCK *****
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

import org.jruby.RubyModule;
import org.jruby.internal.runtime.AbstractIRMethod;
import org.jruby.internal.runtime.SplitSuperCall;
import org.jruby.internal.runtime.SplitSuperState;
import org.jruby.ir.interpreter.ExitableInterpreterContext;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * The JIT-compiled body of a singleton-class opener &mdash; the code inside:
 * <pre>{@code
 *   class << obj
 *     # ...
 *   end
 * }</pre>
 *
 * its interpreted sibling is {@link InterpretedIRBodyMethod}
 *
 * <p>
 *     Such a body runs once, with no arguments where its written.
 *     Unlike ordinary methods its IR is deliberately left <em>without</em> usual frame/scope push/pop instructions
 *     ({@link org.jruby.ir.passes.AddCallProtocolInstructions} skips {@link org.jruby.ir.IRMetaClassBody})
 *     &mdash; hence "NoProtocol".
 *
 *     So this wrapper does that bookkeeping by hand, in {@link #pre}/{@link #post} around the single call.
 */
public class CompiledIRNoProtocolMethod extends AbstractIRMethod {
    private final boolean needsDynamicScope;
    private final MethodHandle variable;

    public CompiledIRNoProtocolMethod(MethodHandle handle, StaticScope scope, String file, int line, RubyModule implementationClass, boolean needsDynamicScope) {
        super(scope, file, line, Visibility.PUBLIC, implementationClass);

        this.needsDynamicScope = needsDynamicScope;
        this.variable = handle;
    }

    public ArgumentDescriptor[] getArgumentDescriptors() {
        return ArgumentDescriptor.EMPTY_ARRAY;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        final StaticScope staticScope = this.staticScope;
        final RubyModule implementationClass = this.implementationClass;
        pre(context, staticScope, implementationClass, self, name, block);

        try {
            return (IRubyObject) this.variable.invokeExact(context, staticScope, self, IRubyObject.NULL_ARRAY, block, implementationClass, name);
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        } finally {
            post(context);
        }
    }

    /**
     * All specific-arity paths chain here via {@link org.jruby.internal.runtime.methods.DynamicMethod}
     * @implNote metaclass bodies are arity-0 by construction so any arg > 0 dispatch is a bug
     */
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        throw new AssertionError("BUG: metaclass body invoked with arguments");
    }

    protected void post(ThreadContext context) {
        context.popFrame();
        if (needsDynamicScope) context.popScope();
    }

    protected void pre(ThreadContext context, StaticScope staticScope, RubyModule implementationClass, IRubyObject self, String name, Block block) {
        context.preMethodFrameOnly(implementationClass, name, self, getVisibility(), block);
        if (needsDynamicScope) {
            // Add a parent-link to current dynscope to support non-local returns cheaply
            // local variable resolution is unaffected (locals carry their own depth)
            context.pushScope(DynamicScope.newDynamicScope(staticScope, context.getCurrentScope()));
        }
    }

    @Override
    protected SplitSuperState<MethodSplitState> tryCompiledTerminalSplit(ThreadContext context, IRubyObject self,
            RubyModule clazz, String name, IRubyObject[] args, Block block, ExitableInterpreterContext ic) {
        Object previous = context.beginSplitSuperCapture(self, name);
        try {
            call(context, self, clazz, name, block);
        } catch (SplitSuperCall split) {
            return new SplitSuperState<>(split.getResult(), new MethodSplitState(ic));
        } finally {
            context.endSplitSuperCapture(previous);
        }

        throw new RuntimeException("BUG: compiled split constructor completed without captured super");
    }

    @Override
    public InterpreterContext ensureInstrsReady() {
        // AbstractIRMethod.getMethodData() calls this; we want the IC because no get/put fields were eliminated.
        return getIRScope().getInterpreterContext();
    }

    @Override
    protected void printMethodIR() {
        // no-op
    }
}
