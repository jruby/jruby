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
 * Copyright (C) 2008 JRuby project
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

package org.jruby.ext.ffi;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * Native types
 */
public enum NativeType {
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

    /* As above, but ok to use temporary memory for native copy of string */
    TRANSIENT_STRING,

    VARARGS,
    // ARRAY and STRUCT are only used internally
    ARRAY,
    STRUCT,

    /* map from one type to another */
    MAPPED;
    
    public final int intValue() {
        return ordinal();
    }
    
    public static final NativeType valueOf(IRubyObject type) {
        if  (type instanceof Type) {
            return ((Type) type).getNativeType();

        } else {
            return NativeType.VOID;
        }
    }
}
