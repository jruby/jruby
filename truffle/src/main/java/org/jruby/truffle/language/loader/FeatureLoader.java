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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.core.array.ArrayOperations;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.core.thread.ThreadManager;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.parser.ParserContext;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class FeatureLoader {

    private final RubyContext context;

    private final ConcurrentHashMap<String, ReentrantLock> fileLocks = new ConcurrentHashMap<>();

    private final Object cextImplementationLock = new Object();
    private boolean cextImplementationLoaded = false;

    public FeatureLoader(RubyContext context) {
        this.context = context;
    }

    public boolean require(VirtualFrame frame, String feature, IndirectCallNode callNode) {
        final String featurePath = findFeature(feature);

        if (featurePath == null) {
            throw new RaiseException(context.getCoreExceptions().loadErrorCannotLoad(
                    feature,
                    callNode));
        }

        return doRequire(frame, featurePath, callNode);
    }

    private String findFeature(String feature) {
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

    private boolean doRequire(
            final VirtualFrame frame,
            final String expandedPath,
            final IndirectCallNode callNode) {

        if (isFeatureLoaded(expandedPath)) {
            return false;
        }

        while (true) {
            final ReentrantLock currentLock = fileLocks.get(expandedPath);
            final ReentrantLock lock;

            if (currentLock == null) {
                ReentrantLock newLock = new ReentrantLock();
                final ReentrantLock wasLock = fileLocks.putIfAbsent(expandedPath, newLock);
                lock = (wasLock == null) ? newLock : wasLock;
            } else {
                lock = currentLock;
            }

            if (lock.isHeldByCurrentThread()) {
                // circular require
                // TODO (pitr-ch 20-Mar-2016): warn user
                return false;
            }

            context.getThreadManager().runUntilResult(
                    callNode,
                    new ThreadManager.BlockingAction<Boolean>() {
                        @Override
                        public Boolean block() throws InterruptedException {
                            lock.lockInterruptibly();
                            return SUCCESS;
                        }
                    });

            // (1) Check that the lock is still correct, otherwise start over
            if (lock != fileLocks.get(expandedPath)) {
                lock.unlock();
                continue;
            }

            try {
                if (isFeatureLoaded(expandedPath)) {
                    return false;
                }

                final Source source;

                try {
                    source = context.getSourceCache().getSource(expandedPath);
                } catch (IOException e) {
                    return false;
                }

                final String mimeType = source.getMimeType();

                switch (mimeType) {
                    case RubyLanguage.MIME_TYPE: {
                        final RubyRootNode rootNode = context.getCodeLoader().parse(
                                source,
                                UTF8Encoding.INSTANCE,
                                ParserContext.TOP_LEVEL,
                                null,
                                true,
                                callNode);

                        final CodeLoader.DeferredCall deferredCall = context.getCodeLoader().prepareExecute(
                                ParserContext.TOP_LEVEL,
                                DeclarationContext.TOP_LEVEL,
                                rootNode,
                                null,
                                context.getCoreLibrary().getMainObject());

                        deferredCall.call(frame, callNode);
                    }
                    break;

                    case RubyLanguage.CEXT_MIME_TYPE: {
                        ensureCExtImplementationLoaded(frame, callNode);

                        final CallTarget callTarget;

                        try {
                            callTarget = context.getEnv().parse(source);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        callNode.call(frame, callTarget, new Object[]{});

                        final Object initFunction = context.getEnv().importSymbol("@Init_" + getBaseName(
                                expandedPath));

                        if (!(initFunction instanceof TruffleObject)) {
                            throw new UnsupportedOperationException();
                        }

                        final TruffleObject initFunctionObject = (TruffleObject) initFunction;

                        final Node isExecutableNode = Message.IS_EXECUTABLE.createNode();

                        if (!ForeignAccess.sendIsExecutable(
                                isExecutableNode,
                                frame,
                                initFunctionObject)) {
                            throw new UnsupportedOperationException();
                        }

                        final Node executeNode = Message.createExecute(0).createNode();

                        try {
                            ForeignAccess.sendExecute(executeNode, frame, initFunctionObject);
                        } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    break;

                    default:
                        throw new RaiseException(
                                context.getCoreExceptions().internalError(
                                        "unknown language " + expandedPath,
                                        callNode));
                }

                final DynamicObject pathString = StringOperations.createString(
                        context,
                        StringOperations.encodeRope(expandedPath, UTF8Encoding.INSTANCE));

                addToLoadedFeatures(pathString);

                return true;
            } finally {
                if (!lock.hasQueuedThreads()) {
                    // may remove lock after a thread starts waiting has to mitigated see (1)
                    fileLocks.remove(expandedPath);
                }
                lock.unlock();
            }
        }
    }

    private void ensureCExtImplementationLoaded(VirtualFrame frame, IndirectCallNode callNode) {
        synchronized (cextImplementationLock) {
            if (cextImplementationLoaded) {
                return;
            }

            final CallTarget callTarget;

            try {
                callTarget = context.getEnv().parse(Source.fromFileName(context.getJRubyRuntime().getJRubyHome() + "/lib/ruby/truffle/cext/ruby.su"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            callNode.call(frame, callTarget, new Object[]{});

            cextImplementationLoaded = true;
        }
    }

    private String getBaseName(String path) {
        final String name = new File(path).getName();

        final int firstDot = name.indexOf('.');

        if (firstDot == -1) {
            return name;
        } else {
            return name.substring(0, firstDot);
        }
    }

    // TODO (pitr-ch 16-Mar-2016): this protects the $LOADED_FEATURES only in this class,
    // it can still be accessed and modified (rare) by Ruby code which may cause issues
    private final Object loadedFeaturesLock = new Object();

    private boolean isFeatureLoaded(String feature) {
        synchronized (loadedFeaturesLock) {
            final DynamicObject loadedFeatures = context.getCoreLibrary().getLoadedFeatures();

            for (Object loaded : ArrayOperations.toIterable(loadedFeatures)) {
                if (loaded.toString().equals(feature)) {
                    return true;
                }
            }

            return false;
        }
    }

    private void addToLoadedFeatures(DynamicObject feature) {
        synchronized (loadedFeaturesLock) {

            final DynamicObject loadedFeatures = context.getCoreLibrary().getLoadedFeatures();
            final int size = Layouts.ARRAY.getSize(loadedFeatures);
            final Object[] store = (Object[]) Layouts.ARRAY.getStore(loadedFeatures);

            if (size < store.length) {
                store[size] = feature;
            } else {
                final Object[] newStore = ArrayUtils.grow(
                        store,
                        ArrayUtils.capacityForOneMore(context, store.length));
                newStore[size] = feature;
                Layouts.ARRAY.setStore(loadedFeatures, newStore);
            }
            Layouts.ARRAY.setSize(loadedFeatures, size + 1);
        }
    }

}
