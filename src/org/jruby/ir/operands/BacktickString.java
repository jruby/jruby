package org.jruby.ir.operands;

import org.jruby.RubyBasicObject;
import org.jruby.RubyString;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// This represents a backtick string in Ruby
// Ex: `ls .`; `cp #{src} #{dst}`
//
// NOTE: This operand is only used in the initial stages of optimization.
// Further down the line, this string operand could get converted to calls
public class BacktickString extends Operand {
    final public List<Operand> pieces;

    public BacktickString(Operand val) {
        pieces = new ArrayList<Operand>();
        pieces.add(val);
    }

    public BacktickString(List<Operand> pieces) {
        this.pieces = pieces;
    }

    @Override
    public boolean hasKnownValue() {
        for (Operand o : pieces) {
            if (!o.hasKnownValue()) return false;
        }

        return true;
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force) {
        List<Operand> newPieces = new ArrayList<Operand>();
        for (Operand p : pieces) {
            newPieces.add(p.getSimplifiedOperand(valueMap, force));
        }

        return new BacktickString(newPieces);
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) {
        for (Operand o : pieces) {
            o.addUsedVariables(l);
        }
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) {
        if (hasKnownValue()) return this;

        List<Operand> newPieces = new ArrayList<Operand>();
        for (Operand p : pieces) {
            newPieces.add(p.cloneForInlining(ii));
        }
        
        return new BacktickString(newPieces);
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        RubyString newString = context.getRuntime().newString();

        for (Operand p: pieces) {
            RubyBasicObject piece = (RubyBasicObject) p.retrieve(context, self, currDynScope, temp);
            newString.append((piece instanceof RubyString) ? (RubyString) piece : piece.to_s());
        }
        
        return self.callMethod(context, "`", newString);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BacktickString(this);
    }
    
    @Override
    public String toString() {
        return "`" + (pieces == null ? "[]" : pieces) + "`";
    }
}
