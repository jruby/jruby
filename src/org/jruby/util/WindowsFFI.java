/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * Copyright (C) 2007-2011 JRuby Team <team@jruby.org>
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.util;

import com.kenai.jaffl.*;
import com.kenai.jaffl.FFIProvider;
import com.kenai.jaffl.LibraryOption;
import java.util.HashMap;
import java.util.Map;

/**
 * A binding of a few key win32 functions we need to behave properly.
 */
public class WindowsFFI {
    public static interface Kernel32 {
     int GetProcessId(NativeLong handle);
    }

    public static Kernel32 getKernel32(FFIProvider provider) {
        Map<LibraryOption, Object> options = new HashMap();
        options.put(LibraryOption.CallingConvention, CallingConvention.STDCALL);
        return provider.loadLibrary("Kernel32.dll", Kernel32.class, options);
    }
}
