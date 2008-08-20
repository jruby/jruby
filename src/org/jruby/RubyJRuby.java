/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2005 Thomas E Enebo <enebo@acm.org>
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
package org.jruby;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.anno.JRubyClass;

import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArgumentNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaObject;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import org.jruby.internal.runtime.methods.DynamicMethod;

import org.jruby.ast.Node;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.ASTCompiler;
import org.jruby.compiler.impl.StandardASMCompiler;
import org.jruby.internal.runtime.methods.MethodArgs;
import org.jruby.runtime.InterpretedBlock;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.TypeConverter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

/**
 * Module which defines JRuby-specific methods for use. 
 */
@JRubyModule(name="JRuby")
public class RubyJRuby {
    public static RubyModule createJRuby(Ruby runtime) {
        ThreadContext context = runtime.getCurrentContext();
        runtime.getKernel().callMethod(context, "require", runtime.newString("java"));
        RubyModule jrubyModule = runtime.defineModule("JRuby");
        
        jrubyModule.defineAnnotatedMethods(RubyJRuby.class);

        RubyClass compiledScriptClass = jrubyModule.defineClassUnder("CompiledScript",runtime.getObject(), runtime.getObject().getAllocator());

        compiledScriptClass.attr_accessor(context, new IRubyObject[]{runtime.newSymbol("name"), runtime.newSymbol("class_name"), runtime.newSymbol("original_script"), runtime.newSymbol("code")});
        compiledScriptClass.defineAnnotatedMethods(JRubyCompiledScript.class);

        return jrubyModule;
    }

    public static RubyModule createJRubyExt(Ruby runtime) {
        runtime.getKernel().callMethod(runtime.getCurrentContext(),"require", runtime.newString("java"));
        RubyModule mJRubyExt = runtime.getOrCreateModule("JRuby").defineModuleUnder("Extensions");
        
        mJRubyExt.defineAnnotatedMethods(JRubyExtensions.class);

        runtime.getObject().includeModule(mJRubyExt);

        return mJRubyExt;
    }

    public static class ExtLibrary implements Library {
        public void load(Ruby runtime, boolean wrap) throws IOException {
            RubyJRuby.createJRubyExt(runtime);
            
            runtime.getMethod().defineAnnotatedMethods(MethodExtensions.class);
        }
    }
    
    public static class TypeLibrary implements Library {
        public void load(Ruby runtime, boolean wrap) throws IOException {
            RubyModule jrubyType = runtime.defineModule("Type");
            jrubyType.defineAnnotatedMethods(TypeLibrary.class);
        }
        
        @JRubyMethod(module = true)
        public static IRubyObject coerce_to(ThreadContext context, IRubyObject self, IRubyObject object, IRubyObject clazz, IRubyObject method) {
            Ruby ruby = object.getRuntime();
            
            if (!(clazz instanceof RubyClass)) {
                throw ruby.newTypeError(clazz, ruby.getClassClass());
            }
            if (!(method instanceof RubySymbol)) {
                throw ruby.newTypeError(method, ruby.getSymbol());
            }
            
            RubyClass rubyClass = (RubyClass)clazz;
            RubySymbol methodSym = (RubySymbol)method;
            
            return TypeConverter.convertToTypeOrRaise(object, rubyClass, methodSym.asJavaString());
        }
    }
    
    @JRubyMethod(name = "runtime", frame = true, module = true)
    public static IRubyObject runtime(IRubyObject recv, Block unusedBlock) {
        return Java.java_to_ruby(recv, JavaObject.wrap(recv.getRuntime(), recv.getRuntime()), Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "objectspace", frame = true, module = true)
    public static IRubyObject getObjectSpaceEnabled(IRubyObject recv, Block b) {
        Ruby runtime = recv.getRuntime();
        return RubyBoolean.newBoolean(
                runtime, runtime.isObjectSpaceEnabled());
    }

    @JRubyMethod(name = "objectspace=", required = 1, frame = true, module = true)
    public static IRubyObject setObjectSpaceEnabled(
            IRubyObject recv, IRubyObject arg, Block b) {
        Ruby runtime = recv.getRuntime();
        runtime.setObjectSpaceEnabled(arg.isTrue());
        return runtime.getNil();
    }

    @JRubyMethod(name = {"parse", "ast_for"}, optional = 3, frame = true, module = true)
    public static IRubyObject parse(IRubyObject recv, IRubyObject[] args, Block block) {
        if(block.isGiven()) {
            if(block.getBody() instanceof org.jruby.runtime.CompiledBlock) {
                throw new RuntimeException("Cannot compile an already compiled block. Use -J-Djruby.jit.enabled=false to avoid this problem.");
            }
            Arity.checkArgumentCount(recv.getRuntime(),args,0,0);
            return Java.java_to_ruby(recv, JavaObject.wrap(recv.getRuntime(), ((InterpretedBlock)block.getBody()).getIterNode().getBodyNode()), Block.NULL_BLOCK);
        } else {
            Arity.checkArgumentCount(recv.getRuntime(),args,1,3);
            String filename = "-";
            boolean extraPositionInformation = false;
            RubyString content = args[0].convertToString();
            if(args.length>1) {
                filename = args[1].convertToString().toString();
                if(args.length>2) {
                    extraPositionInformation = args[2].isTrue();
                }
            }
            return Java.java_to_ruby(recv, JavaObject.wrap(recv.getRuntime(), 
               recv.getRuntime().parse(content.getByteList(), filename, null, 0, extraPositionInformation)), Block.NULL_BLOCK);
        }
    }

    @JRubyMethod(name = "compile", optional = 3, frame = true, module = true)
    public static IRubyObject compile(IRubyObject recv, IRubyObject[] args, Block block) {
        Node node;
        String filename;
        RubyString content;
        if(block.isGiven()) {
            Arity.checkArgumentCount(recv.getRuntime(),args,0,0);
            if(block.getBody() instanceof org.jruby.runtime.CompiledBlock) {
                throw new RuntimeException("Cannot compile an already compiled block. Use -J-Djruby.jit.enabled=false to avoid this problem.");
            }
            content = RubyString.newEmptyString(recv.getRuntime());
            Node bnode = ((InterpretedBlock)block.getBody()).getIterNode().getBodyNode();
            node = new org.jruby.ast.RootNode(bnode.getPosition(), block.getBinding().getDynamicScope(), bnode);
            filename = "__block_" + node.getPosition().getFile();
        } else {
            Arity.checkArgumentCount(recv.getRuntime(),args,1,3);
            filename = "-";
            boolean extraPositionInformation = false;
            content = args[0].convertToString();
            if(args.length>1) {
                filename = args[1].convertToString().toString();
                if(args.length>2) {
                    extraPositionInformation = args[2].isTrue();
                }
            }

            node = recv.getRuntime().parse(content.getByteList(), filename, null, 0, extraPositionInformation);
        }

        String classname;
        if (filename.equals("-e")) {
            classname = "__dash_e__";
        } else {
            classname = filename.replace('\\', '/').replaceAll(".rb", "").replaceAll("-","_dash_");
        }

        ASTInspector inspector = new ASTInspector();
        inspector.inspect(node);
            
        StandardASMCompiler asmCompiler = new StandardASMCompiler(classname, filename);
        ASTCompiler compiler = new ASTCompiler();
        compiler.compileRoot(node, asmCompiler, inspector);
        byte[] bts = asmCompiler.getClassByteArray();

        IRubyObject compiledScript = ((RubyModule)recv).fastGetConstant("CompiledScript").callMethod(recv.getRuntime().getCurrentContext(),"new");
        compiledScript.callMethod(recv.getRuntime().getCurrentContext(), "name=", recv.getRuntime().newString(filename));
        compiledScript.callMethod(recv.getRuntime().getCurrentContext(), "class_name=", recv.getRuntime().newString(classname));
        compiledScript.callMethod(recv.getRuntime().getCurrentContext(), "original_script=", content);
        compiledScript.callMethod(recv.getRuntime().getCurrentContext(), "code=", Java.java_to_ruby(recv, JavaObject.wrap(recv.getRuntime(), bts), Block.NULL_BLOCK));

        return compiledScript;
    }

    @JRubyMethod(name = "reference", required = 1, module = true)
    public static IRubyObject reference(IRubyObject recv, IRubyObject obj) {
        return Java.wrap(recv.getRuntime().getJavaSupport().getJavaUtilitiesModule(),
                JavaObject.wrap(recv.getRuntime(), obj));
    }

    @JRubyClass(name="JRuby::CompiledScript")
    public static class JRubyCompiledScript {
        @JRubyMethod(name = "to_s")
        public static IRubyObject compiled_script_to_s(IRubyObject recv) {
            return recv.getInstanceVariables().fastGetInstanceVariable("@original_script");
        }

        @JRubyMethod(name = "inspect")
        public static IRubyObject compiled_script_inspect(IRubyObject recv) {
            return recv.getRuntime().newString("#<JRuby::CompiledScript " + recv.getInstanceVariables().fastGetInstanceVariable("@name") + ">");
        }

        @JRubyMethod(name = "inspect_bytecode")
        public static IRubyObject compiled_script_inspect_bytecode(IRubyObject recv) {
            StringWriter sw = new StringWriter();
            ClassReader cr = new ClassReader((byte[])org.jruby.javasupport.JavaUtil.convertRubyToJava(recv.getInstanceVariables().fastGetInstanceVariable("@code"),byte[].class));
            TraceClassVisitor cv = new TraceClassVisitor(new PrintWriter(sw));
            cr.accept(cv, ClassReader.SKIP_DEBUG);
            return recv.getRuntime().newString(sw.toString());
        }
    }

    @JRubyModule(name="JRubyExtensions")
    public static class JRubyExtensions {
        @JRubyMethod(name = "steal_method", required = 2, module = true)
        public static IRubyObject steal_method(IRubyObject recv, IRubyObject type, IRubyObject methodName) {
            RubyModule to_add = null;
            if(recv instanceof RubyModule) {
                to_add = (RubyModule)recv;
            } else {
                to_add = recv.getSingletonClass();
            }
            String name = methodName.toString();
            if(!(type instanceof RubyModule)) {
                throw recv.getRuntime().newArgumentError("First argument must be a module/class");
            }

            DynamicMethod method = ((RubyModule)type).searchMethod(name);
            if(method == null || method.isUndefined()) {
                throw recv.getRuntime().newArgumentError("No such method " + name + " on " + type);
            }

            to_add.addMethod(name, method);
            return recv.getRuntime().getNil();
        }

        @JRubyMethod(name = "steal_methods", required = 1, rest = true, module = true)
        public static IRubyObject steal_methods(IRubyObject recv, IRubyObject[] args) {
            IRubyObject type = args[0];
            for(int i=1;i<args.length;i++) {
                steal_method(recv, type, args[i]);
            }
            return recv.getRuntime().getNil();
        }
    }
    
    public static class MethodExtensions {
        @JRubyMethod(name = "args")
        public static IRubyObject methodArgs(IRubyObject recv) {
            Ruby ruby = recv.getRuntime();
            RubyMethod rubyMethod = (RubyMethod)recv;
            
            DynamicMethod method = rubyMethod.method;
            
            if (method instanceof MethodArgs) {
                MethodArgs interpMethod = (MethodArgs)method;
                ArgsNode args = interpMethod.getArgsNode();
                RubyArray argsArray = RubyArray.newArray(ruby);
                
                RubyArray reqArray = RubyArray.newArray(ruby);
                ListNode requiredArgs = args.getArgs();
                for (int i = 0; requiredArgs != null && i < requiredArgs.size(); i++) {
                    ArgumentNode arg = (ArgumentNode)requiredArgs.get(i);
                    reqArray.append(RubySymbol.newSymbol(ruby, arg.getName()));
                }
                argsArray.append(reqArray);
                
                RubyArray optArray = RubyArray.newArray(ruby);
                ListNode optArgs = args.getOptArgs();
                for (int i = 0; optArgs != null && i < optArgs.size(); i++) {
                    LocalAsgnNode arg = (LocalAsgnNode)optArgs.get(i);
                    optArray.append(RubySymbol.newSymbol(ruby, arg.getName()));
                }
                argsArray.append(optArray);
                
                if (args.getRestArgNode() != null) {
                    argsArray.append(RubySymbol.newSymbol(ruby, args.getRestArgNode().getName()));
                } else {
                    argsArray.append(ruby.getNil());
                }
                
                if (args.getBlockArgNode() != null) {
                    argsArray.append(RubySymbol.newSymbol(ruby, args.getBlockArgNode().getName()));
                } else {
                    argsArray.append(ruby.getNil());
                }
                
                return argsArray;
            }
            
            throw ruby.newTypeError("Method args are only available for standard interpreted or jitted methods");
        }
    }
}
