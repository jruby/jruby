package org.jruby.compiler.ir.instructions;

import java.util.Map;
import java.util.HashMap;

import org.jruby.RubyClass;
import org.jruby.RubyMethod;
import org.jruby.RubyProc;
import org.jruby.util.TypeConverter;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.MethodHandle;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.StringLiteral;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.IRClass;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.operands.Nil;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/*
 * args field: [self, receiver, *args]
 */
public class CallInstr extends MultiOperandInstr {
    protected Operand   receiver;
    protected Operand[] arguments;
    protected MethAddr  methAddr;
    protected Operand   closure;

    private boolean flagsComputed;
    private boolean canBeEval;
    private boolean targetRequiresCallersBinding;    // Does this call make use of the caller's binding?
    public HashMap<DynamicMethod, Integer> profile;
    public CallSite callAdapter;
    
    public CallInstr(Variable result, MethAddr methAddr, Operand receiver, Operand[] args, Operand closure) {
        this(Operation.CALL, result, methAddr, receiver, args, closure);
    }

    public CallInstr(Operation op, Variable result, MethAddr methAddr, Operand receiver, Operand[] args, Operand closure) {
        super(op, result);

        this.receiver = receiver;
        this.arguments = args;
        this.methAddr = methAddr;
        this.closure = closure;
        flagsComputed = false;
        canBeEval = true;
        targetRequiresCallersBinding = true;
        callAdapter = MethodIndex.getFunctionalCallSite(methAddr.toString());
    }

    public Operand[] getOperands() {
        return buildAllArgs(methAddr, receiver, arguments, closure);
    }

    public void setMethodAddr(MethAddr mh) {
        this.methAddr = mh;
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

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        receiver = receiver.getSimplifiedOperand(valueMap);
        methAddr = (MethAddr)methAddr.getSimplifiedOperand(valueMap);
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = arguments[i].getSimplifiedOperand(valueMap);
        }
        if (closure != null) closure = closure.getSimplifiedOperand(valueMap);
        flagsComputed = false; // Forces recomputation of flags
    }

    public Operand[] cloneCallArgs(InlinerInfo ii) {
        int length = arguments.length;
        Operand[] clonedArgs = new Operand[length];

        for (int i = 0; i < length; i++) {
            clonedArgs[i] = arguments[i].cloneForInlining(ii);
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
            IRClosure cl = (IRClosure) ((MetaObject) closure).scope;
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
            if (!(object instanceof MetaObject)) return true;

            IRScope c = ((MetaObject) object).scope;
            if ((c instanceof IRClass) && c.getName().equals("Proc")) return true;
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

    public boolean isLVADataflowBarrier() {
        // If the call is an eval, OR if it passes a closure and the callee can capture the caller's binding, we are in trouble
        // We would have to pretty much spill everything at the call site!
        return canBeEval() || targetRequiresCallersBinding();
    }

    @Override
    public String toString() {
        return ""
                + (result == null ? "" : result + " = ")
                + operation + "(" + methAddr + ", " + receiver + ", " +
                java.util.Arrays.toString(getCallArgs())
                + (closure == null ? "" : ", &" + closure) + ")";
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new CallInstr(ii.getRenamedVariable(result), (MethAddr) methAddr.cloneForInlining(ii), receiver.cloneForInlining(ii), cloneCallArgs(ii), closure == null ? null : closure.cloneForInlining(ii));
   }

// --------------- Private methods ---------------

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
    
    private Label interpretMethodHandle(InterpreterContext interp, ThreadContext context, 
            IRubyObject self, MethodHandle mh, IRubyObject[] args) {
        assert mh.getMethodNameOperand() == getReceiver();

        IRubyObject resultValue;
        DynamicMethod m = mh.getResolvedMethod();
        String mn = mh.getResolvedMethodName();
        IRubyObject ro = mh.getReceiverObj();
        if (m.isUndefined()) {
            resultValue = RuntimeHelpers.callMethodMissing(context, ro,
                    m.getVisibility(), mn, CallType.FUNCTIONAL, args, prepareBlock(interp));
        } else {
            try {
                resultValue = m.call(interp.getContext(), ro, ro.getMetaClass(), mn, args,
                        prepareBlock(interp));
            } catch (org.jruby.exceptions.JumpException.BreakJump bj) {
                resultValue = (IRubyObject) bj.getValue();
            }
        }
        
        getResult().store(interp, resultValue);
        return null;        
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        Object ma = methAddr.retrieve(interp);
        IRubyObject[] args = prepareArguments(getCallArgs(), interp);
        
        if (ma instanceof MethodHandle) return interpretMethodHandle(interp, context, self, (MethodHandle) ma, args);

        IRubyObject object = (IRubyObject) getReceiver().retrieve(interp);
        String name = ma.toString(); // SSS FIXME: If this is not a ruby string or a symbol, then this is an error in the source code!
        Object resultValue;
        try {
            resultValue = callAdapter.call(context, self, object, args, prepareBlock(interp));
        } catch (org.jruby.exceptions.JumpException.BreakJump bj) {
            resultValue = (IRubyObject) bj.getValue();
        }

        getResult().store(interp, resultValue);
        return null;
    }

    public Label interpret_with_inline(InterpreterContext interp) {
        Object        ma    = methAddr.retrieve(interp);
        IRubyObject[] args  = prepareArguments(getCallArgs(), interp);
        Object resultValue;
        if (ma instanceof MethodHandle) {
            MethodHandle  mh = (MethodHandle)ma;

            assert mh.getMethodNameOperand() == getReceiver();

            DynamicMethod m  = mh.getResolvedMethod();
            String        mn = mh.getResolvedMethodName();
            IRubyObject   ro = mh.getReceiverObj();
            if (m.isUndefined()) {
                resultValue = RuntimeHelpers.callMethodMissing(interp.getContext(), ro, 
                        m.getVisibility(), mn, CallType.FUNCTIONAL, args, prepareBlock(interp));
            } else {
               ThreadContext tc = interp.getContext();
               RubyClass     rc = ro.getMetaClass();
               if (profile == null) {
                  profile = new HashMap<DynamicMethod, Integer>();
               }
               Integer count = profile.get(m);
               if (count == null) {
                  count = new Integer(1);
               } else {
                  count = new Integer(count + 1);
                  if ((count > 50) && (m instanceof InterpretedIRMethod) && (profile.size() == 1)) {
                     IRMethod inlineableMethod = ((InterpretedIRMethod)m).method;
                     profile.remove(m); // remove it because the interpreter might ignore this hint
                     throw new org.jruby.interpreter.InlineMethodHint(inlineableMethod);
                  }
               }
               profile.put(m, count);
               resultValue = m.call(tc, ro, rc, mn, args, prepareBlock(interp));
            }
        } else {
           IRubyObject object = (IRubyObject) getReceiver().retrieve(interp);
           String name = ma.toString(); // SSS FIXME: If this is not a ruby string or a symbol, then this is an error in the source code!

           resultValue = object.callMethod(interp.getContext(), name, args, prepareBlock(interp));
        }

        getResult().store(interp, resultValue);
        return null;
    }

    protected Block prepareBlock(InterpreterContext interp) {
        if (closure == null) return Block.NULL_BLOCK;
        Object value = closure.retrieve(interp);
        if (value instanceof Block)
            return (Block)value;
        else if (value instanceof RubyProc)
            return ((RubyProc) value).getBlock();
        else if (value instanceof RubyMethod)
            return ((RubyProc)((RubyMethod)value).to_proc(interp.getContext(), null)).getBlock();
        else if ((value instanceof IRubyObject) && ((IRubyObject)value).isNil())
            return Block.NULL_BLOCK;
        else if (value instanceof IRubyObject)
            return ((RubyProc)TypeConverter.convertToType((IRubyObject)value, interp.getRuntime().getProc(), "to_proc", true)).getBlock();
        else
            throw new RuntimeException("Unhandled case in CallInstr:prepareBlock.  Got block arg: " + value);
    }
}
