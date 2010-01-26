package org.jruby.compiler.ir.operands;

import org.jruby.compiler.ir.IR_Class;
import org.jruby.compiler.ir.IR_Module;
import org.jruby.compiler.ir.IR_Closure;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IR_Scope;
import org.jruby.compiler.ir.IR_Script;

public class MetaObject extends Operand {
    public final IR_Scope _scope;

    public MetaObject(IR_Scope s) {
        _scope = s;
    }

    @Override
    public String toString() {
        if (_scope instanceof IR_Class) {
            return "Class " + ((IR_Class) _scope).getName();
        } else if (_scope instanceof IR_Module) {
            return "Module " + ((IR_Module) _scope).getName();
        } else if (_scope instanceof IRMethod) {
            return "Method " + ((IRMethod) _scope).getName();
        } else if (_scope instanceof IR_Script) {
            return "Script " + ((IR_Script) _scope)._fileName;
        } else {
            return ((IR_Closure) _scope).toString().replace("\t", "\t\t");
        }
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    // SSS FIXME: Incomplete!
    @Override
    public IR_Class getTargetClass() {
        return (_scope instanceof IR_Module) ? IR_Class.getCoreClass("Module") : null;
    }
}
