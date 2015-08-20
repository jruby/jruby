/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.loader;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.array.ArrayMirror;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.layouts.Layouts;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class FeatureLoader {

    private final RubyContext context;

    private Source mainScriptSource = null;
    private String mainScriptFullPath = null;

    public FeatureLoader(RubyContext context) {
        this.context = context;
    }

    public boolean require(String feature, Node currentNode) throws IOException {
        final RubyConstant dataConstantBefore = ModuleOperations.lookupConstant(context, context.getCoreLibrary().getObjectClass(), "DATA");

        if (feature.startsWith("./")) {
            final String cwd = context.getRuntime().getCurrentDirectory();
            feature = cwd + "/" + feature.substring(2);
        } else if (feature.startsWith("../")) {
            final String cwd = context.getRuntime().getCurrentDirectory();
            feature = cwd.substring(0, cwd.lastIndexOf('/')) + "/" + feature.substring(3);
        }

        try {
            if (isAbsolutePath(feature)) {
                // Try as a full path

                if (tryToRequireFileInPath(null, feature, currentNode)) {
                    return true;
                }
            } else {
                // Try each load path in turn

                for (Object pathObject : ArrayNodes.slowToArray(context.getCoreLibrary().getLoadPath())) {
                    String loadPath = pathObject.toString();
                    if (!isAbsolutePath(loadPath)) {
                        loadPath = expandPath(context, loadPath);
                    }

                    if (tryToRequireFileInPath(loadPath, feature, currentNode)) {
                        return true;
                    }
                }
            }

            throw new RaiseException(context.getCoreLibrary().loadErrorCannotLoad(feature, currentNode));
        } finally {
            if (dataConstantBefore == null) {
                Layouts.MODULE.getFields(context.getCoreLibrary().getObjectClass()).removeConstant(currentNode, "DATA");
            } else {
                Layouts.MODULE.getFields(context.getCoreLibrary().getObjectClass()).setConstant(currentNode, "DATA", dataConstantBefore.getValue());
            }
        }
    }

    private boolean tryToRequireFileInPath(String path, String feature, Node currentNode) throws IOException {
        String fullPath = new File(path, feature).getPath();

        if (tryToRequireFile(feature, fullPath, currentNode)) {
            return true;
        }

        if (tryToRequireFile(feature, fullPath + ".rb", currentNode)) {
            return true;
        }

        return false;
    }

    private boolean tryToRequireFile(String feature, String path, Node currentNode) throws IOException {
        // We expect '/' in various classpath URLs, so normalize Windows file paths to use '/'
        path = path.replace('\\', '/');
        final DynamicObject loadedFeatures = context.getCoreLibrary().getLoadedFeatures();

        final String expandedPath;

        if (!(path.startsWith(SourceLoader.TRUFFLE_SCHEME) || path.startsWith(SourceLoader.JRUBY_SCHEME))) {
            final File file = new File(path);

            assert file.isAbsolute();

            if (!file.isAbsolute() || !file.isFile()) {
                return false;
            }

            expandedPath = new File(expandPath(context, path)).getCanonicalPath();
        } else {
            expandedPath = path;
        }

        for (Object loaded : Arrays.asList(ArrayNodes.slowToArray(loadedFeatures))) {
            if (loaded.toString().equals(expandedPath)) {
                return true;
            }
        }

        // TODO (nirvdrum 15-Jan-15): If we fail to load, we should remove the path from the loaded features because subsequent requires of the same statement may succeed.
        final DynamicObject pathString = StringNodes.createString(context.getCoreLibrary().getStringClass(), expandedPath);
        ArrayNodes.slowPush(loadedFeatures, pathString);
        try {
            context.loadFile(expandedPath, currentNode);
        } catch (RaiseException e) {
            final ArrayMirror mirror = ArrayMirror.reflect((Object[]) Layouts.ARRAY.getStore(loadedFeatures));
            final int length = Layouts.ARRAY.getSize(loadedFeatures);
            for (int i = length - 1; i >= 0; i--) {
                if (mirror.get(i) == pathString) {
                    for (int j = length - 1; j > i; j--) {
                        mirror.set(i - 1, mirror.get(i));
                    }
                    Layouts.ARRAY.setSize(loadedFeatures, length - 1);
                    break;
                }
            }
            throw e;
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public void setMainScriptSource(Source source) {
        this.mainScriptSource = source;
        if (!source.getPath().equals("-e")) {
            this.mainScriptFullPath = expandPath(context, source.getPath());
        }
    }

    public String getSourcePath(Source source) {
        if (source == mainScriptSource) {
            return mainScriptFullPath;
        } else {
            return source.getPath();
        }
    }

    public boolean isAbsolutePath(String path) {
        return path.startsWith(SourceLoader.TRUFFLE_SCHEME) || path.startsWith(SourceLoader.JRUBY_SCHEME) || new File(path).isAbsolute();
    }

    public static String expandPath(RubyContext context, String fileName) {
        // TODO (nirvdrum 11-Feb-15) This needs to work on Windows without calling into non-Truffle JRuby.
        if (context.isRunningOnWindows()) {
            final org.jruby.RubyString path = context.toJRubyString(StringNodes.createString(context.getCoreLibrary().getStringClass(), fileName));
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

}
