/*
Copyright (c) 2000-2014 The Legion of the Bouncy Castle Inc. (http://www.bouncycastle.org)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software
and associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial
portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
 */
package org.jruby.ext.openssl.impl.pem;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.cert.CRLException;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.DSAParameter;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.cert.X509AttributeCertificateHolder;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.io.pem.PemGenerationException;
import org.bouncycastle.util.io.pem.PemHeader;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemObjectGenerator;
import org.bouncycastle.x509.X509AttributeCertificate;

/**
 * PEM generator for the original set of PEM objects used in Open SSL.
 *
 * @note Based on <code>org.bouncycastle.openssl.MiscPEMGenerator</code>
 * (from BC 1.50) but "re-invented" for 1.47 compatibility
 *
 * @author kares
 */
public class MiscPEMGenerator implements PemObjectGenerator
{
    private static final ASN1ObjectIdentifier[] dsaOids =
    {
        X9ObjectIdentifiers.id_dsa,
        OIWObjectIdentifiers.dsaWithSHA1
    };

    private static final byte[] hexEncodingTable =
    {
        (byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4', (byte)'5', (byte)'6', (byte)'7',
        (byte)'8', (byte)'9', (byte)'A', (byte)'B', (byte)'C', (byte)'D', (byte)'E', (byte)'F'
    };

    private final Object obj;
    private final PEMEncryptor encryptor;

    public MiscPEMGenerator(Object o)
    {
        this.obj = o;              // use of this confuses some earlier JDKs.
        this.encryptor = null;
    }

    private MiscPEMGenerator(Object o, PEMEncryptor encryptor)
    {
        this.obj = o;
        this.encryptor = encryptor;
    }

    public static MiscPEMGenerator newInstance(final Object o,
        final String algorithm, final char[] password,
        final SecureRandom random) {
        return new MiscPEMGenerator(o, buildPEMEncryptor(algorithm, password, random));
    }

    private PemObject createPemObject(Object o)
        throws IOException
    {
        String  type;
        byte[]  encoding;

        if (o instanceof PemObject)
        {
            return (PemObject)o;
        }
        if (o instanceof PemObjectGenerator)
        {
            return ((PemObjectGenerator)o).generate();
        }
        if (o instanceof X509CertificateHolder)
        {
            type = "CERTIFICATE";
            encoding = ((X509CertificateHolder)o).getEncoded();
        }
        else if (o instanceof X509CRLHolder)
        {
            type = "X509 CRL";
            encoding = ((X509CRLHolder)o).getEncoded();
        }
        else if (o instanceof PrivateKeyInfo)
        {
            PrivateKeyInfo info = (PrivateKeyInfo)o;
            ASN1ObjectIdentifier algOID = info.getPrivateKeyAlgorithm().getAlgorithm();

            if (algOID.equals(PKCSObjectIdentifiers.rsaEncryption))
            {
                type = "RSA PRIVATE KEY";
                encoding = info.parsePrivateKey().toASN1Primitive().getEncoded();
            }
            else if (algOID.equals(dsaOids[0]) || algOID.equals(dsaOids[1]))
            {
                type = "DSA PRIVATE KEY";

                DSAParameter p = DSAParameter.getInstance(info.getPrivateKeyAlgorithm().getParameters());
                ASN1EncodableVector v = new ASN1EncodableVector();

                v.add(new ASN1Integer(0));
                v.add(new ASN1Integer(p.getP()));
                v.add(new ASN1Integer(p.getQ()));
                v.add(new ASN1Integer(p.getG()));

                BigInteger x = ASN1Integer.getInstance(info.parsePrivateKey()).getValue();
                BigInteger y = p.getG().modPow(x, p.getP());

                v.add(new ASN1Integer(y));
                v.add(new ASN1Integer(x));

                encoding = new DERSequence(v).getEncoded();
            }
            else if (algOID.equals(X9ObjectIdentifiers.id_ecPublicKey))
            {
                type = "EC PRIVATE KEY";
                encoding = info.parsePrivateKey().toASN1Primitive().getEncoded();
            }
            else
            {
                throw new IOException("Cannot identify private key");
            }
        }
        else if (o instanceof SubjectPublicKeyInfo)
        {
            type = "PUBLIC KEY";
            encoding = ((SubjectPublicKeyInfo)o).getEncoded();
        }
        else if (o instanceof X509AttributeCertificateHolder)
        {
            type = "ATTRIBUTE CERTIFICATE";
            encoding = ((X509AttributeCertificateHolder)o).getEncoded();
        }
        else if (o instanceof PKCS10CertificationRequest)
        {
            type = "CERTIFICATE REQUEST";
            encoding = ((PKCS10CertificationRequest)o).getEncoded();
        }
        else if (o instanceof ContentInfo)
        {
            type = "PKCS7";
            encoding = ((ContentInfo)o).getEncoded();
        }
        //
        // NOTE: added behaviour to provide backwards compatibility with 1.47 :
        //
        else if (o instanceof java.security.cert.X509Certificate) // 1.47 compatibility
        {
            type = "CERTIFICATE";
            try {
                encoding = ((java.security.cert.X509Certificate)o).getEncoded();
            }
            catch (CertificateEncodingException e) {
                throw new PemGenerationException("Cannot encode object: " + e.toString());
            }
        }
        else if (o instanceof java.security.cert.X509CRL) // 1.47 compatibility
        {
            type = "X509 CRL";
            try {
                encoding = ((java.security.cert.X509CRL)o).getEncoded();
            }
            catch (CRLException e) {
                throw new PemGenerationException("Cannot encode object: " + e.toString());
            }
        }
        else if (o instanceof java.security.KeyPair) // 1.47 compatibility
        {
            return createPemObject(((java.security.KeyPair)o).getPrivate());
        }
        else if (o instanceof java.security.PrivateKey) // 1.47 compatibility
        {
            PrivateKeyInfo info = new PrivateKeyInfo(
                (ASN1Sequence) ASN1Primitive.fromByteArray(((java.security.Key)o).getEncoded()));

            if (o instanceof java.security.interfaces.RSAPrivateKey)
            {
                type = "RSA PRIVATE KEY";

                encoding = info.parsePrivateKey().toASN1Primitive().getEncoded();
            }
            else if (o instanceof java.security.interfaces.DSAPrivateKey)
            {
                type = "DSA PRIVATE KEY";

                DSAParameter p = DSAParameter.getInstance(info.getPrivateKeyAlgorithm().getParameters());
                ASN1EncodableVector v = new ASN1EncodableVector();

                v.add(new DERInteger(0));
                v.add(new DERInteger(p.getP()));
                v.add(new DERInteger(p.getQ()));
                v.add(new DERInteger(p.getG()));

                BigInteger x = ((java.security.interfaces.DSAPrivateKey)o).getX();
                BigInteger y = p.getG().modPow(x, p.getP());

                v.add(new DERInteger(y));
                v.add(new DERInteger(x));

                encoding = new DERSequence(v).getEncoded();
            }
            else if (((java.security.PrivateKey)o).getAlgorithm().equals("ECDSA"))
            {
                type = "EC PRIVATE KEY";

                encoding = info.parsePrivateKey().toASN1Primitive().getEncoded();
            }
            else
            {
                throw new IOException("Cannot identify private key");
            }
        }
        else if (o instanceof java.security.PublicKey) // 1.47 compatibility
        {
            type = "PUBLIC KEY";

            encoding = ((java.security.PublicKey)o).getEncoded();
        }
        else if (o instanceof X509AttributeCertificate) // 1.47 compatibility
        {
            type = "ATTRIBUTE CERTIFICATE";
            encoding = ((X509AttributeCertificate)o).getEncoded();
        }
        //
        //
        //
        else
        {
            throw new PemGenerationException("unknown object passed - can't encode.");
        }

        if (encryptor != null) // NEW STUFF (NOT IN OLD)
        {
            String dekAlgName = Strings.toUpperCase(encryptor.getAlgorithm());

            // Note: For backward compatibility
            if (dekAlgName.equals("DESEDE"))
            {
                dekAlgName = "DES-EDE3-CBC";
            }

            byte[] iv = encryptor.getIV();
            byte[] encData = encryptor.encrypt(encoding);

            List<PemHeader> headers = new ArrayList<PemHeader>(2);

            headers.add(new PemHeader("Proc-Type", "4,ENCRYPTED"));
            headers.add(new PemHeader("DEK-Info", dekAlgName + "," + getHexEncoded(iv)));

            return new PemObject(type, headers, encData);
        }
        return new PemObject(type, encoding);
    }

    private static String getHexEncoded(byte[] bytes)
        throws IOException
    {
        char[] chars = new char[bytes.length * 2];

        for (int i = 0; i != bytes.length; i++)
        {
            int    v = bytes[i] & 0xff;

            chars[2 * i] = (char)(hexEncodingTable[(v >>> 4)]);
            chars[2 * i + 1]  = (char)(hexEncodingTable[v & 0xf]);
        }

        return new String(chars);
    }

    public PemObject generate()
        throws PemGenerationException
    {
        try
        {
            return createPemObject(obj);
        }
        catch (IOException e)
        {
            throw new PemGenerationException("encoding exception: " + e.getMessage(), e);
        }
    }

    //
    // NOTE: re-invented piece to provide compatibility for 1.47 - 1.48 :
    //
    private static PEMEncryptor buildPEMEncryptor(final String algorithm,
        final char[] password, final SecureRandom random) {

        int ivLength = algorithm.toUpperCase().startsWith("AES-") ? 16 : 8;
        final byte[] iv = new byte[ivLength];
        ( random == null ? new SecureRandom() : random ).nextBytes(iv);

        return new PEMEncryptor() {
            public String getAlgorithm() { return algorithm; }

            public byte[] getIV() { return iv; }

            public byte[] encrypt(byte[] encoding) throws PEMException {
                return PEMUtilities.crypt(true, encoding, password, algorithm, iv);
            }
        };
    }

    private static interface PEMEncryptor {

        public String getAlgorithm();

        public byte[] getIV();

        public byte[] encrypt(byte[] bytes) throws PEMException;

    }

}