/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.methods;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.language.RubyBaseNode;
import org.jruby.truffle.language.Visibility;
import org.jruby.truffle.language.arguments.RubyArguments;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

/**
 * Define a method from a module body (module/class/class << self ... end).
 */
public class ModuleBodyDefinitionNode extends RubyBaseNode {

    private final String name;
    private final SharedMethodInfo sharedMethodInfo;
    private final CallTarget callTarget;
    private final boolean captureBlock;
    private final boolean dynamicLexicalScope;
    private final Map<DynamicObject, LexicalScope> lexicalScopes;

    public ModuleBodyDefinitionNode(RubyContext context, SourceSection sourceSection, String name, SharedMethodInfo sharedMethodInfo,
                    CallTarget callTarget, boolean captureBlock, boolean dynamicLexicalScope) {
        super(context, sourceSection);
        this.name = name;
        this.sharedMethodInfo = sharedMethodInfo;
        this.callTarget = callTarget;
        this.captureBlock = captureBlock;
        this.dynamicLexicalScope = dynamicLexicalScope;
        this.lexicalScopes = dynamicLexicalScope ? new ConcurrentHashMap<>() : null;
    }

    public InternalMethod createMethod(VirtualFrame frame, LexicalScope staticLexicalScope, DynamicObject module) {
        final DynamicObject capturedBlock;

        if (captureBlock) {
            capturedBlock = RubyArguments.getBlock(frame);
        } else {
            capturedBlock = null;
        }

        final LexicalScope parentLexicalScope = RubyArguments.getMethod(frame).getLexicalScope();
        final LexicalScope lexicalScope = prepareLexicalScope(staticLexicalScope, parentLexicalScope, module);
        return new InternalMethod(getContext(), sharedMethodInfo, lexicalScope, name, module, Visibility.PUBLIC, false, null, callTarget, capturedBlock, null);
    }

    @TruffleBoundary
    private LexicalScope prepareLexicalScope(LexicalScope staticLexicalScope, LexicalScope parentLexicalScope, DynamicObject module) {
        if (!dynamicLexicalScope) {
            staticLexicalScope.unsafeSetLiveModule(module);
            Layouts.MODULE.getFields(staticLexicalScope.getParent().getLiveModule()).addLexicalDependent(module);
            return staticLexicalScope;
        } else {
            // Cache the scope per module in case the module body is run multiple times.
            // This allows dynamic constant lookup to cache better.
            return lexicalScopes.computeIfAbsent(module, m -> new LexicalScope(parentLexicalScope, module));
        }
    }

}
