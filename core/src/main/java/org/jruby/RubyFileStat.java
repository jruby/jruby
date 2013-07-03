/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Joey Gibson <joey@joeygibson.com>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import jnr.posix.FileStat;
import jnr.posix.util.Platform;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JRubyFile;
import org.jruby.util.JRubyNonExistentFile;

/**
 * Implements File::Stat
 */
@JRubyClass(name="File::Stat", include="Comparable")
public class RubyFileStat extends RubyObject {
    private static final long serialVersionUID = 1L;

    private static final int S_IRUGO = (FileStat.S_IRUSR | FileStat.S_IRGRP | FileStat.S_IROTH);
    private static final int S_IWUGO = (FileStat.S_IWUSR | FileStat.S_IWGRP | FileStat.S_IWOTH);
    private static final int S_IXUGO = (FileStat.S_IXUSR | FileStat.S_IXGRP | FileStat.S_IXOTH);

    private JRubyFile file;
    private FileStat stat;

    private static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyFileStat(runtime, klass);
        }
    };

    public static RubyClass createFileStatClass(Ruby runtime) {
        // TODO: NOT_ALLOCATABLE_ALLOCATOR is probably ok here. Confirm. JRUBY-415
        final RubyClass fileStatClass = runtime.getFile().defineClassUnder("Stat",runtime.getObject(), ALLOCATOR);
        runtime.setFileStat(fileStatClass);

        fileStatClass.includeModule(runtime.getModule("Comparable"));
        fileStatClass.defineAnnotatedMethods(RubyFileStat.class);

        return fileStatClass;
    }

    protected RubyFileStat(Ruby runtime, RubyClass clazz) {
        super(runtime, clazz);

    }
    
    public static RubyFileStat newFileStat(Ruby runtime, String filename, boolean lstat) {
        RubyFileStat stat = new RubyFileStat(runtime, runtime.getFileStat());

        stat.setup(filename, lstat);
        
        return stat;
    }

    public static RubyFileStat newFileStat(Ruby runtime, FileDescriptor descriptor) {
        RubyFileStat stat = new RubyFileStat(runtime, runtime.getFileStat());
        
        stat.setup(descriptor);
        
        return stat;
    }

    private void setup(FileDescriptor descriptor) {
        stat = getRuntime().getPosix().fstat(descriptor);
    }
    
    private void setup(String filename, boolean lstat) {
        if (Platform.IS_WINDOWS && filename.length() == 2
                && filename.charAt(1) == ':' && Character.isLetter(filename.charAt(0))) {
            filename += "/";
        }

        if (filename.startsWith("file:") ) {
            if (filename.indexOf('!') != -1) {
                // file: URL handling
                String zipFileEntry = filename.substring(filename.indexOf("!") + 1);
                if (zipFileEntry.length() > 0) {
                    if (zipFileEntry.charAt(0) == '/') {
                        if (zipFileEntry.length() > 1) {
                            zipFileEntry = zipFileEntry.substring(1);
                        } else {
                            throw getRuntime().newErrnoENOENTError("invalid jar/file URL: " + filename);
                        }
                    }
                } else {
                    throw getRuntime().newErrnoENOENTError("invalid jar/file URL: " + filename);
                }
                String zipfilename = filename.substring(5, filename.indexOf("!"));

                try {
                    ZipFile zipFile = new ZipFile(zipfilename);
                    ZipEntry zipEntry = RubyFile.getFileEntry(zipFile, zipFileEntry);

                    if (zipEntry == null) {
                        throw getRuntime().newErrnoENOENTError("invalid jar/file URL: " + filename);
                    }
                    stat = new ZipFileStat(zipEntry);
                    return;
                } catch (IOException ioe) {
                    // fall through and use the zip file as the file to stat
                }

                filename = zipfilename;
            } else {
                throw getRuntime().newErrnoENOENTError("invalid jar/file URL: " + filename);
            }
        }

        file = JRubyFile.create(getRuntime().getCurrentDirectory(), filename);

        if (file instanceof JRubyNonExistentFile) {
            throw getRuntime().newErrnoENOENTError("No such file or directory - " + filename);
        }

        if (lstat) {
            stat = getRuntime().getPosix().lstat(file.getAbsolutePath());
        } else {
            stat = getRuntime().getPosix().stat(file.getAbsolutePath());
        }
    }

    public static class ZipFileStat implements FileStat {
        private final ZipEntry zipEntry;

        public ZipFileStat(ZipEntry zipEntry) {
            this.zipEntry = zipEntry;
        }
        
        public long atime() {
            return zipEntry.getTime();
        }

        public long blocks() {
            return zipEntry.getSize();
        }

        public long blockSize() {
            return 1L;
        }

        public long ctime() {
            return zipEntry.getTime();
        }

        public long dev() {
            return -1;
        }

        public String ftype() {
            return "zip file entry";
        }

        public int gid() {
            return -1;
        }

        public boolean groupMember(int i) {
            return false;
        }

        public long ino() {
            return -1;
        }

        public boolean isBlockDev() {
            return false;
        }

        public boolean isCharDev() {
            return false;
        }

        public boolean isDirectory() {
            return zipEntry.isDirectory();
        }

        public boolean isEmpty() {
            return zipEntry.getSize() == 0;
        }

        public boolean isExecutable() {
            return false;
        }

        public boolean isExecutableReal() {
            return false;
        }

        public boolean isFifo() {
            return false;
        }

        public boolean isFile() {
            return !zipEntry.isDirectory();
        }

        public boolean isGroupOwned() {
            return false;
        }

        public boolean isIdentical(FileStat fs) {
            return fs instanceof ZipFileStat && ((ZipFileStat)fs).zipEntry.equals(zipEntry);
        }

        public boolean isNamedPipe() {
            return false;
        }

        public boolean isOwned() {
            return false;
        }

        public boolean isROwned() {
            return false;
        }

        public boolean isReadable() {
            return true;
        }

        public boolean isReadableReal() {
            return true;
        }

        public boolean isWritable() {
            return false;
        }

        public boolean isWritableReal() {
            return false;
        }

        public boolean isSetgid() {
            return false;
        }

        public boolean isSetuid() {
            return false;
        }

        public boolean isSocket() {
            return false;
        }

        public boolean isSticky() {
            return false;
        }

        public boolean isSymlink() {
            return false;
        }

        public int major(long l) {
            return -1;
        }

        public int minor(long l) {
            return -1;
        }

        public int mode() {
            return -1;
        }

        public long mtime() {
            return zipEntry.getTime();
        }

        public int nlink() {
            return -1;
        }

        public long rdev() {
            return -1;
        }

        public long st_size() {
            return zipEntry.getSize();
        }

        public int uid() {
            return 0;
        }

    }

    @JRubyMethod(name = "initialize", required = 1, visibility = Visibility.PRIVATE, compat = CompatVersion.RUBY1_8)
    public IRubyObject initialize(IRubyObject fname, Block unusedBlock) {
        setup(fname.convertToString().toString(), false);

        return this;
    }

    @JRubyMethod(name = "initialize", required = 1, visibility = Visibility.PRIVATE, compat = CompatVersion.RUBY1_9)
    public IRubyObject initialize19(IRubyObject fname, Block unusedBlock) {
        return initialize(RubyFile.get_path(getRuntime().getCurrentContext(), fname), unusedBlock);
    }
    
    @JRubyMethod(name = "atime")
    public IRubyObject atime() {
        return getRuntime().newTime(stat.atime() * 1000);
    }
    
    @JRubyMethod(name = "blksize")
    public RubyFixnum blksize() {
        return getRuntime().newFixnum(stat.blockSize());
    }

    @JRubyMethod(name = "blockdev?")
    public IRubyObject blockdev_p() {
        return getRuntime().newBoolean(stat.isBlockDev());
    }

    @JRubyMethod(name = "blocks")
    public IRubyObject blocks() {
        return getRuntime().newFixnum(stat.blocks());
    }

    @JRubyMethod(name = "chardev?")
    public IRubyObject chardev_p() {
        return getRuntime().newBoolean(stat.isCharDev());
    }

    @JRubyMethod(name = "<=>", required = 1)
    public IRubyObject cmp(IRubyObject other) {
        if (!(other instanceof RubyFileStat)) return getRuntime().getNil();
        
        long time1 = stat.mtime();
        long time2 = ((RubyFileStat) other).stat.mtime();
        
        if (time1 == time2) {
            return getRuntime().newFixnum(0);
        } else if (time1 < time2) {
            return getRuntime().newFixnum(-1);
        } 

        return getRuntime().newFixnum(1);
    }

    @JRubyMethod(name = "ctime")
    public IRubyObject ctime() {
        return getRuntime().newTime(stat.ctime() * 1000);
    }

    @JRubyMethod(name = "dev")
    public IRubyObject dev() {
        return getRuntime().newFixnum(stat.dev());
    }
    
    @JRubyMethod(name = "dev_major")
    public IRubyObject devMajor() {
        return getRuntime().newFixnum(stat.major(stat.dev()));
    }

    @JRubyMethod(name = "dev_minor")
    public IRubyObject devMinor() {
        return getRuntime().newFixnum(stat.minor(stat.dev()));
    }

    @JRubyMethod(name = "directory?")
    public RubyBoolean directory_p() {
        return getRuntime().newBoolean(stat.isDirectory());
    }

    @JRubyMethod(name = "executable?")
    public IRubyObject executable_p() {
        return getRuntime().newBoolean(stat.isExecutable());
    }

    @JRubyMethod(name = "executable_real?")
    public IRubyObject executableReal_p() {
        return getRuntime().newBoolean(stat.isExecutableReal());
    }

    @JRubyMethod(name = "file?")
    public RubyBoolean file_p() {
        return getRuntime().newBoolean(stat.isFile());
    }

    @JRubyMethod(name = "ftype")
    public RubyString ftype() {
        return getRuntime().newString(stat.ftype());
    }

    @JRubyMethod(name = "gid")
    public IRubyObject gid() {
        return getRuntime().newFixnum(stat.gid());
    }
    
    @JRubyMethod(name = "grpowned?")
    public IRubyObject group_owned_p() {
        return getRuntime().newBoolean(stat.isGroupOwned());
    }
    
    @JRubyMethod(name = "initialize_copy", required = 1)
    public IRubyObject initialize_copy(IRubyObject original) {
        if (!(original instanceof RubyFileStat)) {
            throw getRuntime().newTypeError("wrong argument class");
        }
        
        RubyFileStat originalFileStat = (RubyFileStat) original;
        
        file = originalFileStat.file;
        stat = originalFileStat.stat;
        
        return this;
    }
    
    @JRubyMethod(name = "ino")
    public IRubyObject ino() {
        return getRuntime().newFixnum(stat.ino());
    }

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect() {
        StringBuilder buf = new StringBuilder("#<");
        buf.append(getMetaClass().getRealClass().getName());
        buf.append(" ");
        // FIXME: Obvious issue that not all platforms can display all attributes.  Ugly hacks.
        // Using generic posix library makes pushing inspect behavior into specific system impls
        // rather painful.
        try { buf.append("dev=0x").append(Long.toHexString(stat.dev())); } catch (Exception e) {} finally { buf.append(", "); }
        try { buf.append("ino=").append(stat.ino()); } catch (Exception e) {} finally { buf.append(", "); }
        buf.append("mode=0").append(Integer.toOctalString(stat.mode())).append(", "); 
        try { buf.append("nlink=").append(stat.nlink()); } catch (Exception e) {} finally { buf.append(", "); }
        try { buf.append("uid=").append(stat.uid()); } catch (Exception e) {} finally { buf.append(", "); }
        try { buf.append("gid=").append(stat.gid()); } catch (Exception e) {} finally { buf.append(", "); }
        try { buf.append("rdev=0x").append(Long.toHexString(stat.rdev())); } catch (Exception e) {} finally { buf.append(", "); }
        buf.append("size=").append(sizeInternal()).append(", ");
        try { buf.append("blksize=").append(stat.blockSize()); } catch (Exception e) {} finally { buf.append(", "); }
        try { buf.append("blocks=").append(stat.blocks()); } catch (Exception e) {} finally { buf.append(", "); }
        
        buf.append("atime=").append(atime()).append(", ");
        buf.append("mtime=").append(mtime()).append(", ");
        buf.append("ctime=").append(ctime());
        buf.append(">");
        
        return getRuntime().newString(buf.toString());
    }

    @JRubyMethod(name = "uid")
    public IRubyObject uid() {
        return getRuntime().newFixnum(stat.uid());
    }
    
    @JRubyMethod(name = "mode")
    public IRubyObject mode() {
        return getRuntime().newFixnum(stat.mode());
    }

    @JRubyMethod(name = "mtime")
    public IRubyObject mtime() {
        return getRuntime().newTime(stat.mtime() * 1000);
    }
    
    public IRubyObject mtimeEquals(IRubyObject other) {
        return getRuntime().newBoolean(stat.mtime() == newFileStat(getRuntime(), other.convertToString().toString(), false).stat.mtime()); 
    }

    public IRubyObject mtimeGreaterThan(IRubyObject other) {
        return getRuntime().newBoolean(stat.mtime() > newFileStat(getRuntime(), other.convertToString().toString(), false).stat.mtime()); 
    }

    public IRubyObject mtimeLessThan(IRubyObject other) {
        return getRuntime().newBoolean(stat.mtime() < newFileStat(getRuntime(), other.convertToString().toString(), false).stat.mtime()); 
    }

    @JRubyMethod(name = "nlink")
    public IRubyObject nlink() {
        return getRuntime().newFixnum(stat.nlink());
    }

    @JRubyMethod(name = "owned?")
    public IRubyObject owned_p() {
        return getRuntime().newBoolean(stat.isOwned());
    }

    @JRubyMethod(name = "pipe?")
    public IRubyObject pipe_p() {
        return getRuntime().newBoolean(stat.isNamedPipe());
    }

    @JRubyMethod(name = "rdev")
    public IRubyObject rdev() {
        return getRuntime().newFixnum(stat.rdev());
    }
    
    @JRubyMethod(name = "rdev_major")
    public IRubyObject rdevMajor() {
        return getRuntime().newFixnum(stat.major(stat.rdev()));
    }

    @JRubyMethod(name = "rdev_minor")
    public IRubyObject rdevMinor() {
        return getRuntime().newFixnum(stat.minor(stat.rdev()));
    }

    @JRubyMethod(name = "readable?")
    public IRubyObject readable_p() {
        return getRuntime().newBoolean(stat.isReadable());
    }

    @JRubyMethod(name = "readable_real?")
    public IRubyObject readableReal_p() {
        return getRuntime().newBoolean(stat.isReadableReal());
    }

    @JRubyMethod(name = "setgid?")
    public IRubyObject setgid_p() {
        return getRuntime().newBoolean(stat.isSetgid());
    }

    @JRubyMethod(name = "setuid?")
    public IRubyObject setuid_p() {
        return getRuntime().newBoolean(stat.isSetuid());
    }

    private long sizeInternal() {
        // Workaround for JRUBY-4149
        if (Platform.IS_WINDOWS && file != null) {
            try {
                return file.length();
            } catch (SecurityException ex) {
                return 0L;
            }
        } else {
            return stat.st_size();
        }
    }

    @JRubyMethod(name = "size")
    public IRubyObject size() {
        return getRuntime().newFixnum(sizeInternal());
    }
    
    @JRubyMethod(name = "size?")
    public IRubyObject size_p() {
        long size = sizeInternal();
        
        if (size == 0) return getRuntime().getNil();
        
        return getRuntime().newFixnum(size);
    }

    @JRubyMethod(name = "socket?")
    public IRubyObject socket_p() {
        return getRuntime().newBoolean(stat.isSocket());
    }
    
    @JRubyMethod(name = "sticky?")
    public IRubyObject sticky_p() {
        Ruby runtime = getRuntime();
        
        if (runtime.getPosix().isNative()) {
            return runtime.newBoolean(stat.isSticky());
        }
        
        return runtime.getNil();
    }

    @JRubyMethod(name = "symlink?")
    public IRubyObject symlink_p() {
        return getRuntime().newBoolean(stat.isSymlink());
    }

    @JRubyMethod(name = "writable?")
    public IRubyObject writable_p() {
    	return getRuntime().newBoolean(stat.isWritable());
    }
    
    @JRubyMethod(name = "writable_real?")
    public IRubyObject writableReal_p() {
        return getRuntime().newBoolean(stat.isWritableReal());
    }
    
    @JRubyMethod(name = "zero?")
    public IRubyObject zero_p() {
        return getRuntime().newBoolean(stat.isEmpty());
    }

    @JRubyMethod(name = "world_readable?", compat = CompatVersion.RUBY1_9)
    public IRubyObject worldReadable(ThreadContext context) {
        return getWorldMode(context, FileStat.S_IROTH);
    }

    @JRubyMethod(name = "world_writable?", compat = CompatVersion.RUBY1_9)
    public IRubyObject worldWritable(ThreadContext context) {
        return getWorldMode(context, FileStat.S_IWOTH);
    }

    private IRubyObject getWorldMode(ThreadContext context, int mode) {
        if ((stat.mode() & mode) == mode) {
            return RubyNumeric.int2fix(context.runtime,
                    (stat.mode() & (S_IRUGO | S_IWUGO | S_IXUGO) ));
        }
        return context.runtime.getNil();
    }
}
