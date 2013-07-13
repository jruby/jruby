package org.jruby.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.HashSet;
import java.util.Set;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.ISO8859_1Encoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Encapsulate all logic associated with using Java Charset transcoding 
 * facilities.
 */
// FIXME: Originally this was meant to capture invariant state.  Use specialization to make this much more efficient.
public class CharsetTranscoder {
    // Java seems to find these specific Java charsets but they seem to trancode
    // some strings a little differently than MRI.  Since Java Charset transcoding
    // is a temporary implementation for us, having this gruesome hack is ok
    // for the time being.
    private static Set<String> BAD_TRANSCODINGS_HACK = new HashSet<String>() {{
        add("ISO-2022-JP-2");
        add("CP50220");
        add("CP50221");
    }};
    
    private Encoding toEncoding;
    private CodingErrorActions actions;
    private Encoding forceEncoding = null;
    private boolean didDecode;
    private boolean didEncode;
    
    public CharsetTranscoder(ThreadContext context, Encoding toEncoding, IRubyObject options) {
        this(context, toEncoding, null, getCodingErrorActions(context, options));
    }
    
    public CharsetTranscoder(ThreadContext context, Encoding toEncoding, Encoding forceEncoding, CodingErrorActions actions) {
        this.toEncoding = toEncoding;
        this.forceEncoding = forceEncoding;
        
        if (actions == null) {
            this.actions = getCodingErrorActions(context, null);
        } else {
            this.actions = actions;
        }
    }
    
    public ByteList transcode(ThreadContext context, ByteList value) {
        Encoding fromEncoding = forceEncoding != null ? forceEncoding : value.getEncoding();
        
        return transcode(context.runtime, value, fromEncoding, false);
    }
    
    public ByteList transcode(ThreadContext context, ByteList value, boolean is7BitASCII) {
        Encoding fromEncoding = forceEncoding != null ? forceEncoding : value.getEncoding();
        
        return transcode(context.runtime, value, fromEncoding, is7BitASCII);
    }
    
    protected ByteList transcode(Ruby runtime, ByteList value, Encoding inEncoding, boolean is7BitASCII) {
        Encoding outEncoding = toEncoding != null ? toEncoding : value.getEncoding();
        String outName = outEncoding.toString();
        String inName = inEncoding.toString();
        
        // MRI does not allow ASCII-8BIT bytes > 127 to transcode to text-based
        // encodings, so for transcoding purposes we treat it as US-ASCII. We
        // also set the "invalid" action to "undef" option, since Java's decode
        // logic throws "invalid" errors for high-byte US-ASCII rather than
        // "undefined mapping" errors.
        if (inEncoding == ASCIIEncoding.INSTANCE && toEncoding != ASCIIEncoding.INSTANCE) {
            inEncoding = USASCIIEncoding.INSTANCE;
            actions.onMalformedInput = actions.onUnmappableCharacter;
        }
        
        Charset inCharset = transcodeCharsetFor(runtime, inEncoding, inName, outName, is7BitASCII);
        Charset outCharset = transcodeCharsetFor(runtime, outEncoding, inName, outName, is7BitASCII);

        CharsetEncoder encoder = getCharsetEncoder(outCharset);
        CharsetDecoder decoder = getCharsetDecoder(inCharset);

        ByteBuffer inBytes = ByteBuffer.wrap(value.getUnsafeBytes(), value.begin(), value.length());
        // TODO: dynamic buffer size?
        CharBuffer temp = CharBuffer.allocate(1024);
        int outN = (int)(inBytes.remaining() * decoder.averageCharsPerByte() * encoder.averageBytesPerChar());
        ByteBuffer outBytes = ByteBuffer.allocate(outN);
        byte[] replaceBytes = null;
        didDecode = false;
        didEncode = false;
        
        if (actions.onUnmappableCharacter == CodingErrorAction.REPLACE ||
                actions.onMalformedInput == CodingErrorAction.REPLACE) {
            
            replaceBytes = actions.replaceWith == null ?
                encoder.replacement() :
                actions.replaceWith.getBytes(outCharset);
        }
        
        while (inBytes.hasRemaining()) {
            temp.clear();
            didDecode = true;
            CoderResult result = decoder.decode(inBytes, temp, true);

            if (!result.isError()) {
                // buffer full, transfer to output
                temp.flip();
                outBytes = encode(runtime, encoder, temp, outBytes, replaceBytes);
            } else {
                if (result.isMalformed()) {
                    if (actions.onMalformedInput == CodingErrorAction.REPORT) {
                        throw runtime.newInvalidByteSequenceError("invalid bytes at offset " + inBytes.position());
                    }
                    
                    // transfer to out and skip bad byte
                    temp.flip();
                    outBytes = encode(runtime, encoder, temp, outBytes, replaceBytes);
                    inBytes.get();
                    
                    if (actions.onMalformedInput == CodingErrorAction.REPLACE) {
                        outBytes = putReplacement(outBytes, replaceBytes);
                    }
                } else if (result.isUnmappable()) {
                    if (actions.onUnmappableCharacter == CodingErrorAction.REPORT) {
                        throw runtime.newUndefinedConversionError("unmappable character at " + inBytes.position());
                    }
                    
                    // transfer to out and skip bad byte
                    temp.flip();
                    outBytes = encode(runtime, encoder, temp, outBytes, replaceBytes);
                    inBytes.get();

                    if (actions.onUnmappableCharacter == CodingErrorAction.REPLACE) {
                        outBytes = putReplacement(outBytes, replaceBytes);
                    }
                }
            }
        }
        
        // final flush of all coders
        if (didDecode) {
            temp.clear();
            while (true) {
                CoderResult result = decoder.flush(temp);
                temp.flip();
                outBytes = encode(runtime, encoder, temp, outBytes, replaceBytes);
                if (result == CoderResult.UNDERFLOW) break;
            }
        }
        
        if (didEncode) {
            while (encoder.flush(outBytes) == CoderResult.OVERFLOW) {
                growBuffer(outBytes);
                encoder.flush(outBytes);
            }
        }
        
        outBytes.flip();

        // CharsetEncoder#encode guarantees a newly-allocated buffer, so no need to copy.
        return new ByteList(outBytes.array(), outBytes.arrayOffset(),
                outBytes.limit() - outBytes.arrayOffset(), outEncoding, false);
    }
    
    private ByteBuffer growBuffer(ByteBuffer toBytes) {
        int toN = toBytes.capacity();
        if (toN == Integer.MAX_VALUE) {
            // raise error; we can't make a bigger buffer
            throw new ArrayIndexOutOfBoundsException("cannot allocate output buffer larger than " + Integer.MAX_VALUE + " bytes");
        }
        
        // use long for new size so we don't overflow, but don't exceed int max
        toN = (int)Math.min((long)toN * 2 + 1, Integer.MAX_VALUE);
        
        ByteBuffer newToBytes = ByteBuffer.allocate(toN);
        toBytes.flip();
        newToBytes.put(toBytes);
        
        return newToBytes;
    }

    /**
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
        
        return new CharsetTranscoder(context, toEncoding, forceEncoding, getCodingErrorActions(context, opts)).transcode(context, value, false);
    }
    
    public static ByteList transcode(ThreadContext context, ByteList value, Encoding forceEncoding,
            Encoding toEncoding, IRubyObject opts, boolean is7BitASCII) {
        if (toEncoding == null) return value;
        
        return new CharsetTranscoder(context, toEncoding, forceEncoding, getCodingErrorActions(context, opts)).transcode(context, value, is7BitASCII);
    }

    private ByteBuffer encode(Ruby runtime, CharsetEncoder encoder, CharBuffer inChars, ByteBuffer outBytes, byte[] replaceBytes) {
        CoderResult result;
        while (inChars.hasRemaining()) {
            didEncode = true;
            result = encoder.encode(inChars, outBytes, true);
            if (result.isError() && result.isUnmappable()) {
                // skip bad char
                char badChar = inChars.get();
                
                if (actions.onUnmappableCharacter == CodingErrorAction.REPORT) {
                    throw runtime.newUndefinedConversionError("unmappable character: `" + badChar);
                }

                if (actions.onUnmappableCharacter == CodingErrorAction.REPLACE) {
                    outBytes = putReplacement(outBytes, replaceBytes);
                }
            } else {
                if (result == CoderResult.OVERFLOW) {
                    outBytes = growBuffer(outBytes);
                }
            }
        }
        return outBytes;
    }

    private ByteBuffer putReplacement(ByteBuffer outBytes, byte[] replaceBytes) {
        while (outBytes.remaining() < replaceBytes.length) {
            outBytes = growBuffer(outBytes);
        }
        outBytes.put(replaceBytes);
        return outBytes;
    }

    public static class CodingErrorActions {
        CodingErrorAction onUnmappableCharacter;
        CodingErrorAction onMalformedInput;
        String replaceWith;

        CodingErrorActions(CodingErrorAction onUnmappableCharacter,
                CodingErrorAction onMalformedInput, String replaceWith) {
            this.onUnmappableCharacter = onUnmappableCharacter;
            this.onMalformedInput = onMalformedInput;
            this.replaceWith = replaceWith;
        }
        
        @Override
        public String toString() {
            return "UnmappableCharacter: " + onUnmappableCharacter + ", MalformedInput: " + onMalformedInput + ", replaceWith: " + replaceWith;
        }
    }
    
   public static CodingErrorActions getCodingErrorActions(ThreadContext context, IRubyObject opts) {
        if (opts == null || opts.isNil()) {
            return new CodingErrorActions(CodingErrorAction.REPORT,
                    CodingErrorAction.REPORT, null);
        } 

        Ruby runtime = context.runtime;
        RubyHash hash = (RubyHash) opts;
        CodingErrorAction onMalformedInput = CodingErrorAction.REPORT;
        CodingErrorAction onUnmappableCharacter = CodingErrorAction.REPORT;
        String replaceWith = null;
        
        IRubyObject invalid = hash.fastARef(runtime.newSymbol("invalid"));
        if (invalid != null && invalid.op_equal(context, runtime.newSymbol("replace")).isTrue()) {
            onMalformedInput = CodingErrorAction.REPLACE;
        }

        IRubyObject undef = hash.fastARef(runtime.newSymbol("undef"));
        if (undef != null && undef.op_equal(context, runtime.newSymbol("replace")).isTrue()) {
            onUnmappableCharacter = CodingErrorAction.REPLACE;
        }
        
        if (onUnmappableCharacter == CodingErrorAction.REPLACE || onMalformedInput == CodingErrorAction.REPLACE) {
            
            IRubyObject replace = hash.fastARef(runtime.newSymbol("replace"));
            
            if (replace != null && !replace.isNil()) {
                replaceWith = replace.convertToString().asJavaString();
            }
        }
        
        return new CodingErrorActions(onUnmappableCharacter, onMalformedInput, replaceWith);

        /*
         * Missing options from MRI 1.9.3 source:

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

    private CharsetDecoder getCharsetDecoder(Charset charset) {
        CharsetDecoder decoder = charset.newDecoder();

        return decoder;
    }

    private CharsetEncoder getCharsetEncoder(Charset charset) {
        CharsetEncoder encoder = charset.newEncoder();

        return encoder;
    } 

    private static Charset transcodeCharsetFor(Ruby runtime, Encoding encoding, String fromName, String toName, boolean is7Bit) {
        if (encoding == ASCIIEncoding.INSTANCE) {
            return ISO8859_1Encoding.INSTANCE.getCharset();
        }
        
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
                
                if (from != null) return from;
            } catch (Exception e) {}
        }
        try {
            from = encoding.getCharset();

            // if we have a from charset and the name matches any non-null charset name in the encoding...
            if (from != null
                    && (encoding.getCharsetName() != null && from.name().equals(encoding.getCharsetName()))) {
                return from;
            }
        } catch (Exception e) {}

        try { // We try looking up based on Java's supported charsets...likely missing charset entry in jcodings
            from = Charset.forName(encoding.toString());
        } catch (Exception e) {}
        
        if (from == null) {
            if (is7Bit) {
                from = Charset.forName("US-ASCII");
            } else {
                throw runtime.newConverterNotFoundError("code converter not found (" + fromName + " to " + toName + ")");
            }
        }

        return from;
    }
}
