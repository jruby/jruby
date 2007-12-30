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
 * Copyright (C) 2007 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
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

import com.sun.jna.Library;
import java.nio.ByteBuffer;

public interface LibC extends Library {
    public int chmod(String filename, int mode);
    public int chown(String filename, int user, int group);
    public int getegid();
    public int geteuid();
    public int getgid();
    public int getpgid();
    public int getpgrp();
    public int getppid();
    public int getpid();
    public int getuid();
    public int kill(int pid, int signal);
    public int lchmod(String filename, int mode);
    public int lchown(String filename, int user, int group);
    public int link(String oldpath,String newpath);
    public int lstat(String path, FileStat stat);
    public int mkdir(String path, int mode);
    public int stat(String path, FileStat stat);
    public int symlink(String oldpath,String newpath);
    public int readlink(String oldpath,ByteBuffer buffer,int len);
    public int umask(int mask);
    public int fork();
}
