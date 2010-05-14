package org.jruby.compiler.ir.operands;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class BreakResult extends Operand
{
    final public Operand _result;
    final public Label   _jumpTarget;

    public BreakResult(Operand v, Label l) { _result = v; _jumpTarget = l; }

    public String toString() { return "BRK(" + _result + ", " + _jumpTarget + ")"; }

    public Operand cloneForInlining(InlinerInfo ii) { 
        return new BreakResult(_result.cloneForInlining(ii), ii.getRenamedLabel(_jumpTarget));
    }
}
