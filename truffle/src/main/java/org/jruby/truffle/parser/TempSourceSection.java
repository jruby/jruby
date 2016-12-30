/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.parser;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

public class TempSourceSection {

    private final int charIndex;
    private final int charLength;

    public TempSourceSection(SourceSection sourceSection) {
        charIndex = sourceSection.getCharIndex();
        charLength = sourceSection.getCharLength();
    }

    public TempSourceSection(int charIndex, int charLength) {
        this.charIndex = charIndex;
        this.charLength = charLength;
    }

    public SourceSection toSourceSection(Source source) {
        return source.createSection(charIndex, charLength);
    }

    public int getCharIndex() {
        return charIndex;
    }

    public int getCharLength() {
        return charLength;
    }

    public int getCharEnd() {
        return charIndex + charLength;
    }

    public TempSourceSection extendedUntil(TempSourceSection endPoint) {
        return new TempSourceSection(charIndex, endPoint.getCharEnd() - charIndex);
    }

}
