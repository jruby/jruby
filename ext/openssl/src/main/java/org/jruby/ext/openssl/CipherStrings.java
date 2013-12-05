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
package org.jruby.ext.openssl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class CipherStrings {
    public final static String SSL2_TXT_DES_64_CFB64_WITH_MD5_1 = "DES-CFB-M1";
    public final static String SSL2_TXT_NULL_WITH_MD5 = "NULL-MD5";
    public final static String SSL2_TXT_RC4_128_WITH_MD5 = "RC4-MD5";
    public final static String SSL2_TXT_RC4_128_EXPORT40_WITH_MD5 = "EXP-RC4-MD5";
    public final static String SSL2_TXT_RC2_128_CBC_WITH_MD5 = "RC2-CBC-MD5";
    public final static String SSL2_TXT_RC2_128_CBC_EXPORT40_WITH_MD5 = "EXP-RC2-CBC-MD5";
    public final static String SSL2_TXT_IDEA_128_CBC_WITH_MD5 = "IDEA-CBC-MD5";
    public final static String SSL2_TXT_DES_64_CBC_WITH_MD5 = "DES-CBC-MD5";
    public final static String SSL2_TXT_DES_64_CBC_WITH_SHA = "DES-CBC-SHA";
    public final static String SSL2_TXT_DES_192_EDE3_CBC_WITH_MD5 = "DES-CBC3-MD5";
    public final static String SSL2_TXT_DES_192_EDE3_CBC_WITH_SHA = "DES-CBC3-SHA";
    public final static String SSL2_TXT_RC4_64_WITH_MD5 = "RC4-64-MD5";
    public final static String SSL2_TXT_NULL = "NULL";

    public final static String SSL3_TXT_RSA_NULL_MD5 = "NULL-MD5";
    public final static String SSL3_TXT_RSA_NULL_SHA = "NULL-SHA";
    public final static String SSL3_TXT_RSA_RC4_40_MD5 = "EXP-RC4-MD5";
    public final static String SSL3_TXT_RSA_RC4_128_MD5 = "RC4-MD5";
    public final static String SSL3_TXT_RSA_RC4_128_SHA = "RC4-SHA";
    public final static String SSL3_TXT_RSA_RC2_40_MD5 = "EXP-RC2-CBC-MD5";
    public final static String SSL3_TXT_RSA_IDEA_128_SHA = "IDEA-CBC-SHA";
    public final static String SSL3_TXT_RSA_DES_40_CBC_SHA = "EXP-DES-CBC-SHA";
    public final static String SSL3_TXT_RSA_DES_64_CBC_SHA = "DES-CBC-SHA";
    public final static String SSL3_TXT_RSA_DES_192_CBC3_SHA = "DES-CBC3-SHA";
    public final static String SSL3_TXT_DH_DSS_DES_40_CBC_SHA = "EXP-DH-DSS-DES-CBC-SHA";
    public final static String SSL3_TXT_DH_DSS_DES_64_CBC_SHA = "DH-DSS-DES-CBC-SHA";
    public final static String SSL3_TXT_DH_DSS_DES_192_CBC3_SHA = "DH-DSS-DES-CBC3-SHA";
    public final static String SSL3_TXT_DH_RSA_DES_40_CBC_SHA = "EXP-DH-RSA-DES-CBC-SHA";
    public final static String SSL3_TXT_DH_RSA_DES_64_CBC_SHA = "DH-RSA-DES-CBC-SHA";
    public final static String SSL3_TXT_DH_RSA_DES_192_CBC3_SHA = "DH-RSA-DES-CBC3-SHA";
    public final static String SSL3_TXT_EDH_DSS_DES_40_CBC_SHA = "EXP-EDH-DSS-DES-CBC-SHA";
    public final static String SSL3_TXT_EDH_DSS_DES_64_CBC_SHA = "EDH-DSS-DES-CBC-SHA";
    public final static String SSL3_TXT_EDH_DSS_DES_192_CBC3_SHA = "EDH-DSS-DES-CBC3-SHA";
    public final static String SSL3_TXT_EDH_RSA_DES_40_CBC_SHA = "EXP-EDH-RSA-DES-CBC-SHA";
    public final static String SSL3_TXT_EDH_RSA_DES_64_CBC_SHA = "EDH-RSA-DES-CBC-SHA";
    public final static String SSL3_TXT_EDH_RSA_DES_192_CBC3_SHA = "EDH-RSA-DES-CBC3-SHA";
    public final static String SSL3_TXT_ADH_RC4_40_MD5 = "EXP-ADH-RC4-MD5";
    public final static String SSL3_TXT_ADH_RC4_128_MD5 = "ADH-RC4-MD5";
    public final static String SSL3_TXT_ADH_DES_40_CBC_SHA = "EXP-ADH-DES-CBC-SHA";
    public final static String SSL3_TXT_ADH_DES_64_CBC_SHA = "ADH-DES-CBC-SHA";
    public final static String SSL3_TXT_ADH_DES_192_CBC_SHA = "ADH-DES-CBC3-SHA";
    public final static String SSL3_TXT_FZA_DMS_NULL_SHA = "FZA-NULL-SHA";
    public final static String SSL3_TXT_FZA_DMS_FZA_SHA = "FZA-FZA-CBC-SHA";
    public final static String SSL3_TXT_FZA_DMS_RC4_SHA = "FZA-RC4-SHA";
    public final static String SSL3_TXT_KRB5_DES_64_CBC_SHA = "KRB5-DES-CBC-SHA";
    public final static String SSL3_TXT_KRB5_DES_192_CBC3_SHA = "KRB5-DES-CBC3-SHA";
    public final static String SSL3_TXT_KRB5_RC4_128_SHA = "KRB5-RC4-SHA";
    public final static String SSL3_TXT_KRB5_IDEA_128_CBC_SHA = "KRB5-IDEA-CBC-SHA";
    public final static String SSL3_TXT_KRB5_DES_64_CBC_MD5 = "KRB5-DES-CBC-MD5";
    public final static String SSL3_TXT_KRB5_DES_192_CBC3_MD5 = "KRB5-DES-CBC3-MD5";
    public final static String SSL3_TXT_KRB5_RC4_128_MD5 = "KRB5-RC4-MD5";
    public final static String SSL3_TXT_KRB5_IDEA_128_CBC_MD5 = "KRB5-IDEA-CBC-MD5";
    public final static String SSL3_TXT_KRB5_DES_40_CBC_SHA = "EXP-KRB5-DES-CBC-SHA";
    public final static String SSL3_TXT_KRB5_RC2_40_CBC_SHA = "EXP-KRB5-RC2-CBC-SHA";
    public final static String SSL3_TXT_KRB5_RC4_40_SHA = "EXP-KRB5-RC4-SHA";
    public final static String SSL3_TXT_KRB5_DES_40_CBC_MD5 = "EXP-KRB5-DES-CBC-MD5";
    public final static String SSL3_TXT_KRB5_RC2_40_CBC_MD5 = "EXP-KRB5-RC2-CBC-MD5";
    public final static String SSL3_TXT_KRB5_RC4_40_MD5 = "EXP-KRB5-RC4-MD5";

    public final static String SSL_TXT_NULL_WITH_MD5 = SSL2_TXT_NULL_WITH_MD5;
    public final static String SSL_TXT_RC4_128_WITH_MD5 = SSL2_TXT_RC4_128_WITH_MD5;
    public final static String SSL_TXT_RC4_128_EXPORT40_WITH_MD5 = SSL2_TXT_RC4_128_EXPORT40_WITH_MD5;
    public final static String SSL_TXT_RC2_128_CBC_WITH_MD5 = SSL2_TXT_RC2_128_CBC_WITH_MD5;
    public final static String SSL_TXT_RC2_128_CBC_EXPORT40_WITH_MD5 = SSL2_TXT_RC2_128_CBC_EXPORT40_WITH_MD5;
    public final static String SSL_TXT_IDEA_128_CBC_WITH_MD5 = SSL2_TXT_IDEA_128_CBC_WITH_MD5;
    public final static String SSL_TXT_DES_64_CBC_WITH_MD5 = SSL2_TXT_DES_64_CBC_WITH_MD5;
    public final static String SSL_TXT_DES_64_CBC_WITH_SHA = SSL2_TXT_DES_64_CBC_WITH_SHA;
    public final static String SSL_TXT_DES_192_EDE3_CBC_WITH_MD5 = SSL2_TXT_DES_192_EDE3_CBC_WITH_MD5;
    public final static String SSL_TXT_DES_192_EDE3_CBC_WITH_SHA = SSL2_TXT_DES_192_EDE3_CBC_WITH_SHA;
    
    public final static String SSL_TXT_KRB5_DES_64_CBC_SHA = SSL3_TXT_KRB5_DES_64_CBC_SHA;
    public final static String SSL_TXT_KRB5_DES_192_CBC3_SHA = SSL3_TXT_KRB5_DES_192_CBC3_SHA;
    public final static String SSL_TXT_KRB5_RC4_128_SHA = SSL3_TXT_KRB5_RC4_128_SHA;
    public final static String SSL_TXT_KRB5_IDEA_128_CBC_SHA = SSL3_TXT_KRB5_IDEA_128_CBC_SHA;
    public final static String SSL_TXT_KRB5_DES_64_CBC_MD5 = SSL3_TXT_KRB5_DES_64_CBC_MD5;
    public final static String SSL_TXT_KRB5_DES_192_CBC3_MD5 = SSL3_TXT_KRB5_DES_192_CBC3_MD5;
    public final static String SSL_TXT_KRB5_RC4_128_MD5 = SSL3_TXT_KRB5_RC4_128_MD5;
    public final static String SSL_TXT_KRB5_IDEA_128_CBC_MD5 = SSL3_TXT_KRB5_IDEA_128_CBC_MD5;

    public final static String SSL_TXT_KRB5_DES_40_CBC_SHA = SSL3_TXT_KRB5_DES_40_CBC_SHA;
    public final static String SSL_TXT_KRB5_RC2_40_CBC_SHA = SSL3_TXT_KRB5_RC2_40_CBC_SHA;
    public final static String SSL_TXT_KRB5_RC4_40_SHA = SSL3_TXT_KRB5_RC4_40_SHA;
    public final static String SSL_TXT_KRB5_DES_40_CBC_MD5 = SSL3_TXT_KRB5_DES_40_CBC_MD5;
    public final static String SSL_TXT_KRB5_RC2_40_CBC_MD5 = SSL3_TXT_KRB5_RC2_40_CBC_MD5;
    public final static String SSL_TXT_KRB5_RC4_40_MD5 = SSL3_TXT_KRB5_RC4_40_MD5;

    public final static String SSL_TXT_LOW = "LOW";
    public final static String SSL_TXT_MEDIUM = "MEDIUM";
    public final static String SSL_TXT_HIGH = "HIGH";
    public final static String SSL_TXT_kFZA = "kFZA";
    public final static String SSL_TXT_aFZA = "aFZA";
    public final static String SSL_TXT_eFZA = "eFZA";
    public final static String SSL_TXT_FZA = "FZA";

    public final static String SSL_TXT_aNULL = "aNULL";
    public final static String SSL_TXT_eNULL = "eNULL";
    public final static String SSL_TXT_NULL = "NULL";

    public final static String SSL_TXT_kKRB5 = "kKRB5";
    public final static String SSL_TXT_aKRB5 = "aKRB5";
    public final static String SSL_TXT_KRB5 = "KRB5";

    public final static String SSL_TXT_kRSA = "kRSA";
    public final static String SSL_TXT_kDHr = "kDHr";
    public final static String SSL_TXT_kDHd = "kDHd";
    public final static String SSL_TXT_kEDH = "kEDH";
    public final static String SSL_TXT_aRSA = "aRSA";
    public final static String SSL_TXT_aDSS = "aDSS";
    public final static String SSL_TXT_aDH = "aDH";
    public final static String SSL_TXT_DSS = "DSS";
    public final static String SSL_TXT_DH = "DH";
    public final static String SSL_TXT_EDH = "EDH";
    public final static String SSL_TXT_ADH = "ADH";
    public final static String SSL_TXT_RSA = "RSA";
    public final static String SSL_TXT_DES = "DES";
    public final static String SSL_TXT_3DES = "3DES";
    public final static String SSL_TXT_RC4 = "RC4";
    public final static String SSL_TXT_RC2 = "RC2";
    public final static String SSL_TXT_IDEA = "IDEA";
    public final static String SSL_TXT_AES = "AES";
    public final static String SSL_TXT_MD5 = "MD5";
    public final static String SSL_TXT_SHA1 = "SHA1";
    public final static String SSL_TXT_SHA = "SHA";
    public final static String SSL_TXT_EXP = "EXP";
    public final static String SSL_TXT_EXPORT = "EXPORT";
    public final static String SSL_TXT_EXP40 = "EXPORT40";
    public final static String SSL_TXT_EXP56 = "EXPORT56";
    public final static String SSL_TXT_SSLV2 = "SSLv2";
    public final static String SSL_TXT_SSLV3 = "SSLv3";
    public final static String SSL_TXT_TLSV1 = "TLSv1";
    public final static String SSL_TXT_ALL = "ALL";
    public final static String SSL_TXT_ECC = "ECCdraft";

    public final static String SSL_TXT_CMPALL = "COMPLEMENTOFALL";
    public final static String SSL_TXT_CMPDEF = "COMPLEMENTOFDEFAULT";

    // "ALL:!aNULL:!eNULL:!SSLv2" is for OpenSSL 1.0.0 GA
    public final static String SSL_DEFAULT_CIPHER_LIST = "AES:ALL:!aNULL:!eNULL:+RC4:@STRENGTH";

    public final static long SSL_MKEY_MASK = 0x000000FFL;
    public final static long SSL_kRSA = 0x00000001L;
    public final static long SSL_kDHr = 0x00000002L;
    public final static long SSL_kDHd = 0x00000004L;
    public final static long SSL_kFZA = 0x00000008L;
    public final static long SSL_kEDH = 0x00000010L;
    public final static long SSL_kKRB5 = 0x00000020L;
    public final static long SSL_kECDH = 0x00000040L;
    public final static long SSL_kECDHE = 0x00000080L;
    public final static long SSL_aNULL = 0x00000800L;
    public final static long SSL_AUTH_MASK = 0x00007F00L;
    public final static long SSL_EDH = (SSL_kEDH|(SSL_AUTH_MASK^SSL_aNULL));
    public final static long SSL_aRSA = 0x00000100L;
    public final static long SSL_aDSS = 0x00000200L;
    public final static long SSL_DSS = SSL_aDSS;
    public final static long SSL_aFZA = 0x00000400L;
    public final static long SSL_aDH = 0x00001000L;
    public final static long SSL_aKRB5 = 0x00002000L;
    public final static long SSL_aECDSA = 0x00004000L;
    public final static long SSL_eNULL = 0x00200000L;
    public final static long SSL_eFZA = 0x00100000L;
    public final static long SSL_NULL = (SSL_eNULL);
    public final static long SSL_ADH = (SSL_kEDH|SSL_aNULL);
    public final static long SSL_RSA = (SSL_kRSA|SSL_aRSA);
    public final static long SSL_DH = (SSL_kDHr|SSL_kDHd|SSL_kEDH);
    public final static long SSL_ECDH = (SSL_kECDH|SSL_kECDHE);
    public final static long SSL_FZA = (SSL_aFZA|SSL_kFZA|SSL_eFZA);
    public final static long SSL_KRB5 = (SSL_kKRB5|SSL_aKRB5);
    public final static long SSL_ENC_MASK = 0x043F8000L;
    public final static long SSL_DES = 0x00008000L;
    public final static long SSL_3DES = 0x00010000L;
    public final static long SSL_RC4 = 0x00020000L;
    public final static long SSL_RC2 = 0x00040000L;
    public final static long SSL_IDEA = 0x00080000L;
    public final static long SSL_AES = 0x04000000L;
    public final static long SSL_MAC_MASK = 0x00c00000L;
    public final static long SSL_MD5 = 0x00400000L;
    public final static long SSL_SHA1 = 0x00800000L;
    public final static long SSL_SHA = (SSL_SHA1);
    public final static long SSL_SSL_MASK = 0x03000000L;
    public final static long SSL_SSLV2 = 0x01000000L;
    public final static long SSL_SSLV3 = 0x02000000L;
    public final static long SSL_TLSV1 = SSL_SSLV3;
    public final static long SSL_EXP_MASK = 0x00000003L;
    public final static long SSL_NOT_EXP = 0x00000001L;
    public final static long SSL_EXPORT = 0x00000002L;
    public final static long SSL_STRONG_MASK = 0x000000fcL;
    public final static long SSL_STRONG_NONE = 0x00000004L;
    public final static long SSL_EXP40 = 0x00000008L;
    public final static long SSL_MICRO = (SSL_EXP40);
    public final static long SSL_EXP56 = 0x00000010L;
    public final static long SSL_MINI = (SSL_EXP56);
    public final static long SSL_LOW = 0x00000020L;
    public final static long SSL_MEDIUM = 0x00000040L;
    public final static long SSL_HIGH = 0x00000080L;
    public final static long SSL_ALL = 0xffffffffL;
    public final static long SSL_ALL_CIPHERS = (SSL_MKEY_MASK|SSL_AUTH_MASK|SSL_ENC_MASK|SSL_MAC_MASK);
    public final static long SSL_ALL_STRENGTHS = (SSL_EXP_MASK|SSL_STRONG_MASK);
    public final static long SSL_PKEY_RSA_ENC = 0;
    public final static long SSL_PKEY_RSA_SIGN = 1;
    public final static long SSL_PKEY_DSA_SIGN = 2;
    public final static long SSL_PKEY_DH_RSA = 3;
    public final static long SSL_PKEY_DH_DSA = 4;
    public final static long SSL_PKEY_ECC = 5;
    public final static long SSL_PKEY_NUM = 6;

    public final static long SSL3_CK_RSA_NULL_MD5 = 0x03000001;
    public final static long SSL3_CK_RSA_NULL_SHA = 0x03000002;
    public final static long SSL3_CK_RSA_RC4_40_MD5 = 0x03000003;
    public final static long SSL3_CK_RSA_RC4_128_MD5 = 0x03000004;
    public final static long SSL3_CK_RSA_RC4_128_SHA = 0x03000005;
    public final static long SSL3_CK_RSA_RC2_40_MD5 = 0x03000006;
    public final static long SSL3_CK_RSA_IDEA_128_SHA = 0x03000007;
    public final static long SSL3_CK_RSA_DES_40_CBC_SHA = 0x03000008;
    public final static long SSL3_CK_RSA_DES_64_CBC_SHA = 0x03000009;
    public final static long SSL3_CK_RSA_DES_192_CBC3_SHA = 0x0300000A;
    public final static long SSL3_CK_DH_DSS_DES_40_CBC_SHA = 0x0300000B;
    public final static long SSL3_CK_DH_DSS_DES_64_CBC_SHA = 0x0300000C;
    public final static long SSL3_CK_DH_DSS_DES_192_CBC3_SHA = 0x0300000D;
    public final static long SSL3_CK_DH_RSA_DES_40_CBC_SHA = 0x0300000E;
    public final static long SSL3_CK_DH_RSA_DES_64_CBC_SHA = 0x0300000F;
    public final static long SSL3_CK_DH_RSA_DES_192_CBC3_SHA = 0x03000010;
    public final static long SSL3_CK_EDH_DSS_DES_40_CBC_SHA = 0x03000011;
    public final static long SSL3_CK_EDH_DSS_DES_64_CBC_SHA = 0x03000012;
    public final static long SSL3_CK_EDH_DSS_DES_192_CBC3_SHA = 0x03000013;
    public final static long SSL3_CK_EDH_RSA_DES_40_CBC_SHA = 0x03000014;
    public final static long SSL3_CK_EDH_RSA_DES_64_CBC_SHA = 0x03000015;
    public final static long SSL3_CK_EDH_RSA_DES_192_CBC3_SHA = 0x03000016;
    public final static long SSL3_CK_ADH_RC4_40_MD5 = 0x03000017;
    public final static long SSL3_CK_ADH_RC4_128_MD5 = 0x03000018;
    public final static long SSL3_CK_ADH_DES_40_CBC_SHA = 0x03000019;
    public final static long SSL3_CK_ADH_DES_64_CBC_SHA = 0x0300001A;
    public final static long SSL3_CK_ADH_DES_192_CBC_SHA = 0x0300001B;
    public final static long SSL3_CK_FZA_DMS_NULL_SHA = 0x0300001C;
    public final static long SSL3_CK_FZA_DMS_FZA_SHA = 0x0300001D;
    public final static long SSL3_CK_KRB5_DES_64_CBC_SHA = 0x0300001E;
    public final static long SSL3_CK_KRB5_DES_192_CBC3_SHA = 0x0300001F;
    public final static long SSL3_CK_KRB5_RC4_128_SHA = 0x03000020;
    public final static long SSL3_CK_KRB5_IDEA_128_CBC_SHA = 0x03000021;
    public final static long SSL3_CK_KRB5_DES_64_CBC_MD5 = 0x03000022;
    public final static long SSL3_CK_KRB5_DES_192_CBC3_MD5 = 0x03000023;
    public final static long SSL3_CK_KRB5_RC4_128_MD5 = 0x03000024;
    public final static long SSL3_CK_KRB5_IDEA_128_CBC_MD5 = 0x03000025;
    public final static long SSL3_CK_KRB5_DES_40_CBC_SHA = 0x03000026;
    public final static long SSL3_CK_KRB5_RC2_40_CBC_SHA = 0x03000027;
    public final static long SSL3_CK_KRB5_RC4_40_SHA = 0x03000028;
    public final static long SSL3_CK_KRB5_DES_40_CBC_MD5 = 0x03000029;
    public final static long SSL3_CK_KRB5_RC2_40_CBC_MD5 = 0x0300002A;
    public final static long SSL3_CK_KRB5_RC4_40_MD5 = 0x0300002B;


    public final static long TLS1_CK_RSA_EXPORT1024_WITH_RC4_56_MD5 = 0x03000060;
    public final static long TLS1_CK_RSA_EXPORT1024_WITH_RC2_CBC_56_MD5 = 0x03000061;
    public final static long TLS1_CK_RSA_EXPORT1024_WITH_DES_CBC_SHA = 0x03000062;
    public final static long TLS1_CK_DHE_DSS_EXPORT1024_WITH_DES_CBC_SHA = 0x03000063;
    public final static long TLS1_CK_RSA_EXPORT1024_WITH_RC4_56_SHA = 0x03000064;
    public final static long TLS1_CK_DHE_DSS_EXPORT1024_WITH_RC4_56_SHA = 0x03000065;
    public final static long TLS1_CK_DHE_DSS_WITH_RC4_128_SHA = 0x03000066;
    public final static long TLS1_CK_RSA_WITH_AES_128_SHA = 0x0300002F;
    public final static long TLS1_CK_DH_DSS_WITH_AES_128_SHA = 0x03000030;
    public final static long TLS1_CK_DH_RSA_WITH_AES_128_SHA = 0x03000031;
    public final static long TLS1_CK_DHE_DSS_WITH_AES_128_SHA = 0x03000032;
    public final static long TLS1_CK_DHE_RSA_WITH_AES_128_SHA = 0x03000033;
    public final static long TLS1_CK_ADH_WITH_AES_128_SHA = 0x03000034;
    public final static long TLS1_CK_RSA_WITH_AES_256_SHA = 0x03000035;
    public final static long TLS1_CK_DH_DSS_WITH_AES_256_SHA = 0x03000036;
    public final static long TLS1_CK_DH_RSA_WITH_AES_256_SHA = 0x03000037;
    public final static long TLS1_CK_DHE_DSS_WITH_AES_256_SHA = 0x03000038;
    public final static long TLS1_CK_DHE_RSA_WITH_AES_256_SHA = 0x03000039;
    public final static long TLS1_CK_ADH_WITH_AES_256_SHA = 0x0300003A;
    public final static long TLS1_CK_ECDH_ECDSA_WITH_NULL_SHA = 0x0300C001;
    public final static long TLS1_CK_ECDH_ECDSA_WITH_RC4_128_SHA = 0x0300C002;
    public final static long TLS1_CK_ECDH_ECDSA_WITH_DES_192_CBC3_SHA = 0x0300C003;
    public final static long TLS1_CK_ECDH_ECDSA_WITH_AES_128_CBC_SHA = 0x0300C004;
    public final static long TLS1_CK_ECDH_ECDSA_WITH_AES_256_CBC_SHA = 0x0300C005;
    public final static long TLS1_CK_ECDHE_ECDSA_WITH_NULL_SHA = 0x0300C006;
    public final static long TLS1_CK_ECDHE_ECDSA_WITH_RC4_128_SHA = 0x0300C007;
    public final static long TLS1_CK_ECDHE_ECDSA_WITH_DES_192_CBC3_SHA = 0x0300C008;
    public final static long TLS1_CK_ECDHE_ECDSA_WITH_AES_128_CBC_SHA = 0x0300C009;
    public final static long TLS1_CK_ECDHE_ECDSA_WITH_AES_256_CBC_SHA = 0x0300C00A;
    public final static long TLS1_CK_ECDH_RSA_WITH_NULL_SHA = 0x0300C00B;
    public final static long TLS1_CK_ECDH_RSA_WITH_RC4_128_SHA = 0x0300C00C;
    public final static long TLS1_CK_ECDH_RSA_WITH_DES_192_CBC3_SHA = 0x0300C00D;
    public final static long TLS1_CK_ECDH_RSA_WITH_AES_128_CBC_SHA = 0x0300C00E;
    public final static long TLS1_CK_ECDH_RSA_WITH_AES_256_CBC_SHA = 0x0300C00F;
    public final static long TLS1_CK_ECDHE_RSA_WITH_NULL_SHA = 0x0300C010;
    public final static long TLS1_CK_ECDHE_RSA_WITH_RC4_128_SHA = 0x0300C011;
    public final static long TLS1_CK_ECDHE_RSA_WITH_DES_192_CBC3_SHA = 0x0300C012;
    public final static long TLS1_CK_ECDHE_RSA_WITH_AES_128_CBC_SHA = 0x0300C013;
    public final static long TLS1_CK_ECDHE_RSA_WITH_AES_256_CBC_SHA = 0x0300C014;
    public final static long TLS1_CK_ECDH_anon_WITH_NULL_SHA = 0x0300C015;
    public final static long TLS1_CK_ECDH_anon_WITH_RC4_128_SHA = 0x0300C016;
    public final static long TLS1_CK_ECDH_anon_WITH_DES_192_CBC3_SHA = 0x0300C017;
    public final static long TLS1_CK_ECDH_anon_WITH_AES_128_CBC_SHA = 0x0300C018;
    public final static long TLS1_CK_ECDH_anon_WITH_AES_256_CBC_SHA = 0x0300C019;

    public final static String TLS1_TXT_RSA_EXPORT1024_WITH_RC4_56_MD5 = "EXP1024-RC4-MD5";
    public final static String TLS1_TXT_RSA_EXPORT1024_WITH_RC2_CBC_56_MD5 = "EXP1024-RC2-CBC-MD5";
    public final static String TLS1_TXT_RSA_EXPORT1024_WITH_DES_CBC_SHA = "EXP1024-DES-CBC-SHA";
    public final static String TLS1_TXT_DHE_DSS_EXPORT1024_WITH_DES_CBC_SHA = "EXP1024-DHE-DSS-DES-CBC-SHA";
    public final static String TLS1_TXT_RSA_EXPORT1024_WITH_RC4_56_SHA = "EXP1024-RC4-SHA";
    public final static String TLS1_TXT_DHE_DSS_EXPORT1024_WITH_RC4_56_SHA = "EXP1024-DHE-DSS-RC4-SHA";
    public final static String TLS1_TXT_DHE_DSS_WITH_RC4_128_SHA = "DHE-DSS-RC4-SHA";
    public final static String TLS1_TXT_RSA_WITH_AES_128_SHA = "AES128-SHA";
    public final static String TLS1_TXT_DH_DSS_WITH_AES_128_SHA = "DH-DSS-AES128-SHA";
    public final static String TLS1_TXT_DH_RSA_WITH_AES_128_SHA = "DH-RSA-AES128-SHA";
    public final static String TLS1_TXT_DHE_DSS_WITH_AES_128_SHA = "DHE-DSS-AES128-SHA";
    public final static String TLS1_TXT_DHE_RSA_WITH_AES_128_SHA = "DHE-RSA-AES128-SHA";
    public final static String TLS1_TXT_ADH_WITH_AES_128_SHA = "ADH-AES128-SHA";
    public final static String TLS1_TXT_RSA_WITH_AES_256_SHA = "AES256-SHA";
    public final static String TLS1_TXT_DH_DSS_WITH_AES_256_SHA = "DH-DSS-AES256-SHA";
    public final static String TLS1_TXT_DH_RSA_WITH_AES_256_SHA = "DH-RSA-AES256-SHA";
    public final static String TLS1_TXT_DHE_DSS_WITH_AES_256_SHA = "DHE-DSS-AES256-SHA";
    public final static String TLS1_TXT_DHE_RSA_WITH_AES_256_SHA = "DHE-RSA-AES256-SHA";
    public final static String TLS1_TXT_ADH_WITH_AES_256_SHA = "ADH-AES256-SHA";
    public final static String TLS1_TXT_ECDH_ECDSA_WITH_NULL_SHA = "ECDH-ECDSA-NULL-SHA";
    public final static String TLS1_TXT_ECDH_ECDSA_WITH_RC4_128_SHA = "ECDH-ECDSA-RC4-SHA";
    public final static String TLS1_TXT_ECDH_ECDSA_WITH_DES_192_CBC3_SHA = "ECDH-ECDSA-DES-CBC3-SHA";
    public final static String TLS1_TXT_ECDH_ECDSA_WITH_AES_128_CBC_SHA = "ECDH-ECDSA-AES128-SHA";
    public final static String TLS1_TXT_ECDH_ECDSA_WITH_AES_256_CBC_SHA = "ECDH-ECDSA-AES256-SHA";
    public final static String TLS1_TXT_ECDHE_ECDSA_WITH_NULL_SHA = "ECDHE-ECDSA-NULL-SHA";
    public final static String TLS1_TXT_ECDHE_ECDSA_WITH_RC4_128_SHA = "ECDHE-ECDSA-RC4-SHA";
    public final static String TLS1_TXT_ECDHE_ECDSA_WITH_DES_192_CBC3_SHA = "ECDHE-ECDSA-DES-CBC3-SHA";
    public final static String TLS1_TXT_ECDHE_ECDSA_WITH_AES_128_CBC_SHA = "ECDHE-ECDSA-AES128-SHA";
    public final static String TLS1_TXT_ECDHE_ECDSA_WITH_AES_256_CBC_SHA = "ECDHE-ECDSA-AES256-SHA";
    public final static String TLS1_TXT_ECDH_RSA_WITH_NULL_SHA = "ECDH-RSA-NULL-SHA";
    public final static String TLS1_TXT_ECDH_RSA_WITH_RC4_128_SHA = "ECDH-RSA-RC4-SHA";
    public final static String TLS1_TXT_ECDH_RSA_WITH_DES_192_CBC3_SHA = "ECDH-RSA-DES-CBC3-SHA";
    public final static String TLS1_TXT_ECDH_RSA_WITH_AES_128_CBC_SHA = "ECDH-RSA-AES128-SHA";
    public final static String TLS1_TXT_ECDH_RSA_WITH_AES_256_CBC_SHA = "ECDH-RSA-AES256-SHA";
    public final static String TLS1_TXT_ECDHE_RSA_WITH_NULL_SHA = "ECDHE-RSA-NULL-SHA";
    public final static String TLS1_TXT_ECDHE_RSA_WITH_RC4_128_SHA = "ECDHE-RSA-RC4-SHA";
    public final static String TLS1_TXT_ECDHE_RSA_WITH_DES_192_CBC3_SHA = "ECDHE-RSA-DES-CBC3-SHA";
    public final static String TLS1_TXT_ECDHE_RSA_WITH_AES_128_CBC_SHA = "ECDHE-RSA-AES128-SHA";
    public final static String TLS1_TXT_ECDHE_RSA_WITH_AES_256_CBC_SHA = "ECDHE-RSA-AES256-SHA";
    public final static String TLS1_TXT_ECDH_anon_WITH_NULL_SHA = "AECDH-NULL-SHA";
    public final static String TLS1_TXT_ECDH_anon_WITH_RC4_128_SHA = "AECDH-RC4-SHA";
    public final static String TLS1_TXT_ECDH_anon_WITH_DES_192_CBC3_SHA = "AECDH-DES-CBC3-SHA";
    public final static String TLS1_TXT_ECDH_anon_WITH_AES_128_CBC_SHA = "AECDH-AES128-SHA";
    public final static String TLS1_TXT_ECDH_anon_WITH_AES_256_CBC_SHA = "AECDH-AES256-SHA";

    public static class Def {
        public final int valid;
        public final String name;
        public final long id;
        public final long algorithms;
        public final long algo_strength;
        public final long algorithm2;
        public final int strength_bits;
        public final int alg_bits;
        public final long mask;
        public final long mask_strength;
        public String cipherSuite;
        public Def(int valid, String name, long id, long algorithms, long algo_strength, long algorithm2, int strength_bits, int alg_bits, long mask, long mask_strength) {
            this.valid = valid;
            this.name = name;
            this.id = id;
            this.algorithms = algorithms;
            this.algo_strength = algo_strength;
            this.algorithm2 = algorithm2;
            this.strength_bits = strength_bits;
            this.alg_bits = alg_bits;
            this.mask = mask;
            this.mask_strength = mask_strength;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            boolean ret = this == other;
            if(!ret && (other instanceof Def)) {
                ret = this.name.equals(((Def)other).name);
            }
            return ret;
        }

        @Override
        public String toString() {
            return "Cipher<" + name + ">";
        }

        // from ssl_cipher_apply_rule
        public boolean matches(Def current) {
//            ma = mask & cp->algorithms;
//            ma_s = mask_strength & cp->algo_strength;
//
//            // Select: if none of the mask bit was met from the
//            // cipher or not all of the bits were met, the
//            // selection does not apply.
//            if (((ma == 0) && (ma_s == 0)) ||
//                ((ma & algorithms) != ma) ||
//                ((ma_s & algo_strength) != ma_s))
//                continue; // does not apply
//            }
            long ma = mask & current.algorithms;
            long ma_s = mask_strength & current.algo_strength;
            if (((ma == 0) && (ma_s == 0)) ||
                    ((ma & algorithms) != ma) ||
                    ((ma_s & algo_strength) != ma_s)) {
                return false;
            }
            return true;
        }
    }

    public final static Map<String, Def> Definitions = new HashMap<String, Def>();
    public final static List<Def> Ciphers = new ArrayList<Def>();
    public final static Map<String, Def> CipherNames = new HashMap<String, Def>();
    public final static Map<String, String> SuiteToOSSL = new HashMap<String, String>();

    public static List<Def> getMatchingCiphers(String str, String[] all) {
        String[] parts = str.split("[:, ]+");
        List<Def> currentList = new ArrayList<Def>();
        Set<Def> removed = new HashSet<Def>();

        for (String part : parts) {
            if (part.equals("@STRENGTH")) {
                Collections.sort(currentList, new Comparator<Def>() {

                    public int compare(Def first, Def second) {
                        return second.strength_bits - first.strength_bits;
                    }
                });
                continue;
            }
            int index = 0;
            switch (part.charAt(0)) {
                case '!':
                    index++;
                    break;
                case '+':
                    index++;
                    break;
                case '-':
                    index++;
                    break;
            }
            List<Def> matching = getMatching(part.substring(index), all);
            if (matching != null) {
                if (index > 0) {
                    switch (part.charAt(0)) {
                        case '!':
                            currentList.removeAll(matching);
                            removed.addAll(matching);
                            break;
                        case '+':   // '+' is for moving entry in the list.
                            for (Def ele : matching) {
                                if (!removed.contains(ele) && currentList.contains(ele)) {
                                    currentList.remove(ele);
                                    currentList.add(ele);
                                }
                            }
                            break;
                        case '-':
                            currentList.removeAll(matching);
                            break;
                    }
                } else {
                    for (Def ele : matching) {
                        if (!removed.contains(ele) && !currentList.contains(ele)) {
                            currentList.add(ele);
                        }
                    }
                }
            }
        }
        return currentList;
    }

    private static List<Def> getMatching(String definition, String[] all) {
        List<Def> matching = null;
        for (String name : definition.split("[+]")) {
            Def pattern = Definitions.get(name);
            if (pattern != null) {
                if (matching == null) {
                    matching = getMatchingPattern(pattern, all);
                } else {
                    List<Def> updated = new ArrayList<Def>();
                    for (Def ele : getMatchingPattern(pattern, all)) {
                        if (matching.contains(ele)) {
                            updated.add(ele);
                        }
                    }
                    matching = updated;
                }
            }
        }
        return matching;
    }
    
    private static List<Def> getMatchingPattern(Def pattern, String[] all) {
        List<Def> matching = new ArrayList<Def>();
        for (String entry : all) {
            String ossl = SuiteToOSSL.get(entry);
            if (ossl != null) {
                Def def = CipherNames.get(ossl);
                if (def != null) {
                    def.cipherSuite = entry;
                    if (pattern.matches(def)) {
                        matching.add(def);
                    }
                }
            }
        }
        return matching;
    }

    private static void addAlias(String cipherSuite, String ossl) {
        SuiteToOSSL.put(cipherSuite, ossl);
    }

    static {
        Definitions.put(SSL_TXT_ALL,new Def(0,SSL_TXT_ALL, 0,SSL_ALL & ~SSL_eNULL & ~SSL_kECDH & ~SSL_kECDHE, SSL_ALL ,0,0,0,SSL_ALL,SSL_ALL));
        Definitions.put(SSL_TXT_CMPALL,new Def(0,SSL_TXT_CMPALL,0,SSL_eNULL,0,0,0,0,SSL_ENC_MASK,0));
        Definitions.put(SSL_TXT_CMPDEF,new Def(0,SSL_TXT_CMPDEF,0,SSL_ADH, 0,0,0,0,SSL_AUTH_MASK,0));
        Definitions.put(SSL_TXT_kKRB5,new Def(0,SSL_TXT_kKRB5,0,SSL_kKRB5,0,0,0,0,SSL_MKEY_MASK,0));
        Definitions.put(SSL_TXT_kRSA,new Def(0,SSL_TXT_kRSA,0,SSL_kRSA,  0,0,0,0,SSL_MKEY_MASK,0));
        Definitions.put(SSL_TXT_kDHr,new Def(0,SSL_TXT_kDHr,0,SSL_kDHr,  0,0,0,0,SSL_MKEY_MASK,0));
        Definitions.put(SSL_TXT_kDHd,new Def(0,SSL_TXT_kDHd,0,SSL_kDHd,  0,0,0,0,SSL_MKEY_MASK,0));
        Definitions.put(SSL_TXT_kEDH,new Def(0,SSL_TXT_kEDH,0,SSL_kEDH,  0,0,0,0,SSL_MKEY_MASK,0));
        Definitions.put(SSL_TXT_kFZA,new Def(0,SSL_TXT_kFZA,0,SSL_kFZA,  0,0,0,0,SSL_MKEY_MASK,0));
        Definitions.put(SSL_TXT_DH,new Def(0,SSL_TXT_DH,	0,SSL_DH,    0,0,0,0,SSL_MKEY_MASK,0));
        Definitions.put(SSL_TXT_ECC,new Def(0,SSL_TXT_ECC,	0,(SSL_kECDH|SSL_kECDHE), 0,0,0,0,SSL_MKEY_MASK,0));
        Definitions.put(SSL_TXT_EDH,new Def(0,SSL_TXT_EDH,	0,SSL_EDH,   0,0,0,0,SSL_MKEY_MASK|SSL_AUTH_MASK,0));
        Definitions.put(SSL_TXT_aKRB5,new Def(0,SSL_TXT_aKRB5,0,SSL_aKRB5,0,0,0,0,SSL_AUTH_MASK,0));
        Definitions.put(SSL_TXT_aRSA,new Def(0,SSL_TXT_aRSA,0,SSL_aRSA,  0,0,0,0,SSL_AUTH_MASK,0));
        Definitions.put(SSL_TXT_aDSS,new Def(0,SSL_TXT_aDSS,0,SSL_aDSS,  0,0,0,0,SSL_AUTH_MASK,0));
        Definitions.put(SSL_TXT_aFZA,new Def(0,SSL_TXT_aFZA,0,SSL_aFZA,  0,0,0,0,SSL_AUTH_MASK,0));
        Definitions.put(SSL_TXT_aNULL,new Def(0,SSL_TXT_aNULL,0,SSL_aNULL,0,0,0,0,SSL_AUTH_MASK,0));
        Definitions.put(SSL_TXT_aDH,new Def(0,SSL_TXT_aDH, 0,SSL_aDH,   0,0,0,0,SSL_AUTH_MASK,0));
        Definitions.put(SSL_TXT_DSS,new Def(0,SSL_TXT_DSS,	0,SSL_DSS,   0,0,0,0,SSL_AUTH_MASK,0));
        Definitions.put(SSL_TXT_DES,new Def(0,SSL_TXT_DES,	0,SSL_DES,   0,0,0,0,SSL_ENC_MASK,0));
        Definitions.put(SSL_TXT_3DES,new Def(0,SSL_TXT_3DES,0,SSL_3DES,  0,0,0,0,SSL_ENC_MASK,0));
        Definitions.put(SSL_TXT_RC4,new Def(0,SSL_TXT_RC4,	0,SSL_RC4,   0,0,0,0,SSL_ENC_MASK,0));
        Definitions.put(SSL_TXT_RC2,new Def(0,SSL_TXT_RC2,	0,SSL_RC2,   0,0,0,0,SSL_ENC_MASK,0));
        Definitions.put(SSL_TXT_IDEA,new Def(0,SSL_TXT_IDEA,0,SSL_IDEA,  0,0,0,0,SSL_ENC_MASK,0));
        Definitions.put(SSL_TXT_eNULL,new Def(0,SSL_TXT_eNULL,0,SSL_eNULL,0,0,0,0,SSL_ENC_MASK,0));
        Definitions.put(SSL_TXT_eFZA,new Def(0,SSL_TXT_eFZA,0,SSL_eFZA,  0,0,0,0,SSL_ENC_MASK,0));
        Definitions.put(SSL_TXT_AES,new Def(0,SSL_TXT_AES,	0,SSL_AES,   0,0,0,0,SSL_ENC_MASK,0));
        Definitions.put(SSL_TXT_MD5,new Def(0,SSL_TXT_MD5,	0,SSL_MD5,   0,0,0,0,SSL_MAC_MASK,0));
        Definitions.put(SSL_TXT_SHA1,new Def(0,SSL_TXT_SHA1,0,SSL_SHA1,  0,0,0,0,SSL_MAC_MASK,0));
        Definitions.put(SSL_TXT_SHA,new Def(0,SSL_TXT_SHA,	0,SSL_SHA,   0,0,0,0,SSL_MAC_MASK,0));
        Definitions.put(SSL_TXT_NULL,new Def(0,SSL_TXT_NULL,0,SSL_NULL,  0,0,0,0,SSL_ENC_MASK,0));
        Definitions.put(SSL_TXT_KRB5,new Def(0,SSL_TXT_KRB5,0,SSL_KRB5,  0,0,0,0,SSL_AUTH_MASK|SSL_MKEY_MASK,0));
        Definitions.put(SSL_TXT_RSA,new Def(0,SSL_TXT_RSA,	0,SSL_RSA,   0,0,0,0,SSL_AUTH_MASK|SSL_MKEY_MASK,0));
        Definitions.put(SSL_TXT_ADH,new Def(0,SSL_TXT_ADH,	0,SSL_ADH,   0,0,0,0,SSL_AUTH_MASK|SSL_MKEY_MASK,0));
        Definitions.put(SSL_TXT_FZA,new Def(0,SSL_TXT_FZA,	0,SSL_FZA,   0,0,0,0,SSL_AUTH_MASK|SSL_MKEY_MASK|SSL_ENC_MASK,0));
        Definitions.put(SSL_TXT_SSLV2,new Def(0,SSL_TXT_SSLV2, 0,SSL_SSLV2, 0,0,0,0,SSL_SSL_MASK,0));
        Definitions.put(SSL_TXT_SSLV3,new Def(0,SSL_TXT_SSLV3, 0,SSL_SSLV3, 0,0,0,0,SSL_SSL_MASK,0));
        Definitions.put(SSL_TXT_TLSV1,new Def(0,SSL_TXT_TLSV1, 0,SSL_TLSV1, 0,0,0,0,SSL_SSL_MASK,0));
        Definitions.put(SSL_TXT_EXP,new Def(0,SSL_TXT_EXP   ,0, 0,SSL_EXPORT, 0,0,0,0,SSL_EXP_MASK));
        Definitions.put(SSL_TXT_EXPORT,new Def(0,SSL_TXT_EXPORT,0, 0,SSL_EXPORT, 0,0,0,0,SSL_EXP_MASK));
        Definitions.put(SSL_TXT_EXP40,new Def(0,SSL_TXT_EXP40, 0, 0, SSL_EXP40, 0,0,0,0,SSL_STRONG_MASK));
        Definitions.put(SSL_TXT_EXP56,new Def(0,SSL_TXT_EXP56, 0, 0, SSL_EXP56, 0,0,0,0,SSL_STRONG_MASK));
        Definitions.put(SSL_TXT_LOW,new Def(0,SSL_TXT_LOW,   0, 0,   SSL_LOW, 0,0,0,0,SSL_STRONG_MASK));
        Definitions.put(SSL_TXT_MEDIUM,new Def(0,SSL_TXT_MEDIUM,0, 0,SSL_MEDIUM, 0,0,0,0,SSL_STRONG_MASK));
        Definitions.put(SSL_TXT_HIGH,new Def(0,SSL_TXT_HIGH,  0, 0,  SSL_HIGH, 0,0,0,0,SSL_STRONG_MASK));

        /* Cipher 01 */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_RSA_NULL_MD5,
                            SSL3_CK_RSA_NULL_MD5,
                            SSL_kRSA|SSL_aRSA|SSL_eNULL |SSL_MD5|SSL_SSLV3,
                            SSL_NOT_EXP|SSL_STRONG_NONE,
                            0,
                            0,
                            0,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 02 */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_RSA_NULL_SHA,
                            SSL3_CK_RSA_NULL_SHA,
                            SSL_kRSA|SSL_aRSA|SSL_eNULL |SSL_SHA1|SSL_SSLV3,
                            SSL_NOT_EXP|SSL_STRONG_NONE,
                            0,
                            0,
                            0,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 03 */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_RSA_RC4_40_MD5,
                            SSL3_CK_RSA_RC4_40_MD5,
                            SSL_kRSA|SSL_aRSA|SSL_RC4  |SSL_MD5 |SSL_SSLV3,
                            SSL_EXPORT|SSL_EXP40,
                            0,
                            40,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 04 */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_RSA_RC4_128_MD5,
                            SSL3_CK_RSA_RC4_128_MD5,
                            SSL_kRSA|SSL_aRSA|SSL_RC4  |SSL_MD5|SSL_SSLV3,
                            SSL_NOT_EXP|SSL_MEDIUM,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 05 */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_RSA_RC4_128_SHA,
                            SSL3_CK_RSA_RC4_128_SHA,
                            SSL_kRSA|SSL_aRSA|SSL_RC4  |SSL_SHA1|SSL_SSLV3,
                            SSL_NOT_EXP|SSL_MEDIUM,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 06 */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_RSA_RC2_40_MD5,
                            SSL3_CK_RSA_RC2_40_MD5,
                            SSL_kRSA|SSL_aRSA|SSL_RC2  |SSL_MD5 |SSL_SSLV3,
                            SSL_EXPORT|SSL_EXP40,
                            0,
                            40,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 07 */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_RSA_IDEA_128_SHA,
                            SSL3_CK_RSA_IDEA_128_SHA,
                            SSL_kRSA|SSL_aRSA|SSL_IDEA |SSL_SHA1|SSL_SSLV3,
                            SSL_NOT_EXP|SSL_MEDIUM,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 08 */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_RSA_DES_40_CBC_SHA,
                            SSL3_CK_RSA_DES_40_CBC_SHA,
                            SSL_kRSA|SSL_aRSA|SSL_DES|SSL_SHA1|SSL_SSLV3,
                            SSL_EXPORT|SSL_EXP40,
                            0,
                            40,
                            56,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 09 */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_RSA_DES_64_CBC_SHA,
                            SSL3_CK_RSA_DES_64_CBC_SHA,
                            SSL_kRSA|SSL_aRSA|SSL_DES  |SSL_SHA1|SSL_SSLV3,
                            SSL_NOT_EXP|SSL_LOW,
                            0,
                            56,
                            56,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 0A */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_RSA_DES_192_CBC3_SHA,
                            SSL3_CK_RSA_DES_192_CBC3_SHA,
                            SSL_kRSA|SSL_aRSA|SSL_3DES |SSL_SHA1|SSL_SSLV3,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            168,
                            168,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* The DH ciphers */
        /* Cipher 0B */
        Ciphers.add(new Def(
                            0,
                            SSL3_TXT_DH_DSS_DES_40_CBC_SHA,
                            SSL3_CK_DH_DSS_DES_40_CBC_SHA,
                            SSL_kDHd |SSL_aDH|SSL_DES|SSL_SHA1|SSL_SSLV3,
                            SSL_EXPORT|SSL_EXP40,
                            0,
                            40,
                            56,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 0C */
        Ciphers.add(new Def(
                            0,
                            SSL3_TXT_DH_DSS_DES_64_CBC_SHA,
                            SSL3_CK_DH_DSS_DES_64_CBC_SHA,
                            SSL_kDHd |SSL_aDH|SSL_DES  |SSL_SHA1|SSL_SSLV3,
                            SSL_NOT_EXP|SSL_LOW,
                            0,
                            56,
                            56,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 0D */
        Ciphers.add(new Def(
                            0,
                            SSL3_TXT_DH_DSS_DES_192_CBC3_SHA,
                            SSL3_CK_DH_DSS_DES_192_CBC3_SHA,
                            SSL_kDHd |SSL_aDH|SSL_3DES |SSL_SHA1|SSL_SSLV3,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            168,
                            168,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 0E */
        Ciphers.add(new Def(
                            0,
                            SSL3_TXT_DH_RSA_DES_40_CBC_SHA,
                            SSL3_CK_DH_RSA_DES_40_CBC_SHA,
                            SSL_kDHr |SSL_aDH|SSL_DES|SSL_SHA1|SSL_SSLV3,
                            SSL_EXPORT|SSL_EXP40,
                            0,
                            40,
                            56,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 0F */
        Ciphers.add(new Def(
                            0,
                            SSL3_TXT_DH_RSA_DES_64_CBC_SHA,
                            SSL3_CK_DH_RSA_DES_64_CBC_SHA,
                            SSL_kDHr |SSL_aDH|SSL_DES  |SSL_SHA1|SSL_SSLV3,
                            SSL_NOT_EXP|SSL_LOW,
                            0,
                            56,
                            56,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 10 */
        Ciphers.add(new Def(
                            0,
                            SSL3_TXT_DH_RSA_DES_192_CBC3_SHA,
                            SSL3_CK_DH_RSA_DES_192_CBC3_SHA,
                            SSL_kDHr |SSL_aDH|SSL_3DES |SSL_SHA1|SSL_SSLV3,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            168,
                            168,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* The Ephemeral DH ciphers */
        /* Cipher 11 */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_EDH_DSS_DES_40_CBC_SHA,
                            SSL3_CK_EDH_DSS_DES_40_CBC_SHA,
                            SSL_kEDH|SSL_aDSS|SSL_DES|SSL_SHA1|SSL_SSLV3,
                            SSL_EXPORT|SSL_EXP40,
                            0,
                            40,
                            56,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 12 */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_EDH_DSS_DES_64_CBC_SHA,
                            SSL3_CK_EDH_DSS_DES_64_CBC_SHA,
                            SSL_kEDH|SSL_aDSS|SSL_DES  |SSL_SHA1|SSL_SSLV3,
                            SSL_NOT_EXP|SSL_LOW,
                            0,
                            56,
                            56,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 13 */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_EDH_DSS_DES_192_CBC3_SHA,
                            SSL3_CK_EDH_DSS_DES_192_CBC3_SHA,
                            SSL_kEDH|SSL_aDSS|SSL_3DES |SSL_SHA1|SSL_SSLV3,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            168,
                            168,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 14 */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_EDH_RSA_DES_40_CBC_SHA,
                            SSL3_CK_EDH_RSA_DES_40_CBC_SHA,
                            SSL_kEDH|SSL_aRSA|SSL_DES|SSL_SHA1|SSL_SSLV3,
                            SSL_EXPORT|SSL_EXP40,
                            0,
                            40,
                            56,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 15 */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_EDH_RSA_DES_64_CBC_SHA,
                            SSL3_CK_EDH_RSA_DES_64_CBC_SHA,
                            SSL_kEDH|SSL_aRSA|SSL_DES  |SSL_SHA1|SSL_SSLV3,
                            SSL_NOT_EXP|SSL_LOW,
                            0,
                            56,
                            56,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 16 */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_EDH_RSA_DES_192_CBC3_SHA,
                            SSL3_CK_EDH_RSA_DES_192_CBC3_SHA,
                            SSL_kEDH|SSL_aRSA|SSL_3DES |SSL_SHA1|SSL_SSLV3,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            168,
                            168,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 17 */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_ADH_RC4_40_MD5,
                            SSL3_CK_ADH_RC4_40_MD5,
                            SSL_kEDH |SSL_aNULL|SSL_RC4  |SSL_MD5 |SSL_SSLV3,
                            SSL_EXPORT|SSL_EXP40,
                            0,
                            40,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 18 */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_ADH_RC4_128_MD5,
                            SSL3_CK_ADH_RC4_128_MD5,
                            SSL_kEDH |SSL_aNULL|SSL_RC4  |SSL_MD5 |SSL_SSLV3,
                            SSL_NOT_EXP|SSL_MEDIUM,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 19 */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_ADH_DES_40_CBC_SHA,
                            SSL3_CK_ADH_DES_40_CBC_SHA,
                            SSL_kEDH |SSL_aNULL|SSL_DES|SSL_SHA1|SSL_SSLV3,
                            SSL_EXPORT|SSL_EXP40,
                            0,
                            40,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 1A */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_ADH_DES_64_CBC_SHA,
                            SSL3_CK_ADH_DES_64_CBC_SHA,
                            SSL_kEDH |SSL_aNULL|SSL_DES  |SSL_SHA1|SSL_SSLV3,
                            SSL_NOT_EXP|SSL_LOW,
                            0,
                            56,
                            56,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 1B */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_ADH_DES_192_CBC_SHA,
                            SSL3_CK_ADH_DES_192_CBC_SHA,
                            SSL_kEDH |SSL_aNULL|SSL_3DES |SSL_SHA1|SSL_SSLV3,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            168,
                            168,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Fortezza */
        /* Cipher 1C */
        Ciphers.add(new Def(
                            0,
                            SSL3_TXT_FZA_DMS_NULL_SHA,
                            SSL3_CK_FZA_DMS_NULL_SHA,
                            SSL_kFZA|SSL_aFZA |SSL_eNULL |SSL_SHA1|SSL_SSLV3,
                            SSL_NOT_EXP|SSL_STRONG_NONE,
                            0,
                            0,
                            0,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher 1D */
        Ciphers.add(new Def(
                            0,
                            SSL3_TXT_FZA_DMS_FZA_SHA,
                            SSL3_CK_FZA_DMS_FZA_SHA,
                            SSL_kFZA|SSL_aFZA |SSL_eFZA |SSL_SHA1|SSL_SSLV3,
                            SSL_NOT_EXP|SSL_STRONG_NONE,
                            0,
                            0,
                            0,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher 1E VRS */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_KRB5_DES_64_CBC_SHA,
                            SSL3_CK_KRB5_DES_64_CBC_SHA,
                            SSL_kKRB5|SSL_aKRB5|  SSL_DES|SSL_SHA1   |SSL_SSLV3,
                            SSL_NOT_EXP|SSL_LOW,
                            0,
                            56,
                            56,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher 1F VRS */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_KRB5_DES_192_CBC3_SHA,
                            SSL3_CK_KRB5_DES_192_CBC3_SHA,
                            SSL_kKRB5|SSL_aKRB5|  SSL_3DES|SSL_SHA1  |SSL_SSLV3,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            112,
                            168,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher 20 VRS */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_KRB5_RC4_128_SHA,
                            SSL3_CK_KRB5_RC4_128_SHA,
                            SSL_kKRB5|SSL_aKRB5|  SSL_RC4|SSL_SHA1  |SSL_SSLV3,
                            SSL_NOT_EXP|SSL_MEDIUM,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher 21 VRS */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_KRB5_IDEA_128_CBC_SHA,
                            SSL3_CK_KRB5_IDEA_128_CBC_SHA,
                            SSL_kKRB5|SSL_aKRB5|  SSL_IDEA|SSL_SHA1  |SSL_SSLV3,
                            SSL_NOT_EXP|SSL_MEDIUM,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher 22 VRS */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_KRB5_DES_64_CBC_MD5,
                            SSL3_CK_KRB5_DES_64_CBC_MD5,
                            SSL_kKRB5|SSL_aKRB5|  SSL_DES|SSL_MD5    |SSL_SSLV3,
                            SSL_NOT_EXP|SSL_LOW,
                            0,
                            56,
                            56,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher 23 VRS */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_KRB5_DES_192_CBC3_MD5,
                            SSL3_CK_KRB5_DES_192_CBC3_MD5,
                            SSL_kKRB5|SSL_aKRB5|  SSL_3DES|SSL_MD5   |SSL_SSLV3,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            112,
                            168,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher 24 VRS */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_KRB5_RC4_128_MD5,
                            SSL3_CK_KRB5_RC4_128_MD5,
                            SSL_kKRB5|SSL_aKRB5|  SSL_RC4|SSL_MD5  |SSL_SSLV3,
                            SSL_NOT_EXP|SSL_MEDIUM,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher 25 VRS */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_KRB5_IDEA_128_CBC_MD5,
                            SSL3_CK_KRB5_IDEA_128_CBC_MD5,
                            SSL_kKRB5|SSL_aKRB5|  SSL_IDEA|SSL_MD5  |SSL_SSLV3,
                            SSL_NOT_EXP|SSL_MEDIUM,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher 26 VRS */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_KRB5_DES_40_CBC_SHA,
                            SSL3_CK_KRB5_DES_40_CBC_SHA,
                            SSL_kKRB5|SSL_aKRB5|  SSL_DES|SSL_SHA1   |SSL_SSLV3,
                            SSL_EXPORT|SSL_EXP40,
                            0,
                            40,
                            56,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher 27 VRS */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_KRB5_RC2_40_CBC_SHA,
                            SSL3_CK_KRB5_RC2_40_CBC_SHA,
                            SSL_kKRB5|SSL_aKRB5|  SSL_RC2|SSL_SHA1   |SSL_SSLV3,
                            SSL_EXPORT|SSL_EXP40,
                            0,
                            40,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher 28 VRS */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_KRB5_RC4_40_SHA,
                            SSL3_CK_KRB5_RC4_40_SHA,
                            SSL_kKRB5|SSL_aKRB5|  SSL_RC4|SSL_SHA1   |SSL_SSLV3,
                            SSL_EXPORT|SSL_EXP40,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher 29 VRS */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_KRB5_DES_40_CBC_MD5,
                            SSL3_CK_KRB5_DES_40_CBC_MD5,
                            SSL_kKRB5|SSL_aKRB5|  SSL_DES|SSL_MD5    |SSL_SSLV3,
                            SSL_EXPORT|SSL_EXP40,
                            0,
                            40,
                            56,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher 2A VRS */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_KRB5_RC2_40_CBC_MD5,
                            SSL3_CK_KRB5_RC2_40_CBC_MD5,
                            SSL_kKRB5|SSL_aKRB5|  SSL_RC2|SSL_MD5    |SSL_SSLV3,
                            SSL_EXPORT|SSL_EXP40,
                            0,
                            40,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher 2B VRS */
        Ciphers.add(new Def(
                            1,
                            SSL3_TXT_KRB5_RC4_40_MD5,
                            SSL3_CK_KRB5_RC4_40_MD5,
                            SSL_kKRB5|SSL_aKRB5|  SSL_RC4|SSL_MD5    |SSL_SSLV3,
                            SSL_EXPORT|SSL_EXP40,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher 2F */
        Ciphers.add(new Def(
                            1,
                            TLS1_TXT_RSA_WITH_AES_128_SHA,
                            TLS1_CK_RSA_WITH_AES_128_SHA,
                            SSL_kRSA|SSL_aRSA|SSL_AES|SSL_SHA |SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 30 */
        Ciphers.add(new Def(
                            0,
                            TLS1_TXT_DH_DSS_WITH_AES_128_SHA,
                            TLS1_CK_DH_DSS_WITH_AES_128_SHA,
                            SSL_kDHd|SSL_aDH|SSL_AES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 31 */
        Ciphers.add(new Def(
                            0,
                            TLS1_TXT_DH_RSA_WITH_AES_128_SHA,
                            TLS1_CK_DH_RSA_WITH_AES_128_SHA,
                            SSL_kDHr|SSL_aDH|SSL_AES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 32 */
        Ciphers.add(new Def(
                            1,
                            TLS1_TXT_DHE_DSS_WITH_AES_128_SHA,
                            TLS1_CK_DHE_DSS_WITH_AES_128_SHA,
                            SSL_kEDH|SSL_aDSS|SSL_AES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 33 */
        Ciphers.add(new Def(
                            1,
                            TLS1_TXT_DHE_RSA_WITH_AES_128_SHA,
                            TLS1_CK_DHE_RSA_WITH_AES_128_SHA,
                            SSL_kEDH|SSL_aRSA|SSL_AES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 34 */
        Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ADH_WITH_AES_128_SHA,
                            TLS1_CK_ADH_WITH_AES_128_SHA,
                            SSL_kEDH|SSL_aNULL|SSL_AES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher 35 */
        Ciphers.add(new Def(
                            1,
                            TLS1_TXT_RSA_WITH_AES_256_SHA,
                            TLS1_CK_RSA_WITH_AES_256_SHA,
                            SSL_kRSA|SSL_aRSA|SSL_AES|SSL_SHA |SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            256,
                            256,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 36 */
        Ciphers.add(new Def(
                            0,
                            TLS1_TXT_DH_DSS_WITH_AES_256_SHA,
                            TLS1_CK_DH_DSS_WITH_AES_256_SHA,
                            SSL_kDHd|SSL_aDH|SSL_AES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            256,
                            256,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 37 */
        Ciphers.add(new Def(
                            0,
                            TLS1_TXT_DH_RSA_WITH_AES_256_SHA,
                            TLS1_CK_DH_RSA_WITH_AES_256_SHA,
                            SSL_kDHr|SSL_aDH|SSL_AES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            256,
                            256,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 38 */
        Ciphers.add(new Def(
                            1,
                            TLS1_TXT_DHE_DSS_WITH_AES_256_SHA,
                            TLS1_CK_DHE_DSS_WITH_AES_256_SHA,
                            SSL_kEDH|SSL_aDSS|SSL_AES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            256,
                            256,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 39 */
        Ciphers.add(new Def(
                            1,
                            TLS1_TXT_DHE_RSA_WITH_AES_256_SHA,
                            TLS1_CK_DHE_RSA_WITH_AES_256_SHA,
                            SSL_kEDH|SSL_aRSA|SSL_AES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            256,
                            256,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 3A */
        Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ADH_WITH_AES_256_SHA,
                            TLS1_CK_ADH_WITH_AES_256_SHA,
                            SSL_kEDH|SSL_aNULL|SSL_AES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            256,
                            256,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* New TLS Export CipherSuites */
        /* Cipher 60 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_RSA_EXPORT1024_WITH_RC4_56_MD5,
                            TLS1_CK_RSA_EXPORT1024_WITH_RC4_56_MD5,
                            SSL_kRSA|SSL_aRSA|SSL_RC4|SSL_MD5|SSL_TLSV1,
                            SSL_EXPORT|SSL_EXP56,
                            0,
                            56,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 61 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_RSA_EXPORT1024_WITH_RC2_CBC_56_MD5,
                            TLS1_CK_RSA_EXPORT1024_WITH_RC2_CBC_56_MD5,
                            SSL_kRSA|SSL_aRSA|SSL_RC2|SSL_MD5|SSL_TLSV1,
                            SSL_EXPORT|SSL_EXP56,
                            0,
                            56,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 62 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_RSA_EXPORT1024_WITH_DES_CBC_SHA,
                            TLS1_CK_RSA_EXPORT1024_WITH_DES_CBC_SHA,
                            SSL_kRSA|SSL_aRSA|SSL_DES|SSL_SHA|SSL_TLSV1,
                            SSL_EXPORT|SSL_EXP56,
                            0,
                            56,
                            56,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 63 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_DHE_DSS_EXPORT1024_WITH_DES_CBC_SHA,
                            TLS1_CK_DHE_DSS_EXPORT1024_WITH_DES_CBC_SHA,
                            SSL_kEDH|SSL_aDSS|SSL_DES|SSL_SHA|SSL_TLSV1,
                            SSL_EXPORT|SSL_EXP56,
                            0,
                            56,
                            56,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 64 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_RSA_EXPORT1024_WITH_RC4_56_SHA,
                            TLS1_CK_RSA_EXPORT1024_WITH_RC4_56_SHA,
                            SSL_kRSA|SSL_aRSA|SSL_RC4|SSL_SHA|SSL_TLSV1,
                            SSL_EXPORT|SSL_EXP56,
                            0,
                            56,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 65 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_DHE_DSS_EXPORT1024_WITH_RC4_56_SHA,
                            TLS1_CK_DHE_DSS_EXPORT1024_WITH_RC4_56_SHA,
                            SSL_kEDH|SSL_aDSS|SSL_RC4|SSL_SHA|SSL_TLSV1,
                            SSL_EXPORT|SSL_EXP56,
                            0,
                            56,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher 66 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_DHE_DSS_WITH_RC4_128_SHA,
                            TLS1_CK_DHE_DSS_WITH_RC4_128_SHA,
                            SSL_kEDH|SSL_aDSS|SSL_RC4|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_MEDIUM,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));
        /* Cipher C001 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDH_ECDSA_WITH_NULL_SHA,
                            TLS1_CK_ECDH_ECDSA_WITH_NULL_SHA,
                            SSL_kECDH|SSL_aECDSA|SSL_eNULL|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP,
                            0,
                            0,
                            0,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C002 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDH_ECDSA_WITH_RC4_128_SHA,
                            TLS1_CK_ECDH_ECDSA_WITH_RC4_128_SHA,
                            SSL_kECDH|SSL_aECDSA|SSL_RC4|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C003 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDH_ECDSA_WITH_DES_192_CBC3_SHA,
                            TLS1_CK_ECDH_ECDSA_WITH_DES_192_CBC3_SHA,
                            SSL_kECDH|SSL_aECDSA|SSL_3DES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            168,
                            168,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C004 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDH_ECDSA_WITH_AES_128_CBC_SHA,
                            TLS1_CK_ECDH_ECDSA_WITH_AES_128_CBC_SHA,
                            SSL_kECDH|SSL_aECDSA|SSL_AES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C005 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDH_ECDSA_WITH_AES_256_CBC_SHA,
                            TLS1_CK_ECDH_ECDSA_WITH_AES_256_CBC_SHA,
                            SSL_kECDH|SSL_aECDSA|SSL_AES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            256,
                            256,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C006 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDHE_ECDSA_WITH_NULL_SHA,
                            TLS1_CK_ECDHE_ECDSA_WITH_NULL_SHA,
                            SSL_kECDHE|SSL_aECDSA|SSL_eNULL|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP,
                            0,
                            0,
                            0,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C007 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDHE_ECDSA_WITH_RC4_128_SHA,
                            TLS1_CK_ECDHE_ECDSA_WITH_RC4_128_SHA,
                            SSL_kECDHE|SSL_aECDSA|SSL_RC4|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C008 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDHE_ECDSA_WITH_DES_192_CBC3_SHA,
                            TLS1_CK_ECDHE_ECDSA_WITH_DES_192_CBC3_SHA,
                            SSL_kECDHE|SSL_aECDSA|SSL_3DES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            168,
                            168,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C009 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
                            TLS1_CK_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
                            SSL_kECDHE|SSL_aECDSA|SSL_AES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C00A */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,
                            TLS1_CK_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,
                            SSL_kECDHE|SSL_aECDSA|SSL_AES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            256,
                            256,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C00B */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDH_RSA_WITH_NULL_SHA,
                            TLS1_CK_ECDH_RSA_WITH_NULL_SHA,
                            SSL_kECDH|SSL_aRSA|SSL_eNULL|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP,
                            0,
                            0,
                            0,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C00C */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDH_RSA_WITH_RC4_128_SHA,
                            TLS1_CK_ECDH_RSA_WITH_RC4_128_SHA,
                            SSL_kECDH|SSL_aRSA|SSL_RC4|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C00D */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDH_RSA_WITH_DES_192_CBC3_SHA,
                            TLS1_CK_ECDH_RSA_WITH_DES_192_CBC3_SHA,
                            SSL_kECDH|SSL_aRSA|SSL_3DES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            168,
                            168,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C00E */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDH_RSA_WITH_AES_128_CBC_SHA,
                            TLS1_CK_ECDH_RSA_WITH_AES_128_CBC_SHA,
                            SSL_kECDH|SSL_aRSA|SSL_AES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C00F */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDH_RSA_WITH_AES_256_CBC_SHA,
                            TLS1_CK_ECDH_RSA_WITH_AES_256_CBC_SHA,
                            SSL_kECDH|SSL_aRSA|SSL_AES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            256,
                            256,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C010 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDHE_RSA_WITH_NULL_SHA,
                            TLS1_CK_ECDHE_RSA_WITH_NULL_SHA,
                            SSL_kECDHE|SSL_aRSA|SSL_eNULL|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP,
                            0,
                            0,
                            0,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C011 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDHE_RSA_WITH_RC4_128_SHA,
                            TLS1_CK_ECDHE_RSA_WITH_RC4_128_SHA,
                            SSL_kECDHE|SSL_aRSA|SSL_RC4|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C012 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDHE_RSA_WITH_DES_192_CBC3_SHA,
                            TLS1_CK_ECDHE_RSA_WITH_DES_192_CBC3_SHA,
                            SSL_kECDHE|SSL_aRSA|SSL_3DES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            168,
                            168,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C013 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDHE_RSA_WITH_AES_128_CBC_SHA,
                            TLS1_CK_ECDHE_RSA_WITH_AES_128_CBC_SHA,
                            SSL_kECDHE|SSL_aRSA|SSL_AES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C014 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDHE_RSA_WITH_AES_256_CBC_SHA,
                            TLS1_CK_ECDHE_RSA_WITH_AES_256_CBC_SHA,
                            SSL_kECDHE|SSL_aRSA|SSL_AES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            256,
                            256,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C015 */
        Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDH_anon_WITH_NULL_SHA,
                            TLS1_CK_ECDH_anon_WITH_NULL_SHA,
                            SSL_kECDHE|SSL_aNULL|SSL_eNULL|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP,
                            0,
                            0,
                            0,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C016 */
        Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDH_anon_WITH_RC4_128_SHA,
                            TLS1_CK_ECDH_anon_WITH_RC4_128_SHA,
                            SSL_kECDHE|SSL_aNULL|SSL_RC4|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C017 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDH_anon_WITH_DES_192_CBC3_SHA,
                            TLS1_CK_ECDH_anon_WITH_DES_192_CBC3_SHA,
                            SSL_kECDHE|SSL_aNULL|SSL_3DES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            168,
                            168,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C018 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDH_anon_WITH_AES_128_CBC_SHA,
                            TLS1_CK_ECDH_anon_WITH_AES_128_CBC_SHA,
                            SSL_kECDHE|SSL_aNULL|SSL_AES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            128,
                            128,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        /* Cipher C019 */
	    Ciphers.add(new Def(
                            1,
                            TLS1_TXT_ECDH_anon_WITH_AES_256_CBC_SHA,
                            TLS1_CK_ECDH_anon_WITH_AES_256_CBC_SHA,
                            SSL_kECDHE|SSL_aNULL|SSL_AES|SSL_SHA|SSL_TLSV1,
                            SSL_NOT_EXP|SSL_HIGH,
                            0,
                            256,
                            256,
                            SSL_ALL_CIPHERS,
                            SSL_ALL_STRENGTHS
                            ));

        for(Def def : Ciphers) {
            CipherNames.put(def.name, def);
        }

        addAlias("SSL_RSA_WITH_NULL_MD5","NULL-MD5");
        addAlias("SSL_RSA_WITH_NULL_SHA","NULL-SHA");
        addAlias("SSL_RSA_EXPORT_WITH_RC4_40_MD5","EXP-RC4-MD5");
        addAlias("SSL_RSA_WITH_RC4_128_MD5","RC4-MD5");
        addAlias("SSL_RSA_WITH_RC4_128_SHA","RC4-SHA");
        addAlias("SSL_RSA_EXPORT_WITH_RC2_CBC_40_MD5","EXP-RC2-CBC-MD5");
        addAlias("SSL_RSA_WITH_IDEA_CBC_SHA","IDEA-CBC-SHA");
        addAlias("SSL_RSA_EXPORT_WITH_DES40_CBC_SHA","EXP-DES-CBC-SHA");
        addAlias("SSL_RSA_WITH_DES_CBC_SHA","DES-CBC-SHA");
        addAlias("SSL_RSA_WITH_3DES_EDE_CBC_SHA","DES-CBC3-SHA");
        addAlias("SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA","EXP-EDH-DSS-DES-CBC-SHA");
        addAlias("SSL_DHE_DSS_WITH_DES_CBC_SHA","EDH-DSS-CBC-SHA");
        addAlias("SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA","EDH-DSS-DES-CBC3-SHA");
        addAlias("SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA","EXP-EDH-RSA-DES-CBC-SHA");
        addAlias("SSL_DHE_RSA_WITH_DES_CBC_SHA","EDH-RSA-DES-CBC-SHA");
        addAlias("SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA","EDH-RSA-DES-CBC3-SHA");
        addAlias("SSL_DH_anon_EXPORT_WITH_RC4_40_MD5","EXP-ADH-RC4-MD5");
        addAlias("SSL_DH_anon_WITH_RC4_128_MD5","ADH-RC4-MD5");
        addAlias("SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA","EXP-ADH-DES-CBC-SHA");
        addAlias("SSL_DH_anon_WITH_DES_CBC_SHA","ADH-DES-CBC-SHA");
        addAlias("SSL_DH_anon_WITH_3DES_EDE_CBC_SHA","ADH-DES-CBC3-SHA");
        addAlias("TLS_RSA_WITH_NULL_MD5","NULL-MD5");
        addAlias("TLS_RSA_WITH_NULL_SHA","NULL-SHA");
        addAlias("TLS_RSA_EXPORT_WITH_RC4_40_MD5","EXP-RC4-MD5");
        addAlias("TLS_RSA_WITH_RC4_128_MD5","RC4-MD5");
        addAlias("TLS_RSA_WITH_RC4_128_SHA","RC4-SHA");
        addAlias("TLS_RSA_EXPORT_WITH_RC2_CBC_40_MD5","EXP-RC2-CBC-MD5");
        addAlias("TLS_RSA_WITH_IDEA_CBC_SHA","IDEA-CBC-SHA");
        addAlias("TLS_RSA_EXPORT_WITH_DES40_CBC_SHA","EXP-DES-CBC-SHA");
        addAlias("TLS_RSA_WITH_DES_CBC_SHA","DES-CBC-SHA");
        addAlias("TLS_RSA_WITH_3DES_EDE_CBC_SHA","DES-CBC3-SHA");
        addAlias("TLS_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA","EXP-EDH-DSS-DES-CBC-SHA");
        addAlias("TLS_DHE_DSS_WITH_DES_CBC_SHA","EDH-DSS-CBC-SHA");
        addAlias("TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA","EDH-DSS-DES-CBC3-SHA");
        addAlias("TLS_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA","EXP-EDH-RSA-DES-CBC-SHA");
        addAlias("TLS_DHE_RSA_WITH_DES_CBC_SHA","EDH-RSA-DES-CBC-SHA");
        addAlias("TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA","EDH-RSA-DES-CBC3-SHA");
        addAlias("TLS_DH_anon_EXPORT_WITH_RC4_40_MD5","EXP-ADH-RC4-MD5");
        addAlias("TLS_DH_anon_WITH_RC4_128_MD5","ADH-RC4-MD5");
        addAlias("TLS_DH_anon_EXPORT_WITH_DES40_CBC_SHA","EXP-ADH-DES-CBC-SHA");
        addAlias("TLS_DH_anon_WITH_DES_CBC_SHA","ADH-DES-CBC-SHA");
        addAlias("TLS_DH_anon_WITH_3DES_EDE_CBC_SHA","ADH-DES-CBC3-SHA");
        addAlias("TLS_RSA_WITH_AES_128_CBC_SHA","AES128-SHA");
        addAlias("TLS_RSA_WITH_AES_256_CBC_SHA","AES256-SHA");
        addAlias("TLS_DH_DSS_WITH_AES_128_CBC_SHA","DH-DSS-AES128-SHA");
        addAlias("TLS_DH_DSS_WITH_AES_256_CBC_SHA","DH-DSS-AES256-SHA");
        addAlias("TLS_DH_RSA_WITH_AES_128_CBC_SHA","DH-RSA-AES128-SHA");
        addAlias("TLS_DH_RSA_WITH_AES_256_CBC_SHA","DH-RSA-AES256-SHA");
        addAlias("TLS_DHE_DSS_WITH_AES_128_CBC_SHA","DHE-DSS-AES128-SHA");
        addAlias("TLS_DHE_DSS_WITH_AES_256_CBC_SHA","DHE-DSS-AES256-SHA");
        addAlias("TLS_DHE_RSA_WITH_AES_128_CBC_SHA","DHE-RSA-AES128-SHA");
        addAlias("TLS_DHE_RSA_WITH_AES_256_CBC_SHA","DHE-RSA-AES256-SHA");
        addAlias("TLS_DH_anon_WITH_AES_128_CBC_SHA","ADH-AES128-SHA");
        addAlias("TLS_DH_anon_WITH_AES_256_CBC_SHA","ADH-AES256-SHA");
        addAlias("TLS_RSA_EXPORT1024_WITH_DES_CBC_SHA","EXP1024-DES-CBC-SHA");
        addAlias("TLS_RSA_EXPORT1024_WITH_RC4_56_SHA","EXP1024-RC4-SHA");
        addAlias("TLS_DHE_DSS_EXPORT1024_WITH_DES_CBC_SHA","EXP1024-DHE-DSS-DES-CBC-SHA");
        addAlias("TLS_DHE_DSS_EXPORT1024_WITH_RC4_56_SHA","EXP1024-DHE-DSS-RC4-SHA");
        addAlias("TLS_DHE_DSS_WITH_RC4_128_SHA","DHE-DSS-RC4-SHA");
        addAlias("SSL_CK_RC4_128_WITH_MD5","RC4-MD5");
        addAlias("SSL_CK_RC4_128_EXPORT40_WITH_MD5","EXP-RC4-MD5");
        addAlias("SSL_CK_RC2_128_CBC_WITH_MD5","RC2-MD5");
        addAlias("SSL_CK_RC2_128_CBC_EXPORT40_WITH_MD5","EXP-RC2-MD5");
        addAlias("SSL_CK_IDEA_128_CBC_WITH_MD5","IDEA-CBC-MD5");
        addAlias("SSL_CK_DES_64_CBC_WITH_MD5","DES-CBC-MD5");
        addAlias("SSL_CK_DES_192_EDE3_CBC_WITH_MD5","DES-CBC3-MD5");
	}
}// CipherStrings
