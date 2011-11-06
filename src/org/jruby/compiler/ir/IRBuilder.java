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
import org.jruby.RubyInstanceConfig.CompileMode;
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
import org.jruby.ast.FlipNode;
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
import org.jruby.ast.LiteralNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.PreExeNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.StarNode;
import org.jruby.ast.ToAryNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.WhenNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.ir.compiler_pass.AddBindingInstructions;
import org.jruby.compiler.ir.compiler_pass.CFGBuilder;
import org.jruby.compiler.ir.compiler_pass.IRPrinter;
import org.jruby.compiler.ir.compiler_pass.InlineTest;
import org.jruby.compiler.ir.compiler_pass.LinearizeCFG;
import org.jruby.compiler.ir.compiler_pass.LiveVariableAnalysis;
import org.jruby.compiler.ir.compiler_pass.opts.DeadCodeElimination;
import org.jruby.compiler.ir.compiler_pass.opts.LocalOptimizationPass;
import org.jruby.compiler.ir.instructions.AttrAssignInstr;
import org.jruby.compiler.ir.instructions.BEQInstr;
import org.jruby.compiler.ir.instructions.BNEInstr;
import org.jruby.compiler.ir.instructions.BreakInstr;
import org.jruby.compiler.ir.instructions.CallInstr;
import org.jruby.compiler.ir.instructions.ConstMissingInstr;
import org.jruby.compiler.ir.instructions.ClosureReturnInstr;
import org.jruby.compiler.ir.instructions.CopyInstr;
import org.jruby.compiler.ir.instructions.DefineClassInstr;
import org.jruby.compiler.ir.instructions.DefineClassMethodInstr;
import org.jruby.compiler.ir.instructions.DefineMetaClassInstr;
import org.jruby.compiler.ir.instructions.DefineInstanceMethodInstr;
import org.jruby.compiler.ir.instructions.DefineModuleInstr;
import org.jruby.compiler.ir.instructions.EQQInstr;
import org.jruby.compiler.ir.instructions.EnsureRubyArrayInstr;
import org.jruby.compiler.ir.instructions.ExceptionRegionStartMarkerInstr;
import org.jruby.compiler.ir.instructions.ExceptionRegionEndMarkerInstr;
import org.jruby.compiler.ir.instructions.FilenameInstr;
import org.jruby.compiler.ir.instructions.GetArrayInstr;
import org.jruby.compiler.ir.instructions.InstanceOfInstr;
import org.jruby.compiler.ir.instructions.GetClassVariableInstr;
import org.jruby.compiler.ir.instructions.GetClassVarContainerModuleInstr;
import org.jruby.compiler.ir.instructions.GetConstInstr;
import org.jruby.compiler.ir.instructions.GetFieldInstr;
import org.jruby.compiler.ir.instructions.GetGlobalVariableInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.IsTrueInstr;
import org.jruby.compiler.ir.instructions.JRubyImplCallInstr;
import org.jruby.compiler.ir.instructions.JRubyImplCallInstr.JRubyImplementationMethod;
import org.jruby.compiler.ir.instructions.JumpInstr;
import org.jruby.compiler.ir.instructions.JumpIndirectInstr;
import org.jruby.compiler.ir.instructions.LabelInstr;
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
import org.jruby.compiler.ir.instructions.ReceiveExceptionInstr;
import org.jruby.compiler.ir.instructions.ReceiveOptionalArgumentInstr;
import org.jruby.compiler.ir.instructions.RescueEQQInstr;
import org.jruby.compiler.ir.instructions.ReturnInstr;
import org.jruby.compiler.ir.instructions.RubyInternalCallInstr;
import org.jruby.compiler.ir.instructions.RubyInternalCallInstr.RubyInternalsMethod;
import org.jruby.compiler.ir.instructions.SetReturnAddressInstr;
import org.jruby.compiler.ir.instructions.SetArgumentsInstr;
import org.jruby.compiler.ir.instructions.SearchConstInstr;
import org.jruby.compiler.ir.instructions.SuperInstr;
import org.jruby.compiler.ir.instructions.ThreadPollInstr;
import org.jruby.compiler.ir.instructions.ThrowExceptionInstr;
import org.jruby.compiler.ir.instructions.YieldInstr;
import org.jruby.compiler.ir.operands.Array;
import org.jruby.compiler.ir.operands.AsString;
import org.jruby.compiler.ir.operands.Backref;
import org.jruby.compiler.ir.operands.BacktickString;
import org.jruby.compiler.ir.operands.Bignum;
import org.jruby.compiler.ir.operands.BooleanLiteral;
import org.jruby.compiler.ir.operands.CompoundArray;
import org.jruby.compiler.ir.operands.CompoundString;
import org.jruby.compiler.ir.operands.DynamicSymbol;
import org.jruby.compiler.ir.operands.Fixnum;
import org.jruby.compiler.ir.operands.Float;
import org.jruby.compiler.ir.operands.Hash;
import org.jruby.compiler.ir.operands.IRException;
import org.jruby.compiler.ir.operands.KeyValuePair;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.ModuleMetaObject;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.Nil;
import org.jruby.compiler.ir.operands.UnexecutableNil;
import org.jruby.compiler.ir.operands.NthRef;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Range;
import org.jruby.compiler.ir.operands.Regexp;
import org.jruby.compiler.ir.operands.SValue;
import org.jruby.compiler.ir.operands.Splat;
import org.jruby.compiler.ir.operands.StringLiteral;
import org.jruby.compiler.ir.operands.Symbol;
import org.jruby.compiler.ir.operands.TemporaryVariable;
import org.jruby.compiler.ir.operands.UndefinedValue;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.CallType;
import org.jruby.util.ByteList;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

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
//
// 3. Temporary variable reuse
// ---------------------------
// I am reusing variables a lot in places in this code.  Should I instead always get a new variable when I need it
// This introduces artificial data dependencies, but fewer variables.  But, if we are going to implement SSA pass
// this is not a big deal.  Think this through!

public class IRBuilder {

    private static final Logger LOG = LoggerFactory.getLogger("IRBuilder");

    private static final UnexecutableNil U_NIL = UnexecutableNil.U_NIL;
    // FIXME: Move this
    public static final Operand[] NO_ARGS = new Operand[]{};

    private static boolean inIRGenOnlyMode = false;

    public static boolean inIRGenOnlyMode() {
        return inIRGenOnlyMode;
    }

    public static void main(String[] args) {
        boolean isDebug = args.length > 0 && args[0].equals("-debug");
        int     i = isDebug ? 1 : 0;

        IRBuilder.inIRGenOnlyMode = true;
        LOG.setDebugEnable(isDebug);

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
                LOG.debug("################## Before local optimization pass ##################");
                scope.runCompilerPass(new IRPrinter());
            }
            scope.runCompilerPass(new org.jruby.compiler.ir.compiler_pass.opts.LocalOptimizationPass());
            long t4 = new Date().getTime();
            if (isDebug) {
                LOG.debug("################## After local optimization pass ##################");
                scope.runCompilerPass(new IRPrinter());
            }
            scope.runCompilerPass(new CFGBuilder());
            long t5 = new Date().getTime();
//            scope.runCompilerPass(new org.jruby.compiler.ir.compiler_pass.DominatorTreeBuilder());
            long t6 = new Date().getTime();
           
            if (methName != null) {
                LOG.debug("################## After inline pass ##################");
                LOG.debug("Asked to inline " + methName);
                scope.runCompilerPass(new InlineTest(methName));
                scope.runCompilerPass(new LocalOptimizationPass());
                scope.runCompilerPass(new IRPrinter());
            }
           
            if (isDebug) {
                LOG.debug("################## After dead code elimination pass ##################");
            }
            scope.runCompilerPass(new LiveVariableAnalysis());
            long t7 = new Date().getTime();
            scope.runCompilerPass(new DeadCodeElimination());
            long t8 = new Date().getTime();
            // scope.runCompilerPass(new AddBindingInstructions());
            // long t9 = new Date().getTime();
            if (isDebug) {
                scope.runCompilerPass(new IRPrinter());
            }
            scope.runCompilerPass(new LinearizeCFG());
            if (isDebug) {
                LOG.debug("################## After cfg linearization pass ##################");
                scope.runCompilerPass(new IRPrinter());
            }
           
            LOG.debug("Time to build AST         : {}", (t2 - t1));
            LOG.debug("Time to build IR          : {}", (t3 - t2));
            LOG.debug("Time to run local opts    : {}", (t4 - t3));
            LOG.debug("Time to run build cfg     : {}", (t5 - t4));
            LOG.debug("Time to run build domtree : {}", (t6 - t5));
            LOG.debug("Time to run lva           : {}", (t7 - t6));
            LOG.debug("Time to run dead code elim: {}", (t8 - t7));
            //LOG.debug("Time to add frame instrs  : {}", (t9 - t8));
            i++;
        }
    }

    /* -----------------------------------------------------------------------------------
     * Every ensure block has a start label and end label, and at the end, it will jump
     * to an address stored in a return address variable.
     *
     * This ruby code will translate to the IR shown below
     * -----------------
     *   begin
     *       ... protected body ...
     *   ensure
     *       ... ensure block to run
     *   end
     * -----------------
     *  L_region_start
     *     IR instructions for the protected body
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
        Label    regionStart;
        Label    start;
        Label    end;
        Label    dummyRescueBlockLabel;
        Variable returnAddr;
        Variable savedGlobalException;

        public EnsureBlockInfo(IRScope m)
        {
            regionStart = m.getNewLabel();
            start       = m.getNewLabel();
            end         = m.getNewLabel();
            returnAddr  = m.getNewTemporaryVariable();
            dummyRescueBlockLabel = m.getNewLabel();
            savedGlobalException = null;
        }

        public static void emitJumpChain(IRScope m, Stack<EnsureBlockInfo> ebStack)
        {
            // SSS: There are 2 ways of encoding this:
            // 1. Jump to ensure block 1, return back here, jump ensure block 2, return back here, ...
            //    Generates 3*n instrs. where n is the # of ensure blocks to execute
            // 2. Jump to ensure block 1, then to block 2, then to 3, ...
            //    Generates n+1 instrs. where n is the # of ensure blocks to execute
            // Doesn't really matter all that much since we shouldn't have deep nesting of ensure blocks often
            // but is there a reason to go with technique 1 at all??
            int n = ebStack.size();
            EnsureBlockInfo[] ebArray = ebStack.toArray(new EnsureBlockInfo[n]);
            for (int i = n-1; i >= 0; i--) {
                Label retLabel = m.getNewLabel();
                EnsureBlockInfo ebi = ebArray[i];
                if (ebi.savedGlobalException != null) {
                    m.addInstr(new PutGlobalVarInstr("$!", ebi.savedGlobalException));
					 }
                m.addInstr(new SetReturnAddressInstr(ebi.returnAddr, retLabel));
                m.addInstr(new JumpInstr(ebi.start));
                m.addInstr(new LabelInstr(retLabel));
            }
        }
    }

    private int _lastProcessedLineNum = -1;
    private Stack<EnsureBlockInfo> _ensureBlockStack = new Stack<EnsureBlockInfo>();

    // Stack encoding nested rescue blocks -- this just tracks the start label of the blocks
    private Stack<Tuple<Label, Variable>> _rescueBlockStack = new Stack<Tuple<Label, Variable>>();

    public static Node buildAST(boolean isCommandLineScript, String arg) {
        Ruby ruby = Ruby.getGlobalRuntime();
        
        // set to IR mode, since we use different scopes, etc for IR
        ruby.getInstanceConfig().setCompileMode(CompileMode.OFFIR);
        
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

    public Variable generateRubyInternalsCall(IRScope m, RubyInternalsMethod meth, boolean hasResult, Operand receiver, Operand[] args) {
        Variable ret = hasResult ? m.getNewTemporaryVariable() : null;
        m.addInstr(new RubyInternalCallInstr(ret, meth, receiver, args));
        return ret;
    }

    public Variable generateJRubyUtilityCall(IRScope m, JRubyImplementationMethod meth, Operand receiver, Operand[] args) {
        Variable ret = m.getNewTemporaryVariable();
        
        m.addInstr(JRubyImplCallInstr.createJRubyImplementationMethod(ret, meth, receiver, args));
        
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
            case CONSTNODE: return searchConst(m, m, ((ConstNode) node).getName()); // done
            case DASGNNODE: return buildDAsgn((DAsgnNode) node, m); // done
            case DEFINEDNODE: return buildGetDefinitionBase(((DefinedNode) node).getExpressionNode(), m);
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
            case FLIPNODE: return buildFlip(node, m); // done
            case FLOATNODE: return buildFloat((FloatNode) node, m); // done
            case FORNODE: return buildFor((ForNode) node, (IRExecutionScope)m); // done
            case GLOBALASGNNODE: return buildGlobalAsgn((GlobalAsgnNode) node, m); // done
            case GLOBALVARNODE: return buildGlobalVar((GlobalVarNode) node, m); // done
            case HASHNODE: return buildHash((HashNode) node, m); // done
            case IFNODE: return buildIf((IfNode) node, m); // done
            case INSTASGNNODE: return buildInstAsgn((InstAsgnNode) node, m); // done
            case INSTVARNODE: return buildInstVar((InstVarNode) node, m); // done
            case ITERNODE: return buildIter((IterNode) node, (IRExecutionScope)m); // done
            case LITERALNODE: return buildLiteral((LiteralNode) node, m);
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
            case PREEXENODE: return buildPreExe(node, m); // DEFERRED
            case POSTEXENODE: return buildPostExe(node, m); // DEFERRED
            case REDONODE: return buildRedo(node, (IRExecutionScope)m); // done??
            case REGEXPNODE: return buildRegexp((RegexpNode) node, m); // done
            case RESCUEBODYNODE:
                throw new NotCompilableException("rescue body is handled by rescue compilation at: " + node.getPosition());
            case RESCUENODE: return buildRescue(node, m); // done
            case RETRYNODE: return buildRetry(node, m); // FIXME: done?
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
            case VALIASNODE: return buildVAlias(node, m); // done
            case VCALLNODE: return buildVCall((VCallNode) node, m); // done
            case WHILENODE: return buildWhile((WhileNode) node, (IRExecutionScope)m); // done
            case WHENNODE: assert false : "When nodes are handled by case node compilation."; return null;
            case XSTRNODE: return buildXStr((XStrNode) node, m); // done
            case YIELDNODE: return buildYield((YieldNode) node, m); // done
            case ZARRAYNODE: return buildZArray(node, m); // done
            case ZSUPERNODE: return buildZSuper((ZSuperNode) node, m); // done
            default: new Exception().printStackTrace(); throw new NotCompilableException("Unknown node encountered in builder: " + node.getClass());
        }
    }

    private Variable getValueInTemporaryVariable(IRScope s, Operand val) {
        if ((val != null) && (val instanceof TemporaryVariable)) {
            return (Variable)val;
        }
        else {
            Variable tmpVar = s.getNewTemporaryVariable();
            s.addInstr(new CopyInstr(tmpVar, val));
            return tmpVar;
        }
    }

    public Operand copyAndReturnValue(IRScope s, Operand val) {
        Variable v = s.getNewTemporaryVariable();
        s.addInstr(new CopyInstr(v, val));
        return v;
    }

    public void buildArguments(List<Operand> args, Node node, IRScope s) {
        switch (node.getNodeType()) {
            case ARGSCATNODE: {
                ArgsCatNode n = (ArgsCatNode)node;
                args.add(new Splat(build(n.getFirstNode(), s)));
                args.add(new Splat(build(n.getSecondNode(), s)));
                break;
            }
            case ARGSPUSHNODE: {
                ArgsPushNode n = (ArgsPushNode)node;
                args.add(new Splat(build(n.getFirstNode(), s)));
                args.add(build(n.getSecondNode(), s));
                break;
            }
            case ARRAYNODE: {
                args.add(buildArray(node, s));
                break;
            }
            case SPLATNODE: {
                args.add(buildSplat((SplatNode)node, s));
                break;
            }
            default: {
                args.add(build(node, s));
            }
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
            case CLASSVARASGNNODE:
                s.addInstr(new PutClassVariableInstr(classVarDefinitionContainer(s, true), ((ClassVarAsgnNode)node).getName(), v));
                break;
            case CLASSVARDECLNODE:
                s.addInstr(new PutClassVariableInstr(classVarDefinitionContainer(s, false), ((ClassVarDeclNode)node).getName(), v));
                break;
            case CONSTDECLNODE:
                buildConstDeclAssignment((ConstDeclNode) node, s, v);
                break;
            case DASGNNODE: {
                DAsgnNode variable = (DAsgnNode) node;
                int depth = variable.getDepth();
                s.addInstr(new CopyInstr(s.getLocalVariable(variable.getName(), depth), v));
                break;
            }
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
                s.addInstr(new CopyInstr(s.getLocalVariable(localVariable.getName(), depth), v));
                break;
            }
            case MULTIPLEASGNNODE: {
                /* --------------------------------------------------------------------------------------------
                 * SSS FIXME: For 1.9 mode, this code would be:
                 *
                 * Operand valuesArg = generateRubyInternalsCall(s, RubyInternalsMethod.TO_ARY, true, v, new Operand[] { BooleanLiteral.FALSE });
                 * buildMultipleAsgnAssignment(childNode, s, valuesArg);
                 *
                 * But in 1.8 mode, we do that only in some cases .. so, this is complicated!
                 * ------------------------------------------------------------------------------------------ */
                Operand valuesArg;
                MultipleAsgnNode childNode = (MultipleAsgnNode) node;
                if (childNode.getHeadNode() != null && ((ListNode)childNode.getHeadNode()).childNodes().size() > 0) {
                    // Invoke to_ary on the operand only if it is not an array already
                    valuesArg = generateRubyInternalsCall(s, RubyInternalsMethod.TO_ARY, true, v, new Operand[] { BooleanLiteral.FALSE });
                } else {
                    s.addInstr(new EnsureRubyArrayInstr(v, v));
                    valuesArg = v;
                }
                buildMultipleAsgnAssignment(childNode, s, valuesArg);
                break;
            }
            case ZEROARGNODE:
                throw new NotCompilableException("Shouldn't get here; zeroarg does not do assignment: " + node);
            default:
                throw new NotCompilableException("Can't build assignment node: " + node);
        }
    }

    private LocalVariable getBlockArgVariable(IRClosure cl, String name, int depth) {
        return cl.getLocalVariable(name, depth);
        // SSS FIXME: The code below is probably 1.9 semantics?
        //
        // For non-loops, this name will override any name that exists in outer scopes
        //return cl.isForLoopBody ? cl.getLocalVariable(name, depth) : cl.getNewLocalVariable(name, depth);
    }

    private void receiveBlockArg(IRScope s, Variable v, boolean isClosureArg, int argIndex, boolean isSplat) {
        s.addInstr(isClosureArg ? new ReceiveClosureInstr(v) : new ReceiveClosureArgInstr(v, argIndex, isSplat));
    }

    // This method is called to build arguments for a block!
    public void buildBlockArgsAssignment(Node node, IRScope s, int argIndex, boolean isClosureArg, boolean isRoot, boolean isSplat) {
        Variable v;
        switch (node.getNodeType()) {
            case ATTRASSIGNNODE: 
                v = s.getNewTemporaryVariable();
                receiveBlockArg(s, v, isClosureArg, argIndex, isSplat);
                buildAttrAssignAssignment(node, s, v);
                break;
            // SSS FIXME:
            //
            // There are also differences in variable scoping between 1.8 and 1.9 
            // Ruby 1.8 is the buggy semantics if I understand correctly.
            case DASGNNODE: {
                DAsgnNode dynamicAsgn = (DAsgnNode) node;
                v = getBlockArgVariable((IRClosure)s, dynamicAsgn.getName(), dynamicAsgn.getDepth());
                receiveBlockArg(s, v, isClosureArg, argIndex, isSplat);
                break;
            }
            case CLASSVARASGNNODE:
                v = s.getNewTemporaryVariable();
                receiveBlockArg(s, v, isClosureArg, argIndex, isSplat);
                s.addInstr(new PutClassVariableInstr(classVarDefinitionContainer(s, true), ((ClassVarAsgnNode)node).getName(), v));
                break;
            case CLASSVARDECLNODE:
                v = s.getNewTemporaryVariable();
                receiveBlockArg(s, v, isClosureArg, argIndex, isSplat);
                s.addInstr(new PutClassVariableInstr(classVarDefinitionContainer(s, false), ((ClassVarDeclNode)node).getName(), v));
                break;
            case CONSTDECLNODE:
                v = s.getNewTemporaryVariable();
                receiveBlockArg(s, v, isClosureArg, argIndex, isSplat);
                buildConstDeclAssignment((ConstDeclNode) node, s, v);
                break;
            case GLOBALASGNNODE:
                v = s.getNewTemporaryVariable();
                receiveBlockArg(s, v, isClosureArg, argIndex, isSplat);
                s.addInstr(new PutGlobalVarInstr(((GlobalAsgnNode)node).getName(), v));
                break;
            case INSTASGNNODE:
                v = s.getNewTemporaryVariable();
                receiveBlockArg(s, v, isClosureArg, argIndex, isSplat);
                // NOTE: if 's' happens to the a class, this is effectively an assignment of a class instance variable
                s.addInstr(new PutFieldInstr(getSelf(s), ((InstAsgnNode)node).getName(), v));
                break;
            case LOCALASGNNODE: {
                LocalAsgnNode localVariable = (LocalAsgnNode) node;
                int depth = localVariable.getDepth();
                v = getBlockArgVariable((IRClosure)s, localVariable.getName(), depth);
                receiveBlockArg(s, v, isClosureArg, argIndex, isSplat);
                break;
            }
            case MULTIPLEASGNNODE:
            {
                Variable oldArgs = null;
                MultipleAsgnNode childNode = (MultipleAsgnNode) node;
                // Push
                if (!isRoot) {
                    v = s.getNewTemporaryVariable();
                    receiveBlockArg(s, v, isClosureArg, argIndex, isSplat);
                    oldArgs = s.getNewTemporaryVariable();
                    // SSS FIXME: In 1.9 mode, runToAry will be unconditionally true, but not in 1.8
                    boolean runToAry = childNode.getHeadNode() != null && (((ListNode)childNode.getHeadNode()).childNodes().size() > 0);
                    s.addInstr(new SetArgumentsInstr(oldArgs, v, runToAry));      // convert to array via to_ary if necessary 
                    // SSS FIXME: Are we guaranteed that splats dont head to multiple-assignment nodes!  i.e. |*(a,b)|?
                }
                // Build
                buildMultipleAsgnAssignment(childNode, s, null);
                // Pop
                if (!isRoot) {
                    s.addInstr(new SetArgumentsInstr(null, oldArgs, false));  // restore oldArgs -- no to_ary required
                }
                break;
            }
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
        generateRubyInternalsCall(s, RubyInternalsMethod.DEFINE_ALIAS, false, getSelf(s), new Operand[] { newName, oldName });
        return Nil.NIL;
    }

    // Translate "ret = (a && b)" --> "ret = (a ? b : false)" -->
    // 
    //    v1 = -- build(a) --
    //       OPT: ret can be set to v1, but effectively v1 is false if we take the branch to L.
    //            while this info can be inferred by using attributes, why bother if we can do this?
    //    ret = v1   
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
            // build first node only and return its value
            return build(andNode.getFirstNode(), m);
        } else {
            Label    l   = m.getNewLabel();
            Operand  v1  = build(andNode.getFirstNode(), m);
            Variable ret = getValueInTemporaryVariable(m, v1);
            m.addInstr(new BEQInstr(v1, BooleanLiteral.FALSE, l));
            Operand  v2  = build(andNode.getSecondNode(), m);
            m.addInstr(new CopyInstr(ret, v2));
            m.addInstr(new LabelInstr(l));
            return ret;
        }
    }

    public Operand buildArray(Node node, IRScope m) {
        List<Operand> elts = new ArrayList<Operand>();
        for (Node e: node.childNodes())
            elts.add(build(e, m));

        return copyAndReturnValue(m, new Array(elts));
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
        Operand obj = build(attrAssignNode.getReceiverNode(), s);
        List<Operand> args = setupCallArgs(attrAssignNode.getArgsNode(), s);
        s.addInstr(new AttrAssignInstr(obj, new MethAddr(attrAssignNode.getName()), args.toArray(new Operand[args.size()])));
        Operand lastArg = args.get(args.size()-1);
        return (lastArg instanceof Splat) ? ((Splat)lastArg).getArray() : lastArg;
    }

    public Operand buildAttrAssignAssignment(Node node, IRScope s, Operand value) {
        final AttrAssignNode attrAssignNode = (AttrAssignNode) node;
        Operand obj = build(attrAssignNode.getReceiverNode(), s);
        List<Operand> args = setupCallArgs(attrAssignNode.getArgsNode(), s);
        args.add(value);
        s.addInstr(new AttrAssignInstr(obj, new MethAddr(attrAssignNode.getName()), args.toArray(new Operand[args.size()])));
        return value;
    }

    public Operand buildBackref(BackRefNode node, IRScope m) {
        // SSS FIXME: Required? Verify with Tom/Charlie
        return copyAndReturnValue(m, new Backref(node.getType()));
    }

    public Operand buildBegin(BeginNode beginNode, IRScope s) {
        return build(beginNode.getBodyNode(), s);
    }

    public Operand buildBignum(BignumNode node, IRScope s) {
        // SSS: Since bignum literals are effectively interned objects, no need to copyAndReturnValue(...)
        // Or is this a premature optimization?
        return new Bignum(node.getValue());
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
        // If we have ensure blocks, have to run those first!
        if (!_ensureBlockStack.empty()) EnsureBlockInfo.emitJumpChain(s, _ensureBlockStack);

        IRLoop currLoop = s.getCurrentLoop();
        if (currLoop != null) {
            s.addInstr(new CopyInstr(currLoop.loopResult, rv));
            s.addInstr(new JumpInstr(currLoop.loopEndLabel));
        }
        else {
            if (s instanceof IRClosure) {
                // This lexical scope value is only used (and valid) in regular block contexts.
                // If this instruction is executed in a Proc or Lambda context, the lexical scope value is useless.
                s.addInstr(new BreakInstr(rv, (IRExecutionScope)(s.getLexicalParent())));
            }
            else {
                // SSS FIXME: If we are not in a closure or a loop, the break instruction will throw a runtime exception
                // Since we know this right now, should we build an exception instruction here?
                s.addInstr(new BreakInstr(rv, null));
            }
        }
        return rv;
    }

    public Operand buildCall(CallNode callNode, IRScope s) {
        Node          callArgsNode = callNode.getArgsNode();
        Node          receiverNode = callNode.getReceiverNode();
        // Though you might be tempted to move this build into the CallInstr as:
        //    new Callinstr( ... , build(receiverNode, s), ...)
        // that is incorrect IR because the receiver has to be built *before* call arguments are built
        // to preserve expected code execution order
        Operand       receiver     = build(receiverNode, s);
        List<Operand> args         = setupCallArgs(callArgsNode, s);
        Operand       block        = setupCallClosure(callNode.getIterNode(), s);
        Variable      callResult   = s.getNewTemporaryVariable();
        Instr      callInstr    = CallInstr.create(callResult, new MethAddr(callNode.getName()), receiver, args.toArray(new Operand[args.size()]), block);
        s.addInstr(callInstr);
        return callResult;
    }

    public Operand buildCase(CaseNode caseNode, IRScope m) {
        // get the incoming case value
        Operand value = build(caseNode.getCaseNode(), m);

        // This is for handling case statements without a value (see example below)
        //   case 
        //     when true <blah>
        //     when false <blah>
        //   end
        if (value == null) value = UndefinedValue.UNDEFINED;

        // the CASE instruction
        Label     endLabel  = m.getNewLabel();
        boolean   hasElse   = (caseNode.getElseNode() != null);
        Label     elseLabel = m.getNewLabel();
        Variable  result    = m.getNewTemporaryVariable();

        // lists to aggregate variables and bodies for whens
        List<Operand> variables = new ArrayList<Operand>();
        List<Label> labels = new ArrayList<Label>();

        Map<Label, Node> bodies = new HashMap<Label, Node>();

        // build each "when"
        for (Node aCase : caseNode.getCases().childNodes()) {
            WhenNode whenNode = (WhenNode)aCase;
            Label bodyLabel = m.getNewLabel();

            Variable eqqResult = m.getNewTemporaryVariable();
            variables.add(eqqResult);
            labels.add(bodyLabel);
            Operand v1, v2;
            if (whenNode.getExpressionNodes() instanceof ListNode) {
                // SSS FIXME: Note about refactoring:
                // - BEQInstr has a quick implementation when the second operand is a boolean literal
                //   If it can be fixed to do this even on the first operand, we can switch around
                //   v1 and v2 in the UndefinedValue scenario and DRY out this code.
                // - Even with this asymmetric implementation of BEQInstr, you might be tempted to
                //   switch around v1 and v2 in the else case.  But, that is equivalent to this Ruby code change:
                //      (v1 == value) instead of (value == v1)
                //   It seems that they should be identical, but the first one is v1.==(value) and the second one is
                //   value.==(v1).  This is just fine *if* the Ruby programmer has implemented an algebraically
                //   symmetric "==" method on those objects.  If not, then, the results might be unexpected where the
                //   code (intentionally or otherwise) relies on this asymmetry of "==".  While it could be argued
                //   that this a Ruby code bug, we will just try to preserve the order of the == check as it appears
                //   in the Ruby code.
                if (value == UndefinedValue.UNDEFINED)  {
                    v1 = buildArray(whenNode.getExpressionNodes(), m);
                    v2 = BooleanLiteral.TRUE;
                }
                else {
                    v1 = value;
                    v2 = buildArray(whenNode.getExpressionNodes(), m);
                }
            } else {
                m.addInstr(new EQQInstr(eqqResult, build(whenNode.getExpressionNodes(), m), value));
                v1 = eqqResult;
                v2 = BooleanLiteral.TRUE;
            }
            m.addInstr(new BEQInstr(v1, v2, bodyLabel));

            // SSS FIXME: This doesn't preserve original order of when clauses.  We could consider
            // preserving the order (or maybe not, since we would have to sort the constants first
            // in any case) for outputing jump tables in certain situations.
            //
            // add body to map for emitting later
            bodies.put(bodyLabel, whenNode.getBodyNode());
        }

        // Jump to else in case nothing matches!
        m.addInstr(new JumpInstr(elseLabel));

        // build "else" if it exists
        if (hasElse) {
            labels.add(elseLabel);
            bodies.put(elseLabel, caseNode.getElseNode());
        }

        // now emit bodies while preserving when clauses order
        for (Label whenLabel: labels) {
            m.addInstr(new LabelInstr(whenLabel));
            Operand bodyValue = build(bodies.get(whenLabel), m);
            // bodyValue can be null if the body ends with a return!
            if (bodyValue != null) {
               // SSS FIXME: Do local optimization of break results (followed by a copy & jump) to short-circuit the jump right away
               // rather than wait to do it during an optimization pass when a dead jump needs to be removed.  For this, you have
               // to look at what the last generated instruction was.
               Label tgt = endLabel;
               m.addInstr(new CopyInstr(result, bodyValue));
               m.addInstr(new JumpInstr(tgt));
            }
        }

        if (!hasElse) {
            m.addInstr(new LabelInstr(elseLabel));
            m.addInstr(new CopyInstr(result, Nil.NIL));
            m.addInstr(new JumpInstr(endLabel));
        }

        // close it out
        m.addInstr(new LabelInstr(endLabel));

        // SSS: Got rid of the marker case label instruction

        return result;
    }

    /**
     * Build a new class and add it to the current scope (s).
     */
    public Operand buildClass(ClassNode classNode, IRScope s) {
        Node superNode = classNode.getSuperNode();
        Colon3Node cpath = classNode.getCPath();
        Operand superClass = (superNode == null) ? null : build(superNode, s);
        String className = cpath.getName();
        Operand container = getContainerFromCPath(cpath, s);

        IRClass c = new IRClass(s, (superClass instanceof MetaObject) ? (IRClass)((MetaObject)superClass).scope : null, className, classNode.getScope());
        Variable ret = s.getNewTemporaryVariable();
        s.addInstr(new DefineClassInstr(ret, c, container, superClass));
        // SSS NOTE: This is a debugging tool that works in most cases and is not used
        // at runtime by the executing code since this static nesting might be wrong.
        IRModule nm = s.getNearestModule();
        if (nm != null) nm.addClass(c);

        IRMethod rootMethod = c.getRootMethod();
        Operand rv = build(classNode.getBodyNode(), rootMethod);
        if (rv != null) rootMethod.addInstr(new ReturnInstr(rv));

        return ret;
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

        // Create a dummy meta class and record it as being lexically defined in scope s
        IRMetaClass mc = new IRMetaClass(s, sclassNode.getScope());
        // SSS NOTE: This is a debugging tool that works in most cases and is not used
        // at runtime by the executing code since this static nesting might be wrong.
        IRModule nm = s.getNearestModule();
        if (nm != null) nm.addClass(mc);
        Variable ret = s.getNewTemporaryVariable();
        s.addInstr(new DefineMetaClassInstr(ret, receiver, mc));

        IRMethod rootMethod = mc.getRootMethod();
        Operand rv = build(sclassNode.getBodyNode(), rootMethod);
        if (rv != null) rootMethod.addInstr(new ReturnInstr(rv));

        return ret;
    }

    // @@c
    public Operand buildClassVar(ClassVarNode node, IRScope s) {
        Variable ret = s.getNewTemporaryVariable();
        Variable classVarContainer = classVarDefinitionContainer(s, true);
        s.addInstr(new GetClassVariableInstr(ret, classVarContainer, node.getName()));
        return ret;
    }

    // ClassVarAsgn node is assignment within a method/closure scope
    //
    // def foo
    //   @@c = 1
    // end
    public Operand buildClassVarAsgn(final ClassVarAsgnNode classVarAsgnNode, IRScope s) {
        Operand val = build(classVarAsgnNode.getValueNode(), s);
        Variable classVarContainer = classVarDefinitionContainer(s, true);
        s.addInstr(new PutClassVariableInstr(classVarContainer, classVarAsgnNode.getName(), val));
        return val;
    }

    // ClassVarDecl node is assignment outside method/closure scope (top-level, class, module)
    //
    // class C
    //   @@c = 1
    // end
    public Operand buildClassVarDecl(final ClassVarDeclNode classVarDeclNode, IRScope s) {
        Operand val = build(classVarDeclNode.getValueNode(), s);
        Variable classVarContainer = classVarDefinitionContainer(s, false);
        s.addInstr(new PutClassVariableInstr(classVarContainer, classVarDeclNode.getName(), val));
        return val;
    }
    
    // SSS FIXME: This feels a little ugly.  Is there a better way of representing this?
    public Variable classVarDefinitionContainer(IRScope s, boolean lookInMetaClass) {
        /* -------------------------------------------------------------------------------
         * Find the nearest class/module scope (within which 's' is embedded) that can hold
         * class variables and return its root method.  Skip singleton root methods since
         * singletons can never contain class variables!
         *
         * Ex: check out this ruby code
         * 
         *     o = "huh?"
         *     class << o
         *       @@c = "I escape o!"
         *     end
         *
         *     p @@c
         * 
         * So @@c is accessible outside the singleton class in the script
         *
         * Stop lexical scope walking at an eval script boundary.  Evals are essentially
         * a way for a programmer to splice an entire tree of lexical scopes at the point
         * where the eval happens.  So, when we hit an eval-script boundary at compile-time,
         * defer scope traversal to when we know where this scope has been spliced in.
         * ------------------------------------------------------------------------------- */
        IRScope current = s;
        while (current != null && (   !(current instanceof IREvalScript)
                                   && !(    (current instanceof IRMethod) 
                                         && ((IRMethod)current).isAModuleRootMethod()
                                         && !(current.getLexicalParent() instanceof IRMetaClass)))) {
            current = current.getLexicalParent();
        }

        Variable tmp = s.getNewTemporaryVariable();
        s.addInstr(new GetClassVarContainerModuleInstr(tmp, current instanceof IRMethod ? (IRMethod)current : null, lookInMetaClass ? getSelf(s) : null));
        return tmp;
    }

    public Operand buildConstDecl(ConstDeclNode node, IRScope s) {
        Operand val = build(node.getValueNode(), s);
        return buildConstDeclAssignment(node, s, val);
    }

    private MetaObject findContainerModule(IRScope s) {
        IRModule nearestModule = s.getNearestModule();
        return (nearestModule == null) ? ModuleMetaObject.CURRENT_MODULE : MetaObject.create(nearestModule);
    }

    private IRModule startingSearchScope(IRScope s) {
        return s.getNearestModule();
    }

    public Operand buildConstDeclAssignment(ConstDeclNode constDeclNode, IRScope s, Operand val) {
        Node constNode = constDeclNode.getConstNode();

        if (constNode == null) {
            s.addInstr(new PutConstInstr(findContainerModule(s), constDeclNode.getName(), val));
        } else if (constNode.getNodeType() == NodeType.COLON2NODE) {
            Operand module = build(((Colon2Node) constNode).getLeftNode(), s);
            s.addInstr(new PutConstInstr(module, constDeclNode.getName(), val));
        } else { // colon3, assign in Object
            MetaObject object = MetaObject.create(IRClass.getCoreClass("Object"));            
            s.addInstr(new PutConstInstr(object, constDeclNode.getName(), val));            
        }

        return val;
    }

    private Operand searchConst(IRScope s, IRScope startingScope, String name) {
        IRModule startingModule = startingSearchScope(startingScope);
        Variable v = s.getNewTemporaryVariable();
        s.addInstr(new SearchConstInstr(v, startingModule, name));
        Label foundLabel = s.getNewLabel();
        s.addInstr(new BNEInstr(v, UndefinedValue.UNDEFINED, foundLabel));
        s.addInstr(new ConstMissingInstr(v, startingModule, name));
        s.addInstr(new LabelInstr(foundLabel));
        return v;
    }

    public Operand buildColon2(final Colon2Node iVisited, IRScope s) {
        Node leftNode = iVisited.getLeftNode();
        final String name = iVisited.getName();

        // ENEBO: Does this really happen?
        if (leftNode == null) return searchConst(s, s, name);

        if (iVisited instanceof Colon2ConstNode) {
            // 1. Load the module first (lhs of node)
            // 2. Then load the constant from the module
            Operand module = build(leftNode, s);
            if (module instanceof MetaObject) module = MetaObject.create(((MetaObject)module).scope);
            Variable constVal = s.getNewTemporaryVariable();
            s.addInstr(new GetConstInstr(constVal, module, name));
            return constVal;
        } else if (iVisited instanceof Colon2MethodNode) {
            Colon2MethodNode c2mNode = (Colon2MethodNode)iVisited;
            List<Operand> args       = setupCallArgs(null, s);
            Operand       block      = setupCallClosure(null, s);
            Variable      callResult = s.getNewTemporaryVariable();
            Instr         callInstr  = CallInstr.create(callResult, new MethAddr(c2mNode.getName()), 
                    null, args.toArray(new Operand[args.size()]), block);
            s.addInstr(callInstr);
            return callResult;
        }
        else { throw new NotCompilableException("Not compilable: " + iVisited); }
    }

    public Operand buildColon3(Colon3Node node, IRScope s) {
        return searchConst(s, IRClass.getCoreClass("Object"), node.getName());
    }

    interface CodeBlock {
        public Operand run(Object[] args);
    }

    private Variable protectCodeWithEnsure(IRScope m, CodeBlock protectedCode, Object[] protectedCodeArgs, CodeBlock ensureCode, Object[] ensureCodeArgs) {
        // This effectively mimics a begin-ensure-end code block
        // Except this silently swallows all exceptions raised by the protected code

        Variable ret = m.getNewTemporaryVariable();

        // Push a new ensure block info node onto the stack of ensure block
        EnsureBlockInfo ebi = new EnsureBlockInfo(m);
        _ensureBlockStack.push(ebi);
        Label rBeginLabel = ebi.regionStart;
        Label rEndLabel   = ebi.end;

        // Protected region code
        m.addInstr(new LabelInstr(rBeginLabel));
        m.addInstr(new ExceptionRegionStartMarkerInstr(rBeginLabel, rEndLabel, ebi.dummyRescueBlockLabel, ebi.dummyRescueBlockLabel));
        Operand v1 = protectedCode.run(protectedCodeArgs); // YIELD: Run the protected code block
        m.addInstr(new CopyInstr(ret, v1));
        m.addInstr(new SetReturnAddressInstr(ebi.returnAddr, rEndLabel));

        _ensureBlockStack.pop();

        // Ensure block code
        m.addInstr(new LabelInstr(ebi.start));
        ensureCode.run(ensureCodeArgs); // YIELD: Run the ensure code block
        m.addInstr(new JumpIndirectInstr(ebi.returnAddr));

        // By moving the exception region end marker here to include the ensure block,
        // we effectively swallow those exceptions -- but we will end up trying to rerun the ensure code again!
        // SSS FIXME: Wont this get us stuck in an infinite loop?
        m.addInstr(new ExceptionRegionEndMarkerInstr());

        // Rescue block code
        // SSS FIXME: How do we get this to catch all exceptions, not just Ruby exceptions?
        m.addInstr(new LabelInstr(ebi.dummyRescueBlockLabel));
        m.addInstr(new CopyInstr(ret, Nil.NIL));
        m.addInstr(new SetReturnAddressInstr(ebi.returnAddr, ebi.end));
        m.addInstr(new JumpInstr(ebi.start));

        // End
        m.addInstr(new LabelInstr(rEndLabel));

        return ret;
    }

    private Operand protectCodeWithRescue(IRScope m, CodeBlock protectedCode, Object[] protectedCodeArgs, CodeBlock rescueBlock, Object[] rescueBlockArgs) {
        // This effectively mimics a begin-rescue-end code block
        // Except this catches all exceptions raised by the protected code

        Variable rv = m.getNewTemporaryVariable();
        Label rBeginLabel = m.getNewLabel();
        Label rEndLabel   = m.getNewLabel();
        Label rescueLabel = m.getNewLabel();

        // Protected region code
        m.addInstr(new LabelInstr(rBeginLabel));
        m.addInstr(new ExceptionRegionStartMarkerInstr(rBeginLabel, rEndLabel, null, rescueLabel));
        Object v1 = protectedCode.run(protectedCodeArgs); // YIELD: Run the protected code block
        m.addInstr(new CopyInstr(rv, (Operand)v1));
        m.addInstr(new JumpInstr(rEndLabel));
        m.addInstr(new ExceptionRegionEndMarkerInstr());

        // Rescue code
        Label uncaughtLabel = m.getNewLabel();
        Variable exc = m.getNewTemporaryVariable();
        Variable eqqResult = m.getNewTemporaryVariable();

        m.addInstr(new LabelInstr(rescueLabel));
        m.addInstr(new ReceiveExceptionInstr(exc));
        // Verify that the exception is of type 'RubyException'.
        // Since this is JRuby implementation Java code, we dont need EQQ here.
        // SSS FIXME: Hardcoded exception class name!
        m.addInstr(new InstanceOfInstr(eqqResult, exc, "org.jruby.RubyException")); 
        m.addInstr(new BEQInstr(eqqResult, BooleanLiteral.FALSE, uncaughtLabel));
        Object v2 = rescueBlock.run(rescueBlockArgs); // YIELD: Run the protected code block
        if (v2 != null) m.addInstr(new CopyInstr(rv, Nil.NIL));
        m.addInstr(new JumpInstr(rEndLabel));
        m.addInstr(new LabelInstr(uncaughtLabel));
        m.addInstr(new ThrowExceptionInstr(exc));

        // End
        m.addInstr(new LabelInstr(rEndLabel));

        return rv;
    }

    public Operand buildGetDefinitionBase(Node node, IRScope m) {
        node = skipOverNewlines(m, node);
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
            m.addInstr(new JRubyImplCallInstr(null, JRubyImplementationMethod.SET_WITHIN_DEFINED, null, new Operand[]{BooleanLiteral.TRUE}));

            // Protected code
            CodeBlock protectedCode = new CodeBlock() {
                public Operand run(Object[] args) {
                   return buildGetDefinition((Node)args[0], (IRScope)args[1]);
                }
            };

            // Ensure code
            CodeBlock ensureCode = new CodeBlock() {
                public Operand run(Object[] args) {
                    IRScope m = (IRScope)args[0];
                    m.addInstr(new JRubyImplCallInstr(null, JRubyImplementationMethod.SET_WITHIN_DEFINED, null, new Operand[]{BooleanLiteral.FALSE}));
                    return Nil.NIL;
                }
            };

            return protectCodeWithEnsure(m, protectedCode, new Object[] {node, m}, ensureCode, new Object[] {m});
        }
    }

    private Variable buildDefnCheckIfThenPaths(IRScope s, Label undefLabel, Operand defVal) {
        Label defLabel = s.getNewLabel();
        Variable tmpVar = getValueInTemporaryVariable(s, defVal);
        s.addInstr(new JumpInstr(defLabel));
        s.addInstr(new LabelInstr(undefLabel));
        s.addInstr(new CopyInstr(tmpVar, Nil.NIL));
        s.addInstr(new LabelInstr(defLabel));
        return tmpVar;
    }

    private Variable buildDefinitionCheck(IRScope s, JRubyImplementationMethod defnChecker, Operand receiver, String nameToCheck, String definedReturnValue) {
        Label undefLabel = s.getNewLabel();
        Variable tmpVar  = s.getNewTemporaryVariable();
        Operand[] args   = nameToCheck == null ? NO_ARGS : new Operand[]{new StringLiteral(nameToCheck)};
        s.addInstr(JRubyImplCallInstr.createJRubyImplementationMethod(tmpVar, defnChecker, receiver, args));
        s.addInstr(new BEQInstr(tmpVar, BooleanLiteral.FALSE, undefLabel));
        return buildDefnCheckIfThenPaths(s, undefLabel, new StringLiteral(definedReturnValue));
    }

    public Operand buildGetArgumentDefinition(final Node node, IRScope m, String type) {
        if (node == null) {
            return new StringLiteral(type);
        } else { 
            Operand rv = new StringLiteral(type);
            boolean failPathReqd = false;
            Label   failLabel    = m.getNewLabel();
            if (node instanceof ArrayNode) {
                for (int i = 0; i < ((ArrayNode) node).size(); i++) {
                    Node iterNode = ((ArrayNode) node).get(i);
                    Operand def = buildGetDefinition(iterNode, m);
                    if (def == Nil.NIL) { // Optimization!
                        rv = Nil.NIL;
                        break;
                    }
                    else if (!def.isConstant()) { // Optimization!
                        failPathReqd = true;
                        m.addInstr(new BEQInstr(def, Nil.NIL, failLabel));
                    }
                }
            } else {
                Operand def = buildGetDefinition(node, m);
                if (def == Nil.NIL) { // Optimization!
                    rv = Nil.NIL;
                }
                else if (!def.isConstant()) { // Optimization!
                    failPathReqd = true;
                    m.addInstr(new BEQInstr(def, Nil.NIL, failLabel));
                }
            }

            // Optimization!
            return failPathReqd ? buildDefnCheckIfThenPaths(m, failLabel, rv) : rv;
        }
    }

    public Operand buildGetDefinition(Node defnNode, IRScope s) {
        final Node node = skipOverNewlines(s, defnNode);
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
            case VCALLNODE:
                // SSS FIXME: Can we get away without passing in self?
                return buildDefinitionCheck(s, JRubyImplementationMethod.SELF_IS_METHOD_BOUND, getSelf(s), ((VCallNode) node).getName(), "method");
            case CONSTNODE: {
                Label undefLabel = s.getNewLabel();
                Variable tmpVar  = s.getNewTemporaryVariable();
                s.addInstr(new SearchConstInstr(tmpVar, startingSearchScope(s), ((ConstNode) node).getName()));
                s.addInstr(new BEQInstr(tmpVar, UndefinedValue.UNDEFINED, undefLabel));
                return buildDefnCheckIfThenPaths(s, undefLabel, new StringLiteral("constant"));
            }
            case GLOBALVARNODE:
                return buildDefinitionCheck(s, JRubyImplementationMethod.RT_IS_GLOBAL_DEFINED, null, ((GlobalVarNode) node).getName(), "global-variable");
            case INSTVARNODE:
                // SSS FIXME: Can we get away without passing in self?
                return buildDefinitionCheck(s, JRubyImplementationMethod.SELF_HAS_INSTANCE_VARIABLE, getSelf(s), ((InstVarNode) node).getName(), "instance-variable");
            case YIELDNODE:
                return buildDefinitionCheck(s, JRubyImplementationMethod.BLOCK_GIVEN, null, null, "yield");
            case BACKREFNODE:
                return buildDefinitionCheck(s, JRubyImplementationMethod.BACKREF_IS_RUBY_MATCH_DATA, null, null, "$" + ((BackRefNode) node).getType());
            case FCALLNODE:
            {
                /* ------------------------------------------------------------------
                 * Generate IR for:
                 *    r = self/receiver
                 *    mc = r.metaclass
                 *    return mc.methodBound(meth) ? buildGetArgumentDefn(..) : false
                 * ----------------------------------------------------------------- */
                Label undefLabel = s.getNewLabel();
                Variable tmpVar = s.getNewTemporaryVariable();
                StringLiteral mName = new StringLiteral(((FCallNode)node).getName());
                s.addInstr(new JRubyImplCallInstr(tmpVar, JRubyImplementationMethod.SELF_IS_METHOD_BOUND, getSelf(s), new Operand[]{mName}));
                s.addInstr(new BEQInstr(tmpVar, BooleanLiteral.FALSE, undefLabel));
                Operand argsCheckDefn = buildGetArgumentDefinition(((FCallNode) node).getArgsNode(), s, "method");
                return buildDefnCheckIfThenPaths(s, undefLabel, argsCheckDefn);
            }
            case NTHREFNODE:
            {
            // SSS FIXME: Is there a reason to do this all with low-level IR?
            // Can't this all be folded into a Java method that would be part
            // of the runtime library, which then can be used by buildDefinitionCheck method above?
            // This runtime library would be used both by the interpreter & the compiled code!

                /* -------------------------------------------------------------------------------------
                 * We have to generate IR for this:
                 *    v = backref; (!(v instanceof RubyMatchData) || v.group(n).nil?) ? nil : "$#{n}"
                 *
                 * which happens to be identical to: (where nthRef implicitly fetches backref again!)
                 *    v = backref; (!(v instanceof RubyMatchData) || nthRef(n).nil?) ? nil : "$#{n}"
                 *
                 * I am using the second form since it let us encode it in fewer IR instructions.
                 * But, note that this second form is not as clean as the first one plus it fetches backref twice!
                 * ------------------------------------------------------------------------------------- */
                int n = ((NthRefNode) node).getMatchNumber();
                Label undefLabel = s.getNewLabel();
                Variable tmpVar = s.getNewTemporaryVariable();
                s.addInstr(new JRubyImplCallInstr(tmpVar, JRubyImplementationMethod.BACKREF_IS_RUBY_MATCH_DATA, null, NO_ARGS));
                s.addInstr(new BEQInstr(tmpVar, BooleanLiteral.FALSE, undefLabel));
                // SSS FIXME: 
                // - Can/should I use BEQInstr(new NthRef(n), Nil.NIL, undefLabel)? instead of .nil? & compare with flag?
                // - Or, even create a new IsNilInstr and NotNilInstr to represent optimized scenarios where
                //   the nil? method is not monkey-patched?
                // This matters because if String.nil? is monkey-patched, the two sequences can behave differently.
                s.addInstr(CallInstr.create(tmpVar, new MethAddr("nil?"), new NthRef(n), NO_ARGS, null));
                s.addInstr(new BEQInstr(tmpVar, BooleanLiteral.TRUE, undefLabel));
                return buildDefnCheckIfThenPaths(s, undefLabel, new StringLiteral("$" + n));
            }
            case COLON3NODE:
            case COLON2NODE:
            {
            // SSS FIXME: Is there a reason to do this all with low-level IR?
            // Can't this all be folded into a Java method that would be part
            // of the runtime library, which then can be used by buildDefinitionCheck method above?
            // This runtime library would be used both by the interpreter & the compiled code!

                final Colon3Node iVisited = (Colon3Node) node;
                final String name = iVisited.getName();

                // store previous exception for restoration if we rescue something
                Variable errInfo = s.getNewTemporaryVariable();
                s.addInstr(new JRubyImplCallInstr(errInfo, JRubyImplementationMethod.TC_SAVE_ERR_INFO, null, NO_ARGS));

                CodeBlock protectedCode = new CodeBlock() {
                    public Operand run(Object[] args) {
                        IRScope  m      = (IRScope)args[0];
                        Node     n      = (Node)args[1];
                        String   name   = (String)args[2];
                        Variable tmpVar = m.getNewTemporaryVariable();
                        Operand v;
                        if (n instanceof Colon2Node) {
                            v = build(((Colon2Node) n).getLeftNode(), m);
                        } else {
                            m.addInstr(new JRubyImplCallInstr(tmpVar, JRubyImplementationMethod.RT_GET_OBJECT, null, NO_ARGS));
                            v = tmpVar;
                        }
                        m.addInstr(new JRubyImplCallInstr(tmpVar, JRubyImplementationMethod.RTH_GET_DEFINED_CONSTANT_OR_BOUND_METHOD, null, new Operand[]{v, new StringLiteral(name)}));
                        return tmpVar;
                    }
                };

                // rescue block
                CodeBlock rescueBlock = new CodeBlock() {
                    public Operand run(Object[] args) {
                        // Nothing to do -- ignore the exception, and restore stashed error info!
                        IRScope  m  = (IRScope)args[0];
                        m.addInstr(new JRubyImplCallInstr(null, JRubyImplementationMethod.TC_RESTORE_ERR_INFO, null, new Operand[]{(Variable)args[1]}));
                        return Nil.NIL;
                    }
                };

                // Try verifying definition, and if we get an JumpException exception, process it with the rescue block above
                return protectCodeWithRescue(s, protectedCode, new Object[]{s, iVisited, name}, rescueBlock, new Object[] {s, errInfo});
            }
            case CALLNODE:
            {
            // SSS FIXME: Is there a reason to do this all with low-level IR?
            // Can't this all be folded into a Java method that would be part
            // of the runtime library, which then can be used by buildDefinitionCheck method above?
            // This runtime library would be used both by the interpreter & the compiled code!

                Label    undefLabel = s.getNewLabel();
                CallNode iVisited = (CallNode) node;
                Operand  receiverDefn = buildGetDefinition(iVisited.getReceiverNode(), s);
                s.addInstr(new BEQInstr(receiverDefn, Nil.NIL, undefLabel));

                // protected main block
                CodeBlock protectedCode = new CodeBlock() {
                    public Operand run(Object[] args) {
                        /* --------------------------------------------------------------------------
                         * Generate IR for this sequence:
                         *
                         *    1. r  = receiver
                         *    2. mc = r.metaClass
                         *    3. v  = mc.getVisibility(methodName)
                         *    4. f  = !v || v.isPrivate? || (v.isProtected? && receiver/self?.kindof(mc.getRealClass)
                         *    5. return f ? nil : --check args definition and return "method" or nil--
                         *
                         * Hide the complexity of instrs 2-4 into a verifyMethodIsPublicAccessible call
                         * which can executely entirely in Java-land.  No reason to expose the guts in IR.
                         * ------------------------------------------------------------------------------ */
                        IRScope  s          = (IRScope)args[0];
                        CallNode iVisited   = (CallNode)args[1];
                        Label    undefLabel = (Label)args[2];
                        String   methodName = iVisited.getName();
                        Variable tmpVar     = s.getNewTemporaryVariable();
                        Operand  receiver   = build(iVisited.getReceiverNode(), s);
                        s.addInstr(new JRubyImplCallInstr(tmpVar, JRubyImplementationMethod.METHOD_PUBLIC_ACCESSIBLE, receiver, new Operand[]{new StringLiteral(methodName)}));
                        s.addInstr(new BEQInstr(tmpVar, BooleanLiteral.FALSE, undefLabel));
                        Operand argsCheckDefn = buildGetArgumentDefinition(iVisited.getArgsNode(), s, "method");
                        return buildDefnCheckIfThenPaths(s, undefLabel, argsCheckDefn);
                    }
                };

                // rescue block
                CodeBlock rescueBlock = new CodeBlock() {
                    public Operand run(Object[] args) { return Nil.NIL; } // Nothing to do if we got an exception
                };

                // Try verifying definition, and if we get an exception, throw it out, and return nil
                return protectCodeWithRescue(s, protectedCode, new Object[]{s, iVisited, undefLabel}, rescueBlock, null);
            }
            case CLASSVARNODE:
            {
            // SSS FIXME: Is there a reason to do this all with low-level IR?
            // Can't this all be folded into a Java method that would be part
            // of the runtime library, which would be used both by the interpreter & the compiled code!

                /* --------------------------------------------------------------------------
                 * Generate IR for this ruby pseudo-code:
                 *   cm = tc.getCurrentScope.getStaticScope.getModule || self.metaclass
                 *   cm.isClassVarDefined ? "class variable" : nil
                 * ------------------------------------------------------------------------------ */
                ClassVarNode iVisited = (ClassVarNode) node;
                Variable cm = classVarDefinitionContainer(s, true);
                return buildDefinitionCheck(s, JRubyImplementationMethod.CLASS_VAR_DEFINED, cm, iVisited.getName(), "class variable");
            }
            case ATTRASSIGNNODE:
            {
                Label  undefLabel = s.getNewLabel();
                AttrAssignNode iVisited = (AttrAssignNode) node;
                Operand receiverDefn = buildGetDefinition(iVisited.getReceiverNode(), s);
                s.addInstr(new BEQInstr(receiverDefn, Nil.NIL, undefLabel));

                // protected main block
                CodeBlock protectedCode = new CodeBlock() {
                    public Operand run(Object[] args) {
                        /* --------------------------------------------------------------------------
                         * This basically combines checks from CALLNODE and FCALLNODE
                         *
                         * Generate IR for this sequence
                         *
                         *    1. r  = receiver
                         *    2. mc = r.metaClass
                         *    3. v  = mc.getVisibility(methodName)
                         *    4. f  = !v || v.isPrivate? || (v.isProtected? && receiver/self?.kindof(mc.getRealClass))
                         *    5. return !f && mc.methodBound(attrmethod) ? buildGetArgumentDefn(..) : false
                         *
                         * Hide the complexity of instrs 2-4 into a verifyMethodIsPublicAccessible call
                         * which can executely entirely in Java-land.  No reason to expose the guts in IR.
                         * ------------------------------------------------------------------------------ */
                        IRScope s = (IRScope)args[0];
                        AttrAssignNode iVisited = (AttrAssignNode)args[1];
                        Label undefLabel = (Label)args[2];
                        StringLiteral attrMethodName = new StringLiteral(iVisited.getName());
                        Variable tmpVar     = s.getNewTemporaryVariable();
                        Operand  receiver   = build(iVisited.getReceiverNode(), s);
                        s.addInstr(new JRubyImplCallInstr(tmpVar, JRubyImplementationMethod.METHOD_PUBLIC_ACCESSIBLE, receiver, new Operand[]{attrMethodName}));
                        s.addInstr(new BEQInstr(tmpVar, BooleanLiteral.FALSE, undefLabel));
                        s.addInstr(new JRubyImplCallInstr(tmpVar, JRubyImplementationMethod.SELF_IS_METHOD_BOUND, getSelf(s), new Operand[]{attrMethodName}));
                        s.addInstr(new BEQInstr(tmpVar, BooleanLiteral.FALSE, undefLabel));
                        Operand argsCheckDefn = buildGetArgumentDefinition(((FCallNode) node).getArgsNode(), s, "assignment");
                        return buildDefnCheckIfThenPaths(s, undefLabel, argsCheckDefn);
                    }
                };

                // rescue block
                CodeBlock rescueBlock = new CodeBlock() {
                    public Operand run(Object[] args) { return Nil.NIL; } // Nothing to do if we got an exception
                };

                // Try verifying definition, and if we get an JumpException exception, process it with the rescue block above
                return protectCodeWithRescue(s, protectedCode, new Object[]{s, iVisited, undefLabel}, rescueBlock, null);
            }
            case ZSUPERNODE:
                return buildDefinitionCheck(s, JRubyImplementationMethod.FRAME_SUPER_METHOD_BOUND, getSelf(s), null, "super");
            case SUPERNODE:
            {
                Label undefLabel = s.getNewLabel();
                Variable tmpVar  = s.getNewTemporaryVariable();
                s.addInstr(new JRubyImplCallInstr(tmpVar, JRubyImplementationMethod.FRAME_SUPER_METHOD_BOUND, getSelf(s), NO_ARGS));
                s.addInstr(new BEQInstr(tmpVar, BooleanLiteral.FALSE, undefLabel));
                Operand superDefnVal = buildGetArgumentDefinition(((SuperNode) node).getArgsNode(), s, "super");
                return buildDefnCheckIfThenPaths(s, undefLabel, superDefnVal);
            }
            default:
                // protected code
                CodeBlock protectedCode = new CodeBlock() {
                    public Operand run(Object[] args) { 
                        build((Node)args[0], (IRScope)args[1]);
                        // always an expression as long as we get through here without an exception!
                        return new StringLiteral("expression");
                    }
                };
                // rescue block
                CodeBlock rescueBlock = new CodeBlock() {
                    public Operand run(Object[] args) { return Nil.NIL; } // Nothing to do if we got an exception
                };

                // Try verifying definition, and if we get an JumpException exception, process it with the rescue block above
                return protectCodeWithRescue(s, protectedCode, new Object[]{node, s}, rescueBlock, null);
        }
    }

    public Operand buildDAsgn(final DAsgnNode dasgnNode, IRScope s) {
        // SSS: Looks like we receive the arg in buildBlockArgsAssignment via the IterNode
        // We won't get here for argument receives!  So, buildDasgn is called for
        // assignments to block variables within a block.  As far as the IR is concerned,
        // this is just a simple copy
        int depth = dasgnNode.getDepth();
        Variable arg = s.getLocalVariable(dasgnNode.getName(), depth);
        Operand  value = build(dasgnNode.getValueNode(), s);
        s.addInstr(new CopyInstr(arg, value));
        return value;

        // IMPORTANT: The return value of this method is value, not arg!
        //
        // Consider this Ruby code: foo((a = 1), (a = 2))
        //
        // If we return 'value' this will get translated to:
        //    a = 1
        //    a = 2
        //    call("foo", [1,2]) <---- CORRECT
        //
        // If we return 'arg' this will get translated to:
        //    a = 1
        //    a = 2
        //    call("foo", [a,a]) <---- BUGGY
        //
        // This technique only works if 'value' is an immutable value (ex: fixnum) or a variable
        // So, for Ruby code like this:   
        //     def foo(x); x << 5; end; 
        //     foo(a=[1,2]); 
        //     p a
        // we are guaranteed that the value passed into foo and 'a' point to the same object
        // because of the use of copyAndReturnValue method for literal objects.
    }

    private IRMethod defineNewMethod(MethodDefNode defNode, IRScope s, boolean isInstanceMethod) {
        IRMethod method = new IRMethod(s, defNode.getName(), isInstanceMethod, defNode.getScope());

        // Build IR for arguments
        receiveArgs(defNode.getArgsNode(), method);

        // Build IR for body
        if (defNode.getBodyNode() != null) {
            Node bodyNode = defNode.getBodyNode();

            // if root of method is rescue, build as a light rescue
            Operand rv = (bodyNode instanceof RescueNode) ?  buildRescue(bodyNode, method) : build(bodyNode, method);
            if (rv != null) method.addInstr(new ReturnInstr(rv));
        } else {
            method.addInstr(new ReturnInstr(Nil.NIL));
        }

        return method;
    }

    public Operand buildDefn(MethodDefNode node, IRScope s) { // Instance method
        Operand container;
        IRMethod method;

        // statically determine container where possible?
        // DefineIstanceMethod IR interpretation currently relies on this static determination for handling top-level methods
        if ((s instanceof IRMethod) && ((IRMethod)s).isAModuleRootMethod()) {
            method = defineNewMethod(node, s, true);
            // SSS NOTE: This is a debugging tool that works in most cases and is not used
            // at runtime by the executing code since this static nesting might be wrong.
            IRModule nm = s.getNearestModule();
            if (nm != null) nm.addMethod(method);
        }
        else {
            method = defineNewMethod(node, s, true);
        }
        s.addInstr(new DefineInstanceMethodInstr(new StringLiteral("--unused--"), method));
        return Nil.NIL;
    }

    public Operand buildDefs(DefsNode node, IRScope s) { // Class method
        Operand container =  build(node.getReceiverNode(), s);
        IRMethod method = defineNewMethod(node, s, false);
        // ENEBO: Can all metaobjects be used for this?  closure?
        //if (container instanceof MetaObject) {
        //    ((IRModule) ((MetaObject) container).getScope()).addMethod(method);
        //}
        if (s.getLexicalParent() instanceof IRModule) {
            ((IRModule)s.getLexicalParent()).addMethod(method);
        }
        s.addInstr(new DefineClassMethodInstr(container, method));
        return Nil.NIL;
    }

    // ENEBO: Since we are now targeting 1.9 semantics then it can be assumed that all
    // method parameters are now going to be local to this scope.
    public void receiveArgs(final ArgsNode argsNode, IRScope s) {
        final int required = argsNode.getRequiredArgsCount();
        final int opt = argsNode.getOptionalArgsCount();
        final int rest = argsNode.getRestArg();

        // FIXME: Expensive to this explicitly?  But, 2 advantages:
        // (a) on inlining, we'll be able to get rid of these checks in almost every case.
        // (b) compiler to bytecode will anyway generate this and this is explicit.
        // For now, we are going explicit instruction route.  But later, perhaps can make this implicit in the method setup preamble?  
        Operand[] args = new Operand[] { new Fixnum((long)required), new Fixnum((long)opt), new Fixnum((long)rest) };
        // FIXME: I added getSelf() just so we won't NPE since this is a callinstr. Can we make this something other than callinstr?
        generateJRubyUtilityCall(s, JRubyImplementationMethod.CHECK_ARITY, getSelf(s), args);

        // self = args[0]
        s.addInstr(new ReceiveSelfInstruction(getSelf(s)));

        // Other args begin at index 0
        int argIndex = 0;

        // Both for fixed arity and variable arity methods
        ListNode preArgs  = argsNode.getPre();
        for (int i = 0; i < required; i++, argIndex++) {
            ArgumentNode a = (ArgumentNode)preArgs.get(i);
            s.addInstr(new ReceiveArgumentInstruction(s.getLocalVariable(a.getName(), 0), argIndex));
        }

            // IMPORTANT: Receive the block argument before the opt and splat args
            // This is so that the *arg can be encoded as 'rest of the array'.  This
            // won't work if the block argument hasn't been received yet!
        Variable blockVar = null;
        if (argsNode.getBlock() != null) {
            blockVar = s.getLocalVariable(argsNode.getBlock().getName(), 0);
            s.addInstr(new ReceiveClosureInstr(blockVar));
        }

        // SSS FIXME: This instruction is only needed if there is an yield instr somewhere!
        // In addition, store the block argument in an implicit block variable
        Variable implicitBlockArg = ((IRExecutionScope)s).getImplicitBlockArg();
        if (blockVar == null)
            s.addInstr(new ReceiveClosureInstr(implicitBlockArg));
        else
            s.addInstr(new CopyInstr(implicitBlockArg, blockVar));

            // Now for the rest
        if (opt > 0) {
            ListNode optArgs = argsNode.getOptArgs();
            for (int j = 0; j < opt; j++, argIndex++) {
                    // Jump to 'l' if this arg is not null.  If null, fall through and build the default value!
                Label l = s.getNewLabel();
                LocalAsgnNode n = (LocalAsgnNode)optArgs.get(j);
                Variable av = s.getLocalVariable(n.getName(), 0);
                s.addInstr(new ReceiveOptionalArgumentInstr(av, argIndex));
                s.addInstr(new BNEInstr(av, UndefinedValue.UNDEFINED, l)); // if 'av' is not undefined, go to default
                build(n, s);
                s.addInstr(new LabelInstr(l));
            }
        }

        if (rest > -1) {
            // Consider: def foo(*); .. ; end
            // For this code, there is no argument name available from the ruby code.
            // So, we generate an implicit arg name
            String argName = argsNode.getRestArgNode().getName();
            argName = (argName.equals("")) ? "%_arg_array" : argName;
            s.addInstr(new ReceiveArgumentInstruction(s.getLocalVariable(argName, 0), argIndex, true));
        }

        // FIXME: Ruby 1.9 post args code needs to come here
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
        return copyAndReturnValue(s, new Range(build(dotNode.getBeginNode(), s), build(dotNode.getEndNode(), s), dotNode.isExclusive()));
    }
    
    private Operand dynamicPiece(Node pieceNode, IRScope s) {
        Operand piece = build(pieceNode, s);
        
        return piece == null ? Nil.NIL : piece;
    }

    public Operand buildDRegexp(DRegexpNode dregexpNode, IRScope s) {
        List<Operand> strPieces = new ArrayList<Operand>();
        for (Node n : dregexpNode.childNodes()) {
            strPieces.add(dynamicPiece(n, s));
        }

        return copyAndReturnValue(s, new Regexp(new CompoundString(strPieces), dregexpNode.getOptions()));
    }

    public Operand buildDStr(DStrNode dstrNode, IRScope s) {
        List<Operand> strPieces = new ArrayList<Operand>();
        for (Node n : dstrNode.childNodes()) {
            strPieces.add(dynamicPiece(n, s));
        }
        
        return copyAndReturnValue(s, new CompoundString(strPieces));
    }

    public Operand buildDSymbol(Node node, IRScope s) {
        List<Operand> strPieces = new ArrayList<Operand>();
        for (Node n : node.childNodes()) {
            strPieces.add(dynamicPiece(n, s));
        }

        return copyAndReturnValue(s, new DynamicSymbol(new CompoundString(strPieces)));
    }

    public Operand buildDVar(DVarNode node, IRScope m) {
        return m.getLocalVariable(node.getName(), node.getDepth());
    }

    public Operand buildDXStr(final DXStrNode dstrNode, IRScope m) {
        List<Operand> strPieces = new ArrayList<Operand>();
        for (Node nextNode : dstrNode.childNodes()) {
            strPieces.add(dynamicPiece(nextNode, m));
        }

        return copyAndReturnValue(m, new BacktickString(strPieces));
    }

    /* ****************************************************************
     * Consider the ensure-protected ruby code below:

           begin
             .. protected body ..
           ensure
             .. eb code
           end

       This ruby code is effectively rewritten into the following ruby code

          begin
            .. protected body ..
          rescue <any-exception-or-error> => e
            jump to eb-code, execute it, and come back here
            raise e
          end
          
      which in IR looks like this:

          L1:
            Exception region start marker
            ... IR for protected body ...
            Exception region end marker
            %v = L3       <--- skipped if the protected body had a return!
          L2:
            .. IR for ensure block ..
            jump_indirect %v
          L10:            <--- dummy rescue block
            e = recv_exception
            %v = L11
            jump L2
          L11:
            throw e
          L3:
     
     * ****************************************************************/
    public Operand buildEnsureNode(Node node, IRScope m) {
        EnsureNode ensureNode = (EnsureNode)node;
        Node       bodyNode   = ensureNode.getBodyNode();

        // Push a new ensure block info node onto the stack of ensure block
        EnsureBlockInfo ebi = new EnsureBlockInfo(m);
        _ensureBlockStack.push(ebi);

        Label rBeginLabel = ebi.regionStart;
        Label rEndLabel   = ebi.end;

        // start of protected region
        m.addInstr(new LabelInstr(rBeginLabel));
        m.addInstr(new ExceptionRegionStartMarkerInstr(rBeginLabel, rEndLabel, ebi.dummyRescueBlockLabel, ebi.dummyRescueBlockLabel));

        // Generate IR for code being protected
        Operand rv;  
        if (bodyNode instanceof RescueNode) {
            // The rescue code will ensure that the region is ended
            rv = buildRescueInternal(bodyNode, m, ebi);
        } else {
            rv = build(bodyNode, m);

            // Jump to start of ensure block -- dont bother if we had a return in the protected body 
            if (rv != U_NIL) m.addInstr(new SetReturnAddressInstr(ebi.returnAddr, rEndLabel));
        }

        // end of protected region
        m.addInstr(new ExceptionRegionEndMarkerInstr()); 

        // Pop the current ensure block info node *BEFORE* generating the ensure code for this block itself!
        _ensureBlockStack.pop();

        // Generate the ensure block now
        m.addInstr(new LabelInstr(ebi.start));

        // Two cases:
        // 1. Ensure block has no explicit return => the result of the entire ensure expression is the result of the protected body.
        // 2. Ensure block has an explicit return => the result of the protected body is ignored.
        Operand ensureRetVal = (ensureNode.getEnsureNode() == null) ? Nil.NIL : build(ensureNode.getEnsureNode(), m);
        // U_NIL => there was a return from within the ensure block!
        if (ensureRetVal == U_NIL) rv = U_NIL;

        m.addInstr(new JumpIndirectInstr(ebi.returnAddr));

        // Now build the dummy rescue block that:
        // * catches all exceptions thrown by the body
        // * jumps to the ensure block code
        // * returns back (via set_retaddr instr)
        // * rethrows the caught exception
        Label rethrowExcLabel = m.getNewLabel();
        Variable exc = m.getNewTemporaryVariable();
        m.addInstr(new LabelInstr(ebi.dummyRescueBlockLabel));
        m.addInstr(new ReceiveExceptionInstr(exc));
        m.addInstr(new SetReturnAddressInstr(ebi.returnAddr, rethrowExcLabel));
        m.addInstr(new JumpInstr(ebi.start));
        m.addInstr(new LabelInstr(rethrowExcLabel));
        m.addInstr(new ThrowExceptionInstr(exc));

        // End label for the exception region
        m.addInstr(new LabelInstr(rEndLabel));

        return rv;
    }

    public Operand buildEvStr(EvStrNode node, IRScope s) {
        return new AsString(build(node.getBody(), s));
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
        Instr         callInstr    = CallInstr.create(CallType.FUNCTIONAL, callResult, new MethAddr(fcallNode.getName()), getSelf(s), args.toArray(new Operand[args.size()]), block);
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

    public Operand buildFlip(Node node, IRScope m) {
        /* ----------------------------------------------------------------------
         * Consider a simple 2-state (s1, s2) FSM with the following transitions:
         *
         *     new_state(s1, F) = s1
         *     new_state(s1, T) = s2
         *     new_state(s2, F) = s2
         *     new_state(s2, T) = s1
         *
         * Here is the pseudo-code for evaluating the flip-node.
         * Let 'v' holds the value of the current state. 
         *
         *    1. if (v == 's1') f1 = eval_condition(s1-condition); v = new_state(v, f1); ret = f1
         *    2. if (v == 's2') f2 = eval_condition(s2-condition); v = new_state(v, f2); ret = true
         *    3. return ret
         *
         * For exclusive flip conditions, line 2 changes to:
         *    2. if (!f1 && (v == 's2')) f2 = eval_condition(s2-condition); v = new_state(v, f2)
         *
         * In IR code below, we are representing the two states as 1 and 2.  Any
         * two values are good enough (even true and false), but 1 and 2 is simple
         * enough and also makes the IR output readable 
         * ---------------------------------------------------------------------- */
        final FlipNode flipNode = (FlipNode)node;

        Fixnum s1 = new Fixnum((long)1);
        Fixnum s2 = new Fixnum((long)2);

        // Create a variable to hold the flip state 
        IRMethod nearestMethod = m.getNearestMethod();
        Variable flipState = nearestMethod.getNewFlipStateVariable();
        nearestMethod.initFlipStateVariable(flipState, s1);
        if (m instanceof IRClosure) {
            flipState = ((LocalVariable)flipState).cloneForDepth(((IRClosure)m).getNestingDepth());
        }

        // Variables and labels needed for the code
        Variable returnVal = m.getNewTemporaryVariable();
        Label    s2Label   = m.getNewLabel();
        Label    doneLabel = m.getNewLabel();

        // Init
        m.addInstr(new CopyInstr(returnVal, BooleanLiteral.FALSE));

        // Are we in state 1?
        m.addInstr(new BNEInstr(flipState, s1, s2Label));

        // ----- Code for when we are in state 1 -----
        Operand s1Val = build(flipNode.getBeginNode(), m);
        m.addInstr(new BNEInstr(s1Val, BooleanLiteral.TRUE, s2Label));

        // s1 condition is true => set returnVal to true & move to state 2
        m.addInstr(new CopyInstr(returnVal, BooleanLiteral.TRUE));
        m.addInstr(new CopyInstr(flipState, s2));

        // Check for state 2
        m.addInstr(new LabelInstr(s2Label));

        // For exclusive ranges/flips, we dont evaluate s2's condition if s1's condition was satisfied
        if (flipNode.isExclusive()) m.addInstr(new BEQInstr(returnVal, BooleanLiteral.TRUE, doneLabel));

        // Are we in state 2?
        m.addInstr(new BNEInstr(flipState, s2, doneLabel));

        // ----- Code for when we are in state 2 -----
        Operand s2Val = build(flipNode.getEndNode(), m);
        m.addInstr(new CopyInstr(returnVal, BooleanLiteral.TRUE));
        m.addInstr(new BNEInstr(s2Val, BooleanLiteral.TRUE, doneLabel));

        // s2 condition is true => move to state 1 
        m.addInstr(new CopyInstr(flipState, s1));

        // Done testing for s1's and s2's conditions.  
        // returnVal will have the result of the flip condition
        m.addInstr(new LabelInstr(doneLabel));

        return returnVal;
    }

    public Operand buildFloat(FloatNode node, IRScope m) {
        // SSS: Since flaot literals are effectively interned objects, no need to copyAndReturnValue(...)
        // Or is this a premature optimization?
        return new Float(node.getValue());
    }

    public Operand buildFor(ForNode forNode, IRExecutionScope m) {
        Variable ret      = m.getNewTemporaryVariable();
        Operand  receiver = build(forNode.getIterNode(), m);
        Operand  forBlock = buildForIter(forNode, m);     
        // SSS FIXME: Really?  Why the internal call?
        m.addInstr(new RubyInternalCallInstr(ret, RubyInternalsMethod.FOR_EACH, receiver, NO_ARGS, forBlock));
        return ret;
    }

    public Operand buildForIter(final ForNode forNode, IRExecutionScope s) {
            // Create a new closure context
        IRClosure closure = new IRClosure(s, true, forNode.getScope(), Arity.procArityOf(forNode.getVarNode()), forNode.getArgumentType());
        s.addClosure(closure);

            // Receive self
        closure.addInstr(new ReceiveSelfInstruction(getSelf(closure)));

            // Build args
        NodeType argsNodeId = null;
        if (forNode.getVarNode() != null) {
            argsNodeId = forNode.getVarNode().getNodeType();
            if (argsNodeId != null)
                buildBlockArgsAssignment(forNode.getVarNode(), closure, 0, false, true, false);
        }

            // Start label -- used by redo!
        closure.addInstr(new LabelInstr(closure.startLabel));

            // Build closure body and return the result of the closure
        Operand closureRetVal = forNode.getBodyNode() == null ? Nil.NIL : build(forNode.getBodyNode(), closure);
        if (closureRetVal != null)  // can be null if the node is an if node with returns in both branches.
            closure.addInstr(new ClosureReturnInstr(closureRetVal));

        return MetaObject.create(closure);
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
            return copyAndReturnValue(m, new Hash(new ArrayList<KeyValuePair>()));
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
            return copyAndReturnValue(m, new Hash(args));
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

        Variable result     = null;
        Label    falseLabel = s.getNewLabel();
        Label    doneLabel  = s.getNewLabel();
        Operand  thenResult = null;
        s.addInstr(new BEQInstr(build(actualCondition, s), BooleanLiteral.FALSE, falseLabel));

        boolean thenNull = false;
        boolean elseNull = false;
        boolean thenUnil = false;
        boolean elseUnil = false;

        // Build the then part of the if-statement
        if (ifNode.getThenBody() != null) {
            thenResult = build(ifNode.getThenBody(), s);
            if (thenResult != U_NIL) { // thenResult can be U_NIL if then-body ended with a return!
                // SSS FIXME: Can look at the last instr and short-circuit this jump if it is a break rather
                // than wait for dead code elimination to do it
                Label tgt = doneLabel;
                result = getValueInTemporaryVariable(s, thenResult);
                s.addInstr(new JumpInstr(tgt));
            }
            else {
                result = s.getNewTemporaryVariable();
                thenUnil = true;
            }
        }
        else {
            thenNull = true;
            result = s.getNewTemporaryVariable();
            s.addInstr(new CopyInstr(result, Nil.NIL));
            s.addInstr(new JumpInstr(doneLabel));
        }

        // Build the else part of the if-statement
        s.addInstr(new LabelInstr(falseLabel));
        if (ifNode.getElseBody() != null) {
            Operand elseResult = build(ifNode.getElseBody(), s);
            // elseResult can be U_NIL if then-body ended with a return!
            if (elseResult != U_NIL) {
                s.addInstr(new CopyInstr(result, elseResult));
            }
            else {
                elseUnil = true;
            }
        }
        else {
            elseNull = true;
            s.addInstr(new CopyInstr(result, Nil.NIL));
        }

        if (thenNull && elseNull) {
            s.addInstr(new LabelInstr(doneLabel));
            return Nil.NIL;
        }
        else if (thenUnil && elseUnil) {
            return U_NIL;
        }
        else {
            s.addInstr(new LabelInstr(doneLabel));
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
        IRClosure closure = new IRClosure(s, false, iterNode.getScope(), Arity.procArityOf(iterNode.getVarNode()), iterNode.getArgumentType());
        s.addClosure(closure);

        // Create a new nested builder to ensure this gets its own IR builder state 
        // like the ensure block stack
        IRBuilder closureBuilder = new IRBuilder();

            // Receive self
        closure.addInstr(new ReceiveSelfInstruction(getSelf(closure)));

            // Build args
        NodeType argsNodeId = BlockBody.getArgumentTypeWackyHack(iterNode);
        if ((iterNode.getVarNode() != null) && (argsNodeId != null))
            closureBuilder.buildBlockArgsAssignment(iterNode.getVarNode(), closure, 0, false, true, false);
        if (iterNode.getBlockVarNode() != null)
            closureBuilder.buildBlockArgsAssignment(iterNode.getBlockVarNode(), closure, 0, true, true, false);

            // start label -- used by redo!
        closure.addInstr(new LabelInstr(closure.startLabel));

            // Build closure body and return the result of the closure
        Operand closureRetVal = iterNode.getBodyNode() == null ? Nil.NIL : closureBuilder.build(iterNode.getBodyNode(), closure);
        if (closureRetVal != U_NIL)  // can be U_NIL if the node is an if node with returns in both branches.
            closure.addInstr(new ClosureReturnInstr(closureRetVal));

        return MetaObject.create(closure);
    }

    public Operand buildLiteral(LiteralNode literalNode, IRScope s) {
        return copyAndReturnValue(s, new StringLiteral(literalNode.getName()));
    }

    public Operand buildLocalAsgn(LocalAsgnNode localAsgnNode, IRScope s) {
        Variable var  = s.getLocalVariable(localAsgnNode.getName(), localAsgnNode.getDepth());
        Operand value = build(localAsgnNode.getValueNode(), s);
        s.addInstr(new CopyInstr(var, value));
        return value;  

        // IMPORTANT: The return value of this method is value, not var!
        //
        // Consider this Ruby code: foo((a = 1), (a = 2))
        //
        // If we return 'value' this will get translated to:
        //    a = 1
        //    a = 2
        //    call("foo", [1,2]) <---- CORRECT
        //
        // If we return 'var' this will get translated to:
        //    a = 1
        //    a = 2
        //    call("foo", [a,a]) <---- BUGGY
        //
        // This technique only works if 'value' is an immutable value (ex: fixnum) or a variable
        // So, for Ruby code like this:   
        //     def foo(x); x << 5; end; 
        //     foo(a=[1,2]); 
        //     p a
        // we are guaranteed that the value passed into foo and 'a' point to the same object
        // because of the use of copyAndReturnValue method for literal objects.
    }

    public Operand buildLocalVar(LocalVarNode node, IRScope s) {
        return s.getLocalVariable(node.getName(), node.getDepth());
    }

    public Operand buildMatch(MatchNode matchNode, IRScope m) {
        Operand regexp = build(matchNode.getRegexpNode(), m);
        return generateJRubyUtilityCall(m, JRubyImplementationMethod.MATCH, regexp, NO_ARGS);
    }

    public Operand buildMatch2(Match2Node matchNode, IRScope m) {
        Operand receiver = build(matchNode.getReceiverNode(), m);
        Operand value    = build(matchNode.getValueNode(), m);
        return generateJRubyUtilityCall(m, JRubyImplementationMethod.MATCH2, receiver, new Operand[]{value});
    }

    public Operand buildMatch3(Match3Node matchNode, IRScope m) {
        Operand receiver = build(matchNode.getReceiverNode(), m);
        Operand value    = build(matchNode.getValueNode(), m);
        return generateJRubyUtilityCall(m, JRubyImplementationMethod.MATCH3, receiver, new Operand[]{value});
    }

    private Operand getContainerFromCPath(Colon3Node cpath, IRScope s) {
        Operand container = null;

        if (cpath instanceof Colon2Node) {
            Node leftNode = ((Colon2Node) cpath).getLeftNode();
            
            if (leftNode != null) { // Foo::Bar
                container = build(leftNode, s);
            } else { // Only name with no left-side Bar <- Note no :: on left
                container = findContainerModule(s);
            }
        } else { //::Bar
            container = MetaObject.create(IRClass.getCoreClass("Object"));
        }

        return container;
    }

    public Operand buildModule(ModuleNode moduleNode, IRScope s) {
        Colon3Node cpath = moduleNode.getCPath();
        String moduleName = cpath.getName();
        Operand container = getContainerFromCPath(cpath, s);

        // Build the new module
        IRModule m = new IRModule(s, moduleName, moduleNode.getScope());
        Variable ret = s.getNewTemporaryVariable();
        s.addInstr(new DefineModuleInstr(m, ret, container));
        // SSS NOTE: This is a debugging tool that works in most cases and is not used
        // at runtime by the executing code since this static nesting might be wrong.
        IRModule nm = s.getNearestModule();
        if (nm != null) nm.addModule(m);

        IRMethod rootMethod = m.getRootMethod();
        Operand rv = build(moduleNode.getBodyNode(), rootMethod);
        if (rv != null) rootMethod.addInstr(new ReturnInstr(rv));

        return ret;
    }

    public Operand buildMultipleAsgn(MultipleAsgnNode multipleAsgnNode, IRScope s) {
        Operand  values = build(multipleAsgnNode.getValueNode(), s);
        Variable ret = getValueInTemporaryVariable(s, values);
        buildMultipleAsgnAssignment(multipleAsgnNode, s, ret);
        return ret;
    }

    // SSS: This method is called both for regular multiple assignment as well as argument passing
    //
    // Ex: a,b,*c=v  is a regular assignment and in this case, the "values" operand will be non-null
    // Ex: { |a,b,*c| ..} is the argument passing case
    public void buildMultipleAsgnAssignment(final MultipleAsgnNode multipleAsgnNode, IRScope s, Operand values) {
        final ListNode sourceArray = multipleAsgnNode.getHeadNode();

        // First, build assignments for specific named arguments
        int i = 0; 
        if (sourceArray != null) {
            ListNode headNode = (ListNode) sourceArray;
            for (Node an: headNode.childNodes()) {
                if (values == null) {
                    buildBlockArgsAssignment(an, s, i, false, false, false);
                } else {
                    buildAssignment(an, s, values, i, false);
                }
                i++;
            }
        }

        // First, build an assignment for a splat, if any, with the rest of the args!
        Node argsNode = multipleAsgnNode.getArgsNode();
        if (argsNode == null) {
            if (sourceArray == null)
                throw new NotCompilableException("Something's wrong, multiple assignment with no head or args at: " + multipleAsgnNode.getPosition());
        } 
        else if (argsNode instanceof StarNode) {
            // do nothing
        } 
        else if (values != null) {
            buildAssignment(argsNode, s, values, i, true); // rest of the argument array!
        }
        else {
            buildBlockArgsAssignment(argsNode, s, i, false, false, true); // rest of the argument array!
        }

    }

    public Operand buildNewline(NewlineNode node, IRScope s) {
        return build(skipOverNewlines(s, node), s);
    }

    public Operand buildNext(final NextNode nextNode, IRExecutionScope s) {
        Operand rv = (nextNode.getValueNode() == null) ? Nil.NIL : build(nextNode.getValueNode(), s);
        s.addInstr(new ThreadPollInstr()); // SSS FIXME: Is the ordering correct? (poll before next)

        // If we have ensure blocks, have to run those first!
        if (!_ensureBlockStack.empty()) EnsureBlockInfo.emitJumpChain(s, _ensureBlockStack);
        if (s.getCurrentLoop() != null) {
            // If a regular loop, the next is simply a jump to the end of the iteration
            s.addInstr(new JumpInstr(s.getCurrentLoop().iterEndLabel));
        }
        else {
            // If a closure, the next is simply a return from the closure!
            if (s instanceof IRClosure) s.addInstr(new ClosureReturnInstr(rv));
            else s.addInstr(new ThrowExceptionInstr(IRException.NEXT_LocalJumpError));
        }
        return rv;
    }

    public Operand buildNthRef(NthRefNode nthRefNode, IRScope m) {
        return copyAndReturnValue(m, new NthRef(nthRefNode.getMatchNumber()));
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
        Label l = null;
        Variable readerValue = s.getNewTemporaryVariable();
        Variable writerValue = s.getNewTemporaryVariable();

        // get attr
        Operand  v1 = build(opAsgnNode.getReceiverNode(), s);
        s.addInstr(CallInstr.create(readerValue, new MethAddr(opAsgnNode.getVariableName()), v1, NO_ARGS, null));

        // Ex: e.val ||= n
        //     e.val &&= n
        String opName = opAsgnNode.getOperatorName();
        if (opName.equals("||") || opName.equals("&&")) {
            l = s.getNewLabel();
            Variable flag = s.getNewTemporaryVariable();
            s.addInstr(new IsTrueInstr(flag, readerValue));
            s.addInstr(new BEQInstr(flag, opName.equals("||") ? BooleanLiteral.TRUE : BooleanLiteral.FALSE, l));

            // compute value and set it
            Operand  v2 = build(opAsgnNode.getValueNode(), s);
            s.addInstr(CallInstr.create(writerValue, new MethAddr(opAsgnNode.getVariableNameAsgn()), v1, new Operand[] {v2}, null));
            s.addInstr(new CopyInstr(readerValue, writerValue));
            s.addInstr(new LabelInstr(l));

            return readerValue;
        }
        // Ex: e.val = e.val.f(n)
        else {
            // call operator
            Operand  v2 = build(opAsgnNode.getValueNode(), s);
            Variable setValue = s.getNewTemporaryVariable();
            s.addInstr(CallInstr.create(setValue, new MethAddr(opAsgnNode.getOperatorName()), readerValue, new Operand[]{v2}, null));
           
            // set attr
            s.addInstr(CallInstr.create(writerValue, new MethAddr(opAsgnNode.getVariableNameAsgn()), v1, new Operand[] {setValue}, null));
            return writerValue;
        }
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
        Variable result = getValueInTemporaryVariable(s, v1);
        Variable f = s.getNewTemporaryVariable();
        s.addInstr(new IsTrueInstr(f, v1));
        s.addInstr(new BEQInstr(f, BooleanLiteral.FALSE, l));
        Operand v2 = build(andNode.getSecondNode(), s);  // This does the assignment!
        s.addInstr(new CopyInstr(result, v2));
        s.addInstr(new LabelInstr(l));
        s.addInstr(new ThreadPollInstr());
        return result;
    }

    // FIXME: This logic is not quite right....marked extra branch checks
    // to make sure the value is not defined but nil.  Nil will trigger ||=
    // rhs expression.
    //
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
        Variable flag = s.getNewTemporaryVariable();
        Operand  v1;
        boolean  needsDefnCheck = needsDefinitionCheck(orNode.getFirstNode());
        if (needsDefnCheck) {
            l2 = s.getNewLabel();
            v1 = buildGetDefinitionBase(orNode.getFirstNode(), s);
            s.addInstr(new CopyInstr(flag, v1));
            s.addInstr(new BEQInstr(flag, Nil.NIL, l2)); // if v1 is undefined, go to v2's computation
        }
        v1 = build(orNode.getFirstNode(), s); // build of 'x'
        Variable result = getValueInTemporaryVariable(s, v1);
        s.addInstr(new IsTrueInstr(flag, v1));
        if (needsDefnCheck) {
            s.addInstr(new LabelInstr(l2));
        }
        s.addInstr(new BEQInstr(flag, BooleanLiteral.TRUE, l1));  // if v1 is defined and true, we are done! 
        Operand v2 = build(orNode.getSecondNode(), s); // This is an AST node that sets x = y, so nothing special to do here.
        s.addInstr(new CopyInstr(result, v2));
        s.addInstr(new LabelInstr(l1));
        s.addInstr(new ThreadPollInstr());

        // Return value of x ||= y is always 'x'
        return result;
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
        Label    l     = s.getNewLabel();
        Variable elt   = s.getNewTemporaryVariable();
        Variable flag  = s.getNewTemporaryVariable();
        List<Operand> argList = setupCallArgs(opElementAsgnNode.getArgsNode(), s);
        s.addInstr(CallInstr.create(elt, new MethAddr("[]"), array, argList.toArray(new Operand[argList.size()]), null));
        s.addInstr(new IsTrueInstr(flag, elt));
        s.addInstr(new BEQInstr(flag, BooleanLiteral.TRUE, l));
        Operand value = build(opElementAsgnNode.getValueNode(), s);
        argList.add(value);
        s.addInstr(CallInstr.create(elt, new MethAddr("[]="), array, argList.toArray(new Operand[argList.size()]), null));
        s.addInstr(new CopyInstr(elt, value));
        s.addInstr(new LabelInstr(l));
        return elt;
    }

    // Translate "a[x] &&= n" --> "a[x] = n if is_true(a[x])"
    public Operand buildOpElementAsgnWithAnd(Node node, IRScope s) {
        final OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode) node;
        Operand array = build(opElementAsgnNode.getReceiverNode(), s);
        Label    l     = s.getNewLabel();
        Variable elt   = s.getNewTemporaryVariable();
        Variable flag  = s.getNewTemporaryVariable();
        List<Operand> argList = setupCallArgs(opElementAsgnNode.getArgsNode(), s);
        s.addInstr(CallInstr.create(elt, new MethAddr("[]"), array, argList.toArray(new Operand[argList.size()]), null));
        s.addInstr(new IsTrueInstr(flag, elt));
        s.addInstr(new BEQInstr(flag, BooleanLiteral.FALSE, l));
        Operand value = build(opElementAsgnNode.getValueNode(), s);
        argList.add(value);
        s.addInstr(CallInstr.create(elt, new MethAddr("[]="), array, argList.toArray(new Operand[argList.size()]), null));
        s.addInstr(new CopyInstr(elt, value));
        s.addInstr(new LabelInstr(l));
        return elt;
    }

    // a[i] *= n, etc.  anything that is not "a[i] &&= .. or a[i] ||= .."
    //    arr = build(a) <-- receiver
    //    arg = build(x) <-- args
    //    elt = buildCall([], arr, arg)
    //    val = build(n) <-- val
    //    val = buildCall(METH, elt, val)
    //    val = buildCall([]=, arr, arg, val)
    public Operand buildOpElementAsgnWithMethod(Node node, IRScope s) {
        final OpElementAsgnNode opElementAsgnNode = (OpElementAsgnNode) node;

        Operand array = build(opElementAsgnNode.getReceiverNode(), s);
        List<Operand> argList = setupCallArgs(opElementAsgnNode.getArgsNode(), s);
        Variable elt = s.getNewTemporaryVariable();
        s.addInstr(CallInstr.create(elt, new MethAddr("[]"), array, argList.toArray(new Operand[argList.size()]), null)); // elt = a[args]
        Operand value = build(opElementAsgnNode.getValueNode(), s);                                       // Load 'value'
        String  operation = opElementAsgnNode.getOperatorName();
        s.addInstr(CallInstr.create(elt, new MethAddr(operation), elt, new Operand[] { value }, null)); // elt = elt.OPERATION(value)
        // SSS: do not load the call result into 'elt' to eliminate the RAW dependency on the call
        // We already know what the result is going be .. we are just storing it back into the array
        Variable tmp = s.getNewTemporaryVariable();
        argList.add(elt);
        s.addInstr(CallInstr.create(tmp, new MethAddr("[]="), array, argList.toArray(new Operand[argList.size()]), null));   // a[args] = elt
        return elt;
    }

    // Translate ret = (a || b) to ret = (a ? true : b) as follows
    // 
    //    v1 = -- build(a) --
    //       OPT: ret can be set to v1, but effectively v1 is true if we take the branch to L.
    //            while this info can be inferred by using attributes, why bother if we can do this?
    //    ret = v1
    //    beq(v1, true, L)
    //    v2 = -- build(b) --
    //    ret = v2
    // L:
    //
    public Operand buildOr(final OrNode orNode, IRScope m) {
        if (orNode.getFirstNode().getNodeType().alwaysTrue()) {
            // build first node only and return true
            return build(orNode.getFirstNode(), m);
        } else if (orNode.getFirstNode().getNodeType().alwaysFalse()) {
            // build first node as non-expr and build second node
            build(orNode.getFirstNode(), m);
            return build(orNode.getSecondNode(), m);
        } else {
            Label    l   = m.getNewLabel();
            Operand  v1  = build(orNode.getFirstNode(), m);
            Variable ret = getValueInTemporaryVariable(m, v1);
            m.addInstr(new BEQInstr(v1, BooleanLiteral.TRUE, l));
            Operand  v2  = build(orNode.getSecondNode(), m);
            m.addInstr(new CopyInstr(ret, v2));
            m.addInstr(new LabelInstr(l));
            return ret;
        }
    }

    public Operand buildPostExe(Node node, IRScope s) {
        final PostExeNode postExeNode = (PostExeNode) node;
        IRClosure endClosure = new IRClosure(s, false, postExeNode.getScope(), Arity.procArityOf(postExeNode.getVarNode()), postExeNode.getArgumentType());
        build(postExeNode.getBodyNode(), endClosure);

        // Add an instruction to record the end block at runtime
         Variable tmpVar = s.getNewTemporaryVariable();
        s.addInstr(new JRubyImplCallInstr(tmpVar, JRubyImplementationMethod.RECORD_END_BLOCK, null, new Operand[]{MetaObject.create(s), MetaObject.create(endClosure)}));
        return Nil.NIL;
    }

    public Operand buildPreExe(Node node, IRScope s) {
        final PreExeNode preExeNode = (PreExeNode) node;
        IRClosure beginClosure = new IRClosure(s, false, preExeNode.getScope(), Arity.procArityOf(preExeNode.getVarNode()), preExeNode.getArgumentType());
        build(preExeNode.getBodyNode(), beginClosure);

        // Record the begin block at IR build time
        s.getTopLevelScope().recordBeginBlock(beginClosure);
        return Nil.NIL;
    }

    public Operand buildRedo(Node node, IRExecutionScope s) {
        // For closures, a redo is a jump to the beginning of the closure
        // For non-closures, a redo is a jump to the beginning of the loop
        if (s instanceof IRClosure) {
            s.addInstr(new JumpInstr((s.getCurrentLoop() != null) ?  s.getCurrentLoop().iterStartLabel : ((IRClosure)s).startLabel));
        }
        else {
            s.addInstr(new ThrowExceptionInstr(IRException.REDO_LocalJumpError));
        }
        return Nil.NIL;
    }

    public Operand buildRegexp(RegexpNode reNode, IRScope m) {
        return copyAndReturnValue(m, new Regexp(new StringLiteral(reNode.getValue()), reNode.getOptions()));
    }

    public Operand buildRescue(Node node, IRScope m) {
        return buildRescueInternal(node, m, null);
    }

    private Operand buildRescueInternal(Node node, IRScope m, EnsureBlockInfo ensure) {
        final RescueNode rescueNode = (RescueNode) node;

        // Labels marking start, else, end of the begin-rescue(-ensure)-end block
        Label rBeginLabel = ensure == null ? m.getNewLabel() : ensure.regionStart;
        Label rEndLabel   = ensure == null ? m.getNewLabel() : ensure.end;
        Label rescueLabel = m.getNewLabel(); // Label marking start of the first rescue code.

        if (ensure == null) m.addInstr(new LabelInstr(rBeginLabel));

        // Placeholder rescue instruction that tells rest of the compiler passes the boundaries of the rescue block.
        m.addInstr(new ExceptionRegionStartMarkerInstr(rBeginLabel, rEndLabel, ensure == null ? null : ensure.dummyRescueBlockLabel, rescueLabel));

        // Save $! in a temp var so it can be restored when the exception gets handled -- Ruby ugliness.
        // Not sure why an exception needs to be saved!
        Variable savedGlobalException = m.getNewTemporaryVariable();
        m.addInstr(new GetGlobalVariableInstr(savedGlobalException, "$!"));
        if (ensure != null) ensure.savedGlobalException = savedGlobalException;
        _rescueBlockStack.push(new Tuple<Label, Variable>(rBeginLabel, savedGlobalException));

        // Body
        Operand tmp = Nil.NIL;  // default return value if for some strange reason, we neither have the body node or the else node!
        Variable rv = m.getNewTemporaryVariable();
        if (rescueNode.getBodyNode() != null) tmp = build(rescueNode.getBodyNode(), m);

        // Since rescued regions are well nested within Ruby, this bare marker is sufficient to
        // let us discover the edge of the region during linear traversal of instructions during cfg construction.
        ExceptionRegionEndMarkerInstr rbEndInstr = new ExceptionRegionEndMarkerInstr();
        m.addInstr(rbEndInstr);

        // Else part of the body -- we simply fall through from the main body if there were no exceptions
        Label elseLabel = rescueNode.getElseNode() == null ? null : m.getNewLabel();
        if (elseLabel != null) {
            m.addInstr(new LabelInstr(elseLabel));
            tmp = build(rescueNode.getElseNode(), m);
        }

        if (tmp != U_NIL) {
            m.addInstr(new CopyInstr(rv, tmp));

            // No explicit return from the protected body
            // - If we dont have any ensure blocks, simply jump to the end of the rescue block
            // - If we do, get the innermost ensure block, set up the return address to the end of the ensure block, and go execute the ensure code.
            if (ensure == null) {
                m.addInstr(new JumpInstr(rEndLabel));
            } else {
                // NOTE: rEndLabel is identical to ensure.end, but less confusing to use rEndLabel since that makes more semantic sense
                m.addInstr(new SetReturnAddressInstr(ensure.returnAddr, rEndLabel));
                m.addInstr(new JumpInstr(ensure.start));
            }
        } else {
            // If the body had an explicit return, the return instruction IR build takes care of setting
            // up execution of all necessary ensure blocks.  So, nothing to do here!  
            //
            // Additionally, the value in 'rv' will never be used, so need to set it to any specific value.
            // So, we can leave it undefined.  If on the other hand, there was an exception in that block,
            // 'rv' will get set in the rescue handler -- see the 'rv' being passed into
            // buildRescueBodyInternal below.  So, in either case, we are good!
        }

        // Build the actual rescue block(s)
        m.addInstr(new LabelInstr(rescueLabel));
        buildRescueBodyInternal(m, rescueNode.getRescueNode(), rv, rEndLabel);

        // End label -- only if there is no ensure block!  With an ensure block, you end at ensureEndLabel.
        if (ensure == null) m.addInstr(new LabelInstr(rEndLabel));

        _rescueBlockStack.pop();
        return rv;
    }

    private void outputExceptionCheck(IRScope m, Operand excType, Operand excObj, Label caughtLabel) {
        Variable eqqResult = m.getNewTemporaryVariable();
        m.addInstr(new RescueEQQInstr(eqqResult, excType, excObj));
        m.addInstr(new BEQInstr(eqqResult, BooleanLiteral.TRUE, caughtLabel));
    }

    private void buildRescueBodyInternal(IRScope m, Node node, Variable rv, Label endLabel) {
        final RescueBodyNode rescueBodyNode = (RescueBodyNode) node;
        final Node exceptionList = rescueBodyNode.getExceptionNodes();

        // Load exception & exception comparison type
        Variable exc = m.getNewTemporaryVariable();
        m.addInstr(new ReceiveExceptionInstr(exc));

        // Compare and branch as necessary!
        Label uncaughtLabel = m.getNewLabel();
        Label caughtLabel = m.getNewLabel();
        if (exceptionList != null) {
            if (exceptionList instanceof ListNode) {
               for (Node excType : ((ListNode) exceptionList).childNodes()) {
                   outputExceptionCheck(m, build(excType, m), exc, caughtLabel);
               }
            } else { // splatnode, catch 
                outputExceptionCheck(m, build(((SplatNode)exceptionList).getValue(), m), exc, caughtLabel);
            }
        }
        else {
            // FIXME:
            // rescue => e AND rescue implicitly EQQ the exception object with StandardError
            // We generate explicit IR for this test here.  But, this can lead to inconsistent
            // behavior (when compared to MRI) in certain scenarios.  See example:
            //
            //   self.class.const_set(:StandardError, 1) 
            //   begin; raise TypeError.new; rescue; puts "AHA"; end
            //
            // MRI rescues the error, but we will raise an exception because of reassignment
            // of StandardError.  I am ignoring this for now and treating this as undefined behavior. 
            // 
            Variable v = m.getNewTemporaryVariable();
            m.addInstr(new SearchConstInstr(v, null, "StandardError"));
            outputExceptionCheck(m, v, exc, caughtLabel);
        }

        // Uncaught exception -- build other rescue nodes or rethrow!
        m.addInstr(new LabelInstr(uncaughtLabel));
        if (rescueBodyNode.getOptRescueNode() != null) {
            buildRescueBodyInternal(m, rescueBodyNode.getOptRescueNode(), rv, endLabel);
        } else {
            m.addInstr(new ThrowExceptionInstr(exc));
        }

        // Caught exception case -- build rescue body
        m.addInstr(new LabelInstr(caughtLabel));
        Node realBody = skipOverNewlines(m, rescueBodyNode.getBodyNode());
        Operand x = build(realBody, m);
        if (x != U_NIL) { // can be U_NIL if the rescue block has an explicit return
            // Restore "$!"
            Tuple<Label, Variable> t = _rescueBlockStack.peek();
            m.addInstr(new PutGlobalVarInstr("$!", t.b));
            m.addInstr(new CopyInstr(rv, x));
            // Jump to end of rescue block since we've caught and processed the exception
            if (!_ensureBlockStack.empty()) {
                EnsureBlockInfo ebi = _ensureBlockStack.peek();
                m.addInstr(new SetReturnAddressInstr(ebi.returnAddr, endLabel));
                m.addInstr(new JumpInstr(ebi.start));
            } else {
                m.addInstr(new JumpInstr(endLabel));
            }
        }
    }

    public Operand buildRetry(Node node, IRScope s) {
        // JRuby only supports retry when present in rescue blocks!
        // 1.9 doesn't support retry anywhere else.
        s.addInstr(new ThreadPollInstr());

        // Jump back to the innermost rescue block
        // We either find it, or we add code to throw a runtime exception
        if (_rescueBlockStack.empty()) {
            s.addInstr(new ThrowExceptionInstr(IRException.RETURN_LocalJumpError));
        }
        else {
            // Restore $! and jump back to the entry of the rescue block
            Tuple<Label, Variable> t = _rescueBlockStack.peek();
            s.addInstr(new PutGlobalVarInstr("$!", t.b));
            s.addInstr(new JumpInstr(t.a));
        }
        return Nil.NIL;
    }

    public Operand buildReturn(ReturnNode returnNode, IRScope m) {
        Operand retVal = (returnNode.getValueNode() == null) ? Nil.NIL : build(returnNode.getValueNode(), m);

        // Before we return, 
        // - have to go execute all the ensure blocks if there are any.
        //   this code also takes care of resetting "$!"
        // - if we dont have any ensure blocks, we have to clear "$!"
        if (!_ensureBlockStack.empty()) EnsureBlockInfo.emitJumpChain(m, _ensureBlockStack);
        else m.addInstr(new PutGlobalVarInstr("$!", Nil.NIL));

        // If 'm' is a block scope, a return returns from the closest enclosing method.
        // The runtime takes care of lambdas
        if (m instanceof IRClosure) {
            m.addInstr(new ReturnInstr(retVal, ((IRExecutionScope) m).getClosestMethodAncestor()));
        } else if (!((IRMethod)m).isAModuleRootMethod()) {
            m.addInstr(new ReturnInstr(retVal));
        } else {
            // If 'm' is a root method, find the nearest regular non-root (root methods are synthetic, JRuby-generated) 
            // method that 'm' is embedded in.  The return will have to snap out of that method.  If there is no such
            // method, this is a local jump error!
            IRScope sm = m;
            while ((sm != null) && (!(sm instanceof IRMethod) || ((IRMethod)sm).isAModuleRootMethod())) {
                sm = sm.getLexicalParent();
            }

            if (sm == null) m.addInstr(new ThrowExceptionInstr(IRException.RETURN_LocalJumpError));
            else m.addInstr(new ReturnInstr(retVal, (IRMethod)sm));
        }

        // The value of the return itself in the containing expression can never be used because of control-flow reasons.
        // The expression that uses this result can never be executed beyond the return and hence the value itself is just
        // a placeholder operand. 
        return UnexecutableNil.U_NIL;
    }

    public IREvalScript buildEvalRoot(StaticScope staticScope, IRScope containingIRScope, String file, int lineNumber, RootNode rootNode) {
        // Top-level script!
        IREvalScript script = new IREvalScript(containingIRScope, staticScope);

        // Debug info: record file name and line number
        script.addInstr(new FilenameInstr(file));
        script.addInstr(new LineNumberInstr(script, lineNumber));

        // Build IR for the tree and return the result of the expression tree
        Operand rval = build(rootNode.getBodyNode(), script);
        script.addInstr(new ClosureReturnInstr(rval));

        return script;
    }

    public IRScope buildRoot(RootNode rootNode) {
        String file = rootNode.getPosition().getFile();
        StaticScope staticScope = rootNode.getStaticScope();

        // Top-level script!
        IRScript script = new IRScript("__file__", file, staticScope);
        IRClass  rootClass = script.getRootClass();
        IRMethod rootMethod = rootClass.getRootMethod();

        // Debug info: record file name
        rootMethod.addInstr(new FilenameInstr(file));

        // Build IR for the tree and return the result of the expression tree
        rootMethod.addInstr(new ReturnInstr(build(rootNode.getBodyNode(), rootMethod)));

        return script;
    }

    private Variable getSelf(IRScope s) {
        return ((IRExecutionScope)s).getSelf();
    }

    public Operand buildSelf(Node node, IRScope s) {
        return getSelf(s);
    }

    public Operand buildSplat(SplatNode splatNode, IRScope s) {
        // SSS: Since splats can only occur in call argument lists, no need to copyAndReturnValue(...)
        // Verify with Tom / Charlie
        return new Splat(build(splatNode.getValue(), s));
    }

    public Operand buildStr(StrNode strNode, IRScope s) {
        return copyAndReturnValue(s, new StringLiteral(strNode.getValue()));
    }

    private Operand buildSuperInstr(IRScope s, Operand block, Operand[] args) {
        MethAddr maddr;
        if (s instanceof IRClosure) {
            // We dont always know the method name we are going to be invoking if the super occurs in a closure.
            // This is because the super can be part of a block that will be used by 'define_method' to define
            // a new method.  In that case, the method called by super will be determined by the 'name' argument
            // to 'define_method'.
            maddr = MethAddr.UNKNOWN_SUPER_TARGET;
        }
        else {
            // The case where the method is a class root method is an error in the Ruby code.
            // SSS FIXME: Should we insert an exception instruction here since we know this lexically?
            IRMethod method = (IRMethod) s;
            maddr = IRModule.isAModuleRootMethod(method) ? MethAddr.NO_METHOD : new MethAddr(method.getName());
        }
        Variable ret = s.getNewTemporaryVariable();
        s.addInstr(new SuperInstr(ret, getSelf(s), maddr, args, block));
        return ret;
    }

    public Operand buildSuper(SuperNode superNode, IRScope s) {
        List<Operand> args = setupCallArgs(superNode.getArgsNode(), s);
        Operand  block = setupCallClosure(superNode.getIterNode(), s);
        if (block == null) block = ((IRExecutionScope)s).getImplicitBlockArg();
        return buildSuperInstr(s, block, args.toArray(new Operand[args.size()]));
    }

    public Operand buildSValue(SValueNode node, IRScope s) {
        // SSS FIXME: Required? Verify with Tom/Charlie
        return copyAndReturnValue(s, new SValue(build(node.getValue(), s)));
    }

    public Operand buildSymbol(SymbolNode node, IRScope s) {
        // SSS: Since symbols are interned objects, no need to copyAndReturnValue(...)
        return new Symbol(node.getName());
    }    

    public Operand buildToAry(ToAryNode node, IRScope s) {
        // FIXME: Two possibilities
        // 1. Make this a TO_ARY IR instruction to enable optimization 
        // 2. Alternatively make this a regular call which would be subject to inlining
        //    if these utility methods are implemented as ruby ir code.
        Operand array = build(node.getValue(), s);
        return generateRubyInternalsCall(s, RubyInternalsMethod.TO_ARY, true, array, NO_ARGS);
    }

    public Operand buildTrue(Node node, IRScope m) {
        m.addInstr(new ThreadPollInstr());
        return BooleanLiteral.TRUE; 
    }

    public Operand buildUndef(Node node, IRScope m) {
        Operand methName = build(((UndefNode) node).getName(), m);
        return generateRubyInternalsCall(m, RubyInternalsMethod.UNDEF_METHOD, true, methName, NO_ARGS);
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
            s.addInstr(new LabelInstr(loop.loopStartLabel));
            Variable loopResult = loop.loopResult;

            if (isLoopHeadCondition) {
                Operand cv = build(conditionNode, s);
                s.addInstr(new BEQInstr(cv, isWhile ? BooleanLiteral.TRUE : BooleanLiteral.FALSE, loop.iterStartLabel));
                s.addInstr(new CopyInstr((Variable)loopResult, Nil.NIL));
                s.addInstr(new JumpInstr(loop.loopEndLabel));
            }
            s.addInstr(new LabelInstr(loop.iterStartLabel));

            // Looks like while can be treated as an expression!
            // So, capture the result of the body so that it can be returned.
            if (bodyNode != null) {
                Operand v = build(bodyNode, s);
                if (v != U_NIL) {
                    s.addInstr(new CopyInstr((Variable)loopResult, v));
                }
                else {
                    // If the body of the while had an explicit return, the value in 'loopResult' will never be used.
                    // So, we dont need to set it to anything.  We can leave it undefined!
                }
            }

                // SSS FIXME: Is this correctly placed ... at the end of the loop iteration?
            s.addInstr(new ThreadPollInstr());

            s.addInstr(new LabelInstr(loop.iterEndLabel));
            if (isLoopHeadCondition) {
                // Issue a jump back to the head of the while loop
                s.addInstr(new JumpInstr(loop.loopStartLabel));
            }
            else {
                Operand cv = build(conditionNode, s);
                s.addInstr(new BEQInstr(cv, isWhile ? BooleanLiteral.TRUE : BooleanLiteral.FALSE, loop.iterStartLabel));
                s.addInstr(new CopyInstr((Variable)loopResult, Nil.NIL));
            }

            s.addInstr(new LabelInstr(loop.loopEndLabel));
            s.endLoop(loop);

            return loopResult;
        }
    }

    public Operand buildUntil(final UntilNode untilNode, IRExecutionScope s) {
        return buildConditionalLoop(s, untilNode.getConditionNode(), untilNode.getBodyNode(), false, untilNode.evaluateAtStart());
    }

    // SSS FIXME: Got a little lazy?  We could/should define a special instruction ALIAS_GLOBAL_VAR_Instr probably
    // Is this a ruby-internals or a jruby-internals call?
    public Operand buildVAlias(Node node, IRScope s) {
        VAliasNode valiasNode = (VAliasNode) node;
        generateRubyInternalsCall(s, RubyInternalsMethod.GVAR_ALIAS, false, getSelf(s), new Operand[] { new StringLiteral(valiasNode.getNewName()), new StringLiteral(valiasNode.getOldName()) });
        return Nil.NIL;
    }

    public Operand buildVCall(VCallNode node, IRScope s) {
        Variable callResult = s.getNewTemporaryVariable();
        Instr    callInstr  = CallInstr.create(CallType.VARIABLE, callResult, new MethAddr(node.getName()), getSelf(s), NO_ARGS, null);
        s.addInstr(callInstr);
        return callResult;
    }

    public Operand buildWhile(final WhileNode whileNode, IRExecutionScope s) {
        return buildConditionalLoop(s, whileNode.getConditionNode(), whileNode.getBodyNode(), true, whileNode.evaluateAtStart());
    }

    public Operand buildXStr(XStrNode node, IRScope m) {
        return copyAndReturnValue(m, new BacktickString(new StringLiteral(node.getValue())));
    }

/*
    private List<Operand> setupYieldArgs(Node args, IRScope s) {
        List<Operand> argsList = new ArrayList<Operand>();
        if (args != null) {
           // unwrap newline nodes to get their actual type
           args = skipOverNewlines(s, args);
           buildArgs(argsList, args, s);
        }

        return argsList;
    }
*/

    public Operand buildYield(YieldNode node, IRScope s) {
        Variable ret = s.getNewTemporaryVariable();
        s.addInstr(new YieldInstr(ret, ((IRExecutionScope)s).getImplicitBlockArg(), build(node.getArgsNode(), s), node.getExpandArguments()));
        return ret;
    }

    public Operand buildZArray(Node node, IRScope m) {
       return copyAndReturnValue(m, new Array());
    }

    private Operand[] getZSuperArgs(IRScope s) {
        if (s instanceof IRMethod) {
            return ((IRMethod)s).getCallArgs();
        }
        else {
            Operand[] sArgs = s.getNearestMethod().getCallArgs();

            // Update args to make them accessible at a different depth 
            int n = ((IRClosure)s).getNestingDepth();
            for (int i = 0; i < sArgs.length; i++) {
                Operand arg = sArgs[i];
                sArgs[i] = (arg instanceof Splat) ? new Splat(((LocalVariable)((Splat)arg).getArray()).cloneForDepth(n)) : ((LocalVariable)arg).cloneForDepth(n);
            }
            return sArgs;
        }
    }

    public Operand buildZSuper(ZSuperNode zsuperNode, IRScope s) {
        Operand[] args = getZSuperArgs(s);
        Operand block = setupCallClosure(zsuperNode.getIterNode(), s);
        if (block == null) block = ((IRExecutionScope)s).getImplicitBlockArg();
        return buildSuperInstr(s, block, args);
    }
}
