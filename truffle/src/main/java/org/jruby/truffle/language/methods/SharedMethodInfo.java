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

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.parser.ArgumentDescriptor;

/**
 * {@link InternalMethod} objects are copied as properties such as visibility are changed.
 * {@link NamedSharedMethodInfo} stores the state that does not change, such as where the method was defined.
 */
public class SharedMethodInfo {

    private final SourceSection sourceSection;
    private final LexicalScope lexicalScope;
    private final Arity arity;
    private final DynamicObject definitionModule;
    private final ArgumentDescriptor[] argumentDescriptors;
    private final boolean alwaysClone;
    private final boolean alwaysInline;
    private final boolean needsCallerFrame;

    public SharedMethodInfo(
            SourceSection sourceSection,
            LexicalScope lexicalScope,
            Arity arity,
            DynamicObject definitionModule,
            ArgumentDescriptor[] argumentDescriptors,
            boolean alwaysClone,
            boolean alwaysInline,
            boolean needsCallerFrame) {
        if (argumentDescriptors == null) {
            argumentDescriptors = new ArgumentDescriptor[]{};
        }

        assert lexicalScope != null;
        this.sourceSection = sourceSection;
        this.lexicalScope = lexicalScope;
        this.arity = arity;
        this.definitionModule = definitionModule;
        this.argumentDescriptors = argumentDescriptors;
        this.alwaysClone = alwaysClone;
        this.alwaysInline = alwaysInline;
        this.needsCallerFrame = needsCallerFrame;
    }

    public SourceSection getSourceSection() {
        return sourceSection;
    }

    public LexicalScope getLexicalScope() {
        return lexicalScope;
    }

    public Arity getArity() {
        return arity;
    }

    public ArgumentDescriptor[] getArgumentDescriptors() {
        return argumentDescriptors;
    }

    public boolean shouldAlwaysClone() {
        return alwaysClone;
    }

    public boolean shouldAlwaysInline() {
        return alwaysInline;
    }

    public boolean needsCallerFrame() {
        return needsCallerFrame;
    }

    public DynamicObject getDefinitionModule() {
        return definitionModule;
    }
}
