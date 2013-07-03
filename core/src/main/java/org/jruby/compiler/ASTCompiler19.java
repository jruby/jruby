/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Charles O Nutter <headius@headius.com>
 *  
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.compiler;

import org.jruby.RubyMatchData;
import org.jruby.ast.ArgsCatNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArgsPushNode;
import org.jruby.ast.ArgumentNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.EncodingNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.Hash19Node;
import org.jruby.ast.Node;
import org.jruby.ast.LambdaNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.Match2CaptureNode;
import org.jruby.ast.MultipleAsgn19Node;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NodeType;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.OptArgNode;
import org.jruby.ast.SValue19Node;
import org.jruby.ast.SplatNode;
import org.jruby.ast.StarNode;
import org.jruby.ast.Yield19Node;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.BlockBody;
import org.jruby.util.DefinedMessage;

/**
 *
 * @author headius
 */
public class ASTCompiler19 extends ASTCompiler {
    @Override
    protected boolean is1_9() {
        return true;
    }
    
    @Override
    public void compile(Node node, BodyCompiler context, boolean expr) {
        if (node == null) {
            if (expr) context.loadNil();
            return;
        }
        switch (node.getNodeType()) {
        case ENCODINGNODE:
            compileEncoding(node, context, expr);
            break;
        case LAMBDANODE:
            compileLambda(node, context, expr);
            break;
        case MULTIPLEASGN19NODE:
            compileMultipleAsgn19(node, context, expr);
            break;
        default:
            super.compile(node, context, expr);
        }
    }

    @Override
    public void compileArgs(Node node, BodyCompiler context, boolean expr) {
        final ArgsNode argsNode = (ArgsNode) node;

        final int required = argsNode.getRequiredArgsCount();
        final int opt = argsNode.getOptionalArgsCount();
        final int rest = argsNode.getRestArg();
        
        context.getVariableCompiler().checkMethodArity(required, opt, rest);
        compileMethodArgs(node, context, expr);
    }

    @Override
    public void compileAssignment(Node node, BodyCompiler context) {
        switch (node.getNodeType()) {
            case MULTIPLEASGN19NODE:
                compileMultipleAsgn19Assignment(node, context, false);
                break;
            default:
                super.compileAssignment(node, context);
        }
    }

    @Override
    protected void compileDefinedAndOrDStrDRegexp(final Node node, BodyCompiler context) {
        context.pushDefinedMessage(DefinedMessage.EXPRESSION);
    }

    @Override
    protected void compileDefinedBackref(final Node node, BodyCompiler context) {
        context.backref();
        context.isInstanceOf(RubyMatchData.class,
                new BranchCallback() {

                    public void branch(BodyCompiler context) {
                        context.pushDefinedMessage(DefinedMessage.GLOBAL_VARIABLE);
                    }
                },
                new BranchCallback() {

                    public void branch(BodyCompiler context) {
                        context.pushNull();
                    }
                });
    }

    @Override
    protected void compileDefinedDVar(final Node node, BodyCompiler context) {
        context.pushDefinedMessage(DefinedMessage.LOCAL_VARIABLE);
    }

    @Override
    protected void compileDefinedNthref(final Node node, BodyCompiler context) {
        context.isCaptured(((NthRefNode) node).getMatchNumber(),
                new BranchCallback() {

                    public void branch(BodyCompiler context) {
                        context.pushDefinedMessage(DefinedMessage.GLOBAL_VARIABLE);
                    }
                },
                new BranchCallback() {

                    public void branch(BodyCompiler context) {
                        context.pushNull();
                    }
                });
    }

    public void compileMethodArgs(Node node, BodyCompiler context, boolean expr) {
        final ArgsNode argsNode = (ArgsNode) node;
        
        if (argsNode.getKeyRest() != null || argsNode.getKeywords() != null) {
            throw new NotCompilableException("keyword args not supported in JIT yet: " + argsNode);
        }

        final int required = argsNode.getRequiredArgsCount();
        final int opt = argsNode.getOptionalArgsCount();
        final int rest = argsNode.getRestArg();

        ArrayCallback requiredAssignment = null;
        ArrayCallback optionalGiven = null;
        ArrayCallback optionalNotGiven = null;
        CompilerCallback restAssignment = null;
        CompilerCallback blockAssignment = null;

        if (required > 0) {
            requiredAssignment = new ArrayCallback() {
                public void nextValue(BodyCompiler context, Object object, int index) {
                    ArrayNode arguments = (ArrayNode)object;
                    Node argNode = arguments.get(index);
                    switch (argNode.getNodeType()) {
                    case ARGUMENTNODE:
                        int varIndex = ((ArgumentNode)argNode).getIndex();
                        context.getVariableCompiler().assignLocalVariable(varIndex, false);
                        break;
                    case MULTIPLEASGN19NODE:
                        compileMultipleAsgn19Assignment(argNode, context, false);
                        break;
                    default:
                        throw new NotCompilableException("unknown argument type: " + argNode);
                    }
                }
            };
        }

        if (opt > 0) {
            optionalGiven = new ArrayCallback() {
            public void nextValue(BodyCompiler context, Object object, int index) {
                    OptArgNode optArg = (OptArgNode)((ListNode) object).get(index);

                    compileAssignment(optArg.getValue(), context);
                }
            };
            optionalNotGiven = new ArrayCallback() {
                public void nextValue(BodyCompiler context, Object object, int index) {
                    OptArgNode optArg = (OptArgNode)((ListNode) object).get(index);

                    compile(optArg.getValue(), context, false);
                }
            };
        }

        if (rest > -1) {
            restAssignment = new CompilerCallback() {
                public void call(BodyCompiler context) {
                    context.getVariableCompiler().assignLocalVariable(argsNode.getRestArg(), false);
                }
            };
        }

        if (argsNode.getBlock() != null) {
            blockAssignment = new CompilerCallback() {
                public void call(BodyCompiler context) {
                    context.getVariableCompiler().assignLocalVariable(argsNode.getBlock().getCount(), false);
                }
            };
        }

        context.getVariableCompiler().assignMethodArguments19(
                argsNode.getPre(),
                argsNode.getPreCount(),
                argsNode.getPost(),
                argsNode.getPostCount(),
                argsNode.getPostIndex(),
                argsNode.getOptArgs(),
                argsNode.getOptionalArgsCount(),
                requiredAssignment,
                optionalGiven,
                optionalNotGiven,
                restAssignment,
                blockAssignment);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    @Override
    public void compileArgsPush(Node node, BodyCompiler context, boolean expr) {
        ArgsPushNode argsPush = (ArgsPushNode) node;

        compile(argsPush.getFirstNode(), context,true);
        compile(argsPush.getSecondNode(), context,true);
        context.argsPush();
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileEncoding(Node node, BodyCompiler context, boolean expr) {
        final EncodingNode encodingNode = (EncodingNode)node;

        if (expr) {
            context.loadEncoding(encodingNode.getEncoding());
        }
    }

    @Override
    public void compileIter(Node node, BodyCompiler context) {
        final IterNode iterNode = (IterNode)node;
        final ArgsNode argsNode = (ArgsNode)iterNode.getVarNode();

        // create the closure class and instantiate it
        final CompilerCallback closureBody = new CompilerCallback() {
            public void call(BodyCompiler context) {
                if (iterNode.getBodyNode() != null) {
                    compile(iterNode.getBodyNode(), context, true);
                } else {
                    context.loadNil();
                }
            }
        };

        // create the closure class and instantiate it
        final CompilerCallback closureArgs = new CompilerCallback() {
            public void call(BodyCompiler context) {
                // FIXME: This is temporary since the variable compilers assume we want
                // args already on stack for assignment. We just pop and continue with
                // 1.9 args logic.
                context.consumeCurrentValue(); // args value
                context.consumeCurrentValue(); // passed block
                if (iterNode.getVarNode() != null) {
                    if (iterNode instanceof LambdaNode) {
                        final int required = argsNode.getRequiredArgsCount();
                        final int opt = argsNode.getOptionalArgsCount();
                        final int rest = argsNode.getRestArg();
                        context.getVariableCompiler().checkMethodArity(required, opt, rest);
                        compileMethodArgs(argsNode, context, true);
                    } else {
                        compileMethodArgs(argsNode, context, true);
                    }
                }
            }
        };

        boolean hasMultipleArgsHead = false;
        if (iterNode.getVarNode() instanceof MultipleAsgnNode) {
            hasMultipleArgsHead = ((MultipleAsgnNode) iterNode.getVarNode()).getHeadNode() != null;
        }

        NodeType argsNodeId = BlockBody.getArgumentTypeWackyHack(iterNode);

        ASTInspector inspector = new ASTInspector();
        inspector.inspect(iterNode.getBodyNode());
        inspector.inspect(iterNode.getVarNode());

        if (argsNodeId == null) {
            // no args, do not pass args processor
            context.createNewClosure19(iterNode.getPosition().getFile(), iterNode.getPosition().getStartLine(), iterNode.getScope(), Arity.procArityOf(iterNode.getVarNode()).getValue(),
                    closureBody, null, hasMultipleArgsHead, argsNodeId, Helpers.encodeParameterList(argsNode), inspector);
        } else {
            context.createNewClosure19(iterNode.getPosition().getFile(), iterNode.getPosition().getStartLine(), iterNode.getScope(), Arity.procArityOf(iterNode.getVarNode()).getValue(),
                    closureBody, closureArgs, hasMultipleArgsHead, argsNodeId, Helpers.encodeParameterList(argsNode), inspector);
        }
    }

    public void compileLambda(Node node, BodyCompiler context, boolean expr) {
        final LambdaNode lambdaNode = (LambdaNode)node;

        if (expr) {
            context.createNewLambda(new CompilerCallback() {
                public void call(BodyCompiler context) {
                    compileIter(lambdaNode, context);
                }
            });
        }
    }

    public void compileMultipleAsgn19(Node node, BodyCompiler context, boolean expr) {
        MultipleAsgn19Node multipleAsgn19Node = (MultipleAsgn19Node) node;

        if (expr) {
            // need the array, use unoptz version
            compileUnoptimizedMultipleAsgn19(multipleAsgn19Node, context, expr);
        } else {
            // try optz version
            compileOptimizedMultipleAsgn19(multipleAsgn19Node, context, expr);
        }
    }

    private void compileOptimizedMultipleAsgn19(MultipleAsgn19Node multipleAsgn19Node, BodyCompiler context, boolean expr) {
        // expect value to be an array of nodes
        if (multipleAsgn19Node.getValueNode() instanceof ArrayNode) {
            // head must not be null and there must be no "args" (like *arg)
            if (multipleAsgn19Node.getPreCount() > 0 && multipleAsgn19Node.getPostCount() == 0 && multipleAsgn19Node.getRest() == null) {
                // sizes must match
                if (multipleAsgn19Node.getPreCount() == ((ArrayNode)multipleAsgn19Node.getValueNode()).size()) {
                    // "head" must have no non-trivial assigns (array groupings, basically)
                    boolean normalAssigns = true;
                    for (Node asgn : multipleAsgn19Node.getPre().childNodes()) {
                        if (asgn instanceof ListNode) {
                            normalAssigns = false;
                            break;
                        }
                    }

                    if (normalAssigns) {
                        // only supports simple parallel assignment of up to 4 values to the same number of assignees
                        int size = multipleAsgn19Node.getPreCount();
                        if (size >= 2 && size <= 10) {
                            ArrayNode values = (ArrayNode)multipleAsgn19Node.getValueNode();
                            for (Node value : values.childNodes()) {
                                compile(value, context, true);
                            }
                            context.reverseValues(size);
                            for (Node asgn : multipleAsgn19Node.getPre().childNodes()) {
                                compileAssignment(asgn, context);
                            }
                            return;
                        }
                    }
                }
            }
        }

        // if we get here, no optz cases work; fall back on unoptz.
        compileUnoptimizedMultipleAsgn19(multipleAsgn19Node, context, expr);
    }

    private void compileUnoptimizedMultipleAsgn19(MultipleAsgn19Node multipleAsgn19Node, BodyCompiler context, boolean expr) {
        compile(multipleAsgn19Node.getValueNode(), context, true);

        compileMultipleAsgn19Assignment(multipleAsgn19Node, context, expr);
    }

    public void compileMultipleAsgn19Assignment(Node node, BodyCompiler context, boolean expr) {
        final MultipleAsgn19Node multipleAsgn19Node = (MultipleAsgn19Node) node;

        // normal items at the front or back of the masgn
        ArrayCallback prePostAssignCallback = new ArrayCallback() {

                    public void nextValue(BodyCompiler context, Object sourceArray,
                            int index) {
                        ListNode nodes = (ListNode) sourceArray;
                        Node assignNode = nodes.get(index);

                        // perform assignment for the next node
                        compileAssignment(assignNode, context);
                    }
                };

        CompilerCallback restCallback = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        Node argsNode = multipleAsgn19Node.getRest();
                        if (argsNode instanceof StarNode) {
                            // done processing args
                            context.consumeCurrentValue();
                        } else {
                            // assign to appropriate variable
                            compileAssignment(argsNode, context);
                        }
                    }
                };

        if (multipleAsgn19Node.getPreCount() == 0 && multipleAsgn19Node.getPostCount() == 0) {
            if (multipleAsgn19Node.getRest() == null) {
                throw new NotCompilableException("Something's wrong, multiple assignment with no head or args at: " + multipleAsgn19Node.getPosition());
            } else {
                if (multipleAsgn19Node.getRest() instanceof StarNode) {
                    // do nothing
                } else {
                    context.ensureMultipleAssignableRubyArray(multipleAsgn19Node.getPreCount() != 0 || multipleAsgn19Node.getPostCount() != 0);

                    context.forEachInValueArray(0, 0, null, null, restCallback);
                }
            }
        } else {
            context.ensureMultipleAssignableRubyArray(multipleAsgn19Node.getPreCount() != 0 || multipleAsgn19Node.getPostCount() != 0);

            if (multipleAsgn19Node.getRest() == null) {
                context.forEachInValueArray(0, multipleAsgn19Node.getPreCount(), multipleAsgn19Node.getPre(), multipleAsgn19Node.getPostCount(), multipleAsgn19Node.getPost(), prePostAssignCallback, null);
            } else {
                context.forEachInValueArray(0, multipleAsgn19Node.getPreCount(), multipleAsgn19Node.getPre(), multipleAsgn19Node.getPostCount(), multipleAsgn19Node.getPost(), prePostAssignCallback, restCallback);
            }
        }
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }
    
    @Override
    public void compileHash(Node node, BodyCompiler context, boolean expr) {
        compileHashCommon((Hash19Node) node, context, expr);
    }
    
    @Override
    protected void createNewHash(BodyCompiler context, HashNode hashNode, ArrayCallback hashCallback) {
        context.createNewHash19(hashNode.getListNode(), hashCallback, hashNode.getListNode().size() / 2);
    }

    public void compileMatch2(Node node, BodyCompiler context, boolean expr) {
        if (!(node instanceof Match2CaptureNode)) {
            super.compileMatch2(node, context, expr);
            return;
        }

        // match with capture logic
        final Match2CaptureNode matchNode = (Match2CaptureNode) node;

        compile(matchNode.getReceiverNode(), context,true);
        CompilerCallback value = new CompilerCallback() {
            public void call(BodyCompiler context) {
                compile(matchNode.getValueNode(), context,true);
            }
        };

        context.match2Capture(value, matchNode.getScopeOffsets(), true);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    @Override
    public void compileSValue(Node node, BodyCompiler context, boolean expr) {
        SValue19Node svalueNode = (SValue19Node)node;

        compile(svalueNode.getValue(), context,true);

        context.singlifySplattedValue19();
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    @Override
    protected void splatCurrentValue(BodyCompiler context) {
        context.splatCurrentValue("splatValue19");
    }

    @Override
    public void compileArgsCatArguments(Node node, BodyCompiler context, boolean expr) {
        ArgsCatNode argsCatNode = (ArgsCatNode) node;

        // arguments compilers always create IRubyObject[], but since we then combine
        // with another IRubyObject[] from coercing second node to array, this can
        // be inefficient. Escape analysis may help, though.
        compileArguments(argsCatNode.getFirstNode(), context);
        compile(argsCatNode.getSecondNode(), context,true);
        context.argsCatToArguments19();
        
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    @Override
    public void compileSplatArguments(Node node, BodyCompiler context, boolean expr) {
        SplatNode splatNode = (SplatNode) node;

        compile(splatNode.getValue(), context,true);
        context.splatToArguments19();
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileDRegexp(Node node, BodyCompiler context, boolean expr) {
        final DRegexpNode dregexpNode = (DRegexpNode) node;

        ArrayCallback dElementsCallback = new ArrayCallback() {
            public void nextValue(BodyCompiler context, Object sourceArray, int index) {
                compile((Node)((Object[])sourceArray)[index], context, true);
            }
        };

        if (expr) {
            context.createDRegexp19(dElementsCallback, dregexpNode.childNodes().toArray(), dregexpNode.getOptions().toEmbeddedOptions());
        } else {
            // not an expression, only compile the elements
            for (Node nextNode : dregexpNode.childNodes()) {
                compile(nextNode, context, false);
            }
        }
    }

    @Override
    public void compileYield(Node node, BodyCompiler context, boolean expr) {
        if (!(node instanceof Yield19Node)) {
            super.compileYield(node, context, expr);
            return;
        }
        final Yield19Node yieldNode = (Yield19Node) node;

        CompilerCallback argsCallback = new CompilerCallback() {
            public void call(BodyCompiler context) {
                compile(yieldNode.getArgsNode(), context,true);
            }
        };

        boolean unsplat = false;

        switch (yieldNode.getArgsNode().getNodeType()) {
            case ARGSPUSHNODE:
            case ARGSCATNODE:
            case SPLATNODE:
                unsplat = true;
                break;
        }

        context.getInvocationCompiler().yield19(argsCallback, unsplat);

        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }
}
