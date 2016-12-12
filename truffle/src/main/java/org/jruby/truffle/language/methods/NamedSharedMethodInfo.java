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
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.parser.ArgumentDescriptor;

public class NamedSharedMethodInfo {

    private final SharedMethodInfo sharedMethodInfo;
    /** The original name of the method. Does not change when aliased. */
    private final String name;
    private final String notes;

    public NamedSharedMethodInfo(
            SharedMethodInfo sharedMethodInfo,
            String name,
            String notes) {
        this.sharedMethodInfo = sharedMethodInfo;
        this.name = name;
        this.notes = notes;
    }

    public SourceSection getSourceSection() {
        return sharedMethodInfo.getSourceSection();
    }

    public LexicalScope getLexicalScope() {
        return sharedMethodInfo.getLexicalScope();
    }

    public Arity getArity() {
        return sharedMethodInfo.getArity();
    }

    public String getName() {
        return name;
    }

    public String getNotes() {
        return notes;
    }

    public ArgumentDescriptor[] getArgumentDescriptors() {
        return sharedMethodInfo.getArgumentDescriptors();
    }

    public boolean shouldAlwaysClone() {
        return sharedMethodInfo.shouldAlwaysClone();
    }

    public boolean shouldAlwaysInline() {
        return sharedMethodInfo.shouldAlwaysInline();
    }

    public boolean needsCallerFrame() {
        return sharedMethodInfo.needsCallerFrame();
    }

    public NamedSharedMethodInfo withName(String newName) {
        return new NamedSharedMethodInfo(
                sharedMethodInfo,
                newName,
                notes);
    }

    public String getDescriptiveName() {
        final StringBuilder descriptiveName = new StringBuilder();

        if (sharedMethodInfo.getDefinitionModule() != null) {
            descriptiveName.append(Layouts.MODULE.getFields(sharedMethodInfo.getDefinitionModule()).getName());
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
        if (sharedMethodInfo.getSourceSection() == null || !sharedMethodInfo.getSourceSection().isAvailable()) {
            return getDescriptiveName();
        } else {
            return getDescriptiveName() + " " + RubyLanguage.fileLine(sharedMethodInfo.getSourceSection());
        }
    }

    public SharedMethodInfo getSharedMethodInfo() {
        return sharedMethodInfo;
    }
}
