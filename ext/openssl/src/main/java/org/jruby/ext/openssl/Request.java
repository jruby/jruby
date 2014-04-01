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

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.io.StringWriter;

import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.PrivateKey;

import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.openssl.x509store.PEMInputOutput;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Visibility;

import org.jruby.ext.openssl.impl.PKCS10Request;
import static org.jruby.ext.openssl.Utils.newRubyInstance;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Request extends RubyObject {
    private static final long serialVersionUID = -5551557929791764918L;

    private static ObjectAllocator REQUEST_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new Request(runtime, klass);
        }
    };

    public static void createRequest(Ruby runtime, RubyModule mX509) {
        RubyClass cRequest = mX509.defineClassUnder("Request",runtime.getObject(),REQUEST_ALLOCATOR);
        RubyClass openSSLError = runtime.getModule("OpenSSL").getClass("OpenSSLError");
        mX509.defineClassUnder("RequestError",openSSLError,openSSLError.getAllocator());

        cRequest.defineAnnotatedMethods(Request.class);
    }

    private IRubyObject subject;
    private IRubyObject public_key;

    private List<IRubyObject> attrs;

    private PKCS10Request req;

    public Request(Ruby runtime, RubyClass type) {
        super(runtime,type);
        attrs = new ArrayList<IRubyObject>();
        req = new PKCS10Request((X500Name) null, (PublicKey) null, null);
    }

    @JRubyMethod(name="initialize", rest=true, visibility = Visibility.PRIVATE)
    public IRubyObject _initialize(final ThreadContext context,
        final IRubyObject[] args, final Block block) {
        final Ruby runtime = context.runtime;

        if ( Arity.checkArgumentCount(runtime, args, 0, 1) == 0 ) return this;

        try {
          req = new PKCS10Request( OpenSSLImpl.readX509PEM(context, args[0]) );
        }
        catch (ArrayIndexOutOfBoundsException e) {
            throw newX509ReqError(runtime, "invalid certificate request data");
        }

        final String algorithm;
        final byte[] encoded;
        try {
            PublicKey pkey = req.getPublicKey();
            algorithm = pkey.getAlgorithm();
            encoded = pkey.getEncoded();
        }
        catch (IOException e) {
            throw newX509ReqError(runtime, e.getMessage());
        }

        final IRubyObject enc = RubyString.newString(runtime, encoded);
        if ( "RSA".equalsIgnoreCase(algorithm) ) {
            this.public_key = newRubyInstance(context, "OpenSSL::PKey::RSA", enc);
        }
        else if ( "DSA".equalsIgnoreCase(algorithm) ) {
            this.public_key = newRubyInstance(context, "OpenSSL::PKey::DSA", enc);
        }
        else {
            throw runtime.newLoadError("not implemented algo for public key: " + algorithm);
        }

        this.subject = makeRubyName( context, req.getSubject() );
        this.attrs = new ArrayList<IRubyObject>(req.getAttributes().length);

        try {
            for ( Attribute attr : req.getAttributes() ) {
                attrs.add( makeRubyAttr( context, attr.getAttrType(), attr.getAttrValues() ) );
            }
        } catch (IOException ex) {
            throw newX509ReqError(runtime, ex.getMessage());
        }

        return this;
    }

    private static IRubyObject makeRubyAttr(final ThreadContext context, ASN1ObjectIdentifier type, ASN1Set values)
        throws IOException {
        final Ruby runtime = context.runtime;
        IRubyObject rubyType = runtime.newString( ASN1.getSymLookup(runtime).get(type) );
        IRubyObject rubyValue = ASN1.decode(context,
            runtime.getClassFromPath("OpenSSL::ASN1"),
            RubyString.newString( runtime, ((ASN1Object) values).getEncoded() )
        );
        return newRubyInstance(context, "OpenSSL::X509::Attribute", rubyType, rubyValue);
    }

    private static IRubyObject makeRubyName(final ThreadContext context, X500Name name) {
        final Ruby runtime = context.runtime;
        if ( name == null ) return context.runtime.getNil();

        IRubyObject newName = newRubyInstance(context, "OpenSSL::X509::Name");

        for ( RDN rdn: name.getRDNs() ) {
            for ( AttributeTypeAndValue tv : rdn.getTypesAndValues() ) {
                ASN1ObjectIdentifier oid = tv.getType();
                String val = null;
                if ( tv.getValue() instanceof ASN1String ) {
                    val = ( (ASN1String) tv.getValue() ).getString();
                }
                RubyFixnum typef = runtime.newFixnum( ASN1.idForClass(tv.getValue().getClass()) ); //TODO correct?
                ((X509Name) newName).addEntry(oid, val, typef);
            }
        }

        return newName;
    }

    @Override
    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(IRubyObject obj) {
        System.err.println("WARNING: unimplemented method called: init_copy");
        if (this == obj) {
            return this;
        }
        checkFrozen();
        subject = getRuntime().getNil();
        public_key = getRuntime().getNil();
        return this;
    }

    @JRubyMethod(name={"to_pem","to_s"})
    public IRubyObject to_pem() {
        StringWriter w = new StringWriter();
        try {
            PEMInputOutput.writeX509Request(w, req);
            return getRuntime().newString(w.toString());
        } catch (IOException ex) {
            throw getRuntime().newIOErrorFromException(ex);
        }
        finally {
            try { w.close(); } catch( Exception e ) {}
        }
    }

    @JRubyMethod
    public IRubyObject to_der() {
        try {
            return RubyString.newString(getRuntime(), req.toASN1Structure().getEncoded());
        } catch (IOException ex) {
            throw getRuntime().newIOErrorFromException(ex);
        }
    }

    @JRubyMethod
    public IRubyObject to_text() {
        System.err.println("WARNING: unimplemented method called: to_text");
        return getRuntime().getNil();
    }

    @JRubyMethod
   public IRubyObject version() {
        return getRuntime().newFixnum(req.getVersion());
    }

    @JRubyMethod(name="version=")
    public IRubyObject set_version(IRubyObject val) {
        // NOTE: This is meaningless, it doesn't do anything...
        //System.err.println("WARNING: meaningless method called: version=");
        return val;
    }

    @JRubyMethod
    public IRubyObject subject() {
        return this.subject;
    }

    private static X500Name x500Name(IRubyObject name) {
        return ((X509Name) name).getX500Name();
    }

    @JRubyMethod(name="subject=")
    public IRubyObject set_subject(IRubyObject val) {
        if (val != subject) {
            req.setSubject( x500Name(val) );
            this.subject = val;
        }
        return val;
    }

    @JRubyMethod
    public IRubyObject signature_algorithm() {
        System.err.println("WARNING: unimplemented method called: signature_algorithm");
        return getRuntime().getNil();
    }

    @JRubyMethod
    public IRubyObject public_key() {
        return this.public_key;
    }

    @JRubyMethod(name="public_key=")
    public IRubyObject set_public_key(IRubyObject val) {
        if (val != subject) {
            req.setPublicKey( ((PKey) val).getPublicKey() );
            this.public_key = val;
        }
        return val;
    }

    @JRubyMethod
    public IRubyObject sign(final ThreadContext context,
        final IRubyObject key, final IRubyObject digest) {

        PublicKey publicKey = ((PKey) public_key).getPublicKey();
        PrivateKey privateKey = ((PKey) key).getPrivateKey();

        final String keyAlg = publicKey.getAlgorithm();
        final String digAlg = ((Digest)digest).getShortAlgorithm();
        final String digName = ((Digest)digest).name().toString();

        if (PKCS10Request.algorithmMismatch(keyAlg, digAlg, digName))
            throw newX509ReqError(context.runtime, null);

        // String sigAlgStr = digAlg + "WITH" + keyAlg;
        req = new PKCS10Request(x500Name(subject), publicKey, makeAttrList(attrs));

        try {
            req.sign(privateKey, digAlg);
        }
        catch(IOException e) {
            throw context.runtime.newIOErrorFromException(e);
        }

        return this;
    }

    private static List<Attribute> makeAttrList(final List<IRubyObject> list) {
        ArrayList<Attribute> realList = new ArrayList<Attribute>(list.size());
        for (IRubyObject attr : list) {
            realList.add( makeAttr(attr) );
        }
        return realList;
    }

    private static Attribute makeAttr(IRubyObject attr) {
        return Attribute.getInstance( ((org.jruby.ext.openssl.Attribute) attr).toASN1() );
    }

    @JRubyMethod
    public IRubyObject verify(final ThreadContext context, IRubyObject key) {
        PublicKey publicKey;
        try {
            publicKey = ( (PKey) key.callMethod(context, "public_key") ).getPublicKey();
            return req.verify(publicKey) ? context.runtime.getTrue() : context.runtime.getFalse();
        }
        catch (InvalidKeyException e) {
            throw newX509ReqError(context.runtime, e.getMessage());
        }
        catch(Exception e) {
            return context.runtime.getFalse();
        }
    }

    @JRubyMethod
    public IRubyObject attributes() {
        return getRuntime().newArray(attrs);
    }

    @SuppressWarnings("unchecked")
    @JRubyMethod(name="attributes=")
    public IRubyObject set_attributes(IRubyObject val) {
        attrs.clear();
        attrs.addAll(((RubyArray)val).getList());

        if (req != null) {
            req.setAttributes( makeAttrList(attrs) );
        }
        return val;
    }

    @JRubyMethod
    public IRubyObject add_attribute(IRubyObject val) {
        attrs.add(val);

        if (req != null) {
            req.addAttribute( makeAttr(val) );
        }
        return val;
    }

    private static RaiseException newX509ReqError(Ruby runtime, String message) {
        return Utils.newError(runtime, "OpenSSL::X509::RequestError", message);
    }
}// Request
