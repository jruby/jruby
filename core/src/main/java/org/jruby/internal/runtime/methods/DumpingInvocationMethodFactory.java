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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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
package org.jruby.internal.runtime.methods;

import java.io.File;
import java.io.FileOutputStream;

import org.objectweb.asm.ClassWriter;

/**
 * This factory extends InvocationMethodFactory by also dumping the classes to
 * .class files at runtime. It is used during the build to save off all
 * generated method handles to avoid that expense during startup.
 * 
 * @see org.jruby.internal.runtime.methods.InvocationMethodFactory
 */
public class DumpingInvocationMethodFactory extends InvocationMethodFactory {

    private String dumpPath;
    
    public DumpingInvocationMethodFactory(String path, ClassLoader classLoader) {
        super(classLoader);
        this.dumpPath = path;
    }

    @Override
    protected Class endClass(ClassWriter cw, String name) {
        cw.visitEnd();
        byte[] code = cw.toByteArray();
        String cname = name.replace('.','/');
        File f = new File(dumpPath,cname+".class");
        f.getParentFile().mkdirs();
        try {
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(code);
            fos.close();
        } catch(Exception e) {
        }
        return classLoader.defineClass(name, code);
    }
}// DumpingInvocationMethodFactory
