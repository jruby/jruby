package org.jruby.api;

import org.jcodings.Encoding;
import org.jcodings.transcode.EConv;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyEncoding;
import org.jruby.RubyHash;
import org.jruby.RubyString;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.ByteListHolder;
import org.jruby.util.io.EncodingUtils;
import org.jruby.util.io.IOEncodable;

public class MRI {
    public static int rb_pipe(ThreadContext context, int[] pipes) {
        return API.newPipe(context, pipes);
    }

    public static int rb_cloexec_pipe(ThreadContext context, int[] pipes) {
        return API.cloexecPipe(context, pipes);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Encoding-related functions
    ///////////////////////////////////////////////////////////////////////////

    public static Encoding rb_to_encoding(ThreadContext context, IRubyObject enc) {
        return EncodingUtils.rbToEncoding(context, enc);
    }

    public static Encoding rb_ascii8bit_encoding(Ruby runtime) {
        return EncodingUtils.ascii8bitEncoding(runtime);
    }

    public static void rb_io_extract_modeenc(ThreadContext context,
                                             IOEncodable ioEncodable, API.ModeAndPermission vmodeAndVperm_p, IRubyObject options, int[] oflags_p, int[] fmode_p) {
        EncodingUtils.extractModeEncoding(context, ioEncodable, vmodeAndVperm_p, options, oflags_p, fmode_p);
    }

    public static boolean rb_io_extract_encoding_option(ThreadContext context, IOEncodable ioEncodable, IRubyObject options, int[] fmode_p) {
        return EncodingUtils.ioExtractEncodingOption(context, ioEncodable, options, fmode_p);
    }

    public static RubyString rb_external_str_new_with_enc(Ruby runtime, String string, Encoding encoding) {
        return EncodingUtils.newExternalStringWithEncoding(runtime, string, encoding);
    }

    public static RubyString rb_external_str_new_with_enc(Ruby runtime, ByteList bytelist, Encoding encoding) {
        return EncodingUtils.newExternalStringWithEncoding(runtime, bytelist, encoding);
    }

    public static ByteList rb_econv_str_convert(ThreadContext context, EConv ec, ByteList src, int flags) {
        return EncodingUtils.econvStrConvert(context, ec, src, flags);
    }

    public static ByteList rb_econv_str_convert(ThreadContext context, EConv ec, byte[] bytes, int start, int length, int flags) {
        return EncodingUtils.econvByteConvert(context, ec, bytes, start, length, flags);
    }

    public static ByteList rb_econv_substr_append(ThreadContext context, EConv ec, ByteList src, ByteList dst, int flags) {
        return EncodingUtils.econvSubstrAppend(context, ec, src, dst, flags);
    }

    public static ByteList rb_econv_append(ThreadContext context, EConv ec, ByteList sByteList, ByteList dst, int flags) {
        return EncodingUtils.econvAppend(context, ec, sByteList, dst, flags);
    }

    public static ByteList rb_econv_append(ThreadContext context, EConv ec, byte[] bytes, int start, int length, ByteList dst, int flags) {
        return EncodingUtils.econvAppend(context, ec, bytes, start, length, dst, flags);
    }

    public static void rb_econv_check_error(ThreadContext context, EConv ec) {
        EncodingUtils.econvCheckError(context, ec);
    }

    public static int rb_econv_prepare_opts(ThreadContext context, IRubyObject opthash, IRubyObject[] opts) {
        return EncodingUtils.econvPrepareOpts(context, opthash, opts);
    }

    public static int rb_econv_prepare_options(ThreadContext context, IRubyObject opthash, IRubyObject[] opts, int ecflags) {
        return EncodingUtils.econvPrepareOptions(context, opthash, opts, ecflags);
    }

    public static EConv rb_econv_open_opts(ThreadContext context, byte[] sourceEncoding, byte[] destinationEncoding, int ecflags, IRubyObject opthash) {
        return EncodingUtils.econvOpenOpts(context, sourceEncoding, destinationEncoding, ecflags, opthash);
    }

    public static RaiseException rb_econv_open_exc(ThreadContext context, byte[] sourceEncoding, byte[] destinationEncoding, int ecflags) {
        return EncodingUtils.econvOpenExc(context, sourceEncoding, destinationEncoding, ecflags);
    }

    public static Encoding rb_econv_asciicompat_encoding(Encoding enc) {
        return EncodingUtils.econvAsciicompatEncoding(enc);
    }

    public static boolean rb_enc_asciicompat(Encoding enc) {
        return EncodingUtils.encAsciicompat(enc);
    }

    public static int rb_enc_ascget(byte[] pBytes, int p, int e, int[] len, Encoding enc) {
        return EncodingUtils.encAscget(pBytes, p, e, len, enc);
    }

    public static int rb_enc_mbminlen(Encoding encoding) {
        return EncodingUtils.encMbminlen(encoding);
    }

    public static boolean rb_enc_dummy_p(Encoding enc) {
        return EncodingUtils.encDummy(enc);
    }

    public static Encoding rb_enc_get(ThreadContext context, IRubyObject obj) {
        return EncodingUtils.encGet(context, obj);
    }

    public static Encoding rb_to_encoding_index(ThreadContext context, IRubyObject enc) {
        return EncodingUtils.toEncodingIndex(context, enc);
    }

    public static IRubyObject rb_enc_associate_index(IRubyObject obj, Encoding encidx) {
        return EncodingUtils.encAssociateIndex(obj, encidx);
    }

    public static IRubyObject rb_str_encode(ThreadContext context, IRubyObject str, IRubyObject to, int ecflags, IRubyObject ecopt) {
        return EncodingUtils.rbStrEncode(context, str, to, ecflags, ecopt);
    }

    public static ByteList rb_str_encode(ThreadContext context, byte[] bytes, int start, int length, Encoding encoding, int cr, Encoding to, int ecflags, IRubyObject ecopt) {
        return EncodingUtils.rbByteEncode(context, bytes, start, length, encoding, cr, to, ecflags, ecopt);
    }

    public static IRubyObject rb_obj_encoding(ThreadContext context, IRubyObject obj) {
        return EncodingUtils.objEncoding(context, obj);
    }

    public static Encoding rb_define_dummy_encoding(ThreadContext context, byte[] name) {
        return EncodingUtils.defineDummyEncoding(context, name);
    }

    public static RubyString rb_str_conv_enc_opts(ThreadContext context, RubyString str, Encoding fromEncoding,
                                            Encoding toEncoding, int ecflags, IRubyObject ecopts) {
        return EncodingUtils.strConvEncOpts(context, str, fromEncoding, toEncoding, ecflags, ecopts);
    }

    public static RubyString rb_str_conv_enc(ThreadContext context, RubyString value, Encoding fromEncoding, Encoding toEncoding) {
        return EncodingUtils.strConvEnc(context, value, fromEncoding, toEncoding);
    }

    public static ByteList rb_str_conv_enc(ThreadContext context, ByteList value, Encoding fromEncoding, Encoding toEncoding) {
        return EncodingUtils.strConvEnc(context, value, fromEncoding, toEncoding);
    }

    public static void rb_enc_set_default_external(ThreadContext context, IRubyObject encoding) {
        EncodingUtils.rbEncSetDefaultExternal(context, encoding);
    }

    public static void rb_enc_set_default_internal(ThreadContext context, IRubyObject encoding) {
        EncodingUtils.rbEncSetDefaultInternal(context, encoding);
    }

    public static Encoding rb_default_external_encoding(ThreadContext context) {
        return EncodingUtils.defaultExternalEncoding(context.runtime);
    }

    public static void  rb_str_buf_cat(Ruby runtime, RubyString str, ByteList ptr) {
        EncodingUtils.rbStrBufCat(runtime, str, ptr);
    }

    public static void  rb_str_buf_cat(Ruby runtime, ByteListHolder str, byte[] ptrBytes, int ptr, int len) {
        EncodingUtils.rbStrBufCat(runtime, str, ptrBytes, ptr, len);
    }

    public static void  rb_str_buf_cat(Ruby runtime, ByteList str, byte[] ptrBytes, int ptr, int len) {
        EncodingUtils.rbStrBufCat(runtime, str, ptrBytes, ptr, len);
    }

    public static void rb_enc_str_buf_cat(ThreadContext context, RubyString str, ByteList ptr, Encoding enc) {
        EncodingUtils.encStrBufCat(context.runtime, str, ptr, enc);
    }

    public static void rb_enc_str_buf_cat(ThreadContext context, RubyString str, ByteList ptr) {
        EncodingUtils.encStrBufCat(context.runtime, str, ptr);
    }

    public static void rb_enc_str_buf_cat(ThreadContext context, RubyString str, byte[] ptrBytes) {
        EncodingUtils.encStrBufCat(context.runtime, str, ptrBytes);
    }

    public static void rb_enc_str_buf_cat(ThreadContext context, RubyString str, byte[] ptrBytes, Encoding enc) {
        EncodingUtils.encStrBufCat(context.runtime, str, ptrBytes, enc);
    }

    public static void rb_enc_str_buf_cat(ThreadContext context, RubyString str, byte[] ptrBytes, int ptr, int len, Encoding enc) {
        EncodingUtils.encStrBufCat(context.runtime, str, ptrBytes, ptr, len, enc);
    }

    public static void rb_enc_str_buf_cat(ThreadContext context, RubyString str, CharSequence cseq) {
        EncodingUtils.encStrBufCat(context.runtime, str, cseq);
    }

    public static RubyString rb_enc_uint_chr(ThreadContext context, int code, Encoding enc) {
        return EncodingUtils.encUintChr(context, code, enc);
    }

    public static int rb_enc_mbcput(ThreadContext context, int c, byte[] buf, int p, Encoding enc) {
        return EncodingUtils.encMbcput(context, c, buf, p, enc);
    }

    public static int rb_enc_codepoint_len(ThreadContext context, byte[] pBytes, int p, int e, int[] len_p, Encoding enc) {
        return EncodingUtils.encCodepointLength(context, pBytes, p, e, len_p, enc);
    }

    public static RubyString rb_str_escape(ThreadContext context, RubyString str) {
        return (RubyString) RubyString.rbStrEscape(context, str);
    }

    public static int rb_str_buf_cat_escaped_char(RubyString result, long c, boolean unicode_p) {
        return EncodingUtils.rbStrBufCatEscapedChar(result, c, unicode_p);
    }

    public static int rb_enc_codelen(ThreadContext context, int c, Encoding enc) {
        return EncodingUtils.encCodelen(context, c, enc);
    }
}
