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
import org.jruby.ReifiedRubyObject;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyInstanceConfig;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ClassDefiningClassLoader;
import org.jruby.util.CodegenUtils;
import org.jruby.util.OneShotClassLoader;
import org.jruby.util.collections.NonBlockingHashMapLong;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.LabelNode;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Set;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;
import static org.objectweb.asm.Opcodes.*;

/**
 * A code generator for Ruby objects, to map known instance variables into fields.
 */
public class RubyObjectSpecializer {

    private static ObjectAllocator getClassFromSize(int size) {
        return specializedFactories.get(size);
    }

    private static final NonBlockingHashMapLong<ObjectAllocator> specializedFactories = new NonBlockingHashMapLong<>();

    private static ClassDefiningClassLoader CDCL = new OneShotClassLoader(Ruby.getClassLoader());

    public static ObjectAllocator specializeForVariables(RubyClass klass, Set<String> foundVariables) {
        int size = foundVariables.size();
        ObjectAllocator allocator = getClassFromSize(size);

        if (allocator != null) return allocator;

        final String clsPath = "org/jruby/specialize/RubyObject" + size;
        final String clsName = clsPath.replaceAll("/", ".");

        // try to load the class, in case we have parallel generation happening
        Class p;

        try {
            p = CDCL.loadClass(clsName);
        } catch (ClassNotFoundException cnfe) {
            // try again under lock
            synchronized (CDCL) {
                try {
                    p = CDCL.loadClass(clsName);
                } catch (ClassNotFoundException cnfe2) {
                    // proceed to actually generate the class
                    p = generateInternal(klass, foundVariables, clsPath, clsName);
                }
            }
        }

        // acquire constructor handle and store it
        try {
            // should only be one, the allocator we want
            Class allocatorCls = p.getDeclaredClasses()[0];
            allocator = (ObjectAllocator) allocatorCls.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        klass.setAllocator(allocator);
        klass.setReifiedClass(p);

        return allocator;
    }


    private static Class generateInternal(RubyClass klass, final Set<String> names, final String clsPath, final String clsName) {
        // ensure only one thread will attempt to generate and define the new class
        synchronized (CDCL) {
            // set up table for all names and gather count
            int i = 0;
            for (String name : names) {
                klass.getVariableTableManager().getVariableAccessorForVar(name, i);
                i++;
            }
            final int count = i;

            // create a new one
            final String[] newFields = varList(count);

            final String baseName = p(ReifiedRubyObject.class);
            final String allocatorPath = clsPath + "Allocator";

            final JiteClass jiteClass = new JiteClass(clsPath, baseName, new String[0]) {{
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

                    if (count > 0) genGetSwitch(clsPath, newFields, this, 1);

                    line(1);

                    aload(0);
                    iload(1);
                    invokespecial(p(ReifiedRubyObject.class), "getVariable", sig(Object.class, int.class));
                    areturn();
                }});

                defineMethod("setVariable", ACC_PUBLIC, sig(void.class, int.class, Object.class), new CodeBlock() {{
                    LabelNode parentCall = new LabelNode(new Label());

                    line(2);

                    if (count > 0) genPutSwitch(clsPath, newFields, this, 1);

                    line(3);

                    aload(0);
                    iload(1);
                    aload(2);
                    invokespecial(p(ReifiedRubyObject.class), "setVariable", sig(void.class, int.class, Object.class));
                    voidreturn();
                }});

                for (int i = 0; i < count; i++) {
                    final int offset = i;

                    defineMethod("getVariable" + offset, ACC_PUBLIC, sig(Object.class), new CodeBlock() {{
                        line(4);
                        aload(0);
                        getfield(clsPath, newFields[offset], ci(Object.class));
                        areturn();
                    }});
                }

                for (int i = 0; i < count; i++) {
                    final int offset = i;

                    defineMethod("setVariable" + offset, ACC_PUBLIC, sig(void.class, Object.class), new CodeBlock() {{
                        line(5);
                        aload(0);
                        aload(1);
                        putfield(clsPath, newFields[offset], ci(Object.class));
                        voidreturn();
                    }});
                }

                // fields
                for (String prop : newFields) {
                    defineField(prop, ACC_PUBLIC, ci(Object.class), null);
                }

                // allocator class
                addChildClass(new JiteClass(allocatorPath, p(Object.class), Helpers.arrayOf(p(ObjectAllocator.class))) {{
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
    }

    private static String[] varList(int size) {
        String[] vars = new String[size];

        for (int i = 0; i < size; i++) {
            vars[i] = "var" + i;
        }

        return vars;
    }

    private static void genGetSwitch(String clsPath, String[] newFields, CodeBlock block, int offsetVar) {
        LabelNode defaultError = new LabelNode(new Label());
        int size = newFields.length;
        LabelNode[] cases = new LabelNode[size];
        for (int i = 0; i < size; i++) {
            cases[i] = new LabelNode(new Label());
        }
        block.iload(offsetVar);
        block.tableswitch(0, size - 1, defaultError, cases);
        for (int i = 0; i < size; i++) {
            block.label(cases[i]);
            block.aload(0);
            block.getfield(clsPath, newFields[i], ci(Object.class));
            block.areturn();
        }
        block.label(defaultError);
    }

    private static void genPutSwitch(String clsPath, String[] newFields, CodeBlock block, int offsetVar) {
        LabelNode defaultError = new LabelNode(new Label());
        int size = newFields.length;
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
            block.putfield(clsPath, newFields[i], ci(Object.class));
            block.voidreturn();
        }
        block.label(defaultError);
    }

    private static Class defineClass(JiteClass jiteClass) {
        return CDCL.defineClass(classNameFromJiteClass(jiteClass), jiteClass.toBytes(JDKVersion.V1_7));
    }

    private static String classNameFromJiteClass(JiteClass jiteClass) {
        return jiteClass.getClassName().replaceAll("/", ".");
    }
}
