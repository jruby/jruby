/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;

import org.jruby.util.CodegenUtils;

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
    public static final String SRC_GEN_DIR = "target/generated-sources/org/jruby/gen/";
    private final List<CharSequence> classNames = new ArrayList<>();
    private PrintStream out;
    private Types types;
    private static final boolean DEBUG = false;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        types = processingEnv.getTypeUtils();
    }

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
        }
        catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        }

        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @SuppressWarnings("deprecation")
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
            out.println("");
            out.println("import org.jruby.Ruby;");
            out.println("import org.jruby.RubyModule;");
            out.println("import org.jruby.RubyClass;");
            out.println("import org.jruby.anno.TypePopulator;");
            out.println("import org.jruby.internal.runtime.methods.JavaMethod;");
            out.println("import org.jruby.internal.runtime.methods.DynamicMethod;");
            out.println("import org.jruby.runtime.Arity;");
            out.println("import org.jruby.runtime.Visibility;");
            out.println("import org.jruby.runtime.MethodIndex;");
            out.println("import java.util.Arrays;");
            out.println("import java.util.List;");
            out.println("import jakarta.annotation.Generated;");
            out.println("");
            out.println("@Generated(\"org.jruby.anno.AnnotationBinder\")");
            out.println("@SuppressWarnings(\"deprecation\")");
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
            out.println("        DynamicMethod moduleMethod, aliasedMethod;");
            if (hasMeta || hasModule) out.println("        RubyClass singletonClass = cls.getSingletonClass();");
            out.println("        Ruby runtime = cls.getRuntime();");

            Map<CharSequence, List<ExecutableElement>> annotatedMethods = new HashMap<>();
            Map<CharSequence, List<ExecutableElement>> staticAnnotatedMethods = new HashMap<>();

            Map<Set<FrameField>, List<String>> readGroups = new HashMap<>();
            Map<Set<FrameField>, List<String>> writeGroups = new HashMap<>();

            int methodCount = 0;
            for (ExecutableElement method : ElementFilter.methodsIn(cd.getEnclosedElements())) {
                JRubyMethod anno = method.getAnnotation(JRubyMethod.class);

                if (anno == null) continue;

                if (anno.compat() == org.jruby.CompatVersion.RUBY1_8) continue;

                methodCount++;

                checkForThrows(cd, method);

                CharSequence name = anno.name().length == 0 ? method.getSimpleName() : anno.name()[0];

                final Map<CharSequence, List<ExecutableElement>> methodsHash;
                if (method.getModifiers().contains(Modifier.STATIC)) {
                    methodsHash = staticAnnotatedMethods;
                } else {
                    methodsHash = annotatedMethods;
                }

                List<ExecutableElement> methodDescs = methodsHash.get(name);
                if (methodDescs == null) {
                    methodsHash.put(name, methodDescs = new ArrayList<>(4));
                }

                methodDescs.add(method);

                // check for frame field reads or writes
                AnnotationHelper.groupFrameFields(readGroups, writeGroups, anno, method.getSimpleName().toString());
            }

            if (methodCount == 0) return; // no annotated methods found, skip

            CharSequence primaryName = getActualQualifiedName(cd);
            classNames.add(primaryName);

            List<String> classAndSubs = new ArrayList<>();
            classAndSubs.add(cd.getQualifiedName().toString());

            JRubyClass classAnno = cd.getAnnotation(JRubyClass.class);
            if (classAnno != null) {
                addSubclassNames(classAndSubs, classAnno);
            }

            // set up a list of class names for backtrace generation
            Map<CharSequence, CharSequence> mappings = new HashMap<>();

            processMethodDeclarations(staticAnnotatedMethods);
            gatherMappings(staticAnnotatedMethods, mappings);

            processMethodDeclarations(annotatedMethods);
            gatherMappings(annotatedMethods, mappings);

            out.println("");

            // addBoundMethods(int tuplesIndex, String... classNamesAndTuples)
            out.print("        runtime.addBoundMethods(" + classAndSubs.size() + ", ");
            out.print(classAndSubs.stream().map(s -> quote(s)).collect(Collectors.joining(", ")));
            mappings.forEach((javaName, rubyName) -> out.print(", " + quote(javaName) + ", " + quote(rubyName) + ""));
            out.println(");");
            out.println("    }");

            // write out a static initializer for frame names, so it only fires once
            out.println("    static {");

            AnnotationHelper.populateMethodIndex(readGroups,
                    (bits, names) -> emitIndexCode(bits, names, "        MethodIndex.addMethodReadFieldsPacked(%d, \"%s\");"));
            AnnotationHelper.populateMethodIndex(writeGroups,
                    (bits, names) -> emitIndexCode(bits, names, "        MethodIndex.addMethodWriteFieldsPacked(%d, \"%s\");"));

            out.println("    }");

            out.println("}"); // class
            out.close();
            out = null;

            new File(SRC_GEN_DIR).mkdirs();
            FileOutputStream fos = new FileOutputStream(SRC_GEN_DIR + qualifiedName + POPULATOR_SUFFIX + ".java");
            fos.write(bytes.toByteArray());
            fos.close();
        }
        catch (IOException ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }

    public void addSubclassNames(List<String> classAndSubs, JRubyClass classAnno) {
        // all implementer classes specified in annotation
        try {
            AnnotationHelper.addSubclassNames(classAndSubs, classAnno);
        } catch (MirroredTypesException mte) {
            for (TypeMirror tm : mte.getTypeMirrors()) {
                classAndSubs.add(((TypeElement) types.asElement(tm)).getQualifiedName().toString());
            }
        }

    }

    protected void gatherMappings(Map<CharSequence, List<ExecutableElement>> methods, Map<CharSequence, CharSequence> mappings) {
        for (Map.Entry<CharSequence, List<ExecutableElement>> entry : methods.entrySet()) {
            ExecutableElement decl = entry.getValue().get(0);
            JRubyMethod anno = decl.getAnnotation(JRubyMethod.class);

            if (anno.omit()) continue;

            CharSequence javaName = decl.getSimpleName().toString();
            CharSequence rubyName = entry.getKey();

            mappings.putIfAbsent(javaName, rubyName);
        }
    }

    public void emitIndexCode(int bits, String names, String format) {
        out.println(String.format(format, bits, names));
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
            CharSequence qualifiedName = getActualQualifiedName((TypeElement) method.getEnclosingElement());

            ParametersInfo info = identifyParameters(method.getParameters());

            int actualRequired = calculateActualRequired(method, method.getParameters().size(), anno.optional(), anno.rest(), isStatic, info.hasContext, info.hasBlock);

            String annotatedBindingName = CodegenUtils.getAnnotatedBindingClassName(
                    method.getSimpleName(),
                    qualifiedName,
                    isStatic,
                    actualRequired,
                    anno.optional(),
                    false,
                    anno.frame());
            String implClass = anno.meta() ? "singletonClass" : "cls";

            String baseName = getBaseName(anno.name(), method);
            out.println("        javaMethod = new " + annotatedBindingName + "(" + implClass + ", Visibility." + anno.visibility() + ", \"" + baseName + "\");");
            out.println("        populateMethod(javaMethod, " +
                    join(AnnotationHelper.getArityValue(anno, actualRequired),
                            quote(method.getSimpleName()),
                            isStatic,
                            anno.notImplemented(),
                            ((TypeElement) method.getEnclosingElement()).getQualifiedName() + ".class",
                            quote(method.getSimpleName()),
                            method.getReturnType() + ".class",
                            info.typeDecl
                    ) + ");");
            generateMethodAddCalls(method, anno);
        }
    }

    public void processMethodDeclarationMulti(ExecutableElement method) {
        JRubyMethod anno = method.getAnnotation(JRubyMethod.class);
        if (anno != null && out != null) {
            boolean isStatic = method.getModifiers().contains(Modifier.STATIC);
            CharSequence qualifiedName = getActualQualifiedName((TypeElement)method.getEnclosingElement());

            ParametersInfo info = identifyParameters(method.getParameters());

            int actualRequired = calculateActualRequired(method, method.getParameters().size(), anno.optional(), anno.rest(), isStatic, info.hasContext, info.hasBlock);

            String annotatedBindingName = CodegenUtils.getAnnotatedBindingClassName(
                    method.getSimpleName(),
                    qualifiedName,
                    isStatic,
                    actualRequired,
                    anno.optional(),
                    true,
                    anno.frame());
            String implClass = anno.meta() ? "singletonClass" : "cls";

            String baseName = getBaseName(anno.name(), method);
            out.println("        javaMethod = new " + annotatedBindingName + "(" + implClass + ", Visibility." + anno.visibility() + ", \"" + baseName + "\");");
            out.println("        populateMethod(javaMethod, " +
                        join(-1,
                                quote(method.getSimpleName()),
                                isStatic,
                                anno.notImplemented(),
                                ((TypeElement) method.getEnclosingElement()).getQualifiedName() + ".class",
                                quote(method.getSimpleName()),
                                method.getReturnType() + ".class",
                                info.typeDecl
                        ) + ");");
            generateMethodAddCalls(method, anno);
        }
    }

    private static ParametersInfo identifyParameters(List<? extends VariableElement> parameters) {
        boolean hasContext = false; boolean hasBlock = false;

        int s = 0; int l = parameters.size();

        if (l > 0 && parameters.get(0).asType().toString().equals("org.jruby.runtime.ThreadContext")) {
            hasContext = true; s++;
        }
        if (l > 0 && parameters.get(l - 1).asType().toString().equals("org.jruby.runtime.Block")) {
            hasBlock = true; l--;
        }

        boolean allIRubyObject = true; boolean aryIRubyObject = false;
        final TypeMirror[] types = new TypeMirror[l - s];
        for (int i = 0; i < types.length; i++) {
            types[i] = parameters.get(s + i).asType();

            if (!types[i].toString().startsWith("org.jruby.runtime.builtin.IRubyObject")) {
                allIRubyObject = false;
                break;
            } else if (types[i].toString().endsWith("[]")) { // IRubyObject[]
                aryIRubyObject = true;
            }
        }

        if (allIRubyObject) {
            StringJoiner constant = new StringJoiner("_");
            // e.g. constant CONTEXT_ARG1_BLOCK (from super -> TypePopulator)
            if (hasContext) constant.add("CONTEXT");
            constant.add("ARG" + (aryIRubyObject ? types.length - 1 : types.length));
            if (aryIRubyObject) constant.add("ARY");
            if (hasBlock) constant.add("BLOCK");
            return new ParametersInfo(constant.toString(), hasContext, hasBlock);
        }

        // fallback to old behavior -> generating a new Class[] { ... }
        StringJoiner joiner = new StringJoiner(", ");
        for (VariableElement parameter : parameters) {
            joiner.add(parameter.asType() + ".class");
        }
        return new ParametersInfo("new Class[] { " + joiner + " }", hasContext, hasBlock);
    }

    private static class ParametersInfo {

        final String typeDecl;
        final boolean hasContext;
        final boolean hasBlock;

        ParametersInfo(String name, boolean hasContext, boolean hasBlock) {
            this.typeDecl = name;
            this.hasContext = hasContext;
            this.hasBlock = hasBlock;
        }

    }

    private static CharSequence getActualQualifiedName(TypeElement elem) {
        if (elem.getNestingKind() == NestingKind.MEMBER) {
            return getActualQualifiedName((TypeElement) elem.getEnclosingElement()) + "$" + elem.getSimpleName();
        }
        return elem.getQualifiedName().toString();
    }

    private static StringBuilder join(final Object... vals) {
        return join(", ", Arrays.asList(vals));
    }

    private static StringBuilder join(final String sep, final Iterable<?> names) {
        final StringBuilder str = new StringBuilder();
        for (Object name : names) {
            if (str.length() > 0) str.append(sep);
            str.append(name);
        }
        return str;
    }

    private static CharSequence quote(final Object name) {
        return new StringBuilder().append('"').append(name).append('"');
    }

    private static int calculateActualRequired(ExecutableElement method, int paramsLength, int optional,
                                               boolean rest, boolean isStatic, boolean hasContext, boolean hasBlock) {
        int args = paramsLength;
        if (args == 0) return 0;

        if (isStatic) args--;
        if (hasContext) args--;
        if (hasBlock) args--;

        if (optional == 0 && !rest) {
            // TODO: confirm expected args are IRubyObject (or similar)
            return args;
        } else {
            // optional args, so we have IRubyObject[]

            args--; // minus one more for IRubyObject[]

            // TODO: confirm expected args are IRubyObject (or similar)

            if (args != 0) {
                throw new RuntimeException("Combining specific args with IRubyObject[] is not yet supported: "
                        + ((TypeElement) method.getEnclosingElement()).getQualifiedName() + "." + method);
            }
            return args;
        }
    }

    // @Deprecated // internal API
    public void generateMethodAddCalls(ExecutableElement md, JRubyMethod anno) {
        generateMethodAddCalls(md, anno.meta(), anno.module(), anno.name(), anno.alias());
    }

    private void generateMethodAddCalls(ExecutableElement md, final boolean meta, final boolean module,
        String[] names, String[] aliases) {
        if (meta) {
            defineMethodOnClass("javaMethod", "singletonClass", names, aliases, md);
        } else {
            defineMethodOnClass("javaMethod", "cls", names, aliases, md);
            if (module) {
                out.println("        moduleMethod = populateModuleMethod(cls, javaMethod);");
                defineMethodOnClass("moduleMethod", "singletonClass", names, aliases, md);
            }
        }
    }

    private void defineMethodOnClass(String methodVar, String classVar, final String[] names, final String[] aliases,
        ExecutableElement md) {
        CharSequence baseName = getBaseName(names, md);
        // aliasedMethod = type.putMethod(runtime, baseName, method);
        out.println("        aliasedMethod = " + classVar + ".putMethod(runtime, \"" + baseName + "\", " + methodVar + ");");
        if (names.length > 0) {
            for (String name : names) {
                if (!name.contentEquals(baseName)) {
                    out.println("        " + classVar + ".putMethod(runtime, \"" + name + "\", " + methodVar + ");");
                }
            }
        }

        if (aliases.length > 0) {
            for (String alias : aliases) {
                // type.putAlias(alias, aliasedMethod, baseName); /* baseName == method.getId() */
                out.println("        " + classVar + ".putAlias(\"" + alias + "\", aliasedMethod, \"" + baseName + "\");");
            }
        }
    }

    public static void checkForThrows(TypeElement type, ExecutableElement method) {
        // warn if the method raises any exceptions (except for RaiseException)
        List<String> exNames = new ArrayList<>();
        for (TypeMirror ex : method.getThrownTypes()) {
            final String name = ex.toString();
            if (!name.equals("org.jruby.exceptions.RaiseException")) {
                exNames.add(name);
            }
        }
        if (exNames.size() > 0) {
            warn("method " + type + "." + method + " should not throw exceptions: " + join(", ", exNames));
        }
    }

    public static String getBaseName(String[] names, ExecutableElement md) {
        if (names.length == 0) {
            return md.getSimpleName().toString();
        }
        return names[0];
    }

    private static void warn(CharSequence msg) {
        System.err.println(msg);
    }

}
