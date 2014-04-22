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
 * Copyright (C) 2006, 2007 Ola Bini <ola@ologix.com>
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
package org.jruby.ext.openssl;

import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.IdentityHashMap;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERBoolean;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTCTime;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x509.X509Name;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.RubyTime;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Visibility;
import org.jruby.util.ByteList;

import static org.jruby.ext.openssl.OpenSSLReal.debug;
import static org.jruby.ext.openssl.OpenSSLReal.debugStackTrace;
import static org.jruby.ext.openssl.OpenSSLReal.isDebug;
import static org.jruby.ext.openssl.OpenSSLReal.warn;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class ASN1 {

    private static Map<Ruby, Map<String, ASN1ObjectIdentifier>> SYM_TO_OID = new IdentityHashMap<Ruby, Map<String, ASN1ObjectIdentifier>>();
    private static Map<Ruby, Map<ASN1ObjectIdentifier, String>> OID_TO_SYM = new IdentityHashMap<Ruby, Map<ASN1ObjectIdentifier, String>>();
    private static Map<Ruby, Map<ASN1ObjectIdentifier, Integer>> OID_TO_NID = new IdentityHashMap<Ruby, Map<ASN1ObjectIdentifier, Integer>>();
    private static Map<Ruby, Map<Integer, ASN1ObjectIdentifier>> NID_TO_OID = new IdentityHashMap<Ruby, Map<Integer, ASN1ObjectIdentifier>>();
    private static Map<Ruby, Map<Integer, String>> NID_TO_SN = new IdentityHashMap<Ruby, Map<Integer, String>>();
    private static Map<Ruby, Map<Integer, String>> NID_TO_LN = new IdentityHashMap<Ruby, Map<Integer, String>>();

    @SuppressWarnings("unchecked")
    private static synchronized void initMaps(final Ruby runtime) {
        SYM_TO_OID.put(runtime, new HashMap<String, ASN1ObjectIdentifier>(X509Name.DefaultLookUp));
        OID_TO_SYM.put(runtime, new HashMap<ASN1ObjectIdentifier, String>(X509Name.DefaultSymbols));
        OID_TO_NID.put(runtime, new HashMap<ASN1ObjectIdentifier, Integer>());
        NID_TO_OID.put(runtime, new HashMap<Integer, ASN1ObjectIdentifier>());
        NID_TO_SN.put(runtime, new HashMap<Integer, String>());
        NID_TO_LN.put(runtime, new HashMap<Integer, String>());

        defaultObjects(runtime);
    }

    private static void defaultObjects(final Ruby runtime) {
        addObject(runtime, 0, null, null,"1.2.840.113549.1.12.1");
        addObject(runtime, 1, null, "rsadsi","1.2.840.113549");
        addObject(runtime, 2, null, "pkcs","1.2.840.113549.1");
        addObject(runtime, 3, "MD2", "md2","1.2.840.113549.2.2");
        addObject(runtime, 4, "MD5", "md5","1.2.840.113549.2.5");
        addObject(runtime, 5, "RC4", "rc4","1.2.840.113549.3.4");
        addObject(runtime, 6, null, "rsaEncryption","1.2.840.113549.1.1.1");
        addObject(runtime, 7, "RSA-MD2", "md2WithRSAEncryption","1.2.840.113549.1.1.2");
        addObject(runtime, 8, "RSA-MD5", "md5WithRSAEncryption","1.2.840.113549.1.1.4");
        addObject(runtime, 9, "PBE-MD2-DES", "pbeWithMD2AndDES-CBC","1.2.840.113549.1.5.1");
        addObject(runtime, 10, "PBE-MD5-DES", "pbeWithMD5AndDES-CBC","1.2.840.113549.1.5.3");
        addObject(runtime, 11, null, "X500","2.5");
        addObject(runtime, 12, null, "X509","2.5.4");
        addObject(runtime, 13, "CN", "commonName","2.5.4.3");
        addObject(runtime, 14, "C", "countryName","2.5.4.6");
        addObject(runtime, 15, "L", "localityName","2.5.4.7");
        addObject(runtime, 16, "ST", "stateOrProvinceName","2.5.4.8");
        addObject(runtime, 17, "O", "organizationName","2.5.4.10");
        addObject(runtime, 18, "OU", "organizationalUnitName","2.5.4.11");
        addObject(runtime, 19, "RSA", "rsa","2.5.8.1.1");
        addObject(runtime, 20, null, "pkcs7","1.2.840.113549.1.7");
        addObject(runtime, org.jruby.ext.openssl.impl.ASN1Registry.NID_pkcs7_data, null, "pkcs7-data","1.2.840.113549.1.7.1");
        addObject(runtime, org.jruby.ext.openssl.impl.ASN1Registry.NID_pkcs7_signed, null, "pkcs7-signedData","1.2.840.113549.1.7.2");
        addObject(runtime, org.jruby.ext.openssl.impl.ASN1Registry.NID_pkcs7_enveloped, null, "pkcs7-envelopedData","1.2.840.113549.1.7.3");
        addObject(runtime, org.jruby.ext.openssl.impl.ASN1Registry.NID_pkcs7_signedAndEnveloped, null, "pkcs7-signedAndEnvelopedData","1.2.840.113549.1.7.4");
        addObject(runtime, org.jruby.ext.openssl.impl.ASN1Registry.NID_pkcs7_digest, null, "pkcs7-digestData","1.2.840.113549.1.7.5");
        addObject(runtime, org.jruby.ext.openssl.impl.ASN1Registry.NID_pkcs7_encrypted, null, "pkcs7-encryptedData","1.2.840.113549.1.7.6");
        addObject(runtime, 27, null, "pkcs3","1.2.840.113549.1.3");
        addObject(runtime, 28, null, "dhKeyAgreement","1.2.840.113549.1.3.1");
        addObject(runtime, 29, "DES-ECB", "des-ecb","1.3.14.3.2.6");
        addObject(runtime, 30, "DES-CFB", "des-cfb","1.3.14.3.2.9");
        addObject(runtime, 31, "DES-CBC", "des-cbc","1.3.14.3.2.7");
        addObject(runtime, 32, "DES-EDE", "des-ede","1.3.14.3.2.17");
        addObject(runtime, 33, "DES-EDE3", "des-ede3",null);
        addObject(runtime, 34, "IDEA-CBC", "idea-cbc","1.3.6.1.4.1.188.7.1.1.2");
        addObject(runtime, 35, "IDEA-CFB", "idea-cfb",null);
        addObject(runtime, 36, "IDEA-ECB", "idea-ecb",null);
        addObject(runtime, 37, "RC2-CBC", "rc2-cbc","1.2.840.113549.3.2");
        addObject(runtime, 38, "RC2-ECB", "rc2-ecb",null);
        addObject(runtime, 39, "RC2-CFB", "rc2-cfb",null);
        addObject(runtime, 40, "RC2-OFB", "rc2-ofb",null);
        addObject(runtime, 41, "SHA", "sha","1.3.14.3.2.18");
        addObject(runtime, 42, "RSA-SHA", "shaWithRSAEncryption","1.3.14.3.2.15");
        addObject(runtime, 43, "DES-EDE-CBC", "des-ede-cbc",null);
        addObject(runtime, 44, "DES-EDE3-CBC", "des-ede3-cbc","1.2.840.113549.3.7");
        addObject(runtime, 45, "DES-OFB", "des-ofb","1.3.14.3.2.8");
        addObject(runtime, 46, "IDEA-OFB", "idea-ofb",null);
        addObject(runtime, 47, null, "pkcs9","1.2.840.113549.1.9");
        addObject(runtime, 48, "Email", "emailAddress","1.2.840.113549.1.9.1");
        addObject(runtime, 49, null, "unstructuredName","1.2.840.113549.1.9.2");
        addObject(runtime, 50, null, "contentType","1.2.840.113549.1.9.3");
        addObject(runtime, 51, null, "messageDigest","1.2.840.113549.1.9.4");
        addObject(runtime, 52, null, "signingTime","1.2.840.113549.1.9.5");
        addObject(runtime, 53, null, "countersignature","1.2.840.113549.1.9.6");
        addObject(runtime, 54, null, "challengePassword","1.2.840.113549.1.9.7");
        addObject(runtime, 55, null, "unstructuredAddress","1.2.840.113549.1.9.8");
        addObject(runtime, 56, null, "extendedCertificateAttributes","1.2.840.113549.1.9.9");
        addObject(runtime, 57, "Netscape", "Netscape Communications Corp.","2.16.840.1.113730");
        addObject(runtime, 58, "nsCertExt", "Netscape Certificate Extension","2.16.840.1.113730.1");
        addObject(runtime, 59, "nsDataType", "Netscape Data Type","2.16.840.1.113730.2");
        addObject(runtime, 60, "DES-EDE-CFB", "des-ede-cfb",null);
        addObject(runtime, 61, "DES-EDE3-CFB", "des-ede3-cfb",null);
        addObject(runtime, 62, "DES-EDE-OFB", "des-ede-ofb",null);
        addObject(runtime, 63, "DES-EDE3-OFB", "des-ede3-ofb",null);
        addObject(runtime, 64, "SHA1", "sha1","1.3.14.3.2.26");
        addObject(runtime, 65, "RSA-SHA1", "sha1WithRSAEncryption","1.2.840.113549.1.1.5");
        addObject(runtime, 66, "DSA-SHA", "dsaWithSHA","1.3.14.3.2.13");
        addObject(runtime, 67, "DSA-old", "dsaEncryption-old","1.3.14.3.2.12");
        addObject(runtime, 68, "PBE-SHA1-RC2-64", "pbeWithSHA1AndRC2-CBC","1.2.840.113549.1.5.11");
        addObject(runtime, 69, null, "PBKDF2","1.2.840.113549.1.5.12");
        addObject(runtime, 70, "DSA-SHA1-old", "dsaWithSHA1-old","1.3.14.3.2.27");
        addObject(runtime, 71, "nsCertType", "Netscape Cert Type","2.16.840.1.113730.1.1");
        addObject(runtime, 72, "nsBaseUrl", "Netscape Base Url","2.16.840.1.113730.1.2");
        addObject(runtime, 73, "nsRevocationUrl", "Netscape Revocation Url","2.16.840.1.113730.1.3");
        addObject(runtime, 74, "nsCaRevocationUrl", "Netscape CA Revocation Url","2.16.840.1.113730.1.4");
        addObject(runtime, 75, "nsRenewalUrl", "Netscape Renewal Url","2.16.840.1.113730.1.7");
        addObject(runtime, 76, "nsCaPolicyUrl", "Netscape CA Policy Url","2.16.840.1.113730.1.8");
        addObject(runtime, 77, "nsSslServerName", "Netscape SSL Server Name","2.16.840.1.113730.1.12");
        addObject(runtime, 78, "nsComment", "Netscape Comment","2.16.840.1.113730.1.13");
        addObject(runtime, 79, "nsCertSequence", "Netscape Certificate Sequence","2.16.840.1.113730.2.5");
        addObject(runtime, 80, "DESX-CBC", "desx-cbc",null);
        addObject(runtime, 81, "id-ce", null,"2.5.29");
        addObject(runtime, 82, "subjectKeyIdentifier", "X509v3 Subject Key Identifier","2.5.29.14");
        addObject(runtime, 83, "keyUsage", "X509v3 Key Usage","2.5.29.15");
        addObject(runtime, 84, "privateKeyUsagePeriod", "X509v3 Private Key Usage Period","2.5.29.16");
        addObject(runtime, 85, "subjectAltName", "X509v3 Subject Alternative Name","2.5.29.17");
        addObject(runtime, 86, "issuerAltName", "X509v3 Issuer Alternative Name","2.5.29.18");
        addObject(runtime, 87, "basicConstraints", "X509v3 Basic Constraints","2.5.29.19");
        addObject(runtime, 88, "crlNumber", "X509v3 CRL Number","2.5.29.20");
        addObject(runtime, 89, "certificatePolicies", "X509v3 Certificate Policies","2.5.29.32");
        addObject(runtime, 90, "authorityKeyIdentifier", "X509v3 Authority Key Identifier","2.5.29.35");
        addObject(runtime, 91, "BF-CBC", "bf-cbc","1.3.6.1.4.1.3029.1.2");
        addObject(runtime, 92, "BF-ECB", "bf-ecb",null);
        addObject(runtime, 93, "BF-CFB", "bf-cfb",null);
        addObject(runtime, 94, "BF-OFB", "bf-ofb",null);
        addObject(runtime, 95, "MDC2", "mdc2","2.5.8.3.101");
        addObject(runtime, 96, "RSA-MDC2", "mdc2withRSA","2.5.8.3.100");
        addObject(runtime, 97, "RC4-40", "rc4-40",null);
        addObject(runtime, 98, "RC2-40-CBC", "rc2-40-cbc",null);
        addObject(runtime, 99, "G", "givenName","2.5.4.42");
        addObject(runtime, 100, "S", "surname","2.5.4.4");
        addObject(runtime, 101, "I", "initials","2.5.4.43");
        addObject(runtime, 102, "UID", "uniqueIdentifier","2.5.4.45");
        addObject(runtime, 103, "crlDistributionPoints", "X509v3 CRL Distribution Points","2.5.29.31");
        addObject(runtime, 104, "RSA-NP-MD5", "md5WithRSA","1.3.14.3.2.3");
        addObject(runtime, 105, "SN", "serialNumber","2.5.4.5");
        addObject(runtime, 106, "T", "title","2.5.4.12");
        addObject(runtime, 107, "D", "description","2.5.4.13");
        addObject(runtime, 108, "CAST5-CBC", "cast5-cbc","1.2.840.113533.7.66.10");
        addObject(runtime, 109, "CAST5-ECB", "cast5-ecb",null);
        addObject(runtime, 110, "CAST5-CFB", "cast5-cfb",null);
        addObject(runtime, 111, "CAST5-OFB", "cast5-ofb",null);
        addObject(runtime, 112, null, "pbeWithMD5AndCast5CBC","1.2.840.113533.7.66.12");
        addObject(runtime, 113, "DSA-SHA1", "dsaWithSHA1","1.2.840.10040.4.3");
        addObject(runtime, 114, "MD5-SHA1", "md5-sha1",null);
        addObject(runtime, 115, "RSA-SHA1-2", "sha1WithRSA","1.3.14.3.2.29");
        addObject(runtime, 116, "DSA", "dsaEncryption","1.2.840.10040.4.1");
        addObject(runtime, 117, "RIPEMD160", "ripemd160","1.3.36.3.2.1");
        addObject(runtime, 118, "RSA-RIPEMD160", "ripemd160WithRSA","1.3.36.3.3.1.2");
        addObject(runtime, 119, "RC5-CBC", "rc5-cbc","1.2.840.113549.3.8");
        addObject(runtime, 120, "RC5-ECB", "rc5-ecb",null);
        addObject(runtime, 121, "RC5-CFB", "rc5-cfb",null);
        addObject(runtime, 122, "RC5-OFB", "rc5-ofb",null);
        addObject(runtime, 123, "RLE", "run length compression","1.1.1.1.666.1");
        addObject(runtime, 124, "ZLIB", "zlib compression","1.1.1.1.666.2");
        addObject(runtime, 125, "extendedKeyUsage", "X509v3 Extended Key Usage","2.5.29.37");
        addObject(runtime, 126, "PKIX", null,"1.3.6.1.5.5.7");
        addObject(runtime, 127, "id-kp", null,"1.3.6.1.5.5.7.3");
        addObject(runtime, 128, "serverAuth", "TLS Web Server Authentication","1.3.6.1.5.5.7.3.1");
        addObject(runtime, 129, "clientAuth", "TLS Web Client Authentication","1.3.6.1.5.5.7.3.2");
        addObject(runtime, 130, "codeSigning", "Code Signing","1.3.6.1.5.5.7.3.3");
        addObject(runtime, 131, "emailProtection", "E-mail Protection","1.3.6.1.5.5.7.3.4");
        addObject(runtime, 132, "timeStamping", "Time Stamping","1.3.6.1.5.5.7.3.8");
        addObject(runtime, 133, "msCodeInd", "Microsoft Individual Code Signing","1.3.6.1.4.1.311.2.1.21");
        addObject(runtime, 134, "msCodeCom", "Microsoft Commercial Code Signing","1.3.6.1.4.1.311.2.1.22");
        addObject(runtime, 135, "msCTLSign", "Microsoft Trust List Signing","1.3.6.1.4.1.311.10.3.1");
        addObject(runtime, 136, "msSGC", "Microsoft Server Gated Crypto","1.3.6.1.4.1.311.10.3.3");
        addObject(runtime, 137, "msEFS", "Microsoft Encrypted File System","1.3.6.1.4.1.311.10.3.4");
        addObject(runtime, 138, "nsSGC", "Netscape Server Gated Crypto","2.16.840.1.113730.4.1");
        addObject(runtime, 139, "deltaCRL", "X509v3 Delta CRL Indicator","2.5.29.27");
        addObject(runtime, 140, "CRLReason", "CRL Reason Code","2.5.29.21");
        addObject(runtime, 141, "invalidityDate", "Invalidity Date","2.5.29.24");
        addObject(runtime, 142, "SXNetID", "Strong Extranet ID","1.3.101.1.4.1");
        addObject(runtime, 143, "PBE-SHA1-RC4-128", "pbeWithSHA1And128BitRC4","1.2.840.113549.1.12.1.1");
        addObject(runtime, 144, "PBE-SHA1-RC4-40", "pbeWithSHA1And40BitRC4","1.2.840.113549.1.12.1.2");
        addObject(runtime, 145, "PBE-SHA1-3DES", "pbeWithSHA1And3-KeyTripleDES-CBC","1.2.840.113549.1.12.1.3");
        addObject(runtime, 146, "PBE-SHA1-2DES", "pbeWithSHA1And2-KeyTripleDES-CBC","1.2.840.113549.1.12.1.4");
        addObject(runtime, 147, "PBE-SHA1-RC2-128", "pbeWithSHA1And128BitRC2-CBC","1.2.840.113549.1.12.1.5");
        addObject(runtime, 148, "PBE-SHA1-RC2-40", "pbeWithSHA1And40BitRC2-CBC","1.2.840.113549.1.12.1.6");
        addObject(runtime, 149, null, "keyBag","1.2.840.113549.1.12.10.1.1");
        addObject(runtime, 150, null, "pkcs8ShroudedKeyBag","1.2.840.113549.1.12.10.1.2");
        addObject(runtime, 151, null, "certBag","1.2.840.113549.1.12.10.1.3");
        addObject(runtime, 152, null, "crlBag","1.2.840.113549.1.12.10.1.4");
        addObject(runtime, 153, null, "secretBag","1.2.840.113549.1.12.10.1.5");
        addObject(runtime, 154, null, "safeContentsBag","1.2.840.113549.1.12.10.1.6");
        addObject(runtime, 155, null, "PBES2","1.2.840.113549.1.5.13");
        addObject(runtime, 156, null, "PBMAC1","1.2.840.113549.1.5.14");
        addObject(runtime, 157, null, "hmacWithSHA1","1.2.840.113549.2.7");
        addObject(runtime, 158, "id-qt-cps", "Policy Qualifier CPS","1.3.6.1.5.5.7.2.1");
        addObject(runtime, 159, "id-qt-unotice", "Policy Qualifier User Notice","1.3.6.1.5.5.7.2.2");
        addObject(runtime, 160, "RC2-64-CBC", "rc2-64-cbc",null);
        addObject(runtime, 161, "SMIME-CAPS", "S/MIME Capabilities","1.2.840.113549.1.9.15");
        addObject(runtime, 162, "PBE-MD2-RC2-64", "pbeWithMD2AndRC2-CBC","1.2.840.113549.1.5.4");
        addObject(runtime, 163, "PBE-MD5-RC2-64", "pbeWithMD5AndRC2-CBC","1.2.840.113549.1.5.6");
        addObject(runtime, 164, "PBE-SHA1-DES", "pbeWithSHA1AndDES-CBC","1.2.840.113549.1.5.10");
        addObject(runtime, 165, "msExtReq", "Microsoft Extension Request","1.3.6.1.4.1.311.2.1.14");
        addObject(runtime, 166, "extReq", "Extension Request","1.2.840.113549.1.9.14");
        addObject(runtime, 167, "name", "name","2.5.4.41");
        addObject(runtime, 168, "dnQualifier", "dnQualifier","2.5.4.46");
        addObject(runtime, 169, "id-pe", null,"1.3.6.1.5.5.7.1");
        addObject(runtime, 170, "id-ad", null,"1.3.6.1.5.5.7.48");
        addObject(runtime, 171, "authorityInfoAccess", "Authority Information Access","1.3.6.1.5.5.7.1.1");
        addObject(runtime, 172, "OCSP", "OCSP","1.3.6.1.5.5.7.48.1");
        addObject(runtime, 173, "caIssuers", "CA Issuers","1.3.6.1.5.5.7.48.2");
        addObject(runtime, 174, "OCSPSigning", "OCSP Signing","1.3.6.1.5.5.7.3.9");
        addObject(runtime, 175, "AES-128-EBC", "aes-128-ebc","2.16.840.1.101.3.4.1.1");
        addObject(runtime, 176, "AES-128-CBC", "aes-128-cbc","2.16.840.1.101.3.4.1.2");
        addObject(runtime, 177, "AES-128-OFB", "aes-128-ofb","2.16.840.1.101.3.4.1.3");
        addObject(runtime, 178, "AES-128-CFB", "aes-128-cfb","2.16.840.1.101.3.4.1.4");
        addObject(runtime, 179, "AES-192-EBC", "aes-192-ebc","2.16.840.1.101.3.4.1.21");
        addObject(runtime, 180, "AES-192-CBC", "aes-192-cbc","2.16.840.1.101.3.4.1.22");
        addObject(runtime, 181, "AES-192-OFB", "aes-192-ofb","2.16.840.1.101.3.4.1.23");
        addObject(runtime, 182, "AES-192-CFB", "aes-192-cfb","2.16.840.1.101.3.4.1.24");
        addObject(runtime, 183, "AES-256-EBC", "aes-256-ebc","2.16.840.1.101.3.4.1.41");
        addObject(runtime, 184, "AES-256-CBC", "aes-256-cbc","2.16.840.1.101.3.4.1.42");
        addObject(runtime, 185, "AES-256-OFB", "aes-256-ofb","2.16.840.1.101.3.4.1.43");
        addObject(runtime, 186, "AES-256-CFB", "aes-256-cfb","2.16.840.1.101.3.4.1.44");
    }

    private static void addObject(final Ruby runtime, final int nid,
        final String sn, final String ln, final String oid) {
        if ( oid != null && ( sn != null || ln != null ) ) {

            ASN1ObjectIdentifier objectId = new ASN1ObjectIdentifier(oid);

            if ( sn != null ) {
                symToOid(runtime).put(sn.toLowerCase(), objectId);
            }
            if ( ln != null ) {
                symToOid(runtime).put(ln.toLowerCase(), objectId);
            }

            oidToSym(runtime).put(objectId, sn == null ? ln : sn);
            oidToNid(runtime).put(objectId, nid);
            nidToOid(runtime).put(nid, objectId);
            nidToSn(runtime).put(nid, sn);
            nidToLn(runtime).put(nid, ln);
        }
    }

    private static Map<String, ASN1ObjectIdentifier> symToOid(final Ruby runtime) {
        Map<String, ASN1ObjectIdentifier> map = SYM_TO_OID.get(runtime);
        if ( map == null ) {
            synchronized(ASN1.class) {
                map = SYM_TO_OID.get(runtime);
                if ( map == null ) {
                    initMaps(runtime);
                    map = SYM_TO_OID.get(runtime);
                }
            }
        }
        return map;
    }

    private static Map<ASN1ObjectIdentifier, String> oidToSym(final Ruby runtime) {
        Map<ASN1ObjectIdentifier, String> map = OID_TO_SYM.get(runtime);
        if ( map == null ) {
            synchronized(ASN1.class) {
                map = OID_TO_SYM.get(runtime);
                if ( map == null ) {
                    initMaps(runtime);
                    map = OID_TO_SYM.get(runtime);
                }
            }
        }
        return map;
    }

    private static Map<Integer, ASN1ObjectIdentifier> nidToOid(final Ruby runtime) {
        Map<Integer, ASN1ObjectIdentifier> map = NID_TO_OID.get(runtime);
        if ( map == null ) {
            synchronized(ASN1.class) {
                map = NID_TO_OID.get(runtime);
                if ( map == null ) {
                    initMaps(runtime);
                    map = NID_TO_OID.get(runtime);
                }
            }
        }
        return map;
    }

    private static Map<ASN1ObjectIdentifier, Integer> oidToNid(final Ruby runtime) {
        Map<ASN1ObjectIdentifier, Integer> map = OID_TO_NID.get(runtime);
        if ( map == null ) {
            synchronized(ASN1.class) {
                map = OID_TO_NID.get(runtime);
                if ( map == null ) {
                    initMaps(runtime);
                    map = OID_TO_NID.get(runtime);
                }
            }
        }
        return map;
    }

    private static Map<Integer, String> nidToSn(final Ruby runtime) {
        Map<Integer, String> map = NID_TO_SN.get(runtime);
        if ( map == null ) {
            synchronized(ASN1.class) {
                map = NID_TO_SN.get(runtime);
                if ( map == null ) {
                    initMaps(runtime);
                    map = NID_TO_SN.get(runtime);
                }
            }
        }
        return map;
    }

    private static Map<Integer, String> nidToLn(final Ruby runtime) {
        Map<Integer, String> map = NID_TO_LN.get(runtime);
        if ( map == null ) {
            synchronized(ASN1.class) {
                map = NID_TO_LN.get(runtime);
                if ( map == null ) {
                    initMaps(runtime);
                    map = NID_TO_LN.get(runtime);
                }
            }
        }
        return map;
    }

    static String ln2oid(final Ruby runtime, final String ln) {
        Map<String, ASN1ObjectIdentifier> map = symToOid(runtime);
        final ASN1ObjectIdentifier val = map.get(ln);
        if ( val == null ) {
            throw new NullPointerException("oid not found for ln = '" + ln + "' (" + runtime + ")");
        }
        return val.getId();
    }

    static Integer obj2nid(Ruby runtime, final ASN1ObjectIdentifier oid) {
        return oidToNid(runtime).get(oid);
    }

    static String o2a(final Ruby runtime, final ASN1ObjectIdentifier oid) {
        final Integer nid = obj2nid(runtime, oid);
        if ( nid == null ) {
            throw new NullPointerException("nid not found for oid = '" + oid + "' (" + runtime + ")");
        }
        String one = nidToLn(runtime).get(nid);
        if (one == null) {
            one = nidToSn(runtime).get(nid);
        }
        return one;
    }

    static String nid2ln(final Ruby runtime, final int nid) {
        return nidToLn(runtime).get(nid);
    }

    static Map<String, ASN1ObjectIdentifier> getOIDLookup(final Ruby runtime) {
        return symToOid(runtime);
    }

    static Map<ASN1ObjectIdentifier, String> getSymLookup(final Ruby runtime) {
        return oidToSym(runtime);
    }

    private final static Object[][] ASN1_INFO = {
        {"EOC", null, null },
        {"BOOLEAN", org.bouncycastle.asn1.DERBoolean.class, "Boolean" },
        {"INTEGER", org.bouncycastle.asn1.ASN1Integer.class, "Integer" },
        {"BIT_STRING",  org.bouncycastle.asn1.DERBitString.class, "BitString" },
        {"OCTET_STRING",  org.bouncycastle.asn1.DEROctetString.class, "OctetString" },
        {"NULL",  org.bouncycastle.asn1.DERNull.class, "Null" },
        {"OBJECT",  org.bouncycastle.asn1.ASN1ObjectIdentifier.class, "ObjectId" },
        {"OBJECT_DESCRIPTOR",  null, null },
        {"EXTERNAL",  null, null },
        {"REAL",  null, null },
        {"ENUMERATED",  org.bouncycastle.asn1.DEREnumerated.class, "Enumerated" },
        {"EMBEDDED_PDV",  null, null },
        {"UTF8STRING",  org.bouncycastle.asn1.DERUTF8String.class, "UTF8String" },
        {"RELATIVE_OID",  null, null },
        {"[UNIVERSAL 14]",  null, null },
        {"[UNIVERSAL 15]",  null, null },
        {"SEQUENCE",  org.bouncycastle.asn1.DLSequence.class, "Sequence" },
        {"SET",  org.bouncycastle.asn1.DLSet.class, "Set" },
        {"NUMERICSTRING",  org.bouncycastle.asn1.DERNumericString.class, "NumericString" },
        {"PRINTABLESTRING",  org.bouncycastle.asn1.DERPrintableString.class, "PrintableString" },
        {"T61STRING",  org.bouncycastle.asn1.DERT61String.class, "T61String" },
        {"VIDEOTEXSTRING", null, null },
        {"IA5STRING",  org.bouncycastle.asn1.DERIA5String.class, "IA5String" },
        {"UTCTIME",  org.bouncycastle.asn1.DERUTCTime.class, "UTCTime" },
        {"GENERALIZEDTIME",  org.bouncycastle.asn1.DERGeneralizedTime.class, "GeneralizedTime" },
        {"GRAPHICSTRING",  null, null },
        {"ISO64STRING",  null, null },
        {"GENERALSTRING",  org.bouncycastle.asn1.DERGeneralString.class, "GeneralString" },
        {"UNIVERSALSTRING",  org.bouncycastle.asn1.DERUniversalString.class, "UniversalString" },
        {"CHARACTER_STRING",  null, null },
        {"BMPSTRING", org.bouncycastle.asn1.DERBMPString.class, "BMPString" }};

    private final static Map<Class, Integer> CLASS_TO_ID = new HashMap<Class, Integer>();
    private final static Map<String, Integer> RUBYNAME_TO_ID = new HashMap<String, Integer>();

    static {
        for ( int i = 0; i < ASN1_INFO.length; i++ ) {
            final Object[] info = ASN1_INFO[i];
            if ( info[1] != null ) {
                CLASS_TO_ID.put((Class) info[1], Integer.valueOf(i));
            }
            if ( info[2] != null ) {
                RUBYNAME_TO_ID.put((String) info[2], Integer.valueOf(i));
            }
        }
    }

    static int idForClass(Class type) {
        Integer v = null;
        while ( type != Object.class && v == null ) {
            v = CLASS_TO_ID.get(type);
            if ( v == null ) type = type.getSuperclass();
        }
        return v == null ? -1 : v.intValue();
    }

    static int idForRubyName(String name) {
        Integer v = RUBYNAME_TO_ID.get(name);
        return v == null ? -1 : v.intValue();
    }

    static Class<? extends ASN1Encodable> classForId(int id) {
        @SuppressWarnings("unchecked")
        Class<? extends ASN1Encodable> result = (Class<? extends ASN1Encodable>)(ASN1_INFO[id][1]);
        return result;
    }

    public static void createASN1(Ruby runtime, RubyModule ossl) {
        final RubyModule _ASN1 = ossl.defineModuleUnder("ASN1");
        final RubyClass _OpenSSLError = ossl.getClass("OpenSSLError");
        _ASN1.defineClassUnder("ASN1Error", _OpenSSLError, _OpenSSLError.getAllocator());

        _ASN1.defineAnnotatedMethods(ASN1.class);

        final RubyArray _UNIVERSAL_TAG_NAME = runtime.newArray();
        _ASN1.setConstant("UNIVERSAL_TAG_NAME", _UNIVERSAL_TAG_NAME);

        for ( int i = 0; i < ASN1_INFO.length; i++ ) {
            final String name = (String) ASN1_INFO[i][0];
            if ( name.charAt(0) != '[' ) {
                _UNIVERSAL_TAG_NAME.append( runtime.newString(name) );
                _ASN1.setConstant( name, runtime.newFixnum(i) );
            } else {
                _UNIVERSAL_TAG_NAME.append( runtime.getNil() );
            }
        }

        final ThreadContext context = runtime.getCurrentContext();
        RubyClass _ASN1Data = _ASN1.defineClassUnder("ASN1Data", runtime.getObject(), ASN1Data.ALLOCATOR);
        _ASN1Data.addReadWriteAttribute(context, "value");
        _ASN1Data.addReadWriteAttribute(context, "tag");
        _ASN1Data.addReadWriteAttribute(context, "tag_class");
        _ASN1Data.defineAnnotatedMethods(ASN1Data.class);

        final ObjectAllocator primitiveAllocator = ASN1Primitive.ALLOCATOR;
        RubyClass _Primitive = _ASN1.defineClassUnder("Primitive", _ASN1Data, primitiveAllocator);
        _Primitive.addReadWriteAttribute(context, "tagging");
        _Primitive.defineAnnotatedMethods(ASN1Primitive.class);

        RubyClass _Constructive = _ASN1.defineClassUnder("Constructive", _ASN1Data, ASN1Constructive.ALLOCATOR);
        _Constructive.includeModule( runtime.getModule("Enumerable") );
        _Constructive.addReadWriteAttribute(context, "tagging");
        _Constructive.defineAnnotatedMethods(ASN1Constructive.class);

        _ASN1.defineClassUnder("Boolean", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("Integer", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("Enumerated", _Primitive, primitiveAllocator);

        RubyClass _BitString = _ASN1.defineClassUnder("BitString", _Primitive, primitiveAllocator);

        _ASN1.defineClassUnder("OctetString", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("UTF8String", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("NumericString", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("PrintableString", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("T61String", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("VideotexString", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("IA5String", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("GraphicString", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("ISO64String", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("GeneralString", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("UniversalString", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("BMPString", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("Null", _Primitive, primitiveAllocator);

        RubyClass _ObjectId = _ASN1.defineClassUnder("ObjectId", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("UTCTime", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("GeneralizedTime", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("Sequence", _Constructive, _Constructive.getAllocator());
        _ASN1.defineClassUnder("Set", _Constructive, _Constructive.getAllocator());

        _ObjectId.defineAnnotatedMethods(ObjectId.class);

        _BitString.addReadWriteAttribute(context, "unused_bits");
    }


    private static String getShortNameFor(Ruby runtime, String nameOrOid) {
        return getNameFor(runtime, nameOrOid, true);
    }

    private static String getLongNameFor(Ruby runtime, String nameOrOid) {
        return getNameFor(runtime, nameOrOid, false);
    }

    private static String getNameFor(final Ruby runtime, final String nameOrOid, final boolean shortName) {
        ASN1ObjectIdentifier oid = getObjectIdentifier(runtime, nameOrOid);
        Map<String, ASN1ObjectIdentifier> lookup = getOIDLookup(runtime);
        String name = null;
        for ( final String key : lookup.keySet() ) {
            if ( oid.equals( lookup.get(key) ) ) {
                if ( name == null ||
                ( shortName ? key.length() < name.length() : key.length() > name.length() ) ) {
                    name = key;
                }
            }
        }
        return name;
    }

    static ASN1ObjectIdentifier getObjectIdentifier(final Ruby runtime, final String nameOrOid)
        throws IllegalArgumentException {
        Object val1 = getOIDLookup(runtime).get( nameOrOid.toLowerCase() );
        if ( val1 != null ) return (ASN1ObjectIdentifier) val1;
        return new ASN1ObjectIdentifier(nameOrOid);
    }

    @JRubyMethod(name="Boolean", module=true, rest=true)
    public static IRubyObject fact_Boolean(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "Boolean", args);
    }

    @JRubyMethod(name="Integer", module=true, rest=true)
    public static IRubyObject fact_Integer(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "Integer", args);
    }

    @JRubyMethod(name="Enumerated", module=true, rest=true)
    public static IRubyObject fact_Enumerated(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "Enumerated", args);
    }

    @JRubyMethod(name="BitString", module=true, rest=true)
    public static IRubyObject fact_BitString(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "BitString", args);
    }

    @JRubyMethod(name="OctetString", module=true, rest=true)
    public static IRubyObject fact_OctetString(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "OctetString", args);
    }

    @JRubyMethod(name="UTF8String", module=true, rest=true)
    public static IRubyObject fact_UTF8String(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "UTF8String", args);
    }

    @JRubyMethod(name="NumericString", module=true, rest=true)
    public static IRubyObject fact_NumericString(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "NumericString", args);
    }

    @JRubyMethod(name="PrintableString", module=true, rest=true)
    public static IRubyObject fact_PrintableString(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "PrintableString", args);
    }

    @JRubyMethod(name="T61String", module=true, rest=true)
    public static IRubyObject fact_T61String(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "T61String", args);
    }

    @JRubyMethod(name="VideotexString", module=true, rest=true)
    public static IRubyObject fact_VideotexString(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("VideotexString").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
    }

    @JRubyMethod(name="IA5String", module=true, rest=true)
    public static IRubyObject fact_IA5String(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("IA5String").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
    }

    @JRubyMethod(name="GraphicString", module=true, rest=true)
    public static IRubyObject fact_GraphicString(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("GraphicString").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
    }

    @JRubyMethod(name="ISO64String", module=true, rest=true)
    public static IRubyObject fact_ISO64String(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("ISO64String").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
    }

    @JRubyMethod(name="GeneralString", module=true, rest=true)
    public static IRubyObject fact_GeneralString(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "GeneralString", args);
    }

    @JRubyMethod(name="UniversalString", module=true, rest=true)
    public static IRubyObject fact_UniversalString(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "UniversalString", args);
    }

    @JRubyMethod(name="BMPString", module=true, rest=true)
    public static IRubyObject fact_BMPString(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "BMPString", args);
    }

    @JRubyMethod(name="Nul", module=true, rest=true)
    public static IRubyObject fact_Null(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "Null", args);
    }

    @JRubyMethod(name="ObjectId", module=true, rest=true)
    public static IRubyObject fact_ObjectId(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "ObjectId", args);
    }

    @JRubyMethod(name="UTCTime", module=true, rest=true)
    public static IRubyObject fact_UTCTime(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "UTCTime", args);
    }

    @JRubyMethod(name="GeneralizedTime", module=true, rest=true)
    public static IRubyObject fact_GeneralizedTime(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "GeneralizedTime", args);
    }

    @JRubyMethod(name="Sequence", module=true, rest=true)
    public static IRubyObject fact_Sequence(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "Sequence", args);
    }

    @JRubyMethod(name="Set", module=true, rest=true)
    public static IRubyObject fact_Set(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "Set", args);
    }

    private static IRubyObject callClassNew(final IRubyObject self, final String className, final IRubyObject[] args) {
        return ((RubyModule) self).getClass(className).callMethod(self.getRuntime().getCurrentContext(), "new", args);
    }

    @JRubyMethod(meta=true, required=1)
    public static IRubyObject traverse(final ThreadContext context, final IRubyObject self, IRubyObject arg) {
        warn(context, "WARNING: unimplemented method called: ASN1#traverse");
        return context.runtime.getNil();
    }

    public static class ObjectId {

        @JRubyMethod(meta = true, rest = true)
        public static IRubyObject register(final IRubyObject self, final IRubyObject[] args) {
            final Ruby runtime = self.getRuntime();
            final ASN1ObjectIdentifier derOid = new ASN1ObjectIdentifier( args[0].toString() );
            final String a1 = args[1].toString();
            final String a2 = args[2].toString();
            getOIDLookup(runtime).put(a1.toLowerCase(), derOid);
            getOIDLookup(runtime).put(a2.toLowerCase(), derOid);
            getSymLookup(runtime).put(derOid, a1);
            return runtime.getTrue();
        }

        @JRubyMethod(name = { "sn", "short_name" })
        public static IRubyObject sn(final ThreadContext context, final IRubyObject self) {
            final Ruby runtime = context.runtime;
            return runtime.newString( getShortNameFor(runtime, self.callMethod(context, "value").toString()) );
        }

        @JRubyMethod(name = { "ln", "long_name" })
        public static IRubyObject ln(final ThreadContext context, final IRubyObject self) {
            final Ruby runtime = context.runtime;
            return runtime.newString( getLongNameFor(runtime, self.callMethod(context, "value").toString()) );
        }

        @JRubyMethod
        public static IRubyObject oid(final ThreadContext context, final IRubyObject self) {
            final Ruby runtime = context.runtime;
            return runtime.newString( getObjectIdentifier(runtime, self.callMethod(context, "value").toString()).getId() );
        }

    }

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssz");

    private static IRubyObject decodeObject(final ThreadContext context, final RubyModule _ASN1, final Object obj)
        throws IOException, IllegalArgumentException {

        int ix = idForClass(obj.getClass());
        final String className = ix == -1 ? null : (String) ( ASN1_INFO[ix][2] );

        if ( className != null ) {
            final RubyClass klass = _ASN1.getClass(className);
            if ( obj instanceof DERBitString ) {
                final DERBitString derObj = (DERBitString) obj;
                ByteList bl = new ByteList(derObj.getBytes(), false);
                IRubyObject bString = klass.callMethod(context, "new", context.runtime.newString(bl));
                bString.callMethod(context, "unused_bits=", context.runtime.newFixnum( derObj.getPadBits() ));
                return bString;
            }
            else if ( obj instanceof ASN1String ) {
                final ByteList val;
                if ( obj instanceof DERUTF8String ) {
                    val = new ByteList(((DERUTF8String) obj).getString().getBytes("UTF-8"));
                } else {
                    val = ByteList.create(((ASN1String) obj).getString());
                }
                return klass.callMethod(context, "new", context.runtime.newString(val));
            }
            else if ( obj instanceof ASN1Sequence ) {
                RubyArray arr = decodeObjects(context, _ASN1, ((ASN1Sequence) obj).getObjects());
                return klass.callMethod(context, "new", arr);
            }
            else if ( obj instanceof ASN1Set ) {
                RubyArray arr = decodeObjects(context, _ASN1, ((ASN1Set) obj).getObjects());
                return klass.callMethod(context, "new", arr);
            }
            else if ( obj instanceof DERNull ) {
                return klass.callMethod(context,"new", context.runtime.getNil());
            }
            else if ( obj instanceof ASN1Integer ) {
                return klass.callMethod(context, "new", BN.newBN(context.runtime, ((ASN1Integer) obj).getValue()));
            }
            else if ( obj instanceof DERUTCTime ) {
                final Calendar calendar = Calendar.getInstance();
                try {
                    calendar.setTime( dateFormat.parse(((DERUTCTime) obj).getAdjustedTime()) );
                } catch (ParseException e) { throw new IOException(e); }
                IRubyObject[] argv = new IRubyObject[] {
                    context.runtime.newFixnum(calendar.get(Calendar.YEAR)),
                    context.runtime.newFixnum(calendar.get(Calendar.MONTH) + 1),
                    context.runtime.newFixnum(calendar.get(Calendar.DAY_OF_MONTH)),
                    context.runtime.newFixnum(calendar.get(Calendar.HOUR_OF_DAY)),
                    context.runtime.newFixnum(calendar.get(Calendar.MINUTE)),
                    context.runtime.newFixnum(calendar.get(Calendar.SECOND)),
                };
                return klass.callMethod(context, "new", context.runtime.getClass("Time").callMethod(context, "local", argv));
            }
            else if ( obj instanceof ASN1ObjectIdentifier ) {
                final String objId = ((ASN1ObjectIdentifier) obj).getId();
                return klass.callMethod(context, "new", context.runtime.newString(objId));
            }
            else if ( obj instanceof DEROctetString ) {
                final ByteList octets = new ByteList(((DEROctetString) obj).getOctets(), false);
                return klass.callMethod(context, "new", context.runtime.newString(octets));
            }
            else if ( obj instanceof DERBoolean ) {
                return klass.callMethod(context, "new", context.runtime.newBoolean( ((DERBoolean) obj).isTrue() ));
            }
            else {
                debug(context.runtime, "ASN1.decodeObject() should handle: " + obj.getClass().getName());
            }
        }
        else if ( obj instanceof ASN1TaggedObject ) {
            final ASN1TaggedObject taggedObj = ((ASN1TaggedObject) obj);
            IRubyObject val = decodeObject(context, _ASN1, taggedObj.getObject());
            IRubyObject tag = context.runtime.newFixnum( taggedObj.getTagNo() );
            IRubyObject tag_class = context.runtime.newSymbol("CONTEXT_SPECIFIC");
            final RubyArray valArr = context.runtime.newArray(val);
            return _ASN1.getClass("ASN1Data").callMethod(context, "new",
                new IRubyObject[] { valArr, tag, tag_class }
            );
        }
        else if ( obj instanceof ASN1Sequence) {
            // Likely a DERSequence returned by bouncycastle libs. Convert to DLSequence.
            RubyArray arr = decodeObjects(context, _ASN1, ((ASN1Sequence) obj).getObjects());
            return _ASN1.getClass("Sequence").callMethod(context, "new", arr);
        }
        else if ( obj instanceof ASN1Set ) {
            // Likely a DERSet returned by bouncycastle libs. Convert to DLSet.
            RubyArray arr = decodeObjects(context, _ASN1, ((ASN1Set) obj).getObjects());
            return _ASN1.getClass("Set").callMethod(context, "new", arr);
        }

        //Used to return null. Led to confusing exceptions later.
        throw new IllegalArgumentException("jruby-openssl unable to decode object: " + obj + "[" + obj.getClass().getName() + "]");
    }

    private static RubyArray decodeObjects(final ThreadContext context, final RubyModule _ASN1, final Enumeration e)
        throws IOException {
        final RubyArray arr = context.runtime.newArray();
        while ( e.hasMoreElements() ) {
            arr.append( decodeObject(context, _ASN1, e.nextElement()) );
        }
        return arr;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject decode(final ThreadContext context,
        final IRubyObject self, final IRubyObject obj) {
        try {
            return decodeImpl(context, (RubyModule) self, obj);
        }
        catch (IOException e) {
            throw context.runtime.newIOErrorFromException(e);
        }
        catch (IllegalArgumentException e) {
            throw context.runtime.newArgumentError(e.getMessage());
        }
        catch (RuntimeException e) {
            final Ruby runtime = context.runtime;
            debugStackTrace(runtime, e);
            throw Utils.newRuntimeError(context.runtime, e);
        }
    }

    static IRubyObject decodeImpl(final ThreadContext context, IRubyObject obj)
        throws IOException, IllegalArgumentException {
        return decodeImpl(context, _ASN1(context.runtime), obj);
    }

    static IRubyObject decodeImpl(final ThreadContext context,
        final RubyModule _ASN1, IRubyObject obj) throws IOException, IllegalArgumentException {
        obj = OpenSSLImpl.to_der_if_possible(context, obj);
        ASN1InputStream asis = new ASN1InputStream(obj.convertToString().getBytes());
        return decodeObject(context, _ASN1, asis.readObject());
    }

    @JRubyMethod(meta = true, required = 1)
    public static IRubyObject decode_all(final ThreadContext context, final IRubyObject self, IRubyObject arg) {
        warn(context, "WARNING: unimplemented method called: ASN1#decode_all");
        return context.runtime.getNil();
    }

    public static RaiseException newASN1Error(Ruby runtime, String message) {
        return Utils.newError(runtime, _ASN1(runtime).getClass("ASN1Error"), message, false);
    }

    static RubyModule _ASN1(final Ruby runtime) {
        return (RubyModule) runtime.getModule("OpenSSL").getConstant("ASN1");
    }

    public static class ASN1Data extends RubyObject {
        private static final long serialVersionUID = 6117598347932209839L;

        static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new ASN1Data(runtime, klass);
            }
        };

        public ASN1Data(Ruby runtime, RubyClass type) {
            super(runtime,type);
        }

        @JRubyMethod(visibility = Visibility.PRIVATE)
        public IRubyObject initialize(final ThreadContext context,
            final IRubyObject value, final IRubyObject tag, final IRubyObject tag_class) {
            if ( ! (tag_class instanceof RubySymbol) ) {
                throw newASN1Error(context.runtime, "invalid tag class");
            }
            if ( tag_class.toString().equals(":UNIVERSAL") && RubyNumeric.fix2int(tag) > 31 ) {
                throw newASN1Error(context.runtime, "tag number for Universal too large");
            }
            this.callMethod(context,"tag=", tag);
            this.callMethod(context,"value=", value);
            this.callMethod(context,"tag_class=", tag_class);
            return this;
        }

        ASN1Encodable toASN1(final ThreadContext context) {
            final int tag = RubyNumeric.fix2int(callMethod(context, "tag"));
            final IRubyObject val = callMethod(context, "value");
            if ( val instanceof RubyArray ) {
                RubyArray arr = (RubyArray) callMethod(context, "value");
                if ( arr.size() > 1 ) {
                    ASN1EncodableVector vec = new ASN1EncodableVector();
                    for (IRubyObject obj : arr.toJavaArray()) {
                        vec.add(((ASN1Data)obj).toASN1());
                    }
                    return new DERTaggedObject(tag, new DLSequence(vec));
                } else {
                    return new DERTaggedObject(tag, ((ASN1Data)(arr.getList().get(0))).toASN1(context));
                }
            } else {
                return new DERTaggedObject(tag, ((ASN1Data) val).toASN1(context));
            }
        }

        @Deprecated
        final ASN1Encodable toASN1() {
            return toASN1( getRuntime().getCurrentContext() );
        }

        @JRubyMethod
        public IRubyObject to_der(final ThreadContext context) {
            try {
                final byte[] encoded = toASN1(context).toASN1Primitive().getEncoded(ASN1Encoding.DER);
                return context.runtime.newString(new ByteList(encoded ,false));
            }
            catch (IOException e) {
                throw newASN1Error(context.runtime, e.getMessage());
            }
        }

        protected IRubyObject defaultTag() {
            int i = idForRubyName(getMetaClass().getRealClass().getBaseName());
            if(i != -1) {
                return getRuntime().newFixnum(i);
            } else {
                return getRuntime().getNil();
            }
        }

        IRubyObject value() {
            return this.callMethod(getRuntime().getCurrentContext(), "value");
        }

        @Override
        public String toString() {
            return value().toString();
        }

        protected final void print() {
            print(0);
        }

        protected void print(int indent) {
            final PrintStream out = getRuntime().getOut();
            printIndent(out, indent);
            final IRubyObject value = value();
            out.println("ASN1Data: ");
            if ( value instanceof RubyArray ) {
                printArray(out, indent, (RubyArray) value);
            } else {
                ((ASN1Data) value).print(indent + 1);
            }
        }

        static void printIndent(final PrintStream out, final int indent) {
            for ( int i = 0; i < indent; i++) out.print(" ");
        }

        static void printArray(final PrintStream out, final int indent, final RubyArray array) {
            for ( int i = 0; i < array.size(); i++ ) {
                ((ASN1Data) array.entry(i)).print(indent + 1);
            }
        }

        static RaiseException createNativeRaiseException(final ThreadContext context, final Exception e) {
            Throwable cause = e.getCause(); if ( cause == null ) cause = e;
            return RaiseException.createNativeRaiseException(context.runtime, cause);
        }

    }

    public static class ASN1Primitive extends ASN1Data {
        private static final long serialVersionUID = 8489625559339190259L;

        static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new ASN1Primitive(runtime, klass);
            }
        };

        public ASN1Primitive(Ruby runtime, RubyClass type) {
            super(runtime,type);
        }

        @Override
        @JRubyMethod
        public IRubyObject to_der(final ThreadContext context) {
            return super.to_der(context);
        }

        @JRubyMethod(required=1, optional=4, visibility = Visibility.PRIVATE)
        public IRubyObject initialize(final ThreadContext context, final IRubyObject[] args) {
            final Ruby runtime = context.runtime;
            IRubyObject value = args[0];
            final IRubyObject tag;
            IRubyObject tagging = runtime.getNil();
            IRubyObject tag_class = runtime.getNil();

            if ( args.length > 1 ) {
                tag = args[1];
                if ( args.length > 2 ) {
                    tagging = args[2];
                    if ( args.length > 3 ) tag_class = args[3];
                }

                if ( tag.isNil() ) throw newASN1Error(runtime, "must specify tag number");

                if ( tagging.isNil() ) {
                    tagging = runtime.newSymbol("EXPLICIT");
                }

                if ( ! (tagging instanceof RubySymbol) ) {
                    throw newASN1Error(runtime, "invalid tag default");
                }

                if ( tag_class.isNil() ) {
                    tag_class = runtime.newSymbol("CONTEXT_SPECIFIC");
                }

                if ( ! (tag_class instanceof RubySymbol) ) {
                    throw newASN1Error(runtime, "invalid tag class");
                }

                if ( tagging.toString().equals(":IMPLICIT") && RubyNumeric.fix2int(tag) > 31 ) {
                    throw newASN1Error(runtime, "tag number for Universal too large");
                }
            }
            else {
                tag = defaultTag();
                tag_class = runtime.newSymbol("UNIVERSAL");
            }
            if ( "ObjectId".equals( getMetaClass().getRealClass().getBaseName() ) ) {
                String v = getSymLookup(runtime).get( getObjectIdentifier(runtime, value.toString()) );
                if ( v != null ) value = runtime.newString(v);
            }

            this.callMethod(context, "tag=", tag);
            this.callMethod(context, "value=", value);
            this.callMethod(context, "tagging=", tagging);
            this.callMethod(context, "tag_class=", tag_class);
            return this;
        }

        @Override
        ASN1Encodable toASN1(final ThreadContext context) {
            final int tag = idForRubyName(getMetaClass().getRealClass().getBaseName());
            @SuppressWarnings("unchecked")
            Class<? extends ASN1Encodable> impl = (Class<? extends ASN1Encodable>) ASN1_INFO[tag][1];

            final IRubyObject val = callMethod(context, "value");
            if ( impl == ASN1ObjectIdentifier.class ) {
                return getObjectIdentifier(context.runtime, val.toString());
            }
            else if ( impl == DERNull.class ) {
                return new DERNull();
            }
            else if ( impl == DERBoolean.class ) {
                return new DERBoolean(val.isTrue());
            }
            else if ( impl == DERUTCTime.class ) {
                return new DERUTCTime(((RubyTime) val).getJavaDate());
            }
            else if ( impl == ASN1Integer.class ) {
                if ( val instanceof RubyBignum ) {
                    return new ASN1Integer(((RubyBignum) val).getValue());
                }
                if ( val instanceof BN ) {
                    return new ASN1Integer(((BN) val).getValue());
                }
                return new ASN1Integer(new BigInteger(val.toString()));
            }
            else if ( impl == DEROctetString.class ) {
                return new DEROctetString(val.asString().getBytes());
            }
            else if ( impl == DERBitString.class ) {
                final byte[] bs = val.asString().getBytes();
                int unused = 0;
                for ( int i = (bs.length - 1); i > -1; i-- ) {
                    if (bs[i] == 0) unused += 8;
                    else {
                        byte v2 = bs[i];
                        int x = 8;
                        while ( v2 != 0 ) {
                            v2 <<= 1;
                            x--;
                        }
                        unused += x;
                        break;
                    }
                }
                return new DERBitString(bs,unused);
            }
            else if ( val instanceof RubyString ) {
                try {
                    return impl.getConstructor(String.class).newInstance(val.toString());
                }
                catch (Exception e) {
                    throw createNativeRaiseException(context, e);
                }
            }

            // TODO throw an exception here too?
            if ( isDebug(context.runtime) ) {
                context.runtime.getOut().println("object with tag: " + tag + " and value: " + val + " and val.class: " + val.getClass().getName() + " and impl: " + impl.getName());
            }
            warn(context, "WARNING: unimplemented method called: ASN1Data#toASN1 (" + impl + ")");
            return null;
        }

        @Override
        protected void print(int indent) {
            final PrintStream out = getRuntime().getOut();
            printIndent(out, indent);
            out.print(getMetaClass().getRealClass().getBaseName());
            out.print(": ");
            out.println(value().callMethod(getRuntime().getCurrentContext(), "inspect").toString());
        }

    }

    public static class ASN1Constructive extends ASN1Data {
        private static final long serialVersionUID = -7166662655104776828L;

        static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new ASN1Constructive(runtime, klass);
            }
        };

        public ASN1Constructive(Ruby runtime, RubyClass type) {
            super(runtime,type);
        }

        @Override
        @JRubyMethod
        public IRubyObject to_der(final ThreadContext context) {
            return super.to_der(context);
        }

        @JRubyMethod(required=1, optional=3, visibility = Visibility.PRIVATE)
        public IRubyObject initialize(final ThreadContext context, final IRubyObject[] args) {
            final Ruby runtime = context.runtime;

            final IRubyObject value = args[0];
            final IRubyObject tag;
            IRubyObject tagging = runtime.getNil();
            IRubyObject tag_class = runtime.getNil();

            if ( args.length > 1 ) {
                tag = args[1];
                if ( args.length > 2 ) {
                    tagging = args[2];
                    if ( args.length > 3 ) tag_class = args[3];
                }

                if ( tag.isNil() ) throw newASN1Error(runtime, "must specify tag number");

                if ( tagging.isNil() ) {
                    tagging = runtime.newSymbol("EXPLICIT");
                }

                if ( ! (tagging instanceof RubySymbol) ) {
                    throw newASN1Error(runtime, "invalid tag default");
                }

                if ( tag_class.isNil() ) {
                    tag_class = runtime.newSymbol("CONTEXT_SPECIFIC");
                }

                if ( ! (tag_class instanceof RubySymbol) ) {
                    throw newASN1Error(runtime, "invalid tag class");
                }

                if ( tagging.toString().equals(":IMPLICIT") && RubyNumeric.fix2int(tag) > 31 ) {
                    throw newASN1Error(runtime, "tag number for Universal too large");
                }
            }
            else {
                tag = defaultTag();
                tag_class = runtime.newSymbol("UNIVERSAL");
            }

            callMethod(context, "tag=", tag);
            callMethod(context, "value=", value);
            callMethod(context, "tagging=", tagging);
            callMethod(context, "tag_class=", tag_class);

            return this;
        }

        @Override
        ASN1Encodable toASN1(final ThreadContext context) {
            final int id = idForRubyName(getMetaClass().getRealClass().getBaseName());
            if ( id != -1 ) {
                final ASN1EncodableVector vec = new ASN1EncodableVector();
                final RubyArray value = value(context);
                for ( int i = 0; i < value.size(); i++ ) {
                    final IRubyObject entry = value.entry(i);
                    try {
                        if ( entry instanceof ASN1Data) {
                            vec.add( ( (ASN1Data) entry ).toASN1(context) );
                        } else {
                            vec.add( ( (ASN1Data) decodeImpl(context, entry) ).toASN1(context) );
                        }
                    }
                    catch (Exception e) { // TODO: deprecated
                        throw createNativeRaiseException(context, e);
                    }
                }
                try {
                    @SuppressWarnings("unchecked")
                    ASN1Encodable result = ((Class<? extends ASN1Encodable>)
                        ( ASN1_INFO[id][1]) ).
                            getConstructor(new Class[] { ASN1EncodableVector.class }).
                            newInstance(new Object[] { vec });
                    return result;
                }
                catch (Exception e) { // TODO: deprecated
                    throw createNativeRaiseException(context, e);
                }
            }
            return null;
        }

        @JRubyMethod
        public IRubyObject each(final ThreadContext context, final Block block) {
            final RubyArray value = value(context);
            for ( int i = 0; i < value.size(); i++ ) {
                block.yield(context, value.entry(i));
            }
            return context.runtime.getNil();
        }

        @Override
        protected void print(int indent) {
            final PrintStream out = getRuntime().getOut();
            printIndent(out, indent);
            out.print(getMetaClass().getRealClass().getBaseName()); out.println(": ");
            printArray( out, indent, value( getRuntime().getCurrentContext() ) );
        }

        private RubyArray value(final ThreadContext context) {
            return (RubyArray) this.callMethod(context, "value");
        }

    }
}// ASN1
