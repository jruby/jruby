package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;

/**
 * Represents a literal regexp from ruby, constructed on first traversal and then cached.
 */
public class Regexp extends ImmutableLiteral {
    final public RegexpOptions options;
    final private ByteList source;

    public Regexp(ByteList source, RegexpOptions options) {
        super(OperandType.REGEXP);

        this.source = source;
        this.options = options;
    }

    public ByteList getSource() {
        return source;
    }

    @Override
    public String toString() {
        return "RE:|" + source + "|" + options;
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return IRRuntimeHelpers.newLiteralRegexp(context, source, options);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(source);
        e.encode(options.toEmbeddedOptions());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Regexp(this);
    }
}
