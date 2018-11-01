package org.jruby.util;

import java.nio.channels.SeekableByteChannel;
import jnr.constants.platform.Errno;
import jnr.constants.platform.Fcntl;
import jnr.enxio.channels.NativeDeviceChannel;
import jnr.posix.FileStat;
import jnr.posix.POSIX;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import org.jruby.ext.fcntl.FcntlLibrary;
import org.jruby.RubyFile;
import org.jruby.platform.Platform;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.PosixShim;

/**
 * Represents a "regular" file, backed by regular file system.
 */
class RegularFileResource implements FileResource {

    private final JRubyFile file;
    private final String filePath; // original (non-normalized) file-path
    private final POSIX posix;

    RegularFileResource(POSIX posix, JRubyFile file, String filePath) {
        this.file = file;
        this.filePath = filePath;
        this.posix = posix;
    }

    protected RegularFileResource(POSIX posix, String filename) {
        this.file = new JRubyFile(filename);
        this.filePath = filename;
        this.posix = posix;
    }

    @Override
    public String absolutePath() {
        return RubyFile.canonicalize(file.getAbsolutePath());
    }

    @Override
    public String canonicalPath() {
        // Seems like for Ruby absolute path implies resolving system links,
        // so canonicalization is in order.
        try {
            return file.getCanonicalPath();
        } catch (IOException ioError) {
            // I guess absolute path is next best thing?
            return file.getAbsolutePath();
        }
    }

    @Override
    public long length() {
        return file.length();
    }

    @Override
    public long lastModified() {
        return file.lastModified();
    }

    public FileTime lastModifiedTime() throws IOException {
        return FileTime.fromMillis(file.lastModified());
    }

    public FileTime lastAccessTime() throws IOException {
        return readAttributes().lastAccessTime();
    }

    public FileTime creationTime() throws IOException {
        return readAttributes().creationTime();
    }

    private transient BasicFileAttributes fileAttributes;

    private BasicFileAttributes readAttributes() throws IOException {
        if (fileAttributes != null) return fileAttributes;
        Path path = FileSystems.getDefault().getPath(file.getPath());
        return fileAttributes = Files.readAttributes(path, BasicFileAttributes.class);
    }

    @Override
    public boolean exists() {
        if (file.exists()) {
            String path = filePath;
            if (path.length() > 1 && path.charAt(path.length() - 1) == '/') {
                path = file.getPathDefault();
                if (path.length() > 0 && path.charAt(path.length() - 1) != '/' && !isDirectory()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean canExecute() {
        return file.canExecute();
    }

    public int errno() {
        return posix.errno();
    }

    @Override
    public boolean isFile() {
        return file.isFile();
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public boolean isSymLink() {
        try {
            // Note: We can't use file.exists() to check whether the symlink exists or not, because that method
            // returns false for existing but broken symlink. So, we try without the existence check, but in the
            // try-catch block.
            // MRI behavior: symlink? on broken symlink should return true.
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

            return attrs != null && attrs.isSymbolicLink();
        } catch (SecurityException|IOException|UnsupportedOperationException ex) {
            return false;
        }
    }

    @Override
    public boolean canRead() {
        return file.canRead();
    }

    @Override
    public boolean canWrite() {
        return file.canWrite();
    }

    @Override
    public String[] list() {
        String[] fileList = file.list();

        if (fileList == null) return null;

        // If we got some entries, then it's probably a directory and in Ruby all file
        // directories should have '.' and '..' entries
        String[] list = new String[fileList.length + 2];
        list[0] = ".";
        list[1] = "..";
        System.arraycopy(fileList, 0, list, 2, fileList.length);

        return list;
    }

    @Override
    public FileStat stat() {
        FileStat stat = posix.allocateStat();

        return posix.stat(file.getAbsolutePath(), stat) < 0 ? null : stat;
    }

    @Override
    public FileStat lstat() {
        FileStat stat = posix.allocateStat();

        return posix.lstat(file.getAbsolutePath(), stat) < 0 ? null : stat;
    }

    @Override
    public String toString() {
        return file.toString();
    }

    @Override
    public InputStream openInputStream() throws IOException {
        if (!exists()) {
            throw new ResourceException.NotFound(absolutePath());
        }
        if (isDirectory()) {
            throw new ResourceException.FileIsDirectory(absolutePath());
        }
        return new FileInputStream(file);
    }

    @Override
    public Channel openChannel(final int flags, int perm) throws IOException {
        final ModeFlags modeFlags = ModeFlags.createModeFlags(flags);
        if (posix.isNative() && !Platform.IS_WINDOWS) {
            int fd = posix.open(absolutePath(), modeFlags.getFlags(), perm);
            if (fd < 0) throwFromErrno(posix.errno());
            posix.fcntlInt(fd, Fcntl.F_SETFD, posix.fcntl(fd, Fcntl.F_GETFD) | FcntlLibrary.FD_CLOEXEC);
            return new NativeDeviceChannel(fd, true);
        }

        if (modeFlags.isCreate()) {
            boolean fileCreated;
            try {
                fileCreated = file.createNewFile();
            } catch (IOException ioe) {
                // See JRUBY-4380.
                // when the directory for the file doesn't exist.
                // Java in such cases just throws IOException.
                File parent = file.getParentFile();
                if (parent != null && parent != file && !parent.exists()) {
                    throw new ResourceException.NotFound(absolutePath());
                } else if (!file.canWrite()) {
                    throw new ResourceException.PermissionDenied(absolutePath());
                } else {
                    // for all other IO errors, we report it as general IO error
                    throw ioe;
                }
            }

            if (!fileCreated && modeFlags.isExclusive()) {
                throw new ResourceException.FileExists(absolutePath());
            }

            Channel channel = createChannel(modeFlags);

            // attempt to set the permissions, if we have been passed a POSIX instance,
            // perm is > 0, and only if the file was created in this call.
            if (fileCreated && posix != null) {
                perm = perm & ~PosixShim.umask(posix);

                if (perm > 0) posix.chmod(file.getPath(), perm);
            }

            return channel;
        }

        if (file.isDirectory()) {
            throw new ResourceException.FileIsDirectory(absolutePath());
        }

        if (!file.exists()) {
            throw new ResourceException.NotFound(absolutePath());
        }

        return createChannel(modeFlags);
    }

    private Channel createChannel(ModeFlags flags) throws IOException {
        SeekableByteChannel fileChannel;

        try{
            if (flags.isWritable() && !flags.isReadable()) {
                FileOutputStream fos = new FileOutputStream(file, flags.isAppendable());
                fileChannel = fos.getChannel();
            } else {
                RandomAccessFile raf = new RandomAccessFile(file, flags.toJavaModeString());
                fileChannel = raf.getChannel();

                // O_APPEND specifies that all writes will always be at the end of the open file
                // (even if we happened to have seek'd away from the end of the file o_O). RAF
                // does not have these semantics so we wrap it to support this unusual case.
                if (flags.isAppendable()) fileChannel = new AppendModeChannel((FileChannel) fileChannel);
            }
        }
        catch (FileNotFoundException ex) {
            throw mapFileNotFoundOnGetChannel(this, ex);
        }

        try {
            if (flags.isTruncate()) fileChannel.truncate(0);
        }
        catch (IOException ioe) {
            // ignore; it's a pipe or fifo that can't be truncated (we only care about illegal seek).
            if (!"Illegal seek".equals(ioe.getMessage())) throw ioe;
        }

        return fileChannel;
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        if (type == File.class || type == JRubyFile.class) return (T) file;
        throw new UnsupportedOperationException("unwrap: " + type.getName());
    }

    static IOException mapFileNotFoundOnGetChannel(final FileResource file, final FileNotFoundException ex) {
        // Java throws FileNotFoundException both if the file doesn't exist or there were
        // permission issues, but Ruby needs to disambiguate those two cases
        return file.exists() ?
                new ResourceException.PermissionDenied(file.absolutePath()) :
                new ResourceException.NotFound(file.absolutePath());
    }

    private void throwFromErrno(final int err) throws IOException {
        Errno errno = Errno.valueOf(err);
        switch (errno) {
            case EACCES:
                throw new ResourceException.PermissionDenied(absolutePath());
            case EEXIST:
                throw new ResourceException.FileExists(absolutePath());
            case EINVAL:
                throw new ResourceException.InvalidArguments(absolutePath());
            case ENOENT:
                throw new ResourceException.NotFound(absolutePath());
            case ELOOP:
                throw new ResourceException.TooManySymlinks(absolutePath());
            case EISDIR:
                throw new ResourceException.FileIsDirectory(absolutePath());
            case ENOTDIR:
                throw new ResourceException.FileIsNotDirectory(absolutePath());
            case EMFILE:
            default:
                throw new InternalIOException(errno.description());

        }
    }

    static class InternalIOException extends IOException {

        InternalIOException(final String message) {
            super(message);
        }

        @Override
        public Throwable fillInStackTrace() { return this; }

    }

}
