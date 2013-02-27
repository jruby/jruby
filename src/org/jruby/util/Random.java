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
package org.jruby.util;

import java.math.BigInteger;
import java.util.Arrays;

public class Random {
    public static int N = 624;
    private static int M = 397;
    private static int MATRIX_A = 0x9908b0df; /* constant vector a */
    private static int UMASK = 0x80000000; /* most significant w-r bits */
    private static int LMASK = 0x7fffffff; /* least significant r bits */

    private static int MIXBITS(int u, int v) {
        return (u & UMASK) | (v & LMASK);
    }

    private static int TWIST(int u, int v) {
        return (MIXBITS(u, v) >>> 1) ^ (((v & 1) != 0) ? MATRIX_A : 0);
    }

    private final int[] state = new int[N];
    private int left = 1;

    public Random(int s) {
        initGenrand(s);
    }

    public Random(int[] initKey) {
        initByArray(initKey);
    }

    public Random(Random orig) {
        System.arraycopy(orig.state, 0, this.state, 0, this.state.length);
        this.left = orig.left;
    }

    public Random(int[] state, int left) {
        if (state.length != this.state.length) {
            throw new IllegalStateException("wrong state length: " + state.length);
        }
        System.arraycopy(state, 0, this.state, 0, this.state.length);
        this.left = left;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof Random)) {
            return false;
        }
        Random rhs = (Random) obj;
        return (left == rhs.left) && Arrays.equals(state, rhs.state);
    }

    @Override
    public int hashCode() {
        // Using 17 as the initializer, 37 as the multiplier.
        return (629 + left) * 37 + state.hashCode();
    }
    
    private void initGenrand(int s) {
        state[0] = s;
        for (int j = 1; j < N; j++) {
            state[j] = (1812433253 * (state[j - 1] ^ (state[j - 1] >>> 30)) + j);
        }
        left = 1;
    }

    private void initByArray(int[] initKey) {
        initGenrand(19650218);
        int len = initKey.length;
        int i = 1;
        int j = 0;
        int k = N > len ? N : len;
        for (; k > 0; k--) {
            state[i] = (state[i] ^ ((state[i - 1] ^ (state[i - 1] >>> 30)) * 1664525)) + initKey[j]
                    + j;
            i++;
            j++;
            if (i >= N) {
                state[0] = state[N - 1];
                i = 1;
            }
            if (j >= len) {
                j = 0;
            }
        }
        for (k = N - 1; k > 0; k--) {
            state[i] = (state[i] ^ ((state[i - 1] ^ (state[i - 1] >>> 30)) * 1566083941)) - i;
            i++;
            if (i >= N) {
                state[0] = state[N - 1];
                i = 1;
            }
        }
        state[0] = 0x80000000;
    }

    private void nextState() {
        int p = 0;

        left = N;

        for (int j = N - M + 1; --j > 0; p++) {
            state[p] = state[p + M] ^ TWIST(state[p + 0], state[p + 1]);
        }

        for (int j = M; --j > 0; p++) {
            state[p] = state[p + M - N] ^ TWIST(state[p + 0], state[p + 1]);
        }

        state[p] = state[p + M - N] ^ TWIST(state[p + 0], state[0]);
    }

    public int genrandInt32() {
        int y;

        synchronized (this) {
            if (--left <= 0)
                nextState();

            y = state[N - left];
        }

        /* Tempering */
        y ^= (y >>> 11);
        y ^= (y << 7) & 0x9d2c5680L;
        y ^= (y << 15) & 0xefc60000L;
        y ^= (y >>> 18);

        return y;
    }

    public double genrandReal() {
        int a = genrandInt32() >>> 5;
        int b = genrandInt32() >>> 6;
        return (a * 67108864.0 + b) * (1.0 / 9007199254740992.0);
    }

    public double genrandReal2() {
        int a = genrandInt32();
        int b = genrandInt32();
        return intPairToRealInclusive(a, b);
    }

    private static final BigInteger INTPAIR_CONST = BigInteger.valueOf((1L << 53) + 1);
    private static final double LDEXP_CONST = Math.pow(2.0, -53);

    // c: ldexp((a<< 32)|b) * ((1<<53)+1) >> 64, -53)
    // TODO: not enough prec...
    private double intPairToRealInclusive(int a, int b) {
        BigInteger c = BigInteger.valueOf(a & 0xffffffffL);
        BigInteger d = BigInteger.valueOf(b & 0xffffffffL);
        return c.shiftLeft(32).or(d).multiply(INTPAIR_CONST).shiftRight(64).doubleValue()
                * LDEXP_CONST;
    }

    public int[] getState() {
        return state;
    }

    public int getLeft() {
        return left;
    }
}
