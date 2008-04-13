package org.jruby.anno;

import com.sun.mirror.apt.*;
import com.sun.mirror.declaration.*;
import com.sun.mirror.type.*;
import com.sun.mirror.util.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.CompatVersion;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.InvocationMethodFactory;
import org.jruby.runtime.Arity;

import static java.util.Collections.*;
import static com.sun.mirror.util.DeclarationVisitors.*;

/*
 * This class is used to run an annotation processor that lists class
 * names.  The functionality of the processor is analogous to the
 * ListClass doclet in the Doclet Overview.
 */
public class AnnotationBinder implements AnnotationProcessorFactory {
    // Process any set of annotations
    private static final Collection<String> supportedAnnotations
        = unmodifiableCollection(Arrays.asList("org.jruby.anno.JRubyMethod", "org.jruby.anno.JRubyClass"));

    // No supported options
    private static final Collection<String> supportedOptions = emptySet();

    public Collection<String> supportedAnnotationTypes() {
        return supportedAnnotations;
    }

    public Collection<String> supportedOptions() {
        return supportedOptions;
    }

    public AnnotationProcessor getProcessorFor(
            Set<AnnotationTypeDeclaration> atds,
            AnnotationProcessorEnvironment env) {
        return new AnnotationBindingProcessor(env);
    }

    private static class AnnotationBindingProcessor implements AnnotationProcessor {
        private final AnnotationProcessorEnvironment env;
        AnnotationBindingProcessor(AnnotationProcessorEnvironment env) {
            this.env = env;
        }

        public void process() {
	    for (TypeDeclaration typeDecl : env.getSpecifiedTypeDeclarations())
		typeDecl.accept(getDeclarationScanner(new RubyClassVisitor(),
						      NO_OP));
        }

	private static class RubyClassVisitor extends SimpleDeclarationVisitor {
            private PrintStream out;
            private static final boolean DEBUG = false;
            public void visitClassDeclaration(ClassDeclaration cd) {
                JRubyClass jrubyClass = cd.getAnnotation(JRubyClass.class);
                try {
                    if (jrubyClass != null) {
                        FileOutputStream fos = new FileOutputStream("src_gen/" + cd.getSimpleName() + "Populator.java");
                        out = new PrintStream(fos);

                        // start a new populator
                        out.println("/* THIS FILE IS GENERATED. DO NOT EDIT */");
                        //out.println("package org.jruby.anno;");

                        out.println("import org.jruby.RubyModule;");
                        out.println("import org.jruby.anno.TypePopulator;");
                        out.println("import org.jruby.internal.runtime.methods.CallConfiguration;");
                        out.println("import org.jruby.internal.runtime.methods.JavaMethod;");
                        out.println("import org.jruby.runtime.Arity;");
                        out.println("import org.jruby.runtime.Visibility;");

                        out.println("public class " + cd.getSimpleName() + "Populator implements TypePopulator {");
                        out.println("    public void populate(RubyModule cls) {");
                        if (DEBUG) out.println("        System.out.println(\"Using pregenerated populator: \" + \"" + cd.getSimpleName() + "Populator\");");
                        out.println("        JavaMethod javaMethod;");
                        
                        Map<String, List<MethodDeclaration>> annotatedMethods = new HashMap();
                        Map<String, List<MethodDeclaration>> staticAnnotatedMethods = new HashMap();
                        Map<String, List<MethodDeclaration>> annotatedMethods1_8 = new HashMap();
                        Map<String, List<MethodDeclaration>> staticAnnotatedMethods1_8 = new HashMap();
                        Map<String, List<MethodDeclaration>> annotatedMethods1_9 = new HashMap();
                        Map<String, List<MethodDeclaration>> staticAnnotatedMethods1_9 = new HashMap();
                        
                        for (MethodDeclaration md : cd.getMethods()) {
                            JRubyMethod anno = md.getAnnotation(JRubyMethod.class);
                            if (anno == null || anno.compat() == CompatVersion.RUBY1_9) continue;

                            String name = anno.name().length == 0 ? md.getSimpleName() : anno.name()[0];

                            List<MethodDeclaration> methodDescs;
                            Map<String, List<MethodDeclaration>> methodsHash = null;
                            if (md.getModifiers().contains(Modifier.STATIC)) {
                                if (anno.compat() == CompatVersion.RUBY1_8) {
                                    methodsHash = staticAnnotatedMethods1_8;
                                } else if (anno.compat() == CompatVersion.RUBY1_9) {
                                    methodsHash = staticAnnotatedMethods1_9;
                                } else {
                                    methodsHash = staticAnnotatedMethods;
                                }
                            } else {
                                if (anno.compat() == CompatVersion.RUBY1_8) {
                                    methodsHash = annotatedMethods1_8;
                                } else if (anno.compat() == CompatVersion.RUBY1_9) {
                                    methodsHash = annotatedMethods1_9;
                                } else {
                                    methodsHash = annotatedMethods;
                                }
                            }

                            methodDescs = methodsHash.get(name);
                            if (methodDescs == null) {
                                methodDescs = new ArrayList();
                                methodsHash.put(name, methodDescs);
                            }

                            methodDescs.add(md);
                        }
                        
                        processMethodDeclarations(staticAnnotatedMethods);
                        processMethodDeclarations(staticAnnotatedMethods1_8);
                        processMethodDeclarations(staticAnnotatedMethods1_9);
                        processMethodDeclarations(annotatedMethods);
                        processMethodDeclarations(annotatedMethods1_8);
                        processMethodDeclarations(annotatedMethods1_9);
                        
                        out.println("    }");
                        out.println("}");
                        out.close();
                        out = null;
                    }
                } catch (IOException ioe) {
                    System.err.println("FAILED TO GENERATE:");
                    ioe.printStackTrace();
                    System.exit(1);
                }
            }
            
            public void processMethodDeclarations(Map<String, List<MethodDeclaration>> declarations) {
                for (Map.Entry<String, List<MethodDeclaration>> entry : declarations.entrySet()) {
                    List<MethodDeclaration> list = entry.getValue();
                    
                    if (list.size() == 1) {
                        // single method, use normal logic
                        processMethodDeclaration(list.get(0));
                    } else {
                        // multimethod, new logic
                        processMethodDeclarationMulti(list.get(0));
                    }
                }
            }
            
            public void processMethodDeclaration(MethodDeclaration md) {
                JRubyMethod anno = md.getAnnotation(JRubyMethod.class);
                if (anno != null && out != null) {
                    boolean isStatic = md.getModifiers().contains(Modifier.STATIC);
                    
                    // declared type returns the qualified name without $ for inner classes!!!
                    String qualifiedName;
                    if (md.getDeclaringType().getDeclaringType() != null) {
                        // inner class, use $ to delimit
                        if (md.getDeclaringType().getDeclaringType().getDeclaringType() != null) {
                            qualifiedName = md.getDeclaringType().getDeclaringType().getDeclaringType().getQualifiedName() + "$" + md.getDeclaringType().getDeclaringType().getSimpleName() + "$" + md.getDeclaringType().getSimpleName();
                        } else {
                            qualifiedName = md.getDeclaringType().getDeclaringType().getQualifiedName() + "$" + md.getDeclaringType().getSimpleName();
                        }
                    } else {
                        qualifiedName = md.getDeclaringType().getQualifiedName();
                    }
                    
                    boolean hasContext = false;
                    boolean hasBlock = false;
                    
                    for (ParameterDeclaration pd : md.getParameters()) {
                        hasContext |= pd.getType().toString().equals("org.jruby.runtime.ThreadContext");
                        hasBlock |= pd.getType().toString().equals("org.jruby.runtime.Block");
                    }
                    
                    int actualRequired = calculateActualRequired(md.getParameters().size(), anno.optional(), anno.rest(), isStatic, hasContext, hasBlock);
                    
                    String annotatedBindingName = InvocationMethodFactory.getAnnotatedBindingClassName(
                            md.getSimpleName(),
                            qualifiedName,
                            isStatic,
                            actualRequired,
                            anno.optional(),
                            false);
                    
                    out.println("        javaMethod = new " + annotatedBindingName + "(cls, Visibility." + anno.visibility() + ");");
                    out.println("        javaMethod.setArity(Arity.createArity(" + Arity.fromAnnotation(anno).getValue() + "));");
                    out.println("        javaMethod.setJavaName(\"" + md.getSimpleName() + "\");");
                    out.println("        javaMethod.setSingleton(" + isStatic + ");");
                    out.println("        javaMethod.setCallConfig(CallConfiguration." + CallConfiguration.getCallConfigByAnno(anno).name() + ");");
                    generateMethodAddCalls(md, anno);
                }
            }
            
            public void processMethodDeclarationMulti(MethodDeclaration md) {
                JRubyMethod anno = md.getAnnotation(JRubyMethod.class);
                if (anno != null && out != null) {
                    boolean isStatic = md.getModifiers().contains(Modifier.STATIC);
                    
                    // declared type returns the qualified name without $ for inner classes!!!
                    String qualifiedName;
                    if (md.getDeclaringType().getDeclaringType() != null) {
                        // inner class, use $ to delimit
                        if (md.getDeclaringType().getDeclaringType().getDeclaringType() != null) {
                            qualifiedName = md.getDeclaringType().getDeclaringType().getDeclaringType().getQualifiedName() + "$" + md.getDeclaringType().getDeclaringType().getSimpleName() + "$" + md.getDeclaringType().getSimpleName();
                        } else {
                            qualifiedName = md.getDeclaringType().getDeclaringType().getQualifiedName() + "$" + md.getDeclaringType().getSimpleName();
                        }
                    } else {
                        qualifiedName = md.getDeclaringType().getQualifiedName();
                    }
                    
                    boolean hasContext = false;
                    boolean hasBlock = false;
                    
                    for (ParameterDeclaration pd : md.getParameters()) {
                        hasContext |= pd.getType().toString().equals("org.jruby.runtime.ThreadContext");
                        hasBlock |= pd.getType().toString().equals("org.jruby.runtime.Block");
                    }
                    
                    int actualRequired = calculateActualRequired(md.getParameters().size(), anno.optional(), anno.rest(), isStatic, hasContext, hasBlock);
                    
                    String annotatedBindingName = InvocationMethodFactory.getAnnotatedBindingClassName(
                            md.getSimpleName(),
                            qualifiedName,
                            isStatic,
                            actualRequired,
                            anno.optional(),
                            true);
                    
                    out.println("        javaMethod = new " + annotatedBindingName + "(cls, Visibility." + anno.visibility() + ");");
                    out.println("        javaMethod.setArity(Arity.OPTIONAL);");
                    out.println("        javaMethod.setJavaName(\"" + md.getSimpleName() + "\");");
                    out.println("        javaMethod.setSingleton(" + isStatic + ");");
                    out.println("        javaMethod.setCallConfig(CallConfiguration." + CallConfiguration.getCallConfigByAnno(anno).name() + ");");
                    generateMethodAddCalls(md, anno);
                }
            }
            
            private int calculateActualRequired(int paramsLength, int optional, boolean rest, boolean isStatic, boolean hasContext, boolean hasBlock) {
                int actualRequired;
                if (optional == 0 && !rest) {
                    int args = paramsLength;
                    if (args == 0) {
                        actualRequired = 0;
                    } else {
                        if (isStatic) args--;
                        if (hasContext) args--;
                        if (hasBlock) args--;

                        // TODO: confirm expected args are IRubyObject (or similar)
                        actualRequired = args;
                    }
                } else {
                    // optional args, so we have IRubyObject[]
                    // TODO: confirm
                    int args = paramsLength;
                    if (args == 0) {
                        actualRequired = 0;
                    } else {
                        if (isStatic) args--;
                        if (hasContext) args--;
                        if (hasBlock) args--;

                        // minus one more for IRubyObject[]
                        args--;

                        // TODO: confirm expected args are IRubyObject (or similar)
                        actualRequired = args;
                    }

                    if (actualRequired != 0) {
                        throw new RuntimeException("Combining specific args with IRubyObject[] is not yet supported");
                    }
                }
                
                return actualRequired;
            }
            
            public void generateMethodAddCalls(MethodDeclaration md, JRubyMethod jrubyMethod) {
                // TODO: This information
                if (jrubyMethod.frame()) {
                    for (String name : jrubyMethod.name()) {
                        out.println("        org.jruby.compiler.ASTInspector.FRAME_AWARE_METHODS.add(\"" + name + "\");");
                    }
                }
                // TODO: compat version
//                if(jrubyMethod.compat() == CompatVersion.BOTH ||
//                        module.getRuntime().getInstanceConfig().getCompatVersion() == jrubyMethod.compat()) {
                //RubyModule metaClass = module.metaClass;

                if (jrubyMethod.meta()) {
                    String baseName;
                    if (jrubyMethod.name().length == 0) {
                        baseName = md.getSimpleName();
                        out.println("        cls.getMetaClass().addMethod(\"" + baseName + "\", javaMethod);");
                    } else {
                        baseName = jrubyMethod.name()[0];
                        for (String name : jrubyMethod.name()) {
                            out.println("        cls.getMetaClass().addMethod(\"" + name + "\", javaMethod);");
                        }
                    }

                    if (jrubyMethod.alias().length > 0) {
                        for (String alias : jrubyMethod.alias()) {
                            out.println("        cls.getMetaClass().defineAlias(\"" + alias +"\", \"" + baseName + "\");");
                        }
                    }
                } else {
                    String baseName;
                    if (jrubyMethod.name().length == 0) {
                        baseName = md.getSimpleName();
                        out.println("        cls.addMethod(\"" + baseName + "\", javaMethod);");
                    } else {
                        baseName = jrubyMethod.name()[0];
                        for (String name : jrubyMethod.name()) {
                            out.println("        cls.addMethod(\"" + name + "\", javaMethod);");
                        }
                    }

                    if (jrubyMethod.alias().length > 0) {
                        for (String alias : jrubyMethod.alias()) {
                            out.println("        cls.defineAlias(\"" + alias +"\", \"" + baseName + "\");");
                        }
                    }

                    if (jrubyMethod.module()) {
                        // module/singleton methods are all defined public
                        out.println("        moduleMethod = dynamicMethod.dup();");
                        out.println("        moduleMethod.setVisibility(Visibility.PUBLIC);");

//                        RubyModule singletonClass = module.getSingletonClass();

                        if (jrubyMethod.name().length == 0) {
                            baseName = md.getSimpleName();
                            out.println("        cls.getSingletonClass().addMethod(\"" + baseName + "\", moduleMethod);");
                        } else {
                            baseName = jrubyMethod.name()[0];
                            for (String name : jrubyMethod.name()) {
                                out.println("        cls.getSingletonClass().addMethod(\"" + name + "\", moduleMethod);");
                            }
                        }

                        if (jrubyMethod.alias().length > 0) {
                            for (String alias : jrubyMethod.alias()) {
                                out.println("        cls.getSingletonClass().defineAlias(\"" + alias + "\", \"" + baseName + "\");");
                            }
                        }
                    }
                }
//                }
            }
	}
    }
}
