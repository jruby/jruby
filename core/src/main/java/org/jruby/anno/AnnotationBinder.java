/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2008-2013 Charles Oliver Nutter <headius@headius.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.anno;

import org.jruby.util.CodegenUtils;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Annotation processor for generating "populators" to bind native Java methods as Ruby methods, and
 * to gather a list of classes seen during compilation that should have their invokers regenerated.
 *
 * NOTE: This class must ONLY reference classes in the org.jruby.anno package, to avoid forcing
 * a transitive dependency on any runtime JRuby classes.
 */
@SupportedAnnotationTypes({"org.jruby.anno.JRubyMethod"})
public class AnnotationBinder extends AbstractProcessor {

    public static final String POPULATOR_SUFFIX = "$POPULATOR";
    private static final Logger LOG = Logger.getLogger("AnnotationBinder");
    public static final String SRC_GEN_DIR = "target/generated-sources/org/jruby/gen/";
    private final List<CharSequence> classNames = new ArrayList<CharSequence>();
    private PrintStream out;
    private static final boolean DEBUG = false;

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {
        for (TypeElement element : ElementFilter.typesIn(roundEnvironment.getRootElements())) {
            processType(element);
        }

        try {
            FileWriter fw = new FileWriter("target/generated-sources/annotated_classes.txt");
            for (CharSequence name : classNames) {
                fw.write(name.toString());
                fw.write('\n');
            }
            fw.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    public void processType(TypeElement cd) {
        // process inner classes
        for (TypeElement innerType : ElementFilter.typesIn(cd.getEnclosedElements())) {
            processType(innerType);
        }

        try {
            String qualifiedName = cd.getQualifiedName().toString().replace('.', '$');

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
            out.println("import org.jruby.anno.TypePopulator;");
            out.println("import org.jruby.internal.runtime.methods.CallConfiguration;");
            out.println("import org.jruby.internal.runtime.methods.JavaMethod;");
            out.println("import org.jruby.internal.runtime.methods.DynamicMethod;");
            out.println("import org.jruby.runtime.Arity;");
            out.println("import org.jruby.runtime.Visibility;");
            out.println("import org.jruby.runtime.MethodIndex;");
            out.println("import java.util.Arrays;");
            out.println("import java.util.List;");
            out.println("import javax.annotation.Generated;");

            out.println("@Generated(\"org.jruby.anno.AnnotationBinder\")");
            out.println("public class " + qualifiedName + POPULATOR_SUFFIX + " extends TypePopulator {");
            out.println("    public void populate(RubyModule cls, Class clazz) {");
            if (DEBUG) {
                out.println("        System.out.println(\"Using pregenerated populator: \" + \"" + qualifiedName + POPULATOR_SUFFIX + "\");");
            }

            // scan for meta, compat, etc to reduce findbugs complaints about "dead assignments"
            boolean hasAnno = false;
            boolean hasMeta = false;
            boolean hasModule = false;
            for (ExecutableElement method : ElementFilter.methodsIn(cd.getEnclosedElements())) {
                JRubyMethod anno = method.getAnnotation(JRubyMethod.class);
                if (anno == null) {
                    continue;
                }
                hasAnno = true;
                hasMeta |= anno.meta();
                hasModule |= anno.module();
            }

            if (!hasAnno) return;

            out.println("        JavaMethod javaMethod;");
            out.println("        DynamicMethod moduleMethod;");
            if (hasMeta || hasModule) out.println("        RubyClass singletonClass = cls.getSingletonClass();");
            out.println("        Ruby runtime = cls.getRuntime();");

            Map<CharSequence, List<ExecutableElement>> annotatedMethods = new HashMap<CharSequence, List<ExecutableElement>>();
            Map<CharSequence, List<ExecutableElement>> staticAnnotatedMethods = new HashMap<CharSequence, List<ExecutableElement>>();

            Set<String> frameAwareMethods = new HashSet<String>();
            Set<String> scopeAwareMethods = new HashSet<String>();

            int methodCount = 0;
            for (ExecutableElement method : ElementFilter.methodsIn(cd.getEnclosedElements())) {
                JRubyMethod anno = method.getAnnotation(JRubyMethod.class);
                if (anno == null) {
                    continue;
                }
                methodCount++;

                // warn if the method raises any exceptions (JRUBY-4494)
                if (method.getThrownTypes().size() != 0) {
                    System.err.print("Method " + cd.toString() + "." + method.toString() + " should not throw exceptions: ");
                    boolean comma = false;
                    for (TypeMirror thrownType : method.getThrownTypes()) {
                        if (comma) System.err.print(", ");
                        System.err.print(thrownType);
                        comma = true;
                    }
                    System.err.print("\n");
                }

                CharSequence name = anno.name().length == 0 ? method.getSimpleName() : anno.name()[0];

                List<ExecutableElement> methodDescs;
                Map<CharSequence, List<ExecutableElement>> methodsHash = null;
                if (method.getModifiers().contains(Modifier.STATIC)) {
                    methodsHash = staticAnnotatedMethods;
                } else {
                    methodsHash = annotatedMethods;
                }

                methodDescs = methodsHash.get(name);
                if (methodDescs == null) {
                    methodDescs = new ArrayList<ExecutableElement>();
                    methodsHash.put(name, methodDescs);
                }

                methodDescs.add(method);

                // check for frame field reads or writes
                boolean frame = false;
                boolean scope = false;
                if (anno.frame()) {
                    if (DEBUG)
                        System.out.println("Method has frame = true: " + methodDescs.get(0).getEnclosingElement() + ":" + methodDescs);
                    frame = true;
                }
                if (anno.scope()) {
                    if (DEBUG)
                        System.out.println("Method has frame = true: " + methodDescs.get(0).getEnclosingElement() + ":" + methodDescs);
                    scope = true;
                }
                for (FrameField field : anno.reads()) {
                    frame |= field.needsFrame();
                    scope |= field.needsScope();
                }
                for (FrameField field : anno.writes()) {
                    frame |= field.needsFrame();
                    scope |= field.needsScope();
                }
                
                if (frame) AnnotationHelper.addMethodNamesToSet(frameAwareMethods, anno, method.getSimpleName().toString());
                if (scope) AnnotationHelper.addMethodNamesToSet(scopeAwareMethods, anno, method.getSimpleName().toString());
            }

            if (methodCount == 0) {
                // no annotated methods found, skip
                return;
            }

            classNames.add(getActualQualifiedName(cd));

            processMethodDeclarations(staticAnnotatedMethods);
            for (Map.Entry<CharSequence, List<ExecutableElement>> entry : staticAnnotatedMethods.entrySet()) {
                ExecutableElement decl = entry.getValue().get(0);
                if (!decl.getAnnotation(JRubyMethod.class).omit()) addCoreMethodMapping(entry.getKey(), decl, out);
            }

            processMethodDeclarations(annotatedMethods);
            for (Map.Entry<CharSequence, List<ExecutableElement>> entry : annotatedMethods.entrySet()) {
                ExecutableElement decl = entry.getValue().get(0);
                if (!decl.getAnnotation(JRubyMethod.class).omit()) addCoreMethodMapping(entry.getKey(), decl, out);
            }

            out.println("    }");

            // write out a static initializer for frame names, so it only fires once
            out.println("    static {");
            if (!frameAwareMethods.isEmpty()) {
                StringBuffer frameMethodsString = new StringBuffer();
                boolean first = true;
                for (CharSequence name : frameAwareMethods) {
                    if (!first) frameMethodsString.append(',');
                    first = false;
                    frameMethodsString.append('"').append(name).append('"');
                }
                out.println("        MethodIndex.addFrameAwareMethods(" + frameMethodsString + ");");
            }
            if (!scopeAwareMethods.isEmpty()) {
                StringBuffer scopeMethodsString = new StringBuffer();
                boolean first = true;
                for (CharSequence name : scopeAwareMethods) {
                    if (!first) scopeMethodsString.append(',');
                    first = false;
                    scopeMethodsString.append('"').append(name).append('"');
                }
                out.println("        MethodIndex.addScopeAwareMethods(" + scopeMethodsString + ");");
            }
            out.println("    }");

            out.println("}");
            out.close();
            out = null;

            new File(SRC_GEN_DIR).mkdirs();
            FileOutputStream fos = new FileOutputStream(SRC_GEN_DIR + qualifiedName + POPULATOR_SUFFIX + ".java");
            fos.write(bytes.toByteArray());
            fos.close();
        } catch (IOException ioe) {
            LOG.severe("FAILED TO GENERATE: " + ioe);
            System.exit(1);
        }
    }

    public void processMethodDeclarations(Map<CharSequence, List<ExecutableElement>> declarations) {
        for (Map.Entry<CharSequence, List<ExecutableElement>> entry : declarations.entrySet()) {
            List<ExecutableElement> list = entry.getValue();

            if (list.size() == 1) {
                // single method, use normal logic
                processMethodDeclaration(list.get(0));
            } else {
                // multimethod, new logic
                processMethodDeclarationMulti(list.get(0));
            }
        }
    }

    public void processMethodDeclaration(ExecutableElement method) {
        JRubyMethod anno = method.getAnnotation(JRubyMethod.class);
        if (anno != null && out != null) {
            boolean isStatic = method.getModifiers().contains(Modifier.STATIC);
            CharSequence qualifiedName = getActualQualifiedName((TypeElement)method.getEnclosingElement());

            boolean hasContext = false;
            boolean hasBlock = false;

            StringBuffer buffer = new StringBuffer();
            boolean first = true;
            for (VariableElement parameter : method.getParameters()) {
                if (!first) buffer.append(", ");
                first = false;
                buffer.append(parameter.asType().toString());
                buffer.append(".class");
                hasContext |= parameter.asType().toString().equals("org.jruby.runtime.ThreadContext");
                hasBlock |= parameter.asType().toString().equals("org.jruby.runtime.Block");
            }

            int actualRequired = calculateActualRequired(method, method.getParameters().size(), anno.optional(), anno.rest(), isStatic, hasContext, hasBlock);

            String annotatedBindingName = CodegenUtils.getAnnotatedBindingClassName(
                    method.getSimpleName(),
                    qualifiedName,
                    isStatic,
                    actualRequired,
                    anno.optional(),
                    false,
                    anno.frame());
            String implClass = anno.meta() ? "singletonClass" : "cls";

            String callRequirement = AnnotationHelper.getCallConfigNameByAnno(anno);
            String callerRequirement = AnnotationHelper.getCallerCallConfigNameByAnno(anno);

            out.println("        javaMethod = new " + annotatedBindingName + "(" + implClass + ", Visibility." + anno.visibility() + ");");
            out.println("        populateMethod(javaMethod, " +
                    +AnnotationHelper.getArityValue(anno, actualRequired) + ", \""
                    + method.getSimpleName() + "\", "
                    + isStatic + ", "
                    + "CallConfiguration." + callRequirement + ","
                    + anno.notImplemented() + ", "
                    + ((TypeElement)method.getEnclosingElement()).getQualifiedName() + ".class, "
                    + "\"" + method.getSimpleName() + "\", "
                    + method.getReturnType().toString() + ".class, "
                    + "new Class[] {" + buffer.toString() + "},"
                    + "CallConfiguration." + callerRequirement + ");");
            generateMethodAddCalls(method, anno);
        }
    }

    public void processMethodDeclarationMulti(ExecutableElement method) {
        JRubyMethod anno = method.getAnnotation(JRubyMethod.class);
        if (anno != null && out != null) {
            boolean isStatic = method.getModifiers().contains(Modifier.STATIC);
            CharSequence qualifiedName = getActualQualifiedName((TypeElement)method.getEnclosingElement());

            boolean hasContext = false;
            boolean hasBlock = false;

            StringBuffer buffer = new StringBuffer();
            boolean first = true;
            for (VariableElement parameter : method.getParameters()) {
                if (!first) buffer.append(", ");
                first = false;
                buffer.append(parameter.asType().toString());
                buffer.append(".class");
                hasContext |= parameter.asType().toString().equals("org.jruby.runtime.ThreadContext");
                hasBlock |= parameter.asType().toString().equals("org.jruby.runtime.Block");
            }

            int actualRequired = calculateActualRequired(method, method.getParameters().size(), anno.optional(), anno.rest(), isStatic, hasContext, hasBlock);

            String annotatedBindingName = CodegenUtils.getAnnotatedBindingClassName(
                    method.getSimpleName(),
                    qualifiedName,
                    isStatic,
                    actualRequired,
                    anno.optional(),
                    true,
                    anno.frame());
            String implClass = anno.meta() ? "singletonClass" : "cls";

            String callRequirement = AnnotationHelper.getCallConfigNameByAnno(anno);
            String callerRequirement = AnnotationHelper.getCallerCallConfigNameByAnno(anno);

            out.println("        javaMethod = new " + annotatedBindingName + "(" + implClass + ", Visibility." + anno.visibility() + ");");
            out.println("        populateMethod(javaMethod, " +
                    "-1, \"" +
                    method.getSimpleName() + "\", " +
                    isStatic + ", " +
                    "CallConfiguration." + callRequirement + ", " +
                    anno.notImplemented() + ", "
                    + ((TypeElement)method.getEnclosingElement()).getQualifiedName() + ".class, "
                    + "\"" + method.getSimpleName() + "\", "
                    + method.getReturnType().toString() + ".class, "
                    + "new Class[] {" + buffer.toString() + "},"
                    + "CallConfiguration." + callerRequirement + ");");
            generateMethodAddCalls(method, anno);
        }
    }

    private void addCoreMethodMapping(CharSequence rubyName, ExecutableElement decl, PrintStream out) {
        out.println(new StringBuilder(50)
                .append("        runtime.addBoundMethod(")
                .append('"').append(((TypeElement)decl.getEnclosingElement()).getQualifiedName()).append('"')
                .append(',')
                .append('"').append(decl.getSimpleName()).append('"')
                .append(',')
                .append('"').append(rubyName).append('"')
                .append(");").toString());
    }

    private CharSequence getActualQualifiedName(TypeElement td) {
        if (td.getNestingKind() == NestingKind.MEMBER) {
            return getActualQualifiedName((TypeElement)td.getEnclosingElement()) + "$" + td.getSimpleName();
        }
        return td.getQualifiedName().toString();
    }

    private int calculateActualRequired(ExecutableElement md, int paramsLength, int optional, boolean rest, boolean isStatic, boolean hasContext, boolean hasBlock) {
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
                        + ((TypeElement)md.getEnclosingElement()).getQualifiedName() + "." + md.toString());
            }
        }

        return actualRequired;
    }

    public void generateMethodAddCalls(ExecutableElement md, JRubyMethod jrubyMethod) {
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

    private void defineMethodOnClass(String methodVar, String classVar, JRubyMethod jrubyMethod, ExecutableElement md) {
        CharSequence baseName;
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
