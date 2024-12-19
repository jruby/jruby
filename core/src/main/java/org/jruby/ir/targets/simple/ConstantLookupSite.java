package org.jruby.ir.targets.simple;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.opto.ConstantCache;
import org.jruby.runtime.opto.Invalidator;

import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Error.typeError;

public class ConstantLookupSite {
    private final RubySymbol name;
    private final String id;
    private ConstantCache cache;

    public ConstantLookupSite(RubySymbol name) {
        this.name = name;
        this.id = name.idString();
    }

    private IRubyObject cacheSearchConst(ThreadContext context, StaticScope staticScope, boolean publicOnly) {
        // Lexical lookup
        RubyModule object = objectClass(context);
        String id = this.id;
        IRubyObject constant = staticScope == null ?
                object.getConstant(context, id) : staticScope.getScopedConstant(context, id);

        // Inheritance lookup
        RubyModule module = null;
        if (constant == null) {
            // SSS FIXME: Is this null check case correct?
            module = staticScope == null ? object : staticScope.getModule();
            constant = publicOnly ?
                    module.getConstantFromNoConstMissing(id, false) :
                    module.getConstantNoConstMissing(context, id);
        }

        // Call const_missing or cache
        if (constant == null) {
            constant = module.callMethod(context, "const_missing", name);
        } else {
            // recache
            Invalidator invalidator = context.runtime.getConstantInvalidator(id);
            cache = new ConstantCache(constant, invalidator.getData(), invalidator);
        }

        return constant;
    }

    public IRubyObject searchConst(ThreadContext context, StaticScope currScope, boolean publicOnly) {
        ConstantCache cache = this.cache;
        if (!ConstantCache.isCached(cache)) return cacheSearchConst(context, currScope, publicOnly);

        return cache.value;
    }

    private IRubyObject cacheLexicalSearchConst(ThreadContext context, StaticScope staticScope) {
        IRubyObject constant = staticScope.getConstantDefined(context, id);

        if (constant == null) {
            constant = UndefinedValue.UNDEFINED;
        } else {
            // recache
            Invalidator invalidator = context.runtime.getConstantInvalidator(id);
            cache = new ConstantCache(constant, invalidator.getData(), invalidator);
        }

        return constant;
    }

    public IRubyObject lexicalSearchConst(ThreadContext context, StaticScope staticScope) {
        ConstantCache cache = this.cache; // Store to temp so it does null out on us mid-stream
        if (!ConstantCache.isCached(cache)) return cacheLexicalSearchConst(context, staticScope);

        return cache.value;
    }

    private IRubyObject cacheInheritanceSearchConst(ThreadContext context, RubyModule module) {
        String id = this.id;
        IRubyObject constant = module.getConstantNoConstMissingSkipAutoload(context, id);
        if (constant == null) {
            constant = UndefinedValue.UNDEFINED;
        } else {
            // recache
            Invalidator invalidator = context.runtime.getConstantInvalidator(id);
            cache = new ConstantCache(constant, invalidator.getData(), invalidator, module.hashCode());
        }
        return constant;
    }

    public IRubyObject inheritanceSearchConst(ThreadContext context, IRubyObject cmVal) {
        if (!(cmVal instanceof RubyModule module)) throw typeError(context, "", cmVal, " is not a class/module");

        ConstantCache cache = this.cache;

        return !ConstantCache.isCachedFrom(module, cache) ? cacheInheritanceSearchConst(context, module) : cache.value;
    }

    private IRubyObject cacheSearchModuleForConst(ThreadContext context, RubyModule module, boolean publicOnly) {
        String id = this.id;
        IRubyObject constant = publicOnly ?
                module.getConstantFromNoConstMissing(id, false) :
                module.getConstantNoConstMissing(context, id);
        if (constant != null) {
            Invalidator invalidator = context.runtime.getConstantInvalidator(id);
            cache = new ConstantCache(constant, invalidator.getData(), invalidator, module.hashCode());
        }
        return constant;
    }

    public IRubyObject searchModuleForConst(ThreadContext context, IRubyObject cmVal, boolean publicOnly, boolean callConstMissing) {
        if (!(cmVal instanceof RubyModule module)) throw typeError(context, "", cmVal, " is not a class/module");

        ConstantCache cache = this.cache;
        IRubyObject result = !ConstantCache.isCachedFrom(module, cache) ?
                cacheSearchModuleForConst(context, module, publicOnly) : cache.value;
        if (result != null) return result;

        return callConstMissing ?
                module.callMethod(context, "const_missing", name) :
                UndefinedValue.UNDEFINED;
    }
}
