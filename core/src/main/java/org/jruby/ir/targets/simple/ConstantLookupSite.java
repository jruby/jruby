package org.jruby.ir.targets.simple;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.opto.ConstantCache;
import org.jruby.runtime.opto.Invalidator;

public class ConstantLookupSite {
    private final String id;
    private ConstantCache cache;

    public ConstantLookupSite(String id) {
        this.id = id;
    }

    public Object cacheSearchConst(ThreadContext context, StaticScope staticScope, boolean publicOnly) {
        // Lexical lookup
        Ruby runtime = context.getRuntime();
        RubyModule object = runtime.getObject();
        Object constant = (staticScope == null) ? object.getConstant(id) : staticScope.getConstantInner(id);

        // Inheritance lookup
        RubyModule module = null;
        if (constant == null) {
            // SSS FIXME: Is this null check case correct?
            module = staticScope == null ? object : staticScope.getModule();
            constant = publicOnly ? module.getConstantFromNoConstMissing(id, false) : module.getConstantNoConstMissing(id);
        }

        // Call const_missing or cache
        if (constant == null) {
            constant = module.callMethod(context, "const_missing", runtime.fastNewSymbol(id));
        } else {
            // recache
            Invalidator invalidator = runtime.getConstantInvalidator(id);
            cache = new ConstantCache((IRubyObject)constant, invalidator.getData(), invalidator);
        }

        return constant;
    }

    public Object searchConst(ThreadContext context, StaticScope currScope) {
        ConstantCache cache = this.cache;
        if (!ConstantCache.isCached(cache)) return cacheSearchConst(context, currScope, false);

        return cache.value;
    }

    public Object searchConstPublic(ThreadContext context, StaticScope currScope) {
        ConstantCache cache = this.cache;
        if (!ConstantCache.isCached(cache)) return cacheSearchConst(context, currScope, true);

        return cache.value;
    }
}
