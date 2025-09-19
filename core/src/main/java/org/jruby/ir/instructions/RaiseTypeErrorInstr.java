package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.api.Error;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RaiseTypeErrorInstr extends NoOperandInstr implements FixedArityInstr {
    private final String message;
    private RubyModule kernel;
    private RubyModule typeError;

    public RaiseTypeErrorInstr(String message) {
        super(Operation.RAISE_TYPE_ERROR);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "typeErr: " + message};
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new RaiseTypeErrorInstr(message);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(message);
    }

    public static RaiseTypeErrorInstr decode(IRReaderDecoder d) {
        return new RaiseTypeErrorInstr(d.decodeString());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        throw Error.typeError(context, message);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.RaiseTypeErrorInstr(this);
    }
}
