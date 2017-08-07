package org.jruby.runtime.callsite;

import org.jruby.Ruby;
import org.jruby.RubySymbol;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyClass;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.util.TypeConverter;

public class RespondToCallSite extends NormalCachingCallSite {
    private volatile RespondToTuple respondToTuple = RespondToTuple.NULL_CACHE;
    private final RubySymbol respondToName;
    private final RubySymbol respondToMissing;

    private static class RespondToTuple {
        static final RespondToTuple NULL_CACHE = new RespondToTuple(null, true, CacheEntry.NULL_CACHE, CacheEntry.NULL_CACHE);
        public final RubySymbol name;
        public final boolean checkVisibility;
        public final CacheEntry respondToMethod;
        public final CacheEntry entry;
        public final IRubyObject respondsTo;
        public final boolean respondsToBoolean;

        public RespondToTuple(RubySymbol name, boolean checkVisibility, CacheEntry respondToMethod, CacheEntry entry, IRubyObject respondsTo) {
            this.name = name;
            this.checkVisibility = checkVisibility;
            this.respondToMethod = respondToMethod;
            this.entry = entry;
            this.respondsTo = respondsTo;
            this.respondsToBoolean = respondsTo.isTrue();
        }

        public RespondToTuple(RubySymbol name, boolean checkVisibility, CacheEntry respondToMethod, CacheEntry entry) {
            this.name = name;
            this.checkVisibility = checkVisibility;
            this.respondToMethod = respondToMethod;
            this.entry = entry;
            this.respondsTo = null;
            this.respondsToBoolean = false;
        }

        public boolean cacheOk(RubyClass klass) {
            return respondToMethod.typeOk(klass) && entry.typeOk(klass);
        }
    }

    // For then the site is for respond_to? itself.
    public RespondToCallSite(Ruby runtime) {
        super(runtime.newSymbol("respond_to?"));
        respondToName = null;
        respondToMissing = runtime.newSymbol("respond_to_missing");
    }

    public RespondToCallSite(RubySymbol name) {
        super(name.getRuntime().newSymbol("respond_to?"));
        respondToName = name;
        respondToMissing = name.getRuntime().newSymbol("respond_to_missing");
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

    public boolean respondsTo(ThreadContext context, IRubyObject caller, IRubyObject self) {
        RubyClass klass = self.getMetaClass();
        RespondToTuple tuple = respondToTuple;
        if (tuple.cacheOk(klass)) {
            RubySymbol strName = respondToName;
            if (strName.equals(tuple.name) && tuple.checkVisibility) return tuple.respondsToBoolean;
        }
        // go through normal call logic, which will hit overridden cacheAndCall
        return super.call(context, caller, self, respondToName).isTrue();
    }

    public boolean respondsTo(ThreadContext context, IRubyObject caller, IRubyObject self, boolean includePrivate) {
        RubyClass klass = self.getMetaClass();
        RespondToTuple tuple = respondToTuple;
        if (tuple.cacheOk(klass)) {
            RubySymbol strName = respondToName;
            if (strName.equals(tuple.name) && !includePrivate == tuple.checkVisibility) return tuple.respondsToBoolean;
        }
        // go through normal call logic, which will hit overridden cacheAndCall
        return super.call(context, caller, self, respondToName, context.runtime.newBoolean(includePrivate)).isTrue();
    }

    @Override
    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg);
        }

        // alternate logic to cache the result of respond_to if it's the standard one
        if (entry.method.isBuiltin()) {
            RubySymbol name = TypeConverter.checkID(arg);
            RespondToTuple tuple = recacheRespondsTo(entry, name, selfType, true, context);

            // only cache if it does respond_to? OR there's no custom respond_to_missing? logic
            if (tuple.respondsTo.isTrue() ||
                    selfType.searchWithCache(respondToMissing).method == context.runtime.getRespondToMissingMethod()) {
                respondToTuple = tuple;
                return tuple.respondsTo;
            }
        }

        // normal logic if it's not the builtin respond_to? method
        cache = entry;
        // FIXME: Broken for mbc strings which are not Java charsets
        return method.call(context, self, selfType, methodName.asJavaString(), arg);
    }

    @Override
    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg0, arg1);
        }

        // alternate logic to cache the result of respond_to if it's the standard one
        if (entry.method.equals(context.runtime.getRespondToMethod())) {
            RubySymbol name = TypeConverter.checkID(arg0);
            RespondToTuple tuple = recacheRespondsTo(entry, name, selfType, !arg1.isTrue(), context);

            // only cache if it does respond_to? OR there's no custom respond_to_missing? logic
            if (tuple.respondsTo.isTrue() ||
                    selfType.searchWithCache(respondToMissing).method == context.runtime.getRespondToMissingMethod()) {
                respondToTuple = tuple;
                return tuple.respondsTo;
            }
        }

        // normal logic if it's not the builtin respond_to? method
        cache = entry;
        // FIXME: Broken for mbc strings which are not Java charsets
        return method.call(context, self, selfType, methodName.asJavaString(), arg0, arg1);
    }

    private static RespondToTuple recacheRespondsTo(CacheEntry respondToMethod, RubySymbol newString, RubyClass klass, boolean checkVisibility, ThreadContext context) {
        CacheEntry respondToLookupResult = klass.searchWithCache(newString);
        boolean respondsTo = Helpers.respondsToMethod(respondToLookupResult.method, checkVisibility);

        return new RespondToTuple(newString, checkVisibility, respondToMethod, respondToLookupResult, context.runtime.newBoolean(respondsTo));
    }
}