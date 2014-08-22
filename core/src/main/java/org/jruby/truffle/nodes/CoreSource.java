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

public final class CoreSource implements Source {

    private final String className;
    private final String methodName;

    public CoreSource(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    @Override
    public String getName() {
        return toString();
    }

    @Override
    public String getShortName() {
        return toString();
    }

    @Override
    public String getCode() {
        return toString();
    }

    @Override
    public String toString() {
        return String.format("%s#%s", className, methodName);
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

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

}
