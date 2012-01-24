package org.jruby.compiler.ir;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.ReceiveArgBase;
import org.jruby.compiler.ir.instructions.ReceiveRestArgBase;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Splat;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.parser.StaticScope;
import org.jruby.parser.IRStaticScope;

public class IRMethod extends IRScope {
    public final boolean isInstanceMethod;

    // SSS FIXME: Token can be final for a method -- implying that the token is only for this particular implementation of the method
    // But, if the method is modified, we create a new method object which in turn gets a new token.  What makes sense??  Intuitively,
    // it seems the first one ... but let us see ...
    private CodeVersion version;   // Current code version for this method -- can change during execution as methods get redefined!

    // SSS FIXME: Note that if operands from the method are modified,
    // callArgs would have to be updated as well
    // Call parameters
    private List<Operand> callArgs;

    // Argument description of the form [:req, "a"], [:opt, "b"] ..
    private List<String[]> argDesc;

    // Local variables (their names) are mapped to a slot in a binding shared across all call sites encountered in this method's lexical scope
    // (including all nested closures) -- only variables that need a slot get a slot.  This info is determined by the Binding*PlacementAnalysis
    // dataflow passes in dataflow/analyses/
    private int nextAvailableBindingSlot;
    private Map<String, Integer> bindingSlotMap;
    
    public IRMethod(IRScope lexicalParent, String name, boolean isInstanceMethod, int lineNumber, StaticScope staticScope) {
        super(lexicalParent, name, lexicalParent.getFileName(), lineNumber, staticScope);
        
        this.isInstanceMethod = isInstanceMethod;
        this.callArgs = new ArrayList<Operand>();
        this.argDesc = new ArrayList<String[]>();
        if (!IRBuilder.inIRGenOnlyMode()) {
            if (staticScope != null) ((IRStaticScope)staticScope).setIRScope(this);
            updateVersion();
        }
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
        if (i instanceof ReceiveRestArgBase) callArgs.add(new Splat(((ReceiveRestArgBase)i).getResult()));
        else if (i instanceof ReceiveArgBase) callArgs.add(((ReceiveArgBase) i).getResult());

        super.addInstr(i);
    }

    public void addArgDesc(String type, String argName) {
        argDesc.add(new String[]{type, argName});
    }

    public List<String[]> getArgDesc() {
        return argDesc;
    }

    public Operand[] getCallArgs() {
        return callArgs.toArray(new Operand[callArgs.size()]);
    }

    public LocalVariable findExistingLocalVariable(String name, int scopeDepth) {
        return localVars.getVariable(name);
    }

    public LocalVariable getNewLocalVariable(String name, int depth) {
        assert depth != 0: "Local variable depth in IRMethod should always be zero";
        LocalVariable lvar = new LocalVariable(name, 0, localVars.nextSlot);
        localVars.putVariable(name, lvar);
        return lvar;
    }

    public LocalVariable getLocalVariable(String name, int scopeDepth) {
        LocalVariable lvar = findExistingLocalVariable(name, scopeDepth);
        if (lvar == null) lvar = getNewLocalVariable(name, scopeDepth);
        return lvar;
    }

    public LocalVariable getImplicitBlockArg() {
        return getLocalVariable(Variable.BLOCK, 0);
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
