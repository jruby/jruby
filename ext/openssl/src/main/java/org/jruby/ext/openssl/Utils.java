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
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Utils {
    private Utils() {}

    @Deprecated
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

    @Deprecated
    public static String toHex(byte[] val, char sep) {
        final StringBuilder out = new StringBuilder();
        final String sepStr = Character.toString(sep);
        String separator = "";
        for ( int i=0,j=val.length; i<j; i++ ) {
            out.append(separator);
            String ve = Integer.toString( ( ((int)((char) val[i])) & 0xFF ) , 16);
            if (ve.length() == 1) {
                out.append('0'); // "0#{ve}"
            }
            out.append(ve);
            separator = sepStr;
        }
        return out.toString().toUpperCase();
    }

    static ByteList hexBytes(final byte[] data) {
        return hexBytes(data, 0);
    }

    static ByteList hexBytes(final byte[] data, final int off) {
        final int len = data.length - off;
        return hexBytes(data, off, len, new ByteList( len * 3 ));
    }

    static ByteList hexBytes(final byte[] data, final ByteList out) {
        return hexBytes(data, 0, data.length, out);
    }

    @SuppressWarnings("deprecation")
    static ByteList hexBytes(final ByteList data, final ByteList out) {
        return hexBytes(data.bytes, data.begin, data.realSize, out);
    }

    private static final char[] digits = {
        '0' , '1' , '2' , '3' , '4' , '5' , '6' , '7' ,
        '8' , '9' , 'A' , 'B' , 'C' , 'D' , 'E' , 'F'
    };

    private static ByteList hexBytes(final byte[] data, final int off, final int len, final ByteList out) {
        boolean notFist = false;
        out.ensure( len * 3 - 1 );
        for ( int i = off; i < (off + len); i++ ) {
            if ( notFist ) out.append(':');
            final byte b = data[i];
            out.append( digits[ (b >> 4) & 0xF ] );
            out.append( digits[ b & 0xF ] );
            notFist = true;
        }
        return out;
    }

    @Deprecated
    public static void checkKind(Ruby rt, IRubyObject obj, String path) {
        if (((RubyObject) obj).kind_of_p(rt.getCurrentContext(), rt.getClassFromPath(path)).isFalse()) {
            throw rt.newTypeError(String.format("wrong argument (%s)! (Expected kind of %s)", obj.getMetaClass().getName(), path));
        }
    }

    @Deprecated
    public static RubyClass getClassFromPath(Ruby rt, String path) {
        return (RubyClass) rt.getClassFromPath(path);
    }

    @Deprecated
    public static RaiseException newError(Ruby rt, String path, String message) {
        return new RaiseException(rt, getClassFromPath(rt, path), message, true);
    }

    @Deprecated
    public static RaiseException newError(Ruby rt, String path, String message, boolean nativeException) {
        return new RaiseException(rt, getClassFromPath(rt, path), message, nativeException);
    }

    static RaiseException newRuntimeError(Ruby runtime, Exception e) {
        return new RaiseException(runtime, runtime.getRuntimeError(), e.getMessage(), true);
    }

    static RaiseException newError(Ruby runtime, RubyClass errorClass, String message, boolean nativeException) {
        return new RaiseException(runtime, errorClass, message, nativeException);
    }

    static RaiseException newError(Ruby runtime, RubyClass errorClass, Exception e) {
        return new RaiseException(runtime, errorClass, e.getMessage(), true);
    }

    static RaiseException newError(Ruby runtime, RubyClass errorClass, String msg) {
        return new RaiseException(runtime, errorClass, msg, true);
    }

    @Deprecated
    public static IRubyObject newRubyInstance(Ruby rt, String path) {
        return rt.getClassFromPath(path).callMethod(rt.getCurrentContext(), "new");
    }

    @Deprecated
    public static IRubyObject newRubyInstance(Ruby rt, String path, IRubyObject arg) {
        return rt.getClassFromPath(path).callMethod(rt.getCurrentContext(), "new", arg);
    }

    @Deprecated
    public static IRubyObject newRubyInstance(Ruby rt, String path, IRubyObject... args) {
        return rt.getClassFromPath(path).callMethod(rt.getCurrentContext(), "new", args);
    }

    static IRubyObject newRubyInstance(final ThreadContext context, final String path) {
        final RubyModule klass = context.runtime.getClassFromPath(path);
        return klass.callMethod(context, "new");
    }

    static IRubyObject newRubyInstance(final ThreadContext context, final String path, IRubyObject arg) {
        final RubyModule klass = context.runtime.getClassFromPath(path);
        return klass.callMethod(context, "new", arg);
    }

    static IRubyObject newRubyInstance(final ThreadContext context, final String path, IRubyObject... args) {
        final RubyModule klass = context.runtime.getClassFromPath(path);
        return klass.callMethod(context, "new", args);
    }

    // reinvented parts of org.jruby.runtime.Helpers for compatibility with "older" JRuby :

    static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, Block block) {
        return self.getMetaClass().finvoke(context, self, name, block);
    }

    static IRubyObject invokeSuper(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        return invokeSuper(context, self, context.getFrameKlazz(), context.getFrameName(), args, block);
    }

    static IRubyObject invokeSuper(ThreadContext context, IRubyObject self, RubyModule klass, String name, IRubyObject[] args, Block block) {
        checkSuperDisabledOrOutOfMethod(context, klass, name);

        RubyClass superClass = findImplementerIfNecessary(self.getMetaClass(), klass).getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(name) : UndefinedMethod.INSTANCE;
        // NOTE: method_missing not implemented !
        //if (method.isUndefined()) {
        //    return callMethodMissing(context, self, method.getVisibility(), name, CallType.SUPER, args, block);
        //}
        return method.call(context, self, superClass, name, args, block);
    }

    private static void checkSuperDisabledOrOutOfMethod(ThreadContext context, RubyModule klass, String name) {
        if (klass == null) {
            if (name != null) {
                throw context.runtime.newNameError("superclass method '" + name + "' disabled", name);
            } else {
                throw context.runtime.newNoMethodError("super called outside of method", null, context.nil);
            }
        }
    }

    private static RubyModule findImplementerIfNecessary(RubyModule clazz, RubyModule implementationClass) {
        if (implementationClass != null && implementationClass.needsImplementer()) {
            // modules are included with a shim class; we must find that shim to handle super() appropriately
            return clazz.findImplementer(implementationClass);
        } else {
            // classes are directly in the hierarchy, so no special logic is necessary for implementer
            return implementationClass;
        }
    }

}// Utils
