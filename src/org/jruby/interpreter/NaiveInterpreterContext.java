/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.interpreter;

import java.util.HashMap;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class NaiveInterpreterContext implements InterpreterContext {
    private final Ruby runtime;
    private final ThreadContext context;
    protected Object returnValue;
    protected Object self;
    protected IRubyObject[] parameters;
    protected Object[] temporaryVariables;
    protected Map localVariables;
    protected Frame frame;
    protected Block block;
    
    private static ThreadLocal<Map<Object, Map<String, Object>>> frameVariables = new ThreadLocal<Map<Object, Map<String, Object>>>() {

        @Override
        protected Map<Object, Map<String, Object>> initialValue() {
            return new HashMap<Object, Map<String, Object>>();
        }
    };

    public NaiveInterpreterContext(ThreadContext context, IRubyObject self, int temporaryVariableSize, IRubyObject[] parameters, StaticScope staticScope, Block block) {
        context.preMethodScopeOnly(self.getMetaClass(), staticScope);

        this.context = context;
        this.runtime = context.getRuntime();
        this.self = self;
        this.parameters = parameters;
        this.temporaryVariables = new Object[temporaryVariableSize];
        this.localVariables = new HashMap();
        this.block = block;
    }

    public Ruby getRuntime() {
        return runtime;
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

    public Object getFrameVariable(Object frame, String name) {
        Object value = getFrameVariableMap(frame).get(name);

        if (value == null) value = getRuntime().getNil();

        return value;
    }

    public void setFrameVariable(Object frame, String name, Object value) {
        getFrameVariableMap(frame).put(name, value);
    }

    private Map<String, Object> getFrameVariableMap(Object frame) {
        Map<Object, Map<String, Object>> maps = frameVariables.get();
        Map<String, Object> map = maps.get(frame);

        if (map == null) {
            map = new HashMap<String, Object>();
            maps.put(frame, map);
        }

        System.out.println("MAP = " + map);

        return map;
    }

    public Object getLocalVariable(String name) {
        Object value = localVariables.get(name);

        if (value == null) value = getRuntime().getNil();

        return value;
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

    public Object setLocalVariable(String name, Object value) {
        localVariables.put(name, value);
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
        if (length < 0) {
            return NO_PARAMS;
        }

        IRubyObject[] args = new IRubyObject[length];
        System.arraycopy(parameters, argIndex, args, 0, length);

        return args;
    }
}
