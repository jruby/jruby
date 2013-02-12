/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
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
package org.jruby.util.unsafe;

import java.io.FileOutputStream;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.objectweb.asm.ClassWriter;
import static org.objectweb.asm.Opcodes.*;
import static org.jruby.util.CodegenUtils.*;

public class UnsafeGenerator {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Two args please, the target package and directory");
            System.exit(1);
        }
        
        String pkg = args[0].replace('.', '/');
        String dir = args[1];
        String classname = pkg + "/GeneratedUnsafe";
        String classpath = dir + "/GeneratedUnsafe.class";
        
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, classname, null, p(Object.class), new String[] {p(Unsafe.class)});
        cw.visitSource("<generated>", null);
        
        SkinnyMethodAdapter method = new SkinnyMethodAdapter(cw, ACC_PUBLIC, "<init>", sig(void.class), null, null);
        method.start();
        method.line(0);
        method.aload(0);
        method.invokespecial(p(Object.class), "<init>", sig(void.class));
        method.voidreturn();
        method.end();
        
        method = new SkinnyMethodAdapter(cw, ACC_PUBLIC, "throwException", sig(void.class, Throwable.class), null, null);
        method.line(0);
        method.start();
        method.aload(1);
        method.athrow();
        method.end();
        
        cw.visitEnd();
        
        byte[] bytecode = cw.toByteArray();
        
        try {
            FileOutputStream fos = new FileOutputStream(classpath);
            fos.write(bytecode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
