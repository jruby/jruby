package org.jruby.ir.instructions;

import org.jcodings.Encoding;
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

    final private int options;
    final private RECache reCache;

    // Only used by cloning
    private BuildDynRegExpInstr(Variable result, Operand[] pieces, int embedded, RECache reCache) {
        super(Operation.BUILD_DREGEXP, result, pieces);
        this.options = embedded;
        this.reCache = reCache;
    }

    public BuildDynRegExpInstr(Variable result, Operand[] pieces, RegexpOptions options) {
        this(result, pieces, options.toEmbeddedOptions());
    }

    public BuildDynRegExpInstr(Variable result, Operand[] pieces, int options) {
        super(Operation.BUILD_DREGEXP, result, pieces);
        this.options = options;
        this.reCache = new RECache();
    }

    public Operand[] getPieces() {
       return getOperands();
    }

    public int getOptions() {
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

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getPieces());
        e.encode(getOptions());
    }

    public static BuildDynRegExpInstr decode(IRReaderDecoder d) {
        return new BuildDynRegExpInstr(d.decodeVariable(),
                d.decodeOperandArray(),d.decodeInt());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        // FIXME (from RegexpNode.java): 1.9 should care about internal or external encoding and not kcode.
        // If we have a constant regexp string or if the regexp patterns asks for caching, cache the regexp
        boolean once = RegexpOptions.isOnce(options);
        if (reCache.rubyRegexp == null || !once || context.runtime.getKCode() != reCache.rubyRegexp.getKCode()) {
            RubyString pattern = preprocessPattern(context, currScope, currDynScope, self, temp);
            RubyRegexp re = RubyRegexp.newDRegexp(context.runtime, pattern, options);
            re.setLiteral();
            reCache.updateCache(once, re);
        }

        return reCache.rubyRegexp;
    }

    private RubyString preprocessPattern(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Operand[] operands = getOperands();
        int length = operands.length;
        RubyString pattern = null;
        Encoding[] regexpEnc = context.encodingHolder();

        for (int i = 0; i < length; i++) {
            pattern = RubyRegexp.preprocessDRegexpElement(context.runtime, pattern, regexpEnc, (RubyString) operands[i].retrieve(context, self, currScope, currDynScope, temp), RegexpOptions.isEncodingNone(options));
        }

        if (regexpEnc[0] != null) pattern.setEncoding(regexpEnc[0]);

        return pattern;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BuildDynRegExpInstr(this);
    }
}
