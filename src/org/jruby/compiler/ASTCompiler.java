/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.compiler;

import java.util.Iterator;
import org.jruby.RubyInstanceConfig;
import org.jruby.ast.AliasNode;
import org.jruby.ast.AndNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.AttrAssignNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BignumNode;
import org.jruby.ast.BinaryOperatorNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.BreakNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.Colon3Node;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.ConstNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.DVarNode;
import org.jruby.ast.DefinedNode;
import org.jruby.ast.DefnNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.EnsureNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FloatNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.IfNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.Match2Node;
import org.jruby.ast.Match3Node;
import org.jruby.ast.MatchNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.Node;
import org.jruby.ast.NodeType;
import org.jruby.ast.NotNode;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OrNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.RootNode;
import org.jruby.ast.SValueNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SuperNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.YieldNode;
import org.jruby.runtime.Arity;
import org.jruby.runtime.CallType;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.RubyMatchData;
import org.jruby.ast.ArgsCatNode;
import org.jruby.ast.ArgsPushNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.CaseNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.DSymbolNode;
import org.jruby.ast.DXStrNode;
import org.jruby.ast.DefsNode;
import org.jruby.ast.FlipNode;
import org.jruby.ast.ForNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.PreExeNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.StarNode;
import org.jruby.ast.ToAryNode;
import org.jruby.ast.UndefNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.WhenNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public class ASTCompiler {
    private boolean isAtRoot = true;
    
    public void compile(Node node, MethodCompiler context) {
        if (node == null) {
            context.loadNil();
            return;
        }
        switch (node.nodeId) {
            case ALIASNODE:
                compileAlias(node, context);
                break;
            case ANDNODE:
                compileAnd(node, context);
                break;
            case ARGSCATNODE:
                compileArgsCat(node, context);
                break;
            case ARGSPUSHNODE:
                compileArgsPush(node, context);
                break;
            case ARRAYNODE:
                compileArray(node, context);
                break;
            case ATTRASSIGNNODE:
                compileAttrAssign(node, context);
                break;
            case BACKREFNODE:
                compileBackref(node, context);
                break;
            case BEGINNODE:
                compileBegin(node, context);
                break;
            case BIGNUMNODE:
                compileBignum(node, context);
                break;
            case BLOCKNODE:
                compileBlock(node, context);
                break;
            case BREAKNODE:
                compileBreak(node, context);
                break;
            case CALLNODE:
                compileCall(node, context);
                break;
            case CASENODE:
                compileCase(node, context);
                break;
            case CLASSNODE:
                compileClass(node, context);
                break;
            case CLASSVARNODE:
                compileClassVar(node, context);
                break;
            case CLASSVARASGNNODE:
                compileClassVarAsgn(node, context);
                break;
            case CLASSVARDECLNODE:
                compileClassVarDecl(node, context);
                break;
            case COLON2NODE:
                compileColon2(node, context);
                break;
            case COLON3NODE:
                compileColon3(node, context);
                break;
            case CONSTDECLNODE:
                compileConstDecl(node, context);
                break;
            case CONSTNODE:
                compileConst(node, context);
                break;
            case DASGNNODE:
                compileDAsgn(node, context);
                break;
            case DEFINEDNODE:
                compileDefined(node, context);
                break;
            case DEFNNODE:
                compileDefn(node, context);
                break;
            case DEFSNODE:
                compileDefs(node, context);
                break;
            case DOTNODE:
                compileDot(node, context);
                break;
            case DREGEXPNODE:
                compileDRegexp(node, context);
                break;
            case DSTRNODE:
                compileDStr(node, context);
                break;
            case DSYMBOLNODE:
                compileDSymbol(node, context);
                break;
            case DVARNODE:
                compileDVar(node, context);
                break;
            case DXSTRNODE:
                compileDXStr(node, context);
                break;
            case ENSURENODE:
                compileEnsureNode(node, context);
                break;
            case EVSTRNODE:
                compileEvStr(node, context);
                break;
            case FALSENODE:
                compileFalse(node, context);
                break;
            case FCALLNODE:
                compileFCall(node, context);
                break;
            case FIXNUMNODE:
                compileFixnum(node, context);
                break;
            case FLIPNODE:
                compileFlip(node, context);
                break;
            case FLOATNODE:
                compileFloat(node, context);
                break;
            case FORNODE:
                compileFor(node, context);
                break;
            case GLOBALASGNNODE:
                compileGlobalAsgn(node, context);
                break;
            case GLOBALVARNODE:
                compileGlobalVar(node, context);
                break;
            case HASHNODE:
                compileHash(node, context);
                break;
            case IFNODE:
                compileIf(node, context);
                break;
            case INSTASGNNODE:
                compileInstAsgn(node, context);
                break;
            case INSTVARNODE:
                compileInstVar(node, context);
                break;
            case ITERNODE:
                compileIter(node, context);
                break;
            case LOCALASGNNODE:
                compileLocalAsgn(node, context);
                break;
            case LOCALVARNODE:
                compileLocalVar(node, context);
                break;
            case MATCH2NODE:
                compileMatch2(node, context);
                break;
            case MATCH3NODE:
                compileMatch3(node, context);
                break;
            case MATCHNODE:
                compileMatch(node, context);
                break;
            case MODULENODE:
                compileModule(node, context);
                break;
            case MULTIPLEASGNNODE:
                compileMultipleAsgn(node, context);
                break;
            case NEWLINENODE:
                compileNewline(node, context);
                break;
            case NEXTNODE:
                compileNext(node, context);
                break;
            case NTHREFNODE:
                compileNthRef(node, context);
                break;
            case NILNODE:
                compileNil(node, context);
                break;
            case NOTNODE:
                compileNot(node, context);
                break;
            case OPASGNANDNODE:
                compileOpAsgnAnd(node, context);
                break;
            case OPASGNNODE:
                compileOpAsgn(node, context);
                break;
            case OPASGNORNODE:
                compileOpAsgnOr(node, context);
                break;
            case OPELEMENTASGNNODE:
                compileOpElementAsgn(node, context);
                break;
            case ORNODE:
                compileOr(node, context);
                break;
            case POSTEXENODE:
                compilePostExe(node, context);
                break;
            case PREEXENODE:
                compilePreExe(node, context);
                break;
            case REDONODE:
                compileRedo(node, context);
                break;
            case REGEXPNODE:
                compileRegexp(node, context);
                break;
            case RESCUEBODYNODE:
                throw new NotCompilableException("rescue body is handled by rescue compilation at: " + node.getPosition());
            case RESCUENODE:
                compileRescue(node, context);
                break;
            case RETRYNODE:
                compileRetry(node, context);
                break;
            case RETURNNODE:
                compileReturn(node, context);
                break;
            case ROOTNODE:
                throw new NotCompilableException("Use compileRoot(); Root node at: " + node.getPosition());
            case SCLASSNODE:
                compileSClass(node, context);
                break;
            case SELFNODE:
                compileSelf(node, context);
                break;
            case SPLATNODE:
                compileSplat(node, context);
                break;
            case STRNODE:
                compileStr(node, context);
                break;
            case SUPERNODE:
                compileSuper(node, context);
                break;
            case SVALUENODE:
                compileSValue(node, context);
                break;
            case SYMBOLNODE:
                compileSymbol(node, context);
                break;
            case TOARYNODE:
                compileToAry(node, context);
                break;
            case TRUENODE:
                compileTrue(node, context);
                break;
            case UNDEFNODE:
                compileUndef(node, context);
                break;
            case UNTILNODE:
                compileUntil(node, context);
                break;
            case VALIASNODE:
                compileVAlias(node, context);
                break;
            case VCALLNODE:
                compileVCall(node, context);
                break;
            case WHILENODE:
                compileWhile(node, context);
                break;
            case WHENNODE:
                assert false : "When nodes are handled by case node compilation.";
                break;
            case XSTRNODE:
                compileXStr(node, context);
                break;
            case YIELDNODE:
                compileYield(node, context);
                break;
            case ZARRAYNODE:
                compileZArray(node, context);
                break;
            case ZSUPERNODE:
                compileZSuper(node, context);
                break;
            default:
                assert false : "Unknown node encountered in compiler: " + node;
        }
    }

    public void compileArguments(Node node, MethodCompiler context) {
        switch (node.nodeId) {
            case ARGSCATNODE:
                compileArgsCatArguments(node, context);
                break;
            case ARGSPUSHNODE:
                compileArgsPushArguments(node, context);
                break;
            case ARRAYNODE:
                compileArrayArguments(node, context);
                break;
            case SPLATNODE:
                compileSplatArguments(node, context);
                break;
            default:
                compile(node, context);
                context.convertToJavaArray();
        }
    }
    
    public class VariableArityArguments implements ArgumentsCallback {
        private Node node;
        
        public VariableArityArguments(Node node) {
            this.node = node;
        }
        
        public int getArity() {
            return -1;
        }
        
        public void call(MethodCompiler context) {
            compileArguments(node, context);
        }
    }
    
    public class SpecificArityArguments implements ArgumentsCallback {
        private int arity;
        private Node node;
        
        public SpecificArityArguments(Node node) {
            if (node.nodeId == NodeType.ARRAYNODE && ((ArrayNode)node).isLightweight()) {
                // only arrays that are "lightweight" are being used as args arrays
                this.arity = ((ArrayNode)node).size();
            } else {
                // otherwise, it's a literal array
                this.arity = 1;
            }
            this.node = node;
        }
        
        public int getArity() {
            return arity;
        }
        
        public void call(MethodCompiler context) {
            if (node.nodeId == NodeType.ARRAYNODE) {
                ArrayNode arrayNode = (ArrayNode)node;
                if (arrayNode.isLightweight()) {
                    // explode array, it's an internal "args" array
                    for (Node n : arrayNode.childNodes()) {
                        compile(n, context);
                    }
                } else {
                    // use array as-is, it's a literal array
                    compile(arrayNode, context);
                }
            } else {
                compile(node, context);
            }
        }
    }

    public ArgumentsCallback getArgsCallback(Node node) {
        if (node == null) {
            return null;
        }
        // unwrap newline nodes to get their actual type
        while (node.nodeId == NodeType.NEWLINENODE) {
            node = ((NewlineNode)node).getNextNode();
        }
        switch (node.nodeId) {
            case ARGSCATNODE:
            case ARGSPUSHNODE:
            case SPLATNODE:
                return new VariableArityArguments(node);
            case ARRAYNODE:
                ArrayNode arrayNode = (ArrayNode)node;
                if (arrayNode.size() == 0) {
                    return null;
                } else if (arrayNode.size() > 3) {
                    return new VariableArityArguments(node);
                } else {
                    return new SpecificArityArguments(node);
                }
            default:
                return new SpecificArityArguments(node);
        }
    }

    public void compileAssignment(Node node, MethodCompiler context) {
        switch (node.nodeId) {
            case ATTRASSIGNNODE:
                compileAttrAssignAssignment(node, context);
                break;
            case DASGNNODE:
                compileDAsgnAssignment(node, context);
                break;
            case CLASSVARASGNNODE:
                compileClassVarAsgnAssignment(node, context);
                break;
            case CLASSVARDECLNODE:
                compileClassVarDeclAssignment(node, context);
                break;
            case CONSTDECLNODE:
                compileConstDeclAssignment(node, context);
                break;
            case GLOBALASGNNODE:
                compileGlobalAsgnAssignment(node, context);
                break;
            case INSTASGNNODE:
                compileInstAsgnAssignment(node, context);
                break;
            case LOCALASGNNODE:
                compileLocalAsgnAssignment(node, context);
                break;
            case MULTIPLEASGNNODE:
                compileMultipleAsgnAssignment(node, context);
                break;
            case ZEROARGNODE:
                throw new NotCompilableException("Shouldn't get here; zeroarg does not do assignment: " + node);
            default:
                throw new NotCompilableException("Can't compile assignment node: " + node);
        }
    }

    public static YARVNodesCompiler getYARVCompiler() {
        return new YARVNodesCompiler();
    }

    public void compileAlias(Node node, MethodCompiler context) {
        final AliasNode alias = (AliasNode) node;

        context.defineAlias(alias.getNewName(), alias.getOldName());
    }

    public void compileAnd(Node node, MethodCompiler context) {
        final AndNode andNode = (AndNode) node;

        compile(andNode.getFirstNode(), context);

        BranchCallback longCallback = new BranchCallback() {

                    public void branch(MethodCompiler context) {
                        compile(andNode.getSecondNode(), context);
                    }
                };

        context.performLogicalAnd(longCallback);
    }

    public void compileArray(Node node, MethodCompiler context) {
        ArrayNode arrayNode = (ArrayNode) node;

        ArrayCallback callback = new ArrayCallback() {

                    public void nextValue(MethodCompiler context, Object sourceArray, int index) {
                        Node node = (Node) ((Object[]) sourceArray)[index];
                        compile(node, context);
                    }
                };

        context.createNewArray(arrayNode.childNodes().toArray(), callback, arrayNode.isLightweight());
    }

    public void compileArgsCat(Node node, MethodCompiler context) {
        ArgsCatNode argsCatNode = (ArgsCatNode) node;

        compile(argsCatNode.getFirstNode(), context);
        context.ensureRubyArray();
        compile(argsCatNode.getSecondNode(), context);
        context.splatCurrentValue();
        context.concatArrays();
    }

    public void compileArgsPush(Node node, MethodCompiler context) {
        ArgsPushNode argsPush = (ArgsPushNode) node;

        compile(argsPush.getFirstNode(), context);
        compile(argsPush.getSecondNode(), context);
        context.concatArrays();
    }

    private void compileAttrAssign(Node node, MethodCompiler context) {
        final AttrAssignNode attrAssignNode = (AttrAssignNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {
            public void call(MethodCompiler context) {
                compile(attrAssignNode.getReceiverNode(), context);
            }
        };
        
        ArgumentsCallback argsCallback = getArgsCallback(attrAssignNode.getArgsNode());

        context.getInvocationCompiler().invokeAttrAssign(attrAssignNode.getName(), receiverCallback, argsCallback);
    }

    public void compileAttrAssignAssignment(Node node, MethodCompiler context) {
        AttrAssignNode attrAssignNode = (AttrAssignNode) node;

        compile(attrAssignNode.getReceiverNode(), context);
        context.swapValues();
        if (attrAssignNode.getArgsNode() != null) {
            compileArguments(attrAssignNode.getArgsNode(), context);
            context.swapValues();
            context.appendToObjectArray();
        } else {
            context.createObjectArray(1);
        }

        // FIXME: This is still using the grossly inefficient version of attr assignment
        // but it's used only for masgn/block args so it's fairly rare
        context.getInvocationCompiler().invokeAttrAssign(attrAssignNode.getName());
    }

    public void compileBackref(Node node, MethodCompiler context) {
        BackRefNode iVisited = (BackRefNode) node;

        context.performBackref(iVisited.getType());
    }

    public void compileBegin(Node node, MethodCompiler context) {
        BeginNode beginNode = (BeginNode) node;

        compile(beginNode.getBodyNode(), context);
    }

    public void compileBignum(Node node, MethodCompiler context) {
        context.createNewBignum(((BignumNode) node).getValue());
    }

    public void compileBlock(Node node, MethodCompiler context) {
        BlockNode blockNode = (BlockNode) node;

        for (Iterator<Node> iter = blockNode.childNodes().iterator(); iter.hasNext();) {
            Node n = iter.next();

            compile(n, context);

            if (iter.hasNext()) {
                // clear result from previous line
                context.consumeCurrentValue();
            }
        }
    }

    public void compileBreak(Node node, MethodCompiler context) {
        final BreakNode breakNode = (BreakNode) node;

        CompilerCallback valueCallback = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        if (breakNode.getValueNode() != null) {
                            compile(breakNode.getValueNode(), context);
                        } else {
                            context.loadNil();
                        }
                    }
                };

        context.issueBreakEvent(valueCallback);
    }

    public void compileCall(Node node, MethodCompiler context) {
        final CallNode callNode = (CallNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {
            public void call(MethodCompiler context) {
                compile(callNode.getReceiverNode(), context);
            }
        };

        ArgumentsCallback argsCallback = getArgsCallback(callNode.getArgsNode());
        CompilerCallback closureArg = getBlock(callNode.getIterNode());

        context.getInvocationCompiler().invokeDynamic(callNode.getName(), receiverCallback, argsCallback, CallType.NORMAL, closureArg, callNode.getIterNode() instanceof IterNode);
    }

    public void compileCase(Node node, MethodCompiler context) {
        CaseNode caseNode = (CaseNode) node;

        boolean hasCase = false;
        if (caseNode.getCaseNode() != null) {
            compile(caseNode.getCaseNode(), context);
            hasCase = true;
        }

        context.pollThreadEvents();

        Node firstWhenNode = caseNode.getFirstWhenNode();
        compileWhen(firstWhenNode, context, hasCase);
    }

    public void compileWhen(Node node, MethodCompiler context, final boolean hasCase) {
        if (node == null) {
            // reached the end of the when chain, pop the case (if provided) and we're done
            if (hasCase) {
                context.consumeCurrentValue();
            }
            context.loadNil();
            return;
        }

        if (!(node instanceof WhenNode)) {
            if (hasCase) {
                // case value provided and we're going into "else"; consume it.
                context.consumeCurrentValue();
            }
            compile(node, context);
            return;
        }

        WhenNode whenNode = (WhenNode) node;

        if (whenNode.getExpressionNodes() instanceof ArrayNode) {
            ArrayNode arrayNode = (ArrayNode) whenNode.getExpressionNodes();

            compileMultiArgWhen(whenNode, arrayNode, 0, context, hasCase);
        } else {
            if (hasCase) {
                context.duplicateCurrentValue();
            }

            // evaluate the when argument
            compile(whenNode.getExpressionNodes(), context);

            final WhenNode currentWhen = whenNode;

            if (hasCase) {
                // we have a case value, call === on the condition value passing the case value
                context.swapValues();
                context.getInvocationCompiler().invokeEqq();
            }

            // check if the condition result is true, branch appropriately
            BranchCallback trueBranch = new BranchCallback() {

                        public void branch(MethodCompiler context) {
                            // consume extra case value, we won't need it anymore
                            if (hasCase) {
                                context.consumeCurrentValue();
                            }

                            if (currentWhen.getBodyNode() != null) {
                                compile(currentWhen.getBodyNode(), context);
                            } else {
                                context.loadNil();
                            }
                        }
                    };

            BranchCallback falseBranch = new BranchCallback() {

                        public void branch(MethodCompiler context) {
                            // proceed to the next when
                            compileWhen(currentWhen.getNextCase(), context, hasCase);
                        }
                    };

            context.performBooleanBranch(trueBranch, falseBranch);
        }
    }

    public void compileMultiArgWhen(final WhenNode whenNode, final ArrayNode expressionsNode, final int conditionIndex, MethodCompiler context, final boolean hasCase) {

        if (conditionIndex >= expressionsNode.size()) {
            // done with conditions, continue to next when in the chain
            compileWhen(whenNode.getNextCase(), context, hasCase);
            return;
        }

        Node tag = expressionsNode.get(conditionIndex);

        context.setLinePosition(tag.getPosition());

        // reduce the when cases to a true or false ruby value for the branch below
        if (tag instanceof WhenNode) {
            // prepare to handle the when logic
            if (hasCase) {
                context.duplicateCurrentValue();
            } else {
                context.loadNull();
            }
            compile(((WhenNode) tag).getExpressionNodes(), context);
            context.checkWhenWithSplat();
        } else {
            if (hasCase) {
                context.duplicateCurrentValue();
            }

            // evaluate the when argument
            compile(tag, context);

            if (hasCase) {
                // we have a case value, call === on the condition value passing the case value
                context.swapValues();
                context.getInvocationCompiler().invokeEqq();
            }
        }

        // check if the condition result is true, branch appropriately
        BranchCallback trueBranch = new BranchCallback() {

                    public void branch(MethodCompiler context) {
                        // consume extra case value, we won't need it anymore
                        if (hasCase) {
                            context.consumeCurrentValue();
                        }

                        if (whenNode.getBodyNode() != null) {
                            compile(whenNode.getBodyNode(), context);
                        } else {
                            context.loadNil();
                        }
                    }
                };

        BranchCallback falseBranch = new BranchCallback() {

                    public void branch(MethodCompiler context) {
                        // proceed to the next when
                        compileMultiArgWhen(whenNode, expressionsNode, conditionIndex + 1, context, hasCase);
                    }
                };

        context.performBooleanBranch(trueBranch, falseBranch);
    }

    public void compileClass(Node node, MethodCompiler context) {
        final ClassNode classNode = (ClassNode) node;

        final Node superNode = classNode.getSuperNode();

        final Node cpathNode = classNode.getCPath();

        CompilerCallback superCallback = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        compile(superNode, context);
                    }
                };
        if (superNode == null) {
            superCallback = null;
        }

        CompilerCallback bodyCallback = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        boolean oldIsAtRoot = isAtRoot;
                        isAtRoot = false;
                        if (classNode.getBodyNode() != null) {
                            compile(classNode.getBodyNode(), context);
                        } else {
                            context.loadNil();
                        }
                        isAtRoot = oldIsAtRoot;
                    }
                };

        CompilerCallback pathCallback = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        if (cpathNode instanceof Colon2Node) {
                            Node leftNode = ((Colon2Node) cpathNode).getLeftNode();
                            if (leftNode != null) {
                                compile(leftNode, context);
                            } else {
                                context.loadNil();
                            }
                        } else if (cpathNode instanceof Colon3Node) {
                            context.loadObject();
                        } else {
                            context.loadNil();
                        }
                    }
                };

        context.defineClass(classNode.getCPath().getName(), classNode.getScope(), superCallback, pathCallback, bodyCallback, null);
    }

    public void compileSClass(Node node, MethodCompiler context) {
        final SClassNode sclassNode = (SClassNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        compile(sclassNode.getReceiverNode(), context);
                    }
                };

        CompilerCallback bodyCallback = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        boolean oldIsAtRoot = isAtRoot;
                        isAtRoot = false;
                        if (sclassNode.getBodyNode() != null) {
                            compile(sclassNode.getBodyNode(), context);
                        } else {
                            context.loadNil();
                        }
                        isAtRoot = oldIsAtRoot;
                    }
                };

        context.defineClass("SCLASS", sclassNode.getScope(), null, null, bodyCallback, receiverCallback);
    }

    public void compileClassVar(Node node, MethodCompiler context) {
        ClassVarNode classVarNode = (ClassVarNode) node;

        context.retrieveClassVariable(classVarNode.getName());
    }

    public void compileClassVarAsgn(Node node, MethodCompiler context) {
        final ClassVarAsgnNode classVarAsgnNode = (ClassVarAsgnNode) node;

        CompilerCallback value = new CompilerCallback() {
            public void call(MethodCompiler context) {
                compile(classVarAsgnNode.getValueNode(), context);
            }
        };

        context.assignClassVariable(classVarAsgnNode.getName(), value);
    }

    public void compileClassVarAsgnAssignment(Node node, MethodCompiler context) {
        ClassVarAsgnNode classVarAsgnNode = (ClassVarAsgnNode) node;

        context.assignClassVariable(classVarAsgnNode.getName());
    }

    public void compileClassVarDecl(Node node, MethodCompiler context) {
        final ClassVarDeclNode classVarDeclNode = (ClassVarDeclNode) node;

        CompilerCallback value = new CompilerCallback() {
            public void call(MethodCompiler context) {
                compile(classVarDeclNode.getValueNode(), context);
            }
        };
        
        context.declareClassVariable(classVarDeclNode.getName(), value);
    }

    public void compileClassVarDeclAssignment(Node node, MethodCompiler context) {
        ClassVarDeclNode classVarDeclNode = (ClassVarDeclNode) node;

        context.declareClassVariable(classVarDeclNode.getName());
    }

    public void compileConstDecl(Node node, MethodCompiler context) {
        // TODO: callback for value would be more efficient, but unlikely to be a big cost (constants are rarely assigned)
        ConstDeclNode constDeclNode = (ConstDeclNode) node;
        Node constNode = constDeclNode.getConstNode();

        if (constNode == null) {
            compile(constDeclNode.getValueNode(), context);

            context.assignConstantInCurrent(constDeclNode.getName());
        } else if (constNode.nodeId == NodeType.COLON2NODE) {
            compile(((Colon2Node) constNode).getLeftNode(), context);
            compile(constDeclNode.getValueNode(), context);

            context.assignConstantInModule(constDeclNode.getName());
        } else {// colon3, assign in Object
            compile(constDeclNode.getValueNode(), context);

            context.assignConstantInObject(constDeclNode.getName());
        }
    }

    public void compileConstDeclAssignment(Node node, MethodCompiler context) {
        // TODO: callback for value would be more efficient, but unlikely to be a big cost (constants are rarely assigned)
        ConstDeclNode constDeclNode = (ConstDeclNode) node;
        Node constNode = constDeclNode.getConstNode();

        if (constNode == null) {
            context.assignConstantInCurrent(constDeclNode.getName());
        } else if (constNode.nodeId == NodeType.COLON2NODE) {
            compile(((Colon2Node) constNode).getLeftNode(), context);
            context.swapValues();
            context.assignConstantInModule(constDeclNode.getName());
        } else {// colon3, assign in Object
            context.assignConstantInObject(constDeclNode.getName());
        }
    }

    public void compileConst(Node node, MethodCompiler context) {
        ConstNode constNode = (ConstNode) node;

        context.retrieveConstant(constNode.getName());
    }

    public void compileColon2(Node node, MethodCompiler context) {
        final Colon2Node iVisited = (Colon2Node) node;
        Node leftNode = iVisited.getLeftNode();
        final String name = iVisited.getName();

        if (leftNode == null) {
            context.loadObject();
            context.retrieveConstantFromModule(name);
        } else {
            final CompilerCallback receiverCallback = new CompilerCallback() {

                        public void call(MethodCompiler context) {
                            compile(iVisited.getLeftNode(), context);
                        }
                    };

            BranchCallback moduleCallback = new BranchCallback() {

                        public void branch(MethodCompiler context) {
                            receiverCallback.call(context);
                            context.retrieveConstantFromModule(name);
                        }
                    };

            BranchCallback notModuleCallback = new BranchCallback() {

                        public void branch(MethodCompiler context) {
                            context.getInvocationCompiler().invokeDynamic(name, receiverCallback, null, CallType.FUNCTIONAL, null, false);
                        }
                    };

            context.branchIfModule(receiverCallback, moduleCallback, notModuleCallback);
        }
    }

    public void compileColon3(Node node, MethodCompiler context) {
        Colon3Node iVisited = (Colon3Node) node;
        String name = iVisited.getName();

        context.loadObject();
        context.retrieveConstantFromModule(name);
    }

    public void compileGetDefinitionBase(final Node node, MethodCompiler context) {
        BranchCallback reg = new BranchCallback() {

                    public void branch(MethodCompiler context) {
                        context.inDefined();
                        compileGetDefinition(node, context);
                    }
                };
        BranchCallback out = new BranchCallback() {

                    public void branch(MethodCompiler context) {
                        context.outDefined();
                    }
                };
        context.protect(reg, out, String.class);
    }

    public void compileDefined(final Node node, MethodCompiler context) {
        compileGetDefinitionBase(((DefinedNode) node).getExpressionNode(), context);
        context.stringOrNil();
    }

    public void compileGetArgumentDefinition(final Node node, MethodCompiler context, String type) {
        if (node == null) {
            context.pushString(type);
        } else if (node instanceof ArrayNode) {
            Object endToken = context.getNewEnding();
            for (int i = 0; i < ((ArrayNode) node).size(); i++) {
                Node iterNode = ((ArrayNode) node).get(i);
                compileGetDefinition(iterNode, context);
                context.ifNull(endToken);
            }
            context.pushString(type);
            Object realToken = context.getNewEnding();
            context.go(realToken);
            context.setEnding(endToken);
            context.pushNull();
            context.setEnding(realToken);
        } else {
            compileGetDefinition(node, context);
            Object endToken = context.getNewEnding();
            context.ifNull(endToken);
            context.pushString(type);
            Object realToken = context.getNewEnding();
            context.go(realToken);
            context.setEnding(endToken);
            context.pushNull();
            context.setEnding(realToken);
        }
    }

    public void compileGetDefinition(final Node node, MethodCompiler context) {
        switch (node.nodeId) {
            case CLASSVARASGNNODE:
            case CLASSVARDECLNODE:
            case CONSTDECLNODE:
            case DASGNNODE:
            case GLOBALASGNNODE:
            case LOCALASGNNODE:
            case MULTIPLEASGNNODE:
            case OPASGNNODE:
            case OPELEMENTASGNNODE:
                context.pushString("assignment");
                break;
            case BACKREFNODE:
                context.backref();
                context.isInstanceOf(RubyMatchData.class,
                        new BranchCallback() {

                            public void branch(MethodCompiler context) {
                                context.pushString("$" + ((BackRefNode) node).getType());
                            }
                        },
                        new BranchCallback() {

                            public void branch(MethodCompiler context) {
                                context.pushNull();
                            }
                        });
                break;
            case DVARNODE:
                context.pushString("local-variable(in-block)");
                break;
            case FALSENODE:
                context.pushString("false");
                break;
            case TRUENODE:
                context.pushString("true");
                break;
            case LOCALVARNODE:
                context.pushString("local-variable");
                break;
            case MATCH2NODE:
            case MATCH3NODE:
                context.pushString("method");
                break;
            case NILNODE:
                context.pushString("nil");
                break;
            case NTHREFNODE:
                context.isCaptured(((NthRefNode) node).getMatchNumber(),
                        new BranchCallback() {

                            public void branch(MethodCompiler context) {
                                context.pushString("$" + ((NthRefNode) node).getMatchNumber());
                            }
                        },
                        new BranchCallback() {

                            public void branch(MethodCompiler context) {
                                context.pushNull();
                            }
                        });
                break;
            case SELFNODE:
                context.pushString("self");
                break;
            case VCALLNODE:
                context.loadSelf();
                context.isMethodBound(((VCallNode) node).getName(),
                        new BranchCallback() {

                            public void branch(MethodCompiler context) {
                                context.pushString("method");
                            }
                        },
                        new BranchCallback() {

                            public void branch(MethodCompiler context) {
                                context.pushNull();
                            }
                        });
                break;
            case YIELDNODE:
                context.hasBlock(new BranchCallback() {

                            public void branch(MethodCompiler context) {
                                context.pushString("yield");
                            }
                        },
                        new BranchCallback() {

                            public void branch(MethodCompiler context) {
                                context.pushNull();
                            }
                        });
                break;
            case GLOBALVARNODE:
                context.isGlobalDefined(((GlobalVarNode) node).getName(),
                        new BranchCallback() {

                            public void branch(MethodCompiler context) {
                                context.pushString("global-variable");
                            }
                        },
                        new BranchCallback() {

                            public void branch(MethodCompiler context) {
                                context.pushNull();
                            }
                        });
                break;
            case INSTVARNODE:
                context.isInstanceVariableDefined(((InstVarNode) node).getName(),
                        new BranchCallback() {

                            public void branch(MethodCompiler context) {
                                context.pushString("instance-variable");
                            }
                        },
                        new BranchCallback() {

                            public void branch(MethodCompiler context) {
                                context.pushNull();
                            }
                        });
                break;
            case CONSTNODE:
                context.isConstantDefined(((ConstNode) node).getName(),
                        new BranchCallback() {

                            public void branch(MethodCompiler context) {
                                context.pushString("constant");
                            }
                        },
                        new BranchCallback() {

                            public void branch(MethodCompiler context) {
                                context.pushNull();
                            }
                        });
                break;
            case FCALLNODE:
                context.loadSelf();
                context.isMethodBound(((FCallNode) node).getName(),
                        new BranchCallback() {

                            public void branch(MethodCompiler context) {
                                compileGetArgumentDefinition(((FCallNode) node).getArgsNode(), context, "method");
                            }
                        },
                        new BranchCallback() {

                            public void branch(MethodCompiler context) {
                                context.pushNull();
                            }
                        });
                break;
            case COLON3NODE:
            case COLON2NODE:
                {
                    final Colon3Node iVisited = (Colon3Node) node;

                    final String name = iVisited.getName();

                    BranchCallback setup = new BranchCallback() {

                                public void branch(MethodCompiler context) {
                                    if (iVisited instanceof Colon2Node) {
                                        final Node leftNode = ((Colon2Node) iVisited).getLeftNode();
                                        compile(leftNode, context);
                                    } else {
                                        context.loadObject();
                                    }
                                }
                            };
                    BranchCallback isConstant = new BranchCallback() {

                                public void branch(MethodCompiler context) {
                                    context.pushString("constant");
                                }
                            };
                    BranchCallback isMethod = new BranchCallback() {

                                public void branch(MethodCompiler context) {
                                    context.pushString("method");
                                }
                            };
                    BranchCallback none = new BranchCallback() {

                                public void branch(MethodCompiler context) {
                                    context.pushNull();
                                }
                            };
                    context.isConstantBranch(setup, isConstant, isMethod, none, name);
                    break;
                }
            case CALLNODE:
                {
                    final CallNode iVisited = (CallNode) node;
                    Object isnull = context.getNewEnding();
                    Object ending = context.getNewEnding();
                    compileGetDefinition(iVisited.getReceiverNode(), context);
                    context.ifNull(isnull);

                    context.rescue(new BranchCallback() {

                                public void branch(MethodCompiler context) {
                                    compile(iVisited.getReceiverNode(), context); //[IRubyObject]
                                    context.duplicateCurrentValue(); //[IRubyObject, IRubyObject]
                                    context.metaclass(); //[IRubyObject, RubyClass]
                                    context.duplicateCurrentValue(); //[IRubyObject, RubyClass, RubyClass]
                                    context.getVisibilityFor(iVisited.getName()); //[IRubyObject, RubyClass, Visibility]
                                    context.duplicateCurrentValue(); //[IRubyObject, RubyClass, Visibility, Visibility]
                                    final Object isfalse = context.getNewEnding();
                                    Object isreal = context.getNewEnding();
                                    Object ending = context.getNewEnding();
                                    context.isPrivate(isfalse, 3); //[IRubyObject, RubyClass, Visibility]
                                    context.isNotProtected(isreal, 1); //[IRubyObject, RubyClass]
                                    context.selfIsKindOf(isreal); //[IRubyObject]
                                    context.consumeCurrentValue();
                                    context.go(isfalse);
                                    context.setEnding(isreal); //[]

                                    context.isMethodBound(iVisited.getName(), new BranchCallback() {

                                                public void branch(MethodCompiler context) {
                                                    compileGetArgumentDefinition(iVisited.getArgsNode(), context, "method");
                                                }
                                            },
                                            new BranchCallback() {

                                                public void branch(MethodCompiler context) {
                                                    context.go(isfalse);
                                                }
                                            });
                                    context.go(ending);
                                    context.setEnding(isfalse);
                                    context.pushNull();
                                    context.setEnding(ending);
                                }
                            }, JumpException.class,
                            new BranchCallback() {

                                public void branch(MethodCompiler context) {
                                    context.pushNull();
                                }
                            }, String.class);

                    //          context.swapValues();
            //context.consumeCurrentValue();
                    context.go(ending);
                    context.setEnding(isnull);
                    context.pushNull();
                    context.setEnding(ending);
                    break;
                }
            case CLASSVARNODE:
                {
                    ClassVarNode iVisited = (ClassVarNode) node;
                    final Object ending = context.getNewEnding();
                    final Object failure = context.getNewEnding();
                    final Object singleton = context.getNewEnding();
                    Object second = context.getNewEnding();
                    Object third = context.getNewEnding();

                    context.loadCurrentModule(); //[RubyClass]
                    context.duplicateCurrentValue(); //[RubyClass, RubyClass]
                    context.ifNotNull(second); //[RubyClass]
                    context.consumeCurrentValue(); //[]
                    context.loadSelf(); //[self]
                    context.metaclass(); //[RubyClass]
                    context.duplicateCurrentValue(); //[RubyClass, RubyClass]
                    context.isClassVarDefined(iVisited.getName(),
                            new BranchCallback() {

                                public void branch(MethodCompiler context) {
                                    context.consumeCurrentValue();
                                    context.pushString("class variable");
                                    context.go(ending);
                                }
                            },
                            new BranchCallback() {

                                public void branch(MethodCompiler context) {
                                }
                            });
                    context.setEnding(second);  //[RubyClass]
                    context.duplicateCurrentValue();
                    context.isClassVarDefined(iVisited.getName(),
                            new BranchCallback() {

                                public void branch(MethodCompiler context) {
                                    context.consumeCurrentValue();
                                    context.pushString("class variable");
                                    context.go(ending);
                                }
                            },
                            new BranchCallback() {

                                public void branch(MethodCompiler context) {
                                }
                            });
                    context.setEnding(third); //[RubyClass]
                    context.duplicateCurrentValue(); //[RubyClass, RubyClass]
                    context.ifSingleton(singleton); //[RubyClass]
                    context.consumeCurrentValue();//[]
                    context.go(failure);
                    context.setEnding(singleton);
                    context.attached();//[RubyClass]
                    context.notIsModuleAndClassVarDefined(iVisited.getName(), failure); //[]
                    context.pushString("class variable");
                    context.go(ending);
                    context.setEnding(failure);
                    context.pushNull();
                    context.setEnding(ending);
                }
                break;
            case ZSUPERNODE:
                {
                    Object fail = context.getNewEnding();
                    Object fail2 = context.getNewEnding();
                    Object fail_easy = context.getNewEnding();
                    Object ending = context.getNewEnding();

                    context.getFrameName(); //[String]
                    context.duplicateCurrentValue(); //[String, String]
                    context.ifNull(fail); //[String]
                    context.getFrameKlazz(); //[String, RubyClass]
                    context.duplicateCurrentValue(); //[String, RubyClass, RubyClass]
                    context.ifNull(fail2); //[String, RubyClass]
                    context.superClass();
                    context.ifNotSuperMethodBound(fail_easy);

                    context.pushString("super");
                    context.go(ending);

                    context.setEnding(fail2);
                    context.consumeCurrentValue();
                    context.setEnding(fail);
                    context.consumeCurrentValue();
                    context.setEnding(fail_easy);
                    context.pushNull();
                    context.setEnding(ending);
                }
                break;
            case SUPERNODE:
                {
                    Object fail = context.getNewEnding();
                    Object fail2 = context.getNewEnding();
                    Object fail_easy = context.getNewEnding();
                    Object ending = context.getNewEnding();

                    context.getFrameName(); //[String]
                    context.duplicateCurrentValue(); //[String, String]
                    context.ifNull(fail); //[String]
                    context.getFrameKlazz(); //[String, RubyClass]
                    context.duplicateCurrentValue(); //[String, RubyClass, RubyClass]
                    context.ifNull(fail2); //[String, RubyClass]
                    context.superClass();
                    context.ifNotSuperMethodBound(fail_easy);

                    compileGetArgumentDefinition(((SuperNode) node).getArgsNode(), context, "super");
                    context.go(ending);

                    context.setEnding(fail2);
                    context.consumeCurrentValue();
                    context.setEnding(fail);
                    context.consumeCurrentValue();
                    context.setEnding(fail_easy);
                    context.pushNull();
                    context.setEnding(ending);
                    break;
                }
            case ATTRASSIGNNODE:
                {
                    final AttrAssignNode iVisited = (AttrAssignNode) node;
                    Object isnull = context.getNewEnding();
                    Object ending = context.getNewEnding();
                    compileGetDefinition(iVisited.getReceiverNode(), context);
                    context.ifNull(isnull);

                    context.rescue(new BranchCallback() {

                                public void branch(MethodCompiler context) {
                                    compile(iVisited.getReceiverNode(), context); //[IRubyObject]
                                    context.duplicateCurrentValue(); //[IRubyObject, IRubyObject]
                                    context.metaclass(); //[IRubyObject, RubyClass]
                                    context.duplicateCurrentValue(); //[IRubyObject, RubyClass, RubyClass]
                                    context.getVisibilityFor(iVisited.getName()); //[IRubyObject, RubyClass, Visibility]
                                    context.duplicateCurrentValue(); //[IRubyObject, RubyClass, Visibility, Visibility]
                                    final Object isfalse = context.getNewEnding();
                                    Object isreal = context.getNewEnding();
                                    Object ending = context.getNewEnding();
                                    context.isPrivate(isfalse, 3); //[IRubyObject, RubyClass, Visibility]
                                    context.isNotProtected(isreal, 1); //[IRubyObject, RubyClass]
                                    context.selfIsKindOf(isreal); //[IRubyObject]
                                    context.consumeCurrentValue();
                                    context.go(isfalse);
                                    context.setEnding(isreal); //[]

                                    context.isMethodBound(iVisited.getName(), new BranchCallback() {

                                                public void branch(MethodCompiler context) {
                                                    compileGetArgumentDefinition(iVisited.getArgsNode(), context, "assignment");
                                                }
                                            },
                                            new BranchCallback() {

                                                public void branch(MethodCompiler context) {
                                                    context.go(isfalse);
                                                }
                                            });
                                    context.go(ending);
                                    context.setEnding(isfalse);
                                    context.pushNull();
                                    context.setEnding(ending);
                                }
                            }, JumpException.class,
                            new BranchCallback() {

                                public void branch(MethodCompiler context) {
                                    context.pushNull();
                                }
                            }, String.class);

                    context.go(ending);
                    context.setEnding(isnull);
                    context.pushNull();
                    context.setEnding(ending);
                    break;
                }
            default:
                context.rescue(new BranchCallback() {

                            public void branch(MethodCompiler context) {
                                compile(node, context);
                                context.consumeCurrentValue();
                                context.pushNull();
                            }
                        }, JumpException.class,
                        new BranchCallback() {

                            public void branch(MethodCompiler context) {
                                context.pushNull();
                            }
                        }, String.class);
                context.consumeCurrentValue();
                context.pushString("expression");
        }
    }

    public void compileDAsgn(Node node, MethodCompiler context) {
        final DAsgnNode dasgnNode = (DAsgnNode) node;

        CompilerCallback value = new CompilerCallback() {
            public void call(MethodCompiler context) {
                compile(dasgnNode.getValueNode(), context);
            }
        };
        
        context.getVariableCompiler().assignLocalVariable(dasgnNode.getIndex(), dasgnNode.getDepth(), value);
    }

    public void compileDAsgnAssignment(Node node, MethodCompiler context) {
        DAsgnNode dasgnNode = (DAsgnNode) node;

        context.getVariableCompiler().assignLocalVariable(dasgnNode.getIndex(), dasgnNode.getDepth());
    }

    public void compileDefn(Node node, MethodCompiler context) {
        final DefnNode defnNode = (DefnNode) node;
        final ArgsNode argsNode = defnNode.getArgsNode();

        CompilerCallback body = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        if (defnNode.getBodyNode() != null) {
                            compile(defnNode.getBodyNode(), context);
                        } else {
                            context.loadNil();
                        }
                    }
                };

        CompilerCallback args = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        compileArgs(argsNode, context);
                    }
                };

        // inspect body and args
        ASTInspector inspector = new ASTInspector();
        // check args first, since body inspection can depend on args
        inspector.inspect(defnNode.getArgsNode());
        inspector.inspect(defnNode.getBodyNode());

        context.defineNewMethod(defnNode.getName(), defnNode.getArgsNode().getArity().getValue(), defnNode.getScope(), body, args, null, inspector, isAtRoot);
    }

    public void compileDefs(Node node, MethodCompiler context) {
        final DefsNode defsNode = (DefsNode) node;
        final ArgsNode argsNode = defsNode.getArgsNode();

        CompilerCallback receiver = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        compile(defsNode.getReceiverNode(), context);
                    }
                };

        CompilerCallback body = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        if (defsNode.getBodyNode() != null) {
                            compile(defsNode.getBodyNode(), context);
                        } else {
                            context.loadNil();
                        }
                    }
                };

        CompilerCallback args = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        compileArgs(argsNode, context);
                    }
                };

        // inspect body and args
        ASTInspector inspector = new ASTInspector();
        inspector.inspect(defsNode.getArgsNode());
        inspector.inspect(defsNode.getBodyNode());

        context.defineNewMethod(defsNode.getName(), defsNode.getArgsNode().getArity().getValue(), defsNode.getScope(), body, args, receiver, inspector, false);
    }

    public void compileArgs(Node node, MethodCompiler context) {
        final ArgsNode argsNode = (ArgsNode) node;

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

                        public void nextValue(MethodCompiler context, Object object, int index) {
                            // FIXME: Somehow I'd feel better if this could get the appropriate var index from the ArgumentNode
                            context.getVariableCompiler().assignLocalVariable(index);
                        }
                    };
        }

        if (opt > 0) {
            optionalGiven = new ArrayCallback() {

                        public void nextValue(MethodCompiler context, Object object, int index) {
                            Node optArg = ((ListNode) object).get(index);

                            compileAssignment(optArg, context);
                        }
                    };
            optionalNotGiven = new ArrayCallback() {

                        public void nextValue(MethodCompiler context, Object object, int index) {
                            Node optArg = ((ListNode) object).get(index);

                            compile(optArg, context);
                        }
                    };
        }

        if (rest > -1) {
            restAssignment = new CompilerCallback() {

                        public void call(MethodCompiler context) {
                            context.getVariableCompiler().assignLocalVariable(argsNode.getRestArg());
                        }
                    };
        }

        if (argsNode.getBlockArgNode() != null) {
            blockAssignment = new CompilerCallback() {

                        public void call(MethodCompiler context) {
                            context.getVariableCompiler().assignLocalVariable(argsNode.getBlockArgNode().getCount());
                        }
                    };
        }

        context.getVariableCompiler().checkMethodArity(required, opt, rest);
        context.getVariableCompiler().assignMethodArguments(argsNode.getArgs(),
                argsNode.getRequiredArgsCount(),
                argsNode.getOptArgs(),
                argsNode.getOptionalArgsCount(),
                requiredAssignment,
                optionalGiven,
                optionalNotGiven,
                restAssignment,
                blockAssignment);
    }

    public void compileDot(Node node, MethodCompiler context) {
        DotNode dotNode = (DotNode) node;

        compile(dotNode.getBeginNode(), context);
        compile(dotNode.getEndNode(), context);

        context.createNewRange(dotNode.isExclusive());
    }

    public void compileDRegexp(Node node, MethodCompiler context) {
        final DRegexpNode dregexpNode = (DRegexpNode) node;

        CompilerCallback createStringCallback = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        ArrayCallback dstrCallback = new ArrayCallback() {

                                    public void nextValue(MethodCompiler context, Object sourceArray,
                                            int index) {
                                        compile(dregexpNode.get(index), context);
                                    }
                                };
                        context.createNewString(dstrCallback, dregexpNode.size());
                    }
                };

        context.createNewRegexp(createStringCallback, dregexpNode.getOptions());
    }

    public void compileDStr(Node node, MethodCompiler context) {
        final DStrNode dstrNode = (DStrNode) node;

        ArrayCallback dstrCallback = new ArrayCallback() {

                    public void nextValue(MethodCompiler context, Object sourceArray,
                            int index) {
                        compile(dstrNode.get(index), context);
                    }
                };
        context.createNewString(dstrCallback, dstrNode.size());
    }

    public void compileDSymbol(Node node, MethodCompiler context) {
        final DSymbolNode dsymbolNode = (DSymbolNode) node;

        ArrayCallback dstrCallback = new ArrayCallback() {

                    public void nextValue(MethodCompiler context, Object sourceArray,
                            int index) {
                        compile(dsymbolNode.get(index), context);
                    }
                };
        context.createNewSymbol(dstrCallback, dsymbolNode.size());
    }

    public void compileDVar(Node node, MethodCompiler context) {
        DVarNode dvarNode = (DVarNode) node;

        context.getVariableCompiler().retrieveLocalVariable(dvarNode.getIndex(), dvarNode.getDepth());
    }

    public void compileDXStr(Node node, MethodCompiler context) {
        final DXStrNode dxstrNode = (DXStrNode) node;

        final ArrayCallback dstrCallback = new ArrayCallback() {

                    public void nextValue(MethodCompiler context, Object sourceArray,
                            int index) {
                        compile(dxstrNode.get(index), context);
                    }
                };

        ArgumentsCallback argsCallback = new ArgumentsCallback() {
                    public int getArity() {
                        return 1;
                    }
                    
                    public void call(MethodCompiler context) {
                        context.createNewString(dstrCallback, dxstrNode.size());
                    }
                };

        context.getInvocationCompiler().invokeDynamic("`", null, argsCallback, CallType.FUNCTIONAL, null, false);
    }

    public void compileEnsureNode(Node node, MethodCompiler context) {
        final EnsureNode ensureNode = (EnsureNode) node;

        if (ensureNode.getEnsureNode() != null) {
            context.protect(new BranchCallback() {

                        public void branch(MethodCompiler context) {
                            if (ensureNode.getBodyNode() != null) {
                                compile(ensureNode.getBodyNode(), context);
                            } else {
                                context.loadNil();
                            }
                        }
                    },
                    new BranchCallback() {

                        public void branch(MethodCompiler context) {
                            compile(ensureNode.getEnsureNode(), context);
                            context.consumeCurrentValue();
                        }
                    }, IRubyObject.class);
        } else {
            if (ensureNode.getBodyNode() != null) {
                compile(ensureNode.getBodyNode(), context);
            } else {
                context.loadNil();
            }
        }
    }

    public void compileEvStr(Node node, MethodCompiler context) {
        final EvStrNode evStrNode = (EvStrNode) node;

        compile(evStrNode.getBody(), context);
        context.asString();
    }

    public void compileFalse(Node node, MethodCompiler context) {
        context.loadFalse();

        context.pollThreadEvents();
    }

    public void compileFCall(Node node, MethodCompiler context) {
        final FCallNode fcallNode = (FCallNode) node;

        ArgumentsCallback argsCallback = getArgsCallback(fcallNode.getArgsNode());
        
        CompilerCallback closureArg = getBlock(fcallNode.getIterNode());

        context.getInvocationCompiler().invokeDynamic(fcallNode.getName(), null, argsCallback, CallType.FUNCTIONAL, closureArg, fcallNode.getIterNode() instanceof IterNode);
    }

    private CompilerCallback getBlock(Node node) {
        if (node == null) {
            return null;
        }

        switch (node.nodeId) {
            case ITERNODE:
                final IterNode iterNode = (IterNode) node;

                return new CompilerCallback() {

                            public void call(MethodCompiler context) {
                                compile(iterNode, context);
                            }
                        };
            case BLOCKPASSNODE:
                final BlockPassNode blockPassNode = (BlockPassNode) node;

                return new CompilerCallback() {

                            public void call(MethodCompiler context) {
                                compile(blockPassNode.getBodyNode(), context);
                                context.unwrapPassedBlock();
                            }
                        };
            default:
                throw new NotCompilableException("ERROR: Encountered a method with a non-block, non-blockpass iter node at: " + node);
        }
    }

    public void compileFixnum(Node node, MethodCompiler context) {
        FixnumNode fixnumNode = (FixnumNode) node;

        context.createNewFixnum(fixnumNode.getValue());
    }

    public void compileFlip(Node node, MethodCompiler context) {
        final FlipNode flipNode = (FlipNode) node;

        context.getVariableCompiler().retrieveLocalVariable(flipNode.getIndex(), flipNode.getDepth());

        if (flipNode.isExclusive()) {
            context.performBooleanBranch(new BranchCallback() {

                public void branch(MethodCompiler context) {
                    compile(flipNode.getEndNode(), context);
                    context.performBooleanBranch(new BranchCallback() {

                        public void branch(MethodCompiler context) {
                            context.loadFalse();
                            context.getVariableCompiler().assignLocalVariable(flipNode.getIndex(), flipNode.getDepth());
                            context.consumeCurrentValue();
                        }
                    }, new BranchCallback() {

                        public void branch(MethodCompiler context) {
                        }
                    });
                    context.loadTrue();
                }
            }, new BranchCallback() {

                public void branch(MethodCompiler context) {
                    compile(flipNode.getBeginNode(), context);
                    becomeTrueOrFalse(context);
                    context.getVariableCompiler().assignLocalVariable(flipNode.getIndex(), flipNode.getDepth());
                }
            });
        } else {
            context.performBooleanBranch(new BranchCallback() {

                public void branch(MethodCompiler context) {
                    compile(flipNode.getEndNode(), context);
                    context.performBooleanBranch(new BranchCallback() {

                        public void branch(MethodCompiler context) {
                            context.loadFalse();
                            context.getVariableCompiler().assignLocalVariable(flipNode.getIndex(), flipNode.getDepth());
                            context.consumeCurrentValue();
                        }
                    }, new BranchCallback() {

                        public void branch(MethodCompiler context) {
                        }
                    });
                    context.loadTrue();
                }
            }, new BranchCallback() {

                public void branch(MethodCompiler context) {
                    compile(flipNode.getBeginNode(), context);
                    context.performBooleanBranch(new BranchCallback() {

                        public void branch(MethodCompiler context) {
                            compile(flipNode.getEndNode(), context);
                            flipTrueOrFalse(context);
                            context.getVariableCompiler().assignLocalVariable(flipNode.getIndex(), flipNode.getDepth());
                            context.consumeCurrentValue();
                            context.loadTrue();
                        }
                    }, new BranchCallback() {

                        public void branch(MethodCompiler context) {
                            context.loadFalse();
                        }
                    });
                }
            });
        }
    }

    private void becomeTrueOrFalse(MethodCompiler context) {
        context.performBooleanBranch(new BranchCallback() {

                    public void branch(MethodCompiler context) {
                        context.loadTrue();
                    }
                }, new BranchCallback() {

                    public void branch(MethodCompiler context) {
                        context.loadFalse();
                    }
                });
    }

    private void flipTrueOrFalse(MethodCompiler context) {
        context.performBooleanBranch(new BranchCallback() {

                    public void branch(MethodCompiler context) {
                        context.loadFalse();
                    }
                }, new BranchCallback() {

                    public void branch(MethodCompiler context) {
                        context.loadTrue();
                    }
                });
    }

    public void compileFloat(Node node, MethodCompiler context) {
        FloatNode floatNode = (FloatNode) node;

        context.createNewFloat(floatNode.getValue());
    }

    public void compileFor(Node node, MethodCompiler context) {
        final ForNode forNode = (ForNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        compile(forNode.getIterNode(), context);
                    }
                };

        final CompilerCallback closureArg = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        compileForIter(forNode, context);
                    }
                };

        context.getInvocationCompiler().invokeDynamic("each", receiverCallback, null, CallType.NORMAL, closureArg, true);
    }

    public void compileForIter(Node node, MethodCompiler context) {
        final ForNode forNode = (ForNode) node;

        // create the closure class and instantiate it
        final CompilerCallback closureBody = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        if (forNode.getBodyNode() != null) {
                            compile(forNode.getBodyNode(), context);
                        } else {
                            context.loadNil();
                        }
                    }
                };

        // create the closure class and instantiate it
        final CompilerCallback closureArgs = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        if (forNode.getVarNode() != null) {
                            compileAssignment(forNode.getVarNode(), context);
                        }
                    }
                };

        boolean hasMultipleArgsHead = false;
        if (forNode.getVarNode() instanceof MultipleAsgnNode) {
            hasMultipleArgsHead = ((MultipleAsgnNode) forNode.getVarNode()).getHeadNode() != null;
        }

        NodeType argsNodeId = null;
        if (forNode.getVarNode() != null) {
            argsNodeId = forNode.getVarNode().nodeId;
        }

        if (argsNodeId == null) {
            // no args, do not pass args processor
            context.createNewForLoop(Arity.procArityOf(forNode.getVarNode()).getValue(),
                    closureBody, null, hasMultipleArgsHead, argsNodeId);
        } else {
            context.createNewForLoop(Arity.procArityOf(forNode.getVarNode()).getValue(),
                    closureBody, closureArgs, hasMultipleArgsHead, argsNodeId);
        }
    }

    public void compileGlobalAsgn(Node node, MethodCompiler context) {
        final GlobalAsgnNode globalAsgnNode = (GlobalAsgnNode) node;

        CompilerCallback value = new CompilerCallback() {
            public void call(MethodCompiler context) {
                compile(globalAsgnNode.getValueNode(), context);
            }
        };

        if (globalAsgnNode.getName().length() == 2) {
            switch (globalAsgnNode.getName().charAt(1)) {
                case '_':
                    context.getVariableCompiler().assignLastLine(value);
                    return;
                case '~':
                    context.getVariableCompiler().assignBackRef(value);
                    return;
                default:
                // fall off the end, handle it as a normal global
            }
        }

        context.assignGlobalVariable(globalAsgnNode.getName(), value);
    }

    public void compileGlobalAsgnAssignment(Node node, MethodCompiler context) {
        GlobalAsgnNode globalAsgnNode = (GlobalAsgnNode) node;

        if (globalAsgnNode.getName().length() == 2) {
            switch (globalAsgnNode.getName().charAt(1)) {
                case '_':
                    context.getVariableCompiler().assignLastLine();
                    return;
                case '~':
                    context.getVariableCompiler().assignBackRef();
                    return;
                default:
                // fall off the end, handle it as a normal global
            }
        }

        context.assignGlobalVariable(globalAsgnNode.getName());
    }

    public void compileGlobalVar(Node node, MethodCompiler context) {
        GlobalVarNode globalVarNode = (GlobalVarNode) node;

        if (globalVarNode.getName().length() == 2) {
            switch (globalVarNode.getName().charAt(1)) {
                case '_':
                    context.getVariableCompiler().retrieveLastLine();
                    return;
                case '~':
                    context.getVariableCompiler().retrieveBackRef();
                    return;
            }
        }

        context.retrieveGlobalVariable(globalVarNode.getName());
    }

    public void compileHash(Node node, MethodCompiler context) {
        HashNode hashNode = (HashNode) node;

        if (hashNode.getListNode() == null || hashNode.getListNode().size() == 0) {
            context.createEmptyHash();
            return;
        }

        ArrayCallback hashCallback = new ArrayCallback() {

                    public void nextValue(MethodCompiler context, Object sourceArray,
                            int index) {
                        ListNode listNode = (ListNode) sourceArray;
                        int keyIndex = index * 2;
                        compile(listNode.get(keyIndex), context);
                        compile(listNode.get(keyIndex + 1), context);
                    }
                };

        context.createNewHash(hashNode.getListNode(), hashCallback, hashNode.getListNode().size() / 2);
    }

    public void compileIf(Node node, MethodCompiler context) {
        final IfNode ifNode = (IfNode) node;

        compile(ifNode.getCondition(), context);

        BranchCallback trueCallback = new BranchCallback() {

                    public void branch(MethodCompiler context) {
                        if (ifNode.getThenBody() != null) {
                            compile(ifNode.getThenBody(), context);
                        } else {
                            context.loadNil();
                        }
                    }
                };

        BranchCallback falseCallback = new BranchCallback() {

                    public void branch(MethodCompiler context) {
                        if (ifNode.getElseBody() != null) {
                            compile(ifNode.getElseBody(), context);
                        } else {
                            context.loadNil();
                        }
                    }
                };

        context.performBooleanBranch(trueCallback, falseCallback);
    }

    public void compileInstAsgn(Node node, MethodCompiler context) {
        final InstAsgnNode instAsgnNode = (InstAsgnNode) node;

        CompilerCallback value = new CompilerCallback() {
            public void call(MethodCompiler context) {
                compile(instAsgnNode.getValueNode(), context);
            }
        };

        context.assignInstanceVariable(instAsgnNode.getName(), value);
    }

    public void compileInstAsgnAssignment(Node node, MethodCompiler context) {
        InstAsgnNode instAsgnNode = (InstAsgnNode) node;
        context.assignInstanceVariable(instAsgnNode.getName());
    }

    public void compileInstVar(Node node, MethodCompiler context) {
        InstVarNode instVarNode = (InstVarNode) node;

        context.retrieveInstanceVariable(instVarNode.getName());
    }

    public void compileIter(Node node, MethodCompiler context) {
        final IterNode iterNode = (IterNode) node;

        // create the closure class and instantiate it
        final CompilerCallback closureBody = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        if (iterNode.getBodyNode() != null) {
                            compile(iterNode.getBodyNode(), context);
                        } else {
                            context.loadNil();
                        }
                    }
                };

        // create the closure class and instantiate it
        final CompilerCallback closureArgs = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        if (iterNode.getVarNode() != null) {
                            compileAssignment(iterNode.getVarNode(), context);
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
            context.createNewClosure(iterNode.getPosition().getStartLine(), iterNode.getScope(), Arity.procArityOf(iterNode.getVarNode()).getValue(),
                    closureBody, null, hasMultipleArgsHead, argsNodeId, inspector);
        } else {
            context.createNewClosure(iterNode.getPosition().getStartLine(), iterNode.getScope(), Arity.procArityOf(iterNode.getVarNode()).getValue(),
                    closureBody, closureArgs, hasMultipleArgsHead, argsNodeId, inspector);
        }
    }

    public void compileLocalAsgn(Node node, MethodCompiler context) {
        final LocalAsgnNode localAsgnNode = (LocalAsgnNode) node;

        // just push null for pragmas
        if (ASTInspector.PRAGMAS.contains(localAsgnNode.getName())) {
            context.loadNull();
        } else {
            CompilerCallback value = new CompilerCallback() {
                public void call(MethodCompiler context) {
                    compile(localAsgnNode.getValueNode(), context);
                }
            };

            context.getVariableCompiler().assignLocalVariable(localAsgnNode.getIndex(), localAsgnNode.getDepth(), value);
        }
    }

    public void compileLocalAsgnAssignment(Node node, MethodCompiler context) {
        // "assignment" means the value is already on the stack
        LocalAsgnNode localAsgnNode = (LocalAsgnNode) node;

        context.getVariableCompiler().assignLocalVariable(localAsgnNode.getIndex(), localAsgnNode.getDepth());
    }

    public void compileLocalVar(Node node, MethodCompiler context) {
        LocalVarNode localVarNode = (LocalVarNode) node;

        context.getVariableCompiler().retrieveLocalVariable(localVarNode.getIndex(), localVarNode.getDepth());
    }

    public void compileMatch(Node node, MethodCompiler context) {
        MatchNode matchNode = (MatchNode) node;

        compile(matchNode.getRegexpNode(), context);

        context.match();
    }

    public void compileMatch2(Node node, MethodCompiler context) {
        Match2Node matchNode = (Match2Node) node;

        compile(matchNode.getReceiverNode(), context);
        compile(matchNode.getValueNode(), context);

        context.match2();
    }

    public void compileMatch3(Node node, MethodCompiler context) {
        Match3Node matchNode = (Match3Node) node;

        compile(matchNode.getReceiverNode(), context);
        compile(matchNode.getValueNode(), context);

        context.match3();
    }

    public void compileModule(Node node, MethodCompiler context) {
        final ModuleNode moduleNode = (ModuleNode) node;

        final Node cpathNode = moduleNode.getCPath();

        CompilerCallback bodyCallback = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        if (moduleNode.getBodyNode() != null) {
                            compile(moduleNode.getBodyNode(), context);
                        }
                        context.loadNil();
                    }
                };

        CompilerCallback pathCallback = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        if (cpathNode instanceof Colon2Node) {
                            Node leftNode = ((Colon2Node) cpathNode).getLeftNode();
                            if (leftNode != null) {
                                compile(leftNode, context);
                            } else {
                                context.loadNil();
                            }
                        } else if (cpathNode instanceof Colon3Node) {
                            context.loadObject();
                        } else {
                            context.loadNil();
                        }
                    }
                };

        context.defineModule(moduleNode.getCPath().getName(), moduleNode.getScope(), pathCallback, bodyCallback);
    }

    public void compileMultipleAsgn(Node node, MethodCompiler context) {
        MultipleAsgnNode multipleAsgnNode = (MultipleAsgnNode) node;

        // FIXME: This is a little less efficient than it could be, since in the interpreter we avoid objectspace for these arrays
        compile(multipleAsgnNode.getValueNode(), context);

        compileMultipleAsgnAssignment(node, context);
    }

    public void compileMultipleAsgnAssignment(Node node, MethodCompiler context) {
        final MultipleAsgnNode multipleAsgnNode = (MultipleAsgnNode) node;

        context.ensureMultipleAssignableRubyArray(multipleAsgnNode.getHeadNode() != null);

        // normal items at the "head" of the masgn
        ArrayCallback headAssignCallback = new ArrayCallback() {

                    public void nextValue(MethodCompiler context, Object sourceArray,
                            int index) {
                        ListNode headNode = (ListNode) sourceArray;
                        Node assignNode = headNode.get(index);

                        // perform assignment for the next node
                        compileAssignment(assignNode, context);
                    }
                };

        // head items for which we've run out of assignable elements
        ArrayCallback headNilCallback = new ArrayCallback() {

                    public void nextValue(MethodCompiler context, Object sourceArray,
                            int index) {
                        ListNode headNode = (ListNode) sourceArray;
                        Node assignNode = headNode.get(index);

                        // perform assignment for the next node
                        context.loadNil();
                        compileAssignment(assignNode, context);
                    }
                };

        CompilerCallback argsCallback = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        Node argsNode = multipleAsgnNode.getArgsNode();
                        if (argsNode instanceof StarNode) {
                        // done processing args
                        } else {
                            // assign to appropriate variable
                            compileAssignment(argsNode, context);
                        }
                    }
                };

        if (multipleAsgnNode.getHeadNode() == null) {
            if (multipleAsgnNode.getArgsNode() == null) {
                throw new NotCompilableException("Something's wrong, multiple assignment with no head or args at: " + multipleAsgnNode.getPosition());
            } else {
                context.forEachInValueArray(0, 0, null, null, null, argsCallback);
            }
        } else {
            if (multipleAsgnNode.getArgsNode() == null) {
                context.forEachInValueArray(0, multipleAsgnNode.getHeadNode().size(), multipleAsgnNode.getHeadNode(), headAssignCallback, headNilCallback, null);
            } else {
                context.forEachInValueArray(0, multipleAsgnNode.getHeadNode().size(), multipleAsgnNode.getHeadNode(), headAssignCallback, headNilCallback, argsCallback);
            }
        }
    }

    public void compileNewline(Node node, MethodCompiler context) {
        // TODO: add trace call?
        context.lineNumber(node.getPosition());

        context.setLinePosition(node.getPosition());

        NewlineNode newlineNode = (NewlineNode) node;

        compile(newlineNode.getNextNode(), context);
    }

    public void compileNext(Node node, MethodCompiler context) {
        final NextNode nextNode = (NextNode) node;

        CompilerCallback valueCallback = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        if (nextNode.getValueNode() != null) {
                            compile(nextNode.getValueNode(), context);
                        } else {
                            context.loadNil();
                        }
                    }
                };

        context.pollThreadEvents();
        context.issueNextEvent(valueCallback);
    }

    public void compileNthRef(Node node, MethodCompiler context) {
        NthRefNode nthRefNode = (NthRefNode) node;

        context.nthRef(nthRefNode.getMatchNumber());
    }

    public void compileNil(Node node, MethodCompiler context) {
        context.loadNil();

        context.pollThreadEvents();
    }

    public void compileNot(Node node, MethodCompiler context) {
        NotNode notNode = (NotNode) node;

        compile(notNode.getConditionNode(), context);

        context.negateCurrentValue();
    }

    public void compileOpAsgnAnd(Node node, MethodCompiler context) {
        final BinaryOperatorNode andNode = (BinaryOperatorNode) node;

        compile(andNode.getFirstNode(), context);

        BranchCallback longCallback = new BranchCallback() {

                    public void branch(MethodCompiler context) {
                        compile(andNode.getSecondNode(), context);
                    }
                };

        context.performLogicalAnd(longCallback);
        context.pollThreadEvents();
    }

    public void compileOpAsgnOr(Node node, MethodCompiler context) {
        final OpAsgnOrNode orNode = (OpAsgnOrNode) node;

        compileGetDefinitionBase(orNode.getFirstNode(), context);

        context.isNull(new BranchCallback() {

                    public void branch(MethodCompiler context) {
                        compile(orNode.getSecondNode(), context);
                    }
                }, new BranchCallback() {

                    public void branch(MethodCompiler context) {
                        compile(orNode.getFirstNode(), context);
                        context.duplicateCurrentValue();
                        context.performBooleanBranch(new BranchCallback() {

                                    public void branch(MethodCompiler context) {
                                    //Do nothing
                                    }
                                },
                                new BranchCallback() {

                                    public void branch(MethodCompiler context) {
                                        context.consumeCurrentValue();
                                        compile(orNode.getSecondNode(), context);
                                    }
                                });
                    }
                });

        context.pollThreadEvents();
    }

    public void compileOpAsgn(Node node, MethodCompiler context) {
        final OpAsgnNode opAsgnNode = (OpAsgnNode) node;

        if (opAsgnNode.getOperatorName().equals("||")) {
            compileOpAsgnWithOr(opAsgnNode, context);
        } else if (opAsgnNode.getOperatorName().equals("&&")) {
            compileOpAsgnWithAnd(opAsgnNode, context);
        } else {
            compileOpAsgnWithMethod(opAsgnNode, context);
        }

        context.pollThreadEvents();
    }

    public void compileOpAsgnWithOr(Node node, MethodCompiler context) {
        final OpAsgnNode opAsgnNode = (OpAsgnNode) node;

        final CompilerCallback receiverCallback = new CompilerCallback() {

            public void call(MethodCompiler context) {
                compile(opAsgnNode.getReceiverNode(), context); // [recv]
            }
        };
        
        ArgumentsCallback argsCallback = getArgsCallback(opAsgnNode.getValueNode());
        
        context.getInvocationCompiler().invokeOpAsgnWithOr(opAsgnNode.getVariableName(), opAsgnNode.getVariableNameAsgn(), receiverCallback, argsCallback);
    }

    public void compileOpAsgnWithAnd(Node node, MethodCompiler context) {
        final OpAsgnNode opAsgnNode = (OpAsgnNode) node;

        final CompilerCallback receiverCallback = new CompilerCallback() {

            public void call(MethodCompiler context) {
                compile(opAsgnNode.getReceiverNode(), context); // [recv]
            }
        };
        
        ArgumentsCallback argsCallback = getArgsCallback(opAsgnNode.getValueNode());
        
        context.getInvocationCompiler().invokeOpAsgnWithAnd(opAsgnNode.getVariableName(), opAsgnNode.getVariableNameAsgn(), receiverCallback, argsCallback);
    }

    public void compileOpAsgnWithMethod(Node node, MethodCompiler context) {
        final OpAsgnNode opAsgnNode = (OpAsgnNode) node;

        final CompilerCallback receiverCallback = new CompilerCallback() {
                    public void call(MethodCompiler context) {
                        compile(opAsgnNode.getReceiverNode(), context); // [recv]
                    }
                };

        // eval new value, call operator on old value, and assign
        ArgumentsCallback argsCallback = new ArgumentsCallback() {
            public int getArity() {
                return 1;
            }

            public void call(MethodCompiler context) {
                compile(opAsgnNode.getValueNode(), context);
            }
        };
        
        context.getInvocationCompiler().invokeOpAsgnWithMethod(opAsgnNode.getOperatorName(), opAsgnNode.getVariableName(), opAsgnNode.getVariableNameAsgn(), receiverCallback, argsCallback);
    }

    public void compileOpElementAsgn(Node node, MethodCompiler context) {
        final OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode) node;
        
        if (opElementAsgnNode.getOperatorName() == "||") {
            compileOpElementAsgnWithOr(node, context);
        } else if (opElementAsgnNode.getOperatorName() == "&&") {
            compileOpElementAsgnWithAnd(node, context);
        } else {
            compileOpElementAsgnWithMethod(node, context);
        }
    }

    public void compileOpElementAsgnWithOr(Node node, MethodCompiler context) {
        final OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {
            public void call(MethodCompiler context) {
                compile(opElementAsgnNode.getReceiverNode(), context);
            }
        };

        ArgumentsCallback argsCallback = new ArgumentsCallback() {
            public int getArity() {
                Node node = opElementAsgnNode.getArgsNode();
                switch (node.nodeId) {
                case ARGSCATNODE:
                case ARGSPUSHNODE:
                case SPLATNODE:
                    return -1;
                case ARRAYNODE:
                    ArrayNode arrayNode = (ArrayNode)node;
                    if (arrayNode.size() == 0) {
                        return 0;
                    } else if (arrayNode.size() > 3) {
                        return -1;
                    } else {
                        return ((ArrayNode)node).size();
                    }
                default:
                    return 1;
                }
            }

            public void call(MethodCompiler context) {
                if (getArity() == 1) {
                    // if arity 1, just compile the one element to save us the array cost
                    compile(((ArrayNode)opElementAsgnNode.getArgsNode()).get(0), context);
                } else {
                    // compile into array
                    compileArguments(opElementAsgnNode.getArgsNode(), context);
                }
            }
        };

        CompilerCallback valueCallback = new CompilerCallback() {
            public void call(MethodCompiler context) {
                compile(opElementAsgnNode.getValueNode(), context);
            }
        };

        context.getInvocationCompiler().opElementAsgnWithOr(receiverCallback, argsCallback, valueCallback);
    }

    public void compileOpElementAsgnWithAnd(Node node, MethodCompiler context) {
        final OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {
            public void call(MethodCompiler context) {
                compile(opElementAsgnNode.getReceiverNode(), context);
            }
        };

        ArgumentsCallback argsCallback = new ArgumentsCallback() {
            public int getArity() {
                Node node = opElementAsgnNode.getArgsNode();
                switch (node.nodeId) {
                case ARGSCATNODE:
                case ARGSPUSHNODE:
                case SPLATNODE:
                    return -1;
                case ARRAYNODE:
                    ArrayNode arrayNode = (ArrayNode)node;
                    if (arrayNode.size() == 0) {
                        return 0;
                    } else if (arrayNode.size() > 3) {
                        return -1;
                    } else {
                        return ((ArrayNode)node).size();
                    }
                default:
                    return 1;
                }
            }

            public void call(MethodCompiler context) {
                if (getArity() == 1) {
                    // if arity 1, just compile the one element to save us the array cost
                    compile(((ArrayNode)opElementAsgnNode.getArgsNode()).get(0), context);
                } else {
                    // compile into array
                    compileArguments(opElementAsgnNode.getArgsNode(), context);
                }
            }
        };

        CompilerCallback valueCallback = new CompilerCallback() {
            public void call(MethodCompiler context) {
                compile(opElementAsgnNode.getValueNode(), context);
            }
        };

        context.getInvocationCompiler().opElementAsgnWithAnd(receiverCallback, argsCallback, valueCallback);
    }

    public void compileOpElementAsgnWithMethod(Node node, MethodCompiler context) {
        final OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {
            public void call(MethodCompiler context) {
                compile(opElementAsgnNode.getReceiverNode(), context);
            }
        };

        ArgumentsCallback argsCallback = getArgsCallback(opElementAsgnNode.getArgsNode());

        CompilerCallback valueCallback = new CompilerCallback() {
            public void call(MethodCompiler context) {
                compile(opElementAsgnNode.getValueNode(), context);
            }
        };

        context.getInvocationCompiler().opElementAsgnWithMethod(receiverCallback, argsCallback, valueCallback, opElementAsgnNode.getOperatorName());
    }

    public void compileOr(Node node, MethodCompiler context) {
        final OrNode orNode = (OrNode) node;

        compile(orNode.getFirstNode(), context);

        BranchCallback longCallback = new BranchCallback() {

                    public void branch(MethodCompiler context) {
                        compile(orNode.getSecondNode(), context);
                    }
                };

        context.performLogicalOr(longCallback);
    }

    public void compilePostExe(Node node, MethodCompiler context) {
        final PostExeNode postExeNode = (PostExeNode) node;

        // create the closure class and instantiate it
        final CompilerCallback closureBody = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        if (postExeNode.getBodyNode() != null) {
                            compile(postExeNode.getBodyNode(), context);
                        } else {
                            context.loadNil();
                        }
                    }
                };
        context.createNewEndBlock(closureBody);
    }

    public void compilePreExe(Node node, MethodCompiler context) {
        final PreExeNode preExeNode = (PreExeNode) node;

        // create the closure class and instantiate it
        final CompilerCallback closureBody = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        if (preExeNode.getBodyNode() != null) {
                            compile(preExeNode.getBodyNode(), context);
                        } else {
                            context.loadNil();
                        }
                    }
                };
        context.runBeginBlock(preExeNode.getScope(), closureBody);
    }

    public void compileRedo(Node node, MethodCompiler context) {
        //RedoNode redoNode = (RedoNode)node;

        context.issueRedoEvent();
    }

    public void compileRegexp(Node node, MethodCompiler context) {
        RegexpNode reNode = (RegexpNode) node;

        context.createNewRegexp(reNode.getValue(), reNode.getOptions());
    }

    public void compileRescue(Node node, MethodCompiler context) {
        final RescueNode rescueNode = (RescueNode) node;

        BranchCallback body = new BranchCallback() {

                    public void branch(MethodCompiler context) {
                        if (rescueNode.getBodyNode() != null) {
                            compile(rescueNode.getBodyNode(), context);
                        } else {
                            context.loadNil();
                        }

                        if (rescueNode.getElseNode() != null) {
                            context.consumeCurrentValue();
                            compile(rescueNode.getElseNode(), context);
                        }
                    }
                };

        BranchCallback handler = new BranchCallback() {

                    public void branch(MethodCompiler context) {
                        context.loadException();
                        context.unwrapRaiseException();
                        context.assignGlobalVariable("$!");
                        context.consumeCurrentValue();
                        compileRescueBody(rescueNode.getRescueNode(), context);
                    }
                };

        context.rescue(body, RaiseException.class, handler, IRubyObject.class);
    }

    public void compileRescueBody(Node node, MethodCompiler context) {
        final RescueBodyNode rescueBodyNode = (RescueBodyNode) node;

        context.loadException();
        context.unwrapRaiseException();

        Node exceptionList = rescueBodyNode.getExceptionNodes();
        if (exceptionList == null) {
            context.loadClass("StandardError");
            context.createObjectArray(1);
        } else {
            compileArguments(exceptionList, context);
        }

        context.checkIsExceptionHandled();

        BranchCallback trueBranch = new BranchCallback() {

                    public void branch(MethodCompiler context) {
                        if (rescueBodyNode.getBodyNode() != null) {
                            compile(rescueBodyNode.getBodyNode(), context);
                            context.loadNil();
                            // FIXME: this should reset to what it was before
                            context.assignGlobalVariable("$!");
                            context.consumeCurrentValue();
                        } else {
                            context.loadNil();
                            // FIXME: this should reset to what it was before
                            context.assignGlobalVariable("$!");
                        }
                    }
                };

        BranchCallback falseBranch = new BranchCallback() {

                    public void branch(MethodCompiler context) {
                        if (rescueBodyNode.getOptRescueNode() != null) {
                            compileRescueBody(rescueBodyNode.getOptRescueNode(), context);
                        } else {
                            context.rethrowException();
                        }
                    }
                };

        context.performBooleanBranch(trueBranch, falseBranch);
    }

    public void compileRetry(Node node, MethodCompiler context) {
        context.pollThreadEvents();

        context.issueRetryEvent();
    }

    public void compileReturn(Node node, MethodCompiler context) {
        ReturnNode returnNode = (ReturnNode) node;

        if (returnNode.getValueNode() != null) {
            compile(returnNode.getValueNode(), context);
        } else {
            context.loadNil();
        }

        context.performReturn();
    }

    public void compileRoot(Node node, ScriptCompiler context, ASTInspector inspector) {
        RootNode rootNode = (RootNode) node;

        context.startScript(rootNode.getStaticScope());

        // create method for toplevel of script
        MethodCompiler methodCompiler = context.startMethod("__file__", null, rootNode.getStaticScope(), inspector);

        Node nextNode = rootNode.getBodyNode();
        if (nextNode != null) {
            if (nextNode.nodeId == NodeType.BLOCKNODE) {
                // it's a multiple-statement body, iterate over all elements in turn and chain if it get too long
                BlockNode blockNode = (BlockNode) nextNode;

                for (int i = 0; i < blockNode.size(); i++) {
                    if ((i + 1) % RubyInstanceConfig.CHAINED_COMPILE_LINE_COUNT == 0) {
                        methodCompiler = methodCompiler.chainToMethod("__file__from_line_" + (i + 1), inspector);
                    }
                    compile(blockNode.get(i), methodCompiler);

                    if (i + 1 < blockNode.size()) {
                        // clear result from previous line
                        methodCompiler.consumeCurrentValue();
                    }
                }
            } else {
                // single-statement body, just compile it
                compile(nextNode, methodCompiler);
            }
        } else {
            methodCompiler.loadNil();
        }

        methodCompiler.endMethod();

        context.endScript(true, true, true);
    }

    public void compileSelf(Node node, MethodCompiler context) {
        context.retrieveSelf();
    }

    public void compileSplat(Node node, MethodCompiler context) {
        SplatNode splatNode = (SplatNode) node;

        compile(splatNode.getValue(), context);

        context.splatCurrentValue();
    }

    public void compileStr(Node node, MethodCompiler context) {
        StrNode strNode = (StrNode) node;

        context.createNewString(strNode.getValue());
    }

    public void compileSuper(Node node, MethodCompiler context) {
        final SuperNode superNode = (SuperNode) node;

        CompilerCallback argsCallback = new CompilerCallback() {

                    public void call(MethodCompiler context) {
                        compileArguments(superNode.getArgsNode(), context);
                    }
                };


        if (superNode.getIterNode() == null) {
            // no block, go for simple version
            if (superNode.getArgsNode() != null) {
                context.getInvocationCompiler().invokeSuper(argsCallback, null);
            } else {
                context.getInvocationCompiler().invokeSuper(null, null);
            }
        } else {
            CompilerCallback closureArg = getBlock(superNode.getIterNode());

            if (superNode.getArgsNode() != null) {
                context.getInvocationCompiler().invokeSuper(argsCallback, closureArg);
            } else {
                context.getInvocationCompiler().invokeSuper(null, closureArg);
            }
        }
    }

    public void compileSValue(Node node, MethodCompiler context) {
        SValueNode svalueNode = (SValueNode) node;

        compile(svalueNode.getValue(), context);

        context.singlifySplattedValue();
    }

    public void compileSymbol(Node node, MethodCompiler context) {
        context.createNewSymbol(((SymbolNode) node).getName());
    }    
    
    public void compileToAry(Node node, MethodCompiler context) {
        ToAryNode toAryNode = (ToAryNode) node;

        compile(toAryNode.getValue(), context);

        context.aryToAry();
    }

    public void compileTrue(Node node, MethodCompiler context) {
        context.loadTrue();

        context.pollThreadEvents();
    }

    public void compileUndef(Node node, MethodCompiler context) {
        context.undefMethod(((UndefNode) node).getName());
    }

    public void compileUntil(Node node, MethodCompiler context) {
        final UntilNode untilNode = (UntilNode) node;

        BranchCallback condition = new BranchCallback() {

            public void branch(MethodCompiler context) {
                compile(untilNode.getConditionNode(), context);
                context.negateCurrentValue();
            }
        };

        BranchCallback body = new BranchCallback() {

            public void branch(MethodCompiler context) {
                if (untilNode.getBodyNode() == null) {
                    context.loadNil();
                    return;
                }
                compile(untilNode.getBodyNode(), context);
            }
        };

        if (untilNode.containsNonlocalFlow) {
            context.performBooleanLoopSafe(condition, body, untilNode.evaluateAtStart());
        } else {
            context.performBooleanLoopLight(condition, body, untilNode.evaluateAtStart());
        }

        context.pollThreadEvents();
    }

    public void compileVAlias(Node node, MethodCompiler context) {
        VAliasNode valiasNode = (VAliasNode) node;

        context.aliasGlobal(valiasNode.getNewName(), valiasNode.getOldName());
    }

    public void compileVCall(Node node, MethodCompiler context) {
        VCallNode vcallNode = (VCallNode) node;
        
        context.getInvocationCompiler().invokeDynamic(vcallNode.getName(), null, null, CallType.VARIABLE, null, false);
    }

    public void compileWhile(Node node, MethodCompiler context) {
        final WhileNode whileNode = (WhileNode) node;

        BranchCallback condition = new BranchCallback() {

            public void branch(MethodCompiler context) {
                compile(whileNode.getConditionNode(), context);
            }
        };

        BranchCallback body = new BranchCallback() {

            public void branch(MethodCompiler context) {
                if (whileNode.getBodyNode() == null) {
                    context.loadNil();
                } else {
                    compile(whileNode.getBodyNode(), context);
                }
            }
        };

        if (whileNode.containsNonlocalFlow) {
            context.performBooleanLoopSafe(condition, body, whileNode.evaluateAtStart());
        } else {
            context.performBooleanLoopLight(condition, body, whileNode.evaluateAtStart());
        }

        context.pollThreadEvents();
    }

    public void compileXStr(Node node, MethodCompiler context) {
        final XStrNode xstrNode = (XStrNode) node;

        ArgumentsCallback argsCallback = new ArgumentsCallback() {
            public int getArity() {
                return 1;
            }

            public void call(MethodCompiler context) {
                context.createNewString(xstrNode.getValue());
            }
        };
        context.getInvocationCompiler().invokeDynamic("`", null, argsCallback, CallType.FUNCTIONAL, null, false);
    }

    public void compileYield(Node node, MethodCompiler context) {
        YieldNode yieldNode = (YieldNode) node;

        if (yieldNode.getArgsNode() != null) {
            compile(yieldNode.getArgsNode(), context);
        }

        context.getInvocationCompiler().yield(yieldNode.getArgsNode() != null, yieldNode.getCheckState());
    }

    public void compileZArray(Node node, MethodCompiler context) {
        context.createEmptyArray();
    }

    public void compileZSuper(Node node, MethodCompiler context) {
        ZSuperNode zsuperNode = (ZSuperNode) node;

        CompilerCallback closure = getBlock(zsuperNode.getIterNode());

        context.callZSuper(closure);
    }

    public void compileArgsCatArguments(Node node, MethodCompiler context) {
        ArgsCatNode argsCatNode = (ArgsCatNode) node;

        compileArguments(argsCatNode.getFirstNode(), context);
        // arguments compilers always create IRubyObject[], but we want to use RubyArray.concat here;
        // FIXME: as a result, this is NOT efficient, since it creates and then later unwraps an array
        context.createNewArray(true);
        compile(argsCatNode.getSecondNode(), context);
        context.splatCurrentValue();
        context.concatArrays();
        context.convertToJavaArray();
    }

    public void compileArgsPushArguments(Node node, MethodCompiler context) {
        ArgsPushNode argsPushNode = (ArgsPushNode) node;
        compile(argsPushNode.getFirstNode(), context);
        compile(argsPushNode.getSecondNode(), context);
        context.appendToArray();
        context.convertToJavaArray();
    }

    public void compileArrayArguments(Node node, MethodCompiler context) {
        ArrayNode arrayNode = (ArrayNode) node;

        ArrayCallback callback = new ArrayCallback() {

                    public void nextValue(MethodCompiler context, Object sourceArray, int index) {
                        Node node = (Node) ((Object[]) sourceArray)[index];
                        compile(node, context);
                    }
                };

        context.setLinePosition(arrayNode.getPosition());
        context.createObjectArray(arrayNode.childNodes().toArray(), callback);
    // leave as a normal array
    }

    public void compileSplatArguments(Node node, MethodCompiler context) {
        SplatNode splatNode = (SplatNode) node;

        compile(splatNode.getValue(), context);
        context.splatCurrentValue();
        context.convertToJavaArray();
    }

    /**
     * Check whether the target node can safely be compiled.
     * 
     * @param node 
     */
    public static void confirmNodeIsSafe(Node node) {
        switch (node.nodeId) {
            case ARGSNODE:
                ArgsNode argsNode = (ArgsNode) node;
                // FIXME: We can't compile cases like def(a=(b=1)) because the variables
            // in the arg list get ordered differently than you might expect (b comes first)
            // So the code below searches through all opt args, ensuring none of them skip
            // indicies. A skipped index means there's a hidden local var/arg like b above
            // and so we shouldn't try to compile.
                if (argsNode.getOptArgs() != null && argsNode.getOptArgs().size() > 0) {
                    int index = argsNode.getRequiredArgsCount() - 1;

                    for (int i = 0; i < argsNode.getOptArgs().size(); i++) {
                        int newIndex = ((LocalAsgnNode) argsNode.getOptArgs().get(i)).getIndex();

                        if (newIndex - index != 1) {
                            throw new NotCompilableException("Can't compile def with optional args that assign other variables at: " + node.getPosition());
                        }
                        index = newIndex;
                    }
                }
                break;
        }
    }
}
