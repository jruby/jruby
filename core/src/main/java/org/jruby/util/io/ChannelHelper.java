/*
 * Copyright (c) 2015 JRuby.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *    JRuby - initial API and implementation and/or initial documentation
 */
package org.jruby.util.io;

import org.jruby.RubyInstanceConfig;
import org.jruby.javasupport.Java;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;

/**
 * Helper that attempts to improve Channels' static helpers.
 * @author kares
 */
public abstract class ChannelHelper {

    private static final Field filterInField;
    private static final Field filterOutField;

    private ChannelHelper() { /* */ }

    static {
        Field _filterInField = null, _filterOutField = null;
        try {
            _filterInField = FilterInputStream.class.getDeclaredField("in");
        } catch (Exception e) {
        }
        try {
            _filterOutField = FilterOutputStream.class.getDeclaredField("out");
        } catch (Exception e) {
        }
        filterInField = _filterInField;
        filterOutField = _filterOutField;
    }

    public static ReadableByteChannel readableChannel(final InputStream inputStream) {
        if ( inputStream instanceof ByteArrayInputStream ) {
            if ( SeekableByteChannelImpl.USABLE ) {
                return new SeekableByteChannelImpl((ByteArrayInputStream) inputStream);
            }
        }
        return Channels.newChannel(inputStream);
    }

    public static WritableByteChannel writableChannel(final OutputStream outputStream) {
        return new SyncOutputStreamChannel(outputStream);
    }
    
    private static class SyncOutputStreamChannel extends AbstractInterruptibleChannel implements WritableByteChannel {
        private final OutputStream output;
        
        SyncOutputStreamChannel(OutputStream output) {
            this.output = output;
            this.out = Channels.newChannel(output);
        }
        @Override
        protected void implCloseChannel() throws IOException {
            out.close();
        }

        final WritableByteChannel out;

        @Override
        public int write(ByteBuffer src) throws IOException {
            int written = out.write(src);
            output.flush();
            return written;
        }
    }

    /**
     * Unwrap all filtering streams between the given stream and its actual
     * unfiltered stream. This is primarily to unwrap streams that have
     * buffers that would interfere with interactivity.
     *
     * @param filteredStream The stream to unwrap
     * @return An unwrapped stream, presumably unbuffered
     */
    public static OutputStream unwrapBufferedStream(OutputStream filteredStream) {
        if (RubyInstanceConfig.NO_UNWRAP_PROCESS_STREAMS) return filteredStream;

        return unwrapFilterOutputStream(filteredStream);
    }

    /**
     * Unwrap all filtering streams between the given stream and its actual
     * unfiltered stream. This is primarily to unwrap streams that have
     * buffers that would interfere with interactivity.
     *
     * @param filteredStream The stream to unwrap
     * @return An unwrapped stream, presumably unbuffered
     */
    public static InputStream unwrapBufferedStream(InputStream filteredStream) {
        if (RubyInstanceConfig.NO_UNWRAP_PROCESS_STREAMS) return filteredStream;

        // Java 7+ uses a stream that drains the child on exit, which when
        // unwrapped breaks because the channel gets drained prematurely.
        if (filteredStream.getClass().getName().indexOf("ProcessPipeInputStream") != 1) {
            return filteredStream;
        }

        return unwrapFilterInputStream((FilterInputStream)filteredStream);
    }

    /**
     * Unwrap the given stream to its first non-FilterOutputStream. If the stream is not
     * a FilterOutputStream it is returned immediately.
     *
     * Note that this version is used when you are absolutely sure you want to unwrap;
     * the unwrapBufferedStream version will perform checks for certain types of
     * process-related streams that should not be unwrapped (Java 7+ Process, e.g.).
     *
     * @param filteredStream a stream to be unwrapped, if it is a FilterOutputStream
     * @return the deeped non-FilterOutputStream stream, or filterOutputStream if it is
     *         not a FilterOutputStream to begin with.
     */
    public static OutputStream unwrapFilterOutputStream(OutputStream filteredStream) {
        if (filterOutField != null) {
            while (filteredStream instanceof FilterOutputStream) {
                try {
                    OutputStream tmpStream =
                            Java.trySetAccessible(filterOutField)
                                    ? (OutputStream) filterOutField.get(filteredStream)
                                    : null;

                    // try to unwrap as a Drip stream
                    if (!(tmpStream instanceof FilterOutputStream)) {
                        // try to get stream out of drip stream
                        OutputStream dripStream = unwrapDripStream(tmpStream);

                        if (dripStream != null) {
                            // got it, use it for the next cycle
                            tmpStream = dripStream;
                        }
                    }

                    filteredStream = tmpStream;
                } catch (Exception e) {
                    break; // break out if we've dug as deep as we can
                }
            }
        }
        return filteredStream;
    }

    /**
     * Unwrap the given stream to its first non-FilterInputStream. If the stream is not
     * a FilterInputStream it is returned immediately.
     *
     * Note that this version is used when you are absolutely sure you want to unwrap;
     * the unwrapBufferedStream version will perform checks for certain types of
     * process-related streams that should not be unwrapped (Java 7+ Process, e.g.).
     *
     * @param filteredStream a stream to be unwrapped, if it is a FilterInputStream
     * @return the deeped non-FilterInputStream stream, or filterInputStream if it is
     *         not a FilterInputStream to begin with.
     */
    public static InputStream unwrapFilterInputStream(InputStream filteredStream) {
        if (filterInField != null) {
            while (filteredStream instanceof FilterInputStream) {
                try {
                    InputStream tmpStream =
                            Java.trySetAccessible(filterInField)
                                    ? (InputStream) filterInField.get(filteredStream)
                                    : null;

                    // could not acquire
                    if (tmpStream == null) break;

                    // try to unwrap result as a Drip stream
                    if (!(tmpStream instanceof FilterInputStream)) {

                        // try to get stream out of drip stream
                        InputStream dripStream = unwrapDripStream(tmpStream);

                        if (dripStream != null) {
                            // got it, use it for the next cycle
                            tmpStream = dripStream;
                        }
                    }

                    filteredStream = tmpStream;
                } catch (Exception e) {
                    break; // break out if we've dug as deep as we can
                }
            }
        }
        return filteredStream;
    }

    private static OutputStream unwrapDripStream(OutputStream stream) {
        if (isDripSwitchable(stream)) {
            try {
                Field out = stream.getClass().getDeclaredField("out");
                return Java.trySetAccessible(out) ? (OutputStream) out.get(stream) : null;
            } catch (Exception e) {
            }
        }
        return null;
    }

    private static InputStream unwrapDripStream(InputStream stream) {
        if (isDripSwitchable(stream)) {
            try {
                Field in = stream.getClass().getDeclaredField("in");
                return Java.trySetAccessible(in) ? (InputStream) in.get(stream) : null;
            } catch (Exception e) {
            }
        }
        return null;
    }

    private static boolean isDripSwitchable(Object stream) {
        return stream.getClass().getName().startsWith("org.flatland.drip.Switchable");
    }

}
