package org.jruby.compiler.ir.operands;

import java.util.Map;

import org.jruby.RubyClass;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.callsite.CacheEntry;

// This represents a method handle bound to a receiver -- it is used during
// compilation / interpretation to represent a method handle returned by a
// method lookup instruction.  It may not necessarily have been fully resolved.
// During interpretation / compilation, we may get a fully resolved method handle
// which might be stored in an inline cache.
public class MethodHandle extends Operand {
    protected Operand methodName;
    protected Operand receiver;

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
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap) {
        methodName = methodName.getSimplifiedOperand(valueMap);
        receiver = receiver.getSimplifiedOperand(valueMap);
        return this;
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
    public Object retrieve(InterpreterContext interp) {
        receiverObj = (IRubyObject)receiver.retrieve(interp);

        if (methodName instanceof MethAddr) {
            resolvedMethodName = ((MethAddr)methodName).getName();
        } else {
            IRubyObject mnameObj = (IRubyObject)methodName.retrieve(interp);

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
}
