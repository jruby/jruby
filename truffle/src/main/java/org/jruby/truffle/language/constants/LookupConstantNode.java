/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.constants;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.module.ModuleOperations;
import org.jruby.truffle.language.CheckLayoutNode;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.language.RubyConstant;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

/**
 * Caches {@link ModuleOperations#lookupConstant}
 * and checks visibility.
 */
@NodeChildren({
        @NodeChild(value = "module", type = RubyNode.class),
        @NodeChild(value = "name", type = RubyNode.class)
})
public abstract class LookupConstantNode extends RubyNode {

    private final boolean ignoreVisibility;
    private final boolean lookInObject;

    @Child CheckLayoutNode checkLayoutNode = new CheckLayoutNode();

    public LookupConstantNode(
            RubyContext context,
            SourceSection sourceSection,
            boolean ignoreVisibility,
            boolean lookInObject) {
        super(context, sourceSection);
        this.ignoreVisibility = ignoreVisibility;
        this.lookInObject = lookInObject;
    }

    public abstract RubyConstant executeLookupConstant(
            VirtualFrame frame,
            Object module,
            String name);

    @Specialization(
            guards = {
                    "module == cachedModule",
                    "isRubyModule(cachedModule)",
                    "guardName(name, cachedName, sameNameProfile)" },
            assumptions = "getUnmodifiedAssumption(cachedModule)",
            limit = "getCacheLimit()")
    protected RubyConstant lookupConstant(
            VirtualFrame frame, DynamicObject module, String name,
            @Cached("module") DynamicObject cachedModule,
            @Cached("name") String cachedName,
            @Cached("doLookup(cachedModule, cachedName)") RubyConstant constant,
            @Cached("isVisible(cachedModule, constant)") boolean isVisible,
            @Cached("createBinaryProfile()") ConditionProfile sameNameProfile) {

        if (!isVisible) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(coreExceptions().nameErrorPrivateConstant(module, name, this));
        }
        return constant;
    }

    public Assumption getUnmodifiedAssumption(DynamicObject module) {
        return Layouts.MODULE.getFields(module).getUnmodifiedAssumption();
    }

    @TruffleBoundary
    @Specialization(guards = "isRubyModule(module)")
    protected RubyConstant lookupConstantUncached(DynamicObject module, String name) {
        RubyConstant constant = doLookup(module, name);
        boolean isVisible = isVisible(module, constant);

        if (!isVisible) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(coreExceptions().nameErrorPrivateConstant(module, name, this));
        }
        return constant;
    }

    @Specialization(guards = "!isRubyModule(module)")
    protected RubyConstant lookupNotModule(Object module, String name) {
        CompilerDirectives.transferToInterpreter();
        throw new RaiseException(coreExceptions().typeErrorIsNotA(
                module.toString(),
                "class/module",
                this));
    }

    protected boolean guardName(String name, String cachedName, ConditionProfile sameNameProfile) {
        // This is likely as for literal constant lookup the name does not change and Symbols
        // always return the same String.
        if (sameNameProfile.profile(name == cachedName)) {
            return true;
        } else {
            return name.equals(cachedName);
        }
    }

    protected boolean isRubyModule(DynamicObject module) {
        return checkLayoutNode.isModule(module);
    }

    protected RubyConstant doLookup(DynamicObject module, String name) {
        if (lookInObject) {
            return ModuleOperations.lookupConstantAndObject(getContext(), module, name);
        } else {
            return ModuleOperations.lookupConstant(getContext(), module, name);
        }
    }

    protected boolean isVisible(DynamicObject module, RubyConstant constant) {
        return ignoreVisibility ||
                constant == null ||
                constant.isVisibleTo(getContext(), LexicalScope.NONE, module);
    }

    protected int getCacheLimit() {
        return getContext().getOptions().CONSTANT_CACHE;
    }

}
