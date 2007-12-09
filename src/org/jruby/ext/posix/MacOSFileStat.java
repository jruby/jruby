package org.jruby.ext.posix;

public class MacOSFileStat extends BaseNativeFileStat {
    public int st_dev;       // device inode resides on (dev_t)
    public int st_ino;       // inode's number (ino_t)
    public short st_mode;    // inode protection mode (mode_t - uint16)
    public short st_nlink;   // number or hard links to the file (nlink_y - uint16)
    public int st_uid;       // user-id of owner (uid_t)
    public int st_gid;       // group-id of owner (gid_t)
    public int st_rdev;      // device type, for special file inode (st_rdev - dev_t)
    public int st_atime;     // Time of last access (time_t)
    public int st_atimensec; // Time of last access (nanoseconds)
    public int st_mtime;     // Last data modification time (time_t)
    public int st_mtimensec; // Last data modification time (nanoseconds)
    public int st_ctime;     // Time of last status change (time_t)
    public int st_ctimensec; // Time of last status change (nanoseconds)
    public long st_size;     // file size, in bytes
    public long st_blocks;   // blocks allocated for file
    public int st_blksize;   // optimal file system I/O ops blocksize
    public int st_flags;     // user defined flags for file
    public int st_gen;       // file generation number
    public int st_lspare;    // RESERVED: DO NOT USE!
    public long[] st_qspare; // RESERVED: DO NOT USE!

    public MacOSFileStat(POSIX posix) {
        super(posix);
        this.st_qspare = new long[2];
    }
    
    public long atime() {
        return st_atime;
    }

    public long blocks() {
        return st_blocks;
    }

    public long blockSize() {
        return st_blksize;
    }
    
    public long ctime() {
        return st_ctime;
    }

    public long dev() {
        return st_dev;
    }
    
    public int gid() {
        return st_gid;
    }
    
    public int ino() {
        return st_ino;
    }
    
    public int mode() {
        return st_mode & 0xffff;
    }
    
    public long mtime() {
        return st_mtime;
    }

    public int nlink() {
        return st_nlink;
    }
    
    public long rdev() {
        return st_rdev;
    }
    
    public long st_size() {
        return st_size;
    }
    
    public int uid() {
        return st_uid;
    }
    
    public String toString() {
        return "Stat {DEV: " + st_dev + ", INO: " + st_ino + ", MODE: " + st_mode +
            ", NLINK: " + st_nlink + ", UID: " + st_uid + ", GID: " + st_gid +
            ", RDEV: " + st_rdev + ", BLOCKS: " + st_blocks + ", SIZE: " + st_size +
            ", BLKSIZE: " + st_blksize + ", FLAGS: " + st_flags + ", GEN: " + st_gen +
            ", ATIME: " + st_atime + ", MTIME: " + st_mtime + ", CTIME: " + st_ctime;
    }
}
