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
package org.jruby.ext.openssl;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.HashMap;

import java.security.SecureRandom;
import javax.crypto.spec.DHParameterSpec;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.openssl.x509store.PEMInputOutput;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.runtime.Visibility;

import static org.jruby.ext.openssl.PKey._PKey;

/**
 * OpenSSL::PKey::DH implementation.
 *
 * @author <a href="mailto:bill.dortch@gmail.com">Bill Dortch</a>
 */
public class PKeyDH extends PKey {
    private static final long serialVersionUID = 293266329939132250L;

    // parameters used in generating 'p'; see [ossl]/crypto/dh/dh_gen.c #dh_builtin_genparams
    private static final BigInteger GEN_2_ADD_PARAM = BigInteger.valueOf(24);
    private static final BigInteger GEN_2_REM_PARAM = BigInteger.valueOf(11);
    private static final BigInteger GEN_5_ADD_PARAM = BigInteger.valueOf(10);
    private static final BigInteger GEN_5_REM_PARAM = BigInteger.valueOf(3);
    private static final BigInteger DEFAULT_ADD_PARAM = BigInteger.valueOf(2);
    private static final BigInteger DEFAULT_REM_PARAM = BigInteger.ONE;

    private static final BigInteger TWO = BigInteger.valueOf(2);

    // from [ossl]/crypto/dh/dh.h
    private static final int OPENSSL_DH_MAX_MODULUS_BITS = 10000;

    private static ObjectAllocator PKEYDH_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new PKeyDH(runtime, klass);
        }
    };

    public static void createPKeyDH(Ruby runtime, RubyModule pkeyModule, RubyClass pkeyClass) {
        RubyClass dh = pkeyModule.defineClassUnder("DH", pkeyClass, PKEYDH_ALLOCATOR);

        RubyClass pkeyError = pkeyModule.getClass("PKeyError");
        pkeyModule.defineClassUnder("DHError",pkeyError,pkeyError.getAllocator());

        dh.defineAnnotatedMethods(PKeyDH.class);
    }

    public static RaiseException newDHError(Ruby runtime, String message) {
        return Utils.newError(runtime, _PKey(runtime).getClass("DHError"), message);
    }

    private static SecureRandom _secureRandom;

    private static SecureRandom getSecureRandom() {
        SecureRandom rand;
        if ((rand = _secureRandom) != null) {
            return rand;
        }
        // FIXME: do we want a particular algorithm / provider? BC?
        return _secureRandom = new SecureRandom();
    }

    // transient because: we do not want these value serialized (insecure)
    // volatile because: permits unsynchronized reads in some cases
    private transient volatile BigInteger dh_p;
    private transient volatile BigInteger dh_g;
    private transient volatile BigInteger dh_pub_key;
    private transient volatile BigInteger dh_priv_key;

    // FIXME! need to figure out what it means in MRI/OSSL code to
    // claim a DH is(/has) private if an engine is present -- doesn't really
    // map to Java implementation.

    //private volatile boolean haveEngine;

    public PKeyDH(Ruby runtime, RubyClass clazz) {
        super(runtime, clazz);
    }

    @JRubyMethod(name="initialize", rest=true, visibility = Visibility.PRIVATE)
    public synchronized IRubyObject dh_initialize(IRubyObject[] args) {
        Ruby runtime = getRuntime();
        if (this.dh_p != null || this.dh_g != null || this.dh_pub_key != null || this.dh_priv_key != null) {
            throw newDHError(runtime, "illegal initialization");
        }
        int argc = Arity.checkArgumentCount(runtime, args, 0, 2);
        if (argc > 0) {
            IRubyObject arg0 = args[0];
            if (argc == 1 && arg0 instanceof RubyString) {
                try {
                    DHParameterSpec spec = PEMInputOutput.readDHParameters(new StringReader(arg0.toString()));
                    if (spec == null) {
                        spec = org.jruby.ext.openssl.impl.PKey.readDHParameter(arg0.asString().getByteList().bytes());
                    }
                    if (spec == null) {
                        throw runtime.newArgumentError("invalid DH PARAMETERS");
                    }
                    this.dh_p = spec.getP();
                    this.dh_g = spec.getG();
                } catch (NoClassDefFoundError ncdfe) {
                    throw newDHError(runtime, OpenSSLReal.bcExceptionMessage(ncdfe));
                } catch (IOException e) {
                    throw runtime.newIOErrorFromException(e);
                }
            } else {
                int bits = RubyNumeric.fix2int(arg0);
                // g defaults to 2
                int gval = argc == 2 ? RubyNumeric.fix2int(args[1]) : 2;
                BigInteger p;
                try {
                    p = generateP(bits, gval);
                } catch(IllegalArgumentException e) {
                    throw runtime.newArgumentError(e.getMessage());
                }
                BigInteger g = BigInteger.valueOf(gval);
                BigInteger x = generateX(p);
                BigInteger y = generateY(p, g, x);
                this.dh_p = p;
                this.dh_g = g;
                this.dh_priv_key = x;
                this.dh_pub_key = y;
            }
        }
        return this;
    }

    public static BigInteger generateP(int bits, int g) {

        // FIXME? I'm following algorithms used in OpenSSL, could use JCE provider instead.
        // (Note that I tried that, but got mystifying values of g returned by the param generator.
        // In any case, in OpenSSL/MRI-OpenSSL, the caller supplies g, or it defaults to 2.)

        // see [ossl]/crypto/dh/dh_gen.c #dh_builtin_genparams

        if (bits < 2) throw new IllegalArgumentException("invalid bit length");
        if (g < 2) throw new IllegalArgumentException("invalid generator");

        // generate safe prime meeting appropriate add/rem (mod) criteria

        switch(g) {
        case 2:
            // add = 24, rem = 11
            return BN.generatePrime(bits, true, GEN_2_ADD_PARAM, GEN_2_REM_PARAM);
        case 5:
            // add = 10, rem = 3
            return BN.generatePrime(bits, true, GEN_5_ADD_PARAM, GEN_5_REM_PARAM);
        default:
            // add = 2, rem = 1
            return BN.generatePrime(bits, true, DEFAULT_ADD_PARAM, DEFAULT_REM_PARAM);
        }
    }

    public static BigInteger generateX(BigInteger p, int limit) {
        if (limit < 0) throw new IllegalArgumentException("invalid limit");

        BigInteger x;
        SecureRandom secureRandom = getSecureRandom();
        // adapting algorithm from org.bouncycastle.crypto.generators.DHKeyGeneratorHelper,
        // which seems a little stronger (?) than OpenSSL's (OSSL just generates a random,
        // while BC generates a random potential prime [for limit > 0], though it's not
        // subject to Miller-Rabin [certainty = 0], but is subject to other constraints)
        // see also [ossl]/crypto/dh/dh_key.c #generate_key
        if (limit == 0) {
            BigInteger pSub2 = p.subtract(TWO);
            do {
                x = BN.getRandomBIInRange(pSub2, secureRandom);
            } while (x.equals(BigInteger.ZERO));
        } else {
            do {
                // generate potential prime, though with 0 certainty (no Miller-Rabin tests)
                x = new BigInteger(limit, 0, secureRandom);
            } while (x.equals(BigInteger.ZERO));
        }
        return x;
    }

    public static BigInteger generateX(BigInteger p) {
        // OpenSSL default l(imit) is p bits - 1 -- see [ossl]/crypto/dh/dh_key.c #generate_key
        return generateX(p, p.bitLength() - 1);
    }

    public static BigInteger generateY(BigInteger p, BigInteger g, BigInteger x) {
        return g.modPow(x, p);
    }

    public static BigInteger generateY(BigInteger p, int g, BigInteger x) {
        return generateY(p, BigInteger.valueOf(g), x);
    }

    @JRubyMethod(name="generate_key!")
    public synchronized IRubyObject dh_generate_key() {
        BigInteger p, g, x, y;
        if ((p = this.dh_p) == null || (g = this.dh_g) == null) {
            throw newDHError(getRuntime(), "can't generate key");
        }
        if ((x = this.dh_priv_key) == null) {
            x = generateX(p);
        }
        y = generateY(p, g, x);
        this.dh_priv_key = x;
        this.dh_pub_key = y;
        return this;
    }

    @JRubyMethod(name="compute_key")
    public synchronized IRubyObject dh_compute_key(IRubyObject other_pub_key) {
        BigInteger x, y, p;
        if ((y = BN.getBigInteger(other_pub_key)) == null) {
            throw getRuntime().newArgumentError("invalid public key");
        }
        if ((x = this.dh_priv_key) == null || (p = this.dh_p) == null) {
            throw newDHError(getRuntime(), "can't compute key");
        }
        int plen;
        if ((plen = p.bitLength()) == 0 || plen > OPENSSL_DH_MAX_MODULUS_BITS) {
            throw newDHError(getRuntime(), "can't compute key");
        }
        return getRuntime().newString(new ByteList(computeKey(y, x, p), false));
    }

    public static byte[] computeKey(BigInteger y, BigInteger x, BigInteger p) {
        return y.modPow(x, p).toByteArray();
    }

    @JRubyMethod(name="public?")
    public IRubyObject dh_is_public() {
        return getRuntime().newBoolean(dh_pub_key != null);
    }

    @JRubyMethod(name="private?")
    public IRubyObject dh_is_private() {
        // FIXME! need to figure out what it means in MRI/OSSL code to
        // claim a DH is private if an engine is present -- doesn't really
        // map to Java implementation.
        return getRuntime().newBoolean(dh_priv_key != null /* || haveEngine */);
    }

    @JRubyMethod(name={"export", "to_pem", "to_s"})
    public IRubyObject dh_export() {
        BigInteger p, g;
        synchronized(this) {
            p = this.dh_p;
            g = this.dh_g;
        }
        StringWriter w = new StringWriter();
        try {
            PEMInputOutput.writeDHParameters(w, new DHParameterSpec(p, g));
            w.flush();
            w.close();
        } catch (NoClassDefFoundError ncdfe) {
            throw newDHError(getRuntime(), OpenSSLReal.bcExceptionMessage(ncdfe));
        } catch (IOException e) {
            // shouldn't happen (string/buffer io only)
            throw getRuntime().newIOErrorFromException(e);
        }
        return getRuntime().newString(w.toString());
    }

    @JRubyMethod(name = "to_der")
    public IRubyObject dh_to_der() {
        BigInteger p, g;
        synchronized (this) {
            p = this.dh_p;
            g = this.dh_g;
        }
        try {
            byte[] bytes = org.jruby.ext.openssl.impl.PKey.toDerDHKey(p, g);
            return RubyString.newString(getRuntime(), bytes);
        } catch (NoClassDefFoundError ncdfe) {
            throw newDHError(getRuntime(), OpenSSLReal.bcExceptionMessage(ncdfe));
        } catch (IOException ioe) {
            throw newDHError(getRuntime(), ioe.getMessage());
        }
    }

    @JRubyMethod(name="params")
    public IRubyObject dh_get_params() {
        BigInteger p, g, x, y;
        synchronized(this) {
            p = this.dh_p;
            g = this.dh_g;
            x = this.dh_priv_key;
            y = this.dh_pub_key;
        }
        Ruby runtime = getRuntime();
        HashMap<IRubyObject, IRubyObject> params = new HashMap<IRubyObject, IRubyObject>();

        params.put(runtime.newString("p"), BN.newBN(runtime, p));
        params.put(runtime.newString("g"), BN.newBN(runtime, g));
        params.put(runtime.newString("pub_key"), BN.newBN(runtime, x));
        params.put(runtime.newString("priv_key"), BN.newBN(runtime, y));

        return RubyHash.newHash(runtime, params, runtime.getNil());
    }

    // don't need synchronized as value is volatile
    @JRubyMethod(name="p")
    public IRubyObject dh_get_p() {
        return getBN(dh_p);
    }

    @JRubyMethod(name="p=")
    public synchronized IRubyObject dh_set_p(IRubyObject arg) {
        this.dh_p = BN.getBigInteger(arg);
        return arg;
    }

    // don't need synchronized as value is volatile
    @JRubyMethod(name="g")
    public IRubyObject dh_get_g() {
        return getBN(dh_g);
    }

    @JRubyMethod(name="g=")
    public synchronized IRubyObject dh_set_g(IRubyObject arg) {
        this.dh_g = BN.getBigInteger(arg);
        return arg;
    }

    // don't need synchronized as value is volatile
    @JRubyMethod(name="pub_key")
    public IRubyObject dh_get_pub_key() {
        return getBN(dh_pub_key);
    }

    @JRubyMethod(name="pub_key=")
    public synchronized IRubyObject dh_set_pub_key(IRubyObject arg) {
        this.dh_pub_key = BN.getBigInteger(arg);
        return arg;
    }

    // don't need synchronized as value is volatile
    @JRubyMethod(name="priv_key")
    public IRubyObject dh_get_priv_key() {
        return getBN(dh_priv_key);
    }

    @JRubyMethod(name="priv_key=")
    public synchronized IRubyObject dh_set_priv_key(IRubyObject arg) {
        this.dh_priv_key = BN.getBigInteger(arg);
        return arg;
    }

    private IRubyObject getBN(BigInteger value) {
        if (value != null) {
            return BN.newBN(getRuntime(), value);
        }
        return getRuntime().getNil();
    }

    @Override // override differently-named abstract method from PKey
    public IRubyObject to_der() {
        return dh_to_der();
    }

}
