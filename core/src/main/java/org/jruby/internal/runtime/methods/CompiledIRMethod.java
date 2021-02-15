package org.jruby.internal.runtime.methods;

import java.lang.invoke.MethodHandle;

import org.jruby.RubyModule;
import org.jruby.compiler.Compilable;
import org.jruby.internal.runtime.*;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.*;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;

public class CompiledIRMethod extends AbstractIRMethod implements Compilable<DynamicMethod>  {

    private MethodHandle specific;
    private final int specificArity;
    private final String encodedArgumentDescriptors;
    private final boolean needsToFindImplementer;

    public CompiledIRMethod(MethodHandle variable, String id, int line, StaticScope scope, Visibility visibility,
                            RubyModule implementationClass, String encodedArgumentDescriptors, boolean recievesKeywordArgs, boolean needsToFindImplementer) {
        this(variable, null, -1, id, line, scope, visibility, implementationClass, encodedArgumentDescriptors, recievesKeywordArgs, needsToFindImplementer);
    }

    // Used by spec:compiler
    public CompiledIRMethod(MethodHandle variable, IRScope method, Visibility visibility, RubyModule implementationClass,
                            String encodedArgumentDescriptors) {
        this(variable, null, -1, method.getId(), method.getLine(), method.getStaticScope(),
                visibility, implementationClass, encodedArgumentDescriptors, method.receivesKeywordArgs(),
                !(method instanceof IRMethod && !method.getFullInterpreterContext().getFlags().contains(IRFlags.REQUIRES_CLASS)));
    }

    // Used by spec:compiler
    public CompiledIRMethod(MethodHandle variable, MethodHandle specific, int specificArity, IRScope method,
                            Visibility visibility, RubyModule implementationClass, String encodedArgumentDescriptors) {
        this(variable, specific, specificArity, method.getId(), method.getLine(), method.getStaticScope(),
                visibility, implementationClass, encodedArgumentDescriptors, method.receivesKeywordArgs(),
                !(method instanceof IRMethod && !method.getFullInterpreterContext().getFlags().contains(IRFlags.REQUIRES_CLASS)));
    }

    // Ruby Class/Module constructor (feels like we should maybe have a subtype here...
    public CompiledIRMethod(MethodHandle variable, String id, int line, StaticScope scope,
                            Visibility visibility, RubyModule implementationClass) {
        this(variable, null, -1, id, line, scope, visibility, implementationClass, "", false, false);
    }

    public CompiledIRMethod(MethodHandle variable, MethodHandle specific, int specificArity, String id, int line,
                            StaticScope scope, Visibility visibility, RubyModule implementationClass,
                            String encodedArgumentDescriptors, boolean receivesKeywordArgs, boolean needsToFindImplementer) {
            super(scope, id, line, visibility, implementationClass);

        this.specific = specific;
        // deopt unboxing if we have to process kwargs hash (although this really has nothing to do with arg
        // unboxing -- it was a simple path to hacking this in).
        this.specificArity = receivesKeywordArgs ? -1 : specificArity;
        staticScope.determineModule();

        this.encodedArgumentDescriptors = encodedArgumentDescriptors;
        //assert method.hasExplicitCallProtocol();

        setHandle(variable);

        this.needsToFindImplementer = needsToFindImplementer;

        // FIXME: inliner breaks with this line commented out
        // method.compilable = this;
    }

    public MethodHandle getHandleFor(int arity) {
        if (specificArity != -1 && arity == specificArity) {
            return specific;
        }

        return null;
    }

    public void setVariable(MethodHandle variable) {
        super.setHandle(variable);
    }

    public void setSpecific(MethodHandle specific) {
        this.specific = specific;
    }


    public ArgumentDescriptor[] getArgumentDescriptors() {
        return ArgumentDescriptor.decode(implementationClass.getRuntime(), encodedArgumentDescriptors);
    }

    @Override
    public void completeBuild(DynamicMethod buildResult) {
        // unused but part of compilable interface.  jit task uses setVariable and setSpecific to update code.
    }

    @Override
    protected void printMethodIR() {
        // no-op
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        try {
            return (IRubyObject) ((MethodHandle) this.handle).invokeExact(context, staticScope, self, args, block, clazz, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        if (specificArity != 0) return call(context, self, clazz, name, IRubyObject.NULL_ARRAY, block);

        try {
            return (IRubyObject) this.specific.invokeExact(context, staticScope, self, block, clazz, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        if (specificArity != 1) return call(context, self, clazz, name, new IRubyObject[]{arg0}, block);

        try {
            return (IRubyObject) this.specific.invokeExact(context, staticScope, self, arg0, block, clazz, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        if (specificArity != 2) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1}, block);

        try {
            return (IRubyObject) this.specific.invokeExact(context, staticScope, self, arg0, arg1, block, clazz, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (specificArity != 3) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1, arg2 }, block);

        try {
            return (IRubyObject) this.specific.invokeExact(context, staticScope, self, arg0, arg1, arg2, block, clazz, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
        try {
            return (IRubyObject) ((MethodHandle) this.handle).invokeExact(context, staticScope, self, args, Block.NULL_BLOCK, clazz, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        if (specificArity != 0) return call(context, self, clazz, name, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);

        try {
            return (IRubyObject) this.specific.invokeExact(context, staticScope, self, Block.NULL_BLOCK, clazz, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
        if (specificArity != 1) return call(context, self, clazz, name, new IRubyObject[]{arg0}, Block.NULL_BLOCK);

        try {
            return (IRubyObject) this.specific.invokeExact(context, staticScope, self, arg0, Block.NULL_BLOCK, clazz, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
        if (specificArity != 2) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1}, Block.NULL_BLOCK);

        try {
            return (IRubyObject) this.specific.invokeExact(context, staticScope, self, arg0, arg1, Block.NULL_BLOCK, clazz, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }
    
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        if (specificArity != 3) return call(context, self, clazz, name, new IRubyObject[] {arg0, arg1, arg2 }, Block.NULL_BLOCK);

        try {
            return (IRubyObject) this.specific.invokeExact(context, staticScope, self, arg0, arg1, arg2, Block.NULL_BLOCK, clazz, name);
        }
        catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    //TODO: compile:

    @Override
    public SplitSuperState<MethodSplitState> startSplitSuperCall(ThreadContext context, IRubyObject self, RubyModule clazz, String name,
    		IRubyObject[] args, Block block)
    {
    	// TODO: check if IR method, or is it guaranteed?
		InterpreterContext ic = ((IRMethod) getIRScope()).builtInterperterContextForJavaConstructor();
		if (!(ic instanceof ExitableInterpreterContext))
			return null; // no super call/can't split this
    	
    	MethodSplitState state = new MethodSplitState(context, (ExitableInterpreterContext) ic, clazz, self, name);
    	

        // TODO: JIT?

    	ExitableReturn result = INTERPRET_METHOD(state,  args, block);
        
        return new SplitSuperState<>(result, state);
    }

    private ExitableReturn INTERPRET_METHOD(MethodSplitState state, IRubyObject[] args, Block block)
	{
    	ThreadContext.pushBacktrace(state.context, state.name, state.eic.getFileName(), state.context.getLine());
    	
        try {
            ThreadContext.pushBacktrace(state.context, state.name, state.eic.getFileName(), state.context.getLine());

            //TODO: explicit call protocol?
            try {
                this.preSplit(state.eic, state.context, state.self, state.name, block, state.implClass, state.scope);
                return state.eic.getEngine().interpret(state.context, null, state.self, state.eic, state.state, state.implClass, state.name, args, block);
            } finally {
                this.post(state.eic, state.context);
            }
        } finally {
            ThreadContext.popBacktrace(state.context);
        }
	}

    @Override
    public void finishSplitCall(SplitSuperState state)
    {

        // TODO: JIT?

        INTERPRET_METHOD((MethodSplitState)state.state, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
    }
    protected void post(InterpreterContext ic, ThreadContext context) {
        // update call stacks (pop: ..)
        context.popFrame();
        if (ic.popDynScope()) {
            context.popScope();
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

    public boolean needsToFindImplementer() {
        return needsToFindImplementer;
    }
}
