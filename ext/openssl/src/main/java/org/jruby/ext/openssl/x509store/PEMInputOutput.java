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
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
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
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.math.BigInteger;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.cert.X509CRL;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.pkcs.PKCS12PBEParams;
import org.bouncycastle.asn1.pkcs.EncryptedPrivateKeyInfo;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.pkcs.EncryptionScheme;
import org.bouncycastle.asn1.pkcs.PBES2Parameters;
import org.bouncycastle.asn1.pkcs.PBKDF2Params;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RC2CBCParameter;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DSAParameter;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.engines.DESedeEngine;
import org.bouncycastle.crypto.engines.RC2Engine;
import org.bouncycastle.crypto.generators.OpenSSLPBEParametersGenerator;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.cms.CMSSignedData;

import org.jruby.ext.openssl.Cipher.Algorithm;
import org.jruby.ext.openssl.Cipher.CipherModule;
import org.jruby.ext.openssl.impl.ASN1Registry;
import org.jruby.ext.openssl.impl.CipherSpec;
import org.jruby.ext.openssl.impl.PKCS10Request;

import org.jruby.ext.openssl.SecurityHelper;

/**
 * Helper class to read and write PEM files correctly.
 *
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class PEMInputOutput {
    public static final String BEF = "-----";
    public static final String AFT = "-----";
    public static final String BEF_G = BEF+"BEGIN ";
    public static final String BEF_E = BEF+"END ";
    public static final String PEM_STRING_X509_OLD="X509 CERTIFICATE";
    public static final String PEM_STRING_X509="CERTIFICATE";
    public static final String PEM_STRING_X509_PAIR="CERTIFICATE PAIR";
    public static final String PEM_STRING_X509_TRUSTED="TRUSTED CERTIFICATE";
    public static final String PEM_STRING_X509_REQ_OLD="NEW CERTIFICATE REQUEST";
    public static final String PEM_STRING_X509_REQ="CERTIFICATE REQUEST";
    public static final String PEM_STRING_X509_CRL="X509 CRL";
    public static final String PEM_STRING_EVP_PKEY="ANY PRIVATE KEY";
    public static final String PEM_STRING_PUBLIC="PUBLIC KEY";
    public static final String PEM_STRING_RSA="RSA PRIVATE KEY";
    public static final String PEM_STRING_RSA_PUBLIC="RSA PUBLIC KEY";
    public static final String PEM_STRING_DSA="DSA PRIVATE KEY";
    public static final String PEM_STRING_DSA_PUBLIC="DSA PUBLIC KEY";
    public static final String PEM_STRING_PKCS7="PKCS7";
    public static final String PEM_STRING_PKCS8="ENCRYPTED PRIVATE KEY";
    public static final String PEM_STRING_PKCS8INF="PRIVATE KEY";
    public static final String PEM_STRING_DHPARAMS="DH PARAMETERS";
    public static final String PEM_STRING_SSL_SESSION="SSL SESSION PARAMETERS";
    public static final String PEM_STRING_DSAPARAMS="DSA PARAMETERS";
    public static final String PEM_STRING_ECDSA_PUBLIC="ECDSA PUBLIC KEY";
    public static final String PEM_STRING_ECPARAMETERS="EC PARAMETERS";
    public static final String PEM_STRING_ECPRIVATEKEY="EC PRIVATE KEY";

    private static final Pattern DH_PARAM_PATTERN = Pattern.compile(
            "(-----BEGIN DH PARAMETERS-----)(.*)(-----END DH PARAMETERS-----)",
            Pattern.MULTILINE);
    private static final int DH_PARAM_GROUP = 2; // the group above containing encoded params

    private static BufferedReader makeBuffered(Reader in) {
        if(in instanceof BufferedReader) {
            return (BufferedReader)in;
        }
        return new BufferedReader(in);
    }

    private static BufferedWriter makeBuffered(Writer out) {
        if(out instanceof BufferedWriter) {
            return (BufferedWriter)out;
        }
        return new BufferedWriter(out);
    }

    /**
     * c: PEM_X509_INFO_read_bio
     */
    public static Object readPEM(Reader in,char[] f) throws IOException {
        BufferedReader _in = makeBuffered(in);
        String  line;
        while ((line = _in.readLine()) != null) {
            if(line.indexOf(BEF_G+PEM_STRING_PUBLIC) != -1) {
                try {
                    return readPublicKey(_in,BEF_E+PEM_STRING_PUBLIC);
                } catch (Exception e) {
                    throw new IOException("problem creating public key: " + e.toString());
                }
            } else if(line.indexOf(BEF_G+PEM_STRING_DSA) != -1) {
                try {
                    return readKeyPair(_in,f, "DSA", BEF_E+PEM_STRING_DSA);
                } catch (Exception e) {
                    throw new IOException("problem creating DSA private key: " + e.toString());
                }
            } else if(line.indexOf(BEF_G+PEM_STRING_RSA_PUBLIC) != -1) {
                try {
                    return readPublicKey(_in,BEF_E+PEM_STRING_RSA_PUBLIC);
                } catch (Exception e) {
                    throw new IOException("problem creating RSA public key: " + e.toString());
                }
            } else if(line.indexOf(BEF_G+PEM_STRING_X509_OLD) != -1) {
                try {
                    return readAuxCertificate(_in,BEF_E+PEM_STRING_X509_OLD);
                } catch (Exception e) {
                    throw new IOException("problem creating X509 Aux certificate: " + e.toString());
                }
            } else if(line.indexOf(BEF_G+PEM_STRING_X509) != -1) {
                try {
                    return readAuxCertificate(_in,BEF_E+PEM_STRING_X509);
                } catch (Exception e) {
                    throw new IOException("problem creating X509 Aux certificate: " + e.toString());
                }
            } else if(line.indexOf(BEF_G+PEM_STRING_X509_TRUSTED) != -1) {
                try {
                    return readAuxCertificate(_in,BEF_E+PEM_STRING_X509_TRUSTED);
                } catch (Exception e) {
                    throw new IOException("problem creating X509 Aux certificate: " + e.toString());
                }
            } else if(line.indexOf(BEF_G+PEM_STRING_X509_CRL) != -1) {
                try {
                    return readCRL(_in,BEF_E+PEM_STRING_X509_CRL);
                } catch (Exception e) {
                    throw new IOException("problem creating X509 CRL: " + e.toString());
                }
            } else if(line.indexOf(BEF_G+PEM_STRING_X509_REQ) != -1) {
                try {
                    return readCertificateRequest(_in,BEF_E+PEM_STRING_X509_REQ);
                } catch (Exception e) {
                    throw new IOException("problem creating X509 REQ: " + e.toString());
                }
            }
        }
        return null;
    }

    public static byte[] readX509PEM(Reader in) throws IOException {
        BufferedReader _in = makeBuffered(in);
        String line;
        while ((line = _in.readLine()) != null) {
            if (line.indexOf(BEF_G + PEM_STRING_X509_OLD) != -1) {
                try {
                    return readBytes(_in, BEF_E + PEM_STRING_X509_OLD);
                } catch (Exception e) {
                    throw new IOException("problem reading PEM X509 Aux certificate: " + e.toString());
                }
            } else if (line.indexOf(BEF_G + PEM_STRING_X509) != -1) {
                try {
                    return readBytes(_in, BEF_E + PEM_STRING_X509);
                } catch (Exception e) {
                    throw new IOException("problem reading PEM X509 Aux certificate: " + e.toString());
                }
            } else if (line.indexOf(BEF_G + PEM_STRING_X509_TRUSTED) != -1) {
                try {
                    return readBytes(_in, BEF_E + PEM_STRING_X509_TRUSTED);
                } catch (Exception e) {
                    throw new IOException("problem reading PEM X509 Aux certificate: " + e.toString());
                }
            } else if (line.indexOf(BEF_G + PEM_STRING_X509_CRL) != -1) {
                try {
                    return readBytes(_in, BEF_E + PEM_STRING_X509_CRL);
                } catch (Exception e) {
                    throw new IOException("problem reading PEM X509 CRL: " + e.toString());
                }
            } else if (line.indexOf(BEF_G + PEM_STRING_X509_REQ) != -1) {
                try {
                    return readBytes(_in, BEF_E + PEM_STRING_X509_REQ);
                } catch (Exception e) {
                    throw new IOException("problem reading PEM X509 REQ: " + e.toString());
                }
            }
        }
        return null;
    }

    /**
     * c: PEM_read_PrivateKey + PEM_read_bio_PrivateKey
     * CAUTION: KeyPair#getPublic() may be null.
     */
    public static KeyPair readPrivateKey(Reader in, char[] password) throws IOException {
        BufferedReader _in = makeBuffered(in);
        String line;
        while ((line = _in.readLine()) != null) {
            if (line.indexOf(BEF_G + PEM_STRING_RSA) != -1) {
                try {
                    return readKeyPair(_in, password, "RSA", BEF_E + PEM_STRING_RSA);
                } catch (Exception e) {
                    throw new IOException("problem creating RSA private key: " + e.toString());
                }
            } else if (line.indexOf(BEF_G + PEM_STRING_DSA) != -1) {
                try {
                    return readKeyPair(_in, password, "DSA", BEF_E + PEM_STRING_DSA);
                } catch (Exception e) {
                    throw new IOException("problem creating DSA private key: " + e.toString());
                }
            } else if (line.indexOf(BEF_G + PEM_STRING_ECPRIVATEKEY) != -1) {
                throw new IOException("EC private key not supported");
            } else if (line.indexOf(BEF_G + PEM_STRING_PKCS8INF) != -1) {
                try {
                    byte[] bytes = readBytes(_in, BEF_E + PEM_STRING_PKCS8INF);
                    PrivateKeyInfo info = PrivateKeyInfo.getInstance(bytes);
                    String type = getPrivateKeyTypeFromObjectId(info.getPrivateKeyAlgorithm().getAlgorithm());
                    return org.jruby.ext.openssl.impl.PKey.readPrivateKey(((ASN1Object) info.parsePrivateKey()).getEncoded(ASN1Encoding.DER), type);
                } catch (Exception e) {
                    throw new IOException("problem creating private key: " + e.toString());
                }
            } else if (line.indexOf(BEF_G + PEM_STRING_PKCS8) != -1) {
                try {
                    byte[] bytes = readBytes(_in, BEF_E + PEM_STRING_PKCS8);
                    EncryptedPrivateKeyInfo eIn = EncryptedPrivateKeyInfo.getInstance(bytes);
                    AlgorithmIdentifier algId = eIn.getEncryptionAlgorithm();
                    PrivateKey privKey;
                    if (algId.getAlgorithm().toString().equals("1.2.840.113549.1.5.13")) { // PBES2
                        privKey = derivePrivateKeyPBES2(eIn, algId, password);
                    } else {
                        privKey = derivePrivateKeyPBES1(eIn, algId, password);
                    }
                    return new KeyPair(null, privKey);
                } catch (Exception e) {
                    throw new IOException("problem creating private key: " + e.toString());
                }
            }
        }
        return null;
    }

    private static PrivateKey derivePrivateKeyPBES1(EncryptedPrivateKeyInfo eIn, AlgorithmIdentifier algId, char[] password)
            throws GeneralSecurityException, IOException {
        // From BC's PEMReader
        PKCS12PBEParams pkcs12Params = PKCS12PBEParams.getInstance(algId.getParameters());
        PBEKeySpec pbeSpec = new PBEKeySpec(password);
        PBEParameterSpec pbeParams = new PBEParameterSpec(
            pkcs12Params.getIV(), pkcs12Params.getIterations().intValue()
        );

        //String algorithm = algId.getAlgorithm().getId();
        String algorithm = ASN1Registry.o2a(algId.getAlgorithm());
        algorithm = (algorithm.split("-"))[0];

        SecretKeyFactory secKeyFact = SecurityHelper.getSecretKeyFactory(algorithm);

        Cipher cipher = SecurityHelper.getCipher(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, secKeyFact.generateSecret(pbeSpec), pbeParams);

        PrivateKeyInfo pInfo = PrivateKeyInfo.getInstance(
            ASN1Primitive.fromByteArray(cipher.doFinal(eIn.getEncryptedData()))
        );
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pInfo.getEncoded());

        String keyFactAlg = ASN1Registry.o2a(pInfo.getPrivateKeyAlgorithm().getAlgorithm());

        // TODO: Can we just set it to RSA as in derivePrivateKeyPBES2?
        KeyFactory keyFact;
        if (keyFactAlg.startsWith("dsa")) {
            keyFact = SecurityHelper.getKeyFactory("DSA");
        } else {
            keyFact = SecurityHelper.getKeyFactory("RSA"); // BC
        }

        return keyFact.generatePrivate(keySpec);
    }

    private static PrivateKey derivePrivateKeyPBES2(EncryptedPrivateKeyInfo eIn, AlgorithmIdentifier algId, char[] password)
            throws GeneralSecurityException, InvalidCipherTextException {
        PBES2Parameters pbeParams = PBES2Parameters.getInstance((ASN1Sequence) algId.getParameters());
        CipherParameters cipherParams = extractPBES2CipherParams(password, pbeParams);

        EncryptionScheme scheme = pbeParams.getEncryptionScheme();
        BufferedBlockCipher cipher;
        if (scheme.getAlgorithm().equals(PKCSObjectIdentifiers.RC2_CBC)) {
            RC2CBCParameter rc2Params = RC2CBCParameter.getInstance(scheme);
            byte[] iv = rc2Params.getIV();
            CipherParameters param = new ParametersWithIV(cipherParams, iv);
            cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new RC2Engine()));
            cipher.init(false, param);
        } else {
            byte[] iv = ((ASN1OctetString) scheme.getObject()).getOctets();
            // this version, done for BC 1.49 compat, caused #1238.
//            byte[] iv = ASN1OctetString.getInstance(scheme).getOctets();
            CipherParameters param = new ParametersWithIV(cipherParams, iv);
            cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new DESedeEngine()));
            cipher.init(false, param);
        }

        byte[] data = eIn.getEncryptedData();
        byte[] out = new byte[cipher.getOutputSize(data.length)];
        int len = cipher.processBytes(data, 0, data.length, out, 0);
        len += cipher.doFinal(out, len);
        byte[] pkcs8 = new byte[len];
        System.arraycopy(out, 0, pkcs8, 0, len);
        KeyFactory fact = KeyFactory.getInstance("RSA"); // It seems to work for both RSA and DSA.
        return fact.generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
    }

    private static CipherParameters extractPBES2CipherParams(char[] password, PBES2Parameters pbeParams) {
        PBKDF2Params pbkdfParams = PBKDF2Params.getInstance(pbeParams.getKeyDerivationFunc().getParameters());
        int keySize = 192;
        if (pbkdfParams.getKeyLength() != null) {
            keySize = pbkdfParams.getKeyLength().intValue() * 8;
        }
        int iterationCount = pbkdfParams.getIterationCount().intValue();
        byte[] salt = pbkdfParams.getSalt();
        PBEParametersGenerator generator = new PKCS5S2ParametersGenerator();
        generator.init(PBEParametersGenerator.PKCS5PasswordToBytes(password), salt, iterationCount);
        return generator.generateDerivedParameters(keySize);
    }

    // PEM_read_bio_PUBKEY
    public static PublicKey readPubKey(Reader in) throws IOException {
        PublicKey pubKey = readRSAPubKey(in);
        if (pubKey == null) {
            pubKey = readDSAPubKey(in);
        }
        return pubKey;
    }

    /*
     * c: PEM_read_bio_DSA_PUBKEY
     */
    public static DSAPublicKey readDSAPubKey(Reader in) throws IOException {
        BufferedReader _in = makeBuffered(in);
        String  line;
        while ((line = _in.readLine()) != null) {
            if(line.indexOf(BEF_G+PEM_STRING_DSA_PUBLIC) != -1) {
                try {
                    return (DSAPublicKey)readPublicKey(_in,"DSA",BEF_E+PEM_STRING_DSA_PUBLIC);
                } catch (Exception e) {
                    throw new IOException("problem creating DSA public key: " + e.toString());
                }
            }
        }
        return null;
    }

    /*
     * c: PEM_read_bio_DSAPublicKey
     */
    public static DSAPublicKey readDSAPublicKey(Reader in, char[] f) throws IOException {
        BufferedReader _in = makeBuffered(in);
        String  line;
        while ((line = _in.readLine()) != null) {
            if(line.indexOf(BEF_G+PEM_STRING_PUBLIC) != -1) {
                try {
                    return (DSAPublicKey)readPublicKey(_in,"DSA",BEF_E+PEM_STRING_PUBLIC);
                } catch (Exception e) {
                    throw new IOException("problem creating DSA public key: " + e.toString());
                }
            }
        }
        return null;
    }

    /*
     * c: PEM_read_bio_DSAPrivateKey
     */
    public static KeyPair readDSAPrivateKey(Reader in, char[] f) throws IOException {
        BufferedReader _in = makeBuffered(in);
        String  line;
        while ((line = _in.readLine()) != null) {
            if(line.indexOf(BEF_G+PEM_STRING_DSA) != -1) {
                try {
                    return readKeyPair(_in,f, "DSA", BEF_E+PEM_STRING_DSA);
                } catch (Exception e) {
                    throw new IOException("problem creating DSA private key: " + e.toString());
                }
            }
        }
        return null;
    }

    /**
     * reads an RSA public key encoded in an SubjectPublicKeyInfo RSA structure.
     * c: PEM_read_bio_RSA_PUBKEY
     */
    public static RSAPublicKey readRSAPubKey(Reader in) throws IOException {
        BufferedReader _in = makeBuffered(in);
        String  line;
        while ((line = _in.readLine()) != null) {
            if(line.indexOf(BEF_G+PEM_STRING_PUBLIC) != -1) {
                try {
                    return readRSAPublicKey(_in,BEF_E+PEM_STRING_PUBLIC);
                } catch (Exception e) {
                    throw new IOException("problem creating RSA public key: " + e.toString());
                }
            } else if(line.indexOf(BEF_G+PEM_STRING_RSA_PUBLIC) != -1) {
                try {
                    return readRSAPublicKey(_in,BEF_E+PEM_STRING_RSA_PUBLIC);
                } catch (Exception e) {
                    throw new IOException("problem creating RSA public key: " + e.toString());
                }
            }
        }
        return null;
    }

    /**
     * reads an RSA public key encoded in an PKCS#1 RSA structure.
     * c: PEM_read_bio_RSAPublicKey
     */
    public static RSAPublicKey readRSAPublicKey(Reader in, char[] f) throws IOException {
        BufferedReader _in = makeBuffered(in);
        String  line;
        while ((line = _in.readLine()) != null) {
            if(line.indexOf(BEF_G+PEM_STRING_PUBLIC) != -1) {
                try {
                    return (RSAPublicKey)readPublicKey(_in,"RSA",BEF_E+PEM_STRING_PUBLIC);
                } catch (Exception e) {
                    throw new IOException("problem creating RSA public key: " + e.toString());
                }
            } else if(line.indexOf(BEF_G+PEM_STRING_RSA_PUBLIC) != -1) {
                try {
                    return (RSAPublicKey)readPublicKey(_in,"RSA",BEF_E+PEM_STRING_RSA_PUBLIC);
                } catch (Exception e) {
                    throw new IOException("problem creating RSA public key: " + e.toString());
                }
            }
        }
        return null;
    }

    /**
     * c: PEM_read_bio_RSAPrivateKey
     */
    public static KeyPair readRSAPrivateKey(Reader in, char[] f) throws IOException {
        BufferedReader _in = makeBuffered(in);
        String  line;
        while ((line = _in.readLine()) != null) {
            if(line.indexOf(BEF_G+PEM_STRING_RSA) != -1) {
                try {
                    return readKeyPair(_in,f, "RSA", BEF_E+PEM_STRING_RSA);
                } catch (Exception e) {
                    throw new IOException("problem creating RSA private key: " + e.toString());
                }
            }
        }
        return null;
    }
    public static CMSSignedData readPKCS7(Reader in, char[] f) throws IOException {
        BufferedReader _in = makeBuffered(in);
        String  line;
        while ((line = _in.readLine()) != null) {
            if(line.indexOf(BEF_G+PEM_STRING_PKCS7) != -1) {
                try {
                    return readPKCS7(_in,f, BEF_E+PEM_STRING_PKCS7);
                } catch (Exception e) {
                    throw new IOException("problem creating PKCS7: " + e.toString());
                }
            }
        }
        return null;
    }
    public static X509AuxCertificate readX509Certificate(Reader in, char[] f) throws IOException {
        BufferedReader _in = makeBuffered(in);
        String  line;
        while ((line = _in.readLine()) != null) {
            if(line.indexOf(BEF_G+PEM_STRING_X509_OLD) != -1) {
                try {
                    return new X509AuxCertificate(readCertificate(_in,BEF_E+PEM_STRING_X509_OLD));
                } catch (Exception e) {
                    throw new IOException("problem creating X509 certificate: " + e.toString());
                }
            } else if(line.indexOf(BEF_G+PEM_STRING_X509) != -1) {
                try {
                    return new X509AuxCertificate(readCertificate(_in,BEF_E+PEM_STRING_X509));
                } catch (Exception e) {
                    throw new IOException("problem creating X509 certificate: " + e.toString());
                }
            } else if(line.indexOf(BEF_G+PEM_STRING_X509_TRUSTED) != -1) {
                try {
                    return new X509AuxCertificate(readCertificate(_in,BEF_E+PEM_STRING_X509_TRUSTED));
                } catch (Exception e) {
                    throw new IOException("problem creating X509 certificate: " + e.toString());
                }
            }
        }
        return null;
    }
    public static X509AuxCertificate readX509Aux(Reader in, char[] f) throws IOException {
        BufferedReader _in = makeBuffered(in);
        String  line;
        while ((line = _in.readLine()) != null) {
            if(line.indexOf(BEF_G+PEM_STRING_X509_OLD) != -1) {
                try {
                    return readAuxCertificate(_in,BEF_E+PEM_STRING_X509_OLD);
                } catch (Exception e) {
                    throw new IOException("problem creating X509 Aux certificate: " + e.toString());
                }
            } else if(line.indexOf(BEF_G+PEM_STRING_X509) != -1) {
                try {
                    return readAuxCertificate(_in,BEF_E+PEM_STRING_X509);
                } catch (Exception e) {
                    throw new IOException("problem creating X509 Aux certificate: " + e.toString());
                }
            } else if(line.indexOf(BEF_G+PEM_STRING_X509_TRUSTED) != -1) {
                try {
                    return readAuxCertificate(_in,BEF_E+PEM_STRING_X509_TRUSTED);
                } catch (Exception e) {
                    throw new IOException("problem creating X509 Aux certificate: " + e.toString());
                }
            }
        }
        return null;
    }
    public static X509CRL readX509CRL(Reader in, char[] f) throws IOException {
        BufferedReader _in = makeBuffered(in);
        String  line;
        while ((line = _in.readLine()) != null) {
            if(line.indexOf(BEF_G+PEM_STRING_X509_CRL) != -1) {
                try {
                    return readCRL(_in,BEF_E+PEM_STRING_X509_CRL);
                } catch (Exception e) {
                    throw new IOException("problem creating X509 CRL: " + e.toString());
                }
            }
        }
        return null;
    }
    public static PKCS10Request readX509Request(Reader in, char[] f) throws IOException {
        BufferedReader _in = makeBuffered(in);
        String  line;
        while ((line = _in.readLine()) != null) {
            if(line.indexOf(BEF_G+PEM_STRING_X509_REQ) != -1) {
                try {
                    return readCertificateRequest(_in,BEF_E+PEM_STRING_X509_REQ);
                } catch (Exception e) {
                    throw new IOException("problem creating X509 REQ: " + e.toString());
                }
            }
        }
        return null;
    }

    public static DHParameterSpec readDHParameters(Reader _in) throws IOException {
        BufferedReader in = makeBuffered(_in);
        String line;
        StringBuilder buf = new StringBuilder();
        while ((line = in.readLine()) != null) {
            if (line.indexOf(BEF_G + PEM_STRING_DHPARAMS) >= 0) {
                do {
                    buf.append(line.trim());
                } while (line.indexOf(BEF_E + PEM_STRING_DHPARAMS) < 0 && (line = in.readLine()) != null);
                break;
            }
        }
        Matcher m = DH_PARAM_PATTERN.matcher(buf.toString());
        if (m.find()) {
            try {
                byte[] decoded = Base64.decode(m.group(DH_PARAM_GROUP));
                return org.jruby.ext.openssl.impl.PKey.readDHParameter(decoded);
            } catch (Exception e) {
            }
        }
        return null;
    }

    private static byte[] getEncoded(java.security.Key key) {
        if (key != null) {
            return key.getEncoded();
        }
        return new byte[] { '0', 0 };
    }

    private static byte[] getEncoded(ASN1Encodable obj) throws IOException {
        if (obj != null) {
            return obj.toASN1Primitive().getEncoded();
        }
        return new byte[] { '0', 0 };
    }

    private static byte[] getEncoded(CMSSignedData obj) throws IOException {
        if (obj != null) {
            return obj.getEncoded();
        }
        return new byte[] { '0', 0 };
    }

    private static byte[] getEncoded(X509Certificate cert) throws IOException {
        if (cert != null) {
            try {
                return cert.getEncoded();
            } catch (GeneralSecurityException gse) {
                throw new IOException("problem with encoding object in write_X509");
            }
        }
        return new byte[] { '0', 0 };
    }

    private static byte[] getEncoded(X509CRL crl) throws IOException {
        if (crl != null) {
            try {
                return crl.getEncoded();
            } catch (GeneralSecurityException gse) {
                throw new IOException("problem with encoding object in write_X509_CRL");
            }
        }
        return new byte[] { '0', 0 };
    }

    public static void writeDSAPublicKey(Writer _out, DSAPublicKey obj) throws IOException {
        BufferedWriter out = makeBuffered(_out);
        byte[] encoding = getEncoded(obj);
        out.write(BEF_G + PEM_STRING_DSA_PUBLIC + AFT);
        out.newLine();
        writeEncoded(out, encoding);
        out.write(BEF_E + PEM_STRING_DSA_PUBLIC + AFT);
        out.newLine();
        out.flush();
    }
    /** writes an RSA public key encoded in an PKCS#1 RSA structure. */
    public static void writeRSAPublicKey(Writer _out, RSAPublicKey obj) throws IOException {
        BufferedWriter out = makeBuffered(_out);
        byte[] encoding = getEncoded(obj);
        out.write(BEF_G + PEM_STRING_RSA_PUBLIC + AFT);
        out.newLine();
        writeEncoded(out, encoding);
        out.write(BEF_E + PEM_STRING_RSA_PUBLIC + AFT);
        out.newLine();
        out.flush();
    }
    public static void writePKCS7(Writer _out, ContentInfo obj) throws IOException {
        BufferedWriter out = makeBuffered(_out);
        byte[] encoding = getEncoded(obj);
        out.write(BEF_G + PEM_STRING_PKCS7 + AFT);
        out.newLine();
        writeEncoded(out,encoding);
        out.write(BEF_E + PEM_STRING_PKCS7 + AFT);
        out.newLine();
        out.flush();
    }
    public static void writePKCS7(Writer _out, CMSSignedData obj) throws IOException {
        BufferedWriter out = makeBuffered(_out);
        byte[] encoding = getEncoded(obj);
        out.write(BEF_G + PEM_STRING_PKCS7 + AFT);
        out.newLine();
        writeEncoded(out,encoding);
        out.write(BEF_E + PEM_STRING_PKCS7 + AFT);
        out.newLine();
        out.flush();
    }
    public static void writePKCS7(Writer _out, byte[] encoded) throws IOException {
        BufferedWriter out = makeBuffered(_out);
        out.write(BEF_G + PEM_STRING_PKCS7 + AFT);
        out.newLine();
        writeEncoded(out,encoded);
        out.write(BEF_E + PEM_STRING_PKCS7 + AFT);
        out.newLine();
        out.flush();
    }
    public static void writeX509Certificate(Writer _out, X509Certificate obj) throws IOException {
        BufferedWriter out = makeBuffered(_out);
        byte[] encoding = getEncoded(obj);
        out.write(BEF_G + PEM_STRING_X509 + AFT);
        out.newLine();
        writeEncoded(out, encoding);
        out.write(BEF_E + PEM_STRING_X509 + AFT);
        out.newLine();
        out.flush();
    }
    public static void writeX509Aux(Writer _out, X509AuxCertificate obj) throws IOException {
        BufferedWriter out = makeBuffered(_out);
        byte[] encoding = null;
        try {
            if(obj.getAux() == null) {
                encoding = obj.getEncoded();
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] ymp = obj.getEncoded();
                baos.write(ymp,0,ymp.length);

                X509Aux aux = obj.getAux();
                ASN1EncodableVector a1 = new ASN1EncodableVector();
                if(aux.trust.size()>0) {
                    ASN1EncodableVector a2 = new ASN1EncodableVector();
                    for(String trust : aux.trust) {
                        a2.add(new ASN1ObjectIdentifier(trust));
                    }
                    a1.add(new DLSequence(a2));
                }
                if(aux.reject.size()>0) {
                    ASN1EncodableVector a2 = new ASN1EncodableVector();
                    for(String reject : aux.reject) {
                        a2.add(new ASN1ObjectIdentifier(reject));
                    }
                    a1.add(new DERTaggedObject(0,new DLSequence(a2)));
                }
                if(aux.alias != null) {
                    a1.add(new DERUTF8String(aux.alias));
                }
                if(aux.keyid != null) {
                    a1.add(new DEROctetString(aux.keyid));
                }
                if(aux.other.size()>0) {
                    ASN1EncodableVector a2 = new ASN1EncodableVector();
                    for(ASN1Primitive other : aux.other) {
                        a2.add(other);
                    }
                    a1.add(new DERTaggedObject(1,new DLSequence(a2)));
                }
                ymp = new DLSequence(a1).getEncoded();
                baos.write(ymp,0,ymp.length);
                encoding = baos.toByteArray();
            }
        } catch(CertificateEncodingException e) {
            throw new IOException("problem with encoding object in write_X509_AUX");
        }
        out.write(BEF_G + PEM_STRING_X509_TRUSTED + AFT);
        out.newLine();
        writeEncoded(out,encoding);
        out.write(BEF_E + PEM_STRING_X509_TRUSTED + AFT);
        out.newLine();
        out.flush();
    }
    public static void writeX509CRL(Writer _out, X509CRL obj) throws IOException {
        BufferedWriter out = makeBuffered(_out);
        byte[] encoding = getEncoded(obj);
        out.write(BEF_G + PEM_STRING_X509_CRL + AFT);
        out.newLine();
        writeEncoded(out, encoding);
        out.write(BEF_E + PEM_STRING_X509_CRL + AFT);
        out.newLine();
        out.flush();
    }
    public static void writeX509Request(Writer _out, PKCS10Request obj) throws IOException {
        BufferedWriter out = makeBuffered(_out);
        byte[] encoding = getEncoded(obj.toASN1Structure());
        out.write(BEF_G + PEM_STRING_X509_REQ + AFT);
        out.newLine();
        writeEncoded(out,encoding);
        out.write(BEF_E + PEM_STRING_X509_REQ + AFT);
        out.newLine();
        out.flush();
    }

    private static SecureRandom random;
    static {
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch(Exception e) {
            random = null;
        }
    }

    public static void writeDSAPrivateKey(Writer _out, DSAPrivateKey obj, CipherSpec cipher, char[] passwd) throws IOException {
        BufferedWriter out = makeBuffered(_out);
        PrivateKeyInfo info = new PrivateKeyInfo((ASN1Sequence) new ASN1InputStream(getEncoded(obj)).readObject());
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        ASN1OutputStream aOut = new ASN1OutputStream(bOut);

        DSAParameter p = DSAParameter.getInstance(info.getPrivateKeyAlgorithm().getParameters());
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new ASN1Integer(0));
        v.add(new ASN1Integer(p.getP()));
        v.add(new ASN1Integer(p.getQ()));
        v.add(new ASN1Integer(p.getG()));

        BigInteger x = obj.getX();
        BigInteger y = p.getG().modPow(x, p.getP());

        v.add(new ASN1Integer(y));
        v.add(new ASN1Integer(x));

        aOut.writeObject(new DLSequence(v));
        byte[] encoding = bOut.toByteArray();

        if (cipher != null && passwd != null) {
            writePemEncrypted(out, PEM_STRING_DSA, encoding, cipher, passwd);
        } else {
            writePemPlain(out, PEM_STRING_DSA, encoding);
        }
    }

    public static void writeRSAPrivateKey(Writer _out, RSAPrivateCrtKey obj, CipherSpec cipher, char[] passwd) throws IOException {
        assert (obj != null);
        BufferedWriter out = makeBuffered(_out);
        org.bouncycastle.asn1.pkcs.RSAPrivateKey keyStruct = new org.bouncycastle.asn1.pkcs.RSAPrivateKey(obj.getModulus(), obj.getPublicExponent(), obj.getPrivateExponent(), obj.getPrimeP(),
                obj.getPrimeQ(), obj.getPrimeExponentP(), obj.getPrimeExponentQ(), obj.getCrtCoefficient());

        if (cipher != null && passwd != null) {
            writePemEncrypted(out, PEM_STRING_RSA, keyStruct.getEncoded(), cipher, passwd);
        } else {
            writePemPlain(out, PEM_STRING_RSA, keyStruct.getEncoded());
        }
    }

    private static void writePemPlain(BufferedWriter out, String pemHeader, byte[] encoding) throws IOException {
        out.write(BEF_G + pemHeader + AFT);
        out.newLine();
        writeEncoded(out, encoding);
        out.write(BEF_E + pemHeader + AFT);
        out.newLine();
        out.flush();
    }

    private static void writePemEncrypted(BufferedWriter out, String pemHeader, byte[] encoding, CipherSpec cipherSpec, char[] passwd) throws IOException {
        Cipher cipher = cipherSpec.getCipher();
        byte[] iv = new byte[cipher.getBlockSize()];
        random.nextBytes(iv);
        byte[] salt = new byte[8];
        System.arraycopy(iv, 0, salt, 0, 8);
        OpenSSLPBEParametersGenerator pGen = new OpenSSLPBEParametersGenerator();
        pGen.init(PBEParametersGenerator.PKCS5PasswordToBytes(passwd), salt);
        KeyParameter param = (KeyParameter) pGen.generateDerivedParameters(cipherSpec.getKeyLenInBits());
        SecretKey secretKey = new SecretKeySpec(param.getKey(), Algorithm.getAlgorithmBase(cipher));
        byte[] encData = null;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            encData = cipher.doFinal(encoding);
        } catch (InvalidKeyException ike) {
            if (ike.getMessage().startsWith("Invalid key length")) {
                throw new IOException("Invalid key length. See http://wiki.jruby.org/UnlimitedStrengthCrypto");
            }
            throw new IOException("exception using cipher:" + ike.toString(), ike);
        } catch (GeneralSecurityException gse) {
            throw new IOException("exception using cipher: " + gse.toString(), gse);
        }
        out.write(BEF_G + pemHeader + AFT);
        out.newLine();
        out.write("Proc-Type: 4,ENCRYPTED");
        out.newLine();
        out.write("DEK-Info: " + cipherSpec.getOsslName() + ",");
        writeHexEncoded(out, iv);
        out.newLine();
        out.newLine();
        writeEncoded(out, encData);
        out.write(BEF_E + pemHeader + AFT);
        out.flush();
    }

    public static void writeDHParameters(Writer _out, DHParameterSpec params) throws IOException {
        BufferedWriter out = makeBuffered(_out);
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        ASN1OutputStream aOut = new ASN1OutputStream(bOut);

        ASN1EncodableVector v = new ASN1EncodableVector();

        BigInteger value;
        if ((value = params.getP()) != null) {
            v.add(new ASN1Integer(value));
        }
        if ((value = params.getG()) != null) {
            v.add(new ASN1Integer(value));
        }

        aOut.writeObject(new DLSequence(v));
        byte[] encoding = bOut.toByteArray();

        out.write(BEF_G + PEM_STRING_DHPARAMS + AFT);
        out.newLine();
        writeEncoded(out,encoding);
        out.write(BEF_E + PEM_STRING_DHPARAMS + AFT);
        out.newLine();
        out.flush();
    }

    private static String getPrivateKeyTypeFromObjectId(ASN1ObjectIdentifier oid) {
        if (ASN1Registry.obj2nid(oid) == ASN1Registry.NID_rsaEncryption) {
            return "RSA";
        } else {
            return "DSA";
        }
    }

    private static byte[] readBytes(BufferedReader in, String endMarker) throws IOException {
        String          line;
        StringBuffer    buf = new StringBuffer();

        while ((line = in.readLine()) != null) {
            if (line.indexOf(endMarker) != -1) {
                break;
            }
            buf.append(line.trim());
        }

        if (line == null) {
            throw new IOException(endMarker + " not found");
        }

        return Base64.decode(buf.toString());
    }

    private static RSAPublicKey readRSAPublicKey(BufferedReader in, String endMarker) throws IOException {
        Object asnObject = new ASN1InputStream(readBytes(in, endMarker)).readObject();
        ASN1Sequence sequence = (ASN1Sequence) asnObject;
        org.bouncycastle.asn1.pkcs.RSAPublicKey rsaPubStructure = org.bouncycastle.asn1.pkcs.RSAPublicKey.getInstance(sequence);
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(
                    rsaPubStructure.getModulus(),
                    rsaPubStructure.getPublicExponent());

        try {
            return (RSAPublicKey) SecurityHelper.getKeyFactory("RSA").generatePublic(keySpec);
        }
        catch (NoSuchAlgorithmException e) { /* ignore */ }
        catch (InvalidKeySpecException e) { /* ignore */ }
        return  null;
    }

    private static PublicKey readPublicKey(byte[] input, String alg, String endMarker) throws IOException {
        KeySpec keySpec = new X509EncodedKeySpec(input);
        try {
            return SecurityHelper.getKeyFactory(alg).generatePublic(keySpec);
        }
        catch (NoSuchAlgorithmException e) { /* ignore */ }
        catch (InvalidKeySpecException e) { /* ignore */ }
        return null;
    }

    private static PublicKey readPublicKey(BufferedReader in, String alg, String endMarker) throws IOException {
        return readPublicKey(readBytes(in, endMarker), alg, endMarker);
    }

    private static PublicKey readPublicKey(BufferedReader in, String endMarker) throws IOException {
        byte[] input = readBytes(in, endMarker);
        String[] algs = { "RSA", "DSA" };
        for (int i = 0; i < algs.length; i++) {
            PublicKey key = readPublicKey(input, algs[i], endMarker);
            if (key != null) {
                return key;
            }
        }
        return null;
    }

    /**
     * Read a Key Pair
     */
    private static KeyPair readKeyPair(BufferedReader _in, char[] passwd, String type, String endMarker) throws Exception {
        boolean isEncrypted = false;
        String line = null;
        String dekInfo = null;
        StringBuilder buf = new StringBuilder();

        while ((line = _in.readLine()) != null) {
            if (line.startsWith("Proc-Type: 4,ENCRYPTED")) {
                isEncrypted = true;
            } else if (line.startsWith("DEK-Info:")) {
                dekInfo = line.substring(10);
            } else if (line.indexOf(endMarker) != -1) {
                break;
            } else {
                buf.append(line.trim());
            }
        }
        byte[] keyBytes = null;
        byte[] decoded = Base64.decode(buf.toString());
        if (isEncrypted) {
            keyBytes = decrypt(decoded, dekInfo, passwd);
        } else {
            keyBytes = decoded;
        }
        return org.jruby.ext.openssl.impl.PKey.readPrivateKey(keyBytes, type);
    }

    private static byte[] decrypt(byte[] decoded, String dekInfo, char[] passwd) throws IOException, GeneralSecurityException {
        if (passwd == null) {
            throw new IOException("Password is null, but a password is required");
        }
        StringTokenizer tknz = new StringTokenizer(dekInfo, ",");
        String algorithm = tknz.nextToken();
        byte[] iv = Hex.decode(tknz.nextToken());
        if (!CipherModule.isSupportedCipher(algorithm)) {
            throw new IOException("Unknown algorithm: " + algorithm);
        }
        String[] cipherData = Algorithm.osslToJsse(algorithm);
        String realName = cipherData[3];
        int[] lengths = Algorithm.osslKeyIvLength(algorithm);
        int keyLen = lengths[0];
        int ivLen = lengths[1];
        if (iv.length != ivLen) {
            throw new IOException("Illegal IV length");
        }
        byte[] salt = new byte[8];
        System.arraycopy(iv, 0, salt, 0, 8);
        OpenSSLPBEParametersGenerator pGen = new OpenSSLPBEParametersGenerator();
        pGen.init(PBEParametersGenerator.PKCS5PasswordToBytes(passwd), salt);
        KeyParameter param = (KeyParameter) pGen.generateDerivedParameters(keyLen * 8);
        SecretKey secretKey = new javax.crypto.spec.SecretKeySpec(param.getKey(), realName);
        Cipher cipher = SecurityHelper.getCipher(realName);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
        return cipher.doFinal(decoded);
    }

    /**
     * Reads in a X509Certificate.
     *
     * @return the X509Certificate
     * @throws IOException if an I/O error occured
     */
    private static X509Certificate readCertificate(BufferedReader in, String endMarker) throws IOException {
        String line;
        StringBuilder buf = new StringBuilder();

        while ((line = in.readLine()) != null) {
            if (line.indexOf(endMarker) != -1) {
                break;
            }
            buf.append(line.trim());
        }

        if (line == null) {
            throw new IOException(endMarker + " not found");
        }

        try {
            CertificateFactory certFact = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream bIn = new ByteArrayInputStream(Base64.decode(buf.toString()));
            return (X509Certificate) certFact.generateCertificate(bIn);
        } catch (Exception e) {
            throw new IOException("problem parsing cert: " + e.toString());
        }
    }

    private static X509AuxCertificate readAuxCertificate(BufferedReader in,String  endMarker) throws IOException {
        String          line;
        StringBuilder    buf = new StringBuilder();

        while ((line = in.readLine()) != null) {
            if (line.indexOf(endMarker) != -1) {
                break;
            }
            buf.append(line.trim());
        }

        if (line == null) {
            throw new IOException(endMarker + " not found");
        }

        ASN1InputStream try1 = new ASN1InputStream(Base64.decode(buf.toString()));
        ByteArrayInputStream bIn = new ByteArrayInputStream((try1.readObject()).getEncoded());

        try {
            CertificateFactory certFact = CertificateFactory.getInstance("X.509");
            X509Certificate bCert = (X509Certificate)certFact.generateCertificate(bIn);
            ASN1Sequence aux = (ASN1Sequence)try1.readObject();
            X509Aux ax = null;
            if(aux != null) {
                ax = new X509Aux();
                int ix = 0;
                if(aux.size() > ix && aux.getObjectAt(ix) instanceof ASN1Sequence) {
                    ASN1Sequence trust = (ASN1Sequence)aux.getObjectAt(ix++);
                    for(int i=0;i<trust.size();i++) {
                        ax.trust.add(((ASN1ObjectIdentifier)trust.getObjectAt(i)).getId());
                    }
                }
                if(aux.size() > ix && aux.getObjectAt(ix) instanceof ASN1TaggedObject && ((ASN1TaggedObject)aux.getObjectAt(ix)).getTagNo() == 0) {
                    ASN1Sequence reject = (ASN1Sequence)((ASN1TaggedObject)aux.getObjectAt(ix++)).getObject();
                    for(int i=0;i<reject.size();i++) {
                        ax.reject.add(((ASN1ObjectIdentifier)reject.getObjectAt(i)).getId());
                    }
                }
                if(aux.size()>ix && aux.getObjectAt(ix) instanceof DERUTF8String) {
                    ax.alias = ((DERUTF8String)aux.getObjectAt(ix++)).getString();
                }
                if(aux.size()>ix && aux.getObjectAt(ix) instanceof DEROctetString) {
                    ax.keyid = ((DEROctetString)aux.getObjectAt(ix++)).getOctets();
                }
                if(aux.size() > ix && aux.getObjectAt(ix) instanceof ASN1TaggedObject && ((ASN1TaggedObject)aux.getObjectAt(ix)).getTagNo() == 1) {
                    ASN1Sequence other = (ASN1Sequence)((ASN1TaggedObject)aux.getObjectAt(ix++)).getObject();
                    for(int i=0;i<other.size();i++) {
                        ax.other.add((ASN1Primitive)(other.getObjectAt(i)));
                    }
                }
            }
            return new X509AuxCertificate(bCert,ax);
        } catch (Exception e) {
            throw new IOException("problem parsing cert: " + e.toString());
        }
    }

    /**
     * Reads in a X509CRL.
     *
     * @return the X509CRL
     * @throws IOException if an I/O error occured
     */
    private static X509CRL readCRL(BufferedReader in, String endMarker) throws IOException {
        String line;
        StringBuilder buf = new StringBuilder();

        while ((line = in.readLine()) != null) {
            if (line.indexOf(endMarker) != -1) {
                break;
            }
            buf.append(line.trim());
        }

        if (line == null) {
            throw new IOException(endMarker + " not found");
        }

        try {
            CertificateFactory certFact = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream bIn = new ByteArrayInputStream(Base64.decode(buf.toString()));
            return (X509CRL) certFact.generateCRL(bIn);
        } catch (Exception e) {
            throw new IOException("problem parsing cert: " + e.toString());
        }
    }

    /**
     * Reads in a PKCS10 certification request.
     *
     * @return the certificate request.
     * @throws IOException if an I/O error occured
     */
    private static PKCS10Request readCertificateRequest(BufferedReader in, String endMarker) throws IOException {
        String line;
        StringBuilder buf = new StringBuilder();

        while ((line = in.readLine()) != null) {
            if (line.indexOf(endMarker) != -1) {
                break;
            }
            buf.append(line.trim());
        }

        if (line == null) {
            throw new IOException(endMarker + " not found");
        }

        try {
            return new PKCS10Request(Base64.decode(buf.toString()));
        } catch (Exception e) {
            throw new IOException("problem parsing cert: " + e.toString());
        }
    }

    private static void writeHexEncoded(BufferedWriter out, byte[] bytes) throws IOException {
        bytes = Hex.encode(bytes);
        for (int i = 0; i != bytes.length; i++) {
            out.write((char)bytes[i]);
        }
    }

    private static void writeEncoded(BufferedWriter out, byte[] bytes) throws IOException {
        char[]  buf = new char[64];
        bytes = Base64.encode(bytes);
        for (int i = 0; i < bytes.length; i += buf.length) {
            int index = 0;

            while (index != buf.length) {
                if ((i + index) >= bytes.length) {
                    break;
                }
                buf[index] = (char)bytes[i + index];
                index++;
            }
            out.write(buf, 0, index);
            out.newLine();
        }
    }

    /**
     * Reads in a PKCS7 object. This returns a ContentInfo object suitable for use with the CMS
     * API.
     *
     * @return the X509Certificate
     * @throws IOException if an I/O error occured
     */
    private static CMSSignedData readPKCS7(BufferedReader in, char[] p, String endMarker) throws IOException {
        String line;
        StringBuilder buf = new StringBuilder();
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();

        while ((line = in.readLine()) != null) {
            if (line.indexOf(endMarker) != -1) {
                break;
            }
            line = line.trim();
            buf.append(line.trim());
            Base64.decode(buf.substring(0, (buf.length() / 4) * 4), bOut);
            buf.delete(0, (buf.length() / 4) * 4);
        }
        if (buf.length() != 0) {
            throw new RuntimeException("base64 data appears to be truncated");
        }
        if (line == null) {
            throw new IOException(endMarker + " not found");
        }
        try {
            ASN1InputStream aIn = new ASN1InputStream(bOut.toByteArray());
            return new CMSSignedData(ContentInfo.getInstance(aIn.readObject()));
        } catch (Exception e) {
            throw new IOException("problem parsing PKCS7 object: " + e.toString());
        }
    }
}// PEM
