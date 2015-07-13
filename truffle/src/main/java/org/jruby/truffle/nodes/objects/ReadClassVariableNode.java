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
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyModule;

public class ReadClassVariableNode extends RubyNode {

    private final String name;
    private final LexicalScope lexicalScope;

    public ReadClassVariableNode(RubyContext context, SourceSection sourceSection, String name, LexicalScope lexicalScope) {
        super(context, sourceSection);
        this.name = name;
        this.lexicalScope = lexicalScope;
    }

    public static RubyModule resolveTargetModule(LexicalScope lexicalScope) {
        // MRI logic: ignore lexical scopes (cref) referring to singleton classes
        while ((lexicalScope.getLiveModule() instanceof RubyClass) && ((RubyClass) lexicalScope.getLiveModule()).isSingleton()) {
            lexicalScope = lexicalScope.getParent();
        }
        return lexicalScope.getLiveModule();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        final RubyModule module = resolveTargetModule(lexicalScope);

        final Object value = ModuleOperations.lookupClassVariable(module, name);

        if (value == null) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().nameErrorUninitializedClassVariable(module, name, this));
        }

        return value;
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        final RubyModule module = resolveTargetModule(lexicalScope);

        final Object value = ModuleOperations.lookupClassVariable(module, name);

        if (value == null) {
            return nil();
        } else {
            return createString("class variable");
        }
    }

}
