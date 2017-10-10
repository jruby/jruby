/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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

import org.jruby.runtime.builtin.IRubyObject;

/**
 * Support methods for (native) arrays.
 *
 * These utility methods are intended as `System.arraycopy` replacements esp. for common cases
 * such as (method) argument processing, where the arrays are usually small. These are fairly
 * common and for such arrays a length switch + an "inlined" array copy/instantiation cuts the
 * execution time to (at least) half, compared to doing a bare native `arraycopy` (on 8u102).
 *
 * @author kares
 */
public abstract class ArraySupport {

    private ArraySupport() {}

    /**
     * Copy a source array into a destination array.
     * @param src
     * @param dst
     * @param dstOff off set to start copying to
     * @param length copied array length
     */
    public static void copy(Object[] src, Object[] dst, int dstOff, final int length) {
        switch (length) {
            case 0: return;
            case 1:
                dst[dstOff] = src[0]; return;
            // NOTE: these won't handle src == dst in all cases, for now this is intentional - do not call with src == dst!
            case 2: assert src != dst;
                dst[dstOff] = src[0]; dst[++dstOff] = src[1]; return;
            case 3: assert src != dst;
                dst[dstOff] = src[0]; dst[++dstOff] = src[1]; dst[++dstOff] = src[2]; return;
        }
        System.arraycopy(src, 0, dst, dstOff, length);
    }

    public static void copy(Object[] src, int srcOff, Object[] dst, int dstOff, final int length) {
        switch (length) {
            case 0: return;
            case 1:
                dst[dstOff] = src[srcOff]; return;
            // NOTE: these won't handle src == dst in all cases, for now this is intentional - do not call with src == dst!
            case 2: assert src != dst;
                dst[dstOff] = src[srcOff]; dst[++dstOff] = src[srcOff + 1]; return;
            case 3: assert src != dst;
                dst[dstOff] = src[srcOff]; dst[++dstOff] = src[srcOff + 1]; dst[++dstOff] = src[srcOff + 2]; return;
        }
        System.arraycopy(src, srcOff, dst, dstOff, length);
    }

    public static Object[] newCopy(Object[] src, int length) {
        final Object[] copy = new Object[length];
        if (length > src.length) length = src.length;
        copy(src, copy, 0, length);
        return copy;
    }

    public static IRubyObject[] newCopy(IRubyObject[] src, int length) {
        final IRubyObject[] copy = new IRubyObject[length];
        if (length > src.length) length = src.length;
        copy(src, copy, 0, length);
        return copy;
    }

    public static IRubyObject[] newCopy(IRubyObject[] src, final int srcOff, int length) {
        final IRubyObject[] copy = new IRubyObject[length];
        if (length > src.length) length = src.length;
        copy(src, srcOff, copy, 0, length);
        return copy;
    }

    public static Class[] newCopy(Class[] src, int length) {
        final Class[] copy = new Class[length];
        if (length > src.length) length = src.length;
        copy(src, copy, 0, length);
        return copy;
    }

    public static Object[] newCopy(Object[] src, Object last) {
        switch (src.length) {
            case 0: return new Object[] { last };
            case 1: return new Object[] { src[0], last };
            case 2: return new Object[] { src[0], src[1], last };
            case 3: return new Object[] { src[0], src[1], src[2], last };
        }
        final Object[] copy = new Object[src.length + 1];
        System.arraycopy(src, 0, copy, 0, src.length);
        copy[src.length] = last;
        return copy;
    }

    public static IRubyObject[] newCopy(IRubyObject[] src, IRubyObject last) {
        switch (src.length) {
            case 0: return new IRubyObject[] { last };
            case 1: return new IRubyObject[] { src[0], last };
            case 2: return new IRubyObject[] { src[0], src[1], last };
            case 3: return new IRubyObject[] { src[0], src[1], src[2], last };
            case 4: return new IRubyObject[] { src[0], src[1], src[2], src[3], last };
        }
        final IRubyObject[] copy = new IRubyObject[src.length + 1];
        System.arraycopy(src, 0, copy, 0, src.length);
        copy[src.length] = last;
        return copy;
    }

    public static IRubyObject[] newCopy(IRubyObject first, IRubyObject[] src) {
        switch (src.length) {
            case 0: return new IRubyObject[] { first };
            case 1: return new IRubyObject[] { first, src[0] };
            case 2: return new IRubyObject[] { first, src[0], src[1] };
            case 3: return new IRubyObject[] { first, src[0], src[1], src[2] };
            case 4: return new IRubyObject[] { first, src[0], src[1], src[2], src[3] };
        }
        final IRubyObject[] copy = new IRubyObject[1 + src.length];
        copy[0] = first;
        System.arraycopy(src, 0, copy, 1, src.length);
        return copy;
    }

}
