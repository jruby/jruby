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
import java.security.cert.CertificateFactory;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERBoolean;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.x509.X509V2CRLGenerator;
import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyTime;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.openssl.x509store.PEM;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509CRL extends RubyObject {
    public static void createX509CRL(IRuby runtime, RubyModule mX509) {
        RubyClass cX509CRL = mX509.defineClassUnder("CRL",runtime.getObject());
        mX509.defineClassUnder("CRLError",runtime.getModule("OpenSSL").getClass("OpenSSLError"));

        CallbackFactory crlcb = runtime.callbackFactory(X509CRL.class);

        cX509CRL.defineSingletonMethod("new",crlcb.getOptSingletonMethod("newInstance"));
        cX509CRL.defineMethod("initialize",crlcb.getOptMethod("_initialize"));
        cX509CRL.defineMethod("initialize_copy",crlcb.getMethod("initialize_copy",IRubyObject.class));
        cX509CRL.defineMethod("clone",crlcb.getMethod("rbClone"));

        cX509CRL.defineMethod("version",crlcb.getMethod("version"));
        cX509CRL.defineMethod("version=",crlcb.getMethod("set_version",IRubyObject.class));
        cX509CRL.defineMethod("signature_algorithm",crlcb.getMethod("signature_algorithm"));
        cX509CRL.defineMethod("issuer",crlcb.getMethod("issuer"));
        cX509CRL.defineMethod("issuer=",crlcb.getMethod("set_issuer",IRubyObject.class));
        cX509CRL.defineMethod("last_update",crlcb.getMethod("last_update"));
        cX509CRL.defineMethod("last_update=",crlcb.getMethod("set_last_update",IRubyObject.class));
        cX509CRL.defineMethod("next_update",crlcb.getMethod("next_update"));
        cX509CRL.defineMethod("next_update=",crlcb.getMethod("set_next_update",IRubyObject.class));
        cX509CRL.defineMethod("revoked",crlcb.getMethod("revoked"));
        cX509CRL.defineMethod("revoked=",crlcb.getMethod("set_revoked",IRubyObject.class));
        cX509CRL.defineMethod("add_revoked",crlcb.getMethod("add_revoked",IRubyObject.class));

        cX509CRL.defineMethod("sign",crlcb.getMethod("sign",IRubyObject.class,IRubyObject.class));
        cX509CRL.defineMethod("verify",crlcb.getMethod("verify",IRubyObject.class));

        cX509CRL.defineMethod("to_der",crlcb.getMethod("to_der"));
        cX509CRL.defineMethod("to_pem",crlcb.getMethod("to_pem"));
        cX509CRL.defineMethod("to_s",crlcb.getMethod("to_pem")); 
        cX509CRL.defineMethod("to_text",crlcb.getMethod("to_text"));
        cX509CRL.defineMethod("extensions",crlcb.getMethod("extensions"));
        cX509CRL.defineMethod("extensions=",crlcb.getMethod("set_extensions",IRubyObject.class));
        cX509CRL.defineMethod("add_extension",crlcb.getMethod("add_extension",IRubyObject.class));
    }

    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
        IRubyObject result = new X509CRL(recv.getRuntime(), (RubyClass)recv);
        result.callInit(args);
        return result;
    }

    private IRubyObject version;
    private IRubyObject issuer;
    private IRubyObject last_update;
    private IRubyObject next_update;
    private IRubyObject revoked;
    private List extensions;

    private IRubyObject sig_alg;

    private boolean changed = true;

    private X509V2CRLGenerator generator = new X509V2CRLGenerator();
    private java.security.cert.X509CRL crl;

    private DERObject crl_v;

    java.security.cert.X509CRL getCRL() {
        return crl;
    }

    public X509CRL(IRuby runtime, RubyClass type) {
        super(runtime,type);
    }

    public IRubyObject _initialize(IRubyObject[] args) throws Exception {
        //        System.err.println("WARNING: unimplemented method called: CRL#initialize");
        extensions = new ArrayList();
        if(checkArgumentCount(args,0,1) == 0) {
            version = getRuntime().getNil();
            issuer = getRuntime().getNil();
            last_update = getRuntime().getNil();
            next_update = getRuntime().getNil();
            revoked = getRuntime().newArray();
            return this;
        }
        
        ByteArrayInputStream bis = new ByteArrayInputStream(args[0].toString().getBytes("PLAIN"));
        CertificateFactory cf = CertificateFactory.getInstance("X.509","BC");
        crl = (java.security.cert.X509CRL)cf.generateCRL(bis);
        crl_v = new ASN1InputStream(new ByteArrayInputStream(args[0].toString().getBytes("PLAIN"))).readObject();
        DEREncodable v0 = ((DERSequence)(((DERSequence)crl_v).getObjectAt(0))).getObjectAt(0);
        if(v0 instanceof DERInteger) {
            set_version(getRuntime().newFixnum(((DERInteger)v0).getValue().intValue()));
        } else {
            set_version(getRuntime().newFixnum(2));
        }
        set_last_update(RubyTime.newTime(getRuntime(),crl.getThisUpdate().getTime()));
        set_next_update(RubyTime.newTime(getRuntime(),crl.getNextUpdate().getTime()));
        ThreadContext tc = getRuntime().getCurrentContext();
        set_issuer(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getConstant("Name").callMethod(tc,"new",getRuntime().newString(new String(crl.getIssuerX500Principal().getEncoded(),"PLAIN"))));

        revoked = getRuntime().newArray();

        DERSequence seqa = (DERSequence)((DERSequence)crl_v).getObjectAt(0);
        DERObject maybe_ext = (DERObject)seqa.getObjectAt(seqa.size()-1);
        if(maybe_ext instanceof DERTaggedObject && ((DERTaggedObject)maybe_ext).getTagNo() == 0) {
            DERSequence exts = (DERSequence)((DERTaggedObject)maybe_ext).getObject();
            for(int i=0;i<exts.size();i++) {
                DERSequence seq2 = (DERSequence)exts.getObjectAt(i);
                boolean critical = false;
                String oid = ((DERObjectIdentifier)seq2.getObjectAt(0)).getId();
                if(seq2.getObjectAt(1) == DERBoolean.TRUE) {
                    critical = true;
                }
                byte[] value = crl.getExtensionValue(oid);
                IRubyObject mASN1 = ((RubyModule)(getRuntime().getModule("OpenSSL"))).getConstant("ASN1");
                IRubyObject rValue = null;
                try {
                    rValue = ASN1.decode(mASN1,ASN1.decode(mASN1,getRuntime().newString(new String(value,"PLAIN"))).callMethod(tc,"value"));
                } catch(Exception e) {
                    rValue = getRuntime().newString(new String(value,"PLAIN"));
                }
                X509Extensions.Extension ext1 = (X509Extensions.Extension)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getConstant("Extension").callMethod(tc,"new"));
                ext1.setRealOid(ext1.getObjectIdentifier(oid));
                ext1.setRealValue(rValue);
                ext1.setRealCritical(critical);
                add_extension(ext1);
            }
        }

        changed = false;
        return this;
    }

    public IRubyObject initialize_copy(IRubyObject obj) {
        System.err.println("WARNING: unimplemented method called: CRL#init_copy");
        if(this == obj) {
            return this;
        }
        checkFrozen();
        return this;
    }

    public IRubyObject to_pem() throws Exception {
        StringWriter w = new StringWriter();
        PEM.write_X509_CRL(w,crl);
        w.close();
        return getRuntime().newString(w.toString());
    }

    public IRubyObject to_der() throws Exception {
        return getRuntime().newString(new String(crl_v.getEncoded(),"ISO8859_1"));
    }

    private static final String IND8 = "        ";
    private static final String IND12 = "            ";
    private static final String IND16 = "                ";
    private static final DateFormat ASN_DATE = new SimpleDateFormat("MMM dd HH:mm:ss yyyy zzz");
    public IRubyObject to_text() throws Exception {
        StringBuffer sbe = new StringBuffer();
        sbe.append("Certificate Revocation List (CRL):\n");
        sbe.append(IND8).append("Version ").append(RubyNumeric.fix2int(version)+1).append(" (0x");
        sbe.append(Integer.toString(RubyNumeric.fix2int(version),16)).append(")\n");
        sbe.append(IND8).append("Signature Algorithm: ").append(ASN1.nid2ln(getRuntime(),ASN1.obj2nid(getRuntime(),((DERObjectIdentifier)((DERSequence)((DERSequence)crl_v).getObjectAt(1)).getObjectAt(0))))).append("\n");
        sbe.append(IND8).append("Issuer: ").append(issuer()).append("\n");
        sbe.append(IND8).append("Last Update: ").append(ASN_DATE.format(((RubyTime)last_update()).getJavaDate())).append("\n");
        if(!next_update().isNil()) {
            sbe.append(IND8).append("Next Update: ").append(ASN_DATE.format(((RubyTime)next_update()).getJavaDate())).append("\n");
        } else {
            sbe.append(IND8).append("Next Update: NONE\n");
        }
        if(extensions.size()>0) {
            sbe.append(IND8).append("CRL extensions\n");
            for(Iterator iter = extensions.iterator();iter.hasNext();) {
                X509Extensions.Extension ext = (X509Extensions.Extension)iter.next();
                DERObjectIdentifier oiden = ext.getRealOid();
                sbe.append(IND12).append(ASN1.o2a(getRuntime(),oiden)).append(": ");
                if(ext.getRealCritical()) {
                    sbe.append("critical");
                }
                sbe.append("\n");
                sbe.append(IND16).append(ext.value()).append("\n");
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
        return getRuntime().newString(sbe.toString());
    }

    public IRubyObject version() {
        return this.version;
    }

    public IRubyObject set_version(IRubyObject val) {
        if(!val.equals(this.version)) {
            changed = true;
        }
        this.version = val;
        return val;
    }

    public IRubyObject signature_algorithm() {
        return sig_alg;
    }

    public IRubyObject issuer() {
        return this.issuer;
    }

    public IRubyObject set_issuer(IRubyObject val) {
        if(!val.equals(this.issuer)) {
            changed = true;
        }
        this.issuer = val;
        generator.setIssuerDN(((X509Name)issuer).getRealName());
        return val;
    }

    public IRubyObject last_update() {
        return this.last_update;
    }

    public IRubyObject set_last_update(IRubyObject val) {
        changed = true;
        last_update = val.callMethod(getRuntime().getCurrentContext(),"getutc");
        ((RubyTime)last_update).setMicroseconds(0);
        generator.setThisUpdate(((RubyTime)last_update).getJavaDate());
        this.last_update = val;
        return val;
    }

    public IRubyObject next_update() {
        return this.next_update;
    }

    public IRubyObject set_next_update(IRubyObject val) {
        changed = true;
        next_update = val.callMethod(getRuntime().getCurrentContext(),"getutc");
        ((RubyTime)next_update).setMicroseconds(0);
        generator.setNextUpdate(((RubyTime)next_update).getJavaDate());
        this.next_update = val;
        return val;
    }

    public IRubyObject revoked() {
        return this.revoked;
    }

    public IRubyObject set_revoked(IRubyObject val) {
        changed = true;
        this.revoked = val;
        return val;
    }

    public IRubyObject add_revoked(IRubyObject val) {
        changed = true;
        this.revoked.callMethod(getRuntime().getCurrentContext(),"<<",val);
        return val;
    }

    public IRubyObject extensions() {
        return getRuntime().newArray(this.extensions);
    }

    public IRubyObject set_extensions(IRubyObject val) {
        this.extensions = ((RubyArray)val).getList();
        return val;
    }

    public IRubyObject add_extension(IRubyObject val) {
        this.extensions.add(val);
        return val;
    }

    public IRubyObject sign(IRubyObject key, IRubyObject digest) throws Exception {
        //System.err.println("WARNING: unimplemented method called: CRL#sign");
        // Have to obey some artificial constraints of the OpenSSL implementation. Stupid.
        String keyAlg = ((PKey)key).getAlgorithm();
        String digAlg = ((Digest)digest).getAlgorithm();
        
        if(("DSA".equalsIgnoreCase(keyAlg) && "MD5".equalsIgnoreCase(digAlg)) || 
           ("RSA".equalsIgnoreCase(keyAlg) && "DSS1".equals(((Digest)digest).name().toString())) ||
           ("DSA".equalsIgnoreCase(keyAlg) && "SHA1".equals(((Digest)digest).name().toString()))) {
            throw new RaiseException(getRuntime(), (RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getConstant("CRLError")), null, true);
        }

        sig_alg = getRuntime().newString(digAlg);
        generator.setSignatureAlgorithm(digAlg + "WITH" + keyAlg);

        for(Iterator iter = ((RubyArray)revoked).getList().iterator();iter.hasNext();) {
            X509Revoked rev = (X509Revoked)iter.next();
            BigInteger serial = new BigInteger(rev.callMethod(getRuntime().getCurrentContext(),"serial").toString());
            IRubyObject t1 = rev.callMethod(getRuntime().getCurrentContext(),"time").callMethod(getRuntime().getCurrentContext(),"getutc");
            ((RubyTime)t1).setMicroseconds(0);
            // Extensions ignored, for now
            generator.addCRLEntry(serial,((RubyTime)t1).getJavaDate(),new org.bouncycastle.asn1.x509.X509Extensions(new Hashtable()));
        }

        for(Iterator iter = extensions.iterator();iter.hasNext();) {
            Object arg = iter.next();
            generator.addExtension(((X509Extensions.Extension)arg).getRealOid(),((X509Extensions.Extension)arg).getRealCritical(),((X509Extensions.Extension)arg).getRealValueBytes());
        }

        crl = generator.generateX509CRL(((PKey)key).getPrivateKey());
        crl_v = new ASN1InputStream(new ByteArrayInputStream(crl.getEncoded())).readObject();
        DERSequence v1 = (DERSequence)(((DERSequence)crl_v).getObjectAt(0));
        ASN1EncodableVector build1 = new ASN1EncodableVector();
        int copyIndex = 0;
        if(v1.getObjectAt(0) instanceof DERInteger) {
            copyIndex++;
        }
        build1.add(new DERInteger(new java.math.BigInteger(version.toString())));
        while(copyIndex < v1.size()) {
            build1.add(v1.getObjectAt(copyIndex++));
        }
        ASN1EncodableVector build2 = new ASN1EncodableVector();
        build2.add(new DERSequence(build1));
        build2.add(((DERSequence)crl_v).getObjectAt(1));
        build2.add(((DERSequence)crl_v).getObjectAt(2));
        crl_v = new DERSequence(build2);
        changed = false;
        return this;
    }

    public IRubyObject verify(IRubyObject key) {
        if(changed) {
            return getRuntime().getFalse();
        }
        try {
            crl.verify(((PKey)key).getPublicKey());
            return getRuntime().getTrue();
        } catch(Exception e) {
            return getRuntime().getFalse();
        }
    }

    public IRubyObject rbClone() {
        IRubyObject clone = new X509CRL(getRuntime(),getMetaClass().getRealClass());
        clone.setMetaClass(getMetaClass().getSingletonClassClone());
        clone.setTaint(this.isTaint());
        clone.initCopy(this);
        clone.setFrozen(isFrozen());
        return clone;
    }
}// X509CRL
