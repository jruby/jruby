package org.jruby.ir.instructions;

import org.jruby.RubyArray;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.jruby.ir.IRFlags.*;

public abstract class CallBase extends Instr implements ClosureAcceptingInstr {
    private static long callSiteCounter = 1;

    public final long callSiteId;
    private final CallType callType;
    protected String name;
    protected CallSite callSite;
    protected int argsCount;
    protected boolean hasClosure;

    private boolean flagsComputed;
    private boolean canBeEval;
    private boolean targetRequiresCallersBinding;    // Does this call make use of the caller's binding?
    private boolean targetRequiresCallersFrame;    // Does this call make use of the caller's frame?
    private boolean dontInline;
    private boolean containsArgSplat;
    private boolean procNew;

    protected CallBase(Operation op, CallType callType, String name, Operand receiver, Operand[] args, Operand closure) {
        super(op, getOperands(receiver, args, closure));

        this.callSiteId = callSiteCounter++;
        argsCount = args.length;
        hasClosure = closure != null;
        this.name = name;
        this.callType = callType;
        this.callSite = getCallSiteFor(callType, name);
        containsArgSplat = containsArgSplat(args);
        flagsComputed = false;
        canBeEval = true;
        targetRequiresCallersBinding = true;
        targetRequiresCallersFrame = true;
        dontInline = false;
        procNew = false;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);

        e.encode(getCallType().ordinal());
        e.encode(getName());
        e.encode(getReceiver());
        e.encode(calculateArity(getCallArgs(), hasClosure));

        for (Operand arg: getCallArgs()) {
            e.encode(arg);
        }

        if (hasClosure) e.encode(getClosureArg(null));

    }

    // FIXME: Convert this to some Signature/Arity method
    // -0 is not possible so we add 1 to arguments with closure so we get a valid negative value.
    private int calculateArity(Operand[] arguments, boolean hasClosure) {
        return hasClosure ? -1*(arguments.length + 1) : arguments.length;
    }

    private static Operand[] getOperands(Operand receiver, Operand[] arguments, Operand closure) {
        return buildAllArgs(receiver, arguments, closure);
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

        System.arraycopy(operands, 1, callArgs, 0, argsCount);

        return callArgs;
    }

    public CallSite getCallSite() {
        return callSite;
    }

    public CallType getCallType() {
        return callType;
    }

    public boolean containsArgSplat() {
        return containsArgSplat;
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

    private static CallSite getCallSiteFor(CallType callType, String name) {
        assert callType != null: "Calltype should never be null";

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

        return modifiedScope;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        super.simplifyOperands(valueMap, force);

        // Recompute containsArgSplat flag
        containsArgSplat = containsArgSplat(operands); // also checking receiver but receiver can never be a splat
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
        if (/*mname.equals("call") ||*/ mname.equals("eval") || mname.equals("module_eval") ||
                mname.equals("class_eval") || mname.equals("instance_eval")) return true;

        // Calls to 'send' where the first arg is either unknown or is eval or send (any others?)
        if (potentiallySend(mname) && argsCount >= 1) {
            Operand meth = getArg1();
            if (!(meth instanceof StringLiteral)) return true; // We don't know

            String name = ((StringLiteral) meth).string;
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
    private static Operand[] buildAllArgs(Operand receiver, Operand[] callArgs, Operand closure) {
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

        return callSite.call(context, self, object, values, preparedBlock);
    }

    protected IRubyObject[] prepareArguments(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope dynamicScope, Object[] temp) {
        return containsArgSplat ?
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
        List<IRubyObject> argList = new ArrayList<>(argsCount * 2);
        for (int i = 0; i < argsCount; i++) { // receiver is operands[0]
            IRubyObject rArg = (IRubyObject) operands[i+1].retrieve(context, self, currScope, currDynScope, temp);
            if (operands[i+1] instanceof Splat) {
                RubyArray array = (RubyArray) rArg;
                for (int j = 0; j < array.size(); j++) {
                    argList.add(array.eltOk(j));
                }
            } else {
                argList.add(rArg);
            }
        }

        return argList.toArray(new IRubyObject[argList.size()]);
    }

    public Block prepareBlock(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        if (getClosureArg() == null) return Block.NULL_BLOCK;

        return IRRuntimeHelpers.getBlockFromObject(context, getClosureArg().retrieve(context, self, currScope, currDynScope, temp));
    }
}