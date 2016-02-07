/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.loader;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;

import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.language.ModuleOperations;
import org.jruby.truffle.language.RubyConstant;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.array.ArrayMirror;
import org.jruby.truffle.core.array.ArrayReflector;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.core.array.ArrayOperations;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.core.Layouts;

import java.io.File;
import java.io.IOException;

public class FeatureLoader {

    private final RubyContext context;

    private Source mainScriptSource = null;
    private String mainScriptFullPath = null;

    public FeatureLoader(RubyContext context) {
        this.context = context;
    }

    private enum RequireResult {
        REQUIRED(true, true),
        ALREADY_REQUIRED(true, false),
        FAILED(false, false);

        public final boolean success;
        public final boolean firstRequire;

        RequireResult(boolean success, boolean firstRequire) {
            this.success = success;
            this.firstRequire = firstRequire;
        }
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

                final RequireResult result = tryToRequireFileInPath(null, feature, currentNode);

                if (result.success) {
                    return result.firstRequire;
                }
            } else {
                // Try each load path in turn

                for (Object pathObject : ArrayOperations.toIterable(context.getCoreLibrary().getLoadPath())) {
                    String loadPath = pathObject.toString();
                    if (!isAbsolutePath(loadPath)) {
                        loadPath = expandPath(context, loadPath);
                    }

                    final RequireResult result = tryToRequireFileInPath(loadPath, feature, currentNode);

                    if (result.success) {
                        return result.firstRequire;
                    }
                }
            }

            throw new RaiseException(context.getCoreLibrary().loadErrorCannotLoad(feature, currentNode));
        } finally {
            if (dataConstantBefore == null) {
                Layouts.MODULE.getFields(context.getCoreLibrary().getObjectClass()).removeConstant(context, currentNode, "DATA");
            } else {
                Layouts.MODULE.getFields(context.getCoreLibrary().getObjectClass()).setConstant(context, currentNode, "DATA", dataConstantBefore.getValue());
            }
        }
    }

    private RequireResult tryToRequireFileInPath(String path, String feature, Node currentNode) throws IOException {
        String fullPath = new File(path, feature).getPath();

        final RequireResult firstAttempt = tryToRequireFile(fullPath + ".rb", currentNode);

        if (firstAttempt.success) {
            return firstAttempt;
        }

        return tryToRequireFile(fullPath, currentNode);
    }

    private RequireResult tryToRequireFile(String path, Node currentNode) throws IOException {
        // We expect '/' in various classpath URLs, so normalize Windows file paths to use '/'
        path = path.replace('\\', '/');
        final DynamicObject loadedFeatures = context.getCoreLibrary().getLoadedFeatures();

        final String expandedPath;

        if (!(path.startsWith(SourceLoader.TRUFFLE_SCHEME) || path.startsWith(SourceLoader.JRUBY_SCHEME))) {
            final File file = new File(path);

            assert file.isAbsolute();

            if (!file.isFile()) {
                return RequireResult.FAILED;
            }

            expandedPath = new File(expandPath(context, path)).getCanonicalPath();
        } else {
            expandedPath = path;
        }

        for (Object loaded : ArrayOperations.toIterable(loadedFeatures)) {
            if (loaded.toString().equals(expandedPath)) {
                return RequireResult.ALREADY_REQUIRED;
            }
        }

        // TODO (nirvdrum 15-Jan-15): If we fail to load, we should remove the path from the loaded features because subsequent requires of the same statement may succeed.
        final DynamicObject pathString = StringOperations.createString(context, StringOperations.encodeRope(expandedPath, UTF8Encoding.INSTANCE));
        ArrayOperations.append(loadedFeatures, pathString);
        try {
            context.loadFile(expandedPath, currentNode);
        } catch (RaiseException e) {
            final Object[] store = (Object[]) Layouts.ARRAY.getStore(loadedFeatures);
            final int length = Layouts.ARRAY.getSize(loadedFeatures);
            for (int i = length - 1; i >= 0; i--) {
                if (store[i] == pathString) {
                    ArrayUtils.arraycopy(store, i + 1, store, i, length - i - 1);
                    Layouts.ARRAY.setSize(loadedFeatures, length - 1);
                    break;
                }
            }
            throw e;
        } catch (IOException e) {
            return RequireResult.FAILED;
        }

        return RequireResult.REQUIRED;
    }

    public void setMainScriptSource(Source source) {
        this.mainScriptSource = source;
        if (source.getPath() != null && !source.getPath().equals("-e")) {
            this.mainScriptFullPath = expandPath(context, source.getPath());
        }
    }

    public String getSourcePath(Source source) {
        if (source == mainScriptSource) {
            return mainScriptFullPath;
        } else {
            if (source.getPath() == null) {
                return source.getShortName();
            } else {
                return source.getPath();
            }
        }
    }

    public boolean isAbsolutePath(String path) {
        return path.startsWith(SourceLoader.TRUFFLE_SCHEME) || path.startsWith(SourceLoader.JRUBY_SCHEME) || new File(path).isAbsolute();
    }

    public static String expandPath(RubyContext context, String fileName) {
        // TODO (nirvdrum 11-Feb-15) This needs to work on Windows without calling into non-Truffle JRuby.
        if (context.isRunningOnWindows()) {
            final org.jruby.RubyString path = context.toJRubyString(StringOperations.createString(context, StringOperations.encodeRope(fileName, UTF8Encoding.INSTANCE)));
            final org.jruby.RubyString expanded = (org.jruby.RubyString) org.jruby.RubyFile.expand_path19(
                    context.getRuntime().getCurrentContext(),
                    null,
                    new org.jruby.runtime.builtin.IRubyObject[]{ path });

            return expanded.asJavaString();
        } else {
            String dir = new File(fileName).isAbsolute() ? null : context.getRuntime().getCurrentDirectory();
            return expandPath(fileName, dir);
        }
    }

    private static String expandPath(String fileName, String dir) {
        /*
         * TODO(cs): this isn't quite correct - I think we want to collapse .., but we don't want to
         * resolve symlinks etc. This might be where we want to start borrowing JRuby's
         * implementation, but it looks quite tied to their data structures.
         */

        return org.jruby.RubyFile.canonicalize(new File(dir, fileName).getAbsolutePath());
    }

}
