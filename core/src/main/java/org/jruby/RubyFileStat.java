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

import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Error.typeError;

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

    private void checkInitialized(ThreadContext context) {
        if (stat == null) throw typeError(context, "uninitialized File::Stat");
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

    @Deprecated
    public IRubyObject initialize19(IRubyObject fname, Block unusedBlock) {
        return initialize(fname, unusedBlock);
    }

    /**
     * @param fname
     * @param unusedBlock
     * @return ""
     * @deprecated Use {@link RubyFileStat#initialize(ThreadContext, IRubyObject, Block)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject initialize(IRubyObject fname, Block unusedBlock) {
        return initialize(getCurrentContext(), fname, unusedBlock);
    }

    @JRubyMethod(name = "initialize", visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject fname, Block unusedBlock) {
        setup(RubyFile.get_path(context, fname).toString(), false);
        return this;
    }
    
    @JRubyMethod(name = "atime")
    public IRubyObject atime(ThreadContext context) {
        checkInitialized(context);
        return stat instanceof NanosecondFileStat nanoStat ?
                RubyTime.newTimeFromNanoseconds(context, stat.atime() * BILLION + nanoStat.aTimeNanoSecs()) :
                context.runtime.newTime(stat.atime() * 1000);
    }

    @Deprecated
    public IRubyObject atime() {
        return atime(getCurrentContext());
    }
    
    @JRubyMethod(name = "blksize")
    public IRubyObject blockSize(ThreadContext context) {
        checkInitialized(context);
        if (Platform.IS_WINDOWS) return context.nil;
        return asFixnum(context, stat.blockSize());
    }

    @Deprecated
    public RubyFixnum blksize() {
        ThreadContext context = getCurrentContext();
        checkInitialized(context);
        return asFixnum(context, stat.blockSize());
    }

    @JRubyMethod(name = "blockdev?")
    public IRubyObject blockdev_p(ThreadContext context) {
        checkInitialized(context);
        return asBoolean(context, stat.isBlockDev());
    }

    @Deprecated
    public IRubyObject blockdev_p() {
        return blockdev_p(getCurrentContext());
    }

    @JRubyMethod(name = "blocks")
    public IRubyObject blocks(ThreadContext context) {
        checkInitialized(context);
        if (Platform.IS_WINDOWS) return context.nil;
        return asFixnum(context, stat.blocks());
    }

    @Deprecated
    public IRubyObject blocks() {
        return blocks(getCurrentContext());
    }

    @JRubyMethod(name = "chardev?")
    public IRubyObject chardev_p(ThreadContext context) {
        checkInitialized(context);
        return asBoolean(context, stat.isCharDev());
    }

    @Deprecated
    public IRubyObject chardev_p() {
        return chardev_p(getCurrentContext());
    }

    @JRubyMethod(name = "<=>")
    public IRubyObject cmp(ThreadContext context, IRubyObject other) {
        checkInitialized(context);
        if (!(other instanceof RubyFileStat)) return context.nil;

        long time1 = stat.mtime();
        long time2 = ((RubyFileStat) other).stat.mtime();

        if (time1 == time2) {
            return asFixnum(context, 0);
        } else if (time1 < time2) {
            return asFixnum(context, -1);
        } else {
            return asFixnum(context, 1);
        }
    }

    @Deprecated
    public IRubyObject cmp(IRubyObject other) {
        return cmp(getCurrentContext(), other);
    }

    @JRubyMethod(name = "ctime")
    public IRubyObject ctime(ThreadContext context) {
        checkInitialized(context);
        if (stat instanceof NanosecondFileStat) {
            return RubyTime.newTimeFromNanoseconds(context.runtime, stat.ctime() * BILLION + ((NanosecondFileStat) stat).cTimeNanoSecs());
        }
        return context.runtime.newTime(stat.ctime() * 1000);
    }

    @Deprecated
    public IRubyObject ctime() {
        return ctime(getCurrentContext());
    }

    @JRubyMethod(name = "birthtime")
    public IRubyObject birthtime(ThreadContext context) {
        checkInitialized(context);
        FileTime btime;

        if (Platform.IS_LINUX) {
            throw context.runtime.newNotImplementedError("birthtime() function is unimplemented on this machine");
        }

        if (file == null || (btime = RubyFile.getBirthtimeWithNIO(file.absolutePath())) == null) {
            return ctime();
        }
        return context.runtime.newTime(btime.toMillis());
    }

    @Deprecated
    public IRubyObject birthtime() {
        return birthtime(getCurrentContext());
    }

    @JRubyMethod(name = "dev")
    public IRubyObject dev(ThreadContext context) {
        checkInitialized(context);
        return asFixnum(context, stat.dev());
    }

    @Deprecated
    public IRubyObject dev() {
        return dev(getCurrentContext());
    }
    
    @JRubyMethod(name = "dev_major")
    public IRubyObject devMajor(ThreadContext context) {
        checkInitialized(context);
        if (Platform.IS_WINDOWS) return context.nil;
        return asFixnum(context, stat.major(stat.dev()));
    }

    @Deprecated
    public IRubyObject devMajor() {
        return devMajor(getCurrentContext());
    }

    @JRubyMethod(name = "dev_minor")
    public IRubyObject devMinor(ThreadContext context) {
        checkInitialized(context);
        if (Platform.IS_WINDOWS) return context.nil;
        return asFixnum(context, stat.minor(stat.dev()));
    }

    @Deprecated
    public IRubyObject devMinor() {
        return devMinor(getCurrentContext());
    }

    @JRubyMethod(name = "directory?")
    public RubyBoolean directory_p(ThreadContext context) {
        checkInitialized(context);
        return asBoolean(context, stat.isDirectory());
    }

    @Deprecated
    public RubyBoolean directory_p() {
        return directory_p(getCurrentContext());
    }

    @JRubyMethod(name = "executable?")
    public IRubyObject executable_p(ThreadContext context) {
        checkInitialized(context);
        return asBoolean(context, stat.isExecutable());
    }

    @Deprecated
    public IRubyObject executable_p() {
        return executable_p(getCurrentContext());
    }

    @JRubyMethod(name = "executable_real?")
    public IRubyObject executableReal_p(ThreadContext context) {
        checkInitialized(context);
        return asBoolean(context, stat.isExecutableReal());
    }

    @Deprecated
    public IRubyObject executableReal_p() {
        return executableReal_p(getCurrentContext());
    }

    @JRubyMethod(name = "file?")
    public RubyBoolean file_p(ThreadContext context) {
        checkInitialized(context);
        return asBoolean(context, stat.isFile());
    }

    @Deprecated
    public RubyBoolean file_p() {
        return file_p(getCurrentContext());
    }

    @JRubyMethod(name = "ftype")
    public RubyString ftype(ThreadContext context) {
        checkInitialized(context);
        return newString(context, stat.ftype());
    }

    @Deprecated
    public RubyString ftype() {
        return ftype(getCurrentContext());
    }

    @JRubyMethod(name = "gid")
    public IRubyObject gid(ThreadContext context) {
        checkInitialized(context);
        if (Platform.IS_WINDOWS) return RubyFixnum.zero(context.runtime);
        return asFixnum(context, stat.gid());
    }

    @Deprecated
    public IRubyObject gid() {
        return gid(getCurrentContext());
    }
    
    @JRubyMethod(name = "grpowned?")
    public IRubyObject group_owned_p(ThreadContext context) {
        checkInitialized(context);
        if (Platform.IS_WINDOWS) return context.fals;
        return asBoolean(context, stat.isGroupOwned());
    }

    @Deprecated
    public IRubyObject group_owned_p() {
        return group_owned_p(getCurrentContext());
    }
    
    @JRubyMethod(name = "initialize_copy", visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject original) {
        if (!(original instanceof RubyFileStat)) throw typeError(context, "wrong argument class");

        checkFrozen();
        
        RubyFileStat originalFileStat = (RubyFileStat) original;
        
        file = originalFileStat.file;
        stat = originalFileStat.stat;
        
        return this;
    }
    
    @JRubyMethod(name = "ino")
    public IRubyObject ino(ThreadContext context) {
        checkInitialized(context);
        return asFixnum(context, stat.ino());
    }

    @Deprecated
    public IRubyObject ino() {
        return ino(getCurrentContext());
    }

    @JRubyMethod(name = "inspect")
    @Override
    public IRubyObject inspect() {
        ThreadContext context = metaClass.runtime.getCurrentContext();
        StringBuilder buf = new StringBuilder("#<");
        buf.append(getMetaClass().getRealClass().getName());
        if (stat == null) {
            buf.append(": uninitialized");
        } else {
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
            buf.append("size=").append(sizeInternal(context)).append(", ");
            try {
                buf.append("blksize=").append(blockSize(context).inspect().toString()); } catch (Exception e) {} finally { buf.append(", "); }
            try { buf.append("blocks=").append(blocks().inspect().toString()); } catch (Exception e) {} finally { buf.append(", "); }

            buf.append("atime=").append(atime().inspect()).append(", ");
            buf.append("mtime=").append(mtime().inspect()).append(", ");
            buf.append("ctime=").append(ctime().inspect());
            if (Platform.IS_BSD || Platform.IS_MAC) {
                buf.append(", ").append("birthtime=").append(birthtime());
            }
        }
        buf.append('>');
        
        return newString(context, buf.toString());
    }

    @JRubyMethod(name = "uid")
    public IRubyObject uid(ThreadContext context) {
        checkInitialized(context);
        if (Platform.IS_WINDOWS) return RubyFixnum.zero(context.runtime);
        return asFixnum(context, stat.uid());
    }

    @Deprecated
    public IRubyObject uid() {
        return uid(getCurrentContext());
    }
    
    @JRubyMethod(name = "mode")
    public IRubyObject mode(ThreadContext context) {
        checkInitialized(context);
        return asFixnum(context, stat.mode());
    }

    @Deprecated
    public IRubyObject mode() {
        return mode(getCurrentContext());
    }

    @JRubyMethod(name = "mtime")
    public IRubyObject mtime(ThreadContext context) {
        checkInitialized(context);
        return stat instanceof NanosecondFileStat ?
                RubyTime.newTimeFromNanoseconds(context, stat.mtime() * BILLION + ((NanosecondFileStat) stat).mTimeNanoSecs()) :
                context.runtime.newTime(stat.mtime() * 1000);
    }

    @Deprecated
    public IRubyObject mtime() {
        return mtime(getCurrentContext());
    }

    public IRubyObject mtimeEquals(ThreadContext context, IRubyObject other) {
        FileStat otherStat = newFileStat(context.runtime, other.convertToString().toString(), false).stat;
        boolean equal = stat.mtime() == otherStat.mtime();

        if (stat instanceof NanosecondFileStat && otherStat instanceof NanosecondFileStat) {
            equal = equal && ((NanosecondFileStat) stat).mTimeNanoSecs() == ((NanosecondFileStat) otherStat).mTimeNanoSecs();
        }

        return asBoolean(context, equal);
    }

    @Deprecated
    public IRubyObject mtimeEquals(IRubyObject other) {
        return mtimeEquals(getCurrentContext(), other);
    }

    public IRubyObject mtimeGreaterThan(ThreadContext context, IRubyObject other) {
        FileStat otherStat = newFileStat(context.runtime, other.convertToString().toString(), false).stat;
        boolean gt;

        if (stat instanceof NanosecondFileStat && otherStat instanceof NanosecondFileStat) {
            gt = (stat.mtime() * BILLION + ((NanosecondFileStat) stat).mTimeNanoSecs())
                    > (otherStat.mtime() * BILLION + ((NanosecondFileStat) otherStat).mTimeNanoSecs());
        } else {
            gt = stat.mtime() > otherStat.mtime();
        }

        return asBoolean(context, gt);
    }

    @Deprecated
    public IRubyObject mtimeGreaterThan(IRubyObject other) {
        return mtimeGreaterThan(getCurrentContext(), other);
    }

    public IRubyObject mtimeLessThan(ThreadContext context, IRubyObject other) {
        FileStat otherStat = newFileStat(context.runtime, other.convertToString().toString(), false).stat;
        boolean lt;

        if (stat instanceof NanosecondFileStat && otherStat instanceof NanosecondFileStat) {
            lt = (stat.mtime() * BILLION + ((NanosecondFileStat) stat).mTimeNanoSecs())
                    < (otherStat.mtime() * BILLION + ((NanosecondFileStat) otherStat).mTimeNanoSecs());
        } else {
            lt = stat.mtime() < otherStat.mtime();
        }

        return asBoolean(context, lt);
    }

    @Deprecated
    public IRubyObject mtimeLessThan(IRubyObject other) {
        return mtimeLessThan(getCurrentContext(), other);
    }

    @JRubyMethod(name = "nlink")
    public IRubyObject nlink(ThreadContext context) {
        checkInitialized(context);
        return asFixnum(context, stat.nlink());
    }

    @Deprecated
    public IRubyObject nlink() {
        return nlink(getCurrentContext());
    }

    @JRubyMethod(name = "owned?")
    public IRubyObject owned_p(ThreadContext context) {
        checkInitialized(context);
        return asBoolean(context, stat.isOwned());
    }

    @Deprecated
    public IRubyObject owned_p() {
        return owned_p(getCurrentContext());
    }

    @JRubyMethod(name = "pipe?")
    public IRubyObject pipe_p(ThreadContext context) {
        checkInitialized(context);
        return asBoolean(context, stat.isNamedPipe());
    }

    @Deprecated
    public IRubyObject pipe_p() {
        return pipe_p(getCurrentContext());
    }

    @JRubyMethod(name = "rdev")
    public IRubyObject rdev(ThreadContext context) {
        checkInitialized(context);
        return asFixnum(context, stat.rdev());
    }

    @Deprecated
    public IRubyObject rdev() {
        return rdev(getCurrentContext());
    }
    
    @JRubyMethod(name = "rdev_major")
    public IRubyObject rdevMajor(ThreadContext context) {
        checkInitialized(context);
        if (Platform.IS_WINDOWS) return context.nil;
        return asFixnum(context, stat.major(stat.rdev()));
    }

    @Deprecated
    public IRubyObject rdevMajor() {
        return rdevMajor(getCurrentContext());
    }

    @JRubyMethod(name = "rdev_minor")
    public IRubyObject rdevMinor(ThreadContext context) {
        checkInitialized(context);
        if (Platform.IS_WINDOWS) return context.nil;
        return asFixnum(context, stat.minor(stat.rdev()));
    }

    @Deprecated
    public IRubyObject rdevMinor() {
        return rdevMinor(getCurrentContext());
    }

    @JRubyMethod(name = "readable?")
    public IRubyObject readable_p(ThreadContext context) {
        checkInitialized(context);
        return asBoolean(context, stat.isReadable());
    }

    @Deprecated
    public IRubyObject readable_p() {
        return readable_p(getCurrentContext());
    }

    @JRubyMethod(name = "readable_real?")
    public IRubyObject readableReal_p(ThreadContext context) {
        checkInitialized(context);
        return asBoolean(context, stat.isReadableReal());
    }

    @Deprecated
    public IRubyObject readableReal_p() {
        return readableReal_p(getCurrentContext());
    }

    @JRubyMethod(name = "setgid?")
    public IRubyObject setgid_p(ThreadContext context) {
        checkInitialized(context);
        return asBoolean(context, stat.isSetgid());
    }

    @Deprecated
    public IRubyObject setgid_p() {
        return setgid_p(getCurrentContext());
    }

    @JRubyMethod(name = "setuid?")
    public IRubyObject setuid_p(ThreadContext context) {
        checkInitialized(context);
        return asBoolean(context, stat.isSetuid());
    }

    @Deprecated
    public IRubyObject setuid_p() {
        return setuid_p(getCurrentContext());
    }

    private long sizeInternal(ThreadContext context) {
        checkInitialized(context);
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
    public IRubyObject size(ThreadContext context) {
        return asFixnum(context, sizeInternal(context));
    }

    @Deprecated
    public IRubyObject size() {
        return size(getCurrentContext());
    }
    
    @JRubyMethod(name = "size?")
    public IRubyObject size_p(ThreadContext context) {
        long size = sizeInternal(context);

        if (size == 0) return context.nil;

        return asFixnum(context, size);
    }

    @Deprecated
    public IRubyObject size_p() {
        return size_p(getCurrentContext());
    }

    @JRubyMethod(name = "socket?")
    public IRubyObject socket_p(ThreadContext context) {
        checkInitialized(context);
        return asBoolean(context, stat.isSocket());
    }

    @Deprecated
    public IRubyObject socket_p() {
        return socket_p(getCurrentContext());
    }
    
    @JRubyMethod(name = "sticky?")
    public IRubyObject sticky_p(ThreadContext context) {
        checkInitialized(context);

        return context.runtime.getPosix().isNative() ?
                asBoolean(context, stat.isSticky()) :
                context.nil;
    }

    @Deprecated
    public IRubyObject sticky_p() {
        return sticky_p(getCurrentContext());
    }

    @JRubyMethod(name = "symlink?")
    public IRubyObject symlink_p(ThreadContext context) {
        checkInitialized(context);
        return asBoolean(context, stat.isSymlink());
    }

    @Deprecated
    public IRubyObject symlink_p() {
        return symlink_p(getCurrentContext());
    }

    @JRubyMethod(name = "writable?")
    public IRubyObject writable_p(ThreadContext context) {
        checkInitialized(context);
        return asBoolean(context, stat.isWritable());
    }

    @Deprecated
    public IRubyObject writable_p() {
        return writable_p(getCurrentContext());
    }
    
    @JRubyMethod(name = "writable_real?")
    public IRubyObject writableReal_p(ThreadContext context) {
        checkInitialized(context);
        return asBoolean(context, stat.isWritableReal());
    }

    @Deprecated
    public IRubyObject writableReal_p() {
        return writableReal_p(getCurrentContext());
    }
    
    @JRubyMethod(name = "zero?")
    public IRubyObject zero_p(ThreadContext context) {
        checkInitialized(context);
        return asBoolean(context, stat.isEmpty());
    }

    @Deprecated
    public IRubyObject zero_p() {
        return zero_p(getCurrentContext());
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
        checkInitialized(context);
        if ((stat.mode() & mode) == mode) {
            return RubyNumeric.int2fix(context.runtime,
                    (stat.mode() & (S_IRUGO | S_IWUGO | S_IXUGO) ));
        }
        return context.nil;
    }
}
