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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.DERSet;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Visibility;

import static org.jruby.ext.openssl.OpenSSLReal.warn;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Attribute extends RubyObject {
    private static final long serialVersionUID = 5569940260019783275L;

    private static ObjectAllocator ATTRIBUTE_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new Attribute(runtime, klass);
        }
    };

    public static void createAttribute(Ruby runtime, RubyModule _X509) {
        RubyClass _Attribute = _X509.defineClassUnder("Attribute", runtime.getObject(), ATTRIBUTE_ALLOCATOR);

        RubyClass openSSLError = runtime.getModule("OpenSSL").getClass("OpenSSLError");
        _X509.defineClassUnder("AttributeError", openSSLError, openSSLError.getAllocator());

        _Attribute.defineAnnotatedMethods(Attribute.class);
    }


    static RubyClass _Attribute(final Ruby runtime) {
        RubyModule _X509 = (RubyModule) runtime.getModule("OpenSSL").getConstant("X509");
        return _X509.getClass("Attribute");
    }

    public Attribute(Ruby runtime, RubyClass type) {
        super(runtime,type);
    }

    private IRubyObject oid;
    private IRubyObject value;

    private ASN1ObjectIdentifier getObjectIdentifier(final String nameOrOid) {
        return ASN1.getObjectIdentifier(getRuntime(), nameOrOid);
    }

    ASN1Primitive toASN1() {
        ASN1EncodableVector v1 = new ASN1EncodableVector();
        v1.add(getObjectIdentifier(oid.toString()));
        if(value instanceof ASN1.ASN1Constructive) {
            final ThreadContext context = getRuntime().getCurrentContext();
            v1.add( ((ASN1.ASN1Constructive) value).toASN1(context) );
        } else {
            final ThreadContext context = getRuntime().getCurrentContext();
            ASN1EncodableVector v2 = new ASN1EncodableVector();
            v2.add( ((ASN1.ASN1Data) value).toASN1(context) );
            v1.add( new DERSet(v2) );
        }
        return new DLSequence(v1);
    }

    @JRubyMethod(name="initialize", required=1, optional=1, visibility = Visibility.PRIVATE)
    public IRubyObject _initialize(final ThreadContext context, IRubyObject[] str) {
        if ( Arity.checkArgumentCount(context.runtime, str, 1, 2) == 1 ) {
            set_oid( OpenSSLImpl.to_der_if_possible(context, str[0]) );
            return this;
        }
        set_oid(str[0]);
        set_value(context, str[1]);
        return this;
    }

    @JRubyMethod
    public IRubyObject to_der(final ThreadContext context) {
        warn(context, "WARNING: unimplemented method called: attribute#to_der");
        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject oid() {
        return oid;
    }

    @JRubyMethod(name="oid=")
    public IRubyObject set_oid(final IRubyObject oid) {
        return this.oid = oid;
    }

    @JRubyMethod
    public IRubyObject value() {
        return value;
    }

    @JRubyMethod(name="value=")
    public IRubyObject set_value(final ThreadContext context, IRubyObject val) {
        try {
            return this.value = ASN1.decodeImpl(context, val);
        }
        catch (IOException e) {
            throw Utils.newIOError(context.runtime, e);
        }
        catch (IllegalArgumentException e) {
            throw context.runtime.newArgumentError(e.getMessage());
        }
    }

}// Attribute
