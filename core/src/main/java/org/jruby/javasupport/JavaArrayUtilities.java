/***** BEGIN LICENSE BLOCK *****
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

import org.jruby.RubyBasicObject;
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

import static org.jruby.api.Convert.castAsString;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Define.defineModule;
import static org.jruby.api.Error.typeError;

@JRubyModule(name="JavaArrayUtilities")
public class JavaArrayUtilities {

    public static RubyModule createJavaArrayUtilitiesModule(ThreadContext context) {
        return defineModule(context, "JavaArrayUtilities").defineMethods(context, JavaArrayUtilities.class);
    }

    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject bytes_to_ruby_string(ThreadContext context, IRubyObject recv, IRubyObject wrappedObject) {
        return bytes_to_ruby_string(context, recv, wrappedObject, context.nil);
    }

    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject bytes_to_ruby_string(ThreadContext context, IRubyObject recv, IRubyObject wrappedObject, IRubyObject encoding) {
        byte[] bytes = null;

        if (wrappedObject instanceof JavaProxy proxy) {
            Object wrapped = proxy.getObject();
            if (wrapped instanceof byte[]) bytes = (byte[]) wrapped;
        }

        if (bytes == null) throw typeError(context, wrappedObject, "byte[]");

        RubyString string = newString(context, new ByteList(bytes, true));

        if (!encoding.isNil()) string.force_encoding(context, encoding);

        return string;
    }

    @Deprecated(since = "10.0")
    public static IRubyObject ruby_string_to_bytes(IRubyObject recv, IRubyObject string) {
        return ruby_string_to_bytes(((RubyBasicObject) recv).getCurrentContext(), recv, string);
    }

    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject ruby_string_to_bytes(ThreadContext context, IRubyObject recv, IRubyObject string) {
        return JavaUtil.convertJavaToUsableRubyObject(context.runtime, castAsString(context, string).getBytes());
    }

    @JRubyMethod(module = true)
    public static IRubyObject java_to_ruby(ThreadContext context, IRubyObject recv, IRubyObject ary) {
        if (!(ary instanceof ArrayJavaProxy)) throw typeError(context, ary, context.runtime.getJavaSupport().getArrayProxyClass());

        return ((ArrayJavaProxy)ary).to_a(context);
    }

}
