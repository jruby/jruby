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
import java.security.GeneralSecurityException;
import java.security.PublicKey;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.jce.netscape.NetscapeCertRequest;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.openssl.impl.Base64;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Visibility;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class NetscapeSPKI extends RubyObject {
    private static final long serialVersionUID = 3211242351810109432L;

    private static ObjectAllocator NETSCAPESPKI_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new NetscapeSPKI(runtime, klass);
        }
    };

    public static void createNetscapeSPKI(Ruby runtime, RubyModule ossl) {
        RubyModule mNetscape = ossl.defineModuleUnder("Netscape");
        RubyClass cSPKI = mNetscape.defineClassUnder("SPKI",runtime.getObject(),NETSCAPESPKI_ALLOCATOR);
        RubyClass openSSLError = ossl.getClass("OpenSSLError");
        mNetscape.defineClassUnder("SPKIError",openSSLError,openSSLError.getAllocator());

        cSPKI.defineAnnotatedMethods(NetscapeSPKI.class);
    }

    public NetscapeSPKI(Ruby runtime, RubyClass type) {
        super(runtime,type);
    }

    private IRubyObject public_key;
    private IRubyObject challenge;

    private NetscapeCertRequest cert;

    @JRubyMethod(name = "initialize", rest = true, visibility = Visibility.PRIVATE)
    public IRubyObject _initialize(IRubyObject[] args) {
        if (args.length > 0) {
            byte[] b = args[0].convertToString().getBytes();
            b = tryBase64Decode(b);
            final byte[] b2 = b;
            String algo = null;
            byte[] enc = null;
            try {
                // NetscapeCertRequest requires "BC" provider.
                PublicKey pkey = OpenSSLReal.getWithBCProvider(new OpenSSLReal.Callable<PublicKey>() {
                    public PublicKey call() throws GeneralSecurityException {
                        try {
                            // NetscapeCertRequest throws java.lang.IllegalArgumentException
                            // when no BC provider allowed, with a message
                            // "java.security.NoSuchProviderException: no such provider: BC"
                            // instead of NoSuchProviderException.
                            cert = new NetscapeCertRequest(b2);
                            challenge = getRuntime().newString(cert.getChallenge());
                            return cert.getPublicKey();
                        } catch (IOException ioe) {
                            throw new GeneralSecurityException(ioe.getMessage(), ioe);
                        }
                    }
                });
                algo = pkey.getAlgorithm();
                enc = pkey.getEncoded();
            }
            catch (GeneralSecurityException gse) {
                throw newSPKIError(getRuntime(), gse.getMessage());
            }

            if ("RSA".equalsIgnoreCase(algo)) {
                this.public_key = Utils.newRubyInstance(getRuntime(), "OpenSSL::PKey::RSA", RubyString.newString(getRuntime(), enc));
            } else if ("DSA".equalsIgnoreCase(algo)) {
                this.public_key = Utils.newRubyInstance(getRuntime(), "OpenSSL::PKey::DSA", RubyString.newString(getRuntime(), enc));
            } else {
                throw getRuntime().newLoadError("not implemented algo for public key: " + algo);
            }
        }
        return this;
    }

    // just try to decode for the time when the given bytes are base64 encoded.
    private byte[] tryBase64Decode(byte[] b) {
        try {
            b = Base64.decode(b, 0, b.length, Base64.NO_OPTIONS);
        } catch (Exception ignored) { }
        return b;
    }

    @JRubyMethod
    public IRubyObject to_der() {
        try {
            return RubyString.newString(getRuntime(), internalToDer());
        } catch (IOException ioe) {
            throw newSPKIError(getRuntime(), ioe.getMessage());
        }
    }

    @JRubyMethod(name={"to_pem","to_s"})
    public IRubyObject to_pem() {
        try {
            byte[] source = internalToDer();
            // no Base64.DO_BREAK_LINES option needed for NSPKI.
            return getRuntime().newString(Base64.encodeBytes(source, 0, source.length, Base64.NO_OPTIONS));
        } catch (IOException ioe) {
            throw newSPKIError(getRuntime(), ioe.getMessage());
        }
    }

    private byte[] internalToDer() throws IOException {
        ASN1Sequence b = (ASN1Sequence)cert.toASN1Primitive();
        ASN1ObjectIdentifier encType = null;
        DERBitString publicKey = new DERBitString(((PKey)public_key).to_der().convertToString().getBytes());
        DERIA5String encodedChallenge = new DERIA5String(this.challenge.toString());
        ASN1ObjectIdentifier sigAlg = null;
        DERBitString sig = null;
        encType = (ASN1ObjectIdentifier)((ASN1Sequence)((ASN1Sequence)((ASN1Sequence)b.getObjectAt(0)).getObjectAt(0)).getObjectAt(0)).getObjectAt(0);
        sigAlg = ((AlgorithmIdentifier)b.getObjectAt(1)).getAlgorithm();
        sig = (DERBitString)b.getObjectAt(2);

        ASN1EncodableVector v1 = new ASN1EncodableVector();
        ASN1EncodableVector v1_2 = new ASN1EncodableVector();
        ASN1EncodableVector v2 = new ASN1EncodableVector();
        ASN1EncodableVector v3 = new ASN1EncodableVector();
        ASN1EncodableVector v4 = new ASN1EncodableVector();
        v4.add(encType);
        v4.add(new DERNull());
        v3.add(new DLSequence(v4));
        v3.add(publicKey);
        v2.add(new DLSequence(v3));
        v2.add(encodedChallenge);
        v1.add(new DLSequence(v2));
        v1_2.add(sigAlg);
        v1_2.add(new DERNull());
        v1.add(new DLSequence(v1_2));
        v1.add(sig);
        return new DLSequence(v1).getEncoded();
    }

    @JRubyMethod
    public IRubyObject to_text() {
        System.err.println("WARNING: calling unimplemented method: to_text");
        return getRuntime().getNil();
    }

    @JRubyMethod
    public IRubyObject public_key() {
        return this.public_key;
    }

    @JRubyMethod(name="public_key=")
    public IRubyObject set_public_key(IRubyObject arg) {
        this.public_key = arg;
        return arg;
    }

    @JRubyMethod
    public IRubyObject sign(final IRubyObject key, final IRubyObject digest) {
        final String keyAlg = ((PKey) key).getAlgorithm();
        final String digAlg = ((Digest) digest).getShortAlgorithm();
        final String symKey = keyAlg.toLowerCase() + "-" + digAlg.toLowerCase();
        try { // NetscapeCertRequest requires "BC" provider.
            OpenSSLReal.getWithBCProvider(new OpenSSLReal.Callable<Void>() {
                public Void call() throws GeneralSecurityException {
                    final ASN1ObjectIdentifier alg = ASN1.getOIDLookup(getRuntime()).get( symKey );
                    final PublicKey publicKey = ((PKey) public_key).getPublicKey();
                    cert = new NetscapeCertRequest(challenge.toString(), new AlgorithmIdentifier(alg), publicKey);
                    cert.sign( ((PKey) key).getPrivateKey() ); return null;
                }
            });
        }
        catch (GeneralSecurityException gse) {
            throw newSPKIError(getRuntime(), gse.getMessage());
        }
        return this;
    }

    @JRubyMethod
    public IRubyObject verify(final IRubyObject pkey) {
        cert.setPublicKey(((PKey) pkey).getPublicKey());
        try { // NetscapeCertRequest requires "BC" provider.
            Boolean result = OpenSSLReal.getWithBCProvider(new OpenSSLReal.Callable<Boolean>() {
                public Boolean call() throws GeneralSecurityException {
                    return cert.verify(challenge.toString());
                }
            });
            return getRuntime().newBoolean(result.booleanValue());
        }
        catch (GeneralSecurityException gse) {
            throw newSPKIError(getRuntime(), gse.getMessage());
        }
    }

    @JRubyMethod
    public IRubyObject challenge() {
        return this.challenge;
    }

    @JRubyMethod(name="challenge=")
    public IRubyObject set_challenge(IRubyObject arg) {
        this.challenge = arg;
        return arg;
    }

    private static RaiseException newSPKIError(Ruby runtime, String message) {
        return Utils.newError(runtime, "OpenSSL::Netscape::SPKIError", message);
    }
}// NetscapeSPKI
