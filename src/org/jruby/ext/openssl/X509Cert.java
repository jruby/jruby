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

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyTime;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.openssl.x509store.PEM;
import org.jruby.ext.openssl.x509store.X509AuxCertificate;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509Cert extends RubyObject {
    private static ObjectAllocator X509CERT_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(IRuby runtime, RubyClass klass) {
            return new X509Cert(runtime, klass);
        }
    };
    
    public static void createX509Cert(IRuby runtime, RubyModule mX509) {
        RubyClass cX509Cert = mX509.defineClassUnder("Certificate",runtime.getObject(),X509CERT_ALLOCATOR);
        RubyClass openSSLError = runtime.getModule("OpenSSL").getClass("OpenSSLError");
        mX509.defineClassUnder("CertificateError",openSSLError,openSSLError.getAllocator());

        CallbackFactory certcb = runtime.callbackFactory(X509Cert.class);
        cX509Cert.defineMethod("initialize",certcb.getOptMethod("_initialize"));
        cX509Cert.defineFastMethod("initialize_copy",certcb.getFastMethod("initialize_copy",IRubyObject.class));
        cX509Cert.defineFastMethod("clone",certcb.getFastMethod("rbClone"));
        cX509Cert.defineFastMethod("to_der",certcb.getFastMethod("to_der"));
        cX509Cert.defineFastMethod("to_pem",certcb.getFastMethod("to_pem"));
        cX509Cert.defineFastMethod("to_s",certcb.getFastMethod("to_pem"));
        cX509Cert.defineFastMethod("to_text",certcb.getFastMethod("to_text"));
        cX509Cert.defineFastMethod("version",certcb.getFastMethod("version"));
        cX509Cert.defineFastMethod("version=",certcb.getFastMethod("set_version",IRubyObject.class));
        cX509Cert.defineFastMethod("signature_algorithm",certcb.getFastMethod("signature_algorithm"));
        cX509Cert.defineFastMethod("serial",certcb.getFastMethod("serial"));
        cX509Cert.defineFastMethod("serial=",certcb.getFastMethod("set_serial",IRubyObject.class));
        cX509Cert.defineFastMethod("subject",certcb.getFastMethod("subject"));
        cX509Cert.defineFastMethod("subject=",certcb.getFastMethod("set_subject",IRubyObject.class));
        cX509Cert.defineFastMethod("issuer",certcb.getFastMethod("issuer"));
        cX509Cert.defineFastMethod("issuer=",certcb.getFastMethod("set_issuer",IRubyObject.class));
        cX509Cert.defineFastMethod("not_before",certcb.getFastMethod("not_before"));
        cX509Cert.defineFastMethod("not_before=",certcb.getFastMethod("set_not_before",IRubyObject.class));
        cX509Cert.defineFastMethod("not_after",certcb.getFastMethod("not_after"));
        cX509Cert.defineFastMethod("not_after=",certcb.getFastMethod("set_not_after",IRubyObject.class));
        cX509Cert.defineFastMethod("public_key",certcb.getFastMethod("public_key"));
        cX509Cert.defineFastMethod("public_key=",certcb.getFastMethod("set_public_key",IRubyObject.class));
        cX509Cert.defineFastMethod("sign",certcb.getFastMethod("sign",IRubyObject.class,IRubyObject.class));
        cX509Cert.defineFastMethod("verify",certcb.getFastMethod("verify",IRubyObject.class));
        cX509Cert.defineFastMethod("check_private_key",certcb.getFastMethod("check_private_key",IRubyObject.class));
        cX509Cert.defineFastMethod("extensions",certcb.getFastMethod("extensions"));
        cX509Cert.defineFastMethod("extensions=",certcb.getFastMethod("set_extensions",IRubyObject.class));
        cX509Cert.defineFastMethod("add_extension",certcb.getFastMethod("add_extension",IRubyObject.class));
        cX509Cert.defineFastMethod("inspect",certcb.getFastMethod("inspect"));
    }

    public X509Cert(IRuby runtime, RubyClass type) {
        super(runtime,type);
    }

    private IRubyObject serial;
    private IRubyObject not_before;
    private IRubyObject not_after;
    private IRubyObject issuer;
    private IRubyObject subject;
    private IRubyObject public_key;

    private IRubyObject sig_alg;
    private IRubyObject version;

    private List extensions;

    private boolean changed = true;

    private X509V3CertificateGenerator generator = new X509V3CertificateGenerator();
    private X509Certificate cert;

    X509AuxCertificate getAuxCert() {
        if(null == cert) {
            return null;
        }
        if(cert instanceof X509AuxCertificate) {
            return (X509AuxCertificate)cert;
        }
        return new X509AuxCertificate(cert);
    }

    public static IRubyObject wrap(IRuby runtime, Certificate c) throws Exception {
        RubyClass cr = (RubyClass)(((RubyModule)(runtime.getModule("OpenSSL").getConstant("X509"))).getConstant("Certificate"));
        return cr.callMethod(runtime.getCurrentContext(),"new",runtime.newString(new String(c.getEncoded(),"ISO8859_1")));
    }

    public IRubyObject _initialize(IRubyObject[] args, Block unusedBlock) throws Exception {
        extensions = new ArrayList();
        if(checkArgumentCount(args,0,1) == 0) {
            return this;
        }
        ThreadContext tc = getRuntime().getCurrentContext();
        IRubyObject arg = OpenSSLImpl.to_der_if_possible(args[0]);
        ByteArrayInputStream bis = new ByteArrayInputStream(arg.toString().getBytes("PLAIN"));
        CertificateFactory cf = CertificateFactory.getInstance("X.509","BC");
        cert = (X509Certificate)cf.generateCertificate(bis);

        set_serial(RubyNumeric.str2inum(getRuntime(),getRuntime().newString(cert.getSerialNumber().toString()),10));
        set_not_before(RubyTime.newTime(getRuntime(),cert.getNotBefore().getTime()));
        set_not_after(RubyTime.newTime(getRuntime(),cert.getNotAfter().getTime()));
        set_subject(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getConstant("Name").callMethod(tc,"new",getRuntime().newString(new String(cert.getSubjectX500Principal().getEncoded(),"ISO8859_1"))));
        set_issuer(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getConstant("Name").callMethod(tc,"new",getRuntime().newString(new String(cert.getIssuerX500Principal().getEncoded(),"ISO8859_1"))));

        IRubyObject extFact = ((RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getConstant("ExtensionFactory"))).callMethod(tc,"new");
        extFact.callMethod(tc,"subject_certificate=",this);

        Set crit = cert.getCriticalExtensionOIDs();
        if(crit != null) {
            for(Iterator iter = crit.iterator();iter.hasNext();) {
                String critOid = (String)iter.next();
                byte[] value = cert.getExtensionValue(critOid);
                IRubyObject rValue = ASN1.decode(((RubyModule)(getRuntime().getModule("OpenSSL"))).getConstant("ASN1"),getRuntime().newString(new String(value,"PLAIN"))).callMethod(tc,"value");
                //                add_extension(extFact.callMethod("create_ext", new IRubyObject[]{getRuntime().newString(critOid),getRuntime().newString(Utils.toHex(rValue.toString().substring(2).getBytes("PLAIN"),':')),getRuntime().getTrue()}));
                if(critOid.equals("2.5.29.17")) {
                    add_extension(extFact.callMethod(tc,"create_ext", new IRubyObject[]{getRuntime().newString(critOid),getRuntime().newString(rValue.toString()),getRuntime().getTrue()}));
                } else {
                    add_extension(extFact.callMethod(tc,"create_ext", new IRubyObject[]{getRuntime().newString(critOid),getRuntime().newString(rValue.toString().substring(2)),getRuntime().getTrue()}));
                }
            }
        }

        Set ncrit = cert.getNonCriticalExtensionOIDs();
        if(ncrit != null) {
            for(Iterator iter = ncrit.iterator();iter.hasNext();) {
                String ncritOid = (String)iter.next();
                byte[] value = cert.getExtensionValue(ncritOid);
                IRubyObject rValue = ASN1.decode(((RubyModule)(getRuntime().getModule("OpenSSL"))).getConstant("ASN1"),getRuntime().newString(new String(value,"PLAIN"))).callMethod(tc,"value");
                //                add_extension(extFact.callMethod("create_ext", new IRubyObject[]{getRuntime().newString(ncritOid),getRuntime().newString(Utils.toHex(rValue.toString().substring(2).getBytes("PLAIN"),':')),getRuntime().getFalse()}));

                if(ncritOid.equals("2.5.29.17")) {
                    add_extension(extFact.callMethod(tc,"create_ext", new IRubyObject[]{getRuntime().newString(ncritOid),getRuntime().newString(rValue.toString()),getRuntime().getFalse()}));
                } else {
                    add_extension(extFact.callMethod(tc,"create_ext", new IRubyObject[]{getRuntime().newString(ncritOid),getRuntime().newString(rValue.toString().substring(2)),getRuntime().getFalse()}));
                }
            }
        }
        changed = false;

        return this;
    }

    public IRubyObject initialize_copy(IRubyObject obj) {
        if(this == obj) {
            return this;
        }
        checkFrozen();
        return this;
    }

    public IRubyObject to_der() throws Exception {
        return getRuntime().newString(new String(cert.getEncoded(),"ISO8859_1"));
    }

    public IRubyObject to_pem() throws Exception {
        StringWriter w = new StringWriter();
        PEM.write_X509(w,getAuxCert());
        w.close();
        return getRuntime().newString(w.toString());
    }

    public IRubyObject to_text() {
        return getRuntime().getNil();
    }

    public IRubyObject inspect() {
        return getRuntime().getNil();
    }

    public IRubyObject version() {
        return version;
    }

    public IRubyObject set_version(IRubyObject arg) {
        if(!arg.equals(this.version)) {
            changed = true;
        }
        this.version = arg;
        return arg;
    }

    public IRubyObject signature_algorithm() {
        return sig_alg;
    }

    public IRubyObject serial() {
        return serial;
    }

    public IRubyObject set_serial(IRubyObject num) {
        if(!num.equals(this.serial)) {
            changed = true;
        }
        serial = num;
        generator.setSerialNumber(new BigInteger(serial.toString()));
        return num;
    }

    public IRubyObject subject() {
        return subject;
    }

    public IRubyObject set_subject(IRubyObject arg) {
        if(!arg.equals(this.subject)) {
            changed = true;
        }
        subject = arg;
        generator.setSubjectDN(((X509Name)subject).getRealName());
        return arg;
    }

    public IRubyObject issuer() {
        return issuer;
    }

    public IRubyObject set_issuer(IRubyObject arg) {
        if(!arg.equals(this.issuer)) {
            changed = true;
        }
        issuer = arg;
        generator.setIssuerDN(((X509Name)issuer).getRealName());
        return arg;
    }

    public IRubyObject not_before() {
        return not_before;
    }

    public IRubyObject set_not_before(IRubyObject arg) {
        changed = true;
        not_before = arg.callMethod(getRuntime().getCurrentContext(),"getutc");
        ((RubyTime)not_before).setMicroseconds(0);
        generator.setNotBefore(((RubyTime)not_before).getJavaDate());
        return arg;
    }

    public IRubyObject not_after() {
        return not_after;
    }

    public IRubyObject set_not_after(IRubyObject arg) {
        changed = true;
        not_after = arg.callMethod(getRuntime().getCurrentContext(),"getutc");
        ((RubyTime)not_after).setMicroseconds(0);
        generator.setNotAfter(((RubyTime)not_after).getJavaDate());
        return arg;
    }

    public IRubyObject public_key() {
        return public_key;
    }

    public IRubyObject set_public_key(IRubyObject arg) {
        if(!arg.equals(this.public_key)) {
            changed = true;
        }
        public_key = arg;
        generator.setPublicKey(((PKey)public_key).getPublicKey());
        return arg;
    }

    public IRubyObject sign(IRubyObject key, IRubyObject digest) throws Exception {
        // Have to obey some artificial constraints of the OpenSSL implementation. Stupid.
        String keyAlg = ((PKey)key).getAlgorithm();
        String digAlg = ((Digest)digest).getAlgorithm();
        
        if(("DSA".equalsIgnoreCase(keyAlg) && "MD5".equalsIgnoreCase(digAlg)) || 
           ("RSA".equalsIgnoreCase(keyAlg) && "DSS1".equals(((Digest)digest).name().toString())) ||
           ("DSA".equalsIgnoreCase(keyAlg) && "SHA1".equals(((Digest)digest).name().toString()))) {
            throw new RaiseException(getRuntime(), (RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getConstant("CertificateError")), null, true);
        }

        for(Iterator iter = extensions.iterator();iter.hasNext();) {
            X509Extensions.Extension ag = (X509Extensions.Extension)iter.next();
            generator.addExtension(ag.getRealOid(),ag.getRealCritical(),ag.getRealValueBytes());
        }

        sig_alg = getRuntime().newString(digAlg);
        generator.setSignatureAlgorithm(digAlg + "WITH" + keyAlg);
        cert = generator.generateX509Certificate(((PKey)key).getPrivateKey());
        changed = false;
        return this;
    }

    public IRubyObject verify(IRubyObject key) throws Exception {
        if(changed) {
            return getRuntime().getFalse();
        }
        try {
            cert.verify(((PKey)key).getPublicKey());
            return getRuntime().getTrue();
        } catch(InvalidKeyException e) {
            return getRuntime().getFalse();
        }
    }

    public IRubyObject check_private_key(IRubyObject arg) {
        return getRuntime().getNil();
    }

    public IRubyObject extensions() {
        return getRuntime().newArray(extensions);
    }

    public IRubyObject set_extensions(IRubyObject arg) {
        extensions = ((RubyArray)arg).getList();
        return arg;
    }

    public IRubyObject add_extension(IRubyObject arg) throws Exception {
        changed = true;
        if(((X509Extensions.Extension)arg).getRealOid().equals(new DERObjectIdentifier("2.5.29.17"))) {
            boolean one = true;
            for(Iterator iter = extensions.iterator();iter.hasNext();) {
                X509Extensions.Extension ag = (X509Extensions.Extension)iter.next();
                if(ag.getRealOid().equals(new DERObjectIdentifier("2.5.29.17"))) {
                    GeneralName[] n1 = GeneralNames.getInstance(new ASN1InputStream(ag.getRealValueBytes()).readObject()).getNames();
                    GeneralName[] n2 = GeneralNames.getInstance(new ASN1InputStream(((X509Extensions.Extension)arg).getRealValueBytes()).readObject()).getNames();
                    ASN1EncodableVector v1 = new ASN1EncodableVector();

                    for(int i=0;i<n1.length;i++) {
                        v1.add(n1[i]);
                    }
                    for(int i=0;i<n2.length;i++) {
                        v1.add(n2[i]);
                    }
                    ag.setRealValue(new String(new GeneralNames(new DERSequence(v1)).getDEREncoded(),"ISO8859_1"));
                    one = false;
                    break;
                }
            }
            if(one) {
                extensions.add(arg);
            }
        } else {
            extensions.add(arg);
        }
        return arg;
    }

    public IRubyObject rbClone() {
        IRubyObject clone = new X509Cert(getRuntime(),getMetaClass().getRealClass());
        clone.setMetaClass(getMetaClass().getSingletonClassClone());
        clone.setTaint(this.isTaint());
        clone.initCopy(this);
        clone.setFrozen(isFrozen());
        return clone;
    }
}// X509Cert

