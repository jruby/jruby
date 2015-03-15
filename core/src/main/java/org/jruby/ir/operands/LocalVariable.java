package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class LocalVariable extends Variable implements DepthCloneable {
    protected final String name;
    protected final int scopeDepth;
    protected final int offset;
    protected final int hcode;

    // FIXME: We should resolve to an index into an array but localvariable has no allocator
    public LocalVariable(String name, int scopeDepth, int location) {
        this(OperandType.LOCAL_VARIABLE, name, scopeDepth, location);
    }

    protected LocalVariable(OperandType type, String name, int scopeDepth, int location) {
        super(type);
        this.name = name;
        this.scopeDepth = scopeDepth;
        this.offset = location;
        this.hcode = (name + ":" + offset).hashCode();
    }

    public boolean isSameDepth(LocalVariable other) {
        return getScopeDepth() == other.getScopeDepth();
    }

    public int getScopeDepth() {
        return scopeDepth;
    }

    public int getOffset() {
        return offset;
    }

    public int getLocation() {
        return offset;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return isSelf() ? name : name + "(" + scopeDepth + ":" + offset + ")";
    }

    @Override
    public int hashCode() {
        return hcode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof LocalVariable)) return false;

        return hashCode() == obj.hashCode();
    }

    public int compareTo(Object arg0) {
        // ENEBO: what should compareTo when it is not comparable?
        if (!(arg0 instanceof LocalVariable)) return 0;

        int a = hashCode();
        int b = arg0.hashCode();
        return a < b ? -1 : (a == b ? 0 : 1);
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        IRubyObject value = currDynScope.getValue(offset, scopeDepth);
        if (value == null) value = context.nil;
        return value;
    }

    @Override
    public Variable clone(SimpleCloneInfo ii) {
        return this;
    }

    public LocalVariable cloneForDepth(int n) {
        return new LocalVariable(name, n, offset);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getName());
        e.encode(getScopeDepth());
        // We do not encode location because we rebuild lvars from IRScope when being rebuilt
    }

    public static LocalVariable decode(IRReaderDecoder d) {
        return d.getCurrentScope().getLocalVariable(d.decodeString(), d.decodeInt());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.LocalVariable(this);
    }
}
