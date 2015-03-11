package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A set of variables which are only used in a particular scope and never
 * visible to Ruby itself.
 */
public class TemporaryLocalVariable extends TemporaryVariable {
    public static final String PREFIX = "%v_";
    public final int offset;

    public TemporaryLocalVariable(String name, int offset) {
        super(name);

        this.offset = offset;
    }

    public TemporaryLocalVariable(int offset) {
        this(PREFIX + offset, offset);
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public TemporaryVariableType getType() {
        return TemporaryVariableType.LOCAL;
    }

    public String getPrefix() {
        return PREFIX;
    }

    @Override
    public Variable clone(SimpleCloneInfo ii) {
        return this;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        // SSS FIXME: When AddLocalVarLoadStoreInstructions pass is not enabled, we don't need this check.
        // We only need these because Ruby code can have local vars used before being defined.
        //
        //    a = 1 if always-false
        //    p a   # should print nil since a is not defined on the else path
        //
        // Now, when locals are promoted to temps, this local-var behavior gets transferred to tmp-vars as well!
        //
        // If can canonicalize Ruby code to get rid of use-before-defs, we can get rid of the null checks
        // both here and in DynamicScope var lookups.  To be done later.
        //
        // I dont like this at all.  This feels ugly!
        Object o = temp[offset];
        return o == null ? context.nil : o;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode((byte) getType().ordinal());
        e.encode(offset);
    }

    public static TemporaryLocalVariable decode(IRReaderDecoder d) {
        TemporaryVariableType type = d.decodeTemporaryVariableType();

        switch(type) {
            case CLOSURE: return TemporaryClosureVariable.decode(d);
            case CURRENT_MODULE: return TemporaryCurrentModuleVariable.decode(d);
            case CURRENT_SCOPE: return TemporaryCurrentScopeVariable.decode(d);
            case FLOAT: return TemporaryFloatVariable.decode(d);
            case FIXNUM: return TemporaryFixnumVariable.decode(d);
            case LOCAL: return new TemporaryLocalVariable(d.decodeInt());
        }
        return null; // Should not reach here.
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.TemporaryLocalVariable(this);
    }
}
