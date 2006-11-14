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
 * Copyright (C) 2005 Thomas E Enebo <enebo@acm.org>
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
package org.jruby;

import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaObject;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Module which defines JRuby-specific methods for use. 
 */
public class RubyJRuby {
    public static RubyModule createJRuby(IRuby runtime) {
        runtime.getModule("Kernel").callMethod(runtime.getCurrentContext(),"require", runtime.newString("java"));
        RubyModule comparableModule = runtime.defineModule("JRuby");
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyJRuby.class);
        comparableModule.defineModuleFunction("parse", 
                callbackFactory.getSingletonMethod("parse", IRubyObject.class, IRubyObject.class));
        comparableModule.defineModuleFunction("runtime", 
                callbackFactory.getSingletonMethod("runtime"));

        return comparableModule;
    }
    
    public static IRubyObject runtime(IRubyObject recv) {
        return Java.java_to_ruby(recv, JavaObject.wrap(recv.getRuntime(), recv.getRuntime()));
    }
    
    public static IRubyObject parse(IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        RubyString content = arg1.convertToString();
        RubyString filename = arg2.convertToString();
        return Java.java_to_ruby(recv, JavaObject.wrap(recv.getRuntime(), 
            recv.getRuntime().parse(content.toString(), filename.toString(), null)));
    }
}
