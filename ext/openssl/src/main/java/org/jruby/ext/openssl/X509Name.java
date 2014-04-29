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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.BERTags;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.DLSet;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.X509DefaultEntryConverter;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Visibility;

import org.jruby.ext.openssl.x509store.Name;
import static org.jruby.ext.openssl.OpenSSLReal.isDebug;
import static org.jruby.ext.openssl.X509._X509;

/**
 *
 * TODO member variables and methods are based on BC X509 way of doing things (now deprecated). Change
 * it to do it the X500 way, with RDN and X500NameBuilder.
 *
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509Name extends RubyObject {
    private static final long serialVersionUID = -226196051911335103L;

    private static ObjectAllocator X509NAME_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new X509Name(runtime, klass);
        }
    };

    public static void createX509Name(final Ruby runtime, final RubyModule _X509) {
        RubyClass _Name = _X509.defineClassUnder("Name", runtime.getObject(), X509NAME_ALLOCATOR);
        RubyClass _OpenSSLError = runtime.getModule("OpenSSL").getClass("OpenSSLError");
        _X509.defineClassUnder("NameError", _OpenSSLError, _OpenSSLError.getAllocator());

        _Name.defineAnnotatedMethods(X509Name.class);
        _Name.includeModule(runtime.getComparable());

        _Name.setConstant("COMPAT", runtime.newFixnum(COMPAT));
        _Name.setConstant("RFC2253", runtime.newFixnum(RFC2253));
        _Name.setConstant("ONELINE", runtime.newFixnum(ONELINE));
        _Name.setConstant("MULTILINE", runtime.newFixnum(MULTILINE));

        _Name.setConstant("DEFAULT_OBJECT_TYPE", runtime.newFixnum(BERTags.UTF8_STRING));

        final ThreadContext context = runtime.getCurrentContext();
        RubyHash hash = new RubyHash(runtime, runtime.newFixnum(BERTags.UTF8_STRING));
        hash.op_aset(context, runtime.newString("C"), runtime.newFixnum(BERTags.PRINTABLE_STRING));
        hash.op_aset(context, runtime.newString("countryName"), runtime.newFixnum(BERTags.PRINTABLE_STRING));
        hash.op_aset(context, runtime.newString("serialNumber"), runtime.newFixnum(BERTags.PRINTABLE_STRING));
        hash.op_aset(context, runtime.newString("dnQualifier"), runtime.newFixnum(BERTags.PRINTABLE_STRING));
        hash.op_aset(context, runtime.newString("DC"), runtime.newFixnum(BERTags.IA5_STRING));
        hash.op_aset(context, runtime.newString("domainComponent"), runtime.newFixnum(BERTags.IA5_STRING));
        hash.op_aset(context, runtime.newString("emailAddress"), runtime.newFixnum(BERTags.IA5_STRING));
        _Name.setConstant("OBJECT_TYPE_TEMPLATE", hash);
    }

    static RubyClass _Name(final Ruby runtime) {
        return _X509(runtime).getClass("Name");
    }

    public static final int COMPAT = 0;
    public static final int RFC2253 = 17892119;
    public static final int ONELINE = 8520479;
    public static final int MULTILINE = 44302342;

    public X509Name(Ruby runtime, RubyClass type) {
        super(runtime,type);
        oids = new ArrayList<ASN1ObjectIdentifier>();
        values = new ArrayList<String>();
        types = new ArrayList<RubyInteger>();
    }

    private final List<ASN1ObjectIdentifier> oids;
    private final List<String> values;
    private final List<RubyInteger> types;

    void addEntry(ASN1ObjectIdentifier oid, String value, RubyInteger type) {
        oids.add(oid);
        values.add(value);
        types.add(type);
    }

    public static X509Name create(Ruby runtime, org.bouncycastle.asn1.x500.X500Name realName) {
        final X509Name name = new X509Name( runtime, _Name(runtime) );
        name.fromASN1Sequence((ASN1Sequence) realName.toASN1Primitive());
        return name;
    }

    void fromASN1Sequence(final ASN1Sequence seq) {
        oids.clear(); values.clear(); types.clear();
        if ( seq != null ) {
            for ( Enumeration e = seq.getObjects(); e.hasMoreElements(); ) {
                ASN1Object element = (ASN1Object) e.nextElement();
                if (element instanceof RDN) {
                    fromRDNElement((RDN) element);
                } else if (element instanceof ASN1Sequence) {
                    fromASN1Sequence(element);
                } else {
                    fromASN1Set(element);
                }
            }
        }
    }

    private void fromRDNElement(final RDN rdn) {
        for( AttributeTypeAndValue tv: rdn.getTypesAndValues() ) {
            oids.add(tv.getType());
            if (tv.getValue() instanceof ASN1String) {
                values.add(((ASN1String) tv.getValue()).getString());
            } else {
                values.add(null); //TODO really?
            }
            types.add(getRuntime().newFixnum(ASN1.idForClass(tv.getValue().getClass())));
        }
    }

    private void fromASN1Set(Object element) {
        ASN1Set typeAndValue = ASN1Set.getInstance(element);
        for (Enumeration enumRdn = typeAndValue.getObjects(); enumRdn.hasMoreElements();) {
            fromASN1Sequence(enumRdn.nextElement());
        }
    }

    private void fromASN1Sequence(Object element) {
        ASN1Sequence typeAndValue = ASN1Sequence.getInstance(element);
        oids.add((ASN1ObjectIdentifier) typeAndValue.getObjectAt(0));
        if (typeAndValue.getObjectAt(1) instanceof ASN1String) {
            values.add(((ASN1String) typeAndValue.getObjectAt(1)).getString());
        } else {
            values.add(null);
        }
        types.add(getRuntime().newFixnum(ASN1.idForClass(typeAndValue.getObjectAt(1).getClass())));
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context) {
        return this;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject str_or_dn) {
        return initialize(context, str_or_dn, context.nil);
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(final ThreadContext context, IRubyObject dn, IRubyObject template) {
        final Ruby runtime = context.runtime;

        if ( dn instanceof RubyArray ) {
            RubyArray ary = (RubyArray)dn;

            final RubyClass _Name = _Name(runtime);

            if ( template.isNil() ) template = _Name.getConstant("OBJECT_TYPE_TEMPLATE");

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
                if (entry2.isNil()) entry2 = _Name.getConstant("DEFAULT_OBJECT_TYPE");

                add_entry(context, entry0, entry1, entry2);
            }
        } else {
            try {
                byte[] bytes = OpenSSLImpl.to_der_if_possible(context, dn).asString().getBytes();
                ASN1InputStream is = new ASN1InputStream(bytes);
                ASN1Sequence seq = (ASN1Sequence)is.readObject();
                //StringBuilder b = new StringBuilder();
                //printASN(seq, b);
                fromASN1Sequence(seq);
            }
            catch (IOException e) { //Do not catch Exception. Want to see nullpointer stacktrace.
                throw newNameError(runtime, e.getClass().getName() + ":" + e.getLocalizedMessage());
            }
        }
        return this;
    }


    private static void printASN(final ASN1Encodable obj, final StringBuilder out) {
        printASN(obj, 0, out);
    }

    private static void printASN(final ASN1Encodable obj, final int indent, final StringBuilder out) {
        for( int i = 0; i < indent; i++ ) out.append(' ');
        if ( obj instanceof ASN1Sequence ) {
            out.append("- Sequence:");
            for ( Enumeration e = ((ASN1Sequence) obj).getObjects(); e.hasMoreElements(); ) {
                printASN((ASN1Encodable) e.nextElement(), indent + 1, out);
            }
        }
        else if ( obj instanceof ASN1Set ) {
            out.append("- Set:");
            for ( Enumeration e = ((ASN1Set) obj).getObjects(); e.hasMoreElements(); ) {
                printASN((ASN1Encodable) e.nextElement(), indent + 1, out);
            }
        }
        else {
            if ( obj instanceof ASN1String ) {
                out.append("- ").append(obj).
                    append('=').append( ((ASN1String) obj).getString() ).
                    append('[').append( obj.getClass().getName() ).append(']');
            } else {
                out.append("- ").append(obj).
                    append('[').append( obj.getClass().getName() ).append(']');
            }
        }
    }

    @JRubyMethod
    public IRubyObject add_entry(ThreadContext context, IRubyObject oid, IRubyObject value) {
        return add_entry(context, oid, value, context.nil);
    }

    @JRubyMethod
    public IRubyObject add_entry(final ThreadContext context,
        final IRubyObject oid, final IRubyObject value, IRubyObject type) {
        final Ruby runtime = context.runtime;

        if ( type.isNil() ) {
            type = _Name(runtime).getConstant("OBJECT_TYPE_TEMPLATE").callMethod(context, "[]", oid);
        }

        final ASN1ObjectIdentifier objectId;
        try {
            String oidStr = oid.asString().toString();
            objectId = ASN1.getObjectIdentifier(runtime, oidStr);
        }
        catch (IllegalArgumentException e) {
            throw newNameError(runtime, "invalid field name: " + e.getMessage());
        }

        if ( objectId == null ) throw newNameError(runtime, (String) null);

        String valueStr = value.asString().toString();

        oids.add(objectId);
        values.add(valueStr);
        types.add((RubyInteger) type);

        return this;
    }

    @JRubyMethod(name = "to_s", rest = true)
    public IRubyObject to_s(IRubyObject[] args) {
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
        if ( args.length > 0 && ! args[0].isNil() ) {
            flag = RubyNumeric.fix2int( args[0] );
        }

        final Iterator<ASN1ObjectIdentifier> oidsIter;
        final Iterator<String> valuesIter;
        if ( flag == RFC2253 ) {
            ArrayList<ASN1ObjectIdentifier> reverseOids = new ArrayList<ASN1ObjectIdentifier>(oids);
            ArrayList<String> reverseValues = new ArrayList<String>(values);
            Collections.reverse(reverseOids);
            Collections.reverse(reverseValues);
            oidsIter = reverseOids.iterator();
            valuesIter = reverseValues.iterator();
        } else {
            oidsIter = oids.iterator();
            valuesIter = values.iterator();
        }

        final Map<ASN1ObjectIdentifier, String> lookup = ASN1.getSymLookup(getRuntime());
        final StringBuilder str = new StringBuilder(); String sep = "";
        while( oidsIter.hasNext() ) {
            final ASN1ObjectIdentifier oid = oidsIter.next();
            String outOid = lookup.get(oid);
            if ( outOid == null ) outOid = oid.toString();
            final String value = valuesIter.next();

            if ( flag == RFC2253 ) {
                str.append(sep).append(outOid).append('=').append(value);
                sep = ",";
            } else {
                str.append('/').append(outOid).append('=').append(value);
            }
        }

        return getRuntime().newString( str.toString() );
    }

    @Override
    @JRubyMethod
    public RubyArray to_a() {
        final Ruby runtime = getRuntime();
        final RubyArray entries = runtime.newArray( oids.size() );
        final Map<ASN1ObjectIdentifier, String> lookup = ASN1.getSymLookup(getRuntime());
        final Iterator<ASN1ObjectIdentifier> oidsIter = oids.iterator();
        final Iterator<String> valuesIter = values.iterator();
        final Iterator<RubyInteger> typesIter = types.iterator();
        while ( oidsIter.hasNext() ) {
            ASN1ObjectIdentifier oid = oidsIter.next();
            String outOid = lookup.get(oid);
            if ( outOid == null ) outOid = "UNDEF";
            final String value = valuesIter.next();
            final IRubyObject type = typesIter.next();
            final IRubyObject[] entry = new IRubyObject[] {
                runtime.newString(outOid), runtime.newString(value), type
            };
            entries.append( runtime.newArrayNoCopy(entry) );
        }
        return entries;
    }

    @JRubyMethod(name = { "cmp", "<=>" })
    public IRubyObject cmp(IRubyObject other) {
        if ( eql_p(other).isTrue() ) {
            return RubyFixnum.zero(getRuntime());
        }
        // TODO: huh?
        return RubyFixnum.one(getRuntime());
    }

    org.bouncycastle.asn1.x509.X509Name getRealName() {
        return new org.bouncycastle.asn1.x509.X509Name(new Vector<Object>(oids),new Vector<Object>(values));
    }

    X500Name getX500Name() {
        return X500Name.getInstance(getRealName().toASN1Primitive());
    }

    @Override
    @JRubyMethod(name = "eql?")
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
        Name name = new Name( getX500Name() );
        return getRuntime().newFixnum(name.hash());
    }

    @JRubyMethod
    public IRubyObject to_der() {
        final DLSequence seq;
        if ( oids.size() > 0 ) {
            ASN1EncodableVector vec = new ASN1EncodableVector();
            ASN1EncodableVector sVec = new ASN1EncodableVector();
            ASN1ObjectIdentifier lastOid = null;
            for ( int i = 0; i != oids.size(); i++ ) {
                final ASN1ObjectIdentifier oid = oids.get(i);
                ASN1EncodableVector v = new ASN1EncodableVector();
                v.add(oid);
                final String value = values.get(i);
                final int type = RubyNumeric.fix2int(types.get(i));
                v.add( convert(oid, value, type) );
                if ( lastOid == null ) {
                    sVec.add(new DLSequence(v));
                } else {
                    vec.add(new DLSet(sVec));
                    sVec = new ASN1EncodableVector();
                    sVec.add(new DLSequence(v));
                }
                lastOid = oid;
            }
            vec.add(new DLSet(sVec));
            seq = new DLSequence(vec);
        } else {
            seq = new DLSequence();
        }
        try {
            return RubyString.newString(getRuntime(), seq.getEncoded(ASN1Encoding.DER));
        }
        catch (IOException e) {
            throw newNameError(getRuntime(), e);
        }
    }

    private ASN1Primitive convert(ASN1ObjectIdentifier oid, String value, int type) {
        final Class<? extends ASN1Encodable> clazz = ASN1.classForId(type);
        try {
            if ( clazz != null ) {
                Constructor<?> ctor = clazz.getConstructor(new Class[]{ String.class });
                if (null != ctor) {
                    return (ASN1Primitive) ctor.newInstance(new Object[]{ value });
                }
            }
            return new X509DefaultEntryConverter().getConvertedValue(oid, value);
        }
        catch (NoSuchMethodException e) {
            throw newNameError(getRuntime(), e);
        }
        catch (InstantiationException e) {
            throw newNameError(getRuntime(), e);
        }
        catch (IllegalAccessException e) {
            throw newNameError(getRuntime(), e);
        }
        catch (IllegalArgumentException e) {
            throw newNameError(getRuntime(), e);
        }
        catch (InvocationTargetException e) {
            throw newNameError(getRuntime(), e.getTargetException());
        }
        catch (RuntimeException e) {
            if ( isDebug(getRuntime()) ) e.printStackTrace(getRuntime().getOut());
            throw newNameError(getRuntime(), e);
        }
    }

    private static RaiseException newNameError(Ruby runtime, Throwable e) {
        return Utils.newError(runtime, _X509(runtime).getClass("NameError"), e.getMessage());
    }

    private static RaiseException newNameError(Ruby runtime, String message) {
        return Utils.newError(runtime, _X509(runtime).getClass("NameError"), message);
    }

}// X509Name
