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
import org.jruby.ast.ArgumentNode;
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
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public class ASTCompiler {
    public static void compile(Node node, MethodCompiler context) {
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
            assert false: "When nodes are handled by case node compilation.";
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
            assert false: "Unknown node encountered in compiler: " + node;
        }
    }
    
    public static void compileArguments(Node node, MethodCompiler context) {
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
    
    public static void compileAssignment(Node node, MethodCompiler context) {
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

    public static void compileAlias(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final AliasNode alias = (AliasNode)node;
        
        context.defineAlias(alias.getNewName(),alias.getOldName());
    }
    
    public static void compileAnd(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final AndNode andNode = (AndNode)node;
        
        compile(andNode.getFirstNode(), context);
        
        BranchCallback longCallback = new BranchCallback() {
            public void branch(MethodCompiler context) {
                compile(andNode.getSecondNode(), context);
            }
        };
        
        context.performLogicalAnd(longCallback);
    }
    
    public static void compileArray(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        ArrayNode arrayNode = (ArrayNode)node;
        
        ArrayCallback callback = new ArrayCallback() {
            public void nextValue(MethodCompiler context, Object sourceArray, int index) {
                Node node = (Node)((Object[])sourceArray)[index];
                compile(node, context);
            }
        };
        
        context.createObjectArray(arrayNode.childNodes().toArray(), callback);
        context.createNewArray(arrayNode.isLightweight());
    }
    
    public static void compileArgsCat(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        ArgsCatNode argsCatNode = (ArgsCatNode)node;
        
        compile(argsCatNode.getFirstNode(), context);
        context.ensureRubyArray();
        compile(argsCatNode.getSecondNode(), context);
        context.splatCurrentValue();
        context.concatArrays();
    }
    
    public static void compileArgsPush(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        ArgsPushNode argsPush = (ArgsPushNode)node;
        
        compile(argsPush.getFirstNode(), context);
        compile(argsPush.getSecondNode(), context);
        context.concatArrays();
    }
    
    public static void compileAttrAssign(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        AttrAssignNode attrAssignNode = (AttrAssignNode)node;
        
        compile(attrAssignNode.getReceiverNode(), context);
        compileArguments(attrAssignNode.getArgsNode(), context);
        
        context.getInvocationCompiler().invokeAttrAssign(attrAssignNode.getName());
    }
    
    public static void compileAttrAssignAssignment(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        AttrAssignNode attrAssignNode = (AttrAssignNode)node;
        
        compile(attrAssignNode.getReceiverNode(), context);
        context.swapValues();
        if (attrAssignNode.getArgsNode() != null) {
            compileArguments(attrAssignNode.getArgsNode(), context);
            context.swapValues();
            context.appendToObjectArray();
        } else {
            context.createObjectArray(1);
        }
        
        context.getInvocationCompiler().invokeAttrAssign(attrAssignNode.getName());
    }
    
    public static void compileBackref(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        BackRefNode iVisited = (BackRefNode) node;
        
        context.performBackref(iVisited.getType());
    }
    
    public static void compileBegin(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        BeginNode beginNode = (BeginNode)node;
        
        compile(beginNode.getBodyNode(), context);
    }

    public static void compileBignum(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        context.createNewBignum(((BignumNode)node).getValue());
    }

    public static void compileBlock(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        BlockNode blockNode = (BlockNode)node;
        
        for (Iterator<Node> iter = blockNode.childNodes().iterator(); iter.hasNext();) {
            Node n = iter.next();
            
            compile(n, context);
            
            if (iter.hasNext()) {
                // clear result from previous line
                context.consumeCurrentValue();
            }
        }
    }
    
    public static void compileBreak(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final BreakNode breakNode = (BreakNode)node;
        
        ClosureCallback valueCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (breakNode.getValueNode() != null) {
                    ASTCompiler.compile(breakNode.getValueNode(), context);
                } else {
                    context.loadNil();
                }
            }
        };
        
        context.issueBreakEvent(valueCallback);
    }
    
    public static void compileCall(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final CallNode callNode = (CallNode)node;
        
        ClosureCallback receiverCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                ASTCompiler.compile(callNode.getReceiverNode(), context);
            }
        };
        
        ClosureCallback argsCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                compileArguments(callNode.getArgsNode(), context);
            }
        };
                
        if (callNode.getIterNode() == null) {
            // no block, go for simple version
            if (callNode.getArgsNode() != null) {
                context.getInvocationCompiler().invokeDynamic(callNode.getName(), receiverCallback, argsCallback, CallType.NORMAL, null, false);
            } else {
                context.getInvocationCompiler().invokeDynamic(callNode.getName(), receiverCallback, null, CallType.NORMAL, null, false);
            }
        } else {
            ClosureCallback closureArg = getBlock(callNode.getIterNode());
            
            if (callNode.getArgsNode() != null) {
                context.getInvocationCompiler().invokeDynamic(callNode.getName(), receiverCallback, argsCallback, CallType.NORMAL, closureArg, false);
            } else {
                context.getInvocationCompiler().invokeDynamic(callNode.getName(), receiverCallback, null, CallType.NORMAL, closureArg, false);
            }
        }
    }
    
    public static void compileCase(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        CaseNode caseNode = (CaseNode)node;
        
        boolean hasCase = false;
        if (caseNode.getCaseNode() != null) {
            compile(caseNode.getCaseNode(), context);
            hasCase = true;
        }

        context.pollThreadEvents();

        Node firstWhenNode = caseNode.getFirstWhenNode();
        compileWhen(firstWhenNode, context, hasCase);
    }
    
    public static void compileWhen(Node node, MethodCompiler context, final boolean hasCase) {
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
        
        WhenNode whenNode = (WhenNode)node;
        
        if (whenNode.getExpressionNodes() instanceof ArrayNode) {
            ArrayNode arrayNode = (ArrayNode)whenNode.getExpressionNodes();
            
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
                context.createObjectArray(1);
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
                        ASTCompiler.compile(currentWhen.getBodyNode(), context);
                    } else {
                        context.loadNil();
                    }
                }
            };

            BranchCallback falseBranch = new BranchCallback() {
                public void branch(MethodCompiler context) {
                    // proceed to the next when
                    ASTCompiler.compileWhen(currentWhen.getNextCase(), context, hasCase);
                }
            };

            context.performBooleanBranch(trueBranch, falseBranch);
        }
    }
    
    public static void compileMultiArgWhen(
            final WhenNode whenNode, final ArrayNode expressionsNode, final int conditionIndex, MethodCompiler context, final boolean hasCase) {
        
        if (conditionIndex >= expressionsNode.size()) {
            // done with conditions, continue to next when in the chain
            compileWhen(whenNode.getNextCase(), context, hasCase);
            return;
        }
        
        Node tag = expressionsNode.get(conditionIndex);

        // need to add in position stuff some day :)
        context.setPosition(tag.getPosition());

        // reduce the when cases to a true or false ruby value for the branch below
        if (tag instanceof WhenNode) {
            // prepare to handle the when logic
            if (hasCase) {
                context.duplicateCurrentValue();
            } else {
                context.loadNull();
            }
            compile(((WhenNode)tag).getExpressionNodes(), context);
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
                context.createObjectArray(1);
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
                    ASTCompiler.compile(whenNode.getBodyNode(), context);
                } else {
                    context.loadNil();
                }
            }
        };

        BranchCallback falseBranch = new BranchCallback() {
            public void branch(MethodCompiler context) {
                // proceed to the next when
                ASTCompiler.compileMultiArgWhen(whenNode, expressionsNode, conditionIndex + 1, context, hasCase);
            }
        };

        context.performBooleanBranch(trueBranch, falseBranch);
    }
    
    public static void compileClass(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final ClassNode classNode = (ClassNode)node;
        
        final Node superNode = classNode.getSuperNode();
        
        final Node cpathNode = classNode.getCPath();
        
        ClosureCallback superCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                ASTCompiler.compile(superNode, context);
            }
        };
        if (superNode == null) {
            superCallback = null;
        }
        
        ClosureCallback bodyCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (classNode.getBodyNode() != null) {
                    ASTCompiler.compile(classNode.getBodyNode(), context);
                } else {
                    context.loadNil();
                }
            }
        };
        
        ClosureCallback pathCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (cpathNode instanceof Colon2Node) {
                    Node leftNode = ((Colon2Node)cpathNode).getLeftNode();
                    if (leftNode != null) {
                        ASTCompiler.compile(leftNode, context);
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
    
    public static void compileSClass(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final SClassNode sclassNode = (SClassNode)node;
        
        ClosureCallback receiverCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                ASTCompiler.compile(sclassNode.getReceiverNode(), context);
            }
        };
        
        ClosureCallback bodyCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (sclassNode.getBodyNode() != null) {
                    ASTCompiler.compile(sclassNode.getBodyNode(), context);
                } else {
                    context.loadNil();
                }
            }
        };
        
        context.defineClass("SCLASS", sclassNode.getScope(), null, null, bodyCallback, receiverCallback);
    }

    public static void compileClassVar(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        ClassVarNode classVarNode = (ClassVarNode)node;
        
        context.retrieveClassVariable(classVarNode.getName());
    }

    public static void compileClassVarAsgn(Node node, MethodCompiler context) {
        ClassVarAsgnNode classVarAsgnNode = (ClassVarAsgnNode)node;
        
        // FIXME: probably more efficient with a callback
        compile(classVarAsgnNode.getValueNode(), context);
        
        compileClassVarAsgnAssignment(node, context);
    }

    public static void compileClassVarAsgnAssignment(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        ClassVarAsgnNode classVarAsgnNode = (ClassVarAsgnNode)node;
        
        context.assignClassVariable(classVarAsgnNode.getName());
    }

    public static void compileClassVarDecl(Node node, MethodCompiler context) {
        ClassVarDeclNode classVarDeclNode = (ClassVarDeclNode)node;
        
        // FIXME: probably more efficient with a callback
        compile(classVarDeclNode.getValueNode(), context);
        
        compileClassVarDeclAssignment(node, context);
    }

    public static void compileClassVarDeclAssignment(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        ClassVarDeclNode classVarDeclNode = (ClassVarDeclNode)node;
        
        context.declareClassVariable(classVarDeclNode.getName());
    }
    
    public static void compileConstDecl(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        ConstDeclNode constDeclNode = (ConstDeclNode)node;
        Node constNode = constDeclNode.getConstNode();
        
        if (constDeclNode.getConstNode() == null) {
            compile(constDeclNode.getValueNode(), context);
        
            context.assignConstantInCurrent(constDeclNode.getName());
        } else if (constNode.nodeId == NodeType.COLON2NODE) {
            compile(((Colon2Node)constNode).getLeftNode(), context);
            compile(constDeclNode.getValueNode(), context);
            
            context.assignConstantInModule(constDeclNode.getName());
        } else {// colon3, assign in Object
            compile(constDeclNode.getValueNode(), context);
            
            context.assignConstantInObject(constDeclNode.getName());
        }
    }
    
    public static void compileConstDeclAssignment(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        ConstDeclNode constDeclNode = (ConstDeclNode)node;
        
        if (constDeclNode.getConstNode() == null) {
            context.assignConstantInCurrent(constDeclNode.getName());
        } else if (constDeclNode.nodeId == NodeType.COLON2NODE) {
            context.assignConstantInModule(constDeclNode.getName());
        } else {// colon3, assign in Object
            context.assignConstantInObject(constDeclNode.getName());
        }
    }

    public static void compileConst(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        ConstNode constNode = (ConstNode)node;
        
        context.retrieveConstant(constNode.getName());
    }
    
    public static void compileColon2(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        final Colon2Node iVisited = (Colon2Node) node;
        Node leftNode = iVisited.getLeftNode();
        final String name = iVisited.getName();

        if(leftNode == null) {
            context.loadObject();
            context.retrieveConstantFromModule(name);
        } else {
            final ClosureCallback receiverCallback = new ClosureCallback() {
                public void compile(MethodCompiler context) {
                    ASTCompiler.compile(iVisited.getLeftNode(), context);
                }
            };
            
            BranchCallback moduleCallback = new BranchCallback() {
                    public void branch(MethodCompiler context) {
                        receiverCallback.compile(context);
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
    
    public static void compileColon3(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        Colon3Node iVisited = (Colon3Node) node;
        String name = iVisited.getName();

        context.loadObject();
        context.retrieveConstantFromModule(name);
    }
    
    public static void compileGetDefinitionBase(final Node node, MethodCompiler context) {
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
        context.protect(reg,out,String.class);
    }

    public static void compileDefined(final Node node, MethodCompiler context) {
        compileGetDefinitionBase(((DefinedNode)node).getExpressionNode(), context);
        context.stringOrNil();
    }

    public static void compileGetArgumentDefinition(final Node node, MethodCompiler context, String type) {
        if (node == null) {
            context.pushString(type);
        } else if(node instanceof ArrayNode) {
            Object endToken = context.getNewEnding();
            for (int i = 0; i < ((ArrayNode)node).size(); i++) {
                Node iterNode = ((ArrayNode)node).get(i);
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

    public static void compileGetDefinition(final Node node, MethodCompiler context) {
        switch(node.nodeId) {
        case CLASSVARASGNNODE: case CLASSVARDECLNODE: case CONSTDECLNODE:
        case DASGNNODE: case GLOBALASGNNODE: case LOCALASGNNODE:
        case MULTIPLEASGNNODE: case OPASGNNODE: case OPELEMENTASGNNODE:
            context.pushString("assignment");
            break;
        case BACKREFNODE:
            context.backref();
            context.isInstanceOf(RubyMatchData.class, 
                                 new BranchCallback(){
                public void branch(MethodCompiler context) {
                    context.pushString("$" + ((BackRefNode) node).getType());
                }},
                                 new BranchCallback(){
                                     public void branch(MethodCompiler context) {
                                         context.pushNull();
                                     }});
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
        case MATCH2NODE: case MATCH3NODE:
            context.pushString("method");
            break;
        case NILNODE:
            context.pushString("nil");
            break;
        case NTHREFNODE:
            context.isCaptured(((NthRefNode) node).getMatchNumber(),
                               new BranchCallback(){
                                   public void branch(MethodCompiler context) {
                                       context.pushString("$" + ((NthRefNode) node).getMatchNumber());
                                   }},
                               new BranchCallback(){
                                   public void branch(MethodCompiler context) {
                                       context.pushNull();
                                   }});
            break;
        case SELFNODE:
            context.pushString("self");
            break;
        case VCALLNODE:
            context.loadSelf();
            context.isMethodBound(((VCallNode)node).getName(),
                                  new BranchCallback(){
                                      public void branch(MethodCompiler context){
                                          context.pushString("method");
                                      }
                                  },
                                  new BranchCallback(){
                                      public void branch(MethodCompiler context){
                                          context.pushNull();
                                      }
                                  });
            break;
        case YIELDNODE:
            context.hasBlock(new BranchCallback(){
                    public void branch(MethodCompiler context){
                        context.pushString("yield");
                    }
                },
                new BranchCallback(){
                    public void branch(MethodCompiler context){
                        context.pushNull();
                    }
                });
            break;
        case GLOBALVARNODE:
            context.isGlobalDefined(((GlobalVarNode) node).getName(),
                                    new BranchCallback(){
                                        public void branch(MethodCompiler context){
                                            context.pushString("global-variable");
                                        }
                                    },
                                    new BranchCallback(){
                                        public void branch(MethodCompiler context){
                                            context.pushNull();
                                        }
                                    });
            break;
        case INSTVARNODE:
            context.isInstanceVariableDefined(((InstVarNode) node).getName(),
                                              new BranchCallback(){
                                                  public void branch(MethodCompiler context){
                                                      context.pushString("instance-variable");
                                                  }
                                              },
                                              new BranchCallback(){
                                                  public void branch(MethodCompiler context){
                                                      context.pushNull();
                                                  }
                                              });
            break;
        case CONSTNODE:
            context.isConstantDefined(((ConstNode) node).getName(),
                                      new BranchCallback(){
                                          public void branch(MethodCompiler context){
                                              context.pushString("constant");
                                          }
                                      },
                                      new BranchCallback(){
                                          public void branch(MethodCompiler context){
                                              context.pushNull();
                                          }
                                      });
            break;
        case FCALLNODE:
            context.loadSelf();
            context.isMethodBound(((FCallNode)node).getName(),
                                  new BranchCallback(){
                                      public void branch(MethodCompiler context){
                                          compileGetArgumentDefinition(((FCallNode)node).getArgsNode(), context, "method");
                                      }
                                  },
                                  new BranchCallback(){
                                      public void branch(MethodCompiler context){
                                          context.pushNull();
                                      }
                                  });
            break;
        case COLON3NODE:
        case COLON2NODE: {
            final Colon3Node iVisited = (Colon3Node) node;

            final String name = iVisited.getName();

            BranchCallback setup = new BranchCallback() {
                    public void branch(MethodCompiler context){
                        if(iVisited instanceof Colon2Node) {
                            final Node leftNode = ((Colon2Node)iVisited).getLeftNode();
                            ASTCompiler.compile(leftNode, context);
                        } else {
                            context.loadObject();
                        }
                    }
                };
            BranchCallback isConstant = new BranchCallback() {
                    public void branch(MethodCompiler context){
                        context.pushString("constant");
                    }
                };
            BranchCallback isMethod = new BranchCallback() {
                    public void branch(MethodCompiler context){
                        context.pushString("method");
                    }
                };
            BranchCallback none = new BranchCallback() {
                    public void branch(MethodCompiler context){
                        context.pushNull();
                    }
                };
            context.isConstantBranch(setup, isConstant, isMethod, none, name);
            break;
        }
        case CALLNODE: {
            final CallNode iVisited = (CallNode) node;
            Object isnull = context.getNewEnding();
            Object ending = context.getNewEnding();
            ASTCompiler.compileGetDefinition(iVisited.getReceiverNode(), context);
            context.ifNull(isnull);

            context.rescue(new BranchCallback() {
                    public void branch(MethodCompiler context) {
                        ASTCompiler.compile(iVisited.getReceiverNode(), context); //[IRubyObject]
                        context.duplicateCurrentValue(); //[IRubyObject, IRubyObject]
                        context.metaclass(); //[IRubyObject, RubyClass]
                        context.duplicateCurrentValue(); //[IRubyObject, RubyClass, RubyClass]
                        context.getVisibilityFor(iVisited.getName()); //[IRubyObject, RubyClass, Visibility]
                        context.duplicateCurrentValue(); //[IRubyObject, RubyClass, Visibility, Visibility]
                        final Object isfalse = context.getNewEnding();
                        Object isreal = context.getNewEnding();
                        Object ending = context.getNewEnding();
                        context.isPrivate(isfalse,3); //[IRubyObject, RubyClass, Visibility]
                        context.isNotProtected(isreal,1); //[IRubyObject, RubyClass]
                        context.selfIsKindOf(isreal); //[IRubyObject]
                        context.consumeCurrentValue();
                        context.go(isfalse);
                        context.setEnding(isreal); //[]
                        
                        context.isMethodBound(iVisited.getName(), new BranchCallback(){
                                public void branch(MethodCompiler context) {
                                    compileGetArgumentDefinition(iVisited.getArgsNode(), context, "method");
                                }
                            }, 
                            new BranchCallback(){
                                public void branch(MethodCompiler context) { 
                                    context.go(isfalse); 
                                }
                            });
                        context.go(ending);
                        context.setEnding(isfalse);
                        context.pushNull();
                        context.setEnding(ending);
                    }}, JumpException.class,
                new BranchCallback() {
                        public void branch(MethodCompiler context) {
                            context.pushNull();
                        }}, String.class);

            //          context.swapValues();
            //context.consumeCurrentValue();
            context.go(ending);
            context.setEnding(isnull);            
            context.pushNull();
            context.setEnding(ending);
            break;
        }
        case CLASSVARNODE: {
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
                                          }},
                                      new BranchCallback() {
                                          public void branch(MethodCompiler context) {}});
            context.setEnding(second);  //[RubyClass]
            context.duplicateCurrentValue();
            context.isClassVarDefined(iVisited.getName(),
                                      new BranchCallback() {
                                          public void branch(MethodCompiler context) {
                                              context.consumeCurrentValue();
                                              context.pushString("class variable");
                                              context.go(ending);
                                          }},
                                      new BranchCallback() {
                                          public void branch(MethodCompiler context) {
                                          }});
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
        case ZSUPERNODE: {
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
        case SUPERNODE: {
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

            compileGetArgumentDefinition(((SuperNode)node).getArgsNode(), context, "super");
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
        case ATTRASSIGNNODE: {
            final AttrAssignNode iVisited = (AttrAssignNode) node;
            Object isnull = context.getNewEnding();
            Object ending = context.getNewEnding();
            ASTCompiler.compileGetDefinition(iVisited.getReceiverNode(), context);
            context.ifNull(isnull);

            context.rescue(new BranchCallback() {
                    public void branch(MethodCompiler context) {
                        ASTCompiler.compile(iVisited.getReceiverNode(), context); //[IRubyObject]
                        context.duplicateCurrentValue(); //[IRubyObject, IRubyObject]
                        context.metaclass(); //[IRubyObject, RubyClass]
                        context.duplicateCurrentValue(); //[IRubyObject, RubyClass, RubyClass]
                        context.getVisibilityFor(iVisited.getName()); //[IRubyObject, RubyClass, Visibility]
                        context.duplicateCurrentValue(); //[IRubyObject, RubyClass, Visibility, Visibility]
                        final Object isfalse = context.getNewEnding();
                        Object isreal = context.getNewEnding();
                        Object ending = context.getNewEnding();
                        context.isPrivate(isfalse,3); //[IRubyObject, RubyClass, Visibility]
                        context.isNotProtected(isreal,1); //[IRubyObject, RubyClass]
                        context.selfIsKindOf(isreal); //[IRubyObject]
                        context.consumeCurrentValue();
                        context.go(isfalse);
                        context.setEnding(isreal); //[]

                        context.isMethodBound(iVisited.getName(), new BranchCallback(){
                                public void branch(MethodCompiler context) {
                                    compileGetArgumentDefinition(iVisited.getArgsNode(), context, "assignment");
                                }
                            }, 
                            new BranchCallback(){
                                public void branch(MethodCompiler context) { 
                                    context.go(isfalse); 
                                }
                            });
                        context.go(ending);
                        context.setEnding(isfalse);
                        context.pushNull();
                        context.setEnding(ending);
                    }}, JumpException.class,
                new BranchCallback() {
                        public void branch(MethodCompiler context) {
                            context.pushNull();
                        }}, String.class);

            context.go(ending);
            context.setEnding(isnull);            
            context.pushNull();
            context.setEnding(ending);
            break;
        }
        default:
            context.rescue(new BranchCallback(){
                    public void branch(MethodCompiler context){
                        ASTCompiler.compile(node, context);
                        context.consumeCurrentValue();
                        context.pushNull();
                    }
                },JumpException.class, 
                new BranchCallback(){public void branch(MethodCompiler context){context.pushNull();}}, String.class);
            context.consumeCurrentValue();
            context.pushString("expression");
        }        
    }

    public static void compileDAsgn(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        DAsgnNode dasgnNode = (DAsgnNode)node;
        
        compile(dasgnNode.getValueNode(), context);
        
        compileDAsgnAssignment(dasgnNode, context);
    }

    public static void compileDAsgnAssignment(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        DAsgnNode dasgnNode = (DAsgnNode)node;
        
        context.getVariableCompiler().assignLocalVariable(dasgnNode.getIndex(), dasgnNode.getDepth());
    }
    
    public static void compileDefn(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final DefnNode defnNode = (DefnNode)node;
        final ArgsNode argsNode = defnNode.getArgsNode();
        
        ClosureCallback body = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (defnNode.getBodyNode() != null) {
                    ASTCompiler.compile(defnNode.getBodyNode(), context);
                } else {
                    context.loadNil();
                }
            }
        };
        
        ClosureCallback args = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                compileArgs(argsNode, context);
            }
        };
        
        // inspect body and args
        ASTInspector inspector = new ASTInspector();
        inspector.inspect(defnNode.getArgsNode());
        inspector.inspect(defnNode.getBodyNode());
        
        context.defineNewMethod(defnNode.getName(), defnNode.getScope(), body, args, null, inspector);
    }
    
    public static void compileDefs(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final DefsNode defsNode = (DefsNode)node;
        final ArgsNode argsNode = defsNode.getArgsNode();
        
        ClosureCallback receiver = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                ASTCompiler.compile(defsNode.getReceiverNode(), context);
            }
        };
        
        ClosureCallback body = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (defsNode.getBodyNode() != null) {
                    ASTCompiler.compile(defsNode.getBodyNode(), context);
                } else {
                    context.loadNil();
                }
            }
        };
        
        ClosureCallback args = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                compileArgs(argsNode, context);
            }
        };
        
        // inspect body and args
        ASTInspector inspector = new ASTInspector();
        inspector.inspect(defsNode.getArgsNode());
        inspector.inspect(defsNode.getBodyNode());
        
        context.defineNewMethod(defsNode.getName(), defsNode.getScope(), body, args, receiver, inspector);
    }
    
    public static void compileArgs(Node node, MethodCompiler context) {
        final ArgsNode argsNode = (ArgsNode)node;
        
        final int required = argsNode.getRequiredArgsCount();
        final int opt = argsNode.getOptionalArgsCount();
        final int rest = argsNode.getRestArg();
        
        ArrayCallback requiredAssignment = null;
        ArrayCallback optionalGiven = null;
        ArrayCallback optionalNotGiven = null;
        ClosureCallback restAssignment = null;
        ClosureCallback blockAssignment = null;
        
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
                    Node optArg = ((ListNode)object).get(index);

                    compileAssignment(optArg, context);
                }
            };
            optionalNotGiven = new ArrayCallback() {
                public void nextValue(MethodCompiler context, Object object, int index) {
                    Node optArg = ((ListNode)object).get(index);

                    compile(optArg, context);
                }
            };
        }
            
        if (rest > -1) {
            restAssignment = new ClosureCallback() {
                public void compile(MethodCompiler context) {
                    context.getVariableCompiler().assignLocalVariable(argsNode.getRestArg());
                }
            };
        }
        
        if (argsNode.getBlockArgNode() != null) {
            blockAssignment = new ClosureCallback() {
                public void compile(MethodCompiler context) {
                    context.getVariableCompiler().assignLocalVariable(argsNode.getBlockArgNode().getCount());
                }
            };
        }

        context.lineNumber(argsNode.getPosition());
        
        context.getVariableCompiler().checkMethodArity(required, opt, rest);
        context.getVariableCompiler().assignMethodArguments(
                argsNode.getArgs(),
                argsNode.getRequiredArgsCount(),
                argsNode.getOptArgs(),
                argsNode.getOptionalArgsCount(),
                requiredAssignment,
                optionalGiven,
                optionalNotGiven,
                restAssignment,
                blockAssignment);
    }
    
    public static void compileDot(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        DotNode dotNode = (DotNode)node;

        compile(dotNode.getBeginNode(), context);
        compile(dotNode.getEndNode(), context);
        
        context.createNewRange(dotNode.isExclusive());
    }
    
    public static void compileDRegexp(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());

        final DRegexpNode dregexpNode = (DRegexpNode)node;
        
        ClosureCallback createStringCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                ArrayCallback dstrCallback = new ArrayCallback() {
                public void nextValue(MethodCompiler context, Object sourceArray,
                                      int index) {
                        ASTCompiler.compile(dregexpNode.get(index), context);
                    }
                };
                context.createNewString(dstrCallback,dregexpNode.size());
                context.toJavaString();
            }
        };
   
        int opts = dregexpNode.getOptions();
        String lang = ((opts & 16) != 0) ? "n" : null;
        if((opts & 48) == 48) { // param s
            lang = "s";
        } else if((opts & 32) == 32) { // param e
            lang = "e";
        } else if((opts & 64) != 0) { // param s
            lang = "u";
        }
        
        context.createNewRegexp(createStringCallback, opts, lang);
    }
    
    public static void compileDStr(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());

        final DStrNode dstrNode = (DStrNode)node;
        
        ArrayCallback dstrCallback = new ArrayCallback() {
                public void nextValue(MethodCompiler context, Object sourceArray,
                                      int index) {
                    compile(dstrNode.get(index), context);
                }
            };
        context.createNewString(dstrCallback,dstrNode.size());
    }
    
    public static void compileDSymbol(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());

        final DSymbolNode dsymbolNode = (DSymbolNode)node;
        
        ArrayCallback dstrCallback = new ArrayCallback() {
                public void nextValue(MethodCompiler context, Object sourceArray,
                                      int index) {
                    compile(dsymbolNode.get(index), context);
                }
            };
        context.createNewSymbol(dstrCallback,dsymbolNode.size());
    }
    
    public static void compileDVar(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        DVarNode dvarNode = (DVarNode)node;
        
        context.getVariableCompiler().retrieveLocalVariable(dvarNode.getIndex(), dvarNode.getDepth());
    }
    
    public static void compileDXStr(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());

        final DXStrNode dxstrNode = (DXStrNode)node;
        
        final ArrayCallback dstrCallback = new ArrayCallback() {
            public void nextValue(MethodCompiler context, Object sourceArray,
                                  int index) {
                compile(dxstrNode.get(index), context);
            }
        };
        
        ClosureCallback argsCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                context.createNewString(dstrCallback,dxstrNode.size());
                context.createObjectArray(1);
            }
        };
        
        context.getInvocationCompiler().invokeDynamic("`", null, argsCallback, CallType.FUNCTIONAL, null, false);
    }
    
    public static void compileEnsureNode(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final EnsureNode ensureNode = (EnsureNode)node;
        
        if(ensureNode.getEnsureNode() != null) {
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

    public static void compileEvStr(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final EvStrNode evStrNode = (EvStrNode)node;
        
        compile(evStrNode.getBody(), context);
        context.asString();
    }
    
    public static void compileFalse(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        context.loadFalse();

        context.pollThreadEvents();
    }
    
    public static void compileFCall(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final FCallNode fcallNode = (FCallNode)node;
        
        ClosureCallback argsCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                compileArguments(fcallNode.getArgsNode(), context);
            }
        };
        
        if (fcallNode.getIterNode() == null) {
            // no block, go for simple version
            if (fcallNode.getArgsNode() != null) {
                context.getInvocationCompiler().invokeDynamic(fcallNode.getName(), null, argsCallback, CallType.FUNCTIONAL, null, false);
            } else {
                context.getInvocationCompiler().invokeDynamic(fcallNode.getName(), null, null, CallType.FUNCTIONAL, null, false);
            }
        } else {
            ClosureCallback closureArg = getBlock(fcallNode.getIterNode());

            if (fcallNode.getArgsNode() != null) {
                context.getInvocationCompiler().invokeDynamic(fcallNode.getName(), null, argsCallback, CallType.FUNCTIONAL, closureArg, false);
            } else {
                context.getInvocationCompiler().invokeDynamic(fcallNode.getName(), null, null, CallType.FUNCTIONAL, closureArg, false);
            }
        }
    }
    
    private static ClosureCallback getBlock(Node node) {
        if (node == null) return null;
        
        switch (node.nodeId) {
        case ITERNODE:
            final IterNode iterNode = (IterNode) node;

            return new ClosureCallback() {
                public void compile(MethodCompiler context) {
                    ASTCompiler.compile(iterNode, context);
                }
            };
        case BLOCKPASSNODE:
            final BlockPassNode blockPassNode = (BlockPassNode) node;

            return new ClosureCallback() {
                public void compile(MethodCompiler context) {
                    ASTCompiler.compile(blockPassNode.getBodyNode(), context);
                    context.unwrapPassedBlock();
                }
            };
        default:
            throw new NotCompilableException("ERROR: Encountered a method with a non-block, non-blockpass iter node at: " + node);
        }
    }

    public static void compileFixnum(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        FixnumNode fixnumNode = (FixnumNode)node;
        
        context.createNewFixnum(fixnumNode.getValue());
    }

    public static void compileFlip(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final FlipNode flipNode = (FlipNode)node;
        
        context.getVariableCompiler().retrieveLocalVariable(flipNode.getIndex(), flipNode.getDepth());
   
        if (flipNode.isExclusive()) {
            context.performBooleanBranch(
                    new BranchCallback() {
                        public void branch(MethodCompiler context) {
                            compile(flipNode.getEndNode(), context);
                            context.performBooleanBranch(
                                    new BranchCallback() {
                                        public void branch(MethodCompiler context) {
                                            context.loadFalse();
                                            context.getVariableCompiler().assignLocalVariable(flipNode.getIndex(), flipNode.getDepth());
                                            context.consumeCurrentValue();
                                        }
                                    }, new BranchCallback() {public void branch(MethodCompiler context) {}});
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
            context.performBooleanBranch(
                    new BranchCallback() {
                        public void branch(MethodCompiler context) {
                            compile(flipNode.getEndNode(), context);
                            context.performBooleanBranch(
                                    new BranchCallback() {
                                        public void branch(MethodCompiler context) {
                                            context.loadFalse();
                                            context.getVariableCompiler().assignLocalVariable(flipNode.getIndex(), flipNode.getDepth());
                                            context.consumeCurrentValue();
                                        }
                                    }, new BranchCallback() {public void branch(MethodCompiler context) {}});
                            context.loadTrue();
                        }
                    }, new BranchCallback() {
                        public void branch(MethodCompiler context) {
                            compile(flipNode.getBeginNode(), context);
                            context.performBooleanBranch(
                                    new BranchCallback() {
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
    
    private static void becomeTrueOrFalse(MethodCompiler context) {
        context.performBooleanBranch(
                new BranchCallback() {
                    public void branch(MethodCompiler context) {
                        context.loadTrue();
                    }
                }, new BranchCallback() {
                    public void branch(MethodCompiler context) {
                        context.loadFalse();
                    }
                });
    }
    
    private static void flipTrueOrFalse(MethodCompiler context) {
        context.performBooleanBranch(
                new BranchCallback() {
                    public void branch(MethodCompiler context) {
                        context.loadFalse();
                    }
                }, new BranchCallback() {
                    public void branch(MethodCompiler context) {
                        context.loadTrue();
                    }
                });
    }
    
    public static void compileFloat(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        FloatNode floatNode = (FloatNode)node;
        
        context.createNewFloat(floatNode.getValue());
    }
    
    public static void compileFor(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final ForNode forNode = (ForNode)node;
        
        ClosureCallback receiverCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                ASTCompiler.compile(forNode.getIterNode(), context);
            }
        };
           
        final ClosureCallback closureArg = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                compileForIter(forNode, context);
            }
        };

        context.getInvocationCompiler().invokeDynamic("each", receiverCallback, null, CallType.NORMAL, closureArg, false);
    }
    
    public static void compileForIter(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());

        final ForNode forNode = (ForNode)node;

        // create the closure class and instantiate it
        final ClosureCallback closureBody = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (forNode.getBodyNode() != null) {
                    ASTCompiler.compile(forNode.getBodyNode(), context);
                } else {
                    context.loadNil();
                }
            }
        };

        // create the closure class and instantiate it
        final ClosureCallback closureArgs = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (forNode.getVarNode() != null) {
                    compileAssignment(forNode.getVarNode(), context);
                }
            }
        };
        
        boolean hasMultipleArgsHead = false;
        if (forNode.getVarNode() instanceof MultipleAsgnNode) {
            hasMultipleArgsHead = ((MultipleAsgnNode)forNode.getVarNode()).getHeadNode() != null;
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
    
    public static void compileGlobalAsgn(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        GlobalAsgnNode globalAsgnNode = (GlobalAsgnNode)node;
        
        compile(globalAsgnNode.getValueNode(), context);
                
        if (globalAsgnNode.getName().length() == 2) {
            switch (globalAsgnNode.getName().charAt(1)) {
            case '_':
                context.getVariableCompiler().assignLastLine();
                return;
            case '~':
                assert false: "Parser shouldn't allow assigning to $~";
                return;
            default:
                // fall off the end, handle it as a normal global
            }
        }
        
        context.assignGlobalVariable(globalAsgnNode.getName());
    }
    
    public static void compileGlobalAsgnAssignment(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        GlobalAsgnNode globalAsgnNode = (GlobalAsgnNode)node;
                
        if (globalAsgnNode.getName().length() == 2) {
            switch (globalAsgnNode.getName().charAt(1)) {
            case '_':
                context.getVariableCompiler().assignLastLine();
                return;
            case '~':
                assert false: "Parser shouldn't allow assigning to $~";
                return;
            default:
                // fall off the end, handle it as a normal global
            }
        }
        
        context.assignGlobalVariable(globalAsgnNode.getName());
    }
    
    public static void compileGlobalVar(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        GlobalVarNode globalVarNode = (GlobalVarNode)node;
                
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
    
    public static void compileHash(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        HashNode hashNode = (HashNode)node;
        
        if (hashNode.getListNode() == null || hashNode.getListNode().size() == 0) {
            context.createEmptyHash();
            return;
        }
        
        ArrayCallback hashCallback = new ArrayCallback() {
            public void nextValue(MethodCompiler context, Object sourceArray,
                                  int index) {
                ListNode listNode = (ListNode)sourceArray;
                int keyIndex = index * 2;
                compile(listNode.get(keyIndex), context);
                compile(listNode.get(keyIndex + 1), context);
            }
        };
        
        context.createNewHash(hashNode.getListNode(), hashCallback, hashNode.getListNode().size() / 2);
    }
    
    public static void compileIf(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final IfNode ifNode = (IfNode)node;
        
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
    
    public static void compileInstAsgn(Node node, MethodCompiler context) {
        InstAsgnNode instAsgnNode = (InstAsgnNode)node;
        
        compile(instAsgnNode.getValueNode(), context);
        
        compileInstAsgnAssignment(node, context);
    }
    
    public static void compileInstAsgnAssignment(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        InstAsgnNode instAsgnNode = (InstAsgnNode)node;
        context.assignInstanceVariable(instAsgnNode.getName());
    }
    
    public static void compileInstVar(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        InstVarNode instVarNode = (InstVarNode)node;
        
        context.retrieveInstanceVariable(instVarNode.getName());
    }
    
    public static void compileIter(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());

        final IterNode iterNode = (IterNode)node;

        // create the closure class and instantiate it
        final ClosureCallback closureBody = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (iterNode.getBodyNode() != null) {
                    ASTCompiler.compile(iterNode.getBodyNode(), context);
                } else {
                    context.loadNil();
                }
            }
        };

        // create the closure class and instantiate it
        final ClosureCallback closureArgs = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (iterNode.getVarNode() != null) {
                    compileAssignment(iterNode.getVarNode(), context);
                }
            }
        };
        
        boolean hasMultipleArgsHead = false;
        if (iterNode.getVarNode() instanceof MultipleAsgnNode) {
            hasMultipleArgsHead = ((MultipleAsgnNode)iterNode.getVarNode()).getHeadNode() != null;
        }
        
        NodeType argsNodeId = null;
        if (iterNode.getVarNode() != null && iterNode.getVarNode().nodeId != NodeType.ZEROARGNODE) {
            argsNodeId = iterNode.getVarNode().nodeId;
        }
        
        ASTInspector inspector = new ASTInspector();
        inspector.inspect(iterNode.getBodyNode());
        inspector.inspect(iterNode.getVarNode());
        if (argsNodeId == null) {
            // no args, do not pass args processor
            context.createNewClosure(iterNode.getScope(), Arity.procArityOf(iterNode.getVarNode()).getValue(),
                    closureBody, null, hasMultipleArgsHead, argsNodeId, inspector);
        } else {
            context.createNewClosure(iterNode.getScope(), Arity.procArityOf(iterNode.getVarNode()).getValue(),
                    closureBody, closureArgs, hasMultipleArgsHead, argsNodeId, inspector);
        }
    }

    public static void compileLocalAsgn(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        LocalAsgnNode localAsgnNode = (LocalAsgnNode)node;
        
        compile(localAsgnNode.getValueNode(), context);
        
        context.getVariableCompiler().assignLocalVariable(localAsgnNode.getIndex(), localAsgnNode.getDepth());
    }

    public static void compileLocalAsgnAssignment(Node node, MethodCompiler context) {
        // "assignment" means the value is already on the stack
        context.lineNumber(node.getPosition());
        
        LocalAsgnNode localAsgnNode = (LocalAsgnNode)node;
        
        context.getVariableCompiler().assignLocalVariable(localAsgnNode.getIndex(), localAsgnNode.getDepth());
    }
    
    public static void compileLocalVar(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        LocalVarNode localVarNode = (LocalVarNode)node;
        
        context.getVariableCompiler().retrieveLocalVariable(localVarNode.getIndex(), localVarNode.getDepth());
    }
    
    public static void compileMatch(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        MatchNode matchNode = (MatchNode)node;

        compile(matchNode.getRegexpNode(), context);
        
        context.match();
    }
    
    public static void compileMatch2(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        Match2Node matchNode = (Match2Node)node;

        compile(matchNode.getReceiverNode(), context);
        compile(matchNode.getValueNode(), context);
        
        context.match2();
    }
    public static void compileMatch3(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        Match3Node matchNode = (Match3Node)node;

        compile(matchNode.getReceiverNode(), context);
        compile(matchNode.getValueNode(), context);
        
        context.match3();
    }
    
    public static void compileModule(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final ModuleNode moduleNode = (ModuleNode)node;
        
        final Node cpathNode = moduleNode.getCPath();
        
        ClosureCallback bodyCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (moduleNode.getBodyNode() != null) {
                    ASTCompiler.compile(moduleNode.getBodyNode(), context);
                }
                context.loadNil();
            }
        };
        
        ClosureCallback pathCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (cpathNode instanceof Colon2Node) {
                    Node leftNode = ((Colon2Node)cpathNode).getLeftNode();
                    if (leftNode != null) {
                        ASTCompiler.compile(leftNode, context);
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
    
    public static void compileMultipleAsgn(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        MultipleAsgnNode multipleAsgnNode = (MultipleAsgnNode)node;
        
        // FIXME: This is a little less efficient than it could be, since in the interpreter we avoid objectspace for these arrays
        compile(multipleAsgnNode.getValueNode(), context);
        
        compileMultipleAsgnAssignment(node, context);
    }
    
    public static void compileMultipleAsgnAssignment(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final MultipleAsgnNode multipleAsgnNode = (MultipleAsgnNode)node;
        
        context.ensureMultipleAssignableRubyArray(multipleAsgnNode.getHeadNode() != null);
        
        // normal items at the "head" of the masgn
        ArrayCallback headAssignCallback = new ArrayCallback() {
            public void nextValue(MethodCompiler context, Object sourceArray,
                                  int index) {
                ListNode headNode = (ListNode)sourceArray;
                Node assignNode = headNode.get(index);

                // perform assignment for the next node
                compileAssignment(assignNode, context);
            }
        };

        // head items for which we've run out of assignable elements
        ArrayCallback headNilCallback = new ArrayCallback() {
            public void nextValue(MethodCompiler context, Object sourceArray,
                                  int index) {
                ListNode headNode = (ListNode)sourceArray;
                Node assignNode = headNode.get(index);

                // perform assignment for the next node
                context.loadNil();
                compileAssignment(assignNode, context);
            }
        };

        ClosureCallback argsCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                Node argsNode = multipleAsgnNode.getArgsNode();
                if (argsNode instanceof StarNode) {
                    // done processing args
                } else {
                    // assign to appropriate variable
                    ASTCompiler.compileAssignment(argsNode, context);
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

    public static void compileNewline(Node node, MethodCompiler context) {
        // TODO: add trace call?
        context.lineNumber(node.getPosition());
        
        context.setPosition(node.getPosition());
        
        NewlineNode newlineNode = (NewlineNode)node;
        
        compile(newlineNode.getNextNode(), context);
    }
    
    public static void compileNext(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final NextNode nextNode = (NextNode)node;
        
        ClosureCallback valueCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (nextNode.getValueNode() != null) {
                    ASTCompiler.compile(nextNode.getValueNode(), context);
                } else {
                    context.loadNil();
                }
            }
        };
        
        context.pollThreadEvents();
        context.issueNextEvent(valueCallback);
    }
    public static void compileNthRef(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        NthRefNode nthRefNode = (NthRefNode)node;
        
        context.nthRef(nthRefNode.getMatchNumber());
    }
    
    public static void compileNil(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        context.loadNil();
        
        context.pollThreadEvents();
    }
    
    public static void compileNot(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        NotNode notNode = (NotNode)node;
        
        compile(notNode.getConditionNode(), context);
        
        context.negateCurrentValue();
    }
    
    public static void compileOpAsgnAnd(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final BinaryOperatorNode andNode = (BinaryOperatorNode)node;
        
        compile(andNode.getFirstNode(), context);
        
        BranchCallback longCallback = new BranchCallback() {
            public void branch(MethodCompiler context) {
                compile(andNode.getSecondNode(), context);
            }
        };
        
        context.performLogicalAnd(longCallback);
        context.pollThreadEvents();
    }

    public static void compileOpAsgnOr(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final OpAsgnOrNode orNode = (OpAsgnOrNode)node;

        compileGetDefinitionBase(orNode.getFirstNode(), context);

        context.isNull(new BranchCallback() {
                public void branch(MethodCompiler context) {
                    compile(orNode.getSecondNode(), context);
                }}, new BranchCallback() {
                        public void branch(MethodCompiler context) {
                            compile(orNode.getFirstNode(), context);
                            context.duplicateCurrentValue();
                            context.performBooleanBranch(
                                                         new BranchCallback() {
                                                             public void branch(MethodCompiler context) {
                                                                 //Do nothing
                                                             }},
                                                         new BranchCallback() {
                                                             public void branch(MethodCompiler context) {
                                                                 context.consumeCurrentValue();
                                                                 compile(orNode.getSecondNode(), context);
                                                             }}
                                                         );
                        }});

        context.pollThreadEvents();
    }

    public static void compileOpAsgn(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        // FIXME: This is a little more complicated than it needs to be; do we see now why closures would be nice in Java?
        
        final OpAsgnNode opAsgnNode = (OpAsgnNode)node;
        
        final ClosureCallback receiverCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                ASTCompiler.compile(opAsgnNode.getReceiverNode(), context); // [recv]
                context.duplicateCurrentValue(); // [recv, recv]
            }
        };
        
        BranchCallback doneBranch = new BranchCallback() {
            public void branch(MethodCompiler context) {
                // get rid of extra receiver, leave the variable result present
                context.swapValues();
                context.consumeCurrentValue();
            }
        };
        
        // Just evaluate the value and stuff it in an argument array
        final ArrayCallback justEvalValue = new ArrayCallback() {
            public void nextValue(MethodCompiler context, Object sourceArray,
                    int index) {
                compile(((Node[])sourceArray)[index], context);
            }
        };
        
        BranchCallback assignBranch = new BranchCallback() {
            public void branch(MethodCompiler context) {
                // eliminate extra value, eval new one and assign
                context.consumeCurrentValue();
                context.createObjectArray(new Node[] {opAsgnNode.getValueNode()}, justEvalValue);
                context.getInvocationCompiler().invokeAttrAssign(opAsgnNode.getVariableNameAsgn());
            }
        };
        
        ClosureCallback receiver2Callback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                context.getInvocationCompiler().invokeDynamic(opAsgnNode.getVariableName(), receiverCallback, null, CallType.FUNCTIONAL, null, false);
            }
        };
        
        if (opAsgnNode.getOperatorName() == "||") {
            // if lhs is true, don't eval rhs and assign
            receiver2Callback.compile(context);
            context.duplicateCurrentValue();
            context.performBooleanBranch(doneBranch, assignBranch);
        } else if (opAsgnNode.getOperatorName() == "&&") {
            // if lhs is true, eval rhs and assign
            receiver2Callback.compile(context);
            context.duplicateCurrentValue();
            context.performBooleanBranch(assignBranch, doneBranch);
        } else {
            // eval new value, call operator on old value, and assign
            ClosureCallback argsCallback = new ClosureCallback() {
                public void compile(MethodCompiler context) {
                    context.createObjectArray(new Node[] {opAsgnNode.getValueNode()}, justEvalValue);
                }
            };
            context.getInvocationCompiler().invokeDynamic(opAsgnNode.getOperatorName(), receiver2Callback, argsCallback, CallType.FUNCTIONAL, null, false);
            context.createObjectArray(1);
            context.getInvocationCompiler().invokeAttrAssign(opAsgnNode.getVariableNameAsgn());
        }

        context.pollThreadEvents();
    }
    
    public static void compileOpElementAsgn(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode)node;
        
        compile(opElementAsgnNode.getReceiverNode(), context);
        compileArguments(opElementAsgnNode.getArgsNode(), context);
        
        ClosureCallback valueArgsCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                ASTCompiler.compile(opElementAsgnNode.getValueNode(), context);
            }
        };
        
        context.getInvocationCompiler().opElementAsgn(valueArgsCallback, opElementAsgnNode.getOperatorName());
    }
    
    public static void compileOr(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final OrNode orNode = (OrNode)node;
        
        compile(orNode.getFirstNode(), context);
        
        BranchCallback longCallback = new BranchCallback() {
            public void branch(MethodCompiler context) {
                compile(orNode.getSecondNode(), context);
            }
        };
        
        context.performLogicalOr(longCallback);
    }
    
    public static void compilePostExe(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final PostExeNode postExeNode = (PostExeNode)node;

        // create the closure class and instantiate it
        final ClosureCallback closureBody = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (postExeNode.getBodyNode() != null) {
                    ASTCompiler.compile(postExeNode.getBodyNode(), context);
                } else {
                    context.loadNil();
                }
            }
        };
        context.createNewEndBlock(closureBody);
    }
    
    public static void compilePreExe(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final PreExeNode preExeNode = (PreExeNode)node;

        // create the closure class and instantiate it
        final ClosureCallback closureBody = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                if (preExeNode.getBodyNode() != null) {
                    ASTCompiler.compile(preExeNode.getBodyNode(), context);
                } else {
                    context.loadNil();
                }
            }
        };
        context.runBeginBlock(preExeNode.getScope(), closureBody);
    }
    
    public static void compileRedo(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        //RedoNode redoNode = (RedoNode)node;
        
        context.issueRedoEvent();
    }
    
    public static void compileRegexp(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        RegexpNode reNode = (RegexpNode)node;
        
        int opts = reNode.getOptions();
        String lang = ((opts & 16) == 16) ? "n" : null;
        if((opts & 48) == 48) { // param s
            lang = "s";
        } else if((opts & 32) == 32) { // param e
            lang = "e";
        } else if((opts & 64) != 0) { // param u
            lang = "u";
        }

        context.createNewRegexp(reNode.getValue(), reNode.getOptions(), lang);
    }
    
    public static void compileRescue(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final RescueNode rescueNode = (RescueNode)node;
        
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
    
    public static void compileRescueBody(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final RescueBodyNode rescueBodyNode = (RescueBodyNode)node;
        
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
    
    public static void compileRetry(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        context.pollThreadEvents();
        
        context.issueRetryEvent();
    }
    
    public static void compileReturn(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        ReturnNode returnNode = (ReturnNode)node;
        
        if (returnNode.getValueNode() != null) {
            compile(returnNode.getValueNode(), context);
        } else {
            context.loadNil();
        }
        
        context.performReturn();
    }
    
    public static void compileRoot(Node node, ScriptCompiler context, ASTInspector inspector) {
        RootNode rootNode = (RootNode)node;
        
        context.startScript(rootNode.getStaticScope());
        
        // create method for toplevel of script
        MethodCompiler methodCompiler = context.startMethod("__file__", null, rootNode.getStaticScope(), inspector);

        Node nextNode = rootNode.getBodyNode();
        if (nextNode != null) {
            if (nextNode.nodeId == NodeType.BLOCKNODE) {
                // it's a multiple-statement body, iterate over all elements in turn and chain if it get too long
                BlockNode blockNode = (BlockNode)nextNode;
                
                for (int i = 0; i < blockNode.size(); i++) {
                    if ((i + 1) % 500 == 0) {
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
        
        context.endScript();
    }
    
    public static void compileSelf(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        context.retrieveSelf();
    }    
    
    public static void compileSplat(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        SplatNode splatNode = (SplatNode)node;
        
        compile(splatNode.getValue(), context);
        
        context.splatCurrentValue();
    }
    
    public static void compileStr(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        StrNode strNode = (StrNode)node;
        
        context.createNewString(strNode.getValue());
    }
    
    public static void compileSuper(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final SuperNode superNode = (SuperNode)node;
        
        ClosureCallback argsCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
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
            ClosureCallback closureArg = getBlock(superNode.getIterNode());
            
            if (superNode.getArgsNode() != null) {
                context.getInvocationCompiler().invokeSuper(argsCallback, closureArg);
            } else {
                context.getInvocationCompiler().invokeSuper(null, closureArg);
            }
        }
    }
    
    public static void compileSValue(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        SValueNode svalueNode = (SValueNode)node;
        
        compile(svalueNode.getValue(), context);
        
        context.singlifySplattedValue();
    }
    
    public static void compileSymbol(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        context.createNewSymbol(((SymbolNode)node).getName());
    }    
    
    public static void compileToAry(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());

        ToAryNode toAryNode = (ToAryNode)node;

        compile(toAryNode.getValue(), context);

        context.aryToAry();
    }    
    
    public static void compileTrue(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        context.loadTrue();
        
        context.pollThreadEvents();
    }
    
    public static void compileUndef(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        context.undefMethod(((UndefNode)node).getName());
    }
    
    public static void compileUntil(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final UntilNode untilNode = (UntilNode)node;
        
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
        
        context.performBooleanLoop(condition, body, untilNode.evaluateAtStart());
        
        context.pollThreadEvents();
    }

    public static void compileVAlias(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        VAliasNode valiasNode = (VAliasNode)node;
        
        context.aliasGlobal(valiasNode.getNewName(), valiasNode.getOldName());
    }

    public static void compileVCall(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        VCallNode vcallNode = (VCallNode)node;
        
        context.getInvocationCompiler().invokeDynamic(vcallNode.getName(), null, null, CallType.VARIABLE, null, false);
    }
    
    public static void compileWhile(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final WhileNode whileNode = (WhileNode)node;
        
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
        
        context.performBooleanLoop(condition, body, whileNode.evaluateAtStart());
        
        context.pollThreadEvents();
    }
    
    public static void compileXStr(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        final XStrNode xstrNode = (XStrNode)node;
        
        ClosureCallback argsCallback = new ClosureCallback() {
            public void compile(MethodCompiler context) {
                context.createNewString(xstrNode.getValue());
                context.createObjectArray(1);
            }
        };
        context.getInvocationCompiler().invokeDynamic("`", null, argsCallback, CallType.FUNCTIONAL, null, false);
    }
    
    public static void compileYield(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        YieldNode yieldNode = (YieldNode)node;
        
        if (yieldNode.getArgsNode() != null) {
            compile(yieldNode.getArgsNode(), context);
        }
        
        context.getInvocationCompiler().yield(yieldNode.getArgsNode() != null, yieldNode.getCheckState());
    }
    
    public static void compileZArray(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        context.createEmptyArray();
    }
    
    public static void compileZSuper(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        ZSuperNode zsuperNode = (ZSuperNode)node;
        
        ClosureCallback closure = getBlock(zsuperNode.getIterNode());
        
        context.callZSuper(closure);
    }
    
    public static void compileArgsCatArguments(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        ArgsCatNode argsCatNode = (ArgsCatNode)node;
        
        compileArguments(argsCatNode.getFirstNode(), context);
        // arguments compilers always create IRubyObject[], but we want to use RubyArray.concat here;
        // FIXME: as a result, this is NOT efficient, since it creates and then later unwraps an array
        context.createNewArray(true);
        compile(argsCatNode.getSecondNode(), context);
        context.splatCurrentValue();
        context.concatArrays();
        context.convertToJavaArray();
    }
    
    public static void compileArgsPushArguments(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        ArgsPushNode argsPushNode = (ArgsPushNode)node;
        compile(argsPushNode.getFirstNode(), context);
        compile(argsPushNode.getSecondNode(), context);
        context.appendToArray();
        context.convertToJavaArray();
    }
    
    public static void compileArrayArguments(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        ArrayNode arrayNode = (ArrayNode)node;
        
        ArrayCallback callback = new ArrayCallback() {
            public void nextValue(MethodCompiler context, Object sourceArray, int index) {
                Node node = (Node)((Object[])sourceArray)[index];
                compile(node, context);
            }
        };
        
        context.createObjectArray(arrayNode.childNodes().toArray(), callback);
        // leave as a normal array
    }
    
    public static void compileSplatArguments(Node node, MethodCompiler context) {
        context.lineNumber(node.getPosition());
        
        SplatNode splatNode = (SplatNode)node;
        
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
            ArgsNode argsNode = (ArgsNode)node;
            // FIXME: We can't compile cases like def(a=(b=1)) because the variables
            // in the arg list get ordered differently than you might expect (b comes first)
            // So the code below searches through all opt args, ensuring none of them skip
            // indicies. A skipped index means there's a hidden local var/arg like b above
            // and so we shouldn't try to compile.
            if (argsNode.getOptArgs() != null && argsNode.getOptArgs().size() > 0) {
                int index = argsNode.getRequiredArgsCount() - 1;
                
                for (int i = 0; i < argsNode.getOptArgs().size(); i++) {
                    int newIndex = ((LocalAsgnNode)argsNode.getOptArgs().get(i)).getIndex();
                    
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
