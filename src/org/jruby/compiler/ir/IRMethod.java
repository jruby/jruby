package org.jruby.compiler.ir;

import java.util.List;
import java.util.ArrayList;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.ReceiveArgumentInstruction;
import org.jruby.compiler.ir.instructions.ReceiveSelfInstruction;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.parser.LocalStaticScope;
import org.jruby.parser.StaticScope;

public class IRMethod extends IRExecutionScope {
    public final boolean isInstanceMethod;

    public final Label startLabel; // Label for the start of the method
    public final Label endLabel;   // Label for the end of the method

    // SSS FIXME: Token can be final for a method -- implying that the token is only for this particular implementation of the method
    // But, if the mehod is modified, we create a new method object which in turn gets a new token.  What makes sense??  Intuitively,
    // it seems the first one ... but let us see ...
    private CodeVersion version;   // Current code version for this method -- can change during execution as methods get redefined!

    // Call parameters
    private List<Operand> callArgs;

    public IRMethod(IRScope lexicalParent, Operand container, String name, boolean isInstanceMethod, StaticScope staticScope) {
        super(lexicalParent, container, name, staticScope);
        this.isInstanceMethod = isInstanceMethod;
        startLabel = getNewLabel("_METH_START");
        endLabel = getNewLabel("_METH_END");
        callArgs = new ArrayList<Operand>();
        updateVersion();
    }

    public void updateVersion() {
        version = CodeVersion.getClassVersionToken();
    }

    public String getScopeName() {
        return "Method";
    }

    public CodeVersion getVersion() {
        return version;
    }

    @Override
    public void addInstr(Instr i) {
        // Accumulate call arguments
		  // SSS FIXME: ReceiveSelf should inherit from ReceiveArg?
        if ((i instanceof ReceiveArgumentInstruction) || (i instanceof ReceiveSelfInstruction)) callArgs.add(i.result);

        super.addInstr(i);
    }

    public Operand[] getCallArgs() { 
        return callArgs.toArray(new Operand[callArgs.size()]);
    }

    @Override
    public void setConstantValue(String constRef, Operand val) {
        if (!isAClassRootMethod()) throw new NotCompilableException("Unexpected: Encountered set constant value in a method!");
        
        ((MetaObject) container).scope.setConstantValue(constRef, val);
    }

    public boolean isAClassRootMethod() { 
        return IRModule.isAClassRootMethod(this);
    }

    // SSS FIXME: Incorect!
    // ENEBO: Should it be: return (m == null) ? ":" + getName() : m.getName() + ":" + getName();
    public String getFullyQualifiedName() {
        IRModule m = getDefiningModule();
        
        return (m == null) ? null : m.getName() + ":" + getName();
    }

    public IRModule getDefiningModule() {
        if (!(container instanceof MetaObject)) return null;

        IRScope scope = ((MetaObject) container).scope;

        // FIXME: This is a hot mess and probably should be a while loop...but perhaps bigger change is needed
        if (scope instanceof IRMethod) {
            scope = ((MetaObject) ((IRMethod) scope).container).scope;
        }

        return (IRModule) scope;
    }

    @Override
    protected StaticScope constructStaticScope(StaticScope unused) {
        LocalStaticScope newScope = new LocalStaticScope(null); // method scopes cannot see any lower

        this.requiredArgs = 0;
        this.optionalArgs = 0;
        this.restArg = -1;

        return newScope;
    }
}
