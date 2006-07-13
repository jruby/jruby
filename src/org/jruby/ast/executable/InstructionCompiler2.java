package org.jruby.ast.executable;

import java.util.Iterator;

import org.jruby.IRuby;
import org.jruby.ast.AliasNode;
import org.jruby.ast.AndNode;
import org.jruby.ast.ArgsCatNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BignumNode;
import org.jruby.ast.BlockArgNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.BreakNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.CaseNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.ClassVarNode;
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
import org.jruby.ast.FalseNode;
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
import org.jruby.ast.NotNode;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.OpAsgnAndNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.OptNNode;
import org.jruby.ast.OrNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.RedoNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.RetryNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SValueNode;
import org.jruby.ast.ScopeNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SuperNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.ToAryNode;
import org.jruby.ast.TrueNode;
import org.jruby.ast.UndefNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.WhenNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ZArrayNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.evaluator.Instruction;
import org.jruby.runtime.callback.Callback;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class InstructionCompiler2 implements NodeVisitor {
    private static final boolean EXPERIMENTAL_SOURCING = true; 
    ClassWriter cv;

    MethodVisitor mv;

    public InstructionCompiler2(String classname, String sourceName) {
        cv = new ClassWriter(true);

        cv.visit(Opcodes.V1_2, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
                classname, null, "java/lang/Object", null);
        cv.visitSource(sourceName, null);

        mv = cv.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>",
                "()V");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    public void newMethod(String methodName, String signature, Node node) {
        mv = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                methodName, signature, null, null);
        mv.visitCode();

        node.accept(this);

        mv.visitMaxs(10, 10);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitEnd();
    }
    
    public void defineModuleFunction(IRuby runtime, String module, String name, Callback callback) {
        runtime.getModule("Kernel").definePublicModuleFunction(name, callback);
    }

    public Instruction visitAliasNode(AliasNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitAndNode(AndNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitArgsNode(ArgsNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitArgsCatNode(ArgsCatNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitArrayNode(ArrayNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitBackRefNode(BackRefNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitBeginNode(BeginNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitBignumNode(BignumNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitBlockArgNode(BlockArgNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitBlockNode(BlockNode iVisited) {
        for (Iterator iter = iVisited.childNodes().iterator(); iter.hasNext();) {
            Node n = (Node)iter.next();
            
            n.accept(this);
            
            // TODO need to not keep pushing down more and more values
        }
        
        return null;
    }

    public Instruction visitBlockPassNode(BlockPassNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitBreakNode(BreakNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitConstDeclNode(ConstDeclNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitClassVarDeclNode(ClassVarDeclNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitClassVarNode(ClassVarNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    private void setupArgs(Node node) {
        if (EXPERIMENTAL_SOURCING) lineNumber(node);
        if (node == null) {
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitTypeInsn(Opcodes.ANEWARRAY, "org/jruby/runtime/builtin/IRubyObject");
        } else if (node instanceof ArrayNode) {
            int count = ((ArrayNode)node).size();
            mv.visitLdcInsn(new Integer(count));
            mv.visitTypeInsn(Opcodes.ANEWARRAY, "org/jruby/runtime/builtin/IRubyObject");
            
            int i = 0;
            for (Iterator iter = ((ArrayNode) node).iterator(); iter.hasNext();) {
                final Node next = (Node) iter.next();
                
                mv.visitInsn(Opcodes.DUP);
                mv.visitLdcInsn(new Integer(i));
                
//              implement splatnode logic to make appropriate count
                if (next instanceof SplatNode) {
                    //count += getSplatNodeSize((SplatNode)next) - 1;
                } else {
                    next.accept(this);
                }
                
                mv.visitInsn(Opcodes.AASTORE);
            }
        }
    }

    public Instruction visitCallNode(CallNode iVisited) {
        if (EXPERIMENTAL_SOURCING) lineNumber(iVisited);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/jruby/runtime/builtin/IRubyObject", "getRuntime", "()Lorg/jruby/IRuby;");
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/jruby/IRuby", "getCurrentContext", "()Lorg/jruby/runtime/ThreadContext;");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/runtime/ThreadContext", "beginCallArgs", "()V");
        
        // TODO: try finally around this
        // recv
        iVisited.getReceiverNode().accept(this);
        
        
        // args
        setupArgs(iVisited.getArgsNode());

        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/jruby/runtime/builtin/IRubyObject", "getRuntime", "()Lorg/jruby/IRuby;");
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/jruby/IRuby", "getCurrentContext", "()Lorg/jruby/runtime/ThreadContext;");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/runtime/ThreadContext", "endCallArgs", "()V");
        
        // dup recv for CallType check
        mv.visitInsn(Opcodes.SWAP);
        mv.visitInsn(Opcodes.DUP);
        
        // compare recv with Frame.getSelf
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/jruby/runtime/builtin/IRubyObject", "getRuntime", "()Lorg/jruby/IRuby;");
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/jruby/IRuby", "getCurrentContext", "()Lorg/jruby/runtime/ThreadContext;");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/runtime/ThreadContext", "getCurrentFrame", "()Lorg/jruby/runtime/Frame;");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/runtime/Frame", "getSelf", "()Lorg/jruby/runtime/builtin/IRubyObject;");
        
        Label l1 = new Label();
        Label l2 = new Label();
        
        // choose CallType appropriately
        mv.visitJumpInsn(Opcodes.IF_ACMPEQ, l1);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "org/jruby/runtime/CallType", "NORMAL", "Lorg/jruby/runtime/CallType;");
        mv.visitJumpInsn(Opcodes.GOTO, l2);
        mv.visitLabel(l1);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "org/jruby/runtime/CallType", "VARIABLE", "Lorg/jruby/runtime/CallType;");
        mv.visitLabel(l2);
        
        // swap recv and args on stack
        mv.visitInsn(Opcodes.SWAP);
        
        // name to callMethod
        mv.visitLdcInsn(iVisited.getName());
        
        // put recv first, name second, under args and calltype
        mv.visitInsn(Opcodes.DUP2_X2);
        
        // pop name and recv on top
        mv.visitInsn(Opcodes.POP2);
        
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/jruby/runtime/builtin/IRubyObject", "callMethod", "(Ljava/lang/String;[Lorg/jruby/runtime/builtin/IRubyObject;Lorg/jruby/runtime/CallType;)Lorg/jruby/runtime/builtin/IRubyObject;");
        
        return null;
    }

    public Instruction visitCaseNode(CaseNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitClassNode(ClassNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitColon2Node(Colon2Node iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitColon3Node(Colon3Node iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitConstNode(ConstNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitDAsgnNode(DAsgnNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitDRegxNode(DRegexpNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitDStrNode(DStrNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitDSymbolNode(DSymbolNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitDVarNode(DVarNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitDXStrNode(DXStrNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitDefinedNode(DefinedNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }
    
    private ArgsNode args;

    public Instruction visitDefnNode(DefnNode iVisited) {
        // TODO: build arg list based on number of args, optionals, etc
        mv = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                iVisited.getName(), "(Lorg/jruby/runtime/builtin/IRubyObject;Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;", null, null);
        mv.visitCode();
        
        // TODO: this probably isn't always an ArgsNode
        args = (ArgsNode)iVisited.getArgsNode();
        
        iVisited.getBodyNode().accept(this);

        mv.visitMaxs(1, 1); // automatically calculated by ASM
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitEnd();
        
        return null;
    }

    public Instruction visitDefsNode(DefsNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitDotNode(DotNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitEnsureNode(EnsureNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitEvStrNode(EvStrNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitFCallNode(FCallNode iVisited) {
        if (EXPERIMENTAL_SOURCING) lineNumber(iVisited);
        // TODO: try finally around this
        // recv
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/jruby/runtime/builtin/IRubyObject", "getRuntime", "()Lorg/jruby/IRuby;");
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/jruby/IRuby", "getCurrentContext", "()Lorg/jruby/runtime/ThreadContext;");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/runtime/ThreadContext", "getCurrentFrame", "()Lorg/jruby/runtime/Frame;");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/runtime/Frame", "getSelf", "()Lorg/jruby/runtime/builtin/IRubyObject;");

        mv.visitLdcInsn(iVisited.getName());
        
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/jruby/runtime/builtin/IRubyObject", "getRuntime", "()Lorg/jruby/IRuby;");
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/jruby/IRuby", "getCurrentContext", "()Lorg/jruby/runtime/ThreadContext;");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/runtime/ThreadContext", "beginCallArgs", "()V");
        
        // args
        setupArgs(iVisited.getArgsNode());

        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/jruby/runtime/builtin/IRubyObject", "getRuntime", "()Lorg/jruby/IRuby;");
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/jruby/IRuby", "getCurrentContext", "()Lorg/jruby/runtime/ThreadContext;");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/runtime/ThreadContext", "endCallArgs", "()V");
        
        mv.visitFieldInsn(Opcodes.GETSTATIC, "org/jruby/runtime/CallType", "FUNCTIONAL", "Lorg/jruby/runtime/CallType;");
        
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/jruby/runtime/builtin/IRubyObject", "callMethod", "(Ljava/lang/String;[Lorg/jruby/runtime/builtin/IRubyObject;Lorg/jruby/runtime/CallType;)Lorg/jruby/runtime/builtin/IRubyObject;");
        
        return null;
    }

    public Instruction visitFalseNode(FalseNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitFixnumNode(FixnumNode iVisited) {
        if (EXPERIMENTAL_SOURCING) lineNumber(iVisited);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/jruby/runtime/builtin/IRubyObject", "getRuntime", "()Lorg/jruby/IRuby;");
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/jruby/IRuby", "getCurrentContext", "()Lorg/jruby/runtime/ThreadContext;");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                "org/jruby/runtime/ThreadContext", "getRuntime",
                "()Lorg/jruby/IRuby;");
        mv.visitLdcInsn(new Long(iVisited.getValue()));
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/jruby/IRuby",
                "newFixnum", "(J)Lorg/jruby/RubyFixnum;");

        return null;
    }

    public Instruction visitFlipNode(FlipNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitFloatNode(FloatNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitForNode(ForNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitGlobalVarNode(GlobalVarNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitHashNode(HashNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitInstAsgnNode(InstAsgnNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitInstVarNode(InstVarNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitIfNode(IfNode iVisited) {
        if (EXPERIMENTAL_SOURCING) lineNumber(iVisited);
        Label afterJmp = new Label();
        Label falseJmp = new Label();

        // visit condition
        iVisited.getCondition().accept(this);

        // call isTrue on the result
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                "org/jruby/runtime/builtin/IRubyObject", "isTrue", "()Z");
        mv.visitJumpInsn(Opcodes.IFEQ, falseJmp); // EQ == 0 (i.e. false)
        iVisited.getThenBody().accept(this);
        mv.visitJumpInsn(Opcodes.GOTO, afterJmp);

        mv.visitLabel(falseJmp);
        iVisited.getElseBody().accept(this);

        mv.visitLabel(afterJmp);

        return null;
    }

    public Instruction visitIterNode(IterNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitLocalAsgnNode(LocalAsgnNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitLocalVarNode(LocalVarNode iVisited) {
        if (EXPERIMENTAL_SOURCING) lineNumber(iVisited);
        // check if it's an argument
        int index = iVisited.getCount();
        
        if ((index - 2) < args.getArgsCount()) {
            // load from the incoming params
            // index is 2-based, and our zero is runtime
            mv.visitVarInsn(Opcodes.ALOAD, index - 1);
        } else {
            // load from scoped local vars (probably not right? local vars should get compiled
            mv.visitVarInsn(Opcodes.ALOAD, 0); // load ThreadContext
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/jruby/runtime/builtin/IRubyObject", "getRuntime", "()Lorg/jruby/IRuby;");
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/jruby/IRuby", "getCurrentContext", "()Lorg/jruby/runtime/ThreadContext;");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    "org/jruby/runtime/ThreadContext", "getCurrentScope",
                    "()Lorg/jruby/runtime/Scope;");
            mv.visitLdcInsn(new Integer(iVisited.getCount()));
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/runtime/Scope",
                    "getValue", "(I)Lorg/jruby/runtime/builtin/IRubyObject;");
        }

        return null;
    }
    
    int lastLine = 0;

    private void lineNumber(Node iVisited) {   
        if (lastLine == (lastLine = iVisited.getPosition().getEndLine())) return; // did not change lines for this node, don't bother relabeling
        
        Label l = new Label();
        mv.visitLabel(l);
        mv.visitLineNumber(iVisited.getPosition().getEndLine(), l);
    }

    public Instruction visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitMatch2Node(Match2Node iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitMatch3Node(Match3Node iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitMatchNode(MatchNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitModuleNode(ModuleNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitNewlineNode(NewlineNode iVisited) {
        // TODO: add trace call

        iVisited.getNextNode().accept(this);

        return null;
    }

    public Instruction visitNextNode(NextNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitNilNode(NilNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitNotNode(NotNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitNthRefNode(NthRefNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitOpElementAsgnNode(OpElementAsgnNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitOpAsgnNode(OpAsgnNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitOpAsgnAndNode(OpAsgnAndNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitOpAsgnOrNode(OpAsgnOrNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitOptNNode(OptNNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitOrNode(OrNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitPostExeNode(PostExeNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitRedoNode(RedoNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitRegexpNode(RegexpNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitRescueBodyNode(RescueBodyNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitRescueNode(RescueNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitRetryNode(RetryNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitReturnNode(ReturnNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitSClassNode(SClassNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitScopeNode(ScopeNode iVisited) {
        iVisited.getBodyNode().accept(this);
        
        return null;
    }

    public Instruction visitSelfNode(SelfNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitSplatNode(SplatNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitStrNode(StrNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitSuperNode(SuperNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitSValueNode(SValueNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitSymbolNode(SymbolNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitToAryNode(ToAryNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitTrueNode(TrueNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitUndefNode(UndefNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitUntilNode(UntilNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitVAliasNode(VAliasNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitVCallNode(VCallNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitWhenNode(WhenNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitWhileNode(WhileNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitXStrNode(XStrNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitYieldNode(YieldNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitZArrayNode(ZArrayNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public Instruction visitZSuperNode(ZSuperNode iVisited) {
        // TODO Auto-generated method stub
        return null;
    }

    public ClassWriter getClassWriter() {
        return cv;
    }
    
    public void setClassWriter(ClassWriter cv) {
        this.cv = cv;
    }
}
