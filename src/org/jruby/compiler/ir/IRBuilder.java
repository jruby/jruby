package org.jruby.compiler.ir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.Map;
import java.util.Date;

import org.jruby.Ruby;
import org.jruby.ast.AliasNode;
import org.jruby.ast.AndNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArgumentNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.AttrAssignNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BignumNode;
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
import org.jruby.ast.DotNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.EnsureNode;
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
import org.jruby.ast.MethodDefNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.Node;
import org.jruby.ast.NodeType;
import org.jruby.ast.NotNode;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.OpAsgnAndNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OrNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.RootNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SValueNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SuperNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.UndefNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ArgsCatNode;
import org.jruby.ast.ArgsPushNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.CaseNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.Colon2ConstNode;
import org.jruby.ast.Colon2MethodNode;
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.DXStrNode;
import org.jruby.ast.DefsNode;
import org.jruby.ast.ForNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.StarNode;
import org.jruby.ast.ToAryNode;
import org.jruby.ast.TypedArgumentNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.WhenNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.ir.instructions.AttrAssignInstr;
import org.jruby.compiler.ir.instructions.BEQInstr;
import org.jruby.compiler.ir.instructions.BREAK_Instr;
import org.jruby.compiler.ir.instructions.CallInstr;
import org.jruby.compiler.ir.instructions.CaseInstr;
import org.jruby.compiler.ir.instructions.ClosureReturnInstr;
import org.jruby.compiler.ir.instructions.CopyInstr;
import org.jruby.compiler.ir.instructions.DECLARE_LOCAL_TYPE_Instr;
import org.jruby.compiler.ir.instructions.EQQ_Instr;
import org.jruby.compiler.ir.instructions.FilenameInstr;
import org.jruby.compiler.ir.instructions.GetArrayInstr;
import org.jruby.compiler.ir.instructions.GetConstInstr;
import org.jruby.compiler.ir.instructions.GetClassVariableInstr;
import org.jruby.compiler.ir.instructions.GetFieldInstr;
import org.jruby.compiler.ir.instructions.GetGlobalVariableInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.IsTrueInstr;
import org.jruby.compiler.ir.instructions.JRubyImplCallInstr;
import org.jruby.compiler.ir.instructions.JumpInstr;
import org.jruby.compiler.ir.instructions.JUMP_INDIRECT_Instr;
import org.jruby.compiler.ir.instructions.LABEL_Instr;
import org.jruby.compiler.ir.instructions.LineNumberInstr;
import org.jruby.compiler.ir.instructions.NotInstr;
import org.jruby.compiler.ir.instructions.PutConstInstr;
import org.jruby.compiler.ir.instructions.PutClassVariableInstr;
import org.jruby.compiler.ir.instructions.PutFieldInstr;
import org.jruby.compiler.ir.instructions.PutGlobalVarInstr;
import org.jruby.compiler.ir.instructions.ReceiveSelfInstruction;
import org.jruby.compiler.ir.instructions.ReceiveArgumentInstruction;
import org.jruby.compiler.ir.instructions.ReceiveClosureArgInstr;
import org.jruby.compiler.ir.instructions.ReceiveClosureInstr;
import org.jruby.compiler.ir.instructions.RECV_EXCEPTION_Instr;
import org.jruby.compiler.ir.instructions.ReceiveOptionalArgumentInstr;
import org.jruby.compiler.ir.instructions.RESCUED_BODY_START_MARKER_Instr;
import org.jruby.compiler.ir.instructions.RESCUED_BODY_END_MARKER_Instr;
import org.jruby.compiler.ir.instructions.ReturnInstr;
import org.jruby.compiler.ir.instructions.RubyInternalCallInstr;
import org.jruby.compiler.ir.instructions.SET_RETADDR_Instr;
import org.jruby.compiler.ir.instructions.ThreadPollInstr;
import org.jruby.compiler.ir.instructions.THROW_EXCEPTION_Instr;
import org.jruby.compiler.ir.instructions.YieldInstr;
import org.jruby.compiler.ir.operands.Array;
import org.jruby.compiler.ir.operands.Backref;
import org.jruby.compiler.ir.operands.BacktickString;
import org.jruby.compiler.ir.operands.BooleanLiteral;
import org.jruby.compiler.ir.operands.BreakResult;
import org.jruby.compiler.ir.operands.ClassMetaObject;
import org.jruby.compiler.ir.operands.ClosureMetaObject;
import org.jruby.compiler.ir.operands.CompoundArray;
import org.jruby.compiler.ir.operands.CompoundString;
import org.jruby.compiler.ir.operands.DynamicSymbol;
import org.jruby.compiler.ir.operands.Fixnum;
import org.jruby.compiler.ir.operands.Float;
import org.jruby.compiler.ir.operands.Hash;
import org.jruby.compiler.ir.operands.KeyValuePair;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.ModuleMetaObject;
import org.jruby.compiler.ir.operands.Nil;
import org.jruby.compiler.ir.operands.NthRef;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Range;
import org.jruby.compiler.ir.operands.Regexp;
import org.jruby.compiler.ir.operands.SValue;
import org.jruby.compiler.ir.operands.Splat;
import org.jruby.compiler.ir.operands.StringLiteral;
import org.jruby.compiler.ir.operands.Symbol;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.BlockBody;
import org.jruby.util.ByteList;

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
//   Look in buildIf for an example of this

public class IRBuilder {
    public static void main(String[] args) {
        boolean isDebug = args.length > 0 && args[0].equals("-debug");
        int     i = isDebug ? 1 : 0;

        String methName = null;
        if (args.length > i && args[i].equals("-inline")) {
           methName = args[i+1];
           i += 2;
        }

        boolean isCommandLineScript = args.length > i && args[i].equals("-e");
        i += (isCommandLineScript ? 1 : 0);

        while (i < args.length) {
           long t1 = new Date().getTime();
           Node ast = buildAST(isCommandLineScript, args[i]);
           long t2 = new Date().getTime();
           IRScope scope = new IRBuilder().buildRoot((RootNode) ast);
           long t3 = new Date().getTime();
           if (isDebug) {
               System.out.println("################## Before local optimization pass ##################");
               scope.runCompilerPass(new org.jruby.compiler.ir.compiler_pass.IR_Printer());
           }
           scope.runCompilerPass(new org.jruby.compiler.ir.compiler_pass.opts.LocalOptimizationPass());
           long t4 = new Date().getTime();
           if (isDebug) {
               System.out.println("################## After local optimization pass ##################");
               scope.runCompilerPass(new org.jruby.compiler.ir.compiler_pass.IR_Printer());
           }
           scope.runCompilerPass(new org.jruby.compiler.ir.compiler_pass.CFG_Builder());
           long t5 = new Date().getTime();
//           scope.runCompilerPass(new org.jruby.compiler.ir.compiler_pass.DominatorTreeBuilder());
           long t6 = new Date().getTime();

           if (methName != null) {
              System.out.println("################## After inline pass ##################");
              System.out.println("Asked to inline " + methName);
              scope.runCompilerPass(new org.jruby.compiler.ir.compiler_pass.InlineTest(methName));
              scope.runCompilerPass(new org.jruby.compiler.ir.compiler_pass.opts.LocalOptimizationPass());
              scope.runCompilerPass(new org.jruby.compiler.ir.compiler_pass.IR_Printer());
           }

           if (isDebug) {
               System.out.println("################## After dead code elimination pass ##################");
           }
           scope.runCompilerPass(new org.jruby.compiler.ir.compiler_pass.LiveVariableAnalysis());
           long t7 = new Date().getTime();
           scope.runCompilerPass(new org.jruby.compiler.ir.compiler_pass.opts.DeadCodeElimination());
           long t8 = new Date().getTime();
           scope.runCompilerPass(new org.jruby.compiler.ir.compiler_pass.AddFrameInstructions());
           long t9 = new Date().getTime();
           if (isDebug) {
               scope.runCompilerPass(new org.jruby.compiler.ir.compiler_pass.IR_Printer());
           }
           if (isDebug) {
               System.out.println("################## After cfg linearization pass ##################");
           }
           scope.runCompilerPass(new org.jruby.compiler.ir.compiler_pass.LinearizeCFG());

           System.out.println("Time to build AST         : " + (t2 - t1));
           System.out.println("Time to build IR          : " + (t3 - t2));
           System.out.println("Time to run local opts    : " + (t4 - t3));
           System.out.println("Time to run build cfg     : " + (t5 - t4));
           System.out.println("Time to run build domtree : " + (t6 - t5));
           System.out.println("Time to run lva           : " + (t7 - t6));
           System.out.println("Time to run dead code elim: " + (t8 - t7));
           System.out.println("Time to add frame instrs  : " + (t9 - t8));
           i++;
        }
    }

    /* -----------------------------------------------------------------------------------
     * Every ensure block has a start label and end label, and at the end, it will execute
     * an jump to an address stored in a return address variable.
     *
     * This ruby code will translate to the IR shown below
     * -----------------
     *   begin
     *       ... protected body ...
     *   ensure
     *       ... ensure block to run
     *   end
     * -----------------
     *  IR instructions for the protected body
     *  L_start:
     *     .. ensure block IR ...
     *     jump %ret_addr
     *  L_end:
     * -----------------
     *
     * If N is a node in the protected body that might exit this scope (exception rethrows
     * and returns), N has to first jump to the ensure block and let the ensure block run.
     * In addition, N has to set up a return address label in the return address var of
     * this ensure block so that the ensure block can transfer control block to N.
     *
     * Since we can have a nesting of ensure blocks, we are maintaining a stack of these
     * well-nested ensure blocks.  Every node N that will exit this scope will have to 
     * co-ordinate the jumps in-and-out of the ensure blocks in the top-to-bottom stacked
     * order.
     * ----------------------------------------------------------------------------------- */
    private static class EnsureBlockInfo
    {
        Label    start;
        Label    end;
        Variable returnAddr;
            // Flag set if the protected body has a return or if it rethrows exception
            // -- basically anytime there is a non-fallthru xfer of control flow.
        boolean  noFallThru;
            // The end label for an ensure block is not always needed.
        boolean endLabelNeeded;

        public EnsureBlockInfo(IRScope m)
        {
            returnAddr = m.getNewTemporaryVariable();
            start      = m.getNewLabel();
            end        = m.getNewLabel();
            noFallThru = false;
            endLabelNeeded = false;
        }

        public static void emitJumpChain(IRScope m, Stack<EnsureBlockInfo> ebStack)
        {
            int n = ebStack.size();
            EnsureBlockInfo[] ebArray = ebStack.toArray(new EnsureBlockInfo[n]);
            for (int i = n-1; i >= 0; i--) {
                Label retLabel = m.getNewLabel();
                m.addInstr(new SET_RETADDR_Instr(ebArray[i].returnAddr, retLabel));
                m.addInstr(new JumpInstr(ebArray[i].start));
                m.addInstr(new LABEL_Instr(retLabel));
                ebArray[i].noFallThru = true;
            }
        }
    }

    private int _lastProcessedLineNum = -1;
    private Stack<EnsureBlockInfo> _ensureBlockStack = new Stack<EnsureBlockInfo>();

    // Stack encoding nested rescue blocks -- this just tracks the start label of the blocks
    private Stack<Label> _rescueBlockLabelStack = new Stack<Label>();

    public static Node buildAST(boolean isCommandLineScript, String arg) {
        Ruby ruby = Ruby.getGlobalRuntime();
        if (isCommandLineScript) {
            // inline script
            return ruby.parse(ByteList.create(arg), "-e", null, 0, false);
        } else {
            // from file
            FileInputStream fis = null;
            try {
                File file = new File(arg);
                fis = new FileInputStream(file);
                long size = file.length();
                byte[] bytes = new byte[(int)size];
                fis.read(bytes);
                System.out.println("-- processing " + arg + " --");
                return ruby.parse(new ByteList(bytes), arg, null, 0, false);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            } finally {
                try { if (fis != null) fis.close(); } catch(Exception e) { }
            }
        }
    }

    public Node skipOverNewlines(IRScope s, Node n) {
        if (n.getNodeType() == NodeType.NEWLINENODE) {
            // Do not emit multiple line number instrs for the same line
            int currLineNum = n.getPosition().getStartLine();
            if (currLineNum != _lastProcessedLineNum) {
               s.addInstr(new LineNumberInstr(s, currLineNum));
               _lastProcessedLineNum = currLineNum;
            }
        }

        while (n.getNodeType() == NodeType.NEWLINENODE)
            n = ((NewlineNode)n).getNextNode();

        return n;
    }

    public Operand generateJRubyUtilityCall(IRScope m, MethAddr meth, Operand receiver, Operand[] args) {
        Variable ret = m.getNewTemporaryVariable();
        m.addInstr(new JRubyImplCallInstr(ret, meth, receiver, args));
        return ret;
    }

    public Operand build(Node node, IRScope m) {
        if (node == null) {
            return null;
        }
        if (m == null) {
            System.out.println("Got a null scope!");
            throw new NotCompilableException("Unknown node encountered in builder: " + node);
        }
        switch (node.getNodeType()) {
            case ALIASNODE: return buildAlias((AliasNode) node, m); // done -- see FIXME
            case ANDNODE: return buildAnd((AndNode) node, m); // done
            case ARGSCATNODE: return buildArgsCat((ArgsCatNode) node, m); // done
            case ARGSPUSHNODE: return buildArgsPush((ArgsPushNode) node, m); // Nothing to do for 1.8
            case ARRAYNODE: return buildArray(node, m); // done
            case ATTRASSIGNNODE: return buildAttrAssign((AttrAssignNode) node, m); // done
            case BACKREFNODE: return buildBackref((BackRefNode) node, m); // done
            case BEGINNODE: return buildBegin((BeginNode) node, m); // done
            case BIGNUMNODE: return buildBignum((BignumNode) node, m); // done
            case BLOCKNODE: return buildBlock((BlockNode) node, m); // done
            case BREAKNODE: return buildBreak((BreakNode) node, (IRExecutionScope)m); // done?
            case CALLNODE: return buildCall((CallNode) node, m); // done
            case CASENODE: return buildCase((CaseNode) node, m); // done
            case CLASSNODE: return buildClass((ClassNode) node, m); // done
            case CLASSVARNODE: return buildClassVar((ClassVarNode) node, m); // done
            case CLASSVARASGNNODE: return buildClassVarAsgn((ClassVarAsgnNode) node, m); // done
            case CLASSVARDECLNODE: return buildClassVarDecl((ClassVarDeclNode) node, m); // done
            case COLON2NODE: return buildColon2((Colon2Node) node, m); // done
            case COLON3NODE: return buildColon3((Colon3Node) node, m); // done
            case CONSTDECLNODE: return buildConstDecl((ConstDeclNode) node, m); // done
            case CONSTNODE: return buildConst((ConstNode) node, m); // done
            case DASGNNODE: return buildDAsgn((DAsgnNode) node, m); // done
            case DEFINEDNODE: return buildDefined(node, m); // SSS FIXME: Incomplete
            case DEFNNODE: return buildDefn((MethodDefNode) node, m); // done
            case DEFSNODE: return buildDefs((DefsNode) node, m); // done
            case DOTNODE: return buildDot((DotNode) node, m); // done
            case DREGEXPNODE: return buildDRegexp((DRegexpNode) node, m); // done
            case DSTRNODE: return buildDStr((DStrNode) node, m); // done
            case DSYMBOLNODE: return buildDSymbol(node, m); // done
            case DVARNODE: return buildDVar((DVarNode) node, m); // done
            case DXSTRNODE: return buildDXStr((DXStrNode) node, m); // done
            case ENSURENODE: return buildEnsureNode(node, m); // done
            case EVSTRNODE: return buildEvStr((EvStrNode) node, m); // done
            case FALSENODE: return buildFalse(node, m); // done
            case FCALLNODE: return buildFCall((FCallNode) node, m); // done
            case FIXNUMNODE: return buildFixnum((FixnumNode) node, m); // done
//            case FLIPNODE: return buildFlip(node, m); // SSS FIXME: What code generates this AST?
            case FLOATNODE: return buildFloat((FloatNode) node, m); // done
            case FORNODE: return buildFor((ForNode) node, (IRExecutionScope)m); // done
            case GLOBALASGNNODE: return buildGlobalAsgn((GlobalAsgnNode) node, m); // done
            case GLOBALVARNODE: return buildGlobalVar((GlobalVarNode) node, m); // done
            case HASHNODE: return buildHash((HashNode) node, m); // done
            case IFNODE: return buildIf((IfNode) node, m); // done
            case INSTASGNNODE: return buildInstAsgn((InstAsgnNode) node, m); // done
            case INSTVARNODE: return buildInstVar((InstVarNode) node, m); // done
            case ITERNODE: return buildIter((IterNode) node, (IRExecutionScope)m); // done
            case LOCALASGNNODE: return buildLocalAsgn((LocalAsgnNode) node, m); // done
            case LOCALVARNODE: return buildLocalVar((LocalVarNode) node, m); // done
            case MATCH2NODE: return buildMatch2((Match2Node) node, m); // done
            case MATCH3NODE: return buildMatch3((Match3Node) node, m); // done
            case MATCHNODE: return buildMatch((MatchNode) node, m); // done
            case MODULENODE: return buildModule((ModuleNode) node, m); // done
            case MULTIPLEASGNNODE: return buildMultipleAsgn((MultipleAsgnNode) node, m); // done
            case NEWLINENODE: return buildNewline((NewlineNode) node, m); // done
            case NEXTNODE: return buildNext((NextNode) node, (IRExecutionScope)m); // done?
            case NTHREFNODE: return buildNthRef((NthRefNode) node, m); // done
            case NILNODE: return buildNil(node, m); // done
            case NOTNODE: return buildNot((NotNode) node, m); // done
            case OPASGNANDNODE: return buildOpAsgnAnd((OpAsgnAndNode) node, m); // done
            case OPASGNNODE: return buildOpAsgn((OpAsgnNode) node, m); // done
            case OPASGNORNODE: return buildOpAsgnOr((OpAsgnOrNode) node, m); // done
            case OPELEMENTASGNNODE: return buildOpElementAsgn(node, m); // done
            case ORNODE: return buildOr((OrNode) node, m); // done
//            case POSTEXENODE: return buildPostExe(node, m); // DEFERRED
//            case PREEXENODE: return buildPreExe(node, m); // DEFERRED
            case REDONODE: return buildRedo(node, (IRExecutionScope)m); // done??
            case REGEXPNODE: return buildRegexp((RegexpNode) node, m); // done
            case RESCUEBODYNODE:
                throw new NotCompilableException("rescue body is handled by rescue compilation at: " + node.getPosition());
            case RESCUENODE: return buildRescue(node, m); // done
//            case RETRYNODE: return buildRetry(node, m); // DEFERRED
            case RETURNNODE: return buildReturn((ReturnNode) node, m); // done
            case ROOTNODE:
                throw new NotCompilableException("Use buildRoot(); Root node at: " + node.getPosition());
            case SCLASSNODE: return buildSClass((SClassNode) node, m); // done
            case SELFNODE: return buildSelf((SelfNode) node, m); // done
            case SPLATNODE: return buildSplat((SplatNode) node, m); // done
            case STRNODE: return buildStr((StrNode) node, m); // done
            case SUPERNODE: return buildSuper((SuperNode) node, m); // done
            case SVALUENODE: return buildSValue((SValueNode) node, m); // done
            case SYMBOLNODE: return buildSymbol((SymbolNode) node, m); // done
            case TOARYNODE: return buildToAry((ToAryNode) node, m); // done
            case TRUENODE: return buildTrue(node, m); // done
            case UNDEFNODE: return buildUndef(node, m); // done
            case UNTILNODE: return buildUntil((UntilNode) node, (IRExecutionScope)m); // done
            case VALIASNODE: return buildVAlias(node, m); // done -- see FIXME
            case VCALLNODE: return buildVCall((VCallNode) node, m); // done
            case WHILENODE: return buildWhile((WhileNode) node, (IRExecutionScope)m); // done
            case WHENNODE: assert false : "When nodes are handled by case node compilation."; return null;
            case XSTRNODE: return buildXStr((XStrNode) node, m); // done
            case YIELDNODE: return buildYield((YieldNode) node, m); // done
            case ZARRAYNODE: return buildZArray(node, m); // done
            case ZSUPERNODE: return buildZSuper((ZSuperNode) node, m); // done
            default: new Exception().printStackTrace(); throw new NotCompilableException("Unknown node encountered in builder: " + node);
        }
    }

    public void buildArguments(List<Operand> args, Node node, IRScope s) {
        switch (node.getNodeType()) {
            case ARGSCATNODE: buildArgsCatArguments(args, (ArgsCatNode) node, s); break;
            case ARGSPUSHNODE: buildArgsPushArguments(args, (ArgsPushNode) node, s); break;
            case ARRAYNODE: buildArrayArguments(args, node, s); break;
            case SPLATNODE: buildSplatArguments(args, (SplatNode) node, s); break;
            default: 
                Operand retVal = build(node, s);
                if (retVal != null)    // SSS FIXME: Can this ever be null?
                   args.add(retVal);
        }
    }
    
    public void buildVariableArityArguments(List<Operand> args, Node node, IRScope s) {
       buildArguments(args, node, s);
    }

    public void buildSpecificArityArguments (List<Operand> args, Node node, IRScope s) {
        if (node.getNodeType() == NodeType.ARRAYNODE) {
            ArrayNode arrayNode = (ArrayNode)node;
            if (arrayNode.isLightweight()) {
                // explode array, it's an internal "args" array
                for (Node n : arrayNode.childNodes())
                    args.add(build(n, s));
            } else {
                // use array as-is, it's a literal array
                args.add(build(arrayNode, s));
            }
        } else {
            args.add(build(node, s));
        }
    }

    public List<Operand> setupCallArgs(Node args, IRScope s) {
        List<Operand> argsList = new ArrayList<Operand>();
        if (args != null) {
           // unwrap newline nodes to get their actual type
           args = skipOverNewlines(s, args);
           buildArgs(argsList, args, s);
        }

        return argsList;
    }

    public void buildArgs(List<Operand> argsList, Node args, IRScope s) {
        switch (args.getNodeType()) {
            case ARGSCATNODE:
            case ARGSPUSHNODE:
            case SPLATNODE:
                buildVariableArityArguments(argsList, args, s);
                break;
            case ARRAYNODE:
                ArrayNode arrayNode = (ArrayNode)args;
                // ENEBO: This is not right.  ArrayNode is not just for boxing
//                if (arrayNode.size() > 3)
//                    buildVariableArityArguments(argsList, arrayNode, s);
//                else if (arrayNode.size() > 0)
                    buildSpecificArityArguments(argsList, arrayNode, s);
                break;
            default:
                buildSpecificArityArguments(argsList, args, s);
                break;
        }
    }

    // This method is called to build assignments for a multiple-assignment instruction
    public void buildAssignment(Node node, IRScope s, Operand values, int argIndex, boolean isSplat) {
        Variable v = s.getNewTemporaryVariable();
        s.addInstr(new GetArrayInstr(v, values, argIndex, isSplat));
        switch (node.getNodeType()) {
            case ATTRASSIGNNODE: 
                buildAttrAssignAssignment(node, s, v);
                break;
            // SSS FIXME: What is the difference between ClassVarAsgnNode & ClassVarDeclNode
            case CLASSVARASGNNODE:
                s.addInstr(new PutClassVariableInstr(MetaObject.create(s).getNearestClass(), ((ClassVarAsgnNode)node).getName(), v));
                break;
            case CLASSVARDECLNODE:
                s.addInstr(new PutClassVariableInstr(MetaObject.create(s).getNearestClass(), ((ClassVarDeclNode)node).getName(), v));
                break;
            case CONSTDECLNODE:
                buildConstDeclAssignment((ConstDeclNode) node, s, v);
                break;
            case GLOBALASGNNODE:
                s.addInstr(new PutGlobalVarInstr(((GlobalAsgnNode)node).getName(), v));
                break;
            case INSTASGNNODE:
                // NOTE: if 's' happens to the a class, this is effectively an assignment of a class instance variable
                s.addInstr(new PutFieldInstr(getSelf(s), ((InstAsgnNode)node).getName(), v));
                break;
            case LOCALASGNNODE: {
                LocalAsgnNode localVariable = (LocalAsgnNode) node;
                int depth = localVariable.getDepth();

                s.addInstr(new CopyInstr(getScopeNDown(s, depth).getLocalVariable(localVariable.getName()), v));
                break;
            }
            case MULTIPLEASGNNODE:
                buildMultipleAsgnAssignment((MultipleAsgnNode) node, s, v);
                break;
            case ZEROARGNODE:
                throw new NotCompilableException("Shouldn't get here; zeroarg does not do assignment: " + node);
            default:
                throw new NotCompilableException("Can't build assignment node: " + node);
        }
    }

    // This method is called to build arguments for a block!
    public void buildBlockArgsAssignment(Node node, IRScope s, int argIndex, boolean isSplat) {
        Variable v;
        switch (node.getNodeType()) {
            case ATTRASSIGNNODE: 
                v = s.getNewTemporaryVariable();
                s.addInstr(new ReceiveClosureArgInstr(v, argIndex, isSplat));
                buildAttrAssignAssignment(node, s, v);
                break;
// SSS FIXME:
//
// There are also differences in variable scoping between 1.8 and 1.9 
// Ruby 1.8 is the buggy semantics if I understand correctly.
//
// The semantics of how this shadows other variables outside the block needs
// to be figured out during live var analysis.
            case DASGNNODE: {
                DAsgnNode dynamicAsgn = (DAsgnNode) node;
                v = getScopeNDown(s, dynamicAsgn.getDepth()).getLocalVariable(dynamicAsgn.getName());
                s.addInstr(new ReceiveClosureArgInstr(v, argIndex, isSplat));
                break;
            }
            // SSS FIXME: What is the difference between ClassVarAsgnNode & ClassVarDeclNode
            case CLASSVARASGNNODE:
                v = s.getNewTemporaryVariable();
                s.addInstr(new ReceiveClosureArgInstr(v, argIndex, isSplat));
                s.addInstr(new PutClassVariableInstr(MetaObject.create(s).getNearestClass(), ((ClassVarAsgnNode)node).getName(), v));
                break;
            case CLASSVARDECLNODE:
                v = s.getNewTemporaryVariable();
                s.addInstr(new ReceiveClosureArgInstr(v, argIndex, isSplat));
                s.addInstr(new PutClassVariableInstr(MetaObject.create(s).getNearestClass(), ((ClassVarDeclNode)node).getName(), v));
                break;
            case CONSTDECLNODE:
                v = s.getNewTemporaryVariable();
                s.addInstr(new ReceiveClosureArgInstr(v, argIndex, isSplat));
                buildConstDeclAssignment((ConstDeclNode) node, s, v);
                break;
            case GLOBALASGNNODE:
                v = s.getNewTemporaryVariable();
                s.addInstr(new ReceiveClosureArgInstr(v, argIndex, isSplat));
                s.addInstr(new PutGlobalVarInstr(((GlobalAsgnNode)node).getName(), v));
                break;
            case INSTASGNNODE:
                v = s.getNewTemporaryVariable();
                s.addInstr(new ReceiveClosureArgInstr(v, argIndex, isSplat));
                // NOTE: if 's' happens to the a class, this is effectively an assignment of a class instance variable
                s.addInstr(new PutFieldInstr(getSelf(s), ((InstAsgnNode)node).getName(), v));
                break;
            case LOCALASGNNODE: {
                LocalAsgnNode localVariable = (LocalAsgnNode) node;
                int depth = localVariable.getDepth();

                v = getScopeNDown(s, depth).getLocalVariable(localVariable.getName());
                s.addInstr(new ReceiveClosureArgInstr(v, argIndex, isSplat));
                break;
            }
            case MULTIPLEASGNNODE:
                // SSS FIXME: Are we guaranteed that we splats dont head to multiple-assignment nodes!  i.e. |*(a,b)|?
                buildMultipleAsgnAssignment((MultipleAsgnNode) node, s, null);
                break;
            case ZEROARGNODE:
                throw new NotCompilableException("Shouldn't get here; zeroarg does not do assignment: " + node);
            default:
                throw new NotCompilableException("Can't build assignment node: " + node);
        }
    }

    // SSS FIXME: Got a little lazy?  We could/should define a special instruction ALIAS_METHOD_Instr probably
    // Is this a ruby-internals or a jruby-internals call?
    public Operand buildAlias(final AliasNode alias, IRScope s) {
        Operand newName = build(alias.getNewName(), s);
        Operand oldName = build(alias.getOldName(), s);
        Operand[] args = new Operand[] { newName, oldName };
        s.addInstr(new RubyInternalCallInstr(null, MethAddr.DEFINE_ALIAS, MetaObject.create(s), args));
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
    public Operand buildAnd(final AndNode andNode, IRScope m) {
        if (andNode.getFirstNode().getNodeType().alwaysTrue()) {
            // build first node (and ignore its result) and then second node
            build(andNode.getFirstNode(), m);
            return build(andNode.getSecondNode(), m);
        } else if (andNode.getFirstNode().getNodeType().alwaysFalse()) {
            // build first node only and return false
            build(andNode.getFirstNode(), m);
            return BooleanLiteral.FALSE;
        } else {
            Variable ret = m.getNewTemporaryVariable();
            Label    l   = m.getNewLabel();
            Operand  v1  = build(andNode.getFirstNode(), m);
            m.addInstr(new CopyInstr(ret, BooleanLiteral.FALSE));
            m.addInstr(new BEQInstr(v1, BooleanLiteral.FALSE, l));
            Operand  v2  = build(andNode.getSecondNode(), m);
            m.addInstr(new CopyInstr(ret, v2));
            m.addInstr(new LABEL_Instr(l));
            return ret;
        }
    }

    public Operand buildArray(Node node, IRScope m) {
        List<Operand> elts = new ArrayList<Operand>();
        for (Node e: node.childNodes())
            elts.add(build(e, m));

        return new Array(elts);
    }

    public Operand buildArgsCat(final ArgsCatNode argsCatNode, IRScope s) {
        Operand v1 = build(argsCatNode.getFirstNode(), s);
        Operand v2 = build(argsCatNode.getSecondNode(), s);
        return new CompoundArray(v1, v2);
    }

    public Operand buildArgsPush(final ArgsPushNode node, IRScope m) {
        throw new NotCompilableException("ArgsPush should never be encountered bare in 1.8");
    }

    private Operand buildAttrAssign(final AttrAssignNode attrAssignNode, IRScope s) {
        List<Operand> args = setupCallArgs(attrAssignNode.getArgsNode(), s);
        Operand obj = build(attrAssignNode.getReceiverNode(), s);
        s.addInstr(new AttrAssignInstr(obj, new StringLiteral(attrAssignNode.getName()), args.get(0)));
        return args.get(0);
    }

    public Operand buildAttrAssignAssignment(Node node, IRScope s, Operand value) {
        final AttrAssignNode attrAssignNode = (AttrAssignNode) node;
        List<Operand> args = setupCallArgs(attrAssignNode.getArgsNode(), s);
        Operand obj = build(attrAssignNode.getReceiverNode(), s);
        s.addInstr(new AttrAssignInstr(obj, new StringLiteral(attrAssignNode.getName()), value));
        return value;
    }

    public Operand buildBackref(BackRefNode node, IRScope m) {
        return new Backref(node.getType());
    }

    public Operand buildBegin(BeginNode beginNode, IRScope s) {
        return build(beginNode.getBodyNode(), s);
    }

    public Operand buildBignum(BignumNode node, IRScope s) {
        return new Fixnum(node.getValue());
    }

    public Operand buildBlock(BlockNode node, IRScope s) {
        Operand retVal = null;
        for (Node child : node.childNodes()) {
            retVal = build(child, s);
        }
        
           // Value of the last expression in the block 
        return retVal;
    }

    public Operand buildBreak(BreakNode breakNode, IRExecutionScope s) {
        Operand rv = build(breakNode.getValueNode(), s);
        if (s instanceof IRClosure) {
            s.addInstr(new BREAK_Instr(rv));
            return rv;
        }
        else {
            // If this is not a closure, the break is equivalent to jumping to the loop end label
            // But, since break can return a result even in loops, we need to pass back both
            // the return value as well as the jump target -- so, create a special-purpose operand just for that purpose!
            return new BreakResult(rv, s.getCurrentLoop().loopEndLabel);
        }
    }

    public Operand buildCall(CallNode callNode, IRScope s) {
        Node          callArgsNode = callNode.getArgsNode();
        Node          receiverNode = callNode.getReceiverNode();
        List<Operand> args         = setupCallArgs(callArgsNode, s);
        Operand       block        = setupCallClosure(callNode.getIterNode(), s);
        Variable      callResult   = s.getNewTemporaryVariable();
        Instr      callInstr    = new CallInstr(callResult, new MethAddr(callNode.getName()),
                build(receiverNode, s), args.toArray(new Operand[args.size()]), block);
        s.addInstr(callInstr);
        return callResult;
    }

    public Operand buildCase(CaseNode caseNode, IRScope m) {
        // get the incoming case value
        Operand value = build(caseNode.getCaseNode(), m);

        // the CASE instruction
        Label endLabel = m.getNewLabel();
        Variable result = m.getNewTemporaryVariable();
        CaseInstr caseInstr = new CaseInstr(result, value, endLabel);
        m.addInstr(caseInstr);

        // lists to aggregate variables and bodies for whens
        List<Operand> variables = new ArrayList<Operand>();
        List<Label> labels = new ArrayList<Label>();

        Map<Label, Node> bodies = new HashMap<Label, Node>();

        // build each "when"
        for (Node aCase : caseNode.getCases().childNodes()) {
            WhenNode whenNode = (WhenNode)aCase;
            Label bodyLabel = m.getNewLabel();

            if (whenNode.getExpressionNodes() instanceof ListNode) {
                // multiple conditions for when
                for (Node expression : ((ListNode)whenNode.getExpressionNodes()).childNodes()) {
                    Variable eqqResult = m.getNewTemporaryVariable();

                    variables.add(eqqResult);
                    labels.add(bodyLabel);
                    
                    m.addInstr(new EQQ_Instr(eqqResult, build(expression, m), value));
                    m.addInstr(new BEQInstr(eqqResult, BooleanLiteral.TRUE, bodyLabel));
                }
            } else {
                Variable eqqResult = m.getNewTemporaryVariable();

                variables.add(eqqResult);
                labels.add(bodyLabel);

                m.addInstr(new EQQ_Instr(eqqResult, build(whenNode.getExpressionNodes(), m), value));
                m.addInstr(new BEQInstr(eqqResult, BooleanLiteral.TRUE, bodyLabel));
            }

            // SSS FIXME: This doesn't preserve original order of when clauses.  We could consider
            // preserving the order (or maybe not, since we would have to sort the constants first
            // in any case) for outputing jump tables in certain situations.
            //
            // add body to map for emitting later
            bodies.put(bodyLabel, whenNode.getBodyNode());
        }

        // build "else" if it exists
        if (caseNode.getElseNode() != null) {
            Label elseLbl = m.getNewLabel();
            caseInstr.setElse(elseLbl);

            bodies.put(elseLbl, caseNode.getElseNode());
        }

        // now emit bodies
        for (Map.Entry<Label, Node> entry : bodies.entrySet()) {
            m.addInstr(new LABEL_Instr(entry.getKey()));
            Operand bodyValue = build(entry.getValue(), m);
            // Local optimization of break results (followed by a copy & jump) to short-circuit the jump right away
            // rather than wait to do it during an optimization pass when a dead jump needs to be removed.
            Label tgt = endLabel;
            if (bodyValue instanceof BreakResult) {
                BreakResult br = (BreakResult)bodyValue;
                bodyValue = br._result;
                tgt = br._jumpTarget;
            }
            m.addInstr(new CopyInstr(result, bodyValue));
            m.addInstr(new JumpInstr(tgt));
        }

        // close it out
        m.addInstr(new LABEL_Instr(endLabel));
        caseInstr.setLabels(labels);
        caseInstr.setVariables(variables);

        return result;
    }

    public Operand buildClass(ClassNode classNode, IRScope s) {
        final Node       superNode = classNode.getSuperNode();
        final Colon3Node cpathNode = classNode.getCPath();

        Operand superClass = null;
        if (superNode != null)
            superClass = build(superNode, s);

            // By default, the container for this class is 's'
        Operand container = null;

            // Do we have a dynamic container?
        if (cpathNode instanceof Colon2Node) {
            Node leftNode = ((Colon2Node) cpathNode).getLeftNode();
            if (leftNode != null)
                container = build(leftNode, s);
        } else if (cpathNode instanceof Colon3Node) {
            container = new ClassMetaObject(IRClass.getCoreClass("Object"));
        }

            // Build a new class and add it to the current scope (could be a script / module / class)
        String   className = cpathNode.getName();
        IRClass c = new IRClass(s, container, superClass, className, classNode.getScope());
        s.addClass(c);
        if (container != null)
            s.addInstr(new PutConstInstr(container, className, new ClassMetaObject(c)));

            // Build the class body!
        if (classNode.getBodyNode() != null)
            build(classNode.getBodyNode(), c.getRootMethod());

        return null;
    }

    public Operand buildSClass(SClassNode sclassNode, IRScope s) {
        //  class Foo
        //  ...
        //    class << self
        //    ...
        //    end
        //  ...
        //  end
        //
        // Here, the class << self declaration is in Foo's root method.
        // Foo is the class in whose context this is being defined.
        Operand receiver = build(sclassNode.getReceiverNode(), s);
        IRClass mc = new IRMetaClass(s, receiver, sclassNode.getScope());

        // Record the new class as being lexically defined in scope s
        s.addClass(mc);

        if (sclassNode.getBodyNode() != null)
            build(sclassNode.getBodyNode(), mc.getRootMethod());

        return null;
    }

    public Operand buildClassVar(ClassVarNode node, IRScope s) {
        Variable ret = s.getNewTemporaryVariable();
        s.addInstr(new GetClassVariableInstr(ret, MetaObject.create(s).getNearestClass(), node.getName()));
        return ret;
    }

    // SSS FIXME: Where is this set up?  How is this diff from ClassVarDeclNode??
    public Operand buildClassVarAsgn(final ClassVarAsgnNode classVarAsgnNode, IRScope s) {
        Operand val = build(classVarAsgnNode.getValueNode(), s);
        s.addInstr(new PutClassVariableInstr(MetaObject.create(s).getNearestClass(), classVarAsgnNode.getName(), val));
        return val;
    }

    public Operand buildClassVarDecl(final ClassVarDeclNode classVarDeclNode, IRScope s) {
        Operand val = build(classVarDeclNode.getValueNode(), s);
        s.addInstr(new PutClassVariableInstr(MetaObject.create(s).getNearestClass(), classVarDeclNode.getName(), val));
        return val;
    }

    public Operand buildConstDecl(ConstDeclNode node, IRScope s) {
        Operand val = build(node.getValueNode(), s);
        return buildConstDeclAssignment(node, s, val);
    }

    public Operand buildConstDeclAssignment(ConstDeclNode constDeclNode, IRScope s, Operand val) {
        Node constNode = constDeclNode.getConstNode();

        if (constNode == null) {
            s.setConstantValue(constDeclNode.getName(), val);
        } else if (constNode.getNodeType() == NodeType.COLON2NODE) {
            Operand module = build(((Colon2Node) constNode).getLeftNode(), s);
            s.addInstr(new PutConstInstr(module, constDeclNode.getName(), val));
        } else { // colon3, assign in Object
            s.addInstr(new PutConstInstr(getSelf(s), constDeclNode.getName(), val));
        }

        return val;
    }

    private Operand loadConst(IRScope s, IRScope currScope, String name)
    {
        Operand cv = s.getConstantValue(name);
        if (cv == null) {
            Variable v = currScope.getNewTemporaryVariable();
            currScope.addInstr(new GetConstInstr(v, s, name));
            cv = v;
        }
        return cv;
    }

    public Operand buildConst(ConstNode node, IRScope s) {
        return loadConst(s, s, node.getName()); 
    }

    public Operand buildColon2(final Colon2Node iVisited, IRScope s) {
        Node leftNode = iVisited.getLeftNode();
        final String name = iVisited.getName();

        if (leftNode == null) {
            return loadConst(s, s, name);
        } 
        else if (iVisited instanceof Colon2ConstNode) {
            // 1. Load the module first (lhs of node)
            // 2. Then load the constant from the module
            Operand module = build(iVisited.getLeftNode(), s);
            if (module instanceof MetaObject) {
                return loadConst(((MetaObject)module).scope, s, name);
            }
            else {
                Variable constVal = s.getNewTemporaryVariable();
                s.addInstr(new GetConstInstr(constVal, module, name));
                return constVal;
            }
        }
        else if (iVisited instanceof Colon2MethodNode) {
            Colon2MethodNode c2mNode = (Colon2MethodNode)iVisited;
            List<Operand> args       = setupCallArgs(null, s);
            Operand       block      = setupCallClosure(null, s);
            Variable      callResult = s.getNewTemporaryVariable();
            Instr      callInstr  = new CallInstr(callResult, new MethAddr(c2mNode.getName()), 
                    null, args.toArray(new Operand[args.size()]), block);
            s.addInstr(callInstr);
            return callResult;
        }
        else { throw new NotCompilableException("Not compilable: " + iVisited); }
    }

    public Operand buildColon3(Colon3Node node, IRScope s) {
        Variable cv = s.getNewTemporaryVariable();
        // SSS FIXME: Is this correct?
        s.addInstr(new GetConstInstr(cv, getSelf(s), node.getName()));
        return cv;
    }

    public Operand buildGetDefinitionBase(final Node node, IRScope m) {
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
            // these are all "simple" cases that don't require the heavier defined logic
            return buildGetDefinition(node, m);
        default:
            throw new NotCompilableException(node + " is not yet IR-compilable ..");
/**
 * SSS FIXME: To be completed
 *
        default:
            BranchCallback reg = new BranchCallback() {

                        public void branch(IRScope m) {
                            m.inDefined();
                            buildGetDefinition(node, m);
                        }
                    };
            BranchCallback out = new BranchCallback() {

                        public void branch(IRScope m) {
                            m.outDefined();
                        }
                    };
            m.protect(reg, out, String.class);
**/
        }
    }

    public Operand buildDefined(final Node node, IRScope m) {
        return buildGetDefinitionBase(((DefinedNode) node).getExpressionNode(), m);
    }

/*
    public Operand buildGetArgumentDefinition(final Node node, IRScope m, String type) {
        if (node == null) {
            return new StringLiteral(type);
        } else if (node instanceof ArrayNode) {
            Object endToken = m.getNewEnding();
            for (int i = 0; i < ((ArrayNode) node).size(); i++) {
                Node iterNode = ((ArrayNode) node).get(i);
                buildGetDefinition(iterNode, m);
                m.ifNull(endToken);
            }
            Operand sl = new StringLiteral(type);
            Object realToken = m.getNewEnding();
            m.go(realToken);
            m.setEnding(endToken);
            m.pushNull();
            m.setEnding(realToken);
        } else {
            buildGetDefinition(node, m);
            Object endToken = m.getNewEnding();
            m.ifNull(endToken);
            Operand sl = new StringLiteral(type);
            Object realToken = m.getNewEnding();
            m.go(realToken);
            m.setEnding(endToken);
            m.pushNull();
            m.setEnding(realToken);
        }
    }
*/

    public Operand buildGetDefinition(final Node node, IRScope m) {
        switch (node.getNodeType()) {
            case CLASSVARASGNNODE:
            case CLASSVARDECLNODE:
            case CONSTDECLNODE:
            case DASGNNODE:
            case GLOBALASGNNODE:
            case LOCALASGNNODE:
            case MULTIPLEASGNNODE:
            case OPASGNNODE:
            case OPASGNANDNODE:
            case OPASGNORNODE:
            case OPELEMENTASGNNODE:
            case INSTASGNNODE: // simple assignment cases
                return new StringLiteral("assignment");
            case DVARNODE:  
                return new StringLiteral("local-variable(in-block)");
            case FALSENODE:
                return new StringLiteral("false");
            case TRUENODE:
                return new StringLiteral("true");
            case LOCALVARNODE: 
                return new StringLiteral("local-variable");
            case MATCH2NODE: 
            case MATCH3NODE: 
                return new StringLiteral("method");
            case NILNODE: 
                return new StringLiteral("nil");
            case SELFNODE:
                return new StringLiteral("self");
            default:
                throw new NotCompilableException(node + " is not yet IR-compilable ..");
/**
 *  SSS FIXME: To be completed
 *
            case BACKREFNODE:
                    // SSS FIXME!
                Operand x = m.backref();
                return x instanceof RubyMatchData.class ? new StringLiteral("$" + ((BackRefNode) node).getType()) : Nil.NIL;
            case NTHREFNODE:
                m.isCaptured(((NthRefNode) node).getMatchNumber(),
                        new BranchCallback() {
                            public void branch(IRScope m) {
                                return new StringLiteral("$" + ((NthRefNode) node).getMatchNumber());
                            }
                        },
                        new BranchCallback() {
                            public void branch(IRScope m) {
                                return Nil.NIL;
                            }
                        });
                break;
            case VCALLNODE:
                m.loadSelf();
                m.isMethodBound(((VCallNode) node).getName(),
                        new BranchCallback() {
                            public void branch(IRScope m) {
                                return new StringLiteral("method");
                            }
                        },
                        new BranchCallback() {
                            public void branch(IRScope m) {
                                return Nil.NIL;
                            }
                        });
                break;
            case YIELDNODE:
                m.hasBlock(new BranchCallback() {
                            public void branch(IRScope m) {
                                return new StringLiteral("yield");
                            }
                        },
                        new BranchCallback() {
                            public void branch(IRScope m) {
                                return Nil.NIL;
                            }
                        });
                break;
            case GLOBALVARNODE:
                m.isGlobalDefined(((GlobalVarNode) node).getName(),
                        new BranchCallback() {
                            public void branch(IRScope m) {
                                return new StringLiteral("global-variable");
                            }
                        },
                        new BranchCallback() {
                            public void branch(IRScope m) {
                                return Nil.NIL;
                            }
                        });
                break;
            case INSTVARNODE:
                m.isInstanceVariableDefined(((InstVarNode) node).getName(),
                        new BranchCallback() {
                            public void branch(IRScope m) {
                                return new StringLiteral("instance-variable");
                            }
                        },
                        new BranchCallback() {
                            public void branch(IRScope m) {
                                return Nil.NIL;
                            }
                        });
                break;
            case CONSTNODE:
                m.isConstantDefined(((ConstNode) node).getName(),
                        new BranchCallback() {
                            public void branch(IRScope m) {
                                return new StringLiteral("constant");
                            }
                        },
                        new BranchCallback() {
                            public void branch(IRScope m) {
                                return Nil.NIL;
                            }
                        });
                break;
            case FCALLNODE:
                m.loadSelf();
                m.isMethodBound(((FCallNode) node).getName(),
                        new BranchCallback() {
                            public void branch(IRScope m) {
                                buildGetArgumentDefinition(((FCallNode) node).getArgsNode(), m, "method");
                            }
                        },
                        new BranchCallback() {
                            public void branch(IRScope m) {
                                return Nil.NIL;
                            }
                        });
                break;
            case COLON3NODE:
            case COLON2NODE:
                {
                    final Colon3Node iVisited = (Colon3Node) node;

                    final String name = iVisited.getName();

                    BranchCallback setup = new BranchCallback() {

                                public void branch(IRScope m) {
                                    if (iVisited instanceof Colon2Node) {
                                        final Node leftNode = ((Colon2Node) iVisited).getLeftNode();
                                        build(leftNode, m,true);
                                    } else {
                                        m.loadObject();
                                    }
                                }
                            };
                    BranchCallback isConstant = new BranchCallback() {

                                public void branch(IRScope m) {
                                    return new StringLiteral("constant");
                                }
                            };
                    BranchCallback isMethod = new BranchCallback() {

                                public void branch(IRScope m) {
                                    return new StringLiteral("method");
                                }
                            };
                    BranchCallback none = new BranchCallback() {
                                public void branch(IRScope m) {
                                    return Nil.NIL;
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

                                public void branch(IRScope m) {
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

                                                public void branch(IRScope m) {
                                                    buildGetArgumentDefinition(iVisited.getArgsNode(), m, "method");
                                                }
                                            },
                                            new BranchCallback() {

                                                public void branch(IRScope m) {
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

                                public void branch(IRScope m) {
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

                                public void branch(IRScope m) {
                                    m.consumeCurrentValue();
                                    Operand sl = new StringLiteral("class variable");
                                    m.go(ending);
                                }
                            },
                            new BranchCallback() {

                                public void branch(IRScope m) {
                                }
                            });
                    m.setEnding(second);  //[RubyClass]
                    m.duplicateCurrentValue();
                    m.isClassVarDefined(iVisited.getName(),
                            new BranchCallback() {

                                public void branch(IRScope m) {
                                    m.consumeCurrentValue();
                                    Operand sl = new StringLiteral("class variable");
                                    m.go(ending);
                                }
                            },
                            new BranchCallback() {

                                public void branch(IRScope m) {
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
                    Operand sl = new StringLiteral("class variable");
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

                    Operand sl = new StringLiteral("super");
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

                                public void branch(IRScope m) {
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

                                                public void branch(IRScope m) {
                                                    buildGetArgumentDefinition(iVisited.getArgsNode(), m, "assignment");
                                                }
                                            },
                                            new BranchCallback() {

                                                public void branch(IRScope m) {
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

                                public void branch(IRScope m) {
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

                            public void branch(IRScope m) {
                                build(node, m,true);
                                m.consumeCurrentValue();
                                m.pushNull();
                            }
                        }, JumpException.class,
                        new BranchCallback() {

                            public void branch(IRScope m) {
                                m.pushNull();
                            }
                        }, String.class);
                m.consumeCurrentValue();
                //MPS_FIXME: new StringLiteral("expression");
**/
        }
    }

    public Operand buildDAsgn(final DAsgnNode dasgnNode, IRScope s) {
        // SSS: Looks like we receive the arg in buildBlockArgsAssignment via the IterNode
        // We won't get here for argument receives!  So, buildDasgn is called for
        // assignments to block variables within a block.  As far as the IR is concerned,
        // this is just a simple copy
        int depth = dasgnNode.getDepth();
        Variable arg = getScopeNDown(s, depth).getLocalVariable(dasgnNode.getName());
        
        s.addInstr(new CopyInstr(arg, build(dasgnNode.getValueNode(), s)));
        return arg;
    }
    
    // ENEBO: On IRScope?
    private IRScope getScopeNDown(IRScope current, int depth) {
        for (int i = 0; i < depth; i++) {
            current.getLexicalParent();
        }
            
        return current;
    }

/**
 * SSS FIXME: Used anywhere?  I don't see calls to this anywhere
    public Operand buildDAsgnAssignment(Node node, IRScope s) {
        DAsgnNode dasgnNode = (DAsgnNode) node;
        s.getVariableCompiler().assignLocalVariable(dasgnNode.getIndex(), dasgnNode.getDepth());
    }
**/

    private void defineNewMethod(MethodDefNode defNode, IRScope s, Operand container, boolean isInstanceMethod) {
        IRMethod method = new IRMethod(s, container, defNode.getName(), isInstanceMethod, defNode.getScope());

        // Build IR for arguments
        receiveArgs(defNode.getArgsNode(), method);

        // Build IR for body
        if (defNode.getBodyNode() != null) {
            Node bodyNode = defNode.getBodyNode();

            // if root of method is rescue, build as a light rescue
            Operand rv = (bodyNode instanceof RescueNode) ?
                buildRescueInternal(bodyNode, method) : build(bodyNode, method);
            if (rv != null) method.addInstr(new ReturnInstr(rv));
        } else {
            method.addInstr(new ReturnInstr(Nil.NIL));
        }

        // ENEBO: for module+class methods s.getNearestModule seems to be working...
//        IRModule methodHolder = isInstanceMethod ? s.getNearestModule() :
//            new IRMetaClass(method.getLexicalParent(), container, defNode.getScope());
        IRModule methodHolder = s.getNearestModule();
        methodHolder.addMethod(method);
    }

    public Operand buildDefn(MethodDefNode node, IRScope s) { // Instance method
        defineNewMethod(node, s, MetaObject.create(s), true);
        return null;
    }

    public Operand buildDefs(DefsNode node, IRScope s) { // Class method
        defineNewMethod(node, s, build(node.getReceiverNode(), s), false);
        return null;
    }

    // ENEBO: Since we are now targeting 1.9 semantics then it can be assumed that all
    // method parameters are now going to be local to this scope.
    public Operand receiveArgs(final ArgsNode argsNode, IRScope s) {
        final int required = argsNode.getRequiredArgsCount();
        final int opt = argsNode.getOptionalArgsCount();
        final int rest = argsNode.getRestArg();

          // TODO: Add IR instructions for checking method arity!
        // s.getVariableCompiler().checkMethodArity(required, opt, rest);

            // self = args[0]
        s.addInstr(new ReceiveSelfInstruction(getSelf(s)));

            // Other args begin at index 1
        int argIndex = 1;

            // Both for fixed arity and variable arity methods
        ListNode preArgs  = argsNode.getPre();
        for (int i = 0; i < required; i++, argIndex++) {
            ArgumentNode a = (ArgumentNode)preArgs.get(i);
            if (a instanceof TypedArgumentNode) {
                TypedArgumentNode t = (TypedArgumentNode)a;
                s.addInstr(new DECLARE_LOCAL_TYPE_Instr(argIndex, buildType(t.getTypeNode())));
            }
            s.addInstr(new ReceiveArgumentInstruction(s.getLocalVariable(a.getName()), argIndex));
        }

            // IMPORTANT: Receive the block argument before the opt and splat args
            // This is so that the *arg can be encoded as 'rest of the array'.  This
            // won't work if the block argument hasn't been received yet!
        if (argsNode.getBlock() != null)
            s.addInstr(new ReceiveClosureInstr(s.getLocalVariable(argsNode.getBlock().getName())));

            // Now for the rest
        if (opt > 0 || rest > -1) {
            ListNode optArgs = argsNode.getOptArgs();
            for (int j = 0; j < opt; j++, argIndex++) {
                    // Jump to 'l' if this arg is not null.  If null, fall through and build the default value!
                Label l = s.getNewLabel();
                LocalAsgnNode n = (LocalAsgnNode)optArgs.get(j);
                s.addInstr(new ReceiveOptionalArgumentInstr(s.getLocalVariable(n.getName()), argIndex, l));
                build(n, s);
                s.addInstr(new LABEL_Instr(l));
            }

            if (rest > -1) {
                s.addInstr(new ReceiveArgumentInstruction(s.getLocalVariable(argsNode.getRestArgNode().getName()), argIndex, true));
                argIndex++;
            }
        }

        // FIXME: Ruby 1.9 post args code needs to come here

            // This is not an expression that computes anything
        return null;
    }

    public String buildType(Node typeNode) {
        switch (typeNode.getNodeType()) {
        case CONSTNODE:
            return ((ConstNode)typeNode).getName();
        case SYMBOLNODE:
            return ((SymbolNode)typeNode).getName();
        default:
            return "unknown_type";
        }
    }

    public Operand buildDot(final DotNode dotNode, IRScope s) {
        return new Range(build(dotNode.getBeginNode(), s), build(dotNode.getEndNode(), s), dotNode.isExclusive());
    }

    public Operand buildDRegexp(DRegexpNode dregexpNode, IRScope s) {
        List<Operand> strPieces = new ArrayList<Operand>();
        for (Node n : dregexpNode.childNodes())
            strPieces.add(build(n, s));

        return new Regexp(new CompoundString(strPieces), dregexpNode.getOptions());
    }

    public Operand buildDStr(DStrNode dstrNode, IRScope s) {
        List<Operand> strPieces = new ArrayList<Operand>();
        for (Node n : dstrNode.childNodes())
            strPieces.add(build(n, s));

        return new CompoundString(strPieces);
    }

    public Operand buildDSymbol(Node node, IRScope s) {
        List<Operand> strPieces = new ArrayList<Operand>();
        for (Node n : node.childNodes())
            strPieces.add(build(n, s));

        return new DynamicSymbol(new CompoundString(strPieces));
    }

    public Operand buildDVar(DVarNode node, IRScope m) {
        return m.getLocalVariable(node.getName());
    }

    public Operand buildDXStr(final DXStrNode dstrNode, IRScope m) {
        List<Operand> strPieces = new ArrayList<Operand>();
        for (Node nextNode : dstrNode.childNodes())
            strPieces.add(build(nextNode, m));

        return new BacktickString(strPieces);
    }

    public Operand buildEnsureNode(Node node, IRScope m) {
        // Push a new ensure block info node onto the stack of ensure block
        EnsureBlockInfo ebi = new EnsureBlockInfo(m);
        _ensureBlockStack.push(ebi);

        EnsureNode ensureNode = (EnsureNode)node;
        Node       bodyNode   = ensureNode.getBodyNode();
        Operand rv = (bodyNode instanceof RescueNode) ? buildRescueInternal(bodyNode, m) : build(bodyNode, m);

        // Generate the ensure block now
        //   START:
        //     ... ensure code ...
        //     JMP(ret_addr)
        //   END:
        //
        // Optimization: The start and end labels and the jmp are ignored if the main body falls through
        // ex: begin 
        //        .. something without a return! ..
        //     ensure
        //        .. something else ..
        //     end
        //

        if (ebi.noFallThru)
            m.addInstr(new LABEL_Instr(ebi.start));

        // Pop the current ensure block info node *BEFORE* generating the ensure code for this block itself!
        _ensureBlockStack.pop();

        // Two cases:
        // 1. Ensure block has no explicit return => the result of the entire ensure expression is the result of the protected body.
        // 2. Ensure block has an explicit return => the result of the protected body is ignored.
        Operand ensureRetVal = (ensureNode.getEnsureNode() == null) ? Nil.NIL : build(ensureNode.getEnsureNode(), m);
        if (ensureRetVal == null)   // null => there was a return from within the ensure block!
            rv = null;

        if (ebi.noFallThru) {
            m.addInstr(new JUMP_INDIRECT_Instr(ebi.returnAddr));
            if (ebi.endLabelNeeded)
               m.addInstr(new LABEL_Instr(ebi.end));
        }

        return rv;
    }

    public Operand buildEvStr(EvStrNode node, IRScope s) {
            // SSS: FIXME: Somewhere here, we need to record information the type of this operand as String
        return build(node.getBody(), s);
    }

    public Operand buildFalse(Node node, IRScope s) {
        s.addInstr(new ThreadPollInstr());
        return BooleanLiteral.FALSE; 
    }

    public Operand buildFCall(FCallNode fcallNode, IRScope s) {
        Node          callArgsNode = fcallNode.getArgsNode();
        List<Operand> args         = setupCallArgs(callArgsNode, s);
        Operand       block        = setupCallClosure(fcallNode.getIterNode(), s);
        Variable      callResult   = s.getNewTemporaryVariable();
        Instr      callInstr    = new CallInstr(callResult, new MethAddr(fcallNode.getName()), 
                getSelf(s), args.toArray(new Operand[args.size()]), block);
        s.addInstr(callInstr);
        return callResult;
    }

    private Operand setupCallClosure(Node node, IRScope s) {
        if (node == null)
            return null;

        switch (node.getNodeType()) {
            case ITERNODE:
                return build((IterNode)node, s);
            case BLOCKPASSNODE:
                // SSS FIXME: We need to create a closure out of the named proc.
                //     Ex: a.map(&:id)
                // 1. if the value is a nil, pass a null block.
                // 2. if not a proc, call a toProc on it and pass it in
                //    (and, in cases where the object is a literal proc, the proc & toproc will cancel each other out!)
                // 3. if the value is a proc, pass it in.
                return build(((BlockPassNode)node).getBodyNode(), s);
            default:
                throw new NotCompilableException("ERROR: Encountered a method with a non-block, non-blockpass iter node at: " + node);
        }
    }

    public Operand buildFixnum(FixnumNode node, IRScope m) {
        return new Fixnum(node.getValue());
    }

/**
    public Operand buildFlip(Node node, IRScope m) {
        final FlipNode flipNode = (FlipNode) node;

        m.getVariableCompiler().retrieveLocalVariable(flipNode.getIndex(), flipNode.getDepth());

        if (flipNode.isExclusive()) {
            m.performBooleanBranch(new BranchCallback() {

                public void branch(IRScope m) {
                    build(flipNode.getEndNode(), m,true);
                    m.performBooleanBranch(new BranchCallback() {

                        public void branch(IRScope m) {
                            m.loadFalse();
                            m.getVariableCompiler().assignLocalVariable(flipNode.getIndex(), flipNode.getDepth(), false);
                        }
                    }, new BranchCallback() {

                        public void branch(IRScope m) {
                        }
                    });
                    m.loadTrue();
                }
            }, new BranchCallback() {

                public void branch(IRScope m) {
                    build(flipNode.getBeginNode(), m,true);
                    becomeTrueOrFalse(m);
                    m.getVariableCompiler().assignLocalVariable(flipNode.getIndex(), flipNode.getDepth(), true);
                }
            });
        } else {
            m.performBooleanBranch(new BranchCallback() {

                public void branch(IRScope m) {
                    build(flipNode.getEndNode(), m,true);
                    m.performBooleanBranch(new BranchCallback() {

                        public void branch(IRScope m) {
                            m.loadFalse();
                            m.getVariableCompiler().assignLocalVariable(flipNode.getIndex(), flipNode.getDepth(), false);
                        }
                    }, new BranchCallback() {

                        public void branch(IRScope m) {
                        }
                    });
                    m.loadTrue();
                }
            }, new BranchCallback() {

                public void branch(IRScope m) {
                    build(flipNode.getBeginNode(), m,true);
                    m.performBooleanBranch(new BranchCallback() {

                        public void branch(IRScope m) {
                            build(flipNode.getEndNode(), m,true);
                            flipTrueOrFalse(m);
                            m.getVariableCompiler().assignLocalVariable(flipNode.getIndex(), flipNode.getDepth(), false);
                            m.loadTrue();
                        }
                    }, new BranchCallback() {

                        public void branch(IRScope m) {
                            m.loadFalse();
                        }
                    });
                }
            });
        }
        // TODO: don't require pop
        if (!expr) m.consumeCurrentValue();
    }

    private void becomeTrueOrFalse(IRScope m) {
        m.performBooleanBranch(new BranchCallback() {

                    public void branch(IRScope m) {
                        m.loadTrue();
                    }
                }, new BranchCallback() {

                    public void branch(IRScope m) {
                        m.loadFalse();
                    }
                });
    }

    private void flipTrueOrFalse(IRScope m) {
        m.performBooleanBranch(new BranchCallback() {

                    public void branch(IRScope m) {
                        m.loadFalse();
                    }
                }, new BranchCallback() {

                    public void branch(IRScope m) {
                        m.loadTrue();
                    }
                });
    }
**/

    public Operand buildFloat(FloatNode node, IRScope m) {
        return new Float(node.getValue());
    }

    public Operand buildFor(ForNode forNode, IRExecutionScope m) {
        Variable ret      = m.getNewTemporaryVariable();
        Operand  receiver = build(forNode.getIterNode(), m);
        Operand  forBlock = buildForIter(forNode, m);     
        // SSS FIXME: Really?  Why the internal call?
        m.addInstr(new RubyInternalCallInstr(ret, MethAddr.FOR_EACH, receiver, new Operand[]{}, forBlock));
        return ret;
    }

    public Operand buildForIter(final ForNode forNode, IRExecutionScope s) {
            // Create a new closure context
        IRClosure closure = new IRClosure(s, forNode.getScope(), Arity.procArityOf(forNode.getVarNode()), forNode.getArgumentType());
        s.addClosure(closure);

            // Build args
        NodeType argsNodeId = null;
        if (forNode.getVarNode() != null) {
            argsNodeId = forNode.getVarNode().getNodeType();
            if (argsNodeId != null)
                buildBlockArgsAssignment(forNode.getVarNode(), closure, 1, false);
        }

            // Build closure body and return the result of the closure
        Operand closureRetVal = forNode.getBodyNode() == null ? Nil.NIL : build(forNode.getBodyNode(), closure);
        if (closureRetVal != null)  // can be null if the node is an if node with returns in both branches.
            closure.addInstr(new ClosureReturnInstr(closureRetVal));

        return new ClosureMetaObject(closure);
    }

    public Operand buildGlobalAsgn(GlobalAsgnNode globalAsgnNode, IRScope m) {
        Operand value = build(globalAsgnNode.getValueNode(), m);
        m.addInstr(new PutGlobalVarInstr(globalAsgnNode.getName(), value));
        return value;
    }

    public Operand buildGlobalVar(GlobalVarNode node, IRScope m) {
        Variable rv  = m.getNewTemporaryVariable();
        m.addInstr(new GetGlobalVariableInstr(rv, node.getName()));
        return rv;
    }

    public Operand buildHash(HashNode hashNode, IRScope m) {
        if (hashNode.getListNode() == null || hashNode.getListNode().size() == 0) {
            return new Hash(new ArrayList<KeyValuePair>());
        }
        else {
            int     i     = 0;
            Operand key   = null;
            Operand value = null;
            List<KeyValuePair> args = new ArrayList<KeyValuePair>();
            for (Node nextNode : hashNode.getListNode().childNodes()) {
                Operand v = build(nextNode, m);
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
    public Operand buildIf(final IfNode ifNode, IRScope s) {
        Node actualCondition = skipOverNewlines(s, ifNode.getCondition());

        Variable result     = s.getNewTemporaryVariable();
        Label    falseLabel = s.getNewLabel();
        Label    doneLabel  = s.getNewLabel();
        Operand  thenResult = null;
        s.addInstr(new BEQInstr(build(actualCondition, s), BooleanLiteral.FALSE, falseLabel));

        boolean thenNull = false;
        boolean elseNull = false;

        // Build the then part of the if-statement
        if (ifNode.getThenBody() != null) {
            thenResult = build(ifNode.getThenBody(), s);
            if (thenResult != null) { // thenResult can be null if then-body ended with a return!
                // Local optimization of break results to short-circuit the jump right away
                // rather than wait to do it during an optimization pass.
                Label tgt = doneLabel;
                if (thenResult instanceof BreakResult) {
                    BreakResult br = (BreakResult)thenResult;
                    thenResult = br._result;
                    tgt = br._jumpTarget;
                }
                s.addInstr(new CopyInstr(result, thenResult));
                s.addInstr(new JumpInstr(tgt));
            }
            else {
                thenNull = true;
            }
        }
        else {
            s.addInstr(new CopyInstr(result, Nil.NIL));
            s.addInstr(new JumpInstr(doneLabel));
        }

        // Build the else part of the if-statement
        s.addInstr(new LABEL_Instr(falseLabel));
        if (ifNode.getElseBody() != null) {
            Operand elseResult = build(ifNode.getElseBody(), s);
            if (elseResult != null) // elseResult can be null if then-body ended with a return!
                s.addInstr(new CopyInstr(result, elseResult));
            else
                elseNull = true;
        }
        else {
            s.addInstr(new CopyInstr(result, Nil.NIL));
        }

        if (thenNull && elseNull) {
            return null;
        }
        else {
            s.addInstr(new LABEL_Instr(doneLabel));
            return result;
        }
    }

    public Operand buildInstAsgn(final InstAsgnNode instAsgnNode, IRScope s) {
        Operand val = build(instAsgnNode.getValueNode(), s);
        // NOTE: if 's' happens to the a class, this is effectively an assignment of a class instance variable
        s.addInstr(new PutFieldInstr(getSelf(s), instAsgnNode.getName(), val));
        return val;
    }

    public Operand buildInstVar(InstVarNode node, IRScope m) {
        Variable ret = m.getNewTemporaryVariable();
        m.addInstr(new GetFieldInstr(ret, getSelf(m), node.getName()));
        return ret;
    }

    public Operand buildIter(final IterNode iterNode, IRExecutionScope s) {
        IRClosure closure = new IRClosure(s, iterNode.getScope(), Arity.procArityOf(iterNode.getVarNode()), iterNode.getArgumentType());
        s.addClosure(closure);

            // Build args
        NodeType argsNodeId = BlockBody.getArgumentTypeWackyHack(iterNode);
        if ((iterNode.getVarNode() != null) && (argsNodeId != null))
            buildBlockArgsAssignment(iterNode.getVarNode(), closure, 1, false);

            // Build closure body and return the result of the closure
        Operand closureRetVal = iterNode.getBodyNode() == null ? Nil.NIL : build(iterNode.getBodyNode(), closure);
        if (closureRetVal != null)  // can be null if the node is an if node with returns in both branches.
            closure.addInstr(new ClosureReturnInstr(closureRetVal));

        return new ClosureMetaObject(closure);
    }

    public Operand buildLocalAsgn(LocalAsgnNode localAsgnNode, IRScope s) {
        Operand value = build(localAsgnNode.getValueNode(), s);
        s.addInstr(new CopyInstr(s.getLocalVariable(localAsgnNode.getName()), value));

        return value;
    }

    public Operand buildLocalVar(LocalVarNode node, IRScope s) {
        return s.getLocalVariable(node.getName());
    }

    public Operand buildMatch(MatchNode matchNode, IRScope m) {
        Operand regexp = build(matchNode.getRegexpNode(), m);
        return generateJRubyUtilityCall(m, MethAddr.MATCH, regexp, new Operand[]{});
    }

    public Operand buildMatch2(Match2Node matchNode, IRScope m) {
        Operand receiver = build(matchNode.getReceiverNode(), m);
        Operand value    = build(matchNode.getValueNode(), m);
        return generateJRubyUtilityCall(m, MethAddr.MATCH2, receiver, new Operand[]{value});
    }

    public Operand buildMatch3(Match3Node matchNode, IRScope m) {
        Operand receiver = build(matchNode.getReceiverNode(), m);
        Operand value    = build(matchNode.getValueNode(), m);
        return generateJRubyUtilityCall(m, MethAddr.MATCH3, receiver, new Operand[]{value});
    }

    public Operand buildModule(ModuleNode moduleNode, IRScope s) {
        final Colon3Node cpathNode  = moduleNode.getCPath();

        // By default, the container for this class is 's'
        Operand container = null;

        // Get the container for this new module
        if (cpathNode instanceof Colon2Node) {
            Node leftNode = ((Colon2Node) cpathNode).getLeftNode();
            if (leftNode != null)
                container = build(leftNode, s);
        } else if (cpathNode instanceof Colon3Node) {
            container = new ClassMetaObject(IRClass.getCoreClass("Object"));
        }

        // Build the new module
        String    moduleName = moduleNode.getCPath().getName();
        IRModule m = new IRModule(s, container, moduleName, moduleNode.getScope());
        s.addModule(m);
        if (container != null)
            s.addInstr(new PutConstInstr(container, moduleName, new ModuleMetaObject(m)));

        // Build the module body
        if (moduleNode.getBodyNode() != null)
            build(moduleNode.getBodyNode(), m.getRootMethod());

        return null;
    }

    public Operand buildMultipleAsgn(MultipleAsgnNode multipleAsgnNode, IRScope s) {
        Operand  values = build(multipleAsgnNode.getValueNode(), s);
        Variable ret = s.getNewTemporaryVariable();
        s.addInstr(new CopyInstr(ret, values));
        buildMultipleAsgnAssignment(multipleAsgnNode, s, ret);
        return ret;
    }

    public void buildMultipleAsgnAssignment(final MultipleAsgnNode multipleAsgnNode, IRScope s, Operand values) {
        final ListNode sourceArray = multipleAsgnNode.getHeadNode();

        // First, build assignments for specific named arguments
        int i = 0; 
        if (sourceArray != null) {
            ListNode headNode = (ListNode) sourceArray;
            for (Node an: headNode.childNodes()) {
                if (values == null) {
                    buildBlockArgsAssignment(an, s, i + 1, false); // Add 1 since 0 is self for argument processing
                } else {
                    buildAssignment(an, s, values, i, false);
                }
                i++;
            }
        }

        // First, build an assignment for a splat, if any, with the rest of the args!
        Node an = multipleAsgnNode.getArgsNode();
        if (an == null) {
            if (sourceArray == null)
                throw new NotCompilableException("Something's wrong, multiple assignment with no head or args at: " + multipleAsgnNode.getPosition());
        } 
        else if (an instanceof StarNode) {
            // do nothing
        } 
        else if (values != null) {
            buildAssignment(an, s, values, i, true); // rest of the argument array!
        }
        else {
            buildBlockArgsAssignment(an, s, i, true); // rest of the argument array!
        }

    }

    public Operand buildNewline(NewlineNode node, IRScope s) {
        return build(skipOverNewlines(s, node), s);
    }

    public Operand buildNext(final NextNode nextNode, IRExecutionScope s) {
        Operand rv = (nextNode.getValueNode() == null) ? Nil.NIL : build(nextNode.getValueNode(), s);
        // SSS FIXME: 1. Is the ordering correct? (poll before next)
        s.addInstr(new ThreadPollInstr());
        // If a closure, the next is simply a return from the closure!
        // If a regular loop, the next is simply a jump to the end of the iteration
        s.addInstr((s instanceof IRClosure) ? new ClosureReturnInstr(rv) : new JumpInstr(s.getCurrentLoop().iterEndLabel));
        return rv;
    }

    public Operand buildNthRef(NthRefNode nthRefNode, IRScope m) {
        return new NthRef(nthRefNode.getMatchNumber());
    }

    public Operand buildNil(Node node, IRScope m) {
        m.addInstr(new ThreadPollInstr());
        return Nil.NIL;
    }

    public Operand buildNot(NotNode node, IRScope m) {
        Variable ret = m.getNewTemporaryVariable();
        m.addInstr(new NotInstr(ret, build(node.getConditionNode(), m)));
        return ret;
    }

    public Operand buildOpAsgn(OpAsgnNode opAsgnNode, IRScope s) {
        if (opAsgnNode.getOperatorName().equals("||") || opAsgnNode.getOperatorName().equals("&&")) {
            throw new NotCompilableException("Unknown node encountered in builder: " + opAsgnNode);
        }
        
        // get attr
        Operand  v1 = build(opAsgnNode.getReceiverNode(), s);
        Variable      getResult   = s.getNewTemporaryVariable();
        Instr callInstr = new CallInstr(getResult, new MethAddr(opAsgnNode.getVariableName()), v1,
                new Operand[]{}, null);
        s.addInstr(callInstr);

        // call operator
        Operand  v2 = build(opAsgnNode.getValueNode(), s);
        Variable      setValue   = s.getNewTemporaryVariable();
        callInstr = new CallInstr(setValue, new MethAddr(opAsgnNode.getOperatorName()), getResult,
                new Operand[]{v2}, null);
        s.addInstr(callInstr);

        // set attr
        Variable      setResult   = s.getNewTemporaryVariable();
        callInstr    = new CallInstr(setResult, new MethAddr(opAsgnNode.getVariableNameAsgn()),
                v1, new Operand[] {setValue}, null);
        s.addInstr(callInstr);

        return setResult;
    }

    // Translate "x &&= y" --> "x = y if is_true(x)" -->
    // 
    //    x = -- build(x) should return a variable! --
    //    f = is_true(x)
    //    beq(f, false, L)
    //    x = -- build(y) --
    // L:
    //
    public Operand buildOpAsgnAnd(OpAsgnAndNode andNode, IRScope s) {
        Label    l  = s.getNewLabel();
        Operand  v1 = build(andNode.getFirstNode(), s);
        Variable f  = s.getNewTemporaryVariable();
        s.addInstr(new IsTrueInstr(f, v1));
        s.addInstr(new BEQInstr(f, BooleanLiteral.FALSE, l));
        build(andNode.getSecondNode(), s);  // This does the assignment!
        s.addInstr(new LABEL_Instr(l));
        s.addInstr(new ThreadPollInstr());
        return v1;
    }

    // Translate "x ||= y" --> "x = (is_defined(x) && is_true(x) ? x : y)" -->
    // 
    //    v = -- build(x) should return a variable! --
    //    f = is_true(v)
    //    beq(f, true, L)
    //    -- build(x = y) --
    // L:
    //
    public Operand buildOpAsgnOr(final OpAsgnOrNode orNode, IRScope s) {
        Label    l1 = s.getNewLabel();
        Label    l2 = null;
        Variable f = s.getNewTemporaryVariable();
        Operand  v1;
        boolean  needsDefnCheck = needsDefinitionCheck(orNode.getFirstNode());
        if (needsDefnCheck) {
            l2 = s.getNewLabel();
            v1 = buildGetDefinitionBase(orNode.getFirstNode(), s);
            s.addInstr(new BEQInstr(v1, Nil.NIL, l2)); // if v1 is undefined, go to v2's computation
        }
        v1 = build(orNode.getFirstNode(), s); // build of 'x'
        s.addInstr(new IsTrueInstr(f, v1));
        s.addInstr(new BEQInstr(f, BooleanLiteral.TRUE, l1));  // if v1 is defined and true, we are done! 
        if (needsDefnCheck) {
            s.addInstr(new LABEL_Instr(l2));
        }
        build(orNode.getSecondNode(), s); // This is an AST node that sets x = y, so nothing special to do here.
        s.addInstr(new LABEL_Instr(l1));
        s.addInstr(new ThreadPollInstr());

        // Return value of x ||= y is always 'x'
        return v1;
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

/**
    public Operand buildOpAsgn(Node node, IRScope m) {
        final OpAsgnNode opAsgnNode = (OpAsgnNode) node;

        Operand ret;
        if (opAsgnNode.getOperatorName().equals("||")) {
            ret = buildOpAsgnWithOr(opAsgnNode, m);
        } else if (opAsgnNode.getOperatorName().equals("&&")) {
            ret = buildOpAsgnWithAnd(opAsgnNode, m);
        } else {
            ret = buildOpAsgnWithMethod(opAsgnNode, m);
        }

        m.addInstr(new ThreadPollInstr());
        return ret;
    }

    public Operand buildOpAsgnWithOr(Node node, IRScope s) {
        final OpAsgnNode opAsgnNode = (OpAsgnNode) node;
        Operand receiver = build(opAsgnNode.getReceiverNode(), s); // [recv]
        List<Operand> args = setupCallArgs(opAsgnNode.getValueNode(), s);
        m.getInvocationCompiler().invokeOpAsgnWithOr(opAsgnNode.getVariableName(), opAsgnNode.getVariableNameAsgn(), receiverCallback, argsCallback);
    }

    public Operand buildOpAsgnWithAnd(Node node, IRScope s) {
        final OpAsgnNode opAsgnNode = (OpAsgnNode) node;
        Operand receiver = build(opAsgnNode.getReceiverNode(), s); // [recv]
        List<Operand> args = setupCallArgs(opAsgnNode.getValueNode(), s);
        m.getInvocationCompiler().invokeOpAsgnWithAnd(opAsgnNode.getVariableName(), opAsgnNode.getVariableNameAsgn(), receiverCallback, argsCallback);
    }

    public Operand buildOpAsgnWithMethod(Node node, IRScope s) {
        final OpAsgnNode opAsgnNode = (OpAsgnNode) node;
        Operand receiver = build(opAsgnNode.getReceiverNode(), s); // [recv]
        // eval new value, call operator on old value, and assign
        Operand val = build(opAsgnNode.getValueNode(), m, true);
        m.getInvocationCompiler().invokeOpAsgnWithMethod(opAsgnNode.getOperatorName(), opAsgnNode.getVariableName(), opAsgnNode.getVariableNameAsgn(), receiverCallback, argsCallback);
    }
**/

    public Operand buildOpElementAsgn(Node node, IRScope m) {
        final OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode) node;
        
        if (opElementAsgnNode.getOperatorName() == "||") {
            return buildOpElementAsgnWithOr(node, m);
        } else if (opElementAsgnNode.getOperatorName() == "&&") {
            return buildOpElementAsgnWithAnd(node, m);
        } else {
            return buildOpElementAsgnWithMethod(node, m);
        }
    }
    
    /**
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

        public void call(IRScope m) {
            if (getArity() == 1) {
                // if arity 1, just build the one element to save us the array cost
                build(((ArrayNode)node).get(0), m,true);
            } else {
                // build into array
                buildArguments(node, m);
            }
        }
    };
*/

    // Translate "a[x] ||= n" --> "a[x] = n if !is_true(a[x])"
    // 
    //    tmp = build(a) <-- receiver
    //    arg = build(x) <-- args
    //    val = buildCall([], tmp, arg)
    //    f = is_true(val)
    //    beq(f, true, L)
    //    val = build(n) <-- val
    //    buildCall([]= tmp, arg, val)
    // L:
    //
    public Operand buildOpElementAsgnWithOr(Node node, IRScope s) {
        final OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode) node;
        Operand array = build(opElementAsgnNode.getReceiverNode(), s);
        List<Operand> args = setupCallArgs(opElementAsgnNode.getArgsNode(), s);
        Label    l     = s.getNewLabel();
        Variable elt   = s.getNewTemporaryVariable();
        Variable f     = s.getNewTemporaryVariable();
        Operand[] allArgs = new Operand[args.size()];
        int i = 0;
        for (Operand x: args) {
            allArgs[i] = x;
            i++;
        }
        s.addInstr(new CallInstr(elt, new MethAddr("[]"), array, allArgs, null));
        s.addInstr(new IsTrueInstr(f, elt));
        s.addInstr(new BEQInstr(f, BooleanLiteral.TRUE, l));
        Operand value = build(opElementAsgnNode.getValueNode(), s);
        allArgs = new Operand[args.size()+1];
        i = 0;
        for (Operand x: args) {
            allArgs[i] = x;
            i++;
        }
        allArgs[i] = value;
        s.addInstr(new CallInstr(elt, new MethAddr("[]="), array, allArgs, null));
        s.addInstr(new CopyInstr(elt, value));
        s.addInstr(new LABEL_Instr(l));
        return elt;
    }

    // Translate "a[x] &&= n" --> "a[x] = n if is_true(a[x])"
    public Operand buildOpElementAsgnWithAnd(Node node, IRScope s) {
        final OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode) node;
        Operand array = build(opElementAsgnNode.getReceiverNode(), s);
        List<Operand> args = setupCallArgs(opElementAsgnNode.getArgsNode(), s);
        Label    l     = s.getNewLabel();
        Variable elt   = s.getNewTemporaryVariable();
        Variable f     = s.getNewTemporaryVariable();
        Operand[] allArgs = new Operand[args.size()];
        int i = 0;
        for (Operand x: args) {
            allArgs[i] = x;
            i++;
        }
        s.addInstr(new CallInstr(elt, new MethAddr("[]"), array, allArgs, null));
        s.addInstr(new IsTrueInstr(f, elt));
        s.addInstr(new BEQInstr(f, BooleanLiteral.FALSE, l));
        Operand value = build(opElementAsgnNode.getValueNode(), s);
        allArgs = new Operand[args.size()+1];
        i = 0;
        for (Operand x: args) {
            allArgs[i] = x;
            i++;
        }
        allArgs[i] = value;
        s.addInstr(new CallInstr(elt, new MethAddr("[]="), array, allArgs, null));
        s.addInstr(new CopyInstr(elt, value));
        s.addInstr(new LABEL_Instr(l));
        return elt;
    }

    // a[i] *= n, etc.  anything that is not "a[i] &&= .. or a[i] ||= .."
    public Operand buildOpElementAsgnWithMethod(Node node, IRScope s) {
    // SSS FIXME: Incorrect -- this is just a copy of the OR case
        return buildOpElementAsgnWithOr(node, s);
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
    public Operand buildOr(final OrNode orNode, IRScope m) {
        if (orNode.getFirstNode().getNodeType().alwaysTrue()) {
            // build first node only and return true
            build(orNode.getFirstNode(), m);
            return BooleanLiteral.TRUE;
        } else if (orNode.getFirstNode().getNodeType().alwaysFalse()) {
            // build first node as non-expr and build second node
            build(orNode.getFirstNode(), m);
            return build(orNode.getSecondNode(), m);
        } else {
            Variable ret = m.getNewTemporaryVariable();
            Label    l   = m.getNewLabel();
            Operand  v1  = build(orNode.getFirstNode(), m);
            m.addInstr(new CopyInstr(ret, BooleanLiteral.TRUE));
            m.addInstr(new BEQInstr(v1, BooleanLiteral.TRUE, l));
            Operand  v2  = build(orNode.getSecondNode(), m);
            m.addInstr(new CopyInstr(ret, v2));
            m.addInstr(new LABEL_Instr(l));
            return ret;
        }
    }

/**
    public Operand buildPostExe(Node node, IRScope m) {
        final PostExeNode postExeNode = (PostExeNode) node;

        // create the closure class and instantiate it
        final CompilerCallback closureBody = new CompilerCallback() {

                    public void call(IRScope m) {
                        if (postExeNode.getBodyNode() != null) {
                            build(postExeNode.getBodyNode(), m, true);
                        } else {
                            m.loadNil();
                        }
                    }
                };
        m.createNewEndBlock(closureBody);
    }

    public Operand buildPreExe(Node node, IRScope m) {
        final PreExeNode preExeNode = (PreExeNode) node;

        // create the closure class and instantiate it
        final CompilerCallback closureBody = new CompilerCallback() {

                    public void call(IRScope m) {
                        if (preExeNode.getBodyNode() != null) {
                            build(preExeNode.getBodyNode(), m,true);
                        } else {
                            m.loadNil();
                        }
                    }
                };
        m.runBeginBlock(preExeNode.getScope(), closureBody);
    }
**/

    public Operand buildRedo(Node node, IRExecutionScope s) {
        // For closures, a redo is a jump to the beginning of the closure
        // For non-closures, a redo is a jump to the beginning of the loop
        s.addInstr(new JumpInstr((s instanceof IRClosure) ? ((IRClosure)s).startLabel : s.getCurrentLoop().iterStartLabel));
        return Nil.NIL;
    }

    public Operand buildRegexp(RegexpNode reNode, IRScope m) {
        return new Regexp(new StringLiteral(reNode.getValue()), reNode.getOptions());
    }

    public Operand buildRescue(Node node, IRScope m) {
        return buildRescueInternal(node, m);
    }

    private Operand buildRescueInternal(Node node, IRScope m) {
        final RescueNode rescueNode = (RescueNode) node;
        boolean noEnsure    = _ensureBlockStack.empty();
        Label   rBeginLabel = m.getNewLabel();  // Label marking start of the begin-rescue(-ensure)-end block
        Label   rEndLabel   = noEnsure ? m.getNewLabel() : _ensureBlockStack.peek().end; // Label marking end of the begin-rescue(-ensure)-end block
        Label   elseLabel   = rescueNode.getElseNode() == null ? null : m.getNewLabel();

        // Placeholder rescue instruction that tells rest of the compiler passes the boundaries of the rescue block.
        List<Label> rescueBlockLabels = new ArrayList<Label>();
        m.addInstr(new LABEL_Instr(rBeginLabel));
        RESCUED_BODY_START_MARKER_Instr rbStartInstr = new RESCUED_BODY_START_MARKER_Instr(rBeginLabel, elseLabel, rEndLabel, rescueBlockLabels);
        m.addInstr(rbStartInstr);

        // Body
        Operand tmp = Nil.NIL;  // default return value if for some strange reason, we neither have the body node or the else node!
        Variable rv = m.getNewTemporaryVariable();
        if (rescueNode.getBodyNode() != null)
            tmp = build(rescueNode.getBodyNode(), m);

        // Else part of the body -- we simply fall through from the main body if there were no exceptions
        if (elseLabel != null) {
            m.addInstr(new LABEL_Instr(elseLabel));
            tmp = build(rescueNode.getElseNode(), m);
        }

        if (tmp != null) {
            m.addInstr(new CopyInstr(rv, tmp));

            // No explicit return from the protected body
            // - If we dont have any ensure blocks, simply jump to the end of the rescue block
            // - If we do, get the innermost ensure block, set up the return address to the end of the ensure block, and go execute the ensure code.
            if (noEnsure) {
                m.addInstr(new JumpInstr(rEndLabel));
            }
            else {
                EnsureBlockInfo ebi = _ensureBlockStack.peek();
                ebi.endLabelNeeded = true;
                ebi.noFallThru = true;
                m.addInstr(new SET_RETADDR_Instr(ebi.returnAddr, ebi.end));
                m.addInstr(new JumpInstr(ebi.start));
            }
        }
        else {
            rv = null;
        }

        // Since rescued regions are well nested within Ruby, this bare marker is sufficient to
        // let us discover the edge of the region during linear traversal of instructions during cfg construction.
        RESCUED_BODY_END_MARKER_Instr rbEndInstr = new RESCUED_BODY_END_MARKER_Instr();
        m.addInstr(rbEndInstr);

        // Build the actual rescue block(s)
        Label rbLabel = m.getNewLabel(); // Label marking start of the first rescue code.
        rescueBlockLabels.add(rbLabel);
        m.addInstr(new LABEL_Instr(rbLabel));
        buildRescueBodyInternal(m, rescueNode.getRescueNode(), rv, rEndLabel, rescueBlockLabels);

        // End label -- only if there is no ensure block!  With an ensure block, you end at ensureEndLabel.
        if (noEnsure)
            m.addInstr(new LABEL_Instr(rEndLabel));

        return rv;
    }

    private void buildRescueBodyInternal(IRScope m, Node node, Variable rv, Label endLabel, List<Label> rescueBlockLabels) {
        final RescueBodyNode rescueBodyNode = (RescueBodyNode) node;
        final Node exceptionList = rescueBodyNode.getExceptionNodes();
        boolean haveEnsureBlocks = !_ensureBlockStack.empty();

        // Load exception & exception comparison type
        Variable exc = m.getNewTemporaryVariable();
        m.addInstr(new RECV_EXCEPTION_Instr(exc));
        // Compute all elements of the exception array eagerly
        Operand excType = (exceptionList == null) ? null : build(exceptionList, m);

        // Compare and branch as necessary!
        Label uncaughtLabel = null;
        if (excType != null) {
            uncaughtLabel = m.getNewLabel();
            Variable eqqResult = m.getNewTemporaryVariable();
            m.addInstr(new EQQ_Instr(eqqResult, exc, excType));
            m.addInstr(new BEQInstr(eqqResult, BooleanLiteral.FALSE, uncaughtLabel));
        }

        // Caught exception case -- build rescue body
        Node realBody = skipOverNewlines(m, rescueBodyNode.getBodyNode());
        Operand x = build(realBody, m);
        if (x != null) { // can be null if the rescue block has an explicit return
            m.addInstr(new CopyInstr(rv, x));
            // Jump to end of rescue block since we've caught and processed the exception
            if (haveEnsureBlocks) {
                EnsureBlockInfo ebi = _ensureBlockStack.peek();
                ebi.endLabelNeeded = true;
                ebi.noFallThru = true;
                m.addInstr(new SET_RETADDR_Instr(ebi.returnAddr, ebi.end));
                m.addInstr(new JumpInstr(ebi.start));
            }
            else {
                m.addInstr(new JumpInstr(endLabel));
            }
        }

        // Uncaught exception -- build other rescue nodes or rethrow!
        if (uncaughtLabel != null) {
            rescueBlockLabels.add(uncaughtLabel);
            m.addInstr(new LABEL_Instr(uncaughtLabel));
            if (rescueBodyNode.getOptRescueNode() != null) {
                buildRescueBodyInternal(m, rescueBodyNode.getOptRescueNode(), rv, endLabel, rescueBlockLabels);
            } else {
                // If we have ensure blocks, set up a chain of jumps to execute all the ensure blocks
                if (haveEnsureBlocks)
                    EnsureBlockInfo.emitJumpChain(m, _ensureBlockStack);
                m.addInstr(new THROW_EXCEPTION_Instr(exc));
            }
        }
    }

    public Operand buildRetry(Node node, IRScope s) {
        // JRuby only supports retry when present in rescue blocks!
        // 1.9 doesn't support retry anywhere else.
        s.addInstr(new ThreadPollInstr());

        // Jump back to the innermost rescue block
        // We either find it, or we add code to throw a runtime exception
        if (_rescueBlockLabelStack.empty()) {
            StringLiteral exc = new StringLiteral("retry found outside of rescue clause!");
            s.addInstr(new THROW_EXCEPTION_Instr(exc));
        }
        else {
            s.addInstr(new JumpInstr(_rescueBlockLabelStack.peek()));
        }
        return Nil.NIL;
    }

    public Operand buildReturn(ReturnNode returnNode, IRScope m) {
        Operand retVal = (returnNode.getValueNode() == null) ? Nil.NIL : build(returnNode.getValueNode(), m);
        // Before we return, have to go execute all the ensure blocks
        if (!_ensureBlockStack.empty())
            EnsureBlockInfo.emitJumpChain(m, _ensureBlockStack);
        m.addInstr(new ReturnInstr(retVal));
        return null;
    }

    public IRScope buildRoot(RootNode rootNode) {
        String file = rootNode.getPosition().getFile();
        StaticScope staticScope = rootNode.getStaticScope();

        // Top-level script!
        IRScript script = new IRScript("__file__", file, rootNode.getStaticScope());
        IRClass  rootClass = script.getRootClass();
        IRMethod rootMethod = rootClass.getRootMethod();

        // Debug info: record file name
        rootMethod.addInstr(new FilenameInstr(file));

        // add a "self" recv here
        // TODO: is this right?
        rootMethod.addInstr(new ReceiveSelfInstruction(getSelf(rootMethod)));

        build(rootNode.getBodyNode(), rootMethod);

        return script;
    }

    private Variable getSelf(IRScope s) {
        return ((IRExecutionScope)s).getSelf();
    }

    public Operand buildSelf(Node node, IRScope s) {
        return getSelf(s);
    }

    public Operand buildSplat(SplatNode splatNode, IRScope s) {
        return new Splat(build(splatNode.getValue(), s));
    }

    public Operand buildStr(StrNode strNode, IRScope s) {
        return new StringLiteral(strNode.getValue());
    }

    public Operand buildSuper(SuperNode superNode, IRScope s) {
        List<Operand> args  = setupCallArgs(superNode.getArgsNode(), s);
        Operand       block = setupCallClosure(superNode.getIterNode(), s);
        Variable      ret   = s.getNewTemporaryVariable();
        s.addInstr(new RubyInternalCallInstr(ret, MethAddr.SUPER, getSelf(s),
                args.toArray(new Operand[args.size()]), block));
        return ret;
    }

    public Operand buildSValue(SValueNode node, IRScope s) {
        return new SValue(build(node.getValue(), s));
    }

    public Operand buildSymbol(SymbolNode node, IRScope s) {
        return new Symbol(node.getName());
    }    

    public Operand buildToAry(ToAryNode node, IRScope s) {
        Operand  array = build(node.getValue(), s);
        return generateJRubyUtilityCall(s, MethAddr.TO_ARY, array, new Operand[]{});
    }

    public Operand buildTrue(Node node, IRScope m) {
        m.addInstr(new ThreadPollInstr());
        return BooleanLiteral.TRUE; 
    }

    public Operand buildUndef(Node node, IRScope m) {
        Operand methName = build(((UndefNode) node).getName(), m);
        return generateJRubyUtilityCall(m, MethAddr.UNDEF_METHOD, methName, new Operand[]{});
    }

    private Operand buildConditionalLoop(IRExecutionScope s, Node conditionNode, Node bodyNode, boolean isWhile, boolean isLoopHeadCondition)
    {
        if (isLoopHeadCondition && (   (isWhile && conditionNode.getNodeType().alwaysFalse()) 
                                    || (!isWhile && conditionNode.getNodeType().alwaysTrue())))
        {
            // we won't enter the loop -- just build the condition node
            build(conditionNode, s);
            return Nil.NIL;
        } 
        else {
            IRLoop loop = new IRLoop(s);
            s.startLoop(loop);
            s.addInstr(new LABEL_Instr(loop.loopStartLabel));

            if (isLoopHeadCondition) {
                Operand cv = build(conditionNode, s);
                s.addInstr(new BEQInstr(cv, isWhile ? BooleanLiteral.FALSE : BooleanLiteral.TRUE, loop.loopEndLabel));
            }
            s.addInstr(new LABEL_Instr(loop.iterStartLabel));

            // Looks like while can be treated as an expression!
            // So, capture the result of the body so that it can be returned.
            Variable whileResult = null;
            if (bodyNode != null) {
                Operand v = build(bodyNode, s);
                if (v != null) {
                    whileResult = s.getNewTemporaryVariable();
                    s.addInstr(new CopyInstr(whileResult, v));
                }
            }

                // SSS FIXME: Is this correctly placed ... at the end of the loop iteration?
            s.addInstr(new ThreadPollInstr());

            s.addInstr(new LABEL_Instr(loop.iterEndLabel));
            if (isLoopHeadCondition) {
                // Issue a jump back to the head of the while loop
                s.addInstr(new JumpInstr(loop.loopStartLabel));
            }
            else {
                Operand cv = build(conditionNode, s);
                s.addInstr(new BEQInstr(cv, isWhile ? BooleanLiteral.TRUE : BooleanLiteral.FALSE, loop.iterStartLabel));
            }

            s.addInstr(new LABEL_Instr(loop.loopEndLabel));
            s.endLoop(loop);

            return whileResult;
        }
    }

    public Operand buildUntil(final UntilNode untilNode, IRExecutionScope s) {
        return buildConditionalLoop(s, untilNode.getConditionNode(), untilNode.getBodyNode(), false, untilNode.evaluateAtStart());
    }

    // SSS FIXME: Got a little lazy?  We could/should define a special instruction ALIAS_GLOBAL_VAR_Instr probably
    // Is this a ruby-internals or a jruby-internals call?
    public Operand buildVAlias(Node node, IRScope m) {
        VAliasNode valiasNode = (VAliasNode) node;
        Operand[] args = new Operand[] { new StringLiteral(valiasNode.getOldName()) };
        m.addInstr(new RubyInternalCallInstr(null, MethAddr.GVAR_ALIAS, new StringLiteral(valiasNode.getNewName()), args));
        return Nil.NIL;
    }

    public Operand buildVCall(VCallNode node, IRScope s) {
        List<Operand> args       = new ArrayList<Operand>(); args.add(getSelf(s));
        Variable      callResult = s.getNewTemporaryVariable();
        Instr      callInstr  = new CallInstr(callResult, new MethAddr(node.getName()), getSelf(s), new Operand[]{}, null);
        s.addInstr(callInstr);
        return callResult;
    }

    public Operand buildWhile(final WhileNode whileNode, IRExecutionScope s) {
        return buildConditionalLoop(s, whileNode.getConditionNode(), whileNode.getBodyNode(), true, whileNode.evaluateAtStart());
    }

    public Operand buildXStr(XStrNode node, IRScope m) {
        return new BacktickString(new StringLiteral(node.getValue()));
    }

    private List<Operand> setupYieldArgs(Node args, IRScope s) {
        List<Operand> argsList = new ArrayList<Operand>();
        if (args != null) {
           // unwrap newline nodes to get their actual type
           args = skipOverNewlines(s, args);
           buildArgs(argsList, args, s);
        }

        return argsList;
    }

    public Operand buildYield(YieldNode node, IRScope s) {
        List<Operand> args = setupYieldArgs(node.getArgsNode(), s);
        Variable      ret  = s.getNewTemporaryVariable();
        s.addInstr(new YieldInstr(ret, args.toArray(new Operand[args.size()])));
        return ret;
    }

    public Operand buildZArray(Node node, IRScope m) {
       return new Array();
    }

    public Operand buildZSuper(ZSuperNode zsuperNode, IRScope s) {
        Operand    block = setupCallClosure(zsuperNode.getIterNode(), s);
        Variable   ret   = s.getNewTemporaryVariable();
        s.addInstr(new RubyInternalCallInstr(ret, MethAddr.ZSUPER, getSelf(s),
                ((IRMethod)s).getCallArgs(), block));
        return ret;
    }

    public void buildArgsCatArguments(List<Operand> args, ArgsCatNode argsCatNode, IRScope s) {
        Operand v1 = build(argsCatNode.getFirstNode(), s);
        Operand v2 = build(argsCatNode.getSecondNode(), s);
        args.add(new CompoundArray(v1, v2));
    }

    public void buildArgsPushArguments(List<Operand> args, ArgsPushNode argsPushNode, IRScope m) {
        Operand a = new Array(new Operand[]{ build(argsPushNode.getFirstNode(), m), build(argsPushNode.getSecondNode(), m) });
        args.add(a);
    }

    public void buildArrayArguments(List<Operand> args, Node node, IRScope s) {
        // SSS FIXME: Where does this go?
        // m.setLinePosition(arrayNode.getPosition());
        args.add(buildArray(node, s));
    }

    public void buildSplatArguments(List<Operand> args, SplatNode node, IRScope s) {
        args.add(buildSplat(node, s));
    }
}
