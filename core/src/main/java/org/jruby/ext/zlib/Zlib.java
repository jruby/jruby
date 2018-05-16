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

package org.jruby.ext.zlib;

public final class Zlib {
    private Zlib() {}

    // TODO: Those are defined at com.jcraft.jzlib.Deflate but private
    public static final byte Z_BINARY = (byte) 0;
    public static final byte Z_ASCII = (byte) 1;
    public static final byte Z_UNKNOWN = (byte) 2;
    // os_code
    public static final byte OS_MSDOS = (byte) 0x00;
    public static final byte OS_AMIGA = (byte) 0x01;
    public static final byte OS_VMS = (byte) 0x02;
    public static final byte OS_UNIX = (byte) 0x03;
    public static final byte OS_ATARI = (byte) 0x05;
    public static final byte OS_OS2 = (byte) 0x06;
    public static final byte OS_MACOS = (byte) 0x07;
    public static final byte OS_TOPS20 = (byte) 0x0a;
    public static final byte OS_WIN32 = (byte) 0x0b;
    public static final byte OS_VMCMS = (byte) 0x04;
    public static final byte OS_ZSYSTEM = (byte) 0x08;
    public static final byte OS_CPM = (byte) 0x09;
    public static final byte OS_QDOS = (byte) 0x0c;
    public static final byte OS_RISCOS = (byte) 0x0d;
    public static final byte OS_UNKNOWN = (byte) 0xff;
    public static final byte OS_CODE = OS_WIN32; // TODO: why we define OS_CODE to OS_WIN32?
}