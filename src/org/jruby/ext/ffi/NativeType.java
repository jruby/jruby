/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
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
 * Copyright (C) 2008 JRuby project
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

package org.jruby.ext.ffi;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * Native types
 */
public enum NativeType implements NativeParam {
    VOID,
    BOOL,
    CHAR,
    UCHAR,
    SHORT,
    USHORT,
    INT,
    UINT,
    LONG_LONG,
    ULONG_LONG,
    /** A C long type */
    LONG,
    /** A C unsigned long */
    ULONG,
    FLOAT,
    DOUBLE,
    POINTER,
    BUFFER_IN,
    BUFFER_OUT,
    BUFFER_INOUT,
    CHAR_ARRAY,
    
    /** 
     * An immutable string.  Nul terminated, but only copies in to the native function 
     */
    STRING,
    /** A Rubinus :string arg - copies data both ways, and nul terminates */
    RBXSTRING,
    VARARGS,
    // ARRAY and STRUCT are only used internally
    ARRAY,
    STRUCT;
    
    public final int intValue() {
        return ordinal();
    }

    public final NativeType getNativeType() {
        return this;
    }

    public static final NativeType valueOf(int type) {
        NativeType[] values = NativeType.values();
        if (type < 0 || type >= values.length) {
            return NativeType.VOID;
        }
        return values[type];
    }
    
    public static final NativeType valueOf(IRubyObject type) {
        if (type instanceof Type.Builtin) {
            return ((Type.Builtin) type).getNativeType();
        } else if (type instanceof NativeParam) {
            return ((NativeParam) type).getNativeType();
        } else {
            return NativeType.VOID;
        }
    }
}
