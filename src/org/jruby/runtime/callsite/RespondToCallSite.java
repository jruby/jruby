package org.jruby.runtime.callsite;

import org.jruby.CompatVersion;
import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyClass;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Visibility;

public class RespondToCallSite extends NormalCachingCallSite {
    private volatile RespondToTuple respondToTuple = RespondToTuple.NULL_CACHE;

    private static class RespondToTuple {
        static final RespondToTuple NULL_CACHE = new RespondToTuple("", true, CacheEntry.NULL_CACHE, CacheEntry.NULL_CACHE, null);
        public final String name;
        public final boolean checkVisibility;
        public final CacheEntry respondToMethod;
        public final CacheEntry entry;
        public final IRubyObject respondsTo;
        
        public RespondToTuple(String name, boolean checkVisibility, CacheEntry respondToMethod, CacheEntry entry, IRubyObject respondsTo) {
            this.name = name;
            this.checkVisibility = checkVisibility;
            this.respondToMethod = respondToMethod;
            this.entry = entry;
            this.respondsTo = respondsTo;
        }

        public boolean cacheOk(RubyClass klass) {
            return respondToMethod.typeOk(klass) && entry.typeOk(klass);
        }
    }

    public RespondToCallSite() {
        super("respond_to?");
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject name) { 
        RubyClass klass = self.getMetaClass();
        RespondToTuple tuple = respondToTuple;
        if (tuple.cacheOk(klass)) {
            String strName = name.asJavaString();
            if (strName.equals(tuple.name) && tuple.checkVisibility) return tuple.respondsTo;
        }
        // go through normal call logic, which will hit overridden cacheAndCall
        return super.call(context, caller, self, name);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject name, IRubyObject bool) {
        RubyClass klass = self.getMetaClass();
        RespondToTuple tuple = respondToTuple;
        if (tuple.cacheOk(klass)) {
            String strName = name.asJavaString();
            if (strName.equals(tuple.name) && !bool.isTrue() == tuple.checkVisibility) return tuple.respondsTo;
        }
        // go through normal call logic, which will hit overridden cacheAndCall
        return super.call(context, caller, self, name, bool);
    }

    @Override
    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg);
        }

        // alternate logic to cache the result of respond_to if it's the standard one
        // FIXME: 1.9's respond_to_missing breaks this, so we have to bail out
        if (!context.runtime.is1_9() &&
                entry.method == context.getRuntime().getRespondToMethod()) {
            String name = arg.asJavaString();
            RespondToTuple tuple = recacheRespondsTo(entry, name, selfType, true, context);
            respondToTuple = tuple;
            return tuple.respondsTo;
        }

        // normal logic if it's not the builtin respond_to? method
        cache = entry;
        return method.call(context, self, selfType, methodName, arg);
    }

    @Override
    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg0, arg1);
        }

        // alternate logic to cache the result of respond_to if it's the standard one
        // FIXME: 1.9's respond_to_missing breaks this, so we have to bail out
        // FIXME: 1.9's respond_to_missing breaks this, so we have to bail out
        if (!context.runtime.is1_9() &&
                entry.method == context.runtime.getRespondToMethod()) {
            String name = arg0.asJavaString();
            RespondToTuple tuple = recacheRespondsTo(entry, name, selfType, !arg1.isTrue(), context);
            respondToTuple = tuple;
            return tuple.respondsTo;
        }

        // normal logic if it's not the builtin respond_to? method
        cache = entry;
        return method.call(context, self, selfType, methodName, arg0, arg1);
    }

    private static RespondToTuple recacheRespondsTo(CacheEntry respondToMethod, String newString, RubyClass klass, boolean checkVisibility, ThreadContext context) {
        Ruby runtime = context.getRuntime();
        CacheEntry respondToLookupResult = klass.searchWithCache(newString);
        IRubyObject respondsTo;
        if (!respondToLookupResult.method.isUndefined() && !respondToLookupResult.method.isNotImplemented()) {
            respondsTo = checkVisibilityAndCache(respondToLookupResult, checkVisibility, runtime);
        } else {
            respondsTo = runtime.getFalse();
        }
        return new RespondToTuple(newString, checkVisibility, respondToMethod, respondToLookupResult, respondsTo);
    }

    private static IRubyObject checkVisibilityAndCache(CacheEntry respondEntry, boolean checkVisibility, Ruby runtime) {
        if (!checkVisibility || respondEntry.method.getVisibility() != Visibility.PRIVATE) {
            return runtime.getTrue();
        } else {
            return runtime.getFalse();
        }
    }
}