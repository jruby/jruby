package org.jruby.compiler.ir;

import java.util.List;
import java.util.ArrayList;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.instructions.ReceiveArgumentInstruction;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;

public class IRMethod extends IR_ExecutionScope {
    public final String  name;     // Ruby name
    public final boolean isInstanceMethod;

    public final Label startLabel; // Label for the start of the method
    public final Label endLabel;   // Label for the end of the method

    // SSS FIXME: Token can be final for a method -- implying that the token is only for this particular implementation of the method
    // But, if the mehod is modified, we create a new method object which in turn gets a new token.  What makes sense??  Intuitively,
    // it seems the first one ... but let us see ...
    private CodeVersion version;   // Current code version for this method -- can change during execution as methods get redefined!

    // Call parameters
    private List<Operand> callArgs;

    public IRMethod(IR_Scope lexicalParent, Operand container, String name, boolean isInstanceMethod) {
        super(lexicalParent, container);
        this.name = name;
        this.isInstanceMethod = isInstanceMethod;
        startLabel = getNewLabel("_METH_START");
        endLabel = getNewLabel("_METH_END");
        callArgs = new ArrayList<Operand>();
        updateVersion();
    }

    public void updateVersion() {
        version = CodeVersion.getClassVersionToken();
    }

    public CodeVersion getVersion() {
        return version;
    }

    @Override
    public void addInstr(IR_Instr i) {
        // Accumulate call arguments
        if (i instanceof ReceiveArgumentInstruction) callArgs.add(i._result);

        super.addInstr(i);
    }

    public Operand[] getCallArgs() { 
        return callArgs.toArray(new Operand[callArgs.size()]);
    }

    @Override
    public void setConstantValue(String constRef, Operand val) {
        if (!isAClassRootMethod()) throw new NotCompilableException("Unexpected: Encountered set constant value in a method!");
        
        ((MetaObject) _container)._scope.setConstantValue(constRef, val);
    }

    public boolean isAClassRootMethod() { 
        return IR_Module.isAClassRootMethod(this);
    }

    // SSS FIXME: Incorect!
    // ENEBO: Should it be: return (m == null) ? ":" + getName() : m.getName() + ":" + getName();
    public String getFullyQualifiedName() {
        IR_Module m = getDefiningModule();
        
        return (m == null) ? null : m.getName() + ":" + getName();
    }

    public IR_Module getDefiningModule() {
        return (_container instanceof MetaObject) ? (IR_Module)((MetaObject)_container)._scope : null;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Method: " + getName() + super.toString();
    }
}
