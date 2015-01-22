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

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyException;
import org.jruby.truffle.runtime.core.RubyFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Manages the features loaded into Ruby. This basically means which library files are loaded, but
 * Ruby often talks about requiring features, not files.
 * 
 */
public class FeatureManager {

    private RubyContext context;

    public FeatureManager(RubyContext context) {
        this.context = context;
    }

    public boolean require(String path, String feature, RubyNode currentNode) throws IOException {
        final RubyConstant dataConstantBefore = ModuleOperations.lookupConstant(context, LexicalScope.NONE, context.getCoreLibrary().getObjectClass(), "DATA");

        try {
            if (path != null) {
                if (requireInPath(path, feature, currentNode)) {
                    return true;
                }
            } else {
                // Some features are handled specially

                if (feature.equals("zlib")) {
                    context.getWarnings().warn("zlib not yet implemented");
                    return true;
                }

                if (feature.equals("enumerator")) {
                    context.getWarnings().warn("enumerator not yet implemented");
                    return true;
                }

                if (feature.equals("rbconfig")) {
                    // Kernel#rbconfig is always there
                    return true;
                }

                if (feature.equals("thread")) {
                    return true;
                }

                if (feature.equals("time")) {
                    return true;
                }

                // Try as a full path

                if (requireFile(feature, currentNode)) {
                    return true;
                }

                if (requireFile(feature + ".rb", currentNode)) {
                    return true;
                }

                // Try as a path relative to the current directory

                if (requireInPath(context.getRuntime().getCurrentDirectory(), feature, currentNode)) {
                    return true;
                }

                // Try each load path in turn

                for (Object pathObject : context.getCoreLibrary().getLoadPath().slowToArray()) {
                    final String loadPath = pathObject.toString();

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

    private boolean requireInPath(String path, String feature, RubyNode currentNode) throws IOException {
        if (requireFile(new File(path, feature).getPath(), currentNode)) {
            return true;
        }

        if (requireFile(new File(path, feature).getPath() + ".rb", currentNode)) {
            return true;
        }

        return false;
    }

    private boolean requireFile(String fileName, RubyNode currentNode) throws IOException {
        // We expect '/' in various classpath URLs, so normalize Windows file paths to use '/'
        fileName = fileName.replace('\\', '/');

        if (fileName.startsWith("core:/")) {
            for (Object loaded : Arrays.asList(context.getCoreLibrary().getLoadedFeatures().slowToArray())) {
                if (loaded.toString().equals(fileName)) {
                    return true;
                }
            }

            final String coreFileName = fileName.substring("core:/".length());

            if (context.getRuntime().getLoadService().getClassPathResource(context.getRuntime().getJRubyClassLoader(), coreFileName) == null) {
                return false;
            }

            context.getCoreLibrary().loadRubyCore(coreFileName);
            context.getCoreLibrary().getLoadedFeatures().slowPush(context.makeString(fileName));

            return true;
        } else {
            final File file = new File(fileName);

            if (!file.isFile()) {
                return false;
            }

            final String expandedPath = RubyFile.expandPath(fileName);

            for (Object loaded : Arrays.asList(context.getCoreLibrary().getLoadedFeatures().slowToArray())) {
                if (loaded.toString().equals(expandedPath)) {
                    return true;
                }
            }

            context.getCoreLibrary().getLoadedFeatures().slowPush(context.makeString(expandedPath));

            // TODO (nirvdrum 15-Jan-15): If we fail to load, we should remove the path from the loaded features because subsequent requires of the same statement may succeed.
            context.loadFile(fileName, currentNode);
        }

        return true;
    }

}
