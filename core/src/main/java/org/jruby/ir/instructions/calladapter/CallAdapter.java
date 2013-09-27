package org.jruby.ir.instructions.calladapter;

import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.ImmutableLiteral;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Splat;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class CallAdapter {
    protected CallSite callSite = null;

    public CallAdapter(CallSite callSite) {
    }

    public abstract Object call(ThreadContext context, IRubyObject self, IRubyObject receiver, DynamicScope currDynScope, Object[] temp);

    public CallSite getCallSite() {
        return null;
    }

    /*
    public static CallAdapter createFor(CallType callType, MethAddr methAddr, Operand args[], Operand closure) {
        CallSite callSite = getCallSiteFor(callType, methAddr);

        if (callType == CallType.UNKNOWN) return new AttrAssignCallAdapter(callSite, methAddr.getName(), args);
        if (containsSplat(args)) return new ManyArgBlockSplatCallAdapter(callSite, args, closure);

        switch(args.length) {
            case 0:
                if (closure != null) return new NoArgBlockOperandCallAdapter(callSite, args, closure);

                return new NoArgNoBlockOperandCallAdapter(callSite, args);
            case 1:
                if (isFixnum(args) && closure == null) {
                    return new OneArgNoBlockFixnumCallAdapter(callSite, args);
                }
                if (isConstant(args) && closure == null) return new OneArgNoBlockConstantCallAdapter(callSite, args);

                if (closure != null) return new OneArgBlockOperandCallAdapter(callSite, args, closure);

                return new OneArgNoBlockOperandCallAdapter(callSite, args);
            case 2:
                if (isConstant(args) && closure == null) return new TwoArgNoBlockConstantCallAdapter(callSite, args);

                if (closure != null) return new TwoArgBlockOperandCallAdapter(callSite, args, closure);

                return new TwoArgNoBlockOperandCallAdapter(callSite, args);
            case 3:
                if (isConstant(args) && closure == null) return new ThreeArgNoBlockConstantCallAdapter(callSite, args);

                if (closure != null) return new ThreeArgBlockOperandCallAdapter(callSite, args, closure);

                return new ThreeArgNoBlockOperandCallAdapter(callSite, args);
            case 4:
                if (isConstant(args) && closure == null) return new FourArgNoBlockConstantCallAdapter(callSite, args);

                if (closure == null) return new FourArgNoBlockOperandCallAdapter(callSite, args);
        }

        return new ManyArgBlockOperandCallAdapter(callSite, args, closure);
    }
*/
}
