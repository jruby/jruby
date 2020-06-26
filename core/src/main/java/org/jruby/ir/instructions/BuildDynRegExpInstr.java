package org.jruby.ir.instructions;

import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.RegexpOptions;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

// Represents a dynamic regexp in Ruby
// Ex: /#{a}#{b}/
public class BuildDynRegExpInstr extends NOperandResultBaseInstr {
    // Create a cache object so that this can be shared
    // through the lifetime of the original instruction independent
    // of it being cloned (because of inlining, JIT-ting, whatever).
    private static class RECache  {
        // Cached regexp
        private volatile RubyRegexp rubyRegexp;
        private static final AtomicReferenceFieldUpdater<RECache, RubyRegexp> UPDATER =
                AtomicReferenceFieldUpdater.newUpdater(RECache.class, RubyRegexp.class, "rubyRegexp");
        public void updateCache(boolean isOnce, RubyRegexp re) {
            if (isOnce) {
                // Atomically update this, so we only see one instance cached ever.
                // See MRI's ruby/test_regexp.rb, test_once_multithread
                UPDATER.compareAndSet(this, null, re);
            } else {
                rubyRegexp = re;
            }
        }
    };

    final private RegexpOptions options;
    final private RECache reCache;

    // Only used by cloning
    private BuildDynRegExpInstr(Variable result, Operand[] pieces, RegexpOptions options, RECache reCache) {
        super(Operation.BUILD_DREGEXP, result, pieces);
        this.options = options;
        this.reCache = reCache;
    }

    public BuildDynRegExpInstr(Variable result, Operand[] pieces, RegexpOptions options) {
        super(Operation.BUILD_DREGEXP, result, pieces);
        this.options = options;
        this.reCache = new RECache();
    }

    public Operand[] getPieces() {
       return getOperands();
    }

    public RegexpOptions getOptions() {
       return options;
    }

    public RubyRegexp getRegexp() {
       return reCache.rubyRegexp;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"options: " + options};
    }

    @Override
    public Instr clone(CloneInfo ii) {
        // Share the cache!
        return new BuildDynRegExpInstr(ii.getRenamedVariable(result), cloneOperands(ii), options, this.reCache);
    }

    private RubyString[] retrievePieces(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        Operand[] operands = getOperands();
        int length = operands.length;
        RubyString[] strings = new RubyString[length];
        for (int i = 0; i < length; i++) {
            IRubyObject value = (IRubyObject) operands[i].retrieve(context, self, currScope, currDynScope, temp);

            // TODO: optimize the to_s call this does
            strings[i] = value.asString();

        }
        return strings;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getPieces());
        e.encode(getOptions().toEmbeddedOptions());
    }

    public static BuildDynRegExpInstr decode(IRReaderDecoder d) {
        return new BuildDynRegExpInstr(d.decodeVariable(),
                d.decodeOperandArray(),RegexpOptions.fromEmbeddedOptions(d.decodeInt()));
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        // FIXME (from RegexpNode.java): 1.9 should care about internal or external encoding and not kcode.
        // If we have a constant regexp string or if the regexp patterns asks for caching, cache the regexp
        if (reCache.rubyRegexp == null || !options.isOnce() || context.runtime.getKCode() != reCache.rubyRegexp.getKCode()) {
            RubyString[] pieces  = retrievePieces(context, self, currScope, currDynScope, temp);
            RubyString   pattern = RubyRegexp.preprocessDRegexp(context, options, pieces);
            RubyRegexp re = RubyRegexp.newDRegexp(context.runtime, pattern, options);
            re.setLiteral();
            reCache.updateCache(options.isOnce(), re);
        }

        return reCache.rubyRegexp;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BuildDynRegExpInstr(this);
    }
}
