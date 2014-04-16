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
import java.util.Iterator;
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

    public static void createX509Cert(Ruby runtime, RubyModule mX509) {
        RubyClass cX509Cert = mX509.defineClassUnder("Certificate",runtime.getObject(),X509CERT_ALLOCATOR);
        RubyClass openSSLError = runtime.getModule("OpenSSL").getClass("OpenSSLError");
        mX509.defineClassUnder("CertificateError",openSSLError,openSSLError.getAllocator());

        cX509Cert.defineAnnotatedMethods(X509Cert.class);
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
        final RubyModule _Certificate = runtime.getClassFromPath("OpenSSL::X509::Certificate");
        final RubyString encoded = RubyString.newString(runtime, cert.getEncoded());
        return _Certificate.callMethod(runtime.getCurrentContext(), "new", encoded);
    }

    // this is the javax.security counterpart of the previous wrap method
    public static IRubyObject wrap(Ruby runtime, javax.security.cert.Certificate cert)
        throws javax.security.cert.CertificateEncodingException {
        final RubyModule _Certificate = runtime.getClassFromPath("OpenSSL::X509::Certificate");
        final RubyString encoded = RubyString.newString(runtime, cert.getEncoded());
        return _Certificate.callMethod(runtime.getCurrentContext(), "new", encoded);
    }

    @JRubyMethod(name="initialize", optional = 1, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(final ThreadContext context,
        final IRubyObject[] args, final Block unusedBlock) {
        final Ruby runtime = context.runtime;

        if (args.length == 0) return this;

        byte[] bytes = OpenSSLImpl.readX509PEM(context, args[0]);
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

        RubyModule  _OpenSSL = runtime.getModule("OpenSSL");
        RubyModule _X509 = (RubyModule) _OpenSSL.getConstant("X509");
        IRubyObject _Name = _X509.getConstant("Name");

        try {
            cert = (X509Certificate) SecurityHelper.getCertificateFactory("X.509").generateCertificate(bis);
        }
        catch (CertificateException ex) {
            throw newCertificateError(runtime, ex);
        }

        if (cert == null) {
            throw newCertificateError(runtime, (String) null);
        }

        set_serial(RubyNumeric.str2inum(runtime,runtime.newString(cert.getSerialNumber().toString()),10));
        set_not_before(context, RubyTime.newTime(runtime,cert.getNotBefore().getTime()));
        set_not_after(context, RubyTime.newTime(runtime,cert.getNotAfter().getTime()));
        set_subject(_Name.callMethod(context,"new",RubyString.newString(runtime, cert.getSubjectX500Principal().getEncoded())));
        set_issuer(_Name.callMethod(context,"new",RubyString.newString(runtime, cert.getIssuerX500Principal().getEncoded())));

        String algorithm = cert.getPublicKey().getAlgorithm();
        set_public_key(algorithm, cert.getPublicKey().getEncoded());

        IRubyObject extFact = ((RubyClass)(_X509.getConstant("ExtensionFactory"))).callMethod(context,"new");
        extFact.callMethod(context,"subject_certificate=",this);

        Set<String> crit = cert.getCriticalExtensionOIDs();
        if (crit != null) {
            for (Iterator<String> iter = crit.iterator(); iter.hasNext();) {
                String critOid = iter.next();
                byte[] value = cert.getExtensionValue(critOid);
                IRubyObject rValue = ASN1.decode(context,  _OpenSSL.getConstant("ASN1"), runtime.newString(new ByteList(value, false))).callMethod(context, "value");
                X509Extensions.Extension ext = (X509Extensions.Extension) _X509.getConstant("Extension").callMethod(context, "new",
                        new IRubyObject[] { runtime.newString(critOid), rValue, runtime.getTrue() });
                add_extension(ext);
            }
        }

        Set<String> ncrit = cert.getNonCriticalExtensionOIDs();
        if (ncrit != null) {
            for (Iterator<String> iter = ncrit.iterator(); iter.hasNext();) {
                String ncritOid = iter.next();
                byte[] value = cert.getExtensionValue(ncritOid);
                // TODO: wired. J9 returns null for an OID given in getNonCriticalExtensionOIDs()
                if (value != null) {
                    IRubyObject rValue = ASN1.decode(context,  _OpenSSL.getConstant("ASN1"), runtime.newString(new ByteList(value, false))).callMethod(context, "value");
                    X509Extensions.Extension ext = (X509Extensions.Extension) _X509.getConstant("Extension").callMethod(context, "new",
                            new IRubyObject[] { runtime.newString(ncritOid), rValue, runtime.getFalse() });
                    add_extension(ext);
                }
            }
        }
        changed = false;

        return this;
    }

    //Lazy method for public key instantiation
    private void set_public_key(String algorithm, byte[] encoded) {
        this.public_key_algorithm = algorithm;
        this.public_key_encoded = encoded;
    }

    public static RaiseException newCertificateError(Ruby runtime, Exception ex) {
        return newCertificateError(runtime, ex.getMessage());
    }

    public static RaiseException newCertificateError(Ruby runtime, String message) {
        throw Utils.newError(runtime, "OpenSSL::X509::CertificateError", message);
    }

    @Override
    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(IRubyObject obj) {
        if(this == obj) {
            return this;
        }
        checkFrozen();
        return this;
    }

    @JRubyMethod
    public IRubyObject to_der() {
        try {
            return RubyString.newString(getRuntime(), cert.getEncoded());
        } catch (CertificateEncodingException ex) {
            throw newCertificateError(getRuntime(), ex);
        }
    }

    @JRubyMethod(name={"to_pem","to_s"})
    public IRubyObject to_pem() {
        try {
            StringWriter w = new StringWriter();
            PEMInputOutput.writeX509Certificate(w, getAuxCert());
            w.close();
            return getRuntime().newString(w.toString());
        } catch (IOException ex) {
            throw getRuntime().newIOErrorFromException(ex);
        }
    }

    @JRubyMethod
    public IRubyObject to_text() {
        return getRuntime().newString(getAuxCert().toString());
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
    public IRubyObject set_version(IRubyObject arg) {
        if(!arg.equals(this.version)) {
            changed = true;
        }
        this.version = arg;
        return arg;
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
    public IRubyObject set_serial(IRubyObject num) {
        if(!num.equals(this.serial)) {
            changed = true;
        }
        serial = num;
        String s = serial.toString();

        BigInteger bi;
        if (s.equals("0")) { // MRI compatibility: allow 0 serial number
            bi = BigInteger.ONE;
        } else {
            bi = new BigInteger(s);
        }
	generator.setSerialNumber(new BigInteger(1, bi.toByteArray()));
        return num;
    }

    @JRubyMethod
    public IRubyObject subject() {
        return subject;
    }

    @JRubyMethod(name="subject=")
    public IRubyObject set_subject(IRubyObject arg) {
        if(!arg.equals(this.subject)) {
            changed = true;
        }
        subject = arg;
        generator.setSubjectDN(((X509Name)subject).getRealName());
        return arg;
    }

    @JRubyMethod
    public IRubyObject issuer() {
        return issuer;
    }

    @JRubyMethod(name="issuer=")
    public IRubyObject set_issuer(IRubyObject arg) {
        if(!arg.equals(this.issuer)) {
            changed = true;
        }
        issuer = arg;
        generator.setIssuerDN(((X509Name)issuer).getRealName());
        return arg;
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
    public IRubyObject set_public_key(IRubyObject arg) {
        if ( ! ( arg instanceof PKey ) ) {
            throw getRuntime().newTypeError("OpenSSL::PKey::PKey expected but got " + arg.getMetaClass().getName());
        }
        if ( ! arg.equals(this.public_key) ) {
            changed = true;
        }
        this.public_key = arg;
        generator.setPublicKey(((PKey) public_key).getPublicKey());
        return arg;
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

        if(("DSA".equalsIgnoreCase(keyAlg) && "MD5".equalsIgnoreCase(digAlg)) ||
           ("RSA".equalsIgnoreCase(keyAlg) && "DSS1".equals(digName))) {
            throw newCertificateError(runtime, "signature_algorithm not supported");
        }

        for ( X509Extensions.Extension ext : extensions ) {
            try {
                final byte[] bytes = ext.getRealValueBytes();
                generator.addExtension(ext.getRealOid(), ext.getRealCritical(), bytes);
            } catch (IOException ioe) {
                throw runtime.newIOErrorFromException(ioe);
            }
        }

        generator.setSignatureAlgorithm(digAlg + "WITH" + keyAlg);

        if ( public_key == null ) lazyInitializePublicKey(context);

        try {
            cert = generator.generate(((PKey) key).getPrivateKey());
        } catch (Exception e) {
            throw newCertificateError(getRuntime(), e.getMessage());
        }
        if (cert == null) {
            throw newCertificateError(runtime, (String) null);
        }
        String name = ASN1Registry.o2a(cert.getSigAlgOID());
        if (name == null) {
            name = cert.getSigAlgOID();
        }
        sig_alg = runtime.newString(name);
        changed = false;
        return this;
    }

    @JRubyMethod
    public IRubyObject verify(IRubyObject key) {
        if(changed) {
            return getRuntime().getFalse();
        }
        try {
            cert.verify(((PKey)key).getPublicKey());
            return getRuntime().getTrue();
        } catch (CertificateException ce) {
            throw newCertificateError(getRuntime(), ce);
        } catch (NoSuchAlgorithmException nsae) {
            throw newCertificateError(getRuntime(), nsae);
        } catch (NoSuchProviderException nspe) {
            throw newCertificateError(getRuntime(), nspe);
        } catch (SignatureException se) {
            return getRuntime().getFalse();
        } catch(InvalidKeyException e) {
            return getRuntime().getFalse();
        }
    }

    @JRubyMethod
    public IRubyObject check_private_key(IRubyObject arg) {
        PKey key = (PKey)arg;
        PublicKey pkey = key.getPublicKey();
        PublicKey certPubKey = getAuxCert().getPublicKey();
        if (certPubKey.equals(pkey))
            return getRuntime().getTrue();
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
    public IRubyObject set_extensions(final IRubyObject arg) {
        extensions.clear(); // RubyArray is a List :
        extensions.addAll( (List<X509Extensions.Extension>) arg );
        return arg;
    }

    @JRubyMethod
    public IRubyObject add_extension(final IRubyObject arg) {
        changed = true;
        final X509Extensions.Extension extension = (X509Extensions.Extension) arg;
        final ASN1ObjectIdentifier oid = extension.getRealOid();
        final ASN1ObjectIdentifier _2_5_29_17 = new ASN1ObjectIdentifier("2.5.29.17");
        if ( oid.equals( _2_5_29_17 ) ) {
            boolean one = true;
            for ( X509Extensions.Extension ext : extensions ) {
                if ( ext.getRealOid().equals( _2_5_29_17 ) ) {
                    ASN1EncodableVector v1 = new ASN1EncodableVector();

                    try {
                        GeneralName[] n1 = GeneralNames.getInstance(new ASN1InputStream(ext.getRealValueBytes()).readObject()).getNames();
                        GeneralName[] n2 = GeneralNames.getInstance(new ASN1InputStream(extension.getRealValueBytes()).readObject()).getNames();

                        for(int i=0;i<n1.length;i++) {
                            v1.add(n1[i]);
                        }
                        for(int i=0;i<n2.length;i++) {
                            v1.add(n2[i]);
                        }

                        ext.setRealValue(new String(ByteList.plain(GeneralNames.getInstance(new DLSequence(v1)).getEncoded(ASN1Encoding.DER))));
                    } catch (IOException ex) {
                        throw getRuntime().newIOErrorFromException(ex);
                    }
                    one = false;
                    break;
                }
            }
            if ( one ) extensions.add(extension);
        }
        else {
            extensions.add(extension);
        }
        return arg;
    }
}// X509Cert

