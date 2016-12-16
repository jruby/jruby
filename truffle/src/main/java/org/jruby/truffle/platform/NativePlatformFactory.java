/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.platform;

import com.oracle.truffle.api.TruffleOptions;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.platform.darwin.DarwinPlatform;
import org.jruby.truffle.platform.java.JavaPlatform;
import org.jruby.truffle.platform.linux.LinuxPlatform;
import org.jruby.truffle.platform.solaris.SolarisPlatform;

public abstract class NativePlatformFactory {

    private static final Platform.OS_TYPE OS = Platform.getPlatform().getOS();

    public static NativePlatform createPlatform(RubyContext context) {
        if (!TruffleOptions.AOT &&
                (context.getOptions().PLATFORM_USE_JAVA || (OS == Platform.OS_TYPE.WINDOWS))) {
            return new JavaPlatform(context);
        }

        if (OS == Platform.OS_TYPE.LINUX) {
            return new LinuxPlatform(context);
        }

        if (OS == Platform.OS_TYPE.SOLARIS) {
            return new SolarisPlatform(context);
        }

        if (OS == Platform.OS_TYPE.DARWIN) {
            return new DarwinPlatform(context);
        }

        throw new UnsupportedOperationException();
    }

}
