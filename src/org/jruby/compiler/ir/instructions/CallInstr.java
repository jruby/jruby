package org.jruby.compiler.ir.instructions;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.jruby.RubyArray;
import org.jruby.RubyMethod;
import org.jruby.RubyProc;
import org.jruby.util.TypeConverter;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.MethodHandle;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Splat;
import org.jruby.compiler.ir.operands.StringLiteral;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.IRClass;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.operands.Nil;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.internal.runtime.methods.DynamicMethod;
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
public class CallInstr extends Instr {
    protected Operand   receiver;
    protected Operand[] arguments;
    protected MethAddr  methAddr;
    protected Operand   closure;
    protected CallType  callType;
    public CallSite callAdapter;

    private boolean flagsComputed;
    private boolean canBeEval;
    private boolean targetRequiresCallersBinding;    // Does this call make use of the caller's binding?
    public HashMap<DynamicMethod, Integer> profile;
    
    public static CallInstr create(Variable result, MethAddr methAddr, Operand receiver, Operand[] args, Operand closure) {
        return new CallInstr(CallType.NORMAL, result, methAddr, receiver, args, closure);
    }
    
    public static CallInstr create(CallType callType, Variable result, MethAddr methAddr, Operand receiver, Operand[] args, Operand closure) {
        return new CallInstr(callType, result, methAddr, receiver, args, closure);
    }
    
    public CallInstr(CallType callType, Variable result, MethAddr methAddr, Operand receiver, Operand[] args, Operand closure) {
        this(Operation.CALL, callType, result, methAddr, receiver, args, closure);
    }

    protected CallInstr(Operation op, CallType callType, Variable result, MethAddr methAddr, Operand receiver, Operand[] args, Operand closure) {
        super(op, result);

        this.receiver = receiver;
        this.arguments = args;
        this.methAddr = methAddr;
        this.closure = closure;
        this.callType = callType;
        flagsComputed = false;
        canBeEval = true;
        targetRequiresCallersBinding = true;
        if (callType != null) {
            switch (callType) {
                case NORMAL    : callAdapter = MethodIndex.getCallSite(methAddr.toString()); break;
                case FUNCTIONAL: callAdapter = MethodIndex.getFunctionalCallSite(methAddr.toString()); break;
                case VARIABLE  : callAdapter = MethodIndex.getVariableCallSite(methAddr.toString()); break;
                case SUPER     : callAdapter = MethodIndex.getSuperCallSite(); break;
            }
        }
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
    
    public CallType getCallType() {
        return callType;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        // FIXME: receiver should never be null (checkArity seems to be one culprit)
        if (receiver != null) receiver = receiver.getSimplifiedOperand(valueMap);
        methAddr = (MethAddr)methAddr.getSimplifiedOperand(valueMap);
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = arguments[i].getSimplifiedOperand(valueMap);
        }
        if (closure != null) closure = closure.getSimplifiedOperand(valueMap);
        flagsComputed = false; // Forces recomputation of flags
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

    public boolean isDataflowBarrier() {
        // If the call is an eval, OR if it passes a closure and the callee can capture the caller's binding,
        // we cannot propagate dataflow analysis information across it (in either direction), except where
        // the dataflow analysis has additional information for ignoring this barrier. 
        return canBeEval() || targetRequiresCallersBinding();
    }

    @Override
    public String toString() {
        return ""
                + (getResult() == null ? "" : getResult() + " = ")
                + getOperation() + "(" + methAddr + ", " + receiver + ", " +
                java.util.Arrays.toString(getCallArgs())
                + (closure == null ? "" : ", &" + closure) + ")";
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new CallInstr(callType, ii.getRenamedVariable(getResult()), (MethAddr) methAddr.cloneForInlining(ii), receiver.cloneForInlining(ii), cloneCallArgs(ii), closure == null ? null : closure.cloneForInlining(ii));
   }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        IRubyObject object = (IRubyObject) getReceiver().retrieve(interp, context, self);
        IRubyObject[] args = prepareArguments(interp, context, self, getCallArgs());

        Object ma = methAddr.retrieve(interp, context, self);
        if (ma instanceof MethodHandle) return interpretMethodHandle(interp, context, self, (MethodHandle) ma, args);
        String name = ma.toString(); // SSS FIXME: If this is not a ruby string or a symbol, then this is an error in the source code!

        Block  block = prepareBlock(interp, context, self);

        Object resultValue;
        try {
             if (callType == null) {
                 resultValue = RuntimeHelpers.invoke(context, object, name, args, (self == object) ? CallType.FUNCTIONAL : CallType.NORMAL, block);
             }
             else {
                 // resultValue = RuntimeHelpers.invoke(context, object, name, args, callType, block);
                 //
                 // SSS FIXME:
                 // Some downstream calls dont like if I call the  args[] boxed version with fewer than
                 // 4 arguments!  I think it is bad practice and a bug for those calls to bomb when we
                 // invoke the boxed rather than the unboxed version.  But, since some of this is not
                 // in JRuby core, we'll bite the bullet for now -- this whole CallInstr setup needs
                 // cleaning up to use specialized versions.
                 switch (args.length) {
                 case 0: resultValue = callAdapter.call(context, self, object, block); break;
                 case 1: resultValue = callAdapter.call(context, self, object, args[0], block); break;
                 case 2: resultValue = callAdapter.call(context, self, object, args[0], args[1], block); break;
                 case 3: resultValue = callAdapter.call(context, self, object, args[0], args[1], args[2], block); break;
                 default: resultValue = callAdapter.call(context, self, object, args, block);
                 }
             }
        }
        finally {
            block.escape();
        }

        if (getResult() != null) getResult().store(interp, context, self, resultValue);
        return null;
    }

    /** ENEBO: Dead code for now...
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
     */
    
    protected IRubyObject[] prepareArguments(InterpreterContext interp, ThreadContext context, IRubyObject self, Operand[] args) {
        // SSS FIXME: This encoding of arguments as an array penalizes splats, but keeps other argument arrays fast
        // since there is no array list --> array transformation
        List<IRubyObject> argList = new ArrayList<IRubyObject>();
        for (int i = 0; i < args.length; i++) {
            IRubyObject rArg = (IRubyObject)args[i].retrieve(interp, context, self);
            if (args[i] instanceof Splat) {
                argList.addAll(Arrays.asList(((RubyArray)rArg).toJavaArray()));
            } else {
                argList.add(rArg);
            }
        }

        return argList.toArray(new IRubyObject[argList.size()]);
    }

    protected Block prepareBlock(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        if (closure == null) return Block.NULL_BLOCK;
        
        Object value = closure.retrieve(interp, context, self);
        
        Block b = null;
        if (value instanceof Block)
            b = (Block)value;
        else if (value instanceof RubyProc)
            b = ((RubyProc) value).getBlock();
        else if (value instanceof RubyMethod)
            b = ((RubyProc)((RubyMethod)value).to_proc(context, null)).getBlock();
        else if ((value instanceof IRubyObject) && ((IRubyObject)value).isNil())
            b = Block.NULL_BLOCK;
        else if (value instanceof IRubyObject)
            b = ((RubyProc)TypeConverter.convertToType((IRubyObject)value, context.getRuntime().getProc(), "to_proc", true)).getBlock();
        else
            throw new RuntimeException("Unhandled case in CallInstr:prepareBlock.  Got block arg: " + value);

        // Blocks passed in through calls are always normal blocks, no matter where they came from
        b.type = Block.Type.NORMAL;
        return b;
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
                    m.getVisibility(), mn, CallType.FUNCTIONAL, args, 
                    prepareBlock(interp, context, self));
        } else {
            resultValue = m.call(context, ro, ro.getMetaClass(), mn, args, prepareBlock(interp, context, self));
        }
        
        getResult().store(interp, context, self, resultValue);
        return null;        
    }
}
