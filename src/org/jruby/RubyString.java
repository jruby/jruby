/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2005 Tim Azzopardi <tim@tigerfive.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
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
package org.jruby;

import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.anno.FrameField.BACKREF;
import static org.jruby.util.StringSupport.CR_7BIT;
import static org.jruby.util.StringSupport.CR_BROKEN;
import static org.jruby.util.StringSupport.CR_MASK;
import static org.jruby.util.StringSupport.CR_UNKNOWN;
import static org.jruby.util.StringSupport.CR_VALID;
import static org.jruby.util.StringSupport.codeLength;
import static org.jruby.util.StringSupport.codePoint;
import static org.jruby.util.StringSupport.codeRangeScan;
import static org.jruby.util.StringSupport.searchNonAscii;
import static org.jruby.util.StringSupport.strLengthWithCodeRange;
import static org.jruby.util.StringSupport.toLower;
import static org.jruby.util.StringSupport.toUpper;
import static org.jruby.util.StringSupport.unpackArg;
import static org.jruby.util.StringSupport.unpackResult;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

import org.jcodings.Encoding;
import org.jcodings.EncodingDB.Entry;
import org.jcodings.ascii.AsciiTables;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.joni.Matcher;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Region;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.java.MiniJava;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.ByteList;
import org.jruby.util.Numeric;
import org.jruby.util.Pack;
import org.jruby.util.Sprintf;
import org.jruby.util.StringSupport;
import org.jruby.util.TypeConverter;
import org.jruby.util.string.JavaCrypt;

/**
 * Implementation of Ruby String class
 * 
 * Concurrency: no synchronization is required among readers, but
 * all users must synchronize externally with writers.
 *
 */
@JRubyClass(name="String", include={"Enumerable", "Comparable"})
public class RubyString extends RubyObject implements EncodingCapable {
    private static final ASCIIEncoding ASCII = ASCIIEncoding.INSTANCE;

    // string doesn't share any resources
    private static final int SHARE_LEVEL_NONE = 0;
    // string has it's own ByteList, but it's pointing to a shared buffer (byte[])
    private static final int SHARE_LEVEL_BUFFER = 1;
    // string doesn't have it's own ByteList (values)
    private static final int SHARE_LEVEL_BYTELIST = 2;

    private volatile int shareLevel = SHARE_LEVEL_NONE;

    private ByteList value;

    private static ObjectAllocator STRING_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return RubyString.newEmptyString(runtime, klass);
        }
    };

    public static RubyClass createStringClass(Ruby runtime) {
        RubyClass stringClass = runtime.defineClass("String", runtime.getObject(), STRING_ALLOCATOR);
        runtime.setString(stringClass);
        stringClass.index = ClassIndex.STRING;
        stringClass.kindOf = new RubyModule.KindOf() {
            @Override
                public boolean isKindOf(IRubyObject obj, RubyModule type) {
                    return obj instanceof RubyString;
                }
            };
            
        stringClass.includeModule(runtime.getComparable());
        stringClass.includeModule(runtime.getEnumerable());
        stringClass.defineAnnotatedMethods(RubyString.class);
        
        return stringClass;
    }

    public Encoding getEncoding() {
        return value.encoding;
    }

    public void associateEncoding(Encoding enc) {
        if (value.encoding != enc) {
            if (!isCodeRangeAsciiOnly() || !enc.isAsciiCompatible()) clearCodeRange();
            value.encoding = enc;
        }
    }

    public final void setEncodingAndCodeRange(Encoding enc, int cr) {
        value.encoding = enc;
        setCodeRange(cr);
    }

    public final Encoding toEncoding(Ruby runtime) {
        if (!value.encoding.isAsciiCompatible()) {
            throw runtime.newArgumentError("invalid name encoding (non ASCII)");
        }
        Entry entry = runtime.getEncodingService().findEncodingOrAliasEntry(value);
        if (entry == null) {
            throw runtime.newArgumentError("unknown encoding name - " + value);
        }
        return entry.getEncoding();
    }

    public final int getCodeRange() {
        return flags & CR_MASK;
    }

    public final void setCodeRange(int codeRange) {
        flags |= codeRange & CR_MASK;
    }

    public final void clearCodeRange() {
        flags &= ~CR_MASK;
    }

    private void keepCodeRange() {
        if (getCodeRange() == CR_BROKEN) clearCodeRange();
    }

    public final boolean isCodeRangeAsciiOnly() {
        return getCodeRange() == CR_7BIT;
    }

    public final boolean isCodeRangeValid() {
        return (flags & CR_VALID) != 0;
    }

    public final boolean isCodeRangeBroken() {
        return (flags & CR_BROKEN) != 0;
    }

    static int codeRangeAnd(int cr1, int cr2) {
        if (cr1 == CR_7BIT) return cr2;
        if (cr1 == CR_VALID) return cr2 == CR_7BIT ? CR_VALID : cr2;
        return CR_UNKNOWN;
    }

    private void copyCodeRangeForSubstr(RubyString from) {
        Encoding enc = value.encoding = from.value.encoding;
        int fromCr = from.getCodeRange();
        if (fromCr == CR_7BIT) {
            setCodeRange(fromCr);
        } else if (fromCr == CR_VALID) {
            if (!enc.isAsciiCompatible() || searchNonAscii(value) != -1) {
                setCodeRange(CR_VALID);
            } else {
                setCodeRange(CR_7BIT);
            }
        } else{ 
            if (value.realSize == 0) {
                setCodeRange(!enc.isAsciiCompatible() ? CR_VALID : CR_7BIT);
            }
        }
    }

    private void copyCodeRange(RubyString from) {
        value.encoding = from.value.encoding;
        setCodeRange(from.getCodeRange());
    }

    final int scanForCodeRange() {
        int cr = getCodeRange();
        if (cr == CR_UNKNOWN) {
            cr = codeRangeScan(value.encoding, value);
            setCodeRange(cr);
        }
        return cr;
    }

    private boolean singleByteOptimizable() {
        return getCodeRange() == CR_7BIT || value.encoding.isSingleByte();
    }

    private boolean singleByteOptimizable(Encoding enc) {
        return getCodeRange() == CR_7BIT || enc.isSingleByte();
    }

    private Encoding isCompatibleWith(RubyString other) { 
        Encoding enc1 = value.encoding;;
        Encoding enc2 = other.value.encoding;

        if (enc1 == enc2) return enc1;

        if (other.getByteList().realSize == 0) return enc1;
        if (getByteList().realSize == 0) return enc2;

        if (!enc1.isAsciiCompatible() || !enc2.isAsciiCompatible()) return null;

        return RubyEncoding.areCompatible(enc1, scanForCodeRange(), enc2, other.scanForCodeRange());
    }

    final Encoding checkEncoding(RubyString other) {
        Encoding enc = isCompatibleWith(other);
        if (enc == null) throw getRuntime().newArgumentError("incompatible character encodings: " + 
                                value.encoding + " and " + other.value.encoding);
        return enc;
    }

    private Encoding checkDummyEncoding() {
        Encoding enc = value.encoding;
        if (enc.isDummy()) throw getRuntime().newEncodingCompatibilityError(
                "incompatible encoding with this operation: " + enc);
        return enc;
    }

    private boolean isComparableWith(RubyString other) {
        ByteList otherValue = other.value;
        if (value.encoding == otherValue.encoding || 
            value.realSize == 0 || otherValue.realSize == 0) return true;
        return isComparableViaCodeRangeWith(other);
    }

    private boolean isComparableViaCodeRangeWith(RubyString other) {
        int cr1 = scanForCodeRange();
        int cr2 = other.scanForCodeRange();

        if (cr1 == CR_7BIT && (cr2 == CR_7BIT || other.value.encoding.isAsciiCompatible())) return true;
        if (cr2 == CR_7BIT && value.encoding.isAsciiCompatible()) return true;
        return false;
    }

    private int strLength(Encoding enc) {
        if (singleByteOptimizable()) return value.realSize;
        value.encoding = enc;
        return strLength(value);
    }

    private int strLength() {
        if (singleByteOptimizable()) return value.realSize;
        return strLength(value);
    }

    private int strLength(ByteList bytes) {
        long lencr = strLengthWithCodeRange(value);
        int cr = unpackArg(lencr);
        if (cr != 0) setCodeRange(cr);
        return unpackResult(lencr);
    }

    /** short circuit for String key comparison
     * 
     */
    @Override
    public final boolean eql(IRubyObject other) {
        Ruby runtime = getRuntime();
        if (other.getMetaClass() != runtime.getString()) return super.eql(other);
        return runtime.is1_9() ? eql19(runtime, other) : eql18(runtime, other);
    }

    private boolean eql18(Ruby runtime, IRubyObject other) {
        return value.equal(((RubyString)other).value);
    }

    private boolean eql19(Ruby runtime, IRubyObject other) {
        RubyString otherString = (RubyString)other;
        return isComparableWith(otherString) && value.equal(((RubyString)other).value);
    }

    public RubyString(Ruby runtime, RubyClass rubyClass, CharSequence value) {
        super(runtime, rubyClass);
        assert value != null;
        this.value = new ByteList(ByteList.plain(value), false);
    }

    public RubyString(Ruby runtime, RubyClass rubyClass, byte[] value) {
        super(runtime, rubyClass);
        assert value != null;
        this.value = new ByteList(value);
    }

    public RubyString(Ruby runtime, RubyClass rubyClass, ByteList value) {
        super(runtime, rubyClass);
        assert value != null;
        this.value = value;
    }
    
    public RubyString(Ruby runtime, RubyClass rubyClass, ByteList value, boolean objectSpace) {
        super(runtime, rubyClass, objectSpace);
        assert value != null;
        this.value = value;
    }
    
    protected RubyString(Ruby runtime, RubyClass rubyClass, ByteList value, Encoding enc, int cr) {
        this(runtime, rubyClass, value);
        value.encoding = enc;
        flags |= cr;
    }
    
    protected RubyString(Ruby runtime, RubyClass rubyClass, ByteList value, Encoding enc) {
        this(runtime, rubyClass, value);
        value.encoding = enc;
    }

    // Deprecated String construction routines
    /** Create a new String which uses the same Ruby runtime and the same
     *  class like this String.
     *
     *  This method should be used to satisfy RCR #38.
     *  @deprecated  
     */
    @Deprecated
    public RubyString newString(CharSequence s) {
        return new RubyString(getRuntime(), getType(), s);
    }

    /** Create a new String which uses the same Ruby runtime and the same
     *  class like this String.
     *
     *  This method should be used to satisfy RCR #38.
     *  @deprecated
     */
    @Deprecated
    public RubyString newString(ByteList s) {
        return new RubyString(getRuntime(), getMetaClass(), s);
    }

    @Deprecated
    public static RubyString newString(Ruby runtime, RubyClass clazz, CharSequence str) {
        return new RubyString(runtime, clazz, str);
    }

    public static RubyString newStringLight(Ruby runtime, ByteList bytes) {
        return new RubyString(runtime, runtime.getString(), bytes, false);
    }

    // String construction routines by copying byte[] buffer   
    public static RubyString newString(Ruby runtime, CharSequence str) {
        return new RubyString(runtime, runtime.getString(), str);
    }
    
    public static RubyString newString(Ruby runtime, byte[] bytes) {
        return new RubyString(runtime, runtime.getString(), bytes);
    }

    public static RubyString newString(Ruby runtime, byte[] bytes, int start, int length) {
        byte[] copy = new byte[length];
        System.arraycopy(bytes, start, copy, 0, length);
        return new RubyString(runtime, runtime.getString(), new ByteList(copy, false));
    }

    public static RubyString newString(Ruby runtime, ByteList bytes) {
        return new RubyString(runtime, runtime.getString(), bytes);
    }
    
    public static RubyString newUnicodeString(Ruby runtime, String str) {
        try {
            return new RubyString(runtime, runtime.getString(), new ByteList(str.getBytes("UTF8"), false));
        } catch (UnsupportedEncodingException uee) {
            return new RubyString(runtime, runtime.getString(), str);
        }
    }

    // String construction routines by NOT byte[] buffer and making the target String shared 
    public static RubyString newStringShared(Ruby runtime, RubyString orig) {
        orig.shareLevel = SHARE_LEVEL_BYTELIST;
        RubyString str = new RubyString(runtime, runtime.getString(), orig.value);
        str.shareLevel = SHARE_LEVEL_BYTELIST;
        return str;
    }       

    public static RubyString newStringShared(Ruby runtime, ByteList bytes) {
        return newStringShared(runtime, runtime.getString(), bytes);
    }    

    public static RubyString newStringShared(Ruby runtime, RubyClass clazz, ByteList bytes) {
        RubyString str = new RubyString(runtime, clazz, bytes);
        str.shareLevel = SHARE_LEVEL_BYTELIST;
        return str;
    }

    public static RubyString newStringShared(Ruby runtime, byte[] bytes) {
        return newStringShared(runtime, new ByteList(bytes, false));
    }

    public static RubyString newStringShared(Ruby runtime, byte[] bytes, int start, int length) {
        return newStringShared(runtime, new ByteList(bytes, start, length, false));
    }

    public static RubyString newEmptyString(Ruby runtime) {
        return newEmptyString(runtime, runtime.getString());
    }

    public static RubyString newEmptyString(Ruby runtime, RubyClass metaClass) {
        RubyString empty = new RubyString(runtime, metaClass, ByteList.EMPTY_BYTELIST);
        empty.shareLevel = SHARE_LEVEL_BYTELIST;
        return empty;
    }

    // String construction routines by NOT byte[] buffer and NOT making the target String shared 
    public static RubyString newStringNoCopy(Ruby runtime, ByteList bytes) {
        return newStringNoCopy(runtime, runtime.getString(), bytes);
    }    

    public static RubyString newStringNoCopy(Ruby runtime, RubyClass clazz, ByteList bytes) {
        return new RubyString(runtime, clazz, bytes);
    }    

    public static RubyString newStringNoCopy(Ruby runtime, byte[] bytes, int start, int length) {
        return newStringNoCopy(runtime, new ByteList(bytes, start, length, false));
    }

    public static RubyString newStringNoCopy(Ruby runtime, byte[] bytes) {
        return newStringNoCopy(runtime, new ByteList(bytes, false));
    }

    /** Encoding aware String construction routines for 1.9
     * 
     */
    private static final ByteList EMPTY_BYTELISTS[] = new ByteList[4];
    static ByteList getEmptyByteList(Encoding enc) {
        int index = enc.getIndex();
        ByteList bytes;
        if (index < EMPTY_BYTELISTS.length && (bytes = EMPTY_BYTELISTS[index]) != null) {
            return bytes;
        }
        return prepareEmptyByteList(enc);
    }

    private static ByteList prepareEmptyByteList(Encoding enc) {
        int index = enc.getIndex();
        if (index >= EMPTY_BYTELISTS.length) {
            ByteList tmp[] = new ByteList[index + 4];
            System.arraycopy(EMPTY_BYTELISTS,0, tmp, 0, EMPTY_BYTELISTS.length);
        }
        return EMPTY_BYTELISTS[index] = new ByteList(ByteList.NULL_ARRAY, enc);
    }

    public static RubyString newEmptyString(Ruby runtime, RubyClass metaClass, Encoding enc) {
        RubyString empty = new RubyString(runtime, metaClass, getEmptyByteList(enc));
        empty.shareLevel = SHARE_LEVEL_BYTELIST;
        return empty;
    }

    public static RubyString newEmptyString(Ruby runtime, Encoding enc) {
        return newEmptyString(runtime, runtime.getString(), enc);
    }

    public static RubyString newStringNoCopy(Ruby runtime, RubyClass clazz, ByteList bytes, Encoding enc, int cr) {
        return new RubyString(runtime, clazz, bytes, enc, cr);
    }

    public static RubyString newStringNoCopy(Ruby runtime, ByteList bytes, Encoding enc, int cr) {
        return newStringNoCopy(runtime, runtime.getString(), bytes, enc, cr);
    }

    public static RubyString newUsAsciiStringNoCopy(Ruby runtime, ByteList bytes) {
        return newStringNoCopy(runtime, bytes, USASCIIEncoding.INSTANCE, CR_7BIT);
    }

    public static RubyString newUsAsciiStringShared(Ruby runtime, ByteList bytes) {
        RubyString str = newStringNoCopy(runtime, bytes, USASCIIEncoding.INSTANCE, CR_7BIT);
        str.shareLevel = SHARE_LEVEL_BYTELIST;
        return str;
    }
    
    public static RubyString newUsAsciiStringShared(Ruby runtime, byte[] bytes, int start, int length) {
        byte[] copy = new byte[length];
        System.arraycopy(bytes, start, copy, 0, length);
        return newUsAsciiStringShared(runtime, new ByteList(copy, false));
    }

    @Override
    public int getNativeTypeIndex() {
        return ClassIndex.STRING;
    }

    @Override
    public Class getJavaClass() {
        return String.class;
    }

    @Override
    public RubyString convertToString() {
        return this;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    /** rb_str_dup
     * 
     */
    @Deprecated
    public final RubyString strDup() {
        return strDup(getRuntime(), getMetaClass());
    }
    
    public final RubyString strDup(Ruby runtime) {
        return strDup(runtime, getMetaClass());
    }
    
    @Deprecated
    final RubyString strDup(RubyClass clazz) {
        return strDup(getRuntime(), getMetaClass());
    }

    final RubyString strDup(Ruby runtime, RubyClass clazz) {
        shareLevel = SHARE_LEVEL_BYTELIST;
        RubyString dup = new RubyString(runtime, clazz, value);
        dup.shareLevel = SHARE_LEVEL_BYTELIST;
        dup.flags |= flags & (CR_MASK | TAINTED_F);

        return dup;
    }

    public final RubyString makeShared(Ruby runtime, int index, int len) {
        if (len == 0) {
            RubyString s = newEmptyString(runtime, getMetaClass());
            s.infectBy(this);
            return s;
        }

        if (shareLevel == SHARE_LEVEL_NONE) shareLevel = SHARE_LEVEL_BUFFER;
        RubyString shared = new RubyString(runtime, getMetaClass(), value.makeShared(index, len));
        shared.shareLevel = SHARE_LEVEL_BUFFER;

        shared.infectBy(this);
        return shared;
    }

    final void modifyCheck() {
        if ((flags & FROZEN_F) != 0) throw getRuntime().newFrozenError("string");

        if (!isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw getRuntime().newSecurityError("Insecure: can't modify string");
        }
    }
    
    private final void modifyCheck(byte[] b, int len) {
        if (value.bytes != b || value.realSize != len) throw getRuntime().newRuntimeError("string modified");
    }
    
    private final void frozenCheck() {
        if (isFrozen()) throw getRuntime().newRuntimeError("string frozen");
    }

    /** rb_str_modify
     * 
     */
    public final void modify() {
        modifyCheck();

        if (shareLevel != SHARE_LEVEL_NONE) {
            if (shareLevel == SHARE_LEVEL_BYTELIST) {
                value = value.dup();
            } else {
                value.unshare();
            }
            shareLevel = SHARE_LEVEL_NONE;
        }

        value.invalidate();
    }

    public final void modify19() {
        modify();
        clearCodeRange();
    }

    private void modifyAndKeepCodeRange() {
        modify();
        keepCodeRange();
    }
    
    /** rb_str_modify (with length bytes ensured)
     * 
     */    
    public final void modify(int length) {
        modifyCheck();

        if (shareLevel != SHARE_LEVEL_NONE) {
            if (shareLevel == SHARE_LEVEL_BYTELIST) {
                value = value.dup(length);
            } else {
                value.unshare(length);
            }
            shareLevel = SHARE_LEVEL_NONE;
        } else {
            value.ensure(length);
        }

        value.invalidate();
    }
    
    public final void modify19(int length) {
        modify(length);
        clearCodeRange();
    }
    
    private final void view(ByteList bytes) {
        modifyCheck();

        value = bytes;
        shareLevel = SHARE_LEVEL_NONE;
    }

    private final void view(byte[]bytes) {
        modifyCheck();        

        value.replace(bytes);
        shareLevel = SHARE_LEVEL_NONE;

        value.invalidate();        
    }

    private final void view(int index, int len) {
        modifyCheck();

        if (shareLevel != SHARE_LEVEL_NONE) {
            if (shareLevel == SHARE_LEVEL_BYTELIST) {
                // if len == 0 then shared empty
                value = value.makeShared(index, len);
                shareLevel = SHARE_LEVEL_BUFFER;
            } else {
                value.view(index, len);
            }
        } else {        
            value.view(index, len);
            // FIXME this below is temporary, but its much safer for COW (it prevents not shared Strings with begin != 0)
            // this allows now e.g.: ByteList#set not to be begin aware
            shareLevel = SHARE_LEVEL_BUFFER;
        }

        value.invalidate();
    }

    public static String bytesToString(byte[] bytes, int beg, int len) {
        return new String(ByteList.plain(bytes, beg, len));
    }

    public static String byteListToString(ByteList bytes) {
        return bytesToString(bytes.unsafeBytes(), bytes.begin(), bytes.length());
    }

    public static String bytesToString(byte[] bytes) {
        return bytesToString(bytes, 0, bytes.length);
    }

    public static byte[] stringToBytes(String string) {
        return ByteList.plain(string);
    }

    @Override
    public RubyString asString() {
        return this;
    }

    @Override
    public IRubyObject checkStringType() {
        return this;
    }

    @JRubyMethod(name = {"to_s", "to_str"})
    @Override
    public IRubyObject to_s() {
        Ruby runtime = getRuntime();
        if (getMetaClass().getRealClass() != runtime.getString()) {
            return strDup(runtime, runtime.getString());
        }
        return this;
    }

    /* rb_str_cmp_m */
    @JRubyMethod(name = "<=>", compat = CompatVersion.RUBY1_8)
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) {
            return context.getRuntime().newFixnum(op_cmp((RubyString)other));
        }
        return op_cmpCommon(context, other);
    }

    @JRubyMethod(name = "<=>", compat = CompatVersion.RUBY1_9)
    public IRubyObject op_cmp19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) {
            return context.getRuntime().newFixnum(op_cmp19((RubyString)other));
        }
        return op_cmpCommon(context, other);
    }

    private IRubyObject op_cmpCommon(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.getRuntime();
        // deal with case when "other" is not a string
        if (other.respondsTo("to_str") && other.respondsTo("<=>")) {
            IRubyObject result = other.callMethod(context, "<=>", this);
            if (result.isNil()) return result;
            if (result instanceof RubyFixnum) {
                return RubyFixnum.newFixnum(runtime, -((RubyFixnum)result).getLongValue());
            } else {
                return RubyFixnum.zero(runtime).callMethod(context, "-", result);
            }
        }
        return runtime.getNil();        
    }
        
    /** rb_str_equal
     * 
     */
    @JRubyMethod(name = "==", compat = CompatVersion.RUBY1_8)
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.getRuntime();
        if (this == other) return runtime.getTrue();
        if (other instanceof RubyString) {
            return value.equal(((RubyString)other).value) ? runtime.getTrue() : runtime.getFalse();    
        }
        return op_equalCommon(context, other);
    }

    @JRubyMethod(name = "==", compat = CompatVersion.RUBY1_9)
    public IRubyObject op_equal19(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.getRuntime();
        if (this == other) return runtime.getTrue();
        if (other instanceof RubyString) {
            RubyString otherString = (RubyString)other;
            return isComparableWith(otherString) && value.equal(otherString.value) ? runtime.getTrue() : runtime.getFalse();    
        }
        return op_equalCommon(context, other);
    }

    private IRubyObject op_equalCommon(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.getRuntime();
        if (!other.respondsTo("to_str")) return runtime.getFalse();
        return other.callMethod(context, "==", this).isTrue() ? runtime.getTrue() : runtime.getFalse();
    }

    @JRubyMethod(name = "+", required = 1, compat = CompatVersion.RUBY1_8)
    public IRubyObject op_plus(ThreadContext context, IRubyObject other) {
        RubyString str = other.convertToString();
        RubyString resultStr = newString(context.getRuntime(), addByteLists(value, str.value));
        if (isTaint() || str.isTaint()) resultStr.setTaint(true);
        return resultStr;
    }

    @JRubyMethod(name = "+", required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject op_plus19(ThreadContext context, IRubyObject other) {
        RubyString str = other.convertToString();
        Encoding enc = checkEncoding(str);
        RubyString resultStr = newStringNoCopy(context.getRuntime(), addByteLists(value, str.value),
                                    enc, codeRangeAnd(getCodeRange(), str.getCodeRange()));
        if (isTaint() || str.isTaint()) resultStr.setTaint(true);
        return resultStr;
    }

    private ByteList addByteLists(ByteList value1, ByteList value2) {
        ByteList result = new ByteList(value1.realSize + value2.realSize);
        result.realSize = value1.realSize + value2.realSize;
        System.arraycopy(value1.bytes, value1.begin, result.bytes, 0, value1.realSize);
        System.arraycopy(value2.bytes, value2.begin, result.bytes, value1.realSize, value2.realSize);
        return result;
    }

    @JRubyMethod(name = "*", required = 1, compat = CompatVersion.RUBY1_8)
    public IRubyObject op_mul(ThreadContext context, IRubyObject other) {
        return multiplyByteList(context, other);
    }

    @JRubyMethod(name = "*", required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject op_mul19(ThreadContext context, IRubyObject other) {
        RubyString result = multiplyByteList(context, other);
        result.copyCodeRangeForSubstr(this);
        return result;
    }

    private RubyString multiplyByteList(ThreadContext context, IRubyObject arg) {
        int len = RubyNumeric.num2int(arg);
        if (len < 0) throw context.getRuntime().newArgumentError("negative argument");

        // we limit to int because ByteBuffer can only allocate int sizes
        if (len > 0 && Integer.MAX_VALUE / len < value.realSize) {
            throw context.getRuntime().newArgumentError("argument too big");
        }

        ByteList bytes = new ByteList(len *= value.realSize);
        if (len > 0) {
            bytes.realSize = len;
            int n = value.realSize;
            System.arraycopy(value.bytes, value.begin, bytes.bytes, 0, n);
            while (n <= len >> 1) {
                System.arraycopy(bytes.bytes, 0, bytes.bytes, n, n);
                n <<= 1;
            }
            System.arraycopy(bytes.bytes, 0, bytes.bytes, n, len - n);
        }
        RubyString result = new RubyString(context.getRuntime(), getMetaClass(), bytes);
        result.setTaint(isTaint());
        return result;
    }

    @JRubyMethod(name = "%", required = 1)
    public IRubyObject op_format(ThreadContext context, IRubyObject arg) {
        final RubyString s;

        IRubyObject tmp = arg.checkArrayType();
        if (tmp.isNil()) {
            tmp = arg;
        }

        // FIXME: Should we make this work with platform's locale,
        // or continue hardcoding US?
        s = Sprintf.sprintf(context.getRuntime(), Locale.US, value, tmp);

        s.infectBy(this);
        return s;
    }

    @JRubyMethod(name = "hash")
    @Override
    public RubyFixnum hash() {
        return getRuntime().newFixnum(value.hashCode());
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;

        if (other instanceof RubyString) {
            RubyString string = (RubyString) other;

            if (string.value.equal(value)) return true;
        }

        return false;
    }

    /** rb_obj_as_string
     *
     */
    public static RubyString objAsString(ThreadContext context, IRubyObject obj) {
        if (obj instanceof RubyString) return (RubyString) obj;

        IRubyObject str = obj.callMethod(context, "to_s");

        if (!(str instanceof RubyString)) return (RubyString) obj.anyToString();

        if (obj.isTaint()) str.setTaint(true);

        return (RubyString) str;
    }

    /** rb_str_cmp
     *
     */
    public final int op_cmp(RubyString other) {
        return value.cmp(other.value);
    }

    public final int op_cmp19(RubyString other) {
        int ret = value.cmp(other.value);
        if (ret == 0 && !isComparableWith(other)) {
            return value.encoding.getIndex() > other.value.encoding.getIndex() ? 1 : -1;
        }
        return ret;
    }

    /** rb_to_id
     *
     */
    @Override
    public String asJavaString() {
        // TODO: This used to intern; but it didn't appear to change anything
        // turning that off, and it's unclear if it was needed. Plus, we intern
        // 
        return toString();
    }

    public IRubyObject doClone(){
        return newString(getRuntime(), value.dup());
    }

    public RubyString cat(byte[] str) {
        modify(value.realSize + str.length);
        System.arraycopy(str, 0, value.bytes, value.begin + value.realSize, str.length);
        value.realSize += str.length;
        return this;
    }

    public RubyString cat(byte[] str, int beg, int len) {
        modify(value.realSize + len);        
        System.arraycopy(str, beg, value.bytes, value.begin + value.realSize, len);
        value.realSize += len;
        return this;
    }

    public RubyString cat(ByteList str) {
        modify(value.realSize + str.realSize);
        System.arraycopy(str.bytes, str.begin, value.bytes, value.begin + value.realSize, str.realSize);
        value.realSize += str.realSize;
        return this;
    }

    public RubyString cat(byte ch) {
        modify(value.realSize + 1);        
        value.bytes[value.begin + value.realSize] = ch;
        value.realSize++;
        return this;
    }
    
    /** rb_str_replace_m
     *
     */
    @JRubyMethod(name = {"replace", "initialize_copy"}, required = 1)
    public RubyString replace(IRubyObject other) {
        if (this == other) return this;

        modifyCheck();

        RubyString otherStr =  stringValue(other);

        otherStr.shareLevel = shareLevel = SHARE_LEVEL_BYTELIST;

        value = otherStr.value;

        infectBy(other);
        return this;
    }

    @JRubyMethod(name = "reverse")
    public RubyString reverse(ThreadContext context) {
        if (value.length() <= 1) return strDup(context.getRuntime());

        ByteList buf = new ByteList(value.length()+2);
        buf.realSize = value.length();
        int src = value.length() - 1;
        int dst = 0;
        
        while (src >= 0) buf.set(dst++, value.get(src--));

        RubyString rev = new RubyString(context.getRuntime(), getMetaClass(), buf);
        rev.infectBy(this);
        return rev;
    }

    @JRubyMethod(name = "reverse!")
    public RubyString reverse_bang() {
        if (value.length() > 1) {
            modify();
            for (int i = 0; i < (value.length() / 2); i++) {
                byte b = (byte) value.get(i);
                
                value.set(i, value.get(value.length() - i - 1));
                value.set(value.length() - i - 1, b);
            }
        }
        
        return this;
    }

    /** rb_str_s_new
     *
     */
    public static RubyString newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
        RubyString newString = newStringShared(recv.getRuntime(), ByteList.EMPTY_BYTELIST);
        newString.setMetaClass((RubyClass) recv);
        newString.callInit(args, block);
        return newString;
    }
    
    /**
     * Variable-arity version for compatibility. Not bound to Ruby.
     * @deprecated Use the versions with zero or one arguments
     */
    public IRubyObject initialize(IRubyObject[] args, Block unusedBlock) {
        switch (args.length) {
        case 0:
            return this;
        case 1:
            return initialize(args[0]);
        default:
            Arity.raiseArgumentError(getRuntime(), args.length, 0, 1);
            return null; // not reached
        }
    }

    @JRubyMethod(frame = true, visibility = Visibility.PRIVATE)
    @Override
    public IRubyObject initialize() {
        return this;
    }

    @JRubyMethod(frame = true, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(IRubyObject arg0) {
        replace(arg0);
        return this;
    }

    @JRubyMethod(name = "casecmp", compat = CompatVersion.RUBY1_8)
    public IRubyObject casecmp(ThreadContext context, IRubyObject other) {
        return RubyFixnum.newFixnum(context.getRuntime(), value.caseInsensitiveCmp(other.convertToString().value));
    }

    @JRubyMethod(name = "casecmp", compat = CompatVersion.RUBY1_9)
    public IRubyObject casecmp19(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.getRuntime();
        RubyString otherStr = other.convertToString();
        Encoding enc = checkEncoding(otherStr);
        if (enc == null) return runtime.getNil();
        
        if (singleByteOptimizable() && otherStr.singleByteOptimizable()) {
            return RubyFixnum.newFixnum(runtime, value.caseInsensitiveCmp(otherStr.value));
        } else {
            return multiByteCasecmp(runtime, enc, value, otherStr.value);
        }
    }

    private IRubyObject multiByteCasecmp(Ruby runtime, Encoding enc, ByteList value, ByteList otherValue) {
        byte[]bytes = value.bytes;
        int p = value.begin;
        int end = p + value.realSize;

        byte[]obytes = otherValue.bytes;
        int op = otherValue.begin;
        int oend = op + otherValue.realSize;

        while (p < end && op < oend) {
            final int c, oc;
            if (enc.isAsciiCompatible()) {
                c = bytes[p] & 0xff;
                oc = obytes[op] & 0xff;
            } else {
                c = StringSupport.preciseCodePoint(enc, bytes, p, end);
                oc = StringSupport.preciseCodePoint(enc, obytes, op, oend);                
            }

            int cl, ocl;
            if (Encoding.isAscii(c) && Encoding.isAscii(oc)) {
                if (AsciiTables.ToUpperCaseTable[c] != AsciiTables.ToUpperCaseTable[oc]) {
                    return c < oc ? RubyFixnum.minus_one(runtime) : RubyFixnum.one(runtime); 
                }
                cl = ocl = 1;
            } else {
                cl = StringSupport.length(enc, bytes, p, end);
                ocl = StringSupport.length(enc, obytes, op, oend);
                // TODO: opt for 2 and 3 ?
                int ret = StringSupport.caseCmp(bytes, p, obytes, op, cl < ocl ? cl : ocl);
                if (ret != 0) return ret < 0 ? RubyFixnum.minus_one(runtime) : RubyFixnum.one(runtime);
                if (cl != ocl) return cl < ocl ? RubyFixnum.minus_one(runtime) : RubyFixnum.one(runtime);
            }

            p += cl;
            op += ocl;
        }
        if (end - p == oend - op) return RubyFixnum.zero(runtime);
        return end - p > oend - op ? RubyFixnum.one(runtime) : RubyFixnum.minus_one(runtime);
    }

    /** rb_str_match
     *
     */
    @JRubyMethod(name = "=~")
    @Override
    public IRubyObject op_match(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyRegexp) return ((RubyRegexp) other).op_match(context, this);
        if (other instanceof RubyString) {
            throw context.getRuntime().newTypeError("type mismatch: String given");
        }
        return other.callMethod(context, "=~", this);
    }

    /**
     * String#match(pattern)
     *
     * rb_str_match_m
     *
     * @param pattern Regexp or String
     */
    @JRubyMethod
    public IRubyObject match(ThreadContext context, IRubyObject pattern) {
        return getPattern(pattern, false).callMethod(context, "match", this);
    }

    /** rb_str_capitalize / rb_str_capitalize_bang
     *
     */
    @JRubyMethod(name = "capitalize", compat = CompatVersion.RUBY1_8)
    public IRubyObject capitalize(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.capitalize_bang(context);
        return str;
    }

    @JRubyMethod(name = "capitalize!", compat = CompatVersion.RUBY1_8)
    public IRubyObject capitalize_bang(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.realSize == 0) {
            modifyCheck();
            return runtime.getNil();
        }

        modify();

        int s = value.begin;
        int end = s + value.realSize;
        byte[]bytes = value.bytes;
        boolean modify = false;
        
        int c = bytes[s] & 0xff;
        if (ASCII.isLower(c)) {
            bytes[s] = AsciiTables.ToUpperCaseTable[c];
            modify = true;
        }

        while (++s < end) {
            c = bytes[s] & 0xff;
            if (ASCII.isUpper(c)) {
                bytes[s] = AsciiTables.ToLowerCaseTable[c];
                modify = true;
            }
        }

        return modify ? this : runtime.getNil();
    }

    @JRubyMethod(name = "capitalize", compat = CompatVersion.RUBY1_9)
    public IRubyObject capitalize19(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.capitalize_bang19(context);
        return str;
    }

    @JRubyMethod(name = "capitalize!", compat = CompatVersion.RUBY1_9)
    public IRubyObject capitalize_bang19(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        Encoding enc = checkDummyEncoding();

        if (value.realSize == 0) {
            modifyCheck();
            return runtime.getNil();
        }

        modifyAndKeepCodeRange();

        int s = value.begin;
        int end = s + value.realSize;
        byte[]bytes = value.bytes;
        boolean modify = false;

        int c = codePoint(runtime, enc, bytes, s, end);
        if (enc.isLower(c)) {
            enc.codeToMbc(toUpper(enc, c), bytes, s);
            modify = true;
        }

        s += codeLength(runtime, enc, c);
        while (s < end) {
            c = codePoint(runtime, enc, bytes, s, end);
            if (enc.isUpper(c)) {
                enc.codeToMbc(toLower(enc, c), bytes, s);
                modify = true;
            }
            s += codeLength(runtime, enc, c);
        }

        return modify ? this : runtime.getNil();
    }

    @JRubyMethod(name = ">=", compat = CompatVersion.RUBY1_8)
    public IRubyObject op_ge(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) return context.getRuntime().newBoolean(op_cmp((RubyString) other) >= 0);
        return RubyComparable.op_ge(context, this, other);
    }

    @JRubyMethod(name = ">=", compat = CompatVersion.RUBY1_9)
    public IRubyObject op_ge19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) return context.getRuntime().newBoolean(op_cmp19((RubyString) other) >= 0);
        return RubyComparable.op_ge(context, this, other);
    }

    @JRubyMethod(name = ">", compat = CompatVersion.RUBY1_8)
    public IRubyObject op_gt(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) return context.getRuntime().newBoolean(op_cmp((RubyString) other) > 0);
        return RubyComparable.op_gt(context, this, other);
    }

    @JRubyMethod(name = ">", compat = CompatVersion.RUBY1_9)
    public IRubyObject op_gt19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) return context.getRuntime().newBoolean(op_cmp19((RubyString) other) > 0);
        return RubyComparable.op_gt(context, this, other);
    }

    @JRubyMethod(name = "<=", compat = CompatVersion.RUBY1_8)
    public IRubyObject op_le(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) return context.getRuntime().newBoolean(op_cmp((RubyString) other) <= 0);
        return RubyComparable.op_le(context, this, other);
    }

    @JRubyMethod(name = "<=", compat = CompatVersion.RUBY1_9)
    public IRubyObject op_le19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) return context.getRuntime().newBoolean(op_cmp19((RubyString) other) <= 0);
        return RubyComparable.op_le(context, this, other);
    }

    @JRubyMethod(name = "<", compat = CompatVersion.RUBY1_8)
    public IRubyObject op_lt(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) return context.getRuntime().newBoolean(op_cmp((RubyString) other) < 0);
        return RubyComparable.op_lt(context, this, other);
    }

    @JRubyMethod(name = "<", compat = CompatVersion.RUBY1_9)
    public IRubyObject op_lt19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) return context.getRuntime().newBoolean(op_cmp19((RubyString) other) < 0);
        return RubyComparable.op_lt(context, this, other);
    }

    @JRubyMethod(name = "eql?", compat = CompatVersion.RUBY1_8)
    public IRubyObject str_eql_p(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.getRuntime();
        if (other instanceof RubyString && value.equal(((RubyString)other).value)) return runtime.getTrue();
        return runtime.getFalse();
    }

    @JRubyMethod(name = "eql?", compat = CompatVersion.RUBY1_9)
    public IRubyObject str_eql_p19(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.getRuntime();
        if (other instanceof RubyString) {
            RubyString otherString = (RubyString)other;
            if (isComparableWith(otherString) && value.equal(otherString.value)) return runtime.getTrue();
        }
        return runtime.getFalse();
    }

    /** rb_str_upcase / rb_str_upcase_bang
     *
     */
    @JRubyMethod(name = "upcase", compat = CompatVersion.RUBY1_8)
    public RubyString upcase(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.upcase_bang(context);
        return str;
    }

    @JRubyMethod(name = "upcase!", compat = CompatVersion.RUBY1_8)
    public IRubyObject upcase_bang(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.realSize == 0) {
            modifyCheck();
            return runtime.getNil();
        }
        modify();
        return singleByteUpcase(runtime, value.bytes, value.begin, value.begin + value.realSize); 
    }

    @JRubyMethod(name = "upcase", compat = CompatVersion.RUBY1_9)
    public RubyString upcase19(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.upcase_bang19(context);
        return str;
    }

    @JRubyMethod(name = "upcase!", compat = CompatVersion.RUBY1_9)
    public IRubyObject upcase_bang19(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        Encoding enc = checkDummyEncoding();

        if (value.realSize == 0) {
            modifyCheck();
            return runtime.getNil();
        }

        modifyAndKeepCodeRange();

        int s = value.begin;
        int end = s + value.realSize;
        byte[]bytes = value.bytes;

        if (singleByteOptimizable(enc)) {
            return singleByteUpcase(runtime, bytes, s, end);
        } else {
            return multiByteUpcase(runtime, enc, bytes, s, end);
        }
    }

    private IRubyObject singleByteUpcase(Ruby runtime, byte[]bytes, int s, int end) {
        boolean modify = false;
        while (s < end) {
            int c = bytes[s] & 0xff;
            if (ASCII.isLower(c)) {
                bytes[s] = AsciiTables.ToUpperCaseTable[c];
                modify = true;
            }
            s++;
        }
        return modify ? this : runtime.getNil();
    }

    private IRubyObject multiByteUpcase(Ruby runtime, Encoding enc, byte[]bytes, int s, int end) {
        boolean modify = false;
        int c;
        while (s < end) {
            if (enc.isAsciiCompatible() && Encoding.isAscii(c = bytes[s] & 0xff)) {
                if (ASCII.isLower(c)) {
                    bytes[s] = AsciiTables.ToUpperCaseTable[c];
                    modify = true;
                }
                s++;
            } else {
                c = codePoint(runtime, enc, bytes, s, end);
                if (enc.isLower(c)) {
                    enc.codeToMbc(toUpper(enc, c), bytes, s);
                    modify = true;
                }
                s += codeLength(runtime, enc, c);
            }
        }
        return modify ? this : runtime.getNil();        
    }

    /** rb_str_downcase / rb_str_downcase_bang
     *
     */
    @JRubyMethod(name = "downcase", compat = CompatVersion.RUBY1_8)
    public RubyString downcase(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.downcase_bang(context);
        return str;
    }

    @JRubyMethod(name = "downcase!", compat = CompatVersion.RUBY1_8)
    public IRubyObject downcase_bang(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.realSize == 0) {
            modifyCheck();
            return context.getRuntime().getNil();
        }

        modify();
        return singleByteDowncase(runtime, value.bytes, value.begin, value.begin + value.realSize);
    }

    @JRubyMethod(name = "downcase", compat = CompatVersion.RUBY1_9)
    public RubyString downcase19(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.downcase_bang19(context);
        return str;
    }

    @JRubyMethod(name = "downcase!", compat = CompatVersion.RUBY1_9)
    public IRubyObject downcase_bang19(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        Encoding enc = checkDummyEncoding();

        if (value.realSize == 0) {
            modifyCheck();
            return runtime.getNil();
        }

        modifyAndKeepCodeRange();

        int s = value.begin;
        int end = s + value.realSize;
        byte[]bytes = value.bytes;

        if (singleByteOptimizable(enc)) {
            return singleByteDowncase(runtime, bytes, s, end);
        } else {
            return multiByteDowncase(runtime, enc, bytes, s, end);
        }
    }

    private IRubyObject singleByteDowncase(Ruby runtime, byte[]bytes, int s, int end) {
        boolean modify = false;
        while (s < end) {
            int c = bytes[s] & 0xff;
            if (ASCII.isUpper(c)) {
                bytes[s] = AsciiTables.ToLowerCaseTable[c];
                modify = true;
            }
            s++;
        }
        return modify ? this : runtime.getNil();
    }

    private IRubyObject multiByteDowncase(Ruby runtime, Encoding enc, byte[]bytes, int s, int end) {
        boolean modify = false;
        int c;
        while (s < end) {
            if (enc.isAsciiCompatible() && Encoding.isAscii(c = bytes[s] & 0xff)) {
                if (ASCII.isUpper(c)) {
                    bytes[s] = AsciiTables.ToLowerCaseTable[c];
                    modify = true;
                }
                s++;
            } else {
                c = codePoint(runtime, enc, bytes, s, end);
                if (enc.isUpper(c)) {
                    enc.codeToMbc(toLower(enc, c), bytes, s);
                    modify = true;
                }
                s += codeLength(runtime, enc, c);
            }
        }
        return modify ? this : runtime.getNil();        
    }


    /** rb_str_swapcase / rb_str_swapcase_bang
     *
     */
    @JRubyMethod(name = "swapcase", compat = CompatVersion.RUBY1_8)
    public RubyString swapcase(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.swapcase_bang(context);
        return str;
    }

    @JRubyMethod(name = "swapcase!", compat = CompatVersion.RUBY1_8)
    public IRubyObject swapcase_bang(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.realSize == 0) {
            modifyCheck();
            return runtime.getNil();        
        }
        modify();
        return singleByteSwapcase(runtime, value.bytes, value.begin, value.begin + value.realSize);
    }

    @JRubyMethod(name = "swapcase", compat = CompatVersion.RUBY1_9)
    public RubyString swapcase19(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.swapcase_bang19(context);
        return str;
    }

    @JRubyMethod(name = "swapcase!", compat = CompatVersion.RUBY1_9)
    public IRubyObject swapcase_bang19(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        Encoding enc = checkDummyEncoding();
        if (value.realSize == 0) {
            modifyCheck();
            return runtime.getNil();        
        }
        modifyAndKeepCodeRange();

        int s = value.begin;
        int end = s + value.realSize;
        byte[]bytes = value.bytes;

        if (singleByteOptimizable(enc)) {
            return singleByteSwapcase(runtime, bytes, s, end);
        } else {
            return multiByteSwapcase(runtime, enc, bytes, s, end);
        }
    }

    private IRubyObject singleByteSwapcase(Ruby runtime, byte[]bytes, int s, int end) {
        boolean modify = false;
        while (s < end) {
            int c = bytes[s] & 0xff;
            if (ASCII.isUpper(c)) {
                bytes[s] = AsciiTables.ToLowerCaseTable[c];
                modify = true;
            } else if (ASCII.isLower(c)) {
                bytes[s] = AsciiTables.ToUpperCaseTable[c];
                modify = true;
            }
            s++;
        }

        return modify ? this : runtime.getNil();
    }

    private IRubyObject multiByteSwapcase(Ruby runtime, Encoding enc, byte[]bytes, int s, int end) {
        boolean modify = false;
        while (s < end) {
            int c = codePoint(runtime, enc, bytes, s, end);
            if (enc.isUpper(c)) {
                enc.codeToMbc(toLower(enc, c), bytes, s);
                modify = true;
            } else if (enc.isLower(c)) {
                enc.codeToMbc(toUpper(enc, c), bytes, s);
                modify = true;
            }
            s += codeLength(runtime, enc, c);
        }

        return modify ? this : runtime.getNil();
    }

    /** rb_str_dump
     *
     */
    @JRubyMethod
    public IRubyObject dump() {
        RubyString s = new RubyString(getRuntime(), getMetaClass(), inspectIntoByteList(true));
        s.infectBy(this);
        return s;
    }

    @JRubyMethod
    public IRubyObject insert(ThreadContext context, IRubyObject indexArg, IRubyObject stringArg) {
        // MRI behavior: first check for ability to convert to String...
        RubyString s = (RubyString)stringArg.convertToString();
        ByteList insert = s.value;

        // ... and then the index
        int index = (int) indexArg.convertToInteger().getLongValue();
        if (index < 0) index += value.length() + 1;

        if (index < 0 || index > value.length()) {
            throw context.getRuntime().newIndexError("index " + index + " out of range");
        }

        modify();

        value.unsafeReplace(index, 0, insert);
        this.infectBy(s);
        return this;
    }

    /** rb_str_inspect
     *
     */
    @JRubyMethod
    @Override
    public IRubyObject inspect() {
        RubyString s = getRuntime().newString(inspectIntoByteList(false));
        s.infectBy(this);
        return s;
    }
    
    private ByteList inspectIntoByteList(boolean ignoreKCode) {
        Ruby runtime = getRuntime();
        Encoding enc = runtime.getKCode().getEncoding();
        final int length = value.length();
        ByteList sb = new ByteList(length + 2 + length / 100);

        sb.append('\"');

        for (int i = 0; i < length; i++) {
            int c = value.get(i) & 0xFF;
            
            if (!ignoreKCode) {
                int seqLength = enc.length((byte)c);
                
                if (seqLength > 1 && (i + seqLength -1 < length)) {
                    // don't escape multi-byte characters, leave them as bytes
                    sb.append(value, i, seqLength);
                    i += seqLength - 1;
                    continue;
                }
            } 
            
            if (ASCII.isAlnum(c)) {
                sb.append((char)c);
            } else if (c == '\"' || c == '\\') {
                sb.append('\\').append((char)c);
            } else if (c == '#' && isEVStr(i, length)) {
                sb.append('\\').append((char)c);
            } else if (ASCII.isPrint(c)) {
                sb.append((char)c);
            } else if (c == '\n') {
                sb.append('\\').append('n');
            } else if (c == '\r') {
                sb.append('\\').append('r');
            } else if (c == '\t') {
                sb.append('\\').append('t');
            } else if (c == '\f') {
                sb.append('\\').append('f');
            } else if (c == '\u000B') {
                sb.append('\\').append('v');
            } else if (c == '\u0007') {
                sb.append('\\').append('a');
            } else if (c == '\u0008') {
                sb.append('\\').append('b');
            } else if (c == '\u001B') {
                sb.append('\\').append('e');
            } else {
                sb.append(ByteList.plain(Sprintf.sprintf(runtime,"\\%03o",c)));
            }
        }

        sb.append('\"');
        return sb;
    }
    
    private boolean isEVStr(int i, int length) {
        if (i+1 >= length) return false;
        int c = value.get(i+1) & 0xFF;
        
        return c == '$' || c == '@' || c == '{';
    }

    /** rb_str_length
     *
     */
    @JRubyMethod(name = {"length", "size"}, compat = CompatVersion.RUBY1_8)
    public RubyFixnum length() {
        return getRuntime().newFixnum(value.realSize);
    }

    @JRubyMethod(name = {"length", "size"}, compat = CompatVersion.RUBY1_9)
    public RubyFixnum length19() {
        return getRuntime().newFixnum(strLength());
    }

    @JRubyMethod(name = "bytesize", compat = CompatVersion.RUBY1_9)
    public RubyFixnum bytesize() {
        return length(); // use 1.8 impl
    }
    /** rb_str_empty
     *
     */
    @JRubyMethod(name = "empty?")
    public RubyBoolean empty_p(ThreadContext context) {
        return isEmpty() ? context.getRuntime().getTrue() : context.getRuntime().getFalse();
    }

    public boolean isEmpty() {
        return value.length() == 0;
    }

    /** rb_str_append
     *
     */
    public RubyString append(IRubyObject other) {
        infectBy(other);
        return cat(stringValue(other).value);
    }

    /** rb_str_concat
     *
     */
    @JRubyMethod(name = {"concat", "<<"})
    public RubyString concat(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            long value = ((RubyFixnum) other).getLongValue();
            if (value >= 0 && value < 256) return cat((byte) value);
        }
        return append(other);
    }

    /** rb_str_crypt
     *
     */
    @JRubyMethod(name = "crypt")
    public RubyString crypt(ThreadContext context, IRubyObject other) {
        ByteList salt = stringValue(other).getByteList();
        if (salt.realSize < 2) {
            throw context.getRuntime().newArgumentError("salt too short(need >=2 bytes)");
        }

        salt = salt.makeShared(0, 2);
        RubyString s = RubyString.newStringShared(context.getRuntime(), JavaCrypt.crypt(salt, this.getByteList()));
        s.infectBy(this);
        s.infectBy(other);
        return s;
    }

    /* RubyString aka rb_string_value */
    public static RubyString stringValue(IRubyObject object) {
        return (RubyString) (object instanceof RubyString ? object :
            object.convertToString());
    }

    /**
     * Variable-arity version for compatibility. Not bound to Ruby.
     * @deprecated Use the versions with one or two args.
     */
    public IRubyObject sub(ThreadContext context, IRubyObject[] args, Block block) {
        RubyString str = strDup(context.getRuntime());
        str.sub_bang(context, args, block);
        return str;
    }

    /**
     * Variable-arity version for compatibility. Not bound to Ruby.
     * @deprecated Use the versions with one or two arguments.
     */
    public IRubyObject sub_bang(ThreadContext context, IRubyObject[] args, Block block) {
        switch (args.length) {
        case 1:
            return sub_bang(context, args[0], block);
        case 2:
            return sub_bang(context, args[0], args[1], block);
        default:
            Arity.raiseArgumentError(context.getRuntime(), args.length, 1, 2);
            return null; // not reached
        }
    }

    /** rb_str_sub
     *
     */
    @JRubyMethod(name = "sub", frame = true, compat = CompatVersion.RUBY1_8)
    public IRubyObject sub(ThreadContext context, IRubyObject arg0, Block block) {
        RubyString str = strDup(context.getRuntime());
        str.sub_bang(context, arg0, block);
        return str;
    }

    @JRubyMethod(name = "sub", frame = true, compat = CompatVersion.RUBY1_8)
    public IRubyObject sub(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        RubyString str = strDup(context.getRuntime());
        str.sub_bang(context, arg0, arg1, block);
        return str;
    }

    /** rb_str_sub_bang
     *
     */
    @JRubyMethod(name = "sub!", frame = true, reads = BACKREF, writes = BACKREF, compat = CompatVersion.RUBY1_8)
    public IRubyObject sub_bang(ThreadContext context, IRubyObject arg0, Block block) {
        if (block.isGiven()) return subBangIter(context, getPattern(arg0, true), block);
        throw context.getRuntime().newArgumentError(1, 2);
    }

    /** rb_str_sub_bang
     *
     */
    @JRubyMethod(name = "sub!", frame = true, reads = BACKREF, writes = BACKREF, compat = CompatVersion.RUBY1_8)
    public IRubyObject sub_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return subBangNoIter(context, getPattern(arg0, true), arg1.convertToString());
    }

    private IRubyObject subBangIter(ThreadContext context, RubyRegexp rubyRegex, Block block) {
        Regex regex = rubyRegex.getPattern();
        int range = value.begin + value.realSize;
        Matcher matcher = regex.matcher(value.bytes, value.begin, range);

        Frame frame = context.getPreviousFrame();
        if (matcher.search(value.begin, range, Option.NONE) >= 0) {
            byte[] bytes = value.bytes;
            int size = value.realSize;
            RubyMatchData match = rubyRegex.updateBackRef(context, this, frame, matcher);
            match.use();
            RubyString repl = objAsString(context, block.yield(context, 
                    substr(context.getRuntime(), matcher.getBegin(), matcher.getEnd() - matcher.getBegin())));
            modifyCheck(bytes, size);
            frozenCheck();
            frame.setBackRef(match);            
            return subBangCommon(context, regex, matcher, repl, false);
        } else {
            return frame.setBackRef(context.getRuntime().getNil());
        }
    }

    private IRubyObject subBangNoIter(ThreadContext context, RubyRegexp rubyRegex, RubyString repl) {
        boolean tained = repl.isTaint();
        Regex regex = rubyRegex.getPattern();
        int range = value.begin + value.realSize;
        Matcher matcher = regex.matcher(value.bytes, value.begin, range);

        Frame frame = context.getPreviousFrame();
        if (matcher.search(value.begin, range, Option.NONE) >= 0) {
            repl = rubyRegex.regsub(repl, this, matcher);
            rubyRegex.updateBackRef(context, this, frame, matcher);
            return subBangCommon(context, regex, matcher, repl, tained);
        } else {
            return frame.setBackRef(context.getRuntime().getNil());
        }
    }

    private IRubyObject subBangCommon(ThreadContext context, Regex regex, Matcher matcher, RubyString repl, boolean tainted) {
        final int beg = matcher.getBegin();
        final int plen = matcher.getEnd() - beg;

        ByteList replValue = repl.value;
        if (replValue.realSize > plen) {
            modify(value.realSize + replValue.realSize - plen);
        } else {
            modify();
        }
        if (repl.isTaint()) tainted = true;

        if (replValue.realSize != plen) {
            int src = value.begin + beg + plen;
            int dst = value.begin + beg + replValue.realSize;
            int length = value.realSize - beg - plen;
            System.arraycopy(value.bytes, src, value.bytes, dst, length);
        }
        System.arraycopy(replValue.bytes, replValue.begin, value.bytes, value.begin + beg, replValue.realSize);
        value.realSize += replValue.realSize - plen;
        if (tainted) setTaint(true);
        return this;
    }

    /** rb_str_sub
    *
    */
    @JRubyMethod(name = "sub", frame = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject sub19(ThreadContext context, IRubyObject arg0, Block block) {
        RubyString str = strDup(context.getRuntime());
        str.sub_bang19(context, arg0, block);
        return str;
    }

    @JRubyMethod(name = "sub", frame = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject sub19(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        RubyString str = strDup(context.getRuntime());
        str.sub_bang19(context, arg0, arg1, block);
        return str;
    }

    @JRubyMethod(name = "sub!", frame = true, reads = BACKREF, writes = BACKREF, compat = CompatVersion.RUBY1_9)
    public IRubyObject sub_bang19(ThreadContext context, IRubyObject arg0, Block block) {
        if (block.isGiven()) return subBangIter19(context, getPattern(arg0, true), null, block);
        throw context.getRuntime().newArgumentError(1, 2);
    }

    @JRubyMethod(name = "sub!", frame = true, reads = BACKREF, writes = BACKREF, compat = CompatVersion.RUBY1_9)
    public IRubyObject sub_bang19(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        IRubyObject hash = TypeConverter.convertToTypeWithCheck(arg1, context.getRuntime().getHash(), "to_hash");
        RubyRegexp regexp = getPattern(arg0, true);
        if (hash.isNil()) {
            return subBangNoIter19(context, regexp, arg1.convertToString());
        } else {
            return subBangIter19(context, regexp, (RubyHash)hash, block);
        }
    }

    private IRubyObject subBangIter19(ThreadContext context, RubyRegexp rubyRegex, IRubyObject hash, Block block) {
        Regex regex = rubyRegex.getPattern();
        int range = value.begin + value.realSize;
        Matcher matcher = regex.matcher(value.bytes, value.begin, range);

        Frame frame = context.getPreviousFrame();
        if (matcher.search(value.begin, range, Option.NONE) >= 0) {
            byte[] bytes = value.bytes;
            int size = value.realSize;
            RubyMatchData match = rubyRegex.updateBackRef(context, this, frame, matcher);
            match.use();

            final RubyString repl;
            final boolean tainted;
            IRubyObject subStr = substr(context.getRuntime(), matcher.getBegin(), matcher.getEnd() - matcher.getBegin()); // TODO: 1.9 substr
            if (hash == null) {
                tainted = false;
                repl = objAsString(context, block.yield(context, subStr));
            } else {
                tainted = hash.isTaint();
                repl = objAsString(context, ((RubyHash)hash).fastARef(subStr)); 
            }

            modifyCheck(bytes, size);
            frozenCheck();
            frame.setBackRef(match);            
            return subBangCommon19(context, regex, matcher, repl, tainted);
        } else {
            return frame.setBackRef(context.getRuntime().getNil());
        }
    }

    private IRubyObject subBangNoIter19(ThreadContext context, RubyRegexp rubyRegex, RubyString repl) {
        boolean tained = repl.isTaint();
        Regex regex = rubyRegex.getPattern();
        int range = value.begin + value.realSize;
        Matcher matcher = regex.matcher(value.bytes, value.begin, range);

        Frame frame = context.getPreviousFrame();
        if (matcher.search(value.begin, range, Option.NONE) >= 0) {
            repl = rubyRegex.regsub(repl, this, matcher);
            rubyRegex.updateBackRef(context, this, frame, matcher);
            return subBangCommon19(context, regex, matcher, repl, tained);
        } else {
            return frame.setBackRef(context.getRuntime().getNil());
        }
    }

    private IRubyObject subBangCommon19(ThreadContext context, Regex regex, Matcher matcher, RubyString repl, boolean tainted) {
        final int beg = matcher.getBegin();       
        final int end = matcher.getEnd();

        Encoding enc = RubyEncoding.areCompatible(this, repl);
        if (enc == null) enc = subBangVerifyEncoding(context, repl, beg, end);

        final int plen = end - beg;
        ByteList replValue = repl.value;
        if (replValue.realSize > plen) {
            modify19(value.realSize + replValue.realSize - plen);
        } else {
            modify19();
        }

        associateEncoding(enc);
        if (repl.isTaint()) tainted = true;

        int cr = getCodeRange();
        if (cr > CR_UNKNOWN && cr < CR_BROKEN) {
            int cr2 = repl.getCodeRange();
            if (cr2 == CR_BROKEN || (cr == CR_VALID && cr2 == CR_7BIT)) {
                cr = CR_UNKNOWN;
            } else {
                cr = cr2;
            }
        }

        if (replValue.realSize != plen) {
            int src = value.begin + beg + plen;
            int dst = value.begin + beg + replValue.realSize;
            int length = value.realSize - beg - plen;
            System.arraycopy(value.bytes, src, value.bytes, dst, length);
        }
        System.arraycopy(replValue.bytes, replValue.begin, value.bytes, value.begin + beg, replValue.realSize);
        value.realSize += replValue.realSize - plen;
        setCodeRange(cr);
        if (tainted) setTaint(true);
        return this;
    }

    private Encoding subBangVerifyEncoding(ThreadContext context, RubyString repl, int beg, int end) {
        byte[]bytes = value.bytes;
        int p = value.begin;
        int len = value.realSize;
        Encoding strEnc = value.encoding;
        if (codeRangeScan(strEnc, bytes, p, beg) != CR_7BIT ||
            codeRangeScan(strEnc, bytes, p + end, len - end) != CR_7BIT) {
            throw context.getRuntime().newArgumentError(
                    "incompatible character encodings " + strEnc + " and " + repl.value.encoding);
        }
        return repl.value.encoding;        
    }

    /**
     * Variable-arity version for compatibility. Not bound to Ruby.
     * @deprecated Use the versions with one or two arguments.
     */
    public IRubyObject gsub(ThreadContext context, IRubyObject[] args, Block block) {
        switch (args.length) {
        case 1:
            return gsub(context, args[0], block);
        case 2:
            return gsub(context, args[0], args[1], block);
        default:
            Arity.raiseArgumentError(context.getRuntime(), args.length, 1, 2);
            return null; // not reached
        }
    }
    
    /** rb_str_gsub
     *
     */
    @JRubyMethod(name = "gsub", frame = true, reads = BACKREF, writes = BACKREF)
    public IRubyObject gsub(ThreadContext context, IRubyObject arg0, Block block) {
        return gsub(context, arg0, block, false);
    }

    @JRubyMethod(name = "gsub", frame = true, reads = BACKREF, writes = BACKREF)
    public IRubyObject gsub(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return gsub(context, arg0, arg1, block, false);
    }

    /**
     * Variable-arity version for compatibility. Not bound to Ruby.
     * @deprecated Use the versions with one or two arguments.
     */
    public IRubyObject gsub_bang(ThreadContext context, IRubyObject[] args, Block block) {
        switch (args.length) {
        case 1:
            return gsub_bang(context, args[0], block);
        case 2:
            return gsub_bang(context, args[0], args[1], block);
        default:
            Arity.raiseArgumentError(context.getRuntime(), args.length, 1, 2);
            return null; // not reached
        }
    }

    /** rb_str_gsub_bang
     *
     */
    @JRubyMethod(name = "gsub!", frame = true, reads = BACKREF, writes = BACKREF)
    public IRubyObject gsub_bang(ThreadContext context, IRubyObject arg0, Block block) {
        return gsub(context, arg0, block, true);
    }

    /** rb_str_gsub_bang
     *
     */
    @JRubyMethod(name = "gsub!", frame = true, reads = BACKREF, writes = BACKREF)
    public IRubyObject gsub_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return gsub(context, arg0, arg1, block, true);
    }

    private final IRubyObject gsub(ThreadContext context, IRubyObject arg0, Block block, final boolean bang) {
        if (block.isGiven()) {
            return gsubCommon(context, bang, getPattern(arg0, true), block, null, false);
        } else {
            throw context.getRuntime().newArgumentError("wrong number of arguments (1 for 2)");
        }
    }

    private final IRubyObject gsub(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block, final boolean bang) {
        RubyString repl = arg1.convertToString();
        return gsubCommon(context, bang, getPattern(arg0, true), block, repl, repl.isTaint());
    }

    private IRubyObject gsubCommon(ThreadContext context, final boolean bang, RubyRegexp rubyRegex, Block block, RubyString repl, boolean tainted) {
        Ruby runtime = context.getRuntime();
        Regex regex = rubyRegex.getPattern();
        Frame frame = context.getPreviousFrame();

        int begin = value.begin;
        int range = begin + value.realSize;
        Matcher matcher = regex.matcher(value.bytes, begin, range);

        int beg = matcher.search(begin, range, Option.NONE);
        if (beg < 0) {
            frame.setBackRef(runtime.getNil());
            return bang ? runtime.getNil() : strDup(runtime); /* bang: true, no match, no substitution */
        }

        int blen = value.realSize + 30; /* len + margin */
        ByteList dest = new ByteList(blen);
        dest.realSize = blen;
        int offset = 0, buf = 0, bp = 0, cp = begin;

        RubyMatchData match = null;
        while (beg >= 0) {
            final RubyString val;
            int begz = matcher.getBegin();
            int endz = matcher.getEnd();

            if (repl == null) { // block given
                byte[] bytes = value.bytes;
                int size = value.realSize;
                match = rubyRegex.updateBackRef(context, this, frame, matcher);
                match.use();
                val = objAsString(context, block.yield(context, substr(runtime, begz, endz - begz)));
                modifyCheck(bytes, size);
                if (bang) frozenCheck();
            } else {
                val = rubyRegex.regsub(repl, this, matcher);
            }

            if (val.isTaint()) tainted = true;

            ByteList vbuf = val.value;
            int len = (bp - buf) + (beg - offset) + vbuf.realSize + 3;
            if (blen < len) {
                while (blen < len) {
                    blen <<= 1;
                }
                len = bp - buf;
                dest.realloc(blen);
                dest.realSize = blen;
                bp = buf + len;
            }
            len = beg - offset; /* copy pre-match substr */
            System.arraycopy(value.bytes, cp, dest.bytes, bp, len);
            bp += len;
            System.arraycopy(vbuf.bytes, vbuf.begin, dest.bytes, bp, vbuf.realSize);
            bp += vbuf.realSize;
            offset = endz;

            if (begz == endz) {
                if (value.realSize <= endz) {
                    break;
                }
                len = regex.getEncoding().length(value.bytes, begin + endz, range);
                System.arraycopy(value.bytes, begin + endz, dest.bytes, bp, len);
                bp += len;
                offset = endz + len;
            }
            cp = begin + offset;
            if (offset > value.realSize) {
                break;
            }
            beg = matcher.search(cp, range, Option.NONE);
        }

        if (value.realSize > offset) {
            int len = bp - buf;
            if (blen - len < value.realSize - offset) {
                blen = len + value.realSize - offset;
                dest.realloc(blen);
                bp = buf + len;
            }
            System.arraycopy(value.bytes, cp, dest.bytes, bp, value.realSize - offset);
            bp += value.realSize - offset;
        }

        if (match != null) {
            frame.setBackRef(match);
        } else {
            rubyRegex.updateBackRef(context, this, frame, matcher);
        }

        dest.realSize = bp - buf;
        if (bang) {
            view(dest);
            if (tainted) setTaint(true);
            return this;
        } else {
            RubyString destStr = new RubyString(runtime, getMetaClass(), dest);
            destStr.infectBy(this);
            if (tainted) destStr.setTaint(true);
            return destStr;
        }
    }
    

    /**
     * Variable-arity version for compatibility. Not bound to Ruby.
     * @deprecated Use the versions with one or two args.
     */
    public IRubyObject index(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
        case 1:
            return index(context, args[0]);
        case 2:
            return index(context, args[0], args[1]);
        default:
            Arity.raiseArgumentError(context.getRuntime(), args.length, 1, 2);
            return null; // not reached
        }
    }

    /** rb_str_index_m
     *
     */
    @JRubyMethod(reads = BACKREF, writes = BACKREF)
    public IRubyObject index(ThreadContext context, IRubyObject arg0) {
        return indexCommon(0, arg0, context);
    }

    /** rb_str_index_m
     *
     */
    @JRubyMethod(reads = BACKREF, writes = BACKREF)
    public IRubyObject index(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        int pos = RubyNumeric.num2int(arg1);

        if (pos < 0) {
            pos += value.realSize;
            if (pos < 0) {
                if (arg0 instanceof RubyRegexp) {
                    context.getPreviousFrame().setBackRef(context.getRuntime().getNil());
                }
                return context.getRuntime().getNil();
            }
        }

        return indexCommon(pos, arg0, context);
    }

    private IRubyObject indexCommon(int pos, IRubyObject sub, ThreadContext context) throws RaiseException {
        if (sub instanceof RubyRegexp) {
            RubyRegexp regSub = (RubyRegexp) sub;

            pos = regSub.adjustStartPos(this, pos, false);
            pos = regSub.search(context, this, pos, false);
        } else if (sub instanceof RubyFixnum) {
            int c_int = RubyNumeric.fix2int(sub);
            if (c_int < 0x00 || c_int > 0xFF) {
                // out of byte range
                // there will be no match for sure
                return context.getRuntime().getNil();
            }
            byte c = (byte) c_int;
            byte[] bytes = value.bytes;
            int end = value.begin + value.realSize;

            pos += value.begin;
            for (; pos < end; pos++) {
                if (bytes[pos] == c) {
                    return RubyFixnum.newFixnum(context.getRuntime(), pos - value.begin);
                }
            }
            return context.getRuntime().getNil();
        } else if (sub instanceof RubyString) {
            pos = strIndex((RubyString) sub, pos);
        } else {
            IRubyObject tmp = sub.checkStringType();

            if (tmp.isNil()) {
                throw context.getRuntime().newTypeError("type mismatch: " + sub.getMetaClass().getName() + " given");
            }
            pos = strIndex((RubyString) tmp, pos);
        }

        return pos == -1 ? context.getRuntime().getNil() : RubyFixnum.newFixnum(context.getRuntime(), pos);
    }
    
    private int strIndex(RubyString sub, int offset) {
        ByteList byteList = value;
        if (offset < 0) {
            offset += byteList.realSize;
            if (offset < 0) return -1;
        }
        
        ByteList other = sub.value;
        if (sizeIsSmaller(byteList, offset, other)) return -1;
        if (other.realSize == 0) return offset;
        return byteList.indexOf(other, offset);
    }
    private static boolean sizeIsSmaller(ByteList byteList, int offset, ByteList other) {
        return byteList.realSize - offset < other.realSize;
    }

    /**
     * Variable-arity version for compatibility. Not bound to Ruby.
     * @deprecated Use the versions with one or two arguments.
     */
    public IRubyObject rindex(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
        case 1:
            return rindex(context, args[0]);
        case 2:
            return rindex(context, args[0], args[1]);
        default:
            Arity.raiseArgumentError(context.getRuntime(), args.length, 1, 2);
            return null; // not reached
        }
    }

    /** rb_str_rindex_m
     *
     */
    @JRubyMethod(reads = BACKREF, writes = BACKREF)
    public IRubyObject rindex(ThreadContext context, IRubyObject arg0) {
        return rindexCommon(arg0, value.realSize, context);
    }

    /** rb_str_rindex_m
     *
     */
    @JRubyMethod(reads = BACKREF, writes = BACKREF)
    public IRubyObject rindex(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        int pos = RubyNumeric.num2int(arg1);

        if (pos < 0) {
            pos += value.realSize;
            if (pos < 0) {
                if (arg0 instanceof RubyRegexp) { 
                    context.getPreviousFrame().setBackRef(context.getRuntime().getNil());
                }
                return context.getRuntime().getNil();
            }
        }            
        if (pos > value.realSize) pos = value.realSize;

        return rindexCommon(arg0, pos, context);
    }

    private IRubyObject rindexCommon(final IRubyObject sub, int pos, ThreadContext context) throws RaiseException {

        if (sub instanceof RubyRegexp) {
            RubyRegexp regSub = (RubyRegexp) sub;
            if (regSub.length() > 0) {
                pos = regSub.adjustStartPos(this, pos, true);
                pos = regSub.search(context, this, pos, true);
            }
            if (pos >= 0) {
                return RubyFixnum.newFixnum(context.getRuntime(), pos);
            }
        } else if (sub instanceof RubyString) {
            pos = strRindex((RubyString) sub, pos);
            if (pos >= 0) return RubyFixnum.newFixnum(context.getRuntime(), pos);
        } else if (sub instanceof RubyFixnum) {
            int c_int = RubyNumeric.fix2int(sub);
            if (c_int < 0x00 || c_int > 0xFF) {
                // out of byte range
                // there will be no match for sure
                return context.getRuntime().getNil();
            }
            byte c = (byte) c_int;

            byte[] bytes = value.bytes;
            int pbeg = value.begin;
            int p = pbeg + pos;

            if (pos == value.realSize) {
                if (pos == 0) {
                    return context.getRuntime().getNil();
                }
                --p;
            }
            while (pbeg <= p) {
                if (bytes[p] == c) {
                    return RubyFixnum.newFixnum(context.getRuntime(), p - value.begin);
                }
                p--;
            }
            return context.getRuntime().getNil();
        } else {
            IRubyObject tmp = sub.checkStringType();
            if (tmp.isNil()) throw context.getRuntime().newTypeError("type mismatch: " + sub.getMetaClass().getName() + " given");
            pos = strRindex((RubyString) tmp, pos);
            if (pos >= 0) return RubyFixnum.newFixnum(context.getRuntime(), pos);
        }

        return context.getRuntime().getNil();
    }

    private int strRindex(RubyString sub, int pos) {
        int subLength = sub.value.realSize;
        
        /* substring longer than string */
        if (value.realSize < subLength) return -1;
        if (value.realSize - pos < subLength) pos = value.realSize - subLength;

        return value.lastIndexOf(sub.value, pos);
    }

    @Deprecated
    public IRubyObject substr(int beg, int len) {
        return substr(getRuntime(), beg, len);
    }

    /* rb_str_substr */
    public IRubyObject substr(Ruby runtime, int beg, int len) {    
        int length = value.length();
        if (len < 0 || beg > length) return runtime.getNil();

        if (beg < 0) {
            beg += length;
            if (beg < 0) return runtime.getNil();
        }
        
        int end = Math.min(length, beg + len);
        return makeShared(runtime, beg, end - beg);
    }

    /* rb_str_replace */
    public IRubyObject replace(int beg, int len, RubyString replaceWith) {
        if (beg + len >= value.length()) len = value.length() - beg;

        modify();
        value.unsafeReplace(beg,len,replaceWith.value);

        return infectBy(replaceWith);
    }

    /**
     * Variable-arity version for compatibility. Not bound to Ruby.
     * @deprecated Use the versions with one or two args
     */
    public IRubyObject op_aref(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
        case 1:
            return op_aref(context, args[0]);
        case 2:
            return op_aref(context, args[0], args[1]);
        default:
            Arity.raiseArgumentError(context.getRuntime(), args.length, 1, 2);
            return null; // not reached
        }
    }

    /** rb_str_aref, rb_str_aref_m
     *
     */
    @JRubyMethod(name = {"[]", "slice"}, reads = BACKREF, writes = BACKREF)
    public IRubyObject op_aref(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        if (arg1 instanceof RubyRegexp) {
            if(((RubyRegexp)arg1).search(context, this, 0, false) >= 0) {
                return RubyRegexp.nth_match(RubyNumeric.fix2int(arg2), context.getCurrentFrame().getBackRef());
            }
            return context.getRuntime().getNil();
        }
        return substr(context.getRuntime(), RubyNumeric.fix2int(arg1), RubyNumeric.fix2int(arg2));
    }

    /** rb_str_aref, rb_str_aref_m
     *
     */
    @JRubyMethod(name = {"[]", "slice"}, reads = BACKREF, writes = BACKREF)
    public IRubyObject op_aref(ThreadContext context, IRubyObject arg) {
        if (arg instanceof RubyRegexp) {
            if(((RubyRegexp)arg).search(context, this, 0, false) >= 0) {
                return RubyRegexp.nth_match(0, context.getCurrentFrame().getBackRef());
            }
            return context.getRuntime().getNil();
        } else if (arg instanceof RubyString) {
            return value.indexOf(stringValue(arg).value) != -1 ?
                arg : context.getRuntime().getNil();
        } else if (arg instanceof RubyRange) {
            int[] begLen = ((RubyRange) arg).begLenInt(value.length(), 0);
            return begLen == null ? context.getRuntime().getNil() :
                substr(context.getRuntime(), begLen[0], begLen[1]);
        }
        int idx = (int) arg.convertToInteger().getLongValue();
        
        if (idx < 0) idx += value.length();
        if (idx < 0 || idx >= value.length()) return context.getRuntime().getNil();

        return context.getRuntime().newFixnum(value.get(idx) & 0xFF);
    }

    /**
     * rb_str_subpat_set
     *
     */
    private void subpatSet(ThreadContext context, RubyRegexp regexp, int nth, IRubyObject repl) {
        RubyMatchData match;
        int start, end, len;        
        if (regexp.search(context, this, 0, false) < 0) throw context.getRuntime().newIndexError("regexp not matched");

        match = (RubyMatchData)context.getCurrentFrame().getBackRef();

        if (match.regs == null) {
            if (nth >= 1) throw context.getRuntime().newIndexError("index " + nth + " out of regexp");
            if (nth < 0) {
                if(-nth >= 1) throw context.getRuntime().newIndexError("index " + nth + " out of regexp");
                nth += 1;
            }
            start = match.begin;
            if(start == -1) throw context.getRuntime().newIndexError("regexp group " + nth + " not matched");
            end = match.end;
        } else {
            if(nth >= match.regs.numRegs) throw context.getRuntime().newIndexError("index " + nth + " out of regexp");
            if(nth < 0) {
                if(-nth >= match.regs.numRegs) throw context.getRuntime().newIndexError("index " + nth + " out of regexp");
                nth += match.regs.numRegs;
            }
            start = match.regs.beg[nth];
            if(start == -1) throw context.getRuntime().newIndexError("regexp group " + nth + " not matched");
            end = match.regs.end[nth];
        }
        
        len = end - start;
        replace(start, len, stringValue(repl));
    }

    /**
     * Variable arity version for compatibility. Not bound to a Ruby method.
     * @deprecated Use the versions with two or three args.
     */
    public IRubyObject op_aset(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
        case 2:
            return op_aset(context, args[0], args[1]);
        case 3:
            return op_aset(context, args[0], args[1], args[2]);
        default:
            Arity.raiseArgumentError(context.getRuntime(), args.length, 2, 3);
            return null; // not reached
        }
    }

    /** rb_str_aset, rb_str_aset_m
     *
     */
    @JRubyMethod(name = "[]=", reads = BACKREF)
    public IRubyObject op_aset(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        if (arg0 instanceof RubyFixnum || arg0.respondsTo("to_int")) { // FIXME: RubyNumeric or RubyInteger instead?
            int idx = RubyNumeric.fix2int(arg0);
            
            if (idx < 0) idx += value.length();

            if (idx < 0 || idx >= value.length()) {
                throw context.getRuntime().newIndexError("string index out of bounds");
            }
            if (arg1 instanceof RubyFixnum) {
                modify();
                value.set(idx, (byte) RubyNumeric.fix2int(arg1));
            } else {
                replace(idx, 1, stringValue(arg1));
            }
            return arg1;
        }
        if (arg0 instanceof RubyRegexp) {
            RubyString repl = stringValue(arg1);
            subpatSet(context, (RubyRegexp) arg0, 0, repl);
            return repl;
        }
        if (arg0 instanceof RubyString) {
            RubyString orig = (RubyString)arg0;
            int beg = value.indexOf(orig.value);
            if (beg < 0) throw context.getRuntime().newIndexError("string not matched");
            replace(beg, orig.value.length(), stringValue(arg1));
            return arg1;
        }
        if (arg0 instanceof RubyRange) {
            int[] begLen = ((RubyRange) arg0).begLenInt(value.realSize, 2);
            replace(begLen[0], begLen[1], stringValue(arg1));
            return arg1;
        }
        throw context.getRuntime().newTypeError("wrong argument type");
    }

    /** rb_str_aset, rb_str_aset_m
     *
     */
    @JRubyMethod(name = "[]=", reads = BACKREF)
    public IRubyObject op_aset(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        if (arg0 instanceof RubyRegexp) {
            RubyString repl = stringValue(arg2);
            int nth = RubyNumeric.fix2int(arg1);
            subpatSet(context, (RubyRegexp) arg0, nth, repl);
            return repl;
        }
        RubyString repl = stringValue(arg2);
        int beg = RubyNumeric.fix2int(arg0);
        int len = RubyNumeric.fix2int(arg1);
        if (len < 0) throw context.getRuntime().newIndexError("negative length");
        int strLen = value.length();
        if (beg < 0) beg += strLen;

        if (beg < 0 || (beg > 0 && beg > strLen)) {
            throw context.getRuntime().newIndexError("string index out of bounds");
        }
        if (beg + len > strLen) len = strLen - beg;

        replace(beg, len, repl);
        return repl;
    }

    /**
     * Variable arity version for compatibility. Not bound as a Ruby method.
     * @deprecated Use the versions with one or two args.
     */
    public IRubyObject slice_bang(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
        case 1:
            return slice_bang(context, args[0]);
        case 2:
            return slice_bang(context, args[0], args[1]);
        default:
            Arity.raiseArgumentError(context.getRuntime(), args.length, 1, 2);
            return null; // not reached
        }
    }

    /** rb_str_slice_bang
     *
     */
    @JRubyMethod(name = "slice!", reads = BACKREF, writes = BACKREF)
    public IRubyObject slice_bang(ThreadContext context, IRubyObject arg0) {
        IRubyObject result = op_aref(context, arg0);
        if (result.isNil()) return result;

        op_aset(context, arg0, RubyString.newEmptyString(context.getRuntime()));
        return result;
    }

    /** rb_str_slice_bang
     *
     */
    @JRubyMethod(name = "slice!", reads = BACKREF, writes = BACKREF)
    public IRubyObject slice_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        IRubyObject result = op_aref(context, arg0, arg1);
        if (result.isNil()) return result;

        op_aset(context, arg0, arg1, RubyString.newEmptyString(context.getRuntime()));
        return result;
    }

    @JRubyMethod(name = {"succ", "next"})
    public IRubyObject succ(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.succ_bang();
        return str;
    }

    @JRubyMethod(name = {"succ!", "next!"})
    public IRubyObject succ_bang() {
        if (value.realSize == 0) {
            modifyCheck();
            return this;
        }

        modify();

        boolean alnumSeen = false;
        int pos = -1, n = 0;
        int p = value.begin;
        int end = p + value.realSize;
        byte[]bytes = value.bytes;
        
        for (int i = end - 1; i >= p; i--) {
            int c = bytes[i] & 0xff;
            if (ASCII.isAlnum(c)) {
                alnumSeen = true;
                if ((ASCII.isDigit(c) && c < '9') || (ASCII.isLower(c) && c < 'z') || (ASCII.isUpper(c) && c < 'Z')) {
                    bytes[i] = (byte)(c + 1);
                    pos = -1;
                    break;
                }
                pos = i;
                n = ASCII.isDigit(c) ? '1' : (ASCII.isLower(c) ? 'a' : 'A');
                bytes[i] = ASCII.isDigit(c) ? (byte)'0' : ASCII.isLower(c) ? (byte)'a' : (byte)'A';
            }
        }
        if (!alnumSeen) {
            for (int i = end - 1; i >= p; i--) {
                int c = bytes[i] & 0xff;
                if (c < 0xff) {
                    bytes[i] = (byte)(c + 1);
                    pos = -1;
                    break;
                }
                pos = i;
                n = '\u0001';
                bytes[i] = 0;
            }
        }
        if (pos > -1) {
            // This represents left most digit in a set of incremented
            // values?  Therefore leftmost numeric must be '1' and not '0'
            // 999 -> 1000, not 999 -> 0000.  whereas chars should be
            // zzz -> aaaa and non-alnum byte values should be "\377" -> "\001\000"
            value.insert(pos, (byte) n);
        }
        return this;
    }

    /** rb_str_upto_m
     *
     */
    @JRubyMethod(name = "upto", required = 1, frame = true)
    public IRubyObject upto(ThreadContext context, IRubyObject str, Block block) {
        return upto(context, str, false, block);
    }

    /* rb_str_upto */
    public IRubyObject upto(ThreadContext context, IRubyObject str, boolean excl, Block block) {
        RubyString end = str.convertToString();

        int n = value.cmp(end.value);
        if (n > 0 || (excl && n == 0)) return this;

        IRubyObject afterEnd = end.callMethod(context, "succ");
        RubyString current = this;

        while (!current.op_equal(context, afterEnd).isTrue()) {
            block.yield(context, current);            
            if (!excl && current.op_equal(context, end).isTrue()) break;
            current = current.callMethod(context, "succ").convertToString();
            if (excl && current.op_equal(context, end).isTrue()) break;
            if (current.value.realSize > end.value.realSize || current.value.realSize == 0) break;
        }

        return this;
    }

    /** rb_str_include
     *
     */
    @JRubyMethod(name = "include?", required = 1)
    public RubyBoolean include_p(ThreadContext context, IRubyObject obj) {
        if (obj instanceof RubyFixnum) {
            int c = RubyNumeric.fix2int(obj);
            for (int i = 0; i < value.length(); i++) {
                if (value.get(i) == (byte)c) {
                    return context.getRuntime().getTrue();
                }
            }
            return context.getRuntime().getFalse();
        }
        ByteList str = stringValue(obj).value;
        return context.getRuntime().newBoolean(value.indexOf(str) != -1);
    }

    /**
     * Variable-arity version for compatibility. Not bound as a Ruby method.
     * @deprecated Use the versions with zero or one args.
     */
    public IRubyObject to_i(IRubyObject[] args) {
        switch (args.length) {
        case 0:
            return to_i();
        case 1:
            return to_i(args[0]);
        default:
            Arity.raiseArgumentError(getRuntime(), args.length, 0, 1);
            return null; // not reached
        }
    }

    /** rb_str_to_i
     *
     */
    @JRubyMethod(name = "to_i")
    public IRubyObject to_i() {
        return RubyNumeric.str2inum(getRuntime(), this, 10);
    }

    /** rb_str_to_i
     *
     */
    @JRubyMethod(name = "to_i")
    public IRubyObject to_i(IRubyObject arg0) {
        long base = arg0.convertToInteger().getLongValue();
        return RubyNumeric.str2inum(getRuntime(), this, (int) base);
    }

    /** rb_str_oct
     *
     */
    @JRubyMethod(name = "oct")
    public IRubyObject oct(ThreadContext context) {
        if (isEmpty()) return context.getRuntime().newFixnum(0);

        int base = 8;

        int ix = value.begin;

        while(ix < value.begin+value.realSize && ASCII.isSpace(value.bytes[ix] & 0xff)) {
            ix++;
        }

        int pos = (value.bytes[ix] == '-' || value.bytes[ix] == '+') ? ix+1 : ix;
        if((pos+1) < value.begin+value.realSize && value.bytes[pos] == '0') {
            if(value.bytes[pos+1] == 'x' || value.bytes[pos+1] == 'X') {
                base = 16;
            } else if(value.bytes[pos+1] == 'b' || value.bytes[pos+1] == 'B') {
                base = 2;
            } else if(value.bytes[pos+1] == 'd' || value.bytes[pos+1] == 'D') {
                base = 10;
            }
        }
        return RubyNumeric.str2inum(context.getRuntime(), this, base);
    }

    /** rb_str_hex
     *
     */
    @JRubyMethod(name = "hex")
    public IRubyObject hex(ThreadContext context) {
        return RubyNumeric.str2inum(context.getRuntime(), this, 16);
    }

    /** rb_str_to_f
     *
     */
    @JRubyMethod(name = "to_f")
    public IRubyObject to_f() {
        return RubyNumeric.str2fnum(getRuntime(), this);
    }

    /**
     * Variable arity version for compatibility. Not bound to a Ruby method.
     * @deprecated Use the versions with zero, one, or two args.
     */
    public RubyArray split(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
        case 0:
            return split(context);
        case 1:
            return split(context, args[0]);
        case 2:
            return split(context, args[0], args[1]);
        default:
            Arity.raiseArgumentError(context.getRuntime(), args.length, 0, 2);
            return null; // not reached
        }
    }

    /** rb_str_split_m
     *
     */
    @JRubyMethod(writes = BACKREF)
    public RubyArray split(ThreadContext context) {
        return split(context, context.getRuntime().getNil());
    }

    @JRubyMethod(writes = BACKREF)
    public RubyArray split(ThreadContext context, IRubyObject arg0) {
        return splitCommon(arg0, false, 0, 0, context);
    }

    @JRubyMethod(writes = BACKREF)
    public RubyArray split(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        final int lim = RubyNumeric.fix2int(arg1);
        if (lim <= 0) {
            return splitCommon(arg0, false, lim, 1, context);
        } else {
            if (lim == 1) return value.realSize == 0 ? context.getRuntime().newArray() : context.getRuntime().newArray(this);
            return splitCommon(arg0, true, lim, 1, context);
        }
    }

    private RubyArray splitCommon(IRubyObject spat, final boolean limit, final int lim, final int i, ThreadContext context) {
        final RubyArray result;
        if (spat.isNil() && (spat = context.getRuntime().getGlobalVariables().get("$;")).isNil()) {
            result = awkSplit(limit, lim, i);
        } else {
            if (spat instanceof RubyString && ((RubyString) spat).value.realSize == 1) {
                RubyString strSpat = (RubyString) spat;
                if (strSpat.value.bytes[strSpat.value.begin] == (byte) ' ') {
                    result = awkSplit(limit, lim, i);
                } else {
                    result = regexSplit(context, spat, limit, lim, i);
                }
            } else {
                result = regexSplit(context, spat, limit, lim, i);
            }
        }

        if (!limit && lim == 0) {
            while (result.size() > 0 && ((RubyString) result.eltInternal(result.size() - 1)).value.realSize == 0) {
                result.pop(context);
            }
        }

        return result;
    }

    private RubyArray regexSplit(ThreadContext context, IRubyObject pat, boolean limit, int lim, int i) {
        Ruby runtime = context.getRuntime();

        final Regex regex = getPattern(pat, true).getPattern();

        int begin = value.begin;
        final Matcher matcher = regex.matcher(value.bytes, begin, begin + value.realSize);

        RubyArray result = runtime.newArray();
        final Encoding enc = regex.getEncoding();

        int beg = regexSplit(runtime, result, matcher, enc, limit, lim, i, regex.numberOfCaptures() != 0);

        // only this case affects backrefs 
        context.getCurrentFrame().setBackRef(runtime.getNil());

        if (value.realSize > 0 && (limit || value.realSize > beg || lim < 0)) {
            if (value.realSize == beg) {
                result.append(newEmptyString(runtime, getMetaClass()));
            } else {
                result.append(substr(runtime, beg, value.realSize - beg));
            }
        }
        return result;
    }

    private int regexSplit(Ruby runtime, RubyArray result, Matcher matcher, Encoding enc, boolean limit, int lim, int i, boolean captures) {
        byte[]bytes = value.bytes;
        int begin = value.begin;
        int start = begin;
        int range = begin + value.realSize;
        int end, beg = 0;
        boolean lastNull = false;

        while ((end = matcher.search(start, range, Option.NONE)) >= 0) {
            if (start == end + begin && matcher.getBegin() == matcher.getEnd()) {
                if (value.realSize == 0) {
                    result.append(newEmptyString(runtime, getMetaClass()));
                    break;
                } else if (lastNull) {
                    result.append(substr(runtime, beg, enc.length(bytes, begin + beg, range)));
                    beg = start - begin;
                } else {
                    if (start == range) {
                        start++;
                    } else {
                        start += enc.length(bytes, start, range);
                    }
                    lastNull = true;
                    continue;
                }
            } else {
                result.append(substr(runtime, beg, end - beg));
                beg = matcher.getEnd();
                start = begin + matcher.getEnd();
            }
            lastNull = false;

            if (captures) populateCapturesForSplit(runtime, result, matcher);

            if (limit && lim <= ++i) break;
        }
        return beg;
    }

    private void populateCapturesForSplit(Ruby runtime, RubyArray result, Matcher matcher) {
        Region region = matcher.getRegion();
        for (int i = 1; i < region.numRegs; i++) {
            if (region.beg[i] == -1) continue;
            if (region.beg[i] == region.end[i]) {
                result.append(newEmptyString(runtime, getMetaClass()));
            } else {
                result.append(substr(runtime , region.beg[i], region.end[i] - region.beg[i]));
            }
        }
    }

    private RubyArray awkSplit(boolean limit, int lim, int i) {
        Ruby runtime = getRuntime();
        RubyArray result = runtime.newArray();

        byte[]bytes = value.bytes;
        int p = value.begin; 
        int endp = p + value.realSize;

        boolean skip = true;

        int end, beg = 0;        
        for (end = beg = 0; p < endp; p++) {
            if (skip) {
                if (ASCII.isSpace(bytes[p] & 0xff)) {
                    beg++;
                } else {
                    end = beg + 1;
                    skip = false;
                    if (limit && lim <= i) break;
                }
            } else {
                if (ASCII.isSpace(bytes[p] & 0xff)) {
                    result.append(makeShared(runtime, beg, end - beg));
                    skip = true;
                    beg = end + 1;
                    if (limit) i++;
                } else {
                    end++;
                }
            }
        }

        if (value.realSize > 0 && (limit || value.realSize > beg || lim < 0)) {
            if (value.realSize == beg) {
                result.append(newEmptyString(runtime, getMetaClass()));
            } else {
                result.append(makeShared(runtime, beg, value.realSize - beg));
            }
        }
        return result;
    }

    /** get_pat
     * 
     */
    private final RubyRegexp getPattern(IRubyObject obj, boolean quote) {
        if (obj instanceof RubyRegexp) {
            return (RubyRegexp)obj;
        } else if (!(obj instanceof RubyString)) {
            IRubyObject val = obj.checkStringType();
            if (val.isNil()) throw getRuntime().newTypeError("wrong argument type " + obj.getMetaClass() + " (expected Regexp)");
            obj = val; 
        }

        return RubyRegexp.newRegexp(getRuntime(), ((RubyString)obj).value, 0, quote);
    }

    /** rb_str_scan
     *
     */
    @JRubyMethod(name = "scan", required = 1, frame = true, reads = BACKREF, writes = BACKREF, compat = CompatVersion.RUBY1_8)
    public IRubyObject scan(ThreadContext context, IRubyObject arg, Block block) {
        return scan(context, arg, context.getRuntime().getKCode().getEncoding(), block);
    }
    
    @JRubyMethod(name = "scan", required = 1, frame = true, reads = BACKREF, writes = BACKREF, compat = CompatVersion.RUBY1_9)
    public IRubyObject scan19(ThreadContext context, IRubyObject arg, Block block) {
        return scan(context, arg, value.encoding, block);
    }
    
    private IRubyObject scan(ThreadContext context, IRubyObject arg, Encoding enc, Block block) {
        final RubyRegexp rubyRegex = getPattern(arg, true);
        final Regex regex = rubyRegex.getPattern();

        int begin = value.begin;
        int range = begin + value.realSize;
        final Matcher matcher = regex.matcher(value.bytes, begin, range);

        if (block.isGiven()) {
            return scanIter(context, rubyRegex, matcher, enc, block, begin, range);
        } else {
            return scanNoIter(context, rubyRegex, matcher, enc, begin, range);
        }
    }

    private IRubyObject scanIter(ThreadContext context, RubyRegexp rubyRegex, Matcher matcher, Encoding enc, Block block, int begin, int range) {
        Ruby runtime = context.getRuntime();
        byte[]bytes = value.bytes;
        int size = value.realSize;
        RubyMatchData match = null;
        Frame frame = context.getPreviousFrame();

        int end = 0;
        if (rubyRegex.getPattern().numberOfCaptures() == 0) {
            while (matcher.search(begin + end, range, Option.NONE) >= 0) {
                end = positionEnd(matcher, enc, begin, range);
                match = rubyRegex.updateBackRef(context, this, frame, matcher);
                match.use();
                block.yield(context, substr(runtime, matcher.getBegin(), matcher.getEnd() - matcher.getBegin()).infectBy(rubyRegex));
                modifyCheck(bytes, size);
            }
        } else {
            while (matcher.search(begin + end, range, Option.NONE) >= 0) {
                end = positionEnd(matcher, enc, begin, range);
                match = rubyRegex.updateBackRef(context, this, frame, matcher);
                match.use();
                block.yield(context, populateCapturesForScan(runtime, rubyRegex, matcher, range));
                modifyCheck(bytes, size);
            }
        }
        frame.setBackRef(match == null ? context.getRuntime().getNil() : match);
        return this;
    }

    private IRubyObject scanNoIter(ThreadContext context, RubyRegexp rubyRegex, Matcher matcher, Encoding enc, int begin, int range) {
        Ruby runtime = context.getRuntime();
        RubyArray ary = runtime.newArray();

        int end = 0;
        if (rubyRegex.getPattern().numberOfCaptures() == 0) {
            while (matcher.search(begin + end, range, Option.NONE) >= 0) {
                end = positionEnd(matcher, enc, begin, range);
                ary.append(substr(runtime, matcher.getBegin(), matcher.getEnd() - matcher.getBegin()).infectBy(rubyRegex));
            }
        } else {
            while (matcher.search(begin + end, range, Option.NONE) >= 0) {
                end = positionEnd(matcher, enc, begin, range);
                ary.append(populateCapturesForScan(runtime, rubyRegex, matcher, range));
            }
        }

        Frame frame = context.getPreviousFrame();
        if (ary.size() > 0) {
            rubyRegex.updateBackRef(context, this, frame, matcher);
        } else {
            frame.setBackRef(runtime.getNil());
        }
        return ary;
    }

    private int positionEnd(Matcher matcher, Encoding enc, int begin, int range) {
        int end = matcher.getEnd();
        if (matcher.getBegin() == end) {
            if (value.realSize > end) {
                return end + enc.length(value.bytes, begin + end, range);
            } else {
                return end + 1;
            }
        } else {
            return end;
        }
    }

    private IRubyObject populateCapturesForScan(Ruby runtime, RubyRegexp regex, Matcher matcher, int range) {
        Region region = matcher.getRegion();
        RubyArray result = getRuntime().newArray(region.numRegs);
        for (int i=1; i<region.numRegs; i++) {
            int beg = region.beg[i]; 
            if (beg == -1) {
                result.append(getRuntime().getNil());
            } else {
                result.append(substr(runtime, beg, region.end[i] - beg).infectBy(regex));
            }
        }
        return result;
    }

    private static final ByteList SPACE_BYTELIST = new ByteList(ByteList.plain(" "));
    
    private final IRubyObject justify(IRubyObject arg0, char jflag) {
        Ruby runtime = getRuntime();
        
        int width = RubyFixnum.num2int(arg0);
        
        int f, flen = 0;
        byte[]fbuf;
        
        IRubyObject pad;

        f = SPACE_BYTELIST.begin;
        flen = SPACE_BYTELIST.realSize;
        fbuf = SPACE_BYTELIST.bytes;
        pad = runtime.getNil();
        
        return justifyCommon(width, jflag, flen, fbuf, f, runtime, pad);
    }
    
    private final IRubyObject justify(IRubyObject arg0, IRubyObject arg1, char jflag) {
        Ruby runtime = getRuntime();
        
        int width = RubyFixnum.num2int(arg0);
        
        int f, flen = 0;
        byte[]fbuf;
        
        IRubyObject pad;

        pad = arg1.convertToString();
        ByteList fList = ((RubyString)pad).value;
        f = fList.begin;
        flen = fList.realSize;

        if (flen == 0) throw runtime.newArgumentError("zero width padding");

        fbuf = fList.bytes;
        
        return justifyCommon(width, jflag, flen, fbuf, f, runtime, pad);
    }

    private IRubyObject justifyCommon(int width, char jflag, int flen, byte[] fbuf, int f, Ruby runtime, IRubyObject pad) {
        if (width < 0 || value.realSize >= width) return strDup(runtime);

        ByteList res = new ByteList(width);
        res.realSize = width;

        int p = res.begin;
        int pend;
        byte[] pbuf = res.bytes;

        if (jflag != 'l') {
            int n = width - value.realSize;
            pend = p + ((jflag == 'r') ? n : n / 2);
            if (flen <= 1) {
                while (p < pend) {
                    pbuf[p++] = fbuf[f];
                }
            } else {
                int q = f;
                while (p + flen <= pend) {
                    System.arraycopy(fbuf, f, pbuf, p, flen);
                    p += flen;
                }
                while (p < pend) {
                    pbuf[p++] = fbuf[q++];
                }
            }
        }

        System.arraycopy(value.bytes, value.begin, pbuf, p, value.realSize);

        if (jflag != 'r') {
            p += value.realSize;
            pend = res.begin + width;
            if (flen <= 1) {
                while (p < pend) {
                    pbuf[p++] = fbuf[f];
                }
            } else {
                while (p + flen <= pend) {
                    System.arraycopy(fbuf, f, pbuf, p, flen);
                    p += flen;
                }
                while (p < pend) {
                    pbuf[p++] = fbuf[f++];
                }
            }
        }

        RubyString resStr = new RubyString(runtime, getMetaClass(), res);
        resStr.infectBy(this);
        if (flen > 0) {
            resStr.infectBy(pad);
        }
        return resStr;
    }

    /**
     * Variable-arity version for compatibility. Not bound to Ruby.
     * @deprecated use the one or two argument versions.
     */
    public IRubyObject ljust(IRubyObject [] args) {
        switch (args.length) {
        case 1:
            return ljust(args[0]);
        case 2:
            return ljust(args[0], args[1]);
        default:
            Arity.raiseArgumentError(getRuntime(), args.length, 1, 2);
            return null; // not reached
        }
    }

    /** rb_str_ljust
     *
     */
    @JRubyMethod
    public IRubyObject ljust(IRubyObject arg0) {
        return justify(arg0, 'l');
    }

    /** rb_str_ljust
     *
     */
    @JRubyMethod
    public IRubyObject ljust(IRubyObject arg0, IRubyObject arg1) {
        return justify(arg0, arg1, 'l');
    }

    /**
     * Variable-arity version for compatibility. Not bound to Ruby.
     * @deprecated use the one or two argument versions.
     */
    public IRubyObject rjust(IRubyObject [] args) {
        switch (args.length) {
        case 1:
            return rjust(args[0]);
        case 2:
            return rjust(args[0], args[1]);
        default:
            Arity.raiseArgumentError(getRuntime(), args.length, 1, 2);
            return null; // not reached
        }
    }

    /** rb_str_rjust
     *
     */
    @JRubyMethod
    public IRubyObject rjust(IRubyObject arg0) {
        return justify(arg0, 'r');
    }

    /** rb_str_rjust
     *
     */
    @JRubyMethod
    public IRubyObject rjust(IRubyObject arg0, IRubyObject arg1) {
        return justify(arg0, arg1, 'r');
    }

    /**
     * Variable-arity version for compatibility. Not bound to Ruby.
     * @deprecated use the one or two argument versions.
     */
    public IRubyObject center(IRubyObject [] args) {
        switch (args.length) {
        case 1:
            return center(args[0]);
        case 2:
            return center(args[0], args[1]);
        default:
            Arity.raiseArgumentError(getRuntime(), args.length, 1, 2);
            return null; // not reached
        }
    }

    /** rb_str_center
     *
     */
    @JRubyMethod
    public IRubyObject center(IRubyObject arg0) {
        return justify(arg0, 'c');
    }

    /** rb_str_center
     *
     */
    @JRubyMethod
    public IRubyObject center(IRubyObject arg0, IRubyObject arg1) {
        return justify(arg0, arg1, 'c');
    }

    @JRubyMethod(name = "chop")
    public IRubyObject chop(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.chop_bang();
        return str;
    }

    /** rb_str_chop_bang
     * 
     */
    @JRubyMethod(name = "chop!")
    public IRubyObject chop_bang() {
        int end = value.realSize - 1;
        if (end < 0) return getRuntime().getNil(); 

        if ((value.bytes[value.begin + end]) == '\n') {
            if (end > 0 && (value.bytes[value.begin + end - 1]) == '\r') end--;
        }

        view(0, end);
        return this;
    }

    /**
     * Variable-arity version for compatibility. Not bound to Ruby
     * 
     * @param args
     * @return
     * @deprecated Use the zero or one argument versions.
     */
    public RubyString chomp(IRubyObject[] args) {
        switch (args.length) {
        case 0:
            return chomp(getRuntime().getCurrentContext());
        case 1:
            return chomp(getRuntime().getCurrentContext(), args[0]);
        default:
            Arity.raiseArgumentError(getRuntime(), args.length, 0, 1);
            return null; // not reached
        }
    }

    /** rb_str_chop
     * 
     */
    @JRubyMethod
    public RubyString chomp(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.chomp_bang(context);
        return str;
    }

    /** rb_str_chop
     * 
     */
    @JRubyMethod
    public RubyString chomp(ThreadContext context, IRubyObject arg0) {
        RubyString str = strDup(context.getRuntime());
        str.chomp_bang(context, arg0);
        return str;
    }

    /**
     * Variable-arity version for compatibility. Not bound to Ruby.
     * @deprecated Use the zero or one argument versions.
     */
    public IRubyObject chomp_bang(IRubyObject[] args) {
        switch (args.length) {
        case 0:
            return chomp_bang(getRuntime().getCurrentContext());
        case 1:
            return chomp_bang(getRuntime().getCurrentContext(), args[0]);
        default:
            Arity.raiseArgumentError(getRuntime(), args.length, 0, 1);
            return null; // not reached
        }
    }

    /**
     * rb_str_chomp_bang
     *
     * In the common case, removes CR and LF characters in various ways depending on the value of
     *   the optional args[0].
     * If args.length==0 removes one instance of CR, CRLF or LF from the end of the string.
     * If args.length>0 and args[0] is "\n" then same behaviour as args.length==0 .
     * If args.length>0 and args[0] is "" then removes trailing multiple LF or CRLF (but no CRs at
     *   all(!)).
     * @param args See method description.
     */
    @JRubyMethod(name = "chomp!")
    public IRubyObject chomp_bang(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        int len = value.realSize;
        if (len == 0) return runtime.getNil();
        

        IRubyObject rsObj = getRuntime().getGlobalVariables().get("$/");

        if (rsObj == runtime.getGlobalVariables().getDefaultSeparator()) {
            int len2 = value.realSize;
            int p = value.begin;
            byte[]bytes = value.bytes;
            if (bytes[p + len - 1] == (byte)'\n') {
                len2--;
                if (len2 > 0 && bytes[p + len2 - 1] == (byte)'\r') len2--;
                view(0, len2);
            } else if (bytes[p + len - 1] == (byte)'\r') {
                len2--;
                view(0, len2);
            } else {
                modifyCheck();
                return runtime.getNil();
            }
            return this;                
        }
        
        return chompBangCommon(context, rsObj);
    }

    /**
     * rb_str_chomp_bang
     *
     * In the common case, removes CR and LF characters in various ways depending on the value of
     *   the optional args[0].
     * If args.length==0 removes one instance of CR, CRLF or LF from the end of the string.
     * If args.length>0 and args[0] is "\n" then same behaviour as args.length==0 .
     * If args.length>0 and args[0] is "" then removes trailing multiple LF or CRLF (but no CRs at
     *   all(!)).
     * @param args See method description.
     */
    @JRubyMethod(name = "chomp!")
    public IRubyObject chomp_bang(ThreadContext context, IRubyObject arg0) {
        return chompBangCommon(context, arg0);
    }

    private IRubyObject chompBangCommon(ThreadContext context, IRubyObject rsObj) {
        Ruby runtime = context.getRuntime();
        if (rsObj.isNil() || value.realSize == 0) return runtime.getNil();

        RubyString rs = rsObj.convertToString();

        int len = value.realSize;
        int p = value.begin;
        byte[] bytes = value.bytes;
        int rslen = rs.value.realSize;

        if (rslen == 0) {
            while (len > 0 && bytes[p + len - 1] == (byte)'\n') {
                len--;
                if (len > 0 && bytes[p + len - 1] == (byte)'\r') {
                    len--;
                }
            }
            if (len < value.realSize) {
                view(0, len);
                return this;
            }
            return runtime.getNil();
        }

        if (rslen > len) return runtime.getNil();
        byte newline = rs.value.bytes[rslen - 1];

        if (rslen == 1 && newline == (byte)'\n') {
            int len2 = value.realSize;
            if (bytes[p + len - 1] == (byte)'\n') {
                len2--;
                if (len2 > 0 && bytes[p + len2 - 1] == (byte)'\r') {
                    len2--;
                }
                view(0, len2);
            } else if (bytes[p + len - 1] == (byte) '\r') {
                len2--;
                view(0, len2);
            } else {
                modifyCheck();
                return runtime.getNil();
            }
            return this;
        }

        if (bytes[p + len - 1] == newline && rslen <= 1 || value.endsWith(rs.value)) {
            view(0, value.realSize - rslen);
            return this;
        }
        return runtime.getNil();
    }

    /** rb_str_lstrip / rb_str_lstrip_bang
     * 
     */
    @JRubyMethod(name = "lstrip", compat = CompatVersion.RUBY1_8)
    public IRubyObject lstrip(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.lstrip_bang(context);
        return str;
    }

    @JRubyMethod(name = "lstrip!", compat = CompatVersion.RUBY1_8)
    public IRubyObject lstrip_bang(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.realSize == 0) return runtime.getNil();
        return singleByteLStrip(runtime, ASCII, value.bytes, value.begin, value.begin + value.realSize);
    }

    @JRubyMethod(name = "lstrip", compat = CompatVersion.RUBY1_9)
    public IRubyObject lstrip19(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.lstrip_bang19(context);
        return str;
    }

    @JRubyMethod(name = "lstrip!", compat = CompatVersion.RUBY1_9)
    public IRubyObject lstrip_bang19(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.realSize == 0) {
            modifyCheck();
            return runtime.getNil();
        }

        Encoding enc = value.encoding;
        int s = value.begin;
        int end = s + value.realSize;
        byte[]bytes = value.bytes;

        final IRubyObject result;
        if (singleByteOptimizable(enc)) {
            result = singleByteLStrip(runtime, enc, bytes, s, end);
        } else {
            result = multiByteLStrip(runtime, enc, bytes, s, end);
        }
        keepCodeRange();
        return result;
    }

    private IRubyObject singleByteLStrip(Ruby runtime, Encoding enc, byte[]bytes, int s, int end) {
        int p = s;
        while (p < end && enc.isSpace(bytes[p] & 0xff)) p++;
        if (p > s) {
            view(p - s, end - p);
            return this;
        }
        return runtime.getNil();
    }
    
    private IRubyObject multiByteLStrip(Ruby runtime, Encoding enc, byte[]bytes, int s, int end) {
        int p = s;
        int c;
        while (p < end && enc.isSpace(c = codePoint(runtime, enc, bytes, p, end))) p += codeLength(runtime, enc, c);
        if (p > s) {
            view(p - s, end - p);
            return this;
        }
        return runtime.getNil();
    }

    /** rb_str_rstrip / rb_str_rstrip_bang
     *  
     */
    @JRubyMethod(name = "rstrip", compat = CompatVersion.RUBY1_8)
    public IRubyObject rstrip(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.rstrip_bang(context);
        return str;
    }

    @JRubyMethod(name = "rstrip!", compat = CompatVersion.RUBY1_8)
    public IRubyObject rstrip_bang(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.realSize == 0) return runtime.getNil();
        return singleByteRStrip(runtime, ASCII, value.bytes, value.begin, value.begin + value.realSize);
    }

    @JRubyMethod(name = "rstrip", compat = CompatVersion.RUBY1_9)
    public IRubyObject rstrip19(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.rstrip_bang19(context);
        return str;
    }

    @JRubyMethod(name = "rstrip!", compat = CompatVersion.RUBY1_9)
    public IRubyObject rstrip_bang19(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.realSize == 0) {
            modifyCheck();
            return runtime.getNil();
        }

        Encoding enc = value.encoding;
        int s = value.begin;
        int end = s + value.realSize;
        byte[]bytes = value.bytes;

        final IRubyObject result;
        if (singleByteOptimizable(enc)) {
            result = singleByteRStrip(runtime, enc, bytes, s, end);
        } else {
            result = multiByteRStrip(runtime, enc, bytes, s, end);
        }
        keepCodeRange();
        return result;
    }

    private IRubyObject singleByteRStrip2(Ruby runtime, Encoding enc, byte[]bytes, int s, int end) {
        int endp = end;
        while (endp - 1 >= s && bytes[endp - 1] == 0) endp--;
        while (endp - 1 >= s && enc.isSpace(bytes[endp - 1] & 0xff)) endp--;

        if (endp < end) {
            view(0, endp - s);
            return this;
        }
        return runtime.getNil();
    }

    private IRubyObject singleByteRStrip(Ruby runtime, Encoding enc, byte[]bytes, int s, int end) {
        int endp = end - 1;
        while (endp >= s && bytes[endp] == 0) endp--;
        while (endp >= s && enc.isSpace(bytes[endp] & 0xff)) endp--;

        if (endp < end - 1) {
            view(0, endp - s + 1);
            return this;
        }
        return runtime.getNil();
    }

    private IRubyObject multiByteRStrip(Ruby runtime, Encoding enc, byte[]bytes, int s, int end) {
        int endp = end;
        int prev;
        while ((prev = enc.prevCharHead(bytes, s, endp, end)) != -1) {
            if (!enc.isSpace(codePoint(runtime, enc, bytes, prev, end))) break;
            endp = prev;
        }

        if (prev < end) {
            view(0, prev - s + 1);
            return this;
        }
        return runtime.getNil();
    }

    /** rb_str_strip / rb_str_strip_bang
     *
     */
    @JRubyMethod(name = "strip", compat = CompatVersion.RUBY1_8)
    public IRubyObject strip(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.strip_bang(context);
        return str;
    }

    @JRubyMethod(name = "strip!", compat = CompatVersion.RUBY1_8)
    public IRubyObject strip_bang(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.realSize == 0) return runtime.getNil();
        return singleByteStrip(runtime, ASCII, value.bytes, value.begin, value.begin + value.realSize);
    }

    @JRubyMethod(name = "strip", compat = CompatVersion.RUBY1_9)
    public IRubyObject strip19(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.strip_bang19(context);
        return str;
    }

    @JRubyMethod(name = "strip!", compat = CompatVersion.RUBY1_9)
    public IRubyObject strip_bang19(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.realSize == 0) {
            modifyCheck();
            return runtime.getNil();
        }

        Encoding enc = value.encoding;
        int s = value.begin;
        int end = s + value.realSize;
        byte[]bytes = value.bytes;

        final IRubyObject result;
        if (singleByteOptimizable(enc)) {
            result = singleByteStrip(runtime, enc, bytes, s, end);
        } else {
            result = multiByteStrip(runtime, enc, bytes, s, end);
        }
        keepCodeRange();
        return result;
    }

    private IRubyObject singleByteStrip(Ruby runtime, Encoding enc, byte[]bytes, int s, int end) {
        int p = s;
        while (p < end && enc.isSpace(bytes[p] & 0xff)) p++;
        int endp = end - 1;
        while (endp >= p && bytes[endp] == 0) endp--;
        while (endp >= p && enc.isSpace(bytes[endp] & 0xff)) endp--;

        if (p > s || endp < end - 1) {
            view(p - s, endp - p + 1);
            return this;
        }
        return runtime.getNil();
    }

    private IRubyObject multiByteStrip(Ruby runtime, Encoding enc, byte[]bytes, int s, int end) {
        int p = s;
        int c;
        while (p < end && enc.isSpace(c = codePoint(runtime, enc, bytes, p, end))) p += codeLength(runtime, enc, c);
        
        int endp = end;
        int prev;
        while ((prev = enc.prevCharHead(bytes, s, endp, end)) != -1) {
            if (!enc.isSpace(codePoint(runtime, enc, bytes, prev, end))) break;
            endp = prev;
        }
        if (p > s || prev < end) {
            view(p - s, endp - p);
            return this;
        }
        return runtime.getNil();
    }

    /** rb_str_count
     *
     */
    @JRubyMethod(name = "count", required = 1, rest = true)
    public IRubyObject count(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        if (args.length < 1) throw runtime.newArgumentError("wrong number of arguments");
        if (value.realSize == 0) return RubyFixnum.zero(runtime);

        boolean[]table = new boolean[TRANS_SIZE];
        boolean init = true;
        for (int i=0; i<args.length; i++) {
            RubyString s = args[i].convertToString();
            s.setup_table(table, init);
            init = false;
        }

        int s = value.begin;
        int send = s + value.realSize;
        byte[]bytes = value.bytes;
        int i = 0;

        while (s < send) if (table[bytes[s++] & 0xff]) i++;

        return runtime.newFixnum(i);
    }

    /** rb_str_delete
     *
     */
    @JRubyMethod(name = "delete", required = 1, rest = true)
    public IRubyObject delete(ThreadContext context, IRubyObject[] args) {
        RubyString str = strDup(context.getRuntime());
        str.delete_bang(context, args);
        return str;
    }

    /** rb_str_delete_bang
     *
     */
    @JRubyMethod(name = "delete!", required = 1, rest = true)
    public IRubyObject delete_bang(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        if (args.length < 1) throw runtime.newArgumentError("wrong number of arguments");
        
        boolean[]squeeze = new boolean[TRANS_SIZE];

        boolean init = true;
        for (int i=0; i<args.length; i++) {
            RubyString s = args[i].convertToString();
            s.setup_table(squeeze, init);
            init = false;
        }
        
        modify();
        
        if (value.realSize == 0) return runtime.getNil();
        int s = value.begin;
        int t = s;
        int send = s + value.realSize;
        byte[]bytes = value.bytes;
        boolean modify = false;
        
        while (s < send) {
            if (squeeze[bytes[s] & 0xff]) {
                modify = true;
            } else {
                bytes[t++] = bytes[s];
            }
            s++;
        }
        value.realSize = t - value.begin;

        return modify ? this : runtime.getNil();
    }

    /** rb_str_squeeze
     *
     */
    @JRubyMethod(name = "squeeze", rest = true)
    public IRubyObject squeeze(ThreadContext context, IRubyObject[] args) {
        RubyString str = strDup(context.getRuntime());
        str.squeeze_bang(context, args);
        return str;
    }

    /** rb_str_squeeze_bang
     *
     */
    @JRubyMethod(name = "squeeze!", rest = true)
    public IRubyObject squeeze_bang(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        if (value.realSize == 0) {
            modifyCheck();
            return runtime.getNil();
        }

        final boolean squeeze[] = new boolean[TRANS_SIZE];

        if (args.length == 0) {
            for (int i=0; i<TRANS_SIZE; i++) squeeze[i] = true;
        } else {
            boolean init = true;
            for (int i=0; i<args.length; i++) {
                RubyString s = args[i].convertToString();
                s.setup_table(squeeze, init);
                init = false;
            }
        }

        modify();

        int s = value.begin;
        int t = s;
        int send = s + value.realSize;
        byte[]bytes = value.bytes;
        int save = -1;

        while (s < send) {
            int c = bytes[s++] & 0xff;
            if (c != save || !squeeze[c]) bytes[t++] = (byte)(save = c);
        }

        if (t - value.begin != value.realSize) { // modified
            value.realSize = t - value.begin; 
            return this;
        }

        return runtime.getNil();
    }

    /** rb_str_tr
     *
     */
    @JRubyMethod
    public IRubyObject tr(ThreadContext context, IRubyObject src, IRubyObject repl) {
        RubyString str = strDup(context.getRuntime());
        str.tr_trans(context, src, repl, false);
        return str;
    }

    /** rb_str_tr_bang
    *
    */
    @JRubyMethod(name = "tr!")
    public IRubyObject tr_bang(ThreadContext context, IRubyObject src, IRubyObject repl) {
        return tr_trans(context, src, repl, false);
    }    
    
    private static final class TR {
        int gen, now, max;
        int p, pend;
        byte[]buf;
    }

    private static final int TRANS_SIZE = 256;
    
    /** tr_setup_table
     * 
     */
    private final void setup_table(boolean[]table, boolean init) {
        final boolean[]buf = new boolean[TRANS_SIZE];
        final TR tr = new TR();
        int c;
        
        boolean cflag = false;
        
        tr.p = value.begin;
        tr.pend = value.begin + value.realSize;
        tr.buf = value.bytes;
        tr.gen = tr.now = tr.max = 0;
        
        if (value.realSize > 1 && value.bytes[value.begin] == '^') {
            cflag = true;
            tr.p++;
        }
        
        if (init) for (int i=0; i<TRANS_SIZE; i++) table[i] = true;
        
        for (int i=0; i<TRANS_SIZE; i++) buf[i] = cflag;
        while ((c = trnext(tr)) >= 0) buf[c & 0xff] = !cflag;
        for (int i=0; i<TRANS_SIZE; i++) table[i] = table[i] && buf[i];
    }
    
    /** tr_trans
    *
    */    
    private final IRubyObject tr_trans(ThreadContext context, IRubyObject src, IRubyObject repl, boolean sflag) {
        Ruby runtime = context.getRuntime();
        if (value.realSize == 0) return runtime.getNil();
        
        ByteList replList = repl.convertToString().value;
        
        if (replList.realSize == 0) return delete_bang(context, new IRubyObject[]{src});

        ByteList srcList = src.convertToString().value;
        
        final TR trsrc = new TR();
        final TR trrepl = new TR();
        
        boolean cflag = false;
        boolean modify = false;
        
        trsrc.p = srcList.begin;
        trsrc.pend = srcList.begin + srcList.realSize;
        trsrc.buf = srcList.bytes;
        if (srcList.realSize >= 2 && srcList.bytes[srcList.begin] == '^') {
            cflag = true;
            trsrc.p++;
        }       
        
        trrepl.p = replList.begin;
        trrepl.pend = replList.begin + replList.realSize;
        trrepl.buf = replList.bytes;
        
        trsrc.gen = trrepl.gen = 0;
        trsrc.now = trrepl.now = 0;
        trsrc.max = trrepl.max = 0;
        
        int c;
        final int[]trans = new int[TRANS_SIZE];
        if (cflag) {
            for (int i=0; i<TRANS_SIZE; i++) trans[i] = 1;
            while ((c = trnext(trsrc)) >= 0) trans[c & 0xff] = -1;
            while ((c = trnext(trrepl)) >= 0); 
            for (int i=0; i<TRANS_SIZE; i++) {
                if (trans[i] >= 0) trans[i] = trrepl.now;
            }
        } else {
            for (int i=0; i<TRANS_SIZE; i++) trans[i] = -1;
            while ((c = trnext(trsrc)) >= 0) {
                int r = trnext(trrepl);
                if (r == -1) r = trrepl.now;
                trans[c & 0xff] = r;
            }
        }
        
        modify();
        
        int s = value.begin;
        int send = s + value.realSize;
        byte sbuf[] = value.bytes;
        
        if (sflag) {
            int t = s;
            int c0, last = -1;
            while (s < send) {
                c0 = sbuf[s++];
                if ((c = trans[c0 & 0xff]) >= 0) {
                    if (last == c) continue;
                    last = c;
                    sbuf[t++] = (byte)(c & 0xff);
                    modify = true;
                } else {
                    last = -1;
                    sbuf[t++] = (byte)c0;
                }
            }
            
            if (value.realSize > (t - value.begin)) {
                value.realSize = t - value.begin;
                modify = true;
            }
        } else {
            while (s < send) {
                if ((c = trans[sbuf[s] & 0xff]) >= 0) {
                    sbuf[s] = (byte)(c & 0xff);
                    modify = true;
                }
                s++;
            }
        }

        return modify ? this : runtime.getNil();
    }

    /** trnext
    *
    */    
    private final int trnext(TR t) {
        byte [] buf = t.buf;
        
        for (;;) {
            if (t.gen == 0) {
                if (t.p == t.pend) return -1;
                if (t.p < t.pend -1 && buf[t.p] == '\\') t.p++;
                t.now = buf[t.p++];
                if (t.p < t.pend - 1 && buf[t.p] == '-') {
                    t.p++;
                    if (t.p < t.pend) {
                        if (t.now > ((int)buf[t.p] & 0xFF)) {
                            t.p++;
                            continue;
                        }
                        t.gen = 1;
                        t.max = (int)buf[t.p++] & 0xFF;
                    }
                }
                return t.now & 0xff;
            } else if (++t.now < t.max) {
                return t.now & 0xff;
            } else {
                t.gen = 0;
                return t.max & 0xff;
            }
        }
    }    

    /** rb_str_tr_s
     *
     */
    @JRubyMethod
    public IRubyObject tr_s(ThreadContext context, IRubyObject src, IRubyObject repl) {
        RubyString str = strDup(context.getRuntime());
        str.tr_trans(context, src, repl, true);
        return str;
    }

    /** rb_str_tr_s_bang
     *
     */
    @JRubyMethod(name = "tr_s!")
    public IRubyObject tr_s_bang(ThreadContext context, IRubyObject src, IRubyObject repl) {
        return tr_trans(context, src, repl, true);
    }

    /** rb_str_each_line
     *
     */
    @JRubyMethod(name = {"each_line", "each"}, frame = true)
    public IRubyObject each_line(ThreadContext context, Block block) {
        return each_lineCommon(context, context.getRuntime().getGlobalVariables().get("$/"), block);
    }

    @JRubyMethod(name = {"each_line", "each"}, frame = true)
    public IRubyObject each_line(ThreadContext context, IRubyObject arg, Block block) {
        return each_lineCommon(context, arg, block);
    }

    public IRubyObject each_lineCommon(ThreadContext context, IRubyObject sep, Block block) {        
        Ruby runtime = context.getRuntime();

        if (sep.isNil()) {
            block.yield(context, this);
            return this;
        }

        RubyString rsep = sep.convertToString();
        ByteList rsepValue = rsep.value;
        int rslen = rsepValue.realSize;

        final byte newline;
        if (rslen == 0) {
            newline = '\n';
        } else {
            newline = rsepValue.bytes[rsepValue.begin + rslen - 1];
        }

        int p = value.begin;
        int end = p + value.realSize;
        int ptr = p;
        int len = value.realSize;

        int s = p;
        p += rslen;
        byte[] strBytes = value.bytes;
        for (; p < end; p++) {
            if (rslen == 0 && strBytes[p] == '\n') {
                if (strBytes[++p] != '\n') continue;
                while(p < end && strBytes[p] == '\n') p++;
            }
            if (ptr < p && strBytes[p - 1] == newline &&
               (rslen <= 1 || 
                ByteList.memcmp(rsepValue.bytes, rsepValue.begin, rslen, strBytes, p - rslen, rslen) == 0)) {
                block.yield(context, makeShared(runtime, s - ptr, p - s).infectBy(this));
                modifyCheck(strBytes, len);
                s = p;
            }
        }

        if (s != end) {
            if (p > end) p = end;
            block.yield(context, makeShared(runtime, s - ptr, p - s).infectBy(this));
        }

        return this;
    }

    /**
     * rb_str_each_byte
     */
    @JRubyMethod(name = "each_byte", frame = true, compat = CompatVersion.RUBY1_8)
    public RubyString each_byte(ThreadContext context, Block block) {
        Ruby runtime = getRuntime();
        // Check the length every iteration, since
        // the block can modify this string.
        for (int i = 0; i < value.length(); i++) {
            block.yield(context, runtime.newFixnum(value.get(i) & 0xFF));
        }
        return this;
    }

    @JRubyMethod(name = {"each_byte", "bytes"}, frame = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject each_byte19(ThreadContext context, Block block) {
        return block.isGiven() ? each_byte(context, block) : enumeratorize(context.getRuntime(), this, "each_byte");
    }

    /** rb_str_each_char
     * 
     */
    @JRubyMethod(name = {"each_char", "chars"}, frame = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject each_char(ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorize(context.getRuntime(), this, "each_char");

        byte bytes[] = value.bytes;
        int p = value.begin;
        int end = p + value.realSize;
        Encoding enc = value.encoding;

        while (p < end) {
            int n = StringSupport.length(enc, bytes, p, end);
            block.yield(context, substr(context.getRuntime(), p, n)); // TODO: 1.9 version of substr.
            p += n;
        }
        return this;
    }

    /** rb_str_each_codepoint
     * 
     */
    @JRubyMethod(name = {"each_codepoint", "codepoints"}, frame = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject each_codepoint(ThreadContext context, Block block) {
        if (singleByteOptimizable()) return each_byte19(context, block);
        if (!block.isGiven()) return enumeratorize(context.getRuntime(), this, "each_codepoint");

        Ruby runtime = context.getRuntime();
        byte bytes[] = value.bytes;
        int p = value.begin;
        int end = p + value.realSize;
        Encoding enc = value.encoding;

        while (p < end) {
            int c = codePoint(runtime, enc, bytes, p, end);
            int n = codeLength(runtime, enc, c);
            block.yield(context, runtime.newFixnum(c));
            p += n;
        }
        return this;
    }

    /** rb_str_intern
     *
     */
    public RubySymbol intern() {
        String s = toString();
        if (s.length() == 0) {
            throw getRuntime().newArgumentError("interning empty string");
        }
        if (s.indexOf('\0') >= 0) {
            throw getRuntime().newArgumentError("symbol string may not contain '\\0'");
        }
        return getRuntime().newSymbol(s);
    }

    @JRubyMethod(name = {"to_sym", "intern"})
    public RubySymbol to_sym() {
        return intern();
    }

    @JRubyMethod(name = "sum")
    public IRubyObject sum(ThreadContext context) {
        return sumCommon(context, 16);
    }

    @JRubyMethod(name = "sum")
    public IRubyObject sum(ThreadContext context, IRubyObject arg) {
        return sumCommon(context, RubyNumeric.num2long(arg));
    }

    public IRubyObject sumCommon(ThreadContext context, long bits) {
        Ruby runtime = context.getRuntime();

        byte[]bytes = value.bytes;
        int p = value.begin;
        int len = value.realSize;
        int end = p + len; 

        if (bits >= 8 * 8) { // long size * bits in byte
            IRubyObject one = RubyFixnum.one(runtime);
            IRubyObject sum = RubyFixnum.zero(runtime);
            while (p < end) {
                modifyCheck(bytes, len);
                sum = sum.callMethod(context, "+", RubyFixnum.newFixnum(runtime, bytes[p++] & 0xff));
            }
            if (bits != 0) {
                IRubyObject mod = one.callMethod(context, "<<", RubyFixnum.newFixnum(runtime, bits));
                sum = sum.callMethod(context, "&", mod.callMethod(context, "-", one));
            }
            return sum;
        } else {
            long sum = 0;
            while (p < end) {
                modifyCheck(bytes, len);
                sum += bytes[p++] & 0xff;
            }
            return RubyFixnum.newFixnum(runtime, bits == 0 ? sum : sum & (1L << bits) - 1L);
        }
    }

    /** string_to_c
     * 
     */
    @JRubyMethod(name = "to_c", reads = BACKREF, writes = BACKREF, compat = CompatVersion.RUBY1_9)
    public IRubyObject to_c(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        Frame frame = context.getCurrentFrame();
        IRubyObject backref = frame.getBackRef();
        if (backref != null && backref instanceof RubyMatchData) ((RubyMatchData)backref).use();

        IRubyObject s = RuntimeHelpers.invoke(
                context, this, "gsub",
                RubyRegexp.newRegexp(runtime, Numeric.ComplexPatterns.underscores_pat),
                runtime.newString(new ByteList(new byte[]{'_'})));

        RubyArray a = RubyComplex.str_to_c_internal(context, s);

        frame.setBackRef(backref);

        if (!a.eltInternal(0).isNil()) {
            return a.eltInternal(0);
        } else {
            return RubyComplex.newComplexCanonicalize(context, RubyFixnum.zero(runtime));
        }
    }

    /** string_to_r
     * 
     */
    @JRubyMethod(name = "to_r", reads = BACKREF, writes = BACKREF, compat = CompatVersion.RUBY1_9)
    public IRubyObject to_r(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        Frame frame = context.getCurrentFrame();
        IRubyObject backref = frame.getBackRef();
        if (backref != null && backref instanceof RubyMatchData) ((RubyMatchData)backref).use();

        IRubyObject s = RuntimeHelpers.invoke(
                context, this, "gsub",
                RubyRegexp.newRegexp(runtime, Numeric.ComplexPatterns.underscores_pat),
                runtime.newString(new ByteList(new byte[]{'_'})));

        RubyArray a = RubyRational.str_to_r_internal(context, s);

        frame.setBackRef(backref);

        if (!a.eltInternal(0).isNil()) {
            return a.eltInternal(0);
        } else {
            return RubyRational.newRationalCanonicalize(context, RubyFixnum.zero(runtime));
        }
    }    

    public static RubyString unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        RubyString result = newString(input.getRuntime(), input.unmarshalString());
        input.registerLinkTarget(result);
        return result;
    }

    /**
     * @see org.jruby.util.Pack#unpack
     */
    @JRubyMethod
    public RubyArray unpack(IRubyObject obj) {
        return Pack.unpack(getRuntime(), this.value, stringValue(obj).value);
    }

    public void empty() {
        value = ByteList.EMPTY_BYTELIST;
        shareLevel = SHARE_LEVEL_BYTELIST;
    }

    @JRubyMethod(name = "encoding", compat = CompatVersion.RUBY1_9)
    public IRubyObject encoding(ThreadContext context) {
        return context.getRuntime().getEncodingService().getEncoding(value.encoding);
    }
    
    @JRubyMethod(name = "force_encoding", compat = CompatVersion.RUBY1_9)
    public IRubyObject force_encoding(ThreadContext context, IRubyObject enc) {
        modify();
        associateEncoding(enc.convertToString().toEncoding(context.getRuntime()));
        return this;
    }

    @JRubyMethod(name = "valid_encoding?", compat = CompatVersion.RUBY1_9)
    public IRubyObject valid_encoding_p(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        return scanForCodeRange() == CR_BROKEN ? runtime.getFalse() : runtime.getTrue();
    }

    @JRubyMethod(name = "ascii_only?", compat = CompatVersion.RUBY1_9)
    public IRubyObject ascii_only_p(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        return scanForCodeRange() == CR_7BIT ? runtime.getFalse() : runtime.getTrue();
    }

    /**
     * Mutator for internal string representation.
     *
     * @param value The new java.lang.String this RubyString should encapsulate
     * @deprecated
     */
    public void setValue(CharSequence value) {
        view(ByteList.plain(value));
    }

    public void setValue(ByteList value) {
        view(value);
    }

    public CharSequence getValue() {
        return toString();
    }

    public byte[] getBytes() {
        return value.bytes();
    }

    public ByteList getByteList() {
        return value;
    }

    /** used by ar-jdbc
     * 
     */
    public String getUnicodeValue() {
        try {
            return new String(value.bytes, value.begin, value.realSize, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Something's seriously broken with encodings", e);
        }
    }

    @Override
    public IRubyObject to_java() {
        return MiniJava.javaToRuby(getRuntime(), new String(getBytes()));
    }
}
