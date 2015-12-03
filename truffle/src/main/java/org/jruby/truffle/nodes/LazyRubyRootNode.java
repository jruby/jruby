package org.jruby.truffle.nodes;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import org.jcodings.specific.UTF8Encoding;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.methods.DeclarationContext;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.RubyLanguage;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.subsystems.AttachmentsManager;
import org.jruby.truffle.translator.TranslatorDriver;
import org.jruby.truffle.translator.TranslatorDriver.ParserContext;

public class LazyRubyRootNode extends RootNode {

    private final Source source;

    @CompilationFinal private RubyContext cachedContext;
    @CompilationFinal private DynamicObject mainObject;
    @CompilationFinal private InternalMethod method;

    @Child private Node findContextNode;
    @Child private DirectCallNode callNode;

    public LazyRubyRootNode(SourceSection sourceSection, FrameDescriptor frameDescriptor, Source source) {
        super(RubyLanguage.class, sourceSection, frameDescriptor);
        this.source = source;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (findContextNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            findContextNode = insert(RubyLanguage.INSTANCE.unprotectedCreateFindContextNode());
        }

        final RubyContext context = RubyLanguage.INSTANCE.unprotectedFindContext(findContextNode);

        if (cachedContext == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            cachedContext = context;
        }

        if (callNode == null || context != cachedContext) {
            CompilerDirectives.transferToInterpreter();

            if (AttachmentsManager.ATTACHMENT_SOURCE == source) {
                SourceSection sourceSection;
                DynamicObject block = null;
                try {
                    sourceSection = (SourceSection) frame.getArguments()[0];
                    block = (DynamicObject) frame.getArguments()[1];
                } catch (ClassCastException e) {
                    CompilerDirectives.transferToInterpreter();
                    for (Object arg : frame.getArguments()) {
                        System.out.println(arg.getClass() + " " + arg);
                    }
                    sourceSection = SourceSection.createUnavailable("attachment", "attachment");
                }

                final RootNode rootNode = new AttachmentsManager.AttachmentRootNode(RubyLanguage.class, cachedContext, sourceSection, null, block);
                final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);

                callNode = insert(Truffle.getRuntime().createDirectCallNode(callTarget));
                callNode.forceInlining();
            } else {
                final TranslatorDriver translator = new TranslatorDriver(context);
                final RubyRootNode rootNode = translator.parse(context, source, UTF8Encoding.INSTANCE, ParserContext.TOP_LEVEL, null, true, null);
                final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);

                callNode = insert(Truffle.getRuntime().createDirectCallNode(callTarget));
                callNode.forceInlining();

                mainObject = context.getCoreLibrary().getMainObject();
                method = new InternalMethod(rootNode.getSharedMethodInfo(), rootNode.getSharedMethodInfo().getName(),
                        context.getCoreLibrary().getObjectClass(), Visibility.PUBLIC, callTarget);
            }
        }

        if (method == null) {
            final MaterializedFrame callerFrame = Truffle.getRuntime().getCallerFrame().getFrame(FrameInstance.FrameAccess.MATERIALIZE, false).materialize();
            return callNode.call(frame, new Object[] { callerFrame });
        }

        return callNode.call(frame,
                RubyArguments.pack(method, null, null, mainObject, null, DeclarationContext.TOP_LEVEL, frame.getArguments()));
    }

}
