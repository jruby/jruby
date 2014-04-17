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
import static org.jruby.ext.openssl.ASN1._ASN1;
import static org.jruby.ext.openssl.Attribute._Attribute;
import static org.jruby.ext.openssl.OpenSSLReal.warn;
import static org.jruby.ext.openssl.PKey._PKey;
import static org.jruby.ext.openssl.X509._X509;
import static org.jruby.ext.openssl.X509Name._Name;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Request extends RubyObject {
    private static final long serialVersionUID = -2886532636278901502L;

    private static ObjectAllocator REQUEST_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new Request(runtime, klass);
        }
    };

    public static void createRequest(final Ruby runtime, final RubyModule _X509) {
        RubyClass _Request = _X509.defineClassUnder("Request", runtime.getObject(), REQUEST_ALLOCATOR);
        RubyClass _OpenSSLError = runtime.getModule("OpenSSL").getClass("OpenSSLError");
        _X509.defineClassUnder("RequestError", _OpenSSLError, _OpenSSLError.getAllocator());
        _Request.defineAnnotatedMethods(Request.class);
    }

    private IRubyObject subject;
    private IRubyObject public_key;

    private final List<org.jruby.ext.openssl.Attribute> attributes;

    private PKCS10Request request;

    public Request(Ruby runtime, RubyClass type) {
        super(runtime, type);
        attributes = new ArrayList<org.jruby.ext.openssl.Attribute>();
        request = new PKCS10Request((X500Name) null, (PublicKey) null, null);
    }

    @JRubyMethod(name = "initialize", rest = true, visibility = Visibility.PRIVATE)
    public IRubyObject _initialize(final ThreadContext context,
        final IRubyObject[] args, final Block block) {
        final Ruby runtime = context.runtime;

        if ( Arity.checkArgumentCount(runtime, args, 0, 1) == 0 ) return this;

        try {
            request = new PKCS10Request( OpenSSLImpl.readX509PEM(context, args[0]) );
        }
        catch (ArrayIndexOutOfBoundsException e) {
            throw newRequestError(runtime, "invalid certificate request data");
        }

        final String algorithm;
        final byte[] encoded;
        try {
            PublicKey pkey = request.getPublicKey();
            algorithm = pkey.getAlgorithm();
            encoded = pkey.getEncoded();
        }
        catch (IOException e) {
            throw newRequestError(runtime, e.getMessage());
        }

        final RubyString enc = RubyString.newString(runtime, encoded);
        if ( "RSA".equalsIgnoreCase(algorithm) ) {
            this.public_key = newPKeyImplInstance(context, "RSA", enc);
        }
        else if ( "DSA".equalsIgnoreCase(algorithm) ) {
            this.public_key = newPKeyImplInstance(context, "DSA", enc);
        }
        else {
            throw runtime.newLoadError("not implemented algo for public key: " + algorithm);
        }

        this.subject = newX509Name( context, request.getSubject() );

        try {
            for ( Attribute attr : request.getAttributes() ) {
                attributes.add( (org.jruby.ext.openssl.Attribute)
                    newX509Attribute( context, attr.getAttrType(), attr.getAttrValues() )
                );
            }
        }
        catch (IOException ex) {
            throw newRequestError(runtime, ex.getMessage());
        }

        return this;
    }

    private static IRubyObject newPKeyImplInstance(final ThreadContext context,
        final String className, final RubyString encoded) { // OpenSSL::PKey::RSA.new(encoded)
        return _PKey(context.runtime).getClass(className).callMethod(context, "new", encoded);
    }

    private static IRubyObject newX509Attribute(final ThreadContext context,
        final ASN1ObjectIdentifier type, final ASN1Set values) throws IOException {
        final Ruby runtime = context.runtime;
        IRubyObject attrType = runtime.newString( ASN1.getSymLookup(runtime).get(type) );
        IRubyObject attrValue = ASN1.decode(context, _ASN1(runtime),
            RubyString.newString( runtime, ((ASN1Object) values).getEncoded() )
        );
        return _Attribute(runtime).callMethod(context, "new", new IRubyObject[] { attrType, attrValue });
    }

    private static IRubyObject newX509Name(final ThreadContext context, X500Name name) {
        final Ruby runtime = context.runtime;
        if ( name == null ) return context.runtime.getNil();

        final X509Name newName = (X509Name) _Name(runtime).callMethod(context, "new");

        for ( RDN rdn: name.getRDNs() ) {
            for ( AttributeTypeAndValue tv : rdn.getTypesAndValues() ) {
                ASN1ObjectIdentifier oid = tv.getType();
                String val = null;
                if ( tv.getValue() instanceof ASN1String ) {
                    val = ( (ASN1String) tv.getValue() ).getString();
                }
                RubyFixnum typef = runtime.newFixnum( ASN1.idForClass(tv.getValue().getClass()) ); //TODO correct?
                newName.addEntry(oid, val, typef);
            }
        }

        return newName;
    }

    @Override
    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(IRubyObject obj) {
        final Ruby runtime = getRuntime();
        warn(runtime.getCurrentContext(), "WARNING: unimplemented method called: request#initialize_copy");

        if ( this == obj ) return this;

        checkFrozen();
        subject = public_key = runtime.getNil();
        return this;
    }

    @JRubyMethod(name={"to_pem","to_s"})
    public IRubyObject to_pem() {
        StringWriter writer = new StringWriter();
        try {
            PEMInputOutput.writeX509Request(writer, request);
            return getRuntime().newString( writer.toString() );
        }
        catch (IOException e) {
            throw Utils.newIOError(getRuntime(), e);
        }
    }

    @JRubyMethod
    public IRubyObject to_der() {
        try {
            return RubyString.newString(getRuntime(), request.toASN1Structure().getEncoded());
        }
        catch (IOException ex) {
            throw getRuntime().newIOErrorFromException(ex);
        }
    }

    @JRubyMethod
    public IRubyObject to_text(final ThreadContext context) {
        warn(context, "WARNING: unimplemented method called: request#to_text");
        return context.runtime.getNil();
    }

    @JRubyMethod
   public IRubyObject version() {
        return getRuntime().newFixnum(request.getVersion());
    }

    @JRubyMethod(name="version=")
    public IRubyObject set_version(final ThreadContext context, IRubyObject val) {
        // NOTE: This is meaningless, it doesn't do anything...
        // warn(context, "WARNING: meaningless method called: request#version=");
        return context.runtime.getNil();
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
            request.setSubject( x500Name(val) );
            this.subject = val;
        }
        return val;
    }

    @JRubyMethod
    public IRubyObject signature_algorithm(final ThreadContext context) {
        warn(context, "WARNING: unimplemented method called: request#signature_algorithm");
        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject public_key() {
        return this.public_key;
    }

    @JRubyMethod(name="public_key=")
    public IRubyObject set_public_key(final IRubyObject pkey) {
        if (pkey != subject) {
            request.setPublicKey( ((PKey) pkey).getPublicKey() );
            this.public_key = pkey;
        }
        return pkey;
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
            throw newRequestError(context.runtime, null);

        // String sigAlgStr = digAlg + "WITH" + keyAlg;
        request = new PKCS10Request(x500Name(subject), publicKey, newAttributes(attributes));

        try {
            request.sign(privateKey, digAlg);
        }
        catch(IOException e) {
            throw context.runtime.newIOErrorFromException(e);
        }

        return this;
    }

    private static List<Attribute> newAttributes(final List<org.jruby.ext.openssl.Attribute> attributes) {
        ArrayList<Attribute> attrs = new ArrayList<Attribute>(attributes.size());
        for (org.jruby.ext.openssl.Attribute attribute : attributes) {
            attrs.add( newAttribute(attribute) );
        }
        return attrs;
    }

    private static Attribute newAttribute(final IRubyObject attribute) {
        return Attribute.getInstance( ((org.jruby.ext.openssl.Attribute) attribute).toASN1() );
    }

    @JRubyMethod
    public IRubyObject verify(final ThreadContext context, IRubyObject key) {
        PublicKey publicKey;
        try {
            publicKey = ( (PKey) key.callMethod(context, "public_key") ).getPublicKey();
            return request.verify(publicKey) ? context.runtime.getTrue() : context.runtime.getFalse();
        }
        catch (InvalidKeyException e) {
            throw newRequestError(context.runtime, e.getMessage());
        }
        catch(Exception e) {
            return context.runtime.getFalse();
        }
    }

    @JRubyMethod
    public IRubyObject attributes() {
        @SuppressWarnings("unchecked")
        List<IRubyObject> attributes = (List) this.attributes;
        return getRuntime().newArray(attributes);
    }

    @JRubyMethod(name="attributes=")
    public IRubyObject set_attributes(final IRubyObject attributes) {
        this.attributes.clear();
        final RubyArray attrs = (RubyArray) attributes;
        for ( int i = 0; i < attrs.size(); i++ ) {
            this.attributes.add( (org.jruby.ext.openssl.Attribute) attrs.entry(i) );
        }
        //if (request != null) {
            request.setAttributes( newAttributes(this.attributes) );
        //}
        return attributes;
    }

    @JRubyMethod
    public IRubyObject add_attribute(final IRubyObject attribute) {
        attributes.add( (org.jruby.ext.openssl.Attribute) attribute );
        //if (request != null) {
            request.addAttribute( newAttribute(attribute) );
        //}
        return attribute;
    }

    private static RaiseException newRequestError(Ruby runtime, String message) {
        return Utils.newError(runtime, _X509(runtime).getClass("RequestError"), message);
    }

}// Request
