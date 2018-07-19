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
package org.jruby.specialized;

import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.JDKVersion;
import me.qmx.jitescript.JiteClass;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ClassDefiningClassLoader;
import org.jruby.util.OneShotClassLoader;
import org.jruby.util.collections.NonBlockingHashMapLong;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.LabelNode;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Set;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/**
 * A code generator for Ruby objects, to map known instance variables into fields.
 */
public class RubyObjectSpecializer {

    public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static ClassAndAllocator getClassForSize(int size) {
        return SPECIALIZED_CLASSES.get(size);
    }

    private static final NonBlockingHashMapLong<ClassAndAllocator> SPECIALIZED_CLASSES = new NonBlockingHashMapLong<>();

    private static final ClassDefiningClassLoader LOADER = new OneShotClassLoader(Ruby.getClassLoader());

    static class ClassAndAllocator {
        final Class cls;
        final ObjectAllocator allocator;

        ClassAndAllocator(Class cls, ObjectAllocator allocator) {
            this.cls = cls;
            this.allocator = allocator;
        }
    }

    public static ObjectAllocator specializeForVariables(RubyClass klass, Set<String> foundVariables) {
        int size = foundVariables.size();
        ClassAndAllocator cna = getClassForSize(size);

        if (cna == null) {
            final String clsPath = "org/jruby/gen/RubyObject" + size;

            Class specialized;
            synchronized (LOADER) {
                try {
                    // try loading class without generating
                    specialized = LOADER.loadClass(clsPath.replace('/', '.'));
                } catch (ClassNotFoundException cnfe) {
                    // generate specialized class
                    specialized = generateInternal(foundVariables.size(), clsPath);
                }

                try {
//                    MethodHandle allocatorHandle = LOOKUP
//                            .findConstructor(specialized, MethodType.methodType(void.class, Ruby.class, RubyClass.class))
//                            .asType(MethodType.methodType(IRubyObject.class, Ruby.class, RubyClass.class));

                    // LMF version not working currently
//                    MethodType invokeType = MethodType.methodType(specialized, Ruby.class, RubyClass.class);
//                    MethodType smaType = MethodType.methodType(IRubyObject.class, Ruby.class, RubyClass.class);
//                    CallSite allocatorSite = LambdaMetafactory.metafactory(
//                            LOOKUP,
//                            "allocate",
//                            MethodType.methodType(ObjectAllocator.class),
//                            smaType,
//                            allocatorHandle,
//                            invokeType);
//
//                    ObjectAllocator allocator = (ObjectAllocator) allocatorSite.dynamicInvoker().invokeExact();

//                    ObjectAllocator allocator = (runtime, klazz) -> {
//                        try {
//                            return (IRubyObject) allocatorHandle.invokeExact(runtime, klazz);
//                        } catch (Throwable t) {
//                            Helpers.throwException(t);
//                            return null;
//                        }
//                    };

                    ObjectAllocator allocator = (ObjectAllocator) specialized.getDeclaredClasses()[0].newInstance();

                    SPECIALIZED_CLASSES.put(size, cna = new ClassAndAllocator(specialized, allocator));
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        }

        try {
            int offset = 0;
            for (String name : foundVariables) {
                klass.getVariableTableManager().getVariableAccessorForVar(
                        name,
                        LOOKUP.findGetter(cna.cls, "var" + offset, Object.class),
                        LOOKUP.findSetter(cna.cls, "var" + offset, Object.class));
                offset++;
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        klass.setReifiedClass(cna.cls);
        klass.setAllocator(cna.allocator);

        return cna.allocator;
    }

    private static Class generateInternal(int size, final String clsPath) {
        // ensure only one thread will attempt to generate and define the new class
        final String baseName = p(RubyObject.class);

        final JiteClass jiteClass = new JiteClass(clsPath, baseName, new String[0]) {{
            for (int i = 0; i < size; i++) {
                final int offset = i;

                // fields
                defineField("var" + offset, ACC_PUBLIC, ci(Object.class), null);
            }

            // parent class constructor
            defineMethod("<init>", ACC_PUBLIC, sig(void.class, Ruby.class, RubyClass.class), new CodeBlock() {{
                aload(0);
                aload(1);
                aload(2);
                invokespecial(baseName, "<init>", sig(void.class, Ruby.class, RubyClass.class));
                voidreturn();
            }});

            // required overrides
            defineMethod("getVariable", ACC_PUBLIC, sig(Object.class, int.class), new CodeBlock() {{
                LabelNode parentCall = new LabelNode(new Label());

                line(0);

                if (size > 0) genGetSwitch(clsPath, size, this, 1);

                line(1);

                aload(0);
                iload(1);
                invokespecial(p(RubyObject.class), "getVariable", sig(Object.class, int.class));
                areturn();
            }});

            defineMethod("setVariable", ACC_PUBLIC, sig(void.class, int.class, Object.class), new CodeBlock() {{
                LabelNode parentCall = new LabelNode(new Label());

                line(2);

                if (size > 0) genPutSwitch(clsPath, size, this, 1);

                line(3);

                aload(0);
                iload(1);
                aload(2);
                invokespecial(p(RubyObject.class), "setVariable", sig(void.class, int.class, Object.class));
                voidreturn();
            }});

            // allocator class
            addChildClass(new JiteClass(clsPath + "Allocator", p(Object.class), Helpers.arrayOf(p(ObjectAllocator.class))) {{
                defineDefaultConstructor();

                defineMethod("allocate", ACC_PUBLIC, sig(IRubyObject.class, Ruby.class, RubyClass.class), new CodeBlock() {{
                    newobj(clsPath);
                    dup();
                    aload(1);
                    aload(2);
                    invokespecial(clsPath, "<init>", sig(void.class, Ruby.class, RubyClass.class));
                    areturn();
                }});
            }});
        }};

        Class specializedClass = defineClass(jiteClass);
        defineClass(jiteClass.getChildClasses().get(0));

        return specializedClass;
    }

    private static void genGetSwitch(String clsPath, int size, CodeBlock block, int offsetVar) {
        LabelNode defaultError = new LabelNode(new Label());
        LabelNode[] cases = new LabelNode[size];
        for (int i = 0; i < size; i++) {
            cases[i] = new LabelNode(new Label());
        }
        block.iload(offsetVar);
        block.tableswitch(0, size - 1, defaultError, cases);
        for (int i = 0; i < size; i++) {
            block.label(cases[i]);
            block.aload(0);
            block.getfield(clsPath, "var" + i, ci(Object.class));
            block.areturn();
        }
        block.label(defaultError);
    }

    private static void genPutSwitch(String clsPath, int size, CodeBlock block, int offsetVar) {
        LabelNode defaultError = new LabelNode(new Label());
        LabelNode[] cases = new LabelNode[size];
        for (int i = 0; i < size; i++) {
            cases[i] = new LabelNode(new Label());
        }
        block.iload(offsetVar);
        block.tableswitch(0, size - 1, defaultError, cases);
        for (int i = 0; i < size; i++) {
            block.label(cases[i]);
            block.aload(0);
            block.aload(2);
            block.putfield(clsPath, "var" + i, ci(Object.class));
            block.voidreturn();
        }
        block.label(defaultError);
    }

    private static Class defineClass(JiteClass jiteClass) {
        return LOADER.defineClass(classNameFromJiteClass(jiteClass), jiteClass.toBytes(JDKVersion.V1_8));
    }

    private static String classNameFromJiteClass(JiteClass jiteClass) {
        return jiteClass.getClassName().replace('/', '.');
    }
}
