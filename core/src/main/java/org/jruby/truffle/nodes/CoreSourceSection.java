/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes;

import com.oracle.truffle.api.*;

/**
 * Source sections used for core method nodes.
 */
public final class CoreSourceSection implements NullSourceSection {

    private final String name;

    public CoreSourceSection(String name) {
        this.name = name;
    }

    @Override
    public Source getSource() {
        return new CoreSource(name);
    }

    @Override
    public int getStartLine() {
        return 0;
    }

    @Override
    public int getStartColumn() {
        return 0;
    }

    @Override
    public int getCharIndex() {
        return 0;
    }

    @Override
    public int getCharLength() {
        return 0;
    }

    @Override
    public int getCharEndIndex() {
        return 0;
    }

    @Override
    public String getIdentifier() {
        return null;
    }

    @Override
    public String getCode() {
        return name;
    }

    @Override
    public String getShortDescription() {
        return toString();
    }

    @Override
    public String toString() {
        return name;
    }

}
