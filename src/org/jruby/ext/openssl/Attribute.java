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

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.jruby.IRuby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Attribute extends RubyObject {
    public static void createAttribute(IRuby runtime, RubyModule mX509) {
        RubyClass cAttribute = mX509.defineClassUnder("Attribute",runtime.getObject());

        mX509.defineClassUnder("AttributeError",runtime.getModule("OpenSSL").getClass("OpenSSLError"));

        CallbackFactory attrcb = runtime.callbackFactory(Attribute.class);

        cAttribute.defineSingletonMethod("new",attrcb.getOptSingletonMethod("newInstance"));
        cAttribute.defineMethod("initialize",attrcb.getOptMethod("_initialize"));
        cAttribute.defineMethod("to_der",attrcb.getMethod("to_der"));
        cAttribute.defineMethod("oid",attrcb.getMethod("oid"));
        cAttribute.defineMethod("oid=",attrcb.getMethod("set_oid",IRubyObject.class));
        cAttribute.defineMethod("value",attrcb.getMethod("value"));
        cAttribute.defineMethod("value=",attrcb.getMethod("set_value",IRubyObject.class));
    }

    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
        Attribute result = new Attribute(recv.getRuntime(), (RubyClass)recv);
        result.callInit(args);
        return result;
    }

    public Attribute(IRuby runtime, RubyClass type) {
        super(runtime,type);
    }

    private IRubyObject oid;
    private IRubyObject value;

    private DERObjectIdentifier getObjectIdentifier(String nameOrOid) {
        Object val1 = ASN1.getOIDLookup(getRuntime()).get(nameOrOid.toLowerCase());
        if(null != val1) {
            return (DERObjectIdentifier)val1;
        }
        DERObjectIdentifier val2 = new DERObjectIdentifier(nameOrOid);
        return val2;
    }

    DERObject toASN1() throws Exception {
        ASN1EncodableVector v1 = new ASN1EncodableVector();
        v1.add(getObjectIdentifier(oid.toString()));
        if(value instanceof ASN1.ASN1Constructive) {
            v1.add(((ASN1.ASN1Constructive)value).toASN1());
        } else {
            ASN1EncodableVector v2 = new ASN1EncodableVector();
            v2.add(((ASN1.ASN1Data)value).toASN1());
            v1.add(new DERSet(v2));
        }
        return new DERSequence(v1);
    }

    public IRubyObject _initialize(IRubyObject[] str) throws Exception {
        if(checkArgumentCount(str,1,2) == 1) {
            IRubyObject _oid = OpenSSLImpl.to_der_if_possible(str[0]);
            set_oid(_oid);
            return this;
        }
        set_oid(str[0]);
        set_value(str[1]);
        return this;
    }

    public IRubyObject to_der() {
        System.err.println("WARNING: unimplemented method called: attr#to_der");
        return getRuntime().getNil();
    }

    public IRubyObject oid() {
        return oid;
    }

    public IRubyObject set_oid(IRubyObject val) {
        this.oid = val;
        return val;
    }

    public IRubyObject value() {
        return value;
    }

    public IRubyObject set_value(IRubyObject val) throws Exception {
        IRubyObject tmp = OpenSSLImpl.to_der_if_possible(val);
        this.value = ASN1.decode(getRuntime().getModule("OpenSSL").getConstant("ASN1"),tmp);
        return val;
    }
}// Attribute
