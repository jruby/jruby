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

import java.security.MessageDigest;
import java.util.Iterator;
import java.util.List;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERBoolean;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERString;
import org.bouncycastle.asn1.DERUnknownTag;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509Extensions {
    public static void createX509Ext(IRuby runtime, RubyModule mX509) {
        RubyClass cX509ExtFactory = mX509.defineClassUnder("ExtensionFactory",runtime.getObject());
        mX509.defineClassUnder("ExtensionError",runtime.getModule("OpenSSL").getClass("OpenSSLError"));
        
        CallbackFactory extfcb = runtime.callbackFactory(ExtensionFactory.class);
        cX509ExtFactory.defineSingletonMethod("new",extfcb.getOptSingletonMethod("newInstance"));
        cX509ExtFactory.defineMethod("initialize",extfcb.getOptMethod("initialize"));

        cX509ExtFactory.attr_reader(new IRubyObject[]{runtime.newString("issuer_certificate"),runtime.newString("subject_certificate"),
                                            runtime.newString("subject_request"),runtime.newString("crl"),
                                            runtime.newString("config")});
        cX509ExtFactory.defineMethod("issuer_certificate=",extfcb.getMethod("set_issuer_cert",IRubyObject.class));
        cX509ExtFactory.defineMethod("subject_certificate=",extfcb.getMethod("set_subject_cert",IRubyObject.class));
        cX509ExtFactory.defineMethod("subject_request=",extfcb.getMethod("set_subject_req",IRubyObject.class));
        cX509ExtFactory.defineMethod("crl=",extfcb.getMethod("set_crl",IRubyObject.class));
        cX509ExtFactory.defineMethod("config=",extfcb.getMethod("set_config",IRubyObject.class));
        cX509ExtFactory.defineMethod("create_ext",extfcb.getOptMethod("create_ext"));

        RubyClass cX509Ext = mX509.defineClassUnder("Extension",runtime.getObject());
        CallbackFactory extcb = runtime.callbackFactory(Extension.class);
        cX509Ext.defineSingletonMethod("new",extcb.getOptSingletonMethod("newInstance"));
        cX509Ext.defineMethod("initialize",extcb.getOptMethod("_initialize"));
        cX509Ext.defineMethod("oid=",extcb.getMethod("set_oid",IRubyObject.class));
        cX509Ext.defineMethod("value=",extcb.getMethod("set_value",IRubyObject.class));
        cX509Ext.defineMethod("critical=",extcb.getMethod("set_critical",IRubyObject.class));
        cX509Ext.defineMethod("oid",extcb.getMethod("oid"));
        cX509Ext.defineMethod("value",extcb.getMethod("value"));
        cX509Ext.defineMethod("critical?",extcb.getMethod("critical_p"));
        cX509Ext.defineMethod("to_der",extcb.getMethod("to_der"));
    }

    public static class ExtensionFactory extends RubyObject {
        public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
            ExtensionFactory result = new ExtensionFactory(recv.getRuntime(), (RubyClass)recv);
            result.callInit(args);
            return result;
        }

        public ExtensionFactory(IRuby runtime, RubyClass type) {
            super(runtime,type);
        }

        public IRubyObject initialize(IRubyObject[] args) {
            checkArgumentCount(args,0,4);
            if(args.length > 0 && !args[0].isNil()) {
                set_issuer_cert(args[0]);
            }
            if(args.length > 1 && !args[1].isNil()) {
                set_subject_cert(args[1]);
            }
            if(args.length > 2 && !args[2].isNil()) {
                set_subject_req(args[2]);
            }
            if(args.length > 3 && !args[3].isNil()) {
                set_crl(args[3]);
            }
            return this;
        }

        public IRubyObject set_issuer_cert(IRubyObject arg) {
            setInstanceVariable("@issuer_certificate",arg);
            return arg;
        }

        public IRubyObject set_subject_cert(IRubyObject arg) {
            setInstanceVariable("@subject_certificate",arg);
            return arg;
        }

        public IRubyObject set_subject_req(IRubyObject arg) {
            setInstanceVariable("@subject_request",arg);
            return arg;
        }

        public IRubyObject set_crl(IRubyObject arg) {
            setInstanceVariable("@crl",arg);
            return arg;
        }

        public IRubyObject set_config(IRubyObject arg) {
            setInstanceVariable("@config",arg);
            return arg;
        }

        private DERObjectIdentifier getObjectIdentifier(String nameOrOid) {
            Object val1 = ASN1.getOIDLookup(getRuntime()).get(nameOrOid.toLowerCase());
            if(null != val1) {
                return (DERObjectIdentifier)val1;
            }
            DERObjectIdentifier val2 = new DERObjectIdentifier(nameOrOid);
            return val2;
        }

        private static boolean isHexDigit(char c) {
            return ('0'<=c && c<='9') || ('A'<= c && c <= 'F') || ('a'<= c && c <= 'f');
        }

        public IRubyObject create_ext(IRubyObject[] args) throws Exception {
            IRubyObject critical = getRuntime().getFalse();
            if(checkArgumentCount(args,2,3) == 3 && !args[2].isNil()) {
                critical = args[2];
            }
            String oid = args[0].toString();
            String valuex = args[1].toString();
            Object value = valuex;

            DERObjectIdentifier r_oid = null;

            try {
                r_oid = getObjectIdentifier(oid);
            } catch(IllegalArgumentException e) {
                r_oid = null;
            }
            if(null == r_oid) {
                throw new RaiseException(getRuntime(), (RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getConstant("ExtensionError")), "unknown OID `" + oid + "'", true);
            }

            ThreadContext tc = getRuntime().getCurrentContext();
            Extension ext = (Extension)(((RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getConstant("Extension"))).callMethod(tc,"new"));

            if(valuex.startsWith("critical,")) {
                critical = getRuntime().getTrue();
                value = valuex.substring(9).trim();
            }

            if(r_oid.equals(new DERObjectIdentifier("2.5.29.14"))) { //subjectKeyIdentifier
                if("hash".equalsIgnoreCase(valuex)) {
                    IRubyObject pkey = getInstanceVariable("@subject_certificate").callMethod(tc,"public_key");
                    IRubyObject val = null;
                    if(pkey instanceof PKeyRSA) {
                        val = pkey.callMethod(tc,"to_der");
                    } else {
                        val = ASN1.decode(getRuntime().getModule("OpenSSL").getConstant("ASN1"),pkey.callMethod(tc,"to_der")).callMethod(tc,"value").callMethod(tc,"[]",getRuntime().newFixnum(1)).callMethod(tc,"value");
                    }
                    byte[] b = MessageDigest.getInstance("SHA-1").digest(val.toString().getBytes("PLAIN"));
                    value = new String(new DEROctetString(b).getDEREncoded(),"ISO8859_1");
                } else if(valuex.length() == 20) {
                    value = new String(new DEROctetString(valuex.getBytes("PLAIN")).getDEREncoded(),"ISO8859_1");
                } else {
                    StringBuffer nstr = new StringBuffer();
                    for(int i = 0; i < valuex.length(); i+=2) {
                        if(i+1 >= valuex.length()) {
                            throw new RaiseException(getRuntime(), (RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getConstant("ExtensionError")), oid + " = " + value + ": odd number of digits", true);
                        }

                        char c1 = valuex.charAt(i);
                        char c2 = valuex.charAt(i+1);
                        if(isHexDigit(c1) && isHexDigit(c2)) {
                            nstr.append(Character.toUpperCase(c1)).append(Character.toUpperCase(c2));
                        } else {
                            throw new RaiseException(getRuntime(), (RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getConstant("ExtensionError")), oid + " = " + value + ": illegal hex digit", true);
                        }
                        while((i+2) < valuex.length() && valuex.charAt(i+2) == ':') {
                            i++;
                        }
                    }
                    String v = nstr.toString();
                    byte[] arr = new byte[v.length()/2];
                    for(int i=0;i<v.length();i+=2) {
                        arr[i/2] = (byte)Integer.parseInt(v.substring(i,i+2),16);
                    }
                    value = new String(new DEROctetString(arr).getDEREncoded(),"ISO8859_1");
                }
            } else if(r_oid.equals(new DERObjectIdentifier("2.5.29.35"))) { //authorityKeyIdentifier
                String ourV = valuex;
                ASN1EncodableVector asnv = new ASN1EncodableVector();
               
                if(ourV.startsWith("keyid:always")) {
                    ourV = ourV.substring("keyid:always".length());
                    IRubyObject pkey = getInstanceVariable("@issuer_certificate").callMethod(tc,"public_key");
                    IRubyObject val = null;
                    if(pkey instanceof PKeyRSA) {
                        val = pkey.callMethod(tc,"to_der");
                    } else {
                        val = ASN1.decode(getRuntime().getModule("OpenSSL").getConstant("ASN1"),pkey.callMethod(tc,"to_der")).callMethod(tc,"value").callMethod(tc,"[]",getRuntime().newFixnum(1)).callMethod(tc,"value");
                    }
                    byte[] b = MessageDigest.getInstance("SHA-1").digest(val.toString().getBytes("PLAIN"));
                    asnv.add(new DEROctetString(b));
                } else if(ourV.startsWith("keyid")) {
                    ourV = ourV.substring("keyid".length());
                    IRubyObject pkey = getInstanceVariable("@issuer_certificate").callMethod(tc,"public_key");
                    IRubyObject val = null;
                    if(pkey instanceof PKeyRSA) {
                        val = pkey.callMethod(tc,"to_der");
                    } else {
                        val = ASN1.decode(getRuntime().getModule("OpenSSL").getConstant("ASN1"),pkey.callMethod(tc,"to_der")).callMethod(tc,"value").callMethod(tc,"[]",getRuntime().newFixnum(1)).callMethod(tc,"value");
                    }
                    byte[] b = MessageDigest.getInstance("SHA-1").digest(val.toString().getBytes("PLAIN"));
                    asnv.add(new DEROctetString(b));
                }
                value = new String(new DERSequence(asnv).getDEREncoded(),"ISO8859_1");
            } else if(r_oid.equals(new DERObjectIdentifier("2.5.29.18"))) { //issuerAltName
                if(valuex.startsWith("issuer:copy")) {
                    List exts = ((RubyArray)getInstanceVariable("@issuer_certificate").callMethod(tc,"extensions")).getList();
                    for(Iterator iter = exts.iterator();iter.hasNext();) {
                        Extension exta = (Extension)iter.next();
                        if(exta.getRealOid().equals(new DERObjectIdentifier("2.5.29.17"))) {
                            value = exta.getRealValue();
                            break;
                        }
                    }
                }
            } else if(r_oid.equals(new DERObjectIdentifier("2.5.29.19"))) { //basicConstraints
                String[] spl = valuex.split(",");
                for(int i=0;i<spl.length;i++) {
                    spl[i] = spl[i].trim();
                }
                ASN1EncodableVector asnv = new ASN1EncodableVector();
                for(int i=0;i<spl.length;i++) {
                    if(spl[i].length() > 3 && spl[i].substring(0,3).equalsIgnoreCase("CA:")) {
                        asnv.add(new DERBoolean("TRUE".equalsIgnoreCase(spl[i].substring(3).trim())));
                    }
                }
                for(int i=0;i<spl.length;i++) {
                    if(spl[i].length() > 8 && spl[i].substring(0,8).equalsIgnoreCase("pathlen:")) {
                        asnv.add(new DERInteger(Integer.parseInt(spl[i].substring(8).trim())));
                    }
                }
                value = new String(new DERSequence(asnv).getDEREncoded(),"ISO8859_1");
            } else if(r_oid.equals(new DERObjectIdentifier("2.5.29.15"))) { //keyUsage
                byte[] inp = null;
                inp = null;
                try {
                    String[] exx = valuex.split(":");
                    if(exx != null) {
                        inp = new byte[exx.length];
                        for(int i=0;i<exx.length;i++) {
                            inp[i] = (byte)Integer.parseInt(exx[i],16);
                        }
                    }
                } catch(Exception e) {
                    inp = null;
                }

                if(null == inp && valuex.length()<3) {
                    inp = valuex.getBytes("PLAIN");
                }

                if(inp == null) {
                    byte v1 = 0;
                    byte v2 = 0;
                    String[] spl = valuex.split(",");
                    for(int i=0;i<spl.length;i++) {
                        spl[i] = spl[i].trim();
                    }
                    for(int i=0;i<spl.length;i++) {
                        if("decipherOnly".equals(spl[i].trim()) || "Decipher Only".equals(spl[i].trim())) {
                            v2 |= (byte)128;
                        } else if("digitalSignature".equals(spl[i].trim()) || "Digital Signature".equals(spl[i].trim())) {
                            v1 |= (byte)128;
                        } else if("nonRepudiation".equals(spl[i].trim()) || "Non Repudiation".equals(spl[i].trim())) {
                            v1 |= (byte)64;
                        } else if("keyEncipherment".equals(spl[i].trim()) || "Key Encipherment".equals(spl[i].trim())) {
                            v1 |= (byte)32;
                        } else if("dataEncipherment".equals(spl[i].trim()) || "Data Encipherment".equals(spl[i].trim())) {
                            v1 |= (byte)16;
                        } else if("keyAgreement".equals(spl[i].trim()) || "Key Agreement".equals(spl[i].trim())) {
                            v1 |= (byte)8;
                        } else if("keyCertSign".equals(spl[i].trim()) || "Key Cert Sign".equals(spl[i].trim())) {
                            v1 |= (byte)4;
                        } else if("cRLSign".equals(spl[i].trim())) {
                            v1 |= (byte)2;
                        } else if("encipherOnly".equals(spl[i].trim()) || "Encipher Only".equals(spl[i].trim())) {
                            v1 |= (byte)1;
                        } else {
                            throw new RaiseException(getRuntime(), (RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getConstant("ExtensionError")), oid + " = " + valuex + ": unknown bit string argument", true);
                        }
                    }
                    if(v2 != 0) {
                        inp = new byte[]{v1,v2};
                    } else {
                        inp = new byte[]{v1};
                    }
                }

                int unused = 0;
                for(int i = (inp.length-1); i>-1; i--) {
                    if(inp[i] == 0) {
                        unused += 8;
                    } else {
                        byte a2 = inp[i];
                        int x = 8;
                        while(a2 != 0) {
                            a2 <<= 1;
                            x--;
                        }
                        unused += x;
                        break;
                    }
                }
                
                value = new String(new DERBitString(inp,unused).getDEREncoded(),"ISO8859_1");
            } else if(r_oid.equals(new DERObjectIdentifier("2.5.29.17"))) { //subjectAltName
                if(valuex.startsWith("DNS:")) {
                    value = new String(new GeneralNames(new GeneralName(GeneralName.dNSName,new DERIA5String(valuex.substring(4)))).getDEREncoded(),"ISO8859_1");
                } else if(valuex.startsWith("IP:")) {
                    String[] numbers = valuex.substring(3).split("\\.");
                    byte[] bs = new byte[4];
                    bs[0] = (byte) (Integer.parseInt(numbers[0]) & 0xff);
                    bs[1] = (byte) (Integer.parseInt(numbers[1]) & 0xff);
                    bs[2] = (byte) (Integer.parseInt(numbers[2]) & 0xff);
                    bs[3] = (byte) (Integer.parseInt(numbers[3]) & 0xff);
                    value = new String(new GeneralNames(new GeneralName(GeneralName.iPAddress,new DEROctetString(bs))).getDEREncoded(),"ISO8859_1");
                } else if(valuex.startsWith("IP Address:")) {
                    String[] numbers = valuex.substring(11).split("\\.");
                    byte[] bs = new byte[4];
                    bs[0] = (byte) (Integer.parseInt(numbers[0]) & 0xff);
                    bs[1] = (byte) (Integer.parseInt(numbers[1]) & 0xff);
                    bs[2] = (byte) (Integer.parseInt(numbers[2]) & 0xff);
                    bs[3] = (byte) (Integer.parseInt(numbers[3]) & 0xff);
                    value = new String(new GeneralNames(new GeneralName(GeneralName.iPAddress,new DEROctetString(bs))).getDEREncoded(),"ISO8859_1");
                }
            } else {
                value = new DEROctetString(new DEROctetString(valuex.getBytes("PLAIN")).getDEREncoded());
            }

            ext.setRealOid(r_oid);
            ext.setRealValue(value);
            ext.setRealCritical(critical.isTrue());

            return ext;
        }
    }

    public static class Extension extends RubyObject {
        public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
            Extension result = new Extension(recv.getRuntime(), (RubyClass)recv);
            result.callInit(args);
            return result;
        }

        public Extension(IRuby runtime, RubyClass type) {
            super(runtime,type);
        }

        private DERObjectIdentifier oid;
        private Object value;
        private boolean critical;

        void setRealOid(DERObjectIdentifier oid) {
            this.oid = oid;
        }

        void setRealValue(Object value) {
            this.value = value;
        }

        void setRealCritical(boolean critical) {
            this.critical = critical;
        }
        
        DERObjectIdentifier getRealOid() {
            return oid;
        }

        Object getRealValue() {
            return value;
        }

        byte[] getRealValueBytes() throws Exception {
            if((value instanceof RubyString) || (value instanceof String)) {
                return value.toString().getBytes("PLAIN");
            } else if(value instanceof DEROctetString) {
                return ((DEROctetString)value).getOctets();
            } else {
                return ((ASN1.ASN1Data)value).toASN1().getDEREncoded();
            }
        }

        boolean getRealCritical() {
            return critical;
        }

        DERObjectIdentifier getObjectIdentifier(String nameOrOid) {
            Object val1 = ASN1.getOIDLookup(getRuntime()).get(nameOrOid.toLowerCase());
            if(null != val1) {
                return (DERObjectIdentifier)val1;
            }
            DERObjectIdentifier val2 = new DERObjectIdentifier(nameOrOid);
            return val2;
        }

        public IRubyObject _initialize(IRubyObject[] args) throws Exception {
            byte[] octets = null;
            if(args.length == 1) {
                ASN1InputStream is = new ASN1InputStream(OpenSSLImpl.to_der_if_possible(args[0]).toString().getBytes("PLAIN"));
                Object obj = is.readObject();
                ASN1Sequence seq = (ASN1Sequence)obj;
                setRealOid((DERObjectIdentifier)(seq.getObjectAt(0)));
                setRealCritical(((DERBoolean)(seq.getObjectAt(1))).isTrue());
                octets = ((DEROctetString)(seq.getObjectAt(2))).getOctets();
            } else if(args.length > 1) {
                setRealOid(getObjectIdentifier(args[0].toString()));
                setRealValue(args[1]);
            }
            if(args.length > 2) {
                setRealCritical(args[2].isTrue());
            }
            if(args.length > 0 && octets != null) {
                setRealValue(new String(octets,"ISO8859_1"));
            }

            return this;
        }

        public IRubyObject set_oid(IRubyObject arg) {
            System.err.println("WARNING: calling ext#oid=");
            return getRuntime().getNil();
        }

        public IRubyObject set_value(IRubyObject arg) {
            System.err.println("WARNING: calling ext#value=");
            return getRuntime().getNil();
        }

        public IRubyObject set_critical(IRubyObject arg) {
            System.err.println("WARNING: calling ext#critical=");
            return getRuntime().getNil();
        }

        public IRubyObject oid() {
            return getRuntime().newString((String)(ASN1.getSymLookup(getRuntime()).get(oid)));
        }

        public IRubyObject value() throws Exception {
            if(getRealOid().equals(new DERObjectIdentifier("2.5.29.19"))) { //basicConstraints
                ASN1Sequence seq2 = (ASN1Sequence)(new ASN1InputStream(getRealValueBytes()).readObject());
                String c = "";
                String path = "";
                if(seq2.size()>0) {
                    c = "CA:" + (((DERBoolean)(seq2.getObjectAt(0))).isTrue() ? "TRUE" : "FALSE");
                }
                if(seq2.size()>1) {
                    path = ", pathlen:" + seq2.getObjectAt(1).toString();
                }
                return getRuntime().newString(c+path);
            } else if(getRealOid().equals(new DERObjectIdentifier("2.5.29.15"))) { //keyUsage
                byte[] bx = getRealValueBytes();
                byte[] bs = new byte[bx.length-2];
                System.arraycopy(bx,2,bs,0,bs.length);
                byte b1 = 0;
                byte b2 = bs[0];
                if(bs.length>1) {
                    b1 = bs[1];
                }
                StringBuffer sbe = new StringBuffer();
                String sep = "";
                if((b2 & (byte)128) != 0) {
                    sbe.append(sep).append("Decipher Only");
                    sep = ", ";
                }
                if((b1 & (byte)128) != 0) {
                    sbe.append(sep).append("Digital Signature");
                    sep = ", ";
                }
                if((b1 & (byte)64) != 0) {
                    sbe.append(sep).append("Non Repudiation");
                    sep = ", ";
                }
                if((b1 & (byte)32) != 0) {
                    sbe.append(sep).append("Key Encipherment");
                    sep = ", ";
                }
                if((b1 & (byte)16) != 0) {
                    sbe.append(sep).append("Data Encipherment");
                    sep = ", ";
                }
                if((b1 & (byte)8) != 0) {
                    sbe.append(sep).append("Key Agreement");
                    sep = ", ";
                }
                if((b1 & (byte)4) != 0) {
                    sbe.append(sep).append("Key Cert Sign");
                    sep = ", ";
                }
                if((b1 & (byte)2) != 0) {
                    sbe.append(sep).append("cRLSign");
                    sep = ", ";
                }
                if((b1 & (byte)1) != 0) {
                    sbe.append(sep).append("Encipher Only");
                }
                return getRuntime().newString(sbe.toString());
            } else if(getRealOid().equals(new DERObjectIdentifier("2.5.29.14"))) { //subjectKeyIdentifier
                byte[] b1 = getRealValueBytes();
                byte[] b2 = new byte[b1.length-2];
                System.arraycopy(b1,2,b2,0,b2.length);
                return getRuntime().newString(Utils.toHex(b2,':'));
            } else if(getRealOid().equals(new DERObjectIdentifier("2.5.29.35"))) { // authorityKeyIdentifier
                DERSequence seq = (DERSequence)(new ASN1InputStream(getRealValueBytes()).readObject());
                StringBuffer out1 = new StringBuffer();
                if(seq.size() > 0) {
                    out1.append("keyid:");
                    out1.append(Utils.toHex(((DEROctetString)seq.getObjectAt(0)).getOctets(),':'));
                }
                return getRuntime().newString(out1.toString());
            } else if(getRealOid().equals(new DERObjectIdentifier("2.5.29.21"))) { // CRLReason
                switch(RubyNumeric.fix2int(((IRubyObject)value).callMethod(getRuntime().getCurrentContext(),"value"))) {
                case 0:
                    return getRuntime().newString("Unspecified");
                case 1:
                    return getRuntime().newString("Key Compromise");
                case 2:
                    return getRuntime().newString("CA Compromise");
                case 3:
                    return getRuntime().newString("Affiliation Changed");
                case 4:
                    return getRuntime().newString("Superseded");
                case 5:
                    return getRuntime().newString("Cessation Of Operation");
                case 6:
                    return getRuntime().newString("Certificate Hold");
                case 8:
                    return getRuntime().newString("Remove From CRL");
                case 9:
                    return getRuntime().newString("Privilege Withdrawn");
                default:
                    return getRuntime().newString("Unspecified");
                }
            } else if(getRealOid().equals(new DERObjectIdentifier("2.5.29.17"))) { //subjectAltName
                try {
                    DERObject seq = new ASN1InputStream(getRealValueBytes()).readObject();
                    GeneralName[] n1 = null;
                    if(seq instanceof DERUnknownTag) {
                        n1 = new GeneralName[]{GeneralName.getInstance(seq)};
                    } else if(seq instanceof org.bouncycastle.asn1.DERTaggedObject) {
                        n1 = new GeneralName[]{GeneralName.getInstance(seq)};
                    } else {
                        n1 = GeneralNames.getInstance(seq).getNames();
                    }
                    StringBuffer sbe = new StringBuffer();
                    String sep = "";
                    for(int i=0;i<n1.length;i++) {
                        sbe.append(sep);
                        if(n1[i].getTagNo() == GeneralName.dNSName) {
                            sbe.append("DNS:");
                            sbe.append(((DERString)n1[i].getName()).getString());
                        } else if(n1[i].getTagNo() == GeneralName.iPAddress) {
                            sbe.append("IP Address:");
                            byte[] bs = ((DEROctetString)n1[i].getName()).getOctets();
                            String sep2 = "";
                            for(int j=0;j<bs.length;j++) {
                                sbe.append(sep2);
                                sbe.append(((int)bs[j]) & 0xff);
                                sep2 = ".";
                            }
                        } else {
                            sbe.append(n1[i].toString());
                        }
                        sep = ", ";
                    }
                    return getRuntime().newString(sbe.toString());
                } catch(Exception e) {
                    return getRuntime().newString(getRealValue().toString());
                }
            } else {
                try {
                    return ASN1.decode(getRuntime().getModule("OpenSSL").getConstant("ASN1"),getRuntime().newString(new String(getRealValueBytes(),"ISO8859_1"))).callMethod(getRuntime().getCurrentContext(),"value").callMethod(getRuntime().getCurrentContext(),"to_s");
                } catch(Exception e) {
                    return getRuntime().newString(getRealValue().toString());
                }
            }
        }

        public IRubyObject critical_p() {
            return critical ? getRuntime().getTrue() : getRuntime().getFalse();
        }

        public IRubyObject to_der() throws Exception {
            ASN1EncodableVector all = new ASN1EncodableVector();
            all.add(getRealOid());
            all.add(getRealCritical() ? DERBoolean.TRUE : DERBoolean.FALSE);
            all.add(new DEROctetString(getRealValueBytes()));
            return getRuntime().newString(new String(new DERSequence(all).getDEREncoded(),"ISO8859_1"));
        }
    }
}// X509Extensions
