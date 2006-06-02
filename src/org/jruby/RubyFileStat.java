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

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JRubyFile;

/**
 * note: renamed from FileStatClass.java
 * Implements File::Stat
 */
public class RubyFileStat extends RubyObject {
    private static final int READ = 222;
    private static final int WRITE = 444;

    private RubyFixnum blksize;
    private RubyBoolean isDirectory;
    private RubyBoolean isFile;
    private RubyString ftype;
    private RubyFixnum mode;
    private RubyTime mtime;
    private RubyBoolean isReadable;
    private RubyBoolean isWritable;
    private RubyFixnum size;
    private RubyBoolean isSymlink;

    public static RubyClass createFileStatClass(IRuby runtime) {
        final RubyClass fileStatClass = runtime.getClass("File").defineClassUnder("Stat",runtime.getObject());
        final CallbackFactory callbackFactory = runtime.callbackFactory(RubyFileStat.class);
        //        fileStatClass.defineMethod("<=>", callbackFactory.getMethod(""));
        //        fileStateClass.includeModule(runtime.getModule("Comparable"));
        //        fileStatClass.defineMethod("atime", callbackFactory.getMethod(""));
        fileStatClass.defineMethod("blksize", callbackFactory.getMethod("blksize"));
        //        fileStatClass.defineMethod("blockdev?", callbackFactory.getMethod(""));
        //        fileStatClass.defineMethod("blocks", callbackFactory.getMethod(""));
        //        fileStatClass.defineMethod("chardev?", callbackFactory.getMethod(""));
        //        fileStatClass.defineMethod("ctime", callbackFactory.getMethod(""));
        //        fileStatClass.defineMethod("dev", callbackFactory.getMethod(""));
        //        fileStatClass.defineMethod("dev_major", callbackFactory.getMethod(""));
        //        fileStatClass.defineMethod("dev_minor", callbackFactory.getMethod(""));
        fileStatClass.defineMethod("directory?", callbackFactory.getMethod("directory_p"));
        //        fileStatClass.defineMethod("executable?", callbackFactory.getMethod(""));
        //        fileStatClass.defineMethod("executable_real?", callbackFactory.getMethod(""));
        fileStatClass.defineMethod("file?", callbackFactory.getMethod("file_p"));
        fileStatClass.defineMethod("ftype", callbackFactory.getMethod("ftype"));
        //        fileStatClass.defineMethod("gid", callbackFactory.getMethod(""));
        //        fileStatClass.defineMethod("grpowned?", callbackFactory.getMethod(""));
        fileStatClass.defineMethod("ino", callbackFactory.getMethod("ino"));
        fileStatClass.defineMethod("mode", callbackFactory.getMethod("mode"));
        fileStatClass.defineMethod("mtime", callbackFactory.getMethod("mtime"));
        //        fileStatClass.defineMethod("nlink", callbackFactory.getMethod(""));
        //        fileStatClass.defineMethod("owned?", callbackFactory.getMethod(""));
        //        fileStatClass.defineMethod("pipe?", callbackFactory.getMethod(""));
        //        fileStatClass.defineMethod("rdev", callbackFactory.getMethod(""));
        //        fileStatClass.defineMethod("rdev_major", callbackFactory.getMethod(""));
        //        fileStatClass.defineMethod("rdev_minor", callbackFactory.getMethod(""));
        fileStatClass.defineMethod("readable?", callbackFactory.getMethod("readable_p"));
        //        fileStatClass.defineMethod("readable_real?", callbackFactory.getMethod(""));
        //        fileStatClass.defineMethod("setgid?", callbackFactory.getMethod(""));
        //        fileStatClass.defineMethod("setuid?", callbackFactory.getMethod(""));
        fileStatClass.defineMethod("size", callbackFactory.getMethod("size"));
        //        fileStatClass.defineMethod("size?", callbackFactory.getMethod(""));
        //        fileStatClass.defineMethod("socket?", callbackFactory.getMethod(""));
        //        fileStatClass.defineMethod("sticky?", callbackFactory.getMethod(""));
        fileStatClass.defineMethod("symlink?", callbackFactory.getMethod("symlink_p"));
        //        fileStatClass.defineMethod("uid", callbackFactory.getMethod(""));
        fileStatClass.defineMethod("writable?", callbackFactory.getMethod("writable"));
        //        fileStatClass.defineMethod("writable_real?", callbackFactory.getMethod(""));
        //        fileStatClass.defineMethod("zero?", callbackFactory.getMethod(""));
    	
        return fileStatClass;
    }

    protected RubyFileStat(IRuby runtime, JRubyFile file) {
        super(runtime, runtime.getClass("File").getClass("Stat"));

        if(!file.exists()) {
            throw runtime.newErrnoENOENTError("No such file or directory - " + file.getPath());
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
        isReadable = runtime.newBoolean(file.canRead());
        isWritable = runtime.newBoolean(file.canWrite());
        size = runtime.newFixnum(file.length());
        // We cannot determine this in Java, so we will always return false (better than blowing up)
        isSymlink = runtime.getFalse();
    }
    
    public RubyFixnum blksize() {
        return blksize;
    }

    public RubyBoolean directory_p() {
        return isDirectory;
    }

    public RubyBoolean file_p() {
        return isFile;
    }
    
    public RubyString ftype() {
        return ftype;
    }
    
    // Limitation: We have no pure-java way of getting inode.  webrick needs this defined to work.
    public IRubyObject ino() {
        return getRuntime().newFixnum(0);
    }
    
    public IRubyObject mode() {
        return mode;
    }
    
    public IRubyObject mtime() {
        return mtime;
    }
    
    public IRubyObject readable_p() {
        return isReadable;
    }
    
    public IRubyObject size() {
        return size;
    }
    
    public IRubyObject symlink_p() {
        return isSymlink;
    }
    
    public IRubyObject writable() {
    	return isWritable;
    }
}
