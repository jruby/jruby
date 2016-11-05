/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.language.loader.SourceLoader;

import java.util.ArrayList;
import java.util.List;

public class JRubyInterop {

    private final Ruby jrubyRuntime;

    public JRubyInterop(Ruby jrubyRuntime) {
        this.jrubyRuntime = jrubyRuntime;
    }

    public String[] getOriginalLoadPath() {
        final List<String> loadPath = new ArrayList<>();

        for (IRubyObject path : ((org.jruby.RubyArray) jrubyRuntime.getLoadService().getLoadPath()).toJavaArray()) {
            String pathString = path.toString();

            if (!(pathString.endsWith("lib/ruby/2.2/site_ruby")
                    || pathString.endsWith("lib/ruby/shared")
                    || pathString.endsWith("lib/ruby/stdlib"))) {

                if (pathString.startsWith("uri:classloader:")) {
                    pathString = SourceLoader.JRUBY_SCHEME + pathString.substring("uri:classloader:".length());
                }

                loadPath.add(pathString);
            }
        }

        return loadPath.toArray(new String[loadPath.size()]);
    }

    public void setVerbose(boolean verbose) {
        jrubyRuntime.setVerbose(jrubyRuntime.newBoolean(verbose));
    }

    public void setVerboseNil() {
        jrubyRuntime.setVerbose(jrubyRuntime.getNil());
    }

    public boolean warningsEnabled() {
        return jrubyRuntime.warningsEnabled();
    }

    public boolean isVerbose() {
        return jrubyRuntime.isVerbose();
    }

    public void warn(String message) {
        jrubyRuntime.getWarnings().warn(message);
    }

}
