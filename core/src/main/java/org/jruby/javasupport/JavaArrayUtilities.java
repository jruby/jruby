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
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
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
package org.jruby.javasupport;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.anno.JRubyModule;
import org.jruby.java.proxies.ArrayJavaProxy;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;

/**
 * @author Bill Dortch
 *
 */
@JRubyModule(name="JavaArrayUtilities")
public class JavaArrayUtilities {

    public static RubyModule createJavaArrayUtilitiesModule(Ruby runtime) {
        RubyModule javaArrayUtils = runtime.defineModule("JavaArrayUtilities");
        javaArrayUtils.defineAnnotatedMethods(JavaArrayUtilities.class);
        return javaArrayUtils;
    }

    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject bytes_to_ruby_string(ThreadContext context, IRubyObject recv, IRubyObject wrappedObject) {
        return bytes_to_ruby_string(context, recv, wrappedObject, context.nil);
    }

    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject bytes_to_ruby_string(ThreadContext context, IRubyObject recv, IRubyObject wrappedObject, IRubyObject encoding) {
        byte[] bytes = null;

        if (wrappedObject instanceof JavaProxy) {
            Object wrapped = ((JavaProxy) wrappedObject).getObject();
            if ( wrapped instanceof byte[] ) bytes = (byte[]) wrapped;
        } else {
            IRubyObject byteArray = (IRubyObject) wrappedObject.dataGetStruct();
            if (byteArray instanceof JavaArray) {
                final Object wrapped = ((JavaArray) byteArray).getValue();
                if ( wrapped instanceof byte[] ) bytes = (byte[]) wrapped;
            }
        }

        if (bytes == null) {
            throw context.runtime.newTypeError("wrong argument type " +
                wrappedObject.getMetaClass() + " (expected byte[])"
            );
        }

        RubyString string = context.runtime.newString(new ByteList(bytes, true));

        if ( ! encoding.isNil() ) string.force_encoding(context, encoding);

        return string;
    }

    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject ruby_string_to_bytes(IRubyObject recv, IRubyObject string) {
        Ruby runtime = recv.getRuntime();
        if (!(string instanceof RubyString)) {
            throw runtime.newTypeError(string, runtime.getString());
        }
        return JavaUtil.convertJavaToUsableRubyObject(runtime, ((RubyString)string).getBytes());
    }

    @JRubyMethod(module = true)
    public static IRubyObject java_to_ruby(ThreadContext context, IRubyObject recv, IRubyObject ary) {
        if (!(ary instanceof ArrayJavaProxy)) {
            throw context.runtime.newTypeError(ary, context.runtime.getJavaSupport().getArrayProxyClass());
        }
        return ((ArrayJavaProxy)ary).to_a(context);
    }

}
