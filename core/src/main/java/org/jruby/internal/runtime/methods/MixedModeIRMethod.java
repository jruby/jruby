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

import java.io.ByteArrayOutputStream;

import org.jruby.RubyModule;
import org.jruby.compiler.Compilable;
import org.jruby.internal.runtime.AbstractIRMethod;
import org.jruby.internal.runtime.SplitSuperState;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.ExitableInterpreterContext;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.persistence.IRDumper;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class MixedModeIRMethod extends AbstractIRMethod implements Compilable<DynamicMethod> {
    private static final Logger LOG = LoggerFactory.getLogger(MixedModeIRMethod.class);

    private boolean displayedCFG = false; // FIXME: Remove when we find nicer way of logging CFG

    private volatile DynamicMethod actualMethod; // JIT-ed method

    public MixedModeIRMethod(IRScope method, Visibility visibility, RubyModule implementationClass) {
        super(method, visibility, implementationClass);

        if (!implementationClass.getRuntime().getInstanceConfig().isJitEnabled()) setCallCount(-1);
        // This is so profiled callsite can access the sites original method (callsites has IRScope in it).
        method.compilable = this;
    }

    public DynamicMethod getActualMethod() {
        return actualMethod;
    }

    protected void post(InterpreterContext ic, ThreadContext context) {
        // update call stacks (pop: ..)
        context.popFrame();
        if (ic.popDynScope()) {
            context.popScope();
        }
    }

    protected void pre(InterpreterContext ic, ThreadContext context, IRubyObject self, String name, Block block,
            RubyModule implClass) {
        // update call stacks (push: frame, class, scope, etc.)
        context.preMethodFrameOnly(implClass, name, self, block);
        if (ic.pushNewDynScope()) {
            context.pushScope(DynamicScope.newDynamicScope(ic.getStaticScope()));
        }
    }

    // TODO: new method or make this pre?
    protected void preSplit(InterpreterContext ic, ThreadContext context, IRubyObject self, String name, Block block,
            RubyModule implClass, DynamicScope scope) {
        // update call stacks (push: frame, class, scope, etc.)
        context.preMethodFrameOnly(implClass, name, self, block);
        if (ic.pushNewDynScope()) {
            context.pushScope(scope);
        }
    }

    @Override
    protected void printMethodIR() {
        ByteArrayOutputStream baos = IRDumper.printIR(getIRScope(), false);
        LOG.info("Printing simple IR for " + getIRScope().getId() + ":\n" + new String(baos.toByteArray()));
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args,
            Block block) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        // try jit before checking actualMethod, so we use jitted version immediately if
        // it's ready
        if (callCount >= 0) tryJit(context, this);

        DynamicMethod jittedMethod = actualMethod;
        if (jittedMethod != null) {
            return jittedMethod.call(context, self, clazz, name, args, block);
        }

        return INTERPRET_METHOD(context, ensureInstrsReady(), clazz, self, name, args, block);
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
            IRubyObject self, String name, IRubyObject[] args, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), ic.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.getEngine().interpret(context, null, self, ic, implClass, name, args, block);
            } else {
                try {
                    this.pre(ic, context, self, name, block, implClass);
                    return ic.getEngine().interpret(context, null, self, ic, implClass, name, args, block);
                } finally {
                    this.post(ic, context);
                }
            }
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        // try jit before checking actualMethod, so we use jitted version immediately if
        // it's ready
        if (callCount >= 0) tryJit(context, this);

        DynamicMethod jittedMethod = actualMethod;
        if (jittedMethod != null) {
            return jittedMethod.call(context, self, clazz, name, block);
        }

        return INTERPRET_METHOD(context, ensureInstrsReady(), clazz, self, name, block);
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
            IRubyObject self, String name, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), ic.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.getEngine().interpret(context, null, self, ic, implClass, name, block);
            } else {
                try {
                    this.pre(ic, context, self, name, block, implClass);
                    return ic.getEngine().interpret(context, null, self, ic, implClass, name, block);
                } finally {
                    this.post(ic, context);
                }
            }
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0,
            Block block) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        // try jit before checking actualMethod, so we use jitted version immediately if
        // it's ready
        if (callCount >= 0) tryJit(context, this);

        DynamicMethod jittedMethod = actualMethod;
        if (jittedMethod != null) {
            return jittedMethod.call(context, self, clazz, name, arg0, block);
        }

        return INTERPRET_METHOD(context, ensureInstrsReady(), clazz, self, name, arg0, block);
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
            IRubyObject self, String name, IRubyObject arg1, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), ic.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.getEngine().interpret(context, null, self, ic, implClass, name, arg1, block);
            } else {
                try {
                    this.pre(ic, context, self, name, block, implClass);
                    return ic.getEngine().interpret(context, null, self, ic, implClass, name, arg1, block);
                } finally {
                    this.post(ic, context);
                }
            }
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0,
            IRubyObject arg1, Block block) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        // try jit before checking actualMethod, so we use jitted version immediately if
        // it's ready
        if (callCount >= 0) tryJit(context, this);

        DynamicMethod jittedMethod = actualMethod;
        if (jittedMethod != null) {
            return jittedMethod.call(context, self, clazz, name, arg0, arg1, block);
        }

        return INTERPRET_METHOD(context, ensureInstrsReady(), clazz, self, name, arg0, arg1, block);
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
            IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), ic.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.getEngine().interpret(context, null, self, ic, implClass, name, arg1, arg2, block);
            } else {
                try {
                    this.pre(ic, context, self, name, block, implClass);
                    return ic.getEngine().interpret(context, null, self, ic, implClass, name, arg1, arg2, block);
                } finally {
                    this.post(ic, context);
                }
            }
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0,
            IRubyObject arg1, IRubyObject arg2, Block block) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        // try jit before checking actualMethod, so we use jitted version immediately if
        // it's ready
        if (callCount >= 0) tryJit(context, this);

        DynamicMethod jittedMethod = actualMethod;
        if (jittedMethod != null) {
            return jittedMethod.call(context, self, clazz, name, arg0, arg1, arg2, block);
        }

        return INTERPRET_METHOD(context, ensureInstrsReady(), clazz, self, name, arg0, arg1, arg2, block);
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
            IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), ic.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.getEngine().interpret(context, null, self, ic, implClass, name, arg1, arg2, arg3, block);
            } else {
                try {
                    this.pre(ic, context, self, name, block, implClass);
                    return ic.getEngine().interpret(context, null, self, ic, implClass, name, arg1, arg2, arg3, block);
                } finally {
                    this.post(ic, context);
                }
            }
        } finally {
            ThreadContext.popBacktrace(context);
        }

    }

    @Override
    public SplitSuperState<MethodSplitState> startSplitSuperCall(ThreadContext context, IRubyObject self,
            RubyModule clazz, String name, IRubyObject[] args, Block block) {
        // TODO: check if IR method, or is it guaranteed?
        InterpreterContext ic = ((IRMethod) getIRScope()).builtInterperterContextForJavaConstructor();
        if (!(ic instanceof ExitableInterpreterContext)) return null; // no super call/can't split this

        MethodSplitState state = new MethodSplitState(context, (ExitableInterpreterContext) ic, clazz, self, name);

        if (IRRuntimeHelpers.isDebug()) doDebug(); // TODO?

        // TODO: JIT?

        ExitableReturn result = INTERPRET_METHOD(state, args, block);

        return new SplitSuperState<>(result, state);
    }

    private ExitableReturn INTERPRET_METHOD(MethodSplitState state, IRubyObject[] args, Block block) {
        try {
            ThreadContext.pushBacktrace(state.context, state.name, state.eic.getFileName(), state.eic.getLine());

            // TODO: explicit call protocol?
            try {
                this.preSplit(state.eic, state.context, state.self, state.name, block, state.implClass, state.scope);
                return state.eic.getEngine().interpret(state.context, null, state.self, state.eic, state.state,
                        state.implClass, state.name, args, block);
            } finally {
                this.post(state.eic, state.context);
            }
        } finally {
            ThreadContext.popBacktrace(state.context);
        }
    }

    @Override
    public void finishSplitCall(SplitSuperState state) {
        if (IRRuntimeHelpers.isDebug()) doDebug(); // TODO?

        // TODO: JIT?

        INTERPRET_METHOD((MethodSplitState) state.state, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
    }

    private void doDebug() {
        // FIXME: This is printing out IRScope CFG but JIT may be active and it might
        // not reflect
        // currently executing. Move into JIT and into interp since they will be getting
        // CFG from
        // different sources
        // FIXME: This is only printing out CFG once. If we keep applying more passes
        // then we
        // will want to print out after those new passes.
        ensureInstrsReady();
        LOG.info("Executing '" + getIRScope().getId() + "'");
        if (!displayedCFG) {
            LOG.info(getIRScope().debugOutput());
            displayedCFG = true;
        }
    }

    @Override
    public void completeBuild(DynamicMethod newMethod) {
        setCallCount(-1);
        newMethod.serialNumber = this.serialNumber;
        actualMethod = newMethod;
        getImplementationClass().invalidateCacheDescendants();
    }

    @Override
    public DynamicMethod dup() {
        MixedModeIRMethod x = (MixedModeIRMethod) super.dup();
        x.callCount = callCount;
        x.actualMethod = actualMethod;

        return x;
    }

}
