package org.jruby.util.io;

import java.io.IOException;
import org.jruby.util.ByteList;

/**
 * Hacky marker interface for 1.9 support of CRLFStreamWrapper.
 * So it and the natural stream ChannelStream can both call nonblockingwrite.
 */
public interface NonblockWritingStream {
    public int writenonblock(ByteList buf) throws IOException, BadDescriptorException;
}
