package org.jruby.ir.instructions;

import org.jruby.RubyArray;
import org.jruby.RubyMethod;
import org.jruby.RubyProc;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.ImmutableLiteral;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.TypeConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class CallBase extends Instr implements Specializeable {
    protected Operand   receiver;
    protected Operand[] arguments;
    protected Operand   closure;
    protected MethAddr methAddr;
    protected CallSite callSite;
    private final CallType callType;
    
    private boolean flagsComputed;
    private boolean canBeEval;
    private boolean targetRequiresCallersBinding;    // Does this call make use of the caller's binding?
    public HashMap<DynamicMethod, Integer> profile;
    private boolean dontInline;
    private boolean containsSplat;

    protected CallBase(Operation op, CallType callType, MethAddr methAddr, Operand receiver, Operand[] args, Operand closure) {
        super(op);

        this.receiver = receiver;
        this.arguments = args;
        this.closure = closure;
        this.methAddr = methAddr;
        this.callType = callType;
        this.callSite = getCallSiteFor(callType, methAddr);
        containsSplat = containsSplat(args);
        flagsComputed = false;
        canBeEval = true;
        targetRequiresCallersBinding = true;
        dontInline = false;

    }

    public Operand[] getOperands() {
        return buildAllArgs(getMethodAddr(), receiver, arguments, closure);
    }

    public MethAddr getMethodAddr() {
        return methAddr;
    }

    public Operand getClosureArg(Operand ifUnspecified) {
        return closure == null ? ifUnspecified : closure;
    }

    public Operand getReceiver() {
        return receiver;
    }

    public Operand[] getCallArgs() {
        return arguments;
    }
    
    public CallSite getCallSite() {
        return callSite;
    }
    
    public CallType getCallType() {
        return callType;
    }

    public void blockInlining() {
        dontInline = true;
    }

    public boolean inliningBlocked() {
        return dontInline;
    }
    
    private static CallSite getCallSiteFor(CallType callType, MethAddr methAddr) {
        assert callType != null: "Calltype should never be null";
        
        String name = methAddr.toString();
        
        switch (callType) {
            case NORMAL: return MethodIndex.getCallSite(name);
            case FUNCTIONAL: return MethodIndex.getFunctionalCallSite(name);
            case VARIABLE: return MethodIndex.getVariableCallSite(name);
            case SUPER: return MethodIndex.getSuperCallSite();
            case UNKNOWN:
        }
        
        return null; // fallthrough for unknown
    }
    
    public boolean hasClosure() {
        return closure != null;
    }
    
    public boolean isAllConstants() {
        for (int i = 0; i < arguments.length; i++) {
            if (!(arguments[i] instanceof ImmutableLiteral)) return false;
        }
        
        return true;
    }
    
    public boolean isAllFixnums() {
        for (int i = 0; i < arguments.length; i++) {
            if (!(arguments[i] instanceof Fixnum)) return false;
        }
        
        return true;
    }    
    
    /**
     * Interpreter can ask the instruction if it knows how to make a more
     * efficient instruction for direct interpretation.
     * 
     * @return itself or more efficient but semantically equivalent instr
     */
    public CallBase specializeForInterpretation() {
        return this;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        // FIXME: receiver should never be null (checkArity seems to be one culprit)
        if (receiver != null) receiver = receiver.getSimplifiedOperand(valueMap, force);
        methAddr = (MethAddr)methAddr.getSimplifiedOperand(valueMap, force);
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = arguments[i].getSimplifiedOperand(valueMap, force);
        }

        // Recompute containsSplat flag
        containsSplat = containsSplat(arguments);

        if (closure != null) closure = closure.getSimplifiedOperand(valueMap, force);
        flagsComputed = false; // Forces recomputation of flags

        // recompute whenever instr operands change! (can this really change though?)
        callSite = getCallSiteFor(callType, methAddr);
    }

    public Operand[] cloneCallArgs(InlinerInfo ii) {
        int i = 0;
        Operand[] clonedArgs = new Operand[arguments.length];

        for (Operand a: arguments) {
            clonedArgs[i++] = a.cloneForInlining(ii);
        }

        return clonedArgs;
    }

    public boolean isRubyInternalsCall() {
        return false;
    }

    public boolean isStaticCallTarget() {
        return false;
    }

    // SSS: Unused method
    // Can this call lead to ruby code getting modified?  
    // If we don't know what method we are calling, we assume it can (pessimistic, but safe!)
    public boolean canModifyCode() {
        return true;
    }

    // SSS FIXME: Are all bases covered?
    // How about aliasing of 'call', 'eval', 'send', 'module_eval', 'class_eval', 'instance_eval'?
    private boolean getEvalFlag() {
        // ENEBO: This could be made into a recursive two-method thing so then: send(:send, :send, :send, :send, :eval, "Hosed") works
        String mname = getMethodAddr().getName();
        // checking for "call" is conservative.  It can be eval only if the receiver is a Method
        if (mname.equals("call") || mname.equals("eval") || mname.equals("module_eval") || mname.equals("class_eval") || mname.equals("instance_eval")) return true;

        // Calls to 'send' where the first arg is either unknown or is eval or send (any others?)
        if (mname.equals("send") || mname.equals("__send__")) {
            Operand[] args = getCallArgs();
            if (args.length >= 2) {
                Operand meth = args[0];
                if (!(meth instanceof StringLiteral)) return true; // We don't know

                String name = ((StringLiteral) meth).string;
                if (   name.equals("call") 
                    || name.equals("eval") 
                    || mname.equals("module_eval") 
                    || mname.equals("class_eval") 
                    || mname.equals("instance_eval") 
                    || name.equals("send") 
                    || name.equals("__send__")) return true;
            }
        }

        return false; // All checks passed
    }

    private boolean computeRequiresCallersBindingFlag() {
        if (canBeEval()) return true;

        // Conservative -- assuming that the callee will save the closure
        // and use it at a later point.
        if (closure != null) return true;

        String mname = getMethodAddr().getName();
        if (mname.equals("lambda")) {
            return true;
/**
 * SSS: Not required currently.  You cannot Proc.new without passing it a closure
 * which means it will be captured by the check earlier.
 *
        } else if (mname.equals("new")) {
            Operand object = getReceiver();

            // SSS FIXME: This check is incorrect -- something has gone
            // wrong with the numerous fixes to IR code since this check was written.
            //
            // Unknown receiver -- could be Proc!!
            if (!(object instanceof CurrentScope)) return true;

            IRScope c = ((CurrentScope) object).getScope();
            if (c != null && c instanceof IRClassBody && c.getName().equals("Proc")) return true;
**/
        } else if (mname.equals("binding")) {
            return true;
        } else if (mname.equals("send") || mname.equals("__send__")) {
            Operand[] args = getCallArgs();
            if (args.length >= 1) {
                Operand meth = args[0];
                if (!(meth instanceof StringLiteral)) return true; // We don't know -- could be "binding"

                String name = ((StringLiteral) meth).string;
                if (name.equals("binding")) return true;
            }
        }

        // SSS FIXME: Are all bases covered?  What about aliases?
        return false;  // All checks done -- dont need one
    }

    private void computeFlags() {
        // Order important!
        flagsComputed = true;
        canBeEval = getEvalFlag();
        targetRequiresCallersBinding = canBeEval ? true : computeRequiresCallersBindingFlag();
    }

    public boolean canBeEval() {
        if (!flagsComputed) computeFlags();

        return canBeEval;
    }

    public boolean targetRequiresCallersBinding() {
        if (!flagsComputed) computeFlags();

        return targetRequiresCallersBinding;
    }

    // Regexp and IO calls can do this -- and since we do not know at IR-build time 
    // what the call target is, we have to conservatively assume yes
    public boolean canSetDollarVars() {
        return true;
    }

    @Override
    public String toString() {
        return "" + getOperation()  + "(" + callType + ", " + getMethodAddr() + ", " + receiver +
                ", " + Arrays.toString(getCallArgs()) +
                (closure == null ? "" : ", &" + closure) + ")";
    }

    protected static boolean containsSplat(Operand[] arguments) {
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] instanceof Splat) return true;
        }

        return false;
    }

    private static Operand[] buildAllArgs(Operand methAddr, Operand receiver, Operand[] callArgs, Operand closure) {
        Operand[] allArgs = new Operand[callArgs.length + 2 + ((closure != null) ? 1 : 0)];

        assert methAddr != null : "METHADDR is null";
        assert receiver != null : "RECEIVER is null";


        allArgs[0] = methAddr;
        allArgs[1] = receiver;
        for (int i = 0; i < callArgs.length; i++) {
            assert callArgs[i] != null : "ARG " + i + " is null";

            allArgs[i + 2] = callArgs[i];
        }

        if (closure != null) allArgs[callArgs.length + 2] = closure;

        return allArgs;
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope dynamicScope, IRubyObject self, Object[] temp, Block block) {
        IRubyObject object = (IRubyObject) receiver.retrieve(context, self, dynamicScope, temp);
        IRubyObject[] values = prepareArguments(context, self, arguments, dynamicScope, temp);
        Block preparedBlock = prepareBlock(context, self, dynamicScope, temp);
        
        return callSite.call(context, self, object, values, preparedBlock);
    }

    protected IRubyObject[] prepareArguments(ThreadContext context, IRubyObject self, Operand[] arguments, DynamicScope dynamicScope, Object[] temp) {
        return containsSplat ? 
                prepareArgumentsComplex(context, self, arguments, dynamicScope, temp) :
                prepareArgumentsSimple(context, self, arguments, dynamicScope, temp);
    }

    protected IRubyObject[] prepareArgumentsSimple(ThreadContext context, IRubyObject self, Operand[] args, DynamicScope currDynScope, Object[] temp) {
        IRubyObject[] newArgs = new IRubyObject[args.length];

        for (int i = 0; i < args.length; i++) {
            newArgs[i] = (IRubyObject) args[i].retrieve(context, self, currDynScope, temp);
        }

        return newArgs;
    }

    protected IRubyObject[] prepareArgumentsComplex(ThreadContext context, IRubyObject self, Operand[] args, DynamicScope currDynScope, Object[] temp) {
        // SSS: For regular calls, IR builder never introduces splats except as the first argument
        // But when zsuper is converted to SuperInstr with known args, splats can appear anywhere
        // in the list.  So, this looping handles both these scenarios, although if we wanted to
        // optimize for CallInstr which has splats only in the first position, we could do that.
        List<IRubyObject> argList = new ArrayList<IRubyObject>();
        for (int i = 0; i < args.length; i++) {
            IRubyObject rArg = (IRubyObject)args[i].retrieve(context, self, currDynScope, temp);
            if (args[i] instanceof Splat) {
                argList.addAll(Arrays.asList(((RubyArray)rArg).toJavaArray()));
            } else {
                argList.add(rArg);
            }
        }

        return argList.toArray(new IRubyObject[argList.size()]);
    }
   
    protected Block prepareBlock(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        if (closure == null) return Block.NULL_BLOCK;
        
        Object value = closure.retrieve(context, self, currDynScope, temp);
        
        Block block;
        if (value instanceof Block) {
            block = (Block) value;
        } else if (value instanceof RubyProc) {
            block = ((RubyProc) value).getBlock();
        } else if (value instanceof RubyMethod) {
            block = ((RubyProc)((RubyMethod)value).to_proc(context, null)).getBlock();
        } else if ((value instanceof IRubyObject) && ((IRubyObject)value).isNil()) {
            block = Block.NULL_BLOCK;
        } else if (value instanceof IRubyObject) {
            block = ((RubyProc)TypeConverter.convertToType((IRubyObject)value, context.getRuntime().getProc(), "to_proc", true)).getBlock();
        } else {
            throw new RuntimeException("Unhandled case in CallInstr:prepareBlock.  Got block arg: " + value);
        }

        // ENEBO: This came from duplicated logic from SuperInstr....
        // Blocks passed in through calls are always normal blocks, no matter where they came from
        block.type = Block.Type.NORMAL;
        
        return block;
    }
}
