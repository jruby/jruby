package org.jruby.ext.posix;

/*
  unsigned int st_dev;
  unsigned short st_ino;
  unsigned short st_mode;
  short st_nlink;
  short st_uid;
  short st_gid;
  unsigned int st_rdev;
  long st_size;
  time_t st_atime;
  time_t st_mtime;
  time_t st_ctime;
*/

public class WindowsFileStat extends BaseNativeFileStat {
    public int st_dev;
    public short st_ino;
    public short st_mode;
    public short st_nlink;
    public short st_uid;
    public short st_gid;
    public int st_rdev;
    public int st_size;
    public int st_atime;
    public int st_mtime;
    public int st_ctime;

    public int nothing6;
    public int nothing7;

    public WindowsFileStat(POSIX posix) {
        super(posix);
    }

    public long atime() {
        return st_atime;
    }

    public long blockSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    public long blocks() {
        // TODO Auto-generated method stub
        return 0;
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

    // FIXME: Implement 
    public boolean groupMember(int gid) {
        return true;
    }
    
    @Override
    public boolean isExecutable() {
        if (isOwned()) return (mode() & S_IXUSR) != 0;
        if (isGroupOwned()) return (mode() & S_IXGRP) != 0;
        if ((mode() & S_IXOTH) != 0) return false;

        return true;
    }

    @Override
    public boolean isExecutableReal() {
        if (isROwned()) return (mode() & S_IXUSR) != 0;
        if (groupMember(gid())) return (mode() & S_IXGRP) != 0;
        if ((mode() & S_IXOTH) != 0) return false;

        return true;
    }

    // FIXME: Implement
    public boolean isOwned() {
        return true;
    }
    
    // FIXME: Implement
    public boolean isROwned() {
        return true;
    }
    public boolean isReadable() {
        if (isOwned()) return (mode() & S_IRUSR) != 0;
        if (isGroupOwned()) return (mode() & S_IRGRP) != 0;
        if ((mode() & S_IROTH) != 0) return false;

        return true;
    }
    
    public boolean isReadableReal() {
        if (isROwned()) return (mode() & S_IRUSR) != 0;
        if (groupMember(gid())) return (mode() & S_IRGRP) != 0;
        if ((mode() & S_IROTH) != 0) return false;

        return true;
    }

    public boolean isWritable() {
        if (isOwned()) return (mode() & S_IWUSR) != 0;
        if (isGroupOwned()) return (mode() & S_IWGRP) != 0;
        if ((mode() & S_IWOTH) != 0) return false;

        return true;
    }

    public boolean isWritableReal() {
        if (isROwned()) return (mode() & S_IWUSR) != 0;
        if (groupMember(gid())) return (mode() & S_IWGRP) != 0;
        if ((mode() & S_IWOTH) != 0) return false;

        return true;
    }

    public String toString() {
	return "st_dev: " + st_dev +
	    ", st_mode: " + Integer.toOctalString(st_mode) +
	    ", st_nlink: " + st_nlink +
	    ", st_rdev: " + st_rdev +
	    ", st_size: " + st_size +
	    ", st_uid: " + st_uid +
	    ", st_gid: " + st_gid +
	    ", st_atime: " + st_atime +
	    ", st_ctime: " + st_ctime +
	    ", st_mtime: " + st_mtime +
	    ", st_ino: " + st_ino;
    }
}
