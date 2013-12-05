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
 * Copyright (C) 2009 Hiroshi Nakamura <nahi@ruby-lang.org>
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
package org.jruby.ext.openssl.impl;

import javax.crypto.Cipher;

/**
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class CipherSpec extends BIOFilter {
    private final Cipher cipher;
    private final String osslName;
    private final int keyLenInBits;

    public CipherSpec(Cipher cipher, String osslName, int keyLenInBits) {
        this.cipher = cipher;
        this.osslName = osslName;
        this.keyLenInBits = keyLenInBits;
    }

    public Cipher getCipher() {
        return cipher;
    }

    public String getOsslName() {
        return osslName;
    }

    public int getKeyLenInBits() {
        return keyLenInBits;
    }

    public String getAlgorithm() {
        return getCipher().getAlgorithm();
    }

    public String getWrappingAlgorithm() {
        return getWrappingAlgorithm(getAlgorithm());
    }

    public static String getWrappingAlgorithm(String algorithm) {
        if (algorithm == null) {
            return null;
        }
        if (algorithm.equalsIgnoreCase("RSA")) {
            return "RSA/ECB/PKCS1Padding";
        } else {
            return algorithm;
        }
    }

}// CipherSpec
