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

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.internal.runtime.GlobalVariable;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.InputStream;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        final RubyInstanceConfig config = new RubyInstanceConfig();
        config.setHardExit(true);
        config.processArguments(args);

        InputStream in = config.getScriptSource();
        String filename = config.displayedFileName();

        final Ruby runtime = Ruby.newInstance(config);

        config.setCompileMode(RubyInstanceConfig.CompileMode.TRUFFLE);

        if (in != null) {
            int exitCode = runtime.getTruffleContext().execute(filename);
            System.exit(exitCode);
        }

    }

}
