/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
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
package org.jruby.test;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

class AlternateLoader extends ClassLoader {

    protected Class findModClass(String name) throws ClassNotFoundException {
        byte[] classBytes = loadClassBytes(name);
        replace(classBytes, "Original", "ABCDEFGH");
        return defineClass(name, classBytes, 0, classBytes.length);
    }
    private void replace(byte[] classBytes, String find, String replaceWith) {
        byte[] findBytes = find.getBytes();
        byte[] replaceBytes = replaceWith.getBytes();
        for (int i=0; i<classBytes.length; i++) {
            boolean match = true;
            for (int j=0; j<findBytes.length; j++) {
                if (classBytes[i+j] != findBytes[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                for (int j=0; j<findBytes.length; j++)
                    classBytes[i+j] = replaceBytes[j];
                return;
            }
        }
    }
    @Override
    public Class loadClass(String name) throws ClassNotFoundException {
        if (name.equals("org.jruby.test.AlternativelyLoaded"))
            return findModClass(name);

        return super.loadClass(name);
    }
    private byte[] loadClassBytes(String name) throws ClassNotFoundException {
        InputStream stream = null;
        try {
            String fileName = name.replaceAll("\\.", "/");
            fileName += ".class";
            byte[] buf = new byte[1024];
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            int bytesRead = 0;
            stream = getClass().getResourceAsStream("/" + fileName);
            while ((bytesRead = stream.read(buf)) != -1) {
                bytes.write(buf, 0, bytesRead);
            }
            return bytes.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ClassNotFoundException(e.getMessage(),e);
        } finally {
            if (stream != null)
                try {
                    stream.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
        }
    }
}
