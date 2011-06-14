package org.jruby.anno;

import com.sun.mirror.apt.*;
import com.sun.mirror.declaration.*;
import com.sun.mirror.type.ReferenceType;
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
import java.util.HashSet;
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

                    out.println("import org.jruby.Ruby;");
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
                    out.println("import java.util.Arrays;");
                    out.println("import java.util.List;");

                    out.println("public class " + qualifiedName + "$Populator extends TypePopulator {");
                    out.println("    public void populate(RubyModule cls, Class clazz) {");
                    if (DEBUG) {
                        out.println("        System.out.println(\"Using pregenerated populator: \" + \"" + cd.getSimpleName() + "Populator\");");
                    }

                    // scan for meta, compat, etc to reduce findbugs complaints about "dead assignments"
                    boolean hasMeta = false;
                    boolean hasModule = false;
                    boolean hasCompat = false;
                    for (MethodDeclaration md : cd.getMethods()) {
                        JRubyMethod anno = md.getAnnotation(JRubyMethod.class);
                        if (anno == null) {
                            continue;
                        }
                        hasMeta |= anno.meta();
                        hasModule |= anno.module();
                        hasCompat |= anno.compat() != CompatVersion.BOTH;
                    }

                    out.println("        JavaMethod javaMethod;");
                    out.println("        DynamicMethod moduleMethod;");
                    if (hasMeta || hasModule) out.println("        RubyClass singletonClass = cls.getSingletonClass();");
                    if (hasCompat) out.println("        CompatVersion compatVersion = cls.getRuntime().getInstanceConfig().getCompatVersion();");
                    out.println("        Ruby runtime = cls.getRuntime();");

                    Map<String, List<MethodDeclaration>> annotatedMethods = new HashMap<String, List<MethodDeclaration>>();
                    Map<String, List<MethodDeclaration>> staticAnnotatedMethods = new HashMap<String, List<MethodDeclaration>>();
                    Map<String, List<MethodDeclaration>> annotatedMethods1_8 = new HashMap<String, List<MethodDeclaration>>();
                    Map<String, List<MethodDeclaration>> staticAnnotatedMethods1_8 = new HashMap<String, List<MethodDeclaration>>();
                    Map<String, List<MethodDeclaration>> annotatedMethods1_9 = new HashMap<String, List<MethodDeclaration>>();
                    Map<String, List<MethodDeclaration>> staticAnnotatedMethods1_9 = new HashMap<String, List<MethodDeclaration>>();

                    Set<String> frameAwareMethods = new HashSet<String>();
                    Set<String> scopeAwareMethods = new HashSet<String>();

                    int methodCount = 0;
                    for (MethodDeclaration md : cd.getMethods()) {
                        JRubyMethod anno = md.getAnnotation(JRubyMethod.class);
                        if (anno == null) {
                            continue;
                        }
                        methodCount++;

                        // warn if the method raises any exceptions (JRUBY-4494)
                        if (md.getThrownTypes().size() != 0) {
                            System.err.print("Method " + cd.toString() + "." + md.toString() + " should not throw exceptions: ");
                            boolean comma = false;
                            for (ReferenceType thrownType : md.getThrownTypes()) {
                                if (comma) System.err.print(", ");
                                System.err.print(thrownType);
                                comma = true;
                            }
                            System.err.print("\n");
                        }

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

                        // check for frame field reads or writes
                        boolean frame = false;
                        boolean scope = false;
                        if (anno.frame()) {
                            if (DEBUG) System.out.println("Method has frame = true: " + methodDescs.get(0).getDeclaringType() + ": " + methodDescs);
                            frame = true;
                        }
                        if (anno.reads() != null) for (FrameField read : anno.reads()) {
                            switch (read) {
                            case BACKREF:
                            case LASTLINE:
                                if (DEBUG) System.out.println("Method reads scope field " + read + ": " + methodDescs.get(0).getDeclaringType() + ": " + methodDescs);
                                scope = true;
                                break;
                            default:
                                if (DEBUG) System.out.println("Method reads frame field " + read + ": " + methodDescs.get(0).getDeclaringType() + ": " + methodDescs);
                                frame = true;
                            }
                        }
                        if (anno.writes() != null) for (FrameField write : anno.writes()) {
                            switch (write) {
                            case BACKREF:
                            case LASTLINE:
                                if (DEBUG) System.out.println("Method writes scope field " + write + ": " + methodDescs.get(0).getDeclaringType() + ": " + methodDescs);
                                scope = true;
                                break;
                            default:
                                if (DEBUG) System.out.println("Method writes frame field " + write + ": " + methodDescs.get(0).getDeclaringType() + ": " + methodDescs);
                                frame = true;
                            }
                        }
                        if (frame) frameAwareMethods.addAll(Arrays.asList(anno.name()));
                        if (scope) scopeAwareMethods.addAll(Arrays.asList(anno.name()));
                    }

                    if (methodCount == 0) {
                        // no annotated methods found, skip
                        return;
                    }

                    classNames.add(getActualQualifiedName(cd));

                    processMethodDeclarations(staticAnnotatedMethods);
                    for (Map.Entry<String, List<MethodDeclaration>> entry : staticAnnotatedMethods.entrySet()) {
                        MethodDeclaration decl = entry.getValue().get(0);
                        if (!decl.getAnnotation(JRubyMethod.class).omit()) addCoreMethodMapping(entry.getKey(), decl, out);
                    }

                    if (!staticAnnotatedMethods1_8.isEmpty()) {
                        out.println("        if (compatVersion == CompatVersion.RUBY1_8 || compatVersion == CompatVersion.BOTH) {");
                        processMethodDeclarations(staticAnnotatedMethods1_8);
                        for (Map.Entry<String, List<MethodDeclaration>> entry : staticAnnotatedMethods1_8.entrySet()) {
                            MethodDeclaration decl = entry.getValue().get(0);
                            if (!decl.getAnnotation(JRubyMethod.class).omit()) addCoreMethodMapping(entry.getKey(), decl, out);
                        }
                        out.println("        }");
                    }

                    if (!staticAnnotatedMethods1_9.isEmpty()) {
                        out.println("        if (compatVersion == CompatVersion.RUBY1_9 || compatVersion == CompatVersion.BOTH) {");
                        processMethodDeclarations(staticAnnotatedMethods1_9);
                        for (Map.Entry<String, List<MethodDeclaration>> entry : staticAnnotatedMethods1_9.entrySet()) {
                            MethodDeclaration decl = entry.getValue().get(0);
                            if (!decl.getAnnotation(JRubyMethod.class).omit()) addCoreMethodMapping(entry.getKey(), decl, out);
                        }
                        out.println("        }");
                    }

                    processMethodDeclarations(annotatedMethods);
                    for (Map.Entry<String, List<MethodDeclaration>> entry : annotatedMethods.entrySet()) {
                        MethodDeclaration decl = entry.getValue().get(0);
                        if (!decl.getAnnotation(JRubyMethod.class).omit()) addCoreMethodMapping(entry.getKey(), decl, out);
                    }

                    if (!annotatedMethods1_8.isEmpty()) {
                        out.println("        if (compatVersion == CompatVersion.RUBY1_8 || compatVersion == CompatVersion.BOTH) {");
                        processMethodDeclarations(annotatedMethods1_8);
                        for (Map.Entry<String, List<MethodDeclaration>> entry : annotatedMethods1_8.entrySet()) {
                            MethodDeclaration decl = entry.getValue().get(0);
                            if (!decl.getAnnotation(JRubyMethod.class).omit()) addCoreMethodMapping(entry.getKey(), decl, out);
                        }
                        out.println("        }");
                    }

                    if (!annotatedMethods1_9.isEmpty()) {
                        out.println("        if (compatVersion == CompatVersion.RUBY1_9 || compatVersion == CompatVersion.BOTH) {");
                        processMethodDeclarations(annotatedMethods1_9);
                        for (Map.Entry<String, List<MethodDeclaration>> entry : annotatedMethods1_9.entrySet()) {
                            MethodDeclaration decl = entry.getValue().get(0);
                            if (!decl.getAnnotation(JRubyMethod.class).omit()) addCoreMethodMapping(entry.getKey(), decl, out);
                        }
                        out.println("        }");
                    }

                    out.println("    }");

                    // write out a static initializer for frame names, so it only fires once
                    out.println("    static {");
                    if (!frameAwareMethods.isEmpty()) {
                        StringBuffer frameMethodsString = new StringBuffer();
                        boolean first = true;
                        for (String name : frameAwareMethods) {
                            if (!first) frameMethodsString.append(',');
                            first = false;
                            frameMethodsString.append('"').append(name).append('"');
                        }
                        out.println("        ASTInspector.addFrameAwareMethods(" + frameMethodsString + ");");
                    }
                    if (!scopeAwareMethods.isEmpty()) {
                        StringBuffer scopeMethodsString = new StringBuffer();
                        boolean first = true;
                        for (String name : scopeAwareMethods) {
                            if (!first) scopeMethodsString.append(',');
                            first = false;
                            scopeMethodsString.append('"').append(name).append('"');
                        }
                        out.println("        ASTInspector.addScopeAwareMethods(" + scopeMethodsString + ");");
                    }
                    out.println("     }");

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

                    StringBuffer buffer = new StringBuffer();
                    boolean first = true;
                    for (ParameterDeclaration pd : md.getParameters()) {
                        if (!first) buffer.append(", ");
                        first = false;
                        buffer.append(pd.getType().toString());
                        buffer.append(".class");
                        hasContext |= pd.getType().toString().equals("org.jruby.runtime.ThreadContext");
                        hasBlock |= pd.getType().toString().equals("org.jruby.runtime.Block");
                    }

                    int actualRequired = calculateActualRequired(md, md.getParameters().size(), anno.optional(), anno.rest(), isStatic, hasContext, hasBlock);

                    String annotatedBindingName = CodegenUtils.getAnnotatedBindingClassName(
                            md.getSimpleName(),
                            qualifiedName,
                            isStatic,
                            actualRequired,
                            anno.optional(),
                            false,
                            anno.frame());
                    String implClass = anno.meta() ? "singletonClass" : "cls";

                    out.println("        javaMethod = new " + annotatedBindingName + "(" + implClass + ", Visibility." + anno.visibility() + ");");
                    out.println("        populateMethod(javaMethod, " +
                            + getArityValue(anno, actualRequired) + ", \""
                            + md.getSimpleName() + "\", "
                            + isStatic + ", "
                            + "CallConfiguration." + getCallConfigNameByAnno(anno) + ", "
                            + anno.notImplemented() + ", "
                            + md.getDeclaringType().getQualifiedName() + ".class, "
                            + "\"" + md.getSimpleName() + "\", "
                            + md.getReturnType().toString() + ".class, "
                            + "new Class[] {" + buffer.toString() + "});");
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

                    int actualRequired = calculateActualRequired(md, md.getParameters().size(), anno.optional(), anno.rest(), isStatic, hasContext, hasBlock);

                    String annotatedBindingName = CodegenUtils.getAnnotatedBindingClassName(
                            md.getSimpleName(),
                            qualifiedName,
                            isStatic,
                            actualRequired,
                            anno.optional(),
                            true,
                            anno.frame());
                    String implClass = anno.meta() ? "singletonClass" : "cls";

                    out.println("        javaMethod = new " + annotatedBindingName + "(" + implClass + ", Visibility." + anno.visibility() + ");");
                    out.println("        populateMethod(javaMethod, " +
                            "-1, \"" +
                            md.getSimpleName() + "\", " +
                            isStatic + ", " +
                            "CallConfiguration." + getCallConfigNameByAnno(anno) + ", " +
                            anno.notImplemented() + ");");
                    generateMethodAddCalls(md, anno);
                }
            }

            private void addCoreMethodMapping(String rubyName, MethodDeclaration decl, PrintStream out) {
                out.println(
                        "        runtime.addBoundMethod(\""
                        + decl.getDeclaringType().getQualifiedName()
                        + "."
                        + decl.getSimpleName()
                        + "\", \""
                        + rubyName
                        + "\");");
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

            private int calculateActualRequired(MethodDeclaration md, int paramsLength, int optional, boolean rest, boolean isStatic, boolean hasContext, boolean hasBlock) {
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
                        throw new RuntimeException("Combining specific args with IRubyObject[] is not yet supported: "
                                + md.getDeclaringType().getQualifiedName() + "." + md.toString());
                    }
                }

                return actualRequired;
            }

            public void generateMethodAddCalls(MethodDeclaration md, JRubyMethod jrubyMethod) {
                if (jrubyMethod.meta()) {
                    defineMethodOnClass("javaMethod", "singletonClass", jrubyMethod, md);
                } else {
                    defineMethodOnClass("javaMethod", "cls", jrubyMethod, md);
                    if (jrubyMethod.module()) {
                        out.println("        moduleMethod = populateModuleMethod(cls, javaMethod);");
                        defineMethodOnClass("moduleMethod", "singletonClass", jrubyMethod, md);
                    }
                }
            //                }
            }

            private void defineMethodOnClass(String methodVar, String classVar, JRubyMethod jrubyMethod, MethodDeclaration md) {
                String baseName;
                if (jrubyMethod.name().length == 0) {
                    baseName = md.getSimpleName();
                    out.println("        " + classVar + ".addMethodAtBootTimeOnly(\"" + baseName + "\", " + methodVar + ");");
                } else {
                    baseName = jrubyMethod.name()[0];
                    for (String name : jrubyMethod.name()) {
                        out.println("        " + classVar + ".addMethodAtBootTimeOnly(\"" + name + "\", " + methodVar + ");");
                    }
                }

                if (jrubyMethod.alias().length > 0) {
                    for (String alias : jrubyMethod.alias()) {
                        out.println("        " + classVar + ".defineAlias(\"" + alias + "\", \"" + baseName + "\");");
                    }
                }
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
