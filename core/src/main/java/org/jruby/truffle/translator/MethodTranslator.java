/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.translator;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.common.IRubyWarnings;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.call.*;
import org.jruby.truffle.nodes.cast.ArrayCastNode;
import org.jruby.truffle.nodes.cast.ArrayCastNodeFactory;
import org.jruby.truffle.nodes.control.*;
import org.jruby.truffle.nodes.core.ArrayGetTailNodeFactory;
import org.jruby.truffle.nodes.core.ArrayIndexNode;
import org.jruby.truffle.nodes.core.ArrayIndexNodeFactory;
import org.jruby.truffle.nodes.literal.*;
import org.jruby.truffle.nodes.methods.*;
import org.jruby.truffle.nodes.methods.arguments.*;
import org.jruby.truffle.nodes.methods.locals.*;
import org.jruby.truffle.nodes.respondto.RespondToNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.methods.*;

class MethodTranslator extends BodyTranslator {

    private boolean isBlock;

    private static AtomicInteger translationFailureCount = new AtomicInteger();

    public MethodTranslator(RubyContext context, BodyTranslator parent, TranslatorEnvironment environment, boolean isBlock, Source source) {
        super(context, parent, environment, source);
        this.isBlock = isBlock;
    }

    public MethodDefinitionNode compileFunctionNode(SourceSection sourceSection, String methodName, org.jruby.ast.Node parseTree, org.jruby.ast.ArgsNode argsNode, org.jruby.ast.Node bodyNode, boolean ignoreLocalVisiblity) {
        environment.setMethodName(methodName);

        final ParameterCollector parameterCollector = new ParameterCollector();
        argsNode.accept(parameterCollector);

        for (String parameter : parameterCollector.getParameters()) {
            environment.declareVar(parameter);
        }

        findParameters(argsNode);

        final Arity arity = getArity(argsNode);

        RubyNode body;

        if (bodyNode != null) {
            body = bodyNode.accept(this);
        } else {
            body = new NilNode(context, sourceSection);
        }

        RubyNode originalBody = loadArgumentsIntoLocals(sourceSection, arity, body);

        try {
            final RubyNode newCheckArity = new CheckArityNode(context, sourceSection, arity);
            final LoadArgumentsTranslator loadArgumentsTranslator = new LoadArgumentsTranslator(context, source, isBlock, environment);
            final RubyNode newLoadArguments = argsNode.accept(loadArgumentsTranslator);

            final RubyNode newBlock;

            if (isBlock) {
                // TODO(CS): maybe > 1 is more correct
                if (arity.getMinimum() != 1 && !environment.hasRestParameter) {
                    final RubyNode readArrayNode = new ReadPreArgumentNode(context, sourceSection, 0, MissingArgumentBehaviour.RUNTIME_ERROR);
                    final RubyNode castArrayNode = ArrayCastNodeFactory.create(context, sourceSection, readArrayNode);
                    final FrameSlot arraySlot = environment.declareVar(environment.allocateLocalTemp("destructure"));
                    final RubyNode writeArrayNode = WriteLocalVariableNodeFactory.create(context, sourceSection, arraySlot, castArrayNode);

                    final LoadArgumentsTranslator destructureArgumentsTranslator = new LoadArgumentsTranslator(context, source, isBlock, environment);
                    destructureArgumentsTranslator.pushArraySlot(arraySlot);
                    final RubyNode newDestructureArguments = argsNode.accept(destructureArgumentsTranslator);

                    final RespondToNode respondToConvertAry = new RespondToNode(context, sourceSection, readArrayNode, "to_ary");
                    newBlock = new DestructureSwitchNode(context, sourceSection, arity, newLoadArguments, respondToConvertAry, SequenceNode.sequence(context, sourceSection, writeArrayNode, newDestructureArguments));
                } else {
                    newBlock = newLoadArguments;
                }
            } else {
                newBlock = SequenceNode.sequence(context, sourceSection, newCheckArity, newLoadArguments);
            }

            final RubyNode newBody = SequenceNode.sequence(context, sourceSection, newBlock, body);

            if (norm(NodeUtil.printTreeToString(originalBody)).equals(norm(NodeUtil.printTreeToString(newBody)))) {
                body = newBody;
            } else {
                System.err.println("NEW LOAD FAILED");
                System.err.println(sourceSection);
                System.err.println(argsNode.toString());
                System.err.println("original");
                NodeUtil.printTree(System.err, originalBody);
                System.err.println("new");
                NodeUtil.printTree(System.err, newBody);
                System.err.println("translation failures: " + translationFailureCount.incrementAndGet());
                body = originalBody;
            }
        } catch (Exception e) {
            System.err.println("NEW LOAD FAILED");
            System.err.println(sourceSection);
            System.err.println(argsNode.toString());
            e.printStackTrace();
            body = originalBody;
        }

        if (environment.getFlipFlopStates().size() > 0) {
            body = SequenceNode.sequence(context, sourceSection, initFlipFlopStates(sourceSection), body);
        }

        if (isBlock) {
            body = new RedoableNode(context, sourceSection, body);
        }

        body = new CatchReturnNode(context, sourceSection, body, environment.getReturnID(), isBlock);
        body = new CatchNextNode(context, sourceSection, body);
        body = new CatchRetryAsErrorNode(context, sourceSection, body);

        final RubyRootNode pristineRootNode = new RubyRootNode(sourceSection, environment.getFrameDescriptor(), methodName, parseTree, body);

        final CallTarget callTarget = Truffle.getRuntime().createCallTarget(NodeUtil.cloneNode(pristineRootNode));

        if (isBlock) {
            return new BlockDefinitionNode(context, sourceSection, methodName, environment.getUniqueMethodIdentifier(), environment.getFrameDescriptor(), environment.needsDeclarationFrame(),
                            pristineRootNode, callTarget);
        } else {
            return new MethodDefinitionNode(context, sourceSection, methodName, environment.getUniqueMethodIdentifier(), environment.getFrameDescriptor(), environment.needsDeclarationFrame(),
                            pristineRootNode, callTarget, ignoreLocalVisiblity);
        }
    }

    private static Arity getArity(org.jruby.ast.ArgsNode argsNode) {
        final int minimum = argsNode.getRequiredArgsCount();
        final int maximum = argsNode.getMaxArgumentsCount();
        return new Arity(minimum, maximum == -1 ? Arity.NO_MAXIMUM : maximum);
    }

    private RubyNode loadArgumentsIntoLocals(SourceSection sourceSection, Arity arity, RubyNode body) {
        final List<RubyNode> loadIndividualArgumentsNodes = new ArrayList<>();

        if (!isBlock) {
            loadIndividualArgumentsNodes.add(new CheckArityNode(context, sourceSection, arity));
        }

        final int preCount = environment.getPreParameters().size();
        final int postCount = environment.getPostParameters().size();

        for (int n = 0; n < environment.getPreParameters().size(); n++) {
            final FrameSlot param = environment.getPreParameters().get(n);

            // ReadPre reads from the start of the arguments array

            MissingArgumentBehaviour missingArgumentBehaviour;

            if (isBlock) {
                missingArgumentBehaviour = MissingArgumentBehaviour.NIL;
            } else {
                missingArgumentBehaviour = MissingArgumentBehaviour.RUNTIME_ERROR;
            }

            final ReadPreArgumentNode readArgumentNode = new ReadPreArgumentNode(context, sourceSection, n, missingArgumentBehaviour);

            final WriteLocalVariableNode writeLocal = WriteLocalVariableNodeFactory.create(context, sourceSection, param, readArgumentNode);

            loadIndividualArgumentsNodes.add(writeLocal);
        }

        for (int n = 0; n < environment.getOptionalParameters().size(); n++) {
            final FrameSlot param = environment.getOptionalParameters().get(n);
            final RubyNode defaultValue = environment.getOptionalParametersDefaultValues().get(param);

            /*
             * ReadOptional reads from the start of the arguments array, as long as it is long
             * enough, else uses the default value (which may use locals with arguments just loaded,
             * either from pre or preceding optionals).
             */

            final ReadOptionalArgumentNode readArgumentNode = new ReadOptionalArgumentNode(context, body.getEncapsulatingSourceSection(), preCount + n, preCount + postCount + n + 1,
                            (RubyNode) defaultValue.copy());

            final WriteLocalVariableNode writeLocal = WriteLocalVariableNodeFactory.create(context, sourceSection, param, readArgumentNode);

            loadIndividualArgumentsNodes.add(writeLocal);
        }

        for (int n = 0; n < environment.getPostParameters().size(); n++) {
            final FrameSlot param = environment.getPostParameters().get(n);

            // ReadPost reads from the end of the arguments array

            final ReadPostArgumentNode readArgumentNode = new ReadPostArgumentNode(context, sourceSection, postCount - n - 1);

            final WriteLocalVariableNode writeLocal = WriteLocalVariableNodeFactory.create(context, sourceSection, param, readArgumentNode);

            loadIndividualArgumentsNodes.add(writeLocal);
        }

        if (environment.getRestParameter() != null) {
            /*
             * TODO(cs): this assumes there are no optionals and therefore also no posts, which may
             * not be a valid assumption.
             */

            if (postCount != 0) {
                context.getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, body.getSourceSection().getSource().getName(), body.getSourceSection().getStartLine(), "post arguments as well as a rest argument - they will conflict");
            }

            final ReadRestArgumentNode readArgumentNode = new ReadRestArgumentNode(context, sourceSection, preCount);
            final WriteLocalVariableNode writeLocal = WriteLocalVariableNodeFactory.create(context, sourceSection, environment.getRestParameter(), readArgumentNode);
            loadIndividualArgumentsNodes.add(writeLocal);
        }

        if (environment.getBlockParameter() != null) {
            final FrameSlot param = environment.getBlockParameter();
            final ReadBlockNode readArgumentNode = new ReadBlockNode(context, sourceSection, NilPlaceholder.INSTANCE);
            final WriteLocalVariableNode writeLocal = WriteLocalVariableNodeFactory.create(context, sourceSection, param, readArgumentNode);
            loadIndividualArgumentsNodes.add(writeLocal);
        }

        final RubyNode loadIndividualArguments = SequenceNode.sequence(context, sourceSection, loadIndividualArgumentsNodes.toArray(new RubyNode[loadIndividualArgumentsNodes.size()]));

        final RubyNode noSwitch = SequenceNode.sequence(context, sourceSection, loadIndividualArguments, body);

        if (!isBlock) {
            return noSwitch;
        }

        /*
         * See the test testBlockArgumentsDestructure for a motivation for this. See
         * DestructureSwitchNode for how it works.
         */

        if (preCount + postCount == 1 && environment.getOptionalParameters().size() == 0 && environment.getRestParameter() == null) {
            return noSwitch;
        }

        if (preCount == 0 && environment.getRestParameter() != null) {
            return noSwitch;
        }

        final List<RubyNode> destructureLoadArgumentsNodes = new ArrayList<>();

        final String destructureArrayTemp = environment.allocateLocalTemp("destructure");
        final FrameSlot destructureArrayFrameSlot = environment.declareVar(destructureArrayTemp);
        final ArrayCastNode arrayCast = ArrayCastNodeFactory.create(context, sourceSection, new ReadPreArgumentNode(context, sourceSection, 0, MissingArgumentBehaviour.RUNTIME_ERROR));
        final WriteLocalVariableNode writeArrayToTemp = WriteLocalVariableNodeFactory.create(context, sourceSection, destructureArrayFrameSlot, arrayCast);
        destructureLoadArgumentsNodes.add(writeArrayToTemp);
        final ReadLocalVariableNode readArrayFromTemp = ReadLocalVariableNodeFactory.create(context, sourceSection, destructureArrayFrameSlot);

        for (int n = 0; n < environment.getPreParameters().size(); n++) {
            final FrameSlot param = environment.getPreParameters().get(n);
            final ArrayIndexNode readArgumentNode = ArrayIndexNodeFactory.create(context, sourceSection, n, NodeUtil.cloneNode(readArrayFromTemp));
            final WriteLocalVariableNode writeLocal = WriteLocalVariableNodeFactory.create(context, sourceSection, param, readArgumentNode);
            destructureLoadArgumentsNodes.add(writeLocal);
        }

        if (environment.getRestParameter() != null) {
            /*
             * TODO(cs): this assumes there are no optionals and therefore also no posts, which may
             * not be a valid assumption.
             */

            if (postCount != 0) {
                context.getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, body.getSourceSection().getSource().getName(), body.getSourceSection().getStartLine(), "post arguments as well as a rest argument - they will conflict");
            }

            final RubyNode readRestNode = ArrayGetTailNodeFactory.create(context, sourceSection, preCount, NodeUtil.cloneNode(readArrayFromTemp));
            final WriteLocalVariableNode writeLocal = WriteLocalVariableNodeFactory.create(context, sourceSection, environment.getRestParameter(), readRestNode);
            destructureLoadArgumentsNodes.add(writeLocal);
        }

        if (destructureLoadArgumentsNodes.size() == 1) {
            destructureLoadArgumentsNodes.add(new NilNode(context, sourceSection));
        }

        final RubyNode destructureLoadArguments = SequenceNode.sequence(context, body.getSourceSection(), destructureLoadArgumentsNodes.toArray(new RubyNode[destructureLoadArgumentsNodes.size()]));

        final RubyNode readArrayNode = new ReadPreArgumentNode(context, sourceSection, 0, MissingArgumentBehaviour.RUNTIME_ERROR);
        final RespondToNode respondToConvertAry = new RespondToNode(context, sourceSection, readArrayNode, "to_ary");
        return SequenceNode.sequence(context, sourceSection, new DestructureSwitchNode(context, body.getEncapsulatingSourceSection(), arity, loadIndividualArguments, respondToConvertAry, destructureLoadArguments), body);

    }

    private void findParameters(org.jruby.ast.ArgsNode args) {
        if (args == null) {
            return;
        }

        final SourceSection sourceSection = translate(args.getPosition());

        if (args.getPre() != null) {
            for (org.jruby.ast.Node arg : args.getPre().childNodes()) {
                if (arg instanceof org.jruby.ast.ArgumentNode) {
                    final org.jruby.ast.ArgumentNode argNode = (org.jruby.ast.ArgumentNode) arg;
                    final FrameSlot slot = environment.getFrameDescriptor().findFrameSlot(argNode.getName());
                    assert slot != null;
                    environment.getPreParameters().add(slot);
                } else if (arg instanceof org.jruby.ast.MultipleAsgn19Node) {
                    final org.jruby.ast.MultipleAsgn19Node multAsgn = (org.jruby.ast.MultipleAsgn19Node) arg;

                    final List<String> names = new ArrayList<>();
                    getNamesFromMultipleAssignment(multAsgn, names);

                    for (String name : names) {
                        final FrameSlot slot = environment.getFrameDescriptor().findFrameSlot(name);
                        assert slot != null;
                        environment.getPreParameters().add(slot);
                    }
                } else {
                    throw new UnsupportedOperationException(arg.getClass().toString());
                }
            }
        }

        // The JRuby parser expresses optional arguments as a block of local assignments

        /*
         * Note that default values for optional params can refer to the actual value of previous
         * args, so be careful with the order of args here and in loadArgumentsIntoLocals.
         */

        if (args.getOptArgs() != null) {
            for (org.jruby.ast.Node arg : args.getOptArgs().childNodes()) {
                final org.jruby.ast.OptArgNode optArgNode = (org.jruby.ast.OptArgNode) arg;

                String name;
                org.jruby.ast.Node valueNode;

                if (optArgNode.getValue() instanceof org.jruby.ast.LocalAsgnNode) {
                    final org.jruby.ast.LocalAsgnNode optLocalAsgn = (org.jruby.ast.LocalAsgnNode) optArgNode.getValue();
                    name = optLocalAsgn.getName();
                    valueNode = optLocalAsgn.getValueNode();
                } else if (optArgNode.getValue() instanceof org.jruby.ast.DAsgnNode) {
                    final org.jruby.ast.DAsgnNode optLocalAsgn = (org.jruby.ast.DAsgnNode) optArgNode.getValue();
                    name = optLocalAsgn.getName();
                    valueNode = optLocalAsgn.getValueNode();
                } else {
                    throw new UnsupportedOperationException(optArgNode.getValue().getClass().getName());
                }

                RubyNode paramDefaultValue;

                if (valueNode == null) {
                    paramDefaultValue = new NilNode(context, sourceSection);
                } else {
                    paramDefaultValue = (RubyNode) valueNode.accept(this);
                }

                final FrameSlot frameSlot = environment.getFrameDescriptor().findFrameSlot(name);
                assert frameSlot != null;
                environment.getOptionalParameters().add(frameSlot);
                environment.getOptionalParametersDefaultValues().put(frameSlot, paramDefaultValue);
            }
        }

        if (args.getPost() != null) {
            for (org.jruby.ast.Node arg : args.getPost().childNodes()) {
                final org.jruby.ast.ArgumentNode argNode = (org.jruby.ast.ArgumentNode) arg;
                final FrameSlot slot = environment.getFrameDescriptor().findFrameSlot(argNode.getName());
                assert slot != null;
                environment.getPostParameters().add(slot);
            }
        }

        if (args.getRestArgNode() != null) {
            final org.jruby.ast.RestArgNode rest = (org.jruby.ast.RestArgNode) args.getRestArgNode();
            final FrameSlot slot = environment.getFrameDescriptor().findFrameSlot(rest.getName());
            assert slot != null;
            environment.setRestParameter(slot);
        }

        if (args.getBlock() != null) {
            final org.jruby.ast.BlockArgNode blockArgNode = args.getBlock();
            final FrameSlot slot = environment.getFrameDescriptor().findFrameSlot(blockArgNode.getName());
            assert slot != null;
            environment.setBlockParameter(slot);
        }
    }

    private void getNamesFromMultipleAssignment(org.jruby.ast.MultipleAsgn19Node multAsgn, List<String> names) {
        for (org.jruby.ast.Node a : multAsgn.getPre().childNodes()) {
            if (a instanceof org.jruby.ast.DAsgnNode) {
                names.add(((org.jruby.ast.DAsgnNode) a).getName());
            } else if (a instanceof org.jruby.ast.MultipleAsgn19Node) {
                getNamesFromMultipleAssignment((org.jruby.ast.MultipleAsgn19Node) a, names);
            } else if (a instanceof org.jruby.ast.LocalAsgnNode) {
                names.add(((org.jruby.ast.LocalAsgnNode) a).getName());
            } else {
                throw new RuntimeException(a.getClass().getName());
            }
        }
    }

    @Override
    public RubyNode visitSuperNode(org.jruby.ast.SuperNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final ArgumentsAndBlockTranslation argumentsAndBlock = translateArgumentsAndBlock(sourceSection, node.getIterNode(), node.getArgsNode(), null);

        final String name = environment.getMethodName();

        return new GeneralSuperCallNode(context, sourceSection, name, argumentsAndBlock.getBlock(), argumentsAndBlock.getArguments(), argumentsAndBlock.isSplatted());
    }

    @Override
    public RubyNode visitZSuperNode(org.jruby.ast.ZSuperNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final String name = environment.getMethodName();

        return new GeneralSuperReCallNode(context, sourceSection, name);
    }

    @Override
    protected FlipFlopStateNode createFlipFlopState(SourceSection sourceSection, int depth) {
        if (isBlock) {
            environment.setNeedsDeclarationFrame();
            return parent.createFlipFlopState(sourceSection, depth + 1);
        } else {
            return super.createFlipFlopState(sourceSection, depth);
        }
    }

    private String norm(String tree) {
        return tree
                .replaceAll("@[0-9a-f]+", "@somewhere")
                .replaceAll("rubytruffle_temp_destructure_\\d+", "rubytruffle_temp_destructure_n")
                .replaceAll("frameSlot = \\[\\d+", "frameSlot = [n");
    }

}
