 /*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2007 
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
package org.jruby.ext.posix;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import java.nio.ByteBuffer;
import org.jruby.Ruby;
import org.jruby.ext.posix.POSIX.ERRORS;
import org.jruby.ext.posix.util.Chmod;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ShellLauncher;

/**
 * This libc implementation is created one per runtime instance versus the others which
 * are expected to be one static instance for whole JVM.  Because of this it is no big
 * deal to make reference to a POSIXHandler directly.
 */
public class JavaLibC implements LibC {
    private POSIXHandler handler;
    private Ruby runtime;
    
    public JavaLibC(POSIXHandler handler) {
        this.handler = handler;
        // FIXME: Remove this and runtime field when we make a JRuby-agnostic shell launcher
        this.runtime = ((JRubyPOSIXHandler) handler).runtime;
    }

    public int chmod(String filename, int mode) {
        return Chmod.chmod(new File(filename), Integer.toOctalString(mode));
    }

    public int chown(String filename, int user, int group) {
        int chownResult = -1;
        int chgrpResult = -1;
        try {
            if (user != -1) {
                Process chown = Runtime.getRuntime().exec("chown " + user + " " + filename);
                chownResult = chown.waitFor();
            }
            if (group != -1) {
                Process chgrp = Runtime.getRuntime().exec("chgrp " + user + " " + filename);
                chgrpResult = chgrp.waitFor();
            }
        } catch (IOException ioe) {
            // FIXME: ignore?
        } catch (InterruptedException ie) {
            // FIXME: ignore?
        }
        if (chownResult != -1 && chgrpResult != -1) return 0;
        return -1;
    }

    public int getegid() {
        handler.unimplementedError("getegid");
        
        return -1;
    }
    
    public int geteuid() {
        handler.unimplementedError("geteuid");
        
        return -1;
    }

    public int getgid() {
        handler.unimplementedError("getgid");
        
        return -1;
    }

    public int getpgid() {
        handler.unimplementedError("getpgid");
        
        return -1;
    }

    public int getpgrp() {
        handler.unimplementedError("getpgrp");
        
        return -1;
    }

    public int getpid() {
      return runtime.hashCode();
    }

    public int getppid() {
        handler.unimplementedError("getppid");
        
        return -1;
    }

    public int getuid() {
        handler.unimplementedError("getuid");
        
        return -1;
    }

    // FIXME: Can be implemented
    public int kill(int pid, int signal) {
        handler.unimplementedError("kill");
        
        return -1;
    }

    // FIXME: Can be implemented
    public int lchmod(String filename, int mode) {
        handler.unimplementedError("getuid");
        
        return -1;
    }

    // FIXME: Can be implemented
    public int lchown(String filename, int user, int group) {
        handler.unimplementedError("lchown");
        
        return -1;
    }

    public int link(String oldpath, String newpath) {
        try {
            return new ShellLauncher(runtime).runAndWait(new IRubyObject[] {
                    runtime.newString("ln"), 
                    runtime.newString(oldpath), runtime.newString(newpath)
            });
        } catch (Exception e) { // We tried and failed for some reason. Indicate error.
            return -1;
        }
    }
    
    public int lstat(String path, FileStat stat) {
        // FIXME: Bulletproof this or no?
        JavaFileStat jstat = (JavaFileStat) stat;
        
        jstat.setup(path);

        // TODO: Add error reporting for cases we can calculate: ENOTDIR, ENAMETOOLONG, ENOENT
        // EACCES, ELOOP, EFAULT, EIO

        return 0;
    }
    
    public int mkdir(String path, int mode) {
        File dir = new File(path);
        
        if (!dir.mkdir()) return -1;

        chmod(path, mode);
        
        return 0;
    }
    
    public int stat(String path, FileStat stat) {
        // FIXME: Bulletproof this or no?
        JavaFileStat jstat = (JavaFileStat) stat;
        
        try {
            File file = new File(path);
            
            if (!file.exists()) handler.error(ERRORS.ENOENT, path);
                
            jstat.setup(file.getCanonicalPath());
        } catch (IOException e) {
            // TODO: Throw error when we have problems stat'ing canonicalizing
        }

        // TODO: Add error reporting for cases we can calculate: ENOTDIR, ENAMETOOLONG, ENOENT
        // EACCES, ELOOP, EFAULT, EIO

        return 0;
    }
 
    public int symlink(String oldpath, String newpath) {
        try {
            // Replace with non-JRuby-specific shell lancher
            return new ShellLauncher(runtime).runAndWait(new IRubyObject[] {
                    runtime.newString("ln"), runtime.newString("-s"), 
                    runtime.newString(oldpath), runtime.newString(newpath)
            });
        } catch (Exception e) { // We tried and failed for some reason. Indicate error.
            return -1;
        }
    }
    
    public int readlink(String oldpath, ByteBuffer buffer, int length) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            // Replace with non-JRuby-specific shell lancher
            ShellLauncher launcher = new ShellLauncher(runtime);
            launcher.runAndWait(
                    new IRubyObject[] {runtime.newString("readlink"), runtime.newString(oldpath)},
                    baos);
            
            byte[] bytes = baos.toByteArray();
            
            if (bytes.length > length) return -1;
            // trim off \n
            buffer.put(bytes, 0, bytes.length - 1);
            
            return buffer.position();
        } catch (Exception e) { // We tried and failed for some reason. Indicate error.
            return -1;
        }
    }

    public int umask(int mask) {
        // TODO: We can possibly maintain an internal mask and try and apply it to individual
        // libc methods.  
        return 0;
    }
    
    public int fork() {
        return -1;
    }
}
