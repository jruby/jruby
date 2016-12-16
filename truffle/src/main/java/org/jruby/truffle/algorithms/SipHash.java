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
 * Written by nahi@ruby-lang.org
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
package org.jruby.truffle.algorithms;

import java.nio.ByteBuffer;

public class SipHash {

    public static long hash24(long k0, long k1, byte[] data) {
        return hash24(k0, k1, data, 0, data.length);
    }
    
    private static long hash24(long k0, long k1, byte[] src, int offset, int length) {
        long v0 = 0x736f6d6570736575L ^ k0;
        long v1 = 0x646f72616e646f6dL ^ k1;
        long v2 = 0x6c7967656e657261L ^ k0;
        long v3 = 0x7465646279746573L ^ k1;
        long m;
        int last = offset + length / 8 * 8;
        int i = offset;

        if (offset < 0) {
            throw new ArrayIndexOutOfBoundsException(offset);
        } else if (offset + length > src.length) {
            throw new ArrayIndexOutOfBoundsException(src.length);
        }

        final ByteBuffer buffer = ByteBuffer.wrap(src, offset, length);

        // processing 8 bytes blocks in data
        while (i < last) {
            m = buffer.getLong(i);
            i += 8;
            // MSGROUND {
                v3 ^= m;

                /* SIPROUND wih hand reordering
                 *
                 * SIPROUND in siphash24.c:
                 *   A: v0 += v1;
                 *   B: v1=ROTL(v1,13);
                 *   C: v1 ^= v0;
                 *   D: v0=ROTL(v0,32);
                 *   E: v2 += v3;
                 *   F: v3=ROTL(v3,16);
                 *   G: v3 ^= v2;
                 *   H: v0 += v3;
                 *   I: v3=ROTL(v3,21);
                 *   J: v3 ^= v0;
                 *   K: v2 += v1;
                 *   L: v1=ROTL(v1,17);
                 *   M: v1 ^= v2;
                 *   N: v2=ROTL(v2,32);
                 *
                 * Each dependency:
                 *   B -> A
                 *   C -> A, B
                 *   D -> C
                 *   F -> E
                 *   G -> E, F
                 *   H -> D, G
                 *   I -> H
                 *   J -> H, I
                 *   K -> C, G
                 *   L -> K
                 *   M -> K, L
                 *   N -> M
                 *
                 * Dependency graph:
                 *   D -> C -> B -> A
                 *        G -> F -> E
                 *   J -> I -> H -> D, G
                 *   N -> M -> L -> K -> C, G
                 *
                 * Resulting parallel friendly execution order:
                 *   -> ABCDHIJ
                 *   -> EFGKLMN
                 */

                // SIPROUND {
                    v0 += v1;                    v2 += v3;
                    v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
                    v1 ^= v0;                    v3 ^= v2;
                    v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
                    v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
                    v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
                    v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
                // }
                // SIPROUND {
                    v0 += v1;                    v2 += v3;
                    v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
                    v1 ^= v0;                    v3 ^= v2;
                    v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
                    v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
                    v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
                    v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
                // }
                v0 ^= m;
            // }
        }

        // packing the last block to long, as LE 0-7 bytes + the length in the top byte
        m = 0;
        for (i = offset + length - 1; i >= last; --i) {
            m <<= 8; m |= src[i];
        }
        m |= (long) length << 56;
        // MSGROUND {
            v3 ^= m;
            for (int j = 0; j < 2; j++) {
            // SIPROUND {
                v0 += v1;                    v2 += v3;
                v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
                v1 ^= v0;                    v3 ^= v2;
                v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
                v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
                v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
                v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
            // }
            }
            v0 ^= m;
        // }

        // finishing...
        v2 ^= 0xff;
        for (int j = 0; j < 4; j++) {
        // SIPROUND {
            v0 += v1;                    v2 += v3;
            v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
            v1 ^= v0;                    v3 ^= v2;
            v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
            v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
            v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
            v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
        // }
        }
        return v0 ^ v1 ^ v2 ^ v3;
    }

}
