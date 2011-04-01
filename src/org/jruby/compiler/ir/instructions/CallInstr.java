package org.jruby.compiler.ir.instructions;

import java.util.Map;
import java.util.HashMap;

import org.jruby.RubyClass;
import org.jruby.RubyProc;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.MethodHandle;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.StringLiteral;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.IRClass;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IRModule;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/*
 * args field: [self, receiver, *args]
 */
public class CallInstr extends MultiOperandInstr {
    private Operand   receiver;
    private Operand[] arguments;
    MethAddr methAddr;
    Operand closure;
    
    private boolean _flagsComputed;
    private boolean _canBeEval;
    private boolean _requiresBinding;    // Does this call make use of the caller's binding?
    public HashMap<DynamicMethod, Integer> _profile;

    public CallInstr(Variable result, MethAddr methAddr, Operand receiver, Operand[] args, Operand closure) {
        super(Operation.CALL, result);

        this.receiver = receiver;
        this.arguments = args;
        this.methAddr = methAddr;
        this.closure = closure;

        _flagsComputed = false;
        _canBeEval = true;
        _requiresBinding = true;
    }

    public CallInstr(Operation op, Variable result, MethAddr methAddr, Operand receiver, Operand[] args, Operand closure) {
        super(op, result);

        this.receiver = receiver;
        this.arguments = args;
        this.methAddr = methAddr;
        this.closure = closure;

        _flagsComputed = false;
        _canBeEval = true;
        _requiresBinding = true;
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
        return closure;
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
        _flagsComputed = false; // Forces recomputation of flags
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
        return getTargetMethod() != null;
    }

    // SSS FIXME: Right now, this code is not very smart!
    // In a JIT context, we might be compiling this call in the context of a surrounding PIC (or a monomorphic IC).
    // If so, the receiver type and hence the target method will be known.
    public IRMethod getTargetMethodWithReceiver(Operand receiver) {
        String mname = methAddr.getName();

        if (receiver instanceof MetaObject) {
            IRModule m = (IRModule) (((MetaObject) receiver).scope);
            return m.getClassMethod(mname);
        } // self.foo(..);
        // If this call instruction is in a class method, we'll fetch a class method
        // If this call instruction is in an instance method, we'll fetch an instance method
        else if ((receiver instanceof LocalVariable) && (((LocalVariable)receiver).isSelf())) {
            return null;
        } else {
            IRClass c = receiver.getTargetClass();

            return c == null ? null : c.getInstanceMethod(mname);
        }
    }

    public IRMethod getTargetMethod() {
        return getTargetMethodWithReceiver(getReceiver());
    }

    // Can this call lead to ruby code getting modified?  
    // If we don't know what method we are calling, we assume it can (pessimistic, but safe!)
    // If we do know the target method, we ask the method itself whether it modifies ruby code
    public boolean canModifyCode() {
        IRMethod method = getTargetMethod();

        return method == null ? true : method.modifiesCode();
    }

    // SSS FIXME: Are all bases covered?
    private boolean getEvalFlag() {
        // ENEBO: This could be made into a recursive two-method thing so then: send(:send, :send, :send, :send, :eval, "Hosed") works
        // ENEBO: This is not checking for __send__
        String mname = getMethodAddr().getName();
        // checking for "call" is conservative.  It can be eval only if the receiver is a Method
        if (mname.equals("call") || mname.equals("eval")) return true;

        // Calls to 'send' where the first arg is either unknown or is eval or send (any others?)
        if (mname.equals("send")) {
            Operand[] args = getCallArgs();
            if (args.length >= 2) {
                Operand meth = args[0];
                if (!(meth instanceof StringLiteral)) return true; // We don't know

                // But why?  Why are you killing yourself (and us) doing this?
                String name = ((StringLiteral) meth)._str_value;
                if (name.equals("call") || name.equals("eval") || name.equals("send")) return true;
            }
        }
            
        return false; // All checks passed
    }

    private boolean getRequiresBindingFlag() {
        // This is an eval
        // SSS FIXME: This is conservative, but will let that go for now
        if (canBeEval() /*|| canCaptureCallersBinding()*/) return true;

        if (closure != null) {
            // Can be a symbol .. ex: [1,2,3,4].map(&:foo)
            // SSS FIXME: Is it true that if the closure operand is a symbol, it couldn't access the caller's binding?
            if (!(closure instanceof MetaObject)) return false;

            IRClosure cl = (IRClosure) ((MetaObject) closure).scope;
            if (cl.requiresBinding() /*|| cl.canCaptureCallersBinding()*/) return true;
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
        _flagsComputed = true;
        _canBeEval = getEvalFlag();
        _requiresBinding   = _canBeEval ? true : getRequiresBindingFlag();
    }

    public boolean canBeEval() {
        if (!_flagsComputed) computeFlags();

        return _canBeEval;
    }

    public boolean requiresBinding() {
        if (!_flagsComputed) computeFlags();

        return _requiresBinding;
    }

    // SSS FIXME: Are all bases covered?
    public boolean canCaptureCallersBinding() {
        /**
         * We should do this better by setting default flags for various core library methods
         * and by checking type of receiver to see if the receiver is any core object (string, array, etc.)
         *
        if (methAddr instanceof MethAddr) {
        String n = ((MethAddr)methAddr).getName();
        return !n.equals("each") && !n.equals("inject") && !n.equals("+") && !n.equals("*") && !n.equals("+=") && !n.equals("*=");
        }
         **/

        Operand r = getReceiver();
        IRMethod rm = getTargetMethodWithReceiver(r);

        // If we don't know the method we are dispatching to, or if we know that the method can capture the callers frame,
        // we are in deep doo-doo.  We will need to store all variables in the call frame.
        //
        // SSS FIXME:
        // This is a "static" check and at some point during the execution, the caller's code could change and capture the binding at that point!
        // We need to set a compilation flag that records this dependency on the caller, so that this method can be recompiled whenever
        // the caller changes.
        return ((rm == null) || rm.canCaptureCallersBinding());
    }

    public boolean isLVADataflowBarrier() {
        // If the call is an eval, OR if it passes a closure and the callee can capture the caller's binding, we are in trouble
        // We would have to pretty much spill everything at the call site!
        return canBeEval() || ((getClosureArg() != null) && canCaptureCallersBinding());
    }

    @Override
    public String toString() {
        return "\t"
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

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
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
                try {
                    resultValue = m.call(interp.getContext(), ro, ro.getMetaClass(), mn, args,
                            prepareBlock(interp));
                } catch (org.jruby.exceptions.JumpException.BreakJump bj) {
                    resultValue = (IRubyObject) bj.getValue();
                }
            }
        } else {
            IRubyObject object = (IRubyObject) getReceiver().retrieve(interp);
            String name = ma.toString(); // SSS FIXME: If this is not a ruby string or a symbol, then this is an error in the source code!
           
            try {
                resultValue = object.callMethod(interp.getContext(), name, args, prepareBlock(interp));
            } catch (org.jruby.exceptions.JumpException.BreakJump bj) {
                resultValue = (IRubyObject) bj.getValue();
            }
        }

        getResult().store(interp, resultValue);
        return null;
    }

    public Label interpret_with_inline(InterpreterContext interp, IRubyObject self) {
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
               if (_profile == null) {
                  _profile = new HashMap<DynamicMethod, Integer>();
               }
               Integer count = _profile.get(m);
               if (count == null) {
                  count = new Integer(1);
               } else {
                  count = new Integer(count + 1);
                  if ((count > 50) && (m instanceof InterpretedIRMethod) && (_profile.size() == 1)) {
                     IRMethod inlineableMethod = ((InterpretedIRMethod)m).method;
                     _profile.remove(m); // remove it because the interpreter might ignore this hint
                     throw new org.jruby.interpreter.InlineMethodHint(inlineableMethod);
                  }
               }
               _profile.put(m, count);
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

    private Block prepareBlock(InterpreterContext interp) {
        if (closure == null) return Block.NULL_BLOCK;
        Object value = closure.retrieve(interp);
        return value instanceof RubyProc ? ((RubyProc) value).getBlock() : (Block) value;
    }
}
