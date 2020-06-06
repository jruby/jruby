package org.jruby.ir.instructions;

import org.jruby.RubySymbol;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.GlobalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PutGlobalVarInstr extends TwoOperandInstr implements FixedArityInstr {
    public PutGlobalVarInstr(RubySymbol varName, Operand value) {
        this(new GlobalVariable(varName), value);
    }

    public PutGlobalVarInstr(GlobalVariable gvar, Operand value) {
        super(Operation.PUT_GLOBAL_VAR, gvar, value);
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        switch (getTarget().getId()) {
            case "$_" : case "$LAST_READ_LINE" :
                scope.getFlags().add(IRFlags.REQUIRES_LASTLINE);
                break;
            case "$~" : case "$LAST_MATCH_INFO" :
            case "$`" : case "$PREMATCH" :
            case "$'" : case "$POSTMATCH" :
            case "$+" : case "$LAST_PAREN_MATCH" :
                scope.getFlags().add(IRFlags.REQUIRES_BACKREF);
                return true;
        }
        return false;
    }

    public GlobalVariable getTarget() {
        return (GlobalVariable) getOperand1();
    }

    public Operand getValue() {
        return getOperand2();
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new PutGlobalVarInstr(getTarget().getName(), getValue().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getTarget());
        e.encode(getValue());
    }

    public static PutGlobalVarInstr decode(IRReaderDecoder d) {
        return new PutGlobalVarInstr((GlobalVariable) d.decodeOperand(), d.decodeOperand());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        GlobalVariable target = getTarget();
        IRubyObject    value  = (IRubyObject) getValue().retrieve(context, self, currScope, currDynScope, temp);
        context.runtime.getGlobalVariables().set(target.getId(), value);
        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PutGlobalVarInstr(this);
    }
}
