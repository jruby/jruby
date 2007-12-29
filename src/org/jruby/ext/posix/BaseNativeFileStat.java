package org.jruby.ext.posix;

import com.sun.jna.Structure;

public abstract class BaseNativeFileStat extends Structure implements FileStat {
    protected POSIX posix;
    
    public BaseNativeFileStat(POSIX posix) {
        super();
        this.posix = posix;
    }
    
    public String ftype() {
        if (isFile()) {
            return "file";
        } else if (isDirectory()) {
            return "directory";
        } else if (isCharDev()) {
            return "characterSpecial";
        } else if (isBlockDev()) {
            return "blockSpecial";
        } else if (isFifo()) {
            return "fifo";
        } else if (isSymlink()) {
            return "link";
        } else if (isSocket()) {
            return "socket";
        } 
          
        return "unknown";
    }

    public boolean groupMember(int gid) {
        if (posix.getgid() == gid || posix.getegid() == gid) {
            return true;
        }

        // FIXME: Though not Posix, windows has different mechanism for this.
        
        return false;
    }
    
    public boolean isBlockDev() {
        return (mode() & S_IFMT) == S_IFBLK;
    }
    
    public boolean isCharDev() {
        return (mode() & S_IFMT) == S_IFCHR;
    }

    public boolean isDirectory() {
        return (mode() & S_IFMT) == S_IFDIR;
    }
    
    public boolean isEmpty() {
        return st_size() == 0;
    }

    public boolean isExecutable() {
        if (posix.geteuid() == 0) return (mode() & S_IXUGO) != 0;
        if (isOwned()) return (mode() & S_IXUSR) != 0;
        if (isGroupOwned()) return (mode() & S_IXGRP) != 0;
        return (mode() & S_IXOTH) != 0;
    }
    
    public boolean isExecutableReal() {
        if (posix.getuid() == 0) return (mode() & S_IXUGO) != 0;
        if (isROwned()) return (mode() & S_IXUSR) != 0;
        if (groupMember(gid())) return (mode() & S_IXGRP) != 0;
        return (mode() & S_IXOTH) != 0;
    }
    
    public boolean isFile() {
        return (mode() & S_IFMT) == S_IFREG;
    }

    public boolean isFifo() {
        return (mode() & S_IFMT) == S_IFIFO;
    }
    
    public boolean isGroupOwned() {
        return groupMember(gid());
    }

    public boolean isIdentical(FileStat other) {
        return dev() == other.dev() && ino() == other.ino(); 
    }

    public boolean isNamedPipe() {
        return (mode() & S_IFIFO) != 0;
    }
    
    public boolean isOwned() {
        return posix.geteuid() == uid();
    }
    
    public boolean isROwned() {
        return posix.getuid() == uid();
    }
    
    public boolean isReadable() {
        if (posix.geteuid() == 0) return true;
        if (isOwned()) return (mode() & S_IRUSR) != 0;
        if (isGroupOwned()) return (mode() & S_IRGRP) != 0;
        return (mode() & S_IROTH) != 0;
    }
    
    public boolean isReadableReal() {
        if (posix.getuid() == 0) return true;
        if (isROwned()) return (mode() & S_IRUSR) != 0;
        if (groupMember(gid())) return (mode() & S_IRGRP) != 0;
        return (mode() & S_IROTH) != 0;
    }
    
    public boolean isSetgid() {
        return (mode() & S_ISGID) != 0;
    }

    public boolean isSetuid() {
        return (mode() & S_ISUID) != 0;
    }

    public boolean isSocket() {
        return (mode() & S_IFMT) == S_IFSOCK;
    }
    
    public boolean isSticky() {
        return (mode() & S_ISVTX) != 0;
    }

    public boolean isSymlink() {
        return (mode() & S_IFMT) == S_IFLNK;
    }

    public boolean isWritable() {
        if (posix.geteuid() == 0) return true;
        if (isOwned()) return (mode() & S_IWUSR) != 0;
        if (isGroupOwned()) return (mode() & S_IWGRP) != 0;
        return (mode() & S_IWOTH) != 0;
    }

    public boolean isWritableReal() {
        if (posix.getuid() == 0) return true;
        if (isROwned()) return (mode() & S_IWUSR) != 0;
        if (groupMember(gid())) return (mode() & S_IWGRP) != 0;
        return (mode() & S_IWOTH) != 0;
    }

    public int major(long dev) {
        return (int) (dev >> 24) & 0xff;
    }
    
    public int minor(long dev) {
        return (int) (dev & 0xffffff);
    }
}
