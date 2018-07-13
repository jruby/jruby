package org.jruby.util;

import jnr.posix.FileStat;
import org.jruby.runtime.Helpers;

import java.io.IOException;
import java.nio.file.attribute.FileTime;

final class DummyResourceStat implements FileStat {

    // NOTE: maybe move this to FileResource
    interface FileResourceExt extends FileResource {
        FileTime creationTime() throws IOException;
        FileTime lastAccessTime() throws IOException;
        FileTime lastModifiedTime() throws IOException;
    }

    private final FileResourceExt resource;

    DummyResourceStat(FileResourceExt resource) {
        this.resource = resource;
    }

    @Override
    public long blocks() {
        return resource.length();
    }

    @Override
    public long blockSize() {
        return 1L;
    }

    @Override
    public long dev() {
        return -1;
    }

    @Override
    public String ftype() {
        return "unknown";
    }

    @Override
    public int gid() {
        return -1;
    }

    @Override
    public boolean groupMember(int i) {
        return false;
    }

    @Override
    public long ino() {
        return -1;
    }

    @Override
    public boolean isBlockDev() {
        return false;
    }

    @Override
    public boolean isCharDev() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return resource.isDirectory();
    }

    @Override
    public boolean isEmpty() {
        return resource.length() == 0;
    }

    @Override
    public boolean isExecutable() {
        return false;
    }

    @Override
    public boolean isExecutableReal() {
        return false;
    }

    @Override
    public boolean isFifo() {
        return false;
    }

    @Override
    public boolean isFile() {
        return resource.isFile();
    }

    @Override
    public boolean isGroupOwned() {
        return false;
    }

    @Override
    public boolean isIdentical(FileStat fs) {
        return fs instanceof DummyResourceStat && ((DummyResourceStat) fs).resource.equals(resource);
    }

    @Override
    public boolean isNamedPipe() {
        return false;
    }

    @Override
    public boolean isOwned() {
        return false;
    }

    @Override
    public boolean isROwned() {
        return false;
    }

    @Override
    public boolean isReadable() {
        return resource.canRead();
    }

    @Override
    public boolean isReadableReal() {
        return isReadable();
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public boolean isWritableReal() {
        return isWritable();
    }

    @Override
    public boolean isSetgid() {
        return false;
    }

    @Override
    public boolean isSetuid() {
        return false;
    }

    @Override
    public boolean isSocket() {
        return false;
    }

    @Override
    public boolean isSticky() {
        return false;
    }

    @Override
    public boolean isSymlink() {
        return false;
    }

    @Override
    public int major(long l) {
        return -1;
    }

    @Override
    public int minor(long l) {
        return -1;
    }

    @Override
    public int mode() {
        return -1;
    }

    @Override
    public int nlink() {
        return -1;
    }

    @Override
    public long rdev() {
        return -1;
    }

    @Override
    public long st_size() {
        return resource.length();
    }

    @Override
    public int uid() {
        return 0;
    }

    @Override
    public long ctime() {
        FileTime time = null;
        try {
            time = resource.creationTime();
        }
        catch (IOException ex) {
            Helpers.throwException(ex);
        }
        return time == null ? 0L : time.toMillis();
    }

    @Override
    public long atime() {
        FileTime time = null;
        try {
            time = resource.lastAccessTime();
        }
        catch (IOException ex) {
            Helpers.throwException(ex);
        }
        return time == null ? 0L : time.toMillis();
    }

    @Override
    public long mtime() {
        FileTime time = null;
        try {
            time = resource.lastModifiedTime();
        }
        catch (IOException ex) {
            Helpers.throwException(ex);
        }
        return time == null ? 0L : time.toMillis();
    }

}
