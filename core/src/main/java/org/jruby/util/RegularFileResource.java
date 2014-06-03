package org.jruby.util;

import jnr.constants.platform.Errno;
import jnr.enxio.channels.NativeDeviceChannel;
import jnr.posix.FileStat;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;

import org.jruby.Ruby;
import org.jruby.RubyFile;
import org.jruby.platform.Platform;
import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.ModeFlags;

import jnr.posix.JavaSecuredFile;
import org.jruby.util.io.PosixShim;

import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.io.IOException;

/**
 * Represents a "regular" file, backed by regular file system.
 */
class RegularFileResource implements FileResource {
    private final JRubyFile file;
    private final POSIX symlinkPosix = POSIXFactory.getPOSIX();

    RegularFileResource(File file) {
        this(file.getAbsolutePath());
    }

    protected RegularFileResource(String filename) {
        this.file = new JRubyFile(filename);
    }

    @Override
    public String absolutePath() {
        return file.getAbsolutePath();
    }

    @Override
    public long length() {
        return file.length();
    }

    @Override
    public long lastModified() {
        return file.lastModified();
    }

    @Override
    public boolean exists() {
        // MRI behavior: Even broken symlinks should return true.
        return file.exists() || isSymLink();
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
            return symlinkPosix.lstat(absolutePath()).isSymlink();
        } catch (Throwable t) {
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

        if (fileList != null) {
            // If we got some entries, then it's probably a directory and in Ruby all file
            // directories should have '.' and '..' entries
            String[] list = new String[fileList.length + 2];
            list[0] = ".";
            list[1] = "..";
            for (int i = 0; i < fileList.length; i++) {
                list[i+2] = fileList[i];
            }
            fileList = list;
        }

        return fileList;
    }

    @Override
    public FileStat stat(POSIX posix) {
        return posix.stat(absolutePath());
    }

    @Override
    public FileStat lstat(POSIX posix) {
        return posix.lstat(absolutePath());
    }

    @Override
    public String toString() {
        return file.toString();
    }

    @Override
    public JRubyFile hackyGetJRubyFile() {
      return file;
    }

    @Override
    public Channel openChannel(ModeFlags flags, POSIX posix, int perm) throws ResourceException {
//        if (posix.isNative()) {
//            int fd = posix.open(absolutePath(), flags.getFlags(), perm);
//            if (fd < 0) {
//                Errno errno = Errno.valueOf(posix.errno());
//                switch (errno) {
//                    case EACCES:
//                        throw new ResourceException.PermissionDenied(absolutePath());
//                    case EEXIST:
//                        throw new ResourceException.FileExists(absolutePath());
//                    case EINVAL:
//                        throw new ResourceException.InvalidArguments(absolutePath());
//                    case ENOENT:
//                        throw new ResourceException.NotFound(absolutePath());
//                    default:
//                        throw new ResourceException.IOError(new IOException("unhandled errno: " + errno));
//
//                }
//            }
//            return new NativeDeviceChannel(fd);
//        }

        if (flags.isCreate()) {
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
                    throw new ResourceException.IOError(ioe);
                }
            }

            if (!fileCreated && flags.isExclusive()) {
                throw new ResourceException.FileExists(absolutePath());
            }

            Channel channel = createChannel(flags);

            // attempt to set the permissions, if we have been passed a POSIX instance,
            // perm is > 0, and only if the file was created in this call.
            if (fileCreated && posix != null) {
                perm = perm & ~PosixShim.umask(posix);
                if (posix != null && perm > 0) {
                    posix.chmod(file.getPath(), perm);
                }
            }

            return channel;
        }

        if (file.isDirectory() && flags.isWritable()) {
            throw new ResourceException.FileIsDirectory(absolutePath());
        }

        if (!file.exists()) {
            throw new ResourceException.NotFound(absolutePath());
        }

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
            if (ioe.getMessage().equals("Illegal seek")) {
                // ignore; it's a pipe or fifo that can't be truncated
                // ignore; it's a pipe or fifo that can't be truncated
            } else {
                throw new ResourceException.IOError(ioe);
            }
        }

        return fileChannel;
    }

    @Override
    @Deprecated
    public ChannelDescriptor openDescriptor(ModeFlags flags, POSIX posix, int perm) throws ResourceException {
        if (flags.isCreate()) {
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
                    throw new ResourceException.IOError(ioe);
                }
            }

            if (!fileCreated && flags.isExclusive()) {
                throw new ResourceException.FileExists(absolutePath());
            }

            ChannelDescriptor descriptor = createDescriptor(flags);

            // attempt to set the permissions, if we have been passed a POSIX instance,
            // perm is > 0, and only if the file was created in this call.
            if (fileCreated && posix != null && perm > 0) {
                if (posix != null && perm > 0) {
                    posix.chmod(file.getPath(), perm);
                }
            }

            return descriptor;
        }

        if (file.isDirectory() && flags.isWritable()) {
            throw new ResourceException.FileIsDirectory(absolutePath());
        }

        if (!file.exists()) {
            throw new ResourceException.NotFound(absolutePath());
        }

        return createDescriptor(flags);
    }

    @Deprecated
    private ChannelDescriptor createDescriptor(ModeFlags flags) throws ResourceException {
        FileDescriptor fileDescriptor;
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
        boolean isInAppendMode;
        try{
            if (flags.isWritable() && !flags.isReadable()) {
                FileOutputStream fos = new FileOutputStream(file, flags.isAppendable());
                fileChannel = fos.getChannel();
                fileDescriptor = fos.getFD();
                isInAppendMode = true;
            } else {
                RandomAccessFile raf = new RandomAccessFile(file, flags.toJavaModeString());
                fileChannel = raf.getChannel();
                fileDescriptor = raf.getFD();
                isInAppendMode = false;
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
            if (ioe.getMessage().equals("Illegal seek")) {
                // ignore; it's a pipe or fifo that can't be truncated
                // ignore; it's a pipe or fifo that can't be truncated
            } else {
                throw new ResourceException.IOError(ioe);
            }
        }

        // TODO: append should set the FD to end, no? But there is no seek(int) in libc!
        //if (modes.isAppendable()) seek(0, Stream.SEEK_END);

        return new ChannelDescriptor(fileChannel, flags, fileDescriptor, isInAppendMode);
    }
}
