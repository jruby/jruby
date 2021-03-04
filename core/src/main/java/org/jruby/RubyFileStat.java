/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
import java.nio.file.attribute.FileTime;

import jnr.posix.NanosecondFileStat;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import jnr.posix.FileStat;
import jnr.posix.util.Platform;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.FileResource;
import org.jruby.util.JRubyFile;
import org.jruby.util.StringSupport;

/**
 * Implements File::Stat
 */
@JRubyClass(name="File::Stat", include="Comparable")
public class RubyFileStat extends RubyObject {
    private static final long serialVersionUID = 1L;

    private static final int S_IRUGO = (FileStat.S_IRUSR | FileStat.S_IRGRP | FileStat.S_IROTH);
    private static final int S_IWUGO = (FileStat.S_IWUSR | FileStat.S_IWGRP | FileStat.S_IWOTH);
    private static final int S_IXUGO = (FileStat.S_IXUSR | FileStat.S_IXGRP | FileStat.S_IXOTH);
    public static final int BILLION = 1000000000;

    private FileResource file;
    private FileStat stat;

    private void checkInitialized() {
        if (stat == null) throw getRuntime().newTypeError("uninitialized File::Stat");
    }

    public static RubyClass createFileStatClass(Ruby runtime) {
        // TODO: NOT_ALLOCATABLE_ALLOCATOR is probably ok here. Confirm. JRUBY-415
        final RubyClass fileStatClass = runtime.getFile().defineClassUnder("Stat",runtime.getObject(), RubyFileStat::new);

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

    public static RubyFileStat newFileStat(Ruby runtime, int fileno) {
        RubyFileStat stat = new RubyFileStat(runtime, runtime.getFileStat());

        stat.setup(fileno);

        return stat;
    }

    private void setup(FileDescriptor descriptor) {
        stat = getRuntime().getPosix().fstat(descriptor);
    }

    private void setup(int fileno) {
        stat = getRuntime().getPosix().fstat(fileno);
    }
    
    private void setup(String filename, boolean lstat) {
        Ruby runtime = getRuntime();

        if (Platform.IS_WINDOWS && filename.length() == 2
                && filename.charAt(1) == ':' && Character.isLetter(filename.charAt(0))) {
            filename += '/';
        }

        file = JRubyFile.createResource(runtime, filename);
        stat = lstat ? file.lstat() : file.stat();

        if (stat == null) {
            if (Platform.IS_WINDOWS) {
                switch (file.errno()) {
                    case 2:   // ERROR_FILE_NOT_FOUND
                    case 3:   // ERROR_PATH_NOT_FOUND
                    case 53:  // ERROR_BAD_NETPATH
                    case 123: // ERROR_INVALID_NAME
                        throw runtime.newErrnoENOENTError(filename);
                }
            }

            throw runtime.newErrnoFromInt(file.errno(), filename);
        }
    }

    public IRubyObject initialize(IRubyObject fname, Block unusedBlock) {
        return initialize19(fname, unusedBlock);
    }

    @JRubyMethod(name = "initialize", required = 1, visibility = Visibility.PRIVATE)
    public IRubyObject initialize19(IRubyObject fname, Block unusedBlock) {
        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();
        RubyString path = StringSupport.checkEmbeddedNulls(runtime, RubyFile.get_path(context, fname));
        setup(path.convertToString().toString(), false);

        return this;    
    }
    
    @JRubyMethod(name = "atime")
    public IRubyObject atime() {
        checkInitialized();
        if (stat instanceof NanosecondFileStat) {
            return RubyTime.newTimeFromNanoseconds(getRuntime(), stat.atime() * BILLION + ((NanosecondFileStat) stat).aTimeNanoSecs());
        }
        return getRuntime().newTime(stat.atime() * 1000);
    }
    
    @JRubyMethod(name = "blksize")
    public IRubyObject blockSize(ThreadContext context) {
        checkInitialized();
        if (Platform.IS_WINDOWS) return context.nil;
        return context.runtime.newFixnum(stat.blockSize());
    }

    @Deprecated
    public RubyFixnum blksize() {
        checkInitialized();
        return getRuntime().newFixnum(stat.blockSize());
    }

    @JRubyMethod(name = "blockdev?")
    public IRubyObject blockdev_p() {
        checkInitialized();
        return getRuntime().newBoolean(stat.isBlockDev());
    }

    @JRubyMethod(name = "blocks")
    public IRubyObject blocks() {
        checkInitialized();
        if (Platform.IS_WINDOWS) return getRuntime().getNil();
        return getRuntime().newFixnum(stat.blocks());
    }

    @JRubyMethod(name = "chardev?")
    public IRubyObject chardev_p() {
        checkInitialized();
        return getRuntime().newBoolean(stat.isCharDev());
    }

    @JRubyMethod(name = "<=>", required = 1)
    public IRubyObject cmp(IRubyObject other) {
        checkInitialized();
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
        checkInitialized();
        if (stat instanceof NanosecondFileStat) {
            return RubyTime.newTimeFromNanoseconds(getRuntime(), stat.ctime() * BILLION + ((NanosecondFileStat) stat).cTimeNanoSecs());
        }
        return getRuntime().newTime(stat.ctime() * 1000);
    }

    @JRubyMethod(name = "birthtime")
    public IRubyObject birthtime() {
        checkInitialized();
        FileTime btime = null;

        if (file == null || (btime = RubyFile.getBirthtimeWithNIO(file.absolutePath())) == null) {
            return ctime();
        }
        return getRuntime().newTime(btime.toMillis());
    }

    @JRubyMethod(name = "dev")
    public IRubyObject dev() {
        checkInitialized();
        return getRuntime().newFixnum(stat.dev());
    }
    
    @JRubyMethod(name = "dev_major")
    public IRubyObject devMajor() {
        checkInitialized();
        if (Platform.IS_WINDOWS) return getRuntime().getNil();
        return getRuntime().newFixnum(stat.major(stat.dev()));
    }

    @JRubyMethod(name = "dev_minor")
    public IRubyObject devMinor() {
        checkInitialized();
        if (Platform.IS_WINDOWS) return getRuntime().getNil();
        return getRuntime().newFixnum(stat.minor(stat.dev()));
    }

    @JRubyMethod(name = "directory?")
    public RubyBoolean directory_p() {
        checkInitialized();
        return getRuntime().newBoolean(stat.isDirectory());
    }

    @JRubyMethod(name = "executable?")
    public IRubyObject executable_p() {
        checkInitialized();
        return getRuntime().newBoolean(stat.isExecutable());
    }

    @JRubyMethod(name = "executable_real?")
    public IRubyObject executableReal_p() {
        checkInitialized();
        return getRuntime().newBoolean(stat.isExecutableReal());
    }

    @JRubyMethod(name = "file?")
    public RubyBoolean file_p() {
        checkInitialized();
        return getRuntime().newBoolean(stat.isFile());
    }

    @JRubyMethod(name = "ftype")
    public RubyString ftype() {
        checkInitialized();
        return getRuntime().newString(stat.ftype());
    }

    @JRubyMethod(name = "gid")
    public IRubyObject gid() {
        checkInitialized();
        if (Platform.IS_WINDOWS) return RubyFixnum.zero(getRuntime());
        return getRuntime().newFixnum(stat.gid());
    }
    
    @JRubyMethod(name = "grpowned?")
    public IRubyObject group_owned_p() {
        checkInitialized();
        if (Platform.IS_WINDOWS) return getRuntime().getFalse();
        return getRuntime().newBoolean(stat.isGroupOwned());
    }
    
    @JRubyMethod(name = "initialize_copy", required = 1, visibility = Visibility.PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject original) {
        if (!(original instanceof RubyFileStat)) {
            throw getRuntime().newTypeError("wrong argument class");
        }

        checkFrozen();
        
        RubyFileStat originalFileStat = (RubyFileStat) original;
        
        file = originalFileStat.file;
        stat = originalFileStat.stat;
        
        return this;
    }
    
    @JRubyMethod(name = "ino")
    public IRubyObject ino() {
        checkInitialized();
        return metaClass.runtime.newFixnum(stat.ino());
    }

    @JRubyMethod(name = "inspect")
    @Override
    public IRubyObject inspect() {
        StringBuilder buf = new StringBuilder("#<");
        buf.append(getMetaClass().getRealClass().getName());
        if (stat == null) {
            buf.append(": uninitialized");
        } else {
            ThreadContext context = metaClass.runtime.getCurrentContext();
            buf.append(' ');
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
            try {
                buf.append("blksize=").append(blockSize(context).inspect().toString()); } catch (Exception e) {} finally { buf.append(", "); }
            try { buf.append("blocks=").append(blocks().inspect().toString()); } catch (Exception e) {} finally { buf.append(", "); }

            buf.append("atime=").append(atime()).append(", ");
            buf.append("mtime=").append(mtime()).append(", ");
            buf.append("ctime=").append(ctime());
            if (Platform.IS_BSD || Platform.IS_MAC) {
                buf.append(", ").append("birthtime=").append(birthtime());
            }
        }
        buf.append('>');
        
        return getRuntime().newString(buf.toString());
    }

    @JRubyMethod(name = "uid")
    public IRubyObject uid() {
        checkInitialized();
        if (Platform.IS_WINDOWS) return RubyFixnum.zero(getRuntime());
        return getRuntime().newFixnum(stat.uid());
    }
    
    @JRubyMethod(name = "mode")
    public IRubyObject mode() {
        checkInitialized();
        return getRuntime().newFixnum(stat.mode());
    }

    @JRubyMethod(name = "mtime")
    public IRubyObject mtime() {
        checkInitialized();
        if (stat instanceof NanosecondFileStat) {
            return RubyTime.newTimeFromNanoseconds(getRuntime(), stat.mtime() * BILLION + ((NanosecondFileStat) stat).mTimeNanoSecs());
        }
        return getRuntime().newTime(stat.mtime() * 1000);
    }
    
    public IRubyObject mtimeEquals(IRubyObject other) {
        FileStat otherStat = newFileStat(getRuntime(), other.convertToString().toString(), false).stat;
        boolean equal = stat.mtime() == otherStat.mtime();

        if (stat instanceof NanosecondFileStat && otherStat instanceof NanosecondFileStat) {
            equal = equal && ((NanosecondFileStat) stat).mTimeNanoSecs() == ((NanosecondFileStat) otherStat).mTimeNanoSecs();
        }

        return getRuntime().newBoolean(equal);
    }

    public IRubyObject mtimeGreaterThan(IRubyObject other) {
        FileStat otherStat = newFileStat(getRuntime(), other.convertToString().toString(), false).stat;
        boolean gt;

        if (stat instanceof NanosecondFileStat && otherStat instanceof NanosecondFileStat) {
            gt = (stat.mtime() * BILLION + ((NanosecondFileStat) stat).mTimeNanoSecs())
                    > (otherStat.mtime() * BILLION + ((NanosecondFileStat) otherStat).mTimeNanoSecs());
        } else {
            gt = stat.mtime() > otherStat.mtime();
        }

        return getRuntime().newBoolean(gt);
    }

    public IRubyObject mtimeLessThan(IRubyObject other) {
        FileStat otherStat = newFileStat(getRuntime(), other.convertToString().toString(), false).stat;
        boolean lt;

        if (stat instanceof NanosecondFileStat && otherStat instanceof NanosecondFileStat) {
            lt = (stat.mtime() * BILLION + ((NanosecondFileStat) stat).mTimeNanoSecs())
                    < (otherStat.mtime() * BILLION + ((NanosecondFileStat) otherStat).mTimeNanoSecs());
        } else {
            lt = stat.mtime() < otherStat.mtime();
        }

        return getRuntime().newBoolean(lt);
    }

    @JRubyMethod(name = "nlink")
    public IRubyObject nlink() {
        checkInitialized();
        return getRuntime().newFixnum(stat.nlink());
    }

    @JRubyMethod(name = "owned?")
    public IRubyObject owned_p() {
        checkInitialized();
        return getRuntime().newBoolean(stat.isOwned());
    }

    @JRubyMethod(name = "pipe?")
    public IRubyObject pipe_p() {
        checkInitialized();
        return getRuntime().newBoolean(stat.isNamedPipe());
    }

    @JRubyMethod(name = "rdev")
    public IRubyObject rdev() {
        checkInitialized();
        return getRuntime().newFixnum(stat.rdev());
    }
    
    @JRubyMethod(name = "rdev_major")
    public IRubyObject rdevMajor() {
        checkInitialized();
        if (Platform.IS_WINDOWS) return getRuntime().getNil();
        return getRuntime().newFixnum(stat.major(stat.rdev()));
    }

    @JRubyMethod(name = "rdev_minor")
    public IRubyObject rdevMinor() {
        checkInitialized();
        if (Platform.IS_WINDOWS) return getRuntime().getNil();
        return getRuntime().newFixnum(stat.minor(stat.rdev()));
    }

    @JRubyMethod(name = "readable?")
    public IRubyObject readable_p() {
        checkInitialized();
        return getRuntime().newBoolean(stat.isReadable());
    }

    @JRubyMethod(name = "readable_real?")
    public IRubyObject readableReal_p() {
        checkInitialized();
        return getRuntime().newBoolean(stat.isReadableReal());
    }

    @JRubyMethod(name = "setgid?")
    public IRubyObject setgid_p() {
        checkInitialized();
        return getRuntime().newBoolean(stat.isSetgid());
    }

    @JRubyMethod(name = "setuid?")
    public IRubyObject setuid_p() {
        checkInitialized();
        return getRuntime().newBoolean(stat.isSetuid());
    }

    private long sizeInternal() {
        checkInitialized();
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
        checkInitialized();
        return getRuntime().newBoolean(stat.isSocket());
    }
    
    @JRubyMethod(name = "sticky?")
    public IRubyObject sticky_p() {
        checkInitialized();
        Ruby runtime = getRuntime();
        
        if (runtime.getPosix().isNative()) {
            return runtime.newBoolean(stat.isSticky());
        }
        
        return runtime.getNil();
    }

    @JRubyMethod(name = "symlink?")
    public IRubyObject symlink_p() {
        checkInitialized();
        return getRuntime().newBoolean(stat.isSymlink());
    }

    @JRubyMethod(name = "writable?")
    public IRubyObject writable_p() {
        checkInitialized();
    	return getRuntime().newBoolean(stat.isWritable());
    }
    
    @JRubyMethod(name = "writable_real?")
    public IRubyObject writableReal_p() {
        checkInitialized();
        return getRuntime().newBoolean(stat.isWritableReal());
    }
    
    @JRubyMethod(name = "zero?")
    public IRubyObject zero_p() {
        checkInitialized();
        return getRuntime().newBoolean(stat.isEmpty());
    }

    @JRubyMethod(name = "world_readable?")
    public IRubyObject worldReadable(ThreadContext context) {
        return getWorldMode(context, FileStat.S_IROTH);
    }

    @JRubyMethod(name = "world_writable?")
    public IRubyObject worldWritable(ThreadContext context) {
        return getWorldMode(context, FileStat.S_IWOTH);
    }

    private IRubyObject getWorldMode(ThreadContext context, int mode) {
        checkInitialized();
        if ((stat.mode() & mode) == mode) {
            return RubyNumeric.int2fix(context.runtime,
                    (stat.mode() & (S_IRUGO | S_IWUGO | S_IXUGO) ));
        }
        return context.nil;
    }
}
