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
import org.jruby.parser.LocalStaticScope;
import org.jruby.parser.StaticScope;

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

    // All local variables (their names) are mapped to an integer id in this method's lexical scope (including all nested closures)
    private int nextLocalVariableSlot;
    private Map<String, LocalVariable> localVariables;

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
        bindingSlotMap = new HashMap<String, Integer>();
        nextAvailableBindingSlot = 0;
        localVariables = new HashMap<String, LocalVariable>();
        nextLocalVariableSlot = 0;
        updateVersion();
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
        if (!isAClassRootMethod()) throw new NotCompilableException("Unexpected: Encountered set constant value in a method!");
        
        ((MetaObject) container).scope.setConstantValue(constRef, val);
    }
**/

    public boolean isAClassRootMethod() { 
        return IRModule.isAClassRootMethod(this);
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
        LocalStaticScope newScope = new LocalStaticScope(null); // method scopes cannot see any lower

        this.requiredArgs = 0;
        this.optionalArgs = 0;
        this.restArg = -1;

        return newScope;
    }

    public LocalVariable getLocalVariable(String name, IRExecutionScope currScope) {
        LocalVariable lvar = localVariables.get(name);
        if (lvar == null) {
            if ((currScope instanceof IRClosure) && !((IRClosure)currScope).isForLoopBody) {
                // Variable is local to the closure.  If not, it would have been found in the localVariables.get call above!
                lvar = new ClosureLocalVariable((IRClosure)currScope, name, nextLocalVariableSlot);
            }
            else {
                lvar = new LocalVariable(name, nextLocalVariableSlot);
            }
            localVariables.put(name, lvar);
            nextLocalVariableSlot++;
        }

        /* --------------------------------------------------------------------------------
         * If 'lvar' was previously a closure-local-variable, one of 3 things can happen:
         * (1) We are now in a method scope ==> reset 'lvar' to be a normal variable
         * (2) We are now in the same closure scope in which 'lvar' was defined -- nothing to do
         *     this use of 'lvar' is still closure-local.
         * (3) We are now in a closure scope which is nested in the scope that defined 'lvar'.
         *     So, 'lvar' use in 'currScope' is still "local" to a closure.
         * (4) We are now in a closure scope which is not nested in the scope that defined 'lvar'
         *     So, we are just reusing the variable name, but this is a new variable.
         *     Reset defining scope for 'lvar' to be 'currScope'
         * -------------------------------------------------------------------------------- */
        if (lvar instanceof ClosureLocalVariable) {
            if (currScope instanceof IRMethod) {
                // SCENARIO 1 ABOVE
                lvar = new LocalVariable(name, lvar.getLocation());
                localVariables.put(name, lvar);
            }
            else {
                IRClosure definingScope = ((ClosureLocalVariable)lvar).definingScope;
                if ((currScope == definingScope) || currScope.nestedInClosure(definingScope)) {
                    // SCENARIO 2 & 3 ABOVE.  Nothing to do
                    ;
                }
                else {
                    // SCENARIO 4 ABOVE
                    lvar = new ClosureLocalVariable((IRClosure)currScope, name, lvar.getLocation());
                    localVariables.put(name, lvar);
                }
            }
        }

        return lvar;
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

    @Override
    public int getLocalVariablesCount() {
        return nextLocalVariableSlot;
    }
}
