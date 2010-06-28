/*
 * Copyright (C) 2008, 2009 Wayne Meissner
 *
 * This file is part of jruby-cext.
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jruby.cext;

import com.kenai.jffi.Library;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import org.jruby.platform.Platform;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ModuleLoader {
    public static final String libext = Platform.IS_MAC ? "bundle" : "so";
    private static final Set<Library> modules = new HashSet<Library>();
    
    public void load(IRubyObject recv, String name) {
        ThreadContext context = recv.getRuntime().getCurrentContext();
        
        Library lib = null;
        try {
            URI uri = new URI(name);
            if (uri.getScheme() != null && uri.getScheme().equals("classpath")) {
                // This means, the native lib was found through direct require in the classpath
                String localPath = uri.getPath().substring(1); // remove leading slash
                lib = Library.openLibrary(new File(localPath).getCanonicalPath() + '.' + libext, Library.LAZY | Library.GLOBAL);
                name = new File(uri.getPath()).getName();
            }
        } catch (URISyntaxException e) {
        } catch (IOException e) {
        } finally {
            if (lib == null) {
                // Try loading from system library path
                lib = Library.openLibrary(name + "." + libext, Library.LAZY | Library.GLOBAL);
            }
        }
        
        if (lib == null) {
            throw new UnsatisfiedLinkError(Library.getLastError());
        }

        String initName = new File(name).getName();
        long init = lib.getSymbolAddress("Init_" + initName);
        
        if (init == 0) {
            throw new UnsatisfiedLinkError("Could not locate Init_" + initName + " module entry point");
        }
        
        ExecutionLock.lock();
        try {
            // Ensure the native code is initialized before we load the library
            Native n = Native.getInstance(context.getRuntime());

            Library lib = Library.openLibrary(name + "." + libext, Library.LAZY | Library.GLOBAL);
            if (lib == null) {
                throw new UnsatisfiedLinkError(Library.getLastError());
            }

            String initName = new File(name).getName();
            long init = lib.getSymbolAddress("Init_" + initName);

            if (init == 0) {
                throw new UnsatisfiedLinkError("Could not locate Init_" + initName + " module entry point");
            }
            
            System.out.printf("calling init (%x)\n", init);
            n.callInit(context, init);
            modules.add(lib);
        } finally {
            ExecutionLock.unlock(context);
        }
    }
}
