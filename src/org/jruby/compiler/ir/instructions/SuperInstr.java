package org.jruby.compiler.ir.instructions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyMethod;
import org.jruby.RubyModule;

import org.jruby.RubyProc;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.Splat;
import org.jruby.compiler.ir.representations.InlinerInfo;

import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;

import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.DynamicScope;
import org.jruby.util.TypeConverter;

public class SuperInstr extends CallInstr {
    public SuperInstr(Variable result, Operand receiver, MethAddr superMeth, Operand[] args, Operand closure) {
        super(Operation.SUPER, CallType.SUPER, result, superMeth, receiver, args, closure);
    }

    public SuperInstr(Operation op, Variable result, Operand closure) {
        super(op, CallType.SUPER, result, MethAddr.UNKNOWN_SUPER_TARGET, null, EMPTY_OPERANDS, closure);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new SuperInstr(ii.getRenamedVariable(getResult()), getReceiver().cloneForInlining(ii), getMethodAddr(),
                cloneCallArgs(ii), closure == null ? null : closure.cloneForInlining(ii));
    }

    // We cannot convert this into a NoCallResultInstr
    public Instr discardResult() {
        return this;
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block aBlock) {
        IRubyObject[] args = prepareArguments(context, self, getCallArgs(), currDynScope, temp);
        Block block = prepareBlock(context, self, currDynScope, temp);
        return interpretSuper(context, self, args, block);
    }

    protected Object interpretSuper(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        RubyModule klazz = context.getFrameKlazz();
        // SSS FIXME: Even though we may know the method name in some instances,
        // we are not making use of it here.
        String methodName = context.getCurrentFrame().getName(); // methAddr.getName();

        checkSuperDisabledOrOutOfMethod(context, klazz, methodName);
        RubyClass superClass = RuntimeHelpers.findImplementerIfNecessary(self.getMetaClass(), klazz).getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(methodName) : UndefinedMethod.INSTANCE;
        
        Object rVal = method.isUndefined() ? RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), methodName, CallType.SUPER, args, block)
                                           : method.call(context, self, superClass, methodName, args, block);

        return hasUnusedResult() ? null : rVal;
    }

    protected static void checkSuperDisabledOrOutOfMethod(ThreadContext context, RubyModule frameClass, String methodName) {
        // FIXME: super/zsuper in top-level script still seems to have a frameClass so it will not make it into this if
        if (frameClass == null) {
            if (methodName == null || methodName != "") {
                throw context.getRuntime().newNameError("superclass method '" + methodName + "' disabled", methodName);
            } else {
                throw context.getRuntime().newNoMethodError("super called outside of method", null, context.getRuntime().getNil());
            }
        }
    }
    
    protected IRubyObject[] prepareArguments(ThreadContext context, IRubyObject self, Operand[] args, DynamicScope currDynScope, Object[] temp) {
        // SSS FIXME: This encoding of arguments as an array penalizes splats, but keeps other argument arrays fast
        // since there is no array list --> array transformation
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
}
