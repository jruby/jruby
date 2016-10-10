/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

public class RubySourceSection {

    private final int startLine;
    private final int endLine;

    public RubySourceSection(SourceSection sourceSection) {
        startLine = sourceSection.getStartLine();

        if (sourceSection.getSource() == null) {
            endLine = startLine;
        } else {
            int endLineValue;

            try {
                endLineValue = sourceSection.getEndLine();
            } catch (IllegalArgumentException e) {
                endLineValue = startLine;
            }

            endLine = endLineValue;
        }
    }

    public RubySourceSection(int startLine) {
        this(startLine, startLine);
    }

    public RubySourceSection(int startLine, int endLine) {
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public SourceSection toSourceSection(Source source) {
        if (source == null) {
            throw new UnsupportedOperationException();
        }

        final int index = source.getLineStartOffset(startLine);

        int length = 0;

        for (int n = startLine; n <= endLine; n++) {
            // + 1 because the line length doesn't include any newlines
            length += source.getLineLength(n) + 1;
        }

        length = Math.min(length, source.getLength() - index);
        length = Math.max(0, length);

        return source.createSection(index, length);
    }

}
