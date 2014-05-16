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

import java.io.*;

import com.oracle.truffle.api.*;

/**
 * Singleton source used for core method nodes.
 */
public final class CoreSource implements Source {

    private final String name;

    public CoreSource(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getShortName() {
        return name;
    }

    @Override
    public String getCode() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String getPath() {
        return null;
    }

    @Override
    public Reader getReader() {
        return null;
    }

    @Override
    public InputStream getInputStream() {
        return null;
    }

    @Override
    public String getCode(int lineNumber) {
        return null;
    }

    @Override
    public int getLineCount() {
        return 0;
    }

    @Override
    public int getLineNumber(int offset) {
        return 0;
    }

    @Override
    public int getLineStartOffset(int lineNumber) {
        return 0;
    }

    @Override
    public int getLineLength(int lineNumber) {
        return 0;
    }

}
