package org.jruby.ir;

import java.util.ArrayList;
import java.util.List;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ReceiveArgBase;
import org.jruby.ir.instructions.ReceiveRestArgInstr;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.Variable;
import org.jruby.parser.IRStaticScope;
import org.jruby.parser.StaticScope;

public class IRMethod extends IRScope {
    public final boolean isInstanceMethod;

    // SSS FIXME: Note that if operands from the method are modified,
    // callArgs would have to be updated as well
    // Call parameters
    private List<Operand> callArgs;

    // Argument description of the form [:req, "a"], [:opt, "b"] ..
    private List<String[]> argDesc;

    public IRMethod(IRManager manager, IRScope lexicalParent, String name,
            boolean isInstanceMethod, int lineNumber, StaticScope staticScope) {
        super(manager, lexicalParent, name, lexicalParent.getFileName(), lineNumber, staticScope);

        this.isInstanceMethod = isInstanceMethod;
        this.callArgs = new ArrayList<Operand>();
        this.argDesc = new ArrayList<String[]>();
        if (!getManager().isDryRun()) {
            if (staticScope != null) ((IRStaticScope)staticScope).setIRScope(this);
        }
    }

    public String getScopeName() {
        return "Method";
    }

    @Override
    public void addInstr(Instr i) {
        // Accumulate call arguments
        if (i instanceof ReceiveRestArgInstr) callArgs.add(new Splat(((ReceiveRestArgInstr)i).getResult()));
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

    @Override
    public LocalVariable findExistingLocalVariable(String name, int scopeDepth) {
        assert scopeDepth == 0: "Local variable depth in IRMethod should always be zero (" + name + " had depth of " + scopeDepth + ")";
        return localVars.getVariable(name);
    }

    @Override
    public LocalVariable getNewLocalVariable(String name, int scopeDepth) {
        assert scopeDepth == 0: "Local variable depth in IRMethod should always be zero (" + name + " had depth of " + scopeDepth + ")";
        LocalVariable lvar = new LocalVariable(name, 0, localVars.nextSlot);
        localVars.putVariable(name, lvar);
        return lvar;
    }

    @Override
    public LocalVariable getLocalVariable(String name, int scopeDepth) {
        LocalVariable lvar = findExistingLocalVariable(name, scopeDepth);
        if (lvar == null) lvar = getNewLocalVariable(name, scopeDepth);
        return lvar;
    }

    public LocalVariable getImplicitBlockArg() {
        return getLocalVariable(Variable.BLOCK, 0);
    }
}
