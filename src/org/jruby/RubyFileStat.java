/***** BEGIN LICENSE BLOCK *****
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

import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JRubyFile;

/**
 * note: renamed from FileStatClass.java
 * Implements File::Stat
 */
public class RubyFileStat extends RubyObject {
    private static final int READ = 0222;
    private static final int WRITE = 0444;

    private RubyFixnum blksize;
    private RubyBoolean isDirectory;
    private RubyBoolean isFile;
    private RubyString ftype;
    private RubyFixnum mode;
    private RubyTime mtime;
    private RubyTime ctime;
    private RubyBoolean isReadable;
    private RubyBoolean isWritable;
    private RubyFixnum size;
    private RubyBoolean isSymlink;

    private static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyFileStat(runtime, klass);
        }
    };

    public static RubyClass createFileStatClass(Ruby runtime) {
        // TODO: NOT_ALLOCATABLE_ALLOCATOR is probably ok here. Confirm. JRUBY-415
        final RubyClass fileStatClass = runtime.getFile().defineClassUnder("Stat",runtime.getObject(), ALLOCATOR);
        runtime.setFileStat(fileStatClass);
        final CallbackFactory callbackFactory = runtime.callbackFactory(RubyFileStat.class);

        fileStatClass.includeModule(runtime.fastGetModule("Comparable"));
        fileStatClass.defineMethod("<=>", callbackFactory.getFastMethod("not_implemented1", IRubyObject.class));
        fileStatClass.defineMethod("atime", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("blockdev?", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("blocks", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("chardev?", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("dev", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("dev_major", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("dev_minor", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("executable?", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("executable_real?", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("gid", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("grpowned?", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("initialize_copy", callbackFactory.getFastMethod("not_implemented1", IRubyObject.class));
        fileStatClass.defineMethod("inspect", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("nlink", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("owned?", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("pipe?", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("rdev", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("rdev_major", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("rdev_minor", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("readable_real?", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("setgid?", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("setuid?", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("size?", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("socket?", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("sticky?", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("writable_real?", callbackFactory.getFastMethod("not_implemented"));
        fileStatClass.defineMethod("zero?", callbackFactory.getFastMethod("not_implemented"));
        
        fileStatClass.defineAnnotatedMethods(RubyFileStat.class);
    	
        return fileStatClass;
    }

    protected RubyFileStat(Ruby runtime, RubyClass clazz) {
        super(runtime, clazz);

    }

    @JRubyMethod(name = "initialize", required = 1, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(IRubyObject fname, Block unusedBlock) {
        Ruby runtime = getRuntime();
        String filename = fname.toString();
        JRubyFile file = JRubyFile.create(runtime.getCurrentDirectory(), filename);

        if (!file.exists()) {
            throw runtime.newErrnoENOENTError("No such file or directory - " + filename);
        }

        // We cannot determine, so always return 4096 (better than blowing up)
        blksize = runtime.newFixnum(4096);
        isDirectory = runtime.newBoolean(file.isDirectory());
        isFile = runtime.newBoolean(file.isFile());
        ftype = file.isDirectory()? runtime.newString("directory") : (file.isFile() ? runtime.newString("file") : null);

    	// implementation to lowest common denominator...Windows has no file mode, but C ruby returns either 0100444 or 0100666
    	int baseMode = 0100000;
    	if (file.canRead()) {
            baseMode += READ;
    	}    	
    	if (file.canWrite()) {
            baseMode += WRITE;
    	}
    	mode = runtime.newFixnum(baseMode);
        mtime = runtime.newTime(file.lastModified());
        ctime = runtime.newTime(file.getParentFile().lastModified());
        isReadable = runtime.newBoolean(file.canRead());
        isWritable = runtime.newBoolean(file.canWrite());
        size = runtime.newFixnum(file.length());
        // We cannot determine this in Java, so we will always return false (better than blowing up)
        isSymlink = runtime.getFalse();
        return this;
    }
    
    public IRubyObject not_implemented() {
        throw getRuntime().newNotImplementedError("File::Stat#" + getRuntime().getCurrentContext().getFrameName() + " not yet implemented");
    }
    
    public IRubyObject not_implemented1(IRubyObject arg) {
        throw getRuntime().newNotImplementedError("File::Stat#" + getRuntime().getCurrentContext().getFrameName() + " not yet implemented");
    }
    
    @JRubyMethod(name = "blksize")
    public RubyFixnum blksize() {
        return blksize;
    }

    @JRubyMethod(name = "directory?")
    public RubyBoolean directory_p() {
        return isDirectory;
    }

    @JRubyMethod(name = "file?")
    public RubyBoolean file_p() {
        return isFile;
    }
    
    @JRubyMethod(name = "ftype")
    public RubyString ftype() {
        return ftype;
    }
    
    // Limitation: We have no pure-java way of getting inode.  webrick needs this defined to work.
    @JRubyMethod(name = "ino")
    public IRubyObject ino() {
        return getRuntime().newFixnum(0);
    }
    
    // Limitation: We have no pure-java way of getting uid. RubyZip needs this defined to work.
    @JRubyMethod(name = "uid")
    public IRubyObject uid() {
        return getRuntime().newFixnum(-1);
    }
    
    @JRubyMethod(name = "mode")
    public IRubyObject mode() {
        return mode;
    }
    
    @JRubyMethod(name = "mtime")
    public IRubyObject mtime() {
        return mtime;
    }

    @JRubyMethod(name = "ctime")
    public IRubyObject ctime() {
        return ctime;
    }
    
    @JRubyMethod(name = "readable?")
    public IRubyObject readable_p() {
        return isReadable;
    }
    
    @JRubyMethod(name = "size")
    public IRubyObject size() {
        return size;
    }
    
    @JRubyMethod(name = "symlink?")
    public IRubyObject symlink_p() {
        return isSymlink;
    }
    
    @JRubyMethod(name = "writable?")
    public IRubyObject writable_p() {
    	return isWritable;
    }
}
