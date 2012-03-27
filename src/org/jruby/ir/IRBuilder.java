package org.jruby.ir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig.CompileMode;
import org.jruby.ast.AliasNode;
import org.jruby.ast.AndNode;
import org.jruby.ast.ArgsCatNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArgsPushNode;
import org.jruby.ast.ArgumentNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.AttrAssignNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BignumNode;
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
import org.jruby.ast.DefsNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.EnsureNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.FCallNode;
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
import org.jruby.ast.LiteralNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.Match2Node;
import org.jruby.ast.Match3Node;
import org.jruby.ast.MatchNode;
import org.jruby.ast.MethodDefNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.Node;
import org.jruby.ast.NodeType;
import org.jruby.ast.NotNode;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.OpAsgnAndNode;
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
import org.jruby.ast.SelfNode;
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
import org.jruby.ast.WhileNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.compiler.NotCompilableException;
import org.jruby.ir.instructions.AliasInstr;
import org.jruby.ir.instructions.AttrAssignInstr;
import org.jruby.ir.instructions.BEQInstr;
import org.jruby.ir.instructions.BNEInstr;
import org.jruby.ir.instructions.BlockGivenInstr;
import org.jruby.ir.instructions.BreakInstr;
import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.instructions.CheckArityInstr;
import org.jruby.ir.instructions.ClassSuperInstr;
import org.jruby.ir.instructions.ClosureReturnInstr;
import org.jruby.ir.instructions.ConstMissingInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.DefineClassInstr;
import org.jruby.ir.instructions.DefineClassMethodInstr;
import org.jruby.ir.instructions.DefineInstanceMethodInstr;
import org.jruby.ir.instructions.DefineMetaClassInstr;
import org.jruby.ir.instructions.DefineModuleInstr;
import org.jruby.ir.instructions.EQQInstr;
import org.jruby.ir.instructions.EnsureRubyArrayInstr;
import org.jruby.ir.instructions.ExceptionRegionEndMarkerInstr;
import org.jruby.ir.instructions.ExceptionRegionStartMarkerInstr;
import org.jruby.ir.instructions.GVarAliasInstr;
import org.jruby.ir.instructions.GetClassVarContainerModuleInstr;
import org.jruby.ir.instructions.GetClassVariableInstr;
import org.jruby.ir.instructions.GetFieldInstr;
import org.jruby.ir.instructions.GetGlobalVariableInstr;
import org.jruby.ir.instructions.InheritanceSearchConstInstr;
import org.jruby.ir.instructions.InstanceOfInstr;
import org.jruby.ir.instructions.InstanceSuperInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpIndirectInstr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.LabelInstr;
import org.jruby.ir.instructions.LexicalSearchConstInstr;
import org.jruby.ir.instructions.LineNumberInstr;
import org.jruby.ir.instructions.Match2Instr;
import org.jruby.ir.instructions.Match3Instr;
import org.jruby.ir.instructions.MatchInstr;
import org.jruby.ir.instructions.NotInstr;
import org.jruby.ir.instructions.PutClassVariableInstr;
import org.jruby.ir.instructions.PutConstInstr;
import org.jruby.ir.instructions.PutFieldInstr;
import org.jruby.ir.instructions.PutGlobalVarInstr;
import org.jruby.ir.instructions.ReceiveClosureInstr;
import org.jruby.ir.instructions.ReceiveExceptionInstr;
import org.jruby.ir.instructions.ReceivePreReqdArgInstr;
import org.jruby.ir.instructions.ReceiveSelfInstr;
import org.jruby.ir.instructions.RecordEndBlockInstr;
import org.jruby.ir.instructions.ReqdArgMultipleAsgnInstr;
import org.jruby.ir.instructions.RescueEQQInstr;
import org.jruby.ir.instructions.RestArgMultipleAsgnInstr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.instructions.ReturnInstr;
import org.jruby.ir.instructions.SetReturnAddressInstr;
import org.jruby.ir.instructions.ThreadPollInstr;
import org.jruby.ir.instructions.ThrowExceptionInstr;
import org.jruby.ir.instructions.ToAryInstr;
import org.jruby.ir.instructions.UndefMethodInstr;
import org.jruby.ir.instructions.UnresolvedSuperInstr;
import org.jruby.ir.instructions.YieldInstr;
import org.jruby.ir.instructions.ZSuperInstr;
import org.jruby.ir.instructions.defined.BackrefIsMatchDataInstr;
import org.jruby.ir.instructions.defined.ClassVarIsDefinedInstr;
import org.jruby.ir.instructions.defined.GetDefinedConstantOrMethodInstr;
import org.jruby.ir.instructions.defined.GetErrorInfoInstr;
import org.jruby.ir.instructions.defined.GlobalIsDefinedInstr;
import org.jruby.ir.instructions.defined.HasInstanceVarInstr;
import org.jruby.ir.instructions.defined.IsMethodBoundInstr;
import org.jruby.ir.instructions.defined.MethodDefinedInstr;
import org.jruby.ir.instructions.defined.MethodIsPublicInstr;
import org.jruby.ir.instructions.defined.RestoreErrorInfoInstr;
import org.jruby.ir.instructions.defined.SetWithinDefinedInstr;
import org.jruby.ir.instructions.defined.SuperMethodBoundInstr;
import org.jruby.ir.instructions.ruby18.ReceiveOptArgInstr;
import org.jruby.ir.instructions.ruby18.ReceiveRestArgInstr;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.AsString;
import org.jruby.ir.operands.Backref;
import org.jruby.ir.operands.BacktickString;
import org.jruby.ir.operands.Bignum;
import org.jruby.ir.operands.CompoundArray;
import org.jruby.ir.operands.CompoundString;
import org.jruby.ir.operands.CurrentModule;
import org.jruby.ir.operands.CurrentScope;
import org.jruby.ir.operands.DynamicSymbol;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Float;
import org.jruby.ir.operands.Hash;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.operands.KeyValuePair;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.NthRef;
import org.jruby.ir.operands.ObjectClass;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Range;
import org.jruby.ir.operands.Regexp;
import org.jruby.ir.operands.SValue;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Symbol;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.UnexecutableNil;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.passes.CFGBuilder;
import org.jruby.ir.passes.IRPrinter;
import org.jruby.ir.passes.InlineTest;
import org.jruby.ir.passes.LinearizeCFG;
import org.jruby.ir.passes.LiveVariableAnalysis;
import org.jruby.ir.passes.opts.DeadCodeElimination;
import org.jruby.ir.passes.opts.LocalOptimizationPass;
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
// 2. Returning null vs manager.getNil()
// ----------------------------
// - We should be returning null from the build methods where it is a normal "error" condition
// - We should be returning manager.getNil() where the actual return value of a build is the ruby nil operand
//   Look in buildIf for an example of this
//
// 3. Temporary variable reuse
// ---------------------------
// I am reusing variables a lot in places in this code.  Should I instead always get a new variable when I need it
// This introduces artificial data dependencies, but fewer variables.  But, if we are going to implement SSA pass
// this is not a big deal.  Think this through!

public class IRBuilder {
    protected static final Operand[] NO_ARGS = new Operand[]{};
    protected static final UnexecutableNil U_NIL = UnexecutableNil.U_NIL;

    private static final Logger LOG = LoggerFactory.getLogger("IRBuilder");
    private static String  rubyVersion = "1.8"; // default is 1.8
    private static boolean inIRGenOnlyMode = false;

    public static boolean inIRGenOnlyMode() {
        return inIRGenOnlyMode;
    }

    public static void setRubyVersion(String rubyVersion) {
        IRBuilder.rubyVersion = rubyVersion;
    }

    public static boolean is1_9() {
        return rubyVersion.equals("1.9");
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
            IRManager manager = new IRManager();
            IRScope scope = createIRBuilder(manager).buildRoot((RootNode) ast);
            long t3 = new Date().getTime();
            if (isDebug) {
                LOG.debug("################## Before local optimization pass ##################");
                scope.runCompilerPass(new IRPrinter());
            }
            scope.runCompilerPass(new org.jruby.ir.passes.opts.LocalOptimizationPass());
            long t4 = new Date().getTime();
            if (isDebug) {
                LOG.debug("################## After local optimization pass ##################");
                scope.runCompilerPass(new IRPrinter());
            }
            scope.runCompilerPass(new CFGBuilder());
            long t5 = new Date().getTime();
//            scope.runCompilerPass(new org.jruby.ir.passes.DominatorTreeBuilder());
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
            // scope.runCompilerPass(new AddLocalVarLoadStoreInstructions());
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
    private static class EnsureBlockInfo {
        Label    regionStart;
        Label    start;
        Label    end;
        Label    dummyRescueBlockLabel;
        Variable returnAddr;
        Variable savedGlobalException;

        // Innermost loop within which this ensure block is nested, if any
        IRLoop   innermostLoop;

        // AST node for any associated rescue node in the case of begin-rescue-ensure-end block
        // Will be null in the case of begin-ensure-end block
        RescueNode matchingRescueNode;   

        public EnsureBlockInfo(IRScope s, RescueNode n, IRLoop l) {
            regionStart = s.getNewLabel();
            start       = s.getNewLabel();
            end         = s.getNewLabel();
            returnAddr  = s.getNewTemporaryVariable();
            dummyRescueBlockLabel = s.getNewLabel();
            savedGlobalException = null;
            innermostLoop = l;
            matchingRescueNode = n;
        }

        // Emit jump chain by walking up the ensure block stack
        // If we have been passed a loop value, then emit values that are nested within that loop
        public static void emitJumpChain(IRScope s, Stack<EnsureBlockInfo> ebStack, IRLoop loop) {
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
                EnsureBlockInfo ebi = ebArray[i];

                // 
                if (ebi.innermostLoop != loop) break;

                Label retLabel = s.getNewLabel();
                if (ebi.savedGlobalException != null) {
                    s.addInstr(new PutGlobalVarInstr("$!", ebi.savedGlobalException));
                }
                s.addInstr(new SetReturnAddressInstr(ebi.returnAddr, retLabel));
                s.addInstr(new JumpInstr(ebi.start));
                s.addInstr(new LabelInstr(retLabel));
            }
        }
    }

    // Stack encoding nested ensure blocks
    private Stack<EnsureBlockInfo> _ensureBlockStack = new Stack<EnsureBlockInfo>();

    private static class RescueBlockInfo {
        RescueNode rescueNode;             // Rescue node for which we are tracking info  
        Label      entryLabel;             // Entry of the rescue block
        Variable   savedExceptionVariable; // Variable that contains the saved $! variable
        IRLoop     innermostLoop;          // Innermost loop within which this ensure block is nested, if any

        public RescueBlockInfo(RescueNode n, Label l, Variable v, IRLoop loop) {
            rescueNode = n;
            entryLabel = l;
            savedExceptionVariable = v;
            innermostLoop = loop;
        }

        public void restoreException(IRScope s, IRLoop currLoop) {
            if (currLoop == innermostLoop) s.addInstr(new PutGlobalVarInstr("$!", savedExceptionVariable));
        }
    }

    // Stack encoding nested rescue blocks -- this just tracks the start label of the blocks
    private Stack<RescueBlockInfo> _rescueBlockStack = new Stack<RescueBlockInfo>();

    private int _lastProcessedLineNum = -1;

    // Since we are processing ASTs, loop bodies are processed in depth-first manner
    // with outer loops encountered before inner loops, and inner loops finished before outer ones.
    //
    // So, we can keep track of loops in a loop stack which  keeps track of loops as they are encountered.
    // This lets us implement next/redo/break/retry easily for the non-closure cases
    private Stack<IRLoop> loopStack = new Stack<IRLoop>();

    public IRLoop getCurrentLoop() {
        return loopStack.isEmpty() ? null : loopStack.peek();
    }
    
    protected IRManager manager;
    
    public IRBuilder(IRManager manager) {
        this.manager = manager;
    }

    public static Node buildAST(boolean isCommandLineScript, String arg) {
        Ruby ruby = Ruby.getGlobalRuntime();
        
        // set to IR mode, since we use different scopes, etc for IR
        ruby.getInstanceConfig().setCompileMode(CompileMode.OFFIR);
        
        // inline script
        if (isCommandLineScript) return ruby.parse(ByteList.create(arg), "-e", null, 0, false);

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

    public static IRBuilder createIRBuilder(IRManager manager) {
        return is1_9() ? new IRBuilder19(manager) : new IRBuilder(manager);
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

    public Operand build(Node node, IRScope s) {
        if (node == null) return null;

        if (s == null) {
            System.out.println("Got a null scope!");
            throw new NotCompilableException("Unknown node encountered in builder: " + node);
        }
        switch (node.getNodeType()) {
            case ALIASNODE: return buildAlias((AliasNode) node, s);
            case ANDNODE: return buildAnd((AndNode) node, s);
            case ARGSCATNODE: return buildArgsCat((ArgsCatNode) node, s);
            case ARGSPUSHNODE: return buildArgsPush((ArgsPushNode) node, s);
            case ARRAYNODE: return buildArray(node, s);
            case ATTRASSIGNNODE: return buildAttrAssign((AttrAssignNode) node, s);
            case BACKREFNODE: return buildBackref((BackRefNode) node, s);
            case BEGINNODE: return buildBegin((BeginNode) node, s);
            case BIGNUMNODE: return buildBignum((BignumNode) node, s);
            case BLOCKNODE: return buildBlock((BlockNode) node, s);
            case BREAKNODE: return buildBreak((BreakNode) node, s);
            case CALLNODE: return buildCall((CallNode) node, s);
            case CASENODE: return buildCase((CaseNode) node, s);
            case CLASSNODE: return buildClass((ClassNode) node, s);
            case CLASSVARNODE: return buildClassVar((ClassVarNode) node, s);
            case CLASSVARASGNNODE: return buildClassVarAsgn((ClassVarAsgnNode) node, s);
            case CLASSVARDECLNODE: return buildClassVarDecl((ClassVarDeclNode) node, s);
            case COLON2NODE: return buildColon2((Colon2Node) node, s);
            case COLON3NODE: return buildColon3((Colon3Node) node, s);
            case CONSTDECLNODE: return buildConstDecl((ConstDeclNode) node, s);
            case CONSTNODE: return searchConst(s, s, ((ConstNode) node).getName());
            case DASGNNODE: return buildDAsgn((DAsgnNode) node, s);
            case DEFINEDNODE: return buildGetDefinitionBase(((DefinedNode) node).getExpressionNode(), s);
            case DEFNNODE: return buildDefn((MethodDefNode) node, s);
            case DEFSNODE: return buildDefs((DefsNode) node, s);
            case DOTNODE: return buildDot((DotNode) node, s);
            case DREGEXPNODE: return buildDRegexp((DRegexpNode) node, s);
            case DSTRNODE: return buildDStr((DStrNode) node, s);
            case DSYMBOLNODE: return buildDSymbol((DSymbolNode) node, s);
            case DVARNODE: return buildDVar((DVarNode) node, s);
            case DXSTRNODE: return buildDXStr((DXStrNode) node, s);
            case ENSURENODE: return buildEnsureNode((EnsureNode) node, s);
            case EVSTRNODE: return buildEvStr((EvStrNode) node, s);
            case FALSENODE: return buildFalse(node, s);
            case FCALLNODE: return buildFCall((FCallNode) node, s);
            case FIXNUMNODE: return buildFixnum((FixnumNode) node, s);
            case FLIPNODE: return buildFlip((FlipNode) node, s);
            case FLOATNODE: return buildFloat((FloatNode) node, s);
            case FORNODE: return buildFor((ForNode) node, s);
            case GLOBALASGNNODE: return buildGlobalAsgn((GlobalAsgnNode) node, s);
            case GLOBALVARNODE: return buildGlobalVar((GlobalVarNode) node, s);
            case HASHNODE: return buildHash((HashNode) node, s);
            case IFNODE: return buildIf((IfNode) node, s);
            case INSTASGNNODE: return buildInstAsgn((InstAsgnNode) node, s);
            case INSTVARNODE: return buildInstVar((InstVarNode) node, s);
            case ITERNODE: return buildIter((IterNode) node, s);
            case LITERALNODE: return buildLiteral((LiteralNode) node, s);
            case LOCALASGNNODE: return buildLocalAsgn((LocalAsgnNode) node, s);
            case LOCALVARNODE: return buildLocalVar((LocalVarNode) node, s);
            case MATCH2NODE: return buildMatch2((Match2Node) node, s);
            case MATCH3NODE: return buildMatch3((Match3Node) node, s);
            case MATCHNODE: return buildMatch((MatchNode) node, s);
            case MODULENODE: return buildModule((ModuleNode) node, s);
            case MULTIPLEASGNNODE: return buildMultipleAsgn((MultipleAsgnNode) node, s); // Only for 1.8
            case NEWLINENODE: return buildNewline((NewlineNode) node, s);
            case NEXTNODE: return buildNext((NextNode) node, s);
            case NTHREFNODE: return buildNthRef((NthRefNode) node, s);
            case NILNODE: return buildNil(node, s);
            case NOTNODE: return buildNot((NotNode) node, s);
            case OPASGNANDNODE: return buildOpAsgnAnd((OpAsgnAndNode) node, s);
            case OPASGNNODE: return buildOpAsgn((OpAsgnNode) node, s);
            case OPASGNORNODE: return buildOpAsgnOr((OpAsgnOrNode) node, s);
            case OPELEMENTASGNNODE: return buildOpElementAsgn((OpElementAsgnNode) node, s);
            case ORNODE: return buildOr((OrNode) node, s);
            case PREEXENODE: return buildPreExe((PreExeNode) node, s);
            case POSTEXENODE: return buildPostExe((PostExeNode) node, s);
            case REDONODE: return buildRedo(node, s);
            case REGEXPNODE: return buildRegexp((RegexpNode) node, s);
            case RESCUEBODYNODE:
                throw new NotCompilableException("rescue body is handled by rescue compilation at: " + node.getPosition());
            case RESCUENODE: return buildRescue((RescueNode) node, s);
            case RETRYNODE: return buildRetry(node, s);
            case RETURNNODE: return buildReturn((ReturnNode) node, s);
            case ROOTNODE:
                throw new NotCompilableException("Use buildRoot(); Root node at: " + node.getPosition());
            case SCLASSNODE: return buildSClass((SClassNode) node, s);
            case SELFNODE: return buildSelf((SelfNode) node, s);
            case SPLATNODE: return buildSplat((SplatNode) node, s);
            case STRNODE: return buildStr((StrNode) node, s);
            case SUPERNODE: return buildSuper((SuperNode) node, s);
            case SVALUENODE: return buildSValue((SValueNode) node, s);
            case SYMBOLNODE: return buildSymbol((SymbolNode) node, s);
            case TOARYNODE: return buildToAry((ToAryNode) node, s);
            case TRUENODE: return buildTrue(node, s);
            case UNDEFNODE: return buildUndef(node, s);
            case UNTILNODE: return buildUntil((UntilNode) node, s);
            case VALIASNODE: return buildVAlias(node, s);
            case VCALLNODE: return buildVCall((VCallNode) node, s);
            case WHILENODE: return buildWhile((WhileNode) node, s);
            case WHENNODE: assert false : "When nodes are handled by case node compilation."; return null;
            case XSTRNODE: return buildXStr((XStrNode) node, s);
            case YIELDNODE: return buildYield((YieldNode) node, s);
            case ZARRAYNODE: return buildZArray(node, s);
            case ZSUPERNODE: return buildZSuper((ZSuperNode) node, s);
            default: return buildVersionSpecificNodes(node, s);
        }
    }

    protected Operand buildVersionSpecificNodes(Node node, IRScope s) {
        throw new NotCompilableException("Unknown node encountered in builder: " + node.getClass());
    }

    protected Variable getSelf(IRScope s) {
        return s.getSelf();
    }

    protected Variable copyAndReturnValue(IRScope s, Operand val) {
        Variable v = s.getNewTemporaryVariable();
        s.addInstr(new CopyInstr(v, val));
        return v;
    }

    protected Variable getValueInTemporaryVariable(IRScope s, Operand val) {
        if (val != null && val instanceof TemporaryVariable) return (Variable) val;

        return copyAndReturnValue(s, val);
    }

    // Return the last argument in the list -- AttrAssign needs it
    protected Operand buildCallArgs(List<Operand> argsList, Node args, IRScope s) {
        // unwrap newline nodes to get their actual type
        args = skipOverNewlines(s, args);
        switch (args.getNodeType()) {
            case ARGSCATNODE: {
                CompoundArray a = (CompoundArray)build(args, s);
                argsList.add(new Splat(a));
                return a.getAppendedArg();
            }
            case ARGSPUSHNODE:  {
                ArgsPushNode ap = (ArgsPushNode)args;
                Operand v1 = build(ap.getFirstNode(), s);
                Operand v2 = build(ap.getSecondNode(), s);
                argsList.add(new Splat(new CompoundArray(v1, v2, true)));
                return v2;
            }
            case ARRAYNODE: {
                ArrayNode arrayNode = (ArrayNode)args;
                if (arrayNode.isLightweight()) {
                    List<Node> children = arrayNode.childNodes();
                    if (children.size() == 1) {
                        // skipOverNewlines is required because the parser inserts a NewLineNode in between!
                        Node child = skipOverNewlines(s, children.get(0));
                        if (child instanceof SplatNode) {
                            // SSS: If the only child is a splat, the splat is supposed to get through
                            // as an array without being expanded into the call arg list.
                            //
                            // The AST for the foo([*1]) is: ArrayNode(Splat19Node(..))
                            // The AST for the foo(*1) is: Splat19Node(..)
                            //
                            // Since a lone splat in call args is always expanded, we convert the splat
                            // into a compound array: *n --> args-cat([], *n)
                            SplatNode splat = (SplatNode)child;
                            Variable splatArray = getValueInTemporaryVariable(s, build(splat.getValue(), s));
                            argsList.add(new CompoundArray(new Array(), splatArray));
                            return new Splat(splatArray);
                        } else {
                            Operand childOperand = build(child, s);
                            argsList.add(childOperand);
                            return childOperand;
                        }
                    } else {
                        // explode array, it's an internal "args" array
                        for (Node n: children) {
                            argsList.add(build(n, s));
                        }
                    }
                } else {
                    // use array as-is, it's a literal array
                    argsList.add(build(arrayNode, s));
                }
                break;
            }
            default: {
                argsList.add(build(args, s));
                break;
            }
        }

        return argsList.isEmpty() ? manager.getNil() : argsList.get(argsList.size() - 1);
    }

    public List<Operand> setupCallArgs(Node args, IRScope s) {
        List<Operand> argsList = new ArrayList<Operand>();
        if (args != null) buildCallArgs(argsList, args, s);
        return argsList;
    }

    public void buildVersionSpecificAssignment(Node node, IRScope s, Variable v) {
        switch (node.getNodeType()) {
        case MULTIPLEASGNNODE: {
            Operand valuesArg;
            MultipleAsgnNode childNode = (MultipleAsgnNode) node;
            if (childNode.getHeadNode() != null && ((ListNode)childNode.getHeadNode()).childNodes().size() > 0) {
                // Invoke to_ary on the operand only if it is not an array already
                Variable result = s.getNewTemporaryVariable();
                s.addInstr(new ToAryInstr(result, v, manager.getTrue()));
                valuesArg = result;
            } else {
                s.addInstr(new EnsureRubyArrayInstr(v, v));
                valuesArg = v;
            }
            buildMultipleAsgnAssignment(childNode, s, null, valuesArg);
            break;
        }
        default: 
            throw new NotCompilableException("Can't build assignment node: " + node);
        }
    }

    // This method is called to build assignments for a multiple-assignment instruction
    public void buildAssignment(Node node, IRScope s, Variable rhsVal) {
        switch (node.getNodeType()) {
            case ATTRASSIGNNODE: 
                buildAttrAssignAssignment(node, s, rhsVal);
                break;
            case CLASSVARASGNNODE:
                s.addInstr(new PutClassVariableInstr(classVarDefinitionContainer(s, true), ((ClassVarAsgnNode)node).getName(), rhsVal));
                break;
            case CLASSVARDECLNODE:
                s.addInstr(new PutClassVariableInstr(classVarDefinitionContainer(s, false), ((ClassVarDeclNode)node).getName(), rhsVal));
                break;
            case CONSTDECLNODE:
                buildConstDeclAssignment((ConstDeclNode) node, s, rhsVal);
                break;
            case DASGNNODE: {
                DAsgnNode variable = (DAsgnNode) node;
                int depth = variable.getDepth();
                s.addInstr(new CopyInstr(s.getLocalVariable(variable.getName(), depth), rhsVal));
                break;
            }
            case GLOBALASGNNODE:
                s.addInstr(new PutGlobalVarInstr(((GlobalAsgnNode)node).getName(), rhsVal));
                break;
            case INSTASGNNODE:
                // NOTE: if 's' happens to the a class, this is effectively an assignment of a class instance variable
                s.addInstr(new PutFieldInstr(getSelf(s), ((InstAsgnNode)node).getName(), rhsVal));
                break;
            case LOCALASGNNODE: {
                LocalAsgnNode localVariable = (LocalAsgnNode) node;
                int depth = localVariable.getDepth();
                s.addInstr(new CopyInstr(s.getLocalVariable(localVariable.getName(), depth), rhsVal));
                break;
            }
            case ZEROARGNODE:
                throw new NotCompilableException("Shouldn't get here; zeroarg does not do assignment: " + node);
            default:
                buildVersionSpecificAssignment(node, s, rhsVal);
        }
    }

    protected LocalVariable getBlockArgVariable(IRScope cl, String name, int depth) {
        return cl.getLocalVariable(name, depth);
    }

    protected void receiveBlockArg(IRScope s, Variable v, Operand argsArray, int argIndex, boolean isClosureArg, boolean isSplat) {
        if (argsArray != null) {
            // We are in a nested receive situation -- when we are not at the root of a masgn tree
            // Ex: We are trying to receive (b,c) in this example: "|a, (b,c), d| = ..."
            if (isSplat) s.addInstr(new RestArgMultipleAsgnInstr(v, argsArray, argIndex));
            else s.addInstr(new ReqdArgMultipleAsgnInstr(v, argsArray, argIndex));
        } else {
            // argsArray can be null when the first node in the args-node-ast is a multiple-assignment
            // For example, for-nodes
            s.addInstr(isClosureArg ? new ReceiveClosureInstr(v) : (isSplat ? new ReceiveRestArgInstr(v, argIndex) : new ReceivePreReqdArgInstr(v, argIndex)));
        }
    }

    public void buildVersionSpecificBlockArgsAssignment(Node node, IRScope s, Operand argsArray, int argIndex, boolean isMasgnRoot, boolean isClosureArg, boolean isSplat) {
        switch (node.getNodeType()) {
            case MULTIPLEASGNNODE: {
                Variable oldArgs = null;
                MultipleAsgnNode childNode = (MultipleAsgnNode) node;
                if (!isMasgnRoot) {
                    // Vars used to receive args should always be local-variables because
                    // these arg values may need to be accessed by some zsuper instruction.
                    // During interpretation, only local-vars are accessible (at least right now)
                    // outside the scope they are defined in.
                    Variable v = s.getLocalVariable("%_masgn_arg_" + argIndex, 0);
                    receiveBlockArg(s, v, argsArray, argIndex, isClosureArg, isSplat);
                    boolean runToAry = childNode.getHeadNode() != null && (((ListNode)childNode.getHeadNode()).childNodes().size() > 0);
                    if (runToAry) {
                        s.addInstr(new ToAryInstr(v, v, manager.getFalse()));
                    } else {
                        s.addInstr(new EnsureRubyArrayInstr(v, v));
                    }
                    argsArray = v;
                    // SSS FIXME: Are we guaranteed that splats dont head to multiple-assignment nodes!  i.e. |*(a,b)|?
                }
                // Build
                buildMultipleAsgnAssignment(childNode, s, argsArray, null);
                break;
            }
            default: throw new NotCompilableException("Can't build assignment node: " + node);
        }
    }

    // This method is called to build arguments for a block!
    public void buildBlockArgsAssignment(Node node, IRScope s, Operand argsArray, int argIndex, boolean isMasgnRoot, boolean isClosureArg, boolean isSplat) {
        Variable v;
        switch (node.getNodeType()) {
            case ATTRASSIGNNODE: 
                v = s.getNewTemporaryVariable();
                receiveBlockArg(s, v, argsArray, argIndex, isClosureArg, isSplat);
                buildAttrAssignAssignment(node, s, v);
                break;
            case DASGNNODE: {
                DAsgnNode dynamicAsgn = (DAsgnNode) node;
                v = getBlockArgVariable((IRClosure)s, dynamicAsgn.getName(), dynamicAsgn.getDepth());
                receiveBlockArg(s, v, argsArray, argIndex, isClosureArg, isSplat);
                break;
            }
            case CLASSVARASGNNODE:
                v = s.getNewTemporaryVariable();
                receiveBlockArg(s, v, argsArray, argIndex, isClosureArg, isSplat);
                s.addInstr(new PutClassVariableInstr(classVarDefinitionContainer(s, true), ((ClassVarAsgnNode)node).getName(), v));
                break;
            case CLASSVARDECLNODE:
                v = s.getNewTemporaryVariable();
                receiveBlockArg(s, v, argsArray, argIndex, isClosureArg, isSplat);
                s.addInstr(new PutClassVariableInstr(classVarDefinitionContainer(s, false), ((ClassVarDeclNode)node).getName(), v));
                break;
            case CONSTDECLNODE:
                v = s.getNewTemporaryVariable();
                receiveBlockArg(s, v, argsArray, argIndex, isClosureArg, isSplat);
                buildConstDeclAssignment((ConstDeclNode) node, s, v);
                break;
            case GLOBALASGNNODE:
                v = s.getNewTemporaryVariable();
                receiveBlockArg(s, v, argsArray, argIndex, isClosureArg, isSplat);
                s.addInstr(new PutGlobalVarInstr(((GlobalAsgnNode)node).getName(), v));
                break;
            case INSTASGNNODE:
                v = s.getNewTemporaryVariable();
                receiveBlockArg(s, v, argsArray, argIndex, isClosureArg, isSplat);
                // NOTE: if 's' happens to the a class, this is effectively an assignment of a class instance variable
                s.addInstr(new PutFieldInstr(getSelf(s), ((InstAsgnNode)node).getName(), v));
                break;
            case LOCALASGNNODE: {
                LocalAsgnNode localVariable = (LocalAsgnNode) node;
                int depth = localVariable.getDepth();
                v = getBlockArgVariable((IRClosure)s, localVariable.getName(), depth);
                receiveBlockArg(s, v, argsArray, argIndex, isClosureArg, isSplat);
                break;
            }
            case ZEROARGNODE:
                throw new NotCompilableException("Shouldn't get here; zeroarg does not do assignment: " + node);
            default:
                buildVersionSpecificBlockArgsAssignment(node, s, argsArray, argIndex, isMasgnRoot, isClosureArg, isSplat);
        }
    }

    public Operand buildAlias(final AliasNode alias, IRScope s) {
        Operand newName = build(alias.getNewName(), s);
        Operand oldName = build(alias.getOldName(), s);
        s.addInstr(new AliasInstr(getSelf(s), newName, oldName));
        
        return manager.getNil();
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
    public Operand buildAnd(final AndNode andNode, IRScope s) {
        if (andNode.getFirstNode().getNodeType().alwaysTrue()) {
            // build first node (and ignore its result) and then second node
            build(andNode.getFirstNode(), s);
            return build(andNode.getSecondNode(), s);
        } else if (andNode.getFirstNode().getNodeType().alwaysFalse()) {
            // build first node only and return its value
            return build(andNode.getFirstNode(), s);
        } else {
            Label    l   = s.getNewLabel();
            Operand  v1  = build(andNode.getFirstNode(), s);
            Variable ret = getValueInTemporaryVariable(s, v1);
            s.addInstr(BEQInstr.create(v1, manager.getFalse(), l));
            Operand  v2  = build(andNode.getSecondNode(), s);
            s.addInstr(new CopyInstr(ret, v2));
            s.addInstr(new LabelInstr(l));
            return ret;
        }
    }

    public Operand buildArray(Node node, IRScope s) {
        List<Operand> elts = new ArrayList<Operand>();
        for (Node e: node.childNodes())
            elts.add(build(e, s));

        return copyAndReturnValue(s, new Array(elts));
    }

    public Operand buildArgsCat(final ArgsCatNode argsCatNode, IRScope s) {
        Operand v1 = build(argsCatNode.getFirstNode(), s);
        Operand v2 = build(argsCatNode.getSecondNode(), s);
        return new CompoundArray(v1, v2);
    }

    public Operand buildArgsPush(final ArgsPushNode node, IRScope s) {
        throw new NotCompilableException("ArgsPush should never be encountered bare in 1.8" + node);
    }

    private Operand buildAttrAssign(final AttrAssignNode attrAssignNode, IRScope s) {
        Operand obj = build(attrAssignNode.getReceiverNode(), s);
        List<Operand> args = new ArrayList<Operand>();
        Node argsNode = attrAssignNode.getArgsNode();
        Operand lastArg = (argsNode == null) ? manager.getNil() : buildCallArgs(args, argsNode, s);
        s.addInstr(new AttrAssignInstr(obj, new MethAddr(attrAssignNode.getName()), args.toArray(new Operand[args.size()])));
        return lastArg;
    }

    public Operand buildAttrAssignAssignment(Node node, IRScope s, Operand value) {
        final AttrAssignNode attrAssignNode = (AttrAssignNode) node;
        Operand obj = build(attrAssignNode.getReceiverNode(), s);
        List<Operand> args = setupCallArgs(attrAssignNode.getArgsNode(), s);
        args.add(value);
        s.addInstr(new AttrAssignInstr(obj, new MethAddr(attrAssignNode.getName()), args.toArray(new Operand[args.size()])));
        return value;
    }

    public Operand buildBackref(BackRefNode node, IRScope s) {
        // SSS FIXME: Required? Verify with Tom/Charlie
        return copyAndReturnValue(s, new Backref(node.getType()));
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

    public Operand buildBreak(BreakNode breakNode, IRScope s) {
        IRLoop currLoop = getCurrentLoop();

        Operand rv = build(breakNode.getValueNode(), s);
        // If we have ensure blocks, have to run those first!
        if (!_ensureBlockStack.empty()) EnsureBlockInfo.emitJumpChain(s, _ensureBlockStack, currLoop);
        else if (!_rescueBlockStack.empty()) _rescueBlockStack.peek().restoreException(s, currLoop);

        if (currLoop != null) {
            s.addInstr(new CopyInstr(currLoop.loopResult, rv));
            s.addInstr(new JumpInstr(currLoop.loopEndLabel));
        } else {
            if (s instanceof IRClosure) {
                // This lexical scope value is only used (and valid) in regular block contexts.
                // If this instruction is executed in a Proc or Lambda context, the lexical scope value is useless.
                s.addInstr(new BreakInstr(rv, s.getLexicalParent()));
            } else {
                // SSS FIXME: If we are not in a closure or a loop, the break instruction will throw a runtime exception
                // Since we know this right now, should we build an exception instruction here?
                s.addInstr(new BreakInstr(rv, null));
            }
        }

        // Once the break instruction executes, control exits this scope
        return UnexecutableNil.U_NIL;
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
        Instr         callInstr    = CallInstr.create(callResult, new MethAddr(callNode.getName()), receiver, args.toArray(new Operand[args.size()]), block);
        s.addInstr(callInstr);
        return callResult;
    }

    public Operand buildCase(CaseNode caseNode, IRScope s) {
        // get the incoming case value
        Operand value = build(caseNode.getCaseNode(), s);

        // This is for handling case statements without a value (see example below)
        //   case 
        //     when true <blah>
        //     when false <blah>
        //   end
        if (value == null) value = UndefinedValue.UNDEFINED;

        Label     endLabel  = s.getNewLabel();
        boolean   hasElse   = (caseNode.getElseNode() != null);
        Label     elseLabel = s.getNewLabel();
        Variable  result    = s.getNewTemporaryVariable();

        List<Label> labels = new ArrayList<Label>();
        Map<Label, Node> bodies = new HashMap<Label, Node>();

        // build each "when"
        for (Node aCase : caseNode.getCases().childNodes()) {
            WhenNode whenNode = (WhenNode)aCase;
            Label bodyLabel = s.getNewLabel();

            Variable eqqResult = s.getNewTemporaryVariable();
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
                    v1 = build(whenNode.getExpressionNodes(), s);
                    v2 = manager.getTrue();
                } else {
                    v1 = value;
                    v2 = build(whenNode.getExpressionNodes(), s);
                }
            } else {
                s.addInstr(new EQQInstr(eqqResult, build(whenNode.getExpressionNodes(), s), value));
                v1 = eqqResult;
                v2 = manager.getTrue();
            }
            s.addInstr(BEQInstr.create(v1, v2, bodyLabel));

            // SSS FIXME: This doesn't preserve original order of when clauses.  We could consider
            // preserving the order (or maybe not, since we would have to sort the constants first
            // in any case) for outputing jump tables in certain situations.
            //
            // add body to map for emitting later
            bodies.put(bodyLabel, whenNode.getBodyNode());
        }

        // Jump to else in case nothing matches!
        s.addInstr(new JumpInstr(elseLabel));

        // build "else" if it exists
        if (hasElse) {
            labels.add(elseLabel);
            bodies.put(elseLabel, caseNode.getElseNode());
        }

        // now emit bodies while preserving when clauses order
        for (Label whenLabel: labels) {
            s.addInstr(new LabelInstr(whenLabel));
            Operand bodyValue = build(bodies.get(whenLabel), s);
            // bodyValue can be null if the body ends with a return!
            if (bodyValue != null) {
               // SSS FIXME: Do local optimization of break results (followed by a copy & jump) to short-circuit the jump right away
               // rather than wait to do it during an optimization pass when a dead jump needs to be removed.  For this, you have
               // to look at what the last generated instruction was.
               Label tgt = endLabel;
               s.addInstr(new CopyInstr(result, bodyValue));
               s.addInstr(new JumpInstr(tgt));
            }
        }

        if (!hasElse) {
            s.addInstr(new LabelInstr(elseLabel));
            s.addInstr(new CopyInstr(result, manager.getNil()));
            s.addInstr(new JumpInstr(endLabel));
        }

        // close it out
        s.addInstr(new LabelInstr(endLabel));

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

        IRClassBody c = new IRClassBody(manager, s, className, classNode.getPosition().getLine(), classNode.getScope());
        Variable ret = s.getNewTemporaryVariable();
        s.addInstr(new DefineClassInstr(ret, c, container, superClass));

        c.addInstr(new ReceiveSelfInstr(c.getSelf()));
        // Set %current_scope = <c>
        // Set %current_module = module<c>
        c.addInstr(new CopyInstr(c.getCurrentScopeVariable(), new CurrentScope(c)));
        c.addInstr(new CopyInstr(c.getCurrentModuleVariable(), new CurrentModule(c)));
        // Create a new nested builder to ensure this gets its own IR builder state 
        Operand rv = createIRBuilder(manager).build(classNode.getBodyNode(), c);
        if (rv != null) c.addInstr(new ReturnInstr(rv));

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
        // Here, the class << self declaration is in Foo's body.
        // Foo is the class in whose context this is being defined.
        Operand receiver = build(sclassNode.getReceiverNode(), s);

        // Create a dummy meta class and record it as being lexically defined in scope s
        IRModuleBody mc = new IRMetaClassBody(manager, s, manager.getMetaClassName(), sclassNode.getPosition().getLine(), sclassNode.getScope());
        Variable ret = s.getNewTemporaryVariable();
        s.addInstr(new DefineMetaClassInstr(ret, receiver, mc));

        mc.addInstr(new ReceiveSelfInstr(mc.getSelf()));
        // Set %current_scope = <current-scope>
        // Set %current_module = <current-module>
        mc.addInstr(new ReceiveClosureInstr(mc.getImplicitBlockArg()));
        mc.addInstr(new CopyInstr(mc.getCurrentScopeVariable(), new CurrentScope(mc)));
        mc.addInstr(new CopyInstr(mc.getCurrentModuleVariable(), new CurrentModule(mc)));
        // Create a new nested builder to ensure this gets its own IR builder state 
        Operand rv = createIRBuilder(manager).build(sclassNode.getBodyNode(), mc);
        if (rv != null) mc.addInstr(new ReturnInstr(rv));

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
         * Find the nearest class/module scope (within which 's' is embedded) that can
         * hold class variables and return its ModuleBody.  Skip module bodies since
         * they can never contain class variables!
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
        while (current != null && !(current instanceof IREvalScript) &&
                !(current.isModuleBody() && (current.getLexicalParent() == null || !current.getLexicalParent().isModuleBody()))) {
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

    private Operand findContainerModule(IRScope s) {
        IRScope nearestModuleBody = s.getNearestModuleReferencingScope();
        return (nearestModuleBody == null) ? s.getCurrentModuleVariable() : new CurrentModule(nearestModuleBody);
    }

    private Operand startingSearchScope(IRScope s) {
        IRScope nearestModuleBody = s.getNearestModuleReferencingScope();
        return nearestModuleBody == null ? s.getCurrentScopeVariable() : new CurrentScope(nearestModuleBody);
    }

    public Operand buildConstDeclAssignment(ConstDeclNode constDeclNode, IRScope s, Operand val) {
        Node constNode = constDeclNode.getConstNode();

        if (constNode == null) {
            s.addInstr(new PutConstInstr(findContainerModule(s), constDeclNode.getName(), val));
        } else if (constNode.getNodeType() == NodeType.COLON2NODE) {
            Operand module = build(((Colon2Node) constNode).getLeftNode(), s);
            s.addInstr(new PutConstInstr(module, constDeclNode.getName(), val));
        } else { // colon3, assign in Object
            CurrentModule object = new CurrentModule(manager.getObject());            
            s.addInstr(new PutConstInstr(object, constDeclNode.getName(), val));            
        }

        return val;
    }

    private Operand searchConst(IRScope s, IRScope startingScope, String name) {
        Variable v = s.getNewTemporaryVariable();
        Label foundLabel = s.getNewLabel();
        Operand startingSearchScope = startingSearchScope(startingScope);
        s.addInstr(new LexicalSearchConstInstr(v, startingSearchScope, name));
        s.addInstr(BNEInstr.create(v, UndefinedValue.UNDEFINED, foundLabel));
        // SSS FIXME: should this be the current-module-var or can we resolve
        // this to some statically-known value instead?
        Operand currentModule = s.getCurrentModuleVariable(); 
        s.addInstr(new InheritanceSearchConstInstr(v, currentModule, name));
        s.addInstr(BNEInstr.create(v, UndefinedValue.UNDEFINED, foundLabel));
        s.addInstr(new ConstMissingInstr(v, currentModule, name));
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
            Variable constVal = s.getNewTemporaryVariable();
            Label foundLabel = s.getNewLabel();
            s.addInstr(new InheritanceSearchConstInstr(constVal, module, name));
            s.addInstr(BNEInstr.create(constVal, UndefinedValue.UNDEFINED, foundLabel));
            s.addInstr(new ConstMissingInstr(constVal, module, name));
            s.addInstr(new LabelInstr(foundLabel));
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
        } else { 
            throw new NotCompilableException("Not compilable: " + iVisited);
        }
    }

    public Operand buildColon3(Colon3Node node, IRScope s) {
        return searchConst(s, manager.getObject(), node.getName());
    }

    interface CodeBlock {
        public Operand run(Object[] args);
    }

    private Variable protectCodeWithEnsure(IRScope s, CodeBlock protectedCode, Object[] protectedCodeArgs, CodeBlock ensureCode, Object[] ensureCodeArgs) {
        // This effectively mimics a begin-ensure-end code block
        // Except this silently swallows all exceptions raised by the protected code

        Variable ret = s.getNewTemporaryVariable();

        // Push a new ensure block info node onto the stack of ensure block
        EnsureBlockInfo ebi = new EnsureBlockInfo(s, null, getCurrentLoop());
        _ensureBlockStack.push(ebi);
        Label rBeginLabel = ebi.regionStart;
        Label rEndLabel   = ebi.end;

        // Protected region code
        s.addInstr(new LabelInstr(rBeginLabel));
        s.addInstr(new ExceptionRegionStartMarkerInstr(rBeginLabel, rEndLabel, ebi.dummyRescueBlockLabel, ebi.dummyRescueBlockLabel));
        Operand v1 = protectedCode.run(protectedCodeArgs); // YIELD: Run the protected code block
        s.addInstr(new CopyInstr(ret, v1));
        s.addInstr(new JumpInstr(ebi.start));
        s.addInstr(new ExceptionRegionEndMarkerInstr());

        // Rescue block code
        // SSS FIXME: How do we get this to catch all exceptions, not just Ruby exceptions?
        s.addInstr(new LabelInstr(ebi.dummyRescueBlockLabel));
        s.addInstr(new CopyInstr(ret, manager.getNil()));

        _ensureBlockStack.pop();

        // Ensure block code -- this should not throw exceptions
        s.addInstr(new LabelInstr(ebi.start));
        ensureCode.run(ensureCodeArgs); // YIELD: Run the ensure code block

        // End
        s.addInstr(new LabelInstr(rEndLabel));

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
        m.addInstr(BEQInstr.create(eqqResult, manager.getFalse(), uncaughtLabel));
        Object v2 = rescueBlock.run(rescueBlockArgs); // YIELD: Run the protected code block
        if (v2 != null) m.addInstr(new CopyInstr(rv, manager.getNil()));
        m.addInstr(new JumpInstr(rEndLabel));
        m.addInstr(new LabelInstr(uncaughtLabel));
        m.addInstr(new ThrowExceptionInstr(exc));

        // End
        m.addInstr(new LabelInstr(rEndLabel));

        return rv;
    }

    protected Operand buildGenericGetDefinitionIR(Node node, IRScope s) {
        s.addInstr(new SetWithinDefinedInstr(manager.getTrue()));

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
                m.addInstr(new SetWithinDefinedInstr(manager.getFalse()));
                return manager.getNil();
            }
        };

        return protectCodeWithEnsure(s, protectedCode, new Object[] {node, s}, ensureCode, new Object[] {s});
    }

    protected Operand buildVersionSpecificGetDefinitionIR(Node node, IRScope s) {
        switch (node.getNodeType()) {
            case DVARNODE:
            case BACKREFNODE: 
                return buildGetDefinition(node, s);
            default: 
                return buildGenericGetDefinitionIR(node, s);
        }
    }

    public Operand buildGetDefinitionBase(Node node, IRScope s) {
        node = skipOverNewlines(s, node);
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
        case FALSENODE:
        case TRUENODE:
        case LOCALVARNODE:
        case INSTVARNODE:
        case SELFNODE:
        case VCALLNODE:
        case YIELDNODE:
        case GLOBALVARNODE:
        case CONSTNODE:
        case FCALLNODE:
        case CLASSVARNODE:
            // these are all "simple" cases that don't require the heavier defined logic
            return buildGetDefinition(node, s);

        default: 
            return buildVersionSpecificGetDefinitionIR(node, s);
        }
    }

    protected Variable buildDefnCheckIfThenPaths(IRScope s, Label undefLabel, Operand defVal) {
        Label defLabel = s.getNewLabel();
        Variable tmpVar = getValueInTemporaryVariable(s, defVal);
        s.addInstr(new JumpInstr(defLabel));
        s.addInstr(new LabelInstr(undefLabel));
        s.addInstr(new CopyInstr(tmpVar, manager.getNil()));
        s.addInstr(new LabelInstr(defLabel));
        return tmpVar;
    }
    
    protected Variable buildDefinitionCheck(IRScope s, ResultInstr definedInstr, String definedReturnValue) {
        Label undefLabel = s.getNewLabel();
        s.addInstr((Instr) definedInstr);
        s.addInstr(BEQInstr.create(definedInstr.getResult(), manager.getFalse(), undefLabel));
        return buildDefnCheckIfThenPaths(s, undefLabel, new StringLiteral(definedReturnValue));        
    }

    public Operand buildGetArgumentDefinition(final Node node, IRScope s, String type) {
        if (node == null) return new StringLiteral(type);

        Operand rv = new StringLiteral(type);
        boolean failPathReqd = false;
        Label failLabel = s.getNewLabel();
        if (node instanceof ArrayNode) {
            for (int i = 0; i < ((ArrayNode) node).size(); i++) {
                Node iterNode = ((ArrayNode) node).get(i);
                Operand def = buildGetDefinition(iterNode, s);
                if (def == manager.getNil()) { // Optimization!
                    rv = manager.getNil();
                    break;
                } else if (!def.hasKnownValue()) { // Optimization!
                    failPathReqd = true;
                    s.addInstr(BEQInstr.create(def, manager.getNil(), failLabel));
                }
            }
        } else {
            Operand def = buildGetDefinition(node, s);
            if (def == manager.getNil()) { // Optimization!
                rv = manager.getNil();
            } else if (!def.hasKnownValue()) { // Optimization!
                failPathReqd = true;
                s.addInstr(BEQInstr.create(def, manager.getNil(), failLabel));
            }
        }

        // Optimization!
        return failPathReqd ? buildDefnCheckIfThenPaths(s, failLabel, rv) : rv;

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
            case CONSTNODE: {
                Label defLabel = s.getNewLabel();
                Label doneLabel = s.getNewLabel();
                Variable tmpVar  = s.getNewTemporaryVariable();
                String constName = ((ConstNode) node).getName();
                s.addInstr(new LexicalSearchConstInstr(tmpVar, startingSearchScope(s), constName));
                s.addInstr(BNEInstr.create(tmpVar, UndefinedValue.UNDEFINED, defLabel));
                s.addInstr(new InheritanceSearchConstInstr(tmpVar, s.getCurrentModuleVariable(), constName)); // SSS FIXME: should this be the current-module var or something else?
                s.addInstr(BNEInstr.create(tmpVar, UndefinedValue.UNDEFINED, defLabel));
                s.addInstr(new CopyInstr(tmpVar, manager.getNil()));
                s.addInstr(new JumpInstr(doneLabel));
                s.addInstr(new LabelInstr(defLabel));
                s.addInstr(new CopyInstr(tmpVar, new StringLiteral("constant")));
                s.addInstr(new LabelInstr(doneLabel));
                return tmpVar;
            }
            case GLOBALVARNODE:
                return buildDefinitionCheck(s, new GlobalIsDefinedInstr(s.getNewTemporaryVariable(), new StringLiteral(((GlobalVarNode) node).getName())), "global-variable");
            case INSTVARNODE:
                return buildDefinitionCheck(s, new HasInstanceVarInstr(s.getNewTemporaryVariable(), getSelf(s), new StringLiteral(((InstVarNode) node).getName())), "instance-variable");
            case YIELDNODE:
                return buildDefinitionCheck(s, new BlockGivenInstr(s.getNewTemporaryVariable()), "yield");
            case BACKREFNODE:
                return buildDefinitionCheck(s, new BackrefIsMatchDataInstr(s.getNewTemporaryVariable()), "$" + ((BackRefNode) node).getType());
            case NTHREFNODE: {
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
                s.addInstr(new BackrefIsMatchDataInstr(tmpVar));
                s.addInstr(BEQInstr.create(tmpVar, manager.getFalse(), undefLabel));
                // SSS FIXME: 
                // - Can/should I use BEQInstr(new NthRef(n), manager.getNil(), undefLabel)? instead of .nil? & compare with flag?
                // - Or, even create a new IsNilInstr and NotNilInstr to represent optimized scenarios where
                //   the nil? method is not monkey-patched?
                // This matters because if String.nil? is monkey-patched, the two sequences can behave differently.
                s.addInstr(CallInstr.create(tmpVar, new MethAddr("nil?"), new NthRef(n), NO_ARGS, null));
                s.addInstr(BEQInstr.create(tmpVar, manager.getTrue(), undefLabel));
                return buildDefnCheckIfThenPaths(s, undefLabel, new StringLiteral("$" + n));
            }
            case COLON3NODE:
            case COLON2NODE: {
            // SSS FIXME: Is there a reason to do this all with low-level IR?
            // Can't this all be folded into a Java method that would be part
            // of the runtime library, which then can be used by buildDefinitionCheck method above?
            // This runtime library would be used both by the interpreter & the compiled code!

                final Colon3Node iVisited = (Colon3Node) node;
                final String name = iVisited.getName();

                // store previous exception for restoration if we rescue something
                Variable errInfo = s.getNewTemporaryVariable();
                s.addInstr(new GetErrorInfoInstr(errInfo));

                CodeBlock protectedCode = new CodeBlock() {
                    public Operand run(Object[] args) {
                        IRScope s    = (IRScope)args[0];
                        Node    n    = (Node)args[1];
                        String  name = (String)args[2];
                        Operand v    = (n instanceof Colon2Node) ? build(((Colon2Node)n).getLeftNode(), s) : new ObjectClass();

                        Variable tmpVar = s.getNewTemporaryVariable();
                        s.addInstr(new GetDefinedConstantOrMethodInstr(tmpVar, v, new StringLiteral(name)));
                        return tmpVar;
                    }
                };

                // rescue block
                CodeBlock rescueBlock = new CodeBlock() {
                    public Operand run(Object[] args) {
                        // Nothing to do -- ignore the exception, and restore stashed error info!
                        IRScope  m  = (IRScope)args[0];
                        m.addInstr(new RestoreErrorInfoInstr((Operand) args[1]));
                        return manager.getNil();
                    }
                };

                // Try verifying definition, and if we get an JumpException exception, process it with the rescue block above
                return protectCodeWithRescue(s, protectedCode, new Object[]{s, iVisited, name}, rescueBlock, new Object[] {s, errInfo});
            }
            case FCALLNODE: {
                /* ------------------------------------------------------------------
                 * Generate IR for:
                 *    r = self/receiver
                 *    mc = r.metaclass
                 *    return mc.methodBound(meth) ? buildGetArgumentDefn(..) : false
                 * ----------------------------------------------------------------- */
                Label undefLabel = s.getNewLabel();
                Variable tmpVar = s.getNewTemporaryVariable();
                StringLiteral mName = new StringLiteral(((FCallNode)node).getName());
                s.addInstr(new IsMethodBoundInstr(tmpVar, getSelf(s), mName));
                s.addInstr(BEQInstr.create(tmpVar, manager.getFalse(), undefLabel));
                Operand argsCheckDefn = buildGetArgumentDefinition(((FCallNode) node).getArgsNode(), s, "method");
                return buildDefnCheckIfThenPaths(s, undefLabel, argsCheckDefn);
            }
            case VCALLNODE:
                return buildDefinitionCheck(s, new IsMethodBoundInstr(s.getNewTemporaryVariable(), getSelf(s), new StringLiteral(((VCallNode) node).getName())), "method");
            case CALLNODE: {
            // SSS FIXME: Is there a reason to do this all with low-level IR?
            // Can't this all be folded into a Java method that would be part
            // of the runtime library?

                Label    undefLabel = s.getNewLabel();
                CallNode iVisited = (CallNode) node;
                Operand  receiverDefn = buildGetDefinition(iVisited.getReceiverNode(), s);
                s.addInstr(BEQInstr.create(receiverDefn, manager.getNil(), undefLabel));

                // protected main block
                CodeBlock protectedCode = new CodeBlock() {
                    public Operand run(Object[] args) {
                        IRScope  s          = (IRScope)args[0];
                        CallNode iVisited   = (CallNode)args[1];
                        String   methodName = iVisited.getName();
                        Variable tmpVar     = s.getNewTemporaryVariable();
                        Operand  receiver   = build(iVisited.getReceiverNode(), s);
                        s.addInstr(new MethodDefinedInstr(tmpVar, receiver, new StringLiteral(methodName)));
                        return buildDefnCheckIfThenPaths(s, (Label)args[2], tmpVar);
                    }
                };

                // rescue block
                CodeBlock rescueBlock = new CodeBlock() {
                    public Operand run(Object[] args) { return manager.getNil(); } // Nothing to do if we got an exception
                };

                // Try verifying definition, and if we get an exception, throw it out, and return nil
                return protectCodeWithRescue(s, protectedCode, new Object[]{s, iVisited, undefLabel}, rescueBlock, null);
            }
            case CLASSVARNODE: {
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
                return buildDefinitionCheck(s, new ClassVarIsDefinedInstr(s.getNewTemporaryVariable(), cm, new StringLiteral(iVisited.getName())), "class variable");
            }
            case ATTRASSIGNNODE: {
                Label  undefLabel = s.getNewLabel();
                AttrAssignNode iVisited = (AttrAssignNode) node;
                Operand receiverDefn = buildGetDefinition(iVisited.getReceiverNode(), s);
                s.addInstr(BEQInstr.create(receiverDefn, manager.getNil(), undefLabel));

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
                        s.addInstr(new MethodIsPublicInstr(tmpVar, receiver, attrMethodName));
                        s.addInstr(BEQInstr.create(tmpVar, manager.getFalse(), undefLabel));
                        s.addInstr(new IsMethodBoundInstr(tmpVar, getSelf(s), attrMethodName));
                        s.addInstr(BEQInstr.create(tmpVar, manager.getFalse(), undefLabel));
                        Operand argsCheckDefn = buildGetArgumentDefinition(((AttrAssignNode) node).getArgsNode(), s, "assignment");
                        return buildDefnCheckIfThenPaths(s, undefLabel, argsCheckDefn);
                    }
                };

                // rescue block
                CodeBlock rescueBlock = new CodeBlock() {
                    public Operand run(Object[] args) { return manager.getNil(); } // Nothing to do if we got an exception
                };

                // Try verifying definition, and if we get an JumpException exception, process it with the rescue block above
                return protectCodeWithRescue(s, protectedCode, new Object[]{s, iVisited, undefLabel}, rescueBlock, null);
            }
            case ZSUPERNODE:
                return buildDefinitionCheck(s, new SuperMethodBoundInstr(s.getNewTemporaryVariable(), getSelf(s)), "super");
            case SUPERNODE: {
                Label undefLabel = s.getNewLabel();
                Variable tmpVar  = s.getNewTemporaryVariable();
                s.addInstr(new SuperMethodBoundInstr(tmpVar, getSelf(s)));
                s.addInstr(BEQInstr.create(tmpVar, manager.getFalse(), undefLabel));
                Operand superDefnVal = buildGetArgumentDefinition(((SuperNode) node).getArgsNode(), s, "super");
                return buildDefnCheckIfThenPaths(s, undefLabel, superDefnVal);
            }
            default: {
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
                    public Operand run(Object[] args) { return manager.getNil(); } // Nothing to do if we got an exception
                };

                // Try verifying definition, and if we get an JumpException exception, process it with the rescue block above
                return protectCodeWithRescue(s, protectedCode, new Object[]{node, s}, rescueBlock, null);
            }
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
        IRMethod method = new IRMethod(manager, s, defNode.getName(), isInstanceMethod, defNode.getPosition().getLine(), defNode.getScope());

        s.addInstr(new ReceiveSelfInstr(getSelf(s)));

        // Set %current_scope = <current-scope>
        // Set %current_module = isInstanceMethod ? %self.metaclass : %self
        IRScope nearestScope = s.getNearestModuleReferencingScope();
        method.addInstr(new CopyInstr(method.getCurrentScopeVariable(), new CurrentScope(nearestScope == null ? s : nearestScope)));
        method.addInstr(new CopyInstr(method.getCurrentModuleVariable(), new CurrentModule(nearestScope == null ? s : nearestScope)));

        // Build IR for arguments (including the block arg)
        receiveMethodArgs(defNode.getArgsNode(), method);

        // Thread poll on entry to method
        method.addInstr(new ThreadPollInstr());

        // Build IR for body
        Node bodyNode = defNode.getBodyNode();
        if (bodyNode != null) {
            // Create a new nested builder to ensure this gets its own IR builder state 
            Operand rv = createIRBuilder(manager).build(bodyNode, method);
            if (rv != null) method.addInstr(new ReturnInstr(rv));
        } else {
            method.addInstr(new ReturnInstr(manager.getNil()));
        }

        return method;
    }

    public Operand buildDefn(MethodDefNode node, IRScope s) { // Instance method
        IRMethod method = defineNewMethod(node, s, true);
        s.addInstr(new DefineInstanceMethodInstr(new StringLiteral("--unused--"), method));
        return manager.getNil();
    }

    public Operand buildDefs(DefsNode node, IRScope s) { // Class method
        Operand container =  build(node.getReceiverNode(), s);
        IRMethod method = defineNewMethod(node, s, false);

        s.addInstr(new DefineClassMethodInstr(container, method));
        return manager.getNil();
    }

    protected int receiveOptArgs(final ArgsNode argsNode, IRScope s, int opt, int argIndex) {
        ListNode optArgs = argsNode.getOptArgs();
        for (int j = 0; j < opt; j++, argIndex++) {
            // Jump to 'l' if this arg is not null.  If null, fall through and build the default value!
            Label l = s.getNewLabel();
            LocalAsgnNode n = (LocalAsgnNode)optArgs.get(j);
            String argName = n.getName();
            Variable av = s.getLocalVariable(argName, 0);
            if (s instanceof IRMethod) ((IRMethod)s).addArgDesc("opt", argName);
            s.addInstr(new ReceiveOptArgInstr(av, argIndex));
            s.addInstr(BNEInstr.create(av, UndefinedValue.UNDEFINED, l)); // if 'av' is not undefined, go to default
            build(n, s);
            s.addInstr(new LabelInstr(l));
        }
        return argIndex;
    }

    public void receiveMethodArgs(final ArgsNode argsNode, IRScope s) {
        final int required = argsNode.getRequiredArgsCount();
        final int opt = argsNode.getOptionalArgsCount();
        final int rest = argsNode.getRestArg();

        s.getStaticScope().setArities(required, opt, rest);

        // FIXME: Expensive to do this explicitly?  But, two advantages:
        // (a) on inlining, we'll be able to get rid of these checks in almost every case.
        // (b) compiler to bytecode will anyway generate this and this is explicit.
        // For now, we are going explicit instruction route.  But later, perhaps can make this implicit in the method setup preamble?  
        s.addInstr(new CheckArityInstr(required, opt, rest));

        // Other args begin at index 0
        int argIndex = 0;

        // Both for fixed arity and variable arity methods
        ListNode preArgs  = argsNode.getPre();
        for (int i = 0; i < required; i++, argIndex++) {
            ArgumentNode a = (ArgumentNode)preArgs.get(i);
            String argName = a.getName();
            s.addInstr(new ReceivePreReqdArgInstr(s.getLocalVariable(argName, 0), argIndex));
            if (s instanceof IRMethod) ((IRMethod)s).addArgDesc("req", argName);
        }
      
        if (opt > 0) {
            argIndex = receiveOptArgs(argsNode, s, opt, argIndex);
        }

        if (rest > -1) {
            // Consider: def foo(*); .. ; end
            // For this code, there is no argument name available from the ruby code.
            // So, we generate an implicit arg name
            String argName = argsNode.getRestArgNode().getName();
            if (s instanceof IRMethod) ((IRMethod)s).addArgDesc("rest", argName);
            argName = (argName.equals("")) ? "%_arg_array" : argName;
            s.addInstr(new ReceiveRestArgInstr(s.getLocalVariable(argName, 0), argIndex));
        }

        // Receive block
        receiveMethodClosureArg(argsNode, s);
    }

    private void receiveMethodClosureArg(ArgsNode argsNode, IRScope s) {
        Variable blockVar = null;
        if (argsNode.getBlock() != null) {
            String blockArgName = argsNode.getBlock().getName();
            blockVar = s.getLocalVariable(blockArgName, 0);
            if (s instanceof IRMethod) ((IRMethod)s).addArgDesc("block", blockArgName);
            s.addInstr(new ReceiveClosureInstr(blockVar));
        }

        // SSS FIXME: This instruction is only needed if there is an yield instr somewhere!
        // In addition, store the block argument in an implicit block variable
        Variable implicitBlockArg = s.getImplicitBlockArg();
        if (blockVar == null) s.addInstr(new ReceiveClosureInstr(implicitBlockArg));
        else s.addInstr(new CopyInstr(implicitBlockArg, blockVar));
    }

    public void receiveBlockArgs(final IterNode node, IRScope s) {
        buildBlockArgsAssignment(node.getVarNode(), s, null, 0, true, false, false);
    }

    public void receiveBlockClosureArg(final Node node, IRScope s) {
        if (node != null) buildBlockArgsAssignment(node, s, null, 0, true, true, false);
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
        
        return piece == null ? manager.getNil() : piece;
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
        
        return copyAndReturnValue(s, new CompoundString(strPieces, dstrNode.getEncoding()));
    }

    public Operand buildDSymbol(DSymbolNode node, IRScope s) {
        List<Operand> strPieces = new ArrayList<Operand>();
        for (Node n : node.childNodes()) {
            strPieces.add(dynamicPiece(n, s));
        }

        return copyAndReturnValue(s, new DynamicSymbol(new CompoundString(strPieces, node.getEncoding())));
    }

    public Operand buildDVar(DVarNode node, IRScope s) {
        return s.getLocalVariable(node.getName(), node.getDepth());
    }

    public Operand buildDXStr(final DXStrNode dstrNode, IRScope s) {
        List<Operand> strPieces = new ArrayList<Operand>();
        for (Node nextNode : dstrNode.childNodes()) {
            strPieces.add(dynamicPiece(nextNode, s));
        }

        return copyAndReturnValue(s, new BacktickString(strPieces));
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
    public Operand buildEnsureNode(EnsureNode ensureNode, IRScope s) {
        Node bodyNode = ensureNode.getBodyNode();

        // Push a new ensure block info node onto the stack of ensure block
        EnsureBlockInfo ebi = new EnsureBlockInfo(s, (bodyNode instanceof RescueNode) ? (RescueNode)bodyNode : null, getCurrentLoop());
        _ensureBlockStack.push(ebi);

        Label rBeginLabel = ebi.regionStart;
        Label rEndLabel   = ebi.end;

        // start of protected region
        s.addInstr(new LabelInstr(rBeginLabel));
        s.addInstr(new ExceptionRegionStartMarkerInstr(rBeginLabel, rEndLabel, ebi.dummyRescueBlockLabel, ebi.dummyRescueBlockLabel));

        // Generate IR for code being protected
        Operand rv;  
        if (bodyNode instanceof RescueNode) {
            // The rescue code will ensure that the region is ended
            rv = buildRescueInternal((RescueNode) bodyNode, s, ebi);
        } else {
            rv = build(bodyNode, s);

            // Jump to start of ensure block -- dont bother if we had a return in the protected body 
            if (rv != U_NIL) s.addInstr(new SetReturnAddressInstr(ebi.returnAddr, rEndLabel));
        }

        // end of protected region
        s.addInstr(new ExceptionRegionEndMarkerInstr()); 

        // Pop the current ensure block info node *BEFORE* generating the ensure code for this block itself!
        _ensureBlockStack.pop();

        // Run the ensure block now
        s.addInstr(new JumpInstr(ebi.start));

        // Now build the dummy rescue block that:
        // * catches all exceptions thrown by the body
        // * jumps to the ensure block code
        // * returns back (via set_retaddr instr)
        Label rethrowExcLabel = s.getNewLabel();
        Variable exc = s.getNewTemporaryVariable();
        s.addInstr(new LabelInstr(ebi.dummyRescueBlockLabel));
        s.addInstr(new ReceiveExceptionInstr(exc));
        s.addInstr(new SetReturnAddressInstr(ebi.returnAddr, rethrowExcLabel));

        // Generate the ensure block now
        s.addInstr(new LabelInstr(ebi.start));

        // Two cases:
        // 1. Ensure block has no explicit return => the result of the entire ensure expression is the result of the protected body.
        // 2. Ensure block has an explicit return => the result of the protected body is ignored.
        Operand ensureRetVal = (ensureNode.getEnsureNode() == null) ? manager.getNil() : build(ensureNode.getEnsureNode(), s);
        // U_NIL => there was a return from within the ensure block!
        if (ensureRetVal == U_NIL) rv = U_NIL;

        // Return (rethrow exception/end)
        s.addInstr(new JumpIndirectInstr(ebi.returnAddr));

        // rethrows the caught exception from the dummy ensure block
        s.addInstr(new LabelInstr(rethrowExcLabel));
        s.addInstr(new ThrowExceptionInstr(exc));

        // End label for the exception region
        s.addInstr(new LabelInstr(rEndLabel));

        return rv;
    }

    public Operand buildEvStr(EvStrNode node, IRScope s) {
        return new AsString(build(node.getBody(), s));
    }

    public Operand buildFalse(Node node, IRScope s) {
        return manager.getFalse(); 
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
        if (node == null) return null;

        switch (node.getNodeType()) {
            case ITERNODE:
                return build((IterNode)node, s);
            case BLOCKPASSNODE:
                return build(((BlockPassNode)node).getBodyNode(), s);
            default:
                throw new NotCompilableException("ERROR: Encountered a method with a non-block, non-blockpass iter node at: " + node);
        }
    }

    public Operand buildFixnum(FixnumNode node, IRScope s) {
        return new Fixnum(node.getValue());
    }

    public Operand buildFlip(FlipNode flipNode, IRScope s) {
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

        Fixnum s1 = new Fixnum((long)1);
        Fixnum s2 = new Fixnum((long)2);

        // Create a variable to hold the flip state 
        IRScope nearestNonClosure = s.getNearestFlipVariableScope();
        Variable flipState = nearestNonClosure.getNewFlipStateVariable();
        nearestNonClosure.initFlipStateVariable(flipState, s1);
        if (s instanceof IRClosure) {
            // Clone the flip variable to be usable at the proper-depth.
            int n = 0;
            IRScope x = s;
            while (!x.isFlipScope()) {
                if (!x.isForLoopBody()) n++;
                x = x.getLexicalParent();
            }
            if (n > 0) flipState = ((LocalVariable)flipState).cloneForDepth(n);
        }

        // Variables and labels needed for the code
        Variable returnVal = s.getNewTemporaryVariable();
        Label    s2Label   = s.getNewLabel();
        Label    doneLabel = s.getNewLabel();

        // Init
        s.addInstr(new CopyInstr(returnVal, manager.getFalse()));

        // Are we in state 1?
        s.addInstr(BNEInstr.create(flipState, s1, s2Label));

        // ----- Code for when we are in state 1 -----
        Operand s1Val = build(flipNode.getBeginNode(), s);
        s.addInstr(BNEInstr.create(s1Val, manager.getTrue(), s2Label));

        // s1 condition is true => set returnVal to true & move to state 2
        s.addInstr(new CopyInstr(returnVal, manager.getTrue()));
        s.addInstr(new CopyInstr(flipState, s2));

        // Check for state 2
        s.addInstr(new LabelInstr(s2Label));

        // For exclusive ranges/flips, we dont evaluate s2's condition if s1's condition was satisfied
        if (flipNode.isExclusive()) s.addInstr(BEQInstr.create(returnVal, manager.getTrue(), doneLabel));

        // Are we in state 2?
        s.addInstr(BNEInstr.create(flipState, s2, doneLabel));

        // ----- Code for when we are in state 2 -----
        Operand s2Val = build(flipNode.getEndNode(), s);
        s.addInstr(new CopyInstr(returnVal, manager.getTrue()));
        s.addInstr(BNEInstr.create(s2Val, manager.getTrue(), doneLabel));

        // s2 condition is true => move to state 1 
        s.addInstr(new CopyInstr(flipState, s1));

        // Done testing for s1's and s2's conditions.  
        // returnVal will have the result of the flip condition
        s.addInstr(new LabelInstr(doneLabel));

        return returnVal;
    }

    public Operand buildFloat(FloatNode node, IRScope s) {
        // SSS: Since flaot literals are effectively interned objects, no need to copyAndReturnValue(...)
        // Or is this a premature optimization?
        return new Float(node.getValue());
    }

    public Operand buildFor(ForNode forNode, IRScope s) {
        Variable result = s.getNewTemporaryVariable();
        Operand  receiver = build(forNode.getIterNode(), s);
        Operand  forBlock = buildForIter(forNode, s);     
        // SSS FIXME: Really?  Why the internal call?
        s.addInstr(new CallInstr(CallType.NORMAL, result, new MethAddr("each"), receiver, NO_ARGS, forBlock));

        return result;
    }

    public Operand buildForIter(final ForNode forNode, IRScope s) {
            // Create a new closure context
        IRClosure closure = new IRClosure(manager, s, true, forNode.getPosition().getStartLine(), forNode.getScope(), Arity.procArityOf(forNode.getVarNode()), forNode.getArgumentType(), is1_9());
        s.addClosure(closure);

            // Receive self
        closure.addInstr(new ReceiveSelfInstr(getSelf(closure)));

            // Build args
        Node varNode = forNode.getVarNode();
        if (varNode != null && varNode.getNodeType() != null) receiveBlockArgs(forNode, closure);

        // Set %current_scope = <current-scope>
        // Set %current_module = <current-module>
        closure.addInstr(new CopyInstr(closure.getCurrentScopeVariable(), new CurrentScope(closure)));
        closure.addInstr(new CopyInstr(closure.getCurrentModuleVariable(), new CurrentModule(closure)));

        // Thread poll on entry of closure 
        closure.addInstr(new ThreadPollInstr());

            // Start label -- used by redo!
        closure.addInstr(new LabelInstr(closure.startLabel));

            // Build closure body and return the result of the closure
        Operand closureRetVal = forNode.getBodyNode() == null ? manager.getNil() : build(forNode.getBodyNode(), closure);
        if (closureRetVal != U_NIL)  // can be null if the node is an if node with returns in both branches.
            closure.addInstr(new ClosureReturnInstr(closureRetVal));

        return new WrappedIRClosure(closure);
    }

    public Operand buildGlobalAsgn(GlobalAsgnNode globalAsgnNode, IRScope s) {
        Operand value = build(globalAsgnNode.getValueNode(), s);
        s.addInstr(new PutGlobalVarInstr(globalAsgnNode.getName(), value));
        return value;
    }

    public Operand buildGlobalVar(GlobalVarNode node, IRScope s) {
        Variable rv  = s.getNewTemporaryVariable();
        s.addInstr(new GetGlobalVariableInstr(rv, node.getName()));
        return rv;
    }

    public Operand buildHash(HashNode hashNode, IRScope s) {
        if (hashNode.getListNode() == null || hashNode.getListNode().size() == 0) {
            return copyAndReturnValue(s, new Hash(new ArrayList<KeyValuePair>()));
        } else {
            int     i     = 0;
            Operand key   = null;
            Operand value = null;
            List<KeyValuePair> args = new ArrayList<KeyValuePair>();
            for (Node nextNode : hashNode.getListNode().childNodes()) {
                Operand v = build(nextNode, s);
                if (key == null) {
                    key = v;
                } else {
                    args.add(new KeyValuePair(key, v));
                    key = null; 
                }
            }
            return copyAndReturnValue(s, new Hash(args));
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

        Variable result;
        Label    falseLabel = s.getNewLabel();
        Label    doneLabel  = s.getNewLabel();
        Operand  thenResult;
        s.addInstr(BEQInstr.create(build(actualCondition, s), manager.getFalse(), falseLabel));

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
            } else {
                result = s.getNewTemporaryVariable();
                thenUnil = true;
            }
        } else {
            thenNull = true;
            result = s.getNewTemporaryVariable();
            s.addInstr(new CopyInstr(result, manager.getNil()));
            s.addInstr(new JumpInstr(doneLabel));
        }

        // Build the else part of the if-statement
        s.addInstr(new LabelInstr(falseLabel));
        if (ifNode.getElseBody() != null) {
            Operand elseResult = build(ifNode.getElseBody(), s);
            // elseResult can be U_NIL if then-body ended with a return!
            if (elseResult != U_NIL) {
                s.addInstr(new CopyInstr(result, elseResult));
            } else {
                elseUnil = true;
            }
        } else {
            elseNull = true;
            s.addInstr(new CopyInstr(result, manager.getNil()));
        }

        if (thenNull && elseNull) {
            s.addInstr(new LabelInstr(doneLabel));
            return manager.getNil();
        } else if (thenUnil && elseUnil) {
            return U_NIL;
        } else {
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

    public Operand buildInstVar(InstVarNode node, IRScope s) {
        Variable ret = s.getNewTemporaryVariable();
        s.addInstr(new GetFieldInstr(ret, getSelf(s), node.getName()));
        return ret;
    }

    public Operand buildIter(final IterNode iterNode, IRScope s) {
        IRClosure closure = new IRClosure(manager, s, false, iterNode.getPosition().getStartLine(), iterNode.getScope(), Arity.procArityOf(iterNode.getVarNode()), iterNode.getArgumentType(), is1_9());
        s.addClosure(closure);

        // Create a new nested builder to ensure this gets its own IR builder state 
        // like the ensure block stack
        IRBuilder closureBuilder = createIRBuilder(manager);

        // Receive self
        closure.addInstr(new ReceiveSelfInstr(getSelf(closure)));

        // Build args
        NodeType argsNodeId = BlockBody.getArgumentTypeWackyHack(iterNode);
        if ((iterNode.getVarNode() != null) && (argsNodeId != null))
            closureBuilder.receiveBlockArgs(iterNode, closure);
        closureBuilder.receiveBlockClosureArg(iterNode.getBlockVarNode(), closure);

        // Set %current_scope = <current-scope>
        // Set %current_module = <current-module>
        closure.addInstr(new CopyInstr(closure.getCurrentScopeVariable(), new CurrentScope(closure)));
        closure.addInstr(new CopyInstr(closure.getCurrentModuleVariable(), new CurrentModule(closure)));

        // Thread poll on entry of closure 
        closure.addInstr(new ThreadPollInstr());

        // start label -- used by redo!
        closure.addInstr(new LabelInstr(closure.startLabel));

        // Build closure body and return the result of the closure
        Operand closureRetVal = iterNode.getBodyNode() == null ? manager.getNil() : closureBuilder.build(iterNode.getBodyNode(), closure);
        if (closureRetVal != U_NIL)  // can be U_NIL if the node is an if node with returns in both branches.
            closure.addInstr(new ClosureReturnInstr(closureRetVal));

        return new WrappedIRClosure(closure);
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

    public Operand buildMatch(MatchNode matchNode, IRScope s) {
        Operand regexp = build(matchNode.getRegexpNode(), s);
        Variable result = s.getNewTemporaryVariable();
        s.addInstr(new MatchInstr(result, regexp));
        return result;
    }

    public Operand buildMatch2(Match2Node matchNode, IRScope s) {
        Operand receiver = build(matchNode.getReceiverNode(), s);
        Operand value    = build(matchNode.getValueNode(), s);
        Variable result = s.getNewTemporaryVariable();        
        s.addInstr(new Match2Instr(result, receiver, value));
        return result;
    }

    public Operand buildMatch3(Match3Node matchNode, IRScope s) {
        Operand receiver = build(matchNode.getReceiverNode(), s);
        Operand value    = build(matchNode.getValueNode(), s);
        Variable result = s.getNewTemporaryVariable();
        s.addInstr(new Match3Instr(result, receiver, value));
        return result;
    }

    private Operand getContainerFromCPath(Colon3Node cpath, IRScope s) {
        Operand container;

        if (cpath instanceof Colon2Node) {
            Node leftNode = ((Colon2Node) cpath).getLeftNode();
            
            if (leftNode != null) { // Foo::Bar
                container = build(leftNode, s);
            } else { // Only name with no left-side Bar <- Note no :: on left
                container = findContainerModule(s);
            }
        } else { //::Bar
            container = new CurrentModule(manager.getObject());
        }

        return container;
    }

    public Operand buildModule(ModuleNode moduleNode, IRScope s) {
        Colon3Node cpath = moduleNode.getCPath();
        String moduleName = cpath.getName();
        Operand container = getContainerFromCPath(cpath, s);

        // Build the new module
        IRModuleBody m = new IRModuleBody(manager, s, moduleName, moduleNode.getPosition().getLine(), moduleNode.getScope());
        Variable ret = s.getNewTemporaryVariable();
        s.addInstr(new DefineModuleInstr(m, ret, container));

        m.addInstr(new ReceiveSelfInstr(m.getSelf()));
        // Set %current_scope = <c>
        // Set %current_module = module<c>
        m.addInstr(new CopyInstr(m.getCurrentScopeVariable(), new CurrentScope(m)));
        m.addInstr(new CopyInstr(m.getCurrentModuleVariable(), new CurrentModule(m)));
        // Create a new nested builder to ensure this gets its own IR builder state 
        Operand rv = createIRBuilder(manager).build(moduleNode.getBodyNode(), m);
        if (rv != null) m.addInstr(new ReturnInstr(rv));

        return ret;
    }

    public Operand buildMultipleAsgn(MultipleAsgnNode multipleAsgnNode, IRScope s) {
        Operand  values = build(multipleAsgnNode.getValueNode(), s);
        Variable ret = getValueInTemporaryVariable(s, values);
        buildMultipleAsgnAssignment(multipleAsgnNode, s, null, ret);
        return ret;
    }

    // SSS: This method is called both for regular multiple assignment as well as argument passing
    //
    // Ex: a,b,*c=v  is a regular assignment and in this case, the "values" operand will be non-null
    // Ex: { |a,b,*c| ..} is the argument passing case
    public void buildMultipleAsgnAssignment(final MultipleAsgnNode multipleAsgnNode, IRScope s, Operand argsArray, Operand values) {
        final ListNode sourceArray = multipleAsgnNode.getHeadNode();

        // First, build assignments for specific named arguments
        int i = 0; 
        if (sourceArray != null) {
            for (Node an: sourceArray.childNodes()) {
                if (values == null) {
                    buildBlockArgsAssignment(an, s, argsArray, i, false, false, false);
                } else {
                    Variable rhsVal = s.getNewTemporaryVariable();
                    s.addInstr(new ReqdArgMultipleAsgnInstr(rhsVal, values, i));
                    buildAssignment(an, s, rhsVal);
                }
                i++;
            }
        }

        // First, build an assignment for a splat, if any, with the rest of the args!
        Node argsNode = multipleAsgnNode.getArgsNode();
        if (argsNode == null) {
            if (sourceArray == null)
                throw new NotCompilableException("Something's wrong, multiple assignment with no head or args at: " + multipleAsgnNode.getPosition());
        } else if (argsNode instanceof StarNode) {
            // do nothing
        } else if (values != null) {
            Variable rhsVal = s.getNewTemporaryVariable();
            s.addInstr(new RestArgMultipleAsgnInstr(rhsVal, values, i));
            buildAssignment(argsNode, s, rhsVal); // rest of the argument array!
        } else {
            buildBlockArgsAssignment(argsNode, s, argsArray, i, false, false, true); // rest of the argument array!
        }

    }

    public Operand buildNewline(NewlineNode node, IRScope s) {
        return build(skipOverNewlines(s, node), s);
    }

    public Operand buildNext(final NextNode nextNode, IRScope s) {
        IRLoop currLoop = getCurrentLoop();

        Operand rv = (nextNode.getValueNode() == null) ? manager.getNil() : build(nextNode.getValueNode(), s);

        // If we have ensure blocks, have to run those first!
        if (!_ensureBlockStack.empty()) EnsureBlockInfo.emitJumpChain(s, _ensureBlockStack, currLoop);
        else if (!_rescueBlockStack.empty()) _rescueBlockStack.peek().restoreException(s, currLoop);

        if (currLoop != null) {
            // If a regular loop, the next is simply a jump to the end of the iteration
            s.addInstr(new JumpInstr(currLoop.iterEndLabel));
        } else {
            s.addInstr(new ThreadPollInstr(true));
            // If a closure, the next is simply a return from the closure!
            if (s instanceof IRClosure) s.addInstr(new ClosureReturnInstr(rv));
            else s.addInstr(new ThrowExceptionInstr(IRException.NEXT_LocalJumpError));
        }

        // Once the "next instruction" (closure-return) executes, control exits this scope
        return UnexecutableNil.U_NIL;
    }

    public Operand buildNthRef(NthRefNode nthRefNode, IRScope s) {
        return copyAndReturnValue(s, new NthRef(nthRefNode.getMatchNumber()));
    }

    public Operand buildNil(Node node, IRScope s) {
        return manager.getNil();
    }

    public Operand buildNot(NotNode node, IRScope s) {
        Variable ret = s.getNewTemporaryVariable();
        s.addInstr(new NotInstr(ret, build(node.getConditionNode(), s)));
        return ret;
    }

    public Operand buildOpAsgn(OpAsgnNode opAsgnNode, IRScope s) {
        Label l;
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
            s.addInstr(BEQInstr.create(readerValue, opName.equals("||") ? manager.getTrue() : manager.getFalse(), l));

            // compute value and set it
            Operand  v2 = build(opAsgnNode.getValueNode(), s);
            s.addInstr(CallInstr.create(writerValue, new MethAddr(opAsgnNode.getVariableNameAsgn()), v1, new Operand[] {v2}, null));
            // It is readerValue = v2.
            // readerValue = writerValue is incorrect because the assignment method
            // might return something else other than the value being set!
            s.addInstr(new CopyInstr(readerValue, v2));
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
            // Returning writerValue is incorrect becuase the assignment method
            // might return something else other than the value being set!
            return setValue;
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
        s.addInstr(BEQInstr.create(v1, manager.getFalse(), l));
        Operand v2 = build(andNode.getSecondNode(), s);  // This does the assignment!
        s.addInstr(new CopyInstr(result, v2));
        s.addInstr(new LabelInstr(l));
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
            s.addInstr(BEQInstr.create(flag, manager.getNil(), l2)); // if v1 is undefined, go to v2's computation
        }
        v1 = build(orNode.getFirstNode(), s); // build of 'x'
        s.addInstr(new CopyInstr(flag, v1));
        Variable result = getValueInTemporaryVariable(s, v1);
        if (needsDefnCheck) {
            s.addInstr(new LabelInstr(l2));
        }
        s.addInstr(BEQInstr.create(flag, manager.getTrue(), l1));  // if v1 is defined and true, we are done! 
        Operand v2 = build(orNode.getSecondNode(), s); // This is an AST node that sets x = y, so nothing special to do here.
        s.addInstr(new CopyInstr(result, v2));
        s.addInstr(new LabelInstr(l1));

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

    public Operand buildOpElementAsgn(OpElementAsgnNode node, IRScope s) {
        if (node.isOr()) return buildOpElementAsgnWithOr(node, s);
        if (node.isAnd()) return buildOpElementAsgnWithAnd(node, s);
            
        return buildOpElementAsgnWithMethod(node, s);
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
    public Operand buildOpElementAsgnWithOr(OpElementAsgnNode opElementAsgnNode, IRScope s) {
        Operand array = build(opElementAsgnNode.getReceiverNode(), s);
        Label    l     = s.getNewLabel();
        Variable elt   = s.getNewTemporaryVariable();
        List<Operand> argList = setupCallArgs(opElementAsgnNode.getArgsNode(), s);
        s.addInstr(CallInstr.create(elt, new MethAddr("[]"), array, argList.toArray(new Operand[argList.size()]), null));
        s.addInstr(BEQInstr.create(elt, manager.getTrue(), l));
        Operand value = build(opElementAsgnNode.getValueNode(), s);
        argList.add(value);
        s.addInstr(CallInstr.create(elt, new MethAddr("[]="), array, argList.toArray(new Operand[argList.size()]), null));
        s.addInstr(new CopyInstr(elt, value));
        s.addInstr(new LabelInstr(l));
        return elt;
    }

    // Translate "a[x] &&= n" --> "a[x] = n if is_true(a[x])"
    public Operand buildOpElementAsgnWithAnd(OpElementAsgnNode opElementAsgnNode, IRScope s) {
        Operand array = build(opElementAsgnNode.getReceiverNode(), s);
        Label    l     = s.getNewLabel();
        Variable elt   = s.getNewTemporaryVariable();
        List<Operand> argList = setupCallArgs(opElementAsgnNode.getArgsNode(), s);
        s.addInstr(CallInstr.create(elt, new MethAddr("[]"), array, argList.toArray(new Operand[argList.size()]), null));
        s.addInstr(BEQInstr.create(elt, manager.getFalse(), l));
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
    public Operand buildOpElementAsgnWithMethod(OpElementAsgnNode opElementAsgnNode, IRScope s) {
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
    public Operand buildOr(final OrNode orNode, IRScope s) {
        if (orNode.getFirstNode().getNodeType().alwaysTrue()) {
            // build first node only and return true
            return build(orNode.getFirstNode(), s);
        } else if (orNode.getFirstNode().getNodeType().alwaysFalse()) {
            // build first node as non-expr and build second node
            build(orNode.getFirstNode(), s);
            return build(orNode.getSecondNode(), s);
        } else {
            Label    l   = s.getNewLabel();
            Operand  v1  = build(orNode.getFirstNode(), s);
            Variable ret = getValueInTemporaryVariable(s, v1);
            s.addInstr(BEQInstr.create(v1, manager.getTrue(), l));
            Operand  v2  = build(orNode.getSecondNode(), s);
            s.addInstr(new CopyInstr(ret, v2));
            s.addInstr(new LabelInstr(l));
            return ret;
        }
    }

    public Operand buildPostExe(PostExeNode postExeNode, IRScope s) {
        IRClosure endClosure = new IRClosure(manager, s, false, postExeNode.getPosition().getStartLine(), postExeNode.getScope(), Arity.procArityOf(postExeNode.getVarNode()), postExeNode.getArgumentType(), is1_9());
        // Set up %current_scope and %current_module
        endClosure.addInstr(new CopyInstr(endClosure.getCurrentScopeVariable(), new CurrentScope(endClosure)));
        endClosure.addInstr(new CopyInstr(endClosure.getCurrentModuleVariable(), new CurrentModule(endClosure)));
        build(postExeNode.getBodyNode(), endClosure);

        // Add an instruction to record the end block at runtime
        s.addInstr(new RecordEndBlockInstr(s, endClosure));
        return manager.getNil();
    }

    public Operand buildPreExe(PreExeNode preExeNode, IRScope s) {
        IRClosure beginClosure = new IRClosure(manager, s, false, preExeNode.getPosition().getStartLine(), preExeNode.getScope(), Arity.procArityOf(preExeNode.getVarNode()), preExeNode.getArgumentType(), is1_9());
        // Set up %current_scope and %current_module
        beginClosure.addInstr(new CopyInstr(beginClosure.getCurrentScopeVariable(), new CurrentScope(beginClosure)));
        beginClosure.addInstr(new CopyInstr(beginClosure.getCurrentModuleVariable(), new CurrentModule(beginClosure)));
        build(preExeNode.getBodyNode(), beginClosure);

        // Record the begin block at IR build time
        s.getTopLevelScope().recordBeginBlock(beginClosure);
        return manager.getNil();
    }

    public Operand buildRedo(Node node, IRScope s) {
        // If in a loop, a redo is a jump to the beginning of the loop.
        // If not, for closures, a redo is a jump to the beginning of the closure.
        // If not in a loop or a closure, it is a local jump error
        IRLoop currLoop = getCurrentLoop();
        if (currLoop != null) {
             s.addInstr(new JumpInstr(currLoop.iterStartLabel));
        } else {
            if (s instanceof IRClosure) {
                s.addInstr(new ThreadPollInstr(true));
                s.addInstr(new JumpInstr(((IRClosure)s).startLabel));
            } else {
                s.addInstr(new ThrowExceptionInstr(IRException.REDO_LocalJumpError));
            }
        }
        return manager.getNil();
    }

    public Operand buildRegexp(RegexpNode reNode, IRScope s) {
        return copyAndReturnValue(s, new Regexp(new StringLiteral(reNode.getValue()), reNode.getOptions()));
    }

    public Operand buildRescue(RescueNode node, IRScope s) {
        return buildRescueInternal(node, s, null);
    }

    private Operand buildRescueInternal(RescueNode rescueNode, IRScope s, EnsureBlockInfo ensure) {
        // Labels marking start, else, end of the begin-rescue(-ensure)-end block
        Label rBeginLabel = ensure == null ? s.getNewLabel() : ensure.regionStart;
        Label rEndLabel   = ensure == null ? s.getNewLabel() : ensure.end;
        Label rescueLabel = s.getNewLabel(); // Label marking start of the first rescue code.

        if (ensure == null) s.addInstr(new LabelInstr(rBeginLabel));

        // Placeholder rescue instruction that tells rest of the compiler passes the boundaries of the rescue block.
        s.addInstr(new ExceptionRegionStartMarkerInstr(rBeginLabel, rEndLabel, ensure == null ? null : ensure.dummyRescueBlockLabel, rescueLabel));

        // Save $! in a temp var so it can be restored when the exception gets handled.
        // SSS FIXME: Dont yet understand why an exception needs to be saved/restored.
        Variable savedGlobalException = s.getNewTemporaryVariable();
        s.addInstr(new GetGlobalVariableInstr(savedGlobalException, "$!"));
        if (ensure != null) ensure.savedGlobalException = savedGlobalException;

        // Body
        Operand tmp = manager.getNil();  // default return value if for some strange reason, we neither have the body node or the else node!
        Variable rv = s.getNewTemporaryVariable();
        if (rescueNode.getBodyNode() != null) tmp = build(rescueNode.getBodyNode(), s);

        // Push rescue block *after* body has been built.  
        // If not, this messes up generation of retry in these scenarios like this:
        //
        //     begin    -- 1
        //       ...
        //     rescue
        //       begin  -- 2
        //         ...
        //         retry
        //       rescue   
        //         ...
        //       end
        //     end
        //
        // The retry should jump to 1, not 2.
        // If we push the rescue block before building the body, we will jump to 2.
        _rescueBlockStack.push(new RescueBlockInfo(rescueNode, rBeginLabel, savedGlobalException, getCurrentLoop()));

        // Since rescued regions are well nested within Ruby, this bare marker is sufficient to
        // let us discover the edge of the region during linear traversal of instructions during cfg construction.
        ExceptionRegionEndMarkerInstr rbEndInstr = new ExceptionRegionEndMarkerInstr();
        s.addInstr(rbEndInstr);

        // Else part of the body -- we simply fall through from the main body if there were no exceptions
        Label elseLabel = rescueNode.getElseNode() == null ? null : s.getNewLabel();
        if (elseLabel != null) {
            s.addInstr(new LabelInstr(elseLabel));
            tmp = build(rescueNode.getElseNode(), s);
        }

        if (tmp != U_NIL) {
            s.addInstr(new CopyInstr(rv, tmp));

            // No explicit return from the protected body
            // - If we dont have any ensure blocks, simply jump to the end of the rescue block
            // - If we do, get the innermost ensure block, set up the return address to the end of the ensure block, and go execute the ensure code.
            if (ensure == null) {
                s.addInstr(new JumpInstr(rEndLabel));
            } else {
                // NOTE: rEndLabel is identical to ensure.end, but less confusing to use rEndLabel since that makes more semantic sense
                s.addInstr(new SetReturnAddressInstr(ensure.returnAddr, rEndLabel));
                s.addInstr(new JumpInstr(ensure.start));
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
        s.addInstr(new LabelInstr(rescueLabel));
        buildRescueBodyInternal(s, rescueNode.getRescueNode(), rv, rEndLabel);

        // End label -- only if there is no ensure block!  With an ensure block, you end at ensureEndLabel.
        if (ensure == null) s.addInstr(new LabelInstr(rEndLabel));

        _rescueBlockStack.pop();
        return rv;
    }

    private void outputExceptionCheck(IRScope s, Operand excType, Operand excObj, Label caughtLabel) {
        Variable eqqResult = s.getNewTemporaryVariable();
        s.addInstr(new RescueEQQInstr(eqqResult, excType, excObj));
        s.addInstr(BEQInstr.create(eqqResult, manager.getTrue(), caughtLabel));
    }

    private void buildRescueBodyInternal(IRScope s, Node node, Variable rv, Label endLabel) {
        final RescueBodyNode rescueBodyNode = (RescueBodyNode) node;
        final Node exceptionList = rescueBodyNode.getExceptionNodes();

        // Load exception & exception comparison type
        Variable exc = s.getNewTemporaryVariable();
        s.addInstr(new ReceiveExceptionInstr(exc));

        // Compare and branch as necessary!
        Label uncaughtLabel = s.getNewLabel();
        Label caughtLabel = s.getNewLabel();
        if (exceptionList != null) {
            if (exceptionList instanceof ListNode) {
               for (Node excType : ((ListNode) exceptionList).childNodes()) {
                   outputExceptionCheck(s, build(excType, s), exc, caughtLabel);
               }
            } else { // splatnode, catch 
                outputExceptionCheck(s, build(((SplatNode)exceptionList).getValue(), s), exc, caughtLabel);
            }
        } else {
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
            Variable v = s.getNewTemporaryVariable();
            s.addInstr(new InheritanceSearchConstInstr(v, s.getCurrentModuleVariable(), "StandardError"));
            outputExceptionCheck(s, v, exc, caughtLabel);
        }

        // Uncaught exception -- build other rescue nodes or rethrow!
        s.addInstr(new LabelInstr(uncaughtLabel));
        if (rescueBodyNode.getOptRescueNode() != null) {
            buildRescueBodyInternal(s, rescueBodyNode.getOptRescueNode(), rv, endLabel);
        } else {
            s.addInstr(new ThrowExceptionInstr(exc));
        }

        // Caught exception case -- build rescue body
        s.addInstr(new LabelInstr(caughtLabel));
        Node realBody = skipOverNewlines(s, rescueBodyNode.getBodyNode());
        Operand x = build(realBody, s);
        if (x != U_NIL) { // can be U_NIL if the rescue block has an explicit return
            // Restore "$!"
            RescueBlockInfo rbi = _rescueBlockStack.peek();
            s.addInstr(new PutGlobalVarInstr("$!", rbi.savedExceptionVariable));

            // Set up node return value 'rv'
            s.addInstr(new CopyInstr(rv, x));

            // If we dont have a matching ensure block, jump to the end of the rescue block.
            // If we have a match, jump to that ensure block.  On return, jump to the end of the rescue block.
            if (_ensureBlockStack.empty()) {
                s.addInstr(new JumpInstr(endLabel));
            } else {
                EnsureBlockInfo ebi = _ensureBlockStack.peek();
                if (rbi.rescueNode == ebi.matchingRescueNode) {
                    s.addInstr(new SetReturnAddressInstr(ebi.returnAddr, endLabel));
                    s.addInstr(new JumpInstr(ebi.start));
                } else {
                    s.addInstr(new JumpInstr(endLabel));
                }
            }
        }
    }

    public Operand buildRetry(Node node, IRScope s) {
        // JRuby only supports retry when present in rescue blocks!
        // 1.9 doesn't support retry anywhere else.
        
        // Jump back to the innermost rescue block
        // We either find it, or we add code to throw a runtime exception
        if (_rescueBlockStack.empty()) {
            s.addInstr(new ThrowExceptionInstr(IRException.RETRY_LocalJumpError));
        } else {
            s.addInstr(new ThreadPollInstr(true));
            // Restore $! and jump back to the entry of the rescue block
            RescueBlockInfo rbi = _rescueBlockStack.peek();
            s.addInstr(new PutGlobalVarInstr("$!", rbi.savedExceptionVariable));
            s.addInstr(new JumpInstr(rbi.entryLabel));
            // Retries effectively create a loop
            s.setHasLoopsFlag(true);
        }
        return manager.getNil();
    }

    public Operand buildReturn(ReturnNode returnNode, IRScope s) {
        Operand retVal = (returnNode.getValueNode() == null) ? manager.getNil() : build(returnNode.getValueNode(), s);

        // Before we return, 
        // - have to go execute all the ensure blocks if there are any.
        //   this code also takes care of resetting "$!"
        // - if we dont have any ensure blocks, we have to clear "$!"
        if (!_ensureBlockStack.empty()) EnsureBlockInfo.emitJumpChain(s, _ensureBlockStack, null);
        else if (!_rescueBlockStack.empty()) s.addInstr(new PutGlobalVarInstr("$!", manager.getNil()));

        if (s instanceof IRClosure) {
            // If 'm' is a block scope, a return returns from the closest enclosing method.
            // If this happens to be a module body, the runtime throws a local jump error if
            // the closure is a proc.  If the closure is a lambda, then this is just a normal
            // return and the static methodToReturnFrom value is ignored 
            s.addInstr(new ReturnInstr(retVal, s.getNearestMethod()));
        } else if (s.isModuleBody()) {
            IRMethod sm = s.getNearestMethod();

            // Cannot return from top-level module bodies!
            if (sm == null) s.addInstr(new ThrowExceptionInstr(IRException.RETURN_LocalJumpError));
            else s.addInstr(new ReturnInstr(retVal, sm));
        } else {
            s.addInstr(new ReturnInstr(retVal));
        }

        // The value of the return itself in the containing expression can never be used because of control-flow reasons.
        // The expression that uses this result can never be executed beyond the return and hence the value itself is just
        // a placeholder operand. 
        return UnexecutableNil.U_NIL;
    }

    public IREvalScript buildEvalRoot(StaticScope staticScope, IRScope containingScope, String file, int lineNumber, RootNode rootNode) {
        // Top-level script!
        IREvalScript script = new IREvalScript(manager, containingScope, file, lineNumber, staticScope);

        // Debug info: record line number
        script.addInstr(new LineNumberInstr(script, lineNumber));

        // Set %current_scope = <current-scope>
        // Set %current_module = <current-module>
        script.addInstr(new CopyInstr(script.getCurrentScopeVariable(), new CurrentScope(script)));
        script.addInstr(new CopyInstr(script.getCurrentModuleVariable(), new CurrentModule(script)));
        // Build IR for the tree and return the result of the expression tree
        Operand rval = rootNode.getBodyNode() == null ? manager.getNil() : build(rootNode.getBodyNode(), script);
        script.addInstr(new ClosureReturnInstr(rval));

        return script;
    }

    public IRScope buildRoot(RootNode rootNode) {
        String file = rootNode.getPosition().getFile();
        StaticScope staticScope = rootNode.getStaticScope();

        // Top-level script!
        IRScriptBody script = new IRScriptBody(manager, "__file__", file, staticScope);
        script.addInstr(new ReceiveSelfInstr(script.getSelf()));
        // Set %current_scope = <current-scope>
        // Set %current_module = <current-module>
        script.addInstr(new CopyInstr(script.getCurrentScopeVariable(), new CurrentScope(script)));
        script.addInstr(new CopyInstr(script.getCurrentModuleVariable(), new CurrentModule(script)));

        // Build IR for the tree and return the result of the expression tree
        script.addInstr(new ReturnInstr(build(rootNode.getBodyNode(), script)));

        return script;
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
        Variable ret = s.getNewTemporaryVariable();
        if ((s instanceof IRMethod) && (s.getLexicalParent() instanceof IRClassBody)) {
            IRMethod m = (IRMethod)s;
            if (m.isInstanceMethod) {
                s.addInstr(new InstanceSuperInstr(ret, s.getCurrentModuleVariable(), new MethAddr(s.getName()), args, block));
            } else {
                s.addInstr(new ClassSuperInstr(ret, s.getCurrentModuleVariable(), new MethAddr(s.getName()), args, block));
            }
        } else {
            // We dont always know the method name we are going to be invoking if the super occurs in a closure.
            // This is because the super can be part of a block that will be used by 'define_method' to define
            // a new method.  In that case, the method called by super will be determined by the 'name' argument
            // to 'define_method'.
            s.addInstr(new UnresolvedSuperInstr(ret, getSelf(s), args, block));
        }
        return ret;
    }

    public Operand buildSuper(SuperNode superNode, IRScope s) {
        if (s.isModuleBody()) return buildSuperInScriptBody(s);
        
        List<Operand> args = setupCallArgs(superNode.getArgsNode(), s);
        Operand  block = setupCallClosure(superNode.getIterNode(), s);
        if (block == null) block = s.getImplicitBlockArg();
        return buildSuperInstr(s, block, args.toArray(new Operand[args.size()]));
    }
    
    private Operand buildSuperInScriptBody(IRScope s) {
        Variable ret = s.getNewTemporaryVariable();
        s.addInstr(new UnresolvedSuperInstr(ret, getSelf(s), NO_ARGS, null));
        return ret;
    }

    public Operand buildSValue(SValueNode node, IRScope s) {
        // SSS FIXME: Required? Verify with Tom/Charlie
        return copyAndReturnValue(s, new SValue(build(node.getValue(), s)));
    }

    public Operand buildSymbol(SymbolNode node, IRScope s) {
        // SSS: Since symbols are interned objects, no need to copyAndReturnValue(...)
        return new Symbol(node.getName());
    }    

    // ENEBO: This is it's own instruction, but an older note pointed out we
    // could make this an ordinary method and then depend on inlining.
    public Operand buildToAry(ToAryNode node, IRScope s) {
        Operand array = build(node.getValue(), s);
        Variable result = s.getNewTemporaryVariable();
        s.addInstr(new ToAryInstr(result, array, manager.getFalse()));
        return result;
    }

    public Operand buildTrue(Node node, IRScope s) {
        return manager.getTrue(); 
    }

    public Operand buildUndef(Node node, IRScope s) {
        Operand methName = build(((UndefNode) node).getName(), s);
        Variable result = s.getNewTemporaryVariable();
        s.addInstr(new UndefMethodInstr(result, methName));
        return result;        
    }

    private Operand buildConditionalLoop(IRScope s, Node conditionNode, 
            Node bodyNode, boolean isWhile, boolean isLoopHeadCondition) {
        if (isLoopHeadCondition && 
                ((isWhile && conditionNode.getNodeType().alwaysFalse()) || 
                (!isWhile && conditionNode.getNodeType().alwaysTrue()))) {
            // we won't enter the loop -- just build the condition node
            build(conditionNode, s);
            return manager.getNil();
        } else {
            IRLoop loop = new IRLoop(s, getCurrentLoop());
            Variable loopResult = loop.loopResult;
            Label setupResultLabel = s.getNewLabel();

            // Push new loop
            loopStack.push(loop);

            // End of iteration jumps here
            s.addInstr(new LabelInstr(loop.loopStartLabel));
            if (isLoopHeadCondition) {
                Operand cv = build(conditionNode, s);
                s.addInstr(BEQInstr.create(cv, isWhile ? manager.getFalse() : manager.getTrue(), setupResultLabel));
            }

            // Redo jumps here 
            s.addInstr(new LabelInstr(loop.iterStartLabel));

            // Thread poll at start of iteration -- ensures that redos and nexts run one thread-poll per iteration
            s.addInstr(new ThreadPollInstr(true));

            // Build body
            if (bodyNode != null) build(bodyNode, s);

            // Next jumps here
            s.addInstr(new LabelInstr(loop.iterEndLabel));
            if (isLoopHeadCondition) {
                s.addInstr(new JumpInstr(loop.loopStartLabel));
            } else {
                Operand cv = build(conditionNode, s);
                s.addInstr(BEQInstr.create(cv, isWhile ? manager.getTrue() : manager.getFalse(), loop.iterStartLabel));
            }

            // Loop result -- nil always
            s.addInstr(new LabelInstr(setupResultLabel));
            s.addInstr(new CopyInstr(loopResult, manager.getNil()));

            // Loop end -- breaks jump here bypassing the result set up above
            s.addInstr(new LabelInstr(loop.loopEndLabel));

            // Done with loop
            loopStack.pop();

            return loopResult;
        }
    }

    public Operand buildUntil(final UntilNode untilNode, IRScope s) {
        return buildConditionalLoop(s, untilNode.getConditionNode(), untilNode.getBodyNode(), false, untilNode.evaluateAtStart());
    }

    public Operand buildVAlias(Node node, IRScope s) {
        VAliasNode valiasNode = (VAliasNode) node;
        s.addInstr(new GVarAliasInstr(new StringLiteral(valiasNode.getNewName()), new StringLiteral(valiasNode.getOldName())));
        return manager.getNil();
    }

    public Operand buildVCall(VCallNode node, IRScope s) {
        Variable callResult = s.getNewTemporaryVariable();
        Instr    callInstr  = CallInstr.create(CallType.VARIABLE, callResult, new MethAddr(node.getName()), getSelf(s), NO_ARGS, null);
        s.addInstr(callInstr);
        return callResult;
    }

    public Operand buildWhile(final WhileNode whileNode, IRScope s) {
        return buildConditionalLoop(s, whileNode.getConditionNode(), whileNode.getBodyNode(), true, whileNode.evaluateAtStart());
    }

    public Operand buildXStr(XStrNode node, IRScope s) {
        return copyAndReturnValue(s, new BacktickString(new StringLiteral(node.getValue())));
    }

    public Operand buildYield(YieldNode node, IRScope s) {
        Variable ret = s.getNewTemporaryVariable();
        s.addInstr(new YieldInstr(ret, s.getImplicitBlockArg(), build(node.getArgsNode(), s), node.getExpandArguments()));
        return ret;
    }

    public Operand buildZArray(Node node, IRScope s) {
       return copyAndReturnValue(s, new Array());
    }

/**
    private Operand[] getZSuperArgs(IRScope s) {
        if (s instanceof IRMethod) return ((IRMethod)s).getCallArgs();

        Operand[] sArgs = s.getNearestMethod().getCallArgs();

        // Update args to make them accessible at a different depth 
        int n = ((IRClosure)s).getNestingDepth();
        for (int i = 0; i < sArgs.length; i++) {
            Operand arg = sArgs[i];
            sArgs[i] = (arg instanceof Splat) ? new Splat(((LocalVariable)((Splat)arg).getArray()).cloneForDepth(n)) : ((LocalVariable)arg).cloneForDepth(n);
        }
        
        return sArgs;
    }
**/

    public Operand buildZSuper(ZSuperNode zsuperNode, IRScope s) {
        if (s.isModuleBody()) return buildSuperInScriptBody(s);

        Operand block = setupCallClosure(zsuperNode.getIterNode(), s);
        if (block == null) block = s.getImplicitBlockArg();

        if (s instanceof IRMethod) {
            Operand[] args = ((IRMethod)s).getCallArgs();
            return buildSuperInstr(s, block, args);
        } else {
            // If we are in a block, we cannot make any assumptions about what args
            // the super instr is going to get -- if there were no 'define_method'
            // for defining methods, we could guarantee that the super is going to
            // receive args from the nearest method the block is embedded in.  But,
            // in the presence of 'define_method', all bets are off.
            Variable ret = s.getNewTemporaryVariable();
            s.addInstr(new ZSuperInstr(ret, getSelf(s), block));
            return ret;
        }
    }
}
