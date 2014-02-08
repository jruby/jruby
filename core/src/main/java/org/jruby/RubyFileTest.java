/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2004-2005, 2009 Charles O Nutter <headius@headius.com>
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import static org.jruby.RubyFile.get_path;
import static org.jruby.RubyFile.fileResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.platform.Platform;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JRubyFile;
import org.jruby.util.FileResource;
import org.jruby.util.TypeConverter;

@JRubyModule(name = "FileTest")
public class RubyFileTest {

    public static RubyModule createFileTestModule(Ruby runtime) {
        RubyModule fileTestModule = runtime.defineModule("FileTest");
        runtime.setFileTest(fileTestModule);

        fileTestModule.defineAnnotatedMethods(RubyFileTest.class);

        return fileTestModule;
    }

    @JRubyMethod(name = "blockdev?", required = 1, module = true)
    public static IRubyObject blockdev_p(IRubyObject recv, IRubyObject filename) {
        Ruby runtime = recv.getRuntime();
        FileResource file = fileResource(filename);

        return runtime.newBoolean(file.exists() && file.stat(runtime.getPosix()).isBlockDev());
    }

    @JRubyMethod(name = "chardev?", required = 1, module = true)
    public static IRubyObject chardev_p(IRubyObject recv, IRubyObject filename) {
        Ruby runtime = recv.getRuntime();
        FileResource file = fileResource(filename);

        return runtime.newBoolean(file.exists() && file.stat(runtime.getPosix()).isCharDev());
    }

    public static IRubyObject directory_p(IRubyObject recv, IRubyObject filename) {
        return directory_p(recv.getRuntime().getCurrentContext(), recv, filename);
    }

    public static IRubyObject directory_p(Ruby ruby, IRubyObject filename) {
        return directory_p(ruby.getCurrentContext(), filename);
    }

    @JRubyMethod(name = "directory?", required = 1, module = true)
    public static IRubyObject directory_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        return directory_p(context, filename);
    }

    public static IRubyObject directory_p(ThreadContext context, IRubyObject filename) {
        Ruby runtime = context.runtime;
        if (!(filename instanceof RubyFile || filename instanceof RubyIO)) {
            if (filename.respondsTo("to_io")) {
                filename = (RubyIO) TypeConverter.convertToType(filename, context.runtime.getIO(), "to_io");
            } else {
                filename = get_path(context, filename);
            }
        }

        FileResource file = fileResource(filename);
        return runtime.newBoolean(file.exists() && file.isDirectory());
    }

    @JRubyMethod(name = "executable?", required = 1, module = true)
    public static IRubyObject executable_p(IRubyObject recv, IRubyObject filename) {
        Ruby runtime = recv.getRuntime();
        FileResource file = fileResource(filename);

        return runtime.newBoolean(file.exists() && file.stat(runtime.getPosix()).isExecutable());
    }

    @JRubyMethod(name = "executable_real?", required = 1, module = true)
    public static IRubyObject executable_real_p(IRubyObject recv, IRubyObject filename) {
        Ruby runtime = recv.getRuntime();
        FileResource file = fileResource(filename);

        return runtime.newBoolean(file.exists() && file.stat(runtime.getPosix()).isExecutableReal());
    }

    public static IRubyObject exist_p(IRubyObject recv, IRubyObject filename) {
        return exist_p(recv.getRuntime().getCurrentContext(), recv, filename);
    }

    @JRubyMethod(name = {"exist?", "exists?"}, required = 1, module = true)
    public static IRubyObject exist_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        Ruby runtime = context.runtime;
        if (!(filename instanceof RubyFile)) {
            filename = get_path(context, filename);
        }
        
        if (existsOnClasspath(filename)) {
            return runtime.getTrue();
        }

        if (Ruby.isSecurityRestricted()) {
            return runtime.getFalse();
        }


        return runtime.newBoolean(fileResource(filename).exists());
    }

    public static RubyBoolean file_p(IRubyObject recv, IRubyObject filename) {
        return file_p(recv.getRuntime().getCurrentContext(), recv, filename);
    }

    @JRubyMethod(name = "file?", required = 1, module = true)
    public static RubyBoolean file_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        Ruby runtime = context.runtime;
        if (!(filename instanceof RubyFile)) {
            filename = get_path(context, filename);
        }

        FileResource file = fileResource(filename);
        return runtime.newBoolean(file.exists() && file.isFile());
    }

    @JRubyMethod(name = "grpowned?", required = 1, module = true)
    public static IRubyObject grpowned_p(IRubyObject recv, IRubyObject filename) {
        Ruby runtime = recv.getRuntime();
        FileResource file = fileResource(filename);

        // JRUBY-4446, grpowned? always returns false on Windows
        if (Platform.IS_WINDOWS) {
            return runtime.getFalse();
        }
        
        return runtime.newBoolean(file.exists() && file.stat(runtime.getPosix()).isGroupOwned());
    }

    @JRubyMethod(name = "identical?", required = 2, module = true)
    public static IRubyObject identical_p(IRubyObject recv, IRubyObject filename1, IRubyObject filename2) {
        Ruby runtime = recv.getRuntime();
        FileResource file1 = fileResource(filename1);
        FileResource file2 = fileResource(filename2);

        if (Platform.IS_WINDOWS || !runtime.getPosix().isNative()) {
            // posix stat uses inodes to determine indentity, and windows has no inodes
            // (they are always zero), so we use canonical paths instead. (JRUBY-5726)
            // If we can't load a native POSIX, use this same logic. (JRUBY-6982)
            if (file1.exists() && file2.exists()) {
                try {
                    String canon1 = new File(file1.absolutePath()).getCanonicalPath();
                    String canon2 = new File(file2.absolutePath()).getCanonicalPath();
                    return runtime.newBoolean(canon1.equals(canon2));
                } catch (IOException canonicalizationError) {
                    return runtime.getFalse();
                }
            } else {
                return runtime.getFalse();
            }
        }

        return runtime.newBoolean(file1.exists() && file2.exists() &&
                file1.stat(runtime.getPosix()).isIdentical(file2.stat(runtime.getPosix())));
    }

    @JRubyMethod(name = "owned?", required = 1, module = true)
    public static IRubyObject owned_p(IRubyObject recv, IRubyObject filename) {
        Ruby runtime = recv.getRuntime();
        FileResource file = fileResource(filename);

        return runtime.newBoolean(file.exists() && file.stat(runtime.getPosix()).isOwned());
    }

    @JRubyMethod(name = "pipe?", required = 1, module = true)
    public static IRubyObject pipe_p(IRubyObject recv, IRubyObject filename) {
        Ruby runtime = recv.getRuntime();
        FileResource file = fileResource(filename);

        return runtime.newBoolean(file.exists() && file.stat(runtime.getPosix()).isNamedPipe());
    }

    public static IRubyObject readable_p(IRubyObject recv, IRubyObject filename) {
        return readable_p(recv.getRuntime().getCurrentContext(), recv, filename);
    }

    // We use file test since it is faster than a stat; also euid == uid in Java always
    @JRubyMethod(name = {"readable?", "readable_real?"}, required = 1, module = true)
    public static IRubyObject readable_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        Ruby runtime = context.runtime;
        if (!(filename instanceof RubyFile)) {
            filename = get_path(context, filename);
        }

        FileResource file = fileResource(filename);
        return runtime.newBoolean(file.exists() && file.canRead());
    }

    // Not exposed by filetest, but so similiar in nature that it is stored here
    public static IRubyObject rowned_p(IRubyObject recv, IRubyObject filename) {
        Ruby runtime = recv.getRuntime();
        FileResource file = fileResource(filename);

        return runtime.newBoolean(file.exists() && file.stat(runtime.getPosix()).isROwned());
    }

    @JRubyMethod(name = "setgid?", required = 1, module = true)
    public static IRubyObject setgid_p(IRubyObject recv, IRubyObject filename) {
        Ruby runtime = recv.getRuntime();
        FileResource file = fileResource(filename);

        return runtime.newBoolean(file.exists() && file.stat(runtime.getPosix()).isSetgid());
    }

    @JRubyMethod(name = "setuid?", required = 1, module = true)
    public static IRubyObject setuid_p(IRubyObject recv, IRubyObject filename) {
        Ruby runtime = recv.getRuntime();
        FileResource file = fileResource(filename);

        return runtime.newBoolean(file.exists() && file.stat(runtime.getPosix()).isSetuid());
    }

    public static IRubyObject size(IRubyObject recv, IRubyObject filename) {
        return size(recv.getRuntime().getCurrentContext(), recv, filename);
    }

    @JRubyMethod(name = "size", required = 1, module = true)
    public static IRubyObject size(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        Ruby runtime = recv.getRuntime();
        if (!(filename instanceof RubyFile)) {
            if (filename.respondsTo("to_io")) {
                filename = (RubyIO) TypeConverter.convertToType(filename, runtime.getIO(), "to_io");
            } else {
                filename = get_path(context, filename);
            }
        }

        FileResource file = fileResource(filename);

        if (!file.exists()) {
            noFileError(filename);
        }

        return runtime.newFixnum(file.length());
    }

    public static IRubyObject size_p(IRubyObject recv, IRubyObject filename) {
        return size_p(recv.getRuntime().getCurrentContext(), recv, filename);
    }
    
    @JRubyMethod(name = "size?", required = 1, module = true)
    public static IRubyObject size_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        Ruby runtime = context.runtime;
        if (!(filename instanceof RubyFile)) {
            if (filename.respondsTo("to_io")) {
                filename = (RubyIO) TypeConverter.convertToType(filename, runtime.getIO(), "to_io");
            } else {
                filename = get_path(context, filename);
            }
        }

        FileResource file = fileResource(filename);

        if (!file.exists()) {
            return runtime.getNil();
        }

        long length = file.length();
        if (length > 0) {
            return runtime.newFixnum(length);
        } else {
            return runtime.getNil();
        }
    }

    @JRubyMethod(name = "socket?", required = 1, module = true)
    public static IRubyObject socket_p(IRubyObject recv, IRubyObject filename) {
        Ruby runtime = recv.getRuntime();
        FileResource file = fileResource(filename);

        return runtime.newBoolean(file.exists() && file.stat(runtime.getPosix()).isSocket());
    }

    @JRubyMethod(name = "sticky?", required = 1, module = true)
    public static IRubyObject sticky_p(IRubyObject recv, IRubyObject filename) {
        Ruby runtime = recv.getRuntime();
        FileResource file = fileResource(filename);

        return runtime.newBoolean(file.exists() && file.stat(runtime.getPosix()).isSticky());
    }

    @JRubyMethod(name = "symlink?", required = 1, module = true)
    public static RubyBoolean symlink_p(IRubyObject recv, IRubyObject filename) {
        Ruby runtime = recv.getRuntime();
        FileResource file = fileResource(filename);

        try {
            // Note: We can't use file.exists() to check whether the symlink
            // exists or not, because that method returns false for existing
            // but broken symlink. So, we try without the existence check,
            // but in the try-catch block.
            // MRI behavior: symlink? on broken symlink should return true.
            return runtime.newBoolean(file.lstat(runtime.getPosix()).isSymlink());
        } catch (SecurityException re) {
            return runtime.getFalse();
        } catch (RaiseException re) {
            return runtime.getFalse();
        }
    }

    // We do both writable and writable_real through the same method because
    // in our java process effective and real userid will always be the same.
    @JRubyMethod(name = {"writable?", "writable_real?"}, required = 1, module = true)
    public static RubyBoolean writable_p(IRubyObject recv, IRubyObject filename) {
        return filename.getRuntime().newBoolean(fileResource(filename).canWrite());
    }

    public static RubyBoolean zero_p(IRubyObject recv, IRubyObject filename) {
        return zero_p(recv.getRuntime().getCurrentContext(), recv, filename);
    }

    @JRubyMethod(name = "zero?", required = 1, module = true)
    public static RubyBoolean zero_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        Ruby runtime = context.runtime;
        if (!(filename instanceof RubyFile)) {
            filename = get_path(context, filename);
        }

        FileResource file = fileResource(filename);

        if (file.exists()) {
            if (file.isDirectory()) {
                // MRI behavior, enforced by RubySpecs.
                return runtime.newBoolean(Platform.IS_WINDOWS);
            } else {
                return runtime.newBoolean(file.length() == 0L);
            }
        } else {
            return runtime.getFalse();
        }
    }

    @JRubyMethod(name = "world_readable?", required = 1, module = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject worldReadable(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        RubyFileStat stat = getFileStat(context, filename);
        if (stat == null) {
            return context.runtime.getNil();
        }

        return stat.worldReadable(context);
    }

    @JRubyMethod(name = "world_writable?", required = 1, module = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject worldWritable(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        RubyFileStat stat = getFileStat(context, filename);
        if (stat == null) {
            return context.runtime.getNil();
        }

        return stat.worldWritable(context);
    }

    /**
     * MRI defines the FileTest methods both as module functions on FileTest and
     * as singleton methods on the File class. Since our annotations can't be
     * both module and singleton method definitions, we define proxies here for
     * File.
     *
     * Note that these are to be defined on the singleton class of the File
     * class.
     */
    public static class FileTestFileMethods {
        @JRubyMethod(name = "blockdev?", required = 1)
        public static IRubyObject blockdev_p(IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.blockdev_p(recv, filename);
        }

        @JRubyMethod(name = "chardev?", required = 1)
        public static IRubyObject chardev_p(IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.chardev_p(recv, filename);
        }

        @JRubyMethod(name = "directory?", required = 1)
        public static IRubyObject directory_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.directory_p(context, recv, filename);
        }

        @JRubyMethod(name = "executable?", required = 1)
        public static IRubyObject executable_p(IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.executable_p(recv, filename);
        }

        @JRubyMethod(name = "executable_real?", required = 1)
        public static IRubyObject executable_real_p(IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.executable_real_p(recv, filename);
        }

        @JRubyMethod(name = {"exist?", "exists?"}, required = 1)
        public static IRubyObject exist_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.exist_p(context, recv, filename);
        }

        @JRubyMethod(name = "file?", required = 1)
        public static RubyBoolean file_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.file_p(context, recv, filename);
        }

        @JRubyMethod(name = "grpowned?", required = 1)
        public static IRubyObject grpowned_p(IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.grpowned_p(recv, filename);
        }

        @JRubyMethod(name = "identical?", required = 2)
        public static IRubyObject identical_p(IRubyObject recv, IRubyObject filename1, IRubyObject filename2) {
            return RubyFileTest.identical_p(recv, filename1, filename2);
        }

        @JRubyMethod(name = "owned?", required = 1)
        public static IRubyObject owned_p(IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.owned_p(recv, filename);
        }

        @JRubyMethod(name = "pipe?", required = 1)
        public static IRubyObject pipe_p(IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.pipe_p(recv, filename);
        }

        @JRubyMethod(name = {"readable?", "readable_real?"}, required = 1)
        public static IRubyObject readable_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.readable_p(context, recv, filename);
        }

        @JRubyMethod(name = "setgid?", required = 1)
        public static IRubyObject setgid_p(IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.setgid_p(recv, filename);
        }

        @JRubyMethod(name = "setuid?", required = 1)
        public static IRubyObject setuid_p(IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.setuid_p(recv, filename);
        }

        @JRubyMethod(name = "size", required = 1)
        public static IRubyObject size(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.size(context, recv, filename);
        }

        @JRubyMethod(name = "size?", required = 1)
        public static IRubyObject size_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.size_p(context, recv, filename);
        }

        @JRubyMethod(name = "socket?", required = 1)
        public static IRubyObject socket_p(IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.socket_p(recv, filename);
        }

        @JRubyMethod(name = "sticky?", required = 1)
        public static IRubyObject sticky_p(IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.sticky_p(recv, filename);
        }

        @JRubyMethod(name = "symlink?", required = 1)
        public static RubyBoolean symlink_p(IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.symlink_p(recv, filename);
        }

        @JRubyMethod(name = {"writable?", "writable_real?"}, required = 1)
        public static RubyBoolean writable_p(IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.writable_p(recv, filename);
        }

        @JRubyMethod(name = "zero?", required = 1)
        public static RubyBoolean zero_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.zero_p(context, recv, filename);
        }

        @JRubyMethod(name = "world_readable?", required = 1, compat = CompatVersion.RUBY1_9)
        public static IRubyObject worldReadable(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.worldReadable(context, recv, filename);
        }

        @JRubyMethod(name = "world_writable?", required = 1, compat = CompatVersion.RUBY1_9)
        public static IRubyObject worldWritable(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.worldWritable(context, recv, filename);
        }
    }

    private static RubyFileStat getFileStat(ThreadContext context, IRubyObject filename) {
        Ruby runtime = context.runtime;

        RubyFileStat stat = null;
        if (!(filename instanceof RubyFile)) {
            RubyString path = get_path(context, filename);
            FileResource file = JRubyFile.createResource(runtime.getCurrentDirectory(), path.getUnicodeValue());
            if (file.exists()) {
                stat = runtime.newFileStat(file.absolutePath(), false);
            }
        } else {
            stat = (RubyFileStat) ((RubyFile) filename).stat(context);
        }

        return stat;
    }

    private static boolean existsOnClasspath(IRubyObject path) {
        if (path instanceof RubyFile) {
            return false;
        }

        Ruby runtime = path.getRuntime();
        RubyString pathStr = get_path(runtime.getCurrentContext(), path);
        String pathJStr = pathStr.getUnicodeValue();
        if (pathJStr.startsWith("classpath:/")) {
            pathJStr = pathJStr.substring("classpath:/".length());

            ClassLoader classLoader = runtime.getJRubyClassLoader();
            // handle security-sensitive case
            if (Ruby.isSecurityRestricted() && classLoader == null) {
                classLoader = runtime.getInstanceConfig().getLoader();
            }

            InputStream is = classLoader.getResourceAsStream(pathJStr);
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignore) {
                } catch (NullPointerException wtf) {
                    // that's what sometimes happens, weird
                }
                return true;
            }
        }
        return false;
    }

    private static void noFileError(IRubyObject filename) {
        throw filename.getRuntime().newErrnoENOENTError("No such file or directory - " +
                filename.convertToString());
    }
}
