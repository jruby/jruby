/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2010 Charles Oliver Nutter <headius@headius.com>
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

package org.jruby.ext.digest;

import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.load.Library;
import org.jruby.util.ByteList;

import java.io.IOException;

public class BubbleBabble implements Library {

    public void load(final Ruby runtime, boolean wrap) throws IOException {
        ThreadContext context = runtime.getCurrentContext();
        RubyDigest.createDigestBubbleBabble(context);
    }

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
    public static ByteList bubblebabble(byte[] message, int begin, int length) {
        char[] vowels = new char[]{'a', 'e', 'i', 'o', 'u', 'y'};
        char[] consonants = new char[]{'b', 'c', 'd', 'f', 'g', 'h', 'k', 'l', 'm',
                'n', 'p', 'r', 's', 't', 'v', 'z', 'x'};

        long seed = 1;

        ByteList retval = new ByteList();

        int rounds = (length / 2) + 1;
        retval.append('x');
        for (int i = 0; i < rounds; i++) {
            int idx0, idx1, idx2, idx3, idx4;

            if ((i + 1 < rounds) || (length % 2 != 0)) {
                long b = message[begin + 2 * i] & 0xFF;
                idx0 = (int) ((((b >> 6) & 3) + seed) % 6) & 0xFFFFFFFF;
                idx1 = (int) (((b) >> 2) & 15) & 0xFFFFFFFF;
                idx2 = (int) (((b & 3) + (seed / 6)) % 6) & 0xFFFFFFFF;
                retval.append(vowels[idx0]);
                retval.append(consonants[idx1]);
                retval.append(vowels[idx2]);
                if ((i + 1) < rounds) {
                    long b2 = message[begin + (2 * i) + 1] & 0xFF;
                    idx3 = (int) ((b2 >> 4) & 15) & 0xFFFFFFFF;
                    idx4 = (int) ((b2) & 15) & 0xFFFFFFFF;
                    retval.append(consonants[idx3]);
                    retval.append('-');
                    retval.append(consonants[idx4]);
                    seed = ((seed * 5) +
                            ((b * 7) +
                                    b2)) % 36;
                }
            } else {
                idx0 = (int) (seed % 6) & 0xFFFFFFFF;
                idx1 = 16;
                idx2 = (int) (seed / 6) & 0xFFFFFFFF;
                retval.append(vowels[idx0]);
                retval.append(consonants[idx1]);
                retval.append(vowels[idx2]);
            }
        }
        retval.append('x');

        return retval;
    }
}
