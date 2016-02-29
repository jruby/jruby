/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
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
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.array.ArrayOperations;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.parser.ParserContext;

import java.io.File;
import java.io.IOException;

public class FeatureLoader {

    private final RubyContext context;

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

    public boolean require(String feature, Node currentNode) {
        final String x = findFeature(feature);

        if (x == null) {
            throw new RaiseException(context.getCoreLibrary().loadErrorCannotLoad(feature, currentNode));
        }

        return doRequire(x, currentNode).firstRequire;
    }

    private RequireResult doRequire(String expandedPath, Node currentNode) {
        final DynamicObject loadedFeatures = context.getCoreLibrary().getLoadedFeatures();

        for (Object loaded : ArrayOperations.toIterable(loadedFeatures)) {
            if (loaded.toString().equals(expandedPath)) {
                return RequireResult.ALREADY_REQUIRED;
            }
        }

        // TODO (nirvdrum 15-Jan-15): If we fail to load, we should remove the path from the loaded features because subsequent requires of the same statement may succeed.
        final DynamicObject pathString = StringOperations.createString(context, StringOperations.encodeRope(expandedPath, UTF8Encoding.INSTANCE));
        ArrayOperations.append(loadedFeatures, pathString);
        try {
            final RubyRootNode rootNode = context.getCodeLoader().parse(context.getSourceCache().getSource(expandedPath), UTF8Encoding.INSTANCE, ParserContext.TOP_LEVEL, null, true, currentNode);
            context.getCodeLoader().execute(ParserContext.TOP_LEVEL, DeclarationContext.TOP_LEVEL, rootNode, null, context.getCoreLibrary().getMainObject());
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

    private boolean isAbsolutePath(String path) {
        return path.startsWith(SourceLoader.TRUFFLE_SCHEME) || path.startsWith(SourceLoader.JRUBY_SCHEME) || new File(path).isAbsolute();
    }

    private static String expandPath(RubyContext context, String fileName) {
        String dir = new File(fileName).isAbsolute() ? null : context.getJRubyRuntime().getCurrentDirectory();
        return expandPath(fileName, dir);
    }

    private static String expandPath(String fileName, String dir) {
        /*
         * TODO(cs): this isn't quite correct - I think we want to collapse .., but we don't want to
         * resolve symlinks etc. This might be where we want to start borrowing JRuby's
         * implementation, but it looks quite tied to their data structures.
         */

        return org.jruby.RubyFile.canonicalize(new File(dir, fileName).getAbsolutePath());
    }

    private String findFeature(String feature) {
        if (feature.startsWith("./")) {
            final String cwd = context.getJRubyRuntime().getCurrentDirectory();
            feature = cwd + "/" + feature.substring(2);
        } else if (feature.startsWith("../")) {
            final String cwd = context.getJRubyRuntime().getCurrentDirectory();
            feature = cwd.substring(0, cwd.lastIndexOf('/')) + "/" + feature.substring(3);
        }

        if (isAbsolutePath(feature)) {
            // Try as a full path

            return tryToRequireFileInPath(null, feature, null);
        } else {
            // Try each load path in turn

            for (Object pathObject : ArrayOperations.toIterable(context.getCoreLibrary().getLoadPath())) {
                String loadPath = pathObject.toString();
                if (!isAbsolutePath(loadPath)) {
                    loadPath = expandPath(context, loadPath);
                }

                final String result = tryToRequireFileInPath(loadPath, feature, null);

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private String tryToRequireFileInPath(String path, String feature, Node currentNode) {
        String fullPath = new File(path, feature).getPath();

        final String x = tryToRequireFileInPath(fullPath + ".rb", currentNode);

        if (x != null) {
            return x;
        }

        final String y = tryToRequireFileInPath(fullPath, currentNode);

        if (y != null) {
            return y;
        }

        return null;
    }

    private String tryToRequireFileInPath(String path, Node currentNode) {
        final String expandedPath;

        if (!(path.startsWith(SourceLoader.TRUFFLE_SCHEME) || path.startsWith(SourceLoader.JRUBY_SCHEME))) {
            final File file = new File(path);

            assert file.isAbsolute();

            if (!file.isFile()) {
                return null;
            }

            try {
                expandedPath = new File(expandPath(context, path)).getCanonicalPath();
            } catch (IOException e) {
                return null;
            }
        } else {
            expandedPath = path;
        }

        return expandedPath;
    }

}
