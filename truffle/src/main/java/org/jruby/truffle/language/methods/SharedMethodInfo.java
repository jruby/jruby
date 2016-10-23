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

import com.oracle.truffle.api.source.SourceSection;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.truffle.language.LexicalScope;

/**
 * {@link InternalMethod} objects are copied as properties such as visibility are changed.
 * {@link SharedMethodInfo} stores the state that does not change, such as where the method was defined.
 */
public class SharedMethodInfo {

    private final SourceSection sourceSection;
    private final LexicalScope lexicalScope;
    private final Arity arity;
    /** The original name of the method. Does not change when aliased. */
    private final String name;
    private final boolean isBlock;
    private final ArgumentDescriptor[] argumentDescriptors;
    private final boolean alwaysClone;
    private final boolean alwaysInline;
    private final boolean needsCallerFrame;

    public SharedMethodInfo(
            SourceSection sourceSection,
            LexicalScope lexicalScope,
            Arity arity,
            String name,
            boolean isBlock,
            ArgumentDescriptor[] argumentDescriptors,
            boolean alwaysClone,
            boolean alwaysInline,
            boolean needsCallerFrame) {
        if (argumentDescriptors == null) {
            argumentDescriptors = new ArgumentDescriptor[]{};
        }

        this.sourceSection = sourceSection;
        this.lexicalScope = lexicalScope;
        this.arity = arity;
        this.name = name;
        this.isBlock = isBlock;
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

    public String getName() {
        return name;
    }

    public boolean isBlock() {
        return isBlock;
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

    public SharedMethodInfo withName(String newName) {
        return new SharedMethodInfo(
                sourceSection,
                lexicalScope,
                arity,
                newName,
                isBlock,
                argumentDescriptors,
                alwaysClone,
                alwaysInline,
                needsCallerFrame);
    }

    @Override
    public String toString() {
        final String prefix;
        if (isBlock) {
            prefix = "block in ";
        } else {
            prefix = "";
        }

        final String suffix;
        if (sourceSection == null) {
            suffix = name;
        } else if (!sourceSection.isAvailable()) {
            suffix = String.format("%s %s", name, sourceSection.getSource().getName());
        } else {
            suffix = String.format("%s %s:%d", name, sourceSection.getSource().getName(), sourceSection.getStartLine());
        }

        return prefix + suffix;
    }

}
