/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2004-2006 Charles O Nutter <headius@headius.com>
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
import org.objectweb.asm.Type;

public class InstructionCompiler2 implements NodeVisitor {
    private static final String IRUBY = "org/jruby/IRuby";

    private static final String IRUBYOBJECT = "org/jruby/runtime/builtin/IRubyObject";

    private static final String FRAME = "org/jruby/runtime/Frame";

    private static final String THREADCONTEXT = "org/jruby/runtime/ThreadContext";

    private static final boolean EXPERIMENTAL_SOURCING = true; 
    
    private ArgsNode args;
    private ClassWriter cv;
    private MethodVisitor mv;
    
    private int lastLine = 0;

    private boolean tcLoaded;

    private boolean runtimeLoaded;
    
    public class NotCompilableException extends RuntimeException {
        private static final long serialVersionUID = 8481162670192366492L;

        public NotCompilableException(String message) {
            super(message);
        }
    }

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

        mv.visitMaxs(1, 1); // bogus values, ASM will auto-calculate
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitEnd();
        
        // clean up state
        tcLoaded = false;
        runtimeLoaded = false;
    }
    
    public void defineModuleFunction(IRuby runtime, String module, String name, Callback callback) {
        runtime.getModule("Kernel").definePublicModuleFunction(name, callback);
    }

    // finished
    public Instruction visitAliasNode(AliasNode iVisited) {
        lineNumber(iVisited);
        loadThreadContext();
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, THREADCONTEXT, "getRubyClass", "()Lorg/jruby/RubyModule;");
        mv.visitInsn(Opcodes.DUP);
        mv.visitInsn(Opcodes.DUP);
        Label l1 = new Label();
        mv.visitJumpInsn(Opcodes.IFNONNULL, l1);
        loadRuntime();
        mv.visitLdcInsn("no class to make alias");
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBY, "newTypeError", "(Ljava/lang/String;)Lorg/jruby/exceptions/RaiseException;");
        mv.visitInsn(Opcodes.ATHROW);
        
        mv.visitLabel(l1);
        mv.visitLdcInsn(iVisited.getNewName());
        mv.visitLdcInsn(iVisited.getOldName());
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/RubyModule", "defineAlias", "(Ljava/lang/String;Ljava/lang/String;)V");
        mv.visitLdcInsn("method_added");
        loadRuntime();
        mv.visitLdcInsn(iVisited.getNewName());
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBY, "newSymbol", "()Lorg/jruby/RubySymbol;");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/RubyModule", "callMethod", "(Ljava/lang/String;Lorg/jruby/RubySymbol;)Lorg/jruby/runtime/builtin/IRubyObject;");
        
        return null;
    }

    public Instruction visitAndNode(AndNode iVisited) {
        lineNumber(iVisited);
        iVisited.getFirstNode().accept(this);
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBYOBJECT, "isTrue", "()B");
        Label l1 = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, l1);
        mv.visitInsn(Opcodes.POP); // remove first node's result
        iVisited.getSecondNode().accept(this);
        mv.visitLabel(l1);
        
        return null;
    }

    public Instruction visitArgsNode(ArgsNode iVisited) {
        // TODO: this node should never be visited, but it may simplify argument processing if it were
        return null;
    }

    public Instruction visitArgsCatNode(ArgsCatNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }
    
    private interface ValueCallback {
        public void putValueOnStack(Object sourceArray, int index);
    }

    public Instruction visitArrayNode(ArrayNode iVisited) {
        lineNumber(iVisited);
        loadRuntime();
        
        ValueCallback callback = new ValueCallback() {
            public void putValueOnStack(Object sourceArray, int index) {
                Node node = (Node)((Object[])sourceArray)[index];
                node.accept(InstructionCompiler2.this);
            }
        };
        buildObjectArray(IRUBYOBJECT, iVisited.childNodes().toArray(), callback);

        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBY, "newArray", "([Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/RubyArray;");
        
        return null;
    }

    private void buildObjectArray(String type, Object[] sourceArray, ValueCallback callback) {
        mv.visitLdcInsn(new Integer(sourceArray.length));
        mv.visitTypeInsn(Opcodes.ANEWARRAY, type);
        
        for (int i = 0; i < sourceArray.length; i++) {
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(new Integer(i));
            
            callback.putValueOnStack(sourceArray, i);
            
            mv.visitInsn(Opcodes.AASTORE);
            i++;
        }
    }

    private void buildPrimitiveArray(Type type, Object sourceArray, int length, ValueCallback callback) {
        mv.visitLdcInsn(new Integer(length));
        mv.visitTypeInsn(Opcodes.NEWARRAY, type.getDescriptor());
        
        for (int i = 0; i < length; i++) {
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(new Integer(i));
            
            callback.putValueOnStack(sourceArray, i);
            
            if (type.equals(Type.BYTE_TYPE) || type.equals(Type.BOOLEAN_TYPE)) {
                mv.visitInsn(Opcodes.BASTORE);
            } else if (type.equals(Type.CHAR_TYPE)) {
                mv.visitInsn(Opcodes.CASTORE);
            } else if (type.equals(Type.INT_TYPE)) {
                mv.visitInsn(Opcodes.IASTORE);
            } else if (type.equals(Type.LONG_TYPE)) {
                mv.visitInsn(Opcodes.LASTORE);
            } else if (type.equals(Type.FLOAT_TYPE)) {
                mv.visitInsn(Opcodes.FASTORE);
            } else if (type.equals(Type.DOUBLE_TYPE)) {
                mv.visitInsn(Opcodes.DASTORE);
            }
            i++;
        }
    }

    public Instruction visitBackRefNode(BackRefNode iVisited) {
        loadThreadContext();
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, THREADCONTEXT, "getBackref", "()Lorg/jruby/runtime/builtin/IRubyObject;");
        Label l1 = new Label();
        Label l2 = new Label();
        Label l3 = new Label();
        Label l4 = new Label();
        Label l5 = new Label();
        Label lafter = new Label();
        mv.visitLookupSwitchInsn(lafter, new int[] { '~', '&', '`', '\\', '+' }, new Label[] { l1, l2, l3, l4, l5 });
        mv.visitLabel(l1);
        // ~ do nothing
        mv.visitJumpInsn(Opcodes.GOTO, lafter);
        
        mv.visitLabel(l2);
        // &
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/jruby/RubyRegexp", "last_match", "(Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;");
        mv.visitJumpInsn(Opcodes.GOTO, lafter);
        
        mv.visitLabel(l3);
        // `
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/jruby/RubyRegexp", "match_pre", "(Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;");
        mv.visitJumpInsn(Opcodes.GOTO, lafter);
        
        mv.visitLabel(l4);
        // \
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/jruby/RubyRegexp", "match_post", "(Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;");
        mv.visitJumpInsn(Opcodes.GOTO, lafter);
        
        mv.visitLabel(l5);
        // +
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/jruby/RubyRegexp", "match_last", "(Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;");

        mv.visitLabel(lafter);
        
        return null;
    }

    public Instruction visitBeginNode(BeginNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitBignumNode(BignumNode iVisited) {
        // FIXME: storing the bignum as a string is not as efficient as storing as a byte array
        lineNumber(iVisited);
        loadRuntime();
        mv.visitTypeInsn(Opcodes.NEW, "java/math/BigInteger");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn(iVisited.getValue().toString());
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/math/BigInteger", "<init>", "(Ljava/lang/String;)V");
        
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/jruby/RubyBignum", "newBignum", "(Lorg/jruby/IRuby;Ljava/math/BigInteger;)Lorg/jruby/RubyBignum;");
        
        return null;
    }

    public Instruction visitBlockArgNode(BlockArgNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitBlockNode(BlockNode iVisited) {
        lineNumber(iVisited);
        for (Iterator iter = iVisited.childNodes().iterator(); iter.hasNext();) {
            Node n = (Node)iter.next();
            
            n.accept(this);
            
            if (iter.hasNext()) {
                // clear result from previous line
                mv.visitInsn(Opcodes.POP);
            }
        }
        
        return null;
    }

    public Instruction visitBlockPassNode(BlockPassNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitBreakNode(BreakNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitConstDeclNode(ConstDeclNode iVisited) {
        // TODO untested
        lineNumber(iVisited);
        
        iVisited.getValueNode().accept(this);
        
        if (iVisited.getPathNode() != null) {
            iVisited.getPathNode().accept(this);
        } else {
            loadThreadContext();
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, THREADCONTEXT, "getRubyClass", "()Lorg/jruby/RubyModule;");
            Label l1 = new Label();
            
            mv.visitJumpInsn(Opcodes.IFNONNULL, l1);
            
            loadRuntime();
            mv.visitLdcInsn("no class/module to define constant");
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBY, "newTypeError", "(Ljava/lang/String;)Lorg/jruby/exceptions/RaiseException;");
            mv.visitInsn(Opcodes.ATHROW);
            
            mv.visitLabel(l1);
            
            loadThreadContext();
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, THREADCONTEXT, "peekCRef", "()Lorg/jruby/util/collections/SinglyLinkedList;");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, THREADCONTEXT, "getValue", "()Ljava/lang/Object;");
        }

        mv.visitTypeInsn(Opcodes.CHECKCAST, "org/jruby/RubyModule");
        
        mv.visitInsn(Opcodes.SWAP);
        mv.visitLdcInsn(iVisited.getName());
        mv.visitInsn(Opcodes.SWAP);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/RubyModule", "setConstant", "(Ljava/lang/String;Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;");
        
        return null;
    }

    public Instruction visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
        // TODO untested
        lineNumber(iVisited);
        
        iVisited.getValueNode().accept(this);
                
        loadThreadContext();
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, THREADCONTEXT, "peekCRef", "()Lorg/jruby/util/collections/SinglyLinkedList;");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/util/collections/SinglyLinkedList", "getValue", "()Ljava/lang/Object;");
        mv.visitTypeInsn(Opcodes.CHECKCAST, "org/jruby/RubyModule");
        mv.visitInsn(Opcodes.DUP);
        
        Label l1 = new Label();
        Label l2 = new Label();
        mv.visitJumpInsn(Opcodes.IFNONNULL, l1);
        
        // null class
        mv.visitInsn(Opcodes.POP);
        loadThreadContext();
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, THREADCONTEXT, "getFrameSelf", "()Lorg/jruby/runtime/builtin/IRubyObject;");
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBYOBJECT, "getMetaClass", "()Lorg/jruby/RubyClass;");
        mv.visitJumpInsn(Opcodes.GOTO, l2);
        
        mv.visitLabel(l1);
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/RubyModule", "isSingleton", "()B");
        mv.visitJumpInsn(Opcodes.IFEQ, l2);
        
        mv.visitLdcInsn("__attached__");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/RubyObject", "getInstanceVariable", "(Ljava/lang/String;)Lorg/jruby/runtime/builtin/IRubyObject;");
        mv.visitTypeInsn(Opcodes.CHECKCAST, "org/jruby/RubyModule");
        
        mv.visitLabel(l2);
        
        mv.visitInsn(Opcodes.SWAP);
        mv.visitLdcInsn(iVisited.getName());
        mv.visitInsn(Opcodes.SWAP);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/RubyModule", "setClassVar", "(Ljava/lang/String;Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;");
        
        return null;
    }

    public Instruction visitClassVarDeclNode(ClassVarDeclNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitClassVarNode(ClassVarNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitCallNode(CallNode iVisited) {
        lineNumber(iVisited);
        loadThreadContext();
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, THREADCONTEXT, "beginCallArgs", "()V");
        
        // TODO: try finally around this
        // recv
        iVisited.getReceiverNode().accept(this);
        
        // args
        setupArgs(iVisited.getArgsNode());

        loadThreadContext();
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, THREADCONTEXT, "endCallArgs", "()V");
        
        // dup recv for CallType check
        mv.visitInsn(Opcodes.SWAP);
        mv.visitInsn(Opcodes.DUP);
        
        loadThreadContext();
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, THREADCONTEXT, "getFrameSelf", "()Lorg/jruby/runtime/builtin/IRubyObject;");
        
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
        
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBYOBJECT, "callMethod", "(Ljava/lang/String;[Lorg/jruby/runtime/builtin/IRubyObject;Lorg/jruby/runtime/CallType;)Lorg/jruby/runtime/builtin/IRubyObject;");
        
        return null;
    }

    public Instruction visitCaseNode(CaseNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitClassNode(ClassNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitColon2Node(Colon2Node iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitColon3Node(Colon3Node iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitConstNode(ConstNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitDAsgnNode(DAsgnNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitDRegxNode(DRegexpNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitDStrNode(DStrNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitDSymbolNode(DSymbolNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitDVarNode(DVarNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitDXStrNode(DXStrNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitDefinedNode(DefinedNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitDefnNode(DefnNode iVisited) {
        // TODO: build arg list based on number of args, optionals, etc
        mv = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                iVisited.getName(), "(Lorg/jruby/runtime/builtin/IRubyObject;Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;", null, null);
        mv.visitCode();
        
        // TODO: this probably isn't always an ArgsNode
        args = (ArgsNode)iVisited.getArgsNode();
        
        try {
            iVisited.getBodyNode().accept(this);
        } catch (NotCompilableException nce) {
            // TODO: recover somehow? build a pure eval method?
            throw nce;
        }

        mv.visitMaxs(1, 1); // automatically calculated by ASM
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitEnd();
        
        tcLoaded = false;
        runtimeLoaded = false;
        
        return null;
    }

    public Instruction visitDefsNode(DefsNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitDotNode(DotNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitEnsureNode(EnsureNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitEvStrNode(EvStrNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitFCallNode(FCallNode iVisited) {
        lineNumber(iVisited);
        loadThreadContext();
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, THREADCONTEXT, "getFrameSelf", "()Lorg/jruby/runtime/builtin/IRubyObject;");
        
        mv.visitLdcInsn(iVisited.getName());
        
        loadThreadContext();
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, THREADCONTEXT, "beginCallArgs", "()V");
        
        // args
        setupArgs(iVisited.getArgsNode());

        loadThreadContext();
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, THREADCONTEXT, "endCallArgs", "()V");
        
        mv.visitFieldInsn(Opcodes.GETSTATIC, "org/jruby/runtime/CallType", "FUNCTIONAL", "Lorg/jruby/runtime/CallType;");
        
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBYOBJECT, "callMethod", "(Ljava/lang/String;[Lorg/jruby/runtime/builtin/IRubyObject;Lorg/jruby/runtime/CallType;)Lorg/jruby/runtime/builtin/IRubyObject;");
        
        return null;
    }

    public Instruction visitFalseNode(FalseNode iVisited) {
        lineNumber(iVisited);
        loadRuntime();
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBY, "getFalse", "()Lorg/jruby/RubyBoolean;");
        
        return null;
    }

    public Instruction visitFixnumNode(FixnumNode iVisited) {
        lineNumber(iVisited);
        loadThreadContext();
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                THREADCONTEXT, "getRuntime",
                "()Lorg/jruby/IRuby;");
        mv.visitLdcInsn(new Long(iVisited.getValue()));
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBY,
                "newFixnum", "(J)Lorg/jruby/RubyFixnum;");

        return null;
    }

    public Instruction visitFlipNode(FlipNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitFloatNode(FloatNode iVisited) {
        lineNumber(iVisited);
        loadRuntime();
        mv.visitLdcInsn(new Double(iVisited.getValue()));
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/jruby/RubyFloat", "newFloat", "(Lorg/jruby/IRuby;D)Lorg/jruby/RubyFloat;");
        
        return null;
    }

    public Instruction visitForNode(ForNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitGlobalVarNode(GlobalVarNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitHashNode(HashNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitInstAsgnNode(InstAsgnNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitInstVarNode(InstVarNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitIfNode(IfNode iVisited) {
        lineNumber(iVisited);
        Label afterJmp = new Label();
        Label falseJmp = new Label();

        // visit condition
        iVisited.getCondition().accept(this);

        // call isTrue on the result
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                IRUBYOBJECT, "isTrue", "()Z");
        mv.visitJumpInsn(Opcodes.IFEQ, falseJmp); // EQ == 0 (i.e. false)
        iVisited.getThenBody().accept(this);
        mv.visitJumpInsn(Opcodes.GOTO, afterJmp);

        mv.visitLabel(falseJmp);
        iVisited.getElseBody().accept(this);

        mv.visitLabel(afterJmp);

        return null;
    }

    public Instruction visitIterNode(IterNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitLocalAsgnNode(LocalAsgnNode iVisited) {
        lineNumber(iVisited);
        
        iVisited.getValueNode().accept(this);
        mv.visitInsn(Opcodes.DUP);
        
        loadThreadContext();
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, THREADCONTEXT, "getFrameScope", "()Lorg/jruby/runtime/Scope;");
        mv.visitInsn(Opcodes.SWAP);
        mv.visitLdcInsn(new Integer(iVisited.getCount()));
        mv.visitInsn(Opcodes.SWAP);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/runtime/Scope", "setValue", "(ILorg/jruby/runtime/builtin/IRubyObject;)V");
        
        return null;
    }

    public Instruction visitLocalVarNode(LocalVarNode iVisited) {
        lineNumber(iVisited);
        // check if it's an argument
        int index = iVisited.getCount();
        
        if ((index - 2) < args.getArgsCount()) {
            // load from the incoming params
            // index is 2-based, and our zero is runtime
            mv.visitVarInsn(Opcodes.ALOAD, index - 1);
        } else {
            loadThreadContext();
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    THREADCONTEXT, "getFrameScope",
                    "()Lorg/jruby/runtime/Scope;");
            mv.visitLdcInsn(new Integer(iVisited.getCount()));
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/runtime/Scope",
                    "getValue", "(I)Lorg/jruby/runtime/builtin/IRubyObject;");
        }

        return null;
    }

    public Instruction visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitMatch2Node(Match2Node iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitMatch3Node(Match3Node iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitMatchNode(MatchNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitModuleNode(ModuleNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitNewlineNode(NewlineNode iVisited) {
        // TODO: add trace call

        iVisited.getNextNode().accept(this);

        return null;
    }

    public Instruction visitNextNode(NextNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitNilNode(NilNode iVisited) {
        lineNumber(iVisited);
        loadRuntime();
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBY, "getNil", "()Lorg/jruby/runtime/builtin/IRubyObject;");
        
        return null;
    }

    public Instruction visitNotNode(NotNode iVisited) {
        lineNumber(iVisited);
        loadRuntime();
        iVisited.getConditionNode().accept(this);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBYOBJECT, "isTrue", "()B");
        Label l1 = new Label();
        Label l2 = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, l1);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBY, "getFalse", "()Lorg/jruby/RubyBoolean;");
        mv.visitLabel(l1);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBY, "getTrue", "()Lorg/jruby/RubyBoolean;");
        mv.visitLabel(l2);
        
        return null;
    }

    public Instruction visitNthRefNode(NthRefNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitOpElementAsgnNode(OpElementAsgnNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitOpAsgnNode(OpAsgnNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitOpAsgnAndNode(OpAsgnAndNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitOpAsgnOrNode(OpAsgnOrNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitOptNNode(OptNNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitOrNode(OrNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitPostExeNode(PostExeNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitRedoNode(RedoNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitRegexpNode(RegexpNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitRescueBodyNode(RescueBodyNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitRescueNode(RescueNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitRetryNode(RetryNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitReturnNode(ReturnNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitSClassNode(SClassNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitScopeNode(ScopeNode iVisited) {
        iVisited.getBodyNode().accept(this);
        
        return null;
    }

    public Instruction visitSelfNode(SelfNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitSplatNode(SplatNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitStrNode(StrNode iVisited) {
        lineNumber(iVisited);
        loadRuntime();
        mv.visitLdcInsn(iVisited.getValue());
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBY, "newString", "(Ljava/lang/String;)Lorg/jruby/RubyString;");
        
        return null;
    }

    public Instruction visitSuperNode(SuperNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitSValueNode(SValueNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitSymbolNode(SymbolNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitToAryNode(ToAryNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitTrueNode(TrueNode iVisited) {
        lineNumber(iVisited);
        loadRuntime();
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBY, "getTrue", "()Lorg/jruby/RubyBoolean;");
        
        return null;
    }

    public Instruction visitUndefNode(UndefNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitUntilNode(UntilNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitVAliasNode(VAliasNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitVCallNode(VCallNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitWhenNode(WhenNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitWhileNode(WhileNode iVisited) {
        lineNumber(iVisited);
        
        // leave nil on the stack when we're done
        loadRuntime();
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBY, "getNil", "()Lorg/jruby/runtime/builtin/IRubyObject;");
        
        if (iVisited.getBodyNode() != null) {
            Label l1 = new Label();
            Label l2 = new Label();
            
            mv.visitLabel(l1);
            
            if (iVisited.evaluateAtStart()) {
                iVisited.getConditionNode().accept(this);
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBYOBJECT, "isTrue", "()Z");
                // conditionally jump end for while
                mv.visitJumpInsn(Opcodes.IFEQ, l2);
            }
            
            iVisited.getBodyNode().accept(this);
            // clear last result
            mv.visitInsn(Opcodes.POP);
            
            if (!iVisited.evaluateAtStart()) {
                iVisited.getConditionNode().accept(this);
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBYOBJECT, "isTrue", "()Z");
                // conditionally jump back for do...while
                mv.visitJumpInsn(Opcodes.IFNE, l1);
            } else {
                // jump back for while
                mv.visitJumpInsn(Opcodes.GOTO, l1);
            }
            
            mv.visitLabel(l2);
        }
        
        return null;
    }

    public Instruction visitXStrNode(XStrNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitYieldNode(YieldNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitZArrayNode(ZArrayNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitZSuperNode(ZSuperNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    private void lineNumber(Node iVisited) {
        if (!EXPERIMENTAL_SOURCING) {
            return;
        }
        if (lastLine == (lastLine = iVisited.getPosition().getEndLine())) return; // did not change lines for this node, don't bother relabeling
        
        Label l = new Label();
        mv.visitLabel(l);
        mv.visitLineNumber(iVisited.getPosition().getEndLine(), l);
    }

    public ClassWriter getClassWriter() {
        return cv;
    }
    
    public void setClassWriter(ClassWriter cv) {
        this.cv = cv;
    }

    private void setupArgs(Node node) {
        lineNumber(node);
        if (node == null) {
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitTypeInsn(Opcodes.ANEWARRAY, IRUBYOBJECT);
        } else if (node instanceof ArrayNode) {
            int count = ((ArrayNode)node).size();
            mv.visitLdcInsn(new Integer(count));
            mv.visitTypeInsn(Opcodes.ANEWARRAY, IRUBYOBJECT);
            
            int i = 0;
            for (Iterator iter = ((ArrayNode) node).iterator(); iter.hasNext();) {
                final Node next = (Node) iter.next();
                
                mv.visitInsn(Opcodes.DUP);
                mv.visitLdcInsn(new Integer(i));
                
                // FIXME: implement splatnode logic to make appropriate count
                if (next instanceof SplatNode) {
                    //count += getSplatNodeSize((SplatNode)next) - 1;
                } else {
                    next.accept(this);
                }
                
                mv.visitInsn(Opcodes.AASTORE);
                i++;
            }
        }
    }

    private void loadThreadContext() {
        // FIXME: make this work correctly for non-static, non-singleton
        if (tcLoaded) {
            mv.visitVarInsn(Opcodes.ALOAD, 51);
            return;
        }
        loadRuntime();
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBY, "getCurrentContext", "()Lorg/jruby/runtime/ThreadContext;");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ASTORE, 51);
        tcLoaded = true;
    }

    private void loadRuntime() {
        // FIXME: make this work correctly for non-static, non-singleton
        if (runtimeLoaded) {
            mv.visitVarInsn(Opcodes.ALOAD, 50);
            return;
        } 
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBYOBJECT, "getRuntime", "()Lorg/jruby/IRuby;");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ASTORE, 50);
        runtimeLoaded = true;
    }
}
