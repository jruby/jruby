package org.jruby.ir.operands;

import org.jruby.RubyClass;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;

import java.util.List;
import java.util.Map;

// This represents a method handle bound to a receiver -- it is used during
// compilation / interpretation to represent a method handle returned by a
// method lookup instruction.  It may not necessarily have been fully resolved.
// During interpretation / compilation, we may get a fully resolved method handle
// which might be stored in an inline cache.
public class MethodHandle extends Operand {
    final protected Operand methodName;
    final protected Operand receiver;

    // Used during interpretation
    private String      resolvedMethodName;
    private CacheEntry  cachedMethod;
    private IRubyObject receiverObj;

    public MethodHandle(Operand methodName, Operand receiver) {
        this.methodName = methodName;
        this.receiver = receiver;
    }

    public Operand getMethodNameOperand() {
        return methodName;
    }

    public DynamicMethod getResolvedMethod() {
        return cachedMethod.method;
    }

    public String getResolvedMethodName() {
        return resolvedMethodName;
    }

    public IRubyObject getReceiverObj() {
        return receiverObj;
    }

    @Override
    public void addUsedVariables(List<Variable> l) {
        methodName.addUsedVariables(l);
        receiver.addUsedVariables(l);
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force) {
        Operand newMethodName = methodName.getSimplifiedOperand(valueMap, force);
        Operand newReceiver = receiver.getSimplifiedOperand(valueMap, force);
        return (newMethodName == methodName && newReceiver == receiver) ? this : new MethodHandle(newMethodName, newReceiver);
    }

    @Override
    public String toString() {
        return "<" + receiver + "." + methodName + ">";
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) {
        return new MethodHandle(methodName.cloneForInlining(ii), receiver.cloneForInlining(ii));
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        receiverObj = (IRubyObject)receiver.retrieve(context, self, currDynScope, temp);

        if (methodName instanceof MethAddr) {
            resolvedMethodName = ((MethAddr)methodName).getName();
        } else {
            IRubyObject mnameObj = (IRubyObject)methodName.retrieve(context, self, currDynScope, temp);

            // SSS FIXME: If this is not a ruby string or a symbol, then this is an error in the source code!
            // Raise an exception and throw an error.  This should not be an assert.
            assert (mnameObj instanceof RubyString || mnameObj instanceof RubySymbol);

            // Clear cached method if the method name has changed!
            if (!mnameObj.toString().equals(resolvedMethodName)) {
                cachedMethod = null;
                resolvedMethodName = mnameObj.toString();
            }
        }

        RubyClass receiverClass = receiverObj.getMetaClass();
        if (cachedMethod == null || !cachedMethod.typeOk(receiverClass)) {
            // lookup -- 'm' can be an undefined method.  It is the caller's responsibility to deal with that
            cachedMethod = receiverClass.searchWithCache(resolvedMethodName);
        }

        return this;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.MethodHandle(this);
    }
}
