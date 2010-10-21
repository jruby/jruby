package org.jruby.compiler.ir;

import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.StringLiteral;
import org.jruby.parser.StaticScope;

public class IRScript extends IRScopeImpl {
    public final String fileName;    // SSS FIXME: Should this be a string literal or a string?
    public final IRClass dummyClass;  // Dummy class for the script

    public IRScript(String className, String sourceName, StaticScope staticScope) {
        super((IRScope) null, null, staticScope);
        fileName = sourceName;
        dummyClass = new IRClass(this, null, null, "__SCRIPT_ROOT__", staticScope);
        addClass(dummyClass);
    }

    public Operand getFileName() {
        return new StringLiteral(fileName);
    }

    public IRMethod getRootMethod() {
        return dummyClass.getRootMethod();
    }

    public IRClass getRootClass() {
        return dummyClass;
    }

    @Override
    public String toString() {
        return "Script: file: " + getFileName() + super.toString();
    }

    public LocalVariable getLocalVariable(String name) {
        throw new UnsupportedOperationException("This should be happening on Root Method instead");
    }
}
