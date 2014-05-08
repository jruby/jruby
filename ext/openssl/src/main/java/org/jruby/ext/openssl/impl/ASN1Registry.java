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

import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;

/**
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class ASN1Registry {

    @SuppressWarnings("unchecked")
    private static final Map<String, ASN1ObjectIdentifier> SYM_TO_OID = new HashMap<String, ASN1ObjectIdentifier>(org.bouncycastle.asn1.x509.X509Name.DefaultLookUp);
    @SuppressWarnings("unchecked")
    private static final Map<ASN1ObjectIdentifier, String> OID_TO_SYM = new HashMap<ASN1ObjectIdentifier, String>(org.bouncycastle.asn1.x509.X509Name.DefaultSymbols);

    private static final Map<ASN1ObjectIdentifier, Integer> OID_TO_NID = new HashMap<ASN1ObjectIdentifier, Integer>();
    private static final Map<Integer, ASN1ObjectIdentifier> NID_TO_OID = new HashMap<Integer, ASN1ObjectIdentifier>();
    private static final Map<Integer, String> NID_TO_SN = new HashMap<Integer, String>();
    private static final Map<Integer, String> NID_TO_LN = new HashMap<Integer, String>();

    // seems no longer used
    static Integer obj2nid(String oid) {
        return obj2nid(new ASN1ObjectIdentifier(oid));
    }

    public static Integer obj2nid(final ASN1ObjectIdentifier oid) {
        return OID_TO_NID.get(oid);
    }

    // seems no longer used
    static String ln2oid(final String ln) {
        ASN1ObjectIdentifier oid = sym2oid(ln);
        return oid == null ? null : oid.getId();
    }

    public static String o2a(String oid) {
        return o2a(new ASN1ObjectIdentifier(oid));
    }

    public static String o2a(final ASN1ObjectIdentifier obj) {
        Integer nid = obj2nid(obj);
        String name = NID_TO_LN.get(nid);
        if( name == null ) name = NID_TO_SN.get(nid);
        return name;
    }

    public static ASN1ObjectIdentifier sym2oid(String name) {
        return SYM_TO_OID.get(name.toLowerCase());
    }

    // seems no longer used
    static int sym2nid(String name) {
        return OID_TO_NID.get(SYM_TO_OID.get(name.toLowerCase()));
    }

    static String nid2ln(int nid) {
        return NID_TO_LN.get( Integer.valueOf(nid) );
    }

    public static ASN1ObjectIdentifier nid2obj(int nid) {
        return NID_TO_OID.get(nid);
    }

    // seems no longer used
    static String nid2ln(Integer nid) {
        return NID_TO_LN.get(nid);
    }

    private static ASN1ObjectIdentifier addObject(final int nid, String sn, String ln, final String oid) {
        //if ( oid != null ) {
        final ASN1ObjectIdentifier objectId = new ASN1ObjectIdentifier(oid);
        addObject(Integer.valueOf(nid), sn, ln, objectId);
        return objectId;
        //}
        //return null;
    }

    private static void addObject(final Integer nid, String sn, String ln, final ASN1ObjectIdentifier oid) {
        if ( sn != null ) {
            SYM_TO_OID.put(sn.toLowerCase(), oid);
        }
        if ( ln != null ) {
            SYM_TO_OID.put(ln.toLowerCase(), oid);
        }
        OID_TO_SYM.put(oid, sn == null ? ln : sn);
        OID_TO_NID.put(oid, nid);
        NID_TO_OID.put(nid, oid);
        NID_TO_SN.put(nid, sn);
        NID_TO_LN.put(nid, ln);
    }

    public final static int    NID_id_pkix = 127;
    public final static String SN_id_pkix = "PKIX";
    public final static String OBJ_id_pkix = "1.3.6.1.5.5.7";

    public final static int    NID_id_ad = 176;
    public final static String SN_id_ad = "id-ad";
    public final static String OBJ_id_ad = OBJ_id_pkix + ".48";

    public final static int    NID_ad_OCSP = 178;
    public final static String SN_ad_OCSP = "OCSP";
    public final static String LN_ad_OCSP = "OCSP";
    public final static String OBJ_ad_OCSP = OBJ_id_ad + ".1";

    public final static String OBJ_id_pkix_OCSP = OBJ_ad_OCSP;

    public final static int    NID_iso = 181;
    public final static String SN_iso = "ISO";
    public final static String LN_iso = "iso";
    public final static String OBJ_iso = "1";

    public final static int    NID_org = 379;
    public final static String SN_org = "ORG";
    public final static String LN_org = "org";
    public final static String OBJ_org = OBJ_iso + ".3";

    public final static int    NID_dod = 380;
    public final static String SN_dod = "DOD";
    public final static String LN_dod = "dod";
    public final static String OBJ_dod = OBJ_org + ".6";

    public final static int    NID_iana = 381;
    public final static String SN_iana = "IANA";
    public final static String LN_iana = "iana";
    public final static String OBJ_iana = OBJ_dod + ".1";

    public final static String OBJ_internet = OBJ_iana;

    public final static String OBJ_csor = "2.16.840.1.101.3";

    public final static int    NID_member_body = 182;
    public final static String SN_member_body = "member-body";
    public final static String LN_member_body = "ISO Member Body";
    public final static String OBJ_member_body = OBJ_iso + ".2";

    public final static int    NID_ISO_US = 183;
    public final static String SN_ISO_US = "ISO-US";
    public final static String LN_ISO_US = "ISO US Member Body";
    public final static String OBJ_ISO_US = OBJ_member_body + ".840";

    public final static int    NID_rsadsi = 1;
    public final static String SN_rsadsi = "rsadsi";
    public final static String LN_rsadsi = "RSA Data Security, Inc.";
    public final static String OBJ_rsadsi = OBJ_ISO_US + ".113549";

    public final static int    NID_pkcs = 2;
    public final static String SN_pkcs = "pkcs";
    public final static String LN_pkcs = "RSA Data Security, Inc. PKCS";
    public final static String OBJ_pkcs = OBJ_rsadsi + ".1";

    public final static String OBJ_pkcs12 = OBJ_pkcs + ".12";

    public final static int    NID_X9_57 = 184;
    public final static String SN_X9_57 = "X9-57";
    public final static String LN_X9_57 = "X9.57";
    public final static String OBJ_X9_57 = OBJ_ISO_US + ".10040";

    public final static int    NID_ansi_X9_62 = 405;
    public final static String SN_ansi_X9_62 = "ansi-X9-62";
    public final static String LN_ansi_X9_62 = "ANSI X9.62";
    public final static String OBJ_ansi_X9_62 = OBJ_ISO_US + ".10045";

    public final static String OBJ_holdInstruction = OBJ_X9_57 + ".2";

    public final static String OBJ_X9_62_id_fieldType = OBJ_ansi_X9_62 + ".1";

    public final static String OBJ_X9_62_ellipticCurve = OBJ_ansi_X9_62 + ".3";

    public final static String OBJ_X9_62_id_publicKeyType = OBJ_ansi_X9_62 + ".2";

    public final static String OBJ_X9_62_id_ecSigType = OBJ_ansi_X9_62 + ".4";

    public final static String OBJ_X9_62_primeCurve = OBJ_X9_62_ellipticCurve + ".1";

    public final static String OBJ_X9_62_c_TwoCurve = OBJ_X9_62_ellipticCurve + ".0";

    public final static String OBJ_nistAlgorithms = OBJ_csor + ".4";

    public final static int    NID_joint_iso_itu_t = 646;
    public final static String SN_joint_iso_itu_t = "JOINT-ISO-ITU-T";
    public final static String LN_joint_iso_itu_t = "joint-iso-itu-t";
    public final static String OBJ_joint_iso_itu_t = "2";

    public final static int    NID_international_organizations = 647;
    public final static String SN_international_organizations = "international-organizations";
    public final static String LN_international_organizations = "International Organizations";
    public final static String OBJ_international_organizations = OBJ_joint_iso_itu_t + ".23";

    public final static int    NID_wap = 678;
    public final static String SN_wap = "wap";
    public final static String OBJ_wap = OBJ_international_organizations + ".43";

    public final static int    NID_wap_wsg = 679;
    public final static String SN_wap_wsg = "wap-wsg";
    public final static String OBJ_wap_wsg = OBJ_wap + ".13";

    public final static String OBJ_wap_wsg_idm_ecid = OBJ_wap_wsg + ".4";

    public final static String OBJ_nist_hashalgs = OBJ_nistAlgorithms + ".2";

    public final static String OBJ_pkcs12_Version1 = OBJ_pkcs12 + ".10";

    public final static int    NID_pkcs9 = 47;
    public final static String SN_pkcs9 = "pkcs9";
    public final static String OBJ_pkcs9 = OBJ_pkcs + ".9";

    public final static String OBJ_certTypes = OBJ_pkcs9 + ".22";

    public final static String OBJ_crlTypes = OBJ_pkcs9 + ".23";

    public final static int    NID_identified_organization = 676;
    public final static String SN_identified_organization = "identified-organization";
    public final static String OBJ_identified_organization = OBJ_iso + ".3";

    public final static int    NID_certicom_arc = 677;
    public final static String SN_certicom_arc = "certicom-arc";
    public final static String OBJ_certicom_arc = OBJ_identified_organization + ".132";

    public final static String OBJ_secg_ellipticCurve = OBJ_certicom_arc + ".0";

    public final static String OBJ_aes = OBJ_nistAlgorithms + ".1";

    public final static String OBJ_pkcs12_BagIds = OBJ_pkcs12_Version1 + ".1";

    public final static String OBJ_dsa_with_sha2 = OBJ_nistAlgorithms + ".3";

    public final static int    NID_undef = 0;
    public final static String SN_undef = "UNDEF";
    public final static String LN_undef = "undefined";
    public final static String OBJ_undef = "0";

    public final static int    NID_md2 = 3;
    public final static String SN_md2 = "MD2";
    public final static String LN_md2 = "md2";
    public final static String OBJ_md2 = OBJ_rsadsi + ".2.2";

    public final static int    NID_md5 = 4;
    public final static String SN_md5 = "MD5";
    public final static String LN_md5 = "md5";
    public final static String OBJ_md5 = OBJ_rsadsi + ".2.5";

    public final static int    NID_rc4 = 5;
    public final static String SN_rc4 = "RC4";
    public final static String LN_rc4 = "rc4";
    public final static String OBJ_rc4 = OBJ_rsadsi + ".3.4";

    public final static int    NID_pkcs1 = 186;
    public final static String SN_pkcs1 = "pkcs1";
    public final static String OBJ_pkcs1 = OBJ_pkcs + ".1";

    public final static int    NID_rsaEncryption = 6;
    public final static String LN_rsaEncryption = "rsaEncryption";
    public final static String OBJ_rsaEncryption = OBJ_pkcs1 + ".1";
    public final static ASN1ObjectIdentifier OID_rsaEncryption = new ASN1ObjectIdentifier(OBJ_rsaEncryption);

    public final static int    NID_md2WithRSAEncryption = 7;
    public final static String SN_md2WithRSAEncryption = "RSA-MD2";
    public final static String LN_md2WithRSAEncryption = "md2WithRSAEncryption";
    public final static String OBJ_md2WithRSAEncryption = OBJ_pkcs1 + ".2";

    public final static int    NID_md5WithRSAEncryption = 8;
    public final static String SN_md5WithRSAEncryption = "RSA-MD5";
    public final static String LN_md5WithRSAEncryption = "md5WithRSAEncryption";
    public final static String OBJ_md5WithRSAEncryption = OBJ_pkcs1 + ".4";

    public final static int    NID_pkcs5 = 187;
    public final static String SN_pkcs5 = "pkcs5";
    public final static String OBJ_pkcs5 = OBJ_pkcs + ".5";

    public final static int    NID_pbeWithMD2AndDES_CBC = 9;
    public final static String SN_pbeWithMD2AndDES_CBC = "PBE-MD2-DES";
    public final static String LN_pbeWithMD2AndDES_CBC = "pbeWithMD2AndDES-CBC";
    public final static String OBJ_pbeWithMD2AndDES_CBC = OBJ_pkcs5 + ".1";

    public final static int    NID_pbeWithMD5AndDES_CBC = 10;
    public final static String SN_pbeWithMD5AndDES_CBC = "PBE-MD5-DES";
    public final static String LN_pbeWithMD5AndDES_CBC = "pbeWithMD5AndDES-CBC";
    public final static String OBJ_pbeWithMD5AndDES_CBC = OBJ_pkcs5 + ".3";

    public final static int    NID_X500 = 11;
    public final static String SN_X500 = "X500";
    public final static String LN_X500 = "directory services (X.500)";
    public final static String OBJ_X500 = "2.5";

    public final static int    NID_X509 = 12;
    public final static String SN_X509 = "X509";
    public final static String OBJ_X509 = OBJ_X500 + ".4";

    public final static int    NID_commonName = 13;
    public final static String SN_commonName = "CN";
    public final static String LN_commonName = "commonName";
    public final static String OBJ_commonName = OBJ_X509 + ".3";

    public final static int    NID_countryName = 14;
    public final static String SN_countryName = "C";
    public final static String LN_countryName = "countryName";
    public final static String OBJ_countryName = OBJ_X509 + ".6";

    public final static int    NID_localityName = 15;
    public final static String SN_localityName = "L";
    public final static String LN_localityName = "localityName";
    public final static String OBJ_localityName = OBJ_X509 + ".7";

    public final static int    NID_stateOrProvinceName = 16;
    public final static String SN_stateOrProvinceName = "ST";
    public final static String LN_stateOrProvinceName = "stateOrProvinceName";
    public final static String OBJ_stateOrProvinceName = OBJ_X509 + ".8";

    public final static int    NID_organizationName = 17;
    public final static String SN_organizationName = "O";
    public final static String LN_organizationName = "organizationName";
    public final static String OBJ_organizationName = OBJ_X509 + ".10";

    public final static int    NID_organizationalUnitName = 18;
    public final static String SN_organizationalUnitName = "OU";
    public final static String LN_organizationalUnitName = "organizationalUnitName";
    public final static String OBJ_organizationalUnitName = OBJ_X509 + ".11";

    public final static int    NID_X500algorithms = 378;
    public final static String SN_X500algorithms = "X500algorithms";
    public final static String LN_X500algorithms = "directory services - algorithms";
    public final static String OBJ_X500algorithms = OBJ_X500 + ".8";

    public final static int    NID_rsa = 19;
    public final static String SN_rsa = "RSA";
    public final static String LN_rsa = "rsa";
    public final static String OBJ_rsa = OBJ_X500algorithms + ".1.1";

    public final static int    NID_pkcs7 = 20;
    public final static String SN_pkcs7 = "pkcs7";
    public final static String OBJ_pkcs7 = OBJ_pkcs + ".7";

    public final static int    NID_pkcs7_data = 21;
    public final static String LN_pkcs7_data = "pkcs7-data";
    public final static String OBJ_pkcs7_data = OBJ_pkcs7 + ".1";
    public final static ASN1ObjectIdentifier OID_pkcs7_data = new ASN1ObjectIdentifier(OBJ_pkcs7_data);

    public final static int    NID_pkcs7_signed = 22;
    public final static String LN_pkcs7_signed = "pkcs7-signedData";
    public final static String OBJ_pkcs7_signed = OBJ_pkcs7 + ".2";

    public final static int    NID_pkcs7_enveloped = 23;
    public final static String LN_pkcs7_enveloped = "pkcs7-envelopedData";
    public final static String OBJ_pkcs7_enveloped = OBJ_pkcs7 + ".3";

    public final static int    NID_pkcs7_signedAndEnveloped = 24;
    public final static String LN_pkcs7_signedAndEnveloped = "pkcs7-signedAndEnvelopedData";
    public final static String OBJ_pkcs7_signedAndEnveloped = OBJ_pkcs7 + ".4";

    public final static int    NID_pkcs7_digest = 25;
    public final static String LN_pkcs7_digest = "pkcs7-digestData";
    public final static String OBJ_pkcs7_digest = OBJ_pkcs7 + ".5";

    public final static int    NID_pkcs7_encrypted = 26;
    public final static String LN_pkcs7_encrypted = "pkcs7-encryptedData";
    public final static String OBJ_pkcs7_encrypted = OBJ_pkcs7 + ".6";

    public final static int    NID_pkcs3 = 27;
    public final static String SN_pkcs3 = "pkcs3";
    public final static String OBJ_pkcs3 = OBJ_pkcs + ".3";

    public final static int    NID_dhKeyAgreement = 28;
    public final static String LN_dhKeyAgreement = "dhKeyAgreement";
    public final static String OBJ_dhKeyAgreement = OBJ_pkcs3 + ".1";

    public final static int    NID_algorithm = 376;
    public final static String SN_algorithm = "algorithm";
    public final static String LN_algorithm = "algorithm";
    public final static String OBJ_algorithm = "1.3.14.3.2";

    public final static int    NID_des_ecb = 29;
    public final static String SN_des_ecb = "DES-ECB";
    public final static String LN_des_ecb = "des-ecb";
    public final static String OBJ_des_ecb = OBJ_algorithm + ".6";

    public final static int    NID_des_cfb64 = 30;
    public final static String SN_des_cfb64 = "DES-CFB";
    public final static String LN_des_cfb64 = "des-cfb";
    public final static String OBJ_des_cfb64 = OBJ_algorithm + ".9";

    public final static int    NID_des_cbc = 31;
    public final static String SN_des_cbc = "DES-CBC";
    public final static String LN_des_cbc = "des-cbc";
    public final static String OBJ_des_cbc = OBJ_algorithm + ".7";
    public final static ASN1ObjectIdentifier OID_des_cbc = new ASN1ObjectIdentifier(OBJ_des_cbc);

    public final static int    NID_des_ede_ecb = 32;
    public final static String SN_des_ede_ecb = "DES-EDE";
    public final static String LN_des_ede_ecb = "des-ede";
    public final static String OBJ_des_ede_ecb = OBJ_algorithm + ".17";

    public final static int    NID_des_ede3_ecb = 33;
    public final static String SN_des_ede3_ecb = "DES-EDE3";
    public final static String LN_des_ede3_ecb = "des-ede3";

    public final static int    NID_idea_cbc = 34;
    public final static String SN_idea_cbc = "IDEA-CBC";
    public final static String LN_idea_cbc = "idea-cbc";
    public final static String OBJ_idea_cbc = "1.3.6.1.4.1.188.7.1.1.2";

    public final static int    NID_idea_cfb64 = 35;
    public final static String SN_idea_cfb64 = "IDEA-CFB";
    public final static String LN_idea_cfb64 = "idea-cfb";

    public final static int    NID_idea_ecb = 36;
    public final static String SN_idea_ecb = "IDEA-ECB";
    public final static String LN_idea_ecb = "idea-ecb";

    public final static int    NID_rc2_cbc = 37;
    public final static String SN_rc2_cbc = "RC2-CBC";
    public final static String LN_rc2_cbc = "rc2-cbc";
    public final static String OBJ_rc2_cbc = OBJ_rsadsi + ".3.2";
    public final static ASN1ObjectIdentifier OID_rc2_cbc = new ASN1ObjectIdentifier(OBJ_rc2_cbc);

    public final static int    NID_rc2_ecb = 38;
    public final static String SN_rc2_ecb = "RC2-ECB";
    public final static String LN_rc2_ecb = "rc2-ecb";

    public final static int    NID_rc2_cfb64 = 39;
    public final static String SN_rc2_cfb64 = "RC2-CFB";
    public final static String LN_rc2_cfb64 = "rc2-cfb";

    public final static int    NID_rc2_ofb64 = 40;
    public final static String SN_rc2_ofb64 = "RC2-OFB";
    public final static String LN_rc2_ofb64 = "rc2-ofb";

    public final static int    NID_sha = 41;
    public final static String SN_sha = "SHA";
    public final static String LN_sha = "sha";
    public final static String OBJ_sha = OBJ_algorithm + ".18";

    public final static int    NID_shaWithRSAEncryption = 42;
    public final static String SN_shaWithRSAEncryption = "RSA-SHA";
    public final static String LN_shaWithRSAEncryption = "shaWithRSAEncryption";
    public final static String OBJ_shaWithRSAEncryption = OBJ_algorithm + ".15";

    public final static int    NID_des_ede_cbc = 43;
    public final static String SN_des_ede_cbc = "DES-EDE-CBC";
    public final static String LN_des_ede_cbc = "des-ede-cbc";

    public final static int    NID_des_ede3_cbc = 44;
    public final static String SN_des_ede3_cbc = "DES-EDE3-CBC";
    public final static String LN_des_ede3_cbc = "des-ede3-cbc";
    public final static String OBJ_des_ede3_cbc = OBJ_rsadsi + ".3.7";
    public final static ASN1ObjectIdentifier OID_des_ede3_cbc = new ASN1ObjectIdentifier(OBJ_des_ede3_cbc);

    public final static int    NID_des_ofb64 = 45;
    public final static String SN_des_ofb64 = "DES-OFB";
    public final static String LN_des_ofb64 = "des-ofb";
    public final static String OBJ_des_ofb64 = OBJ_algorithm + ".8";

    public final static int    NID_idea_ofb64 = 46;
    public final static String SN_idea_ofb64 = "IDEA-OFB";
    public final static String LN_idea_ofb64 = "idea-ofb";

    public final static int    NID_pkcs9_emailAddress = 48;
    public final static String LN_pkcs9_emailAddress = "emailAddress";
    public final static String OBJ_pkcs9_emailAddress = OBJ_pkcs9 + ".1";

    public final static int    NID_pkcs9_unstructuredName = 49;
    public final static String LN_pkcs9_unstructuredName = "unstructuredName";
    public final static String OBJ_pkcs9_unstructuredName = OBJ_pkcs9 + ".2";

    public final static int    NID_pkcs9_contentType = 50;
    public final static String LN_pkcs9_contentType = "contentType";
    public final static String OBJ_pkcs9_contentType = OBJ_pkcs9 + ".3";

    public final static int    NID_pkcs9_messageDigest = 51;
    public final static String LN_pkcs9_messageDigest = "messageDigest";
    public final static String OBJ_pkcs9_messageDigest = OBJ_pkcs9 + ".4";

    public final static int    NID_pkcs9_signingTime = 52;
    public final static String LN_pkcs9_signingTime = "signingTime";
    public final static String OBJ_pkcs9_signingTime = OBJ_pkcs9 + ".5";

    public final static int    NID_pkcs9_countersignature = 53;
    public final static String LN_pkcs9_countersignature = "countersignature";
    public final static String OBJ_pkcs9_countersignature = OBJ_pkcs9 + ".6";

    public final static int    NID_pkcs9_challengePassword = 54;
    public final static String LN_pkcs9_challengePassword = "challengePassword";
    public final static String OBJ_pkcs9_challengePassword = OBJ_pkcs9 + ".7";

    public final static int    NID_pkcs9_unstructuredAddress = 55;
    public final static String LN_pkcs9_unstructuredAddress = "unstructuredAddress";
    public final static String OBJ_pkcs9_unstructuredAddress = OBJ_pkcs9 + ".8";

    public final static int    NID_pkcs9_extCertAttributes = 56;
    public final static String LN_pkcs9_extCertAttributes = "extendedCertificateAttributes";
    public final static String OBJ_pkcs9_extCertAttributes = OBJ_pkcs9 + ".9";

    public final static int    NID_netscape = 57;
    public final static String SN_netscape = "Netscape";
    public final static String LN_netscape = "Netscape Communications Corp.";
    public final static String OBJ_netscape = "2.16.840.1.113730";

    public final static int    NID_netscape_cert_extension = 58;
    public final static String SN_netscape_cert_extension = "nsCertExt";
    public final static String LN_netscape_cert_extension = "Netscape Certificate Extension";
    public final static String OBJ_netscape_cert_extension = OBJ_netscape + ".1";

    public final static int    NID_netscape_data_type = 59;
    public final static String SN_netscape_data_type = "nsDataType";
    public final static String LN_netscape_data_type = "Netscape Data Type";
    public final static String OBJ_netscape_data_type = OBJ_netscape + ".2";

    public final static int    NID_des_ede_cfb64 = 60;
    public final static String SN_des_ede_cfb64 = "DES-EDE-CFB";
    public final static String LN_des_ede_cfb64 = "des-ede-cfb";

    public final static int    NID_des_ede3_cfb64 = 61;
    public final static String SN_des_ede3_cfb64 = "DES-EDE3-CFB";
    public final static String LN_des_ede3_cfb64 = "des-ede3-cfb";

    public final static int    NID_des_ede_ofb64 = 62;
    public final static String SN_des_ede_ofb64 = "DES-EDE-OFB";
    public final static String LN_des_ede_ofb64 = "des-ede-ofb";

    public final static int    NID_des_ede3_ofb64 = 63;
    public final static String SN_des_ede3_ofb64 = "DES-EDE3-OFB";
    public final static String LN_des_ede3_ofb64 = "des-ede3-ofb";

    public final static int    NID_sha1 = 64;
    public final static String SN_sha1 = "SHA1";
    public final static String LN_sha1 = "sha1";
    public final static String OBJ_sha1 = OBJ_algorithm + ".26";
    public final static ASN1ObjectIdentifier OID_sha1 = new ASN1ObjectIdentifier(OBJ_sha1);

    public final static int    NID_sha1WithRSAEncryption = 65;
    public final static String SN_sha1WithRSAEncryption = "RSA-SHA1";
    public final static String LN_sha1WithRSAEncryption = "sha1WithRSAEncryption";
    public final static String OBJ_sha1WithRSAEncryption = OBJ_pkcs1 + ".5";

    public final static int    NID_dsaWithSHA = 66;
    public final static String SN_dsaWithSHA = "DSA-SHA";
    public final static String LN_dsaWithSHA = "dsaWithSHA";
    public final static String OBJ_dsaWithSHA = OBJ_algorithm + ".13";

    public final static int    NID_dsa_2 = 67;
    public final static String SN_dsa_2 = "DSA-old";
    public final static String LN_dsa_2 = "dsaEncryption-old";
    public final static String OBJ_dsa_2 = OBJ_algorithm + ".12";

    public final static int    NID_pbeWithSHA1AndRC2_CBC = 68;
    public final static String SN_pbeWithSHA1AndRC2_CBC = "PBE-SHA1-RC2-64";
    public final static String LN_pbeWithSHA1AndRC2_CBC = "pbeWithSHA1AndRC2-CBC";
    public final static String OBJ_pbeWithSHA1AndRC2_CBC = OBJ_pkcs5 + ".11";

    public final static int    NID_id_pbkdf2 = 69;
    public final static String LN_id_pbkdf2 = "PBKDF2";
    public final static String OBJ_id_pbkdf2 = OBJ_pkcs5 + ".12";

    public final static int    NID_dsaWithSHA1_2 = 70;
    public final static String SN_dsaWithSHA1_2 = "DSA-SHA1-old";
    public final static String LN_dsaWithSHA1_2 = "dsaWithSHA1-old";
    public final static String OBJ_dsaWithSHA1_2 = OBJ_algorithm + ".27";

    public final static int    NID_netscape_cert_type = 71;
    public final static String SN_netscape_cert_type = "nsCertType";
    public final static String LN_netscape_cert_type = "Netscape Cert Type";
    public final static String OBJ_netscape_cert_type = OBJ_netscape_cert_extension + ".1";

    public final static int    NID_netscape_base_url = 72;
    public final static String SN_netscape_base_url = "nsBaseUrl";
    public final static String LN_netscape_base_url = "Netscape Base Url";
    public final static String OBJ_netscape_base_url = OBJ_netscape_cert_extension + ".2";

    public final static int    NID_netscape_revocation_url = 73;
    public final static String SN_netscape_revocation_url = "nsRevocationUrl";
    public final static String LN_netscape_revocation_url = "Netscape Revocation Url";
    public final static String OBJ_netscape_revocation_url = OBJ_netscape_cert_extension + ".3";

    public final static int    NID_netscape_ca_revocation_url = 74;
    public final static String SN_netscape_ca_revocation_url = "nsCaRevocationUrl";
    public final static String LN_netscape_ca_revocation_url = "Netscape CA Revocation Url";
    public final static String OBJ_netscape_ca_revocation_url = OBJ_netscape_cert_extension + ".4";

    public final static int    NID_netscape_renewal_url = 75;
    public final static String SN_netscape_renewal_url = "nsRenewalUrl";
    public final static String LN_netscape_renewal_url = "Netscape Renewal Url";
    public final static String OBJ_netscape_renewal_url = OBJ_netscape_cert_extension + ".7";

    public final static int    NID_netscape_ca_policy_url = 76;
    public final static String SN_netscape_ca_policy_url = "nsCaPolicyUrl";
    public final static String LN_netscape_ca_policy_url = "Netscape CA Policy Url";
    public final static String OBJ_netscape_ca_policy_url = OBJ_netscape_cert_extension + ".8";

    public final static int    NID_netscape_ssl_server_name = 77;
    public final static String SN_netscape_ssl_server_name = "nsSslServerName";
    public final static String LN_netscape_ssl_server_name = "Netscape SSL Server Name";
    public final static String OBJ_netscape_ssl_server_name = OBJ_netscape_cert_extension + ".12";

    public final static int    NID_netscape_comment = 78;
    public final static String SN_netscape_comment = "nsComment";
    public final static String LN_netscape_comment = "Netscape Comment";
    public final static String OBJ_netscape_comment = OBJ_netscape_cert_extension + ".13";

    public final static int    NID_netscape_cert_sequence = 79;
    public final static String SN_netscape_cert_sequence = "nsCertSequence";
    public final static String LN_netscape_cert_sequence = "Netscape Certificate Sequence";
    public final static String OBJ_netscape_cert_sequence = OBJ_netscape_data_type + ".5";

    public final static int    NID_desx_cbc = 80;
    public final static String SN_desx_cbc = "DESX-CBC";
    public final static String LN_desx_cbc = "desx-cbc";

    public final static int    NID_id_ce = 81;
    public final static String SN_id_ce = "id-ce";
    public final static String OBJ_id_ce = OBJ_X500 + ".29";

    public final static int    NID_subject_key_identifier = 82;
    public final static String SN_subject_key_identifier = "subjectKeyIdentifier";
    public final static String LN_subject_key_identifier = "X509v3 Subject Key Identifier";
    public final static String OBJ_subject_key_identifier = OBJ_id_ce + ".14";

    public final static int    NID_key_usage = 83;
    public final static String SN_key_usage = "keyUsage";
    public final static String LN_key_usage = "X509v3 Key Usage";
    public final static String OBJ_key_usage = OBJ_id_ce + ".15";

    public final static int    NID_private_key_usage_period = 84;
    public final static String SN_private_key_usage_period = "privateKeyUsagePeriod";
    public final static String LN_private_key_usage_period = "X509v3 Private Key Usage Period";
    public final static String OBJ_private_key_usage_period = OBJ_id_ce + ".16";

    public final static int    NID_subject_alt_name = 85;
    public final static String SN_subject_alt_name = "subjectAltName";
    public final static String LN_subject_alt_name = "X509v3 Subject Alternative Name";
    public final static String OBJ_subject_alt_name = OBJ_id_ce + ".17";

    public final static int    NID_issuer_alt_name = 86;
    public final static String SN_issuer_alt_name = "issuerAltName";
    public final static String LN_issuer_alt_name = "X509v3 Issuer Alternative Name";
    public final static String OBJ_issuer_alt_name = OBJ_id_ce + ".18";

    public final static int    NID_basic_constraints = 87;
    public final static String SN_basic_constraints = "basicConstraints";
    public final static String LN_basic_constraints = "X509v3 Basic Constraints";
    public final static String OBJ_basic_constraints = OBJ_id_ce + ".19";

    public final static int    NID_crl_number = 88;
    public final static String SN_crl_number = "crlNumber";
    public final static String LN_crl_number = "X509v3 CRL Number";
    public final static String OBJ_crl_number = OBJ_id_ce + ".20";

    public final static int    NID_certificate_policies = 89;
    public final static String SN_certificate_policies = "certificatePolicies";
    public final static String LN_certificate_policies = "X509v3 Certificate Policies";
    public final static String OBJ_certificate_policies = OBJ_id_ce + ".32";

    public final static int    NID_authority_key_identifier = 90;
    public final static String SN_authority_key_identifier = "authorityKeyIdentifier";
    public final static String LN_authority_key_identifier = "X509v3 Authority Key Identifier";
    public final static String OBJ_authority_key_identifier = OBJ_id_ce + ".35";

    public final static int    NID_bf_cbc = 91;
    public final static String SN_bf_cbc = "BF-CBC";
    public final static String LN_bf_cbc = "bf-cbc";
    public final static String OBJ_bf_cbc = "1.3.6.1.4.1.3029.1.2";

    public final static int    NID_bf_ecb = 92;
    public final static String SN_bf_ecb = "BF-ECB";
    public final static String LN_bf_ecb = "bf-ecb";

    public final static int    NID_bf_cfb64 = 93;
    public final static String SN_bf_cfb64 = "BF-CFB";
    public final static String LN_bf_cfb64 = "bf-cfb";

    public final static int    NID_bf_ofb64 = 94;
    public final static String SN_bf_ofb64 = "BF-OFB";
    public final static String LN_bf_ofb64 = "bf-ofb";

    public final static int    NID_mdc2 = 95;
    public final static String SN_mdc2 = "MDC2";
    public final static String LN_mdc2 = "mdc2";
    public final static String OBJ_mdc2 = OBJ_X500algorithms + ".3.101";

    public final static int    NID_mdc2WithRSA = 96;
    public final static String SN_mdc2WithRSA = "RSA-MDC2";
    public final static String LN_mdc2WithRSA = "mdc2WithRSA";
    public final static String OBJ_mdc2WithRSA = OBJ_X500algorithms + ".3.100";

    public final static int    NID_rc4_40 = 97;
    public final static String SN_rc4_40 = "RC4-40";
    public final static String LN_rc4_40 = "rc4-40";

    public final static int    NID_rc2_40_cbc = 98;
    public final static String SN_rc2_40_cbc = "RC2-40-CBC";
    public final static String LN_rc2_40_cbc = "rc2-40-cbc";
    public final static String OBJ_rc2_40_cbc = OBJ_rsadsi + ".3.2";

    public final static int    NID_givenName = 99;
    public final static String SN_givenName = "GN";
    public final static String LN_givenName = "givenName";
    public final static String OBJ_givenName = OBJ_X509 + ".42";

    public final static int    NID_surname = 100;
    public final static String SN_surname = "SN";
    public final static String LN_surname = "surname";
    public final static String OBJ_surname = OBJ_X509 + ".4";

    public final static int    NID_initials = 101;
    public final static String LN_initials = "initials";
    public final static String OBJ_initials = OBJ_X509 + ".43";

    public final static int    NID_crl_distribution_points = 103;
    public final static String SN_crl_distribution_points = "crlDistributionPoints";
    public final static String LN_crl_distribution_points = "X509v3 CRL Distribution Points";
    public final static String OBJ_crl_distribution_points = OBJ_id_ce + ".31";

    public final static int    NID_md5WithRSA = 104;
    public final static String SN_md5WithRSA = "RSA-NP-MD5";
    public final static String LN_md5WithRSA = "md5WithRSA";
    public final static String OBJ_md5WithRSA = OBJ_algorithm + ".3";

    public final static int    NID_serialNumber = 105;
    public final static String LN_serialNumber = "serialNumber";
    public final static String OBJ_serialNumber = OBJ_X509 + ".5";

    public final static int    NID_title = 106;
    public final static String LN_title = "title";
    public final static String OBJ_title = OBJ_X509 + ".12";

    public final static int    NID_description = 107;
    public final static String LN_description = "description";
    public final static String OBJ_description = OBJ_X509 + ".13";

    public final static int    NID_cast5_cbc = 108;
    public final static String SN_cast5_cbc = "CAST5-CBC";
    public final static String LN_cast5_cbc = "cast5-cbc";
    public final static String OBJ_cast5_cbc = OBJ_ISO_US + ".113533.7.66.10";

    public final static int    NID_cast5_ecb = 109;
    public final static String SN_cast5_ecb = "CAST5-ECB";
    public final static String LN_cast5_ecb = "cast5-ecb";

    public final static int    NID_cast5_cfb64 = 110;
    public final static String SN_cast5_cfb64 = "CAST5-CFB";
    public final static String LN_cast5_cfb64 = "cast5-cfb";

    public final static int    NID_cast5_ofb64 = 111;
    public final static String SN_cast5_ofb64 = "CAST5-OFB";
    public final static String LN_cast5_ofb64 = "cast5-ofb";

    public final static int    NID_pbeWithMD5AndCast5_CBC = 112;
    public final static String LN_pbeWithMD5AndCast5_CBC = "pbeWithMD5AndCast5CBC";
    public final static String OBJ_pbeWithMD5AndCast5_CBC = OBJ_ISO_US + ".113533.7.66.12";

    public final static int    NID_X9cm = 185;
    public final static String SN_X9cm = "X9cm";
    public final static String LN_X9cm = "X9.57 CM ?";
    public final static String OBJ_X9cm = OBJ_X9_57 + ".4";

    public final static int    NID_dsaWithSHA1 = 113;
    public final static String SN_dsaWithSHA1 = "DSA-SHA1";
    public final static String LN_dsaWithSHA1 = "dsaWithSHA1";
    public final static String OBJ_dsaWithSHA1 = OBJ_X9cm + ".3";

    public final static int    NID_md5_sha1 = 114;
    public final static String SN_md5_sha1 = "MD5-SHA1";
    public final static String LN_md5_sha1 = "md5-sha1";

    public final static int    NID_sha1WithRSA = 115;
    public final static String SN_sha1WithRSA = "RSA-SHA1-2";
    public final static String LN_sha1WithRSA = "sha1WithRSA";
    public final static String OBJ_sha1WithRSA = OBJ_algorithm + ".29";

    public final static int    NID_dsa = 116;
    public final static String SN_dsa = "DSA";
    public final static String LN_dsa = "dsaEncryption";
    public final static String OBJ_dsa = OBJ_X9cm + ".1";
    public final static ASN1ObjectIdentifier OID_dsa = new ASN1ObjectIdentifier(OBJ_dsa);

    public final static int    NID_ripemd160 = 117;
    public final static String SN_ripemd160 = "RIPEMD160";
    public final static String LN_ripemd160 = "ripemd160";
    public final static String OBJ_ripemd160 = "1.3.36.3.2.1";

    public final static int    NID_ripemd160WithRSA = 119;
    public final static String SN_ripemd160WithRSA = "RSA-RIPEMD160";
    public final static String LN_ripemd160WithRSA = "ripemd160WithRSA";
    public final static String OBJ_ripemd160WithRSA = "1.3.36.3.3.1.2";

    public final static int    NID_rc5_cbc = 120;
    public final static String SN_rc5_cbc = "RC5-CBC";
    public final static String LN_rc5_cbc = "rc5-cbc";
    public final static String OBJ_rc5_cbc = OBJ_rsadsi + ".3.8";

    public final static int    NID_rc5_ecb = 121;
    public final static String SN_rc5_ecb = "RC5-ECB";
    public final static String LN_rc5_ecb = "rc5-ecb";

    public final static int    NID_rc5_cfb64 = 122;
    public final static String SN_rc5_cfb64 = "RC5-CFB";
    public final static String LN_rc5_cfb64 = "rc5-cfb";

    public final static int    NID_rc5_ofb64 = 123;
    public final static String SN_rc5_ofb64 = "RC5-OFB";
    public final static String LN_rc5_ofb64 = "rc5-ofb";

    public final static int    NID_rle_compression = 124;
    public final static String SN_rle_compression = "RLE";
    public final static String LN_rle_compression = "run length compression";
    public final static String OBJ_rle_compression = "1.1.1.1.666.1";

    public final static int    NID_zlib_compression = 125;
    public final static String SN_zlib_compression = "ZLIB";
    public final static String LN_zlib_compression = "zlib compression";
    public final static String OBJ_zlib_compression = "1.1.1.1.666.2";

    public final static int    NID_ext_key_usage = 126;
    public final static String SN_ext_key_usage = "extendedKeyUsage";
    public final static String LN_ext_key_usage = "X509v3 Extended Key Usage";
    public final static String OBJ_ext_key_usage = OBJ_id_ce + ".37";

    public final static int    NID_id_kp = 128;
    public final static String SN_id_kp = "id-kp";
    public final static String OBJ_id_kp = OBJ_id_pkix + ".3";

    public final static int    NID_server_auth = 129;
    public final static String SN_server_auth = "serverAuth";
    public final static String LN_server_auth = "TLS Web Server Authentication";
    public final static String OBJ_server_auth = OBJ_id_kp + ".1";

    public final static int    NID_client_auth = 130;
    public final static String SN_client_auth = "clientAuth";
    public final static String LN_client_auth = "TLS Web Client Authentication";
    public final static String OBJ_client_auth = OBJ_id_kp + ".2";

    public final static int    NID_code_sign = 131;
    public final static String SN_code_sign = "codeSigning";
    public final static String LN_code_sign = "Code Signing";
    public final static String OBJ_code_sign = OBJ_id_kp + ".3";

    public final static int    NID_email_protect = 132;
    public final static String SN_email_protect = "emailProtection";
    public final static String LN_email_protect = "E-mail Protection";
    public final static String OBJ_email_protect = OBJ_id_kp + ".4";

    public final static int    NID_time_stamp = 133;
    public final static String SN_time_stamp = "timeStamping";
    public final static String LN_time_stamp = "Time Stamping";
    public final static String OBJ_time_stamp = OBJ_id_kp + ".8";

    public final static int    NID_ms_code_ind = 134;
    public final static String SN_ms_code_ind = "msCodeInd";
    public final static String LN_ms_code_ind = "Microsoft Individual Code Signing";
    public final static String OBJ_ms_code_ind = "1.3.6.1.4.1.311.2.1.21";

    public final static int    NID_ms_code_com = 135;
    public final static String SN_ms_code_com = "msCodeCom";
    public final static String LN_ms_code_com = "Microsoft Commercial Code Signing";
    public final static String OBJ_ms_code_com = "1.3.6.1.4.1.311.2.1.22";

    public final static int    NID_ms_ctl_sign = 136;
    public final static String SN_ms_ctl_sign = "msCTLSign";
    public final static String LN_ms_ctl_sign = "Microsoft Trust List Signing";
    public final static String OBJ_ms_ctl_sign = "1.3.6.1.4.1.311.10.3.1";

    public final static int    NID_ms_sgc = 137;
    public final static String SN_ms_sgc = "msSGC";
    public final static String LN_ms_sgc = "Microsoft Server Gated Crypto";
    public final static String OBJ_ms_sgc = "1.3.6.1.4.1.311.10.3.3";

    public final static int    NID_ms_efs = 138;
    public final static String SN_ms_efs = "msEFS";
    public final static String LN_ms_efs = "Microsoft Encrypted File System";
    public final static String OBJ_ms_efs = "1.3.6.1.4.1.311.10.3.4";

    public final static int    NID_ns_sgc = 139;
    public final static String SN_ns_sgc = "nsSGC";
    public final static String LN_ns_sgc = "Netscape Server Gated Crypto";
    public final static String OBJ_ns_sgc = OBJ_netscape + ".4.1";

    public final static int    NID_delta_crl = 140;
    public final static String SN_delta_crl = "deltaCRL";
    public final static String LN_delta_crl = "X509v3 Delta CRL Indicator";
    public final static String OBJ_delta_crl = OBJ_id_ce + ".27";

    public final static int    NID_crl_reason = 141;
    public final static String SN_crl_reason = "CRLReason";
    public final static String LN_crl_reason = "X509v3 CRL Reason Code";
    public final static String OBJ_crl_reason = OBJ_id_ce + ".21";

    public final static int    NID_invalidity_date = 142;
    public final static String SN_invalidity_date = "invalidityDate";
    public final static String LN_invalidity_date = "Invalidity Date";
    public final static String OBJ_invalidity_date = OBJ_id_ce + ".24";

    public final static int    NID_sxnet = 143;
    public final static String SN_sxnet = "SXNetID";
    public final static String LN_sxnet = "Strong Extranet ID";
    public final static String OBJ_sxnet = "1.3.101.1.4.1";

    public final static String OBJ_pkcs12_pbeids = OBJ_pkcs12 + ".1";

    public final static int    NID_pbe_WithSHA1And128BitRC4 = 144;
    public final static String SN_pbe_WithSHA1And128BitRC4 = "PBE-SHA1-RC4-128";
    public final static String LN_pbe_WithSHA1And128BitRC4 = "pbeWithSHA1And128BitRC4";
    public final static String OBJ_pbe_WithSHA1And128BitRC4 = OBJ_pkcs12_pbeids + ".1";

    public final static int    NID_pbe_WithSHA1And40BitRC4 = 145;
    public final static String SN_pbe_WithSHA1And40BitRC4 = "PBE-SHA1-RC4-40";
    public final static String LN_pbe_WithSHA1And40BitRC4 = "pbeWithSHA1And40BitRC4";
    public final static String OBJ_pbe_WithSHA1And40BitRC4 = OBJ_pkcs12_pbeids + ".2";

    public final static int    NID_pbe_WithSHA1And3_Key_TripleDES_CBC = 146;
    public final static String SN_pbe_WithSHA1And3_Key_TripleDES_CBC = "PBE-SHA1-3DES";
    public final static String LN_pbe_WithSHA1And3_Key_TripleDES_CBC = "pbeWithSHA1And3-KeyTripleDES-CBC";
    public final static String OBJ_pbe_WithSHA1And3_Key_TripleDES_CBC = OBJ_pkcs12_pbeids + ".3";

    public final static int    NID_pbe_WithSHA1And2_Key_TripleDES_CBC = 147;
    public final static String SN_pbe_WithSHA1And2_Key_TripleDES_CBC = "PBE-SHA1-2DES";
    public final static String LN_pbe_WithSHA1And2_Key_TripleDES_CBC = "pbeWithSHA1And2-KeyTripleDES-CBC";
    public final static String OBJ_pbe_WithSHA1And2_Key_TripleDES_CBC = OBJ_pkcs12_pbeids + ".4";

    public final static int    NID_pbe_WithSHA1And128BitRC2_CBC = 148;
    public final static String SN_pbe_WithSHA1And128BitRC2_CBC = "PBE-SHA1-RC2-128";
    public final static String LN_pbe_WithSHA1And128BitRC2_CBC = "pbeWithSHA1And128BitRC2-CBC";
    public final static String OBJ_pbe_WithSHA1And128BitRC2_CBC = OBJ_pkcs12_pbeids + ".5";

    public final static int    NID_pbe_WithSHA1And40BitRC2_CBC = 149;
    public final static String SN_pbe_WithSHA1And40BitRC2_CBC = "PBE-SHA1-RC2-40";
    public final static String LN_pbe_WithSHA1And40BitRC2_CBC = "pbeWithSHA1And40BitRC2-CBC";
    public final static String OBJ_pbe_WithSHA1And40BitRC2_CBC = OBJ_pkcs12_pbeids + ".6";

    public final static int    NID_keyBag = 150;
    public final static String LN_keyBag = "keyBag";
    public final static String OBJ_keyBag = OBJ_pkcs12_BagIds + ".1";

    public final static int    NID_pkcs8ShroudedKeyBag = 151;
    public final static String LN_pkcs8ShroudedKeyBag = "pkcs8ShroudedKeyBag";
    public final static String OBJ_pkcs8ShroudedKeyBag = OBJ_pkcs12_BagIds + ".2";

    public final static int    NID_certBag = 152;
    public final static String LN_certBag = "certBag";
    public final static String OBJ_certBag = OBJ_pkcs12_BagIds + ".3";

    public final static int    NID_crlBag = 153;
    public final static String LN_crlBag = "crlBag";
    public final static String OBJ_crlBag = OBJ_pkcs12_BagIds + ".4";

    public final static int    NID_secretBag = 154;
    public final static String LN_secretBag = "secretBag";
    public final static String OBJ_secretBag = OBJ_pkcs12_BagIds + ".5";

    public final static int    NID_safeContentsBag = 155;
    public final static String LN_safeContentsBag = "safeContentsBag";
    public final static String OBJ_safeContentsBag = OBJ_pkcs12_BagIds + ".6";

    public final static int    NID_friendlyName = 156;
    public final static String LN_friendlyName = "friendlyName";
    public final static String OBJ_friendlyName = OBJ_pkcs9 + ".20";

    public final static int    NID_localKeyID = 157;
    public final static String LN_localKeyID = "localKeyID";
    public final static String OBJ_localKeyID = OBJ_pkcs9 + ".21";

    public final static int    NID_x509Certificate = 158;
    public final static String LN_x509Certificate = "x509Certificate";
    public final static String OBJ_x509Certificate = OBJ_certTypes + ".1";

    public final static int    NID_sdsiCertificate = 159;
    public final static String LN_sdsiCertificate = "sdsiCertificate";
    public final static String OBJ_sdsiCertificate = OBJ_certTypes + ".2";

    public final static int    NID_x509Crl = 160;
    public final static String LN_x509Crl = "x509Crl";
    public final static String OBJ_x509Crl = OBJ_crlTypes + ".1";

    public final static int    NID_pbes2 = 161;
    public final static String LN_pbes2 = "PBES2";
    public final static String OBJ_pbes2 = OBJ_pkcs5 + ".13";

    public final static int    NID_pbmac1 = 162;
    public final static String LN_pbmac1 = "PBMAC1";
    public final static String OBJ_pbmac1 = OBJ_pkcs5 + ".14";

    public final static int    NID_hmacWithSHA1 = 163;
    public final static String LN_hmacWithSHA1 = "hmacWithSHA1";
    public final static String OBJ_hmacWithSHA1 = OBJ_rsadsi + ".2.7";

    public final static int    NID_id_qt = 259;
    public final static String SN_id_qt = "id-qt";
    public final static String OBJ_id_qt = OBJ_id_pkix + ".2";

    public final static int    NID_id_qt_cps = 164;
    public final static String SN_id_qt_cps = "id-qt-cps";
    public final static String LN_id_qt_cps = "Policy Qualifier CPS";
    public final static String OBJ_id_qt_cps = OBJ_id_qt + ".1";

    public final static int    NID_id_qt_unotice = 165;
    public final static String SN_id_qt_unotice = "id-qt-unotice";
    public final static String LN_id_qt_unotice = "Policy Qualifier User Notice";
    public final static String OBJ_id_qt_unotice = OBJ_id_qt + ".2";

    public final static int    NID_rc2_64_cbc = 166;
    public final static String SN_rc2_64_cbc = "RC2-64-CBC";
    public final static String LN_rc2_64_cbc = "rc2-64-cbc";

    public final static int    NID_SMIMECapabilities = 167;
    public final static String SN_SMIMECapabilities = "SMIME-CAPS";
    public final static String LN_SMIMECapabilities = "S/MIME Capabilities";
    public final static String OBJ_SMIMECapabilities = OBJ_pkcs9 + ".15";

    public final static int    NID_pbeWithMD2AndRC2_CBC = 168;
    public final static String SN_pbeWithMD2AndRC2_CBC = "PBE-MD2-RC2-64";
    public final static String LN_pbeWithMD2AndRC2_CBC = "pbeWithMD2AndRC2-CBC";
    public final static String OBJ_pbeWithMD2AndRC2_CBC = OBJ_pkcs5 + ".4";

    public final static int    NID_pbeWithMD5AndRC2_CBC = 169;
    public final static String SN_pbeWithMD5AndRC2_CBC = "PBE-MD5-RC2-64";
    public final static String LN_pbeWithMD5AndRC2_CBC = "pbeWithMD5AndRC2-CBC";
    public final static String OBJ_pbeWithMD5AndRC2_CBC = OBJ_pkcs5 + ".6";

    public final static int    NID_pbeWithSHA1AndDES_CBC = 170;
    public final static String SN_pbeWithSHA1AndDES_CBC = "PBE-SHA1-DES";
    public final static String LN_pbeWithSHA1AndDES_CBC = "pbeWithSHA1AndDES-CBC";
    public final static String OBJ_pbeWithSHA1AndDES_CBC = OBJ_pkcs5 + ".10";

    public final static int    NID_ms_ext_req = 171;
    public final static String SN_ms_ext_req = "msExtReq";
    public final static String LN_ms_ext_req = "Microsoft Extension Request";
    public final static String OBJ_ms_ext_req = "1.3.6.1.4.1.311.2.1.14";

    public final static int    NID_ext_req = 172;
    public final static String SN_ext_req = "extReq";
    public final static String LN_ext_req = "Extension Request";
    public final static String OBJ_ext_req = OBJ_pkcs9 + ".14";

    public final static int    NID_name = 173;
    public final static String SN_name = "name";
    public final static String LN_name = "name";
    public final static String OBJ_name = OBJ_X509 + ".41";

    public final static int    NID_dnQualifier = 174;
    public final static String SN_dnQualifier = "dnQualifier";
    public final static String LN_dnQualifier = "dnQualifier";
    public final static String OBJ_dnQualifier = OBJ_X509 + ".46";

    public final static int    NID_id_pe = 175;
    public final static String SN_id_pe = "id-pe";
    public final static String OBJ_id_pe = OBJ_id_pkix + ".1";

    public final static int    NID_info_access = 177;
    public final static String SN_info_access = "authorityInfoAccess";
    public final static String LN_info_access = "Authority Information Access";
    public final static String OBJ_info_access = OBJ_id_pe + ".1";

    public final static int    NID_ad_ca_issuers = 179;
    public final static String SN_ad_ca_issuers = "caIssuers";
    public final static String LN_ad_ca_issuers = "CA Issuers";
    public final static String OBJ_ad_ca_issuers = OBJ_id_ad + ".2";

    public final static int    NID_OCSP_sign = 180;
    public final static String SN_OCSP_sign = "OCSPSigning";
    public final static String LN_OCSP_sign = "OCSP Signing";
    public final static String OBJ_OCSP_sign = OBJ_id_kp + ".9";

    public final static int    NID_SMIME = 188;
    public final static String SN_SMIME = "SMIME";
    public final static String LN_SMIME = "S/MIME";
    public final static String OBJ_SMIME = OBJ_pkcs9 + ".16";

    public final static int    NID_id_smime_mod = 189;
    public final static String SN_id_smime_mod = "id-smime-mod";
    public final static String OBJ_id_smime_mod = OBJ_SMIME + ".0";

    public final static int    NID_id_smime_ct = 190;
    public final static String SN_id_smime_ct = "id-smime-ct";
    public final static String OBJ_id_smime_ct = OBJ_SMIME + ".1";

    public final static int    NID_id_smime_aa = 191;
    public final static String SN_id_smime_aa = "id-smime-aa";
    public final static String OBJ_id_smime_aa = OBJ_SMIME + ".2";

    public final static int    NID_id_smime_alg = 192;
    public final static String SN_id_smime_alg = "id-smime-alg";
    public final static String OBJ_id_smime_alg = OBJ_SMIME + ".3";

    public final static int    NID_id_smime_cd = 193;
    public final static String SN_id_smime_cd = "id-smime-cd";
    public final static String OBJ_id_smime_cd = OBJ_SMIME + ".4";

    public final static int    NID_id_smime_spq = 194;
    public final static String SN_id_smime_spq = "id-smime-spq";
    public final static String OBJ_id_smime_spq = OBJ_SMIME + ".5";

    public final static int    NID_id_smime_cti = 195;
    public final static String SN_id_smime_cti = "id-smime-cti";
    public final static String OBJ_id_smime_cti = OBJ_SMIME + ".6";

    public final static int    NID_id_smime_mod_cms = 196;
    public final static String SN_id_smime_mod_cms = "id-smime-mod-cms";
    public final static String OBJ_id_smime_mod_cms = OBJ_id_smime_mod + ".1";

    public final static int    NID_id_smime_mod_ess = 197;
    public final static String SN_id_smime_mod_ess = "id-smime-mod-ess";
    public final static String OBJ_id_smime_mod_ess = OBJ_id_smime_mod + ".2";

    public final static int    NID_id_smime_mod_oid = 198;
    public final static String SN_id_smime_mod_oid = "id-smime-mod-oid";
    public final static String OBJ_id_smime_mod_oid = OBJ_id_smime_mod + ".3";

    public final static int    NID_id_smime_mod_msg_v3 = 199;
    public final static String SN_id_smime_mod_msg_v3 = "id-smime-mod-msg-v3";
    public final static String OBJ_id_smime_mod_msg_v3 = OBJ_id_smime_mod + ".4";

    public final static int    NID_id_smime_mod_ets_eSignature_88 = 200;
    public final static String SN_id_smime_mod_ets_eSignature_88 = "id-smime-mod-ets-eSignature-88";
    public final static String OBJ_id_smime_mod_ets_eSignature_88 = OBJ_id_smime_mod + ".5";

    public final static int    NID_id_smime_mod_ets_eSignature_97 = 201;
    public final static String SN_id_smime_mod_ets_eSignature_97 = "id-smime-mod-ets-eSignature-97";
    public final static String OBJ_id_smime_mod_ets_eSignature_97 = OBJ_id_smime_mod + ".6";

    public final static int    NID_id_smime_mod_ets_eSigPolicy_88 = 202;
    public final static String SN_id_smime_mod_ets_eSigPolicy_88 = "id-smime-mod-ets-eSigPolicy-88";
    public final static String OBJ_id_smime_mod_ets_eSigPolicy_88 = OBJ_id_smime_mod + ".7";

    public final static int    NID_id_smime_mod_ets_eSigPolicy_97 = 203;
    public final static String SN_id_smime_mod_ets_eSigPolicy_97 = "id-smime-mod-ets-eSigPolicy-97";
    public final static String OBJ_id_smime_mod_ets_eSigPolicy_97 = OBJ_id_smime_mod + ".8";

    public final static int    NID_id_smime_ct_receipt = 204;
    public final static String SN_id_smime_ct_receipt = "id-smime-ct-receipt";
    public final static String OBJ_id_smime_ct_receipt = OBJ_id_smime_ct + ".1";

    public final static int    NID_id_smime_ct_authData = 205;
    public final static String SN_id_smime_ct_authData = "id-smime-ct-authData";
    public final static String OBJ_id_smime_ct_authData = OBJ_id_smime_ct + ".2";

    public final static int    NID_id_smime_ct_publishCert = 206;
    public final static String SN_id_smime_ct_publishCert = "id-smime-ct-publishCert";
    public final static String OBJ_id_smime_ct_publishCert = OBJ_id_smime_ct + ".3";

    public final static int    NID_id_smime_ct_TSTInfo = 207;
    public final static String SN_id_smime_ct_TSTInfo = "id-smime-ct-TSTInfo";
    public final static String OBJ_id_smime_ct_TSTInfo = OBJ_id_smime_ct + ".4";

    public final static int    NID_id_smime_ct_TDTInfo = 208;
    public final static String SN_id_smime_ct_TDTInfo = "id-smime-ct-TDTInfo";
    public final static String OBJ_id_smime_ct_TDTInfo = OBJ_id_smime_ct + ".5";

    public final static int    NID_id_smime_ct_contentInfo = 209;
    public final static String SN_id_smime_ct_contentInfo = "id-smime-ct-contentInfo";
    public final static String OBJ_id_smime_ct_contentInfo = OBJ_id_smime_ct + ".6";

    public final static int    NID_id_smime_ct_DVCSRequestData = 210;
    public final static String SN_id_smime_ct_DVCSRequestData = "id-smime-ct-DVCSRequestData";
    public final static String OBJ_id_smime_ct_DVCSRequestData = OBJ_id_smime_ct + ".7";

    public final static int    NID_id_smime_ct_DVCSResponseData = 211;
    public final static String SN_id_smime_ct_DVCSResponseData = "id-smime-ct-DVCSResponseData";
    public final static String OBJ_id_smime_ct_DVCSResponseData = OBJ_id_smime_ct + ".8";

    public final static int    NID_id_smime_ct_compressedData  =    786;
    public final static String SN_id_smime_ct_compressedData = "id-smime-ct-compressedData";
    public final static String OBJ_id_smime_ct_compressedData  =  OBJ_id_smime_ct + ".9";

    public final static int    NID_id_smime_aa_receiptRequest = 212;
    public final static String SN_id_smime_aa_receiptRequest = "id-smime-aa-receiptRequest";
    public final static String OBJ_id_smime_aa_receiptRequest = OBJ_id_smime_aa + ".1";

    public final static int    NID_id_smime_aa_securityLabel = 213;
    public final static String SN_id_smime_aa_securityLabel = "id-smime-aa-securityLabel";
    public final static String OBJ_id_smime_aa_securityLabel = OBJ_id_smime_aa + ".2";

    public final static int    NID_id_smime_aa_mlExpandHistory = 214;
    public final static String SN_id_smime_aa_mlExpandHistory = "id-smime-aa-mlExpandHistory";
    public final static String OBJ_id_smime_aa_mlExpandHistory = OBJ_id_smime_aa + ".3";

    public final static int    NID_id_smime_aa_contentHint = 215;
    public final static String SN_id_smime_aa_contentHint = "id-smime-aa-contentHint";
    public final static String OBJ_id_smime_aa_contentHint = OBJ_id_smime_aa + ".4";

    public final static int    NID_id_smime_aa_msgSigDigest = 216;
    public final static String SN_id_smime_aa_msgSigDigest = "id-smime-aa-msgSigDigest";
    public final static String OBJ_id_smime_aa_msgSigDigest = OBJ_id_smime_aa + ".5";

    public final static int    NID_id_smime_aa_encapContentType = 217;
    public final static String SN_id_smime_aa_encapContentType = "id-smime-aa-encapContentType";
    public final static String OBJ_id_smime_aa_encapContentType = OBJ_id_smime_aa + ".6";

    public final static int    NID_id_smime_aa_contentIdentifier = 218;
    public final static String SN_id_smime_aa_contentIdentifier = "id-smime-aa-contentIdentifier";
    public final static String OBJ_id_smime_aa_contentIdentifier = OBJ_id_smime_aa + ".7";

    public final static int    NID_id_smime_aa_macValue = 219;
    public final static String SN_id_smime_aa_macValue = "id-smime-aa-macValue";
    public final static String OBJ_id_smime_aa_macValue = OBJ_id_smime_aa + ".8";

    public final static int    NID_id_smime_aa_equivalentLabels = 220;
    public final static String SN_id_smime_aa_equivalentLabels = "id-smime-aa-equivalentLabels";
    public final static String OBJ_id_smime_aa_equivalentLabels = OBJ_id_smime_aa + ".9";

    public final static int    NID_id_smime_aa_contentReference = 221;
    public final static String SN_id_smime_aa_contentReference = "id-smime-aa-contentReference";
    public final static String OBJ_id_smime_aa_contentReference = OBJ_id_smime_aa + ".10";

    public final static int    NID_id_smime_aa_encrypKeyPref = 222;
    public final static String SN_id_smime_aa_encrypKeyPref = "id-smime-aa-encrypKeyPref";
    public final static String OBJ_id_smime_aa_encrypKeyPref = OBJ_id_smime_aa + ".11";

    public final static int    NID_id_smime_aa_signingCertificate = 223;
    public final static String SN_id_smime_aa_signingCertificate = "id-smime-aa-signingCertificate";
    public final static String OBJ_id_smime_aa_signingCertificate = OBJ_id_smime_aa + ".12";

    public final static int    NID_id_smime_aa_smimeEncryptCerts = 224;
    public final static String SN_id_smime_aa_smimeEncryptCerts = "id-smime-aa-smimeEncryptCerts";
    public final static String OBJ_id_smime_aa_smimeEncryptCerts = OBJ_id_smime_aa + ".13";

    public final static int    NID_id_smime_aa_timeStampToken = 225;
    public final static String SN_id_smime_aa_timeStampToken = "id-smime-aa-timeStampToken";
    public final static String OBJ_id_smime_aa_timeStampToken = OBJ_id_smime_aa + ".14";

    public final static int    NID_id_smime_aa_ets_sigPolicyId = 226;
    public final static String SN_id_smime_aa_ets_sigPolicyId = "id-smime-aa-ets-sigPolicyId";
    public final static String OBJ_id_smime_aa_ets_sigPolicyId = OBJ_id_smime_aa + ".15";

    public final static int    NID_id_smime_aa_ets_commitmentType = 227;
    public final static String SN_id_smime_aa_ets_commitmentType = "id-smime-aa-ets-commitmentType";
    public final static String OBJ_id_smime_aa_ets_commitmentType = OBJ_id_smime_aa + ".16";

    public final static int    NID_id_smime_aa_ets_signerLocation = 228;
    public final static String SN_id_smime_aa_ets_signerLocation = "id-smime-aa-ets-signerLocation";
    public final static String OBJ_id_smime_aa_ets_signerLocation = OBJ_id_smime_aa + ".17";

    public final static int    NID_id_smime_aa_ets_signerAttr = 229;
    public final static String SN_id_smime_aa_ets_signerAttr = "id-smime-aa-ets-signerAttr";
    public final static String OBJ_id_smime_aa_ets_signerAttr = OBJ_id_smime_aa + ".18";

    public final static int    NID_id_smime_aa_ets_otherSigCert = 230;
    public final static String SN_id_smime_aa_ets_otherSigCert = "id-smime-aa-ets-otherSigCert";
    public final static String OBJ_id_smime_aa_ets_otherSigCert = OBJ_id_smime_aa + ".19";

    public final static int    NID_id_smime_aa_ets_contentTimestamp = 231;
    public final static String SN_id_smime_aa_ets_contentTimestamp = "id-smime-aa-ets-contentTimestamp";
    public final static String OBJ_id_smime_aa_ets_contentTimestamp = OBJ_id_smime_aa + ".20";

    public final static int    NID_id_smime_aa_ets_CertificateRefs = 232;
    public final static String SN_id_smime_aa_ets_CertificateRefs = "id-smime-aa-ets-CertificateRefs";
    public final static String OBJ_id_smime_aa_ets_CertificateRefs = OBJ_id_smime_aa + ".21";

    public final static int    NID_id_smime_aa_ets_RevocationRefs = 233;
    public final static String SN_id_smime_aa_ets_RevocationRefs = "id-smime-aa-ets-RevocationRefs";
    public final static String OBJ_id_smime_aa_ets_RevocationRefs = OBJ_id_smime_aa + ".22";

    public final static int    NID_id_smime_aa_ets_certValues = 234;
    public final static String SN_id_smime_aa_ets_certValues = "id-smime-aa-ets-certValues";
    public final static String OBJ_id_smime_aa_ets_certValues = OBJ_id_smime_aa + ".23";

    public final static int    NID_id_smime_aa_ets_revocationValues = 235;
    public final static String SN_id_smime_aa_ets_revocationValues = "id-smime-aa-ets-revocationValues";
    public final static String OBJ_id_smime_aa_ets_revocationValues = OBJ_id_smime_aa + ".24";

    public final static int    NID_id_smime_aa_ets_escTimeStamp = 236;
    public final static String SN_id_smime_aa_ets_escTimeStamp = "id-smime-aa-ets-escTimeStamp";
    public final static String OBJ_id_smime_aa_ets_escTimeStamp = OBJ_id_smime_aa + ".25";

    public final static int    NID_id_smime_aa_ets_certCRLTimestamp = 237;
    public final static String SN_id_smime_aa_ets_certCRLTimestamp = "id-smime-aa-ets-certCRLTimestamp";
    public final static String OBJ_id_smime_aa_ets_certCRLTimestamp = OBJ_id_smime_aa + ".26";

    public final static int    NID_id_smime_aa_ets_archiveTimeStamp = 238;
    public final static String SN_id_smime_aa_ets_archiveTimeStamp = "id-smime-aa-ets-archiveTimeStamp";
    public final static String OBJ_id_smime_aa_ets_archiveTimeStamp = OBJ_id_smime_aa + ".27";

    public final static int    NID_id_smime_aa_signatureType = 239;
    public final static String SN_id_smime_aa_signatureType = "id-smime-aa-signatureType";
    public final static String OBJ_id_smime_aa_signatureType = OBJ_id_smime_aa + ".28";

    public final static int    NID_id_smime_aa_dvcs_dvc = 240;
    public final static String SN_id_smime_aa_dvcs_dvc = "id-smime-aa-dvcs-dvc";
    public final static String OBJ_id_smime_aa_dvcs_dvc = OBJ_id_smime_aa + ".29";

    public final static int    NID_id_smime_alg_ESDHwith3DES = 241;
    public final static String SN_id_smime_alg_ESDHwith3DES = "id-smime-alg-ESDHwith3DES";
    public final static String OBJ_id_smime_alg_ESDHwith3DES = OBJ_id_smime_alg + ".1";

    public final static int    NID_id_smime_alg_ESDHwithRC2 = 242;
    public final static String SN_id_smime_alg_ESDHwithRC2 = "id-smime-alg-ESDHwithRC2";
    public final static String OBJ_id_smime_alg_ESDHwithRC2 = OBJ_id_smime_alg + ".2";

    public final static int    NID_id_smime_alg_3DESwrap = 243;
    public final static String SN_id_smime_alg_3DESwrap = "id-smime-alg-3DESwrap";
    public final static String OBJ_id_smime_alg_3DESwrap = OBJ_id_smime_alg + ".3";

    public final static int    NID_id_smime_alg_RC2wrap = 244;
    public final static String SN_id_smime_alg_RC2wrap = "id-smime-alg-RC2wrap";
    public final static String OBJ_id_smime_alg_RC2wrap = OBJ_id_smime_alg + ".4";

    public final static int    NID_id_smime_alg_ESDH = 245;
    public final static String SN_id_smime_alg_ESDH = "id-smime-alg-ESDH";
    public final static String OBJ_id_smime_alg_ESDH = OBJ_id_smime_alg + ".5";

    public final static int    NID_id_smime_alg_CMS3DESwrap = 246;
    public final static String SN_id_smime_alg_CMS3DESwrap = "id-smime-alg-CMS3DESwrap";
    public final static String OBJ_id_smime_alg_CMS3DESwrap = OBJ_id_smime_alg + ".6";

    public final static int    NID_id_smime_alg_CMSRC2wrap = 247;
    public final static String SN_id_smime_alg_CMSRC2wrap = "id-smime-alg-CMSRC2wrap";
    public final static String OBJ_id_smime_alg_CMSRC2wrap = OBJ_id_smime_alg + ".7";

    public final static int    NID_id_smime_cd_ldap = 248;
    public final static String SN_id_smime_cd_ldap = "id-smime-cd-ldap";
    public final static String OBJ_id_smime_cd_ldap = OBJ_id_smime_cd + ".1";

    public final static int    NID_id_smime_spq_ets_sqt_uri = 249;
    public final static String SN_id_smime_spq_ets_sqt_uri = "id-smime-spq-ets-sqt-uri";
    public final static String OBJ_id_smime_spq_ets_sqt_uri = OBJ_id_smime_spq + ".1";

    public final static int    NID_id_smime_spq_ets_sqt_unotice = 250;
    public final static String SN_id_smime_spq_ets_sqt_unotice = "id-smime-spq-ets-sqt-unotice";
    public final static String OBJ_id_smime_spq_ets_sqt_unotice = OBJ_id_smime_spq + ".2";

    public final static int    NID_id_smime_cti_ets_proofOfOrigin = 251;
    public final static String SN_id_smime_cti_ets_proofOfOrigin = "id-smime-cti-ets-proofOfOrigin";
    public final static String OBJ_id_smime_cti_ets_proofOfOrigin = OBJ_id_smime_cti + ".1";

    public final static int    NID_id_smime_cti_ets_proofOfReceipt = 252;
    public final static String SN_id_smime_cti_ets_proofOfReceipt = "id-smime-cti-ets-proofOfReceipt";
    public final static String OBJ_id_smime_cti_ets_proofOfReceipt = OBJ_id_smime_cti + ".2";

    public final static int    NID_id_smime_cti_ets_proofOfDelivery = 253;
    public final static String SN_id_smime_cti_ets_proofOfDelivery = "id-smime-cti-ets-proofOfDelivery";
    public final static String OBJ_id_smime_cti_ets_proofOfDelivery = OBJ_id_smime_cti + ".3";

    public final static int    NID_id_smime_cti_ets_proofOfSender = 254;
    public final static String SN_id_smime_cti_ets_proofOfSender = "id-smime-cti-ets-proofOfSender";
    public final static String OBJ_id_smime_cti_ets_proofOfSender = OBJ_id_smime_cti + ".4";

    public final static int    NID_id_smime_cti_ets_proofOfApproval = 255;
    public final static String SN_id_smime_cti_ets_proofOfApproval = "id-smime-cti-ets-proofOfApproval";
    public final static String OBJ_id_smime_cti_ets_proofOfApproval = OBJ_id_smime_cti + ".5";

    public final static int    NID_id_smime_cti_ets_proofOfCreation = 256;
    public final static String SN_id_smime_cti_ets_proofOfCreation = "id-smime-cti-ets-proofOfCreation";
    public final static String OBJ_id_smime_cti_ets_proofOfCreation = OBJ_id_smime_cti + ".6";

    public final static int    NID_md4 = 257;
    public final static String SN_md4 = "MD4";
    public final static String LN_md4 = "md4";
    public final static String OBJ_md4 = OBJ_rsadsi + ".2.4";

    public final static int    NID_id_pkix_mod = 258;
    public final static String SN_id_pkix_mod = "id-pkix-mod";
    public final static String OBJ_id_pkix_mod = OBJ_id_pkix + ".0";

    public final static int    NID_id_it = 260;
    public final static String SN_id_it = "id-it";
    public final static String OBJ_id_it = OBJ_id_pkix + ".4";

    public final static int    NID_id_pkip = 261;
    public final static String SN_id_pkip = "id-pkip";
    public final static String OBJ_id_pkip = OBJ_id_pkix + ".5";

    public final static int    NID_id_alg = 262;
    public final static String SN_id_alg = "id-alg";
    public final static String OBJ_id_alg = OBJ_id_pkix + ".6";

    public final static int    NID_id_cmc = 263;
    public final static String SN_id_cmc = "id-cmc";
    public final static String OBJ_id_cmc = OBJ_id_pkix + ".7";

    public final static int    NID_id_on = 264;
    public final static String SN_id_on = "id-on";
    public final static String OBJ_id_on = OBJ_id_pkix + ".8";

    public final static int    NID_id_pda = 265;
    public final static String SN_id_pda = "id-pda";
    public final static String OBJ_id_pda = OBJ_id_pkix + ".9";

    public final static int    NID_id_aca = 266;
    public final static String SN_id_aca = "id-aca";
    public final static String OBJ_id_aca = OBJ_id_pkix + ".10";

    public final static int    NID_id_qcs = 267;
    public final static String SN_id_qcs = "id-qcs";
    public final static String OBJ_id_qcs = OBJ_id_pkix + ".11";

    public final static int    NID_id_cct = 268;
    public final static String SN_id_cct = "id-cct";
    public final static String OBJ_id_cct = OBJ_id_pkix + ".12";

    public final static int    NID_id_pkix1_explicit_88 = 269;
    public final static String SN_id_pkix1_explicit_88 = "id-pkix1-explicit-88";
    public final static String OBJ_id_pkix1_explicit_88 = OBJ_id_pkix_mod + ".1";

    public final static int    NID_id_pkix1_implicit_88 = 270;
    public final static String SN_id_pkix1_implicit_88 = "id-pkix1-implicit-88";
    public final static String OBJ_id_pkix1_implicit_88 = OBJ_id_pkix_mod + ".2";

    public final static int    NID_id_pkix1_explicit_93 = 271;
    public final static String SN_id_pkix1_explicit_93 = "id-pkix1-explicit-93";
    public final static String OBJ_id_pkix1_explicit_93 = OBJ_id_pkix_mod + ".3";

    public final static int    NID_id_pkix1_implicit_93 = 272;
    public final static String SN_id_pkix1_implicit_93 = "id-pkix1-implicit-93";
    public final static String OBJ_id_pkix1_implicit_93 = OBJ_id_pkix_mod + ".4";

    public final static int    NID_id_mod_crmf = 273;
    public final static String SN_id_mod_crmf = "id-mod-crmf";
    public final static String OBJ_id_mod_crmf = OBJ_id_pkix_mod + ".5";

    public final static int    NID_id_mod_cmc = 274;
    public final static String SN_id_mod_cmc = "id-mod-cmc";
    public final static String OBJ_id_mod_cmc = OBJ_id_pkix_mod + ".6";

    public final static int    NID_id_mod_kea_profile_88 = 275;
    public final static String SN_id_mod_kea_profile_88 = "id-mod-kea-profile-88";
    public final static String OBJ_id_mod_kea_profile_88 = OBJ_id_pkix_mod + ".7";

    public final static int    NID_id_mod_kea_profile_93 = 276;
    public final static String SN_id_mod_kea_profile_93 = "id-mod-kea-profile-93";
    public final static String OBJ_id_mod_kea_profile_93 = OBJ_id_pkix_mod + ".8";

    public final static int    NID_id_mod_cmp = 277;
    public final static String SN_id_mod_cmp = "id-mod-cmp";
    public final static String OBJ_id_mod_cmp = OBJ_id_pkix_mod + ".9";

    public final static int    NID_id_mod_qualified_cert_88 = 278;
    public final static String SN_id_mod_qualified_cert_88 = "id-mod-qualified-cert-88";
    public final static String OBJ_id_mod_qualified_cert_88 = OBJ_id_pkix_mod + ".10";

    public final static int    NID_id_mod_qualified_cert_93 = 279;
    public final static String SN_id_mod_qualified_cert_93 = "id-mod-qualified-cert-93";
    public final static String OBJ_id_mod_qualified_cert_93 = OBJ_id_pkix_mod + ".11";

    public final static int    NID_id_mod_attribute_cert = 280;
    public final static String SN_id_mod_attribute_cert = "id-mod-attribute-cert";
    public final static String OBJ_id_mod_attribute_cert = OBJ_id_pkix_mod + ".12";

    public final static int    NID_id_mod_timestamp_protocol = 281;
    public final static String SN_id_mod_timestamp_protocol = "id-mod-timestamp-protocol";
    public final static String OBJ_id_mod_timestamp_protocol = OBJ_id_pkix_mod + ".13";

    public final static int    NID_id_mod_ocsp = 282;
    public final static String SN_id_mod_ocsp = "id-mod-ocsp";
    public final static String OBJ_id_mod_ocsp = OBJ_id_pkix_mod + ".14";

    public final static int    NID_id_mod_dvcs = 283;
    public final static String SN_id_mod_dvcs = "id-mod-dvcs";
    public final static String OBJ_id_mod_dvcs = OBJ_id_pkix_mod + ".15";

    public final static int    NID_id_mod_cmp2000 = 284;
    public final static String SN_id_mod_cmp2000 = "id-mod-cmp2000";
    public final static String OBJ_id_mod_cmp2000 = OBJ_id_pkix_mod + ".16";

    public final static int    NID_biometricInfo = 285;
    public final static String SN_biometricInfo = "biometricInfo";
    public final static String LN_biometricInfo = "Biometric Info";
    public final static String OBJ_biometricInfo = OBJ_id_pe + ".2";

    public final static int    NID_qcStatements = 286;
    public final static String SN_qcStatements = "qcStatements";
    public final static String OBJ_qcStatements = OBJ_id_pe + ".3";

    public final static int    NID_ac_auditEntity = 287;
    public final static String SN_ac_auditEntity = "ac-auditEntity";
    public final static String OBJ_ac_auditEntity = OBJ_id_pe + ".4";

    public final static int    NID_ac_targeting = 288;
    public final static String SN_ac_targeting = "ac-targeting";
    public final static String OBJ_ac_targeting = OBJ_id_pe + ".5";

    public final static int    NID_aaControls = 289;
    public final static String SN_aaControls = "aaControls";
    public final static String OBJ_aaControls = OBJ_id_pe + ".6";

    public final static int    NID_sbgp_ipAddrBlock = 290;
    public final static String SN_sbgp_ipAddrBlock = "sbgp-ipAddrBlock";
    public final static String OBJ_sbgp_ipAddrBlock = OBJ_id_pe + ".7";

    public final static int    NID_sbgp_autonomousSysNum = 291;
    public final static String SN_sbgp_autonomousSysNum = "sbgp-autonomousSysNum";
    public final static String OBJ_sbgp_autonomousSysNum = OBJ_id_pe + ".8";

    public final static int    NID_sbgp_routerIdentifier = 292;
    public final static String SN_sbgp_routerIdentifier = "sbgp-routerIdentifier";
    public final static String OBJ_sbgp_routerIdentifier = OBJ_id_pe + ".9";

    public final static int    NID_textNotice = 293;
    public final static String SN_textNotice = "textNotice";
    public final static String OBJ_textNotice = OBJ_id_qt + ".3";

    public final static int    NID_ipsecEndSystem = 294;
    public final static String SN_ipsecEndSystem = "ipsecEndSystem";
    public final static String LN_ipsecEndSystem = "IPSec End System";
    public final static String OBJ_ipsecEndSystem = OBJ_id_kp + ".5";

    public final static int    NID_ipsecTunnel = 295;
    public final static String SN_ipsecTunnel = "ipsecTunnel";
    public final static String LN_ipsecTunnel = "IPSec Tunnel";
    public final static String OBJ_ipsecTunnel = OBJ_id_kp + ".6";

    public final static int    NID_ipsecUser = 296;
    public final static String SN_ipsecUser = "ipsecUser";
    public final static String LN_ipsecUser = "IPSec User";
    public final static String OBJ_ipsecUser = OBJ_id_kp + ".7";

    public final static int    NID_dvcs = 297;
    public final static String SN_dvcs = "DVCS";
    public final static String LN_dvcs = "dvcs";
    public final static String OBJ_dvcs = OBJ_id_kp + ".10";

    public final static int    NID_id_it_caProtEncCert = 298;
    public final static String SN_id_it_caProtEncCert = "id-it-caProtEncCert";
    public final static String OBJ_id_it_caProtEncCert = OBJ_id_it + ".1";

    public final static int    NID_id_it_signKeyPairTypes = 299;
    public final static String SN_id_it_signKeyPairTypes = "id-it-signKeyPairTypes";
    public final static String OBJ_id_it_signKeyPairTypes = OBJ_id_it + ".2";

    public final static int    NID_id_it_encKeyPairTypes = 300;
    public final static String SN_id_it_encKeyPairTypes = "id-it-encKeyPairTypes";
    public final static String OBJ_id_it_encKeyPairTypes = OBJ_id_it + ".3";

    public final static int    NID_id_it_preferredSymmAlg = 301;
    public final static String SN_id_it_preferredSymmAlg = "id-it-preferredSymmAlg";
    public final static String OBJ_id_it_preferredSymmAlg = OBJ_id_it + ".4";

    public final static int    NID_id_it_caKeyUpdateInfo = 302;
    public final static String SN_id_it_caKeyUpdateInfo = "id-it-caKeyUpdateInfo";
    public final static String OBJ_id_it_caKeyUpdateInfo = OBJ_id_it + ".5";

    public final static int    NID_id_it_currentCRL = 303;
    public final static String SN_id_it_currentCRL = "id-it-currentCRL";
    public final static String OBJ_id_it_currentCRL = OBJ_id_it + ".6";

    public final static int    NID_id_it_unsupportedOIDs = 304;
    public final static String SN_id_it_unsupportedOIDs = "id-it-unsupportedOIDs";
    public final static String OBJ_id_it_unsupportedOIDs = OBJ_id_it + ".7";

    public final static int    NID_id_it_subscriptionRequest = 305;
    public final static String SN_id_it_subscriptionRequest = "id-it-subscriptionRequest";
    public final static String OBJ_id_it_subscriptionRequest = OBJ_id_it + ".8";

    public final static int    NID_id_it_subscriptionResponse = 306;
    public final static String SN_id_it_subscriptionResponse = "id-it-subscriptionResponse";
    public final static String OBJ_id_it_subscriptionResponse = OBJ_id_it + ".9";

    public final static int    NID_id_it_keyPairParamReq = 307;
    public final static String SN_id_it_keyPairParamReq = "id-it-keyPairParamReq";
    public final static String OBJ_id_it_keyPairParamReq = OBJ_id_it + ".10";

    public final static int    NID_id_it_keyPairParamRep = 308;
    public final static String SN_id_it_keyPairParamRep = "id-it-keyPairParamRep";
    public final static String OBJ_id_it_keyPairParamRep = OBJ_id_it + ".11";

    public final static int    NID_id_it_revPassphrase = 309;
    public final static String SN_id_it_revPassphrase = "id-it-revPassphrase";
    public final static String OBJ_id_it_revPassphrase = OBJ_id_it + ".12";

    public final static int    NID_id_it_implicitConfirm = 310;
    public final static String SN_id_it_implicitConfirm = "id-it-implicitConfirm";
    public final static String OBJ_id_it_implicitConfirm = OBJ_id_it + ".13";

    public final static int    NID_id_it_confirmWaitTime = 311;
    public final static String SN_id_it_confirmWaitTime = "id-it-confirmWaitTime";
    public final static String OBJ_id_it_confirmWaitTime = OBJ_id_it + ".14";

    public final static int    NID_id_it_origPKIMessage = 312;
    public final static String SN_id_it_origPKIMessage = "id-it-origPKIMessage";
    public final static String OBJ_id_it_origPKIMessage = OBJ_id_it + ".15";

    public final static int    NID_id_regCtrl = 313;
    public final static String SN_id_regCtrl = "id-regCtrl";
    public final static String OBJ_id_regCtrl = OBJ_id_pkip + ".1";

    public final static int    NID_id_regInfo = 314;
    public final static String SN_id_regInfo = "id-regInfo";
    public final static String OBJ_id_regInfo = OBJ_id_pkip + ".2";

    public final static int    NID_id_regCtrl_regToken = 315;
    public final static String SN_id_regCtrl_regToken = "id-regCtrl-regToken";
    public final static String OBJ_id_regCtrl_regToken = OBJ_id_regCtrl + ".1";

    public final static int    NID_id_regCtrl_authenticator = 316;
    public final static String SN_id_regCtrl_authenticator = "id-regCtrl-authenticator";
    public final static String OBJ_id_regCtrl_authenticator = OBJ_id_regCtrl + ".2";

    public final static int    NID_id_regCtrl_pkiPublicationInfo = 317;
    public final static String SN_id_regCtrl_pkiPublicationInfo = "id-regCtrl-pkiPublicationInfo";
    public final static String OBJ_id_regCtrl_pkiPublicationInfo = OBJ_id_regCtrl + ".3";

    public final static int    NID_id_regCtrl_pkiArchiveOptions = 318;
    public final static String SN_id_regCtrl_pkiArchiveOptions = "id-regCtrl-pkiArchiveOptions";
    public final static String OBJ_id_regCtrl_pkiArchiveOptions = OBJ_id_regCtrl + ".4";

    public final static int    NID_id_regCtrl_oldCertID = 319;
    public final static String SN_id_regCtrl_oldCertID = "id-regCtrl-oldCertID";
    public final static String OBJ_id_regCtrl_oldCertID = OBJ_id_regCtrl + ".5";

    public final static int    NID_id_regCtrl_protocolEncrKey = 320;
    public final static String SN_id_regCtrl_protocolEncrKey = "id-regCtrl-protocolEncrKey";
    public final static String OBJ_id_regCtrl_protocolEncrKey = OBJ_id_regCtrl + ".6";

    public final static int    NID_id_regInfo_utf8Pairs = 321;
    public final static String SN_id_regInfo_utf8Pairs = "id-regInfo-utf8Pairs";
    public final static String OBJ_id_regInfo_utf8Pairs = OBJ_id_regInfo + ".1";

    public final static int    NID_id_regInfo_certReq = 322;
    public final static String SN_id_regInfo_certReq = "id-regInfo-certReq";
    public final static String OBJ_id_regInfo_certReq = OBJ_id_regInfo + ".2";

    public final static int    NID_id_alg_des40 = 323;
    public final static String SN_id_alg_des40 = "id-alg-des40";
    public final static String OBJ_id_alg_des40 = OBJ_id_alg + ".1";

    public final static int    NID_id_alg_noSignature = 324;
    public final static String SN_id_alg_noSignature = "id-alg-noSignature";
    public final static String OBJ_id_alg_noSignature = OBJ_id_alg + ".2";

    public final static int    NID_id_alg_dh_sig_hmac_sha1 = 325;
    public final static String SN_id_alg_dh_sig_hmac_sha1 = "id-alg-dh-sig-hmac-sha1";
    public final static String OBJ_id_alg_dh_sig_hmac_sha1 = OBJ_id_alg + ".3";

    public final static int    NID_id_alg_dh_pop = 326;
    public final static String SN_id_alg_dh_pop = "id-alg-dh-pop";
    public final static String OBJ_id_alg_dh_pop = OBJ_id_alg + ".4";

    public final static int    NID_id_cmc_statusInfo = 327;
    public final static String SN_id_cmc_statusInfo = "id-cmc-statusInfo";
    public final static String OBJ_id_cmc_statusInfo = OBJ_id_cmc + ".1";

    public final static int    NID_id_cmc_identification = 328;
    public final static String SN_id_cmc_identification = "id-cmc-identification";
    public final static String OBJ_id_cmc_identification = OBJ_id_cmc + ".2";

    public final static int    NID_id_cmc_identityProof = 329;
    public final static String SN_id_cmc_identityProof = "id-cmc-identityProof";
    public final static String OBJ_id_cmc_identityProof = OBJ_id_cmc + ".3";

    public final static int    NID_id_cmc_dataReturn = 330;
    public final static String SN_id_cmc_dataReturn = "id-cmc-dataReturn";
    public final static String OBJ_id_cmc_dataReturn = OBJ_id_cmc + ".4";

    public final static int    NID_id_cmc_transactionId = 331;
    public final static String SN_id_cmc_transactionId = "id-cmc-transactionId";
    public final static String OBJ_id_cmc_transactionId = OBJ_id_cmc + ".5";

    public final static int    NID_id_cmc_senderNonce = 332;
    public final static String SN_id_cmc_senderNonce = "id-cmc-senderNonce";
    public final static String OBJ_id_cmc_senderNonce = OBJ_id_cmc + ".6";

    public final static int    NID_id_cmc_recipientNonce = 333;
    public final static String SN_id_cmc_recipientNonce = "id-cmc-recipientNonce";
    public final static String OBJ_id_cmc_recipientNonce = OBJ_id_cmc + ".7";

    public final static int    NID_id_cmc_addExtensions = 334;
    public final static String SN_id_cmc_addExtensions = "id-cmc-addExtensions";
    public final static String OBJ_id_cmc_addExtensions = OBJ_id_cmc + ".8";

    public final static int    NID_id_cmc_encryptedPOP = 335;
    public final static String SN_id_cmc_encryptedPOP = "id-cmc-encryptedPOP";
    public final static String OBJ_id_cmc_encryptedPOP = OBJ_id_cmc + ".9";

    public final static int    NID_id_cmc_decryptedPOP = 336;
    public final static String SN_id_cmc_decryptedPOP = "id-cmc-decryptedPOP";
    public final static String OBJ_id_cmc_decryptedPOP = OBJ_id_cmc + ".10";

    public final static int    NID_id_cmc_lraPOPWitness = 337;
    public final static String SN_id_cmc_lraPOPWitness = "id-cmc-lraPOPWitness";
    public final static String OBJ_id_cmc_lraPOPWitness = OBJ_id_cmc + ".11";

    public final static int    NID_id_cmc_getCert = 338;
    public final static String SN_id_cmc_getCert = "id-cmc-getCert";
    public final static String OBJ_id_cmc_getCert = OBJ_id_cmc + ".15";

    public final static int    NID_id_cmc_getCRL = 339;
    public final static String SN_id_cmc_getCRL = "id-cmc-getCRL";
    public final static String OBJ_id_cmc_getCRL = OBJ_id_cmc + ".16";

    public final static int    NID_id_cmc_revokeRequest = 340;
    public final static String SN_id_cmc_revokeRequest = "id-cmc-revokeRequest";
    public final static String OBJ_id_cmc_revokeRequest = OBJ_id_cmc + ".17";

    public final static int    NID_id_cmc_regInfo = 341;
    public final static String SN_id_cmc_regInfo = "id-cmc-regInfo";
    public final static String OBJ_id_cmc_regInfo = OBJ_id_cmc + ".18";

    public final static int    NID_id_cmc_responseInfo = 342;
    public final static String SN_id_cmc_responseInfo = "id-cmc-responseInfo";
    public final static String OBJ_id_cmc_responseInfo = OBJ_id_cmc + ".19";

    public final static int    NID_id_cmc_queryPending = 343;
    public final static String SN_id_cmc_queryPending = "id-cmc-queryPending";
    public final static String OBJ_id_cmc_queryPending = OBJ_id_cmc + ".21";

    public final static int    NID_id_cmc_popLinkRandom = 344;
    public final static String SN_id_cmc_popLinkRandom = "id-cmc-popLinkRandom";
    public final static String OBJ_id_cmc_popLinkRandom = OBJ_id_cmc + ".22";

    public final static int    NID_id_cmc_popLinkWitness = 345;
    public final static String SN_id_cmc_popLinkWitness = "id-cmc-popLinkWitness";
    public final static String OBJ_id_cmc_popLinkWitness = OBJ_id_cmc + ".23";

    public final static int    NID_id_cmc_confirmCertAcceptance = 346;
    public final static String SN_id_cmc_confirmCertAcceptance = "id-cmc-confirmCertAcceptance";
    public final static String OBJ_id_cmc_confirmCertAcceptance = OBJ_id_cmc + ".24";

    public final static int    NID_id_on_personalData = 347;
    public final static String SN_id_on_personalData = "id-on-personalData";
    public final static String OBJ_id_on_personalData = OBJ_id_on + ".1";

    public final static int    NID_id_pda_dateOfBirth = 348;
    public final static String SN_id_pda_dateOfBirth = "id-pda-dateOfBirth";
    public final static String OBJ_id_pda_dateOfBirth = OBJ_id_pda + ".1";

    public final static int    NID_id_pda_placeOfBirth = 349;
    public final static String SN_id_pda_placeOfBirth = "id-pda-placeOfBirth";
    public final static String OBJ_id_pda_placeOfBirth = OBJ_id_pda + ".2";

    public final static int    NID_id_pda_gender = 351;
    public final static String SN_id_pda_gender = "id-pda-gender";
    public final static String OBJ_id_pda_gender = OBJ_id_pda + ".3";

    public final static int    NID_id_pda_countryOfCitizenship = 352;
    public final static String SN_id_pda_countryOfCitizenship = "id-pda-countryOfCitizenship";
    public final static String OBJ_id_pda_countryOfCitizenship = OBJ_id_pda + ".4";

    public final static int    NID_id_pda_countryOfResidence = 353;
    public final static String SN_id_pda_countryOfResidence = "id-pda-countryOfResidence";
    public final static String OBJ_id_pda_countryOfResidence = OBJ_id_pda + ".5";

    public final static int    NID_id_aca_authenticationInfo = 354;
    public final static String SN_id_aca_authenticationInfo = "id-aca-authenticationInfo";
    public final static String OBJ_id_aca_authenticationInfo = OBJ_id_aca + ".1";

    public final static int    NID_id_aca_accessIdentity = 355;
    public final static String SN_id_aca_accessIdentity = "id-aca-accessIdentity";
    public final static String OBJ_id_aca_accessIdentity = OBJ_id_aca + ".2";

    public final static int    NID_id_aca_chargingIdentity = 356;
    public final static String SN_id_aca_chargingIdentity = "id-aca-chargingIdentity";
    public final static String OBJ_id_aca_chargingIdentity = OBJ_id_aca + ".3";

    public final static int    NID_id_aca_group = 357;
    public final static String SN_id_aca_group = "id-aca-group";
    public final static String OBJ_id_aca_group = OBJ_id_aca + ".4";

    public final static int    NID_id_aca_role = 358;
    public final static String SN_id_aca_role = "id-aca-role";
    public final static String OBJ_id_aca_role = OBJ_id_aca + ".5";

    public final static int    NID_id_qcs_pkixQCSyntax_v1 = 359;
    public final static String SN_id_qcs_pkixQCSyntax_v1 = "id-qcs-pkixQCSyntax-v1";
    public final static String OBJ_id_qcs_pkixQCSyntax_v1 = OBJ_id_qcs + ".1";

    public final static int    NID_id_cct_crs = 360;
    public final static String SN_id_cct_crs = "id-cct-crs";
    public final static String OBJ_id_cct_crs = OBJ_id_cct + ".1";

    public final static int    NID_id_cct_PKIData = 361;
    public final static String SN_id_cct_PKIData = "id-cct-PKIData";
    public final static String OBJ_id_cct_PKIData = OBJ_id_cct + ".2";

    public final static int    NID_id_cct_PKIResponse = 362;
    public final static String SN_id_cct_PKIResponse = "id-cct-PKIResponse";
    public final static String OBJ_id_cct_PKIResponse = OBJ_id_cct + ".3";

    public final static int    NID_ad_timeStamping = 363;
    public final static String SN_ad_timeStamping = "ad_timestamping";
    public final static String LN_ad_timeStamping = "AD Time Stamping";
    public final static String OBJ_ad_timeStamping = OBJ_id_ad + ".3";

    public final static int    NID_ad_dvcs = 364;
    public final static String SN_ad_dvcs = "AD_DVCS";
    public final static String LN_ad_dvcs = "ad dvcs";
    public final static String OBJ_ad_dvcs = OBJ_id_ad + ".4";

    public final static int    NID_id_pkix_OCSP_basic = 365;
    public final static String SN_id_pkix_OCSP_basic = "basicOCSPResponse";
    public final static String LN_id_pkix_OCSP_basic = "Basic OCSP Response";
    public final static String OBJ_id_pkix_OCSP_basic = OBJ_id_pkix_OCSP + ".1";

    public final static int    NID_id_pkix_OCSP_Nonce = 366;
    public final static String SN_id_pkix_OCSP_Nonce = "Nonce";
    public final static String LN_id_pkix_OCSP_Nonce = "OCSP Nonce";
    public final static String OBJ_id_pkix_OCSP_Nonce = OBJ_id_pkix_OCSP + ".2";

    public final static int    NID_id_pkix_OCSP_CrlID = 367;
    public final static String SN_id_pkix_OCSP_CrlID = "CrlID";
    public final static String LN_id_pkix_OCSP_CrlID = "OCSP CRL ID";
    public final static String OBJ_id_pkix_OCSP_CrlID = OBJ_id_pkix_OCSP + ".3";

    public final static int    NID_id_pkix_OCSP_acceptableResponses = 368;
    public final static String SN_id_pkix_OCSP_acceptableResponses = "acceptableResponses";
    public final static String LN_id_pkix_OCSP_acceptableResponses = "Acceptable OCSP Responses";
    public final static String OBJ_id_pkix_OCSP_acceptableResponses = OBJ_id_pkix_OCSP + ".4";

    public final static int    NID_id_pkix_OCSP_noCheck = 369;
    public final static String SN_id_pkix_OCSP_noCheck = "noCheck";
    public final static String LN_id_pkix_OCSP_noCheck = "OCSP No Check";
    public final static String OBJ_id_pkix_OCSP_noCheck = OBJ_id_pkix_OCSP + ".5";

    public final static int    NID_id_pkix_OCSP_archiveCutoff = 370;
    public final static String SN_id_pkix_OCSP_archiveCutoff = "archiveCutoff";
    public final static String LN_id_pkix_OCSP_archiveCutoff = "OCSP Archive Cutoff";
    public final static String OBJ_id_pkix_OCSP_archiveCutoff = OBJ_id_pkix_OCSP + ".6";

    public final static int    NID_id_pkix_OCSP_serviceLocator = 371;
    public final static String SN_id_pkix_OCSP_serviceLocator = "serviceLocator";
    public final static String LN_id_pkix_OCSP_serviceLocator = "OCSP Service Locator";
    public final static String OBJ_id_pkix_OCSP_serviceLocator = OBJ_id_pkix_OCSP + ".7";

    public final static int    NID_id_pkix_OCSP_extendedStatus = 372;
    public final static String SN_id_pkix_OCSP_extendedStatus = "extendedStatus";
    public final static String LN_id_pkix_OCSP_extendedStatus = "Extended OCSP Status";
    public final static String OBJ_id_pkix_OCSP_extendedStatus = OBJ_id_pkix_OCSP + ".8";

    public final static int    NID_id_pkix_OCSP_valid = 373;
    public final static String SN_id_pkix_OCSP_valid = "valid";
    public final static String OBJ_id_pkix_OCSP_valid = OBJ_id_pkix_OCSP + ".9";

    public final static int    NID_id_pkix_OCSP_path = 374;
    public final static String SN_id_pkix_OCSP_path = "path";
    public final static String OBJ_id_pkix_OCSP_path = OBJ_id_pkix_OCSP + ".10";

    public final static int    NID_id_pkix_OCSP_trustRoot = 375;
    public final static String SN_id_pkix_OCSP_trustRoot = "trustRoot";
    public final static String LN_id_pkix_OCSP_trustRoot = "Trust Root";
    public final static String OBJ_id_pkix_OCSP_trustRoot = OBJ_id_pkix_OCSP + ".11";

    public final static int    NID_rsaSignature = 377;
    public final static String SN_rsaSignature = "rsaSignature";
    public final static String OBJ_rsaSignature = OBJ_algorithm + ".11";

    public final static int    NID_Directory = 382;
    public final static String SN_Directory = "directory";
    public final static String LN_Directory = "Directory";
    public final static String OBJ_Directory = OBJ_internet + ".1";

    public final static int    NID_Management = 383;
    public final static String SN_Management = "mgmt";
    public final static String LN_Management = "Management";
    public final static String OBJ_Management = OBJ_internet + ".2";

    public final static int    NID_Experimental = 384;
    public final static String SN_Experimental = "experimental";
    public final static String LN_Experimental = "Experimental";
    public final static String OBJ_Experimental = OBJ_internet + ".3";

    public final static int    NID_Private = 385;
    public final static String SN_Private = "private";
    public final static String LN_Private = "Private";
    public final static String OBJ_Private = OBJ_internet + ".4";

    public final static int    NID_Security = 386;
    public final static String SN_Security = "security";
    public final static String LN_Security = "Security";
    public final static String OBJ_Security = OBJ_internet + ".5";

    public final static int    NID_SNMPv2 = 387;
    public final static String SN_SNMPv2 = "snmpv2";
    public final static String LN_SNMPv2 = "SNMPv2";
    public final static String OBJ_SNMPv2 = OBJ_internet + ".6";

    public final static int    NID_Mail = 388;
    public final static String LN_Mail = "Mail";
    public final static String OBJ_Mail = OBJ_internet + ".7";

    public final static int    NID_Enterprises = 389;
    public final static String SN_Enterprises = "enterprises";
    public final static String LN_Enterprises = "Enterprises";
    public final static String OBJ_Enterprises = OBJ_Private + ".1";

    public final static int    NID_dcObject = 390;
    public final static String SN_dcObject = "dcobject";
    public final static String LN_dcObject = "dcObject";
    public final static String OBJ_dcObject = OBJ_Enterprises + ".1466.344";

    public final static int    NID_itu_t = 645;
    public final static String SN_itu_t = "ITU-T";
    public final static String LN_itu_t = "itu-t";
    public final static String OBJ_itu_t = "0";

    public final static int    NID_data = 434;
    public final static String SN_data = "data";
    public final static String OBJ_data = OBJ_itu_t + ".9";

    public final static int    NID_pss = 435;
    public final static String SN_pss = "pss";
    public final static String OBJ_pss = OBJ_data + ".2342";

    public final static int    NID_ucl = 436;
    public final static String SN_ucl = "ucl";
    public final static String OBJ_ucl = OBJ_pss + ".19200300";

    public final static int    NID_pilot = 437;
    public final static String SN_pilot = "pilot";
    public final static String OBJ_pilot = OBJ_ucl + ".100";

    public final static int    NID_pilotAttributeType = 438;
    public final static String LN_pilotAttributeType = "pilotAttributeType";
    public final static String OBJ_pilotAttributeType = OBJ_pilot + ".1";

    public final static int    NID_domainComponent = 391;
    public final static String SN_domainComponent = "DC";
    public final static String LN_domainComponent = "domainComponent";
    public final static String OBJ_domainComponent = OBJ_pilotAttributeType + ".25";

    public final static int    NID_pilotObjectClass = 440;
    public final static String LN_pilotObjectClass = "pilotObjectClass";
    public final static String OBJ_pilotObjectClass = OBJ_pilot + ".4";

    public final static int    NID_Domain = 392;
    public final static String SN_Domain = "domain";
    public final static String LN_Domain = "Domain";
    public final static String OBJ_Domain = OBJ_pilotObjectClass + ".13";

    public final static int    NID_joint_iso_ccitt = 393;
    private final static String OBJ_joint_iso_ccitt = "OBJ_joint_iso_itu_t";

    public final static int    NID_selected_attribute_types = 394;
    public final static String SN_selected_attribute_types = "selected-attribute-types";
    public final static String LN_selected_attribute_types = "Selected Attribute Types";
    public final static String OBJ_selected_attribute_types = OBJ_joint_iso_itu_t + ".5.1.5";

    public final static int    NID_clearance = 395;
    public final static String SN_clearance = "clearance";
    public final static String OBJ_clearance = OBJ_selected_attribute_types + ".55";

    public final static int    NID_md4WithRSAEncryption = 396;
    public final static String SN_md4WithRSAEncryption = "RSA-MD4";
    public final static String LN_md4WithRSAEncryption = "md4WithRSAEncryption";
    public final static String OBJ_md4WithRSAEncryption = OBJ_pkcs1 + ".3";

    public final static int    NID_ac_proxying = 397;
    public final static String SN_ac_proxying = "ac-proxying";
    public final static String OBJ_ac_proxying = OBJ_id_pe + ".10";

    public final static int    NID_sinfo_access = 398;
    public final static String SN_sinfo_access = "subjectInfoAccess";
    public final static String LN_sinfo_access = "Subject Information Access";
    public final static String OBJ_sinfo_access = OBJ_id_pe + ".11";

    public final static int    NID_id_aca_encAttrs = 399;
    public final static String SN_id_aca_encAttrs = "id-aca-encAttrs";
    public final static String OBJ_id_aca_encAttrs = OBJ_id_aca + ".6";

    public final static int    NID_role = 400;
    public final static String SN_role = "role";
    public final static String LN_role = "role";
    public final static String OBJ_role = OBJ_X509 + ".72";

    public final static int    NID_policy_constraints = 401;
    public final static String SN_policy_constraints = "policyConstraints";
    public final static String LN_policy_constraints = "X509v3 Policy Constraints";
    public final static String OBJ_policy_constraints = OBJ_id_ce + ".36";

    public final static int    NID_target_information = 402;
    public final static String SN_target_information = "targetInformation";
    public final static String LN_target_information = "X509v3 AC Targeting";
    public final static String OBJ_target_information = OBJ_id_ce + ".55";

    public final static int    NID_no_rev_avail = 403;
    public final static String SN_no_rev_avail = "noRevAvail";
    public final static String LN_no_rev_avail = "X509v3 No Revocation Available";
    public final static String OBJ_no_rev_avail = OBJ_id_ce + ".56";

    public final static int    NID_ccitt = 404;
    private final static String OBJ_ccitt = "OBJ_itu_t";

    public final static int    NID_X9_62_prime_field = 406;
    public final static String SN_X9_62_prime_field = "prime-field";
    public final static String OBJ_X9_62_prime_field = OBJ_X9_62_id_fieldType + ".1";

    public final static int    NID_X9_62_characteristic_two_field = 407;
    public final static String SN_X9_62_characteristic_two_field = "characteristic-two-field";
    public final static String OBJ_X9_62_characteristic_two_field = OBJ_X9_62_id_fieldType + ".2";

    public final static int    NID_X9_62_id_ecPublicKey = 408;
    public final static String SN_X9_62_id_ecPublicKey = "id-ecPublicKey";
    public final static String OBJ_X9_62_id_ecPublicKey = OBJ_X9_62_id_publicKeyType + ".1";

    public final static int    NID_X9_62_prime192v1 = 409;
    public final static String SN_X9_62_prime192v1 = "prime192v1";
    public final static String OBJ_X9_62_prime192v1 = OBJ_X9_62_primeCurve + ".1";

    public final static int    NID_X9_62_prime192v2 = 410;
    public final static String SN_X9_62_prime192v2 = "prime192v2";
    public final static String OBJ_X9_62_prime192v2 = OBJ_X9_62_primeCurve + ".2";

    public final static int    NID_X9_62_prime192v3 = 411;
    public final static String SN_X9_62_prime192v3 = "prime192v3";
    public final static String OBJ_X9_62_prime192v3 = OBJ_X9_62_primeCurve + ".3";

    public final static int    NID_X9_62_prime239v1 = 412;
    public final static String SN_X9_62_prime239v1 = "prime239v1";
    public final static String OBJ_X9_62_prime239v1 = OBJ_X9_62_primeCurve + ".4";

    public final static int    NID_X9_62_prime239v2 = 413;
    public final static String SN_X9_62_prime239v2 = "prime239v2";
    public final static String OBJ_X9_62_prime239v2 = OBJ_X9_62_primeCurve + ".5";

    public final static int    NID_X9_62_prime239v3 = 414;
    public final static String SN_X9_62_prime239v3 = "prime239v3";
    public final static String OBJ_X9_62_prime239v3 = OBJ_X9_62_primeCurve + ".6";

    public final static int    NID_X9_62_prime256v1 = 415;
    public final static String SN_X9_62_prime256v1 = "prime256v1";
    public final static String OBJ_X9_62_prime256v1 = OBJ_X9_62_primeCurve + ".7";

    public final static int    NID_ecdsa_with_SHA1 = 416;
    public final static String SN_ecdsa_with_SHA1 = "ecdsa-with-SHA1";
    public final static String OBJ_ecdsa_with_SHA1 = OBJ_X9_62_id_ecSigType + ".1";
    public final static ASN1ObjectIdentifier OID_ecdsa_with_SHA1 = new ASN1ObjectIdentifier(OBJ_ecdsa_with_SHA1);

    public final static int    NID_ms_csp_name = 417;
    public final static String SN_ms_csp_name = "CSPName";
    public final static String LN_ms_csp_name = "Microsoft CSP Name";
    public final static String OBJ_ms_csp_name = "1.3.6.1.4.1.311.17.1";

    public final static int    NID_aes_128_ecb = 418;
    public final static String SN_aes_128_ecb = "AES-128-ECB";
    public final static String LN_aes_128_ecb = "aes-128-ecb";
    public final static String OBJ_aes_128_ecb = OBJ_aes + ".1";

    public final static int    NID_aes_128_cbc = 419;
    public final static String SN_aes_128_cbc = "AES-128-CBC";
    public final static String LN_aes_128_cbc = "aes-128-cbc";
    public final static String OBJ_aes_128_cbc = OBJ_aes + ".2";

    public final static int    NID_aes_128_ofb128 = 420;
    public final static String SN_aes_128_ofb128 = "AES-128-OFB";
    public final static String LN_aes_128_ofb128 = "aes-128-ofb";
    public final static String OBJ_aes_128_ofb128 = OBJ_aes + ".3";

    public final static int    NID_aes_128_cfb128 = 421;
    public final static String SN_aes_128_cfb128 = "AES-128-CFB";
    public final static String LN_aes_128_cfb128 = "aes-128-cfb";
    public final static String OBJ_aes_128_cfb128 = OBJ_aes + ".4";

    public final static int    NID_aes_192_ecb = 422;
    public final static String SN_aes_192_ecb = "AES-192-ECB";
    public final static String LN_aes_192_ecb = "aes-192-ecb";
    public final static String OBJ_aes_192_ecb = OBJ_aes + ".21";

    public final static int    NID_aes_192_cbc = 423;
    public final static String SN_aes_192_cbc = "AES-192-CBC";
    public final static String LN_aes_192_cbc = "aes-192-cbc";
    public final static String OBJ_aes_192_cbc = OBJ_aes + ".22";

    public final static int    NID_aes_192_ofb128 = 424;
    public final static String SN_aes_192_ofb128 = "AES-192-OFB";
    public final static String LN_aes_192_ofb128 = "aes-192-ofb";
    public final static String OBJ_aes_192_ofb128 = OBJ_aes + ".23";

    public final static int    NID_aes_192_cfb128 = 425;
    public final static String SN_aes_192_cfb128 = "AES-192-CFB";
    public final static String LN_aes_192_cfb128 = "aes-192-cfb";
    public final static String OBJ_aes_192_cfb128 = OBJ_aes + ".24";

    public final static int    NID_aes_256_ecb = 426;
    public final static String SN_aes_256_ecb = "AES-256-ECB";
    public final static String LN_aes_256_ecb = "aes-256-ecb";
    public final static String OBJ_aes_256_ecb = OBJ_aes + ".41";

    public final static int    NID_aes_256_cbc = 427;
    public final static String SN_aes_256_cbc = "AES-256-CBC";
    public final static String LN_aes_256_cbc = "aes-256-cbc";
    public final static String OBJ_aes_256_cbc = OBJ_aes + ".42";

    public final static int    NID_aes_256_ofb128 = 428;
    public final static String SN_aes_256_ofb128 = "AES-256-OFB";
    public final static String LN_aes_256_ofb128 = "aes-256-ofb";
    public final static String OBJ_aes_256_ofb128 = OBJ_aes + ".43";

    public final static int    NID_aes_256_cfb128 = 429;
    public final static String SN_aes_256_cfb128 = "AES-256-CFB";
    public final static String LN_aes_256_cfb128 = "aes-256-cfb";
    public final static String OBJ_aes_256_cfb128 = OBJ_aes + ".44";

    public final static int    NID_hold_instruction_code = 430;
    public final static String SN_hold_instruction_code = "holdInstructionCode";
    public final static String LN_hold_instruction_code = "Hold Instruction Code";
    public final static String OBJ_hold_instruction_code = OBJ_id_ce + ".23";

    public final static int    NID_hold_instruction_none = 431;
    public final static String SN_hold_instruction_none = "holdInstructionNone";
    public final static String LN_hold_instruction_none = "Hold Instruction None";
    public final static String OBJ_hold_instruction_none = OBJ_holdInstruction + ".1";

    public final static int    NID_hold_instruction_call_issuer = 432;
    public final static String SN_hold_instruction_call_issuer = "holdInstructionCallIssuer";
    public final static String LN_hold_instruction_call_issuer = "Hold Instruction Call Issuer";
    public final static String OBJ_hold_instruction_call_issuer = OBJ_holdInstruction + ".2";

    public final static int    NID_hold_instruction_reject = 433;
    public final static String SN_hold_instruction_reject = "holdInstructionReject";
    public final static String LN_hold_instruction_reject = "Hold Instruction Reject";
    public final static String OBJ_hold_instruction_reject = OBJ_holdInstruction + ".3";

    public final static int    NID_pilotAttributeSyntax = 439;
    public final static String LN_pilotAttributeSyntax = "pilotAttributeSyntax";
    public final static String OBJ_pilotAttributeSyntax = OBJ_pilot + ".3";

    public final static int    NID_pilotGroups = 441;
    public final static String LN_pilotGroups = "pilotGroups";
    public final static String OBJ_pilotGroups = OBJ_pilot + ".10";

    public final static int    NID_iA5StringSyntax = 442;
    public final static String LN_iA5StringSyntax = "iA5StringSyntax";
    public final static String OBJ_iA5StringSyntax = OBJ_pilotAttributeSyntax + ".4";

    public final static int    NID_caseIgnoreIA5StringSyntax = 443;
    public final static String LN_caseIgnoreIA5StringSyntax = "caseIgnoreIA5StringSyntax";
    public final static String OBJ_caseIgnoreIA5StringSyntax = OBJ_pilotAttributeSyntax + ".5";

    public final static int    NID_pilotObject = 444;
    public final static String LN_pilotObject = "pilotObject";
    public final static String OBJ_pilotObject = OBJ_pilotObjectClass + ".3";

    public final static int    NID_pilotPerson = 445;
    public final static String LN_pilotPerson = "pilotPerson";
    public final static String OBJ_pilotPerson = OBJ_pilotObjectClass + ".4";

    public final static int    NID_account = 446;
    public final static String SN_account = "account";
    public final static String OBJ_account = OBJ_pilotObjectClass + ".5";

    public final static int    NID_document = 447;
    public final static String SN_document = "document";
    public final static String OBJ_document = OBJ_pilotObjectClass + ".6";

    public final static int    NID_room = 448;
    public final static String SN_room = "room";
    public final static String OBJ_room = OBJ_pilotObjectClass + ".7";

    public final static int    NID_documentSeries = 449;
    public final static String LN_documentSeries = "documentSeries";
    public final static String OBJ_documentSeries = OBJ_pilotObjectClass + ".9";

    public final static int    NID_rFC822localPart = 450;
    public final static String LN_rFC822localPart = "rFC822localPart";
    public final static String OBJ_rFC822localPart = OBJ_pilotObjectClass + ".14";

    public final static int    NID_dNSDomain = 451;
    public final static String LN_dNSDomain = "dNSDomain";
    public final static String OBJ_dNSDomain = OBJ_pilotObjectClass + ".15";

    public final static int    NID_domainRelatedObject = 452;
    public final static String LN_domainRelatedObject = "domainRelatedObject";
    public final static String OBJ_domainRelatedObject = OBJ_pilotObjectClass + ".17";

    public final static int    NID_friendlyCountry = 453;
    public final static String LN_friendlyCountry = "friendlyCountry";
    public final static String OBJ_friendlyCountry = OBJ_pilotObjectClass + ".18";

    public final static int    NID_simpleSecurityObject = 454;
    public final static String LN_simpleSecurityObject = "simpleSecurityObject";
    public final static String OBJ_simpleSecurityObject = OBJ_pilotObjectClass + ".19";

    public final static int    NID_pilotOrganization = 455;
    public final static String LN_pilotOrganization = "pilotOrganization";
    public final static String OBJ_pilotOrganization = OBJ_pilotObjectClass + ".20";

    public final static int    NID_pilotDSA = 456;
    public final static String LN_pilotDSA = "pilotDSA";
    public final static String OBJ_pilotDSA = OBJ_pilotObjectClass + ".21";

    public final static int    NID_qualityLabelledData = 457;
    public final static String LN_qualityLabelledData = "qualityLabelledData";
    public final static String OBJ_qualityLabelledData = OBJ_pilotObjectClass + ".22";

    public final static int    NID_userId = 458;
    public final static String SN_userId = "UID";
    public final static String LN_userId = "userId";
    public final static String OBJ_userId = OBJ_pilotAttributeType + ".1";

    public final static int    NID_textEncodedORAddress = 459;
    public final static String LN_textEncodedORAddress = "textEncodedORAddress";
    public final static String OBJ_textEncodedORAddress = OBJ_pilotAttributeType + ".2";

    public final static int    NID_rfc822Mailbox = 460;
    public final static String SN_rfc822Mailbox = "mail";
    public final static String LN_rfc822Mailbox = "rfc822Mailbox";
    public final static String OBJ_rfc822Mailbox = OBJ_pilotAttributeType + ".3";

    public final static int    NID_info = 461;
    public final static String SN_info = "info";
    public final static String OBJ_info = OBJ_pilotAttributeType + ".4";

    public final static int    NID_favouriteDrink = 462;
    public final static String LN_favouriteDrink = "favouriteDrink";
    public final static String OBJ_favouriteDrink = OBJ_pilotAttributeType + ".5";

    public final static int    NID_roomNumber = 463;
    public final static String LN_roomNumber = "roomNumber";
    public final static String OBJ_roomNumber = OBJ_pilotAttributeType + ".6";

    public final static int    NID_photo = 464;
    public final static String SN_photo = "photo";
    public final static String OBJ_photo = OBJ_pilotAttributeType + ".7";

    public final static int    NID_userClass = 465;
    public final static String LN_userClass = "userClass";
    public final static String OBJ_userClass = OBJ_pilotAttributeType + ".8";

    public final static int    NID_host = 466;
    public final static String SN_host = "host";
    public final static String OBJ_host = OBJ_pilotAttributeType + ".9";

    public final static int    NID_manager = 467;
    public final static String SN_manager = "manager";
    public final static String OBJ_manager = OBJ_pilotAttributeType + ".10";

    public final static int    NID_documentIdentifier = 468;
    public final static String LN_documentIdentifier = "documentIdentifier";
    public final static String OBJ_documentIdentifier = OBJ_pilotAttributeType + ".11";

    public final static int    NID_documentTitle = 469;
    public final static String LN_documentTitle = "documentTitle";
    public final static String OBJ_documentTitle = OBJ_pilotAttributeType + ".12";

    public final static int    NID_documentVersion = 470;
    public final static String LN_documentVersion = "documentVersion";
    public final static String OBJ_documentVersion = OBJ_pilotAttributeType + ".13";

    public final static int    NID_documentAuthor = 471;
    public final static String LN_documentAuthor = "documentAuthor";
    public final static String OBJ_documentAuthor = OBJ_pilotAttributeType + ".14";

    public final static int    NID_documentLocation = 472;
    public final static String LN_documentLocation = "documentLocation";
    public final static String OBJ_documentLocation = OBJ_pilotAttributeType + ".15";

    public final static int    NID_homeTelephoneNumber = 473;
    public final static String LN_homeTelephoneNumber = "homeTelephoneNumber";
    public final static String OBJ_homeTelephoneNumber = OBJ_pilotAttributeType + ".20";

    public final static int    NID_secretary = 474;
    public final static String SN_secretary = "secretary";
    public final static String OBJ_secretary = OBJ_pilotAttributeType + ".21";

    public final static int    NID_otherMailbox = 475;
    public final static String LN_otherMailbox = "otherMailbox";
    public final static String OBJ_otherMailbox = OBJ_pilotAttributeType + ".22";

    public final static int    NID_lastModifiedTime = 476;
    public final static String LN_lastModifiedTime = "lastModifiedTime";
    public final static String OBJ_lastModifiedTime = OBJ_pilotAttributeType + ".23";

    public final static int    NID_lastModifiedBy = 477;
    public final static String LN_lastModifiedBy = "lastModifiedBy";
    public final static String OBJ_lastModifiedBy = OBJ_pilotAttributeType + ".24";

    public final static int    NID_aRecord = 478;
    public final static String LN_aRecord = "aRecord";
    public final static String OBJ_aRecord = OBJ_pilotAttributeType + ".26";

    public final static int    NID_pilotAttributeType27 = 479;
    public final static String LN_pilotAttributeType27 = "pilotAttributeType27";
    public final static String OBJ_pilotAttributeType27 = OBJ_pilotAttributeType + ".27";

    public final static int    NID_mXRecord = 480;
    public final static String LN_mXRecord = "mXRecord";
    public final static String OBJ_mXRecord = OBJ_pilotAttributeType + ".28";

    public final static int    NID_nSRecord = 481;
    public final static String LN_nSRecord = "nSRecord";
    public final static String OBJ_nSRecord = OBJ_pilotAttributeType + ".29";

    public final static int    NID_sOARecord = 482;
    public final static String LN_sOARecord = "sOARecord";
    public final static String OBJ_sOARecord = OBJ_pilotAttributeType + ".30";

    public final static int    NID_cNAMERecord = 483;
    public final static String LN_cNAMERecord = "cNAMERecord";
    public final static String OBJ_cNAMERecord = OBJ_pilotAttributeType + ".31";

    public final static int    NID_associatedDomain = 484;
    public final static String LN_associatedDomain = "associatedDomain";
    public final static String OBJ_associatedDomain = OBJ_pilotAttributeType + ".37";

    public final static int    NID_associatedName = 485;
    public final static String LN_associatedName = "associatedName";
    public final static String OBJ_associatedName = OBJ_pilotAttributeType + ".38";

    public final static int    NID_homePostalAddress = 486;
    public final static String LN_homePostalAddress = "homePostalAddress";
    public final static String OBJ_homePostalAddress = OBJ_pilotAttributeType + ".39";

    public final static int    NID_personalTitle = 487;
    public final static String LN_personalTitle = "personalTitle";
    public final static String OBJ_personalTitle = OBJ_pilotAttributeType + ".40";

    public final static int    NID_mobileTelephoneNumber = 488;
    public final static String LN_mobileTelephoneNumber = "mobileTelephoneNumber";
    public final static String OBJ_mobileTelephoneNumber = OBJ_pilotAttributeType + ".41";

    public final static int    NID_pagerTelephoneNumber = 489;
    public final static String LN_pagerTelephoneNumber = "pagerTelephoneNumber";
    public final static String OBJ_pagerTelephoneNumber = OBJ_pilotAttributeType + ".42";

    public final static int    NID_friendlyCountryName = 490;
    public final static String LN_friendlyCountryName = "friendlyCountryName";
    public final static String OBJ_friendlyCountryName = OBJ_pilotAttributeType + ".43";

    public final static int    NID_organizationalStatus = 491;
    public final static String LN_organizationalStatus = "organizationalStatus";
    public final static String OBJ_organizationalStatus = OBJ_pilotAttributeType + ".45";

    public final static int    NID_janetMailbox = 492;
    public final static String LN_janetMailbox = "janetMailbox";
    public final static String OBJ_janetMailbox = OBJ_pilotAttributeType + ".46";

    public final static int    NID_mailPreferenceOption = 493;
    public final static String LN_mailPreferenceOption = "mailPreferenceOption";
    public final static String OBJ_mailPreferenceOption = OBJ_pilotAttributeType + ".47";

    public final static int    NID_buildingName = 494;
    public final static String LN_buildingName = "buildingName";
    public final static String OBJ_buildingName = OBJ_pilotAttributeType + ".48";

    public final static int    NID_dSAQuality = 495;
    public final static String LN_dSAQuality = "dSAQuality";
    public final static String OBJ_dSAQuality = OBJ_pilotAttributeType + ".49";

    public final static int    NID_singleLevelQuality = 496;
    public final static String LN_singleLevelQuality = "singleLevelQuality";
    public final static String OBJ_singleLevelQuality = OBJ_pilotAttributeType + ".50";

    public final static int    NID_subtreeMinimumQuality = 497;
    public final static String LN_subtreeMinimumQuality = "subtreeMinimumQuality";
    public final static String OBJ_subtreeMinimumQuality = OBJ_pilotAttributeType + ".51";

    public final static int    NID_subtreeMaximumQuality = 498;
    public final static String LN_subtreeMaximumQuality = "subtreeMaximumQuality";
    public final static String OBJ_subtreeMaximumQuality = OBJ_pilotAttributeType + ".52";

    public final static int    NID_personalSignature = 499;
    public final static String LN_personalSignature = "personalSignature";
    public final static String OBJ_personalSignature = OBJ_pilotAttributeType + ".53";

    public final static int    NID_dITRedirect = 500;
    public final static String LN_dITRedirect = "dITRedirect";
    public final static String OBJ_dITRedirect = OBJ_pilotAttributeType + ".54";

    public final static int    NID_audio = 501;
    public final static String SN_audio = "audio";
    public final static String OBJ_audio = OBJ_pilotAttributeType + ".55";

    public final static int    NID_documentPublisher = 502;
    public final static String LN_documentPublisher = "documentPublisher";
    public final static String OBJ_documentPublisher = OBJ_pilotAttributeType + ".56";

    public final static int    NID_x500UniqueIdentifier = 503;
    public final static String LN_x500UniqueIdentifier = "x500UniqueIdentifier";
    public final static String OBJ_x500UniqueIdentifier = OBJ_X509 + ".45";

    public final static int    NID_mime_mhs = 504;
    public final static String SN_mime_mhs = "mime-mhs";
    public final static String LN_mime_mhs = "MIME MHS";
    public final static String OBJ_mime_mhs = OBJ_Mail + ".1";

    public final static int    NID_mime_mhs_headings = 505;
    public final static String SN_mime_mhs_headings = "mime-mhs-headings";
    public final static String LN_mime_mhs_headings = "mime-mhs-headings";
    public final static String OBJ_mime_mhs_headings = OBJ_mime_mhs + ".1";

    public final static int    NID_mime_mhs_bodies = 506;
    public final static String SN_mime_mhs_bodies = "mime-mhs-bodies";
    public final static String LN_mime_mhs_bodies = "mime-mhs-bodies";
    public final static String OBJ_mime_mhs_bodies = OBJ_mime_mhs + ".2";

    public final static int    NID_id_hex_partial_message = 507;
    public final static String SN_id_hex_partial_message = "id-hex-partial-message";
    public final static String LN_id_hex_partial_message = "id-hex-partial-message";
    public final static String OBJ_id_hex_partial_message = OBJ_mime_mhs_headings + ".1";

    public final static int    NID_id_hex_multipart_message = 508;
    public final static String SN_id_hex_multipart_message = "id-hex-multipart-message";
    public final static String LN_id_hex_multipart_message = "id-hex-multipart-message";
    public final static String OBJ_id_hex_multipart_message = OBJ_mime_mhs_headings + ".2";

    public final static int    NID_generationQualifier = 509;
    public final static String LN_generationQualifier = "generationQualifier";
    public final static String OBJ_generationQualifier = OBJ_X509 + ".44";

    public final static int    NID_pseudonym = 510;
    public final static String LN_pseudonym = "pseudonym";
    public final static String OBJ_pseudonym = OBJ_X509 + ".65";

    public final static int    NID_id_set = 512;
    public final static String SN_id_set = "id-set";
    public final static String LN_id_set = "Secure Electronic Transactions";
    public final static String OBJ_id_set = OBJ_international_organizations + ".42";

    public final static int    NID_set_ctype = 513;
    public final static String SN_set_ctype = "set-ctype";
    public final static String LN_set_ctype = "content types";
    public final static String OBJ_set_ctype = OBJ_id_set + ".0";

    public final static int    NID_set_msgExt = 514;
    public final static String SN_set_msgExt = "set-msgExt";
    public final static String LN_set_msgExt = "message extensions";
    public final static String OBJ_set_msgExt = OBJ_id_set + ".1";

    public final static int    NID_set_attr = 515;
    public final static String SN_set_attr = "set-attr";
    public final static String OBJ_set_attr = OBJ_id_set + ".3";

    public final static int    NID_set_policy = 516;
    public final static String SN_set_policy = "set-policy";
    public final static String OBJ_set_policy = OBJ_id_set + ".5";

    public final static int    NID_set_certExt = 517;
    public final static String SN_set_certExt = "set-certExt";
    public final static String LN_set_certExt = "certificate extensions";
    public final static String OBJ_set_certExt = OBJ_id_set + ".7";

    public final static int    NID_set_brand = 518;
    public final static String SN_set_brand = "set-brand";
    public final static String OBJ_set_brand = OBJ_id_set + ".8";

    public final static int    NID_setct_PANData = 519;
    public final static String SN_setct_PANData = "setct-PANData";
    public final static String OBJ_setct_PANData = OBJ_set_ctype + ".0";

    public final static int    NID_setct_PANToken = 520;
    public final static String SN_setct_PANToken = "setct-PANToken";
    public final static String OBJ_setct_PANToken = OBJ_set_ctype + ".1";

    public final static int    NID_setct_PANOnly = 521;
    public final static String SN_setct_PANOnly = "setct-PANOnly";
    public final static String OBJ_setct_PANOnly = OBJ_set_ctype + ".2";

    public final static int    NID_setct_OIData = 522;
    public final static String SN_setct_OIData = "setct-OIData";
    public final static String OBJ_setct_OIData = OBJ_set_ctype + ".3";

    public final static int    NID_setct_PI = 523;
    public final static String SN_setct_PI = "setct-PI";
    public final static String OBJ_setct_PI = OBJ_set_ctype + ".4";

    public final static int    NID_setct_PIData = 524;
    public final static String SN_setct_PIData = "setct-PIData";
    public final static String OBJ_setct_PIData = OBJ_set_ctype + ".5";

    public final static int    NID_setct_PIDataUnsigned = 525;
    public final static String SN_setct_PIDataUnsigned = "setct-PIDataUnsigned";
    public final static String OBJ_setct_PIDataUnsigned = OBJ_set_ctype + ".6";

    public final static int    NID_setct_HODInput = 526;
    public final static String SN_setct_HODInput = "setct-HODInput";
    public final static String OBJ_setct_HODInput = OBJ_set_ctype + ".7";

    public final static int    NID_setct_AuthResBaggage = 527;
    public final static String SN_setct_AuthResBaggage = "setct-AuthResBaggage";
    public final static String OBJ_setct_AuthResBaggage = OBJ_set_ctype + ".8";

    public final static int    NID_setct_AuthRevReqBaggage = 528;
    public final static String SN_setct_AuthRevReqBaggage = "setct-AuthRevReqBaggage";
    public final static String OBJ_setct_AuthRevReqBaggage = OBJ_set_ctype + ".9";

    public final static int    NID_setct_AuthRevResBaggage = 529;
    public final static String SN_setct_AuthRevResBaggage = "setct-AuthRevResBaggage";
    public final static String OBJ_setct_AuthRevResBaggage = OBJ_set_ctype + ".10";

    public final static int    NID_setct_CapTokenSeq = 530;
    public final static String SN_setct_CapTokenSeq = "setct-CapTokenSeq";
    public final static String OBJ_setct_CapTokenSeq = OBJ_set_ctype + ".11";

    public final static int    NID_setct_PInitResData = 531;
    public final static String SN_setct_PInitResData = "setct-PInitResData";
    public final static String OBJ_setct_PInitResData = OBJ_set_ctype + ".12";

    public final static int    NID_setct_PI_TBS = 532;
    public final static String SN_setct_PI_TBS = "setct-PI-TBS";
    public final static String OBJ_setct_PI_TBS = OBJ_set_ctype + ".13";

    public final static int    NID_setct_PResData = 533;
    public final static String SN_setct_PResData = "setct-PResData";
    public final static String OBJ_setct_PResData = OBJ_set_ctype + ".14";

    public final static int    NID_setct_AuthReqTBS = 534;
    public final static String SN_setct_AuthReqTBS = "setct-AuthReqTBS";
    public final static String OBJ_setct_AuthReqTBS = OBJ_set_ctype + ".16";

    public final static int    NID_setct_AuthResTBS = 535;
    public final static String SN_setct_AuthResTBS = "setct-AuthResTBS";
    public final static String OBJ_setct_AuthResTBS = OBJ_set_ctype + ".17";

    public final static int    NID_setct_AuthResTBSX = 536;
    public final static String SN_setct_AuthResTBSX = "setct-AuthResTBSX";
    public final static String OBJ_setct_AuthResTBSX = OBJ_set_ctype + ".18";

    public final static int    NID_setct_AuthTokenTBS = 537;
    public final static String SN_setct_AuthTokenTBS = "setct-AuthTokenTBS";
    public final static String OBJ_setct_AuthTokenTBS = OBJ_set_ctype + ".19";

    public final static int    NID_setct_CapTokenData = 538;
    public final static String SN_setct_CapTokenData = "setct-CapTokenData";
    public final static String OBJ_setct_CapTokenData = OBJ_set_ctype + ".20";

    public final static int    NID_setct_CapTokenTBS = 539;
    public final static String SN_setct_CapTokenTBS = "setct-CapTokenTBS";
    public final static String OBJ_setct_CapTokenTBS = OBJ_set_ctype + ".21";

    public final static int    NID_setct_AcqCardCodeMsg = 540;
    public final static String SN_setct_AcqCardCodeMsg = "setct-AcqCardCodeMsg";
    public final static String OBJ_setct_AcqCardCodeMsg = OBJ_set_ctype + ".22";

    public final static int    NID_setct_AuthRevReqTBS = 541;
    public final static String SN_setct_AuthRevReqTBS = "setct-AuthRevReqTBS";
    public final static String OBJ_setct_AuthRevReqTBS = OBJ_set_ctype + ".23";

    public final static int    NID_setct_AuthRevResData = 542;
    public final static String SN_setct_AuthRevResData = "setct-AuthRevResData";
    public final static String OBJ_setct_AuthRevResData = OBJ_set_ctype + ".24";

    public final static int    NID_setct_AuthRevResTBS = 543;
    public final static String SN_setct_AuthRevResTBS = "setct-AuthRevResTBS";
    public final static String OBJ_setct_AuthRevResTBS = OBJ_set_ctype + ".25";

    public final static int    NID_setct_CapReqTBS = 544;
    public final static String SN_setct_CapReqTBS = "setct-CapReqTBS";
    public final static String OBJ_setct_CapReqTBS = OBJ_set_ctype + ".26";

    public final static int    NID_setct_CapReqTBSX = 545;
    public final static String SN_setct_CapReqTBSX = "setct-CapReqTBSX";
    public final static String OBJ_setct_CapReqTBSX = OBJ_set_ctype + ".27";

    public final static int    NID_setct_CapResData = 546;
    public final static String SN_setct_CapResData = "setct-CapResData";
    public final static String OBJ_setct_CapResData = OBJ_set_ctype + ".28";

    public final static int    NID_setct_CapRevReqTBS = 547;
    public final static String SN_setct_CapRevReqTBS = "setct-CapRevReqTBS";
    public final static String OBJ_setct_CapRevReqTBS = OBJ_set_ctype + ".29";

    public final static int    NID_setct_CapRevReqTBSX = 548;
    public final static String SN_setct_CapRevReqTBSX = "setct-CapRevReqTBSX";
    public final static String OBJ_setct_CapRevReqTBSX = OBJ_set_ctype + ".30";

    public final static int    NID_setct_CapRevResData = 549;
    public final static String SN_setct_CapRevResData = "setct-CapRevResData";
    public final static String OBJ_setct_CapRevResData = OBJ_set_ctype + ".31";

    public final static int    NID_setct_CredReqTBS = 550;
    public final static String SN_setct_CredReqTBS = "setct-CredReqTBS";
    public final static String OBJ_setct_CredReqTBS = OBJ_set_ctype + ".32";

    public final static int    NID_setct_CredReqTBSX = 551;
    public final static String SN_setct_CredReqTBSX = "setct-CredReqTBSX";
    public final static String OBJ_setct_CredReqTBSX = OBJ_set_ctype + ".33";

    public final static int    NID_setct_CredResData = 552;
    public final static String SN_setct_CredResData = "setct-CredResData";
    public final static String OBJ_setct_CredResData = OBJ_set_ctype + ".34";

    public final static int    NID_setct_CredRevReqTBS = 553;
    public final static String SN_setct_CredRevReqTBS = "setct-CredRevReqTBS";
    public final static String OBJ_setct_CredRevReqTBS = OBJ_set_ctype + ".35";

    public final static int    NID_setct_CredRevReqTBSX = 554;
    public final static String SN_setct_CredRevReqTBSX = "setct-CredRevReqTBSX";
    public final static String OBJ_setct_CredRevReqTBSX = OBJ_set_ctype + ".36";

    public final static int    NID_setct_CredRevResData = 555;
    public final static String SN_setct_CredRevResData = "setct-CredRevResData";
    public final static String OBJ_setct_CredRevResData = OBJ_set_ctype + ".37";

    public final static int    NID_setct_PCertReqData = 556;
    public final static String SN_setct_PCertReqData = "setct-PCertReqData";
    public final static String OBJ_setct_PCertReqData = OBJ_set_ctype + ".38";

    public final static int    NID_setct_PCertResTBS = 557;
    public final static String SN_setct_PCertResTBS = "setct-PCertResTBS";
    public final static String OBJ_setct_PCertResTBS = OBJ_set_ctype + ".39";

    public final static int    NID_setct_BatchAdminReqData = 558;
    public final static String SN_setct_BatchAdminReqData = "setct-BatchAdminReqData";
    public final static String OBJ_setct_BatchAdminReqData = OBJ_set_ctype + ".40";

    public final static int    NID_setct_BatchAdminResData = 559;
    public final static String SN_setct_BatchAdminResData = "setct-BatchAdminResData";
    public final static String OBJ_setct_BatchAdminResData = OBJ_set_ctype + ".41";

    public final static int    NID_setct_CardCInitResTBS = 560;
    public final static String SN_setct_CardCInitResTBS = "setct-CardCInitResTBS";
    public final static String OBJ_setct_CardCInitResTBS = OBJ_set_ctype + ".42";

    public final static int    NID_setct_MeAqCInitResTBS = 561;
    public final static String SN_setct_MeAqCInitResTBS = "setct-MeAqCInitResTBS";
    public final static String OBJ_setct_MeAqCInitResTBS = OBJ_set_ctype + ".43";

    public final static int    NID_setct_RegFormResTBS = 562;
    public final static String SN_setct_RegFormResTBS = "setct-RegFormResTBS";
    public final static String OBJ_setct_RegFormResTBS = OBJ_set_ctype + ".44";

    public final static int    NID_setct_CertReqData = 563;
    public final static String SN_setct_CertReqData = "setct-CertReqData";
    public final static String OBJ_setct_CertReqData = OBJ_set_ctype + ".45";

    public final static int    NID_setct_CertReqTBS = 564;
    public final static String SN_setct_CertReqTBS = "setct-CertReqTBS";
    public final static String OBJ_setct_CertReqTBS = OBJ_set_ctype + ".46";

    public final static int    NID_setct_CertResData = 565;
    public final static String SN_setct_CertResData = "setct-CertResData";
    public final static String OBJ_setct_CertResData = OBJ_set_ctype + ".47";

    public final static int    NID_setct_CertInqReqTBS = 566;
    public final static String SN_setct_CertInqReqTBS = "setct-CertInqReqTBS";
    public final static String OBJ_setct_CertInqReqTBS = OBJ_set_ctype + ".48";

    public final static int    NID_setct_ErrorTBS = 567;
    public final static String SN_setct_ErrorTBS = "setct-ErrorTBS";
    public final static String OBJ_setct_ErrorTBS = OBJ_set_ctype + ".49";

    public final static int    NID_setct_PIDualSignedTBE = 568;
    public final static String SN_setct_PIDualSignedTBE = "setct-PIDualSignedTBE";
    public final static String OBJ_setct_PIDualSignedTBE = OBJ_set_ctype + ".50";

    public final static int    NID_setct_PIUnsignedTBE = 569;
    public final static String SN_setct_PIUnsignedTBE = "setct-PIUnsignedTBE";
    public final static String OBJ_setct_PIUnsignedTBE = OBJ_set_ctype + ".51";

    public final static int    NID_setct_AuthReqTBE = 570;
    public final static String SN_setct_AuthReqTBE = "setct-AuthReqTBE";
    public final static String OBJ_setct_AuthReqTBE = OBJ_set_ctype + ".52";

    public final static int    NID_setct_AuthResTBE = 571;
    public final static String SN_setct_AuthResTBE = "setct-AuthResTBE";
    public final static String OBJ_setct_AuthResTBE = OBJ_set_ctype + ".53";

    public final static int    NID_setct_AuthResTBEX = 572;
    public final static String SN_setct_AuthResTBEX = "setct-AuthResTBEX";
    public final static String OBJ_setct_AuthResTBEX = OBJ_set_ctype + ".54";

    public final static int    NID_setct_AuthTokenTBE = 573;
    public final static String SN_setct_AuthTokenTBE = "setct-AuthTokenTBE";
    public final static String OBJ_setct_AuthTokenTBE = OBJ_set_ctype + ".55";

    public final static int    NID_setct_CapTokenTBE = 574;
    public final static String SN_setct_CapTokenTBE = "setct-CapTokenTBE";
    public final static String OBJ_setct_CapTokenTBE = OBJ_set_ctype + ".56";

    public final static int    NID_setct_CapTokenTBEX = 575;
    public final static String SN_setct_CapTokenTBEX = "setct-CapTokenTBEX";
    public final static String OBJ_setct_CapTokenTBEX = OBJ_set_ctype + ".57";

    public final static int    NID_setct_AcqCardCodeMsgTBE = 576;
    public final static String SN_setct_AcqCardCodeMsgTBE = "setct-AcqCardCodeMsgTBE";
    public final static String OBJ_setct_AcqCardCodeMsgTBE = OBJ_set_ctype + ".58";

    public final static int    NID_setct_AuthRevReqTBE = 577;
    public final static String SN_setct_AuthRevReqTBE = "setct-AuthRevReqTBE";
    public final static String OBJ_setct_AuthRevReqTBE = OBJ_set_ctype + ".59";

    public final static int    NID_setct_AuthRevResTBE = 578;
    public final static String SN_setct_AuthRevResTBE = "setct-AuthRevResTBE";
    public final static String OBJ_setct_AuthRevResTBE = OBJ_set_ctype + ".60";

    public final static int    NID_setct_AuthRevResTBEB = 579;
    public final static String SN_setct_AuthRevResTBEB = "setct-AuthRevResTBEB";
    public final static String OBJ_setct_AuthRevResTBEB = OBJ_set_ctype + ".61";

    public final static int    NID_setct_CapReqTBE = 580;
    public final static String SN_setct_CapReqTBE = "setct-CapReqTBE";
    public final static String OBJ_setct_CapReqTBE = OBJ_set_ctype + ".62";

    public final static int    NID_setct_CapReqTBEX = 581;
    public final static String SN_setct_CapReqTBEX = "setct-CapReqTBEX";
    public final static String OBJ_setct_CapReqTBEX = OBJ_set_ctype + ".63";

    public final static int    NID_setct_CapResTBE = 582;
    public final static String SN_setct_CapResTBE = "setct-CapResTBE";
    public final static String OBJ_setct_CapResTBE = OBJ_set_ctype + ".64";

    public final static int    NID_setct_CapRevReqTBE = 583;
    public final static String SN_setct_CapRevReqTBE = "setct-CapRevReqTBE";
    public final static String OBJ_setct_CapRevReqTBE = OBJ_set_ctype + ".65";

    public final static int    NID_setct_CapRevReqTBEX = 584;
    public final static String SN_setct_CapRevReqTBEX = "setct-CapRevReqTBEX";
    public final static String OBJ_setct_CapRevReqTBEX = OBJ_set_ctype + ".66";

    public final static int    NID_setct_CapRevResTBE = 585;
    public final static String SN_setct_CapRevResTBE = "setct-CapRevResTBE";
    public final static String OBJ_setct_CapRevResTBE = OBJ_set_ctype + ".67";

    public final static int    NID_setct_CredReqTBE = 586;
    public final static String SN_setct_CredReqTBE = "setct-CredReqTBE";
    public final static String OBJ_setct_CredReqTBE = OBJ_set_ctype + ".68";

    public final static int    NID_setct_CredReqTBEX = 587;
    public final static String SN_setct_CredReqTBEX = "setct-CredReqTBEX";
    public final static String OBJ_setct_CredReqTBEX = OBJ_set_ctype + ".69";

    public final static int    NID_setct_CredResTBE = 588;
    public final static String SN_setct_CredResTBE = "setct-CredResTBE";
    public final static String OBJ_setct_CredResTBE = OBJ_set_ctype + ".70";

    public final static int    NID_setct_CredRevReqTBE = 589;
    public final static String SN_setct_CredRevReqTBE = "setct-CredRevReqTBE";
    public final static String OBJ_setct_CredRevReqTBE = OBJ_set_ctype + ".71";

    public final static int    NID_setct_CredRevReqTBEX = 590;
    public final static String SN_setct_CredRevReqTBEX = "setct-CredRevReqTBEX";
    public final static String OBJ_setct_CredRevReqTBEX = OBJ_set_ctype + ".72";

    public final static int    NID_setct_CredRevResTBE = 591;
    public final static String SN_setct_CredRevResTBE = "setct-CredRevResTBE";
    public final static String OBJ_setct_CredRevResTBE = OBJ_set_ctype + ".73";

    public final static int    NID_setct_BatchAdminReqTBE = 592;
    public final static String SN_setct_BatchAdminReqTBE = "setct-BatchAdminReqTBE";
    public final static String OBJ_setct_BatchAdminReqTBE = OBJ_set_ctype + ".74";

    public final static int    NID_setct_BatchAdminResTBE = 593;
    public final static String SN_setct_BatchAdminResTBE = "setct-BatchAdminResTBE";
    public final static String OBJ_setct_BatchAdminResTBE = OBJ_set_ctype + ".75";

    public final static int    NID_setct_RegFormReqTBE = 594;
    public final static String SN_setct_RegFormReqTBE = "setct-RegFormReqTBE";
    public final static String OBJ_setct_RegFormReqTBE = OBJ_set_ctype + ".76";

    public final static int    NID_setct_CertReqTBE = 595;
    public final static String SN_setct_CertReqTBE = "setct-CertReqTBE";
    public final static String OBJ_setct_CertReqTBE = OBJ_set_ctype + ".77";

    public final static int    NID_setct_CertReqTBEX = 596;
    public final static String SN_setct_CertReqTBEX = "setct-CertReqTBEX";
    public final static String OBJ_setct_CertReqTBEX = OBJ_set_ctype + ".78";

    public final static int    NID_setct_CertResTBE = 597;
    public final static String SN_setct_CertResTBE = "setct-CertResTBE";
    public final static String OBJ_setct_CertResTBE = OBJ_set_ctype + ".79";

    public final static int    NID_setct_CRLNotificationTBS = 598;
    public final static String SN_setct_CRLNotificationTBS = "setct-CRLNotificationTBS";
    public final static String OBJ_setct_CRLNotificationTBS = OBJ_set_ctype + ".80";

    public final static int    NID_setct_CRLNotificationResTBS = 599;
    public final static String SN_setct_CRLNotificationResTBS = "setct-CRLNotificationResTBS";
    public final static String OBJ_setct_CRLNotificationResTBS = OBJ_set_ctype + ".81";

    public final static int    NID_setct_BCIDistributionTBS = 600;
    public final static String SN_setct_BCIDistributionTBS = "setct-BCIDistributionTBS";
    public final static String OBJ_setct_BCIDistributionTBS = OBJ_set_ctype + ".82";

    public final static int    NID_setext_genCrypt = 601;
    public final static String SN_setext_genCrypt = "setext-genCrypt";
    public final static String LN_setext_genCrypt = "generic cryptogram";
    public final static String OBJ_setext_genCrypt = OBJ_set_msgExt + ".1";

    public final static int    NID_setext_miAuth = 602;
    public final static String SN_setext_miAuth = "setext-miAuth";
    public final static String LN_setext_miAuth = "merchant initiated auth";
    public final static String OBJ_setext_miAuth = OBJ_set_msgExt + ".3";

    public final static int    NID_setext_pinSecure = 603;
    public final static String SN_setext_pinSecure = "setext-pinSecure";
    public final static String OBJ_setext_pinSecure = OBJ_set_msgExt + ".4";

    public final static int    NID_setext_pinAny = 604;
    public final static String SN_setext_pinAny = "setext-pinAny";
    public final static String OBJ_setext_pinAny = OBJ_set_msgExt + ".5";

    public final static int    NID_setext_track2 = 605;
    public final static String SN_setext_track2 = "setext-track2";
    public final static String OBJ_setext_track2 = OBJ_set_msgExt + ".7";

    public final static int    NID_setext_cv = 606;
    public final static String SN_setext_cv = "setext-cv";
    public final static String LN_setext_cv = "additional verification";
    public final static String OBJ_setext_cv = OBJ_set_msgExt + ".8";

    public final static int    NID_set_policy_root = 607;
    public final static String SN_set_policy_root = "set-policy-root";
    public final static String OBJ_set_policy_root = OBJ_set_policy + ".0";

    public final static int    NID_setCext_hashedRoot = 608;
    public final static String SN_setCext_hashedRoot = "setCext-hashedRoot";
    public final static String OBJ_setCext_hashedRoot = OBJ_set_certExt + ".0";

    public final static int    NID_setCext_certType = 609;
    public final static String SN_setCext_certType = "setCext-certType";
    public final static String OBJ_setCext_certType = OBJ_set_certExt + ".1";

    public final static int    NID_setCext_merchData = 610;
    public final static String SN_setCext_merchData = "setCext-merchData";
    public final static String OBJ_setCext_merchData = OBJ_set_certExt + ".2";

    public final static int    NID_setCext_cCertRequired = 611;
    public final static String SN_setCext_cCertRequired = "setCext-cCertRequired";
    public final static String OBJ_setCext_cCertRequired = OBJ_set_certExt + ".3";

    public final static int    NID_setCext_tunneling = 612;
    public final static String SN_setCext_tunneling = "setCext-tunneling";
    public final static String OBJ_setCext_tunneling = OBJ_set_certExt + ".4";

    public final static int    NID_setCext_setExt = 613;
    public final static String SN_setCext_setExt = "setCext-setExt";
    public final static String OBJ_setCext_setExt = OBJ_set_certExt + ".5";

    public final static int    NID_setCext_setQualf = 614;
    public final static String SN_setCext_setQualf = "setCext-setQualf";
    public final static String OBJ_setCext_setQualf = OBJ_set_certExt + ".6";

    public final static int    NID_setCext_PGWYcapabilities = 615;
    public final static String SN_setCext_PGWYcapabilities = "setCext-PGWYcapabilities";
    public final static String OBJ_setCext_PGWYcapabilities = OBJ_set_certExt + ".7";

    public final static int    NID_setCext_TokenIdentifier = 616;
    public final static String SN_setCext_TokenIdentifier = "setCext-TokenIdentifier";
    public final static String OBJ_setCext_TokenIdentifier = OBJ_set_certExt + ".8";

    public final static int    NID_setCext_Track2Data = 617;
    public final static String SN_setCext_Track2Data = "setCext-Track2Data";
    public final static String OBJ_setCext_Track2Data = OBJ_set_certExt + ".9";

    public final static int    NID_setCext_TokenType = 618;
    public final static String SN_setCext_TokenType = "setCext-TokenType";
    public final static String OBJ_setCext_TokenType = OBJ_set_certExt + ".10";

    public final static int    NID_setCext_IssuerCapabilities = 619;
    public final static String SN_setCext_IssuerCapabilities = "setCext-IssuerCapabilities";
    public final static String OBJ_setCext_IssuerCapabilities = OBJ_set_certExt + ".11";

    public final static int    NID_setAttr_Cert = 620;
    public final static String SN_setAttr_Cert = "setAttr-Cert";
    public final static String OBJ_setAttr_Cert = OBJ_set_attr + ".0";

    public final static int    NID_setAttr_PGWYcap = 621;
    public final static String SN_setAttr_PGWYcap = "setAttr-PGWYcap";
    public final static String LN_setAttr_PGWYcap = "payment gateway capabilities";
    public final static String OBJ_setAttr_PGWYcap = OBJ_set_attr + ".1";

    public final static int    NID_setAttr_TokenType = 622;
    public final static String SN_setAttr_TokenType = "setAttr-TokenType";
    public final static String OBJ_setAttr_TokenType = OBJ_set_attr + ".2";

    public final static int    NID_setAttr_IssCap = 623;
    public final static String SN_setAttr_IssCap = "setAttr-IssCap";
    public final static String LN_setAttr_IssCap = "issuer capabilities";
    public final static String OBJ_setAttr_IssCap = OBJ_set_attr + ".3";

    public final static int    NID_set_rootKeyThumb = 624;
    public final static String SN_set_rootKeyThumb = "set-rootKeyThumb";
    public final static String OBJ_set_rootKeyThumb = OBJ_setAttr_Cert + ".0";

    public final static int    NID_set_addPolicy = 625;
    public final static String SN_set_addPolicy = "set-addPolicy";
    public final static String OBJ_set_addPolicy = OBJ_setAttr_Cert + ".1";

    public final static int    NID_setAttr_Token_EMV = 626;
    public final static String SN_setAttr_Token_EMV = "setAttr-Token-EMV";
    public final static String OBJ_setAttr_Token_EMV = OBJ_setAttr_TokenType + ".1";

    public final static int    NID_setAttr_Token_B0Prime = 627;
    public final static String SN_setAttr_Token_B0Prime = "setAttr-Token-B0Prime";
    public final static String OBJ_setAttr_Token_B0Prime = OBJ_setAttr_TokenType + ".2";

    public final static int    NID_setAttr_IssCap_CVM = 628;
    public final static String SN_setAttr_IssCap_CVM = "setAttr-IssCap-CVM";
    public final static String OBJ_setAttr_IssCap_CVM = OBJ_setAttr_IssCap + ".3";

    public final static int    NID_setAttr_IssCap_T2 = 629;
    public final static String SN_setAttr_IssCap_T2 = "setAttr-IssCap-T2";
    public final static String OBJ_setAttr_IssCap_T2 = OBJ_setAttr_IssCap + ".4";

    public final static int    NID_setAttr_IssCap_Sig = 630;
    public final static String SN_setAttr_IssCap_Sig = "setAttr-IssCap-Sig";
    public final static String OBJ_setAttr_IssCap_Sig = OBJ_setAttr_IssCap + ".5";

    public final static int    NID_setAttr_GenCryptgrm = 631;
    public final static String SN_setAttr_GenCryptgrm = "setAttr-GenCryptgrm";
    public final static String LN_setAttr_GenCryptgrm = "generate cryptogram";
    public final static String OBJ_setAttr_GenCryptgrm = OBJ_setAttr_IssCap_CVM + ".1";

    public final static int    NID_setAttr_T2Enc = 632;
    public final static String SN_setAttr_T2Enc = "setAttr-T2Enc";
    public final static String LN_setAttr_T2Enc = "encrypted track 2";
    public final static String OBJ_setAttr_T2Enc = OBJ_setAttr_IssCap_T2 + ".1";

    public final static int    NID_setAttr_T2cleartxt = 633;
    public final static String SN_setAttr_T2cleartxt = "setAttr-T2cleartxt";
    public final static String LN_setAttr_T2cleartxt = "cleartext track 2";
    public final static String OBJ_setAttr_T2cleartxt = OBJ_setAttr_IssCap_T2 + ".2";

    public final static int    NID_setAttr_TokICCsig = 634;
    public final static String SN_setAttr_TokICCsig = "setAttr-TokICCsig";
    public final static String LN_setAttr_TokICCsig = "ICC or token signature";
    public final static String OBJ_setAttr_TokICCsig = OBJ_setAttr_IssCap_Sig + ".1";

    public final static int    NID_setAttr_SecDevSig = 635;
    public final static String SN_setAttr_SecDevSig = "setAttr-SecDevSig";
    public final static String LN_setAttr_SecDevSig = "secure device signature";
    public final static String OBJ_setAttr_SecDevSig = OBJ_setAttr_IssCap_Sig + ".2";

    public final static int    NID_set_brand_IATA_ATA = 636;
    public final static String SN_set_brand_IATA_ATA = "set-brand-IATA-ATA";
    public final static String OBJ_set_brand_IATA_ATA = OBJ_set_brand + ".1";

    public final static int    NID_set_brand_Diners = 637;
    public final static String SN_set_brand_Diners = "set-brand-Diners";
    public final static String OBJ_set_brand_Diners = OBJ_set_brand + ".30";

    public final static int    NID_set_brand_AmericanExpress = 638;
    public final static String SN_set_brand_AmericanExpress = "set-brand-AmericanExpress";
    public final static String OBJ_set_brand_AmericanExpress = OBJ_set_brand + ".34";

    public final static int    NID_set_brand_JCB = 639;
    public final static String SN_set_brand_JCB = "set-brand-JCB";
    public final static String OBJ_set_brand_JCB = OBJ_set_brand + ".35";

    public final static int    NID_set_brand_Visa = 640;
    public final static String SN_set_brand_Visa = "set-brand-Visa";
    public final static String OBJ_set_brand_Visa = OBJ_set_brand + ".4";

    public final static int    NID_set_brand_MasterCard = 641;
    public final static String SN_set_brand_MasterCard = "set-brand-MasterCard";
    public final static String OBJ_set_brand_MasterCard = OBJ_set_brand + ".5";

    public final static int    NID_set_brand_Novus = 642;
    public final static String SN_set_brand_Novus = "set-brand-Novus";
    public final static String OBJ_set_brand_Novus = OBJ_set_brand + ".6011";

    public final static int    NID_des_cdmf = 643;
    public final static String SN_des_cdmf = "DES-CDMF";
    public final static String LN_des_cdmf = "des-cdmf";
    public final static String OBJ_des_cdmf = OBJ_rsadsi + ".3.10";

    public final static int    NID_rsaOAEPEncryptionSET = 644;
    public final static String SN_rsaOAEPEncryptionSET = "rsaOAEPEncryptionSET";
    public final static String OBJ_rsaOAEPEncryptionSET = OBJ_rsadsi + ".1.1.6";

    public final static int    NID_ms_smartcard_login = 648;
    public final static String SN_ms_smartcard_login = "msSmartcardLogin";
    public final static String LN_ms_smartcard_login = "Microsoft Smartcardlogin";
    public final static String OBJ_ms_smartcard_login = "1.3.6.1.4.1.311.20.2.2";

    public final static int    NID_ms_upn = 649;
    public final static String SN_ms_upn = "msUPN";
    public final static String LN_ms_upn = "Microsoft Universal Principal Name";
    public final static String OBJ_ms_upn = "1.3.6.1.4.1.311.20.2.3";

    public final static int    NID_aes_128_cfb1 = 650;
    public final static String SN_aes_128_cfb1 = "AES-128-CFB1";
    public final static String LN_aes_128_cfb1 = "aes-128-cfb1";

    public final static int    NID_aes_192_cfb1 = 651;
    public final static String SN_aes_192_cfb1 = "AES-192-CFB1";
    public final static String LN_aes_192_cfb1 = "aes-192-cfb1";

    public final static int    NID_aes_256_cfb1 = 652;
    public final static String SN_aes_256_cfb1 = "AES-256-CFB1";
    public final static String LN_aes_256_cfb1 = "aes-256-cfb1";

    public final static int    NID_aes_128_cfb8 = 653;
    public final static String SN_aes_128_cfb8 = "AES-128-CFB8";
    public final static String LN_aes_128_cfb8 = "aes-128-cfb8";

    public final static int    NID_aes_192_cfb8 = 654;
    public final static String SN_aes_192_cfb8 = "AES-192-CFB8";
    public final static String LN_aes_192_cfb8 = "aes-192-cfb8";

    public final static int    NID_aes_256_cfb8 = 655;
    public final static String SN_aes_256_cfb8 = "AES-256-CFB8";
    public final static String LN_aes_256_cfb8 = "aes-256-cfb8";

    public final static int    NID_des_cfb1 = 656;
    public final static String SN_des_cfb1 = "DES-CFB1";
    public final static String LN_des_cfb1 = "des-cfb1";

    public final static int    NID_des_cfb8 = 657;
    public final static String SN_des_cfb8 = "DES-CFB8";
    public final static String LN_des_cfb8 = "des-cfb8";

    public final static int    NID_des_ede3_cfb1 = 658;
    public final static String SN_des_ede3_cfb1 = "DES-EDE3-CFB1";
    public final static String LN_des_ede3_cfb1 = "des-ede3-cfb1";

    public final static int    NID_des_ede3_cfb8 = 659;
    public final static String SN_des_ede3_cfb8 = "DES-EDE3-CFB8";
    public final static String LN_des_ede3_cfb8 = "des-ede3-cfb8";

    public final static int    NID_streetAddress = 660;
    public final static String LN_streetAddress = "streetAddress";
    public final static String OBJ_streetAddress = OBJ_X509 + ".9";

    public final static int    NID_postalCode = 661;
    public final static String LN_postalCode = "postalCode";
    public final static String OBJ_postalCode = OBJ_X509 + ".17";

    public final static int    NID_id_ppl = 662;
    public final static String SN_id_ppl = "id-ppl";
    public final static String OBJ_id_ppl = OBJ_id_pkix + ".21";

    public final static int    NID_proxyCertInfo = 663;
    public final static String SN_proxyCertInfo = "proxyCertInfo";
    public final static String LN_proxyCertInfo = "Proxy Certificate Information";
    public final static String OBJ_proxyCertInfo = OBJ_id_pe + ".14";

    public final static int    NID_id_ppl_anyLanguage = 664;
    public final static String SN_id_ppl_anyLanguage = "id-ppl-anyLanguage";
    public final static String LN_id_ppl_anyLanguage = "Any language";
    public final static String OBJ_id_ppl_anyLanguage = OBJ_id_ppl + ".0";

    public final static int    NID_id_ppl_inheritAll = 665;
    public final static String SN_id_ppl_inheritAll = "id-ppl-inheritAll";
    public final static String LN_id_ppl_inheritAll = "Inherit all";
    public final static String OBJ_id_ppl_inheritAll = OBJ_id_ppl + ".1";

    public final static int    NID_name_constraints = 666;
    public final static String SN_name_constraints = "nameConstraints";
    public final static String LN_name_constraints = "X509v3 Name Constraints";
    public final static String OBJ_name_constraints = OBJ_id_ce + ".30";

    public final static int    NID_Independent = 667;
    public final static String SN_Independent = "id-ppl-independent";
    public final static String LN_Independent = "Independent";
    public final static String OBJ_Independent = OBJ_id_ppl + ".2";

    public final static int    NID_sha256WithRSAEncryption = 668;
    public final static String SN_sha256WithRSAEncryption = "RSA-SHA256";
    public final static String LN_sha256WithRSAEncryption = "sha256WithRSAEncryption";
    public final static String OBJ_sha256WithRSAEncryption = OBJ_pkcs1 + ".11";

    public final static int    NID_sha384WithRSAEncryption = 669;
    public final static String SN_sha384WithRSAEncryption = "RSA-SHA384";
    public final static String LN_sha384WithRSAEncryption = "sha384WithRSAEncryption";
    public final static String OBJ_sha384WithRSAEncryption = OBJ_pkcs1 + ".12";

    public final static int    NID_sha512WithRSAEncryption = 670;
    public final static String SN_sha512WithRSAEncryption = "RSA-SHA512";
    public final static String LN_sha512WithRSAEncryption = "sha512WithRSAEncryption";
    public final static String OBJ_sha512WithRSAEncryption = OBJ_pkcs1 + ".13";

    public final static int    NID_sha224WithRSAEncryption = 671;
    public final static String SN_sha224WithRSAEncryption = "RSA-SHA224";
    public final static String LN_sha224WithRSAEncryption = "sha224WithRSAEncryption";
    public final static String OBJ_sha224WithRSAEncryption = OBJ_pkcs1 + ".14";

    public final static int    NID_sha256 = 672;
    public final static String SN_sha256 = "SHA256";
    public final static String LN_sha256 = "sha256";
    public final static String OBJ_sha256 = OBJ_nist_hashalgs + ".1";

    public final static int    NID_sha384 = 673;
    public final static String SN_sha384 = "SHA384";
    public final static String LN_sha384 = "sha384";
    public final static String OBJ_sha384 = OBJ_nist_hashalgs + ".2";

    public final static int    NID_sha512 = 674;
    public final static String SN_sha512 = "SHA512";
    public final static String LN_sha512 = "sha512";
    public final static String OBJ_sha512 = OBJ_nist_hashalgs + ".3";

    public final static int    NID_sha224 = 675;
    public final static String SN_sha224 = "SHA224";
    public final static String LN_sha224 = "sha224";
    public final static String OBJ_sha224 = OBJ_nist_hashalgs + ".4";

    public final static int NID_dsa_with_SHA224 = 802;
    public final static String SN_dsa_with_SHA224 = "dsa_with_SHA224";
    public final static String OBJ_dsa_with_SHA224 = OBJ_dsa_with_sha2 + ".1";

    public final static String SN_dsa_with_SHA256 = "dsa_with_SHA256";
    public final static int NID_dsa_with_SHA256 = 803;
    public final static String OBJ_dsa_with_SHA256 = OBJ_dsa_with_sha2 + ".2";

    public final static int    NID_X9_62_id_characteristic_two_basis = 680;
    public final static String SN_X9_62_id_characteristic_two_basis = "id-characteristic-two-basis";
    public final static String OBJ_X9_62_id_characteristic_two_basis = OBJ_X9_62_characteristic_two_field + ".3";

    public final static int    NID_X9_62_onBasis = 681;
    public final static String SN_X9_62_onBasis = "onBasis";
    public final static String OBJ_X9_62_onBasis = OBJ_X9_62_id_characteristic_two_basis + ".1";

    public final static int    NID_X9_62_tpBasis = 682;
    public final static String SN_X9_62_tpBasis = "tpBasis";
    public final static String OBJ_X9_62_tpBasis = OBJ_X9_62_id_characteristic_two_basis + ".2";

    public final static int    NID_X9_62_ppBasis = 683;
    public final static String SN_X9_62_ppBasis = "ppBasis";
    public final static String OBJ_X9_62_ppBasis = OBJ_X9_62_id_characteristic_two_basis + ".3";

    public final static int    NID_X9_62_c2pnb163v1 = 684;
    public final static String SN_X9_62_c2pnb163v1 = "c2pnb163v1";
    public final static String OBJ_X9_62_c2pnb163v1 = OBJ_X9_62_c_TwoCurve + ".1";

    public final static int    NID_X9_62_c2pnb163v2 = 685;
    public final static String SN_X9_62_c2pnb163v2 = "c2pnb163v2";
    public final static String OBJ_X9_62_c2pnb163v2 = OBJ_X9_62_c_TwoCurve + ".2";

    public final static int    NID_X9_62_c2pnb163v3 = 686;
    public final static String SN_X9_62_c2pnb163v3 = "c2pnb163v3";
    public final static String OBJ_X9_62_c2pnb163v3 = OBJ_X9_62_c_TwoCurve + ".3";

    public final static int    NID_X9_62_c2pnb176v1 = 687;
    public final static String SN_X9_62_c2pnb176v1 = "c2pnb176v1";
    public final static String OBJ_X9_62_c2pnb176v1 = OBJ_X9_62_c_TwoCurve + ".4";

    public final static int    NID_X9_62_c2tnb191v1 = 688;
    public final static String SN_X9_62_c2tnb191v1 = "c2tnb191v1";
    public final static String OBJ_X9_62_c2tnb191v1 = OBJ_X9_62_c_TwoCurve + ".5";

    public final static int    NID_X9_62_c2tnb191v2 = 689;
    public final static String SN_X9_62_c2tnb191v2 = "c2tnb191v2";
    public final static String OBJ_X9_62_c2tnb191v2 = OBJ_X9_62_c_TwoCurve + ".6";

    public final static int    NID_X9_62_c2tnb191v3 = 690;
    public final static String SN_X9_62_c2tnb191v3 = "c2tnb191v3";
    public final static String OBJ_X9_62_c2tnb191v3 = OBJ_X9_62_c_TwoCurve + ".7";

    public final static int    NID_X9_62_c2onb191v4 = 691;
    public final static String SN_X9_62_c2onb191v4 = "c2onb191v4";
    public final static String OBJ_X9_62_c2onb191v4 = OBJ_X9_62_c_TwoCurve + ".8";

    public final static int    NID_X9_62_c2onb191v5 = 692;
    public final static String SN_X9_62_c2onb191v5 = "c2onb191v5";
    public final static String OBJ_X9_62_c2onb191v5 = OBJ_X9_62_c_TwoCurve + ".9";

    public final static int    NID_X9_62_c2pnb208w1 = 693;
    public final static String SN_X9_62_c2pnb208w1 = "c2pnb208w1";
    public final static String OBJ_X9_62_c2pnb208w1 = OBJ_X9_62_c_TwoCurve + ".10";

    public final static int    NID_X9_62_c2tnb239v1 = 694;
    public final static String SN_X9_62_c2tnb239v1 = "c2tnb239v1";
    public final static String OBJ_X9_62_c2tnb239v1 = OBJ_X9_62_c_TwoCurve + ".11";

    public final static int    NID_X9_62_c2tnb239v2 = 695;
    public final static String SN_X9_62_c2tnb239v2 = "c2tnb239v2";
    public final static String OBJ_X9_62_c2tnb239v2 = OBJ_X9_62_c_TwoCurve + ".12";

    public final static int    NID_X9_62_c2tnb239v3 = 696;
    public final static String SN_X9_62_c2tnb239v3 = "c2tnb239v3";
    public final static String OBJ_X9_62_c2tnb239v3 = OBJ_X9_62_c_TwoCurve + ".13";

    public final static int    NID_X9_62_c2onb239v4 = 697;
    public final static String SN_X9_62_c2onb239v4 = "c2onb239v4";
    public final static String OBJ_X9_62_c2onb239v4 = OBJ_X9_62_c_TwoCurve + ".14";

    public final static int    NID_X9_62_c2onb239v5 = 698;
    public final static String SN_X9_62_c2onb239v5 = "c2onb239v5";
    public final static String OBJ_X9_62_c2onb239v5 = OBJ_X9_62_c_TwoCurve + ".15";

    public final static int    NID_X9_62_c2pnb272w1 = 699;
    public final static String SN_X9_62_c2pnb272w1 = "c2pnb272w1";
    public final static String OBJ_X9_62_c2pnb272w1 = OBJ_X9_62_c_TwoCurve + ".16";

    public final static int    NID_X9_62_c2pnb304w1 = 700;
    public final static String SN_X9_62_c2pnb304w1 = "c2pnb304w1";
    public final static String OBJ_X9_62_c2pnb304w1 = OBJ_X9_62_c_TwoCurve + ".17";

    public final static int    NID_X9_62_c2tnb359v1 = 701;
    public final static String SN_X9_62_c2tnb359v1 = "c2tnb359v1";
    public final static String OBJ_X9_62_c2tnb359v1 = OBJ_X9_62_c_TwoCurve + ".18";

    public final static int    NID_X9_62_c2pnb368w1 = 702;
    public final static String SN_X9_62_c2pnb368w1 = "c2pnb368w1";
    public final static String OBJ_X9_62_c2pnb368w1 = OBJ_X9_62_c_TwoCurve + ".19";

    public final static int    NID_X9_62_c2tnb431r1 = 703;
    public final static String SN_X9_62_c2tnb431r1 = "c2tnb431r1";
    public final static String OBJ_X9_62_c2tnb431r1 = OBJ_X9_62_c_TwoCurve + ".20";

    public final static int    NID_secp112r1 = 704;
    public final static String SN_secp112r1 = "secp112r1";
    public final static String OBJ_secp112r1 = OBJ_secg_ellipticCurve + ".6";

    public final static int    NID_secp112r2 = 705;
    public final static String SN_secp112r2 = "secp112r2";
    public final static String OBJ_secp112r2 = OBJ_secg_ellipticCurve + ".7";

    public final static int    NID_secp128r1 = 706;
    public final static String SN_secp128r1 = "secp128r1";
    public final static String OBJ_secp128r1 = OBJ_secg_ellipticCurve + ".28";

    public final static int    NID_secp128r2 = 707;
    public final static String SN_secp128r2 = "secp128r2";
    public final static String OBJ_secp128r2 = OBJ_secg_ellipticCurve + ".29";

    public final static int    NID_secp160k1 = 708;
    public final static String SN_secp160k1 = "secp160k1";
    public final static String OBJ_secp160k1 = OBJ_secg_ellipticCurve + ".9";

    public final static int    NID_secp160r1 = 709;
    public final static String SN_secp160r1 = "secp160r1";
    public final static String OBJ_secp160r1 = OBJ_secg_ellipticCurve + ".8";

    public final static int    NID_secp160r2 = 710;
    public final static String SN_secp160r2 = "secp160r2";
    public final static String OBJ_secp160r2 = OBJ_secg_ellipticCurve + ".30";

    public final static int    NID_secp192k1 = 711;
    public final static String SN_secp192k1 = "secp192k1";
    public final static String OBJ_secp192k1 = OBJ_secg_ellipticCurve + ".31";

    public final static int    NID_secp224k1 = 712;
    public final static String SN_secp224k1 = "secp224k1";
    public final static String OBJ_secp224k1 = OBJ_secg_ellipticCurve + ".32";

    public final static int    NID_secp224r1 = 713;
    public final static String SN_secp224r1 = "secp224r1";
    public final static String OBJ_secp224r1 = OBJ_secg_ellipticCurve + ".33";

    public final static int    NID_secp256k1 = 714;
    public final static String SN_secp256k1 = "secp256k1";
    public final static String OBJ_secp256k1 = OBJ_secg_ellipticCurve + ".10";

    public final static int    NID_secp384r1 = 715;
    public final static String SN_secp384r1 = "secp384r1";
    public final static String OBJ_secp384r1 = OBJ_secg_ellipticCurve + ".34";

    public final static int    NID_secp521r1 = 716;
    public final static String SN_secp521r1 = "secp521r1";
    public final static String OBJ_secp521r1 = OBJ_secg_ellipticCurve + ".35";

    public final static int    NID_sect113r1 = 717;
    public final static String SN_sect113r1 = "sect113r1";
    public final static String OBJ_sect113r1 = OBJ_secg_ellipticCurve + ".4";

    public final static int    NID_sect113r2 = 718;
    public final static String SN_sect113r2 = "sect113r2";
    public final static String OBJ_sect113r2 = OBJ_secg_ellipticCurve + ".5";

    public final static int    NID_sect131r1 = 719;
    public final static String SN_sect131r1 = "sect131r1";
    public final static String OBJ_sect131r1 = OBJ_secg_ellipticCurve + ".22";

    public final static int    NID_sect131r2 = 720;
    public final static String SN_sect131r2 = "sect131r2";
    public final static String OBJ_sect131r2 = OBJ_secg_ellipticCurve + ".23";

    public final static int    NID_sect163k1 = 721;
    public final static String SN_sect163k1 = "sect163k1";
    public final static String OBJ_sect163k1 = OBJ_secg_ellipticCurve + ".1";

    public final static int    NID_sect163r1 = 722;
    public final static String SN_sect163r1 = "sect163r1";
    public final static String OBJ_sect163r1 = OBJ_secg_ellipticCurve + ".2";

    public final static int    NID_sect163r2 = 723;
    public final static String SN_sect163r2 = "sect163r2";
    public final static String OBJ_sect163r2 = OBJ_secg_ellipticCurve + ".15";

    public final static int    NID_sect193r1 = 724;
    public final static String SN_sect193r1 = "sect193r1";
    public final static String OBJ_sect193r1 = OBJ_secg_ellipticCurve + ".24";

    public final static int    NID_sect193r2 = 725;
    public final static String SN_sect193r2 = "sect193r2";
    public final static String OBJ_sect193r2 = OBJ_secg_ellipticCurve + ".25";

    public final static int    NID_sect233k1 = 726;
    public final static String SN_sect233k1 = "sect233k1";
    public final static String OBJ_sect233k1 = OBJ_secg_ellipticCurve + ".26";

    public final static int    NID_sect233r1 = 727;
    public final static String SN_sect233r1 = "sect233r1";
    public final static String OBJ_sect233r1 = OBJ_secg_ellipticCurve + ".27";

    public final static int    NID_sect239k1 = 728;
    public final static String SN_sect239k1 = "sect239k1";
    public final static String OBJ_sect239k1 = OBJ_secg_ellipticCurve + ".3";

    public final static int    NID_sect283k1 = 729;
    public final static String SN_sect283k1 = "sect283k1";
    public final static String OBJ_sect283k1 = OBJ_secg_ellipticCurve + ".16";

    public final static int    NID_sect283r1 = 730;
    public final static String SN_sect283r1 = "sect283r1";
    public final static String OBJ_sect283r1 = OBJ_secg_ellipticCurve + ".17";

    public final static int    NID_sect409k1 = 731;
    public final static String SN_sect409k1 = "sect409k1";
    public final static String OBJ_sect409k1 = OBJ_secg_ellipticCurve + ".36";

    public final static int    NID_sect409r1 = 732;
    public final static String SN_sect409r1 = "sect409r1";
    public final static String OBJ_sect409r1 = OBJ_secg_ellipticCurve + ".37";

    public final static int    NID_sect571k1 = 733;
    public final static String SN_sect571k1 = "sect571k1";
    public final static String OBJ_sect571k1 = OBJ_secg_ellipticCurve + ".38";

    public final static int    NID_sect571r1 = 734;
    public final static String SN_sect571r1 = "sect571r1";
    public final static String OBJ_sect571r1 = OBJ_secg_ellipticCurve + ".39";

    public final static int    NID_wap_wsg_idm_ecid_wtls1 = 735;
    public final static String SN_wap_wsg_idm_ecid_wtls1 = "wap-wsg-idm-ecid-wtls1";
    public final static String OBJ_wap_wsg_idm_ecid_wtls1 = OBJ_wap_wsg_idm_ecid + ".1";

    public final static int    NID_wap_wsg_idm_ecid_wtls3 = 736;
    public final static String SN_wap_wsg_idm_ecid_wtls3 = "wap-wsg-idm-ecid-wtls3";
    public final static String OBJ_wap_wsg_idm_ecid_wtls3 = OBJ_wap_wsg_idm_ecid + ".3";

    public final static int    NID_wap_wsg_idm_ecid_wtls4 = 737;
    public final static String SN_wap_wsg_idm_ecid_wtls4 = "wap-wsg-idm-ecid-wtls4";
    public final static String OBJ_wap_wsg_idm_ecid_wtls4 = OBJ_wap_wsg_idm_ecid + ".4";

    public final static int    NID_wap_wsg_idm_ecid_wtls5 = 738;
    public final static String SN_wap_wsg_idm_ecid_wtls5 = "wap-wsg-idm-ecid-wtls5";
    public final static String OBJ_wap_wsg_idm_ecid_wtls5 = OBJ_wap_wsg_idm_ecid + ".5";

    public final static int    NID_wap_wsg_idm_ecid_wtls6 = 739;
    public final static String SN_wap_wsg_idm_ecid_wtls6 = "wap-wsg-idm-ecid-wtls6";
    public final static String OBJ_wap_wsg_idm_ecid_wtls6 = OBJ_wap_wsg_idm_ecid + ".6";

    public final static int    NID_wap_wsg_idm_ecid_wtls7 = 740;
    public final static String SN_wap_wsg_idm_ecid_wtls7 = "wap-wsg-idm-ecid-wtls7";
    public final static String OBJ_wap_wsg_idm_ecid_wtls7 = OBJ_wap_wsg_idm_ecid + ".7";

    public final static int    NID_wap_wsg_idm_ecid_wtls8 = 741;
    public final static String SN_wap_wsg_idm_ecid_wtls8 = "wap-wsg-idm-ecid-wtls8";
    public final static String OBJ_wap_wsg_idm_ecid_wtls8 = OBJ_wap_wsg_idm_ecid + ".8";

    public final static int    NID_wap_wsg_idm_ecid_wtls9 = 742;
    public final static String SN_wap_wsg_idm_ecid_wtls9 = "wap-wsg-idm-ecid-wtls9";
    public final static String OBJ_wap_wsg_idm_ecid_wtls9 = OBJ_wap_wsg_idm_ecid + ".9";

    public final static int    NID_wap_wsg_idm_ecid_wtls10 = 743;
    public final static String SN_wap_wsg_idm_ecid_wtls10 = "wap-wsg-idm-ecid-wtls10";
    public final static String OBJ_wap_wsg_idm_ecid_wtls10 = OBJ_wap_wsg_idm_ecid + ".10";

    public final static int    NID_wap_wsg_idm_ecid_wtls11 = 744;
    public final static String SN_wap_wsg_idm_ecid_wtls11 = "wap-wsg-idm-ecid-wtls11";
    public final static String OBJ_wap_wsg_idm_ecid_wtls11 = OBJ_wap_wsg_idm_ecid + ".11";

    public final static int    NID_wap_wsg_idm_ecid_wtls12 = 745;
    public final static String SN_wap_wsg_idm_ecid_wtls12 = "wap-wsg-idm-ecid-wtls12";
    public final static String OBJ_wap_wsg_idm_ecid_wtls12 = OBJ_wap_wsg_idm_ecid + ".12";

    public final static int    NID_any_policy = 746;
    public final static String SN_any_policy = "anyPolicy";
    public final static String LN_any_policy = "X509v3 Any Policy";
    public final static String OBJ_any_policy = OBJ_certificate_policies + ".0";

    public final static int    NID_policy_mappings = 747;
    public final static String SN_policy_mappings = "policyMappings";
    public final static String LN_policy_mappings = "X509v3 Policy Mappings";
    public final static String OBJ_policy_mappings = OBJ_id_ce + ".33";

    public final static int    NID_inhibit_any_policy = 748;
    public final static String SN_inhibit_any_policy = "inhibitAnyPolicy";
    public final static String LN_inhibit_any_policy = "X509v3 Inhibit Any Policy";
    public final static String OBJ_inhibit_any_policy = OBJ_id_ce + ".54";

    public final static int    NID_ipsec3 = 749;
    public final static String SN_ipsec3 = "Oakley-EC2N-3";
    public final static String LN_ipsec3 = "ipsec3";

    public final static int    NID_ipsec4 = 750;
    public final static String SN_ipsec4 = "Oakley-EC2N-4";
    public final static String LN_ipsec4 = "ipsec4";

    static { initObjects(); }

    private static void initObjects() {
        //addObject(NID_undef, SN_undef, LN_undef, OBJ_undef); // NID: 0
        addObject(NID_rsadsi, SN_rsadsi, LN_rsadsi, OBJ_rsadsi); // NID: 1
        addObject(NID_pkcs, SN_pkcs, LN_pkcs, OBJ_pkcs); // NID: 2
        addObject(NID_md2, SN_md2, LN_md2, OBJ_md2); // NID: 3
        addObject(NID_md5, SN_md5, LN_md5, OBJ_md5); // NID: 4
        addObject(NID_rc4, SN_rc4, LN_rc4, OBJ_rc4); // NID: 5
        addObject(NID_rsaEncryption, null, LN_rsaEncryption, OID_rsaEncryption); // NID: 6
        addObject(NID_md2WithRSAEncryption, SN_md2WithRSAEncryption, LN_md2WithRSAEncryption, OBJ_md2WithRSAEncryption); // NID: 7
        addObject(NID_md5WithRSAEncryption, SN_md5WithRSAEncryption, LN_md5WithRSAEncryption, OBJ_md5WithRSAEncryption); // NID: 8
        addObject(NID_pbeWithMD2AndDES_CBC, SN_pbeWithMD2AndDES_CBC, LN_pbeWithMD2AndDES_CBC, OBJ_pbeWithMD2AndDES_CBC); // NID: 9
        addObject(NID_pbeWithMD5AndDES_CBC, SN_pbeWithMD5AndDES_CBC, LN_pbeWithMD5AndDES_CBC, OBJ_pbeWithMD5AndDES_CBC); // NID: 10
        addObject(NID_X500, SN_X500, LN_X500, OBJ_X500); // NID: 11
        addObject(NID_X509, SN_X509, null, OBJ_X509); // NID: 12
        addObject(NID_commonName, SN_commonName, LN_commonName, OBJ_commonName); // NID: 13
        addObject(NID_countryName, SN_countryName, LN_countryName, OBJ_countryName); // NID: 14
        addObject(NID_localityName, SN_localityName, LN_localityName, OBJ_localityName); // NID: 15
        addObject(NID_stateOrProvinceName, SN_stateOrProvinceName, LN_stateOrProvinceName, OBJ_stateOrProvinceName); // NID: 16
        addObject(NID_organizationName, SN_organizationName, LN_organizationName, OBJ_organizationName); // NID: 17
        addObject(NID_organizationalUnitName, SN_organizationalUnitName, LN_organizationalUnitName, OBJ_organizationalUnitName); // NID: 18
        addObject(NID_rsa, SN_rsa, LN_rsa, OBJ_rsa); // NID: 19
        addObject(NID_pkcs7, SN_pkcs7, null, OBJ_pkcs7); // NID: 20
        addObject(NID_pkcs7_data, null, LN_pkcs7_data, OID_pkcs7_data); // NID: 21
        addObject(NID_pkcs7_signed, null, LN_pkcs7_signed, OBJ_pkcs7_signed); // NID: 22
        addObject(NID_pkcs7_enveloped, null, LN_pkcs7_enveloped, OBJ_pkcs7_enveloped); // NID: 23
        addObject(NID_pkcs7_signedAndEnveloped, null, LN_pkcs7_signedAndEnveloped, OBJ_pkcs7_signedAndEnveloped); // NID: 24
        addObject(NID_pkcs7_digest, null, LN_pkcs7_digest, OBJ_pkcs7_digest); // NID: 25
        addObject(NID_pkcs7_encrypted, null, LN_pkcs7_encrypted, OBJ_pkcs7_encrypted); // NID: 26
        addObject(NID_pkcs3, SN_pkcs3, null, OBJ_pkcs3); // NID: 27
        addObject(NID_dhKeyAgreement, null, LN_dhKeyAgreement, OBJ_dhKeyAgreement); // NID: 28
        addObject(NID_des_ecb, SN_des_ecb, LN_des_ecb, OBJ_des_ecb); // NID: 29
        addObject(NID_des_cfb64, SN_des_cfb64, LN_des_cfb64, OBJ_des_cfb64); // NID: 30
        addObject(NID_des_cbc, SN_des_cbc, LN_des_cbc, OID_des_cbc); // NID: 31
        addObject(NID_des_ede_ecb, SN_des_ede_ecb, LN_des_ede_ecb, OBJ_des_ede_ecb); // NID: 32
        //addObject(NID_des_ede3_ecb, SN_des_ede3_ecb, LN_des_ede3_ecb, null); // NID: 33
        addObject(NID_idea_cbc, SN_idea_cbc, LN_idea_cbc, OBJ_idea_cbc); // NID: 34
        //addObject(NID_idea_cfb64, SN_idea_cfb64, LN_idea_cfb64, null); // NID: 35
        //addObject(NID_idea_ecb, SN_idea_ecb, LN_idea_ecb, null); // NID: 36
        addObject(NID_rc2_cbc, SN_rc2_cbc, LN_rc2_cbc, OID_rc2_cbc); // NID: 37
        //addObject(NID_rc2_ecb, SN_rc2_ecb, LN_rc2_ecb, null); // NID: 38
        //addObject(NID_rc2_cfb64, SN_rc2_cfb64, LN_rc2_cfb64, null); // NID: 39
        //addObject(NID_rc2_ofb64, SN_rc2_ofb64, LN_rc2_ofb64, null); // NID: 40
        addObject(NID_sha, SN_sha, LN_sha, OBJ_sha); // NID: 41
        addObject(NID_shaWithRSAEncryption, SN_shaWithRSAEncryption, LN_shaWithRSAEncryption, OBJ_shaWithRSAEncryption); // NID: 42
        //addObject(NID_des_ede_cbc, SN_des_ede_cbc, LN_des_ede_cbc, null); // NID: 43
        addObject(NID_des_ede3_cbc, SN_des_ede3_cbc, LN_des_ede3_cbc, OID_des_ede3_cbc); // NID: 44
        addObject(NID_des_ofb64, SN_des_ofb64, LN_des_ofb64, OBJ_des_ofb64); // NID: 45
        //addObject(NID_idea_ofb64, SN_idea_ofb64, LN_idea_ofb64, null); // NID: 46
        addObject(NID_pkcs9, SN_pkcs9, null, OBJ_pkcs9); // NID: 47
        addObject(NID_pkcs9_emailAddress, null, LN_pkcs9_emailAddress, OBJ_pkcs9_emailAddress); // NID: 48
        addObject(NID_pkcs9_unstructuredName, null, LN_pkcs9_unstructuredName, OBJ_pkcs9_unstructuredName); // NID: 49
        addObject(NID_pkcs9_contentType, null, LN_pkcs9_contentType, OBJ_pkcs9_contentType); // NID: 50
        addObject(NID_pkcs9_messageDigest, null, LN_pkcs9_messageDigest, OBJ_pkcs9_messageDigest); // NID: 51
        addObject(NID_pkcs9_signingTime, null, LN_pkcs9_signingTime, OBJ_pkcs9_signingTime); // NID: 52
        addObject(NID_pkcs9_countersignature, null, LN_pkcs9_countersignature, OBJ_pkcs9_countersignature); // NID: 53
        addObject(NID_pkcs9_challengePassword, null, LN_pkcs9_challengePassword, OBJ_pkcs9_challengePassword); // NID: 54
        addObject(NID_pkcs9_unstructuredAddress, null, LN_pkcs9_unstructuredAddress, OBJ_pkcs9_unstructuredAddress); // NID: 55
        addObject(NID_pkcs9_extCertAttributes, null, LN_pkcs9_extCertAttributes, OBJ_pkcs9_extCertAttributes); // NID: 56
        addObject(NID_netscape, SN_netscape, LN_netscape, OBJ_netscape); // NID: 57
        addObject(NID_netscape_cert_extension, SN_netscape_cert_extension, LN_netscape_cert_extension, OBJ_netscape_cert_extension); // NID: 58
        addObject(NID_netscape_data_type, SN_netscape_data_type, LN_netscape_data_type, OBJ_netscape_data_type); // NID: 59
        //addObject(NID_des_ede_cfb64, SN_des_ede_cfb64, LN_des_ede_cfb64, null); // NID: 60
        //addObject(NID_des_ede3_cfb64, SN_des_ede3_cfb64, LN_des_ede3_cfb64, null); // NID: 61
        //addObject(NID_des_ede_ofb64, SN_des_ede_ofb64, LN_des_ede_ofb64, null); // NID: 62
        //addObject(NID_des_ede3_ofb64, SN_des_ede3_ofb64, LN_des_ede3_ofb64, null); // NID: 63
        addObject(NID_sha1, SN_sha1, LN_sha1, OID_sha1); // NID: 64
        addObject(NID_sha1WithRSAEncryption, SN_sha1WithRSAEncryption, LN_sha1WithRSAEncryption, OBJ_sha1WithRSAEncryption); // NID: 65
        addObject(NID_dsaWithSHA, SN_dsaWithSHA, LN_dsaWithSHA, OBJ_dsaWithSHA); // NID: 66
        addObject(NID_dsa_2, SN_dsa_2, LN_dsa_2, OBJ_dsa_2); // NID: 67
        addObject(NID_pbeWithSHA1AndRC2_CBC, SN_pbeWithSHA1AndRC2_CBC, LN_pbeWithSHA1AndRC2_CBC, OBJ_pbeWithSHA1AndRC2_CBC); // NID: 68
        addObject(NID_id_pbkdf2, null, LN_id_pbkdf2, OBJ_id_pbkdf2); // NID: 69
        addObject(NID_dsaWithSHA1_2, SN_dsaWithSHA1_2, LN_dsaWithSHA1_2, OBJ_dsaWithSHA1_2); // NID: 70
        addObject(NID_netscape_cert_type, SN_netscape_cert_type, LN_netscape_cert_type, OBJ_netscape_cert_type); // NID: 71
        addObject(NID_netscape_base_url, SN_netscape_base_url, LN_netscape_base_url, OBJ_netscape_base_url); // NID: 72
        addObject(NID_netscape_revocation_url, SN_netscape_revocation_url, LN_netscape_revocation_url, OBJ_netscape_revocation_url); // NID: 73
        addObject(NID_netscape_ca_revocation_url, SN_netscape_ca_revocation_url, LN_netscape_ca_revocation_url, OBJ_netscape_ca_revocation_url); // NID: 74
        addObject(NID_netscape_renewal_url, SN_netscape_renewal_url, LN_netscape_renewal_url, OBJ_netscape_renewal_url); // NID: 75
        addObject(NID_netscape_ca_policy_url, SN_netscape_ca_policy_url, LN_netscape_ca_policy_url, OBJ_netscape_ca_policy_url); // NID: 76
        addObject(NID_netscape_ssl_server_name, SN_netscape_ssl_server_name, LN_netscape_ssl_server_name, OBJ_netscape_ssl_server_name); // NID: 77
        addObject(NID_netscape_comment, SN_netscape_comment, LN_netscape_comment, OBJ_netscape_comment); // NID: 78
        addObject(NID_netscape_cert_sequence, SN_netscape_cert_sequence, LN_netscape_cert_sequence, OBJ_netscape_cert_sequence); // NID: 79
        //addObject(NID_desx_cbc, SN_desx_cbc, LN_desx_cbc, null); // NID: 80
        addObject(NID_id_ce, SN_id_ce, null, OBJ_id_ce); // NID: 81
        addObject(NID_subject_key_identifier, SN_subject_key_identifier, LN_subject_key_identifier, OBJ_subject_key_identifier); // NID: 82
        addObject(NID_key_usage, SN_key_usage, LN_key_usage, OBJ_key_usage); // NID: 83
        addObject(NID_private_key_usage_period, SN_private_key_usage_period, LN_private_key_usage_period, OBJ_private_key_usage_period); // NID: 84
        addObject(NID_subject_alt_name, SN_subject_alt_name, LN_subject_alt_name, OBJ_subject_alt_name); // NID: 85
        addObject(NID_issuer_alt_name, SN_issuer_alt_name, LN_issuer_alt_name, OBJ_issuer_alt_name); // NID: 86
        addObject(NID_basic_constraints, SN_basic_constraints, LN_basic_constraints, OBJ_basic_constraints); // NID: 87
        addObject(NID_crl_number, SN_crl_number, LN_crl_number, OBJ_crl_number); // NID: 88
        addObject(NID_certificate_policies, SN_certificate_policies, LN_certificate_policies, OBJ_certificate_policies); // NID: 89
        addObject(NID_authority_key_identifier, SN_authority_key_identifier, LN_authority_key_identifier, OBJ_authority_key_identifier); // NID: 90
        addObject(NID_bf_cbc, SN_bf_cbc, LN_bf_cbc, OBJ_bf_cbc); // NID: 91
        //addObject(NID_bf_ecb, SN_bf_ecb, LN_bf_ecb, null); // NID: 92
        //addObject(NID_bf_cfb64, SN_bf_cfb64, LN_bf_cfb64, null); // NID: 93
        //addObject(NID_bf_ofb64, SN_bf_ofb64, LN_bf_ofb64, null); // NID: 94
        addObject(NID_mdc2, SN_mdc2, LN_mdc2, OBJ_mdc2); // NID: 95
        addObject(NID_mdc2WithRSA, SN_mdc2WithRSA, LN_mdc2WithRSA, OBJ_mdc2WithRSA); // NID: 96
        //addObject(NID_rc4_40, SN_rc4_40, LN_rc4_40, null); // NID: 97
        addObject(NID_rc2_40_cbc, SN_rc2_40_cbc, LN_rc2_40_cbc, OBJ_rc2_40_cbc); // NID: 98
        addObject(NID_givenName, SN_givenName, LN_givenName, OBJ_givenName); // NID: 99
        addObject(NID_surname, SN_surname, LN_surname, OBJ_surname); // NID: 100
        addObject(NID_initials, null, LN_initials, OBJ_initials); // NID: 101
        addObject(NID_crl_distribution_points, SN_crl_distribution_points, LN_crl_distribution_points, OBJ_crl_distribution_points); // NID: 103
        addObject(NID_md5WithRSA, SN_md5WithRSA, LN_md5WithRSA, OBJ_md5WithRSA); // NID: 104
        addObject(NID_serialNumber, null, LN_serialNumber, OBJ_serialNumber); // NID: 105
        addObject(NID_title, null, LN_title, OBJ_title); // NID: 106
        addObject(NID_description, null, LN_description, OBJ_description); // NID: 107
        addObject(NID_cast5_cbc, SN_cast5_cbc, LN_cast5_cbc, OBJ_cast5_cbc); // NID: 108
        //addObject(NID_cast5_ecb, SN_cast5_ecb, LN_cast5_ecb, null); // NID: 109
        //addObject(NID_cast5_cfb64, SN_cast5_cfb64, LN_cast5_cfb64, null); // NID: 110
        //addObject(NID_cast5_ofb64, SN_cast5_ofb64, LN_cast5_ofb64, null); // NID: 111
        addObject(NID_pbeWithMD5AndCast5_CBC, null, LN_pbeWithMD5AndCast5_CBC, OBJ_pbeWithMD5AndCast5_CBC); // NID: 112
        addObject(NID_dsaWithSHA1, SN_dsaWithSHA1, LN_dsaWithSHA1, OBJ_dsaWithSHA1); // NID: 113
        //addObject(NID_md5_sha1, SN_md5_sha1, LN_md5_sha1, null); // NID: 114
        addObject(NID_sha1WithRSA, SN_sha1WithRSA, LN_sha1WithRSA, OBJ_sha1WithRSA); // NID: 115
        addObject(NID_dsa, SN_dsa, LN_dsa, OID_dsa); // NID: 116
        addObject(NID_ripemd160, SN_ripemd160, LN_ripemd160, OBJ_ripemd160); // NID: 117
        addObject(NID_ripemd160WithRSA, SN_ripemd160WithRSA, LN_ripemd160WithRSA, OBJ_ripemd160WithRSA); // NID: 119
        addObject(NID_rc5_cbc, SN_rc5_cbc, LN_rc5_cbc, OBJ_rc5_cbc); // NID: 120
        //addObject(NID_rc5_ecb, SN_rc5_ecb, LN_rc5_ecb, null); // NID: 121
        //addObject(NID_rc5_cfb64, SN_rc5_cfb64, LN_rc5_cfb64, null); // NID: 122
        //addObject(NID_rc5_ofb64, SN_rc5_ofb64, LN_rc5_ofb64, null); // NID: 123
        addObject(NID_rle_compression, SN_rle_compression, LN_rle_compression, OBJ_rle_compression); // NID: 124
        addObject(NID_zlib_compression, SN_zlib_compression, LN_zlib_compression, OBJ_zlib_compression); // NID: 125
        addObject(NID_ext_key_usage, SN_ext_key_usage, LN_ext_key_usage, OBJ_ext_key_usage); // NID: 126
        addObject(NID_id_pkix, SN_id_pkix, null, OBJ_id_pkix); // NID: 127
        addObject(NID_id_kp, SN_id_kp, null, OBJ_id_kp); // NID: 128
        addObject(NID_server_auth, SN_server_auth, LN_server_auth, OBJ_server_auth); // NID: 129
        addObject(NID_client_auth, SN_client_auth, LN_client_auth, OBJ_client_auth); // NID: 130
        addObject(NID_code_sign, SN_code_sign, LN_code_sign, OBJ_code_sign); // NID: 131
        addObject(NID_email_protect, SN_email_protect, LN_email_protect, OBJ_email_protect); // NID: 132
        addObject(NID_time_stamp, SN_time_stamp, LN_time_stamp, OBJ_time_stamp); // NID: 133
        addObject(NID_ms_code_ind, SN_ms_code_ind, LN_ms_code_ind, OBJ_ms_code_ind); // NID: 134
        addObject(NID_ms_code_com, SN_ms_code_com, LN_ms_code_com, OBJ_ms_code_com); // NID: 135
        addObject(NID_ms_ctl_sign, SN_ms_ctl_sign, LN_ms_ctl_sign, OBJ_ms_ctl_sign); // NID: 136
        addObject(NID_ms_sgc, SN_ms_sgc, LN_ms_sgc, OBJ_ms_sgc); // NID: 137
        addObject(NID_ms_efs, SN_ms_efs, LN_ms_efs, OBJ_ms_efs); // NID: 138
        addObject(NID_ns_sgc, SN_ns_sgc, LN_ns_sgc, OBJ_ns_sgc); // NID: 139
        addObject(NID_delta_crl, SN_delta_crl, LN_delta_crl, OBJ_delta_crl); // NID: 140
        addObject(NID_crl_reason, SN_crl_reason, LN_crl_reason, OBJ_crl_reason); // NID: 141
        addObject(NID_invalidity_date, SN_invalidity_date, LN_invalidity_date, OBJ_invalidity_date); // NID: 142
        addObject(NID_sxnet, SN_sxnet, LN_sxnet, OBJ_sxnet); // NID: 143
        addObject(NID_pbe_WithSHA1And128BitRC4, SN_pbe_WithSHA1And128BitRC4, LN_pbe_WithSHA1And128BitRC4, OBJ_pbe_WithSHA1And128BitRC4); // NID: 144
        addObject(NID_pbe_WithSHA1And40BitRC4, SN_pbe_WithSHA1And40BitRC4, LN_pbe_WithSHA1And40BitRC4, OBJ_pbe_WithSHA1And40BitRC4); // NID: 145
        addObject(NID_pbe_WithSHA1And3_Key_TripleDES_CBC, SN_pbe_WithSHA1And3_Key_TripleDES_CBC, LN_pbe_WithSHA1And3_Key_TripleDES_CBC, OBJ_pbe_WithSHA1And3_Key_TripleDES_CBC); // NID: 146
        addObject(NID_pbe_WithSHA1And2_Key_TripleDES_CBC, SN_pbe_WithSHA1And2_Key_TripleDES_CBC, LN_pbe_WithSHA1And2_Key_TripleDES_CBC, OBJ_pbe_WithSHA1And2_Key_TripleDES_CBC); // NID: 147
        addObject(NID_pbe_WithSHA1And128BitRC2_CBC, SN_pbe_WithSHA1And128BitRC2_CBC, LN_pbe_WithSHA1And128BitRC2_CBC, OBJ_pbe_WithSHA1And128BitRC2_CBC); // NID: 148
        addObject(NID_pbe_WithSHA1And40BitRC2_CBC, SN_pbe_WithSHA1And40BitRC2_CBC, LN_pbe_WithSHA1And40BitRC2_CBC, OBJ_pbe_WithSHA1And40BitRC2_CBC); // NID: 149
        addObject(NID_keyBag, null, LN_keyBag, OBJ_keyBag); // NID: 150
        addObject(NID_pkcs8ShroudedKeyBag, null, LN_pkcs8ShroudedKeyBag, OBJ_pkcs8ShroudedKeyBag); // NID: 151
        addObject(NID_certBag, null, LN_certBag, OBJ_certBag); // NID: 152
        addObject(NID_crlBag, null, LN_crlBag, OBJ_crlBag); // NID: 153
        addObject(NID_secretBag, null, LN_secretBag, OBJ_secretBag); // NID: 154
        addObject(NID_safeContentsBag, null, LN_safeContentsBag, OBJ_safeContentsBag); // NID: 155
        addObject(NID_friendlyName, null, LN_friendlyName, OBJ_friendlyName); // NID: 156
        addObject(NID_localKeyID, null, LN_localKeyID, OBJ_localKeyID); // NID: 157
        addObject(NID_x509Certificate, null, LN_x509Certificate, OBJ_x509Certificate); // NID: 158
        addObject(NID_sdsiCertificate, null, LN_sdsiCertificate, OBJ_sdsiCertificate); // NID: 159
        addObject(NID_x509Crl, null, LN_x509Crl, OBJ_x509Crl); // NID: 160
        addObject(NID_pbes2, null, LN_pbes2, OBJ_pbes2); // NID: 161
        addObject(NID_pbmac1, null, LN_pbmac1, OBJ_pbmac1); // NID: 162
        addObject(NID_hmacWithSHA1, null, LN_hmacWithSHA1, OBJ_hmacWithSHA1); // NID: 163
        addObject(NID_id_qt_cps, SN_id_qt_cps, LN_id_qt_cps, OBJ_id_qt_cps); // NID: 164
        addObject(NID_id_qt_unotice, SN_id_qt_unotice, LN_id_qt_unotice, OBJ_id_qt_unotice); // NID: 165
        //addObject(NID_rc2_64_cbc, SN_rc2_64_cbc, LN_rc2_64_cbc, null); // NID: 166
        addObject(NID_SMIMECapabilities, SN_SMIMECapabilities, LN_SMIMECapabilities, OBJ_SMIMECapabilities); // NID: 167
        addObject(NID_pbeWithMD2AndRC2_CBC, SN_pbeWithMD2AndRC2_CBC, LN_pbeWithMD2AndRC2_CBC, OBJ_pbeWithMD2AndRC2_CBC); // NID: 168
        addObject(NID_pbeWithMD5AndRC2_CBC, SN_pbeWithMD5AndRC2_CBC, LN_pbeWithMD5AndRC2_CBC, OBJ_pbeWithMD5AndRC2_CBC); // NID: 169
        addObject(NID_pbeWithSHA1AndDES_CBC, SN_pbeWithSHA1AndDES_CBC, LN_pbeWithSHA1AndDES_CBC, OBJ_pbeWithSHA1AndDES_CBC); // NID: 170
        addObject(NID_ms_ext_req, SN_ms_ext_req, LN_ms_ext_req, OBJ_ms_ext_req); // NID: 171
        addObject(NID_ext_req, SN_ext_req, LN_ext_req, OBJ_ext_req); // NID: 172
        addObject(NID_name, SN_name, LN_name, OBJ_name); // NID: 173
        addObject(NID_dnQualifier, SN_dnQualifier, LN_dnQualifier, OBJ_dnQualifier); // NID: 174
        addObject(NID_id_pe, SN_id_pe, null, OBJ_id_pe); // NID: 175
        addObject(NID_id_ad, SN_id_ad, null, OBJ_id_ad); // NID: 176
        addObject(NID_info_access, SN_info_access, LN_info_access, OBJ_info_access); // NID: 177
        addObject(NID_ad_OCSP, SN_ad_OCSP, LN_ad_OCSP, OBJ_ad_OCSP); // NID: 178
        addObject(NID_ad_ca_issuers, SN_ad_ca_issuers, LN_ad_ca_issuers, OBJ_ad_ca_issuers); // NID: 179
        addObject(NID_OCSP_sign, SN_OCSP_sign, LN_OCSP_sign, OBJ_OCSP_sign); // NID: 180
        //addObject(NID_iso, SN_iso, LN_iso, OBJ_iso); // NID: 181
        addObject(NID_member_body, SN_member_body, LN_member_body, OBJ_member_body); // NID: 182
        addObject(NID_ISO_US, SN_ISO_US, LN_ISO_US, OBJ_ISO_US); // NID: 183
        addObject(NID_X9_57, SN_X9_57, LN_X9_57, OBJ_X9_57); // NID: 184
        addObject(NID_X9cm, SN_X9cm, LN_X9cm, OBJ_X9cm); // NID: 185
        addObject(NID_pkcs1, SN_pkcs1, null, OBJ_pkcs1); // NID: 186
        addObject(NID_pkcs5, SN_pkcs5, null, OBJ_pkcs5); // NID: 187
        addObject(NID_SMIME, SN_SMIME, LN_SMIME, OBJ_SMIME); // NID: 188
        addObject(NID_id_smime_mod, SN_id_smime_mod, null, OBJ_id_smime_mod); // NID: 189
        addObject(NID_id_smime_ct, SN_id_smime_ct, null, OBJ_id_smime_ct); // NID: 190
        addObject(NID_id_smime_aa, SN_id_smime_aa, null, OBJ_id_smime_aa); // NID: 191
        addObject(NID_id_smime_alg, SN_id_smime_alg, null, OBJ_id_smime_alg); // NID: 192
        addObject(NID_id_smime_cd, SN_id_smime_cd, null, OBJ_id_smime_cd); // NID: 193
        addObject(NID_id_smime_spq, SN_id_smime_spq, null, OBJ_id_smime_spq); // NID: 194
        addObject(NID_id_smime_cti, SN_id_smime_cti, null, OBJ_id_smime_cti); // NID: 195
        addObject(NID_id_smime_mod_cms, SN_id_smime_mod_cms, null, OBJ_id_smime_mod_cms); // NID: 196
        addObject(NID_id_smime_mod_ess, SN_id_smime_mod_ess, null, OBJ_id_smime_mod_ess); // NID: 197
        addObject(NID_id_smime_mod_oid, SN_id_smime_mod_oid, null, OBJ_id_smime_mod_oid); // NID: 198
        addObject(NID_id_smime_mod_msg_v3, SN_id_smime_mod_msg_v3, null, OBJ_id_smime_mod_msg_v3); // NID: 199
        addObject(NID_id_smime_mod_ets_eSignature_88, SN_id_smime_mod_ets_eSignature_88, null, OBJ_id_smime_mod_ets_eSignature_88); // NID: 200
        addObject(NID_id_smime_mod_ets_eSignature_97, SN_id_smime_mod_ets_eSignature_97, null, OBJ_id_smime_mod_ets_eSignature_97); // NID: 201
        addObject(NID_id_smime_mod_ets_eSigPolicy_88, SN_id_smime_mod_ets_eSigPolicy_88, null, OBJ_id_smime_mod_ets_eSigPolicy_88); // NID: 202
        addObject(NID_id_smime_mod_ets_eSigPolicy_97, SN_id_smime_mod_ets_eSigPolicy_97, null, OBJ_id_smime_mod_ets_eSigPolicy_97); // NID: 203
        addObject(NID_id_smime_ct_receipt, SN_id_smime_ct_receipt, null, OBJ_id_smime_ct_receipt); // NID: 204
        addObject(NID_id_smime_ct_authData, SN_id_smime_ct_authData, null, OBJ_id_smime_ct_authData); // NID: 205
        addObject(NID_id_smime_ct_publishCert, SN_id_smime_ct_publishCert, null, OBJ_id_smime_ct_publishCert); // NID: 206
        addObject(NID_id_smime_ct_TSTInfo, SN_id_smime_ct_TSTInfo, null, OBJ_id_smime_ct_TSTInfo); // NID: 207
        addObject(NID_id_smime_ct_TDTInfo, SN_id_smime_ct_TDTInfo, null, OBJ_id_smime_ct_TDTInfo); // NID: 208
        addObject(NID_id_smime_ct_contentInfo, SN_id_smime_ct_contentInfo, null, OBJ_id_smime_ct_contentInfo); // NID: 209
        addObject(NID_id_smime_ct_DVCSRequestData, SN_id_smime_ct_DVCSRequestData, null, OBJ_id_smime_ct_DVCSRequestData); // NID: 210
        addObject(NID_id_smime_ct_DVCSResponseData, SN_id_smime_ct_DVCSResponseData, null, OBJ_id_smime_ct_DVCSResponseData); // NID: 211
        addObject(NID_id_smime_aa_receiptRequest, SN_id_smime_aa_receiptRequest, null, OBJ_id_smime_aa_receiptRequest); // NID: 212
        addObject(NID_id_smime_aa_securityLabel, SN_id_smime_aa_securityLabel, null, OBJ_id_smime_aa_securityLabel); // NID: 213
        addObject(NID_id_smime_aa_mlExpandHistory, SN_id_smime_aa_mlExpandHistory, null, OBJ_id_smime_aa_mlExpandHistory); // NID: 214
        addObject(NID_id_smime_aa_contentHint, SN_id_smime_aa_contentHint, null, OBJ_id_smime_aa_contentHint); // NID: 215
        addObject(NID_id_smime_aa_msgSigDigest, SN_id_smime_aa_msgSigDigest, null, OBJ_id_smime_aa_msgSigDigest); // NID: 216
        addObject(NID_id_smime_aa_encapContentType, SN_id_smime_aa_encapContentType, null, OBJ_id_smime_aa_encapContentType); // NID: 217
        addObject(NID_id_smime_aa_contentIdentifier, SN_id_smime_aa_contentIdentifier, null, OBJ_id_smime_aa_contentIdentifier); // NID: 218
        addObject(NID_id_smime_aa_macValue, SN_id_smime_aa_macValue, null, OBJ_id_smime_aa_macValue); // NID: 219
        addObject(NID_id_smime_aa_equivalentLabels, SN_id_smime_aa_equivalentLabels, null, OBJ_id_smime_aa_equivalentLabels); // NID: 220
        addObject(NID_id_smime_aa_contentReference, SN_id_smime_aa_contentReference, null, OBJ_id_smime_aa_contentReference); // NID: 221
        addObject(NID_id_smime_aa_encrypKeyPref, SN_id_smime_aa_encrypKeyPref, null, OBJ_id_smime_aa_encrypKeyPref); // NID: 222
        addObject(NID_id_smime_aa_signingCertificate, SN_id_smime_aa_signingCertificate, null, OBJ_id_smime_aa_signingCertificate); // NID: 223
        addObject(NID_id_smime_aa_smimeEncryptCerts, SN_id_smime_aa_smimeEncryptCerts, null, OBJ_id_smime_aa_smimeEncryptCerts); // NID: 224
        addObject(NID_id_smime_aa_timeStampToken, SN_id_smime_aa_timeStampToken, null, OBJ_id_smime_aa_timeStampToken); // NID: 225
        addObject(NID_id_smime_aa_ets_sigPolicyId, SN_id_smime_aa_ets_sigPolicyId, null, OBJ_id_smime_aa_ets_sigPolicyId); // NID: 226
        addObject(NID_id_smime_aa_ets_commitmentType, SN_id_smime_aa_ets_commitmentType, null, OBJ_id_smime_aa_ets_commitmentType); // NID: 227
        addObject(NID_id_smime_aa_ets_signerLocation, SN_id_smime_aa_ets_signerLocation, null, OBJ_id_smime_aa_ets_signerLocation); // NID: 228
        addObject(NID_id_smime_aa_ets_signerAttr, SN_id_smime_aa_ets_signerAttr, null, OBJ_id_smime_aa_ets_signerAttr); // NID: 229
        addObject(NID_id_smime_aa_ets_otherSigCert, SN_id_smime_aa_ets_otherSigCert, null, OBJ_id_smime_aa_ets_otherSigCert); // NID: 230
        addObject(NID_id_smime_aa_ets_contentTimestamp, SN_id_smime_aa_ets_contentTimestamp, null, OBJ_id_smime_aa_ets_contentTimestamp); // NID: 231
        addObject(NID_id_smime_aa_ets_CertificateRefs, SN_id_smime_aa_ets_CertificateRefs, null, OBJ_id_smime_aa_ets_CertificateRefs); // NID: 232
        addObject(NID_id_smime_aa_ets_RevocationRefs, SN_id_smime_aa_ets_RevocationRefs, null, OBJ_id_smime_aa_ets_RevocationRefs); // NID: 233
        addObject(NID_id_smime_aa_ets_certValues, SN_id_smime_aa_ets_certValues, null, OBJ_id_smime_aa_ets_certValues); // NID: 234
        addObject(NID_id_smime_aa_ets_revocationValues, SN_id_smime_aa_ets_revocationValues, null, OBJ_id_smime_aa_ets_revocationValues); // NID: 235
        addObject(NID_id_smime_aa_ets_escTimeStamp, SN_id_smime_aa_ets_escTimeStamp, null, OBJ_id_smime_aa_ets_escTimeStamp); // NID: 236
        addObject(NID_id_smime_aa_ets_certCRLTimestamp, SN_id_smime_aa_ets_certCRLTimestamp, null, OBJ_id_smime_aa_ets_certCRLTimestamp); // NID: 237
        addObject(NID_id_smime_aa_ets_archiveTimeStamp, SN_id_smime_aa_ets_archiveTimeStamp, null, OBJ_id_smime_aa_ets_archiveTimeStamp); // NID: 238
        addObject(NID_id_smime_aa_signatureType, SN_id_smime_aa_signatureType, null, OBJ_id_smime_aa_signatureType); // NID: 239
        addObject(NID_id_smime_aa_dvcs_dvc, SN_id_smime_aa_dvcs_dvc, null, OBJ_id_smime_aa_dvcs_dvc); // NID: 240
        addObject(NID_id_smime_alg_ESDHwith3DES, SN_id_smime_alg_ESDHwith3DES, null, OBJ_id_smime_alg_ESDHwith3DES); // NID: 241
        addObject(NID_id_smime_alg_ESDHwithRC2, SN_id_smime_alg_ESDHwithRC2, null, OBJ_id_smime_alg_ESDHwithRC2); // NID: 242
        addObject(NID_id_smime_alg_3DESwrap, SN_id_smime_alg_3DESwrap, null, OBJ_id_smime_alg_3DESwrap); // NID: 243
        addObject(NID_id_smime_alg_RC2wrap, SN_id_smime_alg_RC2wrap, null, OBJ_id_smime_alg_RC2wrap); // NID: 244
        addObject(NID_id_smime_alg_ESDH, SN_id_smime_alg_ESDH, null, OBJ_id_smime_alg_ESDH); // NID: 245
        addObject(NID_id_smime_alg_CMS3DESwrap, SN_id_smime_alg_CMS3DESwrap, null, OBJ_id_smime_alg_CMS3DESwrap); // NID: 246
        addObject(NID_id_smime_alg_CMSRC2wrap, SN_id_smime_alg_CMSRC2wrap, null, OBJ_id_smime_alg_CMSRC2wrap); // NID: 247
        addObject(NID_id_smime_cd_ldap, SN_id_smime_cd_ldap, null, OBJ_id_smime_cd_ldap); // NID: 248
        addObject(NID_id_smime_spq_ets_sqt_uri, SN_id_smime_spq_ets_sqt_uri, null, OBJ_id_smime_spq_ets_sqt_uri); // NID: 249
        addObject(NID_id_smime_spq_ets_sqt_unotice, SN_id_smime_spq_ets_sqt_unotice, null, OBJ_id_smime_spq_ets_sqt_unotice); // NID: 250
        addObject(NID_id_smime_cti_ets_proofOfOrigin, SN_id_smime_cti_ets_proofOfOrigin, null, OBJ_id_smime_cti_ets_proofOfOrigin); // NID: 251
        addObject(NID_id_smime_cti_ets_proofOfReceipt, SN_id_smime_cti_ets_proofOfReceipt, null, OBJ_id_smime_cti_ets_proofOfReceipt); // NID: 252
        addObject(NID_id_smime_cti_ets_proofOfDelivery, SN_id_smime_cti_ets_proofOfDelivery, null, OBJ_id_smime_cti_ets_proofOfDelivery); // NID: 253
        addObject(NID_id_smime_cti_ets_proofOfSender, SN_id_smime_cti_ets_proofOfSender, null, OBJ_id_smime_cti_ets_proofOfSender); // NID: 254
        addObject(NID_id_smime_cti_ets_proofOfApproval, SN_id_smime_cti_ets_proofOfApproval, null, OBJ_id_smime_cti_ets_proofOfApproval); // NID: 255
        addObject(NID_id_smime_cti_ets_proofOfCreation, SN_id_smime_cti_ets_proofOfCreation, null, OBJ_id_smime_cti_ets_proofOfCreation); // NID: 256
        addObject(NID_md4, SN_md4, LN_md4, OBJ_md4); // NID: 257
        addObject(NID_id_pkix_mod, SN_id_pkix_mod, null, OBJ_id_pkix_mod); // NID: 258
        addObject(NID_id_qt, SN_id_qt, null, OBJ_id_qt); // NID: 259
        addObject(NID_id_it, SN_id_it, null, OBJ_id_it); // NID: 260
        addObject(NID_id_pkip, SN_id_pkip, null, OBJ_id_pkip); // NID: 261
        addObject(NID_id_alg, SN_id_alg, null, OBJ_id_alg); // NID: 262
        addObject(NID_id_cmc, SN_id_cmc, null, OBJ_id_cmc); // NID: 263
        addObject(NID_id_on, SN_id_on, null, OBJ_id_on); // NID: 264
        addObject(NID_id_pda, SN_id_pda, null, OBJ_id_pda); // NID: 265
        addObject(NID_id_aca, SN_id_aca, null, OBJ_id_aca); // NID: 266
        addObject(NID_id_qcs, SN_id_qcs, null, OBJ_id_qcs); // NID: 267
        addObject(NID_id_cct, SN_id_cct, null, OBJ_id_cct); // NID: 268
        addObject(NID_id_pkix1_explicit_88, SN_id_pkix1_explicit_88, null, OBJ_id_pkix1_explicit_88); // NID: 269
        addObject(NID_id_pkix1_implicit_88, SN_id_pkix1_implicit_88, null, OBJ_id_pkix1_implicit_88); // NID: 270
        addObject(NID_id_pkix1_explicit_93, SN_id_pkix1_explicit_93, null, OBJ_id_pkix1_explicit_93); // NID: 271
        addObject(NID_id_pkix1_implicit_93, SN_id_pkix1_implicit_93, null, OBJ_id_pkix1_implicit_93); // NID: 272
        addObject(NID_id_mod_crmf, SN_id_mod_crmf, null, OBJ_id_mod_crmf); // NID: 273
        addObject(NID_id_mod_cmc, SN_id_mod_cmc, null, OBJ_id_mod_cmc); // NID: 274
        addObject(NID_id_mod_kea_profile_88, SN_id_mod_kea_profile_88, null, OBJ_id_mod_kea_profile_88); // NID: 275
        addObject(NID_id_mod_kea_profile_93, SN_id_mod_kea_profile_93, null, OBJ_id_mod_kea_profile_93); // NID: 276
        addObject(NID_id_mod_cmp, SN_id_mod_cmp, null, OBJ_id_mod_cmp); // NID: 277
        addObject(NID_id_mod_qualified_cert_88, SN_id_mod_qualified_cert_88, null, OBJ_id_mod_qualified_cert_88); // NID: 278
        addObject(NID_id_mod_qualified_cert_93, SN_id_mod_qualified_cert_93, null, OBJ_id_mod_qualified_cert_93); // NID: 279
        addObject(NID_id_mod_attribute_cert, SN_id_mod_attribute_cert, null, OBJ_id_mod_attribute_cert); // NID: 280
        addObject(NID_id_mod_timestamp_protocol, SN_id_mod_timestamp_protocol, null, OBJ_id_mod_timestamp_protocol); // NID: 281
        addObject(NID_id_mod_ocsp, SN_id_mod_ocsp, null, OBJ_id_mod_ocsp); // NID: 282
        addObject(NID_id_mod_dvcs, SN_id_mod_dvcs, null, OBJ_id_mod_dvcs); // NID: 283
        addObject(NID_id_mod_cmp2000, SN_id_mod_cmp2000, null, OBJ_id_mod_cmp2000); // NID: 284
        addObject(NID_biometricInfo, SN_biometricInfo, LN_biometricInfo, OBJ_biometricInfo); // NID: 285
        addObject(NID_qcStatements, SN_qcStatements, null, OBJ_qcStatements); // NID: 286
        addObject(NID_ac_auditEntity, SN_ac_auditEntity, null, OBJ_ac_auditEntity); // NID: 287
        addObject(NID_ac_targeting, SN_ac_targeting, null, OBJ_ac_targeting); // NID: 288
        addObject(NID_aaControls, SN_aaControls, null, OBJ_aaControls); // NID: 289
        addObject(NID_sbgp_ipAddrBlock, SN_sbgp_ipAddrBlock, null, OBJ_sbgp_ipAddrBlock); // NID: 290
        addObject(NID_sbgp_autonomousSysNum, SN_sbgp_autonomousSysNum, null, OBJ_sbgp_autonomousSysNum); // NID: 291
        addObject(NID_sbgp_routerIdentifier, SN_sbgp_routerIdentifier, null, OBJ_sbgp_routerIdentifier); // NID: 292
        addObject(NID_textNotice, SN_textNotice, null, OBJ_textNotice); // NID: 293
        addObject(NID_ipsecEndSystem, SN_ipsecEndSystem, LN_ipsecEndSystem, OBJ_ipsecEndSystem); // NID: 294
        addObject(NID_ipsecTunnel, SN_ipsecTunnel, LN_ipsecTunnel, OBJ_ipsecTunnel); // NID: 295
        addObject(NID_ipsecUser, SN_ipsecUser, LN_ipsecUser, OBJ_ipsecUser); // NID: 296
        addObject(NID_dvcs, SN_dvcs, LN_dvcs, OBJ_dvcs); // NID: 297
        addObject(NID_id_it_caProtEncCert, SN_id_it_caProtEncCert, null, OBJ_id_it_caProtEncCert); // NID: 298
        addObject(NID_id_it_signKeyPairTypes, SN_id_it_signKeyPairTypes, null, OBJ_id_it_signKeyPairTypes); // NID: 299
        addObject(NID_id_it_encKeyPairTypes, SN_id_it_encKeyPairTypes, null, OBJ_id_it_encKeyPairTypes); // NID: 300
        addObject(NID_id_it_preferredSymmAlg, SN_id_it_preferredSymmAlg, null, OBJ_id_it_preferredSymmAlg); // NID: 301
        addObject(NID_id_it_caKeyUpdateInfo, SN_id_it_caKeyUpdateInfo, null, OBJ_id_it_caKeyUpdateInfo); // NID: 302
        addObject(NID_id_it_currentCRL, SN_id_it_currentCRL, null, OBJ_id_it_currentCRL); // NID: 303
        addObject(NID_id_it_unsupportedOIDs, SN_id_it_unsupportedOIDs, null, OBJ_id_it_unsupportedOIDs); // NID: 304
        addObject(NID_id_it_subscriptionRequest, SN_id_it_subscriptionRequest, null, OBJ_id_it_subscriptionRequest); // NID: 305
        addObject(NID_id_it_subscriptionResponse, SN_id_it_subscriptionResponse, null, OBJ_id_it_subscriptionResponse); // NID: 306
        addObject(NID_id_it_keyPairParamReq, SN_id_it_keyPairParamReq, null, OBJ_id_it_keyPairParamReq); // NID: 307
        addObject(NID_id_it_keyPairParamRep, SN_id_it_keyPairParamRep, null, OBJ_id_it_keyPairParamRep); // NID: 308
        addObject(NID_id_it_revPassphrase, SN_id_it_revPassphrase, null, OBJ_id_it_revPassphrase); // NID: 309
        addObject(NID_id_it_implicitConfirm, SN_id_it_implicitConfirm, null, OBJ_id_it_implicitConfirm); // NID: 310
        addObject(NID_id_it_confirmWaitTime, SN_id_it_confirmWaitTime, null, OBJ_id_it_confirmWaitTime); // NID: 311
        addObject(NID_id_it_origPKIMessage, SN_id_it_origPKIMessage, null, OBJ_id_it_origPKIMessage); // NID: 312
        addObject(NID_id_regCtrl, SN_id_regCtrl, null, OBJ_id_regCtrl); // NID: 313
        addObject(NID_id_regInfo, SN_id_regInfo, null, OBJ_id_regInfo); // NID: 314
        addObject(NID_id_regCtrl_regToken, SN_id_regCtrl_regToken, null, OBJ_id_regCtrl_regToken); // NID: 315
        addObject(NID_id_regCtrl_authenticator, SN_id_regCtrl_authenticator, null, OBJ_id_regCtrl_authenticator); // NID: 316
        addObject(NID_id_regCtrl_pkiPublicationInfo, SN_id_regCtrl_pkiPublicationInfo, null, OBJ_id_regCtrl_pkiPublicationInfo); // NID: 317
        addObject(NID_id_regCtrl_pkiArchiveOptions, SN_id_regCtrl_pkiArchiveOptions, null, OBJ_id_regCtrl_pkiArchiveOptions); // NID: 318
        addObject(NID_id_regCtrl_oldCertID, SN_id_regCtrl_oldCertID, null, OBJ_id_regCtrl_oldCertID); // NID: 319
        addObject(NID_id_regCtrl_protocolEncrKey, SN_id_regCtrl_protocolEncrKey, null, OBJ_id_regCtrl_protocolEncrKey); // NID: 320
        addObject(NID_id_regInfo_utf8Pairs, SN_id_regInfo_utf8Pairs, null, OBJ_id_regInfo_utf8Pairs); // NID: 321
        addObject(NID_id_regInfo_certReq, SN_id_regInfo_certReq, null, OBJ_id_regInfo_certReq); // NID: 322
        addObject(NID_id_alg_des40, SN_id_alg_des40, null, OBJ_id_alg_des40); // NID: 323
        addObject(NID_id_alg_noSignature, SN_id_alg_noSignature, null, OBJ_id_alg_noSignature); // NID: 324
        addObject(NID_id_alg_dh_sig_hmac_sha1, SN_id_alg_dh_sig_hmac_sha1, null, OBJ_id_alg_dh_sig_hmac_sha1); // NID: 325
        addObject(NID_id_alg_dh_pop, SN_id_alg_dh_pop, null, OBJ_id_alg_dh_pop); // NID: 326
        addObject(NID_id_cmc_statusInfo, SN_id_cmc_statusInfo, null, OBJ_id_cmc_statusInfo); // NID: 327
        addObject(NID_id_cmc_identification, SN_id_cmc_identification, null, OBJ_id_cmc_identification); // NID: 328
        addObject(NID_id_cmc_identityProof, SN_id_cmc_identityProof, null, OBJ_id_cmc_identityProof); // NID: 329
        addObject(NID_id_cmc_dataReturn, SN_id_cmc_dataReturn, null, OBJ_id_cmc_dataReturn); // NID: 330
        addObject(NID_id_cmc_transactionId, SN_id_cmc_transactionId, null, OBJ_id_cmc_transactionId); // NID: 331
        addObject(NID_id_cmc_senderNonce, SN_id_cmc_senderNonce, null, OBJ_id_cmc_senderNonce); // NID: 332
        addObject(NID_id_cmc_recipientNonce, SN_id_cmc_recipientNonce, null, OBJ_id_cmc_recipientNonce); // NID: 333
        addObject(NID_id_cmc_addExtensions, SN_id_cmc_addExtensions, null, OBJ_id_cmc_addExtensions); // NID: 334
        addObject(NID_id_cmc_encryptedPOP, SN_id_cmc_encryptedPOP, null, OBJ_id_cmc_encryptedPOP); // NID: 335
        addObject(NID_id_cmc_decryptedPOP, SN_id_cmc_decryptedPOP, null, OBJ_id_cmc_decryptedPOP); // NID: 336
        addObject(NID_id_cmc_lraPOPWitness, SN_id_cmc_lraPOPWitness, null, OBJ_id_cmc_lraPOPWitness); // NID: 337
        addObject(NID_id_cmc_getCert, SN_id_cmc_getCert, null, OBJ_id_cmc_getCert); // NID: 338
        addObject(NID_id_cmc_getCRL, SN_id_cmc_getCRL, null, OBJ_id_cmc_getCRL); // NID: 339
        addObject(NID_id_cmc_revokeRequest, SN_id_cmc_revokeRequest, null, OBJ_id_cmc_revokeRequest); // NID: 340
        addObject(NID_id_cmc_regInfo, SN_id_cmc_regInfo, null, OBJ_id_cmc_regInfo); // NID: 341
        addObject(NID_id_cmc_responseInfo, SN_id_cmc_responseInfo, null, OBJ_id_cmc_responseInfo); // NID: 342
        addObject(NID_id_cmc_queryPending, SN_id_cmc_queryPending, null, OBJ_id_cmc_queryPending); // NID: 343
        addObject(NID_id_cmc_popLinkRandom, SN_id_cmc_popLinkRandom, null, OBJ_id_cmc_popLinkRandom); // NID: 344
        addObject(NID_id_cmc_popLinkWitness, SN_id_cmc_popLinkWitness, null, OBJ_id_cmc_popLinkWitness); // NID: 345
        addObject(NID_id_cmc_confirmCertAcceptance, SN_id_cmc_confirmCertAcceptance, null, OBJ_id_cmc_confirmCertAcceptance); // NID: 346
        addObject(NID_id_on_personalData, SN_id_on_personalData, null, OBJ_id_on_personalData); // NID: 347
        addObject(NID_id_pda_dateOfBirth, SN_id_pda_dateOfBirth, null, OBJ_id_pda_dateOfBirth); // NID: 348
        addObject(NID_id_pda_placeOfBirth, SN_id_pda_placeOfBirth, null, OBJ_id_pda_placeOfBirth); // NID: 349
        addObject(NID_id_pda_gender, SN_id_pda_gender, null, OBJ_id_pda_gender); // NID: 351
        addObject(NID_id_pda_countryOfCitizenship, SN_id_pda_countryOfCitizenship, null, OBJ_id_pda_countryOfCitizenship); // NID: 352
        addObject(NID_id_pda_countryOfResidence, SN_id_pda_countryOfResidence, null, OBJ_id_pda_countryOfResidence); // NID: 353
        addObject(NID_id_aca_authenticationInfo, SN_id_aca_authenticationInfo, null, OBJ_id_aca_authenticationInfo); // NID: 354
        addObject(NID_id_aca_accessIdentity, SN_id_aca_accessIdentity, null, OBJ_id_aca_accessIdentity); // NID: 355
        addObject(NID_id_aca_chargingIdentity, SN_id_aca_chargingIdentity, null, OBJ_id_aca_chargingIdentity); // NID: 356
        addObject(NID_id_aca_group, SN_id_aca_group, null, OBJ_id_aca_group); // NID: 357
        addObject(NID_id_aca_role, SN_id_aca_role, null, OBJ_id_aca_role); // NID: 358
        addObject(NID_id_qcs_pkixQCSyntax_v1, SN_id_qcs_pkixQCSyntax_v1, null, OBJ_id_qcs_pkixQCSyntax_v1); // NID: 359
        addObject(NID_id_cct_crs, SN_id_cct_crs, null, OBJ_id_cct_crs); // NID: 360
        addObject(NID_id_cct_PKIData, SN_id_cct_PKIData, null, OBJ_id_cct_PKIData); // NID: 361
        addObject(NID_id_cct_PKIResponse, SN_id_cct_PKIResponse, null, OBJ_id_cct_PKIResponse); // NID: 362
        addObject(NID_ad_timeStamping, SN_ad_timeStamping, LN_ad_timeStamping, OBJ_ad_timeStamping); // NID: 363
        addObject(NID_ad_dvcs, SN_ad_dvcs, LN_ad_dvcs, OBJ_ad_dvcs); // NID: 364
        addObject(NID_id_pkix_OCSP_basic, SN_id_pkix_OCSP_basic, LN_id_pkix_OCSP_basic, OBJ_id_pkix_OCSP_basic); // NID: 365
        addObject(NID_id_pkix_OCSP_Nonce, SN_id_pkix_OCSP_Nonce, LN_id_pkix_OCSP_Nonce, OBJ_id_pkix_OCSP_Nonce); // NID: 366
        addObject(NID_id_pkix_OCSP_CrlID, SN_id_pkix_OCSP_CrlID, LN_id_pkix_OCSP_CrlID, OBJ_id_pkix_OCSP_CrlID); // NID: 367
        addObject(NID_id_pkix_OCSP_acceptableResponses, SN_id_pkix_OCSP_acceptableResponses, LN_id_pkix_OCSP_acceptableResponses, OBJ_id_pkix_OCSP_acceptableResponses); // NID: 368
        addObject(NID_id_pkix_OCSP_noCheck, SN_id_pkix_OCSP_noCheck, LN_id_pkix_OCSP_noCheck, OBJ_id_pkix_OCSP_noCheck); // NID: 369
        addObject(NID_id_pkix_OCSP_archiveCutoff, SN_id_pkix_OCSP_archiveCutoff, LN_id_pkix_OCSP_archiveCutoff, OBJ_id_pkix_OCSP_archiveCutoff); // NID: 370
        addObject(NID_id_pkix_OCSP_serviceLocator, SN_id_pkix_OCSP_serviceLocator, LN_id_pkix_OCSP_serviceLocator, OBJ_id_pkix_OCSP_serviceLocator); // NID: 371
        addObject(NID_id_pkix_OCSP_extendedStatus, SN_id_pkix_OCSP_extendedStatus, LN_id_pkix_OCSP_extendedStatus, OBJ_id_pkix_OCSP_extendedStatus); // NID: 372
        addObject(NID_id_pkix_OCSP_valid, SN_id_pkix_OCSP_valid, null, OBJ_id_pkix_OCSP_valid); // NID: 373
        addObject(NID_id_pkix_OCSP_path, SN_id_pkix_OCSP_path, null, OBJ_id_pkix_OCSP_path); // NID: 374
        addObject(NID_id_pkix_OCSP_trustRoot, SN_id_pkix_OCSP_trustRoot, LN_id_pkix_OCSP_trustRoot, OBJ_id_pkix_OCSP_trustRoot); // NID: 375
        addObject(NID_algorithm, SN_algorithm, LN_algorithm, OBJ_algorithm); // NID: 376
        addObject(NID_rsaSignature, SN_rsaSignature, null, OBJ_rsaSignature); // NID: 377
        addObject(NID_X500algorithms, SN_X500algorithms, LN_X500algorithms, OBJ_X500algorithms); // NID: 378
        addObject(NID_org, SN_org, LN_org, OBJ_org); // NID: 379
        addObject(NID_dod, SN_dod, LN_dod, OBJ_dod); // NID: 380
        addObject(NID_iana, SN_iana, LN_iana, OBJ_iana); // NID: 381
        addObject(NID_Directory, SN_Directory, LN_Directory, OBJ_Directory); // NID: 382
        addObject(NID_Management, SN_Management, LN_Management, OBJ_Management); // NID: 383
        addObject(NID_Experimental, SN_Experimental, LN_Experimental, OBJ_Experimental); // NID: 384
        addObject(NID_Private, SN_Private, LN_Private, OBJ_Private); // NID: 385
        addObject(NID_Security, SN_Security, LN_Security, OBJ_Security); // NID: 386
        addObject(NID_SNMPv2, SN_SNMPv2, LN_SNMPv2, OBJ_SNMPv2); // NID: 387
        addObject(NID_Mail, null, LN_Mail, OBJ_Mail); // NID: 388
        addObject(NID_Enterprises, SN_Enterprises, LN_Enterprises, OBJ_Enterprises); // NID: 389
        addObject(NID_dcObject, SN_dcObject, LN_dcObject, OBJ_dcObject); // NID: 390
        addObject(NID_domainComponent, SN_domainComponent, LN_domainComponent, OBJ_domainComponent); // NID: 391
        addObject(NID_Domain, SN_Domain, LN_Domain, OBJ_Domain); // NID: 392
        //addObject(NID_joint_iso_ccitt, null, null, OBJ_joint_iso_ccitt); // NID: 393
        addObject(NID_selected_attribute_types, SN_selected_attribute_types, LN_selected_attribute_types, OBJ_selected_attribute_types); // NID: 394
        addObject(NID_clearance, SN_clearance, null, OBJ_clearance); // NID: 395
        addObject(NID_md4WithRSAEncryption, SN_md4WithRSAEncryption, LN_md4WithRSAEncryption, OBJ_md4WithRSAEncryption); // NID: 396
        addObject(NID_ac_proxying, SN_ac_proxying, null, OBJ_ac_proxying); // NID: 397
        addObject(NID_sinfo_access, SN_sinfo_access, LN_sinfo_access, OBJ_sinfo_access); // NID: 398
        addObject(NID_id_aca_encAttrs, SN_id_aca_encAttrs, null, OBJ_id_aca_encAttrs); // NID: 399
        addObject(NID_role, SN_role, LN_role, OBJ_role); // NID: 400
        addObject(NID_policy_constraints, SN_policy_constraints, LN_policy_constraints, OBJ_policy_constraints); // NID: 401
        addObject(NID_target_information, SN_target_information, LN_target_information, OBJ_target_information); // NID: 402
        addObject(NID_no_rev_avail, SN_no_rev_avail, LN_no_rev_avail, OBJ_no_rev_avail); // NID: 403
        //addObject(NID_ccitt, null, null, OBJ_ccitt); // NID: 404
        addObject(NID_ansi_X9_62, SN_ansi_X9_62, LN_ansi_X9_62, OBJ_ansi_X9_62); // NID: 405
        addObject(NID_X9_62_prime_field, SN_X9_62_prime_field, null, OBJ_X9_62_prime_field); // NID: 406
        addObject(NID_X9_62_characteristic_two_field, SN_X9_62_characteristic_two_field, null, OBJ_X9_62_characteristic_two_field); // NID: 407
        addObject(NID_X9_62_id_ecPublicKey, SN_X9_62_id_ecPublicKey, null, OBJ_X9_62_id_ecPublicKey); // NID: 408
        addObject(NID_X9_62_prime192v1, SN_X9_62_prime192v1, null, OBJ_X9_62_prime192v1); // NID: 409
        addObject(NID_X9_62_prime192v2, SN_X9_62_prime192v2, null, OBJ_X9_62_prime192v2); // NID: 410
        addObject(NID_X9_62_prime192v3, SN_X9_62_prime192v3, null, OBJ_X9_62_prime192v3); // NID: 411
        addObject(NID_X9_62_prime239v1, SN_X9_62_prime239v1, null, OBJ_X9_62_prime239v1); // NID: 412
        addObject(NID_X9_62_prime239v2, SN_X9_62_prime239v2, null, OBJ_X9_62_prime239v2); // NID: 413
        addObject(NID_X9_62_prime239v3, SN_X9_62_prime239v3, null, OBJ_X9_62_prime239v3); // NID: 414
        addObject(NID_X9_62_prime256v1, SN_X9_62_prime256v1, null, OBJ_X9_62_prime256v1); // NID: 415
        addObject(NID_ecdsa_with_SHA1, SN_ecdsa_with_SHA1, null, OID_ecdsa_with_SHA1); // NID: 416
        addObject(NID_ms_csp_name, SN_ms_csp_name, LN_ms_csp_name, OBJ_ms_csp_name); // NID: 417
        addObject(NID_aes_128_ecb, SN_aes_128_ecb, LN_aes_128_ecb, OBJ_aes_128_ecb); // NID: 418
        addObject(NID_aes_128_cbc, SN_aes_128_cbc, LN_aes_128_cbc, OBJ_aes_128_cbc); // NID: 419
        addObject(NID_aes_128_ofb128, SN_aes_128_ofb128, LN_aes_128_ofb128, OBJ_aes_128_ofb128); // NID: 420
        addObject(NID_aes_128_cfb128, SN_aes_128_cfb128, LN_aes_128_cfb128, OBJ_aes_128_cfb128); // NID: 421
        addObject(NID_aes_192_ecb, SN_aes_192_ecb, LN_aes_192_ecb, OBJ_aes_192_ecb); // NID: 422
        addObject(NID_aes_192_cbc, SN_aes_192_cbc, LN_aes_192_cbc, OBJ_aes_192_cbc); // NID: 423
        addObject(NID_aes_192_ofb128, SN_aes_192_ofb128, LN_aes_192_ofb128, OBJ_aes_192_ofb128); // NID: 424
        addObject(NID_aes_192_cfb128, SN_aes_192_cfb128, LN_aes_192_cfb128, OBJ_aes_192_cfb128); // NID: 425
        addObject(NID_aes_256_ecb, SN_aes_256_ecb, LN_aes_256_ecb, OBJ_aes_256_ecb); // NID: 426
        addObject(NID_aes_256_cbc, SN_aes_256_cbc, LN_aes_256_cbc, OBJ_aes_256_cbc); // NID: 427
        addObject(NID_aes_256_ofb128, SN_aes_256_ofb128, LN_aes_256_ofb128, OBJ_aes_256_ofb128); // NID: 428
        addObject(NID_aes_256_cfb128, SN_aes_256_cfb128, LN_aes_256_cfb128, OBJ_aes_256_cfb128); // NID: 429
        addObject(NID_hold_instruction_code, SN_hold_instruction_code, LN_hold_instruction_code, OBJ_hold_instruction_code); // NID: 430
        addObject(NID_hold_instruction_none, SN_hold_instruction_none, LN_hold_instruction_none, OBJ_hold_instruction_none); // NID: 431
        addObject(NID_hold_instruction_call_issuer, SN_hold_instruction_call_issuer, LN_hold_instruction_call_issuer, OBJ_hold_instruction_call_issuer); // NID: 432
        addObject(NID_hold_instruction_reject, SN_hold_instruction_reject, LN_hold_instruction_reject, OBJ_hold_instruction_reject); // NID: 433
        addObject(NID_data, SN_data, null, OBJ_data); // NID: 434
        addObject(NID_pss, SN_pss, null, OBJ_pss); // NID: 435
        addObject(NID_ucl, SN_ucl, null, OBJ_ucl); // NID: 436
        addObject(NID_pilot, SN_pilot, null, OBJ_pilot); // NID: 437
        addObject(NID_pilotAttributeType, null, LN_pilotAttributeType, OBJ_pilotAttributeType); // NID: 438
        addObject(NID_pilotAttributeSyntax, null, LN_pilotAttributeSyntax, OBJ_pilotAttributeSyntax); // NID: 439
        addObject(NID_pilotObjectClass, null, LN_pilotObjectClass, OBJ_pilotObjectClass); // NID: 440
        addObject(NID_pilotGroups, null, LN_pilotGroups, OBJ_pilotGroups); // NID: 441
        addObject(NID_iA5StringSyntax, null, LN_iA5StringSyntax, OBJ_iA5StringSyntax); // NID: 442
        addObject(NID_caseIgnoreIA5StringSyntax, null, LN_caseIgnoreIA5StringSyntax, OBJ_caseIgnoreIA5StringSyntax); // NID: 443
        addObject(NID_pilotObject, null, LN_pilotObject, OBJ_pilotObject); // NID: 444
        addObject(NID_pilotPerson, null, LN_pilotPerson, OBJ_pilotPerson); // NID: 445
        addObject(NID_account, SN_account, null, OBJ_account); // NID: 446
        addObject(NID_document, SN_document, null, OBJ_document); // NID: 447
        addObject(NID_room, SN_room, null, OBJ_room); // NID: 448
        addObject(NID_documentSeries, null, LN_documentSeries, OBJ_documentSeries); // NID: 449
        addObject(NID_rFC822localPart, null, LN_rFC822localPart, OBJ_rFC822localPart); // NID: 450
        addObject(NID_dNSDomain, null, LN_dNSDomain, OBJ_dNSDomain); // NID: 451
        addObject(NID_domainRelatedObject, null, LN_domainRelatedObject, OBJ_domainRelatedObject); // NID: 452
        addObject(NID_friendlyCountry, null, LN_friendlyCountry, OBJ_friendlyCountry); // NID: 453
        addObject(NID_simpleSecurityObject, null, LN_simpleSecurityObject, OBJ_simpleSecurityObject); // NID: 454
        addObject(NID_pilotOrganization, null, LN_pilotOrganization, OBJ_pilotOrganization); // NID: 455
        addObject(NID_pilotDSA, null, LN_pilotDSA, OBJ_pilotDSA); // NID: 456
        addObject(NID_qualityLabelledData, null, LN_qualityLabelledData, OBJ_qualityLabelledData); // NID: 457
        addObject(NID_userId, SN_userId, LN_userId, OBJ_userId); // NID: 458
        addObject(NID_textEncodedORAddress, null, LN_textEncodedORAddress, OBJ_textEncodedORAddress); // NID: 459
        addObject(NID_rfc822Mailbox, SN_rfc822Mailbox, LN_rfc822Mailbox, OBJ_rfc822Mailbox); // NID: 460
        addObject(NID_info, SN_info, null, OBJ_info); // NID: 461
        addObject(NID_favouriteDrink, null, LN_favouriteDrink, OBJ_favouriteDrink); // NID: 462
        addObject(NID_roomNumber, null, LN_roomNumber, OBJ_roomNumber); // NID: 463
        addObject(NID_photo, SN_photo, null, OBJ_photo); // NID: 464
        addObject(NID_userClass, null, LN_userClass, OBJ_userClass); // NID: 465
        addObject(NID_host, SN_host, null, OBJ_host); // NID: 466
        addObject(NID_manager, SN_manager, null, OBJ_manager); // NID: 467
        addObject(NID_documentIdentifier, null, LN_documentIdentifier, OBJ_documentIdentifier); // NID: 468
        addObject(NID_documentTitle, null, LN_documentTitle, OBJ_documentTitle); // NID: 469
        addObject(NID_documentVersion, null, LN_documentVersion, OBJ_documentVersion); // NID: 470
        addObject(NID_documentAuthor, null, LN_documentAuthor, OBJ_documentAuthor); // NID: 471
        addObject(NID_documentLocation, null, LN_documentLocation, OBJ_documentLocation); // NID: 472
        addObject(NID_homeTelephoneNumber, null, LN_homeTelephoneNumber, OBJ_homeTelephoneNumber); // NID: 473
        addObject(NID_secretary, SN_secretary, null, OBJ_secretary); // NID: 474
        addObject(NID_otherMailbox, null, LN_otherMailbox, OBJ_otherMailbox); // NID: 475
        addObject(NID_lastModifiedTime, null, LN_lastModifiedTime, OBJ_lastModifiedTime); // NID: 476
        addObject(NID_lastModifiedBy, null, LN_lastModifiedBy, OBJ_lastModifiedBy); // NID: 477
        addObject(NID_aRecord, null, LN_aRecord, OBJ_aRecord); // NID: 478
        addObject(NID_pilotAttributeType27, null, LN_pilotAttributeType27, OBJ_pilotAttributeType27); // NID: 479
        addObject(NID_mXRecord, null, LN_mXRecord, OBJ_mXRecord); // NID: 480
        addObject(NID_nSRecord, null, LN_nSRecord, OBJ_nSRecord); // NID: 481
        addObject(NID_sOARecord, null, LN_sOARecord, OBJ_sOARecord); // NID: 482
        addObject(NID_cNAMERecord, null, LN_cNAMERecord, OBJ_cNAMERecord); // NID: 483
        addObject(NID_associatedDomain, null, LN_associatedDomain, OBJ_associatedDomain); // NID: 484
        addObject(NID_associatedName, null, LN_associatedName, OBJ_associatedName); // NID: 485
        addObject(NID_homePostalAddress, null, LN_homePostalAddress, OBJ_homePostalAddress); // NID: 486
        addObject(NID_personalTitle, null, LN_personalTitle, OBJ_personalTitle); // NID: 487
        addObject(NID_mobileTelephoneNumber, null, LN_mobileTelephoneNumber, OBJ_mobileTelephoneNumber); // NID: 488
        addObject(NID_pagerTelephoneNumber, null, LN_pagerTelephoneNumber, OBJ_pagerTelephoneNumber); // NID: 489
        addObject(NID_friendlyCountryName, null, LN_friendlyCountryName, OBJ_friendlyCountryName); // NID: 490
        addObject(NID_organizationalStatus, null, LN_organizationalStatus, OBJ_organizationalStatus); // NID: 491
        addObject(NID_janetMailbox, null, LN_janetMailbox, OBJ_janetMailbox); // NID: 492
        addObject(NID_mailPreferenceOption, null, LN_mailPreferenceOption, OBJ_mailPreferenceOption); // NID: 493
        addObject(NID_buildingName, null, LN_buildingName, OBJ_buildingName); // NID: 494
        addObject(NID_dSAQuality, null, LN_dSAQuality, OBJ_dSAQuality); // NID: 495
        addObject(NID_singleLevelQuality, null, LN_singleLevelQuality, OBJ_singleLevelQuality); // NID: 496
        addObject(NID_subtreeMinimumQuality, null, LN_subtreeMinimumQuality, OBJ_subtreeMinimumQuality); // NID: 497
        addObject(NID_subtreeMaximumQuality, null, LN_subtreeMaximumQuality, OBJ_subtreeMaximumQuality); // NID: 498
        addObject(NID_personalSignature, null, LN_personalSignature, OBJ_personalSignature); // NID: 499
        addObject(NID_dITRedirect, null, LN_dITRedirect, OBJ_dITRedirect); // NID: 500
        addObject(NID_audio, SN_audio, null, OBJ_audio); // NID: 501
        addObject(NID_documentPublisher, null, LN_documentPublisher, OBJ_documentPublisher); // NID: 502
        addObject(NID_x500UniqueIdentifier, null, LN_x500UniqueIdentifier, OBJ_x500UniqueIdentifier); // NID: 503
        addObject(NID_mime_mhs, SN_mime_mhs, LN_mime_mhs, OBJ_mime_mhs); // NID: 504
        addObject(NID_mime_mhs_headings, SN_mime_mhs_headings, LN_mime_mhs_headings, OBJ_mime_mhs_headings); // NID: 505
        addObject(NID_mime_mhs_bodies, SN_mime_mhs_bodies, LN_mime_mhs_bodies, OBJ_mime_mhs_bodies); // NID: 506
        addObject(NID_id_hex_partial_message, SN_id_hex_partial_message, LN_id_hex_partial_message, OBJ_id_hex_partial_message); // NID: 507
        addObject(NID_id_hex_multipart_message, SN_id_hex_multipart_message, LN_id_hex_multipart_message, OBJ_id_hex_multipart_message); // NID: 508
        addObject(NID_generationQualifier, null, LN_generationQualifier, OBJ_generationQualifier); // NID: 509
        addObject(NID_pseudonym, null, LN_pseudonym, OBJ_pseudonym); // NID: 510
        addObject(NID_id_set, SN_id_set, LN_id_set, OBJ_id_set); // NID: 512
        addObject(NID_set_ctype, SN_set_ctype, LN_set_ctype, OBJ_set_ctype); // NID: 513
        addObject(NID_set_msgExt, SN_set_msgExt, LN_set_msgExt, OBJ_set_msgExt); // NID: 514
        addObject(NID_set_attr, SN_set_attr, null, OBJ_set_attr); // NID: 515
        addObject(NID_set_policy, SN_set_policy, null, OBJ_set_policy); // NID: 516
        addObject(NID_set_certExt, SN_set_certExt, LN_set_certExt, OBJ_set_certExt); // NID: 517
        addObject(NID_set_brand, SN_set_brand, null, OBJ_set_brand); // NID: 518
        addObject(NID_setct_PANData, SN_setct_PANData, null, OBJ_setct_PANData); // NID: 519
        addObject(NID_setct_PANToken, SN_setct_PANToken, null, OBJ_setct_PANToken); // NID: 520
        addObject(NID_setct_PANOnly, SN_setct_PANOnly, null, OBJ_setct_PANOnly); // NID: 521
        addObject(NID_setct_OIData, SN_setct_OIData, null, OBJ_setct_OIData); // NID: 522
        addObject(NID_setct_PI, SN_setct_PI, null, OBJ_setct_PI); // NID: 523
        addObject(NID_setct_PIData, SN_setct_PIData, null, OBJ_setct_PIData); // NID: 524
        addObject(NID_setct_PIDataUnsigned, SN_setct_PIDataUnsigned, null, OBJ_setct_PIDataUnsigned); // NID: 525
        addObject(NID_setct_HODInput, SN_setct_HODInput, null, OBJ_setct_HODInput); // NID: 526
        addObject(NID_setct_AuthResBaggage, SN_setct_AuthResBaggage, null, OBJ_setct_AuthResBaggage); // NID: 527
        addObject(NID_setct_AuthRevReqBaggage, SN_setct_AuthRevReqBaggage, null, OBJ_setct_AuthRevReqBaggage); // NID: 528
        addObject(NID_setct_AuthRevResBaggage, SN_setct_AuthRevResBaggage, null, OBJ_setct_AuthRevResBaggage); // NID: 529
        addObject(NID_setct_CapTokenSeq, SN_setct_CapTokenSeq, null, OBJ_setct_CapTokenSeq); // NID: 530
        addObject(NID_setct_PInitResData, SN_setct_PInitResData, null, OBJ_setct_PInitResData); // NID: 531
        addObject(NID_setct_PI_TBS, SN_setct_PI_TBS, null, OBJ_setct_PI_TBS); // NID: 532
        addObject(NID_setct_PResData, SN_setct_PResData, null, OBJ_setct_PResData); // NID: 533
        addObject(NID_setct_AuthReqTBS, SN_setct_AuthReqTBS, null, OBJ_setct_AuthReqTBS); // NID: 534
        addObject(NID_setct_AuthResTBS, SN_setct_AuthResTBS, null, OBJ_setct_AuthResTBS); // NID: 535
        addObject(NID_setct_AuthResTBSX, SN_setct_AuthResTBSX, null, OBJ_setct_AuthResTBSX); // NID: 536
        addObject(NID_setct_AuthTokenTBS, SN_setct_AuthTokenTBS, null, OBJ_setct_AuthTokenTBS); // NID: 537
        addObject(NID_setct_CapTokenData, SN_setct_CapTokenData, null, OBJ_setct_CapTokenData); // NID: 538
        addObject(NID_setct_CapTokenTBS, SN_setct_CapTokenTBS, null, OBJ_setct_CapTokenTBS); // NID: 539
        addObject(NID_setct_AcqCardCodeMsg, SN_setct_AcqCardCodeMsg, null, OBJ_setct_AcqCardCodeMsg); // NID: 540
        addObject(NID_setct_AuthRevReqTBS, SN_setct_AuthRevReqTBS, null, OBJ_setct_AuthRevReqTBS); // NID: 541
        addObject(NID_setct_AuthRevResData, SN_setct_AuthRevResData, null, OBJ_setct_AuthRevResData); // NID: 542
        addObject(NID_setct_AuthRevResTBS, SN_setct_AuthRevResTBS, null, OBJ_setct_AuthRevResTBS); // NID: 543
        addObject(NID_setct_CapReqTBS, SN_setct_CapReqTBS, null, OBJ_setct_CapReqTBS); // NID: 544
        addObject(NID_setct_CapReqTBSX, SN_setct_CapReqTBSX, null, OBJ_setct_CapReqTBSX); // NID: 545
        addObject(NID_setct_CapResData, SN_setct_CapResData, null, OBJ_setct_CapResData); // NID: 546
        addObject(NID_setct_CapRevReqTBS, SN_setct_CapRevReqTBS, null, OBJ_setct_CapRevReqTBS); // NID: 547
        addObject(NID_setct_CapRevReqTBSX, SN_setct_CapRevReqTBSX, null, OBJ_setct_CapRevReqTBSX); // NID: 548
        addObject(NID_setct_CapRevResData, SN_setct_CapRevResData, null, OBJ_setct_CapRevResData); // NID: 549
        addObject(NID_setct_CredReqTBS, SN_setct_CredReqTBS, null, OBJ_setct_CredReqTBS); // NID: 550
        addObject(NID_setct_CredReqTBSX, SN_setct_CredReqTBSX, null, OBJ_setct_CredReqTBSX); // NID: 551
        addObject(NID_setct_CredResData, SN_setct_CredResData, null, OBJ_setct_CredResData); // NID: 552
        addObject(NID_setct_CredRevReqTBS, SN_setct_CredRevReqTBS, null, OBJ_setct_CredRevReqTBS); // NID: 553
        addObject(NID_setct_CredRevReqTBSX, SN_setct_CredRevReqTBSX, null, OBJ_setct_CredRevReqTBSX); // NID: 554
        addObject(NID_setct_CredRevResData, SN_setct_CredRevResData, null, OBJ_setct_CredRevResData); // NID: 555
        addObject(NID_setct_PCertReqData, SN_setct_PCertReqData, null, OBJ_setct_PCertReqData); // NID: 556
        addObject(NID_setct_PCertResTBS, SN_setct_PCertResTBS, null, OBJ_setct_PCertResTBS); // NID: 557
        addObject(NID_setct_BatchAdminReqData, SN_setct_BatchAdminReqData, null, OBJ_setct_BatchAdminReqData); // NID: 558
        addObject(NID_setct_BatchAdminResData, SN_setct_BatchAdminResData, null, OBJ_setct_BatchAdminResData); // NID: 559
        addObject(NID_setct_CardCInitResTBS, SN_setct_CardCInitResTBS, null, OBJ_setct_CardCInitResTBS); // NID: 560
        addObject(NID_setct_MeAqCInitResTBS, SN_setct_MeAqCInitResTBS, null, OBJ_setct_MeAqCInitResTBS); // NID: 561
        addObject(NID_setct_RegFormResTBS, SN_setct_RegFormResTBS, null, OBJ_setct_RegFormResTBS); // NID: 562
        addObject(NID_setct_CertReqData, SN_setct_CertReqData, null, OBJ_setct_CertReqData); // NID: 563
        addObject(NID_setct_CertReqTBS, SN_setct_CertReqTBS, null, OBJ_setct_CertReqTBS); // NID: 564
        addObject(NID_setct_CertResData, SN_setct_CertResData, null, OBJ_setct_CertResData); // NID: 565
        addObject(NID_setct_CertInqReqTBS, SN_setct_CertInqReqTBS, null, OBJ_setct_CertInqReqTBS); // NID: 566
        addObject(NID_setct_ErrorTBS, SN_setct_ErrorTBS, null, OBJ_setct_ErrorTBS); // NID: 567
        addObject(NID_setct_PIDualSignedTBE, SN_setct_PIDualSignedTBE, null, OBJ_setct_PIDualSignedTBE); // NID: 568
        addObject(NID_setct_PIUnsignedTBE, SN_setct_PIUnsignedTBE, null, OBJ_setct_PIUnsignedTBE); // NID: 569
        addObject(NID_setct_AuthReqTBE, SN_setct_AuthReqTBE, null, OBJ_setct_AuthReqTBE); // NID: 570
        addObject(NID_setct_AuthResTBE, SN_setct_AuthResTBE, null, OBJ_setct_AuthResTBE); // NID: 571
        addObject(NID_setct_AuthResTBEX, SN_setct_AuthResTBEX, null, OBJ_setct_AuthResTBEX); // NID: 572
        addObject(NID_setct_AuthTokenTBE, SN_setct_AuthTokenTBE, null, OBJ_setct_AuthTokenTBE); // NID: 573
        addObject(NID_setct_CapTokenTBE, SN_setct_CapTokenTBE, null, OBJ_setct_CapTokenTBE); // NID: 574
        addObject(NID_setct_CapTokenTBEX, SN_setct_CapTokenTBEX, null, OBJ_setct_CapTokenTBEX); // NID: 575
        addObject(NID_setct_AcqCardCodeMsgTBE, SN_setct_AcqCardCodeMsgTBE, null, OBJ_setct_AcqCardCodeMsgTBE); // NID: 576
        addObject(NID_setct_AuthRevReqTBE, SN_setct_AuthRevReqTBE, null, OBJ_setct_AuthRevReqTBE); // NID: 577
        addObject(NID_setct_AuthRevResTBE, SN_setct_AuthRevResTBE, null, OBJ_setct_AuthRevResTBE); // NID: 578
        addObject(NID_setct_AuthRevResTBEB, SN_setct_AuthRevResTBEB, null, OBJ_setct_AuthRevResTBEB); // NID: 579
        addObject(NID_setct_CapReqTBE, SN_setct_CapReqTBE, null, OBJ_setct_CapReqTBE); // NID: 580
        addObject(NID_setct_CapReqTBEX, SN_setct_CapReqTBEX, null, OBJ_setct_CapReqTBEX); // NID: 581
        addObject(NID_setct_CapResTBE, SN_setct_CapResTBE, null, OBJ_setct_CapResTBE); // NID: 582
        addObject(NID_setct_CapRevReqTBE, SN_setct_CapRevReqTBE, null, OBJ_setct_CapRevReqTBE); // NID: 583
        addObject(NID_setct_CapRevReqTBEX, SN_setct_CapRevReqTBEX, null, OBJ_setct_CapRevReqTBEX); // NID: 584
        addObject(NID_setct_CapRevResTBE, SN_setct_CapRevResTBE, null, OBJ_setct_CapRevResTBE); // NID: 585
        addObject(NID_setct_CredReqTBE, SN_setct_CredReqTBE, null, OBJ_setct_CredReqTBE); // NID: 586
        addObject(NID_setct_CredReqTBEX, SN_setct_CredReqTBEX, null, OBJ_setct_CredReqTBEX); // NID: 587
        addObject(NID_setct_CredResTBE, SN_setct_CredResTBE, null, OBJ_setct_CredResTBE); // NID: 588
        addObject(NID_setct_CredRevReqTBE, SN_setct_CredRevReqTBE, null, OBJ_setct_CredRevReqTBE); // NID: 589
        addObject(NID_setct_CredRevReqTBEX, SN_setct_CredRevReqTBEX, null, OBJ_setct_CredRevReqTBEX); // NID: 590
        addObject(NID_setct_CredRevResTBE, SN_setct_CredRevResTBE, null, OBJ_setct_CredRevResTBE); // NID: 591
        addObject(NID_setct_BatchAdminReqTBE, SN_setct_BatchAdminReqTBE, null, OBJ_setct_BatchAdminReqTBE); // NID: 592
        addObject(NID_setct_BatchAdminResTBE, SN_setct_BatchAdminResTBE, null, OBJ_setct_BatchAdminResTBE); // NID: 593
        addObject(NID_setct_RegFormReqTBE, SN_setct_RegFormReqTBE, null, OBJ_setct_RegFormReqTBE); // NID: 594
        addObject(NID_setct_CertReqTBE, SN_setct_CertReqTBE, null, OBJ_setct_CertReqTBE); // NID: 595
        addObject(NID_setct_CertReqTBEX, SN_setct_CertReqTBEX, null, OBJ_setct_CertReqTBEX); // NID: 596
        addObject(NID_setct_CertResTBE, SN_setct_CertResTBE, null, OBJ_setct_CertResTBE); // NID: 597
        addObject(NID_setct_CRLNotificationTBS, SN_setct_CRLNotificationTBS, null, OBJ_setct_CRLNotificationTBS); // NID: 598
        addObject(NID_setct_CRLNotificationResTBS, SN_setct_CRLNotificationResTBS, null, OBJ_setct_CRLNotificationResTBS); // NID: 599
        addObject(NID_setct_BCIDistributionTBS, SN_setct_BCIDistributionTBS, null, OBJ_setct_BCIDistributionTBS); // NID: 600
        addObject(NID_setext_genCrypt, SN_setext_genCrypt, LN_setext_genCrypt, OBJ_setext_genCrypt); // NID: 601
        addObject(NID_setext_miAuth, SN_setext_miAuth, LN_setext_miAuth, OBJ_setext_miAuth); // NID: 602
        addObject(NID_setext_pinSecure, SN_setext_pinSecure, null, OBJ_setext_pinSecure); // NID: 603
        addObject(NID_setext_pinAny, SN_setext_pinAny, null, OBJ_setext_pinAny); // NID: 604
        addObject(NID_setext_track2, SN_setext_track2, null, OBJ_setext_track2); // NID: 605
        addObject(NID_setext_cv, SN_setext_cv, LN_setext_cv, OBJ_setext_cv); // NID: 606
        addObject(NID_set_policy_root, SN_set_policy_root, null, OBJ_set_policy_root); // NID: 607
        addObject(NID_setCext_hashedRoot, SN_setCext_hashedRoot, null, OBJ_setCext_hashedRoot); // NID: 608
        addObject(NID_setCext_certType, SN_setCext_certType, null, OBJ_setCext_certType); // NID: 609
        addObject(NID_setCext_merchData, SN_setCext_merchData, null, OBJ_setCext_merchData); // NID: 610
        addObject(NID_setCext_cCertRequired, SN_setCext_cCertRequired, null, OBJ_setCext_cCertRequired); // NID: 611
        addObject(NID_setCext_tunneling, SN_setCext_tunneling, null, OBJ_setCext_tunneling); // NID: 612
        addObject(NID_setCext_setExt, SN_setCext_setExt, null, OBJ_setCext_setExt); // NID: 613
        addObject(NID_setCext_setQualf, SN_setCext_setQualf, null, OBJ_setCext_setQualf); // NID: 614
        addObject(NID_setCext_PGWYcapabilities, SN_setCext_PGWYcapabilities, null, OBJ_setCext_PGWYcapabilities); // NID: 615
        addObject(NID_setCext_TokenIdentifier, SN_setCext_TokenIdentifier, null, OBJ_setCext_TokenIdentifier); // NID: 616
        addObject(NID_setCext_Track2Data, SN_setCext_Track2Data, null, OBJ_setCext_Track2Data); // NID: 617
        addObject(NID_setCext_TokenType, SN_setCext_TokenType, null, OBJ_setCext_TokenType); // NID: 618
        addObject(NID_setCext_IssuerCapabilities, SN_setCext_IssuerCapabilities, null, OBJ_setCext_IssuerCapabilities); // NID: 619
        addObject(NID_setAttr_Cert, SN_setAttr_Cert, null, OBJ_setAttr_Cert); // NID: 620
        addObject(NID_setAttr_PGWYcap, SN_setAttr_PGWYcap, LN_setAttr_PGWYcap, OBJ_setAttr_PGWYcap); // NID: 621
        addObject(NID_setAttr_TokenType, SN_setAttr_TokenType, null, OBJ_setAttr_TokenType); // NID: 622
        addObject(NID_setAttr_IssCap, SN_setAttr_IssCap, LN_setAttr_IssCap, OBJ_setAttr_IssCap); // NID: 623
        addObject(NID_set_rootKeyThumb, SN_set_rootKeyThumb, null, OBJ_set_rootKeyThumb); // NID: 624
        addObject(NID_set_addPolicy, SN_set_addPolicy, null, OBJ_set_addPolicy); // NID: 625
        addObject(NID_setAttr_Token_EMV, SN_setAttr_Token_EMV, null, OBJ_setAttr_Token_EMV); // NID: 626
        addObject(NID_setAttr_Token_B0Prime, SN_setAttr_Token_B0Prime, null, OBJ_setAttr_Token_B0Prime); // NID: 627
        addObject(NID_setAttr_IssCap_CVM, SN_setAttr_IssCap_CVM, null, OBJ_setAttr_IssCap_CVM); // NID: 628
        addObject(NID_setAttr_IssCap_T2, SN_setAttr_IssCap_T2, null, OBJ_setAttr_IssCap_T2); // NID: 629
        addObject(NID_setAttr_IssCap_Sig, SN_setAttr_IssCap_Sig, null, OBJ_setAttr_IssCap_Sig); // NID: 630
        addObject(NID_setAttr_GenCryptgrm, SN_setAttr_GenCryptgrm, LN_setAttr_GenCryptgrm, OBJ_setAttr_GenCryptgrm); // NID: 631
        addObject(NID_setAttr_T2Enc, SN_setAttr_T2Enc, LN_setAttr_T2Enc, OBJ_setAttr_T2Enc); // NID: 632
        addObject(NID_setAttr_T2cleartxt, SN_setAttr_T2cleartxt, LN_setAttr_T2cleartxt, OBJ_setAttr_T2cleartxt); // NID: 633
        addObject(NID_setAttr_TokICCsig, SN_setAttr_TokICCsig, LN_setAttr_TokICCsig, OBJ_setAttr_TokICCsig); // NID: 634
        addObject(NID_setAttr_SecDevSig, SN_setAttr_SecDevSig, LN_setAttr_SecDevSig, OBJ_setAttr_SecDevSig); // NID: 635
        addObject(NID_set_brand_IATA_ATA, SN_set_brand_IATA_ATA, null, OBJ_set_brand_IATA_ATA); // NID: 636
        addObject(NID_set_brand_Diners, SN_set_brand_Diners, null, OBJ_set_brand_Diners); // NID: 637
        addObject(NID_set_brand_AmericanExpress, SN_set_brand_AmericanExpress, null, OBJ_set_brand_AmericanExpress); // NID: 638
        addObject(NID_set_brand_JCB, SN_set_brand_JCB, null, OBJ_set_brand_JCB); // NID: 639
        addObject(NID_set_brand_Visa, SN_set_brand_Visa, null, OBJ_set_brand_Visa); // NID: 640
        addObject(NID_set_brand_MasterCard, SN_set_brand_MasterCard, null, OBJ_set_brand_MasterCard); // NID: 641
        addObject(NID_set_brand_Novus, SN_set_brand_Novus, null, OBJ_set_brand_Novus); // NID: 642
        addObject(NID_des_cdmf, SN_des_cdmf, LN_des_cdmf, OBJ_des_cdmf); // NID: 643
        addObject(NID_rsaOAEPEncryptionSET, SN_rsaOAEPEncryptionSET, null, OBJ_rsaOAEPEncryptionSET); // NID: 644
        //addObject(NID_itu_t, SN_itu_t, LN_itu_t, OBJ_itu_t); // NID: 645
        //addObject(NID_joint_iso_itu_t, SN_joint_iso_itu_t, LN_joint_iso_itu_t, OBJ_joint_iso_itu_t); // NID: 646
        addObject(NID_international_organizations, SN_international_organizations, LN_international_organizations, OBJ_international_organizations); // NID: 647
        addObject(NID_ms_smartcard_login, SN_ms_smartcard_login, LN_ms_smartcard_login, OBJ_ms_smartcard_login); // NID: 648
        addObject(NID_ms_upn, SN_ms_upn, LN_ms_upn, OBJ_ms_upn); // NID: 649
        //addObject(NID_aes_128_cfb1, SN_aes_128_cfb1, LN_aes_128_cfb1, null); // NID: 650
        //addObject(NID_aes_192_cfb1, SN_aes_192_cfb1, LN_aes_192_cfb1, null); // NID: 651
        //addObject(NID_aes_256_cfb1, SN_aes_256_cfb1, LN_aes_256_cfb1, null); // NID: 652
        //addObject(NID_aes_128_cfb8, SN_aes_128_cfb8, LN_aes_128_cfb8, null); // NID: 653
        //addObject(NID_aes_192_cfb8, SN_aes_192_cfb8, LN_aes_192_cfb8, null); // NID: 654
        //addObject(NID_aes_256_cfb8, SN_aes_256_cfb8, LN_aes_256_cfb8, null); // NID: 655
        //addObject(NID_des_cfb1, SN_des_cfb1, LN_des_cfb1, null); // NID: 656
        //addObject(NID_des_cfb8, SN_des_cfb8, LN_des_cfb8, null); // NID: 657
        //addObject(NID_des_ede3_cfb1, SN_des_ede3_cfb1, LN_des_ede3_cfb1, null); // NID: 658
        //addObject(NID_des_ede3_cfb8, SN_des_ede3_cfb8, LN_des_ede3_cfb8, null); // NID: 659
        addObject(NID_streetAddress, null, LN_streetAddress, OBJ_streetAddress); // NID: 660
        addObject(NID_postalCode, null, LN_postalCode, OBJ_postalCode); // NID: 661
        addObject(NID_id_ppl, SN_id_ppl, null, OBJ_id_ppl); // NID: 662
        addObject(NID_proxyCertInfo, SN_proxyCertInfo, LN_proxyCertInfo, OBJ_proxyCertInfo); // NID: 663
        addObject(NID_id_ppl_anyLanguage, SN_id_ppl_anyLanguage, LN_id_ppl_anyLanguage, OBJ_id_ppl_anyLanguage); // NID: 664
        addObject(NID_id_ppl_inheritAll, SN_id_ppl_inheritAll, LN_id_ppl_inheritAll, OBJ_id_ppl_inheritAll); // NID: 665
        addObject(NID_name_constraints, SN_name_constraints, LN_name_constraints, OBJ_name_constraints); // NID: 666
        addObject(NID_Independent, SN_Independent, LN_Independent, OBJ_Independent); // NID: 667
        addObject(NID_sha256WithRSAEncryption, SN_sha256WithRSAEncryption, LN_sha256WithRSAEncryption, OBJ_sha256WithRSAEncryption); // NID: 668
        addObject(NID_sha384WithRSAEncryption, SN_sha384WithRSAEncryption, LN_sha384WithRSAEncryption, OBJ_sha384WithRSAEncryption); // NID: 669
        addObject(NID_sha512WithRSAEncryption, SN_sha512WithRSAEncryption, LN_sha512WithRSAEncryption, OBJ_sha512WithRSAEncryption); // NID: 670
        addObject(NID_sha224WithRSAEncryption, SN_sha224WithRSAEncryption, LN_sha224WithRSAEncryption, OBJ_sha224WithRSAEncryption); // NID: 671
        addObject(NID_sha256, SN_sha256, LN_sha256, OBJ_sha256); // NID: 672
        addObject(NID_sha384, SN_sha384, LN_sha384, OBJ_sha384); // NID: 673
        addObject(NID_sha512, SN_sha512, LN_sha512, OBJ_sha512); // NID: 674
        addObject(NID_sha224, SN_sha224, LN_sha224, OBJ_sha224); // NID: 675
        addObject(NID_identified_organization, SN_identified_organization, null, OBJ_identified_organization); // NID: 676
        addObject(NID_certicom_arc, SN_certicom_arc, null, OBJ_certicom_arc); // NID: 677
        addObject(NID_wap, SN_wap, null, OBJ_wap); // NID: 678
        addObject(NID_wap_wsg, SN_wap_wsg, null, OBJ_wap_wsg); // NID: 679
        addObject(NID_X9_62_id_characteristic_two_basis, SN_X9_62_id_characteristic_two_basis, null, OBJ_X9_62_id_characteristic_two_basis); // NID: 680
        addObject(NID_X9_62_onBasis, SN_X9_62_onBasis, null, OBJ_X9_62_onBasis); // NID: 681
        addObject(NID_X9_62_tpBasis, SN_X9_62_tpBasis, null, OBJ_X9_62_tpBasis); // NID: 682
        addObject(NID_X9_62_ppBasis, SN_X9_62_ppBasis, null, OBJ_X9_62_ppBasis); // NID: 683
        addObject(NID_X9_62_c2pnb163v1, SN_X9_62_c2pnb163v1, null, OBJ_X9_62_c2pnb163v1); // NID: 684
        addObject(NID_X9_62_c2pnb163v2, SN_X9_62_c2pnb163v2, null, OBJ_X9_62_c2pnb163v2); // NID: 685
        addObject(NID_X9_62_c2pnb163v3, SN_X9_62_c2pnb163v3, null, OBJ_X9_62_c2pnb163v3); // NID: 686
        addObject(NID_X9_62_c2pnb176v1, SN_X9_62_c2pnb176v1, null, OBJ_X9_62_c2pnb176v1); // NID: 687
        addObject(NID_X9_62_c2tnb191v1, SN_X9_62_c2tnb191v1, null, OBJ_X9_62_c2tnb191v1); // NID: 688
        addObject(NID_X9_62_c2tnb191v2, SN_X9_62_c2tnb191v2, null, OBJ_X9_62_c2tnb191v2); // NID: 689
        addObject(NID_X9_62_c2tnb191v3, SN_X9_62_c2tnb191v3, null, OBJ_X9_62_c2tnb191v3); // NID: 690
        addObject(NID_X9_62_c2onb191v4, SN_X9_62_c2onb191v4, null, OBJ_X9_62_c2onb191v4); // NID: 691
        addObject(NID_X9_62_c2onb191v5, SN_X9_62_c2onb191v5, null, OBJ_X9_62_c2onb191v5); // NID: 692
        addObject(NID_X9_62_c2pnb208w1, SN_X9_62_c2pnb208w1, null, OBJ_X9_62_c2pnb208w1); // NID: 693
        addObject(NID_X9_62_c2tnb239v1, SN_X9_62_c2tnb239v1, null, OBJ_X9_62_c2tnb239v1); // NID: 694
        addObject(NID_X9_62_c2tnb239v2, SN_X9_62_c2tnb239v2, null, OBJ_X9_62_c2tnb239v2); // NID: 695
        addObject(NID_X9_62_c2tnb239v3, SN_X9_62_c2tnb239v3, null, OBJ_X9_62_c2tnb239v3); // NID: 696
        addObject(NID_X9_62_c2onb239v4, SN_X9_62_c2onb239v4, null, OBJ_X9_62_c2onb239v4); // NID: 697
        addObject(NID_X9_62_c2onb239v5, SN_X9_62_c2onb239v5, null, OBJ_X9_62_c2onb239v5); // NID: 698
        addObject(NID_X9_62_c2pnb272w1, SN_X9_62_c2pnb272w1, null, OBJ_X9_62_c2pnb272w1); // NID: 699
        addObject(NID_X9_62_c2pnb304w1, SN_X9_62_c2pnb304w1, null, OBJ_X9_62_c2pnb304w1); // NID: 700
        addObject(NID_X9_62_c2tnb359v1, SN_X9_62_c2tnb359v1, null, OBJ_X9_62_c2tnb359v1); // NID: 701
        addObject(NID_X9_62_c2pnb368w1, SN_X9_62_c2pnb368w1, null, OBJ_X9_62_c2pnb368w1); // NID: 702
        addObject(NID_X9_62_c2tnb431r1, SN_X9_62_c2tnb431r1, null, OBJ_X9_62_c2tnb431r1); // NID: 703
        addObject(NID_secp112r1, SN_secp112r1, null, OBJ_secp112r1); // NID: 704
        addObject(NID_secp112r2, SN_secp112r2, null, OBJ_secp112r2); // NID: 705
        addObject(NID_secp128r1, SN_secp128r1, null, OBJ_secp128r1); // NID: 706
        addObject(NID_secp128r2, SN_secp128r2, null, OBJ_secp128r2); // NID: 707
        addObject(NID_secp160k1, SN_secp160k1, null, OBJ_secp160k1); // NID: 708
        addObject(NID_secp160r1, SN_secp160r1, null, OBJ_secp160r1); // NID: 709
        addObject(NID_secp160r2, SN_secp160r2, null, OBJ_secp160r2); // NID: 710
        addObject(NID_secp192k1, SN_secp192k1, null, OBJ_secp192k1); // NID: 711
        addObject(NID_secp224k1, SN_secp224k1, null, OBJ_secp224k1); // NID: 712
        addObject(NID_secp224r1, SN_secp224r1, null, OBJ_secp224r1); // NID: 713
        addObject(NID_secp256k1, SN_secp256k1, null, OBJ_secp256k1); // NID: 714
        addObject(NID_secp384r1, SN_secp384r1, null, OBJ_secp384r1); // NID: 715
        addObject(NID_secp521r1, SN_secp521r1, null, OBJ_secp521r1); // NID: 716
        addObject(NID_sect113r1, SN_sect113r1, null, OBJ_sect113r1); // NID: 717
        addObject(NID_sect113r2, SN_sect113r2, null, OBJ_sect113r2); // NID: 718
        addObject(NID_sect131r1, SN_sect131r1, null, OBJ_sect131r1); // NID: 719
        addObject(NID_sect131r2, SN_sect131r2, null, OBJ_sect131r2); // NID: 720
        addObject(NID_sect163k1, SN_sect163k1, null, OBJ_sect163k1); // NID: 721
        addObject(NID_sect163r1, SN_sect163r1, null, OBJ_sect163r1); // NID: 722
        addObject(NID_sect163r2, SN_sect163r2, null, OBJ_sect163r2); // NID: 723
        addObject(NID_sect193r1, SN_sect193r1, null, OBJ_sect193r1); // NID: 724
        addObject(NID_sect193r2, SN_sect193r2, null, OBJ_sect193r2); // NID: 725
        addObject(NID_sect233k1, SN_sect233k1, null, OBJ_sect233k1); // NID: 726
        addObject(NID_sect233r1, SN_sect233r1, null, OBJ_sect233r1); // NID: 727
        addObject(NID_sect239k1, SN_sect239k1, null, OBJ_sect239k1); // NID: 728
        addObject(NID_sect283k1, SN_sect283k1, null, OBJ_sect283k1); // NID: 729
        addObject(NID_sect283r1, SN_sect283r1, null, OBJ_sect283r1); // NID: 730
        addObject(NID_sect409k1, SN_sect409k1, null, OBJ_sect409k1); // NID: 731
        addObject(NID_sect409r1, SN_sect409r1, null, OBJ_sect409r1); // NID: 732
        addObject(NID_sect571k1, SN_sect571k1, null, OBJ_sect571k1); // NID: 733
        addObject(NID_sect571r1, SN_sect571r1, null, OBJ_sect571r1); // NID: 734
        addObject(NID_wap_wsg_idm_ecid_wtls1, SN_wap_wsg_idm_ecid_wtls1, null, OBJ_wap_wsg_idm_ecid_wtls1); // NID: 735
        addObject(NID_wap_wsg_idm_ecid_wtls3, SN_wap_wsg_idm_ecid_wtls3, null, OBJ_wap_wsg_idm_ecid_wtls3); // NID: 736
        addObject(NID_wap_wsg_idm_ecid_wtls4, SN_wap_wsg_idm_ecid_wtls4, null, OBJ_wap_wsg_idm_ecid_wtls4); // NID: 737
        addObject(NID_wap_wsg_idm_ecid_wtls5, SN_wap_wsg_idm_ecid_wtls5, null, OBJ_wap_wsg_idm_ecid_wtls5); // NID: 738
        addObject(NID_wap_wsg_idm_ecid_wtls6, SN_wap_wsg_idm_ecid_wtls6, null, OBJ_wap_wsg_idm_ecid_wtls6); // NID: 739
        addObject(NID_wap_wsg_idm_ecid_wtls7, SN_wap_wsg_idm_ecid_wtls7, null, OBJ_wap_wsg_idm_ecid_wtls7); // NID: 740
        addObject(NID_wap_wsg_idm_ecid_wtls8, SN_wap_wsg_idm_ecid_wtls8, null, OBJ_wap_wsg_idm_ecid_wtls8); // NID: 741
        addObject(NID_wap_wsg_idm_ecid_wtls9, SN_wap_wsg_idm_ecid_wtls9, null, OBJ_wap_wsg_idm_ecid_wtls9); // NID: 742
        addObject(NID_wap_wsg_idm_ecid_wtls10, SN_wap_wsg_idm_ecid_wtls10, null, OBJ_wap_wsg_idm_ecid_wtls10); // NID: 743
        addObject(NID_wap_wsg_idm_ecid_wtls11, SN_wap_wsg_idm_ecid_wtls11, null, OBJ_wap_wsg_idm_ecid_wtls11); // NID: 744
        addObject(NID_wap_wsg_idm_ecid_wtls12, SN_wap_wsg_idm_ecid_wtls12, null, OBJ_wap_wsg_idm_ecid_wtls12); // NID: 745
        addObject(NID_any_policy, SN_any_policy, LN_any_policy, OBJ_any_policy); // NID: 746
        addObject(NID_policy_mappings, SN_policy_mappings, LN_policy_mappings, OBJ_policy_mappings); // NID: 747
        addObject(NID_inhibit_any_policy, SN_inhibit_any_policy, LN_inhibit_any_policy, OBJ_inhibit_any_policy); // NID: 748
        //addObject(NID_ipsec3, SN_ipsec3, LN_ipsec3, null); // NID: 749
        //addObject(NID_ipsec4, SN_ipsec4, LN_ipsec4, null); // NID: 750
        addObject(NID_dsa_with_SHA224, SN_dsa_with_SHA224, null, OBJ_dsa_with_SHA224); // NID: 802
        addObject(NID_dsa_with_SHA256, SN_dsa_with_SHA256, null, OBJ_dsa_with_SHA256); // NID: 803
    }

//     public final static String SN_undef = "UNDEF";
//     public final static String LN_undef = "undefined";
//     public final static int NID_undef = 0;
//     public final static String OBJ_undef = "0";

//     public final static String SN_Algorithm = "Algorithm";
//     public final static String LN_algorithm = "algorithm";
//     public final static int NID_algorithm = 38;
//     public final static String OBJ_algorithm = "1.3.14.3.2";

//     public final static String LN_rsadsi = "rsadsi";
//     public final static int NID_rsadsi = 1;
//     public final static String OBJ_rsadsi = "1.2.840.113549";

//     public final static String LN_pkcs = "pkcs";
//     public final static int NID_pkcs = 2;
//     public final static String OBJ_pkcs = OBJ_rsadsi+".1";

//     public final static String SN_md2 = "MD2";
//     public final static String LN_md2 = "md2";
//     public final static int NID_md2 = 3;
//     public final static String OBJ_md2 = OBJ_rsadsi+".2.2";

//     public final static String SN_md5 = "MD5";
//     public final static String LN_md5 = "md5";
//     public final static int NID_md5 = 4;
//     public final static String OBJ_md5 = OBJ_rsadsi+".2.5";

//     public final static String SN_rc4 = "RC4";
//     public final static String LN_rc4 = "rc4";
//     public final static int NID_rc4 = 5;
//     public final static String OBJ_rc4 = OBJ_rsadsi+".3.4";

//     public final static String LN_rsaEncryption = "rsaEncryption";
//     public final static int NID_rsaEncryption = 6;
//     public final static String OBJ_rsaEncryption = OBJ_pkcs+".1.1";

//     public final static String SN_md2WithRSAEncryption = "RSA-MD2";
//     public final static String LN_md2WithRSAEncryption = "md2WithRSAEncryption";
//     public final static int NID_md2WithRSAEncryption = 7;
//     public final static String OBJ_md2WithRSAEncryption = OBJ_pkcs+".1.2";

//     public final static String SN_md5WithRSAEncryption = "RSA-MD5";
//     public final static String LN_md5WithRSAEncryption = "md5WithRSAEncryption";
//     public final static int NID_md5WithRSAEncryption = 8;
//     public final static String OBJ_md5WithRSAEncryption = OBJ_pkcs+".1.4";

//     public final static String SN_pbeWithMD2AndDES_CBC = "PBE-MD2-DES";
//     public final static String LN_pbeWithMD2AndDES_CBC = "pbeWithMD2AndDES-CBC";
//     public final static int NID_pbeWithMD2AndDES_CBC = 9;
//     public final static String OBJ_pbeWithMD2AndDES_CBC = OBJ_pkcs+".5.1";

//     public final static String SN_pbeWithMD5AndDES_CBC = "PBE-MD5-DES";
//     public final static String LN_pbeWithMD5AndDES_CBC = "pbeWithMD5AndDES-CBC";
//     public final static int NID_pbeWithMD5AndDES_CBC = 10;
//     public final static String OBJ_pbeWithMD5AndDES_CBC = OBJ_pkcs+".5.3";

//     public final static String LN_X500 = "X500";
//     public final static int NID_X500 = 11;
//     public final static String OBJ_X500 = "2.5";

//     public final static String LN_X509 = "X509";
//     public final static int NID_X509 = 12;
//     public final static String OBJ_X509 = OBJ_X500+".4";

//     public final static String SN_commonName = "CN";
//     public final static String LN_commonName = "commonName";
//     public final static int NID_commonName = 13;
//     public final static String OBJ_commonName = OBJ_X509+".3";

//     public final static String SN_countryName = "C";
//     public final static String LN_countryName = "countryName";
//     public final static int NID_countryName = 14;
//     public final static String OBJ_countryName = OBJ_X509+".6";

//     public final static String SN_localityName = "";
//     public final static String LN_localityName = "localityName";
//     public final static int NID_localityName = 15;
//     public final static String OBJ_localityName = OBJ_X509+".7";

//     public final static String SN_stateOrProvinceName = "ST";
//     public final static String LN_stateOrProvinceName = "stateOrProvinceName";
//     public final static int NID_stateOrProvinceName = 16;
//     public final static String OBJ_stateOrProvinceName = OBJ_X509+".8";

//     public final static String SN_organizationName = "O";
//     public final static String LN_organizationName = "organizationName";
//     public final static int NID_organizationName = 17;
//     public final static String OBJ_organizationName = OBJ_X509+".10";

//     public final static String SN_organizationalUnitName = "OU";
//     public final static String LN_organizationalUnitName = "organizationalUnitName";
//     public final static int NID_organizationalUnitName = 18;
//     public final static String OBJ_organizationalUnitName = OBJ_X509+".11";

//     public final static String SN_rsa = "RSA";
//     public final static String LN_rsa = "rsa";
//     public final static int NID_rsa = 19;
//     public final static String OBJ_rsa = OBJ_X500+".8.1.1";

//     public final static String LN_pkcs7 = "pkcs7";
//     public final static int NID_pkcs7 = 20;
//     public final static String OBJ_pkcs7 = OBJ_pkcs+".7";

//     public final static String LN_pkcs7_data = "pkcs7-data";
//     public final static int NID_pkcs7_data = 21;
//     public final static String OBJ_pkcs7_data = OBJ_pkcs7+".1";

//     public final static String LN_pkcs7_signed = "pkcs7-signedData";
//     public final static int NID_pkcs7_signed = 22;
//     public final static String OBJ_pkcs7_signed = OBJ_pkcs7+".2";

//     public final static String LN_pkcs7_enveloped = "pkcs7-envelopedData";
//     public final static int NID_pkcs7_enveloped = 23;
//     public final static String OBJ_pkcs7_enveloped = OBJ_pkcs7+".3";

//     public final static String LN_pkcs7_signedAndEnveloped = "pkcs7-signedAndEnvelopedData";
//     public final static int NID_pkcs7_signedAndEnveloped = 24;
//     public final static String OBJ_pkcs7_signedAndEnveloped = OBJ_pkcs7+".4";

//     public final static String LN_pkcs7_digest = "pkcs7-digestData";
//     public final static int NID_pkcs7_digest = 25;
//     public final static String OBJ_pkcs7_digest = OBJ_pkcs7+".5";

//     public final static String LN_pkcs7_encrypted = "pkcs7-encryptedData";
//     public final static int NID_pkcs7_encrypted = 26;
//     public final static String OBJ_pkcs7_encrypted = OBJ_pkcs7+".6";

//     public final static String LN_pkcs3 = "pkcs3";
//     public final static int NID_pkcs3 = 27;
//     public final static String OBJ_pkcs3 = OBJ_pkcs+".3";

//     public final static String LN_dhKeyAgreement = "dhKeyAgreement";
//     public final static int NID_dhKeyAgreement = 28;
//     public final static String OBJ_dhKeyAgreement = OBJ_pkcs3+".1";

//     public final static String SN_des_ecb = "DES-ECB";
//     public final static String LN_des_ecb = "des-ecb";
//     public final static int NID_des_ecb = 29;
//     public final static String OBJ_des_ecb = OBJ_algorithm+".6";

//     public final static String SN_des_cfb64 = "DES-CFB";
//     public final static String LN_des_cfb64 = "des-cfb";
//     public final static int NID_des_cfb64 = 30;
//     public final static String OBJ_des_cfb64 = OBJ_algorithm+".9";

//     public final static String SN_des_cbc = "DES-CBC";
//     public final static String LN_des_cbc = "des-cbc";
//     public final static int NID_des_cbc = 31;
//     public final static String OBJ_des_cbc = OBJ_algorithm+".7";

//     public final static String SN_des_ede = "DES-EDE";
//     public final static String LN_des_ede = "des-ede";
//     public final static int NID_des_ede = 32;
//     public final static String OBJ_des_ede = OBJ_algorithm+".17";

//     public final static String SN_des_ede3 = "DES-EDE3";
//     public final static String LN_des_ede3 = "des-ede3";
//     public final static int NID_des_ede3 = 33;

//     public final static String SN_idea_cbc = "IDEA-CBC";
//     public final static String LN_idea_cbc = "idea-cbc";
//     public final static int NID_idea_cbc = 34;
//     public final static String OBJ_idea_cbc = "1.3.6.1.4.1.188.7.1.1.2";

//     public final static String SN_idea_cfb64 = "IDEA-CFB";
//     public final static String LN_idea_cfb64 = "idea-cfb";
//     public final static int NID_idea_cfb64 = 35;

//     public final static String SN_idea_ecb = "IDEA-ECB";
//     public final static String LN_idea_ecb = "idea-ecb";
//     public final static int NID_idea_ecb = 36;

//     public final static String SN_rc2_cbc = "RC2-CBC";
//     public final static String LN_rc2_cbc = "rc2-cbc";
//     public final static int NID_rc2_cbc = 37;
//     public final static String OBJ_rc2_cbc = OBJ_rsadsi+".3.2";

//     public final static String SN_rc2_ecb = "RC2-ECB";
//     public final static String LN_rc2_ecb = "rc2-ecb";
//     public final static int NID_rc2_ecb = 38;

//     public final static String SN_rc2_cfb64 = "RC2-CFB";
//     public final static String LN_rc2_cfb64 = "rc2-cfb";
//     public final static int NID_rc2_cfb64 = 39;

//     public final static String SN_rc2_ofb64 = "RC2-OFB";
//     public final static String LN_rc2_ofb64 = "rc2-ofb";
//     public final static int NID_rc2_ofb64 = 40;

//     public final static String SN_sha = "SHA";
//     public final static String LN_sha = "sha";
//     public final static int NID_sha = 41;
//     public final static String OBJ_sha = OBJ_algorithm+".18";

//     public final static String SN_shaWithRSAEncryption = "RSA-SHA";
//     public final static String LN_shaWithRSAEncryption = "shaWithRSAEncryption";
//     public final static int NID_shaWithRSAEncryption = 42;
//     public final static String OBJ_shaWithRSAEncryption = OBJ_algorithm+".15";

//     public final static String SN_des_ede_cbc = "DES-EDE-CBC";
//     public final static String LN_des_ede_cbc = "des-ede-cbc";
//     public final static int NID_des_ede_cbc = 43;

//     public final static String SN_des_ede3_cbc = "DES-EDE3-CBC";
//     public final static String LN_des_ede3_cbc = "des-ede3-cbc";
//     public final static int NID_des_ede3_cbc = 44;
//     public final static String OBJ_des_ede3_cbc = OBJ_rsadsi+".3.7";

//     public final static String SN_des_ofb64 = "DES-OFB";
//     public final static String LN_des_ofb64 = "des-ofb";
//     public final static int NID_des_ofb64 = 45;
//     public final static String OBJ_des_ofb64 = OBJ_algorithm+".8";

//     public final static String SN_idea_ofb64 = "IDEA-OFB";
//     public final static String LN_idea_ofb64 = "idea-ofb";
//     public final static int NID_idea_ofb64 = 46;

//     public final static String LN_pkcs9 = "pkcs9";
//     public final static int NID_pkcs9 = 47;
//     public final static String OBJ_pkcs9 = OBJ_pkcs+".9";

//     public final static String SN_pkcs9_emailAddress = "Email";
//     public final static String LN_pkcs9_emailAddress = "emailAddress";
//     public final static int NID_pkcs9_emailAddress = 48;
//     public final static String OBJ_pkcs9_emailAddress = OBJ_pkcs9+".1";

//     public final static String LN_pkcs9_unstructuredName = "unstructuredName";
//     public final static int NID_pkcs9_unstructuredName = 49;
//     public final static String OBJ_pkcs9_unstructuredName = OBJ_pkcs9+".2";

//     public final static String LN_pkcs9_contentType = "contentType";
//     public final static int NID_pkcs9_contentType = 50;
//     public final static String OBJ_pkcs9_contentType = OBJ_pkcs9+".3";

//     public final static String LN_pkcs9_messageDigest = "messageDigest";
//     public final static int NID_pkcs9_messageDigest = 51;
//     public final static String OBJ_pkcs9_messageDigest = OBJ_pkcs9+".4";

//     public final static String LN_pkcs9_signingTime = "signingTime";
//     public final static int NID_pkcs9_signingTime = 52;
//     public final static String OBJ_pkcs9_signingTime = OBJ_pkcs9+".5";

//     public final static String LN_pkcs9_countersignature = "countersignature";
//     public final static int NID_pkcs9_countersignature = 53;
//     public final static String OBJ_pkcs9_countersignature = OBJ_pkcs9+".6";

//     public final static String LN_pkcs9_challengePassword = "challengePassword";
//     public final static int NID_pkcs9_challengePassword = 54;
//     public final static String OBJ_pkcs9_challengePassword = OBJ_pkcs9+".7";

//     public final static String LN_pkcs9_unstructuredAddress = "unstructuredAddress";
//     public final static int NID_pkcs9_unstructuredAddress = 55;
//     public final static String OBJ_pkcs9_unstructuredAddress = OBJ_pkcs9+".8";

//     public final static String LN_pkcs9_extCertAttributes = "extendedCertificateAttributes";
//     public final static int NID_pkcs9_extCertAttributes = 56;
//     public final static String OBJ_pkcs9_extCertAttributes = OBJ_pkcs9+".9";

//     public final static String SN_netscape = "Netscape";
//     public final static String LN_netscape = "Netscape Communications Corp.";
//     public final static int NID_netscape = 57;
//     public final static String OBJ_netscape = "2.16.840.1.113730";

//     public final static String SN_netscape_cert_extension = "nsCertExt";
//     public final static String LN_netscape_cert_extension = "Netscape Certificate Extension";
//     public final static int NID_netscape_cert_extension = 58;
//     public final static String OBJ_netscape_cert_extension = OBJ_netscape+".1";

//     public final static String SN_netscape_data_type = "nsDataType";
//     public final static String LN_netscape_data_type = "Netscape Data Type";
//     public final static int NID_netscape_data_type = 59;
//     public final static String OBJ_netscape_data_type = OBJ_netscape+".2";

//     public final static String SN_des_ede_cfb64 = "DES-EDE-CFB";
//     public final static String LN_des_ede_cfb64 = "des-ede-cfb";
//     public final static int NID_des_ede_cfb64 = 60;

//     public final static String SN_des_ede3_cfb64 = "DES-EDE3-CFB";
//     public final static String LN_des_ede3_cfb64 = "des-ede3-cfb";
//     public final static int NID_des_ede3_cfb64 = 61;

//     public final static String SN_des_ede_ofb64 = "DES-EDE-OFB";
//     public final static String LN_des_ede_ofb64 = "des-ede-ofb";
//     public final static int NID_des_ede_ofb64 = 62;

//     public final static String SN_des_ede3_ofb64 = "DES-EDE3-OFB";
//     public final static String LN_des_ede3_ofb64 = "des-ede3-ofb";
//     public final static int NID_des_ede3_ofb64 = 63;

//     public final static String SN_sha1 = "SHA1";
//     public final static String LN_sha1 = "sha1";
//     public final static int NID_sha1 = 64;
//     public final static String OBJ_sha1 = OBJ_algorithm+".26";

//     public final static String SN_sha1WithRSAEncryption = "RSA-SHA1";
//     public final static String LN_sha1WithRSAEncryption = "sha1WithRSAEncryption";
//     public final static int NID_sha1WithRSAEncryption = 65;
//     public final static String OBJ_sha1WithRSAEncryption = OBJ_pkcs+".1.5";

//     public final static String SN_dsaWithSHA = "DSA-SHA";
//     public final static String LN_dsaWithSHA = "dsaWithSHA";
//     public final static int NID_dsaWithSHA = 66;
//     public final static String OBJ_dsaWithSHA = OBJ_algorithm+".13";

//     public final static String SN_dsa_2 = "DSA-old";
//     public final static String LN_dsa_2 = "dsaEncryption-old";
//     public final static int NID_dsa_2 = 67;
//     public final static String OBJ_dsa_2 = OBJ_algorithm+".12";

//     public final static String SN_pbeWithSHA1AndRC2_CBC = "PBE-SHA1-RC2-64";
//     public final static String LN_pbeWithSHA1AndRC2_CBC = "pbeWithSHA1AndRC2-CBC";
//     public final static int NID_pbeWithSHA1AndRC2_CBC = 68;
//     public final static String OBJ_pbeWithSHA1AndRC2_CBC = OBJ_pkcs+".5.11L ";

//     public final static String LN_id_pbkdf2 = "PBKDF2";
//     public final static int NID_id_pbkdf2 = 69;
//     public final static String OBJ_id_pbkdf2 = OBJ_pkcs+".5.12L ";

//     public final static String SN_dsaWithSHA1_2 = "DSA-SHA1-old";
//     public final static String LN_dsaWithSHA1_2 = "dsaWithSHA1-old";
//     public final static int NID_dsaWithSHA1_2 = 70;
//     public final static String OBJ_dsaWithSHA1_2 = OBJ_algorithm+".27";

//     public final static String SN_netscape_cert_type = "nsCertType";
//     public final static String LN_netscape_cert_type = "Netscape Cert Type";
//     public final static int NID_netscape_cert_type = 71;
//     public final static String OBJ_netscape_cert_type = OBJ_netscape_cert_extension+".1";

//     public final static String SN_netscape_base_url = "nsBaseUrl";
//     public final static String LN_netscape_base_url = "Netscape Base Url";
//     public final static int NID_netscape_base_url = 72;
//     public final static String OBJ_netscape_base_url = OBJ_netscape_cert_extension+".2";

//     public final static String SN_netscape_revocation_url = "nsRevocationUrl";
//     public final static String LN_netscape_revocation_url = "Netscape Revocation Url";
//     public final static int NID_netscape_revocation_url = 73;
//     public final static String OBJ_netscape_revocation_url = OBJ_netscape_cert_extension+".3";

//     public final static String SN_netscape_ca_revocation_url = "nsCaRevocationUrl";
//     public final static String LN_netscape_ca_revocation_url = "Netscape CA Revocation Url";
//     public final static int NID_netscape_ca_revocation_url = 74;
//     public final static String OBJ_netscape_ca_revocation_url = OBJ_netscape_cert_extension+".4";

//     public final static String SN_netscape_renewal_url = "nsRenewalUrl";
//     public final static String LN_netscape_renewal_url = "Netscape Renewal Url";
//     public final static int NID_netscape_renewal_url = 75;
//     public final static String OBJ_netscape_renewal_url = OBJ_netscape_cert_extension+".7";

//     public final static String SN_netscape_ca_policy_url = "nsCaPolicyUrl";
//     public final static String LN_netscape_ca_policy_url = "Netscape CA Policy Url";
//     public final static int NID_netscape_ca_policy_url = 76;
//     public final static String OBJ_netscape_ca_policy_url = OBJ_netscape_cert_extension+".8";

//     public final static String SN_netscape_ssl_server_name = "nsSslServerName";
//     public final static String LN_netscape_ssl_server_name = "Netscape SSL Server Name";
//     public final static int NID_netscape_ssl_server_name = 77;
//     public final static String OBJ_netscape_ssl_server_name = OBJ_netscape_cert_extension+".12";

//     public final static String SN_netscape_comment = "nsComment";
//     public final static String LN_netscape_comment = "Netscape Comment";
//     public final static int NID_netscape_comment = 78;
//     public final static String OBJ_netscape_comment = OBJ_netscape_cert_extension+".13";

//     public final static String SN_netscape_cert_sequence = "nsCertSequence";
//     public final static String LN_netscape_cert_sequence = "Netscape Certificate Sequence";
//     public final static int NID_netscape_cert_sequence = 79;
//     public final static String OBJ_netscape_cert_sequence = OBJ_netscape_data_type+".5";

//     public final static String SN_desx_cbc = "DESX-CBC";
//     public final static String LN_desx_cbc = "desx-cbc";
//     public final static int NID_desx_cbc = 80;

//     public final static String SN_id_ce = "id-ce";
//     public final static int NID_id_ce = 81;
//     public final static String OBJ_id_ce = "2.5.29";

//     public final static String SN_subject_key_identifier = "subjectKeyIdentifier";
//     public final static String LN_subject_key_identifier = "X509v3 Subject Key Identifier";
//     public final static int NID_subject_key_identifier = 82;
//     public final static String OBJ_subject_key_identifier = OBJ_id_ce+".14";

//     public final static String SN_key_usage = "keyUsage";
//     public final static String LN_key_usage = "X509v3 Key Usage";
//     public final static int NID_key_usage = 83;
//     public final static String OBJ_key_usage = OBJ_id_ce+".15";

//     public final static String SN_private_key_usage_period = "privateKeyUsagePeriod";
//     public final static String LN_private_key_usage_period = "X509v3 Private Key Usage Period";
//     public final static int NID_private_key_usage_period = 84;
//     public final static String OBJ_private_key_usage_period = OBJ_id_ce+".16";

//     public final static String SN_subject_alt_name = "subjectAltName";
//     public final static String LN_subject_alt_name = "X509v3 Subject Alternative Name";
//     public final static int NID_subject_alt_name = 85;
//     public final static String OBJ_subject_alt_name = OBJ_id_ce+".17";

//     public final static String SN_issuer_alt_name = "issuerAltName";
//     public final static String LN_issuer_alt_name = "X509v3 Issuer Alternative Name";
//     public final static int NID_issuer_alt_name = 86;
//     public final static String OBJ_issuer_alt_name = OBJ_id_ce+".18";

//     public final static String SN_basic_constraints = "basicConstraints";
//     public final static String LN_basic_constraints = "X509v3 Basic Constraints";
//     public final static int NID_basic_constraints = 87;
//     public final static String OBJ_basic_constraints = OBJ_id_ce+".19";

//     public final static String SN_crl_number = "crlNumber";
//     public final static String LN_crl_number = "X509v3 CRL Number";
//     public final static int NID_crl_number = 88;
//     public final static String OBJ_crl_number = OBJ_id_ce+".20";

//     public final static String SN_certificate_policies = "certificatePolicies";
//     public final static String LN_certificate_policies = "X509v3 Certificate Policies";
//     public final static int NID_certificate_policies = 89;
//     public final static String OBJ_certificate_policies = OBJ_id_ce+".32";

//     public final static String SN_authority_key_identifier = "authorityKeyIdentifier";
//     public final static String LN_authority_key_identifier = "X509v3 Authority Key Identifier";
//     public final static int NID_authority_key_identifier = 90;
//     public final static String OBJ_authority_key_identifier = OBJ_id_ce+".35";

//     public final static String SN_bf_cbc = "BF-CBC";
//     public final static String LN_bf_cbc = "bf-cbc";
//     public final static int NID_bf_cbc = 91;
//     public final static String OBJ_bf_cbc = "1.3.6.1.4.1.3029.1.2";

//     public final static String SN_bf_ecb = "BF-ECB";
//     public final static String LN_bf_ecb = "bf-ecb";
//     public final static int NID_bf_ecb = 92;

//     public final static String SN_bf_cfb64 = "BF-CFB";
//     public final static String LN_bf_cfb64 = "bf-cfb";
//     public final static int NID_bf_cfb64 = 93;

//     public final static String SN_bf_ofb64 = "BF-OFB";
//     public final static String LN_bf_ofb64 = "bf-ofb";
//     public final static int NID_bf_ofb64 = 94;

//     public final static String SN_mdc2 = "MDC2";
//     public final static String LN_mdc2 = "mdc2";
//     public final static int NID_mdc2 = 95;
//     public final static String OBJ_mdc2 = "2.5.8.3.101";

//     public final static String SN_mdc2WithRSA = "RSA-MDC2";
//     public final static String LN_mdc2WithRSA = "mdc2withRSA";
//     public final static int NID_mdc2WithRSA = 96;
//     public final static String OBJ_mdc2WithRSA = "2.5.8.3.100";

//     public final static String SN_rc4_40 = "RC4-40";
//     public final static String LN_rc4_40 = "rc4-40";
//     public final static int NID_rc4_40 = 97;

//     public final static String SN_rc2_40_cbc = "RC2-40-CBC";
//     public final static String LN_rc2_40_cbc = "rc2-40-cbc";
//     public final static int NID_rc2_40_cbc = 98;

//     public final static String SN_givenName = "G";
//     public final static String LN_givenName = "givenName";
//     public final static int NID_givenName = 99;
//     public final static String OBJ_givenName = OBJ_X509+".42";

//     public final static String SN_surname = "S";
//     public final static String LN_surname = "surname";
//     public final static int NID_surname = 100;
//     public final static String OBJ_surname = OBJ_X509+".4";

//     public final static String SN_initials = "I";
//     public final static String LN_initials = "initials";
//     public final static int NID_initials = 101;
//     public final static String OBJ_initials = OBJ_X509+".43";

//     public final static String SN_uniqueIdentifier = "UID";
//     public final static String LN_uniqueIdentifier = "uniqueIdentifier";
//     public final static int NID_uniqueIdentifier = 102;
//     public final static String OBJ_uniqueIdentifier = OBJ_X509+".45";

//     public final static String SN_crl_distribution_points = "crlDistributionPoints";
//     public final static String LN_crl_distribution_points = "X509v3 CRL Distribution Points";
//     public final static int NID_crl_distribution_points = 103;
//     public final static String OBJ_crl_distribution_points = OBJ_id_ce+".31";

//     public final static String SN_md5WithRSA = "RSA-NP-MD5";
//     public final static String LN_md5WithRSA = "md5WithRSA";
//     public final static int NID_md5WithRSA = 104;
//     public final static String OBJ_md5WithRSA = OBJ_algorithm+".3";

//     public final static String SN_serialNumber = "SN";
//     public final static String LN_serialNumber = "serialNumber";
//     public final static int NID_serialNumber = 105;
//     public final static String OBJ_serialNumber = OBJ_X509+".5";

//     public final static String SN_title = "T";
//     public final static String LN_title = "title";
//     public final static int NID_title = 106;
//     public final static String OBJ_title = OBJ_X509+".12";

//     public final static String SN_description = "D";
//     public final static String LN_description = "description";
//     public final static int NID_description = 107;
//     public final static String OBJ_description = OBJ_X509+".13";

//     public final static String SN_cast5_cbc = "CAST5-CBC";
//     public final static String LN_cast5_cbc = "cast5-cbc";
//     public final static int NID_cast5_cbc = 108;
//     public final static String OBJ_cast5_cbc = "1.2.840.113533.7.66.10";

//     public final static String SN_cast5_ecb = "CAST5-ECB";
//     public final static String LN_cast5_ecb = "cast5-ecb";
//     public final static int NID_cast5_ecb = 109;

//     public final static String SN_cast5_cfb64 = "CAST5-CFB";
//     public final static String LN_cast5_cfb64 = "cast5-cfb";
//     public final static int NID_cast5_cfb64 = 110;

//     public final static String SN_cast5_ofb64 = "CAST5-OFB";
//     public final static String LN_cast5_ofb64 = "cast5-ofb";
//     public final static int NID_cast5_ofb64 = 111;

//     public final static String LN_pbeWithMD5AndCast5_CBC = "pbeWithMD5AndCast5CBC";
//     public final static int NID_pbeWithMD5AndCast5_CBC = 112;
//     public final static String OBJ_pbeWithMD5AndCast5_CBC = "1.2.840.113533.7.66.12";

//     public final static String SN_dsaWithSHA1 = "DSA-SHA1";
//     public final static String LN_dsaWithSHA1 = "dsaWithSHA1";
//     public final static int NID_dsaWithSHA1 = 113;
//     public final static String OBJ_dsaWithSHA1 = "1.2.840.10040.4.3";

//     public final static int NID_md5_sha1 = 114;
//     public final static String SN_md5_sha1 = "MD5-SHA1";
//     public final static String LN_md5_sha1 = "md5-sha1";

//     public final static String SN_sha1WithRSA = "RSA-SHA1-2";
//     public final static String LN_sha1WithRSA = "sha1WithRSA";
//     public final static int NID_sha1WithRSA = 115;
//     public final static String OBJ_sha1WithRSA = OBJ_algorithm+".29";

//     public final static String SN_dsa = "DSA";
//     public final static String LN_dsa = "dsaEncryption";
//     public final static int NID_dsa = 116;
//     public final static String OBJ_dsa = "1.2.840.10040.4.1";

//     public final static String SN_ripemd160 = "RIPEMD160";
//     public final static String LN_ripemd160 = "ripemd160";
//     public final static int NID_ripemd160 = 117;
//     public final static String OBJ_ripemd160 = "1.3.36.3.2.1";

//     public final static String SN_ripemd160WithRSA = "RSA-RIPEMD160";
//     public final static String LN_ripemd160WithRSA = "ripemd160WithRSA";
//     public final static int NID_ripemd160WithRSA = 119;
//     public final static String OBJ_ripemd160WithRSA = "1.3.36.3.3.1.2";

//     public final static String SN_rc5_cbc = "RC5-CBC";
//     public final static String LN_rc5_cbc = "rc5-cbc";
//     public final static int NID_rc5_cbc = 120;
//     public final static String OBJ_rc5_cbc = OBJ_rsadsi+".3.8";

//     public final static String SN_rc5_ecb = "RC5-ECB";
//     public final static String LN_rc5_ecb = "rc5-ecb";
//     public final static int NID_rc5_ecb = 121;

//     public final static String SN_rc5_cfb64 = "RC5-CFB";
//     public final static String LN_rc5_cfb64 = "rc5-cfb";
//     public final static int NID_rc5_cfb64 = 122;

//     public final static String SN_rc5_ofb64 = "RC5-OFB";
//     public final static String LN_rc5_ofb64 = "rc5-ofb";
//     public final static int NID_rc5_ofb64 = 123;

//     public final static String SN_rle_compression = "RLE";
//     public final static String LN_rle_compression = "run length compression";
//     public final static int NID_rle_compression = 124;
//     public final static String OBJ_rle_compression = "1.1.1.1.666.1";

//     public final static String SN_zlib_compression = "ZLIB";
//     public final static String LN_zlib_compression = "zlib compression";
//     public final static int NID_zlib_compression = 125;
//     public final static String OBJ_zlib_compression = "1.1.1.1.666.2";

//     public final static String SN_ext_key_usage = "extendedKeyUsage";
//     public final static String LN_ext_key_usage = "X509v3 Extended Key Usage";
//     public final static int NID_ext_key_usage = 126;
//     public final static String OBJ_ext_key_usage = OBJ_id_ce+".37";

//     public final static String SN_id_pkix = "PKIX";
//     public final static int NID_id_pkix = 127;
//     public final static String OBJ_id_pkix = "1.3.6.1.5.5.7";

//     public final static String SN_id_kp = "id-kp";
//     public final static int NID_id_kp = 128;
//     public final static String OBJ_id_kp = OBJ_id_pkix+".3";

//     public final static String SN_server_auth = "serverAuth";
//     public final static String LN_server_auth = "TLS Web Server Authentication";
//     public final static int NID_server_auth = 129;
//     public final static String OBJ_server_auth = OBJ_id_kp+".1";

//     public final static String SN_client_auth = "clientAuth";
//     public final static String LN_client_auth = "TLS Web Client Authentication";
//     public final static int NID_client_auth = 130;
//     public final static String OBJ_client_auth = OBJ_id_kp+".2";

//     public final static String SN_code_sign = "codeSigning";
//     public final static String LN_code_sign = "Code Signing";
//     public final static int NID_code_sign = 131;
//     public final static String OBJ_code_sign = OBJ_id_kp+".3";

//     public final static String SN_email_protect = "emailProtection";
//     public final static String LN_email_protect = "E-mail Protection";
//     public final static int NID_email_protect = 132;
//     public final static String OBJ_email_protect = OBJ_id_kp+".4";

//     public final static String SN_time_stamp = "timeStamping";
//     public final static String LN_time_stamp = "Time Stamping";
//     public final static int NID_time_stamp = 133;
//     public final static String OBJ_time_stamp = OBJ_id_kp+".8";

//     public final static String SN_ms_code_ind = "msCodeInd";
//     public final static String LN_ms_code_ind = "Microsoft Individual Code Signing";
//     public final static int NID_ms_code_ind = 134;
//     public final static String OBJ_ms_code_ind = "1.3.6.1.4.1.311.2.1.21";

//     public final static String SN_ms_code_com = "msCodeCom";
//     public final static String LN_ms_code_com = "Microsoft Commercial Code Signing";
//     public final static int NID_ms_code_com = 135;
//     public final static String OBJ_ms_code_com = "1.3.6.1.4.1.311.2.1.22";

//     public final static String SN_ms_ctl_sign = "msCTLSign";
//     public final static String LN_ms_ctl_sign = "Microsoft Trust List Signing";
//     public final static int NID_ms_ctl_sign = 136;
//     public final static String OBJ_ms_ctl_sign = "1.3.6.1.4.1.311.10.3.1";

//     public final static String SN_ms_sgc = "msSGC";
//     public final static String LN_ms_sgc = "Microsoft Server Gated Crypto";
//     public final static int NID_ms_sgc = 137;
//     public final static String OBJ_ms_sgc = "1.3.6.1.4.1.311.10.3.3";

//     public final static String SN_ms_efs = "msEFS";
//     public final static String LN_ms_efs = "Microsoft Encrypted File System";
//     public final static int NID_ms_efs = 138;
//     public final static String OBJ_ms_efs = "1.3.6.1.4.1.311.10.3.4";

//     public final static String SN_ns_sgc = "nsSGC";
//     public final static String LN_ns_sgc = "Netscape Server Gated Crypto";
//     public final static int NID_ns_sgc = 139;
//     public final static String OBJ_ns_sgc = OBJ_netscape+".4.1";

//     public final static String SN_delta_crl = "deltaCR";
//     public final static String LN_delta_crl = "X509v3 Delta CRL Indicator";
//     public final static int NID_delta_crl = 140;
//     public final static String OBJ_delta_crl = OBJ_id_ce+".27";

//     public final static String SN_crl_reason = "CRLReason";
//     public final static String LN_crl_reason = "CRL Reason Code";
//     public final static int NID_crl_reason = 141;
//     public final static String OBJ_crl_reason = OBJ_id_ce+".21";

//     public final static String SN_invalidity_date = "invalidityDate";
//     public final static String LN_invalidity_date = "Invalidity Date";
//     public final static int NID_invalidity_date = 142;
//     public final static String OBJ_invalidity_date = OBJ_id_ce+".24";

//     public final static String SN_sxnet = "SXNetID";
//     public final static String LN_sxnet = "Strong Extranet ID";
//     public final static int NID_sxnet = 143;
//     public final static String OBJ_sxnet = "1.3.101.1.4.1";

//     public final static String OBJ_pkcs12 = OBJ_pkcs+".12";
//     public final static String OBJ_pkcs12_pbeids = OBJ_pkcs12+".1";

//     public final static String SN_pbe_WithSHA1And128BitRC4 = "PBE-SHA1-RC4-128";
//     public final static String LN_pbe_WithSHA1And128BitRC4 = "pbeWithSHA1And128BitRC4";
//     public final static int NID_pbe_WithSHA1And128BitRC4 = 144;
//     public final static String OBJ_pbe_WithSHA1And128BitRC4 = OBJ_pkcs12_pbeids+".1";

//     public final static String SN_pbe_WithSHA1And40BitRC4 = "PBE-SHA1-RC4-40";
//     public final static String LN_pbe_WithSHA1And40BitRC4 = "pbeWithSHA1And40BitRC4";
//     public final static int NID_pbe_WithSHA1And40BitRC4 = 145;
//     public final static String OBJ_pbe_WithSHA1And40BitRC4 = OBJ_pkcs12_pbeids+".2";

//     public final static String SN_pbe_WithSHA1And3_Key_TripleDES_CBC = "PBE-SHA1-3DES";
//     public final static String LN_pbe_WithSHA1And3_Key_TripleDES_CBC = "pbeWithSHA1And3-KeyTripleDES-CBC";
//     public final static int NID_pbe_WithSHA1And3_Key_TripleDES_CBC = 146;
//     public final static String OBJ_pbe_WithSHA1And3_Key_TripleDES_CBC = OBJ_pkcs12_pbeids+".3";

//     public final static String SN_pbe_WithSHA1And2_Key_TripleDES_CBC = "PBE-SHA1-2DES";
//     public final static String LN_pbe_WithSHA1And2_Key_TripleDES_CBC = "pbeWithSHA1And2-KeyTripleDES-CBC";
//     public final static int NID_pbe_WithSHA1And2_Key_TripleDES_CBC = 147;
//     public final static String OBJ_pbe_WithSHA1And2_Key_TripleDES_CBC = OBJ_pkcs12_pbeids+".4";

//     public final static String SN_pbe_WithSHA1And128BitRC2_CBC = "PBE-SHA1-RC2-128";
//     public final static String LN_pbe_WithSHA1And128BitRC2_CBC = "pbeWithSHA1And128BitRC2-CBC";
//     public final static int NID_pbe_WithSHA1And128BitRC2_CBC = 148;
//     public final static String OBJ_pbe_WithSHA1And128BitRC2_CBC = OBJ_pkcs12_pbeids+".5";

//     public final static String SN_pbe_WithSHA1And40BitRC2_CBC = "PBE-SHA1-RC2-40";
//     public final static String LN_pbe_WithSHA1And40BitRC2_CBC = "pbeWithSHA1And40BitRC2-CBC";
//     public final static int NID_pbe_WithSHA1And40BitRC2_CBC = 149;
//     public final static String OBJ_pbe_WithSHA1And40BitRC2_CBC = OBJ_pkcs12_pbeids+".6";

//     public final static String OBJ_pkcs12_Version1 = OBJ_pkcs12+".10";

//     public final static String OBJ_pkcs12_BagIds = OBJ_pkcs12_Version1+".1";

//     public final static String LN_keyBag = "keyBag";
//     public final static int NID_keyBag = 150;
//     public final static String OBJ_keyBag = OBJ_pkcs12_BagIds+".1";

//     public final static String LN_pkcs8ShroudedKeyBag = "pkcs8ShroudedKeyBag";
//     public final static int NID_pkcs8ShroudedKeyBag = 151;
//     public final static String OBJ_pkcs8ShroudedKeyBag = OBJ_pkcs12_BagIds+".2";

//     public final static String LN_certBag = "certBag";
//     public final static int NID_certBag = 152;
//     public final static String OBJ_certBag = OBJ_pkcs12_BagIds+".3";

//     public final static String LN_crlBag = "crlBag";
//     public final static int NID_crlBag = 153;
//     public final static String OBJ_crlBag = OBJ_pkcs12_BagIds+".4";

//     public final static String LN_secretBag = "secretBag";
//     public final static int NID_secretBag = 154;
//     public final static String OBJ_secretBag = OBJ_pkcs12_BagIds+".5";

//     public final static String LN_safeContentsBag = "safeContentsBag";
//     public final static int NID_safeContentsBag = 155;
//     public final static String OBJ_safeContentsBag = OBJ_pkcs12_BagIds+".6";

//     public final static String LN_friendlyName = "friendlyName";
//     public final static int NID_friendlyName = 156;
//     public final static String OBJ_friendlyName = OBJ_pkcs9+".20";

//     public final static String LN_localKeyID = "localKeyID";
//     public final static int NID_localKeyID = 157;
//     public final static String OBJ_localKeyID = OBJ_pkcs9+".21";

//     public final static String OBJ_certTypes = OBJ_pkcs9+".22";

//     public final static String LN_x509Certificate = "x509Certificate";
//     public final static int NID_x509Certificate = 158;
//     public final static String OBJ_x509Certificate = OBJ_certTypes+".1";

//     public final static String LN_sdsiCertificate = "sdsiCertificate";
//     public final static int NID_sdsiCertificate = 159;
//     public final static String OBJ_sdsiCertificate = OBJ_certTypes+".2";

//     public final static String OBJ_crlTypes = OBJ_pkcs9+".23";

//     public final static String LN_x509Crl = "x509Crl";
//     public final static int NID_x509Crl = 160;
//     public final static String OBJ_x509Crl = OBJ_crlTypes+".1";

//     public final static String LN_pbes2 = "PBES2";
//     public final static int NID_pbes2 = 161;
//     public final static String OBJ_pbes2 = OBJ_pkcs+".5.13";

//     public final static String LN_pbmac1 = "PBMAC1";
//     public final static int NID_pbmac1 = 162;
//     public final static String OBJ_pbmac1 = OBJ_pkcs+".5.14";

//     public final static String LN_hmacWithSHA1 = "hmacWithSHA1";
//     public final static int NID_hmacWithSHA1 = 163;
//     public final static String OBJ_hmacWithSHA1 = OBJ_rsadsi+".2.7";

//     public final static String LN_id_qt_cps = "Policy Qualifier CPS";
//     public final static String SN_id_qt_cps = "id-qt-cps";
//     public final static int NID_id_qt_cps = 164;
//     public final static String OBJ_id_qt_cps = OBJ_id_pkix+".2.1";

//     public final static String LN_id_qt_unotice = "Policy Qualifier User Notice";
//     public final static String SN_id_qt_unotice = "id-qt-unotice";
//     public final static int NID_id_qt_unotice = 165;
//     public final static String OBJ_id_qt_unotice = OBJ_id_pkix+".2.2";

//     public final static String SN_rc2_64_cbc = "RC2-64-CBC";
//     public final static String LN_rc2_64_cbc = "rc2-64-cbc";
//     public final static int NID_rc2_64_cbc = 166;

//     public final static String SN_SMIMECapabilities = "SMIME-CAPS";
//     public final static String LN_SMIMECapabilities = "S/MIME Capabilities";
//     public final static int NID_SMIMECapabilities = 167;
//     public final static String OBJ_SMIMECapabilities = OBJ_pkcs9+".15";

//     public final static String SN_pbeWithMD2AndRC2_CBC = "PBE-MD2-RC2-64";
//     public final static String LN_pbeWithMD2AndRC2_CBC = "pbeWithMD2AndRC2-CBC";
//     public final static int NID_pbeWithMD2AndRC2_CBC = 168;
//     public final static String OBJ_pbeWithMD2AndRC2_CBC = OBJ_pkcs+".5.4";

//     public final static String SN_pbeWithMD5AndRC2_CBC = "PBE-MD5-RC2-64";
//     public final static String LN_pbeWithMD5AndRC2_CBC = "pbeWithMD5AndRC2-CBC";
//     public final static int NID_pbeWithMD5AndRC2_CBC = 169;
//     public final static String OBJ_pbeWithMD5AndRC2_CBC = OBJ_pkcs+".5.6";

//     public final static String SN_pbeWithSHA1AndDES_CBC = "PBE-SHA1-DES";
//     public final static String LN_pbeWithSHA1AndDES_CBC = "pbeWithSHA1AndDES-CBC";
//     public final static int NID_pbeWithSHA1AndDES_CBC = 170;
//     public final static String OBJ_pbeWithSHA1AndDES_CBC = OBJ_pkcs+".5.10";

//     public final static String LN_ms_ext_req = "Microsoft Extension Request";
//     public final static String SN_ms_ext_req = "msExtReq";
//     public final static int NID_ms_ext_req = 171;
//     public final static String OBJ_ms_ext_req = "1.3.6.1.4.1.311.2.1.14";

//     public final static String LN_ext_req = "Extension Request";
//     public final static String SN_ext_req = "extReq";
//     public final static int NID_ext_req = 172;
//     public final static String OBJ_ext_req = OBJ_pkcs9+".14";

//     public final static String SN_name = "name";
//     public final static String LN_name = "name";
//     public final static int NID_name = 173;
//     public final static String OBJ_name = OBJ_X509+".41";

//     public final static String SN_dnQualifier = "dnQualifier";
//     public final static String LN_dnQualifier = "dnQualifier";
//     public final static int NID_dnQualifier = 174;
//     public final static String OBJ_dnQualifier = OBJ_X509+".46";

//     public final static String SN_id_pe = "id-pe";
//     public final static int NID_id_pe = 175;
//     public final static String OBJ_id_pe = OBJ_id_pkix+".1";

//     public final static String SN_id_ad = "id-ad";
//     public final static int NID_id_ad = 176;
//     public final static String OBJ_id_ad = OBJ_id_pkix+".48";

//     public final static String SN_info_access = "authorityInfoAccess";
//     public final static String LN_info_access = "Authority Information Access";
//     public final static int NID_info_access = 177;
//     public final static String OBJ_info_access = OBJ_id_pe+".1";

//     public final static String SN_ad_OCSP = "OCSP";
//     public final static String LN_ad_OCSP = "OCSP";
//     public final static int NID_ad_OCSP = 178;
//     public final static String OBJ_ad_OCSP = OBJ_id_ad+".1";

//     public final static String SN_ad_ca_issuers = "caIssuers";
//     public final static String LN_ad_ca_issuers = "CA Issuers";
//     public final static int NID_ad_ca_issuers = 179;
//     public final static String OBJ_ad_ca_issuers = OBJ_id_ad+".2";

//     public final static String SN_OCSP_sign = "OCSPSigning";
//     public final static String LN_OCSP_sign = "OCSP Signing";
//     public final static int NID_OCSP_sign = 180;
//     public final static String OBJ_OCSP_sign = OBJ_id_kp+".9";

//     static {
//         addObject(NID_undef, SN_undef, LN_undef, OBJ_undef);
//         addObject(NID_rsadsi, SN_rsadsi, LN_rsadsi, OBJ_rsadsi);
//         addObject(NID_pkcs, SN_pkcs, LN_pkcs, OBJ_pkcs);
//         addObject(NID_md2, SN_md2, LN_md2, OBJ_md2);
//         addObject(NID_md5, SN_md5, LN_md5, OBJ_md5);
//         addObject(NID_rc4, SN_rc4, LN_rc4, OBJ_rc4);
//         addObject(NID_rsaEncryption, SN_rsaEncryption, LN_rsaEncryption, OBJ_rsaEncryption);
//         addObject(NID_md2WithRSAEncryption, SN_md2WithRSAEncryption, LN_md2WithRSAEncryption, OBJ_md2WithRSAEncryption);
//         addObject(NID_md5WithRSAEncryption, SN_md5WithRSAEncryption, LN_md5WithRSAEncryption, OBJ_md5WithRSAEncryption);
//         addObject(NID_pbeWithMD2AndDES_CBC, SN_pbeWithMD2AndDES_CBC, LN_pbeWithMD2AndDES_CBC, OBJ_pbeWithMD2AndDES_CBC);
//         addObject(NID_pbeWithMD5AndDES_CBC, SN_pbeWithMD5AndDES_CBC, LN_pbeWithMD5AndDES_CBC, OBJ_pbeWithMD5AndDES_CBC);
//         addObject(NID_X500, SN_X500, LN_X500, OBJ_X500);
//         addObject(NID_X509, SN_X509, LN_X509, OBJ_X509);
//         addObject(NID_commonName, SN_commonName, LN_commonName, OBJ_commonName);
//         addObject(NID_countryName, SN_countryName, LN_countryName, OBJ_countryName);
//         addObject(NID_localityName, SN_localityName, LN_localityName, OBJ_localityName);
//         addObject(NID_stateOrProvinceName, SN_stateOrProvinceName, LN_stateOrProvinceName, OBJ_stateOrProvinceName);
//         addObject(NID_organizationName, SN_organizationName, LN_organizationName, OBJ_organizationName);
//         addObject(NID_organizationalUnitName, SN_organizationalUnitName, LN_organizationalUnitName, OBJ_organizationalUnitName);
//         addObject(NID_rsa, SN_rsa, LN_rsa, OBJ_rsa);
//         addObject(NID_pkcs7, SN_pkcs7, LN_pkcs7, OBJ_pkcs7);
//         addObject(NID_pkcs7_data, SN_pkcs7_data, LN_pkcs7_data, OBJ_pkcs7_data);
//         addObject(NID_pkcs7_signed, SN_pkcs7_signed, LN_pkcs7_signed, OBJ_pkcs7_signed);
//         addObject(NID_pkcs7_enveloped, SN_pkcs7_enveloped, LN_pkcs7_enveloped, OBJ_pkcs7_enveloped);
//         addObject(NID_pkcs7_signedAndEnveloped, SN_pkcs7_signedAndEnveloped, LN_pkcs7_signedAndEnveloped, OBJ_pkcs7_signedAndEnveloped);
//         addObject(NID_pkcs7_digest, SN_pkcs7_digest, LN_pkcs7_digest, OBJ_pkcs7_digest);
//         addObject(NID_pkcs7_encrypted, SN_pkcs7_encrypted, LN_pkcs7_encrypted, OBJ_pkcs7_encrypted);
//         addObject(NID_pkcs3, SN_pkcs3, LN_pkcs3, OBJ_pkcs3);
//         addObject(NID_dhKeyAgreement, SN_dhKeyAgreement, LN_dhKeyAgreement, OBJ_dhKeyAgreement);
//         addObject(NID_des_ecb, SN_des_ecb, LN_des_ecb, OBJ_des_ecb);
//         addObject(NID_des_cfb64, SN_des_cfb64, LN_des_cfb64, OBJ_des_cfb64);
//         addObject(NID_des_cbc, SN_des_cbc, LN_des_cbc, OBJ_des_cbc);
//         addObject(NID_des_ede, SN_des_ede, LN_des_ede, OBJ_des_ede);
//         addObject(NID_des_ede3, SN_des_ede3, LN_des_ede3, OBJ_des_ede3);
//         addObject(NID_idea_cbc, SN_idea_cbc, LN_idea_cbc, OBJ_idea_cbc);
//         addObject(NID_idea_cfb64, SN_idea_cfb64, LN_idea_cfb64, OBJ_idea_cfb64);
//         addObject(NID_idea_ecb, SN_idea_ecb, LN_idea_ecb, OBJ_idea_ecb);
//         addObject(NID_rc2_cbc, SN_rc2_cbc, LN_rc2_cbc, OBJ_rc2_cbc);
//         addObject(NID_rc2_ecb, SN_rc2_ecb, LN_rc2_ecb, OBJ_rc2_ecb);
//         addObject(NID_rc2_cfb64, SN_rc2_cfb64, LN_rc2_cfb64, OBJ_rc2_cfb64);
//         addObject(NID_rc2_ofb64, SN_rc2_ofb64, LN_rc2_ofb64, OBJ_rc2_ofb64);
//         addObject(NID_sha, SN_sha, LN_sha, OBJ_sha);
//         addObject(NID_shaWithRSAEncryption, SN_shaWithRSAEncryption, LN_shaWithRSAEncryption, OBJ_shaWithRSAEncryption);
//         addObject(NID_des_ede_cbc, SN_des_ede_cbc, LN_des_ede_cbc, OBJ_des_ede_cbc);
//         addObject(NID_des_ede3_cbc, SN_des_ede3_cbc, LN_des_ede3_cbc, OBJ_des_ede3_cbc);
//         addObject(NID_des_ofb64, SN_des_ofb64, LN_des_ofb64, OBJ_des_ofb64);
//         addObject(NID_idea_ofb64, SN_idea_ofb64, LN_idea_ofb64, OBJ_idea_ofb64);
//         addObject(NID_pkcs9, SN_pkcs9, LN_pkcs9, OBJ_pkcs9);
//         addObject(NID_pkcs9_emailAddress, SN_pkcs9_emailAddress, LN_pkcs9_emailAddress, OBJ_pkcs9_emailAddress);
//         addObject(NID_pkcs9_unstructuredName, SN_pkcs9_unstructuredName, LN_pkcs9_unstructuredName, OBJ_pkcs9_unstructuredName);
//         addObject(NID_pkcs9_contentType, SN_pkcs9_contentType, LN_pkcs9_contentType, OBJ_pkcs9_contentType);
//         addObject(NID_pkcs9_messageDigest, SN_pkcs9_messageDigest, LN_pkcs9_messageDigest, OBJ_pkcs9_messageDigest);
//         addObject(NID_pkcs9_signingTime, SN_pkcs9_signingTime, LN_pkcs9_signingTime, OBJ_pkcs9_signingTime);
//         addObject(NID_pkcs9_countersignature, SN_pkcs9_countersignature, LN_pkcs9_countersignature, OBJ_pkcs9_countersignature);
//         addObject(NID_pkcs9_challengePassword, SN_pkcs9_challengePassword, LN_pkcs9_challengePassword, OBJ_pkcs9_challengePassword);
//         addObject(NID_pkcs9_unstructuredAddress, SN_pkcs9_unstructuredAddress, LN_pkcs9_unstructuredAddress, OBJ_pkcs9_unstructuredAddress);
//         addObject(NID_pkcs9_extCertAttributes, SN_pkcs9_extCertAttributes, LN_pkcs9_extCertAttributes, OBJ_pkcs9_extCertAttributes);
//         addObject(NID_netscape, SN_netscape, LN_netscape, OBJ_netscape);
//         addObject(NID_netscape_cert_extension, SN_netscape_cert_extension, LN_netscape_cert_extension, OBJ_netscape_cert_extension);
//         addObject(NID_netscape_data_type, SN_netscape_data_type, LN_netscape_data_type, OBJ_netscape_data_type);
//         addObject(NID_des_ede_cfb64, SN_des_ede_cfb64, LN_des_ede_cfb64, OBJ_des_ede_cfb64);
//         addObject(NID_des_ede3_cfb64, SN_des_ede3_cfb64, LN_des_ede3_cfb64, OBJ_des_ede3_cfb64);
//         addObject(NID_des_ede_ofb64, SN_des_ede_ofb64, LN_des_ede_ofb64, OBJ_des_ede_ofb64);
//         addObject(NID_des_ede3_ofb64, SN_des_ede3_ofb64, LN_des_ede3_ofb64, OBJ_des_ede3_ofb64);
//         addObject(NID_sha1, SN_sha1, LN_sha1, OBJ_sha1);
//         addObject(NID_sha1WithRSAEncryption, SN_sha1WithRSAEncryption, LN_sha1WithRSAEncryption, OBJ_sha1WithRSAEncryption);
//         addObject(NID_dsaWithSHA, SN_dsaWithSHA, LN_dsaWithSHA, OBJ_dsaWithSHA);
//         addObject(NID_dsa_2, SN_dsa_2, LN_dsa_2, OBJ_dsa_2);
//         addObject(NID_pbeWithSHA1AndRC2_CBC, SN_pbeWithSHA1AndRC2_CBC, LN_pbeWithSHA1AndRC2_CBC, OBJ_pbeWithSHA1AndRC2_CBC);
//         addObject(NID_id_pbkdf2, SN_id_pbkdf2, LN_id_pbkdf2, OBJ_id_pbkdf2);
//         addObject(NID_dsaWithSHA1_2, SN_dsaWithSHA1_2, LN_dsaWithSHA1_2, OBJ_dsaWithSHA1_2);
//         addObject(NID_netscape_cert_type, SN_netscape_cert_type, LN_netscape_cert_type, OBJ_netscape_cert_type);
//         addObject(NID_netscape_base_url, SN_netscape_base_url, LN_netscape_base_url, OBJ_netscape_base_url);
//         addObject(NID_netscape_revocation_url, SN_netscape_revocation_url, LN_netscape_revocation_url, OBJ_netscape_revocation_url);
//         addObject(NID_netscape_ca_revocation_url, SN_netscape_ca_revocation_url, LN_netscape_ca_revocation_url, OBJ_netscape_ca_revocation_url);
//         addObject(NID_netscape_renewal_url, SN_netscape_renewal_url, LN_netscape_renewal_url, OBJ_netscape_renewal_url);
//         addObject(NID_netscape_ca_policy_url, SN_netscape_ca_policy_url, LN_netscape_ca_policy_url, OBJ_netscape_ca_policy_url);
//         addObject(NID_netscape_ssl_server_name, SN_netscape_ssl_server_name, LN_netscape_ssl_server_name, OBJ_netscape_ssl_server_name);
//         addObject(NID_netscape_comment, SN_netscape_comment, LN_netscape_comment, OBJ_netscape_comment);
//         addObject(NID_netscape_cert_sequence, SN_netscape_cert_sequence, LN_netscape_cert_sequence, OBJ_netscape_cert_sequence);
//         addObject(NID_desx_cbc, SN_desx_cbc, LN_desx_cbc, OBJ_desx_cbc);
//         addObject(NID_id_ce, SN_id_ce, LN_id_ce, OBJ_id_ce);
//         addObject(NID_subject_key_identifier, SN_subject_key_identifier, LN_subject_key_identifier, OBJ_subject_key_identifier);
//         addObject(NID_key_usage, SN_key_usage, LN_key_usage, OBJ_key_usage);
//         addObject(NID_private_key_usage_period, SN_private_key_usage_period, LN_private_key_usage_period, OBJ_private_key_usage_period);
//         addObject(NID_subject_alt_name, SN_subject_alt_name, LN_subject_alt_name, OBJ_subject_alt_name);
//         addObject(NID_issuer_alt_name, SN_issuer_alt_name, LN_issuer_alt_name, OBJ_issuer_alt_name);
//         addObject(NID_basic_constraints, SN_basic_constraints, LN_basic_constraints, OBJ_basic_constraints);
//         addObject(NID_crl_number, SN_crl_number, LN_crl_number, OBJ_crl_number);
//         addObject(NID_certificate_policies, SN_certificate_policies, LN_certificate_policies, OBJ_certificate_policies);
//         addObject(NID_authority_key_identifier, SN_authority_key_identifier, LN_authority_key_identifier, OBJ_authority_key_identifier);
//         addObject(NID_bf_cbc, SN_bf_cbc, LN_bf_cbc, OBJ_bf_cbc);
//         addObject(NID_bf_ecb, SN_bf_ecb, LN_bf_ecb, OBJ_bf_ecb);
//         addObject(NID_bf_cfb64, SN_bf_cfb64, LN_bf_cfb64, OBJ_bf_cfb64);
//         addObject(NID_bf_ofb64, SN_bf_ofb64, LN_bf_ofb64, OBJ_bf_ofb64);
//         addObject(NID_mdc2, SN_mdc2, LN_mdc2, OBJ_mdc2);
//         addObject(NID_mdc2WithRSA, SN_mdc2WithRSA, LN_mdc2WithRSA, OBJ_mdc2WithRSA);
//         addObject(NID_rc4_40, SN_rc4_40, LN_rc4_40, OBJ_rc4_40);
//         addObject(NID_rc2_40_cbc, SN_rc2_40_cbc, LN_rc2_40_cbc, OBJ_rc2_40_cbc);
//         addObject(NID_givenName, SN_givenName, LN_givenName, OBJ_givenName);
//         addObject(NID_surname, SN_surname, LN_surname, OBJ_surname);
//         addObject(NID_initials, SN_initials, LN_initials, OBJ_initials);
//         addObject(NID_uniqueIdentifier, SN_uniqueIdentifier, LN_uniqueIdentifier, OBJ_uniqueIdentifier);
//         addObject(NID_crl_distribution_points, SN_crl_distribution_points, LN_crl_distribution_points, OBJ_crl_distribution_points);
//         addObject(NID_md5WithRSA, SN_md5WithRSA, LN_md5WithRSA, OBJ_md5WithRSA);
//         addObject(NID_serialNumber, SN_serialNumber, LN_serialNumber, OBJ_serialNumber);
//         addObject(NID_title, SN_title, LN_title, OBJ_title);
//         addObject(NID_description, SN_description, LN_description, OBJ_description);
//         addObject(NID_cast5_cbc, SN_cast5_cbc, LN_cast5_cbc, OBJ_cast5_cbc);
//         addObject(NID_cast5_ecb, SN_cast5_ecb, LN_cast5_ecb, OBJ_cast5_ecb);
//         addObject(NID_cast5_cfb64, SN_cast5_cfb64, LN_cast5_cfb64, OBJ_cast5_cfb64);
//         addObject(NID_cast5_ofb64, SN_cast5_ofb64, LN_cast5_ofb64, OBJ_cast5_ofb64);
//         addObject(NID_pbeWithMD5AndCast5_CBC, SN_pbeWithMD5AndCast5_CBC, LN_pbeWithMD5AndCast5_CBC, OBJ_pbeWithMD5AndCast5_CBC);
//         addObject(NID_dsaWithSHA1, SN_dsaWithSHA1, LN_dsaWithSHA1, OBJ_dsaWithSHA1);
//         addObject(NID_md5_sha1, SN_md5_sha1, LN_md5_sha1, OBJ_md5_sha1);
//         addObject(NID_sha1WithRSA, SN_sha1WithRSA, LN_sha1WithRSA, OBJ_sha1WithRSA);
//         addObject(NID_dsa, SN_dsa, LN_dsa, OBJ_dsa);
//         addObject(NID_ripemd160, SN_ripemd160, LN_ripemd160, OBJ_ripemd160);
//         addObject(NID_ripemd160WithRSA, SN_ripemd160WithRSA, LN_ripemd160WithRSA, OBJ_ripemd160WithRSA);
//         addObject(NID_rc5_cbc, SN_rc5_cbc, LN_rc5_cbc, OBJ_rc5_cbc);
//         addObject(NID_rc5_ecb, SN_rc5_ecb, LN_rc5_ecb, OBJ_rc5_ecb);
//         addObject(NID_rc5_cfb64, SN_rc5_cfb64, LN_rc5_cfb64, OBJ_rc5_cfb64);
//         addObject(NID_rc5_ofb64, SN_rc5_ofb64, LN_rc5_ofb64, OBJ_rc5_ofb64);
//         addObject(NID_rle_compression, SN_rle_compression, LN_rle_compression, OBJ_rle_compression);
//         addObject(NID_zlib_compression, SN_zlib_compression, LN_zlib_compression, OBJ_zlib_compression);
//         addObject(NID_ext_key_usage, SN_ext_key_usage, LN_ext_key_usage, OBJ_ext_key_usage);
//         addObject(NID_id_pkix, SN_id_pkix, LN_id_pkix, OBJ_id_pkix);
//         addObject(NID_id_kp, SN_id_kp, LN_id_kp, OBJ_id_kp);
//         addObject(NID_server_auth, SN_server_auth, LN_server_auth, OBJ_server_auth);
//         addObject(NID_client_auth, SN_client_auth, LN_client_auth, OBJ_client_auth);
//         addObject(NID_code_sign, SN_code_sign, LN_code_sign, OBJ_code_sign);
//         addObject(NID_email_protect, SN_email_protect, LN_email_protect, OBJ_email_protect);
//         addObject(NID_time_stamp, SN_time_stamp, LN_time_stamp, OBJ_time_stamp);
//         addObject(NID_ms_code_ind, SN_ms_code_ind, LN_ms_code_ind, OBJ_ms_code_ind);
//         addObject(NID_ms_code_com, SN_ms_code_com, LN_ms_code_com, OBJ_ms_code_com);
//         addObject(NID_ms_ctl_sign, SN_ms_ctl_sign, LN_ms_ctl_sign, OBJ_ms_ctl_sign);
//         addObject(NID_ms_sgc, SN_ms_sgc, LN_ms_sgc, OBJ_ms_sgc);
//         addObject(NID_ms_efs, SN_ms_efs, LN_ms_efs, OBJ_ms_efs);
//         addObject(NID_ns_sgc, SN_ns_sgc, LN_ns_sgc, OBJ_ns_sgc);
//         addObject(NID_delta_crl, SN_delta_crl, LN_delta_crl, OBJ_delta_crl);
//         addObject(NID_crl_reason, SN_crl_reason, LN_crl_reason, OBJ_crl_reason);
//         addObject(NID_invalidity_date, SN_invalidity_date, LN_invalidity_date, OBJ_invalidity_date);
//         addObject(NID_sxnet, SN_sxnet, LN_sxnet, OBJ_sxnet);
//         addObject(NID_pbe_WithSHA1And128BitRC4, SN_pbe_WithSHA1And128BitRC4, LN_pbe_WithSHA1And128BitRC4, OBJ_pbe_WithSHA1And128BitRC4);
//         addObject(NID_pbe_WithSHA1And40BitRC4, SN_pbe_WithSHA1And40BitRC4, LN_pbe_WithSHA1And40BitRC4, OBJ_pbe_WithSHA1And40BitRC4);
//         addObject(NID_pbe_WithSHA1And3_Key_TripleDES_CBC, SN_pbe_WithSHA1And3_Key_TripleDES_CBC, LN_pbe_WithSHA1And3_Key_TripleDES_CBC, OBJ_pbe_WithSHA1And3_Key_TripleDES_CBC);
//         addObject(NID_pbe_WithSHA1And2_Key_TripleDES_CBC, SN_pbe_WithSHA1And2_Key_TripleDES_CBC, LN_pbe_WithSHA1And2_Key_TripleDES_CBC, OBJ_pbe_WithSHA1And2_Key_TripleDES_CBC);
//         addObject(NID_pbe_WithSHA1And128BitRC2_CBC, SN_pbe_WithSHA1And128BitRC2_CBC, LN_pbe_WithSHA1And128BitRC2_CBC, OBJ_pbe_WithSHA1And128BitRC2_CBC);
//         addObject(NID_pbe_WithSHA1And40BitRC2_CBC, SN_pbe_WithSHA1And40BitRC2_CBC, LN_pbe_WithSHA1And40BitRC2_CBC, OBJ_pbe_WithSHA1And40BitRC2_CBC);
//         addObject(NID_keyBag, SN_keyBag, LN_keyBag, OBJ_keyBag);
//         addObject(NID_pkcs8ShroudedKeyBag, SN_pkcs8ShroudedKeyBag, LN_pkcs8ShroudedKeyBag, OBJ_pkcs8ShroudedKeyBag);
//         addObject(NID_certBag, SN_certBag, LN_certBag, OBJ_certBag);
//         addObject(NID_crlBag, SN_crlBag, LN_crlBag, OBJ_crlBag);
//         addObject(NID_secretBag, SN_secretBag, LN_secretBag, OBJ_secretBag);
//         addObject(NID_safeContentsBag, SN_safeContentsBag, LN_safeContentsBag, OBJ_safeContentsBag);
//         addObject(NID_friendlyName, SN_friendlyName, LN_friendlyName, OBJ_friendlyName);
//         addObject(NID_localKeyID, SN_localKeyID, LN_localKeyID, OBJ_localKeyID);
//         addObject(NID_x509Certificate, SN_x509Certificate, LN_x509Certificate, OBJ_x509Certificate);
//         addObject(NID_sdsiCertificate, SN_sdsiCertificate, LN_sdsiCertificate, OBJ_sdsiCertificate);
//         addObject(NID_x509Crl, SN_x509Crl, LN_x509Crl, OBJ_x509Crl);
//         addObject(NID_pbes2, SN_pbes2, LN_pbes2, OBJ_pbes2);
//         addObject(NID_pbmac1, SN_pbmac1, LN_pbmac1, OBJ_pbmac1);
//         addObject(NID_hmacWithSHA1, SN_hmacWithSHA1, LN_hmacWithSHA1, OBJ_hmacWithSHA1);
//         addObject(NID_id_qt_cps, SN_id_qt_cps, LN_id_qt_cps, OBJ_id_qt_cps);
//         addObject(NID_id_qt_unotice, SN_id_qt_unotice, LN_id_qt_unotice, OBJ_id_qt_unotice);
//         addObject(NID_rc2_64_cbc, SN_rc2_64_cbc, LN_rc2_64_cbc, OBJ_rc2_64_cbc);
//         addObject(NID_SMIMECapabilities, SN_SMIMECapabilities, LN_SMIMECapabilities, OBJ_SMIMECapabilities);
//         addObject(NID_pbeWithMD2AndRC2_CBC, SN_pbeWithMD2AndRC2_CBC, LN_pbeWithMD2AndRC2_CBC, OBJ_pbeWithMD2AndRC2_CBC);
//         addObject(NID_pbeWithMD5AndRC2_CBC, SN_pbeWithMD5AndRC2_CBC, LN_pbeWithMD5AndRC2_CBC, OBJ_pbeWithMD5AndRC2_CBC);
//         addObject(NID_pbeWithSHA1AndDES_CBC, SN_pbeWithSHA1AndDES_CBC, LN_pbeWithSHA1AndDES_CBC, OBJ_pbeWithSHA1AndDES_CBC);
//         addObject(NID_ms_ext_req, SN_ms_ext_req, LN_ms_ext_req, OBJ_ms_ext_req);
//         addObject(NID_ext_req, SN_ext_req, LN_ext_req, OBJ_ext_req);
//         addObject(NID_name, SN_name, LN_name, OBJ_name);
//         addObject(NID_dnQualifier, SN_dnQualifier, LN_dnQualifier, OBJ_dnQualifier);
//         addObject(NID_id_pe, SN_id_pe, LN_id_pe, OBJ_id_pe);
//         addObject(NID_id_ad, SN_id_ad, LN_id_ad, OBJ_id_ad);
//         addObject(NID_info_access, SN_info_access, LN_info_access, OBJ_info_access);
//         addObject(NID_ad_OCSP, SN_ad_OCSP, LN_ad_OCSP, OBJ_ad_OCSP);
//         addObject(NID_ad_ca_issuers, SN_ad_ca_issuers, LN_ad_ca_issuers, OBJ_ad_ca_issuers);
//         addObject(NID_OCSP_sign, SN_OCSP_sign, LN_OCSP_sign, OBJ_OCSP_sign);

//         addObject(181, "AES-192-OFB", "aes-192-ofb","2.16.840.1.101.3.4.1.23");
//         addObject(182, "AES-192-CFB", "aes-192-cfb","2.16.840.1.101.3.4.1.24");
//         addObject(183, "AES-256-EBC", "aes-256-ebc","2.16.840.1.101.3.4.1.41");
//         addObject(184, "AES-256-CBC", "aes-256-cbc","2.16.840.1.101.3.4.1.42");
//         addObject(185, "AES-256-OFB", "aes-256-ofb","2.16.840.1.101.3.4.1.43");
//         addObject(186, "AES-256-CFB", "aes-256-cfb","2.16.840.1.101.3.4.1.44");
//     }
}// ASN1Registry
