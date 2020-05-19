package org.jruby.runtime.callsite;

import org.jruby.RubyFixnum;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.RubyBasicObject.getMetaClass;

public class BitAndCallSite extends MonomorphicCallSite {

    public BitAndCallSite() {
        super("&");
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, long arg1) {
        if (self instanceof RubyFixnum) {
            CacheEntry cache = this.cache;
            if (cache instanceof FixnumEntry && cache.typeOk(getMetaClass(self))) {
                return ((RubyFixnum) self).op_and(context, arg1);
            }
        }
        return super.call(context, caller, self, arg1);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1) {
        if (self instanceof RubyFixnum) {
            CacheEntry cache = this.cache;
            if (cache instanceof FixnumEntry && cache.typeOk(getMetaClass(self))) {
                return ((RubyFixnum) self).op_and(context, arg1);
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
    public boolean isBuiltin(final IRubyObject self) {
        if (self instanceof RubyFixnum) {
            CacheEntry cache = this.cache;
            if (cache.typeOk(getMetaClass(self))) return cache instanceof FixnumEntry;
        }
        return super.isBuiltin(self);
    }

    private static class FixnumEntry extends CacheEntry {

        FixnumEntry(CacheEntry entry) {
            super(entry.method, entry.sourceModule, entry.token);
        }

    }

}
