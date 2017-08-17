package org.jruby.ir.instructions;

import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.*;
import org.jruby.ir.operands.Float;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.RefinedCachingCallSite;
import org.jruby.util.ArraySupport;

import java.util.Map;

import static org.jruby.ir.IRFlags.*;

public abstract class CallBase extends NOperandInstr implements ClosureAcceptingInstr {
    private static long callSiteCounter = 1;

    public transient final long callSiteId;
    private final CallType callType;
    protected String name;
    protected final transient CallSite callSite;
    protected final transient int argsCount;
    protected final transient boolean hasClosure;

    private transient boolean flagsComputed;
    private transient boolean canBeEval;
    private transient boolean targetRequiresCallersBinding;    // Does this call make use of the caller's binding?
    private transient boolean targetRequiresCallersFrame;    // Does this call make use of the caller's frame?
    private transient boolean dontInline;
    private transient boolean[] splatMap;
    private transient boolean procNew;
    private boolean potentiallyRefined;

    protected CallBase(Operation op, CallType callType, String name, Operand receiver, Operand[] args, Operand closure,
                       boolean potentiallyRefined) {
        super(op, arrayifyOperands(receiver, args, closure));

        this.callSiteId = callSiteCounter++;
        argsCount = args.length;
        hasClosure = closure != null;
        this.name = name;
        this.callType = callType;
        this.callSite = getCallSiteFor(callType, name, potentiallyRefined);
        splatMap = IRRuntimeHelpers.buildSplatMap(args);
        flagsComputed = false;
        canBeEval = true;
        targetRequiresCallersBinding = true;
        targetRequiresCallersFrame = true;
        dontInline = false;
        procNew = false;
        this.potentiallyRefined = potentiallyRefined;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);

        e.encode(getCallType().ordinal());
        e.encode(getName());
        e.encode(getReceiver());
        e.encode(calculateArity());

        for (Operand arg: getCallArgs()) {
            e.encode(arg);
        }

        if (hasClosure) e.encode(getClosureArg(null));

    }

    // FIXME: Convert this to some Signature/Arity method
    // -0 is not possible so we add 1 to arguments with closure so we get a valid negative value.
    private int calculateArity() {
        return hasClosure ? -1*(argsCount + 1) : argsCount;
    }

    public String getName() {
        return name;
    }

    /** From interface ClosureAcceptingInstr */
    public Operand getClosureArg() {
        return hasClosure ? operands[argsCount + 1] : null;
    }

    public Operand getClosureArg(Operand ifUnspecified) {
        return hasClosure ? getClosureArg() : ifUnspecified;
    }

    public Operand getReceiver() {
        return operands[0];
    }

    /**
     * This getter is potentially unsafe if you do not know you have >=1 arguments to the call.  It may return
     * null of the closure argument from operands.
     */
    public Operand getArg1() {
        return operands[1]; // operands layout: receiver, args*, closure
    }

    // FIXME: Maybe rename this.
    public int getArgsCount() {
        return argsCount;
    }

    // Warning: Potentially expensive.  Analysis should be written around retrieving operands.
    public Operand[] getCallArgs() {
        Operand[] callArgs = new Operand[argsCount];
        ArraySupport.copy(operands, 1, callArgs, 0, argsCount);
        return callArgs;
    }

    public CallSite getCallSite() {
        return callSite;
    }

    public CallType getCallType() {
        return callType;
    }

    public boolean[] splatMap() {
        return splatMap;
    }

    public void setProcNew(boolean procNew) {
        this.procNew = procNew;
    }

    public void blockInlining() {
        dontInline = true;
    }

    public boolean inliningBlocked() {
        return dontInline;
    }

    protected static CallSite getCallSiteFor(CallType callType, String name, boolean potentiallyRefined) {
        assert callType != null: "Calltype should never be null";

        if (potentiallyRefined) return new RefinedCachingCallSite(name, callType);

        switch (callType) {
            case NORMAL: return MethodIndex.getCallSite(name);
            case FUNCTIONAL: return MethodIndex.getFunctionalCallSite(name);
            case VARIABLE: return MethodIndex.getVariableCallSite(name);
            case SUPER: return MethodIndex.getSuperCallSite();
            case UNKNOWN:
        }

        return null; // fallthrough for unknown
    }

    public boolean hasLiteralClosure() {
        return getClosureArg() instanceof WrappedIRClosure;
    }

    public static boolean isAllFixnums(Operand[] args) {
        for (Operand argument : args) {
            if (!(argument instanceof Fixnum)) return false;
        }

        return true;
    }

    public static boolean isAllFloats(Operand[] args) {
        for (Operand argument : args) {
            if (!(argument instanceof Float)) return false;
        }

        return true;
    }

    public boolean isPotentiallyRefined() {
        return potentiallyRefined;
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        boolean modifiedScope = false;

        if (targetRequiresCallersBinding()) {
            modifiedScope = true;
            scope.getFlags().add(BINDING_HAS_ESCAPED);
        }

        if (targetRequiresCallersFrame()) {
            modifiedScope = true;
            scope.getFlags().add(REQUIRES_FRAME);
        }

        if (canBeEval()) {
            modifiedScope = true;
            scope.getFlags().add(USES_EVAL);

            // If eval contains a return then a nonlocal may pass through (e.g. def foo; eval "return 1"; end).
            scope.getFlags().add(CAN_RECEIVE_NONLOCAL_RETURNS);

            // If this method receives a closure arg, and this call is an eval that has more than 1 argument,
            // it could be using the closure as a binding -- which means it could be using pretty much any
            // variable from the caller's binding!
            if (scope.getFlags().contains(RECEIVES_CLOSURE_ARG) && argsCount > 1) {
                scope.getFlags().add(CAN_CAPTURE_CALLERS_BINDING);
            }
        }

        // Kernel.local_variables inspects variables.
        // and JRuby implementation uses dyn-scope to access the static-scope
        // to output the local variables => we cannot strip dynscope in those cases.
        // FIXME: We need to decouple static-scope and dyn-scope.
        String mname = getName();
        if (mname.equals("local_variables")) {
            scope.getFlags().add(REQUIRES_DYNSCOPE);
        } else if (potentiallySend(mname) && argsCount >= 1) {
            Operand meth = getArg1();
            if (meth instanceof StringLiteral && "local_variables".equals(((StringLiteral)meth).getString())) {
                scope.getFlags().add(REQUIRES_DYNSCOPE);
            }
        }

        // Refined scopes require dynamic scope in order to get the static scope
        if (potentiallyRefined) scope.getFlags().add(REQUIRES_DYNSCOPE);

        return modifiedScope;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        super.simplifyOperands(valueMap, force);

        // Recompute splatMap
        splatMap = IRRuntimeHelpers.buildSplatMap(getCallArgs()); // also checking receiver but receiver can never be a splat
        flagsComputed = false; // Forces recomputation of flags
    }

    public Operand[] cloneCallArgs(CloneInfo ii) {
        Operand[] clonedArgs = new Operand[argsCount];
        for (int i = 0; i < argsCount; i++) {
            clonedArgs[i] = operands[i+1].cloneForInlining(ii);  // +1 for receiver being operands[0]
        }

        return clonedArgs;
    }

    // SSS FIXME: Are all bases covered?
    // How about aliasing of 'call', 'eval', 'send', 'module_eval', 'class_eval', 'instance_eval'?
    private boolean computeEvalFlag() {
        // ENEBO: This could be made into a recursive two-method thing so then: send(:send, :send, :send, :send, :eval, "Hosed") works
        String mname = getName();
        // checking for "call" is conservative.  It can be eval only if the receiver is a Method
        // CON: Removed "call" check because we didn't do it in 1.7 and it deopts all callers of Method or Proc objects.
        // CON: eval forms with no arguments are block or block pass, and do not need to deopt
        if (
                (mname.equals("eval") ||
                        mname.equals("module_eval") ||
                        mname.equals("class_eval") ||
                        mname.equals("instance_eval")
                ) &&
                        getArgsCount() != 0) {

            return true;
        }

        // Calls to 'send' where the first arg is either unknown or is eval or send (any others?)
        if (potentiallySend(mname) && argsCount >= 1) {
            Operand meth = getArg1();
            if (!(meth instanceof StringLiteral)) return true; // We don't know

            String name = ((StringLiteral) meth).getString();
            // FIXME: ENEBO - Half of these are name and half mname?
            return name.equals("call") || name.equals("eval") || mname.equals("module_eval") ||
                    mname.equals("class_eval") || mname.equals("instance_eval") || name.equals("send") ||
                    name.equals("__send__");
        }

        return false; // All checks passed
    }

    private boolean computeRequiresCallersBindingFlag() {
        if (canBeEval()) return true;

        // literal closures can be used to capture surrounding binding
        if (hasLiteralClosure()) return true;

        String mname = getName();
        if (MethodIndex.SCOPE_AWARE_METHODS.contains(mname)) {
            return true;
        } else if (potentiallySend(mname) && argsCount >= 1) {
            Operand meth = getArg1();
            if (!(meth instanceof StringLiteral)) return true; // We don't know -- could be anything

            return MethodIndex.SCOPE_AWARE_METHODS.contains(((StringLiteral) meth).getString());
        }

        /* -------------------------------------------------------------
         * SSS FIXME: What about aliased accesses to these same methods?
         * See problem snippet below. To be clear, the problem with this
         * Module.nesting below is because that method uses DynamicScope
         * to access the static-scope. However, even if we moved the static-scope
         * to Frame, the problem just shifts over to optimizations that eliminate
         * push/pop of Frame objects from certain scopes.
         *
         * [subbu@earth ~/jruby] cat /tmp/pgm.rb
         * class Module
         *   class << self
         *     alias_method :foobar, :nesting
         *   end
         * end
         *
         * module X
         *   puts "X. Nesting is: #{Module.foobar}"
         * end
         *
         * module Y
         *   puts "Y. Nesting is: #{Module.nesting}"
         * end
         *
         * [subbu@earth ~/jruby] jruby -X-CIR -Xir.passes=OptimizeTempVarsPass,LocalOptimizationPass,AddLocalVarLoadStoreInstructions,AddCallProtocolInstructions,LinearizeCFG /tmp/pgm.rb
         * X. Nesting is: []
         * Y. Nesting is: [Y]
         * [subbu@earth ~/jruby] jruby -X-CIR -Xir.passes=LinearizeCFG /tmp/pgm.rb
         * X. Nesting is: [X]
         * Y. Nesting is: [Y]
         * ------------------------------------------------------------- */

        // SSS FIXME: Are all bases covered?
        return false;  // All checks done -- dont need one
    }

    private boolean computeRequiresCallersFrameFlag() {
        if (canBeEval()) return true;

        // literal closures can be used to capture surrounding binding
        if (hasLiteralClosure()) return true;

        if (procNew) return true;

        String mname = getName();
        if (MethodIndex.FRAME_AWARE_METHODS.contains(mname)) {
            // Known frame-aware methods.
            return true;

        } else if (potentiallySend(mname) && argsCount >= 1) {
            Operand meth = getArg1();
            if (!(meth instanceof StringLiteral)) return true; // We don't know -- could be anything

            return MethodIndex.FRAME_AWARE_METHODS.contains(((StringLiteral) meth).getString());
        }

        return false;
    }

    private static boolean potentiallySend(String name) {
        return name.equals("send") || name.equals("__send__");
    }

    private void computeFlags() {
        // Order important!
        flagsComputed = true;
        canBeEval = computeEvalFlag();
        targetRequiresCallersBinding = canBeEval || computeRequiresCallersBindingFlag();
        targetRequiresCallersFrame = canBeEval || computeRequiresCallersFrameFlag();
    }

    public boolean canBeEval() {
        if (!flagsComputed) computeFlags();

        return canBeEval;
    }

    public boolean targetRequiresCallersBinding() {
        if (!flagsComputed) computeFlags();

        return targetRequiresCallersBinding;
    }

    public boolean targetRequiresCallersFrame() {
        if (!flagsComputed) computeFlags();

        return targetRequiresCallersFrame;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "n:" + getName(), "t:" + callType.toString().substring(0, 2), "cl:"+ hasClosure};
    }

    public static boolean containsArgSplat(Operand[] arguments) {
        for (Operand argument : arguments) {
            if (argument instanceof Splat) return true;
        }

        return false;
    }

    private final static int REQUIRED_OPERANDS = 1;
    private static Operand[] arrayifyOperands(Operand receiver, Operand[] callArgs, Operand closure) {
        Operand[] allArgs = new Operand[callArgs.length + REQUIRED_OPERANDS + (closure != null ? 1 : 0)];

        assert receiver != null : "RECEIVER is null";

        allArgs[0] = receiver;
        for (int i = 0; i < callArgs.length; i++) {
            assert callArgs[i] != null : "ARG " + i + " is null";

            allArgs[i + REQUIRED_OPERANDS] = callArgs[i];
        }

        if (closure != null) allArgs[callArgs.length + REQUIRED_OPERANDS] = closure;

        return allArgs;
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope dynamicScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) getReceiver().retrieve(context, self, currScope, dynamicScope, temp);
        IRubyObject[] values = prepareArguments(context, self, currScope, dynamicScope, temp);
        Block preparedBlock = prepareBlock(context, self, currScope, dynamicScope, temp);

        if (hasLiteralClosure()) {
            try {
                return callSite.call(context, self, object, values, preparedBlock);
            } finally {
                preparedBlock.escape();
            }
        }

        return callSite.call(context, self, object, values, preparedBlock);
    }

    protected IRubyObject[] prepareArguments(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope dynamicScope, Object[] temp) {
        return splatMap != null ?
                prepareArgumentsComplex(context, self, currScope, dynamicScope, temp) :
                prepareArgumentsSimple(context, self, currScope, dynamicScope, temp);
    }

    protected IRubyObject[] prepareArgumentsSimple(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        IRubyObject[] newArgs = new IRubyObject[argsCount];

        for (int i = 0; i < argsCount; i++) { // receiver is operands[0]
            newArgs[i] = (IRubyObject) operands[i+1].retrieve(context, self, currScope, currDynScope, temp);
        }

        return newArgs;
    }

    protected IRubyObject[] prepareArgumentsComplex(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        // ENEBO: we can probably do this more efficiently than using ArrayList
        // SSS: For regular calls, IR builder never introduces splats except as the first argument
        // But when zsuper is converted to SuperInstr with known args, splats can appear anywhere
        // in the list.  So, this looping handles both these scenarios, although if we wanted to
        // optimize for CallInstr which has splats only in the first position, we could do that.

        // CON: Using same logic as super splatting, but this will at least only allocate at
        // most two "carrier" arrays.
        return IRRuntimeHelpers.splatArguments(
                prepareArgumentsSimple(context, self, currScope, currDynScope, temp),
                splatMap);
    }

    public Block prepareBlock(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        if (getClosureArg() == null) return Block.NULL_BLOCK;

        return IRRuntimeHelpers.getBlockFromObject(context, getClosureArg().retrieve(context, self, currScope, currDynScope, temp));
    }
}
