package org.jruby.util;

import jnr.posix.FileStat;
import jnr.posix.POSIX;
import org.jruby.util.io.ModeFlags;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;

/**
 * Represents a the NUL: device on Windows, which is not a normal file.
 */
class NullDeviceResource extends AbstractFileResource {
    private static final JRubyFile file = new JRubyFile("NUL:");
    private final POSIX posix;

    NullDeviceResource(POSIX posix) {
        this.posix = posix;
    }

    @Override
    public String absolutePath() {
        return "NUL:";
    }

    @Override
    public String canonicalPath() {
        return "NUL:";
    }

    @Override
    public long length() {
        return 0;
    }

    @Override
    public long lastModified() {
        return 0;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public boolean canExecute() {
        return false;
    }

    public int errno() {
        return posix.errno();
    }

    @Override
    public boolean isFile() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isSymLink() {
        return false;
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    public boolean canWrite() {
        return true;
    }

    @Override
    public String[] list() {
        return null;
    }

    @Override
    public FileStat stat() {
        return null;
    }

    @Override
    public FileStat lstat() {
        return null;
    }

    @Override
    public String toString() {
        return "NUL:";
    }

    @Override
    public JRubyFile hackyGetJRubyFile() {
        return file;
    }

    @Override
    InputStream openInputStream() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public Channel openChannel(ModeFlags flags, int perm) throws ResourceException {
        return createChannel(flags);
    }

    private Channel createChannel(ModeFlags flags) throws ResourceException {
        FileChannel fileChannel;

        /* Because RandomAccessFile does not provide a way to pass append
         * mode, we must manually seek if using RAF. FileOutputStream,
         * however, does properly honor append mode at the lowest levels,
         * reducing append write costs when we're only doing writes.
         *
         * The code here will use a FileOutputStream if we're only writing,
         * setting isInAppendMode to true to disable our manual seeking.
         *
         * RandomAccessFile does not handle append for us, so if we must
         * also be readable we pass false for isInAppendMode to indicate
         * we need manual seeking.
         */
        try{
            if (flags.isWritable() && !flags.isReadable()) {
                FileOutputStream fos = new FileOutputStream(file, flags.isAppendable());
                fileChannel = fos.getChannel();
            } else {
                RandomAccessFile raf = new RandomAccessFile(file, flags.toJavaModeString());
                fileChannel = raf.getChannel();
            }
        } catch (FileNotFoundException fnfe) {
            // Jave throws FileNotFoundException both if the file doesn't exist or there were
            // permission issues, but Ruby needs to disambiguate those two cases
            throw file.exists() ?
                    new ResourceException.PermissionDenied(absolutePath()) :
                    new ResourceException.NotFound(absolutePath());
        } catch (IOException ioe) {
            throw new ResourceException.IOError(ioe);
        }

        try {
            if (flags.isTruncate()) fileChannel.truncate(0);
        } catch (IOException ioe) {
            // ignore; it's a pipe or fifo that can't be truncated (we only care about illegal seek).
            if (!ioe.getMessage().equals("Illegal seek")) throw new ResourceException.IOError(ioe);
        }

        return fileChannel;
    }
}
