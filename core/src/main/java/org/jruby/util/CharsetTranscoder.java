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
import static org.jruby.RubyConverter.PARTIAL_INPUT;
import org.jruby.RubyHash;
import org.jruby.RubyString;
import org.jruby.exceptions.RaiseException;
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
    
    private final Ruby runtime;
    public final Encoding toEncoding;
    private CodingActions actions;
    public final Encoding fromEncoding;
    private RubyCoderResult lastResult;
    private RaiseException lastError;
    
    public CharsetTranscoder(ThreadContext context, Encoding toEncoding, Encoding fromEncoding) {
        this(context, toEncoding, fromEncoding, processCodingErrorActions(context, null));
    }
    
    public CharsetTranscoder(ThreadContext context, Encoding toEncoding, Encoding fromEncoding, int flags, IRubyObject replace) {
        this(context, toEncoding, fromEncoding, processCodingErrorActions(context, flags, replace));
    }
    
    public CharsetTranscoder(ThreadContext context, Encoding toEncoding, Encoding fromEncoding, CodingActions actions) {
        this.runtime = context.runtime;
        this.toEncoding = toEncoding;
        this.fromEncoding = fromEncoding;
        
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
    public static ByteList transcode(ThreadContext context, ByteList value, Encoding fromEncoding,
            Encoding toEncoding, IRubyObject opts) {
        if (toEncoding == null) return value;
        
        return new CharsetTranscoder(context, toEncoding, fromEncoding, processCodingErrorActions(context, opts)).transcode(context, value, false);
    }
    
    public static ByteList transcode(ThreadContext context, ByteList value, Encoding fromEncoding,
            Encoding toEncoding, IRubyObject opts, boolean is7BitASCII) {
        if (toEncoding == null) return value;
        
        return new CharsetTranscoder(context, toEncoding, fromEncoding, processCodingErrorActions(context, opts)).transcode(context, value, is7BitASCII);
    }
    
    public ByteList transcode(ThreadContext context, ByteList value) {
        Encoding fromEncoding = this.fromEncoding != null ? this.fromEncoding : value.getEncoding();
        
        ByteList result = new ByteList();
        transcode(context.runtime, value, result, fromEncoding, false);
        
        return result;
    }
    
    public ByteList transcode(ThreadContext context, ByteList value, boolean is7BitASCII) {
        Encoding fromEncoding = this.fromEncoding != null ? this.fromEncoding : value.getEncoding();
        
        ByteList result = new ByteList();
        transcode(context.runtime, value, result, fromEncoding, is7BitASCII);
        
        return result;
    }
    
    public void transcode(Ruby runtime, ByteList inBuffer, ByteList outBuffer, Encoding inEncoding, boolean is7BitASCII) {
        primitiveConvert(runtime, inBuffer.dup(), outBuffer, 0, -1, inEncoding, is7BitASCII, actions.flags);
        
        if (lastResult != null) {
            createLastError();
            
            if (lastError != null) throw lastError;
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
        
        lastResult = state.transcode(flags);
        
        // consume bytes from inBuffer
        inBuffer.setBegin(inBytes.position());
        inBuffer.setRealSize(inBytes.remaining());

        outBytes = state.outBytes;

        // grossly inefficient
        if (outOffset == outBuffer.getRealSize()) {
            outBuffer.append(Arrays.copyOfRange(outBytes.array(), realOffset, outBytes.limit()));
        } else {
            outBuffer.replace(outOffset, outLimit - outOffset, Arrays.copyOfRange(outBytes.array(), realOffset, outBytes.limit()));
        }
        
        outBuffer.setEncoding(outEncoding);
        
        return lastResult;
    }
    
    public RubyCoderResult getLastResult() {
        return lastResult;
    }
    
    public RaiseException getLastError() {
        createLastError();
        
        return lastError;
    }

    private void createLastError() {
        if (lastResult.isError()) {
            RubyString errorBytes = runtime.newString(new ByteList(lastResult.errorBytes, ASCIIEncoding.INSTANCE, true));
            errorBytes.setEncoding(ASCIIEncoding.INSTANCE);
            
            // handle error
            if (lastResult.isInvalid()) {
                // FIXME: gross error message construction
                lastError = runtime.newInvalidByteSequenceError("\"" + errorBytes.inspect19().toString() + "\" on " + lastResult.inCharset);
                lastError.getException().dataWrapStruct(lastResult);
            } else if (lastResult.isUndefined()) {
                // FIXME: gross error message construction
                lastError = runtime.newUndefinedConversionError("\"" + errorBytes.inspect19().toString() + "\" from " + lastResult.inCharset + " to " + lastResult.outCharset);
                lastError.getException().dataWrapStruct(lastResult);
            }
        }
    }
    
    public static class RubyCoderResult {
        public final String stringResult;
        public final byte[] errorBytes;
        public final Charset inCharset;
        public final Charset outCharset;
        public final byte[] readagainBytes;
        private final boolean error;
        private final boolean incomplete;
        private final boolean undefined;
        
        public RubyCoderResult(String stringResult, Charset inCharset, Charset outCharset, byte[] errorBytes, byte[] readagainBytes) {
            this.errorBytes = errorBytes;
            this.inCharset = inCharset;
            this.outCharset = outCharset;
            this.readagainBytes = readagainBytes;
            this.stringResult = stringResult;
            
            this.incomplete = stringResult.equals("invalid_byte_sequence");
            this.undefined = stringResult.equals("undefined_conversion");
            this.error = incomplete || undefined;
        }
        
        public boolean isError() {
            return error;
        }
        
        public boolean isInvalid() {
            return incomplete;
        }
        
        public boolean isUndefined() {
            return undefined;
        }
    }
    
    public static class TranscoderState {
        public final ByteBuffer inBytes;
        public CharBuffer tmpChars;
        public ByteBuffer outBytes;
        public final CharsetDecoder decoder;
        public final CharsetEncoder encoder;
        public boolean binaryToCharacter;
        public boolean growable;
        public RubyCoderResult result;
        public boolean didDecode;
        public boolean didEncode;
        private CodingActions actions;
        
        public TranscoderState(ByteBuffer inBytes, ByteBuffer outBytes, boolean growable, Charset inCharset, Charset outCharset, CodingActions actions) {
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
    
        public RubyCoderResult transcode(int flags) {
            boolean completeInput = (flags & RubyConverter.PARTIAL_INPUT) == 0;
            boolean afterOutput = (flags & RubyConverter.AFTER_OUTPUT) != 0;
            boolean universalNewline = (flags & RubyConverter.UNIVERSAL_NEWLINE_DECORATOR) != 0;
            boolean crlfNewline = (flags & RubyConverter.CRLF_NEWLINE_DECORATOR) != 0;
            boolean crNewline = (flags & RubyConverter.CR_NEWLINE_DECORATOR) != 0;
            boolean xmlText = (flags & RubyConverter.XML_TEXT_DECORATOR) != 0;
            boolean xmlAttr = (flags & RubyConverter.XML_ATTR_CONTENT_DECORATOR) != 0;
            
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

                    if (!encode(
                            doCRLFTranslation(universalNewline, crlfNewline, crNewline, xmlText, xmlAttr),
                            replaceBytes,
                            flags)) return result;
                } else {
                    if (coderResult.isMalformed()) {
                        if (onMalformedInput == CodingErrorAction.REPORT) {
                            byte[] errorBytes = new byte[coderResult.length()];
                            inBytes.get(errorBytes);
                            return result = new RubyCoderResult(stringFromCoderResult(coderResult, flags), decoder.charset(), encoder.charset(), errorBytes, null);
                        }

                        // transfer to out and skip bad byte
                        tmpChars.flip();
                        if (!encode(tmpChars, replaceBytes, flags)) return result;

                        inBytes.get();

                        if (onMalformedInput == CodingErrorAction.REPLACE) {
                            if (!putReplacement(replaceBytes, flags)) return result;
                        }
                    } else if (coderResult.isUnmappable()) {
                        if (onUnmappableCharacter == CodingErrorAction.REPORT) {
                            byte[] errorBytes = new byte[coderResult.length()];
                            inBytes.get(errorBytes);
                            return result = new RubyCoderResult(stringFromCoderResult(coderResult, flags), decoder.charset(), encoder.charset(), errorBytes, null);
                        }

                        // transfer to out and skip bad byte
                        tmpChars.flip();
                        if (!encode(tmpChars, replaceBytes, flags)) return result;

                        inBytes.get();

                        if (onUnmappableCharacter == CodingErrorAction.REPLACE) {
                            if (!putReplacement(replaceBytes, flags)) return result;
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
                        if (!encode(tmpChars, replaceBytes, flags)) return result;
                        if (coderResult == CoderResult.UNDERFLOW) break;
                    }
                }

                if (didEncode) {
                    while (encoder.flush(outBytes) == CoderResult.OVERFLOW) {
                        if (!growBuffer(flags)) return result;
                        encoder.flush(outBytes);
                    }
                }
            }

            outBytes.flip();
            
            if ((flags & PARTIAL_INPUT) == 0) {
                result = new RubyCoderResult("finished", decoder.charset(), encoder.charset(), null, null);
            } else {
                result = new RubyCoderResult("source_buffer_empty", decoder.charset(), encoder.charset(), null, null);
            }
            
            return result;
        }
    
        private boolean growBuffer(int flags) {
            if (!growable) {
                result = new RubyCoderResult(stringFromCoderResult(CoderResult.OVERFLOW, flags), decoder.charset(), encoder.charset(), null, null);
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

        private boolean encode(CharBuffer inChars, byte[] replaceBytes, int flags) {
            CoderResult coderResult;

            while (inChars.hasRemaining()) {
                didEncode = true;
                coderResult = encoder.encode(inChars, outBytes, true);
                
                if (coderResult.isError() && coderResult.isUnmappable()) {
                    // skip bad char
                    char badChar = inChars.get();

                    if (actions.onUnmappableCharacter == CodingErrorAction.REPORT) {
                        result = new RubyCoderResult(stringFromCoderResult(coderResult, flags), decoder.charset(), encoder.charset(), Character.toString(badChar).getBytes(decoder.charset()), null);
                        return false;
                    }

                    if (actions.onUnmappableCharacter == CodingErrorAction.REPLACE) {
                        if (!putReplacement(replaceBytes, flags)) return false;
                    }
                } else {
                    if (coderResult == CoderResult.OVERFLOW) {
                        if (!growBuffer(flags)) return false;
                    }
                }
            }

            return true;
        }

        private boolean putReplacement(byte[] replaceBytes, int flags) {
            if (outBytes.remaining() < replaceBytes.length) {
                if (!growBuffer(flags)) return false;
            }

            outBytes.put(replaceBytes);

            return true;
        }

        private CharBuffer doCRLFTranslation(boolean universalNewline, boolean crlfNewline, boolean crNewline, boolean xmlText, boolean xmlAttr) {
            CharBuffer inChars = tmpChars;
            if (universalNewline || crlfNewline || crNewline) {
                // translate intermediate buffer before encoding
                CharBuffer newTmp;
                
                // universal_newline and cr_newline can use buffer in-place
                if (crlfNewline) {
                    // 2x to accommodate worst-case of all (CR|LF) to CRLF
                    newTmp = CharBuffer.allocate(tmpChars.remaining() * 2);
                } else {
                    newTmp = tmpChars.duplicate();
                }
                
                boolean lastWasCR = false;
                while (tmpChars.remaining() > 0) {
                    char ch = tmpChars.get();
                    
                    if (universalNewline) {
                        switch (ch) {
                            case '\r':
                                // drop, mark previous as \r
                                lastWasCR = true;
                                continue;
                            case '\n':
                                if (lastWasCR) {
                                    // replace \r with \n and write from here
                                    newTmp.put('\n');
                                } else {
                                    // advance, leaving \n in place
                                    newTmp.get();
                                }
                                lastWasCR = false;
                                continue;
                            default:
                                if (lastWasCR) {
                                    // replace \r with \n and write from here
                                    newTmp.put('\n');
                                }
                                // replace with ch, since we may be shifted
                                newTmp.put(ch);
                                lastWasCR = false;
                                continue;
                        }
                    } else if (crNewline) {
                        switch (ch) {
                            case '\n':
                                // advance, replacing \n
                                newTmp.put('\r');
                                continue;
                            default:
                                // leave ch in place
                                newTmp.get();
                                continue;
                        }
                    } else if (crlfNewline) {
                        switch (ch) {
                            case '\n':
                                // advance, replacing \n
                                newTmp.put('\r');
                                newTmp.put('\n');
                                continue;
                            default:
                                // copy ch
                                newTmp.put(ch);
                                continue;
                        }
                    }
                }
                
                // use newTmp for this iteration
                newTmp.flip();
                inChars = newTmp;
            }
            
            if (xmlText || xmlAttr) {
                StringBuilder builder = new StringBuilder(inChars.remaining());
                
                while (inChars.hasRemaining()) {
                    char ch = inChars.get();
                    String replace = null;
                    
                    if (ch >= 128) {
                        builder.append(ch);
                    } else if ((xmlText && (replace = XMLTextCharacterTranslator[ch]) != null) ||
                            (xmlAttr && (replace = XMLTextCharacterTranslator[ch]) != null)) {
                        builder.append(replace);
                    } else {
                        builder.append(ch);
                    }
                }
                
                inChars = CharBuffer.wrap(builder);
            }
            
            return inChars;
        }
    }
    
    public static final String[] XMLTextCharacterTranslator = new String[128];
    public static final String[] XMLAttrCharacterTranslator = new String[128];
    
    static {
        XMLTextCharacterTranslator['&'] = "&amp;";
        XMLTextCharacterTranslator['<'] = "&lt;";
        XMLTextCharacterTranslator['>'] = "&gt;";
        
        XMLAttrCharacterTranslator['&'] = "&amp;";
        XMLAttrCharacterTranslator['<'] = "&lt;";
        XMLAttrCharacterTranslator['>'] = "&gt;";
        XMLAttrCharacterTranslator['\"'] = "&quot;";
    }
    
    public static String stringFromCoderResult(CoderResult coderResult, int flags) {
        if (coderResult == null) return "finished";
        
        if (coderResult.isError()) {
            if (coderResult.isMalformed()) {
                return "invalid_byte_sequence";
            } else if (coderResult.isUnmappable()) {
                return "undefined_conversion";
            } else {
                return "finished";
            }
        } else {
            if (coderResult.isUnderflow()) {
                if ((flags & PARTIAL_INPUT) == 0) {
                    // no more bytes and we're supposed to be done
                    return "incomplete_input";
                } else {
                    // no more bytes but it's ok
                    return "source_buffer_empty";
                }
            } else if (coderResult.isOverflow()) {
                return "destination_buffer_full";
            } else {
                if ((flags & PARTIAL_INPUT) == 0) {
                    // no more bytes and we're done
                    return "finished";
                } else {
                    // no more bytes but more might be coming
                    return "source_buffer_empty";
                }
            }
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

    private static byte[] replaceBytesFromString(CodingActions actions, Charset outCharset) {
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

    public static class CodingActions {
        CodingErrorAction onUnmappableCharacter;
        CodingErrorAction onMalformedInput;
        String replaceWith;
        public int flags;

        CodingActions(CodingErrorAction onUnmappableCharacter,
                CodingErrorAction onMalformedInput, int flags, String replaceWith) {
            this.onUnmappableCharacter = onUnmappableCharacter;
            this.onMalformedInput = onMalformedInput;
            this.replaceWith = replaceWith;
            this.flags = flags;
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
    
    public CodingActions getCodingErrorActions() {
        return actions;
    }
    
    public static CodingActions processCodingErrorActions(ThreadContext context, int flags, IRubyObject replace) {
        if (flags == 0) {
            return new CodingActions(CodingErrorAction.REPORT,
                    CodingErrorAction.REPORT, 0, null);
        } 

        CodingErrorAction onMalformedInput = CodingErrorAction.REPORT;
        CodingErrorAction onUnmappableCharacter = CodingErrorAction.REPORT;
        String replaceWith = null;
        
        if ((flags & RubyConverter.INVALID_MASK) == RubyConverter.INVALID_REPLACE) {
            onMalformedInput = CodingErrorAction.REPLACE;
        }
            
        if ((flags & RubyConverter.UNDEF_MASK) == RubyConverter.UNDEF_REPLACE) {
            onUnmappableCharacter = CodingErrorAction.REPLACE;
        }
        
        if (replace != null && !replace.isNil()) {
            replaceWith = replace.convertToString().toString();
        }
        
        return new CodingActions(onUnmappableCharacter, onMalformedInput, flags, replaceWith);
    }
    
    public static CodingActions processCodingErrorActions(ThreadContext context, IRubyObject opts) {
        if (opts == null || opts.isNil()) {
            return new CodingActions(CodingErrorAction.REPORT,
                    CodingErrorAction.REPORT, 0, null);
        } 

        Ruby runtime = context.runtime;
        int flags = 0;
        
        RubyHash hash = opts.convertToHash();
        flags |= RubyConverter.optHashToFlags(hash, runtime, flags);

        IRubyObject replace = hash.fastARef(runtime.newSymbol("replace"));
        
        return processCodingErrorActions(context, flags, replace);
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
