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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

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
import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509Name extends RubyObject {
    public static void createX509Name(IRuby runtime, RubyModule mX509) {
        RubyClass cX509Name = mX509.defineClassUnder("Name",runtime.getObject());
        mX509.defineClassUnder("NameError",runtime.getModule("OpenSSL").getClass("OpenSSLError"));

        CallbackFactory namecb = runtime.callbackFactory(X509Name.class);

        cX509Name.defineSingletonMethod("new",namecb.getOptSingletonMethod("newInstance"));
        cX509Name.defineMethod("initialize",namecb.getOptMethod("initialize"));
        cX509Name.defineMethod("add_entry",namecb.getOptMethod("add_entry"));
        cX509Name.defineMethod("to_s",namecb.getOptMethod("_to_s"));
        cX509Name.defineMethod("to_a",namecb.getMethod("to_a"));
        cX509Name.defineMethod("cmp",namecb.getMethod("cmp",IRubyObject.class));
        cX509Name.defineMethod("<=>",namecb.getMethod("cmp",IRubyObject.class));
        cX509Name.defineMethod("eql?",namecb.getMethod("eql_p",IRubyObject.class));
        cX509Name.defineMethod("hash",namecb.getMethod("hash"));
        cX509Name.defineMethod("to_der",namecb.getMethod("to_der"));
        
        cX509Name.setConstant("COMPAT",runtime.newFixnum(COMPAT));
        cX509Name.setConstant("RFC2253",runtime.newFixnum(RFC2253));
        cX509Name.setConstant("ONELINE",runtime.newFixnum(ONELINE));
        cX509Name.setConstant("MULTILINE",runtime.newFixnum(MULTILINE));

        cX509Name.setConstant("DEFAULT_OBJECT_TYPE",runtime.newFixnum(DERTags.UTF8_STRING));

        Map val = new HashMap();
        val.put(runtime.newString("C"),runtime.newFixnum(DERTags.PRINTABLE_STRING));
        val.put(runtime.newString("countryName"),runtime.newFixnum(DERTags.PRINTABLE_STRING));
        val.put(runtime.newString("serialNumber"),runtime.newFixnum(DERTags.PRINTABLE_STRING));
        val.put(runtime.newString("dnQualifier"),runtime.newFixnum(DERTags.PRINTABLE_STRING));
        val.put(runtime.newString("DC"),runtime.newFixnum(DERTags.IA5_STRING));
        val.put(runtime.newString("domainComponent"),runtime.newFixnum(DERTags.IA5_STRING));
        val.put(runtime.newString("emailAddress"),runtime.newFixnum(DERTags.IA5_STRING));
        cX509Name.setConstant("OBJECT_TYPE_TEMPLATE",new RubyHash(runtime,val,runtime.newFixnum(DERTags.UTF8_STRING)));
    }

    public static final int COMPAT = 0;
    public static final int RFC2253 = 17892119;
    public static final int ONELINE = 8520479;
    public static final int MULTILINE = 44302342;

    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
        X509Name result = new X509Name(recv.getRuntime(), (RubyClass)recv);
        result.callInit(args);
        return result;
    }

    public X509Name(IRuby runtime, RubyClass type) {
        super(runtime,type);
        oids = new ArrayList();
        values = new ArrayList();
        types = new ArrayList();
    }

    private org.bouncycastle.asn1.x509.X509Name name;

    private List oids;
    private List values;
    private List types;

    void addEntry(Object oid, Object value, Object type) {
        oids.add(oid);
        values.add(value);
        types.add(type);
    }

    public IRubyObject initialize(IRubyObject[] args) {
        if(checkArgumentCount(args,0,2) == 0) {
            return this;
        }
        IRubyObject arg = args[0];
        IRubyObject template = getRuntime().getNil();
        if(args.length > 1) {
            template = args[1];
        }
        IRubyObject tmp = (arg instanceof RubyArray) ? arg : getRuntime().getNil();
        if(!tmp.isNil()) {
            if(template.isNil()) {
                template = ((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getClass("Name").getConstant("OBJECT_TYPE_TEMPLATE");
            }
            for(Iterator iter = ((RubyArray)tmp).getList().iterator();iter.hasNext();) {
                RubyArray arr = (RubyArray)iter.next();
                IRubyObject[] entry = new IRubyObject[3];
                List l = arr.getList();
                entry[0] = (IRubyObject)l.get(0);
                entry[1] = (IRubyObject)l.get(1);
                if(l.size()>2) {
                    entry[2] = (IRubyObject)l.get(2);
                }
                if(entry[2] == null || entry[2].isNil()) {
                    entry[2] = template.callMethod(getRuntime().getCurrentContext(),"[]",entry[0]);
                }
                if(entry[2] == null || entry[2].isNil()) {
                    entry[2] = ((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getClass("Name").getConstant("DEFAULT_OBJECT_TYPE");
                }
                add_entry(entry);
            }
        } else {
            try {
                ASN1Sequence seq = (ASN1Sequence)new ASN1InputStream(OpenSSLImpl.to_der_if_possible(arg).toString().getBytes("PLAIN")).readObject();
                oids = new ArrayList();
                values = new ArrayList();
                types = new ArrayList();
                for(Enumeration enm = seq.getObjects();enm.hasMoreElements();) {
                    ASN1Sequence value = (ASN1Sequence)(((ASN1Set)enm.nextElement()).getObjectAt(0));
                    oids.add(value.getObjectAt(0));
                    if(value.getObjectAt(1) instanceof DERString) {
                        values.add(((DERString)value.getObjectAt(1)).getString());
                    } else {
                        values.add(null);
                    }
                    types.add(getRuntime().newFixnum(ASN1.idForClass(value.getObjectAt(1).getClass())));
                }
            } catch(Exception e) {
                System.err.println("exception in init for X509Name: " + e);
            }
        }
        return this;
    }

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

    private DERObjectIdentifier getObjectIdentifier(String nameOrOid) {
        Object val1 = ASN1.getOIDLookup(getRuntime()).get(nameOrOid.toLowerCase());
        if(null != val1) {
            return (DERObjectIdentifier)val1;
        }
        DERObjectIdentifier val2 = new DERObjectIdentifier(nameOrOid);
        return val2;
    }

    public IRubyObject add_entry(IRubyObject[] args) {
        checkArgumentCount(args,2,3);
        String oid = args[0].toString();
        String value = args[1].toString();
        IRubyObject type = ((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getClass("Name").getConstant("OBJECT_TYPE_TEMPLATE").callMethod(getRuntime().getCurrentContext(),"[]",args[0]);
        if(args.length > 2 && !args[2].isNil()) {
            type = args[2];
        }

        DERObjectIdentifier oid_v;
        try {
            oid_v = getObjectIdentifier(oid);
        } catch(IllegalArgumentException e) {
            throw new RaiseException(getRuntime(), (RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getConstant("NameError")), "invalid field name", true);
        }
        if(null == oid_v) {
            throw new RaiseException(getRuntime(), (RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getConstant("NameError")), null, true);
        }
        oids.add(oid_v);
        values.add(value);
        types.add(type);

        return this;
    }

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
        Map lookup = ASN1.getSymLookup(getRuntime());
        Iterator oiter = null;
        Iterator viter = null;
        if(flag == RFC2253) {
            List ao = new ArrayList(oids);
            List av = new ArrayList(values);
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
            String outOid = (String)lookup.get(oid);
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

    public IRubyObject to_a() {
        List entries = new ArrayList();
        Map lookup = ASN1.getSymLookup(getRuntime());
        Iterator oiter = oids.iterator();
        Iterator viter = values.iterator();
        Iterator titer = types.iterator();
        for(;oiter.hasNext();) {
            DERObjectIdentifier oid = (DERObjectIdentifier)oiter.next();
            String val = (String)viter.next();
            String outOid = (String)lookup.get(oid);
            if(null == outOid) {
                outOid = "UNDEF";
            }
            IRubyObject type = (IRubyObject)titer.next();
            entries.add(getRuntime().newArray(new IRubyObject[]{getRuntime().newString(outOid),getRuntime().newString(val),type}));
        }
        return getRuntime().newArray(entries);
    }

    public IRubyObject cmp(IRubyObject other) {
        if(eql_p(other).isTrue()) {
            return RubyFixnum.zero(getRuntime());
        }

        return RubyFixnum.one(getRuntime());
    }

    org.bouncycastle.asn1.x509.X509Name getRealName() {
        return new org.bouncycastle.asn1.x509.X509Name(new Vector(oids),new Vector(values));
    }

    public IRubyObject eql_p(IRubyObject other) {
        if(!(other instanceof X509Name)) {
            return getRuntime().getFalse();
        }
        X509Name o = (X509Name)other;
        org.bouncycastle.asn1.x509.X509Name nm = new org.bouncycastle.asn1.x509.X509Name(new Vector(oids),new Vector(values));
        org.bouncycastle.asn1.x509.X509Name o_nm = new org.bouncycastle.asn1.x509.X509Name(new Vector(o.oids),new Vector(o.values));
        return nm.equals(o_nm) ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public RubyFixnum hash() {
        return getRuntime().newFixnum(new org.bouncycastle.asn1.x509.X509Name(new Vector(oids),new Vector(values)).hashCode());
    }

    public IRubyObject to_der() throws Exception {
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

        return getRuntime().newString(new String(seq.getDEREncoded(),"ISO8859_1"));
    }

    private DERObject convert(DERObjectIdentifier oid, String value, int type) throws Exception {
        Class clzz = ASN1.classForId(type);
        if(clzz != null) {
            java.lang.reflect.Constructor ctor = clzz.getConstructor(new Class[]{String.class});
            if(null != ctor) {
                return (DERObject)ctor.newInstance(new Object[]{value});
            }
        }
        return new X509DefaultEntryConverter().getConvertedValue(oid, value);
    }
}// X509Name
