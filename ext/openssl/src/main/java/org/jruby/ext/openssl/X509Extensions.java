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
import java.math.BigInteger;
import java.security.GeneralSecurityException;

import org.bouncycastle.asn1.ASN1Boolean;
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
import org.bouncycastle.asn1.ASN1TaggedObject;
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
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Visibility;
import org.jruby.util.ByteList;

import org.jruby.ext.openssl.impl.ASN1Registry;
import static org.jruby.ext.openssl.ASN1._ASN1;
import static org.jruby.ext.openssl.X509._X509;
import static org.jruby.ext.openssl.OpenSSLReal.debug;
import static org.jruby.ext.openssl.OpenSSLReal.debugStackTrace;
import static org.jruby.ext.openssl.OpenSSLReal.isDebug;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509Extensions {

    public static void createX509Ext(final Ruby runtime, final RubyModule _X509) { // OpenSSL::X509
        final RubyClass _ExtensionFactory = _X509.defineClassUnder("ExtensionFactory",
                runtime.getObject(), ExtensionFactory.ALLOCATOR);
        _ExtensionFactory.defineAnnotatedMethods(ExtensionFactory.class);

        final RubyClass _OpenSSLError = runtime.getModule("OpenSSL").getClass("OpenSSLError");
        _X509.defineClassUnder("ExtensionError", _OpenSSLError, _OpenSSLError.getAllocator());

        RubyClass _Extension = _X509.defineClassUnder("Extension", runtime.getObject(), Extension.ALLOCATOR);
        _Extension.defineAnnotatedMethods(Extension.class);
    }

    public static class ExtensionFactory extends RubyObject {

        private static final long serialVersionUID = 3180447029639456500L;

        static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new ExtensionFactory(runtime, klass);
            }
        };

        public ExtensionFactory(Ruby runtime, RubyClass type) {
            super(runtime,type);
        }

        @JRubyMethod(rest = true, visibility = Visibility.PRIVATE)
        public IRubyObject initialize(final IRubyObject[] args, final Block unusedBlock) {
            Arity.checkArgumentCount(getRuntime(), args, 0, 4);
            if ( args.length > 0 && ! args[0].isNil() ) {
                set_issuer_cert( args[0] );
            }
            if ( args.length > 1 && ! args[1].isNil() ) {
                set_subject_cert( args[1] );
            }
            if ( args.length > 2 && ! args[2].isNil() ) {
                set_subject_req( args[2] );
            }
            if ( args.length > 3 && ! args[3].isNil() ) {
                set_crl( args[3] );
            }
            return this;
        }

        @JRubyMethod(name="issuer_certificate")
        public IRubyObject issuer_cert() {
            return getInstanceVariable("@issuer_certificate");
        }

        @JRubyMethod(name="issuer_certificate=")
        public IRubyObject set_issuer_cert(IRubyObject arg) {
            setInstanceVariable("@issuer_certificate", arg);
            return arg;
        }

        @JRubyMethod(name="subject_certificate")
        public IRubyObject subject_cert() {
            return getInstanceVariable("@subject_certificate");
        }

        @JRubyMethod(name="subject_certificate=")
        public IRubyObject set_subject_cert(IRubyObject arg) {
            setInstanceVariable("@subject_certificate", arg);
            return arg;
        }

        @JRubyMethod(name="subject_request")
        public IRubyObject subject_req() {
            return getInstanceVariable("@subject_request");
        }

        @JRubyMethod(name="subject_request=")
        public IRubyObject set_subject_req(IRubyObject arg) {
            setInstanceVariable("@subject_request", arg);
            return arg;
        }

        @JRubyMethod(name="crl")
        public IRubyObject crl() {
            return getInstanceVariable("@crl");
        }

        @JRubyMethod(name="crl=")
        public IRubyObject set_crl(IRubyObject arg) {
            setInstanceVariable("@crl", arg);
            return arg;
        }

        @JRubyMethod(name="config")
        public IRubyObject config() {
            return getInstanceVariable("@config");
        }

        @JRubyMethod(name="config=")
        public IRubyObject set_config(IRubyObject arg) {
            setInstanceVariable("@config", arg);
            return arg;
        }

        @JRubyMethod(rest = true)
        public IRubyObject create_ext(final ThreadContext context, final IRubyObject[] args) {
            final Ruby runtime = context.runtime;

            IRubyObject critical;
            if ( Arity.checkArgumentCount(runtime, args, 2, 3) == 3 && ! args[2].isNil() ) {
                critical = args[2];
            }
            else {
                critical = runtime.getFalse();
            }

            final String oid = args[0].toString();
            String valuex = args[1].toString();
            Object value = valuex;

            final ASN1ObjectIdentifier objectId;
            try {
                objectId = ASN1.getObjectIdentifier(runtime, oid);
            }
            catch (IllegalArgumentException e) {
                debug(runtime, "ASN1.getObjectIdentifier() at ExtensionFactory.create_ext", e);
                throw newExtensionError(runtime, "unknown OID `" + oid + "'");
            }

            if ( valuex.startsWith("critical,") ) {
                critical = runtime.getTrue();
                valuex = valuex.substring(9).trim();
            }

            try {
                final String id  = objectId.getId();
                if ( id.equals("2.5.29.14") ) { //subjectKeyIdentifier
                    DEROctetString inp = parseSubjectKeyIdentifier(context, oid, valuex);
                    value = new String( ByteList.plain( inp.getEncoded(ASN1Encoding.DER) ) );
                }
                else if ( id.equals("2.5.29.35") ) { //authorityKeyIdentifier
                    DLSequence inp = parseAuthorityKeyIdentifier(context, valuex);
                    value = new String( ByteList.plain( inp.getEncoded(ASN1Encoding.DER) ) );
                }
                else if ( id.equals("2.5.29.18") ) { //issuerAltName
                    value = parseIssuerAltName(context, valuex);
                }
                else if ( id.equals("2.5.29.19") ) { //basicConstraints
                    DLSequence inp = parseBasicConstrains(valuex);
                    value = new String( ByteList.plain( inp.getEncoded(ASN1Encoding.DER) ) );
                }
                else if ( id.equals("2.5.29.15") ) { //keyUsage
                    DERBitString inp = parseKeyUsage(oid, valuex);
                    value = new String( ByteList.plain( inp.getEncoded(ASN1Encoding.DER) ) );
                }
                else if ( id.equals("2.16.840.1.113730.1.1") ) { //nsCertType
                    value = parseNsCertType(oid, valuex);
                }
                else if ( id.equals("2.5.29.17") ) { //subjectAltName
                    value = parseSubjectAltName(valuex);
                }
                else if ( id.equals("2.5.29.37") ) { //extendedKeyUsage
                    value = parseExtendedKeyUsage(valuex);
                }
                else {
                    value = new DEROctetString(
                        new DEROctetString(ByteList.plain(valuex)).getEncoded(ASN1Encoding.DER)
                    );
                }
            }
            catch (IOException e) {
                throw newExtensionError(runtime, "Unable to create extension: " + e.getMessage());
            }

            Extension ext = (Extension) _X509(runtime).getClass("Extension").callMethod(context, "new");

            ext.setRealOid(objectId);
            ext.setRealValue(value);
            ext.setRealCritical(critical.isNil() ? null : critical.isTrue());

            return ext;
        }

        private DERBitString parseKeyUsage(final String oid, final String valuex) {
            byte[] inp;
            try {
                final String[] val = valuex.split(":");
                inp = new byte[ val.length ];
                for ( int i = 0; i < val.length; i++ ) {
                    inp[i] = (byte) Integer.parseInt(val[i], 16);
                }
            }
            catch (NumberFormatException e) {
                inp = null;
            }

            if ( inp == null && valuex.length() < 3 ) inp = ByteList.plain(valuex);

            if ( inp == null ) {
                byte v1 = 0; byte v2 = 0;
                final String[] val = valuex.split(",");
                for ( int i = 0; i < val.length; i++ ) {
                    final String value = val[i].trim();
                    if ( "decipherOnly".equals(value) || "Decipher Only".equals(value) ) {
                        v2 |= (byte) 128;
                    } else if ( "digitalSignature".equals(value) || "Digital Signature".equals(value) ) {
                        v1 |= (byte) 128;
                    } else if ( "nonRepudiation".equals(value) || "Non Repudiation".equals(value) ) {
                        v1 |= (byte)  64;
                    } else if ( "keyEncipherment".equals(value) || "Key Encipherment".equals(value) ) {
                        v1 |= (byte)  32;
                    } else if ( "dataEncipherment".equals(value) || "Data Encipherment".equals(value) ) {
                        v1 |= (byte)  16;
                    } else if ( "keyAgreement".equals(value) || "Key Agreement".equals(value) ) {
                        v1 |= (byte)   8;
                    } else if ( "keyCertSign".equals(value) || "Key Cert Sign".equals(value) ) {
                        v1 |= (byte)   4;
                    } else if ( "cRLSign".equals(value) ) {
                        v1 |= (byte)   2;
                    } else if ( "encipherOnly".equals(value) || "Encipher Only".equals(value) ) {
                        v1 |= (byte)   1;
                    } else {
                        throw newExtensionError(getRuntime(), oid + " = " + valuex + ": unknown bit string argument");
                    }
                }
                inp = ( v2 == 0 ) ? new byte[] { v1 } : new byte[] { v1, v2 };
            }

            int unused = 0;
            for ( int i = inp.length- 1; i > -1; i-- ) {
                if ( inp[i] == 0 ) unused += 8;
                else {
                    byte a2 = inp[i];
                    int x = 8;
                    while ( a2 != 0 ) {
                        a2 <<= 1; x--;
                    }
                    unused += x;
                    break;
                }
            }

            return new DERBitString(inp, unused);
        }

        private DERBitString parseNsCertType(String oid, String valuex) {
            byte v = 0;
            if ( valuex.length() < 3 ) {
                byte[] inp = ByteList.plain(valuex); v = inp[0];
            }
            else {
                final String[] val = valuex.split(",");
                for ( int i = 0; i < val.length; i++ ) {
                    final String value = val[i].trim();
                    if ( "SSL Client".equals(value) || "client".equals(value) ) {
                        v |= (byte) 128;
                    } else if ( "SSL Server".equals(value) || "server".equals(value) ) {
                        v |= (byte)  64;
                    } else if ( "S/MIME".equals(value) || "email".equals(value) ) {
                        v |= (byte)  32;
                    } else if ( "Object Signing".equals(value) || "objsign".equals(value) ) {
                        v |= (byte)  16;
                    } else if ( "Unused".equals(value) || "reserved".equals(value) ) {
                        v |= (byte)   8;
                    } else if ( "SSL CA".equals(value) || "sslCA".equals(value) ) {
                        v |= (byte)   4;
                    } else if ( "S/MIME CA".equals(value) || "emailCA".equals(value) ) {
                        v |= (byte)   2;
                    } else if ( "Object Signing CA".equals(value) || "objCA".equals(value) ) {
                        v |= (byte)   1;
                    } else {
                        throw newExtensionError(getRuntime(), oid + " = " + valuex + ": unknown bit string argument");
                    }
                }
            }

            int unused = 0;
            if ( v == 0 ) unused += 8;
            else {
                byte a2 = v; int x = 8;
                while (a2 != 0) {
                    a2 <<= 1; x--;
                }
                unused += x;
            }

            return new DERBitString(new byte[] { v }, unused);
        }

        private static DLSequence parseBasicConstrains(final String valuex) {
            final String[] val = valuex.split(",");
            final ASN1EncodableVector vec = new ASN1EncodableVector();
            for ( int i = 0; i < val.length; i++ ) {
                final String value = ( val[i] = val[i].trim() );
                if ( value.length() > 3 && value.substring(0, 3).equalsIgnoreCase("CA:") ) {
                    boolean isTrue = "true".equalsIgnoreCase( value.substring(3).trim() );
                    vec.add( ASN1Boolean.getInstance(isTrue) );
                }
            }
            for ( int i = 0; i < val.length; i++) {
                final String value = val[i];
                if ( value.length() > 8 && value.substring(0, 8).equalsIgnoreCase("pathlen:") ) {
                    int pathlen = Integer.parseInt( value.substring(8).trim() );
                    vec.add( new ASN1Integer( BigInteger.valueOf(pathlen) ) );
                }
            }
            return new DLSequence(vec);
        }

        private DLSequence parseAuthorityKeyIdentifier(final ThreadContext context, final String valuex) {
            final ASN1EncodableVector vec = new ASN1EncodableVector();
            if ( valuex.startsWith("keyid:always") ) {
                vec.add( new DEROctetString( derDigest(context) ) );
            }
            else if ( valuex.startsWith("keyid") ) {
                vec.add( new DEROctetString( derDigest(context) ) );
            }
            return new DLSequence(vec);
        }

        private byte[] derDigest(final ThreadContext context) {
            final Ruby runtime = context.runtime;
            IRubyObject pkey = getInstanceVariable("@issuer_certificate").callMethod(context, "public_key");
            IRubyObject der;
            if ( pkey instanceof PKeyRSA ) {
                der = pkey.callMethod(context, "to_der");
            } else {
                der = ASN1.decode(context, _ASN1(runtime), pkey.callMethod(context, "to_der"));
                der = der.callMethod(context, "value")
                         .callMethod(context, "[]", runtime.newFixnum(1))
                         .callMethod(context, "value");
            }
            return getSHA1Digest( runtime, der.asString().getBytes() );
        }

        private Object parseIssuerAltName(final ThreadContext context, final String valuex) throws IOException {
            if ( valuex.startsWith("issuer:copy") ) {
                RubyArray exts = (RubyArray) getInstanceVariable("@issuer_certificate").callMethod(context, "extensions");
                for ( int i = 0; i < exts.size(); i++ ) {
                    Extension ext = (Extension) exts.entry(i);
                    if ( ext.getRealOid().equals(new ASN1ObjectIdentifier("2.5.29.17")) ) {
                        return ext.getRealValue();
                    }
                }
            }
            throw new IOException("Malformed IssuerAltName: " + valuex);
        }

        private String parseSubjectAltName(final String valuex) throws IOException {
            if ( valuex.startsWith("DNS:") ) {
                final String dns = valuex.substring(4);
                return derEncoded( new GeneralName( GeneralName.dNSName, new DERIA5String(dns) ) );
            }
            else if ( valuex.startsWith("IP:") || valuex.startsWith("IP Address:") ) {
                final int idx = valuex.charAt(2) == ':' ? 3 : 11;
                String[] numbers = valuex.substring(idx).split("\\.");
                final byte[] ip = new byte[4];
                ip[0] = (byte) (Integer.parseInt(numbers[0]) & 0xff);
                ip[1] = (byte) (Integer.parseInt(numbers[1]) & 0xff);
                ip[2] = (byte) (Integer.parseInt(numbers[2]) & 0xff);
                ip[3] = (byte) (Integer.parseInt(numbers[3]) & 0xff);
                return derEncoded( new GeneralName( GeneralName.iPAddress, new DEROctetString(ip) ) );
            }
            else {
                return valuex;
            }
        }

        private static String derEncoded(final GeneralName name) throws IOException {
            final GeneralNames names = new GeneralNames(name);
            return new String( ByteList.plain( names.getEncoded(ASN1Encoding.DER) ) );
        }

        private DEROctetString parseSubjectKeyIdentifier(final ThreadContext context,
            final String oid, final String valuex) {

            if ( "hash".equalsIgnoreCase(valuex) ) {
                return new DEROctetString( derDigest(context) );
            }
            else if ( valuex.length() == 20 || ! isHex(valuex) ) {
                return new DEROctetString( ByteList.plain(valuex) );
            }
            else {
                final int len = valuex.length();
                final ByteList hex = new ByteList( len / 2 + 1 );
                for ( int i = 0; i < len; i += 2 ) {
                    if ( i + 1 >= len ) {
                        throw newExtensionError(context.runtime, oid + " = " + valuex + ": odd number of digits");
                    }
                    final int c1 = upHex( valuex.charAt(i) );
                    final int c2 = upHex( valuex.charAt(i + 1) );
                    if ( c1 != -1 && c2 != -1 ) {
                        hex.append( ( (c1 << 4) & 0xF0 ) | ( c2 & 0xF ) );
                    } else {
                        throw newExtensionError(context.runtime, oid + " = " + valuex + ": illegal hex digit");
                    }
                    while ( (i + 2) < len && valuex.charAt(i + 2) == ':' ) i++;
                }
                final byte[] hexBytes = new byte[ hex.length() ];
                System.arraycopy(hex.getUnsafeBytes(), hex.getBegin(), hexBytes, 0, hexBytes.length);
                return new DEROctetString( hexBytes );
            }
        }

        private static DLSequence parseExtendedKeyUsage(final String valuex) {
            ASN1EncodableVector vector = new ASN1EncodableVector();
            for ( String name : valuex.split(", ?") ) {
                vector.add( ASN1Registry.sym2oid(name) );
            }
            return new DLSequence(vector);
        }

    }

    private static byte[] getSHA1Digest(Ruby runtime, byte[] bytes) {
        try {
            return SecurityHelper.getMessageDigest("SHA-1").digest(bytes);
        }
        catch (GeneralSecurityException e) {
            throw newExtensionError(runtime, e.getMessage());
        }
    }

    public static class Extension extends RubyObject {
        private static final long serialVersionUID = -1160318458085651926L;

        static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new Extension(runtime, klass);
            }
        };

        public Extension(Ruby runtime, RubyClass type) {
            super(runtime,type);
        }

        private ASN1ObjectIdentifier oid;
        private Object value;
        private Boolean critical;

        ASN1ObjectIdentifier getRealOid() {
            return oid;
        }

        void setRealOid(ASN1ObjectIdentifier oid) {
            this.oid = oid;
        }

        Object getRealValue() {
            return value;
        }

        void setRealValue(Object value) {
            this.value = value;
        }

        byte[] getRealValueBytes() throws IOException {
            if ( value instanceof RubyString ) {
                return ((RubyString) value).getBytes();
            } else if ( value instanceof String ) {
                return ByteList.plain( (String) value );
            } else if ( value instanceof DEROctetString ) {
                return ((DEROctetString) value).getOctets();
            } else if ( value instanceof ASN1Encodable ) {
                return ((ASN1Encodable) value).toASN1Primitive().getEncoded(ASN1Encoding.DER);
            } else {
                ASN1Encodable asn1Value = ((ASN1.ASN1Data) value).toASN1(getRuntime().getCurrentContext());
                return asn1Value.toASN1Primitive().getEncoded(ASN1Encoding.DER);
            }
        }

        boolean isRealCritical() {
            return critical == null ? Boolean.FALSE : critical.booleanValue();
        }

        //Boolean getRealCritical() {
        //    return critical;
        //}

        void setRealCritical(boolean critical) {
            this.critical = Boolean.valueOf(critical);
        }

        private void setRealCritical(Boolean critical) {
            this.critical = critical;
        }

        @JRubyMethod(name = "initialize", rest = true, visibility = Visibility.PRIVATE)
        public IRubyObject _initialize(final ThreadContext context, final IRubyObject[] args) {
            byte[] octets = null;
            if ( args.length == 1 ) {
                try {
                    ASN1InputStream asn1Stream = new ASN1InputStream(
                        OpenSSLImpl.to_der_if_possible(context, args[0]).asString().getBytes()
                    );
                    ASN1Sequence seq = (ASN1Sequence) asn1Stream.readObject();
                    setRealOid( (ASN1ObjectIdentifier) seq.getObjectAt(0) );
                    setRealCritical( ( (DERBoolean) seq.getObjectAt(1) ).isTrue() );
                    octets = ( (DEROctetString) seq.getObjectAt(2) ).getOctets();
                }
                catch (IOException ioe) {
                    throw newExtensionError(context.runtime, ioe.getMessage());
                }
            }
            else if (args.length > 1) {
                setRealOid( ASN1.getObjectIdentifier( context.runtime, args[0].toString() ) );
                setRealValue( args[1] );
            }
            if ( args.length > 2 ) {
                setRealCritical( args[2].isTrue() );
            }
            if ( args.length > 0 && octets != null ) {
                setRealValue( new String(ByteList.plain(octets)) );
            }
            return this;
        }

        @JRubyMethod
        public IRubyObject oid(final ThreadContext context) {
            String val = ASN1.getSymLookup(context.runtime).get(oid);
            if ( val == null ) val = oid.toString();
            return context.runtime.newString(val);
        }

        @JRubyMethod(name="oid=")
        public IRubyObject set_oid(final ThreadContext context, IRubyObject arg) {
            if ( arg instanceof RubyString ) {
                setRealOid( ASN1.getObjectIdentifier( context.runtime, arg.toString() ) );
                return arg;
            }
            throw context.runtime.newTypeError(arg, context.runtime.getString());
        }

        private static final byte[] CA_ = { 'C','A',':' };
        private static final byte[] TRUE = { 'T','R','U','E' };
        private static final byte[] FALSE = { 'F','A','L','S','E' };

        private static final byte[] _ = {};
        private static final byte[] SEP = { ',',' ' };

        private static final byte[] Decipher_Only = { 'D','e','c','i','p','h','e','r',' ','O','n','l','y' };
        private static final byte[] Digital_Signature = { 'D','i','g','i','t','a','l',' ','S','i','g','n','a','t','u','r','e' };
        private static final byte[] Non_Repudiation = { 'N','o','n',' ','R','e','p','u','d','i','a','t','i','o','n' };
        private static final byte[] Key_Encipherment = { 'K','e','y',' ','E','n','c','i','p','h','e','r','m','e','n','t' };
        private static final byte[] Data_Encipherment = { 'D','a','t','a',' ','E','n','c','i','p','h','e','r','m','e','n','t' };
        private static final byte[] Key_Agreement = { 'K','e','y',' ','A','g','r','e','e','m','e','n','t' };
        private static final byte[] Certificate_Sign = { 'C','e','r','t','i','f','i','c','a','t','e',' ','S','i','g','n' };
        private static final byte[] CRL_Sign = { 'C','R','L',' ','S','i','g','n' };
        private static final byte[] Encipher_Only = { 'E','n','c','i','p','h','e','r',' ','O','n','l','y' };
        private static final byte[] SSL_Client = { 'S','S','L',' ','C','l','i','e','n','t' };
        private static final byte[] SSL_Server = { 'S','S','L',' ','S','e','r','v','e','r' };
        private static final byte[] SSL_CA = { 'S','S','L',' ','C','A' };
        private static final byte[] SMIME = { 'S','/','M','I','M','E' };
        private static final byte[] SMIME_CA = { 'S','/','M','I','M','E',' ','C','A' };
        private static final byte[] Object_Signing = { 'O','b','j','e','c','t',' ','S','i','g','n','i','n','g' };
        private static final byte[] Object_Signing_CA = { 'O','b','j','e','c','t',' ','S','i','g','n','i','n','g',' ','C','A' };
        private static final byte[] Unused = { 'U','n','u','s','e','d' };
        private static final byte[] Unspecified = { 'U','n','s','p','e','c','i','f','i','e','d' };
        //private static final byte[] Key_Compromise = { 'K','e','y',' ','C','o','m','p','r','o','m','i','s','e' };
        //private static final byte[] CA_Compromise = { 'C','A',' ','C','o','m','p','r','o','m','i','s','e' };
        //private static final byte[] Affiliation_Changed = { 'A','f','f','i','l','i','a','t','i','o','n',' ','C','h','a','n','g','e','d' };

        private static final byte[] keyid_ = { 'k','e','y','i','d',':' };

        @JRubyMethod
        public IRubyObject value(final ThreadContext context) {
            final Ruby runtime = context.runtime;
            try {
                final String realOid = getRealOid().getId();
                if ( realOid.equals("2.5.29.19") ) { //basicConstraints
                    ASN1Sequence seq2 = (ASN1Sequence) new ASN1InputStream(getRealValueBytes()).readObject();
                    final ByteList val = new ByteList(32);
                    if ( seq2.size() > 0 ) {
                        val.append(CA_);
                        val.append( ( (DERBoolean) seq2.getObjectAt(0) ).isTrue() ? TRUE : FALSE );
                    }
                    if ( seq2.size() > 1 ) {
                        val.append( ", pathlen:".getBytes() );
                        val.append( seq2.getObjectAt(1).toString().getBytes() );
                    }
                    return runtime.newString(val);
                }
                else if ( realOid.equals("2.5.29.15") ) { //keyUsage
                    final byte[] bytes = getRealValueBytes();
                    byte b1 = 0; byte b2 = bytes[2];
                    if ( bytes.length > 3 ) b1 = bytes[3];

                    final ByteList val = new ByteList(64); byte[] sep = _;

                    if ((b2 & (byte) 128) != 0) {
                        val.append(sep); val.append(Decipher_Only); sep = SEP;
                    }
                    if ((b1 & (byte) 128) != 0) {
                        val.append(sep); val.append(Digital_Signature); sep = SEP;
                    }
                    if ((b1 & (byte) 64) != 0) {
                        val.append(sep); val.append(Non_Repudiation); sep = SEP;
                    }
                    if ((b1 & (byte) 32) != 0) {
                        val.append(sep); val.append(Key_Encipherment); sep = SEP;
                    }
                    if ((b1 & (byte) 16) != 0) {
                        val.append(sep); val.append(Data_Encipherment); sep = SEP;
                    }
                    if ((b1 & (byte) 8) != 0) {
                        val.append(sep); val.append(Key_Agreement); sep = SEP;
                    }
                    if ((b1 & (byte) 4) != 0) {
                        val.append(sep); val.append(Certificate_Sign); sep = SEP;
                    }
                    if ((b1 & (byte) 2) != 0) {
                        val.append(sep); val.append(CRL_Sign); sep = SEP;
                    }
                    if ((b1 & (byte) 1) != 0) {
                        val.append(sep); val.append(Encipher_Only); // sep = SEP;
                    }
                    return runtime.newString( val );
                }
                else if ( realOid.equals("2.16.840.1.113730.1.1") ) { //nsCertType
                    final byte b = getRealValueBytes()[0];

                    final ByteList val = new ByteList(64); byte[] sep = _;

                    if ((b & (byte) 128) != 0) {
                        val.append(sep); val.append(SSL_Client); sep = SEP;
                    }
                    if ((b & (byte) 64) != 0) {
                        val.append(sep); val.append(SSL_Server); sep = SEP;
                    }
                    if ((b & (byte) 32) != 0) {
                        val.append(sep); val.append(SMIME); sep = SEP;
                    }
                    if ((b & (byte) 16) != 0) {
                        val.append(sep); val.append(Object_Signing); sep = SEP;
                    }
                    if ((b & (byte) 8) != 0) {
                        val.append(sep); val.append(Unused); sep = SEP;
                    }
                    if ((b & (byte) 4) != 0) {
                        val.append(sep); val.append(SSL_CA); sep = SEP;
                    }
                    if ((b & (byte) 2) != 0) {
                        val.append(sep); val.append(SMIME_CA); sep = SEP;
                    }
                    if ((b & (byte) 1) != 0) {
                        val.append(sep); val.append(Object_Signing_CA);
                    }
                    return runtime.newString( val );
                }
                else if ( realOid.equals("2.5.29.14") ) { //subjectKeyIdentifier
                    final byte[] bytes = getRealValueBytes();
                    return runtime.newString( hexBytes(bytes, 2) );
                }
                else if ( realOid.equals("2.5.29.35") ) { // authorityKeyIdentifier
                    ASN1Sequence seq = (ASN1Sequence) new ASN1InputStream(getRealValueBytes()).readObject();
                    if ( seq.size() == 0 ) return runtime.newString();
                    final ByteList val = new ByteList(32);
                    val.append( keyid_ );
                    ASN1Primitive keyid = seq.getObjectAt(0).toASN1Primitive();
                    final byte[] bytes;
                    if ( keyid instanceof DEROctetString ) {
                        bytes = ((DEROctetString) keyid).getOctets();
                    } else {
                        bytes = keyid.getEncoded(ASN1Encoding.DER);
                    }
                    return runtime.newString( hexBytes(bytes, val) );
                }
                else if ( realOid.equals("2.5.29.21") ) { // CRLReason
                    IRubyObject val = ( (IRubyObject) value ).callMethod(context, "value");
                    switch ( RubyNumeric.fix2int(val) ) {
                        case 0: return runtime.newString(new ByteList(Unspecified));
                        case 1: return runtime.newString("Key Compromise");
                        case 2: return runtime.newString("CA Compromise");
                        case 3: return runtime.newString("Affiliation Changed");
                        case 4: return runtime.newString("Superseded");
                        case 5: return runtime.newString("Cessation Of Operation");
                        case 6: return runtime.newString("Certificate Hold");
                        case 8: return runtime.newString("Remove From CRL");
                        case 9: return runtime.newString("Privilege Withdrawn");
                        default: return runtime.newString(new ByteList(Unspecified));
                    }
                }
                else if ( realOid.equals("2.5.29.17") ) { //subjectAltName
                    try {
                        ASN1Primitive seq = new ASN1InputStream(getRealValueBytes()).readObject();
                        final GeneralName[] names;
                        if ( seq instanceof ASN1TaggedObject ) {
                            names = new GeneralName[] { GeneralName.getInstance(seq) };
                        } else {
                            names = GeneralNames.getInstance(seq).getNames();
                        }
                        final StringBuilder val = new StringBuilder(48); String sep = "";
                        for ( int i = 0; i < names.length; i++ ) {
                            final GeneralName name = names[i];
                            val.append(sep);
                            if ( name.getTagNo() == GeneralName.dNSName ) {
                                val.append("DNS:");
                                val.append( ((ASN1String) name.getName()).getString() );
                            }
                            else if ( name.getTagNo() == GeneralName.iPAddress ) {
                                val.append("IP Address:");
                                byte[] bs = ((DEROctetString) name.getName()).getOctets();
                                String sep2 = "";
                                for ( int j = 0; j < bs.length; j++ ) {
                                    val.append(sep2).append(((int) bs[j]) & 0xff);
                                    sep2 = ".";
                                }
                            }
                            else {
                                val.append( name.toString() );
                            }
                            sep = ", ";
                        }
                        return runtime.newString( val.toString() );
                    }
                    catch (RuntimeException e) {
                        debugStackTrace(runtime, e);
                        return runtime.newString( getRealValue().toString() );
                    }
                }
                else {
                    IRubyObject decoded = RubyString.newString(runtime, getRealValueBytes());
                    try {
                        decoded = ASN1.decodeImpl(context, decoded);
                        return decoded.callMethod(context, "value").asString();
                    }
                    catch (IOException e) {
                        if ( isDebug(runtime) ) e.printStackTrace(runtime.getOut());
                        return runtime.newString( getRealValue().toString() ); // throw e;
                    }
                    catch (IllegalArgumentException e) {
                        return runtime.newString( getRealValue().toString() );
                    }
                }
            }
            catch (IOException e) {
                throw newExtensionError(runtime, e.getMessage());
            }
        }

        @JRubyMethod(name="value=")
        public IRubyObject set_value(final ThreadContext context, IRubyObject arg) {
            if ( arg instanceof RubyString ) {
                setRealValue(arg); return arg;
            }
            throw context.runtime.newTypeError(arg, context.runtime.getString());
        }

        @JRubyMethod(name="critical?")
        public IRubyObject critical_p() {
            return getRuntime().newBoolean( isRealCritical() );
        }

        @JRubyMethod(name="critical=")
        public IRubyObject set_critical(final ThreadContext context, IRubyObject arg) {
            setRealCritical( arg.isTrue() ); return arg;
        }

        @JRubyMethod
        public IRubyObject to_der() {
            final ASN1EncodableVector vec = new ASN1EncodableVector();
            try {
                vec.add( getRealOid() );
                if ( critical != null && critical.booleanValue() ) {
                    vec.add( DERBoolean.TRUE );
                }
                vec.add( new DEROctetString(getRealValueBytes()) );
                return RubyString.newString(getRuntime(), new DLSequence(vec).getEncoded(ASN1Encoding.DER));
            }
            catch (IOException e) {
                throw newExtensionError(getRuntime(), e.getMessage());
            }
        }

    }

    private static RaiseException newExtensionError(Ruby runtime, String message) {
        return Utils.newError(runtime, _X509(runtime).getClass("ExtensionError"), message);
    }

    // our custom "internal" HEX helpers :

    private static boolean isHex(final char c) {
        return ('0' <= c && c <= '9') || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f');
    }

    private static boolean isHex(final String str) {
        for ( int i = 0; i < str.length(); i++ ) {
            if ( ! isHex(str.charAt(i)) ) return false;
        }
        return true;
    }

    private static int upHex(final char c) {
        switch (c) {
            case '0' : return '0';
            case '1' : return '1';
            case '2' : return '2';
            case '3' : return '3';
            case '4' : return '4';
            case '5' : return '5';
            case '6' : return '6';
            case '7' : return '7';
            case '8' : return '8';
            case '9' : return '9';
            case 'A' :
            case 'a' : return 'A';
            case 'B' :
            case 'b' : return 'B';
            case 'C' :
            case 'c' : return 'C';
            case 'D' :
            case 'd' : return 'D';
            case 'E' :
            case 'e' : return 'E';
            case 'F' :
            case 'f' : return 'F';
        }
        return -1;
    }

    private static ByteList hexBytes(final byte[] data, final int off) {
        final int len = data.length - off;
        return hexBytes(data, off, len, new ByteList( len * 3 ));
    }

    private static ByteList hexBytes(final byte[] data, final ByteList out) {
        return hexBytes(data, 0, data.length, out);
    }

    //@SuppressWarnings("deprecation")
    //private static ByteList hexBytes(final ByteList data, final ByteList out) {
    //    return hexBytes(data.bytes, data.begin, data.realSize, out);
    //}

    private static ByteList hexBytes(final byte[] data, final int off, final int len, final ByteList out) {
        boolean notFist = false;
        out.ensure( len * 3 - 1 );
        for ( int i = off; i < (off + len); i++ ) {
            if ( notFist ) out.append(':');
            final byte b = data[i];
            out.append( HEX[ (b >> 4) & 0xF ] );
            out.append( HEX[ b & 0xF ] );
            notFist = true;
        }
        return out;
    }

    private static final char[] HEX = {
        '0' , '1' , '2' , '3' , '4' , '5' , '6' , '7' ,
        '8' , '9' , 'A' , 'B' , 'C' , 'D' , 'E' , 'F'
    };

}// X509Extensions
