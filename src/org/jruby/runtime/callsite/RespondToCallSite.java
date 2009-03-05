package org.jruby.runtime.callsite;

import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyClass;
import org.jruby.runtime.Visibility;

public class RespondToCallSite extends NormalCachingCallSite {
    private volatile String lastString;
    private volatile CacheEntry respondEntry = CacheEntry.NULL_CACHE;
    private volatile IRubyObject respondsTo;

    public RespondToCallSite() {
        super("respond_to?");
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject name) {
        RubyClass klass = self.getMetaClass();
        if (isDefaultRespondTo(klass, context)) {
            String newString = name.asJavaString();
            if (isCacheInvalid(newString, klass)) {
                recacheRespondsTo(newString, klass, true, context);
            }
            return respondsTo;
        } else {
            return super.call(context, caller, self, name);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject name, IRubyObject bool) {
        RubyClass klass = self.getMetaClass();
        if (isDefaultRespondTo(klass, context)) {
            String newString = name.asJavaString();
            if (isCacheInvalid(newString, klass)) {
                recacheRespondsTo(newString, klass, !bool.isTrue(), context);
            }
            return respondsTo;
        } else {
            return super.call(context, caller, self, name, bool);
        }
    }

    // TODO: This and recacheRespondsTo needed to be synchronized for JRUBY-3466,
    // but this degraded performance nearly 2x. It's still faster than MRI, but
    // a reanalysis of this code may show a faster way to ensure we're caching
    // safely.
    private synchronized boolean isCacheInvalid(String newString, RubyClass klass) {
        return lastString != newString || !respondEntry.typeOk(klass);
    }

    private boolean isDefaultRespondTo(RubyClass klass, ThreadContext context) {
        return cache.typeOk(klass) && cache.method == context.getRuntime().getRespondToMethod();
    }

    private synchronized void recacheRespondsTo(String newString, RubyClass klass, boolean checkVisibility, ThreadContext context) {
        Ruby runtime = context.getRuntime();
        lastString = newString;
        respondEntry = klass.searchWithCache(newString);
        if (!respondEntry.method.isUndefined()) {
            respondsTo = checkVisibilityAndCache(respondEntry, checkVisibility, runtime);
        } else {
            respondsTo = runtime.getFalse();
        }
    }

    private static IRubyObject checkVisibilityAndCache(CacheEntry respondEntry, boolean checkVisibility, Ruby runtime) {
        if (!checkVisibility || respondEntry.method.getVisibility() != Visibility.PRIVATE) {
            return runtime.getTrue();
        } else {
            return runtime.getFalse();
        }
    }
}