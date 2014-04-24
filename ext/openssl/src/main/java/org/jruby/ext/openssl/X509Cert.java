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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.x509.X509V3CertificateGenerator;

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubyTime;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.openssl.impl.ASN1Registry;
import org.jruby.ext.openssl.x509store.PEMInputOutput;
import org.jruby.ext.openssl.x509store.X509AuxCertificate;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import static org.jruby.ext.openssl.X509._X509;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509Cert extends RubyObject {
    private static final long serialVersionUID = 5626619026058595493L;

    private static ObjectAllocator X509CERT_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new X509Cert(runtime, klass);
        }
    };

    public static void createX509Cert(final Ruby runtime, final RubyModule _X509) {
        RubyClass _Certificate = _X509.defineClassUnder("Certificate", runtime.getObject(), X509CERT_ALLOCATOR);
        RubyClass _OpenSSLError = runtime.getModule("OpenSSL").getClass("OpenSSLError");
        _X509.defineClassUnder("CertificateError", _OpenSSLError, _OpenSSLError.getAllocator());
        _Certificate.defineAnnotatedMethods(X509Cert.class);
    }

    public X509Cert(Ruby runtime, RubyClass type) {
        super(runtime,type);
    }

    private IRubyObject serial;
    private RubyTime not_before;
    private RubyTime not_after;
    private IRubyObject issuer;
    private IRubyObject subject;
    private IRubyObject public_key;

    private IRubyObject sig_alg;
    private IRubyObject version;

    private final List<X509Extensions.Extension> extensions = new ArrayList<X509Extensions.Extension>();

    private boolean changed = true;

    private X509V3CertificateGenerator generator = new X509V3CertificateGenerator();
    private X509Certificate cert;
    private String public_key_algorithm;
    private byte[] public_key_encoded;

    X509AuxCertificate getAuxCert() {
        if ( cert == null ) return null;
        if ( cert instanceof X509AuxCertificate ) {
            return (X509AuxCertificate) cert;
        }
        return new X509AuxCertificate(cert);
    }

    public static IRubyObject wrap(Ruby runtime, Certificate cert) throws CertificateEncodingException {
        final RubyClass _Certificate = _X509(runtime).getClass("Certificate");
        final RubyString encoded = RubyString.newString(runtime, cert.getEncoded());
        return _Certificate.callMethod(runtime.getCurrentContext(), "new", encoded);
    }

    // this is the javax.security counterpart of the previous wrap method
    public static IRubyObject wrap(Ruby runtime, javax.security.cert.Certificate cert)
        throws javax.security.cert.CertificateEncodingException {
        final RubyClass _Certificate = _X509(runtime).getClass("Certificate");
        final RubyString encoded = RubyString.newString(runtime, cert.getEncoded());
        return _Certificate.callMethod(runtime.getCurrentContext(), "new", encoded);
    }

    @JRubyMethod(name="initialize", optional = 1, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(final ThreadContext context,
        final IRubyObject[] args, final Block unusedBlock) {
        final Ruby runtime = context.runtime;

        if ( args.length == 0 ) return this;

        byte[] bytes = OpenSSLImpl.readX509PEM(context, args[0]);
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

        final RubyModule _OpenSSL = runtime.getModule("OpenSSL");
        final RubyModule _X509 = (RubyModule) _OpenSSL.getConstant("X509");
        final RubyClass _Name = _X509.getClass("Name");

        try {
            cert = (X509Certificate) SecurityHelper.getCertificateFactory("X.509").generateCertificate(bis);
        }
        catch (CertificateException e) {
            throw newCertificateError(runtime, e);
        }

        if ( cert == null ) {
            throw newCertificateError(runtime, (String) null);
        }

        set_serial( RubyNumeric.str2inum(runtime, runtime.newString(cert.getSerialNumber().toString()), 10) );
        set_not_before( context, RubyTime.newTime( runtime, cert.getNotBefore().getTime() ) );
        set_not_after( context, RubyTime.newTime( runtime, cert.getNotAfter().getTime() ) );
        bytes = cert.getSubjectX500Principal().getEncoded();
        set_subject( _Name.callMethod(context, "new", RubyString.newString(runtime, bytes) ) );
        bytes = cert.getIssuerX500Principal().getEncoded();
        set_issuer( _Name.callMethod(context, "new", RubyString.newString(runtime, bytes) ) );

        final String algorithm = cert.getPublicKey().getAlgorithm();
        set_public_key( algorithm, cert.getPublicKey().getEncoded() );

        IRubyObject extFact = ((RubyClass)(_X509.getConstant("ExtensionFactory"))).callMethod(context,"new");
        extFact.callMethod(context, "subject_certificate=", this);

        final RubyModule _ASN1 = (RubyModule) _OpenSSL.getConstant("ASN1");
        final RubyClass _Extension = _X509.getClass("Extension");

        final Set<String> criticalExtOIDs = cert.getCriticalExtensionOIDs();
        if ( criticalExtOIDs != null ) {
            for ( final String extOID : criticalExtOIDs ) {
                addExtension(context, _ASN1, _Extension, extOID, runtime.getTrue());
            }
        }

        final Set<String> nonCriticalExtOIDs = cert.getNonCriticalExtensionOIDs();
        if ( nonCriticalExtOIDs != null ) {
            for ( final String extOID : nonCriticalExtOIDs ) {
                addExtension(context, _ASN1, _Extension, extOID, runtime.getFalse());
            }
        }
        changed = false;

        return this;
    }

    private void addExtension(final ThreadContext context, final RubyModule _ASN1,
        final RubyClass _Extension, final String extOID, final RubyBoolean critical) {
        final byte[] extValue = cert.getExtensionValue(extOID);
        // TODO: wired. J9 returns null for an OID given in getNonCriticalExtensionOIDs()
        if ( extValue == null ) return;

        RubyString extValueStr = context.runtime.newString( new ByteList(extValue, false) );
        IRubyObject rValue = ASN1.decode(context, _ASN1, extValueStr).callMethod(context, "value");
        IRubyObject extension = _Extension.callMethod(context, "new",
            new IRubyObject[] { context.runtime.newString(extOID), rValue, critical });
        add_extension(extension);
    }

    //Lazy method for public key instantiation
    private void set_public_key(String algorithm, byte[] encoded) {
        this.public_key_algorithm = algorithm;
        this.public_key_encoded = encoded;
    }

    private static RubyClass _CertificateError(final Ruby runtime) {
        return _X509(runtime).getClass("CertificateError");
    }

    public static RaiseException newCertificateError(final Ruby runtime, Exception e) {
        return Utils.newError(runtime, _CertificateError(runtime), e);
    }

    static RaiseException newCertificateError(final Ruby runtime, String msg) {
        return Utils.newError(runtime, _CertificateError(runtime), msg);
    }

    @Override
    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(IRubyObject obj) {
        if ( this == obj ) return this;

        checkFrozen();
        return this;
    }

    @JRubyMethod
    public IRubyObject to_der() {
        try {
            return RubyString.newString(getRuntime(), cert.getEncoded());
        }
        catch (CertificateEncodingException ex) {
            throw newCertificateError(getRuntime(), ex);
        }
    }

    @JRubyMethod(name={"to_pem","to_s"})
    public IRubyObject to_pem() {
        final StringWriter str = new StringWriter();
        try {
            PEMInputOutput.writeX509Certificate(str, getAuxCert());
            return getRuntime().newString( str.toString() );
        }
        catch (IOException ex) {
            throw getRuntime().newIOErrorFromException(ex);
        }
    }

    @JRubyMethod
    public IRubyObject to_text() {
        return getRuntime().newString( getAuxCert().toString() );
    }

    @Override
    @JRubyMethod
    public IRubyObject inspect() {
        return getRuntime().getNil();
    }

    @JRubyMethod
    public IRubyObject version() {
        return version;
    }

    @JRubyMethod(name="version=")
    public IRubyObject set_version(final IRubyObject version) {
        if ( ! version.equals(this.version) ) {
            this.changed = true;
        }
        return this.version = version;
    }

    @JRubyMethod
    public IRubyObject signature_algorithm() {
        return sig_alg;
    }

    @JRubyMethod
    public IRubyObject serial() {
        return serial;
    }

    @JRubyMethod(name="serial=")
    public IRubyObject set_serial(final IRubyObject serial) {
        if ( ! serial.equals(this.serial) ) {
            this.changed = true;
        }

        final String serialStr = serial.toString();
        final BigInteger serialInt;
        if ( serialStr.equals("0") ) { // MRI compatibility: allow 0 serial number
            serialInt = BigInteger.ONE;
        } else {
            serialInt = new BigInteger(serialStr);
        }
        generator.setSerialNumber(new BigInteger(1, serialInt.toByteArray()));
        return this.serial = serial;
    }

    @JRubyMethod
    public IRubyObject subject() {
        return subject;
    }

    @JRubyMethod(name="subject=")
    public IRubyObject set_subject(final IRubyObject subject) {
        if ( ! subject.equals(this.subject) ) {
            this.changed = true;
        }
        generator.setSubjectDN( ((X509Name) subject).getRealName() );
        return this.subject = subject;
    }

    @JRubyMethod
    public IRubyObject issuer() {
        return issuer;
    }

    @JRubyMethod(name="issuer=")
    public IRubyObject set_issuer(final IRubyObject issuer) {
        if ( ! issuer.equals(this.issuer) ) {
            this.changed = true;
        }
        generator.setIssuerDN( ((X509Name) issuer).getRealName() );
        return this.issuer = issuer;
    }

    @JRubyMethod
    public IRubyObject not_before() {
        return not_before;
    }

    @JRubyMethod(name="not_before=")
    public IRubyObject set_not_before(final ThreadContext context, final IRubyObject time) {
        changed = true;
        not_before = (RubyTime) time.callMethod(context, "getutc");
        not_before.setMicroseconds(0);
        generator.setNotBefore( not_before.getJavaDate() );
        return time;
    }

    @JRubyMethod
    public IRubyObject not_after() {
        return not_after;
    }

    @JRubyMethod(name="not_after=")
    public IRubyObject set_not_after(final ThreadContext context, final IRubyObject time) {
        changed = true;
        not_after = (RubyTime) time.callMethod(context, "getutc");
        not_after.setMicroseconds(0);
        generator.setNotAfter( not_after.getJavaDate() );
        return time;
    }

    @JRubyMethod
    public IRubyObject public_key(final ThreadContext context) {
        if ( this.public_key == null ) {
            lazyInitializePublicKey(context);
        }
        return public_key.callMethod(context, "public_key");
    }

    @JRubyMethod(name="public_key=")
    public IRubyObject set_public_key(IRubyObject public_key) {
        if ( ! ( public_key instanceof PKey ) ) {
            throw getRuntime().newTypeError("OpenSSL::PKey::PKey expected but got " + public_key.getMetaClass().getName());
        }
        if ( ! public_key.equals(this.public_key) ) {
            this.changed = true;
        }
        generator.setPublicKey(((PKey) public_key).getPublicKey());
        return this.public_key = public_key;
    }

    private void lazyInitializePublicKey(final ThreadContext context) {
        if ( public_key_encoded == null || public_key_algorithm == null ) {
            throw new IllegalStateException("lazy public key initialization failed");
        }
        RubyModule _OpenSSL = context.runtime.getModule("OpenSSL");
        RubyModule _PKey = (RubyModule) _OpenSSL.getConstant("PKey");
        final boolean _changed = changed;
        if ( "RSA".equalsIgnoreCase(public_key_algorithm) ) {
            RubyString encoded = RubyString.newString(context.runtime, public_key_encoded);
            set_public_key( _PKey.getConstant("RSA").callMethod(context, "new", encoded) );
        } else if ( "DSA".equalsIgnoreCase(public_key_algorithm) ) {
            RubyString encoded = RubyString.newString(context.runtime, public_key_encoded);
            set_public_key( _PKey.getConstant("DSA").callMethod(context, "new", encoded) );
        } else {
            throw newCertificateError(context.runtime, "The algorithm " + public_key_algorithm + " is unsupported for public keys");
        }
        changed = _changed;
    }

    @JRubyMethod
    public IRubyObject sign(final ThreadContext context, final IRubyObject key, final IRubyObject digest) {
        final Ruby runtime = context.runtime;

        // Have to obey some artificial constraints of the OpenSSL implementation. Stupid.
        final String keyAlg = ((PKey) key).getAlgorithm();
        final String digAlg = ((Digest) digest).getShortAlgorithm();
        final String digName = ((Digest) digest).name().toString();

        if( ( "DSA".equalsIgnoreCase(keyAlg) && "MD5".equalsIgnoreCase(digAlg) ) ||
            ( "RSA".equalsIgnoreCase(keyAlg) && "DSS1".equals(digName) ) ) {
            throw newCertificateError(runtime, "signature_algorithm not supported");
        }

        for ( X509Extensions.Extension ext : extensions ) {
            try {
                final byte[] bytes = ext.getRealValueBytes();
                generator.addExtension(ext.getRealOid(), ext.isRealCritical(), bytes);
            }
            catch (IOException ioe) {
                throw runtime.newIOErrorFromException(ioe);
            }
        }

        generator.setSignatureAlgorithm(digAlg + "WITH" + keyAlg);

        if ( public_key == null ) lazyInitializePublicKey(context);

        try {
            cert = generator.generate(((PKey) key).getPrivateKey());
        }
        catch (GeneralSecurityException e) {
            throw newCertificateError(getRuntime(), e);
        }
        if (cert == null) {
            throw newCertificateError(runtime, (String) null);
        }
        String name = ASN1Registry.o2a(cert.getSigAlgOID());
        if (name == null) {
            name = cert.getSigAlgOID();
        }
        this.sig_alg = runtime.newString(name);
        this.changed = false;
        return this;
    }

    @JRubyMethod
    public IRubyObject verify(IRubyObject key) {
        if ( changed ) return getRuntime().getFalse();

        try {
            cert.verify(((PKey)key).getPublicKey());
            return getRuntime().getTrue();
        }
        catch (CertificateException e) {
            throw newCertificateError(getRuntime(), e);
        }
        catch (NoSuchAlgorithmException e) {
            throw newCertificateError(getRuntime(), e);
        }
        catch (NoSuchProviderException e) {
            throw newCertificateError(getRuntime(), e);
        }
        catch (SignatureException e) {
            return getRuntime().getFalse();
        }
        catch (InvalidKeyException e) {
            return getRuntime().getFalse();
        }
    }

    @JRubyMethod
    public IRubyObject check_private_key(IRubyObject arg) {
        PKey key = (PKey) arg;
        PublicKey pubKey = key.getPublicKey();
        PublicKey certPubKey = getAuxCert().getPublicKey();
        if ( certPubKey.equals(pubKey) ) return getRuntime().getTrue();
        return getRuntime().getFalse();
    }

    @JRubyMethod
    public IRubyObject extensions() {
        @SuppressWarnings("unchecked")
        final List<IRubyObject> extensions = (List) this.extensions;
        return getRuntime().newArray( extensions );
    }

    @SuppressWarnings("unchecked")
    @JRubyMethod(name="extensions=")
    public IRubyObject set_extensions(final IRubyObject array) {
        extensions.clear(); // RubyArray is a List :
        extensions.addAll( (List<X509Extensions.Extension>) array );
        return array;
    }

    @JRubyMethod
    public IRubyObject add_extension(final IRubyObject ext) {
        changed = true;
        final X509Extensions.Extension newExtension = (X509Extensions.Extension) ext;
        final ASN1ObjectIdentifier oid = newExtension.getRealOid();
        if ( oid.getId().equals( "2.5.29.17" ) ) {
            boolean one = true;
            for ( final X509Extensions.Extension curExtension : extensions ) {
                if ( curExtension.getRealOid().equals( oid ) ) {
                    final ASN1EncodableVector v1 = new ASN1EncodableVector();
                    try {
                        GeneralName[] n1 = GeneralNames.getInstance(new ASN1InputStream(curExtension.getRealValueBytes()).readObject()).getNames();
                        GeneralName[] n2 = GeneralNames.getInstance(new ASN1InputStream(newExtension.getRealValueBytes()).readObject()).getNames();

                        for ( int i = 0; i < n1.length; i++ ) v1.add( n1[i] );
                        for ( int i = 0; i < n2.length; i++ ) v1.add( n2[i] );

                        GeneralNames v1Names = GeneralNames.getInstance(new DLSequence(v1));
                        curExtension.setRealValue( new String( ByteList.plain( v1Names.getEncoded(ASN1Encoding.DER) ) ) );
                    }
                    catch (IOException ex) {
                        throw getRuntime().newIOErrorFromException(ex);
                    }
                    one = false;
                    break;
                }
            }
            if ( one ) extensions.add(newExtension);
        }
        else {
            extensions.add(newExtension);
        }
        return ext;
    }

}// X509Cert
