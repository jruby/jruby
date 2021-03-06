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

package org.jruby.internal.runtime;

import java.util.Collection;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.compiler.Compilable;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.IRMethodArgs;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.PositionAware;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ivars.MethodData;
import org.jruby.util.cli.Options;

public abstract class AbstractIRMethod extends DynamicMethod implements IRMethodArgs, PositionAware, Cloneable {

    protected final Signature signature;
    protected IRScope method;
    protected final int line;
    protected final StaticScope staticScope;
    protected int callCount = 0;
    protected transient InterpreterContext interpreterContext; // cached from method
    private transient MethodData methodData;

    // Interpreted and Jitted but live IRScope known constructor
    public AbstractIRMethod(IRScope method, Visibility visibility, RubyModule implementationClass) {
        this(method.getStaticScope(), method.getId(), method.getLine(), visibility, implementationClass);
        // It is a little hinky to have a callback when we just set method anyways, but debugging in main constructor might need method before we set it.
        this.method = method;
    }

    // Compiled where IRScope must be retrieved at a later date if actually needed
    public AbstractIRMethod(StaticScope scope, String id, int line, Visibility visibility,
                            RubyModule implementationClass) {
        super(implementationClass, visibility, id);
        this.staticScope = scope;
        this.staticScope.determineModule();
        this.signature = staticScope.getSignature();
        this.line = line;

        final Ruby runtime = implementationClass.getRuntime();
        // If we are printing, do the build right at creation time so we can see it
        if (IRRuntimeHelpers.shouldPrintIR(runtime)) {
            ensureInstrsReady();
        }
    }

    public static <T extends AbstractIRMethod & Compilable> void tryJit(ThreadContext context, T self) {
        final Ruby runtime = context.runtime;
        if (runtime.isBooting() && !Options.JIT_KERNEL.load()) return; // don't JIT during runtime boot

        if (self.callCount < 0) return;
        // we don't synchronize callCount++ it does not matter if count isn't accurate
        if (self.callCount++ >= runtime.getInstanceConfig().getJitThreshold()) {
            synchronized (self) { // disable same jit tasks from entering queue twice
                if (self.callCount >= 0) {
                    self.callCount = Integer.MIN_VALUE; // so that callCount++ stays < 0

                    runtime.getJITCompiler().buildThresholdReached(context, self);
                }
            }
        }
    }

    public final void setCallCount(int callCount) {
        synchronized (this) {
            this.callCount = callCount;
        }
    }

    public IRScope getIRScope() {
        try {
            if (method == null) method = staticScope.getIRScope();
            return method;
        } catch (Exception e) {
            return null;
        }
    }

    public StaticScope getStaticScope() {
        return staticScope;
    }

    public ArgumentDescriptor[] getArgumentDescriptors() {
        ensureInstrsReady(); // Make sure method is minimally built before returning this info
        return ((IRMethod) getIRScope()).getArgumentDescriptors();
    }

    public InterpreterContext ensureInstrsReady() {
        final InterpreterContext interpreterContext = this.interpreterContext;
        if (interpreterContext == null) {
            return this.interpreterContext = retrieveInterpreterContext();
        }
        return interpreterContext;
    }

    private InterpreterContext retrieveInterpreterContext() {
        IRScope method = getIRScope();
        final InterpreterContext interpreterContext = method.builtInterpreterContext();

        if (IRRuntimeHelpers.shouldPrintIR(implementationClass.getRuntime())) printMethodIR();

        return interpreterContext;
    }

    protected abstract void printMethodIR() ;

    public Signature getSignature() {
        return signature;
    }

    @Deprecated @Override
    public Arity getArity() {
        return signature.arity();
    }

    @Override
    public DynamicMethod dup() {
        return (DynamicMethod) clone();
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException("not cloneable: " + this);
        }
    }

    public String getFile() {
        return staticScope.getFile();
    }

    public int getLine() {
        return line;
    }

    /**
     * Additional metadata about this method.
     */
    @Override
    public MethodData getMethodData() {
        if (methodData == null) {
            methodData = ((IRMethod) getIRScope()).getMethodData();
        }

        return methodData;
    }

    @Override
    public Collection<String> getInstanceVariableNames() {
        return staticScope.getInstanceVariableNames();
    }

    @Override
    public String toString() {
        return getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(this)) + ' ' + getIRScope() + ' ' + getSignature();
    }

    public boolean needsToFindImplementer() {
        ensureInstrsReady(); // Ensure scope is ready for flags

        IRScope irScope = getIRScope();
        // FIXME: This may stop working if we eliminate startup interp
        return !(irScope instanceof IRMethod && !irScope.getInterpreterContext().getFlags().contains(IRFlags.REQUIRES_CLASS));
    }
    /**
     * Calls a split method (java constructor-invoked initialize) and returns the paused state. If
     * this method doesn't have a super call, returns null without execution.
     */
    public abstract SplitSuperState startSplitSuperCall(ThreadContext context, IRubyObject self, RubyModule klazz,
            String name, IRubyObject[] args, Block block);
    
    public abstract void finishSplitCall(SplitSuperState state);
}
