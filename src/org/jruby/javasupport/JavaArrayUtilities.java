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
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
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
package org.jruby.javasupport;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.anno.JRubyModule;
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
    public static IRubyObject bytes_to_ruby_string(IRubyObject recv, IRubyObject wrappedObject) {
        Ruby runtime = recv.getRuntime();
        IRubyObject byteArray = (JavaObject)wrappedObject.dataGetStruct();
        if (!(byteArray instanceof JavaArray &&
                ((JavaArray)byteArray).getValue() instanceof byte[])) {
            throw runtime.newTypeError("wrong argument type " + wrappedObject.getMetaClass() +
                    " (expected byte[])");
        }
        return runtime.newString(new ByteList((byte[])((JavaArray)byteArray).getValue(), true));
    }
    
    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject ruby_string_to_bytes(IRubyObject recv, IRubyObject string) {
        Ruby runtime = recv.getRuntime();
        if (!(string instanceof RubyString)) {
            throw runtime.newTypeError(string, runtime.getString());
        }
        return JavaUtil.convertJavaToUsableRubyObject(runtime, ((RubyString)string).getBytes());
    }

}
