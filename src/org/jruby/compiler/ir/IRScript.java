package org.jruby.compiler.ir;

import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.StringLiteral;
import org.jruby.parser.StaticScope;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;

public class IRScript extends IRScopeImpl {
    private final IRClass dummyClass;  // Dummy class for the script

    public IRScript(String className, String sourceName, StaticScope staticScope) {
        super((IRScope) null, null, sourceName, staticScope);
        dummyClass = new IRClass(this, null, null, "__SCRIPT_ROOT__", staticScope);
    }

    public Operand getFileName() {
        return new StringLiteral(getName());
    }

    @Override
    public String getScopeName() {
        return "Script";
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

    public void runCompilerPass(CompilerPass p) {
        dummyClass.runCompilerPass(p);
    }
}
