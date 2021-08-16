/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * Copyright (C) 2007-2011 JRuby Team <team@jruby.org>
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

package org.jruby.util;

import jnr.ffi.annotations.Out;
import jnr.ffi.byref.IntByReference;
import jnr.ffi.types.intptr_t;
import jnr.ffi.CallingConvention;
import jnr.ffi.LibraryLoader;

/**
 * A binding of a few key win32 functions we need to behave properly.
 */
public class WindowsFFI {
    public static interface Kernel32 {
        public static final int PROCESS_QUERY_INFORMATION  = 0x0400;
        public static final int ERROR_INVALID_PARAMETER = 0x57;
        public static final int PROCESS_TERMINATE  = 0x0001;
        public static final int STILL_ACTIVE = 259;

        int GetProcessId(@intptr_t long handle);
        jnr.ffi.Pointer OpenProcess(int dwDesiredAccess, int bInheritHandle, int dwProcessId);
        int CloseHandle(jnr.ffi.Pointer handle);
        int GetLastError();
        int SetLastError(int ErrorCode);
        int GetExitCodeProcess(jnr.ffi.Pointer hProcess, @Out IntByReference pointerToExitCodeDword);
        int TerminateProcess(jnr.ffi.Pointer hProcess, int uExitCode);
    }

    private static final class SingletonHolder {
        static final Kernel32 Kernel32 = LibraryLoader.create(Kernel32.class)
                .convention(CallingConvention.STDCALL)
                .load("Kernel32");
    }

    public static Kernel32 getKernel32() {
        return kernel32();
    }

    public static Kernel32 kernel32() {
        return SingletonHolder.Kernel32;
    }
}
