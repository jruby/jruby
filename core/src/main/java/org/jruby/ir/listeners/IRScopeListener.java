package org.jruby.ir.listeners;

import org.jruby.ast.Node;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;

public interface IRScopeListener {
    
    public void addedInstr(IRScope scope, Instr instr);
    
    public void startBuildOperand(Node node, IRScope scope);
    
    public void endBuildOperand(Node node, IRScope scope, Operand operand);
    
}
