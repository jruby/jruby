package org.jruby.ext.digest;

import org.jruby.util.ByteList;

/**
 * Ported from OpenSSH (https://github.com/openssh/openssh-portable/blob/957fbceb0f3166e41b76fdb54075ab3b9cc84cba/sshkey.c#L942-L987)
 *
 * OpenSSH License Notice
 *
 * Copyright (c) 2000, 2001 Markus Friedl.  All rights reserved.
 * Copyright (c) 2008 Alexander von Gernler.  All rights reserved.
 * Copyright (c) 2010,2011 Damien Miller.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

public class BubbleBabble {

    public static ByteList bubblebabble(byte[] message, int begin, int length) {
        char[] vowels = new char[]{'a', 'e', 'i', 'o', 'u', 'y'};
        char[] consonants = new char[]{'b', 'c', 'd', 'f', 'g', 'h', 'k', 'l', 'm',
                'n', 'p', 'r', 's', 't', 'v', 'z', 'x'};

        int seed = 1;

        ByteList retval = new ByteList();

        int rounds = (length / 2) + 1;
        retval.append('x');
        for (int i = 0; i < rounds; i++) {
            int idx0, idx1, idx2, idx3, idx4;

            if ((i + 1 < rounds) || (length % 2 != 0)) {
                idx0 = (((((int) (message[begin + 2 * i])) >> 6) & 3) + seed) % 6;
                idx1 =   (((int) (message[begin + 2 * i])) >> 2) & 15;
                idx2 =  ((((int) (message[begin + 2 * i]))  & 3) + (seed / 6)) % 6;
                retval.append(vowels[idx0]);
                retval.append(consonants[idx1]);
                retval.append(vowels[idx2]);
                if ((i + 1) < rounds) {
                    idx3 = (((int) (message[begin + (2 * i) + 1])) >> 4) & 15;
                    idx4 = (((int) (message[begin + (2 * i) + 1]))) & 15;
                    retval.append(consonants[idx3]);
                    retval.append('-');
                    retval.append(consonants[idx4]);
                    seed = ((seed * 5) +
                            ((((int) (message[begin + 2 * i])) * 7) +
                                    ((int) (message[begin + (2 * i) + 1])))) % 36;
                }
            }
        }
        retval.append('x');

        return retval;
    }
}
