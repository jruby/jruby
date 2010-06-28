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

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.jruby.Ruby;
import org.jruby.platform.Platform;
import org.jruby.runtime.ThreadContext;

import com.kenai.jffi.Library;

public class ModuleLoader {
    public static final String libext = Platform.IS_MAC ? "bundle" : "so";
    private static final Set<Library> modules = new HashSet<Library>();
    
    public void load(Ruby runtime, String name) {
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
        
        ExecutionLock.lock();
        try {
            System.out.printf("calling init (%x)\n", init);
            n.callInit(context, init);
            modules.add(lib);
        } finally {
            ExecutionLock.unlock(context);
        }
    }
}
