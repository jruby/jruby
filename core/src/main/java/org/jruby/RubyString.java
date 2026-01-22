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
import org.jruby.api.Convert;
import org.jruby.api.Create;
import org.jruby.api.JRubyAPI;
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
import org.jruby.runtime.SimpleHash;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.runtime.encoding.MarshalEncoding;
import org.jruby.runtime.marshal.MarshalLoader;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeSupport;
import org.jruby.util.CodeRangeable;
import org.jruby.util.ConvertBytes;
import org.jruby.util.Numeric;
import org.jruby.util.Pack;
import org.jruby.util.PerlHash;
import org.jruby.util.RegexpOptions;
import org.jruby.util.SipHashInline;
import org.jruby.util.Sprintf;
import org.jruby.util.StringSupport;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.EncodingUtils;
import org.jruby.util.io.RubyInputStream;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Function;

import static org.jruby.RubyComparable.invcmp;
import static org.jruby.RubyEnumerator.SizeFn;
import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.anno.FrameField.BACKREF;
import static org.jruby.api.Access.encodingService;
import static org.jruby.api.Access.globalVariables;
import static org.jruby.api.Access.hashClass;
import static org.jruby.api.Access.rangeClass;
import static org.jruby.api.Access.regexpClass;
import static org.jruby.api.Access.stringClass;
import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Convert.checkInt;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Convert.toLong;
import static org.jruby.api.Create.allocArray;
import static org.jruby.api.Create.dupString;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newArrayNoCopy;
import static org.jruby.api.Create.newEmptyArray;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.indexError;
import static org.jruby.api.Error.rangeError;
import static org.jruby.api.Error.runtimeError;
import static org.jruby.api.Error.typeError;
import static org.jruby.api.Warn.warnDeprecated;
import static org.jruby.runtime.Helpers.memchr;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.RubyStringBuilder.types;
import static org.jruby.util.StringSupport.*;

/**
 * Implementation of Ruby String class
 *
 * Concurrency: no synchronization is required among readers, but
 * all users must synchronize externally with writers.
 *
 */
@JRubyClass(name="String", include={"Enumerable", "Comparable"})
public class RubyString extends RubyObject implements CharSequence, EncodingCapable, MarshalEncoding, CodeRangeable, Appendable, SimpleHash {
    static final ASCIIEncoding ASCII = ASCIIEncoding.INSTANCE;
    static final UTF8Encoding UTF8 = UTF8Encoding.INSTANCE;

    // string doesn't share any resources
    private static final byte SHARE_LEVEL_NONE = 0;
    // string has it's own ByteList, but it's pointing to a shared buffer (byte[])
    private static final byte SHARE_LEVEL_BUFFER = 1;
    // string doesn't have it's own ByteList (values)
    private static final byte SHARE_LEVEL_BYTELIST = 2;

    private static final byte[] SCRUB_REPL_UTF8 = new byte[]{(byte)0xEF, (byte)0xBF, (byte)0xBD};
    private static final byte[] SCRUB_REPL_ASCII = new byte[]{(byte)'?'};
    private static final byte[] SCRUB_REPL_UTF16BE = new byte[]{(byte)0xFF, (byte)0xFD};
    private static final byte[] SCRUB_REPL_UTF16LE = new byte[]{(byte)0xFD, (byte)0xFF};
    private static final byte[] SCRUB_REPL_UTF32BE = new byte[]{(byte)0x00, (byte)0x00, (byte)0xFF, (byte)0xFD};
    private static final byte[] SCRUB_REPL_UTF32LE = new byte[]{(byte)0xFD, (byte)0xFF, (byte)0x00, (byte)0x00};
    private static final byte[] FORCE_ENCODING_BYTES = ".force_encoding(\"".getBytes();

    // This mimicks STR_BUF_MIN_SIZE (although MRI currently uses 16) but as we use ByteList we have paths
    // which use private DEFAULT_SIZE in ByteList (which happens to be 4).  MRI now has a lot of logic
    // around capacity and optimizes for single byte strings as well as having a much more liberal default.
    // Follow-up work should reevaluate this value (Currently only explicit capacity to initialize is using
    // this value).
    private static final int STRING_MINIMUM_SIZE = 4;

    public static RubyString[] NULL_ARRAY = {};

    protected volatile byte shareLevel = SHARE_LEVEL_NONE;

    private ByteList value;

    protected byte flags;

    public static RubyClass createStringClass(ThreadContext context, RubyClass Object, RubyModule Comparable) {
        return defineClass(context, "String", Object, RubyString::newAllocatedString).
                reifiedClass(RubyString.class).
                kindOf(new RubyModule.JavaClassKindOf(RubyString.class)).
                classIndex(ClassIndex.STRING).
                include(context, Comparable).
                defineMethods(context, RubyString.class);
    }

    @Override
    public Encoding getEncoding() {
        return value.getEncoding();
    }

    @Override
    @SuppressWarnings("ReferenceEquality")
    public void setEncoding(Encoding encoding) {
        if (encoding != value.getEncoding()) {
            if (shareLevel == SHARE_LEVEL_BYTELIST) modify();
            else modifyCheck();
            value.setEncoding(encoding);
        }
    }

    @Override
    @SuppressWarnings("ReferenceEquality")
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

    @Deprecated(since = "10.0.0.0")
    public final Encoding toEncoding(Ruby runtime) {
        return encodingService(getCurrentContext()).findEncoding(this);
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
        return isSingleByteOptimizable(this, EncodingUtils.STR_ENC_GET(this));
    }

    final boolean singleByteOptimizable(Encoding enc) {
        return isSingleByteOptimizable(this, enc);
    }

    @SuppressWarnings("ReferenceEquality")
    final Encoding isCompatibleWith(EncodingCapable other) {
        if (other instanceof RubyString otherStr) return checkEncoding(otherStr);
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

    @Deprecated(since = "10.0.0.0")
    final Encoding checkEncoding(EncodingCapable other) {
        var context = ((RubyBasicObject) other).getCurrentContext();
        Encoding enc = isCompatibleWith(other);
        if (enc == null) throw context.runtime.newEncodingCompatibilityError("incompatible character encodings: " +
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

    // rb_enc_check but only supports strings
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
    @SuppressWarnings("ReferenceEquality")
    public final boolean eql(IRubyObject other) {
        RubyClass meta = this.metaClass;
        if (meta != meta.runtime.getString() || meta != other.getMetaClass()) return super.eql(other);
        return eqlAndComparable(other);
    }

    @Deprecated(since = "10.0.0.0")
    private boolean eql19(IRubyObject other) {
        return eqlAndComparable(other);
    }

    // rb_str_hash_cmp
    private boolean eqlAndComparable(IRubyObject other) {
        final RubyString otherString = (RubyString) other;
        return StringSupport.areComparable(this, otherString) && value.equal(otherString.value);
    }

    /**
     * Does this string contain \0 anywhere (per byte search).
     * @return true if it does
     */
    public boolean hasNul() {
        ByteList bytes = getByteList();
        return memchr(bytes.unsafeBytes(), bytes.begin(), '\0', bytes.realSize()) != -1;
    }

    // mri: rb_must_asciicompat
    public void verifyAsciiCompatible() {
        if (!getEncoding().isAsciiCompatible()) throw getRuntime().newEncodingCompatibilityError("ASCII incompatible encoding: " + getEncoding());
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

    private RubyString(Ruby runtime, RubyClass rubyClass, String value, Encoding enc, boolean objectspace) {
        super(runtime, rubyClass, objectspace);
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

    protected RubyString(Ruby runtime, RubyClass rubyClass, ByteList value, int cr, boolean objectspace) {
        this(runtime, rubyClass, value, objectspace);
        flags |= cr;
    }

    // Deprecated String construction routines

    @Deprecated(since = "1.1.6")
    public RubyString newString(CharSequence s) {
        return new RubyString(getRuntime(), getType(), s);
    }

    @Deprecated(since = "1.1.6")
    public RubyString newString(ByteList s) {
        return new RubyString(getRuntime(), getMetaClass(), s);
    }

    @Deprecated(since = "1.1.2")
    public static RubyString newString(Ruby runtime, RubyClass clazz, CharSequence str) {
        return new RubyString(runtime, clazz, str);
    }

    public static RubyString newStringLight(Ruby runtime, ByteList bytes) {
        return new RubyString(runtime, runtime.getString(), bytes, false);
    }

    public static RubyString newStringLight(Ruby runtime, int size) {
        checkNegativeSize(runtime, size);
        return new RubyString(runtime, runtime.getString(), new ByteList(size), false);
    }

    public static RubyString newStringLight(Ruby runtime, int size, Encoding encoding) {
        checkNegativeSize(runtime, size);
        return new RubyString(runtime, runtime.getString(), new ByteList(size), encoding, false);
    }

    private static void checkNegativeSize(Ruby runtime, int size) {
        if (size < 0) {
            throw runtime.newArgumentError("negative string size (or size too big)");
        }
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

    public static RubyString newStringNoCopy(Ruby runtime, byte[] bytes, int start, int length, Encoding encoding) {
        return new RubyString(runtime, runtime.getString(), new ByteList(bytes, start, length, encoding, false));
    }

    public static RubyString newString(Ruby runtime, ByteList bytes) {
        return new RubyString(runtime, runtime.getString(), bytes);
    }

    public static RubyString newString(Ruby runtime, ByteList bytes, int coderange) {
        return new RubyString(runtime, runtime.getString(), bytes, coderange);
    }

    public static RubyString newChilledString(Ruby runtime, ByteList bytes, int coderange, String file, int line) {
        if (runtime.getInstanceConfig().isDebuggingFrozenStringLiteral()) {
            return new DebugChilledString(runtime, runtime.getString(), bytes, coderange, file, line + 1);
        }

        return newStringShared(runtime, bytes, coderange).chill();
    }

    public static RubyString newString(Ruby runtime, ByteList bytes, Encoding encoding) {
        return new RubyString(runtime, runtime.getString(), bytes, encoding);
    }

    static RubyString newString(Ruby runtime, byte b) {
        return new RubyString(runtime, runtime.getString(), RubyInteger.singleCharByteList(b));
    }

    @SuppressWarnings("ReferenceEquality")
    public static RubyString newUnicodeString(Ruby runtime, String str) {
        return runtime.getDefaultInternalEncoding() == UTF16BEEncoding.INSTANCE ?
            newUTF16String(runtime, str) : newUTF8String(runtime, str);
    }

    public static RubyString newUTF8String(Ruby runtime, String str) {
        return new RubyString(runtime, runtime.getString(), RubyEncoding.doEncodeUTF8(str));
    }

    public static RubyString newUTF16String(Ruby runtime, String str) {
        return new RubyString(runtime, runtime.getString(), RubyEncoding.doEncodeUTF16(str));
    }

    @SuppressWarnings("ReferenceEquality")
    public static RubyString newUnicodeString(Ruby runtime, CharSequence str) {
        return runtime.getDefaultInternalEncoding() == UTF16BEEncoding.INSTANCE ?
            newUTF16String(runtime, str) : newUTF8String(runtime, str);
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

    @Deprecated(since = "9.3.0.0")
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

    @SuppressWarnings("ReferenceEquality")
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

    @Deprecated(since = "10.0.0.0")
    public static RubyString newStringShared(Ruby runtime, byte[] bytes) {
        return newStringShared(runtime, bytes, 0, bytes.length, ASCIIEncoding.INSTANCE);
    }

    @Deprecated(since = "10.0.0.0")
    public static RubyString newStringShared(Ruby runtime, byte[] bytes, Encoding encoding) {
        return newStringShared(runtime, bytes, 0, bytes.length, encoding);
    }

    @Deprecated(since = "10.0.0.0")
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

    public static RubyString newEmptyBinaryString(Ruby runtime) {
        return newAllocatedString(runtime, runtime.getString());
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
        return str;
    }

    // str_make_independent_expand
    public final RubyString makeIndependent(final int length) {
        RubyClass klass = metaClass;
        RubyString str = strDup(klass.runtime, klass);
        str.modify(length);
        str.setFrozen(true);
        return str;
    }

    // MRI: EXPORT_STR macro in process.c
    public RubyString export(ThreadContext context) {
        if (Platform.IS_WINDOWS) {
            return EncodingUtils.strConvEncOpts(context, this, null, UTF8Encoding.INSTANCE, 0, context.nil);
        }
        return this;
    }

    /**
     * Determine how much space exists after the begin offset in this string's buffer.
     *
     * @return the amount of capacity in this string's buffer after the begin offset
     */
    public int capacity() {
        return value.getUnsafeBytes().length - value.begin();
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
        EmptyByteListHolder[] emptyBytelists = EMPTY_BYTELISTS;
        if (index < emptyBytelists.length && (bytes = emptyBytelists[index]) != null) {
            return bytes;
        }
        return prepareEmptyByteList(enc);
    }

    private static EmptyByteListHolder prepareEmptyByteList(Encoding enc) {
        if (enc == null) enc = ASCIIEncoding.INSTANCE;
        int index = enc.getIndex();
        EmptyByteListHolder[] emptyBytelists = EMPTY_BYTELISTS;
        if (index >= emptyBytelists.length) {
            EMPTY_BYTELISTS = emptyBytelists = Arrays.copyOfRange(emptyBytelists, 0, index + 4);
        }
        return emptyBytelists[index] = new EmptyByteListHolder(enc);
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

    @JRubyMethod
    public IRubyObject dup(ThreadContext context) {
        return isBare(context) ? strDup(context.runtime) : super.dup(context);
    }

    // MRI: rb_str_dup
    public final RubyString strDup(Ruby runtime) {
        return strDup(runtime, metaClass.getRealClass());
    }

    public final RubyString strDup(Ruby runtime, RubyClass clazz) {
        shareLevel = SHARE_LEVEL_BYTELIST;
        RubyString dup = new RubyString(runtime, clazz, value);
        dup.shareLevel = SHARE_LEVEL_BYTELIST;
        dup.flags |= flags & CR_MASK;

        return dup;
    }

    public final RubyString dupAsChilled(Ruby runtime, RubyClass clazz, String file, int line) {
        if (runtime.getInstanceConfig().isDebuggingFrozenStringLiteral()) {
            shareLevel = SHARE_LEVEL_BYTELIST;
            RubyString dup = new DebugChilledString(runtime, clazz, value, getCodeRange(), file, line + 1 + 1);
            dup.flags |= flags & CR_MASK;

            return dup;
        }

        return strDup(runtime, clazz).chill();
    }

    public FString dupAsFString(Ruby runtime) {
        shareLevel = SHARE_LEVEL_BYTELIST;
        FString dup = new FString(runtime, value.dup(), getCodeRange());
        dup.shareLevel = SHARE_LEVEL_BYTELIST;
        dup.flags |= (flags & CR_MASK);
        dup.setFrozen(true);

        return dup;
    }

    /* MRI: rb_str_subseq */
    public final RubyString makeSharedString(Ruby runtime, int index, int len) {
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

        return shared;
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
            shared = RubyInteger.singleCharString(runtime, (byte) value.get(index), meta, enc);
        } else {
            if (shareLevel == SHARE_LEVEL_NONE) shareLevel = SHARE_LEVEL_BUFFER;
            shared = new RubyString(runtime, meta, value.makeShared(index, len));
            shared.shareLevel = SHARE_LEVEL_BUFFER;
        }
        shared.copyCodeRangeForSubstr(this, enc); // no need to assign encoding, same bytelist shared
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

    @SuppressWarnings("ReferenceEquality")
    private void modifyCheck(byte[] b, int len, Encoding enc) {
        if (value.getUnsafeBytes() != b || value.getRealSize() != len || value.getEncoding() != enc) throw getRuntime().newRuntimeError("string modified");
    }

    protected void frozenCheck() {
        if (isChilled()) {
            mutateChilledString();
        } else if (isFrozen()) {
            throw getRuntime().newFrozenError("String", this);
        }
    }

    @Override
    public void checkFrozen() {
        frozenCheck();
    }

    @Override
    public final void ensureInstanceVariablesSettable() {
        frozenCheck();
    }

    protected void mutateChilledString() {
        byte savedFlags = flags;
        flags &= ~(CHILLED_LITERAL|CHILLED_SYMBOL_TO_S);
        if ((savedFlags & CHILLED_LITERAL) != 0) {
            getRuntime().getWarnings().warnDeprecated("literal string will be frozen in the future");
        } else if ((savedFlags & CHILLED_SYMBOL_TO_S) != 0) {
            getRuntime().getWarnings().warnDeprecated("string returned by :" + value + ".to_s will be frozen in the future");
        }
    }

    protected boolean isChilled() {
        return (flags & (CHILLED_LITERAL | CHILLED_SYMBOL_TO_S)) != 0;
    }

    protected boolean isChilledLiteral() {
        return (flags & CHILLED_LITERAL) != 0;
    }

    @Override
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

    @Deprecated(since = "10.0.0.0")
    public final void modify19() {
        modifyAndClearCodeRange();
    }

    // MRI: rb_str_modify
    public final void modifyAndClearCodeRange() {
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
    @Override
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

    /**
     * Ensure the backing store belongs to this string and has enough space to add extraLength bytes.
     *
     * MRI: str_ensure_available_capa
     *
     * @param context the current thread context
     * @param extraLength the extra length needed
     */
    public void ensureAvailable(ThreadContext context, int extraLength) {
        int realSize = value.getRealSize();

        if (realSize > Integer.MAX_VALUE - extraLength) {
            throw argumentError(context, "string sizes too big");
        }

        modifyExpand(realSize + extraLength);
    }

    // io_set_read_length
    public void setReadLength(int length) {
        if (size() != length) {
            modify();
            value.setRealSize(length);
        }
    }

    /**
     * Create anew or deduplicate a RubyString based on the given Java String content.
     *
     * @param runtime the JRuby runtime
     * @param content the Java String content
     * @return a frozen, deduplicated RubyString hosting the given content
     */
    public static RubyString newFString(Ruby runtime, String content) {
        return runtime.freezeAndDedupString(new FString(runtime, content));
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

    public static RubyString newDebugFrozenString(Ruby runtime, RubyClass rubyClass, ByteList value, int cr, String file, int line) {
        return new DebugFrozenString(runtime, rubyClass, value, cr, file, line);
    }

    public static RubyString newDebugChilledString(Ruby runtime, RubyClass rubyClass, ByteList value, int cr, String file, int line) {
        return new DebugChilledString(runtime, rubyClass, value, cr, file, line);
    }

    static class DebugFrozenString extends RubyString {
        private final String file;
        private final int line;

        protected DebugFrozenString(Ruby runtime, RubyClass rubyClass, ByteList value, int cr, String file, int line) {
            super(runtime, rubyClass, value, cr, false);

            this.file = file;
            this.line = line;

            // set flag for code that does not use isFrozen
            setFrozen(true);
        }

        @Override
        public boolean isFrozen() {
            return true;
        }

        @Override
        public void setFrozen(boolean frozen) {
            // ignore, cannot be unfrozen
        }

        @Override
        protected void frozenCheck() {
            Ruby runtime = getRuntime();
            
            throw runtime.newRaiseException(runtime.getFrozenError(),
                    "can't modify frozen String, created at " + file + ":" + line);
        }
    }

    static class DebugChilledString extends RubyString {
        private final String file;
        private final int line;

        protected DebugChilledString(Ruby runtime, RubyClass rubyClass, ByteList value, int cr, String file, int line) {
            super(runtime, rubyClass, value, cr, false);

            this.file = file;
            this.line = line;

            // Always set as shared bytelist, since chilled strings reuse bytelists and will eventually be immutable
            this.shareLevel = SHARE_LEVEL_BYTELIST;

            chill();
        }

        protected void mutateChilledString() {
            byte savedFlags = flags;
            flags &= ~(CHILLED_LITERAL|CHILLED_SYMBOL_TO_S);
            if ((savedFlags & CHILLED_LITERAL) != 0) {
                getRuntime().getWarnings().warn("literal string will be frozen in the future, the string was created here: " + file + ":" + line);
            } else {
                super.mutateChilledString();
            }
        }
    }

    /**
     * An FString is a frozen string that is also deduplicated and cached. We add a field to hold one type of conversion
     * so it won't be performed repeatedly. Whatever type of conversion is requested first wins, since it will be very
     * rare for a String to be converted to a Symbol and a Fixnum and a Float.
     */
    public static class FString extends RubyString {
        private IRubyObject converted;
        private final int hash;
        private final RubyFixnum fixHash;

        protected FString(Ruby runtime, ByteList value, int cr) {
            super(runtime, runtime.getString(), value, cr, false);

            this.shareLevel = SHARE_LEVEL_BYTELIST;
            this.setFrozen(true);
            this.hash = strHashCode(runtime);
            this.fixHash = runtime.newFixnum(hash);
        }

        protected FString(Ruby runtime, String string) {
            super(runtime, runtime.getString(), string, UTF8, false);

            this.shareLevel = SHARE_LEVEL_BYTELIST;
            this.setFrozen(true);
            this.hash = strHashCode(runtime);
            this.fixHash = runtime.newFixnum(hash);
        }

        @Override
        protected void frozenCheck() {
            Ruby runtime = getRuntime();

            throw runtime.newFrozenError("String", this);
        }

        @Override
        public RubySymbol intern() {
            IRubyObject symbol = this.converted;

            if (symbol == null) {
                return (RubySymbol) (this.converted = super.intern());
            }

            if (symbol instanceof RubySymbol sym) return sym;

            return super.intern();
        }

        @Override
        public IRubyObject to_i(ThreadContext context) {
            IRubyObject integer = this.converted;

            if (integer == null) return this.converted = super.to_i(context);

            return integer instanceof RubyInteger ?
                    integer : super.to_i(context);
        }

        @Override
        public IRubyObject to_f(ThreadContext context) {
            IRubyObject flote = this.converted;

            if (flote == null) return this.converted = super.to_f(context);

            return flote instanceof RubyFloat ?
                    flote : super.to_f(context);
        }

        @Override
        public FString dupAsFString(Ruby runtime) {
            return this;
        }

        public RubyFixnum hash(ThreadContext context) {
            return fixHash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public long longHashCode() {
            return hash;
        }
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

    @SuppressWarnings("ReferenceEquality")
    @JRubyMethod(name = {"to_s", "to_str"})
    @Override
    public RubyString to_s(ThreadContext context) {
        return metaClass.getRealClass() != stringClass(context) ? dupString(context, this) : this;
    }

    @Override
    public final int compareTo(IRubyObject other) {
        var context = getRuntime().getCurrentContext();
        return toInt(context, op_cmp(context, other));
    }

    /* rb_str_cmp_m */
    @JRubyMethod(name = "<=>")
    @Override
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString otherStr) return asFixnum(context, op_cmp(otherStr));

        JavaSites.CheckedSites sites = sites(context).to_str_checked;
        if (sites.respond_to_X.respondsTo(context, this, other)) {
            IRubyObject tmp = TypeConverter.checkStringType(context, sites, other);
            if (tmp instanceof RubyString otherStr) return asFixnum(context, op_cmp(otherStr));
            return context.nil;
        } else {
            return invcmp(context, sites(context).recursive_cmp, this, other);
        }
    }

    /** rb_str_equal
     *
     */
    @JRubyMethod(name = {"==", "==="})
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if (this == other) return context.tru;

        return other instanceof RubyString otherStr ?
                asBoolean(context, StringSupport.areComparable(this, otherStr) && value.equal(otherStr.value)) :
                op_equalCommon(context, other);
    }

    private IRubyObject op_equalCommon(ThreadContext context, IRubyObject other) {
        if (!sites(context).respond_to_to_str.respondsTo(context, this, other)) return context.fals;
        return asBoolean(context, sites(context).equals.call(context, this, other, this).isTrue());
    }

    @JRubyMethod(name = {"-@", "dedup"}) // -'foo' returns frozen string
    public final IRubyObject minus_at(ThreadContext context) {
        return context.runtime.freezeAndDedupString(this);
    }

    @Deprecated(since = "10.0.0.0")
    public final IRubyObject plus_at() {
        return plus_at(getCurrentContext());
    }

    @JRubyMethod(name = "+@") // +'foo' returns modifiable string
    public final IRubyObject plus_at(ThreadContext context) {
        return isFrozen() | isChilled() ? this.dup() : this;
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject op_plus19(ThreadContext context, IRubyObject arg) {
        return op_plus(context, arg);
    }

    @JRubyMethod(name = "+")
    public IRubyObject op_plus(ThreadContext context, IRubyObject arg) {
        RubyString str = arg.convertToString();
        Encoding enc = checkEncoding(str);
        long len = (long) value.getRealSize() + str.value.getRealSize();

        // we limit to int because ByteBuffer can only allocate int sizes
        if (len > Integer.MAX_VALUE) throw argumentError(context, "argument too big");
        RubyString resultStr = newStringNoCopy(context.runtime, StringSupport.addByteLists(value, str.value),
                enc, CodeRangeSupport.codeRangeAnd(getCodeRange(), str.getCodeRange()));
        return resultStr;
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject op_mul19(ThreadContext context, IRubyObject other) {
        return op_mul(context, other);
    }

    @JRubyMethod(name = "*")
    public IRubyObject op_mul(ThreadContext context, IRubyObject other) {
        RubyString result = multiplyByteList(context, other);
        result.value.setEncoding(value.getEncoding());
        result.copyCodeRangeForSubstr(this, value.getEncoding());
        return result;
    }

    private RubyString multiplyByteList(ThreadContext context, IRubyObject arg) {
        long longLen = toLong(context, arg);
        if (longLen < 0) throw argumentError(context, "negative argument");
        if (isEmpty()) return (RubyString) dup();

        // we limit to int because ByteBuffer can only allocate int sizes
        int len = Helpers.multiplyBufferLength(context, value.getRealSize(), checkInt(context, longLen));

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
        return Create.newString(context, bytes);
    }

    @JRubyMethod(name = "%")
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

        // FIXME: Should we make this work with platform's locale,
        // or continue hardcoding US?
        Sprintf.sprintf1_9(out, Locale.US, value, tmp);

        return Create.newString(context, out);
    }

    @JRubyMethod
    public RubyFixnum hash(ThreadContext context) {
        return asFixnum(context, strHashCode(context.runtime));
    }

    @Override
    public int hashCode() {
        return strHashCode(getRuntime());
    }

    @Override
    public long longHashCode() {
        return strHashCode(getRuntime());
    }

    /**
     * Generate a hash for the String, using its associated Ruby instance's hash seed.
     *
     * @param runtime the runtime
     * @return calculated hash
     */
    public int strHashCode(Ruby runtime) {
        final ByteList value = this.value;
        final Encoding enc = value.getEncoding();
        long hash;
        if (runtime.isSiphashEnabled()) {
            hash = SipHashInline.hash24(Ruby.getHashSeed0(),
                    Ruby.getHashSeed1(), value.getUnsafeBytes(), value.getBegin(),
                    value.getRealSize());
        } else {
            hash = PerlHash.hash(Ruby.getHashSeed0(),
            value.getUnsafeBytes(), value.getBegin(), value.getRealSize());
        }
        hash ^= (enc.isAsciiCompatible() && scanForCodeRange() == CR_7BIT ? 0 : enc.getIndex());
        return (int) hash;
    }

    /**
     * Generate a hash for the String, without a seed.
     *
     * @param runtime the runtime
     * @return calculated hash
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

        return (other instanceof RubyString otherStr) && equals(otherStr);
    }

    @SuppressWarnings("NonOverridingEquals")
    final boolean equals(RubyString other) {
        return other.value.equal(value);
    }

    /** rb_obj_as_string
     *
     */
    public static RubyString objAsString(ThreadContext context, IRubyObject obj) {
        if (obj instanceof RubyString str) return str;
        IRubyObject str = sites(context).to_s.call(context, obj, obj);
        if (!(str instanceof RubyString)) return (RubyString) Convert.anyToString(context, obj);
        // TODO: MRI sets an fstring flag on fstrings and uses that flag here
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

    // Needs to remain in place until StringIO has migrated to the new methods
    // See https://github.com/ruby/stringio/issues/83
    @Deprecated(since = "10.0.0.0")
    public final RubyString cat19(RubyString str2) {
        return catWithCodeRange(str2);
    }

    // // rb_str_buf_append against VALUE
    public final RubyString catWithCodeRange(RubyString str2) {
        ByteList other = str2.getByteList();
        int str2_cr = catWithCodeRange(other, str2.getCodeRange());

        str2.setCodeRange(str2_cr);

        return this;
    }

    public final RubyString cat(ByteList other, int codeRange) {
        catWithCodeRange(other, codeRange);
        return this;
    }

    // Needs to remain in place until StringIO has migrated to the new methods
    // See https://github.com/ruby/stringio/issues/83
    // jruby-rack also uses this and must be updated: https://github.com/jruby/jruby-rack/issues/267
    @Deprecated(since = "10.0.0.0")
    public final int cat19(ByteList other, int codeRange) {
        return catWithCodeRange(other, codeRange);
    }

    // rb_str_buf_append against ptr
    public final int catWithCodeRange(ByteList other, int codeRange) {
        return EncodingUtils.encCrStrBufCat(metaClass.runtime, this, other, other.getEncoding(), codeRange);
    }

    /**
     * Append a Java String to this RubyString assuming it will be the encoding of the RubyString.  If it is
     * not then then it will end up as an invalid string.  Some methods assume an encoding of BINARY so that
     * broken bytes are possible/expected (e.g. an error message with two names which are not compatible to be
     * combined into a single Ruby String).  Proc#to_s is an example of this.
     * @param str to be appended
     * @return this string after it has appended str
     */
    public final RubyString catStringUnsafe(String str) {
        ByteList other = encodeBytelist(str, getEncoding());
        catWithCodeRange(other, CR_UNKNOWN);
        return this;
    }

    public final RubyString catString(String str) {
        ByteList other = encodeBytelist(str, UTF8);
        // if byte string is same size, assume all 7-bit; otherwise, it must be at least valid
        catWithCodeRange(other, other.realSize() == str.length() ? CR_7BIT : CR_VALID);
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

    @Deprecated(since = "10.0.0.0")
    public RubyString replace19(IRubyObject other) {
        return replace(getCurrentContext(), other);
    }

    @Deprecated(since = "10.0.0.0")
    public RubyString initialize_copy(IRubyObject other) {
        return initialize_copy(getCurrentContext(), other);
    }

    @JRubyMethod(name = "initialize_copy", visibility = Visibility.PRIVATE)
    public RubyString initialize_copy(ThreadContext context, IRubyObject other) {
        return replace(context, other);
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject replace(IRubyObject other) {
        return replace(getCurrentContext(), other);
    }

    /** rb_str_replace_m
     *
     */
    @JRubyMethod(name = "replace")
    public RubyString replace(ThreadContext context, IRubyObject other) {
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
        return otherStr;
    }

    @Deprecated(since = "10.0.0.0")
    public RubyString clear() {
        return clear(getCurrentContext());
    }

    @JRubyMethod
    public RubyString clear(ThreadContext context) {
        modifyCheck();
        Encoding enc = value.getEncoding();

        EmptyByteListHolder holder = getEmptyByteList(enc);
        value = holder.bytes;
        shareLevel = SHARE_LEVEL_BYTELIST;
        setCodeRange(holder.cr);
        return this;
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject reverse19(ThreadContext context) {
        return reverse(context);
    }

    @JRubyMethod(name = "reverse")
    public IRubyObject reverse(ThreadContext context) {
        RubyString str = dupString(context, this);
        str.reverse_bang(context);
        return str;
    }

    @Deprecated(since = "10.0.0.0")
    public RubyString reverse_bang19(ThreadContext context) {
        return reverse_bang(context);
    }

    @JRubyMethod(name = "reverse!")
    public RubyString reverse_bang(ThreadContext context) {
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
                    int cl = encFastMBCLen(bytes, p, end, enc);

                    op -= cl;
                    System.arraycopy(bytes, p, obytes, op, cl);
                    p += cl;
                }
                value.setUnsafeBytes(obytes);
                value.setBegin(0);
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
                value.setBegin(0);
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
        IRubyObject tmp = ArgsUtil.getOptionsArg(context, arg0);
        return tmp.isNil() ? initialize(context, arg0, null) : initialize(context, null, (RubyHash) tmp);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject arg0, IRubyObject opts) {
        IRubyObject tmp = ArgsUtil.getOptionsArg(context, opts);
        if (tmp.isNil()) throw argumentError(context, 2, 1);

        return initialize(context, arg0, (RubyHash) tmp);
    }

    private IRubyObject initialize(ThreadContext context, IRubyObject arg0, RubyHash opts) {
        if (arg0 != null) replace(context, arg0);

        if (opts != null) {
            IRubyObject encoding = opts.fastARef(asSymbol(context, "encoding"));
            IRubyObject capacity = opts.fastARef(asSymbol(context, "capacity"));

            if (capacity != null && !capacity.isNil()) {
                int capa = toInt(context, capacity);
                if (capa < STRING_MINIMUM_SIZE) capa = STRING_MINIMUM_SIZE;
                modify(capa);
            }
            if (encoding != null && !encoding.isNil()) {
                modify();
                setEncodingAndCodeRange(encodingService(context).getEncodingFromObject(encoding), CR_UNKNOWN);
            }
        }

        return this;
    }

    @JRubyMethod(name = "casecmp")
    public IRubyObject casecmp(ThreadContext context, IRubyObject other) {
        IRubyObject tmp = other.checkStringType();
        if (tmp.isNil()) return context.nil;

        RubyString otherStr = (RubyString) tmp;
        Encoding enc = StringSupport.areCompatible(this, otherStr);
        if (enc == null) return context.nil;

        return asFixnum(context, singleByteOptimizable() && otherStr.singleByteOptimizable() ?
                value.caseInsensitiveCmp(otherStr.value) : StringSupport.multiByteCasecmp(enc, value, otherStr.value));
    }

    @JRubyMethod(name = "casecmp?")
    public IRubyObject casecmp_p(ThreadContext context, IRubyObject other) {
        IRubyObject tmp = other.checkStringType();
        if (tmp.isNil()) return context.nil;
        RubyString otherStr = (RubyString) tmp;

        Encoding enc = StringSupport.areCompatible(this, otherStr);
        if (enc == null) return context.nil;

        int flags = Config.CASE_FOLD;
        RubyString down = dupString(context, this);
        down.downcase_bang(context, flags);
        RubyString otherDown = dupString(context, otherStr);
        otherDown.downcase_bang(context, flags);
        return asBoolean(context, down.equals(otherDown));
    }

    /** rb_str_match
     *
     */

    @JRubyMethod(name = "=~", writes = BACKREF)
    @Override
    public IRubyObject op_match(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyRegexp otherRegexp) return otherRegexp.op_match(context, this);
        if (other instanceof RubyString) throw typeError(context, "type mismatch: String given");
        return sites(context).op_match.call(context, other, other, this);
    }
    @Deprecated(since = "10.0.0.0")
    public IRubyObject match19(ThreadContext context, IRubyObject pattern) {
        return match(context, pattern, Block.NULL_BLOCK);
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject match19(ThreadContext context, IRubyObject pattern, IRubyObject pos, Block block) {
        return match(context, pattern, pos, block);
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject match19(ThreadContext context, IRubyObject[] args, Block block) {
        return match(context, args, block);
    }

    /**
     * String#match(pattern)
     *
     * rb_str_match_m
     *
     * @param pattern Regexp or String
     */

    @Deprecated(since = "10.0.0.0")
    public IRubyObject match(ThreadContext context, IRubyObject pattern) {
        return match(context, pattern, Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "match", writes = BACKREF)
    public IRubyObject match(ThreadContext context, IRubyObject pattern, Block block) {
        RubyRegexp coercedPattern = getPattern(context, pattern);
        IRubyObject result = sites(context).match.call(context, coercedPattern, coercedPattern, this);
        return block.isGiven() && result != context.nil ? block.yield(context, result) : result;
    }

    @JRubyMethod(name = "match", writes = BACKREF)
    public IRubyObject match(ThreadContext context, IRubyObject pattern, IRubyObject pos, Block block) {
        RubyRegexp coercedPattern = getPattern(context, pattern);
        IRubyObject result = sites(context).match.call(context, coercedPattern, coercedPattern, this, pos);
        return block.isGiven() && result != context.nil ? block.yield(context, result) : result;
    }

    @JRubyMethod(name = "match", required = 1, rest = true, checkArity = false)
    public IRubyObject match(ThreadContext context, IRubyObject[] args, Block block) {
        if (args.length < 1) {
            Arity.checkArgumentCount(context, args, 1, -1);
        }
        RubyRegexp pattern = getPattern(context, args[0]);
        args[0] = this;
        IRubyObject result = sites(context).match.call(context, pattern, pattern, args);
        return block.isGiven() && result != context.nil ? block.yield(context, result) : result;
    }

    @JRubyMethod(name = "match?")
    public IRubyObject match_p(ThreadContext context, IRubyObject pattern) {
        return getPattern(context, pattern).matchP(context, this, 0);
    }

    @JRubyMethod(name = "match?")
    public IRubyObject match_p(ThreadContext context, IRubyObject pattern, IRubyObject pos) {
        return getPattern(context, pattern).matchP(context, this, toInt(context, pos));
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject op_ge19(ThreadContext context, IRubyObject other) {
        return op_ge(context, other);
    }

    @JRubyMethod(name = ">=")
    public IRubyObject op_ge(ThreadContext context, IRubyObject other) {
        return other instanceof RubyString otherStr && cmpIsBuiltin(context) ?
            asBoolean(context, op_cmp(otherStr) >= 0) : RubyComparable.op_ge(context, this, other);
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject op_gt19(ThreadContext context, IRubyObject other) {
        return op_gt(context, other);
    }

    @JRubyMethod(name = ">")
    public IRubyObject op_gt(ThreadContext context, IRubyObject other) {
        return other instanceof RubyString otherStr && cmpIsBuiltin(context) ?
            asBoolean(context, op_cmp(otherStr) > 0) : RubyComparable.op_gt(context, this, other);
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject op_le19(ThreadContext context, IRubyObject other) {
        return op_le(context, other);
    }

    @JRubyMethod(name = "<=")
    public IRubyObject op_le(ThreadContext context, IRubyObject other) {
        return other instanceof RubyString otherStr && cmpIsBuiltin(context) ?
                asBoolean(context, op_cmp(otherStr) <= 0) : RubyComparable.op_le(context, this, other);
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject op_lt19(ThreadContext context, IRubyObject other) {
        return op_lt(context, other);
    }

    @JRubyMethod(name = "<")
    public IRubyObject op_lt(ThreadContext context, IRubyObject other) {
        return other instanceof RubyString otherStr && cmpIsBuiltin(context) ?
            asBoolean(context, op_cmp(otherStr) < 0) : RubyComparable.op_lt(context, sites(context).cmp, this, other);
    }

    private boolean cmpIsBuiltin(ThreadContext context) {
        return sites(context).cmp.isBuiltin(this);
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject str_eql_p19(ThreadContext context, IRubyObject other) {
        return str_eql_p(context, other);
    }

    @JRubyMethod(name = "eql?")
    public IRubyObject str_eql_p(ThreadContext context, IRubyObject other) {
        return asBoolean(context,
                other instanceof RubyString otherStr && StringSupport.areComparable(this, otherStr) && value.equal(otherStr.value));
    }

    private int caseMap(ThreadContext context, int flags, Encoding enc) {
        IntHolder flagsP = new IntHolder();
        flagsP.value = flags;
        if ((flags & Config.CASE_ASCII_ONLY) != 0) {
            StringSupport.asciiOnlyCaseMap(context, value, flagsP);
        } else {
            value = StringSupport.caseMap(context, value, flagsP, enc);
        }
        return flagsP.value;
    }

    /** rb_str_upcase / rb_str_upcase_bang
     *
     */
    @JRubyMethod(name = "upcase")
    public RubyString upcase(ThreadContext context) {
        RubyString str = dupString(context, this);
        str.upcase_bang(context);
        return str;
    }

    @JRubyMethod(name = "upcase")
    public RubyString upcase(ThreadContext context, IRubyObject arg) {
        RubyString str = dupString(context, this);
        str.upcase_bang(context, arg);
        return str;
    }

    @JRubyMethod(name = "upcase")
    public RubyString upcase(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        RubyString str = dupString(context, this);
        str.upcase_bang(context, arg0, arg1);
        return str;
    }

    @JRubyMethod(name = "upcase!")
    public IRubyObject upcase_bang(ThreadContext context) {
        return upcase_bang(context, Config.CASE_UPCASE);
    }

    @JRubyMethod(name = "upcase!")
    public IRubyObject upcase_bang(ThreadContext context, IRubyObject arg) {
        return upcase_bang(context, StringSupport.checkCaseMapOptions(context, arg, Config.CASE_UPCASE));
    }

    @JRubyMethod(name = "upcase!")
    public IRubyObject upcase_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return upcase_bang(context, StringSupport.checkCaseMapOptions(context, arg0, arg1, Config.CASE_UPCASE));
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
            flags = caseMap(context, flags, enc);
            if ((flags & Config.CASE_MODIFIED) != 0) clearCodeRange();
        }

        return ((flags & Config.CASE_MODIFIED) != 0) ? this : context.nil;
    }

    /** rb_str_downcase / rb_str_downcase_bang
     *
     */
    @JRubyMethod(name = "downcase")
    public RubyString downcase(ThreadContext context) {
        RubyString str = dupString(context, this);
        str.downcase_bang(context);
        return str;
    }

    @JRubyMethod(name = "downcase")
    public RubyString downcase(ThreadContext context, IRubyObject arg) {
        RubyString str = dupString(context, this);
        str.downcase_bang(context, arg);
        return str;
    }

    @JRubyMethod(name = "downcase")
    public RubyString downcase(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        RubyString str = dupString(context, this);
        str.downcase_bang(context, arg0, arg1);
        return str;
    }

    @JRubyMethod(name = "downcase!")
    public IRubyObject downcase_bang(ThreadContext context) {
        return downcase_bang(context, Config.CASE_DOWNCASE);
    }

    @JRubyMethod(name = "downcase!")
    public IRubyObject downcase_bang(ThreadContext context, IRubyObject arg) {
        return downcase_bang(context, StringSupport.checkCaseMapOptions(context, arg, Config.CASE_DOWNCASE));
    }

    @JRubyMethod(name = "downcase!")
    public IRubyObject downcase_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return downcase_bang(context, StringSupport.checkCaseMapOptions(context, arg0, arg1, Config.CASE_DOWNCASE));
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
            flags = caseMap(context, flags, enc);
            if ((flags & Config.CASE_MODIFIED) != 0) clearCodeRange();
        }

        return ((flags & Config.CASE_MODIFIED) != 0) ? this : context.nil;
    }

    /** rb_str_swapcase / rb_str_swapcase_bang
     *
     */
    @JRubyMethod(name = "swapcase")
    public RubyString swapcase(ThreadContext context) {
        RubyString str = dupString(context, this);
        str.swapcase_bang(context);
        return str;
    }

    @JRubyMethod(name = "swapcase")
    public RubyString swapcase(ThreadContext context, IRubyObject arg) {
        RubyString str = dupString(context, this);
        str.swapcase_bang(context, arg);
        return str;
    }

    @JRubyMethod(name = "swapcase")
    public RubyString swapcase(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        RubyString str = dupString(context, this);
        str.swapcase_bang(context, arg0, arg1);
        return str;
    }

    @JRubyMethod(name = "swapcase!")
    public IRubyObject swapcase_bang(ThreadContext context) {
        return swapcase_bang(context, Config.CASE_UPCASE | Config.CASE_DOWNCASE);
    }

    @JRubyMethod(name = "swapcase!")
    public IRubyObject swapcase_bang(ThreadContext context, IRubyObject arg) {
        return swapcase_bang(context, StringSupport.checkCaseMapOptions(context, arg, Config.CASE_UPCASE | Config.CASE_DOWNCASE));
    }

    @JRubyMethod(name = "swapcase!")
    public IRubyObject swapcase_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return swapcase_bang(context, StringSupport.checkCaseMapOptions(context, arg0, arg1, Config.CASE_UPCASE | Config.CASE_DOWNCASE));
    }

    private IRubyObject swapcase_bang(ThreadContext context, int flags) {
        modifyAndKeepCodeRange();
        flags = caseMap(context, flags, checkDummyEncoding());
        if ((flags & Config.CASE_MODIFIED) != 0) {
            clearCodeRange();
            return this;
        }

        return context.nil;
    }

    /** rb_str_capitalize / rb_str_capitalize_bang
     *
     */
    @JRubyMethod(name = "capitalize")
    public RubyString capitalize(ThreadContext context) {
        RubyString str = dupString(context, this);
        str.capitalize_bang(context);
        return str;
    }

    @JRubyMethod(name = "capitalize")
    public RubyString capitalize(ThreadContext context, IRubyObject arg) {
        RubyString str = dupString(context, this);
        str.capitalize_bang(context, arg);
        return str;
    }

    @JRubyMethod(name = "capitalize")
    public RubyString capitalize(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        RubyString str = dupString(context, this);
        str.capitalize_bang(context, arg0, arg1);
        return str;
    }

    @JRubyMethod(name = "capitalize!")
    public IRubyObject capitalize_bang(ThreadContext context) {
        return capitalize_bang(context, Config.CASE_UPCASE | Config.CASE_TITLECASE);
    }

    @JRubyMethod(name = "capitalize!")
    public IRubyObject capitalize_bang(ThreadContext context, IRubyObject arg) {
        return capitalize_bang(context, StringSupport.checkCaseMapOptions(context, arg, Config.CASE_UPCASE | Config.CASE_TITLECASE));
    }

    @JRubyMethod(name = "capitalize!")
    public IRubyObject capitalize_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return capitalize_bang(context, StringSupport.checkCaseMapOptions(context, arg0, arg1, Config.CASE_UPCASE | Config.CASE_TITLECASE));
    }

    private IRubyObject capitalize_bang(ThreadContext context, int flags) {
        modifyAndKeepCodeRange();
        Encoding enc = checkDummyEncoding();

        if (value.getRealSize() == 0) {
            modifyCheck();
            return context.nil;
        }

        flags = caseMap(context, flags, enc);
        if ((flags & Config.CASE_MODIFIED) != 0) {
            clearCodeRange();
            return this;
        }

        return context.nil;
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject dump() {
        return dump(getCurrentContext());
    }

    /** rb_str_dump
     *
     */
    @JRubyMethod(name = "dump")
    public IRubyObject dump(ThreadContext context) {
        ByteList outBytes = StringSupport.dumpCommon(context.runtime, value);

        final RubyString result = Create.newString(context, outBytes);
        Encoding enc = value.getEncoding();

        if (!enc.isAsciiCompatible()) {
            result.cat(".force_encoding(\"".getBytes());
            result.cat(enc.getName());
            result.cat((byte)'"').cat((byte)')');
            enc = ASCII;
        }
        result.associateEncoding(enc);
        result.setCodeRange(CR_7BIT);

        return result;
    }

    @JRubyMethod(name = "undump")
    public IRubyObject undump(ThreadContext context) {
        RubyString str = this;
        ByteList strByteList = str.value;
        byte[] sBytes = strByteList.unsafeBytes();
        int[] s = {strByteList.begin()};
        int sLen = strByteList.realSize();
        int s_end = s[0] + strByteList.realSize();
        Encoding enc[] = {str.getEncoding()};
        RubyString undumped = newString(context.runtime, sBytes, s[0], 0, enc[0]);
        boolean[] utf8 = {false};
        boolean[] binary = {false};

        verifyAsciiCompatible();
        scanForCodeRange();
        if (!isAsciiOnly()) throw runtimeError(context, "non-ASCII character detected");
        if (hasNul()) throw runtimeError(context, "string contains null byte");
        if (sLen < 2) return invalidFormat(context);
        if (sBytes[s[0]] != '"') return invalidFormat(context);

        /* strip '"' at the start */
        s[0]++;

        for (; ; ) {
            if (s[0] >= s_end) throw runtimeError(context, "unterminated dumped string");

            if (sBytes[s[0]] == '"') {
                /* epilogue */
                s[0]++;
                if (s[0] == s_end) {
                    /* ascii compatible dumped string */
                    break;
                } else {
                    int size;

                    if (utf8[0]) throw runtimeError(context, "dumped string contained Unicode escape but used force_encoding");

                    size = FORCE_ENCODING_BYTES.length;
                    if (s_end - s[0] <= size) return invalidFormat(context);
                    if (ByteList.memcmp(sBytes, s[0], FORCE_ENCODING_BYTES, 0, size) != 0) return invalidFormat(context);
                    s[0] += size;

                    int encname = s[0];
                    s[0] = memchr(sBytes, s[0], '"', s_end - s[0]);
                    size = s[0] - encname;
                    if (s[0] == -1) return invalidFormat(context);
                    if (s_end - s[0] != 2) return invalidFormat(context);
                    if (sBytes[s[0]] != '"' || sBytes[s[0] + 1] != ')') return invalidFormat(context);

                    Encoding enc2 = encodingService(context).findEncodingNoError(new ByteList(sBytes, encname, size));
                    if (enc2 == null) throw runtimeError(context, "dumped string has unknown encoding name");

                    undumped.setEncoding(enc2);
                }
                break;
            }

            if (sBytes[s[0]] == '\\'){
                s[0]++;
                if (s[0] >= s_end) throw runtimeError(context, "invalid escape");
                undumped.undumpAfterBackslash(context, sBytes, s, s_end, enc, utf8, binary);
            } else {
                undumped.cat(sBytes, s[0]++, 1);
            }
        }

        return undumped;
    }

    private static IRubyObject invalidFormat(ThreadContext context) {
        throw runtimeError(context, "invalid dumped string; not wrapped with '\"' nor '\"...\".force_encoding(\"...\")' form");
    }

    @SuppressWarnings("ReferenceEquality")
    private void undumpAfterBackslash(ThreadContext context, byte[] ssBytes, int[] ss, int s_end, Encoding[] penc,
                                      boolean[] utf8, boolean[] binary) {
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
                    throw runtimeError(context, "hex escape and Unicode escape are mixed");
                }
                utf8[0] = true;
                if (++s >= s_end) {
                    throw runtimeError(context, "invalid Unicode escape");
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
                            throw runtimeError(context, "unterminated Unicode escape");
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
                            throw runtimeError(context, "invalid Unicode escape");
                        }
                        if (c > 0x10ffff) {
                            throw runtimeError(context, "invalid Unicode codepoint (too large)");
                        }
                        if (0xd800 <= c && c <= 0xdfff) {
                            throw runtimeError(context, "invalid Unicode codepoint");
                        }
                        codelen = EncodingUtils.encMbcput((int) c, buf, 0, penc[0]);
                        cat(buf, 0, codelen);
                        s += hexlen[0];
                    }
                }
                else { /* handle uXXXX form */
                    c = scanHex(ssBytes, s, 4, hexlen);
                    if (hexlen[0] != 4) {
                        throw runtimeError(context, "invalid Unicode escape");
                    }
                    if (0xd800 <= c && c <= 0xdfff) {
                        throw runtimeError(context, "invalid Unicode codepoint");
                    }
                    codelen = EncodingUtils.encMbcput((int) c, buf, 0, penc[0]);
                    cat(buf, 0, codelen);
                    s += hexlen[0];
                }
                break;
            case 'x':
                if (utf8[0]) {
                    throw runtimeError(context, "hex escape and Unicode escape are mixed");
                }
                binary[0] = true;
                if (++s >= s_end) {
                    throw runtimeError(context, "invalid hex escape");
                }
                buf[0] = (byte) scanHex(ssBytes, s, 2, hexlen);
                if (hexlen[0] != 2) {
                    throw runtimeError(context, "invalid hex escape");
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

        while (len-- > 0 && s < bytes.length && (tmp = memchr(hexdigit, 0, bytes[s], hexdigit.length)) != -1) {
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
        int index = toInt(context, indexArg);
        if (index == -1) {
            modifyCheck();
            return catWithCodeRange(str);
        }
        if (index < 0) index++;
        strUpdate(context, index, 0, this, str);
        return this;
    }

    private int checkIndexForRef(ThreadContext context, int beg, int len) {
        if (beg >= len) raiseIndexOutOfString(context, beg);
        if (beg < 0) {
            if (-beg > len) raiseIndexOutOfString(context, beg);
            beg += len;
        }
        return beg;
    }
    private int checkLength(ThreadContext context, int len) {
        if (len < 0) throw indexError(context, "negative length " + len);
        return len;
    }

    private void raiseIndexOutOfString(ThreadContext context, int index) {
        throw indexError(context, "index " + index + " out of string");
    }

    @Deprecated(since = "10.0.0.0")
    public RubyString inspect() {
        // The return type confuses existing extensions who expect this method on RubyString.
        return (RubyString) super.inspect();
    }

    /** rb_str_inspect
     *
     */
    @Override
    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        return inspect(context, value);
    }

    final RubyString inspect(final Ruby runtime) {
        return inspect(runtime.getCurrentContext(), value);
    }

    // MRI: rb_str_escape
    public static IRubyObject rbStrEscape(ThreadContext context, RubyString str) {
        Encoding enc = str.getEncoding();
        ByteList strBL = str.getByteList();
        byte[] pBytes = strBL.unsafeBytes();
        int p = strBL.begin();
        int pend = p + strBL.realSize();
        int prev = p;
        RubyString result = Create.newEmptyString(context);
        boolean unicode_p = enc.isUnicode();
        boolean asciicompat = enc.isAsciiCompatible();

        while (p < pend) {
            int c, cc;
            int n = enc.length(pBytes, p, pend);
            if (!MBCLEN_CHARFOUND_P(n)) {
                if (p > prev) result.cat(pBytes, prev, p - prev);
                n = enc.minLength();
                if (pend < p + n)
                    n = (pend - p);
                while (n-- > 0) {
                    result.modify();
                    Sprintf.sprintf(context.runtime, result.getByteList(), "\\x%02X", pBytes[p] & 0377);
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
                Sprintf.sprintf(context.runtime, result.getByteList(), StringSupport.escapedCharFormat(c, unicode_p), (c & 0xFFFFFFFFL));
                prev = p;
            }
        }
        if (p > prev) result.cat(pBytes, prev, p - prev);
        result.setEncodingAndCodeRange(USASCIIEncoding.INSTANCE, CR_7BIT);

        return result;
    }

    @Deprecated(since = "10.0.0.0")
    public static RubyString inspect(final Ruby runtime, ByteList byteList) {
        return inspect(runtime.getCurrentContext(), byteList);
    }

    @SuppressWarnings("ReferenceEquality")
    public static RubyString inspect(ThreadContext context, ByteList byteList) {
        Encoding enc = byteList.getEncoding();
        byte bytes[] = byteList.getUnsafeBytes();
        int p = byteList.getBegin();
        int end = p + byteList.getRealSize();
        RubyString result = Create.newString(context, new ByteList(end - p));
        Encoding resultEnc = context.runtime.getDefaultInternalEncoding();
        boolean isUnicode = enc.isUnicode();
        boolean asciiCompat = enc.isAsciiCompatible();


        if (resultEnc == null) resultEnc = context.runtime.getDefaultExternalEncoding();
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
                    Sprintf.sprintf(context.runtime, result.getByteList() ,"\\x%02X", bytes[p] & 0377);
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
                                    ((cc = codePoint(context, enc, bytes, p, end)) == '$' ||
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
                Sprintf.sprintf(context.runtime, result.getByteList() , StringSupport.escapedCharFormat(c, isUnicode), (c & 0xFFFFFFFFL));
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
        return asFixnum(context, strLength());
    }

    @JRubyMethod(name = "bytesize")
    public RubyFixnum bytesize(ThreadContext context) {
        return asFixnum(context, value.getRealSize());
    }

    @Deprecated(since = "10.0.0.0")
    public RubyFixnum bytesize() {
        return bytesize(getCurrentContext());
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
        IRubyObject subStr = substrEnc(getRuntime().getCurrentContext(), start, end - start);
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
        return recv.bytesize(context);
    }

    /** rb_str_empty
     *
     */
    @JRubyMethod(name = "empty?")
    public RubyBoolean empty_p(ThreadContext context) {
        return asBoolean(context, isEmpty());
    }

    @JRubyAPI
    public boolean isEmpty() {
        return value.length() == 0;
    }

    public void appendIntoString(RubyString target) {
        target.catWithCodeRange(getByteList(), getCodeRange());
    }

    /** rb_str_append
     *
     */
    public RubyString append(IRubyObject other, Function<IRubyObject, RubyString> convert) {
        if (other instanceof Appendable appendable) {
            appendable.appendIntoString(this);
        } else {
            catWithCodeRange(convert.apply(other));
        }

        return this;
    }

    public RubyString append(IRubyObject other) {
        return append(other, (o) -> o.convertToString());
    }

    public RubyString append(RubyString other) {
        return catWithCodeRange(other);
    }

    @Deprecated(since = "9.4.4.0")
    public RubyString append19(IRubyObject other) {
        return append(other);
    }

    public RubyString appendAsDynamicString(IRubyObject other) {
        return append(other, (o) -> o.asString());
    }

    public RubyString appendAsStringOrAny(IRubyObject other) {
        return append(other, (o) -> (RubyString) o.anyToString());
    }

    // NOTE: append(RubyString) should pbly just do the encoding aware cat
    final RubyString append19(RubyString other) {
        modifyCheck();
        return catWithCodeRange(other);
    }

    /** rb_str_concat
     *
     */
    @JRubyMethod(name = "<<")
    public RubyString concatSingle(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString otherStr) {
            // duplicated (default) return path - since its common
            return append(otherStr);
        }
        if (other instanceof RubyFixnum fixnum) {
            if (fixnum.getValue() < 0) throw rangeError(context, fixnum.getValue() + " out of char range");

            return concatNumeric(context, (int)(fixnum.getValue() & 0xFFFFFFFF));
        }
        if (other instanceof RubyBignum bignum) {
            if (bignum.signum(context) < 0) throw rangeError(context, "negative string size (or size too big)");

            return concatNumeric(context, bignum.asInt(context));
        }
        if (other instanceof RubyFloat flote) {
            modifyCheck();
            return catWithCodeRange((RubyString) flote.to_s(context));
        }
        if (other instanceof RubySymbol) throw typeError(context, "can't convert Symbol into String");

        return append(other.convertToString());
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
        modifyCheck();

        if (objs.length > 0) {
            RubyString tmp = newStringLight(context.runtime, objs.length, getEncoding());

            for (IRubyObject obj : objs) {
                tmp.concatSingle(context, obj);
            }

            catWithCodeRange(tmp);
        }

        return this;
    }

    public RubyString concat(IRubyObject other) {
        return concat(metaClass.runtime.getCurrentContext(), other);
    }

    @SuppressWarnings("ReferenceEquality")
    private RubyString concatNumeric(ThreadContext context, int c) {
        Encoding enc = value.getEncoding();
        int cl;

        try {
            // TODO: much of this encoding promotion logic is duplicated in rbAscii8bitAppendableEncodingIndex
            cl = codeLength(enc, c);

            if (cl <= 0) throw rangeError(context, c + " out of char range or invalid code point");

            modifyExpand(value.getRealSize() + cl);

            if (enc == USASCIIEncoding.INSTANCE) {
                if (c > 0xff) throw rangeError(context, c + " out of char range");
                if (c > 0x79) {
                    value.setEncoding(ASCIIEncoding.INSTANCE);
                    enc = value.getEncoding();
                }
            }
            enc.codeToMbc(c, value.getUnsafeBytes(), value.getBegin() + value.getRealSize());
        } catch (EncodingException e) {
            throw rangeError(context, c + " out of char range");
        }
        value.setRealSize(value.getRealSize() + cl);
        return this;
    }

    /**
     * rb_str_prepend
     */
    @JRubyMethod
    public IRubyObject prepend(ThreadContext context, IRubyObject other) {
        RubyString rubyString = other.convertToString();
        return replace(context, rubyString.op_plus(context, this));
    }

    /**
     * rb_str_prepend
     */
    @JRubyMethod(rest = true)
    public IRubyObject prepend(ThreadContext context, IRubyObject[] objs) {
        modifyCheck();

        if (objs.length > 0) {
            RubyString tmp = newStringLight(context.runtime, objs.length, getEncoding());

            for (IRubyObject obj : objs) {
                tmp.concat(context, obj);
            }

            strUpdate(context, 0, 0, this, tmp);
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
        Encoding ascii8bit = encodingService(context).getAscii8bitEncoding();
        RubyString otherStr = dupString(context, other.convertToString());
        otherStr.modify();
        otherStr.associateEncoding(ascii8bit);
        ByteList otherBL = otherStr.getByteList();
        if (otherBL.length() < 2) throw argumentError(context, "salt too short (need >=2 bytes)");

        POSIX posix = context.runtime.getPosix();
        byte[] keyBytes = Arrays.copyOfRange(value.unsafeBytes(), value.begin(), value.begin() + value.realSize());
        byte[] saltBytes = Arrays.copyOfRange(otherBL.unsafeBytes(), otherBL.begin(), otherBL.begin() + otherBL.realSize());
        if (saltBytes[0] == 0 || saltBytes[1] == 0) throw argumentError(context, "salt too short (need >=2 bytes)");
        byte[] cryptedString = posix.crypt(keyBytes, saltBytes);
        // We differ from MRI in that we do not process salt to make it work and we will
        // return any errors via errno.
        if (cryptedString == null) throw context.runtime.newErrnoFromInt(posix.errno());

        RubyString result = RubyString.newStringNoCopy(context.runtime, cryptedString, 0, cryptedString.length - 1);
        result.associateEncoding(ascii8bit);
        return result;
    }

    /* RubyString aka rb_string_value */
    public static RubyString stringValue(IRubyObject object) {
        return (RubyString) (object instanceof RubyString ? object : object.convertToString());
    }

    /** rb_str_sub / rb_str_sub_bang
     *
     */

    @JRubyMethod(name = "sub", writes = BACKREF)
    public IRubyObject sub(ThreadContext context, IRubyObject arg0, Block block) {
        RubyString str = dupString(context, this);
        str.sub_bang(context, arg0, block);
        return str;
    }

    @JRubyMethod(name = "sub", writes = BACKREF)
    public IRubyObject sub(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        RubyString str = dupString(context, this);
        str.sub_bang(context, arg0, arg1, block);
        return str;
    }

    @JRubyMethod(name = "sub!", writes = BACKREF)
    public IRubyObject sub_bang(ThreadContext context, IRubyObject arg0, Block block) {
        frozenCheck();
        if (!block.isGiven()) throw argumentError(context, 1, 2);
        return subBangIter(context, arg0, null, block);
    }

    @JRubyMethod(name = "sub!", writes = BACKREF)
    public IRubyObject sub_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        IRubyObject hash = TypeConverter.convertToTypeWithCheck(context, arg1, hashClass(context), sites(context).to_hash_checked);
        frozenCheck();

        return hash == context.nil ?
                subBangNoIter(context, arg0, arg1.convertToString()) :
                subBangIter(context, arg0, (RubyHash) hash, block);
    }

    private IRubyObject subBangIter(ThreadContext context, IRubyObject arg0, RubyHash hash, Block block) {
        return arg0 instanceof RubyRegexp regexp ?
                subBangIter(context, regexp, hash, block) :
                subBangIter(context, getStringForPattern(context, arg0),  hash, block);
    }

    private IRubyObject subBangIter(ThreadContext context, RubyString pattern, RubyHash hash, Block block) {
        int len = value.getRealSize();
        byte[] bytes = value.getUnsafeBytes();
        Encoding enc = value.getEncoding();
        final int mBeg = StringSupport.index(getByteList(), pattern.getByteList(), 0, checkEncoding(pattern));

        if (mBeg > -1) {
            final RubyString repl;
            final int mLen = pattern.size();
            final int mEnd = mBeg + mLen;
            final RubyMatchData match = new RubyMatchData(context.runtime);

            match.initMatchData(this, mBeg, pattern);

            // set backref for user
            context.setBackRef(match);

            IRubyObject subStr = makeShared(context.runtime, mBeg, mLen);
            if (hash == null) {
                repl = objAsString(context, block.yield(context, subStr));
            } else {
                repl = objAsString(context, hash.op_aref(context, subStr));
            }

            modifyCheck(bytes, len, enc);

            return subBangCommon(context, mBeg, mEnd, repl);
        }

        // set backref for user
        return context.clearBackRef();
    }

    private IRubyObject subBangIter(ThreadContext context, RubyRegexp regexp, RubyHash hash, Block block) {
        Regex pattern = regexp.getPattern(context);
        Regex prepared = regexp.preparePattern(context, this);

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

            final RubyString repl;
            IRubyObject subStr = makeShared(context.runtime, mBeg, mEnd - mBeg);
            if (hash == null) {
                repl = objAsString(context, block.yield(context, subStr));
            } else {
                repl = objAsString(context, hash.op_aref(context, subStr));
            }

            modifyCheck(bytes, len, enc);

            return subBangCommon(context, mBeg, mEnd, repl);
        }

        // set backref for user
        return context.clearBackRef();
    }

    private IRubyObject subBangNoIter(ThreadContext context, IRubyObject arg0, RubyString repl) {
        if (arg0 instanceof RubyRegexp regexp) {
            return subBangNoIter(context, regexp, repl);
        } else {
            return subBangNoIter(context, getStringForPattern(context, arg0), repl);
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

            return subBangCommon(context, mBeg, mEnd, repl);
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

            return subBangCommon(context, match.begin, match.end, repl);
        }

        // set backref for user
        return context.clearBackRef();
    }

    /**
     * sub! but without any frame globals ...
     * @param context current context
     * @param regexp the regular expression
     * @param repl replacement string value
     * @return sub result
     */
    public final IRubyObject subBangFast(ThreadContext context, RubyRegexp regexp, RubyString repl) {
        RubyMatchData match = subBangMatch(context, regexp, repl);
        if (match != null) {
            repl = RubyRegexp.regsub(context, repl, this, regexp.pattern, match.regs, match.begin, match.end);
            subBangCommon(context, match.begin, match.end, repl);
            return match;
        }
        return context.nil;
    }

    private RubyMatchData subBangMatch(ThreadContext context, RubyRegexp regexp, RubyString repl) {
        Regex pattern = regexp.getPattern(context);
        Regex prepared = regexp.preparePattern(context, this);

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
        final RubyString repl) { // the sub replacement string

        Encoding enc = StringSupport.areCompatible(this, repl);
        if (enc == null) enc = subBangVerifyEncoding(context, repl, beg, end);

        final ByteList replValue = repl.value;
        final int replSize = replValue.getRealSize();
        final int plen = end - beg;

        if (replSize > plen) {
            modifyExpand(value.getRealSize() + replSize - plen);
        } else {
            modifyAndClearCodeRange();
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
        return (RubyString) this; // this
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

    @JRubyMethod(name = "gsub", writes = BACKREF)
    public IRubyObject gsub(ThreadContext context, IRubyObject arg0, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "gsub", arg0);

        return gsubCommon(context, block, null, null, arg0, false);

    }

    @JRubyMethod(name = "gsub", writes = BACKREF)
    public IRubyObject gsub(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return gsubImpl(context, arg0, arg1, block, false);
    }

    @JRubyMethod(name = "gsub!", writes = BACKREF)
    public IRubyObject gsub_bang(ThreadContext context, IRubyObject arg0, Block block) {
        checkFrozen();

        if (!block.isGiven()) return enumeratorize(context.runtime, this, "gsub!", arg0);

        return gsubCommon(context, block, null, null, arg0, true);
    }

    @JRubyMethod(name = "gsub!", writes = BACKREF)
    public IRubyObject gsub_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        checkFrozen();

        return gsubImpl(context, arg0, arg1, block, true);
    }

    private IRubyObject gsubImpl(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block, final boolean bang) {
        IRubyObject tryHash = TypeConverter.convertToTypeWithCheck(context, arg1, hashClass(context), sites(context).to_hash_checked);

        final RubyHash hash;
        final RubyString str;
        if (tryHash == context.nil) {
            hash = null;
            str = arg1.convertToString();
        } else {
            hash = (RubyHash) tryHash;
            str = null;
        }

        return gsubCommon(context, block, str, hash, arg0, bang);
    }

    public RubyString gsubFast(ThreadContext context, RubyRegexp regexp, RubyString repl, Block block) {
        return (RubyString) gsubCommon(context, block, repl, null, regexp, false, false);
    }

    private IRubyObject gsubCommon(ThreadContext context, Block block, RubyString repl,
                                   RubyHash hash, IRubyObject arg0, final boolean bang) {
        return gsubCommon(context, block, repl, hash, arg0, bang, true);
    }

    private IRubyObject gsubCommon(ThreadContext context, Block block, RubyString repl,
                                   RubyHash hash, IRubyObject arg0, final boolean bang, boolean useBackref) {
        if (arg0 instanceof RubyRegexp regexp) {
            return gsubCommon(context, block, repl, hash, regexp, bang, useBackref);
        } else {
            return gsubCommon(context, block, repl, hash, getStringForPattern(context, arg0), bang, useBackref);
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
                                   RubyHash hash, RubyString pattern, final boolean bang, boolean useBackref) {
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

            return bang ? context.nil : dupString(context, this); /* bang: true, no match, no substitution */
        }

        int offset = 0; int cp = spBeg; //int n = 0;
        RubyString dest = Create.newString(context, new ByteList(spLen + 30));
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
                    match = new RubyMatchData(context.runtime);
                    match.initMatchData(this, begz, pattern);

                    // set backref for user
                    if (useBackref) context.setBackRef(match);

                    val = objAsString(context, block.yield(context, dupString(context, pattern)));
                }
                modifyCheck(spBytes, spLen, str_enc);
                if (bang) frozenCheck();
            }

            int len = begz - offset;
            if (len != 0) dest.cat(spBytes, cp, len, str_enc);
            dest.catWithCodeRange(val);
            offset = endz;
            if (begz == endz) {
                if (spLen <= endz) break;
                len = encFastMBCLen(spBytes, spBeg + endz, spBeg + spLen, str_enc);
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
                match = new RubyMatchData(context.runtime);
                match.initMatchData(this, begz, pattern);

                // set backref for user
                context.setBackRef(match);
            }
        }

        if (bang) {
            view(dest.value);
            setCodeRange(dest.getCodeRange());
            return this;
        }
        return dest;
    }

    private IRubyObject gsubCommon(ThreadContext context, Block block, RubyString repl,
                                   RubyHash hash, RubyRegexp regexp, final boolean bang, boolean useBackref) {
        Regex pattern = regexp.getPattern(context);
        Regex prepared = regexp.preparePattern(context, this);

        final byte[] spBytes = value.getUnsafeBytes();
        final int spBeg = value.getBegin();
        final int spLen = value.getRealSize();

        final Matcher matcher = prepared.matcher(spBytes, spBeg, spBeg + spLen);

        int beg = RubyRegexp.matcherSearch(context, matcher, spBeg, spBeg + spLen, Option.NONE);
        if (beg < 0) {
            // set backref for user
            if (useBackref) context.clearBackRef();

            return bang ? context.nil : dupString(context, this); /* bang: true, no match, no substitution */
        }

        int offset = 0; int cp = spBeg; //int n = 0;
        RubyString dest = Create.newString(context, new ByteList(spLen + 30));
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
                final RubyString substr = makeSharedString(context.runtime, begz, endz - begz);
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

            int len = begz - offset;
            if (len != 0) dest.cat(spBytes, cp, len, str_enc);
            dest.catWithCodeRange(val);
            offset = endz;
            if (begz == endz) {
                if (spLen <= endz) break;
                len = encFastMBCLen(spBytes, spBeg + endz, spBeg + spLen, str_enc);
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
            return this;
        }
        return dest;
    }

    /** rb_str_index_m
     *
     */
    @JRubyMethod(name = "index", writes = BACKREF)
    public IRubyObject index(ThreadContext context, IRubyObject arg0) {
        return indexCommon(context, arg0, 0);
    }

    @JRubyMethod(name = "index", writes = BACKREF)
    public IRubyObject index(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        int pos = toInt(context, arg1);
        if (pos < 0) {
            pos += strLength();
            if (pos < 0) {
                // set backref for user
                if (arg0 instanceof RubyRegexp) context.clearBackRef();

                return context.nil;
            }
        }
        return indexCommon(context, arg0, pos);
    }

    @JRubyMethod(writes = BACKREF)
    public IRubyObject byteindex(ThreadContext context, IRubyObject arg0) {
        return byteIndexCommon(context, arg0, 0);
    }

    @JRubyMethod(writes = BACKREF)
    public IRubyObject byteindex(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        int pos = toInt(context, arg1);
        if (pos < 0) {
            pos += value.realSize();
            if (pos < 0) {
                // set backref for user
                if (arg0 instanceof RubyRegexp) context.clearBackRef();

                return context.nil;
            }
        }

        return byteIndexCommon(context, arg0, pos);
    }

    private IRubyObject indexCommon(ThreadContext context, IRubyObject sub, int pos) {
        if (sub instanceof RubyRegexp regexp) {
            if (pos > strLength()) {
                context.clearBackRef();
                return context.nil;
            }

            pos = singleByteOptimizable() ?
                    pos : nth(regexp.checkEncoding(context, this), value, pos) - value.getBegin();
            pos = regexp.adjustStartPos(context, this, pos, false);
            pos = regexp.search(context, this, pos, false);
            if (pos >= 0) pos = subLength(context.getLocalMatch().begin(0));
        } else {
            RubyString str = sub.convertToString();
            pos = StringSupport.index(this, str, pos, checkEncoding(str));
            pos = subLength(pos);
        }

        return pos < 0 ? context.nil : asFixnum(context, pos);
    }

    private IRubyObject byteIndexCommon(ThreadContext context, IRubyObject sub, int pos) {
        int len = value.realSize();
        if (pos < 0 || pos > len) {
            if (sub instanceof RubyRegexp) context.clearBackRef();
            return context.nil;
        }

        ensureBytePosition(context, pos);

        if (sub instanceof RubyRegexp regexp) {
            if (pos > len) return context.nil;
            pos = regexp.search(context, this, pos, false);
            if (pos >= 0) pos = context.getLocalMatch().begin(0);
        } else {
            RubyString str = sub.convertToString();
            pos = StringSupport.byteindex(this, str, pos, checkEncoding(str));
        }

        return pos < 0 ? context.nil : asFixnum(context, pos);
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

    /** rb_str_rindex_m
     *
     */
    @JRubyMethod(name = "rindex", writes = BACKREF)
    public IRubyObject rindex(ThreadContext context, IRubyObject arg0) {
        return rindexCommon(context, arg0, strLength());
    }

    @JRubyMethod(name = "rindex", writes = BACKREF)
    public IRubyObject rindex(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        int pos = toInt(context, arg1);
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

    @JRubyMethod(writes = BACKREF)
    public IRubyObject byterindex(ThreadContext context, IRubyObject arg0) {
        return byterindexCommon(context, arg0, value.realSize());
    }

    @JRubyMethod(writes = BACKREF)
    public IRubyObject byterindex(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        int pos = toInt(context, arg1);
        int length = value.realSize();
        if (pos < 0) {
            pos += length;
            if (pos < 0) {
                // set backref for user
                if (arg0 instanceof RubyRegexp) context.clearBackRef();

                return context.nil;
            }
        }
        if (pos > length) pos = length;

        ensureBytePosition(context, pos);

        return byterindexCommon(context, arg0, pos);
    }

    private IRubyObject byterindexCommon(ThreadContext context, final IRubyObject sub, int pos) {
        if (sub instanceof RubyRegexp regexp) {
            if (pos > value.realSize()) return context.nil;
            pos = regexp.search(context, this, pos, true);
            if (pos >= 0) pos = context.getLocalMatch().begin(0);
        } else {
            RubyString str = sub.convertToString();
            Encoding enc = checkEncoding(str);
            pos = StringSupport.byterindex(value, pos, str, enc);
        }

        return pos < 0 ? context.nil : asFixnum(context, pos);
    }

    private IRubyObject rindexCommon(ThreadContext context, final IRubyObject sub, int pos) {
        if (sub instanceof RubyRegexp regexp) {
            pos = offset(value.getEncoding(), value.getUnsafeBytes(), value.getBegin(),
                    value.getBegin() + value.getRealSize(), pos, singleByteOptimizable());
            pos = regexp.search(context, this, pos, true);
            if (pos >= 0) pos = subLength(context.getLocalMatch().begin(0));
        } else {
            RubyString str = sub.convertToString();
            Encoding enc = checkEncoding(str);
            pos = StringSupport.rindex(value,
                    StringSupport.strLengthFromRubyString(this, enc),
                    StringSupport.strLengthFromRubyString(str, enc),
                    pos, str, enc);
        }

        return pos < 0 ? context.nil : asFixnum(context, pos);
    }

    @Deprecated(since = "9.4-") //2008
    public final IRubyObject substr(int beg, int len) {
        return substr(getCurrentContext(), beg, len);
    }

    @Deprecated(since = "10.0.0.0")
    public final IRubyObject substr(Ruby runtime, int beg, int len) {
        return substr(getCurrentContext(), beg, len);
    }

    /* rb_str_substr */
    public final IRubyObject substr(ThreadContext context, int beg, int len) {
        int length = value.length();
        if (len < 0 || beg > length) return context.nil;

        if (beg < 0) {
            beg += length;
            if (beg < 0) return context.nil;
        }

        int end = Math.min(length, beg + len);
        return makeSharedString(context.runtime, beg, end - beg);
    }

    /* str_byte_substr */
    private IRubyObject byteSubstr(ThreadContext context, long beg, long len) {
        int length = value.length();

        if (len < 0 || beg > length) return context.nil;

        if (beg < 0) {
            beg += length;
            if (beg < 0) return context.nil;
        }

        if (beg + len > length) len = length - beg;
        if (len <= 0) len = 0;

        // above boundary checks confirms we can safely cast to int for beg + len.
        return makeSharedString(context.runtime, (int) beg, (int) len);
    }

    /* str_byte_aref */
    private IRubyObject byteARef(ThreadContext context, IRubyObject idx) {
        final int index;

        if (idx instanceof RubyRange range){
            int[] begLen = range.begLenInt(context, getByteList().length(), 0);
            return begLen == null ? context.nil : byteSubstr(context, begLen[0], begLen[1]);
        } else if (idx instanceof RubyFixnum fixnum) {
            long i = fixnum.getValue();
            if (i > Integer.MAX_VALUE || i < Integer.MIN_VALUE) return context.nil;
            index = (int) i;
        } else {
            StringSites sites = sites(context);
            if (RubyRange.isRangeLike(context, idx, sites.respond_to_begin, sites.respond_to_end)) {
                RubyRange range = RubyRange.rangeFromRangeLike(context, idx, sites.begin, sites.end, sites.exclude_end);

                int[] begLen = range.begLenInt(context, getByteList().length(), 0);
                return begLen == null ? context.nil : byteSubstr(context, begLen[0], begLen[1]);
            } else {
                index = toInt(context, idx);
            }
        }

        IRubyObject obj = byteSubstr(context, index, 1);
        if (obj.isNil() || ((RubyString)obj).getByteList().length() == 0) return context.nil;
        return obj;
    }

    @Deprecated(since = "10.0.0.0")
    public final IRubyObject substr19(Ruby runtime, int beg, int len) {
        return substrEnc(getCurrentContext(), beg, len);
    }

    @Deprecated(since = "10.0.0.0")
    public final IRubyObject substrEnc(Ruby runtime, int beg, int len) {
        return substrEnc(getCurrentContext(), beg, len);
    }

    public final IRubyObject substrEnc(ThreadContext context, int beg, int len) {
        if (len < 0) return context.nil;
        int length = value.getRealSize();
        if (length == 0) len = 0;

        Encoding enc = value.getEncoding();
        if (singleByteOptimizable(enc)) {
            if (beg > length) return context.nil;
            if (beg < 0) {
                beg += length;
                if (beg < 0) return context.nil;
            }
            if (beg + len > length) len = length - beg;
            if (len <= 0) len = beg = 0;
            return makeSharedString(context.runtime, beg, len);
        } else {
            if (beg + len > length) len = length - beg;
            return multibyteSubstr(context.runtime, enc, len, beg, length);
        }
    }

    private IRubyObject multibyteSubstr(Ruby runtime, Encoding enc, int len, int beg, int length) {
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
                return makeSharedString(runtime, p - s, e - p);
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
        } else if ((p = nth(enc, bytes, s, end, beg)) == end) {
            len = 0;
        } else {
            len = offset(enc, bytes, p, end, len);
        }
        return makeSharedString(runtime, p - s, len);
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
        } else if ((p = nth(enc, bytes, s, end, beg)) == end) {
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
        return this;
    }

    @Deprecated(since = "10.0.0.0")
    private void replaceInternal19(Ruby runtime, int beg, int len, RubyString repl) {
        strUpdate(getCurrentContext(), beg, len, this, repl);
    }

    /** rb_str_aref, rb_str_aref_m
     *
     */
    @JRubyMethod(name = {"[]", "slice"}, writes = BACKREF)
    public IRubyObject op_aref(ThreadContext context, IRubyObject arg) {
        if (arg instanceof RubyFixnum fixnum) {
            return op_aref(context, fixnum.asInt(context));
        } else if (arg instanceof RubyRegexp regexp) {
            return subpat(context, regexp);
        } else if (arg instanceof RubyString str) {
            return StringSupport.index(this, str, 0, this.checkEncoding(str)) != -1 ? dupString(context, str) : context.nil;
        } else if (arg instanceof RubyRange range) {
            int[] begLen = range.begLenInt(context, strLength(), 0);
            return begLen == null ? context.nil : substrEnc(context, begLen[0], begLen[1]);
        } else {
            StringSites sites = sites(context);
            if (RubyRange.isRangeLike(context, arg, sites.respond_to_begin, sites.respond_to_end)) {
                int len = strLength();
                RubyRange range = RubyRange.rangeFromRangeLike(context, arg, sites.begin, sites.end, sites.exclude_end);

                int[] begLen = range.begLenInt(context, len, 0);
                return begLen == null ? context.nil : substrEnc(context, begLen[0], begLen[1]);
            }
        }
        return op_aref(context, toInt(context, arg));
    }

    @JRubyMethod(name = {"[]", "slice"}, writes = BACKREF)
    public IRubyObject op_aref(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        return arg1 instanceof RubyRegexp regexp ?
                subpat(context, regexp, arg2) : substrEnc(context, toInt(context, arg1), toInt(context, arg2));
    }

    @JRubyMethod
    public IRubyObject byteslice(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        return byteSubstr(context, toLong(context, arg1), toLong(context, arg2));
    }

    @JRubyMethod
    public IRubyObject byteslice(ThreadContext context, IRubyObject arg) {
        return byteARef(context, arg);
    }

    @JRubyMethod(required = 2, optional = 3, checkArity = false)
    public IRubyObject bytesplice(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 2:
                return bytesplice(context, args[0], args[1]);
            case 3:
                return bytesplice(context, args[0], args[1], args[2]);
            case 5:
                break;
            default:
                throw argumentError(context, "wrong number of arguments (given " + args.length + ", expected 2, 3, or 5)");
        }

        int[] beglen = {toInt(context, args[0]), toInt(context, args[1])};
        RubyString val = args[2].convertToString();
        int[] vbegvlen = {toInt(context, args[3]), toInt(context, args[4])};

        checkBegLen(context, beglen);
        val.checkBegLen(context, vbegvlen);

        return bytespliceCommon(val, beglen[0], beglen[1], vbegvlen[0], vbegvlen[1]);
    }

    @JRubyMethod
    public IRubyObject bytesplice(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        int[] beglen = new int[2];
        if (!RubyRange.rangeBeginLength(context, arg0, value.realSize(), beglen, 2).isTrue()) {
            throw typeError(context, arg0, "Range");
        }
        checkBegLen(context, beglen);

        RubyString val = arg1.convertToString();

        int vbeg = 0;
        int vlen = val.getByteList().realSize();

        // simpler check for vbeg, vlen since we know the indices are valid
        val.checkBegLenPositions(context, vbeg, vlen);

        return bytespliceCommon(val, beglen[0], beglen[1], vbeg, vlen);
    }

    @JRubyMethod
    public IRubyObject bytesplice(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return arg0 instanceof RubyInteger ?
                bytespliceOffsets(context, arg0, arg1, arg2) : bytespliceRange(context, arg0, arg1, arg2);
    }

    /* bytesplice(index, length, str) */
    private RubyString bytespliceOffsets(ThreadContext context, IRubyObject index, IRubyObject length, IRubyObject str) {
        int[] beglen = new int[] {toInt(context, index), toInt(context, length)};
        checkBegLen(context, beglen);

        RubyString val = str.convertToString();
        int vbeg = 0;
        int vlen = val.getByteList().realSize();
        // simpler check for vbeg, vlen since we know the indices are valid
        val.checkBegLenPositions(context, vbeg, vlen);

        return bytespliceCommon(val, beglen[0], beglen[1], vbeg, vlen);
    }

    /* bytesplice(range, str, str_range) */
    private RubyString bytespliceRange(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        int[] beglen = new int[2];

        if (!RubyRange.rangeBeginLength(context, arg0, value.realSize(), beglen, 2).isTrue()) {
            throw typeError(context, arg0, rangeClass(context));
        }
        checkBegLen(context, beglen);

        RubyString val = arg1.convertToString();

        int[] vbegvlen = new int[2];
        if (!RubyRange.rangeBeginLength(context, arg2, val.getByteList().realSize(), vbegvlen, 2).isTrue()) {
            throw typeError(context, arg2, rangeClass(context));
        }
        val.checkBegLen(context, vbegvlen);

        return bytespliceCommon(val, beglen[0], beglen[1], vbegvlen[0], vbegvlen[1]);
    }

    private RubyString bytespliceCommon(RubyString val, int beg, int len, int vbeg, int vlen) {
        Encoding enc = checkEncoding(val);

        modifyAndKeepCodeRange();

        strUpdate1(beg, len, val, vbeg, vlen);
        setEncoding(enc);

        int cr = coderangeAnd(getCodeRange(), val.getCodeRange());
        if (cr != CR_BROKEN) {
            setCodeRange(cr);
        }

        return this;
    }

    // MRI: ENC_CODERANGE_AND, RB_ENC_CODERANGE_AND
    int coderangeAnd(int a, int b) {
        if (a == CR_7BIT) return b;
        if (a != CR_VALID) return CR_UNKNOWN;
        if (b == CR_7BIT) return CR_VALID;

        return b;
    }

    // MRI: str_check_beg_len
    private void checkBegLen(ThreadContext context, int[] beglen) {
        int len = checkLength(context, beglen[1]);
        int slen = size();
        int beg = beglen[0];

        if (slen < beg || (beg < 0 && beg + slen < 0)) throw indexError(context, "index " + beg + " out of string");

        if (beg < 0) {
            beg += slen;
            beglen[0] = beg;
        }

        assert (beg >= 0);
        assert (beg <= slen);

        if (len > slen - beg) {
            len = slen - beg;
            beglen[1] = len;
        }

        checkBegLenPositions(context, beg, len);
    }

    // MRI: lighter version of str_check_beg_len when we know the offsets are valid already
    private void checkBegLenPositions(ThreadContext context, int beg, int len) {
        int end = beg + len;
        ensureBytePosition(context, beg);
        ensureBytePosition(context, end);
    }

    // MRI: str_ensure_byte_pos
    private void ensureBytePosition(ThreadContext context, int pos) {
        ByteList byteList = getByteList();
        byte[] bytes = byteList.unsafeBytes();
        int s = byteList.begin();
        int e = s + byteList.realSize();
        int p = s + pos;

        if (!atCharBoundary(bytes, s, p, e, getEncoding())) {
            throw indexError(context, "offset " + pos + " does not land on character boundary");
        }
    }

    // MRI: at_char_boundary
    public static boolean atCharBoundary(byte[] bytes, int s, int p, int e, Encoding enc) {
        // our version checks if p == bytes.length, where CRuby would have a \0 character and not reposition
        return p == bytes.length || enc.leftAdjustCharHead(bytes, s, p, e) == p;
    }

    // MRI: rb_str_update_1
    private void strUpdate1(int beg, int len, RubyString val, int vbeg, int vlen) {
        if (beg == 0 && vlen == 0) {
            dropBytes(len);
            return;
        }

        modifyAndKeepCodeRange();
        ByteList byteList = value;
        byte[] sbytes = byteList.unsafeBytes();
        int sptr = value.begin();
        int slen = value.realSize();
        if (len < vlen) {
            /* expand string */
            byteList.ensure(slen + vlen - len);
            sbytes = byteList.unsafeBytes();
        }

        int cr = isCodeRangeAsciiOnly() ? val.getCodeRange() : CR_UNKNOWN;

        if (vlen != len) System.arraycopy(sbytes, sptr + beg + len, sbytes, sptr + beg + vlen, slen - (beg + len));
        if (vlen < beg && len < 0) Arrays.fill(sbytes, sptr + slen, sptr + slen + (-len), (byte) 0);
        if (vlen > 0) {
            ByteList valByteList = val.getByteList();
            System.arraycopy(valByteList.unsafeBytes(), valByteList.begin() + vbeg, sbytes, sptr + beg, vlen);
        }
        slen += vlen - len;
        byteList.realSize(slen);
        setCodeRange(cr);
    }

    private IRubyObject op_aref(ThreadContext context, int idx) {
        IRubyObject str = substrEnc(context, idx, 1);
        return !str.isNil() && ((RubyString) str).value.getRealSize() == 0 ? context.nil : str;
    }

    private int subpatSetCheck(ThreadContext context, int nth, Region regs) {
        int numRegs = regs == null ? 1 : regs.getNumRegs();
        if (nth < numRegs) {
            if (nth >= 0) return nth;
            if (-nth < numRegs) return nth + numRegs;
        }
        throw indexError(context, "index " + nth + " out of regexp");
    }

    private void subpatSet(ThreadContext context, RubyRegexp regexp, IRubyObject backref, IRubyObject repl) {
        int result = regexp.searchString(context, this, 0, false);

        if (result < 0) throw indexError(context, "regexp not matched");

        // this cast should be ok, since nil matchdata will be < 0 above
        RubyMatchData match = context.getLocalMatch();

        int nth = backref == null ? 0 : subpatSetCheck(context, match.backrefNumber(context, backref), match.regs);

        final int start, end;
        if (match.regs == null) {
            start = match.begin;
            end = match.end;
        } else {
            start = match.regs.getBeg(nth);
            end = match.regs.getEnd(nth);
        }
        if (start == -1) throw indexError(context, "regexp group " + nth + " not matched");
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

            return RubyRegexp.nth_match(context, match.backrefNumber(context, backref), match);
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

            return RubyRegexp.nth_match(context, 0, match);
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
        if (arg0 instanceof RubyFixnum fixnum) return op_aset(context, fixnum.asInt(context), arg1);
        if (arg0 instanceof RubyRegexp regexp) {
            subpatSet(context, regexp, null, arg1);
            return arg1;
        } else if (arg0 instanceof RubyString orig) {
            int beg = StringSupport.index(this, orig, 0, checkEncoding(orig));
            if (beg < 0) throw indexError(context, "string not matched");
            beg = subLength(beg);
            int len = orig.strLength();
            strUpdate(context, beg, len, this, arg1.convertToString());
            return arg1;
        } else if (arg0 instanceof RubyRange range) {
            int[] begLen = range.begLenInt(context, strLength(), 2);
            strUpdate(context, begLen[0], begLen[1], this, arg1.convertToString());
            return arg1;
        } else {
            StringSites sites = sites(context);
            if (RubyRange.isRangeLike(context, arg0, sites.respond_to_begin, sites.respond_to_end)) {
                RubyRange rng = RubyRange.rangeFromRangeLike(context, arg0, sites.begin, sites.end, sites.exclude_end);
                int[] begLen = rng.begLenInt(context, strLength(), 2);
                strUpdate(context, begLen[0], begLen[1], this, arg1.convertToString());

                return arg1;
            }
        }
        return op_aset(context, toInt(context, arg0), arg1);
    }

    private IRubyObject op_aset(ThreadContext context, int idx, IRubyObject arg1) {
        strUpdate(context, idx, 1, this, arg1.convertToString());
        return arg1;
    }

    @JRubyMethod(name = "[]=", writes = BACKREF)
    public IRubyObject op_aset(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        if (arg0 instanceof RubyRegexp regexp) {
            subpatSet(context, regexp, arg1, arg2);
        } else {
            int beg = toInt(context, arg0);
            int len = checkLength(context, toInt(context, arg1));
            strUpdate(context, beg, len, this, arg2.convertToString());
        }
        return arg2;
    }

    /** rb_str_slice_bang
     *
     */
    @JRubyMethod(name = "slice!", writes = BACKREF)
    public IRubyObject slice_bang(ThreadContext context, IRubyObject arg0) {
        IRubyObject result = op_aref(context, arg0);
        if (result.isNil()) {
            modifyCheck(); // keep cr ?
        } else {
            op_aset(context, arg0, Create.newEmptyString(context));
        }
        return result;
    }

    @JRubyMethod(name = "slice!", writes = BACKREF)
    public IRubyObject slice_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        IRubyObject result = op_aref(context, arg0, arg1);
        if (result.isNil()) {
            modifyCheck(); // keep cr ?
        } else {
            op_aset(context, arg0, arg1, Create.newEmptyString(context));
        }
        return result;
    }

    @JRubyMethod(name = {"succ", "next"})
    public IRubyObject succ(ThreadContext context) {
        return value.getRealSize() > 0 ?
                Create.newString(context, StringSupport.succCommon(context, value)) :
                Create.newEmptyString(context, value.getEncoding());
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject succ_bang() {
        return succ_bang(getCurrentContext());
    }

    @JRubyMethod(name = {"succ!", "next!"})
    public IRubyObject succ_bang(ThreadContext context) {
        modifyCheck();
        if (value.getRealSize() > 0) {
            value = StringSupport.succCommon(context, value);
            shareLevel = SHARE_LEVEL_NONE;
            // TODO: rescan code range ?
        }
        return this;
    }

    /** rb_str_upto_m
     *
     */
    @JRubyMethod(name = "upto")
    public final IRubyObject upto(ThreadContext context, IRubyObject end, Block block) {
        return block.isGiven() ? uptoCommon(context, end, false, block) : enumeratorize(context.runtime, this, "upto", end);
    }

    @JRubyMethod(name = "upto")
    public final IRubyObject upto(ThreadContext context, IRubyObject end, IRubyObject excl, Block block) {
        return block.isGiven() ? uptoCommon(context, end, excl.isTrue(), block) :
            enumeratorize(context.runtime, this, "upto", new IRubyObject[]{end, excl});
    }

    final IRubyObject uptoCommon(ThreadContext context, IRubyObject arg, boolean excl, Block block) {
        if (arg instanceof RubySymbol) throw typeError(context, "can't convert Symbol into String");
        return uptoCommon(context, arg.convertToString(), excl, block, false);
    }

    final IRubyObject uptoCommon(ThreadContext context, RubyString end, boolean excl, Block block, boolean asSymbol) {
        Encoding enc = checkEncoding(end);
        boolean isAscii = scanForCodeRange() == CR_7BIT && end.scanForCodeRange() == CR_7BIT;
        if (value.getRealSize() == 1 && end.value.getRealSize() == 1 && isAscii) {
            byte c = value.getUnsafeBytes()[value.getBegin()];
            byte e = end.value.getUnsafeBytes()[end.value.getBegin()];
            if (c > e || (excl && c == e)) return this;
            while (true) {
                ByteList s = RubyInteger.singleCharByteList(c);
                block.yield(context, asSymbol ? asSymbol(context, s) : newStringShared(context.runtime, s, enc, CR_7BIT));

                if (!excl && c == e) break;
                c++;
                if (excl && c == e) break;
                context.pollThreadEvents();
            }
            return this;
        } else if (isAscii && ASCII.isDigit(value.getUnsafeBytes()[value.getBegin()]) && ASCII.isDigit(end.value.getUnsafeBytes()[end.value.getBegin()])) {
            int s = value.getBegin();
            int send = s + value.getRealSize();
            byte[]bytes = value.getUnsafeBytes();

            while (s < send) {
                if (!ASCII.isDigit(bytes[s] & 0xff)) return uptoCommonNoDigits(context, end, excl, block, asSymbol);
                s++;
                context.pollThreadEvents();
            }
            s = end.value.getBegin();
            send = s + end.value.getRealSize();
            bytes = end.value.getUnsafeBytes();

            while (s < send) {
                if (!ASCII.isDigit(bytes[s] & 0xff)) return uptoCommonNoDigits(context, end, excl, block, asSymbol);
                s++;
                context.pollThreadEvents();
            }

            IRubyObject b = stringToInum(10);
            IRubyObject e = end.stringToInum(10);

            RubyArray argsArr = newArray(context, asFixnum(context, value.length()), context.nil);

            if (b instanceof RubyFixnum bb && e instanceof RubyFixnum ee) {
                long bl = bb.getValue();
                long el = ee.getValue();

                while (bl <= el) {
                    if (excl && bl == el) break;
                    argsArr.eltSetOk(1, asFixnum(context, bl));
                    ByteList to = new ByteList(value.length() + 5);
                    Sprintf.sprintf(to, "%.*d", argsArr);
                    RubyString str = RubyString.newStringNoCopy(context.runtime, to, USASCIIEncoding.INSTANCE, CR_7BIT);
                    block.yield(context, asSymbol ? asSymbol(context, str.toString()) : str);
                    bl++;
                    context.pollThreadEvents();
                }
            } else {
                StringSites sites = sites(context);
                CallSite op = excl ? sites.op_lt : sites.op_le;

                while (op.call(context, b, b, e).isTrue()) {
                    argsArr.eltSetOk(1, b);
                    ByteList to = new ByteList(value.length() + 5);
                    Sprintf.sprintf(to, "%.*d", argsArr);
                    RubyString str = RubyString.newStringNoCopy(context.runtime, to, USASCIIEncoding.INSTANCE, CR_7BIT);
                    block.yield(context, asSymbol ? asSymbol(context, str.toString()) : str);
                    b = sites.succ.call(context, b, b);
                    context.pollThreadEvents();
                }
            }
            return this;
        }

        return uptoCommonNoDigits(context, end, excl, block, asSymbol);
    }

    private IRubyObject uptoCommonNoDigits(ThreadContext context, RubyString end, boolean excl, Block block, boolean asSymbol) {
        int n = op_cmp(end);
        if (n > 0 || (excl && n == 0)) return this;
        StringSites sites = sites(context);
        CallSite succ = sites.succ;
        IRubyObject afterEnd = succ.call(context, end, end);
        RubyString current = dupString(context, this);

        while (!current.op_equal(context, afterEnd).isTrue()) {
            IRubyObject next = null;
            if (excl || !current.op_equal(context, end).isTrue()) next = succ.call(context, current, current);
            block.yield(context, asSymbol ? asSymbol(context, current.toString()) : current);
            if (next == null) break;
            current = next.convertToString();
            if (excl && current.op_equal(context, end).isTrue()) break;
            if (current.getByteList().length() > end.getByteList().length() || current.getByteList().isEmpty()) break;
            context.pollThreadEvents();
        }
        return this;
    }

    final IRubyObject uptoEndless(ThreadContext context, Block block) {
        StringSites sites = sites(context);
        CallSite succ = sites.succ;

        boolean isAscii = scanForCodeRange() == CR_7BIT;
        RubyString current = dupString(context, this);

        if (isAscii && ASCII.isDigit(value.getUnsafeBytes()[value.getBegin()])) {
            IRubyObject b = stringToInum(10);
            RubyArray argsArr = newArray(context, asFixnum(context, value.length()), context.nil);
            ByteList to;

            if (b instanceof RubyFixnum bb) {
                long bl = bb.getValue();

                while (bl < RubyFixnum.MAX) {
                    argsArr.eltSetOk(1, asFixnum(context, bl));
                    to = new ByteList(value.length() + 5);
                    Sprintf.sprintf(to, "%.*d", argsArr);
                    current = RubyString.newStringNoCopy(context.runtime, to, USASCIIEncoding.INSTANCE, CR_7BIT);
                    block.yield(context, current);
                    bl++;
                    context.pollThreadEvents();
                }

                argsArr.eltSetOk(1, asFixnum(context, bl));
                to = new ByteList(value.length() + 5);
                Sprintf.sprintf(to, "%.*d", argsArr);
                current = RubyString.newStringNoCopy(context.runtime, to, USASCIIEncoding.INSTANCE, CR_7BIT);
            }
        }

        while (true) {
            IRubyObject next = succ.call(context, current, current);
            block.yield(context, current);
            if (next == null) break;
            current = next.convertToString();
            if (current.getByteList().isEmpty()) break;
            context.pollThreadEvents();
        }

        return this;
    }

    /** rb_str_include
     *
     */
    @JRubyMethod(name = "include?")
    public RubyBoolean include_p(ThreadContext context, IRubyObject obj) {
        RubyString coerced = obj.convertToString();
        return asBoolean(context, StringSupport.index(this, coerced, 0, this.checkEncoding(coerced)) != -1);
    }

    @JRubyMethod
    public IRubyObject chr(ThreadContext context) {
        return substrEnc(context, 0, 1);
    }

    @JRubyMethod
    public IRubyObject getbyte(ThreadContext context, IRubyObject index) {
        int i = toInt(context, index);
        if (i < 0) i += value.getRealSize();
        if (i < 0 || i >= value.getRealSize()) return context.nil;
        return asFixnum(context, value.getUnsafeBytes()[value.getBegin() + i] & 0xff);
    }

    @JRubyMethod
    public IRubyObject setbyte(ThreadContext context, IRubyObject index, IRubyObject val) {
        int i = toInt(context, index);
        int normalizedIndex = checkIndexForRef(context, i, value.getRealSize());
        RubyInteger v = val.convertToInteger();
        IRubyObject w = v.modulo(context, (long)256);
        int b = toInt(context, w) & 0xff;

        modifyAndClearCodeRange();
        value.getUnsafeBytes()[normalizedIndex] = (byte)b;
        return val;
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject to_i() {
        return to_i(getCurrentContext());
    }

    // MRI: rb_str_to_i
    @JRubyMethod(name = "to_i")
    public IRubyObject to_i(ThreadContext context) {
        return stringToInum(10);
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject to_i(IRubyObject arg0) {
        return to_i(getCurrentContext(), arg0);
    }

    /** rb_str_to_i
     *
     */
    @JRubyMethod(name = "to_i")
    public IRubyObject to_i(ThreadContext context, IRubyObject arg0) {
        int base = toInt(context, arg0);
        if (base < 0) throw argumentError(context, "illegal radix " + base);
        return stringToInum(base);
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

    /** rb_str_oct
     *
     */
    @JRubyMethod(name = "oct")
    public IRubyObject oct(ThreadContext context) {
        return stringToInum(-8, false);
    }

    /** rb_str_hex
     *
     */
    @JRubyMethod(name = "hex")
    public IRubyObject hex(ThreadContext context) {
        return stringToInum(16, false);
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject to_f() {
        return to_f(getCurrentContext());
    }

    // MRI: rb_str_to_f
    @JRubyMethod(name = "to_f")
    public IRubyObject to_f(ThreadContext context) {
        return RubyNumeric.str2fnum(context.runtime, this, false);
    }

    private void populateCapturesForSplit(ThreadContext context, RubyArray result, RubyMatchData match) {
        for (int i = 1; i < match.numRegs(); i++) {
            int beg = match.begin(i);
            if (beg == -1) continue;
            result.append(context, makeSharedString(context.runtime, beg, match.end(i) - beg));
        }
    }

    public RubyArray split(ThreadContext context) {
        return split(context, context.nil);
    }

    public RubyArray split(ThreadContext context, IRubyObject arg0) {
        return splitCommon(context, arg0, 0);
    }

    /** rb_str_split_m
     *
     */
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

    public RubyArray split(ThreadContext context, IRubyObject pattern, IRubyObject limit) {
        return splitCommon(context, pattern, toInt(context, limit));
    }

    @JRubyMethod(name = "split")
    public IRubyObject splitWithBlock(ThreadContext context, IRubyObject pattern, IRubyObject limit, Block block) {
        RubyArray array = split(context, pattern, limit);

        if (!block.isGiven()) return array;

        for (int i = 0; i < array.getLength(); i++) {
            block.yield(context, array.eltOk(i));
        }

        return this;
    }

    /**
     * Split for ext (Java) callers (does not write $~).
     * @param delimiter
     * @return splited entries
     */
    @Deprecated(since = "10.0.0.0")
    public RubyArray split(RubyRegexp delimiter) {
        return splitCommon(getCurrentContext(), delimiter, 0);
    }

    @Deprecated(since = "10.0.0.0")
    public RubyArray split(RubyRegexp delimiter, int limit) {
        return split(getCurrentContext(), delimiter, limit);
    }

    /**
     * Split for ext (Java) callers (does not write $~).
     * @param context the thread context
     * @param delimiter
     * @param limit
     * @return splited entries
     */
    @JRubyAPI
    public RubyArray split(ThreadContext context, RubyRegexp delimiter, int limit) {
        return splitCommon(context, delimiter, limit);
    }

    @Deprecated(since = "10.0.0.0")
    public RubyArray split(RubyString delimiter) {
        return splitCommon(getCurrentContext(), delimiter, 0);
    }

    @Deprecated(since = "10.0.0.0")
    public RubyArray split(RubyString delimiter, int limit) {
        return split(getCurrentContext(), delimiter, limit);
    }

    /**
     * Split for ext (Java) callers (does not write $~).
     * @param context the thread context
     * @param delimiter
     * @param limit
     * @return splited entries
     */
    @JRubyAPI
    public RubyArray split(ThreadContext context, RubyString delimiter, int limit) {
        return splitCommon(context, delimiter, limit);
    }

    // MRI: rb_str_split_m, overall structure
    private RubyArray splitCommon(ThreadContext context, IRubyObject pat, int lim) {
        // limit of 1 is the whole value.
        if (lim == 1) return value.isEmpty() ? newArray(context) : newArray(context, dupString(context, this));

        boolean limit = lim > 0; // We have an explicit number of values we want to split into.
        RubyArray<?> result;
        Object splitPattern = determineSplitPattern(context, pat);

        if (splitPattern == context.nil) {                  // AWK SPLIT
            result = awkSplit(context, limit, lim);
        } else if (splitPattern instanceof ByteList bytelist) {      // OPTIMIZED STRING SPLIT
            // no need to check for broken strings because both have to be 7 bit to be at this point.
            switch (bytelist.realSize()) {
                case 0:                                     // CHARS SPLIT
                    RubyRegexp pattern = RubyRegexp.newRegexp(context.runtime, (ByteList) splitPattern, 0);
                    result = regexSplit(context, pattern, limit, lim);
                    break;
                case 1:                                     // SINGLE CHAR SPLIT
                    result = asciiStringSplitOne(context, (byte) ((ByteList) splitPattern).charAt(0), limit, lim);
                    break;
                default:                                    // MULTIPLE CHAR SPLIT
                    result = asciiStringSplit(context, (ByteList) splitPattern, limit, lim);
                    break;
            }
        } else if (splitPattern instanceof RubyRegexp regexp) {   // REGEXP SPLIT
            result = regexSplit(context, regexp, limit, lim);
        } else {                                           // STRING SPLIT
            RubyString splitString = (RubyString) splitPattern;
            ((RubyString)splitPattern).mustnotBroken(context);

            if (splitString.isEmpty()) { // SPLIT_TYPE_CHARS
                RubyRegexp pattern = RubyRegexp.newRegexpFromStr(context.runtime, splitString, 0);
                result = regexSplit(context, pattern, limit, lim);
            } else {
                ByteList spatValue = ((RubyString)splitPattern).value;
                int len = spatValue.getRealSize();
                Encoding spatEnc = spatValue.getEncoding();
                final int c;
                byte[]bytes = spatValue.getUnsafeBytes();
                int p = spatValue.getBegin();
                if (spatEnc.isAsciiCompatible()) {
                    c = len == 1 ? bytes[p] & 0xff : -1;
                } else {
                    c = len == StringSupport.preciseLength(spatEnc, bytes, p, p + len) ? spatEnc.mbcToCode(bytes, p, p + len) : -1;
                }
                if (c == ' ') {
                    result = awkSplit(context, limit, lim);
                } else {
                    result = stringSplit(context, splitString, limit, lim);
                }
            }
        }

        if (!limit && lim == 0) {
            while (!result.isEmpty() && ((RubyString) result.eltInternal(result.size() - 1)).value.getRealSize() == 0) {
                result.pop(context);
            }
        }

        return result;
    }

    // Determine split pattern we will use to split.  For example, his may decide that a regexp
    // can be a string or awk split.  instanceof check will determine the type of split we perform:
    //   nil == AWK
    //   ByteList == Optimized String Split
    //   RubyString == String Split
    //   RubyRegexp == Regexp
    private Object determineSplitPattern(ThreadContext context, IRubyObject pat) {
        Object pattern;
        if (!pat.isNil()) {
            pattern = getPatternQuoted(context, pat);
        } else {
            IRubyObject splitPattern = globalVariables(context).get("$;");

            if (splitPattern.isNil()) return context.nil;

            warnDeprecated(context, "$; is set to non-nil value");

            pattern = splitPattern;
        }

        if (pattern instanceof RubyRegexp regexp) {
            if (regexp.isSimpleString(context)) return regexp.rawSource(); // Simple string-only regexp use Optimized String split
            return regexp;
        }

        RubyString stringPattern = (RubyString) pattern;

        if (isAsciiOnly() && stringPattern.isAsciiOnly()) { // Optimized String Split
            if (stringPattern.length() != 1 || stringPattern.getByteList().charAt(0) != ' ') {
                return stringPattern.getByteList();
            }
        }

        // String
        return stringPattern;
    }

    /**
     * Call regexpSplit using a thread-local backref holder to avoid cross-thread pollution.
     */
    private RubyArray regexSplit(ThreadContext context, RubyRegexp pattern, boolean limit, int lim) {
        var result = newArray(context);
        int ptr = value.getBegin();
        int len = value.getRealSize();
        byte[] bytes = value.getUnsafeBytes();
        Encoding enc = value.getEncoding();

        boolean captures = pattern.getPattern(context).numberOfCaptures() != 0;

        int end, beg = 0;
        boolean lastNull = false;
        int start = beg;
        int i = 1;
        while (pattern.searchString(context, this, start, false) >= 0) {
            RubyMatchData match = context.getLocalMatch();
            end = match.begin(0);
            if (start == end && match.begin(0) == match.end(0)) {
                if (len == 0 && start != 0) {
                    result.append(context, newEmptyString(context.runtime, metaClass));
                    break;
                } else if (lastNull) {
                    result.append(context, makeSharedString(context.runtime, beg, StringSupport.length(enc, bytes, ptr + beg, ptr + len)));
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
                result.append(context, makeSharedString(context.runtime, beg, end - beg));
                beg = match.end(0);
                start = beg;
            }
            lastNull = false;

            if (captures) populateCapturesForSplit(context, result, match);
            if (limit && lim <= ++i) break;
        }

        if (len > 0 && (limit || len > beg || lim < 0)) result.append(context, makeSharedString(context.runtime, beg, len - beg));

        return result;
    }

    // MRI: rb_str_split_m, when split_type = awk
    private RubyArray awkSplit(final ThreadContext context, boolean limit, int lim) {
        var result = newArray(context);

        byte[]bytes = value.getUnsafeBytes();
        int p = value.getBegin();
        int ptr = p;
        int len = value.getRealSize();
        int end = p + len;
        Encoding enc = value.getEncoding();
        boolean skip = true;
        int i = 1;

        int e = 0, b = 0;
        boolean singlebyte = singleByteOptimizable(enc);
        while (p < end) {
            final int c;
            if (singlebyte) {
                c = bytes[p++] & 0xff;
            } else {
                c = codePoint(context, enc, bytes, p, end);
                p += StringSupport.length(enc, bytes, p, end);
            }

            if (skip) {
                // MRI uses rb_isspace
                if (ASCII.isSpace(c)) {
                    b = p - ptr;
                } else {
                    e = p - ptr;
                    skip = false;
                    if (limit && lim <= i) break;
                }
            } else {
                // MRI uses rb_isspace
                if (ASCII.isSpace(c)) {
                    result.append(context, makeSharedString(context.runtime, b, e - b));
                    skip = true;
                    b = p - ptr;
                    if (limit) i++;
                } else {
                    e = p - ptr;
                }
            }
        }

        if (len > 0 && (limit || len > b || lim < 0)) result.append(context, makeSharedString(context.runtime, b, len - b));
        return result;
    }

    private RubyArray asciiStringSplitOne(ThreadContext context, byte pat, boolean limit, int lim) {
        var result = newArray(context);
        int realSize = value.getRealSize();

        if (realSize == 0) return result;

        byte[] bytes = value.getUnsafeBytes();
        int begin = value.getBegin();

        int startSegment = 0; // start index of currently processed segment in split
        int index = 0;
        int i = 1;

        for (; index < realSize; index++) {
            if (bytes[begin + index] == pat) {
                result.append(context, makeSharedString(context.runtime, startSegment, index - startSegment));
                startSegment = index + 1;
                if (limit && lim <= ++i) break;
            }
        }

        if (limit) {
            result.append(context, makeSharedString(context.runtime, startSegment, realSize - startSegment));
        } else if (index > startSegment || lim < 0) {
            result.append(context, makeSharedString(context.runtime, startSegment, index - startSegment));
        }

        return result;
    }

    // Only for use with two clean 7BIT ASCII strings.
    private RubyArray asciiStringSplit(ThreadContext context, ByteList pattern, boolean limit, int lim) {
        var result = newArray(context);

        byte[] patternBytes = pattern.getUnsafeBytes();
        int patternBegin = pattern.getBegin();
        int patternRealSize = pattern.getRealSize();

        byte[] bytes = value.getUnsafeBytes();
        int begin = value.getBegin();
        int realSize = value.getRealSize();

        int e, p = 0;
        int i = 1;

        while (p < realSize && (e = asciiIndexOf(bytes, begin, realSize, patternBytes, patternBegin, patternRealSize, p)) >= 0) {
            result.append(context, makeSharedString(context.runtime, p, e - p));
            p = e + pattern.getRealSize();
            if (limit && lim <= ++i) break;
        }

        if (realSize > 0 && (limit || realSize > p || lim < 0)) {
            result.append(context, makeSharedString(context.runtime, p, realSize - p));
        }

        return result;
    }

    // MRI: rb_str_split_m, when split_type = string
    private RubyArray stringSplit(ThreadContext context, RubyString spat, boolean limit, int lim) {
        mustnotBroken(context);

        var result = newArray(context);
        Encoding enc = checkEncoding(spat);
        ByteList pattern = spat.value;

        byte[] patternBytes = pattern.getUnsafeBytes();
        int patternBegin = pattern.getBegin();
        int patternRealSize = pattern.getRealSize();

        byte[] bytes = value.getUnsafeBytes();
        int begin = value.getBegin();
        int realSize = value.getRealSize();

        int e, p = 0;
        int i = 1;

        while (p < realSize && (e = indexOf(bytes, begin, realSize, patternBytes, patternBegin, patternRealSize, p, enc)) >= 0) {
            int t = enc.rightAdjustCharHead(bytes, p + begin, e + begin, begin + realSize) - begin;
            if (t != e) {
                p = t;
                continue;
            }
            result.append(context, makeSharedString(context.runtime, p, e - p));
            p = e + pattern.getRealSize();
            if (limit && lim <= ++i) break;
        }

        if (realSize > 0 && (limit || realSize > p || lim < 0)) {
            result.append(context, makeSharedString(context.runtime, p, realSize - p));
        }

        return result;
    }

    // TODO: make the ByteList version public and use it, rather than copying here
    static int asciiIndexOf(byte[] source, int sourceOffset, int sourceCount, byte[] target, int targetOffset, int targetCount, int fromIndex) {
        if (fromIndex >= sourceCount) return (targetCount == 0 ? sourceCount : -1);
        if (fromIndex < 0) fromIndex = 0;
        if (targetCount == 0) return fromIndex;

        byte first  = target[targetOffset];
        int max = sourceOffset + (sourceCount - targetCount);

        int i = sourceOffset + fromIndex;
        while (i <= max) {
            while (i <= max && source[i] != first)
                i += 1;

            if (i <= max) {
                int j = i + 1;
                int end = j + targetCount - 1;
                for (int k = targetOffset + 1; j < end && source[j] == target[k]; j++, k++);

                if (j == end) return i - sourceOffset;
                i += 1;
            }
        }
        return -1;
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

    private static RubyString getStringForPattern(ThreadContext context, IRubyObject obj) {
        if (obj instanceof RubyString str) return str;
        IRubyObject val = obj.checkStringType();
        if (val.isNil()) throw typeError(context, obj, "Regexp");
        return (RubyString) val;
    }

    /** get_pat (used by match/match19)
     *
     */
    private static RubyRegexp getPattern(ThreadContext context, IRubyObject obj) {
        if (obj instanceof RubyRegexp regexp) return regexp;
        return RubyRegexp.newRegexpFromStr(context.runtime, getStringForPattern(context, obj), 0);
    }

    // MRI: get_pat_quoted (split and part of scan logic)
    private static IRubyObject getPatternQuoted(ThreadContext context, IRubyObject pat) {
        if (pat instanceof RubyRegexp regexp) return regexp;
        if (pat instanceof RubyString) return pat;

        IRubyObject val = pat.checkStringType();
        if (val == context.nil) TypeConverter.checkType(context, pat, regexpClass(context));
        return val;
    }

    public RubyClass singletonClass(ThreadContext context) {
        if (isChilled()) {
            mutateChilledString();
        } else if (isFrozen()) {
            throw typeError(context, "can't define singleton");
        }

        return super.singletonClass(context);
    }

    // MRI: get_pat_quoted (scan error checking portion)
    private static Object getScanPatternQuoted(ThreadContext context, IRubyObject pat) {
        pat = getPatternQuoted(context, pat);

        if (pat instanceof RubyString str && str.isBrokenString()) {
            // MRI code does a raise of TypeError with a special regexp string constructor that raises RegexpError
            throw context.runtime.newRegexpError("invalid byte sequence in " + str.getEncoding());
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

        // FIXME: this may return a ByteList
        pat = (IRubyObject) getScanPatternQuoted(context, pat);
        mustnotBroken(context);
        if (!block.isGiven()) {
            RubyArray ary = null;
            while ((result = scanOnce(context, str, pat, startp)) != context.nil) {
                last = prev;
                prev = startp[0];
                if (ary == null) ary = allocArray(context, 4);
                ary.append(context, result);
            }
            if (last >= 0) patternSearch(context, pat, str, last);
            return ary == null ? newEmptyArray(context) : ary;
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
        if (scanForCodeRange() == CR_BROKEN) throw argumentError(context, "invalid byte sequence in " + getEncoding());
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
                return RubyRegexp.nth_match(context, 0, match);
            }
            int size = match.numRegs();
            RubyArray result = RubyArray.newBlankArrayInternal(context.runtime, size - 1);
            for (int i = 1; i < size; i++) {
                result.eltInternalSet(i - 1, RubyRegexp.nth_match(context, i, match));
            }
            result.realLength = size - 1;

            return result;
        }

        return context.nil;
    }

    // MRI: rb_pat_search
    private static int patternSearch(ThreadContext context, final IRubyObject pattern, RubyString str, final int pos) {
        if (pattern instanceof RubyString strPattern) {
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

        context.setBackRef(match);

        return match;
    }

    @JRubyMethod(name = "start_with?")
    public IRubyObject start_with_p(ThreadContext context) {
        return context.fals;
    }

    @JRubyMethod(name = "start_with?")
    public IRubyObject start_with_p(ThreadContext context, IRubyObject arg) {
        return asBoolean(context, arg instanceof RubyRegexp regexp ?
                regexp.startsWith(context, this) : startsWith(arg.convertToString()));
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
        return asBoolean(context, endWith(arg));
    }

    @JRubyMethod(name = "end_with?", rest = true)
    public IRubyObject end_with_p(ThreadContext context, IRubyObject[]args) {
        for (int i = 0; i < args.length; i++) {
            if (endWith(args[i])) return context.tru;
        }
        return context.fals;
    }

    // MRI: rb_str_end_with, loop body
    protected boolean endWith(IRubyObject tmp) {
        tmp = tmp.convertToString();
        ByteList tmpBL = ((RubyString)tmp).value;
        // MRI does not have this condition because starting at end of string can still dereference \0
        if (tmpBL.getRealSize() == 0) return true;
        Encoding enc = checkEncoding((RubyString)tmp);
        if (value.realSize() < tmpBL.realSize()) return false;
        int p = value.begin();
        int e = p + value.realSize();
        int s = e - tmpBL.realSize();
        if (!atCharacterBoundary(value.unsafeBytes(), p, e, s, enc)) return false;
        if (ByteList.memcmp(value.unsafeBytes(), s, tmpBL.unsafeBytes(), tmpBL.begin(), tmpBL.realSize()) == 0) {
            return true;
        }
        return false;
    }

    // MRI: at_char_boundary
    private static boolean atCharacterBoundary(byte[] bytes, int p, int e, int s, Encoding enc) {
        return s == e || enc.leftAdjustCharHead(bytes, p, s, e) == s;
    }

    public boolean endsWithAsciiChar(char c) {
        ByteList value = this.value;
        int size;

        return value.getEncoding().isAsciiCompatible() && (size = value.realSize()) > 0 && value.get(size - 1) == c;
    }

    @JRubyMethod(name = "delete_prefix")
    public IRubyObject delete_prefix(ThreadContext context, IRubyObject prefix) {
        int prefixlen = deletedPrefixLength(prefix);

        return prefixlen <= 0 ? dupString(context, this) : makeSharedString(context.runtime, prefixlen, size() - prefixlen);
    }

    @JRubyMethod(name = "delete_suffix")
    public IRubyObject delete_suffix(ThreadContext context, IRubyObject suffix) {
        int suffixlen = deletedSuffixLength(suffix);

        return suffixlen <= 0 ? dupString(context, this) : makeSharedString(context.runtime, 0, size() - suffixlen);
    }

    @JRubyMethod(name = "delete_prefix!")
    public IRubyObject delete_prefix_bang(ThreadContext context, IRubyObject prefix) {
        modifyAndKeepCodeRange();

        int prefixlen = deletedPrefixLength(prefix);

        if (prefixlen <= 0) return context.nil;

        dropBytes(prefixlen);

        return this;
    }

    // MRI: rb_str_drop_bytes, in a nutshell
    private void dropBytes(int prefixlen) {
        modify();
        value.view(prefixlen, value.realSize() - prefixlen);
        clearCodeRange();
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

    private IRubyObject justify(ThreadContext context, IRubyObject arg0, int jflag) {
        RubyString result = justifyCommon(context, SPACE_BYTELIST, 1, true, EncodingUtils.STR_ENC_GET(this), toInt(context, arg0), jflag);
        if (getCodeRange() != CR_BROKEN) result.setCodeRange(getCodeRange());
        return result;
    }

    private IRubyObject justify(ThreadContext context, IRubyObject arg0, IRubyObject arg1, int jflag) {
        RubyString padStr = arg1.convertToString();
        ByteList pad = padStr.value;
        Encoding enc = checkEncoding(padStr);
        int padCharLen = StringSupport.strLengthFromRubyString(padStr, enc);
        if (pad.getRealSize() == 0 || padCharLen == 0) throw argumentError(context, "zero width padding");
        int width = toInt(context, arg0);
        RubyString result = justifyCommon(context, pad,
                                                   padCharLen,
                                                   padStr.singleByteOptimizable(),
                                                   enc, width, jflag);
        if (result.strLength() > strLength()) {
        }
        int cr = CodeRangeSupport.codeRangeAnd(getCodeRange(), padStr.getCodeRange());
        if (cr != CR_BROKEN) result.setCodeRange(cr);
        return result;
    }

    private RubyString justifyCommon(ThreadContext context, ByteList pad, int padCharLen, boolean padSinglebyte,
                                     Encoding enc, int width, int jflag) {
        int len = StringSupport.strLengthFromRubyString(this, enc);
        if (width < 0 || len >= width) return dupString(context, this);
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
                int padPP = padSinglebyte ? padP + llen : nth(enc, padBytes, padP, padP + padLen, llen);
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
                int padPP = padSinglebyte ? padP + rlen : nth(enc, padBytes, padP, padP + padLen, rlen);
                n = padPP - padP;
                System.arraycopy(padBytes, padP, bytes, p, n);
                p += n;
                break;
            }
        }

        res.setRealSize(p);

        RubyString result = Create.newString(context, res);
        if (result.strLength() > strLength()) {
        }
        result.associateEncoding(enc);
        return result;
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject ljust(IRubyObject arg0) {
        return ljust(getCurrentContext(), arg0);
    }

    /** rb_str_ljust
     *
     */
    @JRubyMethod(name = "ljust")
    public IRubyObject ljust(ThreadContext context, IRubyObject arg0) {
        return justify(context, arg0, 'l');
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject ljust(IRubyObject arg0, IRubyObject arg1) {
        return ljust(getCurrentContext(), arg0, arg1);
    }

    @JRubyMethod(name = "ljust")
    public IRubyObject ljust(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return justify(context, arg0, arg1, 'l');
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject rjust(IRubyObject arg0) {
        return rjust(getCurrentContext(), arg0);
    }

    /** rb_str_rjust
     *
     */
    @JRubyMethod(name = "rjust")
    public IRubyObject rjust(ThreadContext context, IRubyObject arg0) {
        return justify(context, arg0, 'r');
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject rjust(IRubyObject arg0, IRubyObject arg1) {
        return rjust(getCurrentContext(), arg0, arg1);
    }

    @JRubyMethod(name = "rjust")
    public IRubyObject rjust(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return justify(context, arg0, arg1, 'r');
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject center(IRubyObject arg0) {
        return center(getCurrentContext(), arg0);
    }

    /** rb_str_center
     *
     */
    @JRubyMethod(name = "center")
    public IRubyObject center(ThreadContext context, IRubyObject arg0) {
        return justify(context, arg0, 'c');
    }


    @Deprecated(since = "10.0.0.0")
    public IRubyObject center(IRubyObject arg0, IRubyObject arg1) {
        return center(getCurrentContext(), arg0, arg1);
    }

    @JRubyMethod(name = "center")
    public IRubyObject center(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return justify(context, arg0, arg1, 'c');
    }

    @JRubyMethod(reads = BACKREF, writes = BACKREF)
    public IRubyObject partition(ThreadContext context, Block block) {
        return RubyEnumerable.partition(context, this, block);
    }

    @JRubyMethod(reads = BACKREF, writes = BACKREF)
    public IRubyObject partition(ThreadContext context, IRubyObject arg, Block block) {
        int pos;
        final RubyString sep;
        if (arg instanceof RubyRegexp regexp) {
            if (regexp.search(context, this, 0, false) < 0) return partitionMismatch(context);

            RubyMatchData match = context.getLocalMatch();
            pos = match.begin(0);
            sep = makeSharedString(context.runtime, pos, match.end(0) - pos);
        } else {
            IRubyObject tmp = arg.checkStringType();
            if (tmp.isNil()) throw typeError(context, "type mismatch: ", arg, " given");
            sep = (RubyString)tmp;
            pos = StringSupport.index(this, sep, 0, this.checkEncoding(sep));
            if (pos < 0) return partitionMismatch(context);
        }

        return newArrayNoCopy(context, makeSharedString(context.runtime, 0, pos), sep,
                makeSharedString(context.runtime, pos + sep.value.getRealSize(), value.getRealSize() - pos - sep.value.getRealSize()));
    }

    private RubyArray partitionMismatch(ThreadContext context) {
        final Encoding enc = getEncoding();
        return RubyArray.newArrayMayCopy(context.runtime, dupString(context, this),
                newEmptyString(context.runtime, enc), newEmptyString(context.runtime, enc));
    }

    @JRubyMethod(name = "rpartition", writes = BACKREF)
    public IRubyObject rpartition(ThreadContext context, IRubyObject arg) {
        final int pos;
        final RubyString sep;
        if (arg instanceof RubyRegexp) {
            IRubyObject tmp = rindex(context, arg);
            if (tmp.isNil()) return rpartitionMismatch(context);
            pos = toInt(context, tmp);
            sep = (RubyString)RubyRegexp.nth_match(context, 0, context.getLocalMatchOrNil());
        } else {
            IRubyObject tmp = arg.checkStringType();
            if (tmp.isNil()) throw typeError(context, "type mismatch: ", arg, " given");
            sep = (RubyString)tmp;
            pos = StringSupport.rindex(value, StringSupport.strLengthFromRubyString(this, this.checkEncoding(sep)), StringSupport.strLengthFromRubyString(sep, this.checkEncoding(sep)), subLength(value.getRealSize()), sep, this.checkEncoding(sep));
            if (pos < 0) return rpartitionMismatch(context);
        }

        int beg = pos + sep.strLength();
        int len = value.getRealSize();
        return newArrayNoCopy(context, substrEnc(context, 0, pos), sep, substrEnc(context, beg, len));
    }

    private IRubyObject rpartitionMismatch(ThreadContext context) {
        final Encoding enc = getEncoding();
        return newArrayNoCopy(context, newEmptyString(context.runtime, enc),
                newEmptyString(context.runtime, enc), dupString(context, this));
    }

    @JRubyMethod(rest = true)
    public IRubyObject append_as_bytes(ThreadContext context, IRubyObject[] args) {
        int neededCapacity = 0;
        for (var arg : args) neededCapacity += byteCapacityFor(context, arg);
        this.ensureAvailable(context, neededCapacity);
        for (var arg : args) appendBytes(context, arg);

        return this;
    }

    @JRubyMethod
    public IRubyObject append_as_bytes(ThreadContext context) {
        this.ensureAvailable(context, 0);

        return this;
    }

    @JRubyMethod
    public IRubyObject append_as_bytes(ThreadContext context, IRubyObject arg0) {
        ensureAvailable(context, byteCapacityFor(context, arg0));
        appendBytes(context, arg0);

        return this;
    }

    private static int byteCapacityFor(ThreadContext context, IRubyObject arg) {
        return switch (arg) {
            case RubyInteger ignored -> 1;
            case RubyString str -> str.getByteList().realSize();
            default ->
                    throw typeError(context, str(context.runtime, "wrong argument type ", types(context.runtime, arg.getType()), " (expected String or Integer)"));
        };
    }

    private void appendBytes(ThreadContext context, IRubyObject arg) {
        switch (arg) {
            case RubyFixnum fix -> cat(fix.asInt(context) & 0xff);
            case RubyBignum big -> cat(big.asBigInteger(context).intValue() & 0xff);
            case RubyString str -> value.append(str.getByteList());
            default -> throw runtimeError(context, "BUG: append_as_bytes arguments should have been validated");
        }
    }

    /** rb_str_chop / rb_str_chop_bang
     *
     */
    @JRubyMethod(name = "chop")
    public IRubyObject chop(ThreadContext context) {
        return value.isEmpty() ?
                newEmptyString(context.runtime, stringClass(context), value.getEncoding()) :
                makeSharedString(context.runtime, 0, StringSupport.choppedLength(this));
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

    @Deprecated(since = "10.0.0.0")
    public RubyString chomp19(ThreadContext context) {
        return chomp(context);
    }

    @Deprecated(since = "10.0.0.0")
    public RubyString chomp19(ThreadContext context, IRubyObject arg0) {
        return chomp(context, arg0);
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject chomp_bang19(ThreadContext context) {
        return chomp_bang(context);
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject chomp_bang19(ThreadContext context, IRubyObject arg0) {
        return chomp_bang(context, arg0);
    }

    /**
     * rb_str_chomp_bang
     *
     * In the common case, removes CR and LF characters in various ways depending on the value of
     *   the optional args[0].
     * If args.length==0 removes one instance of CR, CRLF or LF from the end of the string.
     * If args.length&gt;0 and args[0] is "\n" then same behaviour as args.length==0 .
     * If args.length&gt;0 and args[0] is "" then removes trailing multiple LF or CRLF (but no CRs at
     *   all(!)).
     */
    @JRubyMethod(name = "chomp")
    public RubyString chomp(ThreadContext context) {
        RubyString str = dupString(context, this);
        str.chomp_bang(context);
        return str;
    }

    @JRubyMethod(name = "chomp")
    public RubyString chomp(ThreadContext context, IRubyObject arg0) {
        RubyString str = dupString(context, this);
        str.chomp_bang(context, arg0);
        return str;
    }

    @JRubyMethod(name = "chomp!")
    public IRubyObject chomp_bang(ThreadContext context) {
        modifyCheck();
        if (value.isEmpty()) return context.nil;

        var globalVariables = globalVariables(context);
        IRubyObject rsObj = globalVariables.get("$/");

        return rsObj == globalVariables.getDefaultSeparator() ?
                smartChopBangCommon(context) : chompBangCommon(context, rsObj);
    }

    @JRubyMethod(name = "chomp!")
    public IRubyObject chomp_bang(ThreadContext context, IRubyObject arg0) {
        modifyCheck();
        if (value.isEmpty()) return context.nil;
        return chompBangCommon(context, arg0);
    }

    private IRubyObject chompBangCommon(ThreadContext context, IRubyObject rsObj) {
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
            return context.nil;
        }

        if (rslen > len) return context.nil;
        byte newline = rs.value.getUnsafeBytes()[rslen - 1];
        if (rslen == 1 && newline == (byte)'\n') return smartChopBangCommon(context);

        Encoding enc = checkEncoding(rs);
        if (rs.scanForCodeRange() == CR_BROKEN) return context.nil;

        int pp = end - rslen;
        if (bytes[p + len - 1] == newline && rslen <= 1 || value.endsWith(rs.value)) {
            if (enc.leftAdjustCharHead(bytes, p, pp, end) != pp) return context.nil;
            if (getCodeRange() != CR_7BIT) clearCodeRange();
            view(0, value.getRealSize() - rslen);
            return this;
        }
        return context.nil;
    }

    private IRubyObject smartChopBangCommon(ThreadContext context) {
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
                return context.nil;
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
                return context.nil;
            }
        }
        return this;
    }

    /** rb_str_lstrip / rb_str_lstrip_bang
     *
     */
    @JRubyMethod(name = "lstrip")
    public IRubyObject lstrip(ThreadContext context) {
        RubyString str = dupString(context, this);
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
        while (p < end && (bytes[p] == 0 || ASCII.isSpace(bytes[p] & 0xff))) p++;
        if (p > s) {
            view(p - s, end - p);
            return this;
        }
        return context.nil;
    }

    private IRubyObject multiByteLStrip(ThreadContext context, Encoding enc, byte[]bytes, int s, int end) {
        int p = s;

        while (p < end) {
            int c = codePoint(context, enc, bytes, p, end);
            if (!ASCII.isSpace(c) && c != 0) break;
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
    @JRubyMethod(name = "rstrip")
    public IRubyObject rstrip(ThreadContext context) {
        RubyString str = dupString(context, this);
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
        byte[] bytes = value.getUnsafeBytes();
        int start = value.getBegin();
        int end = start + value.getRealSize();
        Encoding enc = EncodingUtils.STR_ENC_GET(this);
        int endp = end;
        int prev;
        while ((prev = enc.prevCharHead(bytes, start, endp, end)) != -1) {
            int point;
            try {
                point = codePoint(enc, bytes, prev, end);
            } catch (IllegalArgumentException e) {
                throw context.runtime.newEncodingCompatibilityError(e.getMessage());
            }
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
    @JRubyMethod(name = "strip")
    public IRubyObject strip(ThreadContext context) {
        RubyString str = dupString(context, this);
        str.strip_bang(context);
        return str;
    }

    @JRubyMethod(name = "strip!")
    public IRubyObject strip_bang(ThreadContext context) {
        modifyCheck();

        IRubyObject left = lstrip_bang(context);
        IRubyObject right = rstrip_bang(context);

        return left == context.nil && right == context.nil ? context.nil : this;
    }

    @JRubyMethod(name = "count")
    public IRubyObject count(ThreadContext context) {
        throw argumentError(context, "wrong number of arguments");
    }

    // MRI: rb_str_count, first half
    @JRubyMethod(name = "count")
    public IRubyObject count(ThreadContext context, IRubyObject arg) {
        final RubyString countStr = arg.convertToString();
        final ByteList countValue = countStr.getByteList();
        final Encoding enc = checkEncoding(countStr);

        if ( countValue.length() == 1 && enc.isAsciiCompatible() ) {
            final byte[] countBytes = countValue.unsafeBytes();
            final int begin = countValue.begin(), size = countValue.length();
            if (enc.isReverseMatchAllowed(countBytes, begin, begin + size) && ! isCodeRangeBroken()) {
                if (value.isEmpty()) return asFixnum(context, 0);

                int n = 0;
                int[] len_p = {0};
                int c = EncodingUtils.encCodepointLength(context, countBytes, begin, begin + size, len_p, enc);

                final byte[] bytes = value.unsafeBytes();
                int i = value.begin();
                final int end = i + value.length();
                while (i < end) {
                    if (( bytes[i++] & 0xff ) == c) n++;
                }
                return asFixnum(context, n);
            }
        }

        final boolean[] table = new boolean[StringSupport.TRANS_SIZE + 1];
        StringSupport.TrTables tables = StringSupport.trSetupTable(context, countValue, table, null, true, enc);
        return asFixnum(context, StringSupport.strCount(context, value, table, tables, enc));
    }

    // MRI: rb_str_count for arity > 1, first half
    @JRubyMethod(name = "count", required = 1, rest = true, checkArity = false)
    public IRubyObject count(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 1, -1);

        if (value.isEmpty()) return asFixnum(context, 0);

        RubyString countStr = args[0].convertToString();
        Encoding enc = checkEncoding(countStr);

        final boolean[] table = new boolean[StringSupport.TRANS_SIZE + 1];
        StringSupport.TrTables tables = StringSupport.trSetupTable(context, countStr.value, table, null, true, enc);
        for (int i = 1; i < argc; i++ ) {
            countStr = args[i].convertToString();
            enc = checkEncoding(countStr);
            tables = StringSupport.trSetupTable(context, countStr.value, table, tables, false, enc);
        }

        return asFixnum(context, StringSupport.strCount(context, value, table, tables, enc));
    }

    /** rb_str_delete / rb_str_delete_bang
     *
     */

    @JRubyMethod(name = "delete")
    public IRubyObject delete(ThreadContext context) {
        throw argumentError(context, "wrong number of arguments");
    }

    @JRubyMethod(name = "delete")
    public IRubyObject delete(ThreadContext context, IRubyObject arg) {
        RubyString str = dupString(context, this);
        str.delete_bang(context, arg);
        return str;
    }

    @JRubyMethod(name = "delete", required = 1, rest = true, checkArity = false)
    public IRubyObject delete(ThreadContext context, IRubyObject[] args) {
        RubyString str = dupString(context, this);
        str.delete_bang(context, args);
        return str;
    }

    @JRubyMethod(name = "delete!")
    public IRubyObject delete_bang(ThreadContext context) {
        throw argumentError(context, "wrong number of arguments");
    }

    @JRubyMethod(name = "delete!")
    public IRubyObject delete_bang(ThreadContext context, IRubyObject arg) {
        if (value.isEmpty()) return context.nil;

        RubyString otherStr = arg.convertToString();
        Encoding enc = checkEncoding(otherStr);
        final boolean[] squeeze = new boolean[StringSupport.TRANS_SIZE + 1];
        StringSupport.TrTables tables = StringSupport.trSetupTable(context, otherStr.value, squeeze, null, true, enc);

        return StringSupport.strDeleteBang(context, this, squeeze, tables, enc) == null ? context.nil : this;
    }

    @JRubyMethod(name = "delete!", required = 1, rest = true, checkArity = false)
    public IRubyObject delete_bang(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 1, -1);

        if (value.isEmpty()) return context.nil;

        RubyString otherStr;
        Encoding enc = null;
        boolean[] squeeze = new boolean[StringSupport.TRANS_SIZE + 1];
        StringSupport.TrTables tables = null;

        for (int i = 0; i < argc; i++) {
            otherStr = args[i].convertToString();
            enc = checkEncoding(otherStr);
            tables = StringSupport.trSetupTable(context, otherStr.value, squeeze, tables, i == 0, enc);
        }

        return StringSupport.strDeleteBang(context, this, squeeze, tables, enc) == null ? context.nil : this;
    }

    /** rb_str_squeeze / rb_str_squeeze_bang
     *
     */

    @JRubyMethod(name = "squeeze")
    public IRubyObject squeeze(ThreadContext context) {
        RubyString str = dupString(context, this);
        str.squeeze_bang(context);
        return str;
    }

    @JRubyMethod(name = "squeeze")
    public IRubyObject squeeze(ThreadContext context, IRubyObject arg) {
        RubyString str = dupString(context, this);
        str.squeeze_bang(context, arg);
        return str;
    }

    @JRubyMethod(name = "squeeze", required = 1, rest = true, checkArity = false)
    public IRubyObject squeeze(ThreadContext context, IRubyObject[] args) {
        RubyString str = dupString(context, this);
        str.squeeze_bang(context, args);
        return str;
    }

    @JRubyMethod(name = "squeeze!")
    public IRubyObject squeeze_bang(ThreadContext context) {
        if (value.isEmpty()) {
            modifyCheck();
            return context.nil;
        }

        final boolean squeeze[] = new boolean[StringSupport.TRANS_SIZE];
        for (int i=0; i< StringSupport.TRANS_SIZE; i++) squeeze[i] = true;

        modifyAndKeepCodeRange();
        if (singleByteOptimizable()) {
            if (!StringSupport.singleByteSqueeze(value, squeeze)) return context.nil;
        } else {
            if (!StringSupport.multiByteSqueeze(context, value, squeeze, null, value.getEncoding(), false)) return context.nil;
        }

        return this;
    }

    @JRubyMethod(name = "squeeze!")
    public IRubyObject squeeze_bang(ThreadContext context, IRubyObject arg) {
        RubyString otherStr = arg.convertToString();
        final boolean squeeze[] = new boolean[StringSupport.TRANS_SIZE + 1];
        StringSupport.TrTables tables = StringSupport.trSetupTable(context, otherStr.value, squeeze, null, true, checkEncoding(otherStr));

        modifyAndKeepCodeRange();
        if (singleByteOptimizable() && otherStr.singleByteOptimizable()) {
            if (!StringSupport.singleByteSqueeze(value, squeeze)) return context.nil;
        } else {
            if (!StringSupport.multiByteSqueeze(context, value, squeeze, tables, value.getEncoding(), true)) {
                return context.nil;
            }
        }

        return this;
    }

    @JRubyMethod(name = "squeeze!", required = 1, rest = true, checkArity = false)
    public IRubyObject squeeze_bang(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 1, -1);

        if (value.getRealSize() == 0) {
            modifyCheck();
            return context.nil;
        }

        RubyString otherStr = args[0].convertToString();
        Encoding enc = checkEncoding(otherStr);
        final boolean squeeze[] = new boolean[StringSupport.TRANS_SIZE + 1];
        StringSupport.TrTables tables = StringSupport.trSetupTable(context, otherStr.value, squeeze, null, true, enc);

        boolean singleByte = singleByteOptimizable() && otherStr.singleByteOptimizable();
        for (int i = 1; i< argc; i++) {
            otherStr = args[i].convertToString();
            enc = checkEncoding(otherStr);
            singleByte = singleByte && otherStr.singleByteOptimizable();
            tables = StringSupport.trSetupTable(context, otherStr.value, squeeze, tables, false, enc);
        }

        modifyAndKeepCodeRange();
        if (singleByte) {
            if (! StringSupport.singleByteSqueeze(value, squeeze)) return context.nil;
        } else {
            if (! StringSupport.multiByteSqueeze(context, value, squeeze, tables, enc, true)) return context.nil;
        }

        return this;
    }

    @Deprecated(since = "9.2.0.0")
    public IRubyObject tr19(ThreadContext context, IRubyObject src, IRubyObject repl) {
        return tr(context, src, repl);
    }

    @Deprecated(since = "9.2.0.0")
    public IRubyObject tr_bang19(ThreadContext context, IRubyObject src, IRubyObject repl) {
        return tr_bang(context, src, repl);
    }

    /** rb_str_tr / rb_str_tr_bang
     *
     */
    @JRubyMethod(name = "tr")
    public IRubyObject tr(ThreadContext context, IRubyObject src, IRubyObject repl) {
        RubyString str = dupString(context, this);
        str.trTrans(context, src, repl, false);
        return str;
    }

    @JRubyMethod(name = "tr!")
    public IRubyObject tr_bang(ThreadContext context, IRubyObject src, IRubyObject repl) {
        return trTrans(context, src, repl, false);
    }

    private IRubyObject trTrans(ThreadContext context, IRubyObject src, IRubyObject repl, boolean sflag) {
        RubyString replStr = repl.convertToString();
        ByteList replList = replStr.value;
        RubyString srcStr = src.convertToString();

        if (value.getRealSize() == 0) return context.nil;
        if (replList.getRealSize() == 0) return delete_bang(context, src);

        CodeRangeable ret = trTransHelper(context, srcStr, replStr, sflag);
        return ret == null ? context.nil : (IRubyObject) ret;
    }

    private CodeRangeable trTransHelper(ThreadContext context, CodeRangeable srcStr, CodeRangeable replStr, boolean sflag) {
        try {
            return StringSupport.trTransHelper(this, srcStr, replStr, sflag);
        } catch (IllegalArgumentException e) {
            throw argumentError(context, e.getMessage());
        }
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject tr_s19(ThreadContext context, IRubyObject src, IRubyObject repl) {
        return tr_s(context, src, repl);
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject tr_s_bang19(ThreadContext context, IRubyObject src, IRubyObject repl) {
        return tr_s_bang(context, src, repl);
    }

    /** rb_str_tr_s / rb_str_tr_s_bang
     *
     */
    @JRubyMethod(name = "tr_s")
    public IRubyObject tr_s(ThreadContext context, IRubyObject src, IRubyObject repl) {
        RubyString str = dupString(context, this);
        str.trTrans(context, src, repl, true);
        return str;
    }

    @JRubyMethod(name = "tr_s!")
    public IRubyObject tr_s_bang(ThreadContext context, IRubyObject src, IRubyObject repl) {
        return trTrans(context, src, repl, true);
    }

    /** rb_str_each_line
     *
     */
    @JRubyMethod(name = "each_line")
    public IRubyObject each_line(ThreadContext context, Block block) {
        return StringSupport.rbStrEnumerateLines(this, context, "each_line", globalVariables(context).get("$/"), block, false);
    }

    @JRubyMethod(name = "each_line")
    public IRubyObject each_line(ThreadContext context, IRubyObject arg, Block block) {
        return StringSupport.rbStrEnumerateLines(this, context, "each_line", arg, block, false);
    }

    @JRubyMethod(name = "each_line")
    public IRubyObject each_line(ThreadContext context, IRubyObject arg, IRubyObject opts, Block block) {
        return StringSupport.rbStrEnumerateLines(this, context, "each_line", arg, opts, block, false);
    }

    @Deprecated(since = "9.2.1.0") // no longer used
    public IRubyObject each_lineCommon(ThreadContext context, IRubyObject sep, Block block) {
        if (sep == context.nil) {
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
                block.yield(context, makeShared(context.runtime, s - ptr, p - s));
                modifyCheck(bytes, len);
                s = p;
            }
        }

        if (s != end) {
            if (p > end) p = end;
            block.yield(context, makeShared(context.runtime, s - ptr, p - s));
        }

        return this;
    }

    @JRubyMethod(name = "lines")
    public IRubyObject lines(ThreadContext context, Block block) {
        return StringSupport.rbStrEnumerateLines(this, context, "lines", globalVariables(context).get("$/"), block, true);
    }

    @JRubyMethod(name = "lines")
    public IRubyObject lines(ThreadContext context, IRubyObject arg, Block block) {
        return StringSupport.rbStrEnumerateLines(this, context, "lines", arg, block, true);
    }

    @JRubyMethod(name = "lines")
    public IRubyObject lines(ThreadContext context, IRubyObject arg, IRubyObject opts, Block block) {
        return StringSupport.rbStrEnumerateLines(this, context, "lines", arg, opts, block, true);
    }

    /**
     * rb_str_each_byte
     */
    @JRubyMethod(name = "each_byte")
    public IRubyObject each_byte(ThreadContext context, Block block) {
        return enumerateBytes(context, "each_byte", block, false);
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

    /**
     * A character size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject eachCharSize(ThreadContext context, RubyString recv, IRubyObject[] args) {
        return recv.rubyLength(context);
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
        RubyString str = this;
        int len, n;
        byte[] ptrBytes;
        int ptr;
        Encoding enc;

        if (block.isGiven()) {
            if (wantarray) {
                wantarray = false;
            }
        } else if (!wantarray) {
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
                    n = encFastMBCLen(ptrBytes, ptr + i, ptr + len, enc);
                    IRubyObject substr = str.substr(context, i, n);
                    if (wantarray) ary[a++] = substr;
                    else block.yield(context, substr);
                }
                break;
            default:
                for (int i = 0; i < len; i += n) {
                    n = StringSupport.length(enc, ptrBytes, ptr + i, ptr + len);
                    IRubyObject substr = str.substr(context, i, n);
                    if (wantarray) ary[a++] = substr;
                    else block.yield(context, substr);
                }
        }

        assert !wantarray || a == ary.length;

        return wantarray ? newArrayNoCopy(context, ary) : this;
    }

    // MRI: rb_str_enumerate_codepoints
    private IRubyObject enumerateCodepoints(ThreadContext context, String name, Block block, boolean wantarray) {
        RubyString str = this;
        byte[] ptrBytes;
        int ptr, end;
        Encoding enc;

        if (singleByteOptimizable()) return enumerateBytes(context, name, block, wantarray);

        if (block.isGiven()) {
            if (wantarray) {
                wantarray = false;
            }
        } else if (!wantarray) {
            return enumeratorizeWithSize(context, str, name, RubyString::codepointSize);
        }

        if (!str.isFrozen()) str.setByteListShared();
        ByteList strByteList = str.value;
        ptrBytes = strByteList.unsafeBytes();
        ptr = strByteList.begin();
        end = ptr + strByteList.getRealSize();
        enc = EncodingUtils.getEncoding(strByteList);

        RubyArray<?> ary;
        if (wantarray) {
            final int len = str.strLength(strByteList, enc);
            ary = allocArray(context, len);
        } else {
            ary = null;
        }

        while (ptr < end) {
            int c = codePoint(context, enc, ptrBytes, ptr, end);
            int n = codeLength(enc, c);
            if (wantarray) ary.append(context, asFixnum(context, c));
            else block.yield(context, asFixnum(context, c));
            ptr += n;
        }

        return wantarray ? ary : this;
    }

    private IRubyObject enumerateBytes(ThreadContext context, String name, Block block, boolean wantarray) {
        if (block.isGiven()) {
            if (wantarray) {
                wantarray = false;
            }
        } else if (!wantarray) {
            return enumeratorizeWithSize(context, this, name, RubyString::byteSize);
        }

        IRubyObject[] ary = wantarray ? new IRubyObject[value.getRealSize()] : null;
        // Check the length every iteration, since the block can modify this string.
        for (int i=0; i < value.getRealSize(); i++) {
            RubyFixnum bite = asFixnum(context, value.get(i) & 0xFF);
            if (wantarray) ary[i] = bite;
            else block.yield(context, bite);
        }

        return wantarray ? newArrayNoCopy(context, ary) : this;
    }

    /**
     * A codepoint size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject codepointSize(ThreadContext context, RubyString recv, IRubyObject[] args) {
        return recv.rubyLength(context);
    }

    private static final ByteList GRAPHEME_CLUSTER_PATTERN = new ByteList(new byte[] {(byte)'\\', (byte)'X'}, false);

    /**
     * A grapheme cluster size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject eachGraphemeClusterSize(ThreadContext context, RubyString self, IRubyObject[] args) {
        ByteList value = self.getByteList();
        Encoding enc = value.getEncoding();
        if (!enc.isUnicode()) return self.rubyLength(context);

        Regex reg = RubyRegexp.getRegexpFromCache(context.runtime, GRAPHEME_CLUSTER_PATTERN, enc, RegexpOptions.NULL_OPTIONS);
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
        return asFixnum(context, count);
    }

    private IRubyObject enumerateGraphemeClusters(ThreadContext context, String name, Block block, boolean wantarray) {
        RubyString str = this;
        Encoding enc = str.getEncoding();
        if (!enc.isUnicode()) return enumerateChars(context, name, block, wantarray);

        if (block.isGiven()) {
            if (wantarray) {
                wantarray = false;
            }
        } else if (!wantarray) {
            return enumeratorizeWithSize(context, str, name, RubyString::eachGraphemeClusterSize);
        }

        Regex reg = RubyRegexp.getRegexpFromCache(context.runtime, GRAPHEME_CLUSTER_PATTERN, enc, RegexpOptions.NULL_OPTIONS);

        if (!wantarray) str = str.newFrozen();
        ByteList strByteList = str.value;
        byte[] ptrBytes = strByteList.unsafeBytes();
        int ptr = strByteList.begin();
        int end = ptr + strByteList.getRealSize();
        Matcher matcher = reg.matcher(ptrBytes, ptr, end);

        RubyArray ary;
        if (wantarray) {
            ary = allocArray(context, end - ptr);
        } else {
            ary = null;
        }

        while (ptr < end) {
            int len = matcher.match(ptr, end, Option.DEFAULT);
            if (len <= 0) break;
            RubyString result = newStringShared(context.runtime, ptrBytes, ptr, len, enc);
            if (wantarray) ary.append(context, result);
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

    @Deprecated(since = "10.0.0.0")
    public RubySymbol intern() {
        return intern(getCurrentContext());
    }

    // MRI: rb_str_intern
    @JRubyMethod(name = {"to_sym", "intern"})
    public RubySymbol intern(ThreadContext context) {
        if (scanForCodeRange() == CR_BROKEN) {
            throw context.runtime.newEncodingError("invalid symbol in encoding " + getEncoding() + " :" + inspect(context));
        }

        RubySymbol symbol = context.runtime.getSymbolTable().getSymbol(value);
        if (symbol.getBytes() == value) shareLevel = SHARE_LEVEL_BYTELIST;
        return symbol;
    }

    @JRubyMethod
    public IRubyObject ord(ThreadContext context) {
        return asFixnum(context, codePoint(context, this.value));
    }

    @JRubyMethod
    public IRubyObject sum(ThreadContext context) {
        return sumCommon(context, 16);
    }

    @JRubyMethod
    public IRubyObject sum(ThreadContext context, IRubyObject arg) {
        return sumCommon(context, toLong(context, arg));
    }

    public IRubyObject sumCommon(ThreadContext context, long bits) {
        byte[] bytes = value.getUnsafeBytes();
        int p = value.getBegin();
        int len = value.getRealSize();
        int end = p + len;

        if (bits >= 8 * 8) { // long size * bits in byte
            IRubyObject one = asFixnum(context, 0);
            IRubyObject sum = asFixnum(context, 1);
            StringSites sites = sites(context);
            CallSite op_plus = sites.op_plus;
            while (p < end) {
                modifyCheck(bytes, len);
                sum = op_plus.call(context, sum, sum, asFixnum(context, bytes[p++] & 0xff));
            }
            if (bits != 0) {
                IRubyObject mod = sites.op_lshift.call(context, one, one, asFixnum(context, bits));
                sum = sites.op_and.call(context, sum, sum, sites.op_minus.call(context, mod, mod, one));
            }
            return sum;
        } else {
            long sum = 0;
            while (p < end) {
                modifyCheck(bytes, len);
                sum += bytes[p++] & 0xff;
            }
            return asFixnum(context, bits == 0 ? sum : sum & (1L << bits) - 1L);
        }
    }

    /** string_to_c
     *
     */
    @JRubyMethod
    public IRubyObject to_c(ThreadContext context) {
        verifyAsciiCompatible();

        RubyRegexp underscore_pattern = RubyRegexp.newDummyRegexp(context.runtime, Numeric.ComplexPatterns.underscores_pat);
        RubyString s = gsubFast(context, underscore_pattern, Create.newString(context, UNDERSCORE), Block.NULL_BLOCK);
        IRubyObject[] ary = RubyComplex.str_to_c_internal(context, s);
        IRubyObject first = ary[0];

        return first != context.nil ? first : RubyComplex.newComplexCanonicalize(context, asFixnum(context, 0));
    }

    private static final ByteList UNDERSCORE = new ByteList(new byte[] { '_' }, false);

    /** string_to_r
     *
     */
    @JRubyMethod
    public IRubyObject to_r(ThreadContext context) {
        var first = RubyRational.str_to_r_internal(context, this, true)[0];

        return first != context.nil ?
                first : RubyRational.newRationalNoReduce(context, asFixnum(context, 0), asFixnum(context, 1));
    }

    @Deprecated(since = "10.0.0.0", forRemoval = true)
    @SuppressWarnings("removal")
    public static RubyString unmarshalFrom(org.jruby.runtime.marshal.UnmarshalStream input) throws java.io.IOException {
        return newString(input.getRuntime(), input.unmarshalString());
    }

    public static RubyString unmarshalFrom(ThreadContext context, RubyInputStream in, MarshalLoader input) {
        return Create.newString(context, input.unmarshalString(context, in));
    }

    /**
     * @see org.jruby.util.Pack#unpack
     */
    @JRubyMethod
    public RubyArray unpack(ThreadContext context, IRubyObject obj, Block block) {
        return Pack.unpackWithBlock(context, this, stringValue(obj).value, block);
    }

    @JRubyMethod
    public RubyArray unpack(ThreadContext context, IRubyObject obj, IRubyObject opt, Block block) {
        long offset = unpackOffset(context, opt);
        return Pack.unpackWithBlock(context, this, stringValue(obj).value, offset, block);
    }

    @JRubyMethod
    public IRubyObject unpack1(ThreadContext context, IRubyObject obj, Block block) {
        return Pack.unpack1WithBlock(context, this, stringValue(obj).value, block);
    }

    @JRubyMethod
    public IRubyObject unpack1(ThreadContext context, IRubyObject obj, IRubyObject opt, Block block) {
        long offset = unpackOffset(context, opt);
        return Pack.unpack1WithBlock(context, this, stringValue(obj).value, offset, block);
    }

    private static long unpackOffset(ThreadContext context, IRubyObject opt) {
        if (!(opt instanceof RubyHash options)) throw argumentError(context, 2, 1);

        long offset = 0;
        if (options.size() == 1) {
            IRubyObject offsetArg = options.fastARef(asSymbol(context, "offset"));
            if (offsetArg == null) throw argumentError(context, "unknown keyword: " + options.keys().first(context).inspect(context));
            offset = toLong(context, offsetArg);
        }
        // FIXME: keyword arg processing incomplete.  We need a better system.

        return offset;
    }

    public void empty() {
        value = ByteList.EMPTY_BYTELIST;
        shareLevel = SHARE_LEVEL_BYTELIST;
    }

    @JRubyMethod
    public IRubyObject encoding(ThreadContext context) {
        return encodingService(context).getEncoding(value.getEncoding());
    }

    @JRubyMethod(name = "encode!")
    public IRubyObject encode_bang(ThreadContext context) {
        modifyAndClearCodeRange();

        return EncodingUtils.strTranscode(context, this, RubyString::updateFromTranscode);
    }

    @JRubyMethod(name = "encode!")
    public IRubyObject encode_bang(ThreadContext context, IRubyObject arg0) {
        modifyAndClearCodeRange();

        return EncodingUtils.strTranscode(context, arg0, this, RubyString::updateFromTranscode);
    }

    @JRubyMethod(name = "encode!")
    public IRubyObject encode_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        modifyAndClearCodeRange();

        return EncodingUtils.strTranscode(context, arg0, arg1, this, RubyString::updateFromTranscode);
    }

    @JRubyMethod(name = "encode!")
    public IRubyObject encode_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        modifyAndClearCodeRange();

        return EncodingUtils.strTranscode(context, arg0, arg1, arg2, this, RubyString::updateFromTranscode);
    }

    private static RubyString updateFromTranscode(ThreadContext context, RubyString self, Encoding encindex, RubyString newstr) {
        if (encindex == null) return self;
        if (newstr == self) {
            self.setEncoding(encindex);
            return self;
        }
        self.replace(context, newstr);
        self.setEncoding(encindex);
        return self;
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
        modifyAndClearCodeRange();
        associateEncoding(encoding);
        clearCodeRange();
        return this;
    }

    @JRubyMethod(name = "valid_encoding?")
    public IRubyObject valid_encoding_p(ThreadContext context) {
        return asBoolean(context, scanForCodeRange() != CR_BROKEN);
    }

    @JRubyMethod(name = "ascii_only?")
    public IRubyObject ascii_only_p(ThreadContext context) {
        return asBoolean(context, scanForCodeRange() == CR_7BIT);
    }

    @JRubyMethod
    public IRubyObject b(ThreadContext context) {
        Encoding encoding = ASCIIEncoding.INSTANCE;
        RubyString dup = dupString(context, this);
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
        if (newStr.isNil()) return dupString(context, this);
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
        if (!newStr.isNil()) return replace(context, newStr);
        return this;
    }

    @JRubyMethod @JRubyAPI
    public IRubyObject freeze(ThreadContext context) {
        if (isChilled()) flags &= ~(CHILLED_LITERAL|CHILLED_SYMBOL_TO_S);
        if (isFrozen()) return this;
        resize(size());
        return super.freeze(context);
    }

    public RubyString chill() {
        flags |= CHILLED_LITERAL;
        return this;
    }

    public RubyString chill_symbol_string() {
        flags |= CHILLED_SYMBOL_TO_S;
        return this;
    }

    @Deprecated(since = "10.0.0.0")
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

    /**
     * Get the ByteList which backs this Ruby String
     * @return The byte list
     */
    @JRubyAPI
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
            return (T) Character.valueOf((char) codePoint(getRuntime().getCurrentContext(), value));
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
        Encoding encidx;
        IRubyObject buf = context.nil;
        byte[] repBytes;
        int rep;
        int replen;

        if (block.isGiven() && repl != context.nil) throw argumentError(context, "both of block and replacement given");

        if (cr == CR_7BIT || cr == CR_VALID) return context.nil;

        if (repl != context.nil) repl = EncodingUtils.strCompatAndValid(context, repl, enc);

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
                    if (buf.isNil()) buf = RubyString.newStringLight(context.runtime, value.getRealSize());
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
                        repl = block.yieldSpecific(context, RubyString.newString(context.runtime, pBytes, p, clen, enc));
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
                buf = RubyString.newStringLight(context.runtime, value.getRealSize());
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
                    repl = block.yieldSpecific(context, RubyString.newString(context.runtime, pBytes, p, e - p, enc));
                    repl = EncodingUtils.strCompatAndValid(context, repl, enc);
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
                    if (buf.isNil()) buf = RubyString.newStringLight(context.runtime, value.getRealSize());
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
                        repl = block.yieldSpecific(context, RubyString.newString(context.runtime, pBytes, p, clen, enc));
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
                buf = RubyString.newStringLight(context.runtime, value.getRealSize());
            }
            if (p1 < p) {
                ((RubyString)buf).cat(pBytes, p1, p - p1);
            }
            if (p < e) {
                if (repBytes != null) {
                    ((RubyString)buf).cat(repBytes, rep, replen);
                }
                else {
                    repl = block.yieldSpecific(context, RubyString.newString(context.runtime, pBytes, p, e - p, enc));
                    repl = EncodingUtils.strCompatAndValid(context, repl, enc);
                    ((RubyString)buf).cat((RubyString)repl);
                }
            }
            cr = CR_VALID;
        }

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

    @Deprecated(since = "10.0.0.0")
    public boolean isBare(Ruby runtime) {
        return isBare(getCurrentContext());
    }

    /**
     * Is this a "bare" string, i.e. has no instance vars, and class == String.
     */
    public boolean isBare(ThreadContext context) {
        return !hasInstanceVariables() && metaClass == stringClass(context);
    }

    private static StringSites sites(ThreadContext context) {
        return context.sites.String;
    }

    @Deprecated(since = "9.0.0.0")
    public final RubyString strDup() {
        return strDup(getRuntime(), getMetaClass().getRealClass());
    }

    @Deprecated(since = "9.4.6.0") // not used
    public RubyArray unpack(IRubyObject obj) {
        return Pack.unpack(getRuntime(), this.value, stringValue(obj).value);
    }

    @Deprecated(since = "9.4.6.0")
    public IRubyObject encode_bang(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 2);

        modifyAndClearCodeRange();

        return EncodingUtils.strTranscode(context, args, this, RubyString::updateFromTranscode);
    }

}
