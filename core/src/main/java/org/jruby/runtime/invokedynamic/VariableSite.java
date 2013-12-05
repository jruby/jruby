package org.jruby.runtime.invokedynamic;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.common.IRubyWarnings;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import org.jruby.runtime.ivars.VariableAccessor;

public class VariableSite extends MutableCallSite {
    public final String name;
    private VariableAccessor accessor = VariableAccessor.DUMMY_ACCESSOR;
    private final String file;
    private final int line;
    private int chainCount;

    public VariableSite(MethodType type, String name, String file, int line) {
        super(type);
        this.name = name;
        this.file = file;
        this.line = line;
        this.chainCount = 0;
    }

    public synchronized int chainCount() {
        return chainCount;
    }

    public synchronized void incrementChainCount() {
        chainCount += 1;
    }

    public synchronized void clearChainCount() {
        chainCount = 0;
    }

    public final IRubyObject getVariable(IRubyObject object) {
        VariableAccessor variableAccessor = accessor;
        RubyClass cls = object.getMetaClass().getRealClass();
        if (variableAccessor.getClassId() != cls.hashCode()) {
            accessor = variableAccessor = cls.getVariableAccessorForRead(name);
        }
        IRubyObject value = (IRubyObject) variableAccessor.get(object);
        if (value != null) {
            return value;
        }
        return object.getRuntime().getNil();
    }

    public final IRubyObject setVariable(IRubyObject object, IRubyObject value) {
        VariableAccessor variableAccessor = accessor;
        RubyClass cls = object.getMetaClass().getRealClass();
        if (variableAccessor.getClassId() != cls.hashCode()) {
            accessor = variableAccessor = cls.getVariableAccessorForWrite(name);
        }
        variableAccessor.set(object, value);
        return value;
    }

    public String file() {
        return file;
    }

    public int line() {
        return line;
    }
}
