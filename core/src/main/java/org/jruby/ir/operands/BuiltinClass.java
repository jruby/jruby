package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.List;

import static org.jruby.api.Access.arrayClass;
import static org.jruby.api.Access.hashClass;
import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Access.symbolClass;

/**
 * Reference to common builtin types we care about: Object, Array, Hash.  Tends to be used
 * for comparisons like ===.
 */
public class BuiltinClass extends Operand {
    public enum Type {
        OBJECT, ARRAY, HASH, SYMBOL;

        public static Type fromOrdinal(int value) {
            return value < 0 || value >= values().length ? null : values()[value];
        }
    }

    private final Type type;

    public BuiltinClass(Type type) {
        super();
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.BUILTIN_CLASS;
    }

    @Override
    public String toString() {
        return "<Class:" + type + ">";
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    @Override
    public void addUsedVariables(List<Variable> l) {
        /* Nothing to do */
    }

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        return this;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(type.ordinal());
    }

    public static BuiltinClass decode(IRReaderDecoder d) {
        Type type = Type.fromOrdinal(d.decodeInt());
        return switch (type) {
            case ARRAY -> d.getCurrentScope().getManager().getArrayClass();
            case HASH -> d.getCurrentScope().getManager().getHashClass();
            case OBJECT -> d.getCurrentScope().getManager().getObjectClass();
            case SYMBOL -> d.getCurrentScope().getManager().getSymbolClass();
            default -> throw new RuntimeException("BuiltinClass has unknown type");
        };
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        return switch (type) {
            case ARRAY -> arrayClass(context);
            case HASH -> hashClass(context);
            case OBJECT -> objectClass(context);
            case SYMBOL -> symbolClass(context);
            default -> throw new RuntimeException("BuiltinClass has unknown type");
        };
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BuiltinClass(this);
    }
}
