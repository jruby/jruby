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
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Random {

    static class RandomHolder {

        final java.util.Random plainRandom;
        final java.security.SecureRandom secureRandom;

        RandomHolder(java.util.Random plainRandom, java.security.SecureRandom secureRandom) {
            this.plainRandom = plainRandom; this.secureRandom = secureRandom;
            //this.randomizers = new java.util.Random[] { plainRandom, secureRandom };
        }

        //RandomHolder(java.util.Random... randomizers) {
        //    this.randomizers = randomizers;
        //}

        //public final java.util.Random[] randomizers;

        //java.util.Random get(final int index) {
        //    return randomizers[ index % randomizers.length ];
        //}

    }

    public static void createRandom(final Ruby runtime, final RubyModule ossl) {
        final RubyModule random = ossl.defineModuleUnder("Random");

        RubyClass osslError = (RubyClass) ossl.getConstant("OpenSSLError");
        random.defineClassUnder("RandomError", osslError, osslError.getAllocator());

        random.defineAnnotatedMethods(Random.class);

        random.dataWrapStruct(
            new RandomHolder(new java.util.Random(), SecurityHelper.getSecureRandom())
        );
    }

    @JRubyMethod(meta = true)
    public static IRubyObject seed(final ThreadContext context,
        final IRubyObject self, IRubyObject arg) {
        return context.runtime.getNil(); // TODO this could be implemented !
    }
    @JRubyMethod(meta = true)
    public static IRubyObject load_random_file(final ThreadContext context,
        final IRubyObject self, IRubyObject arg) {
        return context.runtime.getNil();
    }
    @JRubyMethod(meta = true)
    public static IRubyObject write_random_file(final ThreadContext context,
        final IRubyObject self, IRubyObject arg) {
        return context.runtime.getNil();
    }

    @JRubyMethod(meta = true)
    public static IRubyObject random_bytes(final ThreadContext context,
        final IRubyObject self, IRubyObject arg) {
        return generate(context.runtime, self, arg, true); // secure-random
    }

    @JRubyMethod(meta = true)
    public static IRubyObject pseudo_bytes(final ThreadContext context,
        final IRubyObject self, IRubyObject arg) {
        return generate(context.runtime, self, arg, false); // plain-random
    }

    private static RubyString generate(final Ruby runtime,
        final IRubyObject self, IRubyObject arg, final boolean secure) {
        final int len = RubyNumeric.fix2int(arg);
        if ( len < 0 || len > Integer.MAX_VALUE ) {
            throw runtime.newArgumentError("negative string size (or size too big) " + len);
        }
        final RandomHolder holder = (RandomHolder) self.dataGetStruct();
        final byte[] bytes = new byte[len];
        ( secure ? holder.secureRandom : holder.plainRandom ).nextBytes(bytes);
        return RubyString.newString(runtime, new ByteList(bytes, false));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject egd(final ThreadContext context,
        final IRubyObject self, IRubyObject arg) {
        return context.runtime.getNil();
    }

    @JRubyMethod(meta = true)
    public static IRubyObject egd_bytes(final ThreadContext context,
        final IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
        return context.runtime.getNil();
    }

}
