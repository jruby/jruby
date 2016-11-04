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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleOptions;
import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.control.JavaException;
import org.jruby.truffle.language.loader.SourceLoader;
import org.jruby.util.ByteList;
import org.jruby.util.io.EncodingUtils;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class JRubyInterop {

    private final Ruby jrubyRuntime;
    private final RubyContext context;

    private String originalInputFile;
    private final String jrubyHome;

    public JRubyInterop(RubyContext context, Ruby jrubyRuntime) {
        this.context = context;
        this.jrubyRuntime = jrubyRuntime;
        this.jrubyHome = findJRubyHome();
    }

    public String getJRubyHome() {
        return jrubyHome;
    }

    private String findJRubyHome() {
        if (!TruffleOptions.AOT && System.getenv("JRUBY_HOME") == null && System.getProperty("jruby.home") == null) {
            // Set JRuby home automatically for GraalVM
            final CodeSource codeSource = Ruby.class.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                final File currentJarFile;
                try {
                    currentJarFile = new File(codeSource.getLocation().toURI());
                } catch (URISyntaxException e) {
                    throw new JavaException(e);
                }

                if (currentJarFile.getName().equals("ruby.jar")) {
                    String jarDir = currentJarFile.getParent();
                    if (new File(jarDir, "lib").isDirectory()) {
                        jrubyRuntime.setJRubyHome(jarDir);
                        return jarDir;
                    }
                }
            }
        }

        return jrubyRuntime.getJRubyHome();
    }

    @TruffleBoundary
    public String getArg0() {
        return jrubyRuntime.getGlobalVariables().get("$0").toString();
    }

    public String[] getArgv() {
        final IRubyObject[] jrubyStrings = ((org.jruby.RubyArray) jrubyRuntime.getObject().getConstant("ARGV")).toJavaArray();
        final String[] strings = new String[jrubyStrings.length];

        for (int n = 0; n < strings.length; n++) {
            strings[n] = jrubyStrings[n].toString();
        }

        return strings;
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

    public void setOriginalInputFile(String originalInputFile) {
        this.originalInputFile = originalInputFile;
    }

    public String getOriginalInputFile() {
        return originalInputFile;
    }

    public boolean isDebug() {
        return jrubyRuntime.isDebug();
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

    public Random getRandom() {
        return jrubyRuntime.getRandom();
    }

    public ByteList strConvEnc(ByteList byteList, Encoding encoding) {
        return EncodingUtils.strConvEnc(jrubyRuntime.getCurrentContext(), byteList, encoding, UTF8Encoding.INSTANCE);
    }

    public void setDefaultInternalEncoding(Encoding defaultInternalEncoding) {
        jrubyRuntime.setDefaultInternalEncoding(defaultInternalEncoding);
    }

    public void setData(IRubyObject data) {
        IRubyObject verbose = jrubyRuntime.getVerbose();
        jrubyRuntime.setVerbose(jrubyRuntime.getNil());
        jrubyRuntime.defineGlobalConstant("DATA", data);
        jrubyRuntime.setVerbose(verbose);
    }
}
