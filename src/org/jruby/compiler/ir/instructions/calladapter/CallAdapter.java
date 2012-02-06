package org.jruby.compiler.ir.instructions.calladapter;

import org.jruby.compiler.ir.operands.Fixnum;
import org.jruby.compiler.ir.operands.ImmutableLiteral;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Splat;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public abstract class CallAdapter {
    protected final CallSite callSite;
    
    public CallAdapter(CallSite callSite) {
        this.callSite = callSite;
    }
        
    public abstract Object call(ThreadContext context, IRubyObject self, IRubyObject receiver, DynamicScope currDynScope, Object[] temp);

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
    
    private static boolean containsSplat(Operand args[]) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Splat) return true;
        }
        
        return false;
    }
    
    private static boolean isConstant(Operand args[]) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof ImmutableLiteral && !args[i].hasKnownValue()) return false;
        }
        
        return true;
    }
    
    private static boolean isFixnum(Operand args[]) {
        for (int i = 0; i < args.length; i++) {
            if (!(args[i] instanceof Fixnum)) return false;
        }
        
        return true;
    }
}
