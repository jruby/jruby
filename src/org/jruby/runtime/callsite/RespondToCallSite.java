package org.jruby.runtime.callsite;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyClass;
import org.jruby.runtime.Visibility;

public class RespondToCallSite extends NormalCachingCallSite {
    private String lastString;
    private CacheEntry respondEntry;
    private IRubyObject respondsTo;

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

    private boolean isCacheInvalid(String newString, RubyClass klass) {
        return lastString != newString || !respondEntry.typeOk(klass);
    }

    private boolean isDefaultRespondTo(RubyClass klass, ThreadContext context) {
        return cache.typeOk(klass) && cache.method == context.getRuntime().getRespondToMethod();
    }

    private void recacheRespondsTo(String newString, RubyClass klass, boolean checkVisibility, ThreadContext context) {
        lastString = newString;
        respondEntry = klass.searchWithCache(newString);
        if (!respondEntry.method.isUndefined()) {
            if (!checkVisibility || respondEntry.method.getVisibility() != Visibility.PRIVATE) {
                respondsTo = context.getRuntime().getTrue();
            } else {
                respondsTo = context.getRuntime().getFalse();
            }
        } else {
            respondsTo = context.getRuntime().getFalse();
        }
    }
}