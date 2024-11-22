package org.jruby.runtime.callsite;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.asFloat;

public class SuperCallSite extends CallSite {
    protected volatile SuperTuple cache = SuperTuple.NULL_CACHE;

    private record SuperTuple(String name, CacheEntry cache) {
        static final SuperTuple NULL_CACHE = new SuperTuple("", CacheEntry.NULL_CACHE);

        public boolean cacheOk(String name, RubyClass klass) {
                return this.name.equals(name) && cache.typeOk(klass);
            }
    }
    
    public SuperCallSite() {
        super("super", CallType.SUPER);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, long fixnum) {
        return call(context, caller, self, asFixnum(context, fixnum));
    }
    
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, double flote) {
        return call(context, caller, self, asFloat(context, flote));
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject... args) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();

        return call(context, caller, self, klazz, name, args);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, RubyModule klazz, String name, IRubyObject... args) {
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        SuperTuple myCache = cache;
        if (selfType != null && myCache.cacheOk(name, selfType)) {
            return myCache.cache.method.call(context, self, cache.cache.sourceModule, name, args);
        }
        return cacheAndCall(caller, selfType, args, context, self, name);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();

        return call(context, caller, self, klazz, name, args, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, RubyModule klazz, String name, IRubyObject[] args, Block block) {
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        SuperTuple myCache = cache;
        if (selfType != null && myCache.cacheOk(name, selfType)) {
            return myCache.cache.method.call(context, self, cache.cache.sourceModule, name, args, block);
        }
        return cacheAndCall(caller, selfType, block, args, context, self, name);
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        try {
            return call(context, caller, self, args, block);
        } finally {
            block.escape();
        }
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, RubyModule klazz, String name, IRubyObject[] args, Block block) {
        try {
            return call(context, caller, self, klazz, name, args, block);
        } finally {
            block.escape();
        }
    }

    public IRubyObject callVarargs(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject... args) {
        return switch (args.length) {
            case 0 -> call(context, caller, self);
            case 1 -> call(context, caller, self, args[0]);
            case 2 -> call(context, caller, self, args[0], args[1]);
            case 3 -> call(context, caller, self, args[0], args[1], args[2]);
            default -> call(context, caller, self, args);
        };
    }

    public IRubyObject callVarargs(ThreadContext context, IRubyObject caller, IRubyObject self, RubyModule klazz, String name, IRubyObject... args) {
        return switch (args.length) {
            case 0 -> call(context, caller, self, klazz, name);
            case 1 -> call(context, caller, self, klazz, name, args[0]);
            case 2 -> call(context, caller, self, klazz, name, args[0], args[1]);
            case 3 -> call(context, caller, self, klazz, name, args[0], args[1], args[2]);
            default -> call(context, caller, self, klazz, name, args);
        };
    }

    public IRubyObject callVarargs(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        return switch (args.length) {
            case 0 -> call(context, caller, self, block);
            case 1 -> call(context, caller, self, args[0], block);
            case 2 -> call(context, caller, self, args[0], args[1], block);
            case 3 -> call(context, caller, self, args[0], args[1], args[2], block);
            default -> call(context, caller, self, args, block);
        };
    }

    public IRubyObject callVarargs(ThreadContext context, IRubyObject caller, IRubyObject self, RubyModule klazz, String name, IRubyObject[] args, Block block) {
        return switch (args.length) {
            case 0 -> call(context, caller, self, klazz, name, block);
            case 1 -> call(context, caller, self, klazz, name, args[0], block);
            case 2 -> call(context, caller, self, klazz, name, args[0], args[1], block);
            case 3 -> call(context, caller, self, klazz, name, args[0], args[1], args[2], block);
            default -> call(context, caller, self, klazz, name, args, block);
        };
    }

    public IRubyObject callVarargsIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        return switch (args.length) {
            case 0 -> callIter(context, caller, self, block);
            case 1 -> callIter(context, caller, self, args[0], block);
            case 2 -> callIter(context, caller, self, args[0], args[1], block);
            case 3 -> callIter(context, caller, self, args[0], args[1], args[2], block);
            default -> callIter(context, caller, self, args, block);
        };
    }

    public IRubyObject callVarargsIter(ThreadContext context, IRubyObject caller, IRubyObject self, RubyModule klazz, String name, IRubyObject[] args, Block block) {
        return switch (args.length) {
            case 0 -> callIter(context, caller, self, klazz, name, block);
            case 1 -> callIter(context, caller, self, klazz, name, args[0], block);
            case 2 -> callIter(context, caller, self, klazz, name, args[0], args[1], block);
            case 3 -> callIter(context, caller, self, klazz, name, args[0], args[1], args[2], block);
            default -> callIter(context, caller, self, klazz, name, args, block);
        };
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();

        return call(context, caller, self, klazz, name);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, RubyModule klazz, String name) {
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        SuperTuple myCache = cache;
        if (selfType != null && myCache.cacheOk(name, selfType)) {
            return myCache.cache.method.call(context, self, cache.cache.sourceModule, name);
        }
        return cacheAndCall(caller, selfType, context, self, name);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();

        return call(context, caller, self, klazz, name, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, RubyModule klazz, String name, Block block) {
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        SuperTuple myCache = cache;
        if (selfType != null && myCache.cacheOk(name, selfType)) {
            return myCache.cache.method.call(context, self, cache.cache.sourceModule, name, block);
        }
        return cacheAndCall(caller, selfType, block, context, self, name);
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        try {
            return call(context, caller, self, block);
        } finally {
            block.escape();
        }
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, RubyModule klazz, String name, Block block) {
        try {
            return call(context, caller, self, klazz, name, block);
        } finally {
            block.escape();
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();

        return call(context, caller, self, klazz, name, arg1);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1) {
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        SuperTuple myCache = cache;
        if (selfType != null && myCache.cacheOk(name, selfType)) {
            return myCache.cache.method.call(context, self, cache.cache.sourceModule, name, arg1);
        }
        return cacheAndCall(caller, selfType, context, self, name, arg1);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, Block block) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();

        return call(context, caller, self, klazz, name, arg1, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, Block block) {
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        SuperTuple myCache = cache;
        if (selfType != null && myCache.cacheOk(name, selfType)) {
            return myCache.cache.method.call(context, self, cache.cache.sourceModule, name, arg1, block);
        }
        return cacheAndCall(caller, selfType, block, context, self, name, arg1);
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, Block block) {
        try {
            return call(context, caller, self, arg1, block);
        } finally {
            block.escape();
        }
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, Block block) {
        try {
            return call(context, caller, self, klazz, name, arg1, block);
        } finally {
            block.escape();
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();

        return call(context, caller, self, klazz, name, arg1, arg2);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, IRubyObject arg2) {
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        SuperTuple myCache = cache;
        if (selfType != null && myCache.cacheOk(name, selfType)) {
            return myCache.cache.method.call(context, self, cache.cache.sourceModule, name, arg1, arg2);
        }
        return cacheAndCall(caller, selfType, context, self, name, arg1, arg2);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();

        return call(context, caller, self, klazz, name, arg1, arg2, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, IRubyObject arg2, Block block) {
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        SuperTuple myCache = cache;
        if (selfType != null && myCache.cacheOk(name, selfType)) {
            return myCache.cache.method.call(context, self, cache.cache.sourceModule, name, arg1, arg2, block);
        }
        return cacheAndCall(caller, selfType, block, context, self, name, arg1, arg2);
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
        try {
            return call(context, caller, self, arg1, arg2, block);
        } finally {
            block.escape();
        }
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, IRubyObject arg2, Block block) {
        try {
            return call(context, caller, self, klazz, name, arg1, arg2, block);
        } finally {
            block.escape();
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();

        return call(context, caller, self, klazz, name, arg1, arg2, arg3);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        SuperTuple myCache = cache;
        if (selfType != null && myCache.cacheOk(name, selfType)) {
            return myCache.cache.method.call(context, self, cache.cache.sourceModule, name, arg1, arg2, arg3);
        }
        return cacheAndCall(caller, selfType, context, self, name, arg1, arg2, arg3);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();

        return call(context, caller, self, klazz, name, arg1, arg2, arg3, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        SuperTuple myCache = cache;
        if (selfType != null && myCache.cacheOk(name, selfType)) {
            return myCache.cache.method.call(context, self, cache.cache.sourceModule, name, arg1, arg2, arg3, block);
        }
        return cacheAndCall(caller, selfType, block, context, self, name, arg1, arg2, arg3);
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        try {
            return call(context, caller, self, arg1, arg2, arg3, block);
        } finally {
            block.escape();
        }
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        try {
            return call(context, caller, self, klazz, name, arg1, arg2, arg3, block);
        } finally {
            block.escape();
        }
    }
    
    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, IRubyObject[] args, ThreadContext context, IRubyObject self, String name) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, name, method, args, block);
        }
        cache = new SuperTuple(name, entry);
        return method.call(context, self, entry.sourceModule, name, args, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, IRubyObject[] args, ThreadContext context, IRubyObject self, String name) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, name, method, args);
        }
        cache = new SuperTuple(name, entry);
        return method.call(context, self, entry.sourceModule, name, args);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, String name) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, name, method);
        }
        cache = new SuperTuple(name, entry);
        return method.call(context, self, entry.sourceModule, name);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, String name) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, name, method, block);
        }
        cache = new SuperTuple(name, entry);
        return method.call(context, self, entry.sourceModule, name, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, String name, IRubyObject arg) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, name, method, arg);
        }
        cache = new SuperTuple(name, entry);
        return method.call(context, self, entry.sourceModule, name, arg);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, String name, IRubyObject arg) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, name, method, arg, block);
        }
        cache = new SuperTuple(name, entry);
        return method.call(context, self, entry.sourceModule, name, arg, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, name, method, arg1, arg2);
        }
        cache = new SuperTuple(name, entry);
        return method.call(context, self, entry.sourceModule, name, arg1, arg2);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, name, method, arg1, arg2, block);
        }
        cache = new SuperTuple(name, entry);
        return method.call(context, self, entry.sourceModule, name, arg1, arg2, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, name, method, arg1, arg2, arg3);
        }
        cache = new SuperTuple(name, entry);
        return method.call(context, self, entry.sourceModule, name, arg1, arg2, arg3);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, name, method, arg1, arg2, arg3, block);
        }
        cache = new SuperTuple(name, entry);
        return method.call(context, self, entry.sourceModule, name, arg1, arg2, arg3, block);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass selfType, String name, DynamicMethod method, IRubyObject[] args) {
        return Helpers.callMethodMissing(context, self, selfType, method.getVisibility(), name, callType, args, Block.NULL_BLOCK);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass selfType, String name, DynamicMethod method) {
        return Helpers.callMethodMissing(context, self, selfType, method.getVisibility(), name, callType, Block.NULL_BLOCK);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass selfType, String name, DynamicMethod method, Block block) {
        return Helpers.callMethodMissing(context, self, selfType, method.getVisibility(), name, callType, block);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass selfType, String name, DynamicMethod method, IRubyObject arg) {
        return Helpers.callMethodMissing(context, self, selfType, method.getVisibility(), name, callType, arg, Block.NULL_BLOCK);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass selfType, String name, DynamicMethod method, IRubyObject[] args, Block block) {
        return Helpers.callMethodMissing(context, self, selfType, method.getVisibility(), name, callType, args, block);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass selfType, String name, DynamicMethod method, IRubyObject arg0, Block block) {
        return Helpers.callMethodMissing(context, self, selfType, method.getVisibility(), name, callType, arg0, block);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass selfType, String name, DynamicMethod method, IRubyObject arg0, IRubyObject arg1) {
        return Helpers.callMethodMissing(context, self, selfType, method.getVisibility(), name, callType, arg0, arg1, Block.NULL_BLOCK);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass selfType, String name, DynamicMethod method, IRubyObject arg0, IRubyObject arg1, Block block) {
        return Helpers.callMethodMissing(context, self, selfType, method.getVisibility(), name, callType, arg0, arg1, block);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass selfType, String name, DynamicMethod method, IRubyObject arg0, IRubyObject arg1, IRubyObject arg3) {
        return Helpers.callMethodMissing(context, self, selfType, method.getVisibility(), name, callType, arg0, arg1, arg3, Block.NULL_BLOCK);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass selfType, String name, DynamicMethod method, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return Helpers.callMethodMissing(context, self, selfType, method.getVisibility(), name, callType, arg0, arg1, arg2, block);
    }

    protected boolean methodMissing(DynamicMethod method, IRubyObject caller) {
        return method.isUndefined();
    }

    protected static RubyClass pollAndGetClass(ThreadContext context, IRubyObject self, RubyModule frameClass, String frameName) {
        Helpers.checkSuperDisabledOrOutOfMethod(context, frameClass, frameName);

        return frameClass.getSuperClass();
    }

}
