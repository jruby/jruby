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
import org.jruby.platform.Platform;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ModuleLoader {
    public static final String libext = Platform.IS_MAC ? "bundle" : "so";

    
    public void load(IRubyObject recv, String name) {
        ThreadContext context = recv.getRuntime().getCurrentContext();

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
        System.out.println("calling init");

        ExecutionLock.lock(context);
        try {
            n.callInit(context, init);
        } finally {
            ExecutionLock.unlock(context);
        }
    }
}
