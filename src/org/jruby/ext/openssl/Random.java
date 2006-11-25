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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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
package org.jruby.ext.openssl;

import org.jruby.IRuby;
import org.jruby.RubyModule;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Random {
    public static void createRandom(IRuby runtime, RubyModule ossl) {
        RubyModule rand = ossl.defineModuleUnder("Random");

        CallbackFactory randcb = runtime.callbackFactory(Random.class);
        rand.defineSingletonMethod("seed",randcb.getOptSingletonMethod("seed"));
        rand.defineSingletonMethod("load_random_file",randcb.getOptSingletonMethod("load_random_file"));
        rand.defineSingletonMethod("write_random_file",randcb.getOptSingletonMethod("write_random_file"));
        rand.defineSingletonMethod("random_bytes",randcb.getOptSingletonMethod("random_bytes"));
        rand.defineSingletonMethod("pseudo_bytes",randcb.getOptSingletonMethod("pseudo_bytes"));
        rand.defineSingletonMethod("egd",randcb.getOptSingletonMethod("egd"));
        rand.defineSingletonMethod("egd_bytes",randcb.getOptSingletonMethod("egd_bytes"));
    }

    public static IRubyObject seed(IRubyObject recv, IRubyObject[] args) {
        return recv.getRuntime().getNil();
    }
    public static IRubyObject load_random_file(IRubyObject recv, IRubyObject[] args) {
        return recv.getRuntime().getNil();
    }
    public static IRubyObject write_random_file(IRubyObject recv, IRubyObject[] args) {
        return recv.getRuntime().getNil();
    }
    public static IRubyObject random_bytes(IRubyObject recv, IRubyObject[] args) {
        return recv.getRuntime().getNil();
    }
    public static IRubyObject pseudo_bytes(IRubyObject recv, IRubyObject[] args) {
        return recv.getRuntime().getNil();
    }
    public static IRubyObject egd(IRubyObject recv, IRubyObject[] args) {
        return recv.getRuntime().getNil();
    }
    public static IRubyObject egd_bytes(IRubyObject recv, IRubyObject[] args) {
        return recv.getRuntime().getNil();
    }
}
