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
 * Copyright (C) 2013 The JRuby Community (jruby.org)
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
package org.jruby.util.encoding;

import org.jcodings.specific.ISO8859_16Encoding;
import org.jruby.util.*;
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
import org.jcodings.EncodingDB;
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
import org.jruby.util.io.EncodingUtils;
import org.jruby.util.unsafe.UnsafeHolder;

/**
 * Encapsulate all logic associated with using Java Charset transcoding 
 * facilities.
 */
// FIXME: Originally this was meant to capture invariant state.  Use specialization to make this much more efficient.
public class CharsetTranscoder extends Transcoder {
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
        
    private static final int MAX_ARRAY_SIZE;
    static {
        if (UnsafeHolder.U != null) {
            MAX_ARRAY_SIZE = Integer.MAX_VALUE - UnsafeHolder.U.arrayBaseOffset(byte[].class);
        } else {
            MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
        }
    }
    
    private CodingActions actions;
    private TranscoderEngine transcoder;
    private RaiseException lastError;
    
    public CharsetTranscoder(ThreadContext context, Encoding outEncoding, Encoding inEncoding) {
        this(context, outEncoding, inEncoding, processCodingErrorActions(context, null));
    }
    
    public CharsetTranscoder(ThreadContext context, Encoding outEncoding, Encoding inEncoding, int flags, IRubyObject replace) {
        this(context, outEncoding, inEncoding, processCodingErrorActions(context, flags, replace));
    }
    
    public CharsetTranscoder(ThreadContext context, Encoding outEncoding, Encoding inEncoding, CodingActions actions) {
        super(context, outEncoding, inEncoding);
        
        if (actions == null) {
            this.actions = processCodingErrorActions(context, null);
        } else {
            this.actions = actions;
        }
    }
    
    public static CharsetTranscoder open(ThreadContext context, byte[] sourceEncoding, byte[] destinationEncoding, int flags, IRubyObject replace) {
        EncodingDB.Entry src = context.runtime.getEncodingService().findEncodingOrAliasEntry(new ByteList(sourceEncoding, false));
        EncodingDB.Entry dest = context.runtime.getEncodingService().findEncodingOrAliasEntry(new ByteList(destinationEncoding, false));
        
        if (src == null && dest == null) {
            // this should really be a null transcoder that only does decorations
            return new CharsetTranscoder(
                    context,
                    ASCIIEncoding.INSTANCE,
                    ASCIIEncoding.INSTANCE,
                    flags,
                    replace);
        }
        
        return new CharsetTranscoder(context,
                dest.getEncoding(),
                src.getEncoding(),
                flags,
                replace);
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
        if (fromEncoding == null) fromEncoding = value.getEncoding();
        if (fromEncoding == toEncoding) return value;
        
        // This logic appears to not work like in MRI; following code will not
        // properly decode the string:
        // "\x00a".force_encoding("ASCII-8BIT").encode("UTF-8", "UTF-16BE")
        /*
        if ((toEncoding.isAsciiCompatible() && StringSupport.codeRangeScan(value.getEncoding(), value) == StringSupport.CR_7BIT) ||
                toEncoding == ASCIIEncoding.INSTANCE) {
            if (value.getEncoding() != toEncoding) {
                value = value.shallowDup();
                value.setEncoding(toEncoding);
            }
            return value;
        }*/
        
        return new CharsetTranscoder(context, toEncoding, fromEncoding, processCodingErrorActions(context, opts)).transcode(context, value, false);
    }
    
    public static ByteList transcode(ThreadContext context, ByteList value, Encoding fromEncoding,
            Encoding toEncoding, IRubyObject opts, boolean is7BitASCII) {
        if (toEncoding == null) return value;
        if (fromEncoding == null) fromEncoding = value.getEncoding();
        if (fromEncoding == toEncoding) return value;
        
        // This logic appears to not work like in MRI; following code will not
        // properly decode the string:
        // "\x00a".force_encoding("ASCII-8BIT").encode("UTF-8", "UTF-16BE")
        /*
        if ((toEncoding.isAsciiCompatible() && is7BitASCII) ||
                toEncoding == ASCIIEncoding.INSTANCE) {
            if (value.getEncoding() != toEncoding) {
                value = value.shallowDup();
                value.setEncoding(toEncoding);
            }
            return value;
        }
        */
        
        return new CharsetTranscoder(context, toEncoding, fromEncoding, processCodingErrorActions(context, opts)).transcode(context, value, is7BitASCII);
    }
    
    // rb_str_transcode
    public static ByteList strTranscode(ThreadContext context, RubyString self, Encoding fromEncoding, Encoding toEncoding, IRubyObject opt) {
        int ecflags = 0;
        IRubyObject[] ecopts_p = new IRubyObject[1];
        
        if (!opt.isNil()) {
            ecflags = EncodingUtils.econvPrepareOpts(context, opt, ecopts_p);
        }
        
        return strTranscode0(context, self, fromEncoding, toEncoding, ecflags, ecopts_p[0]);
    }
    
    // rb_str_transcode0
    public static ByteList strTranscode0(ThreadContext context, RubyString self, Encoding senc, Encoding denc, int ecflags, IRubyObject ecopts) {
        ByteList selfByteList = self.getByteList();
        boolean is7BitASCII = self.isCodeRangeAsciiOnly();
        IRubyObject replace = ecopts != null && !ecopts.isNil() ?
                ((RubyHash)ecopts).op_aref(context, context.runtime.newSymbol("replace")) :
                context.nil;
        
        if ((ecflags & (EncodingUtils.ECONV_NEWLINE_DECORATOR_MASK
                | EncodingUtils.ECONV_XML_TEXT_DECORATOR
                | EncodingUtils.ECONV_XML_ATTR_CONTENT_DECORATOR
                | EncodingUtils.ECONV_XML_ATTR_QUOTE_DECORATOR)) == 0) {
            if (senc != null && senc == denc) {                
                // TODO: Ruby 2.0 or 2.1 use String#scrub here
                if ((ecflags & EncodingUtils.ECONV_INVALID_MASK) != 0) {
                    // TODO: scrub with replacement
                    return selfByteList;
                } else {
                    // TODO: scrub without replacement
                    return selfByteList;
                }
            } else if (senc != null && denc != null && senc.isAsciiCompatible() && denc.isAsciiCompatible()) {
                if (self.scanForCodeRange() == StringSupport.CR_7BIT) {
                    ByteList value = selfByteList.shallowDup();
                    value.setEncoding(denc);
                    return value;
                }
            }
            // FIXME: I think this should be doing a lookup of the names to see if they're equivalent encodings
//            if (Arrays.equals(fromEncoding.getName(), toEncoding.getName())) {
//                return selfByteList;
//            }
        }
        
        return new CharsetTranscoder(context, denc, senc, processCodingErrorActions(context, ecflags, replace)).transcode(context, selfByteList, is7BitASCII);
    }
    
    // latter portion of transcode_loop
    public RubyCoderResult transcode(ThreadContext context, ByteList value, ByteList dest) {
        Encoding fromEncoding = this.inEncoding != null ? this.inEncoding : value.getEncoding();
        
        return transcode(context, value, dest, fromEncoding, false, true);
    }
    
    public ByteList transcode(ThreadContext context, ByteList value) {
        ByteList dest = new ByteList();
        
        transcode(context, value, dest);
        
        return dest;
    }
    
    public ByteList transcode(ThreadContext context, ByteList value, boolean is7BitASCII) {
        Encoding fromEncoding = this.inEncoding != null ? this.inEncoding : value.getEncoding();
        
        ByteList result = new ByteList();
        transcode(context, value, result, fromEncoding, is7BitASCII, true);
        
        return result;
    }
    
    // from Converter#convert
    public ByteList convert(ThreadContext context, ByteList value, boolean is7BitASCII) {
        Encoding fromEncoding = this.inEncoding != null ? this.inEncoding : value.getEncoding();
        
        ByteList result = new ByteList();
        transcode(context, value, result, fromEncoding, is7BitASCII, false);
        
        lastResult = new RubyCoderResult("finished", fromEncoding, outEncoding, null, null);
        
        return result;
    }
    
    public ByteList econvStrConvert(ThreadContext context, ByteList value, boolean finish) {
        Encoding fromEncoding = this.inEncoding != null ? this.inEncoding : value.getEncoding();
        
        ByteList result = new ByteList();
        transcode(context, value, result, fromEncoding, false, finish);
        
        lastResult = new RubyCoderResult("finished", fromEncoding, outEncoding, null, null);
        
        return result;
    }
    
    private RubyCoderResult transcode(ThreadContext context, ByteList inBuffer, ByteList outBuffer, Encoding inEncoding, boolean is7BitASCII, boolean finish) {
        primitiveConvert(context, inBuffer.shallowDup(), outBuffer, 0, -1, inEncoding, is7BitASCII, actions.ecflags);
        
        if (lastResult != null) {
            createLastError();
            
            if (lastError != null) throw lastError;
        }
        
        if (finish) outBuffer.append(finish(inBuffer.getEncoding()));
        
        return lastResult;
    }
    
    @Override
    public RubyCoderResult primitiveConvert(ThreadContext context, ByteList inBuffer, ByteList outBuffer, int outOffset, int outLimit, Encoding inEncoding, boolean is7BitASCII, int flags) {
        Ruby runtime = context.runtime;
        
        Encoding outEncoding = this.outEncoding != null ? this.outEncoding : inBuffer.getEncoding();

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
        
        // if no transcoder or encodings differ, recreate
        if (transcoder == null || outEncoding != this.outEncoding || inEncoding != this.inEncoding) {
            createTranscoder(inEncoding, outEncoding, actions, is7BitASCII);
        }
        
        TranscoderEngine.TranscoderState state = transcoder.new TranscoderState(inBytes, outBytes, growable);
        
        lastResult = transcoder.transcode(state, flags);
        
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
    
    public ByteList finish(Encoding altEncoding) {
        Encoding outEncoding = this.outEncoding != null ? this.outEncoding : altEncoding;
        
        if (transcoder == null) return new ByteList();
        
        ByteList finish = transcoder.finish();
        
        finish.setEncoding(outEncoding);
        
        // clear
        transcoder.reset();
        
        return finish;
    }
    
    public RubyCoderResult getLastResult() {
        return lastResult;
    }
    
    public RaiseException getLastError() {
        createLastError();
        
        return lastError;
    }
    
    public String getReplaceWith() {
        if (actions.replaceWith == null) {
            if (transcoder == null) {
                if (outEncoding != null && outEncoding.getCharset() != null) {
                    return replaceStringFromActions(actions, outEncoding.getCharset());
                }
                
                return "?";
            }
        } else if (transcoder == null) {
            return actions.replaceWith;
        }
        
        return replaceStringFromActions(actions, transcoder.encoder.charset());
    }

    private void createLastError() {
        if (lastResult != null) {
            if (lastResult.isError()) {
                RubyString errorBytes = runtime.newString(new ByteList(lastResult.errorBytes, ASCIIEncoding.INSTANCE, true));
                errorBytes.setEncoding(ASCIIEncoding.INSTANCE);

                // handle error
                if (lastResult.isInvalid()) {
                    // FIXME: gross error message construction
                    lastError = runtime.newInvalidByteSequenceError("\"" + errorBytes.inspect19().toString() + "\" on " + lastResult.inEncoding);
                    lastError.getException().dataWrapStruct(lastResult);
                } else if (lastResult.isUndefined()) {
                    // FIXME: gross error message construction
                    lastError = runtime.newUndefinedConversionError("\"" + errorBytes.inspect19().toString() + "\" from " + lastResult.inEncoding + " to " + lastResult.outEncoding);
                    lastError.getException().dataWrapStruct(lastResult);
                }
            }
        }
    }

    private void createTranscoder(Encoding inEncoding, Encoding outEncoding, CodingActions actions, boolean is7BitASCII) {
        // MRI does not allow ASCII-8BIT bytes > 127 to transcode to text-based
        // encodings, so for transcoding purposes we treat it as US-ASCII. We
        // also set the "invalid" action to "undef" option, and set a flag
        // indicatine this is a binary to character conversion, since Java's
        // decode logic throws "invalid" errors for high-byte US-ASCII rather
        // than "undefined mapping" errors.
        boolean binaryToCharacter = false;
        if (inEncoding == ASCIIEncoding.INSTANCE && outEncoding != ASCIIEncoding.INSTANCE) {
            inEncoding = USASCIIEncoding.INSTANCE;
            actions.onMalformedInput = actions.onUnmappableCharacter;
            binaryToCharacter = true;
        }
        
        Charset inCharset = transcodeCharsetFor(runtime, inEncoding.getName(), inEncoding, is7BitASCII);
        Charset outCharset = transcodeCharsetFor(runtime, outEncoding.getName(), outEncoding, is7BitASCII);
        
        this.transcoder = new TranscoderEngine(
                inCharset,
                outCharset,
                binaryToCharacter);
    }
    
    public class TranscoderEngine {
        public CharBuffer tmpChars;
        public final CharsetDecoder decoder;
        public final CharsetEncoder encoder;
        public boolean binaryToCharacter;
        public RubyCoderResult result;
        public boolean didDecode;
        public boolean didEncode;
        
        public TranscoderEngine(Charset inCharset, Charset outCharset, boolean binaryToCharacter) {
            this.encoder = outCharset.newEncoder();
            this.decoder = inCharset.newDecoder();
            
            // TODO: dynamic buffer size?
            this.tmpChars = CharBuffer.allocate(1024);

            this.binaryToCharacter = binaryToCharacter;
        }
        
        public float averageByteRatio() {
            return encoder.averageBytesPerChar() * decoder.averageCharsPerByte();
        }
    
        public RubyCoderResult transcode(TranscoderState state, int flags) {
            boolean partialInput = (flags & RubyConverter.PARTIAL_INPUT) != 0;
            boolean afterOutput = (flags & RubyConverter.AFTER_OUTPUT) != 0;
            
            int ecflags = actions.ecflags;
            
            boolean universalNewline = (ecflags & RubyConverter.UNIVERSAL_NEWLINE_DECORATOR) != 0;
            boolean crlfNewline = (ecflags & RubyConverter.CRLF_NEWLINE_DECORATOR) != 0;
            boolean crNewline = (ecflags & RubyConverter.CR_NEWLINE_DECORATOR) != 0;
            boolean xmlText = (ecflags & RubyConverter.XML_TEXT_DECORATOR) != 0;
            boolean xmlAttr = (ecflags & RubyConverter.XML_ATTR_CONTENT_DECORATOR) != 0;
            
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

            String replaceString = null;
            didDecode = false;
            didEncode = false;

            if (onUnmappableCharacter == CodingErrorAction.REPLACE ||
                    onMalformedInput == CodingErrorAction.REPLACE) {
                replaceString = replaceStringFromActions(actions, encoder.charset());
            }
            
            // pre-string quote
            if (xmlAttr) {
                tmpChars.clear();
                tmpChars.put("\"");
                tmpChars.flip();
                encode(state, tmpChars, replaceString, ecflags, false);
            }

            while (state.inBytes.hasRemaining()) {
                tmpChars.clear();
                didDecode = true;
                CoderResult coderResult = decoder.decode(state.inBytes, tmpChars, !partialInput);

                if (!coderResult.isError()) {
                    // buffer full or done with input, transfer to output
                    tmpChars.flip();

                    if (!encode(
                            state,
                            doTranslations(universalNewline, crlfNewline, crNewline, xmlText, xmlAttr),
                            replaceString,
                            ecflags, false)) return result;
                    
                    if (coderResult.isUnderflow()) {
                        // ran out of input, bail out
                        break;
                    }
                } else {
                    if (coderResult.isMalformed()) {
                        if (onMalformedInput == CodingErrorAction.REPORT) {
                            byte[] errorBytes = new byte[coderResult.length()];
                            state.inBytes.get(errorBytes);
                            return result = new RubyCoderResult(stringFromCoderResult(coderResult, ecflags, binaryToCharacter), inEncoding, outEncoding, errorBytes, null);
                        }

                        // transfer to out and skip bad byte
                        tmpChars.flip();
                        if (!encode(state, tmpChars, replaceString, ecflags, false)) return result;

                        state.inBytes.get();

                        if (onMalformedInput == CodingErrorAction.REPLACE) {
                            if (!putReplacement(state, replaceString, ecflags)) return result;
                        }
                    } else if (coderResult.isUnmappable()) {
                        if (onUnmappableCharacter == CodingErrorAction.REPORT) {
                            byte[] errorBytes = new byte[coderResult.length()];
                            state.inBytes.get(errorBytes);
                            return result = new RubyCoderResult(stringFromCoderResult(coderResult, ecflags, binaryToCharacter), inEncoding, outEncoding, errorBytes, null);
                        }

                        // transfer to out and skip bad byte
                        tmpChars.flip();
                        if (!encode(state, tmpChars, replaceString, ecflags, false)) return result;

                        state.inBytes.get();

                        if (onUnmappableCharacter == CodingErrorAction.REPLACE) {
                            if (!putReplacement(state, replaceString, ecflags)) return result;
                        }
                    }
                }
                if (afterOutput) break;
            }
            
            // post-string quote
            if (xmlAttr) {
                tmpChars.clear();
                tmpChars.put("\"");
                tmpChars.flip();
                encode(state, tmpChars, replaceString, ecflags, false);
            }

            state.outBytes.flip();
            
            if (partialInput) {
                result = new RubyCoderResult("source_buffer_empty", inEncoding, outEncoding, null, null);
            } else {
                result = new RubyCoderResult("finished", inEncoding, outEncoding, null, null);
            }
            
            return result;
        }
        
        public ByteList finish() {
            TranscoderState state = new TranscoderState(null, ByteBuffer.allocate(16), true);
            
            CharBuffer tmpChars = this.tmpChars;
            CoderResult result;
            
            // final flush of all coders if this is the end of input
            tmpChars.clear();
            if (didDecode) {
                result = decoder.flush(tmpChars);
                
                while (result == CoderResult.OVERFLOW) {
                    tmpChars.flip();
                    CharBuffer newTmp = CharBuffer.allocate(tmpChars.capacity() * 2);
                    newTmp.put(tmpChars);
                    tmpChars = newTmp;
                    result = decoder.flush(tmpChars);
                }
                
                tmpChars.flip();
                
                if (tmpChars.hasRemaining()) {
                    encode(state, tmpChars, actions.replaceWith, 0, true);
                } else {
                    // if nothing to encode, still signal that we're done
                    encoder.encode(tmpChars, state.outBytes, true);
                }
            } else {
                // we did not decode anything, but signal that we're done
                tmpChars.limit(tmpChars.position());
                encoder.encode(tmpChars, state.outBytes, true);
            }

            if (didEncode) {
                result = encoder.flush(state.outBytes);
                while (result == CoderResult.OVERFLOW) {
                    growBuffer(state, 0);
                    result = encoder.flush(state.outBytes);
                }
            }
            
            state.outBytes.flip();
            
            return new ByteList(state.outBytes.array(), state.outBytes.position(), state.outBytes.limit() - state.outBytes.position());
        }
        
        public void reset() {
            decoder.reset();
            encoder.reset();
            tmpChars.clear();
        }
    
        private boolean growBuffer(TranscoderState state, int flags) {
            if (!state.growable) {
                result = new RubyCoderResult(stringFromCoderResult(CoderResult.OVERFLOW, flags, false), inEncoding, outEncoding, null, null);
                return false;
            }

            int toN = state.outBytes.capacity();
            if (toN == MAX_ARRAY_SIZE) {
                // raise error; we can't make a bigger buffer
                throw new ArrayIndexOutOfBoundsException("cannot allocate output buffer larger than " + MAX_ARRAY_SIZE + " bytes");
            }

            // use long for new size so we don't overflow, but don't exceed int max
            toN = (int)Math.min((long)toN * 2 + 1, MAX_ARRAY_SIZE);

            ByteBuffer newOutBytes = ByteBuffer.allocate(toN);
            state.outBytes.flip();
            newOutBytes.put(state.outBytes);
            state.outBytes = newOutBytes;

            return true;
        }

        private boolean encode(TranscoderState state, CharBuffer inChars, String replaceString, int flags, boolean endOfInput) {
            boolean undefHexCharRef = (flags & RubyConverter.UNDEF_MASK) == RubyConverter.UNDEF_HEX_CHARREF;
            
            CoderResult coderResult;

            while (inChars.hasRemaining()) {
                didEncode = true;
                
                coderResult = encoder.encode(inChars, state.outBytes, endOfInput);
                
                if (coderResult.isError() && coderResult.isUnmappable()) {
                    // skip bad char
                    char badChar = inChars.get();

                    if (actions.onUnmappableCharacter == CodingErrorAction.REPORT) {
                        result = new RubyCoderResult(stringFromCoderResult(coderResult, flags, false), inEncoding, outEncoding, Character.toString(badChar).getBytes(decoder.charset()), null);
                        return false;
                    }

                    if (actions.onUnmappableCharacter == CodingErrorAction.REPLACE) {
                        if (undefHexCharRef) {
                            // replace with hex entity
                            if (!putReplacement(state, "&#x" + Integer.toHexString(badChar).toUpperCase() + ";", flags)) return false;
                        } else {
                            if (!putReplacement(state, replaceString, flags)) return false;
                        }
                    }
                } else {
                    if (coderResult == CoderResult.OVERFLOW) {
                        if (!growBuffer(state, flags)) return false;
                    }
                }
            }

            return true;
        }

        private boolean putReplacement(TranscoderState state, String replaceString, int flags) {
            while (state.outBytes.remaining() < replaceString.length() * encoder.maxBytesPerChar()) {
                if (!growBuffer(state, flags)) return false;
            }

            encoder.encode(CharBuffer.wrap(replaceString), state.outBytes, false);

            return true;
        }

        private CharBuffer doTranslations(boolean universalNewline, boolean crlfNewline, boolean crNewline, boolean xmlText, boolean xmlAttr) {
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
                    } else if (xmlText) {
                        if ((replace = XMLTextCharacterTranslator[ch]) != null) {
                            builder.append(replace);
                        } else {
                            builder.append(ch);
                        }
                    } else if (xmlAttr) {
                        if ((replace = XMLAttrCharacterTranslator[ch]) != null) {
                            builder.append(replace);
                        } else {
                            builder.append(ch);
                        }
                    } else {
                        builder.append(ch);
                    }
                }
                
                inChars = CharBuffer.wrap(builder);
            }
            
            return inChars;
        }
    
        public class TranscoderState {
            public final ByteBuffer inBytes;
            public ByteBuffer outBytes;
            public boolean growable;

            public TranscoderState(ByteBuffer inBytes, ByteBuffer outBytes, boolean growable) {
                this.growable = growable;
                this.inBytes = inBytes;
                this.outBytes = outBytes;
            }

            // unused but may be someday
            public TranscoderState(ByteBuffer inBytes, boolean growable) {
                this(inBytes, ByteBuffer.allocate((int)(inBytes.remaining() * averageByteRatio())), growable);
            }
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
    
    public static String stringFromCoderResult(CoderResult coderResult, int flags, boolean binaryToCharacter) {
        if (coderResult == null) return "finished";
        
        if (coderResult.isError()) {
            if (coderResult.isMalformed() && !binaryToCharacter) {
                return "invalid_byte_sequence";
            } else if (coderResult.isUnmappable() || binaryToCharacter) {
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

    private static String replaceStringFromActions(CodingActions actions, Charset outCharset) {
        String replaceString;
        if (actions.replaceWith != null) {
            replaceString = actions.replaceWith;
        } else {
            if (UNICODE_CHARSETS.contains(outCharset)) {
                replaceString = "\uFFFD";
            } else {
                replaceString = "?";
            }
        }
        return replaceString;
    }

    public static class CodingActions {
        CodingErrorAction onUnmappableCharacter;
        CodingErrorAction onMalformedInput;
        String replaceWith;
        public int ecflags;

        CodingActions(CodingErrorAction onUnmappableCharacter,
                CodingErrorAction onMalformedInput, int ecflags, String replaceWith) {
            this.onUnmappableCharacter = onUnmappableCharacter;
            this.onMalformedInput = onMalformedInput;
            this.replaceWith = replaceWith;
            this.ecflags = ecflags;
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
            
        if ((flags & RubyConverter.UNDEF_MASK) == RubyConverter.UNDEF_REPLACE ||
                (flags & RubyConverter.UNDEF_MASK) == RubyConverter.UNDEF_HEX_CHARREF) {
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
        flags |= EncodingUtils.econvPrepareOpts(context, opts, new IRubyObject[]{opts});

        IRubyObject replace = hash.fastARef(runtime.newSymbol("replace"));
        
        return processCodingErrorActions(context, flags, replace);
    }
    
    public static Charset transcodeCharsetFor(Ruby runtime, byte[] name, Encoding encoding, boolean is7Bit) {
        if (encoding == null) {
            EncodingDB.Entry entry = runtime.getEncodingService().findEncodingOrAliasEntry(name);
            if (entry == null) return null;
            encoding = entry.getEncoding();
        }
        
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
            // special cases
            if (is7Bit) return Charset.forName("US-ASCII");

            if (encoding == ISO8859_16Encoding.INSTANCE) return ISO_8859_16.INSTANCE;
            
            throw runtime.newConverterNotFoundError("code converter not found for " + encoding.toString());
        }

        return from;
    }
}
