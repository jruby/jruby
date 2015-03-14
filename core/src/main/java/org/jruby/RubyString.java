/*
 **** BEGIN LICENSE BLOCK *****
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import jnr.posix.POSIX;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.ascii.AsciiTables;
import org.jcodings.exception.EncodingException;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF16BEEncoding;
import org.jcodings.specific.UTF16LEEncoding;
import org.jcodings.specific.UTF32BEEncoding;
import org.jcodings.specific.UTF32LEEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.util.CaseInsensitiveBytesHash;
import org.jcodings.util.IntHash;
import org.joni.Matcher;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Region;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.platform.Platform;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.runtime.encoding.MarshalEncoding;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.*;
import org.jruby.util.io.EncodingUtils;

import java.nio.charset.Charset;
import java.util.Locale;

import static org.jruby.RubyComparable.invcmp;
import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.anno.FrameField.BACKREF;
import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.runtime.invokedynamic.MethodNames.OP_EQUAL;
import static org.jruby.util.StringSupport.CR_7BIT;
import static org.jruby.util.StringSupport.CR_BROKEN;
import static org.jruby.util.StringSupport.CR_MASK;
import static org.jruby.util.StringSupport.CR_UNKNOWN;
import static org.jruby.util.StringSupport.CR_VALID;
import static org.jruby.util.StringSupport.MBCLEN_CHARFOUND_LEN;
import static org.jruby.util.StringSupport.MBCLEN_CHARFOUND_P;
import static org.jruby.util.StringSupport.MBCLEN_INVALID_P;
import static org.jruby.util.StringSupport.MBCLEN_NEEDMORE_P;
import static org.jruby.util.StringSupport.codeLength;
import static org.jruby.util.StringSupport.codePoint;
import static org.jruby.util.StringSupport.codeRangeScan;
import static org.jruby.util.StringSupport.searchNonAscii;
import static org.jruby.util.StringSupport.strLengthWithCodeRange;
import static org.jruby.util.StringSupport.toLower;
import static org.jruby.util.StringSupport.toUpper;
import static org.jruby.RubyEnumerator.SizeFn;

/**
 * Implementation of Ruby String class
 *
 * Concurrency: no synchronization is required among readers, but
 * all users must synchronize externally with writers.
 *
 */
@JRubyClass(name="String", include={"Enumerable", "Comparable"})
public class RubyString extends RubyObject implements EncodingCapable, MarshalEncoding, CodeRangeable {

    private static final ASCIIEncoding ASCII = ASCIIEncoding.INSTANCE;
    private static final UTF8Encoding UTF8 = UTF8Encoding.INSTANCE;
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    // string doesn't share any resources
    private static final int SHARE_LEVEL_NONE = 0;
    // string has it's own ByteList, but it's pointing to a shared buffer (byte[])
    private static final int SHARE_LEVEL_BUFFER = 1;
    // string doesn't have it's own ByteList (values)
    private static final int SHARE_LEVEL_BYTELIST = 2;

    private static final byte[] SCRUB_REPL_UTF8 = new byte[]{(byte)0xEF, (byte)0xBF, (byte)0xBD};
    private static final byte[] SCRUB_REPL_ASCII = new byte[]{(byte)'?'};
    private static final byte[] SCRUB_REPL_UTF16BE = new byte[]{(byte)0xFF, (byte)0xFD};
    private static final byte[] SCRUB_REPL_UTF16LE = new byte[]{(byte)0xFD, (byte)0xFF};
    private static final byte[] SCRUB_REPL_UTF32BE = new byte[]{(byte)0x00, (byte)0x00, (byte)0xFF, (byte)0xFD};
    private static final byte[] SCRUB_REPL_UTF32LE = new byte[]{(byte)0xFD, (byte)0xFF, (byte)0x00, (byte)0x00};

    private volatile int shareLevel = SHARE_LEVEL_NONE;

    private ByteList value;

    private static final String[][] opTable19 = {
        { "+", "+(binary)" },
        { "-", "-(binary)" }
    };

    public static RubyClass createStringClass(Ruby runtime) {
        RubyClass stringClass = runtime.defineClass("String", runtime.getObject(), STRING_ALLOCATOR);
        runtime.setString(stringClass);
        stringClass.setClassIndex(ClassIndex.STRING);
        stringClass.setReifiedClass(RubyString.class);
        stringClass.kindOf = new RubyModule.JavaClassKindOf(RubyString.class);

        stringClass.includeModule(runtime.getComparable());
        stringClass.defineAnnotatedMethods(RubyString.class);

        return stringClass;
    }

    private static ObjectAllocator STRING_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return RubyString.newAllocatedString(runtime, klass);
        }
    };

    @Override
    public Encoding getEncoding() {
        return value.getEncoding();
    }

    @Override
    public void setEncoding(Encoding encoding) {
        value.setEncoding(encoding);
    }

    @Override
    public boolean shouldMarshalEncoding() {
        return getEncoding() != ASCIIEncoding.INSTANCE;
    }

    @Override
    public Encoding getMarshalEncoding() {
        return getEncoding();
    }

    public void associateEncoding(Encoding enc) {
        StringSupport.associateEncoding(this, enc);
    }

    public final void setEncodingAndCodeRange(Encoding enc, int cr) {
        value.setEncoding(enc);
        setCodeRange(cr);
    }

    public final Encoding toEncoding(Ruby runtime) {
        return runtime.getEncodingService().findEncoding(this);
    }

    @Override
    public final int getCodeRange() {
        return flags & CR_MASK;
    }

    @Override
    public final void setCodeRange(int codeRange) {
        clearCodeRange();
        flags |= codeRange & CR_MASK;
    }

    @Override
    public final void clearCodeRange() {
        flags &= ~CR_MASK;
    }

    @Override
    public final void keepCodeRange() {
        if (getCodeRange() == CR_BROKEN) clearCodeRange();
    }

    // ENC_CODERANGE_ASCIIONLY
    public final boolean isCodeRangeAsciiOnly() {
        return CodeRangeSupport.isCodeRangeAsciiOnly(this);
    }

    // rb_enc_str_asciionly_p
    public final boolean isAsciiOnly() {
        return StringSupport.isAsciiOnly(this);
    }

    @Override
    public final boolean isCodeRangeValid() {
        return (flags & CR_MASK) == CR_VALID;
    }

    public final boolean isCodeRangeBroken() {
        return (flags & CR_MASK) == CR_BROKEN;
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
    @Override
    public final int scanForCodeRange() {
        int cr = getCodeRange();
        if (cr == CR_UNKNOWN) {
            cr = codeRangeScan(value.getEncoding(), value);
            setCodeRange(cr);
        }
        return cr;
    }

    final boolean singleByteOptimizable() {
        return StringSupport.isSingleByteOptimizable(this, value.getEncoding());
    }

    final boolean singleByteOptimizable(Encoding enc) {
        return StringSupport.isSingleByteOptimizable(this, enc);
    }

    // rb_enc_compatible
    private Encoding isCompatibleWith(RubyString other) {
        Encoding enc1 = value.getEncoding();
        Encoding enc2 = other.value.getEncoding();

        if (enc1 == enc2) return enc1;

        if (other.value.getRealSize() == 0) return enc1;
        if (value.getRealSize() == 0) {
            return (enc1.isAsciiCompatible() && other.isAsciiOnly()) ? enc1 : enc2;
        }

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

    // rb_enc_check
    public final Encoding checkEncoding(RubyString other) {
        return checkEncoding((ByteListHolder) other);
    }

    final Encoding checkEncoding(EncodingCapable other) {
        Encoding enc = isCompatibleWith(other);
        if (enc == null) throw getRuntime().newEncodingCompatibilityError("incompatible character encodings: " +
                                value.getEncoding() + " and " + other.getEncoding());
        return enc;
    }

    @Override
    public final Encoding checkEncoding(ByteListHolder other) {
        // TODO (nirvdrum 13-Jan-15): This cast is untenable.  It's a temporary measure until isCompatibleWith and its call graph are generalized.
        Encoding enc = isCompatibleWith((RubyString) other);
        if (enc == null) throw getRuntime().newEncodingCompatibilityError("incompatible character encodings: " +
                value.getEncoding() + " and " + other.getByteList().getEncoding());
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

    public final int strLength() {
        if (StringSupport.isSingleByteOptimizable(this, value.getEncoding())) return value.getRealSize();
        return StringSupport.strLengthFromRubyString(this);
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
        RubyClass metaclass = getMetaClass();
        Ruby runtime = metaclass.getClassRuntime();
        if (metaclass != runtime.getString() || metaclass != other.getMetaClass()) return super.eql(other);
        return eql19(runtime, other);
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
        this(runtime, rubyClass, value, null);
    }

    public RubyString(Ruby runtime, RubyClass rubyClass, CharSequence value, Encoding enc) {
        super(runtime, rubyClass);
        assert value != null;
        if (enc == null) enc = UTF8;

        this.value = encodeBytelist(value, enc);
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

    public static RubyString newString(Ruby runtime, String str, Encoding encoding) {
        return new RubyString(runtime, runtime.getString(), str, encoding);
    }

    public static RubyString newUSASCIIString(Ruby runtime, String str) {
        return new RubyString(runtime, runtime.getString(), str, USASCIIEncoding.INSTANCE);
    }

    public static RubyString newString(Ruby runtime, byte[] bytes) {
        return new RubyString(runtime, runtime.getString(), bytes);
    }

    public static RubyString newString(Ruby runtime, byte[] bytes, int start, int length) {
        return newString(runtime, bytes, start, length, ASCIIEncoding.INSTANCE);
    }

    // rb_enc_str_new
    public static RubyString newString(Ruby runtime, byte[] bytes, int start, int length, Encoding encoding) {
        byte[] copy = new byte[length];
        System.arraycopy(bytes, start, copy, 0, length);
        return new RubyString(runtime, runtime.getString(), new ByteList(copy, encoding, false));
    }

    public static RubyString newString(Ruby runtime, ByteList bytes) {
        return new RubyString(runtime, runtime.getString(), bytes);
    }

    public static RubyString newString(Ruby runtime, ByteList bytes, Encoding encoding) {
        return new RubyString(runtime, runtime.getString(), bytes, encoding);
    }

    public static RubyString newUnicodeString(Ruby runtime, String str) {
        Encoding defaultInternal = runtime.getDefaultInternalEncoding();
        if (defaultInternal == UTF16BEEncoding.INSTANCE) {
            return newUTF16String(runtime, str);
        } else {
            return newUTF8String(runtime, str);
        }
    }

    public static RubyString newUTF8String(Ruby runtime, String str) {
        ByteList byteList = new ByteList(RubyEncoding.encodeUTF8(str), UTF8Encoding.INSTANCE, false);
        return new RubyString(runtime, runtime.getString(), byteList);
    }

    public static RubyString newUTF16String(Ruby runtime, String str) {
        ByteList byteList = new ByteList(RubyEncoding.encodeUTF16(str), UTF16BEEncoding.INSTANCE, false);
        return new RubyString(runtime, runtime.getString(), byteList);
    }

    public static RubyString newUnicodeString(Ruby runtime, CharSequence str) {
        Encoding defaultInternal = runtime.getDefaultInternalEncoding();
        if (defaultInternal == UTF16BEEncoding.INSTANCE) {
            return newUTF16String(runtime, str);
        } else {
            return newUTF8String(runtime, str);
        }
    }

    public static RubyString newUTF8String(Ruby runtime, CharSequence str) {
        ByteList byteList = new ByteList(RubyEncoding.encodeUTF8(str), UTF8Encoding.INSTANCE, false);
        return new RubyString(runtime, runtime.getString(), byteList);
    }

    public static RubyString newUTF16String(Ruby runtime, CharSequence str) {
        ByteList byteList = new ByteList(RubyEncoding.encodeUTF16(str.toString()), UTF16BEEncoding.INSTANCE, false);
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

    public static RubyString newStringShared(Ruby runtime, byte[] bytes, Encoding encoding) {
        return newStringShared(runtime, new ByteList(bytes, encoding, false));
    }

    public static RubyString newStringShared(Ruby runtime, byte[] bytes, int start, int length) {
        return newStringShared(runtime, new ByteList(bytes, start, length, false));
    }

    public static RubyString newStringShared(Ruby runtime, byte[] bytes, int start, int length, Encoding encoding) {
        return newStringShared(runtime, new ByteList(bytes, start, length, encoding, false));
    }

    public static RubyString newEmptyString(Ruby runtime) {
        return newEmptyString(runtime, runtime.getString());
    }

    private static final ByteList EMPTY_ASCII8BIT_BYTELIST = new ByteList(new byte[0], ASCIIEncoding.INSTANCE);
    private static final ByteList EMPTY_USASCII_BYTELIST = new ByteList(new byte[0], USASCIIEncoding.INSTANCE);

    public static RubyString newAllocatedString(Ruby runtime, RubyClass metaClass) {
        RubyString empty = new RubyString(runtime, metaClass, EMPTY_ASCII8BIT_BYTELIST);
        empty.shareLevel = SHARE_LEVEL_BYTELIST;
        return empty;
    }

    public static RubyString newEmptyString(Ruby runtime, RubyClass metaClass) {
        RubyString empty = new RubyString(runtime, metaClass, EMPTY_USASCII_BYTELIST);
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

    // str_independent
    public boolean independent() {
        return shareLevel == SHARE_LEVEL_NONE;
    }

    // str_make_independent, modified to create a new String rather than possibly modifying a frozen one
    public RubyString makeIndependent() {
        RubyClass klass = metaClass;
        RubyString str = strDup(klass.getClassRuntime(), klass);
        str.modify();
        str.setFrozen(true);
        str.infectBy(this);
        return str;
    }

    // MRI: EXPORT_STR macro in process.c
    public RubyString export(ThreadContext context) {
        if (Platform.IS_WINDOWS) {
            return EncodingUtils.strConvEncOpts(context, this, null, UTF8Encoding.INSTANCE, 0, context.nil);
        }
        return this;
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
        RubyString str = newUsAsciiStringNoCopy(runtime, bytes);
        str.shareLevel = SHARE_LEVEL_BYTELIST;
        return str;
    }

    public static RubyString newUsAsciiStringShared(Ruby runtime, byte[] bytes, int start, int length) {
        byte[] copy = new byte[length];
        System.arraycopy(bytes, start, copy, 0, length);
        return newUsAsciiStringShared(runtime, new ByteList(copy, false));
    }

    @Override
    public ClassIndex getNativeClassIndex() {
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
        return Helpers.decodeByteList(getRuntime(), value);
    }

    /**
     * Overridden dup for fast-path logic.
     *
     * @return A new RubyString sharing the original backing store.
     */
    @Override
    public IRubyObject dup() {
        RubyClass mc = metaClass.getRealClass();
        if (mc.getClassIndex() != ClassIndex.STRING) return super.dup();

        return strDup(mc.getClassRuntime(), mc.getRealClass());
    }

    // rb_str_new_frozen or rb_str_dup_frozen
    public IRubyObject dupFrozen() {
        RubyString dup = (RubyString)dup();
        dup.setFrozen(true);
        return dup;
    }

    // MRI: rb_str_dup
    public final RubyString strDup(Ruby runtime) {
        return strDup(runtime, getMetaClass().getRealClass());
    }

    final RubyString strDup(Ruby runtime, RubyClass clazz) {
        shareLevel = SHARE_LEVEL_BYTELIST;
        RubyString dup = new RubyString(runtime, clazz, value);
        dup.shareLevel = SHARE_LEVEL_BYTELIST;
        dup.flags |= flags & (CR_MASK | TAINTED_F);

        return dup;
    }

    /* rb_str_subseq */
    public final RubyString makeSharedString(Ruby runtime, int index, int len) {
        return makeShared19(runtime, runtime.getString(), index, len);
    }

    public RubyString makeSharedString19(Ruby runtime, int index, int len) {
        return makeShared19(runtime, runtime.getString(), value, index, len);
    }

    public final RubyString makeShared(Ruby runtime, int index, int len) {
        return makeShared19(runtime, getType(), index, len);
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

    public final RubyString makeShared19(Ruby runtime, RubyClass meta, int index, int len) {
        return makeShared19(runtime, meta, value, index, len);
    }

    private RubyString makeShared19(Ruby runtime, ByteList value, int index, int len) {
        return makeShared19(runtime, getType(), value, index, len);
    }

    private RubyString makeShared19(Ruby runtime, RubyClass meta, ByteList value, int index, int len) {
        final RubyString shared;
        Encoding enc = value.getEncoding();

        if (len == 0) {
            shared = newEmptyString(runtime, meta, enc);
        } else if (len == 1) {
            // as with the 1.8 makeShared, don't bother sharing for substrings that are a single byte
            // to get a good speed boost in a number of common scenarios (note though that unlike 1.8,
            // we can't take advantage of SINGLE_CHAR_BYTELISTS since our encoding may not be ascii, but the
            // single byte copy is pretty much negligible)
            shared = newStringShared(runtime,
                                     meta,
                                     new ByteList(new byte[] { (byte) value.get(index) }, enc),
                                     enc);
        } else {
            if (shareLevel == SHARE_LEVEL_NONE) shareLevel = SHARE_LEVEL_BUFFER;
            shared = new RubyString(runtime, meta, value.makeShared(index, len));
            shared.shareLevel = SHARE_LEVEL_BUFFER;
        }
        shared.copyCodeRangeForSubstr(this, enc); // no need to assign encoding, same bytelist shared
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

    /**
     * rb_str_modify_expand
     */
    public final void modifyExpand(int length) {
        modify(length);
        clearCodeRange();
    }

    // io_set_read_length
    public void setReadLength(int length) {
        if (size() != length) {
            modify();
            value.setRealSize(length);
        }
    }

    // rb_str_new_frozen, at least in spirit
    public RubyString newFrozen() {
        RubyClass klass;
        RubyString str = this;

        if (isFrozen()) return this;
        klass = getMetaClass();
        str = strDup(klass.getClassRuntime());
        str.setCodeRange(getCodeRange());
        str.modify();
        str.setFrozen(true);
        return str;
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

    @JRubyMethod(meta = true)
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
        return (int)op_cmp(getRuntime().getCurrentContext(), other).convertToInteger().getLongValue();
    }

    /* rb_str_cmp_m */
    @JRubyMethod(name = "<=>")
    @Override
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;
        if (other instanceof RubyString) {
            return runtime.newFixnum(op_cmp((RubyString)other));
        }
        if (other.respondsTo("to_str")) {
            IRubyObject tmp = other.callMethod(context, "to_str");
            if (tmp instanceof RubyString)
              return runtime.newFixnum(op_cmp((RubyString)tmp));
        } else {
            return invcmp(context, this, other);
        }
        return runtime.getNil();
    }

    /** rb_str_equal
     *
     */
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        return op_equal19(context, other);
    }

    @JRubyMethod(name = {"==", "==="})
    public IRubyObject op_equal19(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;
        if (this == other) return runtime.getTrue();
        if (other instanceof RubyString) {
            RubyString otherString = (RubyString)other;
            return isComparableWith(otherString) && value.equal(otherString.value) ? runtime.getTrue() : runtime.getFalse();
        }
        return op_equalCommon(context, other);
    }

    private IRubyObject op_equalCommon(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;
        if (!other.respondsTo("to_str")) return runtime.getFalse();
        return invokedynamic(context, other, OP_EQUAL, this).isTrue() ? runtime.getTrue() : runtime.getFalse();
    }

    public IRubyObject op_plus(ThreadContext context, IRubyObject _str) {
        return op_plus19(context, _str);
    }

    @JRubyMethod(name = "+", required = 1)
    public IRubyObject op_plus19(ThreadContext context, IRubyObject _str) {
        RubyString str = _str.convertToString();
        Encoding enc = checkEncoding(str);
        RubyString resultStr = newStringNoCopy(context.runtime, addByteLists(value, str.value),
                                    enc, CodeRangeSupport.codeRangeAnd(getCodeRange(), str.getCodeRange()));
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

    public IRubyObject op_mul(ThreadContext context, IRubyObject other) {
        return op_mul19(context, other);
    }

    @JRubyMethod(name = "*", required = 1)
    public IRubyObject op_mul19(ThreadContext context, IRubyObject other) {
        RubyString result = multiplyByteList(context, other);
        result.value.setEncoding(value.getEncoding());
        result.copyCodeRange(this);
        return result;
    }

    private RubyString multiplyByteList(ThreadContext context, IRubyObject arg) {
        int len = RubyNumeric.num2int(arg);
        if (len < 0) throw context.runtime.newArgumentError("negative argument");

        // we limit to int because ByteBuffer can only allocate int sizes
        if (len > 0 && Integer.MAX_VALUE / len < value.getRealSize()) {
            throw context.runtime.newArgumentError("argument too big");
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
        RubyString result = new RubyString(context.runtime, getMetaClass(), bytes);
        result.infectBy(this);
        return result;
    }

    @JRubyMethod(name = "%", required = 1)
    public IRubyObject op_format(ThreadContext context, IRubyObject arg) {
        return opFormatCommon(context, arg);
    }

    private IRubyObject opFormatCommon(ThreadContext context, IRubyObject arg) {
        IRubyObject tmp;
        if (arg instanceof RubyHash) {
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
        tainted = Sprintf.sprintf1_9(out, Locale.US, value, tmp);

        RubyString str = newString(context.runtime, out);

        str.setTaint(tainted || isTaint());
        return str;
    }

    @JRubyMethod
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
     * Generate a hash for the String, using its associated Ruby instance's hash seed.
     *
     * @param runtime
     * @return
     */
    public int strHashCode(Ruby runtime) {
        long hash = runtime.isSiphashEnabled() ? SipHashInline.hash24(runtime.getHashSeedK0(),
                runtime.getHashSeedK1(), value.getUnsafeBytes(), value.getBegin(),
                value.getRealSize()) : PerlHash.hash(runtime.getHashSeedK0(),
                value.getUnsafeBytes(), value.getBegin(), value.getRealSize());
        hash ^= (value.getEncoding().isAsciiCompatible() && scanForCodeRange() == CR_7BIT ? 0
                : value.getEncoding().getIndex());
        return (int) hash;
    }

    /**
     * Generate a hash for the String, without a seed.
     *
     * @param runtime
     * @return
     */
    public int unseededStrHashCode(Ruby runtime) {
        long hash = runtime.isSiphashEnabled() ? SipHashInline.hash24(0, 0, value.getUnsafeBytes(),
                value.getBegin(), value.getRealSize()) : PerlHash.hash(0, value.getUnsafeBytes(),
                value.getBegin(), value.getRealSize());
        hash ^= (value.getEncoding().isAsciiCompatible() && scanForCodeRange() == CR_7BIT ? 0
                : value.getEncoding().getIndex());
        return (int) hash;
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

    // // rb_str_buf_append against VALUE
    public final RubyString cat19(RubyString str2) {
        int str2_cr = cat19(str2.getByteList(), str2.getCodeRange());

        infectBy(str2);
        str2.setCodeRange(str2_cr);

        return this;
    }

    // rb_str_buf_append against ptr
    public final int cat19(ByteList other, int codeRange) {
        int[] ptr_cr_ret = {codeRange};
        EncodingUtils.encCrStrBufCat(getRuntime(), this, other, other.getEncoding(), codeRange, ptr_cr_ret);
        return ptr_cr_ret[0];
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

    // rb_enc_str_buf_cat
    public final int cat(byte[]bytes, int p, int len, Encoding enc) {
        int[] ptr_cr_ret = {CR_UNKNOWN};
        EncodingUtils.encCrStrBufCat(getRuntime(), this, new ByteList(bytes, p, len), enc, CR_UNKNOWN, ptr_cr_ret);
        return ptr_cr_ret[0];
    }

    // rb_str_buf_cat_ascii
    public final RubyString catAscii(byte[]bytes, int ptr, int ptrLen) {
        Encoding enc = value.getEncoding();
        if (enc.isAsciiCompatible()) {
            EncodingUtils.encCrStrBufCat(getRuntime(), this, new ByteList(bytes, ptr, ptrLen), enc, CR_7BIT, null);
        } else {
            byte buf[] = new byte[enc.maxLength()];
            int end = ptr + ptrLen;
            while (ptr < end) {
                int c = bytes[ptr];
                int len = codeLength(getRuntime(), enc, c);
                EncodingUtils.encMbcput(c, buf, 0, enc);
                EncodingUtils.encCrStrBufCat(getRuntime(), this, buf, 0, len, enc, CR_VALID, null);
                ptr++;
            }
        }
        return this;
    }

    /** rb_str_replace_m
     *
     */
    public IRubyObject replace(IRubyObject other) {
        return replace19(other);
    }

    @JRubyMethod(name = "initialize_copy", required = 1, visibility = Visibility.PRIVATE)
    @Override
    public RubyString initialize_copy(IRubyObject other) {
        return replace19(other);
    }

    @JRubyMethod(name = "replace", required = 1)
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

    @JRubyMethod
    public RubyString clear() {
        modifyCheck();
        Encoding enc = value.getEncoding();

        EmptyByteListHolder holder = getEmptyByteList(enc);
        value = holder.bytes;
        shareLevel = SHARE_LEVEL_BYTELIST;
        setCodeRange(holder.cr);
        return this;
    }

    public IRubyObject reverse(ThreadContext context) {
        return reverse19(context);
    }

    @JRubyMethod(name = "reverse")
    public IRubyObject reverse19(ThreadContext context) {
        Ruby runtime = context.runtime;
        if (value.getRealSize() <= 1) return strDup(context.runtime);

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

    public RubyString reverse_bang(ThreadContext context) {
        return reverse_bang19(context);
    }

    @JRubyMethod(name = "reverse!")
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

    @Override
    public IRubyObject initialize(ThreadContext context) {
        return initialize19(context);
    }

    public IRubyObject initialize(ThreadContext context, IRubyObject arg0) {
        return initialize19(context, arg0);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    @Override
    public IRubyObject initialize19(ThreadContext context) {
        return this;
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize19(ThreadContext context, IRubyObject arg0) {
        replace19(arg0);
        return this;
    }

    public IRubyObject casecmp(ThreadContext context, IRubyObject other) {
        return casecmp19(context, other);
    }

    @JRubyMethod(name = "casecmp")
    public IRubyObject casecmp19(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;
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
            if (enc.isAsciiCompatible() && Encoding.isAscii(c) && Encoding.isAscii(oc)) {
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
    @Override
    public IRubyObject op_match(ThreadContext context, IRubyObject other) {
        return op_match19(context, other);
    }

    @JRubyMethod(name = "=~", writes = BACKREF)
    @Override
    public IRubyObject op_match19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyRegexp) return ((RubyRegexp) other).op_match19(context, this);
        if (other instanceof RubyString) throw context.runtime.newTypeError("type mismatch: String given");
        return other.callMethod(context, "=~", this);
    }
    /**
     * String#match(pattern)
     *
     * rb_str_match_m
     *
     * @param pattern Regexp or String
     */
    public IRubyObject match(ThreadContext context, IRubyObject pattern) {
        return match19(context, pattern, Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "match", reads = BACKREF)
    public IRubyObject match19(ThreadContext context, IRubyObject pattern, Block block) {
        IRubyObject result = getPattern(pattern).callMethod(context, "match", this);
        return block.isGiven() && !result.isNil() ? block.yield(context, result) : result;
    }

    @JRubyMethod(name = "match", required = 1, rest = true, reads = BACKREF)
    public IRubyObject match19(ThreadContext context, IRubyObject[] args, Block block) {
        RubyRegexp pattern = getPattern(args[0]);
        args[0] = this;
        IRubyObject result = pattern.callMethod(context, "match", args);
        return block.isGiven() && !result.isNil() ? block.yield(context, result) : result;
    }

    /** rb_str_capitalize / rb_str_capitalize_bang
     *
     */
    public IRubyObject capitalize(ThreadContext context) {
        return capitalize19(context);
    }

    public IRubyObject capitalize_bang(ThreadContext context) {
        return capitalize_bang19(context);
    }

    @JRubyMethod(name = "capitalize")
    public IRubyObject capitalize19(ThreadContext context) {
        RubyString str = strDup(context.runtime);
        str.capitalize_bang19(context);
        return str;
    }

    @JRubyMethod(name = "capitalize!")
    public IRubyObject capitalize_bang19(ThreadContext context) {
        Ruby runtime = context.runtime;
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

    public IRubyObject op_ge(ThreadContext context, IRubyObject other) {
        return op_ge19(context, other);
    }

    @JRubyMethod(name = ">=")
    public IRubyObject op_ge19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) return context.runtime.newBoolean(op_cmp((RubyString) other) >= 0);
        return RubyComparable.op_ge(context, this, other);
    }

    public IRubyObject op_gt(ThreadContext context, IRubyObject other) {
        return op_gt19(context, other);
    }

    @JRubyMethod(name = ">")
    public IRubyObject op_gt19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) return context.runtime.newBoolean(op_cmp((RubyString) other) > 0);
        return RubyComparable.op_gt(context, this, other);
    }

    public IRubyObject op_le(ThreadContext context, IRubyObject other) {
        return op_le19(context, other);
    }

    @JRubyMethod(name = "<=")
    public IRubyObject op_le19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) return context.runtime.newBoolean(op_cmp((RubyString) other) <= 0);
        return RubyComparable.op_le(context, this, other);
    }

    public IRubyObject op_lt(ThreadContext context, IRubyObject other) {
        return op_lt19(context, other);
    }

    @JRubyMethod(name = "<")
    public IRubyObject op_lt19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) return context.runtime.newBoolean(op_cmp((RubyString) other) < 0);
        return RubyComparable.op_lt(context, this, other);
    }

    public IRubyObject str_eql_p(ThreadContext context, IRubyObject other) {
        return str_eql_p19(context, other);
    }

    @JRubyMethod(name = "eql?")
    public IRubyObject str_eql_p19(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;
        if (other instanceof RubyString) {
            RubyString otherString = (RubyString)other;
            if (isComparableWith(otherString) && value.equal(otherString.value)) return runtime.getTrue();
        }
        return runtime.getFalse();
    }

    /** rb_str_upcase / rb_str_upcase_bang
     *
     */
    public RubyString upcase(ThreadContext context) {
        return upcase19(context);
    }

    public IRubyObject upcase_bang(ThreadContext context) {
        return upcase_bang19(context);
    }

    @JRubyMethod(name = "upcase")
    public RubyString upcase19(ThreadContext context) {
        RubyString str = strDup(context.runtime);
        str.upcase_bang19(context);
        return str;
    }

    @JRubyMethod(name = "upcase!")
    public IRubyObject upcase_bang19(ThreadContext context) {
        Ruby runtime = context.runtime;
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
    public RubyString downcase(ThreadContext context) {
        return downcase19(context);
    }

    public IRubyObject downcase_bang(ThreadContext context) {
        return downcase_bang19(context);
    }

    @JRubyMethod(name = "downcase")
    public RubyString downcase19(ThreadContext context) {
        RubyString str = strDup(context.runtime);
        str.downcase_bang19(context);
        return str;
    }

    @JRubyMethod(name = "downcase!")
    public IRubyObject downcase_bang19(ThreadContext context) {
        Ruby runtime = context.runtime;
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
    public RubyString swapcase(ThreadContext context) {
        return swapcase19(context);
    }

    public IRubyObject swapcase_bang(ThreadContext context) {
        return swapcase_bang19(context);
    }

    @JRubyMethod(name = "swapcase")
    public RubyString swapcase19(ThreadContext context) {
        RubyString str = strDup(context.runtime);
        str.swapcase_bang19(context);
        return str;
    }

    @JRubyMethod(name = "swapcase!")
    public IRubyObject swapcase_bang19(ThreadContext context) {
        Ruby runtime = context.runtime;
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
    public IRubyObject dump() {
        return dump19();
    }

    @JRubyMethod(name = "dump")
    public IRubyObject dump19() {
        ByteList outBytes = StringSupport.dumpCommon(getRuntime(), value);

        final RubyString result = new RubyString(getRuntime(), getMetaClass(), outBytes);
        Encoding enc = value.getEncoding();

        if (!enc.isAsciiCompatible()) {
            result.cat(".force_encoding(\"".getBytes());
            result.cat(enc.getName());
            result.cat((byte)'"').cat((byte)')');
            enc = ASCII;
        }
        result.associateEncoding(enc);
        result.setCodeRange(CR_7BIT);

        return result.infectBy(this);
    }

    public IRubyObject insert(ThreadContext context, IRubyObject indexArg, IRubyObject stringArg) {
        return insert19(context, indexArg, stringArg);
    }

    @JRubyMethod(name = "insert")
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

    /** rb_str_inspect
     *
     */
    @Override
    public IRubyObject inspect() {
        return inspect19();
    }

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect19() {
        return inspect19(getRuntime(), value).infectBy(this);
    }

    public static IRubyObject inspect19(Ruby runtime, ByteList byteList) {
        byte bytes[] = byteList.getUnsafeBytes();
        int p = byteList.getBegin();
        int end = p + byteList.getRealSize();
        RubyString result = new RubyString(runtime, runtime.getString(), new ByteList(end - p));
        Encoding enc = byteList.getEncoding();

        Encoding resultEnc = runtime.getDefaultInternalEncoding();
        if (resultEnc == null) resultEnc = runtime.getDefaultExternalEncoding();
        if (!resultEnc.isAsciiCompatible()) resultEnc = USASCIIEncoding.INSTANCE;
        result.associateEncoding(resultEnc);

        boolean isUnicode = StringSupport.isUnicode(enc);
        boolean asciiCompat = enc.isAsciiCompatible();

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
                e = encodings.get("ASCII-8BIT".getBytes());
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
                e = encodings.get("ASCII-8BIT".getBytes());
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
            if ((asciiCompat || isUnicode) &&
                    (c == '"' || c == '\\' ||
                        (c == '#' && p < end && (StringSupport.preciseLength(enc, bytes, p, end) > 0) &&
                        (cc = codePoint(runtime, enc, bytes, p, end)) == '$' || cc == '@' || cc == '{'))) {
                if (p - n > prev) result.cat(bytes, prev, p - n - prev);
                result.cat('\\');
                if (asciiCompat || enc == resultEnc) {
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

            if ((enc == resultEnc && enc.isPrint(c)) || (asciiCompat && Encoding.isAscii(c) && enc.isPrint(c))) {
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
        return result;
    }

    public int size() {
        return value.getRealSize();
    }

    /** rb_str_length
     *
     */
    public RubyFixnum length() {
        return length19();
    }

    @JRubyMethod(name = {"length", "size"})
    public RubyFixnum length19() {
        return getRuntime().newFixnum(strLength());
    }

    @JRubyMethod(name = "bytesize")
    public RubyFixnum bytesize() {
        return getRuntime().newFixnum(value.getRealSize());
    }

    private SizeFn eachByteSizeFn() {
        final RubyString self = this;
        return new SizeFn() {
            @Override
            public IRubyObject size(IRubyObject[] args) {
                return self.bytesize();
            }
        };
    }

    /** rb_str_empty
     *
     */
    @JRubyMethod(name = "empty?")
    public RubyBoolean empty_p(ThreadContext context) {
        return isEmpty() ? context.runtime.getTrue() : context.runtime.getFalse();
    }

    public boolean isEmpty() {
        return value.length() == 0;
    }

    /** rb_str_append
     *
     */
    public RubyString append(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            cat(ConvertBytes.longToByteList(((RubyFixnum)other).getLongValue()));
            return this;
        } else if (other instanceof RubyFloat) {
            return cat((RubyString)((RubyFloat)other).to_s());
        } else if (other instanceof RubySymbol) {
            cat(((RubySymbol)other).getBytes());
            return this;
        }
        RubyString otherStr = other.convertToString();
        infectBy(otherStr);
        return cat(otherStr.value);
    }

    public RubyString append19(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            cat19(ConvertBytes.longToByteList(((RubyFixnum)other).getLongValue()), StringSupport.CR_7BIT);
            return this;
        } else if (other instanceof RubyFloat) {
            return cat19((RubyString)((RubyFloat)other).to_s());
        } else if (other instanceof RubySymbol) {
            cat19(((RubySymbol)other).getBytes(), 0);
            return this;
        }
        return cat19(other.convertToString());
    }

    /** rb_str_concat
     *
     */
    public RubyString concat(IRubyObject other) {
        return concat19(getRuntime().getCurrentContext(), other);
    }

    @JRubyMethod(name = {"concat", "<<"})
    public RubyString concat19(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;
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

        if (other instanceof RubySymbol) throw runtime.newTypeError("can't convert Symbol into String");
        return append19(other);
    }

    private RubyString concatNumeric(Ruby runtime, int c) {
        Encoding enc = value.getEncoding();
        int cl;

        try {
            cl = codeLength(runtime, enc, c);
            modify19(value.getRealSize() + cl);

            if (enc == USASCIIEncoding.INSTANCE) {
                if (c > 0xff) runtime.newRangeError(c + " out of char range");
                if (c > 0x79) {
                    value.setEncoding(ASCIIEncoding.INSTANCE);
                    enc = value.getEncoding();
                }
            }
            enc.codeToMbc(c, value.getUnsafeBytes(), value.getBegin() + value.getRealSize());
        } catch (EncodingException e) {
            throw runtime.newRangeError(c + " out of char range");
        }
        value.setRealSize(value.getRealSize() + cl);
        return this;
    }

    /**
     * rb_str_prepend
     */
    @JRubyMethod
    public IRubyObject prepend(ThreadContext context, IRubyObject other) {
        return replace19(other.convertToString().op_plus19(context, this));
    }

    /** rb_str_crypt
     *
     */
    @JRubyMethod(name = "crypt")
    public RubyString crypt(ThreadContext context, IRubyObject other) {
        Encoding ascii8bit = context.runtime.getEncodingService().getAscii8bitEncoding();
        RubyString otherStr = other.convertToString();
        otherStr.associateEncoding(ascii8bit);
        String salt = otherStr.asJavaString();
        if (otherStr.getByteList().length() < 2) {
            throw context.runtime.newArgumentError("salt too short(need >=2 bytes)");
        }

        POSIX posix = context.runtime.getPosix();
        CharSequence cryptedString = posix.crypt(asJavaString(), salt);
        // We differ from MRI in that we do not process salt to make it work and we will
        // return any errors via errno.
        if (cryptedString == null) throw context.runtime.newErrnoFromInt(posix.errno());

        RubyString result = RubyString.newString(context.runtime, cryptedString.toString());
        result.associateEncoding(ascii8bit);
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
    public IRubyObject sub(ThreadContext context, IRubyObject arg0, Block block) {
        return sub19(context, arg0, block);
    }

    public IRubyObject sub(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return sub19(context, arg0, arg1, block);
    }

    public IRubyObject sub_bang(ThreadContext context, IRubyObject arg0, Block block) {
        return sub_bang19(context, arg0, block);
    }

    public IRubyObject sub_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return sub_bang19(context, arg0, arg1, block);
    }

    @JRubyMethod(name = "sub", reads = BACKREF, writes = BACKREF)
    public IRubyObject sub19(ThreadContext context, IRubyObject arg0, Block block) {
        RubyString str = strDup(context.runtime);
        str.sub_bang19(context, arg0, block);
        return str;
    }

    @JRubyMethod(name = "sub", reads = BACKREF, writes = BACKREF)
    public IRubyObject sub19(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        RubyString str = strDup(context.runtime);
        str.sub_bang19(context, arg0, arg1, block);
        return str;
    }

    @JRubyMethod(name = "sub!", reads = BACKREF, writes = BACKREF)
    public IRubyObject sub_bang19(ThreadContext context, IRubyObject arg0, Block block) {
        Ruby runtime = context.runtime;
        frozenCheck();

        RubyRegexp regexp = arg0 instanceof RubyRegexp ? (RubyRegexp) arg0 :
                RubyRegexp.newRegexp(runtime, RubyRegexp.quote19(getStringForPattern(arg0).getByteList(), false), new RegexpOptions());
        Regex pattern = regexp.getPattern();
        Regex prepared = regexp.preparePattern(this);

        if (block.isGiven()) return subBangIter19(runtime, context, pattern, prepared, null, block, regexp);
        throw context.runtime.newArgumentError(1, 2);
    }

    @JRubyMethod(name = "sub!", reads = BACKREF, writes = BACKREF)
    public IRubyObject sub_bang19(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        Ruby runtime = context.runtime;
        IRubyObject hash = TypeConverter.convertToTypeWithCheck(arg1, runtime.getHash(), "to_hash");
        frozenCheck();

        RubyRegexp regexp = arg0 instanceof RubyRegexp ? (RubyRegexp) arg0 :
            RubyRegexp.newRegexp(runtime, RubyRegexp.quote19(getStringForPattern(arg0).getByteList(), false), new RegexpOptions());
        Regex pattern = regexp.getPattern();
        Regex prepared = regexp.preparePattern(this);

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

        if (RubyRegexp.matcherSearch(runtime, matcher, begin, range, Option.NONE) >= 0) {
            RubyMatchData match = RubyRegexp.createMatchData19(context, this, matcher, pattern);
            match.regexp = regexp;
            context.setBackRef(match);
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
            return subBangCommon19(context, pattern, matcher, repl, tuFlags | repl.flags);
        } else {
            return context.setBackRef(runtime.getNil());
        }
    }

    private IRubyObject subBangNoIter19(Ruby runtime, ThreadContext context, Regex pattern, Regex prepared, RubyString repl, RubyRegexp regexp) {
        int begin = value.getBegin();
        int range = begin + value.getRealSize();
        final Matcher matcher = prepared.matcher(value.getUnsafeBytes(), begin, range);

        if (RubyRegexp.matcherSearch(runtime, matcher, begin, range, Option.NONE) >= 0) {
            repl = RubyRegexp.regsub19(context, repl, this, matcher, pattern);
            RubyMatchData match = RubyRegexp.createMatchData19(context, this, matcher, pattern);
            match.regexp = regexp;
            context.setBackRef(match);
            return subBangCommon19(context, pattern, matcher, repl, repl.flags);
        } else {
            return context.setBackRef(runtime.getNil());
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
            throw context.runtime.newArgumentError(
                    "incompatible character encodings " + strEnc + " and " + repl.value.getEncoding());
        }
        return repl.value.getEncoding();
    }

    /** rb_str_gsub / rb_str_gsub_bang
     *
     */
    public IRubyObject gsub(ThreadContext context, IRubyObject arg0, Block block) {
        return gsub19(context, arg0, block);
    }

    public IRubyObject gsub(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return gsub19(context, arg0, arg1, block);
    }

    public IRubyObject gsub_bang(ThreadContext context, IRubyObject arg0, Block block) {
        return gsub_bang19(context, arg0, block);
    }

    public IRubyObject gsub_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return gsub_bang19(context, arg0, arg1, block);
    }

    @JRubyMethod(name = "gsub", reads = BACKREF, writes = BACKREF)
    public IRubyObject gsub19(ThreadContext context, IRubyObject arg0, Block block) {
        return block.isGiven() ? gsubCommon19(context, block, null, null, arg0, false, 0) : enumeratorize(context.runtime, this, "gsub", arg0);
    }

    @JRubyMethod(name = "gsub", reads = BACKREF, writes = BACKREF)
    public IRubyObject gsub19(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return gsub19(context, arg0, arg1, block, false);
    }

    @JRubyMethod(name = "gsub!", reads = BACKREF, writes = BACKREF)
    public IRubyObject gsub_bang19(ThreadContext context, IRubyObject arg0, Block block) {
        checkFrozen();
        return block.isGiven() ? gsubCommon19(context, block, null, null, arg0, true, 0) : enumeratorize(context.runtime, this, "gsub!", arg0);
    }

    @JRubyMethod(name = "gsub!", reads = BACKREF, writes = BACKREF)
    public IRubyObject gsub_bang19(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        checkFrozen();
        return gsub19(context, arg0, arg1, block, true);
    }

    private IRubyObject gsub19(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block, final boolean bang) {
        Ruby runtime = context.runtime;
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
        return gsubCommon19(context, block, repl, hash, arg0, bang, tuFlags, true);
    }

    // MRI: str_gsub, roughly
    private IRubyObject gsubCommon19(ThreadContext context, Block block, RubyString repl,
            RubyHash hash, IRubyObject arg0, final boolean bang, int tuFlags, boolean useBackref) {
        Ruby runtime = context.runtime;
        RubyRegexp regexp = arg0 instanceof RubyRegexp ? (RubyRegexp) arg0 :
                RubyRegexp.newRegexp(runtime, RubyRegexp.quote19(getStringForPattern(arg0).getByteList(), false), new RegexpOptions());
        Regex pattern = regexp.getPattern();
        Regex prepared = regexp.preparePattern(this);

        int offset, cp, n, blen;

        final byte[] spBytes = value.getUnsafeBytes();
        final int sp = value.getBegin();
        final int spLen = value.getRealSize();

        final Matcher matcher = prepared.matcher(spBytes, sp, sp + spLen);

        int beg = RubyRegexp.matcherSearch(runtime, matcher, sp, sp + spLen, Option.NONE);
        if (beg < 0) {
            if (useBackref) context.setBackRef(runtime.getNil());
            return bang ? runtime.getNil() : strDup(runtime); /* bang: true, no match, no substitution */
        }

        offset = 0;
        n = 0;
        blen = value.getRealSize() + 30;
        RubyString dest = new RubyString(runtime, getMetaClass(), new ByteList(blen));
        int slen = value.getRealSize();
        cp = sp;
        Encoding str_enc = value.getEncoding();
        dest.setEncoding(str_enc);
        dest.setCodeRange(str_enc.isAsciiCompatible() ? CR_7BIT : CR_VALID);

        RubyMatchData match = null;
        do {
            n++;
            final RubyString val;
            int begz = matcher.getBegin();
            int endz = matcher.getEnd();

            if (repl != null) {     // string given
                val = RubyRegexp.regsub19(context, repl, this, matcher, pattern);
            } else {
                final RubyString substr = makeShared19(runtime, begz, endz - begz);
                if (hash != null) { // hash given
                    val = objAsString(context, hash.op_aref(context, substr));
                } else {            // block given
                    match = RubyRegexp.createMatchData19(context, this, matcher, pattern);
                    match.regexp = regexp;
                    if (useBackref) context.setBackRef(match);
                    val = objAsString(context, block.yield(context, substr));
                }
                modifyCheck(spBytes, slen, str_enc);
                if (bang) frozenCheck();
            }

            tuFlags |= val.flags;

            int len = beg - offset;
            if (len != 0) dest.cat(spBytes, cp, len, str_enc);
            dest.cat19(val);
            offset = endz;
            if (begz == endz) {
                if (slen <= endz) break;
                len = StringSupport.encFastMBCLen(spBytes, sp + endz, sp + spLen, str_enc);
                dest.cat(spBytes, sp + endz, len, str_enc);
                offset = endz + len;
            }
            cp = sp + offset;
            if (offset > slen) break;
            beg = RubyRegexp.matcherSearch(runtime, matcher, cp, sp + spLen, Option.NONE);
        } while (beg >= 0);

        if (slen > offset) dest.cat(spBytes, cp, slen - offset, str_enc);

        if (match != null) { // block given
            if (useBackref) context.setBackRef(match);
        } else {
            match = RubyRegexp.createMatchData19(context, this, matcher, pattern);
            match.regexp = regexp;
            if (useBackref) context.setBackRef(match);
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
    public IRubyObject index(ThreadContext context, IRubyObject arg0) {
        return index19(context, arg0);
    }

    public IRubyObject index(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return index19(context, arg0, arg1);
    }

    @JRubyMethod(name = "index", reads = BACKREF, writes = BACKREF)
    public IRubyObject index19(ThreadContext context, IRubyObject arg0) {
        return indexCommon19(context.runtime, context, arg0, 0);
    }

    @JRubyMethod(name = "index", reads = BACKREF, writes = BACKREF)
    public IRubyObject index19(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        int pos = RubyNumeric.num2int(arg1);
        Ruby runtime = context.runtime;
        if (pos < 0) {
            pos += strLength();
            if (pos < 0) {
                if (arg0 instanceof RubyRegexp) context.setBackRef(runtime.getNil());
                return runtime.getNil();
            }
        }
        return indexCommon19(runtime, context, arg0, pos);
    }

    private IRubyObject indexCommon19(Ruby runtime, ThreadContext context, IRubyObject sub, int pos) {
        if (sub instanceof RubyRegexp) {
            if (pos > strLength()) return context.nil;
            RubyRegexp regSub = (RubyRegexp) sub;
            pos = singleByteOptimizable() ? pos :
                    StringSupport.nth(checkEncoding(regSub), value.getUnsafeBytes(), value.getBegin(),
                            value.getBegin() + value.getRealSize(),
                                      pos) - value.getBegin();
            pos = regSub.adjustStartPos19(this, pos, false);
            IRubyObject[] holder = {context.nil};
            pos = regSub.search19(context, this, pos, false, holder);
            context.setBackRef(holder[0]);
            pos = subLength(pos);
        } else if (sub instanceof RubyString) {
            pos = StringSupport.index(this, (RubyString) sub, pos, this.checkEncoding((RubyString) sub));
            pos = subLength(pos);
        } else {
            IRubyObject tmp = sub.checkStringType();
            if (tmp.isNil()) throw runtime.newTypeError("type mismatch: " + sub.getMetaClass().getName() + " given");
            pos = StringSupport.index(this, (RubyString) tmp, pos, this.checkEncoding((RubyString) tmp));
            pos = subLength(pos);
        }

        return pos == -1 ? runtime.getNil() : RubyFixnum.newFixnum(runtime, pos);
    }

    /** rb_str_rindex_m
     *
     */
    public IRubyObject rindex(ThreadContext context, IRubyObject arg0) {
        return rindex19(context, arg0);
    }

    public IRubyObject rindex(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return rindex19(context, arg0, arg1);
    }

    @JRubyMethod(name = "rindex", reads = BACKREF, writes = BACKREF)
    public IRubyObject rindex19(ThreadContext context, IRubyObject arg0) {
        return rindexCommon19(context.runtime, context, arg0, strLength());
    }

    @JRubyMethod(name = "rindex", reads = BACKREF, writes = BACKREF)
    public IRubyObject rindex19(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        int pos = RubyNumeric.num2int(arg1);
        Ruby runtime = context.runtime;
        int length = strLength();
        if (pos < 0) {
            pos += length;
            if (pos < 0) {
                if (arg0 instanceof RubyRegexp) context.setBackRef(runtime.getNil());
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
                                      pos) - value.getBegin();
            if (regSub.length() > 0) {
                pos = regSub.adjustStartPos19(this, pos, true);
                IRubyObject[] holder = {context.nil};
                pos = regSub.search19(context, this, pos, true, holder);
                context.setBackRef(holder[0]);
                pos = subLength(pos);
            }
        } else if (sub instanceof RubyString) {
            pos = StringSupport.rindex(value, StringSupport.strLengthFromRubyString(this, this.checkEncoding((RubyString) sub)), StringSupport.strLengthFromRubyString(((RubyString) sub), this.checkEncoding((RubyString) sub)), pos, (RubyString) sub, this.checkEncoding((RubyString) sub));
        } else {
            IRubyObject tmp = sub.checkStringType();
            if (tmp.isNil()) throw runtime.newTypeError("type mismatch: " + sub.getMetaClass().getName() + " given");
            pos = StringSupport.rindex(value, StringSupport.strLengthFromRubyString(this, this.checkEncoding((RubyString) tmp)), StringSupport.strLengthFromRubyString(((RubyString) tmp), this.checkEncoding((RubyString) tmp)), pos, (RubyString) tmp, this.checkEncoding((RubyString) tmp));
        }
        if (pos >= 0) return RubyFixnum.newFixnum(runtime, pos);
        return runtime.getNil();
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
        return makeShared19(runtime, beg, end - beg);
    }

    /* str_byte_substr */
    private IRubyObject byteSubstr(Ruby runtime, int beg, int len) {
        int length = value.length();

        if (len < 0 || beg > length) return runtime.getNil();

        if (beg < 0) {
            beg += length;
            if (beg < 0) return runtime.getNil();
        }
        if (beg + len > length) len = length - beg;

        if (len <= 0) {
            len = 0;
        }

        return makeShared19(runtime, beg, len);
    }

    /* str_byte_aref */
    private IRubyObject byteARef(Ruby runtime, IRubyObject idx) {
        final int index;

        if (idx instanceof RubyRange){
            int[] begLen = ((RubyRange) idx).begLenInt(getByteList().length(), 0);
            return begLen == null ? runtime.getNil() : byteSubstr(runtime, begLen[0], begLen[1]);
        } else if (idx instanceof RubyFixnum) {
            index = RubyNumeric.fix2int((RubyFixnum)idx);
        } else if (idx.respondsTo("begin") && idx.respondsTo("end")) {
            ThreadContext context = runtime.getCurrentContext();
            IRubyObject begin = idx.callMethod(context, "begin");
            IRubyObject end   = idx.callMethod(context, "end");
            IRubyObject excl  = idx.callMethod(context, "exclude_end?");
            RubyRange range = RubyRange.newRange(context, begin, end, excl.isTrue());

            int[] begLen = range.begLenInt(getByteList().length(), 0);
            return begLen == null ? runtime.getNil() : byteSubstr(runtime, begLen[0], begLen[1]);
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
            if (beg + len > length) len = length - beg;
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
                beg += StringSupport.strLengthFromRubyString(this, enc);
                if (beg < 0) return runtime.getNil();
            }
        } else if (beg > 0 && beg > StringSupport.strLengthFromRubyString(this, enc)) {
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
        StringSupport.replaceInternal(beg, len, this, repl);

        // TODO (nirvdrum 13-Jan-15) This should be part of the StringSupport definition but a general notion of tainted needs to emerge first.
        return infectBy(repl);
    }

    private void replaceInternal19(int beg, int len, RubyString repl) {
        StringSupport.replaceInternal19(beg, len, this, repl);
        infectBy(repl);
    }

    /** rb_str_aref, rb_str_aref_m
     *
     */
    public IRubyObject op_aref(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        return op_aref19(context, arg1, arg2);
    }

    public IRubyObject op_aref(ThreadContext context, IRubyObject arg) {
        return op_aref19(context, arg);
    }

    @JRubyMethod(name = {"[]", "slice"}, reads = BACKREF, writes = BACKREF)
    public IRubyObject op_aref19(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        Ruby runtime = context.runtime;
        if (arg1 instanceof RubyRegexp) return subpat19(runtime, context, (RubyRegexp)arg1, arg2);
        return substr19(runtime, RubyNumeric.num2int(arg1), RubyNumeric.num2int(arg2));
    }

    @JRubyMethod(name = {"[]", "slice"}, reads = BACKREF, writes = BACKREF)
    public IRubyObject op_aref19(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;
        if (arg instanceof RubyFixnum) {
            return op_aref19(runtime, RubyNumeric.fix2int((RubyFixnum)arg));
        } else if (arg instanceof RubyRegexp) {
            return subpat19(runtime, context, (RubyRegexp)arg);
        } else if (arg instanceof RubyString) {
            RubyString str = (RubyString)arg;
            return StringSupport.index(this, str, 0, this.checkEncoding(str)) != -1 ? str.strDup(runtime) : runtime.getNil();
        } else if (arg instanceof RubyRange) {
            int len = strLength();
            int[] begLen = ((RubyRange) arg).begLenInt(len, 0);
            return begLen == null ? runtime.getNil() : substr19(runtime, begLen[0], begLen[1]);
        } else if (arg.respondsTo("begin") && arg.respondsTo("end")) {
            int len = strLength();
            IRubyObject begin = arg.callMethod(context, "begin");
            IRubyObject end   = arg.callMethod(context, "end");
            IRubyObject excl  = arg.callMethod(context, "exclude_end?");
            RubyRange range = RubyRange.newRange(context, begin, end, excl.isTrue());

            int[] begLen = range.begLenInt(len, 0);
            return begLen == null ? runtime.getNil() : substr19(runtime, begLen[0], begLen[1]);
        }
        return op_aref19(runtime, RubyNumeric.num2int(arg));
    }

    @JRubyMethod
    public IRubyObject byteslice(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        return byteSubstr(context.runtime, RubyNumeric.num2int(arg1), RubyNumeric.num2int(arg2));
    }

    @JRubyMethod
    public IRubyObject byteslice(ThreadContext context, IRubyObject arg) {
        return byteARef(context.runtime, arg);
    }

    private IRubyObject op_aref19(Ruby runtime, int idx) {
        IRubyObject str = substr19(runtime, idx, 1);
        return !str.isNil() && ((RubyString) str).value.getRealSize() == 0 ? runtime.getNil() : str;
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

    private void subpatSet19(ThreadContext context, RubyRegexp regexp, IRubyObject backref, IRubyObject repl) {
        Ruby runtime = context.runtime;
        IRubyObject[] holder = {context.nil};

        int result = regexp.search19(context, this, 0, false, holder);
        context.setBackRef(holder[0]);

        if (result < 0) throw runtime.newIndexError("regexp not matched");

        // this cast should be ok, since nil matchdata will be < 0 above
        RubyMatchData match = (RubyMatchData)holder[0];

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
        IRubyObject[] holder = {context.nil};

        int result = regex.search19(context, this, 0, false, holder);
        context.setBackRef(holder[0]);

        if (result >= 0) {
            RubyMatchData match = (RubyMatchData)holder[0];
            return RubyRegexp.nth_match(match.backrefNumber(backref), match);
        }

        return runtime.getNil();
    }

    private IRubyObject subpat19(Ruby runtime, ThreadContext context, RubyRegexp regex) {
        IRubyObject[] holder = {context.nil};

        int result = regex.search19(context, this, 0, false, holder);
        context.setBackRef(holder[0]);

        if (result >= 0) {
            return RubyRegexp.nth_match(0, holder[0]);
        }

        return runtime.getNil();
    }

    /** rb_str_aset, rb_str_aset_m
     *
     */
    public IRubyObject op_aset(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return op_aset19(context, arg0, arg1);
    }

    public IRubyObject op_aset(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return op_aset19(context, arg0, arg1, arg2);
    }

    @JRubyMethod(name = "[]=", reads = BACKREF)
    public IRubyObject op_aset19(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        if (arg0 instanceof RubyFixnum) {
            return op_aset19(context, RubyNumeric.fix2int((RubyFixnum)arg0), arg1);
        } else if (arg0 instanceof RubyRegexp) {
            subpatSet19(context, (RubyRegexp)arg0, null, arg1);
            return arg1;
        } else if (arg0 instanceof RubyString) {
            RubyString orig = (RubyString)arg0;
            int beg = StringSupport.index(this, orig, 0, this.checkEncoding(orig));
            if (beg < 0) throw context.runtime.newIndexError("string not matched");
            beg = subLength(beg);
            replaceInternal19(beg, orig.strLength(), arg1.convertToString());
            return arg1;
        } else if (arg0 instanceof RubyRange) {
            int[] begLen = ((RubyRange) arg0).begLenInt(strLength(), 2);
            replaceInternal19(begLen[0], begLen[1], arg1.convertToString());
            return arg1;
        } else if (arg0.respondsTo("begin") && arg0.respondsTo("end")) {
            IRubyObject begin = arg0.callMethod(context, "begin");
            IRubyObject end   = arg0.callMethod(context, "end");
            IRubyObject excl  = arg0.callMethod(context, "exclude_end?");
            RubyRange rng = RubyRange.newRange(context, begin, end, excl.isTrue());

            int[] begLen = rng.begLenInt(strLength(), 2);
            replaceInternal19(begLen[0], begLen[1], arg1.convertToString());

            return arg1;
        }
        return op_aset19(context, RubyNumeric.num2int(arg0), arg1);
    }

    private IRubyObject op_aset19(ThreadContext context, int idx, IRubyObject arg1) {
        replaceInternal19(checkIndex(idx, strLength()), 1, arg1.convertToString());
        return arg1;
    }

    @JRubyMethod(name = "[]=", reads = BACKREF)
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

    /** rb_str_slice_bang
     *
     */
    public IRubyObject slice_bang(ThreadContext context, IRubyObject arg0) {
        return slice_bang19(context, arg0);
    }

    public IRubyObject slice_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return slice_bang19(context, arg0, arg1);
    }

    @JRubyMethod(name = "slice!", reads = BACKREF, writes = BACKREF)
    public IRubyObject slice_bang19(ThreadContext context, IRubyObject arg0) {
        IRubyObject result = op_aref19(context, arg0);
        if (result.isNil()) {
            modifyCheck(); // keep cr ?
        } else {
            op_aset19(context, arg0, RubyString.newEmptyString(context.runtime));
        }
        return result;
    }

    @JRubyMethod(name = "slice!", reads = BACKREF, writes = BACKREF)
    public IRubyObject slice_bang19(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        IRubyObject result = op_aref19(context, arg0, arg1);
        if (result.isNil()) {
            modifyCheck(); // keep cr ?
        } else {
            op_aset19(context, arg0, arg1, RubyString.newEmptyString(context.runtime));
        }
        return result;
    }

    public IRubyObject succ(ThreadContext context) {
        return succ19(context);
    }

    public IRubyObject succ_bang() {
        return succ_bang19();
    }

    @JRubyMethod(name = {"succ", "next"})
    public IRubyObject succ19(ThreadContext context) {
        Ruby runtime = context.runtime;
        final RubyString str;
        if (value.getRealSize() > 0) {
            str = new RubyString(runtime, getMetaClass(), StringSupport.succCommon(value));
            // TODO: rescan code range ?
        } else {
            str = newEmptyString(runtime, getType(), value.getEncoding());
        }
        return str.infectBy(this);
    }

    @JRubyMethod(name = {"succ!", "next!"})
    public IRubyObject succ_bang19() {
        modifyCheck();
        if (value.getRealSize() > 0) {
            value = StringSupport.succCommon(value);
            shareLevel = SHARE_LEVEL_NONE;
            // TODO: rescan code range ?
        }
        return this;
    }

    /** rb_str_upto_m
     *
     */
    @JRubyMethod(name = "upto")
    public IRubyObject upto19(ThreadContext context, IRubyObject end, Block block) {
        Ruby runtime = context.runtime;
        return block.isGiven() ? uptoCommon19(context, end, false, block) : enumeratorize(runtime, this, "upto", end);
    }

    @JRubyMethod(name = "upto")
    public IRubyObject upto19(ThreadContext context, IRubyObject end, IRubyObject excl, Block block) {
        return block.isGiven() ? uptoCommon19(context, end, excl.isTrue(), block) :
            enumeratorize(context.runtime, this, "upto", new IRubyObject[]{end, excl});
    }

    final IRubyObject uptoCommon19(ThreadContext context, IRubyObject arg, boolean excl, Block block) {
        return uptoCommon19(context, arg, excl, block, false);
    }

    final IRubyObject uptoCommon19(ThreadContext context, IRubyObject arg, boolean excl, Block block, boolean asSymbol) {
        Ruby runtime = context.runtime;
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
        int n = op_cmp(end);
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
    public RubyBoolean include_p(ThreadContext context, IRubyObject obj) {
        return include_p19(context, obj);
    }

    @JRubyMethod(name = "include?")
    public RubyBoolean include_p19(ThreadContext context, IRubyObject obj) {
        Ruby runtime = context.runtime;
        RubyString coerced = obj.convertToString();
        return StringSupport.index(this, coerced, 0, this.checkEncoding(coerced)) == -1 ? runtime.getFalse() : runtime.getTrue();
    }

    @JRubyMethod
    public IRubyObject chr(ThreadContext context) {
        return substr19(context.runtime, 0, 1);
    }

    @JRubyMethod
    public IRubyObject getbyte(ThreadContext context, IRubyObject index) {
        Ruby runtime = context.runtime;
        int i = RubyNumeric.num2int(index);
        if (i < 0) i += value.getRealSize();
        if (i < 0 || i >= value.getRealSize()) return runtime.getNil();
        return RubyFixnum.newFixnum(runtime, value.getUnsafeBytes()[value.getBegin() + i] & 0xff);
    }

    @JRubyMethod
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
    public IRubyObject to_i() {
        return to_i19();
    }

    /** rb_str_to_i
     *
     */
    public IRubyObject to_i(IRubyObject arg0) {
        return to_i19(arg0);
    }

    @JRubyMethod(name = "to_i")
    public IRubyObject to_i19() {
        return stringToInum19(10, false);
    }

    @JRubyMethod(name = "to_i")
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
    public IRubyObject oct(ThreadContext context) {
        return oct19(context);
    }

    @JRubyMethod(name = "oct")
    public IRubyObject oct19(ThreadContext context) {
        if (!value.getEncoding().isAsciiCompatible()) {
            throw context.runtime.newEncodingCompatibilityError("ASCII incompatible encoding: " + value.getEncoding());
        }
        return stringToInum(-8, false);
    }

    /** rb_str_hex
     *
     */
    public IRubyObject hex(ThreadContext context) {
        return hex19(context);
    }

    @JRubyMethod(name = "hex")
    public IRubyObject hex19(ThreadContext context) {
        if (!value.getEncoding().isAsciiCompatible()) {
            throw context.runtime.newEncodingCompatibilityError("ASCII incompatible encoding: " + value.getEncoding());
        }
        return stringToInum19(16, false);
    }

    /** rb_str_to_f
     *
     */
    public IRubyObject to_f() {
        return to_f19();
    }

    @JRubyMethod(name = "to_f")
    public IRubyObject to_f19() {
        return RubyNumeric.str2fnum19(getRuntime(), this, false);
    }

    /** rb_str_split_m
     *
     */
    public RubyArray split(ThreadContext context) {
        return split19(context);
    }

    public RubyArray split(ThreadContext context, IRubyObject arg0) {
        return split19(context, arg0);
    }

    public RubyArray split(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return split19(context, arg0, arg1);
    }

    private void populateCapturesForSplit(Ruby runtime, RubyArray result, Matcher matcher, boolean is19) {
        Region region = matcher.getRegion();
        for (int i = 1; i < region.numRegs; i++) {
            int beg = region.beg[i];
            if (beg == -1) continue;
            result.append(is19 ? makeShared19(runtime, beg, region.end[i] - beg) : makeShared(runtime, beg, region.end[i] - beg));
        }
    }

    @JRubyMethod(name = "split", writes = BACKREF)
    public RubyArray split19(ThreadContext context) {
        return split19(context, context.runtime.getNil());
    }

    @JRubyMethod(name = "split", writes = BACKREF)
    public RubyArray split19(ThreadContext context, IRubyObject arg0) {
        return splitCommon19(arg0, false, 0, 0, context, true);
    }

    @JRubyMethod(name = "split", writes = BACKREF)
    public RubyArray split19(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        final int lim = RubyNumeric.num2int(arg1);
        if (lim <= 0) {
            return splitCommon19(arg0, false, lim, 1, context, true);
        } else {
            if (lim == 1) return value.getRealSize() == 0 ? context.runtime.newArray() : context.runtime.newArray(this);
            return splitCommon19(arg0, true, lim, 1, context, true);
        }
    }

    public RubyArray split19(ThreadContext context, IRubyObject arg0, boolean useBackref) {
        return splitCommon19(arg0, useBackref, flags, flags, context, useBackref);
    }

    private RubyArray splitCommon19(IRubyObject spat, final boolean limit, final int lim, final int i, ThreadContext context, boolean useBackref) {
        final RubyArray result;
        if (spat.isNil() && (spat = context.runtime.getGlobalVariables().get("$;")).isNil()) {
            result = awkSplit19(limit, lim, i);
        } else {
            if (spat instanceof RubyString) {
                ByteList spatValue = ((RubyString)spat).value;
                int len = spatValue.getRealSize();
                Encoding spatEnc = spatValue.getEncoding();
                if (len == 0) {
                    Regex pattern = RubyRegexp.getRegexpFromCache(context.runtime, spatValue, spatEnc, new RegexpOptions());
                    result = regexSplit19(context, pattern, pattern, limit, lim, i, useBackref);
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

                Ruby runtime = context.runtime;
                if (spat instanceof RubyRegexp) {
                    RubyRegexp regexp = (RubyRegexp)spat;
                    pattern = regexp.getPattern();
                    prepared = regexp.preparePattern(this);
                } else {
                    pattern = getStringPattern19(runtime, spat);
                    prepared = RubyRegexp.preparePattern(runtime, pattern, this);
                }
                result = regexSplit19(context, pattern, prepared, limit, lim, i, useBackref);
            }
        }

        if (!limit && lim == 0) {
            while (result.size() > 0 && ((RubyString) result.eltInternal(result.size() - 1)).value.getRealSize() == 0) {
                result.pop(context);
            }
        }

        return result;
    }

    private RubyArray regexSplit19(ThreadContext context, Regex pattern, Regex prepared, boolean limit, int lim, int i, boolean useBackref) {
        Ruby runtime = context.runtime;

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
        while ((end = RubyRegexp.matcherSearch(runtime, matcher, start, range, Option.NONE)) >= 0) {
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
        if (useBackref) context.setBackRef(runtime.getNil());

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
        Ruby runtime = context.runtime;
        if (scanForCodeRange() == CR_BROKEN) throw runtime.newArgumentError("invalid byte sequence in " + value.getEncoding());
        if (spat.scanForCodeRange() == CR_BROKEN) throw runtime.newArgumentError("invalid byte sequence in " + spat.value.getEncoding());

        RubyArray result = runtime.newArray();
        Encoding enc = checkEncoding(spat);
        ByteList pattern = spat.value;

        byte[] patternBytes = pattern.getUnsafeBytes();
        int patternBegin = pattern.getBegin();
        int patternRealSize = pattern.getRealSize();

        byte[] bytes = value.getUnsafeBytes();
        int begin = value.getBegin();
        int realSize = value.getRealSize();

        int e, p = 0;

        while (p < realSize && (e = indexOf(bytes, begin, realSize, patternBytes, patternBegin, patternRealSize, p)) >= 0) {
            int t = enc.rightAdjustCharHead(bytes, p + begin, e + begin, begin + realSize) - begin;
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

    // TODO: make the ByteList version public and use it, rather than copying here
    static int indexOf(byte[] source, int sourceOffset, int sourceCount, byte[] target, int targetOffset, int targetCount, int fromIndex) {
        if (fromIndex >= sourceCount) return (targetCount == 0 ? sourceCount : -1);
        if (fromIndex < 0) fromIndex = 0;
        if (targetCount == 0) return fromIndex;

        byte first  = target[targetOffset];
        int max = sourceOffset + (sourceCount - targetCount);

        for (int i = sourceOffset + fromIndex; i <= max; i++) {
            if (source[i] != first) while (++i <= max && source[i] != first);

            if (i <= max) {
                int j = i + 1;
                int end = j + targetCount - 1;
                for (int k = targetOffset + 1; j < end && source[j] == target[k]; j++, k++);

                if (j == end) return i - sourceOffset;
            }
        }
        return -1;
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
    public IRubyObject scan(ThreadContext context, IRubyObject arg, Block block) {
        return scan19(context, arg, block);
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

    @JRubyMethod(name = "scan", reads = BACKREF, writes = BACKREF)
    public IRubyObject scan19(ThreadContext context, IRubyObject arg, Block block) {
        Ruby runtime = context.runtime;
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
        Ruby runtime = context.runtime;
        byte[]bytes = value.getUnsafeBytes();
        int begin = value.getBegin();
        int len = value.getRealSize();
        int range = begin + len;
        final Matcher matcher = prepared.matcher(bytes, begin, range);

        int end = 0;
        RubyMatchData match = null;
        if (pattern.numberOfCaptures() == 0) {
            while (RubyRegexp.matcherSearch(runtime, matcher, begin + end, range, Option.NONE) >= 0) {
                end = StringSupport.positionEndForScan(value, matcher, enc, begin, range);
                match = RubyRegexp.createMatchData19(context, this, matcher, pattern);
                match.regexp = regexp;
                RubyString substr = makeShared19(runtime, matcher.getBegin(), matcher.getEnd() - matcher.getBegin());
                substr.infectBy(tuFlags);
                match.infectBy(tuFlags);
                context.setBackRef(match);
                block.yield(context, substr);
                modifyCheck(bytes, len, enc);
            }
        } else {
            while (RubyRegexp.matcherSearch(runtime, matcher, begin + end, range, Option.NONE) >= 0) {
                end = StringSupport.positionEndForScan(value, matcher, enc, begin, range);
                match = RubyRegexp.createMatchData19(context, this, matcher, pattern);
                match.regexp = regexp;
                match.infectBy(tuFlags);
                context.setBackRef(match);
                block.yield(context, populateCapturesForScan(runtime, matcher, range, tuFlags, true));
                modifyCheck(bytes, len, enc);
            }
        }
        context.setBackRef(match == null ? runtime.getNil() : match);
        return this;
    }

    private IRubyObject scanNoIter19(ThreadContext context, Regex pattern, Regex prepared, Encoding enc, RubyRegexp regexp, int tuFlags) {
        Ruby runtime = context.runtime;
        byte[]bytes = value.getUnsafeBytes();
        int begin = value.getBegin();
        int range = begin + value.getRealSize();
        final Matcher matcher = prepared.matcher(bytes, begin, range);

        RubyArray ary = runtime.newArray();

        int end = 0;
        if (pattern.numberOfCaptures() == 0) {
            while (RubyRegexp.matcherSearch(runtime, matcher, begin + end, range, Option.NONE) >= 0) {
                end = StringSupport.positionEndForScan(value, matcher, enc, begin, range);
                RubyString substr = makeShared19(runtime, matcher.getBegin(), matcher.getEnd() - matcher.getBegin());
                substr.infectBy(tuFlags);
                ary.append(substr);
            }
        } else {
            while (RubyRegexp.matcherSearch(runtime, matcher, begin + end, range, Option.NONE) >= 0) {
                end = StringSupport.positionEndForScan(value, matcher, enc, begin, range);
                ary.append(populateCapturesForScan(runtime, matcher, range, tuFlags, true));
            }
        }

        if (ary.size() > 0) {
            RubyMatchData match = RubyRegexp.createMatchData19(context, this, matcher, pattern);
            match.regexp = regexp;
            match.infectBy(tuFlags);
            context.setBackRef(match);
        } else {
            context.setBackRef(runtime.getNil());
        }
        return ary;
    }

    @JRubyMethod(name = "start_with?")
    public IRubyObject start_with_p(ThreadContext context) {
        return context.runtime.getFalse();
    }

    @JRubyMethod(name = "start_with?")
    public IRubyObject start_with_p(ThreadContext context, IRubyObject arg) {
        return start_with_pCommon(arg) ? context.runtime.getTrue() : context.runtime.getFalse();
    }

    @JRubyMethod(name = "start_with?", rest = true)
    public IRubyObject start_with_p(ThreadContext context, IRubyObject[]args) {
        for (int i = 0; i < args.length; i++) {
            if (start_with_pCommon(args[i])) return context.runtime.getTrue();
        }
        return context.runtime.getFalse();
    }

    private boolean start_with_pCommon(IRubyObject arg) {
        RubyString otherString = arg.convertToString();

        checkEncoding(otherString);

        int otherLength = otherString.value.getRealSize();

        if (otherLength == 0) {
            // other is '', so return true
            return true;
        }

        if (value.getRealSize() < otherLength) return false;

        return value.startsWith(otherString.value);
    }

    @JRubyMethod(name = "end_with?")
    public IRubyObject end_with_p(ThreadContext context) {
        return context.runtime.getFalse();
    }

    @JRubyMethod(name = "end_with?")
    public IRubyObject end_with_p(ThreadContext context, IRubyObject arg) {
        return end_with_pCommon(arg) ? context.runtime.getTrue() : context.runtime.getFalse();
    }

    @JRubyMethod(name = "end_with?", rest = true)
    public IRubyObject end_with_p(ThreadContext context, IRubyObject[]args) {
        for (int i = 0; i < args.length; i++) {
            if (end_with_pCommon(args[i])) return context.runtime.getTrue();
        }
        return context.runtime.getFalse();
    }

    private boolean end_with_pCommon(IRubyObject arg) {
        RubyString otherString = arg.convertToString();

        Encoding enc = checkEncoding(otherString);

        int otherLength = otherString.value.getRealSize();

        if (otherLength == 0) {
            // other is '', so return true
            return true;
        }

        if (value.getRealSize() < otherLength) return false;

        int p = value.getBegin();
        int end = p + value.getRealSize();
        int s = end - otherLength;

        if (enc.leftAdjustCharHead(value.getUnsafeBytes(), p, s, end) != s) return false;

        return value.endsWith(otherString.value);
    }

    private static final ByteList SPACE_BYTELIST = new ByteList(ByteList.plain(" "));

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
        int padCharLen = StringSupport.strLengthFromRubyString(padStr, enc);
        if (pad.getRealSize() == 0 || padCharLen == 0) throw runtime.newArgumentError("zero width padding");
        int width = RubyFixnum.num2int(arg0);
        RubyString result = justifyCommon(runtime, pad,
                                                   padCharLen,
                                                   padStr.singleByteOptimizable(),
                                                   enc, width, jflag);
        if (RubyFixnum.num2int(result.length19()) > RubyFixnum.num2int(length19())) result.infectBy(padStr);
        int cr = CodeRangeSupport.codeRangeAnd(getCodeRange(), padStr.getCodeRange());
        if (cr != CR_BROKEN) result.setCodeRange(cr);
        return result;
    }

    private RubyString justifyCommon(Ruby runtime, ByteList pad, int padCharLen, boolean padSinglebyte, Encoding enc, int width, int jflag) {
        int len = StringSupport.strLengthFromRubyString(this, enc);
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
        if (RubyFixnum.num2int(result.length19()) > RubyFixnum.num2int(length19())) {
                 result.infectBy(this);
             }
        result.associateEncoding(enc);
        return result;
    }

    /** rb_str_ljust
     *
     */
    public IRubyObject ljust(IRubyObject arg0) {
        return ljust19(arg0);
    }

    public IRubyObject ljust(IRubyObject arg0, IRubyObject arg1) {
        return ljust19(arg0, arg1);
    }

    @JRubyMethod(name = "ljust")
    public IRubyObject ljust19(IRubyObject arg0) {
        return justify19(arg0, 'l');
    }

    @JRubyMethod(name = "ljust")
    public IRubyObject ljust19(IRubyObject arg0, IRubyObject arg1) {
        return justify19(arg0, arg1, 'l');
    }

    /** rb_str_rjust
     *
     */
    public IRubyObject rjust(IRubyObject arg0) {
        return rjust19(arg0);
    }

    public IRubyObject rjust(IRubyObject arg0, IRubyObject arg1) {
        return rjust19(arg0, arg1);
    }

    @JRubyMethod(name = "rjust")
    public IRubyObject rjust19(IRubyObject arg0) {
        return justify19(arg0, 'r');
    }

    @JRubyMethod(name = "rjust")
    public IRubyObject rjust19(IRubyObject arg0, IRubyObject arg1) {
        return justify19(arg0, arg1, 'r');
    }

    /** rb_str_center
     *
     */
    public IRubyObject center(IRubyObject arg0) {
        return center19(arg0);
    }

    public IRubyObject center(IRubyObject arg0, IRubyObject arg1) {
        return center19(arg0, arg1);
    }

    @JRubyMethod(name = "center")
    public IRubyObject center19(IRubyObject arg0) {
        return justify19(arg0, 'c');
    }

    @JRubyMethod(name = "center")
    public IRubyObject center19(IRubyObject arg0, IRubyObject arg1) {
        return justify19(arg0, arg1, 'c');
    }

    @JRubyMethod(reads = BACKREF, writes = BACKREF)
    public IRubyObject partition(ThreadContext context, Block block) {
        return RubyEnumerable.partition(context, this, block);
    }

    @JRubyMethod(reads = BACKREF, writes = BACKREF)
    public IRubyObject partition(ThreadContext context, IRubyObject arg, Block block) {
        Ruby runtime = context.runtime;
        final int pos;
        final RubyString sep;
        if (arg instanceof RubyRegexp) {
            RubyRegexp regex = (RubyRegexp)arg;
            IRubyObject[] holder = {context.nil};

            pos = regex.search19(context, this, 0, false, holder);
            context.setBackRef(holder[0]);
            if (pos < 0) return partitionMismatch(runtime);
            sep = (RubyString)subpat19(runtime, context, regex);
            if (pos == 0 && sep.value.getRealSize() == 0) return partitionMismatch(runtime);
        } else {
            IRubyObject tmp = arg.checkStringType();
            if (tmp.isNil()) throw runtime.newTypeError("type mismatch: " + arg.getMetaClass().getName() + " given");
            sep = (RubyString)tmp;
            pos = StringSupport.index(this, sep, 0, this.checkEncoding(sep));
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

    @JRubyMethod(name = "rpartition", reads = BACKREF, writes = BACKREF)
    public IRubyObject rpartition(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;
        final int pos;
        final RubyString sep;
        if (arg instanceof RubyRegexp) {
            RubyRegexp regex = (RubyRegexp)arg;
            IRubyObject[] holder = {context.nil};

            pos = regex.search19(context, this, value.getRealSize(), true, holder);
            context.setBackRef(holder[0]);

            if (pos < 0) return rpartitionMismatch(runtime);
            sep = (RubyString)RubyRegexp.nth_match(0, holder[0]);
        } else {
            IRubyObject tmp = arg.checkStringType();
            if (tmp.isNil()) throw runtime.newTypeError("type mismatch: " + arg.getMetaClass().getName() + " given");
            sep = (RubyString)tmp;
            pos = StringSupport.rindex(value, StringSupport.strLengthFromRubyString(this, this.checkEncoding(sep)), StringSupport.strLengthFromRubyString(sep, this.checkEncoding(sep)), subLength(value.getRealSize()), sep, this.checkEncoding(sep));
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
    public IRubyObject chop(ThreadContext context) {
        return chop19(context);
    }

    public IRubyObject chop_bang(ThreadContext context) {
        return chop_bang19(context);
    }

    @JRubyMethod(name = "chop")
    public IRubyObject chop19(ThreadContext context) {
        Ruby runtime = context.runtime;
        if (value.getRealSize() == 0) return newEmptyString(runtime, getMetaClass(), value.getEncoding()).infectBy(this);
        return makeShared19(runtime, 0, StringSupport.choppedLength19(this, runtime));
    }

    @JRubyMethod(name = "chop!")
    public IRubyObject chop_bang19(ThreadContext context) {
        modifyCheck();
        Ruby runtime = context.runtime;
        if (value.getRealSize() == 0) return runtime.getNil();
        view(0, StringSupport.choppedLength19(this, runtime));
        if (getCodeRange() != CR_7BIT) clearCodeRange();
        return this;
    }

    public RubyString chomp(ThreadContext context) {
        return chomp19(context);
    }

    public RubyString chomp(ThreadContext context, IRubyObject arg0) {
        return chomp19(context, arg0);
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
    public IRubyObject chomp_bang(ThreadContext context) {
        return chomp_bang19(context);
    }

    public IRubyObject chomp_bang(ThreadContext context, IRubyObject arg0) {
        return chomp_bang19(context, arg0);
    }

    @JRubyMethod(name = "chomp")
    public RubyString chomp19(ThreadContext context) {
        RubyString str = strDup(context.runtime);
        str.chomp_bang19(context);
        return str;
    }

    @JRubyMethod(name = "chomp")
    public RubyString chomp19(ThreadContext context, IRubyObject arg0) {
        RubyString str = strDup(context.runtime);
        str.chomp_bang19(context, arg0);
        return str;
    }

    @JRubyMethod(name = "chomp!")
    public IRubyObject chomp_bang19(ThreadContext context) {
        Ruby runtime = context.runtime;
        if (value.getRealSize() == 0) return runtime.getNil();

        IRubyObject rsObj = runtime.getGlobalVariables().get("$/");

        if (rsObj == runtime.getGlobalVariables().getDefaultSeparator()) return smartChopBangCommon19(runtime);
        return chompBangCommon19(runtime, rsObj);
    }

    @JRubyMethod(name = "chomp!")
    public IRubyObject chomp_bang19(ThreadContext context, IRubyObject arg0) {
        modifyCheck();
        Ruby runtime = context.runtime;
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
    public IRubyObject lstrip(ThreadContext context) {
        return lstrip19(context);
    }

    public IRubyObject lstrip_bang(ThreadContext context) {
        return lstrip_bang19(context);
    }

    @JRubyMethod(name = "lstrip")
    public IRubyObject lstrip19(ThreadContext context) {
        RubyString str = strDup(context.runtime);
        str.lstrip_bang19(context);
        return str;
    }

    @JRubyMethod(name = "lstrip!")
    public IRubyObject lstrip_bang19(ThreadContext context) {
        modifyCheck();
        Ruby runtime = context.runtime;
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
    public IRubyObject rstrip(ThreadContext context) {
        return rstrip19(context);
    }

    public IRubyObject rstrip_bang(ThreadContext context) {
        return rstrip_bang19(context);
    }

    @JRubyMethod(name = "rstrip")
    public IRubyObject rstrip19(ThreadContext context) {
        RubyString str = strDup(context.runtime);
        str.rstrip_bang19(context);
        return str;
    }

    @JRubyMethod(name = "rstrip!")
    public IRubyObject rstrip_bang19(ThreadContext context) {
        modifyCheck();
        Ruby runtime = context.runtime;
        if (value.getRealSize() == 0) {
            return runtime.getNil();
        }

        IRubyObject result = singleByteOptimizable(value.getEncoding()) ?
            singleByteRStrip19(runtime) : multiByteRStrip19(runtime);

        keepCodeRange();
        return result;
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
    public IRubyObject strip(ThreadContext context) {
        return strip19(context);
    }

    public IRubyObject strip_bang(ThreadContext context) {
        return strip_bang19(context);
    }

    @JRubyMethod(name = "strip")
    public IRubyObject strip19(ThreadContext context) {
        RubyString str = strDup(context.runtime);
        str.strip_bang19(context);
        return str;
    }

    @JRubyMethod(name = "strip!")
    public IRubyObject strip_bang19(ThreadContext context) {
        modifyCheck();

        IRubyObject left = lstrip_bang19(context);
        IRubyObject right = rstrip_bang19(context);

        return left.isNil() && right.isNil() ? context.runtime.getNil() : this;
    }

    /** rb_str_count
     *
     */
    public IRubyObject count(ThreadContext context) {
        return count19(context);
    }

    public IRubyObject count(ThreadContext context, IRubyObject arg) {
        return count19(context, arg);
    }

    public IRubyObject count(ThreadContext context, IRubyObject[] args) {
        return count19(context, args);
    }

    @JRubyMethod(name = "count")
    public IRubyObject count19(ThreadContext context) {
        throw context.runtime.newArgumentError("wrong number of arguments");
    }

    @JRubyMethod(name = "count")
    public IRubyObject count19(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;
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

        final boolean[]table = new boolean[StringSupport.TRANS_SIZE + 1];
        StringSupport.TrTables tables = StringSupport.trSetupTable(otherStr.value, context.runtime, table, null, true, enc);
        return runtime.newFixnum(StringSupport.countCommon19(value, runtime, table, tables, enc));
    }

    @JRubyMethod(name = "count", required = 1, rest = true)
    public IRubyObject count19(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        if (value.getRealSize() == 0) return RubyFixnum.zero(runtime);

        RubyString otherStr = args[0].convertToString();
        Encoding enc = checkEncoding(otherStr);
        final boolean[]table = new boolean[StringSupport.TRANS_SIZE + 1];
        StringSupport.TrTables tables = StringSupport.trSetupTable(otherStr.value, runtime, table, null, true, enc);
        for (int i = 1; i<args.length; i++) {
            otherStr = args[i].convertToString();
            enc = checkEncoding(otherStr);
            tables = StringSupport.trSetupTable(otherStr.value, runtime, table, tables, false, enc);
        }

        return runtime.newFixnum(StringSupport.countCommon19(value, runtime, table, tables, enc));
    }

    /** rb_str_delete / rb_str_delete_bang
     *
     */
    public IRubyObject delete(ThreadContext context) {
        return delete19(context);
    }

    public IRubyObject delete(ThreadContext context, IRubyObject arg) {
        return delete19(context, arg);
    }

    public IRubyObject delete(ThreadContext context, IRubyObject[] args) {
        return delete19(context, args);
    }

    public IRubyObject delete_bang(ThreadContext context) {
        return delete_bang19(context);
    }

    public IRubyObject delete_bang(ThreadContext context, IRubyObject arg) {
        return delete_bang19(context, arg);
    }

    public IRubyObject delete_bang(ThreadContext context, IRubyObject[] args) {
        return delete_bang19(context, args);
    }

    @JRubyMethod(name = "delete")
    public IRubyObject delete19(ThreadContext context) {
        throw context.runtime.newArgumentError("wrong number of arguments");
    }

    @JRubyMethod(name = "delete")
    public IRubyObject delete19(ThreadContext context, IRubyObject arg) {
        RubyString str = strDup(context.runtime);
        str.delete_bang19(context, arg);
        return str;
    }

    @JRubyMethod(name = "delete", required = 1, rest = true)
    public IRubyObject delete19(ThreadContext context, IRubyObject[] args) {
        RubyString str = strDup(context.runtime);
        str.delete_bang19(context, args);
        return str;
    }

    @JRubyMethod(name = "delete!")
    public IRubyObject delete_bang19(ThreadContext context) {
        throw context.runtime.newArgumentError("wrong number of arguments");
    }

    @JRubyMethod(name = "delete!")
    public IRubyObject delete_bang19(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;
        if (value.getRealSize() == 0) return runtime.getNil();

        RubyString otherStr = arg.convertToString();
        Encoding enc = checkEncoding(otherStr);
        final boolean[]squeeze = new boolean[StringSupport.TRANS_SIZE + 1];
        StringSupport.TrTables tables = StringSupport.trSetupTable(otherStr.value, runtime, squeeze, null, true, enc);

        if (StringSupport.delete_bangCommon19(this, runtime, squeeze, tables, enc) == null) {
            return runtime.getNil();
        }

        return this;
    }

    @JRubyMethod(name = "delete!", required = 1, rest = true)
    public IRubyObject delete_bang19(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        if (value.getRealSize() == 0) return runtime.getNil();

        RubyString otherStr = args[0].convertToString();
        Encoding enc = checkEncoding(otherStr);
        boolean[]squeeze = new boolean[StringSupport.TRANS_SIZE + 1];
        StringSupport.TrTables tables = StringSupport.trSetupTable(otherStr.value, runtime, squeeze, null, true, enc);
        for (int i=1; i<args.length; i++) {
            otherStr = args[i].convertToString();
            enc = checkEncoding(otherStr);
            tables = StringSupport.trSetupTable(otherStr.value, runtime, squeeze, tables, false, enc);
        }

        if (StringSupport.delete_bangCommon19(this, runtime, squeeze, tables, enc) == null) {
            return runtime.getNil();
        }

        return this;
    }

    /** rb_str_squeeze / rb_str_squeeze_bang
     *
     */
    public IRubyObject squeeze(ThreadContext context) {
        return squeeze19(context);
    }

    public IRubyObject squeeze(ThreadContext context, IRubyObject arg) {
        return squeeze19(context, arg);
    }

    public IRubyObject squeeze(ThreadContext context, IRubyObject[] args) {
        return squeeze19(context, args);
    }

    public IRubyObject squeeze_bang(ThreadContext context) {
        return squeeze_bang19(context);
    }

    public IRubyObject squeeze_bang(ThreadContext context, IRubyObject arg) {
        return squeeze_bang19(context, arg);
    }

    public IRubyObject squeeze_bang(ThreadContext context, IRubyObject[] args) {
        return squeeze_bang19(context, args);
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

    @JRubyMethod(name = "squeeze")
    public IRubyObject squeeze19(ThreadContext context) {
        RubyString str = strDup(context.runtime);
        str.squeeze_bang19(context);
        return str;
    }

    @JRubyMethod(name = "squeeze")
    public IRubyObject squeeze19(ThreadContext context, IRubyObject arg) {
        RubyString str = strDup(context.runtime);
        str.squeeze_bang19(context, arg);
        return str;
    }

    @JRubyMethod(name = "squeeze", rest = true)
    public IRubyObject squeeze19(ThreadContext context, IRubyObject[] args) {
        RubyString str = strDup(context.runtime);
        str.squeeze_bang19(context, args);
        return str;
    }

    @JRubyMethod(name = "squeeze!")
    public IRubyObject squeeze_bang19(ThreadContext context) {
        Ruby runtime = context.runtime;
        if (value.getRealSize() == 0) {
            modifyCheck();
            return runtime.getNil();
        }
        final boolean squeeze[] = new boolean[StringSupport.TRANS_SIZE];
        for (int i=0; i< StringSupport.TRANS_SIZE; i++) squeeze[i] = true;

        modifyAndKeepCodeRange();
        if (singleByteOptimizable()) {
            return squeezeCommon(runtime, squeeze); // 1.8
        } else {
            return squeezeCommon19(runtime, squeeze, null, value.getEncoding(), false);
        }
    }

    @JRubyMethod(name = "squeeze!")
    public IRubyObject squeeze_bang19(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;
        if (value.getRealSize() == 0) {
            modifyCheck();
            return runtime.getNil();
        }

        RubyString otherStr = arg.convertToString();
        final boolean squeeze[] = new boolean[StringSupport.TRANS_SIZE + 1];
        StringSupport.TrTables tables = StringSupport.trSetupTable(otherStr.value, runtime, squeeze, null, true, checkEncoding(otherStr));

        modifyAndKeepCodeRange();
        if (singleByteOptimizable() && otherStr.singleByteOptimizable()) {
            return squeezeCommon(runtime, squeeze); // 1.8
        } else {
            return squeezeCommon19(runtime, squeeze, tables, value.getEncoding(), true);
        }

    }

    @JRubyMethod(name = "squeeze!", rest = true)
    public IRubyObject squeeze_bang19(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        if (value.getRealSize() == 0) {
            modifyCheck();
            return runtime.getNil();
        }

        RubyString otherStr = args[0].convertToString();
        Encoding enc = checkEncoding(otherStr);
        final boolean squeeze[] = new boolean[StringSupport.TRANS_SIZE + 1];
        StringSupport.TrTables tables = StringSupport.trSetupTable(otherStr.value, runtime, squeeze, null, true, enc);

        boolean singlebyte = singleByteOptimizable() && otherStr.singleByteOptimizable();
        for (int i=1; i<args.length; i++) {
            otherStr = args[i].convertToString();
            enc = checkEncoding(otherStr);
            singlebyte = singlebyte && otherStr.singleByteOptimizable();
            tables = StringSupport.trSetupTable(otherStr.value, runtime, squeeze, tables, false, enc);
        }

        modifyAndKeepCodeRange();
        if (singlebyte) {
            return squeezeCommon(runtime, squeeze); // 1.8
        } else {
            return squeezeCommon19(runtime, squeeze, tables, enc, true);
        }
    }

    private IRubyObject squeezeCommon19(Ruby runtime, boolean squeeze[], StringSupport.TrTables tables, Encoding enc, boolean isArg) {
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
                if (c != save || (isArg && !StringSupport.trFind(c, squeeze, tables))) {
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
    public IRubyObject tr(ThreadContext context, IRubyObject src, IRubyObject repl) {
        return tr19(context, src, repl);
    }

    public IRubyObject tr_bang(ThreadContext context, IRubyObject src, IRubyObject repl) {
        return tr_bang19(context, src, repl);
    }

    @JRubyMethod(name = "tr")
    public IRubyObject tr19(ThreadContext context, IRubyObject src, IRubyObject repl) {
        RubyString str = strDup(context.runtime);
        str.trTrans19(context, src, repl, false);
        return str;
    }

    @JRubyMethod(name = "tr!")
    public IRubyObject tr_bang19(ThreadContext context, IRubyObject src, IRubyObject repl) {
        return trTrans19(context, src, repl, false);
    }

    private IRubyObject trTrans19(ThreadContext context, IRubyObject src, IRubyObject repl, boolean sflag) {
        Ruby runtime = context.runtime;
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

        final StringSupport.TR trSrc = new StringSupport.TR(srcList);
        boolean cflag = false;
        if (value.getRealSize() > 0) {
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
        final int[]trans = new int[StringSupport.TRANS_SIZE];
        IntHash<Integer> hash = null;
        final StringSupport.TR trRepl = new StringSupport.TR(replList);

        int last = 0;
        if (cflag) {
            for (int i=0; i< StringSupport.TRANS_SIZE; i++) trans[i] = 1;

            while ((c = StringSupport.trNext(trSrc, runtime, enc)) >= 0) {
                if (c < StringSupport.TRANS_SIZE) {
                    trans[c & 0xff] = -1;
                } else {
                    if (hash == null) hash = new IntHash<Integer>();
                    hash.put(c, 1); // QTRUE
                }
            }
            while ((c = StringSupport.trNext(trRepl, runtime, enc)) >= 0) {}  /* retrieve last replacer */
            last = trRepl.now;
            for (int i=0; i< StringSupport.TRANS_SIZE; i++) {
                if (trans[i] >= 0) trans[i] = last;
            }
        } else {
            for (int i=0; i< StringSupport.TRANS_SIZE; i++) trans[i] = -1;

            while ((c = StringSupport.trNext(trSrc, runtime, enc)) >= 0) {
                int r = StringSupport.trNext(trRepl, runtime, enc);
                if (r == -1) r = trRepl.now;
                if (c < StringSupport.TRANS_SIZE) {
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
        if (c < StringSupport.TRANS_SIZE) {
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

    /** rb_str_tr_s / rb_str_tr_s_bang
     *
     */
    public IRubyObject tr_s(ThreadContext context, IRubyObject src, IRubyObject repl) {
        return tr_s19(context, src, repl);
    }

    public IRubyObject tr_s_bang(ThreadContext context, IRubyObject src, IRubyObject repl) {
        return tr_s_bang19(context, src, repl);
    }

    @JRubyMethod(name = "tr_s")
    public IRubyObject tr_s19(ThreadContext context, IRubyObject src, IRubyObject repl) {
        RubyString str = strDup(context.runtime);
        str.trTrans19(context, src, repl, true);
        return str;
    }

    @JRubyMethod(name = "tr_s!")
    public IRubyObject tr_s_bang19(ThreadContext context, IRubyObject src, IRubyObject repl) {
        return trTrans19(context, src, repl, true);
    }

    /** rb_str_each_line
     *
     */
    public IRubyObject each_line(ThreadContext context, Block block) {
        return each_lineCommon(context, context.runtime.getGlobalVariables().get("$/"), block);
    }

    public IRubyObject each_line(ThreadContext context, IRubyObject arg, Block block) {
        return each_lineCommon(context, arg, block);
    }

    public IRubyObject each_lineCommon(ThreadContext context, IRubyObject sep, Block block) {
        Ruby runtime = context.runtime;
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

    @JRubyMethod(name = "each_line")
    public IRubyObject each_line19(ThreadContext context, Block block) {
        return block.isGiven() ? each_lineCommon19(context, block) :
            enumeratorize(context.runtime, this, "each_line");
    }

    @JRubyMethod(name = "each_line")
    public IRubyObject each_line19(ThreadContext context, IRubyObject arg, Block block) {
        return block.isGiven() ? each_lineCommon19(context, arg, block) :
            enumeratorize(context.runtime, this, "each_line", arg);
    }

    public IRubyObject lines(ThreadContext context, Block block) {
        return lines20(context, block);
    }

    public IRubyObject lines(ThreadContext context, IRubyObject arg, Block block) {
        return lines20(context, arg, block);
    }

    @JRubyMethod(name = "lines")
    public IRubyObject lines20(ThreadContext context, Block block) {
        if (block.isGiven()) {
            context.runtime.getWarnings().warn("passing a block to String#lines is deprecated");
            return each_lineCommon19(context, block);
        }
        // FIXME: Inefficient; build array manually rather than via Enumerator
        return enumeratorize(context.runtime, this, "each_line").callMethod(context, "to_a");
    }

    @JRubyMethod(name = "lines")
    public IRubyObject lines20(ThreadContext context, IRubyObject arg, Block block) {
        if (block.isGiven()) {
            context.runtime.getWarnings().warn("passing a block to String#lines is deprecated");
            return each_lineCommon19(context, arg, block);
        }
        // FIXME: Inefficient; build array manually rather than via Enumerator
        return enumeratorize(context.runtime, this, "each_line", arg).callMethod(context, "to_a");
    }

    private IRubyObject each_lineCommon19(ThreadContext context, Block block) {
        return each_lineCommon19(context, context.runtime.getGlobalVariables().get("$/"), block);
    }

    private IRubyObject each_lineCommon19(ThreadContext context, IRubyObject sep, Block block) {
        Ruby runtime = context.runtime;
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
                    if (c == newLine &&
                            rslen <= end - p &&
                            (rslen <= 1 ||
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
        Ruby runtime = context.runtime;
        // Check the length every iteration, since
        // the block can modify this string.
        for (int i = 0; i < value.length(); i++) {
            block.yield(context, runtime.newFixnum(value.get(i) & 0xFF));
        }
        return this;
    }

    @JRubyMethod(name = "each_byte")
    public IRubyObject each_byte19(ThreadContext context, Block block) {
        return enumerateBytes(context, "each_byte", block, false);
    }

    @JRubyMethod
    public IRubyObject bytes(ThreadContext context, Block block) {
        return enumerateBytes(context, "bytes", block, true);
    }

    @JRubyMethod(name = "each_char")
    public IRubyObject each_char19(ThreadContext context, Block block) {
        return enumerateChars(context, "each_char", block, false);
    }

    @JRubyMethod(name = "chars")
    public IRubyObject chars19(ThreadContext context, Block block) {
        return enumerateChars(context, "chars", block, true);
    }

    private SizeFn eachCharSizeFn() {
        final RubyString self = this;
        return new SizeFn() {
            @Override
            public IRubyObject size(IRubyObject[] args) {
                return self.length();
            }
        };
    }

    /** rb_str_each_codepoint
     *
     */
    @JRubyMethod
    public IRubyObject each_codepoint(ThreadContext context, Block block) {
        return enumerateCodepoints(context, "each_codepoint", block, false);
    }

    @JRubyMethod
    public IRubyObject codepoints(ThreadContext context, Block block) {
        return enumerateCodepoints(context, "codepoints", block, true);
    }

    // MRI: rb_str_enumerate_chars
    private IRubyObject enumerateChars(ThreadContext context, String name, Block block, boolean wantarray) {
        Ruby runtime = context.runtime;
        RubyString str = this;
        IRubyObject orig = str;
        IRubyObject substr;
        int i, len, n;
        byte[] ptrBytes;
        int ptr;
        Encoding enc;
        RubyArray ary = null;

        str = RubyString.newString(runtime, str.getByteList().dup());
        ByteList strByteList = str.getByteList();
        ptrBytes = strByteList.unsafeBytes();
        ptr = strByteList.begin();
        len = strByteList.getRealSize();
        enc = str.getEncoding();

        if (block.isGiven()) {
            if (wantarray) {
                // this code should be live in 3.0
                if (false) { // #if STRING_ENUMERATORS_WANTARRAY
                    runtime.getWarnings().warn("given block not used");
                    ary = RubyArray.newArray(runtime, str.length().getLongValue());
                } else {
                    runtime.getWarnings().warning("passing a block to String#chars is deprecated");
                    wantarray = false;
                }
            }
        }
        else {
            if (wantarray)
                ary = RubyArray.newArray(runtime, str.length().getLongValue());
            else
                return enumeratorizeWithSize(context, this, name, eachCharSizeFn());
        }

        switch (getCodeRange()) {
            case CR_VALID:
            case CR_7BIT:
                for (i = 0; i < len; i += n) {
                    n = StringSupport.encFastMBCLen(ptrBytes, ptr + i, ptr + len, enc);
                    substr = str.substr(runtime, i, n);
                    if (wantarray)
                        ary.push(substr);
                    else
                        block.yield(context, substr);
                }
                break;
            default:
                for (i = 0; i < len; i += n) {
                    n = StringSupport.length(enc, ptrBytes, ptr + i, ptr + len);
                    substr = str.substr(runtime, i, n);
                    if (wantarray)
                        ary.push(substr);
                    else
                        block.yield(context, substr);
                }
        }
        if (wantarray)
            return ary;
        else
            return orig;
    }

    // MRI: rb_str_enumerate_codepoints
    private IRubyObject enumerateCodepoints(ThreadContext context, String name, Block block, boolean wantarray) {
        Ruby runtime = context.runtime;
        RubyString str = this;
        IRubyObject orig = str;
        int n;
        int c;
        byte[] ptrBytes;
        int ptr, end;
        Encoding enc;
        RubyArray ary = null;

        if (singleByteOptimizable())
            return enumerateBytes(context, name, block, wantarray);

        str = RubyString.newString(runtime, str.getByteList().dup());
        ByteList strByteList = str.getByteList();
        ptrBytes = strByteList.unsafeBytes();
        ptr = strByteList.begin();
        end = ptr + strByteList.getRealSize();
        enc = str.getEncoding();

        if (block.isGiven()) {
            if (wantarray) {
                // this code should be live in 3.0
                if (false) { // #if STRING_ENUMERATORS_WANTARRAY
                    runtime.getWarnings().warn("given block not used");
                    ary = RubyArray.newArray(runtime, str.length().getLongValue());
                } else {
                    runtime.getWarnings().warning("passing a block to String#codepoints is deprecated");
                    wantarray = false;
                }
            }
        }
        else {
            if (wantarray)
                ary = RubyArray.newArray(runtime, str.length().getLongValue());
            else
                return enumeratorizeWithSize(context, str, name, eachCodepointSizeFn());
        }

        while (ptr < end) {
            c = codePoint(runtime, enc, ptrBytes, ptr, end);
            n = codeLength(runtime, enc, c);
            if (wantarray)
                ary.push(RubyFixnum.newFixnum(runtime, c));
            else
                block.yield(context, RubyFixnum.newFixnum(runtime, c));
            ptr += n;
        }
        if (wantarray)
            return ary;
        else
            return orig;
    }

    private IRubyObject enumerateBytes(ThreadContext context, String name, Block block, boolean wantarray) {
        Ruby runtime = context.runtime;
        RubyString str = this;
        int i;
        RubyArray ary = null;

        if (block.isGiven()) {
            if (wantarray) {
                // this code should be live in 3.0
                if (false) { // #if STRING_ENUMERATORS_WANTARRAY
                    runtime.getWarnings().warn("given block not used");
                    ary = RubyArray.newArray(runtime);
                } else {
                    runtime.getWarnings().warning("passing a block to String#bytes is deprecated");
                    wantarray = false;
                }
            }
        }
        else {
            if (wantarray)
                ary = RubyArray.newArray(runtime, str.size());
            else
                return enumeratorizeWithSize(context, str, name, eachByteSizeFn());
        }

        for (i=0; i<str.size(); i++) {
            RubyFixnum bite = RubyFixnum.newFixnum(runtime, str.getByteList().get(i) & 0xff);
            if (wantarray)
                ary.push(bite);
            else
                block.yield(context, bite);
        }
        if (wantarray)
            return ary;
        else
            return str;
    }

    private SizeFn eachCodepointSizeFn() {
        final RubyString self = this;
        return new SizeFn() {
            @Override
            public IRubyObject size(IRubyObject[] args) {
                return self.length();
            }
        };
    }

    /** rb_str_intern
     *
     */
    private RubySymbol to_sym() {
        RubySymbol specialCaseIntern = checkSpecialCasesIntern(value);
        if (specialCaseIntern != null) return specialCaseIntern;

        RubySymbol symbol = getRuntime().getSymbolTable().getSymbol(value);
        if (symbol.getBytes() == value) shareLevel = SHARE_LEVEL_BYTELIST;
        return symbol;
    }

    private RubySymbol checkSpecialCasesIntern(ByteList value) {
        String[][] opTable = opTable19;

        for (int i = 0; i < opTable.length; i++) {
            String op = opTable[i][1];
            if (value.toString().equals(op)) {
                return getRuntime().getSymbolTable().getSymbol(opTable[i][0]);
            }
        }

        return null;
    }

    public RubySymbol intern() {
        return intern19();
    }

    @JRubyMethod(name = {"to_sym", "intern"})
    public RubySymbol intern19() {
        return to_sym();
    }

    @JRubyMethod
    public IRubyObject ord(ThreadContext context) {
        Ruby runtime = context.runtime;
        return RubyFixnum.newFixnum(runtime, codePoint(runtime, value.getEncoding(), value.getUnsafeBytes(), value.getBegin(),
                                                                value.getBegin() + value.getRealSize()));
    }

    @JRubyMethod
    public IRubyObject sum(ThreadContext context) {
        return sumCommon(context, 16);
    }

    @JRubyMethod
    public IRubyObject sum(ThreadContext context, IRubyObject arg) {
        return sumCommon(context, RubyNumeric.num2long(arg));
    }

    public IRubyObject sumCommon(ThreadContext context, long bits) {
        Ruby runtime = context.runtime;

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
    @JRubyMethod
    public IRubyObject to_c(ThreadContext context) {
        Ruby runtime = context.runtime;

        RubyString underscore = runtime.newString(new ByteList(new byte[]{'_'}));
        RubyRegexp underscore_pattern = RubyRegexp.newDummyRegexp(runtime, Numeric.ComplexPatterns.underscores_pat);
        IRubyObject s = this.gsubCommon19(context, null, underscore, null, underscore_pattern, false, 0, false);

        RubyArray a = RubyComplex.str_to_c_internal(context, s);

        if (!a.eltInternal(0).isNil()) {
            return a.eltInternal(0);
        } else {
            return RubyComplex.newComplexCanonicalize(context, RubyFixnum.zero(runtime));
        }
    }

    /** string_to_r
     *
     */
    @JRubyMethod
    public IRubyObject to_r(ThreadContext context) {
        Ruby runtime = context.runtime;

        RubyString underscore = runtime.newString(new ByteList(new byte[]{'_'}));
        RubyRegexp underscore_pattern = RubyRegexp.newDummyRegexp(runtime, Numeric.ComplexPatterns.underscores_pat);
        IRubyObject s = this.gsubCommon19(context, null, underscore, null, underscore_pattern, false, 0, false);

        RubyArray a = RubyRational.str_to_r_internal(context, s);

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

    @JRubyMethod
    public IRubyObject encoding(ThreadContext context) {
        return context.runtime.getEncodingService().getEncoding(value.getEncoding());
    }

    // TODO: re-split this
    public IRubyObject encode_bang(ThreadContext context, IRubyObject arg0) {
        return encode_bang(context, new IRubyObject[]{arg0});
    }

    public IRubyObject encode_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return encode_bang(context, new IRubyObject[]{arg0,arg1});
    }

    public IRubyObject encode_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return encode_bang(context, new IRubyObject[]{arg0,arg1,arg2});
    }

    @JRubyMethod(name = "encode!", optional = 3)
    public IRubyObject encode_bang(ThreadContext context, IRubyObject[] args) {
        IRubyObject[] newstr_p;
        Encoding encindex;

        modify19();

        newstr_p = new IRubyObject[]{this};
        encindex = EncodingUtils.strTranscode(context, args, newstr_p);

        if (encindex == null) return this;
        if (newstr_p[0] == this) {
            this.setEncoding(encindex);
            return this;
        }
        replace(newstr_p[0]);
        this.setEncoding(encindex);
        return this;
    }

    @JRubyMethod
    public IRubyObject encode(ThreadContext context) {
        return EncodingUtils.strEncode(context, this);
    }

    @JRubyMethod
    public IRubyObject encode(ThreadContext context, IRubyObject arg) {
        return EncodingUtils.strEncode(context, this, arg);
    }

    @JRubyMethod
    public IRubyObject encode(ThreadContext context, IRubyObject toEncoding, IRubyObject arg) {
        return EncodingUtils.strEncode(context, this, toEncoding, arg);
    }

    @JRubyMethod
    public IRubyObject encode(ThreadContext context, IRubyObject toEncoding,
            IRubyObject forcedEncoding, IRubyObject opts) {

        return EncodingUtils.strEncode(context, this, toEncoding, forcedEncoding, opts);
    }

    @JRubyMethod
    public IRubyObject force_encoding(ThreadContext context, IRubyObject enc) {
        return force_encoding(context, context.runtime.getEncodingService().getEncodingFromObject(enc));
    }

    private IRubyObject force_encoding(ThreadContext context, Encoding encoding) {
        modify19();
        associateEncoding(encoding);
        clearCodeRange();
        return this;
    }

    @JRubyMethod(name = "valid_encoding?")
    public IRubyObject valid_encoding_p(ThreadContext context) {
        return context.runtime.newBoolean(scanForCodeRange() != CR_BROKEN);
    }

    @JRubyMethod(name = "ascii_only?")
    public IRubyObject ascii_only_p(ThreadContext context) {
        return context.runtime.newBoolean(scanForCodeRange() == CR_7BIT);
    }

    @JRubyMethod
    public IRubyObject b(ThreadContext context) {
        Encoding encoding = ASCIIEncoding.INSTANCE;
        RubyString dup = (RubyString)dup();
        dup.associateEncoding(encoding);
        dup.clearCodeRange();
        return dup;
    }

    // MRI: str_scrub arity 0
    @JRubyMethod
    public IRubyObject scrub(ThreadContext context, Block block) {
        return scrub(context, context.nil, block);
    }

    // MRI: str_scrub arity 1
    @JRubyMethod
    public IRubyObject scrub(ThreadContext context, IRubyObject repl, Block block) {
        IRubyObject newStr = strScrub(context, repl, block);
        if (newStr.isNil()) return strDup(context.runtime);
        return newStr;
    }

    @JRubyMethod(name="scrub!")
    public IRubyObject scrub_bang(ThreadContext context, Block block) {
        return scrub_bang(context, context.nil, block);
    }

    // MRI: str_scrub arity 1
    @JRubyMethod(name="scrub!")
    public IRubyObject scrub_bang(ThreadContext context, IRubyObject repl, Block block) {
        IRubyObject newStr = strScrub(context, repl, block);
        if (!newStr.isNil()) return replace(newStr);
        return this;
    }

    @JRubyMethod
    public IRubyObject freeze(ThreadContext context) {
        if (isFrozen()) return this;
        resize(size());
        return super.freeze(context);
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

    public static ByteList encodeBytelist(CharSequence value, Encoding encoding) {

        Charset charset = encoding.getCharset();

        // if null charset, fall back on Java default charset
        if (charset == null) charset = Charset.defaultCharset();

        byte[] bytes;
        if (charset == RubyEncoding.UTF8) {
            bytes = RubyEncoding.encodeUTF8(value);
        } else if (charset == RubyEncoding.UTF16) {
            bytes = RubyEncoding.encodeUTF16(value);
        } else {
            bytes = RubyEncoding.encode(value, charset);
        }

        return new ByteList(bytes, encoding, false);
    }

    @Override
    public Object toJava(Class target) {
        if (target.isAssignableFrom(String.class)) {
            return decodeString();
        }
        if (target.isAssignableFrom(ByteList.class)) {
            return value;
        }
        if (target == Character.class || target == Character.TYPE) {
            if ( strLength() != 1 ) {
                throw getRuntime().newArgumentError("could not coerce string of length " + strLength() + " (!= 1) into a char");
            }
            return decodeString().charAt(0);
        }
        return super.toJava(target);
    }

    /**
     * Scrub the contents of this string, replacing invalid characters as appropriate.
     *
     * MRI: rb_str_scrub
     */
    public IRubyObject strScrub(ThreadContext context, IRubyObject repl, Block block) {
        Ruby runtime = context.runtime;
        int cr = getCodeRange();
        Encoding enc;
        Encoding encidx;

        if (cr == CR_7BIT || cr == CR_VALID)
            return context.nil;

        enc = getEncoding();
        if (!repl.isNil()) {
            repl = EncodingUtils.strCompatAndValid(context, repl, enc);
        }

        if (enc.isDummy()) {
            return context.nil;
        }
        encidx = enc;

        if (enc.isAsciiCompatible()) {
            byte[] pBytes = value.unsafeBytes();
            int p = value.begin();
            int e = p + value.getRealSize();
            int p1 = p;
            byte[] repBytes;
            int rep;
            int replen;
            boolean rep7bit_p;
            IRubyObject buf = context.nil;
            if (block.isGiven()) {
                repBytes = null;
                rep = 0;
                replen = 0;
                rep7bit_p = false;
            }
            else if (!repl.isNil()) {
                repBytes = ((RubyString)repl).value.unsafeBytes();
                rep = ((RubyString)repl).value.begin();
                replen = ((RubyString)repl).value.getRealSize();
                rep7bit_p = (((RubyString)repl).getCodeRange() == CR_7BIT);
            }
            else if (encidx == UTF8Encoding.INSTANCE) {
                repBytes = SCRUB_REPL_UTF8;
                rep = 0;
                replen = repBytes.length;
                rep7bit_p = false;
            }
            else {
                repBytes = SCRUB_REPL_ASCII;
                rep = 0;
                replen = repBytes.length;
                rep7bit_p = false;
            }
            cr = CR_7BIT;

            p = StringSupport.searchNonAscii(pBytes, p, e);
            if (p == -1) {
                p = e;
            }
            while (p < e) {
                int ret = enc.length(pBytes, p, e);
                if (MBCLEN_NEEDMORE_P(ret)) {
                    break;
                }
                else if (MBCLEN_CHARFOUND_P(ret)) {
                    cr = CR_VALID;
                    p += MBCLEN_CHARFOUND_LEN(ret);
                }
                else if (MBCLEN_INVALID_P(ret)) {
                    /*
                     * p1~p: valid ascii/multibyte chars
                     * p ~e: invalid bytes + unknown bytes
                     */
                    int clen = enc.maxLength();
                    if (buf.isNil()) buf = RubyString.newStringLight(runtime, value.getRealSize());
                    if (p > p1) {
                        ((RubyString)buf).cat(pBytes, p1, p - p1);
                    }

                    if (e - p < clen) clen = e - p;
                    if (clen <= 2) {
                        clen = 1;
                    }
                    else {
                        int q = p;
                        clen--;
                        for (; clen > 1; clen--) {
                            ret = enc.length(pBytes, q, q + clen);
                            if (MBCLEN_NEEDMORE_P(ret)) break;
                            if (MBCLEN_INVALID_P(ret)) continue;
                        }
                    }
                    if (repBytes != null) {
                        ((RubyString)buf).cat(repBytes, rep, replen);
                        if (!rep7bit_p) cr = CR_VALID;
                    }
                    else {
                        repl = block.yieldSpecific(context, RubyString.newString(runtime, pBytes, p, clen, enc));
                        repl = EncodingUtils.strCompatAndValid(context, repl, enc);
                        ((RubyString)buf).cat((RubyString)repl);
                        if (((RubyString)repl).getCodeRange() == CR_VALID)
                            cr = CR_VALID;
                    }
                    p += clen;
                    p1 = p;
                    p = StringSupport.searchNonAscii(pBytes, p, e);
                    if (p == -1) {
                        p = e;
                        break;
                    }
                }
            }
            if (buf.isNil()) {
                if (p == e) {
                    setCodeRange(cr);
                    return context.nil;
                }
                buf = RubyString.newStringLight(runtime, value.getRealSize());
            }
            if (p1 < p) {
                ((RubyString)buf).cat(pBytes, p1, p - p1);
            }
            if (p < e) {
                if (repBytes != null) {
                    ((RubyString)buf).cat(repBytes, rep, replen);
                    if (!rep7bit_p) cr = CR_VALID;
                }
                else {
                    repl = block.yieldSpecific(context, RubyString.newString(runtime, pBytes, p, e - p, enc));
                    repl = EncodingUtils.strCompatAndValid(context, repl, enc);
                    ((RubyString)buf).cat((RubyString)repl);
                    if (((RubyString)repl).getCodeRange() == CR_VALID)
                        cr = CR_VALID;
                }
            }
            ((RubyString)buf).setEncodingAndCodeRange(enc, cr);
            return buf;
        }
        else {
	        /* ASCII incompatible */
            byte[] pBytes = value.unsafeBytes();
            int p = value.begin();
            int e = p + value.getRealSize();
            int p1 = p;
            IRubyObject buf = context.nil;
            byte[] repBytes;
            int rep;
            int replen;
            int mbminlen = enc.minLength();
            if (!repl.isNil()) {
                repBytes = ((RubyString)repl).value.unsafeBytes();
                rep = ((RubyString)repl).value.begin();
                replen = ((RubyString)repl).value.getRealSize();
            }
            else if (encidx == UTF16BEEncoding.INSTANCE) {
                repBytes = SCRUB_REPL_UTF16BE;
                rep = 0;
                replen = repBytes.length;
            }
            else if (encidx == UTF16LEEncoding.INSTANCE) {
                repBytes = SCRUB_REPL_UTF16LE;
                rep = 0;
                replen = repBytes.length;
            }
            else if (encidx == UTF32BEEncoding.INSTANCE) {
                repBytes = SCRUB_REPL_UTF32BE;
                rep = 0;
                replen = repBytes.length;
            }
            else if (encidx == UTF32LEEncoding.INSTANCE) {
                repBytes = SCRUB_REPL_UTF32LE;
                rep = 0;
                replen = repBytes.length;
            }
            else {
                repBytes = SCRUB_REPL_ASCII;
                rep = 0;
                replen = repBytes.length;
            }

            while (p < e) {
                int ret = enc.length(pBytes, p, e);
                if (MBCLEN_NEEDMORE_P(ret)) {
                    break;
                }
                else if (MBCLEN_CHARFOUND_P(ret)) {
                    p += MBCLEN_CHARFOUND_LEN(ret);
                }
                else if (MBCLEN_INVALID_P(ret)) {
                    int q = p;
                    int clen = enc.maxLength();
                    if (buf.isNil()) buf = RubyString.newStringLight(runtime, value.getRealSize());
                    if (p > p1) ((RubyString)buf).cat(pBytes, p1, p - p1);

                    if (e - p < clen) clen = e - p;
                    if (clen <= mbminlen * 2) {
                        clen = mbminlen;
                    }
                    else {
                        clen -= mbminlen;
                        for (; clen > mbminlen; clen-=mbminlen) {
                            ret = enc.length(pBytes, q, q + clen);
                            if (MBCLEN_NEEDMORE_P(ret)) break;
                            if (MBCLEN_INVALID_P(ret)) continue;
                        }
                    }
                    if (repBytes != null) {
                        ((RubyString)buf).cat(repBytes, rep, replen);
                    }
                    else {
                        repl = block.yieldSpecific(context, RubyString.newString(runtime, pBytes, p, e-p, enc));
                        repl = EncodingUtils.strCompatAndValid(context, repl, enc);
                        ((RubyString)buf).cat((RubyString)repl);
                    }
                    p += clen;
                    p1 = p;
                }
            }
            if (buf.isNil()) {
                if (p == e) {
                    setCodeRange(CR_VALID);
                    return context.nil;
                }
                buf = RubyString.newStringLight(runtime, value.getRealSize());
            }
            if (p1 < p) {
                ((RubyString)buf).cat(pBytes, p1, p - p1);
            }
            if (p < e) {
                if (repBytes != null) {
                    ((RubyString)buf).cat(repBytes, rep, replen);
                }
                else {
                    repl = block.yieldSpecific(context, RubyString.newString(runtime, pBytes, p, e - p, enc));
                    repl = EncodingUtils.strCompatAndValid(context, repl, enc);
                    ((RubyString)buf).cat((RubyString)repl);
                }
            }
            ((RubyString)buf).setEncodingAndCodeRange(enc, CR_VALID);
            return buf;
        }
    }

    @Deprecated
    public final RubyString strDup() {
        return strDup(getRuntime(), getMetaClass().getRealClass());
    }

    @Deprecated
    final RubyString strDup(RubyClass clazz) {
        return strDup(getRuntime(), getMetaClass());
    }

    @Deprecated
    public final void modify19(int length) {
        modifyExpand(length);
    }
}
