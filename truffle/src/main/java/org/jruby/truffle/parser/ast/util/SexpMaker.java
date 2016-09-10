/*
 ***** BEGIN LICENSE BLOCK *****
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
package org.jruby.truffle.parser.ast.util;

import org.jruby.ir.IRScope;
import org.jruby.util.ConvertBytes;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicLong;

public class SexpMaker {
    private static final AtomicLong JITTED_METHOD_NUMBER = new AtomicLong();
    private interface Builder {
        Builder append(String str);
        Builder append(char ch);
        Builder append(int i);
        Builder append(Object o);
        Builder append(boolean b);
        Builder append(long l);
        Builder append(double d);
    }

    private static class DigestBuilder implements Builder {
        MessageDigest d;

        DigestBuilder(MessageDigest digest) {
            this.d = digest;
        }

        @Override
        public Builder append(Object o) {
            append(o.toString());
            return this;
        }

        @Override
        public Builder append(String str) {
            d.update(str.getBytes());
            return this;
        }

        @Override
        public Builder append(boolean b) {
            append((byte) (b ? 1 : 0));
            return this;
        }

        @Override
        public Builder append(char ch) {
            d.update((byte)(ch >> 8));
            d.update((byte)(ch));
            return this;
        }

        @Override
        public Builder append(int i) {
            append((char) (i >> 16));
            append((char) i);
            return this;
        }

        @Override
        public Builder append(long l) {
            append((int) (l >> 32));
            append((int) l);
            return this;
        }

        @Override
        public Builder append(double d) {
            append(Double.doubleToLongBits(d));
            return this;
        }
    }

    public static String sha1(IRScope scope) {
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException(nsae);
        }

        DigestBuilder db = new DigestBuilder(sha1);

        db.append(scope.getName());
        db.append('\n');
        // CON FIXME: We need a better way to uniquely identify this...right?
        db.append(JITTED_METHOD_NUMBER.getAndIncrement());

        byte[] digest = db.d.digest();

        return new String(ConvertBytes.twosComplementToHexBytes(digest, false));
    }
}
