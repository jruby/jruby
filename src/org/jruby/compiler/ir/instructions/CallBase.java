package org.jruby.compiler.ir.instructions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.jruby.compiler.ir.IRBody;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.calladapter.CallAdapter;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.Nil;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.StringLiteral;
import org.jruby.compiler.ir.operands.WrappedIRModule;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.CallType;

/**
 *
 */
public abstract class CallBase extends Instr {
    protected Operand   receiver;
    protected Operand[] arguments;
    protected Operand   closure;
    protected CallAdapter callAdapter = null;
    protected MethAddr methAddr;
    private final CallType callType;
    
    private boolean flagsComputed;
    private boolean canBeEval;
    private boolean targetRequiresCallersBinding;    // Does this call make use of the caller's binding?
    public HashMap<DynamicMethod, Integer> profile;

    protected CallBase(Operation op, CallType callType, MethAddr methAddr, Operand receiver, Operand[] args, Operand closure) {
        super(op);

        this.receiver = receiver;
        this.arguments = args;
        this.closure = closure;
        this.methAddr = methAddr;
        this.callType = callType;
        flagsComputed = false;
        canBeEval = true;
        targetRequiresCallersBinding = true;
    }

    public Operand[] getOperands() {
        return buildAllArgs(getMethodAddr(), receiver, arguments, closure);
    }

	 public CallAdapter getCallAdapter() {
		 return callAdapter;
	 }

    public MethAddr getMethodAddr() {
        return methAddr;
    }

    public Operand getClosureArg() {
        // ENEBO: We should not be passing nulls 
        return closure == null ? Nil.NIL : closure;
    }

    public Operand getReceiver() {
        return receiver;
    }

    public Operand[] getCallArgs() {
        return arguments;
    }
    
    public CallType getCallType() {
        return callType;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        // FIXME: receiver should never be null (checkArity seems to be one culprit)
        if (receiver != null) receiver = receiver.getSimplifiedOperand(valueMap, force);
        methAddr = (MethAddr)methAddr.getSimplifiedOperand(valueMap, force);
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = arguments[i].getSimplifiedOperand(valueMap, force);
        }
        if (closure != null) closure = closure.getSimplifiedOperand(valueMap, force);
        flagsComputed = false; // Forces recomputation of flags

        // recompute call adapter whenever instr operands change!
        callAdapter = CallAdapter.createFor(callType, methAddr, arguments, closure);
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
    private boolean getEvalFlag() {
        // ENEBO: This could be made into a recursive two-method thing so then: send(:send, :send, :send, :send, :eval, "Hosed") works
        String mname = getMethodAddr().getName();
        // checking for "call" is conservative.  It can be eval only if the receiver is a Method
        if (mname.equals("call") || mname.equals("eval")) return true;

        // Calls to 'send' where the first arg is either unknown or is eval or send (any others?)
        if (mname.equals("send") || mname.equals("__send__")) {
            Operand[] args = getCallArgs();
            if (args.length >= 2) {
                Operand meth = args[0];
                if (!(meth instanceof StringLiteral)) return true; // We don't know

                // But why?  Why are you killing yourself (and us) doing this?
                String name = ((StringLiteral) meth)._str_value;
                if (name.equals("call") || name.equals("eval") || name.equals("send") || name.equals("__send__")) return true;
            }
        }

        return false; // All checks passed
    }

    private boolean computeRequiresCallersBindingFlag() {
        if (canBeEval() /*|| canCaptureCallersBinding()*/) return true;

        if (closure != null) {
            /****
            IRClosure cl = (IRClosure) ((WrappedIRClosure) closure).scope;
            if (cl.requiresBinding()) return true;
            ****/
            // SSS FIXME: This is conservative!
            return true;
        }

        // Check if we are calling Proc.new or lambda
        String mname = getMethodAddr().getName();
        if (mname.equals("lambda")) {
            return true;
        } else if (mname.equals("new")) {
            Operand object = getReceiver();

            // Unknown receiver -- could be Proc!!
            if (!(object instanceof WrappedIRModule)) return true;

            IRBody c = ((WrappedIRModule) object).getModule();
            if (c != null && c.isClass() && c.getName().equals("Proc")) return true;
        }

        // SSS FIXME: Are all bases covered?
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

    public boolean isDataflowBarrier() {
        // If the call is an eval, OR if it passes a closure and the callee can capture the caller's binding,
        // we cannot propagate dataflow analysis information across it (in either direction), except where
        // the dataflow analysis has additional information for ignoring this barrier. 
        //
        // return canBeEval() || targetRequiresCallersBinding();
        //
        // SSS FIXME: For now, force all calls with closures to be dataflow barriers
        //
        // return canBeEval() || (closure != null);
        //
        // Argh! If the current method has threading code in there, all local variables have effectively escaped
        // into the new thread which means all calls in this method downstream of the threading code are dataflow
        // barriers.
        //
        // SSS FIXME: For now, force all calls to be dataflow barriers
        return true;
    }

    @Override
    public String toString() {
        return "" + getOperation() + "(" + getMethodAddr() + ", " + receiver +
                ", " + Arrays.toString(getCallArgs()) + 
                (closure == null ? "" : ", &" + closure) + ")";
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
}
