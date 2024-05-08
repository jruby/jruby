package org.jruby.ir.instructions;

import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.callsite.VariableCachingCallSite;

import java.util.Objects;

/**
 * A call to __method__ or __callee__ which can be optimized to use the frame method name directly.
 */
public class FrameNameCallInstr extends NoOperandResultBaseInstr implements FixedArityInstr {
    private final VariableCachingCallSite frameNameSite;
    private final boolean callee;

    public FrameNameCallInstr(Variable result, String methodName) {
        super(Operation.FRAME_NAME_CALL, Objects.requireNonNull(result, "FrameNameCallInstr result is null"));

        this.frameNameSite =
                new VariableCachingCallSite(Objects.requireNonNull(methodName, "FrameNameCallInstr methodName is null"));

        this.callee = methodName.equals("__callee__");
    }

    public String getMethodName() {
        return frameNameSite.getMethodName();
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new FrameNameCallInstr(ii.getRenamedVariable(result), frameNameSite.getMethodName());
    }

    public static FrameNameCallInstr decode(IRReaderDecoder d) {
        return new FrameNameCallInstr(d.decodeVariable(), d.decodeString());
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(frameNameSite.getMethodName());
    }

    public IRubyObject getFrameName(ThreadContext context, IRubyObject self, String compositeName) {
        CacheEntry frameNameEntry = frameNameSite.retrieveCache(self);

        if (!frameNameEntry.method.getRealMethod().isBuiltin()) {
            return frameNameSite.call(context, self, self);
        }

        return callee ?
                RubySymbol.newCalleeSymbolFromCompound(context.runtime, compositeName):
                RubySymbol.newMethodSymbolFromCompound(context.runtime, compositeName);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.FrameNameCallInstr(this);
    }
}
