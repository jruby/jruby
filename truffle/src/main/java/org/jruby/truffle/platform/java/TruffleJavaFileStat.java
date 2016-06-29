/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.platform.java;

import jnr.posix.FileStat;
import jnr.posix.JavaFileStat;
import jnr.posix.POSIX;
import jnr.posix.POSIXHandler;

import java.io.File;

public class TruffleJavaFileStat extends JavaFileStat {

    private boolean executable = false;

    public TruffleJavaFileStat(POSIX posix, POSIXHandler handler) {
        super(posix, handler);
    }

    @Override
    public void setup(String path) {
        super.setup(path);

        executable = new File(path).canExecute();
    }

    @Override
    public int gid() {
        return 1;
    }

    @Override
    public int mode() {
        int mode = super.mode();

        if (executable) {
            mode |= FileStat.S_IXOTH;
        }

        return mode;
    }

}
