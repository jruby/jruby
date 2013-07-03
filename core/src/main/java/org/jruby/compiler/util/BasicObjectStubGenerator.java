/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2009 Charles O Nutter <headius@headius.com>
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
package org.jruby.compiler.util;

import java.lang.reflect.Method;
import org.jruby.BasicObjectStub;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import static org.jruby.util.CodegenUtils.*;

public class BasicObjectStubGenerator {
    private static final Method[] BASIC_OBJECT_STUB_METHODS = BasicObjectStub.class.getDeclaredMethods();
    public static void addBasicObjectStubsToClass(ClassVisitor cv) {
        for (Method stub : BASIC_OBJECT_STUB_METHODS) {
            if (stub.getName().equals("getRuntime") ||
                    stub.getName().equals("getMetaClass")) {
                // skip these and implement appropriately for the specific case
                continue;
            }
            
            // trim off IRubyObject self argument
            Class[] signature = new Class[stub.getParameterTypes().length - 1];
            for (int i = 0; i < signature.length; i++) {
                signature[i] = stub.getParameterTypes()[i + 1];
            }
            
            SkinnyMethodAdapter method = new SkinnyMethodAdapter(
                    cv, Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE, stub.getName(), sig(stub.getReturnType(), signature), null, null);
            method.start();

            // load self
            method.aload(0);

            // load arguments
            int nextIndex = 1;
            for (Class argType : signature) {
                if (argType.isPrimitive()) {
                    if (argType == boolean.class ||
                            argType == byte.class ||
                            argType == char.class ||
                            argType == short.class ||
                            argType == int.class) {
                        method.iload(nextIndex);
                        nextIndex++;
                    } else if (argType == long.class) {
                        method.lload(nextIndex);
                        nextIndex += 2;
                    } else if (argType == float.class) {
                        method.fload(nextIndex);
                        nextIndex++;
                    } else if (argType == double.class) {
                        method.dload(nextIndex);
                        nextIndex += 2;
                    } else {
                        throw new RuntimeException("unknown primitive type: " + argType);
                    }
                } else {
                    method.aload(nextIndex);
                    nextIndex++;
                }
            }

            // invoke stub
            method.invokestatic(p(BasicObjectStub.class), stub.getName(), sig(stub.getReturnType(), stub.getParameterTypes()));

            Class retType = stub.getReturnType();
            if (retType == void.class) {
                method.voidreturn();
            } else {
                if (retType.isPrimitive()) {
                    if (retType == boolean.class ||
                            retType == byte.class ||
                            retType == char.class ||
                            retType == short.class ||
                            retType == int.class) {
                        method.ireturn();
                    } else if (retType == long.class) {
                        method.lreturn();
                    } else if (retType == float.class) {
                        method.freturn();
                    } else if (retType == double.class) {
                        method.dreturn();
                    } else {
                        throw new RuntimeException("unknown primitive type: " + retType);
                    }
                } else {
                    method.areturn();
                }
            }

            method.end();
        }
    }
}
