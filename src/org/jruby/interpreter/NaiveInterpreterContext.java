/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.interpreter;

import org.jruby.RubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.IRMethod;

/**
 *
 * @author enebo
 */
public class NaiveInterpreterContext implements InterpreterContext {
    protected IRExecutionScope irScope;
    protected IRubyObject[] parameters;
    protected Object returnValue;
    protected Object[] temporaryVariables;
    protected Frame frame;
    protected Block block;
    protected Block.Type blockType;
    protected DynamicScope currDynScope = null;
    protected boolean allocatedDynScope = false;
    protected Object currException = null;

    // currentModule is:
    // - self if we are executing a class method of 'self'
    // - self.getMetaClass() if we are executing an instance method of 'self'
    // - the class in which the closure is lexically defined in if we are executing a closure
    public NaiveInterpreterContext(ThreadContext context, IRExecutionScope irScope, RubyModule currentModule, IRubyObject self, String name, IRubyObject[] parameters, Block block, Block.Type blockType) {
        this.irScope = irScope;
        this.frame = context.getCurrentFrame();
        this.parameters = parameters;
        this.currDynScope = context.getCurrentScope();

        int temporaryVariablesSize = irScope.getTemporaryVariableSize();
        this.temporaryVariables = temporaryVariablesSize > 0 ? new Object[temporaryVariablesSize] : null;
        this.block = block;
        // SSS FIXME: Can it happen that (block.type != blockType)?
        this.blockType = blockType;
    }

    public Block getBlock() {
        return block;
    }

    public boolean inLambda() {
        return (blockType != null) && (blockType == Block.Type.LAMBDA);
    }

    public boolean inProc() {
        return (blockType != null) && (blockType == Block.Type.PROC);
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public Object getReturnValue(ThreadContext context) {
        // FIXME: Maybe returnValue is a sure thing and we don't need this check.  Should be this way.
        return returnValue == null ? context.getRuntime().getNil() : returnValue;
    }

    public void setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
    }

    public Object getTemporaryVariable(int offset) {
        return temporaryVariables[offset];
    }

    public Object setTemporaryVariable(int offset, Object value) {
        Object oldValue = temporaryVariables[offset];

        temporaryVariables[offset] = value;

        return oldValue;
    }

    // Post-inlining
    public void updateLocalVariablesCount(int n) {
       // FIXME: Needs to be done.
    }

    public Object getSharedBindingVariable(ThreadContext context, int bindingSlot) {
        Object value = currDynScope.getValue(bindingSlot, 0);
        if (value == null) value = context.getRuntime().getNil();
        return value;
    }

    public void setSharedBindingVariable(int bindingSlot, Object value) {
        currDynScope.setValueDepthZero((IRubyObject)value, bindingSlot);
    }

    public Object getLocalVariable(ThreadContext context, int depth, int offset) {
        Object value = currDynScope.getValue(offset, depth);
        return (value == null) ? context.getRuntime().getNil() : value;
    }

    public Object setLocalVariable(int depth, int offset, Object value) {
        currDynScope.setValue((IRubyObject)value, offset, depth); 
        return null;
    }

    public IRubyObject[] setNewParameters(IRubyObject[] newParams) {
        IRubyObject[] oldParams = parameters;
        this.parameters = newParams;
        return oldParams;
    }

    public Object getParameter(int offset) {
        return parameters[offset];
    }

    public int getParameterCount() {
        return parameters.length;
    }

    public Frame getFrame() {
        return frame;
    }

    public void setFrame(Frame frame) {
        this.frame = frame;
    }
    // FIXME: We have this as a var somewhere else
    private IRubyObject[] NO_PARAMS = new IRubyObject[0];

    public IRubyObject[] getParametersFrom(int argIndex) {
        int length = parameters.length - argIndex;
        
        if (length <= 0) return NO_PARAMS;

        IRubyObject[] args = new IRubyObject[length];
        System.arraycopy(parameters, argIndex, args, 0, length);

        return args;
    }

    public IRExecutionScope getCurrentIRScope() {
        return this.irScope;
    }

    // Set the most recently raised exception
    public void setException(Object e) {
        currException = e;
    }

    // SSS FIXME: Should we get-and-clear instead of just get?
    // Get the most recently raised exception
    public Object getException() {
        return currException;
    }
}
