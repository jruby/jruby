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
import org.jruby.util.NormalizedFile;

/**
 * note: renamed from FileStatClass.java
 * Implements File::Stat
 */
public class RubyFileStat extends RubyObject {
    private NormalizedFile file;
    private static final int READ = 222;
    private static final int WRITE = 444;

    public static RubyClass createFileStatClass(IRuby runtime) {
        RubyClass fileStatClass = runtime.getClass("File").defineClassUnder("Stat", 
        		runtime.getObject());
    	
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyFileStat.class);

        fileStatClass.defineMethod("directory?", callbackFactory.getMethod("directory_p"));
        fileStatClass.defineMethod("mode", callbackFactory.getMethod("mode"));
        fileStatClass.defineMethod("mtime", callbackFactory.getMethod("mtime"));
        fileStatClass.defineMethod("size", callbackFactory.getMethod("size"));
        fileStatClass.defineMethod("writable?", callbackFactory.getMethod("writable"));
        fileStatClass.defineMethod("symlink?", callbackFactory.getMethod("symlink_p"));
        fileStatClass.defineMethod("blksize", callbackFactory.getMethod("blksize"));
        fileStatClass.defineMethod("readable?", callbackFactory.getMethod("readable_p"));
        fileStatClass.defineMethod("ftype", callbackFactory.getMethod("ftype"));

        fileStatClass.defineMethod("file?", callbackFactory.getMethod("file_p"));
    	
        return fileStatClass;
    }

    protected RubyFileStat(IRuby runtime, NormalizedFile file) {
        super(runtime, runtime.getClass("File").getClass("Stat"));
		// In some versions of java changing user.dir will not get reflected in newly constructed
		// files.  Getting the absolutefile does seem to hack around this...
        this.file = (NormalizedFile)file.getAbsoluteFile();
    }
    
    public RubyFixnum blksize() {
        // We cannot determine, so always return 4096 (better than blowing up)
        return RubyFixnum.newFixnum(getRuntime(), 4096);
    }

    public RubyBoolean directory_p() {
        return getRuntime().newBoolean(file.isDirectory());
    }

    public RubyBoolean file_p() {
        return getRuntime().newBoolean(file.isFile());
    }
    
    public RubyString ftype() {
        if (!file.exists()) {
            throw getRuntime().newErrnoENOENTError("No such file or directory: " + file.toString());
        } else if (file.isDirectory()) {
            return getRuntime().newString("directory");
        } else if (file.isFile()) {
            return getRuntime().newString("file");
        } else {
            // possible?
            assert false: "Not a directory and not a file: " + file;
            return null;
        }
    }
    
    public IRubyObject mode() {
    	// implementation to lowest common denominator...Windows has no file mode, but C ruby returns either 0100444 or 0100666
    	int baseMode = 0100000;
    	if (file.canRead()) {
    		baseMode += READ;
    	}
    	
    	if (file.canWrite()) {
    		baseMode += WRITE;
    	}
    	
    	return getRuntime().newFixnum(baseMode);
    }
    
    public IRubyObject mtime() {
        return getRuntime().newFixnum(file.lastModified());
    }
    
    public IRubyObject readable_p() {
        return getRuntime().newBoolean(file.canRead());
    }
    
    public IRubyObject size() {
    	return getRuntime().newFixnum(file.length());
    }
    
    public IRubyObject symlink_p() {
        // We cannot determine this in Java, so we will always return false (better than blowing up)
        return getRuntime().getFalse();
    }
    
    public IRubyObject writable() {
    	return getRuntime().newBoolean(file.canWrite());
    }
}
