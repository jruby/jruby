/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.interpreter;

import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class NaiveInterpreterContext implements InterpreterContext {
 private final ThreadContext context;
        protected Object returnValue;
        protected Object self;
        protected IRubyObject[] parameters;
        protected Object[] temporaryVariables;
        protected DynamicScope localVariables;
        protected Frame frame;
        protected Block block;

        public NaiveInterpreterContext(ThreadContext context, IRubyObject self, int temporaryVariableSize, IRubyObject[] parameters, StaticScope staticScope, Block block) {
            context.preMethodScopeOnly(self.getMetaClass(), staticScope);

            this.context = context;
            this.self = self;
            this.parameters = parameters;
            System.out.println("TVS: " + temporaryVariableSize);
            this.temporaryVariables = new Object[temporaryVariableSize];
            this.localVariables = context.getCurrentScope();
            this.block = block;
        }

        public Block getBlock() {
            return block;
        }

        public void setBlock(Block block) {
            this.block = block;
        }

        public Object getReturnValue() {
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

        public Object getLocalVariable(int location) {
            int depth = location >> 16;
            int offset = location & 0xffff;

            return localVariables.getValue(offset, depth);
        }

        public ThreadContext getContext() {
            return context;
        }

        public Object getParameter(int offset) {
            return parameters[offset - 1];
        }

        public int getParameterCount() {
            return parameters.length;
        }

        public Object setLocalVariable(int location, Object value) {
            int depth = location >> 16;
            int offset = location & 0xffff;

            localVariables.setValue((IRubyObject) value, offset, depth);

            return value;
        }

        public Object getSelf() {
            return self;
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
            argIndex -= 1;

            int length = parameters.length - argIndex;
            if (length < 0) return NO_PARAMS;

            IRubyObject[] args = new IRubyObject[length];
            System.arraycopy(parameters, argIndex, args, 0, length);

            return args;
        }
}
