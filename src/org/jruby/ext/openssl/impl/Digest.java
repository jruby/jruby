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
 * Copyright (C) 2008 Ola Bini <ola.bini@gmail.com>
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

import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

/** PKCS7_DIGEST
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class Digest {
    /**
     * Describe version here.
     */
    private int version;

    /**
     * Describe md here.
     */
    private AlgorithmIdentifier md;

    /**
     * Describe digest here.
     */
    private ASN1OctetString digest;

    PKCS7 contents;

    /**
     * Get the <code>Version</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getVersion() {
        return version;
    }

    /**
     * Set the <code>Version</code> value.
     *
     * @param newVersion The new Version value.
     */
    public final void setVersion(final int newVersion) {
        this.version = newVersion;
    }

    /**
     * Get the <code>Contents</code> value.
     *
     * @return a <code>PKCS7</code> value
     */
    public final PKCS7 getContents() {
        return contents;
    }

    /**
     * Set the <code>Contents</code> value.
     *
     * @param newContents The new Contents value.
     */
    public final void setContents(final PKCS7 newContents) {
        this.contents = newContents;
    }

    /**
     * Get the <code>Md</code> value.
     *
     * @return an <code>AlgorithmIdentifier</code> value
     */
    public final AlgorithmIdentifier getMd() {
        return md;
    }

    /**
     * Set the <code>Md</code> value.
     *
     * @param newMd The new Md value.
     */
    public final void setMd(final AlgorithmIdentifier newMd) {
        this.md = newMd;
    }

    /**
     * Get the <code>Digest</code> value.
     *
     * @return an <code>ASN1OctetString</code> value
     */
    public final ASN1OctetString getDigest() {
        return digest;
    }

    /**
     * Set the <code>Digest</code> value.
     *
     * @param newDigest The new Digest value.
     */
    public final void setDigest(final ASN1OctetString newDigest) {
        this.digest = newDigest;
    }
}// Digest
