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

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509CRL;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.RC2ParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTCTime;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.pkcs.IssuerAndSerialNumber;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

import org.jruby.ext.openssl.SecurityHelper;
import org.jruby.ext.openssl.x509store.Name;
import org.jruby.ext.openssl.x509store.Store;
import org.jruby.ext.openssl.x509store.StoreContext;
import org.jruby.ext.openssl.x509store.X509AuxCertificate;
import org.jruby.ext.openssl.x509store.X509Utils;

/** c: PKCS7
 *
 * Basically equivalent of the ContentInfo structures in PKCS#7.
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class PKCS7 {
    // OpenSSL behavior: PKCS#7 ObjectId for "ITU-T" + "0"
    private static final String EMPTY_PKCS7_OID = "0.0";

	/* content as defined by the type */
	/* all encryption/message digests are applied to the 'contents',
	 * leaving out the 'type' field. */
    private PKCS7Data data;

    public Object ctrl(int cmd, Object v, Object ignored) throws PKCS7Exception {
        return this.data.ctrl(cmd, v, ignored);
    }

    public void setDetached(int v) throws PKCS7Exception {
        ctrl(OP_SET_DETACHED_SIGNATURE, Integer.valueOf(v), null);
    }

    public int getDetached() throws PKCS7Exception {
        return ((Integer)ctrl(OP_GET_DETACHED_SIGNATURE, null, null)).intValue();
    }

    public boolean isDetached() throws PKCS7Exception {
        return isSigned() && getDetached() != 0;
    }

    private void initiateWith(Integer nid, ASN1Encodable content) throws PKCS7Exception {
        this.data = PKCS7Data.fromASN1(nid, content);
    }

    public static PKCS7 newEmpty() {
        PKCS7 p7 = new PKCS7();
        p7.data = new PKCS7DataData();
        return p7;
    }

    /**
     * ContentInfo ::= SEQUENCE {
     *   contentType ContentType,
     *   content [0] EXPLICIT ANY DEFINED BY contentType OPTIONAL }
     *
     * ContentType ::= OBJECT IDENTIFIER
     */
    public static PKCS7 fromASN1(ASN1Encodable obj) throws PKCS7Exception {
        PKCS7 p7 = new PKCS7();

        int size = ((ASN1Sequence) obj).size();
        if (size == 0) {
            return p7;
        }

        ASN1ObjectIdentifier contentType = (ASN1ObjectIdentifier) (((ASN1Sequence) obj).getObjectAt(0));
        if (EMPTY_PKCS7_OID.equals(contentType.getId())) {
            // OpenSSL behavior
            p7.setType(ASN1Registry.NID_undef);
        } else {
            Integer nid = ASN1Registry.obj2nid(contentType);

            ASN1Encodable content = size == 1 ? (ASN1Encodable) null : ((ASN1Sequence) obj).getObjectAt(1);

            if (content != null && content instanceof ASN1TaggedObject && ((ASN1TaggedObject) content).getTagNo() == 0) {
                content = ((ASN1TaggedObject) content).getObject();
            }
            p7.initiateWith(nid, content);
        }
        return p7;
    }

    /* c: d2i_PKCS7_bio
     *
     */
    public static PKCS7 fromASN1(BIO bio) throws IOException, PKCS7Exception {
        ASN1InputStream ais = new ASN1InputStream(BIO.asInputStream(bio));
        return fromASN1(ais.readObject());
    }

    public ASN1Encodable asASN1() {
        ASN1EncodableVector vector = new ASN1EncodableVector();
        ASN1ObjectIdentifier contentType;
        if (data == null) {
            // OpenSSL behavior
            contentType = new ASN1ObjectIdentifier(EMPTY_PKCS7_OID);
        } else {
            contentType = ASN1Registry.nid2obj(getType());
        }
        vector.add(contentType);
        if (data != null) {
            vector.add(new DERTaggedObject(0, data.asASN1()));
        }
        return new DLSequence(vector);
    }

    /* c: i2d_PKCS7
     *
     */
    public byte[] toASN1() throws IOException {
        return asASN1().toASN1Primitive().getEncoded();
    }

    /* c: PKCS7_add_signature
     *
     */
    public SignerInfoWithPkey addSignature(X509AuxCertificate x509, PrivateKey pkey, MessageDigest dgst) throws PKCS7Exception{
        SignerInfoWithPkey si = new SignerInfoWithPkey();
        si.set(x509, pkey, dgst);
        addSigner(si);
        return si;
    }

    /* c: X509_find_by_issuer_and_serial
     *
     */
    public static X509AuxCertificate findByIssuerAndSerial(Collection<X509AuxCertificate> certs, X500Name issuer, BigInteger serial) {
        Name name = new Name(issuer);
        for(X509AuxCertificate cert : certs) {
            if(name.isEqual(cert.getIssuerX500Principal()) && serial.equals(cert.getSerialNumber())) {
                return cert;
            }
        }
        return null;
    }


    /* c: PKCS7_get0_signers
     *
     */
    public List<X509AuxCertificate> getSigners(Collection<X509AuxCertificate> certs, List<SignerInfoWithPkey> sinfos, int flags) throws PKCS7Exception {
        List<X509AuxCertificate> signers = new ArrayList<X509AuxCertificate>();

        if(!isSigned()) {
            throw new PKCS7Exception(F_PKCS7_GET0_SIGNERS,R_WRONG_CONTENT_TYPE);
        }

        if(sinfos.size() == 0) {
            throw new PKCS7Exception(F_PKCS7_GET0_SIGNERS,R_NO_SIGNERS);
        }

        for(SignerInfoWithPkey si : sinfos) {
            IssuerAndSerialNumber ias = si.getIssuerAndSerialNumber();
            X509AuxCertificate signer = null;
//             System.err.println("looking for: " + ias.getName() + " and " + ias.getCertificateSerialNumber());
//             System.err.println(" in: " + certs);
//             System.err.println(" in: " + getSign().getCert());
            if(certs != null) {
                signer = findByIssuerAndSerial(certs, ias.getName(), ias.getCertificateSerialNumber().getValue());
            }
            if(signer == null && (flags & NOINTERN) == 0 && getSign().getCert() != null) {
                signer = findByIssuerAndSerial(getSign().getCert(), ias.getName(), ias.getCertificateSerialNumber().getValue());
            }
            if(signer == null) {
                throw new PKCS7Exception(F_PKCS7_GET0_SIGNERS,R_SIGNER_CERTIFICATE_NOT_FOUND);
            }
            signers.add(signer);
        }
        return signers;
    }

    /* c: PKCS7_digest_from_attributes
     *
     */
    public ASN1OctetString digestFromAttributes(ASN1Set attributes) {
        return (ASN1OctetString)SignerInfoWithPkey.getAttribute(attributes, ASN1Registry.NID_pkcs9_messageDigest);
    }

    /* c: PKCS7_signatureVerify
     *
     */
    public void signatureVerify(BIO bio, SignerInfoWithPkey si, X509AuxCertificate x509) throws PKCS7Exception {
        if(!isSigned() && !isSignedAndEnveloped()) {
            throw new PKCS7Exception(F_PKCS7_SIGNATUREVERIFY, R_WRONG_PKCS7_TYPE);
        }

        int md_type = ASN1Registry.obj2nid(si.getDigestAlgorithm().getAlgorithm()).intValue();
        BIO btmp = bio;
        MessageDigest mdc = null;

        for(;;) {
            if(btmp == null || (btmp = bio.findType(BIO.TYPE_MD)) == null) {
                throw new PKCS7Exception(F_PKCS7_SIGNATUREVERIFY, R_UNABLE_TO_FIND_MESSAGE_DIGEST);
            }

            mdc = ((MessageDigestBIOFilter)btmp).getMessageDigest();
            if(null == mdc) {
                throw new PKCS7Exception(F_PKCS7_SIGNATUREVERIFY, -1);
            }

            if(EVP.type(mdc) == md_type) {
                break;
            }

            btmp = btmp.next();
        }

        MessageDigest mdc_tmp = null;
        try {
            mdc_tmp = (MessageDigest)mdc.clone();
        } catch(Exception e) {}

        byte[] currentData = new byte[0];

        ASN1Set sk = si.getAuthenticatedAttributes();
        try {
            if(sk != null && sk.size() > 0) {
                byte[] md_dat = mdc_tmp.digest();
                ASN1OctetString message_digest = digestFromAttributes(sk);
                if(message_digest == null) {
                    throw new PKCS7Exception(F_PKCS7_SIGNATUREVERIFY, R_UNABLE_TO_FIND_MESSAGE_DIGEST);
                }
                if(!Arrays.equals(md_dat, message_digest.getOctets())) {
                    throw new NotVerifiedPKCS7Exception();
                }

                currentData = sk.getEncoded();
            }

            ASN1OctetString os = si.getEncryptedDigest();
            PublicKey pkey = x509.getPublicKey();

            Signature sign = SecurityHelper.getSignature(EVP.signatureAlgorithm(mdc_tmp, pkey));
            sign.initVerify(pkey);
            if(currentData.length > 0) {
                sign.update(currentData);
            }
            if(!sign.verify(os.getOctets())) {
                throw new NotVerifiedPKCS7Exception();
            }
        } catch(NotVerifiedPKCS7Exception e) {
            throw e;
        } catch(Exception e) {
            System.err.println("Other exception");
            e.printStackTrace(System.err);
            throw new NotVerifiedPKCS7Exception();
        }
    }

    /* c: PKCS7_verify
     *
     */
    public void verify(Collection<X509AuxCertificate> certs, Store store, BIO indata, BIO out, int flags) throws PKCS7Exception {
        if(!isSigned()) {
            throw new PKCS7Exception(F_PKCS7_VERIFY, R_WRONG_CONTENT_TYPE);
        }

        if(getDetached() != 0 && indata == null) {
            throw new PKCS7Exception(F_PKCS7_VERIFY, R_NO_CONTENT);
        }

        List<SignerInfoWithPkey> sinfos = new ArrayList<SignerInfoWithPkey>(getSignerInfo());
        if(sinfos.size() == 0) {
            throw new PKCS7Exception(F_PKCS7_VERIFY, R_NO_SIGNATURES_ON_DATA);
        }

        List<X509AuxCertificate> signers = getSigners(certs, sinfos, flags);
        if(signers == null) {
            throw new NotVerifiedPKCS7Exception();
        }

        /* Now verify the certificates */
        if((flags & NOVERIFY) == 0) {
            for(X509AuxCertificate signer : signers) {
                StoreContext cert_ctx = new StoreContext();
                if((flags & NOCHAIN) == 0) {
                    if(cert_ctx.init(store, signer, new ArrayList<X509AuxCertificate>(getSign().getCert())) == 0) {
                        throw new PKCS7Exception(F_PKCS7_VERIFY, -1);
                    }
                    cert_ctx.setPurpose(X509Utils.X509_PURPOSE_SMIME_SIGN);
                } else if(cert_ctx.init(store, signer, null) == 0) {
                    throw new PKCS7Exception(F_PKCS7_VERIFY, -1);
                }
                cert_ctx.setExtraData(1, store.getExtraData(1));
                if((flags & NOCRL) == 0) {
                    cert_ctx.setCRLs((List<X509CRL>)getSign().getCrl());
                }
                try {
                    int i = cert_ctx.verifyCertificate();
                    int j = 0;
                    if(i <= 0) {
                        j = cert_ctx.getError();
                    }
                    cert_ctx.cleanup();
                    if(i <= 0) {
                        throw new PKCS7Exception(F_PKCS7_VERIFY, R_CERTIFICATE_VERIFY_ERROR, "Verify error:" + X509Utils.verifyCertificateErrorString(j));
                    }
                } catch(PKCS7Exception e) {
                    throw e;
                } catch(Exception e) {
                    throw new PKCS7Exception(F_PKCS7_VERIFY, R_CERTIFICATE_VERIFY_ERROR, e);
                }
            }
        }

        BIO tmpin = indata;
        BIO p7bio = dataInit(tmpin);
        BIO tmpout;
        if((flags & TEXT) != 0) {
            tmpout = BIO.mem();
        } else {
            tmpout = out;
        }

        byte[] buf = new byte[4096];
        for(;;) {
            try {
                int i = p7bio.read(buf, 0, buf.length);
                if(i <= 0) {
                    break;
                }
                if(tmpout != null) {
                    tmpout.write(buf, 0, i);
                }
            } catch(IOException e) {
                throw new PKCS7Exception(F_PKCS7_VERIFY, -1, e);
            }
        }

        if((flags & TEXT) != 0) {
            new SMIME(Mime.DEFAULT).text(tmpout, out);
        }

        if((flags & NOSIGS) == 0) {
            for(int i=0; i<sinfos.size(); i++) {
                SignerInfoWithPkey si = sinfos.get(i);
                X509AuxCertificate signer = signers.get(i);
                signatureVerify(p7bio, si, signer);
            }
        }

        if(tmpin == indata) {
            if(indata != null) {
                p7bio.pop();
            }
        }
    }

    private static final BigInteger BI_128 = BigInteger.valueOf(128);
    private static final BigInteger BI_64 = BigInteger.valueOf(64);
    private static final BigInteger BI_40 = BigInteger.valueOf(40);

    /* c: PKCS7_sign
     *
     */
    public static PKCS7 sign(X509AuxCertificate signcert, PrivateKey pkey, Collection<X509AuxCertificate> certs, BIO data, int flags) throws PKCS7Exception {
        PKCS7 p7 = new PKCS7();
        p7.setType(ASN1Registry.NID_pkcs7_signed);
        p7.contentNew(ASN1Registry.NID_pkcs7_data);
        SignerInfoWithPkey si = p7.addSignature(signcert, pkey, EVP.sha1());
        if ( (flags & NOCERTS) == 0 ) {
            p7.addCertificate(signcert);
            if(certs != null) {
                for(X509AuxCertificate c : certs) {
                    p7.addCertificate(c);
                }
            }
        }

        if ( (flags & NOATTR) == 0 ) {
            si.addSignedAttribute(ASN1Registry.NID_pkcs9_contentType, ASN1Registry.OID_pkcs7_data);
            if ( (flags & NOSMIMECAP) == 0 ) {
                ASN1EncodableVector smcap = new ASN1EncodableVector();
                smcap.add(new AlgorithmIdentifier(ASN1Registry.OID_des_ede3_cbc));
                smcap.add(new AlgorithmIdentifier(ASN1Registry.OID_rc2_cbc, new ASN1Integer(BI_128)));
                smcap.add(new AlgorithmIdentifier(ASN1Registry.OID_rc2_cbc, new ASN1Integer(BI_64)));
                smcap.add(new AlgorithmIdentifier(ASN1Registry.OID_rc2_cbc, new ASN1Integer(BI_40)));
                smcap.add(new AlgorithmIdentifier(ASN1Registry.OID_des_cbc));
                si.addSignedAttribute(ASN1Registry.NID_SMIMECapabilities, new DLSequence(smcap));
            }
        }

        if ( (flags & STREAM) != 0 ) {
            return p7;
        }

        BIO p7bio = p7.dataInit(null);

        try {
            data.crlfCopy(p7bio, flags);
        } catch(IOException e) {
            throw new PKCS7Exception(F_PKCS7_SIGN, R_PKCS7_DATAFINAL_ERROR, e);
        }

        if ( (flags & DETACHED) != 0 ) {
            p7.setDetached(1);
        }

        p7.dataFinal(p7bio);

        return p7;
    }

    /* c: PKCS7_encrypt
     *
     */
    public static PKCS7 encrypt(Collection<X509AuxCertificate> certs, byte[] in, CipherSpec cipher, int flags) throws PKCS7Exception {
        PKCS7 p7 = new PKCS7();

        p7.setType(ASN1Registry.NID_pkcs7_enveloped);

        try {
            p7.setCipher(cipher);

            for(X509AuxCertificate x509 : certs) {
                p7.addRecipient(x509);
            }

            BIO p7bio = p7.dataInit(null);

            BIO.memBuf(in).crlfCopy(p7bio, flags);
            p7bio.flush();
            p7.dataFinal(p7bio);

            return p7;
        } catch(IOException e) {
            throw new PKCS7Exception(F_PKCS7_ENCRYPT, R_PKCS7_DATAFINAL_ERROR, e);
        }
    }

    /* c: PKCS7_decrypt
     *
     */
    public void decrypt(PrivateKey pkey, X509AuxCertificate cert, BIO data, int flags) throws PKCS7Exception {
        if(!isEnveloped()) {
            throw new PKCS7Exception(F_PKCS7_DECRYPT, R_WRONG_CONTENT_TYPE);
        }
        try {
            BIO tmpmem = dataDecode(pkey, null, cert);
            if((flags & TEXT) == TEXT) {
                BIO tmpbuf = BIO.buffered();
                BIO bread = tmpbuf.push(tmpmem);
                new SMIME(Mime.DEFAULT).text(bread, data);
            } else {
                int i;
                byte[] buf = new byte[4096];
                while((i = tmpmem.read(buf, 0, 4096)) > 0) {
                    data.write(buf, 0, i);
                }
            }
        } catch(IOException e) {
            throw new PKCS7Exception(F_PKCS7_DECRYPT, R_DECRYPT_ERROR, e);
        }
    }

    /** c: PKCS7_set_type
     *
     */
    public void setType(int type) throws PKCS7Exception {
        switch(type) {
        case ASN1Registry.NID_undef:
            this.data = null;
            break;
        case ASN1Registry.NID_pkcs7_signed:
            this.data = new PKCS7DataSigned();
            break;
        case ASN1Registry.NID_pkcs7_data:
            this.data = new PKCS7DataData();
            break;
        case ASN1Registry.NID_pkcs7_signedAndEnveloped:
            this.data = new PKCS7DataSignedAndEnveloped();
            break;
        case ASN1Registry.NID_pkcs7_enveloped:
            this.data = new PKCS7DataEnveloped();
            break;
        case ASN1Registry.NID_pkcs7_encrypted:
            this.data = new PKCS7DataEncrypted();
            break;
        case ASN1Registry.NID_pkcs7_digest:
            this.data = new PKCS7DataDigest();
            break;
        default:
            throw new PKCS7Exception(F_PKCS7_SET_TYPE,R_UNSUPPORTED_CONTENT_TYPE);
        }
    }

    /** c: PKCS7_set_cipher
     *
     */
    public void setCipher(CipherSpec cipher) throws PKCS7Exception {
        this.data.setCipher(cipher);
    }

    /** c: PKCS7_add_recipient
     *
     */
    public RecipInfo addRecipient(X509AuxCertificate recip) throws PKCS7Exception {
        RecipInfo ri = new RecipInfo();
        ri.set(recip);
        addRecipientInfo(ri);
        return ri;
    }

    /** c: PKCS7_content_new
     *
     */
    public void contentNew(int nid) throws PKCS7Exception {
        PKCS7 ret = new PKCS7();
        ret.setType(nid);
        this.setContent(ret);
    }

    /** c: PKCS7_add_signer
     *
     */
    public void addSigner(SignerInfoWithPkey psi) throws PKCS7Exception {
        this.data.addSigner(psi);
    }

    /** c: PKCS7_add_certificate
     *
     */
    public void addCertificate(X509AuxCertificate cert) throws PKCS7Exception {
        this.data.addCertificate(cert);
    }

    /** c: PKCS7_add_crl
     *
     */
    public void addCRL(X509CRL crl) throws PKCS7Exception {
        this.data.addCRL(crl);
    }

    /** c: PKCS7_add_recipient_info
     *
     */
    public void addRecipientInfo(RecipInfo ri) throws PKCS7Exception {
        this.data.addRecipientInfo(ri);
    }

    /** c: PKCS7_set_content
     *
     */
    public void setContent(PKCS7 p7) throws PKCS7Exception {
        this.data.setContent(p7);
    }

    /** c: PKCS7_get_signer_info
     *
     */
    public Collection<SignerInfoWithPkey> getSignerInfo() {
        return this.data.getSignerInfo();
    }

    private final static byte[] PEM_STRING_PKCS7_START = "-----BEGIN PKCS7-----".getBytes();

    /** c: PEM_read_bio_PKCS7
     *
     */
    public static PKCS7 readPEM(BIO input) throws PKCS7Exception {
        try {
            byte[] buffer = new byte[SMIME.MAX_SMLEN];
            int read;
            read = input.gets(buffer, SMIME.MAX_SMLEN);
            if(read > PEM_STRING_PKCS7_START.length) {
                byte[] tmp = new byte[PEM_STRING_PKCS7_START.length];
                System.arraycopy(buffer, 0, tmp, 0, tmp.length);
                if(Arrays.equals(PEM_STRING_PKCS7_START, tmp)) {
                    return fromASN1(BIO.base64Filter(input));
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch(IOException e) {
            return null;
        }
    }

    /** c: stati PKCS7_bio_add_digest
     *
     */
    public BIO bioAddDigest(BIO pbio, AlgorithmIdentifier alg) throws PKCS7Exception {
        try {
            MessageDigest md = EVP.getDigest(alg.getAlgorithm());
            BIO btmp = BIO.mdFilter(md);
            if(pbio == null) {
                return btmp;
            } else {
                pbio.push(btmp);
                return pbio;
            }
        } catch(Exception e) {
            throw new PKCS7Exception(F_PKCS7_BIO_ADD_DIGEST, R_UNKNOWN_DIGEST_TYPE, e);
        }
    }

    /** c: PKCS7_dataDecode
     *
     */
    public BIO dataDecode(PrivateKey pkey, BIO inBio, X509AuxCertificate pcert) throws PKCS7Exception {
        BIO out = null; BIO btmp; BIO etmp; BIO bio;
        byte[] dataBody = null;
        Collection<AlgorithmIdentifier> mdSk = null;
        Collection<RecipInfo> rsk = null;
        AlgorithmIdentifier encAlg = null;
        Cipher evpCipher = null;
        RecipInfo ri = null;

        int i = getType();
        switch(i) {
        case ASN1Registry.NID_pkcs7_signed:
            dataBody = getSign().getContents().getOctetString().getOctets();
            mdSk = getSign().getMdAlgs();
            break;
        case ASN1Registry.NID_pkcs7_signedAndEnveloped:
            rsk = getSignedAndEnveloped().getRecipientInfo();
            mdSk = getSignedAndEnveloped().getMdAlgs();
            dataBody = getSignedAndEnveloped().getEncData().getEncData().getOctets();
            encAlg = getSignedAndEnveloped().getEncData().getAlgorithm();
            try {
                evpCipher = EVP.getCipher(encAlg.getAlgorithm());
            } catch(Exception e) {
                e.printStackTrace(System.err);
                throw new PKCS7Exception(F_PKCS7_DATADECODE, R_UNSUPPORTED_CIPHER_TYPE, e);
            }
            break;
        case ASN1Registry.NID_pkcs7_enveloped:
            rsk = getEnveloped().getRecipientInfo();
            dataBody = getEnveloped().getEncData().getEncData().getOctets();
            encAlg = getEnveloped().getEncData().getAlgorithm();
            try {
                evpCipher = EVP.getCipher(encAlg.getAlgorithm());
            } catch(Exception e) {
                e.printStackTrace(System.err);
                throw new PKCS7Exception(F_PKCS7_DATADECODE, R_UNSUPPORTED_CIPHER_TYPE, e);
            }
            break;
        default:
            throw new PKCS7Exception(F_PKCS7_DATADECODE, R_UNSUPPORTED_CONTENT_TYPE);
        }

        /* We will be checking the signature */
        if(mdSk != null) {
            for(AlgorithmIdentifier xa : mdSk) {
                try {
                    MessageDigest evpMd = EVP.getDigest(xa.getAlgorithm());
                    btmp = BIO.mdFilter(evpMd);
                    if(out == null) {
                        out = btmp;
                    } else {
                        out.push(btmp);
                    }
                } catch(Exception e) {
                    e.printStackTrace(System.err);
                    throw new PKCS7Exception(F_PKCS7_DATADECODE, R_UNKNOWN_DIGEST_TYPE, e);
                }
            }
        }


        if(evpCipher != null) {

            /* It was encrypted, we need to decrypt the secret key
             * with the private key */

            /* Find the recipientInfo which matches the passed certificate
             * (if any)
             */
            if(pcert != null) {
                for(Iterator<RecipInfo> iter = rsk.iterator(); iter.hasNext();) {
                    ri = iter.next();
                    if(ri.compare(pcert)) {
                        break;
                    }
                    ri = null;
                }
                if(null == ri) {
                    throw new PKCS7Exception(F_PKCS7_DATADECODE, R_NO_RECIPIENT_MATCHES_CERTIFICATE);
                }
            }

            byte[] tmp = null;
            /* If we haven't got a certificate try each ri in turn */
            if(null == pcert) {
                for(Iterator<RecipInfo> iter = rsk.iterator(); iter.hasNext();) {
                    ri = iter.next();
                    try {
                        tmp = EVP.decrypt(ri.getEncKey().getOctets(), pkey);
                        if(tmp != null) {
                            break;
                        }
                    } catch(Exception e) {
                        tmp = null;
                    }
                    ri = null;
                }
                if(ri == null) {
                    throw new PKCS7Exception(F_PKCS7_DATADECODE, R_NO_RECIPIENT_MATCHES_KEY);
                }
            } else {
                try {
                    Cipher cipher = SecurityHelper.getCipher(CipherSpec.getWrappingAlgorithm(pkey.getAlgorithm()));
                    cipher.init(Cipher.DECRYPT_MODE, pkey);
                    tmp = cipher.doFinal(ri.getEncKey().getOctets());
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    throw new PKCS7Exception(F_PKCS7_DATADECODE, -1, e);
                }
            }

            ASN1Encodable params = encAlg.getParameters();
            try {
                String algo = org.jruby.ext.openssl.Cipher.Algorithm.getAlgorithmBase(evpCipher);
                if(params != null && params instanceof ASN1OctetString) {
                    if (algo.startsWith("RC2")) {
                        // J9's IBMJCE needs this exceptional RC2 support.
                        // Giving IvParameterSpec throws 'Illegal parameter' on IBMJCE.
                        SecretKeySpec sks = new SecretKeySpec(tmp, algo);
                        RC2ParameterSpec s = new RC2ParameterSpec(tmp.length * 8, ((ASN1OctetString) params).getOctets());
                        evpCipher.init(Cipher.DECRYPT_MODE, sks, s);
                    } else {
                        SecretKeySpec sks = new SecretKeySpec(tmp, algo);
                        IvParameterSpec iv = new IvParameterSpec(((ASN1OctetString) params).getOctets());
                        evpCipher.init(Cipher.DECRYPT_MODE, sks, iv);
                    }
                } else {
                    evpCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(tmp, algo));
                }
            } catch(Exception e) {
                e.printStackTrace(System.err);
                throw new PKCS7Exception(F_PKCS7_DATADECODE, -1, e);
            }

            etmp = BIO.cipherFilter(evpCipher);
            if(out == null) {
                out = etmp;
            } else {
                out.push(etmp);
            }
        }

        if(isDetached() || inBio != null) {
            bio = inBio;
        } else {
            if(dataBody != null && dataBody.length > 0) {
                bio = BIO.memBuf(dataBody);
            } else {
                bio = BIO.mem();
            }
        }
        out.push(bio);
        return out;
    }

    /** c: PKCS7_dataInit
     *
     */
    public BIO dataInit(BIO bio) throws PKCS7Exception {
        Collection<AlgorithmIdentifier> mdSk = null;
        ASN1OctetString os = null;
        int i = this.data.getType();
        Collection<RecipInfo> rsk = null;
        AlgorithmIdentifier xa = null;
        CipherSpec evpCipher = null;
        BIO out = null;
        BIO btmp = null;
        EncContent enc = null;
        switch (i) {
            case ASN1Registry.NID_pkcs7_signed:
                mdSk = getSign().getMdAlgs();
                os = getSign().getContents().getOctetString();
                break;
            case ASN1Registry.NID_pkcs7_signedAndEnveloped:
                rsk = getSignedAndEnveloped().getRecipientInfo();
                mdSk = getSignedAndEnveloped().getMdAlgs();
                enc = getSignedAndEnveloped().getEncData();
                evpCipher = getSignedAndEnveloped().getEncData().getCipher();
                if (null == evpCipher) {
                    throw new PKCS7Exception(F_PKCS7_DATAINIT, R_CIPHER_NOT_INITIALIZED);
                }
                break;
            case ASN1Registry.NID_pkcs7_enveloped:
                rsk = getEnveloped().getRecipientInfo();
                enc = getEnveloped().getEncData();
                evpCipher = getEnveloped().getEncData().getCipher();
                if (null == evpCipher) {
                    throw new PKCS7Exception(F_PKCS7_DATAINIT, R_CIPHER_NOT_INITIALIZED);
                }
                break;
            case ASN1Registry.NID_pkcs7_digest:
                xa = getDigest().getMd();
                os = getDigest().getContents().getOctetString();
                break;
            default:
                throw new PKCS7Exception(F_PKCS7_DATAINIT, R_UNSUPPORTED_CONTENT_TYPE);
        }

        if (mdSk != null) {
            for (AlgorithmIdentifier ai : mdSk) {
                if ((out = bioAddDigest(out, ai)) == null) {
                    return null;
                }
            }
        }

        if (xa != null && (out = bioAddDigest(out, xa)) == null) {
            return null;
        }

        if (evpCipher != null) {
            byte[] tmp;
            btmp = BIO.cipherFilter(evpCipher.getCipher());
            String algoBase = evpCipher.getCipher().getAlgorithm();
            if (algoBase.indexOf('/') != -1) {
                algoBase = algoBase.split("/")[0];
            }
            try {
                KeyGenerator gen = SecurityHelper.getKeyGenerator(algoBase);
                gen.init(evpCipher.getKeyLenInBits(), SecurityHelper.getSecureRandom());
                SecretKey key = gen.generateKey();
                evpCipher.getCipher().init(Cipher.ENCRYPT_MODE, key);
                if (null != rsk) {
                    for (RecipInfo ri : rsk) {
                        PublicKey pkey = ri.getCert().getPublicKey();
                        Cipher cipher = SecurityHelper.getCipher(CipherSpec.getWrappingAlgorithm(pkey.getAlgorithm()));
                        cipher.init(Cipher.ENCRYPT_MODE, pkey);
                        tmp = cipher.doFinal(key.getEncoded());
                        ri.setEncKey(new DEROctetString(tmp));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
                throw new PKCS7Exception(F_PKCS7_DATAINIT, R_ERROR_SETTING_CIPHER, e);
            }

            ASN1ObjectIdentifier encAlgo = ASN1Registry.sym2oid(evpCipher.getOsslName());
            if (encAlgo == null) {
                throw new PKCS7Exception(F_PKCS7_DATAINIT, R_CIPHER_HAS_NO_OBJECT_IDENTIFIER);
            }
            if (evpCipher.getCipher().getIV() != null) {
                enc.setAlgorithm(new AlgorithmIdentifier(encAlgo, new DEROctetString(evpCipher.getCipher().getIV())));
            } else {
                enc.setAlgorithm(new AlgorithmIdentifier(encAlgo));
            }

            if (out == null) {
                out = btmp;
            } else {
                out.push(btmp);
            }
        }

        if (bio == null) {
            if (isDetached()) {
                bio = BIO.nullSink();
            } else if (os != null && os.getOctets().length > 0) {
                bio = BIO.memBuf(os.getOctets());
            }
            if (bio == null) {
                bio = BIO.mem();
                bio.setMemEofReturn(0);
            }
        }

        if (out != null) {
            out.push(bio);
        } else {
            out = bio;
        }
        return out;
    }

    /** c: static PKCS7_find_digest
     *
     */
    public BIO findDigest(MessageDigest[] pmd, BIO bio, int nid) throws PKCS7Exception {
        while(true) {
            bio = bio.findType(BIO.TYPE_MD);
            if(bio == null) {
                throw new PKCS7Exception(F_PKCS7_FIND_DIGEST, R_UNABLE_TO_FIND_MESSAGE_DIGEST);
            }
            pmd[0] = ((MessageDigestBIOFilter)bio).getMessageDigest();
            if(pmd[0] == null) {
                throw new PKCS7Exception(F_PKCS7_FIND_DIGEST, -1);
            }

            if(nid == EVP.type(pmd[0])) {
                return bio;
            }

            bio = bio.next();
        }
    }

    /** c: PKCS7_dataFinal
     *
     */
    public int dataFinal(BIO bio) throws PKCS7Exception {
        Collection<SignerInfoWithPkey> siSk = null;
        BIO btmp;
        byte[] buf;
        MessageDigest mdc = null;
        MessageDigest ctx_tmp = null;
        ASN1Set sk;

        int i = this.data.getType();

        switch(i) {
        case ASN1Registry.NID_pkcs7_signedAndEnveloped:
            siSk = getSignedAndEnveloped().getSignerInfo();
            break;
        case ASN1Registry.NID_pkcs7_signed:
            siSk = getSign().getSignerInfo();
            break;
        case ASN1Registry.NID_pkcs7_digest:
            break;
        default:
            break;
        }

        if(siSk != null) {
            for(SignerInfoWithPkey si : siSk) {
                if(si.getPkey() == null) {
                    continue;
                }
                int j = ASN1Registry.obj2nid(si.getDigestAlgorithm().getAlgorithm());
                btmp = bio;
                MessageDigest[] _mdc = new MessageDigest[] {mdc};
                btmp = findDigest(_mdc, btmp, j);
                mdc = _mdc[0];
                if(btmp == null) {
                    return 0;
                }

                try {
                    ctx_tmp = (MessageDigest)mdc.clone();
                } catch(CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }

                sk = si.getAuthenticatedAttributes();

                Signature sign = null;

                try {
                    if(sk != null && sk.size() > 0) {
                        /* Add signing time if not already present */
                        if(null == si.getSignedAttribute(ASN1Registry.NID_pkcs9_signingTime)) {
                            DERUTCTime signTime = new DERUTCTime(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime());
                            si.addSignedAttribute(ASN1Registry.NID_pkcs9_signingTime, signTime);
                        }

                        byte[] md_data = ctx_tmp.digest();
                        ASN1OctetString digest = new DEROctetString(md_data);
                        si.addSignedAttribute(ASN1Registry.NID_pkcs9_messageDigest, digest);

                        sk = si.getAuthenticatedAttributes();
                        sign = SecurityHelper.getSignature(EVP.signatureAlgorithm(ctx_tmp, si.getPkey()));
                        sign.initSign(si.getPkey());

                        byte[] abuf = sk.getEncoded();
                        sign.update(abuf);
                    }

                    if(sign != null) {
                        byte[] out = sign.sign();
                        si.setEncryptedDigest(new DEROctetString(out));
                    }
                } catch(Exception e) {
                    throw new PKCS7Exception(F_PKCS7_DATAFINAL, -1, e);
                }
            }
        } else if(i == ASN1Registry.NID_pkcs7_digest) {
            int nid = ASN1Registry.obj2nid(getDigest().getMd().getAlgorithm());
            MessageDigest[] _mdc = new MessageDigest[] {mdc};
            bio = findDigest(_mdc, bio, nid);
            mdc = _mdc[0];
            byte[] md_data = mdc.digest();
            ASN1OctetString digest = new DEROctetString(md_data);
            getDigest().setDigest(digest);
        }

        if(!isDetached()) {
            btmp = bio.findType(BIO.TYPE_MEM);
            if(null == btmp) {
                throw new PKCS7Exception(F_PKCS7_DATAFINAL, R_UNABLE_TO_FIND_MEM_BIO);
            }
            buf = ((MemBIO)btmp).getMemCopy();
            switch(i) {
            case ASN1Registry.NID_pkcs7_signedAndEnveloped:
                getSignedAndEnveloped().getEncData().setEncData(new DEROctetString(buf));
                break;
            case ASN1Registry.NID_pkcs7_enveloped:
                getEnveloped().getEncData().setEncData(new DEROctetString(buf));
                break;
            case ASN1Registry.NID_pkcs7_signed:
                if(getSign().getContents().isData() && getDetached() != 0) {
                    getSign().getContents().setData(null);
                } else {
                    getSign().getContents().setData(new DEROctetString(buf));
                }
                break;
            case ASN1Registry.NID_pkcs7_digest:
                if(getDigest().getContents().isData() && getDetached() != 0) {
                    getDigest().getContents().setData(null);
                } else {
                    getDigest().getContents().setData(new DEROctetString(buf));
                }
                break;
            }
        }

        return 1;
    }

    @Override
    public String toString() {
        return "#<PKCS7 " + this.data + ">";
    }

    public static final int S_HEADER = 0;
    public static final int S_BODY = 1;
    public static final int S_TAIL = 2;

    public static final int OP_SET_DETACHED_SIGNATURE = 1;
    public static final int OP_GET_DETACHED_SIGNATURE = 2;

    /* S/MIME related flags */
    public static final int TEXT = 0x1;
    public static final int NOCERTS = 0x2;
    public static final int NOSIGS = 0x4;
    public static final int NOCHAIN = 0x8;
    public static final int NOINTERN = 0x10;
    public static final int NOVERIFY = 0x20;
    public static final int DETACHED = 0x40;
    public static final int BINARY = 0x80;
    public static final int NOATTR = 0x100;
    public static final int NOSMIMECAP = 0x200;
    public static final int NOOLDMIMETYPE = 0x400;
    public static final int CRLFEOL = 0x800;
    public static final int STREAM = 0x1000;
    public static final int NOCRL = 0x2000;
    public static final int PARTIAL = 0x4000;
    public static final int REUSE_DIGEST = 0x8000;

    /* Flags: for compatibility with older code */
    public static final int SMIME_TEXT = TEXT;
    public static final int SMIME_NOCERTS = NOCERTS;
    public static final int SMIME_NOSIGS = NOSIGS;
    public static final int SMIME_NOCHAIN = NOCHAIN;
    public static final int SMIME_NOINTERN = NOINTERN;
    public static final int SMIME_NOVERIFY = NOVERIFY;
    public static final int SMIME_DETACHED = DETACHED;
    public static final int SMIME_BINARY = BINARY;
    public static final int SMIME_NOATTR = NOATTR;
    public static final int SMIME_OLDMIME = NOOLDMIMETYPE;
    public static final int SMIME_CRLFEOL = CRLFEOL;

    /* Function codes. */
    public static final int F_B64_READ_PKCS7 = 120;
    public static final int F_B64_WRITE_PKCS7 = 121;
    public static final int F_PKCS7_ADD_ATTRIB_SMIMECAP = 118;
    public static final int F_PKCS7_ADD_CERTIFICATE = 100;
    public static final int F_PKCS7_ADD_CRL = 101;
    public static final int F_PKCS7_ADD_RECIPIENT_INFO = 102;
    public static final int F_PKCS7_ADD_SIGNER = 103;
    public static final int F_PKCS7_BIO_ADD_DIGEST = 125;
    public static final int F_PKCS7_CTRL = 104;
    public static final int F_PKCS7_DATADECODE = 112;
    public static final int F_PKCS7_DATAFINAL = 128;
    public static final int F_PKCS7_DATAINIT = 105;
    public static final int F_PKCS7_DATASIGN = 106;
    public static final int F_PKCS7_DATAVERIFY = 107;
    public static final int F_PKCS7_DECRYPT = 114;
    public static final int F_PKCS7_ENCRYPT = 115;
    public static final int F_PKCS7_FIND_DIGEST = 127;
    public static final int F_PKCS7_GET0_SIGNERS = 124;
    public static final int F_PKCS7_SET_CIPHER = 108;
    public static final int F_PKCS7_SET_CONTENT = 109;
    public static final int F_PKCS7_SET_DIGEST = 126;
    public static final int F_PKCS7_SET_TYPE = 110;
    public static final int F_PKCS7_SIGN = 116;
    public static final int F_PKCS7_SIGNATUREVERIFY = 113;
    public static final int F_PKCS7_SIMPLE_SMIMECAP = 119;
    public static final int F_PKCS7_VERIFY = 117;
    public static final int F_SMIME_READ_PKCS7 = 122;
    public static final int F_SMIME_TEXT = 123;

    /* Reason codes. */
    public static final int R_CERTIFICATE_VERIFY_ERROR = 117;
    public static final int R_CIPHER_HAS_NO_OBJECT_IDENTIFIER = 144;
    public static final int R_CIPHER_NOT_INITIALIZED = 116;
    public static final int R_CONTENT_AND_DATA_PRESENT = 118;
    public static final int R_DECODE_ERROR = 130;
    public static final int R_DECRYPTED_KEY_IS_WRONG_LENGTH = 100;
    public static final int R_DECRYPT_ERROR = 119;
    public static final int R_DIGEST_FAILURE = 101;
    public static final int R_ERROR_ADDING_RECIPIENT = 120;
    public static final int R_ERROR_SETTING_CIPHER = 121;
    public static final int R_INVALID_MIME_TYPE = 131;
    public static final int R_INVALID_NULL_POINTER = 143;
    public static final int R_MIME_NO_CONTENT_TYPE = 132;
    public static final int R_MIME_PARSE_ERROR = 133;
    public static final int R_MIME_SIG_PARSE_ERROR = 134;
    public static final int R_MISSING_CERIPEND_INFO = 103;
    public static final int R_NO_CONTENT = 122;
    public static final int R_NO_CONTENT_TYPE = 135;
    public static final int R_NO_MULTIPART_BODY_FAILURE = 136;
    public static final int R_NO_MULTIPART_BOUNDARY = 137;
    public static final int R_NO_RECIPIENT_MATCHES_CERTIFICATE = 115;
    public static final int R_NO_RECIPIENT_MATCHES_KEY = 146;
    public static final int R_NO_SIGNATURES_ON_DATA = 123;
    public static final int R_NO_SIGNERS = 142;
    public static final int R_NO_SIG_CONTENT_TYPE = 138;
    public static final int R_OPERATION_NOT_SUPPORTED_ON_THIS_TYPE = 104;
    public static final int R_PKCS7_ADD_SIGNATURE_ERROR = 124;
    public static final int R_PKCS7_DATAFINAL = 126;
    public static final int R_PKCS7_DATAFINAL_ERROR = 125;
    public static final int R_PKCS7_DATASIGN = 145;
    public static final int R_PKCS7_PARSE_ERROR = 139;
    public static final int R_PKCS7_SIG_PARSE_ERROR = 140;
    public static final int R_PRIVATE_KEY_DOES_NOT_MATCH_CERTIFICATE = 127;
    public static final int R_SIGNATURE_FAILURE = 105;
    public static final int R_SIGNER_CERTIFICATE_NOT_FOUND = 128;
    public static final int R_SIG_INVALID_MIME_TYPE = 141;
    public static final int R_SMIME_TEXT_ERROR = 129;
    public static final int R_UNABLE_TO_FIND_CERTIFICATE = 106;
    public static final int R_UNABLE_TO_FIND_MEM_BIO = 107;
    public static final int R_UNABLE_TO_FIND_MESSAGE_DIGEST = 108;
    public static final int R_UNKNOWN_DIGEST_TYPE = 109;
    public static final int R_UNKNOWN_OPERATION = 110;
    public static final int R_UNSUPPORTED_CIPHER_TYPE = 111;
    public static final int R_UNSUPPORTED_CONTENT_TYPE = 112;
    public static final int R_WRONG_CONTENT_TYPE = 113;
    public static final int R_WRONG_PKCS7_TYPE = 114;

    public Envelope getEnveloped() {
        return this.data.getEnveloped();
    }

    public SignEnvelope getSignedAndEnveloped() {
        return this.data.getSignedAndEnveloped();
    }

    public Digest getDigest() {
        return this.data.getDigest();
    }

    public Encrypt getEncrypted() {
        return this.data.getEncrypted();
    }

    public ASN1Encodable getOther() {
        return this.data.getOther();
    }

    public void setSign(Signed sign) {
        this.data.setSign(sign);
    }

    public Signed getSign() {
        return this.data.getSign();
    }

    public void setData(ASN1OctetString data) {
        this.data.setData(data);
    }

    public ASN1OctetString getData() {
        return this.data.getData();
    }

    public boolean isSigned() {
        return this.data.isSigned();
    }

    public boolean isEncrypted() {
        return this.data.isEncrypted();
    }

    public boolean isEnveloped() {
        return this.data.isEnveloped();
    }

    public boolean isSignedAndEnveloped() {
        return this.data.isSignedAndEnveloped();
    }

    public boolean isData() {
        return this.data.isData();
    }

    public boolean isDigest() {
        return this.data.isDigest();
    }

    public boolean isOther() {
        return this.data.isOther();
    }

    public int getType() {
        return this.data.getType();
    }

    /* c: static PKCS7_get_octet_string
     *
     */
    public ASN1OctetString getOctetString() {
        if(isData()) {
            return getData();
        } else if(isOther() && getOther() != null && getOther() instanceof ASN1OctetString) {
            return (ASN1OctetString)getOther();
        }
        return null;
    }
}// PKCS7

