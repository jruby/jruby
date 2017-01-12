/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.constants;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.core.string.StringUtils;
import org.jruby.truffle.language.RubyConstant;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.loader.RequireNode;
import org.jruby.truffle.parser.Identifiers;

@NodeChildren({ @NodeChild("module"), @NodeChild("name"), @NodeChild("constant"), @NodeChild("lookupConstantNode") })
public abstract class GetConstantNode extends RubyNode {

    public static GetConstantNode create() {
        return GetConstantNodeGen.create(null, null, null, null);
    }

    @Child private CallDispatchHeadNode constMissingNode;

    public abstract Object executeGetConstant(
            VirtualFrame frame, Object module, String name, RubyConstant constant, LookupConstantInterface lookupConstantNode);

    @Specialization(guards = { "constant != null", "!constant.isAutoload()" })
    protected Object getConstant(DynamicObject module, String name, RubyConstant constant, LookupConstantInterface lookupConstantNode) {
        return constant.getValue();
    }

    @Specialization(guards = { "constant != null", "constant.isAutoload()" })
    protected Object autoloadConstant(VirtualFrame frame, DynamicObject module, String name, RubyConstant constant, LookupConstantInterface lookupConstantNode,
            @Cached("create()") RequireNode requireNode) {

        final DynamicObject path = (DynamicObject) constant.getValue();

        // The autoload constant must only be removed if everything succeeds.
        // We remove it first to allow lookup to ignore it and add it back if there was a failure.
        Layouts.MODULE.getFields(constant.getDeclaringModule()).removeConstant(getContext(), this, name);
        try {
            requireNode.executeRequire(frame, StringOperations.getString(path));
            final RubyConstant resolvedConstant = lookupConstantNode.lookupConstant(frame, module, name);
            return executeGetConstant(frame, module, name, resolvedConstant, lookupConstantNode);
        } catch (RaiseException e) {
            Layouts.MODULE.getFields(constant.getDeclaringModule()).setAutoloadConstant(getContext(), this, name, path);
            throw e;
        }
    }

    @Specialization(
            guards = { "constant == null", "guardName(name, cachedName, sameNameProfile)" },
            limit = "getCacheLimit()")
    protected Object missingConstantCached(
            VirtualFrame frame,
            DynamicObject module,
            String name,
            Object constant,
            LookupConstantInterface lookupConstantNode,
            @Cached("name") String cachedName,
            @Cached("isValidConstantName(name)") boolean isValidConstantName,
            @Cached("getSymbol(name)") DynamicObject symbolName,
            @Cached("createBinaryProfile()") ConditionProfile sameNameProfile) {
        return doMissingConstant(frame, module, name, isValidConstantName, symbolName);
    }

    @Specialization(guards = "constant == null")
    protected Object missingConstantUncached(VirtualFrame frame, DynamicObject module, String name, Object constant, LookupConstantInterface lookupConstantNode,
            @Cached("createBinaryProfile()") ConditionProfile validNameProfile) {
        final boolean isValidConstantName = validNameProfile.profile(isValidConstantName(name));
        return doMissingConstant(frame, module, name, isValidConstantName, getSymbol(name));
    }

    private Object doMissingConstant(
            VirtualFrame frame,
            DynamicObject module,
            String name,
            boolean isValidConstantName,
            DynamicObject symbolName) {
        if (!isValidConstantName) {
            throw new RaiseException(coreExceptions().nameError(formatError(name), module, name, this));
        }

        if (constMissingNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            constMissingNode = insert(DispatchHeadNodeFactory.createMethodCall());
        }

        return constMissingNode.call(frame, module, "const_missing", symbolName);
    }

    @TruffleBoundary
    private String formatError(String name) {
        return StringUtils.format("wrong constant name %s", name);
    }

    protected boolean isValidConstantName(String name) {
        return Identifiers.isValidConstantName19(name);
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

    protected int getCacheLimit() {
        return getContext().getOptions().CONSTANT_CACHE;
    }

}
