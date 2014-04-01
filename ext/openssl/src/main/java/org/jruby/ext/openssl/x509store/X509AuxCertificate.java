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
package org.jruby.ext.openssl.x509store;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;

import java.util.Date;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import java.security.Principal;
import java.security.PublicKey;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.Certificate;

import org.jruby.ext.openssl.SecurityHelper;

/**
 * Since regular X509Certificate doesn't represent the Aux part of a
 * certification, this class uses composition and extension to contain
 * both pieces of information.
 *
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509AuxCertificate extends X509Certificate {
    private static final long serialVersionUID = -909543379295427515L;
    private final X509Certificate wrap;
    private final X509Aux aux;

    private boolean valid = false;
    private int ex_flags = 0;

    public X509AuxCertificate(Certificate wrap) throws IOException, CertificateException {
        super();
        CertificateFactory cf = SecurityHelper.getCertificateFactory("X.509");
        ByteArrayInputStream bis = new ByteArrayInputStream(wrap.getEncoded());
        this.wrap = (X509Certificate) cf.generateCertificate(bis);
        this.aux = null;
    }

    public X509AuxCertificate(X509Certificate wrap) {
        this(wrap,null);
    }

    public X509AuxCertificate(X509Certificate wrap, X509Aux aux) {
        super();
        this.wrap = wrap;
        this.aux = aux;
    }

    public X509Aux getAux() {
        return this.aux;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean v) {
        this.valid = v;
    }

    public int getExFlags() {
        return ex_flags;
    }

    public void setExFlags(int ex_flags) {
        this.ex_flags = ex_flags;
    }

    public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException { wrap.checkValidity(); }
    public void 	checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException { wrap.checkValidity(date); }
    public int 	getBasicConstraints()  { return wrap.getBasicConstraints(); }
    public List<String> 	getExtendedKeyUsage() throws CertificateParsingException { return wrap.getExtendedKeyUsage(); }
    public Collection<List<?>> 	getIssuerAlternativeNames() throws CertificateParsingException { return wrap.getIssuerAlternativeNames(); }
    public Principal 	getIssuerDN() { return wrap.getIssuerDN(); }
    public boolean[] 	getIssuerUniqueID() { return wrap.getIssuerUniqueID(); }
    public X500Principal 	getIssuerX500Principal() { return wrap.getIssuerX500Principal(); }
    public boolean[] 	getKeyUsage() { return wrap.getKeyUsage(); }
    public Date 	getNotAfter() { return wrap.getNotAfter(); }
    public Date 	getNotBefore() { return wrap.getNotBefore(); }
    public BigInteger 	getSerialNumber() { return wrap.getSerialNumber(); }
    public String 	getSigAlgName() { return wrap.getSigAlgName(); }
    public String 	getSigAlgOID() { return wrap.getSigAlgOID(); }
    public byte[] 	getSigAlgParams() { return wrap.getSigAlgParams(); }
    public byte[] 	getSignature() { return wrap.getSignature(); }
    public Collection<List<?>> 	getSubjectAlternativeNames() throws CertificateParsingException { return wrap.getSubjectAlternativeNames(); }
    public Principal 	getSubjectDN() { return wrap.getSubjectDN(); }
    public boolean[] 	getSubjectUniqueID() { return wrap.getSubjectUniqueID(); }
    public X500Principal 	getSubjectX500Principal() { return wrap.getSubjectX500Principal(); }
    public byte[] 	getTBSCertificate() throws CertificateEncodingException { return wrap.getTBSCertificate(); }
    public int 	getVersion() { return wrap.getVersion(); }

    public boolean 	equals(Object other) {
        boolean ret = this == other;
        if(!ret && (other instanceof X509AuxCertificate)) {
            X509AuxCertificate o = (X509AuxCertificate)other;
            ret = this.wrap.equals(o.wrap) && ((this.aux == null) ? o.aux == null : this.aux.equals(o.aux));
        }
        return ret;
    }
    public byte[] 	getEncoded() throws CertificateEncodingException { return wrap.getEncoded(); }
    public PublicKey 	getPublicKey(){ return wrap.getPublicKey(); }
    public int 	hashCode() {
        int ret = wrap.hashCode();
        ret += 3 * (aux == null ? 1 : aux.hashCode());
        return ret;
    }
    public String 	toString(){ return wrap.toString(); }
    public void 	verify(PublicKey key) throws CertificateException,NoSuchAlgorithmException,InvalidKeyException,NoSuchProviderException,SignatureException { wrap.verify(key); }
    public void 	verify(PublicKey key, String sigProvider) throws CertificateException,NoSuchAlgorithmException,InvalidKeyException,NoSuchProviderException,SignatureException { wrap.verify(key,sigProvider); }
    public  Set<String> 	getCriticalExtensionOIDs(){ return wrap.getCriticalExtensionOIDs(); }
    public byte[] 	getExtensionValue(String oid){ return wrap.getExtensionValue(oid); }
    public Set<String> 	getNonCriticalExtensionOIDs(){ return wrap.getNonCriticalExtensionOIDs(); }
    public boolean 	hasUnsupportedCriticalExtension(){ return wrap.hasUnsupportedCriticalExtension(); }

    private static final String NS_CERT_TYPE_OID = "2.16.840.1.113730.1.1";
    public Integer getNsCertType() throws CertificateException {
        byte[] bytes = getExtensionValue(NS_CERT_TYPE_OID);
        if (bytes == null) {
            return null;
        }
        try {
            Object o = new ASN1InputStream(bytes).readObject();
            if (o instanceof DERBitString) {
                return ((DERBitString) o).intValue();
            } else if (o instanceof DEROctetString) {
                // just reads initial object for nsCertType definition and ignores trailing objects.
                ASN1InputStream in = new ASN1InputStream(((DEROctetString) o).getOctets());
                o = in.readObject();
                return ((DERBitString) o).intValue();
            } else {
                throw new CertificateException("unknown type from ASN1InputStream.readObject: " + o);
            }
        } catch (IOException ioe) {
            throw new CertificateEncodingException(ioe.getMessage(), ioe);
        }
    }

}// X509AuxCertificate
