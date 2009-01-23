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
        if (cache.typeOk(klass) && cache.method == context.getRuntime().getRespondToMethod()) {
            String newString = name.asJavaString();
            if (lastString != newString || !respondEntry.typeOk(klass)) {
                lastString = newString;
                respondEntry = klass.searchWithCache(methodName);
                respondsTo = respondEntry.method.isUndefined() ? context.getRuntime().getTrue() : context.getRuntime().getFalse();
            }
            return respondsTo;
        } else {
            return super.call(context, caller, self, name);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject name, IRubyObject bool) {
        RubyClass klass = self.getMetaClass();
        if (cache.typeOk(klass) && cache.method == context.getRuntime().getRespondToMethod()) {
            String newString = name.asJavaString();
            if (lastString != newString || !respondEntry.typeOk(klass)) {
                lastString = newString;
                respondEntry = klass.searchWithCache(methodName);
                boolean checkVisibility = !bool.isTrue();
                if (!respondEntry.method.isUndefined()) {
                    respondsTo = !(checkVisibility && respondEntry.method.getVisibility() == Visibility.PRIVATE)? context.getRuntime().getTrue() : context.getRuntime().getFalse();
                } else {
                    respondsTo = context.getRuntime().getFalse();
                }
            }
            return respondsTo;
        } else {
            return super.call(context, caller, self, name, bool);
        }
    }
}