/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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

import org.jcodings.Config;
import org.jcodings.Encoding;
import org.jcodings.IntHolder;
import org.jcodings.exception.EncodingException;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF16BEEncoding;
import org.jcodings.specific.UTF16LEEncoding;
import org.jcodings.specific.UTF32BEEncoding;
import org.jcodings.specific.UTF32LEEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.unicode.UnicodeEncoding;
import org.joni.Matcher;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Region;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.JumpException;
import org.jruby.platform.Platform;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.CallBlock19;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.JavaSites.StringSites;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.runtime.encoding.MarshalEncoding;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.*;
import org.jruby.util.io.EncodingUtils;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

import static org.jruby.RubyComparable.invcmp;
import static org.jruby.RubyEnumerator.SizeFn;
import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.anno.FrameField.BACKREF;
import static org.jruby.runtime.Visibility.PRIVATE;
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
import static org.jruby.util.StringSupport.encFastMBCLen;
import static org.jruby.util.StringSupport.isSingleByteOptimizable;
import static org.jruby.util.StringSupport.memsearch;
import static org.jruby.util.StringSupport.memchr;
import static org.jruby.util.StringSupport.nth;
import static org.jruby.util.StringSupport.offset;
import static org.jruby.util.StringSupport.searchNonAscii;

/**
 * Implementation of Ruby String class
 *
 * Concurrency: no synchronization is required among readers, but
 * all users must synchronize externally with writers.
 *
 */
@JRubyClass(name="String", include={"Enumerable", "Comparable"})
public class RubyString extends RubyObject implements CharSequence, EncodingCapable, MarshalEncoding, CodeRangeable {
    public static final String DEBUG_INFO_FIELD = "@debug_created_info";

    static final ASCIIEncoding ASCII = ASCIIEncoding.INSTANCE;
    static final UTF8Encoding UTF8 = UTF8Encoding.INSTANCE;

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
    private static final byte[] FORCE_ENCODING_BYTES = ".force_encoding(\"".getBytes();

    public static RubyString[] NULL_ARRAY = {};

    private volatile int shareLevel = SHARE_LEVEL_NONE;

    private ByteList value;

    public static RubyClass createStringClass(Ruby runtime) {
        RubyClass stringClass = runtime.defineClass("String", runtime.getObject(), RubyString::newAllocatedString);

        stringClass.setClassIndex(ClassIndex.STRING);
        stringClass.setReifiedClass(RubyString.class);
        stringClass.kindOf = new RubyModule.JavaClassKindOf(RubyString.class);

        stringClass.includeModule(runtime.getComparable());
        stringClass.defineAnnotatedMethods(RubyString.class);

        return stringClass;
    }

    @Override
    public Encoding getEncoding() {
        return value.getEncoding();
    }

    @Override
    public void setEncoding(Encoding encoding) {
        if (encoding != value.getEncoding()) {
            if (shareLevel == SHARE_LEVEL_BYTELIST) modify();
            else modifyCheck();
            value.setEncoding(encoding);
        }
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

    // MRI: is_broken_string
    public final boolean isBrokenString() {
        return scanForCodeRange() == CR_BROKEN;
    }

    private void copyCodeRangeForSubstr(RubyString from, Encoding enc) {

        if (value.getRealSize() == 0) {
            setCodeRange(!enc.isAsciiCompatible() ? CR_VALID : CR_7BIT);
        } else {
            int fromCr = from.getCodeRange();
            if (fromCr == CR_7BIT) {
                setCodeRange(fromCr);
            } else {
                setCodeRange(CR_UNKNOWN);
            }
        }
    }

    // rb_enc_str_coderange
    @Override
    public final int scanForCodeRange() {
        int cr = getCodeRange();
        if (cr == CR_UNKNOWN) {
            cr = scanForCodeRange(value);
            setCodeRange(cr);
        }
        return cr;
    }

    public static int scanForCodeRange(final ByteList bytes) {
        Encoding enc = bytes.getEncoding();
        if (enc.minLength() > 1 && enc.isDummy() && EncodingUtils.getActualEncoding(enc, bytes).minLength() == 1) {
            return CR_BROKEN;
        }
        return codeRangeScan(enc, bytes);
    }

    final boolean singleByteOptimizable() {
        return StringSupport.isSingleByteOptimizable(this, EncodingUtils.STR_ENC_GET(this));
    }

    final boolean singleByteOptimizable(Encoding enc) {
        return StringSupport.isSingleByteOptimizable(this, enc);
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
        return checkEncoding((CodeRangeable) other);
    }

    final Encoding checkEncoding(EncodingCapable other) {
        Encoding enc = isCompatibleWith(other);
        if (enc == null) throw getRuntime().newEncodingCompatibilityError("incompatible character encodings: " +
                                value.getEncoding() + " and " + other.getEncoding());
        return enc;
    }

    @Override
    public final Encoding checkEncoding(CodeRangeable other) {
        Encoding enc = StringSupport.areCompatible(this, other);
        if (enc == null) throw getRuntime().newEncodingCompatibilityError("incompatible character encodings: " +
                value.getEncoding() + " and " + other.getByteList().getEncoding());
        return enc;
    }

    public static Encoding checkEncoding(final Ruby runtime, ByteList str1, ByteList str2) {
        Encoding enc = StringSupport.areCompatible(str1, str2);
        if (enc == null) {
            throw runtime.newEncodingCompatibilityError("incompatible character encodings: " +
                    str1.getEncoding() + " and " + str2.getEncoding());
        }
        return enc;
    }

    private Encoding checkDummyEncoding() {
        Encoding enc = value.getEncoding();
        if (enc.isDummy()) throw getRuntime().newEncodingCompatibilityError(
                "incompatible encoding with this operation: " + enc);
        return enc;
    }

    public final int strLength() {
        return StringSupport.strLengthFromRubyString(this);
    }

    final int strLength(final ByteList bytes, final Encoding enc) {
        return StringSupport.strLengthFromRubyString(this, bytes, enc);
    }

    // MRI: rb_str_sublen
    final int subLength(int pos) {
        if (pos < 0 || singleByteOptimizable()) return pos;
        return StringSupport.strLength(value.getEncoding(), value.getUnsafeBytes(), value.getBegin(), value.getBegin() + pos);
    }

    /** short circuit for String key comparison
     *
     */
    @Override
    public final boolean eql(IRubyObject other) {
        RubyClass meta = this.metaClass;
        if (meta != meta.runtime.getString() || meta != other.getMetaClass()) return super.eql(other);
        return eql19(other);
    }

    // rb_str_hash_cmp
    private boolean eql19(IRubyObject other) {
        final RubyString otherString = (RubyString) other;
        return StringSupport.areComparable(this, otherString) && value.equal(otherString.value);
    }

    public RubyString(Ruby runtime, RubyClass rubyClass) {
        this(runtime, rubyClass, ByteList.NULL_ARRAY);
    }

    public RubyString(Ruby runtime, RubyClass rubyClass, CharSequence value) {
        this(runtime, rubyClass, value, UTF8);
    }

    public RubyString(Ruby runtime, RubyClass rubyClass, CharSequence value, Encoding enc) {
        super(runtime, rubyClass);
        assert value != null;
        assert enc != null;

        this.value = encodeBytelist(value, enc);
    }

    private RubyString(Ruby runtime, RubyClass rubyClass, String value, Encoding enc) {
        super(runtime, rubyClass);
        assert value != null;
        assert enc != null;

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
        flags |= cr;
        value.setEncoding(enc);
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
        return new RubyString(runtime, runtime.getString(), str, UTF8);
    }

    public static RubyString newString(Ruby runtime, CharSequence str, Encoding encoding) {
        return new RubyString(runtime, runtime.getString(), str, encoding);
    }

    public static RubyString newString(Ruby runtime, String str) {
        return new RubyString(runtime, runtime.getString(), str, UTF8);
    }

    public static RubyString newString(Ruby runtime, String str, Encoding encoding) {
        return new RubyString(runtime, runtime.getString(), str, encoding);
    }

    public static RubyString newBinaryString(Ruby runtime, String str) {
        return new RubyString(runtime, runtime.getString(), new ByteList(ByteList.plain(str), ASCIIEncoding.INSTANCE, false));
    }

    public static RubyString newBinaryString(Ruby runtime, ByteList str) {
        return new RubyString(runtime, runtime.getString(), str, ASCIIEncoding.INSTANCE, false);
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

    public static RubyString newString(Ruby runtime, ByteList bytes, int coderange) {
        return new RubyString(runtime, runtime.getString(), bytes, coderange);
    }

    public static RubyString newString(Ruby runtime, ByteList bytes, Encoding encoding) {
        return new RubyString(runtime, runtime.getString(), bytes, encoding);
    }

    static RubyString newString(Ruby runtime, byte b) {
        return new RubyString(runtime, runtime.getString(), RubyInteger.singleCharByteList(b));
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
        return new RubyString(runtime, runtime.getString(), RubyEncoding.doEncodeUTF8(str));
    }

    public static RubyString newUTF16String(Ruby runtime, String str) {
        return new RubyString(runtime, runtime.getString(), RubyEncoding.doEncodeUTF16(str));
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
        return new RubyString(runtime, runtime.getString(), RubyEncoding.doEncodeUTF8(str));
    }

    public static RubyString newUTF16String(Ruby runtime, CharSequence str) {
        return new RubyString(runtime, runtime.getString(), RubyEncoding.doEncodeUTF16(str));
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
        if ( internal != null ) rubyInt = EncodingUtils.charsetForEncoding(internal);

        if ( rubyInt == null ) {
            Encoding javaExtEncoding = runtime.getEncodingService().getJavaDefault();
            return RubyString.newString(runtime, new ByteList(str.getBytes(), javaExtEncoding));
        }
        return RubyString.newString(runtime,  new ByteList(RubyEncoding.encode(str, rubyInt), internal));
    }

    @Deprecated
    public static RubyString newExternalStringWithEncoding(Ruby runtime, String string, Encoding encoding) {
        return EncodingUtils.newExternalStringWithEncoding(runtime, string, encoding);
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
        if (bytes.getEncoding() == encoding) return newStringShared(runtime, clazz, bytes);
        RubyString str = new RubyString(runtime, clazz, bytes.makeShared(bytes.getBegin(), bytes.getRealSize()), encoding);
        str.shareLevel = SHARE_LEVEL_BUFFER; // since passing an encoding in does bytes.setEncoding(encoding)
        return str;
    }

    private static RubyString newStringShared(Ruby runtime, ByteList bytes, Encoding encoding, int cr) {
        RubyString str = newStringShared(runtime, runtime.getString(), bytes, encoding);
        str.flags |= cr;
        return str;
    }

    public static RubyString newStringShared(Ruby runtime, byte[] bytes) {
        return newStringShared(runtime, bytes, ASCIIEncoding.INSTANCE);
    }

    public static RubyString newStringShared(Ruby runtime, byte[] bytes, Encoding encoding) {
        return newStringShared(runtime, bytes, 0, bytes.length, encoding);
    }

    public static RubyString newStringShared(Ruby runtime, byte[] bytes, int start, int length) {
        return newStringShared(runtime, bytes, start, length, ASCIIEncoding.INSTANCE);
    }

    public static RubyString newStringShared(Ruby runtime, byte[] bytes, int start, int length, Encoding encoding) {
        ByteList byteList = new ByteList(bytes, start, length, encoding, false);
        RubyString str = new RubyString(runtime, runtime.getString(), byteList);
        str.shareLevel = SHARE_LEVEL_BUFFER;
        return str;
    }

    public static RubyString newEmptyString(Ruby runtime) {
        return newEmptyString(runtime, runtime.getString());
    }

    private static final ByteList EMPTY_ASCII8BIT_BYTELIST = new ByteList(ByteList.NULL_ARRAY, ASCIIEncoding.INSTANCE);
    private static final ByteList EMPTY_USASCII_BYTELIST = new ByteList(ByteList.NULL_ARRAY, USASCIIEncoding.INSTANCE);

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
    public final boolean independent() {
        return shareLevel == SHARE_LEVEL_NONE;
    }

    // str_make_independent, modified to create a new String rather than possibly modifying a frozen one
    public final RubyString makeIndependent() {
        RubyClass klass = metaClass;
        RubyString str = strDup(klass.runtime, klass);
        str.modify();
        str.setFrozen(true);
        str.infectBy(this);
        return str;
    }

    // str_make_independent_expand
    public final RubyString makeIndependent(final int length) {
        RubyClass klass = metaClass;
        RubyString str = strDup(klass.runtime, klass);
        str.modify(length);
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
    public static final class EmptyByteListHolder {
        public final ByteList bytes;
        public final int cr;
        EmptyByteListHolder(Encoding enc) {
            this.bytes = new ByteList(ByteList.NULL_ARRAY, enc);
            this.cr = bytes.getEncoding().isAsciiCompatible() ? CR_7BIT : CR_VALID;
        }
    }

    private static EmptyByteListHolder EMPTY_BYTELISTS[] = new EmptyByteListHolder[4];

    public static EmptyByteListHolder getEmptyByteList(Encoding enc) {
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
        RubyString str = newUsAsciiStringNoCopy(runtime, new ByteList(bytes, start, length, false));
        str.shareLevel = SHARE_LEVEL_BUFFER;
        return str;
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
        RubyString dup = (RubyString) dup();
        dup.setFrozen(true);
        return dup;
    }

    // MRI: rb_str_dup
    public final RubyString strDup(Ruby runtime) {
        return strDup(runtime, metaClass.getRealClass());
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
        return makeShared(runtime, runtime.getString(), value, index, len);
    }

    @Deprecated
    public final RubyString makeSharedString19(Ruby runtime, int index, int len) {
        return makeShared(runtime, runtime.getString(), value, index, len);
    }

    public final RubyString makeShared(Ruby runtime, int index, int len) {
        return makeShared(runtime, getType(), value, index, len);
    }

    public final RubyString makeShared(Ruby runtime, RubyClass meta, int index, int len) {
        final RubyString shared;
        if (len == 0) {
            shared = newEmptyString(runtime, meta);
        } else if (len == 1) {
            shared = newStringShared(runtime, meta, RubyInteger.singleCharByteList(value.getUnsafeBytes()[value.getBegin() + index]));
        } else {
            if (shareLevel == SHARE_LEVEL_NONE) shareLevel = SHARE_LEVEL_BUFFER;
            shared = new RubyString(runtime, meta, value.makeShared(index, len));
            shared.shareLevel = SHARE_LEVEL_BUFFER;
        }

        shared.infectBy(this);
        return shared;
    }

    @Deprecated
    public final RubyString makeShared19(Ruby runtime, int index, int len) {
        return makeShared(runtime, value, index, len);
    }

    @Deprecated
    public final RubyString makeShared19(Ruby runtime, RubyClass meta, int index, int len) {
        return makeShared(runtime, meta, value, index, len);
    }

    private RubyString makeShared(Ruby runtime, ByteList value, int index, int len) {
        return makeShared(runtime, getType(), value, index, len);
    }

    private RubyString makeShared(Ruby runtime, RubyClass meta, ByteList value, int index, int len) {
        final RubyString shared;
        Encoding enc = value.getEncoding();

        if (len == 0) {
            shared = newEmptyString(runtime, meta, enc);
        } else if (len == 1) {
            byte b = (byte) value.get(index);

            // only use cache for low ASCII bytes
            if ((b & 0xFF) < 0x80) {
                shared = RubyInteger.singleCharString(runtime, b, meta, enc);
            } else {
                ByteList bytes = new ByteList(new byte[]{(byte) value.get(index)}, enc);
                shared = new RubyString(runtime, meta, bytes);
            }
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

    final void setBufferShared() {
        if (shareLevel == SHARE_LEVEL_NONE) shareLevel = SHARE_LEVEL_BUFFER;
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

    public void modifyCheck(byte[] b, int len) {
        if (value.getUnsafeBytes() != b || value.getRealSize() != len) throw getRuntime().newRuntimeError("string modified");
    }

    private void modifyCheck(byte[] b, int len, Encoding enc) {
        if (value.getUnsafeBytes() != b || value.getRealSize() != len || value.getEncoding() != enc) throw getRuntime().newRuntimeError("string modified");
    }

    private void frozenCheck() {
        frozenCheck(false);
    }

    private void frozenCheck(boolean runtimeError) {
        if (isFrozen()) {
            if (getRuntime().getInstanceConfig().isDebuggingFrozenStringLiteral()) {
                IRubyObject obj = getInstanceVariable(DEBUG_INFO_FIELD);

                if (obj != null && obj instanceof RubyArray) {
                    RubyArray info = (RubyArray) obj;
                    if (info.getLength() == 2) {
                        throw getRuntime().newRaiseException(getRuntime().getFrozenError(),
                                "can't modify frozen String, created at " + info.eltInternal(0) + ":" + info.eltInternal(1));
                    }
                }
            }

            throw getRuntime().newFrozenError("String", runtimeError);
        }
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

    @Override
    public void modifyAndKeepCodeRange() {
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

    // MRI: rb_str_new_frozen, at least in spirit
    // also aliased to rb_str_new4
    public RubyString newFrozen() {
        if (isFrozen()) return this;

        RubyString str = strDup(metaClass.runtime);
        str.setCodeRange(getCodeRange());
        str.setFrozen(true);
        return str;
    }

    /** rb_str_resize
     */
    public final void resize(final int size) {
        final int len = value.length();
        if (len > size) {
            modify(size);
            value.setRealSize(size);
        } else if (len < size) {
            modify(size);
            value.length(size);
        }
    }

    public final void view(ByteList bytes) {
        modifyCheck();

        value = bytes;
        shareLevel = SHARE_LEVEL_NONE;
    }

    private void view(byte[] bytes, boolean copy) {
        modifyCheck();

        value = new ByteList(bytes, copy);
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

    @JRubyMethod(meta = true)
    public static IRubyObject try_convert(ThreadContext context, IRubyObject recv, IRubyObject str) {
        return str.checkStringType();
    }

    @JRubyMethod(name = {"to_s", "to_str"})
    @Override
    public IRubyObject to_s() {
        final Ruby runtime = metaClass.runtime;
        if (metaClass.getRealClass() != runtime.getString()) {
            return strDup(runtime, runtime.getString());
        }
        return this;
    }

    @Override
    public final int compareTo(IRubyObject other) {
        return (int) op_cmp(metaClass.runtime.getCurrentContext(), other).convertToInteger().getLongValue();
    }

    /* rb_str_cmp_m */
    @JRubyMethod(name = "<=>")
    @Override
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;
        if (other instanceof RubyString) {
            return runtime.newFixnum(op_cmp((RubyString) other));
        }
        JavaSites.CheckedSites sites = sites(context).to_str_checked;
        if (sites.respond_to_X.respondsTo(context, this, other)) {
            IRubyObject tmp = TypeConverter.checkStringType(context, sites, other);
            if (tmp instanceof RubyString) return runtime.newFixnum(op_cmp((RubyString) tmp));
        } else {
            return invcmp(context, sites(context).recursive_cmp, this, other);
        }
        return context.nil;
    }

    /** rb_str_equal
     *
     */
    @JRubyMethod(name = {"==", "==="})
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if (this == other) return context.tru;
        if (other instanceof RubyString) {
            RubyString otherString = (RubyString)other;
            return StringSupport.areComparable(this, otherString) && value.equal(otherString.value) ? context.tru : context.fals;
        }
        return op_equalCommon(context, other);
    }

    private IRubyObject op_equalCommon(ThreadContext context, IRubyObject other) {
        if (!sites(context).respond_to_to_str.respondsTo(context, this, other)) return context.fals;
        return sites(context).equals.call(context, this, other, this).isTrue() ? context.tru : context.fals;
    }

    @JRubyMethod(name = "-@") // -'foo' returns frozen string
    public final IRubyObject minus_at(ThreadContext context) {
        Ruby runtime = context.runtime;

        RubyString str = this;

        if (!str.isBare(runtime) && !str.isFrozen()) str = str.strDup(runtime);

        return runtime.freezeAndDedupString(str);
    }

    @JRubyMethod(name = "+@") // +'foo' returns modifiable string
    public final IRubyObject plus_at() {
        return isFrozen() ? this.dup() : this;
    }

    public IRubyObject op_plus(ThreadContext context, IRubyObject arg) {
        return op_plus19(context, arg);
    }

    @JRubyMethod(name = "+", required = 1)
    public IRubyObject op_plus19(ThreadContext context, IRubyObject arg) {
        RubyString str = arg.convertToString();
        Encoding enc = checkEncoding(str);
        long len = (long) value.getRealSize() + str.value.getRealSize();

        // we limit to int because ByteBuffer can only allocate int sizes
        if (len > Integer.MAX_VALUE) throw context.runtime.newArgumentError("argument too big");
        RubyString resultStr = newStringNoCopy(context.runtime, StringSupport.addByteLists(value, str.value),
                enc, CodeRangeSupport.codeRangeAnd(getCodeRange(), str.getCodeRange()));
        resultStr.infectBy(flags | str.flags);
        return resultStr;
    }

    public IRubyObject op_mul(ThreadContext context, IRubyObject other) {
        return op_mul19(context, other);
    }

    @JRubyMethod(name = "*", required = 1)
    public IRubyObject op_mul19(ThreadContext context, IRubyObject other) {
        RubyString result = multiplyByteList(context, other);
        result.value.setEncoding(value.getEncoding());
        result.copyCodeRangeForSubstr(this, value.getEncoding());
        return result;
    }

    private RubyString multiplyByteList(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;

        int len = RubyNumeric.num2int(arg);
        if (len < 0) throw runtime.newArgumentError("negative argument");

        // we limit to int because ByteBuffer can only allocate int sizes
        len = Helpers.multiplyBufferLength(runtime, value.getRealSize(), len);

        ByteList bytes = new ByteList(len);
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
        RubyString result = new RubyString(runtime, metaClass, bytes);
        result.infectBy(this);
        return result;
    }

    @JRubyMethod(name = "%", required = 1)
    public RubyString op_format(ThreadContext context, IRubyObject arg) {
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
        final ByteList value = this.value;
        final Encoding enc = value.getEncoding();
        long hash = runtime.isSiphashEnabled() ? SipHashInline.hash24(runtime.getHashSeedK0(),
                runtime.getHashSeedK1(), value.getUnsafeBytes(), value.getBegin(),
                value.getRealSize()) : PerlHash.hash(runtime.getHashSeedK0(),
                value.getUnsafeBytes(), value.getBegin(), value.getRealSize());
        hash ^= (enc.isAsciiCompatible() && scanForCodeRange() == CR_7BIT ? 0 : enc.getIndex());
        return (int) hash;
    }

    /**
     * Generate a hash for the String, without a seed.
     *
     * @param runtime
     * @return
     */
    public int unseededStrHashCode(Ruby runtime) {
        final ByteList value = this.value;
        final Encoding enc = value.getEncoding();
        long hash = runtime.isSiphashEnabled() ? SipHashInline.hash24(0, 0, value.getUnsafeBytes(),
                value.getBegin(), value.getRealSize()) : PerlHash.hash(0, value.getUnsafeBytes(),
                value.getBegin(), value.getRealSize());
        hash ^= (enc.isAsciiCompatible() && scanForCodeRange() == CR_7BIT ? 0 : enc.getIndex());
        return (int) hash;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;

        return (other instanceof RubyString) && equals((RubyString) other);
    }

    final boolean equals(RubyString other) {
        return ((RubyString) other).value.equal(value);
    }

    /** rb_obj_as_string
     *
     */
    public static RubyString objAsString(ThreadContext context, IRubyObject obj) {
        if (obj instanceof RubyString) return (RubyString) obj;
        IRubyObject str = sites(context).to_s.call(context, obj, obj);
        if (!(str instanceof RubyString)) return (RubyString) obj.anyToString();
        // taint string if it is not untainted and not frozen
        // TODO: MRI sets an fstring flag on fstrings and uses that flag here
        if (obj.isTaint() && !str.isTaint() && !str.isFrozen()) str.setTaint(true);
        return (RubyString) str;
    }

    /** rb_str_cmp
     *
     */
    public final int op_cmp(RubyString other) {
        int ret = value.cmp(other.value);
        if (ret == 0 && !StringSupport.areComparable(this, other)) {
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
        if (len == 0) return this;
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

    public final RubyString cat(ByteList other, int codeRange) {
        cat19(other, codeRange);
        return this;
    }

    // rb_str_buf_append against ptr
    public final int cat19(ByteList other, int codeRange) {
        return EncodingUtils.encCrStrBufCat(metaClass.runtime, this, other, other.getEncoding(), codeRange);
    }

    public final RubyString catString(String str) {
        cat19(encodeBytelist(str, getEncoding()), CR_UNKNOWN);
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
        return cat((byte) ch);
    }

    public final RubyString cat(int code, Encoding enc) {
        int n = codeLength(enc, code);
        modify(value.getRealSize() + n);
        enc.codeToMbc(code, value.getUnsafeBytes(), value.getBegin() + value.getRealSize());
        value.setRealSize(value.getRealSize() + n);
        return this;
    }

    // rb_enc_str_buf_cat
    public final int cat(byte[] bytes, int p, int len, Encoding enc) {
        return EncodingUtils.encCrStrBufCat(getRuntime(), this, new ByteList(bytes, p, len), enc, CR_UNKNOWN);
    }

    // rb_str_buf_cat_ascii
    public final RubyString catAscii(byte[] bytes, int ptr, int ptrLen) {
        Encoding enc = value.getEncoding();
        if (enc.isAsciiCompatible()) {
            EncodingUtils.encCrStrBufCat(getRuntime(), this, new ByteList(bytes, ptr, ptrLen), enc, CR_7BIT);
        } else {
            byte buf[] = new byte[enc.maxLength()];
            int end = ptr + ptrLen;
            while (ptr < end) {
                int c = bytes[ptr];
                int len = codeLength(enc, c);
                EncodingUtils.encMbcput(c, buf, 0, enc);
                EncodingUtils.encCrStrBufCat(getRuntime(), this, buf, 0, len, enc, CR_VALID);
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
        RubyString str = strDup(context.runtime);
        str.reverse_bang19(context);
        return str;
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
            int end = p + len;
            int op = len;
            int cr = getCodeRange();

            Encoding enc = value.getEncoding();
            // this really needs to be inlined here
            if (singleByteOptimizable()) {
                for (int i = 0; i < len >> 1; i++) {
                    byte b = bytes[p + i];
                    bytes[p + i] = bytes[p + len - i - 1];
                    bytes[p + len - i - 1] = b;
                }
            } else if (cr == CR_VALID) {
                byte[] obytes = new byte[len];
                while (p < end) {
                    int cl = StringSupport.encFastMBCLen(bytes, p, end, enc);

                    op -= cl;
                    System.arraycopy(bytes, p, obytes, op, cl);
                    p += cl;
                }
                value.setUnsafeBytes(obytes);
            } else {
                byte[] obytes = new byte[len];
                cr = enc.isAsciiCompatible() ? CR_7BIT : CR_VALID;
                while (p < end) {
                    int cl = StringSupport.length(enc, bytes, p, end);

                    if (cl > 1 || (bytes[p] & 0x80) != 0) cr = CR_UNKNOWN;
                    op -= cl;
                    System.arraycopy(bytes, p, obytes, op, cl);
                    p += cl;
                }
                value.setUnsafeBytes(obytes);
            }

            setCodeRange(cr);
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
    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context) {
        return this;
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject arg0) {
        IRubyObject tmp = ArgsUtil.getOptionsArg(context.runtime, arg0);
        if (tmp.isNil()) {
            return initialize(context, arg0, null);
        }

        return initialize(context, null, (RubyHash) tmp);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject arg0, IRubyObject opts) {
        Ruby runtime = context.runtime;

        IRubyObject tmp = ArgsUtil.getOptionsArg(context.runtime, opts);
        if (tmp.isNil()) {
            throw runtime.newArgumentError(2, 1);
        }

        return initialize(context, arg0, (RubyHash) tmp);
    }

    private IRubyObject initialize(ThreadContext context, IRubyObject arg0, RubyHash opts) {
        Ruby runtime = context.runtime;

        if (arg0 != null) {
            replace19(arg0);
        }

        if (opts != null) {
            IRubyObject encoding = opts.fastARef(context.runtime.newSymbol("encoding"));
            IRubyObject capacity = opts.fastARef(context.runtime.newSymbol("capacity"));

            if (!(capacity == null || capacity.isNil())) {
                modify(capacity.convertToInteger().getIntValue());
            }

            if (!(encoding == null || encoding.isNil())) {
                modify();
                setEncodingAndCodeRange(runtime.getEncodingService().getEncodingFromObject(encoding), CR_UNKNOWN);
            }
        }

        return this;
    }

    @Deprecated
    public IRubyObject initialize19(ThreadContext context, IRubyObject arg0) {
        return initialize(context, arg0);
    }

    @Deprecated
    public IRubyObject casecmp19(ThreadContext context, IRubyObject other) {
        return casecmp(context, other);
    }

    @JRubyMethod(name = "casecmp")
    public IRubyObject casecmp(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;

        IRubyObject tmp = other.checkStringType();
        if (tmp.isNil()) return context.nil;

        RubyString otherStr = (RubyString) tmp;
        Encoding enc = StringSupport.areCompatible(this, otherStr);
        if (enc == null) return context.nil;

        if (singleByteOptimizable() && otherStr.singleByteOptimizable()) {
            return RubyFixnum.newFixnum(runtime, value.caseInsensitiveCmp(otherStr.value));
        }

        final int ret = StringSupport.multiByteCasecmp(enc, value, otherStr.value);

        if (ret < 0) return RubyFixnum.minus_one(runtime);
        if (ret > 0) return RubyFixnum.one(runtime);
        return RubyFixnum.zero(runtime);
    }

    @JRubyMethod(name = "casecmp?")
    public IRubyObject casecmp_p(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;

        IRubyObject tmp = other.checkStringType();
        if (tmp.isNil()) return context.nil;
        RubyString otherStr = (RubyString) tmp;

        Encoding enc = StringSupport.areCompatible(this, otherStr);
        if (enc == null) return context.nil;

        int flags = Config.CASE_FOLD;
        RubyString down = this.strDup(runtime);
        down.downcase_bang(context, flags);
        RubyString otherDown = otherStr.strDup(runtime);
        otherDown.downcase_bang(context, flags);
        return down.equals(otherDown) ? context.tru : context.fals;
    }

    /** rb_str_match
     *
     */

    @JRubyMethod(name = "=~", writes = BACKREF)
    @Override
    public IRubyObject op_match(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyRegexp) return ((RubyRegexp) other).op_match(context, this);
        if (other instanceof RubyString) throw context.runtime.newTypeError("type mismatch: String given");
        return sites(context).op_match.call(context, other, other, this);
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

    @JRubyMethod(name = "match", required = 1, writes = BACKREF)
    public IRubyObject match19(ThreadContext context, IRubyObject pattern, Block block) {
        RubyRegexp coercedPattern = getPattern(context.runtime, pattern);
        IRubyObject result = sites(context).match.call(context, coercedPattern, coercedPattern, this);
        return block.isGiven() && result != context.nil ? block.yield(context, result) : result;
    }

    @JRubyMethod(name = "match", writes = BACKREF)
    public IRubyObject match19(ThreadContext context, IRubyObject pattern, IRubyObject pos, Block block) {
        RubyRegexp coercedPattern = getPattern(context.runtime, pattern);
        IRubyObject result = sites(context).match.call(context, coercedPattern, coercedPattern, this, pos);
        return block.isGiven() && result != context.nil ? block.yield(context, result) : result;
    }

    @JRubyMethod(name = "match", required = 1, rest = true)
    public IRubyObject match19(ThreadContext context, IRubyObject[] args, Block block) {
        if (args.length < 1) {
            Arity.checkArgumentCount(context, args, 1, 2);
        }
        RubyRegexp pattern = getPattern(context.runtime, args[0]);
        args[0] = this;
        IRubyObject result = sites(context).match.call(context, pattern, pattern, args);
        return block.isGiven() && result != context.nil ? block.yield(context, result) : result;
    }

    @JRubyMethod(name = "match?")
    public IRubyObject match_p(ThreadContext context, IRubyObject pattern) {
        return getPattern(context.runtime, pattern).match_p(context, this);
    }

    @JRubyMethod(name = "match?")
    public IRubyObject match_p(ThreadContext context, IRubyObject pattern, IRubyObject pos) {
        return getPattern(context.runtime, pattern).match_p(context, this, pos);
    }

    public IRubyObject op_ge(ThreadContext context, IRubyObject other) {
        return op_ge19(context, other);
    }

    @JRubyMethod(name = ">=")
    public IRubyObject op_ge19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString && cmpIsBuiltin(context)) {
            return RubyBoolean.newBoolean(context, op_cmp((RubyString) other) >= 0);
        }
        return RubyComparable.op_ge(context, this, other);
    }

    public IRubyObject op_gt(ThreadContext context, IRubyObject other) {
        return op_gt19(context, other);
    }

    @JRubyMethod(name = ">")
    public IRubyObject op_gt19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString && cmpIsBuiltin(context)) {
            return RubyBoolean.newBoolean(context, op_cmp((RubyString) other) > 0);
        }
        return RubyComparable.op_gt(context, this, other);
    }

    public IRubyObject op_le(ThreadContext context, IRubyObject other) {
        return op_le19(context, other);
    }

    @JRubyMethod(name = "<=")
    public IRubyObject op_le19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString && cmpIsBuiltin(context)) {
            return RubyBoolean.newBoolean(context, op_cmp((RubyString) other) <= 0);
        }
        return RubyComparable.op_le(context, this, other);
    }

    public IRubyObject op_lt(ThreadContext context, IRubyObject other) {
        return op_lt19(context, other);
    }

    @JRubyMethod(name = "<")
    public IRubyObject op_lt19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString && cmpIsBuiltin(context)) {
            return RubyBoolean.newBoolean(context, op_cmp((RubyString) other) < 0);
        }
        return RubyComparable.op_lt(context, sites(context).cmp, this, other);
    }

    private boolean cmpIsBuiltin(ThreadContext context) {
        return sites(context).cmp.isBuiltin(this);
    }

    public IRubyObject str_eql_p(ThreadContext context, IRubyObject other) {
        return str_eql_p19(context, other);
    }

    @JRubyMethod(name = "eql?")
    public IRubyObject str_eql_p19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) {
            RubyString otherString = (RubyString)other;
            if (StringSupport.areComparable(this, otherString) && value.equal(otherString.value)) return context.tru;
        }
        return context.fals;
    }

    private int caseMap(Ruby runtime, int flags, Encoding enc) {
        IntHolder flagsP = new IntHolder();
        flagsP.value = flags;
        if ((flags & Config.CASE_ASCII_ONLY) != 0) {
            StringSupport.asciiOnlyCaseMap(runtime, value, flagsP, enc);
        } else {
            value = StringSupport.caseMap(runtime, value, flagsP, enc);
        }
        return flagsP.value;
    }

    /** rb_str_upcase / rb_str_upcase_bang
     *
     */
    @Deprecated
    public RubyString upcase19(ThreadContext context) {
        return upcase(context);
    }

    @Deprecated
    public IRubyObject upcase_bang19(ThreadContext context) {
        return upcase_bang(context);
    }

    @JRubyMethod(name = "upcase")
    public RubyString upcase(ThreadContext context) {
        RubyString str = strDup(context.runtime);
        str.upcase_bang(context);
        return str;
    }

    @JRubyMethod(name = "upcase")
    public RubyString upcase(ThreadContext context, IRubyObject arg) {
        RubyString str = strDup(context.runtime);
        str.upcase_bang(context, arg);
        return str;
    }

    @JRubyMethod(name = "upcase")
    public RubyString upcase(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        RubyString str = strDup(context.runtime);
        str.upcase_bang(context, arg0, arg1);
        return str;
    }

    @JRubyMethod(name = "upcase!")
    public IRubyObject upcase_bang(ThreadContext context) {
        return upcase_bang(context, Config.CASE_UPCASE);
    }

    @JRubyMethod(name = "upcase!")
    public IRubyObject upcase_bang(ThreadContext context, IRubyObject arg) {
        return upcase_bang(context, StringSupport.checkCaseMapOptions(context.runtime, arg, Config.CASE_UPCASE));
    }

    @JRubyMethod(name = "upcase!")
    public IRubyObject upcase_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return upcase_bang(context, StringSupport.checkCaseMapOptions(context.runtime, arg0, arg1, Config.CASE_UPCASE));
    }

    private IRubyObject upcase_bang(ThreadContext context, int flags) {
        modifyAndKeepCodeRange();
        Encoding enc = checkDummyEncoding();
        if (((flags & Config.CASE_ASCII_ONLY) != 0 && (enc.isUTF8() || enc.maxLength() == 1)) ||
                (flags & Config.CASE_FOLD_TURKISH_AZERI) == 0 && getCodeRange() == CR_7BIT) {
            int s = value.getBegin();
            int end = s + value.getRealSize();
            byte[]bytes = value.getUnsafeBytes();
            while (s < end) {
                int c = bytes[s] & 0xff;
                if (Encoding.isAscii(c) && 'a' <= c && c <= 'z') {
                    bytes[s] = (byte)('A' + (c - 'a'));
                    flags |= Config.CASE_MODIFIED;
                }
                s++;
            }
        } else {
            flags = caseMap(context.runtime, flags, enc);
            if ((flags & Config.CASE_MODIFIED) != 0) clearCodeRange();
        }

        return ((flags & Config.CASE_MODIFIED) != 0) ? this : context.nil;
    }

    /** rb_str_downcase / rb_str_downcase_bang
    *
    */

    @Deprecated
    public RubyString downcase19(ThreadContext context) {
        return downcase(context);
    }

    @Deprecated
    public IRubyObject downcase_bang19(ThreadContext context) {
        return downcase_bang(context);
    }

    @JRubyMethod(name = "downcase")
    public RubyString downcase(ThreadContext context) {
        RubyString str = strDup(context.runtime);
        str.downcase_bang(context);
        return str;
    }

    @JRubyMethod(name = "downcase")
    public RubyString downcase(ThreadContext context, IRubyObject arg) {
        RubyString str = strDup(context.runtime);
        str.downcase_bang(context, arg);
        return str;
    }

    @JRubyMethod(name = "downcase")
    public RubyString downcase(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        RubyString str = strDup(context.runtime);
        str.downcase_bang(context, arg0, arg1);
        return str;
    }

    @JRubyMethod(name = "downcase!")
    public IRubyObject downcase_bang(ThreadContext context) {
        return downcase_bang(context, Config.CASE_DOWNCASE);
    }

    @JRubyMethod(name = "downcase!")
    public IRubyObject downcase_bang(ThreadContext context, IRubyObject arg) {
        return downcase_bang(context, StringSupport.checkCaseMapOptions(context.runtime, arg, Config.CASE_DOWNCASE));
    }

    @JRubyMethod(name = "downcase!")
    public IRubyObject downcase_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return downcase_bang(context, StringSupport.checkCaseMapOptions(context.runtime, arg0, arg1, Config.CASE_DOWNCASE));
    }

    private IRubyObject downcase_bang(ThreadContext context, int flags) {
        modifyAndKeepCodeRange();
        Encoding enc = checkDummyEncoding();
        if (((flags & Config.CASE_ASCII_ONLY) != 0 && (enc.isUTF8() || enc.maxLength() == 1)) ||
                (flags & Config.CASE_FOLD_TURKISH_AZERI) == 0 && getCodeRange() == CR_7BIT) {
            int s = value.getBegin();
            int end = s + value.getRealSize();
            byte[]bytes = value.getUnsafeBytes();
            while (s < end) {
                int c = bytes[s] & 0xff;
                if (Encoding.isAscii(c) && 'A' <= c && c <= 'Z') {
                    bytes[s] = (byte)('a' + (c - 'A'));
                    flags |= Config.CASE_MODIFIED;
                }
                s++;
            }
        } else {
            flags = caseMap(context.runtime, flags, enc);
            if ((flags & Config.CASE_MODIFIED) != 0) clearCodeRange();
        }

        return ((flags & Config.CASE_MODIFIED) != 0) ? this : context.nil;
    }

    /** rb_str_swapcase / rb_str_swapcase_bang
     *
     */
    @Deprecated
    public RubyString swapcase19(ThreadContext context) {
        return swapcase(context);
    }

    @Deprecated
    public IRubyObject swapcase_bang19(ThreadContext context) {
        return swapcase_bang(context);
    }

    @JRubyMethod(name = "swapcase")
    public RubyString swapcase(ThreadContext context) {
        RubyString str = strDup(context.runtime);
        str.swapcase_bang(context);
        return str;
    }

    @JRubyMethod(name = "swapcase")
    public RubyString swapcase(ThreadContext context, IRubyObject arg) {
        RubyString str = strDup(context.runtime);
        str.swapcase_bang(context, arg);
        return str;
    }

    @JRubyMethod(name = "swapcase")
    public RubyString swapcase(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        RubyString str = strDup(context.runtime);
        str.swapcase_bang(context, arg0, arg1);
        return str;
    }

    @JRubyMethod(name = "swapcase!")
    public IRubyObject swapcase_bang(ThreadContext context) {
        return swapcase_bang(context, Config.CASE_UPCASE | Config.CASE_DOWNCASE);
    }

    @JRubyMethod(name = "swapcase!")
    public IRubyObject swapcase_bang(ThreadContext context, IRubyObject arg) {
        return swapcase_bang(context, StringSupport.checkCaseMapOptions(context.runtime, arg, Config.CASE_UPCASE | Config.CASE_DOWNCASE));
    }

    @JRubyMethod(name = "swapcase!")
    public IRubyObject swapcase_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return swapcase_bang(context, StringSupport.checkCaseMapOptions(context.runtime, arg0, arg1, Config.CASE_UPCASE | Config.CASE_DOWNCASE));
    }

    private IRubyObject swapcase_bang(ThreadContext context, int flags) {
        modifyAndKeepCodeRange();
        Encoding enc = checkDummyEncoding();
        flags = caseMap(context.runtime, flags, enc);
        if ((flags & Config.CASE_MODIFIED) != 0) {
            clearCodeRange();
            return this;
        } else {
            return context.nil;
        }
    }

    /** rb_str_capitalize / rb_str_capitalize_bang
    *
    */
    @Deprecated
    public IRubyObject capitalize19(ThreadContext context) {
        return capitalize(context);
    }

    @Deprecated
    public IRubyObject capitalize_bang19(ThreadContext context) {
        return capitalize_bang(context);
    }

    @JRubyMethod(name = "capitalize")
    public RubyString capitalize(ThreadContext context) {
        RubyString str = strDup(context.runtime);
        str.capitalize_bang(context);
        return str;
    }

    @JRubyMethod(name = "capitalize")
    public RubyString capitalize(ThreadContext context, IRubyObject arg) {
        RubyString str = strDup(context.runtime);
        str.capitalize_bang(context, arg);
        return str;
    }

    @JRubyMethod(name = "capitalize")
    public RubyString capitalize(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        RubyString str = strDup(context.runtime);
        str.capitalize_bang(context, arg0, arg1);
        return str;
    }

    @JRubyMethod(name = "capitalize!")
    public IRubyObject capitalize_bang(ThreadContext context) {
        return capitalize_bang(context, Config.CASE_UPCASE | Config.CASE_TITLECASE);
    }

    @JRubyMethod(name = "capitalize!")
    public IRubyObject capitalize_bang(ThreadContext context, IRubyObject arg) {
        return capitalize_bang(context, StringSupport.checkCaseMapOptions(context.runtime, arg, Config.CASE_UPCASE | Config.CASE_TITLECASE));
    }

    @JRubyMethod(name = "capitalize!")
    public IRubyObject capitalize_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return capitalize_bang(context, StringSupport.checkCaseMapOptions(context.runtime, arg0, arg1, Config.CASE_UPCASE | Config.CASE_TITLECASE));
    }

    private IRubyObject capitalize_bang(ThreadContext context, int flags) {
        modifyAndKeepCodeRange();
        Encoding enc = checkDummyEncoding();

        if (value.getRealSize() == 0) {
            modifyCheck();
            return context.nil;
        }

        flags = caseMap(context.runtime, flags, enc);
        if ((flags & Config.CASE_MODIFIED) != 0) {
            clearCodeRange();
            return this;
        } else {
            return context.nil;
        }
    }

    /** rb_str_dump
     *
     */
    @JRubyMethod(name = "dump")
    public IRubyObject dump() {
        final RubyClass metaClass = this.metaClass;
        ByteList outBytes = StringSupport.dumpCommon(metaClass.runtime, value);

        final RubyString result = new RubyString(metaClass.runtime, metaClass, outBytes);
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

    @JRubyMethod(name = "undump")
    public IRubyObject undump(ThreadContext context) {
        Ruby runtime = context.runtime;
        RubyString str = this;
        ByteList strByteList = str.value;
        byte[] sBytes = strByteList.unsafeBytes();
        int[] s = {strByteList.begin()};
        int sLen = strByteList.realSize();
        int s_end = s[0] + strByteList.realSize();
        Encoding enc[] = {str.getEncoding()};
        RubyString undumped = newString(runtime, sBytes, s[0], 0, enc[0]);
        boolean[] utf8 = {false};
        boolean[] binary = {false};
        int w;

        scanForCodeRange();
        if (!isAsciiOnly()) {
            throw runtime.newRuntimeError("non-ASCII character detected");
        }
        if (memchr(sBytes, s[0], '\0', strByteList.realSize()) != -1) {
            throw runtime.newRuntimeError("string contains null byte");
        }
        if (sLen < 2) return invalidFormat(runtime);
        if (sBytes[s[0]] != '"') return invalidFormat(runtime);

        /* strip '"' at the start */
        s[0]++;

        for (; ; ) {
            if (s[0] >= s_end) {
                throw runtime.newRuntimeError("unterminated dumped string");
            }

            if (sBytes[s[0]] == '"') {
                /* epilogue */
                s[0]++;
                if (s[0] == s_end) {
                    /* ascii compatible dumped string */
                    break;
                } else {
                    int size;

                    if (utf8[0]) {
                        throw runtime.newRuntimeError("dumped string contained Unicode escape but used force_encoding");
                    }

                    size = FORCE_ENCODING_BYTES.length;
                    if (s_end - s[0] <= size) return invalidFormat(runtime);
                    if (ByteList.memcmp(sBytes, s[0], FORCE_ENCODING_BYTES, 0, size) != 0) return invalidFormat(runtime);
                    s[0] += size;

                    int encname = s[0];
                    s[0] = memchr(sBytes, s[0], '"', s_end - s[0]);
                    size = s[0] - encname;
                    if (s[0] == -1) return invalidFormat(runtime);
                    if (s_end - s[0] != 2) return invalidFormat(runtime);
                    if (sBytes[s[0]] != '"' || sBytes[s[0] + 1] != ')') return invalidFormat(runtime);

                    Encoding enc2 = runtime.getEncodingService().findEncodingNoError(new ByteList(sBytes, encname, size));
                    if (enc2 == null) {
                        throw runtime.newRuntimeError("dumped string has unknown encoding name");
                    }
                    undumped.setEncoding(enc2);
                }
                break;
            }

            if (sBytes[s[0]] == '\\'){
                s[0]++;
                if (s[0] >= s_end) {
                    throw runtime.newRuntimeError("invalid escape");
                }
                undumped.undumpAfterBackslash(runtime, sBytes, s, s_end, enc, utf8, binary);
            }
            else{
                undumped.cat(sBytes, s[0]++, 1);
            }
        }

        undumped.infectBy(str);
        return undumped;
    }

    private static final IRubyObject invalidFormat(Ruby runtime) {
        throw runtime.newRuntimeError("invalid dumped string; not wrapped with '\"' nor '\"...\".force_encoding(\"...\")' form");
    }

    private void undumpAfterBackslash(Ruby runtime, byte[] ssBytes, int[] ss, int s_end, Encoding[] penc, boolean[] utf8, boolean[] binary) {
        int s = ss[0];
        long c;
        int codelen;
        int[] hexlen = {0};
        byte[] buf = new byte[6];
        Encoding encUtf8 = null;

        switch (ssBytes[s]) {
            case '\\':
            case '"':
            case '#':
                cat(ssBytes, s, 1); /* cat itself */
                s++;
                break;
            case 'n':
            case 'r':
            case 't':
            case 'f':
            case 'v':
            case 'b':
            case 'a':
            case 'e':
                buf[0] = unescapeAscii(ssBytes[s]);
                cat(buf, 0, 1);
                s++;
                break;
            case 'u':
                if (binary[0]) {
                    throw runtime.newRuntimeError("hex escape and Unicode escape are mixed");
                }
                utf8[0] = true;
                if (++s >= s_end) {
                    throw runtime.newRuntimeError("invalid Unicode escape");
                }
                if (encUtf8 == null) encUtf8 = UTF8Encoding.INSTANCE;
                if (penc[0] != encUtf8) {
                    penc[0] = encUtf8;
                    setEncoding(encUtf8);
                }
                if (ssBytes[s] == '{') { /* handle u{...} form */
                    s++;
                    for (;;) {
                        if (s >= s_end) {
                            throw runtime.newRuntimeError("unterminated Unicode escape");
                        }
                        if (ssBytes[s] == '}') {
                            s++;
                            break;
                        }
                        if (Character.isSpaceChar(ssBytes[s])) {
                            s++;
                            continue;
                        }
                        c = scanHex(ssBytes, s, s_end-s, hexlen);
                        if (hexlen[0] == 0 || hexlen[0] > 6) {
                            throw runtime.newRuntimeError("invalid Unicode escape");
                        }
                        if (c > 0x10ffff) {
                            throw runtime.newRuntimeError("invalid Unicode codepoint (too large)");
                        }
                        if (0xd800 <= c && c <= 0xdfff) {
                            throw runtime.newRuntimeError("invalid Unicode codepoint");
                        }
                        codelen = EncodingUtils.encMbcput((int) c, buf, 0, penc[0]);
                        cat(buf, 0, codelen);
                        s += hexlen[0];
                    }
                }
                else { /* handle uXXXX form */
                    c = scanHex(ssBytes, s, 4, hexlen);
                    if (hexlen[0] != 4) {
                        throw runtime.newRuntimeError("invalid Unicode escape");
                    }
                    if (0xd800 <= c && c <= 0xdfff) {
                        throw runtime.newRuntimeError("invalid Unicode codepoint");
                    }
                    codelen = EncodingUtils.encMbcput((int) c, buf, 0, penc[0]);
                    cat(buf, 0, codelen);
                    s += hexlen[0];
                }
                break;
            case 'x':
                if (utf8[0]) {
                    throw runtime.newRuntimeError("hex escape and Unicode escape are mixed");
                }
                binary[0] = true;
                if (++s >= s_end) {
                    throw runtime.newRuntimeError("invalid hex escape");
                }
                buf[0] = (byte) scanHex(ssBytes, s, 2, hexlen);
                if (hexlen[0] != 2) {
                    throw runtime.newRuntimeError("invalid hex escape");
                }
                cat(buf, 0, 1);
                s += hexlen[0];
                break;
            default:
                cat(ssBytes, s - 1, 2);
                s++;
        }

        ss[0] = s;
    }

    private static final byte[] hexdigit = "0123456789abcdef0123456789ABCDEF".getBytes();

    private static long scanHex(byte[] bytes, int start, int len, int[] retlen) {
        int s = start;
        long retval = 0;
        int tmp;

        while ((len--) > 0 && s < bytes.length && (tmp = memchr(hexdigit, 0, bytes[s], hexdigit.length)) != -1) {
            retval <<= 4;
            retval |= tmp & 15;
            s++;
        }
        retlen[0] = (s - start); /* less than len */
        return retval;
    }

    private static byte unescapeAscii(byte c) {
        switch (c) {
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 't':
                return '\t';
            case 'f':
                return '\f';
            case 'v':
                return '\13';
            case 'b':
                return '\010';
            case 'a':
                return '\007';
            case 'e':
                return 033;
            default:
                // not reached
                return -1;
        }
    }

    @JRubyMethod(name = "insert")
    public IRubyObject insert(ThreadContext context, IRubyObject indexArg, IRubyObject arg) {
        RubyString str = arg.convertToString();
        int index = RubyNumeric.num2int(indexArg);
        if (index == -1) {
            modifyCheck();
            return cat19(str);
        }
        if (index < 0) index++;
        replaceInternal19(index, 0, str);
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
    @JRubyMethod(name = "inspect")
    public RubyString inspect() {
        return inspect(getRuntime());
    }

    final RubyString inspect(final Ruby runtime) {
        return (RubyString) inspect(runtime, value).infectBy(this);
    }

    @Deprecated
    public IRubyObject inspect19() {
        return inspect();
    }

    // MRI: rb_str_escape
    public static IRubyObject rbStrEscape(ThreadContext context, RubyString str) {
        Ruby runtime = context.runtime;

        Encoding enc = str.getEncoding();
        ByteList strBL = str.getByteList();
        byte[] pBytes = strBL.unsafeBytes();
        int p = strBL.begin();
        int pend = p + strBL.realSize();
        int prev = p;
        RubyString result = RubyString.newEmptyString(runtime);
        boolean unicode_p = enc.isUnicode();
        boolean asciicompat = enc.isAsciiCompatible();

        while (p < pend) {
            int c, cc;
            int n = enc.length(pBytes, p, pend);
            if (!MBCLEN_CHARFOUND_P(n)) {
                if (p > prev) result.cat(pBytes, prev, p - prev);
                n = enc.minLength();
                if (pend < p + n)
                    n = (int)(pend - p);
                while ((n--) > 0) {
                    result.modify();
                    Sprintf.sprintf(runtime, result.getByteList(), "\\x%02X", pBytes[p] & 0377);
                    prev = ++p;
                }
                continue;
            }
            n = MBCLEN_CHARFOUND_LEN(n);
            c = enc.mbcToCode(pBytes, p, pend);
            p += n;
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
                if (p - n > prev) result.cat(pBytes, prev, p - n - prev);
                result.cat('\\');
                result.cat((byte) cc);
                prev = p;
            }
            else if (asciicompat && Encoding.isAscii(c) && (c < 0x7F && c > 31 /*ISPRINT(c)*/)) {
            }
            else {
                if (p - n > prev) result.cat(pBytes, prev, p - n - prev);
                result.modify();
                Sprintf.sprintf(runtime, result.getByteList(), StringSupport.escapedCharFormat(c, unicode_p), (c & 0xFFFFFFFFL));
                prev = p;
            }
        }
        if (p > prev) result.cat(pBytes, prev, p - prev);
        result.setEncodingAndCodeRange(USASCIIEncoding.INSTANCE, CR_7BIT);

        result.infectBy(str);
        return result;
    }

    @Deprecated
    public static IRubyObject inspect19(final Ruby runtime, ByteList byteList) {
        return inspect(runtime, byteList);
    }

    public static RubyString inspect(final Ruby runtime, ByteList byteList) {
        Encoding enc = byteList.getEncoding();
        byte bytes[] = byteList.getUnsafeBytes();
        int p = byteList.getBegin();
        int end = p + byteList.getRealSize();
        RubyString result = new RubyString(runtime, runtime.getString(), new ByteList(end - p));
        Encoding resultEnc = runtime.getDefaultInternalEncoding();
        boolean isUnicode = enc.isUnicode();
        boolean asciiCompat = enc.isAsciiCompatible();


        if (resultEnc == null) resultEnc = runtime.getDefaultExternalEncoding();
        if (!resultEnc.isAsciiCompatible()) resultEnc = USASCIIEncoding.INSTANCE;
        result.associateEncoding(resultEnc);
        result.cat('"');

        int prev = p;
        Encoding actEnc = EncodingUtils.getActualEncoding(enc, byteList);
        if (actEnc != enc) {
            enc = actEnc;
            if (isUnicode) isUnicode = enc instanceof UnicodeEncoding;
        }

        while (p < end) {
            int n = StringSupport.preciseLength(enc, bytes, p, end);
            if (!MBCLEN_CHARFOUND_P(n)) {
                if (p > prev) result.cat(bytes, prev, p - prev);
                n = enc.minLength();
                if (end < p + n) n = end - p;
                while (n-- > 0) {
                    result.modifyExpand(result.size() + 4);
                    Sprintf.sprintf(runtime, result.getByteList() ,"\\x%02X", bytes[p] & 0377);
                    prev = ++p;
                }
                continue;
            }
            n = MBCLEN_CHARFOUND_LEN(n);
            final int c = enc.mbcToCode(bytes, p, end); int cc;

            p += n;
            if ((asciiCompat || isUnicode) &&
                    (c == '"' || c == '\\' ||
                            (c == '#' &&
                                    p < end &&
                                    MBCLEN_CHARFOUND_P(StringSupport.preciseLength(enc, bytes, p, end)) &&
                                    ((cc = codePoint(runtime, enc, bytes, p, end)) == '$' ||
                                            cc == '@' || cc == '{')
                            )
                    )) {
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

            // FIXME: Can't use Encoding.isAscii because it does not treat int as unsigned 32-bit
            if ((enc == resultEnc && enc.isPrint(c)) || (asciiCompat && (c < 128 && c > 0) && enc.isPrint(c))) {
                continue;
            } else {
                if (p - n > prev) result.cat(bytes, prev, p - n - prev);
                Sprintf.sprintf(runtime, result.getByteList() , StringSupport.escapedCharFormat(c, isUnicode), (c & 0xFFFFFFFFL));
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

    // MRI: rb_str_length
    @JRubyMethod(name = {"length", "size"})
    public RubyFixnum rubyLength(final ThreadContext context) {
        return rubyLength(context.runtime);
    }

    private RubyFixnum rubyLength(final Ruby runtime) {
        return runtime.newFixnum(strLength());
    }

    @Deprecated
    public RubyFixnum length19() {
        return getRuntime().newFixnum(strLength());
    }

    @JRubyMethod(name = "bytesize")
    public RubyFixnum bytesize() {
        return getRuntime().newFixnum(value.getRealSize());
    }


    // CharSequence

    @Override
    public int length() {
        return strLength();
    }

    @Override
    public char charAt(int offset) {
        int length = value.getRealSize();

        if (length < 1) throw new StringIndexOutOfBoundsException(offset);

        Encoding enc = value.getEncoding();
        if (singleByteOptimizable(enc)) {
            if (offset >= length || offset < 0) throw new StringIndexOutOfBoundsException(offset);
            return (char) value.get(offset);
        }

        return multibyteCharAt(enc, offset, length);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        IRubyObject subStr = substr19(getRuntime(), start, end - start);
        if (subStr.isNil()) {
            throw new StringIndexOutOfBoundsException("String index out of range: <" + start + ", " + end + ")");
        }
        return (RubyString) subStr;
    }

    /**
     * A byte size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject byteSize(ThreadContext context, RubyString recv, IRubyObject[] args) {
        return recv.bytesize();
    }

    /** rb_str_empty
     *
     */
    @JRubyMethod(name = "empty?")
    public RubyBoolean empty_p(ThreadContext context) {
        return isEmpty() ? context.tru : context.fals;
    }

    public boolean isEmpty() {
        return value.length() == 0;
    }

    /** rb_str_append
     *
     */
    public RubyString append(IRubyObject other) {
        modifyCheck();

        if (other instanceof RubyFixnum) {
            cat(ConvertBytes.longToByteList(((RubyFixnum) other).value));
            return this;
        }
        if (other instanceof RubyFloat) {
            return cat((RubyString) ((RubyFloat) other).to_s());
        }
        if (other instanceof RubySymbol) {
            cat(((RubySymbol) other).getBytes());
            return this;
        }
        RubyString otherStr = other.convertToString();
        infectBy(otherStr);
        return cat(otherStr.value);
    }

    public RubyString append(RubyString other) {
        modifyCheck();
        infectBy(other);
        return cat(other.value);
    }

    public RubyString append19(IRubyObject other) {
        // fast path for fixnum straight into ascii-compatible bytelist
        if (other instanceof RubyFixnum && value.getEncoding().isAsciiCompatible()) {
            ConvertBytes.longIntoString(this, ((RubyFixnum) other).value);
            return this;
        }

        modifyCheck();

        if (other instanceof RubyFloat) {
            return cat19((RubyString) ((RubyFloat) other).to_s());
        } else if (other instanceof RubySymbol) {
            cat19(((RubySymbol) other).getBytes(), 0);
            return this;
        }

        return cat19(other.convertToString());
    }

    public RubyString appendAsDynamicString(IRubyObject other) {
        // fast path for fixnum straight into ascii-compatible bytelist
        if (other instanceof RubyFixnum && value.getEncoding().isAsciiCompatible()) {
            ConvertBytes.longIntoString(this, ((RubyFixnum) other).value);
            return this;
        }

        modifyCheck();

        if (other instanceof RubyFloat) {
            return cat19((RubyString) ((RubyFloat) other).to_s());
        } else if (other instanceof RubySymbol) {
            cat19(((RubySymbol) other).getBytes(), 0);
            return this;
        }

        return cat19(other.asString());
    }

    // NOTE: append(RubyString) should pbly just do the encoding aware cat
    final RubyString append19(RubyString other) {
        modifyCheck();
        return cat19(other);
    }

    /** rb_str_concat
     *
     */
    @JRubyMethod(name = "<<")
    public RubyString concatSingle(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) {
            // duplicated (default) return path - since its common
            return append19((RubyString) other);
        }
        if (other instanceof RubyFixnum) {
            long c = RubyNumeric.num2long(other);
            if (c < 0) {
                throw context.runtime.newRangeError(c + " out of char range");
            }
            return concatNumeric(context.runtime, (int)(c & 0xFFFFFFFF));
        }
        if (other instanceof RubyBignum) {
            if (((RubyBignum) other).getBigIntegerValue().signum() < 0) {
                throw context.runtime.newRangeError("negative string size (or size too big)");
            }
            long c = ((RubyBignum) other).getLongValue();
            return concatNumeric(context.runtime, (int) c);
        }
        if (other instanceof RubyFloat) {
            modifyCheck();
            return cat19((RubyString) ((RubyFloat) other).to_s());
        }
        if (other instanceof RubySymbol) throw context.runtime.newTypeError("can't convert Symbol into String");

        return append19(other.convertToString());
    }

    /** rb_str_concat
     *
     */
    @JRubyMethod(name = {"concat"})
    public RubyString concat(ThreadContext context, IRubyObject obj) {
        return concatSingle(context, obj);
    }

    /** rb_str_concat_multi
     *
     */
    @JRubyMethod(name = {"concat"}, rest = true)
    public RubyString concat(ThreadContext context, IRubyObject[] objs) {
        Ruby runtime = context.runtime;

        modifyCheck();

        if (objs.length > 0) {
            RubyString tmp = newStringLight(runtime, objs.length, getEncoding());

            for (IRubyObject obj : objs) {
                tmp.concatSingle(context, obj);
            }

            cat19(tmp);
        }

        return this;
    }

    public RubyString concat(IRubyObject other) {
        return concat(metaClass.runtime.getCurrentContext(), other);
    }

    @Deprecated
    public RubyString concat19(ThreadContext context, IRubyObject other) {
        return concat(context, other);
    }

    private RubyString concatNumeric(Ruby runtime, int c) {
        Encoding enc = value.getEncoding();
        int cl;

        try {
            cl = codeLength(enc, c);

            if (cl <= 0) {
                throw runtime.newRangeError(c + " out of char range or invalid code point");
            }

            modify19(value.getRealSize() + cl);

            if (enc == USASCIIEncoding.INSTANCE) {
                if (c > 0xff) throw runtime.newRangeError(c + " out of char range");
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

    /**
     * rb_str_prepend
     */
    @JRubyMethod(rest = true)
    public IRubyObject prepend(ThreadContext context, IRubyObject[] objs) {
        Ruby runtime = context.runtime;

        modifyCheck();

        if (objs.length > 0) {
            RubyString tmp = newStringLight(runtime, objs.length, getEncoding());

            for (IRubyObject obj : objs) {
                tmp.concat(context, obj);
            }

            replaceInternal19(0, 0, tmp);
        }

        return this;
    }

    public final RubyString prepend(byte ch) {
        modify(value.getRealSize() + 1);
        final int beg = value.getBegin();
        if (beg > 0) {
            value.getUnsafeBytes()[beg - 1] = ch;
            value.setBegin(beg - 1);
            return this;
        }
        value.prepend(ch);
        return this;
    }

    public final RubyString prepend(int ch) {
        return prepend((byte) ch);
    }

    /** rb_str_crypt
     *
     */
    @JRubyMethod(name = "crypt")
    public RubyString crypt(ThreadContext context, IRubyObject other) {
        Encoding ascii8bit = context.runtime.getEncodingService().getAscii8bitEncoding();
        RubyString otherStr = other.convertToString().strDup(context.runtime);
        otherStr.modify();
        otherStr.associateEncoding(ascii8bit);
        ByteList otherBL = otherStr.getByteList();
        if (otherBL.length() < 2) {
            throw context.runtime.newArgumentError("salt too short (need >=2 bytes)");
        }

        POSIX posix = context.runtime.getPosix();
        byte[] keyBytes = Arrays.copyOfRange(value.unsafeBytes(), value.begin(), value.begin() + value.realSize());
        byte[] saltBytes = Arrays.copyOfRange(otherBL.unsafeBytes(), otherBL.begin(), otherBL.begin() + otherBL.realSize());
        if (saltBytes[0] == 0 || saltBytes[1] == 0) {
            throw context.runtime.newArgumentError("salt too short (need >=2 bytes)");
        }
        byte[] cryptedString = posix.crypt(keyBytes, saltBytes);
        // We differ from MRI in that we do not process salt to make it work and we will
        // return any errors via errno.
        if (cryptedString == null) throw context.runtime.newErrnoFromInt(posix.errno());

        RubyString result = RubyString.newStringNoCopy(context.runtime, cryptedString, 0, cryptedString.length - 1);
        result.associateEncoding(ascii8bit);
        result.infectBy(this);
        result.infectBy(otherStr);
        return result;
    }

    /* RubyString aka rb_string_value */
    public static RubyString stringValue(IRubyObject object) {
        return (RubyString) (object instanceof RubyString ? object : object.convertToString());
    }

    @Deprecated
    public IRubyObject sub19(ThreadContext context, IRubyObject arg0, Block block) {
        return sub(context, arg0, block);
    }

    @Deprecated
    public IRubyObject sub19(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return sub(context, arg0, arg1, block);
    }

    @Deprecated
    public IRubyObject sub_bang19(ThreadContext context, IRubyObject arg0, Block block) {
        return sub_bang(context, arg0, block);
    }

    @Deprecated
    public IRubyObject sub_bang19(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return sub_bang(context, arg0, arg1, block);
    }

    /** rb_str_sub / rb_str_sub_bang
     *
     */

    @JRubyMethod(name = "sub", writes = BACKREF)
    public IRubyObject sub(ThreadContext context, IRubyObject arg0, Block block) {
        RubyString str = strDup(context.runtime);
        str.sub_bang(context, arg0, block);
        return str;
    }

    @JRubyMethod(name = "sub", writes = BACKREF)
    public IRubyObject sub(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        RubyString str = strDup(context.runtime);
        str.sub_bang(context, arg0, arg1, block);
        return str;
    }

    @JRubyMethod(name = "sub!", writes = BACKREF)
    public IRubyObject sub_bang(ThreadContext context, IRubyObject arg0, Block block) {
        Ruby runtime = context.runtime;
        frozenCheck();

        if (block.isGiven()) return subBangIter(context, arg0, null, block);
        throw runtime.newArgumentError(1, 2);
    }

    @JRubyMethod(name = "sub!", writes = BACKREF)
    public IRubyObject sub_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        Ruby runtime = context.runtime;
        IRubyObject hash = TypeConverter.convertToTypeWithCheck(context, arg1, runtime.getHash(), sites(context).to_hash_checked);
        frozenCheck();

        if (hash == context.nil) {
            return subBangNoIter(context, arg0, arg1.convertToString());
        }
        return subBangIter(context, arg0, (RubyHash) hash, block);
    }

    private static RubyRegexp asRegexpArg(final Ruby runtime, final IRubyObject arg0) {
        return arg0 instanceof RubyRegexp ? (RubyRegexp) arg0 :
            RubyRegexp.newRegexp(runtime, RubyRegexp.quote(getStringForPattern(runtime, arg0).getByteList(), false), new RegexpOptions());
    }

    private IRubyObject subBangIter(ThreadContext context, IRubyObject arg0, RubyHash hash, Block block) {
        if (arg0 instanceof RubyRegexp ) {
            return subBangIter(context, (RubyRegexp) arg0, hash, block);
        } else {
            return subBangIter(context, getStringForPattern(context.runtime, arg0),  hash, block);
        }
    }

    private IRubyObject subBangIter(ThreadContext context, RubyString pattern, RubyHash hash, Block block) {
        int len = value.getRealSize();
        byte[] bytes = value.getUnsafeBytes();
        Encoding enc = value.getEncoding();
        final int mBeg = StringSupport.index(getByteList(), pattern.getByteList(), 0, checkEncoding(pattern));

        if (mBeg > -1) {
            final RubyString repl; final int tuFlags;
            final Ruby runtime = context.runtime;
            final int mLen = pattern.size();
            final int mEnd = mBeg + mLen;
            final RubyMatchData match = new RubyMatchData(runtime);

            match.initMatchData(this, mBeg, pattern);

            // set backref for user
            context.setBackRef(match);

            IRubyObject subStr = makeShared(runtime, mBeg, mLen);
            if (hash == null) {
                tuFlags = 0;
                repl = objAsString(context, block.yield(context, subStr));
            } else {
                tuFlags = hash.flags;
                repl = objAsString(context, hash.op_aref(context, subStr));
            }

            modifyCheck(bytes, len, enc);

            return subBangCommon(context, mBeg, mEnd, repl, tuFlags | repl.flags);
        }

        // set backref for user
        return context.clearBackRef();
    }

    private IRubyObject subBangIter(ThreadContext context, RubyRegexp regexp, RubyHash hash, Block block) {
        Regex pattern = regexp.getPattern();
        Regex prepared = regexp.preparePattern(this);

        int begin = value.getBegin();
        int len = value.getRealSize();
        int range = begin + len;
        byte[] bytes = value.getUnsafeBytes();
        Encoding enc = value.getEncoding();
        final Matcher matcher = prepared.matcher(bytes, begin, range);

        if (RubyRegexp.matcherSearch(context, matcher, begin, range, Option.NONE) >= 0) {
            RubyMatchData match = RubyRegexp.createMatchData(context, this, matcher, pattern);
            match.regexp = regexp;

            // set backref for user
            context.setBackRef(match);

            final int mBeg = matcher.getBegin(), mEnd = matcher.getEnd();

            final RubyString repl; final int tuFlags;
            IRubyObject subStr = makeShared(context.runtime, mBeg, mEnd - mBeg);
            if (hash == null) {
                tuFlags = 0;
                repl = objAsString(context, block.yield(context, subStr));
            } else {
                tuFlags = hash.flags;
                repl = objAsString(context, hash.op_aref(context, subStr));
            }

            modifyCheck(bytes, len, enc);

            return subBangCommon(context, mBeg, mEnd, repl, tuFlags | repl.flags);
        }

        // set backref for user
        return context.clearBackRef();
    }

    private IRubyObject subBangNoIter(ThreadContext context, IRubyObject arg0, RubyString repl) {
        if (arg0 instanceof RubyRegexp) {
            return subBangNoIter(context, (RubyRegexp) arg0, repl);
        } else {
            return subBangNoIter(context, getStringForPattern(context.runtime, arg0), repl);
        }
    }

    private IRubyObject subBangNoIter(ThreadContext context, RubyString pattern, RubyString repl) {
        final int mBeg = StringSupport.index(getByteList(), pattern.getByteList(), 0, checkEncoding(pattern));
        if (mBeg > -1) {
            final int mEnd = mBeg + pattern.size();
            final RubyMatchData match = new RubyMatchData(context.runtime);

            match.initMatchData(this, mBeg, pattern);

            // set backref for user
            context.setBackRef(match);

            repl = RubyRegexp.regsub(context, repl, this, REPL_MOCK_REGEX, null, mBeg, mEnd);

            return subBangCommon(context, mBeg, mEnd, repl, repl.flags);
        }

        // set backref for user
        return context.clearBackRef();
    }

    private IRubyObject subBangNoIter(ThreadContext context, RubyRegexp regexp, RubyString repl) {
        RubyMatchData match = subBangMatch(context, regexp, repl);
        if (match != null) {
            repl = RubyRegexp.regsub(context, repl, this, regexp.pattern, match.regs, match.begin, match.end);

            // set backref for user
            context.setBackRef(match);

            return subBangCommon(context, match.begin, match.end, repl, repl.flags);
        }

        // set backref for user
        return context.clearBackRef();
    }

    /**
     * sub! but without any frame globals ...
     * @note Internal API, subject to change!
     * @param context
     * @param regexp
     * @param repl
     * @return sub result
     */
    public final IRubyObject subBangFast(ThreadContext context, RubyRegexp regexp, RubyString repl) {
        RubyMatchData match = subBangMatch(context, regexp, repl);
        if (match != null) {
            repl = RubyRegexp.regsub(context, repl, this, regexp.pattern, match.regs, match.begin, match.end);
            subBangCommon(context, match.begin, match.end, repl, repl.flags);
            return match;
        }
        return context.nil;
    }

    private RubyMatchData subBangMatch(ThreadContext context, RubyRegexp regexp, RubyString repl) {
        Regex pattern = regexp.getPattern();
        Regex prepared = regexp.preparePattern(this);

        int begin = value.getBegin();
        int range = begin + value.getRealSize();
        final Matcher matcher = prepared.matcher(value.getUnsafeBytes(), begin, range);

        if (RubyRegexp.matcherSearch(context, matcher, begin, range, Option.NONE) >= 0) {
            RubyMatchData match = RubyRegexp.createMatchData(context, this, matcher, pattern);
            match.regexp = regexp;
            return match;
        }
        return null;
    }

    private RubyString subBangCommon(ThreadContext context, final int beg, final int end,
        final RubyString repl, int tuFlags) { // the sub replacement string

        Encoding enc = StringSupport.areCompatible(this, repl);
        if (enc == null) enc = subBangVerifyEncoding(context, repl, beg, end);

        final ByteList replValue = repl.value;
        final int replSize = replValue.getRealSize();
        final int plen = end - beg;

        if (replSize > plen) {
            modifyExpand(value.getRealSize() + replSize - plen);
        } else {
            modify19();
        }

        final ByteList value = this.value;
        final int size = value.getRealSize();

        associateEncoding(enc);

        int cr = getCodeRange();
        if (cr > CR_UNKNOWN && cr < CR_BROKEN) {
            int cr2 = repl.getCodeRange();
            if (cr2 == CR_BROKEN || (cr == CR_VALID && cr2 == CR_7BIT)) {
                cr = CR_UNKNOWN;
            } else {
                cr = cr2;
            }
        }

        if (replSize != plen) {
            int src = value.getBegin() + beg + plen;
            int dst = value.getBegin() + beg + replSize;
            System.arraycopy(value.getUnsafeBytes(), src, value.getUnsafeBytes(), dst, size - beg - plen);
        }
        System.arraycopy(replValue.getUnsafeBytes(), replValue.getBegin(), value.getUnsafeBytes(), value.getBegin() + beg, replSize);
        value.setRealSize(size + replSize - plen);
        setCodeRange(cr);
        return (RubyString) infectBy(tuFlags); // this
    }

    private Encoding subBangVerifyEncoding(ThreadContext context, final RubyString repl, final int beg, final int end) {
        final ByteList value = this.value;
        byte[] bytes = value.getUnsafeBytes();
        int p = value.getBegin();
        int len = value.getRealSize();
        Encoding strEnc = value.getEncoding();
        if (codeRangeScan(strEnc, bytes, p, beg) != CR_7BIT ||
            codeRangeScan(strEnc, bytes, p + end, len - end) != CR_7BIT) {
            throw context.runtime.newEncodingCompatibilityError(
                    "incompatible character encodings " + strEnc + " and " + repl.value.getEncoding());
        }
        return repl.value.getEncoding();
    }

    @Deprecated
    public IRubyObject gsub19(ThreadContext context, IRubyObject arg0, Block block) {
        return gsub(context, arg0, block);
    }

    @Deprecated
    public IRubyObject gsub19(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return gsub(context, arg0, arg1, block);
    }

    @Deprecated
    public IRubyObject gsub_bang19(ThreadContext context, IRubyObject arg0, Block block) {
        return gsub_bang(context, arg0, block);
    }

    @Deprecated
    public IRubyObject gsub_bang19(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return gsub_bang(context, arg0, arg1, block);
    }

    @JRubyMethod(name = "gsub", writes = BACKREF)
    public IRubyObject gsub(ThreadContext context, IRubyObject arg0, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "gsub", arg0);

        return gsubCommon(context, block, null, null, arg0, false, 0);

    }

    @JRubyMethod(name = "gsub", writes = BACKREF)
    public IRubyObject gsub(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return gsubImpl(context, arg0, arg1, block, false);
    }

    @JRubyMethod(name = "gsub!", writes = BACKREF)
    public IRubyObject gsub_bang(ThreadContext context, IRubyObject arg0, Block block) {
        checkFrozen();

        if (!block.isGiven()) return enumeratorize(context.runtime, this, "gsub!", arg0);

        return gsubCommon(context, block, null, null, arg0, true, 0);
    }

    @JRubyMethod(name = "gsub!", writes = BACKREF)
    public IRubyObject gsub_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        checkFrozen();

        return gsubImpl(context, arg0, arg1, block, true);
    }

    private IRubyObject gsubImpl(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block, final boolean bang) {
        IRubyObject tryHash = TypeConverter.convertToTypeWithCheck(context, arg1, context.runtime.getHash(), sites(context).to_hash_checked);

        final RubyHash hash;
        final RubyString str;
        final int tuFlags;
        if (tryHash == context.nil) {
            hash = null;
            str = arg1.convertToString();
            tuFlags = str.flags;
        } else {
            hash = (RubyHash) tryHash;
            str = null;
            tuFlags = hash.flags & TAINTED_F;
        }

        return gsubCommon(context, block, str, hash, arg0, bang, tuFlags);
    }

    public RubyString gsubFast(ThreadContext context, RubyRegexp regexp, RubyString repl, Block block) {
        return (RubyString) gsubCommon(context, block, repl, null, regexp, false, repl.flags, false);
    }

    private IRubyObject gsubCommon(ThreadContext context, Block block, RubyString repl,
            RubyHash hash, IRubyObject arg0, final boolean bang, int tuFlags) {
        return gsubCommon(context, block, repl, hash, arg0, bang, tuFlags, true);
    }

    private IRubyObject gsubCommon(ThreadContext context, Block block, RubyString repl,
            RubyHash hash, IRubyObject arg0, final boolean bang, int tuFlags, boolean useBackref) {
        if (arg0 instanceof RubyRegexp) {
            return gsubCommon(context, block, repl, hash, (RubyRegexp) arg0, bang, tuFlags, useBackref);
        } else {
            return gsubCommon(context, block, repl, hash, getStringForPattern(context.runtime, arg0), bang, tuFlags, useBackref);
        }
    }

    /**
     * A Regex instance is required to satisfy the type signature of RubyRegexp.regsub
     * In the code paths possible for a string pattern a single instance of a blank regex
     * is enough.
     */
    private static final Regex REPL_MOCK_REGEX = new Regex(new String(""));

    // MRI: str_gsub, roughly
    private IRubyObject gsubCommon(ThreadContext context, Block block, RubyString repl,
            RubyHash hash, RubyString pattern, final boolean bang, int tuFlags, boolean useBackref) {
        final Ruby runtime = context.runtime;

        final byte[] spBytes = value.getUnsafeBytes();
        final int spBeg = value.getBegin();
        final int spLen = value.getRealSize();
        final int patternLen = pattern.size();
        final Encoding patternEnc = this.checkEncoding(pattern);

        int beg = StringSupport.index(getByteList(), pattern.getByteList(), 0, patternEnc);
        int begz;
        if (beg < 0) {
            // set backref for user
            if (useBackref) context.clearBackRef();

            return bang ? context.nil : strDup(runtime); /* bang: true, no match, no substitution */
        }

        int offset = 0; int cp = spBeg; //int n = 0;
        RubyString dest = new RubyString(runtime, metaClass, new ByteList(spLen + 30));
        final Encoding str_enc = value.getEncoding();
        dest.setEncoding(str_enc);
        dest.setCodeRange(str_enc.isAsciiCompatible() ? CR_7BIT : CR_VALID);

        RubyMatchData match = null;
        do {
            final RubyString val;
            begz = beg;
            int endz = beg + patternLen;

            if (repl != null) {     // string given
                val = RubyRegexp.regsub(context, repl, this, REPL_MOCK_REGEX, null, begz, endz);
            } else {
                if (hash != null) { // hash given
                    val = objAsString(context, hash.op_aref(context, pattern));
                } else {            // block given
                    match = new RubyMatchData(runtime);
                    match.initMatchData(this, begz, pattern);

                    // set backref for user
                    if (useBackref) context.setBackRef(match);

                    val = objAsString(context, block.yield(context, pattern.strDup(runtime)));
                }
                modifyCheck(spBytes, spLen, str_enc);
                if (bang) frozenCheck();
            }

            tuFlags |= val.flags;

            int len = begz - offset;
            if (len != 0) dest.cat(spBytes, cp, len, str_enc);
            dest.cat19(val);
            offset = endz;
            if (begz == endz) {
                if (spLen <= endz) break;
                len = StringSupport.encFastMBCLen(spBytes, spBeg + endz, spBeg + spLen, str_enc);
                dest.cat(spBytes, spBeg + endz, len, str_enc);
                offset = endz + len;
            }
            cp = spBeg + offset;
            if (offset > spLen) break;
            beg = StringSupport.index(getByteList(), pattern.getByteList(), offset, patternEnc);
        } while (beg >= 0);

        if (spLen > offset) dest.cat(spBytes, cp, spLen - offset, str_enc);

        if (useBackref) {
            if (match != null) { // block given
                // set backref for user
                context.setBackRef(match);
            } else {
                match = new RubyMatchData(runtime);

                match.initMatchData(this, begz, pattern);

                // set backref for user
                context.setBackRef(match);
            }
        }

        if (bang) {
            view(dest.value);
            setCodeRange(dest.getCodeRange());
            return infectBy(tuFlags);
        }
        return dest.infectBy(tuFlags | flags);
    }

    private IRubyObject gsubCommon(ThreadContext context, Block block, RubyString repl,
            RubyHash hash, RubyRegexp regexp, final boolean bang, int tuFlags, boolean useBackref) {
        final Ruby runtime = context.runtime;
        Regex pattern = regexp.getPattern();
        Regex prepared = regexp.preparePattern(this);

        final byte[] spBytes = value.getUnsafeBytes();
        final int spBeg = value.getBegin();
        final int spLen = value.getRealSize();

        final Matcher matcher = prepared.matcher(spBytes, spBeg, spBeg + spLen);

        int beg = RubyRegexp.matcherSearch(context, matcher, spBeg, spBeg + spLen, Option.NONE);
        if (beg < 0) {
            // set backref for user
            if (useBackref) context.clearBackRef();

            return bang ? context.nil : strDup(runtime); /* bang: true, no match, no substitution */
        }

        int offset = 0; int cp = spBeg; //int n = 0;
        RubyString dest = new RubyString(runtime, metaClass, new ByteList(spLen + 30));
        final Encoding str_enc = value.getEncoding();
        dest.setEncoding(str_enc);
        dest.setCodeRange(str_enc.isAsciiCompatible() ? CR_7BIT : CR_VALID);

        RubyMatchData match = null;
        do {
            final RubyString val;
            int begz = matcher.getBegin();
            int endz = matcher.getEnd();

            if (repl != null) {     // string given
                val = RubyRegexp.regsub(context, repl, this, pattern, matcher);
            } else {
                final RubyString substr = makeShared(runtime, begz, endz - begz);
                if (hash != null) { // hash given
                    val = objAsString(context, hash.op_aref(context, substr));
                } else {            // block given
                    match = RubyRegexp.createMatchData(context, this, matcher, pattern);
                    match.regexp = regexp;

                    // set backref for user
                    if (useBackref) context.setBackRef(match);

                    val = objAsString(context, block.yield(context, substr));
                }
                modifyCheck(spBytes, spLen, str_enc);
                if (bang) frozenCheck();
            }

            tuFlags |= val.flags;

            int len = begz - offset;
            if (len != 0) dest.cat(spBytes, cp, len, str_enc);
            dest.cat19(val);
            offset = endz;
            if (begz == endz) {
                if (spLen <= endz) break;
                len = StringSupport.encFastMBCLen(spBytes, spBeg + endz, spBeg + spLen, str_enc);
                dest.cat(spBytes, spBeg + endz, len, str_enc);
                offset = endz + len;
            }
            cp = spBeg + offset;
            if (offset > spLen) break;
            beg = RubyRegexp.matcherSearch(context, matcher, cp, spBeg + spLen, Option.NONE);
        } while (beg >= 0);

        if (spLen > offset) dest.cat(spBytes, cp, spLen - offset, str_enc);

        if (useBackref) {
            // set backref for user
            if (match != null) { // block given
                context.setBackRef(match);
            } else {
                match = RubyRegexp.createMatchData(context, this, matcher, pattern);

                match.regexp = regexp;

                context.setBackRef(match);
            }
        }

        if (bang) {
            view(dest.value);
            setCodeRange(dest.getCodeRange());
            return infectBy(tuFlags);
        }
        return dest.infectBy(tuFlags | flags);
    }

    /** rb_str_index_m
     *
     */
    @JRubyMethod(name = "index", writes = BACKREF)
    public IRubyObject index(ThreadContext context, IRubyObject arg0) {
        return indexCommon19(context, arg0, 0);
    }

    @JRubyMethod(name = "index", writes = BACKREF)
    public IRubyObject index(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        int pos = RubyNumeric.num2int(arg1);
        if (pos < 0) {
            pos += strLength();
            if (pos < 0) {
                // set backref for user
                if (arg0 instanceof RubyRegexp) context.clearBackRef();

                return context.nil;
            }
        }
        return indexCommon19(context, arg0, pos);
    }

    @Deprecated
    public IRubyObject index19(ThreadContext context, IRubyObject arg0) {
        return index(context, arg0);
    }

    @Deprecated
    public IRubyObject index19(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return index(context, arg0, arg1);
    }

    private IRubyObject indexCommon19(ThreadContext context, IRubyObject sub, int pos) {
        if (sub instanceof RubyRegexp) {
            if (pos > strLength()) return context.nil;
            RubyRegexp regSub = (RubyRegexp) sub;
            pos = singleByteOptimizable() ? pos :
                    StringSupport.nth(checkEncoding(regSub), value.getUnsafeBytes(), value.getBegin(),
                            value.getBegin() + value.getRealSize(),
                                      pos) - value.getBegin();
            pos = regSub.adjustStartPos(this, pos, false);
            pos = regSub.search(context, this, pos, false);
            pos = subLength(pos);
        } else if (sub instanceof RubyString) {
            pos = StringSupport.index(this, (RubyString) sub, pos, this.checkEncoding((RubyString) sub));
            pos = subLength(pos);
        } else {
            IRubyObject tmp = sub.checkStringType();
            if (tmp == context.nil) throw context.runtime.newTypeError("type mismatch: " + sub.getMetaClass().getName() + " given");
            pos = StringSupport.index(this, (RubyString) tmp, pos, this.checkEncoding((RubyString) tmp));
            pos = subLength(pos);
        }

        return pos == -1 ? context.nil : RubyFixnum.newFixnum(context.runtime, pos);
    }

    // MRI: rb_strseq_index
    private int strseqIndex(final RubyString sub, int offset, boolean inBytes) {
        int s, sptr, e;
        int pos, len, slen;
        boolean single_byte = singleByteOptimizable();
        Encoding enc;

        enc = checkEncoding(sub);
        if (sub.isCodeRangeBroken()) return -1;

        len = (inBytes || single_byte) ? value.realSize() : strLength(); /* rb_enc_check */
        slen = inBytes ? sub.value.realSize() : sub.strLength(); /* rb_enc_check */
        if (offset < 0) {
            offset += len;
            if (offset < 0) return -1;
        }
        if (len - offset < slen) return -1;

        byte[] sBytes = value.unsafeBytes();
        s = value.begin();
        e = s + value.realSize();
        if (offset != 0) {
            if (!inBytes) offset = offset(enc, sBytes, s, e, offset, single_byte);
            s += offset;
        }
        if (slen == 0) return offset;
        /* need proceed one character at a time */
        byte[] sptrBytes = sub.value.unsafeBytes();
        sptr = sub.value.begin();
        slen = sub.value.realSize();
        len = value.realSize() - offset;
        for (;;) {
            int t;
            pos = memsearch(sptrBytes, sptr, slen, sBytes, s, len, enc);
            if (pos < 0) return pos;
            t = enc.rightAdjustCharHead(sBytes, s, s+pos, e);
            if (t == s + pos) break;
            len -= t - s;
            if (len <= 0) return -1;
            offset += t - s;
            s = t;
        }
        return pos + offset;
    }

    @Deprecated
    public IRubyObject rindex19(ThreadContext context, IRubyObject arg0) {
        return rindex(context, arg0);
    }

    @Deprecated
    public IRubyObject rindex19(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return rindex(context, arg0, arg1);
    }

    /** rb_str_rindex_m
     *
     */
    @JRubyMethod(name = "rindex", writes = BACKREF)
    public IRubyObject rindex(ThreadContext context, IRubyObject arg0) {
        return rindexCommon(context, arg0, strLength());
    }

    @JRubyMethod(name = "rindex", writes = BACKREF)
    public IRubyObject rindex(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        int pos = RubyNumeric.num2int(arg1);
        int length = strLength();
        if (pos < 0) {
            pos += length;
            if (pos < 0) {
                // set backref for user
                if (arg0 instanceof RubyRegexp) context.clearBackRef();

                return context.nil;
            }
        }
        if (pos > length) pos = length;
        return rindexCommon(context, arg0, pos);
    }

    private IRubyObject rindexCommon(ThreadContext context, final IRubyObject sub, int pos) {
        if (sub instanceof RubyRegexp) {
            RubyRegexp regSub = (RubyRegexp) sub;
            pos = StringSupport.offset(
                    value.getEncoding(), value.getUnsafeBytes(), value.getBegin(), value.getBegin() + value.getRealSize(),
                    pos, singleByteOptimizable());
            pos = regSub.search(context, this, pos, true);
            pos = subLength(pos);
            if (pos >= 0) return RubyFixnum.newFixnum(context.runtime, pos);
        } else if (sub instanceof RubyString) {
            Encoding enc = checkEncoding((RubyString) sub);
            pos = StringSupport.rindex(value,
                    StringSupport.strLengthFromRubyString(this, enc),
                    StringSupport.strLengthFromRubyString(((RubyString) sub), enc),
                    pos, (RubyString) sub, enc
            );
        } else {
            IRubyObject tmp = sub.checkStringType();
            if (tmp.isNil()) throw context.runtime.newTypeError("type mismatch: " + sub.getMetaClass().getName() + " given");
            Encoding enc = checkEncoding((RubyString) tmp);
            pos = StringSupport.rindex(value,
                    StringSupport.strLengthFromRubyString(this, enc),
                    StringSupport.strLengthFromRubyString(((RubyString) tmp), enc),
                    pos, (RubyString) tmp, enc
            );
        }
        if (pos >= 0) return RubyFixnum.newFixnum(context.runtime, pos);
        return context.nil;
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

        if (len < 0 || beg > length) return runtime.getNil();

        if (beg < 0) {
            beg += length;
            if (beg < 0) return runtime.getNil();
        }
        if (beg + len > length) len = length - beg;

        if (len <= 0) {
            len = 0;
        }

        return makeShared(runtime, beg, len);
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
            ThreadContext context = runtime.getCurrentContext();
            StringSites sites = sites(context);
            if (RubyRange.isRangeLike(context, idx, sites.respond_to_begin, sites.respond_to_end)) {
                RubyRange range = RubyRange.rangeFromRangeLike(context, idx, sites.begin, sites.end, sites.exclude_end);

                int[] begLen = range.begLenInt(getByteList().length(), 0);
                return begLen == null ? runtime.getNil() : byteSubstr(runtime, begLen[0], begLen[1]);
            } else {
                index = RubyNumeric.num2int(idx);
            }
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
            return makeShared(runtime, beg, len);
        } else {
            if (beg + len > length) len = length - beg;
            return multibyteSubstr19(runtime, enc, len, beg, length);
        }
    }

    private IRubyObject multibyteSubstr19(Ruby runtime, Encoding enc, int len, int beg, int length) {
        int p;
        int s = value.getBegin();
        int end = s + length;
        byte[] bytes = value.getUnsafeBytes();

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
                return makeShared(runtime, p - s, e - p);
            } else {
                beg += StringSupport.strLengthFromRubyString(this, enc);
                if (beg < 0) return runtime.getNil();
            }
        } else if (beg > 0 && beg > StringSupport.strLengthFromRubyString(this, enc)) {
            return runtime.getNil();
        }
        if (len == 0) {
            p = 0;
        } else if (isCodeRangeValid() && enc.isUTF8()) {
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

        return makeShared(runtime, p - s, len);
    }

    private char multibyteCharAt(Encoding enc, int beg, int length) {
        int p;
        int s = value.getBegin();
        int end = s + length;
        byte[] bytes = value.getUnsafeBytes();


        if (beg > 0 && beg > StringSupport.strLengthFromRubyString(this, enc)) {
            throw new StringIndexOutOfBoundsException(beg);
        }

        if (isCodeRangeValid() && enc.isUTF8()) {
            p = StringSupport.utf8Nth(bytes, s, end, beg);
        } else if (enc.isFixedWidth()) {
            int w = enc.maxLength();
            p = s + beg * w;
            if (p > end || w > end - p) {
                throw new StringIndexOutOfBoundsException(beg);
            }
        } else if ((p = StringSupport.nth(enc, bytes, s, end, beg)) == end) {
            throw new StringIndexOutOfBoundsException(beg);
        }
        int codepoint = enc.mbcToCode(bytes, p, end);

        if (Character.isBmpCodePoint(codepoint)) {
            return (char) codepoint;
        }

        // we can only return high surrogate here
        return Character.highSurrogate(codepoint);
    }

    /* rb_str_splice */
    private IRubyObject replaceInternal(int beg, int len, RubyString repl) {
        StringSupport.replaceInternal(beg, len, this, repl);
        return infectBy(repl);
    }

    private void replaceInternal19(int beg, int len, RubyString repl) {
        StringSupport.replaceInternal19(getRuntime(), beg, len, this, repl);
        infectBy(repl);
    }

    /** rb_str_aref, rb_str_aref_m
     *
     */
    @JRubyMethod(name = {"[]", "slice"}, writes = BACKREF)
    public IRubyObject op_aref(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;
        if (arg instanceof RubyFixnum) {
            return op_aref(runtime, RubyNumeric.fix2int((RubyFixnum)arg));
        } else if (arg instanceof RubyRegexp) {
            return subpat(context, (RubyRegexp) arg);
        } else if (arg instanceof RubyString) {
            RubyString str = (RubyString)arg;
            return StringSupport.index(this, str, 0, this.checkEncoding(str)) != -1 ? str.strDup(runtime) : context.nil;
        } else if (arg instanceof RubyRange) {
            int len = strLength();
            int[] begLen = ((RubyRange) arg).begLenInt(len, 0);
            return begLen == null ? context.nil : substr19(runtime, begLen[0], begLen[1]);
        } else {
            StringSites sites = sites(context);
            if (RubyRange.isRangeLike(context, arg, sites.respond_to_begin, sites.respond_to_end)) {
                int len = strLength();
                RubyRange range = RubyRange.rangeFromRangeLike(context, arg, sites.begin, sites.end, sites.exclude_end);

                int[] begLen = range.begLenInt(len, 0);
                return begLen == null ? context.nil : substr19(runtime, begLen[0], begLen[1]);
            }
        }
        return op_aref(runtime, RubyNumeric.num2int(arg));
    }

    @JRubyMethod(name = {"[]", "slice"}, writes = BACKREF)
    public IRubyObject op_aref(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        Ruby runtime = context.runtime;
        if (arg1 instanceof RubyRegexp) return subpat(context, (RubyRegexp) arg1, arg2);
        return substr19(runtime, RubyNumeric.num2int(arg1), RubyNumeric.num2int(arg2));
    }

    @JRubyMethod
    public IRubyObject byteslice(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        return byteSubstr(context.runtime, RubyNumeric.num2int(arg1), RubyNumeric.num2int(arg2));
    }

    @JRubyMethod
    public IRubyObject byteslice(ThreadContext context, IRubyObject arg) {
        return byteARef(context.runtime, arg);
    }

    private IRubyObject op_aref(Ruby runtime, int idx) {
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

    private void subpatSet(ThreadContext context, RubyRegexp regexp, IRubyObject backref, IRubyObject repl) {
        Ruby runtime = context.runtime;

        int result = regexp.searchString(context, this, 0, false);

        if (result < 0) throw runtime.newIndexError("regexp not matched");

        // this cast should be ok, since nil matchdata will be < 0 above
        RubyMatchData match = context.getLocalMatch();

        int nth = backref == null ? 0 : subpatSetCheck(runtime, match.backrefNumber(context.runtime, backref), match.regs);

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

        // set backref for user
        context.setBackRef(match);
    }

    private IRubyObject subpat(ThreadContext context, RubyRegexp regex, IRubyObject backref) {
        int result = regex.searchString(context, this, 0, false);

        if (result >= 0) {
            RubyMatchData match = context.getLocalMatch();

            // set backref for user
            context.setBackRef(match);

            return RubyRegexp.nth_match(match.backrefNumber(context.runtime, backref), match);
        }

        context.clearBackRef();

        return context.nil;
    }

    private IRubyObject subpat(ThreadContext context, RubyRegexp regex) {
        int result = regex.searchString(context, this, 0, false);

        if (result >= 0) {
            RubyMatchData match = context.getLocalMatch();

            // set backref for user
            context.setBackRef(match);

            return RubyRegexp.nth_match(0, match);
        }

        // set backref for user
        context.clearBackRef();

        return context.nil;
    }

    /** rb_str_aset, rb_str_aset_m
     *
     */
    @JRubyMethod(name = "[]=", writes = BACKREF)
    public IRubyObject op_aset(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        if (arg0 instanceof RubyFixnum) {
            return op_aset(context, RubyNumeric.fix2int((RubyFixnum)arg0), arg1);
        } else if (arg0 instanceof RubyRegexp) {
            subpatSet(context, (RubyRegexp) arg0, null, arg1);
            return arg1;
        } else if (arg0 instanceof RubyString) {
            RubyString orig = (RubyString) arg0;
            int beg = StringSupport.index(this, orig, 0, checkEncoding(orig));
            if (beg < 0) throw context.runtime.newIndexError("string not matched");
            beg = subLength(beg);
            replaceInternal19(beg, orig.strLength(), arg1.convertToString());
            return arg1;
        } else if (arg0 instanceof RubyRange) {
            int[] begLen = ((RubyRange) arg0).begLenInt(strLength(), 2);
            replaceInternal19(begLen[0], begLen[1], arg1.convertToString());
            return arg1;
        } else {
            StringSites sites = sites(context);
            if (RubyRange.isRangeLike(context, arg0, sites.respond_to_begin, sites.respond_to_end)) {
                RubyRange rng = RubyRange.rangeFromRangeLike(context, arg0, sites.begin, sites.end, sites.exclude_end);

                int[] begLen = rng.begLenInt(strLength(), 2);
                replaceInternal19(begLen[0], begLen[1], arg1.convertToString());

                return arg1;
            }
        }
        return op_aset(context, RubyNumeric.num2int(arg0), arg1);
    }

    private IRubyObject op_aset(ThreadContext context, int idx, IRubyObject arg1) {
        StringSupport.replaceInternal19(context.runtime, idx, 1, this, arg1.convertToString());
        return arg1;
    }

    @JRubyMethod(name = "[]=", writes = BACKREF)
    public IRubyObject op_aset(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        if (arg0 instanceof RubyRegexp) {
            subpatSet(context, (RubyRegexp)arg0, arg1, arg2);
        } else {
            int beg = RubyNumeric.num2int(arg0);
            int len = RubyNumeric.num2int(arg1);
            checkLength(len);
            RubyString repl = arg2.convertToString();
            StringSupport.replaceInternal19(context.runtime, beg, len, this, repl);
        }
        return arg2;
    }

    @Deprecated
    public IRubyObject slice_bang19(ThreadContext context, IRubyObject arg0) {
        return slice_bang(context, arg0);
    }

    @Deprecated
    public IRubyObject slice_bang19(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return slice_bang(context, arg0, arg1);
    }

    /** rb_str_slice_bang
     *
     */
    @JRubyMethod(name = "slice!", writes = BACKREF)
    public IRubyObject slice_bang(ThreadContext context, IRubyObject arg0) {
        IRubyObject result = op_aref19(context, arg0);
        if (result.isNil()) {
            modifyCheck(); // keep cr ?
        } else {
            op_aset(context, arg0, RubyString.newEmptyString(context.runtime));
        }
        return result;
    }

    @JRubyMethod(name = "slice!", writes = BACKREF)
    public IRubyObject slice_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        IRubyObject result = op_aref(context, arg0, arg1);
        if (result.isNil()) {
            modifyCheck(); // keep cr ?
        } else {
            op_aset19(context, arg0, arg1, RubyString.newEmptyString(context.runtime));
        }
        return result;
    }

    @Deprecated
    public IRubyObject succ19(ThreadContext context) {
        return succ(context);
    }

    @Deprecated
    public IRubyObject succ_bang19() {
        return succ_bang();
    }

    @JRubyMethod(name = {"succ", "next"})
    public IRubyObject succ(ThreadContext context) {
        Ruby runtime = context.runtime;
        final RubyString str;
        if (value.getRealSize() > 0) {
            str = new RubyString(runtime, metaClass, StringSupport.succCommon(runtime, value));
            // TODO: rescan code range ?
        } else {
            str = newEmptyString(runtime, getType(), value.getEncoding());
        }
        return str.infectBy(this);
    }

    @JRubyMethod(name = {"succ!", "next!"})
    public IRubyObject succ_bang() {
        modifyCheck();
        if (value.getRealSize() > 0) {
            value = StringSupport.succCommon(getRuntime(), value);
            shareLevel = SHARE_LEVEL_NONE;
            // TODO: rescan code range ?
        }
        return this;
    }

    @Deprecated
    public final IRubyObject upto19(ThreadContext context, IRubyObject end, Block block) {
        return upto(context, end, block);
    }

    @Deprecated
    public final IRubyObject upto19(ThreadContext context, IRubyObject end, IRubyObject excl, Block block) {
        return upto(context, end, excl, block);
    }

    /** rb_str_upto_m
     *
     */
    @JRubyMethod(name = "upto")
    public final IRubyObject upto(ThreadContext context, IRubyObject end, Block block) {
        Ruby runtime = context.runtime;
        return block.isGiven() ? uptoCommon(context, end, false, block) : enumeratorize(runtime, this, "upto", end);
    }

    @JRubyMethod(name = "upto")
    public final IRubyObject upto(ThreadContext context, IRubyObject end, IRubyObject excl, Block block) {
        return block.isGiven() ? uptoCommon(context, end, excl.isTrue(), block) :
            enumeratorize(context.runtime, this, "upto", new IRubyObject[]{end, excl});
    }

    final IRubyObject uptoCommon(ThreadContext context, IRubyObject arg, boolean excl, Block block) {
        if (arg instanceof RubySymbol) throw context.runtime.newTypeError("can't convert Symbol into String");
        return uptoCommon(context, arg.convertToString(), excl, block, false);
    }

    final IRubyObject uptoCommon(ThreadContext context, RubyString end, boolean excl, Block block, boolean asSymbol) {
        final Ruby runtime = context.runtime;

        Encoding enc = checkEncoding(end);
        boolean isAscii = scanForCodeRange() == CR_7BIT && end.scanForCodeRange() == CR_7BIT;
        if (value.getRealSize() == 1 && end.value.getRealSize() == 1 && isAscii) {
            byte c = value.getUnsafeBytes()[value.getBegin()];
            byte e = end.value.getUnsafeBytes()[end.value.getBegin()];
            if (c > e || (excl && c == e)) return this;
            while (true) {
                ByteList s = RubyInteger.singleCharByteList(c);
                block.yield(context, asSymbol ? runtime.newSymbol(s) : newStringShared(runtime, s, enc, CR_7BIT));

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
                if (!ASCII.isDigit(bytes[s] & 0xff)) return uptoCommonNoDigits(context, end, excl, block, asSymbol);
                s++;
            }
            s = end.value.getBegin();
            send = s + end.value.getRealSize();
            bytes = end.value.getUnsafeBytes();

            while (s < send) {
                if (!ASCII.isDigit(bytes[s] & 0xff)) return uptoCommonNoDigits(context, end, excl, block, asSymbol);
                s++;
            }

            IRubyObject b = stringToInum(10);
            IRubyObject e = end.stringToInum(10);

            RubyArray argsArr = RubyArray.newArray(runtime, RubyFixnum.newFixnum(runtime, value.length()), context.nil);

            if (b instanceof RubyFixnum && e instanceof RubyFixnum) {
                long bl = RubyNumeric.fix2long(b);
                long el = RubyNumeric.fix2long(e);

                while (bl <= el) {
                    if (excl && bl == el) break;
                    argsArr.eltSetOk(1, RubyFixnum.newFixnum(runtime, bl));
                    ByteList to = new ByteList(value.length() + 5);
                    Sprintf.sprintf(to, "%.*d", argsArr);
                    RubyString str = RubyString.newStringNoCopy(runtime, to, USASCIIEncoding.INSTANCE, CR_7BIT);
                    block.yield(context, asSymbol ? runtime.newSymbol(str.toString()) : str);
                    bl++;
                }
            } else {
                StringSites sites = sites(context);
                CallSite op = excl ? sites.op_lt : sites.op_le;

                while (op.call(context, b, b, e).isTrue()) {
                    argsArr.eltSetOk(1, b);
                    ByteList to = new ByteList(value.length() + 5);
                    Sprintf.sprintf(to, "%.*d", argsArr);
                    RubyString str = RubyString.newStringNoCopy(runtime, to, USASCIIEncoding.INSTANCE, CR_7BIT);
                    block.yield(context, asSymbol ? runtime.newSymbol(str.toString()) : str);
                    b = sites.succ.call(context, b, b);
                }
            }
            return this;
        }

        return uptoCommonNoDigits(context, end, excl, block, asSymbol);
    }

    private IRubyObject uptoCommonNoDigits(ThreadContext context, RubyString end, boolean excl, Block block, boolean asSymbol) {
        Ruby runtime = context.runtime;
        int n = op_cmp(end);
        if (n > 0 || (excl && n == 0)) return this;
        StringSites sites = sites(context);
        CallSite succ = sites.succ;
        IRubyObject afterEnd = succ.call(context, end, end);
        RubyString current = strDup(context.runtime);

        while (!current.op_equal(context, afterEnd).isTrue()) {
            IRubyObject next = null;
            if (excl || !current.op_equal(context, end).isTrue()) next = succ.call(context, current, current);
            block.yield(context, asSymbol ? runtime.newSymbol(current.toString()) : current);
            if (next == null) break;
            current = next.convertToString();
            if (excl && current.op_equal(context, end).isTrue()) break;
            if (current.getByteList().length() > end.getByteList().length() || current.getByteList().length() == 0) break;
        }
        return this;
    }

    final IRubyObject uptoEndless(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;
        StringSites sites = sites(context);
        CallSite succ = sites.succ;

        boolean isAscii = scanForCodeRange() == CR_7BIT;
        RubyString current = strDup(context.runtime);

        if (isAscii && ASCII.isDigit(value.getUnsafeBytes()[value.getBegin()])) {
            IRubyObject b = stringToInum(10);
            RubyArray argsArr = RubyArray.newArray(runtime, RubyFixnum.newFixnum(runtime, value.length()), context.nil);
            ByteList to;

            if (b instanceof RubyFixnum) {
                long bl = RubyNumeric.fix2long(b);

                while (bl < RubyFixnum.MAX) {
                    argsArr.eltSetOk(1, RubyFixnum.newFixnum(runtime, bl));
                    to = new ByteList(value.length() + 5);
                    Sprintf.sprintf(to, "%.*d", argsArr);
                    current = RubyString.newStringNoCopy(runtime, to, USASCIIEncoding.INSTANCE, CR_7BIT);
                    block.yield(context, current);
                    bl++;
                }

                argsArr.eltSetOk(1, RubyFixnum.newFixnum(runtime, bl));
                to = new ByteList(value.length() + 5);
                Sprintf.sprintf(to, "%.*d", argsArr);
                current = RubyString.newStringNoCopy(runtime, to, USASCIIEncoding.INSTANCE, CR_7BIT);
            }
        }

        while (true) {
            IRubyObject next = succ.call(context, current, current);
            block.yield(context, current);
            if (next == null) break;
            current = next.convertToString();
            if (current.getByteList().length() == 0) break;
        }

        return this;
    }

    @Deprecated
    public final RubyBoolean include_p19(ThreadContext context, IRubyObject obj) {
        return include_p(context, obj);
    }

    /** rb_str_include
     *
     */
    @JRubyMethod(name = "include?")
    public RubyBoolean include_p(ThreadContext context, IRubyObject obj) {
        RubyString coerced = obj.convertToString();
        return StringSupport.index(this, coerced, 0, this.checkEncoding(coerced)) == -1 ? context.fals : context.tru;
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
        if (i < 0 || i >= value.getRealSize()) return context.nil;
        return RubyFixnum.newFixnum(runtime, value.getUnsafeBytes()[value.getBegin() + i] & 0xff);
    }

    @JRubyMethod
    public IRubyObject setbyte(ThreadContext context, IRubyObject index, IRubyObject val) {
        int i = RubyNumeric.num2int(index);
        int normalizedIndex = checkIndexForRef(i, value.getRealSize());
        RubyInteger v = val.convertToInteger();
        IRubyObject w = v.modulo(context, (long)256);
        int b = RubyNumeric.num2int(w) & 0xff;

        modify19();
        value.getUnsafeBytes()[normalizedIndex] = (byte)b;
        return val;
    }

    /** rb_str_to_i
     *
     */
    @JRubyMethod(name = "to_i")
    public IRubyObject to_i() {
        return stringToInum(10);
    }

    /** rb_str_to_i
     *
     */
    @JRubyMethod(name = "to_i")
    public IRubyObject to_i(IRubyObject arg0) {
        int base = (int) arg0.convertToInteger().getLongValue();
        if (base < 0) {
            throw getRuntime().newArgumentError("illegal radix " + base);
        }
        return stringToInum(base);
    }

    @Deprecated
    public IRubyObject to_i19() {
        return to_i();
    }

    @Deprecated
    public IRubyObject to_i19(IRubyObject arg0) {
        return to_i(arg0);
    }

    /** rb_str_to_inum
     *
     */
    public IRubyObject stringToInum(int base, boolean badcheck) {
        final ByteList str = this.value;
        if (!str.getEncoding().isAsciiCompatible()) {
            throw getRuntime().newEncodingCompatibilityError("ASCII incompatible encoding: " + str.getEncoding());
        }

        return ConvertBytes.byteListToInum(getRuntime(), str, base, badcheck);
    }

    public final IRubyObject stringToInum(int base) {
        return stringToInum(base, false);
    }

    @Deprecated
    public final IRubyObject stringToInum19(int base, boolean badcheck) {
        return stringToInum(base, badcheck);
    }

    /** rb_str_oct
     *
     */
    @JRubyMethod(name = "oct")
    public IRubyObject oct(ThreadContext context) {
        return stringToInum(-8, false);
    }

    @Deprecated
    public IRubyObject oct19(ThreadContext context) {
        return oct(context);
    }

    /** rb_str_hex
     *
     */
    @JRubyMethod(name = "hex")
    public IRubyObject hex(ThreadContext context) {
        return stringToInum(16, false);
    }

    @Deprecated
    public IRubyObject hex19(ThreadContext context) {
        return hex(context);
    }

    /** rb_str_to_f
     *
     */
    @JRubyMethod(name = "to_f")
    public IRubyObject to_f() {
        return RubyNumeric.str2fnum(getRuntime(), this, false);
    }

    @Deprecated
    public IRubyObject to_f19() {
        return to_f();
    }

    /** rb_str_split_m
     *
     */

    @Deprecated
    public RubyArray split19(ThreadContext context) { return split(context); }

    @Deprecated
    public RubyArray split19(ThreadContext context, IRubyObject arg0) { return split(context, arg0); }

    @Deprecated
    public RubyArray split19(ThreadContext context, IRubyObject arg0, IRubyObject arg1) { return split(context, arg0, arg1); }

    private void populateCapturesForSplit(Ruby runtime, RubyArray result, RubyMatchData match) {
        for (int i = 1; i < match.numRegs(); i++) {
            int beg = match.begin(i);
            if (beg == -1) continue;
            result.append(makeShared(runtime, beg, match.end(i) - beg));
        }
    }

    public RubyArray split(ThreadContext context) {
        return split(context, context.nil);
    }

    public RubyArray split(ThreadContext context, IRubyObject arg0) {
        return splitCommon(context, arg0, false, 0, 0);
    }

    @JRubyMethod(name = "split")
    public IRubyObject splitWithBlock(ThreadContext context, Block block) {
        return splitWithBlock(context, context.nil, block);
    }

    @JRubyMethod(name = "split")
    public IRubyObject splitWithBlock(ThreadContext context, IRubyObject arg0, Block block) {
        RubyArray array = split(context, arg0);
        if (!block.isGiven()) {
            return array;
        }

        for (int i = 0; i < array.getLength(); i++) {
            block.yield(context, array.eltOk(i));
        }

        return this;
    }

    public RubyArray split(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        final int lim = RubyNumeric.num2int(arg1);
        RubyArray array;
        if (lim <= 0) {
            array = splitCommon(context, arg0, false, lim, 1);
        } else if (lim == 1) {
            Ruby runtime = context.runtime;
            array = value.getRealSize() == 0 ? runtime.newArray() : runtime.newArray(this.strDup(runtime));
        } else {
            array = splitCommon(context, arg0, true, lim, 1);
        }

        return array;
    }

    @JRubyMethod(name = "split")
    public IRubyObject splitWithBlock(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        RubyArray array = split(context, arg0, arg1);
        if (!block.isGiven()) {
            return array;
        }

        for (int i = 0; i < array.getLength(); i++) {
            block.yield(context, array.eltOk(i));
        }

        return this;
    }

    @Deprecated
    public RubyArray split19(IRubyObject spat, ThreadContext context, boolean useBackref) {
        return splitCommon(context, spat, false, value.realSize(), 0);
    }

    /**
     * Split for ext (Java) callers (does not write $~).
     * @param delimiter
     * @return splited entries
     */
    public RubyArray split(RubyRegexp delimiter) {
        return doSplit(delimiter, 0);
    }

    /**
     * Split for ext (Java) callers (does not write $~).
     * @param delimiter
     * @param limit
     * @return splited entries
     */
    public RubyArray split(RubyRegexp delimiter, int limit) {
        return doSplit(delimiter, limit);
    }

    /**
     * Split for ext (Java) callers (does not write $~).
     * @param delimiter
     * @return splited entries
     */
    public RubyArray split(RubyString delimiter) {
        return doSplit(delimiter, 0);
    }

    /**
     * Split for ext (Java) callers (does not write $~).
     * @param delimiter
     * @param limit
     * @return splited entries
     */
    public RubyArray split(RubyString delimiter, int limit) {
        return doSplit(delimiter, limit);
    }

    private RubyArray doSplit(IRubyObject delimiter, final int limit) {
        ThreadContext context = getRuntime().getCurrentContext();
        if (limit == 1) {
            Ruby runtime = context.runtime;
            return isEmpty() ? runtime.newEmptyArray() : runtime.newArray(this.strDup(runtime));
        }
        return splitCommon(context, delimiter, limit > 0, limit, 1);
    }

    final RubyArray split(ThreadContext context, RubyRegexp spat, boolean useBackref) {
        return splitCommon(context, spat, false, value.realSize(), 0);
    }

    // MRI: rb_str_split_m, overall structure
    private RubyArray splitCommon(ThreadContext context, IRubyObject spat, final boolean limit, final int lim, final int i) {
        final RubyArray result;
        if (spat == context.nil && (spat = context.runtime.getGlobalVariables().get("$;")) == context.nil) {
            result = awkSplit(context.runtime, limit, lim, i);
        } else {
            spat = getPatternQuoted(context, spat, false);
            if (spat instanceof RubyString) {
                ByteList spatValue = ((RubyString)spat).value;
                int len = spatValue.getRealSize();
                Encoding spatEnc = spatValue.getEncoding();
                ((RubyString)spat).mustnotBroken(context);
                if (len == 0) {
                    // headius FIXME: MRI has a single-entry global cache here to reduce this cost in a loop
                    RubyRegexp pattern = RubyRegexp.newRegexpFromStr(context.runtime, (RubyString) spat, 0);
                    result = regexSplit(context, pattern, limit, lim, i);
                } else {
                    final int c;
                    byte[]bytes = spatValue.getUnsafeBytes();
                    int p = spatValue.getBegin();
                    if (spatEnc.isAsciiCompatible()) {
                        c = len == 1 ? bytes[p] & 0xff : -1;
                    } else {
                        c = len == StringSupport.preciseLength(spatEnc, bytes, p, p + len) ? spatEnc.mbcToCode(bytes, p, p + len) : -1;
                    }
                    result = c == ' ' ? awkSplit(context.runtime, limit, lim, i) : stringSplit(context, (RubyString)spat, limit, lim, i);
                }
            } else {
                result = regexSplit(context, (RubyRegexp) spat, limit, lim, i);
            }
        }

        if (!limit && lim == 0) {
            while (result.size() > 0 && ((RubyString) result.eltInternal(result.size() - 1)).value.getRealSize() == 0) {
                result.pop(context);
            }
        }

        return result;
    }

    /**
     * Call regexpSplit using a thread-local backref holder to avoid cross-thread pollution.
     */
    private RubyArray regexSplit(ThreadContext context, RubyRegexp pattern, boolean limit, int lim, int i) {
        Ruby runtime = context.runtime;

        RubyArray result = runtime.newArray();

        int ptr = value.getBegin();
        int len = value.getRealSize();
        byte[] bytes = value.getUnsafeBytes();
        Encoding enc = value.getEncoding();

        boolean captures = pattern.getPattern().numberOfCaptures() != 0;

        int end, beg = 0;
        boolean lastNull = false;
        int start = beg;
        while ((end = pattern.searchString(context, this, start, false)) >= 0) {
            RubyMatchData match = context.getLocalMatch();
            if (start == end && match.begin(0) == match.end(0)) {
                if (len == 0) {
                    result.append(newEmptyString(runtime, metaClass).infectBy(this));
                    break;
                } else if (lastNull) {
                    result.append(makeShared(runtime, beg, StringSupport.length(enc, bytes, ptr + beg, ptr + len)));
                    beg = start;
                } else {
                    if ((ptr + start) == ptr + len) {
                        start++;
                    } else {
                        start += StringSupport.length(enc, bytes, ptr + start, ptr + len);
                    }
                    lastNull = true;
                    continue;
                }
            } else {
                result.append(makeShared(runtime, beg, end - beg));
                beg = match.end(0);
                start = beg;
            }
            lastNull = false;

            if (captures) populateCapturesForSplit(runtime, result, match);
            if (limit && lim <= ++i) break;
        }

        if (len > 0 && (limit || len > beg || lim < 0)) result.append(makeShared(runtime, beg, len - beg));

        return result;
    }

    // MRI: rb_str_split_m, when split_type = awk
    private RubyArray awkSplit(final Ruby runtime, boolean limit, int lim, int i) {
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
                    result.append(makeShared(runtime, b, e - b));
                    skip = true;
                    b = p - ptr;
                    if (limit) i++;
                } else {
                    e = p - ptr;
                }
            }
        }

        if (len > 0 && (limit || len > b || lim < 0)) result.append(makeShared(runtime, b, len - b));
        return result;
    }

    // MRI: rb_str_split_m, when split_type = string
    private RubyArray stringSplit(ThreadContext context, RubyString spat, boolean limit, int lim, int i) {
        Ruby runtime = context.runtime;
        mustnotBroken(context);

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

        while (p < realSize && (e = indexOf(bytes, begin, realSize, patternBytes, patternBegin, patternRealSize, p, enc)) >= 0) {
            int t = enc.rightAdjustCharHead(bytes, p + begin, e + begin, begin + realSize) - begin;
            if (t != e) {
                p = t;
                continue;
            }
            result.append(makeShared(runtime, p, e - p));
            p = e + pattern.getRealSize();
            if (limit && lim <= ++i) break;
        }

        if (value.getRealSize() > 0 && (limit || value.getRealSize() > p || lim < 0)) {
            result.append(makeShared(runtime, p, value.getRealSize() - p));
        }

        return result;
    }

    // TODO: make the ByteList version public and use it, rather than copying here
    static int indexOf(byte[] source, int sourceOffset, int sourceCount, byte[] target, int targetOffset, int targetCount, int fromIndex, Encoding enc) {
        if (fromIndex >= sourceCount) return (targetCount == 0 ? sourceCount : -1);
        if (fromIndex < 0) fromIndex = 0;
        if (targetCount == 0) return fromIndex;

        byte first  = target[targetOffset];
        int max = sourceOffset + (sourceCount - targetCount);

        int i = sourceOffset + fromIndex;
        while (i <= max) {
            while (i <= max && source[i] != first)
                i += StringSupport.length(enc, source, i, sourceOffset + sourceCount);

            if (i <= max) {
                int j = i + 1;
                int end = j + targetCount - 1;
                for (int k = targetOffset + 1; j < end && source[j] == target[k]; j++, k++);

                if (j == end) return i - sourceOffset;
                i += StringSupport.length(enc, source, i, sourceOffset + sourceCount);
            }
        }
        return -1;
    }

    private static RubyString getStringForPattern(Ruby runtime, IRubyObject obj) {
        if (obj instanceof RubyString) return (RubyString) obj;
        IRubyObject val = obj.checkStringType();
        if (val.isNil()) throw runtime.newTypeError("wrong argument type " + obj.getMetaClass() + " (expected Regexp)");
        return (RubyString) val;
    }

    /** get_pat (used by match/match19)
     *
     */
    private static RubyRegexp getPattern(Ruby runtime, IRubyObject obj) {
        if (obj instanceof RubyRegexp) return (RubyRegexp) obj;
        return RubyRegexp.newRegexpFromStr(runtime, getStringForPattern(runtime, obj), 0);
    }

    // MRI: get_pat_quoted
    private static IRubyObject getPatternQuoted(ThreadContext context, IRubyObject pat, final boolean check) {
        IRubyObject val;

        if (pat instanceof RubyRegexp) return pat;

        if (!(pat instanceof RubyString)) {
            val = pat.checkStringType();
            if (val == context.nil) {
                TypeConverter.checkType(context, pat, context.runtime.getRegexp());
            }
            pat = val;
        }
        if (check && ((RubyString) pat).isBrokenString()) {
            // MRI code does a raise of TypeError with a special regexp string constructor that raises RegexpError
            throw context.runtime.newRegexpError("invalid byte sequence in " + ((RubyString) pat).getEncoding());
        }
        return pat;
    }

    /** rb_str_scan
     *
     */
    @JRubyMethod(name = "scan", writes = BACKREF)
    public IRubyObject scan(ThreadContext context, IRubyObject pat, Block block) {
        final RubyString str = this;

        IRubyObject result;
        int last = -1, prev = 0;
        final int[] startp = {0};

        pat = getPatternQuoted(context, pat, true);
        mustnotBroken(context);
        if (!block.isGiven()) {
            RubyArray ary = null;
            while ((result = scanOnce(context, str, pat, startp)) != context.nil) {
                last = prev;
                prev = startp[0];
                if (ary == null) ary = context.runtime.newArray(4);
                ary.append(result);
            }
            if (last >= 0) patternSearch(context, pat, str, last);
            return ary == null ? context.runtime.newEmptyArray() : ary;
        }

        final byte[] pBytes = value.unsafeBytes();
        final int len = value.realSize();

        while ((result = scanOnce(context, str, pat, startp)) != context.nil) {
            last = prev;
            prev = startp[0];
            block.yieldSpecific(context, result);
            str.modifyCheck(pBytes, len);
        }
        if (last >= 0) patternSearch(context, pat, str, last);
        return this;
    }

    // MRI: mustnot_broken
    private void mustnotBroken(ThreadContext context) {
        if (scanForCodeRange() == CR_BROKEN) {
            throw context.runtime.newArgumentError("invalid byte sequence in " + getEncoding());
        }
    }

    // MRI: scan_once
    private static IRubyObject scanOnce(ThreadContext context, RubyString str, IRubyObject pat, int[] startp) {
        if (patternSearch(context, pat, str, startp[0]) >= 0) {
            final RubyMatchData match = context.getLocalMatch();
            final int matchEnd = match.end(0);
            if (match.begin(0) == matchEnd) {
                Encoding enc = str.getEncoding();
                /*
                 * Always consume at least one character of the input string
                 */
                if (str.size() > matchEnd) {
                    final ByteList strValue = str.value;
                    startp[0] = matchEnd + encFastMBCLen(strValue.unsafeBytes(), strValue.begin() + matchEnd,
                            strValue.begin() + strValue.realSize(), enc);
                } else {
                    startp[0] = matchEnd + 1;
                }
            } else {
                startp[0] = matchEnd;
            }
            if (match.numRegs() == 1) {
                return RubyRegexp.nth_match(0, match);
            }
            int size = match.numRegs();
            RubyArray result = RubyArray.newBlankArrayInternal(context.runtime, size - 1);
            for (int i = 1; i < size; i++) {
                result.eltInternalSet(i - 1, RubyRegexp.nth_match(i, match));
            }
            result.realLength = size - 1;

            return result;
        }

        return context.nil;
    }

    // MRI: rb_pat_search
    private static int patternSearch(ThreadContext context, final IRubyObject pattern, RubyString str, final int pos) {
        if (pattern instanceof RubyString) {
            final RubyString strPattern = (RubyString) pattern;
            final int beg = str.strseqIndex(strPattern, pos, true);
            if (beg >= 0) {
                context.setLocalMatch(setBackRefString(context, str, beg, strPattern));
            } else {
                context.clearLocalMatch();

                // set backref for user
                context.clearBackRef();
            }
            return beg;
        }

        int result = ((RubyRegexp) pattern).searchString(context, str, pos, false);

        // set backref for user
        if (result >= 0) {
            context.setBackRef(context.getLocalMatch());
        } else {
            context.clearBackRef();
        }

        return result;
    }

    // MRI: rb_backref_set_string
    private static RubyMatchData setBackRefString(ThreadContext context, RubyString str, int pos, RubyString pattern) {
        final RubyMatchData match = RubyRegexp.createMatchData(context, str, pos, pattern);

        match.infectBy(pattern);

        context.setBackRef(match);

        return match;
    }

    @JRubyMethod(name = "start_with?")
    public IRubyObject start_with_p(ThreadContext context) {
        return context.fals;
    }

    @JRubyMethod(name = "start_with?")
    public IRubyObject start_with_p(ThreadContext context, IRubyObject arg) {
        if (arg instanceof RubyRegexp) {
            return ((RubyRegexp) arg).startsWith(context, this) ? context.tru : context.fals;
        }
        return startsWith(arg.convertToString()) ? context.tru : context.fals;
    }

    @JRubyMethod(name = "start_with?", rest = true)
    public IRubyObject start_with_p(ThreadContext context, IRubyObject[]args) {
        for (int i = 0; i < args.length; i++) {
            if (start_with_p(context, args[i]).isTrue()) return context.tru;
        }
        return context.fals;
    }

    public boolean startsWith(final RubyString str) {
        checkEncoding(str);

        int otherLength = str.value.getRealSize();

        if (otherLength == 0) return true; // other is '', so return true

        if (value.getRealSize() < otherLength) return false;

        return value.startsWith(str.value);
    }

    @JRubyMethod(name = "end_with?")
    public IRubyObject end_with_p(ThreadContext context) {
        return context.fals;
    }

    @JRubyMethod(name = "end_with?")
    public IRubyObject end_with_p(ThreadContext context, IRubyObject arg) {
        return endWith(arg) ? context.tru : context.fals;
    }

    @JRubyMethod(name = "end_with?", rest = true)
    public IRubyObject end_with_p(ThreadContext context, IRubyObject[]args) {
        for (int i = 0; i < args.length; i++) {
            if (endWith(args[i])) return context.tru;
        }
        return context.fals;
    }

    // MRI: rb_str_end_with, loop body
    private boolean endWith(IRubyObject tmp) {
        int p, s, e;
        Encoding enc;

        tmp = tmp.convertToString();
        ByteList tmpBL = ((RubyString)tmp).value;
        // MRI does not have this condition because starting at end of string can still dereference \0
        if (tmpBL.getRealSize() == 0) return true;
        enc = checkEncoding((RubyString)tmp);
        if (value.realSize() < tmpBL.realSize()) return false;
        p = value.begin();
        e = p + value.realSize();
        s = e - tmpBL.realSize();
        if (enc.leftAdjustCharHead(value.unsafeBytes(), p, s, e) != s) {
            return false;
        }
        if (ByteList.memcmp(value.unsafeBytes(), s, tmpBL.unsafeBytes(), tmpBL.begin(), tmpBL.realSize()) == 0) {
            return true;
        }
        return false;
    }

    public boolean endsWithAsciiChar(char c) {
        ByteList value = this.value;
        int size;

        return value.getEncoding().isAsciiCompatible() && (size = value.realSize()) > 0 && value.get(size - 1) == c;
    }

    @JRubyMethod(name = "delete_prefix")
    public IRubyObject delete_prefix(ThreadContext context, IRubyObject prefix) {
        int prefixlen = deletedPrefixLength(prefix);

        if (prefixlen <= 0) return strDup(context.runtime);

        return makeShared(context.runtime, prefixlen, size() - prefixlen);
    }

    @JRubyMethod(name = "delete_suffix")
    public IRubyObject delete_suffix(ThreadContext context, IRubyObject suffix) {
        int suffixlen = deletedSuffixLength(suffix);

        if (suffixlen <= 0) return strDup(context.runtime);

        return makeShared(context.runtime, 0, size() - suffixlen);
    }

    @JRubyMethod(name = "delete_prefix!")
    public IRubyObject delete_prefix_bang(ThreadContext context, IRubyObject prefix) {
        modifyAndKeepCodeRange();

        int prefixlen = deletedPrefixLength(prefix);

        if (prefixlen <= 0) return context.nil;

        // MRI: rb_str_drop_bytes, in a nutshell
        modify();
        value.view(prefixlen, value.realSize() - prefixlen);
        clearCodeRange();

        return this;
    }

    @JRubyMethod(name = "delete_suffix!")
    public IRubyObject delete_suffix_bang(ThreadContext context, IRubyObject suffix) {
        checkFrozen();

        int suffixlen = deletedSuffixLength(suffix);

        if (suffixlen <= 0) return context.nil;

        int olen = size();

        modifyAndKeepCodeRange();

        int len = olen - suffixlen;

        value.realSize(len);

        if (!isCodeRangeAsciiOnly()) {
            clearCodeRange();
        }

        return this;
    }

    private int deletedPrefixLength(IRubyObject _prefix) {
        RubyString prefix = _prefix.convertToString();

        if (prefix.isBrokenString()) return 0;

        checkEncoding(prefix);

        /* return 0 if not start with prefix */
        int prefixlen = prefix.size();

        if (prefixlen <= 0) return 0;

        int olen = size();

        if (olen < prefixlen) return 0;

        byte[] strBytes = value.unsafeBytes();
        int strptr = value.begin();
        byte[] prefixBytes = prefix.value.unsafeBytes();
        int prefixptr = prefix.value.begin();

        if (ByteList.memcmp(strBytes, strptr, prefixBytes, prefixptr, prefixlen) != 0) return 0;

        return prefixlen;
    }

    private int deletedSuffixLength(IRubyObject _suffix) {
        RubyString suffix = _suffix.convertToString();

        if (suffix.isBrokenString()) return 0;

        Encoding enc = checkEncoding(suffix);

        /* return 0 if not start with suffix */
        int suffixlen = suffix.size();

        if (suffixlen <= 0) return 0;

        int olen = size();

        if (olen < suffixlen) return 0;
        byte[] strBytes = value.unsafeBytes();
        int strptr = value.begin();
        byte[] suffixBytes = suffix.value.unsafeBytes();
        int suffixptr = suffix.value.begin();
        int s = strptr + olen - suffixlen;

        if (ByteList.memcmp(strBytes, s, suffixBytes, suffixptr, suffixlen) != 0) return 0;

        if (enc.leftAdjustCharHead(strBytes, strptr, s, strptr + olen) != s) return 0;

        return suffixlen;
    }

    private static final ByteList SPACE_BYTELIST = RubyInteger.singleCharByteList((byte) ' ');

    private IRubyObject justify(Ruby runtime, IRubyObject arg0, int jflag) {
        RubyString result = justifyCommon(runtime, SPACE_BYTELIST,
                1,
                true, EncodingUtils.STR_ENC_GET(this), RubyFixnum.num2int(arg0), jflag);
        if (getCodeRange() != CR_BROKEN) result.setCodeRange(getCodeRange());
        return result;
    }

    private IRubyObject justify(IRubyObject arg0, IRubyObject arg1, int jflag) {
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
        if (result.strLength() > strLength()) result.infectBy(padStr);
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

        RubyString result = new RubyString(runtime, metaClass, res);
        if (result.strLength() > strLength()) result.infectBy(this);
        result.associateEncoding(enc);
        return result;
    }

    /** rb_str_ljust
     *
     */

    @Deprecated
    public IRubyObject ljust19(IRubyObject arg0) { return ljust(arg0); }

    @Deprecated
    public IRubyObject ljust19(IRubyObject arg0, IRubyObject arg1) { return ljust(arg0, arg1); }

    @JRubyMethod(name = "ljust")
    public IRubyObject ljust(IRubyObject arg0) {
        return justify(getRuntime(), arg0, 'l');
    }

    @JRubyMethod(name = "ljust")
    public IRubyObject ljust(IRubyObject arg0, IRubyObject arg1) {
        return justify(arg0, arg1, 'l');
    }

    /** rb_str_rjust
     *
     */

    @Deprecated
    public IRubyObject rjust19(IRubyObject arg0) { return rjust(arg0); }

    @Deprecated
    public IRubyObject rjust19(IRubyObject arg0, IRubyObject arg1) { return rjust(arg0, arg1); }

    @JRubyMethod(name = "rjust")
    public IRubyObject rjust(IRubyObject arg0) {
        return justify(getRuntime(), arg0, 'r');
    }

    @JRubyMethod(name = "rjust")
    public IRubyObject rjust(IRubyObject arg0, IRubyObject arg1) {
        return justify(arg0, arg1, 'r');
    }

    /** rb_str_center
     *
     */

    @Deprecated
    public IRubyObject center19(IRubyObject arg0) { return center(arg0); }

    @Deprecated
    public IRubyObject center19(IRubyObject arg0, IRubyObject arg1) { return center(arg0, arg1); }

    @JRubyMethod(name = "center")
    public IRubyObject center(IRubyObject arg0) {
        return justify(getRuntime(), arg0, 'c');
    }

    @JRubyMethod(name = "center")
    public IRubyObject center(IRubyObject arg0, IRubyObject arg1) {
        return justify(arg0, arg1, 'c');
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
            RubyRegexp regex = (RubyRegexp) arg;

            pos = regex.search(context, this, 0, false);
            if (pos < 0) return partitionMismatch(runtime);
            sep = (RubyString) subpat(context, regex);
            if (pos == 0 && sep.value.getRealSize() == 0) return partitionMismatch(runtime);
        } else {
            IRubyObject tmp = arg.checkStringType();
            if (tmp.isNil()) throw runtime.newTypeError("type mismatch: " + arg.getMetaClass().getName() + " given");
            sep = (RubyString)tmp;
            pos = StringSupport.index(this, sep, 0, this.checkEncoding(sep));
            if (pos < 0) return partitionMismatch(runtime);
        }

        return RubyArray.newArrayNoCopy(runtime, new IRubyObject[] {
                makeShared(runtime, 0, pos),
                sep,
                makeShared(runtime, pos + sep.value.getRealSize(), value.getRealSize() - pos - sep.value.getRealSize())
        });
    }

    private RubyArray partitionMismatch(Ruby runtime) {
        final Encoding enc = getEncoding();
        return RubyArray.newArrayMayCopy(runtime, this.strDup(runtime), newEmptyString(runtime, enc), newEmptyString(runtime, enc));
    }

    @JRubyMethod(name = "rpartition", writes = BACKREF)
    public IRubyObject rpartition(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;
        final int pos;
        final RubyString sep;
        if (arg instanceof RubyRegexp) {
            IRubyObject tmp = rindex(context, arg);
            if (tmp.isNil()) return rpartitionMismatch(runtime);
            pos = tmp.convertToInteger().getIntValue();
            sep = (RubyString)RubyRegexp.nth_match(0, context.getLocalMatchOrNil());
        } else {
            IRubyObject tmp = arg.checkStringType();
            if (tmp.isNil()) throw runtime.newTypeError("type mismatch: " + arg.getMetaClass().getName() + " given");
            sep = (RubyString)tmp;
            pos = StringSupport.rindex(value, StringSupport.strLengthFromRubyString(this, this.checkEncoding(sep)), StringSupport.strLengthFromRubyString(sep, this.checkEncoding(sep)), subLength(value.getRealSize()), sep, this.checkEncoding(sep));
            if (pos < 0) return rpartitionMismatch(runtime);
        }

        return RubyArray.newArrayNoCopy(runtime, new IRubyObject[] {
                substr19(runtime, 0, pos),
                sep,
                substr19(runtime, pos + sep.strLength(), value.getRealSize())
        });
    }

    private IRubyObject rpartitionMismatch(Ruby runtime) {
        final Encoding enc = getEncoding();
        return RubyArray.newArrayNoCopy(runtime, newEmptyString(runtime, enc), newEmptyString(runtime, enc), this.strDup(runtime));
    }

    /** rb_str_chop / rb_str_chop_bang
     *
     */

    @Deprecated
    public IRubyObject chop19(ThreadContext context) { return chop(context); }

    @Deprecated
    public IRubyObject chop_bang19(ThreadContext context) { return chop_bang(context); }

    @JRubyMethod(name = "chop")
    public IRubyObject chop(ThreadContext context) {
        Ruby runtime = context.runtime;
        if (value.getRealSize() == 0) return newEmptyString(runtime, metaClass, value.getEncoding()).infectBy(this);
        return makeShared(runtime, 0, StringSupport.choppedLength(this));
    }

    @JRubyMethod(name = "chop!")
    public IRubyObject chop_bang(ThreadContext context) {
        modifyAndKeepCodeRange();
        if (size() > 0) {
            int len = StringSupport.choppedLength(this);
            value.realSize(len);
            if (getCodeRange() != CR_7BIT) {
                clearCodeRange();
            }
            return this;
        }
        return context.nil;
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
        modifyCheck();
        Ruby runtime = context.runtime;
        if (value.getRealSize() == 0) return context.nil;

        IRubyObject rsObj = runtime.getGlobalVariables().get("$/");

        if (rsObj == runtime.getGlobalVariables().getDefaultSeparator()) return smartChopBangCommon(runtime);
        return chompBangCommon(runtime, rsObj);
    }

    @JRubyMethod(name = "chomp!")
    public IRubyObject chomp_bang19(ThreadContext context, IRubyObject arg0) {
        modifyCheck();
        if (value.getRealSize() == 0) return context.nil;
        return chompBangCommon(context.runtime, arg0);
    }

    private IRubyObject chompBangCommon(Ruby runtime, IRubyObject rsObj) {
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
        if (rslen == 1 && newline == (byte)'\n') return smartChopBangCommon(runtime);

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

    private IRubyObject smartChopBangCommon(Ruby runtime) {
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

    @Deprecated
    public IRubyObject lstrip19(ThreadContext context) { return lstrip(context); }

    @Deprecated
    public IRubyObject lstrip_bang19(ThreadContext context) { return lstrip_bang(context); }

    @JRubyMethod(name = "lstrip")
    public IRubyObject lstrip(ThreadContext context) {
        RubyString str = strDup(context.runtime);
        str.lstrip_bang(context);
        return str;
    }

    @JRubyMethod(name = "lstrip!")
    public IRubyObject lstrip_bang(ThreadContext context) {
        modifyCheck();
        final ByteList value = this.value;
        if (value.getRealSize() == 0) return context.nil;
        int s = value.getBegin();
        int end = s + value.getRealSize();
        byte[] bytes = value.getUnsafeBytes();

        Encoding enc = EncodingUtils.STR_ENC_GET(this);
        final IRubyObject result;
        if (singleByteOptimizable(enc)) {
            result = singleByteLStrip(context, bytes, s, end);
        } else {
            result = multiByteLStrip(context, enc, bytes, s, end);
        }
        keepCodeRange();
        return result;
    }

    private IRubyObject singleByteLStrip(ThreadContext context, byte[] bytes, int s, int end) {
        int p = s;
        while (p < end && ASCII.isSpace(bytes[p] & 0xff)) p++;
        if (p > s) {
            view(p - s, end - p);
            return this;
        }
        return context.nil;
    }

    private IRubyObject multiByteLStrip(ThreadContext context, Encoding enc, byte[]bytes, int s, int end) {
        final Ruby runtime = context.runtime;
        int p = s;

        while (p < end) {
            int c = codePoint(runtime, enc, bytes, p, end);
            if (!ASCII.isSpace(c)) break;
            p += codeLength(enc, c);
        }

        if (p > s) {
            view(p - s, end - p);
            return this;
        }

        return context.nil;
    }

    /** rb_str_rstrip / rb_str_rstrip_bang
     *
     */

    @Deprecated
    public IRubyObject rstrip19(ThreadContext context) { return rstrip(context); }

    @Deprecated
    public IRubyObject rstrip_bang19(ThreadContext context) { return rstrip_bang(context); }

    @JRubyMethod(name = "rstrip")
    public IRubyObject rstrip(ThreadContext context) {
        RubyString str = strDup(context.runtime);
        str.rstrip_bang(context);
        return str;
    }

    @JRubyMethod(name = "rstrip!")
    public IRubyObject rstrip_bang(ThreadContext context) {
        modifyCheck();
        if (value.getRealSize() == 0) {
            return context.nil;
        }

        checkDummyEncoding();
        Encoding enc = EncodingUtils.STR_ENC_GET(this);
        IRubyObject result = singleByteOptimizable(enc) ? singleByteRStrip(context) : multiByteRStrip(context);

        keepCodeRange();
        return result;
    }

    // In 1.9 we strip any combination of \0 and \s
    private IRubyObject singleByteRStrip(ThreadContext context) {
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

        return context.nil;
    }

    // In 1.9 we strip any combination of \0 and \s
    private IRubyObject multiByteRStrip(ThreadContext context) {
        final Ruby runtime = context.runtime;
        byte[] bytes = value.getUnsafeBytes();
        int start = value.getBegin();
        int end = start + value.getRealSize();
        Encoding enc = EncodingUtils.STR_ENC_GET(this);
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
        return context.nil;
    }

    /** rb_str_strip / rb_str_strip_bang
     *
     */

    @Deprecated
    public IRubyObject strip19(ThreadContext context) { return strip(context); }

    @Deprecated
    public IRubyObject strip_bang19(ThreadContext context) { return strip_bang(context); }

    @JRubyMethod(name = "strip")
    public IRubyObject strip(ThreadContext context) {
        RubyString str = strDup(context.runtime);
        str.strip_bang(context);
        return str;
    }

    @JRubyMethod(name = "strip!")
    public IRubyObject strip_bang(ThreadContext context) {
        modifyCheck();

        IRubyObject left = lstrip_bang19(context);
        IRubyObject right = rstrip_bang19(context);

        return left == context.nil && right == context.nil ? context.nil : this;
    }

    @Deprecated
    public IRubyObject count19(ThreadContext context) { return count(context); }

    @Deprecated
    public IRubyObject count19(ThreadContext context, IRubyObject arg) { return count(context, arg); }

    @Deprecated
    public IRubyObject count19(ThreadContext context, IRubyObject[] args) { return count(context, args); }

    @JRubyMethod(name = "count")
    public IRubyObject count(ThreadContext context) {
        throw context.runtime.newArgumentError("wrong number of arguments");
    }

    // MRI: rb_str_count, first half
    @JRubyMethod(name = "count")
    public IRubyObject count(ThreadContext context, IRubyObject arg) {
        final Ruby runtime = context.runtime;

        final RubyString countStr = arg.convertToString();
        final ByteList countValue = countStr.getByteList();
        final Encoding enc = checkEncoding(countStr);

        if ( countValue.length() == 1 && enc.isAsciiCompatible() ) {
            final byte[] countBytes = countValue.unsafeBytes();
            final int begin = countValue.begin(), size = countValue.length();
            if ( enc.isReverseMatchAllowed(countBytes, begin, begin + size) && ! isCodeRangeBroken() ) {
                if ( value.length() == 0 ) return RubyFixnum.zero(runtime);

                int n = 0;
                int[] len_p = {0};
                int c = EncodingUtils.encCodepointLength(runtime, countBytes, begin, begin + size, len_p, enc);

                final byte[] bytes = value.unsafeBytes();
                int i = value.begin();
                final int end = i + value.length();
                while ( i < end ) {
                    if ( ( bytes[i++] & 0xff ) == c ) n++;
                }
                return RubyFixnum.newFixnum(runtime, n);
            }
        }

        final boolean[] table = new boolean[StringSupport.TRANS_SIZE + 1];
        StringSupport.TrTables tables = StringSupport.trSetupTable(countValue, runtime, table, null, true, enc);
        return runtime.newFixnum(StringSupport.strCount(value, runtime, table, tables, enc));
    }

    // MRI: rb_str_count for arity > 1, first half
    @JRubyMethod(name = "count", required = 1, rest = true)
    public IRubyObject count(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.runtime;

        if ( value.length() == 0 ) return RubyFixnum.zero(runtime);

        RubyString countStr = args[0].convertToString();
        Encoding enc = checkEncoding(countStr);

        final boolean[] table = new boolean[StringSupport.TRANS_SIZE + 1];
        StringSupport.TrTables tables = StringSupport.trSetupTable(countStr.value, runtime, table, null, true, enc);
        for ( int i = 1; i < args.length; i++ ) {
            countStr = args[i].convertToString();
            enc = checkEncoding(countStr);
            tables = StringSupport.trSetupTable(countStr.value, runtime, table, tables, false, enc);
        }

        return runtime.newFixnum(StringSupport.strCount(value, runtime, table, tables, enc));
    }

    /** rb_str_delete / rb_str_delete_bang
     *
     */

    @JRubyMethod(name = "delete")
    public IRubyObject delete(ThreadContext context) {
        throw context.runtime.newArgumentError("wrong number of arguments");
    }

    @JRubyMethod(name = "delete")
    public IRubyObject delete(ThreadContext context, IRubyObject arg) {
        RubyString str = strDup(context.runtime);
        str.delete_bang(context, arg);
        return str;
    }

    @JRubyMethod(name = "delete", required = 1, rest = true)
    public IRubyObject delete(ThreadContext context, IRubyObject[] args) {
        RubyString str = strDup(context.runtime);
        str.delete_bang(context, args);
        return str;
    }

    @JRubyMethod(name = "delete!")
    public IRubyObject delete_bang(ThreadContext context) {
        throw context.runtime.newArgumentError("wrong number of arguments");
    }

    @JRubyMethod(name = "delete!")
    public IRubyObject delete_bang(ThreadContext context, IRubyObject arg) {
        if (value.getRealSize() == 0) return context.nil;

        final Ruby runtime = context.runtime;
        RubyString otherStr = arg.convertToString();
        Encoding enc = checkEncoding(otherStr);
        final boolean[] squeeze = new boolean[StringSupport.TRANS_SIZE + 1];
        StringSupport.TrTables tables = StringSupport.trSetupTable(otherStr.value, runtime, squeeze, null, true, enc);

        if (StringSupport.strDeleteBang(this, runtime, squeeze, tables, enc) == null) {
            return context.nil;
        }

        return this;
    }

    @JRubyMethod(name = "delete!", required = 1, rest = true)
    public IRubyObject delete_bang(ThreadContext context, IRubyObject[] args) {
        if (value.getRealSize() == 0) return context.nil;

        final Ruby runtime = context.runtime;
        RubyString otherStr;
        Encoding enc = null;
        boolean[] squeeze = new boolean[StringSupport.TRANS_SIZE + 1];
        StringSupport.TrTables tables = null;

        for (int i=0; i<args.length; i++) {
            otherStr = args[i].convertToString();
            enc = checkEncoding(otherStr);
            tables = StringSupport.trSetupTable(otherStr.value, runtime, squeeze, tables, i == 0, enc);
        }

        if (StringSupport.strDeleteBang(this, runtime, squeeze, tables, enc) == null) {
            return context.nil;
        }

        return this;
    }

    @Deprecated
    public IRubyObject delete19(ThreadContext context) {
        return delete(context);
    }

    @Deprecated
    public IRubyObject delete19(ThreadContext context, IRubyObject arg) {
        return delete(context, arg);
    }

    @Deprecated
    public IRubyObject delete19(ThreadContext context, IRubyObject[] args) {
        return delete(context, args);
    }

    @Deprecated
    public IRubyObject delete_bang19(ThreadContext context) {
        return delete_bang(context);
    }

    @Deprecated
    public IRubyObject delete_bang19(ThreadContext context, IRubyObject arg) {
        return delete_bang(context, arg);
    }

    @Deprecated
    public IRubyObject delete_bang19(ThreadContext context, IRubyObject[] args) {
        return delete_bang(context, args);
    }

    /** rb_str_squeeze / rb_str_squeeze_bang
     *
     */

    @JRubyMethod(name = "squeeze")
    public IRubyObject squeeze(ThreadContext context) {
        RubyString str = strDup(context.runtime);
        str.squeeze_bang(context);
        return str;
    }

    @JRubyMethod(name = "squeeze")
    public IRubyObject squeeze(ThreadContext context, IRubyObject arg) {
        RubyString str = strDup(context.runtime);
        str.squeeze_bang(context, arg);
        return str;
    }

    @JRubyMethod(name = "squeeze", required = 1, rest = true)
    public IRubyObject squeeze(ThreadContext context, IRubyObject[] args) {
        RubyString str = strDup(context.runtime);
        str.squeeze_bang(context, args);
        return str;
    }

    @JRubyMethod(name = "squeeze!")
    public IRubyObject squeeze_bang(ThreadContext context) {
        if (value.getRealSize() == 0) {
            modifyCheck();
            return context.nil;
        }
        final Ruby runtime = context.runtime;

        final boolean squeeze[] = new boolean[StringSupport.TRANS_SIZE];
        for (int i=0; i< StringSupport.TRANS_SIZE; i++) squeeze[i] = true;

        modifyAndKeepCodeRange();
        if (singleByteOptimizable()) {
            if (! StringSupport.singleByteSqueeze(value, squeeze)) {
                return context.nil;
            }
        } else {
            if (! StringSupport.multiByteSqueeze(runtime, value, squeeze, null, value.getEncoding(), false)) {
                return context.nil;
            }
        }

        return this;
    }

    @JRubyMethod(name = "squeeze!")
    public IRubyObject squeeze_bang(ThreadContext context, IRubyObject arg) {
        final Ruby runtime = context.runtime;

        RubyString otherStr = arg.convertToString();
        final boolean squeeze[] = new boolean[StringSupport.TRANS_SIZE + 1];
        StringSupport.TrTables tables = StringSupport.trSetupTable(otherStr.value, runtime, squeeze, null, true, checkEncoding(otherStr));

        modifyAndKeepCodeRange();
        if (singleByteOptimizable() && otherStr.singleByteOptimizable()) {
            if (! StringSupport.singleByteSqueeze(value, squeeze)) {
                return context.nil;
            }
        } else {
            if (! StringSupport.multiByteSqueeze(runtime, value, squeeze, tables, value.getEncoding(), true)) {
                return context.nil;
            }
        }

        return this;
    }

    @JRubyMethod(name = "squeeze!", required = 1, rest = true)
    public IRubyObject squeeze_bang(ThreadContext context, IRubyObject[] args) {
        if (value.getRealSize() == 0) {
            modifyCheck();
            return context.nil;
        }
        final Ruby runtime = context.runtime;

        RubyString otherStr = args[0].convertToString();
        Encoding enc = checkEncoding(otherStr);
        final boolean squeeze[] = new boolean[StringSupport.TRANS_SIZE + 1];
        StringSupport.TrTables tables = StringSupport.trSetupTable(otherStr.value, runtime, squeeze, null, true, enc);

        boolean singleByte = singleByteOptimizable() && otherStr.singleByteOptimizable();
        for (int i=1; i<args.length; i++) {
            otherStr = args[i].convertToString();
            enc = checkEncoding(otherStr);
            singleByte = singleByte && otherStr.singleByteOptimizable();
            tables = StringSupport.trSetupTable(otherStr.value, runtime, squeeze, tables, false, enc);
        }

        modifyAndKeepCodeRange();
        if (singleByte) {
            if (! StringSupport.singleByteSqueeze(value, squeeze)) {
                return context.nil;
            }
        } else {
            if (! StringSupport.multiByteSqueeze(runtime, value, squeeze, tables, enc, true)) {
                return context.nil;
            }
        }

        return this;
    }

    @Deprecated
    public IRubyObject squeeze19(ThreadContext context) {
        return squeeze(context);
    }

    @Deprecated
    public IRubyObject squeeze19(ThreadContext context, IRubyObject arg) {
        return squeeze(context, arg);
    }

    @Deprecated
    public IRubyObject squeeze19(ThreadContext context, IRubyObject[] args) {
        return squeeze(context, args);
    }

    @Deprecated
    public IRubyObject squeeze_bang19(ThreadContext context) {
        return squeeze_bang(context);
    }

    @Deprecated
    public IRubyObject squeeze_bang19(ThreadContext context, IRubyObject arg) {
        return squeeze_bang(context, arg);
    }

    @Deprecated
    public IRubyObject squeeze_bang19(ThreadContext context, IRubyObject[] args) {
        return squeeze_bang(context, args);
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
        RubyString replStr = repl.convertToString();
        ByteList replList = replStr.value;
        RubyString srcStr = src.convertToString();

        if (value.getRealSize() == 0) return context.nil;
        if (replList.getRealSize() == 0) return delete_bang(context, src);

        CodeRangeable ret = StringSupport.trTransHelper(context.runtime, this, srcStr, replStr, sflag);
        return (ret == null) ? context.nil : (IRubyObject) ret;
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
    @JRubyMethod(name = "each_line")
    public IRubyObject each_line(ThreadContext context, Block block) {
        return StringSupport.rbStrEnumerateLines(this, context, "each_line", context.runtime.getGlobalVariables().get("$/"), block, false);
    }

    @JRubyMethod(name = "each_line")
    public IRubyObject each_line(ThreadContext context, IRubyObject arg, Block block) {
        return StringSupport.rbStrEnumerateLines(this, context, "each_line", arg, block, false);
    }

    @JRubyMethod(name = "each_line")
    public IRubyObject each_line(ThreadContext context, IRubyObject arg, IRubyObject opts, Block block) {
        return StringSupport.rbStrEnumerateLines(this, context, "each_line", arg, opts, block, false);
    }

    @Deprecated // no longer used
    public IRubyObject each_lineCommon(ThreadContext context, IRubyObject sep, Block block) {
        if (sep == context.nil) {
            block.yield(context, this);
            return this;
        }

        final Ruby runtime = context.runtime;
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

    @Deprecated
    public IRubyObject each_line19(ThreadContext context, Block block) {
        return each_line(context, block);
    }

    @Deprecated
    public IRubyObject each_line19(ThreadContext context, IRubyObject arg, Block block) {
        return each_line(context, arg, block);
    }

    @JRubyMethod(name = "lines")
    public IRubyObject lines(ThreadContext context, Block block) {
        return StringSupport.rbStrEnumerateLines(this, context, "lines", context.runtime.getGlobalVariables().get("$/"), block, true);
    }

    @JRubyMethod(name = "lines")
    public IRubyObject lines(ThreadContext context, IRubyObject arg, Block block) {
        return StringSupport.rbStrEnumerateLines(this, context, "lines", arg, block, true);
    }

    /**
     * rb_str_each_byte
     */
    @JRubyMethod(name = "each_byte")
    public IRubyObject each_byte(ThreadContext context, Block block) {
        return enumerateBytes(context, "each_byte", block, false);
    }

    @Deprecated
    public IRubyObject each_byte19(ThreadContext context, Block block) {
        return each_byte(context, block);
    }

    @JRubyMethod
    public IRubyObject bytes(ThreadContext context, Block block) {
        return enumerateBytes(context, "bytes", block, true);
    }

    @JRubyMethod(name = "each_char")
    public IRubyObject each_char(ThreadContext context, Block block) {
        return enumerateChars(context, "each_char", block, false);
    }

    @JRubyMethod(name = "chars")
    public IRubyObject chars(ThreadContext context, Block block) {
        return enumerateChars(context, "chars", block, true);
    }

    @Deprecated
    public IRubyObject each_char19(ThreadContext context, Block block) {
        return each_char(context, block);
    }

    @Deprecated
    public IRubyObject chars19(ThreadContext context, Block block) {
        return chars(context, block);
    }

    /**
     * A character size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject eachCharSize(ThreadContext context, RubyString recv, IRubyObject[] args) {
        return recv.rubyLength(context.runtime);
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
        int len, n;
        byte[] ptrBytes;
        int ptr;
        Encoding enc;

        if (block.isGiven()) {
            if (wantarray) {
                runtime.getWarnings().warning("passing a block to String#" + name + " is deprecated");
                wantarray = false;
            }
        }
        else if (!wantarray) {
            return enumeratorizeWithSize(context, str, name, RubyString::eachCharSize);
        }

        str = str.newFrozen();
        ByteList strByteList = str.value;
        ptrBytes = strByteList.unsafeBytes();
        ptr = strByteList.begin();
        len = strByteList.getRealSize();
        enc = str.getEncoding();

        IRubyObject[] ary = wantarray ? new IRubyObject[str.strLength()] : null; int a = 0;

        switch (getCodeRange()) {
            case CR_VALID:
            case CR_7BIT:
                for (int i = 0; i < len; i += n) {
                    n = StringSupport.encFastMBCLen(ptrBytes, ptr + i, ptr + len, enc);
                    IRubyObject substr = str.substr(runtime, i, n);
                    substr.infectBy(str);
                    if (wantarray) ary[a++] = substr;
                    else block.yield(context, substr);
                }
                break;
            default:
                for (int i = 0; i < len; i += n) {
                    n = StringSupport.length(enc, ptrBytes, ptr + i, ptr + len);
                    IRubyObject substr = str.substr(runtime, i, n);
                    substr.infectBy(str);
                    if (wantarray) ary[a++] = substr;
                    else block.yield(context, substr);
                }
        }

        assert !wantarray || a == ary.length;

        return wantarray ? RubyArray.newArrayNoCopy(runtime, ary) : this;
    }

    // MRI: rb_str_enumerate_codepoints
    private IRubyObject enumerateCodepoints(ThreadContext context, String name, Block block, boolean wantarray) {
        Ruby runtime = context.runtime;
        RubyString str = this;
        byte[] ptrBytes;
        int ptr, end;
        Encoding enc;

        if (singleByteOptimizable()) return enumerateBytes(context, name, block, wantarray);

        if (block.isGiven()) {
            if (wantarray) {
                runtime.getWarnings().warning("passing a block to String#" + name + " is deprecated");
                wantarray = false;
            }
        }
        else if (!wantarray) {
            return enumeratorizeWithSize(context, str, name, RubyString::codepointSize);
        }

        if (!str.isFrozen()) str.setByteListShared();
        ByteList strByteList = str.value;
        ptrBytes = strByteList.unsafeBytes();
        ptr = strByteList.begin();
        end = ptr + strByteList.getRealSize();
        enc = EncodingUtils.getEncoding(strByteList);

        RubyArray ary = wantarray ? RubyArray.newArray(runtime, str.strLength(strByteList, enc)) : null;

        while (ptr < end) {
            int c = codePoint(runtime, enc, ptrBytes, ptr, end);
            int n = codeLength(enc, c);
            if (wantarray) ary.append(RubyFixnum.newFixnum(runtime, c));
            else block.yield(context, RubyFixnum.newFixnum(runtime, c));
            ptr += n;
        }

        return wantarray ? ary : this;
    }

    private IRubyObject enumerateBytes(ThreadContext context, String name, Block block, boolean wantarray) {
        Ruby runtime = context.runtime;

        if (block.isGiven()) {
            if (wantarray) {
                runtime.getWarnings().warning("passing a block to String#" + name + " is deprecated");
                wantarray = false;
            }
        }
        else if (!wantarray) {
            return enumeratorizeWithSize(context, this, name, RubyString::byteSize);
        }

        IRubyObject[] ary = wantarray ? new IRubyObject[value.getRealSize()] : null;
        // Check the length every iteration, since the block can modify this string.
        for (int i=0; i < value.getRealSize(); i++) {
            RubyFixnum bite = RubyFixnum.newFixnum(runtime, value.get(i) & 0xFF);
            if (wantarray) ary[i] = bite;
            else block.yield(context, bite);
        }

        return wantarray ? RubyArray.newArrayNoCopy(runtime, ary) : this;
    }

    /**
     * A codepoint size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject codepointSize(ThreadContext context, RubyString recv, IRubyObject[] args) {
        return recv.rubyLength(context.runtime);
    }

    private static final ByteList GRAPHEME_CLUSTER_PATTERN = new ByteList(new byte[] {(byte)'\\', (byte)'X'}, false);

    /**
     * A grapheme cluster size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject eachGraphemeClusterSize(ThreadContext context, RubyString self, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        ByteList value = self.getByteList();
        Encoding enc = value.getEncoding();
        if (!enc.isUnicode()) return self.rubyLength(runtime);

        Regex reg = RubyRegexp.getRegexpFromCache(runtime, GRAPHEME_CLUSTER_PATTERN, enc, RegexpOptions.NULL_OPTIONS);
        int beg = value.getBegin();
        int end = beg + value.getRealSize();
        Matcher matcher = reg.matcher(value.getUnsafeBytes(), beg, end);
        int count = 0;

        while (beg < end) {
            int len = matcher.match(beg, end, Option.DEFAULT);
            if (len <= 0) break;
            count++;
            beg += len;
        }
        return RubyFixnum.newFixnum(runtime, count);
    }

    private IRubyObject enumerateGraphemeClusters(ThreadContext context, String name, Block block, boolean wantarray) {
        Ruby runtime = context.runtime;
        RubyString str = this;
        Encoding enc = str.getEncoding();
        if (!enc.isUnicode()) {
            return enumerateChars(context, name, block, wantarray);
        }

        if (block.isGiven()) {
            if (wantarray) {
                runtime.getWarnings().warning("passing a block to String#" + name + " is deprecated");
                wantarray = false;
            }
        }
        else if (!wantarray) {
            return enumeratorizeWithSize(context, str, name, RubyString::eachGraphemeClusterSize);
        }

        Regex reg = RubyRegexp.getRegexpFromCache(runtime, GRAPHEME_CLUSTER_PATTERN, enc, RegexpOptions.NULL_OPTIONS);

        if (!wantarray) str = str.newFrozen();
        ByteList strByteList = str.value;
        byte[] ptrBytes = strByteList.unsafeBytes();
        int ptr = strByteList.begin();
        int end = ptr + strByteList.getRealSize();
        Matcher matcher = reg.matcher(ptrBytes, ptr, end);

        RubyArray ary = wantarray ? RubyArray.newArray(runtime, end - ptr) : null;

        while (ptr < end) {
            int len = matcher.match(ptr, end, Option.DEFAULT);
            if (len <= 0) break;
            RubyString result = newStringShared(runtime, ptrBytes, ptr, len, enc);
            result.infectBy(str);
            if (wantarray) ary.append(result);
            else block.yield(context, result);
            ptr += len;
        }

        return wantarray ? ary : this;
    }

    @JRubyMethod
    public IRubyObject grapheme_clusters(ThreadContext context, Block block) {
        return enumerateGraphemeClusters(context, "grapheme_clusters", block, true);
    }

    @JRubyMethod
    public IRubyObject each_grapheme_cluster(ThreadContext context, Block block) {
        return enumerateGraphemeClusters(context, "each_grapheme_cluster", block, false);
    }

    /** rb_str_intern
     *
     */
    @JRubyMethod(name = {"to_sym", "intern"})
    public RubySymbol intern() {
        final Ruby runtime = getRuntime();

        if (scanForCodeRange() == CR_BROKEN) {
            throw runtime.newEncodingError("invalid symbol in encoding " + getEncoding() + " :" + inspect());
        }

        RubySymbol symbol = runtime.getSymbolTable().getSymbol(value);
        if (symbol.getBytes() == value) shareLevel = SHARE_LEVEL_BYTELIST;
        return symbol;
    }

    @Deprecated
    public RubySymbol intern19() {
        return intern();
    }

    @JRubyMethod
    public IRubyObject ord(ThreadContext context) {
        final Ruby runtime = context.runtime;
        return RubyFixnum.newFixnum(runtime, codePoint(runtime, this.value));
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

        byte[] bytes = value.getUnsafeBytes();
        int p = value.getBegin();
        int len = value.getRealSize();
        int end = p + len;

        if (bits >= 8 * 8) { // long size * bits in byte
            IRubyObject one = RubyFixnum.one(runtime);
            IRubyObject sum = RubyFixnum.zero(runtime);
            StringSites sites = sites(context);
            CallSite op_plus = sites.op_plus;
            while (p < end) {
                modifyCheck(bytes, len);
                sum = op_plus.call(context, sum, sum, RubyFixnum.newFixnum(runtime, bytes[p++] & 0xff));
            }
            if (bits != 0) {
                IRubyObject mod = sites.op_lshift.call(context, one, one, RubyFixnum.newFixnum(runtime, bits));
                sum = sites.op_and.call(context, sum, sum, sites.op_minus.call(context, mod, mod, one));
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

        RubyRegexp underscore_pattern = RubyRegexp.newDummyRegexp(runtime, Numeric.ComplexPatterns.underscores_pat);
        RubyString s = gsubFast(context, underscore_pattern, runtime.newString(UNDERSCORE), Block.NULL_BLOCK);

        IRubyObject[] ary = RubyComplex.str_to_c_internal(context, s);

        IRubyObject first = ary[0];
        if ( first != context.nil ) return first;

        return RubyComplex.newComplexCanonicalize(context, RubyFixnum.zero(runtime));
    }

    private static final ByteList UNDERSCORE = new ByteList(new byte[] { '_' }, false);

    /** string_to_r
     *
     */
    @JRubyMethod
    public IRubyObject to_r(ThreadContext context) {
        Ruby runtime = context.runtime;

        RubyRegexp underscore_pattern = RubyRegexp.newDummyRegexp(runtime, Numeric.ComplexPatterns.underscores_pat);
        RubyString s = gsubFast(context, underscore_pattern, runtime.newString(UNDERSCORE), Block.NULL_BLOCK);

        IRubyObject[] ary = RubyRational.str_to_r_internal(context, s);

        IRubyObject first = ary[0];
        if ( first != context.nil ) return first;

        return RubyRational.newRationalNoReduce(context, RubyFixnum.zero(runtime), RubyFixnum.one(runtime));
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
    public RubyArray unpack(ThreadContext context, IRubyObject obj, Block block) {
        return Pack.unpackWithBlock(context, this, stringValue(obj).value, block);
    }

    @JRubyMethod
    public IRubyObject unpack1(ThreadContext context, IRubyObject obj, Block block) {
        return Pack.unpack1WithBlock(context, this, stringValue(obj).value, block);
    }

    @Deprecated // not used
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
            setEncoding(encindex);
            return this;
        }
        replace(newstr_p[0]);
        setEncoding(encindex);
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
        return force_encoding(EncodingUtils.rbToEncoding(context, enc));
    }

    private IRubyObject force_encoding(Encoding encoding) {
        modifyCheck();
        modify19();
        associateEncoding(encoding);
        clearCodeRange();
        return this;
    }

    @JRubyMethod(name = "valid_encoding?")
    public IRubyObject valid_encoding_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context, scanForCodeRange() != CR_BROKEN);
    }

    @JRubyMethod(name = "ascii_only?")
    public IRubyObject ascii_only_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context, scanForCodeRange() == CR_7BIT);
    }

    @JRubyMethod
    public IRubyObject b(ThreadContext context) {
        Encoding encoding = ASCIIEncoding.INSTANCE;
        RubyString dup = strDup(context.runtime);
        dup.clearCodeRange();
        dup.setEncoding(encoding);
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
        view(ByteList.plain(value), false);
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
        if (encoding == UTF8) {
            return RubyEncoding.doEncodeUTF8(value);
        }

        Charset charset = EncodingUtils.charsetForEncoding(encoding);

        // if null charset, let our transcoder handle it
        if (charset == null) {
            return EncodingUtils.transcodeString(value.toString(), encoding, 0);
        }

        if (charset == RubyEncoding.UTF16) {
            byte[] bytes = RubyEncoding.encodeUTF16(value);
            return new ByteList(bytes, encoding, false);
        }

        return RubyEncoding.doEncode(value, charset, encoding);
    }

    static ByteList encodeBytelist(String value, Encoding encoding) {
        if (encoding == UTF8) {
            return RubyEncoding.doEncodeUTF8(value);
        }

        Charset charset = EncodingUtils.charsetForEncoding(encoding);

        // if null charset, let our transcoder handle it
        if (charset == null) {
            return EncodingUtils.transcodeString(value, encoding, 0);
        }

        if (charset == RubyEncoding.UTF16) {
            byte[] bytes = RubyEncoding.encodeUTF16(value);
            return new ByteList(bytes, encoding, false);
        }

        return RubyEncoding.doEncode(value, charset, encoding);
    }

    @Override
    public <T> T toJava(Class<T> target) {
        // converting on Comparable.class due target.isAssignableFrom(String.class) compatibility (< 9.2)
        if (target == String.class || target == Comparable.class || target == Object.class) {
            return target.cast(decodeString());
        }
        if (target == CharSequence.class) { // explicitly here
            return (T) this; // used to convert to java.lang.String (< 9.2)
        }
        if (target == ByteList.class) {
            return target.cast(value);
        }
        if (target == Character.class || target == Character.TYPE) {
            // like ord we will only take the start off the string (not failing if str-length > 1)
            return (T) Character.valueOf((char) codePoint(getRuntime(), value));
        }
        return super.toJava(target);
    }

    /**
     * Scrub the contents of this string, replacing invalid characters as appropriate.
     *
     * MRI: rb_str_scrub
     */
    public IRubyObject strScrub(ThreadContext context, IRubyObject repl, Block block) {
        Encoding enc = EncodingUtils.STR_ENC_GET(this);
        return encStrScrub(context, enc, repl, getCodeRange(), block);
    }

    // MRI: rb_enc_str_scrub
    public IRubyObject encStrScrub(ThreadContext context, Encoding enc, IRubyObject repl, Block block) {
        int cr = CR_UNKNOWN;
        if (enc == EncodingUtils.STR_ENC_GET(this)) {
            cr = getCodeRange();
        }
        return encStrScrub(context, enc, repl, cr, block);
    }

    // MRI: enc_str_scrub
    public IRubyObject encStrScrub(ThreadContext context, Encoding enc, IRubyObject repl, int cr, Block block) {
        Ruby runtime = context.runtime;
        Encoding encidx;
        IRubyObject buf = context.nil;
        byte[] repBytes;
        int rep;
        int replen;
        boolean tainted = false;

        if (block.isGiven()) {
            if (repl != context.nil) {
                throw runtime.newArgumentError("both of block and replacement given");
            }
        }

        if (cr == CR_7BIT || cr == CR_VALID) return context.nil;

        if (repl != context.nil) {
            repl = EncodingUtils.strCompatAndValid(context, repl, enc);
            tainted |= repl.isTaint();
        }

        if (enc.isDummy()) return context.nil;

        encidx = enc;

        if (enc.isAsciiCompatible()) {
            byte[] pBytes = value.unsafeBytes();
            int p = value.begin();
            int e = p + value.getRealSize();
            int p1 = p;
            boolean rep7bit_p;
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
                int ret = StringSupport.preciseLength(enc, pBytes, p, e);
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
                        tainted |= repl.isTaint();
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
                    tainted |= repl.isTaint();
                    ((RubyString)buf).cat((RubyString)repl);
                    if (((RubyString)repl).getCodeRange() == CR_VALID)
                        cr = CR_VALID;
                }
            }
        }
        else {
	        /* ASCII incompatible */
            byte[] pBytes = value.unsafeBytes();
            int p = value.begin();
            int e = p + value.getRealSize();
            int p1 = p;
            int mbminlen = enc.minLength();
            if (block.isGiven()) {
                repBytes = null;
                rep = 0;
                replen = 0;
            }
            else if (!repl.isNil()) {
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
                int ret = StringSupport.preciseLength(enc, pBytes, p, e);
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
                        repl = block.yieldSpecific(context, RubyString.newString(runtime, pBytes, p, clen, enc));
                        repl = EncodingUtils.strCompatAndValid(context, repl, enc);
                        tainted |= repl.isTaint();
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
                    tainted |= repl.isTaint();
                    ((RubyString)buf).cat((RubyString)repl);
                }
            }
            cr = CR_VALID;
        }

        buf.setTaint(tainted | isTaint());
        ((RubyString)buf).setEncodingAndCodeRange(enc, cr);
        return buf;
    }

    // MRI: rb_str_offset
    public int rbStrOffset(int pos) {
        return strOffset(pos, isSingleByteOptimizable(this, getEncoding()));
    }

    // MRI: str_offset
    private int strOffset(int nth, boolean singlebyte) {
        int p = value.begin();
        int size = value.realSize();
        int e = p + size;
        int pp = nth(value.getEncoding(), value.unsafeBytes(), p, e, nth, singlebyte);
        if (pp == -1) return size;
        return pp - p;
    }

    // MRI: rb_str_include_range_p
    public static IRubyObject includeRange(ThreadContext context, RubyString _beg, RubyString _end, IRubyObject _val, boolean exclusive) {
        if (_val.isNil()) return context.fals;
        _val = TypeConverter.checkStringType(context.runtime, _val);
        if (_val.isNil()) return context.fals;

        final RubyString val = _val.convertToString();
        final RubyString beg = _beg.newFrozen();
        final RubyString end = _end.newFrozen();

        if (EncodingUtils.encAsciicompat(beg.getEncoding()) &&
                EncodingUtils.encAsciicompat(end.getEncoding()) &&
                EncodingUtils.encAsciicompat(val.getEncoding())) {
            ByteList bByteList = beg.getByteList();
            ByteList eByteList = end.getByteList();
            ByteList vByteList = val.getByteList();

            if (beg.length() == 1 && end.length() == 1) {
                if (val.length() != 1) return context.fals;
                int b, e, v;
                if ((b = bByteList.get(0)) < 128 &&
                        (e = eByteList.get(0)) < 128 &&
                        (v = vByteList.get(0)) < 128) {
                    if (b <= v && v < e) return context.tru;
                    if (!exclusive && v == e) return context.tru;
                    return context.fals;
                }
            }
        }

        try {
            beg.uptoCommon(
                    context,
                    end,
                    exclusive,
                    CallBlock19.newCallClosure(
                            beg,
                            beg.getMetaClass(),
                            Signature.ONE_ARGUMENT,
                            new IncludeUpToCallback(val),
                            context), false);
        } catch (JumpException.SpecialJump e) {
            return context.tru;
        }
        return context.fals;
    }

    private static class IncludeUpToCallback implements BlockCallback {

        private final IRubyObject beg;

        IncludeUpToCallback(IRubyObject beg) {
            this.beg = beg;
        }

        @Override
        public IRubyObject call(final ThreadContext context, final IRubyObject[] args, final Block block) {
            return call(context, args[0]);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject arg0) {
            if (beg.op_equal(context, arg0).isTrue()) {
                throw JumpException.SPECIAL_JUMP; // return true
            }
            return arg0;
        }
    }

    /**
     * Is this a "bare" string, i.e. has no instance vars and class == String.
     */
    private boolean isBare(Ruby runtime) {
        return !hasVariables() && metaClass == runtime.getString();
    }

    private static StringSites sites(ThreadContext context) {
        return context.sites.String;
    }

    @Deprecated
    public final RubyString strDup() {
        return strDup(getRuntime(), getMetaClass().getRealClass());
    }

    @Deprecated
    public final void modify19(int length) {
        modifyExpand(length);
    }

    @Deprecated
    public RubyArray split19(ThreadContext context, IRubyObject arg0, boolean useBackref) {
        return splitCommon(context, arg0, useBackref, flags, flags);
    }

    @Deprecated
    public IRubyObject lines20(ThreadContext context, Block block) {
        return lines(context, block);
    }

    @Deprecated
    public IRubyObject lines20(ThreadContext context, IRubyObject arg, Block block) {
        return lines(context, arg, block);
    }

    @Deprecated
    public IRubyObject dump19() {
        return dump();
    }

    @Deprecated
    public IRubyObject insert19(ThreadContext context, IRubyObject indexArg, IRubyObject stringArg) {
        return insert(context, indexArg, stringArg);
    }

    @Deprecated
    public IRubyObject op_equal19(ThreadContext context, IRubyObject other) {
        return op_equal(context, other);
    }

    @Deprecated
    public IRubyObject op_aref19(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        return op_aref(context, arg1, arg2);
    }

    @Deprecated
    public IRubyObject op_aref19(ThreadContext context, IRubyObject arg) {
        return op_aref(context, arg);
    }

    @Deprecated
    public IRubyObject op_aset19(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return op_aset(context, arg0, arg1);
    }

    @Deprecated
    public IRubyObject op_aset19(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return op_aset(context, arg0, arg1, arg2);
    }

    @Deprecated
    public IRubyObject op_match19(ThreadContext context, IRubyObject other) {
        return op_match(context, other);
    }

    @Deprecated
    public IRubyObject scan19(ThreadContext context, IRubyObject arg, Block block) { return scan(context, arg, block); }

}
