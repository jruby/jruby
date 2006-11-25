/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.openssl;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.jce.netscape.NetscapeCertRequest;
import org.jruby.IRuby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jvyaml.util.Base64Coder;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class NetscapeSPKI extends RubyObject {
    public static void createNetscapeSPKI(IRuby runtime, RubyModule ossl) {
        RubyModule mNetscape = ossl.defineModuleUnder("Netscape");
        RubyClass cSPKI = mNetscape.defineClassUnder("SPKI",runtime.getObject());
        mNetscape.defineClassUnder("SPKIError",ossl.getClass("OpenSSLError"));

        CallbackFactory spkicb = runtime.callbackFactory(NetscapeSPKI.class);
        cSPKI.defineSingletonMethod("new",spkicb.getOptSingletonMethod("newInstance"));
        cSPKI.defineMethod("initialize",spkicb.getOptMethod("_initialize"));
        cSPKI.defineMethod("to_der",spkicb.getMethod("to_der"));
        cSPKI.defineMethod("to_pem",spkicb.getMethod("to_pem"));
        cSPKI.defineMethod("to_s",spkicb.getMethod("to_pem"));
        cSPKI.defineMethod("to_text",spkicb.getMethod("to_text"));
        cSPKI.defineMethod("public_key",spkicb.getMethod("public_key"));
        cSPKI.defineMethod("public_key=",spkicb.getMethod("set_public_key",IRubyObject.class));
        cSPKI.defineMethod("sign",spkicb.getMethod("sign",IRubyObject.class,IRubyObject.class));
        cSPKI.defineMethod("verify",spkicb.getMethod("verify",IRubyObject.class));
        cSPKI.defineMethod("challenge",spkicb.getMethod("challenge"));
        cSPKI.defineMethod("challenge=",spkicb.getMethod("set_challenge",IRubyObject.class));
    }

    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
        NetscapeSPKI result = new NetscapeSPKI(recv.getRuntime(), (RubyClass)recv);
        result.callInit(args);
        return result;
    }

    public NetscapeSPKI(IRuby runtime, RubyClass type) {
        super(runtime,type);
    }

    private IRubyObject public_key;
    private IRubyObject challenge;

    private NetscapeCertRequest cert;

    public IRubyObject _initialize(IRubyObject[] args) throws Exception {
        if(args.length > 0) {
            byte[] b = args[0].toString().getBytes("PLAIN");
            try {
                b = Base64Coder.decode(args[0].toString()).getBytes("PLAIN");
            } catch(Exception e) {
                b = args[0].toString().getBytes("PLAIN");
            }
            cert = new NetscapeCertRequest(b);
            this.challenge = getRuntime().newString(cert.getChallenge());
            String algo = cert.getPublicKey().getAlgorithm();;
            byte[] enc = cert.getPublicKey().getEncoded();
            if("RSA".equalsIgnoreCase(algo)) {
                this.public_key = ((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("PKey"))).getClass("RSA").callMethod(getRuntime().getCurrentContext(),"new",getRuntime().newString(new String(enc,"ISO8859_1")));
            } else if("DSA".equalsIgnoreCase(algo)) {
                this.public_key = ((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("PKey"))).getClass("DSA").callMethod(getRuntime().getCurrentContext(),"new",getRuntime().newString(new String(enc,"ISO8859_1")));
            } else {
                throw getRuntime().newLoadError("not implemented algo for public key: " + algo);
            }
        }
        return this;
    }

    public IRubyObject to_der() throws Exception {
        DERSequence b = (DERSequence)cert.toASN1Object();
        DERObjectIdentifier encType = null;
        DERBitString publicKey = new DERBitString(((PKey)public_key).to_der().toString().getBytes("PLAIN"));
        DERIA5String challenge = new DERIA5String(this.challenge.toString());
        DERObjectIdentifier sigAlg = null;
        DERBitString sig = null;
        encType = (DERObjectIdentifier)((DERSequence)((DERSequence)((DERSequence)b.getObjectAt(0)).getObjectAt(0)).getObjectAt(0)).getObjectAt(0);
        sigAlg = ((AlgorithmIdentifier)b.getObjectAt(1)).getObjectId();
        sig = (DERBitString)b.getObjectAt(2);

        ASN1EncodableVector v1 = new ASN1EncodableVector();
        ASN1EncodableVector v1_2 = new ASN1EncodableVector();
        ASN1EncodableVector v2 = new ASN1EncodableVector();
        ASN1EncodableVector v3 = new ASN1EncodableVector();
        ASN1EncodableVector v4 = new ASN1EncodableVector();
        v4.add(encType);
        v4.add(new DERNull());
        v3.add(new DERSequence(v4));
        v3.add(publicKey);
        v2.add(new DERSequence(v3));
        v2.add(challenge);
        v1.add(new DERSequence(v2));
        v1_2.add(sigAlg);
        v1_2.add(new DERNull());
        v1.add(new DERSequence(v1_2));
        v1.add(sig);
        return getRuntime().newString(new String(new DERSequence(v1).getEncoded(),"ISO8859_1"));
    }

    public IRubyObject to_pem() throws Exception {
        return getRuntime().newString(Base64Coder.encode(to_der().toString()));
    }

    public IRubyObject to_text() {
        System.err.println("WARNING: calling unimplemented method: to_text");
        return getRuntime().getNil();
    }

    public IRubyObject public_key() {
        return this.public_key;
    }

    public IRubyObject set_public_key(IRubyObject arg) {
        this.public_key = arg;
        return arg;
    }

    public IRubyObject sign(IRubyObject key, IRubyObject digest) throws Exception {
        String keyAlg = ((PKey)key).getAlgorithm();
        String digAlg = ((Digest)digest).getAlgorithm();
        DERObjectIdentifier alg = (DERObjectIdentifier)(ASN1.getOIDLookup(getRuntime()).get(keyAlg.toLowerCase() + "-" + digAlg.toLowerCase()));
        cert = new NetscapeCertRequest(challenge.toString(),new AlgorithmIdentifier(alg),((PKey)public_key).getPublicKey());
        cert.sign(((PKey)key).getPrivateKey());
        return this;
    }

    public IRubyObject verify(IRubyObject pkey) throws Exception {
        cert.setPublicKey(((PKey)pkey).getPublicKey());
        return cert.verify(challenge.toString()) ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public IRubyObject challenge() {
        return this.challenge;
    }

    public IRubyObject set_challenge(IRubyObject arg) {
        this.challenge = arg;
        return arg;
    }
}// NetscapeSPKI
