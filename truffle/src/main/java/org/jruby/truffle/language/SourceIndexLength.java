/*
 * Copyright (c) 2016, 2017 Oracle and/or its affiliates. All rights reserved. This
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

public class SourceIndexLength {

    private final int charIndex;
    private final int length;

    public SourceIndexLength(int charIndex, int length) {
        this.charIndex = charIndex;
        this.length = length;
    }

    public SourceSection toSourceSection(Source source) {
        return source.createSection(charIndex, length);
    }

    public int getCharIndex() {
        return charIndex;
    }

    public int getLength() {
        return length;
    }

    public int getCharEnd() {
        return charIndex + length;
    }

}
