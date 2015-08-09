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
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

public abstract class LookupConstantWithLexicalScopeNode extends RubyNode {

    private final LexicalScope lexicalScope;
    private final String name;

    public LookupConstantWithLexicalScopeNode(RubyContext context, SourceSection sourceSection, LexicalScope lexicalScope, String name) {
        super(context, sourceSection);
        this.lexicalScope = lexicalScope;
        this.name = name;
    }

    public RubyBasicObject getModule() {
        return lexicalScope.getLiveModule();
    }

    public abstract RubyConstant executeLookupConstant(VirtualFrame frame);

    @Specialization(assumptions = "getUnmodifiedAssumption(getModule())")
    protected RubyConstant lookupConstant(VirtualFrame frame,
            @Cached("doLookup()") RubyConstant constant,
            @Cached("isVisible(constant)") boolean isVisible) {
        if (!isVisible) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().nameErrorPrivateConstant(getModule(), name, this));
        }
        return constant;
    }

    public Assumption getUnmodifiedAssumption(RubyBasicObject module) {
        return ModuleNodes.getModel(module).getUnmodifiedAssumption();
    }

    protected RubyConstant doLookup() {
        return ModuleOperations.lookupConstantWithLexicalScope(getContext(), lexicalScope, name);
    }

    protected boolean isVisible(RubyConstant constant) {
        return constant == null || constant.isVisibleTo(getContext(), lexicalScope, getModule());
    }

}
