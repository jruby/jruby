/*
 * RubyToBytecodeCompiler.java
 * Created on 14.02.2002, 16:22:50
 * 
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>. All rights
 * reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:  
 *       "This product includes software developed by
 *        Jan Arne Petersen (jpetersen@uni-bonn.de)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "JRuby" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For 
 *    written permission, please contact jpetersen@uni-bonn.de.
 *
 * 5. Products derived from this software may not be called 
 *    "JRuby", nor may "JRuby" appear in their name, without prior 
 *    written permission of Jan Arne Petersen.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JAN ARNE PETERSEN OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * ====================================================================
 *
 */
package org.jruby.compiler;

import java.io.*;
import java.util.*;

import org.jruby.*;
import org.jruby.nodes.*;
import org.jruby.nodes.visitor.*;
import org.jruby.runtime.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyToJavaCompiler implements NodeVisitor {
    private PrintWriter pw;

    private Ruby ruby;

    private int indent = 2;
    
    private Map methodMap = new HashMap();
    private int method = 0;
    
    private String className = null;

    public RubyToJavaCompiler(PrintWriter pw, Ruby ruby) {
        this.pw = pw;

        this.ruby = ruby;
    }

    public static void main(String[] args) {
        Ruby ruby = Ruby.getDefaultInstance(null);

        String fc = loadFile(args[0]);
        Node script = ruby.getRubyParser().compileJavaString(args[0], fc, fc.length(), 0);

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(System.out));
        // pw.println("package " + (args.length > 2 ? args[2] : "org.jruby.scripts") + ";");
        pw.println();
        pw.println("import java.util.*;");
        pw.println();
        pw.println("import org.jruby.*;");
        pw.println("import org.jruby.exceptions.*;");
        pw.println("import org.jruby.javasupport.*;");
		pw.println("import org.jruby.nodes.*;");
		pw.println("import org.jruby.runtime.*;");
        pw.println();
        pw.println("public class " + (args.length > 1 ? args[1] : "Script") + "{");
        pw.println("    public static void main(String[] args) {");
        pw.println("        Ruby ruby = Ruby.getDefaultInstance(null);");
        pw.println("        RubyObject self = ruby.getRubyTopSelf();");
        pw.println();
        pw.println("        RubyObject lArgv = JavaUtil.convertJavaToRuby(ruby, args);");
        pw.println("        ruby.defineGlobalConstant(\"ARGV\", lArgv);");
        pw.println("        ruby.defineReadonlyVariable(\"$-p\", ruby.getNil());");
        pw.println("        ruby.defineReadonlyVariable(\"$-n\", ruby.getNil());");
        pw.println("        ruby.defineReadonlyVariable(\"$-a\", ruby.getNil());");
        pw.println("        ruby.defineReadonlyVariable(\"$-l\", ruby.getNil());");
        pw.println("        ruby.defineReadonlyVariable(\"$*\", lArgv);");
        pw.println("        ruby.initLoad(new ArrayList());");
        pw.println();
        pw.println("        try {");
        pw.println("            __run_script__(ruby, self);");
        pw.println("        } catch (RaiseException rExcptn) {");
        pw.println("            ruby.getRuntime().printError(rExcptn.getActException());");
        pw.println("        } catch (ThrowJump throwJump) {");
        pw.println("            ruby.getRuntime().printError(throwJump.getNameError());");
        pw.println("        }");
        pw.println("    }");
        pw.println();
		pw.println("    public static void __run_script__(Ruby ruby, RubyObject self) {");
        pw.println("        RubyModule __module__ = null;");
        pw.println("        RubyClass __class__ = null;");
        pw.println("        MethodNode __body__ = null;");
        pw.println("        int __noex__ = 0;");
        pw.println("        RubyObject __recv__ = null;");
        pw.println();
        try {
            Iterator iter = ruby.getScope().getLocalNames().iterator();
            while (iter.hasNext()) {
                String name = (String) iter.next();
                if (name.equals("_")) {
                    name = "__line__";
                } else if (name.equals("~")) {
                    name = "__match__";
                }
                pw.println("        RubyObject " + name + " = ruby.getNil();");
            }
            pw.println();
        } catch (NullPointerException npe) {
        }
        
        RubyToJavaCompiler compiler = new RubyToJavaCompiler(pw, ruby);
        compiler.className = (args.length > 1 ? args[1] : "Script");
        script.accept(compiler);
        pw.println();
        pw.println("        System.out.println();");
        pw.println("    }");
        pw.println();
        compiler.printMethods();
        pw.println("}");
        pw.flush();
    }

    private static String loadFile(String fileName) {
        try {
            File rubyFile = new File(fileName);
            StringBuffer sb = new StringBuffer((int) rubyFile.length());
            BufferedReader br = new BufferedReader(new FileReader(rubyFile));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            br.close();
            return sb.toString();

        } catch (IOException ioExcptn) {
            return "";
        }
    }

    private String indent() {
        StringBuffer sb = new StringBuffer(indent * 4);
        for (int i = 0; i < indent * 4; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /**
     * @see NodeVisitor#visitAliasNode(AliasNode)
     */
    public void visitAliasNode(AliasNode iVisited) {
        // pw.print(indent());
        pw.print("ruby.getRubyClass().aliasMethod(\"");
        pw.print(iVisited.getNewId());
        pw.print("\", \"");
        pw.print(iVisited.getOldId());
        pw.println("\");");
        pw.print(indent());
        pw.print("ruby.getRubyClass().funcall(\"method_added\", RubySymbol.newSymbol(ruby, \"");
        pw.print(iVisited.getNewId());
        // +++
        // pw.println("\"));");
        pw.print("\"))");
        // ---
    }

    /**
     * @see NodeVisitor#visitAndNode(AndNode)
     */
    public void visitAndNode(AndNode iVisited) {
        iVisited.getFirstNode().accept(this);
        pw.print(".isTrue() &&");
        iVisited.getSecondNode().accept(this);
        pw.print(".isTrue()");
    }

    /**
     * @see NodeVisitor#visitArgsCatNode(ArgsCatNode)
     */
    public void visitArgsCatNode(ArgsCatNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitArgsNode(ArgsNode)
     */
    public void visitArgsNode(ArgsNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitArgsPushNode(ArgsPushNode)
     */
    public void visitArgsPushNode(ArgsPushNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitArrayNode(ArrayNode)
     */
    public void visitArrayNode(ArrayNode iVisited) {
        pw.print("RubyArray.newArray(ruby, new RubyObject[] {");
        for (Node node = iVisited; node != null; node = node.getNextNode()) {
            node.getHeadNode().accept(this);
            if (node.getNextNode() != null) {
                pw.print(", ");
            }
        }
        pw.print("})");
    }

    /**
     * @see NodeVisitor#visitAttrSetNode(AttrSetNode)
     */
    public void visitAttrSetNode(AttrSetNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitBackRefNode(BackRefNode)
     */
    public void visitBackRefNode(BackRefNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitBeginNode(BeginNode)
     */
    public void visitBeginNode(BeginNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitBlockArgNode(BlockArgNode)
     */
    public void visitBlockArgNode(BlockArgNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitBlockNode(BlockNode)
     */
    public void visitBlockNode(BlockNode iVisited) {
        Node node = iVisited;
        while (node.getNextNode() != null) {
            // +++
            pw.print(indent());
            node.getHeadNode().accept(this);
            pw.println(';');
            // ---   
            node = node.getNextNode();
        }
        if (node.getHeadNode() != null) {
            // +++
            pw.print(indent());
            node.getHeadNode().accept(this);
            pw.println(';');
            // ---   
        }
    }

    /**
     * @see NodeVisitor#visitBlockPassNode(BlockPassNode)
     */
    public void visitBlockPassNode(BlockPassNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitBreakNode(BreakNode)
     */
    public void visitBreakNode(BreakNode iVisited) {
        // +++
        pw.print("break");
        // ---
    }

    /**
     * @see NodeVisitor#visitCDeclNode(CDeclNode)
     */
    public void visitCDeclNode(CDeclNode iVisited) {
        pw.print("ruby.getRubyClass().setConstant(\"");
        pw.print(iVisited.getVId());
        pw.print("\", ");
        iVisited.getValueNode().accept(this);
        pw.print(")");
    }

    /**
     * @see NodeVisitor#visitCFuncNode(CFuncNode)
     */
    public void visitCFuncNode(CFuncNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitCVAsgnNode(CVAsgnNode)
     */
    public void visitCVAsgnNode(CVAsgnNode iVisited) {
        pw.print("self.getClassVarSingleton().setClassVar(\"");
        pw.print(iVisited.getVId());
        pw.print("\", ");
        iVisited.getValueNode().accept(this);
        pw.print(")");
    }

    /**
     * @see NodeVisitor#visitCVDeclNode(CVDeclNode)
     */
    public void visitCVDeclNode(CVDeclNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitCVar2Node(CVar2Node)
     */
    public void visitCVar2Node(CVar2Node iVisited) {
    }

    /**
     * @see NodeVisitor#visitCVarNode(CVarNode)
     */
    public void visitCVarNode(CVarNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitCallNode(CallNode)
     */
    public void visitCallNode(CallNode iVisited) {
        iVisited.getRecvNode().accept(this);
        pw.print(".funcall3(\"");
        pw.print(iVisited.getMId());
        pw.print("\", new RubyObject[] {");
        for (Node node = iVisited.getArgsNode(); node != null; node = node.getNextNode()) {
            node.getHeadNode().accept(this);
            if (node.getNextNode() != null) {
                pw.print(", ");
            }
        }
        pw.print("})");
    }

    /**
     * @see NodeVisitor#visitCaseNode(CaseNode)
     */
    public void visitCaseNode(CaseNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitClassNode(ClassNode)
     */
    public void visitClassNode(ClassNode iVisited) {
        pw.println("if (ruby.getRubyClass() == null) {");
        indent++;
        pw.print(indent());
        pw.println("throw new TypeError(ruby, \"no outer class/module\");");
        indent--;
        pw.print(indent());
        pw.println("}");
        
        pw.print(indent());
        pw.print("if ((ruby.getRubyClass() == ruby.getClasses().getObjectClass()) && ruby.isAutoloadDefined(\"");
        pw.print(iVisited.getClassNameId());
        pw.println("\")) {");
            // getRuby().rb_autoload_load(node.nd_cname());
        pw.print(indent());
        pw.println("}");
        
        pw.print(indent());
        pw.print("if (ruby.getRubyClass().isConstantDefined(\"");
        pw.print(iVisited.getClassNameId());
        pw.println("\")) {");
        indent++;
        pw.print(indent());
        pw.print("__class__ = (RubyClass) ruby.getRubyClass().getConstant(\"");
        pw.print(iVisited.getClassNameId());
        pw.println("\");");
        
        pw.print(indent());
        pw.println("if (!__class__.isClass()) {");
        indent++;
        pw.print(indent());
        pw.print("throw new TypeError(ruby, \"");
        pw.print(iVisited.getClassNameId());
        pw.println(" is not a class\");");
        indent--;
        pw.print(indent());
        pw.println("}");
        
        if (iVisited.getSuperNode() != null) {
            pw.print(indent());
        	pw.print("RubyClass __superclass__ = (RubyClass)");
        	iVisited.getSuperNode().accept(this);
        	pw.println(";");
        	pw.print(indent());
        	pw.println("if (__class__.getSuperClass().getRealClass() != __superclass__) {");
        	indent++;
        	pw.print(indent());
        	pw.print("__class__ = ruby.defineClass(\"");
        	pw.print(iVisited.getClassNameId());
        	pw.println("\", __superclass__);");
        	pw.print(indent());
        	pw.print("ruby.getRubyClass().setConstant(\"");
        	pw.print(iVisited.getClassNameId());
        	pw.println("\", __class__);");
        	pw.print(indent());
        	pw.print("__class__.setClassPath(ruby.getRubyClass(), \"");
        	pw.print(iVisited.getClassNameId());
        	pw.println("\");");
        	indent--;
        	pw.print(indent());
        	pw.println("} else {");
        	indent++;
        }
            
        pw.print(indent());
      	pw.println("if (ruby.getSafeLevel() >= 4) {");
      	indent++;
      	pw.print(indent());
      	pw.println("throw new RubySecurityException(ruby, \"extending class prohibited\");");
      	indent--;
      	pw.print(indent());
      	pw.println("}");
      	pw.print(indent());
      	pw.println("// rb_clear_cache();");
      	
      	if (iVisited.getSuperNode() != null) {
      	    indent--;
        	pw.print(indent());
        	pw.println("}");
      	}
      	
      	pw.print(indent());
      	pw.println("} else {");
      	indent++;
      	
      	pw.print(indent());
      	pw.print("__class__ = ruby.defineClass(\"");
      	pw.print(iVisited.getClassNameId());
       	pw.print("\", ");
       	
       	if (iVisited.getSuperNode() != null) {
       	    pw.print("(RubyClass)");
       	    iVisited.getSuperNode().accept(this);
       	} else {
       	    pw.print("ruby.getClasses().getObjectClass()");
       	}
       	
       	pw.println(");");
       	pw.print(indent());
       	pw.print("ruby.getRubyClass().setConstant(\"");
       	pw.print(iVisited.getClassNameId());
       	pw.println("\", __class__);");
       	pw.print(indent());
       	pw.print("__class__.setClassPath(ruby.getRubyClass(), \"");
       	pw.print(iVisited.getClassNameId());
     	pw.println("\");");
      	
      	indent--;
      	pw.print(indent());
       	pw.println("}");
       	
       	pw.print(indent());
		pw.println("if (ruby.getWrapper() != null) {");
		indent++;
		pw.print(indent());
        pw.println("__class__.extendObject(ruby.getWrapper());");
		pw.print(indent());
        pw.println("__class__.includeModule(ruby.getWrapper());");
        indent--;
		pw.print(indent());
		pw.println("}");
		
		printModuleScope((ScopeNode)iVisited.getBodyNode(), "__class__");
    }

    /**
     * @see NodeVisitor#visitColon2Node(Colon2Node)
     */
    public void visitColon2Node(Colon2Node iVisited) {
    }

    /**
     * @see NodeVisitor#visitColon3Node(Colon3Node)
     */
    public void visitColon3Node(Colon3Node iVisited) {
    }

    /**
     * @see NodeVisitor#visitConstNode(ConstNode)
     */
    public void visitConstNode(ConstNode iVisited) {
        pw.print("ruby.getNamespace().getConstant(self, \"");
        pw.print(iVisited.getVId());
        pw.print("\")");
    }

    /**
     * @see NodeVisitor#visitDAsgnCurrNode(DAsgnCurrNode)
     */
    public void visitDAsgnCurrNode(DAsgnCurrNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitDAsgnNode(DAsgnNode)
     */
    public void visitDAsgnNode(DAsgnNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitDRegxNode(DRegxNode)
     */
    public void visitDRegxNode(DRegxNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitDRegxOnceNode(DRegxOnceNode)
     */
    public void visitDRegxOnceNode(DRegxOnceNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitDStrNode(DStrNode)
     */
    public void visitDStrNode(DStrNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitDVarNode(DVarNode)
     */
    public void visitDVarNode(DVarNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitDXStrNode(DXStrNode)
     */
    public void visitDXStrNode(DXStrNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitDefinedNode(DefinedNode)
     */
    public void visitDefinedNode(DefinedNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitDefnNode(DefnNode)
     */
    public void visitDefnNode(DefnNode iVisited) {
        pw.println("if (ruby.getRubyClass() == null) {");
        indent++;
        pw.print(indent());
        pw.println("throw new TypeError(ruby, \"no class to add method\");");
        indent--;
        pw.print(indent());
        pw.println("}");

		pw.print(indent());
        pw.print("__body__ = ruby.getRubyClass().searchMethod(\"");
        pw.print(iVisited.getMId());
        pw.println("\");");
        
        if (iVisited.getMId().equals("initialize")) {
            pw.print(indent());
        	pw.println("__noex__ = Constants.NOEX_PRIVATE;");
        } else {
            pw.print(indent());
            pw.println("if (ruby.isScope(Constants.SCOPE_PRIVATE)) {");
            pw.print(indent());
            pw.println("__noex__ = Constants.NOEX_PRIVATE;");
            pw.print(indent());
            pw.println("} else if (ruby.isScope(Constants.SCOPE_PROTECTED)) {");
            pw.print(indent());
            pw.println("__noex__ = Constants.NOEX_PROTECTED;");
            pw.print(indent());
            pw.println("} else if (ruby.getRubyClass() == ruby.getClasses().getObjectClass()) {");
            pw.print(indent());
            pw.print("__noex__ = ");
            pw.print(iVisited.getNoex());
            pw.println(";");
            pw.print(indent());
            pw.println("} else {");
            pw.print(indent());
            pw.println("__noex__ = Constants.NOEX_PUBLIC;");
            pw.print(indent());
            pw.println("}");
            
        }

		pw.print(indent());
        pw.println("if (__body__ != null && __body__.getOrigin() == ruby.getRubyClass() && (__body__.getNoex() & Constants.NOEX_UNDEF) != 0) {");
        pw.print(indent());
        pw.println("__noex__ |= Constants.NOEX_UNDEF;");
        pw.print(indent());
        pw.println("}");

		String methodName = "__m_" + method++ + "__";
		
		methodMap.put(methodName, iVisited.getDefnNode().copyNodeScope(ruby.getNamespace()));

		pw.print(indent());
        pw.println(methodName + "ns__ = ruby.getNamespace().cloneNamespace();");
		
		pw.print(indent());
        pw.print("ruby.getMethodCache().clearByName(\"");
        pw.print(iVisited.getMId());
        pw.println("\");");
        pw.print(indent());
        pw.print("ruby.getRubyClass().addMethod(\"");
        pw.print(iVisited.getMId());
        pw.print("\", new CFuncNode(CallbackFactory.getOptSingletonMethod(");
        pw.print(className);
        pw.print(".class, \"");
        pw.print(methodName);
        pw.println("\")), __noex__);");
        
        pw.print(indent());
        pw.println("if (ruby.getActMethodScope() == Constants.SCOPE_MODFUNC) {");
        pw.print(indent());
        pw.print("ruby.getRubyClass().getSingletonClass().addMethod(\"");
        pw.print(iVisited.getMId());
        pw.print("\", new CFuncNode(CallbackFactory.getOptSingletonMethod(");
        pw.print(className);
        pw.print(".class, \"");
        pw.print(methodName);
        pw.println("\")), Constants.NOEX_PUBLIC);");
        pw.print(indent());
        pw.print("ruby.getRubyClass().funcall(\"singleton_method_added\", RubySymbol.newSymbol(ruby, \"");
        pw.print(iVisited.getMId());
        pw.println("\"));");
        pw.print(indent());
        pw.println("}");
        
        pw.print(indent());
        pw.println("if (ruby.getRubyClass().isSingleton()) {");
        pw.print(indent());
        pw.print("ruby.getRubyClass().getInstanceVar(\"__attached__\").funcall(\"singleton_method_added\", RubySymbol.newSymbol(ruby, \"");
        pw.print(iVisited.getMId());
        pw.println("\"));");
        pw.print(indent());
        pw.println("} else {");
        pw.print(indent());
        pw.print("ruby.getRubyClass().funcall(\"method_added\", RubySymbol.newSymbol(ruby, \"");
        pw.print(iVisited.getMId());
        pw.println("\"));");
		pw.print(indent());
        pw.println("}");
    }

    /**
     * @see NodeVisitor#visitDefsNode(DefsNode)
     */
    public void visitDefsNode(DefsNode iVisited) {
        pw.print(indent());
        pw.print("__recv__ = ");
        iVisited.getRecvNode().accept(this);
        pw.println(";");
            
		pw.print(indent());
        pw.println("if (ruby.getSafeLevel() >= 4 && !__recv__.isTaint()) {");
        pw.print(indent());
        pw.println("throw new RubySecurityException(ruby, \"Insecure; can't define singleton method\");");
        pw.print(indent());
        pw.println("}");
            
        pw.print(indent());
        pw.println("if (__recv__.isFrozen()) {");
        pw.print(indent());
        pw.println("throw new RubyFrozenException(ruby, \"object\");");
        pw.print(indent());
        pw.println("}");
        
        pw.print(indent());
        pw.println("__class__ = __recv__.getSingletonClass();");
            
        pw.print(indent());
        pw.print("__body__ = (MethodNode)__class__.getMethods().get(\"");
        pw.print(iVisited.getMId());
        pw.println("\");");
        
        pw.print(indent());
        pw.println("if (__body__ != null) {");
        pw.print(indent());
        pw.println("if (ruby.getSafeLevel() >= 4) {");
        pw.print(indent());
        pw.println("throw new RubySecurityException(ruby, \"redefining method prohibited\");");
		pw.print(indent());
        pw.println("}");
		pw.print(indent());
        pw.println("}");
        
		String methodName = "__m_" + method++ + "__";
		
		methodMap.put(methodName, iVisited.getDefnNode().copyNodeScope(ruby.getNamespace()));

		pw.print(indent());
        pw.println(methodName + "ns__ = ruby.getNamespace().cloneNamespace();");
        
        pw.print(indent());
        pw.print("ruby.getMethodCache().clearByName(\"");
        pw.print(iVisited.getMId());
        pw.println("\");");
        
        pw.print(indent());
        pw.print("__class__.addMethod(\"");
        pw.print(iVisited.getMId());
        pw.print("\", new CFuncNode(CallbackFactory.getOptSingletonMethod(");
        pw.print(className);
        pw.print(".class, \"");
        pw.print(methodName);
        pw.println("\")), Constants.NOEX_PUBLIC | (__body__ != null ? __body__.getNoex() & Constants.NOEX_UNDEF : 0));");

    }

    /**
     * @see NodeVisitor#visitDotNode(DotNode)
     */
    public void visitDotNode(DotNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitEnsureNode(EnsureNode)
     */
    public void visitEnsureNode(EnsureNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitEvStrNode(EvStrNode)
     */
    public void visitEvStrNode(EvStrNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitFBodyNode(FBodyNode)
     */
    public void visitFBodyNode(FBodyNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitFCallNode(FCallNode)
     */
    public void visitFCallNode(FCallNode iVisited) {
        pw.print("self.funcall(\"");
        pw.print(iVisited.getMId());
        pw.print("\", new RubyObject[] {");
        for (Node node = iVisited.getArgsNode(); node != null; node = node.getNextNode()) {
            node.getHeadNode().accept(this);
            if (node.getNextNode() != null) {
                pw.print(", ");
            }
        }
        pw.print("})");
    }

    /**
     * @see NodeVisitor#visitFalseNode(FalseNode)
     */
    public void visitFalseNode(FalseNode iVisited) {
        pw.print("ruby.getFalse()");
    }

    /**
     * @see NodeVisitor#visitFlipNode(FlipNode)
     */
    public void visitFlipNode(FlipNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitForNode(ForNode)
     */
    public void visitForNode(ForNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitGAsgnNode(GAsgnNode)
     */
    public void visitGAsgnNode(GAsgnNode iVisited) {
        pw.print(indent());
        pw.print("ruby.setGlobalVar(\"");
        pw.print(iVisited.getEntry().getName());
        pw.print("\", ");
        iVisited.getValueNode().accept(this);
        pw.println(");");
    }

    /**
     * @see NodeVisitor#visitGVarNode(GVarNode)
     */
    public void visitGVarNode(GVarNode iVisited) {
        pw.print("ruby.getGlobalVar(\"");
        pw.print(iVisited.getEntry().getName());
        pw.print("\")");
    }

    /**
     * @see NodeVisitor#visitHashNode(HashNode)
     */
    public void visitHashNode(HashNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitIAsgnNode(IAsgnNode)
     */
    public void visitIAsgnNode(IAsgnNode iVisited) {
        pw.print("self.setInstanceVar(\"");
        pw.print(iVisited.getVId());
        pw.print("\", ");
        iVisited.getValueNode().accept(this);
        pw.print(")");
    }

    /**
     * @see NodeVisitor#visitIFuncNode(IFuncNode)
     */
    public void visitIFuncNode(IFuncNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitIVarNode(IVarNode)
     */
    public void visitIVarNode(IVarNode iVisited) {
        pw.print("self.getInstanceVar(\"");
        pw.print(iVisited.getVId());
        pw.print("\")");
    }

    /**
     * @see NodeVisitor#visitIfNode(IfNode)
     */
    public void visitIfNode(IfNode iVisited) {
        // +++
        // pw.print(indent());
        // ---
        pw.print("if (");
        iVisited.getConditionNode().accept(this);
        pw.println(".isTrue()) {");
        indent++;
        // +++
        pw.print(indent());
        // ---
        iVisited.getBodyNode().accept(this);
        // +++
        pw.println(';');
        // ---
        indent--;
        if (iVisited.getElseNode() != null) {
            pw.print(indent());
            pw.println("} else {");
            indent++;
            // +++
            pw.print(indent());
            // ---
            iVisited.getElseNode().accept(this);
            // +++
            pw.println(';');
            // ---
            indent--;
        }
        pw.print(indent());
        // +++
        // pw.println("}");
        pw.print("}");
        // ---
    }

    /**
     * @see NodeVisitor#visitIterNode(IterNode)
     */
    public void visitIterNode(IterNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitLAsgnNode(LAsgnNode)
     */
    public void visitLAsgnNode(LAsgnNode iVisited) {
        pw.print(ruby.getScope().getLocalNames().get(iVisited.getCount()));
        pw.print(" = ");
        iVisited.getValueNode().accept(this);
        pw.println(";");
    }

    /**
     * @see NodeVisitor#visitLVarNode(LVarNode)
     */
    public void visitLVarNode(LVarNode iVisited) {
        pw.print(ruby.getScope().getLocalNames().get(iVisited.getCount()));
    }

    /**
     * @see NodeVisitor#visitLitNode(LitNode)
     */
    public void visitLitNode(LitNode iVisited) {
        RubyObject literal = iVisited.getLiteral();

        if (literal instanceof RubyFixnum) {
            pw.print("RubyFixnum.newFixnum(ruby, ");
            pw.print(((RubyFixnum) literal).getLongValue());
            pw.print(")");
        }
    }

    /**
     * @see NodeVisitor#visitMAsgnNode(MAsgnNode)
     */
    public void visitMAsgnNode(MAsgnNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitMatch2Node(Match2Node)
     */
    public void visitMatch2Node(Match2Node iVisited) {
    }

    /**
     * @see NodeVisitor#visitMatch3Node(Match3Node)
     */
    public void visitMatch3Node(Match3Node iVisited) {
    }

    /**
     * @see NodeVisitor#visitMatchNode(MatchNode)
     */
    public void visitMatchNode(MatchNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitMethodNode(MethodNode)
     */
    public void visitMethodNode(MethodNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitModuleNode(ModuleNode)
     */
    public void visitModuleNode(ModuleNode iVisited) {
        pw.println("if (ruby.getRubyClass() == null) {");
        indent++;
        pw.print(indent());
        pw.println("throw new TypeError(ruby, \"no outer class/module\");");
        indent--;
        pw.print(indent());
        pw.println("}");
        
        pw.print(indent());
        pw.print("if ((ruby.getRubyClass() == ruby.getClasses().getObjectClass()) && ruby.isAutoloadDefined(\"");
        pw.print(iVisited.getClassNameId());
        pw.println("\")) {");
            // getRuby().rb_autoload_load(node.nd_cname());
        pw.print(indent());
        pw.println("}");
        
        pw.print(indent());
        pw.print("if (ruby.getRubyClass().isConstantDefined(\"");
        pw.print(iVisited.getClassNameId());
        pw.println("\")) {");
        indent++;
        pw.print(indent());
        pw.print("__module__ = (RubyModule) ruby.getRubyClass().getConstant(\"");
        pw.print(iVisited.getClassNameId());
        pw.println("\");");
            
            /*if (!(module instanceof RubyModule)) {
                throw new RubyTypeException(moduleName.toName() + " is not a module");
                
            }*/

        pw.print(indent());
		pw.println("if (ruby.getSafeLevel() >= 4) {");
		indent++;
		pw.print(indent());
		pw.println("throw new RubySecurityException(ruby, \"extending module prohibited\");");
		indent--;
		pw.print(indent());
		pw.println("}");
		indent--;
		pw.print(indent());
        pw.println("} else {");
        indent++;
        pw.print(indent());
        pw.print("__module__ = ruby.defineModule(\"");
        pw.print(iVisited.getClassNameId());
        pw.println("\");");
        pw.print(indent());
        pw.print("ruby.getRubyClass().setConstant(\"");
        pw.print(iVisited.getClassNameId());
        pw.println("\", __module__);");
        pw.print(indent());
        pw.print("__module__.setClassPath(ruby.getRubyClass(), \"");
        pw.print(iVisited.getClassNameId());
        pw.println("\");");
        indent--;
		pw.print(indent());
		pw.println("}");

		pw.print(indent());
		pw.println("if (ruby.getWrapper() != null) {");
		indent++;
		pw.print(indent());
        pw.println("__module__.getSingletonClass().includeModule(ruby.getWrapper());");
		pw.print(indent());
        pw.println("__module__.includeModule(ruby.getWrapper());");
        indent--;
		pw.print(indent());
		pw.println("}");
		
		printModuleScope((ScopeNode)iVisited.getBodyNode(), "__module__");
    }

    /**
     * @see NodeVisitor#visitNewlineNode(NewlineNode)
     */
    public void visitNewlineNode(NewlineNode iVisited) {
        iVisited.getNextNode().accept(this);
    }

    /**
     * @see NodeVisitor#visitNextNode(NextNode)
     */
    public void visitNextNode(NextNode iVisited) {
        // +++
        pw.print("continue");
        // ---
    }

    /**
     * @see NodeVisitor#visitNilNode(NilNode)
     */
    public void visitNilNode(NilNode iVisited) {
        pw.print("ruby.getNil()");
    }

    /**
     * @see NodeVisitor#visitNotNode(NotNode)
     */
    public void visitNotNode(NotNode iVisited) {
        iVisited.getBodyNode().accept(this);
        pw.print(".isTrue() ? ruby.getFalse() : ruby.getTrue()");
    }

    /**
     * @see NodeVisitor#visitNthRefNode(NthRefNode)
     */
    public void visitNthRefNode(NthRefNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitOpAsgn1Node(OpAsgn1Node)
     */
    public void visitOpAsgn1Node(OpAsgn1Node iVisited) {
    }

    /**
     * @see NodeVisitor#visitOpAsgn2Node(OpAsgn2Node)
     */
    public void visitOpAsgn2Node(OpAsgn2Node iVisited) {
    }

    /**
     * @see NodeVisitor#visitOpAsgnAndNode(OpAsgnAndNode)
     */
    public void visitOpAsgnAndNode(OpAsgnAndNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitOpAsgnOrNode(OpAsgnOrNode)
     */
    public void visitOpAsgnOrNode(OpAsgnOrNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitOptNNode(OptNNode)
     */
    public void visitOptNNode(OptNNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitOrNode(OrNode)
     */
    public void visitOrNode(OrNode iVisited) {
        iVisited.getFirstNode().accept(this);
        pw.print(".isTrue() ||");
        iVisited.getSecondNode().accept(this);
        pw.print(".isTrue()");
    }

    /**
     * @see NodeVisitor#visitPostExeNode(PostExeNode)
     */
    public void visitPostExeNode(PostExeNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitRedoNode(RedoNode)
     */
    public void visitRedoNode(RedoNode iVisited) {
        // +++
        pw.print("continue");
        // ---
    }

    /**
     * @see NodeVisitor#visitRescueBodyNode(RescueBodyNode)
     */
    public void visitRescueBodyNode(RescueBodyNode iVisited) {
        iVisited.getBodyNode().accept(this);
    }

    /**
     * @see NodeVisitor#visitRescueNode(RescueNode)
     */
    public void visitRescueNode(RescueNode iVisited) {
        // +++
        // pw.print(indent());
        // ---
        pw.println("while(true) {");
        pw.print(indent());
        pw.println("try {");
        indent++;
        // +++
        pw.print(indent());
        // ---
        iVisited.getHeadNode().accept(this);
        // +++
        pw.println(';');
        // ---
        if (iVisited.getElseNode() != null) {
            // +++
            pw.print(indent());
            // ---
            iVisited.getElseNode().accept(this);
            // +++
            pw.println(';');
            // ---
        }
        pw.print(indent());
        pw.println("break;");
        indent--;
        pw.print(indent());
        pw.println("} catch (RaiseException rExcptn) {");
        indent++;
        pw.print(indent());
        pw.println("ruby.setGlobalVar(\"$!\", rExcptn.getActException());");
        pw.println();

        Node body = iVisited.getResqNode();
        pw.print(indent());
        pw.println("RubyObject[] __rescue_args__ = null;");
        while (body != null) {
            if (body.getArgsNode() == null) {
                pw.print(indent());
                pw.println("if (rExcptn.getActException().kind_of(ruby.getExceptions().getStandardError()).isTrue()) {");
                indent++;
                pw.print(indent());
                body.accept(this);
                pw.println(";");
                pw.print(indent());
                pw.println("break;");
                indent--;
                pw.print(indent());
                pw.println("}");
            } else {
                pw.print(indent());
                pw.print("__rescue_args__ = new RubyObject[] {");
                for (Node node = body.getArgsNode(); node != null; node = node.getNextNode()) {
                    node.getHeadNode().accept(this);
                    if (node.getNextNode() != null) {
                        pw.print(", ");
                    }
                }
                pw.println("};");
                pw.print(indent());
                pw.println("for (int i = 0; i < __rescue_args__.length; i++) {");
                pw.print(indent());
                pw.println("if (__rescue_args__[i].kind_of(ruby.getClasses().getModuleClass()).isFalse()) {");
                indent++;
                pw.print(indent());
                pw.println("throw new TypeError(ruby, \"class or module required for rescue clause\");");
                indent--;
                pw.print(indent());
                pw.println("} else if (rExcptn.getActException().kind_of((RubyModule)__rescue_args__[i]).isTrue()) {");
                indent++;
                pw.print(indent());
                body.accept(this);
                pw.println(";");
                indent--;
                pw.print(indent());
                pw.println("}");
            }

            body = body.getHeadNode();
        }

        pw.print(indent());
        pw.println("throw rExcptn;");
        indent--;
        pw.print(indent());
        pw.println("} finally {");
        indent++;
        pw.print(indent());
        pw.println("ruby.setGlobalVar(\"$!\", ruby.getNil());");
        indent--;
        pw.print(indent());
        pw.println("}");
        pw.print(indent());
        pw.println("}");
    }

    /**
     * @see NodeVisitor#visitRestArgsNode(RestArgsNode)
     */
    public void visitRestArgsNode(RestArgsNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitRetryNode(RetryNode)
     */
    public void visitRetryNode(RetryNode iVisited) {
        // +++
        pw.print("continue");
        // ---
    }

    /**
     * @see NodeVisitor#visitReturnNode(ReturnNode)
     */
    public void visitReturnNode(ReturnNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitSClassNode(SClassNode)
     */
    public void visitSClassNode(SClassNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitScopeNode(ScopeNode)
     */
    public void visitScopeNode(ScopeNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitSelfNode(SelfNode)
     */
    public void visitSelfNode(SelfNode iVisited) {
        pw.print("self");
    }

    /**
     * @see NodeVisitor#visitStrNode(StrNode)
     */
    public void visitStrNode(StrNode iVisited) {
        pw.print("RubyString.newString(ruby, \"");
        pw.print(iVisited.getLiteral().to_s());
        pw.print("\")");
    }

    /**
     * @see NodeVisitor#visitSuperNode(SuperNode)
     */
    public void visitSuperNode(SuperNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitTrueNode(TrueNode)
     */
    public void visitTrueNode(TrueNode iVisited) {
        pw.print("ruby.getTrue()");
    }

    /**
     * @see NodeVisitor#visitUndefNode(UndefNode)
     */
    public void visitUndefNode(UndefNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitUntilNode(UntilNode)
     */
    public void visitUntilNode(UntilNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitVAliasNode(VAliasNode)
     */
    public void visitVAliasNode(VAliasNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitVCallNode(VCallNode)
     */
    public void visitVCallNode(VCallNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitWhenNode(WhenNode)
     */
    public void visitWhenNode(WhenNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitWhileNode(WhileNode)
     */
    public void visitWhileNode(WhileNode iVisited) {
        // +++
        // pw.print(indent());
        // ---
        pw.print("while (");
        iVisited.getConditionNode().accept(this);
        pw.println(".isTrue()) {");
        indent++;
        // +++
        pw.print(indent());
        // ---
        iVisited.getBodyNode().accept(this);
        // +++
        pw.println(';');
        // ---
        indent--;
        pw.print(indent());
        // +++
        // pw.println("}");
        pw.print("}");
        // ---
    }

    /**
     * @see NodeVisitor#visitXStrNode(XStrNode)
     */
    public void visitXStrNode(XStrNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitYieldNode(YieldNode)
     */
    public void visitYieldNode(YieldNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitZArrayNode(ZArrayNode)
     */
    public void visitZArrayNode(ZArrayNode iVisited) {
        pw.print("RubyArray.newArray(ruby)");
    }

    /**
     * @see NodeVisitor#visitZSuperNode(ZSuperNode)
     */
    public void visitZSuperNode(ZSuperNode iVisited) {
    }
    
    private void printModuleScope(ScopeNode scope, String module_name) {
        if (scope.getNextNode() == null) {
            return;
        }
        
        pw.print(indent());
        pw.println("ruby.pushClass();");
        pw.print(indent());
        pw.print("ruby.setRubyClass(");
        pw.print(module_name);
        pw.println(");");
        
        pw.print(indent());
        pw.print("ruby.setNamespace(new Namespace(");
        pw.print(module_name);
        pw.println(", ruby.getNamespace()));");
        
        pw.print(indent());
        pw.println("try {");
        indent++;
        
        if (scope.getTable() != null) {
            Iterator iter = scope.getTable().iterator();
            while (iter.hasNext()) {
                String name = (String) iter.next();
                if (name.equals("_")) {
                    name = "__line__";
                } else if (name.equals("~")) {
                    name = "__match__";
                }
                pw.print(indent());
                pw.println("RubyObject " + name + " = ruby.getNil();");
            }
            pw.println();
        }
        
      	pw.print(indent());
      	scope.getNextNode().accept(this);
       	pw.println(";");
        
        indent--;
        pw.print(indent());
        pw.println("} finally {");
        indent++;
        pw.print(indent());
        pw.println("ruby.setNamespace(ruby.getNamespace().getParent());");
        pw.print(indent());
        pw.println("ruby.popClass();");
        indent--;
        pw.print(indent());
        pw.println("}");
    }
    
    public void printMethods() {
        Iterator iter = methodMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            printMethod((String)entry.getKey(), (ScopeNode)entry.getValue());
        }
    }

    public void printMethod(String name, ScopeNode body) {
        indent = 1;
        pw.print(indent());
        pw.println("private static Namespace " + name + "ns__ = null;");
        pw.println();
        pw.print(indent());
        pw.print("public static RubyObject ");
        pw.print(name);
        pw.println("(Ruby ruby, RubyObject self, RubyObject[] __args__) {");
        indent++;

        pw.print(indent());
        pw.println("RubyModule __module__ = null;");
        pw.print(indent());
        pw.println("RubyClass __class__ = null;");
        pw.print(indent());
        pw.println("MethodNode __body__ = null;");
        pw.print(indent());
        pw.println("int __noex__ = 0;");
        pw.print(indent());
        pw.println("RubyObject __recv__ = null;");
        
        if (body.getTable() != null) {
            Iterator iter = body.getTable().iterator();
            while (iter.hasNext()) {
                String local_name = (String) iter.next();
                if (local_name.equals("_")) {
                    local_name = "__line__";
                } else if (local_name.equals("~")) {
                    local_name = "__match__";
                }
                pw.print(indent());
                pw.println("RubyObject " + local_name + " = ruby.getNil();");
            }
            pw.println();
        }

        pw.print(indent());
        pw.println("Namespace savedNamespace = null;");
        pw.print(indent());
        pw.println("if (" + name + "ns__ != null) {");
        pw.print(indent());
        pw.println("savedNamespace = ruby.getNamespace();");
        pw.print(indent());
        pw.println("ruby.setNamespace(" + name + "ns__);");
        pw.print(indent());
        pw.println("}");
        pw.println();

        Node callBody = body.getNextNode();
        Node callNode = null;
        if (callBody.getType() == Constants.NODE_ARGS) {
            callNode = callBody;
            callBody = null;
        } else if (callBody.getType() == Constants.NODE_BLOCK) {
            callNode = callBody.getHeadNode();
            callBody = callBody.getNextNode();
        }
        
        if (callNode != null) {
        	int i = callNode.getCount();
           	/*if (i > (args != null ? args.size() : 0)) {
           		int size = 0;
           		if (args != null) {
               		size = args.size();
           		}
                throw new RubyArgumentException(ruby, getFile() + ":" + getLine() + "wrong # of arguments(" + size + " for " + i + ")");
            }*/
            if (callNode.getRest() == -1) {
            	int opt = i;
                Node optNode = callNode.getOptNode();

                while (optNode != null) {
                	opt++;
                    optNode = optNode.getNextNode();
                }
                /*if (opt < (args != null ? args.size() : 0)) {
                        throw new RubyArgumentException(ruby, "wrong # of arguments(" + args.size() + " for " + opt + ")");
                    }*/

                // +++
                //ruby.getRubyFrame().setArgs(valueList != null ? new DelegateList(valueList, 2, valueList.size()) : null);
                // ---
            }

                if (body.getTable() != null) {
                    if (i > 0) {
                        for (int j = 0; j < i; j++) {
                            pw.print(indent());
                            pw.print(body.getTable().get(j + 2));
                            pw.print(" = __args__[");
                            pw.print(j);
                            pw.println("];");
                        }
                    }

                    // args.inc(i);

                    /*if (callNode.getOptNode() != null) {
                        Node optNode = callNode.getOptNode();

                        while (optNode != null && args.size() != 0) {
                            ((AssignableNode) optNode.getHeadNode()).assign(ruby, recv, args.getRuby(0), true);
                            args.inc(1);
                            optNode = optNode.getNextNode();
                        }
                        recv.eval(optNode);
                    }
                    if (callNode.getRest() >= 0) {
                        RubyArray array = null;
                        if (args.size() > 0) {
                            array = RubyArray.newArray(ruby, args);
                        } else {
                            array = RubyArray.newArray(ruby);
                        }
                        valueList.set(callNode.getRest(), array);
                    }*/
                }
            }

		pw.print(indent());
        pw.println("try {");
        
        pw.print(indent());
        pw.print("return ");
        
        ruby.getScope().push();
        ruby.getScope().setLocalNames(body.getTable());
                
        callBody.accept(this);
        //pw.println(";");
        
        ruby.getScope().pop();
            
        pw.print(indent());
        pw.println("} finally {");
        pw.print(indent());
        pw.println("if (savedNamespace != null) {");
        pw.print(indent());
		pw.println("ruby.setNamespace(savedNamespace);");
		pw.print(indent());
		pw.println("}");
		pw.print(indent());
        pw.println("}");

        indent--;
        pw.print(indent());
        pw.println("}");
        pw.println();
    }
}