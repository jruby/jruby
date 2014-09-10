package org.jruby.util;

import jnr.posix.FileStat;

// TODO needs a better name like ReadOnlyFileStat
class JarFileStat implements FileStat {
    private final FileResource resource;

    public JarFileStat(FileResource resource) {
        this.resource = resource;
    }

    @Override
    public long atime() {
        return resource.lastModified();
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
    public long ctime() {
        return resource.lastModified();
    }

    @Override
    public long dev() {
        return -1;
    }

    @Override
    public String ftype() {
        return "zip file entry";
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
        return fs instanceof JarFileStat && ((JarFileStat)fs).resource.equals(resource);
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
            return resource.canRead();
        }

        @Override
        public boolean isWritable() {
            return false;
        }

        @Override
        public boolean isWritableReal() {
            return false;
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
        public long mtime() {
            return resource.lastModified();
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


}
