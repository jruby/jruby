package org.jruby.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.ISO8859_1Encoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyConverter;
import org.jruby.RubyFixnum;
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
    private static Set<String> BAD_TRANSCODINGS_HACK = new HashSet<String>();
    
    static {
        BAD_TRANSCODINGS_HACK.add("ISO-2022-JP-2");
        BAD_TRANSCODINGS_HACK.add("CP50220");
        BAD_TRANSCODINGS_HACK.add("CP50221");
    }
    
    private static final Charset UTF16 = Charset.forName("UTF-16");
    
    public final Encoding toEncoding;
    private CodingErrorActions actions;
    public final Encoding forceEncoding;
    
    private boolean didDecode;
    private boolean didEncode;
    
    public CharsetTranscoder(ThreadContext context, Encoding toEncoding, IRubyObject options) {
        this(context, toEncoding, null, processCodingErrorActions(context, options));
    }
    
    public CharsetTranscoder(ThreadContext context, Encoding toEncoding, Encoding forceEncoding) {
        this(context, toEncoding, forceEncoding, processCodingErrorActions(context, null));
    }
    
    public CharsetTranscoder(ThreadContext context, Encoding toEncoding, Encoding forceEncoding, IRubyObject options) {
        this(context, toEncoding, forceEncoding, processCodingErrorActions(context, options));
    }
    
    public CharsetTranscoder(ThreadContext context, Encoding toEncoding, Encoding forceEncoding, CodingErrorActions actions) {
        this.toEncoding = toEncoding;
        this.forceEncoding = forceEncoding;
        
        if (actions == null) {
            this.actions = processCodingErrorActions(context, null);
        } else {
            this.actions = actions;
        }
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
        
        return new CharsetTranscoder(context, toEncoding, forceEncoding, processCodingErrorActions(context, opts)).transcode(context, value, false);
    }
    
    public static ByteList transcode(ThreadContext context, ByteList value, Encoding forceEncoding,
            Encoding toEncoding, IRubyObject opts, boolean is7BitASCII) {
        if (toEncoding == null) return value;
        
        return new CharsetTranscoder(context, toEncoding, forceEncoding, processCodingErrorActions(context, opts)).transcode(context, value, is7BitASCII);
    }
    
    public ByteList transcode(ThreadContext context, ByteList value) {
        Encoding fromEncoding = forceEncoding != null ? forceEncoding : value.getEncoding();
        
        ByteList result = new ByteList();
        transcode(context.runtime, value, result, fromEncoding, false);
        
        return result;
    }
    
    public ByteList transcode(ThreadContext context, ByteList value, boolean is7BitASCII) {
        Encoding fromEncoding = forceEncoding != null ? forceEncoding : value.getEncoding();
        
        ByteList result = new ByteList();
        transcode(context.runtime, value, result, fromEncoding, is7BitASCII);
        
        return result;
    }
    
    public void transcode(Ruby runtime, ByteList inBuffer, ByteList outBuffer, Encoding inEncoding, boolean is7BitASCII) {
        RubyCoderResult result = primitiveConvert(runtime, inBuffer, outBuffer, 0, -1, inEncoding, is7BitASCII, 0);
        
        if (result != null) {
            // handle error
            if (result.coderResult.isError()) {
                if (result.coderResult.isMalformed()) {
                    throw runtime.newInvalidByteSequenceError("1");
                } else if (result.coderResult.isUnmappable()) {
                    throw runtime.newUndefinedConversionError("2");
                }
            } else {
                if (result.coderResult.isUnderflow()) {
                    throw runtime.newInvalidByteSequenceError("1");
                } else if (result.coderResult.isOverflow()) {
                    throw new RuntimeException();
                }
            }
        }
    }
    
    public RubyCoderResult primitiveConvert(Ruby runtime, ByteList inBuffer, ByteList outBuffer, int outOffset, int outLimit, Encoding inEncoding, boolean is7BitASCII, int flags) {
        Encoding outEncoding = toEncoding != null ? toEncoding : inBuffer.getEncoding();
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

        ByteBuffer inBytes = ByteBuffer.wrap(inBuffer.getUnsafeBytes(), inBuffer.begin(), inBuffer.length());
        
        boolean growable = true;
        if (outLimit > 0) {
            // with a limit, don't try to grow
            growable = false;
        } else {
            outLimit = outBuffer.getRealSize();
        }
        
        int realOffset = outBuffer.getBegin() + outOffset;
        ByteBuffer outBytes = ByteBuffer.wrap(outBuffer.getUnsafeBytes(), realOffset, outLimit - outOffset);
        
        TranscoderState state = new TranscoderState(inBytes, outBytes, growable, inCharset, outCharset, actions);
        
        RubyCoderResult result = state.transcode(
                (flags & RubyConverter.PARTIAL_INPUT) == 0,
                (flags & RubyConverter.AFTER_OUTPUT) != 0);
        
        if (result != null) return result;

        outBytes = state.outBytes;

        // grossly inefficient
        outBuffer.replace(outOffset, outLimit - outOffset, Arrays.copyOfRange(outBytes.array(), realOffset, outBytes.limit()));
        
        outBuffer.setEncoding(outEncoding);
        
        return null;
    }
    
    public static class RubyCoderResult {
        public final CoderResult coderResult;
        public final byte[] errorBytes;
        public final byte[] readagainBytes;
        
        public RubyCoderResult(CoderResult coderResult, byte[] errorBytes, byte[] readagainBytes) {
            this.coderResult = coderResult;
            this.errorBytes = errorBytes;
            this.readagainBytes = readagainBytes;
        }
    }
    
    public static class TranscoderState {
        public final ByteBuffer inBytes;
        public final CharBuffer tmpChars;
        public ByteBuffer outBytes;
        public final CharsetDecoder decoder;
        public final CharsetEncoder encoder;
        public boolean binaryToCharacter;
        public boolean growable;
        public RubyCoderResult result;
        public boolean didDecode;
        public boolean didEncode;
        private CodingErrorActions actions;
        
        public TranscoderState(ByteBuffer inBytes, ByteBuffer outBytes, boolean growable, Charset inCharset, Charset outCharset, CodingErrorActions actions) {
            this.encoder = outCharset.newEncoder();
            this.decoder = inCharset.newDecoder();
            
            this.growable = growable;
        
            this.inBytes = inBytes;
            
            if (outBytes == null) {
                int outN = (int)(inBytes.remaining() * decoder.averageCharsPerByte() * encoder.averageBytesPerChar());
                outBytes = ByteBuffer.allocate(outN);
            }
            
            this.outBytes = outBytes;
            
            // TODO: dynamic buffer size?
            this.tmpChars = CharBuffer.allocate(1024);
            
            this.actions = actions;
        }
    
        public RubyCoderResult transcode(boolean completeInput, boolean afterOutput) {
            CodingErrorAction onMalformedInput = actions.onMalformedInput;
            CodingErrorAction onUnmappableCharacter = actions.onUnmappableCharacter;

            // MRI does not allow ASCII-8BIT bytes > 127 to transcode to text-based
            // encodings, so for transcoding purposes we treat it as US-ASCII. We
            // also set the "invalid" action to "undef" option, since Java's decode
            // logic throws "invalid" errors for high-byte US-ASCII rather than
            // "undefined mapping" errors.
            if (binaryToCharacter) {
                onMalformedInput = onUnmappableCharacter;
            }

            byte[] replaceBytes = null;
            didDecode = false;
            didEncode = false;

            if (onUnmappableCharacter == CodingErrorAction.REPLACE ||
                    onMalformedInput == CodingErrorAction.REPLACE) {
                replaceBytes = replaceBytesFromString(actions, encoder.charset());
            }

            while (inBytes.hasRemaining()) {
                tmpChars.clear();
                didDecode = true;
                CoderResult coderResult = decoder.decode(inBytes, tmpChars, completeInput);

                if (!coderResult.isError()) {
                    // buffer full, transfer to output
                    tmpChars.flip();

                    if (!encode(replaceBytes)) return result;
                } else {
                    if (coderResult.isMalformed()) {
                        if (onMalformedInput == CodingErrorAction.REPORT) {
                            byte[] errorBytes = new byte[coderResult.length()];
                            inBytes.get(errorBytes);
                            return result = new RubyCoderResult(coderResult, errorBytes, null);
                        }

                        // transfer to out and skip bad byte
                        tmpChars.flip();
                        if (!encode(replaceBytes)) return result;

                        inBytes.get();

                        if (onMalformedInput == CodingErrorAction.REPLACE) {
                            if (!putReplacement(replaceBytes)) return result;
                        }
                    } else if (coderResult.isUnmappable()) {
                        if (onUnmappableCharacter == CodingErrorAction.REPORT) {
                            byte[] errorBytes = new byte[coderResult.length()];
                            inBytes.get(errorBytes);
                            return result = new RubyCoderResult(coderResult, errorBytes, null);
                        }

                        // transfer to out and skip bad byte
                        tmpChars.flip();
                        if (!encode(replaceBytes)) return result;

                        inBytes.get();

                        if (onUnmappableCharacter == CodingErrorAction.REPLACE) {
                            if (!putReplacement(replaceBytes)) return result;
                        }
                    }
                }
                if (afterOutput) break;
            }

            // final flush of all coders if this is the end of input
            if (completeInput) {
                if (didDecode) {
                    tmpChars.clear();
                    while (true) {
                        CoderResult coderResult = decoder.flush(tmpChars);
                        tmpChars.flip();
                        if (!encode(replaceBytes)) return result;
                        if (coderResult == CoderResult.UNDERFLOW) break;
                    }
                }

                if (didEncode) {
                    while (encoder.flush(outBytes) == CoderResult.OVERFLOW) {
                        if (!growBuffer()) return result;
                        encoder.flush(outBytes);
                    }
                }
            }

            outBytes.flip();

            return result;
        }
    
        private boolean growBuffer() {
            if (!growable) {
                result = new RubyCoderResult(CoderResult.OVERFLOW, null, null);
                return false;
            }

            int toN = outBytes.capacity();
            if (toN == Integer.MAX_VALUE) {
                // raise error; we can't make a bigger buffer
                throw new ArrayIndexOutOfBoundsException("cannot allocate output buffer larger than " + Integer.MAX_VALUE + " bytes");
            }

            // use long for new size so we don't overflow, but don't exceed int max
            toN = (int)Math.min((long)toN * 2 + 1, Integer.MAX_VALUE);

            ByteBuffer newOutBytes = ByteBuffer.allocate(toN);
            outBytes.flip();
            newOutBytes.put(outBytes);
            outBytes = newOutBytes;

            return true;
        }

        private boolean encode(byte[] replaceBytes) {
            CoderResult coderResult;

            while (tmpChars.hasRemaining()) {
                didEncode = true;
                coderResult = encoder.encode(tmpChars, outBytes, true);
                
                if (coderResult.isError() && coderResult.isUnmappable()) {
                    // skip bad char
                    char badChar = tmpChars.get();

                    if (actions.onUnmappableCharacter == CodingErrorAction.REPORT) {
                        result = new RubyCoderResult(coderResult, Character.toString(badChar).getBytes(UTF16), null);
                        return false;
                    }

                    if (actions.onUnmappableCharacter == CodingErrorAction.REPLACE) {
                        if (!putReplacement(replaceBytes)) return false;
                    }
                } else {
                    if (coderResult == CoderResult.OVERFLOW) {
                        if (!growBuffer()) return false;
                    }
                }
            }

            return true;
        }

        private boolean putReplacement(byte[] replaceBytes) {
            if (outBytes.remaining() < replaceBytes.length) {
                if (!growBuffer()) return false;
            }

            outBytes.put(replaceBytes);

            return true;
        }
    }
    
    public static final Set<Charset> UNICODE_CHARSETS;
    static {
        Set<Charset> charsets = new HashSet<Charset>();
        
        charsets.add(Charset.forName("UTF-8"));
        charsets.add(Charset.forName("UTF-16"));
        charsets.add(Charset.forName("UTF-16BE"));
        charsets.add(Charset.forName("UTF-16LE"));
        charsets.add(Charset.forName("UTF-32"));
        charsets.add(Charset.forName("UTF-32BE"));
        charsets.add(Charset.forName("UTF-32LE"));
        
        UNICODE_CHARSETS = Collections.unmodifiableSet(charsets);
    }

    private static byte[] replaceBytesFromString(CodingErrorActions actions, Charset outCharset) {
        byte[] replaceBytes;
        if (actions.replaceWith != null) {
            replaceBytes = actions.replaceWith.getBytes(outCharset);
        } else {
            if (UNICODE_CHARSETS.contains(outCharset)) {
                replaceBytes = "\uFFFD".getBytes(outCharset);
            } else {
                replaceBytes = "?".getBytes(outCharset);
            }
        }
        return replaceBytes;
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

        public CodingErrorAction getOnUnmappableCharacter() {
            return onUnmappableCharacter;
        }

        public void setOnUnmappableCharacter(CodingErrorAction onUnmappableCharacter) {
            this.onUnmappableCharacter = onUnmappableCharacter;
        }

        public CodingErrorAction getOnMalformedInput() {
            return onMalformedInput;
        }

        public void setOnMalformedInput(CodingErrorAction onMalformedInput) {
            this.onMalformedInput = onMalformedInput;
        }

        public String getReplaceWith() {
            return replaceWith;
        }

        public void setReplaceWith(String replaceWith) {
            this.replaceWith = replaceWith;
        }
    }
    
    public CodingErrorActions getCodingErrorActions() {
        return actions;
    }
    
    public static CodingErrorActions processCodingErrorActions(ThreadContext context, IRubyObject opts) {
        if (opts == null || opts.isNil()) {
            return new CodingErrorActions(CodingErrorAction.REPORT,
                    CodingErrorAction.REPORT, null);
        } 

        Ruby runtime = context.runtime;
        CodingErrorAction onMalformedInput = CodingErrorAction.REPORT;
        CodingErrorAction onUnmappableCharacter = CodingErrorAction.REPORT;
        String replaceWith = null;
        
        if (opts instanceof RubyFixnum) {
            int flags = (int)((RubyFixnum)opts).getLongValue();
            
            switch (flags & RubyConverter.INVALID_MASK) {
                case RubyConverter.INVALID_REPLACE:
                    onMalformedInput = CodingErrorAction.REPLACE;
                    break;
            }
            
            switch (flags & RubyConverter.UNDEF_MASK) {
                case RubyConverter.UNDEF_REPLACE:
                    onUnmappableCharacter = CodingErrorAction.REPLACE;
                    break;
            }
        } else {
            RubyHash hash = opts.convertToHash();

            IRubyObject invalid = hash.fastARef(runtime.newSymbol("invalid"));
            if (invalid != null && invalid.op_equal(context, runtime.newSymbol("replace")).isTrue()) {
                onMalformedInput = CodingErrorAction.REPLACE;
            }

            IRubyObject undef = hash.fastARef(runtime.newSymbol("undef"));
            if (undef != null && undef.op_equal(context, runtime.newSymbol("replace")).isTrue()) {
                onUnmappableCharacter = CodingErrorAction.REPLACE;
            }

            IRubyObject replace = hash.fastARef(runtime.newSymbol("replace"));
            if (replace != null && !replace.isNil()) {
                replaceWith = replace.convertToString().toString();
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
