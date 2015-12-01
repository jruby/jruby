/*
 * Copyright (c) 2015 JRuby.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    JRuby - initial API and implementation and/or initial documentation
 */
package org.jruby.util.io;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Helper that attempts to improve Channels' static helpers.
 * @author kares
 */
public abstract class ChannelHelper {

    private ChannelHelper() { /* */ }

    public static ReadableByteChannel readableChannel(final InputStream inputStream) {
        if ( inputStream instanceof ByteArrayInputStream ) {
            if ( SeekableByteChannelImpl.USABLE ) {
                return new SeekableByteChannelImpl((ByteArrayInputStream) inputStream);
            }
        }
        return Channels.newChannel(inputStream);
    }

    public static WritableByteChannel writableChannel(final OutputStream ouputStream) {
        return Channels.newChannel(ouputStream);
    }

}
