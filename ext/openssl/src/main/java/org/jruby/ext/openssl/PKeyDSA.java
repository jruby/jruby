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
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

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
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

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
    
    public static void createPKeyDSA(Ruby runtime, RubyModule mPKey) {
        RubyClass cDSA = mPKey.defineClassUnder("DSA",mPKey.getClass("PKey"),PKEYDSA_ALLOCATOR);
        RubyClass pkeyError = mPKey.getClass("PKeyError");
        mPKey.defineClassUnder("DSAError",pkeyError,pkeyError.getAllocator());
        

        cDSA.defineAnnotatedMethods(PKeyDSA.class);
    }

    public static RaiseException newDSAError(Ruby runtime, String message) {
        return Utils.newError(runtime, "OpenSSL::PKey::DSAError", message);
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
    public static IRubyObject generate(IRubyObject recv, IRubyObject arg) {
        int keysize = RubyNumeric.fix2int(arg);
        PKeyDSA dsa = new PKeyDSA(recv.getRuntime(), (RubyClass) recv);
        dsaGenerate(dsa, keysize);
        return dsa;
    }

    /*
     * c: dsa_generate
     */
    private static void dsaGenerate(PKeyDSA dsa, int keysize) throws RaiseException {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("DSA");
            gen.initialize(keysize, new SecureRandom());
            KeyPair pair = gen.generateKeyPair();
            dsa.privKey = (DSAPrivateKey) (pair.getPrivate());
            dsa.pubKey = (DSAPublicKey) (pair.getPublic());
        } catch (Exception e) {
            throw newDSAError(dsa.getRuntime(), e.getMessage());
        }
    }

    @JRubyMethod(rest = true)
    public IRubyObject initialize(IRubyObject[] args) {
        IRubyObject arg;
        IRubyObject pass = null;
        char[] passwd = null;
        if (org.jruby.runtime.Arity.checkArgumentCount(getRuntime(), args, 0, 2) == 0) {
            privKey = null;
            pubKey = null;
        } else {
            arg = args[0];
            if (args.length > 1) {
                pass = args[1];
            }
            if (arg instanceof RubyFixnum) {
                int keysize = RubyNumeric.fix2int(arg);
                dsaGenerate(this, keysize);
            } else {
                if (pass != null && !pass.isNil()) {
                    passwd = pass.toString().toCharArray();
                }
                arg = OpenSSLImpl.to_der_if_possible(arg);
                RubyString str = arg.convertToString();

                Object val = null;
                KeyFactory fact = null;
                try {
                    fact = KeyFactory.getInstance("DSA");
                } catch (NoSuchAlgorithmException e) {
                    throw getRuntime().newLoadError("unsupported key algorithm (DSA)");
                }
                // TODO: ugly NoClassDefFoundError catching for no BC env. How can we remove this?
                if (null == val) {
                    // PEM_read_bio_DSAPrivateKey
                    try {
                        val = PEMInputOutput.readDSAPrivateKey(new StringReader(str.toString()), passwd);
                    } catch (NoClassDefFoundError e) {
                        val = null;
                    } catch (Exception e) {
                        val = null;
                    }
                }
                if (null == val) {
                    // PEM_read_bio_DSAPublicKey
                    try {
                        val = PEMInputOutput.readDSAPublicKey(new StringReader(str.toString()), passwd);
                    } catch (NoClassDefFoundError e) {
                        val = null;
                    } catch (Exception e) {
                        val = null;
                    }
                }
                if (null == val) {
                    // PEM_read_bio_DSA_PUBKEY
                    try {
                        val = PEMInputOutput.readDSAPubKey(new StringReader(str.toString()));
                    } catch (NoClassDefFoundError e) {
                        val = null;
                    } catch (Exception e) {
                        val = null;
                    }
                }
                if (null == val) {
                    // d2i_DSAPrivateKey_bio
                    try {
                        val = org.jruby.ext.openssl.impl.PKey.readDSAPrivateKey(str.getBytes());
                    } catch (NoClassDefFoundError e) {
                        val = null;
                    } catch (Exception e) {
                        val = null;
                    }
                }
                if (null == val) {
                    // d2i_DSA_PUBKEY_bio
                    try {
                        val = org.jruby.ext.openssl.impl.PKey.readDSAPublicKey(str.getBytes());
                    } catch (NoClassDefFoundError e) {
                        val = null;
                    } catch (Exception e) {
                        val = null;
                    }
                }
                if (null == val) {
                    try {
                        val = fact.generatePrivate(new PKCS8EncodedKeySpec(str.getBytes()));
                    } catch (Exception e) {
                        val = null;
                    }
                }
                if (null == val) {
                    try {
                        val = fact.generatePublic(new X509EncodedKeySpec(str.getBytes()));
                    } catch (Exception e) {
                        val = null;
                    }
                }
                if (null == val) {
                    throw newDSAError(getRuntime(), "Neither PUB key nor PRIV key:");
                }

                if (val instanceof KeyPair) {
                    PrivateKey privateKey = ((KeyPair) val).getPrivate();
                    PublicKey publicKey = ((KeyPair) val).getPublic();
                    if (privateKey instanceof DSAPrivateKey) {
                        privKey = (DSAPrivateKey) privateKey;
                        pubKey = (DSAPublicKey) publicKey;
                    } else {
                        throw newDSAError(getRuntime(), "Neither PUB key nor PRIV key:");
                    }
                } else if (val instanceof DSAPrivateKey) {
                    privKey = (DSAPrivateKey) val;
                } else if (val instanceof DSAPublicKey) {
                    pubKey = (DSAPublicKey) val;
                    privKey = null;
                } else {
                    throw newDSAError(getRuntime(), "Neither PUB key nor PRIV key:");
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

    @JRubyMethod
    public IRubyObject to_der() {
        try {
            byte[] bytes = org.jruby.ext.openssl.impl.PKey.toDerDSAKey(pubKey, privKey);
            return RubyString.newString(getRuntime(), bytes);
        } catch (NoClassDefFoundError ncdfe) {
            throw newDSAError(getRuntime(), OpenSSLReal.bcExceptionMessage(ncdfe));
        } catch (IOException ioe) {
            throw newDSAError(getRuntime(), ioe.getMessage());
        }
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
                PEMInputOutput.writeDSAPrivateKey(w, privKey, ciph, passwd);
            } else {
                PEMInputOutput.writeDSAPublicKey(w, pubKey);
            }
            w.close();
            return getRuntime().newString(w.toString());
        } catch (NoClassDefFoundError ncdfe) {
            throw newDSAError(getRuntime(), OpenSSLReal.bcExceptionMessage(ncdfe));
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
                this.pubKey = (DSAPublicKey)KeyFactory.getInstance("DSA").generatePublic(spec);
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
