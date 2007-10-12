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
 * Copyright (C) 2004-2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
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

import java.io.File;
import java.io.IOException;
import org.jruby.anno.JRubyMethod;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JRubyFile;

public class RubyFileTest {
    public static RubyModule createFileTestModule(Ruby runtime) {
        RubyModule fileTestModule = runtime.defineModule("FileTest");
        runtime.setFileTest(fileTestModule);
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyFileTest.class);
        
        fileTestModule.defineAnnotatedMethods(RubyFileTest.class);
        
        return fileTestModule;
    }
    
    @JRubyMethod(name = "setgid?", required = 1, module = true)
    public static RubyBoolean setgid_p(IRubyObject recv, IRubyObject filename) {
        throw recv.getRuntime().newNotImplementedError("FileTest#setgid? not yet implemented");
    }
    
    @JRubyMethod(name = "grpowned?", required = 1, module = true)
    public static RubyBoolean grpowned_p(IRubyObject recv, IRubyObject filename) {
        throw recv.getRuntime().newNotImplementedError("FileTest#grpowned? not yet implemented");
    }
    
    @JRubyMethod(name = "sticky?", required = 1, module = true)
    public static RubyBoolean sticky_p(IRubyObject recv, IRubyObject filename) {
        throw recv.getRuntime().newNotImplementedError("FileTest#sticky? not yet implemented");
    }
    
    @JRubyMethod(name = "chardev?", required = 1, module = true)
    public static RubyBoolean chardev_p(IRubyObject recv, IRubyObject filename) {
        throw recv.getRuntime().newNotImplementedError("FileTest#chardev? not yet implemented");
    }
    
    @JRubyMethod(name = "identical?", required = 2, module = true)
    public static RubyBoolean identical_p(IRubyObject recv, IRubyObject filename1, IRubyObject filename2) {
        throw recv.getRuntime().newNotImplementedError("FileTest#identical? not yet implemented");
    }
    
    @JRubyMethod(name = "socket?", required = 1, module = true)
    public static RubyBoolean socket_p(IRubyObject recv, IRubyObject filename) {
        throw recv.getRuntime().newNotImplementedError("FileTest#socket? not yet implemented");
    }
    
    @JRubyMethod(name = "executable_real?", required = 1, module = true)
    public static RubyBoolean executable_real_p(IRubyObject recv, IRubyObject filename) {
        throw recv.getRuntime().newNotImplementedError("FileTest#executable_real? not yet implemented");
    }
    
    @JRubyMethod(name = "blockdev?", required = 1, module = true)
    public static RubyBoolean blockdev_p(IRubyObject recv, IRubyObject filename) {
        throw recv.getRuntime().newNotImplementedError("FileTest#blockdev? not yet implemented");
    }
    
    @JRubyMethod(name = "owned?", required = 1, module = true)
    public static RubyBoolean owned_p(IRubyObject recv, IRubyObject filename) {
        throw recv.getRuntime().newNotImplementedError("FileTest#owned? not yet implemented");
    }
    
    @JRubyMethod(name = "setuid?", required = 1, module = true)
    public static RubyBoolean setuid_p(IRubyObject recv, IRubyObject filename) {
        throw recv.getRuntime().newNotImplementedError("FileTest#setuid? not yet implemented");
    }
    
    @JRubyMethod(name = "pipe?", required = 1, module = true)
    public static RubyBoolean pipe_p(IRubyObject recv, IRubyObject filename) {
        throw recv.getRuntime().newNotImplementedError("FileTest#pipe? not yet implemented");
    }
    
    @JRubyMethod(name = "size?", required = 1, module = true)
    public static RubyBoolean size_p(IRubyObject recv, IRubyObject filename) {
        throw recv.getRuntime().newNotImplementedError("FileTest#size? not yet implemented");
    }
    
    @JRubyMethod(name = "directory?", required = 1, module = true)
    public static RubyBoolean directory_p(IRubyObject recv, IRubyObject filename) {
        return recv.getRuntime().newBoolean(newFile(filename).isDirectory());
    }
    
    @JRubyMethod(name = "exist?", name2 = "exists?", required = 1, module = true)
    public static IRubyObject exist_p(IRubyObject recv, IRubyObject filename) {
        return recv.getRuntime().newBoolean(newFile(filename).exists());
    }

    // We do both readable and readable_real through the same method because
    // in our java process effective and real userid will always be the same.
    @JRubyMethod(name = "readable?", name2 = "readable_real?", required = 1, module = true)
    public static RubyBoolean readable_p(IRubyObject recv, IRubyObject filename) {
        return filename.getRuntime().newBoolean(newFile(filename).canRead());
    }

    @JRubyMethod(name = "executable?", required = 1, module = true)
    public static IRubyObject executable_p(IRubyObject recv, IRubyObject filename) {
        recv.getRuntime().getWarnings().warn("executable? does not work on JRuby and will return a dummy value");
        return exist_p(recv, filename);
    }
    
    @JRubyMethod(name = "size", required = 1, module = true)
    public static IRubyObject size(IRubyObject recv, IRubyObject filename) {
        JRubyFile file = newFile(filename);
        
        if (!file.exists()) {
            throw recv.getRuntime().newErrnoENOENTError("No such file: " + filename);
        }
        return filename.getRuntime().newFixnum(file.length());
    }
    
    @JRubyMethod(name = "symlink?", required = 1, module = true)
    public static RubyBoolean symlink_p(IRubyObject recv, IRubyObject _filename) {
        Ruby runtime = recv.getRuntime();
        RubyString filename = RubyString.stringValue(_filename);
        
        JRubyFile file = JRubyFile.create(runtime.getCurrentDirectory(), filename.toString());
        
        try {
            // Only way to determine symlink is to compare canonical and absolute files
            // However symlinks in containing path must not produce false positives, so we check that first
            File absoluteParent = file.getAbsoluteFile().getParentFile();
            File canonicalParent = file.getAbsoluteFile().getParentFile().getCanonicalFile();
            
            if (canonicalParent.getAbsolutePath().equals(absoluteParent.getAbsolutePath())) {
                // parent doesn't change when canonicalized, compare absolute and canonical file directly
                return file.getAbsolutePath().equals(file.getCanonicalPath()) ? runtime.getFalse() : runtime.getTrue();
            }
            
            // directory itself has symlinks (canonical != absolute), so build new path with canonical parent and compare
            file = JRubyFile.create(runtime.getCurrentDirectory(), canonicalParent.getAbsolutePath() + "/" + file.getName());
            return file.getAbsolutePath().equals(file.getCanonicalPath()) ? runtime.getFalse() : runtime.getTrue();
        } catch (IOException ioe) {
            // problem canonicalizing the file; nothing we can do but return false
            return runtime.getFalse();
        }
    }

    // We do both writable and writable_real through the same method because
    // in our java process effective and real userid will always be the same.
    @JRubyMethod(name = "writable?", name2 = "writable_real?", required = 1, module = true)
    public static RubyBoolean writable_p(IRubyObject recv, IRubyObject filename) {
        return filename.getRuntime().newBoolean(newFile(filename).canWrite());
    }
    
    @JRubyMethod(name = "zero?", required = 1, module = true)
    public static RubyBoolean zero_p(IRubyObject recv, IRubyObject filename) {
        JRubyFile file = newFile(filename);
        
        return filename.getRuntime().newBoolean(file.exists() && file.length() == 0L);
    }

    @JRubyMethod(name = "file?", required = 1, module = true)
    public static RubyBoolean file_p(IRubyObject recv, IRubyObject filename) {
        JRubyFile file = newFile(filename);
        
        return filename.getRuntime().newBoolean(file.isFile());
    }
    
    private static JRubyFile newFile(IRubyObject path) {
        return JRubyFile.create(path.getRuntime().getCurrentDirectory(), path.convertToString().toString());
    }
}
