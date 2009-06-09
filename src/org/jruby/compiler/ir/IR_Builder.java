package org.jruby.buildr.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.jruby.RubyMatchData;
import org.jruby.ast.ArgsCatNode;
import org.jruby.ast.ArgsPushNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.CaseNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.Colon2ConstNode;
import org.jruby.ast.Colon2MethodNode;
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.DSymbolNode;
import org.jruby.ast.DXStrNode;
import org.jruby.ast.DefsNode;
import org.jruby.ast.FileNode;
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
import org.jruby.ast.WhenOneArgNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.BlockBody;

// This class converts an AST into a bunch of IR instructions
public class IR_Builder
{
    private boolean isAtRoot = false;

    public IR_Method defineNewMethod(String name, int methodArity, StaticScope scope, ASTInspector inspector, boolean root)
    {
        IR_Method m = new IR_Method(name, root);
            // Add IR instructions specific to receiving method arguments
        m.addInstr(...)
        ...
        ...

        return m;
    }

    public void build(Node node, IR_BuilderContext m, boolean expr) {
        if (node == null) {
            return;
        }
        switch (node.getNodeType()) {
            case ALIASNODE:
                buildAlias(node, m, expr);
                break;
            case ANDNODE:
                buildAnd(node, m, expr);
                break;
            case ARGSCATNODE:
                buildArgsCat(node, m, expr);
                break;
            case ARGSPUSHNODE:
                buildArgsPush(node, m, expr);
                break;
            case ARRAYNODE:
                buildArray(node, m, expr);
                break;
            case ATTRASSIGNNODE:
                buildAttrAssign(node, m, expr);
                break;
            case BACKREFNODE:
                buildBackref(node, m, expr);
                break;
            case BEGINNODE:
                buildBegin(node, m, expr);
                break;
            case BIGNUMNODE:
                buildBignum(node, m, expr);
                break;
            case BLOCKNODE:
                buildBlock(node, m, expr);
                break;
            case BREAKNODE:
                buildBreak(node, m, expr);
                break;
            case CALLNODE:
                buildCall(node, m, expr);
                break;
            case CASENODE:
                buildCase(node, m, expr);
                break;
            case CLASSNODE:
                buildClass(node, m, expr);
                break;
            case CLASSVARNODE:
                buildClassVar(node, m, expr);
                break;
            case CLASSVARASGNNODE:
                buildClassVarAsgn(node, m, expr);
                break;
            case CLASSVARDECLNODE:
                buildClassVarDecl(node, m, expr);
                break;
            case COLON2NODE:
                buildColon2(node, m, expr);
                break;
            case COLON3NODE:
                buildColon3(node, m, expr);
                break;
            case CONSTDECLNODE:
                buildConstDecl(node, m, expr);
                break;
            case CONSTNODE:
                buildConst(node, m, expr);
                break;
            case DASGNNODE:
                buildDAsgn(node, m, expr);
                break;
            case DEFINEDNODE:
                buildDefined(node, m, expr);
                break;
            case DEFNNODE:
                buildDefn(node, m, expr);
                break;
            case DEFSNODE:
                buildDefs(node, m, expr);
                break;
            case DOTNODE:
                buildDot(node, m, expr);
                break;
            case DREGEXPNODE:
                buildDRegexp(node, m, expr);
                break;
            case DSTRNODE:
                buildDStr(node, m, expr);
                break;
            case DSYMBOLNODE:
                buildDSymbol(node, m, expr);
                break;
            case DVARNODE:
                buildDVar(node, m, expr);
                break;
            case DXSTRNODE:
                buildDXStr(node, m, expr);
                break;
            case ENSURENODE:
                buildEnsureNode(node, m, expr);
                break;
            case EVSTRNODE:
                buildEvStr(node, m, expr);
                break;
            case FALSENODE:
                buildFalse(node, m, expr);
                break;
            case FCALLNODE:
                buildFCall(node, m, expr);
                break;
            case FIXNUMNODE:
                buildFixnum(node, m, expr);
                break;
            case FLIPNODE:
                buildFlip(node, m, expr);
                break;
            case FLOATNODE:
                buildFloat(node, m, expr);
                break;
            case FORNODE:
                buildFor(node, m, expr);
                break;
            case GLOBALASGNNODE:
                buildGlobalAsgn(node, m, expr);
                break;
            case GLOBALVARNODE:
                buildGlobalVar(node, m, expr);
                break;
            case HASHNODE:
                buildHash(node, m, expr);
                break;
            case IFNODE:
                buildIf(node, m, expr);
                break;
            case INSTASGNNODE:
                buildInstAsgn(node, m, expr);
                break;
            case INSTVARNODE:
                buildInstVar(node, m, expr);
                break;
            case ITERNODE:
                buildIter(node, m);
                break;
            case LOCALASGNNODE:
                buildLocalAsgn(node, m, expr);
                break;
            case LOCALVARNODE:
                buildLocalVar(node, m, expr);
                break;
            case MATCH2NODE:
                buildMatch2(node, m, expr);
                break;
            case MATCH3NODE:
                buildMatch3(node, m, expr);
                break;
            case MATCHNODE:
                buildMatch(node, m, expr);
                break;
            case MODULENODE:
                buildModule(node, m, expr);
                break;
            case MULTIPLEASGNNODE:
                buildMultipleAsgn(node, m, expr);
                break;
            case NEWLINENODE:
                buildNewline(node, m, expr);
                break;
            case NEXTNODE:
                buildNext(node, m, expr);
                break;
            case NTHREFNODE:
                buildNthRef(node, m, expr);
                break;
            case NILNODE:
                buildNil(node, m, expr);
                break;
            case NOTNODE:
                buildNot(node, m, expr);
                break;
            case OPASGNANDNODE:
                buildOpAsgnAnd(node, m, expr);
                break;
            case OPASGNNODE:
                buildOpAsgn(node, m, expr);
                break;
            case OPASGNORNODE:
                buildOpAsgnOr(node, m, expr);
                break;
            case OPELEMENTASGNNODE:
                buildOpElementAsgn(node, m, expr);
                break;
            case ORNODE:
                buildOr(node, m, expr);
                break;
            case POSTEXENODE:
                buildPostExe(node, m, expr);
                break;
            case PREEXENODE:
                buildPreExe(node, m, expr);
                break;
            case REDONODE:
                buildRedo(node, m, expr);
                break;
            case REGEXPNODE:
                buildRegexp(node, m, expr);
                break;
            case RESCUEBODYNODE:
                throw new NotCompilableException("rescue body is handled by rescue compilation at: " + node.getPosition());
            case RESCUENODE:
                buildRescue(node, m, expr);
                break;
            case RETRYNODE:
                buildRetry(node, m, expr);
                break;
            case RETURNNODE:
                buildReturn(node, m, expr);
                break;
            case ROOTNODE:
                throw new NotCompilableException("Use buildRoot(); Root node at: " + node.getPosition());
            case SCLASSNODE:
                buildSClass(node, m, expr);
                break;
            case SELFNODE:
                buildSelf(node, m, expr);
                break;
            case SPLATNODE:
                buildSplat(node, m, expr);
                break;
            case STRNODE:
                buildStr(node, m, expr);
                break;
            case SUPERNODE:
                buildSuper(node, m, expr);
                break;
            case SVALUENODE:
                buildSValue(node, m, expr);
                break;
            case SYMBOLNODE:
                buildSymbol(node, m, expr);
                break;
            case TOARYNODE:
                buildToAry(node, m, expr);
                break;
            case TRUENODE:
                buildTrue(node, m, expr);
                break;
            case UNDEFNODE:
                buildUndef(node, m, expr);
                break;
            case UNTILNODE:
                buildUntil(node, m, expr);
                break;
            case VALIASNODE:
                buildVAlias(node, m, expr);
                break;
            case VCALLNODE:
                buildVCall(node, m, expr);
                break;
            case WHILENODE:
                buildWhile(node, m, expr);
                break;
            case WHENNODE:
                assert false : "When nodes are handled by case node compilation.";
                break;
            case XSTRNODE:
                buildXStr(node, m, expr);
                break;
            case YIELDNODE:
                buildYield(node, m, expr);
                break;
            case ZARRAYNODE:
                buildZArray(node, m, expr);
                break;
            case ZSUPERNODE:
                buildZSuper(node, m, expr);
                break;
            default:
                throw new NotCompilableException("Unknown node encountered in buildr: " + node);
        }
    }

    public void buildArguments(Node node, IR_BuilderContext m) {
        switch (node.getNodeType()) {
            case ARGSCATNODE:
                buildArgsCatArguments(node, m, true);
                break;
            case ARGSPUSHNODE:
                buildArgsPushArguments(node, m, true);
                break;
            case ARRAYNODE:
                buildArrayArguments(node, m, true);
                break;
            case SPLATNODE:
                buildSplatArguments(node, m, true);
                break;
            default:
                build(node, m, true);
                m.convertToJavaArray();
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
        
        public void call(IR_BuilderContext m) {
            buildArguments(node, m);
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
        
        public void call(IR_BuilderContext m) {
            if (node.getNodeType() == NodeType.ARRAYNODE) {
                ArrayNode arrayNode = (ArrayNode)node;
                if (arrayNode.isLightweight()) {
                    // explode array, it's an internal "args" array
                    for (Node n : arrayNode.childNodes()) {
                        build(n, m,true);
                    }
                } else {
                    // use array as-is, it's a literal array
                    build(arrayNode, m,true);
                }
            } else {
                build(node, m,true);
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

    public void buildAssignment(Node node, IR_BuilderContext m, boolean expr) {
        switch (node.getNodeType()) {
            case ATTRASSIGNNODE:
                buildAttrAssignAssignment(node, m, expr);
                break;
            case DASGNNODE:
                DAsgnNode dasgnNode = (DAsgnNode)node;
                m.getVariableCompiler().assignLocalVariable(dasgnNode.getIndex(), dasgnNode.getDepth(), expr);
                break;
            case CLASSVARASGNNODE:
                buildClassVarAsgnAssignment(node, m, expr);
                break;
            case CLASSVARDECLNODE:
                buildClassVarDeclAssignment(node, m, expr);
                break;
            case CONSTDECLNODE:
                buildConstDeclAssignment(node, m, expr);
                break;
            case GLOBALASGNNODE:
                buildGlobalAsgnAssignment(node, m, expr);
                break;
            case INSTASGNNODE:
                buildInstAsgnAssignment(node, m, expr);
                break;
            case LOCALASGNNODE:
                LocalAsgnNode localAsgnNode = (LocalAsgnNode)node;
                m.getVariableCompiler().assignLocalVariable(localAsgnNode.getIndex(), localAsgnNode.getDepth(), expr);
                break;
            case MULTIPLEASGNNODE:
                buildMultipleAsgnAssignment(node, m, expr);
                break;
            case ZEROARGNODE:
                throw new NotCompilableException("Shouldn't get here; zeroarg does not do assignment: " + node);
            default:
                throw new NotCompilableException("Can't build assignment node: " + node);
        }
    }

    public void buildAlias(Node node, IR_BuilderContext m, boolean expr) {
        final AliasNode alias = (AliasNode) node;

        m.defineAlias(alias.getNewName(), alias.getOldName());
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildAnd(Node node, IR_BuilderContext m, final boolean expr) {
        final AndNode andNode = (AndNode) node;

        if (andNode.getFirstNode().getNodeType().alwaysTrue()) {
            // build first node as non-expr and then second node
            build(andNode.getFirstNode(), m, false);
            build(andNode.getSecondNode(), m, expr);
        } else if (andNode.getFirstNode().getNodeType().alwaysFalse()) {
            // build first node only
            build(andNode.getFirstNode(), m, expr);
        } else {
            build(andNode.getFirstNode(), m, true);
            BranchCallback longCallback = new BranchCallback() {
                        public void branch(IR_BuilderContext m) {
                            build(andNode.getSecondNode(), m, true);
                        }
                    };

            m.performLogicalAnd(longCallback);
            if (!expr) m.consumeCurrentValue();
        }
    }

    public void buildArray(Node node, IR_BuilderContext m, boolean expr) {
        ArrayNode arrayNode = (ArrayNode) node;

        boolean doit = expr || !RubyInstanceConfig.PEEPHOLE_OPTZ;
        boolean popit = !RubyInstanceConfig.PEEPHOLE_OPTZ && !expr;
        
        if (doit) {
            ArrayCallback callback = new ArrayCallback() {

                        public void nextValue(IR_BuilderContext m, Object sourceArray, int index) {
                            Node node = (Node) ((Object[]) sourceArray)[index];
                            build(node, m, true);
                        }
                    };

            m.createNewArray(arrayNode.childNodes().toArray(), callback, arrayNode.isLightweight());

            if (popit) m.consumeCurrentValue();
        } else {
            for (Iterator<Node> iter = arrayNode.childNodes().iterator(); iter.hasNext();) {
                Node nextNode = iter.next();
                build(nextNode, m, false);
            }
        }
    }

    public void buildArgsCat(Node node, IR_BuilderContext m, boolean expr) {
        ArgsCatNode argsCatNode = (ArgsCatNode) node;

        build(argsCatNode.getFirstNode(), m,true);
        m.ensureRubyArray();
        build(argsCatNode.getSecondNode(), m,true);
        m.splatCurrentValue();
        m.concatArrays();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildArgsPush(Node node, IR_BuilderContext m, boolean expr) {
        throw new NotCompilableException("ArgsPush should never be encountered bare in 1.8");
    }

    private void buildAttrAssign(Node node, IR_BuilderContext m, boolean expr) {
        final AttrAssignNode attrAssignNode = (AttrAssignNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {
            public void call(IR_BuilderContext m) {
                build(attrAssignNode.getReceiverNode(), m,true);
            }
        };
        
        ArgumentsCallback argsCallback = getArgsCallback(attrAssignNode.getArgsNode());

        m.getInvocationCompiler().invokeAttrAssign(attrAssignNode.getName(), receiverCallback, argsCallback);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildAttrAssignAssignment(Node node, IR_BuilderContext m, boolean expr) {
        final AttrAssignNode attrAssignNode = (AttrAssignNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {
            public void call(IR_BuilderContext m) {
                build(attrAssignNode.getReceiverNode(), m,true);
            }
        };
        ArgumentsCallback argsCallback = getArgsCallback(attrAssignNode.getArgsNode());

        m.getInvocationCompiler().invokeAttrAssignMasgn(attrAssignNode.getName(), receiverCallback, argsCallback);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildBackref(Node node, IR_BuilderContext m, boolean expr) {
        BackRefNode iVisited = (BackRefNode) node;

        m.performBackref(iVisited.getType());
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildBegin(Node node, IR_BuilderContext m, boolean expr) {
        BeginNode beginNode = (BeginNode) node;

        build(beginNode.getBodyNode(), m, expr);
    }

    public void buildBignum(Node node, IR_BuilderContext m, boolean expr) {
        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) m.createNewBignum(((BignumNode) node).getValue());
        } else {
            m.createNewBignum(((BignumNode) node).getValue());
            if (!expr) m.consumeCurrentValue();
        }
    }

    public void buildBlock(Node node, IR_BuilderContext m, boolean expr) {
        BlockNode blockNode = (BlockNode) node;

        for (Iterator<Node> iter = blockNode.childNodes().iterator(); iter.hasNext();) {
            Node n = iter.next();
            build(n, m, iter.hasNext() ? false : expr);
        }
    }

    public void buildBreak(Node node, IR_BuilderContext m, boolean expr) {
        final BreakNode breakNode = (BreakNode) node;

        CompilerCallback valueCallback = new CompilerCallback() {

                    public void call(IR_BuilderContext m) {
                        if (breakNode.getValueNode() != null) {
                            build(breakNode.getValueNode(), m, true);
                        } else {
                            m.loadNil();
                        }
                    }
                };

        m.issueBreakEvent(valueCallback);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildCall(Node node, IR_BuilderContext m, boolean expr) {
        final CallNode callNode = (CallNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {
            public void call(IR_BuilderContext m) {
                build(callNode.getReceiverNode(), m, true);
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
                case '+': case '-': case '<':
                    if (argument instanceof FixnumNode) {
                        m.getInvocationCompiler().invokeBinaryFixnumRHS(name, receiverCallback, ((FixnumNode)argument).getValue());
                        if (!expr) m.consumeCurrentValue();
                        return;
                    }
                }
            }
        }

        // if __send__ with a literal symbol, build it as a direct fcall
        if (RubyInstanceConfig.FASTSEND_COMPILE_ENABLED) {
            String literalSend = getLiteralSend(callNode);
            if (literalSend != null) {
                name = literalSend;
                callType = CallType.FUNCTIONAL;
            }
        }
        
        m.getInvocationCompiler().invokeDynamic(
                name, receiverCallback, argsCallback,
                callType, closureArg, callNode.getIterNode() instanceof IterNode);
        
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
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

    public void buildCase(Node node, IR_BuilderContext m, boolean expr) {
        CaseNode caseNode = (CaseNode) node;

        boolean hasCase = caseNode.getCaseNode() != null;

        // aggregate when nodes into a list, unfortunately, this is no
        List<Node> cases = caseNode.getCases().childNodes();

        // last node, either !instanceof WhenNode or null, is the else
        Node elseNode = caseNode.getElseNode();

        buildWhen(caseNode.getCaseNode(), cases, elseNode, m, expr, hasCase);
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

    public void buildWhen(final Node value, List<Node> whenNodes, final Node elseNode, IR_BuilderContext m, final boolean expr, final boolean hasCase) {
        CompilerCallback caseValue = null;
        if (value != null) caseValue = new CompilerCallback() {
            public void call(IR_BuilderContext m) {
                build(value, m, true);
                m.pollThreadEvents();
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
                public void call(IR_BuilderContext m) {
                    build(whenNode.getBodyNode(), m, expr);
                }
            };
            addConditionalForWhen(whenNode, conditionals, bodies, body);
            if (switchCases != null) switchCases.put(body, getOptimizedCases(whenNode));
        }
        
        CompilerCallback fallback = new CompilerCallback() {
            public void call(IR_BuilderContext m) {
                build(elseNode, m, expr);
            }
        };
        
        m.buildSequencedConditional(caseValue, switchType, switchCases, conditionals, bodies, fallback);
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

                    public void call(IR_BuilderContext m) {
                        build(whenNode.getExpressionNodes(), m, true);
                    }
                });
                return;
            }
        }
        // otherwise, use normal args buildr
        conditionals.add(getArgsCallback(whenNode.getExpressionNodes()));
    }

    public void buildClass(Node node, IR_BuilderContext m, boolean expr) {
        final ClassNode classNode = (ClassNode) node;

        final Node superNode = classNode.getSuperNode();

        final Node cpathNode = classNode.getCPath();

        CompilerCallback superCallback = new CompilerCallback() {

                    public void call(IR_BuilderContext m) {
                        build(superNode, m, true);
                    }
                };
        if (superNode == null) {
            superCallback = null;
        }

        CompilerCallback bodyCallback = new CompilerCallback() {

                    public void call(IR_BuilderContext m) {
                        boolean oldIsAtRoot = isAtRoot;
                        isAtRoot = false;
                        if (classNode.getBodyNode() != null) {
                            build(classNode.getBodyNode(), m, true);
                        } else {
                            m.loadNil();
                        }
                        isAtRoot = oldIsAtRoot;
                    }
                };

        CompilerCallback pathCallback = new CompilerCallback() {

                    public void call(IR_BuilderContext m) {
                        if (cpathNode instanceof Colon2Node) {
                            Node leftNode = ((Colon2Node) cpathNode).getLeftNode();
                            if (leftNode != null) {
                                build(leftNode, m, true);
                            } else {
                                m.loadNil();
                            }
                        } else if (cpathNode instanceof Colon3Node) {
                            m.loadObject();
                        } else {
                            m.loadNil();
                        }
                    }
                };

        ASTInspector inspector = new ASTInspector();
        inspector.inspect(classNode.getBodyNode());

        m.defineClass(classNode.getCPath().getName(), classNode.getScope(), superCallback, pathCallback, bodyCallback, null, inspector);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildSClass(Node node, IR_BuilderContext m, boolean expr) {
        final SClassNode sclassNode = (SClassNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {

                    public void call(IR_BuilderContext m) {
                        build(sclassNode.getReceiverNode(), m, true);
                    }
                };

        CompilerCallback bodyCallback = new CompilerCallback() {

                    public void call(IR_BuilderContext m) {
                        boolean oldIsAtRoot = isAtRoot;
                        isAtRoot = false;
                        if (sclassNode.getBodyNode() != null) {
                            build(sclassNode.getBodyNode(), m, true);
                        } else {
                            m.loadNil();
                        }
                        isAtRoot = oldIsAtRoot;
                    }
                };

        ASTInspector inspector = new ASTInspector();
        inspector.inspect(sclassNode.getBodyNode());

        m.defineClass("SCLASS", sclassNode.getScope(), null, null, bodyCallback, receiverCallback, inspector);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildClassVar(Node node, IR_BuilderContext m, boolean expr) {
        ClassVarNode classVarNode = (ClassVarNode) node;

        m.retrieveClassVariable(classVarNode.getName());
        if (!expr) m.consumeCurrentValue();
    }

    public void buildClassVarAsgn(Node node, IR_BuilderContext m, boolean expr) {
        final ClassVarAsgnNode classVarAsgnNode = (ClassVarAsgnNode) node;

        CompilerCallback value = new CompilerCallback() {
            public void call(IR_BuilderContext m) {
                build(classVarAsgnNode.getValueNode(), m, true);
            }
        };

        m.assignClassVariable(classVarAsgnNode.getName(), value);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildClassVarAsgnAssignment(Node node, IR_BuilderContext m, boolean expr) {
        ClassVarAsgnNode classVarAsgnNode = (ClassVarAsgnNode) node;

        m.assignClassVariable(classVarAsgnNode.getName());
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildClassVarDecl(Node node, IR_BuilderContext m, boolean expr) {
        final ClassVarDeclNode classVarDeclNode = (ClassVarDeclNode) node;

        CompilerCallback value = new CompilerCallback() {
            public void call(IR_BuilderContext m) {
                build(classVarDeclNode.getValueNode(), m, true);
            }
        };
        
        m.declareClassVariable(classVarDeclNode.getName(), value);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildClassVarDeclAssignment(Node node, IR_BuilderContext m, boolean expr) {
        ClassVarDeclNode classVarDeclNode = (ClassVarDeclNode) node;

        m.declareClassVariable(classVarDeclNode.getName());
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildConstDecl(Node node, IR_BuilderContext m, boolean expr) {
        // TODO: callback for value would be more efficient, but unlikely to be a big cost (constants are rarely assigned)
        ConstDeclNode constDeclNode = (ConstDeclNode) node;
        Node constNode = constDeclNode.getConstNode();

        if (constNode == null) {
            build(constDeclNode.getValueNode(), m,true);

            m.assignConstantInCurrent(constDeclNode.getName());
        } else if (constNode.getNodeType() == NodeType.COLON2NODE) {
            build(((Colon2Node) constNode).getLeftNode(), m,true);
            build(constDeclNode.getValueNode(), m,true);

            m.assignConstantInModule(constDeclNode.getName());
        } else {// colon3, assign in Object
            build(constDeclNode.getValueNode(), m,true);

            m.assignConstantInObject(constDeclNode.getName());
        }
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildConstDeclAssignment(Node node, IR_BuilderContext m, boolean expr) {
        // TODO: callback for value would be more efficient, but unlikely to be a big cost (constants are rarely assigned)
        ConstDeclNode constDeclNode = (ConstDeclNode) node;
        Node constNode = constDeclNode.getConstNode();

        if (constNode == null) {
            m.assignConstantInCurrent(constDeclNode.getName());
        } else if (constNode.getNodeType() == NodeType.COLON2NODE) {
            build(((Colon2Node) constNode).getLeftNode(), m,true);
            m.swapValues();
            m.assignConstantInModule(constDeclNode.getName());
        } else {// colon3, assign in Object
            m.assignConstantInObject(constDeclNode.getName());
        }
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildConst(Node node, IR_BuilderContext m, boolean expr) {
        ConstNode constNode = (ConstNode) node;

        m.retrieveConstant(constNode.getName());
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
        // XXX: const lookup can trigger const_missing; is that enough to warrant it always being executed?
    }

    public void buildColon2(Node node, IR_BuilderContext m, boolean expr) {
        final Colon2Node iVisited = (Colon2Node) node;
        Node leftNode = iVisited.getLeftNode();
        final String name = iVisited.getName();

        if (leftNode == null) {
            m.loadObject();
            m.retrieveConstantFromModule(name);
        } else {
            if (node instanceof Colon2ConstNode) {
                build(iVisited.getLeftNode(), m, true);
                m.retrieveConstantFromModule(name);
            } else if (node instanceof Colon2MethodNode) {
                final CompilerCallback receiverCallback = new CompilerCallback() {
                    public void call(IR_BuilderContext m) {
                        build(iVisited.getLeftNode(), m,true);
                    }
                };
                
                m.getInvocationCompiler().invokeDynamic(name, receiverCallback, null, CallType.FUNCTIONAL, null, false);
            }
        }
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildColon3(Node node, IR_BuilderContext m, boolean expr) {
        Colon3Node iVisited = (Colon3Node) node;
        String name = iVisited.getName();

        m.retrieveConstantFromObject(name);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildGetDefinitionBase(final Node node, IR_BuilderContext m) {
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
            buildGetDefinition(node, m);
            break;
        default:
            BranchCallback reg = new BranchCallback() {

                        public void branch(IR_BuilderContext m) {
                            m.inDefined();
                            buildGetDefinition(node, m);
                        }
                    };
            BranchCallback out = new BranchCallback() {

                        public void branch(IR_BuilderContext m) {
                            m.outDefined();
                        }
                    };
            m.protect(reg, out, String.class);
        }
    }

    public void buildDefined(final Node node, IR_BuilderContext m, boolean expr) {
        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) {
                buildGetDefinitionBase(((DefinedNode) node).getExpressionNode(), m);
                m.stringOrNil();
            }
        } else {
            buildGetDefinitionBase(((DefinedNode) node).getExpressionNode(), m);
            m.stringOrNil();
            if (!expr) m.consumeCurrentValue();
        }
    }

    public void buildGetArgumentDefinition(final Node node, IR_BuilderContext m, String type) {
        if (node == null) {
            m.pushString(type);
        } else if (node instanceof ArrayNode) {
            Object endToken = m.getNewEnding();
            for (int i = 0; i < ((ArrayNode) node).size(); i++) {
                Node iterNode = ((ArrayNode) node).get(i);
                buildGetDefinition(iterNode, m);
                m.ifNull(endToken);
            }
            m.pushString(type);
            Object realToken = m.getNewEnding();
            m.go(realToken);
            m.setEnding(endToken);
            m.pushNull();
            m.setEnding(realToken);
        } else {
            buildGetDefinition(node, m);
            Object endToken = m.getNewEnding();
            m.ifNull(endToken);
            m.pushString(type);
            Object realToken = m.getNewEnding();
            m.go(realToken);
            m.setEnding(endToken);
            m.pushNull();
            m.setEnding(realToken);
        }
    }

    public void buildGetDefinition(final Node node, IR_BuilderContext m) {
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
                m.pushString("assignment");
                break;
            case BACKREFNODE:
                m.backref();
                m.isInstanceOf(RubyMatchData.class,
                        new BranchCallback() {

                            public void branch(IR_BuilderContext m) {
                                m.pushString("$" + ((BackRefNode) node).getType());
                            }
                        },
                        new BranchCallback() {

                            public void branch(IR_BuilderContext m) {
                                m.pushNull();
                            }
                        });
                break;
            case DVARNODE:
                m.pushString("local-variable(in-block)");
                break;
            case FALSENODE:
                m.pushString("false");
                break;
            case TRUENODE:
                m.pushString("true");
                break;
            case LOCALVARNODE:
                m.pushString("local-variable");
                break;
            case MATCH2NODE:
            case MATCH3NODE:
                m.pushString("method");
                break;
            case NILNODE:
                m.pushString("nil");
                break;
            case NTHREFNODE:
                m.isCaptured(((NthRefNode) node).getMatchNumber(),
                        new BranchCallback() {

                            public void branch(IR_BuilderContext m) {
                                m.pushString("$" + ((NthRefNode) node).getMatchNumber());
                            }
                        },
                        new BranchCallback() {

                            public void branch(IR_BuilderContext m) {
                                m.pushNull();
                            }
                        });
                break;
            case SELFNODE:
                m.pushString("self");
                break;
            case VCALLNODE:
                m.loadSelf();
                m.isMethodBound(((VCallNode) node).getName(),
                        new BranchCallback() {

                            public void branch(IR_BuilderContext m) {
                                m.pushString("method");
                            }
                        },
                        new BranchCallback() {

                            public void branch(IR_BuilderContext m) {
                                m.pushNull();
                            }
                        });
                break;
            case YIELDNODE:
                m.hasBlock(new BranchCallback() {

                            public void branch(IR_BuilderContext m) {
                                m.pushString("yield");
                            }
                        },
                        new BranchCallback() {

                            public void branch(IR_BuilderContext m) {
                                m.pushNull();
                            }
                        });
                break;
            case GLOBALVARNODE:
                m.isGlobalDefined(((GlobalVarNode) node).getName(),
                        new BranchCallback() {

                            public void branch(IR_BuilderContext m) {
                                m.pushString("global-variable");
                            }
                        },
                        new BranchCallback() {

                            public void branch(IR_BuilderContext m) {
                                m.pushNull();
                            }
                        });
                break;
            case INSTVARNODE:
                m.isInstanceVariableDefined(((InstVarNode) node).getName(),
                        new BranchCallback() {

                            public void branch(IR_BuilderContext m) {
                                m.pushString("instance-variable");
                            }
                        },
                        new BranchCallback() {

                            public void branch(IR_BuilderContext m) {
                                m.pushNull();
                            }
                        });
                break;
            case CONSTNODE:
                m.isConstantDefined(((ConstNode) node).getName(),
                        new BranchCallback() {

                            public void branch(IR_BuilderContext m) {
                                m.pushString("constant");
                            }
                        },
                        new BranchCallback() {

                            public void branch(IR_BuilderContext m) {
                                m.pushNull();
                            }
                        });
                break;
            case FCALLNODE:
                m.loadSelf();
                m.isMethodBound(((FCallNode) node).getName(),
                        new BranchCallback() {

                            public void branch(IR_BuilderContext m) {
                                buildGetArgumentDefinition(((FCallNode) node).getArgsNode(), m, "method");
                            }
                        },
                        new BranchCallback() {

                            public void branch(IR_BuilderContext m) {
                                m.pushNull();
                            }
                        });
                break;
            case COLON3NODE:
            case COLON2NODE:
                {
                    final Colon3Node iVisited = (Colon3Node) node;

                    final String name = iVisited.getName();

                    BranchCallback setup = new BranchCallback() {

                                public void branch(IR_BuilderContext m) {
                                    if (iVisited instanceof Colon2Node) {
                                        final Node leftNode = ((Colon2Node) iVisited).getLeftNode();
                                        build(leftNode, m,true);
                                    } else {
                                        m.loadObject();
                                    }
                                }
                            };
                    BranchCallback isConstant = new BranchCallback() {

                                public void branch(IR_BuilderContext m) {
                                    m.pushString("constant");
                                }
                            };
                    BranchCallback isMethod = new BranchCallback() {

                                public void branch(IR_BuilderContext m) {
                                    m.pushString("method");
                                }
                            };
                    BranchCallback none = new BranchCallback() {

                                public void branch(IR_BuilderContext m) {
                                    m.pushNull();
                                }
                            };
                    m.isConstantBranch(setup, isConstant, isMethod, none, name);
                    break;
                }
            case CALLNODE:
                {
                    final CallNode iVisited = (CallNode) node;
                    Object isnull = m.getNewEnding();
                    Object ending = m.getNewEnding();
                    buildGetDefinition(iVisited.getReceiverNode(), m);
                    m.ifNull(isnull);

                    m.rescue(new BranchCallback() {

                                public void branch(IR_BuilderContext m) {
                                    build(iVisited.getReceiverNode(), m,true); //[IRubyObject]
                                    m.duplicateCurrentValue(); //[IRubyObject, IRubyObject]
                                    m.metaclass(); //[IRubyObject, RubyClass]
                                    m.duplicateCurrentValue(); //[IRubyObject, RubyClass, RubyClass]
                                    m.getVisibilityFor(iVisited.getName()); //[IRubyObject, RubyClass, Visibility]
                                    m.duplicateCurrentValue(); //[IRubyObject, RubyClass, Visibility, Visibility]
                                    final Object isfalse = m.getNewEnding();
                                    Object isreal = m.getNewEnding();
                                    Object ending = m.getNewEnding();
                                    m.isPrivate(isfalse, 3); //[IRubyObject, RubyClass, Visibility]
                                    m.isNotProtected(isreal, 1); //[IRubyObject, RubyClass]
                                    m.selfIsKindOf(isreal); //[IRubyObject]
                                    m.consumeCurrentValue();
                                    m.go(isfalse);
                                    m.setEnding(isreal); //[]

                                    m.isMethodBound(iVisited.getName(), new BranchCallback() {

                                                public void branch(IR_BuilderContext m) {
                                                    buildGetArgumentDefinition(iVisited.getArgsNode(), m, "method");
                                                }
                                            },
                                            new BranchCallback() {

                                                public void branch(IR_BuilderContext m) {
                                                    m.go(isfalse);
                                                }
                                            });
                                    m.go(ending);
                                    m.setEnding(isfalse);
                                    m.pushNull();
                                    m.setEnding(ending);
                                }
                            }, JumpException.class,
                            new BranchCallback() {

                                public void branch(IR_BuilderContext m) {
                                    m.pushNull();
                                }
                            }, String.class);

                    //          m.swapValues();
            //m.consumeCurrentValue();
                    m.go(ending);
                    m.setEnding(isnull);
                    m.pushNull();
                    m.setEnding(ending);
                    break;
                }
            case CLASSVARNODE:
                {
                    ClassVarNode iVisited = (ClassVarNode) node;
                    final Object ending = m.getNewEnding();
                    final Object failure = m.getNewEnding();
                    final Object singleton = m.getNewEnding();
                    Object second = m.getNewEnding();
                    Object third = m.getNewEnding();

                    m.loadCurrentModule(); //[RubyClass]
                    m.duplicateCurrentValue(); //[RubyClass, RubyClass]
                    m.ifNotNull(second); //[RubyClass]
                    m.consumeCurrentValue(); //[]
                    m.loadSelf(); //[self]
                    m.metaclass(); //[RubyClass]
                    m.duplicateCurrentValue(); //[RubyClass, RubyClass]
                    m.isClassVarDefined(iVisited.getName(),
                            new BranchCallback() {

                                public void branch(IR_BuilderContext m) {
                                    m.consumeCurrentValue();
                                    m.pushString("class variable");
                                    m.go(ending);
                                }
                            },
                            new BranchCallback() {

                                public void branch(IR_BuilderContext m) {
                                }
                            });
                    m.setEnding(second);  //[RubyClass]
                    m.duplicateCurrentValue();
                    m.isClassVarDefined(iVisited.getName(),
                            new BranchCallback() {

                                public void branch(IR_BuilderContext m) {
                                    m.consumeCurrentValue();
                                    m.pushString("class variable");
                                    m.go(ending);
                                }
                            },
                            new BranchCallback() {

                                public void branch(IR_BuilderContext m) {
                                }
                            });
                    m.setEnding(third); //[RubyClass]
                    m.duplicateCurrentValue(); //[RubyClass, RubyClass]
                    m.ifSingleton(singleton); //[RubyClass]
                    m.consumeCurrentValue();//[]
                    m.go(failure);
                    m.setEnding(singleton);
                    m.attached();//[RubyClass]
                    m.notIsModuleAndClassVarDefined(iVisited.getName(), failure); //[]
                    m.pushString("class variable");
                    m.go(ending);
                    m.setEnding(failure);
                    m.pushNull();
                    m.setEnding(ending);
                }
                break;
            case ZSUPERNODE:
                {
                    Object fail = m.getNewEnding();
                    Object fail2 = m.getNewEnding();
                    Object fail_easy = m.getNewEnding();
                    Object ending = m.getNewEnding();

                    m.getFrameName(); //[String]
                    m.duplicateCurrentValue(); //[String, String]
                    m.ifNull(fail); //[String]
                    m.getFrameKlazz(); //[String, RubyClass]
                    m.duplicateCurrentValue(); //[String, RubyClass, RubyClass]
                    m.ifNull(fail2); //[String, RubyClass]
                    m.superClass();
                    m.ifNotSuperMethodBound(fail_easy);

                    m.pushString("super");
                    m.go(ending);

                    m.setEnding(fail2);
                    m.consumeCurrentValue();
                    m.setEnding(fail);
                    m.consumeCurrentValue();
                    m.setEnding(fail_easy);
                    m.pushNull();
                    m.setEnding(ending);
                }
                break;
            case SUPERNODE:
                {
                    Object fail = m.getNewEnding();
                    Object fail2 = m.getNewEnding();
                    Object fail_easy = m.getNewEnding();
                    Object ending = m.getNewEnding();

                    m.getFrameName(); //[String]
                    m.duplicateCurrentValue(); //[String, String]
                    m.ifNull(fail); //[String]
                    m.getFrameKlazz(); //[String, RubyClass]
                    m.duplicateCurrentValue(); //[String, RubyClass, RubyClass]
                    m.ifNull(fail2); //[String, RubyClass]
                    m.superClass();
                    m.ifNotSuperMethodBound(fail_easy);

                    buildGetArgumentDefinition(((SuperNode) node).getArgsNode(), m, "super");
                    m.go(ending);

                    m.setEnding(fail2);
                    m.consumeCurrentValue();
                    m.setEnding(fail);
                    m.consumeCurrentValue();
                    m.setEnding(fail_easy);
                    m.pushNull();
                    m.setEnding(ending);
                    break;
                }
            case ATTRASSIGNNODE:
                {
                    final AttrAssignNode iVisited = (AttrAssignNode) node;
                    Object isnull = m.getNewEnding();
                    Object ending = m.getNewEnding();
                    buildGetDefinition(iVisited.getReceiverNode(), m);
                    m.ifNull(isnull);

                    m.rescue(new BranchCallback() {

                                public void branch(IR_BuilderContext m) {
                                    build(iVisited.getReceiverNode(), m,true); //[IRubyObject]
                                    m.duplicateCurrentValue(); //[IRubyObject, IRubyObject]
                                    m.metaclass(); //[IRubyObject, RubyClass]
                                    m.duplicateCurrentValue(); //[IRubyObject, RubyClass, RubyClass]
                                    m.getVisibilityFor(iVisited.getName()); //[IRubyObject, RubyClass, Visibility]
                                    m.duplicateCurrentValue(); //[IRubyObject, RubyClass, Visibility, Visibility]
                                    final Object isfalse = m.getNewEnding();
                                    Object isreal = m.getNewEnding();
                                    Object ending = m.getNewEnding();
                                    m.isPrivate(isfalse, 3); //[IRubyObject, RubyClass, Visibility]
                                    m.isNotProtected(isreal, 1); //[IRubyObject, RubyClass]
                                    m.selfIsKindOf(isreal); //[IRubyObject]
                                    m.consumeCurrentValue();
                                    m.go(isfalse);
                                    m.setEnding(isreal); //[]

                                    m.isMethodBound(iVisited.getName(), new BranchCallback() {

                                                public void branch(IR_BuilderContext m) {
                                                    buildGetArgumentDefinition(iVisited.getArgsNode(), m, "assignment");
                                                }
                                            },
                                            new BranchCallback() {

                                                public void branch(IR_BuilderContext m) {
                                                    m.go(isfalse);
                                                }
                                            });
                                    m.go(ending);
                                    m.setEnding(isfalse);
                                    m.pushNull();
                                    m.setEnding(ending);
                                }
                            }, JumpException.class,
                            new BranchCallback() {

                                public void branch(IR_BuilderContext m) {
                                    m.pushNull();
                                }
                            }, String.class);

                    m.go(ending);
                    m.setEnding(isnull);
                    m.pushNull();
                    m.setEnding(ending);
                    break;
                }
            default:
                m.rescue(new BranchCallback() {

                            public void branch(IR_BuilderContext m) {
                                build(node, m,true);
                                m.consumeCurrentValue();
                                m.pushNull();
                            }
                        }, JumpException.class,
                        new BranchCallback() {

                            public void branch(IR_BuilderContext m) {
                                m.pushNull();
                            }
                        }, String.class);
                m.consumeCurrentValue();
                m.pushString("expression");
        }
    }

    public void buildDAsgn(Node node, IR_BuilderContext m, boolean expr) {
        final DAsgnNode dasgnNode = (DAsgnNode) node;

        CompilerCallback value = new CompilerCallback() {
            public void call(IR_BuilderContext m) {
                build(dasgnNode.getValueNode(), m, true);
            }
        };
        
        m.getVariableCompiler().assignLocalVariable(dasgnNode.getIndex(), dasgnNode.getDepth(), value, expr);
    }

    public void buildDAsgnAssignment(Node node, IR_BuilderContext m, boolean expr) {
        DAsgnNode dasgnNode = (DAsgnNode) node;

        m.getVariableCompiler().assignLocalVariable(dasgnNode.getIndex(), dasgnNode.getDepth(), expr);
    }

    public void buildDefn(Node node, IR_BuilderContext context, boolean expr) {
        final DefnNode defnNode = (DefnNode) node;
        final ArgsNode argsNode = defnNode.getArgsNode();

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

        IR_Method m = defineNewMethod(defnNode.getName(), defnNode.getArgsNode().getArity().getValue(), defnNode.getScope(), inspector, isAtRoot);

              // Build IR for args
        buildArgs(argsNode, m, true);

              // Build IR for body
        if (defnNode.getBodyNode() != null) {
            if (defnNode.getBodyNode() instanceof RescueNode) {
                // if root of method is rescue, build as a light rescue
                buildRescueInternal(defnNode.getBodyNode(), m, true);
            } else {
                build(defnNode.getBodyNode(), m, true);
            }
        } else {
           m.loadNil();
        }

        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();

                  // Add the new method to the current context
          context.addMethod(m);
    }

    public void buildDefs(Node node, IR_BuilderContext m, boolean expr) {
        final DefsNode defsNode = (DefsNode) node;
        final ArgsNode argsNode = defsNode.getArgsNode();

        CompilerCallback receiver = new CompilerCallback() {

                    public void call(IR_BuilderContext m) {
                        build(defsNode.getReceiverNode(), m, true);
                    }
                };

        CompilerCallback body = new CompilerCallback() {

                    public void call(IR_BuilderContext m) {
                        if (defsNode.getBodyNode() != null) {
                            if (defsNode.getBodyNode() instanceof RescueNode) {
                                // if root of method is rescue, build as light rescue
                                buildRescueInternal(defsNode.getBodyNode(), m, true);
                            } else {
                                build(defsNode.getBodyNode(), m, true);
                            }
                        } else {
                            m.loadNil();
                        }
                    }
                };

        CompilerCallback args = new CompilerCallback() {

                    public void call(IR_BuilderContext m) {
                        buildArgs(argsNode, m, true);
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

        m.defineNewMethod(defsNode.getName(), defsNode.getArgsNode().getArity().getValue(), defsNode.getScope(), body, args, receiver, inspector, false);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildArgs(Node node, IR_BuilderContext context, boolean expr) {
        IR_Method m = (IR_Method)context;

        final ArgsNode argsNode = (ArgsNode) node;
        final int required = argsNode.getRequiredArgsCount();
        final int opt = argsNode.getOptionalArgsCount();
        final int rest = argsNode.getRestArg();

          // TODO: Add IR instructions for checking method arity!
        // m.getVariableCompiler().checkMethodArity(required, opt, rest);

            // Both for fixed arity and variable arity methods
        for (int i = 0; i < m.numRequiredArgs(); i++)
            m.addInstr(new RECV_ARG_Instr(m.getNewVariable("arg"), new Constant(i)));

        if (opt > 0 || rest > -1) {
            Node optArgs = argsNode.getOptArgs();
            for (j = 0; j < opt; j++, i++) {
					 	// Jump to 'l' if this arg is not null.  If null, fall through and build the default value!
                Label l = m.getNewLabel();
                m.addInstr(new RECV_OPT_ARG_Instr(m.getNewVariable("arg"), new Constant(i), l));
                build(((ListNode)object).get(j), m, true);
                m.addInstr(new LABEL_Instr(l));
            }

            if (rest > -1) {
                m.addInstr(new RECV_ARG_Instr(m.getNewVariable("arg"), new Constant(i)));
            }
        }

        if (argsNode.getBlock() != null)
            m.addInstr(new RECV_ARG_Instr(m.getNewVariable("arg"), new Constant(argsNode.getBlock().getCount())));

        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildDot(Node node, IR_BuilderContext m, boolean expr) {
        final DotNode dotNode = (DotNode) node;

        boolean doit = expr || !RubyInstanceConfig.PEEPHOLE_OPTZ;
        boolean popit = !RubyInstanceConfig.PEEPHOLE_OPTZ && !expr;

        if (doit) {
            CompilerCallback beginEndCallback = new CompilerCallback() {
                public void call(IR_BuilderContext m) {
                    build(dotNode.getBeginNode(), m, true);
                    build(dotNode.getEndNode(), m, true);
                }
            };

            m.createNewRange(beginEndCallback, dotNode.isExclusive());
        }
        if (popit) m.consumeCurrentValue();
    }

    public void buildDRegexp(Node node, IR_BuilderContext m, boolean expr) {
        final DRegexpNode dregexpNode = (DRegexpNode) node;

        CompilerCallback createStringCallback = new CompilerCallback() {

                    public void call(IR_BuilderContext m) {
                        ArrayCallback dstrCallback = new ArrayCallback() {

                                    public void nextValue(IR_BuilderContext m, Object sourceArray,
                                            int index) {
                                        build(dregexpNode.get(index), m, true);
                                    }
                                };
                        m.createNewString(dstrCallback, dregexpNode.size());
                    }
                };

        boolean doit = expr || !RubyInstanceConfig.PEEPHOLE_OPTZ;
        boolean popit = !RubyInstanceConfig.PEEPHOLE_OPTZ && !expr;

        if (doit) {
            m.createNewRegexp(createStringCallback, dregexpNode.getOptions());
            if (popit) m.consumeCurrentValue();
        } else {
            // not an expression, only build the elements
            for (Node nextNode : dregexpNode.childNodes()) {
                build(nextNode, m, false);
            }
        }
    }

    public void buildDStr(Node node, IR_BuilderContext m, boolean expr) {
        final DStrNode dstrNode = (DStrNode) node;

        ArrayCallback dstrCallback = new ArrayCallback() {

                    public void nextValue(IR_BuilderContext m, Object sourceArray,
                            int index) {
                        build(dstrNode.get(index), m, true);
                    }
                };

        boolean doit = expr || !RubyInstanceConfig.PEEPHOLE_OPTZ;
        boolean popit = !RubyInstanceConfig.PEEPHOLE_OPTZ && !expr;

        if (doit) {
            m.createNewString(dstrCallback, dstrNode.size());
            if (popit) m.consumeCurrentValue();
        } else {
            // not an expression, only build the elements
            for (Node nextNode : dstrNode.childNodes()) {
                build(nextNode, m, false);
            }
        }
    }

    public void buildDSymbol(Node node, IR_BuilderContext m, boolean expr) {
        final DSymbolNode dsymbolNode = (DSymbolNode) node;

        ArrayCallback dstrCallback = new ArrayCallback() {

                    public void nextValue(IR_BuilderContext m, Object sourceArray,
                            int index) {
                        build(dsymbolNode.get(index), m, true);
                    }
                };

        boolean doit = expr || !RubyInstanceConfig.PEEPHOLE_OPTZ;
        boolean popit = !RubyInstanceConfig.PEEPHOLE_OPTZ && !expr;

        if (doit) {
            m.createNewSymbol(dstrCallback, dsymbolNode.size());
            if (popit) m.consumeCurrentValue();
        } else {
            // not an expression, only build the elements
            for (Node nextNode : dsymbolNode.childNodes()) {
                build(nextNode, m, false);
            }
        }
    }

    public void buildDVar(Node node, IR_BuilderContext m, boolean expr) {
        DVarNode dvarNode = (DVarNode) node;

        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) m.getVariableCompiler().retrieveLocalVariable(dvarNode.getIndex(), dvarNode.getDepth());
        } else {
            m.getVariableCompiler().retrieveLocalVariable(dvarNode.getIndex(), dvarNode.getDepth());
            if (!expr) m.consumeCurrentValue();
        }
    }

    public void buildDXStr(Node node, IR_BuilderContext m, boolean expr) {
        final DXStrNode dxstrNode = (DXStrNode) node;

        final ArrayCallback dstrCallback = new ArrayCallback() {

                    public void nextValue(IR_BuilderContext m, Object sourceArray,
                            int index) {
                        build(dxstrNode.get(index), m,true);
                    }
                };

        ArgumentsCallback argsCallback = new ArgumentsCallback() {
                    public int getArity() {
                        return 1;
                    }
                    
                    public void call(IR_BuilderContext m) {
                        m.createNewString(dstrCallback, dxstrNode.size());
                    }
                };

        m.getInvocationCompiler().invokeDynamic("`", null, argsCallback, CallType.FUNCTIONAL, null, false);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildEnsureNode(Node node, IR_BuilderContext m, boolean expr) {
        final EnsureNode ensureNode = (EnsureNode) node;

        if (ensureNode.getEnsureNode() != null) {
            m.performEnsure(new BranchCallback() {

                        public void branch(IR_BuilderContext m) {
                            if (ensureNode.getBodyNode() != null) {
                                build(ensureNode.getBodyNode(), m, true);
                            } else {
                                m.loadNil();
                            }
                        }
                    },
                    new BranchCallback() {

                        public void branch(IR_BuilderContext m) {
                            build(ensureNode.getEnsureNode(), m, false);
                        }
                    });
        } else {
            if (ensureNode.getBodyNode() != null) {
                build(ensureNode.getBodyNode(), m,true);
            } else {
                m.loadNil();
            }
        }
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildEvStr(Node node, IR_BuilderContext m, boolean expr) {
        final EvStrNode evStrNode = (EvStrNode) node;

        build(evStrNode.getBody(), m,true);
        m.asString();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildFalse(Node node, IR_BuilderContext m, boolean expr) {
        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) {
                m.loadFalse();
                m.pollThreadEvents();
            }
        } else {
            m.loadFalse();
            m.pollThreadEvents();
            if (!expr) m.consumeCurrentValue();
        }
    }

    public void buildFCall(Node node, IR_BuilderContext m, boolean expr) {
        final FCallNode fcallNode = (FCallNode) node;

        ArgumentsCallback argsCallback = getArgsCallback(fcallNode.getArgsNode());
        
        CompilerCallback closureArg = getBlock(fcallNode.getIterNode());

        m.getInvocationCompiler().invokeDynamic(fcallNode.getName(), null, argsCallback, CallType.FUNCTIONAL, closureArg, fcallNode.getIterNode() instanceof IterNode);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    private CompilerCallback getBlock(Node node) {
        if (node == null) {
            return null;
        }

        switch (node.getNodeType()) {
            case ITERNODE:
                final IterNode iterNode = (IterNode) node;

                return new CompilerCallback() {

                            public void call(IR_BuilderContext m) {
                                build(iterNode, m,true);
                            }
                        };
            case BLOCKPASSNODE:
                final BlockPassNode blockPassNode = (BlockPassNode) node;

                return new CompilerCallback() {

                            public void call(IR_BuilderContext m) {
                                build(blockPassNode.getBodyNode(), m,true);
                                m.unwrapPassedBlock();
                            }
                        };
            default:
                throw new NotCompilableException("ERROR: Encountered a method with a non-block, non-blockpass iter node at: " + node);
        }
    }

    public void buildFixnum(Node node, IR_BuilderContext m, boolean expr) {
        FixnumNode fixnumNode = (FixnumNode) node;

        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) m.createNewFixnum(fixnumNode.getValue());
        } else {
            m.createNewFixnum(fixnumNode.getValue());
            if (!expr) m.consumeCurrentValue();
        }
    }

    public void buildFlip(Node node, IR_BuilderContext m, boolean expr) {
        final FlipNode flipNode = (FlipNode) node;

        m.getVariableCompiler().retrieveLocalVariable(flipNode.getIndex(), flipNode.getDepth());

        if (flipNode.isExclusive()) {
            m.performBooleanBranch(new BranchCallback() {

                public void branch(IR_BuilderContext m) {
                    build(flipNode.getEndNode(), m,true);
                    m.performBooleanBranch(new BranchCallback() {

                        public void branch(IR_BuilderContext m) {
                            m.loadFalse();
                            m.getVariableCompiler().assignLocalVariable(flipNode.getIndex(), flipNode.getDepth(), false);
                        }
                    }, new BranchCallback() {

                        public void branch(IR_BuilderContext m) {
                        }
                    });
                    m.loadTrue();
                }
            }, new BranchCallback() {

                public void branch(IR_BuilderContext m) {
                    build(flipNode.getBeginNode(), m,true);
                    becomeTrueOrFalse(m);
                    m.getVariableCompiler().assignLocalVariable(flipNode.getIndex(), flipNode.getDepth(), true);
                }
            });
        } else {
            m.performBooleanBranch(new BranchCallback() {

                public void branch(IR_BuilderContext m) {
                    build(flipNode.getEndNode(), m,true);
                    m.performBooleanBranch(new BranchCallback() {

                        public void branch(IR_BuilderContext m) {
                            m.loadFalse();
                            m.getVariableCompiler().assignLocalVariable(flipNode.getIndex(), flipNode.getDepth(), false);
                        }
                    }, new BranchCallback() {

                        public void branch(IR_BuilderContext m) {
                        }
                    });
                    m.loadTrue();
                }
            }, new BranchCallback() {

                public void branch(IR_BuilderContext m) {
                    build(flipNode.getBeginNode(), m,true);
                    m.performBooleanBranch(new BranchCallback() {

                        public void branch(IR_BuilderContext m) {
                            build(flipNode.getEndNode(), m,true);
                            flipTrueOrFalse(m);
                            m.getVariableCompiler().assignLocalVariable(flipNode.getIndex(), flipNode.getDepth(), false);
                            m.loadTrue();
                        }
                    }, new BranchCallback() {

                        public void branch(IR_BuilderContext m) {
                            m.loadFalse();
                        }
                    });
                }
            });
        }
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    private void becomeTrueOrFalse(IR_BuilderContext m) {
        m.performBooleanBranch(new BranchCallback() {

                    public void branch(IR_BuilderContext m) {
                        m.loadTrue();
                    }
                }, new BranchCallback() {

                    public void branch(IR_BuilderContext m) {
                        m.loadFalse();
                    }
                });
    }

    private void flipTrueOrFalse(IR_BuilderContext m) {
        m.performBooleanBranch(new BranchCallback() {

                    public void branch(IR_BuilderContext m) {
                        m.loadFalse();
                    }
                }, new BranchCallback() {

                    public void branch(IR_BuilderContext m) {
                        m.loadTrue();
                    }
                });
    }

    public void buildFloat(Node node, IR_BuilderContext m, boolean expr) {
        FloatNode floatNode = (FloatNode) node;

        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) m.createNewFloat(floatNode.getValue());
        } else {
            m.createNewFloat(floatNode.getValue());
            if (!expr) m.consumeCurrentValue();
        }
    }

    public void buildFor(Node node, IR_BuilderContext m, boolean expr) {
        final ForNode forNode = (ForNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {

                    public void call(IR_BuilderContext m) {
                        build(forNode.getIterNode(), m, true);
                    }
                };

        final CompilerCallback closureArg = new CompilerCallback() {

                    public void call(IR_BuilderContext m) {
                        buildForIter(forNode, m);
                    }
                };

        m.getInvocationCompiler().invokeDynamic("each", receiverCallback, null, CallType.NORMAL, closureArg, true);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildForIter(Node node, IR_BuilderContext m) {
        final ForNode forNode = (ForNode) node;

        // create the closure class and instantiate it
        final CompilerCallback closureBody = new CompilerCallback() {

                    public void call(IR_BuilderContext m) {
                        if (forNode.getBodyNode() != null) {
                            build(forNode.getBodyNode(), m,true);
                        } else {
                            m.loadNil();
                        }
                    }
                };

        // create the closure class and instantiate it
        final CompilerCallback closureArgs = new CompilerCallback() {

                    public void call(IR_BuilderContext m) {
                        if (forNode.getVarNode() != null) {
                            buildAssignment(forNode.getVarNode(), m, false);
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
            m.createNewForLoop(Arity.procArityOf(forNode.getVarNode()).getValue(),
                    closureBody, null, hasMultipleArgsHead, argsNodeId, inspector);
        } else {
            m.createNewForLoop(Arity.procArityOf(forNode.getVarNode()).getValue(),
                    closureBody, closureArgs, hasMultipleArgsHead, argsNodeId, inspector);
        }
    }

    public void buildGlobalAsgn(Node node, IR_BuilderContext m, boolean expr) {
        final GlobalAsgnNode globalAsgnNode = (GlobalAsgnNode) node;

        CompilerCallback value = new CompilerCallback() {
            public void call(IR_BuilderContext m) {
                build(globalAsgnNode.getValueNode(), m, true);
            }
        };

        if (globalAsgnNode.getName().length() == 2) {
            switch (globalAsgnNode.getName().charAt(1)) {
            case '_':
                m.getVariableCompiler().assignLastLine(value);
                break;
            case '~':
                m.getVariableCompiler().assignBackRef(value);
                break;
            default:
                m.assignGlobalVariable(globalAsgnNode.getName(), value);
            }
        } else {
            m.assignGlobalVariable(globalAsgnNode.getName(), value);
        }

        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildGlobalAsgnAssignment(Node node, IR_BuilderContext m, boolean expr) {
        GlobalAsgnNode globalAsgnNode = (GlobalAsgnNode) node;

        if (globalAsgnNode.getName().length() == 2) {
            switch (globalAsgnNode.getName().charAt(1)) {
            case '_':
                m.getVariableCompiler().assignLastLine();
                break;
            case '~':
                m.getVariableCompiler().assignBackRef();
                break;
            default:
                m.assignGlobalVariable(globalAsgnNode.getName());
            }
        } else {
            m.assignGlobalVariable(globalAsgnNode.getName());
        }
        
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildGlobalVar(Node node, IR_BuilderContext m, boolean expr) {
        GlobalVarNode globalVarNode = (GlobalVarNode) node;

        boolean doit = expr || !RubyInstanceConfig.PEEPHOLE_OPTZ;
        boolean popit = !RubyInstanceConfig.PEEPHOLE_OPTZ && !expr;
        
        if (doit) {
            if (globalVarNode.getName().length() == 2) {
                switch (globalVarNode.getName().charAt(1)) {
                case '_':
                    m.getVariableCompiler().retrieveLastLine();
                    break;
                case '~':
                    m.getVariableCompiler().retrieveBackRef();
                    break;
                default:
                    m.retrieveGlobalVariable(globalVarNode.getName());
                }
            } else {
                m.retrieveGlobalVariable(globalVarNode.getName());
            }
        }
        
        if (popit) m.consumeCurrentValue();
    }

    public void buildHash(Node node, IR_BuilderContext m, boolean expr) {
        HashNode hashNode = (HashNode) node;

        boolean doit = expr || !RubyInstanceConfig.PEEPHOLE_OPTZ;
        boolean popit = !RubyInstanceConfig.PEEPHOLE_OPTZ && !expr;

        if (doit) {
            if (hashNode.getListNode() == null || hashNode.getListNode().size() == 0) {
                m.createEmptyHash();
                return;
            }

            ArrayCallback hashCallback = new ArrayCallback() {

                        public void nextValue(IR_BuilderContext m, Object sourceArray,
                                int index) {
                            ListNode listNode = (ListNode) sourceArray;
                            int keyIndex = index * 2;
                            build(listNode.get(keyIndex), m, true);
                            build(listNode.get(keyIndex + 1), m, true);
                        }
                    };

            m.createNewHash(hashNode.getListNode(), hashCallback, hashNode.getListNode().size() / 2);
            if (popit) m.consumeCurrentValue();
        } else {
            for (Node nextNode : hashNode.getListNode().childNodes()) {
                build(nextNode, m, false);
            }
        }
    }

    public void buildIf(Node node, IR_BuilderContext m, final boolean expr) {
        final IfNode ifNode = (IfNode) node;

        // optimizations if we know ahead of time it will always be true or false
        Node actualCondition = ifNode.getCondition();
        while (actualCondition instanceof NewlineNode) {
            actualCondition = ((NewlineNode)actualCondition).getNextNode();
        }

        if (actualCondition.getNodeType().alwaysTrue()) {
            // build condition as non-expr and just build "then" body
            build(actualCondition, m, false);
            build(ifNode.getThenBody(), m, expr);
        } else if (actualCondition.getNodeType().alwaysFalse()) {
            // always false or nil
            build(ifNode.getElseBody(), m, expr);
        } else {
            BranchCallback trueCallback = new BranchCallback() {
                public void branch(IR_BuilderContext m) {
                    if (ifNode.getThenBody() != null) {
                        build(ifNode.getThenBody(), m, expr);
                    } else {
                        if (expr) m.loadNil();
                    }
                }
            };

            BranchCallback falseCallback = new BranchCallback() {
                public void branch(IR_BuilderContext m) {
                    if (ifNode.getElseBody() != null) {
                        build(ifNode.getElseBody(), m, expr);
                    } else {
                        if (expr) m.loadNil();
                    }
                }
            };
            
            // normal
            build(actualCondition, m, true);
            m.performBooleanBranch(trueCallback, falseCallback);
        }
    }

    public void buildInstAsgn(Node node, IR_BuilderContext m, boolean expr) {
        final InstAsgnNode instAsgnNode = (InstAsgnNode) node;

        CompilerCallback value = new CompilerCallback() {
            public void call(IR_BuilderContext m) {
                build(instAsgnNode.getValueNode(), m, true);
            }
        };

        m.assignInstanceVariable(instAsgnNode.getName(), value);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildInstAsgnAssignment(Node node, IR_BuilderContext m, boolean expr) {
        InstAsgnNode instAsgnNode = (InstAsgnNode) node;
        m.assignInstanceVariable(instAsgnNode.getName());
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildInstVar(Node node, IR_BuilderContext m, boolean expr) {
        InstVarNode instVarNode = (InstVarNode) node;

        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) m.retrieveInstanceVariable(instVarNode.getName());
        } else {
            m.retrieveInstanceVariable(instVarNode.getName());
            if (!expr) m.consumeCurrentValue();
        }
    }

    public void buildIter(Node node, IR_BuilderContext m) {
        final IterNode iterNode = (IterNode) node;

        // create the closure class and instantiate it
        final CompilerCallback closureBody = new CompilerCallback() {

                    public void call(IR_BuilderContext m) {
                        if (iterNode.getBodyNode() != null) {
                            build(iterNode.getBodyNode(), m, true);
                        } else {
                            m.loadNil();
                        }
                    }
                };

        // create the closure class and instantiate it
        final CompilerCallback closureArgs = new CompilerCallback() {

                    public void call(IR_BuilderContext m) {
                        if (iterNode.getVarNode() != null) {
                            buildAssignment(iterNode.getVarNode(), m, false);
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
            m.createNewClosure(iterNode.getPosition().getStartLine(), iterNode.getScope(), Arity.procArityOf(iterNode.getVarNode()).getValue(),
                    closureBody, null, hasMultipleArgsHead, argsNodeId, inspector);
        } else {
            m.createNewClosure(iterNode.getPosition().getStartLine(), iterNode.getScope(), Arity.procArityOf(iterNode.getVarNode()).getValue(),
                    closureBody, closureArgs, hasMultipleArgsHead, argsNodeId, inspector);
        }
    }

    public void buildLocalAsgn(Node node, IR_BuilderContext m, boolean expr) {
        final LocalAsgnNode localAsgnNode = (LocalAsgnNode) node;

        // just push nil for pragmas
        if (ASTInspector.PRAGMAS.contains(localAsgnNode.getName())) {
            if (expr) m.loadNil();
        } else {
            CompilerCallback value = new CompilerCallback() {
                public void call(IR_BuilderContext m) {
                    build(localAsgnNode.getValueNode(), m,true);
                }
            };

            m.getVariableCompiler().assignLocalVariable(localAsgnNode.getIndex(), localAsgnNode.getDepth(), value, expr);
        }
    }

    public void buildLocalAsgnAssignment(Node node, IR_BuilderContext m, boolean expr) {
        // "assignment" means the value is already on the stack
        LocalAsgnNode localAsgnNode = (LocalAsgnNode) node;

        m.getVariableCompiler().assignLocalVariable(localAsgnNode.getIndex(), localAsgnNode.getDepth(), expr);
    }

    public void buildLocalVar(Node node, IR_BuilderContext m, boolean expr) {
        LocalVarNode localVarNode = (LocalVarNode) node;

        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) m.getVariableCompiler().retrieveLocalVariable(localVarNode.getIndex(), localVarNode.getDepth());
        } else {
            m.getVariableCompiler().retrieveLocalVariable(localVarNode.getIndex(), localVarNode.getDepth());
            if (!expr) m.consumeCurrentValue();
        }
    }

    public void buildMatch(Node node, IR_BuilderContext m, boolean expr) {
        MatchNode matchNode = (MatchNode) node;

        build(matchNode.getRegexpNode(), m,true);

        m.match();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildMatch2(Node node, IR_BuilderContext m, boolean expr) {
        final Match2Node matchNode = (Match2Node) node;

        build(matchNode.getReceiverNode(), m,true);
        CompilerCallback value = new CompilerCallback() {
            public void call(IR_BuilderContext m) {
                build(matchNode.getValueNode(), m,true);
            }
        };

        m.match2(value);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildMatch3(Node node, IR_BuilderContext m, boolean expr) {
        Match3Node matchNode = (Match3Node) node;

        build(matchNode.getReceiverNode(), m,true);
        build(matchNode.getValueNode(), m,true);

        m.match3();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildModule(Node node, IR_BuilderContext m, boolean expr) {
        final ModuleNode moduleNode = (ModuleNode) node;

        final Node cpathNode = moduleNode.getCPath();

        CompilerCallback bodyCallback = new CompilerCallback() {

                    public void call(IR_BuilderContext m) {
                        if (moduleNode.getBodyNode() != null) {
                            build(moduleNode.getBodyNode(), m,true);
                        }
                        m.loadNil();
                    }
                };

        CompilerCallback pathCallback = new CompilerCallback() {

                    public void call(IR_BuilderContext m) {
                        if (cpathNode instanceof Colon2Node) {
                            Node leftNode = ((Colon2Node) cpathNode).getLeftNode();
                            if (leftNode != null) {
                                build(leftNode, m,true);
                            } else {
                                m.loadNil();
                            }
                        } else if (cpathNode instanceof Colon3Node) {
                            m.loadObject();
                        } else {
                            m.loadNil();
                        }
                    }
                };

        ASTInspector inspector = new ASTInspector();
        inspector.inspect(moduleNode.getBodyNode());

        m.defineModule(moduleNode.getCPath().getName(), moduleNode.getScope(), pathCallback, bodyCallback, inspector);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildMultipleAsgn(Node node, IR_BuilderContext m, boolean expr) {
        MultipleAsgnNode multipleAsgnNode = (MultipleAsgnNode) node;

        if (expr) {
            // need the array, use unoptz version
            buildUnoptimizedMultipleAsgn(multipleAsgnNode, m, expr);
        } else {
            // try optz version
            buildOptimizedMultipleAsgn(multipleAsgnNode, m, expr);
        }
    }

    private void buildOptimizedMultipleAsgn(MultipleAsgnNode multipleAsgnNode, IR_BuilderContext m, boolean expr) {
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
                                build(value, m, true);
                            }
                            m.reverseValues(size);
                            for (Node asgn : multipleAsgnNode.getHeadNode().childNodes()) {
                                buildAssignment(asgn, m, false);
                            }
                            return;
                        }
                    }
                }
            }
        }

        // if we get here, no optz cases work; fall back on unoptz.
        buildUnoptimizedMultipleAsgn(multipleAsgnNode, m, expr);
    }

    private void buildUnoptimizedMultipleAsgn(MultipleAsgnNode multipleAsgnNode, IR_BuilderContext m, boolean expr) {
        build(multipleAsgnNode.getValueNode(), m, true);

        buildMultipleAsgnAssignment(multipleAsgnNode, m, expr);
    }

    public void buildMultipleAsgnAssignment(Node node, IR_BuilderContext m, boolean expr) {
        final MultipleAsgnNode multipleAsgnNode = (MultipleAsgnNode) node;

        // normal items at the "head" of the masgn
        ArrayCallback headAssignCallback = new ArrayCallback() {

                    public void nextValue(IR_BuilderContext m, Object sourceArray,
                            int index) {
                        ListNode headNode = (ListNode) sourceArray;
                        Node assignNode = headNode.get(index);

                        // perform assignment for the next node
                        buildAssignment(assignNode, m, false);
                    }
                };

        CompilerCallback argsCallback = new CompilerCallback() {

                    public void call(IR_BuilderContext m) {
                        Node argsNode = multipleAsgnNode.getArgsNode();
                        if (argsNode instanceof StarNode) {
                            // done processing args
                            m.consumeCurrentValue();
                        } else {
                            // assign to appropriate variable
                            buildAssignment(argsNode, m, false);
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
                    m.ensureMultipleAssignableRubyArray(multipleAsgnNode.getHeadNode() != null);

                    m.forEachInValueArray(0, 0, null, null, argsCallback);
                }
            }
        } else {
            m.ensureMultipleAssignableRubyArray(multipleAsgnNode.getHeadNode() != null);
            
            if (multipleAsgnNode.getArgsNode() == null) {
                m.forEachInValueArray(0, multipleAsgnNode.getHeadNode().size(), multipleAsgnNode.getHeadNode(), headAssignCallback, null);
            } else {
                m.forEachInValueArray(0, multipleAsgnNode.getHeadNode().size(), multipleAsgnNode.getHeadNode(), headAssignCallback, argsCallback);
            }
        }
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildNewline(Node node, IR_BuilderContext m, boolean expr) {
        // TODO: add trace call?
        m.lineNumber(node.getPosition());

        m.setLinePosition(node.getPosition());

        NewlineNode newlineNode = (NewlineNode) node;

        build(newlineNode.getNextNode(), m, expr);
    }

    public void buildNext(Node node, IR_BuilderContext m, boolean expr) {
        final NextNode nextNode = (NextNode) node;

        CompilerCallback valueCallback = new CompilerCallback() {

                    public void call(IR_BuilderContext m) {
                        if (nextNode.getValueNode() != null) {
                            build(nextNode.getValueNode(), m,true);
                        } else {
                            m.loadNil();
                        }
                    }
                };

        m.pollThreadEvents();
        m.issueNextEvent(valueCallback);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildNthRef(Node node, IR_BuilderContext m, boolean expr) {
        NthRefNode nthRefNode = (NthRefNode) node;

        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) m.nthRef(nthRefNode.getMatchNumber());
        } else {
            m.nthRef(nthRefNode.getMatchNumber());
            if (!expr) m.consumeCurrentValue();
        }
    }

    public void buildNil(Node node, IR_BuilderContext m, boolean expr) {
        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) {
                m.loadNil();
                m.pollThreadEvents();
            }
        } else {
            m.loadNil();
            m.pollThreadEvents();
            if (!expr) m.consumeCurrentValue();
        }
    }

    public void buildNot(Node node, IR_BuilderContext m, boolean expr) {
        NotNode notNode = (NotNode) node;

        build(notNode.getConditionNode(), m, true);

        m.negateCurrentValue();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildOpAsgnAnd(Node node, IR_BuilderContext m, boolean expr) {
        final BinaryOperatorNode andNode = (BinaryOperatorNode) node;

        build(andNode.getFirstNode(), m,true);

        BranchCallback longCallback = new BranchCallback() {

                    public void branch(IR_BuilderContext m) {
                        build(andNode.getSecondNode(), m,true);
                    }
                };

        m.performLogicalAnd(longCallback);
        m.pollThreadEvents();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildOpAsgnOr(Node node, IR_BuilderContext m, boolean expr) {
        final OpAsgnOrNode orNode = (OpAsgnOrNode) node;

        if (needsDefinitionCheck(orNode.getFirstNode())) {
            buildGetDefinitionBase(orNode.getFirstNode(), m);

            m.isNull(new BranchCallback() {

                        public void branch(IR_BuilderContext m) {
                            build(orNode.getSecondNode(), m,true);
                        }
                    }, new BranchCallback() {

                        public void branch(IR_BuilderContext m) {
                            build(orNode.getFirstNode(), m,true);
                            m.duplicateCurrentValue();
                            m.performBooleanBranch(new BranchCallback() {

                                        public void branch(IR_BuilderContext m) {
                                        //Do nothing
                                        }
                                    },
                                    new BranchCallback() {

                                        public void branch(IR_BuilderContext m) {
                                            m.consumeCurrentValue();
                                            build(orNode.getSecondNode(), m,true);
                                        }
                                    });
                        }
                    });
        } else {
            build(orNode.getFirstNode(), m,true);
            m.duplicateCurrentValue();
            m.performBooleanBranch(new BranchCallback() {
                public void branch(IR_BuilderContext m) {
                //Do nothing
                }
            },
            new BranchCallback() {
                public void branch(IR_BuilderContext m) {
                    m.consumeCurrentValue();
                    build(orNode.getSecondNode(), m,true);
                }
            });

        }

        m.pollThreadEvents();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
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

    public void buildOpAsgn(Node node, IR_BuilderContext m, boolean expr) {
        final OpAsgnNode opAsgnNode = (OpAsgnNode) node;

        if (opAsgnNode.getOperatorName().equals("||")) {
            buildOpAsgnWithOr(opAsgnNode, m, true);
        } else if (opAsgnNode.getOperatorName().equals("&&")) {
            buildOpAsgnWithAnd(opAsgnNode, m, true);
        } else {
            buildOpAsgnWithMethod(opAsgnNode, m, true);
        }

        m.pollThreadEvents();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildOpAsgnWithOr(Node node, IR_BuilderContext m, boolean expr) {
        final OpAsgnNode opAsgnNode = (OpAsgnNode) node;

        final CompilerCallback receiverCallback = new CompilerCallback() {

            public void call(IR_BuilderContext m) {
                build(opAsgnNode.getReceiverNode(), m, true); // [recv]
            }
        };
        
        ArgumentsCallback argsCallback = getArgsCallback(opAsgnNode.getValueNode());
        
        m.getInvocationCompiler().invokeOpAsgnWithOr(opAsgnNode.getVariableName(), opAsgnNode.getVariableNameAsgn(), receiverCallback, argsCallback);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildOpAsgnWithAnd(Node node, IR_BuilderContext m, boolean expr) {
        final OpAsgnNode opAsgnNode = (OpAsgnNode) node;

        final CompilerCallback receiverCallback = new CompilerCallback() {

            public void call(IR_BuilderContext m) {
                build(opAsgnNode.getReceiverNode(), m, true); // [recv]
            }
        };
        
        ArgumentsCallback argsCallback = getArgsCallback(opAsgnNode.getValueNode());
        
        m.getInvocationCompiler().invokeOpAsgnWithAnd(opAsgnNode.getVariableName(), opAsgnNode.getVariableNameAsgn(), receiverCallback, argsCallback);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildOpAsgnWithMethod(Node node, IR_BuilderContext m, boolean expr) {
        final OpAsgnNode opAsgnNode = (OpAsgnNode) node;

        final CompilerCallback receiverCallback = new CompilerCallback() {
                    public void call(IR_BuilderContext m) {
                        build(opAsgnNode.getReceiverNode(), m, true); // [recv]
                    }
                };

        // eval new value, call operator on old value, and assign
        ArgumentsCallback argsCallback = new ArgumentsCallback() {
            public int getArity() {
                return 1;
            }

            public void call(IR_BuilderContext m) {
                build(opAsgnNode.getValueNode(), m, true);
            }
        };
        
        m.getInvocationCompiler().invokeOpAsgnWithMethod(opAsgnNode.getOperatorName(), opAsgnNode.getVariableName(), opAsgnNode.getVariableNameAsgn(), receiverCallback, argsCallback);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildOpElementAsgn(Node node, IR_BuilderContext m, boolean expr) {
        final OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode) node;
        
        if (opElementAsgnNode.getOperatorName() == "||") {
            buildOpElementAsgnWithOr(node, m, expr);
        } else if (opElementAsgnNode.getOperatorName() == "&&") {
            buildOpElementAsgnWithAnd(node, m, expr);
        } else {
            buildOpElementAsgnWithMethod(node, m, expr);
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

        public void call(IR_BuilderContext m) {
            if (getArity() == 1) {
                // if arity 1, just build the one element to save us the array cost
                build(((ArrayNode)node).get(0), m,true);
            } else {
                // build into array
                buildArguments(node, m);
            }
        }
    };

    public void buildOpElementAsgnWithOr(Node node, IR_BuilderContext m, boolean expr) {
        final OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {
            public void call(IR_BuilderContext m) {
                build(opElementAsgnNode.getReceiverNode(), m, true);
            }
        };

        ArgumentsCallback argsCallback = new OpElementAsgnArgumentsCallback(opElementAsgnNode.getArgsNode());

        CompilerCallback valueCallback = new CompilerCallback() {
            public void call(IR_BuilderContext m) {
                build(opElementAsgnNode.getValueNode(), m, true);
            }
        };

        m.getInvocationCompiler().opElementAsgnWithOr(receiverCallback, argsCallback, valueCallback);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildOpElementAsgnWithAnd(Node node, IR_BuilderContext m, boolean expr) {
        final OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {
            public void call(IR_BuilderContext m) {
                build(opElementAsgnNode.getReceiverNode(), m, true);
            }
        };

        ArgumentsCallback argsCallback = new OpElementAsgnArgumentsCallback(opElementAsgnNode.getArgsNode()); 

        CompilerCallback valueCallback = new CompilerCallback() {
            public void call(IR_BuilderContext m) {
                build(opElementAsgnNode.getValueNode(), m, true);
            }
        };

        m.getInvocationCompiler().opElementAsgnWithAnd(receiverCallback, argsCallback, valueCallback);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildOpElementAsgnWithMethod(Node node, IR_BuilderContext m, boolean expr) {
        final OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {
            public void call(IR_BuilderContext m) {
                build(opElementAsgnNode.getReceiverNode(), m,true);
            }
        };

        ArgumentsCallback argsCallback = getArgsCallback(opElementAsgnNode.getArgsNode());

        CompilerCallback valueCallback = new CompilerCallback() {
            public void call(IR_BuilderContext m) {
                build(opElementAsgnNode.getValueNode(), m,true);
            }
        };

        m.getInvocationCompiler().opElementAsgnWithMethod(receiverCallback, argsCallback, valueCallback, opElementAsgnNode.getOperatorName());
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildOr(Node node, IR_BuilderContext m, boolean expr) {
        final OrNode orNode = (OrNode) node;

        if (orNode.getFirstNode().getNodeType().alwaysTrue()) {
            // build first node only
            build(orNode.getFirstNode(), m, expr);
        } else if (orNode.getFirstNode().getNodeType().alwaysFalse()) {
            // build first node as non-expr and build second node
            build(orNode.getFirstNode(), m, false);
            build(orNode.getSecondNode(), m, expr);
        } else {
            build(orNode.getFirstNode(), m, true);

            BranchCallback longCallback = new BranchCallback() {

                        public void branch(IR_BuilderContext m) {
                            build(orNode.getSecondNode(), m, true);
                        }
                    };

            m.performLogicalOr(longCallback);
            // TODO: don't require pop
            if (!expr) m.consumeCurrentValue();
        }
    }

    public void buildPostExe(Node node, IR_BuilderContext m, boolean expr) {
        final PostExeNode postExeNode = (PostExeNode) node;

        // create the closure class and instantiate it
        final CompilerCallback closureBody = new CompilerCallback() {

                    public void call(IR_BuilderContext m) {
                        if (postExeNode.getBodyNode() != null) {
                            build(postExeNode.getBodyNode(), m, true);
                        } else {
                            m.loadNil();
                        }
                    }
                };
        m.createNewEndBlock(closureBody);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildPreExe(Node node, IR_BuilderContext m, boolean expr) {
        final PreExeNode preExeNode = (PreExeNode) node;

        // create the closure class and instantiate it
        final CompilerCallback closureBody = new CompilerCallback() {

                    public void call(IR_BuilderContext m) {
                        if (preExeNode.getBodyNode() != null) {
                            build(preExeNode.getBodyNode(), m,true);
                        } else {
                            m.loadNil();
                        }
                    }
                };
        m.runBeginBlock(preExeNode.getScope(), closureBody);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildRedo(Node node, IR_BuilderContext m, boolean expr) {
        //RedoNode redoNode = (RedoNode)node;

        m.issueRedoEvent();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildRegexp(Node node, IR_BuilderContext m, boolean expr) {
        RegexpNode reNode = (RegexpNode) node;

        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) m.createNewRegexp(reNode.getValue(), reNode.getOptions());
        } else {
            m.createNewRegexp(reNode.getValue(), reNode.getOptions());
            if (!expr) m.consumeCurrentValue();
        }
    }

    public void buildRescue(Node node, IR_BuilderContext m, boolean expr) {
        buildRescueInternal(node, m, false);
        
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    private void buildRescueInternal(Node node, IR_BuilderContext m, final boolean light) {
        final RescueNode rescueNode = (RescueNode) node;

        BranchCallback body = new BranchCallback() {
            public void branch(IR_BuilderContext m) {
                if (rescueNode.getBodyNode() != null) {
                    build(rescueNode.getBodyNode(), m, true);
                } else {
                    m.loadNil();
                }

                if (rescueNode.getElseNode() != null) {
                    m.consumeCurrentValue();
                    build(rescueNode.getElseNode(), m, true);
                }
            }
        };

        BranchCallback rubyHandler = new BranchCallback() {
            public void branch(IR_BuilderContext m) {
                buildRescueBodyInternal(rescueNode.getRescueNode(), m, light);
            }
        };

        ASTInspector rescueInspector = new ASTInspector();
        rescueInspector.inspect(rescueNode.getRescueNode());
        if (light) {
            m.performRescueLight(body, rubyHandler, rescueInspector.getFlag(ASTInspector.RETRY));
        } else {
            m.performRescue(body, rubyHandler, rescueInspector.getFlag(ASTInspector.RETRY));
        }
    }

    private void buildRescueBodyInternal(Node node, IR_BuilderContext m, final boolean light) {
        final RescueBodyNode rescueBodyNode = (RescueBodyNode) node;

        m.loadException();

        final Node exceptionList = rescueBodyNode.getExceptionNodes();
        ArgumentsCallback rescueArgs = getArgsCallback(exceptionList);
        if (rescueArgs == null) rescueArgs = new ArgumentsCallback() {
            public int getArity() {
                return 1;
            }

            public void call(IR_BuilderContext m) {
                m.loadStandardError();
            }
        };

        m.checkIsExceptionHandled(rescueArgs);

        BranchCallback trueBranch = new BranchCallback() {
            public void branch(IR_BuilderContext m) {
                // check if it's an immediate, and don't outline
                Node realBody = rescueBodyNode.getBodyNode();
                if (realBody instanceof NewlineNode) {
                    m.setLinePosition(realBody.getPosition());
                    while (realBody instanceof NewlineNode) {
                        realBody = ((NewlineNode)realBody).getNextNode();
                    }
                }

                if (realBody.getNodeType().isImmediate()) {
                    build(realBody, m, true);
                    m.clearErrorInfo();
                } else {
                    m.storeExceptionInErrorInfo();
                    if (light) {
                        build(rescueBodyNode.getBodyNode(), m, true);
                    } else {
                        IR_BuilderContext nestedBody = m.outline("rescue_line_" + rescueBodyNode.getPosition().getStartLine());
                        build(rescueBodyNode.getBodyNode(), nestedBody, true);
                        nestedBody.endBody();
                    }

                    // FIXME: this should reset to what it was before
                    m.clearErrorInfo();
                }
            }
        };

        BranchCallback falseBranch = new BranchCallback() {
            public void branch(IR_BuilderContext m) {
                if (rescueBodyNode.getOptRescueNode() != null) {
                    buildRescueBodyInternal(rescueBodyNode.getOptRescueNode(), m, light);
                } else {
                    m.rethrowException();
                }
            }
        };

        m.performBooleanBranch(trueBranch, falseBranch);
    }

    public void buildRetry(Node node, IR_BuilderContext m, boolean expr) {
        m.pollThreadEvents();

        m.issueRetryEvent();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildReturn(Node node, IR_BuilderContext m, boolean expr) {
        ReturnNode returnNode = (ReturnNode) node;

        if (returnNode.getValueNode() != null) {
            build(returnNode.getValueNode(), m,true);
        } else {
            m.loadNil();
        }

        m.performReturn();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildRoot(Node node, ScriptCompiler m, ASTInspector inspector) {
        buildRoot(node, m, inspector, true, true);
    }

    public void buildRoot(Node node, ScriptCompiler m, ASTInspector inspector, boolean load, boolean main) {
        RootNode rootNode = (RootNode) node;
        StaticScope staticScope = rootNode.getStaticScope();

        m.startScript(staticScope);

        // force static scope to claim restarg at 0, so it only implements the [] version of __file__
        staticScope.setRestArg(-2);

        // create method for toplevel of script
        IR_BuilderContext methodCompiler = m.startRoot("__file__", "__file__", staticScope, inspector);

        Node nextNode = rootNode.getBodyNode();
        if (nextNode != null) {
            if (nextNode.getNodeType() == NodeType.BLOCKNODE) {
                // it's a multiple-statement body, iterate over all elements in turn and chain if it get too long
                BlockNode blockNode = (BlockNode) nextNode;

                for (int i = 0; i < blockNode.size(); i++) {
                    if ((i + 1) % RubyInstanceConfig.CHAINED_COMPILE_LINE_COUNT == 0) {
                        methodCompiler = methodCompiler.chainToMethod("__file__from_line_" + (i + 1));
                    }
                    build(blockNode.get(i), methodCompiler, i + 1 >= blockNode.size());
                }
            } else {
                // single-statement body, just build it
                build(nextNode, methodCompiler,true);
            }
        } else {
            methodCompiler.loadNil();
        }

        methodCompiler.endBody();

        m.endScript(load, main);
    }

    public void buildSelf(Node node, IR_BuilderContext m, boolean expr) {
        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) m.retrieveSelf();
        } else {
            m.retrieveSelf();
            if (!expr) m.consumeCurrentValue();
        }
    }

    public void buildSplat(Node node, IR_BuilderContext m, boolean expr) {
        SplatNode splatNode = (SplatNode) node;

        build(splatNode.getValue(), m, true);

        m.splatCurrentValue();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildStr(Node node, IR_BuilderContext m, boolean expr) {
        StrNode strNode = (StrNode) node;

        boolean doit = expr || !RubyInstanceConfig.PEEPHOLE_OPTZ;
        boolean popit = !RubyInstanceConfig.PEEPHOLE_OPTZ && !expr;

        if (doit) {
            if (strNode instanceof FileNode) {
                m.loadFilename();
            } else {
                m.createNewString(strNode.getValue());
            }
        }
        if (popit) m.consumeCurrentValue();
    }

    public void buildSuper(Node node, IR_BuilderContext m, boolean expr) {
        final SuperNode superNode = (SuperNode) node;

        ArgumentsCallback argsCallback = getArgsCallback(superNode.getArgsNode());

        CompilerCallback closureArg = getBlock(superNode.getIterNode());

        m.getInvocationCompiler().invokeDynamic(null, null, argsCallback, CallType.SUPER, closureArg, superNode.getIterNode() instanceof IterNode);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildSValue(Node node, IR_BuilderContext m, boolean expr) {
        SValueNode svalueNode = (SValueNode) node;

        build(svalueNode.getValue(), m,true);

        m.singlifySplattedValue();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildSymbol(Node node, IR_BuilderContext m, boolean expr) {
        m.createNewSymbol(((SymbolNode) node).getName());
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }    
    
    public void buildToAry(Node node, IR_BuilderContext m, boolean expr) {
        ToAryNode toAryNode = (ToAryNode) node;

        build(toAryNode.getValue(), m,true);

        m.aryToAry();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildTrue(Node node, IR_BuilderContext m, boolean expr) {
        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) {
                m.loadTrue();
                m.pollThreadEvents();
            }
        } else {
            m.loadTrue();
            m.pollThreadEvents();
            if (!expr) m.consumeCurrentValue();
        }
    }

    public void buildUndef(Node node, IR_BuilderContext m, boolean expr) {
        m.undefMethod(((UndefNode) node).getName());
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildUntil(Node node, IR_BuilderContext m, boolean expr) {
        final UntilNode untilNode = (UntilNode) node;

        if (untilNode.getConditionNode().getNodeType().alwaysTrue() &&
                untilNode.evaluateAtStart()) {
            // condition is always true, just build it and not body
            build(untilNode.getConditionNode(), m, false);
            if (expr) m.loadNil();
        } else {
            BranchCallback condition = new BranchCallback() {

                public void branch(IR_BuilderContext m) {
                    build(untilNode.getConditionNode(), m, true);
                    m.negateCurrentValue();
                }
            };

            BranchCallback body = new BranchCallback() {

                public void branch(IR_BuilderContext m) {
                    if (untilNode.getBodyNode() != null) {
                        build(untilNode.getBodyNode(), m, true);
                    }
                }
            };

            if (untilNode.containsNonlocalFlow) {
                m.performBooleanLoopSafe(condition, body, untilNode.evaluateAtStart());
            } else {
                m.performBooleanLoopLight(condition, body, untilNode.evaluateAtStart());
            }

            m.pollThreadEvents();
            // TODO: don't require pop
            if (!expr) m.consumeCurrentValue();
        }
    }

    public void buildVAlias(Node node, IR_BuilderContext m, boolean expr) {
        VAliasNode valiasNode = (VAliasNode) node;

        m.aliasGlobal(valiasNode.getNewName(), valiasNode.getOldName());
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildVCall(Node node, IR_BuilderContext m, boolean expr) {
        VCallNode vcallNode = (VCallNode) node;
        
        m.getInvocationCompiler().invokeDynamic(vcallNode.getName(), null, null, CallType.VARIABLE, null, false);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildWhile(Node node, IR_BuilderContext m, boolean expr) {
        final WhileNode whileNode = (WhileNode) node;

        if (whileNode.getConditionNode().getNodeType().alwaysFalse() &&
                whileNode.evaluateAtStart()) {
            // do nothing
            if (expr) m.loadNil();
        } else {
            BranchCallback condition = new BranchCallback() {

                public void branch(IR_BuilderContext m) {
                    build(whileNode.getConditionNode(), m, true);
                }
            };

            BranchCallback body = new BranchCallback() {

                public void branch(IR_BuilderContext m) {
                    if (whileNode.getBodyNode() != null) {
                        build(whileNode.getBodyNode(), m, true);
                    }
                }
            };

            if (whileNode.containsNonlocalFlow) {
                m.performBooleanLoopSafe(condition, body, whileNode.evaluateAtStart());
            } else {
                m.performBooleanLoopLight(condition, body, whileNode.evaluateAtStart());
            }

            m.pollThreadEvents();
            // TODO: don't require pop
            if (!expr) m.consumeCurrentValue();
        }
    }

    public void buildXStr(Node node, IR_BuilderContext m, boolean expr) {
        final XStrNode xstrNode = (XStrNode) node;

        ArgumentsCallback argsCallback = new ArgumentsCallback() {
            public int getArity() {
                return 1;
            }

            public void call(IR_BuilderContext m) {
                m.createNewString(xstrNode.getValue());
            }
        };
        m.getInvocationCompiler().invokeDynamic("`", null, argsCallback, CallType.FUNCTIONAL, null, false);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildYield(Node node, IR_BuilderContext m, boolean expr) {
        final YieldNode yieldNode = (YieldNode) node;

        ArgumentsCallback argsCallback = getArgsCallback(yieldNode.getArgsNode());

        // TODO: This filtering is kind of gross...it would be nice to get some parser help here
        if (argsCallback == null || argsCallback.getArity() == 0) {
            m.getInvocationCompiler().yieldSpecific(argsCallback);
        } else if ((argsCallback.getArity() == 1 || argsCallback.getArity() == 2 || argsCallback.getArity() == 3) && yieldNode.getExpandArguments()) {
            // send it along as arity-specific, we don't need the array
            m.getInvocationCompiler().yieldSpecific(argsCallback);
        } else {
            CompilerCallback argsCallback2 = null;
            if (yieldNode.getArgsNode() != null) {
                argsCallback2 = new CompilerCallback() {
                    public void call(IR_BuilderContext m) {
                        build(yieldNode.getArgsNode(), m,true);
                    }
                };
            }

            m.getInvocationCompiler().yield(argsCallback2, yieldNode.getExpandArguments());
        }
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildZArray(Node node, IR_BuilderContext m, boolean expr) {
        boolean doit = expr || !RubyInstanceConfig.PEEPHOLE_OPTZ;
        boolean popit = !RubyInstanceConfig.PEEPHOLE_OPTZ && !expr;

        if (doit) {
            m.createEmptyArray();
        }

        if (popit) m.consumeCurrentValue();
    }

    public void buildZSuper(Node node, IR_BuilderContext m, boolean expr) {
        ZSuperNode zsuperNode = (ZSuperNode) node;

        CompilerCallback closure = getBlock(zsuperNode.getIterNode());

        m.callZSuper(closure);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildArgsCatArguments(Node node, IR_BuilderContext m, boolean expr) {
        ArgsCatNode argsCatNode = (ArgsCatNode) node;

        buildArguments(argsCatNode.getFirstNode(), m);
        // arguments buildrs always create IRubyObject[], but we want to use RubyArray.concat here;
        // FIXME: as a result, this is NOT efficient, since it creates and then later unwraps an array
        m.createNewArray(true);
        build(argsCatNode.getSecondNode(), m,true);
        m.splatCurrentValue();
        m.concatArrays();
        m.convertToJavaArray();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildArgsPushArguments(Node node, IR_BuilderContext m, boolean expr) {
        ArgsPushNode argsPushNode = (ArgsPushNode) node;
        build(argsPushNode.getFirstNode(), m,true);
        build(argsPushNode.getSecondNode(), m,true);
        m.appendToArray();
        m.convertToJavaArray();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildArrayArguments(Node node, IR_BuilderContext m, boolean expr) {
        ArrayNode arrayNode = (ArrayNode) node;

        ArrayCallback callback = new ArrayCallback() {

                    public void nextValue(IR_BuilderContext m, Object sourceArray, int index) {
                        Node node = (Node) ((Object[]) sourceArray)[index];
                        build(node, m,true);
                    }
                };

        m.setLinePosition(arrayNode.getPosition());
        m.createObjectArray(arrayNode.childNodes().toArray(), callback);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    // leave as a normal array
    }

    public void buildSplatArguments(Node node, IR_BuilderContext m, boolean expr) {
        SplatNode splatNode = (SplatNode) node;

        build(splatNode.getValue(), m,true);
        m.splatCurrentValue();
        m.convertToJavaArray();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }
}
