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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyClass;
import org.jruby.RubyFile;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import org.jruby.ext.openssl.impl.ASN1Registry;
import org.jruby.ext.openssl.impl.BIO;
import org.jruby.ext.openssl.impl.CipherSpec;
import org.jruby.ext.openssl.impl.MemBIO;
import org.jruby.ext.openssl.impl.Mime;
import org.jruby.ext.openssl.impl.NotVerifiedPKCS7Exception;
import org.jruby.ext.openssl.impl.PKCS7Exception;
import org.jruby.ext.openssl.impl.RecipInfo;
import org.jruby.ext.openssl.impl.SMIME;
import org.jruby.ext.openssl.impl.SignerInfoWithPkey;
import org.jruby.ext.openssl.x509store.PEMInputOutput;
import org.jruby.ext.openssl.x509store.Store;
import org.jruby.ext.openssl.x509store.X509AuxCertificate;

import static org.jruby.ext.openssl.OpenSSLReal.isDebug;
import static org.jruby.ext.openssl.OpenSSLReal.warn;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class PKCS7 extends RubyObject {
    private static final long serialVersionUID = -3925104500966826973L;

    private static ObjectAllocator PKCS7_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new PKCS7(runtime, klass);
        }
    };

    public static void createPKCS7(Ruby runtime, RubyModule mOSSL) {
        RubyClass _PKCS7 = mOSSL.defineClassUnder("PKCS7", runtime.getObject(), PKCS7_ALLOCATOR);
        RubyClass _OpenSSLError = runtime.getModule("OpenSSL").getClass("OpenSSLError");
        _PKCS7.defineClassUnder("PKCS7Error", _OpenSSLError, _OpenSSLError.getAllocator());
        _PKCS7.addReadWriteAttribute(runtime.getCurrentContext(), "data");
        _PKCS7.addReadWriteAttribute(runtime.getCurrentContext(), "error_string");
        _PKCS7.defineAnnotatedMethods(PKCS7.class);

        SignerInfo.createSignerInfo(runtime, _PKCS7);
        RecipientInfo.createRecipientInfo(runtime, _PKCS7);

        _PKCS7.setConstant("TEXT", runtime.newFixnum(1));
        _PKCS7.setConstant("NOCERTS", runtime.newFixnum(2));
        _PKCS7.setConstant("NOSIGS", runtime.newFixnum(4));
        _PKCS7.setConstant("NOCHAIN", runtime.newFixnum(8));
        _PKCS7.setConstant("NOINTERN", runtime.newFixnum(16));
        _PKCS7.setConstant("NOVERIFY", runtime.newFixnum(32));
        _PKCS7.setConstant("DETACHED", runtime.newFixnum(64));
        _PKCS7.setConstant("BINARY", runtime.newFixnum(128));
        _PKCS7.setConstant("NOATTR", runtime.newFixnum(256));
        _PKCS7.setConstant("NOSMIMECAP", runtime.newFixnum(512));
    }

    public static BIO obj2bio(IRubyObject obj) {
        if ( obj instanceof RubyFile ) {
            throw obj.getRuntime().newNotImplementedError("TODO: handle RubyFile correctly");
//     if (TYPE(obj) == T_FILE) {
//         OpenFile *fptr;
//         GetOpenFile(obj, fptr);
//         rb_io_check_readable(fptr);
//         bio = BIO_new_fp(fptr->f, BIO_NOCLOSE);
        }
        else {
            final ByteList str = obj.asString().getByteList();
            return BIO.memBuf(str.getUnsafeBytes(), str.getBegin(), str.getRealSize());
        }
    }

    @Deprecated // no loger used
    public static PKCS7 wrap(RubyClass klass, org.jruby.ext.openssl.impl.PKCS7 p7) {
        PKCS7 wrapped = new PKCS7(klass.getRuntime(), klass);
        wrapped.p7 = p7;
        return wrapped;
    }

    private static PKCS7 wrap(final Ruby runtime, org.jruby.ext.openssl.impl.PKCS7 p7) {
        PKCS7 wrapped = new PKCS7(runtime, _PKCS7(runtime));
        wrapped.p7 = p7;
        return wrapped;
    }

    public static IRubyObject membio2str(Ruby runtime, BIO bio) {
        return runtime.newString( new ByteList(((MemBIO) bio).getMemCopy(), false) );
    }

    private static List<X509AuxCertificate> getAuxCerts(final IRubyObject arg) {
        final RubyArray arr = (RubyArray) arg;
        List<X509AuxCertificate> certs = new ArrayList<X509AuxCertificate>(arr.size());
        for ( int i = 0; i<arr.size(); i++ ) {
            certs.add( ((X509Cert) arr.eltInternal(i)).getAuxCert() );
        }
        return certs;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject read_smime(IRubyObject self, IRubyObject arg) {
        final Ruby runtime = self.getRuntime();
        final BIO in = obj2bio(arg);
        final BIO[] out = new BIO[]{ null };
        org.jruby.ext.openssl.impl.PKCS7 pkcs7Impl = null;
        try {
            pkcs7Impl = new SMIME(Mime.DEFAULT).readPKCS7(in, out);
        }
        catch (IOException ioe) {
            throw newPKCS7Error(runtime, ioe.getMessage());
        }
        catch (PKCS7Exception pkcs7e) {
            throw newPKCS7Error(runtime, pkcs7e);
        }
        if ( pkcs7Impl == null ) {
            throw newPKCS7Error(runtime, (String) null);
        }
        IRubyObject data = out[0] != null ? membio2str(runtime, out[0]) : runtime.getNil();
        final PKCS7 pkcs7 = wrap(runtime, pkcs7Impl);
        pkcs7.setData(data);
        return pkcs7;
    }

    @JRubyMethod(meta = true, rest = true)
    public static IRubyObject write_smime(IRubyObject self, IRubyObject[] args) {
        final Ruby runtime = self.getRuntime();

        final PKCS7 pkcs7;
        IRubyObject data = runtime.getNil();
        IRubyObject flags = runtime.getNil();

        switch ( Arity.checkArgumentCount(runtime, args, 1, 3) ) {
            case 3: flags = args[2];
            case 2: data = args[1];
            default: pkcs7 = (PKCS7) args[0];
        }

        final int flg = flags.isNil() ? 0 : RubyNumeric.fix2int(flags);

        String smime = "";
        try {
            smime = new SMIME().writePKCS7(pkcs7.p7, data.asJavaString(), flg);
        }
        catch (PKCS7Exception e) {
            throw newPKCS7Error(runtime, e);
        }
        catch (IOException e) {
            throw newPKCS7Error(runtime, e.getMessage());
        }

        return RubyString.newString(runtime, smime);
    }

    @JRubyMethod(meta = true, rest = true)
    public static IRubyObject sign(IRubyObject self, IRubyObject[] args) {
        final Ruby runtime = self.getRuntime();

        final X509Cert cert; final PKey key; final IRubyObject data;
        IRubyObject certs = runtime.getNil();
        IRubyObject flags = runtime.getNil();

        switch ( Arity.checkArgumentCount(runtime, args, 3, 5) ) {
            case 5: flags = args[4];
            case 4: certs = args[3];
            default:
                cert = (X509Cert) args[0];
                key = (PKey) args[1];
                data = args[2];
        }

        X509AuxCertificate auxCert = cert.getAuxCert();
        PrivateKey privKey = key.getPrivateKey();
        final int flg = flags.isNil() ? 0 : RubyNumeric.fix2int(flags);
        final BIO dataBIO = obj2bio(data);
        List<X509AuxCertificate> auxCerts = certs.isNil() ? null : getAuxCerts(certs);

        org.jruby.ext.openssl.impl.PKCS7 pkcs7Impl;
        try {
            pkcs7Impl = org.jruby.ext.openssl.impl.PKCS7.sign(auxCert, privKey, auxCerts, dataBIO, flg);
        }
        catch (PKCS7Exception e) {
            throw newPKCS7Error(runtime, e);
        }
        final PKCS7 pkcs7 = wrap(runtime, pkcs7Impl);
        pkcs7.setData(data);
        return pkcs7;
    }

    /** ossl_pkcs7_s_encrypt
     *
     */
    @JRubyMethod(meta = true, rest = true)
    public static IRubyObject encrypt(IRubyObject self, IRubyObject[] args) {
        final Ruby runtime = self.getRuntime();

        IRubyObject certs, data, cipher = runtime.getNil(), flags = runtime.getNil();

        switch ( Arity.checkArgumentCount(self.getRuntime(), args, 2, 4) ) {
            case 4: flags = args[3];
            case 3: cipher = args[2];
        }
        data = args[1]; certs = args[0];

        CipherSpec cipherSpec = null;
        if ( cipher.isNil() ) {
            try {
                javax.crypto.Cipher c = SecurityHelper.getCipher("RC2/CBC/PKCS5Padding");
                cipherSpec = new CipherSpec(c, Cipher.Algorithm.jsseToOssl("RC2/CBC/PKCS5Padding", 40), 40);
            }
            catch (GeneralSecurityException e) {
                throw newPKCS7Error(runtime, e.getMessage());
            }
        } else {
            Cipher c = ((Cipher) cipher);
            cipherSpec = new CipherSpec(c.getCipher(), c.getName(), c.getGenerateKeyLen() * 8);
        }
        final int flg = flags.isNil() ? 0 : RubyNumeric.fix2int(flags);
        final List<X509AuxCertificate> auxCerts = getAuxCerts(certs);
        final byte[] dataBytes = data.asString().getBytes();

        org.jruby.ext.openssl.impl.PKCS7 pkcs7Impl;
        try {
            pkcs7Impl = org.jruby.ext.openssl.impl.PKCS7.encrypt(auxCerts, dataBytes, cipherSpec, flg);
        }
        catch (PKCS7Exception pkcs7e) {
            throw newPKCS7Error(self.getRuntime(), pkcs7e);
        }
        final PKCS7 pkcs7 = wrap(runtime, pkcs7Impl);
        pkcs7.setData(data);
        return pkcs7;
    }

    public PKCS7(Ruby runtime, RubyClass type) {
        super(runtime,type);
    }

    private org.jruby.ext.openssl.impl.PKCS7 p7;

    @JRubyMethod(name="initialize", rest=true, visibility = Visibility.PRIVATE)
    public IRubyObject _initialize(final ThreadContext context, IRubyObject[] args) {
        if ( Arity.checkArgumentCount(getRuntime(), args, 0, 1) == 0 ) {
            p7 = new org.jruby.ext.openssl.impl.PKCS7();
            try {
                p7.setType(ASN1Registry.NID_undef);
            }
            catch (PKCS7Exception e) {
                throw newPKCS7Error(getRuntime(), e);
            }
            return this;
        }
        IRubyObject arg = OpenSSLImpl.to_der_if_possible(context, args[0]);
        BIO input = obj2bio(arg);
        try {
            p7 = org.jruby.ext.openssl.impl.PKCS7.readPEM(input);
            if (p7 == null) {
                input.reset();
                p7 = org.jruby.ext.openssl.impl.PKCS7.fromASN1(input);
            }
        }
        catch (IOException ioe) {
            throw newPKCS7Error(getRuntime(), ioe.getMessage());
        }
        catch (PKCS7Exception pkcs7e) {
            throw newPKCS7Error(getRuntime(), pkcs7e);
        }
        setData( getRuntime().getNil() );
        return this;
    }

    @Override
    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(IRubyObject obj) {
        warn(getRuntime().getCurrentContext(), "WARNING: unimplemented method called: PKCS7#initialize_copy");
        return this;
    }

    @JRubyMethod(name="type=")
    public IRubyObject set_type(IRubyObject type) {
        final String typeStr = type.toString(); // likely a Symbol

        int typeId = ASN1Registry.NID_undef;
        if ("signed".equals(typeStr)) {
            typeId = ASN1Registry.NID_pkcs7_signed;
        } else if ("data".equals(typeStr)) {
            typeId = ASN1Registry.NID_pkcs7_data;
        } else if ("signedAndEnveloped".equals(typeStr)) {
            typeId = ASN1Registry.NID_pkcs7_signedAndEnveloped;
        } else if ("enveloped".equals(typeStr)) {
            typeId = ASN1Registry.NID_pkcs7_enveloped;
        } else if ("encrypted".equals(typeStr)) {
            typeId = ASN1Registry.NID_pkcs7_encrypted;
        }

        try {
            p7.setType(typeId);
        }
        catch (PKCS7Exception pkcs7e) {
            throw newPKCS7Error(getRuntime(), pkcs7e);
        }
        return type;
    }

    @JRubyMethod(name="type")
    public IRubyObject get_type() {
        if(p7.isSigned()) {
            return getRuntime().newSymbol("signed");
        }
        if(p7.isEncrypted()) {
            return getRuntime().newSymbol("encrypted");
        }
        if(p7.isEnveloped()) {
            return getRuntime().newSymbol("enveloped");
        }
        if(p7.isSignedAndEnveloped()) {
            return getRuntime().newSymbol("signedAndEnveloped");
        }
        if(p7.isData()) {
            return getRuntime().newSymbol("data");
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "detached")
    public IRubyObject detached() {
        warn(getRuntime().getCurrentContext(), "WARNING: unimplemented method called: PKCS7#detached");
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "detached=")
    public IRubyObject set_detached(IRubyObject obj) {
        warn(getRuntime().getCurrentContext(), "WARNING: unimplemented method called: PKCS7#detached=");
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "detached?")
    public IRubyObject detached_p() {
        warn(getRuntime().getCurrentContext(), "WARNING: unimplemented method called: PKCS7#detached?");
        return getRuntime().getNil();
    }

    @JRubyMethod(name="cipher=")
    public IRubyObject set_cipher(IRubyObject obj) {
        warn(getRuntime().getCurrentContext(), "WARNING: unimplemented method called: PKCS7#cipher=");
        return getRuntime().getNil();
    }

    @JRubyMethod
    public IRubyObject add_signer(IRubyObject obj) {
        SignerInfoWithPkey p7si = ((SignerInfo)obj).getSignerInfo().dup();

        try {
            p7.addSigner(p7si);
        } catch (PKCS7Exception pkcse) {
            throw newPKCS7Error(getRuntime(), pkcse);
        }
        // TODO: Handle exception here

        if(p7.isSigned()) {
            p7si.addSignedAttribute(ASN1Registry.NID_pkcs9_contentType, ASN1Registry.nid2obj(ASN1Registry.NID_pkcs7_data));
        }

        return this;
    }

    /** ossl_pkcs7_get_signer
     *
     * This seems to return a list of SignerInfo objects.
     *
     */
    @JRubyMethod
    public IRubyObject signers() {
        Collection<SignerInfoWithPkey> sk = p7.getSignerInfo();
        RubyArray ary = getRuntime().newArray(sk.size());
        for(SignerInfoWithPkey si : sk) {
            ary.append(SignerInfo.create(getRuntime(), si));
        }
        return ary;
    }

    @JRubyMethod
    public IRubyObject add_recipient(IRubyObject obj) {
        warn(getRuntime().getCurrentContext(), "WARNING: unimplemented method called: PKCS7#add_recipient");
        return getRuntime().getNil();
    }

    @JRubyMethod
    public IRubyObject recipients() {
        Collection<RecipInfo> sk;

        if(p7.isEnveloped()) {
            sk = p7.getEnveloped().getRecipientInfo();
        } else if(p7.isSignedAndEnveloped()) {
            sk = p7.getSignedAndEnveloped().getRecipientInfo();
        } else {
            sk = null;
        }
        if(sk == null) {
            return getRuntime().newArray();
        }

        RubyArray ary = getRuntime().newArray(sk.size());
        for(RecipInfo ri : sk) {
            ary.append(RecipientInfo.create(getRuntime(), ri));
        }
        return ary;
    }

    @JRubyMethod
    public IRubyObject add_certificate(IRubyObject obj) {
        try {
            p7.addCertificate(((X509Cert)obj).getAuxCert());
        } catch (PKCS7Exception pkcse) {
            throw newPKCS7Error(getRuntime(), pkcse);
        }
        return this;
    }

    @JRubyMethod(name="certificates=")
    public IRubyObject set_certificates(IRubyObject obj) {
        warn(getRuntime().getCurrentContext(), "WARNING: unimplemented method called: PKCS7#certificates=");
        return getRuntime().getNil();
    }

    private Collection<X509AuxCertificate> getCertificates() {
        Collection<X509AuxCertificate> certs;
        int i = p7.getType();
        switch(i) {
        case ASN1Registry.NID_pkcs7_signed:
            certs = p7.getSign().getCert();
            break;
        case ASN1Registry.NID_pkcs7_signedAndEnveloped:
            certs = p7.getSignedAndEnveloped().getCert();
            break;
        default:
            certs = new HashSet<X509AuxCertificate>();
            break;
        }
        return certs;
    }

    private RubyArray certsToArray(Collection<X509AuxCertificate> certs) throws CertificateEncodingException {
        RubyArray ary = getRuntime().newArray(certs.size());
        for(X509AuxCertificate x509 : certs) {
            ary.append(X509Cert.wrap(getRuntime(), x509));
        }
        return ary;
    }

    @JRubyMethod
    public IRubyObject certificates() {
        try {
            return certsToArray(getCertificates());
        } catch (CertificateEncodingException cee) {
            throw newPKCS7Error(getRuntime(), cee.getMessage());
        }
    }

    @JRubyMethod
    public IRubyObject add_crl(IRubyObject obj) {
        warn(getRuntime().getCurrentContext(), "WARNING: unimplemented method called: PKCS7#add_crl");
        return getRuntime().getNil();
    }

    @JRubyMethod(name="crls=")
    public IRubyObject set_crls(IRubyObject obj) {
        warn(getRuntime().getCurrentContext(), "WARNING: unimplemented method called: PKCS7#crls=");
        return getRuntime().getNil();
    }

    @JRubyMethod
    public IRubyObject crls() {
        warn(getRuntime().getCurrentContext(), "WARNING: unimplemented method called: PKCS7#crls");
        return getRuntime().getNil();
    }

    @JRubyMethod(name={"add_data", "data="})
    public IRubyObject add_data(IRubyObject obj) {
        if (p7.isSigned()) {
            try {
                p7.contentNew(ASN1Registry.NID_pkcs7_data);
            } catch (PKCS7Exception pkcs7e) {
                throw newPKCS7Error(getRuntime(), pkcs7e);
            }
        }

        BIO in = obj2bio(obj);
        BIO out = null;
        try {
            out = p7.dataInit(null);
        } catch (PKCS7Exception pkcs7e) {
            throw newPKCS7Error(getRuntime(), pkcs7e);
        }
        byte[] buf = new byte[4096];
        for(;;) {
            try {
                int i = in.read(buf, 0, buf.length);
                if(i <= 0) {
                    break;
                }
                if(out != null) {
                    out.write(buf, 0, i);
                }
            } catch(IOException e) {
                throw getRuntime().newIOErrorFromException(e);
            }
        }

        try {
            p7.dataFinal(out);
        } catch (PKCS7Exception pkcs7e) {
            throw newPKCS7Error(getRuntime(), pkcs7e);
        }
        setData(getRuntime().getNil());

        return obj;
    }

    @JRubyMethod(rest=true)
    public IRubyObject verify(IRubyObject[] args) {
        final Ruby runtime = getRuntime();

        IRubyObject certs; X509Store store;
        IRubyObject indata = runtime.getNil();
        IRubyObject vflags = runtime.getNil();

        switch ( Arity.checkArgumentCount(runtime, args, 2, 4) ) {
            case 4: vflags = args[3];
            case 3: indata = args[2];
            default: store = (X509Store) args[1]; certs = args[0];
        }
        final int flg = vflags.isNil() ? 0 : RubyNumeric.fix2int(vflags);

        if ( indata.isNil() ) indata = getData();

        final BIO in = indata.isNil() ? null : obj2bio(indata);

        List<X509AuxCertificate> x509s = certs.isNil() ? null : getAuxCerts(certs);

        final Store storeStr = store.getStore();
        final BIO out = BIO.mem();

        boolean result = false;
        try {
            p7.verify(x509s, storeStr, in, out, flg);
            result = true;
        }
        catch (NotVerifiedPKCS7Exception e) {
            // result = false;
        }
        catch (PKCS7Exception pkcs7e) {
            if ( isDebug(runtime) ) {
                // runtime.getOut().println(pkcs7e);
                pkcs7e.printStackTrace(runtime.getOut());
            }
            // result = false;
        }

        IRubyObject data = membio2str(getRuntime(), out);
        setData(data);

        return result ? runtime.getTrue() : runtime.getFalse();
    }

    @JRubyMethod(rest=true)
    public IRubyObject decrypt(IRubyObject[] args) {
        IRubyObject dflags;
        if ( Arity.checkArgumentCount(getRuntime(), args, 2, 3) == 3 ) {
            dflags = args[2];
        }
        else {
            dflags = getRuntime().getNil();
        }
        PKey pkey = (PKey) args[0];
        X509Cert cert = (X509Cert) args[1];

        final PrivateKey privKey = pkey.getPrivateKey();
        final X509AuxCertificate auxCert = cert.getAuxCert();
        final int flg = dflags.isNil() ? 0 : RubyNumeric.fix2int(dflags);

        final BIO out = BIO.mem();
        try {
            p7.decrypt(privKey, auxCert, out, flg);
        }
        catch (PKCS7Exception pkcs7e) {
            throw newPKCS7Error(getRuntime(), pkcs7e);
        }
        return membio2str(getRuntime(), out);
    }

    @JRubyMethod(name = {"to_pem", "to_s"})
    public IRubyObject to_pem() {
        StringWriter writer = new StringWriter();
        try {
            PEMInputOutput.writePKCS7(writer, p7.toASN1());
            return getRuntime().newString( writer.toString() );
        }
        catch (IOException e) {
            throw getRuntime().newIOErrorFromException(e);
        }
    }

    @JRubyMethod
    public IRubyObject to_der() {
        try {
            return getRuntime().newString(new ByteList(p7.toASN1(), false));
        }
        catch (IOException e) {
            throw newPKCS7Error(getRuntime(), e.getMessage());
        }
    }

    public void setData(IRubyObject object) {
        setInstanceVariable("@data", object);
    }

    public IRubyObject getData() {
        return getInstanceVariable("@data");
    }

    public static class SignerInfo extends RubyObject {

        private static final long serialVersionUID = -3799397032272738848L;

        private static ObjectAllocator SIGNERINFO_ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new SignerInfo(runtime, klass);
            }
        };

        public static void createSignerInfo(final Ruby runtime, final RubyModule _PKCS7) {
            RubyClass _SignerInfo = _PKCS7.defineClassUnder("SignerInfo", runtime.getObject(), SIGNERINFO_ALLOCATOR);
            _PKCS7.defineConstant("Signer",_SignerInfo);
            _SignerInfo.defineAnnotatedMethods(SignerInfo.class);
        }

        private static RubyClass _SignerInfo(final Ruby runtime) {
            return _PKCS7(runtime).getClass("SignerInfo");
        }

        public static SignerInfo create(Ruby runtime, SignerInfoWithPkey info) {
            SignerInfo instance = new SignerInfo(runtime, _SignerInfo(runtime));
            instance.info = info;
            return instance;
        }

        public SignerInfo(Ruby runtime, RubyClass type) {
            super(runtime,type);
        }

        private SignerInfoWithPkey info;

        SignerInfoWithPkey getSignerInfo() {
            return info;
        }

        @JRubyMethod(visibility = Visibility.PRIVATE)
        public IRubyObject initialize(final ThreadContext context,
            IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
            warn(context, "WARNING: unimplemented method called: signerInfo#initialize");
            return this;
        }


        @JRubyMethod(name={"issuer","name"})
        public IRubyObject issuer() {
            return X509Name.create(getRuntime(), info.getIssuerAndSerialNumber().getName());
        }

        @JRubyMethod
        public IRubyObject serial() {
            return RubyBignum.bignorm(getRuntime(), info.getIssuerAndSerialNumber().getCertificateSerialNumber().getValue());
        }

        @JRubyMethod
        public IRubyObject signed_time(final ThreadContext context) {
            warn(context, "WARNING: unimplemented method called: signerInfo#signed_time");
            return context.runtime.getNil();
        }
    }

    public static class RecipientInfo extends RubyObject {

        private static final long serialVersionUID = 6977793206950149902L;

        private static ObjectAllocator RECIPIENTINFO_ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new RecipientInfo(runtime, klass);
            }
        };

        public static void createRecipientInfo(final Ruby runtime, final RubyModule _PKCS7) {
            RubyClass _Recipient = _PKCS7.defineClassUnder("RecipientInfo", runtime.getObject(), RECIPIENTINFO_ALLOCATOR);
            _Recipient.defineAnnotatedMethods(RecipientInfo.class);
        }

        private static RubyClass _RecipientInfo(final Ruby runtime) {
            return _PKCS7(runtime).getClass("RecipientInfo");
        }

        public RecipientInfo(Ruby runtime, RubyClass type) {
            super(runtime, type);
        }

        public static RecipientInfo create(Ruby runtime, RecipInfo info) {
            RecipientInfo instance = new RecipientInfo(runtime, _RecipientInfo(runtime));
            instance.info = info;
            return instance;
        }

        private RecipInfo info;

        @JRubyMethod(visibility = Visibility.PRIVATE)
        public IRubyObject initialize(final ThreadContext context, IRubyObject arg) {
            warn(context, "WARNING: unimplemented method called: recipientInfo#initialize");
            return this;
        }

        @JRubyMethod
        public IRubyObject issuer() {
            return X509Name.create(getRuntime(), info.getIssuerAndSerial().getName());
        }

        @JRubyMethod
        public IRubyObject serial() {
            return RubyBignum.bignorm(getRuntime(), info.getIssuerAndSerial().getCertificateSerialNumber().getValue());
        }

        @JRubyMethod
        public IRubyObject enc_key(final ThreadContext context) {
            warn(context, "WARNING: unimplemented method called: recipientInfo#enc_key");
            return context.runtime.getNil();
        }
    }

    private static RaiseException newPKCS7Error(Ruby ruby, PKCS7Exception e) {
        return newPKCS7Error(ruby, e.getMessage());
    }

    private static RaiseException newPKCS7Error(Ruby runtime, String message) {
        return Utils.newError(runtime, _PKCS7(runtime).getClass("PKCS7Error"), message);
    }

    static RubyClass _PKCS7(final Ruby runtime) {
        return (RubyClass) runtime.getModule("OpenSSL").getConstant("PKCS7");
    }

}// PKCS7
