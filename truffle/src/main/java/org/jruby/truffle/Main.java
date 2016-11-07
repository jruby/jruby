/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle;

import org.jruby.JRubyTruffleInterface;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;

import java.io.InputStream;

public class Main {

    public static void main(String[] args) {
        final RubyInstanceConfig config = new RubyInstanceConfig();
        config.setHardExit(true);
        config.processArguments(args);

        final InputStream in = config.getScriptSource();
        final String filename = config.displayedFileName();

        final JRubyTruffleInterface jrubyTruffle = new JRubyTruffleImpl(config);

        if (in != null) {
            final int exitCode = jrubyTruffle.execute(filename);
            System.exit(exitCode);
        }

    }

}
