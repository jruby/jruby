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
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.parser.ArgumentDescriptor;

/**
 * {@link InternalMethod} objects are copied as properties such as visibility are changed.
 * {@link SharedMethodInfo} stores the state that does not change, such as where the method was defined.
 */
public class SharedMethodInfo {

    private final SourceSection sourceSection;
    private final LexicalScope lexicalScope;
    private final Arity arity;
    private final DynamicObject definitionModule;
    /** The original name of the method. Does not change when aliased. */
    private final String name;
    private final String notes;
    private final ArgumentDescriptor[] argumentDescriptors;
    private final boolean alwaysClone;
    private final boolean alwaysInline;
    private final boolean needsCallerFrame;

    public SharedMethodInfo(
            SourceSection sourceSection,
            LexicalScope lexicalScope,
            Arity arity,
            DynamicObject definitionModule,
            String name,
            String notes,
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
        this.name = name;
        this.notes = notes;
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

    public String getNotes() {
        return notes;
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
                definitionModule,
                newName,
                notes,
                argumentDescriptors,
                alwaysClone,
                alwaysInline,
                needsCallerFrame);
    }

    public String getDescriptiveName() {
        final StringBuilder descriptiveName = new StringBuilder();

        if (definitionModule != null) {
            descriptiveName.append(Layouts.MODULE.getFields(definitionModule).getName());
        }

        if (name != null) {
            descriptiveName.append('#');
            descriptiveName.append(name);
        }

        if (notes != null) {
            final boolean parens = descriptiveName.length() > 0;

            if (parens) {
                descriptiveName.append(" (");
            }

            descriptiveName.append(notes);

            if (parens) {
                descriptiveName.append(')');
            }
        }

        return descriptiveName.toString();
    }

    public String getDescriptiveNameAndSource() {
        if (sourceSection == null || !sourceSection.isAvailable()) {
            return getDescriptiveName();
        } else {
            return getDescriptiveName() + " " + RubyLanguage.fileLine(sourceSection);
        }
    }

}
