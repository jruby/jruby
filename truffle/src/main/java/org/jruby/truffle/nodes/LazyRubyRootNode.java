package org.jruby.truffle.nodes;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.methods.SetMethodDeclarationContext;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.RubyLanguage;
import org.jruby.truffle.translator.NodeWrapper;
import org.jruby.truffle.translator.TranslatorDriver;

public class LazyRubyRootNode extends RootNode {

    private final Source source;

    @CompilationFinal private RubyContext cachedContext;
    @CompilationFinal private DynamicObject mainObject;

    @Child private Node findContextNode;
    @Child private DirectCallNode callNode;

    public LazyRubyRootNode(SourceSection sourceSection, FrameDescriptor frameDescriptor, Source source) {
        super(RubyLanguage.class, sourceSection, frameDescriptor);
        this.source = source;
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        if (findContextNode == null) {
            CompilerDirectives.transferToInterpreter();

            findContextNode = insert(RubyLanguage.INSTANCE.unprotectedCreateFindContextNode());
        }

        final RubyContext context = RubyLanguage.INSTANCE.unprotectedFindContext(findContextNode);

        if (cachedContext == null) {
            CompilerDirectives.transferToInterpreter();

            cachedContext = context;
        }

        if (callNode == null || context != cachedContext) {
            CompilerDirectives.transferToInterpreter();

            final TranslatorDriver translator = new TranslatorDriver(context);

            final RubyRootNode rootNode = translator.parse(context, source, UTF8Encoding.INSTANCE, TranslatorDriver.ParserContext.EVAL, null, true, null, new NodeWrapper() {

                @Override
                public RubyNode wrap(RubyNode node) {
                    return new SetMethodDeclarationContext(node.getContext(), node.getSourceSection(), Visibility.PRIVATE, "TruffleLanguage#parse (lazy)", node);
                }

            });

            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);

            callNode = insert(Truffle.getRuntime().createDirectCallNode(callTarget));
            callNode.forceInlining();

            mainObject = context.getCoreLibrary().getMainObject();
        }

        return callNode.call(virtualFrame, RubyArguments.pack(null, null, mainObject, null, virtualFrame.getArguments()));
    }

}
