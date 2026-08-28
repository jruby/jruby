package org.jruby.exceptions;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Represents a buffer overflow (too much data returned) when calling `readpartial` on an IO-like object.
 *
 * See jruby/jruby#6246 and ruby/webrick#43
 *
 * @see org.jruby.util.IOChannel.IOReadableByteChannel#read(ByteBuffer)
 */
public class ReadPartialBufferOverflowException extends IOException {
    public ReadPartialBufferOverflowException(String message) {
        super(message);
    }
}
