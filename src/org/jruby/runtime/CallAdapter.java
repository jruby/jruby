/*
 * CallAdapter.java
 * 
 * Created on Jul 15, 2007, 2:50:18 AM
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.runtime;

import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public abstract class CallAdapter {
    protected final int methodID;
    protected final String methodName;
    protected final CallType callType;
    
    public CallAdapter(int methodID, String methodName, CallType callType) {
        this.methodID = methodID;
        this.methodName = methodName;
        this.callType = callType;
    }
    
    // no block
    public abstract IRubyObject call(ThreadContext context, IRubyObject self);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject[] args);
    // with block
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, Block block);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, Block block);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block);
    
    public static class DefaultCallAdapter extends CallAdapter {
        public DefaultCallAdapter(int methodID, String methodName, CallType callType) {
            super(methodID, methodName, callType);
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self) {
            return self.getMetaClass().getDispatcher().callMethod(context, self, self.getMetaClass(), methodID, methodName, IRubyObject.NULL_ARRAY, callType, Block.NULL_BLOCK);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1) {
            return self.getMetaClass().getDispatcher().callMethod(context, self, self.getMetaClass(), methodID, methodName, new IRubyObject[] {arg1}, callType, Block.NULL_BLOCK);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
            return self.getMetaClass().getDispatcher().callMethod(context, self, self.getMetaClass(), methodID, methodName, new IRubyObject[] {arg1, arg2}, callType, Block.NULL_BLOCK);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
            return self.getMetaClass().getDispatcher().callMethod(context, self, self.getMetaClass(), methodID, methodName, new IRubyObject[] {arg1, arg2, arg3}, callType, Block.NULL_BLOCK);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return self.getMetaClass().getDispatcher().callMethod(context, self, self.getMetaClass(), methodID, methodName, args, callType, Block.NULL_BLOCK);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, Block block) {
            return self.getMetaClass().getDispatcher().callMethod(context, self, self.getMetaClass(), methodID, methodName, IRubyObject.NULL_ARRAY, callType, block);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, Block block) {
            return self.getMetaClass().getDispatcher().callMethod(context, self, self.getMetaClass(), methodID, methodName, new IRubyObject[] {arg1}, callType, block);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
            return self.getMetaClass().getDispatcher().callMethod(context, self, self.getMetaClass(), methodID, methodName, new IRubyObject[] {arg1, arg2}, callType, block);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
            return self.getMetaClass().getDispatcher().callMethod(context, self, self.getMetaClass(), methodID, methodName, new IRubyObject[] {arg1, arg2, arg3}, callType, block);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
            return self.getMetaClass().getDispatcher().callMethod(context, self, self.getMetaClass(), methodID, methodName, args, callType, block);
        }
    }
}
