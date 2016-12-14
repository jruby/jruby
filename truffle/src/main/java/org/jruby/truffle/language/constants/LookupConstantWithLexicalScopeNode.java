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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.module.ModuleOperations;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.language.RubyConstant;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.WarnNode;
import org.jruby.truffle.language.control.RaiseException;

public abstract class LookupConstantWithLexicalScopeNode extends RubyNode implements LookupConstantInterface {

    private final LexicalScope lexicalScope;
    private final String name;
    @Child private WarnNode warnNode;

    public LookupConstantWithLexicalScopeNode(LexicalScope lexicalScope, String name) {
        this.lexicalScope = lexicalScope;
        this.name = name;
    }

    public DynamicObject getModule() {
        return lexicalScope.getLiveModule();
    }

    public abstract RubyConstant executeLookupConstant(VirtualFrame frame);

    public RubyConstant lookupConstant(VirtualFrame frame, Object module, String name) {
        assert name == this.name;
        return executeLookupConstant(frame);
    }

    @Specialization(assumptions = "getUnmodifiedAssumption(getModule())")
    protected RubyConstant lookupConstant(VirtualFrame frame,
            @Cached("doLookup()") RubyConstant constant,
            @Cached("isVisible(constant)") boolean isVisible) {
        if (!isVisible) {
            throw new RaiseException(coreExceptions().nameErrorPrivateConstant(getModule(), name, this));
        }
        if (constant != null && constant.isDeprecated()) {
            warnDeprecatedConstant(frame, name);
        }
        return constant;
    }

    @Specialization
    protected RubyConstant lookupConstantUncached(VirtualFrame frame,
            @Cached("createBinaryProfile()") ConditionProfile isVisibleProfile,
            @Cached("createBinaryProfile()") ConditionProfile isDeprecatedProfile) {
        RubyConstant constant = doLookup();
        if (isVisibleProfile.profile(!isVisible(constant))) {
            throw new RaiseException(coreExceptions().nameErrorPrivateConstant(getModule(), name, this));
        }
        if (isDeprecatedProfile.profile(constant != null && constant.isDeprecated())) {
            warnDeprecatedConstant(frame, name);
        }
        return constant;
    }

    public Assumption getUnmodifiedAssumption(DynamicObject module) {
        return Layouts.MODULE.getFields(module).getUnmodifiedAssumption();
    }

    @TruffleBoundary
    protected RubyConstant doLookup() {
        return ModuleOperations.lookupConstantWithLexicalScope(getContext(), lexicalScope, name);
    }

    @TruffleBoundary
    protected boolean isVisible(RubyConstant constant) {
        return constant == null || constant.isVisibleTo(getContext(), lexicalScope, getModule());
    }

    private void warnDeprecatedConstant(VirtualFrame frame, String name) {
        if (warnNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            warnNode = insert(new WarnNode());
        }
        warnNode.execute(frame, "constant ", name, " is deprecated");
    }

}
