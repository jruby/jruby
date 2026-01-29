package org.jruby.ir.instructions;

import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.InlineCloneInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;

import java.util.EnumSet;

public class CheckForLJEInstr extends NoOperandInstr {
    // We know the proc/lambda was not lexically made within a method scope.  If it is a lambda
    // then it will not matter but if it is a proc and it is not found to be within a define_method
    // closure then we will raise an LJE if this true.
    private final boolean definedWithinMethod;

    public CheckForLJEInstr(boolean notDefinedWithinMethod) {
        super(Operation.CHECK_FOR_LJE);

        this.definedWithinMethod = notDefinedWithinMethod;
    }

    @Deprecated(since = "9.1.9.0")
    public boolean maybeLambda() {
        return !definedWithinMethod;
    }

    public boolean isDefinedWithinMethod() {
        return definedWithinMethod;
    }

    @Override
    public Instr clone(CloneInfo info) {
        if (info instanceof InlineCloneInfo) {
            InlineCloneInfo ii = (InlineCloneInfo) info;

            // lexical closure
            if (ii.getScopeBeingInlined().isScopeContainedBy(ii.getHostScope())) return NopInstr.NOP;
        }

        return this;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(isDefinedWithinMethod());
    }

    public static CheckForLJEInstr decode(IRReaderDecoder d) {
        return new CheckForLJEInstr(d.decodeBoolean());
    }

    public void visit(IRVisitor visitor) {
        visitor.CheckForLJEInstr(this);
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "definedWithinMethod: " + definedWithinMethod};
    }

    public void check(ThreadContext context, DynamicScope dynamicScope, Block block) {
        IRRuntimeHelpers.checkForLJE(context, dynamicScope, definedWithinMethod, block);
    }

    @Override
    public boolean computeScopeFlags(IRScope scope, EnumSet<IRFlags> flags) {
        super.computeScopeFlags(scope, flags);
        flags.add(IRFlags.REQUIRES_DYNSCOPE);
        return true;
    }
}
