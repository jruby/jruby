package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.StringLiteral;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.IR_Class;
import org.jruby.compiler.ir.IR_Closure;
import org.jruby.compiler.ir.IR_Module;
import org.jruby.compiler.ir.IR_Method;
import org.jruby.compiler.ir.IR_Scope;

public class CALL_Instr extends MultiOperandInstr
{
    Operand _methAddr;
    Operand _closure;

    public CALL_Instr(Variable result, Operand methAddr, Operand[] args, Operand closure)
    {
        super(Operation.CALL, result, buildAllArgs(methAddr, closure, args));
        _methAddr = methAddr;
        _closure = closure;
        _flagsComputed = false;
        _canBeEval = true;
        _requiresFrame = true;
        _numArgs = args.length;

    }
   
    public CALL_Instr(Operation op, Variable result, Operand methAddr, Operand[] args, Operand closure)
    {
        super(op, result, buildAllArgs(methAddr, closure, args));
        _methAddr = methAddr;
        _closure = closure;
        _flagsComputed = false;
        _canBeEval = true;
        _requiresFrame = true;
        _numArgs = args.length;

    }

    public Operand   getMethodAddr() { return _methAddr; }
    public Operand   getClosureArg() { return _closure; }
    public Operand   getReceiver()   { return _args[1]; }
    public int       getNumArgs()    { return _numArgs; }

    // Beware: Expensive call since a new array is allocated on each call.
    public Operand[] getCallArgs()
    {
        Operand[] callArgs = new Operand[_numArgs];
        for (int i = 0; i < _numArgs; i++)
            callArgs[i] = _args[i+1];

        return callArgs;
    }

    public void simplifyOperands(Map<Operand, Operand> valueMap)
    {
        super.simplifyOperands(valueMap);
        _methAddr = _args[0];
        _closure  = (_closure == null) ? null : _args[_args.length-1];
        _flagsComputed = false; // Forces recomputation of flags
    }

    public boolean isRubyInternalsCall() { return false; }
    public boolean isStaticCallTarget()  { return getTargetMethod() != null; }

    // SSS FIXME: Right now, this code is not very smart!
    // In a JIT context, we might be compiling this call in the context of a surrounding PIC (or a monomorphic IC).
    // If so, the receiver type and hence the target method will be known.
    public IR_Method getTargetMethodWithReceiver(Operand receiver)
    {
        if (!(_methAddr instanceof MethAddr))
           return null;

        String mname = ((MethAddr)_methAddr).getName();
        if (receiver instanceof MetaObject) {
            IR_Module m = (IR_Module)(((MetaObject)receiver)._scope);
            return m.getClassMethod(mname);
        }
            // self.foo(..);
            // If this call instruction is in a class method, we'll fetch a class method
            // If this call instruction is in an instance method, we'll fetch an instance method
        else if ((receiver instanceof Variable) && ((Variable)receiver).isSelf()) {
            IR_Class c = null; // SSS FIXME
            return null;
        }
        else {
            IR_Class c = receiver.getTargetClass();
            return (c == null) ? null : c.getInstanceMethod(mname);
        }
    }

    public IR_Method getTargetMethod()
    {
        return getTargetMethodWithReceiver(getReceiver());
    }

    // Can this call lead to ruby code getting modified?  
    // If we don't know what method we are calling, we assume it can (pessimistic, but safe!)
    // If we do know the target method, we ask the method itself whether it modifies ruby code
    public boolean canModifyCode()       
    { 
       IR_Method m = getTargetMethod(); 
       return (m == null) ? true : m.modifiesCode(); 
    }

    private boolean getEvalFlag()
    {
        Operand ma = getMethodAddr();
        if (ma instanceof MethAddr) {
            String mname = ((MethAddr)ma).getName();
            // checking for "call" is conservative.  It can be eval only if the receiver is a Method
            if (mname.equals("call") || mname.equals("eval")) {
                return true;
            }
            // Calls to 'send' where the first arg is either unknown or is eval or send (any others?)
            else if (mname.equals("send")) {
                Operand[] args = getCallArgs();
                if (args.length >= 2) {
                    Operand meth = args[1];
                    if (meth instanceof StringLiteral) {
                        String name = ((StringLiteral)meth)._str_value;
                        // But why?  Why are you killing yourself (and us) doing this?
                        if (name.equals("call") || name.equals("eval") || name.equals("send"))
                            return true;
                    }
                    // Dont know what message we are sending!
                    else {
                        return true;
                    }
                }
            }

            // We fell through all checks
            return false;
        }
        // Unknown method -- could be eval!
        else {
            return true;
        }
    }

    private boolean getRequiresFrameFlag()
    {
        // This is an eval, or it has a closure that requires a frame
        if (canBeEval()) 
            return true;
         
        if (_closure != null) {
            if (_closure instanceof MetaObject) {
                IR_Closure cl = (IR_Closure)((MetaObject)_closure)._scope;
                if (cl.requiresFrame())
                    return true;
            }
            else {  /* can be instance of a symbol .. ex: [1,2,3,4].map(&:foo) .. here &:foo is the closure */
                return false;
            }
        }

        // Check if we are calling Proc.new or lambda
        Operand ma = getMethodAddr();
        if (ma instanceof MethAddr) {
            String mname = ((MethAddr)ma).getName();
            if (mname.equals("lambda")) {
                return true;
            }
            else if (mname.equals("new")) {
                Operand receiver = getReceiver();
                if (receiver instanceof MetaObject) {
                    IR_Scope c = ((MetaObject)receiver)._scope;
                    if ((c instanceof IR_Class) && (((IR_Class)c)._name.equals("Proc")))
                        return true;
                }
                // Unknown receiver -- could be Proc!!
                else {
                    return true;
                }
            }
        }
        // Unknown target -- could be lambda or Proc.new
        else {
            return true;
        }

        // All checks done -- dont need one
        return false;
    }

    private void computeFlags()
    {
        // Order important!
        _flagsComputed = true;
        _canBeEval = getEvalFlag();
        _requiresFrame = getRequiresFrameFlag();
    }

    public boolean canBeEval()
    {
        if (!_flagsComputed)
            computeFlags();
        return _canBeEval;
    }

    public boolean requiresFrame()
    {
        if (!_flagsComputed)
            computeFlags();
        return _requiresFrame;
    }

    public boolean canCaptureCallersFrame()
    {
/**
 * We should do this better by setting default flags for various core library methods
 * and by checking type of receiver to see if the receiver is any core object (string, array, etc.)
 *
        if (_methAddr instanceof MethAddr) {
           String n = ((MethAddr)_methAddr).getName();
           return !n.equals("each") && !n.equals("inject") && !n.equals("+") && !n.equals("*") && !n.equals("+=") && !n.equals("*=");
        }
 **/

        Operand   r  = getReceiver(); 
        IR_Method rm = getTargetMethodWithReceiver(r);

        // If we don't know the method we are dispatching to, or if we know that the method can capture the callers frame,
        // we are in deep doo-doo.  We will need to store all variables in the call frame.
        return ((rm == null) || rm.canCaptureCallersFrame());
    }

    public boolean isLVADataflowBarrier()
    {
        // If the call is an eval, OR if it passes a closure and the callee can capture the caller's frame, we are in trouble
        // We would have to pretty much spill everything at the call site!
        return canBeEval() || ((getClosureArg() != null) && canCaptureCallersFrame());
    }

    public String toString() {
        return   "\t" 
               + (_result == null ? "" : _result + " = ") 
               + _op + "(" + _methAddr + ", " + java.util.Arrays.toString(getCallArgs())
               + (_closure == null ? "" : ", &" + _closure) + ")";
    }

// --------------- Private methods ---------------
    private boolean _flagsComputed;
    private boolean _canBeEval;
    private boolean _requiresFrame;
    private int     _numArgs;

    private static Operand[] buildAllArgs(Operand methAddr, Operand closure, Operand[] callArgs)
    {
        Operand[] allArgs = new Operand[callArgs.length + 1 + ((closure != null) ? 1 : 0)];

        allArgs[0] = methAddr;
        for (int i = 0; i < callArgs.length; i++)
            allArgs[i+1] = callArgs[i];
        if (closure != null)
            allArgs[callArgs.length+1] = closure;

        return allArgs;
    }
}
