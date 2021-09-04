package org.jruby.ir.instructions;

import org.jruby.RubySymbol;
import org.jruby.anno.FrameField;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRManager;
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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.jruby.ir.IRFlags.*;

public abstract class CallBase extends NOperandInstr implements ClosureAcceptingInstr, Site {
    public static long callSiteCounter = 1;
    private static final EnumSet<FrameField> ALL = EnumSet.allOf(FrameField.class);

    public transient long callSiteId;
    private final CallType callType;
    protected final RubySymbol name;
    protected final transient CallSite callSite;
    protected final transient int argsCount;
    protected final transient boolean hasClosure;

    private transient boolean flagsComputed;
    private transient boolean canBeEval;
    private transient boolean targetRequiresCallersBinding;    // Does this call make use of the caller's binding?
    private transient boolean targetRequiresCallersFrame;    // Does this call make use of the caller's frame?
    private transient boolean dontInline;
    private transient boolean[] splatMap;
    protected transient boolean procNew;
    private final boolean potentiallyRefined;
    private transient Set<FrameField> frameReads;
    private transient Set<FrameField> frameWrites;

    // main constructor
    protected CallBase(IRScope scope, Operation op, CallType callType, RubySymbol name, Operand receiver,
                       Operand[] args, Operand closure, boolean potentiallyRefined) {
        this(scope, op, callType, name, receiver, args, closure, potentiallyRefined, null, callSiteCounter++);
    }

    // clone constructor
    protected CallBase(IRScope scope, Operation op, CallType callType, RubySymbol name, Operand receiver,
                       Operand[] args, Operand closure, boolean potentiallyRefined, CallSite callSite, long callSiteId) {
        super(op, arrayifyOperands(receiver, args, closure));

        this.callSiteId = callSiteId;
        argsCount = args.length;
        hasClosure = closure != null;
        this.name = name;
        this.callType = callType;
        this.callSite = callSite == null ? getCallSiteFor(scope, callType, name.idString(), callSiteId, hasLiteralClosure(), potentiallyRefined) : callSite;
        splatMap = IRRuntimeHelpers.buildSplatMap(args);
        flagsComputed = false;
        canBeEval = true;
        targetRequiresCallersBinding = true;
        targetRequiresCallersFrame = true;
        dontInline = false;
        procNew = false;
        this.potentiallyRefined = potentiallyRefined;

        captureFrameReadsAndWrites();
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

    /**
     * raw identifier string (used by method table).
     */
    public String getId() {
        return name.idString();
    }

    public long getCallSiteId() {
        return callSiteId;
    }

    public void setCallSiteId(long callSiteId) {
        this.callSiteId = callSiteId;
    }

    public RubySymbol getName() {
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

    // CallInstr and descendents have results and this method is obvious.
    // NoResultCallInstr still provides an impl (returns null) to make processing in JIT simpler.
    public abstract Variable getResult();

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

    protected static CallSite getCallSiteFor(IRScope scope, CallType callType, String name, long callsiteId, boolean hasLiteralClosure, boolean potentiallyRefined) {
        assert callType != null: "Calltype should never be null";

        if (potentiallyRefined) return new RefinedCachingCallSite(name, scope.getStaticScope(), callType);

        switch (callType) {
            case NORMAL:
                if (IRManager.IR_INLINER && hasLiteralClosure) {
                    return MethodIndex.getProfilingCallSite(callType, name, scope, callsiteId);
                } else {
                    return MethodIndex.getCallSite(name);
                }
            case FUNCTIONAL:
                if (IRManager.IR_INLINER && hasLiteralClosure) {
                    return MethodIndex.getProfilingCallSite(callType, name, scope, callsiteId);
                } else {
                    return MethodIndex.getFunctionalCallSite(name);
                }
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
    public boolean computeScopeFlags(IRScope scope, EnumSet<IRFlags> flags) {
        boolean modifiedScope = super.computeScopeFlags(scope, flags);

        if (targetRequiresCallersBinding()) {
            modifiedScope = true;
            flags.add(BINDING_HAS_ESCAPED);
        }

        modifiedScope |= setIRFlagsFromFrameFields(flags, frameReads);
        modifiedScope |= setIRFlagsFromFrameFields(flags, frameWrites);

        // literal closures can be used to capture surrounding binding
        if (hasLiteralClosure()) {
            modifiedScope = true;
            flags.addAll(IRFlags.REQUIRE_ALL_FRAME_FIELDS);
        }

        if (procNew) {
            modifiedScope = true;
            flags.add(IRFlags.REQUIRES_BLOCK);
        }

        if (canBeEval()) {
            modifiedScope = true;
            scope.setUsesEval();

            // If eval contains a return then a nonlocal may pass through (e.g. def foo; eval "return 1"; end).
            scope.setCanReceiveNonlocalReturns();

            // If this method receives a closure arg, and this call is an eval that has more than 1 argument,
            // it could be using the closure as a binding -- which means it could be using pretty much any
            // variable from the caller's binding!
            if (scope.receivesClosureArg() && argsCount > 1) scope.setCanCaptureCallersBinding();
        }

        if (potentiallySend(getId(), argsCount)) { // ok to look at raw string since we know we are looking for 7bit names.
            Operand meth = getArg1();

            if (isPotentiallyRefined()) {
                // send within a refined scope needs to reflect refinements
                modifiedScope = true;
                flags.add(REQUIRES_DYNSCOPE);
            }

            if (meth instanceof MutableString) {
                // This logic is intended to reduce the framing impact of send if we can
                // statically determine the sent name and we know it does not need to be
                // either framed or scoped. Previously it only did this logic for
                // send(:local_variables).

                // This says getString but I believe this will also always be an id string in this case so it is ok.
                String sendName = ((MutableString) meth).getString();
                if (MethodIndex.SCOPE_AWARE_METHODS.contains(sendName)) {
                    modifiedScope = true;
                    flags.add(REQUIRES_DYNSCOPE);
                }

                if (MethodIndex.FRAME_AWARE_METHODS.contains(sendName)) {
                    modifiedScope = true;
                    flags.addAll(IRFlags.REQUIRE_ALL_FRAME_EXCEPT_SCOPE);
                }
            } else {
                modifiedScope = true;
                flags.addAll(IRFlags.REQUIRE_ALL_FRAME_FIELDS);
            }
        }

        return modifiedScope;
    }

    private boolean setIRFlagsFromFrameFields(EnumSet<IRFlags> flags, Set<FrameField> frameFields) {
        boolean modifiedScope = false;

        for (FrameField field : frameFields) {
            modifiedScope = true;

            switch (field) {
                case LASTLINE: flags.add(IRFlags.REQUIRES_LASTLINE); break;
                case BACKREF: flags.add(IRFlags.REQUIRES_BACKREF); break;
                case VISIBILITY: flags.add(IRFlags.REQUIRES_VISIBILITY); break;
                case BLOCK: flags.add(IRFlags.REQUIRES_BLOCK); break;
                case SELF: flags.add(IRFlags.REQUIRES_SELF); break;
                case METHODNAME: flags.add(IRFlags.REQUIRES_METHODNAME); break;
                case LINE: flags.add(IRFlags.REQUIRES_LINE); break;
                case CLASS: flags.add(IRFlags.REQUIRES_CLASS); break;
                case FILENAME: flags.add(IRFlags.REQUIRES_FILENAME); break;
                case SCOPE: flags.add(IRFlags.REQUIRES_SCOPE); break;
            }
        }

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
        String mname = getId();
        // checking for "call" is conservative.  It can be eval only if the receiver is a Method
        // CON: Removed "call" check because we didn't do it in 1.7 and it deopts all callers of Method or Proc objects.
        // CON: eval forms with no arguments are block or block pass, and do not need to deopt
        if (getArgsCount() != 0 && (mname.equals("eval") || mname.equals("module_eval") ||
                mname.equals("class_eval") || mname.equals("instance_eval"))) {
            return true;
        }

        // Calls to 'send' where the first arg is either unknown or is eval or send (any others?)
        if (potentiallySend(mname, argsCount)) {
            Operand meth = getArg1();
            if (!(meth instanceof MutableString)) return true; // We don't know

            String name = ((MutableString) meth).getString();
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

        String mname = getId();
        if (MethodIndex.SCOPE_AWARE_METHODS.contains(mname)) {
            return true;
        } else if (potentiallySend(mname, argsCount)) {
            Operand meth = getArg1();
            if (!(meth instanceof MutableString)) return true; // We don't know -- could be anything

            return MethodIndex.SCOPE_AWARE_METHODS.contains(((MutableString) meth).getString());
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

        String mname = getId();
        if (frameReads.size() > 0 || frameWrites.size() > 0) {
            // Known frame-aware methods.
            return true;

        } else if (potentiallySend(mname, argsCount)) {
            Operand meth = getArg1();
            String name;
            if (meth instanceof Stringable) {
                name = ((Stringable) meth).getString();
            } else {
                return true; // We don't know -- could be anything
            }

            frameReads = MethodIndex.METHOD_FRAME_READS.getOrDefault(name, Collections.EMPTY_SET);
            frameWrites = MethodIndex.METHOD_FRAME_WRITES.getOrDefault(name, Collections.EMPTY_SET);

            if (frameReads.size() > 0 || frameWrites.size() > 0) {
                return true;
            }
        }

        return false;
    }

    private static boolean potentiallySend(String name, int argsCount) {
        return (name.equals("send") || name.equals("__send__") || name.equals("public_send")) && argsCount >= 1;
    }

    /**
     * Determine based on the method name what frame fields it is likely to need.
     */
    private void captureFrameReadsAndWrites() {
        // grab a reference to frame fields this method name is known to be associated with
        if (potentiallySend(getId(), argsCount)) {
            // Might be a #send, use the frame reads and writes of what it might call
            Operand meth = getArg1();
            String aliasName;
            if (meth instanceof Stringable) {
                aliasName = ((Stringable) meth).getString();
                frameReads = MethodIndex.METHOD_FRAME_READS.getOrDefault(aliasName, Collections.EMPTY_SET);
                frameWrites = MethodIndex.METHOD_FRAME_WRITES.getOrDefault(aliasName, Collections.EMPTY_SET);
            } else {
                // We don't know -- could be anything
                frameReads = ALL;
                frameWrites = ALL;
            }
        } else {
            frameReads = MethodIndex.METHOD_FRAME_READS.getOrDefault(getId(), Collections.EMPTY_SET);
            frameWrites = MethodIndex.METHOD_FRAME_WRITES.getOrDefault(getId(), Collections.EMPTY_SET);
        }
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
            return callSite.callIter(context, self, object, values, preparedBlock);
        }

        return callSite.call(context, self, object, values, preparedBlock);
    }

    public IRubyObject[] prepareArguments(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope dynamicScope, Object[] temp) {
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

        if (potentiallyRefined) {
            return IRRuntimeHelpers.getRefinedBlockFromObject(context, currScope, getClosureArg().retrieve(context, self, currScope, currDynScope, temp));
        } else {
            return IRRuntimeHelpers.getBlockFromObject(context, getClosureArg().retrieve(context, self, currScope, currDynScope, temp));
        }
    }
}
