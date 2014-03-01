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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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
package org.jruby.ext.openssl;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Utils {
    private Utils() {}
    public static String toHex(byte[] val) {
        final StringBuilder out = new StringBuilder();
        for ( int i=0,j=val.length; i<j; i++ ) {
            String ve = Integer.toString( ( ((int)((char)val[i])) & 0xFF ) , 16);
            if (ve.length() == 1) {
                out.append('0'); // "0#{ve}"
            }
            out.append(ve);
        }
        return out.toString();
    }

    public static String toHex(byte[] val, char sep) {
        final StringBuilder out = new StringBuilder();
        final String sepStr = Character.toString(sep);
        String separator = "";
        for ( int i=0,j=val.length; i<j; i++ ) {
            out.append(separator);
            String ve = Integer.toString( ( ((int)((char)val[i])) & 0xFF ) , 16);
            if (ve.length() == 1) {
                out.append('0'); // "0#{ve}"
            }
            out.append(ve);
            separator = sepStr;
        }
        return out.toString().toUpperCase();
    }

    public static void checkKind(Ruby rt, IRubyObject obj, String path) {
        if (((RubyObject) obj).kind_of_p(rt.getCurrentContext(), rt.getClassFromPath(path)).isFalse()) {
            throw rt.newTypeError(String.format("wrong argument (%s)! (Expected kind of %s)", obj.getMetaClass().getName(), path));
        }
    }

    public static RubyClass getClassFromPath(Ruby rt, String path) {
        return (RubyClass) rt.getClassFromPath(path);
    }

    public static RaiseException newError(Ruby rt, String path, String message) {
        return new RaiseException(rt, getClassFromPath(rt, path), message, true);
    }

    public static RaiseException newError(Ruby rt, String path, String message, boolean nativeException) {
        return new RaiseException(rt, getClassFromPath(rt, path), message, nativeException);
    }

    public static IRubyObject newRubyInstance(Ruby rt, String path) {
        return rt.getClassFromPath(path).callMethod(rt.getCurrentContext(), "new");
    }

    public static IRubyObject newRubyInstance(Ruby rt, String path, IRubyObject arg) {
        return rt.getClassFromPath(path).callMethod(rt.getCurrentContext(), "new", arg);
    }

    public static IRubyObject newRubyInstance(Ruby rt, String path, IRubyObject[] args) {
        return rt.getClassFromPath(path).callMethod(rt.getCurrentContext(), "new", args);
    }

}// Utils
