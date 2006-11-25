/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.openssl;

import java.security.MessageDigest;

import org.jruby.IRuby;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Static class that holds various OpenSSL methods that aren't
 * really easy to do any other way.
 *
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class OpenSSLImpl {
    /**
     * No instantiating this class...
     */
    private OpenSSLImpl() {}

    public static IRubyObject to_der(IRubyObject obj) {
        return obj.callMethod(obj.getRuntime().getCurrentContext(),"to_der");
    }

    public static IRubyObject to_der_if_possible(IRubyObject obj) {
        if(obj.respondsTo("to_der")) {
            return to_der(obj);
        } else {
            return obj;
        }
    }

    public static void defaultObjects(IRuby runtime) {
ASN1.addObject(runtime, 0, null, null,"1.2.840.113549.1.12.1");
ASN1.addObject(runtime, 1, null, "rsadsi","1.2.840.113549");
ASN1.addObject(runtime, 2, null, "pkcs","1.2.840.113549.1");
ASN1.addObject(runtime, 3, "MD2", "md2","1.2.840.113549.2.2");
ASN1.addObject(runtime, 4, "MD5", "md5","1.2.840.113549.2.5");
ASN1.addObject(runtime, 5, "RC4", "rc4","1.2.840.113549.3.4");
ASN1.addObject(runtime, 6, null, "rsaEncryption","1.2.840.113549.1.1.1");
ASN1.addObject(runtime, 7, "RSA-MD2", "md2WithRSAEncryption","1.2.840.113549.1.1.2");
ASN1.addObject(runtime, 8, "RSA-MD5", "md5WithRSAEncryption","1.2.840.113549.1.1.4");
ASN1.addObject(runtime, 9, "PBE-MD2-DES", "pbeWithMD2AndDES-CBC","1.2.840.113549.1.5.1");
ASN1.addObject(runtime, 10, "PBE-MD5-DES", "pbeWithMD5AndDES-CBC","1.2.840.113549.1.5.3");
ASN1.addObject(runtime, 11, null, "X500","2.5");
ASN1.addObject(runtime, 12, null, "X509","2.5.4");
ASN1.addObject(runtime, 13, "CN", "commonName","2.5.4.3");
ASN1.addObject(runtime, 14, "C", "countryName","2.5.4.6");
ASN1.addObject(runtime, 15, "L", "localityName","2.5.4.7");
ASN1.addObject(runtime, 16, "ST", "stateOrProvinceName","2.5.4.8");
ASN1.addObject(runtime, 17, "O", "organizationName","2.5.4.10");
ASN1.addObject(runtime, 18, "OU", "organizationalUnitName","2.5.4.11");
ASN1.addObject(runtime, 19, "RSA", "rsa","2.5.8.1.1");
ASN1.addObject(runtime, 20, null, "pkcs7","1.2.840.113549.1.7");
ASN1.addObject(runtime, 21, null, "pkcs7-data","1.2.840.113549.1.7.1");
ASN1.addObject(runtime, 22, null, "pkcs7-signedData","1.2.840.113549.1.7.2");
ASN1.addObject(runtime, 23, null, "pkcs7-envelopedData","1.2.840.113549.1.7.3");
ASN1.addObject(runtime, 24, null, "pkcs7-signedAndEnvelopedData","1.2.840.113549.1.7.4");
ASN1.addObject(runtime, 25, null, "pkcs7-digestData","1.2.840.113549.1.7.5");
ASN1.addObject(runtime, 26, null, "pkcs7-encryptedData","1.2.840.113549.1.7.6");
ASN1.addObject(runtime, 27, null, "pkcs3","1.2.840.113549.1.3");
ASN1.addObject(runtime, 28, null, "dhKeyAgreement","1.2.840.113549.1.3.1");
ASN1.addObject(runtime, 29, "DES-ECB", "des-ecb","1.3.14.3.2.6");
ASN1.addObject(runtime, 30, "DES-CFB", "des-cfb","1.3.14.3.2.9");
ASN1.addObject(runtime, 31, "DES-CBC", "des-cbc","1.3.14.3.2.7");
ASN1.addObject(runtime, 32, "DES-EDE", "des-ede","1.3.14.3.2.17");
ASN1.addObject(runtime, 33, "DES-EDE3", "des-ede3",null);
ASN1.addObject(runtime, 34, "IDEA-CBC", "idea-cbc","1.3.6.1.4.1.188.7.1.1.2");
ASN1.addObject(runtime, 35, "IDEA-CFB", "idea-cfb",null);
ASN1.addObject(runtime, 36, "IDEA-ECB", "idea-ecb",null);
ASN1.addObject(runtime, 37, "RC2-CBC", "rc2-cbc","1.2.840.113549.3.2");
ASN1.addObject(runtime, 38, "RC2-ECB", "rc2-ecb",null);
ASN1.addObject(runtime, 39, "RC2-CFB", "rc2-cfb",null);
ASN1.addObject(runtime, 40, "RC2-OFB", "rc2-ofb",null);
ASN1.addObject(runtime, 41, "SHA", "sha","1.3.14.3.2.18");
ASN1.addObject(runtime, 42, "RSA-SHA", "shaWithRSAEncryption","1.3.14.3.2.15");
ASN1.addObject(runtime, 43, "DES-EDE-CBC", "des-ede-cbc",null);
ASN1.addObject(runtime, 44, "DES-EDE3-CBC", "des-ede3-cbc","1.2.840.113549.3.7");
ASN1.addObject(runtime, 45, "DES-OFB", "des-ofb","1.3.14.3.2.8");
ASN1.addObject(runtime, 46, "IDEA-OFB", "idea-ofb",null);
ASN1.addObject(runtime, 47, null, "pkcs9","1.2.840.113549.1.9");
ASN1.addObject(runtime, 48, "Email", "emailAddress","1.2.840.113549.1.9.1");
ASN1.addObject(runtime, 49, null, "unstructuredName","1.2.840.113549.1.9.2");
ASN1.addObject(runtime, 50, null, "contentType","1.2.840.113549.1.9.3");
ASN1.addObject(runtime, 51, null, "messageDigest","1.2.840.113549.1.9.4");
ASN1.addObject(runtime, 52, null, "signingTime","1.2.840.113549.1.9.5");
ASN1.addObject(runtime, 53, null, "countersignature","1.2.840.113549.1.9.6");
ASN1.addObject(runtime, 54, null, "challengePassword","1.2.840.113549.1.9.7");
ASN1.addObject(runtime, 55, null, "unstructuredAddress","1.2.840.113549.1.9.8");
ASN1.addObject(runtime, 56, null, "extendedCertificateAttributes","1.2.840.113549.1.9.9");
ASN1.addObject(runtime, 57, "Netscape", "Netscape Communications Corp.","2.16.840.1.113730");
ASN1.addObject(runtime, 58, "nsCertExt", "Netscape Certificate Extension","2.16.840.1.113730.1");
ASN1.addObject(runtime, 59, "nsDataType", "Netscape Data Type","2.16.840.1.113730.2");
ASN1.addObject(runtime, 60, "DES-EDE-CFB", "des-ede-cfb",null);
ASN1.addObject(runtime, 61, "DES-EDE3-CFB", "des-ede3-cfb",null);
ASN1.addObject(runtime, 62, "DES-EDE-OFB", "des-ede-ofb",null);
ASN1.addObject(runtime, 63, "DES-EDE3-OFB", "des-ede3-ofb",null);
ASN1.addObject(runtime, 64, "SHA1", "sha1","1.3.14.3.2.26");
ASN1.addObject(runtime, 65, "RSA-SHA1", "sha1WithRSAEncryption","1.2.840.113549.1.1.5");
ASN1.addObject(runtime, 66, "DSA-SHA", "dsaWithSHA","1.3.14.3.2.13");
ASN1.addObject(runtime, 67, "DSA-old", "dsaEncryption-old","1.3.14.3.2.12");
ASN1.addObject(runtime, 68, "PBE-SHA1-RC2-64", "pbeWithSHA1AndRC2-CBC","1.2.840.113549.1.5.11");
ASN1.addObject(runtime, 69, null, "PBKDF2","1.2.840.113549.1.5.12");
ASN1.addObject(runtime, 70, "DSA-SHA1-old", "dsaWithSHA1-old","1.3.14.3.2.27");
ASN1.addObject(runtime, 71, "nsCertType", "Netscape Cert Type","2.16.840.1.113730.1.1");
ASN1.addObject(runtime, 72, "nsBaseUrl", "Netscape Base Url","2.16.840.1.113730.1.2");
ASN1.addObject(runtime, 73, "nsRevocationUrl", "Netscape Revocation Url","2.16.840.1.113730.1.3");
ASN1.addObject(runtime, 74, "nsCaRevocationUrl", "Netscape CA Revocation Url","2.16.840.1.113730.1.4");
ASN1.addObject(runtime, 75, "nsRenewalUrl", "Netscape Renewal Url","2.16.840.1.113730.1.7");
ASN1.addObject(runtime, 76, "nsCaPolicyUrl", "Netscape CA Policy Url","2.16.840.1.113730.1.8");
ASN1.addObject(runtime, 77, "nsSslServerName", "Netscape SSL Server Name","2.16.840.1.113730.1.12");
ASN1.addObject(runtime, 78, "nsComment", "Netscape Comment","2.16.840.1.113730.1.13");
ASN1.addObject(runtime, 79, "nsCertSequence", "Netscape Certificate Sequence","2.16.840.1.113730.2.5");
ASN1.addObject(runtime, 80, "DESX-CBC", "desx-cbc",null);
ASN1.addObject(runtime, 81, "id-ce", null,"2.5.29");
ASN1.addObject(runtime, 82, "subjectKeyIdentifier", "X509v3 Subject Key Identifier","2.5.29.14");
ASN1.addObject(runtime, 83, "keyUsage", "X509v3 Key Usage","2.5.29.15");
ASN1.addObject(runtime, 84, "privateKeyUsagePeriod", "X509v3 Private Key Usage Period","2.5.29.16");
ASN1.addObject(runtime, 85, "subjectAltName", "X509v3 Subject Alternative Name","2.5.29.17");
ASN1.addObject(runtime, 86, "issuerAltName", "X509v3 Issuer Alternative Name","2.5.29.18");
ASN1.addObject(runtime, 87, "basicConstraints", "X509v3 Basic Constraints","2.5.29.19");
ASN1.addObject(runtime, 88, "crlNumber", "X509v3 CRL Number","2.5.29.20");
ASN1.addObject(runtime, 89, "certificatePolicies", "X509v3 Certificate Policies","2.5.29.32");
ASN1.addObject(runtime, 90, "authorityKeyIdentifier", "X509v3 Authority Key Identifier","2.5.29.35");
ASN1.addObject(runtime, 91, "BF-CBC", "bf-cbc","1.3.6.1.4.1.3029.1.2");
ASN1.addObject(runtime, 92, "BF-ECB", "bf-ecb",null);
ASN1.addObject(runtime, 93, "BF-CFB", "bf-cfb",null);
ASN1.addObject(runtime, 94, "BF-OFB", "bf-ofb",null);
ASN1.addObject(runtime, 95, "MDC2", "mdc2","2.5.8.3.101");
ASN1.addObject(runtime, 96, "RSA-MDC2", "mdc2withRSA","2.5.8.3.100");
ASN1.addObject(runtime, 97, "RC4-40", "rc4-40",null);
ASN1.addObject(runtime, 98, "RC2-40-CBC", "rc2-40-cbc",null);
ASN1.addObject(runtime, 99, "G", "givenName","2.5.4.42");
ASN1.addObject(runtime, 100, "S", "surname","2.5.4.4");
ASN1.addObject(runtime, 101, "I", "initials","2.5.4.43");
ASN1.addObject(runtime, 102, "UID", "uniqueIdentifier","2.5.4.45");
ASN1.addObject(runtime, 103, "crlDistributionPoints", "X509v3 CRL Distribution Points","2.5.29.31");
ASN1.addObject(runtime, 104, "RSA-NP-MD5", "md5WithRSA","1.3.14.3.2.3");
ASN1.addObject(runtime, 105, "SN", "serialNumber","2.5.4.5");
ASN1.addObject(runtime, 106, "T", "title","2.5.4.12");
ASN1.addObject(runtime, 107, "D", "description","2.5.4.13");
ASN1.addObject(runtime, 108, "CAST5-CBC", "cast5-cbc","1.2.840.113533.7.66.10");
ASN1.addObject(runtime, 109, "CAST5-ECB", "cast5-ecb",null);
ASN1.addObject(runtime, 110, "CAST5-CFB", "cast5-cfb",null);
ASN1.addObject(runtime, 111, "CAST5-OFB", "cast5-ofb",null);
ASN1.addObject(runtime, 112, null, "pbeWithMD5AndCast5CBC","1.2.840.113533.7.66.12");
ASN1.addObject(runtime, 113, "DSA-SHA1", "dsaWithSHA1","1.2.840.10040.4.3");
ASN1.addObject(runtime, 114, "MD5-SHA1", "md5-sha1",null);
ASN1.addObject(runtime, 115, "RSA-SHA1-2", "sha1WithRSA","1.3.14.3.2.29");
ASN1.addObject(runtime, 116, "DSA", "dsaEncryption","1.2.840.10040.4.1");
ASN1.addObject(runtime, 117, "RIPEMD160", "ripemd160","1.3.36.3.2.1");
ASN1.addObject(runtime, 118, "RSA-RIPEMD160", "ripemd160WithRSA","1.3.36.3.3.1.2");
ASN1.addObject(runtime, 119, "RC5-CBC", "rc5-cbc","1.2.840.113549.3.8");
ASN1.addObject(runtime, 120, "RC5-ECB", "rc5-ecb",null);
ASN1.addObject(runtime, 121, "RC5-CFB", "rc5-cfb",null);
ASN1.addObject(runtime, 122, "RC5-OFB", "rc5-ofb",null);
ASN1.addObject(runtime, 123, "RLE", "run length compression","1.1.1.1.666.1");
ASN1.addObject(runtime, 124, "ZLIB", "zlib compression","1.1.1.1.666.2");
ASN1.addObject(runtime, 125, "extendedKeyUsage", "X509v3 Extended Key Usage","2.5.29.37");
ASN1.addObject(runtime, 126, "PKIX", null,"1.3.6.1.5.5.7");
ASN1.addObject(runtime, 127, "id-kp", null,"1.3.6.1.5.5.7.3");
ASN1.addObject(runtime, 128, "serverAuth", "TLS Web Server Authentication","1.3.6.1.5.5.7.3.1");
ASN1.addObject(runtime, 129, "clientAuth", "TLS Web Client Authentication","1.3.6.1.5.5.7.3.2");
ASN1.addObject(runtime, 130, "codeSigning", "Code Signing","1.3.6.1.5.5.7.3.3");
ASN1.addObject(runtime, 131, "emailProtection", "E-mail Protection","1.3.6.1.5.5.7.3.4");
ASN1.addObject(runtime, 132, "timeStamping", "Time Stamping","1.3.6.1.5.5.7.3.8");
ASN1.addObject(runtime, 133, "msCodeInd", "Microsoft Individual Code Signing","1.3.6.1.4.1.311.2.1.21");
ASN1.addObject(runtime, 134, "msCodeCom", "Microsoft Commercial Code Signing","1.3.6.1.4.1.311.2.1.22");
ASN1.addObject(runtime, 135, "msCTLSign", "Microsoft Trust List Signing","1.3.6.1.4.1.311.10.3.1");
ASN1.addObject(runtime, 136, "msSGC", "Microsoft Server Gated Crypto","1.3.6.1.4.1.311.10.3.3");
ASN1.addObject(runtime, 137, "msEFS", "Microsoft Encrypted File System","1.3.6.1.4.1.311.10.3.4");
ASN1.addObject(runtime, 138, "nsSGC", "Netscape Server Gated Crypto","2.16.840.1.113730.4.1");
ASN1.addObject(runtime, 139, "deltaCRL", "X509v3 Delta CRL Indicator","2.5.29.27");
ASN1.addObject(runtime, 140, "CRLReason", "CRL Reason Code","2.5.29.21");
ASN1.addObject(runtime, 141, "invalidityDate", "Invalidity Date","2.5.29.24");
ASN1.addObject(runtime, 142, "SXNetID", "Strong Extranet ID","1.3.101.1.4.1");
ASN1.addObject(runtime, 143, "PBE-SHA1-RC4-128", "pbeWithSHA1And128BitRC4","1.2.840.113549.1.12.1.1");
ASN1.addObject(runtime, 144, "PBE-SHA1-RC4-40", "pbeWithSHA1And40BitRC4","1.2.840.113549.1.12.1.2");
ASN1.addObject(runtime, 145, "PBE-SHA1-3DES", "pbeWithSHA1And3-KeyTripleDES-CBC","1.2.840.113549.1.12.1.3");
ASN1.addObject(runtime, 146, "PBE-SHA1-2DES", "pbeWithSHA1And2-KeyTripleDES-CBC","1.2.840.113549.1.12.1.4");
ASN1.addObject(runtime, 147, "PBE-SHA1-RC2-128", "pbeWithSHA1And128BitRC2-CBC","1.2.840.113549.1.12.1.5");
ASN1.addObject(runtime, 148, "PBE-SHA1-RC2-40", "pbeWithSHA1And40BitRC2-CBC","1.2.840.113549.1.12.1.6");
ASN1.addObject(runtime, 149, null, "keyBag","1.2.840.113549.1.12.10.1.1");
ASN1.addObject(runtime, 150, null, "pkcs8ShroudedKeyBag","1.2.840.113549.1.12.10.1.2");
ASN1.addObject(runtime, 151, null, "certBag","1.2.840.113549.1.12.10.1.3");
ASN1.addObject(runtime, 152, null, "crlBag","1.2.840.113549.1.12.10.1.4");
ASN1.addObject(runtime, 153, null, "secretBag","1.2.840.113549.1.12.10.1.5");
ASN1.addObject(runtime, 154, null, "safeContentsBag","1.2.840.113549.1.12.10.1.6");
ASN1.addObject(runtime, 155, null, "PBES2","1.2.840.113549.1.5.13");
ASN1.addObject(runtime, 156, null, "PBMAC1","1.2.840.113549.1.5.14");
ASN1.addObject(runtime, 157, null, "hmacWithSHA1","1.2.840.113549.2.7");
ASN1.addObject(runtime, 158, "id-qt-cps", "Policy Qualifier CPS","1.3.6.1.5.5.7.2.1");
ASN1.addObject(runtime, 159, "id-qt-unotice", "Policy Qualifier User Notice","1.3.6.1.5.5.7.2.2");
ASN1.addObject(runtime, 160, "RC2-64-CBC", "rc2-64-cbc",null);
ASN1.addObject(runtime, 161, "SMIME-CAPS", "S/MIME Capabilities","1.2.840.113549.1.9.15");
ASN1.addObject(runtime, 162, "PBE-MD2-RC2-64", "pbeWithMD2AndRC2-CBC","1.2.840.113549.1.5.4");
ASN1.addObject(runtime, 163, "PBE-MD5-RC2-64", "pbeWithMD5AndRC2-CBC","1.2.840.113549.1.5.6");
ASN1.addObject(runtime, 164, "PBE-SHA1-DES", "pbeWithSHA1AndDES-CBC","1.2.840.113549.1.5.10");
ASN1.addObject(runtime, 165, "msExtReq", "Microsoft Extension Request","1.3.6.1.4.1.311.2.1.14");
ASN1.addObject(runtime, 166, "extReq", "Extension Request","1.2.840.113549.1.9.14");
ASN1.addObject(runtime, 167, "name", "name","2.5.4.41");
ASN1.addObject(runtime, 168, "dnQualifier", "dnQualifier","2.5.4.46");
ASN1.addObject(runtime, 169, "id-pe", null,"1.3.6.1.5.5.7.1");
ASN1.addObject(runtime, 170, "id-ad", null,"1.3.6.1.5.5.7.48");
ASN1.addObject(runtime, 171, "authorityInfoAccess", "Authority Information Access","1.3.6.1.5.5.7.1.1");
ASN1.addObject(runtime, 172, "OCSP", "OCSP","1.3.6.1.5.5.7.48.1");
ASN1.addObject(runtime, 173, "caIssuers", "CA Issuers","1.3.6.1.5.5.7.48.2");
ASN1.addObject(runtime, 174, "OCSPSigning", "OCSP Signing","1.3.6.1.5.5.7.3.9");
    }

    public static interface KeyAndIv {
        byte[] getKey();
        byte[] getIv();
    }

    private static class KeyAndIvImpl implements KeyAndIv {
        private final byte[] key;
        private final byte[] iv;
        public KeyAndIvImpl(byte[] key, byte[] iv) {
            this.key = key;
            this.iv = iv;
        }
        public byte[] getKey() {
            return key;
        }
        public byte[] getIv() {
            return iv;
        }
    }

    private static Class pemHandlerImpl;
    public static PEMHandler getPEMHandler() {
        if(null == pemHandlerImpl) {
            try {
                Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
                pemHandlerImpl = Class.forName("org.jruby.ext.openssl.BouncyCastlePEMHandler");
            } catch(Exception e) {
                pemHandlerImpl = DefaultPEMHandler.class;
            }
        }
        try {
            return (PEMHandler)pemHandlerImpl.newInstance();
        } catch(Exception e) {}
        return null;
    }

    public static KeyAndIv EVP_BytesToKey(int key_len, int iv_len, MessageDigest md, byte[] salt, byte[] data, int count) {
        byte[] key = new byte[key_len];
        byte[]  iv = new byte[iv_len];
        int key_ix = 0;
        int iv_ix = 0;
        byte[] md_buf = null;
        int nkey = key_len;
        int niv = iv_len;
        int i = 0;
        if(data == null) {
            return new KeyAndIvImpl(key,iv);
        }
        int addmd = 0;
        for(;;) {
            md.reset();
            if(addmd++ > 0) {
                md.update(md_buf);
            }
            md.update(data);
            if(null != salt) {
                md.update(salt,0,8);
            }
            md_buf = md.digest();
            for(i=1;i<count;i++) {
                md.reset();
                md.update(md_buf);
                md_buf = md.digest();
            }
            i=0;
            if(nkey > 0) {
                for(;;) {
                    if(nkey == 0) break;
                    if(i == md_buf.length) break;
                    key[key_ix++] = md_buf[i];
                    nkey--;
                    i++;
                }
            }
            if(niv > 0 && i != md_buf.length) {
                for(;;) {
                    if(niv == 0) break;
                    if(i == md_buf.length) break;
                    iv[iv_ix++] = md_buf[i];
                    niv--;
                    i++;
                }
            }
            if(nkey == 0 && niv == 0) {
                break;
            }
        }
        for(i=0;i<md_buf.length;i++) {
            md_buf[i] = 0;
        }
        return new KeyAndIvImpl(key,iv);
    }
}// OpenSSLImpl
