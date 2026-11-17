package org.jruby.util;

import jnr.constants.platform.Errno;
import jnr.posix.FileStat;
import org.jruby.util.io.ModeFlags;

import java.io.*;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.file.attribute.FileTime;

import static org.jruby.util.RegularFileResource.mapFileNotFoundOnGetChannel;

/**
 * Represents a the NUL: device on Windows, which is not a normal file.
 */
final class NullDeviceResource implements FileResource {
    private static final String NUL_DEVICE = "NUL:";
    private static final JRubyFile file = new JRubyFile(NUL_DEVICE);

    NullDeviceResource() { /* no-op */ }

    @Override
    public String absolutePath() {
        return NUL_DEVICE;
    }

    @Override
    public String canonicalPath() {
        return NUL_DEVICE;
    }

    @Override
    public String path() {
        return NUL_DEVICE;
    }

    @Override
    public long length() {
        return 0;
    }

    @Override
    public long lastModified() {
        return 0;
    }

    public FileTime creationTime() {
        return FileTime.fromMillis(lastModified());
    }

    public FileTime lastModifiedTime() {
        return FileTime.fromMillis(lastModified());
    }

    public FileTime lastAccessTime() {
        return FileTime.fromMillis(lastModified());
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
        // we're not using posix.stat + Java does treat us with a FileNotFoundException
        return Errno.ENOENT.intValue();
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
    public boolean isNull() { return true; }

    @Override
    public InputStream openInputStream() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public Channel openChannel(int flags, int perm) throws IOException {
        return createChannel(ModeFlags.createModeFlags(flags));
    }

    private Channel createChannel(ModeFlags flags) throws IOException {
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
        }
        catch (FileNotFoundException ex) {
            throw mapFileNotFoundOnGetChannel(this, ex);
        }

        return fileChannel;
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        if (type == File.class || type == JRubyFile.class) return (T) file;
        throw new UnsupportedOperationException("unwrap: " + type.getName());
    }

}
