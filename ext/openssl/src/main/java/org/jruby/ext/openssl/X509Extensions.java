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

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERBoolean;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.openssl.impl.ASN1Registry;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Visibility;
import org.jruby.util.ByteList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509Extensions {
    public static void createX509Ext(Ruby runtime, RubyModule mX509) {
        RubyClass cX509ExtFactory = mX509.defineClassUnder("ExtensionFactory",runtime.getObject(),ExtensionFactory.ALLOCATOR);
        RubyClass openSSLError = runtime.getModule("OpenSSL").getClass("OpenSSLError");
        mX509.defineClassUnder("ExtensionError",openSSLError,openSSLError.getAllocator());

        cX509ExtFactory.attr_reader(runtime.getCurrentContext(), new IRubyObject[]{runtime.newString("issuer_certificate"),runtime.newString("subject_certificate"),
                                                                                   runtime.newString("subject_request"),runtime.newString("crl"),
                                                                                   runtime.newString("config")});

        cX509ExtFactory.defineAnnotatedMethods(ExtensionFactory.class);

        RubyClass cX509Ext = mX509.defineClassUnder("Extension",runtime.getObject(),Extension.ALLOCATOR);
        cX509Ext.defineAnnotatedMethods(Extension.class);
    }

    public static class ExtensionFactory extends RubyObject {
        private static final long serialVersionUID = 3180447029639456500L;

        public static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
                public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                    return new ExtensionFactory(runtime, klass);
                }
            };

        public ExtensionFactory(Ruby runtime, RubyClass type) {
            super(runtime,type);
        }

        @JRubyMethod(rest=true, visibility = Visibility.PRIVATE)
        public IRubyObject initialize(IRubyObject[] args, Block unusedBlock) {
            org.jruby.runtime.Arity.checkArgumentCount(getRuntime(),args,0,4);
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

        @JRubyMethod(name="issuer_certificate=")
        public IRubyObject set_issuer_cert(IRubyObject arg) {
            setInstanceVariable("@issuer_certificate",arg);
            return arg;
        }

        @JRubyMethod(name="subject_certificate=")
        public IRubyObject set_subject_cert(IRubyObject arg) {
            setInstanceVariable("@subject_certificate",arg);
            return arg;
        }

        @JRubyMethod(name="subject_request=")
        public IRubyObject set_subject_req(IRubyObject arg) {
            setInstanceVariable("@subject_request",arg);
            return arg;
        }

        @JRubyMethod(name="crl=")
        public IRubyObject set_crl(IRubyObject arg) {
            setInstanceVariable("@crl",arg);
            return arg;
        }

        @JRubyMethod(name="config=")
        public IRubyObject set_config(IRubyObject arg) {
            setInstanceVariable("@config",arg);
            return arg;
        }

        private ASN1ObjectIdentifier getObjectIdentifier(String nameOrOid) {
            Object val1 = ASN1.getOIDLookup(getRuntime()).get(nameOrOid.toLowerCase());
            if(null != val1) {
                return (ASN1ObjectIdentifier)val1;
            }
            ASN1ObjectIdentifier val2 = new ASN1ObjectIdentifier(nameOrOid);
            return val2;
        }

        private static boolean isHexDigit(char c) {
            return ('0'<=c && c<='9') || ('A'<= c && c <= 'F') || ('a'<= c && c <= 'f');
        }

        private boolean isHexString(String str ){
        	for(int i = 0; i< str.length(); i++) {
        		if (!isHexDigit(str.charAt(i))) return false;
        	}
        	return true;
        }

        @JRubyMethod(rest=true)
        public IRubyObject create_ext(IRubyObject[] args) {
            IRubyObject critical = getRuntime().getFalse();
            if(org.jruby.runtime.Arity.checkArgumentCount(getRuntime(),args,2,3) == 3 && !args[2].isNil()) {
                critical = args[2];
            }
            String oid = args[0].toString();
            String valuex = args[1].toString();
            Object value = valuex;

            ASN1ObjectIdentifier r_oid = null;

            try {
                r_oid = getObjectIdentifier(oid);
            } catch(IllegalArgumentException e) {
                 throw newX509ExtError(getRuntime(), "unknown OID `" + oid + "'");
            }

            Extension ext = (Extension) Utils.newRubyInstance(getRuntime(), "OpenSSL::X509::Extension");

            if(valuex.startsWith("critical,")) {
                critical = getRuntime().getTrue();
                valuex = valuex.substring(9).trim();
            }

            try {
                if(r_oid.equals(new ASN1ObjectIdentifier("2.5.29.14"))) { //subjectKeyIdentifier
                    DEROctetString inp = parseSubjectKeyIdentifier(oid, valuex);
                    value = new String(ByteList.plain(inp.getEncoded(ASN1Encoding.DER)));
                } else if(r_oid.equals(new ASN1ObjectIdentifier("2.5.29.35"))) { //authorityKeyIdentifier
                    DLSequence inp = parseAuthorityKeyIdentifier(valuex);
                    value = new String(ByteList.plain(inp.getEncoded(ASN1Encoding.DER)));
                } else if(r_oid.equals(new ASN1ObjectIdentifier("2.5.29.18"))) { //issuerAltName
                    value = parseIssuerAltName(valuex);
                } else if(r_oid.equals(new ASN1ObjectIdentifier("2.5.29.19"))) { //basicConstraints
                    DLSequence inp = parseBasicConstrains(valuex);
                    value = new String(ByteList.plain(inp.getEncoded(ASN1Encoding.DER)));
                } else if(r_oid.equals(new ASN1ObjectIdentifier("2.5.29.15"))) { //keyUsage
                    DERBitString inp = parseKeyUsage(oid, valuex);
                    value = new String(ByteList.plain(inp.getEncoded(ASN1Encoding.DER)));
                } else if(r_oid.equals(new ASN1ObjectIdentifier("2.16.840.1.113730.1.1"))) { //nsCertType
                    value = parseNsCertType(oid, valuex);
                } else if(r_oid.equals(new ASN1ObjectIdentifier("2.5.29.17"))) { //subjectAltName
                    value = parseSubjectAltName(valuex);
                } else if(r_oid.equals(new ASN1ObjectIdentifier("2.5.29.37"))) { //extendedKeyUsage
                    value = parseExtendedKeyUsage(valuex);
                } else {
                    value = new DEROctetString(new DEROctetString(ByteList.plain(valuex)).getEncoded(ASN1Encoding.DER));
                }
            } catch (IOException ex) {
                throw newX509ExtError(getRuntime(), "Unable to create extension: " + ex.getMessage());
            }

            ext.setRealOid(r_oid);
            ext.setRealValue(value);
            ext.setRealCritical(critical.isTrue());

            return ext;
        }

        private DERBitString parseKeyUsage(String oid, String valuex) {
            byte[] inp = null;
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
                inp = ByteList.plain(valuex);
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
                        throw newX509ExtError(getRuntime(), oid + " = " + valuex + ": unknown bit string argument");
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
            return new DERBitString(inp,unused);
        }

        private DERBitString parseNsCertType(String oid, String valuex) {
            byte v = 0;
            if (valuex.length() < 3) {
                byte[] inp = ByteList.plain(valuex);
                v = inp[0];
            } else {
                String[] spl = valuex.split(",");
                for (int i = 0; i < spl.length; i++) {
                    spl[i] = spl[i].trim();
                }
                for (int i = 0; i < spl.length; i++) {
                    if ("SSL Client".equals(spl[i]) || "client".equals(spl[i])) {
                        v |= (byte) 128;
                    } else if ("SSL Server".equals(spl[i]) || "server".equals(spl[i])) {
                        v |= (byte) 64;
                    } else if ("S/MIME".equals(spl[i]) || "email".equals(spl[i])) {
                        v |= (byte) 32;
                    } else if ("Object Signing".equals(spl[i]) || "objsign".equals(spl[i])) {
                        v |= (byte) 16;
                    } else if ("Unused".equals(spl[i]) || "reserved".equals(spl[i])) {
                        v |= (byte) 8;
                    } else if ("SSL CA".equals(spl[i]) || "sslCA".equals(spl[i])) {
                        v |= (byte) 4;
                    } else if ("S/MIME CA".equals(spl[i]) || "emailCA".equals(spl[i])) {
                        v |= (byte) 2;
                    } else if ("Object Signing CA".equals(spl[i]) || "objCA".equals(spl[i])) {
                        v |= (byte) 1;
                    } else {
                        throw newX509ExtError(getRuntime(), oid + " = " + valuex + ": unknown bit string argument");
                    }
                }
            }
            int unused = 0;
            if (v == 0) {
                unused += 8;
            } else {
                byte a2 = v;
                int x = 8;
                while (a2 != 0) {
                    a2 <<= 1;
                    x--;
                }
                unused += x;
            }
            return new DERBitString(new byte[] { v }, unused);
        }

        private DLSequence parseBasicConstrains(String valuex) {
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
                    asnv.add(new ASN1Integer(Integer.parseInt(spl[i].substring(8).trim())));
                }
            }
            return new DLSequence(asnv);
        }

        private DLSequence parseAuthorityKeyIdentifier(String valuex) {
            ThreadContext tc = getRuntime().getCurrentContext();
            ASN1EncodableVector asnv = new ASN1EncodableVector();
            if(valuex.startsWith("keyid:always")) {
                IRubyObject pkey = getInstanceVariable("@issuer_certificate").callMethod(tc,"public_key");
                IRubyObject val;
                if(pkey instanceof PKeyRSA) {
                    val = pkey.callMethod(tc,"to_der");
                } else {
                    val = ASN1.decode(getRuntime().getClassFromPath("OpenSSL::ASN1"), pkey.callMethod(tc, "to_der")).callMethod(tc, "value")
                            .callMethod(tc, "[]", getRuntime().newFixnum(1)).callMethod(tc, "value");
                }
                byte[] b = getSHA1Digest(getRuntime(), val.convertToString().getBytes());
                asnv.add(new DEROctetString(b));
            } else if(valuex.startsWith("keyid")) {
                IRubyObject pkey = getInstanceVariable("@issuer_certificate").callMethod(tc,"public_key");
                IRubyObject val;
                if(pkey instanceof PKeyRSA) {
                    val = pkey.callMethod(tc,"to_der");
                } else {
                    val = ASN1.decode(getRuntime().getClassFromPath("OpenSSL::ASN1"), pkey.callMethod(tc, "to_der")).callMethod(tc, "value")
                            .callMethod(tc, "[]", getRuntime().newFixnum(1)).callMethod(tc, "value");
                }
                byte[] b = getSHA1Digest(getRuntime(), val.convertToString().getBytes());
                asnv.add(new DEROctetString(b));
            }
            return new DLSequence(asnv);
        }

        private Object parseIssuerAltName(String valuex) throws IOException {
            ThreadContext tc = getRuntime().getCurrentContext();
            if(valuex.startsWith("issuer:copy")) {
                RubyArray exts = (RubyArray)getInstanceVariable("@issuer_certificate").callMethod(tc,"extensions");
                for (IRubyObject obj : exts.toJavaArray()) {
                    Extension exta = (Extension)obj;
                    if(exta.getRealOid().equals(new ASN1ObjectIdentifier("2.5.29.17"))) {
                        return exta.getRealValue();
                    }
                }
            }
            throw new IOException("Malformed IssuerAltName: " + valuex);
        }

        private String parseSubjectAltName(String valuex) throws IOException {
            if(valuex.startsWith("DNS:")) {
                GeneralNames gn = new GeneralNames(new GeneralName(GeneralName.dNSName,new DERIA5String(valuex.substring(4))));
                return new String(ByteList.plain(gn.getEncoded(ASN1Encoding.DER)));
            } else if(valuex.startsWith("IP:")) {
                String[] numbers = valuex.substring(3).split("\\.");
                byte[] bs = new byte[4];
                bs[0] = (byte) (Integer.parseInt(numbers[0]) & 0xff);
                bs[1] = (byte) (Integer.parseInt(numbers[1]) & 0xff);
                bs[2] = (byte) (Integer.parseInt(numbers[2]) & 0xff);
                bs[3] = (byte) (Integer.parseInt(numbers[3]) & 0xff);
                GeneralNames gn = new GeneralNames(new GeneralName(GeneralName.iPAddress,new DEROctetString(bs)));
                return new String(ByteList.plain(gn.getEncoded(ASN1Encoding.DER)));
            } else if(valuex.startsWith("IP Address:")) {
                String[] numbers = valuex.substring(11).split("\\.");
                byte[] bs = new byte[4];
                bs[0] = (byte) (Integer.parseInt(numbers[0]) & 0xff);
                bs[1] = (byte) (Integer.parseInt(numbers[1]) & 0xff);
                bs[2] = (byte) (Integer.parseInt(numbers[2]) & 0xff);
                bs[3] = (byte) (Integer.parseInt(numbers[3]) & 0xff);
                GeneralNames gn = new GeneralNames(new GeneralName(GeneralName.iPAddress,new DEROctetString(bs)));
                return new String(ByteList.plain(gn.getEncoded(ASN1Encoding.DER)));
            } else {
                return valuex;
            }
        }

        private DEROctetString parseSubjectKeyIdentifier(String oid, String valuex) {
            ThreadContext tc = getRuntime().getCurrentContext();
            if("hash".equalsIgnoreCase(valuex)) {
                IRubyObject pkey = getInstanceVariable("@subject_certificate").callMethod(tc,"public_key");
                IRubyObject val;
                if(pkey instanceof PKeyRSA) {
                    val = pkey.callMethod(tc,"to_der");
                } else {
                    val = ASN1.decode(getRuntime().getClassFromPath("OpenSSL::ASN1"), pkey.callMethod(tc, "to_der")).callMethod(tc, "value")
                            .callMethod(tc, "[]", getRuntime().newFixnum(1)).callMethod(tc, "value");
                }
                byte[] b = getSHA1Digest(getRuntime(), val.convertToString().getBytes());
                return new DEROctetString(b);
            } else if(valuex.length() == 20 || !isHexString(valuex)) {
                return new DEROctetString(ByteList.plain(valuex));
            } else {
                StringBuilder nstr = new StringBuilder();
                for(int i = 0; i < valuex.length(); i+=2) {
                    if(i+1 >= valuex.length()) {
                        throw newX509ExtError(getRuntime(), oid + " = " + valuex + ": odd number of digits");
                    }

                    char c1 = valuex.charAt(i);
                    char c2 = valuex.charAt(i+1);
                    if(isHexDigit(c1) && isHexDigit(c2)) {
                        nstr.append(Character.toUpperCase(c1)).append(Character.toUpperCase(c2));
                    } else {
                        throw newX509ExtError(getRuntime(), oid + " = " + valuex + ": illegal hex digit");
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
                return new DEROctetString(arr);
            }
        }

        private DLSequence parseExtendedKeyUsage(String valuex) {
            String[] spl = valuex.split(", ?");
            ASN1EncodableVector vector = new ASN1EncodableVector();
            for(String name : spl) {
                vector.add(ASN1Registry.sym2oid(name));
            }
            return new DLSequence(vector);
        }
    }

    private static byte[] getSHA1Digest(Ruby runtime, byte[] bytes) {
        try {
            return SecurityHelper.getMessageDigest("SHA-1").digest(bytes);
        }
        catch (GeneralSecurityException ex) {
            throw newX509ExtError(runtime, ex.getMessage());
        }
    }

    public static class Extension extends RubyObject {
        private static final long serialVersionUID = -1160318458085651926L;

        public static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
                public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                    return new Extension(runtime, klass);
                }
            };

        public Extension(Ruby runtime, RubyClass type) {
            super(runtime,type);
        }

        private ASN1ObjectIdentifier oid;
        private Object value;
        private boolean critical;

        void setRealOid(ASN1ObjectIdentifier oid) {
            this.oid = oid;
        }

        void setRealValue(Object value) {
            this.value = value;
        }

        void setRealCritical(boolean critical) {
            this.critical = critical;
        }

        ASN1ObjectIdentifier getRealOid() {
            return oid;
        }

        Object getRealValue() {
            return value;
        }

        byte[] getRealValueBytes() throws IOException {
            if((value instanceof RubyString)) {
                return ((RubyString) value).convertToString().getBytes();
            } else if(value instanceof String) {
                return ByteList.plain((String) value);
            } else if(value instanceof DEROctetString) {
                return ((DEROctetString)value).getOctets();
            } else if(value instanceof ASN1Encodable) {
                return ((ASN1Encodable)value).toASN1Primitive().getEncoded(ASN1Encoding.DER);
            } else {
                return ((ASN1.ASN1Data)value).toASN1().toASN1Primitive().getEncoded(ASN1Encoding.DER);
            }
        }

        boolean getRealCritical() {
            return critical;
        }

        ASN1ObjectIdentifier getObjectIdentifier(String nameOrOid) {
            Object val1 = ASN1.getOIDLookup(getRuntime()).get(nameOrOid.toLowerCase());
            if(null != val1) {
                return (ASN1ObjectIdentifier)val1;
            }
            ASN1ObjectIdentifier val2 = new ASN1ObjectIdentifier(nameOrOid);
            return val2;
        }

        @JRubyMethod(name = "initialize", rest = true, visibility = Visibility.PRIVATE)
        public IRubyObject _initialize(IRubyObject[] args) {
            byte[] octets = null;
            if (args.length == 1) {
                try {
                    ASN1InputStream is = new ASN1InputStream(OpenSSLImpl.to_der_if_possible(args[0]).convertToString().getBytes());
                    Object obj = is.readObject();
                    ASN1Sequence seq = (ASN1Sequence) obj;
                    setRealOid((ASN1ObjectIdentifier) (seq.getObjectAt(0)));
                    setRealCritical(((DERBoolean) (seq.getObjectAt(1))).isTrue());
                    octets = ((DEROctetString) (seq.getObjectAt(2))).getOctets();
                } catch (IOException ioe) {
                    throw newX509ExtError(getRuntime(), ioe.getMessage());
                }
            } else if (args.length > 1) {
                setRealOid(getObjectIdentifier(args[0].toString()));
                setRealValue(args[1]);
            }
            if (args.length > 2) {
                setRealCritical(args[2].isTrue());
            }
            if (args.length > 0 && octets != null) {
                setRealValue(new String(ByteList.plain(octets)));
            }
            return this;
        }

        @JRubyMethod(name="oid=")
        public IRubyObject set_oid(IRubyObject arg) {
            System.err.println("WARNING: calling ext#oid=");
            return getRuntime().getNil();
        }

        @JRubyMethod(name="value=")
        public IRubyObject set_value(IRubyObject arg) {
            System.err.println("WARNING: calling ext#value=");
            return getRuntime().getNil();
        }

        @JRubyMethod(name="critical=")
        public IRubyObject set_critical(IRubyObject arg) {
            System.err.println("WARNING: calling ext#critical=");
            return getRuntime().getNil();
        }

        @JRubyMethod
        public IRubyObject oid() {
            Object val = ASN1.getSymLookup(getRuntime()).get(oid);
            if(null == val) {
                val = oid.toString();
            }
            return getRuntime().newString((String)(val));
        }

        @JRubyMethod
        public IRubyObject value() {
            Ruby runtime = getRuntime();
            try {
                if (getRealOid().equals(new ASN1ObjectIdentifier("2.5.29.19"))) { //basicConstraints
                    ASN1Sequence seq2 = (ASN1Sequence) (new ASN1InputStream(getRealValueBytes()).readObject());
                    String c = "";
                    String path = "";
                    if (seq2.size() > 0) {
                        c = "CA:" + (((DERBoolean) (seq2.getObjectAt(0))).isTrue() ? "TRUE" : "FALSE");
                    }
                    if (seq2.size() > 1) {
                        path = ", pathlen:" + seq2.getObjectAt(1).toString();
                    }
                    return runtime.newString(c + path);
                } else if (getRealOid().equals(new ASN1ObjectIdentifier("2.5.29.15"))) { //keyUsage
                    byte[] bx = getRealValueBytes();
                    byte[] bs = new byte[bx.length - 2];
                    System.arraycopy(bx, 2, bs, 0, bs.length);
                    byte b1 = 0;
                    byte b2 = bs[0];
                    if (bs.length > 1) {
                        b1 = bs[1];
                    }
                    StringBuilder sbe = new StringBuilder();
                    String sep = "";
                    if ((b2 & (byte) 128) != 0) {
                        sbe.append(sep).append("Decipher Only");
                        sep = ", ";
                    }
                    if ((b1 & (byte) 128) != 0) {
                        sbe.append(sep).append("Digital Signature");
                        sep = ", ";
                    }
                    if ((b1 & (byte) 64) != 0) {
                        sbe.append(sep).append("Non Repudiation");
                        sep = ", ";
                    }
                    if ((b1 & (byte) 32) != 0) {
                        sbe.append(sep).append("Key Encipherment");
                        sep = ", ";
                    }
                    if ((b1 & (byte) 16) != 0) {
                        sbe.append(sep).append("Data Encipherment");
                        sep = ", ";
                    }
                    if ((b1 & (byte) 8) != 0) {
                        sbe.append(sep).append("Key Agreement");
                        sep = ", ";
                    }
                    if ((b1 & (byte) 4) != 0) {
                        sbe.append(sep).append("Certificate Sign");
                        sep = ", ";
                    }
                    if ((b1 & (byte) 2) != 0) {
                        sbe.append(sep).append("CRL Sign");
                        sep = ", ";
                    }
                    if ((b1 & (byte) 1) != 0) {
                        sbe.append(sep).append("Encipher Only");
                    }
                    return runtime.newString(sbe.toString());
                } else if (getRealOid().equals(new ASN1ObjectIdentifier("2.16.840.1.113730.1.1"))) { //nsCertType
                    byte[] bx = getRealValueBytes();
                    byte b = bx[0];
                    StringBuilder sbe = new StringBuilder();
                    String sep = "";
                    if ((b & (byte) 128) != 0) {
                        sbe.append(sep).append("SSL Client");
                        sep = ", ";
                    }
                    if ((b & (byte) 64) != 0) {
                        sbe.append(sep).append("SSL Servern");
                        sep = ", ";
                    }
                    if ((b & (byte) 32) != 0) {
                        sbe.append(sep).append("S/MIME");
                        sep = ", ";
                    }
                    if ((b & (byte) 16) != 0) {
                        sbe.append(sep).append("Object Signing");
                        sep = ", ";
                    }
                    if ((b & (byte) 8) != 0) {
                        sbe.append(sep).append("Unused");
                        sep = ", ";
                    }
                    if ((b & (byte) 4) != 0) {
                        sbe.append(sep).append("SSL CA");
                        sep = ", ";
                    }
                    if ((b & (byte) 2) != 0) {
                        sbe.append(sep).append("S/MIME CA");
                        sep = ", ";
                    }
                    if ((b & (byte) 1) != 0) {
                        sbe.append(sep).append("Object Signing CA");
                    }
                    return runtime.newString(sbe.toString());
                } else if (getRealOid().equals(new ASN1ObjectIdentifier("2.5.29.14"))) { //subjectKeyIdentifier
                    byte[] b1 = getRealValueBytes();
                    byte[] b2 = new byte[b1.length - 2];
                    System.arraycopy(b1, 2, b2, 0, b2.length);
                    return runtime.newString(Utils.toHex(b2, ':'));
                } else if (getRealOid().equals(new ASN1ObjectIdentifier("2.5.29.35"))) { // authorityKeyIdentifier
                    ASN1Sequence seq = (ASN1Sequence) (new ASN1InputStream(getRealValueBytes()).readObject());
                    StringBuilder out1 = new StringBuilder();
                    if (seq.size() > 0) {
                        out1.append("keyid:");
                        ASN1Primitive keyid = seq.getObjectAt(0).toASN1Primitive();
                        if (keyid instanceof DEROctetString) {
                            out1.append(Utils.toHex(((DEROctetString) keyid).getOctets(), ':'));
                        } else {
                            out1.append(Utils.toHex(keyid.getEncoded(ASN1Encoding.DER), ':'));
                        }
                    }
                    return runtime.newString(out1.toString());
                } else if (getRealOid().equals(new ASN1ObjectIdentifier("2.5.29.21"))) { // CRLReason
                    switch (RubyNumeric.fix2int(((IRubyObject) value).callMethod(runtime.getCurrentContext(), "value"))) {
                        case 0:
                            return runtime.newString("Unspecified");
                        case 1:
                            return runtime.newString("Key Compromise");
                        case 2:
                            return runtime.newString("CA Compromise");
                        case 3:
                            return runtime.newString("Affiliation Changed");
                        case 4:
                            return runtime.newString("Superseded");
                        case 5:
                            return runtime.newString("Cessation Of Operation");
                        case 6:
                            return runtime.newString("Certificate Hold");
                        case 8:
                            return runtime.newString("Remove From CRL");
                        case 9:
                            return runtime.newString("Privilege Withdrawn");
                        default:
                            return runtime.newString("Unspecified");
                    }
                } else if (getRealOid().equals(new ASN1ObjectIdentifier("2.5.29.17"))) { //subjectAltName
                    try {
                        ASN1Primitive seq = new ASN1InputStream(getRealValueBytes()).readObject();
                        GeneralName[] n1 = null;
                        if (seq instanceof org.bouncycastle.asn1.ASN1TaggedObject) {
                            n1 = new GeneralName[]{GeneralName.getInstance(seq)};
                        } else {
                            n1 = GeneralNames.getInstance(seq).getNames();
                        }
                        StringBuilder sbe = new StringBuilder();
                        String sep = "";
                        for (int i = 0; i < n1.length; i++) {
                            sbe.append(sep);
                            if (n1[i].getTagNo() == GeneralName.dNSName) {
                                sbe.append("DNS:");
                                sbe.append(((ASN1String) n1[i].getName()).getString());
                            } else if (n1[i].getTagNo() == GeneralName.iPAddress) {
                                sbe.append("IP Address:");
                                byte[] bs = ((DEROctetString) n1[i].getName()).getOctets();
                                String sep2 = "";
                                for (int j = 0; j < bs.length; j++) {
                                    sbe.append(sep2);
                                    sbe.append(((int) bs[j]) & 0xff);
                                    sep2 = ".";
                                }
                            } else {
                                sbe.append(n1[i].toString());
                            }
                            sep = ", ";
                        }
                        return runtime.newString(sbe.toString());
                    } catch (Exception e) {
                        return runtime.newString(getRealValue().toString());
                    }
                } else {
                    try {
                        return ASN1.decode(runtime.getClassFromPath("OpenSSL::ASN1"), RubyString.newString(runtime, getRealValueBytes()))
                                .callMethod(runtime.getCurrentContext(), "value").callMethod(runtime.getCurrentContext(), "to_s");
                    } catch (Exception e) {
                        return runtime.newString(getRealValue().toString());
                    }
                }
            } catch (IOException ioe) {
                throw newX509ExtError(runtime, ioe.getMessage());
            }
        }

        @JRubyMethod(name="critical?")
        public IRubyObject critical_p() {
            return critical ? getRuntime().getTrue() : getRuntime().getFalse();
        }

        @JRubyMethod
        public IRubyObject to_der() {
            ASN1EncodableVector all = new ASN1EncodableVector();
            try {
                all.add(getRealOid());
                all.add(getRealCritical() ? DERBoolean.TRUE : DERBoolean.FALSE);
                all.add(new DEROctetString(getRealValueBytes()));
                return RubyString.newString(getRuntime(), new DLSequence(all).getEncoded(ASN1Encoding.DER));
            } catch (IOException ioe) {
                throw newX509ExtError(getRuntime(), ioe.getMessage());
            }
        }
    }

    private static RaiseException newX509ExtError(Ruby runtime, String message) {
        return Utils.newError(runtime, "OpenSSL::X509::ExtensionError", message);
    }
}// X509Extensions
