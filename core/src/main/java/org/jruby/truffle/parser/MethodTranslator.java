/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.parser;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.common.IRubyWarnings;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.call.*;
import org.jruby.truffle.nodes.control.*;
import org.jruby.truffle.nodes.literal.*;
import org.jruby.truffle.nodes.methods.*;
import org.jruby.truffle.nodes.methods.arguments.*;
import org.jruby.truffle.nodes.methods.locals.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.methods.*;

class MethodTranslator extends Translator {

    private boolean isBlock;

    public MethodTranslator(RubyContext context, Translator parent, TranslatorEnvironment environment, boolean isBlock, Source source) {
        super(context, parent, environment, source);
        this.isBlock = isBlock;
    }

    public MethodDefinitionNode compileFunctionNode(SourceSection sourceSection, String methodName, org.jruby.ast.ArgsNode argsNode, org.jruby.ast.Node bodyNode) {
        environment.setMethodName(methodName);

        final Arity arity = findParameters(argsNode);

        RubyNode body;

        if (bodyNode != null) {
            body = (RubyNode) bodyNode.accept(this);
        } else {
            body = new NilNode(context, sourceSection);
        }

        body = loadArgumentsIntoLocals(arity, body);

        if (environment.getFlipFlopStates().size() > 0) {
            body = new SequenceNode(context, sourceSection, initFlipFlopStates(sourceSection), body);
        }

        if (isBlock) {
            body = new CatchNextNode(context, sourceSection, body);
        } else {
            body = new CatchReturnNode(context, sourceSection, body, environment.getReturnID());
            body = new CatchNextNode(context, sourceSection, body);
        }

        final RubyRootNode pristineRootNode = new RubyRootNode(sourceSection, methodName, body);

        final CallTarget callTarget = Truffle.getRuntime().createCallTarget(NodeUtil.cloneNode(pristineRootNode), environment.getFrameDescriptor());

        if (isBlock) {
            return new BlockDefinitionNode(context, sourceSection, methodName, environment.getUniqueMethodIdentifier(), environment.getFrameDescriptor(), environment.needsDeclarationFrame(),
                            pristineRootNode, callTarget);
        } else {
            return new MethodDefinitionNode(context, sourceSection, methodName, environment.getUniqueMethodIdentifier(), environment.getFrameDescriptor(), environment.needsDeclarationFrame(),
                            pristineRootNode, callTarget);
        }
    }

    private RubyNode loadArgumentsIntoLocals(Arity arity, RubyNode body) {
        final SourceSection sourceSection = body.getEncapsulatingSourceSection();

        final List<RubyNode> loadIndividualArgumentsNodes = new ArrayList<>();

        if (!isBlock) {
            loadIndividualArgumentsNodes.add(new CheckArityNode(context, sourceSection, arity));
        }

        final int preCount = environment.getPreParameters().size();
        final int postCount = environment.getPostParameters().size();

        for (int n = 0; n < environment.getPreParameters().size(); n++) {
            final FrameSlot param = environment.getPreParameters().get(n);

            // ReadPre reads from the start of the arguments array

            final ReadPreArgumentNode readArgumentNode = new ReadPreArgumentNode(context, sourceSection, n, false);

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

            final ReadBlockArgumentNode readArgumentNode = new ReadBlockArgumentNode(context, sourceSection, false);

            final WriteLocalVariableNode writeLocal = WriteLocalVariableNodeFactory.create(context, sourceSection, param, readArgumentNode);

            loadIndividualArgumentsNodes.add(writeLocal);
        }

        final RubyNode loadIndividualArguments = new SequenceNode(context, sourceSection, loadIndividualArgumentsNodes.toArray(new RubyNode[loadIndividualArgumentsNodes.size()]));

        final RubyNode noSwitch = new SequenceNode(context, body.getSourceSection(), loadIndividualArguments, body);

        if (!isBlock) {
            return noSwitch;
        }

        /*
         * See the test testBlockArgumentsDestructure for a motivation for this. See
         * BlockDestructureSwitchNode for how it works.
         */

        if (preCount + postCount == 1 && environment.getOptionalParameters().size() == 0) {
            return noSwitch;
        }

        final List<RubyNode> destructureLoadArgumentsNodes = new ArrayList<>();

        for (int n = 0; n < environment.getPreParameters().size(); n++) {
            final FrameSlot param = environment.getPreParameters().get(n);

            final ReadDestructureArgumentNode readArgumentNode = new ReadDestructureArgumentNode(context, sourceSection, n);

            final WriteLocalVariableNode writeLocal = WriteLocalVariableNodeFactory.create(context, sourceSection, param, readArgumentNode);

            destructureLoadArgumentsNodes.add(writeLocal);
        }

        final RubyNode destructureLoadArguments = new SequenceNode(context, body.getSourceSection(), destructureLoadArgumentsNodes.toArray(new RubyNode[destructureLoadArgumentsNodes.size()]));

        return new BlockDestructureSwitchNode(context, body.getEncapsulatingSourceSection(), loadIndividualArguments, destructureLoadArguments, body);

    }

    private Arity findParameters(org.jruby.ast.ArgsNode args) {
        if (args == null) {
            return Arity.NO_ARGS;
        }

        final SourceSection sourceSection = translate(args.getPosition());

        if (args.getPre() != null) {
            for (org.jruby.ast.Node arg : args.getPre().childNodes()) {
                if (arg instanceof org.jruby.ast.ArgumentNode) {
                    final org.jruby.ast.ArgumentNode argNode = (org.jruby.ast.ArgumentNode) arg;
                    environment.getPreParameters().add(environment.declareVar(argNode.getName()));
                } else if (arg instanceof org.jruby.ast.MultipleAsgnNode) {
                    /*
                     * TODO(cs): I don't know how to handle this yet, so I just do my best to get
                     * the names out and define them so the rest of the parser succeeds.
                     */

                    context.getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, args.getPosition().getFile(), args.getPosition().getStartLine(), "only extracting names from multiple assignment in arguments");

                    final org.jruby.ast.MultipleAsgnNode multAsgn = (org.jruby.ast.MultipleAsgnNode) arg;

                    final List<String> names = new ArrayList<>();
                    getNamesFromMultipleAssignment(multAsgn, names);

                    for (String name : names) {
                        environment.getPreParameters().add(environment.declareVar(name));
                    }
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

                final FrameSlot frameSlot = environment.declareVar(name);
                environment.getOptionalParameters().add(frameSlot);
                environment.getOptionalParametersDefaultValues().put(frameSlot, paramDefaultValue);
            }
        }

        if (args.getPost() != null) {
            for (org.jruby.ast.Node arg : args.getPost().childNodes()) {
                final org.jruby.ast.ArgumentNode argNode = (org.jruby.ast.ArgumentNode) arg;
                environment.getPostParameters().add(environment.declareVar(argNode.getName()));
            }
        }

        if (args.getRestArgNode() != null) {
            final org.jruby.ast.RestArgNode rest = (org.jruby.ast.RestArgNode) args.getRestArgNode();
            environment.setRestParameter(environment.declareVar(rest.getName()));
        }

        if (args.getBlock() != null) {
            final org.jruby.ast.BlockArgNode blockArgNode = args.getBlock();
            final FrameSlot frameSlot = environment.declareVar(blockArgNode.getName());
            environment.setBlockParameter(frameSlot);
        }

        final int minimum = environment.getPreParameters().size() + environment.getPostParameters().size();

        int maximum = minimum + environment.getOptionalParameters().size();

        if (args.getRestArgNode() != null) {
            maximum = Arity.NO_MAXIMUM;
        }

        return new Arity(minimum, maximum);
    }

    private void getNamesFromMultipleAssignment(org.jruby.ast.MultipleAsgnNode multAsgn, List<String> names) {
        for (org.jruby.ast.Node a : multAsgn.getPre().childNodes()) {
            if (a instanceof org.jruby.ast.DAsgnNode) {
                names.add(((org.jruby.ast.DAsgnNode) a).getName());
            } else if (a instanceof org.jruby.ast.MultipleAsgnNode) {
                getNamesFromMultipleAssignment((org.jruby.ast.MultipleAsgnNode) a, names);
            } else if (a instanceof org.jruby.ast.LocalAsgnNode) {
                names.add(((org.jruby.ast.LocalAsgnNode) a).getName());
            } else {
                throw new RuntimeException(a.getClass().getName());
            }
        }
    }

    @Override
    public Object visitSuperNode(org.jruby.ast.SuperNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final ArgumentsAndBlockTranslation argumentsAndBlock = translateArgumentsAndBlock(sourceSection, node.getIterNode(), node.getArgsNode(), null);

        final String name = environment.getMethodName();

        return new GeneralSuperCallNode(context, sourceSection, name, argumentsAndBlock.getBlock(), argumentsAndBlock.getArguments(), argumentsAndBlock.isSplatted());
    }

    @Override
    public Object visitZSuperNode(org.jruby.ast.ZSuperNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final ArgumentsAndBlockTranslation argumentsAndBlock = translateArgumentsAndBlock(sourceSection, node.getIterNode(), null, null);

        final String name = environment.getMethodName();

        return new GeneralSuperCallNode(context, sourceSection, name, argumentsAndBlock.getBlock(), argumentsAndBlock.getArguments(), argumentsAndBlock.isSplatted());
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

}
