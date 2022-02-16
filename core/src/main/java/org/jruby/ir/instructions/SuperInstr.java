package org.jruby.ir.instructions;

import org.jruby.RubyInstanceConfig;
import org.jruby.RubySymbol;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.EnumSet;

public abstract class SuperInstr extends CallInstr {
    private final boolean isLiteralBlock;

    // clone constructor
    protected SuperInstr(IRScope scope, Operation op, Variable result, Operand receiver, RubySymbol name, Operand[] args,
                         Operand closure, boolean potentiallyRefined, CallSite callSite, long callSiteId) {
        super(scope, op, CallType.SUPER, result, name, receiver, args, closure, potentiallyRefined, callSite, callSiteId);

        isLiteralBlock = closure instanceof WrappedIRClosure;
    }

    // normal constructor
    public SuperInstr(IRScope scope, Operation op, Variable result, Operand definingModule, RubySymbol name, Operand[] args, Operand closure,
                      boolean isPotentiallyRefined) {
        super(scope, op, CallType.SUPER, result, name, definingModule, args, closure, isPotentiallyRefined);

        isLiteralBlock = closure instanceof WrappedIRClosure;
    }

    public abstract Operand getDefiningModule();
}
