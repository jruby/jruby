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
import org.jruby.util.TypeConverter;

public class SuperInstr extends CallInstr {
    public SuperInstr(Variable result, Operand receiver, MethAddr superMeth, Operand[] args, Operand closure) {
        super(Operation.SUPER, CallType.SUPER, result, superMeth, receiver, args, closure);
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
    public Object interpret(ThreadContext context, IRubyObject self, Object[] temp, Block aBlock) {
        // FIXME: Receiver is not being used...should we be retrieving it?
        IRubyObject receiver = (IRubyObject)getReceiver().retrieve(context, self, temp);
        IRubyObject[] args = prepareArguments(context, self, getCallArgs(), temp);
        Block block = prepareBlock(context, self, temp);
        RubyModule klazz = context.getFrameKlazz();
        // SSS FIXME: Even though we may know the method name in some instances,
        // we are not making use of it here.  It is cleaner in the sense of not
        // relying on implicit information whose data flow doesn't show up in the IR.
        String methodName = context.getCurrentFrame().getName(); // methAddr.getName();

        checkSuperDisabledOrOutOfMethod(context, klazz, methodName);
        RubyClass superClass = RuntimeHelpers.findImplementerIfNecessary(self.getMetaClass(), klazz).getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(methodName) : UndefinedMethod.INSTANCE;
        
        Object rVal = method.isUndefined() ? RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), methodName, CallType.SUPER, args, block)
                                           : method.call(context, self, superClass, methodName, args, block);

        if (!hasUnusedResult()) getResult().store(context, temp, rVal);

        return null;
    }

    protected static void checkSuperDisabledOrOutOfMethod(ThreadContext context, RubyModule frameClass, String methodName) {
        if (frameClass == null) {
            if ((methodName == null) || (methodName != "")) {
                throw context.getRuntime().newNameError("superclass method '" + methodName + "' disabled", methodName);
            } else {
                throw context.getRuntime().newNoMethodError("super called outside of method", null, context.getRuntime().getNil());
            }
        }
    }
    
    protected IRubyObject[] prepareArguments(ThreadContext context, IRubyObject self, Operand[] args, Object[] temp) {
        // SSS FIXME: This encoding of arguments as an array penalizes splats, but keeps other argument arrays fast
        // since there is no array list --> array transformation
        List<IRubyObject> argList = new ArrayList<IRubyObject>();
        for (int i = 0; i < args.length; i++) {
            IRubyObject rArg = (IRubyObject)args[i].retrieve(context, self, temp);
            if (args[i] instanceof Splat) {
                argList.addAll(Arrays.asList(((RubyArray)rArg).toJavaArray()));
            } else {
                argList.add(rArg);
            }
        }

        return argList.toArray(new IRubyObject[argList.size()]);
    }

    protected Block prepareBlock(ThreadContext context, IRubyObject self, Object[] temp) {
        if (closure == null) return Block.NULL_BLOCK;
        
        Object value = closure.retrieve(context, self, temp);
        
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
