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

import static org.jruby.CompatVersion.RUBY1_8;
import static org.jruby.CompatVersion.RUBY1_9;
import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.anno.FrameField.BACKREF;
import static org.jruby.javasupport.util.RuntimeHelpers.invokedynamic;
import static org.jruby.runtime.MethodIndex.OP_CMP;
import static org.jruby.runtime.MethodIndex.OP_EQUAL;
import static org.jruby.runtime.Visibility.PRIVATE;
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
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.ascii.AsciiTables;
import org.jcodings.constants.CharacterType;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.util.CaseInsensitiveBytesHash;
import org.jcodings.util.IntHash;
import org.joni.Matcher;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Region;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.ByteList;
import org.jruby.util.ConvertBytes;
import org.jruby.util.MurmurHash;
import org.jruby.util.Numeric;
import org.jruby.util.Pack;
import org.jruby.util.RegexpOptions;
import org.jruby.util.Sprintf;
import org.jruby.util.StringSupport;
import org.jruby.util.TypeConverter;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
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

    private static final Logger LOG = LoggerFactory.getLogger("RubyString");

    private static final ASCIIEncoding ASCII = ASCIIEncoding.INSTANCE;
    private static final UTF8Encoding UTF8 = UTF8Encoding.INSTANCE;
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    // string doesn't share any resources
    private static final int SHARE_LEVEL_NONE = 0;
    // string has it's own ByteList, but it's pointing to a shared buffer (byte[])
    private static final int SHARE_LEVEL_BUFFER = 1;
    // string doesn't have it's own ByteList (values)
    private static final int SHARE_LEVEL_BYTELIST = 2;

    private volatile int shareLevel = SHARE_LEVEL_NONE;

    private ByteList value;

    public static RubyClass createStringClass(Ruby runtime) {
        RubyClass stringClass = runtime.defineClass("String", runtime.getObject(), STRING_ALLOCATOR);
        runtime.setString(stringClass);
        stringClass.index = ClassIndex.STRING;
        stringClass.setReifiedClass(RubyString.class);
        stringClass.kindOf = new RubyModule.KindOf() {
            @Override
                public boolean isKindOf(IRubyObject obj, RubyModule type) {
                    return obj instanceof RubyString;
                }
            };

        stringClass.includeModule(runtime.getComparable());
        if (!runtime.is1_9()) stringClass.includeModule(runtime.getEnumerable());
        stringClass.defineAnnotatedMethods(RubyString.class);

        return stringClass;
    }

    private static ObjectAllocator STRING_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return RubyString.newEmptyString(runtime, klass);
        }
    };

    public Encoding getEncoding() {
        return value.getEncoding();
    }

    public void setEncoding(Encoding encoding) {
        value.setEncoding(encoding);
    }

    public void associateEncoding(Encoding enc) {
        if (value.getEncoding() != enc) {
            if (!isCodeRangeAsciiOnly() || !enc.isAsciiCompatible()) clearCodeRange();
            value.setEncoding(enc);
        }
    }

    public final void setEncodingAndCodeRange(Encoding enc, int cr) {
        value.setEncoding(enc);
        setCodeRange(cr);
    }

    public final Encoding toEncoding(Ruby runtime) {
        return runtime.getEncodingService().findEncoding(this);
    }

    public final int getCodeRange() {
        return flags & CR_MASK;
    }

    public final void setCodeRange(int codeRange) {
        clearCodeRange();
        flags |= codeRange & CR_MASK;
    }

    public final void clearCodeRange() {
        flags &= ~CR_MASK;
    }

    private void keepCodeRange() {
        if (getCodeRange() == CR_BROKEN) clearCodeRange();
    }

    // ENC_CODERANGE_ASCIIONLY
    public final boolean isCodeRangeAsciiOnly() {
        return getCodeRange() == CR_7BIT;
    }

    // rb_enc_str_asciionly_p
    public final boolean isAsciiOnly() {
        return value.getEncoding().isAsciiCompatible() && scanForCodeRange() == CR_7BIT;
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

    private void copyCodeRangeForSubstr(RubyString from, Encoding enc) {
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
            if (value.getRealSize() == 0) {
                setCodeRange(!enc.isAsciiCompatible() ? CR_VALID : CR_7BIT);
            }
        }
    }

    private void copyCodeRange(RubyString from) {
        value.setEncoding(from.value.getEncoding());
        setCodeRange(from.getCodeRange());
    }

    // rb_enc_str_coderange
    final int scanForCodeRange() {
        int cr = getCodeRange();
        if (cr == CR_UNKNOWN) {
            cr = codeRangeScan(value.getEncoding(), value);
            setCodeRange(cr);
        }
        return cr;
    }

    final boolean singleByteOptimizable() {
        return getCodeRange() == CR_7BIT || value.getEncoding().isSingleByte();
    }

    final boolean singleByteOptimizable(Encoding enc) {
        return getCodeRange() == CR_7BIT || enc.isSingleByte();
    }

    private Encoding isCompatibleWith(RubyString other) { 
        Encoding enc1 = value.getEncoding();
        Encoding enc2 = other.value.getEncoding();

        if (enc1 == enc2) return enc1;

        if (other.value.getRealSize() == 0) return enc1;
        if (value.getRealSize() == 0) return enc2;

        if (!enc1.isAsciiCompatible() || !enc2.isAsciiCompatible()) return null;

        return RubyEncoding.areCompatible(enc1, scanForCodeRange(), enc2, other.scanForCodeRange());
    }

    final Encoding isCompatibleWith(EncodingCapable other) {
        if (other instanceof RubyString) return checkEncoding((RubyString)other);
        Encoding enc1 = value.getEncoding();
        Encoding enc2 = other.getEncoding();

        if (enc1 == enc2) return enc1;
        if (value.getRealSize() == 0) return enc2;
        if (!enc1.isAsciiCompatible() || !enc2.isAsciiCompatible()) return null;
        if (enc2 instanceof USASCIIEncoding) return enc1;
        if (scanForCodeRange() == CR_7BIT) return enc2;
        return null;
    }

    public final Encoding checkEncoding(RubyString other) {
        Encoding enc = isCompatibleWith(other);
        if (enc == null) throw getRuntime().newEncodingCompatibilityError("incompatible character encodings: " + 
                                value.getEncoding() + " and " + other.value.getEncoding());
        return enc;
    }

    final Encoding checkEncoding(EncodingCapable other) {
        Encoding enc = isCompatibleWith(other);
        if (enc == null) throw getRuntime().newEncodingCompatibilityError("incompatible character encodings: " + 
                                value.getEncoding() + " and " + other.getEncoding());
        return enc;
    }

    private Encoding checkDummyEncoding() {
        Encoding enc = value.getEncoding();
        if (enc.isDummy()) throw getRuntime().newEncodingCompatibilityError(
                "incompatible encoding with this operation: " + enc);
        return enc;
    }

    private boolean isComparableWith(RubyString other) {
        ByteList otherValue = other.value;
        if (value.getEncoding() == otherValue.getEncoding() ||
            value.getRealSize() == 0 || otherValue.getRealSize() == 0) return true;
        return isComparableViaCodeRangeWith(other);
    }

    private boolean isComparableViaCodeRangeWith(RubyString other) {
        int cr1 = scanForCodeRange();
        int cr2 = other.scanForCodeRange();

        if (cr1 == CR_7BIT && (cr2 == CR_7BIT || other.value.getEncoding().isAsciiCompatible())) return true;
        if (cr2 == CR_7BIT && value.getEncoding().isAsciiCompatible()) return true;
        return false;
    }

    private int strLength(Encoding enc) {
        if (singleByteOptimizable(enc)) return value.getRealSize();
        return strLength(value, enc);
    }

    public final int strLength() {
        if (singleByteOptimizable()) return value.getRealSize();
        return strLength(value);
    }

    private int strLength(ByteList bytes) {
        return strLength(bytes, bytes.getEncoding());
    }

    private int strLength(ByteList bytes, Encoding enc) {
        if (isCodeRangeValid() && enc instanceof UTF8Encoding) return StringSupport.utf8Length(value);

        long lencr = strLengthWithCodeRange(bytes, enc);
        int cr = unpackArg(lencr);
        if (cr != 0) setCodeRange(cr);
        return unpackResult(lencr);
    }

    final int subLength(int pos) {
        if (singleByteOptimizable() || pos < 0) return pos;
        return StringSupport.strLength(value.getEncoding(), value.getUnsafeBytes(), value.getBegin(), value.getBegin() + pos);
    }

    /** short circuit for String key comparison
     * 
     */
    @Override
    public final boolean eql(IRubyObject other) {
        Ruby runtime = getRuntime();
        if (getMetaClass() != runtime.getString() || getMetaClass() != other.getMetaClass()) return super.eql(other);
        return runtime.is1_9() ? eql19(runtime, other) : eql18(runtime, other);
    }

    private boolean eql18(Ruby runtime, IRubyObject other) {
        return value.equal(((RubyString)other).value);
    }

    // rb_str_hash_cmp
    private boolean eql19(Ruby runtime, IRubyObject other) {
        RubyString otherString = (RubyString)other;
        return isComparableWith(otherString) && value.equal(((RubyString)other).value);
    }

    public RubyString(Ruby runtime, RubyClass rubyClass) {
        this(runtime, rubyClass, EMPTY_BYTE_ARRAY);
    }

    public RubyString(Ruby runtime, RubyClass rubyClass, CharSequence value) {
        super(runtime, rubyClass);
        assert value != null;
        Charset charset = null;
        Encoding defaultEncoding = runtime.getEncodingService().getLocaleEncoding();
        if (defaultEncoding == null) defaultEncoding = UTF8;

        charset = defaultEncoding.getCharset();

        // if null charset, fall back on Java default charset
        if (charset == null) charset = Charset.defaultCharset();
        
        byte[] bytes = RubyEncoding.encode(value, charset);

        this.value = new ByteList(bytes, defaultEncoding, false);
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

    public RubyString(Ruby runtime, RubyClass rubyClass, ByteList value, Encoding encoding, boolean objectSpace) {
        this(runtime, rubyClass, value, objectSpace);
        value.setEncoding(encoding);
    }

    protected RubyString(Ruby runtime, RubyClass rubyClass, ByteList value, Encoding enc, int cr) {
        this(runtime, rubyClass, value);
        value.setEncoding(enc);
        flags |= cr;
    }

    protected RubyString(Ruby runtime, RubyClass rubyClass, ByteList value, Encoding enc) {
        this(runtime, rubyClass, value);
        value.setEncoding(enc);
    }

    protected RubyString(Ruby runtime, RubyClass rubyClass, ByteList value, int cr) {
        this(runtime, rubyClass, value);
        flags |= cr;
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

    public static RubyString newStringLight(Ruby runtime, int size) {
        return new RubyString(runtime, runtime.getString(), new ByteList(size), false);
    }

    public static RubyString newStringLight(Ruby runtime, int size, Encoding encoding) {
        return new RubyString(runtime, runtime.getString(), new ByteList(size), encoding, false);
    }
  
    public static RubyString newString(Ruby runtime, CharSequence str) {
        return new RubyString(runtime, runtime.getString(), str);
    }

    public static RubyString newString(Ruby runtime, String str) {
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

    public static RubyString newString(Ruby runtime, ByteList bytes, Encoding encoding) {
        return new RubyString(runtime, runtime.getString(), bytes, encoding);
    }
    
    public static RubyString newUnicodeString(Ruby runtime, String str) {
        ByteList byteList = new ByteList(RubyEncoding.encodeUTF8(str), UTF8Encoding.INSTANCE, false);
        return new RubyString(runtime, runtime.getString(), byteList);
    }
    
    public static RubyString newUnicodeString(Ruby runtime, CharSequence str) {
        ByteList byteList = new ByteList(RubyEncoding.encodeUTF8(str), UTF8Encoding.INSTANCE, false);
        return new RubyString(runtime, runtime.getString(), byteList);
    }

    /**
     * Return a new Ruby String encoded as the default internal encoding given a Java String that
     * has come from an external source. If there is no default internal encoding set, the Ruby
     * String will be encoded using Java's default external encoding. If an internal encoding is
     * set, that encoding will be used for the Ruby String.
     *
     * @param runtime
     * @param str
     * @return
     */
    public static RubyString newInternalFromJavaExternal(Ruby runtime, String str) {
        // Ruby internal
        Encoding internal = runtime.getDefaultInternalEncoding();
        Charset rubyInt = null;
        if (internal != null && internal.getCharset() != null) rubyInt = internal.getCharset();

        // Java external, used if no internal
        Charset javaExt = Charset.defaultCharset();
        Encoding javaExtEncoding = runtime.getEncodingService().getJavaDefault();

        if (rubyInt == null) {
            return RubyString.newString(
                    runtime,
                    new ByteList(str.getBytes(), javaExtEncoding));
        } else {
            return RubyString.newString(
                    runtime,
                    new ByteList(RubyEncoding.encode(str, rubyInt), internal));
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

    public static RubyString newStringShared(Ruby runtime, ByteList bytes, Encoding encoding) {
        return newStringShared(runtime, runtime.getString(), bytes, encoding);
    }


    public static RubyString newStringShared(Ruby runtime, ByteList bytes, int codeRange) {
        RubyString str = new RubyString(runtime, runtime.getString(), bytes, codeRange);
        str.shareLevel = SHARE_LEVEL_BYTELIST;
        return str;
    }

    public static RubyString newStringShared(Ruby runtime, RubyClass clazz, ByteList bytes) {
        RubyString str = new RubyString(runtime, clazz, bytes);
        str.shareLevel = SHARE_LEVEL_BYTELIST;
        return str;
    }

    public static RubyString newStringShared(Ruby runtime, RubyClass clazz, ByteList bytes, Encoding encoding) {
        RubyString str = new RubyString(runtime, clazz, bytes, encoding);
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
    private static final class EmptyByteListHolder {
        final ByteList bytes;
        final int cr;
        EmptyByteListHolder(Encoding enc) {
            this.bytes = new ByteList(ByteList.NULL_ARRAY, enc);
            this.cr = bytes.getEncoding().isAsciiCompatible() ? CR_7BIT : CR_VALID;
        }
    }

    private static EmptyByteListHolder EMPTY_BYTELISTS[] = new EmptyByteListHolder[4];

    static EmptyByteListHolder getEmptyByteList(Encoding enc) {
        if (enc == null) enc = ASCIIEncoding.INSTANCE;
        int index = enc.getIndex();
        EmptyByteListHolder bytes;
        if (index < EMPTY_BYTELISTS.length && (bytes = EMPTY_BYTELISTS[index]) != null) {
            return bytes;
        }
        return prepareEmptyByteList(enc);
    }

    private static EmptyByteListHolder prepareEmptyByteList(Encoding enc) {
        if (enc == null) enc = ASCIIEncoding.INSTANCE;
        int index = enc.getIndex();
        if (index >= EMPTY_BYTELISTS.length) {
            EmptyByteListHolder tmp[] = new EmptyByteListHolder[index + 4];
            System.arraycopy(EMPTY_BYTELISTS,0, tmp, 0, EMPTY_BYTELISTS.length);
            EMPTY_BYTELISTS = tmp;
        }
        return EMPTY_BYTELISTS[index] = new EmptyByteListHolder(enc);
    }

    public static RubyString newEmptyString(Ruby runtime, RubyClass metaClass, Encoding enc) {
        EmptyByteListHolder holder = getEmptyByteList(enc);
        RubyString empty = new RubyString(runtime, metaClass, holder.bytes, holder.cr);
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
        return decodeString();
    }

    /**
     * Convert this Ruby string to a Java String. This version is encoding-aware.
     *
     * @return A decoded Java String, based on this Ruby string's encoding.
     */
    public String decodeString() {
        Ruby runtime = getRuntime();
        // Note: we always choose UTF-8 for outbound strings in 1.8 mode.  This is clearly undesirable
        // but we do not mark any incoming Strings from JI with their real encoding so we just pick utf-8.
        
        if (runtime.is1_9()) {
            Encoding encoding = getEncoding();
            
            if (encoding == UTF8) {
                // faster UTF8 decoding
                return RubyEncoding.decodeUTF8(value.getUnsafeBytes(), value.begin(), value.length());
            }
            
            Charset charset = runtime.getEncodingService().charsetForEncoding(encoding);

            encoding.getCharset();

            // charset is not defined for this encoding in jcodings db.  Try letting Java resolve this.
            if (charset == null) {
                try {
                    return new String(value.getUnsafeBytes(), value.begin(), value.length(), encoding.toString());
                } catch (UnsupportedEncodingException uee) {
                    return value.toString();
                }
            }
            
            return RubyEncoding.decode(value.getUnsafeBytes(), value.begin(), value.length(), charset);
        } else {
            // fast UTF8 decoding
            return RubyEncoding.decodeUTF8(value.getUnsafeBytes(), value.begin(), value.length());
        }
    }

    /**
     * Overridden dup for fast-path logic.
     *
     * @return A new RubyString sharing the original backing store.
     */
    @Override
    public IRubyObject dup() {
        RubyClass mc = metaClass.getRealClass();
        if (mc.index != ClassIndex.STRING) return super.dup();

        return strDup(mc.getClassRuntime(), mc.getRealClass());
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
        dup.flags |= flags & (CR_MASK | TAINTED_F | UNTRUSTED_F);
        
        return dup;
    }
    
    /* rb_str_subseq */
    public final RubyString makeSharedString(Ruby runtime, int index, int len) {
        return makeShared(runtime, runtime.getString(), index, len);
    }
    
    public RubyString makeSharedString19(Ruby runtime, int index, int len) {
        return makeShared19(runtime, runtime.getString(), value, index, len);
    }

    public final RubyString makeShared(Ruby runtime, int index, int len) {
        return makeShared(runtime, getType(), index, len);
    }

    public final RubyString makeShared(Ruby runtime, RubyClass meta, int index, int len) {
        final RubyString shared;
        if (len == 0) {
            shared = newEmptyString(runtime, meta);
        } else if (len == 1) {
            shared = newStringShared(runtime, meta, 
                    RubyInteger.SINGLE_CHAR_BYTELISTS[value.getUnsafeBytes()[value.getBegin() + index] & 0xff]);
        } else {
            if (shareLevel == SHARE_LEVEL_NONE) shareLevel = SHARE_LEVEL_BUFFER;
            shared = new RubyString(runtime, meta, value.makeShared(index, len));
            shared.shareLevel = SHARE_LEVEL_BUFFER;
        }

        shared.infectBy(this);
        return shared;
    }

    public final RubyString makeShared19(Ruby runtime, int index, int len) {
        return makeShared19(runtime, value, index, len);
    }

    private RubyString makeShared19(Ruby runtime, ByteList value, int index, int len) {
        return makeShared19(runtime, getType(), value, index, len);
    }
    
    private RubyString makeShared19(Ruby runtime, RubyClass meta, ByteList value, int index, int len) {
        final RubyString shared;
        Encoding enc = value.getEncoding();

        if (len == 0) {
            shared = newEmptyString(runtime, meta, enc);
        } else {
            if (shareLevel == SHARE_LEVEL_NONE) shareLevel = SHARE_LEVEL_BUFFER;
            shared = new RubyString(runtime, meta, value.makeShared(index, len));
            shared.shareLevel = SHARE_LEVEL_BUFFER;
            shared.copyCodeRangeForSubstr(this, enc); // no need to assign encoding, same bytelist shared
        }
        shared.infectBy(this);
        return shared;
    }

    public final void setByteListShared() {
        if (shareLevel != SHARE_LEVEL_BYTELIST) shareLevel = SHARE_LEVEL_BYTELIST;
    }

    /**
     * Check that the string can be modified, raising error otherwise.
     *
     * If you plan to modify a string with shared backing store, this
     * method is not sufficient; you will need to call modify() instead.
     */
    public final void modifyCheck() {
        frozenCheck();

        if (!isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw getRuntime().newSecurityError("Insecure: can't modify string");
        }
    }

    private void modifyCheck(byte[] b, int len) {
        if (value.getUnsafeBytes() != b || value.getRealSize() != len) throw getRuntime().newRuntimeError("string modified");
    }

    private void modifyCheck(byte[] b, int len, Encoding enc) {
        if (value.getUnsafeBytes() != b || value.getRealSize() != len || value.getEncoding() != enc) throw getRuntime().newRuntimeError("string modified");
    }

    private void frozenCheck() {
        frozenCheck(false);
    }

    private void frozenCheck(boolean runtimeError) {
        if (isFrozen()) throw getRuntime().newFrozenError("string", runtimeError);
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

    /** rb_str_resize
     */
    public final void resize(int length) {
        modify();
        if (value.getRealSize() > length) {
            value.setRealSize(length);
        } else if (value.length() < length) {
            value.length(length);
        }
    }

    public final void view(ByteList bytes) {
        modifyCheck();

        value = bytes;
        shareLevel = SHARE_LEVEL_NONE;
    }

    private void view(byte[]bytes) {
        modifyCheck();

        value = new ByteList(bytes);
        shareLevel = SHARE_LEVEL_NONE;

        value.invalidate();
    }

    private void view(int index, int len) {
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
        return bytesToString(bytes.getUnsafeBytes(), bytes.begin(), bytes.length());
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

    @Override
    public IRubyObject checkStringType19() {
        return this;
    }

    @JRubyMethod(name = "try_convert", meta = true, compat = RUBY1_9)
    public static IRubyObject try_convert(ThreadContext context, IRubyObject recv, IRubyObject str) {
        return str.checkStringType();
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

    @Override
    public final int compareTo(IRubyObject other) {
        Ruby runtime = getRuntime();
        if (other instanceof RubyString) {
            RubyString otherString = (RubyString)other;
            return runtime.is1_9() ? op_cmp19(otherString) : op_cmp(otherString);
        }
        return (int)op_cmpCommon(runtime.getCurrentContext(), other).convertToInteger().getLongValue();
    }

    /* rb_str_cmp_m */
    @JRubyMethod(name = "<=>", compat = RUBY1_8)
    @Override
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) {
            return context.getRuntime().newFixnum(op_cmp((RubyString)other));
        }
        return op_cmpCommon(context, other);
    }

    @JRubyMethod(name = "<=>", compat = RUBY1_9)
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
            IRubyObject result = invokedynamic(context, other, OP_CMP, this);
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
    @JRubyMethod(name = "==", compat = RUBY1_8)
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.getRuntime();
        if (this == other) return runtime.getTrue();
        if (other instanceof RubyString) {
            return value.equal(((RubyString)other).value) ? runtime.getTrue() : runtime.getFalse();
        }
        return op_equalCommon(context, other);
    }

    @JRubyMethod(name = {"==", "==="}, compat = RUBY1_9)
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
        return invokedynamic(context, other, OP_EQUAL, this).isTrue() ? runtime.getTrue() : runtime.getFalse();
    }

    @JRubyMethod(name = "+", required = 1, compat = RUBY1_8)
    public IRubyObject op_plus(ThreadContext context, IRubyObject _str) {
        RubyString str = _str.convertToString();
        RubyString resultStr = newString(context.getRuntime(), addByteLists(value, str.value));
        resultStr.infectBy(flags | str.flags);
        return resultStr;
    }

    @JRubyMethod(name = "+", required = 1, compat = RUBY1_9)
    public IRubyObject op_plus19(ThreadContext context, IRubyObject _str) {
        RubyString str = _str.convertToString();
        Encoding enc = checkEncoding(str);
        RubyString resultStr = newStringNoCopy(context.getRuntime(), addByteLists(value, str.value),
                                    enc, codeRangeAnd(getCodeRange(), str.getCodeRange()));
        resultStr.infectBy(flags | str.flags);
        return resultStr;
    }

    private ByteList addByteLists(ByteList value1, ByteList value2) {
        ByteList result = new ByteList(value1.getRealSize() + value2.getRealSize());
        result.setRealSize(value1.getRealSize() + value2.getRealSize());
        System.arraycopy(value1.getUnsafeBytes(), value1.getBegin(), result.getUnsafeBytes(), 0, value1.getRealSize());
        System.arraycopy(value2.getUnsafeBytes(), value2.getBegin(), result.getUnsafeBytes(), value1.getRealSize(), value2.getRealSize());
        return result;
    }

    @JRubyMethod(name = "*", required = 1, compat = RUBY1_8)
    public IRubyObject op_mul(ThreadContext context, IRubyObject other) {
        return multiplyByteList(context, other);
    }

    @JRubyMethod(name = "*", required = 1, compat = RUBY1_9)
    public IRubyObject op_mul19(ThreadContext context, IRubyObject other) {
        RubyString result = multiplyByteList(context, other);
        result.value.setEncoding(value.getEncoding());
        result.copyCodeRange(this);
        return result;
    }

    private RubyString multiplyByteList(ThreadContext context, IRubyObject arg) {
        int len = RubyNumeric.num2int(arg);
        if (len < 0) throw context.getRuntime().newArgumentError("negative argument");

        // we limit to int because ByteBuffer can only allocate int sizes
        if (len > 0 && Integer.MAX_VALUE / len < value.getRealSize()) {
            throw context.getRuntime().newArgumentError("argument too big");
        }

        ByteList bytes = new ByteList(len *= value.getRealSize());
        if (len > 0) {
            bytes.setRealSize(len);
            int n = value.getRealSize();
            System.arraycopy(value.getUnsafeBytes(), value.getBegin(), bytes.getUnsafeBytes(), 0, n);
            while (n <= len >> 1) {
                System.arraycopy(bytes.getUnsafeBytes(), 0, bytes.getUnsafeBytes(), n, n);
                n <<= 1;
            }
            System.arraycopy(bytes.getUnsafeBytes(), 0, bytes.getUnsafeBytes(), n, len - n);
        }
        RubyString result = new RubyString(context.getRuntime(), getMetaClass(), bytes);
        result.infectBy(this);
        return result;
    }

    @JRubyMethod(name = "%", required = 1)
    public IRubyObject op_format(ThreadContext context, IRubyObject arg) {
        return opFormatCommon(context, arg, context.getRuntime().getInstanceConfig().getCompatVersion());
    }

    private IRubyObject opFormatCommon(ThreadContext context, IRubyObject arg, CompatVersion compat) {
        IRubyObject tmp;
        if (context.runtime.is1_9() && arg instanceof RubyHash) {
            tmp = arg;
        } else {
            tmp = arg.checkArrayType();
            if (tmp.isNil()) tmp = arg;
        }

        ByteList out = new ByteList(value.getRealSize());
        out.setEncoding(value.getEncoding());

        boolean tainted;

        // FIXME: Should we make this work with platform's locale,
        // or continue hardcoding US?
        switch (compat) {
        case RUBY1_8:
            tainted = Sprintf.sprintf(out, Locale.US, value, tmp);
            break;
        case RUBY1_9:
        case RUBY2_0:
            tainted = Sprintf.sprintf1_9(out, Locale.US, value, tmp);
            break;
        default:
            throw new RuntimeException("invalid compat version for sprintf: " + compat);
        }
        RubyString str = newString(context.getRuntime(), out);

        str.setTaint(tainted || isTaint());
        return str;
    }

    @JRubyMethod(name = "hash")
    @Override
    public RubyFixnum hash() {
        Ruby runtime = getRuntime();
        return RubyFixnum.newFixnum(runtime, strHashCode(runtime));
    }

    @Override
    public int hashCode() {
        return strHashCode(getRuntime());
    }

    /**
     * Generate a murmurhash for the String, using its associated Ruby instance's hash seed.
     *
     * @param runtime
     * @return
     */
    public int strHashCode(Ruby runtime) {
        int hash = MurmurHash.hash32(value.getUnsafeBytes(), value.getBegin(), value.getRealSize(), runtime.getHashSeed());
        if (runtime.is1_9()) {
            hash ^= (value.getEncoding().isAsciiCompatible() && scanForCodeRange() == CR_7BIT ? 0 : value.getEncoding().getIndex());
        }
        return hash;
    }

    /**
     * Generate a murmurhash for the String, without a seed.
     *
     * @param runtime
     * @return
     */
    public int unseededStrHashCode(Ruby runtime) {
        int hash = MurmurHash.hash32(value.getUnsafeBytes(), value.getBegin(), value.getRealSize(), 0);
        if (runtime.is1_9()) {
            hash ^= (value.getEncoding().isAsciiCompatible() && scanForCodeRange() == CR_7BIT ? 0 : value.getEncoding().getIndex());
        }
        return hash;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;

        if (other instanceof RubyString) {
            if (((RubyString) other).value.equal(value)) return true;
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
            return value.getEncoding().getIndex() > other.value.getEncoding().getIndex() ? 1 : -1;
        }
        return ret;
    }

    /** rb_to_id
     *
     */
    @Override
    public String asJavaString() {
        return toString();
    }

    public IRubyObject doClone(){
        return newString(getRuntime(), value.dup());
    }

    public final RubyString cat(byte[] str) {
        modify(value.getRealSize() + str.length);
        System.arraycopy(str, 0, value.getUnsafeBytes(), value.getBegin() + value.getRealSize(), str.length);
        value.setRealSize(value.getRealSize() + str.length);
        return this;
    }

    public final RubyString cat(byte[] str, int beg, int len) {
        modify(value.getRealSize() + len);
        System.arraycopy(str, beg, value.getUnsafeBytes(), value.getBegin() + value.getRealSize(), len);
        value.setRealSize(value.getRealSize() + len);
        return this;
    }

    // // rb_str_buf_append
    public final RubyString cat19(RubyString str) {
        ByteList other = str.value;
        int otherCr = cat(other.getUnsafeBytes(), other.getBegin(), other.getRealSize(),
                other.getEncoding(), str.getCodeRange());
        infectBy(str);
        str.setCodeRange(otherCr);
        return this;
    }

    public final RubyString cat(RubyString str) {
        return cat(str.getByteList());
    }

    public final RubyString cat(ByteList str) {
        modify(value.getRealSize() + str.getRealSize());
        System.arraycopy(str.getUnsafeBytes(), str.getBegin(), value.getUnsafeBytes(), value.getBegin() + value.getRealSize(), str.getRealSize());
        value.setRealSize(value.getRealSize() + str.getRealSize());
        return this;
    }

    public final RubyString cat(byte ch) {
        modify(value.getRealSize() + 1);
        value.getUnsafeBytes()[value.getBegin() + value.getRealSize()] = ch;
        value.setRealSize(value.getRealSize() + 1);
        return this;
    }

    public final RubyString cat(int ch) {
        return cat((byte)ch);
    }

    public final RubyString cat(int code, Encoding enc) {
        int n = codeLength(getRuntime(), enc, code);
        modify(value.getRealSize() + n);
        enc.codeToMbc(code, value.getUnsafeBytes(), value.getBegin() + value.getRealSize());
        value.setRealSize(value.getRealSize() + n);
        return this;
    }

    public final int cat(byte[]bytes, int p, int len, Encoding enc, int cr) {
        modify(value.getRealSize() + len);
        int toCr = getCodeRange();
        Encoding toEnc = value.getEncoding();
        int cr2 = cr;

        if (toEnc == enc) {
            if (toCr == CR_UNKNOWN || (toEnc == ASCIIEncoding.INSTANCE && toCr != CR_7BIT)) {
                cr = CR_UNKNOWN;
            } else if (cr == CR_UNKNOWN) {
                cr = codeRangeScan(enc, bytes, p, len);
            }
        } else {
            if (!toEnc.isAsciiCompatible() || !enc.isAsciiCompatible()) {
                if (len == 0) return toCr;
                if (value.getRealSize() == 0) {
                    System.arraycopy(bytes, p, value.getUnsafeBytes(), value.getBegin() + value.getRealSize(), len);
                    value.setRealSize(value.getRealSize() + len);
                    setEncodingAndCodeRange(enc, cr);
                    return cr;
                }
                throw getRuntime().newEncodingCompatibilityError("incompatible character encodings: " + toEnc + " and " + enc);
            }
            if (cr == CR_UNKNOWN) cr = codeRangeScan(enc, bytes, p, len);
            if (toCr == CR_UNKNOWN) {
                if (toEnc == ASCIIEncoding.INSTANCE || cr != CR_7BIT) toCr = scanForCodeRange();
            }
        }
        if (cr2 != 0) cr2 = cr;

        if (toEnc != enc && toCr != CR_7BIT && cr != CR_7BIT) {
            throw getRuntime().newEncodingCompatibilityError("incompatible character encodings: " + toEnc + " and " + enc);
        }

        final int resCr;
        final Encoding resEnc;
        if (toCr == CR_UNKNOWN) {
            resEnc = toEnc;
            resCr = CR_UNKNOWN;
        } else if (toCr == CR_7BIT) {
            if (cr == CR_7BIT) {
                resEnc = toEnc != ASCIIEncoding.INSTANCE ? toEnc : enc;
                resCr = CR_7BIT;
            } else {
                resEnc = enc;
                resCr = cr;
            }
        } else if (toCr == CR_VALID) {
            resEnc = toEnc;
            if (cr == CR_7BIT || cr == CR_VALID) {
                resCr = toCr;
            } else {
                resCr = cr;
            }
        } else {
            resEnc = toEnc;
            resCr = len > 0 ? CR_UNKNOWN : toCr;
        }

        if (len < 0) throw getRuntime().newArgumentError("negative string size (or size too big)");

        System.arraycopy(bytes, p, value.getUnsafeBytes(), value.getBegin() + value.getRealSize(), len);
        value.setRealSize(value.getRealSize() + len);
        setEncodingAndCodeRange(resEnc, resCr);

        return cr2;
    }

    public final int cat(byte[]bytes, int p, int len, Encoding enc) {
        return cat(bytes, p, len, enc, CR_UNKNOWN);
    }

    public final RubyString catAscii(byte[]bytes, int p, int len) {
        Encoding enc = value.getEncoding();
        if (enc.isAsciiCompatible()) {
            cat(bytes, p, len, enc, CR_7BIT);
        } else {
            byte buf[] = new byte[enc.maxLength()];
            int end = p + len;
            while (p < end) {
                int c = bytes[p];
                int cl = codeLength(getRuntime(), enc, c);
                enc.codeToMbc(c, buf, 0);
                cat(buf, 0, cl, enc, CR_VALID);
                p++;
            }
        }
        return this;
    }

    /** rb_str_replace_m
     *
     */
    @JRubyMethod(name = {"replace", "initialize_copy"}, required = 1, compat = RUBY1_8)
    public IRubyObject replace(IRubyObject other) {
        if (this == other) return this;
        replaceCommon(other);
        return this;
    }

    @JRubyMethod(name = {"replace", "initialize_copy"}, required = 1, compat = RUBY1_9)
    public RubyString replace19(IRubyObject other) {
        modifyCheck();
        if (this == other) return this;
        setCodeRange(replaceCommon(other).getCodeRange()); // encoding doesn't have to be copied.
        return this;
    }

    private RubyString replaceCommon(IRubyObject other) {
        modifyCheck();
        RubyString otherStr = other.convertToString();
        otherStr.shareLevel = shareLevel = SHARE_LEVEL_BYTELIST;
        value = otherStr.value;
        infectBy(otherStr);
        return otherStr;
    }

    @JRubyMethod(name = "clear", compat = RUBY1_9)
    public RubyString clear() {
        modifyCheck();
        Encoding enc = value.getEncoding();

        EmptyByteListHolder holder = getEmptyByteList(enc);
        value = holder.bytes;
        shareLevel = SHARE_LEVEL_BYTELIST;
        setCodeRange(holder.cr);
        return this;
    }

    @JRubyMethod(name = "reverse", compat = RUBY1_8)
    public IRubyObject reverse(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() <= 1) return strDup(context.getRuntime());

        byte[]bytes = value.getUnsafeBytes();
        int p = value.getBegin();
        int len = value.getRealSize();
        byte[]obytes = new byte[len];

        for (int i = 0; i <= len >> 1; i++) {
            obytes[i] = bytes[p + len - i - 1];
            obytes[len - i - 1] = bytes[p + i];
        }

        return new RubyString(runtime, getMetaClass(), new ByteList(obytes, false)).infectBy(this);
    }

    @JRubyMethod(name = "reverse", compat = RUBY1_9)
    public IRubyObject reverse19(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() <= 1) return strDup(context.getRuntime());

        byte[]bytes = value.getUnsafeBytes();
        int p = value.getBegin();
        int len = value.getRealSize();
        byte[]obytes = new byte[len];

        boolean single = true;
        Encoding enc = value.getEncoding();
        // this really needs to be inlined here
        if (singleByteOptimizable(enc)) {
            for (int i = 0; i <= len >> 1; i++) {
                obytes[i] = bytes[p + len - i - 1];
                obytes[len - i - 1] = bytes[p + i];
            }
        } else {
            int end = p + len;
            int op = len;
            while (p < end) {
                int cl = StringSupport.length(enc, bytes, p, end);
                if (cl > 1 || (bytes[p] & 0x80) != 0) {
                    single = false;
                    op -= cl;
                    System.arraycopy(bytes, p, obytes, op, cl);
                    p += cl;
                } else {
                    obytes[--op] = bytes[p++];
                }
            }
        }

        RubyString result = new RubyString(runtime, getMetaClass(), new ByteList(obytes, false));

        if (getCodeRange() == CR_UNKNOWN) setCodeRange(single ? CR_7BIT : CR_VALID);
        Encoding encoding = value.getEncoding();
        result.value.setEncoding(encoding);
        result.copyCodeRangeForSubstr(this, encoding);
        return result.infectBy(this);
    }

    @JRubyMethod(name = "reverse!", compat = RUBY1_8)
    public RubyString reverse_bang(ThreadContext context) {
        if (value.getRealSize() > 1) {
            modify();
            byte[]bytes = value.getUnsafeBytes();
            int p = value.getBegin();
            int len = value.getRealSize();
            for (int i = 0; i < len >> 1; i++) {
                byte b = bytes[p + i];
                bytes[p + i] = bytes[p + len - i - 1];
                bytes[p + len - i - 1] = b;
            }
        }

        return this;
    }

    @JRubyMethod(name = "reverse!", compat = RUBY1_9)
    public RubyString reverse_bang19(ThreadContext context) {
        modifyCheck();
        if (value.getRealSize() > 1) {
            modifyAndKeepCodeRange();
            byte[]bytes = value.getUnsafeBytes();
            int p = value.getBegin();
            int len = value.getRealSize();

            Encoding enc = value.getEncoding();
            // this really needs to be inlined here
            if (singleByteOptimizable(enc)) {
                for (int i = 0; i < len >> 1; i++) {
                    byte b = bytes[p + i];
                    bytes[p + i] = bytes[p + len - i - 1];
                    bytes[p + len - i - 1] = b;
                }
            } else {
                int end = p + len;
                int op = len;
                byte[]obytes = new byte[len];
                boolean single = true;
                while (p < end) {
                    int cl = StringSupport.length(enc, bytes, p, end);
                    if (cl > 1 || (bytes[p] & 0x80) != 0) {
                        single = false;
                        op -= cl;
                        System.arraycopy(bytes, p, obytes, op, cl);
                        p += cl;
                    } else {
                        obytes[--op] = bytes[p++];
                    }
                }
                value.setUnsafeBytes(obytes);
                if (getCodeRange() == CR_UNKNOWN) setCodeRange(single ? CR_7BIT : CR_VALID);
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

    @JRubyMethod(visibility = PRIVATE, compat = RUBY1_8)
    @Override
    public IRubyObject initialize(ThreadContext context) {
        return this;
    }

    @JRubyMethod(visibility = PRIVATE, compat = RUBY1_8)
    public IRubyObject initialize(ThreadContext context, IRubyObject arg0) {
        replace(arg0);
        return this;
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_9)
    @Override
    public IRubyObject initialize19(ThreadContext context) {
        return this;
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize19(ThreadContext context, IRubyObject arg0) {
        replace19(arg0);
        return this;
    }

    @JRubyMethod(compat = RUBY1_8)
    public IRubyObject casecmp(ThreadContext context, IRubyObject other) {
        return RubyFixnum.newFixnum(context.getRuntime(), value.caseInsensitiveCmp(other.convertToString().value));
    }

    @JRubyMethod(name = "casecmp", compat = RUBY1_9)
    public IRubyObject casecmp19(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.getRuntime();
        RubyString otherStr = other.convertToString();
        Encoding enc = isCompatibleWith(otherStr);
        if (enc == null) return runtime.getNil();

        if (singleByteOptimizable() && otherStr.singleByteOptimizable()) {
            return RubyFixnum.newFixnum(runtime, value.caseInsensitiveCmp(otherStr.value));
        } else {
            return multiByteCasecmp(runtime, enc, value, otherStr.value);
        }
    }

    private IRubyObject multiByteCasecmp(Ruby runtime, Encoding enc, ByteList value, ByteList otherValue) {
        byte[]bytes = value.getUnsafeBytes();
        int p = value.getBegin();
        int end = p + value.getRealSize();

        byte[]obytes = otherValue.getUnsafeBytes();
        int op = otherValue.getBegin();
        int oend = op + otherValue.getRealSize();

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
                byte uc = AsciiTables.ToUpperCaseTable[c];
                byte uoc = AsciiTables.ToUpperCaseTable[oc];
                if (uc != uoc) {
                    return uc < uoc ? RubyFixnum.minus_one(runtime) : RubyFixnum.one(runtime);
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
    @JRubyMethod(name = "=~", compat = RUBY1_8, writes = BACKREF)
    @Override
    public IRubyObject op_match(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyRegexp) return ((RubyRegexp) other).op_match(context, this);
        if (other instanceof RubyString) throw context.getRuntime().newTypeError("type mismatch: String given");
        return other.callMethod(context, "=~", this);
    }

    @JRubyMethod(name = "=~", compat = RUBY1_9, writes = BACKREF)
    @Override
    public IRubyObject op_match19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyRegexp) return ((RubyRegexp) other).op_match19(context, this);
        if (other instanceof RubyString) throw context.getRuntime().newTypeError("type mismatch: String given");
        return other.callMethod(context, "=~", this);
    }
    /**
     * String#match(pattern)
     *
     * rb_str_match_m
     *
     * @param pattern Regexp or String
     */
    @JRubyMethod(compat = RUBY1_8, reads = BACKREF)
    public IRubyObject match(ThreadContext context, IRubyObject pattern) {
        return getPattern(pattern).callMethod(context, "match", this);
    }

    @JRubyMethod(name = "match", compat = RUBY1_9, reads = BACKREF)
    public IRubyObject match19(ThreadContext context, IRubyObject pattern, Block block) {
        IRubyObject result = getPattern(pattern).callMethod(context, "match", this);
        return block.isGiven() && !result.isNil() ? block.yield(context, result) : result;
    }

    @JRubyMethod(name = "match", required = 1, rest = true, compat = RUBY1_9, reads = BACKREF)
    public IRubyObject match19(ThreadContext context, IRubyObject[] args, Block block) {
        RubyRegexp pattern = getPattern(args[0]);
        args[0] = this;
        IRubyObject result = pattern.callMethod(context, "match", args);
        return block.isGiven() && !result.isNil() ? block.yield(context, result) : result;
    }

    /** rb_str_capitalize / rb_str_capitalize_bang
     *
     */
    @JRubyMethod(name = "capitalize", compat = RUBY1_8)
    public IRubyObject capitalize(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.capitalize_bang(context);
        return str;
    }

    @JRubyMethod(name = "capitalize!", compat = RUBY1_8)
    public IRubyObject capitalize_bang(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) {
            modifyCheck();
            return runtime.getNil();
        }

        modify();

        int s = value.getBegin();
        int end = s + value.getRealSize();
        byte[]bytes = value.getUnsafeBytes();
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

    @JRubyMethod(name = "capitalize", compat = RUBY1_9)
    public IRubyObject capitalize19(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.capitalize_bang19(context);
        return str;
    }

    @JRubyMethod(name = "capitalize!", compat = RUBY1_9)
    public IRubyObject capitalize_bang19(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        Encoding enc = checkDummyEncoding();

        if (value.getRealSize() == 0) {
            modifyCheck();
            return runtime.getNil();
        }

        modifyAndKeepCodeRange();

        int s = value.getBegin();
        int end = s + value.getRealSize();
        byte[]bytes = value.getUnsafeBytes();
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

    @JRubyMethod(name = ">=", compat = RUBY1_8)
    public IRubyObject op_ge(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) return context.getRuntime().newBoolean(op_cmp((RubyString) other) >= 0);
        return RubyComparable.op_ge(context, this, other);
    }

    @JRubyMethod(name = ">=", compat = RUBY1_9)
    public IRubyObject op_ge19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) return context.getRuntime().newBoolean(op_cmp19((RubyString) other) >= 0);
        return RubyComparable.op_ge(context, this, other);
    }

    @JRubyMethod(name = ">", compat = RUBY1_8)
    public IRubyObject op_gt(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) return context.getRuntime().newBoolean(op_cmp((RubyString) other) > 0);
        return RubyComparable.op_gt(context, this, other);
    }

    @JRubyMethod(name = ">", compat = RUBY1_9)
    public IRubyObject op_gt19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) return context.getRuntime().newBoolean(op_cmp19((RubyString) other) > 0);
        return RubyComparable.op_gt(context, this, other);
    }

    @JRubyMethod(name = "<=", compat = RUBY1_8)
    public IRubyObject op_le(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) return context.getRuntime().newBoolean(op_cmp((RubyString) other) <= 0);
        return RubyComparable.op_le(context, this, other);
    }

    @JRubyMethod(name = "<=", compat = RUBY1_9)
    public IRubyObject op_le19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) return context.getRuntime().newBoolean(op_cmp19((RubyString) other) <= 0);
        return RubyComparable.op_le(context, this, other);
    }

    @JRubyMethod(name = "<", compat = RUBY1_8)
    public IRubyObject op_lt(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) return context.getRuntime().newBoolean(op_cmp((RubyString) other) < 0);
        return RubyComparable.op_lt(context, this, other);
    }

    @JRubyMethod(name = "<", compat = RUBY1_9)
    public IRubyObject op_lt19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) return context.getRuntime().newBoolean(op_cmp19((RubyString) other) < 0);
        return RubyComparable.op_lt(context, this, other);
    }

    @JRubyMethod(name = "eql?", compat = RUBY1_8)
    public IRubyObject str_eql_p(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.getRuntime();
        if (other instanceof RubyString && value.equal(((RubyString)other).value)) return runtime.getTrue();
        return runtime.getFalse();
    }

    @JRubyMethod(name = "eql?", compat = RUBY1_9)
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
    @JRubyMethod(name = "upcase", compat = RUBY1_8)
    public RubyString upcase(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.upcase_bang(context);
        return str;
    }

    @JRubyMethod(name = "upcase!", compat = RUBY1_8)
    public IRubyObject upcase_bang(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) {
            modifyCheck();
            return runtime.getNil();
        }
        modify();
        return singleByteUpcase(runtime, value.getUnsafeBytes(), value.getBegin(), value.getBegin() + value.getRealSize());
    }

    @JRubyMethod(name = "upcase", compat = RUBY1_9)
    public RubyString upcase19(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.upcase_bang19(context);
        return str;
    }

    @JRubyMethod(name = "upcase!", compat = RUBY1_9)
    public IRubyObject upcase_bang19(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        Encoding enc = checkDummyEncoding();

        if (value.getRealSize() == 0) {
            modifyCheck();
            return runtime.getNil();
        }

        modifyAndKeepCodeRange();

        int s = value.getBegin();
        int end = s + value.getRealSize();
        byte[]bytes = value.getUnsafeBytes();

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
    @JRubyMethod(name = "downcase", compat = RUBY1_8)
    public RubyString downcase(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.downcase_bang(context);
        return str;
    }

    @JRubyMethod(name = "downcase!", compat = RUBY1_8)
    public IRubyObject downcase_bang(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) {
            modifyCheck();
            return runtime.getNil();
        }

        modify();
        return singleByteDowncase(runtime, value.getUnsafeBytes(), value.getBegin(), value.getBegin() + value.getRealSize());
    }

    @JRubyMethod(name = "downcase", compat = RUBY1_9)
    public RubyString downcase19(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.downcase_bang19(context);
        return str;
    }

    @JRubyMethod(name = "downcase!", compat = RUBY1_9)
    public IRubyObject downcase_bang19(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        Encoding enc = checkDummyEncoding();

        if (value.getRealSize() == 0) {
            modifyCheck();
            return runtime.getNil();
        }

        modifyAndKeepCodeRange();

        int s = value.getBegin();
        int end = s + value.getRealSize();
        byte[]bytes = value.getUnsafeBytes();

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
    @JRubyMethod(name = "swapcase", compat = RUBY1_8)
    public RubyString swapcase(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.swapcase_bang(context);
        return str;
    }

    @JRubyMethod(name = "swapcase!", compat = RUBY1_8)
    public IRubyObject swapcase_bang(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) {
            modifyCheck();
            return runtime.getNil();
        }
        modify();
        return singleByteSwapcase(runtime, value.getUnsafeBytes(), value.getBegin(), value.getBegin() + value.getRealSize());
    }

    @JRubyMethod(name = "swapcase", compat = RUBY1_9)
    public RubyString swapcase19(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.swapcase_bang19(context);
        return str;
    }

    @JRubyMethod(name = "swapcase!", compat = RUBY1_9)
    public IRubyObject swapcase_bang19(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        Encoding enc = checkDummyEncoding();
        if (value.getRealSize() == 0) {
            modifyCheck();
            return runtime.getNil();
        }
        modifyAndKeepCodeRange();

        int s = value.getBegin();
        int end = s + value.getRealSize();
        byte[]bytes = value.getUnsafeBytes();

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
    @JRubyMethod(name = "dump", compat = RUBY1_8)
    public IRubyObject dump() {
        return dumpCommon(false);
    }

    @JRubyMethod(name = "dump", compat = RUBY1_9)
    public IRubyObject dump19() {
        return dumpCommon(true);
    }

    private IRubyObject dumpCommon(boolean is1_9) {
        Ruby runtime = getRuntime();
        ByteList buf = null;
        Encoding enc = value.getEncoding();

        int p = value.getBegin();
        int end = p + value.getRealSize();
        byte[]bytes = value.getUnsafeBytes();

        int len = 2;
        while (p < end) {
            int c = bytes[p++] & 0xff;

            switch (c) {
            case '"':case '\\':case '\n':case '\r':case '\t':case '\f':
            case '\013': case '\010': case '\007': case '\033':
                len += 2;
                break;
            case '#':
                len += isEVStr(bytes, p, end) ? 2 : 1;
                break;
            default:
                if (ASCII.isPrint(c)) {
                    len++;
                } else {
                    if (is1_9 && enc instanceof UTF8Encoding) {
                        int n = StringSupport.preciseLength(enc, bytes, p - 1, end) - 1;
                        if (n > 0) {
                            if (buf == null) buf = new ByteList();
                            int cc = codePoint(runtime, enc, bytes, p - 1, end);
                            Sprintf.sprintf(runtime, buf, "%x", cc);
                            len += buf.getRealSize() + 4;
                            buf.setRealSize(0);
                            p += n;
                            break;
                        }
                    }
                    len += 4;
                }
                break;
            }
        }

        if (is1_9 && !enc.isAsciiCompatible()) {
            len += ".force_encoding(\"".length() + enc.getName().length + "\")".length();
        }

        ByteList outBytes = new ByteList(len);
        byte out[] = outBytes.getUnsafeBytes();
        int q = 0;
        p = value.getBegin();
        end = p + value.getRealSize();

        out[q++] = '"';
        while (p < end) {
            int c = bytes[p++] & 0xff;
            if (c == '"' || c == '\\') {
                out[q++] = '\\';
                out[q++] = (byte)c;
            } else if (c == '#') {
                if (isEVStr(bytes, p, end)) out[q++] = '\\';
                out[q++] = '#';
            } else if (!is1_9 && ASCII.isPrint(c)) {
                out[q++] = (byte)c;
            } else if (c == '\n') {
                out[q++] = '\\';
                out[q++] = 'n';
            } else if (c == '\r') {
                out[q++] = '\\';
                out[q++] = 'r';
            } else if (c == '\t') {
                out[q++] = '\\';
                out[q++] = 't';
            } else if (c == '\f') {
                out[q++] = '\\';
                out[q++] = 'f';
            } else if (c == '\013') {
                out[q++] = '\\';
                out[q++] = 'v';
            } else if (c == '\010') {
                out[q++] = '\\';
                out[q++] = 'b';
            } else if (c == '\007') {
                out[q++] = '\\';
                out[q++] = 'a';
            } else if (c == '\033') {
                out[q++] = '\\';
                out[q++] = 'e';
            } else if (is1_9 && ASCII.isPrint(c)) {
                out[q++] = (byte)c;
            } else {
                out[q++] = '\\';
                if (is1_9) {
                    if (enc instanceof UTF8Encoding) {
                        int n = StringSupport.preciseLength(enc, bytes, p - 1, end) - 1;
                        if (n > 0) {
                            int cc = codePoint(runtime, enc, bytes, p - 1, end);
                            p += n;
                            outBytes.setRealSize(q);
                            Sprintf.sprintf(runtime, outBytes, "u{%x}", cc);
                            q = outBytes.getRealSize();
                            continue;
                        }
                    }
                    outBytes.setRealSize(q);
                    Sprintf.sprintf(runtime, outBytes, "x%02X", c);
                    q = outBytes.getRealSize();
                } else {
                    outBytes.setRealSize(q);
                    Sprintf.sprintf(runtime, outBytes, "%03o", c);
                    q = outBytes.getRealSize();
                }
            }
        }
        out[q++] = '"';
        outBytes.setRealSize(q);
        assert out == outBytes.getUnsafeBytes(); // must not reallocate

        final RubyString result = new RubyString(runtime, getMetaClass(), outBytes);
        if (is1_9) {
            if (!enc.isAsciiCompatible()) {
                result.cat(".force_encoding(\"".getBytes());
                result.cat(enc.getName());
                result.cat((byte)'"').cat((byte)')');
                enc = ASCII;
            }
            result.associateEncoding(enc);
            result.setCodeRange(CR_7BIT);
        }
        return result.infectBy(this);
    }

    @JRubyMethod(name = "insert", compat = RUBY1_8)
    public IRubyObject insert(ThreadContext context, IRubyObject indexArg, IRubyObject stringArg) {
        assert !context.getRuntime().is1_9();
        RubyString str = stringArg.convertToString();
        int index = RubyNumeric.num2int(indexArg);
        if (index == -1) return append(stringArg);
        if (index < 0) index++;
        replaceInternal(checkIndex(index, value.getRealSize()), 0, str);
        return this;
    }

    @JRubyMethod(name = "insert", compat = RUBY1_9)
    public IRubyObject insert19(ThreadContext context, IRubyObject indexArg, IRubyObject stringArg) {
        RubyString str = stringArg.convertToString();
        int index = RubyNumeric.num2int(indexArg);
        if (index == -1) return append19(stringArg);
        if (index < 0) index++;
        replaceInternal19(checkIndex(index, strLength()), 0, str);
        return this;
    }

    private int checkIndex(int beg, int len) {
        if (beg > len) raiseIndexOutOfString(beg);
        if (beg < 0) {
            if (-beg > len) raiseIndexOutOfString(beg);
            beg += len;
        }
        return beg;
    }

    private int checkIndexForRef(int beg, int len) {
        if (beg >= len) raiseIndexOutOfString(beg);
        if (beg < 0) {
            if (-beg > len) raiseIndexOutOfString(beg);
            beg += len;
        }
        return beg;
    }

    private int checkLength(int len) {
        if (len < 0) throw getRuntime().newIndexError("negative length " + len);
        return len;
    }

    private void raiseIndexOutOfString(int index) {
        throw getRuntime().newIndexError("index " + index + " out of string");
    }

    private void prefixEscapeCat(int c) {
        cat('\\');
        cat(c);
    }

    private boolean isEVStr(byte[]bytes, int p, int end) {
        return p < end ? isEVStr(bytes[p] & 0xff) : false;
    }

    public boolean isEVStr(int c) {
        return c == '$' || c == '@' || c == '{';
    }

    /** rb_str_inspect
     *
     */
    @JRubyMethod(name = "inspect", compat = RUBY1_8)
    @Override
    public IRubyObject inspect() {
        Ruby runtime = getRuntime();
        byte bytes[] = value.getUnsafeBytes();
        int p = value.getBegin();
        int end = p + value.getRealSize();
        RubyString result = new RubyString(runtime, runtime.getString(), new ByteList(end - p));
        Encoding enc = runtime.getKCode().getEncoding();

        result.cat('"');
        while (p < end) {
            int c = bytes[p++] & 0xff;
            int n = enc.length((byte)c);

            if (n > 1 && p - 1 <= end - n) {
                result.cat(bytes, p - 1, n);
                p += n - 1;
                continue;
            } else if (c == '"'|| c == '\\' || (c == '#' && isEVStr(bytes, p, end))) {
                result.prefixEscapeCat(c);
            } else if (ASCII.isPrint(c)) {
                result.cat(c);
            } else if (c == '\n') {
                result.prefixEscapeCat('n');
            } else if (c == '\r') {
                result.prefixEscapeCat('r');
            } else if (c == '\t') {
                result.prefixEscapeCat('t');
            } else if (c == '\f') {
                result.prefixEscapeCat('f');
            } else if (c == '\013') {
                result.prefixEscapeCat('v');
            } else if (c == '\010') {
                result.prefixEscapeCat('b');
            } else if (c == '\007') {
                result.prefixEscapeCat('a');
            } else if (c == '\033') {
                result.prefixEscapeCat('e');
            } else  {
                Sprintf.sprintf(runtime, result.value, "\\%03o", c & 0377);
            }
        }
        result.cat('"');
        return result.infectBy(this);        
    }

    @JRubyMethod(name = "inspect", compat = RUBY1_9)
    public IRubyObject inspect19() {
        Ruby runtime = getRuntime();
        byte bytes[] = value.getUnsafeBytes();
        int p = value.getBegin();
        int end = p + value.getRealSize();
        RubyString result = new RubyString(runtime, runtime.getString(), new ByteList(end - p));
        Encoding enc = getEncoding();

        Encoding resultEnc = runtime.getDefaultInternalEncoding();
        if (resultEnc == null) resultEnc = runtime.getDefaultExternalEncoding();
        if (!resultEnc.isAsciiCompatible()) resultEnc = USASCIIEncoding.INSTANCE;
        result.associateEncoding(resultEnc);

        boolean isUnicode = StringSupport.isUnicode(enc);

        EncodingDB.Entry e = null;
        CaseInsensitiveBytesHash<EncodingDB.Entry> encodings = runtime.getEncodingService().getEncodings();
        if (enc == encodings.get("UTF-16".getBytes()).getEncoding() && end - p > 1) {
            int c0 = bytes[p] & 0xff;
            int c1 = bytes[p + 1] & 0xff;
            
            if (c0 == 0xFE && c1 == 0xFF) {
                e = encodings.get("UTF-16BE".getBytes());
            } else if (c0 == 0xFF && c1 == 0xFE) {
                e = encodings.get("UTF-16LE".getBytes());
            } else {
                isUnicode = false;
            }
        } else if (enc == encodings.get("UTF-32".getBytes()).getEncoding() && end - p > 3) {
            int c0 = bytes[p] & 0xff;
            int c1 = bytes[p + 1] & 0xff;
            int c2 = bytes[p + 2] & 0xff;
            int c3 = bytes[p + 3] & 0xff;
            
            if (c0 == 0 && c1 == 0 && c2 == 0xFE && c3 == 0xFF) {
                e = encodings.get("UTF-32BE".getBytes());
            } else if (c3 == 0 && c2 == 0 && c1 == 0xFE && c0 == 0xFF) {
                e = encodings.get("UTF-32LE".getBytes());
            } else {
                isUnicode = false;
            }
        }

        if (e != null) enc = e.getEncoding();

        result.cat('"');
        int prev = p;
        while (p < end) {
            int cc = 0;

            int n = StringSupport.preciseLength(enc, bytes, p, end);
            if (n <= 0) {
                if (p > prev) result.cat(bytes, prev, p - prev);
                n = enc.minLength();
                if (end < p + n) n = end - p;
                while (n-- > 0) {
                    Sprintf.sprintf(runtime, result.getByteList() ,"\\x%02X", bytes[p] & 0377);
                    prev = ++p;
                }
                continue;
            }
            int c = enc.mbcToCode(bytes, p, end);
            p += n;
            if ((enc.isAsciiCompatible() || isUnicode) &&
                    (c == '"' || c == '\\' ||
                        (c == '#' && p < end && (StringSupport.preciseLength(enc, bytes, p, end) > 0) &&
                        (cc = codePoint(runtime, enc, bytes, p, end)) == '$' || cc == '@' || cc == '{'))) {
                if (p - n > prev) result.cat(bytes, prev, p - n - prev);
                result.cat('\\');
                if (enc.isAsciiCompatible() || enc == resultEnc) {
                    prev = p - n;
                    continue;
                }
            }

            switch (c) {
            case '\n': cc = 'n'; break;
            case '\r': cc = 'r'; break;
            case '\t': cc = 't'; break;
            case '\f': cc = 'f'; break;
            case '\013': cc = 'v'; break;
            case '\010': cc = 'b'; break;
            case '\007': cc = 'a'; break;
            case 033: cc = 'e'; break;
            default: cc = 0; break;
            }

            if (cc != 0) {
                if (p - n > prev) result.cat(bytes, prev, p - n - prev);
                result.cat('\\');
                result.cat(cc);
                prev = p;
                continue;
            }

            if ((enc == resultEnc && enc.isPrint(c)) || (enc.isAsciiCompatible() && Encoding.isAscii(c) && enc.isPrint(c))) {
                continue;
            } else {
                if (p - n > prev) result.cat(bytes, prev, p - n - prev);
                Sprintf.sprintf(runtime, result.getByteList() , StringSupport.escapedCharFormat(c, isUnicode), c);
                prev = p;
                continue;
            }
        }

        if (p > prev) result.cat(bytes, prev, p - prev);
        result.cat('"');
        return result.infectBy(this);
    }

    public int size() {
        return value.getRealSize();
    }

    /** rb_str_length
     *
     */
    @JRubyMethod(name = {"length", "size"}, compat = RUBY1_8)
    public RubyFixnum length() {
        return getRuntime().newFixnum(value.getRealSize());
    }

    @JRubyMethod(name = {"length", "size"}, compat = RUBY1_9)
    public RubyFixnum length19() {
        return getRuntime().newFixnum(strLength());
    }

    @JRubyMethod(name = "bytesize")
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
        RubyString otherStr = other.convertToString();
        infectBy(otherStr);
        return cat(otherStr.value);
    }

    public RubyString append19(IRubyObject other) {
        return cat19(other.convertToString());
    }

    /** rb_str_concat
     *
     */
    @JRubyMethod(name = {"concat", "<<"}, compat = RUBY1_8)
    public RubyString concat(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            long longValue = ((RubyFixnum) other).getLongValue();
            if (longValue >= 0 && longValue < 256) return cat((byte) longValue);
        }
        return append(other);
    }

    @JRubyMethod(name = {"concat", "<<"}, compat = RUBY1_9)
    public RubyString concat19(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.getRuntime();
        if (other instanceof RubyFixnum) {
            int c = RubyNumeric.num2int(other);
            if (c < 0) {
                throw runtime.newRangeError("negative string size (or size too big)");
            }
            return concatNumeric(runtime, c);
        } else if (other instanceof RubyBignum) {
            if (((RubyBignum) other).getBigIntegerValue().signum() < 0) {
                throw runtime.newRangeError("negative string size (or size too big)");
            }
            long c = ((RubyBignum) other).getLongValue();
            return concatNumeric(runtime, (int) c);
        }
        return append19(other);
    }

    private RubyString concatNumeric(Ruby runtime, int c) {
        Encoding enc = value.getEncoding();
        int cl = codeLength(runtime, enc, c);
        modify19(value.getRealSize() + cl);
        enc.codeToMbc(c, value.getUnsafeBytes(), value.getBegin() + value.getRealSize());
        value.setRealSize(value.getRealSize() + cl);
        return this;
    }

    /**
     * rb_str_prepend
     */
    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject prepend(ThreadContext context, IRubyObject other) {
        return replace19(other.convertToString().op_plus19(context, this));
    }

    /** rb_str_crypt
     *
     */
    @JRubyMethod(name = "crypt")
    public RubyString crypt(ThreadContext context, IRubyObject other) {
        RubyString otherStr = other.convertToString();
        ByteList salt = otherStr.getByteList();
        if (salt.getRealSize() < 2) {
            throw context.getRuntime().newArgumentError("salt too short(need >=2 bytes)");
        }

        salt = salt.makeShared(0, 2);
        RubyString result = RubyString.newStringShared(context.getRuntime(), JavaCrypt.crypt(salt, this.getByteList()));
        result.infectBy(this);
        result.infectBy(otherStr);
        return result;
    }

    /* RubyString aka rb_string_value */
    public static RubyString stringValue(IRubyObject object) {
        return (RubyString) (object instanceof RubyString ? object :
            object.convertToString());
    }

    /** rb_str_sub / rb_str_sub_bang
     *
     */
    @JRubyMethod(reads = BACKREF, writes = BACKREF, compat = RUBY1_8)
    public IRubyObject sub(ThreadContext context, IRubyObject arg0, Block block) {
        RubyString str = strDup(context.getRuntime());
        str.sub_bang(context, arg0, block);
        return str;
    }

    @JRubyMethod(reads = BACKREF, writes = BACKREF, compat = RUBY1_8)
    public IRubyObject sub(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        RubyString str = strDup(context.getRuntime());
        str.sub_bang(context, arg0, arg1, block);
        return str;
    }

    @JRubyMethod(name = "sub!", reads = BACKREF, writes = BACKREF, compat = RUBY1_8)
    public IRubyObject sub_bang(ThreadContext context, IRubyObject arg0, Block block) {
        if (block.isGiven()) return subBangIter(context, getQuotedPattern(arg0), block);
        throw context.getRuntime().newArgumentError(1, 2);
    }

    @JRubyMethod(name = "sub!", reads = BACKREF, writes = BACKREF, compat = RUBY1_8)
    public IRubyObject sub_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return subBangNoIter(context, getQuotedPattern(arg0), arg1.convertToString());
    }

    private IRubyObject subBangIter(ThreadContext context, Regex pattern, Block block) {
        int range = value.getBegin() + value.getRealSize();
        Matcher matcher = pattern.matcher(value.getUnsafeBytes(), value.getBegin(), range);

        DynamicScope scope = context.getCurrentScope();
        if (matcher.search(value.getBegin(), range, Option.NONE) >= 0) {
            frozenCheck(true);
            byte[] bytes = value.getUnsafeBytes();
            int size = value.getRealSize();
            RubyMatchData match = RubyRegexp.updateBackRef(context, this, scope, matcher, pattern);
            RubyString repl = objAsString(context, block.yield(context,
                    makeShared(context.getRuntime(), matcher.getBegin(), matcher.getEnd() - matcher.getBegin())));
            modifyCheck(bytes, size);
            frozenCheck(true);
            scope.setBackRef(match);
            return subBangCommon(context, pattern, matcher, repl, repl.flags);
        } else {
            return scope.setBackRef(context.getRuntime().getNil());
        }
    }

    private IRubyObject subBangNoIter(ThreadContext context, Regex pattern, RubyString repl) {
        int tuFlags = repl.flags;
        int range = value.getBegin() + value.getRealSize();
        Matcher matcher = pattern.matcher(value.getUnsafeBytes(), value.getBegin(), range);

        DynamicScope scope = context.getCurrentScope();
        if (matcher.search(value.getBegin(), range, Option.NONE) >= 0) {
            repl = RubyRegexp.regsub(repl, this, matcher, context.getRuntime().getKCode().getEncoding());
            RubyRegexp.updateBackRef(context, this, scope, matcher, pattern);
            return subBangCommon(context, pattern, matcher, repl, tuFlags);
        } else {
            return scope.setBackRef(context.getRuntime().getNil());
        }
    }

    private IRubyObject subBangCommon(ThreadContext context, Regex pattern, Matcher matcher, RubyString repl, int tuFlags) {
        final int beg = matcher.getBegin();
        final int plen = matcher.getEnd() - beg;

        ByteList replValue = repl.value;
        if (replValue.getRealSize() > plen) {
            modify(value.getRealSize() + replValue.getRealSize() - plen);
        } else {
            modify();
        }

        if (replValue.getRealSize() != plen) {
            int src = value.getBegin() + beg + plen;
            int dst = value.getBegin() + beg + replValue.getRealSize();
            int length = value.getRealSize() - beg - plen;
            System.arraycopy(value.getUnsafeBytes(), src, value.getUnsafeBytes(), dst, length);
        }
        System.arraycopy(replValue.getUnsafeBytes(), replValue.getBegin(), value.getUnsafeBytes(), value.getBegin() + beg, replValue.getRealSize());
        value.setRealSize(value.getRealSize() + replValue.getRealSize() - plen);
        infectBy(tuFlags);
        return this;
    }

    @JRubyMethod(name = "sub", reads = BACKREF, writes = BACKREF, compat = RUBY1_9)
    public IRubyObject sub19(ThreadContext context, IRubyObject arg0, Block block) {
        RubyString str = strDup(context.getRuntime());
        str.sub_bang19(context, arg0, block);
        return str;
    }

    @JRubyMethod(name = "sub", reads = BACKREF, writes = BACKREF, compat = RUBY1_9)
    public IRubyObject sub19(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        RubyString str = strDup(context.getRuntime());
        str.sub_bang19(context, arg0, arg1, block);
        return str;
    }

    @JRubyMethod(name = "sub!", reads = BACKREF, writes = BACKREF, compat = RUBY1_9)
    public IRubyObject sub_bang19(ThreadContext context, IRubyObject arg0, Block block) {
        Ruby runtime = context.getRuntime();
        frozenCheck();

        final Regex pattern, prepared;
        final RubyRegexp regexp;
        if (arg0 instanceof RubyRegexp) {
            regexp = (RubyRegexp)arg0;
            pattern = regexp.getPattern();
            prepared = regexp.preparePattern(this);
        } else {
            regexp = null;
            pattern = getStringPattern19(runtime, arg0);
            prepared = RubyRegexp.preparePattern(runtime, pattern, this);
        }

        if (block.isGiven()) return subBangIter19(runtime, context, pattern, prepared, null, block, regexp);
        throw context.getRuntime().newArgumentError(1, 2);
    }

    @JRubyMethod(name = "sub!", reads = BACKREF, writes = BACKREF, compat = RUBY1_9)
    public IRubyObject sub_bang19(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        Ruby runtime = context.getRuntime();
        IRubyObject hash = TypeConverter.convertToTypeWithCheck(arg1, runtime.getHash(), "to_hash");
        frozenCheck();

        final Regex pattern, prepared;
        final RubyRegexp regexp;
        if (arg0 instanceof RubyRegexp) {
            regexp = (RubyRegexp)arg0;
            pattern = regexp.getPattern();
            prepared = regexp.preparePattern(this);
        } else {
            regexp = null;
            pattern = getStringPattern19(runtime, arg0);
            prepared = RubyRegexp.preparePattern(runtime, pattern, this);
        }

        if (hash.isNil()) {
            return subBangNoIter19(runtime, context, pattern, prepared, arg1.convertToString(), regexp);
        } else {
            return subBangIter19(runtime, context, pattern, prepared, (RubyHash)hash, block, regexp);
        }
    }

    private IRubyObject subBangIter19(Ruby runtime, ThreadContext context, Regex pattern, Regex prepared, RubyHash hash, Block block, RubyRegexp regexp) {
        int begin = value.getBegin();
        int len = value.getRealSize();
        int range = begin + len;
        byte[]bytes = value.getUnsafeBytes();
        Encoding enc = value.getEncoding();
        final Matcher matcher = prepared.matcher(bytes, begin, range);

        DynamicScope scope = context.getCurrentScope();
        if (matcher.search(begin, range, Option.NONE) >= 0) {
            RubyMatchData match = RubyRegexp.updateBackRef19(context, this, scope, matcher, pattern);
            match.regexp = regexp;
            final RubyString repl;
            final int tuFlags;
            IRubyObject subStr = makeShared19(runtime, matcher.getBegin(), matcher.getEnd() - matcher.getBegin());
            if (hash == null) {
                tuFlags = 0;
                repl = objAsString(context, block.yield(context, subStr));
            } else {
                tuFlags = hash.flags;
                repl = objAsString(context, hash.op_aref(context, subStr));
            }

            modifyCheck(bytes, len, enc);
            frozenCheck();
            scope.setBackRef(match);
            return subBangCommon19(context, pattern, matcher, repl, tuFlags | repl.flags);
        } else {
            return scope.setBackRef(runtime.getNil());
        }
    }

    private IRubyObject subBangNoIter19(Ruby runtime, ThreadContext context, Regex pattern, Regex prepared, RubyString repl, RubyRegexp regexp) {
        int begin = value.getBegin();
        int range = begin + value.getRealSize();
        final Matcher matcher = prepared.matcher(value.getUnsafeBytes(), begin, range);

        DynamicScope scope = context.getCurrentScope();
        if (matcher.search(begin, range, Option.NONE) >= 0) {
            repl = RubyRegexp.regsub19(repl, this, matcher, pattern);
            RubyMatchData match = RubyRegexp.updateBackRef19(context, this, scope, matcher, pattern);
            match.regexp = regexp;
            return subBangCommon19(context, pattern, matcher, repl, repl.flags);
        } else {
            return scope.setBackRef(runtime.getNil());
        }
    }

    private IRubyObject subBangCommon19(ThreadContext context, Regex pattern, Matcher matcher, RubyString repl, int tuFlags) {
        final int beg = matcher.getBegin();
        final int end = matcher.getEnd();
        int cr = getCodeRange();

        Encoding enc = isCompatibleWith(repl);
        if (enc == null) enc = subBangVerifyEncoding(context, repl, beg, end);

        final int plen = end - beg;
        ByteList replValue = repl.value;
        if (replValue.getRealSize() > plen) {
            modify19(value.getRealSize() + replValue.getRealSize() - plen);
        } else {
            modify19();
        }

        associateEncoding(enc);

        if (cr > CR_UNKNOWN && cr < CR_BROKEN) {
            int cr2 = repl.getCodeRange();
            if (cr2 == CR_BROKEN || (cr == CR_VALID && cr2 == CR_7BIT)) {
                cr = CR_UNKNOWN;
            } else {
                cr = cr2;
            }
        }

        if (replValue.getRealSize() != plen) {
            int src = value.getBegin() + beg + plen;
            int dst = value.getBegin() + beg + replValue.getRealSize();
            int length = value.getRealSize() - beg - plen;
            System.arraycopy(value.getUnsafeBytes(), src, value.getUnsafeBytes(), dst, length);
        }
        System.arraycopy(replValue.getUnsafeBytes(), replValue.getBegin(), value.getUnsafeBytes(), value.getBegin() + beg, replValue.getRealSize());
        value.setRealSize(value.getRealSize() + replValue.getRealSize() - plen);
        setCodeRange(cr);
        return infectBy(tuFlags);
    }

    private Encoding subBangVerifyEncoding(ThreadContext context, RubyString repl, int beg, int end) {
        byte[]bytes = value.getUnsafeBytes();
        int p = value.getBegin();
        int len = value.getRealSize();
        Encoding strEnc = value.getEncoding();
        if (codeRangeScan(strEnc, bytes, p, beg) != CR_7BIT ||
            codeRangeScan(strEnc, bytes, p + end, len - end) != CR_7BIT) {
            throw context.getRuntime().newArgumentError(
                    "incompatible character encodings " + strEnc + " and " + repl.value.getEncoding());
        }
        return repl.value.getEncoding();
    }

    /** rb_str_gsub / rb_str_gsub_bang
     *
     */
    @JRubyMethod(reads = BACKREF, writes = BACKREF, compat = RUBY1_8)
    public IRubyObject gsub(ThreadContext context, IRubyObject arg0, Block block) {
        return gsub(context, arg0, block, false);
    }

    @JRubyMethod(reads = BACKREF, writes = BACKREF, compat = RUBY1_8)
    public IRubyObject gsub(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return gsub(context, arg0, arg1, block, false);
    }

    @JRubyMethod(name = "gsub!", reads = BACKREF, writes = BACKREF, compat = RUBY1_8)
    public IRubyObject gsub_bang(ThreadContext context, IRubyObject arg0, Block block) {
        return gsub(context, arg0, block, true);
    }

    @JRubyMethod(name = "gsub!", reads = BACKREF, writes = BACKREF, compat = RUBY1_8)
    public IRubyObject gsub_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return gsub(context, arg0, arg1, block, true);
    }

    private IRubyObject gsub(ThreadContext context, IRubyObject arg0, Block block, final boolean bang) {
        if (block.isGiven()) {
            return gsubCommon(context, bang, arg0, block, null, 0);
        } else {
            return enumeratorize(context.getRuntime(), this, bang ? "gsub!" : "gsub", arg0);
        }
    }

    private IRubyObject gsub(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block, final boolean bang) {
        RubyString repl = arg1.convertToString();
        return gsubCommon(context, bang, arg0, block, repl, repl.flags);
    }

    private IRubyObject gsubCommon(ThreadContext context, final boolean bang, IRubyObject arg, Block block, RubyString repl, int tuFlags) {
        Ruby runtime = context.getRuntime();
        DynamicScope scope = context.getCurrentScope();
        Regex pattern = getQuotedPattern(arg);

        int begin = value.getBegin();
        int slen = value.getRealSize();
        int range = begin + slen;
        byte[]bytes = value.getUnsafeBytes();
        Matcher matcher = pattern.matcher(bytes, begin, range);

        int beg = matcher.search(begin, range, Option.NONE);
        if (beg < 0) {
            scope.setBackRef(runtime.getNil());
            return bang ? runtime.getNil() : strDup(runtime); /* bang: true, no match, no substitution */
        } else if (repl == null && bang && isFrozen()) {
            throw getRuntime().newRuntimeError("can't modify frozen string");
        }

        int blen = slen + 30; /* len + margin */
        ByteList dest = new ByteList(blen);
        dest.setRealSize(blen);
        int offset = 0, buf = 0, bp = 0, cp = begin;

        Encoding enc = getEncodingForKCodeDefault(runtime, pattern, arg);

        RubyMatchData match = null;
        while (beg >= 0) {
            final RubyString val;
            final int begz = matcher.getBegin();
            final int endz = matcher.getEnd();

            if (repl == null) { // block given
                match = RubyRegexp.updateBackRef(context, this, scope, matcher, pattern);
                val = objAsString(context, block.yield(context, substr(runtime, begz, endz - begz)));
                modifyCheck(bytes, slen);
                if (bang) frozenCheck();
            } else {
                val = RubyRegexp.regsub(repl, this, matcher, enc);
            }

            tuFlags |= val.flags;

            ByteList vbuf = val.value;
            int len = (bp - buf) + (beg - offset) + vbuf.getRealSize() + 3;
            if (blen < len) {
                while (blen < len) blen <<= 1;
                len = bp - buf;
                dest.realloc(blen);
                dest.setRealSize(blen);
                bp = buf + len;
            }
            len = beg - offset; /* copy pre-match substr */
            System.arraycopy(bytes, cp, dest.getUnsafeBytes(), bp, len);
            bp += len;
            System.arraycopy(vbuf.getUnsafeBytes(), vbuf.getBegin(), dest.getUnsafeBytes(), bp, vbuf.getRealSize());
            bp += vbuf.getRealSize();
            offset = endz;

            if (begz == endz) {
                if (slen <= endz) break;
                len = enc.length(bytes, begin + endz, range);
                System.arraycopy(bytes, begin + endz, dest.getUnsafeBytes(), bp, len);
                bp += len;
                offset = endz + len;
            }
            cp = begin + offset;
            if (offset > slen) break;
            beg = matcher.search(cp, range, Option.NONE);
        }

        if (repl == null) { // block given
            scope.setBackRef(match);
        } else {
            RubyRegexp.updateBackRef(context, this, scope, matcher, pattern);
        }

        if (slen > offset) {
            int len = bp - buf;
            if (blen - len < slen - offset) {
                blen = len + slen - offset;
                dest.realloc(blen);
                bp = buf + len;
            }
            System.arraycopy(bytes, cp, dest.getUnsafeBytes(), bp, slen - offset);
            bp += slen - offset;
        }

        dest.setRealSize(bp - buf);
        if (bang) {
            view(dest);
            return infectBy(tuFlags);
        } else {
            return new RubyString(runtime, getMetaClass(), dest).infectBy(tuFlags | flags);
        }
    }

    @JRubyMethod(name = "gsub", reads = BACKREF, writes = BACKREF, compat = RUBY1_9)
    public IRubyObject gsub19(ThreadContext context, IRubyObject arg0, Block block) {
        return block.isGiven() ? gsubCommon19(context, block, null, null, arg0, false, 0) : enumeratorize(context.getRuntime(), this, "gsub", arg0);
    }

    @JRubyMethod(name = "gsub", reads = BACKREF, writes = BACKREF, compat = RUBY1_9)
    public IRubyObject gsub19(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return gsub19(context, arg0, arg1, block, false);
    }

    @JRubyMethod(name = "gsub!", reads = BACKREF, writes = BACKREF, compat = RUBY1_9)
    public IRubyObject gsub_bang19(ThreadContext context, IRubyObject arg0, Block block) {
        checkFrozen();
        return block.isGiven() ? gsubCommon19(context, block, null, null, arg0, true, 0) : enumeratorize(context.getRuntime(), this, "gsub!", arg0);
    }

    @JRubyMethod(name = "gsub!", reads = BACKREF, writes = BACKREF, compat = RUBY1_9)
    public IRubyObject gsub_bang19(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        checkFrozen();
        return gsub19(context, arg0, arg1, block, true);
    }

    private IRubyObject gsub19(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block, final boolean bang) {
        Ruby runtime = context.getRuntime();
        IRubyObject tryHash = TypeConverter.convertToTypeWithCheck(arg1, runtime.getHash(), "to_hash");

        final RubyHash hash;
        final RubyString str;
        final int tuFlags;
        if (tryHash.isNil()) {
            hash = null;
            str = arg1.convertToString();
            tuFlags = str.flags;
        } else {
            hash = (RubyHash)tryHash;
            str = null;
            tuFlags = hash.flags & TAINTED_F;
        }

        return gsubCommon19(context, block, str, hash, arg0, bang, tuFlags);
    }

    private IRubyObject gsubCommon19(ThreadContext context, Block block, RubyString repl,
            RubyHash hash, IRubyObject arg0, final boolean bang, int tuFlags) {
        Ruby runtime = context.getRuntime();

        final Regex pattern, prepared;
        final RubyRegexp regexp;
        if (arg0 instanceof RubyRegexp) {
            regexp = (RubyRegexp)arg0;
            pattern = regexp.getPattern();
            prepared = regexp.preparePattern(this);
        } else {
            regexp = null;
            pattern = getStringPattern19(runtime, arg0);
            prepared = RubyRegexp.preparePattern(runtime, pattern, this);
        }

        final int begin = value.getBegin();
        int slen = value.getRealSize();
        final int range = begin + slen;
        byte[]bytes = value.getUnsafeBytes();
        final Matcher matcher = prepared.matcher(bytes, begin, range);

        final DynamicScope scope = context.getCurrentScope();
        int beg = matcher.search(begin, range, Option.NONE);
        if (beg < 0) {
            scope.setBackRef(runtime.getNil());
            return bang ? runtime.getNil() : strDup(runtime); /* bang: true, no match, no substitution */
        }

        RubyString dest = new RubyString(runtime, getMetaClass(), new ByteList(slen + 30));
        int offset = 0, cp = begin;
        Encoding enc = value.getEncoding();
        dest.setEncoding(enc);
        dest.setCodeRange(enc.isAsciiCompatible() ? CR_7BIT : CR_VALID);

        RubyMatchData match = null;
        do {
            final RubyString val;
            int begz = matcher.getBegin();
            int endz = matcher.getEnd();

            if (repl != null) {     // string given
                val = RubyRegexp.regsub19(repl, this, matcher, pattern);
            } else {
                final RubyString substr = makeShared19(runtime, begz, endz - begz);  
                if (hash != null) { // hash given
                    val = objAsString(context, hash.op_aref(context, substr)); 
                } else {            // block given
                    match = RubyRegexp.updateBackRef19(context, this, scope, matcher, pattern);
                    match.regexp = regexp;
                    val = objAsString(context, block.yield(context, substr));
                }
                modifyCheck(bytes, slen, enc);
                if (bang) frozenCheck();
            }

            tuFlags |= val.flags;

            int len = beg - offset;
            if (len != 0) dest.cat(bytes, cp, len, enc);
            dest.cat19(val);
            offset = endz;
            if (begz == endz) {
                if (slen <= endz) break;
                len = StringSupport.length(enc, bytes, begin + endz, range);
                dest.cat(bytes, begin + endz, len, enc);
                offset = endz + len;
            }
            cp = begin + offset;
            if (offset > slen) break;
            beg = matcher.search(cp, range, Option.NONE);
        } while (beg >= 0);

        if (slen > offset) dest.cat(bytes, cp, slen - offset, enc);

        if (match != null) { // block given
            scope.setBackRef(match);
        } else {
            match = RubyRegexp.updateBackRef19(context, this, scope, matcher, pattern);
            match.regexp = regexp;
        }

        if (bang) {
            view(dest.value);
            setCodeRange(dest.getCodeRange());
            return infectBy(tuFlags);
        } else {
            return dest.infectBy(tuFlags | flags);
        }
    }

    /** rb_str_index_m
     *
     */
    @JRubyMethod(name = "index", reads = BACKREF, writes = BACKREF, compat = RUBY1_8)
    public IRubyObject index(ThreadContext context, IRubyObject arg0) {
        return indexCommon(context.getRuntime(), context, arg0, 0);
    }

    @JRubyMethod(name = "index", reads = BACKREF, writes = BACKREF, compat = RUBY1_8)
    public IRubyObject index(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        int pos = RubyNumeric.num2int(arg1);
        Ruby runtime = context.getRuntime();
        if (pos < 0) {
            pos += value.getRealSize();
            if (pos < 0) {
                if (arg0 instanceof RubyRegexp) {
                    context.getCurrentScope().setBackRef(runtime.getNil());
                }
                return runtime.getNil();
            }
        }
        return indexCommon(runtime, context, arg0, pos);
    }

    private IRubyObject indexCommon(Ruby runtime, ThreadContext context, IRubyObject sub, int pos) {
        if (sub instanceof RubyRegexp) {
            RubyRegexp regSub = (RubyRegexp) sub;

            pos = regSub.adjustStartPos(this, pos, false);
            pos = regSub.search(context, this, pos, false);
        } else if (sub instanceof RubyFixnum) {
            int c_int = RubyNumeric.fix2int((RubyFixnum)sub);
            if (c_int < 0x00 || c_int > 0xFF) {
                // out of byte range
                // there will be no match for sure
                return runtime.getNil();
            }
            byte c = (byte) c_int;
            byte[] bytes = value.getUnsafeBytes();
            int end = value.getBegin() + value.getRealSize();

            pos += value.getBegin();
            for (; pos < end; pos++) {
                if (bytes[pos] == c) return RubyFixnum.newFixnum(runtime, pos - value.getBegin());
            }
            return runtime.getNil();
        } else if (sub instanceof RubyString) {
            pos = strIndex((RubyString) sub, pos);
        } else {
            IRubyObject tmp = sub.checkStringType();
            if (tmp.isNil()) throw runtime.newTypeError("type mismatch: " + sub.getMetaClass().getName() + " given");
            pos = strIndex((RubyString) tmp, pos);
        }

        return pos == -1 ? runtime.getNil() : RubyFixnum.newFixnum(runtime, pos);
    }

    private int strIndex(RubyString sub, int offset) {
        ByteList byteList = value;
        if (offset < 0) {
            offset += byteList.getRealSize();
            if (offset < 0) return -1;
        }

        ByteList other = sub.value;
        if (sizeIsSmaller(byteList, offset, other)) return -1;
        if (other.getRealSize() == 0) return offset;
        return byteList.indexOf(other, offset);
    }

    private static boolean sizeIsSmaller(ByteList byteList, int offset, ByteList other) {
        return byteList.getRealSize() - offset < other.getRealSize();
    }

    @JRubyMethod(name = "index", reads = BACKREF, writes = BACKREF, compat = RUBY1_9)
    public IRubyObject index19(ThreadContext context, IRubyObject arg0) {
        return indexCommon19(context.getRuntime(), context, arg0, 0);
    }
    
    @JRubyMethod(name = "index", reads = BACKREF, writes = BACKREF, compat = RUBY1_9)
    public IRubyObject index19(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        int pos = RubyNumeric.num2int(arg1);
        Ruby runtime = context.getRuntime();
        if (pos < 0) {
            pos += strLength();
            if (pos < 0) {
                if (arg0 instanceof RubyRegexp) context.getCurrentScope().setBackRef(runtime.getNil());
                return runtime.getNil();
            }
        }
        return indexCommon19(runtime, context, arg0, pos);
    }

    private IRubyObject indexCommon19(Ruby runtime, ThreadContext context, IRubyObject sub, int pos) {
        if (sub instanceof RubyRegexp) {
            RubyRegexp regSub = (RubyRegexp) sub;
            pos = singleByteOptimizable() ? pos : 
                    StringSupport.nth(checkEncoding(regSub), value.getUnsafeBytes(), value.getBegin(),
                            value.getBegin() + value.getRealSize(),
                            pos);
            pos = regSub.adjustStartPos19(this, pos, false);
            pos = regSub.search19(context, this, pos, false);
            pos = subLength(pos);
        } else if (sub instanceof RubyString) {
            pos = strIndex19((RubyString) sub, pos);
            pos = subLength(pos);
        } else {
            IRubyObject tmp = sub.checkStringType();
            if (tmp.isNil()) throw runtime.newTypeError("type mismatch: " + sub.getMetaClass().getName() + " given");
            pos = strIndex19((RubyString) tmp, pos);
            pos = subLength(pos);
        }

        return pos == -1 ? runtime.getNil() : RubyFixnum.newFixnum(runtime, pos);
    }

    private int strIndex19(RubyString sub, int offset) {
        Encoding enc = checkEncoding(sub);
        if (sub.scanForCodeRange() == CR_BROKEN) return -1;
        int len = strLength(enc);
        int slen = sub.strLength(enc);
        if (offset < 0) {
            offset += len;
            if (offset < 0) return -1;
        }

        if (len - offset < slen) return -1;
        byte[]bytes = value.getUnsafeBytes();
        int p = value.getBegin();
        int end = p + value.getRealSize();
        if (offset != 0) {
            offset = singleByteOptimizable() ? offset : StringSupport.offset(enc, bytes, p, end, offset);
            p += offset;
        }
        if (slen == 0) return offset;

        while (true) {
            int pos = value.indexOf(sub.value, p - value.getBegin());
            if (pos < 0) return pos;
            pos -= (p - value.getBegin());
            int t = enc.rightAdjustCharHead(bytes, p, p + pos, end);
            if (t == p + pos) return pos + offset;
            if ((len -= t - p) <= 0) return -1;
            offset += t - p;
            p = t;
        }
    }

    /** rb_str_rindex_m
     *
     */
    @JRubyMethod(name = "rindex", reads = BACKREF, writes = BACKREF, compat = RUBY1_8)
    public IRubyObject rindex(ThreadContext context, IRubyObject arg0) {
        return rindexCommon(context.getRuntime(), context, arg0, value.getRealSize());
    }

    @JRubyMethod(name = "rindex", reads = BACKREF, writes = BACKREF, compat = RUBY1_8)
    public IRubyObject rindex(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        int pos = RubyNumeric.num2int(arg1);
        Ruby runtime = context.getRuntime();
        if (pos < 0) {
            pos += value.getRealSize();
            if (pos < 0) {
                if (arg0 instanceof RubyRegexp) context.getCurrentScope().setBackRef(runtime.getNil());
                return runtime.getNil();
            }
        }            
        if (pos > value.getRealSize()) pos = value.getRealSize();
        return rindexCommon(runtime, context, arg0, pos);
    }

    private IRubyObject rindexCommon(Ruby runtime, ThreadContext context, final IRubyObject sub, int pos) {
        if (sub instanceof RubyRegexp) {
            RubyRegexp regSub = (RubyRegexp) sub;
            if (regSub.length() > 0) {
                pos = regSub.adjustStartPos(this, pos, true);
                pos = regSub.search(context, this, pos, true);
            }
        } else if (sub instanceof RubyString) {
            pos = strRindex((RubyString) sub, pos);
        } else if (sub instanceof RubyFixnum) {
            int c_int = RubyNumeric.fix2int((RubyFixnum)sub);
            if (c_int < 0x00 || c_int > 0xFF) {
                // out of byte range
                // there will be no match for sure
                return runtime.getNil();
            }
            byte c = (byte) c_int;

            byte[] bytes = value.getUnsafeBytes();
            int pbeg = value.getBegin();
            int p = pbeg + pos;

            if (pos == value.getRealSize()) {
                if (pos == 0) return runtime.getNil();
                --p;
            }
            while (pbeg <= p) {
                if (bytes[p] == c) return RubyFixnum.newFixnum(runtime, p - value.getBegin());
                p--;
            }
            return runtime.getNil();
        } else {
            IRubyObject tmp = sub.checkStringType();
            if (tmp.isNil()) throw runtime.newTypeError("type mismatch: " + sub.getMetaClass().getName() + " given");
            pos = strRindex((RubyString) tmp, pos);
        }
        if (pos >= 0) return RubyFixnum.newFixnum(runtime, pos);
        return runtime.getNil();
    }

    private int strRindex(RubyString sub, int pos) {
        int subLength = sub.value.getRealSize();
        
        /* substring longer than string */
        if (value.getRealSize() < subLength) return -1;
        if (value.getRealSize() - pos < subLength) pos = value.getRealSize() - subLength;

        return value.lastIndexOf(sub.value, pos);
    }

    @JRubyMethod(name = "rindex", reads = BACKREF, writes = BACKREF, compat = RUBY1_9)
    public IRubyObject rindex19(ThreadContext context, IRubyObject arg0) {
        return rindexCommon19(context.getRuntime(), context, arg0, strLength());
    }

    @JRubyMethod(name = "rindex", reads = BACKREF, writes = BACKREF, compat = RUBY1_9)
    public IRubyObject rindex19(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        int pos = RubyNumeric.num2int(arg1);
        Ruby runtime = context.getRuntime();
        int length = strLength();
        if (pos < 0) {
            pos += length;
            if (pos < 0) {
                if (arg0 instanceof RubyRegexp) context.getCurrentScope().setBackRef(runtime.getNil());
                return runtime.getNil();
            }
        }            
        if (pos > length) pos = length;
        return rindexCommon19(runtime, context, arg0, pos);
    }

    private IRubyObject rindexCommon19(Ruby runtime, ThreadContext context, final IRubyObject sub, int pos) {
        if (sub instanceof RubyRegexp) {
            RubyRegexp regSub = (RubyRegexp) sub;
            pos = singleByteOptimizable() ? pos :
                    StringSupport.nth(value.getEncoding(), value.getUnsafeBytes(), value.getBegin(),
                            value.getBegin() + value.getRealSize(),
                            pos);
            if (regSub.length() > 0) {
                pos = regSub.adjustStartPos19(this, pos, true);
                pos = regSub.search19(context, this, pos, true);
                pos = subLength(pos);
            }
        } else if (sub instanceof RubyString) {
            pos = strRindex19((RubyString) sub, pos);
        } else {
            IRubyObject tmp = sub.checkStringType();
            if (tmp.isNil()) throw runtime.newTypeError("type mismatch: " + sub.getMetaClass().getName() + " given");
            pos = strRindex19((RubyString) tmp, pos);
        }
        if (pos >= 0) return RubyFixnum.newFixnum(runtime, pos);
        return runtime.getNil();
    }

    private int strRindex19(RubyString sub, int pos) {
        Encoding enc = checkEncoding(sub);
        if (sub.scanForCodeRange() == CR_BROKEN) return -1;
        int len = strLength(enc);
        int slen = sub.strLength(enc);

        if (len < slen) return -1;
        if (len - pos < slen) pos = len - slen;
        if (len == 0) return pos;

        byte[]bytes = value.getUnsafeBytes();
        int p = value.getBegin();
        int end = p + value.getRealSize();

        byte[]sbytes = sub.value.getUnsafeBytes();
        int sp = sub.value.getBegin();
        slen = sub.value.getRealSize();

        boolean singlebyte = singleByteOptimizable();
        while (true) {
            int s = singlebyte ? p + pos : StringSupport.nth(enc, bytes, p, end, pos);
            if (s == -1) return -1;
            if (ByteList.memcmp(bytes, s, sbytes, sp, slen) == 0) return pos;
            if (pos == 0) return -1;
            pos--;
        }
    }

    @Deprecated
    public final IRubyObject substr(int beg, int len) {
        return substr(getRuntime(), beg, len);
    }

    /* rb_str_substr */
    public final IRubyObject substr(Ruby runtime, int beg, int len) {    
        int length = value.length();
        if (len < 0 || beg > length) return runtime.getNil();

        if (beg < 0) {
            beg += length;
            if (beg < 0) return runtime.getNil();
        }

        int end = Math.min(length, beg + len);
        return makeShared(runtime, beg, end - beg);
    }

    /* str_byte_substr */
    private IRubyObject byteSubstr(Ruby runtime, int beg, int len) {
        int length = value.length();
        int s = value.getBegin();
        
        if (len < 0 || beg > length) return runtime.getNil();

        int p;
        if (beg < 0) {
            beg += length;
            if (beg < 0) return runtime.getNil();
        }
        if (beg + len > length) len = length - beg;
            
        if (len <= 0) {
            len = 0;
            p = 0;
        }
        else {
            p = s + beg;
        }

        return makeShared19(runtime, p, len);
    }

    /* str_byte_aref */
    private IRubyObject byteARef(Ruby runtime, IRubyObject idx) {
        final int index;

        if (idx instanceof RubyRange){
            int[] begLen = ((RubyRange) idx).begLenInt(getByteList().length(), 0);
            return begLen == null ? runtime.getNil() : byteSubstr(runtime, begLen[0], begLen[1]);
        } else if (idx instanceof RubyFixnum) {
            index = RubyNumeric.fix2int((RubyFixnum)idx);
        } else {
            index = RubyNumeric.num2int(idx);
        }

        IRubyObject obj = byteSubstr(runtime, index, 1);
        if (obj.isNil() || ((RubyString)obj).getByteList().length() == 0) return runtime.getNil();
        return obj;
    }
            
    public final IRubyObject substr19(Ruby runtime, int beg, int len) {
        if (len < 0) return runtime.getNil();
        int length = value.getRealSize();
        if (length == 0) len = 0; 

        Encoding enc = value.getEncoding();
        if (singleByteOptimizable(enc)) {
            if (beg > length) return runtime.getNil();
            if (beg < 0) {
                beg += length;
                if (beg < 0) return runtime.getNil();
            }
            if (beg + len > length) len = length - beg;
            if (len <= 0) len = beg = 0;
            return makeShared19(runtime, beg, len);
        } else {
            return multibyteSubstr19(runtime, enc, len, beg, length);
        }
    }

    private IRubyObject multibyteSubstr19(Ruby runtime, Encoding enc, int len, int beg, int length) {
        int p;
        int s = value.getBegin();
        int end = s + length;
        byte[]bytes = value.getUnsafeBytes();

        if (beg < 0) {
            if (len > -beg) len = -beg;
            if (-beg * enc.maxLength() < length >>> 3) {
                beg = -beg;
                int e = end;
                while (beg-- > len && (e = enc.prevCharHead(bytes, s, e, e)) != -1) {} // nothing
                p = e;
                if (p == -1) return runtime.getNil();
                while (len-- > 0 && (p = enc.prevCharHead(bytes, s, p, e)) != -1) {} // nothing
                if (p == -1) return runtime.getNil();
                return makeShared19(runtime, p - s, e - p);
            } else {
                beg += strLength(enc);
                if (beg < 0) return runtime.getNil();
            }
        } else if (beg > 0 && beg > strLength(enc)) {
            return runtime.getNil();
        }
        if (len == 0) {
            p = 0;
        } else if (isCodeRangeValid() && enc instanceof UTF8Encoding) {
            p = StringSupport.utf8Nth(bytes, s, end, beg);
            len = StringSupport.utf8Offset(bytes, p, end, len);
        } else if (enc.isFixedWidth()) {
            int w = enc.maxLength();
            p = s + beg * w;
            if (p > end) {
                p = end;
                len = 0;
            } else if (len * w > end - p) {
                len = end - p;
            } else {
                len *= w;
            }
        } else if ((p = StringSupport.nth(enc, bytes, s, end, beg)) == end) {
            len = 0;
        } else {
            len = StringSupport.offset(enc, bytes, p, end, len); 
        }
        return makeShared19(runtime, p - s, len);
    }

    /* rb_str_splice */
    private IRubyObject replaceInternal(int beg, int len, RubyString repl) {
        int oldLength = value.getRealSize();
        if (beg + len >= oldLength) len = oldLength - beg;
        ByteList replBytes = repl.value;
        int replLength = replBytes.getRealSize();
        int newLength = oldLength + replLength - len;

        byte[]oldBytes = value.getUnsafeBytes();
        int oldBegin = value.getBegin();

        modify(newLength);
        if (replLength != len) {
            System.arraycopy(oldBytes, oldBegin + beg + len, value.getUnsafeBytes(), beg + replLength, oldLength - (beg + len));
        }

        if (replLength > 0) System.arraycopy(replBytes.getUnsafeBytes(), replBytes.getBegin(), value.getUnsafeBytes(), beg, replLength);
        value.setRealSize(newLength);
        return infectBy(repl);
    }

    private void replaceInternal19(int beg, int len, RubyString repl) {
        Encoding enc = checkEncoding(repl);
        int p = value.getBegin();
        int e;
        if (singleByteOptimizable()) {
            p += beg;
            e = p + len;
        } else {
            int end = p + value.getRealSize();
            byte[]bytes = value.getUnsafeBytes();
            p = StringSupport.nth(enc, bytes, p, end, beg);
            if (p == -1) p = end;
            e = StringSupport.nth(enc, bytes, p, end, len);
            if (e == -1) e = end;
        }

        int cr = getCodeRange();
        if (cr == CR_BROKEN) clearCodeRange();
        replaceInternal(p - value.getBegin(), e - p, repl);
        associateEncoding(enc);
        cr = codeRangeAnd(cr, repl.getCodeRange());
        if (cr != CR_BROKEN) setCodeRange(cr);
    }

    /** rb_str_aref, rb_str_aref_m
     *
     */
    @JRubyMethod(name = {"[]", "slice"}, reads = BACKREF, writes = BACKREF, compat = RUBY1_8)
    public IRubyObject op_aref(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        Ruby runtime = context.getRuntime();
        if (arg1 instanceof RubyRegexp) return subpat(runtime, context, (RubyRegexp)arg1, RubyNumeric.num2int(arg2));
        return substr(runtime, RubyNumeric.num2int(arg1), RubyNumeric.num2int(arg2));
    }

    @JRubyMethod(name = {"[]", "slice"}, reads = BACKREF, writes = BACKREF, compat = RUBY1_8)
    public IRubyObject op_aref(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.getRuntime();
        if (arg instanceof RubyFixnum) {
            return op_aref(runtime, RubyFixnum.fix2int((RubyFixnum)arg));
        } if (arg instanceof RubyRegexp) {
            return subpat(runtime, context, (RubyRegexp)arg, 0);
        } else if (arg instanceof RubyString) {
            RubyString str = (RubyString)arg;
            return value.indexOf(str.value) != -1 ? str.strDup(runtime) : runtime.getNil();
        } else if (arg instanceof RubyRange) {
            int[] begLen = ((RubyRange) arg).begLenInt(value.length(), 0);
            return begLen == null ? runtime.getNil() : substr(runtime, begLen[0], begLen[1]);
        }
        return op_aref(runtime, RubyFixnum.num2int(arg));
    }

    private IRubyObject op_aref(Ruby runtime, int idx) {
        if (idx < 0) idx += value.getRealSize();
        return idx < 0 || idx >= value.getRealSize() ? runtime.getNil() : runtime.newFixnum(value.get(idx) & 0xff);
    }

    @JRubyMethod(name = {"[]", "slice"}, reads = BACKREF, writes = BACKREF, compat = RUBY1_9)
    public IRubyObject op_aref19(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        Ruby runtime = context.getRuntime();
        if (arg1 instanceof RubyRegexp) return subpat19(runtime, context, (RubyRegexp)arg1, arg2);
        return substr19(runtime, RubyNumeric.num2int(arg1), RubyNumeric.num2int(arg2));
    }

    @JRubyMethod(name = {"[]", "slice"}, reads = BACKREF, writes = BACKREF, compat = RUBY1_9)
    public IRubyObject op_aref19(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.getRuntime();
        if (arg instanceof RubyFixnum) {
            return op_aref19(runtime, RubyNumeric.fix2int((RubyFixnum)arg));
        } else if (arg instanceof RubyRegexp) {
            return subpat19(runtime, context, (RubyRegexp)arg);
        } else if (arg instanceof RubyString) {
            RubyString str = (RubyString)arg;
            return strIndex19(str, 0) != -1 ? str.strDup(runtime) : runtime.getNil();
        } else if (arg instanceof RubyRange) {
            int len = strLength();
            int[] begLen = ((RubyRange) arg).begLenInt(len, 0);
            return begLen == null ? runtime.getNil() : substr19(runtime, begLen[0], begLen[1]);
        }
        return op_aref19(runtime, RubyNumeric.num2int(arg));
    }

    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject byteslice(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        return byteSubstr(context.runtime, RubyNumeric.num2int(arg1), RubyNumeric.num2int(arg2));
    }

    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject byteslice(ThreadContext context, IRubyObject arg) {
        return byteARef(context.runtime, arg);
    }

    private IRubyObject op_aref19(Ruby runtime, int idx) {
        IRubyObject str = substr19(runtime, idx, 1);
        return !str.isNil() && ((RubyString) str).value.getRealSize() == 0 ? runtime.getNil() : str;
    }

    /**
     * rb_str_subpat_set
     *
     */
    private void subpatSet(ThreadContext context, RubyRegexp regexp, int nth, IRubyObject repl) {
        Ruby runtime = context.getRuntime();
        if (regexp.search(context, this, 0, false) < 0) throw runtime.newIndexError("regexp not matched");
        RubyMatchData match = (RubyMatchData)context.getCurrentScope().getBackRef(runtime);

        nth = subpatSetCheck(runtime, nth, match.regs);

        final int start, end;
        if (match.regs == null) {
            start = match.begin;
            end = match.end;
        } else {
            start = match.regs.beg[nth];
            end = match.regs.end[nth];
        }
        if (start == -1) throw runtime.newIndexError("regexp group " + nth + " not matched");
        replaceInternal(start, end - start, repl.convertToString());
    }

    private int subpatSetCheck(Ruby runtime, int nth, Region regs) {
        int numRegs = regs == null ? 1 : regs.numRegs;
        if (nth < numRegs) {
            if (nth < 0) {
                if (-nth < numRegs) return nth + numRegs;
            } else {
                return nth;
            }
        }
        throw runtime.newIndexError("index " + nth + " out of regexp");
    }

    private IRubyObject subpat(Ruby runtime, ThreadContext context, RubyRegexp regex, int nth) {
        if (regex.search(context, this, 0, false) >= 0) {
            return RubyRegexp.nth_match(nth, context.getCurrentScope().getBackRef(runtime));
        }
        return runtime.getNil();
    }

    private void subpatSet19(ThreadContext context, RubyRegexp regexp, IRubyObject backref, IRubyObject repl) {
        Ruby runtime = context.getRuntime();
        if (regexp.search19(context, this, 0, false) < 0) throw runtime.newIndexError("regexp not matched");
        RubyMatchData match = (RubyMatchData)context.getCurrentScope().getBackRef(runtime);

        int nth = backref == null ? 0 : subpatSetCheck(runtime, match.backrefNumber(backref), match.regs);

        final int start, end;
        if (match.regs == null) {
            start = match.begin;
            end = match.end;
        } else {
            start = match.regs.beg[nth];
            end = match.regs.end[nth];
        }
        if (start == -1) throw runtime.newIndexError("regexp group " + nth + " not matched");
        RubyString replStr =  repl.convertToString();
        Encoding enc = checkEncoding(replStr);
        // TODO: keep cr
        replaceInternal(start, end - start, replStr); // TODO: rb_str_splice_0
        associateEncoding(enc);
    }

    private IRubyObject subpat19(Ruby runtime, ThreadContext context, RubyRegexp regex, IRubyObject backref) {
        if (regex.search19(context, this, 0, false) >= 0) {
            RubyMatchData match = (RubyMatchData)context.getCurrentScope().getBackRef(runtime);
            return RubyRegexp.nth_match(match.backrefNumber(backref), context.getCurrentScope().getBackRef(runtime));
        }
        return runtime.getNil();
    }

    private IRubyObject subpat19(Ruby runtime, ThreadContext context, RubyRegexp regex) {
        if (regex.search19(context, this, 0, false) >= 0) {
            return RubyRegexp.nth_match(0, context.getCurrentScope().getBackRef(runtime));
        }
        return runtime.getNil();
    }

    /** rb_str_aset, rb_str_aset_m
     *
     */
    @JRubyMethod(name = "[]=", reads = BACKREF, compat = RUBY1_8)
    public IRubyObject op_aset(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        if (arg0 instanceof RubyFixnum) {
            return op_aset(context, RubyNumeric.fix2int((RubyFixnum)arg0), arg1);
        } else if (arg0 instanceof RubyRegexp) {
            subpatSet(context, (RubyRegexp)arg0, 0, arg1.convertToString());
            return arg1;
        } else if (arg0 instanceof RubyString) {
            RubyString orig = (RubyString)arg0;
            int beg = value.indexOf(orig.value);
            if (beg < 0) throw context.getRuntime().newIndexError("string not matched");
            replaceInternal(beg, orig.value.getRealSize(), arg1.convertToString());
            return arg1;
        } else if (arg0 instanceof RubyRange) {
            int[] begLen = ((RubyRange) arg0).begLenInt(value.getRealSize(), 2);
            replaceInternal(begLen[0], begLen[1], arg1.convertToString());
            return arg1;
        }
        return op_aset(context, RubyNumeric.num2int(arg0), arg1);
    }

    private IRubyObject op_aset(ThreadContext context, int idx, IRubyObject arg1) {
        idx = checkIndexForRef(idx, value.getRealSize());
        if (arg1 instanceof RubyFixnum) {
            modify();
            value.set(idx, RubyNumeric.fix2int((RubyFixnum)arg1));
        } else {
            replaceInternal(idx, 1, arg1.convertToString());
        }
        return arg1;
    }

    @JRubyMethod(name = "[]=", reads = BACKREF, compat = RUBY1_8)
    public IRubyObject op_aset(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        if (arg0 instanceof RubyRegexp) {
            subpatSet(context, (RubyRegexp)arg0, RubyNumeric.num2int(arg1), arg2);
        } else {
            int beg = RubyNumeric.num2int(arg0);
            int len = RubyNumeric.num2int(arg1);
            checkLength(len);
            RubyString repl = arg2.convertToString();
            replaceInternal(checkIndex(beg, value.getRealSize()), len, repl);
        }
        return arg2;
    }

    @JRubyMethod(name = "[]=", reads = BACKREF, compat = RUBY1_9)
    public IRubyObject op_aset19(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        if (arg0 instanceof RubyFixnum) {
            return op_aset19(context, RubyNumeric.fix2int((RubyFixnum)arg0), arg1);
        } else if (arg0 instanceof RubyRegexp) {
            subpatSet19(context, (RubyRegexp)arg0, null, arg1);
            return arg1;
        } else if (arg0 instanceof RubyString) {
            RubyString orig = (RubyString)arg0;
            int beg = strIndex19(orig, 0);
            if (beg < 0) throw context.getRuntime().newIndexError("string not matched");
            beg = subLength(beg);
            replaceInternal19(beg, orig.strLength(), arg1.convertToString());
            return arg1;
        } else if (arg0 instanceof RubyRange) {
            int[] begLen = ((RubyRange) arg0).begLenInt(strLength(), 2);
            replaceInternal19(begLen[0], begLen[1], arg1.convertToString());
            return arg1;
        }
        return op_aset19(context, RubyNumeric.num2int(arg0), arg1);
    }

    private IRubyObject op_aset19(ThreadContext context, int idx, IRubyObject arg1) {
        replaceInternal19(checkIndex(idx, strLength()), 1, arg1.convertToString());
        return arg1;
    }

    @JRubyMethod(name = "[]=", reads = BACKREF, compat = RUBY1_9)
    public IRubyObject op_aset19(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        if (arg0 instanceof RubyRegexp) {
            subpatSet19(context, (RubyRegexp)arg0, arg1, arg2);
        } else {
            int beg = RubyNumeric.num2int(arg0);
            int len = RubyNumeric.num2int(arg1);
            checkLength(len);
            RubyString repl = arg2.convertToString();
            replaceInternal19(checkIndex(beg, strLength()), len, repl);
        }
        return arg2;
    }

    private boolean isHeadSlice(int beg, int len) {
        return beg == 0 && len > 0 && len <= value.getRealSize();
    }

    private boolean isTailSlice(int beg, int len) {
        return beg >= 0 && len > 0 && (beg + len) == value.getRealSize();
    }

    
    /**
     * Excises (removes) a slice of the string that starts at index zero
     *
     * @param len The number of bytes to remove.
     */
    private void exciseHead(int len) {
        // just adjust the view start
        view(len, value.getRealSize() - len);
    }

    /**
     * Excises (removes) a slice of the string that ends at the last byte in the string
     *
     * @param len The number of bytes to remove.
     */
    private void exciseTail(int len) {
        // just adjust the view length
        view(0, value.getRealSize() - len);
    }
    
    /** rb_str_slice_bang
     *
     */
    @JRubyMethod(name = "slice!", reads = BACKREF, writes = BACKREF, compat = RUBY1_8)
    public IRubyObject slice_bang(ThreadContext context, IRubyObject arg0) {
        IRubyObject result = op_aref(context, arg0);
        if (!result.isNil()) {
            // Optimize slice!(0), slice!(0..len), and slice!(pos..-1)
            int beg = -1;
            int len = 1;
            if (arg0 instanceof RubyFixnum) {
                 beg = RubyNumeric.num2int(arg0);

            } else if (arg0 instanceof RubyRange) {
                int[] begLen = ((RubyRange) arg0).begLenInt(value.getRealSize(), 2);
                beg = begLen[0];
                len = begLen[1];
            }
            
            if (isHeadSlice(beg, len)) {
                exciseHead(len);

            } else if (isTailSlice(beg, len)) {
                exciseTail(len);

            } else {
                op_aset(context, arg0, RubyString.newEmptyString(context.getRuntime()));
            }
        }
        return result;
    }

    @JRubyMethod(name = "slice!", reads = BACKREF, writes = BACKREF, compat = RUBY1_8)
    public IRubyObject slice_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        IRubyObject result = op_aref(context, arg0, arg1);
        if (!result.isNil()) {
            // Optimize slice!(0, len) and slice!(str.length - len, len)
            int beg = -1;
            int len = 0;
            if (arg0 instanceof RubyFixnum && arg1 instanceof RubyFixnum) {
                 beg = RubyNumeric.num2int(arg0);
                 len = RubyNumeric.num2int(arg1);
            }

            if (isHeadSlice(beg, len)) {
                exciseHead(len);

            } else if (isTailSlice(beg, len)) {
                exciseTail(len);

            } else {
                op_aset(context, arg0, arg1, RubyString.newEmptyString(context.getRuntime()));
            }
        }
        return result;
    }

    @JRubyMethod(name = "slice!", reads = BACKREF, writes = BACKREF, compat = RUBY1_9)
    public IRubyObject slice_bang19(ThreadContext context, IRubyObject arg0) {
        IRubyObject result = op_aref19(context, arg0);
        if (result.isNil()) {
            modifyCheck(); // keep cr ?
        } else {
            op_aset19(context, arg0, RubyString.newEmptyString(context.getRuntime()));
        }
        return result;
    }

    @JRubyMethod(name = "slice!", reads = BACKREF, writes = BACKREF, compat = RUBY1_9)
    public IRubyObject slice_bang19(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        IRubyObject result = op_aref19(context, arg0, arg1);
        if (result.isNil()) {
            modifyCheck(); // keep cr ?
        } else {
            op_aset19(context, arg0, arg1, RubyString.newEmptyString(context.getRuntime()));
        }
        return result;
    }

    @JRubyMethod(name = {"succ", "next"}, compat = RUBY1_8)
    public IRubyObject succ(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.succ_bang();
        return str;
    }

    @JRubyMethod(name = {"succ!", "next!"}, compat = RUBY1_8)
    public IRubyObject succ_bang() {
        if (value.getRealSize() == 0) {
            modifyCheck();
            return this;
        }

        modify();

        boolean alnumSeen = false;
        int pos = -1, n = 0;
        int p = value.getBegin();
        int end = p + value.getRealSize();
        byte[]bytes = value.getUnsafeBytes();
        
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

    private static enum NeighborChar {NOT_CHAR, FOUND, WRAPPED}

    private static NeighborChar succChar(Encoding enc, byte[]bytes, int p, int len) {
        while (true) {
            int i = len - 1;
            for (; i >= 0 && bytes[p + i] == (byte)0xff; i--) bytes[p + i] = 0;
            if (i < 0) return NeighborChar.WRAPPED;
            bytes[p + i] = (byte)((bytes[p + i] & 0xff) + 1);
            int cl = StringSupport.preciseLength(enc, bytes, p, p + len);
            if (cl > 0) {
                if (cl == len) {
                    return NeighborChar.FOUND;
                } else {
                    for (int j = p + cl; j < p + len - cl; j++) bytes[j] = (byte)0xff;
                }
            }
            if (cl == -1 && i < len - 1) {
                int len2 = len - 1;
                for (; len2 > 0; len2--) {
                    if (StringSupport.preciseLength(enc, bytes, p, p + len2) != -1) break;
                }
                for (int j = p + len2 + 1; j < p + len - (len2 + 1); j++) bytes[j] = (byte)0xff;
            }
        }
    }
    
    private static NeighborChar predChar(Encoding enc, byte[]bytes, int p, int len) {
        while (true) {
            int i = len - 1;
            for (; i >= 0 && bytes[p + i] == 0; i--) bytes[p + i] = (byte)0xff;
            if (i < 0) return NeighborChar.WRAPPED;
            bytes[p + i] = (byte)((bytes[p + i] & 0xff) - 1);
            int cl = StringSupport.preciseLength(enc, bytes, p, p + len);
            if (cl > 0) {
                if (cl == len) {
                    return NeighborChar.FOUND;
                } else {
                    for (int j = p + cl; j < p + len - cl; j++) bytes[j] = 0;
                }
            }
            if (cl == -1 && i < len - 1) {
                int len2 = len - 1;
                for (; len2 > 0; len2--) {
                    if (StringSupport.preciseLength(enc, bytes, p, p + len2) != -1) break;
                }
                for (int j = p + len2 + 1; j < p + len - (len2 + 1); j++) bytes[j] = 0;
            }
        }
    }

    private static NeighborChar succAlnumChar(Encoding enc, byte[]bytes, int p, int len, byte[]carry, int carryP) {
        byte save[] = new byte[org.jcodings.Config.ENC_CODE_TO_MBC_MAXLEN];
        int c = enc.mbcToCode(bytes, p, p + len);

        final int cType;
        if (enc.isDigit(c)) {
            cType = CharacterType.DIGIT;
        } else if (enc.isAlpha(c)) {
            cType = CharacterType.ALPHA;
        } else {
            return NeighborChar.NOT_CHAR;
        }

        System.arraycopy(bytes, p, save, 0, len);
        NeighborChar ret = succChar(enc, bytes, p, len);
        if (ret == NeighborChar.FOUND) {
            c = enc.mbcToCode(bytes, p, p + len);
            if (enc.isCodeCType(c, cType)) return NeighborChar.FOUND;
        }

        System.arraycopy(save, 0, bytes, p, len);
        int range = 1;

        while (true) {
            System.arraycopy(bytes, p, save, 0, len);
            ret = predChar(enc, bytes, p, len);
            if (ret == NeighborChar.FOUND) {
                c = enc.mbcToCode(bytes, p, p + len);
                if (!enc.isCodeCType(c, cType)) {
                    System.arraycopy(save, 0, bytes, p, len);
                    break;
                }
            } else {
                System.arraycopy(save, 0, bytes, p, len);
                break;
            }
            range++;
        }

        if (range == 1) return NeighborChar.NOT_CHAR;

        if (cType != CharacterType.DIGIT) {
            System.arraycopy(bytes, p, carry, carryP, len);
            return NeighborChar.WRAPPED;
        }

        System.arraycopy(bytes, p, carry, carryP, len);
        succChar(enc, carry, carryP, len);
        return NeighborChar.WRAPPED;
    }

    @JRubyMethod(name = {"succ", "next"}, compat = RUBY1_9)
    public IRubyObject succ19(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        final RubyString str;
        if (value.getRealSize() > 0) {
            str = new RubyString(runtime, getMetaClass(), succCommon19(value));
            // TODO: rescan code range ?
        } else {
            str = newEmptyString(runtime, getType(), value.getEncoding());
        }
        return str.infectBy(this);
    }

    @JRubyMethod(name = {"succ!", "next!"}, compat = RUBY1_9)
    public IRubyObject succ_bang19() {
        modifyCheck();
        if (value.getRealSize() > 0) {
            value = succCommon19(value);
            shareLevel = SHARE_LEVEL_NONE;
            // TODO: rescan code range ?
        }
        return this;
    }

    private ByteList succCommon19(ByteList original) {
        byte carry[] = new byte[org.jcodings.Config.ENC_CODE_TO_MBC_MAXLEN];
        int carryP = 0;
        carry[0] = 1;
        int carryLen = 1;

        ByteList valueCopy = new ByteList(original);
        valueCopy.setEncoding(original.getEncoding());
        Encoding enc = original.getEncoding();
        int p = valueCopy.getBegin();
        int end = p + valueCopy.getRealSize();
        int s = end;
        byte[]bytes = valueCopy.getUnsafeBytes();

        NeighborChar neighbor = NeighborChar.FOUND;
        int lastAlnum = -1;
        boolean alnumSeen = false;
        while ((s = enc.prevCharHead(bytes, p, s, end)) != -1) {
            if (neighbor == NeighborChar.NOT_CHAR && lastAlnum != -1) {
                if (ASCII.isAlpha(bytes[lastAlnum] & 0xff) ?
                        ASCII.isDigit(bytes[s] & 0xff) :
                        ASCII.isDigit(bytes[lastAlnum] & 0xff) ?
                                ASCII.isAlpha(bytes[s] & 0xff) : false) {
                    s = lastAlnum;
                    break;
                }
            }

            int cl = StringSupport.preciseLength(enc, bytes, s, end);
            if (cl <= 0) continue;
            switch (neighbor = succAlnumChar(enc, bytes, s, cl, carry, 0)) {
            case NOT_CHAR: continue;
            case FOUND:    return valueCopy;
            case WRAPPED:  lastAlnum = s;
            }
            alnumSeen = true;
            carryP = s - p;
            carryLen = cl;
        }

        if (!alnumSeen) {
            s = end;
            while ((s = enc.prevCharHead(bytes, p, s, end)) != -1) {
                int cl = StringSupport.preciseLength(enc, bytes, s, end);
                if (cl <= 0) continue;
                neighbor = succChar(enc, bytes, s, cl);
                if (neighbor == NeighborChar.FOUND) return valueCopy;
                if (StringSupport.preciseLength(enc, bytes, s, s + 1) != cl) succChar(enc, bytes, s, cl); /* wrapped to \0...\0.  search next valid char. */
                if (!enc.isAsciiCompatible()) {
                    System.arraycopy(bytes, s, carry, 0, cl);
                    carryLen = cl;
                }
                carryP = s - p;
            }
        }
        valueCopy.ensure(valueCopy.getBegin() + valueCopy.getRealSize() + carryLen);
        s = valueCopy.getBegin() + carryP;
        System.arraycopy(valueCopy.getUnsafeBytes(), s, valueCopy.getUnsafeBytes(), s + carryLen, valueCopy.getRealSize() - carryP);
        System.arraycopy(carry, 0, valueCopy.getUnsafeBytes(), s, carryLen);
        valueCopy.setRealSize(valueCopy.getRealSize() + carryLen);
        return valueCopy;
    }

    /** rb_str_upto_m
     *
     */

    @JRubyMethod(name = "upto", compat = RUBY1_8)
    public IRubyObject upto18(ThreadContext context, IRubyObject end, Block block) {
        return uptoCommon18(context, end, false, block);
    }

    @JRubyMethod(name = "upto", compat = RUBY1_8)
    public IRubyObject upto18(ThreadContext context, IRubyObject end, IRubyObject excl, Block block) {
        return uptoCommon18(context, end, excl.isTrue(), block);
    }

    final IRubyObject uptoCommon18(ThreadContext context, IRubyObject arg, boolean excl, Block block) {
        RubyString end = arg.convertToString();
        checkEncoding(end);

        int n = op_cmp19(end);
        if (n > 0 || (excl && n == 0)) return this;

        IRubyObject afterEnd = end.callMethod(context, "succ");
        RubyString current = this;
        while (!current.op_equal19(context, afterEnd).isTrue()) {
            block.yield(context, current);
            if (!excl && current.op_equal19(context, end).isTrue()) break;
            current = current.callMethod(context, "succ").convertToString();
            if (excl && current.op_equal19(context, end).isTrue()) break;
            if (current.value.getRealSize() > end.value.getRealSize() || current.value.getRealSize() == 0) break;
        }

        return this;
    }

    @JRubyMethod(name = "upto", compat = RUBY1_9)
    public IRubyObject upto19(ThreadContext context, IRubyObject end, Block block) {
        Ruby runtime = context.getRuntime();
        return block.isGiven() ? uptoCommon19(context, end, false, block) : enumeratorize(runtime, this, "upto", end);
    }

    @JRubyMethod(name = "upto", compat = RUBY1_9)
    public IRubyObject upto19(ThreadContext context, IRubyObject end, IRubyObject excl, Block block) {
        return block.isGiven() ? uptoCommon19(context, end, excl.isTrue(), block) : 
            enumeratorize(context.getRuntime(), this, "upto", new IRubyObject[]{end, excl});
    }

    final IRubyObject uptoCommon19(ThreadContext context, IRubyObject arg, boolean excl, Block block) {
        return uptoCommon19(context, arg, excl, block, false);
    }

    final IRubyObject uptoCommon19(ThreadContext context, IRubyObject arg, boolean excl, Block block, boolean asSymbol) {
        Ruby runtime = context.getRuntime();
        if (arg instanceof RubySymbol) throw runtime.newTypeError("can't convert Symbol into String");

        RubyString end = arg.convertToString();
        Encoding enc = checkEncoding(end);
        boolean isAscii = scanForCodeRange() == CR_7BIT && end.scanForCodeRange() == CR_7BIT;
        if (value.getRealSize() == 1 && end.value.getRealSize() == 1 && isAscii) {
            byte c = value.getUnsafeBytes()[value.getBegin()];
            byte e = end.value.getUnsafeBytes()[end.value.getBegin()];
            if (c > e || (excl && c == e)) return this;
            while (true) {
                RubyString s = new RubyString(runtime, runtime.getString(), RubyInteger.SINGLE_CHAR_BYTELISTS[c & 0xff],
                                                                            enc, CR_7BIT);
                s.shareLevel = SHARE_LEVEL_BYTELIST;
                block.yield(context, asSymbol ? runtime.newSymbol(s.toString()) : s);

                if (!excl && c == e) break;
                c++;
                if (excl && c == e) break;
            }
            return this;
        } else if (isAscii && ASCII.isDigit(value.getUnsafeBytes()[value.getBegin()]) && ASCII.isDigit(end.value.getUnsafeBytes()[end.value.getBegin()])) {
            int s = value.getBegin();
            int send = s + value.getRealSize();
            byte[]bytes = value.getUnsafeBytes();

            while (s < send) {
                if (!ASCII.isDigit(bytes[s] & 0xff)) return uptoCommon19NoDigits(context, end, excl, block, asSymbol);
                s++;
            }
            s = end.value.getBegin();
            send = s + end.value.getRealSize();
            bytes = end.value.getUnsafeBytes();

            while (s < send) {
                if (!ASCII.isDigit(bytes[s] & 0xff)) return uptoCommon19NoDigits(context, end, excl, block, asSymbol);
                s++;
            }

            IRubyObject b = stringToInum19(10, false);
            IRubyObject e = end.stringToInum19(10, false);

            IRubyObject[]args = new IRubyObject[2];
            args[0] = RubyFixnum.newFixnum(runtime, value.length());
            RubyArray argsArr = runtime.newArrayNoCopy(args);
            
            if (b instanceof RubyFixnum && e instanceof RubyFixnum) {
                int bi = RubyNumeric.fix2int(b);
                int ei = RubyNumeric.fix2int(e);

                while (bi <= ei) {
                    if (excl && bi == ei) break;
                    args[1] = RubyFixnum.newFixnum(runtime, bi);
                    ByteList to = new ByteList(value.length() + 5);
                    Sprintf.sprintf(to, "%.*d", argsArr);
                    RubyString str = RubyString.newStringNoCopy(runtime, to, USASCIIEncoding.INSTANCE, CR_7BIT);
                    block.yield(context, asSymbol ? runtime.newSymbol(str.toString()) : str);
                    bi++;
                }
            } else {
                String op = excl ? "<" : "<=";

                while (b.callMethod(context, op, e).isTrue()) {
                    args[1] = b;
                    ByteList to = new ByteList(value.length() + 5);
                    Sprintf.sprintf(to, "%.*d", argsArr);
                    RubyString str = RubyString.newStringNoCopy(runtime, to, USASCIIEncoding.INSTANCE, CR_7BIT);
                    block.yield(context, asSymbol ? runtime.newSymbol(str.toString()) : str);
                    b = b.callMethod(context, "succ");
                }
            }
            return this;
        }

        return uptoCommon19NoDigits(context, end, excl, block, asSymbol);
    }

    private IRubyObject uptoCommon19NoDigits(ThreadContext context, RubyString end, boolean excl, Block block, boolean asSymbol) {
        Ruby runtime = context.runtime;
        int n = op_cmp19(end);
        if (n > 0 || (excl && n == 0)) return this;
        IRubyObject afterEnd = end.callMethod(context, "succ");
        RubyString current = strDup(context.runtime);

        while (!current.op_equal19(context, afterEnd).isTrue()) {
            IRubyObject next = null;
            if (excl || !current.op_equal19(context, end).isTrue()) next = current.callMethod(context, "succ");
            block.yield(context, asSymbol ? runtime.newSymbol(current.toString()) : current);
            if (next == null) break;
            current = next.convertToString();
            if (excl && current.op_equal19(context, end).isTrue()) break;
            if (current.getByteList().length() > end.getByteList().length() || current.getByteList().length() == 0) break;
        }
        return this;
    }

    /** rb_str_include
     *
     */
    @JRubyMethod(name = "include?", compat = RUBY1_8)
    public RubyBoolean include_p(ThreadContext context, IRubyObject obj) {
        Ruby runtime = context.getRuntime();
        if (obj instanceof RubyFixnum) {
            int c = RubyNumeric.fix2int((RubyFixnum)obj);
            for (int i = 0; i < value.getRealSize(); i++) {
                if (value.get(i) == (byte)c) return runtime.getTrue();
            }
            return runtime.getFalse();
        }
        return value.indexOf(obj.convertToString().value) == -1 ? runtime.getFalse() : runtime.getTrue();
    }

    @JRubyMethod(name = "include?", compat = RUBY1_9)
    public RubyBoolean include_p19(ThreadContext context, IRubyObject obj) {
        Ruby runtime = context.getRuntime();
        return strIndex19(obj.convertToString(), 0) == -1 ? runtime.getFalse() : runtime.getTrue();
    }

    @JRubyMethod(name = "chr", compat = RUBY1_9)
    public IRubyObject chr(ThreadContext context) {
        return substr19(context.getRuntime(), 0, 1);
    }

    @JRubyMethod(name = "getbyte", compat = RUBY1_9)
    public IRubyObject getbyte(ThreadContext context, IRubyObject index) {
        Ruby runtime = context.getRuntime();
        int i = RubyNumeric.num2int(index);
        if (i < 0) i += value.getRealSize();
        if (i < 0 || i >= value.getRealSize()) return runtime.getNil();
        return RubyFixnum.newFixnum(runtime, value.getUnsafeBytes()[value.getBegin() + i] & 0xff);
    }

    @JRubyMethod(name = "setbyte", compat = RUBY1_9)
    public IRubyObject setbyte(ThreadContext context, IRubyObject index, IRubyObject val) {
        modifyAndKeepCodeRange();
        int i = RubyNumeric.num2int(index);
        int b = RubyNumeric.num2int(val);
        value.getUnsafeBytes()[checkIndexForRef(i, value.getRealSize())] = (byte)b;
        return val;
    }

    /** rb_str_to_i
     *
     */
    @JRubyMethod(name = "to_i", compat = RUBY1_8)
    public IRubyObject to_i() {
        return stringToInum(10, false);
    }

    /** rb_str_to_i
     *
     */
    @JRubyMethod(name = "to_i", compat = RUBY1_8)
    public IRubyObject to_i(IRubyObject arg0) {
        long base = checkBase(arg0);
        return stringToInum((int)base, false);
    }

    @JRubyMethod(name = "to_i", compat = RUBY1_9)
    public IRubyObject to_i19() {
        return stringToInum19(10, false);
    }

    @JRubyMethod(name = "to_i", compat = RUBY1_9)
    public IRubyObject to_i19(IRubyObject arg0) {
        long base = checkBase(arg0);
        return stringToInum19((int)base, false);
    }

    private long checkBase(IRubyObject arg0) {
        long base = arg0.convertToInteger().getLongValue();
        if(base < 0) {
            throw getRuntime().newArgumentError("illegal radix " + base);
        }
        return base;
    }

    /** rb_str_to_inum
     * 
     */
    public IRubyObject stringToInum(int base, boolean badcheck) {
        ByteList s = this.value;
        return ConvertBytes.byteListToInum(getRuntime(), s, base, badcheck);
    }

    public IRubyObject stringToInum19(int base, boolean badcheck) {
        ByteList s = this.value;
        return ConvertBytes.byteListToInum19(getRuntime(), s, base, badcheck);
    }

    /** rb_str_oct
     *
     */
    @JRubyMethod(name = "oct", compat = RUBY1_8)
    public IRubyObject oct(ThreadContext context) {
        return stringToInum(-8, false);
    }

    @JRubyMethod(name = "oct", compat = RUBY1_9)
    public IRubyObject oct19(ThreadContext context) {
        if (!value.getEncoding().isAsciiCompatible()) {
            throw context.getRuntime().newEncodingCompatibilityError("ASCII incompatible encoding: " + value.getEncoding());
        }
        return oct(context);
    }

    /** rb_str_hex
     *
     */
    @JRubyMethod(name = "hex", compat = RUBY1_8)
    public IRubyObject hex(ThreadContext context) {
        return stringToInum(16, false);
    }

    @JRubyMethod(name = "hex", compat = RUBY1_9)
    public IRubyObject hex19(ThreadContext context) {
        if (!value.getEncoding().isAsciiCompatible()) {
            throw context.getRuntime().newEncodingCompatibilityError("ASCII incompatible encoding: " + value.getEncoding());
        }
        return stringToInum19(16, false);
    }

    /** rb_str_to_f
     *
     */
    @JRubyMethod(name = "to_f", compat = RUBY1_8)
    public IRubyObject to_f() {
        return RubyNumeric.str2fnum(getRuntime(), this);
    }

    @JRubyMethod(name = "to_f", compat = RUBY1_9)
    public IRubyObject to_f19() {
        return RubyNumeric.str2fnum19(getRuntime(), this, false);
    }

    /** rb_str_split_m
     *
     */
    @JRubyMethod(name = "split", writes = BACKREF, compat = RUBY1_8)
    public RubyArray split(ThreadContext context) {
        return split(context, context.getRuntime().getNil());
    }

    @JRubyMethod(name = "split", writes = BACKREF, compat = RUBY1_8)
    public RubyArray split(ThreadContext context, IRubyObject arg0) {
        return splitCommon(arg0, false, 0, 0, context);
    }

    @JRubyMethod(name = "split", writes = BACKREF, compat = RUBY1_8)
    public RubyArray split(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        final int lim = RubyNumeric.num2int(arg1);
        if (lim <= 0) {
            return splitCommon(arg0, false, lim, 1, context);
        } else {
            if (lim == 1) return value.getRealSize() == 0 ? context.getRuntime().newArray() : context.getRuntime().newArray(this);
            return splitCommon(arg0, true, lim, 1, context);
        }
    }

    private RubyArray splitCommon(IRubyObject spat, final boolean limit, final int lim, final int i, ThreadContext context) {
        final RubyArray result;
        if (spat.isNil() && (spat = context.getRuntime().getGlobalVariables().get("$;")).isNil()) {
            result = awkSplit(limit, lim, i);
        } else {
            if (spat instanceof RubyString && ((RubyString) spat).value.getRealSize() == 1) {
                RubyString strSpat = (RubyString) spat;
                if (strSpat.value.getUnsafeBytes()[strSpat.value.getBegin()] == (byte) ' ') {
                    result = awkSplit(limit, lim, i);
                } else {
                    result = regexSplit(context, spat, limit, lim, i);
                }
            } else {
                result = regexSplit(context, spat, limit, lim, i);
            }
        }

        if (!limit && lim == 0) {
            while (result.size() > 0 && ((RubyString) result.eltInternal(result.size() - 1)).value.getRealSize() == 0) {
                result.pop(context);
            }
        }

        return result;
    }

    private RubyArray regexSplit(ThreadContext context, IRubyObject pat, boolean limit, int lim, int i) {
        Ruby runtime = context.getRuntime();
        final Regex pattern = getQuotedPattern(pat);

        int begin = value.getBegin();
        int len = value.getRealSize();
        int range = begin + len;
        byte[]bytes = value.getUnsafeBytes();

        final Matcher matcher = pattern.matcher(bytes, begin, range);

        RubyArray result = runtime.newArray();
        Encoding enc = getEncodingForKCodeDefault(runtime, pattern, pat);

        boolean captures = pattern.numberOfCaptures() != 0;

        int end, beg = 0;
        boolean lastNull = false;
        int start = begin;
        while ((end = matcher.search(start, range, Option.NONE)) >= 0) {
            if (start == end + begin && matcher.getBegin() == matcher.getEnd()) {
                if (len == 0) {
                    result.append(newEmptyString(runtime, getMetaClass()).infectBy(this));
                    break;
                } else if (lastNull) {
                    result.append(makeShared(runtime, beg, enc.length(bytes, begin + beg, range)));
                    beg = start - begin;
                } else {
                    start += start == range ? 1 : enc.length(bytes, start, range);
                    lastNull = true;
                    continue;
                }
            } else {
                result.append(makeShared(runtime, beg, end - beg));
                beg = matcher.getEnd();
                start = begin + beg;
            }
            lastNull = false;

            if (captures) populateCapturesForSplit(runtime, result, matcher, false);
            if (limit && lim <= ++i) break;
        }

        // only this case affects backrefs 
        context.getCurrentScope().setBackRef(runtime.getNil());

        if (len > 0 && (limit || len > beg || lim < 0)) result.append(makeShared(runtime, beg, len - beg));
        return result;
    }

    private Encoding getEncodingForKCodeDefault(Ruby runtime, Regex pattern, IRubyObject pat) {
        Encoding enc = pattern.getEncoding();
        if (enc != runtime.getKCode().getEncoding() && pat instanceof RubyRegexp) {
            RubyRegexp regexp = (RubyRegexp) pat;
            if (regexp.isKCodeDefault()) {
                enc = runtime.getKCode().getEncoding();
            }
        }
        return enc;
    }

    private void populateCapturesForSplit(Ruby runtime, RubyArray result, Matcher matcher, boolean is19) {
        Region region = matcher.getRegion();
        for (int i = 1; i < region.numRegs; i++) {
            int beg = region.beg[i];
            if (beg == -1) continue;
            result.append(is19 ? makeShared19(runtime, beg, region.end[i] - beg) : makeShared(runtime, beg, region.end[i] - beg));
        }
    }

    private RubyArray awkSplit(boolean limit, int lim, int i) {
        Ruby runtime = getRuntime();
        RubyArray result = runtime.newArray();

        byte[]bytes = value.getUnsafeBytes();
        int p = value.getBegin();
        int len = value.getRealSize();
        int end = p + len;

        boolean skip = true;

        int e = 0, b = 0;        
        while (p < end) {
            int c = bytes[p++] & 0xff;
            if (skip) {
                if (ASCII.isSpace(c)) {
                    b++;
                } else {
                    e = b + 1;
                    skip = false;
                    if (limit && lim <= i) break;
                }
            } else {
                if (ASCII.isSpace(c)) {
                    result.append(makeShared(runtime, b, e - b));
                    skip = true;
                    b = e + 1;
                    if (limit) i++;
                } else {
                    e++;
                }
            }
        }

        if (len > 0 && (limit || len > b || lim < 0)) result.append(makeShared(runtime, b, len - b));
        return result;
    }

    @JRubyMethod(name = "split", writes = BACKREF, compat = RUBY1_9)
    public RubyArray split19(ThreadContext context) {
        return split19(context, context.getRuntime().getNil());
    }

    @JRubyMethod(name = "split", writes = BACKREF, compat = RUBY1_9)
    public RubyArray split19(ThreadContext context, IRubyObject arg0) {
        return splitCommon19(arg0, false, 0, 0, context);
    }

    @JRubyMethod(name = "split", writes = BACKREF, compat = RUBY1_9)
    public RubyArray split19(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        final int lim = RubyNumeric.num2int(arg1);
        if (lim <= 0) {
            return splitCommon19(arg0, false, lim, 1, context);
        } else {
            if (lim == 1) return value.getRealSize() == 0 ? context.getRuntime().newArray() : context.getRuntime().newArray(this);
            return splitCommon19(arg0, true, lim, 1, context);
        }
    }

    private RubyArray splitCommon19(IRubyObject spat, final boolean limit, final int lim, final int i, ThreadContext context) {
        final RubyArray result;
        if (spat.isNil() && (spat = context.getRuntime().getGlobalVariables().get("$;")).isNil()) {
            result = awkSplit19(limit, lim, i);
        } else {
            if (spat instanceof RubyString) {
                ByteList spatValue = ((RubyString)spat).value;
                int len = spatValue.getRealSize();
                Encoding spatEnc = spatValue.getEncoding();
                if (len == 0) {
                    Regex pattern = RubyRegexp.getRegexpFromCache(context.getRuntime(), spatValue, spatEnc, new RegexpOptions());
                    result = regexSplit19(context, pattern, pattern, limit, lim, i);
                } else {
                    final int c;
                    byte[]bytes = spatValue.getUnsafeBytes();
                    int p = spatValue.getBegin();
                    if (spatEnc.isAsciiCompatible()) {
                        c = len == 1 ? bytes[p] & 0xff : -1;
                    } else {
                        c = len == StringSupport.preciseLength(spatEnc, bytes, p, p + len) ? spatEnc.mbcToCode(bytes, p, p + len) : -1;
                    }
                    result = c == ' ' ? awkSplit19(limit, lim, i) : stringSplit19(context, (RubyString)spat, limit, lim, i);
                }
            } else {
                final Regex pattern, prepared;
                final RubyRegexp regexp;
                Ruby runtime = context.getRuntime();
                if (spat instanceof RubyRegexp) {
                    regexp = (RubyRegexp)spat;
                    pattern = regexp.getPattern();
                    prepared = regexp.preparePattern(this);
                } else {
                    regexp = null;
                    pattern = getStringPattern19(runtime, spat);
                    prepared = RubyRegexp.preparePattern(runtime, pattern, this);
                }
                result = regexSplit19(context, pattern, prepared, limit, lim, i);
            }
        }

        if (!limit && lim == 0) {
            while (result.size() > 0 && ((RubyString) result.eltInternal(result.size() - 1)).value.getRealSize() == 0) {
                result.pop(context);
            }
        }

        return result;
    }

    private RubyArray regexSplit19(ThreadContext context, Regex pattern, Regex prepared, boolean limit, int lim, int i) {
        Ruby runtime = context.getRuntime();

        int begin = value.getBegin();
        int len = value.getRealSize();
        int range = begin + len;
        byte[]bytes = value.getUnsafeBytes();

        final Matcher matcher = prepared.matcher(bytes, begin, range);

        RubyArray result = runtime.newArray();
        Encoding enc = value.getEncoding();
        boolean captures = pattern.numberOfCaptures() != 0;

        int end, beg = 0;
        boolean lastNull = false;
        int start = begin;
        while ((end = matcher.search(start, range, Option.NONE)) >= 0) {
            if (start == end + begin && matcher.getBegin() == matcher.getEnd()) {
                if (len == 0) {
                    result.append(newEmptyString(runtime, getMetaClass()).infectBy(this));
                    break;
                } else if (lastNull) {
                    result.append(makeShared19(runtime, beg, StringSupport.length(enc, bytes, begin + beg, range)));
                    beg = start - begin;
                } else {
                    start += start == range ? 1 : StringSupport.length(enc, bytes, start, range);
                    lastNull = true;
                    continue;
                }
            } else {
                result.append(makeShared19(runtime, beg, end - beg));
                beg = matcher.getEnd();
                start = begin + beg;
            }
            lastNull = false;

            if (captures) populateCapturesForSplit(runtime, result, matcher, true);
            if (limit && lim <= ++i) break;
        }

        // only this case affects backrefs 
        context.getCurrentScope().setBackRef(runtime.getNil());

        if (len > 0 && (limit || len > beg || lim < 0)) result.append(makeShared19(runtime, beg, len - beg));
        return result;
    }

    private RubyArray awkSplit19(boolean limit, int lim, int i) {
        Ruby runtime = getRuntime();
        RubyArray result = runtime.newArray();

        byte[]bytes = value.getUnsafeBytes();
        int p = value.getBegin();
        int ptr = p;
        int len = value.getRealSize();
        int end = p + len;
        Encoding enc = value.getEncoding();
        boolean skip = true;

        int e = 0, b = 0;
        boolean singlebyte = singleByteOptimizable(enc);
        while (p < end) {
            final int c;
            if (singlebyte) {
                c = bytes[p++] & 0xff;
            } else {
                c = StringSupport.codePoint(runtime, enc, bytes, p, end);
                p += StringSupport.length(enc, bytes, p, end);
            }

            if (skip) {
                if (enc.isSpace(c)) {
                    b = p - ptr;
                } else {
                    e = p - ptr;
                    skip = false;
                    if (limit && lim <= i) break;
                }
            } else {
                if (enc.isSpace(c)) {
                    result.append(makeShared19(runtime, b, e - b));
                    skip = true;
                    b = p - ptr;
                    if (limit) i++;
                } else {
                    e = p - ptr;
                }
            }
        }

        if (len > 0 && (limit || len > b || lim < 0)) result.append(makeShared19(runtime, b, len - b));
        return result;
    }

    private RubyArray stringSplit19(ThreadContext context, RubyString spat, boolean limit, int lim, int i) {
        Ruby runtime = context.getRuntime();
        if (scanForCodeRange() == CR_BROKEN) throw runtime.newArgumentError("invalid byte sequence in " + value.getEncoding());
        if (spat.scanForCodeRange() == CR_BROKEN) throw runtime.newArgumentError("invalid byte sequence in " + spat.value.getEncoding());

        RubyArray result = runtime.newArray();
        Encoding enc = checkEncoding(spat);
        ByteList pattern = spat.value;

        int e, p = 0;
        
        while (p < value.getRealSize() && (e = value.indexOf(pattern, p)) >= 0) {
            int t = enc.rightAdjustCharHead(value.getUnsafeBytes(), p + value.getBegin(), e, p + value.getRealSize());
            if (t != e) {
                p = t;
                continue;
            }
            result.append(makeShared19(runtime, p, e - p));
            p = e + pattern.getRealSize();
            if (limit && lim <= ++i) break;
        }

        if (value.getRealSize() > 0 && (limit || value.getRealSize() > p || lim < 0)) {
            result.append(makeShared19(runtime, p, value.getRealSize() - p));
        }

        return result;
    }

    private RubyString getStringForPattern(IRubyObject obj) {
        if (obj instanceof RubyString) return (RubyString)obj;
        IRubyObject val = obj.checkStringType();
        if (val.isNil()) throw getRuntime().newTypeError("wrong argument type " + obj.getMetaClass() + " (expected Regexp)");
        return (RubyString)val;
    }

    /** get_pat (used by match/match19)
     * 
     */
    private RubyRegexp getPattern(IRubyObject obj) {
        if (obj instanceof RubyRegexp) return (RubyRegexp)obj;
        return RubyRegexp.newRegexp(getRuntime(), getStringForPattern(obj).value);
    }

    private Regex getQuotedPattern(IRubyObject obj) {
        if (obj instanceof RubyRegexp) return ((RubyRegexp)obj).getPattern();
        Ruby runtime = getRuntime();
        return RubyRegexp.getQuotedRegexpFromCache(runtime, getStringForPattern(obj).value, runtime.getKCode().getEncoding(), new RegexpOptions());
    }

    private Regex getStringPattern(Ruby runtime, Encoding enc, IRubyObject obj) {
        return RubyRegexp.getQuotedRegexpFromCache(runtime, getStringForPattern(obj).value, enc, new RegexpOptions());
    }

    private Regex getStringPattern19(Ruby runtime, IRubyObject obj) {
        RubyString str = getStringForPattern(obj);
        if (str.scanForCodeRange() == CR_BROKEN) {
            throw runtime.newRegexpError("invalid multybyte character: " +
                    RubyRegexp.regexpDescription19(runtime, str.value, new RegexpOptions(), str.value.getEncoding()).toString());
        }
        if (str.value.getEncoding().isDummy()) {
            throw runtime.newArgumentError("can't make regexp with dummy encoding");
        }
        
        return RubyRegexp.getQuotedRegexpFromCache19(runtime, str.value, new RegexpOptions(), str.isAsciiOnly());
    }

    /** rb_str_scan
     *
     */
    @JRubyMethod(reads = BACKREF, writes = BACKREF, compat = RUBY1_8)
    public IRubyObject scan(ThreadContext context, IRubyObject arg, Block block) {
        Ruby runtime = context.getRuntime();
        Encoding enc = runtime.getKCode().getEncoding();
        final Regex pattern;
        final int tuFlags;
        if (arg instanceof RubyRegexp) {
            RubyRegexp regex = (RubyRegexp)arg;
            pattern = regex.getPattern();
            tuFlags = regex.flags;
        } else {
            pattern = getStringPattern(runtime, enc, arg);
            if (arg.isTaint()) {
              tuFlags = RubyBasicObject.TAINTED_F; 
            }
            else {
              tuFlags = 0;
            }
        }

        int begin = value.getBegin();
        int range = begin + value.getRealSize();
        final Matcher matcher = pattern.matcher(value.getUnsafeBytes(), begin, range);

        if (block.isGiven()) {
            return scanIter(context, pattern, matcher, enc, block, begin, range, tuFlags);
        } else {
            return scanNoIter(context, pattern, matcher, enc, begin, range, tuFlags);
        }
    }

    private IRubyObject scanIter(ThreadContext context, Regex pattern, Matcher matcher, Encoding enc, Block block, int begin, int range, int tuFlags) {
        Ruby runtime = context.getRuntime();
        byte[]bytes = value.getUnsafeBytes();
        int size = value.getRealSize();
        RubyMatchData match = null;
        DynamicScope scope = context.getCurrentScope();

        int end = 0;
        if (pattern.numberOfCaptures() == 0) {
            while (matcher.search(begin + end, range, Option.NONE) >= 0) {
                end = positionEnd(matcher, enc, begin, range);
                match = RubyRegexp.updateBackRef(context, this, scope, matcher, pattern);
                RubyString substr = makeShared(runtime, matcher.getBegin(), matcher.getEnd() - matcher.getBegin());
                substr.infectBy(tuFlags);
                match.infectBy(tuFlags);
                block.yield(context, substr);
                modifyCheck(bytes, size);
            }
        } else {
            while (matcher.search(begin + end, range, Option.NONE) >= 0) {
                end = positionEnd(matcher, enc, begin, range);
                match = RubyRegexp.updateBackRef(context, this, scope, matcher, pattern);
                match.infectBy(tuFlags);
                block.yield(context, populateCapturesForScan(runtime, matcher, range, tuFlags, false));
                modifyCheck(bytes, size);
            }
        }
        scope.setBackRef(match == null ? runtime.getNil() : match);
        return this;
    }

    private IRubyObject scanNoIter(ThreadContext context, Regex pattern, Matcher matcher, Encoding enc, int begin, int range, int tuFlags) {
        Ruby runtime = context.getRuntime();
        RubyArray ary = runtime.newArray();

        int end = 0;
        if (pattern.numberOfCaptures() == 0) {
            while (matcher.search(begin + end, range, Option.NONE) >= 0) {
                end = positionEnd(matcher, enc, begin, range);
                RubyString substr = makeShared(runtime, matcher.getBegin(), matcher.getEnd() - matcher.getBegin());
                substr.infectBy(tuFlags);
                ary.append(substr);
            }
        } else {
            while (matcher.search(begin + end, range, Option.NONE) >= 0) {
                end = positionEnd(matcher, enc, begin, range);
                ary.append(populateCapturesForScan(runtime, matcher, range, tuFlags, false));
            }
        }

        DynamicScope scope = context.getCurrentScope();
        if (ary.size() > 0) {
            RubyMatchData match = RubyRegexp.updateBackRef(context, this, scope, matcher, pattern);
            match.infectBy(tuFlags);
        } else {
            scope.setBackRef(runtime.getNil());
        }
        return ary;
    }

    private int positionEnd(Matcher matcher, Encoding enc, int begin, int range) {
        int end = matcher.getEnd();
        if (matcher.getBegin() == end) {
            if (value.getRealSize() > end) {
                return end + enc.length(value.getUnsafeBytes(), begin + end, range);
            } else {
                return end + 1;
            }
        } else {
            return end;
        }
    }

    private IRubyObject populateCapturesForScan(Ruby runtime, Matcher matcher, int range, int tuFlags, boolean is19) {
        Region region = matcher.getRegion();
        RubyArray result = getRuntime().newArray(region.numRegs);
        for (int i=1; i<region.numRegs; i++) {
            int beg = region.beg[i]; 
            if (beg == -1) {
                result.append(runtime.getNil());
            } else {
                RubyString substr = is19 ? makeShared19(runtime, beg, region.end[i] - beg) : makeShared(runtime, beg, region.end[i] - beg);
                substr.infectBy(tuFlags);
                result.append(substr);
            }
        }
        return result;
    }

    @JRubyMethod(name = "scan", reads = BACKREF, writes = BACKREF, compat = RUBY1_9)
    public IRubyObject scan19(ThreadContext context, IRubyObject arg, Block block) {
        Ruby runtime = context.getRuntime();
        Encoding enc = value.getEncoding();
        final Regex pattern, prepared;
        final RubyRegexp regexp;
        final int tuFlags;
        if (arg instanceof RubyRegexp) {
            regexp = (RubyRegexp)arg;
            tuFlags = regexp.flags;
            pattern = regexp.getPattern();
            prepared = regexp.preparePattern(this);
        } else {
            regexp = null;
            tuFlags = arg.isTaint() ? RubyBasicObject.TAINTED_F : 0;
            pattern = getStringPattern19(runtime, arg);
            prepared = RubyRegexp.preparePattern(runtime, pattern, this);
        }

        if (block.isGiven()) {
            return scanIter19(context, pattern, prepared, enc, block, regexp, tuFlags);
        } else {
            return scanNoIter19(context, pattern, prepared, enc, regexp, tuFlags);
        }
    }

    private IRubyObject scanIter19(ThreadContext context, Regex pattern, Regex prepared, Encoding enc, Block block, RubyRegexp regexp, int tuFlags) {
        Ruby runtime = context.getRuntime();
        byte[]bytes = value.getUnsafeBytes();
        int begin = value.getBegin();
        int len = value.getRealSize();
        int range = begin + len;
        final Matcher matcher = prepared.matcher(bytes, begin, range);

        DynamicScope scope = context.getCurrentScope();

        int end = 0;
        RubyMatchData match = null;
        if (pattern.numberOfCaptures() == 0) {
            while (matcher.search(begin + end, range, Option.NONE) >= 0) {
                end = positionEnd(matcher, enc, begin, range);
                match = RubyRegexp.updateBackRef19(context, this, scope, matcher, pattern);
                match.regexp = regexp;
                RubyString substr = makeShared19(runtime, matcher.getBegin(), matcher.getEnd() - matcher.getBegin());
                substr.infectBy(tuFlags);
                match.infectBy(tuFlags);
                block.yield(context, substr);
                modifyCheck(bytes, len, enc);
            }
        } else {
            while (matcher.search(begin + end, range, Option.NONE) >= 0) {
                end = positionEnd(matcher, enc, begin, range);
                match = RubyRegexp.updateBackRef19(context, this, scope, matcher, pattern);
                match.regexp = regexp;
                match.infectBy(tuFlags);
                block.yield(context, populateCapturesForScan(runtime, matcher, range, tuFlags, true));
                modifyCheck(bytes, len, enc);
            }
        }
        scope.setBackRef(match == null ? runtime.getNil() : match);
        return this;
    }

    private IRubyObject scanNoIter19(ThreadContext context, Regex pattern, Regex prepared, Encoding enc, RubyRegexp regexp, int tuFlags) {
        Ruby runtime = context.getRuntime();
        byte[]bytes = value.getUnsafeBytes();
        int begin = value.getBegin();
        int range = begin + value.getRealSize();
        final Matcher matcher = prepared.matcher(bytes, begin, range);

        RubyArray ary = runtime.newArray();

        int end = 0;
        if (pattern.numberOfCaptures() == 0) {
            while (matcher.search(begin + end, range, Option.NONE) >= 0) {
                end = positionEnd(matcher, enc, begin, range);
                RubyString substr = makeShared19(runtime, matcher.getBegin(), matcher.getEnd() - matcher.getBegin());
                substr.infectBy(tuFlags);
                ary.append(substr);
            }
        } else {
            while (matcher.search(begin + end, range, Option.NONE) >= 0) {
                end = positionEnd(matcher, enc, begin, range);
                ary.append(populateCapturesForScan(runtime, matcher, range, tuFlags, true));
            }
        }

        DynamicScope scope = context.getCurrentScope();
        if (ary.size() > 0) {
            RubyMatchData match = RubyRegexp.updateBackRef19(context, this, scope, matcher, pattern);
            match.regexp = regexp;
            match.infectBy(tuFlags);
        } else {
            scope.setBackRef(runtime.getNil());
        }
        return ary;
    }

    @JRubyMethod(name = "start_with?")
    public IRubyObject start_with_p(ThreadContext context) {
        return context.getRuntime().getFalse();
    }

    @JRubyMethod(name = "start_with?")
    public IRubyObject start_with_p(ThreadContext context, IRubyObject arg) {
        return start_with_pCommon(arg) ? context.getRuntime().getTrue() : context.getRuntime().getFalse();
    }

    @JRubyMethod(name = "start_with?", rest = true)
    public IRubyObject start_with_p(ThreadContext context, IRubyObject[]args) {
        for (int i = 0; i < args.length; i++) {
            if (start_with_pCommon(args[i])) return context.getRuntime().getTrue();
        }
        return context.getRuntime().getFalse();
    }

    private boolean start_with_pCommon(IRubyObject arg) {
        IRubyObject tmp = arg.checkStringType();
        if (tmp.isNil()) return false;
        RubyString otherString = (RubyString)tmp;
        checkEncoding(otherString);
        if (value.getRealSize() < otherString.value.getRealSize()) return false;
        return value.startsWith(otherString.value);
    }

    @JRubyMethod(name = "end_with?")
    public IRubyObject end_with_p(ThreadContext context) {
        return context.getRuntime().getFalse();
    }

    @JRubyMethod(name = "end_with?")
    public IRubyObject end_with_p(ThreadContext context, IRubyObject arg) {
        return end_with_pCommon(arg) ? context.getRuntime().getTrue() : context.getRuntime().getFalse();
    }

    @JRubyMethod(name = "end_with?", rest = true)
    public IRubyObject end_with_p(ThreadContext context, IRubyObject[]args) {
        for (int i = 0; i < args.length; i++) {
            if (end_with_pCommon(args[i])) return context.getRuntime().getTrue();
        }
        return context.getRuntime().getFalse();
    }

    private boolean end_with_pCommon(IRubyObject arg) {
        IRubyObject tmp = arg.checkStringType();
        if (tmp.isNil()) return false;
        RubyString otherString = (RubyString)tmp;
        Encoding enc = checkEncoding(otherString);
        if (value.getRealSize() < otherString.value.getRealSize()) return false;
        int p = value.getBegin();
        int end = p + value.getRealSize();
        int s = end - otherString.value.getRealSize();
        if (enc.leftAdjustCharHead(value.getUnsafeBytes(), p, s, end) != s) return false;
        return value.endsWith(otherString.value);
    }

    private static final ByteList SPACE_BYTELIST = new ByteList(ByteList.plain(" "));

    private IRubyObject justify(IRubyObject arg0, int jflag) {
        Ruby runtime = getRuntime();
        return justifyCommon(runtime, SPACE_BYTELIST, RubyFixnum.num2int(arg0), jflag);
    }

    private IRubyObject justify(IRubyObject arg0, IRubyObject arg1, int jflag) {
        Ruby runtime = getRuntime();
        RubyString padStr = arg1.convertToString();
        ByteList pad = padStr.value;
        if (pad.getRealSize() == 0) throw runtime.newArgumentError("zero width padding");
        int width = RubyFixnum.num2int(arg0);
        RubyString result = justifyCommon(runtime, pad, width, jflag);
        if (value.getRealSize() < width) result.infectBy(padStr);
        return result;
    }

    private RubyString justifyCommon(Ruby runtime, ByteList pad, int width, int jflag) {
        if (width < 0 || value.getRealSize() >= width) return strDup(runtime);

        ByteList res = new ByteList(width);
        res.setRealSize(width);

        int padP = pad.getBegin();
        int padLen = pad.getRealSize();
        byte padBytes[] = pad.getUnsafeBytes();

        int p = res.getBegin();
        byte bytes[] = res.getUnsafeBytes();

        if (jflag != 'l') {
            int n = width - value.getRealSize();
            int end = p + ((jflag == 'r') ? n : n / 2);
            if (padLen <= 1) {
                while (p < end) {
                    bytes[p++] = padBytes[padP];
                }
            } else {
                int q = padP;
                while (p + padLen <= end) {
                    System.arraycopy(padBytes, padP, bytes, p, padLen);
                    p += padLen;
                }
                while (p < end) {
                    bytes[p++] = padBytes[q++];
                }
            }
        }

        System.arraycopy(value.getUnsafeBytes(), value.getBegin(), bytes, p, value.getRealSize());

        if (jflag != 'r') {
            p += value.getRealSize();
            int end = res.getBegin() + width;
            if (padLen <= 1) {
                while (p < end) {
                    bytes[p++] = padBytes[padP];
                }
            } else {
                while (p + padLen <= end) {
                    System.arraycopy(padBytes, padP, bytes, p, padLen);
                    p += padLen;
                }
                while (p < end) {
                    bytes[p++] = padBytes[padP++];
                }
            }
        }

        RubyString result = new RubyString(runtime, getMetaClass(), res);
        if ((!runtime.is1_9()) && (RubyFixnum.num2int(result.length())   > RubyFixnum.num2int(length())) ||
             (runtime.is1_9()  && (RubyFixnum.num2int(result.length19()) > RubyFixnum.num2int(length19())))) {
                 result.infectBy(this);
             }
        return result;
    }

    private IRubyObject justify19(IRubyObject arg0, int jflag) {
        Ruby runtime = getRuntime();
        RubyString result = justifyCommon(runtime, SPACE_BYTELIST, 
                                                   1,
                                                   true, value.getEncoding(), RubyFixnum.num2int(arg0), jflag);
        if (getCodeRange() != CR_BROKEN) result.setCodeRange(getCodeRange());
        return result;
    }

    private IRubyObject justify19(IRubyObject arg0, IRubyObject arg1, int jflag) {
        Ruby runtime = getRuntime();
        RubyString padStr = arg1.convertToString();
        ByteList pad = padStr.value;
        Encoding enc = checkEncoding(padStr);
        int padCharLen = padStr.strLength(enc);
        if (pad.getRealSize() == 0 || padCharLen == 0) throw runtime.newArgumentError("zero width padding");
        int width = RubyFixnum.num2int(arg0);
        RubyString result = justifyCommon(runtime, pad, 
                                                   padCharLen, 
                                                   padStr.singleByteOptimizable(), 
                                                   enc, width, jflag);
        if (RubyFixnum.num2int(result.length19()) > RubyFixnum.num2int(length19())) result.infectBy(padStr);
        int cr = codeRangeAnd(getCodeRange(), padStr.getCodeRange());
        if (cr != CR_BROKEN) result.setCodeRange(cr);
        return result;
    }

    private RubyString justifyCommon(Ruby runtime, ByteList pad, int padCharLen, boolean padSinglebyte, Encoding enc, int width, int jflag) {
        int len = strLength(enc);
        if (width < 0 || len >= width) return strDup(runtime);
        int n = width - len;

        int llen = (jflag == 'l') ? 0 : ((jflag == 'r') ? n : n / 2);
        int rlen = n - llen;

        int padP = pad.getBegin();
        int padLen = pad.getRealSize();
        byte padBytes[] = pad.getUnsafeBytes();

        ByteList res = new ByteList(value.getRealSize() + n * padLen / padCharLen + 2);

        int p = res.getBegin();
        byte bytes[] = res.getUnsafeBytes();

        while (llen > 0) {
            if (padLen <= 1) { 
                bytes[p++] = padBytes[padP];
                llen--;
            } else if (llen > padCharLen) {
                System.arraycopy(padBytes, padP, bytes, p, padLen);
                p += padLen;
                llen -= padCharLen;
            } else {
                int padPP = padSinglebyte ? padP + llen : StringSupport.nth(enc, padBytes, padP, padP + padLen, llen);
                n = padPP - padP;
                System.arraycopy(padBytes, padP, bytes, p, n);
                p += n;
                break;
            }
        }

        System.arraycopy(value.getUnsafeBytes(), value.getBegin(), bytes, p, value.getRealSize());
        p += value.getRealSize();

        while (rlen > 0) {
            if (padLen <= 1) { 
                bytes[p++] = padBytes[padP];
                rlen--;
            } else if (rlen > padCharLen) {
                System.arraycopy(padBytes, padP, bytes, p, padLen);
                p += padLen;
                rlen -= padCharLen;
            } else {
                int padPP = padSinglebyte ? padP + rlen : StringSupport.nth(enc, padBytes, padP, padP + padLen, rlen);
                n = padPP - padP;
                System.arraycopy(padBytes, padP, bytes, p, n);
                p += n;
                break;
            }
        }
        
        res.setRealSize(p);

        RubyString result = new RubyString(runtime, getMetaClass(), res);
        if ((!runtime.is1_9()) && (RubyFixnum.num2int(result.length())   > RubyFixnum.num2int(length())) ||
             (runtime.is1_9()  && (RubyFixnum.num2int(result.length19()) > RubyFixnum.num2int(length19())))) {
                 result.infectBy(this);
             }
        result.associateEncoding(enc);
        return result;
    }

    /** rb_str_ljust
     *
     */
    @JRubyMethod(name = "ljust", compat = RUBY1_8)
    public IRubyObject ljust(IRubyObject arg0) {
        return justify(arg0, 'l');
    }

    @JRubyMethod(name = "ljust", compat = RUBY1_8)
    public IRubyObject ljust(IRubyObject arg0, IRubyObject arg1) {
        return justify(arg0, arg1, 'l');
    }

    @JRubyMethod(name = "ljust", compat = RUBY1_9)
    public IRubyObject ljust19(IRubyObject arg0) {
        return justify19(arg0, 'l');
    }

    @JRubyMethod(name = "ljust", compat = RUBY1_9)
    public IRubyObject ljust19(IRubyObject arg0, IRubyObject arg1) {
        return justify19(arg0, arg1, 'l');
    }

    /** rb_str_rjust
     *
     */
    @JRubyMethod(name = "rjust", compat = RUBY1_8)
    public IRubyObject rjust(IRubyObject arg0) {
        return justify(arg0, 'r');
    }

    @JRubyMethod(name = "rjust", compat = RUBY1_8)
    public IRubyObject rjust(IRubyObject arg0, IRubyObject arg1) {
        return justify(arg0, arg1, 'r');
    }

    @JRubyMethod(name = "rjust", compat = RUBY1_9)
    public IRubyObject rjust19(IRubyObject arg0) {
        return justify19(arg0, 'r');
    }

    @JRubyMethod(name = "rjust", compat = RUBY1_9)
    public IRubyObject rjust19(IRubyObject arg0, IRubyObject arg1) {
        return justify19(arg0, arg1, 'r');
    }

    /** rb_str_center
     *
     */
    @JRubyMethod(compat = RUBY1_8)
    public IRubyObject center(IRubyObject arg0) {
        return justify(arg0, 'c');
    }

    @JRubyMethod(compat = RUBY1_8)
    public IRubyObject center(IRubyObject arg0, IRubyObject arg1) {
        return justify(arg0, arg1, 'c');
    }

    @JRubyMethod(name = "center", compat = RUBY1_9)
    public IRubyObject center19(IRubyObject arg0) {
        return justify19(arg0, 'c');
    }

    @JRubyMethod(name = "center", compat = RUBY1_9)
    public IRubyObject center19(IRubyObject arg0, IRubyObject arg1) {
        return justify19(arg0, arg1, 'c');
    }

    @JRubyMethod
    public IRubyObject partition(ThreadContext context, Block block) {
        return RubyEnumerable.partition(context, this, block);
    }

    @JRubyMethod
    public IRubyObject partition(ThreadContext context, IRubyObject arg, Block block) {
        Ruby runtime = context.getRuntime();
        final int pos;
        final RubyString sep;
        if (arg instanceof RubyRegexp) {
            RubyRegexp regex = (RubyRegexp)arg;
            pos = regex.search19(context, this, 0, false);
            if (pos < 0) return partitionMismatch(runtime);
            sep = (RubyString)subpat19(runtime, context, regex);
            if (pos == 0 && sep.value.getRealSize() == 0) return partitionMismatch(runtime);
        } else {
            IRubyObject tmp = arg.checkStringType();
            if (tmp.isNil()) throw runtime.newTypeError("type mismatch: " + arg.getMetaClass().getName() + " given");
            sep = (RubyString)tmp;
            pos = strIndex19(sep, 0);
            if (pos < 0) return partitionMismatch(runtime);
        }

        return RubyArray.newArray(runtime, new IRubyObject[]{
                makeShared19(runtime, 0, pos),
                sep,
                makeShared19(runtime, pos + sep.value.getRealSize(), value.getRealSize() - pos - sep.value.getRealSize())});
    }

    private IRubyObject partitionMismatch(Ruby runtime) {
        return RubyArray.newArray(runtime, new IRubyObject[]{this, newEmptyString(runtime), newEmptyString(runtime)});
    }
    
    @JRubyMethod(name = "rpartition")
    public IRubyObject rpartition(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.getRuntime();
        final int pos;
        final RubyString sep;
        if (arg instanceof RubyRegexp) {
            RubyRegexp regex = (RubyRegexp)arg;
            pos = regex.search19(context, this, value.getRealSize(), true);
            if (pos < 0) return rpartitionMismatch(runtime);
            sep = (RubyString)RubyRegexp.nth_match(0, context.getCurrentScope().getBackRef(runtime));
        } else {
            IRubyObject tmp = arg.checkStringType();
            if (tmp.isNil()) throw runtime.newTypeError("type mismatch: " + arg.getMetaClass().getName() + " given");
            sep = (RubyString)tmp;
            pos = strRindex19(sep, subLength(value.getRealSize()));
            if (pos < 0) return rpartitionMismatch(runtime);
        }

        return RubyArray.newArray(runtime, new IRubyObject[]{
                substr19(runtime, 0, pos),
                sep,
                substr19(runtime, pos + sep.strLength(), value.getRealSize())});
    }

    private IRubyObject rpartitionMismatch(Ruby runtime) {
        return RubyArray.newArray(runtime, new IRubyObject[]{newEmptyString(runtime), newEmptyString(runtime), this});
    }
    
    /** rb_str_chop / rb_str_chop_bang
     * 
     */
    @JRubyMethod(name = "chop", compat = RUBY1_8)
    public IRubyObject chop(ThreadContext context) {
        if (value.getRealSize() == 0) return newEmptyString(context.getRuntime(), getMetaClass()).infectBy(this);
        return makeShared(context.getRuntime(), 0, choppedLength());
    }

    @JRubyMethod(name = "chop!", compat = RUBY1_8)
    public IRubyObject chop_bang(ThreadContext context) {
        if (value.getRealSize() == 0) return context.getRuntime().getNil();
        view(0, choppedLength());
        return this;
    }

    private int choppedLength() {
        int end = value.getRealSize() - 1;
        if ((value.getUnsafeBytes()[value.getBegin() + end]) == '\n') {
            if (end > 0 && (value.getUnsafeBytes()[value.getBegin() + end - 1]) == '\r') end--;
        }
        return end;
    }

    @JRubyMethod(name = "chop", compat = RUBY1_9)
    public IRubyObject chop19(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) return newEmptyString(runtime, getMetaClass(), value.getEncoding()).infectBy(this);
        return makeShared19(runtime, 0, choppedLength19(runtime));
    }

    @JRubyMethod(name = "chop!", compat = RUBY1_9)
    public IRubyObject chop_bang19(ThreadContext context) {
        modifyCheck();
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) return runtime.getNil();
        view(0, choppedLength19(runtime));
        if (getCodeRange() != CR_7BIT) clearCodeRange();
        return this;
    }

    private int choppedLength19(Ruby runtime) {
        int p = value.getBegin();
        int end = p + value.getRealSize();

        if (p > end) return 0;
        byte bytes[] = value.getUnsafeBytes();
        Encoding enc = value.getEncoding();

        int s = enc.prevCharHead(bytes, p, end, end);
        if (s == -1) return 0;
        if (s > p && codePoint(runtime, enc, bytes, s, end) == '\n') {
            int s2 = enc.prevCharHead(bytes, p, s, end);
            if (s2 != -1 && codePoint(runtime, enc, bytes, s2, end) == '\r') s = s2;
        }
        return s - p;
    }

    /** rb_str_chop
     * 
     */
    @JRubyMethod(name = "chomp", compat = RUBY1_8)
    public RubyString chomp(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.chomp_bang(context);
        return str;
    }

    @JRubyMethod(name = "chomp", compat = RUBY1_8)
    public RubyString chomp(ThreadContext context, IRubyObject arg0) {
        RubyString str = strDup(context.getRuntime());
        str.chomp_bang(context, arg0);
        return str;
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
     */
    @JRubyMethod(name = "chomp!", compat = RUBY1_8)
    public IRubyObject chomp_bang(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) return runtime.getNil();

        IRubyObject rsObj = runtime.getGlobalVariables().get("$/");

        if (rsObj == runtime.getGlobalVariables().getDefaultSeparator()) return smartChopBangCommon(runtime);
        return chompBangCommon(runtime, rsObj);
    }

    @JRubyMethod(name = "chomp!", compat = RUBY1_8)
    public IRubyObject chomp_bang(ThreadContext context, IRubyObject arg0) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) return runtime.getNil();
        return chompBangCommon(runtime, arg0);
    }

    private IRubyObject chompBangCommon(Ruby runtime, IRubyObject rsObj) {
        if (rsObj.isNil()) return rsObj;

        RubyString rs = rsObj.convertToString();
        int p = value.getBegin();
        int len = value.getRealSize();
        byte[] bytes = value.getUnsafeBytes();

        int rslen = rs.value.getRealSize();
        if (rslen == 0) {
            while (len > 0 && bytes[p + len - 1] == (byte)'\n') {
                len--;
                if (len > 0 && bytes[p + len - 1] == (byte)'\r') len--;
            }
            if (len < value.getRealSize()) {
                view(0, len);
                return this;
            }
            return runtime.getNil();
        }

        if (rslen > len) return runtime.getNil();
        byte newline = rs.value.getUnsafeBytes()[rslen - 1];
        if (rslen == 1 && newline == (byte)'\n') return smartChopBangCommon(runtime);

        if (bytes[p + len - 1] == newline && rslen <= 1 || value.endsWith(rs.value)) {
            view(0, value.getRealSize() - rslen);
            return this;
        }
        return runtime.getNil();
    }
    
    private IRubyObject smartChopBangCommon(Ruby runtime) {
        ByteList v = this.value;
        int len = v.getRealSize();
        int p = v.getBegin();
        byte[] bytes = v.getUnsafeBytes();
        byte b = bytes[p + len - 1];
        if (b == (byte)'\n') {
            len--;
            if (len > 0 && bytes[p + len - 1] == (byte)'\r') len--;
            view(0, len);
        } else if (b == (byte)'\r') {
            len--;
            view(0, len);
        } else {
            modifyCheck();
            return runtime.getNil();
        }
        return this; 
    }

    @JRubyMethod(name = "chomp", compat = RUBY1_9)
    public RubyString chomp19(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.chomp_bang19(context);
        return str;
    }

    @JRubyMethod(name = "chomp", compat = RUBY1_9)
    public RubyString chomp19(ThreadContext context, IRubyObject arg0) {
        RubyString str = strDup(context.getRuntime());
        str.chomp_bang19(context, arg0);
        return str;
    }

    @JRubyMethod(name = "chomp!", compat = RUBY1_9)
    public IRubyObject chomp_bang19(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) return runtime.getNil();

        IRubyObject rsObj = runtime.getGlobalVariables().get("$/");

        if (rsObj == runtime.getGlobalVariables().getDefaultSeparator()) return smartChopBangCommon19(runtime);
        return chompBangCommon19(runtime, rsObj);
    }

    @JRubyMethod(name = "chomp!", compat = RUBY1_9)
    public IRubyObject chomp_bang19(ThreadContext context, IRubyObject arg0) {
        modifyCheck();
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) return runtime.getNil();
        return chompBangCommon19(runtime, arg0);
    }
    
    private IRubyObject chompBangCommon19(Ruby runtime, IRubyObject rsObj) {
        if (rsObj.isNil()) return rsObj;

        RubyString rs = rsObj.convertToString();
        int p = value.getBegin();
        int len = value.getRealSize();
        int end = p + len;  
        byte[] bytes = value.getUnsafeBytes();

        int rslen = rs.value.getRealSize();
        if (rslen == 0) {
            while (len > 0 && bytes[p + len - 1] == (byte)'\n') {
                len--;
                if (len > 0 && bytes[p + len - 1] == (byte)'\r') len--;
            }
            if (len < value.getRealSize()) {
                keepCodeRange();
                view(0, len);
                return this;
            }
            return runtime.getNil();
        }

        if (rslen > len) return runtime.getNil();
        byte newline = rs.value.getUnsafeBytes()[rslen - 1];
        if (rslen == 1 && newline == (byte)'\n') return smartChopBangCommon19(runtime);

        Encoding enc = checkEncoding(rs);
        if (rs.scanForCodeRange() == CR_BROKEN) return runtime.getNil();

        int pp = end - rslen; 
        if (bytes[p + len - 1] == newline && rslen <= 1 || value.endsWith(rs.value)) {
            if (enc.leftAdjustCharHead(bytes, p, pp, end) != pp) return runtime.getNil();
            if (getCodeRange() != CR_7BIT) clearCodeRange();
            view(0, value.getRealSize() - rslen);
            return this;
        }
        return runtime.getNil();
    }
    
    private IRubyObject smartChopBangCommon19(Ruby runtime) {
        final int p = value.getBegin();
        int len = value.getRealSize();
        int end = p + len;
        byte bytes[] = value.getUnsafeBytes();
        Encoding enc = value.getEncoding();

        keepCodeRange();
        if (enc.minLength() > 1) {
            int pp = enc.leftAdjustCharHead(bytes, p, end - enc.minLength(), end);
            if (enc.isNewLine(bytes, pp, end)) end = pp;
            pp = end - enc.minLength();
            if (pp >= p) {
                pp = enc.leftAdjustCharHead(bytes, p, pp, end);
                if (StringSupport.preciseLength(enc, bytes, pp, end) > 0 && 
                        enc.mbcToCode(bytes, pp, end) == '\r') end = pp;
            }
            if (end == p + value.getRealSize()) {
                modifyCheck();
                return runtime.getNil();
            }
            len = end - p;
            view(0, len);
        } else {
            if (bytes[p + len - 1] == (byte)'\n') {
                len--;
                if (len > 0 && bytes[p + len - 1] == (byte)'\r') len--;
                view(0, len);
            } else if (bytes[p + len - 1] == (byte)'\r') {
                len--;
                view(0, len);
            } else {
                modifyCheck();
                return runtime.getNil();
            }
        }
        return this;
    }

    /** rb_str_lstrip / rb_str_lstrip_bang
     * 
     */
    @JRubyMethod(name = "lstrip", compat = RUBY1_8)
    public IRubyObject lstrip(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.lstrip_bang(context);
        return str;
    }

    @JRubyMethod(name = "lstrip!", compat = RUBY1_8)
    public IRubyObject lstrip_bang(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) return runtime.getNil();
        return singleByteLStrip(runtime, value.getUnsafeBytes(), value.getBegin(), value.getBegin() + value.getRealSize());
    }

    @JRubyMethod(name = "lstrip", compat = RUBY1_9)
    public IRubyObject lstrip19(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.lstrip_bang19(context);
        return str;
    }

    @JRubyMethod(name = "lstrip!", compat = RUBY1_9)
    public IRubyObject lstrip_bang19(ThreadContext context) {
        modifyCheck();
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) {
            return runtime.getNil();
        }

        Encoding enc = value.getEncoding();
        int s = value.getBegin();
        int end = s + value.getRealSize();
        byte[]bytes = value.getUnsafeBytes();

        final IRubyObject result;
        if (singleByteOptimizable(enc)) {
            result = singleByteLStrip(runtime, bytes, s, end);
        } else {
            result = multiByteLStrip(runtime, enc, bytes, s, end);
        }
        keepCodeRange();
        return result;
    }

    private IRubyObject singleByteLStrip(Ruby runtime, byte[]bytes, int s, int end) {
        int p = s;
        while (p < end && ASCII.isSpace(bytes[p] & 0xff)) p++;
        if (p > s) {
            view(p - s, end - p);
            return this;
        }
        return runtime.getNil();
    }

    private IRubyObject multiByteLStrip(Ruby runtime, Encoding enc, byte[]bytes, int s, int end) {
        int p = s;

        while (p < end) {
            int c = codePoint(runtime, enc, bytes, p, end);
            if (!ASCII.isSpace(c)) break;
            p += codeLength(runtime, enc, c);
        }

        if (p > s) {
            view(p - s, end - p);
            return this;
        }

        return runtime.getNil();
    }

    /** rb_str_rstrip / rb_str_rstrip_bang
     *  
     */
    @JRubyMethod(name = "rstrip", compat = RUBY1_8)
    public IRubyObject rstrip(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.rstrip_bang(context);
        return str;
    }

    @JRubyMethod(name = "rstrip!", compat = RUBY1_8)
    public IRubyObject rstrip_bang(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) return runtime.getNil();
        return singleByteRStrip(runtime, value.getUnsafeBytes(), value.getBegin(), value.getBegin() + value.getRealSize());
    }

    @JRubyMethod(name = "rstrip", compat = RUBY1_9)
    public IRubyObject rstrip19(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.rstrip_bang19(context);
        return str;
    }

    @JRubyMethod(name = "rstrip!", compat = RUBY1_9)
    public IRubyObject rstrip_bang19(ThreadContext context) {
        modifyCheck();
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) {
            return runtime.getNil();
        }

        IRubyObject result = singleByteOptimizable(value.getEncoding()) ?
            singleByteRStrip19(runtime) : multiByteRStrip19(runtime);

        keepCodeRange();
        return result;
    }

    // In 1.8 we will strip rightmost \0 followed by any \s
    private IRubyObject singleByteRStrip(Ruby runtime, byte[]bytes, int s, int end) {
        int endp = end - 1;
        while (endp >= s && bytes[endp] == 0) endp--;
        while (endp >= s && ASCII.isSpace(bytes[endp] & 0xff)) endp--;

        if (endp < end - 1) {
            view(0, endp - s + 1);
            return this;
        }
        return runtime.getNil();
    }
    
    // In 1.9 we strip any combination of \0 and \s
    private IRubyObject singleByteRStrip19(Ruby runtime) {
        byte[] bytes = value.getUnsafeBytes();
        int start = value.getBegin();
        int end = start + value.getRealSize();
        int endp = end - 1;
        while (endp >= start && (bytes[endp] == 0 || 
                ASCII.isSpace(bytes[endp] & 0xff))) endp--;

        if (endp < end - 1) {
            view(0, endp - start + 1);
            return this;
        }
        
        return runtime.getNil();
    }    
    
    // In 1.9 we strip any combination of \0 and \s
    private IRubyObject multiByteRStrip19(Ruby runtime) {
        byte[] bytes = value.getUnsafeBytes();
        int start = value.getBegin();
        int end = start + value.getRealSize();
        Encoding enc = value.getEncoding();
        int endp = end;
        int prev;
        while ((prev = enc.prevCharHead(bytes, start, endp, end)) != -1) {
            int point = codePoint(runtime, enc, bytes, prev, end);
            if (point != 0 && !ASCII.isSpace(point)) break;
            endp = prev;
        }

        if (endp < end) {
            view(0, endp - start);
            return this;
        }
        return runtime.getNil();
    }

    /** rb_str_strip / rb_str_strip_bang
     *
     */
    @JRubyMethod(name = "strip", compat = RUBY1_8)
    public IRubyObject strip(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.strip_bang(context);
        return str;
    }

    @JRubyMethod(name = "strip!", compat = RUBY1_8)
    public IRubyObject strip_bang(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) return runtime.getNil();
        return singleByteStrip(runtime, value.getUnsafeBytes(), value.getBegin(), value.getBegin() + value.getRealSize());
    }

    @JRubyMethod(name = "strip", compat = RUBY1_9)
    public IRubyObject strip19(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.strip_bang19(context);
        return str;
    }

    @JRubyMethod(name = "strip!", compat = RUBY1_9)
    public IRubyObject strip_bang19(ThreadContext context) {
        modifyCheck();
        
        IRubyObject left = lstrip_bang19(context);
        IRubyObject right = rstrip_bang19(context);
        
        return left.isNil() && right.isNil() ? context.getRuntime().getNil() : this;
    }

    private IRubyObject singleByteStrip(Ruby runtime, byte[]bytes, int s, int end) {
        int p = s;
        while (p < end && ASCII.isSpace(bytes[p] & 0xff)) p++;
        int endp = end - 1;
        while (endp >= p && bytes[endp] == 0) endp--;
        while (endp >= p && ASCII.isSpace(bytes[endp] & 0xff)) endp--;

        if (p > s || endp < end - 1) {
            view(p - s, endp - p + 1);
            return this;
        }
        return runtime.getNil();
    }
    
    /** rb_str_count
     *
     */
    @JRubyMethod(name = "count", compat = RUBY1_8)
    public IRubyObject count(ThreadContext context) {
        throw context.getRuntime().newArgumentError("wrong number of arguments");
    }

    @JRubyMethod(name = "count", compat = RUBY1_8)
    public IRubyObject count(ThreadContext context, IRubyObject arg) {
        final boolean[]table = new boolean[TRANS_SIZE];
        arg.convertToString().trSetupTable(table, true);
        return countCommon(context.getRuntime(), table);
    }

    @JRubyMethod(name = "count", required = 1, rest = true, compat = RUBY1_8)
    public IRubyObject count(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) return RubyFixnum.zero(runtime);

        final boolean[]table = new boolean[TRANS_SIZE];
        args[0].convertToString().trSetupTable(table, true);
        for (int i = 1; i<args.length; i++) {
            args[i].convertToString().trSetupTable(table, false);
        }

        return countCommon(runtime, table);
    }

    private IRubyObject countCommon(Ruby runtime, boolean[]table) {
        int i = 0;
        byte[]bytes = value.getUnsafeBytes();
        int p = value.getBegin();
        int end = p + value.getRealSize();

        while (p < end) if (table[bytes[p++] & 0xff]) i++;
        return runtime.newFixnum(i);
    }

    @JRubyMethod(name = "count", compat = RUBY1_9)
    public IRubyObject count19(ThreadContext context) {
        throw context.getRuntime().newArgumentError("wrong number of arguments");
    }

    @JRubyMethod(name = "count", compat = RUBY1_9)
    public IRubyObject count19(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) return RubyFixnum.zero(runtime);

        RubyString otherStr = arg.convertToString();
        Encoding enc = checkEncoding(otherStr);
        
        int c;
        if (otherStr.value.length() == 1 && enc.isAsciiCompatible() &&
                ((c = otherStr.value.unsafeBytes()[otherStr.value.getBegin()] & 0xff)) < 0x80 && scanForCodeRange() != CR_BROKEN) {

            if (value.length() ==0) return RubyFixnum.zero(runtime);
            byte[]bytes = value.unsafeBytes();
            int p = value.getBegin();
            int end = p + value.length();
            int n = 0;
            while (p < end) {
                if ((bytes[p++] & 0xff) == c) n++;
            }
            return RubyFixnum.newFixnum(runtime, n);
        }
        
        final boolean[]table = new boolean[TRANS_SIZE + 1];
        TrTables tables = otherStr.trSetupTable(context.getRuntime(), table, null, true, enc);
        return countCommon19(runtime, table, tables, enc);
    }

    @JRubyMethod(name = "count", required = 1, rest = true, compat = RUBY1_9)
    public IRubyObject count19(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) return RubyFixnum.zero(runtime);

        RubyString otherStr = args[0].convertToString();
        Encoding enc = checkEncoding(otherStr);
        final boolean[]table = new boolean[TRANS_SIZE + 1];
        TrTables tables = otherStr.trSetupTable(runtime, table, null, true, enc);
        for (int i = 1; i<args.length; i++) {
            otherStr = args[i].convertToString();
            enc = checkEncoding(otherStr);
            tables = otherStr.trSetupTable(runtime, table, tables, false, enc);
        }

        return countCommon19(runtime, table, tables, enc);
    }

    private IRubyObject countCommon19(Ruby runtime, boolean[]table, TrTables tables, Encoding enc) {
        int i = 0;
        byte[]bytes = value.getUnsafeBytes();
        int p = value.getBegin();
        int end = p + value.getRealSize();

        int c;
        while (p < end) {
            if (enc.isAsciiCompatible() && (c = bytes[p] & 0xff) < 0x80) {
                if (table[c]) i++;
                p++;
            } else {
                c = codePoint(runtime, enc, bytes, p, end);
                int cl = codeLength(runtime, enc, c);
                if (trFind(c, table, tables)) i++;
                p += cl;
            }
        }

        return runtime.newFixnum(i);
    }

    /** rb_str_delete / rb_str_delete_bang
     *
     */
    @JRubyMethod(name = "delete", compat = RUBY1_8)
    public IRubyObject delete(ThreadContext context) {
        throw context.getRuntime().newArgumentError("wrong number of arguments");
    }

    @JRubyMethod(name = "delete", compat = RUBY1_8)
    public IRubyObject delete(ThreadContext context, IRubyObject arg) {
        RubyString str = strDup(context.getRuntime());
        str.delete_bang(context, arg);
        return str;
    }

    @JRubyMethod(name = "delete", required = 1, rest = true, compat = RUBY1_8)
    public IRubyObject delete(ThreadContext context, IRubyObject[] args) {
        RubyString str = strDup(context.getRuntime());
        str.delete_bang(context, args);
        return str;
    }

    @JRubyMethod(name = "delete!", compat = RUBY1_8)
    public IRubyObject delete_bang(ThreadContext context) {
        throw context.getRuntime().newArgumentError("wrong number of arguments");
    }

    @JRubyMethod(name = "delete!", compat = RUBY1_8)
    public IRubyObject delete_bang(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) return runtime.getNil();
        final boolean[]squeeze = new boolean[TRANS_SIZE];
        arg.convertToString().trSetupTable(squeeze, true);
        return delete_bangCommon(runtime, squeeze);
    }

    @JRubyMethod(name = "delete!", required = 1, rest = true, compat = RUBY1_8)
    public IRubyObject delete_bang(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) return runtime.getNil();
        boolean[]squeeze = new boolean[TRANS_SIZE];

        args[0].convertToString().trSetupTable(squeeze, true);
        for (int i=1; i<args.length; i++) {
            args[i].convertToString().trSetupTable(squeeze, false);
        }

        return delete_bangCommon(runtime, squeeze);
    }

    private IRubyObject delete_bangCommon(Ruby runtime, boolean[]squeeze) {
        modify();

        int s = value.getBegin();
        int t = s;
        int send = s + value.getRealSize();
        byte[]bytes = value.getUnsafeBytes();
        boolean modify = false;

        while (s < send) {
            if (squeeze[bytes[s] & 0xff]) {
                modify = true;
            } else {
                bytes[t++] = bytes[s];
            }
            s++;
        }
        value.setRealSize(t - value.getBegin());

        return modify ? this : runtime.getNil();        
    }

    @JRubyMethod(name = "delete", compat = RUBY1_9)
    public IRubyObject delete19(ThreadContext context) {
        throw context.getRuntime().newArgumentError("wrong number of arguments");
    }

    @JRubyMethod(name = "delete", compat = RUBY1_9)
    public IRubyObject delete19(ThreadContext context, IRubyObject arg) {
        RubyString str = strDup(context.getRuntime());
        str.delete_bang19(context, arg);
        return str;
    }

    @JRubyMethod(name = "delete", required = 1, rest = true, compat = RUBY1_9)
    public IRubyObject delete19(ThreadContext context, IRubyObject[] args) {
        RubyString str = strDup(context.getRuntime());
        str.delete_bang19(context, args);
        return str;
    }

    @JRubyMethod(name = "delete!", compat = RUBY1_9)
    public IRubyObject delete_bang19(ThreadContext context) {
        throw context.getRuntime().newArgumentError("wrong number of arguments");
    }

    @JRubyMethod(name = "delete!", compat = RUBY1_9)
    public IRubyObject delete_bang19(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) return runtime.getNil();

        RubyString otherStr = arg.convertToString();
        Encoding enc = checkEncoding(otherStr);
        final boolean[]squeeze = new boolean[TRANS_SIZE + 1];
        TrTables tables = otherStr.trSetupTable(runtime, squeeze, null, true, enc);
        return delete_bangCommon19(runtime, squeeze, tables, enc);
    }

    @JRubyMethod(name = "delete!", required = 1, rest = true, compat = RUBY1_9)
    public IRubyObject delete_bang19(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) return runtime.getNil();

        RubyString otherStr = args[0].convertToString();
        Encoding enc = checkEncoding(otherStr);
        boolean[]squeeze = new boolean[TRANS_SIZE + 1];
        TrTables tables = otherStr.trSetupTable(runtime, squeeze, null, true, enc);
        for (int i=1; i<args.length; i++) {
            otherStr = args[i].convertToString();
            enc = checkEncoding(otherStr);
            tables = otherStr.trSetupTable(runtime, squeeze, tables, false, enc);
        }

        return delete_bangCommon19(runtime, squeeze, tables, enc);
    }

    private IRubyObject delete_bangCommon19(Ruby runtime, boolean[]squeeze, TrTables tables, Encoding enc) {
        modifyAndKeepCodeRange();

        int s = value.getBegin();
        int t = s;
        int send = s + value.getRealSize();
        byte[]bytes = value.getUnsafeBytes();
        boolean modify = false;
        boolean asciiCompatible = enc.isAsciiCompatible();
        int cr = asciiCompatible ? CR_7BIT : CR_VALID; 
        while (s < send) {
            int c;
            if (asciiCompatible && Encoding.isAscii(c = bytes[s] & 0xff)) {
                if (squeeze[c]) {
                    modify = true;
                } else {
                    if (t != s) bytes[t] = (byte)c;
                    t++;
                }
                s++;
            } else {
                c = codePoint(runtime, enc, bytes, s, send);
                int cl = codeLength(runtime, enc, c);
                if (trFind(c, squeeze, tables)) {
                    modify = true;
                } else {
                    if (t != s) enc.codeToMbc(c, bytes, t);
                    t += cl;
                    if (cr == CR_7BIT) cr = CR_VALID;
                }
                s += cl;
            }
        }
        value.setRealSize(t - value.getBegin());
        setCodeRange(cr);

        return modify ? this : runtime.getNil();        
    }

    /** rb_str_squeeze / rb_str_squeeze_bang
     *
     */
    @JRubyMethod(name = "squeeze", compat = RUBY1_8)
    public IRubyObject squeeze(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.squeeze_bang(context);
        return str;
    }

    @JRubyMethod(name = "squeeze", compat = RUBY1_8)
    public IRubyObject squeeze(ThreadContext context, IRubyObject arg) {
        RubyString str = strDup(context.getRuntime());
        str.squeeze_bang(context, arg);
        return str;
    }

    @JRubyMethod(name = "squeeze", rest = true, compat = RUBY1_8)
    public IRubyObject squeeze(ThreadContext context, IRubyObject[] args) {
        RubyString str = strDup(context.getRuntime());
        str.squeeze_bang(context, args);
        return str;
    }

    @JRubyMethod(name = "squeeze!", compat = RUBY1_8)
    public IRubyObject squeeze_bang(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) {
            modifyCheck();
            return runtime.getNil();
        }
        final boolean squeeze[] = new boolean[TRANS_SIZE];
        for (int i=0; i<TRANS_SIZE; i++) squeeze[i] = true;
        modify();
        return squeezeCommon(runtime, squeeze);
    }

    @JRubyMethod(name = "squeeze!", compat = RUBY1_8)
    public IRubyObject squeeze_bang(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) {
            modifyCheck();
            return runtime.getNil();
        }
        final boolean squeeze[] = new boolean[TRANS_SIZE];
        arg.convertToString().trSetupTable(squeeze, true);
        modify();
        return squeezeCommon(runtime, squeeze);
    }

    @JRubyMethod(name = "squeeze!", rest = true, compat = RUBY1_8)
    public IRubyObject squeeze_bang(ThreadContext context, IRubyObject[] args) {
        if (args.length == 0) return squeeze_bang(context);
        
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) {
            modifyCheck();
            return runtime.getNil();
        }

        final boolean squeeze[] = new boolean[TRANS_SIZE];
        args[0].convertToString().trSetupTable(squeeze, true);
        for (int i=1; i<args.length; i++) {
            args[i].convertToString().trSetupTable(squeeze, false);
        }

        modify();
        return squeezeCommon(runtime, squeeze);
    }

    private IRubyObject squeezeCommon(Ruby runtime, boolean squeeze[]) {
        int s = value.getBegin();
        int t = s;
        int send = s + value.getRealSize();
        byte[]bytes = value.getUnsafeBytes();
        int save = -1;

        while (s < send) {
            int c = bytes[s++] & 0xff;
            if (c != save || !squeeze[c]) bytes[t++] = (byte)(save = c);
        }

        if (t - value.getBegin() != value.getRealSize()) { // modified
            value.setRealSize(t - value.getBegin());
            return this;
        }

        return runtime.getNil();        
    }

    @JRubyMethod(name = "squeeze", compat = RUBY1_9)
    public IRubyObject squeeze19(ThreadContext context) {
        RubyString str = strDup(context.getRuntime());
        str.squeeze_bang19(context);
        return str;
    }

    @JRubyMethod(name = "squeeze", compat = RUBY1_9)
    public IRubyObject squeeze19(ThreadContext context, IRubyObject arg) {
        RubyString str = strDup(context.getRuntime());
        str.squeeze_bang19(context, arg);
        return str;
    }

    @JRubyMethod(name = "squeeze", rest = true, compat = RUBY1_9)
    public IRubyObject squeeze19(ThreadContext context, IRubyObject[] args) {
        RubyString str = strDup(context.getRuntime());
        str.squeeze_bang19(context, args);
        return str;
    }

    @JRubyMethod(name = "squeeze!", compat = RUBY1_9)
    public IRubyObject squeeze_bang19(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) {
            modifyCheck();
            return runtime.getNil();
        }
        final boolean squeeze[] = new boolean[TRANS_SIZE];
        for (int i=0; i<TRANS_SIZE; i++) squeeze[i] = true;

        modifyAndKeepCodeRange();
        if (singleByteOptimizable()) {
            return squeezeCommon(runtime, squeeze); // 1.8
        } else {
            return squeezeCommon19(runtime, squeeze, null, value.getEncoding(), false);
        }
    }

    @JRubyMethod(name = "squeeze!", compat = RUBY1_9)
    public IRubyObject squeeze_bang19(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) {
            modifyCheck();
            return runtime.getNil();
        }

        RubyString otherStr = arg.convertToString();
        final boolean squeeze[] = new boolean[TRANS_SIZE + 1];
        TrTables tables = otherStr.trSetupTable(runtime, squeeze, null, true, checkEncoding(otherStr));

        modifyAndKeepCodeRange();
        if (singleByteOptimizable() && otherStr.singleByteOptimizable()) {
            return squeezeCommon(runtime, squeeze); // 1.8
        } else {
            return squeezeCommon19(runtime, squeeze, tables, value.getEncoding(), true);
        }
        
    }

    @JRubyMethod(name = "squeeze!", rest = true, compat = RUBY1_9)
    public IRubyObject squeeze_bang19(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) {
            modifyCheck();
            return runtime.getNil();
        }

        RubyString otherStr = args[0].convertToString();
        Encoding enc = checkEncoding(otherStr);
        final boolean squeeze[] = new boolean[TRANS_SIZE + 1];
        TrTables tables = otherStr.trSetupTable(runtime, squeeze, null, true, enc);

        boolean singlebyte = singleByteOptimizable() && otherStr.singleByteOptimizable();
        for (int i=1; i<args.length; i++) {
            otherStr = args[i].convertToString();
            enc = checkEncoding(otherStr);
            singlebyte = singlebyte && otherStr.singleByteOptimizable();
            tables = otherStr.trSetupTable(runtime, squeeze, tables, false, enc);
        }

        modifyAndKeepCodeRange();
        if (singlebyte) {
            return squeezeCommon(runtime, squeeze); // 1.8
        } else {
            return squeezeCommon19(runtime, squeeze, tables, enc, true);
        }
    }

    private IRubyObject squeezeCommon19(Ruby runtime, boolean squeeze[], TrTables tables, Encoding enc, boolean isArg) {
        int s = value.getBegin();
        int t = s;
        int send = s + value.getRealSize();
        byte[]bytes = value.getUnsafeBytes();
        int save = -1;
        int c;

        while (s < send) {
            if (enc.isAsciiCompatible() && (c = bytes[s] & 0xff) < 0x80) {
                if (c != save || (isArg && !squeeze[c])) bytes[t++] = (byte)(save = c);
                s++;
            } else {
                c = codePoint(runtime, enc, bytes, s, send);
                int cl = codeLength(runtime, enc, c);
                if (c != save || (isArg && !trFind(c, squeeze, tables))) {
                    if (t != s) enc.codeToMbc(c, bytes, t);
                    save = c;
                    t += cl;
                }
                s += cl;
            }
        }

        if (t - value.getBegin() != value.getRealSize()) { // modified
            value.setRealSize(t - value.getBegin());
            return this;
        }

        return runtime.getNil();   
    }

    /** rb_str_tr / rb_str_tr_bang
     *
     */
    @JRubyMethod(name = "tr", compat = RUBY1_8)
    public IRubyObject tr(ThreadContext context, IRubyObject src, IRubyObject repl) {
        RubyString str = strDup(context.getRuntime());
        str.trTrans(context, src, repl, false);
        return str;
    }

    @JRubyMethod(name = "tr!", compat = RUBY1_8)
    public IRubyObject tr_bang(ThreadContext context, IRubyObject src, IRubyObject repl) {
        return trTrans(context, src, repl, false);
    }    

    @JRubyMethod(name = "tr", compat = RUBY1_9)
    public IRubyObject tr19(ThreadContext context, IRubyObject src, IRubyObject repl) {
        RubyString str = strDup(context.getRuntime());
        str.trTrans19(context, src, repl, false);
        return str;
    }

    @JRubyMethod(name = "tr!")
    public IRubyObject tr_bang19(ThreadContext context, IRubyObject src, IRubyObject repl) {
        return trTrans19(context, src, repl, false);
    }    

    private static final class TR {
        TR(ByteList bytes) {
            p = bytes.getBegin();
            pend = bytes.getRealSize() + p;
            buf = bytes.getUnsafeBytes();
            now = max = 0;
            gen = false;
        }

        int p, pend, now, max;
        boolean gen;
        byte[]buf;
    }

    private static final int TRANS_SIZE = 256;

    /** tr_setup_table
     * 
     */
    private void trSetupTable(boolean[]table, boolean init) {
        final TR tr = new TR(value);
        boolean cflag = false;
        if (value.getRealSize() > 1 && value.getUnsafeBytes()[value.getBegin()] == '^') {
            cflag = true;
            tr.p++;
        }

        if (init) for (int i=0; i<TRANS_SIZE; i++) table[i] = true;

        final boolean[]buf = new boolean[TRANS_SIZE];
        for (int i=0; i<TRANS_SIZE; i++) buf[i] = cflag;

        int c;
        while ((c = trNext(tr)) >= 0) buf[c & 0xff] = !cflag;
        for (int i=0; i<TRANS_SIZE; i++) table[i] = table[i] && buf[i];
    }

    private static final class TrTables {
        private IntHash<IRubyObject> del, noDel;
    }

    private TrTables trSetupTable(Ruby runtime, boolean[]table, TrTables tables, boolean init, Encoding enc) {
        final TR tr = new TR(value);
        boolean cflag = false;
        if (value.getRealSize() > 1) {
            if (enc.isAsciiCompatible()) {
                if ((value.getUnsafeBytes()[value.getBegin()] & 0xff) == '^') {
                    cflag = true;
                    tr.p++;
                }
            } else {
                int l = StringSupport.preciseLength(enc, tr.buf, tr.p, tr.pend);
                if (enc.mbcToCode(tr.buf, tr.p, tr.pend) == '^') {
                    cflag = true;
                    tr.p += l;
                }
            }
        }

        if (init) {
            for (int i=0; i<TRANS_SIZE; i++) table[i] = true;
            table[TRANS_SIZE] = cflag;
        } else if (table[TRANS_SIZE] && !cflag) {
            table[TRANS_SIZE] = false;
        }
        
        final boolean[]buf = new boolean[TRANS_SIZE];
        for (int i=0; i<TRANS_SIZE; i++) buf[i] = cflag;

        int c;
        IntHash<IRubyObject> hash = null, phash = null;
        while ((c = trNext(tr, runtime, enc)) >= 0) {
            if (c < TRANS_SIZE) {
                buf[c & 0xff] = !cflag;
            } else {
                if (hash == null) {
                    hash = new IntHash<IRubyObject>();
                    if (tables == null) tables = new TrTables();
                    if (cflag) {
                        phash = tables.noDel;
                        tables.noDel = hash;
                    } else {
                        phash  = tables.del;
                        tables.del = hash;
                    }
                }
                if (phash == null || phash.get(c) != null) hash.put(c, NEVER);
            }
        }

        for (int i=0; i<TRANS_SIZE; i++) table[i] = table[i] && buf[i];
        return tables;
    }

    private boolean trFind(int c, boolean[]table, TrTables tables) {
        if (c < TRANS_SIZE) {
            return table[c];
        } else {
            if (tables != null) {
                if (tables.del != null) {
                    if (tables.noDel == null || tables.noDel.get(c) == null) return true;
                } else if (tables.noDel != null && tables.noDel.get(c) != null) return false;
            }
            return table[TRANS_SIZE];
        }
    }

    /** tr_trans
    *
    */    
    private IRubyObject trTrans(ThreadContext context, IRubyObject src, IRubyObject repl, boolean sflag) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) return runtime.getNil();

        ByteList replList = repl.convertToString().value;
        if (replList.getRealSize() == 0) return delete_bang(context, src);

        ByteList srcList = src.convertToString().value;
        final TR trSrc = new TR(srcList);
        boolean cflag = false;
        if (srcList.getRealSize() >= 2 && srcList.getUnsafeBytes()[srcList.getBegin()] == '^') {
            cflag = true;
            trSrc.p++;
        }       

        int c;
        final int[]trans = new int[TRANS_SIZE];
        final TR trRepl = new TR(replList);
        if (cflag) {
            for (int i=0; i<TRANS_SIZE; i++) trans[i] = 1;
            while ((c = trNext(trSrc)) >= 0) trans[c & 0xff] = -1;
            while ((c = trNext(trRepl)) >= 0) {}
            for (int i=0; i<TRANS_SIZE; i++) {
                if (trans[i] >= 0) trans[i] = trRepl.now;
            }
        } else {
            for (int i=0; i<TRANS_SIZE; i++) trans[i] = -1;
            while ((c = trNext(trSrc)) >= 0) {
                int r = trNext(trRepl);
                if (r == -1) r = trRepl.now;
                trans[c & 0xff] = r;
            }
        }

        modify();

        int s = value.getBegin();
        int send = s + value.getRealSize();
        byte sbytes[] = value.getUnsafeBytes();
        boolean modify = false;
        if (sflag) {
            int t = s;
            int last = -1;
            while (s < send) {
                int c0 = sbytes[s++];
                if ((c = trans[c0 & 0xff]) >= 0) {
                    if (last == c) continue;
                    last = c;
                    sbytes[t++] = (byte)(c & 0xff);
                    modify = true;
                } else {
                    last = -1;
                    sbytes[t++] = (byte)c0;
                }
            }

            if (value.getRealSize() > (t - value.getBegin())) {
                value.setRealSize(t - value.getBegin());
                modify = true;
            }
        } else {
            while (s < send) {
                if ((c = trans[sbytes[s] & 0xff]) >= 0) {
                    sbytes[s] = (byte)(c & 0xff);
                    modify = true;
                }
                s++;
            }
        }

        return modify ? this : runtime.getNil();
    }

    private IRubyObject trTrans19(ThreadContext context, IRubyObject src, IRubyObject repl, boolean sflag) {
        Ruby runtime = context.getRuntime();
        if (value.getRealSize() == 0) return runtime.getNil();

        RubyString replStr = repl.convertToString();
        ByteList replList = replStr.value;
        if (replList.getRealSize() == 0) return delete_bang19(context, src);

        RubyString srcStr = src.convertToString();
        ByteList srcList = srcStr.value;
        Encoding e1 = checkEncoding(srcStr);
        Encoding e2 = checkEncoding(replStr);
        Encoding enc = e1 == e2 ? e1 : srcStr.checkEncoding(replStr);

        int cr = getCodeRange();

        final TR trSrc = new TR(srcList);
        boolean cflag = false;
        if (value.getRealSize() > 1) {
            if (enc.isAsciiCompatible()) {
                if (trSrc.buf.length > 0 && (trSrc.buf[trSrc.p] & 0xff) == '^' && trSrc.p + 1 < trSrc.pend) {
                    cflag = true;
                    trSrc.p++;
                }
            } else {
                int cl = StringSupport.preciseLength(enc, trSrc.buf, trSrc.p, trSrc.pend);
                if (enc.mbcToCode(trSrc.buf, trSrc.p, trSrc.pend) == '^' && trSrc.p + cl < trSrc.pend) {
                    cflag = true;
                    trSrc.p += cl;
                }
            }            
        }

        boolean singlebyte = singleByteOptimizable();

        int c;
        final int[]trans = new int[TRANS_SIZE];
        IntHash<Integer> hash = null;
        final TR trRepl = new TR(replList);

        int last = 0;        
        if (cflag) {
            for (int i=0; i<TRANS_SIZE; i++) trans[i] = 1;
            
            while ((c = trNext(trSrc, runtime, enc)) >= 0) {
                if (c < TRANS_SIZE) {
                    trans[c & 0xff] = -1;
                } else {
                    if (hash == null) hash = new IntHash<Integer>();
                    hash.put(c, 1); // QTRUE
                }
            }
            while ((c = trNext(trRepl, runtime, enc)) >= 0) {}  /* retrieve last replacer */
            last = trRepl.now;
            for (int i=0; i<TRANS_SIZE; i++) {
                if (trans[i] >= 0) trans[i] = last;
            }
        } else {
            for (int i=0; i<TRANS_SIZE; i++) trans[i] = -1;

            while ((c = trNext(trSrc, runtime, enc)) >= 0) {
                int r = trNext(trRepl, runtime, enc);
                if (r == -1) r = trRepl.now;
                if (c < TRANS_SIZE) {
                    trans[c] = r;
                    if (codeLength(runtime, enc, r) != 1) singlebyte = false;
                } else {
                    if (hash == null) hash = new IntHash<Integer>();
                    hash.put(c, r);
                }
            }
        }

        if (cr == CR_VALID) cr = CR_7BIT;
        modifyAndKeepCodeRange();
        int s = value.getBegin();
        int send = s + value.getRealSize();
        byte sbytes[] = value.getUnsafeBytes();
        int max = value.getRealSize();
        boolean modify = false;

        int clen, tlen, c0;

        if (sflag) {
            int save = -1;
            byte[]buf = new byte[max];
            int t = 0;
            while (s < send) {
                boolean mayModify = false;
                c0 = c = codePoint(runtime, e1, sbytes, s, send);
                clen = codeLength(runtime, e1, c);
                tlen = enc == e1 ? clen : codeLength(runtime, enc, c);
                s += clen;

                c = trCode(c, trans, hash, cflag, last, false);
                if (c != -1) {
                    if (save == c) {
                        if (cr == CR_7BIT && !Encoding.isAscii(c)) cr = CR_VALID;
                        continue;
                    }
                    save = c;
                    tlen = codeLength(runtime, enc, c);
                    modify = true;
                } else {
                    save = -1;
                    c = c0;
                    if (enc != e1) mayModify = true;
                }

                while (t + tlen >= max) {
                    max <<= 1;
                    byte[]tbuf = new byte[max];
                    System.arraycopy(buf, 0, tbuf, 0, buf.length);
                    buf = tbuf;
                }
                enc.codeToMbc(c, buf, t);
                if (mayModify && (tlen == 1 ? sbytes[s] != buf[t] : ByteList.memcmp(sbytes, s, buf, t, tlen) != 0)) modify = true;
                if (cr == CR_7BIT && !Encoding.isAscii(c)) cr = CR_VALID;
                t += tlen;
            }
            value.setUnsafeBytes(buf);
            value.setRealSize(t);
        } else if (enc.isSingleByte() || (singlebyte && hash == null)) {
            while (s < send) {
                c = sbytes[s] & 0xff;
                if (trans[c] != -1) {
                    if (!cflag) {
                        c = trans[c];
                        sbytes[s] = (byte)c;
                    } else {
                        sbytes[s] = (byte)last;
                    }
                    modify = true;
                }
                if (cr == CR_7BIT && !Encoding.isAscii(c)) cr = CR_VALID;
                s++;
            }
        } else {
            max += max >> 1;
            byte[]buf = new byte[max];
            int t = 0;

            while (s < send) {
                boolean mayModify = false;
                c0 = c = codePoint(runtime, e1, sbytes, s, send);
                clen = codeLength(runtime, e1, c);
                tlen = enc == e1 ? clen : codeLength(runtime, enc, c);

                c = trCode(c, trans, hash, cflag, last, true);

                if (c != -1) {
                    tlen = codeLength(runtime, enc, c);
                    modify = true;
                } else {
                    c = c0;
                    if (enc != e1) mayModify = true;
                }
                while (t + tlen >= max) {
                    max <<= 1;
                    byte[]tbuf = new byte[max];
                    System.arraycopy(buf, 0, tbuf, 0, buf.length);
                    buf = tbuf;
                }
                enc.codeToMbc(c, buf, t);
                if (mayModify && (tlen == 1 ? sbytes[s] != buf[t] : ByteList.memcmp(sbytes, s, buf, t, tlen) != 0)) modify = true;  

                if (cr == CR_7BIT && !Encoding.isAscii(c)) cr = CR_VALID;
                s += clen;
                t += tlen;
            }
            value.setUnsafeBytes(buf);
            value.setRealSize(t);
        }

        if (modify) {
            if (cr != CR_BROKEN) setCodeRange(cr);
            associateEncoding(enc);
            return this;
        }
        return runtime.getNil();
    }

    private int trCode(int c, int[]trans, IntHash<Integer> hash, boolean cflag, int last, boolean set) {
        if (c < TRANS_SIZE) {
            return trans[c];
        } else if (hash != null) {
            Integer tmp = hash.get(c);
            if (tmp == null) {
                return cflag ? last : -1;
            } else {
                return cflag ? -1 : tmp;
            }
        } else {
            return cflag && set ? last : -1; 
        }
    }

    /** trnext
    *
    */    
    private int trNext(TR t) {
        byte[]buf = t.buf;
        
        for (;;) {
            if (!t.gen) {
                if (t.p == t.pend) return -1;
                if (t.p < t.pend -1 && buf[t.p] == '\\') t.p++;
                t.now = buf[t.p++] & 0xff;
                if (t.p < t.pend - 1 && buf[t.p] == '-') {
                    t.p++;
                    if (t.p < t.pend) {
                        if (t.now > (buf[t.p] & 0xff)) {
                            t.p++;
                            continue;
                        }
                        t.gen = true;
                        t.max = buf[t.p++] & 0xff;
                    }
                }
                return t.now;
            } else if (++t.now < t.max) {
                return t.now;
            } else {
                t.gen = false;
                return t.max;
            }
        }
    }

    private int trNext(TR t, Ruby runtime, Encoding enc) {
        byte[]buf = t.buf;
        
        for (;;) {
            if (!t.gen) {
                if (t.p == t.pend) return -1;
                if (t.p < t.pend -1 && buf[t.p] == '\\') t.p++;
                t.now = codePoint(runtime, enc, buf, t.p, t.pend);
                t.p += codeLength(runtime, enc, t.now);
                if (t.p < t.pend - 1 && buf[t.p] == '-') {
                    t.p++;
                    if (t.p < t.pend) {
                        int c = codePoint(runtime, enc, buf, t.p, t.pend);
                        t.p += codeLength(runtime, enc, c);
                        if (t.now > c) {
                            if (t.now < 0x80 && c < 0x80) {
                                throw runtime.newArgumentError("invalid range \""
                                        + (char) t.now + "-" + (char) c + "\" in string transliteration");
                            }

                            throw runtime.newArgumentError("invalid range in string transliteration");
                        }
                        t.gen = true;
                        t.max = c;
                    }
                }
                return t.now;
            } else if (++t.now < t.max) {
                return t.now;
            } else {
                t.gen = false;
                return t.max;
            }
        }
    }

    /** rb_str_tr_s / rb_str_tr_s_bang
     *
     */
    @JRubyMethod(name ="tr_s", compat = RUBY1_8)
    public IRubyObject tr_s(ThreadContext context, IRubyObject src, IRubyObject repl) {
        RubyString str = strDup(context.getRuntime());
        str.trTrans(context, src, repl, true);
        return str;
    }

    @JRubyMethod(name = "tr_s!", compat = RUBY1_8)
    public IRubyObject tr_s_bang(ThreadContext context, IRubyObject src, IRubyObject repl) {
        return trTrans(context, src, repl, true);
    }

    @JRubyMethod(name ="tr_s", compat = RUBY1_9)
    public IRubyObject tr_s19(ThreadContext context, IRubyObject src, IRubyObject repl) {
        RubyString str = strDup(context.getRuntime());
        str.trTrans19(context, src, repl, true);
        return str;
    }

    @JRubyMethod(name = "tr_s!", compat = RUBY1_9)
    public IRubyObject tr_s_bang19(ThreadContext context, IRubyObject src, IRubyObject repl) {
        return trTrans19(context, src, repl, true);
    }

    /** rb_str_each_line
     *
     */
    public IRubyObject each_line(ThreadContext context, Block block) {
        return each_lineCommon(context, context.getRuntime().getGlobalVariables().get("$/"), block);
    }

    public IRubyObject each_line(ThreadContext context, IRubyObject arg, Block block) {
        return each_lineCommon(context, arg, block);
    }

    public IRubyObject each_lineCommon(ThreadContext context, IRubyObject sep, Block block) {        
        Ruby runtime = context.getRuntime();
        if (sep.isNil()) {
            block.yield(context, this);
            return this;
        }

        RubyString sepStr = sep.convertToString();
        ByteList sepValue = sepStr.value;
        int rslen = sepValue.getRealSize();

        final byte newline;
        if (rslen == 0) {
            newline = '\n';
        } else {
            newline = sepValue.getUnsafeBytes()[sepValue.getBegin() + rslen - 1];
        }

        int p = value.getBegin();
        int end = p + value.getRealSize();
        int ptr = p, s = p;
        int len = value.getRealSize();
        byte[] bytes = value.getUnsafeBytes();

        p += rslen;

        for (; p < end; p++) {
            if (rslen == 0 && bytes[p] == '\n') {
                if (++p == end || bytes[p] != '\n') continue;
                while(p < end && bytes[p] == '\n') p++;
            }
            if (ptr < p && bytes[p - 1] == newline &&
               (rslen <= 1 || 
                ByteList.memcmp(sepValue.getUnsafeBytes(), sepValue.getBegin(), rslen, bytes, p - rslen, rslen) == 0)) {
                block.yield(context, makeShared(runtime, s - ptr, p - s).infectBy(this));
                modifyCheck(bytes, len);
                s = p;
            }
        }

        if (s != end) {
            if (p > end) p = end;
            block.yield(context, makeShared(runtime, s - ptr, p - s).infectBy(this));
        }

        return this;
    }

    @JRubyMethod(name = "each", compat = RUBY1_8)
    public IRubyObject each18(ThreadContext context, Block block) {
        return block.isGiven() ? each_line(context, block) : 
            enumeratorize(context.getRuntime(), this, "each");
    }

    @JRubyMethod(name = "each", compat = RUBY1_8)
    public IRubyObject each18(ThreadContext context, IRubyObject arg, Block block) {
        return block.isGiven() ? each_lineCommon(context, arg, block) : 
            enumeratorize(context.getRuntime(), this, "each", arg);
    }

    @JRubyMethod(name = "each_line", compat = RUBY1_8)
    public IRubyObject each_line18(ThreadContext context, Block block) {
        return block.isGiven() ? each_line(context, block) : 
            enumeratorize(context.getRuntime(), this, "each_line");
    }

    @JRubyMethod(name = "each_line", compat = RUBY1_8)
    public IRubyObject each_line18(ThreadContext context, IRubyObject arg, Block block) {
        return block.isGiven() ? each_lineCommon(context, arg, block) : 
            enumeratorize(context.getRuntime(), this, "each_line", arg);
    }

    @JRubyMethod(name = "lines", compat = RUBY1_8)
    public IRubyObject lines18(ThreadContext context, Block block) {
        return block.isGiven() ? each_line(context, block) : 
            enumeratorize(context.getRuntime(), this, "lines");
    }

    @JRubyMethod(name = "lines", compat = RUBY1_8)
    public IRubyObject lines18(ThreadContext context, IRubyObject arg, Block block) {
        return block.isGiven() ? each_lineCommon(context, arg, block) : 
            enumeratorize(context.getRuntime(), this, "lines", arg);
    }

    @JRubyMethod(name = "each_line", compat = RUBY1_9)
    public IRubyObject each_line19(ThreadContext context, Block block) {
        return block.isGiven() ? each_lineCommon19(context, block) : 
            enumeratorize(context.getRuntime(), this, "each_line");
    }

    @JRubyMethod(name = "each_line", compat = RUBY1_9)
    public IRubyObject each_line19(ThreadContext context, IRubyObject arg, Block block) {
        return block.isGiven() ? each_lineCommon19(context, arg, block) : 
            enumeratorize(context.getRuntime(), this, "each_line", arg);
    }

    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject lines(ThreadContext context, Block block) {
        return block.isGiven() ? each_lineCommon19(context, block) : 
            enumeratorize(context.getRuntime(), this, "lines");
    }

    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject lines(ThreadContext context, IRubyObject arg, Block block) {
        return block.isGiven() ? each_lineCommon19(context, arg, block) : 
            enumeratorize(context.getRuntime(), this, "lines", arg);
    }

    private IRubyObject each_lineCommon19(ThreadContext context, Block block) {
        return each_lineCommon19(context, context.getRuntime().getGlobalVariables().get("$/"), block);
    }

    private IRubyObject each_lineCommon19(ThreadContext context, IRubyObject sep, Block block) {        
        Ruby runtime = context.getRuntime();
        if (sep.isNil()) {
            block.yield(context, this);
            return this;
        }
        if (! sep.respondsTo("to_str")) {
            throw runtime.newTypeError("can't convert " + sep.getMetaClass() + " into String");
        }

        ByteList val = value.shallowDup();
        int p = val.getBegin();
        int s = p;
        int offset = p;
        int len = val.getRealSize();
        int end = p + len;
        byte[]bytes = val.getUnsafeBytes();

        final Encoding enc;
        RubyString sepStr = sep.convertToString();
        if (sepStr == runtime.getGlobalVariables().getDefaultSeparator()) {
            enc = val.getEncoding();
            while (p < end) {
                if (bytes[p] == (byte)'\n') {
                    int p0 = enc.leftAdjustCharHead(bytes, s, p, end);
                    if (enc.isNewLine(bytes, p0, end)) {
                        p = p0 + StringSupport.length(enc, bytes, p0, end);
                        block.yield(context, makeShared19(runtime, val, s - offset, p - s).infectBy(this));
                        s = p;
                        continue;
                    }
                }
                p++;
            }
        } else {
            enc = checkEncoding(sepStr);
            ByteList sepValue = sepStr.value;
            final int newLine;
            int rslen = sepValue.getRealSize();
            if (rslen == 0) {
                newLine = '\n';
            } else {
                newLine = codePoint(runtime, enc, sepValue.getUnsafeBytes(), sepValue.getBegin(), sepValue.getBegin() + sepValue.getRealSize());
            }

            while (p < end) {
                int c = codePoint(runtime, enc, bytes, p, end);
                again: do {
                    int n = codeLength(runtime, enc, c);
                    if (rslen == 0 && c == newLine) {
                        p += n;
                        if (p < end && (c = codePoint(runtime, enc, bytes, p, end)) != newLine) continue again;
                        while (p < end && codePoint(runtime, enc, bytes, p, end) == newLine) p += n;
                        p -= n;
                    }
                    if (c == newLine && (rslen <= 1 ||
                            ByteList.memcmp(sepValue.getUnsafeBytes(), sepValue.getBegin(), rslen, bytes, p, rslen) == 0)) {
                        block.yield(context, makeShared19(runtime, val, s - offset, p - s + (rslen != 0 ? rslen : n)).infectBy(this));
                        s = p + (rslen != 0 ? rslen : n);
                    }
                    p += n;
                } while (false);
            }
        }

        if (s != end) {
            block.yield(context, makeShared19(runtime, val, s-offset, end - s).infectBy(this));
        }
        return this;
    }

    /**
     * rb_str_each_byte
     */
    public RubyString each_byte(ThreadContext context, Block block) {
        Ruby runtime = context.getRuntime();
        // Check the length every iteration, since
        // the block can modify this string.
        for (int i = 0; i < value.length(); i++) {
            block.yield(context, runtime.newFixnum(value.get(i) & 0xFF));
        }
        return this;
    }

    @JRubyMethod(name = "each_byte")
    public IRubyObject each_byte19(ThreadContext context, Block block) {
        return block.isGiven() ? each_byte(context, block) : enumeratorize(context.getRuntime(), this, "each_byte");
    }

    @JRubyMethod
    public IRubyObject bytes(ThreadContext context, Block block) {
        return block.isGiven() ? each_byte(context, block) : enumeratorize(context.getRuntime(), this, "bytes");
    }

    /** rb_str_each_char
     * 
     */
    @JRubyMethod(name = "each_char", compat = RUBY1_8)
    public IRubyObject each_char18(ThreadContext context, Block block) {
        return block.isGiven() ? each_charCommon18(context, block) : enumeratorize(context.getRuntime(), this, "each_char");
    }

    @JRubyMethod(name = "chars", compat = RUBY1_8)
    public IRubyObject chars18(ThreadContext context, Block block) {
        return block.isGiven() ? each_charCommon18(context, block) : enumeratorize(context.getRuntime(), this, "chars");
    }

    private IRubyObject each_charCommon18(ThreadContext context, Block block) {
        byte bytes[] = value.getUnsafeBytes();
        int p = value.getBegin();
        int end = p + value.getRealSize();

        Ruby runtime = context.getRuntime();
        Encoding enc = runtime.getKCode().getEncoding();
        ByteList val = value.shallowDup();
        while (p < end) {
            int n = StringSupport.length(enc, bytes, p, end);
            block.yield(context, makeShared19(runtime, val, p-val.getBegin(), n));
            p += n;
        }
        return this;
    }

    @JRubyMethod(name = "each_char", compat = RUBY1_9)
    public IRubyObject each_char19(ThreadContext context, Block block) {
        return block.isGiven() ? each_charCommon19(context, block) : enumeratorize(context.getRuntime(), this, "each_char");
    }

    @JRubyMethod(name = "chars", compat = RUBY1_9)
    public IRubyObject chars19(ThreadContext context, Block block) {
        return block.isGiven() ? each_charCommon19(context, block) : enumeratorize(context.getRuntime(), this, "chars");
    }

    private IRubyObject each_charCommon19(ThreadContext context, Block block) {
        byte bytes[] = value.getUnsafeBytes();
        int p = value.getBegin();
        int end = p + value.getRealSize();
        Encoding enc = value.getEncoding();

        Ruby runtime = context.getRuntime();
        ByteList val = value.shallowDup();
        while (p < end) {
            int n = StringSupport.length(enc, bytes, p, end);
            block.yield(context, makeShared19(runtime, val, p-value.getBegin(), n));
            p += n;
        }
        return this;
    }

    /** rb_str_each_codepoint
     * 
     */
    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject each_codepoint(ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorize(context.getRuntime(), this, "each_codepoint");
        return singleByteOptimizable() ? each_byte(context, block) : each_codepointCommon(context, block);
    }

    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject codepoints(ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorize(context.getRuntime(), this, "codepoints");
        return singleByteOptimizable() ? each_byte(context, block) : each_codepointCommon(context, block);
    }

    private IRubyObject each_codepointCommon(ThreadContext context, Block block) {
        Ruby runtime = context.getRuntime();
        byte bytes[] = value.getUnsafeBytes();
        int p = value.getBegin();
        int end = p + value.getRealSize();
        Encoding enc = value.getEncoding();

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
    private RubySymbol to_sym() {
        RubySymbol symbol = getRuntime().getSymbolTable().getSymbol(value);
        if (symbol.getBytes() == value) shareLevel = SHARE_LEVEL_BYTELIST;
        return symbol;
    }

    @JRubyMethod(name = {"to_sym", "intern"}, compat = RUBY1_8)
    public RubySymbol intern() {
        if (value.getRealSize() == 0) throw getRuntime().newArgumentError("interning empty string");
        for (int i = 0; i < value.getRealSize(); i++) {
            if (value.getUnsafeBytes()[value.getBegin() + i] == 0) throw getRuntime().newArgumentError("symbol string may not contain '\\0'");
        }
        return to_sym();
    }

    @JRubyMethod(name = {"to_sym", "intern"}, compat = RUBY1_9)
    public RubySymbol intern19() {
        return to_sym();
    }

    @JRubyMethod(name = "ord", compat = RUBY1_9)
    public IRubyObject ord(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        return RubyFixnum.newFixnum(runtime, codePoint(runtime, value.getEncoding(), value.getUnsafeBytes(), value.getBegin(),
                                                                value.getBegin() + value.getRealSize()));
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

        byte[]bytes = value.getUnsafeBytes();
        int p = value.getBegin();
        int len = value.getRealSize();
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
    @JRubyMethod(name = "to_c", reads = BACKREF, writes = BACKREF, compat = RUBY1_9)
    public IRubyObject to_c(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        DynamicScope scope = context.getCurrentScope();
        IRubyObject backref = scope.getBackRef(runtime);
        if (backref instanceof RubyMatchData) ((RubyMatchData)backref).use();

        IRubyObject s = RuntimeHelpers.invoke(
                context, this, "gsub",
                RubyRegexp.newDummyRegexp(runtime, Numeric.ComplexPatterns.underscores_pat),
                runtime.newString(new ByteList(new byte[]{'_'})));

        RubyArray a = RubyComplex.str_to_c_internal(context, s);

        scope.setBackRef(backref);

        if (!a.eltInternal(0).isNil()) {
            return a.eltInternal(0);
        } else {
            return RubyComplex.newComplexCanonicalize(context, RubyFixnum.zero(runtime));
        }
    }

    /** string_to_r
     * 
     */
    @JRubyMethod(name = "to_r", reads = BACKREF, writes = BACKREF, compat = RUBY1_9)
    public IRubyObject to_r(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        DynamicScope scope = context.getCurrentScope();
        IRubyObject backref = scope.getBackRef(runtime);
        if (backref instanceof RubyMatchData) ((RubyMatchData)backref).use();

        IRubyObject s = RuntimeHelpers.invoke(
                context, this, "gsub",
                RubyRegexp.newDummyRegexp(runtime, Numeric.ComplexPatterns.underscores_pat),
                runtime.newString(new ByteList(new byte[]{'_'})));

        RubyArray a = RubyRational.str_to_r_internal(context, s);

        scope.setBackRef(backref);

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
    @JRubyMethod(name = "unpack")
    public RubyArray unpack(IRubyObject obj) {
        return Pack.unpack(getRuntime(), this.value, stringValue(obj).value);
    }

    public void empty() {
        value = ByteList.EMPTY_BYTELIST;
        shareLevel = SHARE_LEVEL_BYTELIST;
    }

    @JRubyMethod(name = "encoding", compat = RUBY1_9)
    public IRubyObject encoding(ThreadContext context) {
        return context.getRuntime().getEncodingService().getEncoding(value.getEncoding());
    }

    @JRubyMethod(name = "encode!", compat = RUBY1_9)
    public IRubyObject encode_bang(ThreadContext context) {
        modify19();
        Ruby runtime = context.getRuntime();
        Encoding defaultInternal = runtime.getDefaultInternalEncoding();

        if (defaultInternal == null) return dup();

        value = transcode(context, value, null, defaultInternal, runtime.getNil());

        return this;
    }

    @JRubyMethod(name = "encode!", compat = RUBY1_9)
    public IRubyObject encode_bang(ThreadContext context, IRubyObject enc) {
        Ruby runtime = context.getRuntime();
        modify19();

        value = transcode(context, value, null, getEncoding(runtime, enc), runtime.getNil());

        return this;
    }

    @JRubyMethod(name = "encode!", compat = RUBY1_9)
    public IRubyObject encode_bang(ThreadContext context, IRubyObject toEncoding, IRubyObject arg) {
        Ruby runtime = context.getRuntime();
        modify19();
        Encoding forceEncoding;
        IRubyObject options;
        
        if (arg instanceof RubyHash) {
            forceEncoding = null;
            options = arg;
        } else {
            forceEncoding = getEncoding(runtime, arg);
            options = runtime.getNil();
        }

        value = transcode(context, value, forceEncoding, getEncoding(runtime, toEncoding), options);

        return this;
    }

    @JRubyMethod(name = "encode!", compat = RUBY1_9)
    public IRubyObject encode_bang(ThreadContext context, IRubyObject toEncoding, IRubyObject forceEncoding, IRubyObject opts) {
        Ruby runtime = context.getRuntime();
        modify19();

        value = transcode(context, value, getEncoding(runtime, forceEncoding),
                getEncoding(runtime, toEncoding), opts);

        return this;
    }

    @JRubyMethod(name = "encode", compat = RUBY1_9)
    public IRubyObject encode(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        Encoding defaultInternal = runtime.getDefaultInternalEncoding();

        if (defaultInternal == null) return dup();

        return runtime.newString(transcode(context, value, null, defaultInternal, runtime.getNil()));
    }

    @JRubyMethod(name = "encode", compat = RUBY1_9)
    public IRubyObject encode(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.getRuntime();
        Encoding forceEncoding;
        IRubyObject options;

        if (arg instanceof RubyHash) {
            forceEncoding = runtime.getDefaultInternalEncoding();
            if (forceEncoding == null) forceEncoding = runtime.getEncodingService().getLocaleEncoding();
            if (forceEncoding == null) return dup();
            options = arg;
        } else {
            forceEncoding = getEncoding(runtime, arg);
            options = runtime.getNil();
        }

        return runtime.newString(transcode(context, value, null, forceEncoding, options));
    }

    @JRubyMethod(name = "encode", compat = RUBY1_9)
    public IRubyObject encode(ThreadContext context, IRubyObject toEncoding, IRubyObject arg) {
        Ruby runtime = context.getRuntime();
        Encoding forceEncoding;
        IRubyObject options;

        if (arg instanceof RubyHash) {
            forceEncoding = null;
            options = arg;
        } else {
            forceEncoding = getEncoding(runtime, arg);
            options = runtime.getNil();
        }

        return runtime.newString(transcode(context, value, forceEncoding,
                getEncoding(runtime, toEncoding), options));
    }

    @JRubyMethod(name = "encode", compat = RUBY1_9)
    public IRubyObject encode(ThreadContext context, IRubyObject toEncoding,
            IRubyObject forcedEncoding, IRubyObject opts) {
        Ruby runtime = context.getRuntime();

        return runtime.newString(transcode(context, value, getEncoding(runtime, forcedEncoding),
                getEncoding(runtime, toEncoding), opts));
    }

    // Java seems to find these specific Java charsets but they seem to trancode
    // some strings a little differently than MRI.  Since Java Charset transcoding
    // is a temporary implementation for us, having this gruesome hack is ok
    // for the time being.
    private static Set<String> BAD_TRANSCODINGS_HACK = new HashSet<String>() {{
        add("ISO-2022-JP-2");
        add("CP50220");
        add("CP50221");
    }};

    private static Charset transcodeCharsetFor(Ruby runtime, Encoding encoding, String fromName, String toName) {
        Charset from = null;
        String realEncodingName = new String(encoding.getName());
        
        // Doing a manual forName over and over sucks, but this is only meant
        // to be a transitional impl.  The reason for this extra mechanism is 
        // that jcodings is representing these encodings with an alias.  So,
        // for example, IBM866 ends up being associated with ISO-8859-1 which
        // will not know how to trancsode higher than ascii values properly.
        if (!realEncodingName.equals(encoding.getCharsetName()) && !BAD_TRANSCODINGS_HACK.contains(realEncodingName)) {
            try {
                from = Charset.forName(realEncodingName);
                if (from != null) {
                    return from;
                }
            } catch (Exception e) {}
        }
        try {
            from = encoding.getCharset();
            if (from != null) return from;
        } catch (Exception e) {}

        try { // We try looking up based on Java's supported charsets...likely missing charset entry in jcodings
            from = Charset.forName(encoding.toString());
        } catch (Exception e) {}
        
        if (from == null) throw runtime.newConverterNotFoundError("code converter not found (" +
                fromName + " to " + toName + ")");

        return from;
    }

    /*
     * This will try and transcode the supplied ByteList to the supplied toEncoding.  It will use
     * forceEncoding as its encoding if it is supplied; otherwise it will use the encoding it has
     * tucked away in the bytelist.  This will return a new copy of a ByteList in the request
     * encoding or die trying (ConverterNotFound).
     * 
     * c: rb_str_conv_enc_opts
     */
    public static ByteList transcode(ThreadContext context, ByteList value, Encoding forceEncoding,
            Encoding toEncoding, IRubyObject opts) {
        if (toEncoding == null) return value;
        Ruby runtime = context.getRuntime();
        Encoding fromEncoding = forceEncoding != null ? forceEncoding : value.getEncoding();

        String fromName = fromEncoding.toString();
        String toName = toEncoding.toString();

        Charset from = transcodeCharsetFor(runtime, fromEncoding, fromEncoding.toString(), toEncoding.toString());
        Charset to = transcodeCharsetFor(runtime, toEncoding, fromEncoding.toString(), toEncoding.toString());

        CharsetEncoder encoder = getCharsetEncoder(context, to, opts);
        CharsetDecoder decoder = getCharsetDecoder(context, from, opts);

        ByteBuffer fromBytes = ByteBuffer.wrap(value.getUnsafeBytes(), value.begin(), value.length());

        // MRI does not allow ASCII-8BIT chars > 127 to transcode to multibyte encodings
        if (fromName.equals("ASCII-8BIT") && encoder.maxBytesPerChar() > 1.0) {
            for (byte b : fromBytes.array()) {
                if ((b & 0xFF) > 0x7F) {
                    throw runtime.newUndefinedConversionError(
                            "\"\\x" + Integer.toHexString(b & 0xFF).toUpperCase() +
                                    "\" from " + fromName +
                                    " to " + toName);
                }
            }
        }
        
        try {
            ByteBuffer toBytes = encoder.encode(decoder.decode(fromBytes));

            // CharsetEncoder#encode guarantees a newly-allocated buffer, so no need to copy.
            return new ByteList(toBytes.array(), toBytes.arrayOffset(),
                    toBytes.limit() - toBytes.arrayOffset(), toEncoding, false);
        } catch (CharacterCodingException e) {
            throw runtime.newUndefinedConversionError(e.getLocalizedMessage());
        }
    }

    private static CharsetDecoder getCharsetDecoder(ThreadContext context, Charset charset, IRubyObject opts) {
        CharsetDecoder decoder = charset.newDecoder();

        CodingErrorActions actions = getCodingErrorActions(context, opts);
        decoder.onUnmappableCharacter(actions.onUnmappableCharacter);
        decoder.onMalformedInput(actions.onMalformedInput);
        if (actions.replaceWith != null) {
            decoder.replaceWith(actions.replaceWith.toString());
        }

        return decoder;
    }

    private static CharsetEncoder getCharsetEncoder(ThreadContext context, Charset charset, IRubyObject opts) {
        CharsetEncoder encoder = charset.newEncoder();

        CodingErrorActions actions = getCodingErrorActions(context, opts);
        encoder.onUnmappableCharacter(actions.onUnmappableCharacter);
        encoder.onMalformedInput(actions.onMalformedInput);
        if (actions.replaceWith != null) {
            encoder.replaceWith(actions.replaceWith.getBytes());
        }

        return encoder;
    }

    private static class CodingErrorActions {
        final CodingErrorAction onUnmappableCharacter;
        final CodingErrorAction onMalformedInput;
        final RubyString replaceWith;

        CodingErrorActions(
                CodingErrorAction onUnmappableCharacter,
                CodingErrorAction onMalformedInput,
                RubyString replaceWith) {
            this.onUnmappableCharacter = onUnmappableCharacter;
            this.onMalformedInput = onMalformedInput;
            this.replaceWith = replaceWith;
        }
    }

    private static CodingErrorActions getCodingErrorActions(ThreadContext context, IRubyObject opts) {
        if (opts.isNil()) {
            return new CodingErrorActions(
                    CodingErrorAction.REPORT,
                    CodingErrorAction.REPORT,
                    null);
        } else {
            Ruby runtime = context.runtime;
            RubyHash hash = (RubyHash) opts;
            CodingErrorAction onMalformedInput = CodingErrorAction.REPORT;
            CodingErrorAction onUnmappableCharacter = CodingErrorAction.REPORT;
            RubyString replaceWith = null;
            
            IRubyObject replace = hash.fastARef(runtime.newSymbol("replace"));
            if (replace != null && !replace.isNil()) {
                RubyString replaceWithStr = replace.convertToString();
                if (replaceWithStr.size() == 1) { // we can only replaceWith a single char
                    replaceWith = replaceWithStr;
                }
            }
            
            IRubyObject invalid = hash.fastARef(runtime.newSymbol("invalid"));
            if (invalid != null && invalid.op_equal(context, runtime.newSymbol("replace")).isTrue()) {
                onMalformedInput = CodingErrorAction.REPLACE;
            }

            IRubyObject undef = hash.fastARef(runtime.newSymbol("undef"));
            if (undef != null && undef.op_equal(context, runtime.newSymbol("replace")).isTrue()) {
                onUnmappableCharacter = CodingErrorAction.REPLACE;
            }

            return new CodingErrorActions(
                    onUnmappableCharacter,
                    onMalformedInput,
                    replaceWith);

            /*
            Missing options from MRI 1.9.3 source:

 *  :replace ::
 *    Sets the replacement string to the given value. The default replacement
 *    string is "\uFFFD" for Unicode encoding forms, and "?" otherwise.
 *  :fallback ::
 *    Sets the replacement string by the given object for undefined
 *    character.  The object should be a Hash, a Proc, a Method, or an
 *    object which has [] method.
 *    Its key is an undefined character encoded in the source encoding
 *    of current transcoder. Its value can be any encoding until it
 *    can be converted into the destination encoding of the transcoder.
 *  :xml ::
 *    The value must be +:text+ or +:attr+.
 *    If the value is +:text+ #encode replaces undefined characters with their
 *    (upper-case hexadecimal) numeric character references. '&', '<', and '>'
 *    are converted to "&amp;", "&lt;", and "&gt;", respectively.
 *    If the value is +:attr+, #encode also quotes the replacement result
 *    (using '"'), and replaces '"' with "&quot;".
 *  :cr_newline ::
 *    Replaces LF ("\n") with CR ("\r") if value is true.
 *  :crlf_newline ::
 *    Replaces LF ("\n") with CRLF ("\r\n") if value is true.
 *  :universal_newline ::
 *    Replaces CRLF ("\r\n") and CR ("\r") with LF ("\n") if value is true.
 *    
             */
        }
    }

    private static Encoding getEncoding(Ruby runtime, IRubyObject toEnc) {
        try {
            return runtime.getEncodingService().getEncodingFromObject(toEnc);
        } catch (Exception e) {
            throw runtime.newConverterNotFoundError("code converter not found (" + toEnc.toString() + ")");
        }
    }

    @JRubyMethod(name = "force_encoding", compat = RUBY1_9)
    public IRubyObject force_encoding(ThreadContext context, IRubyObject enc) {
        modify19();
        Encoding encoding = context.runtime.getEncodingService().getEncodingFromObject(enc);
        associateEncoding(encoding);
        clearCodeRange();
        return this;
    }

    @JRubyMethod(name = "valid_encoding?", compat = RUBY1_9)
    public IRubyObject valid_encoding_p(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        return scanForCodeRange() == CR_BROKEN ? runtime.getFalse() : runtime.getTrue();
    }

    @JRubyMethod(name = "ascii_only?", compat = RUBY1_9)
    public IRubyObject ascii_only_p(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        return scanForCodeRange() == CR_7BIT ? runtime.getTrue() : runtime.getFalse();
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
        return RubyEncoding.decodeUTF8(value.getUnsafeBytes(), value.getBegin(), value.getRealSize());
    }

    @Override
    public Object toJava(Class target) {
        if (target.isAssignableFrom(String.class)) {
            return decodeString();
        } else if (target.isAssignableFrom(ByteList.class)) {
            return value;
        } else {
            return super.toJava(target);
        }
    }
}
