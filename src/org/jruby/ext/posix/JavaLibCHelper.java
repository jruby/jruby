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
import java.io.FileDescriptor;
import java.io.IOException;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import org.jruby.ext.posix.POSIX.ERRORS;
import org.jruby.ext.posix.util.Chmod;
import org.jruby.ext.posix.util.ExecIt;

/**
 * This libc implementation is created one per runtime instance versus the others which
 * are expected to be one static instance for whole JVM.  Because of this it is no big
 * deal to make reference to a POSIXHandler directly.
 */
// FIXME: we ignore all exceptions with shell launcher...should we do something better
public class JavaLibCHelper {
    public static final int STDIN = 0;
    public static final int STDOUT = 1;
    public static final int STDERR = 2;

    POSIXHandler handler;
    Field field;

    public JavaLibCHelper(POSIXHandler handler) {
        this.handler = handler;
        
        try {
            field = FileDescriptor.class.getDeclaredField("fd");
            field.setAccessible(true);
        } catch (SecurityException e) {
            // ignore
        } catch (NoSuchFieldException e) {
            field = null;
        }
    }
    
    public int chmod(String filename, int mode) {
        return Chmod.chmod(new File(filename), Integer.toOctalString(mode));
    }

    public int chown(String filename, int user, int group) {
        ExecIt launcher = new ExecIt(handler);
        int chownResult = -1;
        int chgrpResult = -1;
        
        try {
            if (user != -1) chownResult = launcher.runAndWait("chown", ""+user, filename);
            if (group != -1) chgrpResult = launcher.runAndWait("chgrp ", ""+user, filename);
        } catch (Exception e) {}
        
        return chownResult != -1 && chgrpResult != -1 ? 0 : 1;
    }

    public int getfd(FileDescriptor descriptor) {
        if (descriptor == null || field == null) return -1;
        
        try {
            return field.getInt(descriptor);
        } catch (SecurityException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        }

        return -1;
    }

    public String getlogin() {
        return System.getProperty("user.name");
    }

    public int getpid() {
        return handler.getPID();
    }

    public Passwd getpwent() {
        return new JavaPasswd(handler);
    }

    public int setpwent() {
        return 0;
    }

    public int endpwent() {
        return 0;
    }
    
    public int isatty(int fd) {
        return (fd == STDOUT || fd == STDIN || fd == STDERR) ? 1 : 0;
    }

    public int link(String oldpath, String newpath) {
        try {
            return new ExecIt(handler).runAndWait("ln", oldpath, newpath);
        } catch (Exception e) {}

        return -1;  // We tried and failed for some reason. Indicate error.
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
            return new ExecIt(handler).runAndWait("ln", "-s", oldpath, newpath);
        } catch (Exception e) {} 
            
        return -1;  // We tried and failed for some reason. Indicate error.
    }
    
    public int readlink(String oldpath, ByteBuffer buffer, int length) throws IOException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ExecIt(handler).runAndWait(baos, "readlink", oldpath);
            
            byte[] bytes = baos.toByteArray();
            
            if (bytes.length > length || bytes.length == 0) return -1;
            buffer.put(bytes, 0, bytes.length - 1); // trim off \n

            
            return buffer.position();
        } catch (InterruptedException e) {
        } 

        return -1; // We tried and failed for some reason. Indicate error.
    }

}
