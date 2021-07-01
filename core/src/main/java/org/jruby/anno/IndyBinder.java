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

import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.internal.runtime.methods.DescriptorInfo;
import org.jruby.runtime.Visibility;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import jakarta.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jruby.util.CodegenUtils.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Annotation processor for generating "populators" to bind native Java methods as Ruby methods, and
 * to gather a list of classes seen during compilation that should have their invokers regenerated.
 *
 * NOTE: This class must ONLY reference classes in the org.jruby.anno package, to avoid forcing
 * a transitive dependency on any runtime JRuby classes.
 */
@SupportedAnnotationTypes({"org.jruby.anno.JRubyMethod"})
public class IndyBinder extends AbstractProcessor {

    public static final String POPULATOR_SUFFIX = "$POPULATOR";
    public static final String SRC_GEN_DIR = "target/classes/org/jruby/gen/";
    public static final int CLASS = 1;
    public static final int BASEMETHOD = 3;
    public static final int MODULEMETHOD = 4;
    public static final int RUNTIME = 5;
    public static final int SINGLETONCLASS = 6;
    public static final int RUBYMODULE = 1;
    private final List<CharSequence> classNames = new ArrayList<CharSequence>();
    private SkinnyMethodAdapter mv;
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

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

            cw.visitAnnotation(p(Generated.class), true);

            cw.visit(Opcodes.V1_8, ACC_PUBLIC, ("org.jruby.gen." + qualifiedName + POPULATOR_SUFFIX).replace('.', '/'), null, "org/jruby/anno/TypePopulator", null);

            mv = new SkinnyMethodAdapter(cw, ACC_PUBLIC, "<init>", "()V", null, null);
            mv.start();
            mv.aload(0);
            mv.invokespecial("org/jruby/anno/TypePopulator", "<init>", "()V");
            mv.voidreturn();
            mv.end();

            mv = new SkinnyMethodAdapter(cw, ACC_PUBLIC, "populate", "(Lorg/jruby/RubyModule;Ljava/lang/Class;)V", null, null);

            mv.start();

            if (DEBUG) {
                mv.ldc("Using pregenerated populator: " + qualifiedName + POPULATOR_SUFFIX);
                mv.aprintln();
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

//            mv.local(BASEMETHOD, "javaMethod", "Lorg/jruby/internal/runtime/methods/HandleMethod;");
//            mv.local(MODULEMETHOD, "moduleMethod", "Lorg/jruby/internal/runtime/methods/HandleMethod;");
//
//            mv.local(RUNTIME, "runtime", RUBY_TYPE);
            mv.aload(RUBYMODULE);
            mv.invokevirtual("org/jruby/RubyModule", "getRuntime", "()Lorg/jruby/Ruby;");
            mv.astore(RUNTIME);

            if (hasMeta || hasModule) {
//                mv.local(SINGLETONCLASS, "singletonClass", RUBYMODULE_TYPE);
                mv.aload(1);
                mv.invokevirtual("org/jruby/RubyModule", "getSingletonClass", "()Lorg/jruby/RubyClass;");
                mv.astore(SINGLETONCLASS);
            }

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

                AnnotationBinder.checkForThrows(cd, method);

                final String[] names = anno.name();
                CharSequence name = names.length == 0 ? method.getSimpleName() : names[0];

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

                AnnotationHelper.groupFrameFields(readGroups, writeGroups, anno, method.getSimpleName().toString());
            }

            if (methodCount == 0) {
                // no annotated methods found, skip
                return;
            }

            classNames.add(getActualQualifiedName(cd));

            processMethodDeclarations(staticAnnotatedMethods);

            List<ExecutableElement> simpleNames = new ArrayList<>();
            Map<CharSequence, List<ExecutableElement>> complexNames = new HashMap<>();

            for (Map.Entry<CharSequence, List<ExecutableElement>> entry : staticAnnotatedMethods.entrySet()) {
                ExecutableElement decl = entry.getValue().get(0);
                JRubyMethod anno = decl.getAnnotation(JRubyMethod.class);

                if (anno.omit()) continue;

                CharSequence rubyName = entry.getKey();

                if (decl.getSimpleName().equals(rubyName) && anno.name().length <= 1) {
                    simpleNames.add(decl);
                    continue;
                }

                List<ExecutableElement> complex = complexNames.get(rubyName);
                if (complex == null) complexNames.put(rubyName, complex = new ArrayList<ExecutableElement>());
                complex.add(decl);
            }

            processMethodDeclarations(annotatedMethods);

            for (Map.Entry<CharSequence, List<ExecutableElement>> entry : annotatedMethods.entrySet()) {
                ExecutableElement decl = entry.getValue().get(0);
                JRubyMethod anno = decl.getAnnotation(JRubyMethod.class);

                if (anno.omit()) continue;

                CharSequence rubyName = entry.getKey();

                if (decl.getSimpleName().equals(rubyName) && anno.name().length <= 1) {
                    simpleNames.add(decl);
                    continue;
                }

                List<ExecutableElement> complex = complexNames.get(rubyName);
                if (complex == null) complexNames.put(rubyName, complex = new ArrayList<ExecutableElement>());
                complex.add(decl);
            }

            addCoreMethodMapping(cd, complexNames);

            addSimpleMethodMappings(cd, simpleNames);

            mv.voidreturn();
            mv.end();

            // write out a static initializer for frame names, so it only fires once
            mv = new SkinnyMethodAdapter(cw, ACC_PUBLIC | ACC_STATIC, "<clinit>", "()V", null, null);

            mv.start();

            AnnotationHelper.populateMethodIndex(readGroups, (bits, names) -> emitIndexCode(bits, names, "addMethodReadFieldsPacked"));
            AnnotationHelper.populateMethodIndex(writeGroups, (bits, names) -> emitIndexCode(bits, names, "addMethodWriteFieldsPacked"));

            mv.voidreturn();
            mv.end();

            cw.visitEnd();

            new File(SRC_GEN_DIR).mkdirs();
            FileOutputStream fos = new FileOutputStream(SRC_GEN_DIR + qualifiedName + POPULATOR_SUFFIX + ".class");
            fos.write(cw.toByteArray());
            fos.close();
        }
        catch (IOException ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }

    public void emitIndexCode(Integer bits, String names, String methodName) {
        mv.pushInt(bits);
        mv.ldc(names);
        mv.invokestatic("org/jruby/runtime/MethodIndex", methodName, "(ILjava/lang/String;)V");
    }

    public void processMethodDeclarations(Map<CharSequence, List<ExecutableElement>> declarations) {
        for (Map.Entry<CharSequence, List<ExecutableElement>> entry : declarations.entrySet()) {
            List<ExecutableElement> list = entry.getValue();

            if (list.size() == 1) {
                // single method, use normal logic
                processMethodDeclaration(list.get(0));
            } else {
                // multimethod, new logic
                processMethodDeclarationMulti(list);
            }
        }
    }

    public void processMethodDeclaration(ExecutableElement method) {
        processMethodDeclarationMulti(Collections.singletonList(method));
    }

    public static long getEncodedSignature(JRubyMethod anno) {
        return encodeSignature(anno.required(), anno.optional(), 0, 0, 0, anno.rest(), false);
    }

    public void processMethodDeclarationMulti(List<ExecutableElement> methods) {
        Handle[] handles = new Handle[5];
        List<ExecutableElementDescriptor> descs = new ArrayList<>();
        boolean meta = false;
        boolean isStatic = false;
        JRubyMethod anno = null;
        int min = Integer.MAX_VALUE;
        int max = 0;

        Map<Handle, ExecutableElementDescriptor> handleToDesc = new HashMap<>();

        for (ExecutableElement method : methods) {
            anno = method.getAnnotation(JRubyMethod.class);
            ExecutableElementDescriptor desc = new ExecutableElementDescriptor(method);
            descs.add(desc);

            if (anno != null && mv != null) {
                isStatic |= desc.isStatic;
                CharSequence qualifiedName = desc.declaringClassName;

                boolean hasContext = desc.hasContext;
                boolean hasBlock = desc.hasBlock;

                StringBuilder buffer = new StringBuilder(method.getReturnType().toString()).append(" foo(");
                boolean first = true;
                for (VariableElement parameter : method.getParameters()) {
                    if (!first) buffer.append(',');
                    first = false;
                    buffer.append(parameter.asType().toString());
                }
                buffer.append(')');

                Handle handle = new Handle(
                        isStatic ? H_INVOKESTATIC : H_INVOKEVIRTUAL,
                        qualifiedName.toString().replace('.', '/'),
                        method.getSimpleName().toString(),
                        Method.getMethod(buffer.toString()).getDescriptor(),
                        false);

                int handleOffset = calculateHandleOffset(method.getParameters().size(), anno.required(), anno.optional(), anno.rest(), isStatic, hasContext, hasBlock);

                handles[handleOffset] = handle;
                handleToDesc.put(handle, desc);

                meta |= anno.meta();

                int specificArity = desc.calculateSpecificCallArity();

                if (specificArity != -1) {
                    if (specificArity < min) min = specificArity;
                    if (specificArity > max) max = specificArity;
                } else {
                    if (desc.required < min) min = desc.required;
                    if (desc.rest) max = Integer.MAX_VALUE;
                    if (desc.required + desc.optional > max) max = desc.required + desc.optional;
                }
            }
        }

        int implClass = meta ? SINGLETONCLASS : CLASS;

        mv.newobj("org/jruby/internal/runtime/methods/HandleMethod");
        mv.dup();

        mv.aload(implClass);
        mv.getstatic(p(Visibility.class), anno.visibility().name(), ci(Visibility.class));
        mv.ldc(AnnotationBinder.getBaseName(anno.name(), methods.get(0)));
        mv.ldc(encodeSignature(0, 0, 0, 0, 0, true, false));
        mv.ldc(true);
        mv.ldc(anno.notImplemented());

        DescriptorInfo info = new DescriptorInfo(descs);

        mv.ldc(info.getParameterDesc());

        mv.ldc(min);
        mv.ldc(max);

        for (int i = 0; i < 5; i++) {
            if (handles[i] != null) {
                mv.ldc(handles[i]);

                adaptHandle(handleToDesc.get(handles[i]), implClass);
            } else {
                mv.aconst_null();
            }
        }

        Method handleInit = Method.getMethod("void foo(org.jruby.RubyModule, org.jruby.runtime.Visibility, java.lang.String, long, boolean, boolean, java.lang.String, int, int, java.util.concurrent.Callable, java.util.concurrent.Callable, java.util.concurrent.Callable, java.util.concurrent.Callable, java.util.concurrent.Callable)");
        mv.invokespecial("org/jruby/internal/runtime/methods/HandleMethod", "<init>", handleInit.getDescriptor());

        mv.astore(BASEMETHOD);

        generateMethodAddCalls(methods.get(0), anno);
    }

    public void adaptHandle(ExecutableElementDescriptor executableElementDescriptor, int implClass) {
        ExecutableElementDescriptor desc = executableElementDescriptor;

        // adapt handle
        mv.aload(RUNTIME);
        mv.ldc(calculateActualRequired(desc.method, desc.method.getParameters().size(), desc.optional, desc.rest, desc.isStatic, desc.hasContext, desc.hasBlock));
        mv.ldc(desc.required);
        mv.ldc(desc.optional);
        mv.ldc(desc.rest);
        mv.ldc(desc.rubyName);
        mv.ldc(Type.getObjectType(desc.declaringClassPath));
        mv.ldc(desc.isStatic);
        mv.ldc(desc.hasContext);
        mv.ldc(desc.hasBlock);
        mv.ldc(desc.anno.frame());
        mv.aload(implClass);
        mv.invokestatic("org/jruby/internal/runtime/methods/InvokeDynamicMethodFactory", "adaptHandle", Method.getMethod("java.util.concurrent.Callable adaptHandle(java.lang.invoke.MethodHandle, org.jruby.Ruby, int, int, int, boolean, java.lang.String, java.lang.Class, boolean, boolean, boolean, boolean, org.jruby.RubyModule)").getDescriptor());
    }

    private void addCoreMethodMapping(TypeElement cls, Map<CharSequence, List<ExecutableElement>> complexNames) {
        StringBuilder encoded = new StringBuilder();

        for (Map.Entry<CharSequence, List<ExecutableElement>> entry : complexNames.entrySet()) {

            for (Iterator<ExecutableElement> iterator = entry.getValue().iterator(); iterator.hasNext(); ) {
                if (encoded.length() > 0) encoded.append(";");

                ExecutableElement elt = iterator.next();
                encoded
                        .append(elt.getSimpleName())
                        .append(";")
                        .append(entry.getKey());
            }
        }

        if (encoded.length() == 0) return;

        mv.aload(RUNTIME);
        mv.ldc(cls.getQualifiedName().toString());
        mv.ldc(encoded.toString());
        mv.invokevirtual("org/jruby/Ruby", "addBoundMethodsPacked", "(Ljava/lang/String;Ljava/lang/String;)V");
    }

    private void addSimpleMethodMappings(TypeElement cls, List<ExecutableElement> simpleNames) {
        StringBuilder encoded = new StringBuilder();
        for (ExecutableElement elt : simpleNames) {
            if (encoded.length() > 0) encoded.append(";");
            encoded.append(elt.getSimpleName());
        }

        if (encoded.length() == 0) return;

        mv.aload(RUNTIME);
        mv.ldc(cls.getSimpleName().toString());
        mv.ldc(encoded.toString());
        mv.invokevirtual("org/jruby/Ruby", "addSimpleBoundMethodsPacked", "(Ljava/lang/String;Ljava/lang/String;)V");
    }

    private static CharSequence getActualQualifiedName(TypeElement td) {
        if (td.getNestingKind() == NestingKind.MEMBER) {
            return getActualQualifiedName((TypeElement)td.getEnclosingElement()) + "$" + td.getSimpleName();
        }
        return td.getQualifiedName().toString();
    }

    // FIXME: duplicated from Signature, since it pulls in org.jruby.Ruby

    private static final int MAX_ENCODED_ARGS_EXPONENT = 8;
    private static final int MAX_ENCODED_ARGS_MASK = 0xFF;
    private static final int ENCODE_RESTKWARGS_SHIFT = 0;
    private static final int ENCODE_REST_SHIFT = ENCODE_RESTKWARGS_SHIFT + 1;
    private static final int ENCODE_REQKWARGS_SHIFT = ENCODE_REST_SHIFT + MAX_ENCODED_ARGS_EXPONENT;
    private static final int ENCODE_KWARGS_SHIFT = ENCODE_REQKWARGS_SHIFT + MAX_ENCODED_ARGS_EXPONENT;
    private static final int ENCODE_POST_SHIFT = ENCODE_KWARGS_SHIFT + MAX_ENCODED_ARGS_EXPONENT;
    private static final int ENCODE_OPT_SHIFT = ENCODE_POST_SHIFT + MAX_ENCODED_ARGS_EXPONENT;
    private static final int ENCODE_PRE_SHIFT = ENCODE_OPT_SHIFT + MAX_ENCODED_ARGS_EXPONENT;

    public static long encodeSignature(int pre, int opt, int post, int kwargs, int requiredKwargs, boolean rest, boolean restKwargs) {
        return
                ((long)pre << ENCODE_PRE_SHIFT) |
                        ((long)opt << ENCODE_OPT_SHIFT) |
                        ((long)post << ENCODE_POST_SHIFT) |
                        ((long)kwargs << ENCODE_KWARGS_SHIFT) |
                        ((long)requiredKwargs << ENCODE_REQKWARGS_SHIFT) |
                        ((rest ? 1 : 0) << ENCODE_REST_SHIFT) |
                        ((restKwargs?1:0) << ENCODE_RESTKWARGS_SHIFT);
    }

    private static int calculateActualRequired(ExecutableElement md, int paramsLength, int optional, boolean rest, boolean isStatic, boolean hasContext, boolean hasBlock) {
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

    private static int calculateHandleOffset(int paramsLength, int required, int optional, boolean rest, boolean isStatic, boolean hasContext, boolean hasBlock) {
        if (required < 4 && optional == 0 && !rest) {
            int args = paramsLength;
            if (args == 0) {
                return 0;
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
                return args;
            }
        } else {
            return 4;
        }
    }

    public void generateMethodAddCalls(ExecutableElement md, JRubyMethod jrubyMethod) {
        final String[] names = jrubyMethod.name();
        final String[] aliases = jrubyMethod.alias();
        if (jrubyMethod.meta()) {
            defineMethodOnClass(BASEMETHOD, SINGLETONCLASS, names, aliases, md);
        } else {
            defineMethodOnClass(BASEMETHOD, CLASS, names, aliases, md);
            if (jrubyMethod.module()) {
                mv.aload(CLASS);
                mv.aload(BASEMETHOD);
                mv.invokestatic("org/jruby/anno/TypePopulator", "populateModuleMethod", "(Lorg/jruby/RubyModule;Lorg/jruby/internal/runtime/methods/DynamicMethod;)Lorg/jruby/internal/runtime/methods/DynamicMethod;");
                mv.astore(MODULEMETHOD);
                defineMethodOnClass(MODULEMETHOD, SINGLETONCLASS, names, aliases, md);
            }
        }
    }

    private void defineMethodOnClass(int methodVar, int classVar, final String[] names, final String[] aliases, ExecutableElement md) {
        final String baseName;
        if (names.length == 0) {
            baseName = md.getSimpleName().toString();
            mv.aload(classVar);
            mv.ldc(baseName);
            mv.aload(methodVar);
            mv.invokevirtual("org/jruby/RubyModule", "addMethodAtBootTimeOnly", "(Ljava/lang/String;Lorg/jruby/internal/runtime/methods/DynamicMethod;)V");
        } else {
            baseName = names[0];
            for (String name : names) {
                mv.aload(classVar);
                mv.ldc(name);
                mv.aload(methodVar);
                mv.invokevirtual("org/jruby/RubyModule", "addMethodAtBootTimeOnly", "(Ljava/lang/String;Lorg/jruby/internal/runtime/methods/DynamicMethod;)V");
            }
        }

        if (aliases.length > 0) {
            for (String alias : aliases) {
                mv.aload(classVar);
                mv.ldc(alias);
                mv.ldc(baseName);
                mv.invokevirtual("org/jruby/RubyModule", "defineAlias", "(Ljava/lang/String;Ljava/lang/String;)V");
            }
        }
    }
}
