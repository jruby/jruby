/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.platform.sockets;

import org.jruby.ext.ffi.Platform;

public abstract class FDSetFactoryFactory {

    public static FDSetFactory create() {
        switch (Platform.getPlatform().getOS()) {
            case DARWIN:
            case LINUX:
                return new FDSetFactory() {
                    @Override
                    public FDSet create() {
                        return new LinuxFDSet();
                    }
                };

            default:
                throw new UnsupportedOperationException();
        }
    }

}
