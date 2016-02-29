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
import com.oracle.truffle.api.source.Source;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
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

    public boolean require(String feature, Node currentNode) {
        final String featurePath = findFeature(feature);

        if (featurePath == null) {
            throw new RaiseException(context.getCoreLibrary().loadErrorCannotLoad(feature, currentNode));
        }

        return doRequire(featurePath, currentNode);
    }

    private String findFeature(String feature) {
        final String currentDirectory = context.getNativePlatform().getPosix().getcwd();

        if (feature.startsWith("./")) {
            feature = currentDirectory + "/" + feature.substring(2);
        } else if (feature.startsWith("../")) {
            feature = currentDirectory.substring(0, currentDirectory.lastIndexOf('/')) + "/" + feature.substring(3);
        }
        
        if (feature.startsWith(SourceLoader.TRUFFLE_SCHEME)
                || feature.startsWith(SourceLoader.JRUBY_SCHEME)
                || new File(feature).isAbsolute()) {
            return findFeatureWithAndWithoutExtension(feature);
        }

        for (Object pathObject : ArrayOperations.toIterable(context.getCoreLibrary().getLoadPath())) {
            final String fileWithinPath = new File(pathObject.toString(), feature).getPath();
            final String result = findFeatureWithAndWithoutExtension(fileWithinPath);

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private String findFeatureWithAndWithoutExtension(String path) {
        final String withExtension = findFeatureWithExactPath(path + RubyLanguage.EXTENSION);

        if (withExtension != null) {
            return withExtension;
        }

        final String withoutExtension = findFeatureWithExactPath(path);

        if (withoutExtension != null) {
            return withoutExtension;
        }

        return null;
    }

    private String findFeatureWithExactPath(String path) {
        if (path.startsWith(SourceLoader.TRUFFLE_SCHEME) || path.startsWith(SourceLoader.JRUBY_SCHEME)) {
            return path;
        }

        final File file = new File(path);

        if (!file.isFile()) {
            return null;
        }

        try {
            if (file.isAbsolute()) {
                return file.getCanonicalPath();
            } else {
                return new File(context.getNativePlatform().getPosix().getcwd(), file.getPath()).getCanonicalPath();
            }
        } catch (IOException e) {
            return null;
        }
    }

    private boolean doRequire(String expandedPath, Node currentNode) {
        if (isFeatureLoaded(expandedPath)) {
            return false;
        }

        final DynamicObject pathString = StringOperations.createString(context,
                StringOperations.encodeRope(expandedPath, UTF8Encoding.INSTANCE));

        final Source source;

        try {
            source = context.getSourceCache().getSource(expandedPath);
        } catch (IOException e) {
            return false;
        }

        addToLoadedFeatures(pathString);

        try {
            final RubyRootNode rootNode = context.getCodeLoader().parse(
                    source,
                    UTF8Encoding.INSTANCE,
                    ParserContext.TOP_LEVEL,
                    null,
                    true,
                    currentNode);

            context.getCodeLoader().execute(
                    ParserContext.TOP_LEVEL,
                    DeclarationContext.TOP_LEVEL,
                    rootNode, null,
                    context.getCoreLibrary().getMainObject());
        } catch (RaiseException e) {
            removeFromLoadedFeatures(pathString);
            throw e;
        }

        return true;
    }

    private boolean isFeatureLoaded(String feature) {
        final DynamicObject loadedFeatures = context.getCoreLibrary().getLoadedFeatures();

        for (Object loaded : ArrayOperations.toIterable(loadedFeatures)) {
            if (loaded.toString().equals(feature)) {
                return true;
            }
        }

        return false;
    }

    private void addToLoadedFeatures(DynamicObject feature) {
        final DynamicObject loadedFeatures = context.getCoreLibrary().getLoadedFeatures();

        ArrayOperations.append(loadedFeatures, feature);
    }

    private void removeFromLoadedFeatures(DynamicObject feature) {
        final DynamicObject loadedFeatures = context.getCoreLibrary().getLoadedFeatures();
        final Object[] store = (Object[]) Layouts.ARRAY.getStore(loadedFeatures);
        final int length = Layouts.ARRAY.getSize(loadedFeatures);

        for (int i = length - 1; i >= 0; i--) {
            if (store[i] == feature) {
                ArrayUtils.arraycopy(store, i + 1, store, i, length - i - 1);
                Layouts.ARRAY.setSize(loadedFeatures, length - 1);
                break;
            }
        }
    }

}
