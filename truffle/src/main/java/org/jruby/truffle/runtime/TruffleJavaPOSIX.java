/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

import jnr.constants.platform.Fcntl;
import jnr.posix.LibC;
import jnr.posix.POSIX;

public class TruffleJavaPOSIX extends POSIXDelegator implements POSIX {

    private final RubyContext context;

    public TruffleJavaPOSIX(RubyContext context, POSIX delegateTo) {
        super(delegateTo);
        this.context = context;
    }

    @Override
    public int fcntlInt(int fd, Fcntl fcntlConst, int arg) {
        if (fcntlConst.longValue() == Fcntl.F_GETFL.longValue()) {
            switch (fd) {
                case 0:
                case 1:
                case 2:
                    return 0;
            }
        }

        return super.fcntlInt(fd, fcntlConst, arg);
    }

    @Override
    public int getpid() {
        return context.hashCode();
    }

    @Override
    public LibC libc() {
        return JavaLibC.INSTANCE;
    }

}
