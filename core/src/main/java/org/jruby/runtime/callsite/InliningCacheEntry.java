package org.jruby.runtime.callsite;

import org.jruby.RubyClass;
import org.jruby.compiler.Compilable;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.UndefinedIRMethod;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;

/**
 * Created by enebo on 11/4/15.
 */
public class InliningCacheEntry {
    public static final InliningCacheEntry NULL_CACHE = new InliningCacheEntry(UndefinedIRMethod.INSTANCE, 0);
    private static final int INLINE_THRESHOLD = 10;

    public final DynamicMethod method;
    public final int token;
    public int cacheCount;

    public InliningCacheEntry(DynamicMethod method, int token) {
        this.method = method;
        this.token = token;
        cacheCount = 0;
    }

    public boolean typeOk(RubyClass incomingType) {
        boolean matches = token == incomingType.getGeneration();

        if (matches) {
            cacheCount++;
        } else {
            cacheCount = 0;
        }

        return matches;
    }

    public boolean maybeInline(IRScope callingScope) {
        return cacheCount >= INLINE_THRESHOLD && callingScope.isFullBuildComplete();
    }
}
