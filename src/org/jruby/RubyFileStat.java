/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.io.FileDescriptor;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.posix.FileStat;
import org.jruby.ext.posix.util.Platform;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JRubyFile;

/**
 * Implements File::Stat
 */
@JRubyClass(name="File::Stat", include="Comparable")
public class RubyFileStat extends RubyObject {
    private static final long serialVersionUID = 1L;
    
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

        fileStatClass.includeModule(runtime.fastGetModule("Comparable"));
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
            
        file = JRubyFile.create(getRuntime().getCurrentDirectory(), filename);

        if (lstat) {
            stat = getRuntime().getPosix().lstat(file.getAbsolutePath());
        } else {
            stat = getRuntime().getPosix().stat(file.getAbsolutePath());
        }
    }

    @JRubyMethod(name = "initialize", required = 1, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(IRubyObject fname, Block unusedBlock) {
        setup(fname.convertToString().toString(), false);

        return this;
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
        if (!(other instanceof RubyFileStat)) getRuntime().getNil();
        
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
        try { buf.append("dev=0x").append(Long.toHexString(stat.dev())).append(", "); } catch (Exception e) {}
        try { buf.append("ino=").append(stat.ino()).append(", "); } catch (Exception e) {}
        buf.append("mode=0").append(Integer.toOctalString(stat.mode())).append(", "); 
        try { buf.append("nlink=").append(stat.nlink()).append(", "); } catch (Exception e) {}
        try { buf.append("uid=").append(stat.uid()).append(", "); } catch (Exception e) {}
        try { buf.append("gid=").append(stat.gid()).append(", "); } catch (Exception e) {}
        try { buf.append("rdev=0x").append(Long.toHexString(stat.rdev())).append(", "); } catch (Exception e) {}
        buf.append("size=").append(sizeInternal()).append(", ");
        try { buf.append("blksize=").append(stat.blockSize()).append(", "); } catch (Exception e) {}
        try { buf.append("blocks=").append(stat.blocks()).append(", "); } catch (Exception e) {}
        
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
        return getRuntime().newBoolean(stat.isSticky());
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
}
