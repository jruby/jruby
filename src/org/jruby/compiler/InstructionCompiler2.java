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
package org.jruby.compiler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jruby.IRuby;
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
import org.jruby.ast.RootNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SValueNode;
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
import org.jruby.internal.runtime.methods.MultiStub;
import org.jruby.internal.runtime.methods.MultiStubMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Visibility;
import org.jruby.util.JRubyClassLoader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class InstructionCompiler2 implements NodeVisitor {
    private static final String RUBYMODULE = "org/jruby/RubyModule";

    private static final String IRUBY = "org/jruby/IRuby";

    private static final String IRUBYOBJECT = "org/jruby/runtime/builtin/IRubyObject";

    private static final String FRAME = "org/jruby/runtime/Frame";

    private static final String THREADCONTEXT = "org/jruby/runtime/ThreadContext";

    private static final boolean EXPERIMENTAL_SOURCING = true; 
    
    private ArgsNode args;
    private ClassWriter cv;
    private MethodVisitor mv;
    
    private int lastLine = 0;

    private boolean runtimeLoaded;
    
    Map classWriters = new HashMap();
    ClassWriter currentMultiStub = null;
    int multiStubIndex = -1;
    int multiStubCount = -1;

    private String classname;

    private String sourceName;

    public InstructionCompiler2() {
    }
    
    public void closeOutMultiStub() {
        if (currentMultiStub != null) {
            while (multiStubIndex < 9) {
                MethodVisitor multiStubMethod = createNewMethod();
                multiStubMethod.visitCode();
                multiStubMethod.visitInsn(Opcodes.ACONST_NULL);
                multiStubMethod.visitInsn(Opcodes.ARETURN);
                multiStubMethod.visitMaxs(1, 1);
                multiStubMethod.visitEnd();
            }
        }
    }
    
    public void defineModuleFunction(IRuby runtime, String module, String name, MultiStub stub, int index, Arity arity, Visibility visibility) {
        runtime.getModule(module).addMethod(name, new MultiStubMethod(stub, index, runtime.getModule(module), arity, visibility));
    }
    
    public String[] compile(String classname, String sourceName, Node node) {
        cv = new ClassWriter(true);
        if (classname.startsWith("-e")) {
            classname = classname.replaceAll("-e", "DashE");
        }
        classWriters.put(classname, cv);
        this.classname = classname;
        this.sourceName = sourceName;

        cv.visit(Opcodes.V1_2, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
                classname, null, "java/lang/Object", new String[] {"org/jruby/ast/executable/Script"});
        cv.visitSource(sourceName, null);

        mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>",
                "()V");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // create method for toplevel of script
        mv = createNewMethod();
        String className = classname + "$MultiStub" + multiStubCount;
        String methodName = "method" + multiStubIndex;
        mv.visitCode();

        try {
        node.accept(this);
        } catch (NotCompilableException nce) {
            // TODO: recover somehow? build a pure eval method?
            throw nce;
        }

        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(1, 1); // automatically calculated by ASM
        mv.visitEnd();
        
        runtimeLoaded = false;
        
        closeOutMultiStub();

        // add Script#run impl
        mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "run", "(Lorg/jruby/runtime/ThreadContext;Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;", null, null);
        mv.visitTypeInsn(Opcodes.NEW, className);
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, className, "<init>", "()V");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, methodName, "(Lorg/jruby/runtime/ThreadContext;Lorg/jruby/runtime/builtin/IRubyObject;[Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;");
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        
        return new String[] {className, methodName};
    }
    
    public Class loadClasses(JRubyClassLoader loader) throws ClassNotFoundException {
        for (Iterator iter = classWriters.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry)iter.next();
            String key = (String)entry.getKey();
            ClassWriter writer = (ClassWriter)entry.getValue();
            
            loader.defineClass(key.replaceAll("/", "."), writer.toByteArray());
        }

        return loader.loadClass(classname.replaceAll("/", "."));
    }

    // finished
    public Instruction visitAliasNode(AliasNode iVisited) {
        lineNumber(iVisited);
        getRubyClass();
        mv.visitInsn(Opcodes.DUP);
        mv.visitInsn(Opcodes.DUP);
        Label l1 = new Label();
        mv.visitJumpInsn(Opcodes.IFNONNULL, l1);
        newTypeError("no class to make alias");
        mv.visitInsn(Opcodes.ATHROW);
        
        mv.visitLabel(l1);
        loadThreadContext();
        defineAlias(iVisited.getNewName(), iVisited.getOldName());
        mv.visitLdcInsn("method_added");
        newSymbol(iVisited.getNewName());
        invokeRubyModule("callMethod", "(Lorg/jruby/runtime/ThreadContext;Ljava/lang/String;Lorg/jruby/RubySymbol;)Lorg/jruby/runtime/builtin/IRubyObject;");
        
        return null;
    }

    private void newSymbol(String name) {
        loadRuntime();
        mv.visitLdcInsn(name);
        invokeIRuby("newSymbol", "()Lorg/jruby/RubySymbol;");
    }

    private void defineAlias(String newName, String oldName) {
        mv.visitLdcInsn(newName);
        mv.visitLdcInsn(oldName);
        invokeRubyModule("defineAlias", "(Ljava/lang/String;Ljava/lang/String;)V");
    }

    private void newTypeError(String error) {
        loadRuntime();
        mv.visitLdcInsn(error);
        invokeIRuby("newTypeError", "(Ljava/lang/String;)Lorg/jruby/exceptions/RaiseException;");
    }

    public Instruction visitAndNode(AndNode iVisited) {
        lineNumber(iVisited);
        iVisited.getFirstNode().accept(this);
        mv.visitInsn(Opcodes.DUP);
        invokeIRubyObject("isTrue", "()B");
        Label l1 = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, l1);
        mv.visitInsn(Opcodes.POP); // remove first node's result
        iVisited.getSecondNode().accept(this);
        mv.visitLabel(l1);
        
        return null;
    }

    private void invokeIRubyObject(String methodName, String signature) {
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBYOBJECT, methodName, signature);
    }

    private void invokeIRuby(String methodName, String signature) {
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBY, methodName, signature);
    }

    private void invokeThreadContext(String methodName, String signature) {
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, THREADCONTEXT, methodName, signature);
    }

    private void invokeRubyModule(String methodName, String signature) {
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, RUBYMODULE, methodName, signature);
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

        invokeIRuby("newArray", "([Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/RubyArray;");
        
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
    
    public Instruction visitArgsPushNode(ArgsPushNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }
    
    // FIXME: I just copied logic for CallNode, but the return for this should be lhs of assignment
    public Instruction visitAttrAssignNode(AttrAssignNode iVisited) {
        lineNumber(iVisited);
        loadThreadContext(); // [tc]
        invokeThreadContext("beginCallArgs", "()V"); // []
        
        // TODO: try finally around this
        // recv
        iVisited.getReceiverNode().accept(this); // [recv]
        
        // args
        setupArgs(iVisited.getArgsNode()); // [recv, args]

        loadThreadContext(); // [recv, args, tc]
        invokeThreadContext("endCallArgs", "()V"); // [recv, args]
        
        // dup recv for CallType check
        mv.visitInsn(Opcodes.SWAP); 
        mv.visitInsn(Opcodes.DUP); // [args, recv, recv]
        
        loadThreadContext();
        invokeThreadContext("getFrameSelf", "()Lorg/jruby/runtime/builtin/IRubyObject;"); // [args, recv, recv, frameSelf]
        
        Label l1 = new Label();
        Label l2 = new Label();
        
        // choose CallType appropriately
        mv.visitJumpInsn(Opcodes.IF_ACMPEQ, l1); // [args, recv]
        mv.visitFieldInsn(Opcodes.GETSTATIC, "org/jruby/runtime/CallType", "NORMAL", "Lorg/jruby/runtime/CallType;");
        mv.visitJumpInsn(Opcodes.GOTO, l2);
        mv.visitLabel(l1);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "org/jruby/runtime/CallType", "VARIABLE", "Lorg/jruby/runtime/CallType;");
        mv.visitLabel(l2);
        
        // [args, recv, calltype]
        
        // swap recv and args on stack
        mv.visitInsn(Opcodes.SWAP); // [args, calltype, recv]
        
        loadThreadContext(); // [args, calltype, recv, tc]
        mv.visitInsn(Opcodes.DUP2_X2); // [recv, tc, args, calltype, recv, tc]
        mv.visitInsn(Opcodes.POP2); // [recv, tc, args, calltype]
        
        // name to callMethod
        mv.visitLdcInsn(iVisited.getName()); // [recv, tc, args, calltype, name]
        
        // put name under args and calltype
        mv.visitInsn(Opcodes.DUP_X2); // [recv, tc, name, args, calltype, name]
        
        // pop name on top
        mv.visitInsn(Opcodes.POP); // [recv, name, args, calltype]
        
        invokeIRubyObject("callMethod", "(Lorg/jruby/runtime/ThreadContext;Ljava/lang/String;[Lorg/jruby/runtime/builtin/IRubyObject;Lorg/jruby/runtime/CallType;)Lorg/jruby/runtime/builtin/IRubyObject;");
        
        return null;
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
        iVisited.getBodyNode().accept(this);
        
        return null;
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
            getRubyClass();
            Label l1 = new Label();
            
            mv.visitJumpInsn(Opcodes.IFNONNULL, l1);
            
            newTypeError("no class/module to define constant");
            mv.visitInsn(Opcodes.ATHROW);
            
            mv.visitLabel(l1);
            
            peekCRef();
        }

        mv.visitTypeInsn(Opcodes.CHECKCAST, RUBYMODULE);
        
        mv.visitInsn(Opcodes.SWAP);
        mv.visitLdcInsn(iVisited.getName());
        mv.visitInsn(Opcodes.SWAP);
        invokeRubyModule("setConstant", "(Ljava/lang/String;Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;");
        
        return null;
    }

    private void peekCRef() {
        loadThreadContext();
        invokeThreadContext("peekCRef", "()Lorg/jruby/util/collections/SinglyLinkedList;");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/util/collections/SingleLinkedList", "getValue", "()Ljava/lang/Object;");
    }

    public Instruction visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
        lineNumber(iVisited);
        
        // eval value
        iVisited.getValueNode().accept(this);
                
        getCRefClass();
        
        // dup it
        mv.visitInsn(Opcodes.DUP);
        
        Label l1 = new Label();
        Label l2 = new Label();
        mv.visitJumpInsn(Opcodes.IFNONNULL, l1);
        
        // pop extra null class
        // FIXME this bit is untested
        mv.visitInsn(Opcodes.POP);
        loadSelf();
        invokeIRubyObject("getMetaClass", "()Lorg/jruby/RubyClass;");
        mv.visitJumpInsn(Opcodes.GOTO, l2);
        
        // FIXME this bit is untested
        mv.visitLabel(l1);
        // dup it again
        mv.visitInsn(Opcodes.DUP);
        invokeRubyModule("isSingleton", "()Z");
        mv.visitJumpInsn(Opcodes.IFEQ, l2);
        
        mv.visitLdcInsn("__attached__");
        invokeIRubyObject("getInstanceVariable", "(Ljava/lang/String;)Lorg/jruby/runtime/builtin/IRubyObject;");
        mv.visitTypeInsn(Opcodes.CHECKCAST, RUBYMODULE);
        
        mv.visitLabel(l2);
        
        mv.visitInsn(Opcodes.SWAP);
        mv.visitLdcInsn(iVisited.getName());
        mv.visitInsn(Opcodes.SWAP);
        invokeRubyModule("setClassVar", "(Ljava/lang/String;Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;");
        
        return null;
    }

    public Instruction visitClassVarDeclNode(ClassVarDeclNode iVisited) {
        // FIXME INCOMPLETE, probably broken
        lineNumber(iVisited);
        
        getRubyClass();
        Label l298 = new Label();
        mv.visitJumpInsn(Opcodes.IFNONNULL, l298);
        newTypeError("no class/module to define class variable");
        mv.visitInsn(Opcodes.ATHROW);
        
        mv.visitLabel(l298);
        getCRefClass();
        
        mv.visitLdcInsn(iVisited.getName());
        
        iVisited.getValueNode().accept(this);
        
        invokeRubyModule("setClassVar", "(Ljava/lang/String;Lorg/jruby/runtime/builtin/IRubyObject;)V");
        
        loadRuntime();
        invokeIRuby("getNil", "()Lorg/jruby/runtime/builtin/IRubyObject;");
        
        return null;
    }

    public Instruction visitClassVarNode(ClassVarNode iVisited) {
        // TODO untested
        lineNumber(iVisited);
        
        getCRefClass();
        
        // dup it
        mv.visitInsn(Opcodes.DUP);
        
        Label l1 = new Label();
        Label l2 = new Label();
        mv.visitJumpInsn(Opcodes.IFNONNULL, l1);
        
        // pop extra null class
        mv.visitInsn(Opcodes.POP);
        loadSelf();
        invokeIRubyObject("getMetaClass", "()Lorg/jruby/RubyClass;");
        mv.visitJumpInsn(Opcodes.GOTO, l2);
        
        mv.visitLabel(l1);
        // dup it again
        mv.visitInsn(Opcodes.DUP);
        invokeRubyModule("isSingleton", "()Z");
        mv.visitJumpInsn(Opcodes.IFEQ, l2);
        
        mv.visitLdcInsn("__attached__");
        invokeIRubyObject("getInstanceVariable", "(Ljava/lang/String;)Lorg/jruby/runtime/builtin/IRubyObject;");
        mv.visitTypeInsn(Opcodes.CHECKCAST, RUBYMODULE);
        
        mv.visitLabel(l2);
        
        mv.visitLdcInsn(iVisited.getName());
        invokeRubyModule("getClassVar", "(Ljava/lang/String;)Lorg/jruby/runtime/builtin/IRubyObject;");
        
        return null;
    }

    public Instruction visitCallNode(CallNode iVisited) {
        lineNumber(iVisited);
        loadThreadContext(); // [tc]
        invokeThreadContext("beginCallArgs", "()V"); // []
        
        // TODO: try finally around this
        // recv
        iVisited.getReceiverNode().accept(this); // [recv]
        
        // args
        setupArgs(iVisited.getArgsNode()); // [recv, args]

        loadThreadContext(); // [recv, args, tc]
        invokeThreadContext("endCallArgs", "()V"); // [recv, args]
        
        // dup recv for CallType check
        mv.visitInsn(Opcodes.SWAP); 
        mv.visitInsn(Opcodes.DUP); // [args, recv, recv]
        
        loadThreadContext();
        invokeThreadContext("getFrameSelf", "()Lorg/jruby/runtime/builtin/IRubyObject;"); // [args, recv, recv, frameSelf]
        
        Label l1 = new Label();
        Label l2 = new Label();
        
        // choose CallType appropriately
        mv.visitJumpInsn(Opcodes.IF_ACMPEQ, l1); // [args, recv]
        mv.visitFieldInsn(Opcodes.GETSTATIC, "org/jruby/runtime/CallType", "NORMAL", "Lorg/jruby/runtime/CallType;");
        mv.visitJumpInsn(Opcodes.GOTO, l2);
        mv.visitLabel(l1);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "org/jruby/runtime/CallType", "VARIABLE", "Lorg/jruby/runtime/CallType;");
        mv.visitLabel(l2);
        
        // [args, recv, calltype]
        
        // swap recv and args on stack
        mv.visitInsn(Opcodes.SWAP); // [args, calltype, recv]
        
        loadThreadContext(); // [args, calltype, recv, tc]
        mv.visitInsn(Opcodes.DUP2_X2); // [recv, tc, args, calltype, recv, tc]
        mv.visitInsn(Opcodes.POP2); // [recv, tc, args, calltype]
        
        // name to callMethod
        mv.visitLdcInsn(iVisited.getName()); // [recv, tc, args, calltype, name]
        
        // put name under args and calltype
        mv.visitInsn(Opcodes.DUP_X2); // [recv, tc, name, args, calltype, name]
        
        // pop name on top
        mv.visitInsn(Opcodes.POP); // [recv, name, args, calltype]
        
        invokeIRubyObject("callMethod", "(Lorg/jruby/runtime/ThreadContext;Ljava/lang/String;[Lorg/jruby/runtime/builtin/IRubyObject;Lorg/jruby/runtime/CallType;)Lorg/jruby/runtime/builtin/IRubyObject;");
        
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
        lineNumber(iVisited);
        
        loadThreadContext();
        mv.visitLdcInsn(iVisited.getName());
        invokeThreadContext("getConstant", "(Ljava/lang/String;)Lorg/jruby/runtime/builtin/IRubyObject;");
        
        return null;
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

    public MethodVisitor createNewMethod() {
        // create a new MultiStub-based method impl and provide the method visitor for it
        if (currentMultiStub == null || multiStubIndex == 9) {
            if (currentMultiStub != null) {
                // FIXME can we end if there's still a method in flight?
                currentMultiStub.visitEnd();
            }
            
            multiStubCount++;
            
            currentMultiStub = new ClassWriter(true);
            currentMultiStub.visit(Opcodes.V1_2, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER + Opcodes.ACC_STATIC,
                    classname + "$MultiStub" + multiStubCount, null, "java/lang/Object", new String[] {"org/jruby/internal/runtime/methods/MultiStub"});
            cv.visitInnerClass(classname + "$MultiStub" + multiStubCount, classname, "MultiStub" + multiStubCount, Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC);
            multiStubIndex = 0;
            classWriters.put(classname + "$MultiStub" + multiStubCount, currentMultiStub);
            currentMultiStub.visitSource(sourceName, null);

            MethodVisitor stubConstructor = currentMultiStub.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            stubConstructor.visitCode();
            stubConstructor.visitVarInsn(Opcodes.ALOAD, 0);
            stubConstructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>",
                    "()V");
            stubConstructor.visitInsn(Opcodes.RETURN);
            stubConstructor.visitMaxs(1, 1);
            stubConstructor.visitEnd();
        } else {
            multiStubIndex++;
        }
        
        return currentMultiStub.visitMethod(Opcodes.ACC_PUBLIC, "method" + multiStubIndex, "(Lorg/jruby/runtime/ThreadContext;Lorg/jruby/runtime/builtin/IRubyObject;[Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;", null, null);
    }

    public Instruction visitDefnNode(DefnNode iVisited) {
        // TODO: build arg list based on number of args, optionals, etc
        MethodVisitor oldMethod = mv;
        boolean oldRuntimeLoaded = runtimeLoaded;
        
        runtimeLoaded = false;
        
        mv = createNewMethod();
        mv.visitCode();
        
        // TODO: this probably isn't always an ArgsNode
        args = (ArgsNode)iVisited.getArgsNode();
        
        mv.visitLdcInsn(new Integer(iVisited.getScope().getNumberOfVariables()));
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "org/jruby/runtime/builtin/IRubyObject");
        mv.visitInsn(Opcodes.DUP);
        // FIXME: use constant for index of local vars
        mv.visitVarInsn(Opcodes.ASTORE, 4);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitInsn(Opcodes.SWAP);
        mv.visitLdcInsn(new Integer(0));
        mv.visitInsn(Opcodes.SWAP);
        mv.visitLdcInsn(new Integer(0));
        mv.visitLdcInsn(new Integer(args.getArity().getValue()));
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
        
        try {
            iVisited.getBodyNode().accept(this);
        } catch (NotCompilableException nce) {
            // TODO: recover somehow? build a pure eval method?
            throw nce;
        }

        mv.visitMaxs(1, 1); // automatically calculated by ASM
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitEnd();
        
        runtimeLoaded = oldRuntimeLoaded;
        
        mv = oldMethod;
        
        // method compiled, add to class
        getRubyClass();
        Label l335 = new Label();
        // if class is null, throw error
        mv.visitJumpInsn(Opcodes.IFNONNULL, l335);
        newTypeError("No class to add method.");
        mv.visitInsn(Opcodes.ATHROW);
        mv.visitLabel(l335);
        
        Label l338 = new Label();
        
        // only do Object#initialize check if necessary
        if (iVisited.getName().equals("initialize")) {
            // got class, compare to Object
            getRubyClass();
            loadRuntime();
            invokeIRuby("getObject", "()Lorg/jruby/RubyClass;");
            // if class == Object
            mv.visitJumpInsn(Opcodes.IF_ACMPNE, l338);
            loadRuntime();
            // display warning about redefining Object#initialize
            invokeIRuby("getWarnings", "()Lorg/jruby/common/RubyWarnings;");
            mv.visitLdcInsn("redefining Object#initialize may cause infinite loop");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/common/RubyWarnings", "warn", "(Ljava/lang/String;)V");
        }
        
        mv.visitLabel(l338);
        // TODO: fix this section for initialize visibility
//        mv.visitLdcInsn(iVisited.getName());
//        mv.visitLdcInsn("initialize");
//        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z");
//        Label l341 = new Label();
//        mv.visitJumpInsn(Opcodes.IFNE, l341);
//        getCurrentVisibility();
//        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/runtime/Visibility", "isModuleFunction", "()Z");
//        Label l342 = new Label();
//        mv.visitJumpInsn(Opcodes.IFEQ, l342);
//        mv.visitLabel(l341);
//        mv.visitFieldInsn(Opcodes.GETSTATIC, "org/jruby/runtime/Visibility", "PRIVATE", "Lorg/jruby/runtime/Visibility;");
//        mv.visitVarInsn(ASTORE, 7);
//        mv.visitLabel(l342);
        getRubyClass();
        mv.visitTypeInsn(Opcodes.NEW, "org/jruby/internal/runtime/methods/MultiStubMethod");
        mv.visitInsn(Opcodes.DUP);
        mv.visitTypeInsn(Opcodes.NEW, classname + "$MultiStub" + multiStubCount);
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, classname + "$MultiStub" + multiStubCount, "<init>", "()V");
        mv.visitLdcInsn(new Integer(multiStubIndex));
        getRubyClass();
        // TODO: handle args some way? maybe unnecessary with method compiled?
//        mv.visitVarInsn(ALOAD, 4);
//        mv.visitMethodInsn(INVOKEVIRTUAL, "org/jruby/ast/DefnNode", "getArgsNode", "()Lorg/jruby/ast/Node;");
//        mv.visitTypeInsn(CHECKCAST, "org/jruby/ast/ArgsNode");
        mv.visitLdcInsn(new Integer(args.getArity().getValue()));
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/jruby/runtime/Arity", "createArity", "(I)Lorg/jruby/runtime/Arity;");
        getCurrentVisibility();
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/jruby/internal/runtime/methods/MultiStubMethod", "<init>", "(Lorg/jruby/internal/runtime/methods/MultiStub;ILorg/jruby/RubyModule;Lorg/jruby/runtime/Arity;Lorg/jruby/runtime/Visibility;)V");
        
        mv.visitLdcInsn(iVisited.getName());
        // put name before MultiStubMethod instance
        mv.visitInsn(Opcodes.SWAP);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/RubyModule", "addMethod", "(Ljava/lang/String;Lorg/jruby/runtime/ICallable;)V");
        // FIXME: this part is for invoking method_added or singleton_method_added
//        Label l345 = new Label();
//        mv.visitLabel(l345);
//        mv.visitLineNumber(538, l345);
//        loadThreadContext();
//        mv.visitMethodInsn(INVOKEVIRTUAL, "org/jruby/runtime/ThreadContext", "getCurrentVisibility", "()Lorg/jruby/runtime/Visibility;");
//        mv.visitMethodInsn(INVOKEVIRTUAL, "org/jruby/runtime/Visibility", "isModuleFunction", "()Z");
//        Label l346 = new Label();
//        mv.visitJumpInsn(IFEQ, l346);
//        Label l347 = new Label();
//        mv.visitLabel(l347);
//        mv.visitLineNumber(539, l347);
//        mv.visitVarInsn(ALOAD, 5);
//        mv.visitMethodInsn(INVOKEVIRTUAL, "org/jruby/RubyModule", "getSingletonClass", "()Lorg/jruby/MetaClass;");
//        mv.visitLdcInsn(iVisited.getName());
//        mv.visitTypeInsn(NEW, "org/jruby/internal/runtime/methods/WrapperCallable");
//        mv.visitInsn(DUP);
//        mv.visitVarInsn(ALOAD, 5);
//        mv.visitMethodInsn(INVOKEVIRTUAL, "org/jruby/RubyModule", "getSingletonClass", "()Lorg/jruby/MetaClass;");
//        mv.visitVarInsn(ALOAD, 8);
//        mv.visitFieldInsn(GETSTATIC, "org/jruby/runtime/Visibility", "PUBLIC", "Lorg/jruby/runtime/Visibility;");
//        mv.visitMethodInsn(INVOKESPECIAL, "org/jruby/internal/runtime/methods/WrapperCallable", "<init>", "(Lorg/jruby/RubyModule;Lorg/jruby/runtime/ICallable;Lorg/jruby/runtime/Visibility;)V");
//        mv.visitMethodInsn(INVOKEVIRTUAL, "org/jruby/MetaClass", "addMethod", "(Ljava/lang/String;Lorg/jruby/runtime/ICallable;)V");
//        Label l348 = new Label();
//        mv.visitLabel(l348);
//        mv.visitLineNumber(543, l348);
//        mv.visitVarInsn(ALOAD, 5);
//        mv.visitLdcInsn("singleton_method_added");
//        loadRuntime();
//        mv.visitLdcInsn(iVisited.getName());
//        invokeIRuby("newSymbol", "(Ljava/lang/String;)Lorg/jruby/RubySymbol;");
//        mv.visitMethodInsn(INVOKEVIRTUAL, "org/jruby/RubyModule", "callMethod", "(Lorg/jruby/runtime/ThreadContext;Ljava/lang/String;Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;");
//        mv.visitInsn(POP);
//        mv.visitLabel(l346);
//        mv.visitLineNumber(547, l346);
//        mv.visitVarInsn(ALOAD, 5);
//        mv.visitMethodInsn(INVOKEVIRTUAL, "org/jruby/RubyModule", "isSingleton", "()Z");
//        Label l349 = new Label();
//        mv.visitJumpInsn(IFEQ, l349);
//        Label l350 = new Label();
//        mv.visitLabel(l350);
//        mv.visitLineNumber(548, l350);
//        mv.visitVarInsn(ALOAD, 5);
//        mv.visitTypeInsn(CHECKCAST, "org/jruby/MetaClass");
//        mv.visitMethodInsn(INVOKEVIRTUAL, "org/jruby/MetaClass", "getAttachedObject", "()Lorg/jruby/runtime/builtin/IRubyObject;");
//        mv.visitLdcInsn("singleton_method_added");
//        loadRuntime();
//        mv.visitVarInsn(ALOAD, 4);
//        mv.visitMethodInsn(INVOKEVIRTUAL, "org/jruby/ast/DefnNode", "getName", "()Ljava/lang/String;");
//        invokeIRuby("newSymbol", "(Ljava/lang/String;)Lorg/jruby/RubySymbol;");
//        mv.visitMethodInsn(INVOKEINTERFACE, "org/jruby/runtime/builtin/IRubyObject", "callMethod", "(Lorg/jruby/runtime/ThreadContext;Ljava/lang/String;Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;");
//        mv.visitInsn(POP);
//        Label l351 = new Label();
//        mv.visitJumpInsn(GOTO, l351);
//        mv.visitLabel(l349);
//        mv.visitLineNumber(551, l349);
//        mv.visitVarInsn(ALOAD, 5);
//        mv.visitLdcInsn("method_added");
//        loadRuntime();
//        mv.visitLdcInsn(iVisited.getName());
//        invokeIRuby("newSymbol", "(Ljava/lang/String;)Lorg/jruby/RubySymbol;");
//        mv.visitMethodInsn(INVOKEVIRTUAL, "org/jruby/RubyModule", "callMethod", "(Lorg/jruby/runtime/ThreadContext;Ljava/lang/String;Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;");
//        mv.visitInsn(POP);
//        mv.visitLabel(l351);
//        mv.visitLineNumber(554, l351);
        loadRuntime();
        invokeIRuby("getNil", "()Lorg/jruby/runtime/builtin/IRubyObject;");
        
        return null;
    }

    private void getCurrentVisibility() {
        loadThreadContext();
        invokeThreadContext("getCurrentVisibility", "()Lorg/jruby/runtime/Visibility;");
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
        invokeThreadContext("getFrameSelf", "()Lorg/jruby/runtime/builtin/IRubyObject;"); // [frameSelf]
        
        mv.visitLdcInsn(iVisited.getName()); // [frameSelf, name]
        
        loadThreadContext(); // [frameSelf, name, tc]
        invokeThreadContext("beginCallArgs", "()V"); // [frameSelf, name]
        
        // args
        setupArgs(iVisited.getArgsNode()); // [frameSelf, name, args]

        loadThreadContext();
        invokeThreadContext("endCallArgs", "()V"); // [frameSelf, name, args]
        
        loadThreadContext();
        mv.visitInsn(Opcodes.DUP_X2); // [frameSelf, tc, name, args, tc]
        mv.visitInsn(Opcodes.POP); // [frameSelf, tc, name, args]
        
        mv.visitFieldInsn(Opcodes.GETSTATIC, "org/jruby/runtime/CallType", "FUNCTIONAL", "Lorg/jruby/runtime/CallType;"); // [frameSelf, tc, name, args, calltype]
        
        invokeIRubyObject("callMethod", "(Lorg/jruby/runtime/ThreadContext;Ljava/lang/String;[Lorg/jruby/runtime/builtin/IRubyObject;Lorg/jruby/runtime/CallType;)Lorg/jruby/runtime/builtin/IRubyObject;");
        
        return null;
    }

    public Instruction visitFalseNode(FalseNode iVisited) {
        lineNumber(iVisited);
        loadRuntime();
        invokeIRuby("getFalse", "()Lorg/jruby/RubyBoolean;");
        
        return null;
    }

    public Instruction visitFixnumNode(FixnumNode iVisited) {
        lineNumber(iVisited);
        loadThreadContext();
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                THREADCONTEXT, "getRuntime",
                "()Lorg/jruby/IRuby;");
        mv.visitLdcInsn(new Long(iVisited.getValue()));
        invokeIRuby("newFixnum", "(J)Lorg/jruby/RubyFixnum;");

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
        invokeIRubyObject("isTrue", "()Z");
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
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitInsn(Opcodes.SWAP);
        mv.visitLdcInsn(new Integer(iVisited.getIndex()));
        mv.visitInsn(Opcodes.SWAP);
        mv.visitInsn(Opcodes.AASTORE);
        
        return null;
    }

    public Instruction visitLocalVarNode(LocalVarNode iVisited) {
        lineNumber(iVisited);
        // check if it's an argument
        int index = iVisited.getIndex();
        
        if (args != null && (index - 2) < args.getArgsCount()) {
            // load from the incoming params
            // index is 2-based, and our zero is runtime
            
            // load args array
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitLdcInsn(new Integer(index - 2));
            mv.visitInsn(Opcodes.AALOAD);
        } else {
            mv.visitVarInsn(Opcodes.ALOAD, 4);
            mv.visitLdcInsn(new Integer(iVisited.getIndex()));
            mv.visitInsn(Opcodes.AALOAD);
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
        invokeIRuby("getNil", "()Lorg/jruby/runtime/builtin/IRubyObject;");
        
        return null;
    }

    public Instruction visitNotNode(NotNode iVisited) {
        lineNumber(iVisited);
        loadRuntime();
        iVisited.getConditionNode().accept(this);
        invokeIRubyObject("isTrue", "()B");
        Label l1 = new Label();
        Label l2 = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, l1);
        invokeIRuby("getFalse", "()Lorg/jruby/RubyBoolean;");
        mv.visitLabel(l1);
        invokeIRuby("getTrue", "()Lorg/jruby/RubyBoolean;");
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
    
    public Instruction visitRootNode(RootNode iVisited) {
        mv.visitLdcInsn(new Integer(iVisited.getStaticScope().getNumberOfVariables()));
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "org/jruby/runtime/builtin/IRubyObject");
        // FIXME: use constant for index of local vars
        mv.visitVarInsn(Opcodes.ASTORE, 4);
        
        iVisited.getBodyNode().accept(this);
        
        return null;
    }

    public Instruction visitSClassNode(SClassNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
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
        invokeIRuby("newString", "(Ljava/lang/String;)Lorg/jruby/RubyString;");
        
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
        invokeIRuby("getTrue", "()Lorg/jruby/RubyBoolean;");
        
        return null;
    }

    public Instruction visitUndefNode(UndefNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitUntilNode(UntilNode iVisited) {
        lineNumber(iVisited);
        
        // FIXME support next, break, etc
        
        // leave nil on the stack when we're done
        loadRuntime();
        invokeIRuby("getNil", "()Lorg/jruby/runtime/builtin/IRubyObject;");
        
        if (iVisited.getBodyNode() != null) {
            Label l1 = new Label();
            Label l2 = new Label();
            
            mv.visitLabel(l1);
            
            iVisited.getConditionNode().accept(this);
            invokeIRubyObject("isTrue", "()Z");
            // conditionally jump end for until
            mv.visitJumpInsn(Opcodes.IFNE, l2);
            
            iVisited.getBodyNode().accept(this);
            // clear last result
            mv.visitInsn(Opcodes.POP);
            
            // jump back for until
            mv.visitJumpInsn(Opcodes.GOTO, l1);
            
            mv.visitLabel(l2);
    }

        return null;
    }

    public Instruction visitVAliasNode(VAliasNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitVCallNode(VCallNode iVisited) {
        lineNumber(iVisited);
        loadSelf();
        loadThreadContext();
        
        mv.visitLdcInsn(iVisited.getName());

        mv.visitFieldInsn(Opcodes.GETSTATIC, "org/jruby/runtime/builtin/IRubyObject", "NULL_ARRAY", "[Lorg/jruby/runtime/builtin/IRubyObject;");
        mv.visitFieldInsn(Opcodes.GETSTATIC, "org/jruby/runtime/CallType", "FUNCTIONAL", "Lorg/jruby/runtime/CallType;");
        
        invokeIRubyObject("callMethod", "(Lorg/jruby/runtime/ThreadContext;Ljava/lang/String;[Lorg/jruby/runtime/builtin/IRubyObject;Lorg/jruby/runtime/CallType;)Lorg/jruby/runtime/builtin/IRubyObject;");
        
        return null;
    }

    public Instruction visitWhenNode(WhenNode iVisited) {
        throw new NotCompilableException("Node not supported: " + iVisited.toString());
    }

    public Instruction visitWhileNode(WhileNode iVisited) {
        lineNumber(iVisited);
        
        // leave nil on the stack when we're done
        loadRuntime();
        invokeIRuby("getNil", "()Lorg/jruby/runtime/builtin/IRubyObject;");
        
        if (iVisited.getBodyNode() != null) {
            Label l1 = new Label();
            Label l2 = new Label();
            
            mv.visitLabel(l1);
            
            if (iVisited.evaluateAtStart()) {
                iVisited.getConditionNode().accept(this);
                invokeIRubyObject("isTrue", "()Z");
                // conditionally jump end for while
                mv.visitJumpInsn(Opcodes.IFEQ, l2);
            }
            
            iVisited.getBodyNode().accept(this);
            // clear last result
            mv.visitInsn(Opcodes.POP);
            
            if (!iVisited.evaluateAtStart()) {
                iVisited.getConditionNode().accept(this);
                invokeIRubyObject("isTrue", "()Z");
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

    public Map getClassWriters() {
        return classWriters;
    }
    
    private void setupArgs(Node node) {
        if (node == null) {
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitTypeInsn(Opcodes.ANEWARRAY, IRUBYOBJECT);
        } else if (node instanceof ArrayNode) {
            lineNumber(node);
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
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        }

    private void loadRuntime() {
        // FIXME: make this work correctly for non-static, non-singleton
//        if (runtimeLoaded) {
//            mv.visitVarInsn(Opcodes.ALOAD, 50);
//            return;
//        }
        // load ThreadContext param
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        invokeThreadContext("getRuntime", "()Lorg/jruby/IRuby;");
//        mv.visitInsn(Opcodes.DUP);
//        mv.visitVarInsn(Opcodes.ASTORE, 50);
//        // FIXME find a better way of caching, since the path that caches may be conditional
//        runtimeLoaded = false;
        } 

    private void loadSelf() {
        mv.visitVarInsn(Opcodes.ALOAD, 2);
    }

    private void getCRefClass() {
        loadThreadContext();
        invokeThreadContext("peekCRef", "()Lorg/jruby/util/collections/SinglyLinkedList;");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/util/collections/SinglyLinkedList", "getValue", "()Ljava/lang/Object;");
        mv.visitTypeInsn(Opcodes.CHECKCAST, RUBYMODULE);
}
    private void getRubyClass() {
        loadThreadContext();
        invokeThreadContext("getRubyClass", "()Lorg/jruby/RubyModule;");
    }
}
