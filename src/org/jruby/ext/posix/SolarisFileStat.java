package org.jruby.ext.posix;

import com.sun.jna.Structure;

public class SolarisFileStat extends BaseNativeFileStat {
    public static class TimeStruct extends Structure {
        public volatile int tv_sec;
        public volatile int tv_nsec;
    }
    public volatile int st_dev;
    public volatile int[] st_pad1;
    public volatile int st_ino;
    public volatile int st_mode;
    public volatile int st_nlink;
    public volatile int st_uid;
    public volatile int st_gid;
    public volatile int st_rdev;
    public volatile int[] st_pad2;
    public volatile int st_size;
    public volatile int st_pad3;
    public volatile TimeStruct st_atim;
    public volatile TimeStruct st_mtim;
    public volatile TimeStruct st_ctim;
    public volatile int st_blksize;
    public volatile int st_blocks;
    public volatile int pad7;
    public volatile int pad8;
    public volatile String st_fstype;
    public volatile int[] st_pad4;

    public SolarisFileStat(POSIX posix) {
        super(posix);
        
        st_pad1 = new int[3];
        st_pad2 = new int[2];
        st_pad4 = new int[8];
    }

    public long atime() {
        return st_atim.tv_sec;
    }

    public long blocks() {
        return st_blocks;
    }

    public long blockSize() {
        return st_blksize;
    }

    public long ctime() {
        return st_ctim.tv_sec;
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
        return st_mode;
    }

    public long mtime() {
        return st_mtim.tv_sec;
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
}
