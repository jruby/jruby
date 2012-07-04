package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.specialized.OneFixnumArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.ZeroOperandArgNoBlockCallInstr;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.CallType;

/*
 * args field: [self, receiver, *args]
 */
public class CallInstr extends CallBase implements ResultInstr {
    protected Variable result;
    
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
        super(op, callType, methAddr, receiver, args, closure);

        assert result != null;
        
        this.result = result;
    }
    
    public CallInstr(CallInstr ordinary) {
        this(ordinary.getOperation(), ordinary.getCallType(), ordinary.getResult(),
                ordinary.getMethodAddr(), ordinary.getReceiver(), ordinary.getCallArgs(),
                ordinary.getClosureArg(null));
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }
    
    @Override
    public CallBase specializeForInterpretation() {
        Operand[] callArgs = getCallArgs();
        if (hasClosure() || containsSplat(callArgs)) return this;
        
        switch (callArgs.length) {
            case 0:
                return new ZeroOperandArgNoBlockCallInstr(this);
            case 1:
                if (isAllFixnums()) return new OneFixnumArgNoBlockCallInstr(this);

                return new OneOperandArgNoBlockCallInstr(this);
        }
        return this;
    }    

    public Instr discardResult() {
        return new NoResultCallInstr(getOperation(), getCallType(), getMethodAddr(), getReceiver(), getCallArgs(), closure);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new CallInstr(getCallType(), ii.getRenamedVariable(result), 
                (MethAddr) getMethodAddr().cloneForInlining(ii), 
                receiver.cloneForInlining(ii), cloneCallArgs(ii), 
                closure == null ? null : closure.cloneForInlining(ii));
    }

    @Override
    public String toString() {
        return (hasUnusedResult() ? "[DEAD-RESULT]" : "") + result + " = " + super.toString();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.CallInstr(this);
    }
    
/* FIXME: Dead code which I think should be a special instr (enebo)
        Object ma = methAddr.retrieve(interp, context, self);
        if (ma instanceof MethodHandle) return interpretMethodHandle(interp, context, self, (MethodHandle) ma, args);
         */    

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
                     throw new org.jruby.ir.interpreter.InlineMethodHint(inlineableMethod);
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
    }*/
}
