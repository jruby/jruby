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
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyBignum;
import org.jruby.RubyFile;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Visibility;

import org.jruby.ext.openssl.impl.CipherSpec;
import org.jruby.ext.openssl.x509store.PEMInputOutput;
import static org.jruby.ext.openssl.PKey._PKey;
import static org.jruby.ext.openssl.OpenSSLReal.debug;
import static org.jruby.ext.openssl.OpenSSLReal.debugStackTrace;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class PKeyRSA extends PKey {
    private static final long serialVersionUID = 3675324750727019454L;

    private static ObjectAllocator PKEYRSA_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new PKeyRSA(runtime, klass);
        }
    };

    public static void createPKeyRSA(Ruby runtime, RubyModule mPKey) {
        RubyClass cRSA = mPKey.defineClassUnder("RSA",mPKey.getClass("PKey"),PKEYRSA_ALLOCATOR);
        RubyClass pkeyError = mPKey.getClass("PKeyError");
        mPKey.defineClassUnder("RSAError",pkeyError,pkeyError.getAllocator());

        cRSA.defineAnnotatedMethods(PKeyRSA.class);

        cRSA.setConstant("PKCS1_PADDING",runtime.newFixnum(1));
        cRSA.setConstant("SSLV23_PADDING",runtime.newFixnum(2));
        cRSA.setConstant("NO_PADDING",runtime.newFixnum(3));
        cRSA.setConstant("PKCS1_OAEP_PADDING",runtime.newFixnum(4));
   }

    public static RaiseException newRSAError(Ruby runtime, String message) {
        return Utils.newError(runtime, _PKey(runtime).getClass("RSAError"), message);
    }

    public PKeyRSA(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    public PKeyRSA(Ruby runtime, RubyClass type, RSAPrivateCrtKey privKey, RSAPublicKey pubKey) {
        super(runtime, type);
        this.privKey = privKey;
        this.pubKey = pubKey;
    }

    public PKeyRSA(Ruby runtime, RubyClass type, RSAPublicKey pubKey) {
        this(runtime, type, null, pubKey);
    }

    private transient volatile RSAPrivateCrtKey privKey;
    private transient volatile RSAPublicKey pubKey;

    // fields to hold individual RSAPublicKeySpec components. this allows
    // a public key to be constructed incrementally, as required by the
    // current implementation of Net::SSH.
    // (see net-ssh-1.1.2/lib/net/ssh/transport/ossl/buffer.rb #read_keyblob)
    private transient volatile BigInteger rsa_e;
    private transient volatile BigInteger rsa_n;

    private transient volatile BigInteger rsa_d;
    private transient volatile BigInteger rsa_p;
    private transient volatile BigInteger rsa_q;
    private transient volatile BigInteger rsa_dmp1;
    private transient volatile BigInteger rsa_dmq1;
    private transient volatile BigInteger rsa_iqmp;

    @Override
    PublicKey getPublicKey() {
        return pubKey;
    }

    @Override
    PrivateKey getPrivateKey() {
        return privKey;
    }

    @Override
    String getAlgorithm() {
        return "RSA";
    }

    @JRubyMethod(name = "generate", meta = true, rest = true)
    public static IRubyObject generate(IRubyObject recv, IRubyObject[] args) {
        BigInteger exp = RSAKeyGenParameterSpec.F4;
        if (Arity.checkArgumentCount(recv.getRuntime(), args, 1, 2) == 2) {
            if (args[1] instanceof RubyFixnum) {
                exp = BigInteger.valueOf(RubyNumeric.num2long(args[1]));
            } else {
                exp = ((RubyBignum) args[1]).getValue();
            }
        }
        int keysize = RubyNumeric.fix2int(args[0]);
        PKeyRSA rsa = new PKeyRSA(recv.getRuntime(), (RubyClass) recv);
        rsaGenerate(rsa, keysize, exp);
        return rsa;
    }

    /*
     * c: rsa_generate
     */
    private static void rsaGenerate(PKeyRSA rsa, int keysize, BigInteger exp) throws RaiseException {
        try {
            KeyPairGenerator gen = SecurityHelper.getKeyPairGenerator("RSA");
            if (gen.getProvider().getName().equals("IBMJCEFIPS")) {
                // IBMJCEFIPS does not support parameters
                gen.initialize(keysize);
            } else {
                gen.initialize(new RSAKeyGenParameterSpec(keysize, exp), new SecureRandom());
            }
            KeyPair pair = gen.generateKeyPair();
            rsa.privKey = (RSAPrivateCrtKey) (pair.getPrivate());
            rsa.pubKey = (RSAPublicKey) (pair.getPublic());
        } catch (Exception e) {
            throw newRSAError(rsa.getRuntime(), e.getMessage());
        }
    }

    @JRubyMethod(rest = true, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(final ThreadContext context,
        final IRubyObject[] args, final Block block) {
        final Ruby runtime = context.runtime;
        IRubyObject arg; IRubyObject pass = null; char[] passwd = null;
        if (Arity.checkArgumentCount(runtime, args, 0, 2) == 0) {
            privKey = null;
            pubKey = null;
        } else {
            arg = args[0];
            if (args.length > 1) {
                pass = args[1];
            }
            if (arg instanceof RubyFixnum) {
                int keysize = RubyNumeric.fix2int(arg);
                BigInteger exp = RSAKeyGenParameterSpec.F4;
                if (null != pass && !pass.isNil()) {
                    exp = BigInteger.valueOf(RubyNumeric.num2long(pass));
                }
                rsaGenerate(this, keysize, exp);
            } else {
                if (pass != null && !pass.isNil()) {
                    passwd = pass.toString().toCharArray();
                }
                arg = OpenSSLImpl.to_der_if_possible(context, arg);
                RubyString str;
                if (arg instanceof RubyFile) {
                    str = (RubyString)((RubyFile)arg.dup()).read(context);
                } else {
                    str = arg.convertToString();
                }

                Object val = null;
                final KeyFactory rsaFactory;
                try {
                    rsaFactory = SecurityHelper.getKeyFactory("RSA");
                } catch (NoSuchAlgorithmException e) {
                    throw context.runtime.newRuntimeError("unsupported key algorithm (RSA)");
                } catch (RuntimeException e) {
                    throw context.runtime.newRuntimeError("unsupported key algorithm (RSA) " + e);
                }
                // TODO: ugly NoClassDefFoundError catching for no BC env. How can we remove this?
                boolean noClassDef = false;
                if ( val == null && ! noClassDef ) { // PEM_read_bio_RSAPrivateKey
                    try {
                        val = PEMInputOutput.readPrivateKey(new StringReader(str.toString()), passwd);
                    } catch (NoClassDefFoundError e) {
                        noClassDef = true; debugStackTrace(runtime, e);
                    } catch (Exception e) { debugStackTrace(runtime, e); }
                }
                if ( val == null && ! noClassDef )  { // PEM_read_bio_RSAPublicKey
                    try {
                        val = PEMInputOutput.readRSAPublicKey(new StringReader(str.toString()), passwd);
                    } catch (NoClassDefFoundError e) {
                        noClassDef = true; debugStackTrace(runtime, e);
                    } catch (Exception e) { debugStackTrace(runtime, e); }
                }
                if ( val == null && ! noClassDef ) { // PEM_read_bio_RSA_PUBKEY
                    try {
                        val = PEMInputOutput.readRSAPubKey(new StringReader(str.toString()));
                    } catch (NoClassDefFoundError e) {
                        noClassDef = true; debugStackTrace(runtime, e);
                    } catch (Exception e) { debugStackTrace(runtime, e); }
                }
                if ( val == null && ! noClassDef ) { // d2i_RSAPrivateKey_bio
                    try {
                        val = org.jruby.ext.openssl.impl.PKey.readRSAPrivateKey(str.getBytes());
                    } catch (NoClassDefFoundError e) {
                        noClassDef = true; debugStackTrace(runtime, e);
                    } catch (Exception e) { debugStackTrace(runtime, e); }
                }
                if ( val == null && ! noClassDef ) { // d2i_RSAPublicKey_bio
                    try {
                        val = org.jruby.ext.openssl.impl.PKey.readRSAPublicKey(str.getBytes());
                    } catch (NoClassDefFoundError e) {
                        noClassDef = true; debugStackTrace(runtime, e);
                    } catch (Exception e) { debugStackTrace(runtime, e); }
                }
                if ( val == null ) { // try to read PrivateKeyInfo.
                    try {
                        val = rsaFactory.generatePrivate(new PKCS8EncodedKeySpec(str.getBytes()));
                    }
                    catch (InvalidKeySpecException e) { debug(runtime, "PKeyRSA could not read private key", e); }
                    catch (RuntimeException e) { debugStackTrace(runtime, e); }
                }
                if ( val == null ) { // try to read SubjectPublicKeyInfo.
                    try {
                        val = rsaFactory.generatePublic(new X509EncodedKeySpec(str.getBytes()));
                    }
                    catch (InvalidKeySpecException e) { debug(runtime, "PKeyRSA could not read public key", e); }
                    catch (RuntimeException e) { debugStackTrace(runtime, e); }
                }
                if ( val == null ) {
                    throw newRSAError(context.runtime, "Neither PUB key nor PRIV key:");
                }

                if (val instanceof KeyPair) {
                    PrivateKey privateKey = ((KeyPair) val).getPrivate();
                    PublicKey publicKey = ((KeyPair) val).getPublic();
                    if (privateKey instanceof RSAPrivateCrtKey) {
                        privKey = (RSAPrivateCrtKey) privateKey;
                        pubKey = (RSAPublicKey) publicKey;
                    } else {
                        throw newRSAError(context.runtime, "Neither PUB key nor PRIV key: " + val.getClass().getName());
                    }
                } else if (val instanceof RSAPrivateCrtKey) {
                    privKey = (RSAPrivateCrtKey) val;
                    try {
                        pubKey = (RSAPublicKey) (rsaFactory.generatePublic(new RSAPublicKeySpec(privKey.getModulus(), privKey.getPublicExponent())));
                    } catch (GeneralSecurityException e) {
                        throw newRSAError(context.runtime, e.getMessage());
                    } catch (RuntimeException e) {
                        debugStackTrace(runtime, e);
                        throw newRSAError(context.runtime, e.toString());
                    }
                } else if (val instanceof RSAPublicKey) {
                    pubKey = (RSAPublicKey) val;
                    privKey = null;
                } else {
                    throw newRSAError(context.runtime, "Neither PUB key nor PRIV key: " + val.getClass().getName());
                }
            }
        }
        return this;
    }

    @JRubyMethod(name="public?")
    public IRubyObject public_p() {
        return pubKey != null ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    @JRubyMethod(name="private?")
    public IRubyObject private_p() {
        return privKey != null ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    @Override
    @JRubyMethod
    public IRubyObject to_der() {
        try {
            byte[] bytes = org.jruby.ext.openssl.impl.PKey.toDerRSAKey(pubKey, privKey);
            return RubyString.newString(getRuntime(), bytes);
        } catch (NoClassDefFoundError ncdfe) {
            throw newRSAError(getRuntime(), OpenSSLReal.bcExceptionMessage(ncdfe));
        } catch (IOException ioe) {
            throw newRSAError(getRuntime(), ioe.getMessage());
        }
    }

    @JRubyMethod
    public IRubyObject public_key() {
        PKeyRSA val = new PKeyRSA(getRuntime(),getMetaClass().getRealClass());
        val.privKey = null;
        val.pubKey = this.pubKey;
        return val;
    }

    @JRubyMethod
    public IRubyObject params() {
        ThreadContext ctx = getRuntime().getCurrentContext();
        RubyHash hash = RubyHash.newHash(getRuntime());
        if(privKey != null) {
            hash.op_aset(ctx, getRuntime().newString("iqmp"), BN.newBN(getRuntime(), privKey.getCrtCoefficient()));
            hash.op_aset(ctx, getRuntime().newString("n"), BN.newBN(getRuntime(), privKey.getModulus()));
            hash.op_aset(ctx, getRuntime().newString("d"), BN.newBN(getRuntime(), privKey.getPrivateExponent()));
            hash.op_aset(ctx, getRuntime().newString("p"), BN.newBN(getRuntime(), privKey.getPrimeP()));
            hash.op_aset(ctx, getRuntime().newString("e"), BN.newBN(getRuntime(), privKey.getPublicExponent()));
            hash.op_aset(ctx, getRuntime().newString("q"), BN.newBN(getRuntime(), privKey.getPrimeQ()));
            hash.op_aset(ctx, getRuntime().newString("dmq1"), BN.newBN(getRuntime(), privKey.getPrimeExponentQ()));
            hash.op_aset(ctx, getRuntime().newString("dmp1"), BN.newBN(getRuntime(), privKey.getPrimeExponentP()));

        } else {
            hash.op_aset(ctx, getRuntime().newString("iqmp"), BN.newBN(getRuntime(), BigInteger.ZERO));
            hash.op_aset(ctx, getRuntime().newString("n"), BN.newBN(getRuntime(), pubKey.getModulus()));
            hash.op_aset(ctx, getRuntime().newString("d"), BN.newBN(getRuntime(), BigInteger.ZERO));
            hash.op_aset(ctx, getRuntime().newString("p"), BN.newBN(getRuntime(), BigInteger.ZERO));
            hash.op_aset(ctx, getRuntime().newString("e"), BN.newBN(getRuntime(), pubKey.getPublicExponent()));
            hash.op_aset(ctx, getRuntime().newString("q"), BN.newBN(getRuntime(), BigInteger.ZERO));
            hash.op_aset(ctx, getRuntime().newString("dmq1"), BN.newBN(getRuntime(), BigInteger.ZERO));
            hash.op_aset(ctx, getRuntime().newString("dmp1"), BN.newBN(getRuntime(), BigInteger.ZERO));
        }
        return hash;
    }

    @JRubyMethod
    public IRubyObject to_text() {
        StringBuilder result = new StringBuilder();
        if (privKey != null) {
            int len = privKey.getModulus().bitLength();
            result.append("Private-Key: (").append(len).append(" bit)").append("\n");
            result.append("modulus:");
            addSplittedAndFormatted(result, privKey.getModulus());
            result.append("publicExponent: ").append(privKey.getPublicExponent()).append(" (0x").append(privKey.getPublicExponent().toString(16)).append(")\n");
            result.append("privateExponent:");
            addSplittedAndFormatted(result, privKey.getPrivateExponent());
            result.append("prime1:");
            addSplittedAndFormatted(result, privKey.getPrimeP());
            result.append("prime2:");
            addSplittedAndFormatted(result, privKey.getPrimeQ());
            result.append("exponent1:");
            addSplittedAndFormatted(result, privKey.getPrimeExponentP());
            result.append("exponent2:");
            addSplittedAndFormatted(result, privKey.getPrimeExponentQ());
            result.append("coefficient:");
            addSplittedAndFormatted(result, privKey.getCrtCoefficient());
        } else {
            int len = pubKey.getModulus().bitLength();
            result.append("Modulus (").append(len).append(" bit):");
            addSplittedAndFormatted(result, pubKey.getModulus());
            result.append("Exponent: ").append(pubKey.getPublicExponent()).append(" (0x").append(pubKey.getPublicExponent().toString(16)).append(")\n");
        }
        return getRuntime().newString(result.toString());
    }

    @JRubyMethod(name = { "export", "to_pem", "to_s" }, rest = true)
    public IRubyObject export(IRubyObject[] args) {
        StringWriter w = new StringWriter();
        org.jruby.runtime.Arity.checkArgumentCount(getRuntime(), args, 0, 2);
        CipherSpec ciph = null;
        char[] passwd = null;
        if (args.length > 0 && !args[0].isNil()) {
            org.jruby.ext.openssl.Cipher c = (org.jruby.ext.openssl.Cipher) args[0];
            ciph = new CipherSpec(c.getCipher(), c.getName(), c.getKeyLen() * 8);
            if (args.length > 1 && !args[1].isNil()) {
                passwd = args[1].toString().toCharArray();
            }
        }
        try {
            if (privKey != null) {
                PEMInputOutput.writeRSAPrivateKey(w, privKey, ciph, passwd);
            } else {
                PEMInputOutput.writeRSAPublicKey(w, pubKey);
            }
            w.close();
            return getRuntime().newString(w.toString());
        } catch (NoClassDefFoundError ncdfe) {
            throw newRSAError(getRuntime(), OpenSSLReal.bcExceptionMessage(ncdfe));
        } catch (IOException ioe) {
            throw newRSAError(getRuntime(), ioe.getMessage());
        }
    }

    private String getPadding(final int padding) {
        if ( padding < 1 || padding > 4 ) {
            throw newRSAError(getRuntime(), null);
        }
        // BC accepts "/NONE/*" but SunJCE doesn't. use "/ECB/*"
        String p = "/ECB/PKCS1Padding";
        if ( padding == 3 ) {
            p = "/ECB/NoPadding";
        } else if ( padding == 4 ) {
            p = "/ECB/OAEPWithMD5AndMGF1Padding";
        } else if ( padding == 2 ) {
            p = "/ECB/ISO9796-1Padding";
        }
        return p;
    }

    @JRubyMethod(rest = true)
    public IRubyObject private_encrypt(final ThreadContext context, final IRubyObject[] args) {
        int padding = 1;
        if ( Arity.checkArgumentCount(context.runtime, args, 1, 2) == 2 && ! args[1].isNil() ) {
            padding = RubyNumeric.fix2int(args[1]);
        }
        if ( privKey == null ) throw newRSAError(context.runtime, "private key needed.");
        return doCipherRSA(context.runtime, args[0], padding, Cipher.ENCRYPT_MODE, privKey);
    }

    @JRubyMethod(rest = true)
    public IRubyObject private_decrypt(final ThreadContext context, final IRubyObject[] args) {
        int padding = 1;
        if ( Arity.checkArgumentCount(context.runtime, args, 1, 2) == 2 && ! args[1].isNil())  {
            padding = RubyNumeric.fix2int(args[1]);
        }
        if ( privKey == null ) throw newRSAError(context.runtime, "private key needed.");
        return doCipherRSA(context.runtime, args[0], padding, Cipher.DECRYPT_MODE, privKey);
    }

    @JRubyMethod(rest = true)
    public IRubyObject public_encrypt(final ThreadContext context, final IRubyObject[] args) {
        int padding = 1;
        if ( Arity.checkArgumentCount(context.runtime, args, 1, 2) == 2 && ! args[1].isNil())  {
            padding = RubyNumeric.fix2int(args[1]);
        }
        return doCipherRSA(context.runtime, args[0], padding, Cipher.ENCRYPT_MODE, pubKey);
    }

    @JRubyMethod(rest = true)
    public IRubyObject public_decrypt(final ThreadContext context, final IRubyObject[] args) {
        int padding = 1;
        if ( Arity.checkArgumentCount(context.runtime, args, 1, 2) == 2 && ! args[1].isNil() ) {
            padding = RubyNumeric.fix2int(args[1]);
        }
        return doCipherRSA(context.runtime, args[0], padding, Cipher.DECRYPT_MODE, pubKey);
    }

    private RubyString doCipherRSA(final Ruby runtime,
        final IRubyObject content, final int padding,
        final int initMode, final Key initKey) {

        final String cipherPadding = getPadding(padding);
        final RubyString buffer = content.convertToString();
        try {
            Cipher engine = SecurityHelper.getCipher("RSA" + cipherPadding);
            engine.init(initMode, initKey);
            byte[] output = engine.doFinal(buffer.getBytes());
            return RubyString.newString(runtime, output);
        }
        catch (GeneralSecurityException gse) {
            throw newRSAError(runtime, gse.getMessage());
        }
    }

    @JRubyMethod(name="d=")
    public synchronized IRubyObject set_d(final ThreadContext context, IRubyObject value) {
        if ( privKey != null ) {
            throw newRSAError(context.runtime, "illegal modification");
        }
        rsa_d = BN.getBigInteger(value);
        generatePrivateKeyIfParams(context);
        return value;
    }

    @JRubyMethod(name="p=")
    public synchronized IRubyObject set_p(final ThreadContext context, IRubyObject value) {
        if ( privKey != null ) {
            throw newRSAError(context.runtime, "illegal modification");
        }
        rsa_p = BN.getBigInteger(value);
        generatePrivateKeyIfParams(context);
        return value;
    }

    @JRubyMethod(name="q=")
    public synchronized IRubyObject set_q(final ThreadContext context, IRubyObject value) {
        if ( privKey != null ) {
            throw newRSAError(context.runtime, "illegal modification");
        }
        rsa_q = BN.getBigInteger(value);
        generatePrivateKeyIfParams(context);
        return value;
    }

    @JRubyMethod(name="dmp1=")
    public synchronized IRubyObject set_dmp1(final ThreadContext context, IRubyObject value) {
        if ( privKey != null ) {
            throw newRSAError(context.runtime, "illegal modification");
        }
        rsa_dmp1 = BN.getBigInteger(value);
        generatePrivateKeyIfParams(context);
        return value;
    }

    @JRubyMethod(name="dmq1=")
    public synchronized IRubyObject set_dmq1(final ThreadContext context, IRubyObject value) {
        if ( privKey != null ) {
            throw newRSAError(context.runtime, "illegal modification");
        }
        rsa_dmq1 = BN.getBigInteger(value);
        generatePrivateKeyIfParams(context);
        return value;
    }

    @JRubyMethod(name="iqmp=")
    public synchronized IRubyObject set_iqmp(final ThreadContext context, IRubyObject value) {
        if ( privKey != null ) {
            throw newRSAError(context.runtime, "illegal modification");
        }
        rsa_iqmp = BN.getBigInteger(value);
        generatePrivateKeyIfParams(context);
        return value;
    }

    @JRubyMethod(name="iqmp")
    public synchronized IRubyObject get_iqmp() {
        BigInteger iqmp;
        if (privKey != null) {
            iqmp = privKey.getCrtCoefficient();
        } else {
            iqmp = rsa_iqmp;
        }
        if (iqmp != null) {
            return BN.newBN(getRuntime(), iqmp);
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(name="dmp1")
    public synchronized IRubyObject get_dmp1() {
        BigInteger dmp1;
        if (privKey != null) {
            dmp1 = privKey.getPrimeExponentP();
        } else {
            dmp1 = rsa_dmp1;
        }
        if (dmp1 != null) {
            return BN.newBN(getRuntime(), dmp1);
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(name="dmq1")
    public synchronized IRubyObject get_dmq1() {
        BigInteger dmq1;
        if (privKey != null) {
            dmq1 = privKey.getPrimeExponentQ();
        } else {
            dmq1 = rsa_dmq1;
        }
        if (dmq1 != null) {
            return BN.newBN(getRuntime(), dmq1);
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(name="d")
    public synchronized IRubyObject get_d() {
        BigInteger d;
        if (privKey != null) {
            d = privKey.getPrivateExponent();
        } else {
            d = rsa_d;
        }
        if (d != null) {
            return BN.newBN(getRuntime(), d);
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(name="p")
    public synchronized IRubyObject get_p() {
        BigInteger p;
        if (privKey != null) {
            p = privKey.getPrimeP();
        } else {
            p = rsa_p;
        }
        if (p != null) {
            return BN.newBN(getRuntime(), p);
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(name="q")
    public synchronized IRubyObject get_q() {
        BigInteger q;
        if (privKey != null) {
            q = privKey.getPrimeQ();
        } else {
            q = rsa_q;
        }
        if (q != null) {
            return BN.newBN(getRuntime(), q);
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(name="e")
    public synchronized IRubyObject get_e() {
        RSAPublicKey key;
        BigInteger e;
        if ((key = pubKey) != null) {
            e = key.getPublicExponent();
        } else if(privKey != null) {
            e = privKey.getPublicExponent();
        } else {
            e = rsa_e;
        }
        if (e != null) {
            return BN.newBN(getRuntime(), e);
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(name="e=")
    public synchronized IRubyObject set_e(final ThreadContext context, IRubyObject value) {
        this.rsa_e = BN.getBigInteger(value);

        if ( privKey == null ) {
            generatePrivateKeyIfParams(context);
        }
        if ( pubKey == null ) {
            generatePublicKeyIfParams(context);
        }

        return value;
    }

    @JRubyMethod(name="n")
    public synchronized IRubyObject get_n() {
        RSAPublicKey key;
        BigInteger n;
        if ((key = pubKey) != null) {
            n = key.getModulus();
        } else if(privKey != null) {
            n = privKey.getModulus();
        } else {
            n = rsa_n;
        }
        if (n != null) {
            return BN.newBN(getRuntime(), n);
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(name="n=")
    public synchronized IRubyObject set_n(final ThreadContext context, IRubyObject value) {
        this.rsa_n = BN.getBigInteger(value);

        if ( privKey == null ) {
            generatePrivateKeyIfParams(context);
        }
        if ( pubKey == null ) {
            generatePublicKeyIfParams(context);
        }

        return value;
    }

    private void generatePublicKeyIfParams(final ThreadContext context) {
        final Ruby runtime = context.runtime;

        if ( pubKey != null ) throw newRSAError(runtime, "illegal modification");

        BigInteger e, n;
        if ( (e = rsa_e) != null && (n = rsa_n) != null ) {
            final KeyFactory rsaFactory;
            try {
                rsaFactory = SecurityHelper.getKeyFactory("RSA");
            }
            catch (Exception ex) {
                throw runtime.newLoadError("unsupported key algorithm (RSA)");
            }

            try {
                pubKey = (RSAPublicKey) rsaFactory.generatePublic(new RSAPublicKeySpec(n, e));
            }
            catch (InvalidKeySpecException ex) {
                throw newRSAError(runtime, "invalid parameters");
            }
            rsa_e = null;
            rsa_n = null;
        }
    }

    private void generatePrivateKeyIfParams(final ThreadContext context) {
        final Ruby runtime = context.runtime;

        if ( privKey != null ) throw newRSAError(runtime, "illegal modification");

        if (rsa_e != null && rsa_n != null && rsa_p != null && rsa_q != null && rsa_d != null && rsa_dmp1 != null && rsa_dmq1 != null && rsa_iqmp != null) {
            final KeyFactory rsaFactory;
            try {
                rsaFactory = SecurityHelper.getKeyFactory("RSA");
            }
            catch (Exception ex) {
                throw runtime.newLoadError("unsupported key algorithm (RSA)");
            }

            try {
                privKey = (RSAPrivateCrtKey) rsaFactory.generatePrivate(
                    new RSAPrivateCrtKeySpec(rsa_n, rsa_e, rsa_d, rsa_p, rsa_q, rsa_dmp1, rsa_dmq1, rsa_iqmp)
                );
            }
            catch (InvalidKeySpecException ex) {
                throw newRSAError(runtime, "invalid parameters");
            }
            rsa_n = null;
            rsa_e = null;
            rsa_d = null;
            rsa_p = null;
            rsa_q = null;
            rsa_dmp1 = null;
            rsa_dmq1 = null;
            rsa_iqmp = null;
        }
    }

}// PKeyRSA
