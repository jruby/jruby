package org.jruby.runtime.callsite;

import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.RubyBasicObject.getMetaClass;

public class PlusCallSite extends BimorphicCallSite {

    public PlusCallSite() {
        super("+");
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, long arg1) {
        if (self instanceof RubyFixnum) {
            CacheEntry cache = this.cache;
            if (cache instanceof FixnumEntry && cache.typeOk(getMetaClass(self))) {
                return ((RubyFixnum) self).op_plus(context, arg1);
            }
        } else if (self instanceof RubyFloat) {
            CacheEntry cache = this.secondaryCache;
            if (cache instanceof FloatEntry && cache.typeOk(getMetaClass(self))) {
                return ((RubyFloat) self).op_plus(context, arg1);
            }
        }
        return super.call(context, caller, self, arg1);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1) {
        if (self instanceof RubyFixnum) {
            CacheEntry cache = this.cache;
            if (cache instanceof FixnumEntry && cache.typeOk(getMetaClass(self))) {
                return ((RubyFixnum) self).op_plus(context, arg1);
            }
        } else if (self instanceof RubyFloat) {
            CacheEntry cache = this.secondaryCache;
            if (cache instanceof FloatEntry && cache.typeOk(getMetaClass(self))) {
                return ((RubyFloat) self).op_plus(context, arg1);
            }
        }
        return super.call(context, caller, self, arg1);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, double arg1) {
        if (self instanceof RubyFixnum) {
            CacheEntry cache = this.cache;
            if (cache instanceof FixnumEntry && cache.typeOk(getMetaClass(self))) {
                return ((RubyFixnum) self).op_plus(context, arg1);
            }
        } else if (self instanceof RubyFloat) {
            CacheEntry cache = this.secondaryCache;
            if (cache instanceof FloatEntry && cache.typeOk(getMetaClass(self))) {
                return ((RubyFloat) self).op_plus(context, arg1);
            }
        }
        return super.call(context, caller, self, arg1);
    }

    @Override
    protected CacheEntry setCache(final CacheEntry entry, final IRubyObject self) {
        if (self instanceof RubyFixnum && entry.method.isBuiltin()) {
            return cache = new FixnumEntry(entry); // tagged entry - do isBuiltin check once
        }
        return cache = entry;
    }

    @Override
    protected CacheEntry setSecondaryCache(final CacheEntry entry, final IRubyObject self) {
        if (self instanceof RubyFloat && entry.method.isBuiltin()) {
            return secondaryCache = new FloatEntry(entry); // tagged entry - do isBuiltin check once
        }
        return secondaryCache = entry;
    }

    @Override
    public boolean isBuiltin(final IRubyObject self) {
        if (self instanceof RubyFixnum) {
            CacheEntry cache = this.cache;
            if (cache.typeOk(getMetaClass(self))) return cache instanceof FixnumEntry;
        }
        return super.isBuiltin(self);
    }

    @Override
    public boolean isSecondaryBuiltin(final IRubyObject self) {
        if (self instanceof RubyFloat) {
            CacheEntry cache = this.secondaryCache;
            if (cache.typeOk(getMetaClass(self))) return cache instanceof FloatEntry;
        }
        return super.isSecondaryBuiltin(self);
    }

    private static class FixnumEntry extends CacheEntry {

        FixnumEntry(CacheEntry entry) {
            super(entry.method, entry.sourceModule, entry.token);
        }

    }

    private static class FloatEntry extends CacheEntry {

        FloatEntry(CacheEntry entry) {
            super(entry.method, entry.sourceModule, entry.token);
        }

    }

}
