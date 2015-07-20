/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.constants;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyModule;

@NodeChildren({ @NodeChild("module"), @NodeChild("name") })
public abstract class LookupConstantNode extends RubyNode {

    private final LexicalScope lexicalScope;

    public LookupConstantNode(RubyContext context, SourceSection sourceSection, LexicalScope lexicalScope) {
        super(context, sourceSection);
        this.lexicalScope = lexicalScope;
    }

    public LexicalScope getLexicalScope() {
        return lexicalScope;
    }

    public abstract RubyConstant executeLookupConstant(VirtualFrame frame, Object module, String name);

    @Specialization(guards = {
            "module == cachedModule",
            "guardName(name, cachedName, sameNameProfile)"
    }, assumptions = "getUnmodifiedAssumption(cachedModule)", limit = "getCacheLimit()")
    protected RubyConstant lookupConstant(VirtualFrame frame, RubyModule module, String name,
            @Cached("module") RubyModule cachedModule,
            @Cached("name") String cachedName,
            @Cached("doLookup(cachedModule, cachedName)") RubyConstant constant,
            @Cached("isVisible(cachedModule, constant)") boolean isVisible,
            @Cached("createBinaryProfile()") ConditionProfile sameNameProfile) {
        if (!isVisible) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().nameErrorPrivateConstant(module, name, this));
        }
        return constant;
    }

    public Assumption getUnmodifiedAssumption(RubyModule module) {
        return ModuleNodes.getModel(module).getUnmodifiedAssumption();
    }

    @TruffleBoundary
    @Specialization
    protected RubyConstant lookupConstantUncached(RubyModule module, String name) {
        RubyConstant constant = doLookup(module, name);
        boolean isVisible = isVisible(module, constant);

        if (!isVisible) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().nameErrorPrivateConstant(module, name, this));
        }
        return constant;
    }

    @Specialization(guards = "!isRubyModule(module)")
    protected RubyConstant lookupNotModule(Object module, String name) {
        CompilerDirectives.transferToInterpreter();
        throw new RaiseException(getContext().getCoreLibrary().typeErrorIsNotA(module.toString(), "class/module", this));
    }

    protected boolean guardName(String name, String cachedName, ConditionProfile sameNameProfile) {
        // This is likely as for literal constant lookup the name does not change and Symbols always return the same String.
        if (sameNameProfile.profile(name == cachedName)) {
            return true;
        } else {
            return name.equals(cachedName);
        }
    }

    protected RubyConstant doLookup(RubyModule module, String name) {
        return ModuleOperations.lookupConstant(getContext(), lexicalScope, module, name);
    }

    protected boolean isVisible(RubyModule module, RubyConstant constant) {
        return constant == null || constant.isVisibleTo(getContext(), lexicalScope, module);
    }

}
