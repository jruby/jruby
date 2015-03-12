package org.jruby.ir.operands;

// Records the nil object

import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;

/**
 * Represents Block.NULL block, the block that cannot be yielded.
 */
public class NullBlock extends ImmutableLiteral {
    public static final NullBlock INSTANCE = new NullBlock();

    private NullBlock() {
        super(OperandType.NULL_BLOCK);
    }

    @Override
    public String toString() {
        return "null_block";
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return Block.NULL_BLOCK;
    }

    public static NullBlock decode(IRReaderDecoder d) {
        return INSTANCE;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.NullBlock(this);
    }
}
