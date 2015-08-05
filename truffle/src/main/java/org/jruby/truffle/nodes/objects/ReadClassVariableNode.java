/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;

public class ReadClassVariableNode extends RubyNode {

    private final String name;

    public ReadClassVariableNode(RubyContext context, SourceSection sourceSection, String name) {
        super(context, sourceSection);
        this.name = name;
    }

    public static RubyBasicObject resolveTargetModule(LexicalScope lexicalScope) {
        // MRI logic: ignore lexical scopes (cref) referring to singleton classes
        while (RubyGuards.isRubyClass(lexicalScope.getModule()) && ModuleNodes.getModel((lexicalScope.getModule())).isSingleton()) {
            lexicalScope = lexicalScope.getParent();
        }
        return lexicalScope.getModule();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        final LexicalScope lexicalScope = RubyArguments.getMethod(frame.getArguments()).getLexicalScope();
        final RubyBasicObject module = resolveTargetModule(lexicalScope);

        assert RubyGuards.isRubyModule(module);

        final Object value = ModuleOperations.lookupClassVariable(module, name);

        if (value == null) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().nameErrorUninitializedClassVariable(module, name, this));
        }

        return value;
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        final LexicalScope lexicalScope = RubyArguments.getMethod(frame.getArguments()).getLexicalScope();
        final RubyBasicObject module = resolveTargetModule(lexicalScope);

        final Object value = ModuleOperations.lookupClassVariable(module, name);

        if (value == null) {
            return nil();
        } else {
            return createString("class variable");
        }
    }

}
