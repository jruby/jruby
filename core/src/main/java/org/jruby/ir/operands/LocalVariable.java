package org.jruby.ir.operands;

import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class LocalVariable extends Variable implements DepthCloneable {
    protected final RubySymbol name;
    protected final int scopeDepth;
    protected final int offset;
    protected final int hcode;
    // Note that we cannot use (scopeDepth > 0) check to detect this.
    // When a dyn-scope is eliminated for a leaf scope, depths for all
    // closure local vars are decremented by 1 => a non-local variable
    // can have scope depth 0.
    //
    // Can only transition in one direction (from true to false)
    protected final boolean isOuterScopeVar;

    public LocalVariable(RubySymbol name, int scopeDepth, int location) {
        super();
        this.name = name;
        this.scopeDepth = scopeDepth;
        this.isOuterScopeVar = scopeDepth > 0;
        this.offset = location;
        this.hcode = (name + ":" + offset).hashCode();
    }

    public LocalVariable(RubySymbol name, int scopeDepth, int location, boolean isOuterScopeVar) {
        super();
        this.name = name;
        this.scopeDepth = scopeDepth;
        this.isOuterScopeVar = isOuterScopeVar;
        this.offset = location;
        this.hcode = (name + ":" + offset).hashCode();
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.LOCAL_VARIABLE;
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
    public String getId() {
        return name.idString();
    }

    public RubySymbol getName() {
        return name;
    }

    @Override
    public String toString() {
        return isSelf() ? name.toString() : name + "(" + scopeDepth + ":" + offset + (isOuterScopeVar ? ":outer=true" : "") + ")";
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
        return n > scopeDepth ? new LocalVariable(name, n, offset) : new LocalVariable(name, n, offset, isOuterScopeVar);
    }

    public boolean isOuterScopeVar() {
        return isOuterScopeVar;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getName());

        int forCount = e.getCurrentScope().countForLoops();
        e.encode(getScopeDepth() - forCount);
        // We do not encode location because we rebuild lvars from IRScope when being rebuilt
    }

    public static LocalVariable decode(IRReaderDecoder d) {
        return d.getCurrentScope().getLocalVariable(d.decodeSymbol(), d.decodeInt());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.LocalVariable(this);
    }
}
