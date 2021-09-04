package org.jruby.ir.instructions;

import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.targets.simple.ConstantLookupSite;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.opto.ConstantCache;
import org.jruby.runtime.opto.Invalidator;

// The runtime method call that GET_CONST is translated to in this case will call
// a get_constant method on the scope meta-object which does the lookup of the constant table
// on the meta-object.  In the case of method & closures, the runtime method will delegate
// this call to the parent scope.

public class LexicalSearchConstInstr extends OneOperandResultBaseInstr implements FixedArityInstr {
    private RubySymbol constantName;

    // Constant caching
    private final ConstantLookupSite site;

    public LexicalSearchConstInstr(Variable result, Operand definingScope, RubySymbol constantName) {
        super(Operation.LEXICAL_SEARCH_CONST, result, definingScope);

        assert result != null: "LexicalSearchConstInstr result is null";

        this.constantName = constantName;
        this.site = new ConstantLookupSite(constantName);
    }

    public Operand getDefiningScope() {
        return getOperand1();
    }

    public String getId() {
        return constantName.idString();
    }

    public RubySymbol getName() {
        return constantName;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "name: " + constantName};
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new LexicalSearchConstInstr(ii.getRenamedVariable(result), getDefiningScope().cloneForInlining(ii), constantName);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getDefiningScope());
        e.encode(getName());
    }

    public static LexicalSearchConstInstr decode(IRReaderDecoder d) {
        return new LexicalSearchConstInstr(d.decodeVariable(), d.decodeOperand(), d.decodeSymbol());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        return site.lexicalSearchConst(context, (StaticScope) getDefiningScope().retrieve(context, self, currScope, currDynScope, temp));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.LexicalSearchConstInstr(this);
    }
}
