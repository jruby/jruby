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
 * Copyright (C) 2008, 2009 Wayne Meissner
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

package org.jruby.cext;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;

import com.kenai.jffi.Library;

/**
 * The {@link ModuleLoader} is responsible for loading native extensions into the process and calling
 * their initialization function.
 */
public class ModuleLoader {
    private static final Set<Library> modules = new HashSet<Library>();
    
    public static void load(Ruby runtime, String name) {
        ThreadContext context = runtime.getCurrentContext();

        // Ensure the native code is initialized before we load the library
        Native n = Native.getInstance(runtime);

        Library lib = Library.openLibrary(name, Library.LAZY | Library.GLOBAL);
        if (lib == null) {
            throw new UnsatisfiedLinkError(Library.getLastError());
        }

        String fileName = new File(name).getName();
        String initName = fileName;
        int dotPos;
        if ((dotPos = fileName.lastIndexOf('.')) != -1) {
            // Remove the extension, if needed
            initName = fileName.substring(0, dotPos);
        }
        long init = lib.getSymbolAddress("Init_" + initName);
        
        if (init == 0) {
            throw new UnsatisfiedLinkError("Could not locate Init_" + initName + " module entry point");
        }
        
        GIL.acquire();
        try {
            n.callInit(context, init);
            modules.add(lib);
        } finally {
            GIL.release();
        }
    }
}
