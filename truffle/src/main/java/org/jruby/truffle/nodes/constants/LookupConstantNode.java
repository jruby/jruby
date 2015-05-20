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

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.DispatchNode;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyModule;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

@NodeChildren({ @NodeChild("module"), @NodeChild("name") })
public abstract class LookupConstantNode extends RubyNode {

    public static int getCacheLimit() {
        return DispatchNode.DISPATCH_POLYMORPHIC_MAX;
    }

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
            "name.equals(cachedName)"
    }, assumptions = "cachedModule.getUnmodifiedAssumption()", limit = "getCacheLimit()")
    protected RubyConstant lookupConstant(VirtualFrame frame, RubyModule module, String name,
            @Cached("module") RubyModule cachedModule,
            @Cached("name") String cachedName,
            @Cached("doLookup(cachedModule, cachedName)") RubyConstant constant,
            @Cached("isVisible(cachedModule, constant)") boolean isVisible) {
        if (!isVisible) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().nameErrorPrivateConstant(module, name, this));
        }
        return constant;
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

    protected RubyConstant doLookup(RubyModule module, String name) {
        return ModuleOperations.lookupConstant(getContext(), lexicalScope, module, name);
    }

    protected boolean isVisible(RubyModule module, RubyConstant constant) {
        return constant == null || constant.isVisibleTo(getContext(), lexicalScope, module);
    }

}
