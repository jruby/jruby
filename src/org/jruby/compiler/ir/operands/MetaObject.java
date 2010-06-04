package org.jruby.compiler.ir.operands;

import org.jruby.compiler.ir.IR_Class;
import org.jruby.compiler.ir.IR_Module;
import org.jruby.compiler.ir.IR_Closure;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IR_Scope;
import org.jruby.compiler.ir.IR_Script;
import org.jruby.interpreter.InterpreterContext;

public class MetaObject extends Operand {
    public final IR_Scope scope;

    public MetaObject(IR_Scope s) {
        scope = s;
    }

    @Override
    public String toString() {
        if (scope instanceof IR_Class) {
            return "Class " + ((IR_Class) scope).getName();
        } else if (scope instanceof IR_Module) {
            return "Module " + ((IR_Module) scope).getName();
        } else if (scope instanceof IRMethod) {
            return "Method " + ((IRMethod) scope).getName();
        } else if (scope instanceof IR_Script) {
            return "Script " + ((IR_Script) scope)._fileName;
        } else {
            return ((IR_Closure) scope).toString().replace("\t", "\t\t");
        }
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    public boolean isClass() {
        return scope instanceof IR_Class;
    }

    public Operand getContainer() {
        return scope.getContainer();
    }

    // SSS FIXME: Incomplete!
    @Override
    public IR_Class getTargetClass() {
        return (scope instanceof IR_Module) ? IR_Class.getCoreClass("Module") : null;
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
        System.out.println("METAOOBJECT RETRIEVE: " + scope + ", C: " + scope.getClass().getSimpleName());
        return null;
    }


}
