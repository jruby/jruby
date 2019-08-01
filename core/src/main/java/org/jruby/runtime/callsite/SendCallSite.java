package org.jruby.runtime.callsite;

import org.jruby.RubyClass;
import org.jruby.RubySymbol;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.RubyBasicObject.getMetaClass;

public class SendCallSite extends FunctionalCachingCallSite {
    private volatile SendTuple sendTuple = SendTuple.NULL_CACHE;

    private static class SendTuple {
        static final SendTuple NULL_CACHE = new SendTuple(null, true, CacheEntry.NULL_CACHE, CacheEntry.NULL_CACHE);
        public final RubySymbol name;
        public final boolean checkVisibility;
        public final CacheEntry sendMethod;
        public final CacheEntry entry;

        public SendTuple(RubySymbol name, boolean checkVisibility, CacheEntry sendMethod, CacheEntry entry) {
            this.name = name;
            this.checkVisibility = checkVisibility;
            this.sendMethod = sendMethod;
            this.entry = entry;
        }

        public boolean cacheOk(RubyClass klass, IRubyObject symName) {
            return sendMethod.typeOk(klass) && symName == name && entry.typeOk(klass);
        }
    }

    public SendCallSite() {
        super("send");
    }

    public SendCallSite(String name) {
        super("send");
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject name) { 
        RubyClass klass = getMetaClass(self);
        SendTuple tuple = sendTuple;
        if (tuple.cacheOk(klass, name)) {
            String strName = name.asJavaString();
            return tuple.entry.method.call(context, self, tuple.entry.sourceModule, strName);
        }
        // go through normal call logic, which will hit overridden cacheAndCall
        return super.call(context, caller, self, name);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject name, IRubyObject arg0) {
        RubyClass klass = getMetaClass(self);
        SendTuple tuple = sendTuple;
        if (tuple.cacheOk(klass, name)) {
            String strName = name.asJavaString();
            return tuple.entry.method.call(context, self, tuple.entry.sourceModule, strName, arg0);
        }
        // go through normal call logic, which will hit overridden cacheAndCall
        return super.call(context, caller, self, name, arg0);
    }

    @Override
    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg0) {
        final CacheEntry entry = selfType.searchWithCache(methodName);
        final DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, method, arg0);
        }

        // alternate logic to cache the result of respond_to if it's the standard one
        if (method.isBuiltin() && arg0 instanceof RubySymbol) {
            RubySymbol name = (RubySymbol) arg0;
            SendTuple tuple = recacheSend(entry, name, selfType, true, context);

            if (!methodMissing(tuple.entry.method, self)) {
                sendTuple = tuple;

                return tuple.entry.method.call(context, self, tuple.entry.sourceModule, name.idString());
            }
        }

        // normal logic if it's not the builtin respond_to? method
        cache = entry;
        return method.call(context, self, entry.sourceModule, methodName, arg0);
    }

    @Override
    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1) {
        final CacheEntry entry = selfType.searchWithCache(methodName);
        final DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, method, arg0, arg1);
        }

        // alternate logic to cache the result of respond_to if it's the standard one
        if (method.isBuiltin() && arg0 instanceof RubySymbol) {
            RubySymbol name = (RubySymbol) arg0;
            SendTuple tuple = recacheSend(entry, name, selfType, !arg1.isTrue(), context);

            if (!methodMissing(tuple.entry.method, self)) {
                sendTuple = tuple;

                return tuple.entry.method.call(context, self, tuple.entry.sourceModule, name.idString(), arg1);
            }
        }

        // normal logic if it's not the builtin respond_to? method
        cache = entry;
        return method.call(context, self, entry.sourceModule, methodName, arg0, arg1);
    }

    private static SendTuple recacheSend(CacheEntry respondToMethod, RubySymbol symName, RubyClass klass, boolean checkVisibility, ThreadContext context) {
        CacheEntry respondToLookupResult = klass.searchWithCache(symName.idString());

        return new SendTuple(symName, checkVisibility, respondToMethod, respondToLookupResult);
    }
}