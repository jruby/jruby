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
 * Copyright (C) 2006, 2007 Ola Bini <ola@ologix.com>
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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERString;
import org.bouncycastle.asn1.DERTags;
import org.bouncycastle.asn1.x509.X509DefaultEntryConverter;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.openssl.x509store.Name;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
@SuppressWarnings("deprecation")
public class X509Name extends RubyObject {
    private static final long serialVersionUID = -226196051911335103L;

    private static ObjectAllocator X509NAME_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new X509Name(runtime, klass);
        }
    };
    
    public static void createX509Name(Ruby runtime, RubyModule mX509) {
        RubyClass cX509Name = mX509.defineClassUnder("Name",runtime.getObject(),X509NAME_ALLOCATOR);
        RubyClass openSSLError = runtime.getModule("OpenSSL").getClass("OpenSSLError");
        mX509.defineClassUnder("NameError",openSSLError,openSSLError.getAllocator());

        cX509Name.defineAnnotatedMethods(X509Name.class);

        cX509Name.setConstant("COMPAT",runtime.newFixnum(COMPAT));
        cX509Name.setConstant("RFC2253",runtime.newFixnum(RFC2253));
        cX509Name.setConstant("ONELINE",runtime.newFixnum(ONELINE));
        cX509Name.setConstant("MULTILINE",runtime.newFixnum(MULTILINE));

        cX509Name.setConstant("DEFAULT_OBJECT_TYPE",runtime.newFixnum(DERTags.UTF8_STRING));

        RubyHash hash = new RubyHash(runtime, runtime.newFixnum(DERTags.UTF8_STRING));
        hash.op_aset(runtime.getCurrentContext(), runtime.newString("C"),runtime.newFixnum(DERTags.PRINTABLE_STRING));
        hash.op_aset(runtime.getCurrentContext(), runtime.newString("countryName"),runtime.newFixnum(DERTags.PRINTABLE_STRING));
        hash.op_aset(runtime.getCurrentContext(), runtime.newString("serialNumber"),runtime.newFixnum(DERTags.PRINTABLE_STRING));
        hash.op_aset(runtime.getCurrentContext(), runtime.newString("dnQualifier"),runtime.newFixnum(DERTags.PRINTABLE_STRING));
        hash.op_aset(runtime.getCurrentContext(), runtime.newString("DC"),runtime.newFixnum(DERTags.IA5_STRING));
        hash.op_aset(runtime.getCurrentContext(), runtime.newString("domainComponent"),runtime.newFixnum(DERTags.IA5_STRING));
        hash.op_aset(runtime.getCurrentContext(), runtime.newString("emailAddress"),runtime.newFixnum(DERTags.IA5_STRING));
        cX509Name.setConstant("OBJECT_TYPE_TEMPLATE", hash);
    }

    public static final int COMPAT = 0;
    public static final int RFC2253 = 17892119;
    public static final int ONELINE = 8520479;
    public static final int MULTILINE = 44302342;

    public X509Name(Ruby runtime, RubyClass type) {
        super(runtime,type);
        oids = new ArrayList<Object>();
        values = new ArrayList<Object>();
        types = new ArrayList<Object>();
    }

    private List<Object> oids;
    private List<Object> values;
    private List<Object> types;

    void addEntry(Object oid, Object value, Object type) {
        oids.add(oid);
        values.add(value);
        types.add(type);
    }
    
    public static X509Name create(Ruby runtime, org.bouncycastle.asn1.x509.X509Name realName) {
        X509Name name = new X509Name(runtime, Utils.getClassFromPath(runtime, "OpenSSL::X509::Name"));
        name.fromASN1Sequence((ASN1Sequence)realName.getDERObject());
        return name;
    }

    void fromASN1Sequence(ASN1Sequence seq) {
        oids = new ArrayList<Object>();
        values = new ArrayList<Object>();
        types = new ArrayList<Object>();
        for (Enumeration enumRdn = seq.getObjects(); enumRdn.hasMoreElements();) {
            ASN1Set rdn = (ASN1Set) enumRdn.nextElement();
            for (Enumeration enumTypeAndValue = rdn.getObjects(); enumTypeAndValue.hasMoreElements();) {
                ASN1Sequence typeAndValue = (ASN1Sequence) enumTypeAndValue.nextElement();
                oids.add(typeAndValue.getObjectAt(0));
                if (typeAndValue.getObjectAt(1) instanceof DERString) {
                    values.add(((DERString) typeAndValue.getObjectAt(1)).getString());
                } else {
                    values.add(null);
                }
                types.add(getRuntime().newFixnum(ASN1.idForClass(typeAndValue.getObjectAt(1).getClass())));
            }
        }
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context) {
        return this;
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject str_or_dn) {
        return initialize(
                context,
                str_or_dn,
                context.nil);
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject dn, IRubyObject template) {
        Ruby runtime = context.runtime;

        if(dn instanceof RubyArray) {
            RubyArray ary = (RubyArray)dn;

            if(template.isNil()) {
                template = runtime.getClassFromPath("OpenSSL::X509::Name").getConstant("OBJECT_TYPE_TEMPLATE");
            }

            for (int i = 0; i < ary.size(); i++) {
                IRubyObject obj = ary.eltOk(i);

                if (!(obj instanceof RubyArray)) {
                    throw runtime.newTypeError(obj, runtime.getArray());
                }

                RubyArray arr = (RubyArray)obj;

                IRubyObject entry0, entry1, entry2;
                entry0 = arr.size() > 0 ? arr.eltOk(0) : context.nil;
                entry1 = arr.size() > 1 ? arr.eltOk(1) : context.nil;
                entry2 = arr.size() > 2 ? arr.eltOk(2) : context.nil;

                if (entry2.isNil()) entry2 = template.callMethod(context, "[]", entry0);
                if (entry2.isNil()) entry2 = runtime.getClassFromPath("OpenSSL::X509::Name").getConstant("DEFAULT_OBJECT_TYPE");

                add_entry(context, entry0, entry1, entry2);
            }
        } else {
            try {
                byte[] bytes = OpenSSLImpl.to_der_if_possible(dn).convertToString().getBytes();
                ASN1InputStream asn1IS = new ASN1InputStream(bytes);
                fromASN1Sequence((ASN1Sequence)asn1IS.readObject());
            } catch(Exception e) {
                throw newX509NameError(runtime, e.getLocalizedMessage());
            }
        }
        return this;
    }

    /*
    private void printASN(org.bouncycastle.asn1.DERObject obj) {
        printASN(obj,0);
    }
    private void printASN(org.bouncycastle.asn1.DERObject obj, int indent) {
        if(obj instanceof org.bouncycastle.asn1.ASN1Sequence) {
            for(int i=0;i<indent;i++) {
                System.err.print(" ");
            }
            System.err.println("- Sequence:");
            for(java.util.Enumeration enm = ((org.bouncycastle.asn1.ASN1Sequence)obj).getObjects();enm.hasMoreElements();) {
                printASN((org.bouncycastle.asn1.DERObject)enm.nextElement(),indent+1);
            }
        } else if(obj instanceof org.bouncycastle.asn1.ASN1Set) {
            for(int i=0;i<indent;i++) {
                System.err.print(" ");
            }
            System.err.println("- Set:");
            for(java.util.Enumeration enm = ((org.bouncycastle.asn1.ASN1Set)obj).getObjects();enm.hasMoreElements();) {
                printASN((org.bouncycastle.asn1.DERObject)enm.nextElement(),indent+1);
            }
        } else {
            for(int i=0;i<indent;i++) {
                System.err.print(" ");
            }
            if(obj instanceof org.bouncycastle.asn1.DERString) {
                System.err.println("- " + obj + "=" + ((org.bouncycastle.asn1.DERString)obj).getString() + "[" + obj.getClass() + "]");
            } else {
                System.err.println("- " + obj + "[" + obj.getClass() + "]");
            }
        }
    }
    */

    private DERObjectIdentifier getObjectIdentifier(String nameOrOid) {
        Object val1 = ASN1.getOIDLookup(getRuntime()).get(nameOrOid.toLowerCase());
        if(null != val1) {
            return (DERObjectIdentifier)val1;
        }
        DERObjectIdentifier val2 = new DERObjectIdentifier(nameOrOid);
        return val2;
    }

    @JRubyMethod
    public IRubyObject add_entry(ThreadContext context, IRubyObject oid, IRubyObject value) {
        return add_entry(context, oid, value, context.nil);
    }

    @JRubyMethod
    public IRubyObject add_entry(ThreadContext context, IRubyObject _oid, IRubyObject _value, IRubyObject _type) {
        Ruby runtime = context.runtime;

        String oid = _oid.convertToString().toString();
        String value = _value.convertToString().toString();
        IRubyObject type = !_type.isNil() ? _type : runtime.getClassFromPath("OpenSSL::X509::Name").getConstant("OBJECT_TYPE_TEMPLATE").callMethod(context, "[]", _oid);

        DERObjectIdentifier oid_v;
        try {
            oid_v = getObjectIdentifier(oid);
        } catch (IllegalArgumentException e) {
            throw newX509NameError(getRuntime(), "invalid field name: " + e.getMessage());
        }

        if (null == oid_v) {
            throw newX509NameError(getRuntime(), null);
        }

        oids.add(oid_v);
        values.add(value);
        types.add(type);

        return this;
    }

    @JRubyMethod(name="to_s", rest=true)
    public IRubyObject _to_s(IRubyObject[] args) {
        /*
Should follow parameters like this: 
if 0 (COMPAT):
irb(main):025:0> x.to_s(OpenSSL::X509::Name::COMPAT)
=> "CN=ola.bini, O=sweden/streetAddress=sweden, O=sweden/2.5.4.43343=sweden"
irb(main):026:0> x.to_s(OpenSSL::X509::Name::ONELINE)
=> "CN = ola.bini, O = sweden, streetAddress = sweden, O = sweden, 2.5.4.43343 = sweden"
irb(main):027:0> x.to_s(OpenSSL::X509::Name::MULTILINE)
=> "commonName                = ola.bini\norganizationName          = sweden\nstreetAddress             = sweden\norganizationName          = sweden\n2.5.4.43343 = sweden"
irb(main):028:0> x.to_s(OpenSSL::X509::Name::RFC2253)
=> "2.5.4.43343=#0C0673776564656E,O=sweden,streetAddress=sweden,O=sweden,CN=ola.bini"
else
=> /CN=ola.bini/O=sweden/streetAddress=sweden/O=sweden/2.5.4.43343=sweden

         */

        int flag = -1;
        if(args.length > 0 && !args[0].isNil()) {
            flag = RubyNumeric.fix2int(args[0]);
        }

        StringBuffer sb = new StringBuffer();
        Map<DERObjectIdentifier, String>  lookup = ASN1.getSymLookup(getRuntime());
        Iterator<Object> oiter = null;
        Iterator<Object> viter = null;
        if(flag == RFC2253) {
            List<Object> ao = new ArrayList<Object>(oids);
            List<Object> av = new ArrayList<Object>(values);
            java.util.Collections.reverse(ao);
            java.util.Collections.reverse(av);
            oiter = ao.iterator();
            viter = av.iterator();
        } else {
            oiter = oids.iterator();
            viter = values.iterator();
        }

        String sep = "";
        for(;oiter.hasNext();) {
            DERObjectIdentifier oid = (DERObjectIdentifier)oiter.next();
            String val = (String)viter.next();
            String outOid = lookup.get(oid);
            if(null == outOid) {
                outOid = oid.toString();
            }
            if(flag == RFC2253) {
                sb.append(sep).append(outOid).append("=").append(val);
                sep = ",";
            } else {
                sb.append("/").append(outOid).append("=").append(val);
            }
        }
        return getRuntime().newString(sb.toString());
    }

    @Override
    @JRubyMethod
    public RubyArray to_a() {
        List<IRubyObject> entries = new ArrayList<IRubyObject>();
        Map<DERObjectIdentifier, String> lookup = ASN1.getSymLookup(getRuntime());
        Iterator<Object> oiter = oids.iterator();
        Iterator<Object> viter = values.iterator();
        Iterator<Object> titer = types.iterator();
        for(;oiter.hasNext();) {
            DERObjectIdentifier oid = (DERObjectIdentifier)oiter.next();
            String val = (String)viter.next();
            String outOid = lookup.get(oid);
            if(null == outOid) {
                outOid = "UNDEF";
            }
            IRubyObject type = (IRubyObject)titer.next();
            entries.add(getRuntime().newArrayNoCopy(new IRubyObject[]{getRuntime().newString(outOid),getRuntime().newString(val),type}));
        }
        return getRuntime().newArray(entries);
    }

    @JRubyMethod(name={"cmp","<=>"})
    public IRubyObject cmp(IRubyObject other) {
        if(eql_p(other).isTrue()) {
            return RubyFixnum.zero(getRuntime());
        }
        // TODO: huh?
        return RubyFixnum.one(getRuntime());
    }

    org.bouncycastle.asn1.x509.X509Name getRealName() {
        return new org.bouncycastle.asn1.x509.X509Name(new Vector<Object>(oids),new Vector<Object>(values));
    }

    @Override
    @JRubyMethod(name="eql?")
    public IRubyObject eql_p(IRubyObject other) {
        if(!(other instanceof X509Name)) {
            return getRuntime().getFalse();
        }
        X509Name o = (X509Name)other;
        org.bouncycastle.asn1.x509.X509Name nm = new org.bouncycastle.asn1.x509.X509Name(new Vector<Object>(oids),new Vector<Object>(values));
        org.bouncycastle.asn1.x509.X509Name o_nm = new org.bouncycastle.asn1.x509.X509Name(new Vector<Object>(o.oids),new Vector<Object>(o.values));
        return nm.equals(o_nm) ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    @Override
    @JRubyMethod
    public RubyFixnum hash() {
        Name name = new Name(new org.bouncycastle.asn1.x509.X509Name(new Vector<Object>(oids),new Vector<Object>(values)));
        return getRuntime().newFixnum(name.hash());
    }

    @JRubyMethod
    public IRubyObject to_der() {
        DERSequence seq = null;
        if(oids.size()>0) {
            ASN1EncodableVector  vec = new ASN1EncodableVector();
            ASN1EncodableVector  sVec = new ASN1EncodableVector();
            DERObjectIdentifier  lstOid = null;
            for (int i = 0; i != oids.size(); i++) {
                ASN1EncodableVector     v = new ASN1EncodableVector();
                DERObjectIdentifier     oid = (DERObjectIdentifier)oids.get(i);
                v.add(oid);
                String  str = (String)values.get(i);
                v.add(convert(oid,str,RubyNumeric.fix2int(((RubyFixnum)types.get(i)))));
                if (lstOid == null) {
                    sVec.add(new DERSequence(v));
                } else {
                    vec.add(new DERSet(sVec));
                    sVec = new ASN1EncodableVector();
                    sVec.add(new DERSequence(v));
                }
                lstOid = oid;
            }
            vec.add(new DERSet(sVec));
            seq = new DERSequence(vec);
        } else {
            seq = new DERSequence();
        }

        return RubyString.newString(getRuntime(), seq.getDEREncoded());
    }

    private DERObject convert(DERObjectIdentifier oid, String value, int type) {
        try {
            Class<? extends ASN1Encodable> clzz = ASN1.classForId(type);
            if (clzz != null) {
                java.lang.reflect.Constructor<?> ctor = clzz.getConstructor(new Class[]{String.class});
                if (null != ctor) {
                    return (DERObject) ctor.newInstance(new Object[]{value});
                }
            }
            return new X509DefaultEntryConverter().getConvertedValue(oid, value);
        } catch (Exception e) {
            throw newX509NameError(getRuntime(), e.getMessage());
        }
    }

    private static RaiseException newX509NameError(Ruby runtime, String message) {
        return Utils.newError(runtime, "OpenSSL::X509::NameError", message);
    }
}// X509Name
