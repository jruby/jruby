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
import org.jruby.ast.RootNode;
import org.jruby.internal.runtime.GlobalVariable;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.ThreadContext;
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
            // Global variables
            IAccessor programName = new ValueAccessor(runtime.newString(filename));
            runtime.getGlobalVariables().define("$PROGRAM_NAME", programName, GlobalVariable.Scope.GLOBAL);
            runtime.getGlobalVariables().define("$0", programName, GlobalVariable.Scope.GLOBAL);

            for (Map.Entry<String, String> entry : config.getOptionGlobals().entrySet()) {
                final IRubyObject varvalue;
                if (entry.getValue() != null) {
                    varvalue = runtime.newString(entry.getValue());
                } else {
                    varvalue = runtime.getTrue();
                }

                runtime.getGlobalVariables().set("$" + entry.getKey(), varvalue);
            }

            RootNode scriptNode = (RootNode) runtime.parseFromMain(filename, in);

            ThreadContext context = runtime.getCurrentContext();

            String oldFile = context.getFile();
            int oldLine = context.getLine();

            try {
                context.setFileAndLine(scriptNode.getPosition());
                runtime.getTruffleContext().execute(scriptNode);
            } finally {
                context.setFileAndLine(oldFile, oldLine);
            }
        }

    }

}
