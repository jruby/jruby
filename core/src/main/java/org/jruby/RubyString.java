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

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF16BEEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.unicode.UnicodeEncoding;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Convert;
import org.jruby.api.Create;
import org.jruby.api.JRubyAPI;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.JavaSites.StringSites;
import org.jruby.runtime.SimpleHash;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.runtime.encoding.MarshalEncoding;
import org.jruby.runtime.marshal.MarshalLoader;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeable;
import org.jruby.util.Sprintf;
import org.jruby.util.StringSupport;
import org.jruby.util.io.EncodingUtils;
import org.jruby.util.io.RubyInputStream;

import java.nio.charset.Charset;
import java.util.function.Function;

import static org.jruby.RubyComparable.invcmp;
import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.anno.FrameField.BACKREF;
import static org.jruby.api.Access.stringClass;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Convert.toLong;
import static org.jruby.api.Create.allocArray;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;
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
public abstract class RubyString extends RubyObject implements CharSequence, EncodingCapable, MarshalEncoding, CodeRangeable, Appendable, SimpleHash {
    static final ASCIIEncoding ASCII = ASCIIEncoding.INSTANCE;
    static final UTF8Encoding UTF8 = UTF8Encoding.INSTANCE;

    public static RubyString[] NULL_ARRAY = {};

    public static RubyClass createStringClass(ThreadContext context, RubyClass Object, RubyModule Comparable) {
        return defineClass(context, "String", Object, RubyStringByteList::newAllocatedString).
                reifiedClass(RubyString.class).
                kindOf(new RubyModule.JavaClassKindOf(RubyString.class)).
                classIndex(ClassIndex.STRING).
                include(context, Comparable).
                defineMethods(context, RubyString.class);
    }

    @Override
    public abstract Encoding getEncoding();

    @Override
    public abstract void setEncoding(Encoding encoding);

    @Override
    @SuppressWarnings("ReferenceEquality")
    public boolean shouldMarshalEncoding() {
        return getEncoding() != ASCIIEncoding.INSTANCE;
    }

    @Override
    public Encoding getMarshalEncoding() {
        return getEncoding();
    }

    public abstract void associateEncoding(Encoding enc);

    public abstract void setEncodingAndCodeRange(Encoding enc, int cr);

    @Deprecated(since = "10.0.0.0")
    public abstract Encoding toEncoding(Ruby runtime);

    @Override
    public abstract int getCodeRange();

    @Override
    public abstract void setCodeRange(int codeRange);

    @Override
    public abstract void clearCodeRange();

    @Override
    public abstract void keepCodeRange();

    // ENC_CODERANGE_ASCIIONLY
    public abstract boolean isCodeRangeAsciiOnly();

    // rb_enc_str_asciionly_p
    public abstract boolean isAsciiOnly();

    @Override
    public abstract boolean isCodeRangeValid();

    public abstract boolean isCodeRangeBroken();

    // MRI: is_broken_string
    public abstract boolean isBrokenString();

    // rb_enc_str_coderange
    @Override
    public abstract int scanForCodeRange();

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

    abstract Encoding isCompatibleWith(EncodingCapable other);

    // rb_enc_check
    public final Encoding checkEncoding(RubyString other) {
        return checkEncoding((CodeRangeable) other);
    }

    @Deprecated(since = "10.0.0.0")
    abstract Encoding checkEncoding(EncodingCapable other);

    @Override
    public abstract Encoding checkEncoding(CodeRangeable other);

    // rb_enc_check but only supports strings
    public static Encoding checkEncoding(final Ruby runtime, ByteList str1, ByteList str2) {
        Encoding enc = StringSupport.areCompatible(str1, str2);
        if (enc == null) {
            throw runtime.newEncodingCompatibilityError("incompatible character encodings: " +
                    str1.getEncoding() + " and " + str2.getEncoding());
        }
        return enc;
    }

    public abstract int strLength();

    abstract int strLength(final ByteList bytes, final Encoding enc);

    // MRI: rb_str_sublen
    abstract int subLength(int pos);

    /** short circuit for String key comparison
     *
     */
    @Override
    public boolean eql(IRubyObject other) {
        return super.eql(other);
    }
    /**
     * Does this string contain \0 anywhere (per byte search).
     * @return true if it does
     */
    public abstract boolean hasNul();

    // mri: rb_must_asciicompat
    public void verifyAsciiCompatible() {
        if (!getEncoding().isAsciiCompatible()) throw getRuntime().newEncodingCompatibilityError("ASCII incompatible encoding: " + getEncoding());
    }

    protected RubyString(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    protected RubyString(Ruby runtime, RubyClass rubyClass, boolean objectspace) {
        super(runtime, rubyClass, objectspace);
    }

//    public RubyString(Ruby runtime, RubyClass rubyClass, CharSequence value) {
//        this(runtime, rubyClass, value, UTF8);
//    }
//
//    public RubyString(Ruby runtime, RubyClass rubyClass, CharSequence value, Encoding enc) {
//        super(runtime, rubyClass);
//        assert value != null;
//        assert enc != null;
//
//        this.value = encodeBytelist(value, enc);
//    }
//
//    public RubyString(Ruby runtime, RubyClass rubyClass, byte[] value) {
//        super(runtime, rubyClass);
//        assert value != null;
//        this.value = new ByteList(value);
//    }
//
//    public RubyString(Ruby runtime, RubyClass rubyClass, ByteList value) {
//        super(runtime, rubyClass);
//        assert value != null;
//        this.value = value;
//    }
//
//    public RubyString(Ruby runtime, RubyClass rubyClass, ByteList value, boolean objectSpace) {
//        super(runtime, rubyClass, objectSpace);
//        assert value != null;
//        this.value = value;
//    }
//
//    public RubyString(Ruby runtime, RubyClass rubyClass, ByteList value, Encoding encoding, boolean objectSpace) {
//        this(runtime, rubyClass, value, objectSpace);
//        value.setEncoding(encoding);
//    }

    // Deprecated String construction routines

    public static RubyString newStringLight(Ruby runtime, ByteList bytes) {
        return new RubyStringByteList(runtime, runtime.getString(), bytes, false);
    }

    public static RubyString newStringLight(Ruby runtime, int size) {
        checkNegativeSize(runtime, size);
        return new RubyStringByteList(runtime, runtime.getString(), new ByteList(size), false);
    }

    public static RubyString newStringLight(Ruby runtime, int size, Encoding encoding) {
        checkNegativeSize(runtime, size);
        return new RubyStringByteList(runtime, runtime.getString(), new ByteList(size), encoding, false);
    }

    private static void checkNegativeSize(Ruby runtime, int size) {
        if (size < 0) {
            throw runtime.newArgumentError("negative string size (or size too big)");
        }
    }

    public static RubyString newString(Ruby runtime, CharSequence str) {
        return new RubyStringByteList(runtime, runtime.getString(), str, UTF8);
    }

    public static RubyString newString(Ruby runtime, CharSequence str, Encoding encoding) {
        return new RubyStringByteList(runtime, runtime.getString(), str, encoding);
    }

    public static RubyString newString(Ruby runtime, String str) {
        return new RubyStringByteList(runtime, runtime.getString(), str, UTF8);
    }

    public static RubyString newString(Ruby runtime, String str, Encoding encoding) {
        return new RubyStringByteList(runtime, runtime.getString(), str, encoding);
    }

    public static RubyString newBinaryString(Ruby runtime, String str) {
        return new RubyStringByteList(runtime, runtime.getString(), new ByteList(ByteList.plain(str), ASCIIEncoding.INSTANCE, false));
    }

    public static RubyString newBinaryString(Ruby runtime, ByteList str) {
        return new RubyStringByteList(runtime, runtime.getString(), str, ASCIIEncoding.INSTANCE, false);
    }

    public static RubyString newUSASCIIString(Ruby runtime, String str) {
        return new RubyStringByteList(runtime, runtime.getString(), str, USASCIIEncoding.INSTANCE);
    }

    public static RubyString newString(Ruby runtime, byte[] bytes) {
        return new RubyStringByteList(runtime, runtime.getString(), bytes);
    }

    public static RubyString newString(Ruby runtime, byte[] bytes, int start, int length) {
        return newString(runtime, bytes, start, length, ASCIIEncoding.INSTANCE);
    }

    // rb_enc_str_new
    public static RubyString newString(Ruby runtime, byte[] bytes, int start, int length, Encoding encoding) {
        byte[] copy = new byte[length];
        System.arraycopy(bytes, start, copy, 0, length);
        return new RubyStringByteList(runtime, runtime.getString(), new ByteList(copy, encoding, false));
    }

    public static RubyString newStringNoCopy(Ruby runtime, byte[] bytes, int start, int length, Encoding encoding) {
        return new RubyStringByteList(runtime, runtime.getString(), new ByteList(bytes, start, length, encoding, false));
    }

    public static RubyString newString(Ruby runtime, ByteList bytes) {
        return new RubyStringByteList(runtime, runtime.getString(), bytes);
    }

    public static RubyString newString(Ruby runtime, ByteList bytes, int coderange) {
        return new RubyStringByteList(runtime, runtime.getString(), bytes, coderange);
    }

    public static RubyString newChilledString(Ruby runtime, ByteList bytes, int coderange, String file, int line) {
        if (runtime.getInstanceConfig().isDebuggingFrozenStringLiteral()) {
            return new RubyStringByteList.DebugChilledString(runtime, runtime.getString(), bytes, coderange, file, line + 1);
        }

        return newStringShared(runtime, bytes, coderange).chill();
    }

    public static RubyString newString(Ruby runtime, ByteList bytes, Encoding encoding) {
        return new RubyStringByteList(runtime, runtime.getString(), bytes, encoding);
    }

    @SuppressWarnings("ReferenceEquality")
    public static RubyString newUnicodeString(Ruby runtime, String str) {
        return runtime.getDefaultInternalEncoding() == UTF16BEEncoding.INSTANCE ?
            newUTF16String(runtime, str) : newUTF8String(runtime, str);
    }

    public static RubyString newUTF8String(Ruby runtime, String str) {
        return new RubyStringByteList(runtime, runtime.getString(), RubyEncoding.doEncodeUTF8(str));
    }

    public static RubyString newUTF16String(Ruby runtime, String str) {
        return new RubyStringByteList(runtime, runtime.getString(), RubyEncoding.doEncodeUTF16(str));
    }

    @SuppressWarnings("ReferenceEquality")
    public static RubyString newUnicodeString(Ruby runtime, CharSequence str) {
        return runtime.getDefaultInternalEncoding() == UTF16BEEncoding.INSTANCE ?
            newUTF16String(runtime, str) : newUTF8String(runtime, str);
    }

    public static RubyString newUTF8String(Ruby runtime, CharSequence str) {
        return new RubyStringByteList(runtime, runtime.getString(), RubyEncoding.doEncodeUTF8(str));
    }

    public static RubyString newUTF16String(Ruby runtime, CharSequence str) {
        return new RubyStringByteList(runtime, runtime.getString(), RubyEncoding.doEncodeUTF16(str));
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

    // String construction routines by NOT byte[] buffer and making the target String shared
    public static RubyString newStringShared(Ruby runtime, RubyString orig) {
        if (orig instanceof RubyStringByteList rsbl) {
            return RubyStringByteList.newStringShared(runtime, rsbl);
        }
        throw new RuntimeException("unknown string type: " + orig.getClass());
    }

    public static RubyString newStringShared(Ruby runtime, ByteList bytes) {
        return newStringShared(runtime, runtime.getString(), bytes);
    }

    public static RubyString newStringShared(Ruby runtime, ByteList bytes, Encoding encoding) {
        return newStringShared(runtime, runtime.getString(), bytes, encoding);
    }


    public static RubyString newStringShared(Ruby runtime, ByteList bytes, int codeRange) {
        return RubyStringByteList.newStringShared(runtime, bytes, codeRange);
    }

    public static RubyString newStringShared(Ruby runtime, RubyClass clazz, ByteList bytes) {
        return RubyStringByteList.newStringShared(runtime, clazz, bytes);
    }

    @SuppressWarnings("ReferenceEquality")
    public static RubyString newStringShared(Ruby runtime, RubyClass clazz, ByteList bytes, Encoding encoding) {
        return RubyStringByteList.newStringShared(runtime, clazz, bytes, encoding);
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
        return RubyStringByteList.newStringShared(runtime, bytes, start, length, encoding);
    }

    public static RubyString newEmptyString(Ruby runtime) {
        return newEmptyString(runtime, runtime.getString());
    }

    public static RubyString newEmptyBinaryString(Ruby runtime) {
        return newAllocatedString(runtime, runtime.getString());
    }

    public static RubyString newAllocatedString(Ruby runtime, RubyClass metaClass) {
        return RubyStringByteList.newAllocatedString(runtime, metaClass);
    }

    public static RubyString newEmptyString(Ruby runtime, RubyClass metaClass) {
        return RubyStringByteList.newEmptyString(runtime, metaClass);
    }

    // String construction routines by NOT byte[] buffer and NOT making the target String shared
    public static RubyString newStringNoCopy(Ruby runtime, ByteList bytes) {
        return newStringNoCopy(runtime, runtime.getString(), bytes);
    }

    public static RubyString newStringNoCopy(Ruby runtime, RubyClass clazz, ByteList bytes) {
        return new RubyStringByteList(runtime, clazz, bytes);
    }

    public static RubyString newStringNoCopy(Ruby runtime, byte[] bytes, int start, int length) {
        return newStringNoCopy(runtime, new ByteList(bytes, start, length, false));
    }

    public static RubyString newStringNoCopy(Ruby runtime, byte[] bytes) {
        return newStringNoCopy(runtime, new ByteList(bytes, false));
    }

    // str_independent
    public abstract boolean independent();

    // str_make_independent, modified to create a new String rather than possibly modifying a frozen one
    public abstract RubyString makeIndependent();

    // str_make_independent_expand
    public abstract RubyString makeIndependent(final int length);

    // MRI: EXPORT_STR macro in process.c
    public abstract RubyString export(ThreadContext context);

    /**
     * Determine how much space exists after the begin offset in this string's buffer.
     *
     * @return the amount of capacity in this string's buffer after the begin offset
     */
    public abstract int capacity();

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

    public static EmptyByteListHolder getEmptyByteList(Encoding enc) {
        return RubyStringByteList.getEmptyByteList(enc);
    }

    public static RubyString newEmptyString(Ruby runtime, RubyClass metaClass, Encoding enc) {
        return RubyStringByteList.newEmptyString(runtime, metaClass, enc);
    }

    public static RubyString newEmptyString(Ruby runtime, Encoding enc) {
        return newEmptyString(runtime, runtime.getString(), enc);
    }

    public static RubyString newStringNoCopy(Ruby runtime, RubyClass clazz, ByteList bytes, Encoding enc, int cr) {
        return new RubyStringByteList(runtime, clazz, bytes, enc, cr);
    }

    public static RubyString newStringNoCopy(Ruby runtime, ByteList bytes, Encoding enc, int cr) {
        return newStringNoCopy(runtime, runtime.getString(), bytes, enc, cr);
    }

    public static RubyString newUsAsciiStringNoCopy(Ruby runtime, ByteList bytes) {
        return newStringNoCopy(runtime, bytes, USASCIIEncoding.INSTANCE, CR_7BIT);
    }

    public static RubyString newUsAsciiStringShared(Ruby runtime, ByteList bytes) {
        return RubyStringByteList.newUsAsciiStringShared(runtime, bytes);
    }

    public static RubyString newUsAsciiStringShared(Ruby runtime, byte[] bytes, int start, int length) {
        return RubyStringByteList.newUsAsciiStringShared(runtime, bytes, start, length);
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
    public abstract String decodeString();

    /**
     * Overridden dup for fast-path logic.
     *
     * @return A new RubyString sharing the original backing store.
     */
    @Override
    public IRubyObject dup() {
        return super.dup();
    }

    // rb_str_new_frozen or rb_str_dup_frozen
    public abstract IRubyObject dupFrozen();

    @JRubyMethod
    public IRubyObject dup(ThreadContext context) {
        return isBare(context) ? strDup(context.runtime) : super.dup(context);
    }

    // MRI: rb_str_dup
    public abstract RubyString strDup(Ruby runtime);

    public abstract RubyString strDup(Ruby runtime, RubyClass clazz);

    public abstract RubyString dupAsChilled(Ruby runtime, RubyClass clazz, String file, int line);

    public abstract FString dupAsFString(Ruby runtime);

    /* MRI: rb_str_subseq */
    public abstract RubyString makeSharedString(Ruby runtime, int index, int len);

    public abstract RubyString makeShared(Ruby runtime, int index, int len);

    public abstract RubyString makeShared(Ruby runtime, RubyClass meta, int index, int len);

    @Deprecated
    public abstract void setByteListShared();

    abstract void setBufferShared();

    /**
     * Check that the string can be modified, raising error otherwise.
     *
     * If you plan to modify a string with shared backing store, this
     * method is not sufficient; you will need to call modify() instead.
     */
    public final void modifyCheck() {
        frozenCheck();
    }

    public abstract void modifyCheck(byte[] b, int len);

    protected abstract void frozenCheck();

    @Override
    public void checkFrozen() {
        frozenCheck();
    }

    @Override
    public final void ensureInstanceVariablesSettable() {
        frozenCheck();
    }

    protected abstract boolean isChilled();

    protected abstract boolean isChilledLiteral();

    @Override
    public abstract void modify();

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
    public abstract void modify(int length);

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
    public abstract void ensureAvailable(ThreadContext context, int extraLength);

    // io_set_read_length
    public abstract void setReadLength(int length);

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
    public abstract RubyString newFrozen();

    public static RubyString newDebugFrozenString(Ruby runtime, RubyClass rubyClass, ByteList value, int cr, String file, int line) {
        return RubyStringByteList.newDebugFrozenString(runtime, rubyClass, value, cr, file, line);
    }

    public static RubyString newDebugChilledString(Ruby runtime, RubyClass rubyClass, ByteList value, int cr, String file, int line) {
        return RubyStringByteList.newDebugChilledString(runtime, rubyClass, value, cr, file, line);
    }

    /**
     * An FString is a frozen string that is also deduplicated and cached. We add a field to hold one type of conversion
     * so it won't be performed repeatedly. Whatever type of conversion is requested first wins, since it will be very
     * rare for a String to be converted to a Symbol and a Fixnum and a Float.
     */
    public static class FString extends RubyStringByteList {
        private IRubyObject converted;
        private final int hash;
        private final RubyFixnum fixHash;

        protected FString(Ruby runtime, ByteList value, int cr) {
            super(runtime, runtime.getString(), value, cr, false);

            this.shareLevel = RubyStringByteList.SHARE_LEVEL_BYTELIST;
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
    public abstract void resize(final int size);

    public abstract void view(ByteList bytes);

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
    public abstract RubyString to_s(ThreadContext context);

    @Override
    public final int compareTo(IRubyObject other) {
        var context = getRuntime().getCurrentContext();
        return toInt(context, op_cmp(context, other));
    }

    /* rb_str_cmp_m */
    @JRubyMethod(name = "<=>")
    @Override
    public abstract IRubyObject op_cmp(ThreadContext context, IRubyObject other);

    /** rb_str_equal
     *
     */
    @JRubyMethod(name = {"==", "==="})
    @Override
    public abstract IRubyObject op_equal(ThreadContext context, IRubyObject other);

    @JRubyMethod(name = {"-@", "dedup"}) // -'foo' returns frozen string
    public abstract IRubyObject minus_at(ThreadContext context);

    @Deprecated(since = "10.0.0.0")
    public final IRubyObject plus_at() {
        return plus_at(getCurrentContext());
    }

    @JRubyMethod(name = "+@") // +'foo' returns modifiable string
    public abstract IRubyObject plus_at(ThreadContext context);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject op_plus19(ThreadContext context, IRubyObject arg) {
        return op_plus(context, arg);
    }

    @JRubyMethod(name = "+")
    public abstract IRubyObject op_plus(ThreadContext context, IRubyObject arg);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject op_mul19(ThreadContext context, IRubyObject other) {
        return op_mul(context, other);
    }

    @JRubyMethod(name = "*")
    public abstract IRubyObject op_mul(ThreadContext context, IRubyObject other);

    @JRubyMethod(name = "%")
    public abstract RubyString op_format(ThreadContext context, IRubyObject arg);

    @JRubyMethod
    public abstract RubyFixnum hash(ThreadContext context);

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
    public abstract int strHashCode(Ruby runtime);

    /**
     * Generate a hash for the String, without a seed.
     *
     * @param runtime the runtime
     * @return calculated hash
     */
    public abstract int unseededStrHashCode(Ruby runtime);

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;

        return (other instanceof RubyString otherStr) && equals(otherStr);
    }

    abstract boolean equals(RubyString other);

    /** rb_obj_as_string
     *
     */
    public static RubyString objAsString(ThreadContext context, IRubyObject obj) {
        if (obj instanceof RubyString str) return str;
        IRubyObject str = sites(context).to_s.call(context, obj, obj);
        if (!(str instanceof RubyString)) return Convert.anyToString(context, obj);
        // TODO: MRI sets an fstring flag on fstrings and uses that flag here
        return (RubyString) str;
    }

    /** rb_str_cmp
     *
     */
    public abstract int op_cmp(RubyString other);

    /** rb_to_id
     *
     */
    @Override
    public String asJavaString() {
        return toString();
    }

    public abstract IRubyObject doClone();

    public abstract RubyString cat(byte[] str);

    public abstract RubyString cat(byte[] str, int beg, int len);

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

    public abstract RubyString cat(RubyString str);

    public abstract RubyString cat(ByteList str);

    public abstract RubyString cat(byte ch);

    public final RubyString cat(int ch) {
        return cat((byte) ch);
    }

    public abstract RubyString cat(int code, Encoding enc);

    // rb_enc_str_buf_cat
    public abstract int cat(byte[] bytes, int p, int len, Encoding enc);

    // rb_str_buf_cat_ascii
    public abstract RubyString catAscii(byte[] bytes, int ptr, int ptrLen);

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
    public abstract RubyString replace(ThreadContext context, IRubyObject other);;

    @Deprecated(since = "10.0.0.0")
    public RubyString clear() {
        return clear(getCurrentContext());
    }

    @JRubyMethod
    public abstract RubyString clear(ThreadContext context);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject reverse19(ThreadContext context) {
        return reverse(context);
    }

    @JRubyMethod(name = "reverse")
    public abstract IRubyObject reverse(ThreadContext context);

    @Deprecated(since = "10.0.0.0")
    public RubyString reverse_bang19(ThreadContext context) {
        return reverse_bang(context);
    }

    @JRubyMethod(name = "reverse!")
    public abstract RubyString reverse_bang(ThreadContext context);

    /** rb_str_s_new
     *
     */
    public static RubyString newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
        return RubyStringByteList.newInstance(recv, args, block);
    }

    @Override
    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public abstract IRubyObject initialize(ThreadContext context);

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public abstract IRubyObject initialize(ThreadContext context, IRubyObject arg0);

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public abstract IRubyObject initialize(ThreadContext context, IRubyObject arg0, IRubyObject opts);

    @JRubyMethod(name = "casecmp")
    public abstract IRubyObject casecmp(ThreadContext context, IRubyObject other);

    @JRubyMethod(name = "casecmp?")
    public abstract IRubyObject casecmp_p(ThreadContext context, IRubyObject other);

    /** rb_str_match
     *
     */

    @JRubyMethod(name = "=~", writes = BACKREF)
    @Override
    public abstract IRubyObject op_match(ThreadContext context, IRubyObject other);

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
    public abstract IRubyObject match(ThreadContext context, IRubyObject pattern, Block block);

    @JRubyMethod(name = "match", writes = BACKREF)
    public abstract IRubyObject match(ThreadContext context, IRubyObject pattern, IRubyObject pos, Block block);

    @JRubyMethod(name = "match", required = 1, rest = true, checkArity = false)
    public abstract IRubyObject match(ThreadContext context, IRubyObject[] args, Block block);

    @JRubyMethod(name = "match?")
    public abstract IRubyObject match_p(ThreadContext context, IRubyObject pattern);

    @JRubyMethod(name = "match?")
    public abstract IRubyObject match_p(ThreadContext context, IRubyObject pattern, IRubyObject pos);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject op_ge19(ThreadContext context, IRubyObject other) {
        return op_ge(context, other);
    }

    @JRubyMethod(name = ">=")
    public abstract IRubyObject op_ge(ThreadContext context, IRubyObject other);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject op_gt19(ThreadContext context, IRubyObject other) {
        return op_gt(context, other);
    }

    @JRubyMethod(name = ">")
    public abstract IRubyObject op_gt(ThreadContext context, IRubyObject other);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject op_le19(ThreadContext context, IRubyObject other) {
        return op_le(context, other);
    }

    @JRubyMethod(name = "<=")
    public abstract IRubyObject op_le(ThreadContext context, IRubyObject other);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject op_lt19(ThreadContext context, IRubyObject other) {
        return op_lt(context, other);
    }

    @JRubyMethod(name = "<")
    public abstract IRubyObject op_lt(ThreadContext context, IRubyObject other);

    @Deprecated(since = "10.0.0.0")
    public abstract IRubyObject str_eql_p19(ThreadContext context, IRubyObject other);

    @JRubyMethod(name = "eql?")
    public abstract IRubyObject str_eql_p(ThreadContext context, IRubyObject other);

    /** rb_str_upcase / rb_str_upcase_bang
     *
     */
    @JRubyMethod(name = "upcase")
    public abstract RubyString upcase(ThreadContext context);

    @JRubyMethod(name = "upcase")
    public abstract RubyString upcase(ThreadContext context, IRubyObject arg);

    @JRubyMethod(name = "upcase")
    public abstract RubyString upcase(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    @JRubyMethod(name = "upcase!")
    public abstract IRubyObject upcase_bang(ThreadContext context);

    @JRubyMethod(name = "upcase!")
    public abstract IRubyObject upcase_bang(ThreadContext context, IRubyObject arg);

    @JRubyMethod(name = "upcase!")
    public abstract IRubyObject upcase_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    /** rb_str_downcase / rb_str_downcase_bang
     *
     */
    @JRubyMethod(name = "downcase")
    public abstract RubyString downcase(ThreadContext context);

    @JRubyMethod(name = "downcase")
    public abstract RubyString downcase(ThreadContext context, IRubyObject arg);

    @JRubyMethod(name = "downcase")
    public abstract RubyString downcase(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    @JRubyMethod(name = "downcase!")
    public abstract IRubyObject downcase_bang(ThreadContext context);

    @JRubyMethod(name = "downcase!")
    public abstract IRubyObject downcase_bang(ThreadContext context, IRubyObject arg);

    @JRubyMethod(name = "downcase!")
    public abstract IRubyObject downcase_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    /** rb_str_swapcase / rb_str_swapcase_bang
     *
     */
    @JRubyMethod(name = "swapcase")
    public abstract RubyString swapcase(ThreadContext context);

    @JRubyMethod(name = "swapcase")
    public abstract RubyString swapcase(ThreadContext context, IRubyObject arg);

    @JRubyMethod(name = "swapcase")
    public abstract RubyString swapcase(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    @JRubyMethod(name = "swapcase!")
    public abstract IRubyObject swapcase_bang(ThreadContext context);

    @JRubyMethod(name = "swapcase!")
    public abstract IRubyObject swapcase_bang(ThreadContext context, IRubyObject arg);

    @JRubyMethod(name = "swapcase!")
    public abstract IRubyObject swapcase_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    /** rb_str_capitalize / rb_str_capitalize_bang
     *
     */
    @JRubyMethod(name = "capitalize")
    public abstract RubyString capitalize(ThreadContext context);

    @JRubyMethod(name = "capitalize")
    public abstract RubyString capitalize(ThreadContext context, IRubyObject arg);

    @JRubyMethod(name = "capitalize")
    public abstract RubyString capitalize(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    @JRubyMethod(name = "capitalize!")
    public abstract IRubyObject capitalize_bang(ThreadContext context);

    @JRubyMethod(name = "capitalize!")
    public abstract IRubyObject capitalize_bang(ThreadContext context, IRubyObject arg);

    @JRubyMethod(name = "capitalize!")
    public abstract IRubyObject capitalize_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject dump() {
        return dump(getCurrentContext());
    }

    /** rb_str_dump
     *
     */
    @JRubyMethod(name = "dump")
    public abstract IRubyObject dump(ThreadContext context);

    @JRubyMethod(name = "undump")
    public abstract IRubyObject undump(ThreadContext context);

    @JRubyMethod(name = "insert")
    public abstract IRubyObject insert(ThreadContext context, IRubyObject indexArg, IRubyObject arg);

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
    public abstract IRubyObject inspect(ThreadContext context);

    @Deprecated(since = "10.1.0.0")
    abstract RubyString inspect(final Ruby runtime);


    // MRI: rb_str_escape
    public static IRubyObject rbStrEscape(ThreadContext context, RubyString str) {
        if (str instanceof RubyStringByteList rsbl) {
            return RubyStringByteList.rbStrEscape(context, rsbl);
        }
        throw new RuntimeException("uknown string type: " + str.getClass().getName());
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

    public abstract int size();

    // MRI: rb_str_length
    @JRubyMethod(name = {"length", "size"})
    public abstract RubyFixnum rubyLength(final ThreadContext context);

    @JRubyMethod(name = "bytesize")
    public abstract RubyFixnum bytesize(ThreadContext context);

    @Deprecated(since = "10.0.0.0")
    public RubyFixnum bytesize() {
        return bytesize(getCurrentContext());
    }

    // CharSequence

    @Override
    public abstract int length();

    @Override
    public abstract char charAt(int offset);

    @Override
    public CharSequence subSequence(int start, int end) {
        IRubyObject subStr = substrEnc(getRuntime().getCurrentContext(), start, end - start);
        if (subStr.isNil()) {
            throw new StringIndexOutOfBoundsException("String index out of range: <" + start + ", " + end + ")");
        }
        return (RubyString) subStr;
    }

    /** rb_str_empty
     *
     */
    @JRubyMethod(name = "empty?")
    public abstract RubyBoolean empty_p(ThreadContext context);

    @JRubyAPI
    public abstract boolean isEmpty();

    public abstract void appendIntoString(RubyString target);

    /** rb_str_append
     *
     */
    public abstract RubyString append(IRubyObject other, Function<IRubyObject, RubyString> convert);

    public abstract RubyString append(IRubyObject other);

    public abstract RubyString append(RubyString other);

    @Deprecated(since = "9.4.4.0")
    public RubyString append19(IRubyObject other) {
        return append(other);
    }

    public abstract RubyString appendAsDynamicString(IRubyObject other);

    public abstract RubyString appendAsStringOrAny(IRubyObject other);

    /** rb_str_concat
     *
     */
    @JRubyMethod(name = "<<")
    public abstract RubyString concatSingle(ThreadContext context, IRubyObject other);

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

    public abstract RubyString concat(IRubyObject other);

    /**
     * rb_str_prepend
     */
    @JRubyMethod
    public IRubyObject prepend(ThreadContext context, IRubyObject other){
        RubyString rubyString = other.convertToString();
        return replace(context, rubyString.op_plus(context, this));
    }

    /**
     * rb_str_prepend
     */
    @JRubyMethod(rest = true)
    public abstract IRubyObject prepend(ThreadContext context, IRubyObject[] objs);

    public abstract RubyString prepend(byte ch);

    public abstract RubyString prepend(int ch);

    /** rb_str_crypt
     *
     */
    @JRubyMethod(name = "crypt")
    public abstract RubyString crypt(ThreadContext context, IRubyObject other);

    /* RubyString aka rb_string_value */
    public static RubyString stringValue(IRubyObject object) {
        return (RubyString) (object instanceof RubyString ? object : object.convertToString());
    }

    /** rb_str_sub / rb_str_sub_bang
     *
     */

    @JRubyMethod(name = "sub", writes = BACKREF)
    public abstract IRubyObject sub(ThreadContext context, IRubyObject arg0, Block block);

    @JRubyMethod(name = "sub", writes = BACKREF)
    public abstract IRubyObject sub(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block);

    @JRubyMethod(name = "sub!", writes = BACKREF)
    public abstract IRubyObject sub_bang(ThreadContext context, IRubyObject arg0, Block block);

    @JRubyMethod(name = "sub!", writes = BACKREF)
    public abstract IRubyObject sub_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block);

    /**
     * sub! but without any frame globals ...
     * @param context current context
     * @param regexp the regular expression
     * @param repl replacement string value
     * @return sub result
     */
    public abstract IRubyObject subBangFast(ThreadContext context, RubyRegexp regexp, RubyString repl);;

    @JRubyMethod(name = "gsub", writes = BACKREF)
    public abstract IRubyObject gsub(ThreadContext context, IRubyObject arg0, Block block);

    @JRubyMethod(name = "gsub", writes = BACKREF)
    public abstract IRubyObject gsub(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block);

    @JRubyMethod(name = "gsub!", writes = BACKREF)
    public abstract IRubyObject gsub_bang(ThreadContext context, IRubyObject arg0, Block block);

    @JRubyMethod(name = "gsub!", writes = BACKREF)
    public abstract IRubyObject gsub_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block);

    public abstract RubyString gsubFast(ThreadContext context, RubyRegexp regexp, RubyString repl, Block block);

    /** rb_str_index_m
     *
     */
    @JRubyMethod(name = "index", writes = BACKREF)
    public abstract IRubyObject index(ThreadContext context, IRubyObject arg0);

    @JRubyMethod(name = "index", writes = BACKREF)
    public abstract IRubyObject index(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    @JRubyMethod(writes = BACKREF)
    public abstract IRubyObject byteindex(ThreadContext context, IRubyObject arg0);

    @JRubyMethod(writes = BACKREF)
    public abstract IRubyObject byteindex(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    /** rb_str_rindex_m
     *
     */
    @JRubyMethod(name = "rindex", writes = BACKREF)
    public abstract IRubyObject rindex(ThreadContext context, IRubyObject arg0);

    @JRubyMethod(name = "rindex", writes = BACKREF)
    public abstract IRubyObject rindex(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    @JRubyMethod(writes = BACKREF)
    public abstract IRubyObject byterindex(ThreadContext context, IRubyObject arg0);

    @JRubyMethod(writes = BACKREF)
    public abstract IRubyObject byterindex(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    @Deprecated(since = "9.4-") //2008
    public final IRubyObject substr(int beg, int len) {
        return substr(getCurrentContext(), beg, len);
    }

    @Deprecated(since = "10.0.0.0")
    public final IRubyObject substr(Ruby runtime, int beg, int len) {
        return substr(getCurrentContext(), beg, len);
    }

    /* rb_str_substr */
    public abstract IRubyObject substr(ThreadContext context, int beg, int len);

    @Deprecated(since = "10.0.0.0")
    public final IRubyObject substr19(Ruby runtime, int beg, int len) {
        return substrEnc(getCurrentContext(), beg, len);
    }

    @Deprecated(since = "10.0.0.0")
    public final IRubyObject substrEnc(Ruby runtime, int beg, int len) {
        return substrEnc(getCurrentContext(), beg, len);
    }

    public abstract IRubyObject substrEnc(ThreadContext context, int beg, int len);

    /** rb_str_aref, rb_str_aref_m
     *
     */
    @JRubyMethod(name = {"[]", "slice"}, writes = BACKREF)
    public abstract IRubyObject op_aref(ThreadContext context, IRubyObject arg);

    @JRubyMethod(name = {"[]", "slice"}, writes = BACKREF)
    public abstract IRubyObject op_aref(ThreadContext context, IRubyObject arg1, IRubyObject arg2);

    @JRubyMethod
    public abstract IRubyObject byteslice(ThreadContext context, IRubyObject arg1, IRubyObject arg2);

    @JRubyMethod
    public abstract IRubyObject byteslice(ThreadContext context, IRubyObject arg);

    @JRubyMethod(required = 2, optional = 3, checkArity = false)
    public abstract IRubyObject bytesplice(ThreadContext context, IRubyObject[] args);

    @JRubyMethod
    public abstract IRubyObject bytesplice(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    @JRubyMethod
    public abstract IRubyObject bytesplice(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2);

    // MRI: at_char_boundary
    public static boolean atCharBoundary(byte[] bytes, int s, int p, int e, Encoding enc) {
        // our version checks if p == bytes.length, where CRuby would have a \0 character and not reposition
        return p == bytes.length || enc.leftAdjustCharHead(bytes, s, p, e) == p;
    }

    /** rb_str_aset, rb_str_aset_m
     *
     */
    @JRubyMethod(name = "[]=", writes = BACKREF)
    public abstract IRubyObject op_aset(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    @JRubyMethod(name = "[]=", writes = BACKREF)
    public abstract IRubyObject op_aset(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2);

    /** rb_str_slice_bang
     *
     */
    @JRubyMethod(name = "slice!", writes = BACKREF)
    public abstract IRubyObject slice_bang(ThreadContext context, IRubyObject arg0);

    @JRubyMethod(name = "slice!", writes = BACKREF)
    public abstract IRubyObject slice_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    @JRubyMethod(name = {"succ", "next"})
    public abstract IRubyObject succ(ThreadContext context);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject succ_bang() {
        return succ_bang(getCurrentContext());
    }

    @JRubyMethod(name = {"succ!", "next!"})
    public abstract IRubyObject succ_bang(ThreadContext context);

    /** rb_str_upto_m
     *
     */
    @JRubyMethod(name = "upto")
    public abstract IRubyObject upto(ThreadContext context, IRubyObject end, Block block);

    @JRubyMethod(name = "upto")
    public abstract IRubyObject upto(ThreadContext context, IRubyObject end, IRubyObject excl, Block block);

    abstract IRubyObject uptoCommon(ThreadContext context, IRubyObject arg, boolean excl, Block block);

    abstract IRubyObject uptoCommon(ThreadContext context, RubyString end, boolean excl, Block block, boolean asSymbol);

    abstract IRubyObject uptoEndless(ThreadContext context, Block block);

    /** rb_str_include
     *
     */
    @JRubyMethod(name = "include?")
    public abstract RubyBoolean include_p(ThreadContext context, IRubyObject obj);

    @JRubyMethod
    public abstract IRubyObject chr(ThreadContext context);

    @JRubyMethod
    public abstract IRubyObject getbyte(ThreadContext context, IRubyObject index);

    @JRubyMethod
    public abstract IRubyObject setbyte(ThreadContext context, IRubyObject index, IRubyObject val);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject to_i() {
        return to_i(getCurrentContext());
    }

    // MRI: rb_str_to_i
    @JRubyMethod(name = "to_i")
    public abstract IRubyObject to_i(ThreadContext context);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject to_i(IRubyObject arg0) {
        return to_i(getCurrentContext(), arg0);
    }

    /** rb_str_to_i
     *
     */
    @JRubyMethod(name = "to_i")
    public abstract IRubyObject to_i(ThreadContext context, IRubyObject arg0);

    /** rb_str_to_inum
     *
     */
    public abstract IRubyObject stringToInum(int base, boolean badcheck);

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
    public abstract IRubyObject to_f(ThreadContext context);

    public abstract RubyArray split(ThreadContext context);

    public abstract RubyArray split(ThreadContext context, IRubyObject arg0);

    /** rb_str_split_m
     *
     */
    @JRubyMethod(name = "split")
    public abstract IRubyObject splitWithBlock(ThreadContext context, Block block);

    @JRubyMethod(name = "split")
    public abstract IRubyObject splitWithBlock(ThreadContext context, IRubyObject arg0, Block block);

    public abstract RubyArray split(ThreadContext context, IRubyObject pattern, IRubyObject limit);

    @JRubyMethod(name = "split")
    public abstract IRubyObject splitWithBlock(ThreadContext context, IRubyObject pattern, IRubyObject limit, Block block);

    /**
     * Split for ext (Java) callers (does not write $~).
     * @param delimiter
     * @return splited entries
     */
    @Deprecated(since = "10.0.0.0")
    public abstract RubyArray split(RubyRegexp delimiter);

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
    public abstract RubyArray split(ThreadContext context, RubyRegexp delimiter, int limit);

    @Deprecated(since = "10.0.0.0")
    public RubyArray split(RubyString delimiter) {
        return split(getCurrentContext(), delimiter);
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
    public abstract RubyArray split(ThreadContext context, RubyString delimiter, int limit);;

    public RubyClass singletonClass(ThreadContext context) {
        return super.singletonClass(context);
    }

    /** rb_str_scan
     *
     */
    @JRubyMethod(name = "scan", writes = BACKREF)
    public abstract IRubyObject scan(ThreadContext context, IRubyObject pat, Block block);

    @JRubyMethod(name = "start_with?")
    public abstract IRubyObject start_with_p(ThreadContext context);

    @JRubyMethod(name = "start_with?")
    public abstract IRubyObject start_with_p(ThreadContext context, IRubyObject arg);

    @JRubyMethod(name = "start_with?", rest = true)
    public abstract IRubyObject start_with_p(ThreadContext context, IRubyObject[]args);

    public abstract boolean startsWith(final RubyString str);

    @JRubyMethod(name = "end_with?")
    public abstract IRubyObject end_with_p(ThreadContext context);

    @JRubyMethod(name = "end_with?")
    public abstract IRubyObject end_with_p(ThreadContext context, IRubyObject arg);

    @JRubyMethod(name = "end_with?", rest = true)
    public abstract IRubyObject end_with_p(ThreadContext context, IRubyObject[]args);

    protected abstract boolean endWith(IRubyObject tmp);

    public abstract boolean endsWithAsciiChar(char c);

    @JRubyMethod(name = "delete_prefix")
    public abstract IRubyObject delete_prefix(ThreadContext context, IRubyObject prefix);

    @JRubyMethod(name = "delete_suffix")
    public abstract IRubyObject delete_suffix(ThreadContext context, IRubyObject suffix);

    @JRubyMethod(name = "delete_prefix!")
    public abstract IRubyObject delete_prefix_bang(ThreadContext context, IRubyObject prefix);

    @JRubyMethod(name = "delete_suffix!")
    public abstract IRubyObject delete_suffix_bang(ThreadContext context, IRubyObject suffix);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject ljust(IRubyObject arg0) {
        return ljust(getCurrentContext(), arg0);
    }

    /** rb_str_ljust
     *
     */
    @JRubyMethod(name = "ljust")
    public abstract IRubyObject ljust(ThreadContext context, IRubyObject arg0);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject ljust(IRubyObject arg0, IRubyObject arg1) {
        return ljust(getCurrentContext(), arg0, arg1);
    }

    @JRubyMethod(name = "ljust")
    public abstract IRubyObject ljust(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject rjust(IRubyObject arg0) {
        return rjust(getCurrentContext(), arg0);
    }

    /** rb_str_rjust
     *
     */
    @JRubyMethod(name = "rjust")
    public abstract IRubyObject rjust(ThreadContext context, IRubyObject arg0);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject rjust(IRubyObject arg0, IRubyObject arg1) {
        return rjust(getCurrentContext(), arg0, arg1);
    }

    @JRubyMethod(name = "rjust")
    public abstract IRubyObject rjust(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject center(IRubyObject arg0) {
        return center(getCurrentContext(), arg0);
    }

    /** rb_str_center
     *
     */
    @JRubyMethod(name = "center")
    public abstract IRubyObject center(ThreadContext context, IRubyObject arg0);

    @Deprecated(since = "10.0.0.0")
    public IRubyObject center(IRubyObject arg0, IRubyObject arg1) {
        return center(getCurrentContext(), arg0, arg1);
    }

    @JRubyMethod(name = "center")
    public abstract IRubyObject center(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    @JRubyMethod(reads = BACKREF, writes = BACKREF)
    public abstract IRubyObject partition(ThreadContext context, Block block);

    @JRubyMethod(reads = BACKREF, writes = BACKREF)
    public abstract IRubyObject partition(ThreadContext context, IRubyObject arg, Block block);

    @JRubyMethod(name = "rpartition", writes = BACKREF)
    public abstract IRubyObject rpartition(ThreadContext context, IRubyObject arg);

    @JRubyMethod(rest = true)
    public abstract IRubyObject append_as_bytes(ThreadContext context, IRubyObject[] args);

    @JRubyMethod
    public abstract IRubyObject append_as_bytes(ThreadContext context);

    @JRubyMethod
    public abstract IRubyObject append_as_bytes(ThreadContext context, IRubyObject arg0);

    /** rb_str_chop / rb_str_chop_bang
     *
     */
    @JRubyMethod(name = "chop")
    public abstract IRubyObject chop(ThreadContext context);

    @JRubyMethod(name = "chop!")
    public abstract IRubyObject chop_bang(ThreadContext context);

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
    public abstract RubyString chomp(ThreadContext context);

    @JRubyMethod(name = "chomp")
    public abstract RubyString chomp(ThreadContext context, IRubyObject arg0);

    @JRubyMethod(name = "chomp!")
    public abstract IRubyObject chomp_bang(ThreadContext context);

    @JRubyMethod(name = "chomp!")
    public abstract IRubyObject chomp_bang(ThreadContext context, IRubyObject arg0);

    /** rb_str_lstrip / rb_str_lstrip_bang
     *
     */
    @JRubyMethod(name = "lstrip")
    public abstract IRubyObject lstrip(ThreadContext context);

    @JRubyMethod(name = "lstrip!")
    public abstract IRubyObject lstrip_bang(ThreadContext context);

    /** rb_str_rstrip / rb_str_rstrip_bang
     *
     */
    @JRubyMethod(name = "rstrip")
    public abstract IRubyObject rstrip(ThreadContext context);

    @JRubyMethod(name = "rstrip!")
    public abstract IRubyObject rstrip_bang(ThreadContext context);

    /** rb_str_strip / rb_str_strip_bang
     *
     */
    @JRubyMethod(name = "strip")
    public abstract IRubyObject strip(ThreadContext context);

    @JRubyMethod(name = "strip!")
    public abstract IRubyObject strip_bang(ThreadContext context);

    @JRubyMethod(name = "count")
    public IRubyObject count(ThreadContext context) {
        throw argumentError(context, "wrong number of arguments");
    }

    // MRI: rb_str_count, first half
    @JRubyMethod(name = "count")
    public abstract IRubyObject count(ThreadContext context, IRubyObject arg);

    // MRI: rb_str_count for arity > 1, first half
    @JRubyMethod(name = "count", required = 1, rest = true, checkArity = false)
    public abstract IRubyObject count(ThreadContext context, IRubyObject[] args);

    /** rb_str_delete / rb_str_delete_bang
     *
     */

    @JRubyMethod(name = "delete")
    public abstract IRubyObject delete(ThreadContext context);

    @JRubyMethod(name = "delete")
    public abstract IRubyObject delete(ThreadContext context, IRubyObject arg);

    @JRubyMethod(name = "delete", required = 1, rest = true, checkArity = false)
    public abstract IRubyObject delete(ThreadContext context, IRubyObject[] args);

    @JRubyMethod(name = "delete!")
    public abstract IRubyObject delete_bang(ThreadContext context);

    @JRubyMethod(name = "delete!")
    public abstract IRubyObject delete_bang(ThreadContext context, IRubyObject arg);

    @JRubyMethod(name = "delete!", required = 1, rest = true, checkArity = false)
    public abstract IRubyObject delete_bang(ThreadContext context, IRubyObject[] args);

    /** rb_str_squeeze / rb_str_squeeze_bang
     *
     */

    @JRubyMethod(name = "squeeze")
    public abstract IRubyObject squeeze(ThreadContext context);

    @JRubyMethod(name = "squeeze")
    public abstract IRubyObject squeeze(ThreadContext context, IRubyObject arg);

    @JRubyMethod(name = "squeeze", required = 1, rest = true, checkArity = false)
    public abstract IRubyObject squeeze(ThreadContext context, IRubyObject[] args);

    @JRubyMethod(name = "squeeze!")
    public abstract IRubyObject squeeze_bang(ThreadContext context);

    @JRubyMethod(name = "squeeze!")
    public abstract IRubyObject squeeze_bang(ThreadContext context, IRubyObject arg);

    @JRubyMethod(name = "squeeze!", required = 1, rest = true, checkArity = false)
    public abstract IRubyObject squeeze_bang(ThreadContext context, IRubyObject[] args);

    /** rb_str_tr / rb_str_tr_bang
     *
     */
    @JRubyMethod(name = "tr")
    public abstract IRubyObject tr(ThreadContext context, IRubyObject src, IRubyObject repl);

    @JRubyMethod(name = "tr!")
    public abstract IRubyObject tr_bang(ThreadContext context, IRubyObject src, IRubyObject repl);

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
    public abstract IRubyObject tr_s(ThreadContext context, IRubyObject src, IRubyObject repl);

    @JRubyMethod(name = "tr_s!")
    public abstract IRubyObject tr_s_bang(ThreadContext context, IRubyObject src, IRubyObject repl);

    /** rb_str_each_line
     *
     */
    @JRubyMethod(name = "each_line")
    public abstract IRubyObject each_line(ThreadContext context, Block block);

    @JRubyMethod(name = "each_line")
    public abstract IRubyObject each_line(ThreadContext context, IRubyObject arg, Block block);

    @JRubyMethod(name = "each_line")
    public abstract IRubyObject each_line(ThreadContext context, IRubyObject arg, IRubyObject opts, Block block);

    @JRubyMethod(name = "lines")
    public abstract IRubyObject lines(ThreadContext context, Block block);

    @JRubyMethod(name = "lines")
    public abstract IRubyObject lines(ThreadContext context, IRubyObject arg, Block block);

    @JRubyMethod(name = "lines")
    public abstract IRubyObject lines(ThreadContext context, IRubyObject arg, IRubyObject opts, Block block);

    /**
     * rb_str_each_byte
     */
    @JRubyMethod(name = "each_byte")
    public abstract IRubyObject each_byte(ThreadContext context, Block block);

    @JRubyMethod
    public abstract IRubyObject bytes(ThreadContext context, Block block);

    @JRubyMethod(name = "each_char")
    public abstract IRubyObject each_char(ThreadContext context, Block block);

    @JRubyMethod(name = "chars")
    public abstract IRubyObject chars(ThreadContext context, Block block);

    /** rb_str_each_codepoint
     *
     */
    @JRubyMethod
    public abstract IRubyObject each_codepoint(ThreadContext context, Block block);

    @JRubyMethod
    public abstract IRubyObject codepoints(ThreadContext context, Block block);;

    @JRubyMethod
    public abstract IRubyObject grapheme_clusters(ThreadContext context, Block block);

    @JRubyMethod
    public abstract IRubyObject each_grapheme_cluster(ThreadContext context, Block block);

    @Deprecated(since = "10.0.0.0")
    public RubySymbol intern() {
        return intern(getCurrentContext());
    }

    // MRI: rb_str_intern
    @JRubyMethod(name = {"to_sym", "intern"})
    public abstract RubySymbol intern(ThreadContext context);

    @JRubyMethod
    public abstract IRubyObject ord(ThreadContext context);

    @JRubyMethod
    public abstract IRubyObject sum(ThreadContext context);

    @JRubyMethod
    public abstract IRubyObject sum(ThreadContext context, IRubyObject arg);

    public abstract IRubyObject sumCommon(ThreadContext context, long bits);

    /** string_to_c
     *
     */
    @JRubyMethod
    public abstract IRubyObject to_c(ThreadContext context);

    /** string_to_r
     *
     */
    @JRubyMethod
    public abstract IRubyObject to_r(ThreadContext context);

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
    public abstract RubyArray unpack(ThreadContext context, IRubyObject obj, Block block);

    @JRubyMethod
    public abstract RubyArray unpack(ThreadContext context, IRubyObject obj, IRubyObject opt, Block block);

    @JRubyMethod
    public abstract IRubyObject unpack1(ThreadContext context, IRubyObject obj, Block block);

    @JRubyMethod
    public abstract IRubyObject unpack1(ThreadContext context, IRubyObject obj, IRubyObject opt, Block block);

    public abstract void empty();

    @JRubyMethod
    public abstract IRubyObject encoding(ThreadContext context);

    @JRubyMethod(name = "encode!")
    public abstract IRubyObject encode_bang(ThreadContext context);

    @JRubyMethod(name = "encode!")
    public abstract IRubyObject encode_bang(ThreadContext context, IRubyObject arg0);

    @JRubyMethod(name = "encode!")
    public abstract IRubyObject encode_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1);

    @JRubyMethod(name = "encode!")
    public abstract IRubyObject encode_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2);

    @JRubyMethod
    public abstract IRubyObject encode(ThreadContext context);

    @JRubyMethod
    public abstract IRubyObject encode(ThreadContext context, IRubyObject arg);

    @JRubyMethod
    public abstract IRubyObject encode(ThreadContext context, IRubyObject toEncoding, IRubyObject arg);

    @JRubyMethod
    public abstract IRubyObject encode(ThreadContext context, IRubyObject toEncoding,
            IRubyObject forcedEncoding, IRubyObject opts);

    @JRubyMethod
    public abstract IRubyObject force_encoding(ThreadContext context, IRubyObject enc);

    @JRubyMethod(name = "valid_encoding?")
    public abstract IRubyObject valid_encoding_p(ThreadContext context);

    @JRubyMethod(name = "ascii_only?")
    public abstract IRubyObject ascii_only_p(ThreadContext context);

    @JRubyMethod
    public abstract IRubyObject b(ThreadContext context);

    // MRI: str_scrub arity 0
    @JRubyMethod
    public abstract IRubyObject scrub(ThreadContext context, Block block);

    // MRI: str_scrub arity 1
    @JRubyMethod
    public abstract IRubyObject scrub(ThreadContext context, IRubyObject repl, Block block);

    @JRubyMethod(name="scrub!")
    public abstract IRubyObject scrub_bang(ThreadContext context, Block block);

    // MRI: str_scrub arity 1
    @JRubyMethod(name="scrub!")
    public abstract IRubyObject scrub_bang(ThreadContext context, IRubyObject repl, Block block);

    @JRubyMethod @JRubyAPI
    public IRubyObject freeze(ThreadContext context) {
        return super.freeze(context);
    }

    public abstract RubyString chill();

    public abstract RubyString chill_symbol_string();

    @Deprecated(since = "10.0.0.0")
    public abstract void setValue(CharSequence value);

    public abstract void setValue(ByteList value);

    public CharSequence getValue() {
        return toString();
    }

    public abstract byte[] getBytes();

    /**
     * Get the ByteList which backs this Ruby String
     * @return The byte list
     */
    @JRubyAPI
    public abstract ByteList getByteList();

    /** used by ar-jdbc
     *
     */
    public abstract String getUnicodeValue();

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
        return super.toJava(target);
    }

    /**
     * Scrub the contents of this string, replacing invalid characters as appropriate.
     *
     * MRI: rb_str_scrub
     */
    public abstract IRubyObject strScrub(ThreadContext context, IRubyObject repl, Block block);

    // MRI: rb_enc_str_scrub
    public abstract IRubyObject encStrScrub(ThreadContext context, Encoding enc, IRubyObject repl, Block block);

    // MRI: enc_str_scrub
    public abstract IRubyObject encStrScrub(ThreadContext context, Encoding enc, IRubyObject repl, int cr, Block block);

    // MRI: rb_str_offset
    public abstract int rbStrOffset(int pos);

    // MRI: rb_str_include_range_p
    public static IRubyObject includeRange(ThreadContext context, RubyString _beg, RubyString _end, IRubyObject _val, boolean exclusive) {
        if (!(_beg instanceof RubyStringByteList beg && _end instanceof RubyStringByteList end)) {
            throw new RuntimeException("unexpected string type for beg or end: " + _beg.getClass() + ", " + _end.getClass());
        }
        return RubyStringByteList.includeRange(context, beg, end, _val, exclusive);
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

    /** Deprecated but still used by JIT, see {@link org.jruby.ir.targets.indy.ArrayDerefInvokeSite#STRDUP_FILTER} */
    @Deprecated(since = "9.0.0.0")
    public final RubyString strDup() {
        return strDup(getRuntime(), getMetaClass().getRealClass());
    }

    @Deprecated(since = "9.4.6.0") // not used
    public RubyArray unpack(IRubyObject obj) {
        return unpack(getCurrentContext(), obj, Block.NULL_BLOCK);
    }

    @Deprecated(since = "9.4.6.0")
    public IRubyObject encode_bang(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 0:
                return encode_bang(context);
            case 1:
                return encode_bang(context, args[0]);
            case 2:
                return encode_bang(context, args[0], args[1]);
            case 3:
                return encode_bang(context, args[0], args[1], args[2]);
            default:
                throw getRuntime().newArgumentError("wrong number of arguments (given " + args.length + ", expected 0, 1, 2 or 3)");
        }
    }

}
