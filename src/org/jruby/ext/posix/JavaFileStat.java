package org.jruby.ext.posix;

import java.io.File;
import java.io.IOException;

public class JavaFileStat implements FileStat {
    private POSIXHandler handler;
    short st_mode;
    int st_blksize;
    long st_size;
    int st_ctime;
    int st_mtime;
    POSIX posix;
    
    public JavaFileStat(POSIX posix, POSIXHandler handler) {
        this.handler = handler;
        this.posix = posix;
    }
    
    public void setup(String path) {
        // ENEBO: This was originally JRubyFile, but I believe we use JRuby file for normalization
        // of paths.  So we should be safe.
        File file = new File(path);
        // Limitation: We cannot determine, so always return 4096 (better than blowing up)
        st_blksize = 4096;
        st_mode = calculateMode(file, st_mode);
        st_size = file.length();

        // Parent file last modified will only represent when something was added or removed.
        // This is not correct, but it is better than nothing and does work in one common use
        // case.
        st_ctime = (int) (file.getParentFile().lastModified() / 1000);
        st_mtime = (int) (file.lastModified() / 1000);
    }

    private short calculateMode(File file, short st_mode) {
        // implementation to lowest common denominator...
        // Windows has no file mode, but C ruby returns either 0100444 or 0100666

        if (file.canRead()) st_mode |= ALL_READ;
        if (file.canWrite()) st_mode |= ALL_WRITE;

        if (file.isDirectory()) {
            st_mode |= S_IFDIR;
        } else if (file.isFile()) {
            st_mode |= S_IFREG;
        }
        
        try {
            st_mode = calculateSymlink(file, st_mode);
        } catch (IOException e) {
            // Not sure we can do much in this case...
        }
        
        return st_mode;
    }
    
    private short calculateSymlink(File file, short st_mode) throws IOException {
        File absoluteParent = file.getAbsoluteFile().getParentFile();
        File canonicalParent = absoluteParent.getCanonicalFile();
        
        if (canonicalParent.getAbsolutePath().equals(absoluteParent.getAbsolutePath())) {
            // parent doesn't change when canonicalized, compare absolute and canonical file directly
            if (!file.getAbsolutePath().equals(file.getCanonicalPath())) {
                st_mode |= S_IFLNK;
                return st_mode;
            }
        }
        
        // FIXME: See if we can get rid of JRubyFile use here.
        // directory itself has symlinks (canonical != absolute), so build new path with canonical parent and compare
        file = new File(canonicalParent.getAbsolutePath() + "/" + file.getName());
        if (!file.getAbsolutePath().equals(file.getCanonicalPath())) {
            st_mode |= S_IFLNK;
        }
        
        return st_mode;
    }

    /**
     * Limitation: Java has no access time support, so we return mtime as the next best thing.
     */
    public long atime() {
        return st_mtime;
    }

    public long blocks() {
        handler.unimplementedError("stat.st_blocks");
        
        return -1;
    }

    public long blockSize() {
        return st_blksize;
    }

    public long ctime() {
        return st_ctime;
    }
    
    public long dev() {
        handler.unimplementedError("stat.st_dev");
        
        return -1;
    }

    public String ftype() {
        if (isFile()) {
            return "file";
        } else if (isDirectory()) {
            return "directory";
        }
        
        return "unknown";
    }

    public int gid() {
        handler.unimplementedError("stat.st_gid");
        
        return -1;
    }
    
    public boolean groupMember(int gid) {
        return posix.getgid() == gid || posix.getegid() == gid;
    }

    /**
     *  Limitation: We have no pure-java way of getting inode.  webrick needs this defined to work.
     */
    public int ino() {
        return 0;
    }

    public boolean isBlockDev() {
        handler.unimplementedError("block device detection");
        
        return false;
    }
    
    /**
     * Limitation: [see JRUBY-1516] We just pick more likely value.  This is a little scary.
     */
    public boolean isCharDev() {
        return false;
    }
    
    public boolean isDirectory() {
        return (mode() & S_IFDIR) != 0;
    }

    public boolean isEmpty() {
        return st_size() == 0;
    }

    // At least one major library depends on this method existing.
    public boolean isExecutable() {
        handler.warn("executable? does not in this environment and will return a dummy value");
        
        return true;
    }

    // At least one major library depends on this method existing.
    public boolean isExecutableReal() {
        handler.warn("executable_real? does not work in this environmnt and will return a dummy value");
        
        return true;
    }

    public boolean isFifo() {
        handler.unimplementedError("fifo file detection");
        
        return false;
    }
    
    public boolean isFile() {
        return (mode() & S_IFREG) != 0;
    }
    
    public boolean isGroupOwned() {
        return groupMember(gid());
    }

    public boolean isIdentical(FileStat other) {
        handler.unimplementedError("identical file detection");
        
        return false;
    }

    public boolean isNamedPipe() {
        handler.unimplementedError("piped file detection");
        
        return false;
    }

    public boolean isOwned() {
        return posix.geteuid() == uid();
    }

    public boolean isROwned() {
        return posix.getuid() == uid();
    }

    public boolean isReadable() {
        int mode = mode();
        
        if ((mode & S_IRUSR) != 0) return true;
        if ((mode & S_IRGRP) != 0) return true;
        if ((mode & S_IROTH) != 0) return true;
        
        return false;
    }

    // We do both readable and readable_real through the same method because
    public boolean isReadableReal() {
        return isReadable();
    }

    public boolean isSymlink() {
        return (mode() & S_IFLNK) == S_IFLNK;
    }
    
    public boolean isWritable() {
        int mode = mode();
        
        if ((mode & S_IWUSR) != 0) return true;
        if ((mode & S_IWGRP) != 0) return true;
        if ((mode & S_IWOTH) != 0) return true;

        return false;
    }
    
    // We do both readable and readable_real through the same method because
    // in our java process effective and real userid will always be the same.
    public boolean isWritableReal() {
        return isWritable();
    }

    public boolean isSetgid() {
        handler.unimplementedError("setgid detection");
        
        return false;
    }

    public boolean isSetuid() {
        handler.unimplementedError("setuid detection");
        
        return false;
    }

    public boolean isSocket() {
        handler.unimplementedError("socket file type detection");
        
        return false;
    }
    
    public boolean isSticky() {
        handler.unimplementedError("sticky bit detection");
        
        return false;
    }

    public int major(long dev) {
        handler.unimplementedError("major device");
        
        return -1;
    }

    public int minor(long dev) {
        handler.unimplementedError("minor device");
        
        return -1;
    }

    public int mode() {
        return st_mode & 0xffff;
    }

    public long mtime() {
        return st_mtime;
    }

    public int nlink() {
        handler.unimplementedError("stat.nlink");
        
        return -1;
    }

    public long rdev() {
        handler.unimplementedError("stat.rdev");
        
        return -1;
    }
    
    public long st_size() {
        return st_size;
    }

    // Limitation: We have no pure-java way of getting uid. RubyZip needs this defined to work.
    public int uid() {
        return -1;
    }
}
