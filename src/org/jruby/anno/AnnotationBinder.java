package org.jruby.anno;

import com.sun.mirror.apt.*;
import com.sun.mirror.declaration.*;
import com.sun.mirror.util.*;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
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
import org.jruby.util.CodegenUtils;
import static java.util.Collections.*;
import static com.sun.mirror.util.DeclarationVisitors.*;

/*
 * This class is used to run an annotation processor that lists class
 * names.  The functionality of the processor is analogous to the
 * ListClass doclet in the Doclet Overview.
 */
public class AnnotationBinder implements AnnotationProcessorFactory {
    // Process any set of annotations
    private static final Collection<String> supportedAnnotations = unmodifiableCollection(Arrays.asList("org.jruby.anno.JRubyMethod", "org.jruby.anno.JRubyClass"));    // No supported options
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
        private final List<String> classNames = new ArrayList<String>();

        AnnotationBindingProcessor(AnnotationProcessorEnvironment env) {
            this.env = env;
        }

        public void process() {
            for (TypeDeclaration typeDecl : env.getSpecifiedTypeDeclarations()) {
                typeDecl.accept(getDeclarationScanner(new RubyClassVisitor(),
                        NO_OP));
            }
            try {
                FileWriter fw = new FileWriter("src_gen/annotated_classes.txt");
                for (String name : classNames) {
                    fw.write(name);
                    fw.write('\n');
                }
                fw.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private class RubyClassVisitor extends SimpleDeclarationVisitor {

            private PrintStream out;
            private static final boolean DEBUG = false;

            @Override
            public void visitClassDeclaration(ClassDeclaration cd) {
                try {
                    String qualifiedName = cd.getQualifiedName().replace('.', '$');

                    // skip anything not related to jruby
                    if (!qualifiedName.contains("org$jruby")) {
                        return;
                    }
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream(1024);
                    out = new PrintStream(bytes);

                    // start a new populator
                    out.println("/* THIS FILE IS GENERATED. DO NOT EDIT */");
                    out.println("package org.jruby.gen;");

                    out.println("import org.jruby.RubyModule;");
                    out.println("import org.jruby.RubyClass;");
                    out.println("import org.jruby.CompatVersion;");
                    out.println("import org.jruby.anno.TypePopulator;");
                    out.println("import org.jruby.internal.runtime.methods.CallConfiguration;");
                    out.println("import org.jruby.internal.runtime.methods.JavaMethod;");
                    out.println("import org.jruby.internal.runtime.methods.DynamicMethod;");
                    out.println("import org.jruby.runtime.Arity;");
                    out.println("import org.jruby.runtime.Visibility;");
                    out.println("import org.jruby.compiler.ASTInspector;");

                    out.println("public class " + qualifiedName + "$Populator extends TypePopulator {");
                    out.println("    public void populate(RubyModule cls, Class clazz) {");
                    if (DEBUG) {
                        out.println("        System.out.println(\"Using pregenerated populator: \" + \"" + cd.getSimpleName() + "Populator\");");
                    }
                    out.println("        JavaMethod javaMethod;");
                    out.println("        DynamicMethod moduleMethod;");
                    out.println("        RubyClass metaClass = cls.getMetaClass();");
                    out.println("        RubyModule singletonClass;");
                    out.println("        CompatVersion compatVersion = cls.getRuntime().getInstanceConfig().getCompatVersion();");

                    Map<String, List<MethodDeclaration>> annotatedMethods = new HashMap<String, List<MethodDeclaration>>();
                    Map<String, List<MethodDeclaration>> staticAnnotatedMethods = new HashMap<String, List<MethodDeclaration>>();
                    Map<String, List<MethodDeclaration>> annotatedMethods1_8 = new HashMap<String, List<MethodDeclaration>>();
                    Map<String, List<MethodDeclaration>> staticAnnotatedMethods1_8 = new HashMap<String, List<MethodDeclaration>>();
                    Map<String, List<MethodDeclaration>> annotatedMethods1_9 = new HashMap<String, List<MethodDeclaration>>();
                    Map<String, List<MethodDeclaration>> staticAnnotatedMethods1_9 = new HashMap<String, List<MethodDeclaration>>();

                    List<String> frameAwareMethods = new ArrayList<String>();

                    int methodCount = 0;
                    for (MethodDeclaration md : cd.getMethods()) {
                        JRubyMethod anno = md.getAnnotation(JRubyMethod.class);
                        if (anno == null) {
                            continue;
                        }
                        methodCount++;

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
                            methodDescs = new ArrayList<MethodDeclaration>();
                            methodsHash.put(name, methodDescs);
                        }

                        methodDescs.add(md);

                        if (anno.frame() || (anno.reads() != null && anno.reads().length >= 1) || (anno.writes() != null && anno.writes().length >= 1)) {
                            frameAwareMethods.add(name);
                        }
                    }

                    if (methodCount == 0) {
                        // no annotated methods found, skip
                        return;
                    }

                    classNames.add(getActualQualifiedName(cd));

                    processMethodDeclarations(staticAnnotatedMethods);

                    if (!staticAnnotatedMethods1_8.isEmpty()) {
                        out.println("        if (compatVersion == CompatVersion.RUBY1_8 || compatVersion == CompatVersion.BOTH) {");
                        processMethodDeclarations(staticAnnotatedMethods1_8);
                        out.println("        }");
                    }

                    if (!staticAnnotatedMethods1_9.isEmpty()) {
                        out.println("        if (compatVersion == CompatVersion.RUBY1_9 || compatVersion == CompatVersion.BOTH) {");
                        processMethodDeclarations(staticAnnotatedMethods1_9);
                        out.println("        }");
                    }

                    processMethodDeclarations(annotatedMethods);

                    if (!annotatedMethods1_8.isEmpty()) {
                        out.println("        if (compatVersion == CompatVersion.RUBY1_8 || compatVersion == CompatVersion.BOTH) {");
                        processMethodDeclarations(annotatedMethods1_8);
                        out.println("        }");
                    }

                    if (!annotatedMethods1_9.isEmpty()) {
                        out.println("        if (compatVersion == CompatVersion.RUBY1_9 || compatVersion == CompatVersion.BOTH) {");
                        processMethodDeclarations(annotatedMethods1_9);
                        out.println("        }");
                    }

                    for (String name : frameAwareMethods) {
                        out.println("        ASTInspector.FRAME_AWARE_METHODS.add(\"" + name + "\");");
                    }

                    out.println("    }");
                    out.println("}");
                    out.close();
                    out = null;

                    FileOutputStream fos = new FileOutputStream("src_gen/" + qualifiedName + "$Populator.java");
                    fos.write(bytes.toByteArray());
                    fos.close();
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
                    String qualifiedName = getActualQualifiedName(md.getDeclaringType());

                    boolean hasContext = false;
                    boolean hasBlock = false;

                    for (ParameterDeclaration pd : md.getParameters()) {
                        hasContext |= pd.getType().toString().equals("org.jruby.runtime.ThreadContext");
                        hasBlock |= pd.getType().toString().equals("org.jruby.runtime.Block");
                    }

                    int actualRequired = calculateActualRequired(md.getParameters().size(), anno.optional(), anno.rest(), isStatic, hasContext, hasBlock);

                    String annotatedBindingName = CodegenUtils.getAnnotatedBindingClassName(
                            md.getSimpleName(),
                            qualifiedName,
                            isStatic,
                            actualRequired,
                            anno.optional(),
                            false,
                            anno.frame());
                    String implClass = anno.meta() ? "metaClass" : "cls";

                    out.println("        javaMethod = new " + annotatedBindingName + "(" + implClass + ", Visibility." + anno.visibility() + ");");
                    out.println("        populateMethod(javaMethod, " +
                            getArityValue(anno, actualRequired) + ", \"" +
                            md.getSimpleName() + "\", " +
                            isStatic + ", " +
                            "CallConfiguration." + getCallConfigNameByAnno(anno) + ");");
                    generateMethodAddCalls(md, anno);
                }
            }

            public void processMethodDeclarationMulti(MethodDeclaration md) {
                JRubyMethod anno = md.getAnnotation(JRubyMethod.class);
                if (anno != null && out != null) {
                    boolean isStatic = md.getModifiers().contains(Modifier.STATIC);
                    String qualifiedName = getActualQualifiedName(md.getDeclaringType());

                    boolean hasContext = false;
                    boolean hasBlock = false;

                    for (ParameterDeclaration pd : md.getParameters()) {
                        hasContext |= pd.getType().toString().equals("org.jruby.runtime.ThreadContext");
                        hasBlock |= pd.getType().toString().equals("org.jruby.runtime.Block");
                    }

                    int actualRequired = calculateActualRequired(md.getParameters().size(), anno.optional(), anno.rest(), isStatic, hasContext, hasBlock);

                    String annotatedBindingName = CodegenUtils.getAnnotatedBindingClassName(
                            md.getSimpleName(),
                            qualifiedName,
                            isStatic,
                            actualRequired,
                            anno.optional(),
                            true,
                            anno.frame());
                    String implClass = anno.meta() ? "metaClass" : "cls";

                    out.println("        javaMethod = new " + annotatedBindingName + "(" + implClass + ", Visibility." + anno.visibility() + ");");
                    out.println("        populateMethod(javaMethod, " +
                            "-1, \"" +
                            md.getSimpleName() + "\", " +
                            isStatic + ", " +
                            "CallConfiguration." + getCallConfigNameByAnno(anno) + ");");
                    generateMethodAddCalls(md, anno);
                }
            }

            private String getActualQualifiedName(TypeDeclaration td) {
                // declared type returns the qualified name without $ for inner classes!!!
                String qualifiedName;
                if (td.getDeclaringType() != null) {
                    // inner class, use $ to delimit
                    if (td.getDeclaringType().getDeclaringType() != null) {
                        qualifiedName = td.getDeclaringType().getDeclaringType().getQualifiedName() + "$" + td.getDeclaringType().getSimpleName() + "$" + td.getSimpleName();
                    } else {
                        qualifiedName = td.getDeclaringType().getQualifiedName() + "$" + td.getSimpleName();
                    }
                } else {
                    qualifiedName = td.getQualifiedName();
                }

                return qualifiedName;
            }

            private boolean tryClass(String className) {
                Class tryClass = null;
                try {
                    tryClass = Class.forName(className);
                } catch (ClassNotFoundException cnfe) {
                }

                return tryClass != null;
            }

            private int calculateActualRequired(int paramsLength, int optional, boolean rest, boolean isStatic, boolean hasContext, boolean hasBlock) {
                int actualRequired;
                if (optional == 0 && !rest) {
                    int args = paramsLength;
                    if (args == 0) {
                        actualRequired = 0;
                    } else {
                        if (isStatic) {
                            args--;
                        }
                        if (hasContext) {
                            args--;
                        }
                        if (hasBlock) {
                            args--;                        // TODO: confirm expected args are IRubyObject (or similar)
                        }
                        actualRequired = args;
                    }
                } else {
                    // optional args, so we have IRubyObject[]
                    // TODO: confirm
                    int args = paramsLength;
                    if (args == 0) {
                        actualRequired = 0;
                    } else {
                        if (isStatic) {
                            args--;
                        }
                        if (hasContext) {
                            args--;
                        }
                        if (hasBlock) {
                            args--;                        // minus one more for IRubyObject[]
                        }
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
                        out.println("        ASTInspector.FRAME_AWARE_METHODS.add(\"" + name + "\");");
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
                        out.println("        metaClass.addMethod(\"" + baseName + "\", javaMethod);");
                    } else {
                        baseName = jrubyMethod.name()[0];
                        for (String name : jrubyMethod.name()) {
                            out.println("        metaClass.addMethod(\"" + name + "\", javaMethod);");
                        }
                    }

                    if (jrubyMethod.alias().length > 0) {
                        for (String alias : jrubyMethod.alias()) {
                            out.println("        metaClass.defineAlias(\"" + alias + "\", \"" + baseName + "\");");
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
                            out.println("        cls.defineAlias(\"" + alias + "\", \"" + baseName + "\");");
                        }
                    }

                    if (jrubyMethod.module()) {
                        out.println("        moduleMethod = populateModuleMethod(cls, javaMethod);");
                        out.println("        singletonClass = moduleMethod.getImplementationClass();");

                        //                        RubyModule singletonClass = module.getSingletonClass();

                        if (jrubyMethod.name().length == 0) {
                            baseName = md.getSimpleName();
                            out.println("        singletonClass.addMethod(\"" + baseName + "\", moduleMethod);");
                        } else {
                            baseName = jrubyMethod.name()[0];
                            for (String name : jrubyMethod.name()) {
                                out.println("        singletonClass.addMethod(\"" + name + "\", moduleMethod);");
                            }
                        }

                        if (jrubyMethod.alias().length > 0) {
                            for (String alias : jrubyMethod.alias()) {
                                out.println("        singletonClass.defineAlias(\"" + alias + "\", \"" + baseName + "\");");
                            }
                        }
                    }
                }
            //                }
            }
        }

        public static int getArityValue(JRubyMethod anno, int actualRequired) {
            if (anno.optional() > 0 || anno.rest()) {
                return -(actualRequired + 1);
            }
            return actualRequired;
        }
    
        public static String getCallConfigNameByAnno(JRubyMethod anno) {
            return getCallConfigName(anno.frame(), anno.scope(), anno.backtrace());
        }

        public static String getCallConfigName(boolean frame, boolean scope, boolean backtrace) {
            if (frame) {
                if (scope) {
                    return "FrameFullScopeFull";
                } else {
                    return "FrameFullScopeNone";
                }
            } else if (scope) {
                if (backtrace) {
                    return "FrameBacktraceScopeFull";
                } else {
                    return "FrameNoneScopeFull";
                }
            } else if (backtrace) {
                return "FrameBacktraceScopeNone";
            } else {
                return "FrameNoneScopeNone";
            }
        }
    }
}
