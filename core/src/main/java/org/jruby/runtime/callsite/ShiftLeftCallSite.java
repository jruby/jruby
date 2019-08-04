package org.jruby.runtime.callsite;

import org.jruby.RubyFixnum;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ShiftLeftCallSite extends MonomorphicCallSite {
    public ShiftLeftCallSite() {
        super("<<");
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, long arg1) {
        if (self instanceof RubyFixnum && cache instanceof FixnumEntry) {
            return ((RubyFixnum) self).op_lshift(context, arg1);
        }
        return super.call(context, caller, self, arg1);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1) {
        if (self instanceof RubyFixnum && cache instanceof FixnumEntry) {
            return ((RubyFixnum) self).op_lshift(context, arg1);
        }
        return super.call(context, caller, self, arg1);
    }

    @Override
    protected CacheEntry setCache(final CacheEntry entry, final IRubyObject self) {
        // used as a primary cache - for Fixnum targets
        if (self instanceof RubyFixnum && entry.method.isBuiltin()) {
            return cache = new FixnumEntry(entry); // tagged entry replacement - a (costly) isBuiltin replacement
        }
        return cache = entry;
    }

    @Override
    public boolean isBuiltin(final IRubyObject self) {
        if (self instanceof RubyFixnum && cache instanceof FixnumEntry) return true;
        return super.isBuiltin(self);
    }

    private static class FixnumEntry extends CacheEntry {

        FixnumEntry(CacheEntry entry) {
            super(entry.method, entry.sourceModule, entry.token);
        }

    }

}
