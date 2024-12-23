/***** BEGIN LICENSE BLOCK *****
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
import static org.jruby.api.Access.instanceConfig;
import static org.jruby.api.Access.ioClass;
import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Define.defineModule;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import jnr.posix.FileStat;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.platform.Platform;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JRubyFile;
import org.jruby.util.FileResource;
import org.jruby.util.TypeConverter;

@JRubyModule(name = "FileTest")
public class RubyFileTest {
    public static RubyModule createFileTestModule(ThreadContext context) {
        return defineModule(context, "FileTest").defineMethods(context, RubyFileTest.class);
    }

    @JRubyMethod(name = "blockdev?", module = true)
    public static IRubyObject blockdev_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        FileStat stat = fileResource(context, filename).stat();

        return asBoolean(context, stat != null && stat.isBlockDev());
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public static IRubyObject blockdev_p(IRubyObject recv, IRubyObject filename) {
        return blockdev_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
    }

    @JRubyMethod(name = "chardev?", module = true)
    public static IRubyObject chardev_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        FileStat stat = fileResource(context, filename).stat();

        return asBoolean(context, stat != null && stat.isCharDev());
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public static IRubyObject chardev_p(IRubyObject recv, IRubyObject filename) {
        return chardev_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public static IRubyObject directory_p(IRubyObject recv, IRubyObject filename) {
        return directory_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public static IRubyObject directory_p(Ruby ruby, IRubyObject filename) {
        return directory_p(ruby.getCurrentContext(), filename);
    }

    @JRubyMethod(name = "directory?", module = true)
    public static IRubyObject directory_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        return directory_p(context, filename);
    }

    public static IRubyObject directory_p(ThreadContext context, IRubyObject filename) {
        if (!(filename instanceof RubyIO) && filename.respondsTo("to_io")) {
            filename = TypeConverter.convertToType(filename, ioClass(context), "to_io");
        }

        return asBoolean(context, fileResource(context, filename).isDirectory());
    }

    @JRubyMethod(name = "executable?", module = true)
    public static IRubyObject executable_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        return asBoolean(context, fileResource(context, filename).canExecute());
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public static IRubyObject executable_p(IRubyObject recv, IRubyObject filename) {
        return executable_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
    }

    @JRubyMethod(name = "executable_real?", module = true)
    public static IRubyObject executable_real_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        if (context.runtime.getPosix().isNative()) {
            FileStat stat = fileResource(context, filename).stat();
            return asBoolean(context, stat != null && stat.isExecutableReal());
        } else {
            return executable_p(context, recv, filename);
        }
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public static IRubyObject executable_real_p(IRubyObject recv, IRubyObject filename) {
        return executable_real_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public static IRubyObject exist_p(IRubyObject recv, IRubyObject filename) {
        return exist_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
    }

    @JRubyMethod(name = "exist?", module = true)
    public static IRubyObject exist_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        // We get_path here to prevent doing it both existsOnClasspath and fileResource (Only call to_path once).
        return asBoolean(context, exist(context, get_path(context, filename)));
    }

    static boolean exist(ThreadContext context, RubyString path) {
        final String pathStr = path.decodeString();
        if (!Ruby.isSecurityRestricted()) {
            if (JRubyFile.createResource(context.runtime, pathStr).exists()) return true;
        }
        return existsOnClasspath(context, pathStr);
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public static RubyBoolean file_p(IRubyObject recv, IRubyObject filename) {
        return file_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
    }

    @JRubyMethod(name = "file?", module = true)
    public static RubyBoolean file_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        return asBoolean(context, fileResource(context, filename).isFile());
    }

    @JRubyMethod(name = "grpowned?", module = true)
    public static IRubyObject grpowned_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        FileStat stat = fileResource(context, filename).stat();

        // JRUBY-4446, grpowned? always returns false on Windows
        if (Platform.IS_WINDOWS) return context.fals;

        return asBoolean(context, stat != null && stat.isGroupOwned());
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public static IRubyObject grpowned_p(IRubyObject recv, IRubyObject filename) {
        return grpowned_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
    }

    @JRubyMethod(name = "identical?", module = true)
    public static IRubyObject identical_p(ThreadContext context, IRubyObject recv, IRubyObject filename1, IRubyObject filename2) {
        FileResource file1 = fileResource(context, filename1);
        FileResource file2 = fileResource(context, filename2);

        // Try posix first
        if (!Platform.IS_WINDOWS && context.runtime.getPosix().isNative()) {
            FileStat stat1 = file1.stat();
            FileStat stat2 = file2.stat();

            return asBoolean(context, stat1 != null && stat2 != null && stat1.isIdentical(stat2));
        }

        // fallback to NIO2 to support more platforms
        if (file1.exists() && file2.exists()) {
            try {
                Path canon1 = new File(file1.absolutePath()).getCanonicalFile().toPath();
                Path canon2 = new File(file2.absolutePath()).getCanonicalFile().toPath();
                return asBoolean(context, Files.isSameFile(canon1, canon2));
            } catch (IOException canonicalizationError) {
                // fall through
            }
        }

        return context.fals;
    }

    @JRubyMethod(name = "owned?", module = true)
    public static IRubyObject owned_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        FileStat stat = fileResource(context, filename).stat();

        return asBoolean(context, stat != null && stat.isOwned());
    }

    @Deprecated
    public static IRubyObject owned_p(IRubyObject recv, IRubyObject filename) {
        return owned_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
    }

    @JRubyMethod(name = "pipe?", module = true)
    public static IRubyObject pipe_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        FileStat stat = fileResource(context, filename).stat();

        return asBoolean(context, stat != null && stat.isNamedPipe());
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public static IRubyObject pipe_p(IRubyObject recv, IRubyObject filename) {
        return pipe_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public static IRubyObject readable_p(IRubyObject recv, IRubyObject filename) {
        return readable_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
    }

    // We use file test since it is faster than a stat; also euid == uid in Java always
    @JRubyMethod(name = {"readable?", "readable_real?"}, module = true)
    public static IRubyObject readable_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        Ruby runtime = context.runtime;
        if (!(filename instanceof RubyFile)) {
            filename = get_path(context, filename);
        }

        return asBoolean(context, fileResource(context, filename).canRead());
    }

    // Not exposed by filetest, but so similar in nature that it is stored here
    public static IRubyObject rowned_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        FileStat stat = fileResource(context, filename).stat();

        return asBoolean(context, stat != null && stat.isROwned());
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public static IRubyObject rowned_p(IRubyObject recv, IRubyObject filename) {
        return rowned_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
    }

    @JRubyMethod(name = "setgid?", module = true)
    public static IRubyObject setgid_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        FileStat stat = fileResource(context, filename).stat();

        return asBoolean(context, stat != null && stat.isSetgid());
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public static IRubyObject setgid_p(IRubyObject recv, IRubyObject filename) {
        return setgid_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
    }

    @JRubyMethod(name = "setuid?", module = true)
    public static IRubyObject setuid_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        FileStat stat = fileResource(context, filename).stat();

        return asBoolean(context, stat != null && stat.isSetuid());
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public static IRubyObject setuid_p(IRubyObject recv, IRubyObject filename) {
        return setuid_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public static IRubyObject size(IRubyObject recv, IRubyObject filename) {
        return size(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
    }

    @JRubyMethod(name = "size", module = true)
    public static IRubyObject size(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        if (!(filename instanceof RubyFile) && filename.respondsTo("to_io")) {
             filename = TypeConverter.convertToType(filename, ioClass(context), "to_io");
        }

        FileStat stat = fileResource(context, filename).stat();

        if (stat == null) noFileError(context, filename);

        return asFixnum(context, stat.st_size());
    }

    @Deprecated
    public static IRubyObject size_p(IRubyObject recv, IRubyObject filename) {
        return size_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
    }
    
    @JRubyMethod(name = "size?", module = true)
    public static IRubyObject size_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        if (!(filename instanceof RubyFile) && filename.respondsTo("to_io")) {
            filename = TypeConverter.convertToType(filename, ioClass(context), "to_io");
        }

        FileStat stat = fileResource(context, filename).stat();

        if (stat == null) return context.nil;

        long length = stat.st_size();
        return length > 0 ? asFixnum(context, length) : context.nil;
    }

    @JRubyMethod(name = "socket?", module = true)
    public static IRubyObject socket_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        FileStat stat = fileResource(context, filename).stat();

        return asBoolean(context, stat != null && stat.isSocket());
    }

    @Deprecated
    public static IRubyObject socket_p(IRubyObject recv, IRubyObject filename) {
        return socket_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
    }

    @JRubyMethod(name = "sticky?", module = true)
    public static IRubyObject sticky_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        FileStat stat = fileResource(context, filename).stat();

        return asBoolean(context, stat != null && stat.isSticky());
    }

    @Deprecated
    public static IRubyObject sticky_p(IRubyObject recv, IRubyObject filename) {
        return sticky_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
    }

    @JRubyMethod(name = "symlink?", module = true)
    public static RubyBoolean symlink_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        return asBoolean(context, fileResource(context, filename).isSymLink());        
    }
    
    @Deprecated
    public static RubyBoolean symlink_p(IRubyObject recv, IRubyObject filename) {
        return symlink_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
    }

    // We do both writable and writable_real through the same method because
    // in our java process effective and real userid will always be the same.
    @JRubyMethod(name = {"writable?", "writable_real?"}, module = true)
    public static RubyBoolean writable_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        return asBoolean(context, fileResource(context, filename).canWrite());        
    }
    
    @Deprecated
    public static RubyBoolean writable_p(IRubyObject recv, IRubyObject filename) {
        return writable_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
    }

    @Deprecated
    public static RubyBoolean zero_p(IRubyObject recv, IRubyObject filename) {
        return zero_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
    }

    @JRubyMethod(name = {"empty?", "zero?"}, module = true)
    public static RubyBoolean zero_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        FileResource resource = fileResource(context, filename);

        // FIXME: Ultimately we should return a valid stat() from this but without massive NUL coverage
        // this is less risky.
        if (resource.isNull()) return asBoolean(context, true);

        FileStat stat = resource.stat();

        if (stat == null) return context.fals;
        // MRI behavior, enforced by RubySpecs.
        if (stat.isDirectory()) return asBoolean(context, Platform.IS_WINDOWS);

        return asBoolean(context, stat.st_size() == 0L);
    }

    @JRubyMethod(name = "world_readable?", module = true)
    public static IRubyObject worldReadable(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        RubyFileStat stat = getRubyFileStat(context, filename);

        return stat == null ? context.nil : stat.worldReadable(context);
    }

    @JRubyMethod(name = "world_writable?", module = true)
    public static IRubyObject worldWritable(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        RubyFileStat stat = getRubyFileStat(context, filename);

        return stat == null ? context.nil : stat.worldWritable(context);
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
        @JRubyMethod(name = "blockdev?")
        public static IRubyObject blockdev_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.blockdev_p(context, recv, filename);
        }
        
        @Deprecated
        public static IRubyObject blockdev_p(IRubyObject recv, IRubyObject filename) {
            return blockdev_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
        }

        @JRubyMethod(name = "chardev?")
        public static IRubyObject chardev_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.chardev_p(context, recv, filename);            
        }
        
        @Deprecated
        public static IRubyObject chardev_p(IRubyObject recv, IRubyObject filename) {
            return chardev_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
        }

        @JRubyMethod(name = "directory?")
        public static IRubyObject directory_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.directory_p(context, recv, filename);
        }

        @JRubyMethod(name = "executable?")
        public static IRubyObject executable_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.executable_p(context, recv, filename);            
        }
        
        @Deprecated
        public static IRubyObject executable_p(IRubyObject recv, IRubyObject filename) {
            return executable_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
        }

        @JRubyMethod(name = "executable_real?")
        public static IRubyObject executable_real_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.executable_real_p(context, recv, filename);            
        }
        
        @Deprecated
        public static IRubyObject executable_real_p(IRubyObject recv, IRubyObject filename) {
            return executable_real_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
        }

        @JRubyMethod(name = {"exist?"})
        public static IRubyObject exist_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.exist_p(context, recv, filename);
        }

        @JRubyMethod(name = "file?")
        public static RubyBoolean file_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.file_p(context, recv, filename);
        }

        @JRubyMethod(name = "grpowned?")
        public static IRubyObject grpowned_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.grpowned_p(context, recv, filename);            
        }
        
        @Deprecated
        public static IRubyObject grpowned_p(IRubyObject recv, IRubyObject filename) {
            return grpowned_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
        }

        @JRubyMethod(name = "identical?")
        public static IRubyObject identical_p(ThreadContext context, IRubyObject recv, IRubyObject filename1, IRubyObject filename2) {
            return RubyFileTest.identical_p(context, recv, filename1, filename2);
        }

        @JRubyMethod(name = "owned?")
        public static IRubyObject owned_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.owned_p(context, recv, filename);
        }
        
        @Deprecated
        public static IRubyObject owned_p(IRubyObject recv, IRubyObject filename) {
            return owned_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
        }

        @JRubyMethod(name = "pipe?")
        public static IRubyObject pipe_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.pipe_p(context, recv, filename);
        }
        
        @Deprecated
        public static IRubyObject pipe_p(IRubyObject recv, IRubyObject filename) {
            return pipe_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
        }

        @JRubyMethod(name = {"readable?", "readable_real?"})
        public static IRubyObject readable_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.readable_p(context, recv, filename);
        }

        @JRubyMethod(name = "setgid?")
        public static IRubyObject setgid_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.setgid_p(context, recv, filename);
        }

        @Deprecated(since = "10.0", forRemoval = true)
        public static IRubyObject setgid_p(IRubyObject recv, IRubyObject filename) {
            return setgid_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
        }

        @JRubyMethod(name = "setuid?")
        public static IRubyObject setuid_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.setuid_p(context, recv, filename);            
        }

        @Deprecated(since = "10.0", forRemoval = true)
        public static IRubyObject setuid_p(IRubyObject recv, IRubyObject filename) {
            return setuid_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
        }

        @JRubyMethod(name = "size")
        public static IRubyObject size(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.size(context, recv, filename);
        }

        @JRubyMethod(name = "size?")
        public static IRubyObject size_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.size_p(context, recv, filename);
        }

        @JRubyMethod(name = "socket?")
        public static IRubyObject socket_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.socket_p(context, recv, filename);            
        }
        
        @Deprecated
        public static IRubyObject socket_p(IRubyObject recv, IRubyObject filename) {
            return socket_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
        }

        @JRubyMethod(name = "sticky?")
        public static IRubyObject sticky_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.sticky_p(context, recv, filename);
        }

        @Deprecated
        public static IRubyObject sticky_p(IRubyObject recv, IRubyObject filename) {
            return sticky_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
        }

        @JRubyMethod(name = "symlink?")
        public static RubyBoolean symlink_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.symlink_p(context, recv, filename);            
        }
        
        @Deprecated
        public static RubyBoolean symlink_p(IRubyObject recv, IRubyObject filename) {
            return symlink_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
        }

        @JRubyMethod(name = {"writable?", "writable_real?"})
        public static RubyBoolean writable_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.writable_p(context, recv, filename);
        }
        
        @Deprecated
        public static RubyBoolean writable_p(IRubyObject recv, IRubyObject filename) {
            return writable_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename);
        }

        @JRubyMethod(name = {"empty?", "zero?"})
        public static RubyBoolean zero_p(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.zero_p(context, recv, filename);
        }

        @JRubyMethod(name = "world_readable?")
        public static IRubyObject worldReadable(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.worldReadable(context, recv, filename);
        }

        @JRubyMethod(name = "world_writable?")
        public static IRubyObject worldWritable(ThreadContext context, IRubyObject recv, IRubyObject filename) {
            return RubyFileTest.worldWritable(context, recv, filename);
        }

        @Deprecated
        public static IRubyObject identical_p(IRubyObject recv, IRubyObject filename1, IRubyObject filename2) {
            return RubyFileTest.identical_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename1, filename2);
        }
    }

    private static RubyFileStat getRubyFileStat(ThreadContext context, IRubyObject filename) {
        Ruby runtime = context.runtime;

        RubyFileStat stat = null;
        if (!(filename instanceof RubyFile)) {
            RubyString path = get_path(context, filename);
            FileResource file = JRubyFile.createResource(runtime, path.getUnicodeValue());
            if (file.exists()) {
                stat = runtime.newFileStat(file.absolutePath(), false);
            }
        } else {
            stat = (RubyFileStat) ((RubyFile) filename).stat(context);
        }

        return stat;
    }

    private static boolean existsOnClasspath(ThreadContext context, String path) {
        if (path.startsWith("classpath:/")) {
            path = path.substring("classpath:/".length());

            ClassLoader classLoader = context.runtime.getJRubyClassLoader();
            // handle security-sensitive case
            if (classLoader == null && Ruby.isSecurityRestricted()) {
                classLoader = instanceConfig(context).getLoader();
            }

            InputStream is = classLoader.getResourceAsStream(path);
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

    private static void noFileError(ThreadContext context, IRubyObject filename) {
        throw context.runtime.newErrnoENOENTError("No such file or directory - " + filename.convertToString());
    }

    @Deprecated
    public static IRubyObject identical_p(IRubyObject recv, IRubyObject filename1, IRubyObject filename2) {
        return identical_p(((RubyBasicObject) recv).getCurrentContext(), recv, filename1, filename2);
    }
}
