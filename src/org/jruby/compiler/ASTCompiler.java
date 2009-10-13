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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jruby.RubyInstanceConfig;
import org.jruby.RubyMatchData;
import org.jruby.ast.AliasNode;
import org.jruby.ast.AndNode;
import org.jruby.ast.ArgsCatNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArgsPushNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.AttrAssignNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BignumNode;
import org.jruby.ast.BinaryOperatorNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.BreakNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.CaseNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.Colon2ConstNode;
import org.jruby.ast.Colon2MethodNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.Colon3Node;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.ConstNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.DSymbolNode;
import org.jruby.ast.DVarNode;
import org.jruby.ast.DXStrNode;
import org.jruby.ast.DefinedNode;
import org.jruby.ast.DefnNode;
import org.jruby.ast.DefsNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.EnsureNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.FileNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FlipNode;
import org.jruby.ast.FloatNode;
import org.jruby.ast.ForNode;
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
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.NilNode;
import org.jruby.ast.Node;
import org.jruby.ast.NodeType;
import org.jruby.ast.NotNode;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.OrNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.PreExeNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.RootNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SValueNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.StarNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SuperNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.ToAryNode;
import org.jruby.ast.UndefNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.WhenNode;
import org.jruby.ast.WhenOneArgNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.exceptions.JumpException;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.CallType;

/**
 *
 * @author headius
 */
public class ASTCompiler {
    private boolean isAtRoot = true;
    
    public void compile(Node node, BodyCompiler context, boolean expr) {
        if (node == null) {
            if (expr) context.loadNil();
            return;
        }
        switch (node.getNodeType()) {
            case ALIASNODE:
                compileAlias(node, context, expr);
                break;
            case ANDNODE:
                compileAnd(node, context, expr);
                break;
            case ARGSCATNODE:
                compileArgsCat(node, context, expr);
                break;
            case ARGSPUSHNODE:
                compileArgsPush(node, context, expr);
                break;
            case ARRAYNODE:
                compileArray(node, context, expr);
                break;
            case ATTRASSIGNNODE:
                compileAttrAssign(node, context, expr);
                break;
            case BACKREFNODE:
                compileBackref(node, context, expr);
                break;
            case BEGINNODE:
                compileBegin(node, context, expr);
                break;
            case BIGNUMNODE:
                compileBignum(node, context, expr);
                break;
            case BLOCKNODE:
                compileBlock(node, context, expr);
                break;
            case BREAKNODE:
                compileBreak(node, context, expr);
                break;
            case CALLNODE:
                compileCall(node, context, expr);
                break;
            case CASENODE:
                compileCase(node, context, expr);
                break;
            case CLASSNODE:
                compileClass(node, context, expr);
                break;
            case CLASSVARNODE:
                compileClassVar(node, context, expr);
                break;
            case CLASSVARASGNNODE:
                compileClassVarAsgn(node, context, expr);
                break;
            case CLASSVARDECLNODE:
                compileClassVarDecl(node, context, expr);
                break;
            case COLON2NODE:
                compileColon2(node, context, expr);
                break;
            case COLON3NODE:
                compileColon3(node, context, expr);
                break;
            case CONSTDECLNODE:
                compileConstDecl(node, context, expr);
                break;
            case CONSTNODE:
                compileConst(node, context, expr);
                break;
            case DASGNNODE:
                compileDAsgn(node, context, expr);
                break;
            case DEFINEDNODE:
                compileDefined(node, context, expr);
                break;
            case DEFNNODE:
                compileDefn(node, context, expr);
                break;
            case DEFSNODE:
                compileDefs(node, context, expr);
                break;
            case DOTNODE:
                compileDot(node, context, expr);
                break;
            case DREGEXPNODE:
                compileDRegexp(node, context, expr);
                break;
            case DSTRNODE:
                compileDStr(node, context, expr);
                break;
            case DSYMBOLNODE:
                compileDSymbol(node, context, expr);
                break;
            case DVARNODE:
                compileDVar(node, context, expr);
                break;
            case DXSTRNODE:
                compileDXStr(node, context, expr);
                break;
            case ENSURENODE:
                compileEnsureNode(node, context, expr);
                break;
            case EVSTRNODE:
                compileEvStr(node, context, expr);
                break;
            case FALSENODE:
                compileFalse(node, context, expr);
                break;
            case FCALLNODE:
                compileFCall(node, context, expr);
                break;
            case FIXNUMNODE:
                compileFixnum(node, context, expr);
                break;
            case FLIPNODE:
                compileFlip(node, context, expr);
                break;
            case FLOATNODE:
                compileFloat(node, context, expr);
                break;
            case FORNODE:
                compileFor(node, context, expr);
                break;
            case GLOBALASGNNODE:
                compileGlobalAsgn(node, context, expr);
                break;
            case GLOBALVARNODE:
                compileGlobalVar(node, context, expr);
                break;
            case HASHNODE:
                compileHash(node, context, expr);
                break;
            case IFNODE:
                compileIf(node, context, expr);
                break;
            case INSTASGNNODE:
                compileInstAsgn(node, context, expr);
                break;
            case INSTVARNODE:
                compileInstVar(node, context, expr);
                break;
            case ITERNODE:
                compileIter(node, context);
                break;
            case LOCALASGNNODE:
                compileLocalAsgn(node, context, expr);
                break;
            case LOCALVARNODE:
                compileLocalVar(node, context, expr);
                break;
            case MATCH2NODE:
                compileMatch2(node, context, expr);
                break;
            case MATCH3NODE:
                compileMatch3(node, context, expr);
                break;
            case MATCHNODE:
                compileMatch(node, context, expr);
                break;
            case MODULENODE:
                compileModule(node, context, expr);
                break;
            case MULTIPLEASGNNODE:
                compileMultipleAsgn(node, context, expr);
                break;
            case NEWLINENODE:
                compileNewline(node, context, expr);
                break;
            case NEXTNODE:
                compileNext(node, context, expr);
                break;
            case NTHREFNODE:
                compileNthRef(node, context, expr);
                break;
            case NILNODE:
                compileNil(node, context, expr);
                break;
            case NOTNODE:
                compileNot(node, context, expr);
                break;
            case OPASGNANDNODE:
                compileOpAsgnAnd(node, context, expr);
                break;
            case OPASGNNODE:
                compileOpAsgn(node, context, expr);
                break;
            case OPASGNORNODE:
                compileOpAsgnOr(node, context, expr);
                break;
            case OPELEMENTASGNNODE:
                compileOpElementAsgn(node, context, expr);
                break;
            case ORNODE:
                compileOr(node, context, expr);
                break;
            case POSTEXENODE:
                compilePostExe(node, context, expr);
                break;
            case PREEXENODE:
                compilePreExe(node, context, expr);
                break;
            case REDONODE:
                compileRedo(node, context, expr);
                break;
            case REGEXPNODE:
                compileRegexp(node, context, expr);
                break;
            case RESCUEBODYNODE:
                throw new NotCompilableException("rescue body is handled by rescue compilation at: " + node.getPosition());
            case RESCUENODE:
                compileRescue(node, context, expr);
                break;
            case RETRYNODE:
                compileRetry(node, context, expr);
                break;
            case RETURNNODE:
                compileReturn(node, context, expr);
                break;
            case ROOTNODE:
                throw new NotCompilableException("Use compileRoot(); Root node at: " + node.getPosition());
            case SCLASSNODE:
                compileSClass(node, context, expr);
                break;
            case SELFNODE:
                compileSelf(node, context, expr);
                break;
            case SPLATNODE:
                compileSplat(node, context, expr);
                break;
            case STRNODE:
                compileStr(node, context, expr);
                break;
            case SUPERNODE:
                compileSuper(node, context, expr);
                break;
            case SVALUENODE:
                compileSValue(node, context, expr);
                break;
            case SYMBOLNODE:
                compileSymbol(node, context, expr);
                break;
            case TOARYNODE:
                compileToAry(node, context, expr);
                break;
            case TRUENODE:
                compileTrue(node, context, expr);
                break;
            case UNDEFNODE:
                compileUndef(node, context, expr);
                break;
            case UNTILNODE:
                compileUntil(node, context, expr);
                break;
            case VALIASNODE:
                compileVAlias(node, context, expr);
                break;
            case VCALLNODE:
                compileVCall(node, context, expr);
                break;
            case WHILENODE:
                compileWhile(node, context, expr);
                break;
            case WHENNODE:
                assert false : "When nodes are handled by case node compilation.";
                break;
            case XSTRNODE:
                compileXStr(node, context, expr);
                break;
            case YIELDNODE:
                compileYield(node, context, expr);
                break;
            case ZARRAYNODE:
                compileZArray(node, context, expr);
                break;
            case ZSUPERNODE:
                compileZSuper(node, context, expr);
                break;
            default:
                throw new NotCompilableException("Unknown node encountered in compiler: " + node);
        }
    }

    public void compileArguments(Node node, BodyCompiler context) {
        switch (node.getNodeType()) {
            case ARGSCATNODE:
                compileArgsCatArguments(node, context, true);
                break;
            case ARGSPUSHNODE:
                compileArgsPushArguments(node, context, true);
                break;
            case ARRAYNODE:
                compileArrayArguments(node, context, true);
                break;
            case SPLATNODE:
                compileSplatArguments(node, context, true);
                break;
            default:
                compile(node, context, true);
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
        
        public void call(BodyCompiler context) {
            compileArguments(node, context);
        }
    }
    
    public class SpecificArityArguments implements ArgumentsCallback {
        private int arity;
        private Node node;
        
        public SpecificArityArguments(Node node) {
            if (node.getNodeType() == NodeType.ARRAYNODE && ((ArrayNode)node).isLightweight()) {
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
        
        public void call(BodyCompiler context) {
            if (node.getNodeType() == NodeType.ARRAYNODE) {
                ArrayNode arrayNode = (ArrayNode)node;
                if (arrayNode.isLightweight()) {
                    // explode array, it's an internal "args" array
                    for (Node n : arrayNode.childNodes()) {
                        compile(n, context,true);
                    }
                } else {
                    // use array as-is, it's a literal array
                    compile(arrayNode, context,true);
                }
            } else {
                compile(node, context,true);
            }
        }
    }

    public ArgumentsCallback getArgsCallback(Node node) {
        if (node == null) {
            return null;
        }
        // unwrap newline nodes to get their actual type
        while (node.getNodeType() == NodeType.NEWLINENODE) {
            node = ((NewlineNode)node).getNextNode();
        }
        switch (node.getNodeType()) {
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

    public void compileAssignment(Node node, BodyCompiler context, boolean expr) {
        switch (node.getNodeType()) {
            case ATTRASSIGNNODE:
                compileAttrAssignAssignment(node, context, expr);
                break;
            case DASGNNODE:
                DAsgnNode dasgnNode = (DAsgnNode)node;
                context.getVariableCompiler().assignLocalVariable(dasgnNode.getIndex(), dasgnNode.getDepth(), expr);
                break;
            case CLASSVARASGNNODE:
                compileClassVarAsgnAssignment(node, context, expr);
                break;
            case CLASSVARDECLNODE:
                compileClassVarDeclAssignment(node, context, expr);
                break;
            case CONSTDECLNODE:
                compileConstDeclAssignment(node, context, expr);
                break;
            case GLOBALASGNNODE:
                compileGlobalAsgnAssignment(node, context, expr);
                break;
            case INSTASGNNODE:
                compileInstAsgnAssignment(node, context, expr);
                break;
            case LOCALASGNNODE:
                LocalAsgnNode localAsgnNode = (LocalAsgnNode)node;
                context.getVariableCompiler().assignLocalVariable(localAsgnNode.getIndex(), localAsgnNode.getDepth(), expr);
                break;
            case MULTIPLEASGNNODE:
                compileMultipleAsgnAssignment(node, context, expr);
                break;
            case ZEROARGNODE:
                throw new NotCompilableException("Shouldn't get here; zeroarg does not do assignment: " + node);
            default:
                throw new NotCompilableException("Can't compile assignment node: " + node);
        }
    }

    public void compileAlias(Node node, BodyCompiler context, boolean expr) {
        final AliasNode alias = (AliasNode) node;

        context.defineAlias(alias.getNewName(), alias.getOldName());
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileAnd(Node node, BodyCompiler context, final boolean expr) {
        final AndNode andNode = (AndNode) node;

        if (andNode.getFirstNode().getNodeType().alwaysTrue()) {
            // compile first node as non-expr and then second node
            compile(andNode.getFirstNode(), context, false);
            compile(andNode.getSecondNode(), context, expr);
        } else if (andNode.getFirstNode().getNodeType().alwaysFalse()) {
            // compile first node only
            compile(andNode.getFirstNode(), context, expr);
        } else {
            compile(andNode.getFirstNode(), context, true);
            BranchCallback longCallback = new BranchCallback() {
                        public void branch(BodyCompiler context) {
                            compile(andNode.getSecondNode(), context, true);
                        }
                    };

            context.performLogicalAnd(longCallback);
            if (!expr) context.consumeCurrentValue();
        }
    }

    public void compileArray(Node node, BodyCompiler context, boolean expr) {
        ArrayNode arrayNode = (ArrayNode) node;

        boolean doit = expr || !RubyInstanceConfig.PEEPHOLE_OPTZ;
        boolean popit = !RubyInstanceConfig.PEEPHOLE_OPTZ && !expr;
        
        if (doit) {
            ArrayCallback callback = new ArrayCallback() {

                        public void nextValue(BodyCompiler context, Object sourceArray, int index) {
                            Node node = (Node) ((Object[]) sourceArray)[index];
                            compile(node, context, true);
                        }
                    };

            context.createNewArray(arrayNode.childNodes().toArray(), callback, arrayNode.isLightweight());

            if (popit) context.consumeCurrentValue();
        } else {
            for (Iterator<Node> iter = arrayNode.childNodes().iterator(); iter.hasNext();) {
                Node nextNode = iter.next();
                compile(nextNode, context, false);
            }
        }
    }

    public void compileArgsCat(Node node, BodyCompiler context, boolean expr) {
        ArgsCatNode argsCatNode = (ArgsCatNode) node;

        compile(argsCatNode.getFirstNode(), context,true);
        context.ensureRubyArray();
        compile(argsCatNode.getSecondNode(), context,true);
        context.splatCurrentValue();
        context.concatArrays();
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileArgsPush(Node node, BodyCompiler context, boolean expr) {
        throw new NotCompilableException("ArgsPush should never be encountered bare in 1.8");
    }

    private void compileAttrAssign(Node node, BodyCompiler context, boolean expr) {
        final AttrAssignNode attrAssignNode = (AttrAssignNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {
            public void call(BodyCompiler context) {
                compile(attrAssignNode.getReceiverNode(), context,true);
            }
        };
        
        ArgumentsCallback argsCallback = getArgsCallback(attrAssignNode.getArgsNode());

        context.getInvocationCompiler().invokeAttrAssign(attrAssignNode.getName(), receiverCallback, argsCallback);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileAttrAssignAssignment(Node node, BodyCompiler context, boolean expr) {
        final AttrAssignNode attrAssignNode = (AttrAssignNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {
            public void call(BodyCompiler context) {
                compile(attrAssignNode.getReceiverNode(), context,true);
            }
        };
        ArgumentsCallback argsCallback = getArgsCallback(attrAssignNode.getArgsNode());

        context.getInvocationCompiler().invokeAttrAssignMasgn(attrAssignNode.getName(), receiverCallback, argsCallback);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileBackref(Node node, BodyCompiler context, boolean expr) {
        BackRefNode iVisited = (BackRefNode) node;

        context.performBackref(iVisited.getType());
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileBegin(Node node, BodyCompiler context, boolean expr) {
        BeginNode beginNode = (BeginNode) node;

        compile(beginNode.getBodyNode(), context, expr);
    }

    public void compileBignum(Node node, BodyCompiler context, boolean expr) {
        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) context.createNewBignum(((BignumNode) node).getValue());
        } else {
            context.createNewBignum(((BignumNode) node).getValue());
            if (!expr) context.consumeCurrentValue();
        }
    }

    public void compileBlock(Node node, BodyCompiler context, boolean expr) {
        BlockNode blockNode = (BlockNode) node;

        for (Iterator<Node> iter = blockNode.childNodes().iterator(); iter.hasNext();) {
            Node n = iter.next();

            compile(n, context, iter.hasNext() ? false : expr);
        }
    }

    public void compileBreak(Node node, BodyCompiler context, boolean expr) {
        final BreakNode breakNode = (BreakNode) node;

        CompilerCallback valueCallback = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        if (breakNode.getValueNode() != null) {
                            compile(breakNode.getValueNode(), context, true);
                        } else {
                            context.loadNil();
                        }
                    }
                };

        context.issueBreakEvent(valueCallback);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileCall(Node node, BodyCompiler context, boolean expr) {
        final CallNode callNode = (CallNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {
            public void call(BodyCompiler context) {
                compile(callNode.getReceiverNode(), context, true);
            }
        };

        ArgumentsCallback argsCallback = getArgsCallback(callNode.getArgsNode());
        CompilerCallback closureArg = getBlock(callNode.getIterNode());

        String name = callNode.getName();
        CallType callType = CallType.NORMAL;

        if (argsCallback != null && argsCallback.getArity() == 1) {
            Node argument = callNode.getArgsNode().childNodes().get(0);
            if (name.length() == 1) {
                switch (name.charAt(0)) {
                case '+': case '-': case '*': case '/': case '<': case '>':
                    if (argument instanceof FixnumNode) {
                        context.getInvocationCompiler().invokeBinaryFixnumRHS(name, receiverCallback, ((FixnumNode)argument).getValue());
                        if (!expr) context.consumeCurrentValue();
                        return;
                    }
                }
            } else if (name.length() == 2) {
                if (argument instanceof FixnumNode) {
                    switch (name.charAt(0)) {
                    case '<': case '>': case '=': case '[':
                        switch (name.charAt(1)) {
                        case '=': case '<': case ']':
                            context.getInvocationCompiler().invokeBinaryFixnumRHS(name, receiverCallback, ((FixnumNode)argument).getValue());
                            if (!expr) context.consumeCurrentValue();
                            return;
                        }
                    }
                }
            }
        }

        // if __send__ with a literal symbol, compile it as a direct fcall
        if (RubyInstanceConfig.FASTSEND_COMPILE_ENABLED) {
            String literalSend = getLiteralSend(callNode);
            if (literalSend != null) {
                name = literalSend;
                callType = CallType.FUNCTIONAL;
            }
        }
        
        context.getInvocationCompiler().invokeDynamic(
                name, receiverCallback, argsCallback,
                callType, closureArg, callNode.getIterNode() instanceof IterNode);
        
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    private String getLiteralSend(CallNode callNode) {
        if (callNode.getName().equals("__send__")) {
            if (callNode.getArgsNode() instanceof ArrayNode) {
                ArrayNode arrayNode = (ArrayNode)callNode.getArgsNode();
                if (arrayNode.get(0) instanceof SymbolNode) {
                    return ((SymbolNode)arrayNode.get(0)).getName();
                } else if (arrayNode.get(0) instanceof StrNode) {
                    return ((StrNode)arrayNode.get(0)).getValue().toString();
                }
            }
        }
        return null;
    }

    public void compileCase(Node node, BodyCompiler context, boolean expr) {
        CaseNode caseNode = (CaseNode) node;

        boolean hasCase = caseNode.getCaseNode() != null;

        // aggregate when nodes into a list, unfortunately, this is no
        List<Node> cases = caseNode.getCases().childNodes();

        // last node, either !instanceof WhenNode or null, is the else
        Node elseNode = caseNode.getElseNode();

        compileWhen(caseNode.getCaseNode(), cases, elseNode, context, expr, hasCase);
    }

    private FastSwitchType getHomogeneousSwitchType(List<Node> whenNodes) {
        FastSwitchType foundType = null;
        Outer: for (Node node : whenNodes) {
            WhenNode whenNode = (WhenNode)node;
            if (whenNode.getExpressionNodes() instanceof ArrayNode) {
                ArrayNode arrayNode = (ArrayNode)whenNode.getExpressionNodes();

                for (Node maybeFixnum : arrayNode.childNodes()) {
                    if (maybeFixnum instanceof FixnumNode) {
                        FixnumNode fixnumNode = (FixnumNode)maybeFixnum;
                        long value = fixnumNode.getValue();
                        if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
                            if (foundType != null && foundType != FastSwitchType.FIXNUM) return null;
                            if (foundType == null) foundType = FastSwitchType.FIXNUM;
                            continue;
                        } else {
                            return null;
                        }
                    } else {
                        return null;
                    }
                }
            } else if (whenNode.getExpressionNodes() instanceof FixnumNode) {
                FixnumNode fixnumNode = (FixnumNode)whenNode.getExpressionNodes();
                long value = fixnumNode.getValue();
                if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
                    if (foundType != null && foundType != FastSwitchType.FIXNUM) return null;
                    if (foundType == null) foundType = FastSwitchType.FIXNUM;
                    continue;
                } else {
                    return null;
                }
            } else if (whenNode.getExpressionNodes() instanceof StrNode) {
                StrNode strNode = (StrNode)whenNode.getExpressionNodes();
                if (strNode.getValue().length() == 1) {
                    if (foundType != null && foundType != FastSwitchType.SINGLE_CHAR_STRING) return null;
                    if (foundType == null) foundType = FastSwitchType.SINGLE_CHAR_STRING;

                    continue;
                } else {
                    if (foundType != null && foundType != FastSwitchType.STRING) return null;
                    if (foundType == null) foundType = FastSwitchType.STRING;

                    continue;
                }
            } else if (whenNode.getExpressionNodes() instanceof SymbolNode) {
                SymbolNode symbolNode = (SymbolNode)whenNode.getExpressionNodes();
                if (symbolNode.getName().length() == 1) {
                    if (foundType != null && foundType != FastSwitchType.SINGLE_CHAR_SYMBOL) return null;
                    if (foundType == null) foundType = FastSwitchType.SINGLE_CHAR_SYMBOL;

                    continue;
                } else {
                    if (foundType != null && foundType != FastSwitchType.SYMBOL) return null;
                    if (foundType == null) foundType = FastSwitchType.SYMBOL;

                    continue;
                }
            } else {
                return null;
            }
        }
        return foundType;
    }

    public void compileWhen(final Node value, List<Node> whenNodes, final Node elseNode, BodyCompiler context, final boolean expr, final boolean hasCase) {
        CompilerCallback caseValue = null;
        if (value != null) caseValue = new CompilerCallback() {
            public void call(BodyCompiler context) {
                compile(value, context, true);
                context.pollThreadEvents();
            }
        };

        List<ArgumentsCallback> conditionals = new ArrayList<ArgumentsCallback>();
        List<CompilerCallback> bodies = new ArrayList<CompilerCallback>();
        Map<CompilerCallback, int[]> switchCases = null;
        FastSwitchType switchType = getHomogeneousSwitchType(whenNodes);
        if (switchType != null) {
            // NOTE: Currently this optimization is limited to the following situations:
            // * All expressions must be int-ranged literal fixnums
            // It also still emits the code for the "safe" when logic, which is rather
            // wasteful (since it essentially doubles each code body). As such it is
            // normally disabled, but it serves as an example of how this optimization
            // could be done. Ideally, it should be combined with the when processing
            // to improve code reuse before it's generally available.
            switchCases = new HashMap<CompilerCallback, int[]>();
        }
        for (Node node : whenNodes) {
            final WhenNode whenNode = (WhenNode)node;
            CompilerCallback body = new CompilerCallback() {
                public void call(BodyCompiler context) {
                    compile(whenNode.getBodyNode(), context, expr);
                }
            };
            addConditionalForWhen(whenNode, conditionals, bodies, body);
            if (switchCases != null) switchCases.put(body, getOptimizedCases(whenNode));
        }
        
        CompilerCallback fallback = new CompilerCallback() {
            public void call(BodyCompiler context) {
                compile(elseNode, context, expr);
            }
        };
        
        context.compileSequencedConditional(caseValue, switchType, switchCases, conditionals, bodies, fallback);
    }

    private int[] getOptimizedCases(WhenNode whenNode) {
        if (whenNode.getExpressionNodes() instanceof ArrayNode) {
            ArrayNode expression = (ArrayNode)whenNode.getExpressionNodes();
            if (expression.get(expression.size() - 1) instanceof WhenNode) {
                // splatted when, can't do it yet
                return null;
            }

            int[] cases = new int[expression.size()];
            for (int i = 0; i < cases.length; i++) {
                switch (expression.get(i).getNodeType()) {
                case FIXNUMNODE:
                    cases[i] = (int)((FixnumNode)expression.get(i)).getValue();
                    break;
                default:
                    // can't do it
                    return null;
                }
            }
            return cases;
        } else if (whenNode.getExpressionNodes() instanceof FixnumNode) {
            FixnumNode fixnumNode = (FixnumNode)whenNode.getExpressionNodes();
            return new int[] {(int)fixnumNode.getValue()};
        } else if (whenNode.getExpressionNodes() instanceof StrNode) {
            StrNode strNode = (StrNode)whenNode.getExpressionNodes();
            if (strNode.getValue().length() == 1) {
                return new int[] {strNode.getValue().get(0)};
            } else {
                return new int[] {strNode.getValue().hashCode()};
            }
        } else if (whenNode.getExpressionNodes() instanceof SymbolNode) {
            SymbolNode symbolNode = (SymbolNode)whenNode.getExpressionNodes();
            if (symbolNode.getName().length() == 1) {
                return new int[] {symbolNode.getName().charAt(0)};
            } else {
                return new int[] {symbolNode.getName().hashCode()};
            }
        }
        return null;
    }

    private void addConditionalForWhen(final WhenNode whenNode, List<ArgumentsCallback> conditionals, List<CompilerCallback> bodies, CompilerCallback body) {
        bodies.add(body);

        // If it's a single-arg when but contains an array, we know it's a real literal array
        // FIXME: This is a gross way to figure it out; parser help similar to yield argument passing (expandArguments) would be better
        if (whenNode.getExpressionNodes() instanceof ArrayNode) {
            if (whenNode instanceof WhenOneArgNode) {
                // one arg but it's an array, treat it as a proper array
                conditionals.add(new ArgumentsCallback() {
                    public int getArity() {
                        return 1;
                    }

                    public void call(BodyCompiler context) {
                        compile(whenNode.getExpressionNodes(), context, true);
                    }
                });
                return;
            }
        }
        // otherwise, use normal args compiler
        conditionals.add(getArgsCallback(whenNode.getExpressionNodes()));
    }

    public void compileClass(Node node, BodyCompiler context, boolean expr) {
        final ClassNode classNode = (ClassNode) node;

        final Node superNode = classNode.getSuperNode();

        final Node cpathNode = classNode.getCPath();

        CompilerCallback superCallback = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        compile(superNode, context, true);
                    }
                };
        if (superNode == null) {
            superCallback = null;
        }

        CompilerCallback bodyCallback = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        boolean oldIsAtRoot = isAtRoot;
                        isAtRoot = false;
                        if (classNode.getBodyNode() != null) {
                            compile(classNode.getBodyNode(), context, true);
                        } else {
                            context.loadNil();
                        }
                        isAtRoot = oldIsAtRoot;
                    }
                };

        CompilerCallback pathCallback = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        if (cpathNode instanceof Colon2Node) {
                            Node leftNode = ((Colon2Node) cpathNode).getLeftNode();
                            if (leftNode != null) {
                                if (leftNode instanceof NilNode) {
                                    context.raiseTypeError("No outer class");
                                } else {
                                    compile(leftNode, context, true);
                                }
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

        ASTInspector inspector = new ASTInspector();
        inspector.inspect(classNode.getBodyNode());

        context.defineClass(classNode.getCPath().getName(), classNode.getScope(), superCallback, pathCallback, bodyCallback, null, inspector);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileSClass(Node node, BodyCompiler context, boolean expr) {
        final SClassNode sclassNode = (SClassNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        compile(sclassNode.getReceiverNode(), context, true);
                    }
                };

        CompilerCallback bodyCallback = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        boolean oldIsAtRoot = isAtRoot;
                        isAtRoot = false;
                        if (sclassNode.getBodyNode() != null) {
                            compile(sclassNode.getBodyNode(), context, true);
                        } else {
                            context.loadNil();
                        }
                        isAtRoot = oldIsAtRoot;
                    }
                };

        ASTInspector inspector = new ASTInspector();
        inspector.inspect(sclassNode.getBodyNode());

        context.defineClass("SCLASS", sclassNode.getScope(), null, null, bodyCallback, receiverCallback, inspector);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileClassVar(Node node, BodyCompiler context, boolean expr) {
        ClassVarNode classVarNode = (ClassVarNode) node;

        context.retrieveClassVariable(classVarNode.getName());
        if (!expr) context.consumeCurrentValue();
    }

    public void compileClassVarAsgn(Node node, BodyCompiler context, boolean expr) {
        final ClassVarAsgnNode classVarAsgnNode = (ClassVarAsgnNode) node;

        CompilerCallback value = new CompilerCallback() {
            public void call(BodyCompiler context) {
                compile(classVarAsgnNode.getValueNode(), context, true);
            }
        };

        context.assignClassVariable(classVarAsgnNode.getName(), value);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileClassVarAsgnAssignment(Node node, BodyCompiler context, boolean expr) {
        ClassVarAsgnNode classVarAsgnNode = (ClassVarAsgnNode) node;

        context.assignClassVariable(classVarAsgnNode.getName());
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileClassVarDecl(Node node, BodyCompiler context, boolean expr) {
        final ClassVarDeclNode classVarDeclNode = (ClassVarDeclNode) node;

        CompilerCallback value = new CompilerCallback() {
            public void call(BodyCompiler context) {
                compile(classVarDeclNode.getValueNode(), context, true);
            }
        };
        
        context.declareClassVariable(classVarDeclNode.getName(), value);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileClassVarDeclAssignment(Node node, BodyCompiler context, boolean expr) {
        ClassVarDeclNode classVarDeclNode = (ClassVarDeclNode) node;

        context.declareClassVariable(classVarDeclNode.getName());
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileConstDecl(Node node, BodyCompiler context, boolean expr) {
        // TODO: callback for value would be more efficient, but unlikely to be a big cost (constants are rarely assigned)
        ConstDeclNode constDeclNode = (ConstDeclNode) node;
        Node constNode = constDeclNode.getConstNode();

        if (constNode == null) {
            compile(constDeclNode.getValueNode(), context,true);

            context.assignConstantInCurrent(constDeclNode.getName());
        } else if (constNode.getNodeType() == NodeType.COLON2NODE) {
            compile(((Colon2Node) constNode).getLeftNode(), context,true);
            compile(constDeclNode.getValueNode(), context,true);

            context.assignConstantInModule(constDeclNode.getName());
        } else {// colon3, assign in Object
            compile(constDeclNode.getValueNode(), context,true);

            context.assignConstantInObject(constDeclNode.getName());
        }
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileConstDeclAssignment(Node node, BodyCompiler context, boolean expr) {
        // TODO: callback for value would be more efficient, but unlikely to be a big cost (constants are rarely assigned)
        ConstDeclNode constDeclNode = (ConstDeclNode) node;
        Node constNode = constDeclNode.getConstNode();

        if (constNode == null) {
            context.assignConstantInCurrent(constDeclNode.getName());
        } else if (constNode.getNodeType() == NodeType.COLON2NODE) {
            compile(((Colon2Node) constNode).getLeftNode(), context,true);
            context.swapValues();
            context.assignConstantInModule(constDeclNode.getName());
        } else {// colon3, assign in Object
            context.assignConstantInObject(constDeclNode.getName());
        }
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileConst(Node node, BodyCompiler context, boolean expr) {
        ConstNode constNode = (ConstNode) node;

        context.retrieveConstant(constNode.getName());
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
        // XXX: const lookup can trigger const_missing; is that enough to warrant it always being executed?
    }

    public void compileColon2(Node node, BodyCompiler context, boolean expr) {
        final Colon2Node iVisited = (Colon2Node) node;
        Node leftNode = iVisited.getLeftNode();
        final String name = iVisited.getName();

        if (leftNode == null) {
            context.loadObject();
            context.retrieveConstantFromModule(name);
        } else {
            if (node instanceof Colon2ConstNode) {
                compile(iVisited.getLeftNode(), context, true);
                context.retrieveConstantFromModule(name);
            } else if (node instanceof Colon2MethodNode) {
                final CompilerCallback receiverCallback = new CompilerCallback() {
                    public void call(BodyCompiler context) {
                        compile(iVisited.getLeftNode(), context,true);
                    }
                };
                
                context.getInvocationCompiler().invokeDynamic(name, receiverCallback, null, CallType.FUNCTIONAL, null, false);
            }
        }
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileColon3(Node node, BodyCompiler context, boolean expr) {
        Colon3Node iVisited = (Colon3Node) node;
        String name = iVisited.getName();

        context.retrieveConstantFromObject(name);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileGetDefinitionBase(final Node node, BodyCompiler context) {
        switch (node.getNodeType()) {
        case CLASSVARASGNNODE:
        case CLASSVARDECLNODE:
        case CONSTDECLNODE:
        case DASGNNODE:
        case GLOBALASGNNODE:
        case LOCALASGNNODE:
        case MULTIPLEASGNNODE:
        case OPASGNNODE:
        case OPELEMENTASGNNODE:
        case DVARNODE:
        case FALSENODE:
        case TRUENODE:
        case LOCALVARNODE:
        case INSTVARNODE:
        case BACKREFNODE:
        case SELFNODE:
        case VCALLNODE:
        case YIELDNODE:
        case GLOBALVARNODE:
        case CONSTNODE:
        case FCALLNODE:
        case CLASSVARNODE:
            // these are all simple cases that don't require the heavier defined logic
            compileGetDefinition(node, context);
            break;
        default:
            BranchCallback reg = new BranchCallback() {

                        public void branch(BodyCompiler context) {
                            context.inDefined();
                            compileGetDefinition(node, context);
                        }
                    };
            BranchCallback out = new BranchCallback() {

                        public void branch(BodyCompiler context) {
                            context.outDefined();
                        }
                    };
            context.protect(reg, out, String.class);
        }
    }

    public void compileDefined(final Node node, BodyCompiler context, boolean expr) {
        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) {
                compileGetDefinitionBase(((DefinedNode) node).getExpressionNode(), context);
                context.stringOrNil();
            }
        } else {
            compileGetDefinitionBase(((DefinedNode) node).getExpressionNode(), context);
            context.stringOrNil();
            if (!expr) context.consumeCurrentValue();
        }
    }

    public void compileGetArgumentDefinition(final Node node, BodyCompiler context, String type) {
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

    public void compileGetDefinition(final Node node, BodyCompiler context) {
        switch (node.getNodeType()) {
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

                            public void branch(BodyCompiler context) {
                                context.pushString("$" + ((BackRefNode) node).getType());
                            }
                        },
                        new BranchCallback() {

                            public void branch(BodyCompiler context) {
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

                            public void branch(BodyCompiler context) {
                                context.pushString("$" + ((NthRefNode) node).getMatchNumber());
                            }
                        },
                        new BranchCallback() {

                            public void branch(BodyCompiler context) {
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

                            public void branch(BodyCompiler context) {
                                context.pushString("method");
                            }
                        },
                        new BranchCallback() {

                            public void branch(BodyCompiler context) {
                                context.pushNull();
                            }
                        });
                break;
            case YIELDNODE:
                context.hasBlock(new BranchCallback() {

                            public void branch(BodyCompiler context) {
                                context.pushString("yield");
                            }
                        },
                        new BranchCallback() {

                            public void branch(BodyCompiler context) {
                                context.pushNull();
                            }
                        });
                break;
            case GLOBALVARNODE:
                context.isGlobalDefined(((GlobalVarNode) node).getName(),
                        new BranchCallback() {

                            public void branch(BodyCompiler context) {
                                context.pushString("global-variable");
                            }
                        },
                        new BranchCallback() {

                            public void branch(BodyCompiler context) {
                                context.pushNull();
                            }
                        });
                break;
            case INSTVARNODE:
                context.isInstanceVariableDefined(((InstVarNode) node).getName(),
                        new BranchCallback() {

                            public void branch(BodyCompiler context) {
                                context.pushString("instance-variable");
                            }
                        },
                        new BranchCallback() {

                            public void branch(BodyCompiler context) {
                                context.pushNull();
                            }
                        });
                break;
            case CONSTNODE:
                context.isConstantDefined(((ConstNode) node).getName(),
                        new BranchCallback() {

                            public void branch(BodyCompiler context) {
                                context.pushString("constant");
                            }
                        },
                        new BranchCallback() {

                            public void branch(BodyCompiler context) {
                                context.pushNull();
                            }
                        });
                break;
            case FCALLNODE:
                context.loadSelf();
                context.isMethodBound(((FCallNode) node).getName(),
                        new BranchCallback() {

                            public void branch(BodyCompiler context) {
                                compileGetArgumentDefinition(((FCallNode) node).getArgsNode(), context, "method");
                            }
                        },
                        new BranchCallback() {

                            public void branch(BodyCompiler context) {
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

                                public void branch(BodyCompiler context) {
                                    if (iVisited instanceof Colon2Node) {
                                        final Node leftNode = ((Colon2Node) iVisited).getLeftNode();
                                        compile(leftNode, context,true);
                                    } else {
                                        context.loadObject();
                                    }
                                }
                            };
                    BranchCallback isConstant = new BranchCallback() {

                                public void branch(BodyCompiler context) {
                                    context.pushString("constant");
                                }
                            };
                    BranchCallback isMethod = new BranchCallback() {

                                public void branch(BodyCompiler context) {
                                    context.pushString("method");
                                }
                            };
                    BranchCallback none = new BranchCallback() {

                                public void branch(BodyCompiler context) {
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

                                public void branch(BodyCompiler context) {
                                    compile(iVisited.getReceiverNode(), context,true); //[IRubyObject]
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

                                                public void branch(BodyCompiler context) {
                                                    compileGetArgumentDefinition(iVisited.getArgsNode(), context, "method");
                                                }
                                            },
                                            new BranchCallback() {

                                                public void branch(BodyCompiler context) {
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

                                public void branch(BodyCompiler context) {
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

                                public void branch(BodyCompiler context) {
                                    context.consumeCurrentValue();
                                    context.pushString("class variable");
                                    context.go(ending);
                                }
                            },
                            new BranchCallback() {

                                public void branch(BodyCompiler context) {
                                }
                            });
                    context.setEnding(second);  //[RubyClass]
                    context.duplicateCurrentValue();
                    context.isClassVarDefined(iVisited.getName(),
                            new BranchCallback() {

                                public void branch(BodyCompiler context) {
                                    context.consumeCurrentValue();
                                    context.pushString("class variable");
                                    context.go(ending);
                                }
                            },
                            new BranchCallback() {

                                public void branch(BodyCompiler context) {
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

                                public void branch(BodyCompiler context) {
                                    compile(iVisited.getReceiverNode(), context,true); //[IRubyObject]
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

                                                public void branch(BodyCompiler context) {
                                                    compileGetArgumentDefinition(iVisited.getArgsNode(), context, "assignment");
                                                }
                                            },
                                            new BranchCallback() {

                                                public void branch(BodyCompiler context) {
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

                                public void branch(BodyCompiler context) {
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

                            public void branch(BodyCompiler context) {
                                compile(node, context,true);
                                context.consumeCurrentValue();
                                context.pushNull();
                            }
                        }, JumpException.class,
                        new BranchCallback() {

                            public void branch(BodyCompiler context) {
                                context.pushNull();
                            }
                        }, String.class);
                context.consumeCurrentValue();
                context.pushString("expression");
        }
    }

    public void compileDAsgn(Node node, BodyCompiler context, boolean expr) {
        final DAsgnNode dasgnNode = (DAsgnNode) node;

        CompilerCallback value = new CompilerCallback() {
            public void call(BodyCompiler context) {
                compile(dasgnNode.getValueNode(), context, true);
            }
        };
        
        context.getVariableCompiler().assignLocalVariable(dasgnNode.getIndex(), dasgnNode.getDepth(), value, expr);
    }

    public void compileDAsgnAssignment(Node node, BodyCompiler context, boolean expr) {
        DAsgnNode dasgnNode = (DAsgnNode) node;

        context.getVariableCompiler().assignLocalVariable(dasgnNode.getIndex(), dasgnNode.getDepth(), expr);
    }

    public void compileDefn(Node node, BodyCompiler context, boolean expr) {
        final DefnNode defnNode = (DefnNode) node;
        final ArgsNode argsNode = defnNode.getArgsNode();

        CompilerCallback body = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        if (defnNode.getBodyNode() != null) {
                            if (defnNode.getBodyNode() instanceof RescueNode) {
                                // if root of method is rescue, compile as a light rescue
                                compileRescueInternal(defnNode.getBodyNode(), context, true);
                            } else {
                                compile(defnNode.getBodyNode(), context, true);
                            }
                        } else {
                            context.loadNil();
                        }
                    }
                };

        CompilerCallback args = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        compileArgs(argsNode, context, true);
                    }
                };

        // inspect body and args
        ASTInspector inspector = new ASTInspector();
        // check args first, since body inspection can depend on args
        inspector.inspect(defnNode.getArgsNode());

        // if body is a rescue node, inspect its pieces separately to avoid it disabling all optz
        // TODO: this is gross.
        if (defnNode.getBodyNode() instanceof RescueNode) {
            RescueNode rescueNode = (RescueNode)defnNode.getBodyNode();
            inspector.inspect(rescueNode.getBodyNode());
            inspector.inspect(rescueNode.getElseNode());
            inspector.inspect(rescueNode.getRescueNode());
        } else {
            inspector.inspect(defnNode.getBodyNode());
        }

        context.defineNewMethod(defnNode.getName(), defnNode.getArgsNode().getArity().getValue(), defnNode.getScope(), body, args, null, inspector, isAtRoot);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileDefs(Node node, BodyCompiler context, boolean expr) {
        final DefsNode defsNode = (DefsNode) node;
        final ArgsNode argsNode = defsNode.getArgsNode();

        CompilerCallback receiver = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        compile(defsNode.getReceiverNode(), context, true);
                    }
                };

        CompilerCallback body = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        if (defsNode.getBodyNode() != null) {
                            if (defsNode.getBodyNode() instanceof RescueNode) {
                                // if root of method is rescue, compile as light rescue
                                compileRescueInternal(defsNode.getBodyNode(), context, true);
                            } else {
                                compile(defsNode.getBodyNode(), context, true);
                            }
                        } else {
                            context.loadNil();
                        }
                    }
                };

        CompilerCallback args = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        compileArgs(argsNode, context, true);
                    }
                };

        // inspect body and args
        ASTInspector inspector = new ASTInspector();
        inspector.inspect(defsNode.getArgsNode());

        // if body is a rescue node, inspect its pieces separately to avoid it disabling all optz
        // TODO: this is gross.
        if (defsNode.getBodyNode() instanceof RescueNode) {
            RescueNode rescueNode = (RescueNode)defsNode.getBodyNode();
            inspector.inspect(rescueNode.getBodyNode());
            inspector.inspect(rescueNode.getElseNode());
            inspector.inspect(rescueNode.getRescueNode());
        } else {
            inspector.inspect(defsNode.getBodyNode());
        }

        context.defineNewMethod(defsNode.getName(), defsNode.getArgsNode().getArity().getValue(), defsNode.getScope(), body, args, receiver, inspector, false);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileArgs(Node node, BodyCompiler context, boolean expr) {
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

                        public void nextValue(BodyCompiler context, Object object, int index) {
                            // FIXME: Somehow I'd feel better if this could get the appropriate var index from the ArgumentNode
                            context.getVariableCompiler().assignLocalVariable(index, false);
                        }
                    };
        }

        if (opt > 0) {
            optionalGiven = new ArrayCallback() {

                        public void nextValue(BodyCompiler context, Object object, int index) {
                            Node optArg = ((ListNode) object).get(index);

                            compileAssignment(optArg, context,true);
                            context.consumeCurrentValue();
                        }
                    };
            optionalNotGiven = new ArrayCallback() {

                        public void nextValue(BodyCompiler context, Object object, int index) {
                            Node optArg = ((ListNode) object).get(index);

                            compile(optArg, context,true);
                            context.consumeCurrentValue();
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

        context.getVariableCompiler().checkMethodArity(required, opt, rest);
        context.getVariableCompiler().assignMethodArguments(argsNode.getPre(),
                argsNode.getRequiredArgsCount(),
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

    public void compileDot(Node node, BodyCompiler context, boolean expr) {
        final DotNode dotNode = (DotNode) node;

        boolean doit = expr || !RubyInstanceConfig.PEEPHOLE_OPTZ;
        boolean popit = !RubyInstanceConfig.PEEPHOLE_OPTZ && !expr;

        if (doit) {
            CompilerCallback beginEndCallback = new CompilerCallback() {
                public void call(BodyCompiler context) {
                    compile(dotNode.getBeginNode(), context, true);
                    compile(dotNode.getEndNode(), context, true);
                }
            };

            context.createNewRange(beginEndCallback, dotNode.isExclusive());
        }
        if (popit) context.consumeCurrentValue();
    }

    public void compileDRegexp(Node node, BodyCompiler context, boolean expr) {
        final DRegexpNode dregexpNode = (DRegexpNode) node;

        CompilerCallback createStringCallback = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        ArrayCallback dstrCallback = new ArrayCallback() {

                                    public void nextValue(BodyCompiler context, Object sourceArray,
                                            int index) {
                                        compile(dregexpNode.get(index), context, true);
                                    }
                                };
                        context.createNewString(dstrCallback, dregexpNode.size());
                    }
                };

        boolean doit = expr || !RubyInstanceConfig.PEEPHOLE_OPTZ;
        boolean popit = !RubyInstanceConfig.PEEPHOLE_OPTZ && !expr;

        if (doit) {
            context.createNewRegexp(createStringCallback, dregexpNode.getOptions());
            if (popit) context.consumeCurrentValue();
        } else {
            // not an expression, only compile the elements
            for (Node nextNode : dregexpNode.childNodes()) {
                compile(nextNode, context, false);
            }
        }
    }

    public void compileDStr(Node node, BodyCompiler context, boolean expr) {
        final DStrNode dstrNode = (DStrNode) node;

        ArrayCallback dstrCallback = new ArrayCallback() {

                    public void nextValue(BodyCompiler context, Object sourceArray,
                            int index) {
                        compile(dstrNode.get(index), context, true);
                    }
                };

        boolean doit = expr || !RubyInstanceConfig.PEEPHOLE_OPTZ;
        boolean popit = !RubyInstanceConfig.PEEPHOLE_OPTZ && !expr;

        if (doit) {
            context.createNewString(dstrCallback, dstrNode.size());
            if (popit) context.consumeCurrentValue();
        } else {
            // not an expression, only compile the elements
            for (Node nextNode : dstrNode.childNodes()) {
                compile(nextNode, context, false);
            }
        }
    }

    public void compileDSymbol(Node node, BodyCompiler context, boolean expr) {
        final DSymbolNode dsymbolNode = (DSymbolNode) node;

        ArrayCallback dstrCallback = new ArrayCallback() {

                    public void nextValue(BodyCompiler context, Object sourceArray,
                            int index) {
                        compile(dsymbolNode.get(index), context, true);
                    }
                };

        boolean doit = expr || !RubyInstanceConfig.PEEPHOLE_OPTZ;
        boolean popit = !RubyInstanceConfig.PEEPHOLE_OPTZ && !expr;

        if (doit) {
            context.createNewSymbol(dstrCallback, dsymbolNode.size());
            if (popit) context.consumeCurrentValue();
        } else {
            // not an expression, only compile the elements
            for (Node nextNode : dsymbolNode.childNodes()) {
                compile(nextNode, context, false);
            }
        }
    }

    public void compileDVar(Node node, BodyCompiler context, boolean expr) {
        DVarNode dvarNode = (DVarNode) node;

        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) context.getVariableCompiler().retrieveLocalVariable(dvarNode.getIndex(), dvarNode.getDepth());
        } else {
            context.getVariableCompiler().retrieveLocalVariable(dvarNode.getIndex(), dvarNode.getDepth());
            if (!expr) context.consumeCurrentValue();
        }
    }

    public void compileDXStr(Node node, BodyCompiler context, boolean expr) {
        final DXStrNode dxstrNode = (DXStrNode) node;

        final ArrayCallback dstrCallback = new ArrayCallback() {

                    public void nextValue(BodyCompiler context, Object sourceArray,
                            int index) {
                        compile(dxstrNode.get(index), context,true);
                    }
                };

        ArgumentsCallback argsCallback = new ArgumentsCallback() {
                    public int getArity() {
                        return 1;
                    }
                    
                    public void call(BodyCompiler context) {
                        context.createNewString(dstrCallback, dxstrNode.size());
                    }
                };

        context.getInvocationCompiler().invokeDynamic("`", null, argsCallback, CallType.FUNCTIONAL, null, false);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileEnsureNode(Node node, BodyCompiler context, boolean expr) {
        final EnsureNode ensureNode = (EnsureNode) node;

        if (ensureNode.getEnsureNode() != null) {
            context.performEnsure(new BranchCallback() {

                        public void branch(BodyCompiler context) {
                            if (ensureNode.getBodyNode() != null) {
                                compile(ensureNode.getBodyNode(), context, true);
                            } else {
                                context.loadNil();
                            }
                        }
                    },
                    new BranchCallback() {

                        public void branch(BodyCompiler context) {
                            compile(ensureNode.getEnsureNode(), context, false);
                        }
                    });
        } else {
            if (ensureNode.getBodyNode() != null) {
                compile(ensureNode.getBodyNode(), context,true);
            } else {
                context.loadNil();
            }
        }
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileEvStr(Node node, BodyCompiler context, boolean expr) {
        final EvStrNode evStrNode = (EvStrNode) node;

        compile(evStrNode.getBody(), context,true);
        context.asString();
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileFalse(Node node, BodyCompiler context, boolean expr) {
        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) {
                context.loadFalse();
                context.pollThreadEvents();
            }
        } else {
            context.loadFalse();
            context.pollThreadEvents();
            if (!expr) context.consumeCurrentValue();
        }
    }

    public void compileFCall(Node node, BodyCompiler context, boolean expr) {
        final FCallNode fcallNode = (FCallNode) node;

        ArgumentsCallback argsCallback = getArgsCallback(fcallNode.getArgsNode());
        
        CompilerCallback closureArg = getBlock(fcallNode.getIterNode());

        context.getInvocationCompiler().invokeDynamic(fcallNode.getName(), null, argsCallback, CallType.FUNCTIONAL, closureArg, fcallNode.getIterNode() instanceof IterNode);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    private CompilerCallback getBlock(Node node) {
        if (node == null) {
            return null;
        }

        switch (node.getNodeType()) {
            case ITERNODE:
                final IterNode iterNode = (IterNode) node;

                return new CompilerCallback() {

                            public void call(BodyCompiler context) {
                                compile(iterNode, context,true);
                            }
                        };
            case BLOCKPASSNODE:
                final BlockPassNode blockPassNode = (BlockPassNode) node;

                return new CompilerCallback() {

                            public void call(BodyCompiler context) {
                                compile(blockPassNode.getBodyNode(), context,true);
                                context.unwrapPassedBlock();
                            }
                        };
            default:
                throw new NotCompilableException("ERROR: Encountered a method with a non-block, non-blockpass iter node at: " + node);
        }
    }

    public void compileFixnum(Node node, BodyCompiler context, boolean expr) {
        FixnumNode fixnumNode = (FixnumNode) node;

        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) context.createNewFixnum(fixnumNode.getValue());
        } else {
            context.createNewFixnum(fixnumNode.getValue());
            if (!expr) context.consumeCurrentValue();
        }
    }

    public void compileFlip(Node node, BodyCompiler context, boolean expr) {
        final FlipNode flipNode = (FlipNode) node;

        context.getVariableCompiler().retrieveLocalVariable(flipNode.getIndex(), flipNode.getDepth());

        if (flipNode.isExclusive()) {
            context.performBooleanBranch(new BranchCallback() {

                public void branch(BodyCompiler context) {
                    compile(flipNode.getEndNode(), context,true);
                    context.performBooleanBranch(new BranchCallback() {

                        public void branch(BodyCompiler context) {
                            context.loadFalse();
                            context.getVariableCompiler().assignLocalVariable(flipNode.getIndex(), flipNode.getDepth(), false);
                        }
                    }, new BranchCallback() {

                        public void branch(BodyCompiler context) {
                        }
                    });
                    context.loadTrue();
                }
            }, new BranchCallback() {

                public void branch(BodyCompiler context) {
                    compile(flipNode.getBeginNode(), context,true);
                    becomeTrueOrFalse(context);
                    context.getVariableCompiler().assignLocalVariable(flipNode.getIndex(), flipNode.getDepth(), true);
                }
            });
        } else {
            context.performBooleanBranch(new BranchCallback() {

                public void branch(BodyCompiler context) {
                    compile(flipNode.getEndNode(), context,true);
                    context.performBooleanBranch(new BranchCallback() {

                        public void branch(BodyCompiler context) {
                            context.loadFalse();
                            context.getVariableCompiler().assignLocalVariable(flipNode.getIndex(), flipNode.getDepth(), false);
                        }
                    }, new BranchCallback() {

                        public void branch(BodyCompiler context) {
                        }
                    });
                    context.loadTrue();
                }
            }, new BranchCallback() {

                public void branch(BodyCompiler context) {
                    compile(flipNode.getBeginNode(), context,true);
                    context.performBooleanBranch(new BranchCallback() {

                        public void branch(BodyCompiler context) {
                            compile(flipNode.getEndNode(), context,true);
                            flipTrueOrFalse(context);
                            context.getVariableCompiler().assignLocalVariable(flipNode.getIndex(), flipNode.getDepth(), false);
                            context.loadTrue();
                        }
                    }, new BranchCallback() {

                        public void branch(BodyCompiler context) {
                            context.loadFalse();
                        }
                    });
                }
            });
        }
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    private void becomeTrueOrFalse(BodyCompiler context) {
        context.performBooleanBranch(new BranchCallback() {

                    public void branch(BodyCompiler context) {
                        context.loadTrue();
                    }
                }, new BranchCallback() {

                    public void branch(BodyCompiler context) {
                        context.loadFalse();
                    }
                });
    }

    private void flipTrueOrFalse(BodyCompiler context) {
        context.performBooleanBranch(new BranchCallback() {

                    public void branch(BodyCompiler context) {
                        context.loadFalse();
                    }
                }, new BranchCallback() {

                    public void branch(BodyCompiler context) {
                        context.loadTrue();
                    }
                });
    }

    public void compileFloat(Node node, BodyCompiler context, boolean expr) {
        FloatNode floatNode = (FloatNode) node;

        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) context.createNewFloat(floatNode.getValue());
        } else {
            context.createNewFloat(floatNode.getValue());
            if (!expr) context.consumeCurrentValue();
        }
    }

    public void compileFor(Node node, BodyCompiler context, boolean expr) {
        final ForNode forNode = (ForNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        compile(forNode.getIterNode(), context, true);
                    }
                };

        final CompilerCallback closureArg = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        compileForIter(forNode, context);
                    }
                };

        context.getInvocationCompiler().invokeDynamic("each", receiverCallback, null, CallType.NORMAL, closureArg, true);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileForIter(Node node, BodyCompiler context) {
        final ForNode forNode = (ForNode) node;

        // create the closure class and instantiate it
        final CompilerCallback closureBody = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        if (forNode.getBodyNode() != null) {
                            compile(forNode.getBodyNode(), context,true);
                        } else {
                            context.loadNil();
                        }
                    }
                };

        // create the closure class and instantiate it
        final CompilerCallback closureArgs = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        if (forNode.getVarNode() != null) {
                            compileAssignment(forNode.getVarNode(), context, false);
                        }
                    }
                };

        boolean hasMultipleArgsHead = false;
        if (forNode.getVarNode() instanceof MultipleAsgnNode) {
            hasMultipleArgsHead = ((MultipleAsgnNode) forNode.getVarNode()).getHeadNode() != null;
        }

        NodeType argsNodeId = null;
        if (forNode.getVarNode() != null) {
            argsNodeId = forNode.getVarNode().getNodeType();
        }
        
        ASTInspector inspector = new ASTInspector();
        inspector.inspect(forNode.getBodyNode());
        inspector.inspect(forNode.getVarNode());

        // force heap-scope behavior, since it uses parent's scope
        inspector.setFlag(ASTInspector.CLOSURE);

        if (argsNodeId == null) {
            // no args, do not pass args processor
            context.createNewForLoop(Arity.procArityOf(forNode.getVarNode()).getValue(),
                    closureBody, null, hasMultipleArgsHead, argsNodeId, inspector);
        } else {
            context.createNewForLoop(Arity.procArityOf(forNode.getVarNode()).getValue(),
                    closureBody, closureArgs, hasMultipleArgsHead, argsNodeId, inspector);
        }
    }

    public void compileGlobalAsgn(Node node, BodyCompiler context, boolean expr) {
        final GlobalAsgnNode globalAsgnNode = (GlobalAsgnNode) node;

        CompilerCallback value = new CompilerCallback() {
            public void call(BodyCompiler context) {
                compile(globalAsgnNode.getValueNode(), context, true);
            }
        };

        if (globalAsgnNode.getName().length() == 2) {
            switch (globalAsgnNode.getName().charAt(1)) {
            case '_':
                context.getVariableCompiler().assignLastLine(value);
                break;
            case '~':
                context.getVariableCompiler().assignBackRef(value);
                break;
            default:
                context.assignGlobalVariable(globalAsgnNode.getName(), value);
            }
        } else {
            context.assignGlobalVariable(globalAsgnNode.getName(), value);
        }

        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileGlobalAsgnAssignment(Node node, BodyCompiler context, boolean expr) {
        GlobalAsgnNode globalAsgnNode = (GlobalAsgnNode) node;

        if (globalAsgnNode.getName().length() == 2) {
            switch (globalAsgnNode.getName().charAt(1)) {
            case '_':
                context.getVariableCompiler().assignLastLine();
                break;
            case '~':
                context.getVariableCompiler().assignBackRef();
                break;
            default:
                context.assignGlobalVariable(globalAsgnNode.getName());
            }
        } else {
            context.assignGlobalVariable(globalAsgnNode.getName());
        }
        
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileGlobalVar(Node node, BodyCompiler context, boolean expr) {
        GlobalVarNode globalVarNode = (GlobalVarNode) node;

        boolean doit = expr || !RubyInstanceConfig.PEEPHOLE_OPTZ;
        boolean popit = !RubyInstanceConfig.PEEPHOLE_OPTZ && !expr;
        
        if (doit) {
            if (globalVarNode.getName().length() == 2) {
                switch (globalVarNode.getName().charAt(1)) {
                case '_':
                    context.getVariableCompiler().retrieveLastLine();
                    break;
                case '~':
                    context.getVariableCompiler().retrieveBackRef();
                    break;
                default:
                    context.retrieveGlobalVariable(globalVarNode.getName());
                }
            } else {
                context.retrieveGlobalVariable(globalVarNode.getName());
            }
        }
        
        if (popit) context.consumeCurrentValue();
    }

    public void compileHash(Node node, BodyCompiler context, boolean expr) {
        compileHashCommon((HashNode) node, context, expr);
    }
    
    protected void compileHashCommon(HashNode hashNode, BodyCompiler context, boolean expr) {
        boolean doit = expr || !RubyInstanceConfig.PEEPHOLE_OPTZ;
        boolean popit = !RubyInstanceConfig.PEEPHOLE_OPTZ && !expr;

        if (doit) {
            if (hashNode.getListNode() == null || hashNode.getListNode().size() == 0) {
                context.createEmptyHash();
                return;
            }

            ArrayCallback hashCallback = new ArrayCallback() {

                        public void nextValue(BodyCompiler context, Object sourceArray,
                                int index) {
                            ListNode listNode = (ListNode) sourceArray;
                            int keyIndex = index * 2;
                            compile(listNode.get(keyIndex), context, true);
                            compile(listNode.get(keyIndex + 1), context, true);
                        }
                    };

            createNewHash(context, hashNode, hashCallback);
            if (popit) context.consumeCurrentValue();
        } else {
            for (Node nextNode : hashNode.getListNode().childNodes()) {
                compile(nextNode, context, false);
            }
        }
    }
    
    protected void createNewHash(BodyCompiler context, HashNode hashNode, ArrayCallback hashCallback) {
        context.createNewHash(hashNode.getListNode(), hashCallback, hashNode.getListNode().size() / 2);
    }

    public void compileIf(Node node, BodyCompiler context, final boolean expr) {
        final IfNode ifNode = (IfNode) node;

        // optimizations if we know ahead of time it will always be true or false
        Node actualCondition = ifNode.getCondition();
        while (actualCondition instanceof NewlineNode) {
            actualCondition = ((NewlineNode)actualCondition).getNextNode();
        }

        if (actualCondition.getNodeType().alwaysTrue()) {
            // compile condition as non-expr and just compile "then" body
            compile(actualCondition, context, false);
            compile(ifNode.getThenBody(), context, expr);
        } else if (actualCondition.getNodeType().alwaysFalse()) {
            // always false or nil
            compile(ifNode.getElseBody(), context, expr);
        } else {
            BranchCallback trueCallback = new BranchCallback() {
                public void branch(BodyCompiler context) {
                    if (ifNode.getThenBody() != null) {
                        compile(ifNode.getThenBody(), context, expr);
                    } else {
                        if (expr) context.loadNil();
                    }
                }
            };

            BranchCallback falseCallback = new BranchCallback() {
                public void branch(BodyCompiler context) {
                    if (ifNode.getElseBody() != null) {
                        compile(ifNode.getElseBody(), context, expr);
                    } else {
                        if (expr) context.loadNil();
                    }
                }
            };
            
            // normal
            compile(actualCondition, context, true);
            context.performBooleanBranch(trueCallback, falseCallback);
        }
    }

    public void compileInstAsgn(Node node, BodyCompiler context, boolean expr) {
        final InstAsgnNode instAsgnNode = (InstAsgnNode) node;

        CompilerCallback value = new CompilerCallback() {
            public void call(BodyCompiler context) {
                compile(instAsgnNode.getValueNode(), context, true);
            }
        };

        context.assignInstanceVariable(instAsgnNode.getName(), value);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileInstAsgnAssignment(Node node, BodyCompiler context, boolean expr) {
        InstAsgnNode instAsgnNode = (InstAsgnNode) node;
        context.assignInstanceVariable(instAsgnNode.getName());
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileInstVar(Node node, BodyCompiler context, boolean expr) {
        InstVarNode instVarNode = (InstVarNode) node;

        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) context.retrieveInstanceVariable(instVarNode.getName());
        } else {
            context.retrieveInstanceVariable(instVarNode.getName());
            if (!expr) context.consumeCurrentValue();
        }
    }

    public void compileIter(Node node, BodyCompiler context) {
        final IterNode iterNode = (IterNode) node;

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
                        if (iterNode.getVarNode() != null) {
                            compileAssignment(iterNode.getVarNode(), context, false);
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

    public void compileLocalAsgn(Node node, BodyCompiler context, boolean expr) {
        final LocalAsgnNode localAsgnNode = (LocalAsgnNode) node;

        // just push nil for pragmas
        if (ASTInspector.PRAGMAS.contains(localAsgnNode.getName())) {
            if (expr) context.loadNil();
        } else {
            CompilerCallback value = new CompilerCallback() {
                public void call(BodyCompiler context) {
                    compile(localAsgnNode.getValueNode(), context,true);
                }
            };

            context.getVariableCompiler().assignLocalVariable(localAsgnNode.getIndex(), localAsgnNode.getDepth(), value, expr);
        }
    }

    public void compileLocalAsgnAssignment(Node node, BodyCompiler context, boolean expr) {
        // "assignment" means the value is already on the stack
        LocalAsgnNode localAsgnNode = (LocalAsgnNode) node;

        context.getVariableCompiler().assignLocalVariable(localAsgnNode.getIndex(), localAsgnNode.getDepth(), expr);
    }

    public void compileLocalVar(Node node, BodyCompiler context, boolean expr) {
        LocalVarNode localVarNode = (LocalVarNode) node;

        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) context.getVariableCompiler().retrieveLocalVariable(localVarNode.getIndex(), localVarNode.getDepth());
        } else {
            context.getVariableCompiler().retrieveLocalVariable(localVarNode.getIndex(), localVarNode.getDepth());
            if (!expr) context.consumeCurrentValue();
        }
    }

    public void compileMatch(Node node, BodyCompiler context, boolean expr) {
        MatchNode matchNode = (MatchNode) node;

        compile(matchNode.getRegexpNode(), context,true);

        context.match();
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileMatch2(Node node, BodyCompiler context, boolean expr) {
        final Match2Node matchNode = (Match2Node) node;

        compile(matchNode.getReceiverNode(), context,true);
        CompilerCallback value = new CompilerCallback() {
            public void call(BodyCompiler context) {
                compile(matchNode.getValueNode(), context,true);
            }
        };

        context.match2(value);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileMatch3(Node node, BodyCompiler context, boolean expr) {
        Match3Node matchNode = (Match3Node) node;

        compile(matchNode.getReceiverNode(), context,true);
        compile(matchNode.getValueNode(), context,true);

        context.match3();
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileModule(Node node, BodyCompiler context, boolean expr) {
        final ModuleNode moduleNode = (ModuleNode) node;

        final Node cpathNode = moduleNode.getCPath();

        CompilerCallback bodyCallback = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        if (moduleNode.getBodyNode() != null) {
                            compile(moduleNode.getBodyNode(), context,true);
                        }
                        context.loadNil();
                    }
                };

        CompilerCallback pathCallback = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        if (cpathNode instanceof Colon2Node) {
                            Node leftNode = ((Colon2Node) cpathNode).getLeftNode();
                            if (leftNode != null) {
                                compile(leftNode, context,true);
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

        ASTInspector inspector = new ASTInspector();
        inspector.inspect(moduleNode.getBodyNode());

        context.defineModule(moduleNode.getCPath().getName(), moduleNode.getScope(), pathCallback, bodyCallback, inspector);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileMultipleAsgn(Node node, BodyCompiler context, boolean expr) {
        MultipleAsgnNode multipleAsgnNode = (MultipleAsgnNode) node;

        if (expr) {
            // need the array, use unoptz version
            compileUnoptimizedMultipleAsgn(multipleAsgnNode, context, expr);
        } else {
            // try optz version
            compileOptimizedMultipleAsgn(multipleAsgnNode, context, expr);
        }
    }

    private void compileOptimizedMultipleAsgn(MultipleAsgnNode multipleAsgnNode, BodyCompiler context, boolean expr) {
        // expect value to be an array of nodes
        if (multipleAsgnNode.getValueNode() instanceof ArrayNode) {
            // head must not be null and there must be no "args" (like *arg)
            if (multipleAsgnNode.getHeadNode() != null && multipleAsgnNode.getArgsNode() == null) {
                // sizes must match
                if (multipleAsgnNode.getHeadNode().size() == ((ArrayNode)multipleAsgnNode.getValueNode()).size()) {
                    // "head" must have no non-trivial assigns (array groupings, basically)
                    boolean normalAssigns = true;
                    for (Node asgn : multipleAsgnNode.getHeadNode().childNodes()) {
                        if (asgn instanceof ListNode) {
                            normalAssigns = false;
                            break;
                        }
                    }
                    
                    if (normalAssigns) {
                        // only supports simple parallel assignment of up to 10 values to the same number of assignees
                        int size = multipleAsgnNode.getHeadNode().size();
                        if (size >= 2 && size <= 10) {
                            ArrayNode values = (ArrayNode)multipleAsgnNode.getValueNode();
                            for (Node value : values.childNodes()) {
                                compile(value, context, true);
                            }
                            context.reverseValues(size);
                            for (Node asgn : multipleAsgnNode.getHeadNode().childNodes()) {
                                compileAssignment(asgn, context, false);
                            }
                            return;
                        }
                    }
                }
            }
        }

        // if we get here, no optz cases work; fall back on unoptz.
        compileUnoptimizedMultipleAsgn(multipleAsgnNode, context, expr);
    }

    private void compileUnoptimizedMultipleAsgn(MultipleAsgnNode multipleAsgnNode, BodyCompiler context, boolean expr) {
        compile(multipleAsgnNode.getValueNode(), context, true);

        compileMultipleAsgnAssignment(multipleAsgnNode, context, expr);
    }

    public void compileMultipleAsgnAssignment(Node node, BodyCompiler context, boolean expr) {
        final MultipleAsgnNode multipleAsgnNode = (MultipleAsgnNode) node;

        // normal items at the "head" of the masgn
        ArrayCallback headAssignCallback = new ArrayCallback() {

                    public void nextValue(BodyCompiler context, Object sourceArray,
                            int index) {
                        ListNode headNode = (ListNode) sourceArray;
                        Node assignNode = headNode.get(index);

                        // perform assignment for the next node
                        compileAssignment(assignNode, context, false);
                    }
                };

        CompilerCallback argsCallback = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        Node argsNode = multipleAsgnNode.getArgsNode();
                        if (argsNode instanceof StarNode) {
                            // done processing args
                            context.consumeCurrentValue();
                        } else {
                            // assign to appropriate variable
                            compileAssignment(argsNode, context, false);
                        }
                    }
                };

        if (multipleAsgnNode.getHeadNode() == null) {
            if (multipleAsgnNode.getArgsNode() == null) {
                throw new NotCompilableException("Something's wrong, multiple assignment with no head or args at: " + multipleAsgnNode.getPosition());
            } else {
                if (multipleAsgnNode.getArgsNode() instanceof StarNode) {
                    // do nothing
                } else {
                    context.ensureMultipleAssignableRubyArray(multipleAsgnNode.getHeadNode() != null);

                    context.forEachInValueArray(0, 0, null, null, argsCallback);
                }
            }
        } else {
            context.ensureMultipleAssignableRubyArray(multipleAsgnNode.getHeadNode() != null);
            
            if (multipleAsgnNode.getArgsNode() == null) {
                context.forEachInValueArray(0, multipleAsgnNode.getHeadNode().size(), multipleAsgnNode.getHeadNode(), headAssignCallback, null);
            } else {
                context.forEachInValueArray(0, multipleAsgnNode.getHeadNode().size(), multipleAsgnNode.getHeadNode(), headAssignCallback, argsCallback);
            }
        }
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileNewline(Node node, BodyCompiler context, boolean expr) {
        // TODO: add trace call?
        context.lineNumber(node.getPosition());

        context.setLinePosition(node.getPosition());

        NewlineNode newlineNode = (NewlineNode) node;

        compile(newlineNode.getNextNode(), context, expr);
    }

    public void compileNext(Node node, BodyCompiler context, boolean expr) {
        final NextNode nextNode = (NextNode) node;

        CompilerCallback valueCallback = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        if (nextNode.getValueNode() != null) {
                            compile(nextNode.getValueNode(), context,true);
                        } else {
                            context.loadNil();
                        }
                    }
                };

        context.pollThreadEvents();
        context.issueNextEvent(valueCallback);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileNthRef(Node node, BodyCompiler context, boolean expr) {
        NthRefNode nthRefNode = (NthRefNode) node;

        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) context.nthRef(nthRefNode.getMatchNumber());
        } else {
            context.nthRef(nthRefNode.getMatchNumber());
            if (!expr) context.consumeCurrentValue();
        }
    }

    public void compileNil(Node node, BodyCompiler context, boolean expr) {
        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) {
                context.loadNil();
                context.pollThreadEvents();
            }
        } else {
            context.loadNil();
            context.pollThreadEvents();
            if (!expr) context.consumeCurrentValue();
        }
    }

    public void compileNot(Node node, BodyCompiler context, boolean expr) {
        NotNode notNode = (NotNode) node;

        compile(notNode.getConditionNode(), context, true);

        context.negateCurrentValue();
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileOpAsgnAnd(Node node, BodyCompiler context, boolean expr) {
        final BinaryOperatorNode andNode = (BinaryOperatorNode) node;

        compile(andNode.getFirstNode(), context,true);

        BranchCallback longCallback = new BranchCallback() {

                    public void branch(BodyCompiler context) {
                        compile(andNode.getSecondNode(), context,true);
                    }
                };

        context.performLogicalAnd(longCallback);
        context.pollThreadEvents();
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileOpAsgnOr(Node node, BodyCompiler context, boolean expr) {
        final OpAsgnOrNode orNode = (OpAsgnOrNode) node;

        if (needsDefinitionCheck(orNode.getFirstNode())) {
            compileGetDefinitionBase(orNode.getFirstNode(), context);

            context.isNull(new BranchCallback() {

                        public void branch(BodyCompiler context) {
                            compile(orNode.getSecondNode(), context,true);
                        }
                    }, new BranchCallback() {

                        public void branch(BodyCompiler context) {
                            compile(orNode.getFirstNode(), context,true);
                            context.duplicateCurrentValue();
                            context.performBooleanBranch(new BranchCallback() {

                                        public void branch(BodyCompiler context) {
                                        //Do nothing
                                        }
                                    },
                                    new BranchCallback() {

                                        public void branch(BodyCompiler context) {
                                            context.consumeCurrentValue();
                                            compile(orNode.getSecondNode(), context,true);
                                        }
                                    });
                        }
                    });
        } else {
            compile(orNode.getFirstNode(), context,true);
            context.duplicateCurrentValue();
            context.performBooleanBranch(new BranchCallback() {
                public void branch(BodyCompiler context) {
                //Do nothing
                }
            },
            new BranchCallback() {
                public void branch(BodyCompiler context) {
                    context.consumeCurrentValue();
                    compile(orNode.getSecondNode(), context,true);
                }
            });

        }

        context.pollThreadEvents();
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    /**
     * Check whether the given node is considered always "defined" or whether it
     * has some form of definition check.
     *
     * @param node Then node to check
     * @return Whether the type of node represents a possibly undefined construct
     */
    private boolean needsDefinitionCheck(Node node) {
        switch (node.getNodeType()) {
        case CLASSVARASGNNODE:
        case CLASSVARDECLNODE:
        case CONSTDECLNODE:
        case DASGNNODE:
        case GLOBALASGNNODE:
        case LOCALASGNNODE:
        case MULTIPLEASGNNODE:
        case OPASGNNODE:
        case OPELEMENTASGNNODE:
        case DVARNODE:
        case FALSENODE:
        case TRUENODE:
        case LOCALVARNODE:
        case MATCH2NODE:
        case MATCH3NODE:
        case NILNODE:
        case SELFNODE:
            // all these types are immediately considered "defined"
            return false;
        default:
            return true;
        }
    }

    public void compileOpAsgn(Node node, BodyCompiler context, boolean expr) {
        final OpAsgnNode opAsgnNode = (OpAsgnNode) node;

        if (opAsgnNode.getOperatorName().equals("||")) {
            compileOpAsgnWithOr(opAsgnNode, context, true);
        } else if (opAsgnNode.getOperatorName().equals("&&")) {
            compileOpAsgnWithAnd(opAsgnNode, context, true);
        } else {
            compileOpAsgnWithMethod(opAsgnNode, context, true);
        }

        context.pollThreadEvents();
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileOpAsgnWithOr(Node node, BodyCompiler context, boolean expr) {
        final OpAsgnNode opAsgnNode = (OpAsgnNode) node;

        final CompilerCallback receiverCallback = new CompilerCallback() {

            public void call(BodyCompiler context) {
                compile(opAsgnNode.getReceiverNode(), context, true); // [recv]
            }
        };
        
        ArgumentsCallback argsCallback = getArgsCallback(opAsgnNode.getValueNode());
        
        context.getInvocationCompiler().invokeOpAsgnWithOr(opAsgnNode.getVariableName(), opAsgnNode.getVariableNameAsgn(), receiverCallback, argsCallback);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileOpAsgnWithAnd(Node node, BodyCompiler context, boolean expr) {
        final OpAsgnNode opAsgnNode = (OpAsgnNode) node;

        final CompilerCallback receiverCallback = new CompilerCallback() {

            public void call(BodyCompiler context) {
                compile(opAsgnNode.getReceiverNode(), context, true); // [recv]
            }
        };
        
        ArgumentsCallback argsCallback = getArgsCallback(opAsgnNode.getValueNode());
        
        context.getInvocationCompiler().invokeOpAsgnWithAnd(opAsgnNode.getVariableName(), opAsgnNode.getVariableNameAsgn(), receiverCallback, argsCallback);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileOpAsgnWithMethod(Node node, BodyCompiler context, boolean expr) {
        final OpAsgnNode opAsgnNode = (OpAsgnNode) node;

        final CompilerCallback receiverCallback = new CompilerCallback() {
                    public void call(BodyCompiler context) {
                        compile(opAsgnNode.getReceiverNode(), context, true); // [recv]
                    }
                };

        // eval new value, call operator on old value, and assign
        ArgumentsCallback argsCallback = new ArgumentsCallback() {
            public int getArity() {
                return 1;
            }

            public void call(BodyCompiler context) {
                compile(opAsgnNode.getValueNode(), context, true);
            }
        };
        
        context.getInvocationCompiler().invokeOpAsgnWithMethod(opAsgnNode.getOperatorName(), opAsgnNode.getVariableName(), opAsgnNode.getVariableNameAsgn(), receiverCallback, argsCallback);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileOpElementAsgn(Node node, BodyCompiler context, boolean expr) {
        final OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode) node;
        
        if (opElementAsgnNode.getOperatorName() == "||") {
            compileOpElementAsgnWithOr(node, context, expr);
        } else if (opElementAsgnNode.getOperatorName() == "&&") {
            compileOpElementAsgnWithAnd(node, context, expr);
        } else {
            compileOpElementAsgnWithMethod(node, context, expr);
        }
    }
    
    private class OpElementAsgnArgumentsCallback implements ArgumentsCallback  {
        private Node node;

        public OpElementAsgnArgumentsCallback(Node node) {
            this.node = node;
        }
        
        public int getArity() {
            switch (node.getNodeType()) {
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

        public void call(BodyCompiler context) {
            if (getArity() == 1) {
                // if arity 1, just compile the one element to save us the array cost
                compile(((ArrayNode)node).get(0), context,true);
            } else {
                // compile into array
                compileArguments(node, context);
            }
        }
    };

    public void compileOpElementAsgnWithOr(Node node, BodyCompiler context, boolean expr) {
        final OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {
            public void call(BodyCompiler context) {
                compile(opElementAsgnNode.getReceiverNode(), context, true);
            }
        };

        ArgumentsCallback argsCallback = new OpElementAsgnArgumentsCallback(opElementAsgnNode.getArgsNode());

        CompilerCallback valueCallback = new CompilerCallback() {
            public void call(BodyCompiler context) {
                compile(opElementAsgnNode.getValueNode(), context, true);
            }
        };

        context.getInvocationCompiler().opElementAsgnWithOr(receiverCallback, argsCallback, valueCallback);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileOpElementAsgnWithAnd(Node node, BodyCompiler context, boolean expr) {
        final OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {
            public void call(BodyCompiler context) {
                compile(opElementAsgnNode.getReceiverNode(), context, true);
            }
        };

        ArgumentsCallback argsCallback = new OpElementAsgnArgumentsCallback(opElementAsgnNode.getArgsNode()); 

        CompilerCallback valueCallback = new CompilerCallback() {
            public void call(BodyCompiler context) {
                compile(opElementAsgnNode.getValueNode(), context, true);
            }
        };

        context.getInvocationCompiler().opElementAsgnWithAnd(receiverCallback, argsCallback, valueCallback);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileOpElementAsgnWithMethod(Node node, BodyCompiler context, boolean expr) {
        final OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {
            public void call(BodyCompiler context) {
                compile(opElementAsgnNode.getReceiverNode(), context,true);
            }
        };

        ArgumentsCallback argsCallback = getArgsCallback(opElementAsgnNode.getArgsNode());

        CompilerCallback valueCallback = new CompilerCallback() {
            public void call(BodyCompiler context) {
                compile(opElementAsgnNode.getValueNode(), context,true);
            }
        };

        context.getInvocationCompiler().opElementAsgnWithMethod(receiverCallback, argsCallback, valueCallback, opElementAsgnNode.getOperatorName());
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileOr(Node node, BodyCompiler context, boolean expr) {
        final OrNode orNode = (OrNode) node;

        if (orNode.getFirstNode().getNodeType().alwaysTrue()) {
            // compile first node only
            compile(orNode.getFirstNode(), context, expr);
        } else if (orNode.getFirstNode().getNodeType().alwaysFalse()) {
            // compile first node as non-expr and compile second node
            compile(orNode.getFirstNode(), context, false);
            compile(orNode.getSecondNode(), context, expr);
        } else {
            compile(orNode.getFirstNode(), context, true);

            BranchCallback longCallback = new BranchCallback() {

                        public void branch(BodyCompiler context) {
                            compile(orNode.getSecondNode(), context, true);
                        }
                    };

            context.performLogicalOr(longCallback);
            // TODO: don't require pop
            if (!expr) context.consumeCurrentValue();
        }
    }

    public void compilePostExe(Node node, BodyCompiler context, boolean expr) {
        final PostExeNode postExeNode = (PostExeNode) node;

        // create the closure class and instantiate it
        final CompilerCallback closureBody = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        if (postExeNode.getBodyNode() != null) {
                            compile(postExeNode.getBodyNode(), context, true);
                        } else {
                            context.loadNil();
                        }
                    }
                };
        context.createNewEndBlock(closureBody);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compilePreExe(Node node, BodyCompiler context, boolean expr) {
        final PreExeNode preExeNode = (PreExeNode) node;

        // create the closure class and instantiate it
        final CompilerCallback closureBody = new CompilerCallback() {

                    public void call(BodyCompiler context) {
                        if (preExeNode.getBodyNode() != null) {
                            compile(preExeNode.getBodyNode(), context,true);
                        } else {
                            context.loadNil();
                        }
                    }
                };
        context.runBeginBlock(preExeNode.getScope(), closureBody);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileRedo(Node node, BodyCompiler context, boolean expr) {
        //RedoNode redoNode = (RedoNode)node;

        context.issueRedoEvent();
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileRegexp(Node node, BodyCompiler context, boolean expr) {
        RegexpNode reNode = (RegexpNode) node;

        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) context.createNewRegexp(reNode.getValue(), reNode.getOptions());
        } else {
            context.createNewRegexp(reNode.getValue(), reNode.getOptions());
            if (!expr) context.consumeCurrentValue();
        }
    }

    public void compileRescue(Node node, BodyCompiler context, boolean expr) {
        compileRescueInternal(node, context, false);
        
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    private void compileRescueInternal(Node node, BodyCompiler context, final boolean light) {
        final RescueNode rescueNode = (RescueNode) node;

        BranchCallback body = new BranchCallback() {
            public void branch(BodyCompiler context) {
                if (rescueNode.getBodyNode() != null) {
                    compile(rescueNode.getBodyNode(), context, true);
                } else {
                    context.loadNil();
                }

                if (rescueNode.getElseNode() != null) {
                    context.consumeCurrentValue();
                    compile(rescueNode.getElseNode(), context, true);
                }
            }
        };

        BranchCallback rubyHandler = new BranchCallback() {
            public void branch(BodyCompiler context) {
                compileRescueBodyInternal(rescueNode.getRescueNode(), context, light);
            }
        };

        ASTInspector rescueInspector = new ASTInspector();
        rescueInspector.inspect(rescueNode.getRescueNode());
        if (light) {
            context.performRescueLight(body, rubyHandler, rescueInspector.getFlag(ASTInspector.RETRY));
        } else {
            context.performRescue(body, rubyHandler, rescueInspector.getFlag(ASTInspector.RETRY));
        }
    }

    private void compileRescueBodyInternal(Node node, BodyCompiler context, final boolean light) {
        final RescueBodyNode rescueBodyNode = (RescueBodyNode) node;

        context.loadException();

        final Node exceptionList = rescueBodyNode.getExceptionNodes();
        ArgumentsCallback rescueArgs = getArgsCallback(exceptionList);
        if (rescueArgs == null) rescueArgs = new ArgumentsCallback() {
            public int getArity() {
                return 1;
            }

            public void call(BodyCompiler context) {
                context.loadStandardError();
            }
        };

        context.checkIsExceptionHandled(rescueArgs);

        BranchCallback trueBranch = new BranchCallback() {
            public void branch(BodyCompiler context) {
                // check if it's an immediate, and don't outline
                Node realBody = rescueBodyNode.getBodyNode();
                if (realBody instanceof NewlineNode) {
                    context.setLinePosition(realBody.getPosition());
                    while (realBody instanceof NewlineNode) {
                        realBody = ((NewlineNode)realBody).getNextNode();
                    }
                }

                if (realBody.getNodeType().isImmediate()) {
                    compile(realBody, context, true);
                    context.clearErrorInfo();
                } else {
                    context.storeExceptionInErrorInfo();
                    if (light) {
                        compile(rescueBodyNode.getBodyNode(), context, true);
                    } else {
                        BodyCompiler nestedBody = context.outline("rescue_line_" + rescueBodyNode.getPosition().getStartLine());
                        compile(rescueBodyNode.getBodyNode(), nestedBody, true);
                        nestedBody.endBody();
                    }

                    // FIXME: this should reset to what it was before
                    context.clearErrorInfo();
                }
            }
        };

        BranchCallback falseBranch = new BranchCallback() {
            public void branch(BodyCompiler context) {
                if (rescueBodyNode.getOptRescueNode() != null) {
                    compileRescueBodyInternal(rescueBodyNode.getOptRescueNode(), context, light);
                } else {
                    context.rethrowException();
                }
            }
        };

        context.performBooleanBranch(trueBranch, falseBranch);
    }

    public void compileRetry(Node node, BodyCompiler context, boolean expr) {
        context.pollThreadEvents();

        context.issueRetryEvent();
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileReturn(Node node, BodyCompiler context, boolean expr) {
        ReturnNode returnNode = (ReturnNode) node;

        if (returnNode.getValueNode() != null) {
            compile(returnNode.getValueNode(), context,true);
        } else {
            context.loadNil();
        }

        context.performReturn();
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileRoot(Node node, ScriptCompiler context, ASTInspector inspector) {
        compileRoot(node, context, inspector, true, true);
    }

    public void compileRoot(Node node, ScriptCompiler context, ASTInspector inspector, boolean load, boolean main) {
        RootNode rootNode = (RootNode) node;
        StaticScope staticScope = rootNode.getStaticScope();

        context.startScript(staticScope);

        // force static scope to claim restarg at 0, so it only implements the [] version of __file__
        staticScope.setRestArg(-2);

        // create method for toplevel of script
        BodyCompiler methodCompiler = context.startFileMethod(null, staticScope, inspector);

        Node nextNode = rootNode.getBodyNode();
        if (nextNode != null) {
            if (nextNode.getNodeType() == NodeType.BLOCKNODE) {
                // it's a multiple-statement body, iterate over all elements in turn and chain if it get too long
                BlockNode blockNode = (BlockNode) nextNode;

                for (int i = 0; i < blockNode.size(); i++) {
                    if ((i + 1) % RubyInstanceConfig.CHAINED_COMPILE_LINE_COUNT == 0) {
                        methodCompiler = methodCompiler.chainToMethod("__file__from_line_" + (i + 1));
                    }
                    compile(blockNode.get(i), methodCompiler, i + 1 >= blockNode.size());
                }
            } else {
                // single-statement body, just compile it
                compile(nextNode, methodCompiler,true);
            }
        } else {
            methodCompiler.loadNil();
        }

        methodCompiler.endBody();

        context.endScript(load, main);
    }

    public void compileSelf(Node node, BodyCompiler context, boolean expr) {
        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) context.retrieveSelf();
        } else {
            context.retrieveSelf();
            if (!expr) context.consumeCurrentValue();
        }
    }

    public void compileSplat(Node node, BodyCompiler context, boolean expr) {
        SplatNode splatNode = (SplatNode) node;

        compile(splatNode.getValue(), context, true);

        context.splatCurrentValue();
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileStr(Node node, BodyCompiler context, boolean expr) {
        StrNode strNode = (StrNode) node;

        boolean doit = expr || !RubyInstanceConfig.PEEPHOLE_OPTZ;
        boolean popit = !RubyInstanceConfig.PEEPHOLE_OPTZ && !expr;

        if (doit) {
            if (strNode instanceof FileNode) {
                context.loadFilename();
            } else {
                context.createNewString(strNode.getValue());
            }
        }
        if (popit) context.consumeCurrentValue();
    }

    public void compileSuper(Node node, BodyCompiler context, boolean expr) {
        final SuperNode superNode = (SuperNode) node;

        ArgumentsCallback argsCallback = getArgsCallback(superNode.getArgsNode());

        CompilerCallback closureArg = getBlock(superNode.getIterNode());

        context.getInvocationCompiler().invokeDynamic(null, null, argsCallback, CallType.SUPER, closureArg, superNode.getIterNode() instanceof IterNode);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileSValue(Node node, BodyCompiler context, boolean expr) {
        SValueNode svalueNode = (SValueNode) node;

        compile(svalueNode.getValue(), context,true);

        context.singlifySplattedValue();
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileSymbol(Node node, BodyCompiler context, boolean expr) {
        context.createNewSymbol(((SymbolNode) node).getName());
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }    
    
    public void compileToAry(Node node, BodyCompiler context, boolean expr) {
        ToAryNode toAryNode = (ToAryNode) node;

        compile(toAryNode.getValue(), context,true);

        context.aryToAry();
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileTrue(Node node, BodyCompiler context, boolean expr) {
        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) {
                context.loadTrue();
                context.pollThreadEvents();
            }
        } else {
            context.loadTrue();
            context.pollThreadEvents();
            if (!expr) context.consumeCurrentValue();
        }
    }

    public void compileUndef(Node node, BodyCompiler context, boolean expr) {
        context.undefMethod(((UndefNode) node).getName());
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileUntil(Node node, BodyCompiler context, boolean expr) {
        final UntilNode untilNode = (UntilNode) node;

        if (untilNode.getConditionNode().getNodeType().alwaysTrue() &&
                untilNode.evaluateAtStart()) {
            // condition is always true, just compile it and not body
            compile(untilNode.getConditionNode(), context, false);
            if (expr) context.loadNil();
        } else {
            BranchCallback condition = new BranchCallback() {

                public void branch(BodyCompiler context) {
                    compile(untilNode.getConditionNode(), context, true);
                    context.negateCurrentValue();
                }
            };

            BranchCallback body = new BranchCallback() {

                public void branch(BodyCompiler context) {
                    if (untilNode.getBodyNode() != null) {
                        compile(untilNode.getBodyNode(), context, true);
                    }
                }
            };

            if (untilNode.containsNonlocalFlow) {
                context.performBooleanLoopSafe(condition, body, untilNode.evaluateAtStart());
            } else {
                context.performBooleanLoopLight(condition, body, untilNode.evaluateAtStart());
            }

            context.pollThreadEvents();
            // TODO: don't require pop
            if (!expr) context.consumeCurrentValue();
        }
    }

    public void compileVAlias(Node node, BodyCompiler context, boolean expr) {
        VAliasNode valiasNode = (VAliasNode) node;

        context.aliasGlobal(valiasNode.getNewName(), valiasNode.getOldName());
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileVCall(Node node, BodyCompiler context, boolean expr) {
        VCallNode vcallNode = (VCallNode) node;
        
        context.getInvocationCompiler().invokeDynamic(vcallNode.getName(), null, null, CallType.VARIABLE, null, false);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileWhile(Node node, BodyCompiler context, boolean expr) {
        final WhileNode whileNode = (WhileNode) node;

        if (whileNode.getConditionNode().getNodeType().alwaysFalse() &&
                whileNode.evaluateAtStart()) {
            // do nothing
            if (expr) context.loadNil();
        } else {
            BranchCallback condition = new BranchCallback() {

                public void branch(BodyCompiler context) {
                    compile(whileNode.getConditionNode(), context, true);
                }
            };

            BranchCallback body = new BranchCallback() {

                public void branch(BodyCompiler context) {
                    if (whileNode.getBodyNode() != null) {
                        compile(whileNode.getBodyNode(), context, true);
                    }
                }
            };

            if (whileNode.containsNonlocalFlow) {
                context.performBooleanLoopSafe(condition, body, whileNode.evaluateAtStart());
            } else {
                context.performBooleanLoopLight(condition, body, whileNode.evaluateAtStart());
            }

            context.pollThreadEvents();
            // TODO: don't require pop
            if (!expr) context.consumeCurrentValue();
        }
    }

    public void compileXStr(Node node, BodyCompiler context, boolean expr) {
        final XStrNode xstrNode = (XStrNode) node;

        ArgumentsCallback argsCallback = new ArgumentsCallback() {
            public int getArity() {
                return 1;
            }

            public void call(BodyCompiler context) {
                context.createNewString(xstrNode.getValue());
            }
        };
        context.getInvocationCompiler().invokeDynamic("`", null, argsCallback, CallType.FUNCTIONAL, null, false);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileYield(Node node, BodyCompiler context, boolean expr) {
        final YieldNode yieldNode = (YieldNode) node;

        ArgumentsCallback argsCallback = getArgsCallback(yieldNode.getArgsNode());

        // TODO: This filtering is kind of gross...it would be nice to get some parser help here
        if (argsCallback == null || argsCallback.getArity() == 0) {
            context.getInvocationCompiler().yieldSpecific(argsCallback);
        } else if ((argsCallback.getArity() == 1 || argsCallback.getArity() == 2 || argsCallback.getArity() == 3) && yieldNode.getExpandArguments()) {
            // send it along as arity-specific, we don't need the array
            context.getInvocationCompiler().yieldSpecific(argsCallback);
        } else {
            CompilerCallback argsCallback2 = null;
            if (yieldNode.getArgsNode() != null) {
                argsCallback2 = new CompilerCallback() {
                    public void call(BodyCompiler context) {
                        compile(yieldNode.getArgsNode(), context,true);
                    }
                };
            }

            context.getInvocationCompiler().yield(argsCallback2, yieldNode.getExpandArguments());
        }
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileZArray(Node node, BodyCompiler context, boolean expr) {
        boolean doit = expr || !RubyInstanceConfig.PEEPHOLE_OPTZ;
        boolean popit = !RubyInstanceConfig.PEEPHOLE_OPTZ && !expr;

        if (doit) {
            context.createEmptyArray();
        }

        if (popit) context.consumeCurrentValue();
    }

    public void compileZSuper(Node node, BodyCompiler context, boolean expr) {
        ZSuperNode zsuperNode = (ZSuperNode) node;

        CompilerCallback closure = getBlock(zsuperNode.getIterNode());

        context.callZSuper(closure);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileArgsCatArguments(Node node, BodyCompiler context, boolean expr) {
        ArgsCatNode argsCatNode = (ArgsCatNode) node;

        compileArguments(argsCatNode.getFirstNode(), context);
        // arguments compilers always create IRubyObject[], but we want to use RubyArray.concat here;
        // FIXME: as a result, this is NOT efficient, since it creates and then later unwraps an array
        context.createNewArray(true);
        compile(argsCatNode.getSecondNode(), context,true);
        context.splatCurrentValue();
        context.concatArrays();
        context.convertToJavaArray();
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileArgsPushArguments(Node node, BodyCompiler context, boolean expr) {
        ArgsPushNode argsPushNode = (ArgsPushNode) node;
        compile(argsPushNode.getFirstNode(), context,true);
        compile(argsPushNode.getSecondNode(), context,true);
        context.appendToArray();
        context.convertToJavaArray();
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }

    public void compileArrayArguments(Node node, BodyCompiler context, boolean expr) {
        ArrayNode arrayNode = (ArrayNode) node;

        ArrayCallback callback = new ArrayCallback() {

                    public void nextValue(BodyCompiler context, Object sourceArray, int index) {
                        Node node = (Node) ((Object[]) sourceArray)[index];
                        compile(node, context,true);
                    }
                };

        context.setLinePosition(arrayNode.getPosition());
        context.createObjectArray(arrayNode.childNodes().toArray(), callback);
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    // leave as a normal array
    }

    public void compileSplatArguments(Node node, BodyCompiler context, boolean expr) {
        SplatNode splatNode = (SplatNode) node;

        compile(splatNode.getValue(), context,true);
        context.splatCurrentValue();
        context.convertToJavaArray();
        // TODO: don't require pop
        if (!expr) context.consumeCurrentValue();
    }
}
