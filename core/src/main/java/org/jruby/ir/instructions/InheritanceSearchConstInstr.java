package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.*;
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

public class InheritanceSearchConstInstr extends OneOperandResultBaseInstr implements FixedArityInstr {
    private RubySymbol constName;

    // Constant caching
    private final ConstantLookupSite site;

    public InheritanceSearchConstInstr(Variable result, Operand currentModule, RubySymbol constName) {
        super(Operation.INHERITANCE_SEARCH_CONST, result, currentModule);

        assert result != null: "InheritanceSearchConstInstr result is null";

        this.constName = constName;
        this.site = new ConstantLookupSite(constName);
    }

    public Operand getCurrentModule() {
        return getOperand1();
    }

    public String getId() {
        return constName.idString();
    }

    public RubySymbol getName() {
        return constName;
    }

    @Deprecated(since = "9.1.3.0")
    public boolean isNoPrivateConsts() {
        return false;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new InheritanceSearchConstInstr(ii.getRenamedVariable(result), getCurrentModule().cloneForInlining(ii), getName());
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "name: " + getName() };
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getName());
    }

    public static InheritanceSearchConstInstr decode(IRReaderDecoder d) {
        return new InheritanceSearchConstInstr(d.decodeVariable(), d.decodeOperand(), d.decodeSymbol());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Object cmVal = getCurrentModule().retrieve(context, self, currScope, currDynScope, temp);

        return site.inheritanceSearchConst(context, (IRubyObject) cmVal);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.InheritanceSearchConstInstr(this);
    }
}
