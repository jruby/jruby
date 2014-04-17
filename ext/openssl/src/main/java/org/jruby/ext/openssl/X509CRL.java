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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Hashtable;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.CertificateFactory;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERBoolean;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.x509.X509V2CRLGenerator;
import org.bouncycastle.asn1.x509.X509Extensions;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubyTime;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.openssl.x509store.PEMInputOutput;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.ext.openssl.OpenSSLReal.isDebug;
import static org.jruby.ext.openssl.OpenSSLReal.warn;
import static org.jruby.ext.openssl.X509._X509;
import static org.jruby.ext.openssl.X509Extensions.Extension;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509CRL extends RubyObject {
    private static final long serialVersionUID = -2463300006179688577L;

    private static ObjectAllocator X509CRL_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new X509CRL(runtime, klass);
        }
    };

    public static void createX509CRL(final Ruby runtime, final RubyModule _X509) {
        RubyClass _CRL = _X509.defineClassUnder("CRL",runtime.getObject(),X509CRL_ALLOCATOR);
        RubyClass _OpenSSLError = runtime.getModule("OpenSSL").getClass("OpenSSLError");
        _X509.defineClassUnder("CRLError", _OpenSSLError, _OpenSSLError.getAllocator());
        _CRL.defineAnnotatedMethods(X509CRL.class);
    }

    private IRubyObject version;
    private IRubyObject issuer;
    private IRubyObject last_update;
    private IRubyObject next_update;
    private IRubyObject revoked; // RubyArray

    private RubyArray extensions;

    private IRubyObject sig_alg;

    private boolean changed = true;

    private final X509V2CRLGenerator generator = new X509V2CRLGenerator();
    private java.security.cert.X509CRL crl;

    private ASN1Primitive crl_v;

    java.security.cert.X509CRL getCRL() {
        return crl;
    }

    public X509CRL(Ruby runtime, RubyClass type) {
        super(runtime,type);
    }

    @JRubyMethod(name="initialize", rest=true, visibility = Visibility.PRIVATE)
    public IRubyObject _initialize(final ThreadContext context,
        final IRubyObject[] args, final Block block) {
        final Ruby runtime = context.runtime;

        this.extensions = runtime.newArray();
        this.revoked = runtime.newArray();

        if ( Arity.checkArgumentCount(runtime, args, 0, 1) == 0 ) {
            issuer = version = runtime.getNil();
            next_update = last_update = runtime.getNil();
            return this;
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(args[0].convertToString().getBytes());
        try { // FIXME: use BC for now :
            // SunJCE throws java.security.cert.CRLException: Invalid encoding of AuthorityKeyIdentifierExtension.
            CertificateFactory x509 = OpenSSLReal.getX509CertificateFactoryBC();
            crl = (java.security.cert.X509CRL) x509.generateCRL(bis);
        }
        catch (GeneralSecurityException e) {
            throw newCRLError(runtime, e.getMessage());
        }

        byte[] crl_bytes = OpenSSLImpl.readX509PEM(context, args[0]);
        try {
            crl_v = new ASN1InputStream(new ByteArrayInputStream(crl_bytes)).readObject();
        }
        catch (IOException e) {
            throw newCRLError(runtime, e);
        }

        final ASN1Sequence seqa = (ASN1Sequence)((ASN1Sequence) crl_v).getObjectAt(0);
        final ASN1Encodable v0 = seqa.getObjectAt(0);
        if ( v0 instanceof ASN1Integer ) {
            set_version( runtime.newFixnum( ((ASN1Integer) v0).getValue().intValue() ) );
        }
        else {
            set_version( runtime.newFixnum(2) );
        }

        set_last_update( context, RubyTime.newTime(runtime, crl.getThisUpdate().getTime()) );
        set_next_update( context, RubyTime.newTime(runtime, crl.getNextUpdate().getTime()) );
        final RubyString name = RubyString.newString(runtime, crl.getIssuerX500Principal().getEncoded());
        final RubyModule _OpenSSL = runtime.getModule("OpenSSL");
        final RubyModule _X509 = (RubyModule) _OpenSSL.getConstant("X509");
        set_issuer( _X509.getClass("Name").callMethod(context, "new", name) );

        final ASN1Primitive maybe_ext = (ASN1Primitive) seqa.getObjectAt( seqa.size() - 1 );
        if ( maybe_ext instanceof ASN1TaggedObject && ( (ASN1TaggedObject) maybe_ext ).getTagNo() == 0 ) {
            final RubyModule _ASN1 = (RubyModule) _OpenSSL.getConstant("ASN1");
            final RubyClass _Extension = _X509.getClass("Extension");

            ASN1Sequence exts = (ASN1Sequence) ( (ASN1TaggedObject) maybe_ext ).getObject();
            for ( int i = 0; i < exts.size(); i++ ) {
                ASN1Sequence extSeq = (ASN1Sequence) exts.getObjectAt(i);
                boolean critical = false;
                if ( extSeq.getObjectAt(1) == DERBoolean.TRUE ) critical = true;

                String oid = ( (ASN1ObjectIdentifier) extSeq.getObjectAt(0) ).getId();
                final byte[] valueBytes = crl.getExtensionValue(oid);
                IRubyObject realValue;
                try {
                    IRubyObject value = RubyString.newString(runtime, valueBytes);
                    value = ASN1.decodeImpl(context, _ASN1, value).callMethod(context, "value");
                    realValue = ASN1.decodeImpl(context, _ASN1, value);
                }
                catch (Exception e) {
                    if ( isDebug(runtime) ) e.printStackTrace( runtime.getOut() );
                    realValue = RubyString.newString(runtime, valueBytes);
                }

                Extension ext = (Extension) _Extension.callMethod(context, "new");
                ext.setRealOid( ASN1.getObjectIdentifier(context.runtime, oid) );
                ext.setRealValue(realValue);
                ext.setRealCritical(critical);
                this.extensions.append(ext);
            }
        }

        this.changed = false;
        return this;
    }

    @Override
    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(final IRubyObject obj) {
        final ThreadContext context = getRuntime().getCurrentContext();
        warn(context, "WARNING: unimplemented method called: CRL#init_copy");
        if ( this == obj ) return this;
        checkFrozen(); return this;
    }

    @JRubyMethod(name = {"to_pem", "to_s"})
    public IRubyObject to_pem(final ThreadContext context) {
        StringWriter writer = new StringWriter();
        try {
            PEMInputOutput.writeX509CRL(writer, crl);
            return context.runtime.newString(writer.toString());
        }
        catch (IOException e) {
            throw newCRLError(context.runtime, e);
        }
    }

    @JRubyMethod
    public IRubyObject to_der(final ThreadContext context) {
        try {
            return RubyString.newString(context.runtime, crl_v.getEncoded());
        }
        catch (IOException e) {
            throw newCRLError(context.runtime, e);
        }
    }

    private static final String IND8 = "        ";
    private static final String IND12 = "            ";
    private static final String IND16 = "                ";
    private static final DateFormat ASN_DATE = new SimpleDateFormat("MMM dd HH:mm:ss yyyy zzz");

    @JRubyMethod
    public IRubyObject to_text(final ThreadContext context) {
        final Ruby runtime = context.runtime;
        final StringBuilder text = new StringBuilder(128);

        text.append("Certificate Revocation List (CRL):\n");
        text.append(IND8).append("Version ").append( RubyNumeric.fix2int(version) + 1 ).append(" (0x");
        text.append( Integer.toString( RubyNumeric.fix2int(version), 16 ) ).append(")\n");
        ASN1ObjectIdentifier oid = (ASN1ObjectIdentifier) ( (ASN1Sequence) ((ASN1Sequence) crl_v).getObjectAt(1) ).getObjectAt(0);
        text.append(IND8).append("Signature Algorithm: ").append( ASN1.nid2ln(runtime, ASN1.obj2nid(runtime, oid)) ).append("\n");
        text.append(IND8).append("Issuer: ").append( issuer() ).append("\n");
        text.append(IND8).append("Last Update: ").
            append( ASN_DATE.format( ((RubyTime) last_update()).getJavaDate() ) ).append("\n");
        if ( ! next_update().isNil() ) {
            text.append(IND8).append("Next Update: ").
                append( ASN_DATE.format(((RubyTime) next_update()).getJavaDate() )).append("\n");
        } else {
            text.append(IND8).append("Next Update: NONE\n");
        }
        if ( extensions.size() > 0 ) {
            text.append(IND8).append("CRL extensions\n");
            for ( int i = 0; i < extensions.size(); i++ ) {
                Extension ext = (Extension) extensions.entry(i);
                ASN1ObjectIdentifier oiden = ext.getRealOid();
                text.append(IND12).append( ASN1.o2a(runtime, oiden) ).append(": ");
                if ( ext.getRealCritical() ) text.append("critical");
                text.append("\n");
                text.append(IND16).append( ext.value(context) ).append("\n");
            }
        }
        /*
    114         rev = X509_CRL_get_REVOKED(x);
    115
    116         if(sk_X509_REVOKED_num(rev) > 0)
    117             BIO_printf(out, "Revoked Certificates:\n");
    118         else BIO_printf(out, "No Revoked Certificates.\n");
    119
    120         for(i = 0; i < sk_X509_REVOKED_num(rev); i++) {
    121                 r = sk_X509_REVOKED_value(rev, i);
    122                 BIO_printf(out,"    Serial Number: ");
    123                 i2a_ASN1_INTEGER(out,r->serialNumber);
    124                 BIO_printf(out,"\n        Revocation Date: ");
    125                 ASN1_TIME_print(out,r->revocationDate);
    126                 BIO_printf(out,"\n");
    127                 X509V3_extensions_print(out, "CRL entry extensions",
    128                                                 r->extensions, 0, 8);
    129         }
    130         X509_signature_print(out, x->sig_alg, x->signature);
    131
        */
        return runtime.newString( text.toString() );
    }

    @JRubyMethod
    public IRubyObject version() {
        return this.version;
    }

    @JRubyMethod(name="version=")
    public IRubyObject set_version(IRubyObject val) {
        if ( ! val.equals(this.version) ) {
            this.changed = true;
        }
        this.version = val;
        return val;
    }

    @JRubyMethod
    public IRubyObject signature_algorithm() {
        return sig_alg;
    }

    @JRubyMethod
    public IRubyObject issuer() {
        return this.issuer;
    }

    @JRubyMethod(name="issuer=")
    public IRubyObject set_issuer(final IRubyObject issuer) {
        if ( ! issuer.equals(this.issuer) ) {
            this.changed = true;
        }
        generator.setIssuerDN(((X509Name) issuer).getRealName());
        return this.issuer = issuer;
    }

    @JRubyMethod
    public IRubyObject last_update() {
        return this.last_update;
    }

    @JRubyMethod(name="last_update=")
    public IRubyObject set_last_update(final ThreadContext context, IRubyObject val) {
        changed = true;
        final RubyTime value = (RubyTime) val.callMethod(context, "getutc");
        value.setMicroseconds(0);
        generator.setThisUpdate( value.getJavaDate() );
        return this.last_update = val;
    }

    @JRubyMethod
    public IRubyObject next_update() {
        return this.next_update;
    }

    @JRubyMethod(name="next_update=")
    public IRubyObject set_next_update(final ThreadContext context, IRubyObject val) {
        this.changed = true;
        final RubyTime value = (RubyTime) val.callMethod(context, "getutc");
        value.setMicroseconds(0);
        generator.setNextUpdate( value.getJavaDate() );
        return this.next_update = val;
    }

    @JRubyMethod
    public IRubyObject revoked() {
        return this.revoked;
    }

    @JRubyMethod(name="revoked=")
    public IRubyObject set_revoked(final IRubyObject revoked) {
        this.changed = true;
        return this.revoked = revoked;
    }

    @JRubyMethod
    public IRubyObject add_revoked(final ThreadContext context, IRubyObject val) {
        this.changed = true;
        this.revoked.callMethod(context, "<<", val); return val;
    }

    @JRubyMethod
    public IRubyObject extensions() {
        return this.extensions;
    }

    @SuppressWarnings("unchecked")
    @JRubyMethod(name="extensions=")
    public IRubyObject set_extensions(final IRubyObject extensions) {
        this.extensions = (RubyArray) extensions; return extensions;
    }

    @JRubyMethod
    public IRubyObject add_extension(final IRubyObject extension) {
        this.extensions.append(extension); return extension;
    }

    @JRubyMethod
    public IRubyObject sign(final ThreadContext context, final IRubyObject key, IRubyObject digest) {
        final Ruby runtime = context.runtime;
        // Have to obey some artificial constraints of the OpenSSL implementation. Stupid.
        final String keyAlg = ((PKey) key).getAlgorithm();
        final String digAlg = ((Digest) digest).getShortAlgorithm();

        if ( ( "DSA".equalsIgnoreCase(keyAlg) && "MD5".equalsIgnoreCase(digAlg) ) ||
             ( "RSA".equalsIgnoreCase(keyAlg) && "DSS1".equals(((Digest)digest).name().toString()) ) ||
             ( "DSA".equalsIgnoreCase(keyAlg) && "SHA1".equals(((Digest)digest).name().toString()) ) ) {
            throw newCRLError(runtime, "unsupported key / digest algorithm ("+ key +" / "+ digAlg +")");
        }

        sig_alg = runtime.newString(digAlg);
        generator.setSignatureAlgorithm(digAlg + "WITH" + keyAlg);

        final RubyArray revoked = (RubyArray) this.revoked;
        for ( int i = 0; i < revoked.size(); i++ ) {
            X509Revoked rev = (X509Revoked) revoked.entry(i); // TODO: can throw CCE
            BigInteger serial = new BigInteger( rev.callMethod(context, "serial").toString() );
            RubyTime t1 = (RubyTime) rev.callMethod(context, "time").callMethod(context, "getutc");
            t1.setMicroseconds(0);
            // TODO Extensions ignored, for now
            generator.addCRLEntry( serial, t1.getJavaDate(), new X509Extensions(new Hashtable()) );
        }

        try {
            for ( int i = 0; i < extensions.size(); i++ ) {
                Extension ext = (Extension) extensions.entry(i);
                generator.addExtension(ext.getRealOid(), ext.getRealCritical(), ext.getRealValueBytes());
            }
        }
        catch (IOException e) { throw newCRLError(runtime, e); }

        final PrivateKey privateKey = ((PKey) key).getPrivateKey();
        try {
            crl = generator.generate(privateKey);
        }
        catch (IllegalStateException e) {
            if ( isDebug(runtime) ) e.printStackTrace( runtime.getOut() );
            throw newCRLError(runtime, e);
        }
        catch (GeneralSecurityException e) {
            if ( isDebug(runtime) ) e.printStackTrace( runtime.getOut() );
            throw newCRLError(runtime, e.getMessage());
        }

        try {
            crl_v = new ASN1InputStream(new ByteArrayInputStream(crl.getEncoded())).readObject();
        }
        catch (CRLException e) { throw newCRLError(runtime, e); }
        catch (IOException e) { throw newCRLError(runtime, e); }

        ASN1Sequence v1 = (ASN1Sequence) ( ((ASN1Sequence) crl_v).getObjectAt(0) );
        final ASN1EncodableVector build1 = new ASN1EncodableVector();
        int copyIndex = 0;
        if ( v1.getObjectAt(0) instanceof ASN1Integer ) copyIndex++;
        build1.add( new ASN1Integer( new BigInteger(version.toString()) ) );
        while(copyIndex < v1.size()) {
            build1.add(v1.getObjectAt(copyIndex++));
        }
        final ASN1EncodableVector build2 = new ASN1EncodableVector();
        build2.add(new DLSequence(build1));
        build2.add(((ASN1Sequence)crl_v).getObjectAt(1));
        build2.add(((ASN1Sequence)crl_v).getObjectAt(2));
        crl_v = new DLSequence(build2);
        changed = false;
        return this;
    }

    @JRubyMethod
    public IRubyObject verify(final ThreadContext context, final IRubyObject key) {
        if ( changed ) return context.runtime.getFalse();
        final PublicKey publicKey = ((PKey) key).getPublicKey();
        try {
            boolean valid = SecurityHelper.verify(crl, publicKey, true);
            return context.runtime.newBoolean(valid);
        }
        catch (CRLException e) {
            return context.runtime.getFalse();
        }
        catch (InvalidKeyException e) {
            return context.runtime.getFalse();
        }
        catch (SignatureException e) {
            return context.runtime.getFalse();
        }
        catch (NoSuchAlgorithmException e) {
            return context.runtime.getFalse();
        }
    }

    private static RubyBoolean printExceptionAndGetFalse(final Ruby runtime, final Exception e) {
        if ( isDebug(runtime) ) e.printStackTrace( runtime.getOut() );
        return runtime.getFalse();
    }

    private static RubyClass _CRLError(final Ruby runtime) {
        return _X509(runtime).getClass("CertificateError");
    }

    static RaiseException newCRLError(Ruby runtime, Exception e) {
        return Utils.newError(runtime, _CRLError(runtime), e);
    }

    private static RaiseException newCRLError(Ruby runtime, String message) {
        return Utils.newError(runtime, _CRLError(runtime), message);
    }

}// X509CRL
