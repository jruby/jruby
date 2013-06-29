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

import java.security.cert.X509CRL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.jruby.ext.openssl.x509store.X509AuxCertificate;

/** PKCS7_SIGN_ENVELOPE
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class SignEnvelope {
    private int version;

    /**
     * Describe encContent here.
     */
    private EncContent encData = new EncContent();

    /**
     * Describe crl here.
     */
    private Collection<X509CRL> crl = new ArrayList<X509CRL>();

    /**
     * Describe cert here.
     */
    private Collection<X509AuxCertificate> cert = new ArrayList<X509AuxCertificate>();

    /**
     * Describe mdAlgs here.
     */
    private Set<AlgorithmIdentifier> mdAlgs = new HashSet<AlgorithmIdentifier>();

    /**
     * Describe signerInfo here.
     */
    private Collection<SignerInfoWithPkey> signerInfo = new ArrayList<SignerInfoWithPkey>();

    /**
     * Describe recipientInfo here.
     */
    private Collection<RecipInfo> recipientInfo = new ArrayList<RecipInfo>();

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
     * Get the <code>EncData</code> value.
     *
     * @return an <code>EncContent</code> value
     */
    public final EncContent getEncData() {
        return encData;
    }

    /**
     * Set the <code>EncData</code> value.
     *
     * @param newEncContent The new EncContent value.
     */
    public final void setEncData(final EncContent newEncData) {
        this.encData = newEncData;
    }

    /**
     * Get the <code>RecipientInfo</code> value.
     *
     * @return a <code>Collection<RecipInfo></code> value
     */
    public final Collection<RecipInfo> getRecipientInfo() {
        return recipientInfo;
    }

    /**
     * Set the <code>RecipientInfo</code> value.
     *
     * @param newRecipientInfo The new RecipientInfo value.
     */
    public final void setRecipientInfo(final Collection<RecipInfo> newRecipientInfo) {
        this.recipientInfo = newRecipientInfo;
    }

    /**
     * Get the <code>SignerInfoWithPkey</code> value.
     *
     * @return a <code>Collection<SignerInfoWithPkey></code> value
     */
    public final Collection<SignerInfoWithPkey> getSignerInfo() {
        return signerInfo;
    }

    /**
     * Set the <code>SignerInfoWithPkey</code> value.
     *
     * @param newSignerInfo The new SignerInfo value.
     */
    public final void setSignerInfo(final Collection<SignerInfoWithPkey> newSignerInfo) {
        this.signerInfo = newSignerInfo;
    }

    /**
     * Get the <code>MdAlgs</code> value.
     *
     * @return a <code>Set<AlgorithmIdentifier></code> value
     */
    public final Set<AlgorithmIdentifier> getMdAlgs() {
        return mdAlgs;
    }

    /**
     * Set the <code>MdAlgs</code> value.
     *
     * @param newMdAlgs The new MdAlgs value.
     */
    public final void setMdAlgs(final Set<AlgorithmIdentifier> newMdAlgs) {
        this.mdAlgs = newMdAlgs;
    }

    /**
     * Get the <code>Cert</code> value.
     *
     * @return a <code>Collection<X509AuxCertificate></code> value
     */
    public final Collection<X509AuxCertificate> getCert() {
        return cert;
    }

    /**
     * Set the <code>Cert</code> value.
     *
     * @param newCert The new Cert value.
     */
    public final void setCert(final Collection<X509AuxCertificate> newCert) {
        this.cert = newCert;
    }

    /**
     * Get the <code>Crl</code> value.
     *
     * @return a <code>Collection<X509CRL></code> value
     */
    public final Collection<X509CRL> getCrl() {
        return crl;
    }

    /**
     * Set the <code>Crl</code> value.
     *
     * @param newCrl The new Crl value.
     */
    public final void setCrl(final Collection<X509CRL> newCrl) {
        this.crl = newCrl;
    }
}// SignEnvelope
