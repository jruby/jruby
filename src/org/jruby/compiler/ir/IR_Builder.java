package org.jruby.compiler.ir;

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
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.NotCompilableException;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.BlockBody;

// This class converts an AST into a bunch of IR instructions

// IR Building Notes
// -----------------
//
// 1. More copy instructions added than necessary
// ----------------------------------------------
// Note that in general, there will be lots of a = b kind of copies
// introduced in the IR because the translation is entirely single-node focused.
// An example will make this clear
//
// RUBY: 
//     v = @f 
// will translate to 
//
// AST: 
//     LocalAsgnNode v 
//       InstrVarNode f 
// will translate to
//
// IR: 
//     tmp = self.f [ GET_FIELD(tmp,self,f) ]
//     v = tmp      [ COPY(v, tmp) ]
//
// instead of
//     v = self.f   [ GET_FIELD(v, self, f) ]
//
// We could get smarter and pass in the variable into which this expression is going to get evaluated
// and use that to store the value of the expression (or not build the expression if the variable is null).
//
// But, that makes the code more complicated, and in any case, all this will get fixed in a single pass of
// copy propagation and dead-code elimination.
//
// Something to pay attention to and if this extra pass becomes a concern (not convinced that it is yet),
// this smart can be built in here.  Right now, the goal is to do something simple and straightforward that is going to be correct.
//
// 2. Returning null vs Nil.NIL
// ----------------------------
// - We should be returning null from the build methods where it is a normal "error" condition
// - We should be returning Nil.NIL where the actual return value of a build is the ruby nil operand
//   Look in buildIfNode for an example of this

public class IR_Builder
{
    private boolean isAtRoot = false;

    public static Node skipOverNewlines(Node n)
    {
        //Equivalent check ..
        //while (n instanceof NewlineNode)
        while (n.getNodeType() == NodeType.NEWLINENODE)
            n = ((NewlineNode)n).getNextNode();

        return n;
    }

    public Operand build(Node node, IR_Scope m) {
        if (node == null) {
            return null;
        }
        switch (node.getNodeType()) {
            case ALIASNODE: return buildAlias(node, m);
            case ANDNODE: return buildAnd(node, m);
            case ARGSCATNODE: return buildArgsCat(node, m);
            case ARGSPUSHNODE: return buildArgsPush(node, m);
            case ARRAYNODE: return buildArray(node, m);
            case ATTRASSIGNNODE: return buildAttrAssign(node, m);
            case BACKREFNODE: return buildBackref(node, m);
            case BEGINNODE: return buildBegin(node, m);
            case BIGNUMNODE: return buildBignum(node, m);
            case BLOCKNODE: return buildBlock(node, m);
            case BREAKNODE: return buildBreak(node, m);
            case CALLNODE: return buildCall(node, m);
            case CASENODE: return buildCase(node, m);
            case CLASSNODE: return buildClass(node, m);
            case CLASSVARNODE: return buildClassVar(node, m);
            case CLASSVARASGNNODE: return buildClassVarAsgn(node, m);
            case CLASSVARDECLNODE: return buildClassVarDecl(node, m);
            case COLON2NODE: return buildColon2(node, m);
            case COLON3NODE: return buildColon3(node, m);
            case CONSTDECLNODE: return buildConstDecl(node, m);
            case CONSTNODE: return buildConst(node, m);
            case DASGNNODE: return buildDAsgn(node, m);
            case DEFINEDNODE: return buildDefined(node, m);
            case DEFNNODE: return buildDefn(node, m);
            case DEFSNODE: return buildDefs(node, m);
            case DOTNODE: return buildDot(node, m);
            case DREGEXPNODE: return buildDRegexp(node, m);
            case DSTRNODE: return buildDStr(node, m);
            case DSYMBOLNODE: return buildDSymbol(node, m);
            case DVARNODE: return buildDVar(node, m);
            case DXSTRNODE: return buildDXStr(node, m);
            case ENSURENODE: return buildEnsureNode(node, m);
            case EVSTRNODE: return buildEvStr(node, m);
            case FALSENODE: return buildFalse(node, m);
            case FCALLNODE: return buildFCall(node, m);
            case FIXNUMNODE: return buildFixnum(node, m);
            case FLIPNODE: return buildFlip(node, m);
            case FLOATNODE: return buildFloat(node, m);
            case FORNODE: return buildFor(node, m);
            case GLOBALASGNNODE: return buildGlobalAsgn(node, m);
            case GLOBALVARNODE: return buildGlobalVar(node, m);
            case HASHNODE: return buildHash(node, m);
            case IFNODE: return buildIf(node, m);
            case INSTASGNNODE: return buildInstAsgn(node, m);
            case INSTVARNODE: return buildInstVar(node, m);
            case ITERNODE: return buildIter(node, m);
            case LOCALASGNNODE: return buildLocalAsgn(node, m);
            case LOCALVARNODE: return buildLocalVar(node, m);
            case MATCH2NODE: return buildMatch2(node, m);
            case MATCH3NODE: return buildMatch3(node, m);
            case MATCHNODE: return buildMatch(node, m);
            case MODULENODE: return buildModule(node, m);
            case MULTIPLEASGNNODE: return buildMultipleAsgn(node, m);
            case NEWLINENODE: return buildNewline(node, m);
            case NEXTNODE: return buildNext(node, m);
            case NTHREFNODE: return buildNthRef(node, m);
            case NILNODE: return buildNil(node, m);
            case NOTNODE: return buildNot(node, m);
            case OPASGNANDNODE: return buildOpAsgnAnd(node, m);
            case OPASGNNODE: return buildOpAsgn(node, m);
            case OPASGNORNODE: return buildOpAsgnOr(node, m);
            case OPELEMENTASGNNODE: return buildOpElementAsgn(node, m);
            case ORNODE: return buildOr(node, m);
            case POSTEXENODE: return buildPostExe(node, m);
            case PREEXENODE: return buildPreExe(node, m);
            case REDONODE: return buildRedo(node, m);
            case REGEXPNODE: return buildRegexp(node, m);
            case RESCUEBODYNODE:
                throw new NotCompilableException("rescue body is handled by rescue compilation at: " + node.getPosition());
            case RESCUENODE: return buildRescue(node, m);
            case RETRYNODE: return buildRetry(node, m);
            case RETURNNODE: return buildReturn(node, m);
            case ROOTNODE:
                throw new NotCompilableException("Use buildRoot(); Root node at: " + node.getPosition());
            case SCLASSNODE: return buildSClass(node, m);
            case SELFNODE: return buildSelf(node, m);
            case SPLATNODE: return buildSplat(node, m);
            case STRNODE: return buildStr(node, m);
            case SUPERNODE: return buildSuper(node, m);
            case SVALUENODE: return buildSValue(node, m);
            case SYMBOLNODE: return buildSymbol(node, m);
            case TOARYNODE: return buildToAry(node, m);
            case TRUENODE: return buildTrue(node, m);
            case UNDEFNODE: return buildUndef(node, m);
            case UNTILNODE: return buildUntil(node, m);
            case VALIASNODE: return buildVAlias(node, m);
            case VCALLNODE: return buildVCall(node, m);
            case WHILENODE: return buildWhile(node, m);
            case WHENNODE: assert false : "When nodes are handled by case node compilation."; break;
            case XSTRNODE: return buildXStr(node, m);
            case YIELDNODE: return buildYield(node, m);
            case ZARRAYNODE: return buildZArray(node, m);
            case ZSUPERNODE: return buildZSuper(node, m);
            default: throw new NotCompilableException("Unknown node encountered in buildr: " + node);
        }
    }

    public void buildArguments(List<Operand> args, Node node, IR_Scope s) {
        switch (node.getNodeType()) {
            case ARGSCATNODE: buildArgsCatArguments(args, node, s, true);
            case ARGSPUSHNODE: buildArgsPushArguments(args, node, s, true);
            case ARRAYNODE: buildArrayArguments(args, node, s, true);
            case SPLATNODE: buildSplatArguments(args, node, s, true);
            default: 
                Operand retVal = build(node, s, true);
                s.convertToJavaArray(); 
                return (retVal == null) ? null : new ArrayList<Operand>(retVal);
        }
    }
    
    public void buildVariableArityArguments(List<Operand> args, Node node, IR_Scope s) {
       buildArguments(args, node, s);
    }

    public void buildSpecificArityArguments (List<Operand> args, Node node, IR_Scope s) {
        if (node.getNodeType() == NodeType.ARRAYNODE) {
            ArrayNode arrayNode = (ArrayNode)node;
            if (arrayNode.isLightweight()) {
                // explode array, it's an internal "args" array
                for (Node n : arrayNode.childNodes())
                    args.add(build(n, s, true));
            } else {
                // use array as-is, it's a literal array
                args.add(build(arrayNode, s, true));
            }
        } else {
            args.add(build(node, s, true));
        }
    }

    public List<Operand> setupArgs(Node receiver, Node args, IR_Scope s) {
        if (args == null)
            return null;

        // unwrap newline nodes to get their actual type
        args = skipOverNewlines(args);

        List<Operand> argsList = new ArrayList<Operand>();
        argList.add(build(receiver, s, true)); // SSS FIXME: I added this in.  Is this correct?
        buildArgs(argsList, args, s);

        return argsList;
    }

    public List<Operand> setupArgs(Node args, IR_Scope s) {
        if (args == null)
            return null;

        // unwrap newline nodes to get their actual type
        args = skipOverNewlines(args);

        List<Operand> argList = new ArrayList<Operand>();
        argList.add(m.getSelf());
        buildArgs(argList, args, s);

        return argList;
    }

    public void buildArgs(List<Operand> argsList, Node args, IR_Scope s) {
        switch (args.getNodeType()) {
            case ARGSCATNODE:
            case ARGSPUSHNODE:
            case SPLATNODE:
                buildVariableArityArguments(argsList, args, s);
                break;
            case ARRAYNODE:
                ArrayNode arrayNode = (ArrayNode)args;
                if (arrayNode.size() > 3)
                    buildVariableArityArguments(argsList, arrayNode, s);
                else if (arrayNode.size() > 0)
                    buildSpecificArityArguments(argsList, arrayNode, s);
                break;
            default:
                buildSpecificArityArguments(argsList, arrayNode, s);
                break;
        }
    }

    public Operand buildAssignment(Node node, IR_Scope m) {
        switch (node.getNodeType()) {
            case ATTRASSIGNNODE:
                buildAttrAssignAssignment(node, m);
                break;
            case DASGNNODE:
                DAsgnNode dasgnNode = (DAsgnNode)node;
                m.getVariableCompiler().assignLocalVariable(dasgnNode.getIndex(), dasgnNode.getDepth());
                break;
            case CLASSVARASGNNODE:
                buildClassVarAsgnAssignment(node, m);
                break;
            case CLASSVARDECLNODE:
                buildClassVarDeclAssignment(node, m);
                break;
            case CONSTDECLNODE:
                buildConstDeclAssignment(node, m);
                break;
            case GLOBALASGNNODE:
                buildGlobalAsgnAssignment(node, m);
                break;
            case INSTASGNNODE:
                buildInstAsgnAssignment(node, m);
                break;
            case LOCALASGNNODE:
                LocalAsgnNode localAsgnNode = (LocalAsgnNode)node;
                m.getVariableCompiler().assignLocalVariable(localAsgnNode.getIndex(), localAsgnNode.getDepth());
                break;
            case MULTIPLEASGNNODE:
                buildMultipleAsgnAssignment(node, m);
                break;
            case ZEROARGNODE:
                throw new NotCompilableException("Shouldn't get here; zeroarg does not do assignment: " + node);
            default:
                throw new NotCompilableException("Can't build assignment node: " + node);
        }
    }

    public Operand buildAlias(Node node, IR_Scope s) {
        final AliasNode alias = (AliasNode) node;

            // SSS FIXME: This method name should be fetched from some other place rather than be hardcoded here?
        MethAddr   ma   = new MethAddr("defineAlias");  
        Operand[]  args = new Operand[] { new MetaObject(s), new MethAddr(alias.getNewName()), new MethAddr(alias.getOldName()) };
        m.addInstr(new RUBY_IMPL_CALL_Instr(null, ma, ));

            // SSS FIXME: Can this return anything other than nil?
        return Nil.NIL;
    }

    // Translate "ret = (a && b)" --> "ret = (a ? b : false)" -->
    // 
    //    v1 = -- build(a) --
    //       OPT: ret can be set to v1, but effectively v1 is false if we take the branch to L.
    //            while this info can be inferred by using attributes, why bother if we can do this?
    //    ret = false   
    //    beq(v1, false, L)
    //    v2 = -- build(b) --
    //    ret = v2
    // L:
    //
    public Operand buildAnd(Node node, IR_Scope m) {
        final AndNode andNode = (AndNode) node;

        if (andNode.getFirstNode().getNodeType().alwaysTrue()) {
            // build first node (and ignore its result) and then second node
            build(andNode.getFirstNode(), m);
            return build(andNode.getSecondNode(), m);
        } else if (andNode.getFirstNode().getNodeType().alwaysFalse()) {
            // build first node only and return false
            build(andNode.getFirstNode(), m);
            return BooleanLiteral.FALSE;
        } else {
            Variable ret = m.getNewTmpVariable();
            Label    l   = m.getNewLabel();
            Operand  v1  = build(andNode.getFirstNode(), m);
            m.addInstr(new COPY_Instr(ret, BooleanLiteral.FALSE));
            m.addInstr(new BEQ_Instr(v1, BooleanLiteral.FALSE, l));
            Operand  v2  = build(andNode.getSecondNode(), m);
            m.addInstr(new COPY_Instr(ret, v2);
            m.addInstr(new LABEL_Instr(l));
            return ret;
        }
    }

    public Operand buildArray(Node node, IR_Scope m) {
        List<Operand> elts = new ArrayList<Operand>();
        for (Node e: node.childNodes())
            elts.add(build(e, m));

        return new Array(elts);
    }

    public Operand buildArgsCat(Node node, IR_Scope m) {
        ArgsCatNode argsCatNode = (ArgsCatNode) node;

        build(argsCatNode.getFirstNode(), m);
        m.ensureRubyArray();
        build(argsCatNode.getSecondNode(), m);
        m.splatCurrentValue();
        m.concatArrays();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildArgsPush(Node node, IR_Scope m) {
        throw new NotCompilableException("ArgsPush should never be encountered bare in 1.8");
    }

    private void buildAttrAssign(Node node, IR_Scope m) {
        final AttrAssignNode attrAssignNode = (AttrAssignNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {
            public void call(IR_Scope m) {
                build(attrAssignNode.getReceiverNode(), m,true);
            }
        };
        
        ArgumentsCallback argsCallback = setupArgs(attrAssignNode.getArgsNode());

        m.getInvocationCompiler().invokeAttrAssign(attrAssignNode.getName(), receiverCallback, argsCallback);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildAttrAssignAssignment(Node node, IR_Scope m) {
        final AttrAssignNode attrAssignNode = (AttrAssignNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {
            public void call(IR_Scope m) {
                build(attrAssignNode.getReceiverNode(), m,true);
            }
        };
        ArgumentsCallback argsCallback = setupArgs(attrAssignNode.getArgsNode());

        m.getInvocationCompiler().invokeAttrAssignMasgn(attrAssignNode.getName(), receiverCallback, argsCallback);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildBackref(Node node, IR_Scope m) {
        BackRefNode iVisited = (BackRefNode) node;

        m.performBackref(iVisited.getType());
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildBegin(Node node, IR_Scope m) {
        return build(((BeginNode)node).getBodyNode(), m);
    }

    public Operand buildBignum(Node node, IR_Scope s) {
        return new Fixnum(((BignumNode)node).getValue());
    }

    public Operand buildBlock(Node node, IR_Scope s) {
        Operand retVal = null;
        for (Iterator<Node> iter = ((BlockNode) node).childNodes().iterator(); iter.hasNext();)
            retVal = build(iter.next(), s);

           // Value of the last expression in the block 
        return retVal;
    }

    public Operand buildBreak(Node node, IR_Scope m) {
        final BreakNode breakNode = (BreakNode) node;
        m.addInstr(new BREAK_Instr(build(breakNode.getValueNode(), m)));

            // SSS FIXME: Should I be returning the operand constructed here?
        return Nil.NIL;
    }

    public Operand buildCall(Node node, IR_Scope s) {
        CallNode callNode = (CallNode) node;

        Node          callArgsNode = callNode.getArgsNode();
        Node          receiverNode = callNode.getReceiverNode();
        List<Operand> args         = setupArgs(receiverNode, callArgsNode, s);
        Operand       block        = setupCallClosure(callNode.getIterNode(), s);
        Variable      callResult   = s.getNewTmpVariable();
        IR_Instr      callInstr    = new CALL_Instr(callResult, new MethAddr(callNode.getName()), args.toArray(new Operand[args.size()]), block);
        s.addInstr(callInstr);
        return callResult;
    }

    public Operand buildCase(Node node, IR_Scope m) {
        CaseNode caseNode = (CaseNode) node;

        boolean hasCase = caseNode.getCaseNode() != null;

        // aggregate when nodes into a list, unfortunately, this is no
        List<Node> cases = caseNode.getCases().childNodes();

        // last node, either !instanceof WhenNode or null, is the else
        Node elseNode = caseNode.getElseNode();

        buildWhen(caseNode.getCaseNode(), cases, elseNode, m, hasCase);
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

    public Operand buildWhen(final Node value, List<Node> whenNodes, final Node elseNode, IR_Scope m, final boolean expr, final boolean hasCase) {
        CompilerCallback caseValue = null;
        if (value != null) caseValue = new CompilerCallback() {
            public void call(IR_Scope m) {
                build(value, m, true);
                m.addInstr(new THREAD_POLL_Instr());
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
                public void call(IR_Scope m) {
                    build(whenNode.getBodyNode(), m);
                }
            };
            addConditionalForWhen(whenNode, conditionals, bodies, body);
            if (switchCases != null) switchCases.put(body, getOptimizedCases(whenNode));
        }
        
        CompilerCallback fallback = new CompilerCallback() {
            public void call(IR_Scope m) {
                build(elseNode, m);
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

                    public void call(IR_Scope m) {
                        build(whenNode.getExpressionNodes(), m, true);
                    }
                });
                return;
            }
        }
        // otherwise, use normal args buildr
        conditionals.add(setupArgs(whenNode.getExpressionNodes()));
    }

    public Operand buildClass(Node node, IR_Scope m) {
        final ClassNode classNode = (ClassNode) node;

        final Node superNode = classNode.getSuperNode();

        final Node cpathNode = classNode.getCPath();

        CompilerCallback superCallback = new CompilerCallback() {

                    public void call(IR_Scope m) {
                        build(superNode, m, true);
                    }
                };
        if (superNode == null) {
            superCallback = null;
        }

        CompilerCallback bodyCallback = new CompilerCallback() {

                    public void call(IR_Scope m) {
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

                    public void call(IR_Scope m) {
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

    public Operand buildSClass(Node node, IR_Scope m) {
        final SClassNode sclassNode = (SClassNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {

                    public void call(IR_Scope m) {
                        build(sclassNode.getReceiverNode(), m, true);
                    }
                };

        CompilerCallback bodyCallback = new CompilerCallback() {

                    public void call(IR_Scope m) {
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

    public Operand buildClassVar(Node node, IR_Scope s) {
        Variable ret = s.getNewTmpVariable();
            // SSS FIXME: Is this right?  What if 's' is not a class??  Can that happen?
        s.addInstr(new GET_FIELD_Instr(ret, new MetaObject(s), ((ClassVarNode)node).getName()));
        return ret;
    }

    public Operand buildClassVarAsgn(Node node, IR_Scope s) {
        final ClassVarAsgnNode classVarAsgnNode = (ClassVarAsgnNode) node;
        Operand val = build(classVarAsgnNode.getValueNode(), s);
        s.addInstr(new PUT_FIELD_Instr(new MetaObject(s), ((ClassVarNode)node).getName(), val));
        return val;
    }

    public Operand buildClassVarAsgnAssignment(Node node, IR_Scope m) {
        ClassVarAsgnNode classVarAsgnNode = (ClassVarAsgnNode) node;

        m.assignClassVariable(classVarAsgnNode.getName());
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildClassVarDecl(Node node, IR_Scope m) {
        final ClassVarDeclNode classVarDeclNode = (ClassVarDeclNode) node;

        CompilerCallback value = new CompilerCallback() {
            public void call(IR_Scope m) {
                build(classVarDeclNode.getValueNode(), m, true);
            }
        };
        
        m.declareClassVariable(classVarDeclNode.getName(), value);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildClassVarDeclAssignment(Node node, IR_Scope m) {
        ClassVarDeclNode classVarDeclNode = (ClassVarDeclNode) node;

        m.declareClassVariable(classVarDeclNode.getName());
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildConstDecl(Node node, IR_Scope m) {
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

    public Operand buildConstDeclAssignment(Node node, IR_Scope m) {
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

    public Operand buildConst(Node node, IR_Scope s) {
        String constName = ((ConstNode) node).getName();

            // Sometimes the value can be retrieved at compile time.
            // If we succeed, nothing like it!  We might not succeed for the following reasons:
            // 1. The constant is missing,
            // 2. The reference is a forward-reference,
            // 3. The constant's value is known at runt-time on first-access,
            // 4. Our compiler isn't able to right away infer that this is a constant.
            //
            // SSS FIXME:
            // 1. The operand can be a literal array, range, or hash -- hence Operand
            //    because Array, Range, and Hash derive from Operand and not Constant ...
            //    Is there a way to fix this impedance mismatch?
            // 2. It should be possible to handle the forward-reference case by creating a new
            //    ForwardReference operand and then inform the scope of the forward reference
            //    which the scope can fix up when the reference gets defined.  At code-gen time,
            //    if the reference is unresolved, when a value is retrieved for the forward-ref
            //    and we get a null, we can throw a ConstMissing exception!  Not sure!
        Operand constVal = s.getConstantValue(constName);
        if (constVal == null) {
            constVal = s.getNewTmpVariable();
                // SSS FIXME: 
                // 1. Is "retrieveConstant" the right utility method for loading the constant?
                // 2. This method name should be fetched from some other place rather than be hardcoded here?
            s.addInstr(new RUBY_IMPL_CALL_Instr(constVal, 
                                                new MethAddr("retrieveConstant"), 
                                                new Operand[] { new MetaObject(s), new Reference(constName) }));
            // XXX: const lookup can trigger const_missing; is that enough to warrant it always being executed?
        }
        return constVal;
    }

    public Operand buildColon2(Node node, IR_Scope m) {
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
                    public void call(IR_Scope m) {
                        build(iVisited.getLeftNode(), m,true);
                    }
                };
                
                m.getInvocationCompiler().invokeDynamic(name, receiverCallback, null, CallType.FUNCTIONAL, null, false);
            }
        }
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildColon3(Node node, IR_Scope m) {
        Colon3Node iVisited = (Colon3Node) node;
        String name = iVisited.getName();

        m.retrieveConstantFromObject(name);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildGetDefinitionBase(final Node node, IR_Scope m) {
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

                        public void branch(IR_Scope m) {
                            m.inDefined();
                            buildGetDefinition(node, m);
                        }
                    };
            BranchCallback out = new BranchCallback() {

                        public void branch(IR_Scope m) {
                            m.outDefined();
                        }
                    };
            m.protect(reg, out, String.class);
        }
    }

    public Operand buildDefined(final Node node, IR_Scope m) {
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

    public Operand buildGetArgumentDefinition(final Node node, IR_Scope m, String type) {
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

    public Operand buildGetDefinition(final Node node, IR_Scope m) {
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

                            public void branch(IR_Scope m) {
                                m.pushString("$" + ((BackRefNode) node).getType());
                            }
                        },
                        new BranchCallback() {

                            public void branch(IR_Scope m) {
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

                            public void branch(IR_Scope m) {
                                m.pushString("$" + ((NthRefNode) node).getMatchNumber());
                            }
                        },
                        new BranchCallback() {

                            public void branch(IR_Scope m) {
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

                            public void branch(IR_Scope m) {
                                m.pushString("method");
                            }
                        },
                        new BranchCallback() {

                            public void branch(IR_Scope m) {
                                m.pushNull();
                            }
                        });
                break;
            case YIELDNODE:
                m.hasBlock(new BranchCallback() {

                            public void branch(IR_Scope m) {
                                m.pushString("yield");
                            }
                        },
                        new BranchCallback() {

                            public void branch(IR_Scope m) {
                                m.pushNull();
                            }
                        });
                break;
            case GLOBALVARNODE:
                m.isGlobalDefined(((GlobalVarNode) node).getName(),
                        new BranchCallback() {

                            public void branch(IR_Scope m) {
                                m.pushString("global-variable");
                            }
                        },
                        new BranchCallback() {

                            public void branch(IR_Scope m) {
                                m.pushNull();
                            }
                        });
                break;
            case INSTVARNODE:
                m.isInstanceVariableDefined(((InstVarNode) node).getName(),
                        new BranchCallback() {

                            public void branch(IR_Scope m) {
                                m.pushString("instance-variable");
                            }
                        },
                        new BranchCallback() {

                            public void branch(IR_Scope m) {
                                m.pushNull();
                            }
                        });
                break;
            case CONSTNODE:
                m.isConstantDefined(((ConstNode) node).getName(),
                        new BranchCallback() {

                            public void branch(IR_Scope m) {
                                m.pushString("constant");
                            }
                        },
                        new BranchCallback() {

                            public void branch(IR_Scope m) {
                                m.pushNull();
                            }
                        });
                break;
            case FCALLNODE:
                m.loadSelf();
                m.isMethodBound(((FCallNode) node).getName(),
                        new BranchCallback() {

                            public void branch(IR_Scope m) {
                                buildGetArgumentDefinition(((FCallNode) node).getArgsNode(), m, "method");
                            }
                        },
                        new BranchCallback() {

                            public void branch(IR_Scope m) {
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

                                public void branch(IR_Scope m) {
                                    if (iVisited instanceof Colon2Node) {
                                        final Node leftNode = ((Colon2Node) iVisited).getLeftNode();
                                        build(leftNode, m,true);
                                    } else {
                                        m.loadObject();
                                    }
                                }
                            };
                    BranchCallback isConstant = new BranchCallback() {

                                public void branch(IR_Scope m) {
                                    m.pushString("constant");
                                }
                            };
                    BranchCallback isMethod = new BranchCallback() {

                                public void branch(IR_Scope m) {
                                    m.pushString("method");
                                }
                            };
                    BranchCallback none = new BranchCallback() {

                                public void branch(IR_Scope m) {
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

                                public void branch(IR_Scope m) {
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

                                                public void branch(IR_Scope m) {
                                                    buildGetArgumentDefinition(iVisited.getArgsNode(), m, "method");
                                                }
                                            },
                                            new BranchCallback() {

                                                public void branch(IR_Scope m) {
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

                                public void branch(IR_Scope m) {
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

                                public void branch(IR_Scope m) {
                                    m.consumeCurrentValue();
                                    m.pushString("class variable");
                                    m.go(ending);
                                }
                            },
                            new BranchCallback() {

                                public void branch(IR_Scope m) {
                                }
                            });
                    m.setEnding(second);  //[RubyClass]
                    m.duplicateCurrentValue();
                    m.isClassVarDefined(iVisited.getName(),
                            new BranchCallback() {

                                public void branch(IR_Scope m) {
                                    m.consumeCurrentValue();
                                    m.pushString("class variable");
                                    m.go(ending);
                                }
                            },
                            new BranchCallback() {

                                public void branch(IR_Scope m) {
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

                                public void branch(IR_Scope m) {
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

                                                public void branch(IR_Scope m) {
                                                    buildGetArgumentDefinition(iVisited.getArgsNode(), m, "assignment");
                                                }
                                            },
                                            new BranchCallback() {

                                                public void branch(IR_Scope m) {
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

                                public void branch(IR_Scope m) {
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

                            public void branch(IR_Scope m) {
                                build(node, m,true);
                                m.consumeCurrentValue();
                                m.pushNull();
                            }
                        }, JumpException.class,
                        new BranchCallback() {

                            public void branch(IR_Scope m) {
                                m.pushNull();
                            }
                        }, String.class);
                m.consumeCurrentValue();
                m.pushString("expression");
        }
    }

    public Operand buildDAsgn(Node node, IR_Scope m) {
        final DAsgnNode dasgnNode = (DAsgnNode) node;

        CompilerCallback value = new CompilerCallback() {
            public void call(IR_Scope m) {
                build(dasgnNode.getValueNode(), m, true);
            }
        };
        
        m.getVariableCompiler().assignLocalVariable(dasgnNode.getIndex(), dasgnNode.getDepth(), value);
    }

    public Operand buildDAsgnAssignment(Node node, IR_Scope m) {
        DAsgnNode dasgnNode = (DAsgnNode) node;

        m.getVariableCompiler().assignLocalVariable(dasgnNode.getIndex(), dasgnNode.getDepth());
    }

    public Operand buildDefn(Node node, IR_Scope s) {
        final DefnNode defnNode = (DefnNode) node;
        IR_Method m = new IR_Method(s, defnNode.getName());

            // Build IR for args
        buildArgs(defnNode.getArgsNode(), m);

            // Build IR for body
        if (defnNode.getBodyNode() != null) {
                // if root of method is rescue, build as a light rescue
            if (defnNode.getBodyNode() instanceof RescueNode)
                buildRescueInternal(defnNode.getBodyNode(), m, true);
            else
                build(defnNode.getBodyNode(), m);
        } else {
           m.addInstr(new RETURN_Instr(Nil.NIL));
        }

            // No value returned for a method definition 
            // SSS FIXME: Verify from the ruby spec that this is true
        return null;
    }

    public Operand buildDefs(Node node, IR_Scope m) {
        final DefsNode defsNode = (DefsNode) node;
        final ArgsNode argsNode = defsNode.getArgsNode();

        CompilerCallback receiver = new CompilerCallback() {

                    public void call(IR_Scope m) {
                        build(defsNode.getReceiverNode(), m, true);
                    }
                };

        CompilerCallback body = new CompilerCallback() {

                    public void call(IR_Scope m) {
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

                    public void call(IR_Scope m) {
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

    public Operand buildArgs(Node node, IR_Scope s) {
        final IR_Method m = (IR_Method)s;
        final ArgsNode argsNode = (ArgsNode)node;
        final int required = argsNode.getRequiredArgsCount();
        final int opt = argsNode.getOptionalArgsCount();
        final int rest = argsNode.getRestArg();

          // TODO: Add IR instructions for checking method arity!
        // m.getVariableCompiler().checkMethodArity(required, opt, rest);

            // self = args[0]
            // SSS FIXME: Verify that this is correct
        m.addInstr(new RECV_ARG_Instr(m.getSelf(), new Constant(0)));

            // Other args begin at index 1
        int argIndex = 1;

            // Both for fixed arity and variable arity methods
        ListNode preArgs  = argsNode.getPre();
        for (int i = 0; i < m.numRequiredArgs(); i++, argIndex++) {
            ArgumentNode a = (ArgumentNode)preArgs.get(i);
            m.addInstr(new RECV_ARG_Instr(new Variable(a.getName()), new Constant(argIndex)));
        }

        if (opt > 0 || rest > -1) {
            ListNode optArgs = argsNode.getOptArgs();
            for (j = 0; j < opt; j++, argIndex++) {
                    // Jump to 'l' if this arg is not null.  If null, fall through and build the default value!
                Label l = m.getNewLabel();
                LoclAsgnNode n = optArgs.get(j);
                m.addInstr(new RECV_OPT_ARG_Instr(new Variable(n.getName()), new Constant(argIndex), l));
                build(n, m, true);
                m.addInstr(new LABEL_Instr(l));
            }

            if (rest > -1) {
                m.addInstr(new RECV_ARG_Instr(new Variable(argsNode.getRestArgNode().getName()), new Constant(argIndex)));
                argIndex++;
            }
        }

        // FIXME: Ruby 1.9 post args code needs to come here

        if (argsNode.getBlock() != null)
            m.addInstr(new RECV_ARG_Instr(argsNode.getBlockNode().getName(), new Constant(argIndex)));

        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();

            // This is not an expression that computes anything
        return null;
    }

    public Operand buildDot(Node node, IR_Scope s) {
        final DotNode dotNode = (DotNode) node;
        return new Range(build(dotNode.getBeginNode(), s), build(dotNode.getEndNode(), s));
    }

    public Operand buildDRegexp(Node node, IR_Scope m) {
        final DRegexpNode dregexpNode = (DRegexpNode) node;

        CompilerCallback createStringCallback = new CompilerCallback() {

                    public void call(IR_Scope m) {
                        ArrayCallback dstrCallback = new ArrayCallback() {

                                    public void nextValue(IR_Scope m, Object sourceArray,
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

    public Operand buildDStr(Node node, IR_Scope s) {
        final DStrNode dstrNode = (DStrNode) node;
        List<Operand> strPieces = new ArrayList<Operand>();
        for (Node n : dstrNode.childNodes()) {
            strPieces.add(build(n, s));

        return new CompoundString(strPieces);
    }

    public Operand buildDSymbol(Node node, IR_Scope s) {
        List<Operand> strPieces = new ArrayList<Operand>();
        for (Node n : node.childNodes()) {
            strPieces.add(build(n, s));

        return new DynamicSymbol(new CompoundString(strPieces));
    }

    public Operand buildDVar(Node node, IR_Scope m) {
        DVarNode dvarNode = (DVarNode) node;

        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) m.getVariableCompiler().retrieveLocalVariable(dvarNode.getIndex(), dvarNode.getDepth());
        } else {
            m.getVariableCompiler().retrieveLocalVariable(dvarNode.getIndex(), dvarNode.getDepth());
            if (!expr) m.consumeCurrentValue();
        }
    }

    public Operand buildDXStr(Node node, IR_Scope m) {
        final DXStrNode dstrNode = (DXStrNode) node;
        List<Operand> strPieces = new ArrayList<Operand>();
        for (Node nextNode : dstrNode.childNodes()) {
            strPieces.add(build(nextNode, m));

        return new BacktickString(strPieces);
    }

    public Operand buildEnsureNode(Node node, IR_Scope m) {
        final EnsureNode ensureNode = (EnsureNode) node;

        if (ensureNode.getEnsureNode() != null) {
            m.performEnsure(new BranchCallback() {

                        public void branch(IR_Scope m) {
                            if (ensureNode.getBodyNode() != null) {
                                build(ensureNode.getBodyNode(), m, true);
                            } else {
                                m.loadNil();
                            }
                        }
                    },
                    new BranchCallback() {

                        public void branch(IR_Scope m) {
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

    public Operand buildEvStr(Node node, IR_Scope m) {
            // SSS: FIXME: Somewhere here, we need to record information the type of this operand as String
        return build(((EvStrNode) node).getBody(), s)
    }

    public Operand buildFalse(Node node, IR_Scope m) {
        m.addInstr(new THREAD_POLL_Instr());
        return BooleanLiteral.FALSE; 
    }

    public Operand buildFCall(Node node, IR_Scope s) {
        FCallNode     fcallNode    = (FCallNode)node;
        Node          callArgsNode = fcallNode.getArgsNode();
        List<Operand> args         = setupArgs(callArgsNode, s);
        Operand       block        = setupCallClosure(fcallNode.getIterNode(), s);
        Variable      callResult   = s.getNewTmpVariable();
        IR_Instr      callInstr    = new CALL_Instr(callResult, new MethAddr(fcallNode.getName()), args.toArray(new Operand[args.size()]), block);
        s.addInstr(callInstr);
        return callResult;
    }

    private Operand setupCallClosure(Node node, IR_Scope s) {
        if (node == null)
            return null;

        switch (node.getNodeType()) {
            case ITERNODE:
                build((IterNode)node, s, true);
                return new Operand(); //FIXME
            case BLOCKPASSNODE:
                build(((BlockPassNode)node).getBodyNode(), s, true);
                // FIXME: Translate this call below!
                s.unwrapPassedBlock();
                return new Operand(); //FIXME
            default:
                throw new NotCompilableException("ERROR: Encountered a method with a non-block, non-blockpass iter node at: " + node);
        }
    }

    public Operand buildFixnum(Node node, IR_Scope m) {
        return new Fixnum(((FixnumNode)node).getValue());
    }

    public Operand buildFlip(Node node, IR_Scope m) {
        final FlipNode flipNode = (FlipNode) node;

        m.getVariableCompiler().retrieveLocalVariable(flipNode.getIndex(), flipNode.getDepth());

        if (flipNode.isExclusive()) {
            m.performBooleanBranch(new BranchCallback() {

                public void branch(IR_Scope m) {
                    build(flipNode.getEndNode(), m,true);
                    m.performBooleanBranch(new BranchCallback() {

                        public void branch(IR_Scope m) {
                            m.loadFalse();
                            m.getVariableCompiler().assignLocalVariable(flipNode.getIndex(), flipNode.getDepth(), false);
                        }
                    }, new BranchCallback() {

                        public void branch(IR_Scope m) {
                        }
                    });
                    m.loadTrue();
                }
            }, new BranchCallback() {

                public void branch(IR_Scope m) {
                    build(flipNode.getBeginNode(), m,true);
                    becomeTrueOrFalse(m);
                    m.getVariableCompiler().assignLocalVariable(flipNode.getIndex(), flipNode.getDepth(), true);
                }
            });
        } else {
            m.performBooleanBranch(new BranchCallback() {

                public void branch(IR_Scope m) {
                    build(flipNode.getEndNode(), m,true);
                    m.performBooleanBranch(new BranchCallback() {

                        public void branch(IR_Scope m) {
                            m.loadFalse();
                            m.getVariableCompiler().assignLocalVariable(flipNode.getIndex(), flipNode.getDepth(), false);
                        }
                    }, new BranchCallback() {

                        public void branch(IR_Scope m) {
                        }
                    });
                    m.loadTrue();
                }
            }, new BranchCallback() {

                public void branch(IR_Scope m) {
                    build(flipNode.getBeginNode(), m,true);
                    m.performBooleanBranch(new BranchCallback() {

                        public void branch(IR_Scope m) {
                            build(flipNode.getEndNode(), m,true);
                            flipTrueOrFalse(m);
                            m.getVariableCompiler().assignLocalVariable(flipNode.getIndex(), flipNode.getDepth(), false);
                            m.loadTrue();
                        }
                    }, new BranchCallback() {

                        public void branch(IR_Scope m) {
                            m.loadFalse();
                        }
                    });
                }
            });
        }
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    private void becomeTrueOrFalse(IR_Scope m) {
        m.performBooleanBranch(new BranchCallback() {

                    public void branch(IR_Scope m) {
                        m.loadTrue();
                    }
                }, new BranchCallback() {

                    public void branch(IR_Scope m) {
                        m.loadFalse();
                    }
                });
    }

    private void flipTrueOrFalse(IR_Scope m) {
        m.performBooleanBranch(new BranchCallback() {

                    public void branch(IR_Scope m) {
                        m.loadFalse();
                    }
                }, new BranchCallback() {

                    public void branch(IR_Scope m) {
                        m.loadTrue();
                    }
                });
    }

    public Operand buildFloat(Node node, IR_Scope m) {
        return new Float(((FloatNode)node).getValue());
    }

    public Operand buildFor(Node node, IR_Scope m) {
        final ForNode forNode = (ForNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {

                    public void call(IR_Scope m) {
                        build(forNode.getIterNode(), m, true);
                    }
                };

        final CompilerCallback closureArg = new CompilerCallback() {

                    public void call(IR_Scope m) {
                        buildForIter(forNode, m);
                    }
                };

        m.getInvocationCompiler().invokeDynamic("each", receiverCallback, null, CallType.NORMAL, closureArg, true);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildForIter(Node node, IR_Scope m) {
        final ForNode forNode = (ForNode) node;

        // create the closure class and instantiate it
        final CompilerCallback closureBody = new CompilerCallback() {

                    public void call(IR_Scope m) {
                        if (forNode.getBodyNode() != null) {
                            build(forNode.getBodyNode(), m,true);
                        } else {
                            m.loadNil();
                        }
                    }
                };

        // create the closure class and instantiate it
        final CompilerCallback closureArgs = new CompilerCallback() {

                    public void call(IR_Scope m) {
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

    public Operand buildGlobalAsgn(Node node, IR_Scope m) {
        final GlobalAsgnNode globalAsgnNode = (GlobalAsgnNode) node;

        CompilerCallback value = new CompilerCallback() {
            public void call(IR_Scope m) {
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

    public Operand buildGlobalAsgnAssignment(Node node, IR_Scope m) {
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

    public Operand buildGlobalVar(Node node, IR_Scope m) {
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

    public Operand buildHash(Node node, IR_Scope m) {
        HashNode hashNode = (HashNode) node;
        if (hashNode.getListNode() == null || hashNode.getListNode().size() == 0) {
            return new Hash(null);
        }
        else {
            int     i     = 0;
            Operand key   = null;
            Operand value = null;
            List<KeyValuePair> args = new ArrayList<KeyValuePair>();
            for (Node nextNode : hashNode.getListNode().childNodes()) {
                Operand v = build(nextNode, m, false);
                if (key == null) {
                    key = v;
                }
                else {
                    args.add(new KeyValuePair(key, v));
                    key = null; 
                }
            }
            return new Hash(args);
        }
    }

    // Translate "r = if (cond); .. thenbody ..; else; .. elsebody ..; end" to
    //
    //     v = -- build(cond) --
    //     BEQ(v, FALSE, L1)
    //     r = -- build(thenbody) --
    //     jump L2
    // L1:
    //     r = -- build(elsebody) --
    // L2:
    //     --- r is the result of the if expression --
    //
    public Operand buildIf(Node node, IR_Scope s) {
        final IfNode ifNode = (IfNode) node;

        Node actualCondition = skipOverNewlines(ifNode.getCondition());

        // optimizations if we know ahead of time it will always be true or false
        if (actualCondition.getNodeType().alwaysTrue()) {
            build(actualCondition, s);
            return build(ifNode.getThenBody(), s);
        } else if (actualCondition.getNodeType().alwaysFalse()) {
            // always false or nil
            return build(ifNode.getElseBody(), s);
        } else {
            Variable result     = s.getNewTmpVariable();
            Label    falseLabel = s.getNewLabel();
            Label    doneLabel  = s.getNewLabel();
            s.addInstr(new BEQ_Instr(build(actualCondition, s), BooleanLiteral.FALSE, falseLabel));
            if (ifNode.getThenBody() != null)
                s.addInstr(new COPY_Instr(result, build(ifNode.getThenBody(), s)));
            else
                s.addInstr(new COPY_Instr(result, Nil.NIL));
            s.addInstr(new JUMP_Instr(doneLabel));
            s.addInstr(new LABEL_Instr(falseLabel));
            if (ifNode.getElseBody() != null)
                s.addInstr(new COPY_Instr(result, build(ifNode.getElseBody(), s)));
            else
                s.addInstr(new COPY_Instr(result, Nil.NIL));
            s.addInstr(new LABEL_Instr(doneLabel));
            return result;
        }
    }

    public Operand buildInstAsgn(Node node, IR_Scope m) {
        final InstAsgnNode instAsgnNode = (InstAsgnNode) node;

        CompilerCallback value = new CompilerCallback() {
            public void call(IR_Scope m) {
                build(instAsgnNode.getValueNode(), m, true);
            }
        };

        m.assignInstanceVariable(instAsgnNode.getName(), value);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildInstAsgnAssignment(Node node, IR_Scope m) {
        InstAsgnNode instAsgnNode = (InstAsgnNode) node;
        m.assignInstanceVariable(instAsgnNode.getName());
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildInstVar(Node node, IR_Scope m) {
        Variable ret = m.getNewTmpVariable();
        m.addInstr(new GET_FIELD_Instr(ret, m.getSelf(), ((InstrVarNode)node).getName()));
        return ret;
    }

    public Operand buildIter(Node node, IR_Scope m) {
            // Create a new closure context
        m = new IR_Closure(m);

        final IterNode iterNode = (IterNode) node;

        // create the closure class and instantiate it
        final CompilerCallback closureBody = new CompilerCallback() {

                    public void call(IR_Scope m) {
                        if (iterNode.getBodyNode() != null) {
                            build(iterNode.getBodyNode(), m, true);
                        } else {
                            m.loadNil();
                        }
                    }
                };

        // create the closure class and instantiate it
        final CompilerCallback closureArgs = new CompilerCallback() {

                    public void call(IR_Scope m) {
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

    public Operand buildLocalAsgn(Node node, IR_Scope s) {
        s.addIntsr(new COPY_Instr(new Variable(localAsgnNode.getName()), build(localAsgnNode.getValueNode(), s, true)));
/**
 * SSS FIXME: How does this PRAGMA business affect the IR? 
 *
        if (ASTInspector.PRAGMAS.contains(localAsgnNode.getName())) {
            if (expr) m.loadNil();
        }
**/
    }

    public Operand buildLocalAsgnAssignment(Node node, IR_Scope m) {
        // "assignment" means the value is already on the stack
        LocalAsgnNode localAsgnNode = (LocalAsgnNode) node;

        m.getVariableCompiler().assignLocalVariable(localAsgnNode.getIndex(), localAsgnNode.getDepth());
    }

    public Operand buildLocalVar(Node node, IR_Scope contexs) {
        return new Variable(((LocalVarNode) node).getName());
    }

    public Operand buildMatch(Node node, IR_Scope m) {
        MatchNode matchNode = (MatchNode) node;

        build(matchNode.getRegexpNode(), m,true);

        m.match();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildMatch2(Node node, IR_Scope m) {
        final Match2Node matchNode = (Match2Node) node;

        build(matchNode.getReceiverNode(), m,true);
        CompilerCallback value = new CompilerCallback() {
            public void call(IR_Scope m) {
                build(matchNode.getValueNode(), m,true);
            }
        };

        m.match2(value);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildMatch3(Node node, IR_Scope m) {
        Match3Node matchNode = (Match3Node) node;

        build(matchNode.getReceiverNode(), m,true);
        build(matchNode.getValueNode(), m,true);

        m.match3();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildModule(Node node, IR_Scope m) {
        final ModuleNode moduleNode = (ModuleNode) node;

        final Node cpathNode = moduleNode.getCPath();

        CompilerCallback bodyCallback = new CompilerCallback() {

                    public void call(IR_Scope m) {
                        if (moduleNode.getBodyNode() != null) {
                            build(moduleNode.getBodyNode(), m,true);
                        }
                        m.loadNil();
                    }
                };

        CompilerCallback pathCallback = new CompilerCallback() {

                    public void call(IR_Scope m) {
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

    public Operand buildMultipleAsgn(Node node, IR_Scope m) {
        MultipleAsgnNode multipleAsgnNode = (MultipleAsgnNode) node;

        if (expr) {
            // need the array, use unoptz version
            buildUnoptimizedMultipleAsgn(multipleAsgnNode, m);
        } else {
            // try optz version
            buildOptimizedMultipleAsgn(multipleAsgnNode, m);
        }
    }

    private void buildOptimizedMultipleAsgn(MultipleAsgnNode multipleAsgnNode, IR_Scope m) {
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
        buildUnoptimizedMultipleAsgn(multipleAsgnNode, m);
    }

    private void buildUnoptimizedMultipleAsgn(MultipleAsgnNode multipleAsgnNode, IR_Scope m) {
        build(multipleAsgnNode.getValueNode(), m, true);

        buildMultipleAsgnAssignment(multipleAsgnNode, m);
    }

    public Operand buildMultipleAsgnAssignment(Node node, IR_Scope m) {
        final MultipleAsgnNode multipleAsgnNode = (MultipleAsgnNode) node;

        // normal items at the "head" of the masgn
        ArrayCallback headAssignCallback = new ArrayCallback() {

                    public void nextValue(IR_Scope m, Object sourceArray,
                            int index) {
                        ListNode headNode = (ListNode) sourceArray;
                        Node assignNode = headNode.get(index);

                        // perform assignment for the next node
                        buildAssignment(assignNode, m, false);
                    }
                };

        CompilerCallback argsCallback = new CompilerCallback() {

                    public void call(IR_Scope m) {
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

    public Operand buildNewline(Node node, IR_Scope s) {
        // SSS FIXME: We need to build debug information tracking into the IR in some fashion
        // So, these methods below would have to have equivalents in IR_Scope implementations.
        s.lineNumber(node.getPosition());
        s.setLinePosition(node.getPosition());

        return build(((NewlineNode)node).getNextNode(), s);
    }

    public Operand buildNext(Node node, IR_Scope m) {
        final NextNode nextNode = (NextNode) node;

        CompilerCallback valueCallback = new CompilerCallback() {

                    public void call(IR_Scope m) {
                        if (nextNode.getValueNode() != null) {
                            build(nextNode.getValueNode(), m,true);
                        } else {
                            m.loadNil();
                        }
                    }
                };

        m.addInstr(new THREAD_POLL_Instr());
        m.issueNextEvent(valueCallback);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildNthRef(Node node, IR_Scope m) {
        NthRefNode nthRefNode = (NthRefNode) node;

        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) m.nthRef(nthRefNode.getMatchNumber());
        } else {
            m.nthRef(nthRefNode.getMatchNumber());
            if (!expr) m.consumeCurrentValue();
        }
    }

    public Operand buildNil(Node node, IR_Scope m) {
        m.addInstr(new THREAD_POLL_Instr());
        return Nil.NIL;
    }

    public Operand buildNot(Node node, IR_Scope m) {
        Variable ret = m.getNewTmpVariable();
        m.addInstr(new ALU_Instr(NOT, dst, build(((NotNode)node).getConditionNode(), m, true)));
        return ret;
    }

    public Operand buildOpAsgnAnd(Node node, IR_Scope m) {
        final BinaryOperatorNode andNode = (BinaryOperatorNode) node;

        build(andNode.getFirstNode(), m,true);

        BranchCallback longCallback = new BranchCallback() {

                    public void branch(IR_Scope m) {
                        build(andNode.getSecondNode(), m,true);
                    }
                };

        m.performLogicalAnd(longCallback);
        m.addInstr(new THREAD_POLL_Instr());
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildOpAsgnOr(Node node, IR_Scope m) {
        final OpAsgnOrNode orNode = (OpAsgnOrNode) node;

        if (needsDefinitionCheck(orNode.getFirstNode())) {
            buildGetDefinitionBase(orNode.getFirstNode(), m);

            m.isNull(new BranchCallback() {

                        public void branch(IR_Scope m) {
                            build(orNode.getSecondNode(), m,true);
                        }
                    }, new BranchCallback() {

                        public void branch(IR_Scope m) {
                            build(orNode.getFirstNode(), m,true);
                            m.duplicateCurrentValue();
                            m.performBooleanBranch(new BranchCallback() {

                                        public void branch(IR_Scope m) {
                                        //Do nothing
                                        }
                                    },
                                    new BranchCallback() {

                                        public void branch(IR_Scope m) {
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
                public void branch(IR_Scope m) {
                //Do nothing
                }
            },
            new BranchCallback() {
                public void branch(IR_Scope m) {
                    m.consumeCurrentValue();
                    build(orNode.getSecondNode(), m,true);
                }
            });

        }

        m.addInstr(new THREAD_POLL_Instr());
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

    public Operand buildOpAsgn(Node node, IR_Scope m) {
        final OpAsgnNode opAsgnNode = (OpAsgnNode) node;

        if (opAsgnNode.getOperatorName().equals("||")) {
            buildOpAsgnWithOr(opAsgnNode, m, true);
        } else if (opAsgnNode.getOperatorName().equals("&&")) {
            buildOpAsgnWithAnd(opAsgnNode, m, true);
        } else {
            buildOpAsgnWithMethod(opAsgnNode, m, true);
        }

        m.addInstr(new THREAD_POLL_Instr());
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildOpAsgnWithOr(Node node, IR_Scope m) {
        final OpAsgnNode opAsgnNode = (OpAsgnNode) node;

        final CompilerCallback receiverCallback = new CompilerCallback() {

            public void call(IR_Scope m) {
                build(opAsgnNode.getReceiverNode(), m, true); // [recv]
            }
        };
        
        ArgumentsCallback argsCallback = setupArgs(opAsgnNode.getValueNode());
        
        m.getInvocationCompiler().invokeOpAsgnWithOr(opAsgnNode.getVariableName(), opAsgnNode.getVariableNameAsgn(), receiverCallback, argsCallback);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildOpAsgnWithAnd(Node node, IR_Scope m) {
        final OpAsgnNode opAsgnNode = (OpAsgnNode) node;

        final CompilerCallback receiverCallback = new CompilerCallback() {

            public void call(IR_Scope m) {
                build(opAsgnNode.getReceiverNode(), m, true); // [recv]
            }
        };
        
        ArgumentsCallback argsCallback = setupArgs(opAsgnNode.getValueNode());
        
        m.getInvocationCompiler().invokeOpAsgnWithAnd(opAsgnNode.getVariableName(), opAsgnNode.getVariableNameAsgn(), receiverCallback, argsCallback);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildOpAsgnWithMethod(Node node, IR_Scope m) {
        final OpAsgnNode opAsgnNode = (OpAsgnNode) node;

        final CompilerCallback receiverCallback = new CompilerCallback() {
                    public void call(IR_Scope m) {
                        build(opAsgnNode.getReceiverNode(), m, true); // [recv]
                    }
                };

        // eval new value, call operator on old value, and assign
        ArgumentsCallback argsCallback = new ArgumentsCallback() {
            public int getArity() {
                return 1;
            }

            public void call(IR_Scope m) {
                build(opAsgnNode.getValueNode(), m, true);
            }
        };
        
        m.getInvocationCompiler().invokeOpAsgnWithMethod(opAsgnNode.getOperatorName(), opAsgnNode.getVariableName(), opAsgnNode.getVariableNameAsgn(), receiverCallback, argsCallback);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildOpElementAsgn(Node node, IR_Scope m) {
        final OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode) node;
        
        if (opElementAsgnNode.getOperatorName() == "||") {
            buildOpElementAsgnWithOr(node, m);
        } else if (opElementAsgnNode.getOperatorName() == "&&") {
            buildOpElementAsgnWithAnd(node, m);
        } else {
            buildOpElementAsgnWithMethod(node, m);
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

        public void call(IR_Scope m) {
            if (getArity() == 1) {
                // if arity 1, just build the one element to save us the array cost
                build(((ArrayNode)node).get(0), m,true);
            } else {
                // build into array
                buildArguments(node, m);
            }
        }
    };

    public Operand buildOpElementAsgnWithOr(Node node, IR_Scope m) {
        final OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {
            public void call(IR_Scope m) {
                build(opElementAsgnNode.getReceiverNode(), m, true);
            }
        };

        ArgumentsCallback argsCallback = new OpElementAsgnArgumentsCallback(opElementAsgnNode.getArgsNode());

        CompilerCallback valueCallback = new CompilerCallback() {
            public void call(IR_Scope m) {
                build(opElementAsgnNode.getValueNode(), m, true);
            }
        };

        m.getInvocationCompiler().opElementAsgnWithOr(receiverCallback, argsCallback, valueCallback);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildOpElementAsgnWithAnd(Node node, IR_Scope m) {
        final OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {
            public void call(IR_Scope m) {
                build(opElementAsgnNode.getReceiverNode(), m, true);
            }
        };

        ArgumentsCallback argsCallback = new OpElementAsgnArgumentsCallback(opElementAsgnNode.getArgsNode()); 

        CompilerCallback valueCallback = new CompilerCallback() {
            public void call(IR_Scope m) {
                build(opElementAsgnNode.getValueNode(), m, true);
            }
        };

        m.getInvocationCompiler().opElementAsgnWithAnd(receiverCallback, argsCallback, valueCallback);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildOpElementAsgnWithMethod(Node node, IR_Scope m) {
        final OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode) node;

        CompilerCallback receiverCallback = new CompilerCallback() {
            public void call(IR_Scope m) {
                build(opElementAsgnNode.getReceiverNode(), m,true);
            }
        };

        ArgumentsCallback argsCallback = setupArgs(opElementAsgnNode.getArgsNode());

        CompilerCallback valueCallback = new CompilerCallback() {
            public void call(IR_Scope m) {
                build(opElementAsgnNode.getValueNode(), m,true);
            }
        };

        m.getInvocationCompiler().opElementAsgnWithMethod(receiverCallback, argsCallback, valueCallback, opElementAsgnNode.getOperatorName());
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    // Translate ret = (a || b) to ret = (a ? true : b) as follows
    // 
    //    v1 = -- build(a) --
    //       OPT: ret can be set to v1, but effectively v1 is true if we take the branch to L.
    //            while this info can be inferred by using attributes, why bother if we can do this?
    //    ret = true
    //    beq(v1, true, L)
    //    v2 = -- build(b) --
    //    ret = v2
    // L:
    //
    public Operand buildOr(Node node, IR_Scope m) {
        final OrNode orNode = (OrNode) node;

        if (orNode.getFirstNode().getNodeType().alwaysTrue()) {
            // build first node only and return true
            build(orNode.getFirstNode(), m);
            return BooleanLiteral.TRUE;
        } else if (orNode.getFirstNode().getNodeType().alwaysFalse()) {
            // build first node as non-expr and build second node
            build(orNode.getFirstNode(), m);
            return build(orNode.getSecondNode(), m);
        } else {
            Variable ret = m.getNewTmpVariable();
            Label    l   = m.getNewLabel();
            Operand  v1  = build(orNode.getFirstNode(), m);
            m.addInstr(new COPY_Instr(ret, BooleanLiteral.TRUE));
            m.addInstr(new BEQ_Instr(v1, BooleanLiteral.TRUE, l));
            Operand  v2  = build(orNode.getSecondNode(), m);
            m.addInstr(new COPY_Instr(ret, v2);
            m.addInstr(new LABEL_Instr(l));
            return ret;
        }
    }

    public Operand buildPostExe(Node node, IR_Scope m) {
        final PostExeNode postExeNode = (PostExeNode) node;

        // create the closure class and instantiate it
        final CompilerCallback closureBody = new CompilerCallback() {

                    public void call(IR_Scope m) {
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

    public Operand buildPreExe(Node node, IR_Scope m) {
        final PreExeNode preExeNode = (PreExeNode) node;

        // create the closure class and instantiate it
        final CompilerCallback closureBody = new CompilerCallback() {

                    public void call(IR_Scope m) {
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

    public Operand buildRedo(Node node, IR_Scope m) {
        //RedoNode redoNode = (RedoNode)node;

        m.issueRedoEvent();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildRegexp(Node node, IR_Scope m) {
        RegexpNode reNode = (RegexpNode) node;

        if (RubyInstanceConfig.PEEPHOLE_OPTZ) {
            if (expr) m.createNewRegexp(reNode.getValue(), reNode.getOptions());
        } else {
            m.createNewRegexp(reNode.getValue(), reNode.getOptions());
            if (!expr) m.consumeCurrentValue();
        }
    }

    public Operand buildRescue(Node node, IR_Scope m) {
        buildRescueInternal(node, m, false);
        
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    private void buildRescueInternal(Node node, IR_Scope m, final boolean light) {
        final RescueNode rescueNode = (RescueNode) node;

        BranchCallback body = new BranchCallback() {
            public void branch(IR_Scope m) {
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
            public void branch(IR_Scope m) {
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

    private void buildRescueBodyInternal(Node node, IR_Scope m, final boolean light) {
        final RescueBodyNode rescueBodyNode = (RescueBodyNode) node;

        m.loadException();

        final Node exceptionList = rescueBodyNode.getExceptionNodes();
        ArgumentsCallback rescueArgs = setupArgs(exceptionList);
        if (rescueArgs == null) rescueArgs = new ArgumentsCallback() {
            public int getArity() {
                return 1;
            }

            public void call(IR_Scope m) {
                m.loadStandardError();
            }
        };

        m.checkIsExceptionHandled(rescueArgs);

        BranchCallback trueBranch = new BranchCallback() {
            public void branch(IR_Scope m) {
                // check if it's an immediate, and don't outline
                Node realBody = rescueBodyNode.getBodyNode();
                if (realBody instanceof NewlineNode) {
                    m.setLinePosition(realBody.getPosition());
                    realBody = IR_Builder.skipOverNewlines(realBody);
                }

                if (realBody.getNodeType().isImmediate()) {
                    build(realBody, m, true);
                    m.clearErrorInfo();
                } else {
                    m.storeExceptionInErrorInfo();
                    if (light) {
                        build(rescueBodyNode.getBodyNode(), m, true);
                    } else {
                        IR_Scope nestedBody = m.outline("rescue_line_" + rescueBodyNode.getPosition().getStartLine());
                        build(rescueBodyNode.getBodyNode(), nestedBody, true);
                        nestedBody.endBody();
                    }

                    // FIXME: this should reset to what it was before
                    m.clearErrorInfo();
                }
            }
        };

        BranchCallback falseBranch = new BranchCallback() {
            public void branch(IR_Scope m) {
                if (rescueBodyNode.getOptRescueNode() != null) {
                    buildRescueBodyInternal(rescueBodyNode.getOptRescueNode(), m, light);
                } else {
                    m.rethrowException();
                }
            }
        };

        m.performBooleanBranch(trueBranch, falseBranch);
    }

    public Operand buildRetry(Node node, IR_Scope m) {
        m.addInstr(new THREAD_POLL_Instr());

        m.issueRetryEvent();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildReturn(Node node, IR_Scope m) {
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

    public Operand buildRoot(Node node, ScriptCompiler m, ASTInspector inspector) {
        buildRoot(node, m, inspector, true, true);
    }

    public Operand buildRoot(Node node, ScriptCompiler m, ASTInspector inspector, boolean load, boolean main) {
        RootNode rootNode = (RootNode) node;
        StaticScope staticScope = rootNode.getStaticScope();

        m.startScript(staticScope);

        // force static scope to claim restarg at 0, so it only implements the [] version of __file__
        staticScope.setRestArg(-2);

        // create method for toplevel of script
        IR_Scope methodCompiler = m.startRoot("__file__", "__file__", staticScope, inspector);

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

    public Operand buildSelf(Node node, IR_Scope m) {
        return m.getSelf();
    }

    public Operand buildSplat(Node node, IR_Scope m) {
        SplatNode splatNode = (SplatNode) node;

        build(splatNode.getValue(), m, true);

        m.splatCurrentValue();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildStr(Node node, IR_Scope s) {
        StrNode strNode = (StrNode) node;
        return (strNode instanceof FileNode) ? s.getFileName() : new StringLiteral(strNode.getValue());
    }

    public Operand buildSuper(Node node, IR_Scope m) {
        final SuperNode superNode = (SuperNode) node;

        ArgumentsCallback argsCallback = setupArgs(superNode.getArgsNode());

        CompilerCallback closureArg = setupCallClosure(superNode.getIterNode());

        m.getInvocationCompiler().invokeDynamic(null, null, argsCallback, CallType.SUPER, closureArg, superNode.getIterNode() instanceof IterNode);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildSValue(Node node, IR_Scope m) {
        SValueNode svalueNode = (SValueNode) node;

        build(svalueNode.getValue(), m,true);

        m.singlifySplattedValue();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildSymbol(Node node, IR_Scope m) {
        return new Symbol(((SymbolNode) node).getName());
    }    
    
    public Operand buildToAry(Node node, IR_Scope m) {
        ToAryNode toAryNode = (ToAryNode) node;

        build(toAryNode.getValue(), m,true);

        m.aryToAry();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildTrue(Node node, IR_Scope m) {
        m.addInstr(new THREAD_POLL_Instr());
        return BooleanLiteral.TRUE; 
    }

    public Operand buildUndef(Node node, IR_Scope m) {
        m.undefMethod(((UndefNode) node).getName());
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildUntil(Node node, IR_Scope m) {
        final UntilNode untilNode = (UntilNode) node;

        if (untilNode.getConditionNode().getNodeType().alwaysTrue() &&
                untilNode.evaluateAtStart()) {
            // condition is always true, just build it and not body
            build(untilNode.getConditionNode(), m, false);
            if (expr) m.loadNil();
        } else {
            BranchCallback condition = new BranchCallback() {

                public void branch(IR_Scope m) {
                    build(untilNode.getConditionNode(), m, true);
                    m.negateCurrentValue();
                }
            };

            BranchCallback body = new BranchCallback() {

                public void branch(IR_Scope m) {
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

            m.addInstr(new THREAD_POLL_Instr());
            // TODO: don't require pop
            if (!expr) m.consumeCurrentValue();
        }
    }

    public Operand buildVAlias(Node node, IR_Scope m) {
        VAliasNode valiasNode = (VAliasNode) node;

        m.aliasGlobal(valiasNode.getNewName(), valiasNode.getOldName());
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildVCall(Node node, IR_Scope s) {
        List<Operand> args       = new ArrayList<Operand>(); args.add(s.getSelf());
        Variable      callResult = s.getNewTmpVariable();
        IR_Instr      callInstr  = new CALL_Instr(callResult, new MethAddr(callNode.getName()), args.toArray(new Operand[args.size()]), block);
        s.addInstr(callInstr);
        return callResult;
    }

    public Operand buildWhile(Node node, IR_Scope m) {
        final WhileNode whileNode = (WhileNode) node;

        if (whileNode.getConditionNode().getNodeType().alwaysFalse() && whileNode.evaluateAtStart()) {
            return Nil.NIL;
        } else {
            BranchCallback condition = new BranchCallback() {

                public void branch(IR_Scope m) {
                    build(whileNode.getConditionNode(), m, true);
                }
            };

            BranchCallback body = new BranchCallback() {

                public void branch(IR_Scope m) {
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

            m.addInstr(new THREAD_POLL_Instr());
        }
    }

    public Operand buildXStr(Node node, IR_Scope m) {
        return new BacktickString(((XStrNode)node).getValue());
    }

    public Operand buildYield(Node node, IR_Scope m) {
        final YieldNode yieldNode = (YieldNode) node;

        ArgumentsCallback argsCallback = setupArgs(yieldNode.getArgsNode());

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
                    public void call(IR_Scope m) {
                        build(yieldNode.getArgsNode(), m,true);
                    }
                };
            }

            m.getInvocationCompiler().yield(argsCallback2, yieldNode.getExpandArguments());
        }
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public Operand buildZArray(Node node, IR_Scope m) {
        boolean doit = expr || !RubyInstanceConfig.PEEPHOLE_OPTZ;
        boolean popit = !RubyInstanceConfig.PEEPHOLE_OPTZ && !expr;

        if (doit) {
            m.createEmptyArray();
        }

        if (popit) m.consumeCurrentValue();
    }

    public Operand buildZSuper(Node node, IR_Scope m) {
        ZSuperNode zsuperNode = (ZSuperNode) node;

        CompilerCallback closure = setupCallClosure(zsuperNode.getIterNode());

        m.callZSuper(closure);
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildArgsCatArguments(List<Operand> args, Node node, IR_Scope m) {
        ArgsCatNode argsCatNode = (ArgsCatNode) node;

        buildArguments(args, argsCatNode.getFirstNode(), m);
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

    public void buildArgsPushArguments(List<Operand> args, Node node, IR_Scope m) {
        ArgsPushNode argsPushNode = (ArgsPushNode) node;
        build(argsPushNode.getFirstNode(), m,true);
        build(argsPushNode.getSecondNode(), m,true);
        m.appendToArray();
        m.convertToJavaArray();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    public void buildArrayArguments(List<Operand> args, Node node, IR_Scope m) {
        ArrayNode arrayNode = (ArrayNode) node;

        ArrayCallback callback = new ArrayCallback() {

                    public void nextValue(IR_Scope m, Object sourceArray, int index) {
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

    public void buildSplatArguments(List<Operand> args, Node node, IR_Scope m) {
        SplatNode splatNode = (SplatNode) node;

        build(splatNode.getValue(), m,true);
        m.splatCurrentValue();
        m.convertToJavaArray();
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }
}
