package org.jruby.truffle.language.loader;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.Source;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.parser.ParserContext;

@NodeChild("feature")
public abstract class RequireNode extends RubyNode {

    @Child IndirectCallNode callNode = IndirectCallNode.create();
    @Child CallDispatchHeadNode isInLoadedFeatures = CallDispatchHeadNode.createMethodCall();
    @Child CallDispatchHeadNode addToLoadedFeatures = CallDispatchHeadNode.createMethodCall();

    @Child Node isExecutableNode = Message.IS_EXECUTABLE.createNode();
    @Child Node executeNode = Message.createExecute(0).createNode();

    public static RequireNode create() {
        return RequireNodeGen.create(null);
    }

    public abstract boolean executeRequire(VirtualFrame frame, String feature);

    @Specialization
    protected boolean require(VirtualFrame frame, String feature,
            @Cached("create()") BranchProfile errorProfile,
            @Cached("createBinaryProfile()") ConditionProfile isLoadedProfile) {
        final FeatureLoader featureLoader = getContext().getFeatureLoader();

        final String expandedPath = featureLoader.findFeature(feature);

        if (expandedPath == null) {
            errorProfile.enter();
            throw new RaiseException(getContext().getCoreExceptions().loadErrorCannotLoad(feature, this));
        }

        final DynamicObject pathString = StringOperations.createString(getContext(),
                StringOperations.encodeRope(expandedPath, UTF8Encoding.INSTANCE));

        if (isLoadedProfile.profile(isFeatureLoaded(frame, pathString))) {
            return false;
        }

        final ReentrantLockFreeingMap<String> fileLocks = featureLoader.getFileLocks();

        while (true) {
            final ReentrantLock lock = fileLocks.get(expandedPath);

            if (lock.isHeldByCurrentThread()) {
                // circular require
                // TODO (pitr-ch 20-Mar-2016): warn user
                return false;
            }

            if (!fileLocks.lock(this, getContext().getThreadManager(), expandedPath, lock)) {
                continue;
            }

            try {
                if (isFeatureLoaded(frame, pathString)) {
                    return false;
                }

                final Source source;
                try {
                    source = getContext().getSourceCache().getSource(expandedPath);
                } catch (IOException e) {
                    return false;
                }

                final String mimeType = source.getMimeType();
                switch (mimeType) {
                    case RubyLanguage.MIME_TYPE: {
                        final RubyRootNode rootNode = getContext().getCodeLoader().parse(
                                source,
                                UTF8Encoding.INSTANCE,
                                ParserContext.TOP_LEVEL,
                                null,
                                true,
                                this);

                        final CodeLoader.DeferredCall deferredCall = getContext().getCodeLoader().prepareExecute(
                                ParserContext.TOP_LEVEL,
                                DeclarationContext.TOP_LEVEL,
                                rootNode,
                                null,
                                coreLibrary().getMainObject());

                        deferredCall.call(frame, callNode);
                        break;
                    }

                    case RubyLanguage.CEXT_MIME_TYPE: {
                        featureLoader.ensureCExtImplementationLoaded(frame, callNode);

                        final CallTarget callTarget = featureLoader.parseSource(source);
                        callNode.call(frame, callTarget, new Object[] {});

                        final TruffleObject initFunction = getInitFunction(expandedPath);

                        if (!ForeignAccess.sendIsExecutable(isExecutableNode, frame, initFunction)) {
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            throw new UnsupportedOperationException();
                        }

                        try {
                            ForeignAccess.sendExecute(executeNode, frame, initFunction);
                        } catch (InteropException e) {
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            throw new RuntimeException(e);
                        }
                        break;
                    }

                    default:
                        errorProfile.enter();
                        throw new RaiseException(unknownLanguage(expandedPath, mimeType));
                }

                addToLoadedFeatures(frame, pathString);

                return true;
            } finally {
                fileLocks.unlock(expandedPath, lock);
            }
        }
    }

    @TruffleBoundary
    private DynamicObject unknownLanguage(String expandedPath, final String mimeType) {
        return getContext().getCoreExceptions().internalError(
                "unknown language " + mimeType + " for " + expandedPath,
                callNode);
    }

    @TruffleBoundary
    private TruffleObject getInitFunction(final String expandedPath) {
        final Object initFunction = getContext().getEnv().importSymbol("@Init_" + getBaseName(expandedPath));

        if (!(initFunction instanceof TruffleObject)) {
            throw new UnsupportedOperationException();
        }

        return (TruffleObject) initFunction;
    }

    @TruffleBoundary
    private String getBaseName(String path) {
        final String name = new File(path).getName();
        final int firstDot = name.indexOf('.');
        if (firstDot == -1) {
            return name;
        } else {
            return name.substring(0, firstDot);
        }
    }

    public boolean isFeatureLoaded(VirtualFrame frame, DynamicObject feature) {
        final DynamicObject loadedFeatures = getContext().getCoreLibrary().getLoadedFeatures();
        synchronized (getContext().getFeatureLoader().getLoadedFeaturesLock()) {
            return isInLoadedFeatures.callBoolean(frame, loadedFeatures, "include?", null, feature);
        }
    }

    private void addToLoadedFeatures(VirtualFrame frame, DynamicObject feature) {
        final DynamicObject loadedFeatures = coreLibrary().getLoadedFeatures();
        synchronized (getContext().getFeatureLoader().getLoadedFeaturesLock()) {
            addToLoadedFeatures.call(frame, loadedFeatures, "<<", null, feature);
        }
    }

}
