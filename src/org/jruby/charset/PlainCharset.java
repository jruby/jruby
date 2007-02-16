package org.jruby.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

public class PlainCharset extends Charset {
    public PlainCharset() {
        super("PLAIN", new String[0]);
    }
    public boolean contains(final Charset chs) {
        return chs instanceof PlainCharset;
    }
    
    public CharsetDecoder newDecoder() {
        return new PlainCharsetDecoder(this);
    }

    public CharsetEncoder newEncoder() {
        return new PlainCharsetEncoder(this);
    }

    private static class PlainCharsetDecoder extends CharsetDecoder {
        PlainCharsetDecoder(final PlainCharset charset) {
            super(charset,1F,1F);
        }
        public CoderResult decodeLoop(final ByteBuffer in, final CharBuffer out) {
            while(in.remaining() > 0 && out.remaining() > 0) {
                out.put((char)(in.get() & 0xFF));
            }
            if(in.remaining() > 0) {
                return CoderResult.OVERFLOW;
            }
            return CoderResult.UNDERFLOW;
        }
    }

    private static class PlainCharsetEncoder extends CharsetEncoder {
        PlainCharsetEncoder(final PlainCharset charset) {
            super(charset,1F,1F);
        }
        public CoderResult encodeLoop(final CharBuffer in, final ByteBuffer out) {
            while(in.remaining() > 0 && out.remaining() > 0) {
                out.put((byte)in.get());
            }
            if(in.remaining() > 0) {
                return CoderResult.OVERFLOW;
            }
            return CoderResult.UNDERFLOW;
        }
    }
}
