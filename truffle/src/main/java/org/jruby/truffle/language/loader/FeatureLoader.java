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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.source.Source;
import java.io.File;
import java.io.IOException;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.core.array.ArrayOperations;

public class FeatureLoader {

    private final RubyContext context;

    private final ReentrantLockFreeingMap<String> fileLocks = new ReentrantLockFreeingMap<String>();

    private final Object cextImplementationLock = new Object();
    private boolean cextImplementationLoaded = false;

    public FeatureLoader(RubyContext context) {
        this.context = context;
    }

    public ReentrantLockFreeingMap<String> getFileLocks() {
        return fileLocks;
    }

    @TruffleBoundary
    public String findFeature(String feature) {
        final String currentDirectory = context.getNativePlatform().getPosix().getcwd();

        if (feature.startsWith("./")) {
            feature = currentDirectory + "/" + feature.substring(2);
        } else if (feature.startsWith("../")) {
            feature = currentDirectory.substring(
                    0,
                    currentDirectory.lastIndexOf('/')) + "/" + feature.substring(3);
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
        final String asCExt = findFeatureWithExactPath(path + RubyLanguage.CEXT_EXTENSION);

        if (asCExt != null) {
            return asCExt;
        }

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
                return new File(
                        context.getNativePlatform().getPosix().getcwd(),
                        file.getPath()).getCanonicalPath();
            }
        } catch (IOException e) {
            return null;
        }
    }

    public void ensureCExtImplementationLoaded(VirtualFrame frame, IndirectCallNode callNode) {
        synchronized (cextImplementationLock) {
            if (cextImplementationLoaded) {
                return;
            }

            final CallTarget callTarget = getCExtLibRuby();
            callNode.call(frame, callTarget, new Object[] {});

            cextImplementationLoaded = true;
        }
    }

    @TruffleBoundary
    private CallTarget getCExtLibRuby() {
        final String path = context.getJRubyRuntime().getJRubyHome() + "/lib/ruby/truffle/cext/ruby.su";
        try {
            return parseSource(context.getSourceLoader().load(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @TruffleBoundary
    public CallTarget parseSource(Source source) {
        try {
            return context.getEnv().parse(source);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // TODO (pitr-ch 16-Mar-2016): this protects the $LOADED_FEATURES only in this class,
    // it can still be accessed and modified (rare) by Ruby code which may cause issues
    private final Object loadedFeaturesLock = new Object();

    public Object getLoadedFeaturesLock() {
        return loadedFeaturesLock;
    }

}
