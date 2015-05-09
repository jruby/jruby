/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.subsystems;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Arrays;

/**
 * Manages the features loaded into Ruby. This basically means which library files are loaded, but
 * Ruby often talks about requiring features, not files.
 * 
 */
public class FeatureManager {

    private final RubyContext context;

    private Source mainScriptSource = null;
    private String mainScriptFullPath = null;

    public FeatureManager(RubyContext context) {
        this.context = context;
    }

    public boolean require(String feature, Node currentNode) throws IOException {
        final RubyConstant dataConstantBefore = ModuleOperations.lookupConstant(context, LexicalScope.NONE, context.getCoreLibrary().getObjectClass(), "DATA");

        try {
            if (isAbsolutePath(feature)) {
                // Try as a full path

                if (requireInPath(null, feature, currentNode)) {
                    return true;
                }

            } else {
                // Some features are handled specially

                if (feature.equals("zlib")) {
                    context.getWarnings().warn("zlib not yet implemented");
                    return true;
                }

                if (feature.equals("rbconfig")) {
                    // Kernel#rbconfig is always there
                    return true;
                }

                // Try each load path in turn

                for (Object pathObject : context.getCoreLibrary().getLoadPath().slowToArray()) {
                    String loadPath = pathObject.toString();
                    if (!isAbsolutePath(loadPath)) {
                        loadPath = expandPath(context, loadPath);
                    }

                    if (requireInPath(loadPath, feature, currentNode)) {
                        return true;
                    }
                }
            }

            throw new RaiseException(context.getCoreLibrary().loadErrorCannotLoad(feature, currentNode));
        } finally {
            if (dataConstantBefore == null) {
                context.getCoreLibrary().getObjectClass().removeConstant(currentNode, "DATA");
            } else {
                context.getCoreLibrary().getObjectClass().setConstant(currentNode, "DATA", dataConstantBefore.getValue());
            }
        }
    }

    private boolean requireInPath(String path, String feature, Node currentNode) throws IOException {
        String fullPath = new File(path, feature).getPath();

        if (requireFile(fullPath, currentNode)) {
            return true;
        }

        if (requireFile(fullPath + ".rb", currentNode)) {
            return true;
        }

        return false;
    }

    public boolean isAbsolutePath(String path) {
        return path.startsWith("uri:classloader:") || path.startsWith("core:") || new File(path).isAbsolute();
    }

    private boolean requireFile(String path, Node currentNode) throws IOException {
        // We expect '/' in various classpath URLs, so normalize Windows file paths to use '/'
        path = path.replace('\\', '/');

        if (path.startsWith("uri:classloader:/")) {
            // TODO CS 13-Feb-15 this uri:classloader:/ and core:/ thing is a hack - simplify it

            for (Object loaded : Arrays.asList(context.getCoreLibrary().getLoadedFeatures().slowToArray())) {
                if (loaded.toString().equals(path)) {
                    return true;
                }
            }

            String coreFileName = path.substring("uri:classloader:/".length());

            coreFileName = FileSystems.getDefault().getPath(coreFileName).normalize().toString();

            if (context.getRuntime().getLoadService().getClassPathResource(context.getRuntime().getJRubyClassLoader(), coreFileName) == null) {
                return false;
            }

            context.getCoreLibrary().loadRubyCore(coreFileName, "uri:classloader:/");
            context.getCoreLibrary().getLoadedFeatures().slowPush(context.makeString(path));

            return true;
        }
        else if (path.startsWith("core:/")) {
            for (Object loaded : Arrays.asList(context.getCoreLibrary().getLoadedFeatures().slowToArray())) {
                if (loaded.toString().equals(path)) {
                    return true;
                }
            }

            final String coreFileName = path.substring("core:/".length());

            if (context.getRuntime().getLoadService().getClassPathResource(context.getRuntime().getJRubyClassLoader(), coreFileName) == null) {
                return false;
            }


            context.getCoreLibrary().loadRubyCore(coreFileName, "core:/");
            context.getCoreLibrary().getLoadedFeatures().slowPush(context.makeString(path));

            return true;
        } else {
            final File file = new File(path);

            assert file.isAbsolute();

            if (!file.isAbsolute() || !file.isFile()) {
                return false;
            }

            final String expandedPath = expandPath(context, path);

            for (Object loaded : Arrays.asList(context.getCoreLibrary().getLoadedFeatures().slowToArray())) {
                if (loaded.toString().equals(expandedPath)) {
                    return true;
                }
            }

            context.getCoreLibrary().getLoadedFeatures().slowPush(context.makeString(expandedPath));

            // TODO (nirvdrum 15-Jan-15): If we fail to load, we should remove the path from the loaded features because subsequent requires of the same statement may succeed.
            context.loadFile(path, currentNode);
        }

        return true;
    }

    public void setMainScriptSource(Source source) {
        this.mainScriptSource = source;
        if (!source.getPath().equals("-e")) {
            this.mainScriptFullPath = expandPath(context, source.getPath());
        }
    }

    public static String expandPath(RubyContext context, String fileName) {
        // TODO (nirvdrum 11-Feb-15) This needs to work on Windows without calling into non-Truffle JRuby.
        if (context.isRunningOnWindows()) {
            final org.jruby.RubyString path = context.toJRuby(context.makeString(fileName));
            final org.jruby.RubyString expanded = (org.jruby.RubyString) org.jruby.RubyFile.expand_path19(
                    context.getRuntime().getCurrentContext(),
                    null,
                    new org.jruby.runtime.builtin.IRubyObject[] { path });

            return expanded.asJavaString();
        } else {
            return expandPath(fileName, null);
        }
    }

    public static String expandPath(String fileName, String dir) {
        /*
         * TODO(cs): this isn't quite correct - I think we want to collapse .., but we don't want to
         * resolve symlinks etc. This might be where we want to start borrowing JRuby's
         * implementation, but it looks quite tied to their data structures.
         */

        return org.jruby.RubyFile.canonicalize(new File(dir, fileName).getAbsolutePath());
    }


    public String getSourcePath(Source source) {
        if (source == mainScriptSource) {
            return mainScriptFullPath;
        } else {
            return source.getPath();
        }
    }

}
