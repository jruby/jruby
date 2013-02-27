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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.openssl.x509store.PEMInputOutput;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public abstract class PKey extends RubyObject {
    private static final long serialVersionUID = 6114668087816965720L;

    public static void createPKey(Ruby runtime, RubyModule ossl) {
        RubyModule mPKey = ossl.defineModuleUnder("PKey");
        mPKey.defineAnnotatedMethods(PKeyModule.class);
        // PKey is abstract
        RubyClass cPKey = mPKey.defineClassUnder("PKey",runtime.getObject(),ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        RubyClass openSSLError = ossl.getClass("OpenSSLError");
        mPKey.defineClassUnder("PKeyError",openSSLError,openSSLError.getAllocator());

        cPKey.defineAnnotatedMethods(PKey.class);

        PKeyRSA.createPKeyRSA(runtime,mPKey);
        PKeyDSA.createPKeyDSA(runtime,mPKey);
        PKeyDH.createPKeyDH(runtime, mPKey, cPKey);
    }

    public static RaiseException newPKeyError(Ruby runtime, String message) {
        return Utils.newError(runtime, "OpenSSL::PKey::PKeyError", message);
    }

    public static class PKeyModule {

        @JRubyMethod(name = "read", meta = true, required = 1, optional = 1)
        public static IRubyObject read(ThreadContext ctx, IRubyObject recv, IRubyObject[] args) {
            Ruby runtime = ctx.runtime;
            IRubyObject data;
            char[] pass;
            switch (args.length) {
            case 1:
                data = args[0];
                pass = null;
                break;
            default:
                data = args[0];
                pass = args[1].isNil() ? null : args[1].toString().toCharArray();
            }
            byte[] input = OpenSSLImpl.readX509PEM(data);
            KeyPair key = null;
            // d2i_PrivateKey_bio
            try {
                key = org.jruby.ext.openssl.impl.PKey.readPrivateKey(input);
            } catch (IOException ioe) {
                // ignore
            } catch (GeneralSecurityException gse) {
                // ignore
            }
            // PEM_read_bio_PrivateKey
            if (key == null) {
                try {
                    key = PEMInputOutput.readPrivateKey(new InputStreamReader(new ByteArrayInputStream(input)), pass);
                } catch (IOException ioe) {
                    // ignore
                }
            }
            if (key != null) {
                if (key.getPublic().getAlgorithm().equals("RSA")) {
                    return new PKeyRSA(runtime, Utils.getClassFromPath(runtime, "OpenSSL::PKey::RSA"), (RSAPrivateCrtKey) key.getPrivate(),
                            (RSAPublicKey) key.getPublic());
                } else if (key.getPublic().getAlgorithm().equals("DSA")) {
                    return new PKeyDSA(runtime, Utils.getClassFromPath(runtime, "OpenSSL::PKey::DSA"), (DSAPrivateKey) key.getPrivate(),
                            (DSAPublicKey) key.getPublic());
                }
            }

            PublicKey pubKey = null;
            // d2i_PUBKEY_bio
            try {
                pubKey = org.jruby.ext.openssl.impl.PKey.readPublicKey(input);
            } catch (IOException ioe) {
                // ignore
            } catch (GeneralSecurityException gse) {
                // ignore
            }
            // PEM_read_bio_PUBKEY
            if (pubKey == null) {
                try {
                    pubKey = PEMInputOutput.readPubKey(new InputStreamReader(new ByteArrayInputStream(input)));
                } catch (IOException ioe) {
                    // ignore
                }
            }

            if (pubKey != null) {
                if (pubKey.getAlgorithm().equals("RSA")) {
                    return new PKeyRSA(runtime, Utils.getClassFromPath(runtime, "OpenSSL::PKey::RSA"), (RSAPublicKey) pubKey);
                } else if (key.getPublic().getAlgorithm().equals("DSA")) {
                    return new PKeyDSA(runtime, Utils.getClassFromPath(runtime, "OpenSSL::PKey::DSA"), (DSAPublicKey) pubKey);
                }
            }

            throw runtime.newArgumentError("Could not parse PKey");
        }
    }

    public PKey(Ruby runtime, RubyClass type) {
        super(runtime,type);
    }

    @Override
    @JRubyMethod
    public IRubyObject initialize(ThreadContext context) {
        return this;
    }

    PublicKey getPublicKey() {
        return null;
    }

    PrivateKey getPrivateKey() {
        return null;
    }

    String getAlgorithm() {
        return "NONE";
    }

    // NetscapeSPKI uses it.
    public abstract IRubyObject to_der();

    @JRubyMethod(name = "sign")
    public IRubyObject sign(IRubyObject digest, IRubyObject data) {
        if (!this.callMethod(getRuntime().getCurrentContext(), "private?").isTrue()) {
            throw getRuntime().newArgumentError("Private key is needed.");
        }
        String digAlg = ((Digest) digest).getShortAlgorithm();
        try {
            Signature sig = Signature.getInstance(digAlg + "WITH" + getAlgorithm());
            sig.initSign(getPrivateKey());
            byte[] inp = data.convertToString().getBytes();
            sig.update(inp);
            byte[] sigge = sig.sign();
            return RubyString.newString(getRuntime(), sigge);
        } catch (GeneralSecurityException gse) {
            throw newPKeyError(getRuntime(), gse.getMessage());
        }
        /*
    GetPKey(self, pkey);
    EVP_SignInit(&ctx, GetDigestPtr(digest));
    StringValue(data);
    EVP_SignUpdate(&ctx, RSTRING(data)->ptr, RSTRING(data)->len);
    str = rb_str_new(0, EVP_PKEY_size(pkey)+16);
    if (!EVP_SignFinal(&ctx, RSTRING(str)->ptr, &buf_len, pkey))
    ossl_raise(ePKeyError, NULL);
    assert(buf_len <= RSTRING(str)->len);
    RSTRING(str)->len = buf_len;
    RSTRING(str)->ptr[buf_len] = 0;

    return str;
         */
    }

    @JRubyMethod(name = "verify")
    public IRubyObject verify(IRubyObject digest, IRubyObject sig, IRubyObject data) {
        if (!(digest instanceof Digest)) {
            throw newPKeyError(getRuntime(), "invalid digest");
        }
        if (!(sig instanceof RubyString)) {
            throw newPKeyError(getRuntime(), "invalid signature");
        }
        if (!(data instanceof RubyString)) {
            throw newPKeyError(getRuntime(), "invalid data");
        }
        byte[] sigBytes = ((RubyString)sig).getBytes();
        byte[] dataBytes = ((RubyString)data).getBytes();
        String algorithm = ((Digest)digest).getShortAlgorithm() + "WITH" + getAlgorithm();
        boolean valid;
        try {
            Signature signature = Signature.getInstance(algorithm);
            signature.initVerify(getPublicKey());
            signature.update(dataBytes);
            valid = signature.verify(sigBytes);
        } catch (NoSuchAlgorithmException e) {
            throw newPKeyError(getRuntime(), "unsupported algorithm: " + algorithm);
        } catch (SignatureException e) {
            throw newPKeyError(getRuntime(), "invalid signature");
        } catch (InvalidKeyException e) {
            throw newPKeyError(getRuntime(), "invalid key");
        }
        return getRuntime().newBoolean(valid);
    }

    protected static void addSplittedAndFormatted(StringBuilder result, BigInteger value) {
        String v = value.toString(16);
        if ((v.length() % 2) != 0) {
            v = "0" + v;
        }
        String sep = "";
        for (int i = 0; i < v.length(); i += 2) {
            result.append(sep);
            if ((i % 30) == 0) {
                result.append("\n    ");
            }
            result.append(v.substring(i, i + 2));
            sep = ":";
        }
        result.append("\n");
    }
}// PKey
