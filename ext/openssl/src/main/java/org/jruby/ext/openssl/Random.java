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

import java.security.SecureRandom;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Random {
    private final static class RandomHolder {
        public java.util.Random[] randomizers;
    }
    public static void createRandom(Ruby runtime, RubyModule ossl) {
        RubyModule rand = ossl.defineModuleUnder("Random");

        RubyClass osslError = (RubyClass)ossl.getConstant("OpenSSLError");
        rand.defineClassUnder("RandomError",osslError,osslError.getAllocator());

        rand.defineAnnotatedMethods(Random.class);

        RandomHolder holder = new RandomHolder();
        holder.randomizers = new java.util.Random[]{new java.util.Random(), new SecureRandom()};
        rand.dataWrapStruct(holder);
    }

    @JRubyMethod(meta=true)
    public static IRubyObject seed(IRubyObject recv, IRubyObject arg) {
        return recv.getRuntime().getNil();
    }
    @JRubyMethod(meta=true)
    public static IRubyObject load_random_file(IRubyObject recv, IRubyObject arg) {
        return recv.getRuntime().getNil();
    }
    @JRubyMethod(meta=true)
    public static IRubyObject write_random_file(IRubyObject recv, IRubyObject arg) {
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(meta=true)
    public static IRubyObject random_bytes(IRubyObject recv, IRubyObject arg) {
        return generate(recv, arg, 1);
    }

    @JRubyMethod(meta=true)
    public static IRubyObject pseudo_bytes(IRubyObject recv, IRubyObject arg) {
        return generate(recv, arg, 0);
    }

    private static RubyString generate(IRubyObject recv, IRubyObject arg, int ix) {
        RandomHolder holder = (RandomHolder)recv.dataGetStruct();
        int len = RubyNumeric.fix2int(arg);
        if (len < 0 || len > Integer.MAX_VALUE) {
            throw recv.getRuntime().newArgumentError("negative string size (or size too big)");
        }
        byte[] buf = new byte[len];
        holder.randomizers[ix].nextBytes(buf);
        return RubyString.newString(recv.getRuntime(), new ByteList(buf,false));
    }

    @JRubyMethod(meta=true)
    public static IRubyObject egd(IRubyObject recv, IRubyObject arg) {
        return recv.getRuntime().getNil();
    }
    @JRubyMethod(meta=true)
    public static IRubyObject egd_bytes(IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        return recv.getRuntime().getNil();
    }
}
