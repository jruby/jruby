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
import org.jruby.RubyObjectShaped;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;
import org.jruby.util.collections.NonBlockingHashMapLong;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.LabelNode;

import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/**
 * A code generator for Ruby objects, to map known instance variables into fields.
 */
public class RubyObjectSpecializer {

    public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final String GENERATED_PACKAGE = "org/jruby/gen/";

    private final Ruby runtime;

    private ClassAndAllocator getClassForSize(int size) {
        return specializedClasses.get(size);
    }

    private final NonBlockingHashMapLong<ClassAndAllocator> specializedClasses = new NonBlockingHashMapLong<>();

    public RubyObjectSpecializer(Ruby runtime) {
        this.runtime = runtime;
    }

    static class ClassAndAllocator {
        final Class cls;
        final ObjectAllocator allocator;

        ClassAndAllocator(Class cls, ObjectAllocator allocator) {
            this.cls = cls;
            this.allocator = allocator;
        }
    }

    public ObjectAllocator specializeForVariables(RubyClass klass, Set<String> foundVariables) {
        // clamp to max object width
        int size = Math.min(foundVariables.size(), Options.REIFY_VARIABLES_MAX.load());

        ClassAndAllocator cna;

        if (Options.REIFY_VARIABLES_NAME.load()) {
            // use Ruby class name for debugging, profiling
            cna = generateSpecializedRubyObject(uniqueClassName(klass), size, false);
        } else {
            // Generic class for specified size
            cna = getClassForSize(size);

            if (cna == null) {
                cna = generateSpecializedRubyObject(genericClassName(size), size, true);
            }
        }

        // Pre-initialize variable table with field accessors for size
        try {
            int offset = 0;

            // TODO: this just ends up reifying the first N variables it finds, which may not be the most valuable
            for (String name : foundVariables) {
                klass.getVariableTableManager().getVariableAccessorForRubyVar(
                        name,
                        LOOKUP.findGetter(cna.cls, "var" + offset, Object.class),
                        LOOKUP.findSetter(cna.cls, "var" + offset, Object.class));
                offset++;
                if (offset >= size) break;
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        klass.reifiedClass(cna.cls).allocator(cna.allocator);

        return cna.allocator;
    }

    private ClassAndAllocator generateSpecializedRubyObject(String className, int size, boolean cache) {
        ClassAndAllocator cna;

        synchronized (this) {
            Class specialized;
            try {
                // try loading class without generating
                specialized = runtime.getJRubyClassLoader().loadClass(className.replace('/', '.'));
            } catch (ClassNotFoundException cnfe) {
                // generate specialized class
                specialized = generateInternal(className, size);
            }

            try {
                ObjectAllocator allocator = (ObjectAllocator) specialized.getDeclaredClasses()[0].getConstructor().newInstance();

                cna = new ClassAndAllocator(specialized, allocator);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        if (cache) {
            specializedClasses.put(size, cna);
        }

        return cna;
    }

    private static String genericClassName(int size) {
        return GENERATED_PACKAGE + "RubyObject" + size;
    }

    private static String uniqueClassName(RubyClass klass) {
        String className = klass.getName();

        if (className.startsWith("#")) {
            className = "Anonymous" + Integer.toHexString(System.identityHashCode(klass));
        } else {
            className = className.replace("::", "/");
        }

        return GENERATED_PACKAGE + className;
    }

    /**
     * Emit all generic RubyObject specializations to disk, so they do not need to generate at runtime.
     */
    public static void main(String[] args) throws Throwable {
        String targetPath = args[0];

        Files.createDirectories(Paths.get(targetPath, GENERATED_PACKAGE));

        int maxVars = Options.REIFY_VARIABLES_MAX.load();
        for (int i = 0; i <= maxVars; i++) {
            String clsPath = genericClassName(i);
            JiteClass jcls = generateJiteClass(clsPath, i);
            Files.write(Paths.get(targetPath, clsPath + ".class"), jcls.toBytes(JDKVersion.V1_8));
            Files.write(Paths.get(targetPath, clsPath + "Allocator.class"), jcls.getChildClasses().get(0).toBytes(JDKVersion.V1_8));
        }
    }

    private Class generateInternal(final String clsPath, int size) {
        final JiteClass jiteClass = generateJiteClass(clsPath, size);

        Class specializedClass = defineClass(jiteClass);
        defineClass(jiteClass.getChildClasses().get(0));

        return specializedClass;
    }

    private static JiteClass generateJiteClass(String clsPath, int size) {
        // ensure only one thread will attempt to generate and define the new class
        final String baseName = p(RubyObjectShaped.class);

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
        return jiteClass;
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

    private Class defineClass(JiteClass jiteClass) {
        return runtime.getJRubyClassLoader().defineClass(classNameFromJiteClass(jiteClass), jiteClass.toBytes(JDKVersion.V1_8));
    }

    private static String classNameFromJiteClass(JiteClass jiteClass) {
        return jiteClass.getClassName().replace('/', '.');
    }
}
