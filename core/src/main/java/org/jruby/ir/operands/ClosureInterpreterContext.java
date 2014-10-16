package org.jruby.ir.operands;

import java.util.EnumSet;
import org.jruby.ir.IRFlags;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by enebo on 10/14/14.
 */
public class ClosureInterpreterContext extends InterpreterContext {
    private Operand self;
    private StaticScope staticScope;
    private BlockBody body;

    public ClosureInterpreterContext(int temporaryVariablecount, int temporaryBooleanVariablecount,
                                     int temporaryFixnumVariablecount, int temporaryFloatVariablecount,
                                     EnumSet<IRFlags> flags, Instr[] instructions,
                                     Operand self, StaticScope staticScope, BlockBody body) {
        super(temporaryVariablecount, temporaryBooleanVariablecount, temporaryFixnumVariablecount,
                temporaryFloatVariablecount, flags, instructions);

        this.self = self;
        this.staticScope = staticScope;
        this.body = body;
    }

    public StaticScope getStaticScope() { return staticScope; }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        staticScope.determineModule();

        // In non-inlining scenarios, this.self will always be %self.
        // However, in inlined scenarios, this.self will be the self in the original scope where the closure
        // was present before inlining.
        IRubyObject selfVal = (this.self instanceof Self) ? self : (IRubyObject) this.self.retrieve(context, self, currScope, currDynScope, temp);
        Binding binding = context.currentBinding(selfVal, currDynScope);

        return new Block(body, binding);
    }
}
