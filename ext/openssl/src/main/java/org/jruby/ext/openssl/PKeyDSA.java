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
 * Copyright (C) 2007 Wiliam N Dortch <bill.dortch@gmail.com>
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
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.DSAKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.openssl.impl.CipherSpec;
import org.jruby.ext.openssl.x509store.PEMInputOutput;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;

import static org.jruby.ext.openssl.OpenSSLReal.bcExceptionMessage;
import static org.jruby.ext.openssl.OpenSSLReal.debug;
import static org.jruby.ext.openssl.OpenSSLReal.debugStackTrace;
import static org.jruby.ext.openssl.impl.PKey.readDSAPrivateKey;
import static org.jruby.ext.openssl.impl.PKey.readDSAPublicKey;
import static org.jruby.ext.openssl.impl.PKey.toDerDSAKey;
import static org.jruby.ext.openssl.PKey._PKey;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class PKeyDSA extends PKey {
    private static final long serialVersionUID = 2359742219218350277L;

    private static ObjectAllocator PKEYDSA_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new PKeyDSA(runtime, klass);
        }
    };

    public static void createPKeyDSA(final Ruby runtime, final RubyModule _PKey) {
        RubyClass _DSA = _PKey.defineClassUnder("DSA",_PKey.getClass("PKey"),PKEYDSA_ALLOCATOR);
        RubyClass _PKeyError = _PKey.getClass("PKeyError");
        _PKey.defineClassUnder("DSAError", _PKeyError, _PKeyError.getAllocator());

        _DSA.defineAnnotatedMethods(PKeyDSA.class);
    }

    public static RaiseException newDSAError(Ruby runtime, String message) {
        return Utils.newError(runtime, _PKey(runtime).getClass("DSAError"), message);
    }

    public PKeyDSA(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    public PKeyDSA(Ruby runtime, RubyClass type, DSAPrivateKey privKey, DSAPublicKey pubKey) {
        super(runtime, type);
        this.privKey = privKey;
        this.pubKey = pubKey;
    }

    public PKeyDSA(Ruby runtime, RubyClass type, DSAPublicKey pubKey) {
        this(runtime, type, null, pubKey);
    }

    private DSAPrivateKey privKey;
    private DSAPublicKey pubKey;

    // specValues holds individual DSAPublicKeySpec components. this allows
    // a public key to be constructed incrementally, as required by the
    // current implementation of Net::SSH.
    // (see net-ssh-1.1.2/lib/net/ssh/transport/ossl/buffer.rb #read_keyblob)
    private BigInteger[] specValues;

    private static final int SPEC_Y = 0;
    private static final int SPEC_P = 1;
    private static final int SPEC_Q = 2;
    private static final int SPEC_G = 3;

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
        return "DSA";
    }

    @JRubyMethod(name = "generate", meta = true)
    public static IRubyObject generate(IRubyObject self, IRubyObject arg) {
        final int keysize = RubyNumeric.fix2int(arg);
        PKeyDSA dsa = new PKeyDSA(self.getRuntime(), (RubyClass) self);
        dsaGenerate(dsa, keysize);
        return dsa;
    }

    /*
     * c: dsa_generate
     */
    private static void dsaGenerate(PKeyDSA dsa, int keysize) throws RaiseException {
        try {
            KeyPairGenerator gen = SecurityHelper.getKeyPairGenerator("DSA");
            gen.initialize(keysize, new SecureRandom());
            KeyPair pair = gen.generateKeyPair();
            dsa.privKey = (DSAPrivateKey) pair.getPrivate();
            dsa.pubKey = (DSAPublicKey) pair.getPublic();
        }
        catch (NoSuchAlgorithmException e) {
            throw newDSAError(dsa.getRuntime(), e.getMessage());
        }
        catch (RuntimeException e) {
            throw newDSAError(dsa.getRuntime(), e.getMessage());
        }
    }

    @JRubyMethod(rest = true, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(final ThreadContext context, final IRubyObject[] args) {
        final Ruby runtime = context.runtime;

        if ( Arity.checkArgumentCount(runtime, args, 0, 2) == 0 ) {
            this.privKey = null; this.pubKey = null; return this;
        }

        IRubyObject arg = args[0]; IRubyObject pass = null;
        if ( args.length > 1 ) pass = args[1];

        if ( arg instanceof RubyFixnum ) {
            int keysize = RubyNumeric.fix2int((RubyFixnum) arg);
            dsaGenerate(this, keysize); return this;
        }

        final char[] passwd = password(pass);
        final RubyString str = readInitArg(context, arg);

        Object key = null;
        final KeyFactory dsaFactory;
        try {
            dsaFactory = SecurityHelper.getKeyFactory("DSA");
        } catch (NoSuchAlgorithmException e) {
            throw runtime.newRuntimeError("unsupported key algorithm (DSA)");
        } catch (RuntimeException e) {
            throw runtime.newRuntimeError("unsupported key algorithm (DSA) " + e);
        }
        // TODO: ugly NoClassDefFoundError catching for no BC env. How can we remove this?
        boolean noClassDef = false;
        if ( key == null && ! noClassDef ) { // PEM_read_bio_DSAPrivateKey
            try {
                key = PEMInputOutput.readDSAPrivateKey(new StringReader(str.toString()), passwd);
            }
            catch (NoClassDefFoundError e) { noClassDef = true; debugStackTrace(runtime, e); }
            catch (Exception e) { debugStackTrace(runtime, e); }
        }
        if ( key == null && ! noClassDef ) { // PEM_read_bio_DSAPublicKey
            try {
                key = PEMInputOutput.readDSAPublicKey(new StringReader(str.toString()), passwd);
            }
            catch (NoClassDefFoundError e) { noClassDef = true; debugStackTrace(runtime, e); }
            catch (Exception e) { debugStackTrace(runtime, e); }
        }
        if ( key == null && ! noClassDef ) { // PEM_read_bio_DSA_PUBKEY
            try {
                key = PEMInputOutput.readDSAPubKey(new StringReader(str.toString()));
            }
            catch (NoClassDefFoundError e) { noClassDef = true; debugStackTrace(runtime, e); }
            catch (Exception e) { debugStackTrace(runtime, e); }
        }
        if ( key == null && ! noClassDef ) { // d2i_DSAPrivateKey_bio
            try {
                key = readDSAPrivateKey(dsaFactory, str.getBytes());
            }
            catch (NoClassDefFoundError e) { noClassDef = true; debugStackTrace(runtime, e); }
            catch (InvalidKeySpecException e) { debug(runtime, "PKeyDSA could not read private key", e); }
            catch (IOException e) { debug(runtime, "PKeyDSA could not read private key", e); }
            catch (RuntimeException e) {
                if ( isKeyGenerationFailure(e) ) debug(runtime, "PKeyDSA could not read private key", e);
                else debugStackTrace(runtime, e);
            }
        }
        if ( key == null && ! noClassDef ) { // d2i_DSA_PUBKEY_bio
            try {
                key = readDSAPublicKey(dsaFactory, str.getBytes());
            }
            catch (NoClassDefFoundError e) { noClassDef = true; debugStackTrace(runtime, e); }
            catch (InvalidKeySpecException e) { debug(runtime, "PKeyDSA could not read public key", e); }
            catch (IOException e) { debug(runtime, "PKeyDSA could not read public key", e); }
            catch (RuntimeException e) {
                if ( isKeyGenerationFailure(e) ) debug(runtime, "PKeyDSA could not read public key", e);
                else debugStackTrace(runtime, e);
            }
        }

        if ( key == null ) key = tryPKCS8EncodedKey(runtime, dsaFactory, str.getBytes());
        if ( key == null ) key = tryX509EncodedKey(runtime, dsaFactory, str.getBytes());

        if ( key == null ) throw newDSAError(runtime, "Neither PUB key nor PRIV key:");

        if ( key instanceof KeyPair ) {
            PublicKey publicKey = ((KeyPair) key).getPublic();
            PrivateKey privateKey = ((KeyPair) key).getPrivate();
            if ( ! ( privateKey instanceof DSAPrivateKey ) ) {
                if ( privateKey == null ) {
                    throw newDSAError(runtime, "Neither PUB key nor PRIV key: (private key is null)");
                }
                throw newDSAError(runtime, "Neither PUB key nor PRIV key: (invalid key type " + privateKey.getClass().getName() + ")");
            }
            this.privKey = (DSAPrivateKey) privateKey;
            this.pubKey = (DSAPublicKey) publicKey;
        }
        else if ( key instanceof DSAPrivateKey ) {
            this.privKey = (DSAPrivateKey) key;
        }
        else if ( key instanceof DSAPublicKey ) {
            this.pubKey = (DSAPublicKey) key; this.privKey = null;
        }
        else {
            throw newDSAError(runtime, "Neither PUB key nor PRIV key: "  + key.getClass().getName());
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
        final byte[] bytes;
        try {
            bytes = toDerDSAKey(pubKey, privKey);
        }
        catch (NoClassDefFoundError e) {
            throw newDSAError(getRuntime(), bcExceptionMessage(e));
        }
        catch (IOException e) {
            throw newDSAError(getRuntime(), e.getMessage());
        }
        return RubyString.newString(getRuntime(), bytes);
    }

    @JRubyMethod
    public IRubyObject to_text() {
        StringBuilder result = new StringBuilder();
        if (privKey != null) {
            int len = privKey.getParams().getP().bitLength();
            result.append("Private-Key: (").append(len).append(" bit)").append("\n");
            result.append("priv:");
            addSplittedAndFormatted(result, privKey.getX());
        }
        result.append("pub:");
        addSplittedAndFormatted(result, pubKey.getY());
        result.append("P:");
        addSplittedAndFormatted(result, pubKey.getParams().getP());
        result.append("Q:");
        addSplittedAndFormatted(result, pubKey.getParams().getQ());
        result.append("G:");
        addSplittedAndFormatted(result, pubKey.getParams().getG());
        return getRuntime().newString(result.toString());
    }

    @JRubyMethod
    public IRubyObject public_key() {
        PKeyDSA val = new PKeyDSA(getRuntime(),getMetaClass().getRealClass());
        val.privKey = null;
        val.pubKey = this.pubKey;
        return val;
    }

    @JRubyMethod(name = { "export", "to_pem", "to_s" }, rest = true)
    public IRubyObject export(IRubyObject[] args) {
        Arity.checkArgumentCount(getRuntime(), args, 0, 2);
        CipherSpec spec = null;
        char[] passwd = null;
        if (args.length > 0 && !args[0].isNil()) {
            org.jruby.ext.openssl.Cipher c = (org.jruby.ext.openssl.Cipher) args[0];
            spec = new CipherSpec(c.getCipher(), c.getName(), c.getKeyLen() * 8);
            if (args.length > 1 && !args[1].isNil()) {
                passwd = args[1].toString().toCharArray();
            }
        }
        try {
            final StringWriter writer = new StringWriter();
            if (privKey != null) {
                PEMInputOutput.writeDSAPrivateKey(writer, privKey, spec, passwd);
            } else {
                PEMInputOutput.writeDSAPublicKey(writer, pubKey);
            }
            return getRuntime().newString(writer.toString());
        } catch (NoClassDefFoundError ncdfe) {
            throw newDSAError(getRuntime(), bcExceptionMessage(ncdfe));
        } catch (IOException ioe) {
            throw newDSAError(getRuntime(), ioe.getMessage());
        }
    }

    @JRubyMethod
    public IRubyObject syssign(IRubyObject arg) {
        // TODO
        return getRuntime().getNil();
    }

    @JRubyMethod
    public IRubyObject sysverify(IRubyObject arg, IRubyObject arg2) {
        // TODO
        return getRuntime().getNil();
    }

    @JRubyMethod(name="p")
    public synchronized IRubyObject get_p() {
        // FIXME: return only for public?
        DSAKey key;
        BigInteger param;
        if ((key = this.pubKey) != null || (key = this.privKey) != null) {
            if ((param = key.getParams().getP()) != null) {
                return BN.newBN(getRuntime(), param);
            }
        } else if (specValues != null) {
            if ((param = specValues[SPEC_P]) != null) {
                return BN.newBN(getRuntime(), param);
            }
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(name="p=")
    public synchronized IRubyObject set_p(IRubyObject p) {
        return setKeySpecComponent(SPEC_P, p);
    }

    @JRubyMethod(name="q")
    public synchronized IRubyObject get_q() {
        // FIXME: return only for public?
        DSAKey key;
        BigInteger param;
        if ((key = this.pubKey) != null || (key = this.privKey) != null) {
            if ((param = key.getParams().getQ()) != null) {
                return BN.newBN(getRuntime(), param);
            }
        } else if (specValues != null) {
            if ((param = specValues[SPEC_Q]) != null) {
                return BN.newBN(getRuntime(), param);
            }
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(name="q=")
    public synchronized IRubyObject set_q(IRubyObject q) {
        return setKeySpecComponent(SPEC_Q, q);
    }

    @JRubyMethod(name="g")
    public synchronized IRubyObject get_g() {
        // FIXME: return only for public?
        DSAKey key;
        BigInteger param;
        if ((key = this.pubKey) != null || (key = this.privKey) != null) {
            if ((param = key.getParams().getG()) != null) {
                return BN.newBN(getRuntime(), param);
            }
        } else if (specValues != null) {
            if ((param = specValues[SPEC_G]) != null) {
                return BN.newBN(getRuntime(), param);
            }
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(name="g=")
    public synchronized IRubyObject set_g(IRubyObject g) {
        return setKeySpecComponent(SPEC_G, g);
    }

    @JRubyMethod(name="pub_key")
    public synchronized IRubyObject get_pub_key() {
        DSAPublicKey key;
        BigInteger param;
        if ((key = this.pubKey) != null) {
            return BN.newBN(getRuntime(), key.getY());
        } else if (specValues != null) {
            if ((param = specValues[SPEC_Y]) != null) {
                return BN.newBN(getRuntime(), param);
            }
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(name="priv_key")
    public synchronized IRubyObject get_priv_key() {
        DSAPrivateKey key;
        BigInteger param;
        if ((key = this.privKey) != null) {
            return BN.newBN(getRuntime(), key.getX());
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(name="pub_key=")
    public synchronized IRubyObject set_pub_key(IRubyObject pub_key) {
        return setKeySpecComponent(SPEC_Y, pub_key);
    }

    private IRubyObject setKeySpecComponent(int index, IRubyObject value) {
        BigInteger[] vals;
        // illegal to set if we already have a key for this component
        // FIXME: allow changes after keys are created? MRI doesn't prevent it...
        if (this.pubKey != null || this.privKey != null ||
                (vals = this.specValues) != null && vals[index] != null) {
            throw newDSAError(getRuntime(), "illegal modification");
        }
        // get the BigInteger value
        BigInteger bival = BN.getBigInteger(value);

        if (vals != null) {
            // we already have some vals stored, store this one, too
            vals[index] = bival;
            // check to see if we have all values yet
            for (int i = vals.length; --i >= 0; ) {
                if (vals[i] == null) {
                    // still missing components, return
                    return value;
                }
            }
            // we now have all components. create the key.
            DSAPublicKeySpec spec = new DSAPublicKeySpec(vals[SPEC_Y], vals[SPEC_P], vals[SPEC_Q], vals[SPEC_G]);
            try {
                this.pubKey = (DSAPublicKey) SecurityHelper.getKeyFactory("DSA").generatePublic(spec);
            } catch (InvalidKeySpecException e) {
                throw newDSAError(getRuntime(), "invalid keyspec");
            } catch (NoSuchAlgorithmException e) {
                throw newDSAError(getRuntime(), "unsupported key algorithm (DSA)");
            }
            // clear out the specValues
            this.specValues = null;

        } else {

            // first value received, save
            this.specValues = new BigInteger[4];
            this.specValues[index] = bival;
        }
        return value;
    }

}// PKeyDSA
