package org.jruby.compiler.ir;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.ReceiveArgumentInstruction;
import org.jruby.compiler.ir.instructions.ReceiveSelfInstruction;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.ClosureLocalVariable;
import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Splat;
import org.jruby.parser.StaticScope;
import org.jruby.parser.IRStaticScope;

public class IRMethod extends IRExecutionScope {
    public final boolean isInstanceMethod;

    public final Label startLabel; // Label for the start of the method
    public final Label endLabel;   // Label for the end of the method

    // SSS FIXME: Token can be final for a method -- implying that the token is only for this particular implementation of the method
    // But, if the method is modified, we create a new method object which in turn gets a new token.  What makes sense??  Intuitively,
    // it seems the first one ... but let us see ...
    private CodeVersion version;   // Current code version for this method -- can change during execution as methods get redefined!

    // Call parameters
    private List<Operand> callArgs;

    // Local variables (their names) are mapped to a slot in a binding shared across all call sites encountered in this method's lexical scope
    // (including all nested closures) -- only variables that need a slot get a slot.  This info is determined by the Binding*PlacementAnalysis
    // dataflow passes in dataflow/analyses/
    private int nextAvailableBindingSlot;
    private Map<String, Integer> bindingSlotMap;

    public IRMethod(IRScope lexicalParent, Operand container, String name, boolean isInstanceMethod, StaticScope staticScope) {
        super(lexicalParent, container, name, staticScope);
        this.isInstanceMethod = isInstanceMethod;
        startLabel = getNewLabel("_METH_START");
        endLabel = getNewLabel("_METH_END");
        callArgs = new ArrayList<Operand>();
        if (staticScope != null) ((IRStaticScope)staticScope).setIRScope(this);
        updateVersion();
/*
 * SSS: Only necessary when we run the add binding load/store dataflow pass to promote all ruby local vars to java local vars
 *
        bindingSlotMap = new HashMap<String, Integer>();
        nextAvailableBindingSlot = 0;
 */
    }

    public final void updateVersion() {
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
        if (i instanceof ReceiveArgumentInstruction) callArgs.add(((ReceiveArgumentInstruction) i).isRestOfArgArray() ? new Splat(i.result) : i.result);

        super.addInstr(i);
    }

    public Operand[] getCallArgs() { 
        return callArgs.toArray(new Operand[callArgs.size()]);
    }

/**
    @Override
    public void setConstantValue(String constRef, Operand val) {
        if (!isAModuleRootMethod()) throw new NotCompilableException("Unexpected: Encountered set constant value in a method!");
        
        ((MetaObject) container).scope.setConstantValue(constRef, val);
    }
**/

    public boolean isAModuleRootMethod() { 
        return IRModule.isAModuleRootMethod(this);
    }

    // SSS FIXME: Incorect!
    // ENEBO: Should it be: return (m == null) ? ":" + getName() : m.getName() + ":" + getName();
    public String getFullyQualifiedName() {
        IRModule m = getDefiningIRModule();
        
        return (m == null) ? null : m.getName() + ":" + getName();
    }

    public IRModule getDefiningIRModule() {
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
        this.requiredArgs = 0;
        this.optionalArgs = 0;
        this.restArg = -1;

        return StaticScope.newLocalScope(null); // method scopes cannot see any lower
    }

    public LocalVariable findExistingLocalVariable(String name) {
        return localVariables.get(name);
    }

    public LocalVariable getLocalVariable(String name, int scopeDepth) {
        LocalVariable lvar = findExistingLocalVariable(name);
        if (lvar == null) {
            lvar = new LocalVariable(name, scopeDepth, nextLocalVariableSlot);
            localVariables.put(name, lvar);
            nextLocalVariableSlot++;
        }

        return lvar;
    }

    public LocalVariable getImplicitBlockArg() {
        return getLocalVariable("%block", 0);
    }

    public LocalVariable getNewFlipStateVariable() {
        return getLocalVariable("%flip_" + allocateNextPrefixedName("%flip"), 0);
    }

    public int assignBindingSlot(String varName) {
        Integer slot = bindingSlotMap.get(varName);
        if (slot == null) {
            slot = nextAvailableBindingSlot;
            bindingSlotMap.put(varName, nextAvailableBindingSlot);
            nextAvailableBindingSlot++;
        }
        return slot;
    }

    public Integer getBindingSlot(String varName) {
        return bindingSlotMap.get(varName);
    }

    public int getBindingSlotsCount() {
        return nextAvailableBindingSlot;
    }
}
