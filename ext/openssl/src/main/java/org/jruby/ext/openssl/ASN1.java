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
import java.io.PrintStream;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.IdentityHashMap;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERBoolean;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTCTime;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x509.X509Name;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.RubyTime;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Visibility;
import org.jruby.util.ByteList;

import static org.jruby.ext.openssl.OpenSSLReal.isDebug;
import static org.jruby.ext.openssl.OpenSSLReal.warn;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class ASN1 {

    private static Map<Ruby, Map<String, ASN1ObjectIdentifier>> SYM_TO_OID = new IdentityHashMap<Ruby, Map<String, ASN1ObjectIdentifier>>();
    private static Map<Ruby, Map<ASN1ObjectIdentifier, String>> OID_TO_SYM = new IdentityHashMap<Ruby, Map<ASN1ObjectIdentifier, String>>();
    private static Map<Ruby, Map<ASN1ObjectIdentifier, Integer>> OID_TO_NID = new IdentityHashMap<Ruby, Map<ASN1ObjectIdentifier, Integer>>();
    private static Map<Ruby, Map<Integer, ASN1ObjectIdentifier>> NID_TO_OID = new IdentityHashMap<Ruby, Map<Integer, ASN1ObjectIdentifier>>();
    private static Map<Ruby, Map<Integer, String>> NID_TO_SN = new IdentityHashMap<Ruby, Map<Integer, String>>();
    private static Map<Ruby, Map<Integer, String>> NID_TO_LN = new IdentityHashMap<Ruby, Map<Integer, String>>();

    @SuppressWarnings("unchecked")
    private static synchronized void initMaps(final Ruby runtime) {
        SYM_TO_OID.put(runtime, new HashMap<String, ASN1ObjectIdentifier>(X509Name.DefaultLookUp));
        OID_TO_SYM.put(runtime, new HashMap<ASN1ObjectIdentifier, String>(X509Name.DefaultSymbols));
        OID_TO_NID.put(runtime, new HashMap<ASN1ObjectIdentifier, Integer>());
        NID_TO_OID.put(runtime, new HashMap<Integer, ASN1ObjectIdentifier>());
        NID_TO_SN.put(runtime, new HashMap<Integer, String>());
        NID_TO_LN.put(runtime, new HashMap<Integer, String>());

        OpenSSLImpl.defaultObjects(runtime);
    }

    private static Map<String, ASN1ObjectIdentifier> symToOid(final Ruby runtime) {
        Map<String, ASN1ObjectIdentifier> map = SYM_TO_OID.get(runtime);
        if ( map == null ) {
            synchronized(ASN1.class) {
                map = SYM_TO_OID.get(runtime);
                if ( map == null ) {
                    initMaps(runtime);
                    map = SYM_TO_OID.get(runtime);
                }
            }
        }
        return map;
    }

    private static Map<ASN1ObjectIdentifier, String> oidToSym(final Ruby runtime) {
        Map<ASN1ObjectIdentifier, String> map = OID_TO_SYM.get(runtime);
        if ( map == null ) {
            synchronized(ASN1.class) {
                map = OID_TO_SYM.get(runtime);
                if ( map == null ) {
                    initMaps(runtime);
                    map = OID_TO_SYM.get(runtime);
                }
            }
        }
        return map;
    }

    private static Map<Integer, ASN1ObjectIdentifier> nidToOid(final Ruby runtime) {
        Map<Integer, ASN1ObjectIdentifier> map = NID_TO_OID.get(runtime);
        if ( map == null ) {
            synchronized(ASN1.class) {
                map = NID_TO_OID.get(runtime);
                if ( map == null ) {
                    initMaps(runtime);
                    map = NID_TO_OID.get(runtime);
                }
            }
        }
        return map;
    }

    private static Map<ASN1ObjectIdentifier, Integer> oidToNid(final Ruby runtime) {
        Map<ASN1ObjectIdentifier, Integer> map = OID_TO_NID.get(runtime);
        if ( map == null ) {
            synchronized(ASN1.class) {
                map = OID_TO_NID.get(runtime);
                if ( map == null ) {
                    initMaps(runtime);
                    map = OID_TO_NID.get(runtime);
                }
            }
        }
        return map;
    }

    private static Map<Integer, String> nidToSn(final Ruby runtime) {
        Map<Integer, String> map = NID_TO_SN.get(runtime);
        if ( map == null ) {
            synchronized(ASN1.class) {
                map = NID_TO_SN.get(runtime);
                if ( map == null ) {
                    initMaps(runtime);
                    map = NID_TO_SN.get(runtime);
                }
            }
        }
        return map;
    }

    private static Map<Integer, String> nidToLn(final Ruby runtime) {
        Map<Integer, String> map = NID_TO_LN.get(runtime);
        if ( map == null ) {
            synchronized(ASN1.class) {
                map = NID_TO_LN.get(runtime);
                if ( map == null ) {
                    initMaps(runtime);
                    map = NID_TO_LN.get(runtime);
                }
            }
        }
        return map;
    }

    static void addObject(final Ruby runtime, final int nid,
        final String sn, final String ln, final String oid) {
        if ( oid != null && ( sn != null || ln != null ) ) {

            ASN1ObjectIdentifier objectId = new ASN1ObjectIdentifier(oid);

            if ( sn != null ) {
                symToOid(runtime).put(sn.toLowerCase(), objectId);
            }
            if ( ln != null ) {
                symToOid(runtime).put(ln.toLowerCase(), objectId);
            }

            oidToSym(runtime).put(objectId, sn == null ? ln : sn);
            oidToNid(runtime).put(objectId, nid);
            nidToOid(runtime).put(nid, objectId);
            nidToSn(runtime).put(nid, sn);
            nidToLn(runtime).put(nid, ln);
        }
    }

    static String ln2oid(final Ruby runtime, final String ln) {
        Map<String, ASN1ObjectIdentifier> map = symToOid(runtime);
        final ASN1ObjectIdentifier val = map.get(ln);
        if ( val == null ) {
            throw new NullPointerException("oid not found for ln = '" + ln + "' (" + runtime + ")");
        }
        return val.getId();
    }

    static Integer obj2nid(Ruby runtime, final ASN1ObjectIdentifier oid) {
        return oidToNid(runtime).get(oid);
    }

    static String o2a(final Ruby runtime, final ASN1ObjectIdentifier oid) {
        final Integer nid = obj2nid(runtime, oid);
        if ( nid == null ) {
            throw new NullPointerException("nid not found for oid = '" + oid + "' (" + runtime + ")");
        }
        String one = nidToLn(runtime).get(nid);
        if (one == null) {
            one = nidToSn(runtime).get(nid);
        }
        return one;
    }

    static String nid2ln(final Ruby runtime, final int nid) {
        return nidToLn(runtime).get(nid);
    }

    static Map<String, ASN1ObjectIdentifier> getOIDLookup(final Ruby runtime) {
        return symToOid(runtime);
    }

    static Map<ASN1ObjectIdentifier, String> getSymLookup(final Ruby runtime) {
        return oidToSym(runtime);
    }

    private final static Object[][] ASN1_INFO = {
        {"EOC", null, null },
        {"BOOLEAN", org.bouncycastle.asn1.DERBoolean.class, "Boolean" },
        {"INTEGER", org.bouncycastle.asn1.ASN1Integer.class, "Integer" },
        {"BIT_STRING",  org.bouncycastle.asn1.DERBitString.class, "BitString" },
        {"OCTET_STRING",  org.bouncycastle.asn1.DEROctetString.class, "OctetString" },
        {"NULL",  org.bouncycastle.asn1.DERNull.class, "Null" },
        {"OBJECT",  org.bouncycastle.asn1.ASN1ObjectIdentifier.class, "ObjectId" },
        {"OBJECT_DESCRIPTOR",  null, null },
        {"EXTERNAL",  null, null },
        {"REAL",  null, null },
        {"ENUMERATED",  org.bouncycastle.asn1.DEREnumerated.class, "Enumerated" },
        {"EMBEDDED_PDV",  null, null },
        {"UTF8STRING",  org.bouncycastle.asn1.DERUTF8String.class, "UTF8String" },
        {"RELATIVE_OID",  null, null },
        {"[UNIVERSAL 14]",  null, null },
        {"[UNIVERSAL 15]",  null, null },
        {"SEQUENCE",  org.bouncycastle.asn1.DLSequence.class, "Sequence" },
        {"SET",  org.bouncycastle.asn1.DLSet.class, "Set" },
        {"NUMERICSTRING",  org.bouncycastle.asn1.DERNumericString.class, "NumericString" },
        {"PRINTABLESTRING",  org.bouncycastle.asn1.DERPrintableString.class, "PrintableString" },
        {"T61STRING",  org.bouncycastle.asn1.DERT61String.class, "T61String" },
        {"VIDEOTEXSTRING", null, null },
        {"IA5STRING",  org.bouncycastle.asn1.DERIA5String.class, "IA5String" },
        {"UTCTIME",  org.bouncycastle.asn1.DERUTCTime.class, "UTCTime" },
        {"GENERALIZEDTIME",  org.bouncycastle.asn1.DERGeneralizedTime.class, "GeneralizedTime" },
        {"GRAPHICSTRING",  null, null },
        {"ISO64STRING",  null, null },
        {"GENERALSTRING",  org.bouncycastle.asn1.DERGeneralString.class, "GeneralString" },
        {"UNIVERSALSTRING",  org.bouncycastle.asn1.DERUniversalString.class, "UniversalString" },
        {"CHARACTER_STRING",  null, null },
        {"BMPSTRING", org.bouncycastle.asn1.DERBMPString.class, "BMPString" }};

    private final static Map<Class, Integer> CLASS_TO_ID = new HashMap<Class, Integer>();
    private final static Map<String, Integer> RUBYNAME_TO_ID = new HashMap<String, Integer>();

    static {
        for ( int i = 0; i < ASN1_INFO.length; i++ ) {
            final Object[] info = ASN1_INFO[i];
            if ( info[1] != null ) {
                CLASS_TO_ID.put((Class) info[1], Integer.valueOf(i));
            }
            if ( info[2] != null ) {
                RUBYNAME_TO_ID.put((String) info[2], Integer.valueOf(i));
            }
        }
    }

    static int idForClass(Class type) {
        Integer v = null;
        while ( type != Object.class && v == null ) {
            v = CLASS_TO_ID.get(type);
            if ( v == null ) type = type.getSuperclass();
        }
        return v == null ? -1 : v.intValue();
    }

    static int idForRubyName(String name) {
        Integer v = RUBYNAME_TO_ID.get(name);
        return v == null ? -1 : v.intValue();
    }

    static Class<? extends ASN1Encodable> classForId(int id) {
        @SuppressWarnings("unchecked")
        Class<? extends ASN1Encodable> result = (Class<? extends ASN1Encodable>)(ASN1_INFO[id][1]);
        return result;
    }

    public static void createASN1(Ruby runtime, RubyModule ossl) {
        final RubyModule _ASN1 = ossl.defineModuleUnder("ASN1");
        final RubyClass _OpenSSLError = ossl.getClass("OpenSSLError");
        _ASN1.defineClassUnder("ASN1Error", _OpenSSLError, _OpenSSLError.getAllocator());

        _ASN1.defineAnnotatedMethods(ASN1.class);

        final RubyArray _UNIVERSAL_TAG_NAME = runtime.newArray();
        _ASN1.setConstant("UNIVERSAL_TAG_NAME", _UNIVERSAL_TAG_NAME);

        for ( int i = 0; i < ASN1_INFO.length; i++ ) {
            final String name = (String) ASN1_INFO[i][0];
            if ( name.charAt(0) != '[' ) {
                _UNIVERSAL_TAG_NAME.append( runtime.newString(name) );
                _ASN1.setConstant( name, runtime.newFixnum(i) );
            } else {
                _UNIVERSAL_TAG_NAME.append( runtime.getNil() );
            }
        }

        final ThreadContext context = runtime.getCurrentContext();
        RubyClass _ASN1Data = _ASN1.defineClassUnder("ASN1Data", runtime.getObject(), ASN1Data.ALLOCATOR);
        _ASN1Data.addReadWriteAttribute(context, "value");
        _ASN1Data.addReadWriteAttribute(context, "tag");
        _ASN1Data.addReadWriteAttribute(context, "tag_class");
        _ASN1Data.defineAnnotatedMethods(ASN1Data.class);

        final ObjectAllocator primitiveAllocator = ASN1Primitive.ALLOCATOR;
        RubyClass _Primitive = _ASN1.defineClassUnder("Primitive", _ASN1Data, primitiveAllocator);
        _Primitive.addReadWriteAttribute(context, "tagging");
        _Primitive.defineAnnotatedMethods(ASN1Primitive.class);

        RubyClass _Constructive = _ASN1.defineClassUnder("Constructive", _ASN1Data, ASN1Constructive.ALLOCATOR);
        _Constructive.includeModule( runtime.getModule("Enumerable") );
        _Constructive.addReadWriteAttribute(context, "tagging");
        _Constructive.defineAnnotatedMethods(ASN1Constructive.class);

        _ASN1.defineClassUnder("Boolean", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("Integer", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("Enumerated", _Primitive, primitiveAllocator);

        RubyClass _BitString = _ASN1.defineClassUnder("BitString", _Primitive, primitiveAllocator);

        _ASN1.defineClassUnder("OctetString", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("UTF8String", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("NumericString", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("PrintableString", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("T61String", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("VideotexString", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("IA5String", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("GraphicString", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("ISO64String", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("GeneralString", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("UniversalString", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("BMPString", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("Null", _Primitive, primitiveAllocator);

        RubyClass _ObjectId = _ASN1.defineClassUnder("ObjectId", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("UTCTime", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("GeneralizedTime", _Primitive, primitiveAllocator);
        _ASN1.defineClassUnder("Sequence", _Constructive, _Constructive.getAllocator());
        _ASN1.defineClassUnder("Set", _Constructive, _Constructive.getAllocator());

        _ObjectId.defineAnnotatedMethods(ObjectId.class);

        _BitString.addReadWriteAttribute(context, "unused_bits");
    }


    private static String getShortNameFor(Ruby runtime, String nameOrOid) {
        return getNameFor(runtime, nameOrOid, true);
    }

    private static String getLongNameFor(Ruby runtime, String nameOrOid) {
        return getNameFor(runtime, nameOrOid, false);
    }

    private static String getNameFor(final Ruby runtime, final String nameOrOid, final boolean shortName) {
        ASN1ObjectIdentifier oid = getObjectIdentifier(runtime, nameOrOid);
        Map<String, ASN1ObjectIdentifier> lookup = getOIDLookup(runtime);
        String name = null;
        for ( final String key : lookup.keySet() ) {
            if ( oid.equals( lookup.get(key) ) ) {
                if ( name == null ||
                ( shortName ? key.length() < name.length() : key.length() > name.length() ) ) {
                    name = key;
                }
            }
        }
        return name;
    }

    static ASN1ObjectIdentifier getObjectIdentifier(final Ruby runtime, final String nameOrOid)
        throws IllegalArgumentException {
        Object val1 = getOIDLookup(runtime).get( nameOrOid.toLowerCase() );
        if ( val1 != null ) return (ASN1ObjectIdentifier) val1;
        return new ASN1ObjectIdentifier(nameOrOid);
    }

    @JRubyMethod(name="Boolean", module=true, rest=true)
    public static IRubyObject fact_Boolean(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "Boolean", args);
    }

    @JRubyMethod(name="Integer", module=true, rest=true)
    public static IRubyObject fact_Integer(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "Integer", args);
    }

    @JRubyMethod(name="Enumerated", module=true, rest=true)
    public static IRubyObject fact_Enumerated(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "Enumerated", args);
    }

    @JRubyMethod(name="BitString", module=true, rest=true)
    public static IRubyObject fact_BitString(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "BitString", args);
    }

    @JRubyMethod(name="OctetString", module=true, rest=true)
    public static IRubyObject fact_OctetString(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "OctetString", args);
    }

    @JRubyMethod(name="UTF8String", module=true, rest=true)
    public static IRubyObject fact_UTF8String(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "UTF8String", args);
    }

    @JRubyMethod(name="NumericString", module=true, rest=true)
    public static IRubyObject fact_NumericString(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "NumericString", args);
    }

    @JRubyMethod(name="PrintableString", module=true, rest=true)
    public static IRubyObject fact_PrintableString(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "PrintableString", args);
    }

    @JRubyMethod(name="T61String", module=true, rest=true)
    public static IRubyObject fact_T61String(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "T61String", args);
    }

    @JRubyMethod(name="VideotexString", module=true, rest=true)
    public static IRubyObject fact_VideotexString(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("VideotexString").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
    }

    @JRubyMethod(name="IA5String", module=true, rest=true)
    public static IRubyObject fact_IA5String(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("IA5String").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
    }

    @JRubyMethod(name="GraphicString", module=true, rest=true)
    public static IRubyObject fact_GraphicString(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("GraphicString").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
    }

    @JRubyMethod(name="ISO64String", module=true, rest=true)
    public static IRubyObject fact_ISO64String(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("ISO64String").callMethod(recv.getRuntime().getCurrentContext(),"new",args);
    }

    @JRubyMethod(name="GeneralString", module=true, rest=true)
    public static IRubyObject fact_GeneralString(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "GeneralString", args);
    }

    @JRubyMethod(name="UniversalString", module=true, rest=true)
    public static IRubyObject fact_UniversalString(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "UniversalString", args);
    }

    @JRubyMethod(name="BMPString", module=true, rest=true)
    public static IRubyObject fact_BMPString(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "BMPString", args);
    }

    @JRubyMethod(name="Nul", module=true, rest=true)
    public static IRubyObject fact_Null(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "Null", args);
    }

    @JRubyMethod(name="ObjectId", module=true, rest=true)
    public static IRubyObject fact_ObjectId(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "ObjectId", args);
    }

    @JRubyMethod(name="UTCTime", module=true, rest=true)
    public static IRubyObject fact_UTCTime(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "UTCTime", args);
    }

    @JRubyMethod(name="GeneralizedTime", module=true, rest=true)
    public static IRubyObject fact_GeneralizedTime(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "GeneralizedTime", args);
    }

    @JRubyMethod(name="Sequence", module=true, rest=true)
    public static IRubyObject fact_Sequence(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "Sequence", args);
    }

    @JRubyMethod(name="Set", module=true, rest=true)
    public static IRubyObject fact_Set(IRubyObject self, IRubyObject[] args) {
        return callClassNew(self, "Set", args);
    }

    private static IRubyObject callClassNew(final IRubyObject self, final String className, final IRubyObject[] args) {
        return ((RubyModule) self).getClass(className).callMethod(self.getRuntime().getCurrentContext(), "new", args);
    }

    @JRubyMethod(meta=true, required=1)
    public static IRubyObject traverse(final ThreadContext context, final IRubyObject self, IRubyObject arg) {
        warn(context, "WARNING: unimplemented method called: ASN1#traverse");
        return context.runtime.getNil();
    }

    public static class ObjectId {

        @JRubyMethod(meta = true, rest = true)
        public static IRubyObject register(final IRubyObject self, final IRubyObject[] args) {
            final Ruby runtime = self.getRuntime();
            final ASN1ObjectIdentifier derOid = new ASN1ObjectIdentifier( args[0].toString() );
            final String a1 = args[1].toString();
            final String a2 = args[2].toString();
            getOIDLookup(runtime).put(a1.toLowerCase(), derOid);
            getOIDLookup(runtime).put(a2.toLowerCase(), derOid);
            getSymLookup(runtime).put(derOid, a1);
            return runtime.getTrue();
        }

        @JRubyMethod(name = { "sn", "short_name" })
        public static IRubyObject sn(final ThreadContext context, final IRubyObject self) {
            final Ruby runtime = context.runtime;
            return runtime.newString( getShortNameFor(runtime, self.callMethod(context, "value").toString()) );
        }

        @JRubyMethod(name = { "ln", "long_name" })
        public static IRubyObject ln(final ThreadContext context, final IRubyObject self) {
            final Ruby runtime = context.runtime;
            return runtime.newString( getLongNameFor(runtime, self.callMethod(context, "value").toString()) );
        }

        @JRubyMethod
        public static IRubyObject oid(final ThreadContext context, final IRubyObject self) {
            final Ruby runtime = context.runtime;
            return runtime.newString( getObjectIdentifier(runtime, self.callMethod(context, "value").toString()).getId() );
        }

    }

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssz");

    private static IRubyObject decodeObject(final ThreadContext context, final RubyModule _ASN1, final Object obj)
        throws IOException, IllegalArgumentException {

        int ix = idForClass(obj.getClass());
        final String className = ix == -1 ? null : (String) ( ASN1_INFO[ix][2] );

        if ( className != null ) {
            final RubyClass klass = _ASN1.getClass(className);
            if ( obj instanceof DERBitString ) {
                final DERBitString derObj = (DERBitString) obj;
                ByteList bl = new ByteList(derObj.getBytes(), false);
                IRubyObject bString = klass.callMethod(context, "new", context.runtime.newString(bl));
                bString.callMethod(context, "unused_bits=", context.runtime.newFixnum( derObj.getPadBits() ));
                return bString;
            }
            else if ( obj instanceof ASN1String ) {
                final ByteList val;
                if ( obj instanceof DERUTF8String ) {
                    val = new ByteList(((DERUTF8String) obj).getString().getBytes("UTF-8"));
                } else {
                    val = ByteList.create(((ASN1String) obj).getString());
                }
                return klass.callMethod(context, "new", context.runtime.newString(val));
            }
            else if ( obj instanceof ASN1Sequence ) {
                RubyArray arr = decodeObjects(context, _ASN1, ((ASN1Sequence) obj).getObjects());
                return klass.callMethod(context, "new", arr);
            }
            else if ( obj instanceof ASN1Set ) {
                RubyArray arr = decodeObjects(context, _ASN1, ((ASN1Set) obj).getObjects());
                return klass.callMethod(context, "new", arr);
            }
            else if ( obj instanceof DERNull ) {
                return klass.callMethod(context,"new", context.runtime.getNil());
            }
            else if ( obj instanceof ASN1Integer ) {
                return klass.callMethod(context, "new", BN.newBN(context.runtime, ((ASN1Integer) obj).getValue()));
            }
            else if ( obj instanceof DERUTCTime ) {
                final Calendar calendar = Calendar.getInstance();
                try {
                    calendar.setTime( dateFormat.parse(((DERUTCTime) obj).getAdjustedTime()) );
                } catch (ParseException e) { throw new IOException(e); }
                IRubyObject[] argv = new IRubyObject[] {
                    context.runtime.newFixnum(calendar.get(Calendar.YEAR)),
                    context.runtime.newFixnum(calendar.get(Calendar.MONTH) + 1),
                    context.runtime.newFixnum(calendar.get(Calendar.DAY_OF_MONTH)),
                    context.runtime.newFixnum(calendar.get(Calendar.HOUR_OF_DAY)),
                    context.runtime.newFixnum(calendar.get(Calendar.MINUTE)),
                    context.runtime.newFixnum(calendar.get(Calendar.SECOND)),
                };
                return klass.callMethod(context, "new", context.runtime.getClass("Time").callMethod(context, "local", argv));
            }
            else if ( obj instanceof ASN1ObjectIdentifier ) {
                final String objId = ((ASN1ObjectIdentifier) obj).getId();
                return klass.callMethod(context, "new", context.runtime.newString(objId));
            }
            else if ( obj instanceof DEROctetString ) {
                final ByteList octets = new ByteList(((DEROctetString) obj).getOctets(), false);
                return klass.callMethod(context, "new", context.runtime.newString(octets));
            }
            else if ( obj instanceof DERBoolean ) {
                return klass.callMethod(context, "new", context.runtime.newBoolean( ((DERBoolean) obj).isTrue() ));
            }
            else {
                if ( isDebug(context.runtime) ) {
                    context.runtime.getOut().println("ASN1.decodeObject() should handle: " + obj.getClass().getName());
                }
            }
        }
        else if ( obj instanceof ASN1TaggedObject ) {
            final ASN1TaggedObject taggedObj = ((ASN1TaggedObject) obj);
            IRubyObject val = decodeObject(context, _ASN1, taggedObj.getObject());
            IRubyObject tag = context.runtime.newFixnum( taggedObj.getTagNo() );
            IRubyObject tag_class = context.runtime.newSymbol("CONTEXT_SPECIFIC");
            final RubyArray valArr = context.runtime.newArray(val);
            return _ASN1.getClass("ASN1Data").callMethod(context, "new",
                new IRubyObject[] { valArr, tag, tag_class }
            );
        }
        else if ( obj instanceof ASN1Sequence) {
            // Likely a DERSequence returned by bouncycastle libs. Convert to DLSequence.
            RubyArray arr = decodeObjects(context, _ASN1, ((ASN1Sequence) obj).getObjects());
            return _ASN1.getClass("Sequence").callMethod(context, "new", arr);
        }
        else if ( obj instanceof ASN1Set ) {
            // Likely a DERSet returned by bouncycastle libs. Convert to DLSet.
            RubyArray arr = decodeObjects(context, _ASN1, ((ASN1Set) obj).getObjects());
            return _ASN1.getClass("Set").callMethod(context, "new", arr);
        }

        //Used to return null. Led to confusing exceptions later.
        throw new IllegalArgumentException("jruby-openssl unable to decode object: " + obj + "[" + obj.getClass().getName() + "]");
    }

    private static RubyArray decodeObjects(final ThreadContext context, final RubyModule _ASN1, final Enumeration e)
        throws IOException {
        final RubyArray arr = context.runtime.newArray();
        while ( e.hasMoreElements() ) {
            arr.append( decodeObject(context, _ASN1, e.nextElement()) );
        }
        return arr;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject decode(final ThreadContext context,
        final IRubyObject self, final IRubyObject obj) {
        try {
            return decodeImpl(context, (RubyModule) self, obj);
        }
        catch (IOException e) {
            throw context.runtime.newIOErrorFromException(e);
        }
        catch (IllegalArgumentException e) {
            throw context.runtime.newArgumentError(e.getMessage());
        }
        catch (RuntimeException e) {
            throw Utils.newRuntimeError(context.runtime, e);
        }
    }

    static IRubyObject decodeImpl(final ThreadContext context, IRubyObject obj)
        throws IOException, IllegalArgumentException {
        return decodeImpl(context, _ASN1(context.runtime), obj);
    }

    static IRubyObject decodeImpl(final ThreadContext context,
        final RubyModule _ASN1, IRubyObject obj) throws IOException, IllegalArgumentException {
        obj = OpenSSLImpl.to_der_if_possible(context, obj);
        ASN1InputStream asis = new ASN1InputStream(obj.convertToString().getBytes());
        return decodeObject(context, _ASN1, asis.readObject());
    }

    @JRubyMethod(meta = true, required = 1)
    public static IRubyObject decode_all(final ThreadContext context, final IRubyObject self, IRubyObject arg) {
        warn(context, "WARNING: unimplemented method called: ASN1#decode_all");
        return context.runtime.getNil();
    }

    public static RaiseException newASN1Error(Ruby runtime, String message) {
        return Utils.newError(runtime, _ASN1(runtime).getClass("ASN1Error"), message, false);
    }

    private static RubyModule _ASN1(final Ruby runtime) {
        return (RubyModule) runtime.getModule("OpenSSL").getConstant("ASN1");
    }

    public static class ASN1Data extends RubyObject {
        private static final long serialVersionUID = 6117598347932209839L;

        static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new ASN1Data(runtime, klass);
            }
        };

        public ASN1Data(Ruby runtime, RubyClass type) {
            super(runtime,type);
        }

        @JRubyMethod(visibility = Visibility.PRIVATE)
        public IRubyObject initialize(final ThreadContext context,
            final IRubyObject value, final IRubyObject tag, final IRubyObject tag_class) {
            if ( ! (tag_class instanceof RubySymbol) ) {
                throw newASN1Error(context.runtime, "invalid tag class");
            }
            if ( tag_class.toString().equals(":UNIVERSAL") && RubyNumeric.fix2int(tag) > 31 ) {
                throw newASN1Error(context.runtime, "tag number for Universal too large");
            }
            this.callMethod(context,"tag=", tag);
            this.callMethod(context,"value=", value);
            this.callMethod(context,"tag_class=", tag_class);
            return this;
        }

        ASN1Encodable toASN1(final ThreadContext context) {
            final int tag = RubyNumeric.fix2int(callMethod(context, "tag"));
            final IRubyObject val = callMethod(context, "value");
            if ( val instanceof RubyArray ) {
                RubyArray arr = (RubyArray) callMethod(context, "value");
                if ( arr.size() > 1 ) {
                    ASN1EncodableVector vec = new ASN1EncodableVector();
                    for (IRubyObject obj : arr.toJavaArray()) {
                        vec.add(((ASN1Data)obj).toASN1());
                    }
                    return new DERTaggedObject(tag, new DLSequence(vec));
                } else {
                    return new DERTaggedObject(tag, ((ASN1Data)(arr.getList().get(0))).toASN1(context));
                }
            } else {
                return new DERTaggedObject(tag, ((ASN1Data) val).toASN1(context));
            }
        }

        @Deprecated
        final ASN1Encodable toASN1() {
            return toASN1( getRuntime().getCurrentContext() );
        }

        @JRubyMethod
        public IRubyObject to_der(final ThreadContext context) {
            try {
                final byte[] encoded = toASN1(context).toASN1Primitive().getEncoded(ASN1Encoding.DER);
                return context.runtime.newString(new ByteList(encoded ,false));
            }
            catch (IOException ex) {
                throw Utils.newError(context.runtime, "OpenSSL::ASN1::ASN1Error", ex.getMessage());
            }
        }

        protected IRubyObject defaultTag() {
            int i = idForRubyName(getMetaClass().getRealClass().getBaseName());
            if(i != -1) {
                return getRuntime().newFixnum(i);
            } else {
                return getRuntime().getNil();
            }
        }

        IRubyObject value() {
            return this.callMethod(getRuntime().getCurrentContext(), "value");
        }

        @Override
        public String toString() {
            return value().toString();
        }

        protected final void print() {
            print(0);
        }

        protected void print(int indent) {
            final PrintStream out = getRuntime().getOut();
            printIndent(out, indent);
            final IRubyObject value = value();
            out.println("ASN1Data: ");
            if ( value instanceof RubyArray ) {
                printArray(out, indent, (RubyArray) value);
            } else {
                ((ASN1Data) value).print(indent + 1);
            }
        }

        static void printIndent(final PrintStream out, final int indent) {
            for ( int i = 0; i < indent; i++) out.print(" ");
        }

        static void printArray(final PrintStream out, final int indent, final RubyArray array) {
            for ( int i = 0; i < array.size(); i++ ) {
                ((ASN1Data) array.entry(i)).print(indent + 1);
            }
        }

        static RaiseException createNativeRaiseException(final ThreadContext context, final Exception e) {
            Throwable cause = e.getCause(); if ( cause == null ) cause = e;
            return RaiseException.createNativeRaiseException(context.runtime, cause);
        }

    }

    public static class ASN1Primitive extends ASN1Data {
        private static final long serialVersionUID = 8489625559339190259L;

        static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new ASN1Primitive(runtime, klass);
            }
        };

        public ASN1Primitive(Ruby runtime, RubyClass type) {
            super(runtime,type);
        }

        @Override
        @JRubyMethod
        public IRubyObject to_der(final ThreadContext context) {
            return super.to_der(context);
        }

        @JRubyMethod(required=1, optional=4, visibility = Visibility.PRIVATE)
        public IRubyObject initialize(final ThreadContext context, final IRubyObject[] args) {
            final Ruby runtime = context.runtime;
            IRubyObject value = args[0];
            final IRubyObject tag;
            IRubyObject tagging = runtime.getNil();
            IRubyObject tag_class = runtime.getNil();

            if ( args.length > 1 ) {
                tag = args[1];
                if ( args.length > 2 ) {
                    tagging = args[2];
                    if ( args.length > 3 ) tag_class = args[3];
                }

                if ( tag.isNil() ) throw newASN1Error(runtime, "must specify tag number");

                if ( tagging.isNil() ) {
                    tagging = runtime.newSymbol("EXPLICIT");
                }

                if ( ! (tagging instanceof RubySymbol) ) {
                    throw newASN1Error(runtime, "invalid tag default");
                }

                if ( tag_class.isNil() ) {
                    tag_class = runtime.newSymbol("CONTEXT_SPECIFIC");
                }

                if ( ! (tag_class instanceof RubySymbol) ) {
                    throw newASN1Error(runtime, "invalid tag class");
                }

                if ( tagging.toString().equals(":IMPLICIT") && RubyNumeric.fix2int(tag) > 31 ) {
                    throw newASN1Error(runtime, "tag number for Universal too large");
                }
            }
            else {
                tag = defaultTag();
                tag_class = runtime.newSymbol("UNIVERSAL");
            }
            if ( "ObjectId".equals( getMetaClass().getRealClass().getBaseName() ) ) {
                String v = getSymLookup(runtime).get( getObjectIdentifier(runtime, value.toString()) );
                if ( v != null ) value = runtime.newString(v);
            }

            this.callMethod(context, "tag=", tag);
            this.callMethod(context, "value=", value);
            this.callMethod(context, "tagging=", tagging);
            this.callMethod(context, "tag_class=", tag_class);
            return this;
        }

        @Override
        ASN1Encodable toASN1(final ThreadContext context) {
            final int tag = idForRubyName(getMetaClass().getRealClass().getBaseName());
            @SuppressWarnings("unchecked")
            Class<? extends ASN1Encodable> impl = (Class<? extends ASN1Encodable>) ASN1_INFO[tag][1];

            final IRubyObject val = callMethod(context, "value");
            if ( impl == ASN1ObjectIdentifier.class ) {
                return getObjectIdentifier(context.runtime, val.toString());
            }
            else if ( impl == DERNull.class ) {
                return new DERNull();
            }
            else if ( impl == DERBoolean.class ) {
                return new DERBoolean(val.isTrue());
            }
            else if ( impl == DERUTCTime.class ) {
                return new DERUTCTime(((RubyTime) val).getJavaDate());
            }
            else if ( impl == ASN1Integer.class && val instanceof RubyBignum ) {
                return new ASN1Integer(((RubyBignum) val).getValue());
            }
            else if ( impl == ASN1Integer.class ) {
                return new ASN1Integer(new BigInteger(val.toString()));
            }
            else if ( impl == DEROctetString.class ) {
                return new DEROctetString(val.convertToString().getBytes());
            }
            else if ( impl == DERBitString.class ) {
                final byte[] bs = val.convertToString().getBytes();
                int unused = 0;
                for ( int i = (bs.length - 1); i > -1; i-- ) {
                    if (bs[i] == 0) unused += 8;
                    else {
                        byte v2 = bs[i];
                        int x = 8;
                        while ( v2 != 0 ) {
                            v2 <<= 1;
                            x--;
                        }
                        unused += x;
                        break;
                    }
                }
                return new DERBitString(bs,unused);
            }
            else if ( val instanceof RubyString ) {
                try {
                    return impl.getConstructor(String.class).newInstance(val.toString());
                }
                catch (Exception e) {
                    throw createNativeRaiseException(context, e);
                }
            }

            // TODO throw an exception here too?
            if ( isDebug(context.runtime) ) {
                context.runtime.getOut().println("object with tag: " + tag + " and value: " + val + " and val.class: " + val.getClass().getName() + " and impl: " + impl.getName());
            }
            warn(context, "WARNING: unimplemented method called: asn1data#toASN1 (" + impl + ")");
            return null;
        }

        @Override
        protected void print(int indent) {
            final PrintStream out = getRuntime().getOut();
            printIndent(out, indent);
            out.print(getMetaClass().getRealClass().getBaseName());
            out.print(": ");
            out.println(value().callMethod(getRuntime().getCurrentContext(), "inspect").toString());
        }

    }

    public static class ASN1Constructive extends ASN1Data {
        private static final long serialVersionUID = -7166662655104776828L;

        static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new ASN1Constructive(runtime, klass);
            }
        };

        public ASN1Constructive(Ruby runtime, RubyClass type) {
            super(runtime,type);
        }

        @Override
        @JRubyMethod
        public IRubyObject to_der(final ThreadContext context) {
            return super.to_der(context);
        }

        @JRubyMethod(required=1, optional=3, visibility = Visibility.PRIVATE)
        public IRubyObject initialize(final ThreadContext context, final IRubyObject[] args) {
            final Ruby runtime = context.runtime;

            final IRubyObject value = args[0];
            final IRubyObject tag;
            IRubyObject tagging = runtime.getNil();
            IRubyObject tag_class = runtime.getNil();

            if ( args.length > 1 ) {
                tag = args[1];
                if ( args.length > 2 ) {
                    tagging = args[2];
                    if ( args.length > 3 ) tag_class = args[3];
                }

                if ( tag.isNil() ) throw newASN1Error(runtime, "must specify tag number");

                if ( tagging.isNil() ) {
                    tagging = runtime.newSymbol("EXPLICIT");
                }

                if ( ! (tagging instanceof RubySymbol) ) {
                    throw newASN1Error(runtime, "invalid tag default");
                }

                if ( tag_class.isNil() ) {
                    tag_class = runtime.newSymbol("CONTEXT_SPECIFIC");
                }

                if ( ! (tag_class instanceof RubySymbol) ) {
                    throw newASN1Error(runtime, "invalid tag class");
                }

                if ( tagging.toString().equals(":IMPLICIT") && RubyNumeric.fix2int(tag) > 31 ) {
                    throw newASN1Error(runtime, "tag number for Universal too large");
                }
            }
            else {
                tag = defaultTag();
                tag_class = runtime.newSymbol("UNIVERSAL");
            }

            callMethod(context, "tag=", tag);
            callMethod(context, "value=", value);
            callMethod(context, "tagging=", tagging);
            callMethod(context, "tag_class=", tag_class);

            return this;
        }

        @Override
        ASN1Encodable toASN1(final ThreadContext context) {
            final int id = idForRubyName(getMetaClass().getRealClass().getBaseName());
            if ( id != -1 ) {
                final ASN1EncodableVector vec = new ASN1EncodableVector();
                final RubyArray value = value(context);
                for ( int i = 0; i < value.size(); i++ ) {
                    final IRubyObject entry = value.entry(i);
                    try {
                        if ( entry instanceof ASN1Data) {
                            vec.add( ( (ASN1Data) entry ).toASN1(context) );
                        } else {
                            vec.add( ( (ASN1Data) decodeImpl(context, entry) ).toASN1(context) );
                        }
                    }
                    catch (Exception e) { // TODO: deprecated
                        throw createNativeRaiseException(context, e);
                    }
                }
                try {
                    @SuppressWarnings("unchecked")
                    ASN1Encodable result = ((Class<? extends ASN1Encodable>)
                        ( ASN1_INFO[id][1]) ).
                            getConstructor(new Class[] { ASN1EncodableVector.class }).
                            newInstance(new Object[] { vec });
                    return result;
                }
                catch (Exception e) { // TODO: deprecated
                    throw createNativeRaiseException(context, e);
                }
            }
            return null;
        }

        @JRubyMethod
        public IRubyObject each(final ThreadContext context, final Block block) {
            final RubyArray value = value(context);
            for ( int i = 0; i < value.size(); i++ ) {
                block.yield(context, value.entry(i));
            }
            return context.runtime.getNil();
        }

        @Override
        protected void print(int indent) {
            final PrintStream out = getRuntime().getOut();
            printIndent(out, indent);
            out.print(getMetaClass().getRealClass().getBaseName()); out.println(": ");
            printArray( out, indent, value( getRuntime().getCurrentContext() ) );
        }

        private RubyArray value(final ThreadContext context) {
            return (RubyArray) this.callMethod(context, "value");
        }

    }
}// ASN1
