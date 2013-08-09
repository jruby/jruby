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

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;
import org.jruby.util.io.EncodingUtils;

/**
 * Abstract superclass for all transcoders.
 * 
 * This and its implementations are roughly equivalent to rb_econv_t in MRI.
 */
public abstract class Transcoder {
    protected final Ruby runtime;
    public final Encoding outEncoding;
    public final Encoding inEncoding;
    public RubyCoderResult lastResult;
    private RaiseException lastError;
    
    public Transcoder(ThreadContext context, Encoding outEncoding, Encoding inEncoding) {
        this.runtime = context.runtime;
        this.outEncoding = outEncoding;
        this.inEncoding = inEncoding;
    }
    
    // rb_econv_open
    public static Transcoder open(ThreadContext context, byte[] sourceEncoding, byte[] destinationEncoding, int ecflags, IRubyObject replacement) {
        // TODO: decorator finish logic
        
        // TODO: lighter-weight pass for non-transcoding with decorators (NullTranscoder)
        
        // TODO: set error handler mask for decorator logic
//        Transcoder transcoder = open0(ThreadContext context, sourceEncoding, destinationEncoding, ecflags & EncodingUtils.ECONV_ERROR_HANDLER_MASK);
        Transcoder transcoder = open0(context, sourceEncoding, destinationEncoding, ecflags, replacement);
        
        if (transcoder == null) return null;
        
        // TODO: decorator finish logic
        
        // TODO: clear error handler mask
        
        return transcoder;
    }
    
    public static Transcoder open0(ThreadContext context, byte[] sourceEncoding, byte[] destinationEncoding, int ecflags, IRubyObject replacement) {
        Encoding senc, denc;
        
        senc = null;
        if (sourceEncoding.length > 0) {
            EncodingDB.Entry src = context.runtime.getEncodingService().findEncodingOrAliasEntry(new ByteList(sourceEncoding, false));
            if (src != null) {
                senc = src.getEncoding();
            }
        }
        
        denc = null;
        if (destinationEncoding.length > 0) {
            EncodingDB.Entry dest = context.runtime.getEncodingService().findEncodingOrAliasEntry(new ByteList(destinationEncoding, false));
            if (dest != null) {
                denc = dest.getEncoding();
            }
        }
        
        if (sourceEncoding.length == 0 && destinationEncoding.length == 0) {
            // no transcoding; for our purposes, we force both to be binary
            senc = denc = ASCIIEncoding.INSTANCE;
        } else {
            Charset from;
            Charset to;
            
            // inefficient; doing Charset lookup here *and* in the transcoder
            if (CharsetTranscoder.transcodeCharsetFor(context.runtime, sourceEncoding, senc, false) == null ||
                    CharsetTranscoder.transcodeCharsetFor(context.runtime, destinationEncoding, denc, false) == null) {
                return null;
            }
        }
            
        return new CharsetTranscoder(context,
                denc,
                senc,
                ecflags,
                replacement);
    }
    
    /**
     * This will try and transcode the supplied ByteList to the supplied toEncoding.  It will use
     * forceEncoding as its encoding if it is supplied; otherwise it will use the encoding it has
     * tucked away in the bytelist.  This will return a new copy of a ByteList in the request
     * encoding or die trying (ConverterNotFound).
     * 
     * c: rb_str_conv_enc_opts
     */
    public static ByteList strConvEncOpts(ThreadContext context, ByteList value, Encoding fromEncoding,
            Encoding toEncoding, int ecflags, IRubyObject ecopts) {
        if (toEncoding == null) return value;
        if (fromEncoding == null) fromEncoding = value.getEncoding();
        if (fromEncoding == toEncoding) return value;
        
        // This logic appears to not work like in MRI; following code will not
        // properly decode the string:
        // "\x00a".force_encoding("ASCII-8BIT").encode("UTF-8", "UTF-16BE")
        if ((toEncoding.isAsciiCompatible() && StringSupport.codeRangeScan(value.getEncoding(), value) == StringSupport.CR_7BIT) ||
                toEncoding == ASCIIEncoding.INSTANCE) {
            if (value.getEncoding() != toEncoding) {
                value = value.shallowDup();
                value.setEncoding(toEncoding);
            }
            return value;
        }
        
        Transcoder ec = EncodingUtils.econvOpenOpts(context, fromEncoding.getName(), toEncoding.getName(), ecflags, ecopts);
        if (ec == null) return value;
        
        ByteList ret = ec.convert(context, value, false);
        
        ret.setEncoding(toEncoding);
        
        return ret;
    }
    
    // rb_str_conv_enc
    public static ByteList strConvEnc(ThreadContext context, ByteList value, Encoding fromEncoding, Encoding toEncoding) {
        return strConvEncOpts(context, value, fromEncoding, toEncoding, 0, context.nil);
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
        
        return new CharsetTranscoder(context, toEncoding, fromEncoding, CharsetTranscoder.processCodingErrorActions(context, opts)).transcode(context, value, is7BitASCII);
    }
    
    // rb_econv_convert
    public abstract RubyCoderResult transcode(ThreadContext context, ByteList value, ByteList dest);
    
    public abstract ByteList transcode(ThreadContext context, ByteList value);
    
    public abstract ByteList transcode(ThreadContext context, ByteList value, boolean is7BitASCII);
    
    // from Converter#convert
    public abstract ByteList convert(ThreadContext context, ByteList value, boolean is7BitASCII);
    
    public abstract ByteList econvStrConvert(ThreadContext context, ByteList value, boolean finish);
    
    public abstract RubyCoderResult primitiveConvert(ThreadContext context, ByteList inBuffer, ByteList outBuffer, int outOffset, int outLimit, Encoding inEncoding, boolean is7BitASCII, int flags);
    
    public abstract ByteList finish(Encoding altEncoding);
    
    public RubyCoderResult getLastResult() {
        return lastResult;
    }
    
    public RaiseException getLastError() {
        createLastError();
        
        return lastError;
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
}
