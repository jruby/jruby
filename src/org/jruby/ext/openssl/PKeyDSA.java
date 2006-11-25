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

import java.io.StringReader;
import java.io.StringWriter;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyPair;
import java.security.KeyFactory;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.jruby.IRuby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyObject;

import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERSequence;

import org.jruby.ext.openssl.x509store.PEM;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class PKeyDSA extends PKey {
    public static void createPKeyDSA(IRuby runtime, RubyModule mPKey) {
        RubyClass cDSA = mPKey.defineClassUnder("DSA",mPKey.getClass("PKey"));
        mPKey.defineClassUnder("DSAError",mPKey.getClass("PKeyError"));
        
        CallbackFactory dsacb = runtime.callbackFactory(PKeyDSA.class);

        cDSA.defineSingletonMethod("new",dsacb.getOptSingletonMethod("newInstance"));
        cDSA.defineMethod("initialize",dsacb.getOptMethod("initialize"));

        cDSA.defineMethod("public?",dsacb.getMethod("public_p"));
        cDSA.defineMethod("private?",dsacb.getMethod("private_p"));
        cDSA.defineMethod("to_der",dsacb.getMethod("to_der"));
        cDSA.defineMethod("to_text",dsacb.getMethod("to_text"));
        cDSA.defineMethod("public_key",dsacb.getMethod("public_key"));
        cDSA.defineMethod("export",dsacb.getOptMethod("export"));
        cDSA.defineMethod("to_pem",dsacb.getOptMethod("export"));
        cDSA.defineMethod("to_s",dsacb.getOptMethod("export"));
        cDSA.defineMethod("syssign",dsacb.getMethod("syssign",IRubyObject.class));
        cDSA.defineMethod("sysverify",dsacb.getMethod("sysverify",IRubyObject.class,IRubyObject.class));
    }

    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
        PKeyDSA result = new PKeyDSA(recv.getRuntime(), (RubyClass)recv);
        result.callInit(args);
        return result;
    }

    public PKeyDSA(IRuby runtime, RubyClass type) {
        super(runtime,type);
    }

    private DSAPrivateKey privKey;
    private DSAPublicKey pubKey;

    PublicKey getPublicKey() {
        return pubKey;
    }

    PrivateKey getPrivateKey() {
        return privKey;
    }

    String getAlgorithm() {
        return "DSA";
    }

    public IRubyObject initialize(IRubyObject[] args) {
        Object rsa;
        IRubyObject arg;
        IRubyObject pass = null;
        char[] passwd = null;
        if(checkArgumentCount(args,0,2) == 0) {
            rsa = null; //DSA.new
        } else {
            arg = args[0];
            if(args.length > 1) {
                pass = args[1];
            }
            if(arg instanceof RubyFixnum) {
            } else {
                if(pass != null && !pass.isNil()) {
                    passwd = pass.toString().toCharArray();
                }
                String input = arg.toString();

                Object val = null;
                KeyFactory fact = null;
                try {
                    fact = KeyFactory.getInstance("DSA");
                } catch(Exception e) {
                    throw getRuntime().newLoadError("unsupported key algorithm (DSA)");
                }
                if(null == val) {
                    try {
                        val = PEM.read_DSAPrivateKey(new StringReader(input),passwd);
                    } catch(Exception e3) {
                        val = null;
                    }
                }
                if(null == val) {
                    try {
                        val = PEM.read_DSAPublicKey(new StringReader(input),passwd);
                    } catch(Exception e3) {
                        val = null;
                    }
                }
                if(null == val) {
                    try {
                        val = PEM.read_DSA_PUBKEY(new StringReader(input),passwd);
                    } catch(Exception e3) {
                        val = null;
                    }
                }
                if(null == val) {
                    try {
                        val = fact.generatePrivate(new PKCS8EncodedKeySpec(input.getBytes("PLAIN")));
                    } catch(Exception e) {
                        val = null;
                    }
                }
                if(null == val) {
                    try {
                        val = fact.generatePublic(new X509EncodedKeySpec(input.getBytes("PLAIN")));
                    } catch(Exception e) {
                        val = null;
                    }
                }
                if(null == val) {
                    throw new RaiseException(getRuntime(), (RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("PKey"))).getConstant("DSAError")), "Neither PUB key nor PRIV key:", true);
                }

                if(val instanceof KeyPair) {
                    privKey = (DSAPrivateKey)(((KeyPair)val).getPrivate());
                    pubKey = (DSAPublicKey)(((KeyPair)val).getPublic());
                } else if(val instanceof DSAPrivateKey) {
                    privKey = (DSAPrivateKey)val;
                } else if(val instanceof DSAPublicKey) {
                    pubKey = (DSAPublicKey)val;
                    privKey = null;
                } else {
                    throw new RaiseException(getRuntime(), (RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("PKey"))).getConstant("DSAError")), "Neither PUB key nor PRIV key:", true);
                }
            }
        }

        return this;
    }

    public IRubyObject public_p() {
        return pubKey != null ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public IRubyObject private_p() {
        return privKey != null ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public IRubyObject to_der() throws Exception {
        if(pubKey != null && privKey == null) {
            return getRuntime().newString( new String(pubKey.getEncoded(),"ISO8859_1"));
        } else if(privKey != null && pubKey != null) {
            DSAParams params = privKey.getParams();
            ASN1EncodableVector v1 = new ASN1EncodableVector();
            v1.add(new DERInteger(0));
            v1.add(new DERInteger(params.getP()));
            v1.add(new DERInteger(params.getQ()));
            v1.add(new DERInteger(params.getG()));
            v1.add(new DERInteger(pubKey.getY()));
            v1.add(new DERInteger(privKey.getX()));
            return getRuntime().newString( new String(new DERSequence(v1).getEncoded(),"ISO8859_1"));
        } else {
            return getRuntime().newString( new String(privKey.getEncoded(),"ISO8859_1"));
        }
    }

    public IRubyObject to_text() throws Exception {
        return getRuntime().getNil();
    }

    public IRubyObject public_key() {
        PKeyDSA val = new PKeyDSA(getRuntime(),getMetaClass().getRealClass());
        val.privKey = null;
        val.pubKey = this.pubKey;
        return val;
    }

    public IRubyObject export(IRubyObject[] args) throws Exception {
        StringWriter w = new StringWriter();
        checkArgumentCount(args,0,2);
        char[] passwd = null;
        String algo = null;
        if(args.length > 0 && !args[0].isNil()) {
            algo = ((Cipher)args[0]).getAlgorithm();
            if(args.length > 1 && !args[1].isNil()) {
                passwd = args[1].toString().toCharArray();
            }
        }
        if(privKey != null) {
            PEM.write_DSAPrivateKey(w,privKey,algo,passwd);
        } else {
            PEM.write_DSAPublicKey(w,pubKey);
        }
        w.close();
        return getRuntime().newString(w.toString());
    }

    private String getPadding(int padding) {
        if(padding < 1 || padding > 4) {
            throw new RaiseException(getRuntime(), (RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("PKey"))).getConstant("DSAError")), null, true);
        }

        String p = "/NONE/PKCS1Padding";
        if(padding == 3) {
            p = "/NONE/NoPadding";
        } else if(padding == 4) {
            p = "/NONE/OAEPWithMD5AndMGF1Padding";
        } else if(padding == 2) {
            p = "/NONE/ISO9796-1Padding";
        }
        return p;
    }        

    public IRubyObject syssign(IRubyObject arg) {
        return getRuntime().getNil();
    }

    public IRubyObject sysverify(IRubyObject arg, IRubyObject arg2) {
        return getRuntime().getNil();
    }
}// PKeyDSA
