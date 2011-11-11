/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.interpreter;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.compiler.ir.IRExecutionScope;

/**
 *
 * @author enebo
 */
public class NaiveInterpreterContext implements InterpreterContext {
    protected IRubyObject[] parameters;

    // currentModule is:
    // - self if we are executing a class method of 'self'
    // - self.getMetaClass() if we are executing an instance method of 'self'
    // - the class in which the closure is lexically defined in if we are executing a closure
    public NaiveInterpreterContext(ThreadContext context, IRExecutionScope irScope, IRubyObject[] parameters) {
        this.parameters = parameters;
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

    // FIXME: We have this as a var somewhere else
    private IRubyObject[] NO_PARAMS = new IRubyObject[0];

    public IRubyObject[] getParametersFrom(int argIndex) {
        int length = parameters.length - argIndex;
        
        if (length <= 0) return NO_PARAMS;

        IRubyObject[] args = new IRubyObject[length];
        System.arraycopy(parameters, argIndex, args, 0, length);

        return args;
    }
}
